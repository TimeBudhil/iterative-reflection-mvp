"use client";

import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api/client";

/** Unexpected-error panel (R11.6): a retry button plus the response's X-Request-Id for support. */
export function RetryPanel({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const requestId = error instanceof ApiError ? error.requestId : null;
  return (
    <div role="alert" className="space-y-3 rounded-lg border p-6">
      <p className="font-medium">Something went wrong. Please try again.</p>
      {requestId && <p className="text-sm text-muted-foreground">Request ID: {requestId}</p>}
      <Button variant="outline" onClick={onRetry}>
        Retry
      </Button>
    </div>
  );
}
