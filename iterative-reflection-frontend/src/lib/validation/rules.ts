import { ApiError } from "@/lib/api/client";
import { mapFieldErrors, statusCopy } from "@/lib/api/error-copy";

/** Shared client-side rules that mirror the backend (R1.5, R8.3, R10.4). Pure; forms add their own copy for the rest. */

export const MIN_PASSWORD_LENGTH = 12;
/** bcrypt's limit: the backend counts bytes, so a 37-character "é" password is already too long. */
export const MAX_PASSWORD_BYTES = 72;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** A syntactically plausible email (the caller trims and checks "blank" first, with its own copy). */
export function isValidEmail(email: string): boolean {
  return email.length <= 254 && EMAIL_PATTERN.test(email);
}

/** The copy for a password outside 12..72 (bytes), or undefined when the length is fine. */
export function passwordLengthError(password: string): string | undefined {
  if (password.length < MIN_PASSWORD_LENGTH) return `Use at least ${MIN_PASSWORD_LENGTH} characters.`;
  if (new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES) return `Use ${MAX_PASSWORD_BYTES} characters or fewer.`;
  return undefined;
}

export const PASSWORDS_DONT_MATCH = "Passwords don't match.";

export type PasswordFormValues = { currentPassword: string; newPassword: string; confirm: string };
export type PasswordFormErrors = Partial<Record<keyof PasswordFormValues, string>>;

/** Client-side checks for every "change my password" form (the backend re-validates). Empty object = valid. */
export function validatePasswordChange(v: PasswordFormValues): PasswordFormErrors {
  const errors: PasswordFormErrors = {};
  if (!v.currentPassword) errors.currentPassword = "Enter your current password.";
  const length = passwordLengthError(v.newPassword);
  if (length) errors.newPassword = length;
  if (v.confirm !== v.newPassword) errors.confirm = PASSWORDS_DONT_MATCH;
  return errors;
}

/**
 * Maps a failed `PUT /api/me/password` onto the form: per-field server messages by name; a field-less 400 is the
 * wrong current password; anything else is a general message from `statusCopy`.
 */
export function mapPasswordChangeError(error: unknown): { fields: PasswordFormErrors; general: string | null } {
  if (error instanceof ApiError && error.status === 400) {
    const fields: PasswordFormErrors = mapFieldErrors(error, ["newPassword", "currentPassword"]);
    if (Object.keys(fields).length === 0) fields.currentPassword = "Current password is incorrect.";
    return { fields, general: null };
  }
  return { fields: {}, general: statusCopy(error instanceof ApiError ? error.status : 0) };
}
