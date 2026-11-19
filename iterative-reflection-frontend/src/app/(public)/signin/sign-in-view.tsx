"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { landingFor } from "@/lib/session/model";
import { useMe } from "@/lib/session/session";
import { SignInForm } from "./sign-in-form";

export function SignInView() {
  const router = useRouter();
  const params = useSearchParams();
  const mode = params.get("mode") === "signup" ? "signup" : "signin";
  const me = useMe();

  // A signed-in user never sees the form (R1.8).
  useEffect(() => {
    if (me.data) router.replace(landingFor(me.data));
  }, [me.data, router]);

  const ready = !me.isPending && !me.data;

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center p-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-xl">Iterative Reflection</CardTitle>
        </CardHeader>
        <CardContent>
          {ready ? (
            <Tabs
              value={mode}
              onValueChange={(next) => router.replace(next === "signup" ? "/signin?mode=signup" : "/signin")}
            >
              <TabsList className="mb-4 w-full">
                <TabsTrigger value="signin">Sign in</TabsTrigger>
                <TabsTrigger value="signup">Sign up</TabsTrigger>
              </TabsList>
              <TabsContent value="signin">
                <SignInForm />
              </TabsContent>
              <TabsContent value="signup">
                {/* Sign up form arrives with ticket 06. */}
                <p className="text-sm text-muted-foreground">Sign up is coming soon.</p>
              </TabsContent>
            </Tabs>
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
