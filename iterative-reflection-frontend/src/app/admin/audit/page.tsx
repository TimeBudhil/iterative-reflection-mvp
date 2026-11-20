import { Suspense } from "react";
import { AuditTabs } from "./audit-tabs";

export default function Page() {
  return (
    <Suspense>
      <AuditTabs />
    </Suspense>
  );
}
