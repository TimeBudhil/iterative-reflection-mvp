export type DateRange = { from: string; to: string };
import type { VerdictCounts } from "@/lib/verdict";

function validDate(s: string | null): string {
  if (!s || !/^\d{4}-\d{2}-\d{2}$/.test(s)) return "";
  const d = new Date(`${s}T00:00:00Z`);
  return Number.isNaN(d.getTime()) || d.toISOString().slice(0, 10) !== s ? "" : s;
}

/** The signup-date filter as the URL holds it; anything unparsable counts as unset. */
export function rangeFromParams(params: URLSearchParams): DateRange {
  return { from: validDate(params.get("from")), to: validDate(params.get("to")) };
}

export function rangeToQuery(r: DateRange): { from?: string; to?: string } {
  return { ...(r.from && { from: r.from }), ...(r.to && { to: r.to }) };
}

export function rangeToSearch(r: DateRange): string {
  const s = new URLSearchParams(rangeToQuery(r)).toString();
  return s ? `?${s}` : "";
}

/** Bar widths in percent, relative to the largest verdict count. */
export function barWidths(c: VerdictCounts): VerdictCounts {
  const max = Math.max(c.YES, c.MAYBE, c.NO);
  const w = (n: number) => (max === 0 ? 0 : Math.round((n / max) * 100));
  return { YES: w(c.YES), MAYBE: w(c.MAYBE), NO: w(c.NO) };
}
