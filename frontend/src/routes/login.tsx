import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Quote } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Logo } from "@/components/portal";
import { login } from "@/lib/api";
import { updateSessionCache } from "@/lib/store";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Sign in — BookLeaf Portal" },
      {
        name: "description",
        content: "Sign in to the BookLeaf author and operations support portal.",
      },
      { property: "og:title", content: "Sign in — BookLeaf Portal" },
      {
        property: "og:description",
        content: "Sign in to the BookLeaf author and operations support portal.",
      },
    ],
  }),
  component: LoginPage,
});

const schema = z.object({
  email: z.string().trim().email("Enter a valid email").max(190),
  password: z.string().min(1, "Password is required").max(72),
});

function LoginPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const r = schema.safeParse({ email, password });
    if (!r.success) return setError(r.error.issues[0]?.message ?? "Invalid input");
    setLoading(true);
    setError("");
    try {
      const user = await login(r.data.email, r.data.password);
      updateSessionCache(queryClient, user);
      await navigate({ to: user.role === "admin" ? "/admin/dashboard" : "/author/dashboard" });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Sign in failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between bg-primary p-12 text-primary-foreground lg:flex">
        <div className="flex items-center gap-2">
          <Logo className="[&>div]:bg-primary-foreground [&>div]:text-primary" />
        </div>
        <div>
          <Quote className="h-8 w-8 opacity-60" />
          <p className="mt-4 max-w-md font-display text-3xl font-semibold leading-tight">
            Authors are our partners. Every question deserves a specific, honest answer.
          </p>
          <p className="mt-6 text-sm opacity-70">
            1,200+ books every month · 22,000+ authors across India & the US
          </p>
        </div>
        <p className="text-xs opacity-60">© 2026 BookLeaf Publishing</p>
      </div>
      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="w-full max-w-sm">
          <Logo className="mb-10 lg:hidden" />
          <h1 className="text-[28px] font-semibold">Welcome back</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Sign in to your author or operations account.
          </p>
          <div className="mt-8 space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                maxLength={190}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@email.com"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="pw">Password</Label>
              <Input
                id="pw"
                type="password"
                maxLength={72}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Signing in…" : "Sign in"}
            </Button>
          </div>
          <p className="mt-6 text-center text-sm text-muted-foreground">
            New to BookLeaf?{" "}
            <Link
              to="/signup"
              className="font-medium text-primary underline-offset-4 hover:underline"
            >
              Create an account
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
