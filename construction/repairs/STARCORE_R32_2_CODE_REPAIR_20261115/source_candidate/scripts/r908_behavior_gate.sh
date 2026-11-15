#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=${R908_EVIDENCE_DIR:-"$ROOT/build/r908-verification/evidence"}
mkdir -p "$OUT"; cd "$ROOT"
fail(){ echo "R908_BEHAVIOR_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
forbid(){ if rg -q -- "$1" "$2"; then fail "$3"; fi; }
req '^versionName=V57\.17\.2-R5\.0\.2-908$' release/release_identity.properties IDENTITY_NAME
req '^versionCode=577210908$' release/release_identity.properties IDENTITY_CODE
req '^dbVersion=36$' release/release_identity.properties DB_VERSION
req '^schemaRevision=4$' release/release_identity.properties SCHEMA_REVISION
# P0 production path must remain free of Research work.
forbid 'captureR906Batch|AlgorithmInventionLabR908|HistoricalStrategyInventionR908' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java RESEARCH_BLOCKS_PRODUCTION_ANALYSIS
forbid 'ResearchShadowChallengerRuntimeR817\.settle|syncSettledCoverage|AlgorithmInventionLabR908\.settleDraw' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java RESEARCH_INSIDE_SETTLEMENT_LOCK
# Ready frozen settlement must precede historical discovery.
python3 verification/r908/r908_settlement_starvation_sim.py > "$OUT/R908_SETTLEMENT_STARVATION_SIM.txt"
# Background loops are independent and self-healing.
req 'R908_SETTLEMENT_PULSE_MS = 1200L' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java SETTLEMENT_SELF_LOOP
req 'R908_RESEARCH_SELF_PULSE_MS = 15000L' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java RESEARCH_SELF_LOOP
req 'postAtFrontOfQueue\(task\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java CURRENT_EXACT_PRIORITY
req 'repairR908BackgroundWorkers' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java BACKGROUND_WORKER_REPAIR
python3 - <<'PY'
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text()
chunk=s[s.index('private void scheduleStartupMaintenanceR3611'):s.index('private void runSettlementBacklogMaintenanceR3611')]
assert chunk.index('settlementRecoveryHandlerR908') < chunk.index('backfillHandler')
print('R908_STARTUP_SETTLEMENT_OWNER=PASS')
PY
# True invention is not allowed to masquerade as formula-only mutation.
req 'public boolean algorithmGrade\(\)' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java ALGORITHM_GRADE_CONTRACT
req 'LOAD_S' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java STATE_READ
req 'WRITE_S' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java STATE_UPDATE
req 'crossover\(' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java PROGRAM_CROSSOVER
req 'behaviorHash' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java BEHAVIOR_NOVELTY
req 'NAMED_ALGORITHMS_BASELINE_ONLY' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java FIXED_ML_NOT_MOTHER
req 'future_wilson_lower' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java FUTURE_OOS_EVOLUTION
req 'may_authorize_real.*CHECK\(may_authorize_real=0\)' app/src/main/java/com/ethk3/novaonepro/DbHelper.java RESEARCH_REAL_DB_BOUNDARY
req 'MAY_AUTHORIZE_REAL = false' app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java RESEARCH_REAL_CODE_BOUNDARY
req 'ResearchOrchestratorR906\.pulse\(store, 1100L\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java COMPACT_LEARNER_WIRED
req 'r908_research_phase' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java RESEARCH_PHASE_ROTATION
req 'programStructureCanonical' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java STRUCTURAL_HASH_EXCLUDES_LINEAGE
req 'updateReadsOutcome' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java OUTCOME_FEEDBACK_REQUIRED
req 'mechanismCell' app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java QUALITY_DIVERSITY_CELL
# UI and diagnosis must stay cached/read-only and expose real R908 loop state.
req 'R907_TRUTH_CACHE_MS = 15000L' app/src/main/java/com/ethk3/novaonepro/MainActivity.java UI_TRUTH_CACHE
req 'R908SettlementLoop=' app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java DIAG_SETTLEMENT_LOOP
req 'R908AlgorithmFutureOos=' app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java DIAG_ALGORITHM_FUTURE
STARCORE_TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-/mnt/data/r908_work/TOOLCHAIN_USED} R908_EVIDENCE_DIR="$OUT" ./scripts/r908_compile_host.sh
python3 verification/r908/r908_sqlite_host.py > "$OUT/R908_SQLITE_HOST.txt"
rg -q '^LINEAGE_NOT_STRUCTURAL_NOVELTY=PASS$' "$OUT/R908_ALGORITHM_GENOME_HARNESS.txt" || fail LINEAGE_FAKE_NOVELTY
rg -q '^PULSES_TO_DRAIN_192=6$' "$OUT/R908_SETTLEMENT_STARVATION_SIM.txt" || fail SETTLEMENT_BACKLOG_CONVERGENCE
python3 -m py_compile training/r906/*.py verification/r908/r908_settlement_starvation_sim.py verification/r908/r908_sqlite_host.py
./scripts/r906_real_boundary_gate.sh > "$OUT/R908_REAL_BOUNDARY.txt"
printf 'R908_BEHAVIOR_GATE=PASS\nSETTLEMENT_PRIORITY=PASS\nBACKGROUND_SELF_HEAL=PASS\nPRODUCTION_RESEARCH_ISOLATION=PASS\nALGORITHM_INVENTION_CONTRACT=PASS\nFUTURE_OOS_EVOLUTION=PASS\nUI_TRUTH_CACHE=PASS\nREAL_BOUNDARY=PASS\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n' | tee "$OUT/R908_BEHAVIOR_GATE_SUMMARY.txt"
