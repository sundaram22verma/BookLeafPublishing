# BookLeaf Author Support Portal

BookLeaf is a full-stack support and communication portal for BookLeaf authors and operations staff. Authors can review their books and royalty figures, submit support tickets, and follow up in a conversation thread. Administrators can triage and manage the ticket queue, respond to authors, and keep private internal notes.

## Key features

- Separate author and administrator experiences with role-based access.
- Author book details including publication status, sales, and royalty figures.
- Support tickets linked to a book or a general account query, with file and URL attachments.
- Live author ticket updates through Server-Sent Events (SSE), with polling as a fallback.
- Admin queue filters and priority-based ordering, plus status, category, priority, and assignment controls.
- Optional AI ticket triage and response drafts using Groq; administrators review and send drafts.
- Private internal notes that are not exposed in author-facing ticket responses.

## Technology stack

| Area | Technologies |
|---|---|
| Frontend | React 19, TypeScript, TanStack Start and Router, TanStack Query, Vite, Tailwind CSS 4, Radix UI, Lucide React |
| Backend | Java 21, Spring Boot 3.4, Spring Web, Spring Data JPA, Spring Security, JWT, Bean Validation |
| Database | MySQL |
| AI | Spring AI with Groq's OpenAI-compatible API |
| API documentation | Springdoc OpenAPI / Swagger UI |
| Frontend package manager | npm |
| Backend build | Maven |

## Architecture

The frontend is a server-rendered TanStack Start application that calls the backend REST API. The Spring Boot backend exposes REST endpoints, applies role and ownership checks, and uses a controller → service → repository structure to access MySQL through JPA. The backend also serves authenticated ticket attachments and streams ticket updates to authors over SSE.

Ticket triage runs asynchronously after ticket creation. If AI is unavailable, the backend uses rule-based classification; AI response drafts are generated on demand for administrators. The Groq API key is used only by the backend.

## Project structure

```text
BookLeaf/
├── frontend/
│   ├── src/
│   │   ├── components/       # Shared portal and UI components
│   │   ├── lib/              # API client, data types, queries, and ticket stream
│   │   ├── routes/           # Author, admin, login, and signup pages
│   │   └── test/             # Frontend tests
│   ├── .env.example
│   └── package.json
└── backend/
    ├── src/main/java/com/bookleaf/support/
    │   ├── config/
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── repository/
    │   └── service/
    ├── src/main/resources/    # application.yml, seed data, and AI knowledge base
    ├── src/test/
    ├── Dockerfile
    └── pom.xml
```

## Prerequisites

- Node.js and npm for the frontend.
- Java 21 and Maven for the backend.
- A running MySQL server and a database user with permission to create/use the application database.
- A Groq API key only if you want AI triage and response drafts enabled.

## Installation and local setup

### 1. Configure the frontend

In PowerShell from the project root:

```powershell
Set-Location frontend
Copy-Item .env.example .env.local
npm install
```

The example configures the API URL as `http://localhost:9090/api`. Change `VITE_API_BASE_URL` in `frontend/.env.local` if your backend uses a different address. This variable is public frontend configuration; do not put secrets in it.

### 2. Configure and start the backend

In a new PowerShell terminal from the project root, ensure MySQL is running. The default JDBC URL targets a local database named `bookleaf` and requests that MySQL create it if it does not exist. Set the connection credentials and a unique JWT secret in the PowerShell session, then start Spring Boot:

```powershell
Set-Location backend
$env:DB_USERNAME = "<your-mysql-user>"
$env:DB_PASSWORD = "<your-mysql-password>"
$env:JWT_SECRET = "<unique-random-secret-at-least-32-bytes>"
mvn spring-boot:run
```

The backend listens on `http://localhost:9090` by default. Configure `GROQ_API_KEY` in the same environment if AI features are needed; without it, ticket triage falls back to rules and AI drafts are unavailable. Keep database credentials, JWT secrets, and API keys out of source control.

### 3. Start the frontend

Open a second PowerShell terminal from the project root:

```powershell
Set-Location frontend
npm run dev
```

Open the local URL printed by Vite (normally `http://localhost:5173`). The backend's default CORS configuration allows this origin. If you use a different frontend origin, add it to `CORS_ALLOWED_ORIGINS` on the backend.

### Useful development commands

| Directory | Command | Description |
|---|---|---|
| `frontend` | `npm test` | Run frontend tests |
| `frontend` | `npm run build` | Build the frontend |
| `frontend` | `npm run lint` | Lint the frontend |
| `backend` | `mvn test` | Run backend tests |
| `backend` | `mvn spring-boot:run` | Start the backend |

## Environment variables

Backend variables are configured in `backend/src/main/resources/application.yml` and may be overridden through the process environment. The frontend reads `VITE_API_BASE_URL` from `frontend/.env.local`.

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | Frontend API root; defaults in `.env.example` to `http://localhost:9090/api`. |
| `PORT` | Backend HTTP port; defaults to `9090`. |
| `DB_URL` | MySQL JDBC connection URL; defaults to a local `bookleaf` database. |
| `DB_USERNAME`, `DB_PASSWORD` | MySQL connection credentials. Set these for your local or deployed database. |
| `DDL_AUTO` | Hibernate schema handling; defaults to `update`. |
| `JWT_SECRET` | Secret used to sign JWTs; must be at least 32 bytes. Use a unique, randomly generated value. |
| `JWT_EXPIRATION_MINUTES` | JWT lifetime in minutes; defaults to `480`. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins; defaults include common localhost development ports. |
| `ATTACHMENT_STORAGE_DIR` | Private directory for uploaded ticket files; defaults to `./uploads`. Use persistent storage in deployments. |
| `GROQ_API_KEY` | Optional server-side credential for Groq AI features. |
| `GROQ_MODEL` | Groq model ID; defaults to `qwen/qwen3.8-27b`. |
| `AI_ENABLED` | Enables or disables AI functionality; defaults to `true`. |
| `SEED_ENABLED` | When `true`, seed an initial admin and the bundled sample authors/books if the users table is empty; defaults to `false`. |
| `SEED_ADMIN_EMAIL` | Email for the seeded administrator. |
| `SEED_ADMIN_PASSWORD`, `SEED_AUTHOR_PASSWORD` | Passwords used during seeding; each must be at least 12 characters when seeding is enabled. |

Seeding is optional. Configure the seed passwords before enabling it; do not use sample credentials in a shared or production environment. The bundled sample dataset is `backend/src/main/resources/bookleaf_sample_data.json`.

## Database

The backend uses MySQL through Spring Data JPA and Hibernate. By default, it connects to the local `bookleaf` database and uses Hibernate's `update` schema mode. For production, configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` with deployment-specific values and use a migration-based schema strategy rather than relying on automatic schema updates. The backend does not include a separate database migration tool.

## API endpoints

All endpoints are rooted at `/api`. Endpoints marked **AUTHOR** or **ADMIN** require a JWT bearer token for the corresponding role. Swagger UI is available at `http://localhost:9090/swagger-ui.html`.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/auth/login` | Public | Authenticate and receive a JWT. |
| `POST` | `/auth/signup` | Public | Create an author account. |
| `POST` | `/auth/admin-signup` | Public, invitation required | Create an administrator account from a valid invitation. |
| `GET` | `/auth/me` | Authenticated | Get the current user. |
| `POST` | `/admin/invites` | ADMIN | Create a one-time, email-bound admin invitation. |
| `GET` | `/author/books` | AUTHOR | List the author's books and royalty figures. |
| `POST` | `/author/tickets` | AUTHOR | Create a ticket, optionally with uploaded files. Accepts JSON or multipart form data. |
| `GET` | `/author/tickets` | AUTHOR | List the author's tickets; optional `since` returns tickets changed after that time. |
| `GET` | `/author/tickets/stream` | AUTHOR | Receive ticket update events over SSE. |
| `GET` | `/author/tickets/{id}` | AUTHOR | Read a ticket and its conversation. |
| `POST` | `/author/tickets/{id}/replies` | AUTHOR | Add a message to the author's ticket. |
| `GET` | `/author/tickets/{ticketId}/attachments/{attachmentId}` | AUTHOR | Download an attachment from the author's ticket. |
| `GET` | `/admin/tickets` | ADMIN | Search and page through tickets; supports `status`, `category`, `priority`, `assignedAdminId`, `from`, `to`, `q`, `page`, and `size` filters. |
| `GET` | `/admin/tickets/agents` | ADMIN | List administrators available for assignment. |
| `GET` | `/admin/tickets/{id}` | ADMIN | Read ticket details, conversation, notes, and associated book data. |
| `PATCH` | `/admin/tickets/{id}/status`, `/category`, `/priority` | ADMIN | Update status or manually override classification and priority. |
| `POST` | `/admin/tickets/{id}/assign`; `PATCH /admin/tickets/{id}/assignee` | ADMIN | Assign a ticket to yourself or assign/unassign an administrator. |
| `POST` | `/admin/tickets/{id}/notes`, `/responses` | ADMIN | Add a private internal note or send a response to the author. |
| `POST` | `/admin/tickets/{id}/ai-draft` | ADMIN | Generate or retrieve a response draft; `regenerate=true` requests a new draft. |
| `POST` | `/admin/tickets/{id}/retriage` | ADMIN | Re-run ticket triage. |
| `GET` | `/admin/tickets/{id}/attachments/{attachmentId}` | ADMIN | Download a ticket attachment. |

Ticket uploads support up to five files, 10 MB each. Uploaded files are stored outside the web root; downloads require an authorized ticket owner or administrator. Admin invitations are valid for 48 hours. Email delivery is not configured, so invitation links must be shared securely.

## How the application works

1. Authors sign up through the public signup page or sign in with an existing account. Public signup always creates an author account.
2. An administrator account must first be provisioned through backend seeding or another trusted process. Administrators can then create email-bound invitations for additional admins.
3. Authors view their own book and royalty records, create tickets, and add replies. Their ticket conversations update over SSE, with a polling fallback.
4. New tickets receive asynchronous triage. Admins can filter and prioritize the queue, update ticket fields and assignment, review or regenerate AI response drafts, and send replies.
5. Internal notes are accessible only to administrators. Attachments are stored on the backend filesystem and must use persistent storage if the backend is deployed.
