"use client";

import { useState } from "react";
import { toast } from "sonner";
import { QueryView } from "@/components/feedback/query-view";
import { VerdictBadge } from "@/components/shared/verdict-badge";
import { buttonVariants } from "@/components/ui/button";
import { ProgressBar } from "@/components/ui/progress-bar";
import { statusCopy } from "@/lib/api/error-copy";
import { cn } from "@/lib/utils";
import { useAdminTheme, useSaveTheme } from "@/lib/theme/admin-theme";
import { resolvePresetId, type PresetId } from "@/lib/theme/presets";
import { canSaveTheme, lastChangedLine, PRESET_OPTIONS } from "@/lib/theme/theme-card-model";
import { SettingsCard } from "./settings-card";

const SWATCH_SLOTS = ["bg-background", "bg-primary", "bg-highlight", "bg-positive"] as const;

/** A mini feed card drawn in one preset through a scoped `data-theme`; the page itself stays in the saved theme. */
function MockCard({ presetId }: { presetId: PresetId }) {
  return (
    <div
      aria-hidden="true"
      data-theme={presetId}
      data-testid={`mock-${presetId}`}
      className="space-y-3 rounded-lg border border-border bg-background p-3 text-foreground"
    >
      <div className="flex gap-1.5">
        {SWATCH_SLOTS.map((slot) => (
          <span key={slot} className={cn("size-5 rounded-full border border-border", slot)} />
        ))}
      </div>
      <div className="space-y-2 rounded-md border border-border bg-surface p-3">
        <p className="text-sm font-semibold">Marine biologist</p>
        <div className="flex flex-wrap gap-1.5">
          <VerdictBadge verdict="YES" className="rounded-full" />
          <VerdictBadge verdict="MAYBE" className="rounded-full" />
          <VerdictBadge verdict="NO" className="rounded-full" />
        </div>
        <ProgressBar fraction={0.6} value={3} max={5} label="Progress" />
        <span className={cn(buttonVariants({ size: "sm" }), "pointer-events-none")}>Start</span>
      </div>
    </div>
  );
}

function ThemeForm({ saved, updatedAt, updatedByName }: { saved: PresetId; updatedAt?: string | null; updatedByName?: string | null }) {
  const save = useSaveTheme();
  // The radio shows the saved preset until the admin picks another; success and failure both drop the pick.
  const [picked, setPicked] = useState<PresetId | null>(null);
  const selected = picked ?? saved;
  const enabled = canSaveTheme(selected, saved, save.isPending);
  const line = lastChangedLine(updatedAt, updatedByName);

  return (
    <form
      className="space-y-4"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        if (!enabled) return;
        save.mutate(selected, {
          onSuccess: () => {
            setPicked(null);
            toast.success("Theme saved.");
          },
          onError: (err) => {
            setPicked(null);
            toast.error(statusCopy((err as { status?: number }).status ?? 0));
          },
        });
      }}
    >
      <fieldset className="space-y-3">
        <legend className="sr-only">App theme</legend>
        <div className="grid gap-3 sm:grid-cols-2">
          {PRESET_OPTIONS.map(({ id, name }) => (
            <label
              key={id}
              className="flex cursor-pointer flex-col gap-2 rounded-lg border border-border p-3 has-[:checked]:border-primary has-[:checked]:ring-2 has-[:checked]:ring-ring has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring"
            >
              <span className="flex items-center gap-2 text-sm font-medium">
                <input
                  type="radio"
                  name="app-theme"
                  value={id}
                  checked={selected === id}
                  onChange={() => setPicked(id)}
                  className="size-4 accent-primary"
                />
                {name}
              </span>
              <MockCard presetId={id} />
            </label>
          ))}
        </div>
      </fieldset>
      {line && (
        <p data-testid="theme-last-changed" className="text-sm text-muted-foreground">
          {line}
        </p>
      )}
      {/* aria-disabled (not disabled) so focus stays on Save through the pending state and after a save. */}
      <button
        type="submit"
        aria-disabled={!enabled}
        className={cn(buttonVariants(), "aria-disabled:pointer-events-none aria-disabled:opacity-50")}
      >
        Save
      </button>
    </form>
  );
}

/** Admin-only "App theme" card: preview presets on mock cards, save one for everybody. */
export function ThemeCard() {
  const theme = useAdminTheme();
  return (
    <SettingsCard id="app-theme" title="App theme" description="Choose the look of the app for everyone. Changes show on each person's next page load.">
      <QueryView query={theme}>
        {(t) => <ThemeForm saved={resolvePresetId(t.presetId)} updatedAt={t.updatedAt} updatedByName={t.updatedByName} />}
      </QueryView>
    </SettingsCard>
  );
}
