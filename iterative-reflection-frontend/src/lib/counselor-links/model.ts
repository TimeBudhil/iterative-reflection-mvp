import { TOO_MANY_ATTEMPTS } from "@/lib/api/error-copy";

/** Pure rules for the My counselors card (R8.4 to R8.6). */

/** Join codes are case-insensitive; strip whitespace a user may paste in. */
export function normalizeCode(raw: string): string {
  return raw.replace(/\s+/g, "").toUpperCase();
}

/** Maps by status: the backend's own messages differ from the spec wording. */
export function linkErrorMessage(status: number): string {
  switch (status) {
    case 404:
      return "Code not found.";
    case 409:
      return "You're already linked to this counselor.";
    case 429:
      return TOO_MANY_ATTEMPTS;
    default:
      return "Could not link. Please try again.";
  }
}

export function revokeDescription(name: string): string {
  return `${name} will no longer see your reflections.`;
}

export function formatLinkedDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric", timeZone: "UTC" });
}
