import { createFileRoute, Link } from "@tanstack/react-router";
import { Inbox, MessageSquare, PlusCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, EmptyState, LiveIndicator, PageHeader, StatusBadge, ago } from "@/components/portal";
import { useAuthorTickets, useSession } from "@/lib/store";

export const Route = createFileRoute("/author/tickets/")({
  head: () => ({
    meta: [
      { title: "My Tickets — BookLeaf Author Portal" },
      {
        name: "description",
        content: "All your support queries and replies from the BookLeaf team.",
      },
      { property: "og:title", content: "My Tickets — BookLeaf" },
      {
        property: "og:description",
        content: "All your support queries and replies from the BookLeaf team.",
      },
    ],
  }),
  component: MyTickets,
});

function MyTickets() {
  const user = useSession()!;
  const query = useAuthorTickets(user);
  const tickets = [...(query.data ?? [])].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  if (query.isPending)
    return (
      <>
        <PageHeader title="My Tickets" />
        <p role="status">Loading your tickets…</p>
      </>
    );
  if (query.isError)
    return (
      <>
        <PageHeader title="My Tickets" />
        <p role="alert" className="text-sm text-destructive">
          Unable to load tickets. {query.error.message}
        </p>
      </>
    );
  return (
    <>
      <PageHeader
        title="My Tickets"
        subtitle="Replies from our team appear here instantly."
        action={
          <div className="flex items-center gap-3">
            <LiveIndicator />
            <Button asChild>
              <Link to="/author/tickets/new">
                <PlusCircle className="h-4 w-4" /> New query
              </Link>
            </Button>
          </div>
        }
      />
      {tickets.length === 0 ? (
        <EmptyState
          icon={Inbox}
          title="No tickets yet"
          body="Have a question about royalties, ISBNs or your book's status? Raise a query and we'll respond quickly."
          action={
            <Button asChild>
              <Link to="/author/tickets/new">Raise a query</Link>
            </Button>
          }
        />
      ) : (
        <div className="space-y-3">
          {tickets.map((t) => {
            const last = t.messages[t.messages.length - 1]!;
            const replies = t.messages.filter((m) => m.from === "admin").length;
            return (
              <Link key={t.id} to="/author/tickets/$id" params={{ id: t.id }} className="block">
                <Card className="transition-shadow hover:shadow-md">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="font-mono text-xs text-muted-foreground">
                        #{t.id} · {t.bookTitle ?? "General / Account"}
                      </p>
                      <p className="mt-1 font-display font-semibold">{t.subject}</p>
                    </div>
                    <StatusBadge s={t.status} />
                  </div>
                  <p className="mt-2 line-clamp-1 text-sm text-muted-foreground">
                    {last.from === "admin" ? "BookLeaf: " : "You: "}
                    {last.body}
                  </p>
                  <p className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <MessageSquare className="h-3.5 w-3.5" /> {replies}{" "}
                      {replies === 1 ? "reply" : "replies"}
                    </span>{" "}
                    Updated {ago(t.updatedAt)}
                  </p>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </>
  );
}
