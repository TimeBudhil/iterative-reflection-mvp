const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

function plural(n: number, unit: string) {
  return `${n} ${unit}${n === 1 ? "" : "s"} ago`;
}

/** "5 minutes ago" style text for an ISO instant; `Never` when there isn't one. */
export function relativeTime(iso: string | null | undefined, now: Date = new Date()): string {
  if (!iso) return "Never";
  const then = new Date(iso);
  const diff = now.getTime() - then.getTime();
  if (diff < MIN) return "just now";
  if (diff < HOUR) return plural(Math.floor(diff / MIN), "minute");
  if (diff < DAY) return plural(Math.floor(diff / HOUR), "hour");
  if (diff < 30 * DAY) return plural(Math.floor(diff / DAY), "day");
  return then.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
}
