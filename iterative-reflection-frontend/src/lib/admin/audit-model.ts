/** Pure helpers for the admin audit log (R10.6). */

export function counselorLabel(counselorId: number | undefined, names: Map<number, string>): string {
  if (counselorId === undefined) return "–";
  return names.get(counselorId) ?? `#${counselorId}`;
}

export function outcomeLabel(outcome: string | undefined): string {
  return outcome === "ALLOWED" ? "Allowed" : outcome === "DENIED" ? "Denied" : (outcome ?? "–");
}

export type StudentIdFilter = { ok: true; id: number | undefined } | { ok: false; error: string };

/** Blank means no filter; otherwise a positive integer (a leading `#` is tolerated). */
export function parseStudentIdFilter(text: string): StudentIdFilter {
  const t = text.trim().replace(/^#/, "");
  if (t === "") return { ok: true, id: undefined };
  if (!/^\d+$/.test(t) || Number(t) < 1) return { ok: false, error: "Enter a student ID (a whole number)." };
  return { ok: true, id: Number(t) };
}
