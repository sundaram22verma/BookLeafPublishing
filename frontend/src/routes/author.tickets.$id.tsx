import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Send } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Card, EmptyState, LiveIndicator, StatusBadge, fmtDate } from "@/components/portal";
import { Thread } from "@/components/thread";
import { replyToAuthorTicket } from "@/lib/api";
import { queryKeys, useAuthorTicket, useSession } from "@/lib/store";
import { Inbox } from "lucide-react";

export const Route = createFileRoute("/author/tickets/$id")({
  head: ({ params }) => ({
    meta: [
      { title: `Ticket #${params.id} — BookLeaf` },
      { name: "description", content: "Conversation with the BookLeaf support team." },
      { property: "og:title", content: `Ticket #${params.id} — BookLeaf` },
      { property: "og:description", content: "Conversation with the BookLeaf support team." },
    ],
  }),
  component: TicketView,
});

function TicketView() {
  const { id } = Route.useParams();
  const user = useSession()!;
  const queryClient = useQueryClient();
  const query = useAuthorTicket(id, user);
  const t = query.data;
  const [reply, setReply] = useState("");
  const [sending, setSending] = useState(false);
  const count = useRef(t?.messages.length ?? 0);

  // Notify when a new admin reply arrives live (cross-tab sync).
  useEffect(() => {
    if (!t) return;
    if (t.messages.length > count.current && t.messages[t.messages.length - 1]?.from === "admin")
      toast("New reply from BookLeaf", { description: t.subject });
    count.current = t.messages.length;
  }, [t]);

  if (query.isPending) return <p role="status">Loading ticket…</p>;
  if (query.isError)
    return (
      <p role="alert" className="text-sm text-destructive">
        Unable to load ticket. {query.error.message}
      </p>
    );
  if (!t)
    return (
      <EmptyState
        icon={Inbox}
        title="Ticket not found"
        body="This ticket doesn't exist or belongs to another account."
        action={
          <Button asChild variant="outline">
            <Link to="/author/tickets">Back to tickets</Link>
          </Button>
        }
      />
    );
  const closed = t.status === "Closed";
  const send = async () => {
    const message = reply.trim();
    if (message.length < 2 || sending) return;
    setSending(true);
    try {
      const updated = await replyToAuthorTicket(t.id, message.slice(0, 5000), user);
      queryClient.setQueryData(queryKeys.authorTicket(user.id, t.id), updated);
      setReply("");
      await queryClient.invalidateQueries({ queryKey: queryKeys.authorTickets(user.id) });
    } catch (cause) {
      toast.error(cause instanceof Error ? cause.message : "Unable to send your reply.");
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        to="/author/tickets"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> All tickets
      </Link>
      <Card>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="font-mono text-xs text-muted-foreground">
              #{t.id} · Opened {fmtDate(t.createdAt)}
            </p>
            <h1 className="mt-1 text-xl font-semibold">{t.subject}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {t.bookTitle ?? "General / Account Level"}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <LiveIndicator />
            <StatusBadge s={t.status} />
          </div>
        </div>
        <div className="mt-6 border-t pt-6">
          <Thread messages={t.messages} viewer="author" />
        </div>
        {t.messages.every((m) => m.from === "author") && (
          <p className="mt-4 text-center text-xs text-muted-foreground">
            Our team has received your query and will reply here — no need to refresh.
          </p>
        )}
        <div className="mt-6 border-t pt-4">
          {closed ? (
            <p className="text-center text-sm text-muted-foreground">
              This ticket is closed. Raise a new query if you need more help.
            </p>
          ) : (
            <div className="flex gap-2">
              <Textarea
                rows={2}
                value={reply}
                onChange={(e) => setReply(e.target.value)}
                placeholder="Write a follow-up…"
                onKeyDown={(e) => {
                  if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) send();
                }}
              />
              <Button
                onClick={send}
                className="self-end"
                aria-label="Send"
                disabled={sending || reply.trim().length < 2}
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
