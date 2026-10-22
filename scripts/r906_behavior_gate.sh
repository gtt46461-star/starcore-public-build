#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd); OUT=${R906_EVIDENCE_DIR:-"$ROOT/build/r906-verification/evidence"}; mkdir -p "$OUT"; cd "$ROOT"
fail(){ echo "R906_ENGINEERING_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
req '^versionName=V57\.17\.0-R5\.0\.0-906$' release/release_identity.properties IDENTITY_NAME
req '^versionCode=577210906$' release/release_identity.properties IDENTITY_CODE
req '^dbVersion=36$' release/release_identity.properties DB_VERSION
req '^schemaRevision=4$' release/release_identity.properties SCHEMA_REVISION
req 'ensureR906Schema\(db\)' app/src/main/java/com/ethk3/novaonepro/DbHelper.java SCHEMA_OWNER
req 'closeCurrentExactR906' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java EXACT_SETTLEMENT
req 'SettlementRecoverySupervisorR906\.shared\(\)\.pulse' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java SUPERVISOR
req 'RuntimeTruthSnapshotR906' app/src/main/java/com/ethk3/novaonepro/MainActivity.java TRUTH_SNAPSHOT
req 'WhyTruthSectionRendererR906' app/src/main/java/com/ethk3/novaonepro/MainActivity.java WHY_OWNER
req 'ResearchCompiledGraphRuntimeR906' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java EXECUTABLE_RESEARCH_MAINLINE
req 'ACTIVE_SHADOW' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java CERTIFICATE_GATE
./scripts/r906_compile_host.sh
python3 verification/r906/r906_sqlite_host.py > "$OUT/R906_SQLITE_HOST.txt"
python3 -m py_compile training/r906/*.py
./scripts/r906_real_boundary_gate.sh
# Static source-contract prohibitions.
if rg -n 'dynamicK|K4_REAL|martingale|doubling|frequency[_ -]?debt' app/src/main/java/com/ethk3/novaonepro/*R906.java > "$OUT/R906_FORBIDDEN_SCAN.txt"; then fail FORBIDDEN_PLAY_CONTRACT; fi
printf 'R906_ENGINEERING_GATE=PASS\nHOST_JAVA17=PASS\nHOST_SQLITE_MODEL=PASS\nRESEARCH_96X120=PASS\nMETHOD_PARITY_JAVA=PASS\nUI_LATEST_WINS_HOST=PASS\nREAL_BOUNDARY=PASS\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\nDEVICE_360_SOAK=NOT_RUN_NO_DEVICE\n' | tee "$OUT/R906_ENGINEERING_GATE_SUMMARY.txt"
