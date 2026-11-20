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

/** Reflections needed before the interests form unlocks. Mirrors the backend rule. */
export const INTAKE_UNLOCK_THRESHOLD = 3;

export type FeedProgress = {
  intake: { label: string; current: number; max: number } | null;
  explored: string | null;
};

/** Feed progress lines from the reflection count. Each is null (hidden) when it doesn't apply. */
export function feedProgress(reflectionCount: number): FeedProgress {
  const n = Math.max(0, reflectionCount);
  return {
    intake:
      n < INTAKE_UNLOCK_THRESHOLD
        ? {
            label: `${n} of ${INTAKE_UNLOCK_THRESHOLD} reflections until your interests form unlocks`,
            current: n,
            max: INTAKE_UNLOCK_THRESHOLD,
          }
        : null,
    explored: n >= 1 ? `${n} ${n === 1 ? "career" : "careers"} explored` : null,
  };
}
