"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { RetryPanel } from "@/components/shared/retry-panel";
import { ApiError } from "@/lib/api/client";
import { guardDecision, type Role } from "@/lib/session/model";
import { isUnauthorized, useMe, type Me } from "@/lib/session/session";

/**
 * Renders `fallback` (a skeleton) until the user is confirmed to hold `role`; otherwise redirects
 * (signed out -> /signin, wrong role -> that role's home). Children are never rendered before confirmation.
 */
export function RoleGuard({
  role,
  fallback,
  children,
}: {
  role: Role;
  fallback: React.ReactNode;
  children: (me: Me) => React.ReactNode;
}) {
  const me = useMe();
  const router = useRouter();
  const pathname = usePathname();
  const decision = me.data ? guardDecision(me.data.role, pathname, me.data.mustChangePassword) : null;

  useEffect(() => {
    // A 401 is redirected (with `next` and the expiry notice) by SessionEvents.
    if (me.data?.mustChangePassword) router.replace("/change-password");
    else if (decision && !decision.allow) router.replace(decision.redirectTo);
  }, [me.data?.mustChangePassword, decision, router]);

  if (me.data && me.data.role === role && decision?.allow && !me.data.mustChangePassword) return <>{children(me.data)}</>;
  // 401/403 on /api/me are handled by SessionEvents (redirect / refetch); keep the skeleton meanwhile.
  if (me.error && !isUnauthorized(me.error) && !(me.error instanceof ApiError && me.error.status === 403)) return <RetryPanel error={me.error} onRetry={() => void me.refetch()} />;
  return <>{fallback}</>;
}
