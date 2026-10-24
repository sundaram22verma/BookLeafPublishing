import { useEffect, useRef } from "react";
import { Paperclip } from "lucide-react";
import { toast } from "sonner";
import { downloadAttachment } from "@/lib/api";
import { cn } from "@/lib/utils";
import type { Message, Role } from "@/lib/data";
import { ago } from "./portal";

function isHttpUrl(value: string) {
  try {
    const protocol = new URL(value).protocol;
    return protocol === "http:" || protocol === "https:";
  } catch {
    return false;
  }
}

export function Thread({ messages, viewer }: { messages: Message[]; viewer: Role }) {
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => {
    end.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [messages.length]);
  return (
    <div className="space-y-4">
      {messages.map((m) => {
        const mine = m.from === viewer;
        const attachments = m.attachments ?? (m.attachment ? [m.attachment] : []);
        return (
          <div key={m.id} className={cn("flex", mine ? "justify-end" : "justify-start")}>
            <div
              className={cn(
                "max-w-[85%] rounded-lg px-4 py-3 text-sm",
                mine ? "bg-primary text-primary-foreground" : "border bg-muted",
              )}
            >
              <div
                className={cn(
                  "mb-1 flex items-center gap-2 text-xs font-semibold",
                  mine ? "opacity-80" : "text-muted-foreground",
                )}
              >
                {m.authorName}
                {m.from === "admin" && " · BookLeaf"}{" "}
                <span className="font-normal">{ago(m.at)}</span>
              </div>
              <p className="whitespace-pre-wrap leading-relaxed">{m.body}</p>
              {attachments.map((attachment, index) =>
                isHttpUrl(attachment) ? (
                  <a
                    key={`${attachment}-${index}`}
                    href={attachment}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mt-2 flex items-center gap-1.5 text-xs underline opacity-80"
                  >
                    <Paperclip className="h-3.5 w-3.5" />
                    {attachments.length > 1 ? `Open attachment ${index + 1}` : "Open attachment"}
                  </a>
                ) : (
                  <p key={`${attachment}-${index}`} className="mt-2 text-xs opacity-80">
                    Attachment link unavailable.
                  </p>
                ),
              )}
              {m.uploadedAttachments?.map((attachment) => (
                <button
                  key={attachment.downloadUrl}
                  type="button"
                  className="mt-2 flex max-w-full items-center gap-1.5 text-left text-xs underline opacity-80"
                  onClick={() =>
                    downloadAttachment(attachment.downloadUrl, attachment.fileName).catch((error) =>
                      toast.error(
                        error instanceof Error ? error.message : "Unable to download attachment.",
                      ),
                    )
                  }
                >
                  <Paperclip className="h-3.5 w-3.5 shrink-0" />
                  <span className="truncate">{attachment.fileName}</span>
                </button>
              ))}
            </div>
          </div>
        );
      })}
      <div ref={end} />
    </div>
  );
}
