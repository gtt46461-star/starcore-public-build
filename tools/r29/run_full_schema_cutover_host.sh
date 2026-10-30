#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/full-schema-cutover-host}
mkdir -p "$OUT"
FULL_TMP=$(mktemp -d)
trap 'rm -rf "$FULL_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$FULL_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$FULL_TMP/shim:$CLASSES" -d "$FULL_TMP/test" tools/r29/qual_host/FullSchemaCutoverR29HostTest.java
java -Xmx768m -cp "$FULL_TMP/shim:$FULL_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.FullSchemaCutoverR29HostTest | tee "$OUT/r29_full_schema_cutover_host.log"
# Actual immutable implementation from the failing API34 run, with the identical schema/protocol/hash/query
# assertions above. This is a historical-source negative control, not a test-only production switch.
mkdir -p "$FULL_TMP/old-source" "$FULL_TMP/old-classes"
git show '1388a63c3e3ac9ce512d19399a03c9f68e9bb092:app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRowsR29.java' > "$FULL_TMP/old-source/ResearchRowsR29.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_HOME/platforms/android-36/android.jar" -d "$FULL_TMP/old-classes" "$FULL_TMP/old-source/ResearchRowsR29.java"
set +e
java -Xmx768m -cp "$FULL_TMP/shim:$FULL_TMP/test:$FULL_TMP/old-classes:$CLASSES:$SQLITE_JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.FullSchemaCutoverR29HostTest > "$OUT/old-1388a63-control.log" 2>&1
FULL_CONTROL_STATUS=$?
set -e
if [ "$FULL_CONTROL_STATUS" -eq 0 ] || ! rg -q 'F4_4200_DRAW_COPY_DOES_NOT_ISSUE_PER_SMALL_CELL_SQL\|FAILED' "$OUT/old-1388a63-control.log"; then
  cat "$OUT/old-1388a63-control.log"
  exit 1
fi
echo "R29_FULL_SCHEMA_CUTOVER|OLD_1388_IMPLEMENTATION_REJECTED|exit=$FULL_CONTROL_STATUS"
