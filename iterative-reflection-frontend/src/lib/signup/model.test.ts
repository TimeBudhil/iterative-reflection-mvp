import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/client";
import { ageOn, isUnder13, mapSignupError, validateSignup, type SignupValues } from "./model";

const today = new Date("2026-11-18T12:00:00Z");
const ok: SignupValues = {
  email: "a@b.co",
  password: "twelve-chars",
  displayName: "Ada",
  birthdate: "2000-01-01",
  counselorCode: "",
};

describe("ageOn", () => {
  it("counts full years", () => {
    expect(ageOn("2013-11-18", today)).toBe(13);
    expect(ageOn("2013-11-19", today)).toBe(12);
  });
});

describe("validateSignup", () => {
  it("accepts valid values", () => expect(validateSignup(ok, today)).toEqual({}));
  it("blocks an 11-char password", () => {
    expect(validateSignup({ ...ok, password: "elevenchars" }, today).password).toMatch(/12/);
  });
  it("blocks passwords over 72 bytes", () => {
    expect(validateSignup({ ...ok, password: "é".repeat(37) }, today).password).toBeDefined();
  });
  it("requires display name and email", () => {
    const e = validateSignup({ ...ok, displayName: " ", email: "nope" }, today);
    expect(e.displayName).toBeDefined();
    expect(e.email).toBeDefined();
  });
  it("blocks under 13 and future birthdates", () => {
    expect(validateSignup({ ...ok, birthdate: "2020-01-01" }, today).birthdate).toBe("You must be 13 or older to use this app.");
    expect(validateSignup({ ...ok, birthdate: "2027-01-01" }, today).birthdate).toBeDefined();
    expect(validateSignup({ ...ok, birthdate: "" }, today).birthdate).toBeDefined();
  });
});

describe("mapSignupError", () => {
  it("maps 400 errors[] to their fields", () => {
    const err = new ApiError({ status: 400, detail: "Validation failed.", fieldErrors: { email: "bad", displayName: "blank" } });
    expect(mapSignupError(err)).toEqual({ fields: { email: "bad", displayName: "blank" } });
  });
  it("maps the under-13 400 (no errors[]) to birthdate", () => {
    const err = new ApiError({ status: 400, detail: "You must be at least 13 years old to sign up." });
    expect(mapSignupError(err).fields.birthdate).toBe("You must be 13 or older to use this app.");
  });
  it("maps 409 to email and 404 to the code field", () => {
    expect(mapSignupError(new ApiError({ status: 409, detail: "x" })).fields.email).toBe("An account with this email already exists.");
    expect(mapSignupError(new ApiError({ status: 404, detail: "Unknown join code." })).fields.counselorCode).toBe("Unknown join code.");
  });
  it("falls back to a general message", () => {
    expect(mapSignupError(new ApiError({ status: 500, detail: "x" })).general).toBeDefined();
    expect(mapSignupError(new Error("boom")).general).toBeDefined();
  });
});

describe("isUnder13", () => {
  it("matches the 400 that names the age limit", () => {
    expect(isUnder13(new ApiError({ status: 400, detail: "You must be at least 13 years old to sign up." }))).toBe(true);
    expect(isUnder13(new ApiError({ status: 400, detail: "Validation failed." }))).toBe(false);
    expect(isUnder13(new ApiError({ status: 409, detail: "13" }))).toBe(false);
    expect(isUnder13(null)).toBe(false);
  });
});
