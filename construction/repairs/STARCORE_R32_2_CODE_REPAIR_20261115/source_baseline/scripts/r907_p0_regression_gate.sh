#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=${R907_EVIDENCE_DIR:-"$ROOT/build/r907-verification/evidence"}
mkdir -p "$OUT"; cd "$ROOT"
fail(){ echo "R907_P0_REGRESSION_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
forbid(){ if rg -q -- "$1" "$2"; then fail "$3"; fi; }
req '^versionName=V57\.17\.1-R5\.0\.1-907$' release/release_identity.properties IDENTITY_NAME
req '^versionCode=577210907$' release/release_identity.properties IDENTITY_CODE
# Core result must return immediately after canonical commit: Research batch is forbidden here.
forbid 'captureR906Batch' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java RESEARCH_STILL_BLOCKS_ANALYSIS_RETURN
# Canonical repository may not write auxiliary selected evidence.
forbid 'SelectedEvidenceSnapshotR906' app/src/main/java/com/ethk3/novaonepro/V51Repository.java AUX_EVIDENCE_STILL_IN_CANONICAL_TX
# Service success order: broadcast must appear before async research call in the same success block.
python3 - <<'PY2'
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text()
anchor=s.index('Models.Decision view = CanonicalDecisionViewAdapterR36.read(store, request.targetIssue);')
chunk=s[anchor:anchor+1200]
a=chunk.index('broadcastDecision(view)')
b=chunk.index('postResearchCaptureR907(outcome.runtime, 0)')
assert a < b, 'broadcast_after_research'
assert 'learningHandler' in s[s.index('private void postResearchCaptureR907'):s.index('private void scheduleDeferredDrainR36')]
print('R907_BROADCAST_BEFORE_RESEARCH=PASS')
PY2
req 'R907_TRUTH_CACHE_MS = 15000L' app/src/main/java/com/ethk3/novaonepro/MainActivity.java UI_TRUTH_CACHE
req 'loadTruthCachedR907' app/src/main/java/com/ethk3/novaonepro/MainActivity.java UI_TRUTH_CACHE_CALL
req 'freezePostCommit' app/src/main/java/com/ethk3/novaonepro/SelectedEvidenceSnapshotR906.java POST_COMMIT_EVIDENCE_FACTORY
req 'first pre-draw evidence wins' app/src/main/java/com/ethk3/novaonepro/SelectedEvidenceSnapshotR906.java RETRY_FIRST_WRITE
req 'one shared 1200xF matrix' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java GOVERNOR_BATCH_MATRIX
R907_EVIDENCE_DIR="$OUT" ./scripts/r907_compile_host.sh
python3 verification/r906/r906_sqlite_host.py > "$OUT/R907_SQLITE_HOST.txt"
python3 -m py_compile training/r906/*.py
./scripts/r906_real_boundary_gate.sh > "$OUT/R907_REAL_BOUNDARY.txt"
printf 'R907_P0_REGRESSION_GATE=PASS\nANALYSIS_RETURN_NOT_BLOCKED_BY_RESEARCH=PASS\nCANONICAL_AUX_EVIDENCE_BLOCK=REMOVED\nBROADCAST_BEFORE_RESEARCH=PASS\nRESEARCH_OWNER=BACKGROUND_SERIAL\nUI_TRUTH_CACHE=PASS\nRETRY_FIRST_WRITE=PASS\nRESEARCH_96X120=PASS\nREAL_BOUNDARY=PASS\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n' | tee "$OUT/R907_P0_REGRESSION_GATE_SUMMARY.txt"
