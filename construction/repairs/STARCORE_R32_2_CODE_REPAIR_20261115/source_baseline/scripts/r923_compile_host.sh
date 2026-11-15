#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../09_TOOLCHAIN_USED"}
ANDROID="$TC/android-platform-36/android.jar"
OUT=${R923_EVIDENCE_DIR:-"$ROOT/build/r923-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r923-host.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT" "$TMP/classes"
cd "$ROOT"
VN=$(sed -n 's/^versionName=//p' release/release_identity.properties)
VC=$(sed -n 's/^versionCode=//p' release/release_identity.properties)
test "$VN" = V57.20.9-R5.3.9-923
test "$VC" = 577240923
test -f "$ANDROID"
find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    verification/r36-stubs/com/ethk3/novaonepro/R.java \
    > "$OUT/R923_JAVA17_FULL_COMPILE.log" 2>&1
HARNESSES=(
    r906/R906ContractHarness r906/R906FormulaCoverageHarness r906/R906MethodParityHarness
    r908/R908AlgorithmGenomeHarness r910/R910TinyDeepHarness r912/R912AlgorithmEvidenceHarness
    r914/R914RuntimeContractHarness r915/R915RuntimeContractHarness
    r923/R923LegacyRuntimeContractHarness r923/R923RegimeEvidenceHarness r923/R923RuntimeRepairHarness
)
for harness in "${HARNESSES[@]}"; do
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
        -cp "$ANDROID:$TMP/classes" -d "$TMP/classes" "verification/$harness.java" \
        >> "$OUT/R923_JAVA17_FULL_COMPILE.log" 2>&1
    java -cp "$TMP/classes:$ANDROID" "com.ethk3.novaonepro.${harness##*/}" \
        > "$OUT/${harness##*/}.txt"
done
J=$(wc -l < "$TMP/sources.list")
C=$(find "$TMP/classes" -name '*.class' | wc -l)
printf 'R923_COMPILE_HOST=PASS|VERSION=%s|VERSION_CODE=%s|JAVA=%s|CLASS=%s|HARNESSES=%s\n' \
    "$VN" "$VC" "$J" "$C" "${#HARNESSES[@]}" | tee "$OUT/R923_COMPILE_HOST_SUMMARY.txt"
