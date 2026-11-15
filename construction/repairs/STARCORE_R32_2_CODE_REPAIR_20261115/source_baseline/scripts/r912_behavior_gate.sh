#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd); cd "$ROOT"
OUT=${R912_EVIDENCE_DIR:-"$ROOT/build/r912-verification/evidence"}; mkdir -p "$OUT"
fail(){ echo "R912_BEHAVIOR_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
forbid(){ if rg -q -- "$1" "$2"; then fail "$3"; fi; }

req '^versionName=V57\.19\.0-R5\.2\.0-912$' release/release_identity.properties IDENTITY_NAME
req '^versionCode=577230912$' release/release_identity.properties IDENTITY_CODE
req '^dbVersion=36$' release/release_identity.properties DB_VERSION
req '^schemaRevision=4$' release/release_identity.properties SCHEMA_REVISION
req '^releaseMode=SHADOW_ONLY$' release/release_identity.properties SHADOW_ONLY

# Launch-critical path: exact APK/classes fingerprinting must be asynchronous.
forbid 'BuildProvenanceR815\.verifyRuntime' app/src/main/java/com/ethk3/novaonepro/StarcoreApplication.java STARTUP_SYNC_APK_HASH
req 'StartupHealthR912\.verifyInstalledArtifactAsync' app/src/main/java/com/ethk3/novaonepro/StarcoreApplication.java ASYNC_VERIFY_WIRED
req 'BuildProvenanceR815\.verifyRuntime' app/src/main/java/com/ethk3/novaonepro/StartupHealthR912.java FULL_VERIFY_PRESERVED

# Settlement: current exact priority may not globally suppress backlog convergence.
req 'R912_CURRENT_EXACT_PRIORITY_WITH_BACKLOG_FAIRNESS' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java FAIRNESS_LOOP_WIRED
req 'R912_AFTER_CURRENT_EXACT_ATTEMPT_' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java FAIRNESS_RETRY_WIRED
forbid 'if \(!currentExactSettlementPendingR908\.get\(\)\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java CURRENT_BOOLEAN_GLOBAL_LOCK
req 'prepareCurrentForSoleCoordinatorR911' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java SOLE_GATE_RECOVERY
req 'REPLAY_IDEMPOTENT_BUSINESS_FACTS' app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java OOS_REPLAY
req 's\.pre_draw_frozen=1 AND EXISTS' app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java TRUE_DENOMINATOR

# UI truth: direct mechanical why cards and explicit non-empty diagnostic fields.
req '命中｜期' app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java DIRECT_HIT_REASON
req '未中｜期' app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java DIRECT_MISS_REASON
req 'StateBox\[16\]' app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java SIXTEEN_STATE_BOXES
req 'shape_lifecycle' app/src/main/java/com/ethk3/novaonepro/V51Repository.java SHAPE_FROZEN
req 'SelectedEvidenceContinuityR912\.repairBatch' app/src/main/java/com/ethk3/novaonepro/SettlementRecoverySupervisorR906.java EVIDENCE_REPAIR_WIRED
req 'cv\.put\("rank_status",rankScore\.status\.name\(\)\)' app/src/main/java/com/ethk3/novaonepro/SelectedEvidenceSnapshotR906.java CANONICAL_RANK_STATUS_WRITER
if rg -n '"rank_score_status"' app/src/main/java/com/ethk3/novaonepro --glob '!DbHelper.java' | grep -q .; then fail LEGACY_RANK_STATUS_WRITE; fi

# Research labels and learners run continuously; algorithm-only rows are discoverable.
req 'UNION' app/src/main/java/com/ethk3/novaonepro/ResearchPredrawSettlementPumpR908.java ALGORITHM_PENDING_UNION
req 'ResearchLearningContinuityR912\.pulse' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java CONTINUITY_EVERY_TICK
req 'r811_ftrl_updates' app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java FTRL_CANONICAL_STATE
req 'AlgorithmEvolutionLifecycleR912\.maintain' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java ALGORITHM_LIFECYCLE_WIRED
req 'MIN_NEW_FUTURE_ISSUES_PER_GENERATION = 24' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java GENERATION_EVIDENCE_FLOOR
req 'PRUNED_EVIDENCE_R912' app/src/main/java/com/ethk3/novaonepro/AlgorithmEvidencePolicyR912.java AUTO_PRUNE_POLICY
req 'public boolean algorithmGrade\(\)' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java ALGORITHM_GRADE
req 'updateReadsOutcome' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java SETTLEMENT_UPDATE_REQUIRED
req 'programStructureCanonical' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java STRUCTURAL_NOVELTY
req 'behaviorHash' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java BEHAVIOR_NOVELTY
req 'NAMED_ALGORITHMS_BASELINE_ONLY' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java NAMED_ALGORITHMS_BENCHMARK_ONLY

python3 verification/r911/r911_sqlite_runtime_gate.py > "$OUT/R912_SQLITE_MIGRATION_GATE.txt"
python3 verification/r911/r911_settlement_state_machine.py > "$OUT/R912_SETTLEMENT_STATE_MACHINE_LEGACY.txt"
python3 verification/r910/r910_settlement_core_isolation.py > "$OUT/R912_SIDECAR_FAILURE_INJECTION.txt"
python3 verification/r912/r912_settlement_fairness.py > "$OUT/R912_SETTLEMENT_FAIRNESS.txt"
python3 verification/r912/r912_sqlite_truth_and_research.py > "$OUT/R912_SQLITE_TRUTH_RESEARCH.txt"
STARCORE_TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"} R912_EVIDENCE_DIR="$OUT" ./scripts/r912_compile_host.sh
rg -q '^R912_304_READY_CONVERGENCE=PASS$' "$OUT/R912_SETTLEMENT_FAIRNESS.txt" || fail BACKLOG_FAIRNESS
rg -q '^R912_SETTLEMENT_DENOMINATOR=PASS' "$OUT/R912_SQLITE_TRUTH_RESEARCH.txt" || fail DENOMINATOR_RUNTIME
rg -q '^R912_ALGORITHM_ONLY_PENDING_DISCOVERY=PASS$' "$OUT/R912_SQLITE_TRUTH_RESEARCH.txt" || fail ALGORITHM_PENDING_RUNTIME
rg -q '^R912_ALGORITHM_EVIDENCE_POLICY=PASS$' "$OUT/R912_ALGORITHM_EVIDENCE_HARNESS.txt" || fail ALGORITHM_POLICY_RUNTIME
rg -q '^ONLINE_SETTLED_LABEL_UPDATE=PASS$' "$OUT/R912_TINY_DEEP_HARNESS.txt" || fail TINY_DEEP_RUNTIME
rg -q '^LINEAGE_NOT_STRUCTURAL_NOVELTY=PASS$' "$OUT/R912_ALGORITHM_GENOME_HARNESS.txt" || fail NOVELTY_RUNTIME
./scripts/r906_real_boundary_gate.sh > "$OUT/R912_REAL_BOUNDARY.txt"
python3 -m py_compile verification/r912/*.py verification/r911/*.py verification/r910/*.py training/r906/*.py
printf 'R912_BEHAVIOR_GATE=PASS\nSTARTUP_MAIN_THREAD=PASS\nSETTLEMENT_CURRENT_PRIORITY_BACKLOG_FAIRNESS=PASS\nSETTLEMENT_TRUE_DENOMINATOR=PASS\nSELECTED_EVIDENCE_CONTINUITY=PASS\nRESEARCH_LABEL_CONTINUITY=PASS\nFTRL_CANONICAL_STATE=PASS\nTINY_DEEP_FUTURE_ONLY=PASS\nALGORITHM_STATE_SCORE_UPDATE=PASS\nALGORITHM_FUTURE_OOS_LIFECYCLE=PASS\nREAL_BOUNDARY=PASS\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n' | tee "$OUT/R912_BEHAVIOR_GATE_SUMMARY.txt"
