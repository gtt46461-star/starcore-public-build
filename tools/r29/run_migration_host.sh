#!/usr/bin/env bash
# Real SQLite staged migration failure injection. No private application databases or signing inputs.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
OUT=${OUT:-evidence/r29/migration-host}
mkdir -p "$OUT"
MIGRATION_TMP=$(mktemp -d)
trap 'rm -rf "$MIGRATION_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$MIGRATION_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$MIGRATION_TMP/shim:$CLASSES" -d "$MIGRATION_TMP/test" tools/r29/qual_host/MigrationR29HostTest.java
java -cp "$MIGRATION_TMP/shim:$MIGRATION_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.MigrationR29HostTest | tee "$OUT/r29_migration_host.log"
