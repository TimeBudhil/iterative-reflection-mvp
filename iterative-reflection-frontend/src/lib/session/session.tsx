"use client";

import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { setSigningOut } from "./access";
import type { Role } from "./model";

export type Me = {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  mustChangePassword: boolean;
  intakePromptDue: boolean;
};

/** `GET /api/me`: the one place the current user is fetched (the query below, the 403 refetch, the password refetch). */
export async function fetchMe(): Promise<Me> {
  return (await api.GET("/api/me")).data as Me;
}

/** The `/api/me` query: the single source of truth for who is signed in. A 401 surfaces as `error.status === 401`. */
export function useMe() {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: fetchMe,
  });
}

export function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
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

/** `PUT /api/me/password`, then refetch `/api/me` so the guards see `mustChangePassword: false`. Resolves to the fresh user. */
export function useChangePassword() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: { currentPassword: string; newPassword: string }) => {
      await api.PUT("/api/me/password", { body });
      return await qc.fetchQuery({
        queryKey: queryKeys.me,
        queryFn: fetchMe,
        staleTime: 0,
      });
    },
  });
}
