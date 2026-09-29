#!/usr/bin/env python3
"""
Generate the dummy content for the local Readeck and Wallabag instances.

Both backends are seeded from this one definition so that the two lists in the
app hold the same articles, which makes it obvious when something is
backend-specific rather than a data problem.

Emits:
  seed/readeck-bookmarks.sql   INSERTs against Readeck's `bookmark` table
  seed/wallabag-import.json   a Wallabag export, for `wallabag:import`

Run from this directory:  python3 generate.py
"""

import json
import os
import uuid
from datetime import datetime, timedelta, timezone

HERE = os.path.dirname(os.path.abspath(__file__))

# Must match the username seed.sh passes to `readeck user`.
DEMO_USERNAME = "demo"

# Readeck stores article text in the bookmark, which is what its
# fetchArticleHtml endpoint serves. Without it the reader opens an empty page,
# so every entry here gets a body.
ARTICLES = [
    ("Readeck", "readeck.org", "Self-hosting Readeck with Docker Compose",
     "Running your own read-it-later service is a handful of containers. "
     "This walks through Postgres, the app itself, and the OAuth device flow "
     "you need for a third-party client."),
    ("Kubernetes", "kubernetes.io", "A gentle introduction to operators",
     "An operator is a controller that has been given a custom resource. "
     "The pattern is simple enough that you can write one before you fully "
     "understand the machinery underneath it."),
    ("Rust Blog", "blog.rust-lang.org", "Why we are moving to inline const expressions",
     "Inline const expressions let you pass a constant where a type was "
     "required. It is a small ergonomic win, but it removes a class of "
     "workaround that people were carrying for years."),
    ("Julia Evans", "jvns.ca", "How to write a good commit message",
     "A commit message is documentation that happens to be versioned with the "
     "code. The most useful ones explain why a change was made, because the "
     "diff already says what changed."),
    ("Martin Fowler", "martinfowler.com", "Patterns of distributed systems",
     "Circuit breaker, saga, and the two generals problem. Distributed "
     "systems fail in ways that local systems do not, and most of the patterns "
     "exist to make those failures survivable rather than impossible."),
    ("Deno", "deno.land", "Deno 2 and the case for stability",
     "Shipping a runtime means promising not to break things. Deno 2 is an "
     "attempt to hold a stable line while still moving, which is harder than "
     "it sounds."),
    ("SQLite", "sqlite.org", "Write-ahead logging in SQLite",
     "WAL mode lets readers and a writer work at the same time, which is the "
     "common reason people outgrow the default rollback journal."),
    ("F-Droid", "f-droid.org", "Reproducible builds for Android apps",
     "A reproducible build means the same source produces the same bits, so a "
     "user can verify that what they installed is what you published."),
    ("Android Developers", "developer.android.com", "Predictive back gestures",
     "Predictive back animates the app out of view as the user swipes. "
     "Opting in means handling the callback rather than the key event."),
    ("Compose Multiplatform", "jetpack.compose", "Sharing a Compose UI across platforms",
     "Compose Multiplatform reuses the same composable code for desktop and "
     "web, at the cost of a platform abstraction that is not always free."),
    ("Pro Git", "git-scm.com", "Rebasing versus merging",
     "Rebasing rewrites history to look linear, which reads well and is "
     "dangerous to share. The practical rule is to rebase private work and "
     "merge published work."),
    ("Jane Street", "occams.xyz", "Why OCaml for a trading system",
     "A few decades of running a real system is the only kind of evidence "
     "that settles a language choice. This is an account of what they learned."),
    ("Haskell", "wiki.haskell.org", "Monads as a programming pattern",
     "Once you can describe a computation as a monad you can compose it with "
     "other such computations. The abstraction pays off earlier than it "
     "first appears."),
    ("Wirecutter", "theverge.com", "How we test battery claims",
     "Manufacturer numbers are measured under conditions that do not resemble "
     "use. Running the same test across many devices is the only way to get a "
     "comparable number."),
    ("Jane Doe", "example.org", "A placeholder domain for testing",
     "This entry exists so the list has something that is obviously fake, "
     "which makes it easy to check that filtering and search behave."),
    ("Localhost", "localhost", "Testing cleartext HTTP against a LAN server",
     "The app permits cleartext because the host is chosen at runtime. This "
     "entry is here to confirm that path still works end to end."),
    ("Kotlin", "kotlinlang.org", "Coroutines are structured concurrency",
     "A coroutine launched in a scope is a child of that scope, and is "
     "cancelled with it. That is the whole idea, and it is worth the "
     "indirection."),
    ("Architecture Notes", "notes.example.com", "Local-first is not offline-first",
     "Local-first means the local copy is the source of truth and sync is "
     "eventual. Offline-first only says the app works without a network, "
     "which is a weaker claim."),
    ("Observability", "grafana.com", "Cardinality is the expensive part of metrics",
     "A metric with a user id in the label is a time series per user. That is "
     "the difference between a dashboard and a bill."),
    ("Typography", "typewolf.com", "Choosing a reading face",
     "A reading face has to survive a long paragraph at a small size. That "
     "rules out more display faces than their character counts suggest."),
]

# A few entries are pre-marked, so the list has favourites and an archive to
# show without having to tap anything. Keyed by index into ARTICLES: keying by
# url would silently stop matching the moment a title or domain is edited,
# because the urls are generated.
FAVOURITE_INDEXES = {4, 10, 17}
ARCHIVED_INDEXES = {7, 6, 8}


def slug_url(domain, index):
    return f"https://{domain}/articles/{index:02d}-entry"


def readeck_uid():
    """Readeck uids are varchar(32); a uuid without dashes is 32 chars."""
    return uuid.uuid4().hex


def build():
    now = datetime.now(timezone.utc)
    rows = []
    for i, (site, domain, title, excerpt) in enumerate(ARTICLES):
        # Spread over the last ~90 days so date sorting and the saved-date
        # label in the list have something to order.
        saved = now - timedelta(days=i * 3, hours=i)
        body = (
            f"<h1>{title}</h1>\n"
            f"<p>{excerpt}</p>\n"
            + "".join(
                f"<p>This is paragraph {p} of the seeded body. The reader "
                f"renders what the server stores, so this text exists to give "
                f"the article view something real to lay out.</p>"
                for p in range(1, 6)
            )
        )
        rows.append(
            {
                "url": slug_url(domain, i),
                "initial_url": slug_url(domain, i),
                "domain": domain,
                "title": title,
                "site": site,
                "description": excerpt,
                "text": body,
                "created": saved,
                "updated": saved,
                "is_marked": i in FAVOURITE_INDEXES,
                "is_archived": i in ARCHIVED_INDEXES,
                "duration": 3 + (i % 17),
                "word_count": 400 + (i * 37) % 2200,
                "uid": readeck_uid(),
                "labels": ["seeding"] if i % 4 == 0 else [],
            }
        )
    return rows


def write_readeck_sql(rows):
    """
    Only columns that need a value are listed; everything else takes the schema
    default. `text` is what Readeck's fetchArticleHtml serves, so the reader has
    something to render.

    Split explicitly into the columns the VALUES list supplies and the ones
    written as literals, so every selected column actually exists in the alias.
    """
    # column -> source. "v" means it comes from the VALUES list below.
    from_values = [
        ("uid", "v.uid"),
        ("user_id", "u.id"),
        ("created", "v.created::timestamptz"),
        ("updated", "v.updated::timestamptz"),
        ("is_marked", "v.is_marked"),
        ("is_archived", "v.is_archived"),
        ("url", "v.url"),
        ("initial_url", "v.initial_url"),
        ("domain", "v.domain"),
        ("title", "v.title"),
        ("site", "v.site"),
        ("site_name", "v.site_name"),
        ("description", "v.description"),
        ("note", "''::text"),
        ("text", "v.text"),
        ("word_count", "v.word_count"),
        ("duration", "v.duration"),
        ("labels", "v.labels::jsonb"),
    ]
    literals = [
        ("authors", "'[]'::jsonb"),
        ("lang", "''"),
        ("dir", "''"),
        ("type", "''"),
        ("embed", "''"),
        ("file_path", "''"),
        ("files", "'[]'::jsonb"),
        ("errors", "'[]'::jsonb"),
        ("read_progress", "0"),
        ("read_anchor", "''"),
        ("annotations", "'[]'::jsonb"),
        ("links", "'[]'::jsonb"),
    ]
    # `state` is NOT NULL DEFAULT 0, and published is nullable: both are left out.

    cols = [c for c, _ in from_values] + [c for c, _ in literals]
    quoted = [f'"{c}"' if c == "text" else c for c in cols]

    out = [
        "-- Generated by generate.py. Do not edit by hand.",
        "--",
        f"-- Inserts into the `bookmark` table. user_id is resolved by username",
        f"-- ({DEMO_USERNAME!r}), which seed.sh creates first with `readeck user`,",
        "-- so this does not depend on the row id being 1.",
        "",
        "INSERT INTO bookmark (" + ", ".join(quoted) + ")",
        "SELECT",
        ",\n".join("    " + expr for _, expr in from_values + literals),
    ]

    out.append("FROM (VALUES")
    values = []
    for r in rows:
        values.append(
            "    ({uid}, {created}, {updated}, {marked}, {archived}, {url}, "
            "{initial_url}, {domain}, {title}, {site}, {site}, {description}, "
            "{note}, {text}, {word_count}, {duration}, {labels})".format(
                uid=sql_str(r["uid"]),
                created=sql_ts(r["created"]),
                updated=sql_ts(r["updated"]),
                marked="true" if r["is_marked"] else "false",
                archived="true" if r["is_archived"] else "false",
                url=sql_str(r["url"]),
                initial_url=sql_str(r["initial_url"]),
                domain=sql_str(r["domain"]),
                title=sql_str(r["title"]),
                site=sql_str(r["site"]),
                description=sql_str(r["description"]),
                note=sql_str(r["description"]),
                text=sql_str(r["text"]),
                word_count=r["word_count"],
                duration=r["duration"],
                labels=sql_str(json.dumps(r["labels"])),
            )
        )
    out.append(",\n".join(values))
    out.append(") AS v(uid, created, updated, is_marked, is_archived, url, "
               "initial_url, domain, title, site, site_name, description, "
               "note, text, word_count, duration, labels)")
    out.append('CROSS JOIN "user" u')
    out.append("WHERE u.username = " + sql_str(DEMO_USERNAME))
    out.append(";")
    out.append("")
    out.append(f"-- {len(rows)} bookmarks.")
    path = os.path.join(HERE, "readeck-bookmarks.sql")
    with open(path, "w") as f:
        f.write("\n".join(out) + "\n")
    return path


def sql_str(v):
    return "'" + str(v).replace("'", "''") + "'"


def sql_ts(dt):
    return sql_str(dt.strftime("%Y-%m-%d %H:%M:%S+00"))


def write_wallabag_json(rows):
    """
    Wallabag's import format, for `wallabag:import --importer=wallabag`.

    Three things the format is picky about, all learned from it rejecting a
    file, and all from WallabagV2Import::prepareEntry:
      * the top level must be a flat array of entries, not an object with an
        "entries" key - parseEntries() iterates whatever the top level is;
      * the starred flag is "is_starred", not "favorite";
      * "content" and "mimetype" are required, and is_starred/is_archived are
        NOT NULL in the database, so they must be present and non-null.
    """
    entries = []
    for r in rows:
        entries.append({
            "url": r["url"],
            "title": r["title"],
            # The body is what the reader will show, so it carries the same
            # seeded text Readeck gets.
            "content": r["text"],
            "mimetype": "text/html",
            "tags": list(r["labels"]),
            "is_archived": bool(r["is_archived"]),
            "is_starred": bool(r["is_marked"]),
            "created_at": r["created"].strftime("%Y-%m-%dT%H:%M:%S+00:00"),
            "authors": r["site"],
            "language": "en",
            "published_at": None,
            "reading_time": r["duration"],
        })
    path = os.path.join(HERE, "wallabag-import.json")
    with open(path, "w") as f:
        json.dump(entries, f, indent=2)
    return path


def main():
    rows = build()
    a = write_readeck_sql(rows)
    b = write_wallabag_json(rows)
    print(f"{len(rows)} articles")
    print(f"  {a}")
    print(f"  {b}")


if __name__ == "__main__":
    main()
