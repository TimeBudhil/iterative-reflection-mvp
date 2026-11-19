import { useMutation, useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import type { Experience } from "./model";

export function useExperience(careerId: number) {
  return useQuery({
    queryKey: queryKeys.experience(careerId),
    queryFn: async () => (await api.GET("/api/careers/{id}/experience", { params: { path: { id: careerId } } })).data as Experience,
    retry: false,
  });
}

/** Same cache entry as My Journey / reflect (R4.5, R6.1): every page at size=100. */
export function useReflectedCareers() {
  return useQuery({
    queryKey: queryKeys.reflections,
    queryFn: async () =>
      (await api.GET("/api/students/me/reflections", {
          params: { query: { pageable: { page: 0, size: 100 } } },
          querySerializer: () => "page=0&size=100", // the schema models Spring's Pageable as one object; the wire is flat
        })).data as {
        content?: { careerId?: number }[];
      },
  });
}

export function useMarkViewed(careerId: number) {
  return useMutation({
    mutationFn: async (n: number) =>
      (await api.POST("/api/careers/{id}/pages/{n}/viewed", { params: { path: { id: careerId, n } } })).data,
  });
}
