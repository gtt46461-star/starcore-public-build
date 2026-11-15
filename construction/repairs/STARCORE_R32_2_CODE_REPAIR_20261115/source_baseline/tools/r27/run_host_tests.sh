#!/usr/bin/env bash
# R27 HOST checks that are not JUnit tests (JUnit H01–H17 + R27-056 run under ./gradlew testDebugUnitTest).
#   H18        tools/r27_null_baseline.py on the repository CSV vs the published results (±1pp, fixed seed)
#   MIGRATION  tools/r27/test_r27_migration.py (additive upgrade from a 1713d11 database + H15 SQL half)
# Env: CLASSES = compiled app classes dir; ANDROID_JAR = android.jar (only for the SQL dump); OUT = log dir.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd); cd "$ROOT"
OUT=${OUT:-evidence/r27/host}; mkdir -p "$OUT"
PLAN=construction/plans/STARCORE_R27_FINAL_20261028
rc=0
echo "== H18 null baseline"
python3 -I $PLAN/tools/r27_null_baseline.py research_data/ethpk10_draws_20260316_201059.csv "$OUT/r27_null_baseline_fresh.json" > "$OUT/h18_run.log" 2>&1; r=$?
python3 -I tools/r27/compare_null_baseline.py "$OUT/r27_null_baseline_fresh.json" $PLAN/results/r27_null_baseline_results.json | tee "$OUT/h18_compare.log"; r2=${PIPESTATUS[0]}
echo "H18 run_rc=$r compare_rc=$r2" | tee -a "$OUT/h18_compare.log"; [ $r -eq 0 ] && [ $r2 -eq 0 ] || rc=1
echo "== migration + H15 SQL"
if [ -n "${CLASSES:-}" ] && [ -n "${ANDROID_JAR:-}" ]; then
  T=$(mktemp -d); javac -nowarn -encoding UTF-8 -proc:none -cp "$ANDROID_JAR:$CLASSES" -d "$T" tools/r27/R27SqlDump.java && java -cp "$ANDROID_JAR:$CLASSES:$T" R27SqlDump > "$OUT/r27_sql_dump.json"; r=$?
  python3 -I tools/r27/test_r27_migration.py "$OUT/r27_sql_dump.json" | tee "$OUT/migration.log"; r2=${PIPESTATUS[0]}
  echo "MIGRATION dump_rc=$r test_rc=$r2" | tee -a "$OUT/migration.log"; [ $r -eq 0 ] && [ $r2 -eq 0 ] || rc=1
else echo "MIGRATION NOT_RUN: CLASSES/ANDROID_JAR not set" | tee "$OUT/migration.log"; rc=1; fi
echo "RUN_HOST_TESTS rc=$rc"; exit $rc
