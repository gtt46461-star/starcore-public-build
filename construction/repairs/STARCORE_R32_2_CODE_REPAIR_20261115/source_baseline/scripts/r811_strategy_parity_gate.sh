#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASELINE_COMMIT=${R811_BASELINE_COMMIT:-d49f28e86de15418db3f936cee763cac472aeff0}
CASE_COUNT=${R811_GOLDEN_CASES:-100}
EVIDENCE_DIR=${R811_EVIDENCE_DIR:-"$ROOT/build/r811-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r811-parity.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() { echo "R811_STRATEGY_PARITY=FAIL|$1" >&2; exit 1; }

ANDROID_JAR=${STARCORE_ANDROID_JAR:-}
if [ -z "$ANDROID_JAR" ]; then
    for candidate in \
        "$ROOT/TOOLCHAIN_USED/android-platform-36/android.jar" \
        "$ROOT/../r367_package/Starcore-Tianshu-V57.15.3-R3.6.7-Full-Blood-Repair/TOOLCHAIN_USED/android-platform-36/android.jar" \
        "$ROOT/../toolchain/android-platform-36/android.jar"
    do
        if [ -f "$candidate" ]; then ANDROID_JAR=$candidate; break; fi
    done
fi
[ -f "$ANDROID_JAR" ] || fail OFFICIAL_ANDROID_36_JAR_MISSING
git -C "$ROOT" cat-file -e "$BASELINE_COMMIT^{commit}" 2>/dev/null \
    || fail "BASELINE_COMMIT_MISSING:$BASELINE_COMMIT"

mkdir -p "$TMP/baseline" "$TMP/current" "$TMP/baseline-classes" "$TMP/current-classes"
git -C "$ROOT" archive "$BASELINE_COMMIT" | tar -x -C "$TMP/baseline"
mkdir -p "$TMP/baseline/verification/r811"
cp "$ROOT/verification/r811/R811StrategyGoldenHarness.java" \
    "$TMP/baseline/verification/r811/R811StrategyGoldenHarness.java"

compile_tree() {
    tree=$1
    classes=$2
    find "$tree/app/src/main/java" -type f -name '*.java' -print | sort > "$classes.sources"
    java -m jdk.compiler/com.sun.tools.javac.Main \
        -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID_JAR" -d "$classes" \
        @"$classes.sources" \
        "$tree/verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java" \
        "$tree/verification/r36-stubs/com/ethk3/novaonepro/R.java" \
        "$tree/verification/r811/R811StrategyGoldenHarness.java" \
        >"$classes.javac.log" 2>&1 \
        || { sed -n '1,240p' "$classes.javac.log" >&2; fail "COMPILE:$(basename "$classes")"; }
}

compile_tree "$TMP/baseline" "$TMP/baseline-classes"
compile_tree "$ROOT" "$TMP/current-classes"

java -cp "$TMP/baseline-classes" com.ethk3.novaonepro.R811StrategyGoldenHarness \
    "$CASE_COUNT" > "$TMP/baseline.golden"
java -cp "$TMP/current-classes" com.ethk3.novaonepro.R811StrategyGoldenHarness \
    "$CASE_COUNT" > "$TMP/current.golden"

baseline_rows=$(wc -l < "$TMP/baseline.golden" | tr -d ' ')
current_rows=$(wc -l < "$TMP/current.golden" | tr -d ' ')
[ "$baseline_rows" = "$CASE_COUNT" ] || fail "BASELINE_ROWS:$baseline_rows"
[ "$current_rows" = "$CASE_COUNT" ] || fail "CURRENT_ROWS:$current_rows"
awk -F'|' 'NF != 11 { exit 1 }' "$TMP/baseline.golden" \
    || fail BASELINE_FIELD_COUNT
awk -F'|' 'NF != 11 { exit 1 }' "$TMP/current.golden" \
    || fail CURRENT_FIELD_COUNT
if ! cmp -s "$TMP/baseline.golden" "$TMP/current.golden"; then
    diff -u "$TMP/baseline.golden" "$TMP/current.golden" | sed -n '1,160p' >&2 || true
    fail BUILD_FAIL_STRATEGY_DRIFT
fi

mkdir -p "$EVIDENCE_DIR"
cp "$TMP/baseline.golden" "$EVIDENCE_DIR/R811_STRATEGY_GOLDEN_BASELINE.txt"
cp "$TMP/current.golden" "$EVIDENCE_DIR/R811_STRATEGY_GOLDEN_CURRENT.txt"
cp "$TMP/baseline-classes.javac.log" "$EVIDENCE_DIR/R811_BASELINE_JAVAC.log"
cp "$TMP/current-classes.javac.log" "$EVIDENCE_DIR/R811_CURRENT_JAVAC.log"
golden_sha=$(sha256sum "$TMP/current.golden" | awk '{print $1}')
android_sha=$(sha256sum "$ANDROID_JAR" | awk '{print $1}')
{
    echo "R811_STRATEGY_PARITY=PASS"
    echo "BASELINE_COMMIT=$BASELINE_COMMIT"
    echo "CASES=$CASE_COUNT|FIELDS_PER_CASE=11|EXACT_BYTE_COMPARE=PASS"
    echo "FIELDS=targetIssue,historyHash,featureContractHash,stateHash,numberMatrixChecksum,actionUniverseHash,rankOrderHash,championChecksum,selectedContractHash,authorizationAction,rejectCode"
    echo "GOLDEN_SHA256=$golden_sha"
    echo "ANDROID_36_JAR_SHA256=$android_sha"
} | tee "$EVIDENCE_DIR/R811_STRATEGY_PARITY_SUMMARY.txt"
