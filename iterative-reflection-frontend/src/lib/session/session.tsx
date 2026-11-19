"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
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
      qc.clear();
      router.replace("/signin");
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
    if (isUnauthorized(me.error)) router.replace("/signin");
    else if (decision && !decision.allow) router.replace(decision.redirectTo);
  }, [me.error, decision, router]);

  if (me.data && me.data.role === role && decision?.allow) return <>{children(me.data)}</>;
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
  return <>{fallback}</>;
}
