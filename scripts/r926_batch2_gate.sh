#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

fail() { echo "R926_BATCH2_GATE=FAIL|$1" >&2; exit 71; }
need_file() { [[ -f "$1" ]] || fail "MISSING_FILE:$1"; }
need_text() {
  local file=$1 text=$2 code=$3
  grep -Fq -- "$text" "$file" || fail "$code:$file"
}
need_absent() {
  local file=$1 text=$2 code=$3
  if grep -Fq -- "$text" "$file"; then fail "$code:$file"; fi
}

CORE=app/src/main/java/com/ethk3/novaonepro/Batch2ContractCoreR926.java
OBS=app/src/main/java/com/ethk3/novaonepro/RuntimeObservabilityR925.java
ATTEMPT_OBS=app/src/main/java/com/ethk3/novaonepro/RuntimeAttemptObservabilityR926.java
FETCH=app/src/main/java/com/ethk3/novaonepro/FetchLedgerR925.java
SCHEMA=app/src/main/java/com/ethk3/novaonepro/SchemaRevisionR925.java
FRESH=app/src/main/java/com/ethk3/novaonepro/ProviderFreshnessGateR3611.java
BACKLOG=app/src/main/java/com/ethk3/novaonepro/BacklogConvergenceR925.java
RECOVERY=app/src/main/java/com/ethk3/novaonepro/RecoveryCoordinatorR815.java
WATCHDOG=app/src/main/java/com/ethk3/novaonepro/ServiceWatchdogJobService.java
HOOKS=app/src/main/java/com/ethk3/novaonepro/RuntimeEventHooksR926.java
GATE=app/src/main/java/com/ethk3/novaonepro/Batch2GateEvidenceR926.java
FTRL=app/src/main/java/com/ethk3/novaonepro/ResearchAdaptiveLearnerStoreR811.java
DEEP=app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java
HARNESS=verification/r925/Batch2ContractHarnessR926.java
E02C_HARNESS=scripts/r926_e02c_learning_freeze_harness.py
MANIFEST=release/release_manifest.json

for f in "$CORE" "$OBS" "$ATTEMPT_OBS" "$FETCH" "$SCHEMA" "$FRESH" "$BACKLOG" "$RECOVERY" "$WATCHDOG" "$HOOKS" "$GATE" "$FTRL" "$DEEP" "$HARNESS" "$E02C_HARNESS" "$MANIFEST"; do need_file "$f"; done

# B05: real report must expose the complete evidence distribution and must not pass short samples.
need_text "$OBS" 'P50=' 'B05_P50_MISSING'
need_text "$OBS" 'P95=' 'B05_P95_MISSING'
need_text "$OBS" 'P99=' 'B05_P99_MISSING'
need_text "$OBS" 'max=' 'B05_MAX_MISSING'
need_text "$OBS" 'overBudget=' 'B05_OVER_BUDGET_MISSING'
need_text "$OBS" 'INCOMPLETE_OR_FAILED_ATTEMPT_WINDOW' 'B05_SHORT_SAMPLE_GUARD_MISSING'
need_text "$OBS" 'RAW_LAST_120' 'B05_RAW_EVIDENCE_MISSING'
need_text "$OBS" 'RuntimeAttemptObservabilityR926.report(s)' 'B05_ATTEMPT_REPORT_NOT_WIRED'
need_text "$ATTEMPT_OBS" 'B05_ATTEMPT_OUTCOMES' 'B05_ATTEMPT_REPORT_MARKER_MISSING'
need_text "$ATTEMPT_OBS" "event_type='REQUESTED'" 'B05_REQUEST_ATTEMPT_COUNT_MISSING'
need_text "$ATTEMPT_OBS" 'explicit_timeout=' 'B05_TIMEOUT_COUNT_MISSING'
need_text "$ATTEMPT_OBS" 'explicit_cancel=' 'B05_CANCEL_COUNT_MISSING'
need_text "$ATTEMPT_OBS" 'FAILED_TERMINAL' 'B05_TERMINAL_FAILURE_MISSING'
need_text "$ATTEMPT_OBS" 'RETRY_WAIT' 'B05_RETRY_VISIBILITY_MISSING'
need_text "$ATTEMPT_OBS" 'EXPIRED' 'B05_EXPIRED_VISIBILITY_MISSING'
need_text "$ATTEMPT_OBS" 'error_code' 'B05_ACTIVE_ERROR_MISSING'
need_text "$ATTEMPT_OBS" 'first_error_code' 'B05_FIRST_ERROR_MISSING'
need_text "$ATTEMPT_OBS" 'deadline_exceeded=' 'B05_DEADLINE_EVIDENCE_MISSING'

# B07: explicit user stop is fail-closed; only independently healthy watchdog truth may close recovery.
need_text "$RECOVERY" 'USER_DISABLED_FAIL_CLOSED' 'B07_USER_STOP_FAIL_CLOSED_MISSING'
need_text "$RECOVERY" 'business_restored_at' 'B07_RESTORED_TIMESTAMP_MISSING'
need_text "$RECOVERY" 'markBusinessRestored' 'B07_RESTORED_METHOD_MISSING'
need_text "$RECOVERY" 'ALREADY_RUNNING_RECONCILE' 'B07_ALREADY_RUNNING_OBSERVATION_MISSING'
need_text "$WATCHDOG" 'truth.healthy()' 'B07_HEALTHY_TRUTH_GUARD_MISSING'
need_text "$WATCHDOG" 'isRuntimeLeaseFresh' 'B07_RUNTIME_LEASE_GUARD_MISSING'
need_text "$WATCHDOG" 'RecoveryCoordinatorR815.markBusinessRestored' 'B07_WATCHDOG_CLOSE_MISSING'
need_text "$WATCHDOG" 'USER_DISABLED_FAIL_CLOSED' 'B07_WATCHDOG_USER_STOP_GUARD_MISSING'

# B08/E02B: trusted SourceEvent and clock governance must be durable, versioned by payload, not UI-only.
for token in TRANSPORT_TIMEOUT TRANSPORT_HTTP_503 TRANSPORT_NETWORK_UNAVAILABLE PAYLOAD_REJECTED_DIRTY_JSON PAYLOAD_REJECTED_WRONG_GAME PAYLOAD_REJECTED_NUMBER_PATTERN CONFLICT_QUARANTINED OUT_OF_ORDER_REJECTED ACCEPTED_DUPLICATE CLOCK_SKEW_QUARANTINED; do
  need_text "$CORE" "$token" "B08_CLASSIFIER_VOCAB_MISSING_$token"
done
need_text "$FETCH" 'Batch2ContractCoreR926.classifyFetch(' 'B08_FETCH_CLASSIFIER_WIRING_MISSING'
need_text "$FETCH" 'raw.errorMessage' 'B08_REAL_TRANSPORT_REASON_MISSING'
need_text "$FETCH" 'PROCESS_INTERRUPTED' 'B08_PROCESS_INTERRUPTED_MISSING'
need_text "$FETCH" 'PROCESS_DIED_BEFORE_RESPONSE_DURABLE' 'B08_INTERRUPTED_REASON_MISSING'
need_text "$FETCH" 'SOURCE_EVENT_LABEL_VERSION' 'E02B_LABEL_VERSION_MISSING'
need_text "$FETCH" 'firstAcceptedAvailability' 'E02B_FIRST_AVAILABLE_OWNER_MISSING'
need_text "$FETCH" 'latest_issue=? AND payload_hash=? AND first_available_at>0' 'E02B_FIRST_AVAILABLE_NOT_PAYLOAD_VERSIONED'
need_text "$FETCH" 'Late corrections create a new request row' 'E02B_LATE_CORRECTION_APPEND_CONTRACT_MISSING'
need_text "$FETCH" 'drawAt(' 'E02B_DRAW_AT_OWNER_MISSING'
need_text "$FETCH" 'CLOCK_EPOCH' 'E02B_CLOCK_EPOCH_MISSING'
need_text "$FRESH" 'TimeSourceR811.detectSkew()' 'E02B_CLOCK_SKEW_DETECT_NOT_WIRED'
for column in provider payload_hash first_available_at received_at draw_at clock_epoch label_version; do
  need_text "$SCHEMA" "$column" "E02B_SCHEMA_COLUMN_MISSING_$column"
  need_text "$FETCH" "$column" "E02B_FETCH_FIELD_MISSING_$column"
done
need_text "$SCHEMA" 'ALTER TABLE' 'E02B_DB36_ADDITIVE_MIGRATION_MISSING'
python3 - "$MANIFEST" <<'PY_GATE_REVISION'
import json,sys
path=sys.argv[1]
manifest=json.load(open(path,encoding='utf-8'))
schema=manifest.get('schema') or {}
def fail(code,detail):
    print('R926_BATCH2_GATE=FAIL|%s:%s'%(code,detail),file=sys.stderr); raise SystemExit(71)
if schema.get('r925Revision') != 'R925_SCHEMA_7': fail('E02B_R925_REVISION_NOT_ADVANCED',schema.get('r925Revision'))
if int(schema.get('criticalDomainRevision',-1)) < 5: fail('E02C_CRITICAL_REVISION_NOT_ADVANCED',schema.get('criticalDomainRevision'))
if int(schema.get('researchDomainRevision',-1)) < 2: fail('E02C_RESEARCH_REVISION_NOT_ADVANCED',schema.get('researchDomainRevision'))
if int(schema.get('userVersion',-1)) != 36: fail('E02B_DB_USER_VERSION_DRIFT',schema.get('userVersion'))
print('R926_SCHEMA_REVISION_GATE=PASS|critical=%s|research=%s|userVersion=%s'%(schema.get('criticalDomainRevision'),schema.get('researchDomainRevision'),schema.get('userVersion')))
PY_GATE_REVISION

# B09: both online Research learners bind model artifact and LEARNED evidence in one real DB transaction.
need_text "$HOOKS" 'db.inTransaction()' 'B09_TRANSACTION_GUARD_MISSING'
need_text "$HOOKS" 'B09_LEARNING_IDENTITY_INVALID' 'B09_IDENTITY_GUARD_MISSING'
need_text "$HOOKS" 'B09_PRE_STATE_HASH_INVALID' 'B09_PRE_HASH_GUARD_MISSING'
need_text "$HOOKS" 'B09_POST_STATE_HASH_INVALID' 'B09_POST_HASH_GUARD_MISSING'
need_text "$HOOKS" 'B09_TRAINED_UNTIL_MISMATCH' 'B09_TRAINED_UNTIL_GUARD_MISSING'
need_text "$FTRL" '.runTransaction(' 'B09_FTRL_REAL_TRANSACTION_API_MISSING'
need_text "$FTRL" 'B09_FTRL_TRANSACTION_NOT_ACTIVE' 'B09_FTRL_TRANSACTION_ASSERT_MISSING'
need_text "$FTRL" 'B09_FTRL_STALE_PRE_STATE' 'B09_FTRL_LINEAR_CAS_MISSING'
need_text "$FTRL" 'B09_FTRL_TARGET_ARTIFACT_CONFLICT' 'B09_FTRL_TARGET_CONFLICT_GUARD_MISSING'
need_text "$FTRL" 'R811_FTRL_ARTIFACT_B09_ATOMIC' 'B09_FTRL_ATOMIC_OWNER_MISSING'
need_text "$FTRL" 'RuntimeEventHooksR926.learnedWithinTransaction' 'B09_FTRL_LEDGER_BINDING_MISSING'
need_text "$DEEP" '.runTransaction(' 'B09_DEEP_REAL_TRANSACTION_API_MISSING'
need_text "$DEEP" 'B09_TINY_DEEP_TRANSACTION_NOT_ACTIVE' 'B09_DEEP_TRANSACTION_ASSERT_MISSING'
need_text "$DEEP" 'R909_RESEARCH_TINY_DEEP_ARTIFACT_B09_ATOMIC' 'B09_DEEP_ATOMIC_OWNER_MISSING'
need_text "$DEEP" 'RuntimeEventHooksR926.learnedWithinTransaction' 'B09_DEEP_LEDGER_BINDING_MISSING'
need_text "$DEEP" 'MAY_AUTHORIZE_REAL = false' 'B09_DEEP_REAL_BOUNDARY_MISSING'

# E02C-A03: once LEARNED_EXACTLY_ONCE exists, identity-affecting inputs cannot be silently corrected.
# This is a DB-owned fail-close contract shared by FTRL, TinyDeep, restart and dual-worker paths.
for trigger in \
  trg_r927_e02c_ftrl_oos_update_freeze \
  trg_r927_e02c_ftrl_oos_delete_freeze \
  trg_r927_e02c_ftrl_evidence_update_freeze \
  trg_r927_e02c_ftrl_evidence_delete_freeze \
  trg_r927_e02c_ftrl_evidence_insert_freeze \
  trg_r927_e02c_tiny_label_update_freeze \
  trg_r927_e02c_tiny_label_delete_freeze; do
  need_text "$SCHEMA" "$trigger" "E02C_TRIGGER_MISSING_$trigger"
done
need_text "$SCHEMA" 'LEARNED_EXACTLY_ONCE' 'E02C_LEARNED_EVENT_GUARD_MISSING'
need_text "$SCHEMA" 'ResearchAdaptiveLearnerStoreR811' 'E02C_FTRL_OWNER_MISSING'
need_text "$SCHEMA" 'ResearchTinyDeepLearnerR909' 'E02C_TINY_OWNER_MISSING'
need_text "$SCHEMA" 'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN' 'E02C_FTRL_LABEL_ABORT_MISSING'
need_text "$SCHEMA" 'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN' 'E02C_FTRL_EVIDENCE_ABORT_MISSING'
need_text "$SCHEMA" 'E02C_TINY_LABEL_FROZEN_AFTER_LEARN' 'E02C_TINY_ABORT_MISSING'
need_text "$SCHEMA" 'strict_oos,prediction_created_at,draw_open_time,hit_label,selected_position,selected_hit_p' 'E02C_FTRL_LABEL_IDENTITY_FIELDS_MISSING'
need_text "$SCHEMA" 'raw_action_p_value,rank_score_value,third_margin_value,entropy_risk_value,stability_value,window_consensus_value' 'E02C_FTRL_EVIDENCE_IDENTITY_FIELDS_MISSING'
need_text "$SCHEMA" 'raw_action_p_status,rank_status,third_margin_status,entropy_risk_status,stability_status,window_consensus_status' 'E02C_FTRL_EVIDENCE_ELIGIBILITY_FIELDS_MISSING'
need_text "$SCHEMA" 'asset_id,target_issue,feature_blob,hit_label' 'E02C_TINY_IDENTITY_FIELDS_MISSING'
need_text "$SCHEMA" "OLD.asset_id='PRODUCTION_SELECTED'" 'E02C_TINY_PRODUCTION_ASSET_SCOPE_MISSING'
need_text "$E02C_HARNESS" 'unlearned_ftrl_and_tiny_inputs_remain_mutable' 'E02C_PRELEARN_MUTABILITY_TEST_MISSING'
need_text "$E02C_HARNESS" 'learned_ftrl_new_join_evidence_rejected' 'E02C_FTRL_INSERT_BYPASS_TEST_MISSING'
need_text "$E02C_HARNESS" 'unrelated_research_asset_not_overfrozen' 'E02C_SCOPE_TEST_MISSING'

# B10: a newer FULL must not hide an older hole or FAILED_TERMINAL row.
need_text "$BACKLOG" 'continuous_full_watermark=' 'B10_WATERMARK_MISSING'
need_text "$BACKLOG" 'holes=' 'B10_HOLE_COUNT_MISSING'
need_text "$BACKLOG" 'first_hole=' 'B10_FIRST_HOLE_MISSING'
need_text "$BACKLOG" "status='FAILED_TERMINAL'" 'B10_TERMINAL_COUNT_MISSING'
need_text "$BACKLOG" 'continuity.closure' 'B10_CONTINUITY_CLOSURE_MISSING'

# B11/E02D: full immutable package must sit in the existing canonical transaction and bind E02B PIT truth.
need_text "$HOOKS" 'Batch2GateEvidenceR926.validateAndAppendWithinTransaction' 'B11_CANONICAL_BINDING_MISSING'
need_text "$GATE" 'db.inTransaction()' 'B11_TRANSACTION_GUARD_MISSING'
need_text "$CORE" 'BET_PROVENANCE_VALID_V2' 'B11_PROVENANCE_V2_CONTRACT_MISSING'
need_text "$CORE" 'BET_OPPORTUNITY_HASH_MISSING' 'B11_OPPORTUNITY_HASH_GUARD_MISSING'
need_text "$CORE" 'BET_NUMBERS_NOT_DISTINCT' 'B11_FIXED_K3_GUARD_MISSING'
for field in dataHash featureHash modelHash stateHash selectionHash calibrationHash certificateHash budgetHash actualConfig; do
  need_text "$CORE" "$field" "E02D_GATE_INPUT_MISSING_$field"
done
for token in BET_DATA_HASH_MISSING BET_FEATURE_HASH_MISSING BET_MODEL_HASH_MISSING BET_STATE_HASH_MISSING BET_SELECTION_HASH_MISSING BET_CALIBRATION_HASH_MISSING BET_CERTIFICATE_HASH_MISSING BET_BUDGET_HASH_MISSING BET_ACTUAL_CONFIG_MISSING; do
  need_text "$CORE" "$token" "E02D_FAIL_CLOSE_MISSING_$token"
done
for field in data_hash feature_hash model_hash state_hash selection_hash calibration_hash certificate_hash budget_hash actual_config_hash; do
  need_text "$GATE" "$field" "E02D_EVENT_EVIDENCE_MISSING_$field"
done
need_text "$GATE" 'sourceEvidence(db, request)' 'E02D_SOURCE_EVENT_HELPER_NOT_CONSUMED'
need_text "$GATE" 'SchemaRevisionR925.TABLE_FETCH_LEDGER' 'E02D_SOURCE_EVENT_LEDGER_NOT_CONSUMED'
need_text "$GATE" 'E02B_SOURCE_EVENT|' 'E02D_SOURCE_EVENT_MARKER_MISSING'
need_text "$GATE" 'SOURCE_EVENT_MISSING_BEFORE_PIT' 'E02D_MISSING_SOURCE_FAIL_CLOSE_MISSING'
need_text "$GATE" 'first_available_at<=?' 'E02D_SOURCE_FIRST_AVAILABLE_CUTOFF_MISSING'
need_text "$GATE" 'received_at<=?' 'E02D_SOURCE_RECEIVED_CUTOFF_MISSING'
need_text "$GATE" 'targetDrawAt > 0L && targetDrawAt < cutoffAt' 'E02D_HISTORICAL_PIT_TIGHTENING_MISSING'
need_text "$GATE" 'source_event_hash=' 'E02D_SOURCE_EVENT_HASH_NOT_AUDITED'
need_text "$GATE" "status='ACTIVE_SHADOW'" 'E02D_ACTIVE_CERTIFICATE_CHECK_MISSING'
need_text "$GATE" 'ResearchCertificateR906.MAY_AUTHORIZE_REAL' 'E02D_CERT_REAL_BOUNDARY_MISSING'
need_absent "$DEEP" 'MAY_AUTHORIZE_REAL = true' 'B11_RESEARCH_REAL_AUTH_FORBIDDEN'

# Deterministic fault matrix must stay executable by the host compile harness.
need_text "$HARNESS" 'BATCH2_CONTRACT_HARNESS_PASS' 'BATCH2_HOST_PASS_MARKER_MISSING'
for token in 'F08 dirty200' 'F09 conflict' 'B08 clock jump' 'F11 duplicate label' 'F12 hidden middle holes' 'F13 unrecoverable snapshot' 'B11 missing certificate evidence rejects BET' 'B05 percentile evidence'; do
  need_text "$HARNESS" "$token" "BATCH2_HARNESS_CASE_MISSING"
done

# B05/E02A hardened clock-domain gate and exact delivery contract.
need_text app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java 'requestMonoAt' 'B05_REQUEST_MONOTONIC_CLOCK_MISSING'
need_text app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java 'acquiredMonoAt' 'B05_LEASE_MONOTONIC_CLOCK_MISSING'
need_text app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java 'lastProgressMonoAt' 'B05_PROGRESS_MONOTONIC_CLOCK_MISSING'
need_text app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java 'normalizeElapsedClockInput' 'B05_WALL_TO_MONOTONIC_COMPATIBILITY_MISSING'
need_text scripts/r927_cumulative_delivery.py '02_COMPLETE_SOURCE' 'BATCH2_TOTAL_PACKAGE_COMPLETE_SOURCE_MISSING'
need_text scripts/r927_cumulative_delivery.py 'MANIFEST_SHA256.txt' 'BATCH2_TOTAL_PACKAGE_MANIFEST_MISSING'

printf 'R926_BATCH2_GATE=PASS|B05=1|B07=1|B08=1|B09=1|B10=1|B11=1|E02B=1|E02C=1|E02D=1|FAULT_MATRIX=1\n'
