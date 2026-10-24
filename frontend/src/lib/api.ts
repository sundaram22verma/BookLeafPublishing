import type {
  Book,
  Category,
  Message,
  Note,
  Priority,
  Role,
  Ticket,
  TicketStatus,
  User,
} from "./data";

const API_BASE_URL = (import.meta.env["VITE_API_BASE_URL"] || "http://localhost:9090/api").replace(
  /\/+$/,
  "",
);
const TOKEN_KEY = "bookleaf-access-token";
const accessTokenListeners = new Set<() => void>();

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type ApiUser = { id: number; name: string; email: string; role: string };
type AuthResponse = { token: string; user: ApiUser };
type ApiBook = {
  id: number;
  title: string;
  isbn?: string | null;
  genre?: string | null;
  publicationDate?: string | null;
  status?: string | null;
  mrp?: number | null;
  totalCopiesSold?: number | null;
  totalRoyaltyEarned?: number | null;
  royaltyPaid?: number | null;
  royaltyPending?: number | null;
};
type ApiMessage = {
  id: number;
  senderName: string;
  senderRole: string;
  message: string;
  createdAt: string;
};
type ApiAttachment = { id: number; fileName: string; downloadUrl: string };
type ApiAuthorTicket = {
  id: number;
  bookId?: number | null;
  bookTitle?: string | null;
  subject: string;
  description: string;
  status: string;
  attachmentUrl?: string | null;
  attachmentUrls?: string[];
  attachments?: ApiAttachment[];
  createdAt: string;
  lastActivityAt: string;
  responses: ApiMessage[];
};
type ApiTicketSummary = {
  id: number;
  authorId: number;
  authorName: string;
  authorEmail: string;
  bookId?: number | null;
  bookTitle?: string | null;
  subject: string;
  category: string;
  priority: string;
  status: string;
  assignedAdminId?: number | null;
  assignedAdminName?: string | null;
  aiClassified: boolean;
  categoryOverridden: boolean;
  priorityOverridden: boolean;
  createdAt: string;
  lastActivityAt: string;
};
type ApiAdminMessage = ApiMessage & { aiDrafted: boolean };
type ApiNote = { id: number; adminName: string; note: string; createdAt: string };
type ApiAdminDetail = {
  ticket: ApiTicketSummary;
  description: string;
  attachmentUrl?: string | null;
  attachmentUrls?: string[];
  attachments?: ApiAttachment[];
  book?: ApiBook | null;
  responses: ApiAdminMessage[];
  internalNotes: ApiNote[];
};
type ApiPage<T> = { content: T[]; page: number; totalPages: number };
type ApiAdminUser = { id: number; name: string; email: string };

export type AdminUser = User;
export type AiDraft = {
  available: boolean;
  draft: string | null;
  message: string | null;
  cached: boolean;
};
export type AdminInvite = { token: string; expiresAt: string };

function token() {
  return typeof window === "undefined" ? null : window.localStorage.getItem(TOKEN_KEY);
}

export function getAccessToken() {
  return token();
}

export function subscribeToAccessToken(listener: () => void) {
  accessTokenListeners.add(listener);
  const onStorage = (event: StorageEvent) => {
    if (event.key === TOKEN_KEY || event.key === null) listener();
  };
  if (typeof window !== "undefined") window.addEventListener("storage", onStorage);
  return () => {
    accessTokenListeners.delete(listener);
    if (typeof window !== "undefined") window.removeEventListener("storage", onStorage);
  };
}

function notifyAccessTokenListeners() {
  accessTokenListeners.forEach((listener) => listener());
}

function saveToken(value: string) {
  const previous = token();
  window.localStorage.setItem(TOKEN_KEY, value);
  if (previous !== value) notifyAccessTokenListeners();
}

export function clearToken() {
  if (typeof window !== "undefined" && token() !== null) {
    window.localStorage.removeItem(TOKEN_KEY);
    notifyAccessTokenListeners();
  }
}

export function hasToken() {
  return token() !== null;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const accessToken = token();
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  if (init.body !== undefined && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError("Cannot reach the BookLeaf service. Check your connection and API URL.", 0);
  }

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as {
      message?: string;
      fieldErrors?: Record<string, string>;
    } | null;
    if (response.status === 401) clearToken();
    throw new ApiError(
      error?.message || `Request failed (${response.status})`,
      response.status,
      error?.fieldErrors,
    );
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function role(value: string): Role {
  if (value === "ADMIN") return "admin";
  if (value === "AUTHOR") return "author";
  throw new Error(`Unexpected account role: ${value}`);
}

function ticketStatus(value: string): TicketStatus {
  const status = value
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
  if (status === "Open" || status === "In Progress" || status === "Resolved" || status === "Closed")
    return status;
  throw new Error(`Unexpected ticket status: ${value}`);
}

const CATEGORY_FROM_API: Record<string, Category> = {
  ROYALTY_PAYMENTS: "Royalty & Payments",
  ISBN_METADATA: "ISBN & Metadata Issues",
  PRINTING_QUALITY: "Printing & Quality",
  DISTRIBUTION: "Distribution & Availability",
  PRODUCTION_STATUS: "Book Status & Production Updates",
  GENERAL_INQUIRY: "General Inquiry",
};
const CATEGORY_TO_API = Object.fromEntries(
  Object.entries(CATEGORY_FROM_API).map(([key, value]) => [value, key]),
) as Record<Category, string>;

function category(value: string): Category {
  const mapped = CATEGORY_FROM_API[value];
  if (!mapped) throw new Error(`Unexpected ticket category: ${value}`);
  return mapped;
}

function priority(value: string): Priority {
  const mapped = value.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  if (mapped === "Critical" || mapped === "High" || mapped === "Medium" || mapped === "Low")
    return mapped;
  throw new Error(`Unexpected ticket priority: ${value}`);
}

function mapUser(user: ApiUser): User {
  return { id: String(user.id), name: user.name, email: user.email, role: role(user.role) };
}

function mapBook(book: ApiBook, authorId: number): Book {
  return {
    id: String(book.id),
    authorId: String(authorId),
    title: book.title,
    isbn: book.isbn ?? null,
    genre: book.genre ?? null,
    publicationDate: book.publicationDate ?? null,
    status: book.status ?? null,
    mrp: book.mrp ?? null,
    copiesSold: book.totalCopiesSold ?? null,
    royaltyEarned: book.totalRoyaltyEarned ?? null,
    royaltyPaid: book.royaltyPaid ?? null,
    royaltyPending: book.royaltyPending ?? null,
  };
}

function mapMessage(message: ApiMessage): Message {
  return {
    id: String(message.id),
    from: role(message.senderRole),
    authorName: message.senderName,
    body: message.message,
    at: message.createdAt,
  };
}

function initialMessage(
  ticket: {
    id: number;
    description: string;
    createdAt: string;
    attachmentUrl?: string | null;
    attachmentUrls?: string[];
    attachments?: ApiAttachment[];
  },
  authorName: string,
): Message {
  const attachments = [
    ...new Set([
      ...(ticket.attachmentUrls ?? []),
      ...(ticket.attachmentUrl ? [ticket.attachmentUrl] : []),
    ]),
  ];
  return {
    id: `ticket-${ticket.id}-description`,
    from: "author",
    authorName,
    body: ticket.description,
    at: ticket.createdAt,
    ...(attachments.length ? { attachment: attachments[0], attachments } : {}),
    ...(ticket.attachments?.length
      ? {
          uploadedAttachments: ticket.attachments.map((attachment) => ({
            fileName: attachment.fileName,
            downloadUrl: attachment.downloadUrl,
          })),
        }
      : {}),
  };
}

function mapAuthorTicket(ticket: ApiAuthorTicket, author: Pick<User, "id" | "name">): Ticket {
  return {
    id: String(ticket.id),
    authorId: author.id,
    authorName: author.name,
    bookId: ticket.bookId == null ? null : String(ticket.bookId),
    bookTitle: ticket.bookTitle ?? null,
    subject: ticket.subject,
    status: ticketStatus(ticket.status),
    createdAt: ticket.createdAt,
    updatedAt: ticket.lastActivityAt,
    description: ticket.description,
    attachmentUrl: ticket.attachmentUrl ?? null,
    attachmentUrls: ticket.attachmentUrls ?? (ticket.attachmentUrl ? [ticket.attachmentUrl] : []),
    messages: [initialMessage(ticket, author.name), ...ticket.responses.map(mapMessage)],
    notes: [],
  };
}

function mapTicketSummary(summary: ApiTicketSummary): Ticket {
  return {
    id: String(summary.id),
    authorId: String(summary.authorId),
    authorName: summary.authorName,
    authorEmail: summary.authorEmail,
    bookId: summary.bookId == null ? null : String(summary.bookId),
    bookTitle: summary.bookTitle ?? null,
    subject: summary.subject,
    category: category(summary.category),
    priority: priority(summary.priority),
    status: ticketStatus(summary.status),
    aiClassified: summary.aiClassified,
    categoryOverridden: summary.categoryOverridden,
    priorityOverridden: summary.priorityOverridden,
    assigneeId: summary.assignedAdminId == null ? null : String(summary.assignedAdminId),
    assigneeName: summary.assignedAdminName ?? null,
    createdAt: summary.createdAt,
    updatedAt: summary.lastActivityAt,
    messages: [],
    notes: [],
  };
}

function mapAdminDetail(detail: ApiAdminDetail): Ticket {
  const ticket = mapTicketSummary(detail.ticket);
  const original = initialMessage(
    {
      id: detail.ticket.id,
      description: detail.description,
      createdAt: detail.ticket.createdAt,
      attachmentUrl: detail.attachmentUrl ?? null,
      ...(detail.attachmentUrls !== undefined ? { attachmentUrls: detail.attachmentUrls } : {}),
      ...(detail.attachments !== undefined ? { attachments: detail.attachments } : {}),
    },
    detail.ticket.authorName,
  );
  return {
    ...ticket,
    description: detail.description,
    attachmentUrl: detail.attachmentUrl ?? null,
    attachmentUrls: detail.attachmentUrls ?? (detail.attachmentUrl ? [detail.attachmentUrl] : []),
    book: detail.book ? mapBook(detail.book, detail.ticket.authorId) : null,
    messages: [original, ...detail.responses.map(mapMessage)],
    notes: detail.internalNotes.map((note): Note => ({
      id: String(note.id),
      adminName: note.adminName,
      body: note.note,
      at: note.createdAt,
    })),
  };
}

export async function login(email: string, password: string): Promise<User> {
  const response = await request<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  saveToken(response.token);
  return mapUser(response.user);
}

export async function signup(name: string, email: string, password: string): Promise<User> {
  const response = await request<AuthResponse>("/auth/signup", {
    method: "POST",
    body: JSON.stringify({ name, email, password }),
  });
  saveToken(response.token);
  return mapUser(response.user);
}

export async function signupAdmin(
  inviteToken: string,
  name: string,
  email: string,
  password: string,
): Promise<User> {
  const response = await request<AuthResponse>("/auth/admin-signup", {
    method: "POST",
    body: JSON.stringify({ inviteToken, name, email, password }),
  });
  saveToken(response.token);
  return mapUser(response.user);
}

export async function createAdminInvite(email: string): Promise<AdminInvite> {
  return request<AdminInvite>("/admin/invites", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export async function getCurrentUser(): Promise<User> {
  return mapUser(await request<ApiUser>("/auth/me"));
}

export async function getAuthorBooks(authorId: string): Promise<Book[]> {
  const rows = await request<ApiBook[]>("/author/books");
  return rows.map((book) => mapBook(book, Number(authorId)));
}

export async function getAuthorTickets(author: Pick<User, "id" | "name">): Promise<Ticket[]> {
  return getAuthorTicketsSince(author);
}

export async function getAuthorTicketsSince(
  author: Pick<User, "id" | "name">,
  since?: string,
): Promise<Ticket[]> {
  const query = since ? `?${new URLSearchParams({ since })}` : "";
  const rows = await request<ApiAuthorTicket[]>(`/author/tickets${query}`);
  return rows.map((ticket) => mapAuthorTicket(ticket, author));
}

export function getAuthorTicketStreamUrl() {
  return `${API_BASE_URL}/author/tickets/stream`;
}

export async function getAuthorTicket(
  id: string,
  author: Pick<User, "id" | "name">,
): Promise<Ticket> {
  return mapAuthorTicket(
    await request<ApiAuthorTicket>(`/author/tickets/${encodeURIComponent(id)}`),
    author,
  );
}

export async function createAuthorTicket(
  input: {
    bookId: string | null;
    subject: string;
    description: string;
    attachmentUrls?: string[];
    files?: File[];
  },
  author: User,
): Promise<Ticket> {
  const formData = new FormData();
  formData.append(
    "ticket",
    new Blob(
      [
        JSON.stringify({
          bookId: input.bookId === null ? null : Number(input.bookId),
          subject: input.subject,
          description: input.description,
          attachmentUrls: input.attachmentUrls ?? [],
        }),
      ],
      { type: "application/json" },
    ),
  );
  input.files?.forEach((file) => formData.append("files", file, file.name));
  const created = await request<ApiAuthorTicket>("/author/tickets", {
    method: "POST",
    body: formData,
  });
  return mapAuthorTicket(created, author);
}

export async function downloadAttachment(downloadUrl: string, fileName: string) {
  if (!downloadUrl.startsWith("/author/tickets/") && !downloadUrl.startsWith("/admin/tickets/")) {
    throw new ApiError("Invalid attachment URL.", 400);
  }
  const headers = new Headers();
  const accessToken = token();
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  const response = await fetch(`${API_BASE_URL}${downloadUrl}`, { headers });
  if (!response.ok) {
    if (response.status === 401) clearToken();
    throw new ApiError(`Unable to download attachment (${response.status}).`, response.status);
  }
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}

export async function replyToAuthorTicket(id: string, message: string, author: User) {
  return mapAuthorTicket(
    await request<ApiAuthorTicket>(`/author/tickets/${encodeURIComponent(id)}/replies`, {
      method: "POST",
      body: JSON.stringify({ message }),
    }),
    author,
  );
}

export async function getAdminTickets(): Promise<Ticket[]> {
  const all: ApiTicketSummary[] = [];
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const result = await request<ApiPage<ApiTicketSummary>>(`/admin/tickets?page=${page}&size=100`);
    all.push(...result.content);
    totalPages = result.totalPages;
    page += 1;
  }
  return all.map(mapTicketSummary);
}

export async function getAdminTicket(id: string): Promise<Ticket> {
  return mapAdminDetail(await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}`));
}

export async function getAdminUsers(): Promise<AdminUser[]> {
  return (await request<ApiAdminUser[]>("/admin/tickets/agents")).map((user) => ({
    id: String(user.id),
    name: user.name,
    email: user.email,
    role: "admin",
  }));
}

export async function createTicketReply(id: string, message: string, aiDrafted: boolean) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/responses`, {
      method: "POST",
      body: JSON.stringify({ message, aiDrafted }),
    }),
  );
}

export async function createInternalNote(id: string, note: string) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/notes`, {
      method: "POST",
      body: JSON.stringify({ note }),
    }),
  );
}

export async function updateTicketStatus(id: string, status: TicketStatus) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: status.toUpperCase().replaceAll(" ", "_") }),
    }),
  );
}

export async function updateTicketCategory(id: string, value: Category) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/category`, {
      method: "PATCH",
      body: JSON.stringify({ category: CATEGORY_TO_API[value] }),
    }),
  );
}

export async function updateTicketPriority(id: string, value: Priority) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/priority`, {
      method: "PATCH",
      body: JSON.stringify({ priority: value.toUpperCase() }),
    }),
  );
}

export async function updateTicketAssignee(id: string, adminId: string | null) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/assignee`, {
      method: "PATCH",
      body: JSON.stringify({ adminId: adminId === null ? null : Number(adminId) }),
    }),
  );
}

export async function assignTicketToMe(id: string) {
  return mapAdminDetail(
    await request<ApiAdminDetail>(`/admin/tickets/${encodeURIComponent(id)}/assign`, {
      method: "POST",
    }),
  );
}

export async function getAiDraft(id: string, regenerate = false): Promise<AiDraft> {
  return request<AiDraft>(
    `/admin/tickets/${encodeURIComponent(id)}/ai-draft?regenerate=${regenerate}`,
    { method: "POST" },
  );
}

export function sortQueue(a: Ticket, b: Ticket) {
  const active = (status: TicketStatus) => status === "Open" || status === "In Progress";
  const rank: Record<Priority, number> = { Critical: 0, High: 1, Medium: 2, Low: 3 };
  const statusDifference = Number(!active(a.status)) - Number(!active(b.status));
  if (statusDifference !== 0) return statusDifference;
  const priorityDifference = rank[a.priority ?? "Low"] - rank[b.priority ?? "Low"];
  return priorityDifference || a.createdAt.localeCompare(b.createdAt);
}
