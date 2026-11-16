#!/bin/sh
# Minimal Maven wrapper (Linux / macOS).
# Downloads the Maven version from .mvn/wrapper/maven-wrapper.properties once into
# ~/.m2/wrapper/dists, then runs it. Only Java 17+ is required on the machine.
set -e
BASE_DIR=$(cd "$(dirname "$0")" && pwd)
DIST_URL=$(sed -n 's/^distributionUrl=//p' "$BASE_DIR/.mvn/wrapper/maven-wrapper.properties" | tr -d '\r')
NAME=$(basename "$DIST_URL" -bin.zip)
DEST="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/$NAME"

if [ ! -f "$DEST/bin/mvn" ]; then
  echo "Downloading $DIST_URL"
  TMP=$(mktemp -d)
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$TMP/maven.zip" "$DIST_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$TMP/maven.zip" "$DIST_URL"
  else
    echo "Error: curl or wget is required to download Maven." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$TMP/maven.zip" -d "$TMP"
  else
    (cd "$TMP" && jar xf maven.zip)
  fi
  mkdir -p "$(dirname "$DEST")"
  rm -rf "$DEST"
  mv "$TMP/$NAME" "$DEST"
  rm -rf "$TMP"
fi
chmod +x "$DEST/bin/mvn" 2>/dev/null || true
exec "$DEST/bin/mvn" "$@"
