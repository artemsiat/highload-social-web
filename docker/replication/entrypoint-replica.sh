#!/bin/bash
# docker/replication/entrypoint-replica.sh
set -eu

: "${REPLICA_NAME:?REPLICA_NAME is required, for example replica1 or replica2}"
: "${PRIMARY_HOST:?PRIMARY_HOST is required}"
: "${REPLICATION_USER:?REPLICATION_USER is required}"
: "${REPLICATION_PASSWORD:?REPLICATION_PASSWORD is required}"

PRIMARY_PORT="${PRIMARY_PORT:-5432}"
REPLICATION_CONNINFO="host=$PRIMARY_HOST port=$PRIMARY_PORT user=$REPLICATION_USER password=$REPLICATION_PASSWORD application_name=$REPLICA_NAME"

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  until pg_isready -h "$PRIMARY_HOST" -p "$PRIMARY_PORT"; do
    echo "Waiting for primary at $PRIMARY_HOST..."
    sleep 2
  done

  echo "Cloning data from primary via pg_basebackup for replica '$REPLICA_NAME'..."
  rm -rf "$PGDATA"
  mkdir -p "$PGDATA"
  chown postgres:postgres "$PGDATA"
  gosu postgres pg_basebackup \
    -d "$REPLICATION_CONNINFO" \
    -D "$PGDATA" -Fp -Xs -P -R
  chmod 700 "$PGDATA"
  echo "Base backup completed."
fi

echo "Starting PostgreSQL replica..."
exec gosu postgres postgres "$@"
