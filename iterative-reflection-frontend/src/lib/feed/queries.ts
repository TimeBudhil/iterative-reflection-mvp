import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import type { FeedCard } from "./model";

export function useFeed() {
  return useQuery({
    queryKey: queryKeys.feed,
    queryFn: async () => (await api.GET("/api/feed/next")).data as { career?: FeedCard | null; exhausted?: boolean },
  });
}
