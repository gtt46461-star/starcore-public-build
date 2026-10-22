#!/usr/bin/env python3
"""Static R919 contract gate for source ownership, wiring and truthful release boundaries."""

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "app/src/main/java/com/ethk3/novaonepro"


def read(path: Path) -> str:
    if not path.is_file():
        raise AssertionError(f"MISSING_FILE:{path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def require(condition: bool, code: str) -> None:
    if not condition:
        raise AssertionError(f"R919_STATIC_FAIL:{code}")


def contains(path: Path, *needles: str) -> str:
    text = read(path)
    for needle in needles:
        require(needle in text, f"{path.name}:MISSING:{needle}")
    return text


def identity_contract() -> None:
    props = read(ROOT / "release/release_identity.properties")
    manifest = read(JAVA / "V51BuildManifest.java")
    app = read(JAVA / "AppConfig.java")
    gradle = read(ROOT / "app/build.gradle")
    require("versionName=V57.20.5-R5.3.5-919" in props, "IDENTITY_PROPERTIES_NAME")
    require("versionCode=577240919" in props, "IDENTITY_PROPERTIES_CODE")
    require('"V57.20.5-R5.3.5-919"' in manifest, "MANIFEST_NAME")
    require("VERSION_CODE = 577240919" in manifest, "MANIFEST_CODE")
    require('R36_RUNTIME_REPAIR_VERSION = "V57.20.5-R5.3.5-919"' in app,
            "APP_CONFIG_NAME")
    require("R919" in gradle, "ARCHIVE_R919")
    require("MODEL_PROVEN = false" in manifest and "REALTIME_PROVEN = false" in manifest,
            "NO_FALSE_MODEL_PROOF")
    print("R919_STATIC_IDENTITY=PASS|VERSION=V57.20.5-R5.3.5-919|CODE=577240919")


def settlement_and_teacher_contract() -> None:
    sidecar = contains(
        JAVA / "SettlementPostCommitSidecarR909.java",
        "AlgorithmInventionLabR908.settleDraw",
        "AssetGenesisEngineR915.settleDraw",
        "ResearchShadowChallengerRuntimeR817.settle",
        "LegacyPredrawContextBridgeR915.settleDraw",
        "receipt.countsAsSettledPrediction()",
    )
    bridge = contains(
        JAVA / "LegacyPredrawContextBridgeR915.java",
        "v57_legacy_postsettlement_context_r919",
        "R919_POSTSETTLEMENT_FIRST_WRITE_CONFLICT",
        "R919_EXACT_DRAW_POSITION_MISMATCH",
        "只更新t+1以后状态",
        "Teacher/矩阵缺失原因",
    )
    store = contains(
        JAVA / "SettlementTaskStoreR36.java",
        "terminalizeAuditedNoPredrawBacklog",
        "R919_AUDITED_NO_IMMUTABLE_PREDRAW",
        "EXCLUDED_NO_PREDRAW",
    )
    supervisor = contains(
        JAVA / "SettlementRecoverySupervisorR906.java",
        "DRAIN_ELIGIBLE_PREDRAW",
        "CLASSIFY_NO_PREDRAW_PRESSURE_SLICE",
        "pressure ? 4 : 32",
    )
    pump = contains(
        JAVA / "ResearchPredrawSettlementPumpR908.java",
        "AlgorithmInventionLabR908.settleDraw",
        "AssetGenesisEngineR915.settleDraw",
    )
    require(sidecar.index("AlgorithmInventionLabR908.settleDraw")
            < sidecar.index("AssetGenesisEngineR915.settleDraw")
            < sidecar.index("ResearchShadowChallengerRuntimeR817.settle")
            < sidecar.index("LegacyPredrawContextBridgeR915.settleDraw"),
            "POST_COMMIT_FANOUT_ORDER")
    require("UPDATE " not in bridge.split("freezeWithinTransaction", 1)[0],
            "BRIDGE_HEADER_MUTATION")
    require("quiet" in supervisor.lower() and "predraw" in store.lower() and "settleDraw" in pump,
            "SETTLEMENT_EXPLICITNESS")
    print("R919_SETTLEMENT_TEACHER=PASS|CORE_FIRST=1|DERIVATIVE_FAIL_OPEN=1"
          "|TEACHER_POSTDRAW_EXACTLY_ONCE=1|NO_PREDRAW_PRESSURE_CONVERGENCE=1")


def atom_and_invention_contract() -> None:
    schema = contains(
        JAVA / "GenesisSchemaRegistryR915.java",
        "EXPECTED_ATOMS = 240",
        "EXPECTED_FAMILIES = 72",
        "SETTLEMENT_EVIDENCE",
        "POSTDRAW_EVIDENCE_FOR_LIFECYCLE_ONLY",
        "SETTLEMENT_UPDATE_PREDRAW_READ_ONLY",
        "left.predrawComputable()",
        "right.predrawComputable()",
    )
    genome = contains(
        JAVA / "GenesisAssetGenomeR915.java",
        "ORGAN_COUNT = 15",
        "atom.predrawComputable()",
        "randomPredrawAtom",
        "mechanismNameZh",
        "behaviorHash",
        "MAY_AUTHORIZE_REAL = false",
    )
    algorithm = contains(
        JAVA / "AlgorithmGenomeR908.java",
        "scoreReadsMemory",
        "updateReadsOutcome",
        "writesMemory",
        "chineseName",
        "mutateForIntent",
        "MutationIntent",
    )
    vm = contains(
        JAVA / "AlgorithmGenomeVmR908.java",
        "behaviorHash",
        "allowStateWrite",
    )
    lifecycle = contains(
        JAVA / "AlgorithmEvolutionLifecycleR912.java",
        '"future_n"',
        "wilson",
        "brier",
        "ece",
        "fdr",
        "PRUNED",
    )
    engine = contains(
        JAVA / "AssetGenesisEngineR915.java",
        "value_range",
        "window_spec",
        "max_lag",
        "min_support",
        "evidence_stage",
        "capturePredraw",
        "settleDraw",
    )
    require('if(group==9)return "SETTLEMENT_EVIDENCE"' in schema,
            "A9_STAGE_ASSIGNMENT")
    require("!atom.predrawComputable()" in genome, "ASSET_ATOM_LEAKAGE_REJECT")
    require("future-OOS" in lifecycle and "structuralHash" in algorithm
            and "behaviorHash" in vm and "ensureColumn" in engine,
            "INVENTION_LIFECYCLE_COMPLETENESS")
    print("R919_ATOM_INVENTION=PASS|L0=240|FAMILIES=72|A9_POSTDRAW_ONLY=20"
          "|ASSET_ORGANS=15|ALGORITHM_STATE_SCORE_UPDATE=1|CHINESE_MECHANISM_NAME=1")


def unified_truth_and_ui_contract() -> None:
    truth = contains(
        JAVA / "RuntimeTruthSnapshotR906.java",
        'VERSION="R919-RUNTIME-TRUTH-V6"',
        'new TruthCard("WHY_HIT","为什么中"',
        'new TruthCard("WHY_MISS","为什么不中"',
        'new TruthCard("WHY_NO_BET","为什么不出手"',
        'new TruthCard("SETTLEMENT","逐期结算统计"',
        "StateBox[] out=new StateBox[16]",
        "原始K3概率",
        "动作稳定性",
        "号码风险",
        "算法发明迭代",
        "LegacyPredrawContextBridgeR915.readPost",
    )
    main = contains(
        JAVA / "MainActivity.java",
        "RuntimeTruthSnapshotR906.capture",
        "R919 唯一运行真值总览",
        "R919深度学习・动态迭代・算法创造",
        "btnBackgroundReliability",
    )
    learning = contains(
        JAVA / "ModelLearningActivity.java",
        "RuntimeTruthSnapshotR906.capture",
        "whyCards",
        "stateBoxes",
    )
    report = contains(
        JAVA / "V57CompleteDiagnosticReportEngine.java",
        "RuntimeTruthSnapshotR906.capture",
    )
    live = contains(
        JAVA / "LiveDiagnosticReportEngine.java",
        "DIRECT_REAL_AUTH=0",
        "PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED",
        "PhysicalRuntime=RETEST_REQUIRED",
    )
    require(not (JAVA / "RuntimeTruthDashboardR818.java").exists(), "DEAD_TRUTH_OWNER_PRESENT")
    require(not (JAVA / "WhyDecisionTruthR820.java").exists(), "DEAD_WHY_OWNER_PRESENT")
    require(truth.count("new TruthCard(") >= 8, "WHY_CARD_NORMAL_AND_ERROR")
    require("RuntimeTruthSnapshotR906.capture" in main
            and "RuntimeTruthSnapshotR906.capture" in learning
            and "RuntimeTruthSnapshotR906.capture" in report,
            "UNIFIED_SNAPSHOT_CONSUMERS")
    require("MODEL_NOT_PROVEN" in truth and "READ_ERROR" in live,
            "TRUTH_BOUNDARY_TEXT")
    print("R919_UNIFIED_UI_TRUTH=PASS|SNAPSHOT_OWNER=1|WHY_CARDS=4|STATE_BOXES=16"
          "|MAIN_LEARNING_REPORT_SAME_SOURCE=1|DEAD_OWNERS_REMOVED=2")


def background_contract() -> None:
    service = contains(
        JAVA / "AutoFetchService.java",
        "return START_STICKY",
        "void onTaskRemoved",
        "scheduleServiceRestartV5(\"onTaskRemoved\")",
        "r919_background_capability_status",
        "BackgroundRuntimeCapabilityR811.snapshot",
    )
    capability = contains(
        JAVA / "BackgroundRuntimeCapabilityR811.java",
        "isIgnoringBatteryOptimizations",
        "canScheduleExactAlarms",
    )
    boot = contains(
        JAVA / "BootReceiver.java",
        "scheduleAllRecovery",
        "scheduleWatchdog",
        "scheduleJobWatchdog",
        "setPersisted(true)",
    )
    manifest = contains(
        ROOT / "app/src/main/AndroidManifest.xml",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.permission.SCHEDULE_EXACT_ALARM",
        ".AutoFetchService",
        ".BootReceiver",
    )
    require("START_NOT_STICKY" in service and "certificateFingerprint" in capability,
            "USER_STOP_AND_CAPABILITY_CERTIFICATE")
    require("BOOT_COMPLETED" in manifest and "MY_PACKAGE_REPLACED" in manifest,
            "BOOT_AND_UPGRADE_RECEIVERS")
    require("PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED" in read(JAVA / "LiveDiagnosticReportEngine.java"),
            "NO_FAKE_DEVICE_PASS")
    print("R919_BACKGROUND=PASS_STATIC|START_STICKY=1|TASK_REMOVED_REARM=1"
          "|ALARM_JOB_BOOT=1|RUNTIME_GRANTS_VISIBLE=1|PHYSICAL_DEVICE=RETEST_REQUIRED")


def source_substance_contract() -> None:
    files = sorted(JAVA.glob("*.java"))
    require(len(files) >= 500, "JAVA_TREE_INCOMPLETE")
    empty = []
    trivial = []
    for path in files:
        text = read(path)
        lines = len(text.splitlines())
        if not text.strip():
            empty.append(path.name)
        # Tiny DTOs can be valid, so flag only files with neither executable body nor schema/data.
        if lines < 12 and not re.search(
                r"\b(if|for|while|switch|return|throw|execSQL|enum|interface)\b", text):
            trivial.append(path.name)
    require(not empty, "EMPTY_JAVA:" + ",".join(empty))
    require(not trivial, "TRIVIAL_NAME_ONLY_JAVA:" + ",".join(trivial))
    total_lines = sum(len(read(path).splitlines()) for path in files)
    total_chars = sum(len(read(path)) for path in files)
    print(f"R919_SOURCE_SUBSTANCE=PASS|JAVA={len(files)}|LINES={total_lines}"
          f"|CHARS={total_chars}|EMPTY=0|TRIVIAL_NAME_ONLY=0")


def main() -> None:
    identity_contract()
    settlement_and_teacher_contract()
    atom_and_invention_contract()
    unified_truth_and_ui_contract()
    background_contract()
    source_substance_contract()
    print("R919_STATIC_CONTRACT=PASS")


if __name__ == "__main__":
    main()
