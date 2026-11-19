import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";
import { queryKeys } from "@/lib/query/keys";
import type { Experience } from "./model";

export function useExperience(careerId: number) {
  return useQuery({
    queryKey: queryKeys.experience(careerId),
    queryFn: async () => (await api.GET("/api/careers/{id}/experience", { params: { path: { id: careerId } } })).data as Experience,
    retry: false,
  });
}

export type ReflectionView = components["schemas"]["ReflectionView"];

/** Same cache entry as My Journey / reflect (R4.5, R6.1): every page at size=100. */
export function useReflectedCareers() {
  return useQuery({
    queryKey: queryKeys.reflections,
    queryFn: async () => {
      const content: ReflectionView[] = [];
      for (let page = 0; ; page++) {
        const data = (
          await api.GET("/api/students/me/reflections", {
            params: { query: { pageable: { page, size: 100 } } },
            querySerializer: () => `page=${page}&size=100`, // the schema models Spring's Pageable as one object; the wire is flat
          })
        ).data as { content?: ReflectionView[]; page?: { totalPages?: number } } | undefined;
        content.push(...(data?.content ?? []));
        if (page + 1 >= (data?.page?.totalPages ?? 0)) break;
      }
      return { content };
    },
  });
}

export function useMarkViewed(careerId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (n: number) =>
      (await api.POST("/api/careers/{id}/pages/{n}/viewed", { params: { path: { id: careerId, n } } })).data,
    // The feed card's "Continue (page n of m)" and the resume page both read lastViewedPage.
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.feed });
      void qc.invalidateQueries({ queryKey: queryKeys.experience(careerId) });
    },
  });
}
