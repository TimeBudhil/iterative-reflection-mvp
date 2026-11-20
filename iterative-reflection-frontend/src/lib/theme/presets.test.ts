import { afterEach, describe, expect, it } from "vitest";
import { applyTheme, DEFAULT_PRESET_ID, resolvePresetId } from "./presets";

describe("resolvePresetId", () => {
  it("keeps the four known presets", () => {
    for (const id of ["grove", "harbor", "plum", "slate"]) expect(resolvePresetId(id)).toBe(id);
  });

  it("falls back to grove for anything else", () => {
    for (const bad of ["neon", "", "GROVE", null, undefined, 3, {}]) expect(resolvePresetId(bad)).toBe("grove");
    expect(DEFAULT_PRESET_ID).toBe("grove");
  });
});

describe("applyTheme", () => {
  afterEach(() => document.documentElement.removeAttribute("data-theme"));

  it("sets data-theme on <html>", () => {
    expect(applyTheme("plum")).toBe("plum");
    expect(document.documentElement.dataset.theme).toBe("plum");
  });

  it("applies grove for an unknown id", () => {
    expect(applyTheme("neon")).toBe("grove");
    expect(document.documentElement.dataset.theme).toBe("grove");
  });
});
