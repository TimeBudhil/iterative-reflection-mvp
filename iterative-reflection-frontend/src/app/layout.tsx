import type { Metadata } from "next";
import { GeistSans } from "geist/font/sans";
import { GeistMono } from "geist/font/mono";
import { Providers } from "./providers";
import { THEME_HEAD_SCRIPT } from "@/lib/theme/presets";
import "./globals.css";

export const metadata: Metadata = {
  title: "Iterative Reflection",
  description: "Explore careers and reflect on what you learn.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${GeistSans.variable} ${GeistMono.variable}`} suppressHydrationWarning>
      <head>
        {/* Sets data-theme from the stored hint before first paint (no flash). Inline: the CSP has no script-src. */}
        <script dangerouslySetInnerHTML={{ __html: THEME_HEAD_SCRIPT }} />
      </head>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
