import { createFileRoute } from "@tanstack/react-router";
import { PortalLayout } from "@/components/portal";

export const Route = createFileRoute("/author")({
  ssr: false,
  component: () => <PortalLayout role="author" />,
});
