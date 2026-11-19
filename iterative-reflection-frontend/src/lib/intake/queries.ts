"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import type { ExistingIntake, IntakeQuestion, IntakeRequest } from "./model";

export function useIntakeQuestions() {
  return useQuery({
    queryKey: queryKeys.intakeQuestions,
    queryFn: async () => (await api.GET("/api/intake/questions")).data as IntakeQuestion[],
  });
}

/** The student's intake, or `null` when none exists yet (the 404 has no `detail`, so only the status is used). */
export function useIntake() {
  return useQuery({
    queryKey: queryKeys.intake,
    queryFn: async (): Promise<ExistingIntake | null> => {
      try {
        return (await api.GET("/api/intake")).data as ExistingIntake;
      } catch (e) {
        if (e instanceof ApiError && e.status === 404) return null;
        throw e;
      }
    },
  });
}

/** POST for a first submit, PUT (full replace) for an edit. */
export function useSaveIntake(exists: boolean) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: IntakeRequest) =>
      (exists ? await api.PUT("/api/intake", { body }) : await api.POST("/api/intake", { body })).data,
    onSuccess: async () => {
      await Promise.all([
        qc.invalidateQueries({ queryKey: queryKeys.intake }),
        qc.invalidateQueries({ queryKey: queryKeys.me }),
      ]);
    },
  });
}

/** `POST /api/intake/dismiss`; refreshes `/api/me` so `intakePromptDue` turns false. */
export function useDismissIntake() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await api.POST("/api/intake/dismiss", {});
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.me }),
  });
}
