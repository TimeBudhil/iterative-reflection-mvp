import { describe, expect, it } from "vitest";
import { formatLinkedDate, linkErrorMessage, normalizeCode, revokeDescription } from "./model";

describe("counselor link model", () => {
  it("normalizes a pasted code", () => {
    expect(normalizeCode("  abcd 2345 ")).toBe("ABCD2345");
  });
  it("maps link statuses to the spec messages", () => {
    expect(linkErrorMessage(404)).toBe("Code not found.");
    expect(linkErrorMessage(409)).toBe("You're already linked to this counselor.");
    expect(linkErrorMessage(429)).toBe("Too many attempts. Try again in 15 minutes.");
    expect(linkErrorMessage(500)).toBe("Could not link. Please try again.");
  });
  it("words the revoke confirmation", () => {
    expect(revokeDescription("Ms. Rivera")).toBe("Ms. Rivera will no longer see your reflections.");
  });
  it("formats the linked date", () => {
    expect(formatLinkedDate("2026-11-18T12:00:00Z")).toMatch(/Nov 18, 2026/);
  });
});
