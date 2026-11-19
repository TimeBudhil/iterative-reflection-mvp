"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import type { NewCounselor } from "./counselor-model";

export const COUNSELORS_PAGE_SIZE = 20;

/**
 * `GET /api/admin/counselors` (newest first). Reusable: ticket 21 (audit log) resolves counselor ids to names
 * from this list (pass a large `size`, the backend clamps at 100). Flat `page`/`size` query: see DECISIONS.md.
 */
export function useAdminCounselors(page: number, size: number = COUNSELORS_PAGE_SIZE) {
  return useQuery({
    queryKey: queryKeys.adminCounselors(page, size),
    queryFn: async () =>
      (
        await api.GET("/api/admin/counselors", {
          params: { query: { pageable: { page, size } } },
        })
      ).data!,
    placeholderData: keepPreviousData,
  });
}

export function useCreateCounselor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: NewCounselor) => (await api.POST("/api/admin/counselors", { body })).data!,
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.adminCounselorsAll }),
  });
}

export function useSetCounselorActive() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, active }: { id: number; active: boolean }) =>
      (await api.PATCH("/api/admin/counselors/{id}", { params: { path: { id } }, body: { active } })).data!,
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.adminCounselorsAll }),
  });
}
