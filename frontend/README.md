# BookLeaf Frontend

The BookLeaf frontend is a React and TypeScript author support portal built with TanStack Start, TanStack Router, Vite, and Tailwind CSS. It provides author and administrator views and communicates with the Spring Boot backend over its REST API.

## Features

- Author registration and sign-in, book and royalty details, support tickets, and ticket replies.
- Live author ticket updates using Server-Sent Events, with polling as a fallback.
- Administrator ticket queue, ticket triage and assignment, author responses, AI draft support, and private notes.
- Responsive shared interface components and role-protected routes.

## Requirements

- Node.js and npm.
- The BookLeaf backend running locally or at a configured API URL.

## Local development

From this directory:

```sh
npm install
```

Copy `.env.example` to `.env.local`. Set `VITE_API_BASE_URL` to the backend API root; the example value is `http://localhost:9090/api`.

```sh
npm run dev
```

Open the local address printed by Vite (typically `http://localhost:5173`). The backend's `CORS_ALLOWED_ORIGINS` must include the frontend origin.

## Scripts

| Command | Description |
|---|---|
| `npm run dev` | Start the Vite development server. |
| `npm run build` | Build the application. |
| `npm run preview` | Preview the production build locally. |
| `npm test` | Run the Vitest test suite. |
| `npm run lint` | Run ESLint. |

## Routes

| Route | Purpose |
|---|---|
| `/login`, `/signup` | Sign-in and account creation. Public signup creates author accounts; admin signup requires an invitation. |
| `/author/dashboard`, `/author/books` | Author dashboard and book/royalty information. |
| `/author/tickets`, `/author/tickets/new`, `/author/tickets/:id` | Author ticket list, submission, and conversation. |
| `/admin/dashboard`, `/admin/tickets`, `/admin/tickets/:id` | Admin overview, ticket queue, and ticket management. |

For backend setup, API details, and database configuration, see [`../backend/README.md`](../backend/README.md). For monorepo setup and architecture, see [`../README.md`](../README.md).

## Deployment

The Vite configuration uses Nitro's Vercel preset. To deploy the frontend to Vercel, set the project root directory to `frontend`, use `npm run build`, and configure `VITE_API_BASE_URL` with the deployed backend API root (for example, `https://<backend-domain>/api`). Deploy the backend separately and configure its CORS allowlist to include the frontend's deployed origin.
