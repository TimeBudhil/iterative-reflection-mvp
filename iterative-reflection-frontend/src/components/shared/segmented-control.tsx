"use client";

import { useId } from "react";
import { cn } from "@/lib/utils";

export type SegmentedOption<T extends string | number> = { value: T; label: string };

/**
 * A row of mutually exclusive choices built on native radio inputs, so Tab, arrow keys and Space work
 * without custom key handling. Used for the 1-10 scales and the YES / MAYBE / NO verdict.
 */
export function SegmentedControl<T extends string | number>({
  label,
  options,
  value,
  onChange,
  disabled,
  describedBy,
}: {
  label: string;
  options: SegmentedOption<T>[];
  value: T | undefined;
  onChange: (value: T) => void;
  disabled?: boolean;
  describedBy?: string;
}) {
  const name = useId();
  return (
    <div role="radiogroup" aria-label={label} aria-describedby={describedBy} className="flex flex-wrap gap-1">
      {options.map((o) => (
        <label
          key={String(o.value)}
          className={cn(
            "inline-flex h-9 min-w-9 cursor-pointer items-center justify-center rounded-md border px-3 text-sm font-medium has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring",
            value === o.value ? "border-primary bg-primary text-primary-foreground" : "bg-background hover:bg-accent",
            disabled && "pointer-events-none opacity-50",
          )}
        >
          <input
            type="radio"
            name={name}
            className="sr-only"
            value={String(o.value)}
            checked={value === o.value}
            disabled={disabled}
            onChange={() => onChange(o.value)}
          />
          {o.label}
        </label>
      ))}
    </div>
  );
}
