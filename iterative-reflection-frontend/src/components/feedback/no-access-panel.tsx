"use client";

import { setAccessDenied } from "@/lib/session/access";
import Link from "next/link";

/** R2.7: shown when an unexpected 403 didn't change who the user is. `homeHref` is the user's role home. */
export function NoAccessPanel({ homeHref }: { homeHref: string }) {
  return (
    <div role="alert" className="max-w-md space-y-3 rounded-lg border p-6" data-testid="no-access">
      <p className="font-medium">You don&apos;t have access to this.</p>
      <Link href={homeHref} onClick={() => setAccessDenied(null)} className="text-sm underline">
        Go to your home
      </Link>
    </div>
  );
}
