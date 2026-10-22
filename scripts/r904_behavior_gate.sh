#!/usr/bin/env bash
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
ANDROID=${STARCORE_ANDROID_JAR:-"$TC/android-platform-36/android.jar"}
R8=${STARCORE_R8_JAR:-"$TC/r8-9.2.23/r8-9.2.23.jar"}
EVIDENCE=${R904_EVIDENCE_DIR:-"$ROOT/build/r904-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r904-gate.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
cd "$ROOT"

fail() { echo "R904_GATE=FAIL|$1" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "MISSING:$2"; }
require_text() { rg -q -- "$1" "$2" || fail "$3"; }
forbid_text() { if rg -q -- "$1" "$2"; then fail "$3"; fi; }

require_file "$ANDROID" ANDROID_PLATFORM_36
require_file "$R8" R8_D8
mkdir -p "$TMP/classes" "$TMP/d8" "$EVIDENCE"

require_text '^versionName=V57\.16\.4-R4\.0\.4-904$' release/release_identity.properties VERSION_NAME
require_text '^versionCode=577200904$' release/release_identity.properties VERSION_CODE
require_text '^dbVersion=36$' release/release_identity.properties DB_CONTINUITY
require_text 'drainReadyPredraw\(store, 48, 1200L\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java ALL_CLASS_DRAIN_WIRED
require_text 'R903_DRAIN_ALL_READY_PREDRAW' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java READY_PREDRAW_OWNER
require_text "t.status IN \('PENDING','RETRY_WAIT'\)" app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java RETRY_WAIT_INCLUDED
require_text 'isTransientSettlementFailure' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java TRANSIENT_RETRY_POLICY
require_text 'captureBatch\(' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java MULTI_CANDIDATE_WIRED
require_text 'NOT EXISTS\(SELECT 1 FROM' app/src/main/java/com/ethk3/novaonepro/ResearchShadowChallengerRuntimeR817.java SAME_ISSUE_DUPLICATE_GUARD
require_text 'drainBatch\(store, 4, 1200L\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java RESEARCH_BATCH_SETTLEMENT
require_text 'R903_STRUCTURAL_SYNTHESIS' app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionLifecycleR900.java STRUCTURAL_CREATION_LIFECYCLE
require_text 'R903_METHOD_GRAPH_SYNTHESIS_' app/src/main/java/com/ethk3/novaonepro/StrategyChallengerFactoryEngine.java METHOD_FAMILY_CREATION
require_text 'MARKOV_REGIME' app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java MARKOV_FAMILY
require_text 'ONLINE_FOREST' app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java FOREST_FAMILY
require_text 'TEMPORAL_NEURAL' app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java NEURAL_FAMILY
require_text 'NOVA_RESIDUAL_TRANSPORT_' app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java NOVEL_METHOD_FAMILY
require_text 'ResearchMainlineGovernorR903\.govern' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java CERTIFIED_MAINLINE_WIRED
require_text 'MIN_FUTURE_N = 120' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java MAINLINE_MIN_FUTURE_N
require_text 'MIN_WILSON_LOWER = 0\.30d' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java MAINLINE_WILSON_GATE
require_text 'MAX_BRIER = 0\.21d' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java MAINLINE_BRIER_GATE
require_text 'o\.target_issue<\?' app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java MAINLINE_PIT_BOUNDARY
require_text 'renderWhyTruthCardsR903' app/src/main/java/com/ethk3/novaonepro/MainActivity.java RICH_WHY_CARDS
require_text 'captureSummary\(truthStore\)' app/src/main/java/com/ethk3/novaonepro/MainActivity.java LIGHTWEIGHT_PERIODIC_TRUTH
require_text 'UI_RUNTIME_TRUTH_REFRESH_MS = 15000L' app/src/main/java/com/ethk3/novaonepro/MainActivity.java TRUTH_REFRESH_THROTTLED
require_text 'requestR818TruthDetail\(kind\)' app/src/main/java/com/ethk3/novaonepro/MainActivity.java FULL_DETAIL_CLICK_ONLY
require_text 'BUILD_VERSION_NAME' app/src/main/java/com/ethk3/novaonepro/ReleaseIdentityR817.java TYPED_IDENTITY_DIAGNOSTIC
require_text '原始动作K3概率（未校准）' app/src/main/java/com/ethk3/novaonepro/MainActivity.java RAW_PROBABILITY_HONEST_LABEL
require_text '号码熵风险：N/A（冻结字段缺失）' app/src/main/java/com/ethk3/novaonepro/MainActivity.java RISK_NA_EXPLICIT
forbid_text 'R815_SHADOW_ONLY = false|R815_MODEL_PROVEN = true|R815_REAL_BET_HARD_DISABLED = false|MODEL_PROVEN = true|SHADOW_ONLY = false|REAL_BET_HARD_DISABLED = false' app/src/main/java/com/ethk3/novaonepro REAL_BOUNDARY_CHANGED

find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    verification/r36-stubs/com/ethk3/novaonepro/R.java > "$TMP/javac.log" 2>&1 || fail JAVAC
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$TMP/classes:$ANDROID" -d "$TMP/classes" verification/r903/R904RepairHarness.java \
    >> "$TMP/javac.log" 2>&1 || fail HARNESS_COMPILE
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R904RepairHarness \
    > "$TMP/harness.log" 2>&1 || fail HARNESS
(cd "$TMP/classes" && zip -q -r "$TMP/classes.jar" .)
java -cp "$R8" com.android.tools.r8.D8 --release --min-api 21 --lib "$ANDROID" \
    --output "$TMP/d8" "$TMP/classes.jar" > "$TMP/d8.log" 2>&1 || fail D8
test -s "$TMP/d8/classes.dex" || fail DEX_EMPTY

cp "$TMP/javac.log" "$TMP/harness.log" "$TMP/d8.log" "$EVIDENCE/"
sha256sum "$TMP/d8"/classes*.dex > "$EVIDENCE/R904_DEX_SHA256.txt"
printf 'R904_GATE=PASS\nIDENTITY=V57.16.4-R4.0.4-904/577200904\nSETTLEMENT_ALL_CLASS=PASS\nRESEARCH_BATCH_AND_CREATION=PASS\nWHY_AND_RISK_UI=PASS\nJAVA17_D8=PASS\nREAL_BOUNDARY=PASS\n' \
    | tee "$EVIDENCE/R904_GATE_SUMMARY.txt"
