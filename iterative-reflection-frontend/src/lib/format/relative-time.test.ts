import { describe, expect, it } from "vitest";
import { relativeTime } from "./relative-time";

const now = new Date("2026-11-18T12:00:00Z");
const ago = (ms: number) => new Date(now.getTime() - ms).toISOString();
const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

describe("relativeTime", () => {
  it("shows Never when there is no timestamp", () => {
    expect(relativeTime(undefined, now)).toBe("Never");
    expect(relativeTime(null, now)).toBe("Never");
  });
  it("says just now under a minute (and for slight clock skew into the future)", () => {
    expect(relativeTime(ago(20_000), now)).toBe("just now");
    expect(relativeTime(ago(-5_000), now)).toBe("just now");
  });
  it("uses minutes, hours, days", () => {
    expect(relativeTime(ago(5 * MIN), now)).toBe("5 minutes ago");
    expect(relativeTime(ago(1 * MIN), now)).toBe("1 minute ago");
    expect(relativeTime(ago(3 * HOUR), now)).toBe("3 hours ago");
    expect(relativeTime(ago(2 * DAY), now)).toBe("2 days ago");
  });
  it("falls back to a date after 30 days", () => {
    expect(relativeTime(ago(45 * DAY), now)).toMatch(/2026/);
  });
});
