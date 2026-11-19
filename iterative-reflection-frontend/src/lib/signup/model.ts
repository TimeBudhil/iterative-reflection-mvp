import { EMAIL_TAKEN, mapFieldErrors } from "@/lib/api/error-copy";
import { isValidEmail, passwordLengthError } from "@/lib/validation/rules";

/** Pure signup rules: client-side validation (mirrors the backend) and error-to-field mapping. */

export const TERMS_VERSION = process.env.NEXT_PUBLIC_TERMS_VERSION || "2026-11-18";

export type SignupValues = {
  email: string;
  password: string;
  displayName: string;
  birthdate: string; // YYYY-MM-DD
  counselorCode: string;
};

export type SignupField = keyof SignupValues;
export type FieldErrors = Partial<Record<SignupField, string>>;

export const UNDER_13 = "You must be 13 or older to use this app.";

/** Whole years between `birthdate` (YYYY-MM-DD) and `today`, in UTC. */
export function ageOn(birthdate: string, today: Date): number {
  const [y, m, d] = birthdate.split("-").map(Number);
  let age = today.getUTCFullYear() - y;
  const before = today.getUTCMonth() + 1 < m || (today.getUTCMonth() + 1 === m && today.getUTCDate() < d);
  if (before) age -= 1;
  return age;
}

export function validateSignup(v: SignupValues, today: Date = new Date()): FieldErrors {
  const errors: FieldErrors = {};
  const email = v.email.trim();
  if (!email) errors.email = "Enter your email.";
  else if (!isValidEmail(email)) errors.email = "Enter a valid email address.";

  const passwordError = passwordLengthError(v.password);
  if (passwordError) errors.password = passwordError;

  if (!v.displayName.trim()) errors.displayName = "Enter a display name.";
  else if (v.displayName.length > 100) errors.displayName = "Display name must be at most 100 characters.";

  if (!/^\d{4}-\d{2}-\d{2}$/.test(v.birthdate)) errors.birthdate = "Enter your birthdate.";
  else {
    const age = ageOn(v.birthdate, today);
    if (Number.isNaN(age) || new Date(`${v.birthdate}T00:00:00Z`) >= today) errors.birthdate = "Birthdate must be in the past.";
    else if (age < 13) errors.birthdate = UNDER_13;
  }

  if (v.counselorCode.trim().length > 32) errors.counselorCode = "Counselor code is too long.";
  return errors;
}

export type MappedSignupError = { fields: FieldErrors; general?: string };

const SIGNUP_FIELDS: SignupField[] = ["email", "password", "displayName", "birthdate", "counselorCode"];

/**
 * Map a failed `POST /api/auth/signup` to form copy. The backend's under-13 400 and bad-code 404 carry no
 * field, so they are told apart by status and request context (see notes/backend-api.md).
 */
export function mapSignupError(error: unknown): MappedSignupError {
  const e = error as { status?: number; detail?: string; fieldErrors?: Record<string, string> } | null;
  const generic = { fields: {}, general: "Something went wrong. Please try again." };
  if (!e || typeof e.status !== "number") return generic;
  if (e.status === 400) {
    const fields: FieldErrors = mapFieldErrors(error, SIGNUP_FIELDS);
    if (Object.keys(fields).length > 0) return { fields };
    if (/13/.test(e.detail ?? "")) return { fields: { birthdate: UNDER_13 } };
    return generic;
  }
  if (e.status === 409) return { fields: { email: EMAIL_TAKEN } };
  if (e.status === 404) return { fields: { counselorCode: "Unknown join code." } };
  return generic;
}
