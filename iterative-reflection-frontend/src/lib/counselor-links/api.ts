"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";

const SIZE = 100;

export function useCounselorLinks() {
  return useQuery({
    queryKey: queryKeys.counselorLinks,
    queryFn: async () =>
      (await api.GET("/api/students/me/counselors", { params: { query: { pageable: { page: 0, size: SIZE } } } })).data!
        .content,
  });
}

export function useLinkCounselor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (code: string) => (await api.POST("/api/students/me/counselors", { body: { code } })).data!,
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.counselorLinks }),
  });
}

export function useRevokeCounselor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await api.DELETE("/api/students/me/counselors/{id}", { params: { path: { id } } });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.counselorLinks }),
  });
}
