import Link from "next/link";
import { TERMS_VERSION } from "@/lib/signup/model";

export const metadata = { title: "Terms" };

export default function TermsPage() {
  return (
    <main className="mx-auto max-w-2xl space-y-4 p-6">
      <p className="inline-block rounded-md border border-destructive px-2 py-1 text-sm font-medium text-destructive">
        Draft — not legal text
      </p>
      <h1 className="text-2xl font-semibold">Terms of use</h1>
      <p className="text-sm text-muted-foreground">
        Version: <span data-testid="terms-version">{TERMS_VERSION}</span>
      </p>
      <p>
        This is placeholder text. Real terms of use and a privacy notice will replace it before any school uses this
        app.
      </p>
      <p>
        By creating an account you confirm you are 13 or older and agree to use Iterative Reflection to explore careers
        and reflect on them honestly.
      </p>
      <Link href="/signin?mode=signup" className="underline">
        Back to sign up
      </Link>
    </main>
  );
}
