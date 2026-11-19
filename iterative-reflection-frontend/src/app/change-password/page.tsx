"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { roleHome, validatePasswordChange, type PasswordFormErrors } from "@/lib/session/model";
import { isUnauthorized, useChangePassword, useMe, useSignOut } from "@/lib/session/session";

type ApiLikeError = { status?: number; detail?: string; fieldErrors?: Record<string, string> };

/** Maps a failed PUT onto the form: field errors by name; a field-less 400 is the wrong current password. */
function serverErrors(error: unknown): { fields: PasswordFormErrors; general: string | null } {
  const e = error as ApiLikeError;
  if (e?.status === 400) {
    const fields: PasswordFormErrors = {};
    if (e.fieldErrors?.newPassword) fields.newPassword = e.fieldErrors.newPassword;
    if (e.fieldErrors?.currentPassword) fields.currentPassword = e.fieldErrors.currentPassword;
    if (Object.keys(fields).length === 0) fields.currentPassword = e.detail || "Current password is incorrect.";
    return { fields, general: null };
  }
  return { fields: {}, general: "Something went wrong. Please try again." };
}

function Field({
  id,
  label,
  error,
  ...props
}: { id: string; label: string; error?: string } & React.ComponentProps<typeof Input>) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        type="password"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        {...props}
      />
      {error && (
        <p id={`${id}-error`} className="text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}

export default function ChangePasswordPage() {
  const router = useRouter();
  const me = useMe();
  const change = useChangePassword();
  const signOut = useSignOut();
  const [values, setValues] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [errors, setErrors] = useState<PasswordFormErrors>({});
  const [general, setGeneral] = useState<string | null>(null);

  const user = me.data;

  // Signed out -> /signin; nobody who isn't forced to change gets to stay here.
  useEffect(() => {
    if (isUnauthorized(me.error)) router.replace("/signin");
    else if (user && !user.mustChangePassword && !change.isPending && !change.isSuccess) {
      router.replace(roleHome(user.role));
    }
  }, [me.error, user, change.isPending, change.isSuccess, router]);

  const set = (key: keyof typeof values) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setValues((v) => ({ ...v, [key]: e.target.value }));

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center p-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-xl">Choose a new password</CardTitle>
          <p className="text-sm text-muted-foreground">
            You&apos;re using a temporary password. Pick a new one to continue.
          </p>
        </CardHeader>
        <CardContent>
          {user?.mustChangePassword ? (
            <form
              className="space-y-4"
              noValidate
              onSubmit={(e) => {
                e.preventDefault();
                setGeneral(null);
                const local = validatePasswordChange(values);
                setErrors(local);
                if (Object.keys(local).length > 0) return;
                change.mutate(
                  { currentPassword: values.currentPassword, newPassword: values.newPassword },
                  {
                    onSuccess: (fresh) => router.replace(roleHome(fresh.role)),
                    onError: (err) => {
                      const next = serverErrors(err);
                      setErrors(next.fields);
                      setGeneral(next.general);
                    },
                  },
                );
              }}
            >
              <Field
                id="currentPassword"
                label="Current password"
                autoComplete="current-password"
                value={values.currentPassword}
                onChange={set("currentPassword")}
                error={errors.currentPassword}
              />
              <Field
                id="newPassword"
                label="New password"
                autoComplete="new-password"
                value={values.newPassword}
                onChange={set("newPassword")}
                error={errors.newPassword}
              />
              <Field
                id="confirm"
                label="Confirm new password"
                autoComplete="new-password"
                value={values.confirm}
                onChange={set("confirm")}
                error={errors.confirm}
              />
              {general && (
                <p role="alert" className="text-sm text-destructive">
                  {general}
                </p>
              )}
              <Button type="submit" className="w-full" disabled={change.isPending}>
                Change password
              </Button>
              <Button
                type="button"
                variant="ghost"
                className="w-full"
                disabled={signOut.isPending}
                onClick={() => signOut.mutate()}
              >
                Sign out
              </Button>
            </form>
          ) : (
            <div aria-busy="true" aria-label="Loading" className="space-y-3">
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
