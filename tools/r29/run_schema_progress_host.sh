#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/schema-progress-host}
mkdir -p "$OUT"
SCHEMA_PROGRESS_TMP=$(mktemp -d)
trap 'rm -rf "$SCHEMA_PROGRESS_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$SCHEMA_PROGRESS_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$SCHEMA_PROGRESS_TMP/shim:$CLASSES" -d "$SCHEMA_PROGRESS_TMP/test" tools/r29/qual_host/ResearchSchemaProgressR29HostTest.java
java -cp "$SCHEMA_PROGRESS_TMP/shim:$SCHEMA_PROGRESS_TMP/test:$CLASSES:$SQLITE_JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.ResearchSchemaProgressR29HostTest | tee "$OUT/r29_schema_progress_host.log"
