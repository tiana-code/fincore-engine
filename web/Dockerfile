# SPDX-License-Identifier: BUSL-1.1
# SPDX-FileCopyrightText: 2026 FinCore Engine Authors

FROM node:22-alpine AS builder
WORKDIR /web
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY --from=builder /web/dist /usr/share/nginx/html
COPY default.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
