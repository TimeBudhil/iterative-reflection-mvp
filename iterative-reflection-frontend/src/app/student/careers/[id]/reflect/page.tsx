"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { QueryView } from "@/components/feedback/query-view";
import { ReflectionForm } from "@/components/reflection/reflection-form";
import { useExperience, useReflectedCareers } from "@/lib/experience/queries";
import { useEditReflection, useReflectionQuestions, useSubmitReflection } from "@/lib/reflection/queries";
import { journeyModel } from "@/lib/journey/model";
import { CONCURRENT_EDIT_MESSAGE, isConcurrentEdit, type ExistingReflection, type Question } from "@/lib/reflection/form";
import { statusCopy } from "@/lib/api/error-copy";
import type { ApiError } from "@/lib/api/client";

function ReflectView({ careerId }: { careerId: number }) {
  const questions = useReflectionQuestions();
  const reflected = useReflectedCareers();
  const experience = useExperience(careerId);

  return (
    <QueryView query={questions}>
      {(qs) => (
        <QueryView query={reflected}>
          {(list) => (
            <ReflectBody
              careerId={careerId}
              questions={qs}
              title={experience.data?.title}
              existing={journeyModel(list.content ?? []).reflectionFor(careerId)}
            />
          )}
        </QueryView>
      )}
    </QueryView>
  );
}

/** Edit (PUT, prefilled) when the career is already reflected on, otherwise the first submission (POST). */
function ReflectBody({
  careerId,
  questions,
  title,
  existing,
}: {
  careerId: number;
  questions: Question[];
  title?: string;
  existing?: ExistingReflection & { updatedAt?: string };
}) {
  const router = useRouter();
  // Decided once: after a first submit the list refetches, but the page must not flip into edit mode before it navigates away.
  const [editing] = useState(!!existing);
  const submit = useSubmitReflection(careerId);
  const edit = useEditReflection(careerId);
  const mutation = editing ? edit : submit;

  const error = mutation.error as ApiError | null;
  const mustViewAll = !editing && error?.status === 409 && /whole experience/i.test(error.detail);
  const concurrent = editing && isConcurrentEdit(error);

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold">
          {editing ? "Edit reflection" : "Reflect"}
          {title ? `: ${title}` : ""}
        </h1>
        <p className="text-sm text-muted-foreground">Rate each question from 1 to 10, then give your verdict.</p>
      </header>
      {mustViewAll ? (
        <div role="alert" className="max-w-3xl rounded-md border p-3 text-sm">
          View the whole experience before reflecting.{" "}
          <Link href={`/student/careers/${careerId}`} className="underline">
            Back to the experience
          </Link>
        </div>
      ) : concurrent ? (
        <div role="alert" className="max-w-3xl rounded-md border p-3 text-sm" data-testid="concurrent-edit">
          {CONCURRENT_EDIT_MESSAGE}
        </div>
      ) : error && error.status !== 400 ? (
        <div role="alert" className="max-w-3xl rounded-md border p-3 text-sm" data-testid="submit-error">
          {error.status === 409 ? "This career isn't open for reflection right now." : statusCopy(error.status)}{" "}
          <Link href={editing ? "/student/journey" : "/student/feed"} className="underline">
            {editing ? "Back to My Journey" : "Back to the feed"}
          </Link>
        </div>
      ) : null}
      <ReflectionForm
        // Remounts with the latest values when the list refetches after a 409.
        key={editing ? (existing?.updatedAt ?? "edit") : "new"}
        questions={questions}
        initial={editing ? existing : undefined}
        submitLabel={editing ? "Save changes" : undefined}
        pending={mutation.isPending}
        serverFieldErrors={error?.status === 400 ? error.fieldErrors : undefined}
        onSubmit={(body) =>
          editing
            ? edit.mutate(body, { onSuccess: () => router.push("/student/journey") })
            : submit.mutate(body, {
                onSuccess: () => router.push("/student/feed"),
                onError: (e) => {
                  const err = e as ApiError;
                  if (err.status === 409 && /whole experience/i.test(err.detail)) router.push(`/student/careers/${careerId}`);
                },
              })
        }
      />
    </div>
  );
}

export default function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <ReflectView careerId={Number(id)} />;
}
