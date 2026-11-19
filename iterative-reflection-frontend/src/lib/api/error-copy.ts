import { ApiError } from "./client";

export const TOO_MANY_ATTEMPTS = "Too many attempts. Try again in 15 minutes.";
export const EMAIL_TAKEN = "An account with this email already exists.";

/**
 * One written message per backend status (N5.2), so raw JSON or stack text never reaches the UI.
 * Pages with more specific copy (R1-R10) use their own text and fall back to this.
 */
export function statusCopy(status: number): string {
  if (status === 0) return "We couldn't reach the server. Check your connection and try again.";
  if (status >= 500) return "Something went wrong on our side. Please try again.";
  switch (status) {
    case 400:
      return "Please check what you entered and try again.";
    case 401:
      return "Your session expired. Please sign in again.";
    case 403:
      return "You don't have access to this.";
    case 404:
      return "We couldn't find that.";
    case 409:
      return "That can't be done right now.";
    case 429:
      return TOO_MANY_ATTEMPTS;
    default:
      return "Something went wrong. Please try again.";
  }
}

/** Whether an error deserves the retry panel: a 5xx or a network failure (status 0). */
export function isRetryable(error: unknown): boolean {
  const status = (error as { status?: unknown } | null)?.status;
  return typeof status === "number" && (status === 0 || status >= 500);
}

/**
 * The server's per-field validation messages (R1.5: each `errors[]` entry under its input) for a 400, limited to
 * the form's own `fields`. Returns `{}` for anything that isn't a 400 `ApiError` or names none of them.
 */
export function mapFieldErrors<F extends string>(error: unknown, fields: readonly F[]): Partial<Record<F, string>> {
  const mapped: Partial<Record<F, string>> = {};
  if (!(error instanceof ApiError) || error.status !== 400) return mapped;
  for (const field of fields) {
    const message = error.fieldErrors[field];
    if (message) mapped[field] = message;
  }
  return mapped;
}
