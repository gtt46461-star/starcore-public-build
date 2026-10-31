#!/usr/bin/env bash
# R29 CI build: bind identities to the committed source, run every existing suite, assemble and receipt the APKs.
# Requires ANDROID_HOME with platforms;android-36, build-tools;36.0.0, ndk;28.2.13676358, cmake;3.22.1.
# Signing comes from the environment (R13_SIGNING_P12/R13_SIGNING_PASSWORD/R13_EXPECTED_CERT_SHA256, R29_SIGNING).
# Without a signing identity the build still binds identities and runs all tests, and reports APK_NOT_BUILT.
#   PUSH_REF=<branch>  push the generated identity commit to that branch (CI only)
set -euo pipefail
cd "$(dirname "$0")/../.."
LOG=evidence/r30/ci; mkdir -p "$LOG"
commit_generated() {
  git add release/ app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java
  # The two generated identity files are tracked but live under the ignored app/build/: git refuses a plain add there.
  git add -f app/build/generated/r967/semantic_manifest.json app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java
  if ! git diff --cached --quiet; then
    git -c user.name='STARCORE build identity' -c user.email='starcore-build@users.noreply.github.com' commit -q -m "$1"
  fi
}
python3 tools/r29/rebind_identity.py | tail -n 3
commit_generated 'R29: bind generated identities to reviewed implementation bytes'
# The compiled call index is bound to actual class files: bootstrap compile, then refreeze and re-pin.
./gradlew --no-daemon -q :app:compileDebugJavaWithJavac -x :app:generateR982SourceCallAsset 2>&1 | tail -n 40 | tee "$LOG/bootstrap_compile.log"
python3 tools/r29/rebind_identity.py --classes app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes | tail -n 3
commit_generated 'R29: bind compiled call inventory'
git diff --exit-code -- app/src/main release app/build/generated
if [ -n "${PUSH_REF:-}" ]; then timeout 45s git push origin "HEAD:$PUSH_REF"; fi
python3 scripts/r926_release_manifest.py --check
python3 scripts/r967_semantic_identity.py --check
python3 scripts/test_r13_production_ddl.py
python3 scripts/generate_strategy_su2_schema.py --check
python3 scripts/verify_strategy_su1.py > "$LOG/su1.log"
python3 scripts/verify_plan1025.py > "$LOG/plan1025.log"
python3 scripts/verify_r26.py > "$LOG/r26.log"
python3 tools/r29/host_checks.py --out "$LOG/r29_host_checks.json"
bash tools/r30/ci_build_r30.sh "$LOG"
./gradlew --no-daemon :app:testDebugUnitTest 2>&1 | tee "$LOG/unit.log" | tail -n 30
# R29-010/035: contiguous qualification + late/out-of-order settlement on a real SQLite engine.
JDBC=$(mktemp -d)/sqlite-jdbc.jar
curl -fLsS --connect-timeout 15 --max-time 90 -o "$JDBC" https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
echo "4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa  $JDBC" | sha256sum -c -
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_qual_host.sh
# R30-B01-04-a: chunked settlement equals the pre-R30 batch; process death between chunks resumes exactly once.
T30=$(mktemp -d); javac -nowarn -encoding UTF-8 -proc:none -d "$T30/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes" -d "$T30/test" tools/r29/qual_host/QualInboxR29HostTest.java tools/r30/qual_host/SettleChunkR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC" com.ethk3.novaonepro.strategyv2.SettleChunkR30HostTest | tee "$LOG/r30_settle_chunk_host.log"
# R30-B02-03-a: missing draw backfilled later settles the original freeze once; recheck backoff; STALE kept.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes" -d "$T30/test" tools/r29/qual_host/QualInboxR29HostTest.java tools/r30/qual_host/PendingDrawR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC" com.ethk3.novaonepro.strategyv2.PendingDrawR30HostTest | tee "$LOG/r30_pending_draw_host.log"
# R30-B03-04-b: exercise the actual live-cache reader against SQLite backlogs and source corrections.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/LiveCacheR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.LiveCacheR30HostTest | tee "$LOG/r30_live_cache_backlog_host.log"
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.LiveCacheR30HostTest correction | tee "$LOG/r30_live_cache_correction_host.log"
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/FreezeSourceR30HostTest.java
for mode in race correction import delete tamper missing valid metadata schema; do
  if [ "$mode" = race ]; then args=(); else args=("$mode"); fi
  java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.FreezeSourceR30HostTest "${args[@]}" | tee "$LOG/r30_freeze_source_${mode}_host.log"
done
# R30-B03-03-b: a committed confirmation reservation survives process death and rejects stale writers.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/ConfirmReadStoreR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.ConfirmReadStoreR30HostTest | tee "$LOG/r30_confirm_store_host.log"
# R30-B05-01: the actual independent stage, raw-evidence restart and frozen FUTURE registration.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/FrozenConfirmationR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.FrozenConfirmationR30HostTest | tee "$LOG/r30_frozen_confirmation_host.log"
# R30-B06-04: 82 actual handlers and retained evidence, including audit-only and checkpoint dependencies.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/CapabilityRegistryR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.CapabilityRegistryR30HostTest | tee "$LOG/r30_capability_registry_host.log"
# R30-B06-03: twenty existing matrix bindings and original retained-snapshot watermarks.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/MatrixRegistryR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.MatrixRegistryR30HostTest | tee "$LOG/r30_matrix_registry_host.log"
# R30-B06-02: actual atom/molecule paired Brier and commit-failure publication boundaries.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/BrierContributionR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.BrierContributionR30HostTest | tee "$LOG/r30_brier_contribution_host.log"
# R30-B06-01: production rolling calibration, temporal cutoffs and atomic old/new reports.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/CalibrationR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.CalibrationR30HostTest | tee "$LOG/r30_calibration_host.log"
# R30-B05-04: actual policy/model pairing, immutable archives, and explicit missing/target/source blocks.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/ModelSurfaceR30HostTest.java
for mode in valid missing target race; do
  java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.ModelSurfaceR30HostTest "$mode" | tee "$LOG/r30_model_surface_${mode}_host.log"
done
# R30-B05-03: actual ten-position training, rollback and process death at position five.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/TenPositionR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.TenPositionR30HostTest | tee "$LOG/r30_ten_position_host.log"
# R30-B04-02-a: real sink/report receipts use the same committed, idempotent duplicate rows.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/BehaviorDedupR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.BehaviorDedupR30HostTest | tee "$LOG/r30_behavior_dedup_host.log"
# R30-B04-05-a: selector rate reaches the actual qualification registration without resetting budget on retry.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/SelectorRateR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.SelectorRateR30HostTest | tee "$LOG/r30_selector_rate_host.log"
# R30-B07-01: real re-ranking never evicts the durable confirmation queue or resets its receipts.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/ConfirmationQueueR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.ConfirmationQueueR30HostTest | tee "$LOG/r30_confirmation_queue_host.log"
# R30-B07-02: additive version metadata, actual V1 issuance and incompatible-version rejection.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/CertificateVersionR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.CertificateVersionR30HostTest | tee "$LOG/r30_certificate_version_host.log"
# R30-B07-03: actual certificate/freeze projection, append-only explanation and honest mismatch directions.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/DecisionExplanationR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.DecisionExplanationR30HostTest | tee "$LOG/r30_decision_explanation_host.log"
# A separate JVM scopes the read-only DbStore adapter to this authority test alone.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/authority-db" tools/r30/qual_host/authority_shim/com/ethk3/novaonepro/DbStore.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:$T30/authority-db:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/RealAuthorityR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/authority-db:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.RealAuthorityR30HostTest | tee "$LOG/r30_real_authority_host.log"
# R30-B07-04: same-source transaction revocation and real next-read rejection; HOST time never claims phone time.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:$T30/authority-db:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/CertificateRevocationR30HostTest.java
java -Xmx768m -cp "$T30/shim:$T30/authority-db:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.CertificateRevocationR30HostTest | tee "$LOG/r30_certificate_revocation_host.log"
# R30-B08-01: actual compiled report captures one detached SQLite view while a WAL writer advances.
# Build's HOST identity is scoped to this separate JVM; the authority DbStore adapter is excluded.
javac -nowarn -encoding UTF-8 -proc:none -d "$T30/report-platform" tools/r30/qual_host/report_shim/android/os/Build.java
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:$T30/report-platform:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/ReportSnapshotR30HostTest.java
java -Xmx768m -cp "$T30/report-platform:$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.ReportSnapshotR30HostTest | tee "$LOG/r30_report_snapshot_host.log"
# R30-B08-03: original upgrade/ablation sources distinguish missing, empty and stored without fake Q06.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:$T30/report-platform:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$ANDROID_HOME/platforms/android-36/android.jar" -d "$T30/test" tools/r30/qual_host/UpgradeEvidenceR30HostTest.java
java -Xmx768m -cp "$T30/report-platform:$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC:$ANDROID_HOME/platforms/android-36/android.jar" com.ethk3.novaonepro.strategyv2.UpgradeEvidenceR30HostTest | tee "$LOG/r30_upgrade_ablation_host.log"
# R30-B08-04: original production JNI plus real ZIP part reconstruction and durable cancellation/cleanup.
REPORT_EXPORT_OUT="$LOG/r30_report_export_host" bash tools/r30/run_report_export_host.sh
if [ -z "${R13_SIGNING_P12:-}" ]; then
  echo "APK_NOT_BUILT: no existing signing identity reachable from this branch" | tee "$LOG/apk_status.txt"
  exit 0
fi
REL=''
if [ -n "${DUOQU_API_KEY:-}" ]; then REL=':app:assembleRelease'; else echo 'RELEASE_NOT_BUILT: no API credential' | tee "$LOG/release_status.txt"; fi
./gradlew --no-daemon :app:verifyCanonicalPlanBuildCP3 $REL :app:assembleDebug 2>&1 | tee "$LOG/assemble.log" | tail -n 40
python3 tools/r29/package_receipt.py
