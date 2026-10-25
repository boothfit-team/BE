#!/usr/bin/env bash
set -Eeuo pipefail

image="${1:-}"
container="boothfit-be"
env_file="/etc/boothfit/app.env"

if [[ ! "$image" =~ ^[0-9]{12}\.dkr\.ecr\.([a-z0-9-]+)\.amazonaws\.com/boothfit-be:[0-9a-f]{40}$ ]]; then
  echo "Expected a boothfit-be ECR image tagged with a full commit SHA" >&2
  exit 2
fi

region="${BASH_REMATCH[1]}"
registry="${image%%/*}"

if [[ ! -f "$env_file" ]]; then
  echo "Missing $env_file; instance bootstrap may not have completed" >&2
  exit 2
fi

for key in SPRING_PROFILES_ACTIVE DB_HOST POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD; do
  if ! grep -Eq "^${key}=.+$" "$env_file"; then
    echo "Missing $key in $env_file" >&2
    exit 2
  fi
done

if ! grep -qx 'SPRING_PROFILES_ACTIVE=prod' "$env_file" ||
   ! grep -qx 'DB_HOST=boothfit-db' "$env_file"; then
  echo "Expected production profile and DB_HOST=boothfit-db in $env_file" >&2
  exit 2
fi

exec 9>/var/lock/boothfit-deploy.lock
flock -n 9 || { echo "Another deployment is running" >&2; exit 1; }

previous_image="$(docker inspect --format '{{.Config.Image}}' "$container" 2>/dev/null || true)"

docker network inspect boothfit-net >/dev/null 2>&1 || docker network create boothfit-net >/dev/null
docker volume inspect boothfit-postgres-data >/dev/null 2>&1 || docker volume create boothfit-postgres-data >/dev/null

if docker container inspect boothfit-db >/dev/null 2>&1; then
  docker start boothfit-db >/dev/null 2>&1 || true
else
  docker run -d \
    --name boothfit-db \
    --restart unless-stopped \
    --network boothfit-net \
    --memory 512m \
    --env-file "$env_file" \
    -v boothfit-postgres-data:/var/lib/postgresql/data \
    postgres:17 >/dev/null
fi

db_ready=false
for _ in $(seq 1 30); do
  if docker exec boothfit-db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "SELECT 1"' \
    2>/dev/null | grep -qx '1'; then
    db_ready=true
    break
  fi
  sleep 2
done
if [[ "$db_ready" != true ]]; then
  echo "PostgreSQL is not ready" >&2
  docker logs --tail 80 boothfit-db >&2 || true
  exit 1
fi

aws ecr get-login-password --region "$region" \
  | docker login --username AWS --password-stdin "$registry" >/dev/null
docker pull "$image"

start_container() {
  local target_image="$1"
  docker rm -f "$container" >/dev/null 2>&1 || true
  docker run -d \
    --name "$container" \
    --restart unless-stopped \
    --memory 1g \
    --network boothfit-net \
    --env-file "$env_file" \
    -p 80:8080 \
    "$target_image" >/dev/null
}

wait_for_health() {
  for _ in $(seq 1 30); do
    if curl --fail --silent --show-error --max-time 2 \
      http://127.0.0.1/actuator/health 2>/dev/null \
      | grep -q '"status":"UP"'; then
      return 0
    fi
    sleep 2
  done
  return 1
}

if start_container "$image" && wait_for_health; then
  echo "Deployment healthy: $image"
  exit 0
fi

echo "Deployment failed health check; recent container logs:" >&2
docker logs --tail 80 "$container" >&2 || true

if [[ -n "$previous_image" ]]; then
  echo "Restoring previous image: $previous_image" >&2
  if start_container "$previous_image" && wait_for_health; then
    echo "Previous image restored" >&2
  else
    echo "Rollback also failed; manual intervention required" >&2
  fi
else
  docker rm -f "$container" >/dev/null 2>&1 || true
  echo "No previous image is available for rollback" >&2
fi

exit 1
