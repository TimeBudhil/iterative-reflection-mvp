import { describe, expect, it } from "vitest";
import { guardDecision, identityChanged, landingFor, postSignInPath, navFor, roleHome, safeReturnPath, signInHref } from "./model";

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

describe("safeReturnPath", () => {
  it("keeps same-role paths, including query and hash", () => {
    expect(safeReturnPath("STUDENT", "/student/journey")).toBe("/student/journey");
    expect(safeReturnPath("COUNSELOR", "/counselor/students/7?tab=x#y")).toBe("/counselor/students/7?tab=x#y");
  });
  it("falls back to the role home for other roles, foreign origins and junk", () => {
    for (const next of [
      "/admin/metrics",
      "/counselor/students",
      "/students/x",
      "https://evil.com",
      "//evil.com",
      "/\\evil.com",
      "/student/../admin/metrics",
      "/student//evil.com",
      "javascript:alert(1)",
      "student/feed",
      "",
      null,
      undefined,
    ]) {
      expect(safeReturnPath("STUDENT", next)).toBe("/student/feed");
    }
  });
});

describe("signInHref", () => {
  it("carries the current path and the expiry flag", () => {
    expect(signInHref("/student/journey?x=1")).toBe("/signin?next=%2Fstudent%2Fjourney%3Fx%3D1&expired=1");
  });
  it("omits next and the notice for the root", () => {
    expect(signInHref("/")).toBe("/signin");
  });
});

describe("identityChanged", () => {
  const student = { role: "STUDENT" as const, mustChangePassword: false };
  it("is true when the role or mustChangePassword differs", () => {
    expect(identityChanged(student, { ...student, role: "ADMIN" })).toBe(true);
    expect(identityChanged(student, { ...student, mustChangePassword: true })).toBe(true);
  });
  it("is false when nothing changed", () => {
    expect(identityChanged(student, { ...student })).toBe(false);
  });
  it("is true when there was nothing to compare against", () => {
    expect(identityChanged(undefined, student)).toBe(true);
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

describe("postSignInPath", () => {
  it("mustChangePassword wins over next", () => {
    expect(postSignInPath({ role: "STUDENT", mustChangePassword: true }, "/student/feed")).toBe("/change-password");
  });
  it("honours a safe next, else role home", () => {
    expect(postSignInPath({ role: "STUDENT", mustChangePassword: false }, "/student/careers/1")).toBe("/student/careers/1");
    expect(postSignInPath({ role: "STUDENT", mustChangePassword: false }, "/admin/x")).toBe("/student/feed");
  });
});
