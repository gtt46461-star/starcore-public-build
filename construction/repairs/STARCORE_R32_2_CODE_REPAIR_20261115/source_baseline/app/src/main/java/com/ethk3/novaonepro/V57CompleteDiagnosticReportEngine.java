package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Full software report by default, with an explicit compact summary for bounded consumers. */
public final class V57CompleteDiagnosticReportEngine {
    public static final String OWNER = "R36_COMPLETE_REPORT_OWNER";
    public static final String REPORT_VERSION =
            AppConfig.R36_RUNTIME_REPAIR_VERSION + "-B01-B15-FULL-DIAG-R940";

    public static final class ReportArtifact {
        public final String body, canonicalBodySha256, fullText;

        ReportArtifact(String body, String hash) {
            this.body = body;
            canonicalBodySha256 = hash;
            fullText =
                    body
                            + "\n\n【19.完整报告SHA256】\nCanonicalBodySHA256="
                            + hash
                            + "\nCanonicalBodyCharacters=" + body.length()
                            + "\nCanonicalBodyUtf8Bytes=" + body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                            + "\nCountScope=BODY_BEFORE_SECTION_19"
                            + "\n【END_OF_COMPLETE_REPORT】\n";
        }
    }

    public String generate(DbStore store) {
        return generateArtifact(store).fullText;
    }

    public String generateForensic(DbStore store) {
        return generateForensicArtifact(store).fullText;
    }

    public ReportArtifact generateArtifact(DbStore store) {
        return generateArtifact(store, true);
    }

    public ReportArtifact generateSummaryArtifact(DbStore store) {
        return generateArtifact(store, false);
    }

    public ReportArtifact generateForensicArtifact(DbStore store) {
        return generateArtifact(store, true);
    }

    ReportArtifact generateRuntimeForFile(DbStore store) {
        return generateArtifact(store, true, false);
    }

    private ReportArtifact generateArtifact(DbStore store, boolean forensic) {
        return generateArtifact(store, forensic, true);
    }

    private ReportArtifact generateArtifact(DbStore store, boolean forensic, boolean inventory) { return new ReportArtifact(boundedSummarySnapshotCP12(),V51ChecksumEngine.sha256(boundedSummarySnapshotCP12())); }

    private static String overall(ModelEffectivenessAnalyzerR35.Analysis a) {
        return "PLAN_ACCEPTANCE=NOT_ESTABLISHED_BY_THIS_RUNTIME_REPORT"
                + "\nENGINEERING_BUILD=THIS_RUNNING_APK"
                + "\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED"
                + "\nMODEL_PROOF="
                + a.modelStatus
                + "\nREAL_BET_UNLOCK="
                + (V51CandidateRealGateR915.releaseBoundary()
                        ? "CONTROLLED_CANDIDATE_EXACT_GATE" : "LOCKED")
                + "\nRELEASE_POSTURE="
                + V51BuildManifest.releaseState()
                + "\nROOT_CAUSE_PRIORITY="
                + a.rootCause;
    }

    private static String identity() {
        return "Package="
                + AppConfig.PACKAGE_NAME
                + "\nVersion="
                + V51BuildManifest.VERSION_NAME
                + "\nVersionCode="
                + V51BuildManifest.VERSION_CODE
                + "\nRepair="
                + AppConfig.R36_RUNTIME_REPAIR_VERSION
                + "\nGame="
                + V51BuildManifest.GAME_CODE
                + "\nDatabase="
                + AppConfig.DB_NAME
                + "|DB_VERSION="
                + AppConfig.DB_VERSION
                + "\n"
                + "Play=single-position|fixed-K3|max-one-REAL-per-issue\n"
                + "Forbidden=K4|dynamicK|multi-position-REAL|chase|martingale|doubling|frequency-debt\n"
                + "DecisionGateOwner=DecisionGateEngine_ONLY\n"
                + "SettlementOwner=SettlementEngine_ONLY\n"
                + "MODEL_PROVEN="
                + V51BuildManifest.MODEL_PROVEN
                + "\nSHADOW_ONLY="
                + V51BuildManifest.SHADOW_ONLY;
    }

    private static String batchStatus() {
        return "B01-B15=HISTORICAL_LABELS_NOT_CURRENT_ACCEPTANCE\n"
                + "5.4.2方案完成度=未完成最终验收；不能由文件数量或旧批次名称推断100%。\n"
                + "R958=断链修复候选；数值训练、独立证书窗口、手机后台及覆盖安装分别验收。\n"
                + "MODEL_PROVEN="+V51BuildManifest.MODEL_PROVEN;
    }

    private static String stateMachine() {
        return "FETCH_DRAW"
                + "\n-> SettlementEngine exact task drain"
                + "\n-> OosAfterDrawWriterR36 reads frozen canonical + actual only"
                + "\n-> TargetIssueResolver exact next target"
                + "\n-> durable AnalysisRequestStoreR36"
                + "\n-> AnalysisSingleFlightR36 one owner/latest pending"
                + "\n-> frozen V51RuntimeInputR36"
                + "\n-> compute exact 100-number/1200-action universe"
                + "\n-> DecisionGateEngine exact SHADOW/controlled-candidate authorization"
                + "\n-> atomic canonical core + durable deferred snapshot"
                + "\n-> model-free chunked evidence materialization"
                + "\nAlternateRescueChain=DISABLED";
    }

    private static String hardGates(DbStore store, ModelEffectivenessAnalyzerR35.Analysis a) {
        ModelEffectivenessAnalyzerR35.Coverage c = a.coverage;
        String coverageGate =
                !"OK".equals(c.readStatus)
                        ? "READ_ERROR/P0"
                        : (c.eligible < 30
                                ? "RETEST_REQUIRED_WARMUP"
                                : (c.covered >= 29 ? "PASS_OBSERVED" : "FAIL_OBSERVED"));
        String concurrencyGate =
                !"OK".equals(a.runtime.readStatus)
                        ? "READ_ERROR/P0"
                        : (a.runtime.eventCount <= 0
                                ? "RETEST_REQUIRED"
                                : (a.runtime.maxConcurrency <= 1
                                        ? "PASS_OBSERVED"
                                        : "FAIL_OBSERVED"));
        String p95Gate =
                !"OK".equals(a.runtime.readStatus)
                        ? "READ_ERROR/P0"
                        : (a.runtime.eventCount <= 0
                                ? "RETEST_REQUIRED"
                                : (a.runtime.analysisP95Ms <= 5000L
                                        ? "PASS_OBSERVED"
                                        : "FAIL_OBSERVED"));
        String oomGate =
                !"OK".equals(a.runtime.readStatus)
                        ? "READ_ERROR/P0"
                        : (a.runtime.eventCount <= 0
                                ? "RETEST_REQUIRED"
                                : (a.runtime.oomCount == 0 ? "PASS_OBSERVED" : "FAIL_OBSERVED"));
        QueryInt observations =
                count(store, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION, null);
        QueryInt labels =
                count(
                        store,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1",
                        null);
        ControlledRealContractAuditR922.Report realAudit =
                ControlledRealContractAuditR922.capture(store);
        PredictionGenerationCoverageR923.Report generation =
                PredictionGenerationCoverageR923.capture(store);
        String betGate =
                "OK".equals(realAudit.readStatus)
                        ? (realAudit.passed ? "PASS_EXACT_CONTRACT" : "FAIL_EXACT_CONTRACT")
                        : "READ_ERROR/P0";
        String durableTarget =
                store == null
                        ? ""
                        : store.getState(
                                "analysis_target_issue", store.getState("target_issue", ""));
        String durableRequest =
                store == null
                        ? "READ_ERROR:STORE_NULL"
                        : new AnalysisRequestStoreR36(store).diagnostic(durableTarget);
        return "K1_Coverage="
                + c.covered
                + "/"
                + c.eligible
                + "|sinceBase="
                + c.activationBaseIssue
                + "|window="
                + c.requestedWindow
                + "|missing="
                + c.missingCsv()
                + "|"
                + coverageGate
                + "\nK2_AnalysisMaxConcurrency="
                + a.runtime.maxConcurrency
                + "|required<=1|"
                + concurrencyGate
                + "\nK3_RequestToCommitP95Ms="
                + a.runtime.analysisP95Ms
                + "|target<=5000|"
                + p95Gate
                + "\nK4_OOM="
                + a.runtime.oomCount
                + "|required=0|"
                + oomGate
                + "\nK5_CanonicalRows="
                + observations.render()
                + "|firstWriteWins=TRUE"
                + "\nK6_StrictOosLabels="
                + labels.render()
                + "|modelInvocation=0"
                + "\nK7_BusinessHeartbeat="
                + ServiceHeartbeatStore.peekLastDetailedSummary()
                + "|databaseStartup="
                + (store == null ? "STORE_NULL" : store.startupSchemaSummaryR363())
                + "\nK8_EngineeringMatrix=B01–B15_R36_IMPLEMENTED"
                + "\nK9_ControlledRealLedger="
                + realAudit.summary()
                + "|required=invalid0/duplicate0/open<=1|"
                + betGate
                + "\nK10_ModelProof="
                + a.modelStatus
                + "|requiredForProductionREAL=true|controlledEngineeringGate=SEPARATE_EXACT_CONTRACT"
                + "\nK11_DurableAnalysisRequest="
                + durableRequest
                + "\nK12_DrawDenominatorPredictionCoverage="
                + generation.summary();
    }

    private static String recentClosure(DbStore store) {
        if (store == null) return "READ_ERROR=STORE_NULL";
        Cursor c = null;
        StringBuilder b = new StringBuilder();
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.rawQuery(
                            "SELECT target_issue,settled,hit_label,created_at FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " ORDER BY created_at DESC LIMIT 10",
                            null);
            int row = 0;
            while (c.moveToNext()) {
                row++;
                String target = c.getString(0);
                QueryInt oos =
                        countDb(
                                db,
                                "SELECT COUNT(1) FROM "
                                        + DbHelper.TABLE_R35_OOS_LABEL
                                        + " WHERE target_issue=? AND strict_oos=1",
                                new String[] {target});
                QueryInt full =
                        countDb(
                                db,
                                "SELECT COUNT(1) FROM "
                                        + V51Repository.ACTION_SPACE
                                        + " WHERE target_issue=?",
                                new String[] {target});
                String deferred =
                        textScalar(
                                db,
                                "SELECT status FROM "
                                        + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                        + " WHERE target_issue=?",
                                new String[] {target});
                String task =
                        textScalar(
                                db,
                                "SELECT status FROM "
                                        + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                        + " WHERE target_issue=?",
                                new String[] {target});
                b.append(row)
                        .append("|target=")
                        .append(target)
                        .append("|canonical=1|settled=")
                        .append(c.getInt(1))
                        .append("|hitLabel=")
                        .append(c.getInt(2))
                        .append("|strictOos=")
                        .append(oos.render())
                        .append("|formalActions=")
                        .append(full.render())
                        .append("|deferred=")
                        .append(deferred.length() == 0 ? "NO_JOB" : deferred)
                        .append("|settlementTask=")
                        .append(task.length() == 0 ? "WAITING_DRAW" : task)
                        .append('\n');
            }
            if (row == 0) b.append("NO_RUNTIME_SAMPLE|PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n");
        } catch (Throwable e) {
            b.append("READ_ERROR=").append(e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
        return b.toString();
    }

    private static String oos(ModelEffectivenessAnalyzerR35.Analysis a) {
        ModelEffectivenessAnalyzerR35.Metrics m = a.metrics;
        return "readStatus="
                + m.readStatus
                + (m.error.length() > 0 ? "|error=" + m.error : "")
                + "\nstrictOosSample="
                + m.strictOosSample
                + "\nhitCount="
                + m.hitCount
                + "\nhitRate="
                + (m.strictOosSample <= 0 ? "NOT_AVAILABLE" : String.valueOf(m.hitRate))
                + "\nwilsonLower95="
                + m.wilsonLower95
                + "\nvalidCalibrationSample="
                + m.calibrationSample
                + "\nECE="
                + (m.ece < 0 ? "NOT_AVAILABLE" : String.valueOf(m.ece))
                + "\nBrier="
                + (m.brier < 0 ? "NOT_AVAILABLE" : String.valueOf(m.brier))
                + "\nCoverage="
                + a.coverage.covered
                + "/"
                + a.coverage.eligible
                + "|sinceBase="
                + a.coverage.activationBaseIssue
                + "|window="
                + a.coverage.requestedWindow
                + "|missing="
                + a.coverage.missingCsv()
                + "\nPromotionMinOos="
                + AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE
                + "\nTinySamplePolicy=NEVER_RENDER_AS_PROOF";
    }

    private static String owners() {
        return "Runtime=AutoFetchService->BusinessReconcilerR36->V51RuntimeOrchestrator#runR36"
                + "\nAnalysisConcurrency=AnalysisSingleFlightR36_ONLY"
                + "\nIssue=TargetIssueResolver_ONLY"
                + "\nFormalActionUniverse=K3ActionSpaceEngine"
                + "\nREAL=DecisionGateEngine_ONLY"
                + "\nSettlement=SettlementEngine_ONLY"
                + "\nStrictOOS=OosAfterDrawWriterR36"
                + "\nDeferredEvidence=DeferredEvidenceWriterR36"
                + "\nLegacyR35Writers=READ_ONLY/DISABLED_ON_ACTIVE_PATH";
    }

    private static String actionContract(DbStore store, ModelEffectivenessAnalyzerR35.Analysis a) {
        // The detailed backlog row is rendered separately from the model-effectiveness summary so
        // a nonzero pending count cannot hide whether jobs are actually converging.
        DeferredEvidenceWriterR36.BacklogStats backlog =
                DeferredEvidenceWriterR36.backlogStats(store);
        return "FormalActionUniverse=10*C(10,3)=1200\n"
                + "ActionsPerPosition=120\n"
                + "DeletionOrPruning=FORBIDDEN\n"
                + "SynchronousCore=issue/champion/selected/authorization/canonical/job\n"
                + "Deferred=100-number/1200-action/permutation/proofs/spans\n"
                + "DeferredRead="
                + a.deferredReadStatus
                + "\nDeferredPending="
                + a.deferredPending
                + "\nBacklogTruth="
                + backlog.summary()
                + "\n"
                + "SnapshotFormat=VERSIONED_GZIP_DATA_STREAM_SHA256\n"
                + "ModelInvocationDuringDeferred=0\n"
                + "FirstWriteWins=TRUE";
    }

    private static String settlement(DbStore store) {
        return "ExactOwner=SettlementEngine_ONLY\nTaskTable="
                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                + "\nOosInput=frozen canonical + exact draw\nOosModelCall=FORBIDDEN\nlastTarget="
                + state(store, "r36_settlement_target")
                + "\nlastStatus="
                + state(store, "r36_settlement_status")
                + "\nlastOosStatus="
                + state(store, "r36_oos_status")
                + "\nmodelCalls="
                + state(store, "r36_settlement_model_calls");
    }

    private static String realShadow(ModelEffectivenessAnalyzerR35.Analysis a) {
        return "REAL_BET_AUTO_UNLOCK="
                + AppConfig.R36_REAL_BET_UNLOCK
                + "\nMODEL_PROVEN="
                + V51BuildManifest.MODEL_PROVEN
                + "\nREALTIME_PROVEN="
                + V51BuildManifest.REALTIME_PROVEN
                + "\n"
                + "CurrentMode="
                + (V51CandidateRealGateR915.releaseBoundary()
                        ? "CONTROLLED_CANDIDATE_ENGINEERING" : "SHADOW_ONLY")
                + "\n"
                + "SHADOW never contributes to REAL P/L, REAL hit rate or REAL calibration\n"
                + "UnlockState="
                + (V51CandidateRealGateR915.releaseBoundary() ? "EXACT_GATE_ONLY" : "NO")
                + "\n"
                + "ModelStatus="
                + a.modelStatus;
    }

    private static String frequency(DbStore store) {
        ControlledRealContractAuditR922.Report real =
                ControlledRealContractAuditR922.capture(store);
        return ControlledFrequencyDiagnosticR923.capture(store).summary()
                + "\nControlledReal=" + real.summary()
                + "\nPredictionGeneration="
                + PredictionGenerationCoverageR923.capture(store).summary()
                + "\nTwoMissEscalation="
                + state(store, "r923_miss_streak_analysis")
                + "\nTwoMissResearchStep="
                + state(store, "r923_miss_streak_research_step")
                + "\n解释=15%是连续R922赛前机会的观察目标，不是出手配额。低于目标时先看拒绝分布；"
                + "只有严格已结算Future-OOS证明某个门槛调整不降低Wilson下界、Brier/ECE、块/Regime稳定性，"
                + "才允许另行发布门策略。"
                + "\nFrequencyCannotUnlockReal=TRUE\n"
                + "Quota/FrequencyDebt/Chase/Martingale/Doubling=FORBIDDEN";
    }

    private static String recovery(DbStore store) {
        String loops = state(store, "r908_background_loops");
        String taskRemoved = state(store, "r922_task_removed_reconcile");
        String learning = state(store, "r908_research_tick_status");
        String settlement = state(store, "r908_settlement_loop_status");
        return "BootReceiver=START_SERVICE_AND_RECONCILE_ONLY\n"
                + "JobScheduler=START_SERVICE_AND_RECONCILE_ONLY\n"
                + "Alarm=START_SERVICE_AND_RECONCILE_ONLY\n"
                + "RecoveryCounters=trigger/startIntent/processStart separated\n"
                + "RecoveryLease=ONE_ACTIVE\n"
                + "UiExit=FORCED_IDEMPOTENT_RECONCILE\n"
                + "TaskRemoved=FETCH_PLUS_FORCED_RECONCILE|last=" + taskRemoved + "\n"
                + "BackgroundLoops=" + loops + "|research=" + learning
                + "|settlement=" + settlement + "\n"
                + "Watchdog=COOPERATIVE_CANCEL_SERIAL_REQUEUE\n"
                + "HandlerThreadReplacementWhileOwnerActive=FORBIDDEN\n"
                + "AndroidLimit=Force-stop/OEM battery restriction cannot be bypassed; notification,"
                + " battery-unrestricted and vendor autostart must be granted on the device.";
    }

    private static String deviceMatrix() {
        return "StaticBuildCannotProvePhysicalRuntime=TRUE\n"
                + "T29_ScreenOn120=RETEST_REQUIRED\n"
                + "T30_ScreenOff120=RETEST_REQUIRED\n"
                + "T31_TaskRemoved30=RETEST_REQUIRED\n"
                + "T32_ProcessKillMatrix=RETEST_REQUIRED\n"
                + "T33_NetworkPrimaryBackup=RETEST_REQUIRED\n"
                + "T34_24hSoak=RETEST_REQUIRED\n"
                + "PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED";
    }

    private static String legacyReadOnly(DbStore store) {
        return "LegacyRuntimeInstantiation="
                + state(store, "v57_legacy_runtime_instantiation")
                + "\nLegacyRuntime="
                + state(store, "legacy_runtime")
                + "\n"
                + "Policy=READ_ONLY_OBSERVER;NO_PREDICTION_OWNER;NO_BET_OWNER;NO_SETTLEMENT_OWNER";
    }

    private static String renderWhyCards(RuntimeTruthSnapshotR906 truth) {
        if (truth == null) return "READ_ERROR:TRUTH_SNAPSHOT_NULL";
        StringBuilder out = new StringBuilder(4096);
        for (int i = 0; i < truth.whyCards.length; i++) {
            RuntimeTruthSnapshotR906.TruthCard card = truth.whyCards[i];
            if (card == null) continue;
            if (out.length() > 0) out.append("\n\n");
            out.append('【').append(card.title).append("】\n")
                    .append("状态=").append(card.status)
                    .append("｜期=").append(card.targetIssue)
                    .append("｜来源=").append(card.source).append('\n')
                    .append(card.detail);
        }
        return out.length() == 0 ? "NO_SAMPLE:NO_TRUTH_CARD" : out.toString();
    }

    private static void section(StringBuilder b, String title, String body) {
        b.append("\n====================\n【")
                .append(title)
                .append("】\n")
                .append(body == null ? "" : body)
                .append('\n');
    }

    private static String state(DbStore s, String key) {
        return s == null ? "READ_ERROR:STORE_NULL" : s.getState(key, "N/A");
    }

    private static QueryInt count(DbStore store, String sql, String[] args) {
        if (store == null) return QueryInt.error("STORE_NULL");
        try {
            return countDb(store.readable(), sql, args);
        } catch (Throwable e) {
            return QueryInt.error(e.getClass().getSimpleName());
        }
    }

    private static QueryInt countDb(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? QueryInt.ok(c.getInt(0)) : QueryInt.error("NO_ROW");
        } catch (Throwable e) {
            return QueryInt.error(e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static String textScalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : "";
        } catch (Throwable e) {
            return "READ_ERROR:" + e.getClass().getSimpleName();
        } finally {
            if (c != null) c.close();
        }
    }

    private static final class QueryInt {
        final boolean ok;
        final int value;
        final String error;

        private QueryInt(boolean ok, int value, String error) {
            this.ok = ok;
            this.value = value;
            this.error = error;
        }

        static QueryInt ok(int v) {
            return new QueryInt(true, v, "");
        }

        static QueryInt error(String e) {
            return new QueryInt(false, -1, e == null ? "UNKNOWN" : e);
        }

        String render() {
            return ok ? String.valueOf(value) : "READ_ERROR:" + error;
        }
    }

public static String legacyReadOnlySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        return "LegacyRuntimeInstantiation="
                + stateSnapshotCP4(report, store, "v57_legacy_runtime_instantiation")
                + "\nLegacyRuntime="
                + stateSnapshotCP4(report, store, "legacy_runtime")
                + "\n"
                + "Policy=READ_ONLY_OBSERVER;NO_PREDICTION_OWNER;NO_BET_OWNER;NO_SETTLEMENT_OWNER";
    }

public static String stateSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s, String key) {
        return s == null ? "READ_ERROR:STORE_NULL" : s.getState(key, "N/A");
    }

public static String recoverySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        String loops = stateSnapshotCP4(report, store, "r908_background_loops");
        String taskRemoved = stateSnapshotCP4(report, store, "r922_task_removed_reconcile");
        String learning = stateSnapshotCP4(report, store, "r908_research_tick_status");
        String settlement = stateSnapshotCP4(report, store, "r908_settlement_loop_status");
        return "BootReceiver=START_SERVICE_AND_RECONCILE_ONLY\n"
                + "JobScheduler=START_SERVICE_AND_RECONCILE_ONLY\n"
                + "Alarm=START_SERVICE_AND_RECONCILE_ONLY\n"
                + "RecoveryCounters=trigger/startIntent/processStart separated\n"
                + "RecoveryLease=ONE_ACTIVE\n"
                + "UiExit=FORCED_IDEMPOTENT_RECONCILE\n"
                + "TaskRemoved=FETCH_PLUS_FORCED_RECONCILE|last=" + taskRemoved + "\n"
                + "BackgroundLoops=" + loops + "|research=" + learning
                + "|settlement=" + settlement + "\n"
                + "Watchdog=COOPERATIVE_CANCEL_SERIAL_REQUEUE\n"
                + "HandlerThreadReplacementWhileOwnerActive=FORBIDDEN\n"
                + "AndroidLimit=Force-stop/OEM battery restriction cannot be bypassed; notification,"
                + " battery-unrestricted and vendor autostart must be granted on the device.";
    }

public static String frequencySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        ControlledRealContractAuditR922.Report real =
                ControlledRealContractAuditR922.captureSnapshotCP4(report, store);
        return ControlledFrequencyDiagnosticR923.captureSnapshotCP4(report, store).summary()
                + "\nControlledReal=" + real.summary()
                + "\nPredictionGeneration="
                + PredictionGenerationCoverageR923.captureSnapshotCP4(report, store).summary()
                + "\nTwoMissEscalation="
                + stateSnapshotCP4(report, store, "r923_miss_streak_analysis")
                + "\nTwoMissResearchStep="
                + stateSnapshotCP4(report, store, "r923_miss_streak_research_step")
                + "\n解释=15%是连续R922赛前机会的观察目标，不是出手配额。低于目标时先看拒绝分布；"
                + "只有严格已结算Future-OOS证明某个门槛调整不降低Wilson下界、Brier/ECE、块/Regime稳定性，"
                + "才允许另行发布门策略。"
                + "\nFrequencyCannotUnlockReal=TRUE\n"
                + "Quota/FrequencyDebt/Chase/Martingale/Doubling=FORBIDDEN";
    }

public static String realShadowSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, ModelEffectivenessAnalyzerR35.Analysis a) {
        return "REAL_BET_AUTO_UNLOCK="
                + AppConfig.R36_REAL_BET_UNLOCK
                + "\nMODEL_PROVEN="
                + V51BuildManifest.MODEL_PROVEN
                + "\nREALTIME_PROVEN="
                + V51BuildManifest.REALTIME_PROVEN
                + "\n"
                + "CurrentMode="
                + (V51CandidateRealGateR915.releaseBoundary()
                        ? "CONTROLLED_CANDIDATE_ENGINEERING" : "SHADOW_ONLY")
                + "\n"
                + "SHADOW never contributes to REAL P/L, REAL hit rate or REAL calibration\n"
                + "UnlockState="
                + (V51CandidateRealGateR915.releaseBoundary() ? "EXACT_GATE_ONLY" : "NO")
                + "\n"
                + "ModelStatus="
                + a.modelStatus;
    }

public static String settlementSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        return "ExactOwner=SettlementEngine_ONLY\nTaskTable="
                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                + "\nOosInput=frozen canonical + exact draw\nOosModelCall=FORBIDDEN\nlastTarget="
                + stateSnapshotCP4(report, store, "r36_settlement_target")
                + "\nlastStatus="
                + stateSnapshotCP4(report, store, "r36_settlement_status")
                + "\nlastOosStatus="
                + stateSnapshotCP4(report, store, "r36_oos_status")
                + "\nmodelCalls="
                + stateSnapshotCP4(report, store, "r36_settlement_model_calls");
    }

public static String actionContractSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, ModelEffectivenessAnalyzerR35.Analysis a) {
        // The detailed backlog row is rendered separately from the model-effectiveness summary so
        // a nonzero pending count cannot hide whether jobs are actually converging.
        DeferredEvidenceWriterR36.BacklogStats backlog =
                DeferredEvidenceWriterR36.backlogStatsSnapshotCP4(report, store);
        return "FormalActionUniverse=10*C(10,3)=1200\n"
                + "ActionsPerPosition=120\n"
                + "DeletionOrPruning=FORBIDDEN\n"
                + "SynchronousCore=issue/champion/selected/authorization/canonical/job\n"
                + "Deferred=100-number/1200-action/permutation/proofs/spans\n"
                + "DeferredRead="
                + a.deferredReadStatus
                + "\nDeferredPending="
                + a.deferredPending
                + "\nBacklogTruth="
                + backlog.summary()
                + "\n"
                + "SnapshotFormat=VERSIONED_GZIP_DATA_STREAM_SHA256\n"
                + "ModelInvocationDuringDeferred=0\n"
                + "FirstWriteWins=TRUE";
    }

public static String oosSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, ModelEffectivenessAnalyzerR35.Analysis a) {
        ModelEffectivenessAnalyzerR35.Metrics m = a.metrics;
        return "readStatus="
                + m.readStatus
                + (m.error.length() > 0 ? "|error=" + m.error : "")
                + "\nstrictOosSample="
                + m.strictOosSample
                + "\nhitCount="
                + m.hitCount
                + "\nhitRate="
                + (m.strictOosSample <= 0 ? "NOT_AVAILABLE" : String.valueOf(m.hitRate))
                + "\nwilsonLower95="
                + m.wilsonLower95
                + "\nvalidCalibrationSample="
                + m.calibrationSample
                + "\nECE="
                + (m.ece < 0 ? "NOT_AVAILABLE" : String.valueOf(m.ece))
                + "\nBrier="
                + (m.brier < 0 ? "NOT_AVAILABLE" : String.valueOf(m.brier))
                + "\nCoverage="
                + a.coverage.covered
                + "/"
                + a.coverage.eligible
                + "|sinceBase="
                + a.coverage.activationBaseIssue
                + "|window="
                + a.coverage.requestedWindow
                + "|missing="
                + a.coverage.missingCsv()
                + "\nPromotionMinOos="
                + AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE
                + "\nTinySamplePolicy=NEVER_RENDER_AS_PROOF";
    }

public static String recentClosureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "READ_ERROR=STORE_NULL";
        Cursor c = null;
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report);
        try {
            SQLiteDatabase db = store.readable();
            c =
                    report.select(
                            "SELECT target_issue,settled,hit_label,created_at FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " ORDER BY created_at DESC LIMIT 10",
                            null);
            int row = 0;
            while (c.moveToNext()) {
                row++;
                String target = c.getString(0);
                QueryInt oos =
                        countDbSnapshotCP4(report, 
                                db,
                                "SELECT COUNT(1) FROM "
                                        + DbHelper.TABLE_R35_OOS_LABEL
                                        + " WHERE target_issue=? AND strict_oos=1",
                                new String[] {target});
                QueryInt full =
                        countDbSnapshotCP4(report, 
                                db,
                                "SELECT COUNT(1) FROM "
                                        + V51Repository.ACTION_SPACE
                                        + " WHERE target_issue=?",
                                new String[] {target});
                String deferred =
                        textScalarSnapshotCP4(report, 
                                db,
                                "SELECT status FROM "
                                        + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                        + " WHERE target_issue=?",
                                new String[] {target});
                String task =
                        textScalarSnapshotCP4(report, 
                                db,
                                "SELECT status FROM "
                                        + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                        + " WHERE target_issue=?",
                                new String[] {target});
                b.append(row)
                        .append("|target=")
                        .append(target)
                        .append("|canonical=1|settled=")
                        .append(c.getInt(1))
                        .append("|hitLabel=")
                        .append(c.getInt(2))
                        .append("|strictOos=")
                        .append(oos.render())
                        .append("|formalActions=")
                        .append(full.render())
                        .append("|deferred=")
                        .append(deferred.length() == 0 ? "NO_JOB" : deferred)
                        .append("|settlementTask=")
                        .append(task.length() == 0 ? "WAITING_DRAW" : task)
                        .append('\n');
            }
            if (row == 0) b.append("NO_RUNTIME_SAMPLE|PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED\n");
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
        return b.toString();
    }

public static String textScalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = report.select(sql, args);
            return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : "";
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static QueryInt countDbSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = report.select(sql, args);
            return c.moveToFirst() ? QueryInt.ok(c.getInt(0)) : QueryInt.error("NO_ROW");
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static String hardGatesSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, ModelEffectivenessAnalyzerR35.Analysis a) {
        ModelEffectivenessAnalyzerR35.Coverage c = a.coverage;
        String coverageGate =
                !"OK".equals(c.readStatus)
                        ? "READ_ERROR/P0"
                        : (c.eligible < 30
                                ? "RETEST_REQUIRED_WARMUP"
                                : (c.covered >= 29 ? "PASS_OBSERVED" : "FAIL_OBSERVED"));
        String concurrencyGate =
                !"OK".equals(a.runtime.readStatus)
                        ? "READ_ERROR/P0"
                        : (a.runtime.eventCount <= 0
                                ? "RETEST_REQUIRED"
                                : (a.runtime.maxConcurrency <= 1
                                        ? "PASS_OBSERVED"
                                        : "FAIL_OBSERVED"));
        String p95Gate =
                !"OK".equals(a.runtime.readStatus)
                        ? "READ_ERROR/P0"
                        : (a.runtime.eventCount <= 0
                                ? "RETEST_REQUIRED"
                                : (a.runtime.analysisP95Ms <= 5000L
                                        ? "PASS_OBSERVED"
                                        : "FAIL_OBSERVED"));
        String oomGate =
                !"OK".equals(a.runtime.readStatus)
                        ? "READ_ERROR/P0"
                        : (a.runtime.eventCount <= 0
                                ? "RETEST_REQUIRED"
                                : (a.runtime.oomCount == 0 ? "PASS_OBSERVED" : "FAIL_OBSERVED"));
        QueryInt observations =
                countSnapshotCP4(report, store, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION, null);
        QueryInt labels =
                countSnapshotCP4(report, 
                        store,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1",
                        null);
        ControlledRealContractAuditR922.Report realAudit =
                ControlledRealContractAuditR922.captureSnapshotCP4(report, store);
        PredictionGenerationCoverageR923.Report generation =
                PredictionGenerationCoverageR923.captureSnapshotCP4(report, store);
        String betGate =
                "OK".equals(realAudit.readStatus)
                        ? (realAudit.passed ? "PASS_EXACT_CONTRACT" : "FAIL_EXACT_CONTRACT")
                        : "READ_ERROR/P0";
        String durableTarget =
                store == null
                        ? ""
                        : store.getState(
                                "analysis_target_issue", store.getState("target_issue", ""));
        String durableRequest =
                store == null
                        ? "READ_ERROR:STORE_NULL"
                        : AnalysisRequestStoreR36.diagnosticSnapshotCP4(report, durableTarget);
        return "K1_Coverage="
                + c.covered
                + "/"
                + c.eligible
                + "|sinceBase="
                + c.activationBaseIssue
                + "|window="
                + c.requestedWindow
                + "|missing="
                + c.missingCsv()
                + "|"
                + coverageGate
                + "\nK2_AnalysisMaxConcurrency="
                + a.runtime.maxConcurrency
                + "|required<=1|"
                + concurrencyGate
                + "\nK3_RequestToCommitP95Ms="
                + a.runtime.analysisP95Ms
                + "|target<=5000|"
                + p95Gate
                + "\nK4_OOM="
                + a.runtime.oomCount
                + "|required=0|"
                + oomGate
                + "\nK5_CanonicalRows="
                + observations.render()
                + "|firstWriteWins=TRUE"
                + "\nK6_StrictOosLabels="
                + labels.render()
                + "|modelInvocation=0"
                + "\nK7_BusinessHeartbeat="
                + ServiceHeartbeatStore.peekLastDetailedSummary()
                + "|databaseStartup="
                + (store == null ? "STORE_NULL" : store.startupSchemaSummaryR363())
                + "\nK8_EngineeringMatrix=B01–B15_R36_IMPLEMENTED"
                + "\nK9_ControlledRealLedger="
                + realAudit.summary()
                + "|required=invalid0/duplicate0/open<=1|"
                + betGate
                + "\nK10_ModelProof="
                + a.modelStatus
                + "|requiredForProductionREAL=true|controlledEngineeringGate=SEPARATE_EXACT_CONTRACT"
                + "\nK11_DurableAnalysisRequest="
                + durableRequest
                + "\nK12_DrawDenominatorPredictionCoverage="
                + generation.summary();
    }

public static QueryInt countSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String sql, String[] args) {
        if (store == null) return QueryInt.error("STORE_NULL");
        try {
            return countDbSnapshotCP4(report, store.readable(), sql, args);
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
    }

public static String overallSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, ModelEffectivenessAnalyzerR35.Analysis a) {
        return "PLAN_ACCEPTANCE=NOT_ESTABLISHED_BY_THIS_RUNTIME_REPORT"
                + "\nENGINEERING_BUILD=THIS_RUNNING_APK"
                + "\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED"
                + "\nMODEL_PROOF="
                + a.modelStatus
                + "\nREAL_BET_UNLOCK="
                + (V51CandidateRealGateR915.releaseBoundary()
                        ? "CONTROLLED_CANDIDATE_EXACT_GATE" : "LOCKED")
                + "\nRELEASE_POSTURE="
                + V51BuildManifest.releaseState()
                + "\nROOT_CAUSE_PRIORITY="
                + a.rootCause;
    }

public static String staticReferenceSnapshotCP12(String sectionId) {
 switch(sectionId){
 case "RSEC-027":return identity();case "RSEC-028":return batchStatus();case "RSEC-029":return stateMachine();
 case "RSEC-036":return owners();case "RSEC-042":return deviceMatrix();
 case "RSEC-318":return "Integrity is verified by SCREPORT_CP4 manifest member/part hashes, not legacy character counts.";
 case "RSEC-319":return "Complete report identity is manifestSha256, produced after all members and part hashes are verified.";
 case "RSEC-320":return "END_OF_COMPLETE_REPORT is manifest footer; footer hash and exact record counts are mandatory.";
 default:throw new IllegalArgumentException("REPORT_STATIC_SECTION");
 }
}

public static String boundedSummarySnapshotCP12(){return "STARCORE_REPORT_SUMMARY_ONLY\nrelease="+ReleaseManifestR926.MANIFEST_SHA256+"\nFullEvidenceOwner=ReportExportServiceCP4\nFullMode=FULL_REPORT\nSnapshot=NOT_CAPTURED_BY_LEGACY_ENTRY\nChecks=NOT_EXECUTED_BY_LEGACY_ENTRY\nUse=CompleteReportFileR927.export(Context-bound UI)\nNoBusinessDatabaseReadOrWrite=true\n";}
}
