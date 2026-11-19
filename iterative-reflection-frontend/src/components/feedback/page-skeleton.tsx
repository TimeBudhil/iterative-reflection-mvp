import { Skeleton } from "@/components/ui/skeleton";

/** Shared loading state for data pages (R11.6): a heading bar plus content blocks. */
export function PageSkeleton() {
  return (
    <div aria-busy="true" aria-label="Loading" className="space-y-4" data-testid="page-skeleton">
      <Skeleton className="h-8 w-64" />
      <Skeleton className="h-40 w-full max-w-2xl" />
      <Skeleton className="h-24 w-full max-w-2xl" />
    </div>
  );
}
