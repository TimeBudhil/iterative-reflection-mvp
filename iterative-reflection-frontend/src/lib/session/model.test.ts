import { describe, expect, it } from "vitest";
import { guardDecision, landingFor, navFor, roleHome, validatePasswordChange } from "./model";

describe("roleHome", () => {
  it("maps each role to its home", () => {
    expect(roleHome("STUDENT")).toBe("/student/feed");
    expect(roleHome("COUNSELOR")).toBe("/counselor/students");
    expect(roleHome("ADMIN")).toBe("/admin/metrics");
  });
});

describe("guardDecision", () => {
  it("allows a role under its own prefix", () => {
    expect(guardDecision("STUDENT", "/student/journey")).toEqual({ allow: true });
    expect(guardDecision("ADMIN", "/admin")).toEqual({ allow: true });
  });
  it("redirects every other role/prefix pair to the user's home", () => {
    expect(guardDecision("STUDENT", "/admin/counselors")).toEqual({ allow: false, redirectTo: "/student/feed" });
    expect(guardDecision("STUDENT", "/counselor/students")).toEqual({ allow: false, redirectTo: "/student/feed" });
    expect(guardDecision("COUNSELOR", "/student/feed")).toEqual({ allow: false, redirectTo: "/counselor/students" });
    expect(guardDecision("ADMIN", "/counselor/settings")).toEqual({ allow: false, redirectTo: "/admin/metrics" });
  });
  it("does not treat a look-alike prefix as the role's own", () => {
    expect(guardDecision("STUDENT", "/students/x")).toEqual({ allow: false, redirectTo: "/student/feed" });
  });
});

describe("navFor", () => {
  it("lists the Page map sidebar items per role", () => {
    expect(navFor("STUDENT").map((n) => n.label)).toEqual(["Feed", "My Journey", "Settings"]);
    expect(navFor("COUNSELOR").map((n) => n.label)).toEqual(["Students", "Settings"]);
    expect(navFor("ADMIN").map((n) => n.label)).toEqual(["Metrics", "Counselors", "Audit log", "Settings"]);
  });
});

describe("guardDecision with mustChangePassword", () => {
  it("sends every route except /change-password there, for any role", () => {
    for (const role of ["STUDENT", "COUNSELOR", "ADMIN"] as const) {
      expect(guardDecision(role, "/change-password", true)).toEqual({ allow: true });
      expect(guardDecision(role, "/student/feed", true)).toEqual({ allow: false, redirectTo: "/change-password" });
      expect(guardDecision(role, "/admin/metrics", true)).toEqual({ allow: false, redirectTo: "/change-password" });
      expect(guardDecision(role, "/", true)).toEqual({ allow: false, redirectTo: "/change-password" });
    }
  });
  it("sends /change-password to the role home once the password is changed", () => {
    expect(guardDecision("COUNSELOR", "/change-password", false)).toEqual({ allow: false, redirectTo: "/counselor/students" });
  });
});

describe("landingFor", () => {
  it("prefers the forced change over the role home", () => {
    expect(landingFor({ role: "COUNSELOR", mustChangePassword: true })).toBe("/change-password");
    expect(landingFor({ role: "COUNSELOR", mustChangePassword: false })).toBe("/counselor/students");
  });
});

describe("validatePasswordChange", () => {
  const ok = { currentPassword: "temp", newPassword: "a-long-enough-pw", confirm: "a-long-enough-pw" };
  it("accepts a 12+ char matching password", () => expect(validatePasswordChange(ok)).toEqual({}));
  it("accepts exactly 12 chars", () => {
    const p = "x".repeat(12);
    expect(validatePasswordChange({ ...ok, newPassword: p, confirm: p })).toEqual({});
  });
  it("rejects 11 chars", () => {
    const p = "x".repeat(11);
    expect(validatePasswordChange({ ...ok, newPassword: p, confirm: p }).newPassword).toBeTruthy();
  });
  it("rejects a mismatched confirm and a blank current password", () => {
    const e = validatePasswordChange({ ...ok, currentPassword: "", confirm: "different-pw-123" });
    expect(e.confirm).toBeTruthy();
    expect(e.currentPassword).toBeTruthy();
  });
});
