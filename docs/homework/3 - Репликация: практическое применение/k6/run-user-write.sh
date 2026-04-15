#!/usr/bin/env bash
set -euo pipefail

HOMEWORK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HOMEWORK_DIR"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not installed." >&2
  echo "Install it on macOS with: brew install k6" >&2
  exit 1
fi

VUS="${1:-10}"
DURATION="${2:-120s}"
BASE_URL="${BASE_URL:-http://localhost:8075}"
WRITE_DATA_FILE="${WRITE_DATA_FILE:-./user-write-data.json}"
WRITE_LOGIN_PREFIX="${WRITE_LOGIN_PREFIX:-k6w}"
WRITE_RUN_ID="${WRITE_RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
WRITE_PASSWORD="${WRITE_PASSWORD:-test_password}"
DB_SCHEMA="${DB_SCHEMA:-highload_social_web}"
RESULTS_DIR="${RESULTS_DIR:-${HOMEWORK_DIR}/results}"
SUMMARY_FILE="${RESULTS_DIR}/user-write-${VUS}vus-${DURATION}-${WRITE_RUN_ID}.json"

mkdir -p "$RESULTS_DIR"

cat <<INFO
Running k6 user write test with:
  BASE_URL=${BASE_URL}
  ENDPOINT=POST /user/register
  WRITE_DATA_FILE=${WRITE_DATA_FILE}
  WRITE_LOGIN_PREFIX=${WRITE_LOGIN_PREFIX}
  WRITE_RUN_ID=${WRITE_RUN_ID}
  DB_SCHEMA=${DB_SCHEMA}
  K6_VUS=${VUS}
  K6_DURATION=${DURATION}
  SUMMARY_FILE=${SUMMARY_FILE}

After the run, compare successfulWrites with:
  SELECT count(*) FROM ${DB_SCHEMA}.users WHERE login LIKE '${WRITE_LOGIN_PREFIX}-${WRITE_RUN_ID}-%';
INFO

K6_VUS="$VUS" \
K6_DURATION="$DURATION" \
BASE_URL="$BASE_URL" \
WRITE_DATA_FILE="$WRITE_DATA_FILE" \
WRITE_LOGIN_PREFIX="$WRITE_LOGIN_PREFIX" \
WRITE_RUN_ID="$WRITE_RUN_ID" \
WRITE_PASSWORD="$WRITE_PASSWORD" \
DB_SCHEMA="$DB_SCHEMA" \
SUMMARY_FILE="$SUMMARY_FILE" \
k6 run ./user-write.js
