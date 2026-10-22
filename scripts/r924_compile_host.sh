#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../09_TOOLCHAIN_USED"}
ANDROID="$TC/android-platform-36/android.jar"
OUT=${R924_EVIDENCE_DIR:-"$ROOT/build/r923-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r923-host.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT" "$TMP/classes"
cd "$ROOT"
VN=$(sed -n 's/^versionName=//p' release/release_identity.properties)
VC=$(sed -n 's/^versionCode=//p' release/release_identity.properties)
test "$VN" = V57.21.0-R5.4.0-924
test "$VC" = 577250924
test -f "$ANDROID"
find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    verification/r36-stubs/com/ethk3/novaonepro/R.java \
    > "$OUT/R924_JAVA17_FULL_COMPILE.log" 2>&1
HARNESSES=(
    r906/R906ContractHarness r906/R906FormulaCoverageHarness r906/R906MethodParityHarness
    r908/R908AlgorithmGenomeHarness r910/R910TinyDeepHarness r912/R912AlgorithmEvidenceHarness
    r914/R914RuntimeContractHarness r915/R915RuntimeContractHarness
    r36/R36HostContractHarness
    r924/R924LegacyRuntimeContractHarness r923/R923RegimeEvidenceHarness r924/R924RuntimeRepairHarness r924/R924SustainedHarness
)
for harness in "${HARNESSES[@]}"; do
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
        -cp "$ANDROID:$TMP/classes" -d "$TMP/classes" "verification/$harness.java" \
        >> "$OUT/R924_JAVA17_FULL_COMPILE.log" 2>&1
    if [[ "$harness" = r36/R36HostContractHarness ]]; then continue; fi
    java -cp "$TMP/classes:$ANDROID" "com.ethk3.novaonepro.${harness##*/}" \
        > "$OUT/${harness##*/}.txt"
done
J=$(wc -l < "$TMP/sources.list")
C=$(find "$TMP/classes" -name '*.class' | wc -l)
printf 'R924_COMPILE_HOST=PASS|VERSION=%s|VERSION_CODE=%s|JAVA=%s|CLASS=%s|HARNESSES=%s\n' \
    "$VN" "$VC" "$J" "$C" "${#HARNESSES[@]}" | tee "$OUT/R924_COMPILE_HOST_SUMMARY.txt"
