import { Link, Outlet, useNavigate, useRouterState } from "@tanstack/react-router";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect, type ReactNode } from "react";
import {
  BookOpen,
  Inbox,
  LayoutDashboard,
  LogOut,
  PlusCircle,
  Ticket as TicketIcon,
  Radio,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { logout, updateSessionCache, useSession, useSessionError } from "@/lib/store";
import { useTicketStream } from "@/lib/use-ticket-stream";
import type { Priority, Role, TicketStatus, BookStatus } from "@/lib/data";

export function Logo({ className }: { className?: string }) {
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <div className="grid h-8 w-8 place-items-center rounded-md bg-primary text-primary-foreground">
        <BookOpen className="h-4 w-4" />
      </div>
      <span className="font-display text-lg font-bold tracking-tight">BookLeaf</span>
    </div>
  );
}

const NAV: Record<Role, { to: string; label: string; icon: typeof Inbox }[]> = {
  author: [
    { to: "/author/dashboard", label: "Dashboard", icon: LayoutDashboard },
    { to: "/author/books", label: "My Books", icon: BookOpen },
    { to: "/author/tickets", label: "My Tickets", icon: TicketIcon },
    { to: "/author/tickets/new", label: "New Query", icon: PlusCircle },
  ],
  admin: [
    { to: "/admin/dashboard", label: "Overview", icon: LayoutDashboard },
    { to: "/admin/tickets", label: "Ticket Queue", icon: Inbox },
  ],
};

export function PortalLayout({ role }: { role: Role }) {
  const user = useSession();
  const sessionError = useSessionError();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const path = useRouterState({ select: (s) => s.location.pathname });
  useTicketStream(role === "author" && user?.role === "author" ? user : null);
  useEffect(() => {
    if (user === null || (user && user.role !== role)) navigate({ to: "/login", replace: true });
  }, [user, role, navigate]);
  if (!user || user.role !== role) {
    return (
      <div className="grid min-h-screen place-items-center text-sm text-muted-foreground">
        {sessionError ? (
          <p role="alert">Unable to verify your session. {sessionError.message}</p>
        ) : (
          "Loading…"
        )}
      </div>
    );
  }
  return (
    <div className="flex min-h-screen w-full">
      <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r bg-sidebar p-4 md:flex">
        <Logo />
        <p className="mt-1 pl-10 text-[11px] font-medium uppercase tracking-widest text-muted-foreground">
          {role === "admin" ? "Operations" : "Author Portal"}
        </p>
        <nav className="mt-8 flex flex-col gap-1">
          {NAV[role].map((n) => {
            const active =
              path === n.to ||
              (n.to.endsWith("/tickets") && path.startsWith(n.to + "/") && !path.endsWith("/new"));
            return (
              <Link
                key={n.to}
                to={n.to}
                className={cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                  active
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-sidebar-accent hover:text-foreground",
                )}
              >
                <n.icon className="h-4 w-4" /> {n.label}
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto border-t pt-4">
          <p className="text-sm font-semibold">{user.name}</p>
          <p className="truncate text-xs text-muted-foreground">{user.email}</p>
          <button
            onClick={() => {
              logout();
              updateSessionCache(queryClient, null);
              void navigate({ to: "/login" });
            }}
            className="mt-3 flex items-center gap-2 text-xs font-medium text-muted-foreground hover:text-foreground"
          >
            <LogOut className="h-3.5 w-3.5" /> Sign out
          </button>
        </div>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b bg-card px-4 py-3 md:hidden">
          <Logo />
          <button
            onClick={() => {
              logout();
              updateSessionCache(queryClient, null);
              void navigate({ to: "/login" });
            }}
            aria-label="Sign out"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </header>
        <nav className="flex gap-1 overflow-x-auto border-b bg-card px-2 py-2 md:hidden">
          {NAV[role].map((n) => (
            <Link
              key={n.to}
              to={n.to}
              className={cn(
                "whitespace-nowrap rounded-md px-3 py-1.5 text-xs font-medium",
                path === n.to ? "bg-primary text-primary-foreground" : "text-muted-foreground",
              )}
            >
              {n.label}
            </Link>
          ))}
        </nav>
        <main className="mx-auto w-full max-w-7xl flex-1 p-4 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export function PageHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold md:text-[28px]">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Pill({ className, children }: { className?: string; children: ReactNode }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 whitespace-nowrap rounded px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide",
        className,
      )}
    >
      {children}
    </span>
  );
}

const STATUS_CLS: Record<TicketStatus, string> = {
  Open: "bg-amber-soft text-amber-soft-foreground",
  "In Progress": "bg-ink-soft text-ink-soft-foreground",
  Resolved: "bg-brand-soft text-brand-soft-foreground",
  Closed: "bg-accent text-muted-foreground",
};
export const StatusBadge = ({ s }: { s: TicketStatus }) => (
  <Pill className={STATUS_CLS[s]}>{s}</Pill>
);

const PRI_CLS: Record<Priority, string> = {
  Critical: "bg-destructive text-destructive-foreground",
  High: "bg-danger-soft text-danger-soft-foreground",
  Medium: "bg-amber-soft text-amber-soft-foreground",
  Low: "bg-accent text-muted-foreground",
};
export const PriorityBadge = ({ p }: { p: Priority }) => (
  <Pill className={PRI_CLS[p]}>
    {p === "Critical" && "● "}
    {p}
  </Pill>
);

export const BookStatusBadge = ({ s }: { s: BookStatus }) =>
  s ? (
    <Pill
      className={
        s === "Published & Live"
          ? "bg-brand-soft text-brand-soft-foreground"
          : "bg-amber-soft text-amber-soft-foreground"
      }
    >
      {s}
    </Pill>
  ) : (
    <Pill className="bg-accent text-muted-foreground">Status unavailable</Pill>
  );

export function LiveIndicator({ label = "Live" }: { label?: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border bg-card px-2.5 py-1 text-[11px] font-medium text-muted-foreground">
      <span className="relative flex h-2 w-2">
        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-60" />
        <span className="relative inline-flex h-2 w-2 rounded-full bg-primary" />
      </span>
      <Radio className="h-3 w-3" /> {label}
    </span>
  );
}

export function EmptyState({
  icon: Icon,
  title,
  body,
  action,
}: {
  icon: typeof Inbox;
  title: string;
  body: string;
  action?: ReactNode;
}) {
  return (
    <div className="rounded-lg border border-dashed bg-card p-10 text-center">
      <Icon className="mx-auto h-8 w-8 text-muted-foreground" />
      <p className="mt-3 font-display font-semibold">{title}</p>
      <p className="mx-auto mt-1 max-w-sm text-sm text-muted-foreground">{body}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export const inr = (n: number) => "₹" + n.toLocaleString("en-IN");
export const fmtDate = (s: string | null) =>
  s
    ? new Date(s).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })
    : "—";
export function ago(s: string) {
  const m = Math.round((Date.now() - new Date(s).getTime()) / 60000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.round(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.round(h / 24)}d ago`;
}
export const Card = ({ className, children }: { className?: string; children: ReactNode }) => (
  <div className={cn("rounded-lg border bg-card p-5", className)}>{children}</div>
);
