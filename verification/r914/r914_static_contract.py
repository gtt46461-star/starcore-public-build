#!/usr/bin/env python3
"""Fail-closed source contract for the R914 single mainline and visible runtime truth."""

from pathlib import Path
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
    "versionName": "V57.20.0-R5.3.0-914",
    "versionCode": "577240914",
    "compileSdk": "36",
    "minSdk": "21",
    "targetSdk": "28",
    "dbVersion": "36",
    "schemaRevision": "4",
    "gameCode": "Binance1-PK10",
    "releaseMode": "SHADOW_ONLY",
}, f"R914_RELEASE_IDENTITY:{identity}"

require("app/src/main/java/com/ethk3/novaonepro/AppConfig.java",
        r'PACKAGE_NAME\s*=\s*"com\.ethk3\.novaonepro"', "PACKAGE")
require("app/src/main/java/com/ethk3/novaonepro/AppConfig.java",
        r'DB_NAME\s*=\s*"binance_k3_nova_one_pro_v57\.db"', "DB_NAME")
require("app/src/main/java/com/ethk3/novaonepro/AppConfig.java",
        r'DB_VERSION\s*=\s*36', "DB_VERSION")
require("app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java",
        r'GAME_CODE\s*=\s*"Binance1-PK10"', "GAME_CODE")
require("app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java",
        r'MODEL_PROVEN\s*=\s*false', "MODEL_NOT_PROVEN")
require("app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java",
        r'SHADOW_ONLY\s*=\s*true', "SHADOW_ONLY")
require("app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java",
        r'REAL_BET_HARD_DISABLED\s*=\s*true', "REAL_DISABLED")

java_root = ROOT / "app/src/main/java/com/ethk3/novaonepro"
java_files = sorted(java_root.glob("*.java"))
assert len(java_files) == 512, f"JAVA_FILE_COUNT:{len(java_files)}"
all_java = "\n".join(path.read_text(encoding="utf-8") for path in java_files)
assert len(re.findall(r"public\s+(?:final\s+)?class\s+SettlementEngine\b", all_java)) == 1
assert len(re.findall(r"public\s+(?:final\s+)?class\s+DecisionGateEngine\b", all_java)) == 1

# Atomic draw/task handoff and one execution/recovery fence.
require("app/src/main/java/com/ethk3/novaonepro/DbStore.java",
        r'R914_DRAW_TASK_ATOMIC|enqueueConfirmedWithinWrite', "ATOMIC_DRAW_TASK")
require("app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java",
        r'handoffCurrentExactR914', "CURRENT_HANDOFF")
require("app/src/main/java/com/ethk3/novaonepro/SettlementExecutionGateR819.java",
        r'tryEnterRecovery.*exitRecovery', "RECOVERY_FENCE")
require("app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java",
        r'queueCurrentExactR914.*requester\s*!=\s*null\)\s*requester\.request', "SETTLE_PREDRAW_INDEPENDENT")
require("app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java",
        r'queueCurrentExactR914.*postAnalysis', "LIVE_PIPELINE_NONBLOCKING")

# Algorithm-family invention is stateful, outcome-updated, future-only and behavior-deduplicated.
genome = "app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java"
for token in ("LOAD_S", "LOAD_G", "LOAD_PEER", "WRITE_S", "WRITE_G", "algorithmGrade",
              "mechanismDescriptionZh", "programStructureCanonical"):
    require(genome, re.escape(token), "GENOME_" + token)
require("app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java",
        r'R914_BEHAVIOR_V3', "BEHAVIOR_HASH_V3")
forbid("app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java",
       r'R914_BEHAVIOR_V3\|"\s*\+\s*genome\.programStructureCanonical', "BEHAVIOR_HASH_STRUCTURE_LEAK")
require("app/src/main/java/com/ethk3/novaonepro/AlgorithmEvolutionLifecycleR912.java",
        r'future_brier.*future_ece.*future_coverage.*paired_mainline_delta', "MULTI_METRIC_LIFECYCLE")
require("app/src/main/java/com/ethk3/novaonepro/AlgorithmEvidencePolicyR912.java",
        r'MULTI_METRIC_FUTURE_OOS_PASS', "FUTURE_PROMOTION")
require("app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java",
        r'INVENTION_SLICE_MS\s*=\s*2600L', "COOPERATIVE_SLICE")

# UI truth cannot collapse draw-task closure into the smaller prediction denominator.
require("app/src/main/java/com/ethk3/novaonepro/SettlementCoverageTruthR906.java",
        r'drawTaskTotal.*predictedEligible', "TWO_DENOMINATORS")
require("app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java",
        r'new StateBox\[16\]', "SIXTEEN_STATE_BOXES")
require("app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java",
        r'赛前Teacher/矩阵=.*开奖后反哺=', "RICH_WHY_TRUTH")
require("app/src/main/java/com/ethk3/novaonepro/MainActivity.java",
        r'pendingLightRefresh.*isUserScrollingNow', "SCROLL_LIGHT_DEFER")
forbid("app/src/main/java/com/ethk3/novaonepro/MainActivity.java",
       r'db\.refreshRealBetDiagnosisState\(\)', "UI_WRITES_DIAGNOSIS")
require("app/src/main/java/com/ethk3/novaonepro/DiagnosticStateRendererR906.java",
        r'BOX_COUNT\s*=\s*16', "SIXTEEN_RENDER_ROWS")
require("app/src/main/java/com/ethk3/novaonepro/FrameMetricsR906.java",
        r'scrollSlowFrames.*truthBindMaxMs', "SCROLL_FRAME_METRICS")

# One vertical root owner; history containers remain ordinary layouts.
layout = read("app/src/main/res/layout/activity_main.xml")
assert layout.count("<ScrollView") == 1, f"ROOT_SCROLL_COUNT:{layout.count('<ScrollView')}"
assert 'android:id="@+id/diagnostic_state_host"' in layout
assert 'android:id="@+id/why_truth_host"' in layout

print(f"R914_STATIC_IDENTITY=PASS|VERSION={identity['versionName']}|CODE={identity['versionCode']}|DB=36")
print("R914_SINGLE_MAINLINE=PASS|DECISION_GATE=1|SETTLEMENT_ENGINE=1")
print("R914_ALGORITHM_FAMILY=PASS|STATE=LOCAL_GLOBAL_PEER|FUTURE_MULTI_METRIC=1")
print("R914_UI_TRUTH=PASS|WHY=4|STATE_BOXES=16|TWO_DENOMINATORS=1|SCROLL_DEFER=1")
print(f"R914_STATIC_CONTRACT=PASS|JAVA={len(java_files)}")
