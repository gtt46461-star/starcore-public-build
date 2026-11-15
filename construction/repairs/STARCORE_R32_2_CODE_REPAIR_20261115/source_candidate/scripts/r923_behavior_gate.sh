#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=${R923_EVIDENCE_DIR:-"$ROOT/build/r923-verification/evidence"}
AUDIT=${R923_SOURCE_AUDIT_DIR:-"$ROOT/build/r923-verification/source-audit"}
mkdir -p "$OUT" "$AUDIT"
cd "$ROOT"
SUITES=(r923/r923_sqlite_repairs r919/r919_sqlite_truth_closure r915/r915_sqlite_closed_loop
 r914/r914_end_to_end_settlement_gate r911/r911_sqlite_runtime_gate r911/r911_settlement_state_machine
 r910/r910_settlement_core_isolation r912/r912_settlement_fairness r912/r912_sqlite_truth_and_research)
for s in "${SUITES[@]}"; do python3 "verification/$s.py" > "$OUT/${s##*/}.txt"; done
R915_BASE_COMMIT=997457c R915_SOURCE_AUDIT_DIR="$AUDIT" SOURCE_AUDIT_PREFIX=R923 python3 verification/r915/r915_source_substance_audit.py > "$OUT/R923_SOURCE_SUBSTANCE_AUDIT.txt"
R915_EVIDENCE_DIR="$OUT" bash scripts/r915_real_boundary_gate.sh > "$OUT/R923_REAL_BOUNDARY.txt"
R923_EVIDENCE_DIR="$OUT" bash scripts/r923_compile_host.sh
printf 'R923_BEHAVIOR_GATE=PASS|PYTHON_SUITES=9|JAVA_HARNESSES=11|PHYSICAL_DEVICE=NOT_TESTED\n' | tee "$OUT/R923_BEHAVIOR_GATE_SUMMARY.txt"
