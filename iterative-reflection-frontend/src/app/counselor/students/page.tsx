"use client";

import { useState } from "react";
import Link from "next/link";
import { Check, Copy, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { PaginatedTable, type Column } from "@/components/shared/paginated-table";
import { RetryPanel } from "@/components/shared/retry-panel";
import { useCounselorProfile, useCounselorStudents, useRotateJoinCode } from "@/lib/counselor/portal";
import { relativeTime } from "@/lib/format/relative-time";
import type { components } from "@/lib/api/schema";

type StudentRow = components["schemas"]["Row"];

const columns: Column<StudentRow>[] = [
  {
    header: "Name",
    cell: (r) => (
      <Link href={`/counselor/students/${r.studentId}`} className="font-medium underline-offset-4 hover:underline">
        {r.displayName}
      </Link>
    ),
  },
  { header: "Seen", cell: (r) => r.careersSeen, className: "text-right" },
  { header: "Reflected", cell: (r) => r.reflected, className: "text-right" },
  { header: "Abandoned", cell: (r) => r.abandoned, className: "text-right" },
  { header: "YES / MAYBE / NO", cell: (r) => `${r.yes} / ${r.maybe} / ${r.no}` },
  {
    header: "Intake",
    cell: (r) => (r.intakeCompleted ? <Check className="size-4" aria-label="Completed" /> : <span aria-label="Not completed">–</span>),
  },
  { header: "Last active", cell: (r) => relativeTime(r.lastActive) },
];

function JoinCodeCard() {
  const profile = useCounselorProfile();
  const rotate = useRotateJoinCode();
  const [confirming, setConfirming] = useState(false);

  async function copy(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      toast.success("Join code copied");
    } catch {
      toast.error("Couldn't copy the code. Select it and copy manually.");
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Your join code</CardTitle>
      </CardHeader>
      <CardContent>
        {profile.error ? (
          <RetryPanel error={profile.error} onRetry={() => void profile.refetch()} />
        ) : !profile.data ? (
          <Skeleton className="h-9 w-48" aria-label="Loading" />
        ) : (
          <div className="flex flex-wrap items-center gap-3">
            <code data-testid="join-code" className="rounded-md bg-muted px-3 py-1.5 font-mono text-lg tracking-widest">
              {profile.data.joinCode}
            </code>
            <Button variant="outline" onClick={() => void copy(profile.data!.joinCode ?? "")}>
              <Copy className="size-4" aria-hidden /> Copy
            </Button>
            <Button variant="outline" onClick={() => setConfirming(true)}>
              <RefreshCw className="size-4" aria-hidden /> Rotate
            </Button>
          </div>
        )}
        <ConfirmDialog
          open={confirming}
          onOpenChange={setConfirming}
          title="Rotate join code?"
          description="The old code stops working. Linked students stay linked."
          confirmLabel="Rotate"
          pending={rotate.isPending}
          onConfirm={() =>
            rotate.mutate(undefined, {
              onSuccess: () => {
                setConfirming(false);
                toast.success("Join code rotated");
              },
              onError: () => toast.error("Couldn't rotate the code. Please try again."),
            })
          }
        />
      </CardContent>
    </Card>
  );
}

export default function Page() {
  const [page, setPage] = useState(0);
  const students = useCounselorStudents(page);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Students</h1>
      <JoinCodeCard />
      <PaginatedTable
        label="Linked students"
        columns={columns}
        rowKey={(r) => r.studentId!}
        query={students}
        page={page}
        onPageChange={setPage}
        empty="No students yet. Share your join code above."
      />
    </div>
  );
}
