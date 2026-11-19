"use client";

import { Button } from "@/components/ui/button";
import { statusCopy } from "@/lib/api/error-copy";

/**
 * Shared error panel for a 5xx or network failure (R11.6): written copy, the `X-Request-Id` for support,
 * and a retry button. Never renders `error.message`/detail (N5.2). Pass any thrown value as `error`.
 */
export function RetryPanel({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const e = error as { status?: number; requestId?: string | null } | null;
  return (
    <div role="alert" className="max-w-md space-y-3 rounded-lg border p-6" data-testid="retry-panel">
      <p className="font-medium">{statusCopy(typeof e?.status === "number" ? e.status : 500)}</p>
      {e?.requestId && (
        <p className="text-sm text-muted-foreground">
          Request ID: <code data-testid="request-id">{e.requestId}</code>
        </p>
      )}
      <Button type="button" onClick={onRetry}>
        Try again
      </Button>
    </div>
  );
}
