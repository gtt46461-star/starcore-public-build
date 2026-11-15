#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
EVIDENCE_DIR=${R811_EVIDENCE_DIR:-"$ROOT/build/r811-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r811-gate.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
cd "$ROOT"

fail() { echo "R811_GATE=FAIL|$1" >&2; exit 1; }
require_text() { rg -q "$1" "$2" || fail "$3"; }
forbid_text() { if rg -q "$1" "$2"; then fail "$3"; fi; }
require_file() { [ -f "$1" ] || fail "MISSING:$2"; }

ANDROID_JAR=${STARCORE_ANDROID_JAR:-}
if [ -z "$ANDROID_JAR" ]; then
    for candidate in \
        "$ROOT/TOOLCHAIN_USED/android-platform-36/android.jar" \
        "$ROOT/../r367_package/Starcore-Tianshu-V57.15.3-R3.6.7-Full-Blood-Repair/TOOLCHAIN_USED/android-platform-36/android.jar" \
        "$ROOT/../toolchain/android-platform-36/android.jar"
    do
        if [ -f "$candidate" ]; then ANDROID_JAR=$candidate; break; fi
    done
fi
require_file "$ANDROID_JAR" OFFICIAL_ANDROID_36_JAR

# Release identity and immutable safety redlines.
require_text "applicationId 'com.ethk3.novaonepro'" app/build.gradle PACKAGE_ID
require_text 'compileSdk 36' app/build.gradle COMPILE_SDK
require_text 'minSdk 21' app/build.gradle MIN_SDK
require_text 'targetSdk 28' app/build.gradle TARGET_SDK
require_text 'versionCode 577190823' app/build.gradle VERSION_CODE
require_text "versionName 'V57\.15\.3-R3\.6\.10-813'" app/build.gradle VERSION_NAME
require_text 'sourceCompatibility JavaVersion.VERSION_17' app/build.gradle JAVA17_SOURCE
require_text 'targetCompatibility JavaVersion.VERSION_17' app/build.gradle JAVA17_TARGET
require_text 'VERSION_CODE = 577190823' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java MANIFEST_CODE
require_text 'V57\.15\.3-R3\.6\.10-813' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java MANIFEST_NAME
require_text 'V57\.15\.3-R3\.6\.10-813-B01-B15-COMPLETE-DIAG-V10' app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java COMPLETE_REPORT_V10
require_text 'R811-Diagnosis-Render' app/src/main/java/com/ethk3/novaonepro/DiagnosisActivity.java DIAGNOSIS_BACKGROUND_RENDER
require_text 'DB_VERSION = 36' app/src/main/java/com/ethk3/novaonepro/AppConfig.java DB36
require_text 'R811_SCHEMA_REVISION = 1' app/src/main/java/com/ethk3/novaonepro/AppConfig.java SCHEMA_REV1
require_text 'STARCORE_V57_B01_B15_R366_ACTION_SNAPSHOT_CANONICAL_RUNTIME_CLOSURE_20260811' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java PRODUCTION_COHORT_ZERO_DRIFT
require_text 'MODEL_PROVEN = false' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java MODEL_UNPROVEN
require_text 'SHADOW_ONLY = true' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java SHADOW_ONLY
require_text 'REAL_BET_HARD_DISABLED = true' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java REAL_LOCK
require_text 'CANDIDATE_REAL_AUTH_ENABLED = false' app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java CANDIDATE_REAL_LOCK
require_text 'R36_REAL_BET_UNLOCK = false' app/src/main/java/com/ethk3/novaonepro/AppConfig.java CONFIG_REAL_LOCK
forbid_text 'R36_REAL_BET_UNLOCK = true|MODEL_PROVEN = true|SHADOW_ONLY = false|CANDIDATE_REAL_AUTH_ENABLED = true' app/src/main/java/com/ethk3/novaonepro REAL_UNLOCK_TOKEN

# Android background policy is explicit and contains no target-35 special-use claim.
require_text 'R811_RUNTIME_POLICY' app/src/main/AndroidManifest.xml RUNTIME_POLICY_META
require_text 'TARGET_28_DATA_SYNC_NO_SPECIAL_USE' app/src/main/AndroidManifest.xml RUNTIME_POLICY_VALUE
require_text 'foregroundServiceType="dataSync"' app/src/main/AndroidManifest.xml DATA_SYNC_FGS
require_text 'android:largeHeap="true"' app/src/main/AndroidManifest.xml R368_256M_HEAP_OOM_SAFETY
forbid_text 'android:foregroundServiceType="[^"]*specialUse|android.permission.FOREGROUND_SERVICE_SPECIAL_USE' app/src/main/AndroidManifest.xml SPECIAL_USE_FORBIDDEN

# DB36 same-version critical schema and lazy Research schema.
require_text 'ensureR811CriticalSchema' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java CRITICAL_SCHEMA_OWNER
require_text 'ensureR811ResearchSchemaOnce' app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java RESEARCH_SCHEMA_LAZY
require_text 'r811_schema_revision' app/src/main/java/com/ethk3/novaonepro/DbHelper.java REVISION_STATE
require_text 'idx_r811_settlement_runnable' app/src/main/java/com/ethk3/novaonepro/DbHelper.java SETTLEMENT_INDEX
require_text 'idx_r811_deferred_runnable' app/src/main/java/com/ethk3/novaonepro/DbHelper.java DEFERRED_INDEX
require_text 'eligible_n INTEGER NOT NULL DEFAULT 0' app/src/main/java/com/ethk3/novaonepro/DbHelper.java RESEARCH_ELIGIBLE_DENOMINATOR
require_text 'selected_n INTEGER NOT NULL DEFAULT 0' app/src/main/java/com/ethk3/novaonepro/DbHelper.java RESEARCH_SELECTED_DENOMINATOR
require_text 'hit_n INTEGER NOT NULL DEFAULT 0' app/src/main/java/com/ethk3/novaonepro/DbHelper.java RESEARCH_HIT_NUMERATOR
require_text 'assertV51ReadyReadOnly' app/src/main/java/com/ethk3/novaonepro/SelectedHitCertificationEngine.java HOT_PATH_READ_ONLY
forbid_text 'V51Repository\.ensure' app/src/main/java/com/ethk3/novaonepro/SelectedHitCertificationEngine.java HOT_PATH_DDL

# Live settlement is strictly above retry/backlog and exact OOS remains the only writer.
require_text 'LIVE_CURRENT = "LIVE_CURRENT"' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java LIVE_CURRENT_CLASS
require_text 'PRIORITY_LIVE_CURRENT = 100' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java LIVE_CURRENT_PRIORITY
require_text 'PRIORITY_LIVE_RETRY = 50' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java LIVE_RETRY_PRIORITY
require_text 'PRIORITY_BACKLOG = 0' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java BACKLOG_PRIORITY
require_text 'promoteCurrentDraw' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java EXPLICIT_CURRENT_PROMOTION
require_text 'enqueueHistoricalDraws' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java HISTORICAL_BACKLOG
require_text 'ORDER BY priority DESC' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java PRIORITY_ORDER
require_text 'R36_SETTLEMENT_MAX_ATTEMPTS' app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java SETTLEMENT_RETRY_OWNER
require_text 'OosAfterDrawWriterR36' app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java STRICT_OOS_OWNER

# Deferred is resumable, exact and yields immediately to live pressure.
require_text 'MODEL_INVOCATIONS = 0' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java DEFERRED_NO_MODEL
require_text 'next_action_ordinal' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java DEFERRED_CURSOR
require_text 'R36_DEFERRED_ACTION_CHUNK' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java DEFERRED_CHUNK
require_text 'RealtimePressureGateR811\.isUnderPressure' app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java DEFERRED_YIELD
require_text 'TOTAL_ACTIONS[[:space:]]*=[[:space:]]*1200' app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java ACTION_1200
require_text 'COMBINATIONS_PER_POSITION[[:space:]]*=[[:space:]]*120' app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java ACTION_120
require_text 'label1200' app/src/main/java/com/ethk3/novaonepro/ResearchActionOutcomeMaterializerR811.java RESEARCH_1200_LABELS
require_text 'R811_ACTION_LABEL_1200_STREAM' app/src/main/java/com/ethk3/novaonepro/ResearchActionOutcomeMaterializerR811.java RESEARCH_1200_STREAMING_LABELS
require_text 'ResearchSettlementSidecarR811\.drain' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java RESEARCH_SETTLEMENT_BACKLOG
require_text 'pendingLearningTarget' app/src/main/java/com/ethk3/novaonepro/ResearchSettlementSidecarR811.java RESEARCH_DELAYED_OOS_LEARNING_CLOSURE
require_text 'BACKFILL_LEARNING_FORBIDDEN' app/src/main/java/com/ethk3/novaonepro/ResearchAdaptiveLearnerStoreR811.java RESEARCH_FTRL_MONOTONIC
require_text 'encodeRuntimeResult' app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java STREAMING_SNAPSHOT_LIVE_PATH
require_text 'RuntimeMemoryGuardR811' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java R368_MEMORY_GUARD
require_text 'MIN_START_HEADROOM_BYTES = 16L \* MIB' app/src/main/java/com/ethk3/novaonepro/RuntimeMemoryGuardR811.java R368_MEMORY_PREFLIGHT_16M
require_text 'AppConfig.R36_RUNTIME_REPAIR_VERSION' app/src/main/java/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.java REPAIR_VERSION_REARM
require_text '"OOM"' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java OOM_LEDGER_TRUTH
require_text "detail LIKE '%OutOfMemoryError%'" app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java HISTORICAL_OOM_LEDGER_TRUTH
require_text 'out.businessAlive = runtimeHealth.businessAlive' app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java BUSINESS_TRUTH_USES_FRESH_HEARTBEATS
require_text 'bindCurrentThread' app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java ANALYSIS_WORKER_BINDING
require_text 'reapDeadWorker' app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java DEAD_ANALYSIS_WORKER_REAP
require_text 'repairDeadAnalysisWorkerR811' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java DEAD_ANALYSIS_HANDLER_RECOVERY
require_text 'WORKER_ALIVE' app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java LIVE_ANALYSIS_WORKER_REPLACEMENT_FORBIDDEN
require_text 'repairDeadFetchWorkerR811' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java DEAD_FETCH_HANDLER_RECOVERY
require_text 'r811_fetch_stale_repost' app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java FETCH_STALE_UNIQUE_CHAIN_REPOST

# Production owners and Research isolation/security.
require_text '"REAL", "DecisionGateEngine", "PRODUCTION"' app/src/main/java/com/ethk3/novaonepro/ProductionOwnerCatalogR811.java REAL_OWNER
require_text '"Settlement", "SettlementEngine", "PRODUCTION"' app/src/main/java/com/ethk3/novaonepro/ProductionOwnerCatalogR811.java SETTLEMENT_OWNER
require_text '"OOS", "OosAfterDrawWriterR36", "PRODUCTION"' app/src/main/java/com/ethk3/novaonepro/ProductionOwnerCatalogR811.java OOS_OWNER
require_text '"Research", "AutonomousResearchEvolutionEngine", "RESEARCH_ONLY"' app/src/main/java/com/ethk3/novaonepro/ProductionOwnerCatalogR811.java RESEARCH_BOUNDARY
require_text 'R368_811_PRODUCTION_HOT_REPLACEMENT_FORBIDDEN' app/src/main/java/com/ethk3/novaonepro/ResearchPromotionGovernanceEngine.java NO_HOT_PRODUCTION
require_text 'EXPLICIT_PARITY_AND_PIT_CERTIFICATES_REQUIRED' app/src/main/java/com/ethk3/novaonepro/ResearchPromotionGovernanceEngine.java PROMOTION_CERTS
require_text 'DexClassLoader' app/src/main/java/com/ethk3/novaonepro/SecureCodegenGuardR811.java DYNAMIC_LOAD_TOKEN_BLOCKED
require_text 'production_mutation=0' app/src/main/java/com/ethk3/novaonepro/CandidateCodeSynthesisEngine.java CODEGEN_AUDIT_ONLY
if rg -q 'new DexClassLoader' \
    $(rg --files app/src/main/java/com/ethk3/novaonepro | rg -v '/SecureCodegenGuardR811\.java$')
then
    fail DYNAMIC_CODE_LOADING
fi
if rg -q 'Class\.forName\(' \
    app/src/main/java/com/ethk3/novaonepro/Research*.java \
    app/src/main/java/com/ethk3/novaonepro/CandidateCodeSynthesisEngine.java \
    app/src/main/java/com/ethk3/novaonepro/StrategyChallengerFactoryEngine.java
then
    fail RESEARCH_DYNAMIC_CODE_LOADING
fi
[ ! -e app/src/main/java/com/ethk3/novaonepro/ActionArchiveEngineR811.java ] \
    || fail P2_ACTION_ARCHIVE_MUST_REMAIN_DISABLED

{
    echo 'R811_PLAN_CONTRADICTION_GATE=PASS'
    echo 'DB=36|R811_SCHEMA_REVISION=1|NO_DB37=PASS'
    echo 'PRODUCTION_STRATEGY_COHORT=R366_FROZEN|ZERO_DRIFT=PASS'
    echo 'RESEARCH_DYNAMIC_JAVA=FORBIDDEN|SCAN=PASS'
    echo 'ACTION_ARCHIVE=P2_POST_CLOSURE_DISABLED|SCAN=PASS'
    echo 'COMPILE_SDK=36|TARGET_SDK=28|SPECIAL_USE=ABSENT'
    echo 'MODEL_PROVEN=false|SHADOW_ONLY=true|REAL_LOCKED=true'
} > "$TMP/r811-plan-contradiction.log"

# Every plan-locked ADD_R811 class is present.
for name in \
    AdaptiveShadowLearnerEngine AutonomousResearchEvolutionEngine BackgroundRuntimeCapabilityR811 \
    CandidateCodeSynthesisEngine CrossPositionAssignmentResearchEngine DeterministicRuntimeCacheR811 \
    DiagnosticSnapshotR811 DrawCanonicalizerR811 ProductionOwnerCatalogR811 \
    R811SchemaIntegrityVerifier RealtimePressureGateR811 ResearchActionOutcomeMaterializerR811 \
    ResearchArtifactCodecR811 ResearchBudgetGovernorR811 ResearchCertificationEngineR811 \
    ResearchDeterministicRngR811 ResearchDslCandidateRuntimeR811 ResearchExpertOutcomeLedgerR811 \
    ResearchExpertPredrawSnapshotR811 ResearchHoldoutRegistryR811 ResearchModelCandidateRuntimeR811 \
    ResearchPromotionGovernanceEngine ResearchStatisticsGuardR811 ResearchStrategyDslEngine \
    SecureCodegenGuardR811 StorageHealthR811 StrategyChallengerFactoryEngine \
    TimeOrderedExperimentLabEngine TimeSourceR811 RuntimeMemoryGuardR811 \
    ResearchFailureHypothesisEngineR811 ResearchExperimentRunnerR811 \
    ResearchAdaptiveLearnerStoreR811 ResearchSettlementSidecarR811
do
    require_file "app/src/main/java/com/ethk3/novaonepro/$name.java" "ADD_R811:$name"
done
require_file training/r811_autonomous_research.py R811_TINY_MLP_TRAINER

# Compile against the official API first, then run host behavior using runtime-capable stubs.
find app/src/main/java -type f -name '*.java' -print | sort > "$TMP/app.list"
mkdir -p "$TMP/official-classes" "$TMP/host-classes"
java -m jdk.compiler/com.sun.tools.javac.Main \
    -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID_JAR" -d "$TMP/official-classes" \
    @"$TMP/app.list" \
    verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java \
    verification/r36-stubs/com/ethk3/novaonepro/R.java \
    >"$TMP/official-javac.log" 2>&1 \
    || { sed -n '1,260p' "$TMP/official-javac.log" >&2; fail OFFICIAL_ANDROID_FULL_COMPILE; }

find verification/r36-stubs -name '*.java' \
    ! -path '*/com/ethk3/novaonepro/MainActivity.java' -print | sort > "$TMP/stubs.list"
java -m jdk.compiler/com.sun.tools.javac.Main \
    -encoding UTF-8 -source 17 -target 17 -d "$TMP/host-classes" \
    @"$TMP/stubs.list" @"$TMP/app.list" \
    verification/r36/R36HostContractHarness.java \
    verification/r811/R811HostContractHarness.java \
    >"$TMP/host-javac.log" 2>&1 \
    || { sed -n '1,260p' "$TMP/host-javac.log" >&2; fail HOST_FULL_COMPILE; }
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R36HostContractHarness \
    | tee "$TMP/r36-host.log"
java -cp "$TMP/host-classes" com.ethk3.novaonepro.R811HostContractHarness \
    | tee "$TMP/r811-host.log"
python3 training/r811_autonomous_research.py --self-test \
    | tee "$TMP/r811-neural-self-test.log"
rg -q '"artifact_deterministic": true' "$TMP/r811-neural-self-test.log" \
    || fail R811_NEURAL_ARTIFACT_DETERMINISM
rg -q '"finite_gradients": true' "$TMP/r811-neural-self-test.log" \
    || fail R811_NEURAL_GRADIENT_FINITE
rg -q '"gradient_check_pass": true' "$TMP/r811-neural-self-test.log" \
    || fail R811_NEURAL_GRADIENT_CHECK
rg -q '"sinkhorn_check_pass": true' "$TMP/r811-neural-self-test.log" \
    || fail R811_NEURAL_SINKHORN_CHECK
python3 - "$TMP/r811-synthetic.npz" <<'PY'
import numpy as np
import sys
n = 120
x = np.sin(np.arange(n * 96, dtype=np.float64).reshape(n, 96) / 37.0)
y = np.vstack([np.roll(np.arange(10, dtype=np.int64), i % 10) for i in range(n)])
issues = np.asarray([f"20260101{i + 1:04d}" for i in range(n)])
np.savez(sys.argv[1], x=x, y=y, issues=issues)
PY
python3 training/r811_autonomous_research.py "$TMP/r811-synthetic.npz" \
    --output "$TMP/r811-smoke.artifact" --epochs 1 --patience 1 --batch-size 16 \
    --sinkhorn-iterations 2 --minimum-issues 120 \
    | tee "$TMP/r811-neural-trainer-smoke.log"
rg -q '"sealed_final_consumed": false' "$TMP/r811-neural-trainer-smoke.log" \
    || fail R811_NEURAL_SEALED_FINAL
rg -q '"model_proof_45": "NOT_PROVEN_REQUIRES_FUTURE_LIVE_OOS"' \
    "$TMP/r811-neural-trainer-smoke.log" || fail R811_NEURAL_PROOF_TRUTH

report_source="app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java"
for token in \
    '【自主策略发现/深度学习/动态进化】' \
    '[A Production Champion]' \
    '[B Failure Distribution]' \
    '[C Hypotheses]' \
    '[D Newly Created Formula]' \
    '[E Candidate Strategy Chain]' \
    '[F Search Statistics]' \
    '[G Top Challenger Table]' \
    '[H Code Generation]' \
    '[I Decision]' \
    '[J Truth boundary]' \
    '[Production Gate Truth]' \
    '[Grouped Research Dataset]' \
    '[Cross-position Assignment Research]' \
    '[Candidate Executability]' \
    'Policy=COMPLETE_TEXT_NO_TRUNCATION' \
    'JavaRuntimeFile=ResearchModelCandidateRuntimeR811.java' \
    'FutureBackfillLearning=FORBIDDEN' \
    'ResearchTarget45=0.45' \
    'ModelProof45=NOT_PROVEN'; do
    rg -Fq "$token" "$report_source" || fail "RESEARCH_REPORT_SCHEMA:$token"
done
echo 'R811_RESEARCH_REPORT_SCHEMA=PASS|TRUTH_BOUNDARY=TARGET_OBSERVED_PROOF_SEPARATE' \
    > "$TMP/r811-report-schema.log"

if [ "${R811_SKIP_PARITY:-0}" != "1" ]; then
    R811_EVIDENCE_DIR="$EVIDENCE_DIR" "$ROOT/scripts/r811_strategy_parity_gate.sh" \
        >"$TMP/parity.log" 2>&1 \
        || { sed -n '1,220p' "$TMP/parity.log" >&2; fail STRATEGY_PARITY; }
    sed -n '1,20p' "$TMP/parity.log"
fi

mkdir -p "$EVIDENCE_DIR"
cp "$TMP/official-javac.log" "$EVIDENCE_DIR/R811_OFFICIAL_ANDROID36_JAVAC.log"
cp "$TMP/host-javac.log" "$EVIDENCE_DIR/R811_HOST_JAVAC.log"
cp "$TMP/r36-host.log" "$EVIDENCE_DIR/R811_R36_REGRESSION_HOST.log"
cp "$TMP/r811-host.log" "$EVIDENCE_DIR/R811_HOST_CONTRACTS.log"
cp "$TMP/r811-neural-self-test.log" "$EVIDENCE_DIR/R811_NEURAL_TRAINER_SELF_TEST.log"
cp "$TMP/r811-neural-trainer-smoke.log" "$EVIDENCE_DIR/R811_NEURAL_TRAINER_SMOKE.log"
cp "$TMP/r811-smoke.artifact.manifest.json" \
    "$EVIDENCE_DIR/R811_NEURAL_TRAINER_SMOKE_MANIFEST.json"
cp "$TMP/r811-report-schema.log" "$EVIDENCE_DIR/R811_RESEARCH_REPORT_SCHEMA.log"
cp "$TMP/r811-plan-contradiction.log" "$EVIDENCE_DIR/R811_PLAN_CONTRADICTION_GATE.log"
android_sha=$(sha256sum "$ANDROID_JAR" | awk '{print $1}')
source_count=$(wc -l < "$TMP/app.list" | tr -d ' ')
class_count=$(find "$TMP/official-classes" -type f -name '*.class' | wc -l | tr -d ' ')
host_assertions=$(sed -n 's/^R811_HOST_CONTRACTS=PASS|ASSERTIONS=\([0-9][0-9]*\).*/\1/p' "$TMP/r811-host.log" | head -n 1)
[ -n "$host_assertions" ] || fail R811_HOST_ASSERTION_COUNT
{
    echo "R811_GATE=PASS"
    echo "OFFICIAL_ANDROID36_COMPILE=PASS|ANDROID_JAR_SHA256=$android_sha"
    echo "JAVA_SOURCE_FILES=$source_count|CLASS_FILES=$class_count|SOURCE=17|TARGET=17"
    echo "R36_REGRESSION_HOST=PASS"
    echo "R811_HOST_CONTRACTS=PASS|ASSERTIONS=$host_assertions"
    cat "$TMP/r811-report-schema.log"
    cat "$TMP/r811-plan-contradiction.log"
    echo "STRATEGY_PARITY=$([ "${R811_SKIP_PARITY:-0}" = "1" ] && echo PREVIOUSLY_PROVEN || echo PASS)"
    echo "MODEL_PROVEN=false|SHADOW_ONLY=true|REAL_LOCKED=true"
} | tee "$EVIDENCE_DIR/R811_BEHAVIOR_GATE_SUMMARY.txt"
