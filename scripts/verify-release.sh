#!/usr/bin/env bash
# Release gate for Menu, modelled on readwise-review's.
#
# Menu is the only route to every app hidden from the LightOS toolbox, so an
# APK signed by the wrong key would quietly take the whole shelf with it.
# Three checks, all fatal:
#   1. the signer is the expected certificate
#   2. the build is not debuggable
#   3. backups are off
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
PIN_FILE="$(dirname "$0")/release-cert-sha256.txt"

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$(ls -1 "$SDK/build-tools" | sort -V | tail -1)"
APKSIGNER="$SDK/build-tools/$BT/apksigner"
AAPT2="$SDK/build-tools/$BT/aapt2"

[ -f "$APK" ] || { echo "✗ no APK at $APK"; exit 1; }

fail() { echo "✗ $1"; exit 1; }

# 1. signer identity
actual="$("$APKSIGNER" verify --print-certs "$APK" \
    | grep -m1 'SHA-256 digest' | awk '{print $NF}')"
if [ -f "$PIN_FILE" ]; then
    expected="$(tr -d '[:space:]' < "$PIN_FILE")"
    [ "$actual" = "$expected" ] || fail "signer mismatch
    expected $expected
    got      $actual"
    echo "✓ signer matches pin"
else
    echo "! no pin recorded yet — writing $PIN_FILE"
    echo "$actual" > "$PIN_FILE"
    echo "✓ pinned $actual"
fi

# 2. not debuggable
badging="$("$AAPT2" dump badging "$APK")"
echo "$badging" | grep -q "application-debuggable" \
    && fail "APK is debuggable" || echo "✓ not debuggable"

# 3. backups off
manifest="$("$AAPT2" dump xmltree --file AndroidManifest.xml "$APK")"
echo "$manifest" | grep -q 'allowBackup.*=false' \
    || fail "allowBackup is not false"
echo "✓ allowBackup=false"

echo "$APK looks releasable."
