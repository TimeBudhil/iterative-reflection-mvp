export type FeedCard = {
  id?: number;
  title?: string;
  summary?: string;
  estimatedMinutes?: number;
  tags?: { id?: number; category?: string; name?: string }[];
  lastPage?: number;
  lastViewedPage?: number;
};

/** "Start" with no progress, otherwise "Continue (page n of m)" (R3.2). The feed card already carries `lastViewedPage`. */
export function startLabel(card: { lastViewedPage?: number; lastPage?: number }): string {
  const n = card.lastViewedPage ?? 0;
  return n > 0 ? `Continue (page ${n} of ${card.lastPage ?? n})` : "Start";
}

export type FeedView =
  | { kind: "exhausted" }
  | { kind: "card"; card: FeedCard; minutes: string; label: string; href: string };

export function feedView(res: { career?: FeedCard | null; exhausted?: boolean }): FeedView {
  const card = res.career;
  if (!card) return { kind: "exhausted" };
  return {
    kind: "card",
    card,
    minutes: `~${card.estimatedMinutes ?? 0} min`,
    label: startLabel(card),
    href: `/student/careers/${card.id}`,
  };
}
