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

export const CHANGE_PASSWORD_PATH = "/change-password";

/** Where a freshly known user lands: the forced password change, else their role home. */
export const landingFor = (me: { role: Role; mustChangePassword: boolean }): string =>
  me.mustChangePassword ? CHANGE_PASSWORD_PATH : HOME[me.role];

/**
 * Whether the user may view `pathname`; otherwise where to send them. While `mustChangePassword`, only
 * `/change-password` is allowed (sign-out is an action, not a route). Otherwise `role` must own the route prefix.
 */
export function guardDecision(role: Role, pathname: string, mustChangePassword = false): GuardDecision {
  if (mustChangePassword) {
    return pathname === CHANGE_PASSWORD_PATH ? { allow: true } : { allow: false, redirectTo: CHANGE_PASSWORD_PATH };
  }
  const prefix = PREFIX[role];
  return pathname === prefix || pathname.startsWith(`${prefix}/`)
    ? { allow: true }
    : { allow: false, redirectTo: HOME[role] };
}

export const MIN_PASSWORD_LENGTH = 12;

export type PasswordFormValues = { currentPassword: string; newPassword: string; confirm: string };
export type PasswordFormErrors = Partial<Record<keyof PasswordFormValues, string>>;

/** Client-side checks for the change-password form (the backend re-validates). Empty object = valid. */
export function validatePasswordChange(v: PasswordFormValues): PasswordFormErrors {
  const errors: PasswordFormErrors = {};
  if (!v.currentPassword) errors.currentPassword = "Enter your current password.";
  if (v.newPassword.length < MIN_PASSWORD_LENGTH) {
    errors.newPassword = `Use at least ${MIN_PASSWORD_LENGTH} characters.`;
  } else if (new TextEncoder().encode(v.newPassword).length > 72) {
    errors.newPassword = "Use at most 72 bytes.";
  }
  if (v.confirm !== v.newPassword) errors.confirm = "Passwords don't match.";
  return errors;
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
