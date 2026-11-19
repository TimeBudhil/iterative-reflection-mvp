import { describe, expect, it } from "vitest";
import { feedView, startLabel } from "./model";

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
