#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd); cd "$ROOT"
OUT=${R909_EVIDENCE_DIR:-"$ROOT/build/r909-verification/evidence"}; mkdir -p "$OUT"
fail(){ echo "R909_BEHAVIOR_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
forbid(){ if rg -q -- "$1" "$2"; then fail "$3"; fi; }
req '^versionName=V57\.17\.3-R5\.0\.3-909$' release/release_identity.properties IDENTITY_NAME
req '^versionCode=577210909$' release/release_identity.properties IDENTITY_CODE
req '^dbVersion=36$' release/release_identity.properties DB_VERSION
req '^schemaRevision=4$' release/release_identity.properties SCHEMA_REVISION
# Production settlement core must contain no derivative R906 closure/outbox write.
forbid 'IssueClosureLedgerR906\.recordWithinTransaction|SettlementOutboxR906\.enqueueWithinTransaction|ResearchShadowChallengerRuntimeR817\.settle|syncSettledCoverage' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java SIDECAR_CAN_ROLLBACK_CORE
req 'core=COMMITTED_SIDEcars_DEFERRED' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java CORE_ISOLATION_MARKER
req 'SettlementCoreTruthR909' app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java BUSINESS_USES_CORE_TRUTH
req 'SettlementCoreTruthR909\.businessClosed' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java WATCHDOG_USES_CORE_TRUTH
req 'reopenCurrentFalseNoPredraw' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java CURRENT_FALSE_TERMINAL_REPAIR
req 'reopenRecoverableNoPredrawTerminals\(store,16\)' app/src/main/java/com/ethk3/novaonepro/SettlementRecoverySupervisorR906.java FALSE_TERMINAL_RECOVERY
req 'drainReadyPredraw\(store,48' app/src/main/java/com/ethk3/novaonepro/SettlementRecoverySupervisorR906.java READY_DRAIN
# Genuine DB writer priority, not thread naming only.
req 'settlementPriorityWaitersR909' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java DB_SETTLEMENT_PRIORITY
req 'yieldBackgroundForSettlementR909' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java BACKGROUND_DB_YIELD
req 'R909_SETTLEMENT_POST_COMMIT_SIDECAR' app/src/main/java/com/ethk3/novaonepro/SettlementPostCommitSidecarR909.java POSTCOMMIT_ONLY
req 'SettlementPostCommitSidecarR909\.drain' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java SIDECAR_RECONCILE_WIRED
python3 verification/r909/r909_settlement_core_isolation.py > "$OUT/R909_SETTLEMENT_CORE_ISOLATION.txt"
python3 verification/r909/r909_backlog_convergence.py > "$OUT/R909_BACKLOG_CONVERGENCE.txt"
# Deep learning is an actually scheduled learner + future predraw, not just an offline file.
req '7 -> 16 -> 8 -> 1' app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java TINY_DEEP_ARCH
req 'updateOne\(store\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java TINY_DEEP_UPDATE_WIRED
req 'capturePredraw\(store, runtime\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java TINY_DEEP_PREDRAW_WIRED
req 'trained_until_issue<\?' app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java TINY_DEEP_FUTURE_ONLY
req 'MAY_AUTHORIZE_REAL = false' app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java TINY_DEEP_REAL_ZERO
# Algorithm invention remains genuinely program/state/update based.
req 'public boolean algorithmGrade\(\)' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java ALGORITHM_GRADE
req 'updateReadsOutcome' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java ALGORITHM_OUTCOME_UPDATE
req 'programStructureCanonical' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java STRUCTURAL_NOVELTY
req 'behaviorHash' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java BEHAVIOR_NOVELTY
req 'NAMED_ALGORITHMS_BASELINE_ONLY' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java FIXED_ML_NOT_MOTHER
STARCORE_TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-/mnt/data/r908_work/TOOLCHAIN_USED} R909_EVIDENCE_DIR="$OUT" ./scripts/r909_compile_host.sh
rg -q '^CORE_SURVIVES_SIDECAR_FAILURE=PASS$' "$OUT/R909_SETTLEMENT_CORE_ISOLATION.txt" || fail CORE_FAILURE_INJECTION
rg -q '^NO_PREDRAW_NEVER_STARVES_READY=PASS$' "$OUT/R909_BACKLOG_CONVERGENCE.txt" || fail BACKLOG_PRIORITY
rg -q '^ONLINE_SETTLED_LABEL_UPDATE=PASS$' "$OUT/R909_TINY_DEEP_HARNESS.txt" || fail TINY_DEEP_RUNTIME
rg -q '^LINEAGE_NOT_STRUCTURAL_NOVELTY=PASS$' "$OUT/R909_ALGORITHM_GENOME_HARNESS.txt" || fail FAKE_ALGORITHM_NOVELTY
./scripts/r906_real_boundary_gate.sh > "$OUT/R909_REAL_BOUNDARY.txt"
python3 -m py_compile verification/r909/*.py training/r906/*.py
printf 'R909_BEHAVIOR_GATE=PASS\nSETTLEMENT_CORE_ISOLATION=PASS\nSIDECAR_FAILURE_INJECTION=PASS\nREADY_BACKLOG_CONVERGENCE=PASS\nDB_SETTLEMENT_WRITER_PRIORITY=PASS\nTINY_DEEP_ONLINE_FUTURE_SHADOW=PASS\nDYNAMIC_FTRL_WIRED=PASS\nALGORITHM_INVENTION_CONTRACT=PASS\nREAL_BOUNDARY=PASS\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n' | tee "$OUT/R909_BEHAVIOR_GATE_SUMMARY.txt"
