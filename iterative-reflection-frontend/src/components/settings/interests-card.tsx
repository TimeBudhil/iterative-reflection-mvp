"use client";

import Link from "next/link";
import { RetryPanel } from "@/components/shared/retry-panel";
import { Skeleton } from "@/components/ui/skeleton";
import { useIntake } from "@/lib/intake/queries";
import { interestsStatus } from "@/lib/intake/model";
import { SettingsCard } from "./settings-card";

export function InterestsCard() {
  const intake = useIntake();
  return (
    <SettingsCard id="interests" title="Interests" description="What you tell us here helps tailor your careers.">
      {intake.isPending ? (
        <Skeleton className="h-9 w-48" />
      ) : intake.isError ? (
        <RetryPanel error={intake.error} onRetry={() => void intake.refetch()} />
      ) : (
        <div className="flex items-center justify-between gap-3">
          <p data-testid="interests-status">{interestsStatus(intake.data !== null)}</p>
          <Link href="/student/intake" className="underline">
            {intake.data !== null ? "Edit interests" : "Fill in interests"}
          </Link>
        </div>
      )}
    </SettingsCard>
  );
}
