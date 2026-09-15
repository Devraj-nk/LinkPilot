# Link Shortening — How It Actually Works

This document explains the core backend mechanism behind LinkPilot: turning a long
URL into a short code, and turning that short code back into a redirect. It's
grounded in the real implementation (`ShortCodeGenerator`, `LinkService`,
`RedirectController`), not just the general theory.

## 1. The problem in one sentence

A URL shortener needs a bijective-ish mapping: given a long `originalUrl`, produce
a short, unique `shortCode`; given that `shortCode`, look up the original URL fast
enough that a redirect feels instant. Everything below is one of two paths built
around that mapping: the **write path** (creating a link) and the **read path**
(resolving/redirecting).

```
Write path:  originalUrl ──► generate/validate shortCode ──► persist ──► return short link
Read path:   shortCode    ──► look up ──► validate (active/not expired) ──► 302 redirect
```

## 2. Data model

[Link.java](backend/src/main/java/com/example/linkpilot/model/Link.java) is the
whole mapping table:

| Column | Purpose |
|---|---|
| `id` (UUID) | internal primary key |
| `short_code` | the public-facing token, `UNIQUE`, max length 10 |
| `original_url` | the destination |
| `status` | `ACTIVE`, `DISABLED`, `EXPIRED` — a link can be "shortened" but not resolvable |
| `click_count` | incremented on every successful resolve |
| `expires_at` | optional TTL for the link itself (not to be confused with cache TTL) |

The `unique` constraint on `short_code` at the database level is what actually
guarantees no two links collide — everything upstream of it (see §3) is just
trying to avoid hitting that constraint.

## 3. Write path: generating the short code

[ShortCodeGenerator.java](backend/src/main/java/com/example/linkpilot/service/ShortCodeGenerator.java)

Two ways a link gets a code, decided in
[LinkService.create()](backend/src/main/java/com/example/linkpilot/service/LinkService.java#L57-L79):

**a) Custom code.** If the caller supplies `customCode`, it's used as-is after a
single `existsByShortCode` check. If it's taken, the request fails with
`DuplicateResourceException` (409) rather than silently falling back to a
generated one — the caller asked for that exact code, so LinkPilot doesn't
substitute a different one behind their back.

**b) Generated code.** Otherwise, `ShortCodeGenerator.generate()` produces a
random 7-character string drawn from a 62-character alphabet
(`0-9A-Za-z`) using `SecureRandom`:

```java
private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
private static final int LENGTH = 7;
```

This is **not** an incrementing counter encoded in base62 (the classic "encode
the auto-increment row ID" approach used by services like the original bit.ly).
It's pure random sampling from a 62^7 ≈ 3.5 trillion-slot keyspace, checked
against the database for collisions:

```java
for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    String candidate = randomCode();
    if (!linkRepository.existsByShortCode(candidate)) {
        return candidate;
    }
}
throw new IllegalStateException("Could not generate a unique short code");
```

**Why random instead of sequential IDs?** Two practical reasons:
- Sequential/base62-encoded IDs are *guessable and enumerable* — a client can walk
  `/r/1`, `/r/2`, `/r/3`... and scrape every link in the system. Random codes make
  that infeasible.
  - It decouples the short code from any internal row ID, so nothing about the
  database's insertion order or size leaks through the public URL.

**Why the retry loop instead of a stronger guarantee?** At 7 characters and 62
symbols, the collision probability for any single random draw is astronomically
small (this is a birthday-paradox problem — with millions of existing links, the
odds of hitting an existing code are still ~1 in millions). A check-then-retry
loop is simpler than a reservation scheme and is good enough at this scale;
`MAX_ATTEMPTS = 10` exists purely as a circuit breaker against something being
structurally wrong (e.g. the keyspace being nearly exhausted), not because
collisions are expected in practice.

One real gap worth knowing: the existence check
(`existsByShortCode`) and the eventual `save()` aren't atomic with each other —
under concurrent requests there's a narrow TOCTOU window where two requests could
both pass the check for the same code before either commits. The `UNIQUE`
constraint on `short_code` is the actual backstop here: if that race is ever hit,
the second `save()` fails at the database level rather than silently overwriting
the first link.

## 4. Persisting the link

Once a `shortCode` is settled, `LinkService.create()` builds a `Link` entity,
attaches the owning `User` and optional `Campaign`, and saves it via
`linkRepository.save(link)` (JPA/Hibernate → SQLite). This is the single source
of truth for the mapping.

## 5. Read path: resolving and redirecting

[RedirectController.java](backend/src/main/java/com/example/linkpilot/controller/RedirectController.java)
exposes the public, unauthenticated endpoint:

```
GET /r/{shortCode}  →  302 Found, Location: <originalUrl>
```

The handler is intentionally thin — it just delegates to
[LinkService.resolve()](backend/src/main/java/com/example/linkpilot/service/LinkService.java#L102-L115), which does three things in order:

1. **Look up** the link by `shortCode` (`findByShortCode`) — 404 if it doesn't exist.
2. **Validate** it's usable:
   - `status != ACTIVE` → `410 Gone` ("This link is no longer active")
   - `expiresAt` in the past → `410 Gone` ("This link has expired")
3. **Record the click**: `clickCount++` and save, in the same transaction as the
   lookup (`@Transactional`), then return the link so the controller can issue the
   redirect.

**Why 302 (Found) and not 301 (Moved Permanently)?** A 301 tells browsers and
crawlers "cache this redirect forever, stop asking me" — which would make every
subsequent visit skip the backend entirely, and LinkPilot would lose click
tracking for repeat visitors. 302 says "this is where it points *right now*",
so the client comes back to `/r/{shortCode}` every time, which is required for
per-click analytics.

## 6. Where Redis fits in

`LinkService` is constructed with a `RedisTemplate<String, String>`, and the
intent (per the README: "Redirects served ... with Redis caching") is a classic
cache-aside layer in front of the SQLite lookup, keyed by `shortCode`:

```java
private void cache(Link link) {
    redisTemplate.opsForValue().set(link.getShortCode(), link.getOriginalUrl(), 1, TimeUnit.HOURS);
}
```

This is called on `create()` and `update()`, and the entry is evicted on
`delete()`. That's a **write-through** cache: every create/update pushes the
current `originalUrl` into Redis with a 1-hour TTL, and deletes clean up after
themselves.

**As currently implemented, `resolve()` never reads from Redis** — it queries
`linkRepository.findByShortCode()` (SQLite) unconditionally on every redirect.
The cache is populated and invalidated correctly, but nothing consumes it on the
hot path yet. In the intended design, `resolve()` would:

1. `GET shortCode` from Redis — on a hit, use the cached `originalUrl` directly
   (skipping the DB) and still increment `click_count` in SQLite, likely via an
   async/batched update since click tracking doesn't need to block the redirect.
2. On a cache miss, fall back to the DB lookup and repopulate Redis.

That's the standard shape of this pattern: reads are cheap after the first hit,
Redis absorbs the vast majority of traffic (redirects are read-heavy by nature —
a link is created once and clicked many times), and SQLite remains the durable
source of truth that Redis is rebuilt from.

## 7. End-to-end flow

```
CREATE
  Client ──POST /api/links {originalUrl, customCode?}──► LinkController
      └► LinkService.create()
            ├─ customCode given? check uniqueness : ShortCodeGenerator.generate()
            ├─ linkRepository.save(Link)               [SQLite — source of truth]
            └─ redisTemplate.set(shortCode → originalUrl, TTL 1h)  [cache warm-up]
      ◄── 201 { shortCode, originalUrl, ... }

REDIRECT
  Browser ──GET /r/{shortCode}──► RedirectController
      └► LinkService.resolve(shortCode)
            ├─ linkRepository.findByShortCode(shortCode)   [DB lookup — see §6 gap]
            ├─ status == ACTIVE?        else 410 Gone
            ├─ not expired?             else 410 Gone
            └─ clickCount++, save()
      ◄── 302 Found
          Location: <originalUrl>
  Browser follows Location header to the real destination
```

## 8. Summary of design choices

| Decision | Why |
|---|---|
| Random 7-char base62 code, not sequential ID | prevents enumeration/scraping of all links |
| Collision check-and-retry (max 10 attempts) | keyspace is large enough that retries are a formality, not a bottleneck |
| `UNIQUE` DB constraint on `short_code` | the real correctness guarantee; the retry loop is just an optimization to avoid hitting it |
| Custom codes fail loudly on conflict (409) instead of silently regenerating | caller explicitly chose that code — silent substitution would be surprising |
| 302 redirect, not 301 | keeps every click flowing through the backend for click tracking; a 301 would let browsers bypass the server on repeat visits |
| Redis as a write-through cache keyed by `shortCode` | redirects are read-heavy; caching avoids a DB hit per click once wired into the read path |
| `status` + `expiresAt` checked on every resolve | lets a link be disabled/expired without deleting the row (preserves click history) |
