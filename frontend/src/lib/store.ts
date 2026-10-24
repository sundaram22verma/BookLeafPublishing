import { useQuery, type QueryClient } from "@tanstack/react-query";
import {
  clearToken,
  getAdminTicket,
  getAdminTickets,
  getAdminUsers,
  getAuthorTicket,
  getAuthorBooks,
  getAuthorTickets,
  getCurrentUser,
  hasToken,
} from "./api";
import type { Book, Ticket, User } from "./data";
import { ApiError } from "./api";

export const queryKeys = {
  session: ["session"] as const,
  books: (authorId: string) => ["books", authorId] as const,
  authorTickets: (authorId: string) => ["author-tickets", authorId] as const,
  authorTicket: (authorId: string, ticketId: string) =>
    ["author-ticket", authorId, ticketId] as const,
  adminTickets: ["admin-tickets"] as const,
  adminTicket: (id: string) => ["admin-ticket", id] as const,
  adminUsers: ["admin-users"] as const,
};

export function useSession(): User | null | undefined {
  const query = useQuery({
    queryKey: queryKeys.session,
    queryFn: async () => {
      try {
        return await getCurrentUser();
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) clearToken();
        throw error;
      }
    },
    enabled: hasToken(),
    retry: false,
    staleTime: 60_000,
  });
  if (!hasToken()) return null;
  if (query.isError) return undefined;
  return query.data ?? (query.isPending ? undefined : null);
}

export function useSessionError() {
  const query = useQuery<User>({
    queryKey: queryKeys.session,
    queryFn: getCurrentUser,
    enabled: hasToken(),
    retry: false,
  });
  return hasToken() && query.error instanceof Error ? query.error : null;
}

export function useBooks(authorId: string) {
  return useQuery<Book[]>({
    queryKey: queryKeys.books(authorId),
    queryFn: () => getAuthorBooks(authorId),
    enabled: hasToken() && Boolean(authorId),
  });
}

export function useAuthorTickets(author: User) {
  return useQuery<Ticket[]>({
    queryKey: queryKeys.authorTickets(author.id),
    queryFn: () => getAuthorTickets(author),
    enabled: hasToken(),
  });
}

export function useAuthorTicket(id: string, author: User) {
  return useQuery<Ticket>({
    queryKey: queryKeys.authorTicket(author.id, id),
    queryFn: () => getAuthorTicket(id, author),
    enabled: hasToken() && Boolean(id),
  });
}

export function useAdminTickets() {
  return useQuery<Ticket[]>({
    queryKey: queryKeys.adminTickets,
    queryFn: getAdminTickets,
    enabled: hasToken(),
    refetchInterval: 10_000,
  });
}

export function useAdminTicket(id: string) {
  return useQuery<Ticket>({
    queryKey: queryKeys.adminTicket(id),
    queryFn: () => getAdminTicket(id),
    enabled: hasToken() && Boolean(id),
    refetchInterval: 5_000,
  });
}

export function useAdminUsers() {
  return useQuery<User[]>({
    queryKey: queryKeys.adminUsers,
    queryFn: getAdminUsers,
    enabled: hasToken(),
  });
}

export function booksFor(authorId: string, books: Book[] | undefined) {
  return books?.filter((book) => book.authorId === authorId) ?? [];
}

export function updateSessionCache(queryClient: QueryClient, user: User | null) {
  queryClient.setQueryData(queryKeys.session, user);
}

export function logout() {
  clearToken();
}
