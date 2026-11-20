import { afterEach, describe, expect, it, vi } from "vitest";
import { applyTheme, DEFAULT_PRESET_ID, readThemeHint, resolvePresetId, THEME_HEAD_SCRIPT, THEME_HINT_KEY } from "./presets";

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
  afterEach(() => {
    document.documentElement.removeAttribute("data-theme");
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it("sets data-theme on <html>", () => {
    expect(applyTheme("plum")).toBe("plum");
    expect(document.documentElement.dataset.theme).toBe("plum");
  });

  it("applies grove for an unknown id", () => {
    expect(applyTheme("neon")).toBe("grove");
    expect(document.documentElement.dataset.theme).toBe("grove");
  });

  it("stores the applied id as the hint, and an unknown id becomes grove", () => {
    applyTheme("plum");
    expect(window.localStorage.getItem(THEME_HINT_KEY)).toBe("plum");
    applyTheme("neon");
    expect(readThemeHint()).toBe("grove");
  });

  it("ignores an unknown stored hint", () => {
    window.localStorage.setItem(THEME_HINT_KEY, "neon");
    expect(readThemeHint()).toBeNull();
  });

  it("does not throw when storage throws", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    expect(readThemeHint()).toBeNull();
    expect(applyTheme("harbor")).toBe("harbor");
    expect(document.documentElement.dataset.theme).toBe("harbor");
  });
});

describe("THEME_HEAD_SCRIPT", () => {
  afterEach(() => {
    document.documentElement.removeAttribute("data-theme");
    window.localStorage.clear();
  });

  it("applies a known hint and ignores an unknown one", () => {
    window.localStorage.setItem(THEME_HINT_KEY, "slate");
    new Function(THEME_HEAD_SCRIPT)();
    expect(document.documentElement.dataset.theme).toBe("slate");
    document.documentElement.removeAttribute("data-theme");
    window.localStorage.setItem(THEME_HINT_KEY, "<b>");
    new Function(THEME_HEAD_SCRIPT)();
    expect(document.documentElement.dataset.theme).toBeUndefined();
  });
});
