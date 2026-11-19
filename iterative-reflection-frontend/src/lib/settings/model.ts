/** Pure validation for the Profile card. Mirrors the backend limit (R8.2); password rules live in lib/validation/rules.ts. */

export type NameResult = { ok: true; value: string } | { ok: false; error: string };

export function validateDisplayName(raw: string): NameResult {
  const value = raw.trim();
  if (!value) return { ok: false, error: "Enter a display name." };
  if (value.length > 100) return { ok: false, error: "Use 100 characters or fewer." };
  return { ok: true, value };
}
