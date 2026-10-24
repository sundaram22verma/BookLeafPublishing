import { createFileRoute, Link } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Inbox, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  Card,
  EmptyState,
  LiveIndicator,
  PageHeader,
  PriorityBadge,
  StatusBadge,
  ago,
} from "@/components/portal";
import { CATEGORIES, PRIORITIES, STATUSES, type Ticket } from "@/lib/data";
import { sortQueue } from "@/lib/api";
import { useAdminTickets } from "@/lib/store";

export const Route = createFileRoute("/admin/tickets/")({
  head: () => ({
    meta: [
      { title: "Ticket Queue — BookLeaf Admin" },
      { name: "description", content: "All author support tickets, prioritised by AI triage." },
      { property: "og:title", content: "Ticket Queue — BookLeaf" },
      {
        property: "og:description",
        content: "All author support tickets, prioritised by AI triage.",
      },
    ],
  }),
  component: Queue,
});

const DATES = {
  all: "Any time",
  "1": "Last 24 hours",
  "7": "Last 7 days",
  "30": "Last 30 days",
} as const;
const sel = "h-9 rounded-md border border-input bg-card px-2 text-sm";
const EMPTY_TICKETS: Ticket[] = [];

function Queue() {
  const query = useAdminTickets();
  const tickets = query.data ?? EMPTY_TICKETS;
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("active");
  const [cat, setCat] = useState("all");
  const [pri, setPri] = useState("all");
  const [date, setDate] = useState<keyof typeof DATES>("all");

  const rows = useMemo(
    () =>
      tickets
        .filter((t) => {
          if (
            status === "active"
              ? !(t.status === "Open" || t.status === "In Progress")
              : status !== "all" && t.status !== status
          )
            return false;
          if (cat !== "all" && t.category !== cat) return false;
          if (pri !== "all" && t.priority !== pri) return false;
          if (date !== "all" && Date.now() - new Date(t.createdAt).getTime() > +date * 86400000)
            return false;
          const s = q.toLowerCase();
          return (
            !s ||
            t.subject.toLowerCase().includes(s) ||
            t.id.toLowerCase().includes(s) ||
            t.authorName?.toLowerCase().includes(s)
          );
        })
        .sort(sortQueue),
    [tickets, q, status, cat, pri, date],
  );

  if (query.isPending)
    return (
      <>
        <PageHeader title="Ticket queue" />
        <p role="status">Loading ticket queue…</p>
      </>
    );
  if (query.isError)
    return (
      <>
        <PageHeader title="Ticket queue" />
        <p role="alert" className="text-sm text-destructive">
          Unable to load tickets. {query.error.message}
        </p>
      </>
    );

  return (
    <>
      <PageHeader
        title="Ticket queue"
        subtitle="Sorted by unresolved → priority → oldest first."
        action={<LiveIndicator label="Queue live" />}
      />
      <div className="mb-4 flex flex-wrap gap-2">
        <div className="relative min-w-[220px] flex-1">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            className="h-9 bg-card pl-8"
            placeholder="Search subject, ticket ID or author"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <select
          className={sel}
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          aria-label="Status"
        >
          <option value="active">Unresolved</option>
          <option value="all">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
        <select
          className={sel}
          value={cat}
          onChange={(e) => setCat(e.target.value)}
          aria-label="Category"
        >
          <option value="all">All categories</option>
          {CATEGORIES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
        <select
          className={sel}
          value={pri}
          onChange={(e) => setPri(e.target.value)}
          aria-label="Priority"
        >
          <option value="all">All priorities</option>
          {PRIORITIES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
        <select
          className={sel}
          value={date}
          onChange={(e) => setDate(e.target.value as keyof typeof DATES)}
          aria-label="Date"
        >
          {Object.entries(DATES).map(([k, v]) => (
            <option key={k} value={k}>
              {v}
            </option>
          ))}
        </select>
      </div>
      {rows.length === 0 ? (
        <EmptyState
          icon={Inbox}
          title="No tickets match"
          body="Try widening your filters. An empty unresolved queue means the team is fully caught up."
        />
      ) : (
        <Card className="overflow-x-auto p-0">
          <table className="w-full min-w-[900px] text-sm">
            <thead className="border-b bg-muted text-left text-[11px] uppercase tracking-wider text-muted-foreground">
              <tr>
                {["Priority", "Ticket", "Author", "Category", "Status", "Assignee", "Age"].map(
                  (h) => (
                    <th key={h} className="px-4 py-3 font-semibold">
                      {h}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody className="divide-y">
              {rows.map((t) => (
                <tr
                  key={t.id}
                  className={`hover:bg-muted/50 ${t.priority === "Critical" && t.status !== "Resolved" && t.status !== "Closed" ? "border-l-2 border-l-destructive" : ""}`}
                >
                  <td className="px-4 py-3">{t.priority && <PriorityBadge p={t.priority} />}</td>
                  <td className="px-4 py-3">
                    <Link
                      to="/admin/tickets/$id"
                      params={{ id: t.id }}
                      className="font-medium hover:underline"
                    >
                      {t.subject}
                    </Link>
                    <p className="font-mono text-xs text-muted-foreground">
                      #{t.id} · {t.bookTitle ?? "General"}
                    </p>
                  </td>
                  <td className="px-4 py-3">{t.authorName}</td>
                  <td className="px-4 py-3">
                    <span className="text-xs">{t.category}</span>
                    {t.categoryOverridden && (
                      <p className="text-[10px] text-muted-foreground">manually overridden</p>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge s={t.status} />
                  </td>
                  <td className="px-4 py-3 text-xs">
                    {t.assigneeName ?? <span className="text-muted-foreground">Unassigned</span>}
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{ago(t.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  );
}
