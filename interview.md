# LinkPilot — Interview Prep

## 1. Why this project, and what's different from a "legacy" URL shortener?

**Why build a URL shortener at all** — it looks simple on the surface (one table,
one redirect) but it's actually a good vehicle for demonstrating full-stack
judgment: auth, ownership modeling, a real analytics pipeline, and infra decisions,
without the problem domain itself getting in the way. It's small enough to reason
about completely, but has enough moving parts (auth, cache, two databases, async
processing) to talk about real trade-offs.

**What's not just "another bit.ly clone":**
- **Multi-tenant ownership from the ground up.** Every resource (link, campaign,
  domain, API key) is scoped to the authenticated user at the service layer, not
  just hidden in the UI — `LinkService.getForUser(userId, linkId)` 404s (not 403s)
  for a link you don't own, so the API doesn't even confirm the resource exists to
  someone who isn't its owner.
- **Custom domains that actually constrain routing**, not just a decorative list.
  A link bound to a verified domain only resolves when the request's `Host` header
  matches — most portfolio-scale shorteners stop at "store the domain string."
- **A real analytics pipeline**, not a single click counter. Redirects fire an
  async event into ClickHouse (a column-store built for exactly this — high-volume
  event ingestion + fast aggregation), separate from the transactional SQLite store.
  That's the actual architecture production shorteners use (hot path vs. analytics
  store), just at a scale you can run on a laptop.
- **Self-hosted-first.** It deliberately runs on SQLite instead of requiring a
  Postgres server, specifically so the whole thing — app + cache + analytics store —
  can be handed to someone as `docker compose up` with zero external accounts or
  managed services.

## 2. Tech stack, and why

| Layer | Choice | Why this, not the obvious alternative |
|---|---|---|
| Backend | Spring Boot 3 / Java 17 | Mature, typed, and the ecosystem (Spring Security, Spring Data JPA) gives real auth and persistence machinery instead of hand-rolling it — worth demonstrating fluency in, since it's what most backend roles actually use. |
| Primary DB | SQLite (migrated from Postgres) | The project's actual constraint was "must be trivially self-hostable" — SQLite is a file, not a service. Postgres is the better choice at real scale (proper concurrent writes, richer types), and I can say exactly why: SQLite serializes writes (one at a time), which is why the datasource pool is capped at 1 connection and WAL mode + `busy_timeout` are turned on — that's a deliberate concurrency trade-off, not an oversight. |
| Auth | JWT access tokens + rotating opaque refresh tokens | Stateless access tokens mean any backend instance can verify a request with no shared session store. Refresh tokens are *not* JWTs — they're random tokens, SHA-256-hashed before being stored, so a leaked database dump doesn't hand out usable credentials. Rotation (old token revoked, new one issued on every refresh) limits how long a stolen refresh token is useful. |
| Token storage (frontend) | httpOnly cookies, not localStorage | localStorage is readable by any JS on the page — one XSS bug anywhere leaks every session. httpOnly cookies are invisible to JS entirely; the Next.js server proxies every API call and attaches the token server-side. |
| Analytics store | ClickHouse | Purpose-built for exactly this shape of workload: enormous write volume of small immutable events, aggregated by time/dimension. A relational OLTP table would choke on the same query patterns (`GROUP BY day, device` over millions of rows) that ClickHouse is architected for. |
| Cache | Redis | Standard read-through/write-through cache in front of the hot lookup path; also the natural place for future rate-limiting. |
| Frontend | Next.js (App Router) | Server-side route handlers let the browser talk only to the same origin — no CORS surface, and secrets/tokens never reach client JS. |
| QR generation | ZXing | Well-established, no external service call needed — QR codes are generated in-process, which matters because they need to happen synchronously in an HTTP response. |

## 3. STAR-format summary (for "tell me about a project you built")

**Situation:** Most portfolio URL-shortener projects stop at "form submits, row
saves, redirect works." I wanted one that held up to the questions an interviewer
would actually ask about a real system: how do you handle auth securely, what
happens at scale, what's your analytics story, what did you cut and why.

**Task:** Build a link shortener that's genuinely self-hostable (no managed DB
required), has real multi-tenant auth, and has an analytics pipeline that's
architecturally correct, not just a counter column — while being explicit about
what's in scope for a solo/portfolio build versus what a production team would add.

**Action:** Designed the data model around per-user ownership first, then layered
JWT + rotating-refresh auth with httpOnly cookies; migrated the datastore from
Postgres to SQLite mid-project when the actual requirement (zero-dependency local
hosting) became clear, which meant re-deriving the whole schema (SQLite has no
native `ENUM`, so status fields became `TEXT` + `CHECK` constraints) and picking a
Hibernate dialect module that isn't in core anymore. Built click tracking as an
async, fire-and-forget path into ClickHouse specifically so a broken analytics
write could never break a redirect — the one guarantee the product can't lose.

**Result:** A working system I can run end-to-end locally or in Docker — register,
create a link, hit it, see a real 302, generate a scannable QR code, see the click
show up in an aggregated analytics view — plus a documented, honest list of what's
still a stub (domain DNS verification) versus what's fully real (everything else).

## 4. Flow of the project + challenges and how they were solved

**Build order (and why that order):** data model → auth → core CRUD → the thing
that makes it a *product* (redirects, QR, analytics) → hardening (tests, Docker).
Auth had to come before "real" CRUD because ownership is load-bearing everywhere
else — every other service method takes a `userId` and filters by it.

**Challenge 1 — inherited a codebase that didn't compile.**
Before any feature work, `LinkService` referenced a `clickCount` field that didn't
exist on `Link`, and `LinkRepository` was typed `<Link, Long>` while `Link`'s actual
ID was a `UUID`. *Approach:* rather than patch around it, traced every call site
back to what the code was actually trying to do, fixed the entity/repository
mismatch at the source, and only then moved forward — patching symptoms here would
have just relocated the bug.

**Challenge 2 — Postgres → SQLite wasn't just a connection string swap.**
Postgres-specific features silently don't exist in SQLite: `CREATE TYPE ... AS
ENUM`, `uuid-ossp`, timezone-aware timestamps, and PL/pgSQL trigger functions.
*Approach:* rewrote the schema natively for SQLite (`TEXT` + `CHECK` for enums,
per-table `AFTER UPDATE` triggers instead of a shared function) rather than trying
to emulate Postgres semantics on top of it — and in the process found that three of
the original triggers referenced an `updated_at` column that didn't even exist on
those tables, a latent bug in the original schema that had just never been
executed.

**Challenge 3 — Hibernate 6 doesn't ship a SQLite dialect in core anymore.**
*Approach:* found it lives in a separate `hibernate-community-dialects` module,
verified the exact version Spring Boot's dependency management would resolve
before committing to it in `pom.xml` (rather than guessing and debugging a runtime
`ClassNotFoundException` later).

**Challenge 4 — SQLite only allows one writer at a time.**
A default connection pool (10 concurrent connections) would produce intermittent
`SQLITE_BUSY` errors under any concurrent write load. *Approach:* capped the pool
at 1 connection and enabled WAL journal mode + a busy timeout — a deliberate
concurrency ceiling that's fine at this scale and is the correct, documented
trade-off for choosing SQLite in the first place, not a bug to "fix" later.

**Challenge 5 — writing the test suite surfaced a real, silent correctness bug.**
Adding integration tests exposed that `UNIQUE` constraints (on user email, short codes,
API key hashes, etc.) were never actually being enforced at the database level in the
running app — Hibernate's auto-DDL tries to add them via `ALTER TABLE ... ADD
CONSTRAINT`, which SQLite doesn't support, so it silently logs a warning and moves on.
*First instinct:* make the hand-written `schema.sql` (which declares these constraints
correctly) authoritative instead of Hibernate's auto-DDL. That approach broke
authentication entirely — it turned out Hibernate's SQLite dialect stores UUID primary
keys as raw BLOBs and timestamps as epoch-millisecond text under the hood, not as the
plain ISO-8601 `TEXT` a hand-written schema would assume, so a differently-authored
table wasn't actually read/write compatible with what the JPA entities produce.
*Actual fix:* keep Hibernate's auto-DDL as the source of truth for table structure
(it's internally consistent, which matters more than matching a hand-written schema),
and add a small startup step that runs `CREATE UNIQUE INDEX IF NOT EXISTS` statements
afterward — a unique index enforces exactly the same constraint SQLite can't express via
`ALTER TABLE`, without needing to touch or second-guess how Hibernate encodes columns.
This is a good story on its own: the fix that seemed obviously "more correct" first
(hand-authored schema) was actually wrong once tested against the real serialization
behavior, and the test suite is what caught it before it shipped.

**Challenge 6 — analytics must never be allowed to break the product.**
The redirect endpoint is the one path that has to work every time. *Approach:*
click-event recording runs on a dedicated async executor, fully decoupled from the
request thread, with every failure path caught and logged inside the recording
method itself — so if ClickHouse is down, unreachable, or slow, redirects are
completely unaffected. This is also why ClickHouse's own schema is initialized
defensively at startup (logs a warning and continues rather than failing app boot
if it can't connect) — it's optional infrastructure for a bonus feature, not a hard
dependency for the core product.

**Challenge 7 — rate limiting isn't one mechanism, it's a per-endpoint keying decision.**
"Add a rate limiter" doesn't actually specify anything useful on its own - the real
design question is what key each limit uses and why, and that's different per
endpoint. *Approach:* `/api/auth/login` and `/register` have no authenticated caller
yet, so they're keyed by IP - that's the axis credential-stuffing and registration
spam actually happen on. `POST /api/links` is keyed by user ID instead, since the
caller is already authenticated and the thing being guarded against is one account
scripting mass link creation, not whatever network it happens to route through -
user ID is also more precise than IP here, since it doesn't false-positive on a
shared office/campus IP. The public redirect (`GET /r/{shortCode}`) is keyed by IP
again, but for a different reason: a link going viral is expected to pull traffic
from thousands of different IPs, so capping *the link* would be wrong - the actual
concern is one IP hammering the redirect endpoint itself as a cheap way to point
traffic at an arbitrary destination URL through the service. Implementation is a
small Redis-backed fixed-window counter (`INCR` a per-window key, `EXPIRE` it on
first use) rather than a rate-limiting library - three call sites don't justify a
new dependency, and a fixed-window counter is simple enough to fully explain rather
than just cite. It deliberately fails open (allows the request) on any Redis error,
same philosophy as ClickHouse's graceful degradation in Challenge 6 - infrastructure
that's there to protect the product should never become the thing that takes it
down.

## 5. Results (generic, not fabricated metrics)

- Full auth-to-redirect loop verified end-to-end with real HTTP requests (not just
  "it compiles") — register → login → create link → click → 302 → click count
  increments — both hitting the backend directly and through the full Next.js proxy
  layer with real cookies.
- Reduced the API's attack surface for credential handling to zero client-side
  token exposure (httpOnly cookies only) and zero raw refresh tokens ever stored
  (SHA-256 hashed at rest).
- Caught and fixed a codebase that didn't compile, a schema bug that would have
  thrown at first write (undefined trigger columns), and an exposed database
  password sitting in git history — before writing a single new feature.
- Writing the test suite caught a silent, real production bug: uniqueness
  constraints (email, short codes, API keys) weren't actually enforced at the
  database level at all, because of a SQLite/Hibernate DDL limitation nothing had
  surfaced before. Fixed with a targeted unique-index step rather than a riskier
  full schema rewrite, after the first (more invasive) fix attempt itself broke
  authentication and was caught before being kept.
- Every documented "not implemented" gap (SVG/PDF QR rendering, DNS domain
  verification, email verification, admin-role enforcement) is a conscious,
  written-down scope cut with a one-line reason, not an unnoticed hole — which is
  itself something to point to as a practice, not just a list of missing features.
- Closed a real scale gap after naming it out loud first: every list endpoint
  (links, campaigns, domains, API keys) returned its full result set with no limit.
  Fine at demo scale, a real outage waiting to happen with real usage - fixed with
  proper offset-based pagination (`Page<T>`, clamped page/size params, a consistent
  `PageResponse` envelope) rather than leaving it as a known-but-ignored issue, and
  verified live against 25 seeded links that it actually splits into pages correctly.
- Added rate limiting to the three endpoints that actually needed it - login/register
  by IP, link creation by user, the public redirect by IP - rather than one blanket
  global limiter, and verified each live against a running Redis: exactly the
  configured limit passes, the next request is rejected with 429, and a different
  user or IP is completely unaffected by another one being throttled.

## 6. Questions an interviewer is likely to ask

- **"Why SQLite instead of Postgres for a real product?"** — I wouldn't for a real
  product at scale; the actual constraint here was zero-dependency self-hosting.
  Be ready to name exactly what you'd lose (real concurrent writes, richer types,
  replication) and what you'd need to change to move back (swap the dialect/driver,
  the schema is already close to portable since it avoids Postgres-only features
  now).
- **"Walk me through what happens when someone clicks a short link."** — Use the
  write-path/read-path breakdown: lookup by shortCode → domain/host check → status
  + expiry check → click count increment (sync, in the same DB transaction) → async
  fire-and-forget event into ClickHouse → 302 response. Know why click-count
  increment is synchronous (it's cheap, same DB, same transaction) but the detailed
  analytics event is async (device/browser parsing + a second datastore write
  shouldn't be on the critical path).
- **"How does the analytics know anything about the click — is it tracking the
  destination page too?"** — No, and that's worth stating precisely: everything
  recorded (device, browser, OS, referrer, approximate uniqueness) comes from HTTP
  headers on the *one* request the browser makes to `/r/{shortCode}` — `User-Agent`,
  `Referer`, and the client IP off the connection itself. The backend responds with a
  302 and is never involved in the second request the browser then makes straight to
  the destination URL — that's a direct browser-to-target request LinkPilot doesn't
  see at all. So this is click-through analytics (did someone hit the short link),
  not conversion/destination analytics (what they did after). Getting the latter
  would need either a tracking pixel on the destination page or an interstitial
  redirect page — a meaningfully more invasive design I deliberately didn't build.
  Also worth naming: recording is async and fire-and-forget specifically so a slow or
  down ClickHouse can never delay or break the redirect itself.
- **"How do you know a user can't access someone else's link?"** — every
  `LinkService` method that isn't the public resolve path takes the caller's
  `userId` and queries with it baked into the lookup (`findByIdAndUserId`) — a
  non-owner gets a 404, not data, and not even a 403 that would confirm the
  resource exists.
- **"What would you do differently at 10x/100x the traffic?"** — Redis is already
  populated as a write-through cache but the redirect path doesn't read from it yet
  (a known, documented gap) — that's the first lever. After that: move off SQLite,
  shard/partition ClickHouse by time (the schema already does monthly partitions),
  and move click-event recording off in-process async to a real queue (Kafka/SQS)
  so a traffic spike can't exhaust the app's own thread pool.
- **"What's the biggest risk in your auth design?"** — the JWT secret is
  environment-overridable but ships with a dev-only default in the repo — fine for
  local dev, and called out explicitly as something that must be rotated/overridden
  before any real deployment, but worth naming unprompted rather than waiting to be
  asked.
- **"Why 302 and not 301 for the redirect?"** — a 301 tells browsers to cache the
  redirect permanently and skip the server on repeat visits, which would silently
  break click tracking for anyone who's visited before; 302 keeps every click
  flowing back through the backend.
- **"What's the one thing that looks done but isn't, if I dig in?"** — be upfront:
  domain verification is a stub (flips a status flag, no real DNS TXT check), and
  say so before they find it — that's a stronger answer than getting caught by it.
- **"Why does list pagination matter, and how did you implement it?"** — every list
  endpoint (`/api/links`, `/api/campaigns`, `/api/domains`, `/api/api-keys`) used to
  return every row a user owned in one response. Harmless with a handful of test
  links, but it doesn't show up until real usage does: a large response the client
  has to fully receive and parse before rendering anything, a query that loads every
  matching row into memory on every list call regardless of what's actually shown,
  and a frontend table rendering however many thousand DOM rows with no
  virtualization — exactly the kind of thing that's invisible in your own testing
  and only breaks once someone's real data grows. *Implementation:* offset-based
  pagination via Spring Data's `Page<T>`/`Pageable` rather than a hand-rolled
  `LIMIT/OFFSET` - the mechanical, well-supported option, not the interesting one to
  build from scratch. A small `PageRequestFactory` clamps client-supplied `page`/
  `size` server-side (negative page → 0, size capped at 100) so the endpoint can't be
  made to return an unbounded result set no matter what a client sends. Every list
  endpoint returns the same `{content, page, size, totalElements, totalPages,
  hasNext}` envelope, and the frontend list pages got real Previous/Next controls
  instead of rendering a raw array. Also worth naming why it wasn't in scope
  earlier: it's real, but it's a "will break down with real data" gap, not a
  "the feature doesn't work at all" gap like custom domains were - worth fixing, but
  correctly triaged as lower priority than the things that were actually broken.
- **"How would you rate-limit this API - by IP or by user?"** — Depends on the
  endpoint, and that's the actual point of the question: for `login`/`register`
  there's no authenticated caller yet, so IP is the only axis available and the
  right one - that's what credential-stuffing and registration spam look like. For
  an authenticated endpoint like link creation, user ID is more precise than IP -
  it survives shared-IP false positives (an office network shouldn't get throttled
  because one account behind it is scripting) and targets the account actually doing
  it. For the public redirect, IP is right again but for a different reason: the
  resource being protected is the endpoint itself, not any one link - legitimate
  traffic to a popular link can validly come from thousands of different IPs, so
  capping the link would be wrong. Mechanically: a Redis `INCR`+`EXPIRE` fixed-window
  counter, failing open if Redis is down - the same "optional infrastructure never
  blocks the core path" pattern as ClickHouse.
- **"Tell me about a bug you found and how you fixed it."** — the SQLite unique-
  constraint gap (Challenge 5 above) is the strongest answer available: it's a real,
  silent correctness bug the test suite caught, the first fix attempt was wrong and
  you caught that too before it shipped, and the final fix is small, targeted, and
  you can explain exactly why it works instead of just that it does.
