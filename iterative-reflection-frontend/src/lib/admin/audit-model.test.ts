import { describe, expect, it } from "vitest";
import { counselorLabel, outcomeLabel, parseStudentIdFilter } from "./audit-model";

describe("counselorLabel", () => {
  const names = new Map([[2, "Demo Counselor"]]);
  it("uses the resolved name", () => expect(counselorLabel(2, names)).toBe("Demo Counselor"));
  it("falls back to #id", () => expect(counselorLabel(9, names)).toBe("#9"));
});

describe("outcomeLabel", () => {
  it("maps ALLOWED and DENIED", () => {
    expect(outcomeLabel("ALLOWED")).toBe("Allowed");
    expect(outcomeLabel("DENIED")).toBe("Denied");
  });
});

describe("parseStudentIdFilter", () => {
  it("accepts a positive integer, trimmed, and an optional #", () => {
    expect(parseStudentIdFilter(" 12 ")).toEqual({ ok: true, id: 12 });
    expect(parseStudentIdFilter("#7")).toEqual({ ok: true, id: 7 });
  });
  it("treats blank as no filter", () => expect(parseStudentIdFilter("  ")).toEqual({ ok: true, id: undefined }));
  it("rejects non-numeric input", () => {
    expect(parseStudentIdFilter("abc").ok).toBe(false);
    expect(parseStudentIdFilter("1.5").ok).toBe(false);
    expect(parseStudentIdFilter("-3").ok).toBe(false);
  });
});
