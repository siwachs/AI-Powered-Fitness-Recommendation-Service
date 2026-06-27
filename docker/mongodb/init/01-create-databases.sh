#!/bin/bash
set -euo pipefail

mongosh <<EOF
// Dev: only create DBs when using root auth (no app users)
db = db.getSiblingDB('activitydb');
db.createCollection('_init');

db = db.getSiblingDB('aidb');
db.createCollection('_init');
EOF

# Prod: create scoped users from env (only if vars set)
if [[ -n "${ACTIVITY_SERVICE_DB_USER:-}" && -n "${ACTIVITY_SERVICE_DB_PASSWORD:-}" ]]; then
mongosh <<EOF
db = db.getSiblingDB('activitydb');
db.createUser({
  user: '${ACTIVITY_SERVICE_DB_USER}',
  pwd: '${ACTIVITY_SERVICE_DB_PASSWORD}',
  roles: [{ role: 'readWrite', db: 'activitydb' }]
});
EOF
fi

if [[ -n "${AI_SERVICE_DB_USER:-}" && -n "${AI_SERVICE_DB_PASSWORD:-}" ]]; then
mongosh <<EOF
db = db.getSiblingDB('aidb');
db.createUser({
  user: '${AI_SERVICE_DB_USER}',
  pwd: '${AI_SERVICE_DB_PASSWORD}',
  roles: [{ role: 'readWrite', db: 'aidb' }]
});
EOF
fi