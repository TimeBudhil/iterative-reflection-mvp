/** One place for TanStack Query keys. Each resource adds its keys here; mutations invalidate by these. */
export const queryKeys = {
  me: ["me"] as const,
  pilotMetrics: (from: string, to: string) => ["admin", "pilot-metrics", from, to] as const,
  reflections: ["reflections"] as const,
  reflectionQuestions: ["reflection-questions"] as const,
  counselorMe: ["counselor", "me"] as const,
  counselorStudents: (page: number) => ["counselor", "students", page] as const,
  counselorStudent: (id: number | string) => ["counselor", "student", String(id)] as const,
  counselorLinks: ["student", "counselors"] as const,
  feed: ["feed"] as const,
  experience: (careerId: number | string) => ["experience", String(careerId)] as const,
};
