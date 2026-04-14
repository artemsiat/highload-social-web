#!/bin/bash
# docker/replication/entrypoint-replica.sh
set -e

until pg_isready -h "$PRIMARY_HOST" -p 5432; do
  echo "Waiting for primary at $PRIMARY_HOST..."
  sleep 2
done

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo "Cloning data from primary via pg_basebackup..."
  rm -rf "$PGDATA"/*
  PGPASSWORD="$REPLICATION_PASSWORD" pg_basebackup \
    -h "$PRIMARY_HOST" -p 5432 -U "$REPLICATION_USER" \
    -D "$PGDATA" -Fp -Xs -P -R
  chmod 700 "$PGDATA"
  echo "Base backup completed."
fi

echo "Starting PostgreSQL replica..."
exec postgres "$@"
