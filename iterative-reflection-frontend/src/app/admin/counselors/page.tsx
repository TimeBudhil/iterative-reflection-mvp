"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { PaginatedTable, type Column } from "@/components/shared/paginated-table";
import { useAdminCounselors, useCreateCounselor, useSetCounselorActive } from "@/lib/admin/counselors";
import {
  mapCreateCounselorError,
  validateNewCounselor,
  type NewCounselor,
  type NewCounselorErrors,
} from "@/lib/admin/counselor-model";
import type { components } from "@/lib/api/schema";

type CounselorRow = components["schemas"]["ListItem"];

const TEMP_PASSWORD_NOTICE = "Share this temporary password with the counselor. They must change it on first sign-in.";

function StatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={
        "inline-flex rounded-full px-2 py-0.5 text-xs font-medium " +
        (active ? "bg-emerald-100 text-emerald-900" : "bg-muted text-muted-foreground")
      }
    >
      {active ? "Active" : "Inactive"}
    </span>
  );
}

function Field({
  id,
  label,
  value,
  onChange,
  error,
  type = "text",
  autoComplete = "off",
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  type?: string;
  autoComplete?: string;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        type={type}
        autoComplete={autoComplete}
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(e) => onChange(e.target.value)}
      />
      {error && (
        <p id={`${id}-error`} role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}

const EMPTY: NewCounselor = { email: "", displayName: "", temporaryPassword: "" };

function CreateCounselorDialog({ open, onOpenChange, onCreated }: { open: boolean; onOpenChange: (o: boolean) => void; onCreated: () => void }) {
  const create = useCreateCounselor();
  const [values, setValues] = useState<NewCounselor>(EMPTY);
  const [errors, setErrors] = useState<NewCounselorErrors>({});
  const [general, setGeneral] = useState<string>();
  const [created, setCreated] = useState<{ email: string } | null>(null);

  function close(next: boolean) {
    onOpenChange(next);
    if (!next) {
      setValues(EMPTY);
      setErrors({});
      setGeneral(undefined);
      setCreated(null);
    }
  }

  const set = (k: keyof NewCounselor) => (v: string) => setValues((prev) => ({ ...prev, [k]: v }));

  return (
    <Dialog open={open} onOpenChange={(next) => (create.isPending ? undefined : close(next))}>
      <DialogContent>
        {created ? (
          <>
            <DialogHeader>
              <DialogTitle>Counselor created</DialogTitle>
              <DialogDescription>
                {created.email}: {TEMP_PASSWORD_NOTICE}
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button onClick={() => close(false)}>Done</Button>
            </DialogFooter>
          </>
        ) : (
          <form
            noValidate
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault();
              const found = validateNewCounselor(values);
              setErrors(found);
              setGeneral(undefined);
              if (Object.keys(found).length) return;
              const body = { ...values, email: values.email.trim(), displayName: values.displayName.trim() };
              create.mutate(body, {
                onSuccess: (c) => {
                  setCreated({ email: c.email ?? body.email });
                  onCreated();
                },
                onError: (err) => {
                  const m = mapCreateCounselorError(err);
                  setErrors(m.fields);
                  setGeneral(m.general);
                },
              });
            }}
          >
            <DialogHeader>
              <DialogTitle>Add counselor</DialogTitle>
              <DialogDescription>They sign in with this temporary password and must change it right away.</DialogDescription>
            </DialogHeader>
            <Field id="new-counselor-email" label="Email" type="email" autoComplete="off" value={values.email} onChange={set("email")} error={errors.email} />
            <Field id="new-counselor-name" label="Display name" value={values.displayName} onChange={set("displayName")} error={errors.displayName} />
            <Field
              id="new-counselor-password"
              label="Temporary password"
              type="password"
              autoComplete="new-password"
              value={values.temporaryPassword}
              onChange={set("temporaryPassword")}
              error={errors.temporaryPassword}
            />
            {general && (
              <p role="alert" className="text-sm text-destructive">
                {general}
              </p>
            )}
            <DialogFooter>
              <Button type="button" variant="outline" disabled={create.isPending} onClick={() => close(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={create.isPending}>
                Create counselor
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

export default function Page() {
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [target, setTarget] = useState<CounselorRow | null>(null);
  const counselors = useAdminCounselors(page);
  const setActive = useSetCounselorActive();

  const columns: Column<CounselorRow>[] = [
    { header: "Name", cell: (r) => r.displayName },
    { header: "Email", cell: (r) => r.email },
    { header: "Join code", cell: (r) => (r.joinCode ? <code className="font-mono">{r.joinCode}</code> : "–") },
    { header: "Status", cell: (r) => <StatusBadge active={!!r.active} /> },
    { header: "Students", cell: (r) => r.linkedStudentCount ?? 0, className: "text-right" },
    {
      header: "Actions",
      cell: (r) => (
        <Button variant="outline" size="sm" onClick={() => setTarget(r)}>
          {r.active ? "Deactivate" : "Reactivate"}
          <span className="sr-only"> {r.displayName}</span>
        </Button>
      ),
    },
  ];

  const deactivating = !!target?.active;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Counselors</h1>
        <Button onClick={() => setCreating(true)}>
          <Plus className="size-4" aria-hidden /> Add counselor
        </Button>
      </div>
      <PaginatedTable
        label="Counselors"
        columns={columns}
        rowKey={(r) => r.id!}
        query={counselors}
        page={page}
        onPageChange={setPage}
        empty="No counselors yet. Add one to get started."
      />
      <CreateCounselorDialog open={creating} onOpenChange={setCreating} onCreated={() => setPage(0)} />
      <ConfirmDialog
        open={target !== null}
        onOpenChange={(o) => !o && setTarget(null)}
        title={deactivating ? `Deactivate ${target?.displayName}?` : `Reactivate ${target?.displayName}?`}
        description={
          deactivating
            ? "They are signed out immediately and can't sign in until reactivated."
            : "They can sign in again."
        }
        confirmLabel={deactivating ? "Deactivate" : "Reactivate"}
        destructive={deactivating}
        pending={setActive.isPending}
        onConfirm={() =>
          target &&
          setActive.mutate(
            { id: target.id!, active: !target.active },
            {
              onSuccess: () => {
                setTarget(null);
                toast.success(deactivating ? "Counselor deactivated" : "Counselor reactivated");
              },
              onError: () => toast.error("Couldn't update the counselor. Please try again."),
            },
          )
        }
      />
    </div>
  );
}
