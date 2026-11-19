"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { NoAccessPanel } from "@/components/feedback/no-access-panel";
import { useAccessDenied } from "@/lib/session/access";
import { navFor, roleHome, settingsHref, type Role } from "@/lib/session/model";
import { RoleGuard, useSignOut, type Me } from "@/lib/session/session";

function PortalSkeleton() {
  return (
    <div className="flex min-h-screen" aria-busy="true" aria-label="Loading">
      <div className="w-60 shrink-0 space-y-3 border-r p-4">
        <Skeleton className="h-6 w-32" />
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-8 w-full" />
      </div>
      <div className="flex-1 space-y-4 p-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full max-w-2xl" />
      </div>
    </div>
  );
}

function UserMenu({ me }: { me: Me }) {
  const signOut = useSignOut();
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex w-full items-center justify-between rounded-md px-3 py-2 text-sm hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
        <span className="truncate" data-testid="user-menu-name">
          {me.displayName}
        </span>
        <ChevronsUpDown className="size-4 shrink-0 text-muted-foreground" aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" side="top" className="w-56">
        <DropdownMenuItem asChild>
          <Link href={settingsHref(me.role)}>Settings</Link>
        </DropdownMenuItem>
        <DropdownMenuItem disabled={signOut.isPending} onSelect={() => signOut.mutate()}>
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function Shell({ me, children }: { me: Me; children: React.ReactNode }) {
  const pathname = usePathname();
  const denied = useAccessDenied(pathname);
  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 shrink-0 flex-col border-r p-3">
        <div className="px-3 py-2 text-base font-semibold">Iterative Reflection</div>
        <nav aria-label="Main" className="mt-4 flex flex-1 flex-col gap-1">
          {navFor(me.role).map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "rounded-md px-3 py-2 text-sm hover:bg-accent",
                  active && "bg-accent font-medium text-accent-foreground",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
        <UserMenu me={me} />
      </aside>
      <main className="min-w-0 flex-1 p-8">
        {denied ? <NoAccessPanel homeHref={roleHome(me.role)} /> : children}
      </main>
    </div>
  );
}

/** Role layout: guard (skeleton until the role is confirmed) + sidebar + user menu. */
export function PortalShell({ role, children }: { role: Role; children: React.ReactNode }) {
  return (
    <RoleGuard role={role} fallback={<PortalSkeleton />}>
      {(me) => <Shell me={me}>{children}</Shell>}
    </RoleGuard>
  );
}
