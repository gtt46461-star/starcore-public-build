#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd); OUT=${R906_EVIDENCE_DIR:-"$ROOT/build/r906-verification/evidence"}; mkdir -p "$OUT"
fail(){ echo "R906_REAL_BOUNDARY=FAIL|$1" >&2; exit 1; }
rg -q 'SHADOW_ONLY *= *true' "$ROOT/app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java" || fail SHADOW_ONLY
rg -q 'MODEL_PROVEN *= *false' "$ROOT/app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java" || fail MODEL_PROVEN
rg -q 'REAL_BET_HARD_DISABLED *= *true' "$ROOT/app/src/main/java/com/ethk3/novaonepro/AppConfig.java" || fail REAL_HARD_DISABLED
rg -q 'MAY_AUTHORIZE_REAL *= *false' "$ROOT/app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java" || fail GOVERNOR
# Research R906 files must not write Trade/REAL authorization. Mentions in comments/constants are permitted; API invocations are not.
if rg -n --glob '*R906.java' '(createReal|insertReal|authorizeReal|writeTrade|insertTrade|REAL_TRADE)' "$ROOT/app/src/main/java/com/ethk3/novaonepro" > "$OUT/R906_REAL_BOUNDARY_SUSPECTS.txt"; then fail RESEARCH_REAL_WRITE_API; fi
printf 'R906_REAL_BOUNDARY=PASS\nSHADOW_ONLY=true\nMODEL_PROVEN=false\nREAL_BET_HARD_DISABLED=true\nRESEARCH_MAY_AUTHORIZE_REAL=false\n' | tee "$OUT/R906_REAL_BOUNDARY.txt"
