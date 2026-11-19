"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { RetryPanel } from "@/components/feedback/retry-panel";
import { setSigningOut } from "./access";
import { guardDecision, type Role } from "./model";

export type Me = {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  mustChangePassword: boolean;
  intakePromptDue: boolean;
};

/** The `/api/me` query: the single source of truth for who is signed in. A 401 surfaces as `error.status === 401`. */
export function useMe() {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: async () => (await api.GET("/api/me")).data as Me,
  });
}

export function isUnauthorized(error: unknown): boolean {
  return (error as { status?: number } | null)?.status === 401;
}

export function useSignIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: { email: string; password: string }) =>
      (await api.POST("/api/auth/signin", { body })).data as Me,
    onSuccess: (me) => qc.setQueryData(queryKeys.me, me),
  });
}

/** Sign out: POST, wipe the whole cache (so Back can't show protected data), then go to /signin. */
export function useSignOut() {
  const qc = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: async () => {
      try {
        await api.POST("/api/auth/signout", {});
      } catch (e) {
        if (!isUnauthorized(e)) throw e;
      }
    },
    onSuccess: () => {
      setSigningOut(true); // the refetches triggered by clearing the cache must not look like an expiry
      router.replace("/signin");
      qc.clear();
      setTimeout(() => setSigningOut(false), 1000);
    },
  });
}

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
  const decision = me.data ? guardDecision(me.data.role, pathname) : null;

  useEffect(() => {
    // A 401 is redirected (with `next` and the expiry notice) by SessionEvents.
    if (me.data?.mustChangePassword) router.replace("/change-password");
    else if (decision && !decision.allow) router.replace(decision.redirectTo);
  }, [me.data?.mustChangePassword, decision, router]);

  if (me.data && me.data.role === role && decision?.allow && !me.data.mustChangePassword) return <>{children(me.data)}</>;
  // 401/403 on /api/me are handled by SessionEvents (redirect / refetch); keep the skeleton meanwhile.
  if (me.error && !isUnauthorized(me.error) && (me.error as { status?: number }).status !== 403) return <RetryPanel error={me.error} onRetry={() => void me.refetch()} />;
  return <>{fallback}</>;
}
