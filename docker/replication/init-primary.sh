#!/bin/bash
# docker/replication/init-primary.sh
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v replication_password="$REPLICATION_PASSWORD" <<-EOSQL
    CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD :'replication_password';
EOSQL

echo "host replication replicator 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"
