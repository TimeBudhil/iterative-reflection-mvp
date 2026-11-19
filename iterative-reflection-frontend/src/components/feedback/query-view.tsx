"use client";

import type { UseQueryResult } from "@tanstack/react-query";
import { PageSkeleton } from "./page-skeleton";
import { RetryPanel } from "./retry-panel";
import { statusCopy } from "@/lib/api/error-copy";

/**
 * The standard way for a data page to render a query: skeleton while loading, retry panel (with request ID)
 * on error, children once data is there. 401/403 are handled globally by the session hooks, so while those
 * are in flight the skeleton stays up rather than flashing an error.
 */
export function QueryView<T>({
  query,
  children,
}: {
  query: UseQueryResult<T>;
  children: (data: T) => React.ReactNode;
}) {
  if (query.data !== undefined) return <>{children(query.data)}</>;
  if (query.isError) {
    const status = (query.error as { status?: number } | null)?.status;
    if (status === 401 || status === 403) return <PageSkeleton />;
    if (status === undefined || status === 0 || status >= 500) {
      return <RetryPanel error={query.error} onRetry={() => void query.refetch()} />;
    }
    return (
      <div role="alert" className="max-w-md rounded-lg border p-6">
        {statusCopy(status)}
      </div>
    );
  }
  return <PageSkeleton />;
}
