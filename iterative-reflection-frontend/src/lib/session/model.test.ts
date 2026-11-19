import { describe, expect, it } from "vitest";
import { guardDecision, navFor, roleHome } from "./model";

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
