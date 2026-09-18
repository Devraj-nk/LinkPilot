# LinkPilot — Self-Hosted URL Shortener with Multi-Tenant Auth, Custom Domains & Real-Time Click Analytics

A full-stack URL shortener where every resource is scoped to its owner at the service
layer, custom domains actually constrain where a link redirects, and clicks flow
through an async event pipeline into a real analytics store — designed to run
entirely self-hosted with `docker compose up`, no managed database or external
service required.

## High-Level Design

```
 Browser
   |
   |  /api/* calls: cookies only, no tokens in client JS
   |  short links (e.g. /abc123): the REAL Host header lands here first -
   |  this is also the real entry point for a custom domain
   v
 Next.js frontend
   - API route proxies: attach Bearer JWT                -----------+
   - [shortCode] route: 307s to the backend, forwarding             |
     the real Host as a ?host=<host> query param                    |
   |                                                                 |
   | Authorization: Bearer <JWT>                                    | GET /r/{shortCode}?host=<host>
   v                                                                 v
 +---------------------------------------------------------------------+
 |                        Spring Boot backend                            |
 |                                                                        |
 |   REST controllers                RedirectController                   |
 |   (auth, links, campaigns,        GET /r/{shortCode}                    |
 |    domains, api-keys)                     |                              |
 |            \                              /                              |
 |             \                            /                               |
 |              v                          v                                |
 |       Services (ownership checks, status/expiry rules,                    |
 |       domain/host match, short-code generation)                            |
 |              |              |                  |                           |
 +--------------|--------------|------------------|---------------------------+
                |              |                  |
                v              v                  v
            SQLite           Redis          ClickEventService
       users, links,     shortCode ->        (async, fires after
       campaigns,        originalUrl          the redirect response;
       domains, tokens    cache               every failure is
                                               swallowed internally)
                                                       |
                                                       v
                                                  ClickHouse
                                          click_events + daily/device/
                                          referrer aggregates. Optional
                                          infra - the product works
                                          fine even if it's down.
```

**Component responsibilities**

| Component | Responsibility |
|---|---|
| Next.js frontend | Renders the dashboard and proxies every `/api/*` call server-side (so the browser only ever holds httpOnly session cookies, never a raw JWT); also the actual entry point for short links themselves - `/{shortCode}` is a frontend route that forwards to the backend |
| Spring Boot backend | REST API, JWT/refresh-token auth, and per-user ownership checks enforced in the service layer (not just hidden in the UI) |
| SQLite | Primary transactional store - single-writer (WAL mode, capped connection pool), chosen specifically so the whole stack self-hosts as one file, not a managed DB service |
| Redis | Read-through cache in front of the hot short-code lookup |
| ClickHouse | Append-only analytics store for `click_events`, aggregated via materialized views; optional infrastructure - if it's unreachable, redirects and the rest of the product are unaffected, only the analytics dashboard degrades |

**Key request flows**

- **Auth** - `/login`/`/register` hit `POST /api/auth/*`, which returns a short-lived
  JWT access token plus an opaque, rotating, SHA-256-hashed-at-rest refresh token; the
  Next.js route writes both as httpOnly cookies. Every later `/api/*` call goes
  through a proxy route that attaches the access token as a Bearer header and, on a
  single 401, silently calls `/api/auth/refresh` and retries once before giving up.
- **Create a link** - `LinkService` validates that any given campaign/domain belongs
  to the caller and that a domain is `VERIFIED` before it can be used, generates or
  validates the short code, persists the link, and warms the Redis cache.
- **Resolve a redirect** - a short link is a root-level path on the *frontend*
  (`go.example.com/abc123`), not a direct hit on the backend. `[shortCode]/route.ts`
  is where the real `Host` header is actually seen (including for a custom domain),
  and it 307s the browser to the backend, forwarding that host as a `?host=` query
  param so it survives the hop - the browser's follow-up request to the backend
  otherwise carries the backend's own address as its `Host` header, not the domain
  the visitor typed. The backend then does: domain/host match -> status check ->
  expiry check -> click-count increment (same DB transaction) -> `302` straight back
  to the browser, which finally lands on the real destination. Recording the detailed
  click event into ClickHouse happens *after* the redirect is resolved, on a separate
  thread pool, with every failure caught internally - a slow or unreachable analytics
  store can never delay or break a redirect.
- **Analytics** - `GET /api/links/{id}/analytics` is ownership-checked, then queries
  ClickHouse directly for a daily click series, device breakdown, and top referrers;
  if ClickHouse can't be reached it returns `analyticsAvailable: false` with the fast
  SQLite click counter still intact, instead of failing the request.

## Tech Stack

- **Frontend**: Next.js + TypeScript
- **Backend**: Spring Boot + Java
- **Primary Database**: SQLite (embedded)
- **Cache / Temporary State**: Redis
- **Analytics**: ClickHouse (click event ingestion + aggregation)

## Run everything with Docker

```bash
docker compose up --build
```

This starts Redis, ClickHouse, the backend (`:8080`), and the frontend (`:3000`) together -
no local Java/Node/Maven install needed. The SQLite file and ClickHouse data persist in
named volumes across restarts. To set a real `JWT_SECRET` (recommended for anything beyond
a local demo), put it in a `.env` file next to `docker-compose.yml`:

```
JWT_SECRET=<a base64-encoded 256-bit value>
```

Without one, it falls back to the same dev-only default baked into
`backend/src/main/resources/application.properties`.

## Project Structure

```
LinkPilot/
├── frontend/                 # Next.js frontend application
│   ├── app/                  # App router pages
│   ├── lib/                  # Utility functions
│   ├── public/               # Static assets
│   ├── styles/               # CSS styles
│   ├── package.json          # Frontend dependencies
│   └── next.config.js        # Next.js configuration
├── backend/                  # Spring Boot backend application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/         # Java source code
│   │   │   │   └── com/example/linkpilot/
│   │   │   │       ├── controller/  # REST controllers
│   │   │   │       ├── model/       # JPA entities
│   │   │   │       ├── repository/  # Data access layer
│   │   │   │       └── service/     # Business logic
│   │   │   └── resources/     # Configuration files
│   │   │       └── application.properties
│   └── pom.xml               # Maven dependencies
├── requirements.txt          # Documentation of tech stack and dependencies
�└── README.md                 # This file
```

## Setup Instructions

### Prerequisites

- Node.js 18+ and npm
- Java 17+ and Maven
- Redis server

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. The database is an embedded SQLite file (no server to install or configure) — it's created automatically at `backend/data/linkpilot.db` on first run. Configure Redis in `src/main/resources/application.properties` if it's not running on the defaults:
   ```properties
   # Database
   spring.datasource.url=jdbc:sqlite:data/linkpilot.db?journal_mode=WAL&busy_timeout=5000&foreign_keys=on
   spring.datasource.driver-class-name=org.sqlite.JDBC
   spring.datasource.hikari.maximum-pool-size=1
   spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
   spring.jpa.hibernate.ddl-auto=update

   # Redis
   spring.data.redis.host=localhost
   spring.data.redis.port=6379
   ```

3. Build and run the backend:
   ```bash
   mvn spring-boot:run
   ```
   The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. (Optional) Create `frontend/.env.local` if the backend isn't on the defaults below:
   ```
   # Server-side: used by the Next.js API routes that proxy to the backend
   BACKEND_URL=http://localhost:8080
   # Client-side: used to load QR code images directly from the backend
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
   ```

4. Run the development server:
   ```bash
   npm run dev
   ```
   The frontend will be available at `http://localhost:3000`

## Authentication

Registering or logging in (`/register`, `/login`) sets `accessToken` and `refreshToken`
as `httpOnly` cookies via the Next.js API routes - the browser never holds the raw
tokens. Every other `/api/*` route under `frontend/app/api` proxies to the Spring
backend, attaching the access token and silently refreshing it once on a 401. Routes
under `/dashboard` are gated by `frontend/proxy.ts`.

## API Endpoints (Backend, Spring Boot)

All routes below are under `http://localhost:8080` and, except where noted, require
`Authorization: Bearer <accessToken>`.

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout` - public
- `GET /api/users/me` - current user profile
- `GET/POST /api/campaigns`, `GET/PUT/DELETE /api/campaigns/{id}`
- `GET/POST /api/links`, `GET/PUT/DELETE /api/links/{id}`, `PATCH /api/links/{id}/status`
- `GET /api/links/{id}/analytics` - clicks over time, device breakdown, top referrers
- `GET /r/{shortCode}` - public redirect (302 to the original URL, tracks the click)
- `GET/POST /api/links/{linkId}/qrcodes`, `DELETE /api/qrcodes/{id}`
- `GET /api/qrcodes/{id}/image` - public, returns a PNG
- `GET/POST /api/domains`, `DELETE /api/domains/{id}`, `POST /api/domains/{id}/verify`
- `GET/POST /api/api-keys`, `DELETE /api/api-keys/{id}`

The four `GET` list endpoints (`/api/links`, `/api/campaigns`, `/api/domains`,
`/api/api-keys`) are paginated: `?page=0&size=20` (0-indexed, size capped at 100,
sorted newest-first). Each returns `{ content, page, size, totalElements,
totalPages, hasNext }` rather than a bare array.

The frontend never calls these directly from the browser - it goes through the
matching proxy routes under `frontend/app/api/**` (see [Authentication](#authentication)).

`POST /api/auth/login` (10/min), `POST /api/auth/register` (5/hour), `POST /api/links`
(30/min per user), and `GET /r/{shortCode}` (60/min per IP) are rate-limited via a
Redis-backed fixed-window counter; exceeding the limit returns `429 Too Many Requests`.

## Features Implemented

1. Email/password auth (JWT access tokens + rotating refresh tokens)
2. Link shortening with custom or generated short codes, campaigns, expiration, status
   (active/disabled), and click tracking
3. Redirects served at `/r/{shortCode}` via the backend, with Redis caching
4. QR code generation (PNG) for any link, with configurable size/colors
5. Campaign management; custom domains that actually constrain redirects (a link bound
   to a verified domain only resolves on that domain's `Host` header)
6. Real click analytics - an async event pipeline into ClickHouse (device/browser/OS,
   referrer, daily-salted unique-visitor hashing) feeding a per-link dashboard, with
   graceful degradation if ClickHouse isn't reachable
7. API key management (for future programmatic access - see Future Enhancements)
8. Docker packaging (`docker compose up --build` - see above)
9. Backend test suite (auth flow, `LinkService` ownership/expiry/status logic)
10. Rate limiting on auth, link creation, and redirects (Redis-backed, fails open if
    Redis is unreachable)

## Future Enhancements

- Email verification (the `is_verified` flag exists but nothing sets it)
- QR code SVG/PDF rendering (PNG is the only format actually implemented)
- Real DNS-based domain verification (currently a stub that just flips the status)
- Using API keys to authenticate API requests (currently only CRUD-managed, not a working auth path)
- Admin-only endpoints (the `ADMIN` role exists but nothing checks it)
- GeoIP enrichment for analytics (`country`/`region`/`city` are unpopulated placeholders)