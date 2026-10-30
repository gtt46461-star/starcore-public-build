#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/root-read-pool-host}
mkdir -p "$OUT"
POOL_TMP=$(mktemp -d)
trap 'rm -rf "$POOL_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$POOL_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$POOL_TMP/shim:$CLASSES" -d "$POOL_TMP/test" tools/r29/qual_host/RootReadPoolR29HostTest.java
java -cp "$POOL_TMP/shim:$POOL_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.RootReadPoolR29HostTest | tee "$OUT/positive.log"
set +e
java -cp "$POOL_TMP/shim:$POOL_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.RootReadPoolR29HostTest --shared-rw-control > "$OUT/shared-rw-control.log" 2>&1
POOL_CONTROL_STATUS=$?
set -e
if [ "$POOL_CONTROL_STATUS" -eq 0 ] || ! rg -q PRIMARY_POOL_CAPTURE_WAITED_FOR_RT_WRITER "$OUT/shared-rw-control.log"; then
  cat "$OUT/shared-rw-control.log"
  exit 1
fi
echo "R29_ROOT_READ_POOL|SHARED_RW_CONTROL_REJECTED|exit=$POOL_CONTROL_STATUS"
