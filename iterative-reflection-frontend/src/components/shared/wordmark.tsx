import { cn } from "@/lib/utils";

/** The "Iterative Reflection" wordmark: semibold Geist text in the primary color, no logo image. */
export function Wordmark({ className }: { className?: string }) {
  return (
    <span className={cn("font-semibold text-primary", className)}>
      Iterative Reflection
    </span>
  );
}
