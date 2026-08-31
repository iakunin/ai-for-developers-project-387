# syntax=docker/dockerfile:1

# 1. The UI. VITE_API_BASE_URL=/ makes apiBaseUrl() resolve to the empty string, so every
#    request becomes a same-origin relative path — exactly what one container needs.
FROM node:22 AS frontend
RUN apt-get update && apt-get install -y --no-install-recommends make \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY api-contract/openapi ./api-contract/openapi
COPY code/frontend ./code/frontend
WORKDIR /src/code/frontend
ENV VITE_API_BASE_URL=/
# Same reason as the Gradle cache below: without it every build re-downloads the whole npm
# dependency tree into a layer that is thrown away.
RUN --mount=type=cache,target=/root/.npm make install-ci && make build

# 2. The API. The frontend build output goes onto the classpath, so one jar carries both.
FROM bellsoft/liberica-openjdk-alpine:25 AS backend
RUN apk add --no-cache make
WORKDIR /src
COPY api-contract/openapi ./api-contract/openapi
COPY code/backend ./code/backend
COPY --from=frontend /src/code/frontend/dist ./code/backend/src/main/resources/static
WORKDIR /src/code/backend

# Gradle keeps its distribution and every downloaded dependency under GRADLE_USER_HOME. Without
# this cache mount they are re-fetched from scratch on every build — ~7 minutes of silence before
# the first task prints anything, which looks exactly like a hang. The daemon is dead weight in a
# container that is discarded after one build, so it only adds startup time.
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
RUN --mount=type=cache,target=/root/.gradle make build

# 3. Runtime.
FROM bellsoft/liberica-openjre-alpine:25 AS runtime
RUN adduser --system --shell /sbin/nologin app
WORKDIR /app
COPY --from=backend /src/code/backend/build/libs/*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
