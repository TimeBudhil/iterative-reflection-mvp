"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useAbandonCareer } from "@/lib/experience/abandon";
import { statusCopy } from "@/lib/api/error-copy";

/** "This isn't for me" (R4.4): confirm, abandon, then back to the feed for a new card. */
export function AbandonButton({ careerId }: { careerId: number }) {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const abandon = useAbandonCareer(careerId);

  return (
    <>
      <Button variant="outline" onClick={() => setOpen(true)}>
        This isn&apos;t for me
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={setOpen}
        title="Skip this career?"
        description="We'll take you back to the feed and show you a different career. You can't undo this."
        confirmLabel="Yes, skip it"
        cancelLabel="Keep reading"
        pending={abandon.isPending}
        onConfirm={() =>
          abandon.mutate(undefined, {
            onSuccess: () => router.push("/student/feed"),
            onError: (e) => {
              setOpen(false);
              toast.error(statusCopy((e as { status?: number }).status ?? 0));
            },
          })
        }
      />
    </>
  );
}
