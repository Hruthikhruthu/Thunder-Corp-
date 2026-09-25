# Stage 1: Build the React/Vite static application.
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
# npm ci keeps Docker builds reproducible from package-lock.json.
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve compiled assets and proxy API/WebSocket traffic through Nginx.
FROM nginx:1.25-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
# Compose uses this healthcheck before reporting the frontend as healthy.
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
