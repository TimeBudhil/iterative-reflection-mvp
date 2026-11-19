/** Pure rules for the admin counselor form (R10.4) and list helpers. */

export type NewCounselor = { email: string; displayName: string; temporaryPassword: string };
export type NewCounselorField = keyof NewCounselor;
export type NewCounselorErrors = Partial<Record<NewCounselorField, string>>;

export const EMAIL_TAKEN = "An account with this email already exists.";

export function validateNewCounselor(v: NewCounselor): NewCounselorErrors {
  const errors: NewCounselorErrors = {};
  const email = v.email.trim();
  if (!email) errors.email = "Enter an email.";
  else if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = "Enter a valid email address.";

  if (!v.displayName.trim()) errors.displayName = "Enter a display name.";
  else if (v.displayName.trim().length > 100) errors.displayName = "Use 100 characters or fewer.";

  if (v.temporaryPassword.length < 12) errors.temporaryPassword = "Use at least 12 characters.";
  else if (v.temporaryPassword.length > 72 || new TextEncoder().encode(v.temporaryPassword).length > 72)
    errors.temporaryPassword = "Use 72 characters or fewer.";
  return errors;
}

export type MappedCreateError = { fields: NewCounselorErrors; general?: string };

const FIELDS: NewCounselorField[] = ["email", "displayName", "temporaryPassword"];

/** Maps a failed `POST /api/admin/counselors`: 400 per field, 409 on the email, anything else general. */
export function mapCreateCounselorError(error: unknown): MappedCreateError {
  const { status, fieldErrors } = (error ?? {}) as { status?: number; fieldErrors?: Record<string, string> };
  if (status === 409) return { fields: { email: EMAIL_TAKEN } };
  if (status === 400) {
    const fields: NewCounselorErrors = {};
    for (const f of FIELDS) if (fieldErrors?.[f]) fields[f] = fieldErrors[f];
    if (Object.keys(fields).length) return { fields };
    return { fields: {}, general: "Please check what you entered and try again." };
  }
  return { fields: {}, general: "Could not create the counselor. Please try again." };
}

/** id -> display name, for screens that show a counselor id (the audit log, ticket 21). */
export function counselorNameMap(counselors: { id?: number; displayName?: string }[]): Map<number, string> {
  const map = new Map<number, string>();
  for (const c of counselors) if (c.id !== undefined && c.displayName) map.set(c.id, c.displayName);
  return map;
}
