#!/usr/bin/env bash
# Actual projection installers on SQLite; old immutable source reproduces needless schema DDL.
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/projection-schema-host}
mkdir -p "$OUT"
PROJECTION_SCHEMA_TMP=$(mktemp -d)
trap 'rm -rf "$PROJECTION_SCHEMA_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$PROJECTION_SCHEMA_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
PROJECTION_SOURCES=(app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyAuthorityProjectionR29.java app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyLiveProjectionR29.java app/src/main/java/com/ethk3/novaonepro/strategyv2/RealtimeResearchProjectionR29.java)
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$PROJECTION_SCHEMA_TMP/product" "${PROJECTION_SOURCES[@]}"
javac -nowarn -encoding UTF-8 -proc:none -cp "$PROJECTION_SCHEMA_TMP/shim:$PROJECTION_SCHEMA_TMP/product:$CLASSES:$ANDROID_JAR" -d "$PROJECTION_SCHEMA_TMP/test" tools/r29/qual_host/ProjectionSchemaR29HostTest.java tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/LegacyLiveProjectionR29HostTest.java tools/r29/qual_host/LegacyAuthorityProjectionR29HostTest.java tools/r29/qual_host/CutoverR29HostTest.java
java -cp "$PROJECTION_SCHEMA_TMP/shim:$PROJECTION_SCHEMA_TMP/test:$PROJECTION_SCHEMA_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ProjectionSchemaR29HostTest | tee "$OUT/r29_projection_schema_host.log"
java -cp "$PROJECTION_SCHEMA_TMP/shim:$PROJECTION_SCHEMA_TMP/test:$PROJECTION_SCHEMA_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.LegacyLiveProjectionR29HostTest | tee "$OUT/r29_projection_schema_existing_live_host.log"
java -cp "$PROJECTION_SCHEMA_TMP/shim:$PROJECTION_SCHEMA_TMP/test:$PROJECTION_SCHEMA_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.LegacyAuthorityProjectionR29HostTest | tee "$OUT/r29_projection_schema_existing_authority_host.log"
java -cp "$PROJECTION_SCHEMA_TMP/shim:$PROJECTION_SCHEMA_TMP/test:$PROJECTION_SCHEMA_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.CutoverR29HostTest | tee "$OUT/r29_projection_schema_existing_cutover_host.log"
mkdir -p "$PROJECTION_SCHEMA_TMP/old-source" "$PROJECTION_SCHEMA_TMP/old-product"
for name in LegacyAuthorityProjectionR29 LegacyLiveProjectionR29 RealtimeResearchProjectionR29; do git show "0c639ccb37025f6d573f0293abccff061d41826e:app/src/main/java/com/ethk3/novaonepro/strategyv2/$name.java" > "$PROJECTION_SCHEMA_TMP/old-source/$name.java"; done
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$PROJECTION_SCHEMA_TMP/old-product" "$PROJECTION_SCHEMA_TMP/old-source/"*.java
for control in root research; do
  set +e
  java -cp "$PROJECTION_SCHEMA_TMP/shim:$PROJECTION_SCHEMA_TMP/test:$PROJECTION_SCHEMA_TMP/old-product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.ProjectionSchemaR29HostTest "--old-$control-control" > "$OUT/old-0c-$control-ddl-control.log" 2>&1
  PROJECTION_SCHEMA_STATUS=$?
  set -e
  if [ "$control" = root ]; then expected=P1_1000_ROOT_DRAIN_SCHEMA_CHECKS_ZERO_DDL; else expected=P2_100_RESEARCH_SETUP_REPEATS_ZERO_DDL; fi
  if [ "$PROJECTION_SCHEMA_STATUS" -eq 0 ] || ! grep -Fq "$expected" "$OUT/old-0c-$control-ddl-control.log"; then cat "$OUT/old-0c-$control-ddl-control.log"; exit 1; fi
  echo "R29_PROJECTION_SCHEMA|OLD_${control}_REPEATED_DDL_REJECTED|exit=$PROJECTION_SCHEMA_STATUS"
done
