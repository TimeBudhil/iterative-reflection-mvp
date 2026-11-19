"use client";

import { useState } from "react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { RetryPanel } from "@/components/shared/retry-panel";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError } from "@/lib/api/client";
import { useCounselorLinks, useLinkCounselor, useRevokeCounselor } from "@/lib/counselor-links/api";
import { formatLinkedDate, linkErrorMessage, normalizeCode, revokeDescription } from "@/lib/counselor-links/model";
import { SettingsCard } from "./settings-card";

export function MyCounselorsCard() {
  const links = useCounselorLinks();
  const link = useLinkCounselor();
  const revoke = useRevokeCounselor();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [revoking, setRevoking] = useState<{ id: number; name: string } | null>(null);

  return (
    <SettingsCard id="my-counselors" title="My counselors" description="Counselors you link can see your reflections.">
      <div className="space-y-6">
        {links.isPending ? (
          <Skeleton className="h-12 w-full" />
        ) : links.isError ? (
          <RetryPanel error={links.error} onRetry={() => links.refetch()} />
        ) : links.data.length === 0 ? (
          <p className="text-sm text-muted-foreground">You have not linked a counselor yet.</p>
        ) : (
          <ul className="divide-y" aria-label="Linked counselors">
            {links.data.map((l) => (
              <li key={l.id} className="flex items-center justify-between gap-3 py-3" data-testid="counselor-link">
                <div>
                  <p className="font-medium">
                    {l.displayName}
                    {l.counselorActive === false && (
                      <span className="ml-2 rounded bg-muted px-1.5 py-0.5 text-xs font-normal text-muted-foreground">Inactive</span>
                    )}
                  </p>
                  <p className="text-sm text-muted-foreground">Linked {formatLinkedDate(l.linkedAt!)}</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => setRevoking({ id: l.id!, name: l.displayName! })}>
                  Revoke
                </Button>
              </li>
            ))}
          </ul>
        )}

        <form
          className="max-w-md space-y-2"
          noValidate
          onSubmit={(e) => {
            e.preventDefault();
            const value = normalizeCode(code);
            if (!value) return setError("Enter a join code.");
            setError(null);
            link.mutate(value, {
              onSuccess: () => {
                setCode("");
                toast.success("Counselor linked.");
              },
              onError: (err) => setError(linkErrorMessage(err instanceof ApiError ? err.status : 0)),
            });
          }}
        >
          <Label htmlFor="counselor-code">Join code</Label>
          <div className="flex gap-2">
            <Input
              id="counselor-code"
              value={code}
              maxLength={32}
              autoComplete="off"
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? "counselor-code-error" : undefined}
              onChange={(e) => setCode(e.target.value)}
            />
            <Button type="submit" disabled={link.isPending}>
              Link counselor
            </Button>
          </div>
          {error && (
            <p id="counselor-code-error" role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
        </form>
      </div>

      <ConfirmDialog
        open={revoking !== null}
        onOpenChange={(open) => !open && setRevoking(null)}
        title="Revoke counselor?"
        description={revoking ? revokeDescription(revoking.name) : ""}
        confirmLabel="Revoke"
        destructive
        pending={revoke.isPending}
        onConfirm={() =>
          revoking &&
          revoke.mutate(revoking.id, {
            onSuccess: () => {
              setRevoking(null);
              toast.success("Counselor removed.");
            },
            onError: () => {
              setRevoking(null);
              toast.error("Could not revoke. Please try again.");
            },
          })
        }
      />
    </SettingsCard>
  );
}
