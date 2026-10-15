# syntax=docker/dockerfile:1
# pokedex-web: Node builds the static app; nginx serves it and proxies /api to pokedex-api.

FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Where the app calls the API. Same origin by default: nginx proxies /api below.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.31-alpine
# The backend nginx proxies /api/ to (runtime setting, no rebuild needed).
ENV API_UPSTREAM=http://api:8080
# Fills NGINX_LOCAL_RESOLVERS from /etc/resolv.conf, so the upstream host is resolved per request
# and nginx starts even when the backend is not up yet.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/healthz >/dev/null || exit 1
