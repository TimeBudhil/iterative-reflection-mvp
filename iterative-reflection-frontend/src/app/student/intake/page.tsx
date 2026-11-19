"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { PageSkeleton } from "@/components/feedback/page-skeleton";
import { RetryPanel } from "@/components/shared/retry-panel";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";
import { statusCopy } from "@/lib/api/error-copy";
import { useIntake, useIntakeQuestions, useSaveIntake } from "@/lib/intake/queries";
import {
  MAX_TEXT,
  buildRequest,
  groupByCategory,
  initialDraft,
  isNotYetAvailable,
  mergeFieldErrors,
  toggleTag,
  type ExistingIntake,
  type IntakeDraft,
  type IntakeQuestion,
} from "@/lib/intake/model";

export default function Page() {
  const questions = useIntakeQuestions();
  const intake = useIntake();
  const failed = questions.isError ? questions : intake.isError ? intake : null;

  return (
    <div className="max-w-2xl space-y-4">
      <h1 className="text-2xl font-semibold">Your interests</h1>
      {failed ? (
        <RetryPanel error={failed.error} onRetry={() => void failed.refetch()} />
      ) : questions.data === undefined || intake.data === undefined ? (
        <PageSkeleton />
      ) : (
        <IntakeForm questions={questions.data} existing={intake.data} />
      )}
    </div>
  );
}

function IntakeForm({ questions, existing }: { questions: IntakeQuestion[]; existing: ExistingIntake | null }) {
  const router = useRouter();
  const editing = existing !== null;
  const save = useSaveIntake(editing);
  const [draft, setDraft] = useState<IntakeDraft>(() => initialDraft(questions, existing));
  const [unavailable, setUnavailable] = useState(false);
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  if (unavailable) {
    return (
      <div role="status" data-testid="intake-unavailable" className="rounded-lg border p-6">
        The intake opens after your 3rd reflection.
      </div>
    );
  }

  const request = buildRequest(questions, draft);
  const merged = mergeFieldErrors(serverErrors);

  function submit() {
    if (!request) return;
    setServerErrors({});
    setFormError(null);
    save.mutate(request, {
      onSuccess: () => {
        toast.success(editing ? "Your interests are updated." : "Thanks for sharing your interests.");
        router.push(editing ? "/student/settings" : "/student/feed");
      },
      onError: (err) => {
        if (!(err instanceof ApiError)) return setFormError(statusCopy(0));
        if (isNotYetAvailable(err)) return setUnavailable(true);
        setServerErrors(err.fieldErrors);
        if (err.status !== 400 || Object.keys(err.fieldErrors).length === 0) setFormError(err.status === 409 ? err.detail : statusCopy(err.status));
      },
    });
  }

  return (
    <form
      className="space-y-8"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      <p className="text-sm text-muted-foreground">Answer any of these. You only need to fill in one.</p>
      {questions.map((q) => (
        <fieldset key={q.key} data-testid={`intake-${q.key}`} className="space-y-3">
          <legend className="text-base font-medium">{q.prompt}</legend>
          {q.type === "TAGS" ? (
            <div className="space-y-4">
              {groupByCategory(q.options ?? []).map((g) => (
                <div key={g.category} role="group" aria-label={g.category} className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{g.category}</p>
                  <div className="flex flex-wrap gap-2">
                    {g.options.map((o) => {
                      const selected = (draft.tags[q.key] ?? []).includes(o.id);
                      return (
                        <button
                          key={o.id}
                          type="button"
                          aria-pressed={selected}
                          onClick={() => setDraft((d) => ({ ...d, tags: { ...d.tags, [q.key]: toggleTag(d.tags[q.key] ?? [], o.id) } }))}
                          className={`rounded-full border px-3 py-1 text-sm focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none ${
                            selected ? "border-primary bg-primary text-primary-foreground" : "hover:bg-muted"
                          }`}
                        >
                          {o.name}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <textarea
              aria-label={q.prompt}
              rows={3}
              maxLength={MAX_TEXT}
              value={draft.texts[q.key] ?? ""}
              onChange={(e) => setDraft((d) => ({ ...d, texts: { ...d.texts, [q.key]: e.target.value } }))}
              className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
            />
          )}
          {merged.byKey[q.key] && (
            <p role="alert" className="text-sm text-destructive">
              {merged.byKey[q.key]}
            </p>
          )}
        </fieldset>
      ))}
      {(formError || merged.form) && (
        <p role="alert" className="text-sm text-destructive">
          {formError ?? merged.form}
        </p>
      )}
      <Button type="submit" disabled={!request || save.isPending}>
        {editing ? "Save changes" : "Save interests"}
      </Button>
    </form>
  );
}
