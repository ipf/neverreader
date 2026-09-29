# Local backends for development

Readeck and Wallabag in Docker, with a demo user and dummy articles, for
exercising the app against real servers instead of mocks.

```
docker compose up -d        # both
./seed.sh                   # demo user + 20 articles in each
```

Then point the app at them:

| | URL | Sign-in |
|---|---|---|
| Readeck | `http://localhost:8000` | pick Readeck, tap through the device flow, log in as `demo` / `password` |
| Wallabag | `http://localhost:8080` | pick Wallabag, `demo` / `password` |

From an Android emulator the host machine is `10.0.2.2`, so use
`http://10.0.2.2:8000` and `http://10.0.2.2:8080` there instead of `localhost`.

Cleartext is permitted by the app's network security config, so plain HTTP to
these works. See the Privacy section of the top-level README for why that is
still the case.

The two halves are behind the `readeck` and `wallabag` profiles, so either can
run on its own:

```
docker compose --profile wallabag up -d
./seed.sh wallabag
```

## What gets seeded

`demo` / `demo@example.com` / `password`, and 20 articles with titles,
excerpts, and a body, spread over the last ~60 days. Three are favourites,
three are archived, five carry a tag, so the list has something to filter and
sort without touching anything. Both servers get the same 20 articles, from one
definition in `seed/generate.py`, so a difference between the two lists is a
backend difference rather than a data difference.

`seed.sh` is safe to re-run: the user is created only if missing, the OAuth
client is reused if it exists, and entries are replaced rather than appended.
It needs `python3` to regenerate the fixtures; the generated files are
committed, so it only runs when they are missing.

## Layout

```
docker-compose.yml       the four containers
seed.sh                  creates the user and loads the content
seed/generate.py         the single definition of the dummy articles
seed/readeck-bookmarks.sql    generated; INSERTs against Readeck's bookmark table
seed/wallabag-import.json      generated; Wallabag's v2 import format
seed/create-client.php        creates the Wallabag OAuth client
```

Both databases are internal to the compose network and are not published. Only
the two application ports are.

## Wallabag client

The seeded OAuth client is `3_neverreader` / `neverreader`, printed by
`seed.sh`. The row id is part of the client id and changes if you recreate the
volume, so re-run `seed.sh` and read the value it prints.

**This does not match the app's built-in default, and that is a bug in the app
rather than a quirk of this setup.** `AuthenticationViewModel` hardcodes
`WALLABAG_CLIENT_ID = "wallabag"`, on the assumption that self-hosted instances
have Wallabag's own public client registered. They do not, and it could not work
if they did: Wallabag resolves a client by the public id `"<row id>_<random
id>"`, and

```php
// friendsofsymfony/oauth-server-bundle, Model/ClientManager.php
public function findClientByPublicId($publicId)
{
    if (false === $pos = mb_strpos($publicId, '_')) {
        return null;
    }
    ...
```

A client id with no underscore returns null before any lookup, so `wallabag` can
never match a client. `AuthenticationViewModel` already has
`loginWallabagWithClient` for a custom client, but nothing in the UI calls it, so
Wallabag sign-in cannot succeed as the app stands. To test Wallabag, point
`WALLABAG_CLIENT_ID` at the value `seed.sh` prints.

## Readeck

The image is the official one, `ghcr.io/readeck/readeck:latest`. Note that
ghcr.io requires no credentials for it, but it is not a Docker Hub image, so a
registry mirror that only carries Docker Hub will not have it.

Content is inserted with SQL rather than through the API. Readeck's tokens are
signed rather than stored, so a token cannot simply be written into the
database, and its `import` subcommand wants a full export archive. The INSERTs
target the `bookmark` table and resolve the user by username, so they do not
assume any particular row id. They are written against Readeck's own
`internal/db/migrations/postgres/schema.sql`; if you move to a version that
renames columns, check that file against the first comment block in the
generated SQL.

Readeck is AGPL-3.0. That does not affect running it locally, but it is worth
knowing before you go looking for its source and reuse any of it.

## Signing in as the app

Readeck uses the OAuth device flow, so the app registers its own client and then
waits while you approve it in a browser. Open the verification URL it shows and
log in as `demo` / `password`. No client needs seeding, because the app registers
one itself.

Wallabag uses the password grant, so it does need the client above.

## Tearing down

```
docker compose down       # keep the data
docker compose down -v    # delete it, including all articles
```
