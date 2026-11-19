"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import type { Me } from "@/lib/session/session";

/** `PATCH /api/me`. The response replaces the cached `/api/me`, so the user menu updates without a reload. */
export function useUpdateDisplayName() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (displayName: string) => (await api.PATCH("/api/me", { body: { displayName } })).data as Me,
    onSuccess: (me) => qc.setQueryData(queryKeys.me, me),
  });
}
