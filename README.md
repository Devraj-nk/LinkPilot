# LinkPilot - URL Shortener Service

A modern URL shortening service built with a full-stack architecture.

## Tech Stack

- **Frontend**: Next.js + TypeScript
- **Backend**: Spring Boot + Java
- **Primary Database**: SQLite (embedded)
- **Cache / Temporary State**: Redis

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
- `GET/POST /api/links`, `GET/PUT/DELETE /api/links/{id}`
- `GET /r/{shortCode}` - public redirect (302 to the original URL, tracks the click)
- `GET/POST /api/links/{linkId}/qrcodes`, `DELETE /api/qrcodes/{id}`
- `GET /api/qrcodes/{id}/image` - public, returns a PNG
- `GET/POST /api/domains`, `DELETE /api/domains/{id}`, `POST /api/domains/{id}/verify`
- `GET/POST /api/api-keys`, `DELETE /api/api-keys/{id}`

The frontend never calls these directly from the browser - it goes through the
matching proxy routes under `frontend/app/api/**` (see [Authentication](#authentication)).

## Features Implemented

1. Email/password auth (JWT access tokens + rotating refresh tokens)
2. Link shortening with custom or generated short codes, campaigns, expiration, and click tracking
3. Redirects served at the root (`/{shortCode}`) via the backend, with Redis caching
4. QR code generation (PNG) for any link, with configurable size/colors
5. Campaign and custom-domain management
6. API key management (for future programmatic access - see Future Enhancements)

## Future Enhancements

- Email verification (the `is_verified` flag exists but nothing sets it)
- QR code SVG/PDF rendering (PNG is the only format actually implemented)
- Real DNS-based domain verification (currently a stub that just flips the status)
- Using API keys to authenticate API requests (currently only CRUD-managed, not a working auth path)
- Admin-only endpoints (the `ADMIN` role exists but nothing checks it)
- Advanced analytics dashboard
- Rate limiting and abuse prevention
- Docker containerization for easy deployment