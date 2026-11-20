"use client";

import { useState } from "react";
import { PaginatedTable, type Column } from "@/components/shared/paginated-table";
import { useAdminActivity } from "@/lib/admin/activity";
import { actionLabel, detailText } from "@/lib/admin/activity-model";
import type { components } from "@/lib/api/schema";

type ActivityRow = components["schemas"]["ActivityEntry"];

export function ActivityView() {
  const [page, setPage] = useState(0);
  const activity = useAdminActivity(page);

  const columns: Column<ActivityRow>[] = [
    { header: "When", cell: (r) => (r.createdAt ? new Date(r.createdAt).toLocaleString() : "–") },
    { header: "Admin", cell: (r) => r.adminName ?? "–" },
    { header: "Action", cell: (r) => actionLabel(r.action) },
    { header: "Details", cell: (r) => detailText(r.action, r.detail) },
  ];

  return (
    <PaginatedTable
      label="Admin activity"
      columns={columns}
      rowKey={(r) => `${r.createdAt}-${r.action}-${JSON.stringify(r.detail)}`}
      query={activity}
      page={page}
      onPageChange={setPage}
      empty="No admin activity yet."
    />
  );
}
