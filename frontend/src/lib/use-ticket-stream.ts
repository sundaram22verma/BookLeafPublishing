import { useEffect, useSyncExternalStore } from "react";
import { fetchEventSource } from "@microsoft/fetch-event-source";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import {
  ApiError,
  clearToken,
  getAccessToken,
  getAuthorTicket,
  getAuthorTicketStreamUrl,
  getAuthorTicketsSince,
  subscribeToAccessToken,
} from "./api";
import type { Ticket, User } from "./data";
import { queryKeys } from "./store";

class UnauthorizedStreamError extends Error {}

export function useTicketStream(author: User | null | undefined) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const accessToken = useSyncExternalStore(subscribeToAccessToken, getAccessToken, () => null);
  const authorId = author?.id;
  const authorName = author?.name;

  useEffect(() => {
    if (!authorId || !authorName || !accessToken) return;

    const authorUser = { id: authorId, name: authorName };
    const controller = new AbortController();
    let consecutiveFailures = 0;
    let fallbackTimer: ReturnType<typeof setInterval> | undefined;
    let redirected = false;

    const logoutOnUnauthorized = () => {
      if (redirected) return;
      redirected = true;
      clearToken();
      queryClient.setQueryData(queryKeys.session, null);
      void navigate({ to: "/login", replace: true });
    };

    const mergeTicket = (ticket: Ticket) => {
      queryClient.setQueryData<Ticket[]>(queryKeys.authorTickets(authorId), (current) => {
        if (!current) return current;
        const index = current.findIndex((existing) => existing.id === ticket.id);
        if (index < 0) return [...current, ticket];
        const updated = [...current];
        updated[index] = ticket;
        return updated;
      });
      queryClient.setQueryData(queryKeys.authorTicket(authorId, ticket.id), ticket);
    };

    const latestActivityAt = () => {
      const timestamps: string[] = [];
      const tickets = queryClient.getQueryData<Ticket[]>(queryKeys.authorTickets(authorId));
      tickets?.forEach((ticket) => timestamps.push(ticket.updatedAt));
      queryClient
        .getQueriesData<Ticket>({ queryKey: ["author-ticket", authorId] })
        .forEach(([, ticket]) => {
          if (ticket) timestamps.push(ticket.updatedAt);
        });
      return timestamps.sort().at(-1);
    };

    const catchUp = async () => {
      try {
        const changed = await getAuthorTicketsSince(authorUser, latestActivityAt());
        changed.forEach(mergeTicket);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          logoutOnUnauthorized();
          throw new UnauthorizedStreamError("Authentication required");
        }
        throw error;
      }
    };

    const pollFallback = async () => {
      try {
        await catchUp();
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) logoutOnUnauthorized();
      }
    };

    const startFallback = () => {
      if (!fallbackTimer) fallbackTimer = setInterval(() => void pollFallback(), 10_000);
    };

    const stopFallback = () => {
      if (fallbackTimer) clearInterval(fallbackTimer);
      fallbackTimer = undefined;
    };

    void fetchEventSource(getAuthorTicketStreamUrl(), {
      method: "GET",
      headers: { Authorization: `Bearer ${accessToken}` },
      signal: controller.signal,
      openWhenHidden: true,
      async onopen(response) {
        if (response.status === 401) {
          logoutOnUnauthorized();
          throw new UnauthorizedStreamError("Authentication required");
        }
        if (!response.ok) throw new Error(`Ticket stream failed (${response.status})`);
        if (!response.headers.get("content-type")?.includes("text/event-stream")) {
          throw new Error("Ticket stream returned an unexpected content type");
        }
        await catchUp();
        consecutiveFailures = 0;
        stopFallback();
      },
      onmessage(message) {
        if (message.event !== "ticket-updated") return;
        let ticketId: string;
        try {
          const payload: unknown = JSON.parse(message.data);
          if (!payload || typeof payload !== "object" || !("ticketId" in payload)) return;
          const value = payload.ticketId;
          if (typeof value !== "number" && typeof value !== "string") return;
          ticketId = String(value);
        } catch {
          return;
        }
        void getAuthorTicket(ticketId, authorUser)
          .then(mergeTicket)
          .catch((error: unknown) => {
            if (error instanceof ApiError && error.status === 401) logoutOnUnauthorized();
            else startFallback();
          });
      },
      onclose() {
        throw new Error("Ticket stream closed");
      },
      onerror(error) {
        if (error instanceof UnauthorizedStreamError) throw error;
        consecutiveFailures += 1;
        if (consecutiveFailures >= 3) startFallback();
        return Math.min(1_000 * 2 ** (consecutiveFailures - 1), 30_000);
      },
    }).catch((error: unknown) => {
      if (error instanceof UnauthorizedStreamError) logoutOnUnauthorized();
    });

    return () => {
      controller.abort();
      stopFallback();
    };
  }, [accessToken, authorId, authorName, navigate, queryClient]);
}
