"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useUpdateDisplayName } from "@/lib/settings/api";
import { validateDisplayName } from "@/lib/settings/model";
import type { Me } from "@/lib/session/session";
import { SettingsCard } from "./settings-card";

export function ProfileCard({ me }: { me: Me }) {
  const update = useUpdateDisplayName();
  const [name, setName] = useState(me.displayName);
  const [error, setError] = useState<string | null>(null);

  return (
    <SettingsCard id="profile" title="Profile">
      <form
        className="max-w-md space-y-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          const result = validateDisplayName(name);
          if (!result.ok) return setError(result.error);
          setError(null);
          update.mutate(result.value, {
            onSuccess: (updated) => {
              setName(updated.displayName);
              toast.success("Display name updated.");
            },
            onError: (err) =>
              setError((err as { fieldErrors?: Record<string, string> }).fieldErrors?.displayName ?? "Could not save. Please try again."),
          });
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="profile-email">Email</Label>
          <Input id="profile-email" type="email" value={me.email} readOnly disabled />
        </div>
        <div className="space-y-2">
          <Label htmlFor="profile-name">Display name</Label>
          <Input
            id="profile-name"
            value={name}
            maxLength={100}
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? "profile-name-error" : undefined}
            onChange={(e) => setName(e.target.value)}
          />
          {error && (
            <p id="profile-name-error" role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
        </div>
        <Button type="submit" disabled={update.isPending}>
          Save
        </Button>
      </form>
    </SettingsCard>
  );
}
