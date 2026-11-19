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
