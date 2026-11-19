"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { postSignInPath } from "@/lib/session/model";
import { useMe } from "@/lib/session/session";
import { SignInForm } from "./sign-in-form";
import { SignUpForm } from "./sign-up-form";

export function SignInView() {
  const router = useRouter();
  const params = useSearchParams();
  const mode = params.get("mode") === "signup" ? "signup" : "signin";
  const next = params.get("next");
  const expired = params.get("expired") === "1";
  const me = useMe();

  // A signed-in user never sees the form (R1.8).
  useEffect(() => {
    if (me.data) router.replace(postSignInPath(me.data, next));
  }, [me.data, next, router]);

  const ready = !me.isPending && !me.data;

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center p-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-xl">Iterative Reflection</CardTitle>
        </CardHeader>
        <CardContent>
          {ready ? (
            <>
            {expired && (
              <p role="status" className="mb-4 rounded-md bg-muted p-3 text-sm">
                Your session expired. Please sign in again.
              </p>
            )}
            <Tabs
              value={mode}
              onValueChange={(tab) => {
                const q = new URLSearchParams(params.toString());
                if (tab === "signup") q.set("mode", "signup");
                else q.delete("mode");
                const qs = q.toString();
                router.replace(qs ? `/signin?${qs}` : "/signin");
              }}
            >
              <TabsList className="mb-4 w-full">
                <TabsTrigger value="signin">Sign in</TabsTrigger>
                <TabsTrigger value="signup">Sign up</TabsTrigger>
              </TabsList>
              <TabsContent value="signin">
                <SignInForm />
              </TabsContent>
              <TabsContent value="signup">
                <SignUpForm />
              </TabsContent>
            </Tabs>
            </>
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
