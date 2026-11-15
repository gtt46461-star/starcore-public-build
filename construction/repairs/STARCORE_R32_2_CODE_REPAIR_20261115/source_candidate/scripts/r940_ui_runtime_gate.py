#!/usr/bin/env python3
"""Fail-closed source gate for the R940 UI/runtime stabilization patch."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/ethk3/novaonepro/MainActivity.java").read_text(encoding="utf-8")
FRAME = (ROOT / "app/src/main/java/com/ethk3/novaonepro/FrameMetricsR906.java").read_text(encoding="utf-8")
CRASH = (ROOT / "app/src/main/java/com/ethk3/novaonepro/CrashRecorder.java").read_text(encoding="utf-8")
REPORT = (ROOT / "app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java").read_text(encoding="utf-8")
UNIT = (ROOT / "app/src/test/java/com/ethk3/novaonepro/Batch3ContractsR929Test.java").read_text(encoding="utf-8")
MANIFEST = json.loads((ROOT / "release/release_manifest.json").read_text(encoding="utf-8"))


def require(ok: bool, code: str) -> None:
    if not ok:
        raise SystemExit("R940_GATE_FAIL:" + code)


require(MANIFEST["versionName"] == "V57.21.1.0.10-R5.4.1.0.10-941", "VERSION_NAME")
require(MANIFEST["versionCode"] == 577250941, "VERSION_CODE")
require(MANIFEST["databaseName"] == "binance_k3_nova_one_pro_v57.db", "DATABASE")
require(MANIFEST["gameCode"] == "Binance1-PK10", "GAME")
require(MANIFEST["schema"]["userVersion"] == 36, "DB_VERSION")
require(MANIFEST["runtimeContract"]["decisionOwner"] == "DecisionGateEngine_ONLY", "DECISION_OWNER")
require(MANIFEST["runtimeContract"]["settlementOwner"] == "SettlementEngine_ONLY", "SETTLEMENT_OWNER")
require("panel.addView(txtR925Background);" in MAIN, "BACKGROUND_CARD_PARENT")
require("fixedRoot.addView(txtR925Background" not in MAIN, "ILLEGAL_VIEW_REPARENT")
require("contentGroup.removeView(rootView)" not in MAIN, "ROOT_DETACH")
require("List<Models.Trade> trades = db.getRecentTrades" in MAIN, "BATCH_TRADE_READ")
heavy = MAIN[MAIN.index("private HeavySnapshot loadHeavySnapshotSafely"):
             MAIN.index("private void loadRealBetDiagnosisForSnapshot")]
require("getTradeByTargetIssue" not in heavy, "N_PLUS_ONE_TRADE_READ")
diag = MAIN[MAIN.index("private void loadFullDiagnosisClosedLoopForSnapshot"):
            MAIN.index("private String buildAdaptiveJudgeStateText")]
require("ensureDiagnosisLoopTablesNow" not in diag, "UI_TABLE_WRITE")
require("refreshDiagnosisClosedLoopState" not in diag, "UI_DIAGNOSIS_WRITE")
require("if (destroyed || !r941UiResumed || !r940DiagnosticPanelInstalled)" in MAIN, "ASYNC_LIFECYCLE_GUARD")
require("public static String latestSummary()" in FRAME, "FRAME_EXPORT")
require("public static String detailedSummary()" in CRASH, "CRASH_EXPORT")
require("0C.R940界面卡顿与闪退取证" in REPORT, "REPORT_DIAGNOSTICS")
require("ZeroCounterInterpretation=NOT_ONLY_RUNTIME" in REPORT, "ZERO_COUNTER_EXPLANATION")
require('policy.policyHash, H0, H1, "global", H1, "100", H3' in UNIT,
        "CALIBRATION_TEST_DOMAIN_SCHEMA")
require('policy, raw, artifact, "101", "global", H1, 120' in UNIT,
        "CALIBRATION_TEST_CURRENT_DOMAIN_SCHEMA")
print("R941_RETAINED_UI_SOURCE_GATE=PASS|checks=21|scope=SOURCE_ONLY|physical=RETEST_REQUIRED")
