#!/usr/bin/env bash
#
# Seed the local Readeck and Wallabag instances with a demo user and dummy
# articles. Safe to re-run: everything it creates is either idempotent or
# replaced wholesale.
#
#   ./seed.sh              # both, if they are up
#   ./seed.sh wallabag     # just one
#
# The compose halves are behind profiles, so start them first:
#   docker compose up -d                      # both
#   docker compose --profile wallabag up -d   # just Wallabag

set -euo pipefail

cd "$(dirname "$0")"

WHICH="${1:-all}"
COMPOSE=(docker compose)
if [ "$WHICH" != "all" ]; then
    COMPOSE+=(--profile "$WHICH")
fi

# Kept in step with the defaults in docker-compose.yml and .env.
DEMO_USER="${DEMO_USER:-demo}"
DEMO_EMAIL="${DEMO_EMAIL:-demo@example.com}"
DEMO_PASSWORD="${DEMO_PASSWORD:-password}"
# Must match AuthenticationViewModel's WALLABAG_CLIENT_ID/SECRET. See the
# "Wallabag client" section of README.md before changing it.
WALLABAG_CLIENT_SECRET="${WALLABAG_CLIENT_SECRET:-neverreader}"
WALLABAG_RANDOM_ID="${WALLABAG_RANDOM_ID:-neverreader}"
WALLABAG_DB_ROOT_PASSWORD="${WALLABAG_DB_ROOT_PASSWORD:-wallabag-root}"

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
die() { printf '\n!! %s\n' "$*" >&2; exit 1; }

running() {
    "${COMPOSE[@]}" ps --services --filter status=running 2>/dev/null | grep -qx "$1"
}

case "$WHICH" in
all|readeck|wallabag) ;;
*) die "unknown target: $WHICH (expected all, readeck or wallabag)" ;;
esac

if [ "$WHICH" = "all" ] || [ "$WHICH" = "readeck" ]; then
    say "Readeck"
    if ! running readeck; then
        die "readeck is not running: docker compose --profile readeck up -d"
    fi
    # The DB container holds psql; the DSN Readeck itself uses is in the compose.
    DSN="postgres://${READECK_DB_USER:-readeck}:${READECK_DB_PASSWORD:-readeck}@readeck-db:5432/${READECK_DB_NAME:-readeck}?sslmode=disable"

    # `readeck user` is idempotent, so this is safe on an existing database.
    "${COMPOSE[@]}" exec -T -e READECK_DATABASE_SOURCE="$DSN" readeck \
        readeck user --user "$DEMO_USER" --email "$DEMO_EMAIL" --password "$DEMO_PASSWORD" \
        2>&1 | tail -3

    if [ ! -f seed/readeck-bookmarks.sql ]; then
        say "generating seed data"
        python3 seed/generate.py
    fi

    # Replace rather than append, so re-running does not duplicate the list.
    "${COMPOSE[@]}" exec -T readeck-db \
        psql -v ON_ERROR_STOP=1 -U "${READECK_DB_USER:-readeck}" -d "${READECK_DB_NAME:-readeck}" \
        -c 'DELETE FROM bookmark;'
    "${COMPOSE[@]}" exec -T readeck-db \
        psql -v ON_ERROR_STOP=1 -U "${READECK_DB_USER:-readeck}" -d "${READECK_DB_NAME:-readeck}" \
        < seed/readeck-bookmarks.sql
    "${COMPOSE[@]}" exec -T readeck-db \
        psql -U "${READECK_DB_USER:-readeck}" -d "${READECK_DB_NAME:-readeck}" \
        -c "SELECT count(*) AS bookmarks, count(*) FILTER (WHERE is_marked) AS favourites, count(*) FILTER (WHERE is_archived) AS archived FROM bookmark;"
fi

if [ "$WHICH" = "all" ] || [ "$WHICH" = "wallabag" ]; then
    say "Wallabag"
    if ! running wallabag; then
        die "wallabag is not running: docker compose --profile wallabag up -d"
    fi

    if [ ! -f seed/wallabag-import.json ]; then
        say "generating seed data"
        python3 seed/generate.py
    fi

    if ! "${COMPOSE[@]}" exec -T wallabag \
        sh -c "cd /var/www/wallabag && php bin/console fos:user:create --env=prod '$DEMO_USER' '$DEMO_EMAIL' '$DEMO_PASSWORD'" \
        >/dev/null 2>&1; then
        echo "   user $DEMO_USER already exists"
    else
        echo "   created user $DEMO_USER"
    fi

    # fos:oauth-server:create-client is broken in this Wallabag version: its
    # Client entity takes a User in the constructor, so the command dies with an
    # ArgumentCountError. seed/create-client.php does the same job via Doctrine.
    "${COMPOSE[@]}" cp seed/create-client.php wallabag:/tmp/create-client.php >/dev/null
    CLIENT_LINE="$("${COMPOSE[@]}" exec -T wallabag \
        php /tmp/create-client.php "$DEMO_USER" "$WALLABAG_RANDOM_ID" "$WALLABAG_CLIENT_SECRET" \
        2>/dev/null | grep client_id || true)"
    CLIENT_ID="$(printf '%s' "$CLIENT_LINE" | sed -n 's/.*client_id *= *//p')"
    if [ -z "$CLIENT_ID" ]; then
        die "could not create the Wallabag OAuth client; run seed/create-client.php by hand to see why"
    fi

    # Importer "v2" is the wallabag one. --disableContentUpdate stops it
    # fetching every URL, which matters because the seeded urls do not resolve.
    "${COMPOSE[@]}" cp seed/wallabag-import.json wallabag:/tmp/import.json >/dev/null
    "${COMPOSE[@]}" exec -T wallabag \
        sh -c "cd /var/www/wallabag && php bin/console wallabag:import --env=prod --importer=v2 --disableContentUpdate '$DEMO_USER' /tmp/import.json" \
        2>&1 | tail -4

    "${COMPOSE[@]}" exec -T wallabag-db \
        mariadb -uroot -p"$WALLABAG_DB_ROOT_PASSWORD" -N -e \
        "SELECT CONCAT(COUNT(*),' entries, ',SUM(is_starred),' starred, ',SUM(is_archived),' archived') FROM wallabag.wallabag_entry;" \
        2>/dev/null | tail -1

    cat <<EOF

   Wallabag is ready.

     url     http://localhost:${WALLABAG_PORT:-8080}
     user    $DEMO_USER / $DEMO_PASSWORD
     client  $CLIENT_ID / $WALLABAG_CLIENT_SECRET

   The client_id is NOT "wallabag". Wallabag's token endpoint looks a client up
   by the public id "<row id>_<random id>", and returns null for anything
   without that underscore, so the app's hardcoded default cannot authenticate
   against any Wallabag. See README.md.
EOF
fi
