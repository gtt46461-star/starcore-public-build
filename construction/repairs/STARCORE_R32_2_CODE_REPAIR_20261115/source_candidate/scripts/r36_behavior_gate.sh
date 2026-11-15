#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r36-gate.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

cd "$ROOT"

fail() { echo "R36_GATE=FAIL|$1" >&2; exit 1; }
search_text() {
    pattern=$1
    target=$2
    if [ -d "$target" ]; then
        grep -ERq -- "$pattern" "$target"
    else
        grep -Eq -- "$pattern" "$target"
    fi
}
require_text() { search_text "$1" "$2" || fail "$3"; }
forbid_text() { if search_text "$1" "$2"; then fail "$3"; fi; }

# R926+ has one editable release identity source. Never hard-code an old version here.
python3 scripts/r926_release_manifest.py --check >/dev/null || fail RELEASE_MANIFEST_IDENTITY
require_text '"applicationId"[[:space:]]*:[[:space:]]*"com\.ethk3\.novaonepro"' release/release_manifest.json PACKAGE_ID
require_text '"userVersion"[[:space:]]*:[[:space:]]*36' release/release_manifest.json DB_VERSION_MANIFEST
require_text 'sourceCompatibility JavaVersion.VERSION_17' app/build.gradle JAVA17_SOURCE
require_text 'targetCompatibility JavaVersion.VERSION_17' app/build.gradle JAVA17_TARGET
require_text 'OFFICIAL_ANDROID_36_ANDROID_JAR' scripts/r36_apk_abi_gate.sh OFFICIAL_ANDROID_API_PROVENANCE
require_text 'com.android.tools.r8.D8' scripts/r36_build_release.sh MODERN_D8_BACKEND
require_text 'source 17 -target 17' scripts/r36_build_release.sh JAVA17_RELEASE_COMPILER
forbid_text 'source 7' scripts/r36_build_release.sh LEGACY_JAVA7_RELEASE_COMPILER
forbid_text 'com.android.dx' scripts/r36_build_release.sh LEGACY_DX_BACKEND
require_text 'DB_VERSION[[:space:]]*=[[:space:]]*ReleaseManifestR926\.DB_USER_VERSION' app/src/main/java/com/ethk3/novaonepro/AppConfig.java DB_VERSION
require_text 'R36_REAL_BET_UNLOCK = false' app/src/main/java/com/ethk3/novaonepro/AppConfig.java REAL_LOCK
require_text 'MODEL_PROVEN = false' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java MODEL_PROOF
require_text 'SHADOW_ONLY[[:space:]]*=[[:space:]]*false' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java CONTROLLED_CANDIDATE_NOT_SHADOW_ONLY
require_text 'real_action_policy' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_META_REAL_POLICY_KEY
require_text 'V51BuildManifest\.releaseState\(\)' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_META_REAL_POLICY_VALUE
require_text 'TOTAL_ACTIONS[[:space:]]*=[[:space:]]*1200' app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java ACTION_1200
require_text 'validFourthBoundary' app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java R366_SINGLE_N4_CONTRACT_OWNER
require_text 'actionSpaceHashFromPayloadBody' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java R366_ACTION_HASH_PRODUCER_CONSUMER_PARITY
forbid_text 'n4 != 0' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java R366_LEGITIMATE_N4_MUST_NOT_BE_ZERO
require_text 'T45_R366_1200_ACTION_SNAPSHOT_ROUNDTRIP' verification/r36/R36HostContractHarness.java R366_FULL_SNAPSHOT_ROUNDTRIP
require_text 'STAGE_SIZES[[:space:]]*=[[:space:]]*new int\[\][[:space:]]*\{9, 8, 7, 6, 5, 4, 3\}' app/src/main/java/com/ethk3/novaonepro/K3FunnelOrchestrator.java K3_FUNNEL_EXACT_STAGES
require_text 'traceForPosition' app/src/main/java/com/ethk3/novaonepro/V51Repository.java B10_EXACT_FUNNEL_TRACE_PERSISTED
require_text 'K3FunnelOrchestrator\.Audit' app/src/main/java/com/ethk3/novaonepro/V51RuntimeResult.java B10_FUNNEL_AUDIT_IN_RUNTIME_RESULT
require_text 'B14_B15_RESEARCH_DIAGNOSIS_ORCHESTRATOR_ONLY' app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java B14_DIAGNOSIS_OWNER
require_text 'B15_LIVE_DIAGNOSTIC_REPORT_AND_COPY_ONLY' app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java B15_REPORT_OWNER
require_text 'replayDeterminism' app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java B15_REPORT_REPLAY
require_text 'DecisionGateEngine\(\)\.decideV51B12' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java DECISION_OWNER
require_text 'OosAfterDrawWriterR36' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java OOS_OWNER
require_text 'MODEL_INVOCATIONS[[:space:]]*=[[:space:]]*0' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java DEFERRED_NO_MODEL
require_text 'ensureCriticalSchemaOrThrow' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java R363_CRITICAL_SCHEMA_SINGLE_FLIGHT
require_text 'R35_TO_R36_ADDITIVE_ONLY' app/src/main/java/com/ethk3/novaonepro/DbHelper.java R363_DB35_DB36_ADDITIVE_ONLY
require_text 'next_attempt_at INTEGER NOT NULL DEFAULT 0' app/src/main/java/com/ethk3/novaonepro/DbHelper.java R366_ANALYSIS_RETRY_BACKOFF_SCHEMA
require_text 'first_error_code TEXT NOT NULL DEFAULT' app/src/main/java/com/ethk3/novaonepro/DbHelper.java R366_FIRST_ROOT_CAUSE_PRESERVED
require_text 'latestRunnable' app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java R366_ACTIVE_FOREIGN_LEASE_NOT_REQUEUED
require_text 'CLAIM_STALE_TAKEOVER' app/src/main/java/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.java R366_STALE_TAKEOVER_POLICY
require_text 'DEFER_ACTIVE_OWNER' app/src/main/java/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.java R366_ACTIVE_OWNER_DEFER_POLICY
# R947 added pre-draw deadline refresh to this owner. The correctness prohibition
# applies to the durable claim decision, whose inputs remain generation/hash,
# lease/backoff, canonical presence and draw presence. Keep the claim gate active
# without rejecting the separately guarded recovery method.
sed -n '/public static Decision decide(/,/^    }/p' \
    app/src/main/java/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.java > "$TMP/r366-lease-claim.java"
require_text 'public static Decision decide' "$TMP/r366-lease-claim.java" R366_CLAIM_DECISION_MISSING
require_text '^    }' "$TMP/r366-lease-claim.java" R366_CLAIM_DECISION_INCOMPLETE
require_text 'CANONICAL_ALREADY' "$TMP/r366-lease-claim.java" R366_CLAIM_CANONICAL_BOUNDARY_MISSING
require_text 'TARGET_ALREADY_DRAWN' "$TMP/r366-lease-claim.java" R366_CLAIM_DRAW_BOUNDARY_MISSING
forbid_text 'deadlineAt|deadline_at' "$TMP/r366-lease-claim.java" R366_DEADLINE_MUST_NOT_BE_CLAIM_CORRECTNESS_GATE
require_text 'LEASE_FENCE_LOST' app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java R366_DURABLE_OWNER_FENCE
require_text 'ServiceStartupStateR363' app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java R363_STARTUP_TRUTH_CLASSIFIER
require_text 'markActualProcessStart\("AutoFetchService.onCreate"\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R363_ACTUAL_SERVICE_ENTRY_COUNTED
require_text 'DB_CRITICAL_READY' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R363_DB_CRITICAL_STAGE_EXPOSED
require_text 'ensureModuleRegistryR363' app/src/main/java/com/ethk3/novaonepro/DbStore.java R363_BUILD_IDENTITY_NO_FULL_SCHEMA_COUPLING
require_text 'cached.add\(column\)' app/src/main/java/com/ethk3/novaonepro/DbStore.java R363_COLUMN_CACHE_INCREMENTAL
require_text 'last_recovery_evaluation' app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java R363_RECOVERY_RESULT_NOT_OVERWRITTEN
require_text 'V51PermutationFeatureEngine\.historyHash\(frames\)' app/src/main/java/com/ethk3/novaonepro/V51RuntimeInputR36.java R364_CANONICAL_HISTORY_HASH_OWNER
require_text 'prepareAnalysisHistoryR364' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R364_FRESH_INSTALL_HISTORY_BOOTSTRAP
require_text 'PREFERRED_BOOTSTRAP_HISTORY = 100' app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java R364_BOOTSTRAP_100
require_text 'MIN_LIVE_HISTORY = 12' app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java R364_MIN_HISTORY_12
require_text 'MAX_FROZEN_HISTORY = 240' app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java R364_240_IS_MAX_ONLY
require_text 'lastAttemptType' app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java R364_FAILED_ATTEMPT_DIAGNOSIS
require_text 'isAnalysisWorkFresh' app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java R364_WAITING_NOT_FALSE_STALE
require_text 's\.startsWith\("ANALYSIS"\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R364_B13_ANALYSIS_PREFIX
require_text 'rearmKnownR363PreCommitFailureR364' app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java R364_UPGRADE_REQUEST_REARM
require_text 'isKnownR363RepairableError' app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java R364_REARM_ERROR_ALLOWLIST
require_text 'rearmUpgradeFailureR366' app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java R366_OVERWRITE_INSTALL_REARM
require_text 'R366_DURABLE_LEASE_FENCE_LOST' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java R366_COMPUTE_CHECKPOINT_FENCED
require_text 'ANALYSIS_FAILED.*return "ANALYSIS_FAILED"' app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java R366_FAILED_ANALYSIS_WINS_HEALTH_TRUTH
require_text 'leaseOwner' app/src/main/java/com/ethk3/novaonepro/V51Repository.java R366_CANONICAL_COMMIT_FENCED
require_text 'analysisRequestStatus' app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java R366_INFLIGHT_NOT_FALSE_LEDGER_FAIL
require_text 'statusForTarget' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java R366_DEFERRED_STATUS_TARGET_SPECIFIC
sed -n '/public static B12InvariantAudit auditB12/,/^    }/p' \
    app/src/main/java/com/ethk3/novaonepro/V51Repository.java > "$TMP/r366-audit-b12.java"
forbid_text 'backfillB10ProofLedger\(db, target\);' "$TMP/r366-audit-b12.java" R366_DIAGNOSIS_READ_ONLY
forbid_text 'ensure\(' "$TMP/r366-audit-b12.java" R366_B12_AUDIT_NO_SCHEMA_MUTATION
sed -n '/public static DiagnosticStats diagnosticStats/,/^    }/p' \
    app/src/main/java/com/ethk3/novaonepro/V51Repository.java > "$TMP/r366-diagnostic-stats.java"
forbid_text 'ensure\(' "$TMP/r366-diagnostic-stats.java" R366_DIAGNOSTIC_STATS_NO_SCHEMA_MUTATION
sed -n '/public static String statusForTarget/,/^    }/p' \
    app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java > "$TMP/r366-deferred-status.java"
forbid_text 'ensure\(' "$TMP/r366-deferred-status.java" R366_DEFERRED_STATUS_NO_SCHEMA_MUTATION
sed -n '/private void runAnalysisWatchdogOnce()/,/private void runAnalysisWatchdogR36()/p' \
    app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java > "$TMP/r366-watchdog-router.java"
require_text 'useR36Runtime' "$TMP/r366-watchdog-router.java" R366_WATCHDOG_ROUTER_MISSING
require_text 'runAnalysisWatchdogR36' "$TMP/r366-watchdog-router.java" R366_WATCHDOG_ROUTER_NOT_R36
sed -n '/private void runAnalysisWatchdogR36()/,/private List<Models.Draw> loadAnalysisHistory()/p' \
    app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java > "$TMP/r366-active-watchdog.java"
forbid_text 'hardResetAnalysisThreadAndRepost' "$TMP/r366-active-watchdog.java" R366_ACTIVE_WATCHDOG_HARD_RESET_FORBIDDEN
forbid_text 'new HandlerThread' "$TMP/r366-active-watchdog.java" R366_ACTIVE_WATCHDOG_THREAD_REPLACEMENT_FORBIDDEN
forbid_text 'R36_REAL_BET_UNLOCK = true' app/src/main/java/com/ethk3/novaonepro REAL_UNLOCK_TRUE
forbid_text 'putB03Meta\([[:space:]]*db,[[:space:]]*"shadow_only",[[:space:]]*"false"' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_META_SHADOW_FALSE
forbid_text 'putB03Meta\([[:space:]]*db,[[:space:]]*"real_bet_hard_disabled",[[:space:]]*"false"' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_META_REAL_UNLOCKED
forbid_text 'new BusinessReconcilerR35' app/src/main/java/com/ethk3/novaonepro ACTIVE_R35_RECONCILER
forbid_text 'AnalysisSingleFlightR35[[:space:]]*\.[[:space:]]*shared' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java ACTIVE_R35_SINGLE_FLIGHT
forbid_text 'markActualProcessStart\("startWork"\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R363_DOUBLE_ACTUAL_START_COUNTER
forbid_text 'new int\[\][[:space:]]*\{10, 9, 7, 5, 4, 3\}' app/src/main/java/com/ethk3/novaonepro B10_OLD_INCONSISTENT_FUNNEL_STAGES

require_text 'lastAuthoritativeUiSignalAt' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R367_UI_FRESHNESS_FENCE
forbid_text 'renderLightSnapshot\(snapshot\.light\)' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R367_HEAVY_MUST_NOT_REPAINT_LIVE
require_text 'Models.Decision pending = db.getLatestDecision\(\)' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R367_PENDING_DECISION_VISIBLE
require_text 'isLatestDrawStaleForRecovery' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R367_STALE_LATEST_WATCHDOG
require_text 'DRAW_DATA_STALE' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R367_B13_DATA_FRESHNESS_TRUTH
require_text 'latestDrawAge' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R367_B13_LATEST_DRAW_AGE_EXPOSED
require_text 'settlementTerminalizedCount' app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java R367_SETTLEMENT_TERMINAL_BROADCAST
require_text 'SettlementTaskStoreR36\.terminal\(store, issue\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R367_SETTLEMENT_TRUTH_FROM_DURABLE_TASK
require_text 'repairActivationBoundaryFromCanonicalWithinTransaction' app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java R367_COVERAGE_CANONICAL_BOUNDARY
require_text 'r922_activation_base_issue' app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java R925_COVERAGE_ACTIVATION_BOUNDARY
require_text 'deferredDrainPostedR367' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R367_DEFERRED_CATCHUP
require_text 'R36_DEFERRED_LEASE_MS = 20000L' app/src/main/java/com/ethk3/novaonepro/AppConfig.java R367_DEFERRED_RECOVERY_LEASE
require_text 'R36_SETTLEMENT_LEASE_MS = 15000L' app/src/main/java/com/ethk3/novaonepro/AppConfig.java R367_SETTLEMENT_RECOVERY_LEASE


ANDROID=${ANDROID_PLATFORM_JAR:-}
TC=${STARCORE_TOOLCHAIN_ROOT:-"$ROOT/../08_TOOLCHAIN"}
if [ -z "$ANDROID" ] && [ -f "$TC/android-platform-36/android.jar" ]; then ANDROID="$TC/android-platform-36/android.jar"; fi
if [ -z "$ANDROID" ] && [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -f "$ANDROID_SDK_ROOT/platforms/android-36/android.jar" ]; then ANDROID="$ANDROID_SDK_ROOT/platforms/android-36/android.jar"; fi
if [ -z "$ANDROID" ] && [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/platforms/android-36/android.jar" ]; then ANDROID="$ANDROID_HOME/platforms/android-36/android.jar"; fi
if [ -z "$ANDROID" ] && command -v sdkmanager >/dev/null 2>&1; then
    yes | sdkmanager "platforms;android-36" >/dev/null 2>&1 || true
    SDK_BASE=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
    if [ -n "$SDK_BASE" ] && [ -f "$SDK_BASE/platforms/android-36/android.jar" ]; then ANDROID="$SDK_BASE/platforms/android-36/android.jar"; fi
fi
[ -n "$ANDROID" ] && [ -f "$ANDROID" ] || fail ANDROID_36_JAR_MISSING
find app/src/main/java -name '*.java' -print | sort > "$TMP/app.list"
# These are actual Gradle-generated product sources and resource symbols, not
# hand-written verification replacements. Bootstrap the Android compile first.
GENERATED_SEMANTICS=app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java
GENERATED_PROVENANCE=app/build/generated/source/r815/com/ethk3/novaonepro/BuildSourceIdentityCP12.java
RESOURCE_JAR=app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar
test -f "$GENERATED_SEMANTICS" && test -f "$GENERATED_PROVENANCE" && test -f "$RESOURCE_JAR" \
    || fail CURRENT_ANDROID_GENERATED_INPUTS_MISSING
python3 scripts/r967_semantic_identity.py --check || fail CURRENT_SEMANTIC_IDENTITY_DRIFT
printf '%s\n' "$GENERATED_SEMANTICS" "$GENERATED_PROVENANCE" >> "$TMP/app.list"
mkdir -p "$TMP/classes" "$TMP/android-runtime" "$TMP/host-adapters"
unzip -q "$ANDROID" '*.class' -d "$TMP/android-runtime"
java -m jdk.compiler/com.sun.tools.javac.Main -d "$TMP/host-adapters" \
    verification/r960/host/android/os/SystemClock.java || fail HOST_CLOCK_ADAPTER_COMPILE
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID:$RESOURCE_JAR" -d "$TMP/classes" @"$TMP/app.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    >"$TMP/javac.stdout" 2>"$TMP/javac.stderr" || {
        sed -n '1,240p' "$TMP/javac.stderr" >&2
        fail FULL_SOURCE_COMPILE
    }
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 \
    -cp "$ANDROID:$TMP/classes" -d "$TMP/classes" verification/r36/R36HostContractHarness.java \
    >"$TMP/harness-javac.stdout" 2>"$TMP/harness-javac.stderr" || {
        sed -n '1,160p' "$TMP/harness-javac.stderr" >&2
        fail HOST_HARNESS_COMPILE
    }
java -cp "$TMP/host-adapters:$TMP/classes:$RESOURCE_JAR:$TMP/android-runtime" com.ethk3.novaonepro.R36HostContractHarness || fail HOST_HARNESS_RUNTIME

echo "R36_JAVA17_FULL_SOURCE_COMPILE=PASS"
echo "R36_STATIC_REDLINE_GATE=PASS"
echo "R36_GATE=PASS"
