/** Pure My Journey model (R6.2, R6.3). Everything is computed from the reflections list. */
export type JourneyVerdict = "YES" | "MAYBE" | "NO";

export type JourneyReflection = {
  id?: number;
  careerId?: number;
  careerTitle?: string;
  verdict?: string;
  updatedAt?: string;
  answers?: { key?: string; score?: number; prompt?: string }[];
};

export type TabId = "saved" | "YES" | "MAYBE" | "NO" | "all";
export const TABS: { id: TabId; label: string }[] = [
  { id: "saved", label: "Saved" },
  { id: "YES", label: "YES" },
  { id: "MAYBE", label: "MAYBE" },
  { id: "NO", label: "NO" },
  { id: "all", label: "All" },
];

const time = (r: JourneyReflection) => (r.updatedAt ? Date.parse(r.updatedAt) || 0 : 0);

const VERDICT_ORDER: JourneyVerdict[] = ["YES", "MAYBE", "NO"];

/** Each verdict's count and share (0..1) of all verdicts. Zero-count verdicts are omitted; no reflections gives []. */
export function verdictDistribution(counts: Record<JourneyVerdict, number>): { verdict: JourneyVerdict; count: number; share: number }[] {
  const total = VERDICT_ORDER.reduce((n, v) => n + counts[v], 0);
  if (total === 0) return [];
  return VERDICT_ORDER.filter((v) => counts[v] > 0).map((v) => ({ verdict: v, count: counts[v], share: counts[v] / total }));
}

export function journeyModel<T extends JourneyReflection>(reflections: readonly T[]) {
  const counts: Record<JourneyVerdict, number> = { YES: 0, MAYBE: 0, NO: 0 };
  const sums = new Map<string, { sum: number; n: number }>();
  for (const r of reflections) {
    if (r.verdict === "YES" || r.verdict === "MAYBE" || r.verdict === "NO") counts[r.verdict]++;
    for (const a of r.answers ?? []) {
      if (!a.key || typeof a.score !== "number") continue;
      const s = sums.get(a.key) ?? { sum: 0, n: 0 };
      s.sum += a.score;
      s.n++;
      sums.set(a.key, s);
    }
  }
  const newestFirst = [...reflections].sort((a, b) => time(b) - time(a) || (b.id ?? 0) - (a.id ?? 0));
  const matches: Record<TabId, (r: T) => boolean> = {
    saved: (r) => r.verdict === "YES" || r.verdict === "MAYBE",
    YES: (r) => r.verdict === "YES",
    MAYBE: (r) => r.verdict === "MAYBE",
    NO: (r) => r.verdict === "NO",
    all: () => true,
  };
  return {
    total: reflections.length,
    counts,
    distribution: verdictDistribution(counts),
    averages: [...sums].map(([key, s]) => ({ key, average: s.sum / s.n })),
    tab: (id: TabId): T[] => newestFirst.filter(matches[id]),
    /** For review mode (R4.5) and edit prefill (R5.4). */
    reflectionFor: (careerId: number): T | undefined => reflections.find((r) => r.careerId === careerId),
  };
}

export const formatAverage = (n: number) => n.toFixed(1);
export const dimensionLabel = (key: string) => key.charAt(0).toUpperCase() + key.slice(1);
