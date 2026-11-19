"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { RetryPanel } from "@/components/feedback/retry-panel";
import { Skeleton } from "@/components/ui/skeleton";
import { roleHome } from "@/lib/session/model";
import { isUnauthorized, useMe } from "@/lib/session/session";

/** `/`: signed out -> /signin, otherwise the role's home. */
export default function Root() {
  const me = useMe();
  const router = useRouter();

  useEffect(() => {
    if (me.data) router.replace(roleHome(me.data.role));
    else if (isUnauthorized(me.error)) router.replace("/signin");
  }, [me.data, me.error, router]);

  if (me.error && !isUnauthorized(me.error)) {
    return (
      <div className="p-8">
        <RetryPanel error={me.error} onRetry={() => void me.refetch()} />
      </div>
    );
  }
  return (
    <div className="p-8" aria-busy="true" aria-label="Loading">
      <Skeleton className="h-8 w-64" />
    </div>
  );
}
