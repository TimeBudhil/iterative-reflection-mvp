/** Pure helpers for the admin activity tab (R13.9). */
import { PRESET_NAMES } from "@/lib/theme/theme-card-model";
import { isPresetId } from "@/lib/theme/presets";

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

const ACTION_LABELS: Record<string, string> = {
  THEME_CHANGED: "Theme changed",
  COUNSELOR_CREATED: "Counselor created",
  COUNSELOR_DEACTIVATED: "Counselor deactivated",
  COUNSELOR_REACTIVATED: "Counselor reactivated",
};

export function actionLabel(action: string | undefined): string {
  return (action && ACTION_LABELS[action]) || action || "–";
}

function presetName(id: string | undefined): string {
  return isPresetId(id) ? PRESET_NAMES[id] : (id ?? "?");
}

/** "Theme: Grove → Harbor", "Created counselor <email>", and so on. */
export function detailText(action: string | undefined, detail: { [key: string]: string } | undefined): string {
  const d = detail ?? {};
  switch (action) {
    case "THEME_CHANGED":
      return `Theme: ${presetName(d.from)} → ${presetName(d.to)}`;
    case "COUNSELOR_CREATED":
      return `Created counselor ${d.email ?? "?"}`;
    case "COUNSELOR_DEACTIVATED":
      return `Deactivated counselor ${d.email ?? "?"}`;
    case "COUNSELOR_REACTIVATED":
      return `Reactivated counselor ${d.email ?? "?"}`;
    default:
      return Object.values(d).join(", ") || "–";
  }
}
