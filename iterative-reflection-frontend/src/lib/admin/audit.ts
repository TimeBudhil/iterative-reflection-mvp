"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";

export const AUDIT_PAGE_SIZE = 20;

/** `GET /api/admin/audit` (newest first), optionally for one student. Flat query: see DECISIONS.md. */
export function useAuditLog(page: number, studentId?: number) {
  return useQuery({
    queryKey: queryKeys.adminAudit(page, studentId),
    queryFn: async () =>
      (
        await api.GET("/api/admin/audit", {
          params: { query: { studentId, pageable: { page, size: AUDIT_PAGE_SIZE } } },
        })
      ).data!,
    placeholderData: keepPreviousData,
  });
}
