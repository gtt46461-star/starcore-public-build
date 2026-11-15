#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=${R915_EVIDENCE_DIR:-"$ROOT/build/r915-verification/evidence"}
JAVA="$ROOT/app/src/main/java/com/ethk3/novaonepro"
mkdir -p "$OUT"

fail() {
    echo "R915_REAL_BOUNDARY=FAIL|$1" >&2
    exit 1
}

require() {
    local pattern=$1
    local file=$2
    local code=$3
    rg -q -- "$pattern" "$file" || fail "$code"
}

MANIFEST="$JAVA/V51BuildManifest.java"
require 'MODEL_PROVEN *= *false' "$MANIFEST" MODEL_NOT_PROVEN
require 'REALTIME_PROVEN *= *false' "$MANIFEST" REALTIME_NOT_PROVEN
require 'SHADOW_ONLY *= *false' "$MANIFEST" CONTROLLED_NOT_SHADOW_ONLY
require 'REAL_BET_HARD_DISABLED *= *false' "$MANIFEST" CONTROLLED_NOT_HARD_DISABLED
require 'CANDIDATE_REAL_AUTH_ENABLED *= *true' "$MANIFEST" CANDIDATE_AUTH_ENABLED
require 'R36_REAL_BET_UNLOCK *= *false' "$JAVA/AppConfig.java" LEGACY_GLOBAL_UNLOCK_REMAINS_OFF
require 'R815_REAL_BET_HARD_DISABLED *= *false' "$JAVA/AppConfig.java" CANDIDATE_PATH_NOT_HARD_DISABLED

for source in \
    ResearchMainlineGovernorR903.java \
    AlgorithmInventionLabR908.java \
    AlgorithmProductionGovernorR915.java \
    AssetGenesisEngineR915.java \
    CertifiedResearchMixerR915.java; do
    require 'MAY_AUTHORIZE_REAL *= *false' "$JAVA/$source" "RESEARCH_AUTHORITY_$source"
done

# Research can return scores/ranks only. Any direct Trade writer or authorization call in the new
# invention boundary is a release blocker; comments are excluded to keep the check evidence-based.
SUSPECTS="$OUT/R915_RESEARCH_REAL_BOUNDARY_SUSPECTS.txt"
if rg -n '(insertAuthorizedTrade|saveBetDecisionAsTrade|insertTrade|authorizeReal|writeTrade)' \
        "$JAVA/AlgorithmInventionLabR908.java" \
        "$JAVA/AlgorithmProductionGovernorR915.java" \
        "$JAVA/AssetGenesisEngineR915.java" \
        "$JAVA/CertifiedResearchMixerR915.java" > "$SUSPECTS"; then
    fail RESEARCH_DIRECT_TRADE_API
fi

require 'return expected\.equals\(decision\.authorizationHash\)' \
    "$JAVA/V51AuthorizationCore.java" EXACT_AUTH_SIGNATURE
if rg -n 'authorizationHash.*(startsWith|substring)' "$JAVA/V51AuthorizationCore.java" \
        > "$OUT/R915_FUZZY_AUTH_SUSPECTS.txt"; then
    fail FUZZY_AUTH_SIGNATURE
fi
require 'insertAuthorizationCoreR36' "$JAVA/V51Repository.java" CANONICAL_AUTH_WRITER
require 'insertAuthorizedTradeWithinCanonicalR915' "$JAVA/V51Repository.java" CANONICAL_TRADE_WRITER
require 'insertWithinTransaction' "$JAVA/V51Repository.java" CANONICAL_TRANSACTION_OWNER
require 'R915_REAL_CANONICAL_TRANSACTION_REQUIRED' "$JAVA/DbStore.java" CANONICAL_TRANSACTION_FENCE
require 'R915_REAL_OPEN_TRADE_EXISTS' "$JAVA/DbStore.java" ONE_OPEN_REAL_FENCE

{
    echo 'R915_REAL_BOUNDARY=PASS'
    echo 'MODE=CONTROLLED_CANDIDATE_MODEL_NOT_PROVEN'
    echo 'MODEL_PROVEN=false'
    echo 'REALTIME_PROVEN=false'
    echo 'CANDIDATE_REAL_AUTH_ENABLED=true'
    echo 'RESEARCH_DIRECT_REAL_AUTHORITY=false'
    echo 'AUTH_SIGNATURE=EXACT_SHA256'
    echo 'CANONICAL_AUTH_TRADE=ONE_TRANSACTION'
    echo 'ONE_OPEN_REAL=ENFORCED'
} | tee "$OUT/R915_REAL_BOUNDARY.txt"
