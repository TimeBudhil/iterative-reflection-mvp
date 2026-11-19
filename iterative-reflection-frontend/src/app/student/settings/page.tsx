import { InterestsCard } from "@/components/settings/interests-card";
import { MyCounselorsCard } from "@/components/settings/my-counselors-card";
import { SettingsPage } from "@/components/settings/settings-page";

export default function Page() {
  return (
    <SettingsPage>
      <MyCounselorsCard />
      <InterestsCard />
    </SettingsPage>
  );
}
