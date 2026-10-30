#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/large-rows-host}
mkdir -p "$OUT"
LARGE_ROWS_TMP=$(mktemp -d)
trap 'rm -rf "$LARGE_ROWS_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$LARGE_ROWS_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_HOME/platforms/android-36/android.jar" -d "$LARGE_ROWS_TMP/production" app/src/main/java/com/ethk3/novaonepro/strategyv2/{ResearchRowsR29,MigrationCopyR29,ResearchCutoverR29,LegacyDomainUpgradeR29}.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$LARGE_ROWS_TMP/shim:$LARGE_ROWS_TMP/production:$CLASSES" -d "$LARGE_ROWS_TMP/test" tools/r29/qual_host/LargeResearchRowsR29HostTest.java
java -cp "$LARGE_ROWS_TMP/shim:$LARGE_ROWS_TMP/test:$LARGE_ROWS_TMP/production:$CLASSES:$SQLITE_JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.LargeResearchRowsR29HostTest | tee "$OUT/r29_large_rows_host.log"
