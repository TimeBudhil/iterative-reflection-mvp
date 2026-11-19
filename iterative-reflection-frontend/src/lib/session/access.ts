"use client";

import { useEffect, useSyncExternalStore } from "react";

/**
 * Tiny store for "an unexpected 403 left the user where they were" (R2.7). Holds the path it happened on, so
 * navigating anywhere else clears the panel without any extra bookkeeping.
 */
let deniedPath: string | null = null;
const listeners = new Set<() => void>();

export function setAccessDenied(path: string | null) {
  deniedPath = path;
  listeners.forEach((l) => l());
}

export function useAccessDenied(pathname: string): boolean {
  const current = useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => deniedPath,
    () => null,
  );
  return current === pathname;
}

/** Set while a deliberate sign-out is clearing the cache, so the resulting 401s are not reported as an expiry. */
let signingOut = false;
export const setSigningOut = (v: boolean) => {
  signingOut = v;
};
export const isSigningOut = () => signingOut;

/**
 * A page that renders its own copy for a 403 (e.g. R9.5, a counselor opening a student who isn't theirs) calls
 * this hook while mounted; the global 403 handler then skips the generic no-access panel. The session module
 * never learns which routes those are.
 */
let ownForbiddenCopy = 0;
export function useOwnForbiddenCopy() {
  useEffect(() => {
    ownForbiddenCopy += 1;
    return () => {
      ownForbiddenCopy -= 1;
    };
  }, []);
}
export const hasOwnForbiddenCopy = () => ownForbiddenCopy > 0;
