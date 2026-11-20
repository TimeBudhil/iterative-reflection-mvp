"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { tabFromParam, tabToSearch, type ActivityTab } from "@/lib/admin/activity-model";
import { ActivityView } from "./activity-view";
import { CounselorViews } from "./counselor-views";

export function AuditTabs() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const tab = tabFromParam(params.get("tab"));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Audit log</h1>
      <Tabs
        value={tab}
        onValueChange={(next) => router.replace(`${pathname}${tabToSearch(next as ActivityTab, params)}`)}
      >
        <TabsList>
          <TabsTrigger value="views">Counselor views</TabsTrigger>
          <TabsTrigger value="activity">Admin activity</TabsTrigger>
        </TabsList>
        <TabsContent value="views" className="pt-4">
          <CounselorViews />
        </TabsContent>
        <TabsContent value="activity" className="pt-4">
          <ActivityView />
        </TabsContent>
      </Tabs>
    </div>
  );
}
