import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

/**
 * Wrapper every settings card uses. `id` is the `#anchor` (e.g. "profile", "my-counselors", "interests").
 * To add a card: build a component that renders <SettingsCard id title>, then pass it as a child of
 * <SettingsPage> in that role's page (src/app/<role>/settings/page.tsx). Children render after Password.
 */
export function SettingsCard({
  id,
  title,
  description,
  children,
}: {
  id: string;
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <Card id={id} className="scroll-mt-6" aria-labelledby={`${id}-title`}>
      <CardHeader>
        <CardTitle id={`${id}-title`} className="text-lg">
          {title}
        </CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  );
}
