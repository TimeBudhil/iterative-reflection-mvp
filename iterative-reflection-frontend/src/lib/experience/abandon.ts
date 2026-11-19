import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";

/** Abandons the in-progress career (204) and drops the stale feed card so the next one loads. */
export function useAbandonCareer(careerId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await api.POST("/api/careers/{id}/abandon", { params: { path: { id: careerId } } });
    },
    onSuccess: async () => {
      qc.removeQueries({ queryKey: queryKeys.experience(careerId) });
      await qc.invalidateQueries({ queryKey: queryKeys.feed });
    },
  });
}
