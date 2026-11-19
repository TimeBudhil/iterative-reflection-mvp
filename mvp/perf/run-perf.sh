#!/usr/bin/env bash
# Manual performance check (ticket 19). NOT run by `./mvnw verify` or CI.
# Usage: perf/run-perf.sh [students=300] [threads=4]
# Builds the jar, starts it with -Xmx512m on a throwaway file-based H2 db (dev profile, demo accounts),
# seeds pilot-scale data over JDBC, drives the real HTTP API and prints p95 per endpoint.
# Exit code is 0 only if every target is met.
set -euo pipefail

STUDENTS="${1:-300}"
THREADS="${2:-4}"
PORT="${PORT:-18080}"
HEAP="${HEAP:--Xmx512m}"
PERF_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PERF_DIR/.."

WORK="$(pwd)/target/perf"
rm -rf "$WORK"
mkdir -p "$WORK/lib"

./mvnw -q -DskipTests package
JAR="$(ls target/mvp-*.jar | grep -v original | head -1)"
(cd "$WORK/lib" && unzip -q -o "../../../$JAR" 'BOOT-INF/lib/h2-*.jar' && mv BOOT-INF/lib/h2-*.jar h2.jar)

DB="jdbc:h2:file:$WORK/db/perf;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE"
java "$HEAP" -jar "$JAR" --spring.profiles.active=dev --server.port="$PORT" --spring.datasource.url="$DB" \
  --logging.level.root=WARN > "$WORK/app.log" 2>&1 &
APP=$!
trap 'kill $APP 2>/dev/null || true' EXIT

for _ in $(seq 1 120); do
  curl -fs "http://localhost:$PORT/actuator/health" >/dev/null 2>&1 && break
  kill -0 $APP 2>/dev/null || { echo "app died:"; tail -30 "$WORK/app.log"; exit 2; }
  sleep 1
done

java -cp "$WORK/lib/h2.jar" "$PERF_DIR/PerfCheck.java" "http://localhost:$PORT" "$DB" "$STUDENTS" "$THREADS" \
  | tee "$WORK/results.txt"
