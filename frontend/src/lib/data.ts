export type Role = "author" | "admin";
export type BookStatus = string | null;
export type TicketStatus = "Open" | "In Progress" | "Resolved" | "Closed";
export type Priority = "Critical" | "High" | "Medium" | "Low";
export type Category =
  | "Royalty & Payments"
  | "ISBN & Metadata Issues"
  | "Printing & Quality"
  | "Distribution & Availability"
  | "Book Status & Production Updates"
  | "General Inquiry";

export const CATEGORIES: Category[] = [
  "Royalty & Payments",
  "ISBN & Metadata Issues",
  "Printing & Quality",
  "Distribution & Availability",
  "Book Status & Production Updates",
  "General Inquiry",
];
export const PRIORITIES: Priority[] = ["Critical", "High", "Medium", "Low"];
export const STATUSES: TicketStatus[] = ["Open", "In Progress", "Resolved", "Closed"];

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface Book {
  id: string;
  authorId: string;
  title: string;
  isbn: string | null;
  genre: string | null;
  publicationDate: string | null;
  status: BookStatus;
  mrp: number | null;
  copiesSold: number | null;
  royaltyEarned: number | null;
  royaltyPaid: number | null;
  royaltyPending: number | null;
}

export interface Message {
  id: string;
  from: Role;
  authorName: string;
  body: string;
  at: string;
  attachment?: string;
  attachments?: string[];
  uploadedAttachments?: { fileName: string; downloadUrl: string }[];
}

export interface Note {
  id: string;
  adminName: string;
  body: string;
  at: string;
}

export interface Ticket {
  id: string;
  authorId: string;
  authorName?: string;
  authorEmail?: string;
  bookId: string | null;
  bookTitle?: string | null;
  subject: string;
  status: TicketStatus;
  category?: Category;
  priority?: Priority;
  aiClassified?: boolean;
  categoryOverridden?: boolean;
  priorityOverridden?: boolean;
  assigneeId?: string | null;
  assigneeName?: string | null;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
  notes: Note[];
  description?: string;
  attachmentUrl?: string | null;
  attachmentUrls?: string[];
  book?: Book | null;
}
