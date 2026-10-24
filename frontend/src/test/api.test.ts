import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  getAuthorBooks,
  getAuthorTicket,
  getAuthorTickets,
  getAuthorTicketsSince,
  getCurrentUser,
  createAdminInvite,
  createAuthorTicket,
  signupAdmin,
  login,
  signup,
} from "@/lib/api";

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

describe("BookLeaf API service", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("authenticates against the backend and uses the returned bearer token", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          token: "signed-token",
          user: { id: 12, name: "A Book Author", email: "author@example.com", role: "AUTHOR" },
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          id: 12,
          name: "A Book Author",
          email: "author@example.com",
          role: "AUTHOR",
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    const user = await login("author@example.com", "password-123");
    expect(user).toEqual({
      id: "12",
      name: "A Book Author",
      email: "author@example.com",
      role: "author",
    });
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:9090/api/auth/login");
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toEqual({
      email: "author@example.com",
      password: "password-123",
    });
    expect(localStorage.getItem("bookleaf-access-token")).toBe("signed-token");

    await getCurrentUser();
    expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).get("Authorization")).toBe(
      "Bearer signed-token",
    );
  });

  it("creates only author accounts and surfaces backend validation errors", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(
          {
            token: "author-token",
            user: { id: 14, name: "A New Author", email: "new@example.com", role: "AUTHOR" },
          },
          201,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse({ message: "An account with this email already exists" }, 409),
      );
    vi.stubGlobal("fetch", fetchMock);

    await signup("A New Author", "new@example.com", "password-123");
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toEqual({
      name: "A New Author",
      email: "new@example.com",
      password: "password-123",
    });
    const failure = await signup("A New Author", "new@example.com", "password-123").catch(
      (error: unknown) => error,
    );
    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({
      status: 409,
      message: "An account with this email already exists",
    });
    expect(localStorage.getItem("bookleaf-access-token")).toBe("author-token");
  });

  it("accepts an admin invitation and uses the returned admin session", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          token: "admin-token",
          user: { id: 18, name: "New Admin", email: "admin@example.com", role: "ADMIN" },
        },
        201,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      signupAdmin("one-time-invite", "New Admin", "admin@example.com", "password-123"),
    ).resolves.toMatchObject({ id: "18", role: "admin" });
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:9090/api/auth/admin-signup");
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toEqual({
      inviteToken: "one-time-invite",
      name: "New Admin",
      email: "admin@example.com",
      password: "password-123",
    });
    expect(localStorage.getItem("bookleaf-access-token")).toBe("admin-token");
  });

  it("creates an admin invitation using the authenticated admin session", async () => {
    localStorage.setItem("bookleaf-access-token", "admin-token");
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        jsonResponse({ token: "one-time-invite", expiresAt: "2026-10-25T10:00:00" }, 201),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(createAdminInvite("admin@example.com")).resolves.toEqual({
      token: "one-time-invite",
      expiresAt: "2026-10-25T10:00:00",
    });
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:9090/api/admin/invites");
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).get("Authorization")).toBe(
      `Bearer ${localStorage.getItem("bookleaf-access-token")}`,
    );
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toEqual({
      email: "admin@example.com",
    });
  });

  it("requests changed author tickets using the latest activity timestamp", async () => {
    localStorage.setItem("bookleaf-access-token", "signed-token");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]));
    vi.stubGlobal("fetch", fetchMock);
    const author = {
      id: "12",
      name: "A Book Author",
      email: "author@example.com",
      role: "author" as const,
    };

    await getAuthorTicketsSince(author, "2026-10-22T01:25:07");
    expect(new URL(String(fetchMock.mock.calls[0]?.[0])).searchParams.get("since")).toBe(
      "2026-10-22T01:25:07",
    );

    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        id: 21,
        bookId: 4,
        bookTitle: "Live Book",
        subject: "Production update",
        description: "Please share an update about production.",
        status: "OPEN",
        createdAt: "2026-10-20T10:00:00",
        lastActivityAt: "2026-10-22T01:25:08",
        responses: [],
      }),
    );
    await getAuthorTicket("21", author);
    expect(fetchMock.mock.calls[1]?.[0]).toBe("http://localhost:9090/api/author/tickets/21");
  });

  it("maps books and ticket messages from the backend response", async () => {
    localStorage.setItem("bookleaf-access-token", "signed-token");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse([
          {
            id: 4,
            title: "Live Book",
            isbn: null,
            genre: "Fiction",
            publicationDate: null,
            status: "Typesetting",
            mrp: 0,
            totalCopiesSold: 0,
            totalRoyaltyEarned: 0,
            royaltyPaid: 0,
            royaltyPending: 0,
          },
        ]),
      )
      .mockResolvedValueOnce(
        jsonResponse([
          {
            id: 20,
            bookId: 4,
            bookTitle: "Live Book",
            subject: "Production update",
            description: "Please share an update about production.",
            status: "IN_PROGRESS",
            createdAt: "2026-10-20T10:00:00",
            lastActivityAt: "2026-10-21T10:00:00",
            responses: [
              {
                id: 21,
                senderName: "Support",
                senderRole: "ADMIN",
                message: "We are checking.",
                createdAt: "2026-10-21T10:00:00",
              },
            ],
          },
        ]),
      );
    vi.stubGlobal("fetch", fetchMock);
    const author = {
      id: "12",
      name: "A Book Author",
      email: "author@example.com",
      role: "author" as const,
    };

    await expect(getAuthorBooks(author.id)).resolves.toMatchObject([
      { id: "4", authorId: "12", title: "Live Book", status: "Typesetting" },
    ]);
    await expect(getAuthorTickets(author)).resolves.toMatchObject([
      {
        id: "20",
        status: "In Progress",
        bookTitle: "Live Book",
        messages: [
          { from: "author", body: "Please share an update about production." },
          { from: "admin", body: "We are checking." },
        ],
      },
    ]);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).get("Authorization")).toBe(
      "Bearer signed-token",
    );
  });

  it("submits selected files and links as multipart ticket attachments", async () => {
    localStorage.setItem("bookleaf-access-token", "signed-token");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        id: 31,
        subject: "Missing royalty report",
        description: "Please check the attached report for this quarter.",
        status: "OPEN",
        attachmentUrls: ["https://example.com/report"],
        attachments: [
          {
            id: 9,
            fileName: "report.pdf",
            downloadUrl: "/author/tickets/31/attachments/9",
          },
        ],
        createdAt: "2026-10-20T10:00:00",
        lastActivityAt: "2026-10-20T10:00:00",
        responses: [],
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const author = {
      id: "12",
      name: "A Book Author",
      email: "author@example.com",
      role: "author" as const,
    };

    const ticket = await createAuthorTicket(
      {
        bookId: null,
        subject: "Missing royalty report",
        description: "Please check the attached report for this quarter.",
        attachmentUrls: ["https://example.com/report"],
        files: [new File(["report"], "report.pdf", { type: "application/pdf" })],
      },
      author,
    );

    const formData = fetchMock.mock.calls[0]?.[1]?.body as FormData;
    expect(formData.get("ticket")).toBeInstanceOf(Blob);
    expect((formData.get("ticket") as Blob).type).toBe("application/json");
    expect((formData.get("files") as File).name).toBe("report.pdf");
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).get("Content-Type")).toBeNull();
    expect(ticket.messages[0]?.uploadedAttachments).toEqual([
      {
        fileName: "report.pdf",
        downloadUrl: "/author/tickets/31/attachments/9",
      },
    ]);
  });
});
