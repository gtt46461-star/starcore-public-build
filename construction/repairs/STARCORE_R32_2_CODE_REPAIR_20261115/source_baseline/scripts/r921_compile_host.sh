#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
ANDROID="$TC/android-platform-36/android.jar"
OUT=${R921_EVIDENCE_DIR:-"$ROOT/build/r921-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r921-host.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT" "$TMP/classes"

VN=$(sed -n 's/^versionName=//p' "$ROOT/release/release_identity.properties")
VC=$(sed -n 's/^versionCode=//p' "$ROOT/release/release_identity.properties")
if [ "$VN" != "V57.20.7-R5.3.7-921" ] || [ "$VC" != "577240921" ]; then
    echo "R921_COMPILE_HOST=FAIL|RELEASE_IDENTITY=$VN/$VC" >&2
    exit 1
fi
[ -f "$ANDROID" ] || { echo "R921_COMPILE_HOST=FAIL|ANDROID_JAR_MISSING" >&2; exit 1; }

find "$ROOT/app/src/main/java" -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    "$ROOT/verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java" \
    "$ROOT/verification/r36-stubs/com/ethk3/novaonepro/R.java" \
    > "$OUT/R921_JAVA17_FULL_COMPILE.log" 2>&1

java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID:$TMP/classes" -d "$TMP/classes" \
    "$ROOT/verification/r906/R906ContractHarness.java" \
    "$ROOT/verification/r906/R906FormulaCoverageHarness.java" \
    "$ROOT/verification/r906/R906MethodParityHarness.java" \
    "$ROOT/verification/r908/R908AlgorithmGenomeHarness.java" \
    "$ROOT/verification/r910/R910TinyDeepHarness.java" \
    "$ROOT/verification/r912/R912AlgorithmEvidenceHarness.java" \
    "$ROOT/verification/r914/R914RuntimeContractHarness.java" \
    "$ROOT/verification/r915/R915RuntimeContractHarness.java" \
    "$ROOT/verification/r921/R921RuntimeContractHarness.java" \
    >> "$OUT/R921_JAVA17_FULL_COMPILE.log" 2>&1

java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R906ContractHarness \
    > "$OUT/R921_R906_CONTRACT_HARNESS.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R906FormulaCoverageHarness \
    > "$OUT/R921_FORMULA_96X120.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R906MethodParityHarness \
    > "$OUT/R921_METHOD_PARITY_JAVA.tsv"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R908AlgorithmGenomeHarness \
    > "$OUT/R921_ALGORITHM_GENOME_HARNESS.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R910TinyDeepHarness \
    > "$OUT/R921_TINY_DEEP_HARNESS.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R912AlgorithmEvidenceHarness \
    > "$OUT/R921_ALGORITHM_EVIDENCE_HARNESS.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R914RuntimeContractHarness \
    > "$OUT/R921_R914_RUNTIME_REGRESSION.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R915RuntimeContractHarness \
    > "$OUT/R921_R915_RUNTIME_REGRESSION.txt"
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R921RuntimeContractHarness \
    > "$OUT/R921_RUNTIME_CONTRACT_HARNESS.txt"

J=$(wc -l < "$TMP/sources.list")
C=$(find "$TMP/classes" -name '*.class' | wc -l)
printf 'R921_COMPILE_HOST=PASS|VERSION=%s|VERSION_CODE=%s|JAVA=%s|CLASS=%s\n' \
    "$VN" "$VC" "$J" "$C" | tee "$OUT/R921_COMPILE_HOST_SUMMARY.txt"
