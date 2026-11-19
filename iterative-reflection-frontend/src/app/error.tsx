"use client";

import { RetryPanel } from "@/components/shared/retry-panel";

/** Last-resort boundary for unexpected render errors: written copy only, never the error text (N5.2). */
export default function Error({ reset }: { error: Error; reset: () => void }) {
  return (
    <div className="p-8">
      <RetryPanel error={{ status: 500 }} onRetry={reset} />
    </div>
  );
}
