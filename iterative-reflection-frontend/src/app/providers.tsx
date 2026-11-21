"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/sonner";
import { SessionEvents } from "@/lib/session/events";
import { ThemeApplier } from "@/lib/theme/theme";

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: false } } }),
  );
  return (
    <QueryClientProvider client={client}>
      <ThemeApplier />
      <SessionEvents />
      {children}
      <Toaster />
    </QueryClientProvider>
  );
}
