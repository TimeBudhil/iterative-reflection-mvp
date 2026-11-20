import type { Verdict } from "@/lib/verdict";

export type VerdictStyle = { badge: string; bar: string };

const POSITIVE = "bg-positive text-positive-foreground";
const NEUTRAL = "bg-muted text-muted-foreground";

/**
 * One mapping for verdict colors: YES positive, MAYBE a highlight tint, NO muted. Slots only.
 * The MAYBE tint is highlight at 10% painted over an opaque `surface` layer, so highlight-text keeps AA wherever the chip sits.
 */
const VERDICT: Record<Verdict, VerdictStyle> = {
  YES: { badge: POSITIVE, bar: "bg-positive border border-positive-foreground/40" },
  MAYBE: {
    badge: "bg-surface bg-linear-to-r from-highlight/10 to-highlight/10 text-highlight-text border border-highlight",
    bar: "bg-highlight",
  },
  NO: { badge: NEUTRAL, bar: "bg-muted border border-border" },
};

export const verdictStyle = (verdict: Verdict): VerdictStyle => VERDICT[verdict];

export const activeBadge = (active: boolean): string => (active ? POSITIVE : NEUTRAL);

export const outcomeBadge = (allowed: boolean): string => (allowed ? POSITIVE : "bg-destructive/10 text-destructive");
