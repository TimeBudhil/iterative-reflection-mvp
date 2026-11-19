/** One place for TanStack Query keys. Each resource adds its keys here; mutations invalidate by these. */
export const queryKeys = {
  me: ["me"] as const,
  counselorMe: ["counselor", "me"] as const,
  counselorStudents: (page: number) => ["counselor", "students", page] as const,
};
