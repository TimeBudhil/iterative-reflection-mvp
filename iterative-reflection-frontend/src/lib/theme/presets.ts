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

/** localStorage key for the last-seen preset id (a no-flash hint only; never anything auth-related). */
export const THEME_HINT_KEY = "ir-theme";

/** Stored hint to a known preset id, or null. Blocked or throwing storage is the same as no hint. */
export function readThemeHint(): PresetId | null {
  try {
    const value = window.localStorage.getItem(THEME_HINT_KEY);
    return isPresetId(value) ? value : null;
  } catch {
    return null;
  }
}

function writeThemeHint(id: PresetId): void {
  try {
    window.localStorage.setItem(THEME_HINT_KEY, id);
  } catch {
    // Storage blocked: the server value still applies for this page load.
  }
}

/**
 * Inline script for the document head: sets `data-theme` from the stored hint before first paint.
 * Accepts only known preset ids; any storage failure is swallowed. Built from PRESET_IDS so it can't drift.
 */
export const THEME_HEAD_SCRIPT = `try{var t=localStorage.getItem(${JSON.stringify(THEME_HINT_KEY)});if(${JSON.stringify(PRESET_IDS)}.indexOf(t)>-1)document.documentElement.setAttribute("data-theme",t)}catch(e){}`;

/**
 * The one place that applies a theme to the page: sets `data-theme` on `<html>` and refreshes the no-flash hint.
 * Unknown ids fall back to Grove (and the hint becomes `grove`).
 */
export function applyTheme(value: unknown): PresetId {
  const id = resolvePresetId(value);
  if (typeof document !== "undefined") {
    document.documentElement.dataset.theme = id;
    writeThemeHint(id);
  }
  return id;
}
