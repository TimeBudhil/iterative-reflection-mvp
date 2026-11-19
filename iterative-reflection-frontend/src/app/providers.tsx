"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionEvents } from "@/lib/session/events";

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: false } } }),
  );
  return (
    <QueryClientProvider client={client}>
      <SessionEvents />
      {children}
    </QueryClientProvider>
  );
}
