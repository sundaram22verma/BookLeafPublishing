import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { Quote } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Logo } from "@/components/portal";
import { signup, signupAdmin } from "@/lib/api";
import { updateSessionCache } from "@/lib/store";

export const Route = createFileRoute("/signup")({
  head: () => ({
    meta: [
      { title: "Create account — BookLeaf Portal" },
      {
        name: "description",
        content: "Create a BookLeaf author or operations support portal account.",
      },
      { property: "og:title", content: "Create account — BookLeaf Portal" },
      {
        property: "og:description",
        content: "Create a BookLeaf author or operations support portal account.",
      },
    ],
  }),
  component: SignupPage,
});

const schema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, "Name is required")
      .max(100, "Name must be 100 characters or fewer"),
    email: z
      .string()
      .trim()
      .email("Enter a valid email")
      .max(190, "Email must be 190 characters or fewer"),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .max(72, "Password must be 72 characters or fewer"),
    confirmPassword: z.string().min(1, "Please confirm your password"),
  })
  .superRefine((input, context) => {
    if (input.password !== input.confirmPassword) {
      context.addIssue({
        code: "custom",
        path: ["confirmPassword"],
        message: "Passwords do not match",
      });
    }
  });

function SignupPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [inviteToken, setInviteToken] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setInviteToken(new URLSearchParams(window.location.hash.slice(1)).get("invite") ?? "");
  }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const result = schema.safeParse({ name, email, password, confirmPassword });
    if (!result.success) {
      setError(result.error.issues[0]?.message ?? "Invalid input");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const user = inviteToken
        ? await signupAdmin(inviteToken, result.data.name, result.data.email, result.data.password)
        : await signup(result.data.name, result.data.email, result.data.password);
      updateSessionCache(queryClient, user);
      if (inviteToken && typeof window !== "undefined") {
        window.history.replaceState(null, "", window.location.pathname + window.location.search);
      }
      await navigate({ to: user.role === "admin" ? "/admin/dashboard" : "/author/dashboard" });
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : "Account creation failed. Please try again.",
      );
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
            Your publishing journey deserves support at every step.
          </p>
          <p className="mt-6 text-sm opacity-70">
            Join 22,000+ authors building their stories with BookLeaf.
          </p>
        </div>
        <p className="text-xs opacity-60">© 2026 BookLeaf Publishing</p>
      </div>
      <div className="flex items-center justify-center p-6 py-10">
        <form onSubmit={submit} noValidate className="w-full max-w-sm">
          <Logo className="mb-8 lg:hidden" />
          <h1 className="text-[28px] font-semibold">Create your account</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {inviteToken
              ? "Complete your invited administrator account."
              : "Join BookLeaf's author support portal."}
          </p>
          {!inviteToken && (
            <p className="mt-2 text-xs text-muted-foreground">
              Admin account? Ask an existing administrator for a secure invitation link.
            </p>
          )}
          <div className="mt-8 space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                name="name"
                autoComplete="name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Your full name"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="email">{inviteToken ? "Invited email address" : "Email"}</Label>
              <Input
                id="email"
                name="email"
                type="email"
                maxLength={190}
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@email.com"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                name="password"
                type="password"
                maxLength={72}
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="confirm-password">Confirm password</Label>
              <Input
                id="confirm-password"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </div>
            {error && (
              <p role="alert" className="text-sm text-destructive">
                {error}
              </p>
            )}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Creating account…" : "Create account"}
            </Button>
          </div>
          <p className="mt-8 text-center text-sm text-muted-foreground">
            Already have an account?{" "}
            <Link
              to="/login"
              className="font-medium text-primary underline-offset-4 hover:underline"
            >
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
