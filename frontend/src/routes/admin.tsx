import { createFileRoute } from "@tanstack/react-router";
import { PortalLayout } from "@/components/portal";

export const Route = createFileRoute("/admin")({
  ssr: false,
  component: () => <PortalLayout role="admin" />,
});
