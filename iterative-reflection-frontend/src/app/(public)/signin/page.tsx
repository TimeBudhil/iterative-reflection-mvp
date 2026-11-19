import { Suspense } from "react";
import { SignInView } from "./sign-in-view";

export default function SignInPage() {
  return (
    <Suspense>
      <SignInView />
    </Suspense>
  );
}
