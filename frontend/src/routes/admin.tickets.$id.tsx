import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  BrainCircuit,
  Inbox,
  Lock,
  MessageSquare,
  RotateCcw,
  Send,
  Wand2,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Card,
  EmptyState,
  LiveIndicator,
  Pill,
  PriorityBadge,
  StatusBadge,
  ago,
  fmtDate,
  inr,
} from "@/components/portal";
import { Thread } from "@/components/thread";
import {
  CATEGORIES,
  PRIORITIES,
  STATUSES,
  type Category,
  type Priority,
  type Ticket,
  type TicketStatus,
} from "@/lib/data";
import {
  assignTicketToMe,
  createInternalNote,
  createTicketReply,
  getAiDraft,
  updateTicketAssignee,
  updateTicketCategory,
  updateTicketPriority,
  updateTicketStatus,
} from "@/lib/api";
import { queryKeys, useAdminTicket, useAdminUsers, useSession } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/admin/tickets/$id")({
  head: ({ params }) => ({
    meta: [
      { title: `Ticket #${params.id} — BookLeaf Admin` },
      { name: "description", content: "Triage, respond to and manage an author ticket." },
      { property: "og:title", content: `Ticket #${params.id} — BookLeaf Admin` },
      { property: "og:description", content: "Triage, respond to and manage an author ticket." },
    ],
  }),
  component: AdminTicket,
});

const sel = "h-9 w-full rounded-md border border-input bg-background px-2 text-sm";

function AdminTicket() {
  const { id } = Route.useParams();
  const me = useSession()!;
  const query = useAdminTicket(id);
  if (query.isPending) return <p role="status">Loading ticket…</p>;
  if (query.isError)
    return (
      <p role="alert" className="text-sm text-destructive">
        Unable to load ticket. {query.error.message}
      </p>
    );
  if (!query.data)
    return (
      <EmptyState
        icon={Inbox}
        title="Ticket not found"
        body="It may have been removed."
        action={
          <Button asChild variant="outline">
            <Link to="/admin/tickets">Back to queue</Link>
          </Button>
        }
      />
    );
  return <Detail key={query.data.id} ticket={query.data} meName={me.name} meId={me.id} />;
}

function Detail({ ticket, meName, meId }: { ticket: Ticket; meName: string; meId: string }) {
  const queryClient = useQueryClient();
  const usersQuery = useAdminUsers();
  const [draft, setDraft] = useState("");
  const [draftIsAi, setDraftIsAi] = useState(false);
  const [draftLoading, setDraftLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [note, setNote] = useState("");
  const [tab, setTab] = useState<"reply" | "notes">("reply");
  const words = draft.trim().split(/\s+/).filter(Boolean).length;
  const category = ticket.category;
  const priority = ticket.priority;
  const authorName = ticket.authorName ?? "Author";

  useEffect(() => {
    let cancelled = false;
    getAiDraft(ticket.id)
      .then((result) => {
        if (cancelled) return;
        setDraft(result.draft ?? "");
        setDraftIsAi(result.available && Boolean(result.draft));
        if (!result.available && result.message) toast(result.message);
      })
      .catch((error: unknown) => {
        if (!cancelled)
          toast.error(error instanceof Error ? error.message : "Unable to load an AI draft.");
      })
      .finally(() => {
        if (!cancelled) setDraftLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [ticket.id]);

  const refreshTicket = async (updated: Ticket) => {
    queryClient.setQueryData(queryKeys.adminTicket(ticket.id), updated);
    await queryClient.invalidateQueries({ queryKey: queryKeys.adminTickets });
  };

  const performUpdate = async (
    operation: () => Promise<Ticket>,
    success?: string,
  ): Promise<boolean> => {
    if (saving) return false;
    setSaving(true);
    try {
      await refreshTicket(await operation());
      if (success) toast.success(success);
      return true;
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to update this ticket.");
      return false;
    } finally {
      setSaving(false);
    }
  };

  const send = () => {
    const message = draft.trim();
    if (message.length < 10) {
      toast.error("Response is too short to send.");
      return;
    }
    void performUpdate(
      () => createTicketReply(ticket.id, message, draftIsAi),
      `Response sent to ${authorName.split(" ")[0]}`,
    ).then((success) => {
      if (success) setDraft("");
    });
  };

  const generateDraft = async (regenerate: boolean) => {
    setDraftLoading(true);
    try {
      const result = await getAiDraft(ticket.id, regenerate);
      if (result.available && result.draft) {
        setDraft(result.draft);
        setDraftIsAi(true);
      } else {
        setDraft("");
        setDraftIsAi(false);
        if (result.message) toast(result.message);
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Unable to generate an AI draft.");
      setDraftIsAi(false);
    } finally {
      setDraftLoading(false);
    }
  };

  const postNote = () => {
    const text = note.trim();
    if (!text) return;
    void performUpdate(
      () => createInternalNote(ticket.id, text.slice(0, 3000)),
      "Internal note added",
    ).then((success) => {
      if (success) setNote("");
    });
  };

  return (
    <>
      <Link
        to="/admin/tickets"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Ticket queue
      </Link>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Pill className="bg-primary font-mono text-primary-foreground">#{ticket.id}</Pill>
            {priority && <PriorityBadge p={priority} />}
            <StatusBadge s={ticket.status} />
          </div>
          <h1 className="mt-2 text-2xl font-semibold">{ticket.subject}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Submitted {fmtDate(ticket.createdAt)} · {ago(ticket.createdAt)} · Author Portal
          </p>
        </div>
        <LiveIndicator />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card>
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <MessageSquare className="h-4 w-4" /> Conversation{" "}
              <span className="text-xs font-normal text-muted-foreground">
                {ticket.messages.length} messages
              </span>
            </h2>
            <div className="mt-4">
              <Thread messages={ticket.messages} viewer="admin" />
            </div>
          </Card>

          <Card className="p-0">
            <div className="flex border-b">
              {(["reply", "notes"] as const).map((key) => (
                <button
                  key={key}
                  onClick={() => setTab(key)}
                  className={cn(
                    "flex items-center gap-2 px-5 py-3 text-sm font-medium",
                    tab === key
                      ? "border-b-2 border-primary text-foreground"
                      : "text-muted-foreground",
                  )}
                >
                  {key === "reply" ? (
                    <>
                      <Wand2 className="h-4 w-4" /> AI-drafted response
                    </>
                  ) : (
                    <>
                      <Lock className="h-4 w-4" /> Internal notes ({ticket.notes.length})
                    </>
                  )}
                </button>
              ))}
            </div>
            {tab === "reply" ? (
              <div className="p-5">
                <div className="mb-3 rounded-md border bg-muted px-3 py-2 text-xs text-muted-foreground">
                  {draftIsAi
                    ? "AI draft from the BookLeaf Knowledge Base. Review it before sending; the author receives it verbatim."
                    : "AI drafting is unavailable or no draft has been generated. Write a response manually."}
                </div>
                <Textarea
                  rows={12}
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="Write your response…"
                  className="font-sans leading-relaxed"
                />
                <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs text-muted-foreground">
                  <span className="font-mono">{words} words</span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => void generateDraft(true)}
                    disabled={draftLoading}
                  >
                    <RotateCcw className="h-3.5 w-3.5" />{" "}
                    {draftLoading ? "Generating…" : "Regenerate"}
                  </Button>
                </div>
                <div className="mt-4 flex flex-wrap justify-end gap-2">
                  <Button
                    variant="outline"
                    onClick={() => {
                      setDraft("");
                      setDraftIsAi(false);
                    }}
                  >
                    Discard
                  </Button>
                  <Button
                    onClick={send}
                    disabled={saving || ticket.status === "Closed" || draftLoading}
                  >
                    <Send className="h-4 w-4" /> Send to author
                  </Button>
                </div>
              </div>
            ) : (
              <div className="bg-amber-soft/30 p-5">
                <p className="mb-4 text-xs font-semibold uppercase tracking-wider text-amber-soft-foreground">
                  Staff only · never visible to authors
                </p>
                {ticket.notes.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No internal notes yet.</p>
                ) : (
                  <ul className="space-y-3">
                    {ticket.notes.map((item) => (
                      <li key={item.id} className="rounded-md border bg-card p-3 text-sm">
                        <p className="mb-1 text-xs font-semibold">
                          {item.adminName}{" "}
                          <span className="font-normal text-muted-foreground">
                            · {ago(item.at)}
                          </span>
                        </p>
                        <p className="whitespace-pre-wrap">{item.body}</p>
                      </li>
                    ))}
                  </ul>
                )}
                <Textarea
                  rows={3}
                  className="mt-4 bg-card"
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                  placeholder="Add a confidential operational update…"
                />
                <div className="mt-2 flex justify-end">
                  <Button size="sm" onClick={postNote} disabled={saving || !note.trim()}>
                    <Lock className="h-3.5 w-3.5" /> Post internal note
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Management
            </h3>
            <label className="mt-4 block text-xs font-medium">Status</label>
            <select
              className={sel}
              value={ticket.status}
              disabled={saving}
              onChange={(event) =>
                void performUpdate(() =>
                  updateTicketStatus(ticket.id, event.target.value as TicketStatus),
                )
              }
            >
              {STATUSES.map((status) => (
                <option key={status}>{status}</option>
              ))}
            </select>
            <label className="mt-3 block text-xs font-medium">Assignee</label>
            <select
              className={sel}
              value={ticket.assigneeId ?? ""}
              disabled={saving || usersQuery.isPending}
              onChange={(event) =>
                void performUpdate(() =>
                  updateTicketAssignee(ticket.id, event.target.value || null),
                )
              }
            >
              <option value="">Unassigned</option>
              {(usersQuery.data ?? []).map((admin) => (
                <option key={admin.id} value={admin.id}>
                  {admin.name}
                  {admin.id === meId ? " (You)" : ""}
                </option>
              ))}
            </select>
            {usersQuery.isError && (
              <p role="alert" className="mt-2 text-xs text-destructive">
                Unable to load admin users. {usersQuery.error.message}
              </p>
            )}
            {ticket.assigneeId !== meId && (
              <Button
                variant="outline"
                size="sm"
                className="mt-3 w-full"
                disabled={saving}
                onClick={() => void performUpdate(() => assignTicketToMe(ticket.id))}
              >
                Assign to me
              </Button>
            )}
          </Card>

          <Card>
            <div className="flex items-center justify-between">
              <h3 className="flex items-center gap-2 font-display font-semibold">
                <BrainCircuit className="h-4 w-4" /> Triage engine
              </h3>
              <span className="font-mono text-xs text-amber">
                {ticket.aiClassified ? "AI classified" : "Rule-based fallback"}
              </span>
            </div>
            <label className="mt-4 block text-xs font-medium">
              Category{" "}
              {ticket.categoryOverridden && (
                <span className="text-muted-foreground">(manually overridden)</span>
              )}
            </label>
            <select
              className={sel}
              value={category ?? "General Inquiry"}
              disabled={saving}
              onChange={(event) => {
                const next = event.target.value as Category;
                void performUpdate(() => updateTicketCategory(ticket.id, next)).then((success) => {
                  if (success) void generateDraft(true);
                });
              }}
            >
              {CATEGORIES.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
            <p className="mt-4 text-xs font-medium">
              Priority{" "}
              {ticket.priorityOverridden && (
                <span className="text-muted-foreground">(manually overridden)</span>
              )}
            </p>
            <div className="mt-1 grid grid-cols-4 gap-1">
              {PRIORITIES.map((value: Priority) => (
                <button
                  key={value}
                  disabled={saving}
                  onClick={() => void performUpdate(() => updateTicketPriority(ticket.id, value))}
                  className={cn(
                    "rounded px-1 py-1.5 text-[11px] font-semibold",
                    priority === value
                      ? value === "Critical"
                        ? "bg-destructive text-destructive-foreground"
                        : "bg-primary text-primary-foreground"
                      : "bg-muted text-muted-foreground hover:bg-accent",
                  )}
                >
                  {value}
                </button>
              ))}
            </div>
          </Card>

          <Card>
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Author
            </h3>
            <div className="mt-3 flex items-center gap-3">
              <div className="grid h-10 w-10 place-items-center rounded-md bg-primary font-semibold text-primary-foreground">
                {authorName
                  .split(" ")
                  .map((part) => part[0])
                  .join("")}
              </div>
              <div>
                <p className="font-medium">{authorName}</p>
                <p className="text-xs text-muted-foreground">{ticket.authorEmail}</p>
              </div>
            </div>
            {ticket.book ? (
              <dl className="mt-4 grid grid-cols-2 gap-x-3 gap-y-2 rounded-md border bg-muted p-3 text-xs">
                <dt className="col-span-2 font-display text-sm font-semibold">
                  {ticket.book.title}
                </dt>
                <dt className="text-muted-foreground">ISBN</dt>
                <dd className="font-mono">{ticket.book.isbn ?? "Pending"}</dd>
                <dt className="text-muted-foreground">Status</dt>
                <dd>{ticket.book.status ?? "Unavailable"}</dd>
                <dt className="text-muted-foreground">Copies sold</dt>
                <dd>{ticket.book.copiesSold ?? "Unavailable"}</dd>
                <dt className="text-muted-foreground">Earned</dt>
                <dd>
                  {ticket.book.royaltyEarned === null
                    ? "Unavailable"
                    : inr(ticket.book.royaltyEarned)}
                </dd>
                <dt className="text-muted-foreground">Paid</dt>
                <dd>
                  {ticket.book.royaltyPaid === null ? "Unavailable" : inr(ticket.book.royaltyPaid)}
                </dd>
                <dt className="text-muted-foreground">Pending</dt>
                <dd className="font-semibold text-amber">
                  {ticket.book.royaltyPending === null
                    ? "Unavailable"
                    : inr(ticket.book.royaltyPending)}
                </dd>
              </dl>
            ) : (
              <p className="mt-4 text-xs text-muted-foreground">General / account-level query.</p>
            )}
          </Card>
        </div>
      </div>
    </>
  );
}
