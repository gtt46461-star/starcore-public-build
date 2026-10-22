#!/usr/bin/env python3
"""Audit V50 runtime ownership, isolation, redlines, and fail-closed release state."""
from __future__ import annotations

import re

from v50_harness_common import ROOT, emit, read_java, require


REQUIRED_V50_FILES = (
    "V50BuildManifest.java",
    "V50Models.java",
    "V50FieldOwnerRegistry.java",
    "V50PredictionContract.java",
    "V50ChecksumEngine.java",
    "V50Repository.java",
    "V50NumberFeatureEngine.java",
    "V50StateTrajectoryEngine.java",
    "V50MixtureOfExpertsEngine.java",
    "V50EffectiveConsensusEngine.java",
    "V50ExpertRegretEngine.java",
    "V50TeacherEvidenceEngine.java",
    "V50ActionMetaFeatureEngine.java",
    "V50ActionRankerEngine.java",
    "V50SelectedActionCalibrationEngine.java",
    "V50SelectiveRiskController.java",
    "V50DomainCertificateEngine.java",
    "V50TrueAblationEngine.java",
    "V50RuntimeOrchestrator.java",
    "V50AtomicDecisionLedgerEngine.java",
    "V50FullInformationLearningEngine.java",
)


def direct_bool(source: str, name: str, expected: bool) -> None:
    value = "true" if expected else "false"
    require(
        re.search(
            r"public\s+static\s+final\s+boolean\s+%s\s*=\s*%s\s*;"
            % (re.escape(name), value),
            source,
        ) is not None,
        "BOOLEAN_CONTRACT_MISMATCH",
        name,
    )


def java_code_only(source: str) -> str:
    token = re.compile(
        r'"(?:\\.|[^"\\])*"|'
        r"'(?:\\.|[^'\\])*'|"
        r"//[^\n]*|/\*.*?\*/",
        re.DOTALL,
    )
    return token.sub(lambda match: "\n" * match.group(0).count("\n"), source)


def main() -> int:
    for name in REQUIRED_V50_FILES:
        require((ROOT / "app" / "src" / "main" / "java"
                 / "com" / "ethk3" / "novaonepro" / name).is_file(),
                "V50_FILE_MISSING", name)

    config = read_java("AppConfig.java")
    require('LOTTERY_CODE = "Ethereum1-PK10"' in config, "LOTTERY_REDLINE")
    require("FIXED_K = 3" in config, "FIXED_K3_REDLINE")
    require("REAL_TRADE_MAX_PER_ISSUE = 1" in config,
            "ONE_TRADE_PER_ISSUE_REDLINE")
    for flag in (
        "ALLOW_K4", "ALLOW_DYNAMIC_K", "ALLOW_MULTI_POSITION_BET",
        "ALLOW_MARTINGALE", "ALLOW_DOUBLE_BET", "ALLOW_CHASE_BET",
        "ALLOW_MULTI_ISSUE_CHASE", "ALLOW_POSITION_SPLIT_BET",
        "ALLOW_AUTO_INCREASE_STAKE",
    ):
        direct_bool(config, flag, False)
    direct_bool(config, "ENABLE_V50_PRIME", True)
    direct_bool(config, "ENABLE_V49_FULL_MOE", False)
    direct_bool(config, "LEGACY_READ_ONLY_OBSERVER", True)
    for flag in (
        "ENABLE_V20_DYNAMIC_CAUSE_CHAIN",
        "ENABLE_V20_FULL_CANDIDATE_SCAN",
        "ENABLE_V20_CAUSAL_FACTOR_MATRIX",
        "ENABLE_V20_TEACHER_NUMBER_MATRIX",
        "ENABLE_V20_DYNAMIC_CAUSAL_WINDOW_SYSTEM",
        "ENABLE_ADAPTIVE_JUDGE_OUTPUT_TO_DECISION",
        "TEACHER_MATRIX_FEED_REALTIME",
        "TEACHER_MATRIX_FEED_PROBABILITY",
        "TEACHER_MATRIX_FEED_GATE",
        "TEACHER_MATRIX_FEED_DECISION",
        "TEACHER_ASSIST_APPLY_TO_REALTIME",
        "TEACHER_APPLY_TO_REALTIME",
        "ENABLE_QUALIFIED_FREQUENCY_FLOOR",
        "ENABLE_FREQUENCY_DEBT",
        "ENABLE_REAL_BET_PROBE_BUDGET",
    ):
        direct_bool(config, flag, False)

    manifest = read_java("V50BuildManifest.java")
    require('PRODUCT_NAME = "V50 PRIME"' in manifest, "PRODUCT_IDENTITY")
    direct_bool(manifest, "ENGINEERING_COMPLETE", False)
    direct_bool(manifest, "REALTIME_PROVEN", False)
    require("MODEL_PROVEN =" in manifest
            and "V50SelectivePolicyArtifact.MODEL_GATE_PASSED" in manifest,
            "MODEL_PROOF_NOT_ARTIFACT_OWNED")
    require("mayLabelFullRelease()" in manifest, "FULL_RELEASE_GATE_MISSING")

    owners = read_java("V50FieldOwnerRegistry.java")
    entries = re.findall(r'add\("([^"]+)"\s*,\s*([^,]+),', owners)
    fields = [entry[0] for entry in entries]
    require(len(fields) >= 13, "FIELD_OWNER_COUNT", str(len(fields)))
    require(len(fields) == len(set(fields)), "DUPLICATE_FIELD_OWNER")
    for field in (
        "NumberP", "RawActionP", "RankScore", "RankAdjustedP",
        "SelectedHitP", "DomainCertificate", "Lane",
        "AuthorizationSignature", "DecisionAction", "SettlementResult",
    ):
        require(field in fields, "FIELD_OWNER_MISSING", field)

    gate = read_java("DecisionGateEngine.java")
    typed_gate = gate.split("public V50Models.AuthorizedDecision decideV50", 1)[1]
    typed_gate = typed_gate.split("public V50Models.AuthorizedDecision blockV50", 1)[0]
    require(typed_gate.count("action = V50Enums.DecisionAction.BET") == 1,
            "V50_BET_OWNER_COUNT")
    for token in (
        "V50BuildManifest.ENGINEERING_COMPLETE",
        "V50BuildManifest.MODEL_PROVEN",
        "V50BuildManifest.REALTIME_PROVEN",
        "V50SelectivePolicyArtifact.MODEL_GATE_PASSED",
        "getTradeByTargetIssue", "hasOpenRealTrade",
    ):
        require(token in typed_gate, "V50_GATE_GUARD_MISSING", token)
    for forbidden in ("readDouble(", "readString(", "java.lang.reflect"):
        require(forbidden not in typed_gate, "V50_TYPED_GATE_REFLECTION", forbidden)

    runtime = read_java("V50RuntimeOrchestrator.java")
    for stage in (
        "featureEngine.build(", "stateEngine.build(", "mixtureEngine.fuse(",
        "actionBuilder.build(", "ranker.rank(", "calibration.calibrate(",
        "LanePolicyCompiler.compileV50(", "decisionGate.decideV50(",
        "ledgerEngine.persist(",
    ):
        require(stage in runtime, "V50_RUNTIME_STAGE_MISSING", stage)
    for forbidden in ("V20", "V41", "V42", "V47", "V48", "V49RealtimePipeline"):
        require(forbidden not in runtime, "LEGACY_REFERENCE_IN_V50_RUNTIME", forbidden)

    auto = read_java("AutoFetchService.java")
    require("if (AppConfig.ENABLE_V50_PRIME)" in auto
            and "return buildV50PrimeDecision" in auto,
            "AUTOFETCH_V50_EARLY_ROUTE_MISSING")
    for method_guard in (
        "postLearningTick",
        "postMicroWalkForward",
        "postTeacherMatrixForDraw",
        "postV20CauseChainForDraw",
    ):
        method = auto.find("private void " + method_guard)
        require(method >= 0, "AUTOFETCH_METHOD_MISSING", method_guard)
        body = auto[method:method + 700]
        require("if (AppConfig.ENABLE_V50_PRIME) return;" in body,
                "LEGACY_BACKGROUND_NOT_BLOCKED", method_guard)

    atomic = read_java("V50AtomicDecisionLedgerEngine.java")
    for token in (
        "V50Repository.transaction(", "insertIssuePrediction(",
        "insertNumberEvents(", "insertPositionActions(",
        "insertAuthorizedDecision(", "LedgerReconciliationEngine.reconcile(",
        "persistV50DecisionAndTrade(",
    ):
        require(token in atomic, "ATOMIC_LEDGER_STAGE_MISSING", token)
    learning = read_java("V50FullInformationLearningEngine.java")
    for token in (
        "db.beginTransaction()", "db.setTransactionSuccessful()",
        "NUMBER_EVENT_COUNT_NOT_100", "POSITION_ACTION_COUNT_NOT_10",
        "updateTeacher(", "updateExpertRegret(", "updateAttribution(",
        "updateDomain(",
    ):
        require(token in learning, "LEARNING_CLOSED_LOOP_MISSING", token)

    build = (ROOT / "app" / "build.gradle").read_text("utf-8")
    require("versionCode 500500725" in build, "GRADLE_VERSION_CODE")
    require(re.search(r"versionName\s+['\"]V50\.0\.50072403['\"]", build)
            is not None, "GRADLE_VERSION_NAME")
    require("JavaVersion.VERSION_1_7" in build, "JAVA7_TARGET_MISSING")

    java_files = sorted((ROOT / "app" / "src" / "main" / "java").rglob("*.java"))
    java8_patterns = (
        ("JAVA8_LAMBDA", re.compile(r"->")),
        ("JAVA8_METHOD_REFERENCE", re.compile(r"::")),
        ("JAVA8_COLLECTION_API", re.compile(
            r"\.(?:compute|computeIfAbsent|computeIfPresent|getOrDefault|merge|"
            r"putIfAbsent|removeIf|forEach|stream|parallelStream)\s*\("
        )),
        ("JAVA8_COMPARATOR_API", re.compile(r"\bComparator\.comparing\s*\(")),
        ("JAVA8_INSTANCE_SORT", re.compile(
            r"(?<!Collections)(?<!Arrays)\.sort\s*\("
        )),
        ("JAVA8_DOUBLE_API", re.compile(r"\bDouble\.isFinite\s*\(")),
        ("JAVA8_MATH_API", re.compile(r"\bMath\.floorMod\s*\(")),
    )
    for path in java_files:
        code = java_code_only(path.read_text("utf-8"))
        for error, pattern in java8_patterns:
            require(pattern.search(code) is None, error, str(path.relative_to(ROOT)))
    require(
        "private static int[] descendingOrder(final double[] p)"
        in read_java("V49RealtimePipelineEngine.java"),
        "JAVA7_ANONYMOUS_CAPTURE_NOT_FINAL",
        "V49RealtimePipelineEngine.descendingOrder",
    )

    return emit("v50_architecture_contract", {
        "required_v50_files": len(REQUIRED_V50_FILES),
        "field_owners": len(fields),
        "java7_source_files": len(java_files),
        "active_runtime": "V50_ACTIVE_RUNTIME",
        "legacy_mode": "LEGACY_READ_ONLY_OBSERVER",
        "unique_v50_bet_owner": "DecisionGateEngine",
        "fixed_k": 3,
        "max_real_trade_per_issue": 1,
        "engineering_complete": False,
        "realtime_proven": False,
    })


if __name__ == "__main__":
    raise SystemExit(main())
