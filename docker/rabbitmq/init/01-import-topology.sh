#!/usr/bin/env bash
set -euo pipefail

: "${RABBITMQ_HOST:?RABBITMQ_HOST must be set}"
: "${RABBITMQ_USERNAME:?RABBITMQ_USERNAME must be set}"
: "${RABBITMQ_PASSWORD:?RABBITMQ_PASSWORD must be set}"

DEFINITIONS_FILE="${RABBITMQ_DEFINITIONS_FILE:-/etc/rabbitmq/definitions.json}"
MANAGEMENT_PORT="${RABBITMQ_MANAGEMENT_PORT:-15672}"

[[ -f "${DEFINITIONS_FILE}" ]] || { echo "ERROR: ${DEFINITIONS_FILE} not found" >&2; exit 1; }

ADMIN=(rabbitmqadmin
  --host "${RABBITMQ_HOST}"
  --port "${MANAGEMENT_PORT}"
  --username "${RABBITMQ_USERNAME}"
  --password "${RABBITMQ_PASSWORD}"
)

echo "Waiting for RabbitMQ management API at ${RABBITMQ_HOST}:${MANAGEMENT_PORT}..."

count=0
until "${ADMIN[@]}" show overview >/dev/null 2>&1 || (( count++ >= 60 )); do
  sleep 2
done

if (( count >= 60 )); then
  echo "ERROR: management API not reachable" >&2
  "${ADMIN[@]}" show overview || true
  exit 1
fi

echo "Importing topology from ${DEFINITIONS_FILE}..."
"${ADMIN[@]}" definitions import "${DEFINITIONS_FILE}"

echo "Topology import complete."