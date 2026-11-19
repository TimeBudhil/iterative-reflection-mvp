import { describe, expect, it } from "vitest";
import { canSubmit, clientErrors, isConcurrentEdit, initialDraft, mergeServerErrors, toRequest, type Question } from "./form";

const questions: Question[] = [
  { key: "interest", prompt: "I?", openPrompt: "Why?", minScore: 1, maxScore: 10 },
  { key: "values", prompt: "V?", openPrompt: "Which?", minScore: 1, maxScore: 10 },
];

describe("reflection form model", () => {
  it("is gated until every scale and the verdict are set", () => {
    let d = initialDraft(questions);
    expect(canSubmit(questions, d)).toBe(false);
    d = { ...d, scores: { interest: 7 }, verdict: "YES" };
    expect(canSubmit(questions, d)).toBe(false);
    d = { ...d, scores: { interest: 7, values: 3 } };
    expect(canSubmit(questions, d)).toBe(true);
    expect(canSubmit(questions, { ...d, verdict: undefined })).toBe(false);
  });

  it("rejects scores outside the API's min/max and over-long texts", () => {
    const d = { scores: { interest: 11, values: 3 }, texts: { values: "x".repeat(1001) }, verdict: "NO" as const };
    expect(canSubmit(questions, d)).toBe(false);
    const e = clientErrors(questions, d);
    expect(e.questions.interest?.score).toBeTruthy();
    expect(e.questions.values?.openText).toBeTruthy();
  });

  it("keys the payload by question key and omits blank texts", () => {
    const d = { scores: { interest: 7, values: 3 }, texts: { interest: " because ", values: "   " }, verdict: "MAYBE" as const };
    expect(toRequest(questions, d)).toEqual({
      answers: { interest: { score: 7, openText: "because" }, values: { score: 3 } },
      verdict: "MAYBE",
    });
  });

  it("prefills from an existing reflection", () => {
    const d = initialDraft(questions, {
      verdict: "NO",
      answers: [
        { key: "interest", score: 4, openText: "meh" },
        { key: "values", score: 9, openText: null },
      ],
    });
    expect(d).toEqual({ scores: { interest: 4, values: 9 }, texts: { interest: "meh" }, verdict: "NO" });
  });

  it("maps server field errors back onto questions and the verdict", () => {
    const e = mergeServerErrors({
      "answers[values].score": "A score from 1 to 10 is required.",
      "answers[interest].openText": "Open answer must be at most 1000 characters.",
      verdict: "A verdict of YES, MAYBE or NO is required.",
      "answers[bogus]": "Unknown question.",
    });
    expect(e.questions.values?.score).toBe("A score from 1 to 10 is required.");
    expect(e.questions.interest?.openText).toMatch(/1000/);
    expect(e.verdict).toMatch(/verdict/);
    expect(e.other).toEqual(["Unknown question."]);
  });
});

describe("isConcurrentEdit", () => {
  it("matches only the optimistic-lock 409", () => {
    expect(isConcurrentEdit({ status: 409, detail: "The reflection was changed concurrently; retry." })).toBe(true);
    expect(isConcurrentEdit({ status: 409, detail: "Finish your current career before starting another." })).toBe(false);
    expect(isConcurrentEdit({ status: 400, detail: "concurrent" })).toBe(false);
    expect(isConcurrentEdit(null)).toBe(false);
  });
});
