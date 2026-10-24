import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Link2, Paperclip, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, PageHeader } from "@/components/portal";
import { createAuthorTicket } from "@/lib/api";
import { booksFor, queryKeys, useBooks, useSession } from "@/lib/store";

export const Route = createFileRoute("/author/tickets/new")({
  head: () => ({
    meta: [
      { title: "New Support Query — BookLeaf" },
      {
        name: "description",
        content: "Raise a support query about a book or your BookLeaf account.",
      },
      { property: "og:title", content: "New Support Query — BookLeaf" },
      {
        property: "og:description",
        content: "Raise a support query about a book or your BookLeaf account.",
      },
    ],
  }),
  component: NewTicket,
});

const schema = z.object({
  subject: z.string().trim().min(5, "Subject must be at least 5 characters").max(150),
  body: z.string().trim().min(10, "Please describe the issue in at least 10 characters").max(5000),
  attachmentUrls: z
    .array(
      z
        .string()
        .trim()
        .max(500)
        .refine((value) => /^https?:\/\/\S+$/i.test(value), "Enter a valid http or https URL"),
    )
    .max(10, "You can add up to 10 links"),
});

function NewTicket() {
  const user = useSession()!;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const booksQuery = useBooks(user.id);
  const books = booksFor(user.id, booksQuery.data);
  const [bookId, setBookId] = useState("general");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [attachmentFiles, setAttachmentFiles] = useState<File[]>([]);
  const [attachmentUrls, setAttachmentUrls] = useState([""]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const attachmentFileRef = useRef<HTMLInputElement>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const r = schema.safeParse({
      subject,
      body,
      attachmentUrls: attachmentUrls.filter((value) => value.trim()),
    });
    if (!r.success)
      return setErrors(Object.fromEntries(r.error.issues.map((i) => [i.path[0], i.message])));
    setLoading(true);
    setErrors({});
    try {
      const ticket = await createAuthorTicket(
        {
          bookId: bookId === "general" ? null : bookId,
          subject: r.data.subject,
          description: r.data.body,
          attachmentUrls: r.data.attachmentUrls,
          files: attachmentFiles,
        },
        user,
      );
      await queryClient.invalidateQueries({ queryKey: queryKeys.authorTickets(user.id) });
      toast.success(`Ticket #${ticket.id} submitted`, {
        description: "Our team typically replies within 24 hours.",
      });
      await navigate({ to: "/author/tickets/$id", params: { id: ticket.id } });
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "Unable to submit your ticket.";
      setErrors({ submit: message });
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Raise a support query"
        subtitle="Tell us what's happening — the more specific, the faster we can help."
      />
      <Card>
        <form onSubmit={submit} className="space-y-5">
          <div className="space-y-1.5">
            <Label htmlFor="book">Which book is this about?</Label>
            {booksQuery.isError && (
              <p role="alert" className="text-xs text-destructive">
                Unable to load your books. {booksQuery.error.message}
              </p>
            )}
            <select
              id="book"
              value={bookId}
              onChange={(e) => setBookId(e.target.value)}
              className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
            >
              <option value="general">General / Account Level</option>
              {books.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.title}
                  {b.isbn ? ` (${b.isbn})` : ` — ${b.status ?? "Status unavailable"}`}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="subject">Subject</Label>
            <Input
              id="subject"
              value={subject}
              maxLength={150}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="e.g. Royalty not received for Q2"
            />
            {errors["subject"] && <p className="text-xs text-destructive">{errors["subject"]}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="body">Description</Label>
            <Textarea
              id="body"
              rows={7}
              value={body}
              maxLength={5000}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Include dates, order numbers, platforms or copy counts where relevant."
            />
            <div className="flex justify-between text-xs">
              {errors["body"] ? <p className="text-destructive">{errors["body"]}</p> : <span />}
              <span className="text-muted-foreground">{body.length}/5000</span>
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="attachmentFile">
              Attach a file <span className="font-normal text-muted-foreground">(optional)</span>
            </Label>
            <div className="flex flex-wrap items-center gap-3">
              <Input
                ref={attachmentFileRef}
                id="attachmentFile"
                type="file"
                accept=".png,.jpg,.jpeg,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.csv"
                multiple
                onChange={(e) => {
                  const selected = Array.from(e.target.files ?? []);
                  const existing = new Set(
                    attachmentFiles.map((file) => `${file.name}:${file.size}:${file.lastModified}`),
                  );
                  const nextFiles = [
                    ...attachmentFiles,
                    ...selected.filter(
                      (file) => !existing.has(`${file.name}:${file.size}:${file.lastModified}`),
                    ),
                  ];
                  if (nextFiles.length > 5) {
                    setErrors((current) => ({
                      ...current,
                      files: "You can attach up to 5 files.",
                    }));
                  } else if (nextFiles.some((file) => file.size > 10 * 1024 * 1024)) {
                    setErrors((current) => ({
                      ...current,
                      files: "Each file must be 10 MB or smaller.",
                    }));
                  } else {
                    setAttachmentFiles(nextFiles);
                    setErrors((current) => {
                      const rest = { ...current };
                      delete rest["files"];
                      return rest;
                    });
                  }
                  e.currentTarget.value = "";
                }}
                className="max-w-md"
              />
            </div>
            {attachmentFiles.length > 0 && (
              <ul className="space-y-1">
                {attachmentFiles.map((file, index) => (
                  <li
                    key={`${file.name}-${file.lastModified}-${index}`}
                    className="flex items-center gap-1 text-sm text-muted-foreground"
                  >
                    <Paperclip className="h-4 w-4 shrink-0" />
                    <span className="max-w-48 truncate" title={file.name}>
                      {file.name}
                    </span>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      aria-label={`Remove ${file.name}`}
                      onClick={() => {
                        setAttachmentFiles((files) =>
                          files.filter((_, fileIndex) => fileIndex !== index),
                        );
                        setErrors((current) => {
                          const rest = { ...current };
                          delete rest["files"];
                          return rest;
                        });
                        if (attachmentFileRef.current) attachmentFileRef.current.value = "";
                      }}
                    >
                      <X />
                    </Button>
                  </li>
                ))}
              </ul>
            )}
            {errors["files"] && <p className="text-xs text-destructive">{errors["files"]}</p>}
            <p className="text-xs text-muted-foreground">
              Attach up to 5 files, 10 MB each. Supported: images, PDF, Word, Excel, and CSV.
            </p>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="attachmentUrl">
              Attachment links <span className="font-normal text-muted-foreground">(optional)</span>
            </Label>
            <div className="space-y-2">
              {attachmentUrls.map((url, index) => (
                <div key={index} className="flex gap-2">
                  <div className="relative flex-1">
                    <Link2 className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input
                      id={index === 0 ? "attachmentUrl" : `attachmentUrl-${index}`}
                      type="url"
                      value={url}
                      onChange={(e) =>
                        setAttachmentUrls((urls) =>
                          urls.map((current, urlIndex) =>
                            urlIndex === index ? e.target.value : current,
                          ),
                        )
                      }
                      className="pl-9"
                      placeholder="https://…"
                    />
                  </div>
                  {attachmentUrls.length > 1 && (
                    <Button
                      type="button"
                      variant="outline"
                      aria-label={`Remove link ${index + 1}`}
                      onClick={() =>
                        setAttachmentUrls((urls) => urls.filter((_, i) => i !== index))
                      }
                    >
                      <X />
                    </Button>
                  )}
                </div>
              ))}
              {attachmentUrls.length < 10 && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setAttachmentUrls((urls) => [...urls, ""])}
                >
                  Add another link
                </Button>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              Add up to 10 shareable links to photos, documents, or sales reports.
            </p>
            {errors["attachmentUrls"] && (
              <p className="text-xs text-destructive">{errors["attachmentUrls"]}</p>
            )}
          </div>
          {errors["submit"] && (
            <p role="alert" className="text-sm text-destructive">
              {errors["submit"]}
            </p>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate({ to: "/author/tickets" })}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={loading || booksQuery.isPending}>
              {loading ? "Submitting…" : "Submit query"}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
