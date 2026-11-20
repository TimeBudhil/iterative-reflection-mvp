"use client";

import { useEffect, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { statusCopy } from "@/lib/api/error-copy";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { barWidths, rangeFromParams, rangeToQuery, rangeToSearch } from "@/lib/metrics/model";
import { VERDICTS, type VerdictCounts } from "@/lib/verdict";
import { verdictStyle } from "@/lib/theme/status-styles";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";

const VERDICT_LABELS = { YES: "Yes", MAYBE: "Maybe", NO: "No" } as const;

export function MetricsView() {
  const router = useRouter();
  const pathname = usePathname();
  const range = rangeFromParams(useSearchParams());

  const q = useQuery({
    queryKey: queryKeys.pilotMetrics(range.from, range.to),
    queryFn: async () =>
      (await api.GET("/api/admin/pilot-metrics", { params: { query: rangeToQuery(range) } })).data!,
  });

  // The ref holds the latest requested range so two quick edits can't overwrite each other
  // before the router has re-rendered with the first one.
  const latest = useRef(range);
  useEffect(() => {
    latest.current = range;
  }, [range.from, range.to]); // eslint-disable-line react-hooks/exhaustive-deps
  const setBound = (key: "from" | "to", value: string) => {
    latest.current = { ...latest.current, [key]: value };
    router.replace(`${pathname}${rangeToSearch(latest.current)}`);
  };

  const m = q.data;
  const verdicts: VerdictCounts = { YES: m?.verdicts?.YES ?? 0, MAYBE: m?.verdicts?.MAYBE ?? 0, NO: m?.verdicts?.NO ?? 0 };
  const widths = barWidths(verdicts);
  const pct = (n: number | undefined) => `${Math.round((n ?? 0) * 10) / 10}%`;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Metrics</h1>

      <div className="flex flex-wrap items-end gap-4">
        <div className="space-y-1">
          <Label htmlFor="metrics-from">From</Label>
          <Input id="metrics-from" type="date" value={range.from} max={range.to || undefined} onChange={(e) => setBound("from", e.target.value)} />
        </div>
        <div className="space-y-1">
          <Label htmlFor="metrics-to">To</Label>
          <Input id="metrics-to" type="date" value={range.to} min={range.from || undefined} onChange={(e) => setBound("to", e.target.value)} />
        </div>
        <p className="text-sm text-muted-foreground">Signup date, inclusive. Leave both empty for all time.</p>
      </div>

      {q.isError ? (
        <p role="alert" className="text-sm text-destructive">
          {statusCopy((q.error as { status?: number }).status ?? 0)}
        </p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <Tile label="Total students" value={m ? String(m.totalStudents ?? 0) : null} testId="tile-total" />
            <Tile label="With 3+ reflections" value={m ? pct(m.percentWithThreeReflections) : null} testId="tile-reflections" />
            <Tile label="Intake completed" value={m ? pct(m.percentIntakeCompleted) : null} testId="tile-intake" />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Verdicts</CardTitle>
            </CardHeader>
            <CardContent>
              <ul aria-label="Verdict counts" className="space-y-3">
                {VERDICTS.map((v) => (
                  <li key={v} className="grid grid-cols-[4rem_1fr_3rem] items-center gap-3 text-sm">
                    <span>{VERDICT_LABELS[v]}</span>
                    <div className="h-6 rounded bg-muted" aria-hidden="true">
                      <div className={`h-full rounded ${verdictStyle(v).bar}`} style={{ width: `${widths[v]}%` }} />
                    </div>
                    <span className="text-right tabular-nums" data-testid={`verdict-${v}`}>{m ? verdicts[v] : "-"}</span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        </>
      )}

      <p className="text-sm text-muted-foreground">Demo accounts are excluded from these numbers.</p>
    </div>
  );
}

function Tile({ label, value, testId }: { label: string; value: string | null; testId: string }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
      </CardHeader>
      <CardContent>
        {value === null ? <Skeleton className="h-8 w-20" /> : <p className="text-3xl font-semibold" data-testid={testId}>{value}</p>}
      </CardContent>
    </Card>
  );
}
