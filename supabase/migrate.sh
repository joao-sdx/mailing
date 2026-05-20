#!/usr/bin/env bash
# Run from the supabase-project directory on the server:
#   bash /path/to/supabase/migrate.sh
set -euo pipefail

MIGRATIONS_DIR="$(cd "$(dirname "$0")/migrations" && pwd)"

psql() {
  docker compose exec -T db psql -U postgres -d postgres "$@"
}

# Create tracking table if it doesn't exist
psql -c "
  CREATE TABLE IF NOT EXISTS schema_migrations (
    version    TEXT PRIMARY KEY,
    applied_at TIMESTAMPTZ DEFAULT now()
  );
"

for file in $(ls "$MIGRATIONS_DIR"/V*.sql | sort); do
  version=$(basename "$file" .sql)
  applied=$(psql -tA -c "SELECT 1 FROM schema_migrations WHERE version = '$version'")
  if [ "$applied" = "1" ]; then
    echo "skip  $version"
  else
    echo "apply $version"
    psql -f "$file"
    psql -c "INSERT INTO schema_migrations (version) VALUES ('$version')"
    echo "done  $version"
  fi
done

echo "migrations up to date"
