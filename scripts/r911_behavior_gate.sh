#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd); cd "$ROOT"
OUT=${R911_EVIDENCE_DIR:-"$ROOT/build/r911-verification/evidence"}; mkdir -p "$OUT"
fail(){ echo "R911_BEHAVIOR_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
forbid(){ if rg -q -- "$1" "$2"; then fail "$3"; fi; }
req '^versionName=V57\.18\.0-R5\.1\.0-911$' release/release_identity.properties IDENTITY_NAME
req '^versionCode=577220911$' release/release_identity.properties IDENTITY_CODE
req '^dbVersion=36$' release/release_identity.properties DB_VERSION
req '^schemaRevision=4$' release/release_identity.properties SCHEMA_REVISION
# R911 settlement core: no derivative/audit/diagnostic write may roll back OOS + task terminal.
req 'runSettlementCoreTransactionR911' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java SETTLEMENT_CORE_V2
forbid 'appendTransactionAudit\(db, tx, "COMMITTED"' <(sed -n '/runSettlementCoreTransactionR911/,/^    }/p' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java) CORE_AUDIT_INSIDE_TX
forbid 'RuntimeHealthLedgerR36\.recordWithinTransaction|DbStore\.putStatesR36|IssueClosureLedgerR906\.recordWithinTransaction|SettlementOutboxR906\.enqueueWithinTransaction' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java DERIVATIVE_CAN_ROLLBACK_CORE
req 'recordPostCommitTelemetryR911' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java TELEMETRY_POSTCOMMIT
# Stale lease root cause from R910 device report.
req 'prepareCurrentForSoleCoordinatorR911' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java SOLE_GATE_RECOVERY_WIRED
req 'R911_SOLE_GATE_ORPHAN_RECOVERED' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java SOLE_GATE_RECOVERY_IMPL
forbid 'GAVE_UP_TO_DURABLE_QUEUE' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java NO_CURRENT_GIVEUP
req 'R911_RETRY_UNTIL_TERMINAL' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java RETRY_UNTIL_TERMINAL
req 'retryDelayMsR911' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java SECOND_SCALE_RETRY
req 'reopenCurrentRecoverableFailedR911' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java RETRYABLE_FAILED_REOPEN
# OOS legacy-hash compatibility preserves business-fact integrity.
req 'REPLAY_IDEMPOTENT_BUSINESS_FACTS' app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java OOS_BUSINESS_FACT_REPLAY
req 'existingBusinessLabel' app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java OOS_FIRST_WRITE_FACT_CHECK
# R910 real SQLite failure must be migrated and impossible in active queries.
req 'migrateSelectedEvidenceSchemaR911' app/src/main/java/com/ethk3/novaonepro/DbHelper.java RANK_STATUS_MIGRATION
req 'ResearchRuntimeSqlGateR911\.verifyAndPublish' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java SQL_GATE_WIRED
# Legacy token may appear only in the migration implementation/docs, never active Research SELECTs.
if rg -n 'rank_score_status' app/src/main/java/com/ethk3/novaonepro --glob '!DbHelper.java' | grep -q .; then fail LEGACY_RANK_COLUMN_ACTIVE_QUERY; fi
# Core truth and diagnostics.
req 'SettlementRuntimeTruthR911\.snapshot' app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java R911_RUNTIME_TRUTH_WIRED
req 'R911ResearchSqlGate' app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java SQL_GATE_DIAGNOSTIC
# Deep learning/dynamic iteration/algorithm invention remain scheduled and future-only.
req 'ResearchOrchestratorR906\.pulse' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java FTRL_ORCHESTRATOR_WIRED
req 'ResearchTinyDeepLearnerR909\.updateOne' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java TINY_DEEP_UPDATE_WIRED
req 'ResearchTinyDeepLearnerR909\.capturePredraw' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java TINY_DEEP_PREDRAW_WIRED
req 'MAY_AUTHORIZE_REAL = false' app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java TINY_DEEP_REAL_ZERO
req 'public boolean algorithmGrade\(\)' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java ALGORITHM_GRADE
req 'updateReadsOutcome' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java ALGORITHM_OUTCOME_UPDATE
req 'programStructureCanonical' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java STRUCTURAL_NOVELTY
req 'behaviorHash' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java BEHAVIOR_NOVELTY
req 'NAMED_ALGORITHMS_BASELINE_ONLY' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java FIXED_ML_NOT_MOTHER
# Executable fault-injection/runtime SQL gates.
python3 verification/r911/r911_sqlite_runtime_gate.py > "$OUT/R911_SQLITE_RUNTIME_GATE.txt"
python3 verification/r911/r911_settlement_state_machine.py > "$OUT/R911_SETTLEMENT_STATE_MACHINE.txt"
python3 verification/r910/r910_settlement_core_isolation.py > "$OUT/R911_SIDECAR_FAILURE_INJECTION.txt"
python3 verification/r910/r910_backlog_convergence.py > "$OUT/R911_BACKLOG_CONVERGENCE_LEGACY_SHAPE.txt"
STARCORE_TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-/mnt/data/r911_work/TOOLCHAIN_USED} R911_EVIDENCE_DIR="$OUT" ./scripts/r911_compile_host.sh
rg -q '^R910_LEGACY_RANK_COLUMN_MIGRATION=PASS$' "$OUT/R911_SQLITE_RUNTIME_GATE.txt" || fail SQLITE_MIGRATION
rg -q '^COMPACT_FTRL_TINYDEEP_SQL_CONTRACT=PASS$' "$OUT/R911_SQLITE_RUNTIME_GATE.txt" || fail SQLITE_RUNTIME_CONTRACT
rg -q '^R911_SOLE_GATE_ORPHAN_RECOVERY=PASS$' "$OUT/R911_SETTLEMENT_STATE_MACHINE.txt" || fail LEASE_RECOVERY
rg -q '^R911_304_READY_CONVERGENCE=PASS$' "$OUT/R911_SETTLEMENT_STATE_MACHINE.txt" || fail REPORT_SHAPED_BACKLOG
rg -q '^CORE_SURVIVES_SIDECAR_FAILURE=PASS$' "$OUT/R911_SIDECAR_FAILURE_INJECTION.txt" || fail CORE_FAILURE_INJECTION
rg -q '^ONLINE_SETTLED_LABEL_UPDATE=PASS$' "$OUT/R911_TINY_DEEP_HARNESS.txt" || fail TINY_DEEP_RUNTIME
rg -q '^LINEAGE_NOT_STRUCTURAL_NOVELTY=PASS$' "$OUT/R911_ALGORITHM_GENOME_HARNESS.txt" || fail FAKE_ALGORITHM_NOVELTY
./scripts/r906_real_boundary_gate.sh > "$OUT/R911_REAL_BOUNDARY.txt"
python3 -m py_compile verification/r911/*.py verification/r910/*.py training/r906/*.py
printf 'R911_BEHAVIOR_GATE=PASS\nSETTLEMENT_CORE_V2=PASS\nSTALE_LEASE_RECOVERY=PASS\nCURRENT_RETRY_UNTIL_TERMINAL=PASS\nOOS_BUSINESS_FACT_REPLAY=PASS\nSQLITE_RUNTIME_GATE=PASS\nR910_LEGACY_SCHEMA_MIGRATION=PASS\n304_READY_CONVERGENCE=PASS\nSIDECAR_FAILURE_ISOLATION=PASS\nDYNAMIC_FTRL_WIRED=PASS\nTINY_DEEP_FUTURE_ONLY=PASS\nALGORITHM_INVENTION_CONTRACT=PASS\nREAL_BOUNDARY=PASS\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n' | tee "$OUT/R911_BEHAVIOR_GATE_SUMMARY.txt"
