/** Pure validation for the Profile and Password cards. Mirrors the backend limits (R8.2, R8.3). */

export type NameResult = { ok: true; value: string } | { ok: false; error: string };

export function validateDisplayName(raw: string): NameResult {
  const value = raw.trim();
  if (!value) return { ok: false, error: "Enter a display name." };
  if (value.length > 100) return { ok: false, error: "Use 100 characters or fewer." };
  return { ok: true, value };
}

export type PasswordForm = { current: string; next: string; confirm: string };
export type PasswordErrors = Partial<Record<keyof PasswordForm, string>>;

/** Returns per-field messages; an empty object means the form may be submitted. */
export function validatePasswordChange({ current, next, confirm }: PasswordForm): PasswordErrors {
  const errors: PasswordErrors = {};
  if (!current) errors.current = "Enter your current password.";
  if (next.length < 12) errors.next = "Use at least 12 characters.";
  else if (next.length > 72) errors.next = "Use 72 characters or fewer.";
  if (confirm !== next) errors.confirm = "Passwords do not match.";
  return errors;
}
