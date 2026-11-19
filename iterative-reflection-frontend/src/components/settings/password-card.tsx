"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useChangePassword } from "@/lib/settings/api";
import { validatePasswordChange, type PasswordErrors } from "@/lib/settings/model";
import { SettingsCard } from "./settings-card";

function Field({
  id,
  label,
  value,
  onChange,
  error,
  autoComplete,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  autoComplete: string;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        type="password"
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

export function PasswordCard() {
  const change = useChangePassword();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [errors, setErrors] = useState<PasswordErrors>({});

  return (
    <SettingsCard id="password" title="Password" description="Use 12 to 72 characters.">
      <form
        className="max-w-md space-y-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          const found = validatePasswordChange({ current, next, confirm });
          setErrors(found);
          if (Object.keys(found).length) return;
          change.mutate(
            { currentPassword: current, newPassword: next },
            {
              onSuccess: () => {
                setCurrent("");
                setNext("");
                setConfirm("");
                toast.success("Password changed.");
              },
              onError: (err) => {
                const { status, fieldErrors } = err as { status?: number; fieldErrors?: Record<string, string> };
                if (fieldErrors?.newPassword) setErrors({ next: fieldErrors.newPassword });
                else if (status === 400) setErrors({ current: "Current password is incorrect." });
                else setErrors({ current: "Could not change password. Please try again." });
              },
            },
          );
        }}
      >
        <Field id="current-password" label="Current password" autoComplete="current-password" value={current} onChange={setCurrent} error={errors.current} />
        <Field id="new-password" label="New password" autoComplete="new-password" value={next} onChange={setNext} error={errors.next} />
        <Field id="confirm-password" label="Confirm new password" autoComplete="new-password" value={confirm} onChange={setConfirm} error={errors.confirm} />
        <Button type="submit" disabled={change.isPending}>
          Change password
        </Button>
      </form>
    </SettingsCard>
  );
}
