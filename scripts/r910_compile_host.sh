#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"/mnt/data/r908_work/TOOLCHAIN_USED"}
ANDROID=${STARCORE_ANDROID_JAR:-"$TC/android-platform-36/android.jar"}
OUT=${R910_EVIDENCE_DIR:-"$ROOT/build/r910-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/r910-compile.XXXXXX"); trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/classes" "$TMP/gen/com/ethk3/novaonepro" "$OUT"
VN=$(sed -n 's/^versionName=//p' "$ROOT/release/release_identity.properties")
VC=$(sed -n 's/^versionCode=//p' "$ROOT/release/release_identity.properties")
cat > "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java" <<EOF
package com.ethk3.novaonepro; public final class BuildConfig { public static final boolean DEBUG=false; public static final String APPLICATION_ID="com.ethk3.novaonepro"; public static final String BUILD_TYPE="release"; public static final int VERSION_CODE=$VC; public static final String VERSION_NAME="$VN"; public static final String DUOQU_API_KEY_A=""; public static final String DUOQU_API_KEY_B=""; }
EOF
find "$ROOT/app/src/main/java" -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" "$TMP/gen/com/ethk3/novaonepro/BuildConfig.java" "$ROOT/verification/r36-stubs/com/ethk3/novaonepro/R.java" > "$OUT/R910_JAVA17_FULL_COMPILE.log" 2>&1
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "$TMP/classes:$ANDROID" -d "$TMP/classes" \
  "$ROOT/verification/r906/R906ContractHarness.java" "$ROOT/verification/r906/R906FormulaCoverageHarness.java" \
  "$ROOT/verification/r906/R906MethodParityHarness.java" "$ROOT/verification/r908/R908AlgorithmGenomeHarness.java" \
  "$ROOT/verification/r910/R910TinyDeepHarness.java" >> "$OUT/R910_JAVA17_FULL_COMPILE.log" 2>&1
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R906ContractHarness > "$OUT/R910_R906_CONTRACT_HARNESS.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R906FormulaCoverageHarness > "$OUT/R910_FORMULA_96X120.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R906MethodParityHarness > "$OUT/R910_METHOD_PARITY_JAVA.tsv"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R908AlgorithmGenomeHarness > "$OUT/R910_ALGORITHM_GENOME_HARNESS.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R910TinyDeepHarness > "$OUT/R910_TINY_DEEP_HARNESS.txt"
J=$(wc -l < "$TMP/sources.list"); C=$(find "$TMP/classes" -type f -name '*.class' | wc -l)
printf 'R910_COMPILE_HOST=PASS|VERSION=%s|VERSION_CODE=%s|JAVA=%s|CLASS=%s\n' "$VN" "$VC" "$J" "$C" | tee "$OUT/R910_COMPILE_HOST_SUMMARY.txt"
