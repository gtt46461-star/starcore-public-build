#!/usr/bin/env bash
# R31.2 HOST_SUITE runner: one entry id -> the exact tools/r31 (or tools/r30-repair) command with its arguments.
# Runs after the gate's gradle tasks on the same HEAD (classes, R.jar and unit runtime jar already built).
# Supplies only platform inputs (Android 36 jar, pinned sqlite-jdbc, Gradle cache); never changes assertions.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
id="${1:?entry id required}"
CL="$PWD/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
JAR="$PWD/app/build/intermediates/runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar"
OUT="$PWD/app/build/r312-host/$id"; rm -rf "$OUT"; mkdir -p "$(dirname "$OUT")"
CACHE="${R312_HOST_CACHE:-$HOME/.cache/r312-host}"; mkdir -p "$CACHE"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
test -f "$ANDROID_HOME/platforms/android-36/android.jar" || { echo "R312_HOST_ANDROID_36_JAR_MISSING:$ANDROID_HOME"; exit 3; }
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
JDBC="$CACHE/sqlite-jdbc-3.46.1.3.jar"
if [ ! -f "$JDBC" ]; then
  curl -fLsS --retry 4 --connect-timeout 15 --max-time 120 -o "$JDBC.tmp" https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
  mv "$JDBC.tmp" "$JDBC"
fi
echo "4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa  $JDBC" | sha256sum -c - >/dev/null
export SQLITE_JDBC="$JDBC"
test -d "$CL" || { echo "R312_HOST_CLASSES_MISSING (gradle compileDebugJavaWithJavac must run first)"; exit 3; }
if [ ! -f "$JAR" ]; then mkdir -p "$(dirname "$JAR")"; (cd "$CL" && jar cf "$JAR" .); fi
case "$id" in
  run_atom_publication_host) exec python3 tools/r31/run_atom_publication_host.py --output "$OUT" ;;
  run_atomnet_publication_host) exec python3 tools/r31/run_atomnet_publication_host.py --classes "$JAR" --out "$OUT" ;;
  run_certificate_startup_host)
    for order in TEST_OPEN SU_BEFORE_R27 SU_AFTER_R27 NEG_TAMPERED_SU1_TRIGGER NEG_FOREIGN_DRAW_TRIGGER; do
      echo "== R31_ORDER=$order"; R31_ORDER=$order python3 tools/r31/run_certificate_startup_host.py; done ;;
  run_checkpoint_startup_host) exec python3 tools/r31/run_checkpoint_startup_host.py --out "$OUT" --classes "$CL" ;;
  run_evolution_publication_host) exec python3 tools/r31/run_evolution_publication_host.py --case all --output "$OUT" ;;
  run_legacy_qualification_contract) exec python3 tools/r31/run_legacy_qualification_contract.py --classes "$JAR" --out "$OUT" ;;
  run_report_cas_scope_host) exec python3 tools/r31/run_report_cas_scope_host.py --out "$OUT" --classes "$CL" ;;
  run_report_history_reader_host) exec python3 tools/r31/run_report_history_reader_host.py --out "$OUT" --classes "$CL" ;;
  run_telemetry_budget_host) exec python3 tools/r31/run_telemetry_budget_host.py --output "$OUT" ;;
  run_writer_hold_fault_host) exec python3 tools/r31/run_writer_hold_fault_host.py --classes "$JAR" --joint-classes "$CL" --out "$OUT" ;;
  run_research_schema_host|run_runtime_ddl_host|run_runtime_ddl_writer_regression|run_service_protocol_host|run_task_runtime_host|run_write_protocol_host)
    exec python3 "tools/r31/$id.py" ;;
  verify_ast_family_scope) exec python3 tools/r31/verify_ast_family_scope.py --support-jar "$JAR" --main-source app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicyAstR27.java ;;
  verify_baseline) exec python3 tools/r31/verify_baseline.py --output "$OUT/baseline.json" ;;
  verify_continuation) exec python3 tools/r31/verify_continuation.py --classes "$CL" --out "$OUT" ;;
  verify_family_admission|verify_pin_capacity) exec python3 "tools/r31/$id.py" --support-jar "$JAR" ;;
  verify_memory|verify_pitguard_allocation|verify_resource_recovery|verify_runtime_memory_control|verify_runtime_observation|verify_task_status)
    exec python3 "tools/r31/$id.py" ;;
  run_runtime_health_host) R31_RUNTIME_HOST_CLASS=R31RuntimeHealthHost R31_RUNTIME_OUTPUT="app/build/r312-host/$id-receipt" exec python3 tools/r31/run_task_runtime_host.py ;;
  run_resource_recovery_consumer_host) R31_RUNTIME_HOST_CLASS=R31ResourceRecoveryConsumerHost R31_RUNTIME_OUTPUT="app/build/r312-host/$id-receipt" exec python3 tools/r31/run_task_runtime_host.py ;;
  run_storage_report_host) R31_STORAGE_OUT="$OUT" exec bash tools/r31/storage_host/run_storage_report_host.sh ;;
  run_unified_migration_host) exec python3 tools/r31/run_unified_migration_host.py --out "$OUT" ;;
  run_keep_store_host|run_gc_store_host|run_report_keep_closure_host|run_storage_checkpoint_host) exec python3 "tools/r31/$id.py" --out "$OUT" --classes "$CL" ;;
  run_hypothesis_store_host) R31_HYPOTHESIS_SUPPORT_CLASSES="$CL" exec python3 tools/r31/run_hypothesis_store_host.py --out "$OUT" ;;
  run_lifecycle_schema_host|run_lifecycle_stores_host) R31_COMPILED_SUPPORT="$CL" exec python3 "tools/r31/$id.py" --out "$OUT" ;;
  run_replay_knowledge_host) R31_REPLAY_KNOWLEDGE_SUPPORT_CLASSES="$CL" exec python3 tools/r31/run_replay_knowledge_host.py --out "$OUT" ;;
  run_replay_knowledge_publication_host) R31_HOST_CLASSES="$CL" exec python3 tools/r31/run_replay_knowledge_publication_host.py --out "$OUT" ;;
  run_typed_mainline_host) exec python3 tools/r31/run_typed_mainline_host.py --out "$OUT" --classes "$CL" ;;
  run_gap_runtime_entry_host) R31_COMPILED_SUPPORT="$CL" exec python3 tools/r31/run_gap_runtime_entry_host.py --out "$OUT" ;;
  run_startup_checkpoint_host) exec python3 tools/r31/run_startup_checkpoint_host.py --out "$OUT" --classes "$CL" ;;
  run_legacy_replay_checkpoint_host) exec python3 tools/r31/run_legacy_replay_checkpoint_host.py --output "$OUT" --classes "$CL" ;;
  run_restore_proof_host) R31_HOST_CLASSES="$CL" exec python3 tools/r31/run_restore_proof_host.py --output "$OUT" ;;
  run_factory_transport_host) R31_FACTORY_TRANSPORT_OUT="$OUT" exec python3 tools/r31/run_factory_transport_host.py ;;
  run_r31q_lock_regression) R31Q_OUTPUT="$OUT" exec python3 tools/r31/run_r31q_lock_regression.py ;;
  test_current_acceptance) exec python3 tools/r31/test_current_acceptance.py ;;
  run_upgrade_matrix_host) exec python3 tools/r31/upgrade_matrix.py --out "$OUT" ;;
  run_full_chain_fault_host) exec python3 tools/r31/run_full_chain_fault_host.py --out "$OUT" ;;
  r31_verification_receipt_selftest) exec python3 tools/r31/test_verification_receipt.py ;;
  r31_package_release_selftest) exec python3 tools/r31/test_package_release.py ;;
  r31_retirement_manifest_check) exec python3 tools/r31/retirement_inventory.py --check ;;
  r31_device_receipts_selftest) exec python3 tools/r31/test_device_receipts.py ;;
  r30repair_*) exec python3 "tools/r30-repair/run_${id#r30repair_}_host.py" ;;
  *) echo "R312_HOST_UNKNOWN_ENTRY:$id"; exit 2 ;;
esac
