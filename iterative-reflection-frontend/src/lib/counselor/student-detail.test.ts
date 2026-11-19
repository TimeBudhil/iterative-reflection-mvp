import { describe, expect, it } from "vitest";
import { toStudentDetailView } from "./student-detail";

const raw = {
  studentId: 5,
  displayName: "Sam",
  reflections: [
    {
      id: 1,
      careerId: 1,
      careerTitle: "Nurse",
      verdict: "YES",
      versionNumber: 1,
      createdAt: "2026-01-02T00:00:00Z",
      updatedAt: "2026-01-03T00:00:00Z",
      answers: [
        { key: "interest", prompt: "Interested?", score: 7, openText: "Loved it" },
        { key: "values", prompt: "Values?", score: 3, openText: null },
      ],
    },
  ],
  abandonedCareers: [{ careerId: 2, title: "Chef", lastViewedPage: 2, abandonCount: 1, abandonedAt: "2026-01-04T00:00:00Z" }],
  topAffinities: [
    { tagId: 1, tagName: "Hands-On", category: "Work Style", score: 0.5 },
    { tagId: 2, tagName: "Outdoors", category: "Environment", score: -0.1 },
  ],
  intake: null,
};

describe("toStudentDetailView", () => {
  it("keeps API order and exposes score per dimension plus only the open answers that exist", () => {
    const v = toStudentDetailView(raw);
    expect(v.name).toBe("Sam");
    expect(v.affinities.map((a) => `${a.tag} (${a.category})`)).toEqual(["Hands-On (Work Style)", "Outdoors (Environment)"]);
    expect(v.reflections[0].career).toBe("Nurse");
    expect(v.reflections[0].verdict).toBe("YES");
    expect(v.reflections[0].dimensions.map((d) => [d.prompt, d.score])).toEqual([
      ["Interested?", 7],
      ["Values?", 3],
    ]);
    expect(v.reflections[0].openAnswers).toEqual([{ prompt: "Interested?", text: "Loved it" }]);
    expect(v.abandoned).toEqual([{ career: "Chef", lastPage: 2, count: 1 }]);
    expect(v.intake).toBeNull();
  });

  it("flattens intake answers to tag names or text and skips empty ones", () => {
    const v = toStudentDetailView({
      ...raw,
      intake: {
        answers: [
          { key: "a", prompt: "Which interest you?", type: "TAGS", text: null, tags: [{ id: 1, category: "C", name: "Art" }, { id: 2, category: "C", name: "Tech" }] },
          { key: "b", prompt: "Goals?", type: "TEXT", text: "College", tags: [] },
          { key: "c", prompt: "Blank?", type: "TEXT", text: null, tags: [] },
        ],
      },
    });
    expect(v.intake).toEqual([
      { prompt: "Which interest you?", answer: "Art, Tech" },
      { prompt: "Goals?", answer: "College" },
    ]);
  });
});
