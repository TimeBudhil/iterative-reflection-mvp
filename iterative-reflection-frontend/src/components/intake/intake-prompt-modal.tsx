"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useDismissIntake } from "@/lib/intake/queries";
import { useMe } from "@/lib/session/session";

/**
 * Shown on the feed while `/api/me` says `intakePromptDue` (R3.5). Both buttons call `POST /api/intake/dismiss`
 * first so the prompt doesn't return on reload; "Fill it in" then goes to the intake.
 */
export function IntakePromptModal() {
  const me = useMe();
  const dismiss = useDismissIntake();
  const router = useRouter();
  const [closed, setClosed] = useState(false);
  const open = me.data?.intakePromptDue === true && !closed;

  function finish(fillIn: boolean) {
    dismiss.mutate(undefined, {
      onSuccess: () => {
        setClosed(true);
        if (fillIn) router.push("/student/intake");
      },
      onError: () => {
        // Dismiss failing must not trap the student: let them through, the prompt may reappear.
        setClosed(true);
        if (fillIn) router.push("/student/intake");
      },
    });
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && !dismiss.isPending && finish(false)}>
      <DialogContent data-testid="intake-prompt">
        <DialogHeader>
          <DialogTitle>Tell us about your interests</DialogTitle>
          <DialogDescription>You have reflected on 3 careers. Share what interests you so we can tailor what you see next.</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" disabled={dismiss.isPending} onClick={() => finish(false)}>
            Not now
          </Button>
          <Button disabled={dismiss.isPending} onClick={() => finish(true)}>
            Fill it in
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
