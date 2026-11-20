import { describe, expect, it } from "vitest";
import { feedProgress, feedView, startLabel } from "./model";

describe("startLabel", () => {
  it("says Start with no progress", () => {
    expect(startLabel({ lastViewedPage: 0, lastPage: 5 })).toBe("Start");
    expect(startLabel({ lastPage: 5 })).toBe("Start");
  });
  it("says Continue (page n of m) with progress", () => {
    expect(startLabel({ lastViewedPage: 2, lastPage: 5 })).toBe("Continue (page 2 of 5)");
  });
});

describe("feedView", () => {
  const card = { id: 7, title: "Nurse", summary: "s", estimatedMinutes: 12, tags: [], lastPage: 3, lastViewedPage: 1 };
  it("is a card when a career is present", () => {
    expect(feedView({ career: card, exhausted: false })).toEqual({
      kind: "card",
      card,
      minutes: "~12 min",
      label: "Continue (page 1 of 3)",
      href: "/student/careers/7",
    });
  });
  it("is exhausted when there is no career", () => {
    expect(feedView({ career: undefined, exhausted: true })).toEqual({ kind: "exhausted" });
    expect(feedView({ career: null, exhausted: true })).toEqual({ kind: "exhausted" });
  });
});

describe("feedProgress", () => {
  it("0 reflections: intake line, no explored count", () => {
    expect(feedProgress(0)).toEqual({
      intake: { label: "0 of 3 reflections until your interests form unlocks", current: 0, max: 3 },
      explored: null,
    });
  });
  it("1 reflection: singular explored count", () => {
    const p = feedProgress(1);
    expect(p.intake?.label).toBe("1 of 3 reflections until your interests form unlocks");
    expect(p.explored).toBe("1 career explored");
  });
  it("2 reflections: plural explored count", () => {
    const p = feedProgress(2);
    expect(p.intake).toMatchObject({ current: 2, max: 3 });
    expect(p.explored).toBe("2 careers explored");
  });
  it("3 and 5 reflections: intake hidden", () => {
    expect(feedProgress(3)).toEqual({ intake: null, explored: "3 careers explored" });
    expect(feedProgress(5)).toEqual({ intake: null, explored: "5 careers explored" });
  });
});
