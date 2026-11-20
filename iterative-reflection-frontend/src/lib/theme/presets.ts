/** The curated theme presets. Colors live in globals.css; the backend stores only the id. */
export const PRESET_IDS = ["grove", "harbor", "plum", "slate"] as const;
export type PresetId = (typeof PRESET_IDS)[number];

export const DEFAULT_PRESET_ID: PresetId = "grove";

export function isPresetId(value: unknown): value is PresetId {
  return typeof value === "string" && (PRESET_IDS as readonly string[]).includes(value);
}

/** Any value to a known preset id; anything unknown (or missing) is Grove. A theme problem is never an error. */
export function resolvePresetId(value: unknown): PresetId {
  return isPresetId(value) ? value : DEFAULT_PRESET_ID;
}

/**
 * The one place that applies a theme to the page: sets `data-theme` on `<html>`. Unknown ids fall back to Grove.
 * (Update 2 ticket 04 extends this to also keep the no-flash localStorage hint.)
 */
export function applyTheme(value: unknown): PresetId {
  const id = resolvePresetId(value);
  if (typeof document !== "undefined") document.documentElement.dataset.theme = id;
  return id;
}
