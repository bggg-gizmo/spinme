#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK" ]]; then
  echo "Set ANDROID_SDK_ROOT (or ANDROID_HOME) to your Android SDK." >&2
  exit 1
fi

PLATFORM="$SDK/platforms/android-36/android.jar"
if [[ ! -f "$PLATFORM" ]]; then
  echo "Android SDK Platform 36 is required: $PLATFORM" >&2
  exit 1
fi

BT="${ANDROID_BUILD_TOOLS:-}"
if [[ -z "$BT" ]]; then
  BT="$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
fi
for tool in aapt d8 zipalign apksigner; do
  [[ -x "$BT/$tool" ]] || { echo "Missing $tool in $BT" >&2; exit 1; }
done
command -v javac >/dev/null || { echo "JDK with javac is required." >&2; exit 1; }
command -v zip >/dev/null || { echo "zip is required." >&2; exit 1; }

SRC="$ROOT/app/src/main"
OUT="$ROOT/build/manual-release"
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

"$BT/aapt" package -f -m \
  -M "$SRC/AndroidManifest.xml" \
  -S "$SRC/res" \
  -I "$PLATFORM" \
  -J "$OUT/gen" \
  -F "$OUT/base-unsigned.apk" \
  --min-sdk-version 29 \
  --target-sdk-version 36 \
  --version-code 3 \
  --version-name 1.0.0

find "$SRC/java" "$OUT/gen" -name '*.java' -print > "$OUT/sources.list"
javac -source 8 -target 8 -encoding UTF-8 -classpath "$PLATFORM" -d "$OUT/classes" @"$OUT/sources.list"
"$BT/d8" --min-api 29 --lib "$PLATFORM" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class' -print)
cp "$OUT/base-unsigned.apk" "$OUT/app-unsigned.apk"
(cd "$OUT/dex" && zip -q -j "$OUT/app-unsigned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$OUT/app-unsigned.apk" "$OUT/SpinMe-v1.0.0-aligned-unsigned.apk"

if [[ -n "${SPINME_KEYSTORE:-}" ]]; then
  : "${SPINME_KEY_ALIAS:?Set SPINME_KEY_ALIAS}"
  : "${SPINME_STOREPASS:?Set SPINME_STOREPASS}"
  KEY_PASS="${SPINME_KEYPASS:-$SPINME_STOREPASS}"
  "$BT/apksigner" sign \
    --ks "$SPINME_KEYSTORE" \
    --ks-key-alias "$SPINME_KEY_ALIAS" \
    --ks-pass "pass:$SPINME_STOREPASS" \
    --key-pass "pass:$KEY_PASS" \
    --out "$OUT/SpinMe-v1.0.0.apk" \
    "$OUT/SpinMe-v1.0.0-aligned-unsigned.apk"
  "$BT/apksigner" verify --verbose --print-certs "$OUT/SpinMe-v1.0.0.apk"
  sha256sum "$OUT/SpinMe-v1.0.0.apk"
else
  echo "Unsigned aligned APK: $OUT/SpinMe-v1.0.0-aligned-unsigned.apk"
fi
