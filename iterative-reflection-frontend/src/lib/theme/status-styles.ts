export type VerdictKey = "YES" | "MAYBE" | "NO";

/** One mapping for verdict colors: YES positive, MAYBE highlight tint, NO muted. Slots only. */
const VERDICT: Record<VerdictKey, { badge: string; bar: string }> = {
  YES: { badge: "bg-positive text-positive-foreground", bar: "bg-positive border border-positive-foreground/40" },
  MAYBE: { badge: "bg-highlight/20 text-highlight-text", bar: "bg-highlight" },
  NO: { badge: "bg-muted text-muted-foreground", bar: "bg-muted border border-border" },
};

export function verdictStyle(verdict: string | null | undefined): { badge: string; bar: string } {
  return VERDICT[(verdict ?? "") as VerdictKey] ?? VERDICT.NO;
}

export const activeBadge = (active: boolean): string =>
  active ? "bg-positive text-positive-foreground" : "bg-muted text-muted-foreground";

export const outcomeBadge = (allowed: boolean): string =>
  allowed ? "bg-positive text-positive-foreground" : "bg-destructive/10 text-destructive";
