/** One place for TanStack Query keys. Each resource adds its keys here; mutations invalidate by these. */
export const queryKeys = {
  me: ["me"] as const,
  reflections: ["reflections"] as const,
  feed: ["feed"] as const,
  experience: (careerId: number | string) => ["experience", String(careerId)] as const,
};
