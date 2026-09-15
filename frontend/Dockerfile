# Dependencies stage
FROM node:20-alpine AS deps
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci

# Build stage
FROM node:20-alpine AS build
WORKDIR /build
COPY --from=deps /build/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* vars are inlined into the client bundle at build time, not read at
# container start - this has to be a build ARG, not a docker-compose `environment:`.
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
RUN npm run build

# Runtime stage - standalone output only, not the full node_modules/project
FROM node:20-alpine AS runtime
WORKDIR /app
RUN addgroup -S linkpilot && adduser -S linkpilot -G linkpilot
COPY --from=build /build/public ./public
COPY --from=build --chown=linkpilot:linkpilot /build/.next/standalone ./
COPY --from=build --chown=linkpilot:linkpilot /build/.next/static ./.next/static
USER linkpilot
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
ENTRYPOINT ["node", "server.js"]
