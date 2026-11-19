import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import type { Question, SubmitRequest } from "./form";

export function useReflectionQuestions() {
  return useQuery({
    queryKey: queryKeys.reflectionQuestions,
    queryFn: async () => (await api.GET("/api/reflection-questions")).data as Question[],
  });
}

/** POST a new reflection. On success the reflections list, the feed card and `/api/me` are refetched (R5.3). */
export function useSubmitReflection(careerId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: SubmitRequest) =>
      (await api.POST("/api/careers/{careerId}/reflection", { params: { path: { careerId } }, body })).data,
    onSuccess: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.reflections }),
        qc.invalidateQueries({ queryKey: queryKeys.feed }),
        qc.invalidateQueries({ queryKey: queryKeys.me }),
      ]);
    },
  });
}
