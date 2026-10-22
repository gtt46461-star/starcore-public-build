#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../TOOLCHAIN_USED"}
ANDROID=${STARCORE_ANDROID_JAR:-"$TC/android-platform-36/android.jar"}
EVIDENCE=${R905_EVIDENCE_DIR:-"$ROOT/build/r905-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r905-gate.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
cd "$ROOT"
fail(){ echo "R905_GATE=FAIL|$1" >&2; exit 1; }
req(){ rg -q -- "$1" "$2" || fail "$3"; }
mkdir -p "$TMP/classes" "$EVIDENCE"
req '^versionName=V57\.16\.5-R4\.0\.5-905$' release/release_identity.properties VERSION
req '^versionCode=577200905$' release/release_identity.properties VERSION_CODE
req '^dbVersion=36$' release/release_identity.properties DB_CONTINUITY
req 'reopenRecoverableNoPredrawTerminals' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java FALSE_TERMINAL_RECOVERY
req 'o\.pre_draw_frozen=1 AND o\.settled=0' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java PREDRAW_RECOVERY_PROOF
req 'drainReadyPredraw\(store, 64, 2200L\)' app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java URGENT_ALL_CLASS_DRAIN
req 'if \(running\) runSettlementBacklogMaintenanceR3611' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java SETTLEMENT_NOT_PRESSURE_STARVED
req 'r905_new_draw_settlement_first' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java DRAW_SETTLEMENT_FIRST
req 'R905_PREDRAW_SETTLE_FAILED' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java RESEARCH_SETTLEMENT_INDEPENDENT
req 'store, result, 24, 1200L' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java RESEARCH_COHORT_24
req 'generateMethodBlueprintsR905' app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionLifecycleR900.java METHOD_BLUEPRINT_LIFECYCLE
req 'NOVA_EVIDENCE_WEAVE_' app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java INVENTED_WEAVE
req 'NOVA_CAUSAL_SURVIVAL_LATTICE_' app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java INVENTED_LATTICE
req 'new String\[\] \{"为什么中", "为什么不中", "为什么不出手", "逐期结算统计"\}' app/src/main/java/com/ethk3/novaonepro/MainActivity.java WHY_TITLES
req 'lastWhyTruthRenderKeyR905' app/src/main/java/com/ethk3/novaonepro/MainActivity.java UI_JANK_CACHE
req 'buildCompactSummary' app/src/main/java/com/ethk3/novaonepro/WhyDecisionTruthR820.java WHY_COMPACT_TRUTH
req 'WhyDecisionTruthR820.buildCompactSummary' app/src/main/java/com/ethk3/novaonepro/MainActivity.java WHY_HOME_COMPACT
req 'rawAction = V51Math\.clamp' app/src/main/java/com/ethk3/novaonepro/V51Repository.java RAW_P_SYNCHRONOUS_FALLBACK
req 'predictability\.stability' app/src/main/java/com/ethk3/novaonepro/V51Repository.java STABILITY_SYNCHRONOUS_FALLBACK
if rg -q 'MODEL_PROVEN = true|SHADOW_ONLY = false|REAL_BET_HARD_DISABLED = false' app/src/main/java/com/ethk3/novaonepro; then fail REAL_BOUNDARY_CHANGED; fi
find app/src/main/java -name '*.java' -print | sort > "$TMP/sources.list"
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID" -d "$TMP/classes" @"$TMP/sources.list" verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java verification/r36-stubs/com/ethk3/novaonepro/R.java > "$TMP/javac.log" 2>&1 || fail JAVAC
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "$TMP/classes:$ANDROID" -d "$TMP/classes" verification/r903/R905RepairHarness.java >> "$TMP/javac.log" 2>&1 || fail HARNESS_COMPILE
java -cp "$TMP/classes:$ANDROID" com.ethk3.novaonepro.R905RepairHarness > "$TMP/harness.log" 2>&1 || fail HARNESS
cp "$TMP/javac.log" "$TMP/harness.log" "$EVIDENCE/"
printf 'R905_GATE=PASS\nSETTLEMENT_CONTINUITY=PASS_STATIC_HOST\nRESEARCH_CREATION=PASS_HOST\nWHY_UI_AND_JANK=PASS_STATIC\nJAVA17=PASS\nREAL_BOUNDARY=PASS\n' | tee "$EVIDENCE/R905_GATE_SUMMARY.txt"
