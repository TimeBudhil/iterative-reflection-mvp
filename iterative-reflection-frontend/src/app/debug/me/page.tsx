"use client";

// TEMPORARY: shows the raw /api/me result. Removed once the session module (ticket 03) lands.
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";

export default function DebugMePage() {
  const qc = useQueryClient();
  const me = useQuery({
    queryKey: ["debug", "me"],
    queryFn: async () => (await api.GET("/api/me")).data,
  });

  return (
    <main className="p-8">
      <h1 className="text-2xl font-semibold">Debug: /api/me</h1>
      <button
        type="button"
        className="mt-4 rounded border px-3 py-1"
        onClick={async () => {
          await api.POST("/api/auth/signin", { body: { email: "admin@demo.example", password: "demo" } });
          await qc.invalidateQueries({ queryKey: ["debug", "me"] });
        }}
      >
        Sign in as demo admin
      </button>
      <pre data-testid="me-result" className="mt-4 text-sm">
        {me.isPending
          ? "loading"
          : me.error
            ? JSON.stringify({ error: me.error, status: (me.error as { status?: number }).status })
            : JSON.stringify(me.data, null, 2)}
      </pre>
    </main>
  );
}
