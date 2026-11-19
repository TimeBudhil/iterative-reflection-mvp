"use client";

import type { ReactNode } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { RetryPanel } from "@/components/shared/retry-panel";
import { pageSummary, type Paged } from "@/lib/pagination/model";

export type Column<T> = { header: string; cell: (row: T) => ReactNode; className?: string };

type PaginatedTableProps<T> = {
  /** Accessible name for the table. */
  label: string;
  columns: Column<T>[];
  rowKey: (row: T) => string | number;
  /** The slice of a TanStack Query result the table needs. */
  query: { data: Paged<T> | undefined; isPending: boolean; error: unknown; refetch: () => unknown };
  /** 0-based page index (the backend's `page.number`). */
  page: number;
  onPageChange: (page: number) => void;
  /** Shown instead of the table when there are no rows at all. */
  empty: ReactNode;
};

/** Paginated table bound to the backend's `{content, page}` envelope. Server order is kept as-is. */
export function PaginatedTable<T>({ label, columns, rowKey, query, page, onPageChange, empty }: PaginatedTableProps<T>) {
  const { data, isPending, error, refetch } = query;

  if (error && !data) return <RetryPanel error={error} onRetry={() => void refetch()} />;
  if (isPending) {
    return (
      <div className="space-y-2" aria-busy="true" aria-label="Loading">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }
  if (!data) return null;
  if (data.page.totalElements === 0) return <p className="text-muted-foreground">{empty}</p>;

  const s = pageSummary(data.page);
  return (
    <div className="space-y-3">
      <Table aria-label={label}>
        <TableHeader>
          <TableRow>
            {columns.map((c) => (
              <TableHead key={c.header} className={c.className}>
                {c.header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.content.map((row) => (
            <TableRow key={rowKey(row)}>
              {columns.map((c) => (
                <TableCell key={c.header} className={c.className}>
                  {c.cell(row)}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <nav aria-label={`${label} pages`} className="flex items-center justify-between text-sm">
        <span aria-live="polite">
          {s.from}–{s.to} of {s.total}
        </span>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" disabled={!s.hasPrev} onClick={() => onPageChange(page - 1)}>
            <ChevronLeft className="size-4" aria-hidden /> Previous
          </Button>
          <span>
            Page {s.pageNumber} of {s.totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={!s.hasNext} onClick={() => onPageChange(page + 1)}>
            Next <ChevronRight className="size-4" aria-hidden />
          </Button>
        </div>
      </nav>
    </div>
  );
}
