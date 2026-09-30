#!/usr/bin/env bash
#
# Smoke test the seeded backends over real HTTP.
#
# This is the check that would have caught the Wallabag OAuth client bug: the
# app's hardcoded client id was "wallabag", which Wallabag can never match,
# because it resolves clients by the public id "<row id>_<random id>". MockWebServer
# tests cannot catch that, because they agree with whatever the app sends. Only
# a real server disagrees.
#
# It deliberately does not go through the app. It replays the same requests the
# adapters make, so a failure points at the server contract rather than at the
# UI.
#
#   ./smoke-test.sh
#
# Expects dev/seed.sh to have run. Non-fatal per backend: Readeck's image is
# only published to ghcr.io, which some environments cannot reach anonymously,
# and that should not fail a run over Wallabag.

cd "$(dirname "$0")"

READECK_URL="${READECK_URL:-http://localhost:${READECK_PORT:-8000}}"
WALLABAG_URL="${WALLABAG_URL:-http://localhost:${WALLABAG_PORT:-8080}}"
DEMO_USER="${DEMO_USER:-demo}"
DEMO_PASSWORD="${DEMO_PASSWORD:-password}"

fail=0
ok()   { printf '  \033[32mok\033[0m     %s\n' "$*"; }
bad()  { printf '  \033[31mFAIL\033[0m   %s\n' "$*"; fail=1; }
skip() { printf '  \033[33mskip\033[0m   %s\n' "$*"; }

# Prints the requested field, or nothing. Note the caller must test the *value*,
# not this function's exit status: printing an empty string still exits 0, and
# an `if ... | jqf ...` would then pass unconditionally.
jqf() { python3 -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

# ---------------------------------------------------------------- Wallabag
printf '\n\033[1mWallabag\033[0m  %s\n' "$WALLABAG_URL"

# seed.sh prints the client id; read it back out of the database so this does
# not depend on the row id being stable.
client_id="$(
    docker compose --profile wallabag exec -T wallabag-db \
        mariadb -uroot -p"${WALLABAG_DB_ROOT_PASSWORD:-wallabag-root}" -N -e \
        "SELECT CONCAT(id,'_',random_id) FROM wallabag.wallabag_oauth2_clients LIMIT 1;" \
        2>/dev/null | tr -d '\r'
)"
if [ -z "$client_id" ]; then
    bad "no OAuth client in the database; run ./seed.sh"
else
    ok "oauth client is $client_id"
fi

# The exact request WallabagAdapter.login makes.
token_json="$(
    curl -sS -X POST "$WALLABAG_URL/oauth/v2/token" \
        -d grant_type=password \
        -d "username=$DEMO_USER" \
        -d "password=$DEMO_PASSWORD" \
        -d "client_id=$client_id" \
        -d "client_secret=${WALLABAG_CLIENT_SECRET:-neverreader}" \
        --max-time 30 2>/dev/null || true
)"
token="$(printf '%s' "$token_json" | jqf "d.get('access_token','')")"
if [ -n "$token" ]; then
    ok "password grant returned an access token"
else
    bad "password grant failed: $(printf '%s' "$token_json" | head -c 160)"
fi

if [ -n "${token:-}" ]; then
    # The shape WallabagAdapter.listBookmarks expects: total in "total", items
    # under _embedded.
    entries_json="$(
        curl -sS -H "Authorization: Bearer $token" \
            "$WALLABAG_URL/api/entries?perPage=5" --max-time 30 2>/dev/null || true
    )"
    total="$(printf '%s' "$entries_json" | jqf "d.get('total',0)")"
    if [ "${total:-0}" -gt 0 ] 2>/dev/null; then
        ok "list returned $total entries"
    else
        bad "list returned no entries: $(printf '%s' "$entries_json" | head -c 160)"
    fi

    # And the call fetchArticleHtml makes, including the content field, which
    # the reader renders.
    first_id="$(printf '%s' "$entries_json" \
        | jqf "d['_embedded'].get('items', d['_embedded'].get('entries', []))[0]['id']" 2>/dev/null)"
    if [ -n "$first_id" ]; then
        entry_json="$(
            curl -sS -H "Authorization: Bearer $token" \
                "$WALLABAG_URL/api/entries/$first_id.json" --max-time 30 2>/dev/null || true
        )"
        chars="$(printf '%s' "$entry_json" | jqf "len(d.get('content') or '')")"
        if [ "${chars:-0}" -gt 200 ] 2>/dev/null; then
            ok "entry $first_id has $chars chars of article content"
        else
            bad "entry $first_id content was empty or missing"
        fi
    else
        bad "could not read an id out of the entry list"
    fi
fi

# Does the client id the app is configured with work? MockWebServer cannot answer
# this: it agrees with whatever the app sends, so a client id that no real
# Wallabag would accept passes every existing test. This is the check that
# catches it, and it is why the credentials are asked for in the setup screen
# rather than defaulted.
#
# The app no longer carries a client id at all, so this asserts the one the seed
# created, which is the same value a user would paste in.
expected_client_id="$client_id"
token_json="$(
    curl -sS -X POST "$WALLABAG_URL/oauth/v2/token" \
        -d grant_type=password \
        -d "username=$DEMO_USER" \
        -d "password=$DEMO_PASSWORD" \
        -d "client_id=$expected_client_id" \
        -d "client_secret=${WALLABAG_CLIENT_SECRET:-neverreader}" \
        --max-time 30 2>/dev/null || true
)"
if [ -n "$(printf '%s' "$token_json" | jqf "d.get('access_token','')")" ]; then
    ok "the seeded client ($expected_client_id) authenticates, as a user-supplied one would"
else
    bad "the seeded client ($expected_client_id) does not authenticate"
fi

# The regression that mattered: a bare name with no underscore can never match,
# because findClientByPublicId returns null without one. Assert that, so nobody
# reintroduces a hardcoded default and believes it works.
# --fail, so a 4xx is a non-zero exit. Without it curl exits 0 for any response
# it managed to receive, and this check passes for the wrong reason.
if curl -sSf -X POST "$WALLABAG_URL/oauth/v2/token" \
    -d grant_type=password \
    -d "username=$DEMO_USER" \
    -d "password=$DEMO_PASSWORD" \
    -d client_id=wallabag \
    -d client_secret=wallabag \
    --max-time 30 >/dev/null 2>&1; then
    bad "a bare \"wallabag\" client id was accepted; the underscore rule is not what we think"
else
    ok "a bare name with no underscore is still rejected, as Wallabag requires"
fi

# ----------------------------------------------------------------- Readeck
printf '\n\033[1mReadeck\033[0m  %s\n' "$READECK_URL"

if curl -sS -o /dev/null --max-time 20 "$READECK_URL/api/healthcheck" 2>/dev/null \
   || curl -sS -o /dev/null --max-time 20 "$READECK_URL/" 2>/dev/null; then
    ok "server responds"

    # Dynamic client registration, exactly as ReadeckAuth.registerClient sends
    # it. Readeck rejects the whole registration without software_version, so a
    # missing field shows up here.
    reg="$(curl -sS -X POST "$READECK_URL/oauth/client" \
        -H 'Content-Type: application/json' \
        -d '{"client_name":"NeverReader","client_uri":"https://readeck.org",
             "software_id":"com.neverreader","software_version":"1.0.0",
             "grant_types":["urn:ietf:params:oauth:grant-type:device_code"],
             "token_endpoint_auth_method":"none"}' --max-time 30 2>/dev/null || true)"
    client="$(printf '%s' "$reg" | jqf "d.get('client_id','')")"
    if [ -n "$client" ]; then
        ok "dynamic client registration returned a client_id"

        # The device flow the app starts. The code is not approved here, so the
        # token request is expected to be refused; what matters is that the
        # server issued a user code and verification uri.
        dev="$(curl -sS -X POST "$READECK_URL/oauth/device" \
            -d "client_id=$client" \
            -d 'scope=bookmarks:read bookmarks:write profile:read' \
            --max-time 30 2>/dev/null || true)"
        user_code="$(printf '%s' "$dev" | jqf "d.get('user_code','')")"
        verify="$(printf '%s' "$dev" | jqf "d.get('verification_uri_complete') or d.get('verification_uri') or ''")"
        if [ -n "$user_code" ] && [ -n "$verify" ]; then
            ok "device flow issued a user code and a verification uri"
        else
            bad "device flow did not start: $(printf '%s' "$dev" | head -c 160)"
        fi
    else
        bad "client registration failed: $(printf '%s' "$reg" | head -c 200)"
    fi

    # The seeded bookmarks, read straight from the database, because issuing a
    # usable API token here would mean driving the browser approval flow.
    count="$(
        docker compose --profile readeck exec -T readeck-db \
            psql -U "${READECK_DB_USER:-readeck}" -d "${READECK_DB_NAME:-readeck}" -tAc \
            "SELECT count(*) FROM bookmark;" 2>/dev/null | tr -d '\r ' || true
    )"
    if [ "${count:-0}" -gt 0 ] 2>/dev/null; then
        ok "$count bookmarks seeded"
    else
        bad "no bookmarks seeded; run ./seed.sh"
    fi
else
    skip "Readeck is not reachable here"
    skip "its image is only on ghcr.io, which some environments cannot pull anonymously"
fi

# Both test tasks are run in the unit-tests job; this only reports. Keeping it
# separate means a Readeck image problem is visible as a skip rather than
# silently narrowing what is checked.
printf '\n'
if [ "$fail" -eq 0 ]; then
    printf '\033[32mSmoke test passed.\033[0m\n'
else
    printf '\033[31mSmoke test failed.\033[0m\n'
fi
exit "$fail"
