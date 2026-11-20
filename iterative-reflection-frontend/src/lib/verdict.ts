/** The one verdict type and its display order (YES, MAYBE, NO). */
export const VERDICTS = ["YES", "MAYBE", "NO"] as const;
export type Verdict = (typeof VERDICTS)[number];
export type VerdictCounts = Record<Verdict, number>;

export const isVerdict = (v: unknown): v is Verdict => (VERDICTS as readonly unknown[]).includes(v);
