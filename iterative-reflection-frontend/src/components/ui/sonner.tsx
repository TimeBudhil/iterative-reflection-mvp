"use client";

import { Toaster as Sonner, type ToasterProps } from "sonner";

/** Mounted once in `app/providers.tsx`. Anywhere in the app: `import { toast } from "sonner"` then `toast.success("Saved")`. */
export function Toaster(props: ToasterProps) {
  return (
    <Sonner
      className="toaster group"
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
        } as React.CSSProperties
      }
      {...props}
    />
  );
}
