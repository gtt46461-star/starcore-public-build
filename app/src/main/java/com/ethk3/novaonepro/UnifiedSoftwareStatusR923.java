package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Compact index to every decision-grade runtime subsystem; raw appendices remain below it. */
public final class UnifiedSoftwareStatusR923 {
    public static final String VERSION = "R923_UNIFIED_SOFTWARE_STATUS_V1";

    private UnifiedSoftwareStatusR923() {}

    public static String capture(DbStore store) {
        if (store == null) return VERSION + "\nSTATUS=READ_ERROR|reason=STORE_NULL";
        SQLiteDatabase db;
        try {
            db = store.readable();
        } catch (Throwable failure) {
            return VERSION + "\nSTATUS=READ_ERROR|reason=" + failure.getClass().getSimpleName();
        }

        int algorithmTotal = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME);
        int algorithmActive = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                + " AND status NOT LIKE 'REVOKED%' AND status<>'PAUSED_CAPACITY_R912'");
        int algorithmArchive = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%'"
                + " OR status LIKE 'REVOKED%'");
        int algorithmPaused = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status='PAUSED_CAPACITY_R912'");
        int algorithmCertified = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status='CHALLENGER_FUTURE_OOS'");
        int algorithmPending = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " WHERE hit_label=-1");
        int algorithmSettled = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " WHERE hit_label IN(0,1)");

        int assetTotal = count(db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_GENOME);
        int assetActive = count(db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_GENOME
                + " WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                + " AND status NOT LIKE 'REVOKED%'");
        int assetArchive = count(db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_GENOME
                + " WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%'"
                + " OR status LIKE 'REVOKED%'");
        int assetCertified = count(db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE + " WHERE status='ACTIVE'");
        int assetPending = count(db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_FREEZE + " WHERE hit_label=-1");
        int assetSettled = count(db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_FREEZE + " WHERE hit_label IN(0,1)");

        int formulaTotal = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R906_RESEARCH_ASSET);
        int formulaExecutable = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R906_RESEARCH_ASSET + " WHERE runtime_executable=1");
        int formulaPending = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " WHERE hit_label=-1");
        int formulaSettled = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " WHERE hit_label IN(0,1)");
        int feedbackPending = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t WHERE t.status='"
                + SettlementTaskStoreR36.COMPLETED + "' AND NOT EXISTS(SELECT 1 FROM "
                + LegacyPredrawContextBridgeR915.POST_TABLE
                + " p WHERE p.target_issue=t.target_issue)");
        int feedbackSettled = count(db, "SELECT COUNT(1) FROM "
                + LegacyPredrawContextBridgeR915.POST_TABLE);
        int researchQueuePending = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R815_RESEARCH_QUEUE
                + " WHERE state IN('PENDING','RETRY_WAIT','RUNNING')");
        int researchQueueFailed = count(db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R815_RESEARCH_QUEUE + " WHERE state='FAILED'");

        StringBuilder b = new StringBuilder(8192);
        b.append(VERSION)
                .append("\nSTATUS=RUNTIME_OBSERVATION_NOT_MODEL_GUARANTEE")
                .append("\nIdentity=").append(V51BuildManifest.VERSION_NAME).append('/')
                .append(V51BuildManifest.VERSION_CODE).append('|')
                .append(AppConfig.R36_RUNTIME_REPAIR_VERSION).append("|DB=")
                .append(AppConfig.DB_NAME).append('/').append(AppConfig.DB_VERSION)
                .append("\n\n[后台与预测闭环]\n")
                .append(AutoFetchService.b13Diagnostics(store))
                .append("\nPredictionCoverage=")
                .append(PredictionGenerationCoverageR923.capture(store).summary())
                .append("\nThreeDayBackfill=")
                .append(state(store, "backfill_status", "WAIT_FIRST_FORCED_BACKFILL"))
                .append('|').append(state(store, "backfill_message", "NO_BACKFILL_MESSAGE"))
                .append("\nRealtimeHotPath=")
                .append(V51TianshuEvidenceBridgeR923.VERSION)
                .append("|freezeSnapshot=")
                .append(state(store, "r923_freeze_snapshot_status", "WAIT_FIRST_FULL_DIAGNOSIS"))
                .append("\nHotPathStageTrace=")
                .append(state(store, "r923_hot_path_stage_trace", "WAIT_FIRST_CANONICAL_COMMIT"))
                .append("\n\n[真实出手与连续未中]\n")
                .append(ControlledFrequencyDiagnosticR923.capture(store).summary())
                .append("\nREALContract=")
                .append(ControlledRealContractAuditR922.capture(store).summary())
                .append("\nProvenanceRepair=")
                .append(state(store, "r923_realtime_provenance_repair_summary",
                        state(store, "r923_realtime_provenance_repair_version", "NOT_RUN")))
                .append("\nTwoMissEscalation=")
                .append(state(store, "r923_miss_streak_analysis", "NO_PROVEN_REAL_SAMPLE"))
                .append("|researchStep=")
                .append(state(store, "r923_miss_streak_research_step", "IDLE"))
                .append("\n\n[深度学习/算法/原子分子资产]\n")
                .append("Atoms=").append(GenesisSchemaRegistryR915.atomCount())
                .append("|productionSemantic=")
                .append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                .append("|researchProxy=").append(GenesisSchemaRegistryR915.researchProxyCount())
                .append("|auditIdentity=").append(GenesisSchemaRegistryR915.auditIdentityCount())
                .append("|postdrawOnly=").append(GenesisSchemaRegistryR915.settlementEvidenceCount())
                .append("|families=").append(GenesisSchemaRegistryR915.familyCount())
                .append("\nAlgorithms=total:").append(render(algorithmTotal))
                .append("|active:").append(render(algorithmActive))
                .append("|archivePreserved:").append(render(algorithmArchive))
                .append("|paused:").append(render(algorithmPaused))
                .append("|futureOosCandidates:").append(render(algorithmCertified))
                .append("|predrawPending/settled:").append(render(algorithmPending)).append('/')
                .append(render(algorithmSettled))
                .append("|inventoryEquation=")
                .append(equation(algorithmTotal, algorithmActive, algorithmArchive, algorithmPaused))
                .append("\nAssets=total:").append(render(assetTotal))
                .append("|active:").append(render(assetActive))
                .append("|archivePreserved:").append(render(assetArchive))
                .append("|certified:").append(render(assetCertified))
                .append("|predrawPending/settled:").append(render(assetPending)).append('/')
                .append(render(assetSettled))
                .append("\nFormulas=total:").append(render(formulaTotal))
                .append("|executable:").append(render(formulaExecutable))
                .append("|predrawPending/settled:").append(render(formulaPending)).append('/')
                .append(render(formulaSettled))
                .append("\nLearners=FTRL:")
                .append(state(store, "r811_ftrl_updates",
                        state(store, "r817_research_adaptive_updates", "0")))
                .append("|TinyDeep:").append(state(store, "r909_tiny_deep_updates", "0"))
                .append("|continuity:")
                .append(state(store, "r912_research_continuity_status", "WAITING"))
                .append("\nRegimeEvidence=")
                .append(state(store, "r923_algorithm_regime_support_audit", "WAIT_NEXT_LIFECYCLE"))
                .append("\nEvolutionTick=")
                .append(state(store, "r908_research_tick_status", "WAITING"))
                .append("\n\n[教师/推理/反推/因果与队列]\n")
                .append("PostsettlementFeedback=rows:").append(render(feedbackSettled))
                .append("|pending:").append(render(feedbackPending))
                .append("|last:")
                .append(state(store, "r923_essential_feedback_status", "NO_SETTLEMENT"))
                .append("\nDeferred=").append(DeferredEvidenceWriterR36.backlogStats(store).summary())
                .append("\nResearchQueuePending=").append(render(researchQueuePending))
                .append("|failed=").append(render(researchQueueFailed))
                .append("\n\n").append(ResearchReadinessR924.capture(store))
                .append("\n\n").append(ResearchDashboardR924.report(store))
                .append("\n\n[F06概率与修复证据]\n").append(f06ReceiptSummary(db))
                .append("\n\n[验收边界]\n")
                .append("Code/host/build=由本次发布包证据给出；后台存活、真实P95、连续三日覆盖、"
                        + "未来OOS胜率与15%观察目标必须安装本APK后在物理设备重新采样。\n")
                .append("NoGuaranteedWin=TRUE|NoGuaranteedThirdHitAfterTwoMisses=TRUE|"
                        + "NoPauseOnTwoMisses=TRUE|NoQuotaOrFrequencyDebt=TRUE");
        return b.toString();
    }

    private static final String F06_EXACT_RECEIPT_QUERY="SELECT (SELECT COUNT(*) FROM research_exact_probability_f05), (SELECT COUNT(*) FROM v57_settlement_repair_audit_f02), (SELECT COUNT(*) FROM research_exact_probability_f05 q LEFT JOIN trades t ON t._id=q.trade_id LEFT JOIN r974_exact_real_receipt e ON e.target_issue=q.target_issue LEFT JOIN research_certificate_r929 c ON c.certificate_hash=q.certificate_hash WHERE t._id IS NULL OR t.target_issue<>q.target_issue OR e.target_issue IS NULL OR e.trade_id<>q.trade_id OR e.model_hash<>q.model_hash OR e.certificate_hash<>q.certificate_hash OR e.authorization_hash<>q.authorization_hash OR c.certificate_hash IS NULL OR c.model_hash<>q.model_hash OR q.selected_hit_p<=0 OR q.selected_hit_p>=1 OR length(q.scope_policy_hash)<>64 OR length(q.selection_hash)<>64 OR NOT EXISTS(SELECT 1 FROM research_certificate_gate_r932 g WHERE g.certificate_hash=q.certificate_hash AND g.gate_id='EXACT_SELECTION_SCOPE' AND g.pass=1 AND g.source='F05_EXACT_MODEL_SELECTION_POLICY' AND g.source_hash=q.scope_policy_hash) OR c.calibration_hash<>q.calibration_hash OR c.selection_hash<>q.selection_hash OR e.candidate_id<>q.candidate_id OR e.proof_hash<>q.proof_hash OR e.action_hash<>q.action_hash), (SELECT COUNT(*) FROM research_exact_probability_f05 q JOIN trades t ON t._id=q.trade_id WHERE t.settled=0)";
    private static String f06ReceiptSummary(SQLiteDatabase db) {
        try(Cursor row=db.rawQuery(F06_EXACT_RECEIPT_QUERY,null)){return f06ReceiptSummaryRow(row);}
        catch(RuntimeException error){return "F06_PROOF_READ_ERROR:"+error.getClass().getSimpleName();}
    }
    private static String f06ReceiptSummarySnapshot(ReportReadContextCP4 ctx) {
        try(Cursor row=ctx.select(F06_EXACT_RECEIPT_QUERY,null)){return f06ReceiptSummaryRow(row);}
    }
    private static String f06ReceiptSummaryRow(Cursor row) {
        if(!row.moveToFirst()||row.getColumnCount()!=4)throw new IllegalStateException("F06_RECEIPT_SUMMARY_SHAPE");
        return "exactProbabilityReceipts="+row.getLong(0)+"|settlementRepairAudits="+row.getLong(1)
                +"|orphanOrInvalidReceipts="+row.getLong(2)+"|inflightReceipts="+row.getLong(3)
                +"|proofHealth="+(row.getLong(2)==0?"OBSERVED_NOT_MODEL_PASS":"FAIL_RECEIPT_ORPHAN")
                +"|rawEvidence=snapshot.db|archiveRefs=research_evidence_archive_v2/research_evidence_archive_member_v2";
    }

    private static int count(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (Throwable failure) {
            return -1;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String render(int value) {
        return value < 0 ? "READ_ERROR" : String.valueOf(value);
    }

    private static String equation(int total, int active, int archive, int paused) {
        if (total < 0 || active < 0 || archive < 0 || paused < 0) return "READ_ERROR";
        return total == active + archive + paused ? "PASS" : "FAIL_UNCLASSIFIED_"
                + (total - active - archive - paused);
    }

    private static String state(DbStore store, String key, String fallback) {
        try {
            String value = store.getState(key, "");
            return value == null || value.trim().length() == 0 ? fallback : value.trim();
        } catch (Throwable failure) {
            return "READ_ERROR:" + failure.getClass().getSimpleName();
        }
    }

public static String captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return VERSION + "\nSTATUS=READ_ERROR|reason=STORE_NULL";
        SQLiteDatabase db;
        try {
            db = store.readable();
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}

        int algorithmTotal = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME);
        int algorithmActive = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                + " AND status NOT LIKE 'REVOKED%' AND status<>'PAUSED_CAPACITY_R912'");
        int algorithmArchive = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%'"
                + " OR status LIKE 'REVOKED%'");
        int algorithmPaused = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status='PAUSED_CAPACITY_R912'");
        int algorithmCertified = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_GENOME
                + " WHERE status='CHALLENGER_FUTURE_OOS'");
        int algorithmPending = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " WHERE hit_label=-1");
        int algorithmSettled = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " WHERE hit_label IN(0,1)");

        int assetTotal = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_GENOME);
        int assetActive = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_GENOME
                + " WHERE status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REJECTED%'"
                + " AND status NOT LIKE 'REVOKED%'");
        int assetArchive = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_GENOME
                + " WHERE status LIKE 'PRUNED%' OR status LIKE 'REJECTED%'"
                + " OR status LIKE 'REVOKED%'");
        int assetCertified = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE + " WHERE status='ACTIVE'");
        int assetPending = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_FREEZE + " WHERE hit_label=-1");
        int assetSettled = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + AssetGenesisEngineR915.TABLE_ASSET_FREEZE + " WHERE hit_label IN(0,1)");

        int formulaTotal = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R906_RESEARCH_ASSET);
        int formulaExecutable = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R906_RESEARCH_ASSET + " WHERE runtime_executable=1");
        int formulaPending = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " WHERE hit_label=-1");
        int formulaSettled = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " WHERE hit_label IN(0,1)");
        int feedbackPending = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t WHERE t.status='"
                + SettlementTaskStoreR36.COMPLETED + "' AND NOT EXISTS(SELECT 1 FROM "
                + LegacyPredrawContextBridgeR915.POST_TABLE
                + " p WHERE p.target_issue=t.target_issue)");
        int feedbackSettled = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + LegacyPredrawContextBridgeR915.POST_TABLE);
        int researchQueuePending = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R815_RESEARCH_QUEUE
                + " WHERE state IN('PENDING','RETRY_WAIT','RUNNING')");
        int researchQueueFailed = countSnapshotCP4(report, db, "SELECT COUNT(1) FROM "
                + DbHelper.TABLE_R815_RESEARCH_QUEUE + " WHERE state='FAILED'");

        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 8192);
        b.append(VERSION)
                .append("\nSTATUS=RUNTIME_OBSERVATION_NOT_MODEL_GUARANTEE")
                .append("\nIdentity=").append(V51BuildManifest.VERSION_NAME).append('/')
                .append(V51BuildManifest.VERSION_CODE).append('|')
                .append(AppConfig.R36_RUNTIME_REPAIR_VERSION).append("|DB=")
                .append(AppConfig.DB_NAME).append('/').append(AppConfig.DB_VERSION)
                .append("\n\n[后台与预测闭环]\n")
                .append(AutoFetchService.b13DiagnosticsSnapshotCP4(report, store))
                .append("\nPredictionCoverage=")
                .append(PredictionGenerationCoverageR923.captureSnapshotCP4(report, store).summary())
                .append("\nThreeDayBackfill=")
                .append(stateSnapshotCP4(report, store, "backfill_status", "WAIT_FIRST_FORCED_BACKFILL"))
                .append('|').append(stateSnapshotCP4(report, store, "backfill_message", "NO_BACKFILL_MESSAGE"))
                .append("\nRealtimeHotPath=")
                .append(V51TianshuEvidenceBridgeR923.VERSION)
                .append("|freezeSnapshot=")
                .append(stateSnapshotCP4(report, store, "r923_freeze_snapshot_status", "WAIT_FIRST_FULL_DIAGNOSIS"))
                .append("\nHotPathStageTrace=")
                .append(stateSnapshotCP4(report, store, "r923_hot_path_stage_trace", "WAIT_FIRST_CANONICAL_COMMIT"))
                .append("\n\n[真实出手与连续未中]\n")
                .append(ControlledFrequencyDiagnosticR923.captureSnapshotCP4(report, store).summary())
                .append("\nREALContract=")
                .append(ControlledRealContractAuditR922.captureSnapshotCP4(report, store).summary())
                .append("\nProvenanceRepair=")
                .append(stateSnapshotCP4(report, store, "r923_realtime_provenance_repair_summary",
                        stateSnapshotCP4(report, store, "r923_realtime_provenance_repair_version", "NOT_RUN")))
                .append("\nTwoMissEscalation=")
                .append(stateSnapshotCP4(report, store, "r923_miss_streak_analysis", "NO_PROVEN_REAL_SAMPLE"))
                .append("|researchStep=")
                .append(stateSnapshotCP4(report, store, "r923_miss_streak_research_step", "IDLE"))
                .append("\n\n[深度学习/算法/原子分子资产]\n")
                .append("Atoms=").append(GenesisSchemaRegistryR915.atomCount())
                .append("|productionSemantic=")
                .append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                .append("|researchProxy=").append(GenesisSchemaRegistryR915.researchProxyCount())
                .append("|auditIdentity=").append(GenesisSchemaRegistryR915.auditIdentityCount())
                .append("|postdrawOnly=").append(GenesisSchemaRegistryR915.settlementEvidenceCount())
                .append("|families=").append(GenesisSchemaRegistryR915.familyCount())
                .append("\nAlgorithms=total:").append(renderSnapshotCP4(report, algorithmTotal))
                .append("|active:").append(renderSnapshotCP4(report, algorithmActive))
                .append("|archivePreserved:").append(renderSnapshotCP4(report, algorithmArchive))
                .append("|paused:").append(renderSnapshotCP4(report, algorithmPaused))
                .append("|futureOosCandidates:").append(renderSnapshotCP4(report, algorithmCertified))
                .append("|predrawPending/settled:").append(renderSnapshotCP4(report, algorithmPending)).append('/')
                .append(renderSnapshotCP4(report, algorithmSettled))
                .append("|inventoryEquation=")
                .append(equationSnapshotCP4(report, algorithmTotal, algorithmActive, algorithmArchive, algorithmPaused))
                .append("\nAssets=total:").append(renderSnapshotCP4(report, assetTotal))
                .append("|active:").append(renderSnapshotCP4(report, assetActive))
                .append("|archivePreserved:").append(renderSnapshotCP4(report, assetArchive))
                .append("|certified:").append(renderSnapshotCP4(report, assetCertified))
                .append("|predrawPending/settled:").append(renderSnapshotCP4(report, assetPending)).append('/')
                .append(renderSnapshotCP4(report, assetSettled))
                .append("\nFormulas=total:").append(renderSnapshotCP4(report, formulaTotal))
                .append("|executable:").append(renderSnapshotCP4(report, formulaExecutable))
                .append("|predrawPending/settled:").append(renderSnapshotCP4(report, formulaPending)).append('/')
                .append(renderSnapshotCP4(report, formulaSettled))
                .append("\nLearners=FTRL:")
                .append(stateSnapshotCP4(report, store, "r811_ftrl_updates",
                        stateSnapshotCP4(report, store, "r817_research_adaptive_updates", "0")))
                .append("|TinyDeep:").append(stateSnapshotCP4(report, store, "r909_tiny_deep_updates", "0"))
                .append("|continuity:")
                .append(stateSnapshotCP4(report, store, "r912_research_continuity_status", "WAITING"))
                .append("\nRegimeEvidence=")
                .append(stateSnapshotCP4(report, store, "r923_algorithm_regime_support_audit", "WAIT_NEXT_LIFECYCLE"))
                .append("\nEvolutionTick=")
                .append(stateSnapshotCP4(report, store, "r908_research_tick_status", "WAITING"))
                .append("\n\n[教师/推理/反推/因果与队列]\n")
                .append("PostsettlementFeedback=rows:").append(renderSnapshotCP4(report, feedbackSettled))
                .append("|pending:").append(renderSnapshotCP4(report, feedbackPending))
                .append("|last:")
                .append(stateSnapshotCP4(report, store, "r923_essential_feedback_status", "NO_SETTLEMENT"))
                .append("\nDeferred=").append(DeferredEvidenceWriterR36.backlogStatsSnapshotCP4(report, store).summary())
                .append("\nResearchQueuePending=").append(renderSnapshotCP4(report, researchQueuePending))
                .append("|failed=").append(renderSnapshotCP4(report, researchQueueFailed))
                .append("\n\n").append(ResearchReadinessR924.captureSnapshotCP4(report, store))
                .append("\n\n").append(ResearchDashboardR924.reportSnapshotCP4(report, store))
                .append("\n\n[F06概率与修复证据]\n").append(f06ReceiptSummarySnapshot(report))
                .append("\n\n[验收边界]\n")
                .append("Code/host/build=由本次发布包证据给出；后台存活、真实P95、连续三日覆盖、"
                        + "未来OOS胜率与15%观察目标必须安装本APK后在物理设备重新采样。\n")
                .append("NoGuaranteedWin=TRUE|NoGuaranteedThirdHitAfterTwoMisses=TRUE|"
                        + "NoPauseOnTwoMisses=TRUE|NoQuotaOrFrequencyDebt=TRUE");
        return b.toString();
    }

public static String renderSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int value) {
        return value < 0 ? "READ_ERROR" : String.valueOf(value);
    }

public static String stateSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String key, String fallback) {
        try {
            String value = store.getState(key, "");
            return value == null || value.trim().length() == 0 ? fallback : value.trim();
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
    }

public static String equationSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int total, int active, int archive, int paused) {
        if (total < 0 || active < 0 || archive < 0 || paused < 0) return "READ_ERROR";
        return total == active + archive + paused ? "PASS" : "FAIL_UNCLASSIFIED_"
                + (total - active - archive - paused);
    }

public static int countSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = report.select(sql, null);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);} finally {
            if (c != null) c.close();
        }
    }
}
