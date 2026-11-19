"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { postSignInPath } from "@/lib/session/model";
import { useSignIn } from "@/lib/session/session";

function errorCopy(error: unknown): string {
  const status = (error as { status?: number } | null)?.status;
  if (status === 401) return "Email or password is incorrect.";
  if (status === 429) return "Too many attempts. Try again in 15 minutes.";
  return "Something went wrong. Please try again.";
}

export function SignInForm() {
  const router = useRouter();
  const next = useSearchParams().get("next");
  const signIn = useSignIn();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <form
      className="space-y-4"
      onSubmit={(e) => {
        e.preventDefault();
        signIn.mutate(
          { email, password },
          { onSuccess: (me) => router.replace(postSignInPath(me, next)) },
        );
      }}
    >
      <div className="space-y-2">
        <Label htmlFor="email">Email</Label>
        <Input id="email" type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
      </div>
      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>
      {signIn.error && (
        <p role="alert" className="text-sm text-destructive">
          {errorCopy(signIn.error)}
        </p>
      )}
      <Button type="submit" className="w-full" disabled={signIn.isPending}>
        Sign in
      </Button>
    </form>
  );
}
