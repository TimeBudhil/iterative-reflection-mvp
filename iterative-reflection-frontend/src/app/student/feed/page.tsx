"use client";

import Link from "next/link";
import { IntakePromptModal } from "@/components/intake/intake-prompt-modal";
import { RetryPanel } from "@/components/shared/retry-panel";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { feedView } from "@/lib/feed/model";
import { useFeed } from "@/lib/feed/queries";

export default function Page() {
  const feed = useFeed();
  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Feed</h1>
      <IntakePromptModal />
      {feed.isPending ? (
        <Skeleton className="h-48 w-full" aria-label="Loading" />
      ) : feed.isError ? (
        <RetryPanel error={feed.error} onRetry={() => void feed.refetch()} />
      ) : (
        <FeedBody data={feed.data} />
      )}
    </div>
  );
}

function FeedBody({ data }: { data: Parameters<typeof feedView>[0] }) {
  const view = feedView(data);
  if (view.kind === "exhausted") {
    return (
      <div data-testid="feed-exhausted" className="space-y-2">
        <p>You&apos;ve explored every career.</p>
        <Link href="/student/journey" className="underline">
          My Journey
        </Link>
      </div>
    );
  }
  return (
    <Card data-testid="career-card">
      <CardHeader>
        <CardTitle className="text-xl">{view.card.title}</CardTitle>
        <CardDescription>{view.minutes}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        <p>{view.card.summary}</p>
        <ul className="flex flex-wrap gap-2" aria-label="Tags">
          {view.card.tags?.map((t) => (
            <li key={t.id} className="rounded-full border px-2.5 py-0.5 text-xs">
              {t.name}
            </li>
          ))}
        </ul>
      </CardContent>
      <CardFooter>
        <Link
          href={view.href}
          className="inline-flex h-9 items-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          {view.label}
        </Link>
      </CardFooter>
    </Card>
  );
}
