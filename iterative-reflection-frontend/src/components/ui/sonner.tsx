"use client";

import { Toaster as Sonner, type ToasterProps } from "sonner";

const Toaster = ({ ...props }: ToasterProps) => (
  <Sonner
    className="toaster group"
    style={
      {
        "--normal-bg": "var(--surface)",
        "--normal-text": "var(--foreground)",
        "--normal-border": "var(--border)",
      } as React.CSSProperties
    }
    {...props}
  />
);

export { Toaster };
