import { describe, expect, it } from "vitest";
import { counselorNameMap, EMAIL_TAKEN, mapCreateCounselorError, validateNewCounselor } from "./counselor-model";

const ok = { email: "a@b.co", displayName: "Ann", temporaryPassword: "twelve-chars!" };

describe("validateNewCounselor", () => {
  it("accepts a valid form", () => expect(validateNewCounselor(ok)).toEqual({}));
  it("flags every bad field", () => {
    const e = validateNewCounselor({ email: "nope", displayName: "  ", temporaryPassword: "short" });
    expect(Object.keys(e).sort()).toEqual(["displayName", "email", "temporaryPassword"]);
  });
  it("enforces 12..72 on the password", () => {
    expect(validateNewCounselor({ ...ok, temporaryPassword: "x".repeat(11) }).temporaryPassword).toBeDefined();
    expect(validateNewCounselor({ ...ok, temporaryPassword: "x".repeat(12) }).temporaryPassword).toBeUndefined();
    expect(validateNewCounselor({ ...ok, temporaryPassword: "x".repeat(73) }).temporaryPassword).toBeDefined();
  });
});

describe("mapCreateCounselorError", () => {
  it("puts a 409 on the email", () => expect(mapCreateCounselorError({ status: 409 }).fields.email).toBe(EMAIL_TAKEN));
  it("maps 400 field errors", () => {
    const m = mapCreateCounselorError({ status: 400, fieldErrors: { email: "bad", temporaryPassword: "short", other: "x" } });
    expect(m.fields).toEqual({ email: "bad", temporaryPassword: "short" });
  });
  it("falls back to a general message", () => {
    expect(mapCreateCounselorError({ status: 400 }).general).toBeDefined();
    expect(mapCreateCounselorError({ status: 500 }).general).toBeDefined();
  });
});

describe("counselorNameMap", () => {
  it("maps ids to names", () => expect(counselorNameMap([{ id: 2, displayName: "C" }, { id: 3 }]).get(2)).toBe("C"));
});
