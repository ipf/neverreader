# Local backends for development

Readeck and Wallabag in Docker, with a demo user and dummy articles, for
exercising the app against real servers instead of mocks.

```
cp .env.example .env        # without this, `up` starts nothing - see below
docker compose up -d        # both
./seed.sh                   # demo user + 20 articles in each
```

The `.env` is not optional. Both halves sit behind a compose profile so either
can run alone, which means a bare `docker compose up` selects no services at
all and exits quietly. `.env.example` sets `COMPOSE_PROFILES=readeck,wallabag`,
which is what makes the plain command above work.

Then point the app at them:

| | URL | Sign-in |
|---|---|---|
| Readeck | `http://localhost:8000` | pick Readeck, tap Authorize, log in as `demo` / `password` and approve in the browser |
| Wallabag | `http://localhost:8080` | pick Wallabag, then `demo` / `password` **plus the client ID and secret** `seed.sh` prints |

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

`seed.sh` is safe to re-run: the user is created only if missing and the OAuth
client is reused if it exists. The article load is idempotent for Readeck,
which clears its table first; for Wallabag the import endpoint dedupes by URL,
so a re-run does not multiply entries but it is not a reset either. Use
`docker compose down -v` if you want a genuinely clean slate.

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

The seeded OAuth client id and secret are printed by `seed.sh` when it creates
the client, and read back out of the database by `smoke-test.sh`. The row id is
part of the client id (`3_neverreader`) and changes if you recreate the volume,
so take the value from `seed.sh` rather than assuming it.

The setup screen asks for both, under the Wallabag tab. **There is no default,
and there cannot be one.** Wallabag resolves a client by the public id
`"<row id>_<random id>"`, and

```php
// friendsofsymfony/oauth-server-bundle, Model/ClientManager.php
public function findClientByPublicId($publicId)
{
    if (false === $pos = mb_strpos($publicId, '_')) {
        return null;
    }
    ...
```

returns null for any id without an underscore, before it looks at the database
at all. A self-hosted instance has no Wallabag-owned client to fall back on, so
the only credentials that can work are the ones from that instance's own
Settings → API clients page.

The app used to send a hardcoded `"wallabag"`, which that lookup can never match.
It is why sign-in could not succeed at all, and why `smoke-test.sh` has a check
that the seeded client really does authenticate and a bare name really does not.
`WALLABAG_CLIENT_SECRET` overrides the secret `smoke-test.sh` uses; the app takes
whatever is typed into the form.

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

Readeck uses the OAuth authorization-code flow, so the app registers its own
client and sends you to the instance's authorization page in the browser. Sign in
there as `demo` / `password` and approve, and the browser redirects straight back
into the app - there is no code to copy and nothing left to do. No client needs
seeding, because the app registers one itself.

That redirect is `com.neverreader.app://oauth-callback`, a custom scheme. PKCE
means an app that intercepts the redirect gets a code it cannot redeem, and
verified https App Links are not an option because the app does not control the
domain your instance is on.

If the device has no browser to redirect, "Sign in with a code instead" runs the
device-code flow, where the app polls while you approve in a browser.

Wallabag uses the password grant, so it does need the client above, pasted into
the setup screen's Wallabag tab.

## Tearing down

```
docker compose down       # keep the data
docker compose down -v    # delete it, including all articles
```
