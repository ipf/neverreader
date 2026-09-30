#!/usr/bin/env bash
#
# Generate a release signing key and the GitHub secrets that CI needs to use it.
#
#   ./scripts/make-signing-secrets.sh
#
# Run this once. It writes the key to gitignored-release/, and prints the four
# `gh secret set` commands. It does not create the secrets itself, because that
# would mean typing a password into a terminal argument twice, and the key is
# the one thing in this repository that must never be committed.
#
# The key you generate here is the app's identity. Lose it and you cannot ship
# an update over an existing install; back it up somewhere that is not this
# machine, and do not regenerate it casually.

set -euo pipefail

cd "$(dirname "$0")/.."

OUT="release"
ALIAS="neverreader"
VALIDITY_DAYS=10000

if [ -f "$OUT/$ALIAS.jks" ]; then
    echo "A key already exists at $OUT/$ALIAS.jks."
    echo "Reusing it. Delete it first if you really mean to make a new one."
    echo
else
    mkdir -p "$OUT"
    chmod 700 "$OUT"
    echo "Generating a new $VALIDITY_DAYS-day key in $OUT/$ALIAS.jks ..."
    read -rsp "Store password (leave blank to generate one): " STORE_PASSWORD; echo
    if [ -z "$STORE_PASSWORD" ]; then
        STORE_PASSWORD="$(head -c 24 /dev/urandom | base64 | tr -d '/+=' | head -c 24)"
        echo "Generated a store password for you."
    fi
    keytool -genkeypair -v \
        -keystore "$OUT/$ALIAS.jks" \
        -alias "$ALIAS" \
        -keyalg RSA -keysize 4096 -validity "$VALIDITY_DAYS" \
        -storepass "$STORE_PASSWORD" -keypass "$STORE_PASSWORD" \
        -dname "CN=NeverReader, OU=NeverReader, O=NeverReader, L=-, ST=-, C=DE"
    echo
    printf '%s' "$STORE_PASSWORD" > "$OUT/$ALIAS.password"
    chmod 600 "$OUT/$ALIAS.password"
    echo "Password also written to $OUT/$ALIAS.password"
    echo
fi

KEYSTORE_B64="$(base64 -w0 < "$OUT/$ALIAS.jks")"
PASSWORD="$(cat "$OUT/$ALIAS.password")"

cat <<EOF

The key is gitignored, so it will not be committed. Nothing below is a real
secret either: these are the four secrets the workflow reads, and the values
come from the files above.

Run these with the key handy. The first is large; pipe it rather than pasting.

  gh secret set ANDROID_SIGNING_storeFile     --body "$OUT/$ALIAS.jks"
  gh secret set ANDROID_SIGNING_storePassword --body "\$PASSWORD"
  gh secret set ANDROID_SIGNING_keyAlias      --body "$ALIAS"
  gh secret set ANDROID_SIGNING_keyPassword   --body "\$PASSWORD"
  gh secret set ANDROID_SIGNING_KEYSTORE_B64  --body "\$KEYSTORE_B64"

Note the mismatch, which is deliberate: the build reads the four
ANDROID_SIGNING_* values, but the first one has to be a path that exists in the
checkout, so CI reconstructs the keystore from ANDROID_SIGNING_KEYSTORE_B64
first and then points storeFile at it. See .github/workflows/build-apk.yml.

To build signed locally instead of in CI, put this in ~/.gradle/gradle.properties:

  android.signing.storeFile=$PWD/$OUT/$ALIAS.jks
  android.signing.storePassword=...
  android.signing.keyAlias=$ALIAS
  android.signing.keyPassword=...

EOF
