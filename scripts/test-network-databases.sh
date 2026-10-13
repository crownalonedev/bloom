#!/usr/bin/env bash
# Runs bloom-storage's NetworkDatabaseIT against real MySQL 8.4 and MariaDB 11 containers.
# Requires a running Docker daemon. Containers are removed afterwards.
set -euo pipefail
cd "$(dirname "$0")/.."

MYSQL_PORT="${BLOOM_TEST_MYSQL_PORT:-33061}"
MARIADB_PORT="${BLOOM_TEST_MARIADB_PORT:-33062}"

cleanup() {
  docker rm -f bloom-test-mysql bloom-test-mariadb >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run -d --name bloom-test-mysql -p "${MYSQL_PORT}:3306" \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=bloom -e MYSQL_USER=bloom -e MYSQL_PASSWORD=bloom mysql:8.4 >/dev/null
docker run -d --name bloom-test-mariadb -p "${MARIADB_PORT}:3306" \
  -e MARIADB_ROOT_PASSWORD=root -e MARIADB_DATABASE=bloom -e MARIADB_USER=bloom -e MARIADB_PASSWORD=bloom mariadb:11 >/dev/null

wait_for() {
  local name="$1" cmd="$2"
  for _ in $(seq 1 60); do
    if docker exec "$name" sh -c "$cmd" >/dev/null 2>&1; then return 0; fi
    sleep 2
  done
  echo "Timed out waiting for $name" >&2
  docker logs "$name" | tail -20 >&2
  return 1
}
wait_for bloom-test-mysql "mysql -ubloom -pbloom -e 'SELECT 1' bloom"
wait_for bloom-test-mariadb "mariadb -ubloom -pbloom -e 'SELECT 1' bloom"

BLOOM_TEST_MYSQL_PORT="$MYSQL_PORT" BLOOM_TEST_MARIADB_PORT="$MARIADB_PORT" \
  ./gradlew :bloom-storage:networkDatabaseTest --rerun --console=plain
