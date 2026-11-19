"use client";

import { use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { QueryView } from "@/components/feedback/query-view";
import { ReflectionForm } from "@/components/reflection/reflection-form";
import { useExperience, useReflectedCareers } from "@/lib/experience/queries";
import { useReflectionQuestions, useSubmitReflection } from "@/lib/reflection/queries";
import { statusCopy } from "@/lib/api/error-copy";
import type { ApiError } from "@/lib/api/client";

function ReflectView({ careerId }: { careerId: number }) {
  const router = useRouter();
  const questions = useReflectionQuestions();
  const reflected = useReflectedCareers();
  const experience = useExperience(careerId);
  const submit = useSubmitReflection(careerId);

  const error = submit.error as ApiError | null;
  const mustViewAll = error?.status === 409 && /whole experience/i.test(error.detail);

  return (
    <QueryView query={questions}>
      {(qs) => (
        <QueryView query={reflected}>
          {(list) =>
            list.content?.some((r) => r.careerId === careerId) ? (
              <p>
                You already reflected on this career.{" "}
                <Link href={`/student/careers/${careerId}`} className="underline">
                  Review it
                </Link>
              </p>
            ) : (
              <div className="space-y-6">
                <header className="space-y-1">
                  <h1 className="text-2xl font-semibold">Reflect{experience.data?.title ? `: ${experience.data.title}` : ""}</h1>
                  <p className="text-sm text-muted-foreground">Rate each question from 1 to 10, then give your verdict.</p>
                </header>
                {mustViewAll ? (
                  <div role="alert" className="max-w-3xl rounded-md border p-3 text-sm">
                    View the whole experience before reflecting.{" "}
                    <Link href={`/student/careers/${careerId}`} className="underline">
                      Back to the experience
                    </Link>
                  </div>
                ) : error && error.status !== 400 ? (
                  <div role="alert" className="max-w-3xl rounded-md border p-3 text-sm" data-testid="submit-error">
                    {error.status === 409 ? "This career isn't open for reflection right now." : statusCopy(error.status)}{" "}
                    <Link href="/student/feed" className="underline">
                      Back to the feed
                    </Link>
                  </div>
                ) : null}
                <ReflectionForm
                  questions={qs}
                  pending={submit.isPending}
                  serverFieldErrors={error?.status === 400 ? error.fieldErrors : undefined}
                  onSubmit={(body) =>
                    submit.mutate(body, {
                      onSuccess: () => router.push("/student/feed"),
                      onError: (e) => {
                        const err = e as ApiError;
                        if (err.status === 409 && /whole experience/i.test(err.detail)) router.push(`/student/careers/${careerId}`);
                      },
                    })
                  }
                />
              </div>
            )
          }
        </QueryView>
      )}
    </QueryView>
  );
}

export default function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <ReflectView careerId={Number(id)} />;
}
