#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/ack-recovery-host}
mkdir -p "$OUT"
ACK_TMP=$(mktemp -d)
trap 'rm -rf "$ACK_TMP"' EXIT
ANDROID_JAR=${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}
javac -nowarn -encoding UTF-8 -proc:none -d "$ACK_TMP/shim" $(find tools/r29/scoped_schema_host/shim -name '*.java') tools/r29/schema_owner_host/shim/android/content/ContentValues.java tools/r29/schema_owner_host/shim/android/database/Cursor.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabaseLockedException.java tools/r29/qual_host/shim/android/os/SystemClock.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$ACK_TMP/product" app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchEventBridgeR29.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$ACK_TMP/shim:$ACK_TMP/product:$CLASSES:$ANDROID_JAR" -d "$ACK_TMP/test" tools/r29/qual_host/EventYieldR29HostTest.java tools/r29/qual_host/AcknowledgedRecoveryR29HostTest.java
java -Xmx768m -cp "$ACK_TMP/shim:$ACK_TMP/test:$ACK_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.AcknowledgedRecoveryR29HostTest | tee "$OUT/r29_ack_recovery_host.log"
mkdir -p "$ACK_TMP/old-source" "$ACK_TMP/old-product"
git show '84c360e09eaff62fdb2ce63dffdf21572f282c52:app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchEventBridgeR29.java' > "$ACK_TMP/old-source/ResearchEventBridgeR29.java"
javac -nowarn -encoding UTF-8 -proc:none -cp "$CLASSES:$ANDROID_JAR" -d "$ACK_TMP/old-product" "$ACK_TMP/old-source/ResearchEventBridgeR29.java"
set +e
java -Xmx768m -cp "$ACK_TMP/shim:$ACK_TMP/test:$ACK_TMP/old-product:$ACK_TMP/product:$CLASSES:$SQLITE_JDBC:$ANDROID_JAR" com.ethk3.novaonepro.strategyv2.AcknowledgedRecoveryR29HostTest --old-84-control > "$OUT/old-84-ack-recovery-control.log" 2>&1
ACK_OLD_STATUS=$?
set -e
if [ "$ACK_OLD_STATUS" -eq 0 ] || ! grep -Fq A1_LEGACY_ACK_PAGE_REPAIRS_REAL_RP_INBOX "$OUT/old-84-ack-recovery-control.log"; then cat "$OUT/old-84-ack-recovery-control.log"; exit 1; fi
echo "R29_ACK_RECOVERY|OLD_84_MISSING_ACK_RECOVERY_REJECTED|exit=$ACK_OLD_STATUS"
