"use client";

import Link from "next/link";
import { roleHome } from "@/lib/session/model";
import { useMe } from "@/lib/session/session";

/** R2.6: any unmatched path. Links to the user's home (or sign-in when signed out). */
export default function NotFound() {
  const me = useMe();
  const home = me.data ? roleHome(me.data.role) : "/signin";
  return (
    <main className="mx-auto max-w-md space-y-3 p-8">
      <h1 className="text-2xl font-semibold">Page not found</h1>
      <p className="text-muted-foreground">We couldn&apos;t find that page.</p>
      <Link href={home} className="underline" data-testid="not-found-home">
        {me.data ? "Go to your home" : "Go to sign in"}
      </Link>
    </main>
  );
}
