import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/client";
import { statusCopy } from "@/lib/api/error-copy";
import { isValidEmail, mapPasswordChangeError, passwordLengthError, validatePasswordChange } from "./rules";

describe("isValidEmail", () => {
  it("accepts a plain address and rejects junk", () => {
    expect(isValidEmail("a@b.co")).toBe(true);
    expect(isValidEmail("nope")).toBe(false);
    expect(isValidEmail("a b@c.de")).toBe(false);
    expect(isValidEmail(`${"x".repeat(250)}@b.co`)).toBe(false);
  });
});

describe("passwordLengthError", () => {
  it("allows 12..72 bytes", () => {
    expect(passwordLengthError("x".repeat(12))).toBeUndefined();
    expect(passwordLengthError("x".repeat(72))).toBeUndefined();
  });
  it("rejects 11 characters and 73 characters", () => {
    expect(passwordLengthError("x".repeat(11))).toMatch(/12/);
    expect(passwordLengthError("x".repeat(73))).toMatch(/72/);
  });
  it("counts bytes, not characters, for the upper bound", () => {
    expect(passwordLengthError("é".repeat(37))).toMatch(/72/); // 74 bytes
    expect(passwordLengthError("é".repeat(36))).toBeUndefined(); // 72 bytes
  });
});

describe("validatePasswordChange", () => {
  const ok = { currentPassword: "temp", newPassword: "a-long-enough-pw", confirm: "a-long-enough-pw" };
  it("accepts a 12+ char matching password", () => expect(validatePasswordChange(ok)).toEqual({}));
  it("rejects a short new password and a mismatched confirm", () => {
    const e = validatePasswordChange({ ...ok, newPassword: "short", confirm: "different" });
    expect(e.newPassword).toBe("Use at least 12 characters.");
    expect(e.confirm).toBe("Passwords don't match.");
  });
  it("requires the current password", () => {
    expect(validatePasswordChange({ ...ok, currentPassword: "" }).currentPassword).toBeTruthy();
  });
});

describe("mapPasswordChangeError", () => {
  it("puts server field errors on their fields", () => {
    const err = new ApiError({ status: 400, detail: "x", fieldErrors: { newPassword: "too weak" } });
    expect(mapPasswordChangeError(err)).toEqual({ fields: { newPassword: "too weak" }, general: null });
  });
  it("treats a field-less 400 as the wrong current password", () => {
    const m = mapPasswordChangeError(new ApiError({ status: 400, detail: "x" }));
    expect(m.fields.currentPassword).toBe("Current password is incorrect.");
  });
  it("falls back to statusCopy for anything else", () => {
    expect(mapPasswordChangeError(new ApiError({ status: 500, detail: "x" })).general).toBe(statusCopy(500));
    expect(mapPasswordChangeError(new Error("boom")).general).toBe(statusCopy(0));
  });
});
