"use client";

import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { verdictStyle } from "@/lib/theme/status-styles";
import { VERDICTS } from "@/lib/verdict";
import { SegmentedControl } from "@/components/shared/segmented-control";
import {
  MAX_OPEN_TEXT,
  canSubmit,
  clientErrors,
  initialDraft,
  mergeServerErrors,
  toRequest,
  type Draft,
  type ExistingReflection,
  type Question,
  type SubmitRequest,
} from "@/lib/reflection/form";

/**
 * The scrolling reflection form (R5.1). `initial` prefills it (edit mode, ticket 14). The parent owns the request:
 * `onSubmit` is called at most once at a time, `pending` disables the button and `serverFieldErrors` are the
 * `ApiError.fieldErrors` of the last failed attempt (R5.2).
 */
export function ReflectionForm({
  questions,
  initial,
  pending,
  serverFieldErrors,
  submitLabel = "Submit reflection",
  onSubmit,
}: {
  questions: Question[];
  initial?: ExistingReflection;
  pending: boolean;
  serverFieldErrors?: Record<string, string>;
  submitLabel?: string;
  onSubmit: (request: SubmitRequest) => void;
}) {
  const [draft, setDraft] = useState<Draft>(() => initialDraft(questions, initial));
  const [open, setOpen] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(Object.entries(initialDraft(questions, initial).texts).map(([k]) => [k, true])),
  );
  const sending = useRef(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  // Release the lock once the request has settled (pending went true, then false).
  useEffect(() => {
    if (!pending) sending.current = false;
  }, [pending]);

  const local = clientErrors(questions, draft);
  const server = mergeServerErrors(serverFieldErrors ?? {});
  const ready = canSubmit(questions, draft);

  function submit(e: React.FormEvent) {
    e.preventDefault();
    // Re-entrancy guard: a double-click can fire twice before React re-renders the disabled button (R5.5).
    if (sending.current || pending || !ready) return;
    sending.current = true;
    setTouched({});
    onSubmit(toRequest(questions, draft));
  }

  // Server messages describe the submitted value: hide one as soon as the student edits that part again.
  const touch = (k: string) => setTouched((t) => ({ ...t, [k]: true }));
  const shown = (key: string, part: "score" | "openText") =>
    local.questions[key]?.[part] ?? (touched[`${key}.${part}`] ? undefined : server.questions[key]?.[part]);

  return (
    <form onSubmit={submit} className="max-w-3xl space-y-8" noValidate>
      {questions.map((q) => {
        const scoreError = shown(q.key, "score");
        const textError = shown(q.key, "openText");
        const text = draft.texts[q.key] ?? "";
        const options = Array.from({ length: q.maxScore - q.minScore + 1 }, (_, i) => ({
          value: q.minScore + i,
          label: String(q.minScore + i),
        }));
        return (
          <fieldset key={q.key} className="space-y-3" data-testid={`question-${q.key}`} disabled={pending}>
            <legend className="text-base font-medium">{q.prompt}</legend>
            <SegmentedControl
              label={q.prompt}
              options={options}
              mode="range"
              value={draft.scores[q.key]}
              describedBy={scoreError ? `${q.key}-score-error` : undefined}
              onChange={(v) => {
                touch(`${q.key}.score`);
                setDraft((d) => ({ ...d, scores: { ...d.scores, [q.key]: v } }));
              }}
            />
            <p className="flex justify-between text-xs text-muted-foreground" aria-hidden>
              <span>Not at all</span>
              <span>Completely</span>
            </p>
            {scoreError ? (
              <p id={`${q.key}-score-error`} role="alert" className="text-sm text-destructive">
                {scoreError}
              </p>
            ) : null}

            <details open={open[q.key] ?? false} onToggle={(e) => {
                const isOpen = e.currentTarget.open;
                setOpen((o) => ({ ...o, [q.key]: isOpen }));
              }}>
              <summary className="cursor-pointer text-sm underline">{q.openPrompt}</summary>
              <div className="mt-2 space-y-1">
                <textarea
                  aria-label={q.openPrompt}
                  className="min-h-24 w-full rounded-md border bg-background p-2 text-sm"
                  value={text}
                  aria-invalid={textError ? true : undefined}
                  aria-describedby={`${q.key}-count${textError ? ` ${q.key}-text-error` : ""}`}
                  onChange={(e) => {
                    touch(`${q.key}.openText`);
                    setDraft((d) => ({ ...d, texts: { ...d.texts, [q.key]: e.target.value } }));
                  }}
                />
                <p id={`${q.key}-count`} className="text-xs text-muted-foreground" data-testid={`${q.key}-counter`}>
                  {text.length}/{MAX_OPEN_TEXT}
                </p>
                {textError ? (
                  <p id={`${q.key}-text-error`} role="alert" className="text-sm text-destructive">
                    {textError}
                  </p>
                ) : null}
              </div>
            </details>
          </fieldset>
        );
      })}

      <fieldset className="space-y-3" disabled={pending}>
        <legend className="text-base font-medium">Would I genuinely enjoy this?</legend>
        <SegmentedControl
          label="Would I genuinely enjoy this?"
          options={VERDICTS.map((v) => ({ value: v, label: v }))}
          value={draft.verdict}
          mode="single"
          singleSelectedClassName={(v) => verdictStyle(v).badge}
          onChange={(v) => {
            touch("verdict");
            setDraft((d) => ({ ...d, verdict: v }));
          }}
        />
        {server.verdict && !touched.verdict ? (
          <p role="alert" className="text-sm text-destructive">
            {server.verdict}
          </p>
        ) : null}
      </fieldset>

      {server.other.length > 0 ? (
        <p role="alert" className="text-sm text-destructive">
          {server.other.join(" ")}
        </p>
      ) : null}

      <Button type="submit" disabled={!ready || pending}>
        {pending ? "Submitting..." : submitLabel}
      </Button>
    </form>
  );
}
