"use client";

import { useEffect } from "react";
import { useMe } from "@/lib/session/session";
import { PasswordCard } from "./password-card";
import { ProfileCard } from "./profile-card";

/**
 * Shared settings page: Profile and Password for every role. Role-specific cards (student: My counselors,
 * Interests) are passed as `children` and render after Password, in the order given.
 */
export function SettingsPage({ children }: { children?: React.ReactNode }) {
  const { data: me } = useMe();

  // The cards mount after the session loads, so the browser's own #anchor scroll misses them.
  useEffect(() => {
    if (me && window.location.hash) document.getElementById(window.location.hash.slice(1))?.scrollIntoView();
  }, [me]);

  if (!me) return null; // the role layout's guard has already confirmed the user
  return (
    <div className="max-w-2xl space-y-6">
      <h1 className="text-2xl font-semibold">Settings</h1>
      <ProfileCard me={me} />
      <PasswordCard />
      {children}
    </div>
  );
}
