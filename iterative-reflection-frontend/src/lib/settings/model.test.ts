import { describe, expect, it } from "vitest";
import { validateDisplayName } from "./model";

describe("validateDisplayName", () => {
  it("rejects blank and over-long names, trims valid ones", () => {
    expect(validateDisplayName("   ")).toEqual({ ok: false, error: "Enter a display name." });
    expect(validateDisplayName("x".repeat(101))).toEqual({ ok: false, error: "Use 100 characters or fewer." });
    expect(validateDisplayName("  Ada ")).toEqual({ ok: true, value: "Ada" });
  });
});
