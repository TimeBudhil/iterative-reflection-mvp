"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api/client";
import { roleHome } from "@/lib/session/model";
import { useSignIn } from "@/lib/session/session";
import { mapSignupError, TERMS_VERSION, validateSignup, type FieldErrors, type SignupField, type SignupValues } from "@/lib/signup/model";

const EMPTY: SignupValues = { email: "", password: "", displayName: "", birthdate: "", counselorCode: "" };

function Field({
  id,
  label,
  error,
  hint,
  children,
}: {
  id: SignupField;
  label: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {hint && !error && (
        <p id={`${id}-hint`} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}

export function SignUpForm() {
  const router = useRouter();
  const signIn = useSignIn();
  const [values, setValues] = useState<SignupValues>(EMPTY);
  const [accepted, setAccepted] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [general, setGeneral] = useState<string | undefined>();

  // Signup does not start a session, so sign in with the same credentials once it returns 201.
  const signUp = useMutation({
    mutationFn: async (v: SignupValues) => {
      const code = v.counselorCode.trim();
      await api.POST("/api/auth/signup", {
        body: {
          email: v.email.trim(),
          password: v.password,
          displayName: v.displayName.trim(),
          birthdate: v.birthdate,
          acceptedTermsVersion: TERMS_VERSION,
          ...(code ? { counselorCode: code } : {}),
        },
      });
      return signIn.mutateAsync({ email: v.email.trim(), password: v.password });
    },
  });

  const set = (field: SignupField) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setValues((prev) => ({ ...prev, [field]: e.target.value }));
  const props = (field: SignupField) => ({
    id: field,
    value: values[field],
    onChange: set(field),
    "aria-invalid": errors[field] ? true : undefined,
    "aria-describedby": errors[field] ? `${field}-error` : field === "password" ? "password-hint" : undefined,
  });

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        setGeneral(undefined);
        const local = validateSignup(values);
        setErrors(local);
        if (Object.keys(local).length > 0) return;
        signUp.mutate(values, {
          onSuccess: (me) => router.replace(roleHome(me.role)),
          onError: (error) => {
            const mapped = mapSignupError(error);
            setErrors(mapped.fields);
            setGeneral(mapped.general);
          },
        });
      }}
    >
      <Field id="email" label="Email" error={errors.email}>
        <Input {...props("email")} type="email" autoComplete="username" required />
      </Field>
      <Field id="password" label="Password" error={errors.password} hint="12+ characters">
        <Input {...props("password")} type="password" autoComplete="new-password" required />
      </Field>
      <Field id="displayName" label="Display name" error={errors.displayName}>
        <Input {...props("displayName")} type="text" autoComplete="nickname" required />
      </Field>
      <Field id="birthdate" label="Birthdate" error={errors.birthdate}>
        <Input {...props("birthdate")} type="date" autoComplete="bday" required />
      </Field>
      <Field id="counselorCode" label="Counselor code (optional)" error={errors.counselorCode}>
        <Input {...props("counselorCode")} type="text" autoComplete="off" />
      </Field>
      <div className="flex items-start gap-2">
        <input
          id="terms"
          type="checkbox"
          className="mt-1 size-4"
          checked={accepted}
          onChange={(e) => setAccepted(e.target.checked)}
        />
        <Label htmlFor="terms" className="font-normal leading-snug">
          I agree to the&nbsp;
          <Link href="/terms" target="_blank" className="underline">
            terms
          </Link>
        </Label>
      </div>
      {general && (
        <p role="alert" className="text-sm text-destructive">
          {general}
        </p>
      )}
      <Button type="submit" className="w-full" disabled={!accepted || signUp.isPending}>
        Create account
      </Button>
    </form>
  );
}
