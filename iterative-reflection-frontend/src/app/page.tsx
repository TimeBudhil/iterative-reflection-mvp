"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Skeleton } from "@/components/ui/skeleton";
import { landingFor } from "@/lib/session/model";
import { isUnauthorized, useMe } from "@/lib/session/session";

/** `/`: signed out -> /signin, otherwise the role's home. */
export default function Root() {
  const me = useMe();
  const router = useRouter();

  useEffect(() => {
    if (me.data) router.replace(landingFor(me.data));
    else if (isUnauthorized(me.error)) router.replace("/signin");
  }, [me.data, me.error, router]);

  if (me.error && !isUnauthorized(me.error)) {
    return (
      <div className="p-8" role="alert">
        <p>Something went wrong.</p>
        <button type="button" className="mt-2 underline" onClick={() => me.refetch()}>
          Try again
        </button>
      </div>
    );
  }
  return (
    <div className="p-8" aria-busy="true" aria-label="Loading">
      <Skeleton className="h-8 w-64" />
    </div>
  );
}
