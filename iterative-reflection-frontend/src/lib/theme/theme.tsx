"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { applyTheme, DEFAULT_PRESET_ID, resolvePresetId, type PresetId } from "./presets";

/** `GET /api/settings/theme` (public). Any failure or unknown id resolves to Grove: a theme problem is never shown. */
async function fetchThemePresetId(): Promise<PresetId> {
  try {
    const { data } = await api.GET("/api/settings/theme");
    return resolvePresetId((data as { presetId?: unknown } | undefined)?.presetId);
  } catch {
    return DEFAULT_PRESET_ID;
  }
}

/** Fetched once per full page load: no refetch on focus or reconnect, no polling, never retried. `undefined` until it resolves. */
export function useThemePresetId(): PresetId | undefined {
  const { data } = useQuery({
    queryKey: queryKeys.theme,
    queryFn: fetchThemePresetId,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchOnMount: false,
  });
  return data;
}

/** Renders nothing; keeps `<html data-theme>` in step with the app theme on every page, signed in or out. */
export function ThemeApplier() {
  const presetId = useThemePresetId();
  useEffect(() => {
    // Until the server answers, keep whatever the head script set from the hint.
    if (presetId) applyTheme(presetId);
  }, [presetId]);
  return null;
}
