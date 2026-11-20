import { cn } from "cn"

/** Slot-styled progressbar: a `muted` track filled in `highlight`. Fill width is value / max. */
function ProgressBar({
  value,
  max,
  label,
  className,
}: {
  value: number
  max: number
  label: string
  className?: string
}) {
  const pct = max > 0 ? Math.min(Math.max(value / max, 0), 1) * 100 : 0
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
      <div className="h-full rounded-full bg-highlight transition-[width]" style={{ width: `${pct}%` }} />
    </div>
  )
}

export { ProgressBar }
