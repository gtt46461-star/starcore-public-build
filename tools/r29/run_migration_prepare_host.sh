#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/migration-prepare-host}
mkdir -p "$OUT"
PREPARE_TMP=$(mktemp -d)
trap 'rm -rf "$PREPARE_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$PREPARE_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$PREPARE_TMP/shim:$CLASSES" -d "$PREPARE_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/MigrationCopyR29.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$PREPARE_TMP/shim:$PREPARE_TMP/product:$CLASSES" -d "$PREPARE_TMP/test" tools/r29/qual_host/MigrationPrepareR29HostTest.java
java -cp "$PREPARE_TMP/shim:$PREPARE_TMP/product:$PREPARE_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.MigrationPrepareR29HostTest | tee "$OUT/positive.log"
set +e
java -cp "$PREPARE_TMP/shim:$PREPARE_TMP/product:$PREPARE_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.MigrationPrepareR29HostTest --old-prefix-control > "$OUT/old-prefix-control.log" 2>&1
PREPARE_CONTROL_STATUS=$?
set -e
if [ "$PREPARE_CONTROL_STATUS" -eq 0 ] || ! grep -Fq P01_PREFIX_PREPARE_CANNOT_RESUME_PAST_BUSY "$OUT/old-prefix-control.log"; then
  cat "$OUT/old-prefix-control.log"
  exit 1
fi
echo "R29_MIGRATION_PREPARE|OLD_PREFIX_RESTART_REJECTED|exit=$PREPARE_CONTROL_STATUS"
