"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";

export const ACTIVITY_PAGE_SIZE = 20;

/** `GET /api/admin/activity` (newest first). */
export function useAdminActivity(page: number) {
  return useQuery({
    queryKey: queryKeys.adminActivity(page),
    queryFn: async () =>
      (await api.GET("/api/admin/activity", { params: { query: { pageable: { page, size: ACTIVITY_PAGE_SIZE } } } })).data!,
    // a log that must show what the admin just did, so never serve it stale
    staleTime: 0,
    placeholderData: keepPreviousData,
  });
}
