#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/schema-ddl-proof-host}
mkdir -p "$OUT"
DDL_TMP=$(mktemp -d)
trap 'rm -rf "$DDL_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$DDL_TMP/shim" tools/r29/scoped_schema_host/shim/android/database/sqlite/SQLiteDatabase.java tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$DDL_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/StrategySchemaSU1.java app/src/main/java/com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$DDL_TMP/shim:$DDL_TMP/product:$CLASSES:$ANDROID_JAR" -d "$DDL_TMP/test" tools/r29/qual_host/FullSchemaCutoverR29HostTest.java tools/r29/qual_host/SchemaDdlProofR29HostTest.java
java -Xmx768m -cp "$DDL_TMP/shim:$DDL_TMP/test:$DDL_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.SchemaDdlProofR29HostTest | tee "$OUT/r29_schema_ddl_proof_host.log"
mkdir -p "$DDL_TMP/old-source" "$DDL_TMP/old-product"
for name in strategyv2/StrategySchemaSU1 r952/ResearchMigrationRegistryR952; do git show "0c639ccb:app/src/main/java/com/ethk3/novaonepro/$name.java" > "$DDL_TMP/old-source/$(basename "$name").java"; done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$DDL_TMP/old-product" "$DDL_TMP/old-source/StrategySchemaSU1.java" "$DDL_TMP/old-source/ResearchMigrationRegistryR952.java"
set +e
java -Xmx768m -cp "$DDL_TMP/shim:$DDL_TMP/test:$DDL_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.SchemaDdlProofR29HostTest > "$OUT/old-0c639-schema-ddl-control.log" 2>&1
DDL_CONTROL_STATUS=$?
set -e
if [ "$DDL_CONTROL_STATUS" -eq 0 ] || ! grep -Fq 'D2_MISSING_JOURNAL_INSTALLS_ONLY_ORIGINAL_TABLE_AND_THREE_GUARDS|FAILED' "$OUT/old-0c639-schema-ddl-control.log"; then cat "$OUT/old-0c639-schema-ddl-control.log"; exit 1; fi
echo "R29_SCHEMA_DDL_PROOF|OLD_REPEATED_DDL_REJECTED|exit=$DDL_CONTROL_STATUS"

set +e
java -Xmx768m -cp "$DDL_TMP/shim:$DDL_TMP/test:$DDL_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.SchemaDdlProofR29HostTest --cold-ready-control > "$OUT/old-0c639-cold-ready-control.log" 2>&1
DDL_COLD_STATUS=$?
set -e
if [ "$DDL_COLD_STATUS" -eq 0 ] || ! grep -Fq 'D6_READY_PROOF_FRESH_BULK_CENSUS|FAILED' "$OUT/old-0c639-cold-ready-control.log"; then cat "$OUT/old-0c639-cold-ready-control.log"; exit 1; fi
echo "R29_SCHEMA_DDL_PROOF|OLD_PER_OBJECT_QUERY_AMPLIFICATION_REJECTED|exit=$DDL_COLD_STATUS"
