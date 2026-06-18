#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="/opt/ingoboka"
COMPOSE_DIR="${DEPLOY_DIR}/deploy"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yml"

mkdir -p "${COMPOSE_DIR}"

if [[ -f "${DEPLOY_DIR}/ingoboka-deploy.tar.gz" ]]; then
  cd "${DEPLOY_DIR}"
  rm -rf src Dockerfile pom.xml mvnw mvnw.cmd .mvn
  tar -xzf ingoboka-deploy.tar.gz
fi

cd "${COMPOSE_DIR}"

if [[ ! -f .env ]]; then
  echo "Missing ${COMPOSE_DIR}/.env — copy .env.example and configure secrets."
  exit 1
fi

docker compose -f "${COMPOSE_FILE}" pull api || true
docker compose up -d --build --remove-orphans
docker compose ps
