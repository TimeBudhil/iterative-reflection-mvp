/** One place for TanStack Query keys. Each resource adds its keys here; mutations invalidate by these. */
export const queryKeys = {
  me: ["me"] as const,
  pilotMetrics: (from: string, to: string) => ["admin", "pilot-metrics", from, to] as const,
};
