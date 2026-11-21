import { PRESET_IDS, PRESET_NAMES, type PresetId } from "./presets";

export const PRESET_OPTIONS = PRESET_IDS.map((id) => ({ id, name: PRESET_NAMES[id] }));

/** Save is off while nothing differs from the saved preset and while a save is in flight. */
export function canSaveTheme(selected: PresetId, saved: PresetId, pending: boolean): boolean {
  return !pending && selected !== saved;
}

const dateFormat = new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric" });

/** "Last changed by <name> on <date>" in local time; null (line hidden) when nobody has saved a theme. */
export function lastChangedLine(updatedAt: string | null | undefined, updatedByName: string | null | undefined): string | null {
  if (!updatedAt) return null;
  const when = new Date(updatedAt);
  if (Number.isNaN(when.getTime())) return null;
  return `Last changed by ${updatedByName || "an admin"} on ${dateFormat.format(when)}`;
}
