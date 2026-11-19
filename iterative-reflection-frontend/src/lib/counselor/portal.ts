"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api/client";
import { queryKeys } from "@/lib/query/keys";
import { toStudentDetailView, type RawStudentDetail } from "./student-detail";

export const STUDENTS_PAGE_SIZE = 20;

export function useCounselorProfile() {
  return useQuery({
    queryKey: queryKeys.counselorMe,
    queryFn: async () => (await api.GET("/api/counselor/me")).data!,
  });
}

export function useRotateJoinCode() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => (await api.POST("/api/counselor/me/join-code/rotate")).data!,
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.counselorMe }),
  });
}

export function useStudentDetail(studentId: string) {
  return useQuery({
    queryKey: queryKeys.counselorStudent(studentId),
    queryFn: async () =>
      toStudentDetailView(
        (await api.GET("/api/counselor/students/{studentId}", { params: { path: { studentId: Number(studentId) } } })).data as RawStudentDetail,
      ),
  });
}

export function useCounselorStudents(page: number) {
  return useQuery({
    queryKey: queryKeys.counselorStudents(page),
    queryFn: async () =>
      (
        await api.GET("/api/counselor/students", {
          params: { query: { pageable: { page, size: STUDENTS_PAGE_SIZE } } },
          // The generated schema models Spring's Pageable as one object param; the wire format is flat page/size.
          querySerializer: () => `page=${page}&size=${STUDENTS_PAGE_SIZE}`,
        })
      ).data!,
    placeholderData: keepPreviousData,
  });
}
