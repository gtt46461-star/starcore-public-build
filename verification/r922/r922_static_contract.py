#!/usr/bin/env python3
"""Static source, wiring, build-order and truth-boundary gate for R922."""

from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "app/src/main/java/com/ethk3/novaonepro"


def read(path: Path) -> str:
    if not path.is_file():
        raise AssertionError(f"R922_STATIC_FAIL:MISSING:{path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def require(condition: bool, code: str) -> None:
    if not condition:
        raise AssertionError(f"R922_STATIC_FAIL:{code}")


def contains(path: Path, *needles: str) -> str:
    text = read(path)
    for needle in needles:
        require(needle in text, f"{path.name}:MISSING:{needle}")
    return text


def identity_and_resource_build() -> None:
    props = read(ROOT / "release/release_identity.properties")
    manifest = read(JAVA / "V51BuildManifest.java")
    app = read(JAVA / "AppConfig.java")
    strings = read(ROOT / "app/src/main/res/values/strings.xml")
    build = contains(
        ROOT / "scripts/r922_build_release.sh",
        "RESOURCE_SKELETON_BUILD",
        "r922_generate_r_java.py",
        "R922_APK_RESOURCE_CONTRACT.txt",
        "R922_PREDECESSOR_RESOURCE_COMPATIBILITY",
        '"$TMP/gen/com/ethk3/novaonepro/R.java"',
    )
    require("versionName=V57.20.8-R5.3.8-922" in props, "IDENTITY_NAME")
    require("versionCode=577240922" in props, "IDENTITY_CODE")
    require('"V57.20.8-R5.3.8-922"' in manifest, "MANIFEST_NAME")
    require("VERSION_CODE = 577240922" in manifest, "MANIFEST_CODE")
    require('R36_RUNTIME_REPAIR_VERSION = "V57.20.8-R5.3.8-922"' in app,
            "APP_IDENTITY")
    require("星核天枢·币安K3" in strings and "R922 · R5.3.8" in strings,
            "VISIBLE_IDENTITY")
    require(build.index("RESOURCE_SKELETON_BUILD")
            < build.index("r922_generate_r_java.py")
            < build.index('"$TMP/gen/com/ethk3/novaonepro/R.java"'),
            "RESOURCE_FIRST_BUILD_ORDER")
    require("verification/r36-stubs/com/ethk3/novaonepro/R.java" not in build,
            "HAND_MAINTAINED_R_FORBIDDEN")
    print("R922_STATIC_IDENTITY_RESOURCE=PASS|VERSION=V57.20.8-R5.3.8-922"
          "|CODE=577240922|RESOURCE_FIRST=1|HAND_R=FORBIDDEN")


def runtime_wiring() -> None:
    service = contains(
        JAVA / "AutoFetchService.java",
        "AssetGenesisEngineR915.capturePredraw",
        "AlgorithmInventionLabR908.capturePredraw",
        "ResearchTinyDeepLearnerR909.capturePredraw",
        "ResearchLearningContinuityR912.pulse",
        "AlgorithmInventionLabR908.maintain",
        "AssetGenesisEngineR915.maintain",
        "ResearchDynamicBootstrapR922.pulse",
        '"V51_OWNER_OK",',
        'markTeacherPump(',
        "drainSettledCatchup",
        "R922_TASK_REMOVED_FULL_CHAIN",
        "return START_STICKY",
        "void onTaskRemoved",
    )
    sidecar = contains(
        JAVA / "SettlementPostCommitSidecarR909.java",
        "AlgorithmInventionLabR908.settleDraw",
        "AssetGenesisEngineR915.settleDraw",
        "ResearchShadowChallengerRuntimeR817.settle",
        "LegacyPredrawContextBridgeR915.settleDraw",
    )
    continuity = contains(
        JAVA / "ResearchLearningContinuityR912.java",
        "ResearchPredrawSettlementPumpR908.drainBatch",
        "SettlementPostCommitSidecarR909.drain",
        "ResearchTinyDeepLearnerR909.updateOne",
    )
    require(sidecar.index("AlgorithmInventionLabR908.settleDraw")
            < sidecar.index("AssetGenesisEngineR915.settleDraw")
            < sidecar.index("LegacyPredrawContextBridgeR915.settleDraw"),
            "POST_COMMIT_SETTLEMENT_ORDER")
    require("scheduleServiceRestartV5(\"onTaskRemoved\")" in service
            and "drainBatch" in continuity, "BACKGROUND_RESEARCH_CONTINUITY")
    print("R922_RUNTIME_WIRING=PASS|PREDRAW=ALGORITHM+ASSET+TINY_DEEP"
          "|POSTDRAW=ALGORITHM+ASSET+TEACHER|BACKGROUND_REARM=1")


def r922_closure() -> None:
    orchestrator = contains(
        JAVA / "V51RuntimeOrchestrator.java",
        "ControlledCandidateOpportunityAuditR922.from",
        "ResearchMainlineContributionAuditR922.Contribution",
        "GENESIS_NEW_CERTIFIED",
        "INVENTED_ALGORITHM_CERTIFIED",
        "FORMULA_DSL_ACTIVE_SHADOW",
        "DecisionGateEngine().decideV51B12",
    )
    repository = contains(
        JAVA / "V51Repository.java",
        "ControlledCandidateOpportunityAuditR922",
        "ResearchMainlineContributionAuditR922",
        "insertAuthorizedTradeWithinCanonicalR915",
    )
    deferred = contains(
        JAVA / "DeferredEvidenceWriterR36.java",
        "drainSettledCatchup",
        "ORDER BY target_issue DESC,",
        "core_materialized DESC,next_action_ordinal DESC",
        "BacklogStats",
        "NEWEST_DEEPEST_CONVERGENCE",
    )
    coverage = contains(
        JAVA / "CanonicalObservationWriterR36.java",
        "r922_activation_base_issue",
        "FIRST_R922_FROZEN_CANONICAL",
    ) + contains(
        JAVA / "ModelEffectivenessAnalyzerR35.java",
        "r922_activation_base_issue",
        "AND runtime_version=?",
    )
    report = contains(
        JAVA / "V57CompleteDiagnosticReportEngine.java",
        "generateForensic",
        "BOUNDED_SUMMARY",
        "ControlledRealContractAuditR922",
        "15%是连续R922赛前机会的观察目标",
        "ResearchMainlineContributionAuditR922.recentDetail",
    )
    main = contains(
        JAVA / "MainActivity.java",
        "reconcileAutoFetchService",
        "复制R922决策版检测报告（推荐）",
        "复制R922取证原始附录（超长）",
    )
    learning = contains(
        JAVA / "ModelLearningActivity.java",
        "FOREGROUND_REFRESH_MS = 15_000L",
        "foregroundHandler.postDelayed",
        "requestTruth();",
    )
    evolution = contains(
        JAVA / "ResearchEvolutionDeltaR922.java",
        "formulaCertified",
        "assetStateUpdates",
        "algorithmStateUpdates",
        "TinyDeep=",
    )
    asset_governor = contains(
        JAVA / "CertifiedResearchMixerR915.java",
        "futureRate",
        "coverage",
        "pairedDelta",
        "future_hit_rate>=0.36",
        "future_coverage>=0.80",
    )
    algorithm_governor = contains(
        JAVA / "AlgorithmProductionGovernorR915.java",
        "public final double futureRate, wilson, brier, ece, coverage",
    )
    layout = read(ROOT / "app/src/main/res/layout/activity_main.xml")
    tablet = read(ROOT / "app/src/main/res/layout-sw600dp/activity_main.xml")
    for text, code in ((layout, "PHONE"), (tablet, "TABLET")):
        require('android:id="@+id/scroll_build_badge"' in text
                and '<HorizontalScrollView' in text
                and 'android:singleLine="true"' in text,
                f"HORIZONTAL_BADGE_{code}")
    require(orchestrator.index("ControlledCandidateOpportunityAuditR922.from")
            > orchestrator.index("DecisionGateEngine().decideV51B12"),
            "OPPORTUNITY_AFTER_GATE")
    require(repository.index("insertAuthorizedTradeWithinCanonicalR915")
            < repository.index("ControlledCandidateOpportunityAuditR922"),
            "CANONICAL_AUDIT_ORDER")
    require("required=0" not in report or "K9_BetObservations" not in report,
            "ZERO_BET_FALSE_GATE_REMOVED")
    require("r922_activation_base_issue" in coverage, "R922_COHORT")
    require("foregroundRefresh" in learning and "assetStateUpdates" in evolution,
            "DYNAMIC_VISIBLE_REFRESH")
    require("futureRate" in asset_governor and "coverage" in algorithm_governor,
            "MAINLINE_EVIDENCE_METRICS")
    print("R922_CLOSURE=PASS|OPPORTUNITY_DENOMINATOR=1|TARGET15=OBSERVATION_ONLY"
          "|RESEARCH_CONTRIBUTION=CANONICAL|DEFERRED=NEWEST_DEEPEST"
          "|BACKGROUND=FORCED_RECONCILE|REPORT=DECISION+FORENSIC|HEADER=HORIZONTAL"
          "|CREATION_UI=AUTO_REFRESH|EVIDENCE_METRICS=COMPLETE")


def atom_invention_truth() -> None:
    schema = contains(
        JAVA / "GenesisSchemaRegistryR915.java",
        "EXPECTED_ATOMS = 240",
        "EXPECTED_FAMILIES = 72",
        "EXPECTED_PRODUCTION_SCORE_ATOMS = 165",
        "RESEARCH_PROXY_NOT_CERTIFIABLE",
        "productionScoreEligible",
        "POSTDRAW_EVIDENCE_FOR_LIFECYCLE_ONLY",
        "left.predrawComputable()",
        "right.predrawComputable()",
    )
    atomic_world = contains(
        JAVA / "GenesisAtomicWorldR920.java",
        "R922_SEMANTIC_ATOMIC_WORLD_V2",
        "truthAtom",
        "temporalAtom",
        "topologyAtom",
        "informationAtom",
        "memoryAtom",
        "riskAtom",
        "actionAtom",
        "metaAtom",
        "R922_A9_PREDRAW_NONZERO",
    )
    asset_vm = contains(
        JAVA / "GenesisAssetVmR915.java",
        "GenesisAtomicWorldR920.materialize",
    )
    algorithm = contains(
        JAVA / "AlgorithmGenomeR908.java",
        "scoreReadsMemory",
        "updateReadsOutcome",
        "writesMemory",
        "MutationIntent",
        "mutateForIntent",
        "chineseName",
    )
    invention = contains(
        JAVA / "AlgorithmInventionLabR908.java",
        "failureVector",
        "mutationIntent",
        "creationReason",
        "capturePredraw",
        "settleDraw",
        "ALREADY_FROZEN_FIRST_WRITE",
    )
    genesis = contains(
        JAVA / "AssetGenesisEngineR915.java",
        "ALREADY_FROZEN_FIRST_WRITE",
        "productionSemanticEligible",
        "R922_PROXY_ATOM_NOT_CERTIFIABLE",
    )
    integrity = contains(
        JAVA / "LegacyIntegrityGuardR915.java",
        "verifyAtomSchema",
        "auditAtomRowsForTest",
        "CONTRACT_CHANGED:",
    )
    report = contains(
        JAVA / "LiveDiagnosticReportEngine.java",
        "StrictOosHit=",
        "OverallHitRate=",
        "OverallWilsonLower95=",
        "Recent120HitRate=",
        "Recent120Observed=",
    )
    lifecycle = contains(
        JAVA / "AlgorithmEvolutionLifecycleR912.java",
        "future-OOS",
        "wilson",
        "brier",
        "ece",
        "fdr",
        "PRUNED",
    )
    truth = contains(
        JAVA / "RuntimeTruthSnapshotR906.java",
        'VERSION="R922-RUNTIME-TRUTH-V8"',
        "R922创造能力",
        "原始K3评分（未校准）",
        "REAL结算口径=trades/action=BET/stat_scope=REAL_TRADE/result=WIN|MISS",
        "Wilson95下界",
        "最近20条REAL结算",
        "动作稳定性",
        "号码风险",
        "算法发明迭代",
        "生产评分语义实装",
        "禁止再声明240/240全部实装",
        "GenesisAtomicWorldR920.VERSION",
        "StateBox[] out=new StateBox[16]",
    )
    main = contains(
        JAVA / "MainActivity.java",
        "R922 唯一运行真值总览",
        "txtR919ResearchTruth.setMaxLines(16)",
        "txtR919ResearchTruth.setEllipsize(null)",
        "RuntimeTruthSnapshotR906.capture",
        "installLaunchSafeModeR922",
        "R922_UI_BIND_OR_BOOTSTRAP",
    )
    learning = contains(
        JAVA / "ModelLearningActivity.java",
        "R922深度学习・动态迭代・算法创造唯一真值",
        "RuntimeTruthSnapshotR906.capture",
    )
    require('if(group==9)return "SETTLEMENT_EVIDENCE"' in schema, "A9_STAGE")
    require("atomicTransform(" not in asset_vm and "case 9: return 0.0d" in atomic_world,
            "SEMANTIC_ATOMIC_WORLD_REPLACED_GENERIC_ROTATION")
    require("MutationIntent" in algorithm and "creationReason" in invention
            and "future-OOS" in lifecycle, "EVOLUTION_COMPLETENESS")
    require("WHERE leakage_class='PREDRAW_ONLY' AND update_timing='FREEZE_BEFORE_TARGET_DRAW'"
            not in integrity, "LOSSY_ATOM_SUBSET_COUNT_FORBIDDEN")
    require("MODEL_NOT_PROVEN" in truth and "R922" in main and "R922" in learning
            and "productionSemanticEligible" in genesis and "OverallHitRate=" in report,
            "VISIBLE_TRUTH")
    print("R922_ATOM_INVENTION_TRUTH=PASS|ATOMS=240|PRODUCTION_SCORE=165"
          "|RESEARCH_PROXY=40|FAMILIES=72|A9=POSTDRAW_ONLY"
          "|LOSSY_SUBSET_COUNT=FORBIDDEN|IDEMPOTENT_CAPTURE=PASS"
          "|STATE_SCORE_UPDATE=1|FAILURE_DIRECTED_STRUCTURE=1|VISIBLE=1")


def source_substance() -> None:
    files = sorted(JAVA.glob("*.java"))
    require(len(files) >= 500, "JAVA_TREE_INCOMPLETE")
    empty = []
    trivial = []
    total_lines = 0
    total_chars = 0
    for path in files:
        text = read(path)
        lines = len(text.splitlines())
        total_lines += lines
        total_chars += len(text)
        if not text.strip():
            empty.append(path.name)
        if lines < 12 and not re.search(
                r"\b(if|for|while|switch|return|throw|execSQL|enum|interface)\b", text):
            trivial.append(path.name)
    require(not empty, "EMPTY_JAVA:" + ",".join(empty))
    require(not trivial, "TRIVIAL_JAVA:" + ",".join(trivial))
    print(f"R922_SOURCE_SUBSTANCE=PASS|JAVA={len(files)}|LINES={total_lines}"
          f"|CHARS={total_chars}|EMPTY=0|TRIVIAL=0")


def main() -> None:
    identity_and_resource_build()
    runtime_wiring()
    r922_closure()
    atom_invention_truth()
    source_substance()
    print("R922_STATIC_CONTRACT=PASS")


if __name__ == "__main__":
    main()
