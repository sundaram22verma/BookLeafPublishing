import { createFileRoute, Navigate } from "@tanstack/react-router";
import { useSession, useSessionError } from "@/lib/store";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "BookLeaf Author Support Portal" },
      {
        name: "description",
        content: "Track your books, royalties and support tickets with BookLeaf Publishing.",
      },
      { property: "og:title", content: "BookLeaf Author Support Portal" },
      {
        property: "og:description",
        content: "Track your books, royalties and support tickets with BookLeaf Publishing.",
      },
    ],
  }),
  component: Index,
});

function Index() {
  const user = useSession();
  const error = useSessionError();
  if (user === undefined)
    return (
      <p role="status" className="p-8 text-sm text-muted-foreground">
        {error ? `Unable to verify your session. ${error.message}` : "Loading…"}
      </p>
    );
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === "admin" ? "/admin/dashboard" : "/author/dashboard"} replace />;
}
