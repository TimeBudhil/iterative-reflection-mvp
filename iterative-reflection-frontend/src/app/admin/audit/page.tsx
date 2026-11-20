"use client";

import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PaginatedTable, type Column } from "@/components/shared/paginated-table";
import { outcomeBadge } from "@/lib/theme/status-styles";
import { useAuditLog } from "@/lib/admin/audit";
import { counselorLabel, outcomeLabel, parseStudentIdFilter } from "@/lib/admin/audit-model";
import { counselorNameMap } from "@/lib/admin/counselor-model";
import { useAdminCounselors } from "@/lib/admin/counselors";
import type { components } from "@/lib/api/schema";

type AuditRow = components["schemas"]["AuditEntry"];

function OutcomeBadge({ outcome }: { outcome?: string }) {
  const allowed = outcome === "ALLOWED";
  return (
    <span
      className={
        "inline-flex rounded-full px-2 py-0.5 text-xs font-medium " +
        outcomeBadge(allowed)
      }
    >
      {outcomeLabel(outcome)}
    </span>
  );
}

export default function Page() {
  const [page, setPage] = useState(0);
  const [draft, setDraft] = useState("");
  const [studentId, setStudentId] = useState<number | undefined>();
  const [error, setError] = useState<string>();

  const audit = useAuditLog(page, studentId);
  const counselors = useAdminCounselors(0, 100);
  const names = useMemo(() => counselorNameMap(counselors.data?.content ?? []), [counselors.data]);

  const columns: Column<AuditRow>[] = [
    { header: "Counselor", cell: (r) => counselorLabel(r.counselorId, names) },
    { header: "Student", cell: (r) => `#${r.studentId}` },
    { header: "Outcome", cell: (r) => <OutcomeBadge outcome={r.outcome} /> },
    { header: "When", cell: (r) => (r.at ? new Date(r.at).toLocaleString() : "–") },
  ];

  function apply(next: string) {
    const parsed = parseStudentIdFilter(next);
    if (!parsed.ok) {
      setError(parsed.error);
      return;
    }
    setError(undefined);
    setPage(0);
    setStudentId(parsed.id);
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Audit log</h1>
      <form
        noValidate
        className="flex flex-wrap items-end gap-3"
        onSubmit={(e) => {
          e.preventDefault();
          apply(draft);
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="audit-student-id">Student ID</Label>
          <Input
            id="audit-student-id"
            inputMode="numeric"
            value={draft}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? "audit-student-id-error" : undefined}
            onChange={(e) => setDraft(e.target.value)}
          />
        </div>
        <Button type="submit">Filter</Button>
        {studentId !== undefined && (
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setDraft("");
              apply("");
            }}
          >
            Clear
          </Button>
        )}
      </form>
      {error && (
        <p id="audit-student-id-error" role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      <PaginatedTable
        label="Audit log"
        columns={columns}
        rowKey={(r) => r.id!}
        query={audit}
        page={page}
        onPageChange={setPage}
        empty={studentId !== undefined ? `No audit entries for student #${studentId}.` : "No audit entries yet."}
      />
    </div>
  );
}
