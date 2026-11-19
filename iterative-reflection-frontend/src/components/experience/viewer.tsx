"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Skeleton } from "@/components/ui/skeleton";
import { buttonVariants } from "@/components/ui/button";
import { RetryPanel } from "@/components/shared/retry-panel";
import { BlockView } from "./blocks";
import { AbandonButton } from "./abandon-button";
import {
  isUnavailable,
  modeFor,
  startPage,
  viewerState,
  type Experience,
  type ViewerMode,
} from "@/lib/experience/model";
import { useExperience, useMarkViewed, useReflectedCareers } from "@/lib/experience/queries";

function Unavailable() {
  return (
    <div data-testid="career-unavailable" className="space-y-2">
      <p>This career isn&apos;t available right now.</p>
      <Link href="/student/feed" className="underline">
        Back to the feed
      </Link>
    </div>
  );
}

/** Loads the experience and the reflected list (which decides the mode, R4.5), then hands over to the viewer. */
export function ExperienceViewer({ careerId }: { careerId: number }) {
  const experience = useExperience(careerId);
  const reflected = useReflectedCareers();

  if (experience.isError && isUnavailable(experience.error)) return <Unavailable />;
  if (experience.isError) return <RetryPanel error={experience.error} onRetry={() => void experience.refetch()} />;
  if (reflected.isError) return <RetryPanel error={reflected.error} onRetry={() => void reflected.refetch()} />;
  if (!experience.data || !reflected.data) return <Skeleton className="h-64 w-full" aria-label="Loading" />;

  const mode = modeFor(careerId, reflected.data.content ?? []);
  return <Pager careerId={careerId} experience={experience.data} mode={mode} />;
}

function Pager({ careerId, experience, mode }: { careerId: number; experience: Experience; mode: ViewerMode }) {
  const lastPage = experience.lastPage ?? experience.pages?.length ?? 1;
  const [page, setPage] = useState(() => startPage(mode, experience));
  const [blocked, setBlocked] = useState(false);
  const { mutate } = useMarkViewed(careerId);

  // Progress is only recorded for the in-progress card; the backend rejects it (409) for reviewed careers.
  useEffect(() => {
    if (mode !== "in-progress") return;
    mutate(page, { onError: (e) => isUnavailable(e) && setBlocked(true) });
  }, [mode, page, mutate]);

  if (blocked) return <Unavailable />;

  const state = viewerState(mode, page, lastPage);
  const current = experience.pages?.find((p) => p.number === page);

  return (
    <article className="max-w-3xl space-y-6" data-mode={mode}>
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold">{experience.title}</h1>
        <p className="text-sm text-muted-foreground" data-testid="page-indicator">
          {state.label}
        </p>
      </header>

      <section aria-label={current?.title ?? `Page ${page}`} className="space-y-4">
        {current?.title ? <h2 className="text-xl font-medium">{current.title}</h2> : null}
        {current?.blocks?.map((b, i) => <BlockView key={`${page}-${i}`} block={b} />)}
      </section>

      <nav aria-label="Pages" className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          className={buttonVariants({ variant: "outline" })}
          disabled={!state.hasPrev}
          onClick={() => setPage(page - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className={buttonVariants({ variant: "outline" })}
          disabled={!state.hasNext}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
        {state.freeNavigation ? (
          <ol className="flex gap-1" aria-label="Jump to page">
            {Array.from({ length: lastPage }, (_, i) => i + 1).map((n) => (
              <li key={n}>
                <button
                  type="button"
                  aria-current={n === page ? "page" : undefined}
                  className={buttonVariants({ variant: n === page ? "default" : "ghost", size: "sm" })}
                  onClick={() => setPage(n)}
                >
                  {n}
                </button>
              </li>
            ))}
          </ol>
        ) : null}
      </nav>

      {/* Mode actions: Reflect / Abandon for in-progress, Edit reflection for review. */}
      <div className="flex gap-2">
        {state.showReflect ? (
          <Link href={`/student/careers/${careerId}/reflect`} className={buttonVariants({})}>
            Reflect on this career
          </Link>
        ) : null}
        {state.showEditReflection ? (
          <Link href={`/student/careers/${careerId}/reflect`} className={buttonVariants({})}>
            Edit reflection
          </Link>
        ) : null}
        {mode === "in-progress" ? <AbandonButton careerId={careerId} /> : null}
      </div>
    </article>
  );
}
