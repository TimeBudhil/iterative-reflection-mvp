/** Pure helpers for the admin activity tab (R13.9). */
import { isPresetId, PRESET_NAMES } from "@/lib/theme/presets";

export type ActivityTab = "views" | "activity";

/** `?tab=activity` selects the activity tab; anything else (or nothing) is the counselor views. */
export function tabFromParam(value: string | null | undefined): ActivityTab {
  return value === "activity" ? "activity" : "views";
}

/** The URL search for a tab, keeping any other params; the default tab leaves no `tab` param. */
export function tabToSearch(tab: ActivityTab, current: URLSearchParams | string = ""): string {
  const params = new URLSearchParams(typeof current === "string" ? current : current.toString());
  if (tab === "activity") params.set("tab", "activity");
  else params.delete("tab");
  const s = params.toString();
  return s ? `?${s}` : "";
}

type Detail = { [key: string]: string };

function presetName(id: string | undefined): string {
  return isPresetId(id) ? PRESET_NAMES[id] : (id ?? "?");
}

/** Per action: the tab's label and the one-line detail ("Theme: Grove → Harbor", "Created counselor <email>"). */
const ACTIONS: Record<string, { label: string; detail: (d: Detail) => string }> = {
  THEME_CHANGED: { label: "Theme changed", detail: (d) => `Theme: ${presetName(d.from)} → ${presetName(d.to)}` },
  COUNSELOR_CREATED: { label: "Counselor created", detail: (d) => `Created counselor ${d.email ?? "?"}` },
  COUNSELOR_DEACTIVATED: { label: "Counselor deactivated", detail: (d) => `Deactivated counselor ${d.email ?? "?"}` },
  COUNSELOR_REACTIVATED: { label: "Counselor reactivated", detail: (d) => `Reactivated counselor ${d.email ?? "?"}` },
};

export function actionLabel(action: string | undefined): string {
  return (action && ACTIONS[action]?.label) || action || "–";
}

export function detailText(action: string | undefined, detail: Detail | undefined): string {
  const d = detail ?? {};
  const known = action ? ACTIONS[action] : undefined;
  return known ? known.detail(d) : Object.values(d).join(", ") || "–";
}
