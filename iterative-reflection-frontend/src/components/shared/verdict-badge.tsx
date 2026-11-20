import { cn } from "@/lib/utils";
import { verdictStyle } from "@/lib/theme/status-styles";
import { isVerdict } from "@/lib/verdict";

/** A verdict chip in the shared verdict colors. Renders nothing when the value isn't a known verdict. */
export function VerdictBadge({ verdict, className }: { verdict: string | null | undefined; className?: string }) {
  if (!isVerdict(verdict)) return null;
  return <span className={cn("px-2 py-0.5 text-xs font-semibold", verdictStyle(verdict).badge, className)}>{verdict}</span>;
}
