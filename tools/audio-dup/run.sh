#!/usr/bin/env bash
# Compiles Poc.java to dex, pushes it to the device and runs it as shell.
# Usage: ./run.sh status|set|clear
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ANDROID_JAR="$(ls -d "$SDK"/platforms/android-*/android.jar | sort -V | tail -1)"
D8="$(ls -d "$SDK"/build-tools/*/d8 | sort -V | tail -1)"
ADB="$SDK/platform-tools/adb"

DIR="$(cd "$(dirname "$0")" && pwd)"
ROUTER="$DIR/../../app/src/main/java/com/byxnd/dualaudio/routing/AudioRouter.java"
OUT="$DIR/build"
rm -rf "$OUT" && mkdir -p "$OUT/classes"

# AudioRouter.java is the same file the app (Shizuku version) uses
javac --release 17 -cp "$ANDROID_JAR" -d "$OUT/classes" "$DIR/src/Poc.java" "$ROUTER"
"$D8" --min-api 31 --lib "$ANDROID_JAR" --output "$OUT" $(find "$OUT/classes" -name '*.class')
mv "$OUT/classes.dex" "$OUT/audio-dup.dex"

"$ADB" push "$OUT/audio-dup.dex" /data/local/tmp/audio-dup.dex >/dev/null
"$ADB" shell CLASSPATH=/data/local/tmp/audio-dup.dex app_process / Poc "${1:-status}"
