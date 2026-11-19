"use client";

import { use } from "react";
import { ExperienceViewer } from "@/components/experience/viewer";

export default function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <ExperienceViewer careerId={Number(id)} />;
}
