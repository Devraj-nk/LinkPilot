# LinkPilot - URL Shortener Service

A modern URL shortening service built with a full-stack architecture.

## Tech Stack

- **Frontend**: Next.js + TypeScript
- **Backend**: Spring Boot + Java
- **Primary Database**: PostgreSQL
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
- PostgreSQL database
- Redis server

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Configure PostgreSQL and Redis in `src/main/resources/application.properties`:
   ```properties
   # Database
   spring.datasource.url=jdbc:postgresql://localhost:5432/LinkPilot
   spring.datasource.username=postgres
   spring.datasource.password=postgres
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

3. Run the development server:
   ```bash
   npm run dev
   ```
   The frontend will be available at `http://localhost:3000`

## API Endpoints

### Backend (Spring Boot)

- `POST /api/links` - Create a shortened URL
  - Parameters: `url` (form parameter)
  - Returns: JSON with the created link object

- `GET /api/links/{shortCode}` - Redirect to original URL
  - Path variable: `shortCode`
  - Returns: HTTP 302 redirect to original URL

### Frontend (Next.js API Routes)

- `POST /api/links` - Proxy to backend for creating shortened URLs
  - Body: `{ "url": "https://example.com" }`
  - Returns: JSON response from backend

## Features Implemented

1. URL shortening with unique short codes
2. Redirect functionality from short URLs to original URLs
3. Click tracking and analytics
4. Redis caching for improved performance
5. Responsive UI with copy-to-clipboard functionality

## Future Enhancements

- User authentication and link management
- Custom short code generation
- QR code generation for short links
- Advanced analytics dashboard
- Rate limiting and abuse prevention
- Docker containerization for easy deployment