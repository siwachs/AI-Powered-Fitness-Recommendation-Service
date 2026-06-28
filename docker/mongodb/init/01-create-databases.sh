#!/bin/bash
set -euo pipefail

: "${ACTIVITY_SERVICE_DB_NAME:?ACTIVITY_SERVICE_DB_NAME must be set}"
: "${AI_SERVICE_DB_NAME:?AI_SERVICE_DB_NAME must be set}"

mongosh <<EOF
db = db.getSiblingDB('${ACTIVITY_SERVICE_DB_NAME}');
db.createCollection('_init');

db = db.getSiblingDB('${AI_SERVICE_DB_NAME}');
db.createCollection('_init');
EOF

# Create app user only when the service URI uses that user (prod). Dev URIs use root (mongo).
if [[ -n "${ACTIVITY_SERVICE_DB_USER:-}" && -n "${ACTIVITY_SERVICE_DB_PASSWORD:-}" && -n "${ACTIVITY_SERVICE_MONGO_URI:-}" && "${ACTIVITY_SERVICE_MONGO_URI}" == *"${ACTIVITY_SERVICE_DB_USER}"* ]]; then
mongosh <<EOF
db = db.getSiblingDB('${ACTIVITY_SERVICE_DB_NAME}');
db.createUser({
  user: '${ACTIVITY_SERVICE_DB_USER}',
  pwd: '${ACTIVITY_SERVICE_DB_PASSWORD}',
  roles: [{ role: 'readWrite', db: '${ACTIVITY_SERVICE_DB_NAME}' }]
});
EOF
fi

if [[ -n "${AI_SERVICE_DB_USER:-}" && -n "${AI_SERVICE_DB_PASSWORD:-}" && -n "${AI_SERVICE_MONGO_URI:-}" && "${AI_SERVICE_MONGO_URI}" == *"${AI_SERVICE_DB_USER}"* ]]; then
mongosh <<EOF
db = db.getSiblingDB('${AI_SERVICE_DB_NAME}');
db.createUser({
  user: '${AI_SERVICE_DB_USER}',
  pwd: '${AI_SERVICE_DB_PASSWORD}',
  roles: [{ role: 'readWrite', db: '${AI_SERVICE_DB_NAME}' }]
});
EOF
fi
