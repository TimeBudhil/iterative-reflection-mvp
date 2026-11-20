import { cn } from "cn"

/**
 * Slot-styled progressbar: a `muted` track filled in `highlight`. `fraction` (0..1, from the caller's model) drives the fill;
 * `value` / `max` are the ARIA current and maximum.
 */
function ProgressBar({
  fraction,
  value,
  max,
  label,
  className,
}: {
  fraction: number
  value: number
  max: number
  label: string
  className?: string
}) {
  return (
    <div
      role="progressbar"
      data-slot="progress-bar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={value}
      className={cn("h-2 overflow-hidden rounded-full bg-muted", className)}
    >
      <div className="h-full rounded-full bg-highlight transition-[width]" style={{ width: `${fraction * 100}%` }} />
    </div>
  )
}

export { ProgressBar }
