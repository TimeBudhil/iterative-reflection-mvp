import { describe, expect, it } from "vitest";
import { validateDisplayName, validatePasswordChange } from "./model";

describe("validateDisplayName", () => {
  it("rejects blank and over-long names, trims valid ones", () => {
    expect(validateDisplayName("   ")).toEqual({ ok: false, error: "Enter a display name." });
    expect(validateDisplayName("x".repeat(101))).toEqual({ ok: false, error: "Use 100 characters or fewer." });
    expect(validateDisplayName("  Ada ")).toEqual({ ok: true, value: "Ada" });
  });
});

describe("validatePasswordChange", () => {
  const ok = { current: "old-password", next: "a-new-password", confirm: "a-new-password" };
  it("accepts a valid change", () => {
    expect(validatePasswordChange(ok)).toEqual({});
  });
  it("requires the current password", () => {
    expect(validatePasswordChange({ ...ok, current: "" }).current).toBeDefined();
  });
  it("requires 12 to 72 characters for the new password", () => {
    expect(validatePasswordChange({ ...ok, next: "short", confirm: "short" }).next).toMatch(/12/);
    const long = "x".repeat(73);
    expect(validatePasswordChange({ ...ok, next: long, confirm: long }).next).toMatch(/72/);
  });
  it("blocks mismatched confirmation", () => {
    expect(validatePasswordChange({ ...ok, confirm: "different-password" }).confirm).toBe("Passwords do not match.");
  });
});
