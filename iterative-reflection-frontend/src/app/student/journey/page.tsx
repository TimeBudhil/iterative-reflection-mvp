"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { QueryView } from "@/components/feedback/query-view";
import { Card, CardContent } from "@/components/ui/card";
import { useReflectedCareers, type ReflectionView } from "@/lib/experience/queries";
import { dimensionLabel, formatAverage, journeyModel, TABS, type TabId } from "@/lib/journey/model";
import { cn } from "@/lib/utils";

const BADGE: Record<string, string> = {
  YES: "bg-green-100 text-green-900",
  MAYBE: "bg-amber-100 text-amber-900",
  NO: "bg-red-100 text-red-900",
};

function Journey({ reflections }: { reflections: ReflectionView[] }) {
  const model = useMemo(() => journeyModel(reflections), [reflections]);
  const [tab, setTab] = useState<TabId>("saved");

  if (model.total === 0) {
    return (
      <p data-testid="journey-empty">
        Reflect on your first career to see your journey here.{" "}
        <Link href="/student/feed" className="underline">
          Go to the feed
        </Link>
      </p>
    );
  }
  const rows = model.tab(tab);
  return (
    <div className="space-y-6">
      <section aria-label="Progress" data-testid="stat-strip" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Careers reflected on" value={model.total} id="total" />
        <Stat label="YES" value={model.counts.YES} id="YES" />
        <Stat label="MAYBE" value={model.counts.MAYBE} id="MAYBE" />
        <Stat label="NO" value={model.counts.NO} id="NO" />
        {model.averages.map((a) => (
          <Stat key={a.key} label={`Average ${dimensionLabel(a.key).toLowerCase()}`} value={formatAverage(a.average)} id={`avg-${a.key}`} />
        ))}
      </section>

      <div role="tablist" aria-label="Filter careers" className="flex flex-wrap gap-1">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id)}
            className={cn("h-9 rounded-md border px-3 text-sm font-medium", tab === t.id ? "border-primary bg-primary text-primary-foreground" : "hover:bg-accent")}
          >
            {t.label}
          </button>
        ))}
      </div>

      {rows.length === 0 ? (
        <p className="text-sm text-muted-foreground">No careers here yet.</p>
      ) : (
        <ul className="space-y-2" data-testid="journey-rows">
          {rows.map((r) => (
            <li key={r.id} data-testid="journey-row" className="flex flex-wrap items-center gap-3 rounded-lg border p-3">
              <Link href={`/student/careers/${r.careerId}`} className="min-w-0 flex-1 font-medium underline-offset-2 hover:underline">
                {r.careerTitle}
              </Link>
              <span className={cn("rounded-full px-2 py-0.5 text-xs font-semibold", BADGE[r.verdict ?? ""])}>{r.verdict}</span>
              <time dateTime={r.updatedAt} className="text-sm text-muted-foreground">
                {r.updatedAt ? new Date(r.updatedAt).toLocaleDateString() : ""}
              </time>
              <Link href={`/student/careers/${r.careerId}/reflect`} className="text-sm underline">
                Edit reflection
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Stat({ label, value, id }: { label: string; value: number | string; id: string }) {
  return (
    <Card>
      <CardContent className="p-4" data-testid={`stat-${id}`}>
        <div className="text-2xl font-semibold">{value}</div>
        <div className="text-sm text-muted-foreground">{label}</div>
      </CardContent>
    </Card>
  );
}

export default function Page() {
  const query = useReflectedCareers();
  return (
    <div className="max-w-4xl space-y-4">
      <h1 className="text-2xl font-semibold">My Journey</h1>
      <QueryView query={query}>{(data) => <Journey reflections={data.content} />}</QueryView>
    </div>
  );
}
