#!/usr/bin/env bash
# Exact production event/first-seen source on real SQLite, including an original-source negative control.
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/event-schema-host}
mkdir -p "$OUT"
EVENT_SCHEMA_TMP=$(mktemp -d)
trap 'rm -rf "$EVENT_SCHEMA_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$EVENT_SCHEMA_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$EVENT_SCHEMA_TMP/shim:$CLASSES" -d "$EVENT_SCHEMA_TMP/current" app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchEventBridgeR29.java app/src/main/java/com/ethk3/novaonepro/FetchObservationR29.java tools/r29/qual_host/EventSchemaR29HostTest.java tools/r29/qual_host/EventBridgeR29HostTest.java tools/r29/qual_host/FetchObservationR29HostTest.java
java -cp "$EVENT_SCHEMA_TMP/shim:$EVENT_SCHEMA_TMP/current:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.EventSchemaR29HostTest | tee "$OUT/r29_event_schema_host.log"
java -cp "$EVENT_SCHEMA_TMP/shim:$EVENT_SCHEMA_TMP/current:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.EventBridgeR29HostTest | tee "$OUT/r29_event_schema_existing_event_host.log"
java -cp "$EVENT_SCHEMA_TMP/shim:$EVENT_SCHEMA_TMP/current:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.FetchObservationR29HostTest | tee "$OUT/r29_event_schema_existing_fetch_host.log"
mkdir -p "$EVENT_SCHEMA_TMP/old/com/ethk3/novaonepro/strategyv2"
git show 2f1e2ab9caf2722647127625076335de523f1244:app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchEventBridgeR29.java > "$EVENT_SCHEMA_TMP/old/com/ethk3/novaonepro/strategyv2/ResearchEventBridgeR29.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$EVENT_SCHEMA_TMP/shim:$CLASSES" -d "$EVENT_SCHEMA_TMP/negative" "$EVENT_SCHEMA_TMP/old/com/ethk3/novaonepro/strategyv2/ResearchEventBridgeR29.java" tools/r29/qual_host/EventSchemaR29HostTest.java
if java -cp "$EVENT_SCHEMA_TMP/shim:$EVENT_SCHEMA_TMP/negative:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.EventSchemaR29HostTest > "$OUT/r29_event_schema_original_negative.log" 2>&1; then
  echo 'ORIGINAL_EVENT_SCHEMA_REPETITION_NEGATIVE_UNEXPECTED_PASS'; exit 1
fi
grep -Fq 'R29_EVENT_SCHEMA|Q2_1000_ROW_REPEATED_CANONICAL_BATCH_DDL_ZERO|FAILED' "$OUT/r29_event_schema_original_negative.log"
echo 'R29_EVENT_SCHEMA|ORIGINAL_2f1_SOURCE_REPEATED_DDL_DETECTED|OK' | tee -a "$OUT/r29_event_schema_host.log"
