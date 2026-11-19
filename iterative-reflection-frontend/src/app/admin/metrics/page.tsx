import { Suspense } from "react";
import { MetricsView } from "./metrics-view";

export default function Page() {
  return (
    <Suspense>
      <MetricsView />
    </Suspense>
  );
}
