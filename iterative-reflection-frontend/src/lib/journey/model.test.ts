import { describe, expect, it } from "vitest";
import { journeyModel, TABS, type JourneyReflection } from "./model";

const r = (id: number, verdict: "YES" | "MAYBE" | "NO", updatedAt: string, scores: Record<string, number> = {}): JourneyReflection => ({
  id,
  careerId: id * 10,
  careerTitle: `Career ${id}`,
  verdict,
  updatedAt,
  answers: Object.entries(scores).map(([key, score]) => ({ key, score })),
});

const list = [
  r(1, "YES", "2026-01-01T10:00:00Z", { interest: 8, values: 4 }),
  r(2, "NO", "2026-03-01T10:00:00Z", { interest: 2, values: 5 }),
  r(3, "MAYBE", "2026-02-01T10:00:00Z", { interest: 5, values: 6 }),
  r(4, "YES", "2026-04-01T10:00:00Z", { interest: 9, values: 3 }),
];

describe("journeyModel", () => {
  const m = journeyModel(list);
  it("counts verdicts and total", () => {
    expect(m.total).toBe(4);
    expect(m.counts).toEqual({ YES: 2, MAYBE: 1, NO: 1 });
  });
  it("averages each dimension exactly", () => {
    expect(m.averages).toEqual([
      { key: "interest", average: 6 },
      { key: "values", average: 4.5 },
    ]);
  });
  it("filters by tab, newest first", () => {
    expect(m.tab("saved").map((x) => x.id)).toEqual([4, 3, 1]);
    expect(m.tab("YES").map((x) => x.id)).toEqual([4, 1]);
    expect(m.tab("MAYBE").map((x) => x.id)).toEqual([3]);
    expect(m.tab("NO").map((x) => x.id)).toEqual([2]);
    expect(m.tab("all").map((x) => x.id)).toEqual([4, 2, 3, 1]);
    expect(TABS[0].id).toBe("saved");
  });
  it("does not mutate the input", () => {
    expect(list.map((x) => x.id)).toEqual([1, 2, 3, 4]);
  });
  it("finds a reflection by career id", () => {
    expect(m.reflectionFor(30)?.id).toBe(3);
    expect(m.reflectionFor(999)).toBeUndefined();
  });
  it("handles empty input", () => {
    const e = journeyModel([]);
    expect(e.total).toBe(0);
    expect(e.counts).toEqual({ YES: 0, MAYBE: 0, NO: 0 });
    expect(e.averages).toEqual([]);
    expect(e.tab("all")).toEqual([]);
  });
  it("breaks equal timestamps by higher id first", () => {
    const t = journeyModel([r(1, "YES", "2026-01-01T00:00:00Z"), r(2, "YES", "2026-01-01T00:00:00Z")]);
    expect(t.tab("all").map((x) => x.id)).toEqual([2, 1]);
  });
});
