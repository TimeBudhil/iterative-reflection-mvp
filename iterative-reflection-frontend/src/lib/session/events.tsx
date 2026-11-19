"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { isSigningOut, setAccessDenied } from "./access";
import { identityChanged, signInHref } from "./model";
import type { Me } from "./session";

const currentPath = () => window.location.pathname + window.location.search;

/**
 * Plugs the app-level reactions into the API client's hooks (once, at startup):
 * - 401 anywhere except /signin -> /signin?next=<path> with the expiry notice (R2.3)
 * - unexpected 403 -> refetch /api/me; a changed role/mustChangePassword re-runs the guards, otherwise the
 *   no-access panel shows (R2.7)
 */
export function SessionEvents() {
  const router = useRouter();
  const qc = useQueryClient();

  useEffect(() => {
    api.setHooks({
      onUnauthorized: () => {
        if (isSigningOut() || window.location.pathname.startsWith("/signin")) return;
        router.replace(signInHref(currentPath()));
        qc.clear();
      },
      onForbidden: () => {
        const path = window.location.pathname;
        const before = qc.getQueryData<Me>(queryKeys.me);
        qc
          .fetchQuery({
            queryKey: queryKeys.me,
            staleTime: 0,
            queryFn: async () => (await api.GET("/api/me")).data as Me,
          })
          .then((after) => {
            if (!identityChanged(before, after)) setAccessDenied(path);
          })
          .catch(() => {
            // A 401 here is handled by onUnauthorized; any other failure leaves the page as is.
          });
      },
    });
  }, [router, qc]);

  return null;
}
