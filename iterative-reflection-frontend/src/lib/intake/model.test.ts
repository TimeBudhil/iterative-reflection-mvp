import { describe, expect, it } from "vitest";
import {
  MAX_TEXT,
  buildRequest,
  groupByCategory,
  initialDraft,
  interestsStatus,
  isNotYetAvailable,
  mergeFieldErrors,
  toggleTag,
  type IntakeQuestion,
} from "./model";

const questions: IntakeQuestion[] = [
  { key: "interests", prompt: "Which?", type: "TAGS", options: [{ id: 1, category: "Cluster", name: "A" }, { id: 2, category: "Cluster", name: "B" }, { id: 3, category: "Work Style", name: "C" }] },
  { key: "goals", prompt: "Goals?", type: "TEXT" },
];

describe("groupByCategory", () => {
  it("keeps first-seen category order", () => {
    const g = groupByCategory(questions[0].options!);
    expect(g.map((x) => x.category)).toEqual(["Cluster", "Work Style"]);
    expect(g[0].options.map((o) => o.id)).toEqual([1, 2]);
  });
});

describe("initialDraft", () => {
  it("is empty without an existing intake", () => {
    expect(initialDraft(questions)).toEqual({ tags: {}, texts: {} });
  });
  it("prefills tags and text", () => {
    const d = initialDraft(questions, { answers: [{ key: "interests", tags: [{ id: 2 }] }, { key: "goals", text: "Be a nurse" }] });
    expect(d).toEqual({ tags: { interests: [2] }, texts: { goals: "Be a nurse" } });
  });
});

describe("toggleTag", () => {
  it("adds and removes", () => {
    expect(toggleTag([1], 2)).toEqual([1, 2]);
    expect(toggleTag([1, 2], 1)).toEqual([2]);
  });
});

describe("buildRequest", () => {
  it("drops blank answers and trims text; sends the whole form", () => {
    const r = buildRequest(questions, { tags: { interests: [1, 3] }, texts: { goals: "  hi  " } });
    expect(r).toEqual({ answers: { interests: { tagIds: [1, 3] }, goals: { text: "hi" } } });
  });
  it("omits empty tag lists and whitespace text", () => {
    expect(buildRequest(questions, { tags: { interests: [] }, texts: { goals: "   " } })).toBeNull();
  });
  it("rejects over-long text", () => {
    expect(buildRequest(questions, { tags: {}, texts: { goals: "x".repeat(MAX_TEXT + 1) } })).toBeNull();
  });
});

describe("isNotYetAvailable", () => {
  it("only the 3-reflection 409", () => {
    expect(isNotYetAvailable({ status: 409, detail: "Reflect on at least 3 careers before filling in the intake." })).toBe(true);
    expect(isNotYetAvailable({ status: 409, detail: "The intake was already submitted; edit it instead." })).toBe(false);
    expect(isNotYetAvailable({ status: 400, detail: "Reflect on at least 3" })).toBe(false);
  });
});

describe("interestsStatus", () => {
  it("labels", () => {
    expect(interestsStatus(true)).toBe("Completed");
    expect(interestsStatus(false)).toBe("Not filled in");
  });
});

describe("mergeFieldErrors", () => {
  it("maps answers[k]... to the question key and keeps the rest", () => {
    const m = mergeFieldErrors({ "answers[goals].text": "Too long", answers: "Answer at least one question." });
    expect(m.byKey).toEqual({ goals: "Too long" });
    expect(m.form).toBe("Answer at least one question.");
  });
});
