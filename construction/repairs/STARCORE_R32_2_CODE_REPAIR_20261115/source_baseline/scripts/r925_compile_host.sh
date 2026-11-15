#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../08_TOOLCHAIN"}
ANDROID=${ANDROID_PLATFORM_JAR:-}
if [[ -z "$ANDROID" && -f "$TC/android-platform-36/android.jar" ]]; then
    ANDROID="$TC/android-platform-36/android.jar"
fi
if [[ -z "$ANDROID" && -n "${ANDROID_SDK_ROOT:-}" && -f "$ANDROID_SDK_ROOT/platforms/android-36/android.jar" ]]; then
    ANDROID="$ANDROID_SDK_ROOT/platforms/android-36/android.jar"
fi
if [[ -z "$ANDROID" && -n "${ANDROID_HOME:-}" && -f "$ANDROID_HOME/platforms/android-36/android.jar" ]]; then
    ANDROID="$ANDROID_HOME/platforms/android-36/android.jar"
fi
if [[ -z "$ANDROID" && -x "$(command -v sdkmanager 2>/dev/null || true)" ]]; then
    yes | sdkmanager "platforms;android-36" >/dev/null 2>&1 || true
    SDK_BASE=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
    if [[ -n "$SDK_BASE" && -f "$SDK_BASE/platforms/android-36/android.jar" ]]; then ANDROID="$SDK_BASE/platforms/android-36/android.jar"; fi
fi
[[ -n "$ANDROID" && -f "$ANDROID" ]] || { echo "R925_COMPILE_HOST=FAIL|ANDROID_36_JAR_MISSING" >&2; exit 41; }
OUT=${R925_EVIDENCE_DIR:-"$ROOT/build/r925-host"}
mkdir -p "$OUT"
# Separate execution sandboxes may reuse the same PID. Serialize publishers and use a genuinely
# unique staging directory, so interrupted/concurrent builds cannot nest or mix class trees.
exec 9>"$OUT/.compile-publish.lock"
flock 9
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r923-host.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
mkdir -p "$OUT" "$TMP/classes" "$TMP/android-runtime"
# Some retained platform bundles are APK-shaped ZIPs: javac can read them but the desktop
# class loader cannot. Extract only .class entries for deterministic host-harness execution.
unzip -q "$ANDROID" "*.class" -d "$TMP/android-runtime"
cd "$ROOT"
VN=$(sed -n 's/^versionName=//p' release/release_identity.properties)
VC=$(sed -n 's/^versionCode=//p' release/release_identity.properties)
python3 "$ROOT/scripts/r926_release_manifest.py" --check >/dev/null
test -n "$VN"
test -n "$VC"
test -f "$ANDROID"
find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
GENERATED_SEMANTICS=app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java
GENERATED_PROVENANCE=app/build/generated/source/r815/com/ethk3/novaonepro/BuildSourceIdentityCP12.java
RESOURCE_JAR=app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar
test -f "$GENERATED_SEMANTICS" && test -f "$GENERATED_PROVENANCE" && test -f "$RESOURCE_JAR" \
    || { echo 'R925_COMPILE_HOST=FAIL|CURRENT_ANDROID_GENERATED_INPUTS_MISSING' >&2; exit 42; }
python3 scripts/r967_semantic_identity.py --check > "$OUT/R925_JAVA17_FULL_COMPILE.log" 2>&1
printf '%s\n' "$GENERATED_SEMANTICS" "$GENERATED_PROVENANCE" >> "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID:$RESOURCE_JAR" -d "$TMP/classes" @"$TMP/sources.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    >> "$OUT/R925_JAVA17_FULL_COMPILE.log" 2>&1
mkdir -p "$TMP/host-adapters"
java -m jdk.compiler/com.sun.tools.javac.Main -d "$TMP/host-adapters" \
    verification/r960/host/android/os/SystemClock.java
HARNESSES=(
    r906/R906ContractHarness r906/R906FormulaCoverageHarness r906/R906MethodParityHarness
    r908/R908AlgorithmGenomeHarness r910/R910TinyDeepHarness r912/R912AlgorithmEvidenceHarness
    r914/R914RuntimeContractHarness r915/R915RuntimeContractHarness
    r36/R36HostContractHarness
    r924/R924LegacyRuntimeContractHarness r923/R923RegimeEvidenceHarness r924/R924RuntimeRepairHarness r924/R924SustainedHarness r925/R925PolicyHarness
    r925/Batch2ContractHarnessR926
    r929/Batch34ContractHarnessR929
)
for harness in "${HARNESSES[@]}"; do
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
        -cp "$ANDROID:$RESOURCE_JAR:$TMP/classes" -d "$TMP/classes" "verification/$harness.java" \
        >> "$OUT/R925_JAVA17_FULL_COMPILE.log" 2>&1
    java -cp "$TMP/host-adapters:$TMP/classes:$RESOURCE_JAR:$TMP/android-runtime" "com.ethk3.novaonepro.${harness##*/}" \
        > "$OUT/${harness##*/}.txt"
done
# Explicit markers prevent a harness that silently exits from being counted as a pass.
grep -Fxq 'BATCH2_CONTRACT_HARNESS_PASS' "$OUT/Batch2ContractHarnessR926.txt"
grep -Fxq 'R929_BATCH34_CONTRACT_PASS' "$OUT/Batch34ContractHarnessR929.txt"
grep -Fq 'SUMMARY|batch3=12/12|batch4=10/10|tasks=22/22|' "$OUT/Batch34ContractHarnessR929.txt"
PUBLISH=$(mktemp -d "$OUT/compiled-classes.publish.XXXXXX")
cp -R "$TMP/classes/." "$PUBLISH/"
rm -rf "$OUT/compiled-classes"
mv -T "$PUBLISH" "$OUT/compiled-classes"
J=$(wc -l < "$TMP/sources.list")
C=$(find "$TMP/classes" -name '*.class' | wc -l)
printf 'R925_COMPILE_HOST=PASS|VERSION=%s|VERSION_CODE=%s|JAVA=%s|CLASS=%s|HARNESSES=%s|BATCH2=PASS|BATCH34_CONTRACT=PASS|BATCH34_SQL=SEPARATE_REQUIRED\n' \
    "$VN" "$VC" "$J" "$C" "${#HARNESSES[@]}" | tee "$OUT/R925_COMPILE_HOST_SUMMARY.txt"
