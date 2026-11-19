"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { RetryPanel } from "@/components/shared/retry-panel";
import { ApiError } from "@/lib/api/client";
import { useOwnForbiddenCopy } from "@/lib/session/access";
import { useStudentDetail } from "@/lib/counselor/portal";
import type { StudentDetailView } from "@/lib/counselor/student-detail";

const dateFormat = new Intl.DateTimeFormat("en-US", { year: "numeric", month: "short", day: "numeric" });

function Section({ title, empty, children }: { title: string; empty?: string; children?: React.ReactNode }) {
  return (
    <Card role="region" aria-label={title}>
      <CardHeader>
        <CardTitle>
          <h2>{title}</h2>
        </CardTitle>
      </CardHeader>
      <CardContent>{children || <p className="text-sm text-muted-foreground">{empty}</p>}</CardContent>
    </Card>
  );
}

function Detail({ view }: { view: StudentDetailView }) {
  return (
    <div className="space-y-6">
      <Section title="Top interests" empty="No interests recorded yet.">
        {view.affinities.length > 0 && (
          <ul className="flex flex-wrap gap-2">
            {view.affinities.map((a) => (
              <li key={a.id} className="rounded-full border px-3 py-1 text-sm">
                {a.tag} <span className="text-muted-foreground">({a.category})</span>
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="Reflections" empty="No reflections yet.">
        {view.reflections.length > 0 && (
          <ul className="space-y-6">
            {view.reflections.map((r) => (
              <li key={r.id} data-testid="reflection" className="space-y-2">
                <div className="flex flex-wrap items-baseline gap-x-3">
                  <h3 className="font-medium">{r.career}</h3>
                  <span className="rounded-md bg-muted px-2 py-0.5 text-xs font-semibold">{r.verdict}</span>
                  {r.date && <span className="text-sm text-muted-foreground">{dateFormat.format(new Date(r.date))}</span>}
                </div>
                <dl className="grid gap-x-4 gap-y-1 text-sm sm:grid-cols-[1fr_auto]">
                  {r.dimensions.map((d) => (
                    <div key={d.prompt} className="contents">
                      <dt>{d.prompt}</dt>
                      <dd className="font-medium">{d.score} / 10</dd>
                    </div>
                  ))}
                </dl>
                {r.openAnswers.map((o) => (
                  <blockquote key={o.prompt} className="border-l-2 pl-3 text-sm">
                    <p className="text-muted-foreground">{o.prompt}</p>
                    <p>{o.text}</p>
                  </blockquote>
                ))}
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="Abandoned careers" empty="No abandoned careers.">
        {view.abandoned.length > 0 && (
          <ul className="space-y-1 text-sm">
            {view.abandoned.map((c) => (
              <li key={c.career}>
                <span className="font-medium">{c.career}</span>: reached page {c.lastPage}, abandoned {c.count} {c.count === 1 ? "time" : "times"}
              </li>
            ))}
          </ul>
        )}
      </Section>

      <Section title="Intake answers" empty="The student hasn't filled in the intake yet.">
        {view.intake && view.intake.length > 0 && (
          <dl className="space-y-3 text-sm">
            {view.intake.map((a) => (
              <div key={a.prompt}>
                <dt className="text-muted-foreground">{a.prompt}</dt>
                <dd>{a.answer}</dd>
              </div>
            ))}
          </dl>
        )}
      </Section>
    </div>
  );
}

export default function Page() {
  const { id } = useParams<{ id: string }>();
  useOwnForbiddenCopy(); // a 403 here has its own copy (R9.5), so skip the generic no-access panel
  const detail = useStudentDetail(id);
  const forbidden = detail.error instanceof ApiError && detail.error.status === 403;

  return (
    <div className="space-y-6">
      <Link href="/counselor/students" className="inline-flex items-center gap-1 text-sm underline-offset-4 hover:underline">
        <ArrowLeft className="size-4" aria-hidden /> Students
      </Link>
      <h1 className="text-2xl font-semibold">{detail.data ? detail.data.name : "Student"}</h1>
      {forbidden ? (
        <p role="alert">This student hasn&apos;t shared their progress with you.</p>
      ) : detail.error ? (
        <RetryPanel error={detail.error} onRetry={() => void detail.refetch()} />
      ) : !detail.data ? (
        <Skeleton className="h-48 w-full" aria-label="Loading" />
      ) : (
        <Detail view={detail.data} />
      )}
    </div>
  );
}
