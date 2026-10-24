import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";
import { AlertTriangle, CheckCircle2, Copy, Inbox, UserCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  PageHeader,
  PriorityBadge,
  StatusBadge,
  ago,
  LiveIndicator,
} from "@/components/portal";
import { CATEGORIES } from "@/lib/data";
import { createAdminInvite, sortQueue } from "@/lib/api";
import { useAdminTickets, useSession } from "@/lib/store";

export const Route = createFileRoute("/admin/dashboard")({
  head: () => ({
    meta: [
      { title: "Operations Overview — BookLeaf Admin" },
      { name: "description", content: "Support queue health at a glance." },
      { property: "og:title", content: "Operations Overview — BookLeaf" },
      { property: "og:description", content: "Support queue health at a glance." },
    ],
  }),
  component: AdminDash,
});

function AdminDash() {
  const me = useSession()!;
  const query = useAdminTickets();
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteLink, setInviteLink] = useState("");
  const [inviteLoading, setInviteLoading] = useState(false);
  const tickets = query.data ?? [];

  const createInvite = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (inviteLoading) return;
    setInviteLoading(true);
    setInviteLink("");
    try {
      const invite = await createAdminInvite(inviteEmail.trim());
      const url = new URL("/signup", window.location.origin);
      url.hash = new URLSearchParams({ invite: invite.token }).toString();
      setInviteLink(url.toString());
      setInviteEmail("");
      toast.success("Admin invitation created", {
        description: "It expires in 48 hours. Share the link only with the invited person.",
      });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to create an admin invitation.");
    } finally {
      setInviteLoading(false);
    }
  };

  const copyInvite = async () => {
    try {
      await navigator.clipboard.writeText(inviteLink);
      toast.success("Invitation link copied");
    } catch {
      toast.error("Could not copy the link. Select and copy it manually.");
    }
  };

  if (query.isPending)
    return (
      <>
        <PageHeader title="Operations overview" />
        <p role="status">Loading support queue…</p>
      </>
    );
  if (query.isError)
    return (
      <>
        <PageHeader title="Operations overview" />
        <p role="alert" className="text-sm text-destructive">
          Unable to load the support queue. {query.error.message}
        </p>
      </>
    );
  const open = tickets.filter((t) => t.status === "Open" || t.status === "In Progress");
  const stats = [
    { label: "Unresolved", value: open.length, icon: Inbox },
    {
      label: "Critical / High",
      value: open.filter((t) => t.priority === "Critical" || t.priority === "High").length,
      icon: AlertTriangle,
    },
    {
      label: "Assigned to me",
      value: open.filter((t) => t.assigneeId === me.id).length,
      icon: UserCheck,
    },
    {
      label: "Resolved",
      value: tickets.filter((t) => t.status === "Resolved" || t.status === "Closed").length,
      icon: CheckCircle2,
    },
  ];
  return (
    <>
      <PageHeader
        title="Operations overview"
        subtitle="Live view of the author support queue."
        action={<LiveIndicator label="Queue live" />}
      />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.label}>
            <s.icon className="h-4 w-4 text-muted-foreground" />
            <p className="mt-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {s.label}
            </p>
            <p className="mt-1 font-display text-3xl font-bold">{s.value}</p>
          </Card>
        ))}
      </div>
      <Card className="mt-6">
        <h2 className="text-lg font-semibold">Invite an administrator</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Create a one-time link for a specific email. It expires after 48 hours; share it securely.
        </p>
        <form onSubmit={createInvite} className="mt-4 flex flex-wrap items-end gap-3">
          <div className="min-w-[240px] flex-1 space-y-1.5">
            <Label htmlFor="admin-invite-email">New administrator&apos;s email</Label>
            <Input
              id="admin-invite-email"
              type="email"
              autoComplete="email"
              maxLength={190}
              required
              value={inviteEmail}
              onChange={(event) => setInviteEmail(event.target.value)}
              placeholder="admin@example.com"
            />
          </div>
          <Button type="submit" disabled={inviteLoading}>
            {inviteLoading ? "Creating invite…" : "Create invite"}
          </Button>
        </form>
        {inviteLink && (
          <div className="mt-4 space-y-2">
            <Label htmlFor="admin-invite-link">One-time invitation link</Label>
            <div className="flex gap-2">
              <Input id="admin-invite-link" readOnly value={inviteLink} />
              <Button type="button" variant="outline" onClick={() => void copyInvite()}>
                <Copy className="h-4 w-4" /> Copy link
              </Button>
            </div>
          </div>
        )}
      </Card>
      <div className="mt-8 grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Needs attention</h2>
            <Link to="/admin/tickets" className="text-sm font-medium text-primary hover:underline">
              Open queue
            </Link>
          </div>
          <ul className="mt-4 divide-y">
            {[...open]
              .sort(sortQueue)
              .slice(0, 6)
              .map((t) => (
                <li key={t.id}>
                  <Link
                    to="/admin/tickets/$id"
                    params={{ id: t.id }}
                    className="flex items-center justify-between gap-3 py-3 hover:opacity-80"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{t.subject}</p>
                      <p className="text-xs text-muted-foreground">
                        #{t.id} · {t.authorName} · {ago(t.createdAt)}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      {t.priority && <PriorityBadge p={t.priority} />}
                      <StatusBadge s={t.status} />
                    </div>
                  </Link>
                </li>
              ))}
          </ul>
        </Card>
        <Card className="lg:col-span-2">
          <h2 className="text-lg font-semibold">Open by category</h2>
          <ul className="mt-4 space-y-3">
            {CATEGORIES.map((c) => {
              const n = open.filter((t) => t.category === c).length;
              return (
                <li key={c}>
                  <div className="flex justify-between text-sm">
                    <span>{c}</span>
                    <span className="font-semibold">{n}</span>
                  </div>
                  <div className="mt-1 h-1.5 rounded-full bg-accent">
                    <div
                      className="h-1.5 rounded-full bg-primary"
                      style={{ width: `${open.length ? (n / open.length) * 100 : 0}%` }}
                    />
                  </div>
                </li>
              );
            })}
          </ul>
        </Card>
      </div>
    </>
  );
}
