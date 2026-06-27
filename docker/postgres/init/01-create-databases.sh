#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE userdb'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'userdb')\gexec
EOSQL

# Only create app user if credentials are provided (prod)
if [[ -n "${USER_SERVICE_DB_USER:-}" && -n "${USER_SERVICE_DB_PASSWORD:-}" ]]; then
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${USER_SERVICE_DB_USER}') THEN
        CREATE ROLE ${USER_SERVICE_DB_USER} WITH LOGIN PASSWORD '${USER_SERVICE_DB_PASSWORD}';
      END IF;
    END
    \$\$;
    GRANT ALL PRIVILEGES ON DATABASE userdb TO ${USER_SERVICE_DB_USER};
EOSQL

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "userdb" <<-EOSQL
    GRANT ALL ON SCHEMA public TO ${USER_SERVICE_DB_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${USER_SERVICE_DB_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${USER_SERVICE_DB_USER};
EOSQL
fi