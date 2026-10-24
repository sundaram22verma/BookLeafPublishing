import { createFileRoute } from "@tanstack/react-router";
import { BookOpen, Hourglass } from "lucide-react";
import { BookStatusBadge, Card, EmptyState, PageHeader, fmtDate, inr } from "@/components/portal";
import { booksFor, useBooks, useSession } from "@/lib/store";

export const Route = createFileRoute("/author/books")({
  head: () => ({
    meta: [
      { title: "My Books — BookLeaf Author Portal" },
      { name: "description", content: "ISBNs, sales and royalty details for each of your books." },
      { property: "og:title", content: "My Books — BookLeaf" },
      {
        property: "og:description",
        content: "ISBNs, sales and royalty details for each of your books.",
      },
    ],
  }),
  component: Books,
});

const STAGES = [
  "Manuscript",
  "Editing",
  "Cover Design",
  "Typesetting",
  "Proofreading",
  "ISBN",
  "Printing",
  "Distribution Setup",
  "Live",
];

function Books() {
  const user = useSession()!;
  const query = useBooks(user.id);
  const books = booksFor(user.id, query.data);
  if (query.isPending)
    return (
      <>
        <PageHeader title="My Books" />
        <p role="status">Loading your books…</p>
      </>
    );
  if (query.isError)
    return (
      <>
        <PageHeader title="My Books" />
        <p role="alert" className="text-sm text-destructive">
          Unable to load books. {query.error.message}
        </p>
      </>
    );
  if (!books.length)
    return (
      <>
        <PageHeader title="My Books" />
        <EmptyState
          icon={BookOpen}
          title="No books yet"
          body="Once your manuscript is received, it'll appear here with live production updates."
        />
      </>
    );
  const inProd = books.filter((b) => b.status !== "Published & Live");
  const live = books.filter((b) => b.status === "Published & Live");
  return (
    <>
      <PageHeader
        title="My Books"
        subtitle={`${live.length} published · ${inProd.length} in production`}
      />
      {inProd.length > 0 && (
        <div className="mb-6 grid gap-4 md:grid-cols-2">
          {inProd.map((b) => {
            const idx = b.status ? STAGES.indexOf(b.status) : -1;
            return (
              <Card key={b.id}>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-display font-semibold">{b.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {b.genre ?? "Genre unavailable"} · MRP {b.mrp === null ? "—" : inr(b.mrp)}
                    </p>
                  </div>
                  <BookStatusBadge s={b.status} />
                </div>
                {idx >= 0 && (
                  <div className="mt-4 flex gap-1">
                    {STAGES.map((s, i) => (
                      <div
                        key={s}
                        title={s}
                        className={`h-1.5 flex-1 rounded-full ${i < idx ? "bg-primary" : i === idx ? "bg-amber" : "bg-accent"}`}
                      />
                    ))}
                  </div>
                )}
                <p className="mt-3 flex items-center gap-1.5 text-xs text-muted-foreground">
                  <Hourglass className="h-3.5 w-3.5" />{" "}
                  {idx >= 0
                    ? `Stage ${idx + 1} of ${STAGES.length}`
                    : "Production stage unavailable"}
                  . ISBN, sales and royalties appear once your book goes live.
                </p>
              </Card>
            );
          })}
        </div>
      )}
      <Card className="overflow-x-auto p-0">
        <table className="w-full min-w-[1000px] text-sm">
          <thead className="border-b bg-muted text-left text-[11px] uppercase tracking-wider text-muted-foreground">
            <tr>
              {[
                "Title",
                "ISBN",
                "Genre",
                "Published",
                "Status",
                "MRP",
                "Copies sold",
                "Earned",
                "Paid",
                "Pending",
              ].map((h, i) => (
                <th key={h} className={`px-4 py-3 font-semibold ${i >= 5 ? "text-right" : ""}`}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y">
            {books.map((b) => {
              const pending = b.royaltyPending;
              return (
                <tr key={b.id} className="hover:bg-muted/50">
                  <td className="px-4 py-3 font-medium">{b.title}</td>
                  <td className="px-4 py-3 font-mono text-xs">
                    {b.isbn ?? <span className="text-muted-foreground">Pending</span>}
                  </td>
                  <td className="px-4 py-3">{b.genre ?? "—"}</td>
                  <td className="px-4 py-3">{fmtDate(b.publicationDate)}</td>
                  <td className="px-4 py-3">
                    <BookStatusBadge s={b.status} />
                  </td>
                  <td className="px-4 py-3 text-right">{b.mrp === null ? "—" : inr(b.mrp)}</td>
                  <td className="px-4 py-3 text-right">
                    {b.copiesSold === null ? "—" : b.copiesSold.toLocaleString("en-IN")}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {b.royaltyEarned === null || b.royaltyEarned === 0 ? (
                      <span className="text-muted-foreground">—</span>
                    ) : (
                      inr(b.royaltyEarned)
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {b.royaltyPaid === null || b.royaltyPaid === 0 ? (
                      <span className="text-muted-foreground">—</span>
                    ) : (
                      inr(b.royaltyPaid)
                    )}
                  </td>
                  <td
                    className={`px-4 py-3 text-right font-semibold ${pending !== null && pending > 0 ? "text-amber" : "text-muted-foreground"}`}
                  >
                    {pending !== null && pending > 0 ? inr(pending) : "—"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </Card>
      <p className="mt-3 text-xs text-muted-foreground">
        Royalties are 80% of net profit, calculated quarterly and paid within 45 days of quarter
        end. Minimum payout ₹1,000.
      </p>
    </>
  );
}
