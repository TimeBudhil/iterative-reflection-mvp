/** Pure session rules: role -> home, role prefix guard, sidebar items. No React, no fetching. */

export type Role = "STUDENT" | "COUNSELOR" | "ADMIN";

const PREFIX: Record<Role, string> = { STUDENT: "/student", COUNSELOR: "/counselor", ADMIN: "/admin" };
const HOME: Record<Role, string> = {
  STUDENT: "/student/feed",
  COUNSELOR: "/counselor/students",
  ADMIN: "/admin/metrics",
};

export const roleHome = (role: Role): string => HOME[role];
export const rolePrefix = (role: Role): string => PREFIX[role];

export type GuardDecision = { allow: true } | { allow: false; redirectTo: string };

/** Whether `role` may view `pathname` (a role-prefixed route); otherwise where to send them. */
export function guardDecision(role: Role, pathname: string): GuardDecision {
  const prefix = PREFIX[role];
  return pathname === prefix || pathname.startsWith(`${prefix}/`)
    ? { allow: true }
    : { allow: false, redirectTo: HOME[role] };
}

/**
 * Where to go after sign-in. `next` is kept only if it's a plain same-site path under the role's own prefix;
 * anything else (other roles, absolute/protocol-relative URLs, backslashes, `..`, `//`) returns the role home.
 */
export function safeReturnPath(role: Role, next: string | null | undefined): string {
  if (!next || !next.startsWith("/") || next.includes("\\")) return HOME[role];
  const path = next.split(/[?#]/)[0];
  const segments = path.split("/");
  if (path.includes("//") || segments.includes("..") || segments.includes(".")) return HOME[role];
  const prefix = PREFIX[role];
  return path === prefix || path.startsWith(`${prefix}/`) ? next : HOME[role];
}

/** The sign-in URL for a 401 at `currentPath` (path + query): carries `next` and the expiry notice flag. */
export function signInHref(currentPath: string): string {
  if (currentPath === "/" || currentPath === "") return "/signin";
  return `/signin?next=${encodeURIComponent(currentPath)}&expired=1`;
}

/** After an unexpected 403 and an /api/me refetch: did who the user is change (so the guards should redirect)? */
export function identityChanged(
  before: { role: Role; mustChangePassword: boolean } | undefined,
  after: { role: Role; mustChangePassword: boolean },
): boolean {
  return !before || before.role !== after.role || before.mustChangePassword !== after.mustChangePassword;
}

export type NavItem = { label: string; href: string };

const NAV: Record<Role, NavItem[]> = {
  STUDENT: [
    { label: "Feed", href: "/student/feed" },
    { label: "My Journey", href: "/student/journey" },
    { label: "Settings", href: "/student/settings" },
  ],
  COUNSELOR: [
    { label: "Students", href: "/counselor/students" },
    { label: "Settings", href: "/counselor/settings" },
  ],
  ADMIN: [
    { label: "Metrics", href: "/admin/metrics" },
    { label: "Counselors", href: "/admin/counselors" },
    { label: "Audit log", href: "/admin/audit" },
    { label: "Settings", href: "/admin/settings" },
  ],
};

export const navFor = (role: Role): NavItem[] => NAV[role];
export const settingsHref = (role: Role): string => `${PREFIX[role]}/settings`;
