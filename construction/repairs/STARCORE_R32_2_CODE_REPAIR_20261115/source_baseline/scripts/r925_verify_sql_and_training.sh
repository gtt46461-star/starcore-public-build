#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../08_TOOLCHAIN"}
ANDROID="$TC/android-platform-36/android.jar"
HOST="$ROOT/build/r925-host"
CLASSES="$ROOT/build/r925-sql-classes"
RUNTIME="$ROOT/build/r925-android-runtime"
mkdir -p "$CLASSES" "$HOST" "$RUNTIME"
rm -rf "$RUNTIME"/*
unzip -q "$ANDROID" "*.class" -d "$RUNTIME"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -cp "$HOST/compiled-classes:$ANDROID" -d "$CLASSES" \
    "$ROOT/verification/r925/stubs/android/database/sqlite/SQLiteDatabase.java" \
    "$ROOT/verification/r925/stubs/com/ethk3/novaonepro/DbStore.java" \
    "$ROOT/verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java" \
    "$ROOT/verification/r925/R925SqlHarness.java"
java -cp "$CLASSES:$HOST/compiled-classes:$RUNTIME" com.ethk3.novaonepro.R925SqlHarness "$ROOT/verification/r925/sqlite_bridge.py" > "$HOST/R925_SQL_RUNTIME.log"
OPENBLAS_NUM_THREADS=1 python "$ROOT/verification/r925/verify_training.py" > "$HOST/R925_TRAINING_TEST.log" 2>&1
OPENBLAS_NUM_THREADS=1 python "$ROOT/verification/r925/verify_parity.py" "$ANDROID" > "$HOST/R925_PARITY_TEST.log" 2>&1
printf 'R925_SQL_TRAINING_PARITY=PASS\n'
