"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { applyTheme, resolvePresetId, type PresetId } from "./presets";

/** `GET /api/admin/settings/theme`: the saved preset plus who changed it and when (nulls before any save). */
export function useAdminTheme() {
  return useQuery({
    queryKey: queryKeys.adminTheme,
    queryFn: async () => (await api.GET("/api/admin/settings/theme")).data!,
    refetchOnWindowFocus: false,
  });
}

/**
 * `PUT /api/admin/settings/theme`. On success the page re-themes at once (`applyTheme`, which also keeps the
 * no-flash hint) and both theme queries are updated, so nothing waits for a reload.
 */
export function useSaveTheme() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (presetId: PresetId) =>
      (await api.PUT("/api/admin/settings/theme", { body: { presetId } })).data!,
    onSuccess: (saved) => {
      const id = applyTheme(saved.presetId);
      qc.setQueryData(queryKeys.theme, id);
      qc.setQueryData(queryKeys.adminTheme, { ...saved, presetId: resolvePresetId(saved.presetId) });
    },
  });
}
