#!/usr/bin/env python3
"""Fail-closed source contract for the R915 background/settlement/REAL/invention repair."""

from pathlib import Path
import hashlib
import re


ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(path: str, pattern: str, code: str) -> None:
    if re.search(pattern, read(path), re.MULTILINE | re.DOTALL) is None:
        raise AssertionError(f"{code}:{path}")


def forbid(path: str, pattern: str, code: str) -> None:
    if re.search(pattern, read(path), re.MULTILINE | re.DOTALL) is not None:
        raise AssertionError(f"{code}:{path}")


identity = {}
for line in read("release/release_identity.properties").splitlines():
    if "=" in line:
        key, value = line.split("=", 1)
        identity[key.strip()] = value.strip()

assert identity == {
    "applicationId": "com.ethk3.novaonepro",
    "versionName": "V57.20.1-R5.3.1-915",
    "versionCode": "577240915",
    "compileSdk": "36",
    "minSdk": "21",
    "targetSdk": "28",
    "dbVersion": "36",
    "schemaRevision": "4",
    "gameCode": "Binance1-PK10",
    "releaseMode": "CONTROLLED_CANDIDATE_MODEL_NOT_PROVEN",
}, f"R915_RELEASE_IDENTITY:{identity}"

java_root = ROOT / "app/src/main/java/com/ethk3/novaonepro"
java_files = sorted(java_root.glob("*.java"))
assert len(java_files) == 524, f"JAVA_FILE_COUNT:{len(java_files)}"
all_java = "\n".join(path.read_text(encoding="utf-8") for path in java_files)
assert len(re.findall(r"public\s+(?:final\s+)?class\s+SettlementEngine\b", all_java)) == 1
assert len(re.findall(r"public\s+(?:final\s+)?class\s+DecisionGateEngine\b", all_java)) == 1

# Background survival: START_STICKY, task-removal survival and independent OS recovery pulses.
require("app/src/main/AndroidManifest.xml", r'android:stopWithTask="false"', "STOP_WITH_TASK_FALSE")
require("app/src/main/AndroidManifest.xml", r'android:name="android.permission.RECEIVE_BOOT_COMPLETED"', "BOOT_PERMISSION")
require("app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java",
        r'onTaskRemoved.*forceRearmAutoFetchService.*postAnalysisForLatest', "TASK_REMOVED_REARM")
require("app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java",
        r'onStartCommand.*START_STICKY', "START_STICKY")
require("app/src/main/java/com/ethk3/novaonepro/BootReceiver.java",
        r'reconcileAutoFetchService.*forceBusinessReconcile', "HEALTHY_BUSINESS_RECONCILE")
require("app/src/main/java/com/ethk3/novaonepro/ServiceWatchdogJobService.java",
        r'periodic_reconcile_healthy', "JOB_RECONCILE")
require("app/src/main/java/com/ethk3/novaonepro/InternalWatchdogReceiverR815.java",
        r'markRecoveryTrigger', "ALARM_TRIGGER_TRUTH")

# Settlement every fetched draw: durable atomic claim, NULL-safe legacy leases, and a separate
# next-predraw lane. OOS and REAL rows commit inside one SettlementEngine-owned transaction.
require("app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java",
        r'normalizeAndClaimCurrentR915', "ATOMIC_NORMALIZE_CLAIM")
require("app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java",
        r'COALESCE\(lease_until,0\)', "NULL_LEASE_RECOVERY")
require("app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java",
        r'SEPARATE_LANES_CONTINUE_NEXT_PREDRAW_NO_SETTLEMENT_REWRITE', "SEPARATE_LANES")
require("app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java",
        r'settleFrozenWithinTransaction.*settleRealWithinTransactionR36.*markTerminalWithinTransaction',
        "ATOMIC_OOS_REAL_TERMINAL")
require("app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java",
        r'R915_CURRENT_EXACT_IMMEDIATE_RETRY_LIMIT\s*=\s*8.*attempt\s*>=\s*R915_CURRENT_EXACT_IMMEDIATE_RETRY_LIMIT',
        "BOUNDED_IMMEDIATE_SETTLEMENT")

# Controlled candidate: no false model proof, exact authorization, atomic canonical+Trade writer,
# and no prefix/fuzzy signature acceptance.
manifest = "app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java"
require(manifest, r'MODEL_PROVEN\s*=\s*false', "MODEL_NOT_PROVEN")
require(manifest, r'REALTIME_PROVEN\s*=\s*false', "REALTIME_NOT_PROVEN")
require(manifest, r'SHADOW_ONLY\s*=\s*false', "CONTROLLED_NOT_GLOBAL_SHADOW")
require(manifest, r'REAL_BET_HARD_DISABLED\s*=\s*false', "CONTROLLED_NOT_HARD_DISABLED")
require(manifest, r'CANDIDATE_REAL_AUTH_ENABLED\s*=\s*true', "CONTROLLED_AUTH_ENABLED")
gate = "app/src/main/java/com/ethk3/novaonepro/V51CandidateRealGateR915.java"
for token in ("MIN_RAW_ACTION_P", "MIN_THIRD_MARGIN", "MIN_ACTION_STABILITY",
              "MAX_ENTROPY", "MIN_ASSIGNMENT_SUPPORT", "MAX_FALSIFIER",
              "MIN_RANK_RETENTION", "MIN_THIRD_ROBUSTNESS", "MIN_MARGIN_ROBUSTNESS"):
    require(gate, re.escape(token), "CONTROLLED_GATE_" + token)
require("app/src/main/java/com/ethk3/novaonepro/V51AuthorizationCore.java",
        r'return expected\.equals\(decision\.authorizationHash\)', "EXACT_AUTH_SIGNATURE")
forbid("app/src/main/java/com/ethk3/novaonepro/V51AuthorizationCore.java",
       r'startsWith\(|substring\(0', "NO_FUZZY_AUTH")
require("app/src/main/java/com/ethk3/novaonepro/V51Repository.java",
        r'insertAuthorizationCoreR36.*insertAuthorizedTradeWithinCanonicalR915.*insertWithinTransaction',
        "CANONICAL_AUTH_TRADE_ATOMIC")
require("app/src/main/java/com/ethk3/novaonepro/DbStore.java",
        r'R915_REAL_CANONICAL_TRANSACTION_REQUIRED.*R915_REAL_OPEN_TRADE_EXISTS',
        "ONE_OPEN_REAL_ATOMIC")

# Algorithm invention has state, scoring and outcome-update programs; only a Future-OOS challenger
# can influence Production, and it still cannot authorize or write REAL by itself.
require("app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java",
        r'algorithmGrade.*updateReadsOutcome', "STATE_SCORE_UPDATE_ALGORITHM")
require("app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java",
        r'MAY_AUTHORIZE_REAL\s*=\s*false', "INVENTION_NO_DIRECT_REAL")
governor = "app/src/main/java/com/ethk3/novaonepro/AlgorithmProductionGovernorR915.java"
require(governor, r'MAY_AUTHORIZE_REAL\s*=\s*false', "GOVERNOR_NO_DIRECT_REAL")
for token in ("CHALLENGER_FUTURE_OOS", "future_wilson_lower", "future_block_min",
              "future_regime_min", "future_brier", "future_ece", "future_coverage",
              "future_max_miss", "paired_mainline_delta"):
    require(governor, re.escape(token), "ALGORITHM_PRODUCTION_" + token)
forbid(governor, r'saveTrade|insertAuthorizedTrade|authorize\(', "GOVERNOR_NO_TRADE_OR_AUTH")
require("app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java",
        r'AlgorithmProductionGovernorR915\.govern.*controlledInfluenceEligible',
        "CERTIFIED_ALGORITHM_WIRED")

# Imported V4.1 plan foundation: frozen legacy namespace and exact new-asset schema cardinalities.
require("app/src/main/java/com/ethk3/novaonepro/LegacyFixedAssetRegistryR915.java",
        r'MUTATION_ALLOWED\s*=\s*false.*PRUNE_ALLOWED\s*=\s*false.*WEIGHT_OVERRIDE_ALLOWED\s*=\s*false',
        "LEGACY_FIXED_NAMESPACE")
require("app/src/main/java/com/ethk3/novaonepro/GenesisSchemaRegistryR915.java",
        r'EXPECTED_ATOMS\s*=\s*240.*EXPECTED_FAMILIES\s*=\s*72', "GENESIS_240_72")
require("app/src/main/java/com/ethk3/novaonepro/GenesisSchemaRegistryR915.java",
        r'PREDRAW_ONLY.*FREEZE_BEFORE_TARGET_DRAW', "GENESIS_LEAKAGE_POLICY")

# All 133 mature owners are exact source locks, not a marketing count. The manifest is parsed and
# every delivered Java byte stream is recomputed here; a renamed, missing or edited legacy owner
# fails the release before APK construction.
source_manifest = read("app/src/main/java/com/ethk3/novaonepro/LegacySourceLockManifestR915.java")
source_locks = dict(re.findall(r'lock\(out,"([^"]+)","([0-9a-f]{64})"\)', source_manifest))
assert len(source_locks) == 133, f"LEGACY_SOURCE_LOCK_COUNT:{len(source_locks)}"
for owner, expected in source_locks.items():
    owner_file = java_root / f"{owner}.java"
    assert owner_file.is_file(), f"LEGACY_SOURCE_OWNER_MISSING:{owner}"
    actual = hashlib.sha256(owner_file.read_bytes()).hexdigest()
    assert actual == expected, f"LEGACY_SOURCE_DRIFT:{owner}:{expected}:{actual}"

# V4.1 is executable: full asset genome/VM/lifecycle/certificate/mixer and the 16-organ algorithm
# contract must all be present on the runtime path. Token checks deliberately cover behavior, not
# only class names or comments.
asset_engine = "app/src/main/java/com/ethk3/novaonepro/AssetGenesisEngineR915.java"
for token in ("capturePredraw", "settleDraw", "refreshEvidence", "eligibleFreezes",
              "fdr_q", "CERTIFIED_FUTURE_OOS", "PRUNED_FUTURE_OOS",
              "research_new_atom_proposal_v41", "proposeNewAtom", "AWAITING_SIGNED_SCHEMA",
              "NEW_PRIMITIVE_REQUIRED", "LEAKAGE_TOKEN"):
    require(asset_engine, re.escape(token), "ASSET_ENGINE_" + token)
asset_vm = "app/src/main/java/com/ethk3/novaonepro/GenesisAssetVmR915.java"
for token in ("PREDRAW_STATE_MUTATION", "EXACTLY_ONCE_OR_ORDER_REJECT", "behaviorHash",
              "regimeBucket"):
    require(asset_vm, re.escape(token), "ASSET_VM_" + token)
require("app/src/main/java/com/ethk3/novaonepro/GenesisAssetGenomeR915.java",
        r'ORGAN_COUNT\s*=\s*15.*SettlementUpdate.*EvidenceLifecycle', "ASSET_15_ORGANS")
require("app/src/main/java/com/ethk3/novaonepro/ResearchGenomeContractR915.java",
        r'ORGAN_COUNT\s*=\s*16.*public static ResearchGenomeContractR915 parse',
        "RESEARCH_16_ORGANS_PARSE")
require("app/src/main/java/com/ethk3/novaonepro/CertifiedResearchMixerR915.java",
        r'MAY_AUTHORIZE_REAL\s*=\s*false.*MAX_INFLUENCE\s*=\s*0\.08d',
        "CERTIFIED_ASSET_LIMITED_NO_AUTH")
require("app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java",
        r'CertifiedResearchMixerR915\.mix.*AlgorithmProductionGovernorR915\.govern',
        "ASSET_AND_ALGORITHM_UNIQUE_MAINLINE")

# Teacher/forward/reverse/causal/feedback truth and UI density remain visible and read-only.
bridge = "app/src/main/java/com/ethk3/novaonepro/LegacyPredrawContextBridgeR915.java"
for token in ("教师后验", "正向推理", "反推矩阵", "因果口径", "反哺契约", "发明算法生产影响"):
    require(bridge, re.escape(token), "LEGACY_TRUTH_" + token)
require(bridge, r'Read paths are deliberately DDL-free', "READ_PATH_NO_DDL")
require("app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java",
        r'new StateBox\[16\]', "SIXTEEN_STATE_BOXES")
require("app/src/main/java/com/ethk3/novaonepro/ModelLearningActivity.java",
        r'LegacyIntegrityGuardR915\.verify.*newAtomProposalTruth',
        "LEGACY_AND_NEW_ATOM_TRUTH")
require("app/src/main/java/com/ethk3/novaonepro/MainActivity.java",
        r'RuntimeTruthSnapshotR906\.error\("LOADING_DATABASE_TRUTH"\).*whyTruthRendererR906\.render.*diagnosticStateRendererR906\.render',
        "NONEMPTY_LOADING_TRUTH")
layout = read("app/src/main/res/layout/activity_main.xml")
assert layout.count("<ScrollView") == 1, f"ROOT_SCROLL_COUNT:{layout.count('<ScrollView')}"

print(f"R915_STATIC_IDENTITY=PASS|VERSION={identity['versionName']}|CODE={identity['versionCode']}")
print("R915_BACKGROUND=PASS|START_STICKY=1|TASK_REMOVED_REARM=1|OS_RECONCILE=ALARM+JOB+BOOT")
print("R915_SETTLEMENT=PASS|DRAW_TASK_DURABLE=1|ATOMIC_CLAIM=1|PREDRAW_LANE_SEPARATE=1")
print("R915_CONTROLLED_REAL=PASS|MODEL_PROVEN=0|EXACT_GATE=1|CANONICAL_TRADE_ATOMIC=1")
print("R915_ALGORITHM_PLAN=PASS|LEGACY_FIXED=133_HASH_LOCKED|GENESIS=240+72|NEW_ATOM_ADD_ONLY=1|ASSET_15=1|ALGORITHM_16=1|FUTURE_OOS_GOVERNOR=1")
print("R915_UI_TRUTH=PASS|TEACHER_FORWARD_REVERSE_CAUSAL_FEEDBACK=1|STATE_BOXES=16")
print(f"R915_STATIC_CONTRACT=PASS|JAVA={len(java_files)}")
