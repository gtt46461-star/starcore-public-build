#!/usr/bin/env bash
# R29 CI build: bind identities to the committed source, run every existing suite, assemble and receipt the APKs.
# Requires ANDROID_HOME with platforms;android-36, build-tools;36.0.0, ndk;28.2.13676358, cmake;3.22.1.
# Signing comes from the environment (R13_SIGNING_P12/R13_SIGNING_PASSWORD/R13_EXPECTED_CERT_SHA256, R29_SIGNING).
# Without a signing identity the build still binds identities and runs all tests, and reports APK_NOT_BUILT.
#   PUSH_REF=<branch>  push the generated identity commit to that branch (CI only)
set -euo pipefail
cd "$(dirname "$0")/../.."
LOG=evidence/r29/ci; mkdir -p "$LOG"
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
python3 -m unittest discover -s tools/r29 -p test_inspect_runtime.py -v > "$LOG/device_protocol_qa.log" 2>&1
python3 tools/r29/test_smoke_phases.py > "$LOG/smoke_phase_orchestration_qa.log" 2>&1
./gradlew --no-daemon :app:testDebugUnitTest 2>&1 | tee "$LOG/unit.log" | tail -n 30
# R29-010/035: contiguous qualification + late/out-of-order settlement on a real SQLite engine.
JDBC=$(mktemp -d)/sqlite-jdbc.jar
curl -fLsS --connect-timeout 15 --max-time 90 -o "$JDBC" https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
echo "4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa  $JDBC" | sha256sum -c -
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_qual_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_event_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_event_yield_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_event_schema_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_migration_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_migration_prepare_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_expert_snapshot_reader_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_incumbent_observation_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_continuation_observation_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_registration_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_cutover_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_cutover_snapshot_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_cutover_resume_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_durable_handoff_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_ack_recovery_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_owner_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_report_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_legacy_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes OUT="$LOG" bash tools/r29/run_legacy_archive_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_model_chunk_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_rows_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_large_rows_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_full_schema_cutover_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_terminal_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_legacy_live_projection_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_domain_upgrade_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_critical_mirror_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_root_read_pool_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_dbstore_observation_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_learned_input_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_legacy_learning_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_critical_production_trigger_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_projection_schema_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_research_schema_owner_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_schema_ddl_proof_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_schema_installer_budget_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_cold_start_fairness_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_research_phase_stamp_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_domain_extension_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_schema_progress_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_retained_formal_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_canonical_input_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_dispatch_pending_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_scoped_schema_pending_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_shadow_retirement_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_evolution_dataset_pin_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_runtime_evolution_dataset_pin_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_terminal_projection_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_decision_retention_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_legacy_authority_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_prepared_host.sh
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_fetch_host.sh
if [ -z "${R13_SIGNING_P12:-}" ]; then
  echo "APK_NOT_BUILT: no existing signing identity reachable from this branch" | tee "$LOG/apk_status.txt"
  exit 0
fi
REL=''
if [ -n "${DUOQU_API_KEY:-}" ]; then REL=':app:assembleRelease'; else echo 'RELEASE_NOT_BUILT: no API credential' | tee "$LOG/release_status.txt"; fi
./gradlew --no-daemon :app:verifyCanonicalPlanBuildCP3 $REL :app:assembleDebug 2>&1 | tee "$LOG/assemble.log" | tail -n 40
python3 tools/r29/package_receipt.py
