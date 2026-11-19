"use client";

import { useSyncExternalStore } from "react";

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
