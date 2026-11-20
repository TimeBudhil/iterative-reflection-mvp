import { describe, expect, it } from "vitest";
import { PRESET_NAMES } from "./presets";
import { canSaveTheme, lastChangedLine, PRESET_OPTIONS } from "./theme-card-model";

describe("PRESET_OPTIONS", () => {
  it("offers the four presets by display name, Grove first", () => {
    expect(PRESET_OPTIONS.map((o) => o.name)).toEqual(["Grove", "Harbor", "Plum", "Slate"]);
    expect(PRESET_NAMES.harbor).toBe("Harbor");
  });
});

describe("canSaveTheme", () => {
  it("is off when the selection equals the saved preset", () => {
    expect(canSaveTheme("grove", "grove", false)).toBe(false);
  });
  it("is off while a save is pending", () => {
    expect(canSaveTheme("harbor", "grove", true)).toBe(false);
  });
  it("is on for a different preset and no pending save", () => {
    expect(canSaveTheme("harbor", "grove", false)).toBe(true);
  });
});

describe("lastChangedLine", () => {
  it("is hidden when nothing was ever saved", () => {
    expect(lastChangedLine(null, null)).toBeNull();
    expect(lastChangedLine(undefined, "Ada")).toBeNull();
  });
  it("names the admin and the local date", () => {
    const iso = "2026-11-20T15:30:00Z";
    const date = new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric" }).format(new Date(iso));
    expect(lastChangedLine(iso, "Ada Admin")).toBe(`Last changed by Ada Admin on ${date}`);
  });
  it("ignores an unparseable date", () => {
    expect(lastChangedLine("not a date", "Ada")).toBeNull();
  });
});
