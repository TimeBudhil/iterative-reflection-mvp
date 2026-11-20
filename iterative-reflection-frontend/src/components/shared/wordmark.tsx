import { cn } from "@/lib/utils";

/** The "Iterative Reflection" wordmark: semibold Geist text in the primary color, no logo image. */
export function Wordmark({ className }: { className?: string }) {
  return (
    <span data-testid="wordmark" className={cn("font-semibold text-primary", className)}>
      Iterative Reflection
    </span>
  );
}
