#!/usr/bin/env bash
# Host/static acceptance gate for the R817 release tree. The historical filename is retained for
# compatibility; it deliberately does not claim physical-device coverage.
set -euo pipefail

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASE_ROOT=${STARCORE_BASE_ROOT:-"$ROOT/../full813/STARCORE_TIANSHU_V57.15.3-R3.6.10-813_FULL_ALL_IN_ONE"}
TOOLCHAIN_ROOT=${STARCORE_TOOLCHAIN_ROOT:-"$BASE_ROOT/TOOLCHAIN_USED"}
ANDROID_JAR=${STARCORE_ANDROID_JAR:-"$TOOLCHAIN_ROOT/android-platform-36/android.jar"}
R8_JAR=${STARCORE_R8_JAR:-"$TOOLCHAIN_ROOT/r8-9.2.23/r8-9.2.23.jar"}
APKTOOL_JAR=${STARCORE_APKTOOL_JAR:-"$TOOLCHAIN_ROOT/apktool/apktool.jar"}
AAPT2=${STARCORE_AAPT2:-"$TOOLCHAIN_ROOT/aapt/bin/x64/linux/aapt2"}
EVIDENCE_DIR=${R817_EVIDENCE_DIR:-${R816_EVIDENCE_DIR:-${R815_EVIDENCE_DIR:-"$ROOT/build/r817-verification/evidence"}}}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r817-gate.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
cd "$ROOT"

fail() { echo "R817_GATE=FAIL|$1" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "MISSING:$2"; }
require_text() { rg -q -- "$1" "$2" || fail "$3"; }
forbid_text() { if rg -q -- "$1" "$2"; then fail "$3"; fi; }

require_file "$ANDROID_JAR" ANDROID_PLATFORM_36
require_file "$R8_JAR" R8_D8
require_file "$APKTOOL_JAR" APKTOOL
require_file "$AAPT2" AAPT2
require_file release/release_identity.properties RELEASE_IDENTITY
require_file scripts/r815_generate_provenance.sh PROVENANCE_GENERATOR

R817_TOOLCHAIN_FILES="$ANDROID_JAR|$R8_JAR|$APKTOOL_JAR|$AAPT2" \
    scripts/r815_generate_provenance.sh > "$TMP/provenance.log"

# Immutable identity, explicit release-key gate, and non-negotiable research-only boundary.
require_text '^applicationId=com\.ethk3\.novaonepro$' release/release_identity.properties PACKAGE_ID
require_text '^versionName=V57\.16\.2-R4\.0\.2-902$' release/release_identity.properties VERSION_NAME
require_text '^versionCode=577200902$' release/release_identity.properties VERSION_CODE
require_text '^compileSdk=36$' release/release_identity.properties COMPILE_SDK
require_text '^minSdk=21$' release/release_identity.properties MIN_SDK
require_text '^targetSdk=28$' release/release_identity.properties TARGET_SDK
require_text '^dbVersion=36$' release/release_identity.properties DB_VERSION
require_text '^schemaRevision=3$' release/release_identity.properties SCHEMA_REVISION
require_text '^gameCode=Binance1-PK10$' release/release_identity.properties GAME_CODE
require_text '^releaseMode=SHADOW_ONLY$' release/release_identity.properties RELEASE_MODE
require_text 'R818_RELEASE_DUOQU_API_KEY_REQUIRED' app/build.gradle RELEASE_KEY_REQUIRED
require_text 'injectedApiKey ==~ /\[A-Za-z0-9\]\{8,128\}/' app/build.gradle RELEASE_KEY_FORMAT
require_text 'sourceCompatibility JavaVersion.VERSION_17' app/build.gradle JAVA17_SOURCE
require_text 'targetCompatibility JavaVersion.VERSION_17' app/build.gradle JAVA17_TARGET
require_text '"V57\.16\.2-R4\.0\.2-902"' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java BUILD_MANIFEST_VERSION
require_text 'VERSION_CODE = 577200902' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java BUILD_MANIFEST_CODE
require_text 'R815_SCHEMA_REVISION = 3' app/src/main/java/com/ethk3/novaonepro/AppConfig.java R815_SCHEMA_REVISION
require_text 'R815_SHADOW_ONLY = true' app/src/main/java/com/ethk3/novaonepro/AppConfig.java SHADOW_LOCK
require_text 'R815_MODEL_PROVEN = false' app/src/main/java/com/ethk3/novaonepro/AppConfig.java MODEL_NOT_PROVEN
require_text 'R815_REAL_BET_HARD_DISABLED = true' app/src/main/java/com/ethk3/novaonepro/AppConfig.java REAL_LOCK
forbid_text 'R815_SHADOW_ONLY = false|R815_MODEL_PROVEN = true|R815_REAL_BET_HARD_DISABLED = false|MODEL_PROVEN = true|SHADOW_ONLY = false|REAL_BET_HARD_DISABLED = false' app/src/main/java/com/ethk3/novaonepro
if rg -q --glob '*.java' 'V57\.15\.3-R3\.6\.11-814|577190823' app/src/main/java/com/ethk3/novaonepro; then
    fail STALE_R814_IDENTITY
fi
if rg -q --glob '*.java' 'V57\.15\.3-R3\.6\.12-815|577190824' app/src/main/java/com/ethk3/novaonepro; then
    fail STALE_R815_RELEASE_IDENTITY
fi
if rg -q --glob '*.java' 'V57\.15\.3-R3\.6\.13-816|577190825' app/src/main/java/com/ethk3/novaonepro; then
    fail STALE_R816_RELEASE_IDENTITY
fi
if rg -q --glob '*.java' '__SOURCE_TREE_SHA256__|__RESOURCE_MANIFEST_SHA256__|__TOOLCHAIN_SHA256__|__GENERATED_AT_UTC__' app/src/main/java/com/ethk3/novaonepro; then
    fail PROVENANCE_PLACEHOLDER
fi

# R820 regression closures: schema starvation, orphan settlement leases, real why-truth UI and formula catalog.
require_text 'criticalSchemaStage = "R820_RESEARCH_SCHEMA"' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java R820_RESEARCH_SCHEMA_STARTUP
require_text 'recoverOrphanedRunningLeasesOnProcessStart' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R820_SETTLEMENT_STARTUP_RECOVERY_CALL
require_text 'recoverImpossibleRunningLeasesWhenIdle' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java R900_SETTLEMENT_IDLE_GATE_RECOVERY
require_text 'WhyDecisionTruthR820.buildSummary' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R820_WHY_TRUTH_WIRED
require_text '为什么中 / 为什么不中 / 为什么不出手' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R820_WHY_CARD_VISIBLE
forbid_text 'parent.getChildCount\(\) == 1 && parent.getChildAt\(0\) instanceof LinearLayout' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R820_OLD_REASON_ROW_REUSE_FORBIDDEN
require_text 'ResearchCandidateFormulaLibraryR3611.SEEDS.length' app/src/main/java/com/ethk3/novaonepro/StrategyChallengerFactoryEngine.java R820_FORMULA_LIBRARY_FACTORY_WIRED
require_text 'FORMULA_COUNT = ResearchCandidateFormulaLibraryR3611.SEEDS.length' app/src/main/java/com/ethk3/novaonepro/ResearchStrategyCatalogR820.java R820_FORMULA_CATALOG
require_text 'AppConfig\.R36_RUNTIME_REPAIR_VERSION\.equals\(existingVersion\)' app/src/main/java/com/ethk3/novaonepro/RuntimeReleaseEpochR817.java R820_VERSION_SCOPED_RUNTIME_EPOCH
require_text 'MAX_LOCAL_HISTORY_SAVE = 10000' app/src/main/java/com/ethk3/novaonepro/AppConfig.java R900_HISTORY_10000
require_text 'ResearchEvolutionLifecycleR900.maintain' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R900_RESEARCH_LIFECYCLE_WIRED
require_text 'PRUNED_R900_INEFFECTIVE' app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionLifecycleR900.java R900_RESEARCH_PRUNE
require_text 'KEEP_ACTIVE_R900' app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionLifecycleR900.java R900_RESEARCH_KEEP
require_text 'COUNT\(p.target_issue\) ASC' app/src/main/java/com/ethk3/novaonepro/ResearchShadowChallengerRuntimeR817.java R900_RESEARCH_ROTATION
require_text 'DRAW_HISTORY_RENDER_LIMIT = 30' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R900_HISTORY_HOME_30
require_text 'DbHelper.TABLE_DRAWS' app/src/main/java/com/ethk3/novaonepro/HistoryActivity.java R900_HISTORY_DRAW_FIRST
require_text 'drainPredrawBacklog' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java R901_PREDRAW_SETTLEMENT_DRAIN
require_text 'seedHistorical\(store, 48\)' app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java R901_PREDRAW_SETTLEMENT_SEED_BEFORE_ANALYSIS
require_text 'r901_watchdog_settlement_drained' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R901_WATCHDOG_SETTLEMENT_CATCHUP
require_text 'writeOneSettledCatchup' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R902_DEFERRED_SETTLED_CATCHUP
require_text 'r902_watchdog_gap_count' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R902_GAP_VISIBLE_AND_FORCE_BACKFILL
require_text 'renderLightReasonTruthR902' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R902_WHY_TRUTH_LIGHT_RENDER
require_text 'R900_V51_SELECTED_EVIDENCE' app/src/main/java/com/ethk3/novaonepro/CanonicalDecisionViewAdapterR36.java R902_SELECTED_EVIDENCE_FALLBACK
require_text 'R902主链接入真值' app/src/main/java/com/ethk3/novaonepro/ModelLearningActivity.java R902_RESEARCH_CHAIN_VISIBLE
require_text 'decision_broadcast_r901' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R901_UI_SINGLE_RENDER_OWNER
require_text 'setLines\(stableLines\)' app/src/main/java/com/ethk3/novaonepro/MainActivity.java R901_UI_FIXED_TEXT_LINES
require_text 'android:lines="4"' app/src/main/res/layout/activity_main.xml R901_UI_FIXED_DECISION_SUMMARY_LINES

# Android exposure and recovery boundaries.
require_text 'android:name="\.InternalWatchdogReceiverR815"' app/src/main/AndroidManifest.xml INTERNAL_WATCHDOG_DECLARED
require_text 'android:exported="false"' app/src/main/AndroidManifest.xml INTERNAL_COMPONENT_NONEXPORTED
require_text 'android:name="\.BootReceiver"' app/src/main/AndroidManifest.xml BOOT_OWNER_DECLARED
require_text 'android:foregroundServiceType="dataSync"' app/src/main/AndroidManifest.xml DATA_SYNC_FGS
forbid_text 'FOREGROUND_SERVICE_SPECIAL_USE|foregroundServiceType="[^"]*specialUse' app/src/main/AndroidManifest.xml SPECIAL_USE_FORBIDDEN
require_text 'R815_INTERNAL_WATCHDOG' app/src/main/java/com/ethk3/novaonepro/InternalWatchdogReceiverR815.java WATCHDOG_ACTION
require_text 'RecoveryCoordinatorR815' app/src/main/java/com/ethk3/novaonepro/BootReceiver.java RECOVERY_OWNER
require_text 'FETCH_SETTLEMENT_ONLY' app/src/main/java/com/ethk3/novaonepro/RecoveryCoordinatorR815.java RECOVERY_ESCALATION

# Required non-shell R815 modules and explicit core ownership routes.
for module in \
    ReleaseIdentityR817 RuntimeReleaseEpochR817 ProviderTimeReconcilerR817 \
    DrawHistoryProjectionR817 DrawRawPayloadCompactorR817 ResearchShadowChallengerRuntimeR817 \
    ReleaseIdentityR816 RuntimeReleaseEpochR816 ProviderTimeCodecR816 \
    ReleaseIdentityR815 BuildProvenanceR815 SourceManifestR815 CriticalPathErrorLedgerR815 \
    SchemaUpgradeCoordinatorR815 IssueFreshnessResolverR815 HistoryIntegrityVerifierR815 \
    PredictionAvailabilityR815 PredictionOutputContractR815 ProductionPredictionDispatcherR815 \
    CrossPositionPrimitiveCacheR815 ResearchFeatureSchemaR815 ResearchFeatureVectorR815 \
    ResearchFeatureMaterializerR815 DatasetIdentityGateR815 SettlementCoordinatorR815 SettlementExecutionGateR819 WhyDecisionTruthR820 ResearchStrategyCatalogR820 \
    ShadowObservationSidecarR815 RecoveryCoordinatorR815 InternalWatchdogReceiverR815 \
    RuntimeOwnerAuditR815 LongTextStoreR815 DiagnosticSnapshotR815 ResearchCandidateExecutorRegistryR815 \
    ResearchFailureAttributionR815 ResearchAblationR815 ResearchRepositoryR815 \
    ResearchPromotionVerifierR815 ResearchSchedulerR815
do
    source_file="app/src/main/java/com/ethk3/novaonepro/$module.java"
    require_file "$source_file" "R815_MODULE:$module"
    lines=$(wc -l < "$source_file" | tr -d ' ')
    [ "$lines" -ge 20 ] || fail "R815_MODULE_TOO_SHORT:$module"
done
require_text 'ensureR815Schema' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_R815_SCHEMA
require_text 'first_seen_at' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_FIRST_SEEN
require_text 'draw_hash_v2' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DB_V2_HASH
require_text 'R815_HISTORY_RETENTION_MIN' app/src/main/java/com/ethk3/novaonepro/DbStore.java RETENTION_1440
require_text 'canonicalV2' app/src/main/java/com/ethk3/novaonepro/DrawHashR36.java BUSINESS_HASH_V2
require_text 'PredictionOutputContractR815.requireCanonicalReady' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java OUTPUT_GATE_BEFORE_COMMIT
require_text 'new V51RuntimeOrchestrator\(\)\.runR36' app/src/main/java/com/ethk3/novaonepro/ProductionPredictionDispatcherR815.java SINGLE_PREDICTION_DISPATCHER
runtime_calls=$(rg -l --glob '*.java' 'new V51RuntimeOrchestrator\(' app/src/main/java/com/ethk3/novaonepro | wc -l | tr -d ' ')
[ "$runtime_calls" = "1" ] || fail PREDICTION_RUNTIME_OWNER_COUNT
if rg -q --glob '*.java' '\.settleExactR36\(|\.settleV51Strict\(|\.settleV51ByDraw\(' app/src/main/java/com/ethk3/novaonepro; then
    fail SETTLEMENT_BYPASS_REFERENCE
fi
execute_calls=$(rg -l --glob '*.java' 'executeFromCoordinatorR815' app/src/main/java/com/ethk3/novaonepro | wc -l | tr -d ' ')
[ "$execute_calls" = "2" ] || fail SETTLEMENT_EXECUTION_OWNER_COUNT
require_text 'settleV51Compatibility' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java SETTLEMENT_COMPAT_COORDINATED
require_text 'onProductionSettled' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java SHADOW_SIDECAR_POST_TERMINAL
require_text 'contiguousSuffixAscending' app/src/main/java/com/ethk3/novaonepro/HistoryIntegrityVerifierR815.java RECENT_SUFFIX_FREEZE
require_text 'R817_HISTORY_CONTIGUOUS_SUFFIX_LT_12' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java HISTORY_FAIL_FAST
require_text 'drainCurrent\(store, 16\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java LIVE_SETTLEMENT_DRAIN
require_text 'drainBacklog\(store, 8\)' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java BACKLOG_SETTLEMENT_DRAIN
require_text 'SettlementExecutionGateR819\.tryEnter' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java SETTLEMENT_PROCESS_SINGLE_FLIGHT
require_text 'retireLegacyBacklogWithoutPredraw' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java LEGACY_BACKLOG_AUDITED_TERMINAL
require_text 'hasActiveCriticalWork' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java DEFERRED_NO_QUIET_WINDOW_STARVATION
require_text 'R819_APPLICATION_PROCESS_REARM' app/src/main/java/com/ethk3/novaonepro/StarcoreApplication.java APPLICATION_RECOVERY_REARM
require_text 'ProviderTimeCodecR816\.parse' app/src/main/java/com/ethk3/novaonepro/ApiParser.java UTC_PARSER_DELEGATION
require_text 'ProviderTimeReconcilerR817\.prefer' app/src/main/java/com/ethk3/novaonepro/DbStore.java PROVIDER_CURRENT_SELF_HEAL
require_text 'DrawHistoryProjectionR817\.COLUMNS' app/src/main/java/com/ethk3/novaonepro/DbStore.java HISTORY_PROJECTION_NO_RAW
require_text 'draw\.rawJson = obj\.toString\(\)' app/src/main/java/com/ethk3/novaonepro/ApiParser.java RAW_JSON_PER_DRAW_ONLY
require_text 'recordValidatedFetchResponseR817' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java FETCH_RECEIPT_HEARTBEAT
require_text 'providerCorroborated' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java PROVIDER_HEALTH_CORROBORATION
require_text 'first_seen_at' app/src/main/java/com/ethk3/novaonepro/SettlementBacklogAuditR3611.java PIT_FIRST_SEEN_AUDIT
forbid_text 'd\.open_time>o\.commit_at' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java OPEN_TIME_PIT_DUPLICATION
require_text 'R36_DEFERRED_ACTION_CHUNK = 120' app/src/main/java/com/ethk3/novaonepro/AppConfig.java DEFERRED_CHUNK_120
require_text 'scope=R900_CURRENT_RELEASE' app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java HEALTH_RELEASE_SCOPE
require_text 'readUiStateSnapshotR816\(map, 2048, 4096\)' app/src/main/java/com/ethk3/novaonepro/MainActivity.java UI_BATCH_STATE_READ
require_text 'TABLE_R817_RESEARCH_PREDRAW' app/src/main/java/com/ethk3/novaonepro/DbHelper.java RESEARCH_PREDRAW_SCHEMA
require_text 'ResearchShadowChallengerRuntimeR817\.capture' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java RESEARCH_PREDRAW_CAPTURE
require_text 'ResearchShadowChallengerRuntimeR817\.settle' app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java RESEARCH_AFTER_DRAW_LABEL
require_text 'MAY_AUTHORIZE_REAL = false' app/src/main/java/com/ethk3/novaonepro/ResearchShadowChallengerRuntimeR817.java RESEARCH_REAL_LOCK
require_text 'ResearchAdaptiveLearnerStoreR811\.loadForTarget' app/src/main/java/com/ethk3/novaonepro/ResearchShadowChallengerRuntimeR817.java DYNAMIC_FTRL_PIT_LOAD
require_text 'predictResearch' app/src/main/java/com/ethk3/novaonepro/ResearchShadowChallengerRuntimeR817.java DYNAMIC_FTRL_PREDRAW_CONSUMER
require_text 'DrawRawPayloadCompactorR817\.compactBatch' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java RAW_PAYLOAD_BOUNDED_COMPACTION

# Full source quality scan: no generated placeholders, secret material, or empty R815 catch block.
if find . -path './build' -prune -o -type f \( -iname '*.p12' -o -iname '*.jks' -o -iname '*credentials*' \) -print | grep -q .; then
    fail SOURCE_CONTAINS_SIGNING_MATERIAL
fi
if rg -q --glob '!build/**' 'DUOQU_API_KEY_[AB][[:space:]]*=[[:space:]]*"[A-Za-z0-9]{8,}"' .; then
    fail SOURCE_CONTAINS_EMBEDDED_API_KEY
fi
if rg -U -q --glob '*R81[567].java' 'catch\s*\([^)]*\)\s*\{\s*\}' app/src/main/java/com/ethk3/novaonepro; then
    fail EMPTY_RELEASE_CATCH
fi
if rg -q --glob '*R81[567].java' 'TODO|FIXME|PLACEHOLDER' app/src/main/java/com/ethk3/novaonepro; then
    fail RELEASE_PLACEHOLDER_TOKEN
fi

# Compile once against Android 36 and once with host-capable stubs, then execute R36/R811/R815
# regression contracts. A host pass is intentionally labelled host-only, not a device claim.
find app/src/main/java -type f -name '*.java' -print | sort > "$TMP/app.list"
mkdir -p "$TMP/official-classes" "$TMP/host-classes"
java -m jdk.compiler/com.sun.tools.javac.Main \
    -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID_JAR" -d "$TMP/official-classes" \
    @"$TMP/app.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    verification/r36-stubs/com/ethk3/novaonepro/R.java \
    >"$TMP/official-javac.log" 2>&1 \
    || { sed -n '1,260p' "$TMP/official-javac.log" >&2; fail OFFICIAL_ANDROID36_COMPILE; }
find verification/r36-stubs -name '*.java' ! -path '*/com/ethk3/novaonepro/MainActivity.java' -print | sort > "$TMP/stubs.list"
java -m jdk.compiler/com.sun.tools.javac.Main \
    -encoding UTF-8 -source 17 -target 17 -d "$TMP/host-classes" \
    @"$TMP/stubs.list" @"$TMP/app.list" \
    verification/r36/R36HostContractHarness.java \
    verification/r811/R811HostContractHarness.java \
    verification/r815/R815HostContractHarness.java \
    verification/r816/R816RuntimeClosureHarness.java \
    verification/r816/R816CorePerformanceHarness.java \
    verification/r817/R817RuntimeResearchClosureHarness.java \
    >"$TMP/host-javac.log" 2>&1 \
    || { sed -n '1,260p' "$TMP/host-javac.log" >&2; fail HOST_COMPILE; }
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R36HostContractHarness > "$TMP/r36-host.log"
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R811HostContractHarness > "$TMP/r811-host.log"
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R815HostContractHarness > "$TMP/r815-host.log"
rg -q '^R815_HOST_CONTRACTS=PASS' "$TMP/r815-host.log" || fail R815_HOST_CONTRACTS
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R816RuntimeClosureHarness > "$TMP/r816-host.log"
rg -q '^R816_RUNTIME_CLOSURE=PASS' "$TMP/r816-host.log" || fail R816_RUNTIME_CLOSURE
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R817RuntimeResearchClosureHarness > "$TMP/r817-host.log"
rg -q '^R817_RUNTIME_RESEARCH_CLOSURE=PASS' "$TMP/r817-host.log" || fail R817_RUNTIME_RESEARCH_CLOSURE
java -Xmx512m -cp "$TMP/host-classes" com.ethk3.novaonepro.R816CorePerformanceHarness > "$TMP/r816-performance.log"
rg -q '^R816_CORE_PERFORMANCE=PASS' "$TMP/r816-performance.log" || fail R816_CORE_PERFORMANCE
java -Xmx512m -cp "$TMP/host-classes" com.ethk3.novaonepro.R816CorePerformanceHarness 240 > "$TMP/r816-performance-240.log"
rg -q '^R816_CORE_PERFORMANCE=PASS\|HISTORY=240\|' "$TMP/r816-performance-240.log" || fail R816_CORE_PERFORMANCE_240
java -Xmx192m -cp "$TMP/host-classes" com.ethk3.novaonepro.R816CorePerformanceHarness 96 > "$TMP/r817-low-heap.log"
rg -q '^R816_CORE_PERFORMANCE=PASS\|HISTORY=96\|' "$TMP/r817-low-heap.log" || fail R817_LOW_HEAP_192M

mkdir -p "$EVIDENCE_DIR"
cp "$TMP/provenance.log" "$EVIDENCE_DIR/R817_PROVENANCE.log"
cp "$TMP/official-javac.log" "$EVIDENCE_DIR/R817_OFFICIAL_ANDROID36_JAVAC.log"
cp "$TMP/host-javac.log" "$EVIDENCE_DIR/R817_HOST_JAVAC.log"
cp "$TMP/r36-host.log" "$EVIDENCE_DIR/R817_R36_REGRESSION_HOST.log"
cp "$TMP/r811-host.log" "$EVIDENCE_DIR/R817_R811_REGRESSION_HOST.log"
cp "$TMP/r815-host.log" "$EVIDENCE_DIR/R815_HOST_CONTRACTS.log"
cp "$TMP/r816-host.log" "$EVIDENCE_DIR/R817_R816_REGRESSION_CLOSURE.log"
cp "$TMP/r817-host.log" "$EVIDENCE_DIR/R817_RUNTIME_RESEARCH_CLOSURE.log"
cp "$TMP/r816-performance.log" "$EVIDENCE_DIR/R817_CORE_PERFORMANCE_96.log"
cp "$TMP/r816-performance-240.log" "$EVIDENCE_DIR/R817_CORE_PERFORMANCE_240.log"
cp "$TMP/r817-low-heap.log" "$EVIDENCE_DIR/R817_LOW_HEAP_192M.log"
source_count=$(wc -l < "$TMP/app.list" | tr -d ' ')
class_count=$(find "$TMP/official-classes" -type f -name '*.class' | wc -l | tr -d ' ')
android_sha=$(sha256sum "$ANDROID_JAR" | awk '{print $1}')
{
    echo 'R817_GATE=PASS'
    echo "OFFICIAL_ANDROID36_COMPILE=PASS|ANDROID_JAR_SHA256=$android_sha"
    echo "JAVA_SOURCE_FILES=$source_count|CLASS_FILES=$class_count|SOURCE=17|TARGET=17"
    cat "$TMP/r36-host.log"
    cat "$TMP/r811-host.log"
    cat "$TMP/r815-host.log"
    cat "$TMP/r816-host.log"
    cat "$TMP/r817-host.log"
    cat "$TMP/r816-performance.log"
    cat "$TMP/r816-performance-240.log"
    echo "R817_LOW_HEAP_192M=PASS|$(cat "$TMP/r817-low-heap.log")"
    echo 'STATIC_OWNERSHIP=PASS|PREDICTION=ONE_DISPATCHER|SETTLEMENT=COORDINATOR_ONLY'
    echo 'R817_RESEARCH_BOUNDARY=EXECUTABLE_PREDRAW_SHADOW|PIT_FTRL_CONSUMER|FUTURE_LIVE_LABELS|REAL_AUTHORITY=0'
    echo 'R817_TRUTH_BOUNDARY=HOST_AND_BUILD_ONLY|PHYSICAL_DEVICE_NOT_CLAIMED|MODEL_NOT_PROVEN'
} | tee "$EVIDENCE_DIR/R817_BEHAVIOR_GATE_SUMMARY.txt"
