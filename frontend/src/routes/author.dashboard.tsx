import { createFileRoute, Link } from "@tanstack/react-router";
import { BookOpen, IndianRupee, PlusCircle, Clock, TrendingUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BookStatusBadge, Card, PageHeader, StatusBadge, ago, inr } from "@/components/portal";
import { booksFor, useAuthorTickets, useBooks, useSession } from "@/lib/store";

export const Route = createFileRoute("/author/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard — BookLeaf Author Portal" },
      { name: "description", content: "Your books, royalties and open support queries." },
      { property: "og:title", content: "Author Dashboard — BookLeaf" },
      { property: "og:description", content: "Your books, royalties and open support queries." },
    ],
  }),
  component: Dashboard,
});

function Dashboard() {
  const user = useSession()!;
  const booksQuery = useBooks(user.id);
  const ticketsQuery = useAuthorTickets(user);
  const books = booksFor(user.id, booksQuery.data);
  const tickets = ticketsQuery.data ?? [];
  if (booksQuery.isPending || ticketsQuery.isPending)
    return <p role="status">Loading your dashboard…</p>;
  if (booksQuery.isError || ticketsQuery.isError)
    return (
      <p role="alert" className="text-sm text-destructive">
        Unable to load your dashboard. {booksQuery.error?.message ?? ticketsQuery.error?.message}
      </p>
    );
  const total = (value: "copiesSold" | "royaltyEarned" | "royaltyPaid" | "royaltyPending") =>
    books.some((book) => book[value] === null)
      ? null
      : books.reduce((sum, book) => sum + (book[value] ?? 0), 0);
  const earned = total("royaltyEarned");
  const paid = total("royaltyPaid");
  const pending = total("royaltyPending");
  const sold = total("copiesSold");
  const stats = [
    {
      label: "Copies sold",
      value: sold === null ? "—" : sold.toLocaleString("en-IN"),
      icon: TrendingUp,
    },
    { label: "Royalty earned", value: earned === null ? "—" : inr(earned), icon: IndianRupee },
    { label: "Royalty paid", value: paid === null ? "—" : inr(paid), icon: BookOpen },
    {
      label: "Royalty pending",
      value: pending === null ? "—" : inr(pending),
      icon: Clock,
      hl: true,
    },
  ];
  return (
    <>
      <PageHeader
        title={`Good to see you, ${user.name.split(" ")[0]}`}
        subtitle="Here's how your books are doing."
        action={
          <Button asChild>
            <Link to="/author/tickets/new">
              <PlusCircle className="h-4 w-4" /> Raise a query
            </Link>
          </Button>
        }
      />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.label} className={s.hl ? "border-amber/30 bg-amber-soft/40" : ""}>
            <s.icon className="h-4 w-4 text-muted-foreground" />
            <p className="mt-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {s.label}
            </p>
            <p className="mt-1 font-display text-2xl font-bold">{s.value}</p>
          </Card>
        ))}
      </div>
      {pending !== null && pending > 0 && pending < 1000 && (
        <p className="mt-3 text-xs text-muted-foreground">
          Pending royalties below ₹1,000 roll over to the next quarter's payout.
        </p>
      )}
      <div className="mt-8 grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">My books</h2>
            <Link to="/author/books" className="text-sm font-medium text-primary hover:underline">
              View all
            </Link>
          </div>
          <ul className="mt-4 divide-y">
            {books.map((b) => (
              <li key={b.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate font-medium">{b.title}</p>
                  <p className="font-mono text-xs text-muted-foreground">
                    {b.isbn ?? "ISBN pending"}
                  </p>
                </div>
                <BookStatusBadge s={b.status} />
              </li>
            ))}
          </ul>
        </Card>
        <Card className="lg:col-span-2">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Recent tickets</h2>
            <Link to="/author/tickets" className="text-sm font-medium text-primary hover:underline">
              View all
            </Link>
          </div>
          {tickets.length === 0 ? (
            <p className="mt-4 text-sm text-muted-foreground">
              No queries yet. We're here whenever you need us.
            </p>
          ) : (
            <ul className="mt-4 divide-y">
              {tickets.slice(0, 4).map((t) => (
                <li key={t.id}>
                  <Link
                    to="/author/tickets/$id"
                    params={{ id: t.id }}
                    className="flex items-center justify-between gap-3 py-3 hover:opacity-80"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{t.subject}</p>
                      <p className="text-xs text-muted-foreground">
                        {t.id} · {ago(t.updatedAt)}
                      </p>
                    </div>
                    <StatusBadge s={t.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}
