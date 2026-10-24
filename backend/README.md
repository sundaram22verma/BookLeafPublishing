# BookLeaf Author Support & Communication Portal: Backend

Spring Boot 3.4 · Java 21 · MySQL · Spring Data JPA · Spring Security (JWT, RBAC) · Spring AI (Groq)

## Run locally
```bash
# 1. MySQL running locally (the DB is auto-created) or set DB_URL
export DB_USERNAME=bookleaf_app DB_PASSWORD=yourpassword
export GROQ_API_KEY=...                # optional: without it AI degrades gracefully
export GROQ_MODEL=qwen/qwen3.8-27b     # optional: default Groq production model
export JWT_SECRET="at-least-32-random-characters-please!!"
# Optional first-boot admin/author/book seed; provide distinct passwords of at least 12 characters.
export SEED_ENABLED=true SEED_ADMIN_PASSWORD="..." SEED_AUTHOR_PASSWORD="..."
mvn spring-boot:run
```
Swagger UI: `http://localhost:9090/swagger-ui.html` (click Authorize, paste the login token).
Support tickets can include up to 5 uploaded files (10 MB each; images, PDF, Word, Excel, or CSV).
Files are stored outside the web root in `ATTACHMENT_STORAGE_DIR` (default `./uploads`); configure
that directory on persistent storage for deployments. Attachment downloads require the ticket owner's
AUTHOR session or an ADMIN session.

The provided author/book seed dataset is included at `src/main/resources/bookleaf_sample_data.json`.
Seeding is disabled by default; when enabled against an empty database, the configured admin and author
passwords are required. Self-service signup always creates author accounts. After the first admin has been
provisioned through the seed or another trusted process, admins can create email-bound invitations from
the admin dashboard. The recipient must use the link and matching email within 48 hours to create an admin
account. Invitation email delivery is not configured; share the generated link securely.

| Env var | Purpose | Default |
|---|---|---|
| `GROQ_API_KEY` | Groq API key (server-side only) | none: AI features degrade |
| `GROQ_MODEL` | Groq hosted model ID | `qwen/qwen3.8-27b` |
| `AI_ENABLED` | kill switch | `true` |
| `JWT_SECRET` | Unique HMAC key, at least 32 bytes | Set explicitly; the application config includes an insecure development fallback |
| `DB_URL` | MySQL connection URL | localhost `bookleaf` database |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL credentials for a least-privilege application account | required |
| `CORS_ALLOWED_ORIGINS` | comma-separated frontends | localhost:3000, 5173, 8080 |
| `ATTACHMENT_STORAGE_DIR` | Private persistent directory for uploaded ticket files | `./uploads` |
| `SEED_ENABLED` | Seed admin and sample authors/books on an empty database | `false` |

Always configure a unique, randomly generated `JWT_SECRET`. The former source fallback was public,
so treat tokens signed with it as compromised: setting a new secret and restarting invalidates all
existing login sessions.

## Deploy the backend to Railway

The backend is a Java service and runs separately from the Vercel frontend. The included `Dockerfile`
builds it with Java 21 and listens on Railway's `PORT`. Create a Railway project, add a MySQL service,
and deploy the monorepo repository as a service with its root directory set to `backend`, the sibling
directory containing this README and `Dockerfile`. Railway should detect the Dockerfile automatically.

Add a Railway volume mounted at `/data` so uploaded ticket attachments survive restarts. Set these
variables on the backend service, replacing `MySQL` with the actual Railway MySQL service name if
it differs:

| Variable | Value |
|---|---|
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=UTC` |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `JWT_SECRET` | A newly generated random secret of at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | `https://<your-project>.vercel.app` (add any custom production domain too) |
| `ATTACHMENT_STORAGE_DIR` | `/data/uploads` |
| `GROQ_API_KEY` | Optional; set only if AI triage and drafts should be enabled |

Railway provides `PORT` automatically. After deployment, copy the backend's public HTTPS domain and
set the Vercel `VITE_API_BASE_URL` to `https://<backend-domain>/api`. Redeploy the Vercel frontend
after setting that variable. Keep database credentials, the JWT secret, and the Groq key only in
Railway's service variables; do not add them to source control.

## API (JSON except multipart ticket uploads; errors use one shape: `timestamp,status,error,message,path,fieldErrors`)
| Method & path | Role | Purpose |
|---|---|---|
| POST `/api/auth/login` · `/api/auth/signup` · `/api/auth/admin-signup` · GET `/api/auth/me` | public / any | JWT login, author signup, invitation-based admin signup, whoami |
| POST `/api/admin/invites` | ADMIN | create a one-time, email-bound admin invitation (expires in 48 hours) |
| GET `/api/author/books` | AUTHOR | own books + royalty figures |
| POST `/api/author/tickets` | AUTHOR | create ticket (`bookId` null = General); accepts JSON or multipart with `ticket` JSON and optional `files` |
| GET `/api/author/tickets/{id}/attachments/{attachmentId}` | AUTHOR | download an attachment from own ticket |
| GET `/api/author/tickets?since=` | AUTHOR | own tickets + admin replies; `since` = poll for changes only |
| GET `/api/author/tickets/stream` | AUTHOR | live ticket update stream (SSE) |
| GET `/api/author/tickets/{id}` · POST `/api/author/tickets/{id}/replies` | AUTHOR | detail, follow-up message |
| GET `/api/admin/tickets` | ADMIN | queue; filters `status,category,priority,assignedAdminId,from,to,q,page,size` |
| GET `/api/admin/tickets/agents` | ADMIN | list assignable admin users |
| GET `/api/admin/tickets/{id}` | ADMIN | detail + thread + internal notes + book |
| GET `/api/admin/tickets/{id}/attachments/{attachmentId}` | ADMIN | download a ticket attachment |
| PATCH `/api/admin/tickets/{id}/status` · `/category` · `/priority` | ADMIN | status change; override AI category / priority |
| POST `/api/admin/tickets/{id}/assign` · PATCH `/{id}/assignee` | ADMIN | assign to me; assign/unassign admin |
| POST `/api/admin/tickets/{id}/notes` · `/responses` | ADMIN | internal note; reply to author |
| POST `/api/admin/tickets/{id}/ai-draft?regenerate=` · `/retriage` | ADMIN | AI draft reply; re-run AI triage |

Enum values: status `OPEN|IN_PROGRESS|RESOLVED|CLOSED`, priority `CRITICAL|HIGH|MEDIUM|LOW`, category
`ROYALTY_PAYMENTS|ISBN_METADATA|PRINTING_QUALITY|DISTRIBUTION|PRODUCTION_STATUS|GENERAL_INQUIRY`.
Roles are the enum `AUTHOR`/`ADMIN`. Author ticket updates use the SSE stream, with polling via
`GET /api/author/tickets?since=<max lastActivityAt>` as a fallback.

## Design decisions
- **Layering**: controller (HTTP only) → service (rules, transactions) → repository. DTOs are records; entities never leave the service layer.
  DTOs live in holder classes (`AuthDtos`, `AuthorDtos`, `AdminDtos`) instead of sub-packages.
- **Authorisation**: URL rules by role plus ownership in the query (`findByIdAndAuthorId`); another author's ticket returns 404, not 403.
- **Admin queue ordering**: `queue_rank = priority rank (+10 if resolved/closed)`, indexed with `created_at`, so
  "most urgent, then oldest, active first" is one index-friendly `ORDER BY`. Changing priority/status recomputes it.
- **AI never blocks the ticket flow**: ticket is saved with defaults, then triaged asynchronously after commit.
- **Overrides are sticky**: `categoryOverridden` / `priorityOverridden` stop later AI runs from undoing an admin's decision.
- **Author privacy**: authors never see priority, AI fields or internal notes (separate DTOs).

## AI integration
- **Provider**: Groq's OpenAI-compatible Chat Completions endpoint (`https://api.groq.com/openai/v1/chat/completions`), configured through Spring AI's OpenAI client. The API key stays server-side.
- **Model**: Groq `qwen/qwen3.8-27b`, configurable through `GROQ_MODEL`; `AiService` is the only application service that calls the LLM.
- **Triage**: one call returns category + priority + one-line reasoning as structured JSON (enums constrain the answer), temperature 0, max 150 output tokens. Rubric in the prompt encodes BookLeaf rules (ISBN errors ≥ High, long-unpaid royalty Critical, etc.). Ticket text is passed as data, not instructions.
- **Drafting**: on demand when an admin opens the draft. Prompt = persona + company overview/tone + **only the KB section for the ticket's category** (with its sample answer) + writing rules (no invented facts, own mistakes, give a timeline, end with a next step) + book figures + pre-computed royalty payout dates (the model quotes dates rather than calculating them).
- **Cost control**: KB filtered by category; description truncated; only last 3 thread messages (400 chars each); drafts cached on the ticket and invalidated on new author messages or category override; max-token caps; no LLM call at read time.
- **Degradation**: key missing / API down / rate limited / bad output → triage falls back to keyword rules (`aiClassified=false`, flagged in the queue); draft endpoint returns `available:false` and the admin writes manually; a circuit breaker (3 failures → 60 s pause) avoids slow retries; `/retriage` fixes fallback tickets later. Calls use 5 s connect and 45 s read timeouts.
- **Key security**: key comes from `GROQ_API_KEY` only; never sent to or readable by clients. Provider exceptions are logged without response bodies or credential-bearing details.

## Known limitations / next steps
- `GroqApiIntegrationTest` makes one real Groq completion when `GROQ_API_KEY` is set; it is skipped when the key is absent. Core fallback/error behavior is tested with simulated unauthorized, bad-request, rate-limit, server, and network failures.
- `ddl-auto=update` instead of Flyway migrations; consider Testcontainers and a mocked `ChatClient` for service-level tests.
- Login rate limiting is in-memory and per instance; ticket creation is not rate limited. Uploaded files require persistent storage to survive deployments; no refresh tokens.
- Triage saves the whole ticket row, so a same-second admin edit could be overwritten (use a targeted update or `@Version` in production).
- Sample-data seeder matches field names tolerantly; verify the seeded counts (10 authors, 18 books) in the startup log when seeding is enabled.
