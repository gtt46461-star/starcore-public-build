package com.ethk3.novaonepro;

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * RealBetDiagnosisEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环升级 · 最终真实BET诊断闭环修复版（第6/6）
 *
 * <p>只负责诊断，不参与出号、不参与结算、不创建BET、不修改概率、不修改门槛、 不写真实盈亏、不绕过 DecisionGateEngine。
 *
 * <p>目标： 1. 复盘最近真实 BET 的胜率、盈亏、位置、FinalP、Gate、Edge、Teacher、V20因链、WindowState、错因。 2. 复盘最近 SHADOW
 * 的反事实命中率，判断是否错杀强单。 3. 复盘门槛反事实，判断 FinalP/Gate/Edge 哪个区间才是真的有效。 4. 输出逐单 CaseDiagnosis /
 * ShadowCaseDiagnosis / ModuleDiagnosis，给后续修复计划、模块评分、冻结矩阵、智能审判读取。 5.
 * 严格只读诊断：不创建Trade、不结算、不改Decision、不写真实盈亏、不影响BET。 6. 第6/6彻底修复：识别Teacher
 * WEAK_NO_BET、P40假高P、Edge虚强、K3覆盖风险、第三号弱、最终RejectCode。 7.
 * 第6/6彻底修复：真实BET、SHADOW反事实、失败类型、模块责任、修复建议全部按第2-5文件新字段重算。
 *
 * <p>Java7 / AIDE 兼容：无 lambda、无 stream、无第三方库。
 */
public final class RealBetDiagnosisEngine {
    public static final String FREQUENCY_FLOOR_DIAG_VERSION = AppConfig.FREQUENCY_FLOOR_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String AIDE_SCOPE_FIX_PATCH = V48BuildManifest.DIAGNOSIS_VERSION;
    public static final String VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String MAIN_CHAIN_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String CLOSED_LOOP_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String READ_ONLY_SPLIT_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String STATUS_NO_SAMPLE_NEUTRAL = "NO_SAMPLE_NEUTRAL";
    public static final String STATUS_WATCH_NEUTRAL = "WATCH_NEUTRAL";
    public static final String LINK_V2_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    public static final String V50_DIAGNOSIS_VERSION =
            "V50_PRIME_TYPED_READ_ONLY_DIAGNOSIS_50072403";
    private static final int DEFAULT_BET_LIMIT = 100;
    private static final int DEFAULT_SHADOW_LIMIT = 720;
    private static final int DEFAULT_DECISION_SCAN_LIMIT = 1200;
    private static final int MAX_LINE_LEN = 220;
    private static final int MAX_REPORT_LEN = 240000;
    private static final int POSITION_COUNT = 10;
    private static final String BUCKET_EMPTY = "空";
    private static final double FIX_EDGE_FALSE_STRONG_LINE = 0.020d;
    private static final double FIX_PREFERRED_P_MIN = 0.380d;
    private static final double FIX_PREFERRED_P_MAX = 0.400d;
    private static final double FIX_PREFERRED_EDGE_MIN = 0.010d;
    private static final double FIX_PREFERRED_EDGE_MAX = 0.020d;
    private static final double FIX_K3_COVERAGE_RISK_LINE = 0.620d;
    private static final double FIX_K3_COVERAGE_HARD_RISK = 0.700d;
    private static final double FIX_THIRD_NUMBER_MIN = 0.088d;
    private static final double FIX_COVERAGE_QUALITY_MIN = 0.330d;
    private static final double FIX_P40_FALSE_HIGH_LINE = 0.400d;
    private static final String FIELD_CHAIN_EXPECT_AUTOFETCH_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String FIELD_CHAIN_EXPECT_DBSTORE_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String FIELD_CHAIN_EXPECT_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String DYNAMIC_CAUSAL_EXPECT_DECISION_GATE_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final String DYNAMIC_CAUSAL_EXPECT_AUTOFETCH_VERSION =
            "V47_CROSSFIT_NUMBER_CHAMPION_FINAL40_FULL_DIAGNOSIS_20260721_47072147";
    private static final double DYNAMIC_CAUSAL_RANDOM_BASELINE = 0.300d;
    private static final double DYNAMIC_CAUSAL_READY_RATE = 0.365d;
    private static final double DYNAMIC_CAUSAL_MIN_NEED_SUPPLY = 0.500d;
    private static final double DYNAMIC_CAUSAL_MIN_THIRD_QUALITY = 0.100d;
    private static final double DYNAMIC_CAUSAL_MAX_RISK = 0.620d;
    private static final double DYNAMIC_CAUSAL_HARD_RISK = 0.720d;
    private static final int DYNAMIC_CAUSAL_MIN_CLUSTER_SAMPLE = 20;
    private static final int DYNAMIC_CAUSAL_MAX_LOSE = 5;

    public RealBetDiagnosisEngine() {}

    public DiagnosisReport diagnose(DbStore store) {
        return diagnose(store, DEFAULT_BET_LIMIT, DEFAULT_SHADOW_LIMIT);
    }

    public DiagnosisReport diagnose(DbStore store, int betLimit, int shadowLimit) {
        DiagnosisReport report = new DiagnosisReport();
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "RealBetDiagnosisEngine",
                        AppConfig.ACTIVE_QUALITY_BUILD_VERSION,
                        "20-file diagnosis funnel");
        } catch (Throwable ignore) {
        }
        report.version = LINK_V2_VERSION;
        report.createdAt = System.currentTimeMillis();
        if (betLimit <= 0) {
            betLimit = DEFAULT_BET_LIMIT;
        }
        if (shadowLimit <= 0) {
            shadowLimit = DEFAULT_SHADOW_LIMIT;
        }
        if (store == null) {
            report.ok = 0;
            report.summary = "诊断失败：DbStore为空";
            report.detail = report.summary;
            return report;
        }
        if (AppConfig.ENABLE_V50_PRIME) {
            return diagnoseV50(store, betLimit, shadowLimit);
        }
        ArrayList<BetCase> betCases = loadRecentRealBetCases(store, betLimit);
        ArrayList<BetCase> shadowCases = loadRecentShadowCases(store, shadowLimit);
        report.ok = 1;
        report.betLimit = betLimit;
        report.shadowLimit = shadowLimit;
        report.betCases = betCases;
        report.shadowCases = shadowCases;
        loadFieldChainVerifyState(store, report);
        analyzeBetCases(report, betCases);
        analyzeShadowCases(report, shadowCases);
        analyzeDynamicCausalCases(report, betCases, shadowCases);
        analyzeMainChainNeutralCases(report, betCases, shadowCases);
        loadLinkModificationV2State(store, report);
        loadBackgroundServiceEvidenceState(store, report);
        analyzeRealtimeValidityFixState(store, report);
        loadV23EvidenceState(store, report);
        report.v28OosReliabilityAudit = OosReliabilityAuditEngine.buildAndPersist(store);
        report.v44DecisionFunnel = V48DecisionFunnelDiagnosisEngine.buildAndPersist(store);
        report.v42RiskCoverage = getStateSafe(store, "v42_risk_coverage_summary", "--");
        report.v42LanePolicy =
                getStateSafe(
                        store,
                        "v42_oos_last_eligible",
                        getStateSafe(store, "v42_authorization_last_summary", "--"));
        report.v42OpportunityFunnel = getStateSafe(store, "v42_opportunity_funnel_summary", "--");
        report.v42ActionUtility = getStateSafe(store, "v42_action_utility_last", "--");
        applyR811ReleaseTruth(store, report);
        buildDiagnosisText(report);
        return report;
    }

    private DiagnosisReport diagnoseV50(DbStore store, int betLimit, int shadowLimit) {
        DiagnosisReport report = new DiagnosisReport();
        report.version = V50_DIAGNOSIS_VERSION;
        report.createdAt = System.currentTimeMillis();
        report.betLimit = betLimit <= 0 ? DEFAULT_BET_LIMIT : betLimit;
        report.shadowLimit = shadowLimit <= 0 ? DEFAULT_SHADOW_LIMIT : shadowLimit;
        try {
            V50CohortDiagnosisEngine.Report cohort = new V50CohortDiagnosisEngine().diagnose(store);
            V50LedgerDiagnosisEngine.Report ledger =
                    new V50LedgerDiagnosisEngine().diagnose(store, 100);
            V50ModelHealthDiagnosisEngine.Report model =
                    new V50ModelHealthDiagnosisEngine().diagnose(store);
            String performance = new V50PerformanceDiagnosisEngine().diagnose(store, 500);
            Models.Decision latest = store.getLatestDecision();
            boolean latestIsV50 =
                    latest != null && V50BuildManifest.COHORT_ID.equals(safe(latest.cohortId));
            boolean hasK3 =
                    latestIsV50
                            && AppConfig.isValidPosition(latest.position)
                            && AppConfig.isValidK3Numbers(latest.n1, latest.n2, latest.n3);

            report.ok = 1;
            report.totalBet = cohort.realBets;
            report.hitBet = cohort.hits;
            report.missBet = Math.max(0, cohort.realBets - cohort.hits);
            report.winRate = cohort.hitRate;
            report.pnl = cohort.pnl;
            report.shadowTotal = cohort.shadows;
            report.shadowHitRate = 0.0d;
            report.performanceSummary = performance;
            applyR811ReleaseTruth(store, report);
            report.mainProblem =
                    hasK3
                            ? (V50SelectivePolicyArtifact.MODEL_GATE_PASSED
                                    ? "V50动作已生成；等待独立实时门验收"
                                    : "V50动作已生成，但严格OOS模型门未通过，真实BET保持关闭")
                            : "当前目标期尚无V50位置+K3动作；检查历史引导与实时运行状态";
            report.nextFixAdvice =
                    hasK3
                            ? "继续结算纯V50 SHADOW并保持1/10/100账本完整；不得靠降门槛补频率"
                            : "先完成历史引导；12至95期必须冷启动出SHADOW，96期以上进入完整PIT运行";

            StringBuilder summary = new StringBuilder();
            summary.append("V50纯链｜目标期=")
                    .append(
                            latestIsV50
                                    ? safe(latest.targetIssue)
                                    : getStateSafe(store, "target_issue", "--"))
                    .append("｜动作=")
                    .append(latestIsV50 ? safe(latest.action) : "PENDING")
                    .append("｜位置/K3=");
            if (hasK3) {
                summary.append('P')
                        .append(latest.position)
                        .append(' ')
                        .append(latest.n1)
                        .append(',')
                        .append(latest.n2)
                        .append(',')
                        .append(latest.n3);
            } else {
                summary.append("--");
            }
            summary.append("｜SelectedHitP=")
                    .append(hasK3 ? formatPctV20(latest.finalP) : "--")
                    .append("｜历史=")
                    .append(getStateSafe(store, "v50_runtime_history_rows", "0"))
                    .append('/')
                    .append(getStateSafe(store, "v50_runtime_history_mode", "--"))
                    .append("｜账本=")
                    .append(ledger.valid ? "OK" : "ERROR")
                    .append("｜发布=")
                    .append(V50BuildManifest.RELEASE_STATE);
            report.summary = summary.toString();

            StringBuilder detail = new StringBuilder();
            detail.append("【V50 PRIME 当前动作】\n")
                    .append("版本=")
                    .append(V50BuildManifest.STRATEGY_VERSION)
                    .append("\nBuild/Cohort=")
                    .append(V50BuildManifest.BUILD_ID)
                    .append('/')
                    .append(V50BuildManifest.COHORT_ID)
                    .append("\n基础期/目标期=")
                    .append(latestIsV50 ? safe(latest.baseIssue) : "--")
                    .append('/')
                    .append(latestIsV50 ? safe(latest.targetIssue) : "--")
                    .append("\n动作=")
                    .append(latestIsV50 ? safe(latest.action) : "PENDING")
                    .append("｜位置=")
                    .append(hasK3 ? latest.position : 0)
                    .append("｜号码=")
                    .append(hasK3 ? latest.n1 + "," + latest.n2 + "," + latest.n3 : "--")
                    .append("\nRawActionP/RankAdjustedP/SelectedHitP=")
                    .append(hasK3 ? formatPctV20(latest.rawP) : "--")
                    .append('/')
                    .append(hasK3 ? formatPctV20(latest.modelP) : "--")
                    .append('/')
                    .append(hasK3 ? formatPctV20(latest.finalP) : "--")
                    .append("\nLane/Reject=")
                    .append(latestIsV50 ? safe(latest.laneCandidate) : "--")
                    .append('/')
                    .append(latestIsV50 ? safe(latest.finalDecisionRejectCode) : "--")
                    .append("\n历史行/模式=")
                    .append(getStateSafe(store, "v50_runtime_history_rows", "0"))
                    .append('/')
                    .append(getStateSafe(store, "v50_runtime_history_mode", "--"))
                    .append("\n历史引导=")
                    .append(
                            getStateSafe(
                                    store,
                                    "v50_history_bootstrap_status",
                                    getStateSafe(store, "v50_history_seed_import_summary", "--")))
                    .append("\n运行状态=")
                    .append(getStateSafe(store, "v50_runtime_status", "--"))
                    .append("\n\n【V50 1/10/100账本】\n")
                    .append(ledger.detail)
                    .append("\n\n【纯V50 Cohort】\n")
                    .append(cohort.detail)
                    .append("\n\n【V50模型健康】\n")
                    .append(model.detail)
                    .append("\n\n【V50性能】\n")
                    .append(performance)
                    .append("\n\n【发布结论】\n")
                    .append("工程状态=")
                    .append(V50BuildManifest.RELEASE_STATE)
                    .append("｜严格OOS模型门=")
                    .append(V50SelectivePolicyArtifact.MODEL_GATE_PASSED ? "PASS" : "FAIL")
                    .append("｜真实BET兼容=")
                    .append(V50BuildManifest.mayLabelFullRelease() ? "1" : "0")
                    .append("\n")
                    .append(report.mainProblem)
                    .append("\n下一步=")
                    .append(report.nextFixAdvice)
                    .append("\n红线：本诊断只读；唯一真实BET出口仍是DecisionGateEngine。");
            report.detail = limit(detail.toString(), MAX_REPORT_LEN);
            report.caseReplayText = report.detail;
            report.moduleResponsibilityText = model.detail;
            return report;
        } catch (Throwable error) {
            report.ok = 0;
            report.mainProblem = "V50诊断读取失败";
            report.nextFixAdvice = "先检查V50数据库迁移和Schema合同";
            report.summary =
                    "V50诊断失败｜" + error.getClass().getSimpleName() + ':' + safe(error.getMessage());
            report.detail =
                    report.summary
                            + "\n运行状态="
                            + getStateSafe(store, "v50_runtime_status", "--")
                            + "\n历史引导="
                            + getStateSafe(store, "v50_history_bootstrap_status", "--");
            return report;
        }
    }

    public String buildDiagnosisSummary(DbStore store) {
        DiagnosisReport r = diagnose(store);
        return r == null ? "" : safe(r.summary);
    }

    public String buildDiagnosisDetail(DbStore store) {
        DiagnosisReport r = diagnose(store);
        return r == null ? "" : safe(r.detail);
    }

    public String buildDiagnosisCopyText(DbStore store) {
        DiagnosisReport r = diagnose(store);
        if (r == null) {
            return "V20真实BET诊断为空";
        }
        return safe(r.detail);
    }

    public String buildCaseReplayText(DbStore store) {
        DiagnosisReport r = diagnose(store);
        if (r == null) {
            return "V20逐单复盘为空";
        }
        if (V50_DIAGNOSIS_VERSION.equals(r.version)) {
            return safe(r.detail);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【真实BET逐单复盘】\n");
        appendCaseDiagnosisList(sb, r.caseDiagnoses, 100);
        sb.append("\n【SHADOW反事实逐单复盘】\n");
        appendShadowDiagnosisList(sb, r.shadowCaseDiagnoses, 120);
        return limit(sb.toString(), MAX_REPORT_LEN);
    }

    public String buildModuleDiagnosisText(DbStore store) {
        DiagnosisReport r = diagnose(store);
        if (r == null) {
            return "V20模块归因为空";
        }
        if (V50_DIAGNOSIS_VERSION.equals(r.version)) {
            return safe(r.moduleResponsibilityText);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【模块初步责任诊断】\n");
        appendModuleDiagnosisList(sb, r.moduleDiagnoses, 30);
        return limit(sb.toString(), MAX_REPORT_LEN);
    }

    public DiagnosisReport saveDiagnosisState(DbStore store) {
        DiagnosisReport r = diagnose(store);
        saveDiagnosisState(store, r);
        return r;
    }

    public void saveDiagnosisState(DbStore store, DiagnosisReport report) {
        if (store == null || report == null) {
            return;
        }
        if (V50_DIAGNOSIS_VERSION.equals(report.version)) {
            saveV50DiagnosisState(store, report);
            return;
        }
        trySetState(store, "real_bet_diagnosis_version", safe(report.version));
        trySetState(store, "real_bet_diagnosis_updated_at", String.valueOf(report.createdAt));
        trySetState(store, "real_bet_diagnosis_status", report.ok == 1 ? "OK" : "ERROR");
        trySetState(store, "real_bet_diagnosis_summary", safe(report.summary));
        trySetState(store, "real_bet_diagnosis_detail", safe(report.detail));
        trySetState(store, "real_bet_diagnosis_total", String.valueOf(report.totalBet));
        trySetState(store, "real_bet_diagnosis_hit", String.valueOf(report.hitBet));
        trySetState(store, "real_bet_diagnosis_miss", String.valueOf(report.missBet));
        trySetState(store, "real_bet_diagnosis_win_rate", String.valueOf(report.winRate));
        trySetState(store, "real_bet_diagnosis_pnl", String.valueOf(report.pnl));
        trySetState(store, "real_bet_diagnosis_shadow_total", String.valueOf(report.shadowTotal));
        trySetState(store, "real_bet_diagnosis_shadow_hit", String.valueOf(report.shadowHit));
        trySetState(store, "real_bet_diagnosis_shadow_rate", String.valueOf(report.shadowHitRate));
        trySetState(store, "real_bet_diagnosis_main_problem", safe(report.mainProblem));
        trySetState(store, "real_bet_diagnosis_next_fix", safe(report.nextFixAdvice));
        trySetState(
                store,
                "real_bet_diagnosis_case_count",
                String.valueOf(report.caseDiagnoses == null ? 0 : report.caseDiagnoses.size()));
        trySetState(
                store,
                "real_bet_diagnosis_shadow_case_count",
                String.valueOf(
                        report.shadowCaseDiagnoses == null
                                ? 0
                                : report.shadowCaseDiagnoses.size()));
        trySetState(
                store,
                "real_bet_diagnosis_module_count",
                String.valueOf(report.moduleDiagnoses == null ? 0 : report.moduleDiagnoses.size()));
        trySetState(store, "real_bet_diagnosis_case_replay", safe(report.caseReplayText));
        trySetState(
                store,
                "real_bet_diagnosis_module_responsibility",
                safe(report.moduleResponsibilityText));
        trySetState(store, "real_bet_diagnosis_final_bet_fix_version", VERSION);
        trySetState(
                store,
                "real_bet_diagnosis_teacher_weak_no_bet_count",
                String.valueOf(report.teacherWeakNoBetStillBet));
        trySetState(
                store,
                "real_bet_diagnosis_false_high_p_count",
                String.valueOf(report.falseHighPStillBet));
        trySetState(
                store,
                "real_bet_diagnosis_k3_coverage_risk_count",
                String.valueOf(report.k3CoverageRiskStillBet));
        trySetState(
                store,
                "real_bet_diagnosis_edge_false_strong_count",
                String.valueOf(report.edgeFalseStrongStillBet));
        trySetState(
                store,
                "real_bet_diagnosis_preferred_domain_bet_count",
                String.valueOf(report.preferredDomainBet));
        trySetState(
                store,
                "real_bet_diagnosis_preferred_domain_hit_count",
                String.valueOf(report.preferredDomainHit));
        trySetState(store, "real_bet_diagnosis_new_field_summary", safe(report.finalBetFixSummary));
        trySetState(store, "real_bet_diagnosis_v44_funnel", safe(report.v44DecisionFunnel));
        trySetState(store, "real_bet_diagnosis_v24_probability", safe(report.v24Probability));
        trySetState(
                store,
                "real_bet_diagnosis_v24_probability_policy",
                safe(report.v24ProbabilityPolicy));
        trySetState(
                store,
                "real_bet_diagnosis_v24_micro_walkforward",
                safe(report.v24MicroWalkForward));
        trySetState(store, "real_bet_diagnosis_v24_authorization", safe(report.v24Authorization));
        trySetState(store, "real_bet_diagnosis_v24_oos_state", safe(report.v24OosState));
        trySetState(
                store,
                "real_bet_diagnosis_v24_unauthorized_bet",
                String.valueOf(report.v24UnauthorizedBetCount));
        trySetState(
                store, "real_bet_diagnosis_v24_watch_bet", String.valueOf(report.v24WatchBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_v24_hard_risk_bet",
                String.valueOf(report.v24HardRiskBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_v24_version_mismatch_bet",
                String.valueOf(report.v24VersionMismatchBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_field_chain_summary",
                safe(report.fieldChainVerifySummary));
        trySetState(
                store,
                "real_bet_diagnosis_field_chain_problem",
                safe(report.fieldChainMainProblem));
        trySetState(store, "real_bet_diagnosis_field_chain_version", VERSION);
        trySetState(
                store,
                "real_bet_diagnosis_weak_v20_empty_bet",
                String.valueOf(report.fieldChainWeakV20EmptyStillBet));
        trySetState(
                store,
                "real_bet_diagnosis_cover_empty_bet",
                String.valueOf(report.fieldChainCoverEmptyStillBet));
        trySetState(
                store,
                "real_bet_diagnosis_edge_false_strong_phenomenon",
                String.valueOf(report.fieldChainEdgeFalseStrongByPhenomenon));
        trySetState(
                store,
                "real_bet_diagnosis_reject_missing_bet",
                String.valueOf(report.fieldChainRejectMissingBet));
        trySetState(
                store,
                "real_bet_diagnosis_version_missing_bet",
                String.valueOf(report.fieldChainVersionMissingBet));
        trySetState(store, "real_bet_dynamic_causal_version", VERSION);
        trySetState(store, "real_bet_dynamic_causal_summary", safe(report.dynamicCausalSummary));
        trySetState(
                store, "real_bet_dynamic_causal_problem", safe(report.dynamicCausalMainProblem));
        trySetState(store, "real_bet_dynamic_causal_next_fix", safe(report.dynamicCausalNextFix));
        trySetState(
                store, "real_bet_dynamic_cluster_bad", String.valueOf(report.dynamicClusterBadBet));
        trySetState(store, "real_bet_need_supply_bad", String.valueOf(report.needSupplyBadBet));
        trySetState(
                store, "real_bet_counterfactual_bad", String.valueOf(report.counterfactualBadBet));
        trySetState(
                store,
                "real_bet_adaptive_freeze_bad",
                String.valueOf(report.adaptiveFreezeStillBet));
        trySetState(
                store,
                "real_bet_dynamic_causal_ready_bet",
                String.valueOf(report.dynamicCausalReadyBet));
        trySetState(
                store,
                "real_bet_dynamic_causal_ready_hit",
                String.valueOf(report.dynamicCausalReadyHit));
        trySetState(
                store,
                "real_bet_dynamic_causal_shadow_ready",
                String.valueOf(report.shadowDynamicCausalReady));
        trySetState(
                store,
                "real_bet_dynamic_causal_shadow_ready_hit",
                String.valueOf(report.shadowDynamicCausalReadyHit));
        trySetState(store, "real_bet_dynamic_causal_detail", safe(report.dynamicCausalDetailText));
        trySetState(store, "real_bet_diagnosis_main_chain_version", MAIN_CHAIN_VERSION);
        trySetState(store, "real_bet_diagnosis_closed_loop_version", CLOSED_LOOP_VERSION);
        trySetState(store, "real_bet_diagnosis_read_only_split_version", READ_ONLY_SPLIT_VERSION);
        trySetState(
                store,
                "real_bet_diagnosis_real_scope_bet_count",
                String.valueOf(report.realScopeBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_shadow_scope_count",
                String.valueOf(report.shadowScopeCount));
        trySetState(
                store,
                "real_bet_diagnosis_hold_scope_count",
                String.valueOf(report.holdScopeCount));
        trySetState(
                store,
                "real_bet_diagnosis_no_sample_neutral_count",
                String.valueOf(report.noSampleNeutralCount));
        trySetState(
                store,
                "real_bet_diagnosis_v20_watch_neutral_count",
                String.valueOf(report.v20WatchNeutralCount));
        trySetState(
                store,
                "real_bet_diagnosis_adaptive_watch_neutral_count",
                String.valueOf(report.adaptiveWatchNeutralCount));
        trySetState(
                store,
                "real_bet_diagnosis_main_chain_field_count",
                String.valueOf(report.mainChainFieldCaseCount));
        trySetState(
                store,
                "real_bet_diagnosis_main_chain_reject_count",
                String.valueOf(report.mainChainRejectCodeCount));
        trySetState(store, "real_bet_diagnosis_main_chain_summary", safe(report.mainChainSummary));
        trySetState(
                store,
                "real_bet_diagnosis_real_shadow_split_summary",
                safe(report.realShadowSplitSummary));
        trySetState(store, "real_bet_diagnosis_link_v2_version", LINK_V2_VERSION);
        trySetState(store, "real_bet_diagnosis_freq50", String.valueOf(report.realFrequency50));
        trySetState(store, "real_bet_diagnosis_freq100", String.valueOf(report.realFrequency100));
        trySetState(store, "real_bet_diagnosis_freq300", String.valueOf(report.realFrequency300));
        trySetState(
                store, "real_bet_diagnosis_frequency_debt", String.valueOf(report.frequencyDebt));
        trySetState(store, "real_bet_diagnosis_frequency_state", safe(report.frequencyState));
        trySetState(
                store,
                "real_bet_diagnosis_authorization_state",
                safe(report.realAuthorizationState));
        trySetState(
                store,
                "real_bet_diagnosis_required_real_bet",
                String.valueOf(report.requiredRealBet));
        trySetState(
                store,
                "real_bet_diagnosis_real_bet_debt",
                String.valueOf(report.realBetDebt));
        trySetState(
                store,
                "real_bet_diagnosis_runtime_first",
                safe(report.runtimeFirstSummary));
        trySetState(
                store, "real_bet_diagnosis_probe_frequency", String.valueOf(report.probeFrequency));
        trySetState(
                store,
                "real_bet_diagnosis_qualified_opportunities",
                String.valueOf(report.qualifiedOpportunityCount));
        trySetState(
                store,
                "real_bet_diagnosis_normal_bet_count",
                String.valueOf(report.normalBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_freq_recovery_bet_count",
                String.valueOf(report.frequencyRecoveryBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_real_probe_bet_count",
                String.valueOf(report.realProbeBetCount));
        trySetState(
                store,
                "real_bet_diagnosis_promotion_watch_domains",
                String.valueOf(report.promotionWatchDomains));
        trySetState(
                store,
                "real_bet_diagnosis_promotion_ready_domains",
                String.valueOf(report.promotionReadyDomains));
        trySetState(
                store,
                "real_bet_diagnosis_promotion_strong_domains",
                String.valueOf(report.promotionStrongDomains));
        trySetState(
                store,
                "real_bet_diagnosis_promotion_block_domains",
                String.valueOf(report.promotionBlockDomains));
        trySetState(store, "real_bet_diagnosis_snapshot_freshness", safe(report.snapshotFreshness));
        trySetState(
                store, "real_bet_diagnosis_performance_summary", safe(report.performanceSummary));
    }

    private void saveV50DiagnosisState(DbStore store, DiagnosisReport report) {
        trySetState(store, "real_bet_diagnosis_version", safe(report.version));
        trySetState(store, "real_bet_diagnosis_updated_at", String.valueOf(report.createdAt));
        trySetState(store, "real_bet_diagnosis_status", report.ok == 1 ? "OK" : "ERROR");
        trySetState(store, "real_bet_diagnosis_summary", safe(report.summary));
        trySetState(store, "real_bet_diagnosis_detail", safe(report.detail));
        trySetState(store, "real_bet_diagnosis_case_replay", safe(report.detail));
        trySetState(
                store,
                "real_bet_diagnosis_module_responsibility",
                safe(report.moduleResponsibilityText));
        trySetState(store, "real_bet_diagnosis_total", String.valueOf(report.totalBet));
        trySetState(store, "real_bet_diagnosis_hit", String.valueOf(report.hitBet));
        trySetState(store, "real_bet_diagnosis_miss", String.valueOf(report.missBet));
        trySetState(store, "real_bet_diagnosis_win_rate", String.valueOf(report.winRate));
        trySetState(store, "real_bet_diagnosis_pnl", String.valueOf(report.pnl));
        trySetState(store, "real_bet_diagnosis_shadow_total", String.valueOf(report.shadowTotal));
        trySetState(store, "real_bet_diagnosis_main_problem", safe(report.mainProblem));
        trySetState(store, "real_bet_diagnosis_next_fix", safe(report.nextFixAdvice));
        trySetState(
                store,
                "real_bet_diagnosis_authorization_state",
                safe(report.realAuthorizationState));
        trySetState(
                store,
                "real_bet_diagnosis_required_real_bet",
                String.valueOf(report.requiredRealBet));
        trySetState(
                store,
                "real_bet_diagnosis_real_bet_debt",
                String.valueOf(report.realBetDebt));
        trySetState(
                store,
                "real_bet_diagnosis_runtime_first",
                safe(report.runtimeFirstSummary));
        trySetState(
                store, "real_bet_diagnosis_performance_summary", safe(report.performanceSummary));
        trySetState(store, "real_bet_dynamic_causal_summary", "");
        trySetState(store, "real_bet_dynamic_causal_detail", "");
        trySetState(store, "real_bet_diagnosis_v44_funnel", "LEGACY_READ_ONLY");
    }

    private ArrayList<BetCase> loadRecentRealBetCases(DbStore store, int limit) {
        ArrayList<BetCase> out = new ArrayList<BetCase>();
        if (store == null) {
            return out;
        }
        ArrayList<Models.Decision> decisions =
                loadRecentDecisions(store, DEFAULT_DECISION_SCAN_LIMIT);
        HashMap<String, String> used = new HashMap<String, String>();
        for (int i = 0; i < decisions.size(); i++) {
            if (out.size() >= limit) {
                break;
            }
            Models.Decision d = decisions.get(i);
            if (d == null || !isBetDecision(d)) {
                continue;
            }
            String issue = safe(d.targetIssue);
            if (issue.length() == 0 || used.containsKey(issue)) {
                continue;
            }
            Models.Trade t = getTradeByTargetIssueSafe(store, issue);
            if (t == null) {
                continue;
            }
            if (!isRealBetTrade(t)) {
                continue;
            }
            BetCase c = fromTrade(store, t, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }
        if (out.size() < limit) {
            ArrayList<Models.Trade> reflectedTrades =
                    loadRecentTradesByReflection(store, limit * 2);
            for (int i = 0; i < reflectedTrades.size(); i++) {
                if (out.size() >= limit) {
                    break;
                }
                Models.Trade t = reflectedTrades.get(i);
                if (t == null || !isRealBetTrade(t)) {
                    continue;
                }
                String issue = safe(t.targetIssue);
                if (issue.length() == 0 || used.containsKey(issue)) {
                    continue;
                }
                Models.Decision d = getDecisionByTargetIssueSafe(store, issue);
                BetCase c = fromTrade(store, t, d);
                if (c != null) {
                    out.add(c);
                    used.put(issue, "1");
                }
            }
        }
        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<BetCase>(out.subList(0, limit));
        }
        return out;
    }

    private ArrayList<BetCase> loadRecentShadowCases(DbStore store, int limit) {
        ArrayList<BetCase> out = new ArrayList<BetCase>();
        if (store == null) {
            return out;
        }
        ArrayList<Models.Decision> decisions =
                loadRecentDecisions(store, DEFAULT_DECISION_SCAN_LIMIT);
        HashMap<String, String> used = new HashMap<String, String>();
        for (int i = 0; i < decisions.size(); i++) {
            if (out.size() >= limit) {
                break;
            }
            Models.Decision d = decisions.get(i);
            if (d == null || !isShadowDecision(d)) {
                continue;
            }
            String issue = safe(d.targetIssue);
            if (issue.length() == 0 || used.containsKey(issue)) {
                continue;
            }
            BetCase c = fromShadowDecision(store, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }
        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<BetCase>(out.subList(0, limit));
        }
        return out;
    }

    private BetCase fromTrade(DbStore store, Models.Trade t, Models.Decision d) {
        if (t == null) {
            return null;
        }
        BetCase c = new BetCase();
        c.kind = "BET";
        c.targetIssue = safe(t.targetIssue);
        c.baseIssue = safe(t.baseIssue);
        c.createdAt = t.createdAt > 0L ? t.createdAt : t.betTime;
        c.position = t.position;
        c.n1 = t.n1;
        c.n2 = t.n2;
        c.n3 = t.n3;
        c.actualNumber = t.actualNumber;
        c.result = normalizeResult(safe(t.result));
        c.hit = isWinResult(c.result) ? 1 : 0;
        c.settled = t.settled;
        c.pnl = t.pnl;
        c.missReason = safe(t.missReason);
        c.finalP = t.finalP;
        c.rawP = t.rawP;
        c.calibratedP = t.calibratedP;
        c.dynamicGate = t.dynamicGate;
        c.edge = t.edge;
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        c.effectiveSample = t.effectiveSample;
        c.comboSample = t.comboSample;
        c.similarHitRate = t.similarHitRate;
        c.comboHitRate = t.comboHitRate;
        c.stateMode = safe(t.stateMode);
        c.reasonSummary = safe(t.reasonSummary);
        copyOptionalCaseFields(c, t);
        if (d != null) {
            mergeDecisionFields(c, d);
        }
        if (c.actualNumber <= 0) {
            Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
            if (draw != null && draw.isValid() && AppConfig.isValidPosition(c.position)) {
                c.actualNumber = draw.getNumberAtPosition(c.position);
                if (AppConfig.isValidNumber(c.actualNumber)) {
                    c.hit = containsK3(c, c.actualNumber) ? 1 : 0;
                    c.result = c.hit == 1 ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS;
                }
            }
        }
        c.finalPBucket = finalPBucket(c.finalP);
        c.edgeBucket = edgeBucket(c.edge);
        c.teacherBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.teacherStatus, c.teacherMatrixStatus, c.teacherNumberStatus));
        c.v20Bucket =
                normalizeBucket(
                        firstNonEmpty(c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
        c.windowBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.windowStateKey,
                                c.decisionWindowStateKey,
                                c.dynamicWindowStateKey,
                                c.probabilityWindowStateKey));
        c.missBucket = normalizeBucket(c.missReason);
        normalizeDynamicCausalBuckets(c);
        return c;
    }

    private BetCase fromShadowDecision(DbStore store, Models.Decision d) {
        if (d == null || !isShadowDecision(d)) {
            return null;
        }
        BetCase c = new BetCase();
        c.kind = "SHADOW";
        c.targetIssue = safe(d.targetIssue);
        c.baseIssue = safe(d.baseIssue);
        c.createdAt = d.createdAt;
        c.position = d.position;
        c.n1 = d.n1;
        c.n2 = d.n2;
        c.n3 = d.n3;
        c.finalP = d.finalP;
        c.rawP = d.rawP;
        c.calibratedP = d.calibratedP;
        c.dynamicGate = d.dynamicGate;
        c.edge = d.edge;
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        c.effectiveSample = d.effectiveSample;
        c.comboSample = d.comboSample;
        c.similarHitRate = d.similarHitRate;
        c.comboHitRate = d.comboHitRate;
        c.stateMode = safe(d.stateMode);
        c.reasonSummary = safe(d.reasonSummary);
        mergeDecisionFields(c, d);
        Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
        if (draw != null && draw.isValid() && AppConfig.isValidPosition(c.position)) {
            c.actualNumber = draw.getNumberAtPosition(c.position);
            if (AppConfig.isValidNumber(c.actualNumber)) {
                c.settled = 1;
                c.hit = containsK3(c, c.actualNumber) ? 1 : 0;
                c.result = c.hit == 1 ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS;
            } else {
                c.settled = 0;
                c.result = AppConfig.RESULT_WAIT;
            }
        } else {
            c.settled = 0;
            c.result = AppConfig.RESULT_WAIT;
        }
        c.finalPBucket = finalPBucket(c.finalP);
        c.edgeBucket = edgeBucket(c.edge);
        c.teacherBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.teacherStatus, c.teacherMatrixStatus, c.teacherNumberStatus));
        c.v20Bucket =
                normalizeBucket(
                        firstNonEmpty(c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
        c.windowBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.windowStateKey,
                                c.decisionWindowStateKey,
                                c.dynamicWindowStateKey,
                                c.probabilityWindowStateKey));
        normalizeDynamicCausalBuckets(c);
        return c;
    }

    private void copyOptionalCaseFields(BetCase c, Object source) {
        if (c == null || source == null) {
            return;
        }
        c.teacherMatrixStatus = getString(source, "teacherMatrixStatus", c.teacherMatrixStatus);
        c.teacherNumberStatus = getString(source, "teacherNumberStatus", c.teacherNumberStatus);
        c.teacherStatus = getString(source, "teacherStatus", c.teacherStatus);
        c.teacherMatrixSummary = getString(source, "teacherMatrixSummary", c.teacherMatrixSummary);
        c.teacherEvidenceText =
                firstNonEmpty(
                        getString(source, "teacherEvidenceText", ""),
                        getString(source, "teacherMatrixEvidenceText", ""),
                        c.teacherEvidenceText);
        c.teacherCounterText =
                firstNonEmpty(
                        getString(source, "teacherCounterEvidenceText", ""),
                        getString(source, "teacherMatrixCounterText", ""),
                        c.teacherCounterText);
        c.teacherMatrixScore = getDouble(source, "teacherMatrixScore", c.teacherMatrixScore);
        c.teacherMatrixRisk = getDouble(source, "teacherMatrixRisk", c.teacherMatrixRisk);
        c.teacherMatrixConfidence =
                getDouble(source, "teacherMatrixConfidence", c.teacherMatrixConfidence);
        c.teacherMatrixSample = getInt(source, "teacherMatrixSample", c.teacherMatrixSample);
        c.teacherMatrixHitRate = getDouble(source, "teacherMatrixHitRate", c.teacherMatrixHitRate);
        c.teacherEvidenceContribution =
                firstPositive(
                        getDouble(source, "teacherEvidenceContribution", 0.0d),
                        getDouble(source, "teacherMatrixEvidenceContribution", 0.0d),
                        c.teacherEvidenceContribution);
        c.teacherCounterContribution =
                firstPositive(
                        getDouble(source, "teacherCounterContribution", 0.0d),
                        getDouble(source, "teacherMatrixCounterContribution", 0.0d),
                        c.teacherCounterContribution);
        c.windowStateKey =
                firstNonEmpty(
                        getString(source, "windowStateKey", ""),
                        getString(source, "finalDecisionWindowStateKey", ""),
                        getString(source, "teacherWindowStateKey", ""),
                        c.windowStateKey);
        c.dynamicWindowStateKey =
                getString(source, "dynamicWindowStateKey", c.dynamicWindowStateKey);
        c.probabilityWindowStateKey =
                getString(source, "probabilityWindowStateKey", c.probabilityWindowStateKey);
        c.decisionWindowStateKey =
                getString(source, "decisionWindowStateKey", c.decisionWindowStateKey);
        c.dynamicStatus =
                firstNonEmpty(
                        getString(source, "dynamicStatus", ""),
                        getString(source, "dynamicCauseStatus", ""),
                        c.dynamicStatus);
        c.v20Status =
                firstNonEmpty(
                        getString(source, "v20Status", ""),
                        getString(source, "v20CauseStatus", ""),
                        c.v20Status);
        c.evidenceContribution =
                firstPositive(
                        getDouble(source, "decisionEvidenceContribution", 0.0d),
                        getDouble(source, "dynamicGateEvidenceContribution", 0.0d),
                        getDouble(source, "probabilityEvidenceContribution", 0.0d),
                        getDouble(source, "evidenceContribution", c.evidenceContribution));
        c.counterContribution =
                firstPositive(
                        getDouble(source, "decisionCounterContribution", 0.0d),
                        getDouble(source, "dynamicGateCounterContribution", 0.0d),
                        getDouble(source, "probabilityCounterEvidence", 0.0d),
                        getDouble(source, "counterContribution", c.counterContribution));
        c.sampleReliability =
                firstPositive(
                        getDouble(source, "decisionSampleReliability", 0.0d),
                        getDouble(source, "dynamicGateSampleReliability", 0.0d),
                        getDouble(source, "probabilitySampleReliability", 0.0d),
                        getDouble(source, "sampleReliability", c.sampleReliability));
        c.recentValidity =
                firstPositive(
                        getDouble(source, "decisionRecentValidity", 0.0d),
                        getDouble(source, "dynamicGateRecentValidity", 0.0d),
                        getDouble(source, "probabilityRecentValidity", 0.0d),
                        getDouble(source, "recentValidity", c.recentValidity));
        c.featureDistance =
                firstPositive(
                        getDouble(source, "decisionFeatureDistance", 0.0d),
                        getDouble(source, "dynamicGateFeatureDistance", 0.0d),
                        getDouble(source, "probabilityFeatureDistance", 0.0d),
                        getDouble(source, "featureDistance", c.featureDistance));
        c.similarWindowSample =
                firstPositiveInt(
                        getInt(source, "similarWindowSample", 0),
                        getInt(source, "teacherSimilarWindowSample", 0),
                        c.similarWindowSample);
        c.similarWindowHitRate =
                firstPositive(
                        getDouble(source, "similarWindowHitRate", 0.0d),
                        getDouble(source, "teacherSimilarWindowHitRate", 0.0d),
                        c.similarWindowHitRate);
        c.evidenceText =
                firstNonEmpty(
                        getString(source, "decisionEvidenceText", ""),
                        getString(source, "dynamicGateEvidenceText", ""),
                        getString(source, "probabilityEvidenceText", ""),
                        getString(source, "evidenceText", c.evidenceText));
        c.counterText =
                firstNonEmpty(
                        getString(source, "decisionCounterEvidenceText", ""),
                        getString(source, "dynamicGateCounterEvidenceText", ""),
                        getString(source, "probabilityCounterText", ""),
                        getString(source, "counterText", c.counterText));
        c.decisionActionAdvice =
                firstNonEmpty(
                        getString(source, "decisionActionAdvice", ""),
                        getString(source, "dynamicGateActionAdvice", ""),
                        getString(source, "probabilityActionAdvice", ""),
                        getString(source, "actionAdvice", c.decisionActionAdvice));
        c.decisionHardRisk = getInt(source, "decisionHardRisk", c.decisionHardRisk);
        c.decisionCounterRiskHigh =
                getInt(source, "decisionCounterRiskHigh", c.decisionCounterRiskHigh);
        c.decisionEvidenceStrong =
                getInt(source, "decisionEvidenceStrong", c.decisionEvidenceStrong);
        c.decisionEvidenceElite = getInt(source, "decisionEvidenceElite", c.decisionEvidenceElite);
        c.v20SupportScore =
                firstPositive(
                        getDouble(source, "v20SupportScore", 0.0d),
                        getDouble(source, "dynamicV20SupportScore", 0.0d),
                        getDouble(source, "causeSupportScore", 0.0d),
                        c.v20SupportScore);
        c.v20RiskScore =
                firstPositive(
                        getDouble(source, "v20RiskScore", 0.0d),
                        getDouble(source, "dynamicV20RiskScore", 0.0d),
                        getDouble(source, "causeRiskScore", 0.0d),
                        c.v20RiskScore);
        c.causeScore =
                firstPositive(
                        getDouble(source, "causeScore", 0.0d),
                        getDouble(source, "dynamicCauseScore", 0.0d),
                        c.causeScore);
        c.positionCauseScore = getDouble(source, "positionCauseScore", c.positionCauseScore);
        c.numberCauseScore = getDouble(source, "numberCauseScore", c.numberCauseScore);
        c.counterfactualScore = getDouble(source, "counterfactualScore", c.counterfactualScore);
        c.thirdGuardScore = getDouble(source, "thirdGuardScore", c.thirdGuardScore);
        c.ruleHealthScore = getDouble(source, "ruleHealthScore", c.ruleHealthScore);
        c.shadowHealthScore = getDouble(source, "shadowHealthScore", c.shadowHealthScore);
        c.falseStrongRisk =
                firstPositive(
                        getDouble(source, "falseStrongRisk", 0.0d),
                        getDouble(source, "falseStrongRiskScore", 0.0d),
                        c.falseStrongRisk);
        c.seedExpansionScore = getDouble(source, "seedExpansionScore", c.seedExpansionScore);
        c.teacherRecentValidity =
                firstPositive(
                        getDouble(source, "teacherRecentValidity", 0.0d),
                        getDouble(source, "teacherMatrixRecentValidity", 0.0d),
                        c.teacherRecentValidity);
        c.teacherWeakNoBetFlag =
                firstPositiveInt(
                        getInt(source, "teacherWeakNoBetFlag", 0),
                        getInt(source, "teacherMatrixWeakNoBetFlag", 0),
                        c.teacherWeakNoBetFlag);
        c.teacherMatrixOrdinaryBetBlockFlag =
                getInt(
                        source,
                        "teacherMatrixOrdinaryBetBlockFlag",
                        c.teacherMatrixOrdinaryBetBlockFlag);
        c.teacherOrdinaryBetPolicy =
                firstNonEmpty(
                        getString(source, "teacherOrdinaryBetPolicy", ""),
                        getString(source, "teacherMatrixOrdinaryBetPolicy", ""),
                        c.teacherOrdinaryBetPolicy);
        c.coverageQualityScore =
                firstPositive(
                        getDouble(source, "coverageQualityScore", 0.0d),
                        getDouble(source, "dynamicGateCoverageQualityScore", 0.0d),
                        getDouble(source, "finalDecisionCoverageQuality", 0.0d),
                        c.coverageQualityScore);
        c.thirdNumberQualityScore =
                firstPositive(
                        getDouble(source, "thirdNumberQualityScore", 0.0d),
                        getDouble(source, "dynamicGateThirdNumberQuality", 0.0d),
                        getDouble(source, "finalDecisionThirdNumberQuality", 0.0d),
                        c.thirdNumberQualityScore);
        c.k3CoverageRisk =
                firstPositive(
                        getDouble(source, "k3CoverageRisk", 0.0d),
                        getDouble(source, "dynamicGateCoverageRisk", 0.0d),
                        getDouble(source, "finalDecisionCoverageRisk", 0.0d),
                        c.k3CoverageRisk);
        c.probabilityFalseHighRisk =
                firstPositive(
                        getDouble(source, "probabilityFalseHighRisk", 0.0d),
                        getDouble(source, "dynamicGateVirtualEdgeRisk", 0.0d),
                        c.probabilityFalseHighRisk);
        c.falseHighPFlag =
                firstPositiveInt(
                        getInt(source, "falseHighPFlag", 0),
                        getInt(source, "probabilityFalseHighPFlag", 0),
                        c.falseHighPFlag);
        c.dynamicGateFalseHighPFlag =
                firstPositiveInt(
                        getInt(source, "dynamicGateFalseHighPFlag", 0),
                        getInt(source, "finalDecisionFalseHighPFlag", 0),
                        c.dynamicGateFalseHighPFlag);
        c.dynamicGatePreferredEdgeBand =
                getInt(source, "dynamicGatePreferredEdgeBand", c.dynamicGatePreferredEdgeBand);
        c.probabilityPreferredEdgeBand =
                getInt(source, "probabilityPreferredEdgeBand", c.probabilityPreferredEdgeBand);
        c.probabilityTeacherWeakOrEmpty =
                getInt(source, "probabilityTeacherWeakOrEmpty", c.probabilityTeacherWeakOrEmpty);
        c.probabilityV20UnknownOrEmpty =
                getInt(source, "probabilityV20UnknownOrEmpty", c.probabilityV20UnknownOrEmpty);
        c.probabilityStrongCauseFlag =
                getInt(source, "probabilityStrongCauseFlag", c.probabilityStrongCauseFlag);
        c.dynamicGateEdgePolicy =
                getString(source, "dynamicGateEdgePolicy", c.dynamicGateEdgePolicy);
        c.dynamicGateVersion = getString(source, "dynamicGateVersion", c.dynamicGateVersion);
        c.finalDecisionVersion = getString(source, "finalDecisionVersion", c.finalDecisionVersion);
        c.finalDecisionEdgePolicy =
                getString(source, "finalDecisionEdgePolicy", c.finalDecisionEdgePolicy);
        c.finalDecisionTeacherPolicy =
                getString(source, "finalDecisionTeacherPolicy", c.finalDecisionTeacherPolicy);
        c.finalDecisionV20Policy =
                getString(source, "finalDecisionV20Policy", c.finalDecisionV20Policy);
        c.finalDecisionBetDomain =
                firstNonEmpty(
                        getString(source, "betDomain", ""),
                        getString(source, "finalDecisionBetDomain", ""),
                        c.finalDecisionBetDomain);
        c.finalDecisionRejectCode =
                getString(source, "finalDecisionRejectCode", c.finalDecisionRejectCode);
        c.finalDecisionShadowReason =
                getString(source, "finalDecisionShadowReason", c.finalDecisionShadowReason);
        c.finalDecisionCoverageRisk =
                firstPositive(
                        getDouble(source, "finalDecisionCoverageRisk", 0.0d),
                        c.k3CoverageRisk,
                        c.finalDecisionCoverageRisk);
        c.finalDecisionThirdNumberQuality =
                firstPositive(
                        getDouble(source, "finalDecisionThirdNumberQuality", 0.0d),
                        c.thirdNumberQualityScore,
                        c.finalDecisionThirdNumberQuality);
        c.finalDecisionFalseHighPFlag =
                getInt(source, "finalDecisionFalseHighPFlag", c.finalDecisionFalseHighPFlag);
        c.finalDecisionStatus = getString(source, "finalDecisionStatus", c.finalDecisionStatus);
        c.finalDecisionQualityScore =
                getDouble(source, "finalDecisionQualityScore", c.finalDecisionQualityScore);
        c.finalDecisionRiskScore =
                getDouble(source, "finalDecisionRiskScore", c.finalDecisionRiskScore);
        c.finalDecisionActionAdvice =
                getString(source, "finalDecisionActionAdvice", c.finalDecisionActionAdvice);
        c.adaptiveStatus =
                firstNonEmpty(
                        getString(source, "adaptiveStatus", ""),
                        getString(source, "adaptiveJudgeStatus", ""),
                        c.adaptiveStatus);
        c.adaptiveAction =
                firstNonEmpty(
                        getString(source, "adaptiveAction", ""),
                        getString(source, "adaptiveJudgeAction", ""),
                        c.adaptiveAction);
        c.adaptiveRejectCode = getString(source, "adaptiveRejectCode", c.adaptiveRejectCode);
        c.shadowPromotionStatus =
                getString(source, "shadowPromotionStatus", c.shadowPromotionStatus);
        c.shadowPromotionDomain =
                firstNonEmpty(
                        getString(source, "shadowPromotionDomain", ""),
                        getString(source, "shadowPromotionDomainKey", ""),
                        c.shadowPromotionDomain);
        c.shadowPromotionRejectCode =
                firstNonEmpty(
                        getString(source, "shadowPromotionRejectCode", ""),
                        getString(source, "shadowRejectCode", ""),
                        c.shadowPromotionRejectCode);
        c.shadowPromoteScore =
                firstPositive(
                        getDouble(source, "shadowPromoteScore", 0.0d),
                        getDouble(source, "shadowPromotionScore", 0.0d),
                        c.shadowPromoteScore);
        c.ruleDecayRisk = getDouble(source, "ruleDecayRisk", c.ruleDecayRisk);
        c.ruleRejectCode = getString(source, "ruleRejectCode", c.ruleRejectCode);
        c.thirdDragRisk =
                firstPositive(
                        getDouble(source, "thirdDragRisk", 0.0d),
                        getDouble(source, "thirdDragScore", 0.0d),
                        c.thirdDragRisk);
        c.thirdRejectCode = getString(source, "thirdRejectCode", c.thirdRejectCode);
        c.counterfactualRisk =
                firstPositive(
                        getDouble(source, "counterfactualRisk", 0.0d),
                        getDouble(source, "counterEvidenceScore", 0.0d),
                        c.counterfactualRisk);
        c.positionNeedScore = getDouble(source, "positionNeedScore", c.positionNeedScore);
        c.numberSupplyScore = getDouble(source, "numberSupplyScore", c.numberSupplyScore);
        c.coverageQuality =
                firstPositive(
                        getDouble(source, "coverageQuality", 0.0d),
                        c.coverageQualityScore,
                        c.coverageQuality);
        c.thirdQuality =
                firstPositive(
                        getDouble(source, "thirdQuality", 0.0d),
                        c.thirdNumberQualityScore,
                        c.thirdQuality);
        c.netEvidenceScore = getDouble(source, "netEvidenceScore", c.netEvidenceScore);
        c.mainChainRejectCode =
                firstNonEmpty(
                        getString(source, "mainChainRejectCode", ""),
                        getString(source, "dynamicGateMainChainRejectCode", ""),
                        getString(source, "probabilityMainChainRejectCode", ""),
                        c.mainChainRejectCode);
        c.fieldChainVersion =
                firstNonEmpty(
                        getString(source, "fieldChainVersion", ""),
                        getString(source, "dynamicGateFieldVersion", ""),
                        getString(source, "probabilityMainChainVersion", ""),
                        c.fieldChainVersion);
        c.decisionGateVersion =
                firstNonEmpty(
                        getString(source, "decisionGateVersion", ""),
                        getString(source, "finalDecisionVersion", ""),
                        c.decisionGateVersion);
        c.realProbeAllowed = getInt(source, "realProbeAllowed", c.realProbeAllowed);
        c.teacherNoSampleNeutralFlag =
                getInt(source, "teacherNoSampleNeutralFlag", c.teacherNoSampleNeutralFlag);
        c.v20WatchNeutralFlag = getInt(source, "v20WatchNeutralFlag", c.v20WatchNeutralFlag);
        c.adaptiveWatchNeutralFlag =
                getInt(source, "adaptiveWatchNeutralFlag", c.adaptiveWatchNeutralFlag);
        copyDynamicCausalCaseFields(c, source);
    }

    private void copyDynamicCausalCaseFields(BetCase c, Object source) {
        if (c == null || source == null) return;
        c.dynamicClusterId = getString(source, "dynamicClusterId", c.dynamicClusterId);
        c.dynamicClusterStatus = getString(source, "dynamicClusterStatus", c.dynamicClusterStatus);
        c.dynamicClusterSample = getInt(source, "dynamicClusterSample", c.dynamicClusterSample);
        c.dynamicClusterRecentSample =
                getInt(source, "dynamicClusterRecentSample", c.dynamicClusterRecentSample);
        c.dynamicClusterHitRate =
                getDouble(source, "dynamicClusterHitRate", c.dynamicClusterHitRate);
        c.dynamicClusterRecentHitRate =
                getDouble(source, "dynamicClusterRecentHitRate", c.dynamicClusterRecentHitRate);
        c.dynamicClusterMaxLose = getInt(source, "dynamicClusterMaxLose", c.dynamicClusterMaxLose);
        c.needSupplyMatchScore = getDouble(source, "needSupplyMatchScore", c.needSupplyMatchScore);
        c.needSupplyThirdQuality =
                getDouble(source, "needSupplyThirdQuality", c.needSupplyThirdQuality);
        c.needSupplyRisk = getDouble(source, "needSupplyRisk", c.needSupplyRisk);
        c.counterfactualWhyNotRisk =
                getDouble(source, "counterfactualWhyNotRisk", c.counterfactualWhyNotRisk);
        c.causeAblationScore = getDouble(source, "causeAblationScore", c.causeAblationScore);
        c.causeAblationFalseCauseCount =
                getInt(source, "causeAblationFalseCauseCount", c.causeAblationFalseCauseCount);
        c.adaptiveJudgeStatus = getString(source, "adaptiveJudgeStatus", c.adaptiveJudgeStatus);
        c.adaptiveJudgeAction = getString(source, "adaptiveJudgeAction", c.adaptiveJudgeAction);
        c.adaptiveFreezeScore = getDouble(source, "adaptiveFreezeScore", c.adaptiveFreezeScore);
        c.promotionClusterStatus =
                getString(source, "promotionClusterStatus", c.promotionClusterStatus);
        c.promotionAction = getString(source, "promotionAction", c.promotionAction);
        c.dynamicCausalDecisionSummary =
                firstNonEmpty(
                        getString(source, "dynamicCausalDecisionSummary", ""),
                        getString(source, "finalDecisionDynamicCausalSummary", ""),
                        c.dynamicCausalDecisionSummary);
        c.dynamicCausalDecisionQuality =
                getDouble(source, "dynamicCausalDecisionQuality", c.dynamicCausalDecisionQuality);
        c.dynamicCausalDecisionRisk =
                getDouble(source, "dynamicCausalDecisionRisk", c.dynamicCausalDecisionRisk);
        c.dynamicCausalSummary = getString(source, "dynamicCausalSummary", c.dynamicCausalSummary);
    }

    private void normalizeDynamicCausalBuckets(BetCase c) {
        if (c == null) return;
        c.dynamicClusterBucket =
                normalizeBucket(firstNonEmpty(c.dynamicClusterId, c.dynamicClusterStatus));
        c.needSupplyBucket = dynamicNeedSupplyBucket(c);
        c.counterfactualBucket = dynamicCounterfactualBucket(c);
        c.adaptiveBucket = dynamicAdaptiveBucket(c);
        c.promotionBucket =
                normalizeBucket(firstNonEmpty(c.promotionClusterStatus, c.promotionAction));
        c.dynamicCausalBucket =
                dynamicCausalHardRisk(c)
                        ? "DYNAMIC_CAUSAL_HARD_RISK"
                        : (dynamicCausalReady(c) ? "DYNAMIC_CAUSAL_READY" : "DYNAMIC_CAUSAL_WATCH");
    }

    private String dynamicNeedSupplyBucket(BetCase c) {
        if (c == null) return BUCKET_EMPTY;
        if (c.needSupplyRisk >= DYNAMIC_CAUSAL_HARD_RISK) return "NS_HARD_RISK";
        if (c.needSupplyMatchScore >= DYNAMIC_CAUSAL_MIN_NEED_SUPPLY
                && c.needSupplyThirdQuality >= DYNAMIC_CAUSAL_MIN_THIRD_QUALITY
                && c.needSupplyRisk <= DYNAMIC_CAUSAL_MAX_RISK) return "NS_READY";
        if (c.needSupplyThirdQuality > 0.0d
                && c.needSupplyThirdQuality < DYNAMIC_CAUSAL_MIN_THIRD_QUALITY)
            return "NS_THIRD_DRAG";
        return "NS_WEAK";
    }

    private String dynamicCounterfactualBucket(BetCase c) {
        if (c == null) return BUCKET_EMPTY;
        if (c.counterfactualWhyNotRisk >= DYNAMIC_CAUSAL_HARD_RISK
                || c.causeAblationFalseCauseCount >= 4) return "CF_HARD_RISK";
        if (c.counterfactualWhyNotRisk >= DYNAMIC_CAUSAL_MAX_RISK) return "CF_RISK";
        if (c.causeAblationScore > 0.0d && c.causeAblationFalseCauseCount <= 2) return "CF_SUPPORT";
        return "CF_WATCH";
    }

    private String dynamicAdaptiveBucket(BetCase c) {
        if (c == null) return BUCKET_EMPTY;
        String s = upper(firstNonEmpty(c.adaptiveJudgeAction, c.adaptiveJudgeStatus));
        if (s.indexOf("FREEZE") >= 0 || s.indexOf("REJECT") >= 0 || s.indexOf("RISK") >= 0)
            return "ADAPTIVE_FREEZE";
        if (s.indexOf("STRONG") >= 0) return "ADAPTIVE_STRONG";
        if (s.indexOf("READY") >= 0 || s.indexOf("PROMOTE") >= 0 || s.indexOf("PASS") >= 0)
            return "ADAPTIVE_READY";
        return "ADAPTIVE_WATCH";
    }

    private void mergeDecisionFields(BetCase c, Models.Decision d) {
        if (c == null || d == null) {
            return;
        }
        if (c.reasonSummary.length() == 0) {
            c.reasonSummary = safe(d.reasonSummary);
        }
        if (c.finalP <= 0.0d) c.finalP = d.finalP;
        if (c.rawP <= 0.0d) c.rawP = d.rawP;
        if (c.calibratedP <= 0.0d) c.calibratedP = d.calibratedP;
        if (c.dynamicGate <= 0.0d) c.dynamicGate = d.dynamicGate;
        if (c.edge == 0.0d) c.edge = d.edge;
        if (c.effectiveSample <= 0) c.effectiveSample = d.effectiveSample;
        if (c.comboSample <= 0) c.comboSample = d.comboSample;
        if (c.similarHitRate <= 0.0d) c.similarHitRate = d.similarHitRate;
        if (c.comboHitRate <= 0.0d) c.comboHitRate = d.comboHitRate;
        if (c.stateMode.length() == 0) c.stateMode = safe(d.stateMode);
        copyOptionalCaseFields(c, d);
    }

    private void analyzeBetCases(DiagnosisReport report, ArrayList<BetCase> cases) {
        if (report == null) {
            return;
        }
        report.positionStats = new StatBucket[POSITION_COUNT + 1];
        for (int i = 0; i <= POSITION_COUNT; i++) {
            report.positionStats[i] = new StatBucket("P" + i);
        }
        report.finalPStats = new HashMap<String, StatBucket>();
        report.edgeStats = new HashMap<String, StatBucket>();
        report.teacherStats = new HashMap<String, StatBucket>();
        report.v20Stats = new HashMap<String, StatBucket>();
        report.windowStats = new HashMap<String, StatBucket>();
        report.missStats = new HashMap<String, StatBucket>();
        report.stateModeStats = new HashMap<String, StatBucket>();
        report.dynamicClusterStats = new HashMap<String, StatBucket>();
        report.needSupplyStats = new HashMap<String, StatBucket>();
        report.counterfactualStats = new HashMap<String, StatBucket>();
        report.adaptiveStats = new HashMap<String, StatBucket>();
        report.promotionStats = new HashMap<String, StatBucket>();
        report.dynamicCausalStats = new HashMap<String, StatBucket>();
        report.failureTypeStats = new HashMap<String, StatBucket>();
        report.responsibleModuleStats = new HashMap<String, StatBucket>();
        report.moduleDiagnosisMap = new HashMap<String, ModuleDiagnosis>();
        report.caseDiagnoses = new ArrayList<CaseDiagnosis>();
        if (cases == null) {
            return;
        }
        for (int i = 0; i < cases.size(); i++) {
            BetCase c = cases.get(i);
            if (c == null || !"BET".equals(c.kind)) {
                continue;
            }
            if (c.settled != 1) {
                report.waitBet++;
                continue;
            }
            if (!isWinResult(c.result) && !isMissResult(c.result)) {
                report.dataErrorBet++;
                continue;
            }
            report.totalBet++;
            if (c.hit == 1 || isWinResult(c.result)) {
                report.hitBet++;
            } else {
                report.missBet++;
            }
            report.pnl += c.pnl;
            report.avgFinalP += c.finalP;
            report.avgGate += c.dynamicGate;
            report.avgEdge += c.edge;
            addStat(report.positionStats, c.position, c);
            addStat(report.finalPStats, c.finalPBucket, c);
            addStat(report.edgeStats, c.edgeBucket, c);
            addStat(report.teacherStats, c.teacherBucket, c);
            addStat(report.v20Stats, c.v20Bucket, c);
            addStat(report.windowStats, c.windowBucket, c);
            addStat(report.missStats, c.missBucket, c);
            addStat(report.stateModeStats, normalizeBucket(c.stateMode), c);
            addStat(report.dynamicClusterStats, c.dynamicClusterBucket, c);
            addStat(report.needSupplyStats, c.needSupplyBucket, c);
            addStat(report.counterfactualStats, c.counterfactualBucket, c);
            addStat(report.adaptiveStats, c.adaptiveBucket, c);
            addStat(report.promotionStats, c.promotionBucket, c);
            addStat(report.dynamicCausalStats, c.dynamicCausalBucket, c);
            applyCounterfactualThreshold(report, c);
            addModuleRealSample(report, "ProbabilityEngine", true, c);
            addModuleRealSample(
                    report, "DynamicGateEngine", c.dynamicGate > 0.0d || c.edge != 0.0d, c);
            addModuleRealSample(report, "DecisionGateEngine", true, c);
            addModuleRealSample(
                    report, "TeacherNumberMatrixEngine", !isEmptyBucket(c.teacherBucket), c);
            addModuleRealSample(report, "DynamicCauseChainEngine", !isEmptyBucket(c.v20Bucket), c);
            addModuleRealSample(report, "WindowState", !isEmptyBucket(c.windowBucket), c);
            addModuleRealSample(report, "K3Coverage", true, c);
            if (c.edge < 0.0d) report.negativeEdgeBet++;
            if (containsAny(c.teacherBucket, "RISK", "FREEZE", "风险", "冻结")) report.teacherRiskBet++;
            if (c.decisionHardRisk > 0
                    || c.decisionCounterRiskHigh > 0
                    || c.v20RiskScore > c.v20SupportScore + 0.050d) report.v20RiskBet++;
            updateFinalBetFixCounters(report, c);
            updateFieldChainVerifyCounters(report, c);
            updateDynamicCausalVerifyCounters(report, c);
        }
        if (report.totalBet > 0) {
            report.winRate = safeRate(report.hitBet, report.totalBet);
            report.avgFinalP = report.avgFinalP / report.totalBet;
            report.avgGate = report.avgGate / report.totalBet;
            report.avgEdge = report.avgEdge / report.totalBet;
        }
        report.bestPosition = bestBucketFromArray(report.positionStats, true);
        report.worstPosition = bestBucketFromArray(report.positionStats, false);
        report.bestFinalPBucket = bestBucket(report.finalPStats, true);
        report.worstFinalPBucket = bestBucket(report.finalPStats, false);
        report.bestTeacherBucket = bestBucket(report.teacherStats, true);
        report.worstTeacherBucket = bestBucket(report.teacherStats, false);
        report.bestV20Bucket = bestBucket(report.v20Stats, true);
        report.worstV20Bucket = bestBucket(report.v20Stats, false);
        report.topMissBucket = topCountBucket(report.missStats);
        buildCaseDiagnoses(report, cases);
        finalizeModuleDiagnoses(report);
    }

    private void analyzeShadowCases(DiagnosisReport report, ArrayList<BetCase> cases) {
        if (report == null) {
            return;
        }
        report.shadowFinalPStats = new HashMap<String, StatBucket>();
        report.shadowTeacherStats = new HashMap<String, StatBucket>();
        report.shadowV20Stats = new HashMap<String, StatBucket>();
        report.shadowDynamicCausalStats = new HashMap<String, StatBucket>();
        report.shadowCaseDiagnoses = new ArrayList<ShadowCaseDiagnosis>();
        if (cases == null) {
            return;
        }
        for (int i = 0; i < cases.size(); i++) {
            BetCase c = cases.get(i);
            if (c == null || !"SHADOW".equals(c.kind)) {
                continue;
            }
            if (c.settled != 1) {
                report.shadowWait++;
                continue;
            }
            report.shadowTotal++;
            if (c.hit == 1 || isWinResult(c.result)) {
                report.shadowHit++;
            } else {
                report.shadowMiss++;
            }
            addStat(report.shadowFinalPStats, c.finalPBucket, c);
            addStat(report.shadowTeacherStats, c.teacherBucket, c);
            addStat(report.shadowV20Stats, c.v20Bucket, c);
            addStat(report.shadowDynamicCausalStats, c.dynamicCausalBucket, c);
            applyShadowCounterfactualThreshold(report, c);
            addModuleShadowSample(report, "DecisionGateEngine", true, c);
            addModuleShadowSample(report, "ProbabilityEngine", true, c);
            addModuleShadowSample(
                    report, "TeacherNumberMatrixEngine", !isEmptyBucket(c.teacherBucket), c);
            addModuleShadowSample(
                    report, "DynamicCauseChainEngine", !isEmptyBucket(c.v20Bucket), c);
            updateShadowFixCounters(report, c);
            updateShadowDynamicCausalCounters(report, c);
            report.shadowCaseDiagnoses.add(buildShadowCaseDiagnosis(c));
        }
        if (report.shadowTotal > 0) {
            report.shadowHitRate = safeRate(report.shadowHit, report.shadowTotal);
        }
        report.bestShadowFinalPBucket = bestBucket(report.shadowFinalPStats, true);
        report.bestShadowTeacherBucket = bestBucket(report.shadowTeacherStats, true);
        report.bestShadowV20Bucket = bestBucket(report.shadowV20Stats, true);
        finalizeModuleDiagnoses(report);
    }

    private void applyCounterfactualThreshold(DiagnosisReport r, BetCase c) {
        if (r == null || c == null) {
            return;
        }
        if (c.finalP >= 0.360d) addThreshold(r.betP36, c);
        if (c.finalP >= 0.380d) addThreshold(r.betP38, c);
        if (c.finalP >= 0.400d) addThreshold(r.betP40, c);
        if (c.edge >= 0.000d) addThreshold(r.betEdge0, c);
        if (c.edge >= 0.010d) addThreshold(r.betEdge1, c);
        if (c.edge >= 0.020d) addThreshold(r.betEdge2, c);
    }

    private void applyShadowCounterfactualThreshold(DiagnosisReport r, BetCase c) {
        if (r == null || c == null) {
            return;
        }
        if (c.finalP >= 0.360d) addThreshold(r.shadowP36, c);
        if (c.finalP >= 0.380d) addThreshold(r.shadowP38, c);
        if (c.finalP >= 0.400d) addThreshold(r.shadowP40, c);
        if (c.edge >= 0.000d) addThreshold(r.shadowEdge0, c);
        if (c.edge >= 0.010d) addThreshold(r.shadowEdge1, c);
        if (c.edge >= 0.020d) addThreshold(r.shadowEdge2, c);
    }

    private void addThreshold(StatBucket b, BetCase c) {
        if (b == null || c == null) return;
        b.add(c);
    }

    private void buildCaseDiagnoses(DiagnosisReport report, ArrayList<BetCase> cases) {
        if (report == null) {
            return;
        }
        if (report.caseDiagnoses == null) {
            report.caseDiagnoses = new ArrayList<CaseDiagnosis>();
        }
        if (cases == null) {
            return;
        }
        for (int i = 0; i < cases.size(); i++) {
            BetCase c = cases.get(i);
            if (c == null || !"BET".equals(c.kind) || c.settled != 1) {
                continue;
            }
            CaseDiagnosis cd = buildCaseDiagnosis(report, c);
            if (cd != null) {
                report.caseDiagnoses.add(cd);
                addStat(report.failureTypeStats, cd.failureType, c);
                addStat(report.responsibleModuleStats, cd.responsibleModule, c);
                markModuleResponsibility(report, cd.responsibleModule, c, cd.failureType);
            }
            if (report.caseDiagnoses.size() >= DEFAULT_BET_LIMIT) {
                break;
            }
        }
    }

    private CaseDiagnosis buildCaseDiagnosis(DiagnosisReport report, BetCase c) {
        if (c == null) {
            return null;
        }
        CaseDiagnosis d = new CaseDiagnosis();
        d.targetIssue = safe(c.targetIssue);
        d.baseIssue = safe(c.baseIssue);
        d.decisionType = safe(c.kind);
        d.position = c.position;
        d.n1 = c.n1;
        d.n2 = c.n2;
        d.n3 = c.n3;
        d.actualNumber = c.actualNumber;
        d.result = normalizeResult(c.result);
        d.hit = c.hit;
        d.pnl = c.pnl;
        d.finalP = c.finalP;
        d.dynamicGate = c.dynamicGate;
        d.edge = c.edge;
        d.rawP = c.rawP;
        d.calibratedP = c.calibratedP;
        d.teacherStatus =
                firstNonEmpty(c.teacherStatus, c.teacherMatrixStatus, c.teacherNumberStatus);
        d.teacherSample = firstPositiveInt(c.teacherMatrixSample, c.comboSample, c.effectiveSample);
        d.teacherHitRate = firstPositive(c.teacherMatrixHitRate, c.comboHitRate, c.similarHitRate);
        d.teacherConfidence = c.teacherMatrixConfidence;
        d.teacherRecentValidity = firstPositive(c.teacherRecentValidity, c.recentValidity, 0.0d);
        d.v20Status = firstNonEmpty(c.v20Status, c.dynamicStatus, c.decisionActionAdvice);
        d.v20Support = firstPositive(c.v20SupportScore, c.causeScore, c.evidenceContribution);
        d.v20Risk = firstPositive(c.v20RiskScore, c.falseStrongRisk, c.counterContribution);
        d.evidenceContribution = c.evidenceContribution;
        d.counterContribution = c.counterContribution;
        d.sampleReliability = c.sampleReliability;
        d.recentValidity = c.recentValidity;
        d.windowStateKey =
                firstNonEmpty(
                        c.windowStateKey,
                        c.decisionWindowStateKey,
                        c.dynamicWindowStateKey,
                        c.probabilityWindowStateKey);
        d.similarWindowSample = c.similarWindowSample;
        d.similarWindowHitRate = c.similarWindowHitRate;
        d.missReason = safe(c.missReason);
        d.reasonSummary = safe(c.reasonSummary);
        fillDynamicCausalCaseDiagnosis(d, c);
        fillMainChainCaseDiagnosis(d, c);
        d.failureType = classifyFailureType(report, c, d);
        d.responsibleModule = responsibleModuleForFailure(d.failureType);
        d.fixHint = fixHintForFailure(d.failureType, c, d);
        return d;
    }

    private String classifyFailureType(DiagnosisReport report, BetCase c, CaseDiagnosis d) {
        if (c == null || d == null) {
            return "UNKNOWN";
        }
        if (c.hit == 1 || isWinResult(c.result)) {
            return "HIT_CONFIRMED";
        }
        String dynamicFailure = classifyDynamicCausalFailure(c, d);
        if (dynamicFailure.length() > 0) {
            return dynamicFailure;
        }
        String fieldFailure = classifyFieldChainFailure(c, d);
        if (fieldFailure.length() > 0) {
            return fieldFailure;
        }
        if (c.edge < 0.0d
                || (c.finalP > 0.0d && c.dynamicGate > 0.0d && c.finalP < c.dynamicGate)) {
            return "NEGATIVE_EDGE_BET";
        }
        if (isV20Empty(c, d) && !isV20WatchOrEmptyNeutral(c, d) && c.finalP >= 0.380d) {
            return "V20_EMPTY_STILL_BET";
        }
        if (isTeacherRisk(d.teacherStatus)
                || containsAny(c.teacherBucket, "RISK", "FREEZE", "风险", "冻结")) {
            return "TEACHER_FAKE_STRONG";
        }
        if (isTeacherWeak(d.teacherStatus)
                && !isTeacherNoSampleNeutral(c, d)
                && c.finalP >= 0.370d) {
            return "TEACHER_WEAK_STILL_BET";
        }
        if (isTeacherStrong(d.teacherStatus)
                && d.teacherSample >= 20
                && d.teacherHitRate > 0.0d
                && d.teacherHitRate < 0.330d) {
            return "TEACHER_FAKE_STRONG";
        }
        if (!isV20Empty(c, d) && d.v20Support > 0.0d && d.v20Risk >= d.v20Support) {
            return "V20_FAKE_SUPPORT";
        }
        if (isWindowUntrusted(d)) {
            return "WINDOW_STATE_UNTRUSTED";
        }
        if (isPositionDrag(report, c.position)) {
            return "POSITION_DRAG";
        }
        if (c.actualNumber > 0 && !containsK3(c, c.actualNumber)) {
            if (c.n3 > 0
                    && c.finalP >= 0.390d
                    && d.teacherSample >= 30
                    && d.teacherHitRate < 0.340d) {
                return "THIRD_NUMBER_WEAK";
            }
            return "K3_COVERAGE_MISS";
        }
        if (c.finalP >= 0.400d
                && ((isTeacherWeak(d.teacherStatus) && !isTeacherNoSampleNeutral(c, d))
                        || (isV20Empty(c, d) && !isV20WatchOrEmptyNeutral(c, d))
                        || d.sampleReliability < 0.450d
                        || d.recentValidity < 0.450d)) {
            return "PROBABILITY_OVER_ESTIMATED";
        }
        if (c.decisionHardRisk > 0 || c.decisionCounterRiskHigh > 0) {
            return "GATE_WRONG_RELEASE";
        }
        return "PROBABILITY_OVER_ESTIMATED";
    }

    private ShadowCaseDiagnosis buildShadowCaseDiagnosis(BetCase c) {
        ShadowCaseDiagnosis d = new ShadowCaseDiagnosis();
        if (c == null) {
            return d;
        }
        d.targetIssue = safe(c.targetIssue);
        d.position = c.position;
        d.n1 = c.n1;
        d.n2 = c.n2;
        d.n3 = c.n3;
        d.actualNumber = c.actualNumber;
        d.wouldHit = c.hit;
        d.finalP = c.finalP;
        d.gate = c.dynamicGate;
        d.edge = c.edge;
        d.teacherStatus =
                firstNonEmpty(c.teacherStatus, c.teacherMatrixStatus, c.teacherNumberStatus);
        d.v20Status = firstNonEmpty(c.v20Status, c.dynamicStatus, c.decisionActionAdvice);
        d.shadowReason = safe(c.reasonSummary);
        d.shadowPromotionDomain = AppConfig.sanitizePromotionDomainKey(c.shadowPromotionDomain);
        if (d.wouldHit == 1 && c.finalP >= 0.380d && c.edge >= 0.0d) {
            d.promotionHint =
                    "PROMOTE_WATCH：SHADOW命中且FinalP/Edge达标，后续由ShadowPromotionVerifier统计同类域，不可直接转BET";
        } else if (d.wouldHit == 1) {
            d.promotionHint = "WATCH：SHADOW命中，但证据强度不足，需要同类样本继续验证";
        } else {
            d.promotionHint = "NO_PROMOTE：SHADOW未中或证据不足，只保留反事实样本";
        }
        fillDynamicCausalShadowDiagnosis(d, c);
        fillMainChainShadowDiagnosis(d, c);
        return d;
    }

    private void loadFieldChainVerifyState(DbStore store, DiagnosisReport report) {
        if (store == null || report == null) {
            return;
        }
        report.fieldChainAutoFetchVersion =
                getStateSafe(store, "field_chain_auto_downgrade_version", "");
        if (report.fieldChainAutoFetchVersion.length() == 0) {
            report.fieldChainAutoFetchVersion =
                    getStateSafe(store, "field_chain_patch_version", "");
        }
        report.fieldChainCandidateMatch = getStateSafe(store, "field_chain_candidate_match", "");
        report.fieldChainCandidateStage = getStateSafe(store, "field_chain_candidate_stage", "");
        report.fieldChainCandidateSummary =
                getStateSafe(store, "field_chain_candidate_summary", "");
        report.fieldChainAutoDowngrade = getStateSafe(store, "field_chain_auto_downgrade", "");
        report.fieldChainAutoDowngradeCode =
                getStateSafe(store, "field_chain_auto_downgrade_code", "");
        report.fieldChainAutoDowngradeReason =
                getStateSafe(store, "field_chain_auto_downgrade_reason", "");
        report.fieldChainTradeRepair = getStateSafe(store, "field_chain_trade_repair", "");
        report.fieldChainTradeRepairSummary =
                getStateSafe(store, "field_chain_trade_repair_summary", "");
        report.fieldChainDbStoreVersion = getStateSafe(store, "field_chain_dbstore_version", "");
        report.fieldChainLastDecisionSummary =
                getStateSafe(store, "field_chain_last_decision_summary", "");
        report.lastTradeCreateResult = getStateSafe(store, "last_trade_create_result", "");
        try {
            Models.Decision current = store.getLatestDecision();
            if (current != null
                    && !AppConfig.DECISION_BET.equals(
                            AppConfig.normalizeDecision(current.decision))) {
                report.lastTradeCreateResult = AppConfig.TRADE_LINK_SKIP_NOT_REAL_BET;
                report.lastTradeCreateError = "";
            } else {
                report.lastTradeCreateError = getStateSafe(store, "last_trade_create_error", "");
            }
        } catch (Throwable e) {
            report.lastTradeCreateError = getStateSafe(store, "last_trade_create_error", "");
        }
        report.lastTradeFieldChainReason = getStateSafe(store, "last_trade_field_chain_reason", "");
    }

    private String getStateSafe(DbStore store, String key, String def) {
        if (store == null) {
            return def == null ? "" : def;
        }
        try {
            String v = store.getState(key, def == null ? "" : def);
            return v == null ? (def == null ? "" : def) : v;
        } catch (Throwable e) {
            return def == null ? "" : def;
        }
    }

    private void updateDynamicCausalVerifyCounters(DiagnosisReport report, BetCase c) {
        if (report == null || c == null) return;
        if (dynamicClusterBadStillBet(c)) report.dynamicClusterBadBet++;
        if (needSupplyBadStillBet(c)) report.needSupplyBadBet++;
        if (counterfactualBadStillBet(c)) report.counterfactualBadBet++;
        if (adaptiveFreezeStillBet(c)) report.adaptiveFreezeStillBet++;
        if (promotionFreezeStillBet(c)) report.promotionFreezeStillBet++;
        if (dynamicCausalReady(c)) {
            report.dynamicCausalReadyBet++;
            if (c.hit == 1 || isWinResult(c.result)) report.dynamicCausalReadyHit++;
        }
        if (dynamicCausalHardRisk(c)) report.dynamicCausalHardRiskStillBet++;
    }

    private void updateShadowDynamicCausalCounters(DiagnosisReport report, BetCase c) {
        if (report == null || c == null) return;
        if (dynamicCausalReady(c)) {
            report.shadowDynamicCausalReady++;
            if (c.hit == 1 || isWinResult(c.result)) report.shadowDynamicCausalReadyHit++;
        }
    }

    private void analyzeDynamicCausalCases(
            DiagnosisReport report, ArrayList<BetCase> betCases, ArrayList<BetCase> shadowCases) {
        if (report == null) return;
        report.bestDynamicCausalBucket = bestBucket(report.dynamicCausalStats, true);
        report.worstDynamicCausalBucket = bestBucket(report.dynamicCausalStats, false);
        double readyRate = safeRate(report.dynamicCausalReadyHit, report.dynamicCausalReadyBet);
        double shadowReadyRate =
                safeRate(report.shadowDynamicCausalReadyHit, report.shadowDynamicCausalReady);
        report.dynamicCausalSummary =
                "动态因果诊断：真实READY="
                        + report.dynamicCausalReadyBet
                        + "/中"
                        + report.dynamicCausalReadyHit
                        + "/率"
                        + pct(readyRate)
                        + "；SHADOW_READY="
                        + report.shadowDynamicCausalReady
                        + "/中"
                        + report.shadowDynamicCausalReadyHit
                        + "/率"
                        + pct(shadowReadyRate)
                        + "；动态簇坏单="
                        + report.dynamicClusterBadBet
                        + "；NeedSupply坏单="
                        + report.needSupplyBadBet
                        + "；反事实坏单="
                        + report.counterfactualBadBet
                        + "；Adaptive冻结仍BET="
                        + report.adaptiveFreezeStillBet
                        + "；Promotion冻结仍BET="
                        + report.promotionFreezeStillBet;
        String problem = "";
        String fix = "";
        if (report.dynamicClusterBadBet > 0) {
            problem = append(problem, "动态窗口簇低于随机/样本不足/连挂高仍BET=" + report.dynamicClusterBadBet);
            fix =
                    append(
                            fix,
                            "检查 DynamicWindowClusterEngine → AutoFetchService → DecisionGateEngine"
                                + " 字段桥");
        }
        if (report.needSupplyBadBet > 0) {
            problem = append(problem, "NeedSupply匹配不足/第三号拖拽仍BET=" + report.needSupplyBadBet);
            fix = append(fix, "检查 NeedSupplyReasonEngine 与 DecisionGateEngine 硬拦");
        }
        if (report.counterfactualBadBet > 0) {
            problem = append(problem, "反事实硬反证/假因过多仍BET=" + report.counterfactualBadBet);
            fix = append(fix, "检查 CounterfactualReasonEngine 与 dynamicCausalFinalBlockCode");
        }
        if (report.adaptiveFreezeStillBet > 0 || report.promotionFreezeStillBet > 0) {
            problem =
                    append(
                            problem,
                            "Adaptive/Promotion冻结域仍BET="
                                    + (report.adaptiveFreezeStillBet
                                            + report.promotionFreezeStillBet));
            fix =
                    append(
                            fix,
                            "检查 ShadowPromotionVerifierEngine / AdaptiveLearningJudgeEngine 字段写回");
        }
        if (problem.length() == 0) problem = "动态因果字段暂未发现明显失控，或样本不足";
        if (fix.length() == 0) fix = "继续积累真实BET与SHADOW动态样本，观察READY域是否稳定高于30%基准";
        report.dynamicCausalMainProblem = problem;
        report.dynamicCausalNextFix = fix;
        report.dynamicCausalDetailText =
                report.dynamicCausalSummary + "\n主要问题：" + problem + "\n下一步：" + fix;
    }

    private String classifyDynamicCausalFailure(BetCase c, CaseDiagnosis d) {
        if (c == null) return "";
        if (dynamicClusterBadStillBet(c)) return "DYNAMIC_CLUSTER_BAD_STILL_BET";
        if (needSupplyBadStillBet(c)) return "DYNAMIC_NEED_SUPPLY_BAD_STILL_BET";
        if (counterfactualBadStillBet(c)) return "DYNAMIC_COUNTERFACTUAL_BAD_STILL_BET";
        if (adaptiveFreezeStillBet(c)) return "DYNAMIC_ADAPTIVE_FREEZE_STILL_BET";
        if (promotionFreezeStillBet(c)) return "DYNAMIC_PROMOTION_FREEZE_STILL_BET";
        if (c.dynamicCausalDecisionRisk >= DYNAMIC_CAUSAL_HARD_RISK && c.hit != 1)
            return "DYNAMIC_CAUSAL_DECISION_RISK_STILL_BET";
        return "";
    }

    private boolean dynamicClusterBadStillBet(BetCase c) {
        if (c == null) return false;
        String s = upper(c.dynamicClusterStatus);
        if (s.indexOf("FREEZE") >= 0
                || s.indexOf("RISK") >= 0
                || s.indexOf("DEAD") >= 0
                || s.indexOf("WEAK") >= 0) return true;
        if (c.dynamicClusterSample > 0
                && c.dynamicClusterSample < DYNAMIC_CAUSAL_MIN_CLUSTER_SAMPLE) return true;
        if (c.dynamicClusterHitRate > 0.0d
                && c.dynamicClusterHitRate < DYNAMIC_CAUSAL_RANDOM_BASELINE) return true;
        if (c.dynamicClusterRecentHitRate > 0.0d
                && c.dynamicClusterRecentHitRate < DYNAMIC_CAUSAL_RANDOM_BASELINE) return true;
        if (c.dynamicClusterMaxLose >= DYNAMIC_CAUSAL_MAX_LOSE) return true;
        return false;
    }

    private boolean needSupplyBadStillBet(BetCase c) {
        if (c == null) return false;
        if (c.needSupplyRisk >= DYNAMIC_CAUSAL_HARD_RISK) return true;
        if (c.needSupplyMatchScore > 0.0d
                && c.needSupplyMatchScore < DYNAMIC_CAUSAL_MIN_NEED_SUPPLY) return true;
        if (c.needSupplyThirdQuality > 0.0d
                && c.needSupplyThirdQuality < DYNAMIC_CAUSAL_MIN_THIRD_QUALITY) return true;
        return false;
    }

    private boolean counterfactualBadStillBet(BetCase c) {
        if (c == null) return false;
        return c.counterfactualWhyNotRisk >= DYNAMIC_CAUSAL_HARD_RISK
                || c.causeAblationFalseCauseCount >= 4;
    }

    private boolean adaptiveFreezeStillBet(BetCase c) {
        if (c == null) return false;
        String s = upper(firstNonEmpty(c.adaptiveJudgeAction, c.adaptiveJudgeStatus));
        return s.indexOf("FREEZE") >= 0
                || s.indexOf("REJECT") >= 0
                || s.indexOf("RISK") >= 0
                || c.adaptiveFreezeScore >= DYNAMIC_CAUSAL_HARD_RISK;
    }

    private boolean promotionFreezeStillBet(BetCase c) {
        if (c == null) return false;
        String s = upper(firstNonEmpty(c.promotionAction, c.promotionClusterStatus));
        return s.indexOf("FREEZE") >= 0 || s.indexOf("BLOCK") >= 0 || s.indexOf("RISK") >= 0;
    }

    private boolean dynamicCausalReady(BetCase c) {
        if (c == null) return false;
        boolean clusterReady =
                containsAny(c.dynamicClusterStatus, "READY", "STRONG")
                        || (c.dynamicClusterSample >= DYNAMIC_CAUSAL_MIN_CLUSTER_SAMPLE
                                && c.dynamicClusterHitRate >= DYNAMIC_CAUSAL_READY_RATE);
        boolean needReady =
                c.needSupplyMatchScore >= DYNAMIC_CAUSAL_MIN_NEED_SUPPLY
                        && c.needSupplyThirdQuality >= DYNAMIC_CAUSAL_MIN_THIRD_QUALITY
                        && c.needSupplyRisk <= DYNAMIC_CAUSAL_MAX_RISK;
        boolean cfOk =
                c.counterfactualWhyNotRisk <= DYNAMIC_CAUSAL_MAX_RISK
                        && c.causeAblationFalseCauseCount <= 3;
        return clusterReady
                && needReady
                && cfOk
                && !adaptiveFreezeStillBet(c)
                && !promotionFreezeStillBet(c);
    }

    private boolean dynamicCausalHardRisk(BetCase c) {
        return dynamicClusterBadStillBet(c)
                || needSupplyBadStillBet(c)
                || counterfactualBadStillBet(c)
                || adaptiveFreezeStillBet(c)
                || promotionFreezeStillBet(c);
    }

    private void fillDynamicCausalCaseDiagnosis(CaseDiagnosis d, BetCase c) {
        if (d == null || c == null) return;
        d.dynamicClusterStatus = c.dynamicClusterStatus;
        d.dynamicClusterSample = c.dynamicClusterSample;
        d.dynamicClusterHitRate = c.dynamicClusterHitRate;
        d.needSupplyMatchScore = c.needSupplyMatchScore;
        d.needSupplyThirdQuality = c.needSupplyThirdQuality;
        d.needSupplyRisk = c.needSupplyRisk;
        d.counterfactualWhyNotRisk = c.counterfactualWhyNotRisk;
        d.causeAblationFalseCauseCount = c.causeAblationFalseCauseCount;
        d.adaptiveJudgeStatus = c.adaptiveJudgeStatus;
        d.adaptiveJudgeAction = c.adaptiveJudgeAction;
        d.promotionClusterStatus = c.promotionClusterStatus;
        d.promotionAction = c.promotionAction;
        d.dynamicCausalDecisionQuality = c.dynamicCausalDecisionQuality;
        d.dynamicCausalDecisionRisk = c.dynamicCausalDecisionRisk;
        d.dynamicCausalSummary = buildDynamicCausalCaseSummary(c);
    }

    private void fillDynamicCausalShadowDiagnosis(ShadowCaseDiagnosis d, BetCase c) {
        if (d == null || c == null) return;
        d.dynamicClusterStatus = c.dynamicClusterStatus;
        d.needSupplyMatchScore = c.needSupplyMatchScore;
        d.needSupplyThirdQuality = c.needSupplyThirdQuality;
        d.counterfactualWhyNotRisk = c.counterfactualWhyNotRisk;
        d.adaptiveJudgeAction = c.adaptiveJudgeAction;
        d.promotionClusterStatus = c.promotionClusterStatus;
        d.dynamicCausalBucket = c.dynamicCausalBucket;
        d.dynamicCausalSummary = buildDynamicCausalCaseSummary(c);
    }

    private String buildDynamicCausalCaseSummary(BetCase c) {
        if (c == null) return "";
        return "动态因果｜cluster="
                + c.dynamicClusterBucket
                + "｜NS="
                + c.needSupplyBucket
                + "｜CF="
                + c.counterfactualBucket
                + "｜AD="
                + c.adaptiveBucket
                + "｜PROM="
                + c.promotionBucket
                + "｜Q="
                + pct(c.dynamicCausalDecisionQuality)
                + "｜R="
                + pct(c.dynamicCausalDecisionRisk);
    }

    private String dynamicCausalFixHintForFailure(String failureType) {
        String f = safe(failureType);
        if ("DYNAMIC_CLUSTER_BAD_STILL_BET".equals(f))
            return "检查 DynamicWindowClusterEngine/ShadowPromotionVerifier/DecisionGate：低于随机基准、样本不足、连挂高的簇必须冻结或SHADOW";
        if ("DYNAMIC_NEED_SUPPLY_BAD_STILL_BET".equals(f))
            return "检查 NeedSupplyReasonEngine/DecisionGate：NeedSupply匹配不足、第三号拖拽、供给风险高不得真实BET";
        if ("DYNAMIC_COUNTERFACTUAL_BAD_STILL_BET".equals(f))
            return "检查 CounterfactualReasonEngine/DecisionGate：反事实硬反证、疑似假因过多必须转SHADOW/HOLD";
        if ("DYNAMIC_ADAPTIVE_FREEZE_STILL_BET".equals(f))
            return "检查 AdaptiveLearningJudgeEngine字段桥：FREEZE/REJECT仍真实BET说明最终门没有吃到审判结果";
        if ("DYNAMIC_PROMOTION_FREEZE_STILL_BET".equals(f))
            return "检查 ShadowPromotionVerifierEngine晋级域：FREEZE/BLOCK域仍真实BET必须硬拦";
        if ("DYNAMIC_CAUSAL_DECISION_RISK_STILL_BET".equals(f))
            return "检查 DecisionGateEngine"
                       + " dynamicCausalFinalBlockCode：dynamicCausalDecisionRisk硬风险仍放行";
        return "";
    }

    private String upper(String s) {
        return s == null ? "" : s.toUpperCase(java.util.Locale.ROOT);
    }

    private void updateFieldChainVerifyCounters(DiagnosisReport report, BetCase c) {
        if (report == null || c == null) {
            return;
        }
        if (fieldChainWeakV20EmptyStillBet(c)) report.fieldChainWeakV20EmptyStillBet++;
        if (fieldChainWindowEmptyStillBet(c)) report.fieldChainWindowEmptyStillBet++;
        if (fieldChainCoverEmptyStillBet(c)) report.fieldChainCoverEmptyStillBet++;
        if (fieldChainEdgeFalseStrongByPhenomenon(c))
            report.fieldChainEdgeFalseStrongByPhenomenon++;
        if (fieldChainP36_38WeakStillBet(c)) report.fieldChainP36_38WeakStillBet++;
        if (fieldChainP40NoStrongStillBet(c)) report.fieldChainP40NoStrongStillBet++;
        if (fieldChainRejectMissingBet(c)) report.fieldChainRejectMissingBet++;
        if (fieldChainVersionMissingBet(c)) report.fieldChainVersionMissingBet++;
    }

    private String classifyFieldChainFailure(BetCase c, CaseDiagnosis d) {
        if (c == null) {
            return "";
        }
        String reject = safe(c.finalDecisionRejectCode);
        if (reject.length() > 0
                && !containsAny(
                        reject,
                        "PASS",
                        "READY",
                        "OK",
                        "NORMAL_REVIEW",
                        "NONE",
                        "NO_SAMPLE_NEUTRAL",
                        "WATCH_NEUTRAL",
                        "PENDING",
                        "NEUTRAL")) {
            return "FIELD_CHAIN_REJECT_CODE_STILL_BET";
        }
        if (teacherWeakNoBetStillBet(c, d)) return "FINAL_TEACHER_WEAK_NO_BET_STILL_BET";
        if (falseHighPStillBet(c, d)) return "FINAL_FALSE_HIGH_P_STILL_BET";
        if (k3CoverageRiskStillBet(c, d)) return "FINAL_K3_COVERAGE_RISK_STILL_BET";
        if (thirdNumberWeakStillBet(c, d)) return "FINAL_THIRD_NUMBER_WEAK_STILL_BET";
        if (fieldChainWeakV20EmptyStillBet(c)) return "FIELD_CHAIN_WEAK_V20_EMPTY_STILL_BET";
        if (fieldChainCoverEmptyStillBet(c)) return "FIELD_CHAIN_COVER_EMPTY_STILL_BET";
        if (fieldChainEdgeFalseStrongByPhenomenon(c))
            return "FIELD_CHAIN_EDGE_FALSE_STRONG_STILL_BET";
        if (fieldChainP36_38WeakStillBet(c)) return "FIELD_CHAIN_P36_38_WEAK_STILL_BET";
        if (fieldChainP40NoStrongStillBet(c)) return "FIELD_CHAIN_P40_NO_STRONG_CAUSE_STILL_BET";
        if (fieldChainVersionMissingBet(c)) return "FIELD_CHAIN_VERSION_MISSING_STILL_BET";
        return "";
    }

    private boolean fieldChainWeakV20EmptyStillBet(BetCase c) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, null) || isV20WatchOrEmptyNeutral(c, null)) return false;
        return teacherWeakOrEmpty(c, null)
                && v20UnknownOrWeak(c, null)
                && c.finalP < 0.430d
                && !hasStrongOverrideForDiagnosis(c, null);
    }

    private boolean fieldChainWindowEmptyStillBet(BetCase c) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, null) || isV20WatchOrEmptyNeutral(c, null)) return false;
        return isEmptyBucket(
                        firstNonEmpty(
                                c.windowStateKey,
                                c.decisionWindowStateKey,
                                c.dynamicWindowStateKey,
                                c.probabilityWindowStateKey))
                && teacherWeakOrEmpty(c, null)
                && v20UnknownOrWeak(c, null)
                && c.finalP < 0.430d;
    }

    private boolean fieldChainCoverEmptyStillBet(BetCase c) {
        if (c == null) return false;
        double risk = firstPositive(c.k3CoverageRisk, c.finalDecisionCoverageRisk, 0.0d);
        double cover = firstPositive(c.coverageQualityScore, 0.0d, 0.0d);
        double third =
                firstPositive(c.thirdNumberQualityScore, c.finalDecisionThirdNumberQuality, 0.0d);
        if (isTeacherNoSampleNeutral(c, null) || isV20WatchOrEmptyNeutral(c, null)) return false;
        return cover <= 0.0001d
                && risk <= 0.0001d
                && third <= 0.0001d
                && teacherWeakOrEmpty(c, null)
                && v20UnknownOrWeak(c, null)
                && c.finalP < 0.425d;
    }

    private boolean fieldChainEdgeFalseStrongByPhenomenon(BetCase c) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, null) || isV20WatchOrEmptyNeutral(c, null)) return false;
        return normalizedEdge(c) > FIX_EDGE_FALSE_STRONG_LINE
                && teacherWeakOrEmpty(c, null)
                && v20UnknownOrWeak(c, null)
                && !hasStrongOverrideForDiagnosis(c, null)
                && c.finalP < 0.430d;
    }

    private boolean fieldChainP36_38WeakStillBet(BetCase c) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, null) || isV20WatchOrEmptyNeutral(c, null)) return false;
        return c.finalP >= 0.360d
                && c.finalP < 0.380d
                && teacherWeakOrEmpty(c, null)
                && v20UnknownOrWeak(c, null)
                && !hasStrongOverrideForDiagnosis(c, null);
    }

    private boolean fieldChainP40NoStrongStillBet(BetCase c) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, null) || isV20WatchOrEmptyNeutral(c, null)) return false;
        return c.finalP >= 0.400d
                && c.finalP < 0.430d
                && teacherWeakOrEmpty(c, null)
                && v20UnknownOrWeak(c, null)
                && !hasStrongOverrideForDiagnosis(c, null);
    }

    private boolean fieldChainRejectMissingBet(BetCase c) {
        if (c == null) return false;
        String reject = safe(c.finalDecisionRejectCode);
        return reject.length() == 0
                && !isTeacherNoSampleNeutral(c, null)
                && !isV20WatchOrEmptyNeutral(c, null)
                && (teacherWeakOrEmpty(c, null)
                        || v20UnknownOrWeak(c, null)
                        || normalizedEdge(c) > FIX_EDGE_FALSE_STRONG_LINE)
                && c.finalP >= 0.360d;
    }

    private boolean fieldChainVersionMissingBet(BetCase c) {
        if (c == null) return true;
        String v =
                firstNonEmpty(
                        c.finalDecisionVersion,
                        firstNonEmpty(c.fieldChainVersion, c.decisionGateVersion));
        if (v == null || v.length() == 0) return true;
        return !(v.indexOf("V20_") >= 0 || v.indexOf(AppConfig.ALWAYS_ON_RUNTIME_FIX_VERSION) >= 0);
    }

    private void updateFinalBetFixCounters(DiagnosisReport report, BetCase c) {
        if (report == null || c == null) {
            return;
        }
        if (teacherWeakNoBetStillBet(c, null)) report.teacherWeakNoBetStillBet++;
        if (falseHighPStillBet(c, null)) report.falseHighPStillBet++;
        if (k3CoverageRiskStillBet(c, null)) report.k3CoverageRiskStillBet++;
        if (thirdNumberWeakStillBet(c, null)) report.thirdNumberWeakStillBet++;
        if (edgeFalseStrongStillBet(c, null)) report.edgeFalseStrongStillBet++;
        if (p40NoStrongCauseStillBet(c, null)) report.p40NoStrongCauseStillBet++;
        if (hasFinalRejectStillBet(c)) report.finalRejectCodeStillBet++;
        if (finalPreferredDomain(c)) {
            report.preferredDomainBet++;
            if (c.hit == 1 || isWinResult(c.result)) report.preferredDomainHit++;
        }
    }

    private void updateShadowFixCounters(DiagnosisReport report, BetCase c) {
        if (report == null || c == null) {
            return;
        }
        if (finalPreferredShadowDomain(c)) {
            report.shadowPreferredDomain++;
            if (c.hit == 1 || isWinResult(c.result)) report.shadowPreferredDomainHit++;
        }
        if (falseHighPStillBet(c, null)) report.shadowFalseHighP++;
        if (edgeFalseStrongStillBet(c, null)) report.shadowEdgeFalseStrong++;
        if (k3CoverageRiskStillBet(c, null)) report.shadowCoverageRisk++;
    }

    private boolean hasFinalRejectStillBet(BetCase c) {
        if (c == null) return false;
        String r = safe(c.finalDecisionRejectCode);
        if (r.length() == 0 || isEmptyBucket(r)) return false;
        if (containsAny(r, "NONE", "NORMAL_REVIEW")) return false;
        return c.hit != 1;
    }

    private boolean teacherWeakNoBetStillBet(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, d)) {
            return false;
        }
        if (c.teacherWeakNoBetFlag > 0
                || c.teacherMatrixOrdinaryBetBlockFlag > 0
                || containsAny(c.teacherOrdinaryBetPolicy, "WEAK_NO_BET", "弱", "不得普通BET")) {
            return !hasStrongOverrideForDiagnosis(c, d);
        }
        return false;
    }

    private boolean falseHighPStillBet(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        if (c.falseHighPFlag > 0
                || c.dynamicGateFalseHighPFlag > 0
                || c.finalDecisionFalseHighPFlag > 0
                || containsAny(c.finalDecisionRejectCode, "FALSE_HIGH")
                || containsAny(c.dynamicGateEdgePolicy, "假高P")) {
            return c.finalP < 0.425d || !hasStrongOverrideForDiagnosis(c, d);
        }
        return false;
    }

    private boolean k3CoverageRiskStillBet(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        double risk = firstPositive(c.k3CoverageRisk, c.finalDecisionCoverageRisk, 0.0d);
        if (risk >= FIX_K3_COVERAGE_HARD_RISK) return true;
        if (risk >= FIX_K3_COVERAGE_RISK_LINE
                && !finalPreferredDomain(c)
                && !hasStrongOverrideForDiagnosis(c, d)) return true;
        return false;
    }

    private boolean thirdNumberWeakStillBet(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        double q =
                firstPositive(c.thirdNumberQualityScore, c.finalDecisionThirdNumberQuality, 1.0d);
        return q > 0.0d && q < FIX_THIRD_NUMBER_MIN && c.finalP < 0.420d;
    }

    private boolean edgeFalseStrongStillBet(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        double e = normalizedEdge(c);
        if (containsAny(c.dynamicGateEdgePolicy, "虚强", "SHADOW复核")) {
            return !hasStrongOverrideForDiagnosis(c, d);
        }
        return e > FIX_EDGE_FALSE_STRONG_LINE
                && !hasStrongOverrideForDiagnosis(c, d)
                && c.finalP < 0.430d;
    }

    private boolean p40NoStrongCauseStillBet(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c, d) || isV20WatchOrEmptyNeutral(c, d)) return false;
        return c.finalP >= FIX_P40_FALSE_HIGH_LINE
                && teacherWeakOrEmpty(c, d)
                && v20UnknownOrWeak(c, d)
                && !hasStrongOverrideForDiagnosis(c, d);
    }

    private boolean hasStrongOverrideForDiagnosis(BetCase c, CaseDiagnosis d) {
        if (c == null) return false;
        if (c.probabilityStrongCauseFlag > 0
                && c.counterContribution < 0.56d
                && c.k3CoverageRisk < 0.56d) return true;
        String teacher =
                firstNonEmpty(
                        c.teacherMatrixStatus, c.teacherStatus, d == null ? "" : d.teacherStatus);
        String v20 = firstNonEmpty(c.v20Status, c.dynamicStatus, d == null ? "" : d.v20Status);
        if (isTeacherStrong(teacher) && c.teacherMatrixRisk < 0.62d) return true;
        if (containsAny(v20, "STRONG", "READY", "PASS", "V20_STRONG", "V20_READY", "S", "A")
                && c.v20RiskScore <= c.v20SupportScore + 0.050d) return true;
        if (c.decisionEvidenceStrong > 0 || c.decisionEvidenceElite > 0) return true;
        return false;
    }

    private boolean teacherWeakOrEmpty(BetCase c, CaseDiagnosis d) {
        if (isTeacherNoSampleNeutral(c, d)) {
            return false;
        }
        String s =
                c == null
                        ? ""
                        : firstNonEmpty(
                                c.teacherMatrixStatus,
                                c.teacherStatus,
                                c.teacherNumberStatus,
                                c.teacherBucket);
        if (s.length() == 0 && d != null) s = safe(d.teacherStatus);
        if (isEmptyBucket(s)) return false;
        return isTeacherWeak(s) && !isTeacherStrong(s) && !isTeacherHardRisk(s);
    }

    private boolean v20UnknownOrWeak(BetCase c, CaseDiagnosis d) {
        if (isV20WatchOrEmptyNeutral(c, d)) {
            return false;
        }
        String s = c == null ? "" : firstNonEmpty(c.v20Status, c.dynamicStatus, c.v20Bucket);
        if (s.length() == 0 && d != null) s = safe(d.v20Status);
        if (isEmptyBucket(s)) return false;
        String u = safe(s).toUpperCase(java.util.Locale.ROOT);
        return u.indexOf("STRONG") < 0
                && u.indexOf("READY") < 0
                && u.indexOf("PASS") < 0
                && !"S".equals(u)
                && !"A".equals(u)
                && !isV20HardRiskText(u);
    }

    private boolean finalPreferredDomain(BetCase c) {
        if (c == null) return false;
        if (containsAny(c.finalDecisionBetDomain, "P38_40_EDGE1_2")) return true;
        if (c.dynamicGatePreferredEdgeBand > 0 || c.probabilityPreferredEdgeBand > 0) {
            return firstPositive(c.k3CoverageRisk, c.finalDecisionCoverageRisk, 1.0d) < 0.50d
                    && firstPositive(
                                    c.thirdNumberQualityScore,
                                    c.finalDecisionThirdNumberQuality,
                                    0.0d)
                            >= FIX_THIRD_NUMBER_MIN;
        }
        double e = normalizedEdge(c);
        return c.finalP >= FIX_PREFERRED_P_MIN
                && c.finalP <= FIX_PREFERRED_P_MAX
                && e >= FIX_PREFERRED_EDGE_MIN
                && e <= FIX_PREFERRED_EDGE_MAX
                && firstPositive(c.k3CoverageRisk, c.finalDecisionCoverageRisk, 1.0d) < 0.50d
                && firstPositive(c.thirdNumberQualityScore, c.finalDecisionThirdNumberQuality, 0.0d)
                        >= FIX_THIRD_NUMBER_MIN;
    }

    private boolean finalPreferredShadowDomain(BetCase c) {
        return finalPreferredDomain(c)
                && c != null
                && c.finalP >= FIX_PREFERRED_P_MIN
                && c.edge >= 0.0d
                && !k3CoverageRiskStillBet(c, null)
                && !thirdNumberWeakStillBet(c, null);
    }

    private double normalizedEdge(BetCase c) {
        if (c == null) return 0.0d;
        double e = c.edge;
        if (e == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            e = c.finalP - c.dynamicGate;
        }
        if (Double.isNaN(e) || Double.isInfinite(e)) return 0.0d;
        return e;
    }

    private String coverageBucket(BetCase c) {
        if (c == null) return BUCKET_EMPTY;
        double risk = firstPositive(c.k3CoverageRisk, c.finalDecisionCoverageRisk, 0.0d);
        double q = c.coverageQualityScore;
        double third =
                firstPositive(c.thirdNumberQualityScore, c.finalDecisionThirdNumberQuality, 0.0d);
        if (risk >= FIX_K3_COVERAGE_HARD_RISK) return "COVERAGE_HARD_RISK";
        if (risk >= FIX_K3_COVERAGE_RISK_LINE) return "COVERAGE_RISK";
        if (third > 0.0d && third < FIX_THIRD_NUMBER_MIN) return "THIRD_WEAK";
        if (q > 0.0d && q < FIX_COVERAGE_QUALITY_MIN) return "COVERAGE_LOW";
        if (finalPreferredDomain(c)) return "P38_40_EDGE1_2_COVER_OK";
        return "COVERAGE_NORMAL";
    }

    private String edgePolicyBucket(BetCase c) {
        if (c == null) return BUCKET_EMPTY;
        if (containsAny(c.dynamicGateEdgePolicy, "虚强")) return "EDGE_FALSE_STRONG_POLICY";
        if (containsAny(c.dynamicGateEdgePolicy, "P38-40", "Edge1-2"))
            return "P38_40_EDGE1_2_POLICY";
        if (normalizedEdge(c) > FIX_EDGE_FALSE_STRONG_LINE) return "EDGE_GE_2_RECHECK";
        if (normalizedEdge(c) >= FIX_PREFERRED_EDGE_MIN) return "EDGE_1_2";
        return "EDGE_LOW";
    }

    private String responsibleModuleForFailure(String failureType) {
        String f = safe(failureType);
        if (f.indexOf("DYNAMIC_CLUSTER") >= 0) return "DynamicWindowClusterEngine";
        if (f.indexOf("DYNAMIC_NEED_SUPPLY") >= 0) return "NeedSupplyReasonEngine";
        if (f.indexOf("DYNAMIC_COUNTERFACTUAL") >= 0) return "CounterfactualReasonEngine";
        if (f.indexOf("DYNAMIC_ADAPTIVE") >= 0) return "AdaptiveLearningJudgeEngine";
        if (f.indexOf("DYNAMIC_PROMOTION") >= 0) return "ShadowPromotionVerifierEngine";
        if (f.indexOf("DYNAMIC_CAUSAL") >= 0) return "DecisionGateEngine";
        if (f.indexOf("FIELD_CHAIN") >= 0) return "DecisionGateEngine";
        if (f.indexOf("FINAL_TEACHER") >= 0
                || f.indexOf("FINAL_EDGE") >= 0
                || f.indexOf("FINAL_P40") >= 0
                || f.indexOf("FINAL_REJECT") >= 0) return "DecisionGateEngine";
        if (f.indexOf("FINAL_FALSE_HIGH") >= 0
                || f.indexOf("FINAL_K3") >= 0
                || f.indexOf("FINAL_THIRD") >= 0) return "DecisionGateEngine";
        if (f.indexOf("PROBABILITY") >= 0) return "ProbabilityEngine";
        if (f.indexOf("TEACHER") >= 0) return "TeacherNumberMatrixEngine";
        if (f.indexOf("V20") >= 0) return "DynamicCauseChainEngine";
        if (f.indexOf("WINDOW") >= 0) return "DynamicCauseChainEngine";
        if (f.indexOf("POSITION") >= 0) return "DynamicGateEngine";
        if (f.indexOf("K3") >= 0 || f.indexOf("THIRD") >= 0) return "ProbabilityEngine";
        if (f.indexOf("EDGE") >= 0 || f.indexOf("GATE") >= 0) return "DecisionGateEngine";
        if (f.indexOf("SHADOW") >= 0) return "DecisionGateEngine";
        if ("HIT_CONFIRMED".equals(f)) return "NONE";
        return "DecisionGateEngine";
    }

    private String fixHintForFailure(String failureType, BetCase c, CaseDiagnosis d) {
        String f = safe(failureType);
        String dynFix = dynamicCausalFixHintForFailure(f);
        if (dynFix.length() > 0) return dynFix;
        if ("HIT_CONFIRMED".equals(f)) return "本单命中，作为强域样本沉淀，不需要降权";
        if ("FIELD_CHAIN_REJECT_CODE_STILL_BET".equals(f)
                || "FINAL_REJECT_CODE_STILL_BET".equals(f))
            return "检查 DecisionGateEngine/AutoFetchService/DbStore：finalDecisionRejectCode"
                       + " 已标记拒绝但仍成为真实 BET，说明最终出口字段复制、保存或Trade创建顺序仍需联调";
        if ("FIELD_CHAIN_WEAK_V20_EMPTY_STILL_BET".equals(f))
            return "检查 AutoFetchService + DecisionGateEngine：Teacher弱/空 +"
                       + " V20空/弱现象仍进入真实BET，字段链兜底硬审判未生效";
        if ("FIELD_CHAIN_COVER_EMPTY_STILL_BET".equals(f))
            return "检查 ProbabilityEngine/AutoFetchService/DbStore：Cover=0/Third=0字段未穿透，或覆盖空仍被创建真实Trade";
        if ("FIELD_CHAIN_EDGE_FALSE_STRONG_STILL_BET".equals(f))
            return "检查 DynamicGateEngine/DecisionGateEngine：Edge>=2虚强无Teacher/V20强因仍BET，应转SHADOW";
        if ("FIELD_CHAIN_P36_38_WEAK_STILL_BET".equals(f))
            return "检查 DecisionGateEngine：P36-38弱域无Teacher/V20强因不得普通BET";
        if ("FIELD_CHAIN_P40_NO_STRONG_CAUSE_STILL_BET".equals(f))
            return "检查 ProbabilityEngine/DecisionGateEngine：P40-42无Teacher/V20/Window强因属于假高P";
        if ("FIELD_CHAIN_VERSION_MISSING_STILL_BET".equals(f))
            return "检查 DbStore：finalDecisionVersion未读写V20_FIELD_CHAIN_DECISION_GATE版本，说明Decision/Trade字段落库仍丢失";
        if ("FINAL_TEACHER_WEAK_NO_BET_STILL_BET".equals(f))
            return "检查 DecisionGateEngine：Teacher闭环已给WEAK_NO_BET/teacherWeakNoBetFlag，最终BET必须转SHADOW，除非Teacher/V20/覆盖质量强共振";
        if ("FINAL_FALSE_HIGH_P_STILL_BET".equals(f))
            return "检查 ProbabilityEngine/DynamicGateEngine/DecisionGateEngine：falseHighPFlag 或"
                       + " P40假高P不得普通BET，只能SHADOW复核";
        if ("FINAL_K3_COVERAGE_RISK_STILL_BET".equals(f))
            return "检查 ProbabilityEngine/DecisionGateEngine：K3覆盖风险高说明三号覆盖不稳，最终BET应转SHADOW或提高门槛";
        if ("FINAL_THIRD_NUMBER_WEAK_STILL_BET".equals(f))
            return "检查 ProbabilityEngine/TeacherNumberMatrixEngine：第三号质量弱不得占K3名额，需替换或转SHADOW";
        if ("FINAL_EDGE_FALSE_STRONG_STILL_BET".equals(f))
            return "检查 DynamicGateEngine/DecisionGateEngine：Edge>=2不是强证据，无Teacher/V20/覆盖强因时必须转SHADOW";
        if ("FINAL_P40_NO_STRONG_CAUSE_STILL_BET".equals(f))
            return "检查 ProbabilityEngine/DecisionGateEngine：P40-42无Teacher/V20/Window强因属于假高P，不能普通BET";
        if ("PROBABILITY_OVER_ESTIMATED".equals(f))
            return "检查 ProbabilityEngine：FinalP需要吃Teacher弱、V20空、Window低样本、recentValidity低的动态折扣";
        if ("TEACHER_FAKE_STRONG".equals(f))
            return "检查 TeacherNumberMatrixEngine：重做STRONG/READY/WEAK/RISK/FREEZE，并加入近期有效性和连续误导降权";
        if ("TEACHER_WEAK_STILL_BET".equals(f))
            return "检查 DecisionGateEngine：Teacher弱且无V20强证据时不得普通BET，应降级SHADOW";
        if ("V20_EMPTY_STILL_BET".equals(f))
            return "检查 DynamicCauseChainEngine：候选、Decision、Trade必须落库V20状态/证据/反证/样本可信/近期有效性";
        if ("V20_FAKE_SUPPORT".equals(f))
            return "检查 DynamicCauseChainEngine：V20支撑高但反证更高时要输出falseStrongRisk并交给冻结矩阵";
        if ("WINDOW_STATE_UNTRUSTED".equals(f))
            return "检查 DynamicCauseChainEngine/ProbabilityEngine：WindowState样本不足或近期无效时折扣，不允许普通高P直接放行";
        if ("POSITION_DRAG".equals(f))
            return "检查 DynamicFreezeMatrixEngine：该位置近期拖后腿时冻结真实BET，只允许SHADOW恢复验证";
        if ("K3_COVERAGE_MISS".equals(f)) return "检查 ProbabilityEngine：位置方向不等于K3覆盖，三号排序和第三号质量需要复盘";
        if ("THIRD_NUMBER_WEAK".equals(f))
            return "检查 ProbabilityEngine/TeacherNumberMatrixEngine：第三号长期弱时不得继续占用K3名额";
        if ("NEGATIVE_EDGE_BET".equals(f))
            return "检查 DecisionGateEngine：edge为负或低于门槛候选不得通过低频救援进入真实BET";
        if ("GATE_WRONG_RELEASE".equals(f))
            return "检查 DecisionGateEngine：诊断高风险、反证高、冻结命中时必须HOLD或SHADOW";
        if ("GATE_WRONG_BLOCK".equals(f))
            return "检查 ShadowPromotionVerifierEngine：把同类SHADOW强域标记PROMOTE_WATCH，但仍不可直接BET";
        return "按失败类型检查 Probability/Teacher/V20/DynamicGate/DecisionGate 的证据、反证、样本可信和近期有效性";
    }

    private boolean isV20Empty(BetCase c, CaseDiagnosis d) {
        if (isV20WatchOrEmptyNeutral(c, d)) {
            return false;
        }
        String s = d == null ? "" : safe(d.v20Status);
        String b = c == null ? "" : safe(c.v20Bucket);
        return isEmptyBucket(s)
                || isEmptyBucket(b)
                || "空".equals(s)
                || "V20空".equals(s)
                || "V20因链空".equals(s);
    }

    private boolean isWindowUntrusted(CaseDiagnosis d) {
        if (d == null) return true;
        if (d.similarWindowSample > 0 && d.similarWindowSample < 20) return true;
        if (d.sampleReliability > 0.0d && d.sampleReliability < 0.450d) return true;
        if (d.recentValidity > 0.0d && d.recentValidity < 0.420d) return true;
        return false;
    }

    private boolean isPositionDrag(DiagnosisReport report, int position) {
        if (report == null || report.positionStats == null) return false;
        if (position <= 0 || position >= report.positionStats.length) return false;
        StatBucket b = report.positionStats[position];
        return b != null && b.total >= 5 && b.rate() < 0.300d;
    }

    private boolean isTeacherWeak(String s) {
        if (isTeacherNoSampleText(s)) return false;
        return containsAny(s, "WEAK", "弱", "偏弱", "INSUFFICIENT", "样本不足");
    }

    private boolean isTeacherRisk(String s) {
        return isTeacherHardRisk(s);
    }

    private boolean isTeacherStrong(String s) {
        return containsAny(s, "STRONG", "READY", "强", "有效", "支撑", "PASS");
    }

    private boolean isTeacherHardRisk(String s) {
        return containsAny(s, "RISK", "FREEZE", "HARD", "REJECT", "风险", "冻结", "假强");
    }

    private boolean isV20HardRiskText(String s) {
        return containsAny(s, "RISK", "FREEZE", "HARD", "REJECT", "BLOCK", "风险", "冻结");
    }

    private boolean isAdaptiveHardRiskText(String s) {
        return containsAny(s, "RISK", "FREEZE", "HARD", "REJECT", "BLOCK", "DOWNGRADE", "风险", "冻结");
    }

    private boolean isTeacherNoSampleText(String s) {
        String u = safe(s).toUpperCase(java.util.Locale.ROOT);
        if (u.length() == 0) return true;
        if (isEmptyBucket(u)) return true;
        return u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("NONE") >= 0
                || u.indexOf("EMPTY") >= 0
                || u.indexOf("UNKNOWN") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || u.indexOf("无样本") >= 0
                || u.indexOf("中性") >= 0;
    }

    private boolean isV20WatchText(String s) {
        String u = safe(s).toUpperCase(java.util.Locale.ROOT);
        if (u.length() == 0) return true;
        if (isEmptyBucket(u)) return true;
        if (isV20HardRiskText(u)) return false;
        return u.indexOf("WATCH") >= 0
                || u.indexOf("UNKNOWN") >= 0
                || u.indexOf("PENDING") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || u.indexOf("NONE") >= 0
                || u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("观察") >= 0
                || u.indexOf("中性") >= 0;
    }

    private boolean isAdaptiveWatchText(String s) {
        String u = safe(s).toUpperCase(java.util.Locale.ROOT);
        if (u.length() == 0) return true;
        if (isAdaptiveHardRiskText(u)) return false;
        return u.indexOf("WATCH") >= 0
                || u.indexOf("PENDING") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || u.indexOf("NONE") >= 0
                || u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("观察") >= 0
                || u.indexOf("中性") >= 0;
    }

    private boolean isTeacherNoSampleNeutral(BetCase c, CaseDiagnosis d) {
        if (c != null && c.teacherNoSampleNeutralFlag > 0) return true;
        String s =
                c == null
                        ? ""
                        : firstNonEmpty(
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus,
                                c.teacherStatus,
                                c.teacherBucket);
        if (s.length() == 0 && d != null) s = safe(d.teacherStatus);
        return isTeacherNoSampleText(s);
    }

    private boolean isV20WatchOrEmptyNeutral(BetCase c, CaseDiagnosis d) {
        if (c != null && c.v20WatchNeutralFlag > 0) return true;
        String s = c == null ? "" : firstNonEmpty(c.v20Status, c.dynamicStatus, c.v20Bucket);
        if (s.length() == 0 && d != null) s = safe(d.v20Status);
        return isV20WatchText(s);
    }

    private boolean isAdaptiveWatchNeutral(BetCase c) {
        if (c != null && c.adaptiveWatchNeutralFlag > 0) return true;
        String s =
                c == null
                        ? ""
                        : firstNonEmpty(
                                c.adaptiveStatus,
                                c.adaptiveAction,
                                c.adaptiveJudgeStatus,
                                c.adaptiveJudgeAction,
                                c.adaptiveBucket);
        return isAdaptiveWatchText(s);
    }

    private boolean containsAny(String text, String... arr) {
        String s = safe(text).toUpperCase(java.util.Locale.ROOT);
        if (arr == null) return false;
        for (int i = 0; i < arr.length; i++) {
            String x = arr[i];
            if (x == null || x.length() == 0) continue;
            String xu = x.toUpperCase(java.util.Locale.ROOT);
            if (s.indexOf(xu) >= 0 || safe(text).indexOf(x) >= 0) return true;
        }
        return false;
    }

    private boolean isEmptyBucket(String s) {
        String x = safe(s).trim();
        if (x.length() == 0) return true;
        return BUCKET_EMPTY.equals(x)
                || "空".equals(x)
                || "FinalP空".equals(x)
                || "无".equals(x)
                || "NONE".equalsIgnoreCase(x)
                || "NULL".equalsIgnoreCase(x);
    }

    private void addModuleRealSample(
            DiagnosisReport report, String module, boolean supported, BetCase c) {
        if (report == null || module == null || module.length() == 0 || c == null) return;
        if (report.moduleDiagnosisMap == null)
            report.moduleDiagnosisMap = new HashMap<String, ModuleDiagnosis>();
        ModuleDiagnosis m = report.moduleDiagnosisMap.get(module);
        if (m == null) {
            m = new ModuleDiagnosis();
            m.moduleName = module;
            report.moduleDiagnosisMap.put(module, m);
        }
        if (supported) m.supportCount++;
        m.betCount++;
        if (c.hit == 1 || isWinResult(c.result)) m.hitCount++;
        else m.missCount++;
        m.avgFinalP += c.finalP;
        m.realEdge += c.edge;
        m.profit += c.pnl;
    }

    private void addModuleShadowSample(
            DiagnosisReport report, String module, boolean supported, BetCase c) {
        if (report == null || module == null || module.length() == 0 || c == null) return;
        if (report.moduleDiagnosisMap == null)
            report.moduleDiagnosisMap = new HashMap<String, ModuleDiagnosis>();
        ModuleDiagnosis m = report.moduleDiagnosisMap.get(module);
        if (m == null) {
            m = new ModuleDiagnosis();
            m.moduleName = module;
            report.moduleDiagnosisMap.put(module, m);
        }
        if (supported) m.shadowSupportCount++;
        m.shadowTotal++;
        if (c.hit == 1 || isWinResult(c.result)) m.shadowWouldHit++;
        m.virtualEdge += c.edge;
    }

    private void markModuleResponsibility(
            DiagnosisReport report, String module, BetCase c, String failureType) {
        if (report == null || module == null || module.length() == 0 || "NONE".equals(module))
            return;
        if (report.moduleDiagnosisMap == null) return;
        ModuleDiagnosis m = report.moduleDiagnosisMap.get(module);
        if (m == null) return;
        if (c != null && c.hit != 1 && !isWinResult(c.result)) m.responsibleMissCount++;
        if (m.problem == null || m.problem.length() == 0) {
            m.problem = failureType;
        } else if (m.problem.indexOf(failureType) < 0) {
            m.problem = m.problem + ";" + failureType;
        }
    }

    private void finalizeModuleDiagnoses(DiagnosisReport report) {
        if (report == null) return;
        ArrayList<ModuleDiagnosis> list = new ArrayList<ModuleDiagnosis>();
        if (report.moduleDiagnosisMap != null) {
            for (String key : report.moduleDiagnosisMap.keySet()) {
                ModuleDiagnosis m = report.moduleDiagnosisMap.get(key);
                if (m == null) continue;
                if (m.betCount > 0) {
                    m.hitRate = ((double) m.hitCount) / ((double) m.betCount);
                    m.avgFinalP = m.avgFinalP / ((double) m.betCount);
                    m.realEdge = m.realEdge / ((double) m.betCount);
                }
                if (m.shadowTotal > 0) {
                    m.shadowWouldHitRate = ((double) m.shadowWouldHit) / ((double) m.shadowTotal);
                    m.virtualEdge = m.virtualEdge / ((double) m.shadowTotal);
                }
                m.reliability = reliabilityBySample(m.betCount, m.shadowTotal);
                m.recentEffectiveness = m.hitRate;
                m.effectStatus = effectStatus(m);
                m.fixAdvice = moduleFixAdvice(m);
                list.add(m);
            }
        }
        Collections.sort(
                list,
                new Comparator<ModuleDiagnosis>() {
                    public int compare(ModuleDiagnosis a, ModuleDiagnosis b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.responsibleMissCount != b.responsibleMissCount)
                            return b.responsibleMissCount - a.responsibleMissCount;
                        if (a.betCount != b.betCount) return b.betCount - a.betCount;
                        return safe(a.moduleName).compareTo(safe(b.moduleName));
                    }
                });
        report.moduleDiagnoses = list;
    }

    private double reliabilityBySample(int betCount, int shadowCount) {
        int s = betCount + shadowCount / 2;
        if (s >= 120) return 1.0d;
        if (s >= 60) return 0.80d;
        if (s >= 30) return 0.60d;
        if (s >= 10) return 0.35d;
        return 0.15d;
    }

    private String effectStatus(ModuleDiagnosis m) {
        if (m == null) return "INSUFFICIENT";
        if (m.betCount < 10) return "INSUFFICIENT";
        if (m.hitRate >= 0.380d && m.responsibleMissCount <= m.hitCount) return "EFFECTIVE";
        if (m.hitRate < 0.300d || m.responsibleMissCount >= Math.max(5, m.hitCount + 3))
            return "MISLEADING";
        if (m.shadowTotal >= 20 && m.shadowWouldHitRate > m.hitRate + 0.080d) return "CONFLICT";
        return "NEUTRAL";
    }

    private String moduleFixAdvice(ModuleDiagnosis m) {
        if (m == null) return "";
        String n = safe(m.moduleName);
        if ("ProbabilityEngine".equals(n))
            return "根据Teacher/V20/Window/Freeze/ShadowPromotion做动态折扣和小幅加权，输出finalPBefore/AfterDiagnosis";
        if ("TeacherNumberMatrixEngine".equals(n))
            return "按总样本、近期样本、真实BET前推、SHADOW反事实、连续误导重分STRONG/READY/WEAK/RISK/FREEZE";
        if ("DynamicCauseChainEngine".equals(n))
            return "每个候选、Decision、Trade落库V20证据贡献、反证贡献、样本可信、近期有效性、动作建议";
        if ("DynamicGateEngine".equals(n)) return "从频率软控改成质量风险门槛：高质量不抬门，假强聚集才局部抬门";
        if ("DecisionGateEngine".equals(n))
            return "最后接入AdaptiveJudge/Freeze/ModuleEffect/ShadowPromotion，仍保持唯一BET出口";
        if ("K3Coverage".equals(n)) return "检查K3三号排序、第三号质量、实际号覆盖失败原因";
        return "保留诊断样本，交给后续ModuleEffectScoreEngine细算";
    }

    private void analyzeMainChainNeutralCases(
            DiagnosisReport report, ArrayList<BetCase> betCases, ArrayList<BetCase> shadowCases) {
        if (report == null) return;
        report.realScopeBetCount = betCases == null ? 0 : betCases.size();
        report.shadowScopeCount = shadowCases == null ? 0 : shadowCases.size();
        report.holdScopeCount = countRecentHoldDecisions(report);
        if (betCases != null) {
            for (int i = 0; i < betCases.size(); i++) {
                BetCase c = betCases.get(i);
                if (c == null) continue;
                if (hasMainChainFields(c)) report.mainChainFieldCaseCount++;
                if (safe(c.finalDecisionRejectCode).length() > 0
                        || safe(c.mainChainRejectCode).length() > 0)
                    report.mainChainRejectCodeCount++;
                if (isTeacherNoSampleNeutral(c, null)) report.noSampleNeutralCount++;
                if (isV20WatchOrEmptyNeutral(c, null)) report.v20WatchNeutralCount++;
                if (isAdaptiveWatchNeutral(c)) report.adaptiveWatchNeutralCount++;
            }
        }
        if (shadowCases != null) {
            for (int i = 0; i < shadowCases.size(); i++) {
                BetCase c = shadowCases.get(i);
                if (c == null) continue;
                if (hasMainChainFields(c)) report.shadowMainChainFieldCaseCount++;
                if (isTeacherNoSampleNeutral(c, null)) report.shadowNoSampleNeutralCount++;
                if (isV20WatchOrEmptyNeutral(c, null)) report.shadowV20WatchNeutralCount++;
                if (isAdaptiveWatchNeutral(c)) report.shadowAdaptiveWatchNeutralCount++;
            }
        }
        report.realShadowSplitSummary =
                "真实BET="
                        + report.realScopeBetCount
                        + "，SHADOW="
                        + report.shadowScopeCount
                        + "，HOLD="
                        + report.holdScopeCount
                        + "；真实胜率/盈亏只统计REAL_TRADE+BET，SHADOW仅反事实。";
        report.mainChainSummary =
                "主链诊断字段穿透="
                        + report.mainChainFieldCaseCount
                        + "；最终拒绝码="
                        + report.mainChainRejectCodeCount
                        + "；Teacher无样本中性="
                        + report.noSampleNeutralCount
                        + "；V20观察中性="
                        + report.v20WatchNeutralCount
                        + "；Adaptive观察中性="
                        + report.adaptiveWatchNeutralCount
                        + "；SHADOW主链样本="
                        + report.shadowMainChainFieldCaseCount;
    }

    private int countRecentHoldDecisions(DiagnosisReport report) {
        return 0;
    }

    private boolean hasMainChainFields(BetCase c) {
        if (c == null) return false;
        if (safe(c.finalDecisionStatus).length() > 0) return true;
        if (safe(c.fieldChainVersion).length() > 0) return true;
        if (safe(c.decisionGateVersion).length() > 0) return true;
        if (c.finalDecisionQualityScore > 0.0d || c.finalDecisionRiskScore > 0.0d) return true;
        if (c.positionNeedScore > 0.0d || c.numberSupplyScore > 0.0d) return true;
        if (c.coverageQuality > 0.0d || c.thirdQuality > 0.0d) return true;
        if (c.ruleDecayRisk > 0.0d || c.thirdDragRisk > 0.0d || c.counterfactualRisk > 0.0d)
            return true;
        return false;
    }

    private void fillMainChainCaseDiagnosis(CaseDiagnosis d, BetCase c) {
        if (d == null || c == null) return;
        d.finalDecisionStatus = c.finalDecisionStatus;
        d.finalDecisionQualityScore = c.finalDecisionQualityScore;
        d.finalDecisionRiskScore = c.finalDecisionRiskScore;
        d.finalDecisionActionAdvice = c.finalDecisionActionAdvice;
        d.adaptiveStatus = firstNonEmpty(c.adaptiveStatus, c.adaptiveJudgeStatus);
        d.adaptiveAction = firstNonEmpty(c.adaptiveAction, c.adaptiveJudgeAction);
        d.shadowPromotionStatus = c.shadowPromotionStatus;
        d.shadowPromoteScore = c.shadowPromoteScore;
        d.ruleDecayRisk = c.ruleDecayRisk;
        d.thirdDragRisk = c.thirdDragRisk;
        d.counterfactualRisk = c.counterfactualRisk;
        d.positionNeedScore = c.positionNeedScore;
        d.numberSupplyScore = c.numberSupplyScore;
        d.coverageQuality = firstPositive(c.coverageQuality, c.coverageQualityScore, 0.0d);
        d.thirdQuality = firstPositive(c.thirdQuality, c.thirdNumberQualityScore, 0.0d);
        d.netEvidenceScore = c.netEvidenceScore;
        d.fieldChainVersion = c.fieldChainVersion;
        d.decisionGateVersion = c.decisionGateVersion;
        d.noSampleNeutral = isTeacherNoSampleNeutral(c, d) ? 1 : 0;
        d.v20WatchNeutral = isV20WatchOrEmptyNeutral(c, d) ? 1 : 0;
        d.adaptiveWatchNeutral = isAdaptiveWatchNeutral(c) ? 1 : 0;
    }

    private void fillMainChainShadowDiagnosis(ShadowCaseDiagnosis d, BetCase c) {
        if (d == null || c == null) return;
        d.finalDecisionStatus = c.finalDecisionStatus;
        d.finalDecisionQualityScore = c.finalDecisionQualityScore;
        d.finalDecisionRiskScore = c.finalDecisionRiskScore;
        d.adaptiveStatus = firstNonEmpty(c.adaptiveStatus, c.adaptiveJudgeStatus);
        d.shadowPromotionStatus = c.shadowPromotionStatus;
        d.shadowPromoteScore = c.shadowPromoteScore;
        d.ruleDecayRisk = c.ruleDecayRisk;
        d.thirdDragRisk = c.thirdDragRisk;
        d.counterfactualRisk = c.counterfactualRisk;
        d.positionNeedScore = c.positionNeedScore;
        d.numberSupplyScore = c.numberSupplyScore;
        d.coverageQuality = firstPositive(c.coverageQuality, c.coverageQualityScore, 0.0d);
        d.thirdQuality = firstPositive(c.thirdQuality, c.thirdNumberQualityScore, 0.0d);
        d.fieldChainVersion = c.fieldChainVersion;
    }

    private void loadBackgroundServiceEvidenceState(DbStore store, DiagnosisReport r) {
        if (r == null) return;
        try {
            Models.ServiceRuntimeState svc = ServiceHeartbeatStore.peekLastSnapshot();
            r.serviceHeartbeatAgeMs = svc.heartbeatAgeMs;
            r.serviceUserEnabled = svc.userEnabled;
            r.serviceRestartCount = svc.restartCount;

            Models.TeacherSliceState warm =
                    store == null
                            ? new Models.TeacherSliceState()
                            : store.loadTeacherSliceState("WARM");
            Models.TeacherSliceState cold =
                    store == null
                            ? new Models.TeacherSliceState()
                            : store.loadTeacherSliceState("COLD");
            Models.TeacherSliceState deep =
                    store == null
                            ? new Models.TeacherSliceState()
                            : store.loadTeacherSliceState("DEEP");
            r.teacherWarmSlice =
                    safe(warm.status)
                            + ":"
                            + warm.processed
                            + "/"
                            + warm.required
                            + " cursor="
                            + safe(warm.cursorIssue);
            r.teacherColdSlice =
                    safe(cold.status)
                            + ":"
                            + cold.processed
                            + "/"
                            + cold.required
                            + " cursor="
                            + safe(cold.cursorIssue);
            r.teacherDeepSlice =
                    safe(deep.status)
                            + ":"
                            + deep.processed
                            + "/"
                            + deep.required
                            + " cursor="
                            + safe(deep.cursorIssue);

            String pumpStatus = getStateSafe(store, "background_evidence_pump_status", "NONE");
            String pumpCost = getStateSafe(store, "background_evidence_pump_cost_ms", "--");
            long pumpAt =
                    parseLongSafe(getStateSafe(store, "background_evidence_pump_last_at", "0"));
            long pumpAge =
                    pumpAt <= 0L
                            ? Long.MAX_VALUE
                            : Math.max(0L, System.currentTimeMillis() - pumpAt);
            String pumpFresh =
                    pumpAge == Long.MAX_VALUE
                            ? "NO_TIME"
                            : (pumpAge > 5L * 60L * 1000L ? "STALE" : "FRESH");

            String distribution =
                    "Pdist="
                            + getStateSafe(store, "probability_distribution_lt30", "0")
                            + "/"
                            + getStateSafe(store, "probability_distribution_30_34", "0")
                            + "/"
                            + getStateSafe(store, "probability_distribution_34_36", "0")
                            + "/"
                            + getStateSafe(store, "probability_distribution_36_38", "0")
                            + "/"
                            + getStateSafe(store, "probability_distribution_ge38", "0")
                            + " min="
                            + getStateSafe(store, "probability_distribution_min", "0")
                            + " max="
                            + getStateSafe(store, "probability_distribution_max", "0")
                            + " avg="
                            + getStateSafe(store, "probability_distribution_avg", "0");

            r.backgroundServiceSummary =
                    "后台真实运行="
                            + ServiceHeartbeatStore.peekLastDetailedSummary()
                            + "｜TypedTeacher="
                            + safe(
                                    store == null
                                            ? ""
                                            : store.getState(
                                                    AppConfig.STATE_TYPED_TEACHER_STATUS, ""))
                            + "｜PromotionMatched="
                            + safe(
                                    store == null
                                            ? ""
                                            : store.getState(
                                                    AppConfig.STATE_PROMOTION_MATCH_COUNT, "0"))
                            + "｜AdaptiveInput="
                            + safe(
                                    store == null
                                            ? ""
                                            : store.getState(
                                                    AppConfig.STATE_ADAPTIVE_INPUT_COUNT, "0"))
                            + "｜SCHEDULED不等于TRIGGERED，TRIGGERED不等于SERVICE_STARTED，SERVICE_STARTED不等于FETCH_SUCCEEDED"
                            + "｜pump="
                            + pumpStatus
                            + "/"
                            + pumpCost
                            + "ms/"
                            + pumpFresh
                            + " age="
                            + (pumpAge == Long.MAX_VALUE ? "--" : String.valueOf(pumpAge) + "ms")
                            + "｜WARM="
                            + safe(r.teacherWarmSlice)
                            + "｜COLD="
                            + safe(r.teacherColdSlice)
                            + "｜DEEP="
                            + safe(r.teacherDeepSlice)
                            + "｜TeacherLast="
                            + getStateSafe(store, "teacher_warm_last_slice_status", "--")
                            + "/"
                            + getStateSafe(store, "teacher_warm_last_slice_processed", "0")
                            + "/"
                            + getStateSafe(store, "teacher_warm_last_slice_cursor", "")
                            + "｜Reserved="
                            + getStateSafe(store, AppConfig.STATE_TEACHER_RESERVED_SLICE, "--")
                            + "｜PreDraw="
                            + getStateSafe(store, "predraw_feature_state_complete", "--")
                            + "｜FullScanPreDraw="
                            + getStateSafe(store, "full_scan_predraw_state_complete", "--")
                            + "｜Cohort="
                            + getStateSafe(
                                    store, "probability_active_cohort", AppConfig.ACTIVE_FIX_COHORT)
                            + "｜Pipeline="
                            + getStateSafe(
                                    store,
                                    "probability_active_pipeline",
                                    AppConfig.ACTIVE_PROBABILITY_PIPELINE_VERSION)
                            + "｜"
                            + distribution
                            + "｜未归因="
                            + getStateSafe(store, AppConfig.STATE_DECISION_UNATTRIBUTED_MS, "0")
                            + "ms"
                            + "｜端到端=service@"
                            + ageOfState(store, "last_service_started_at")
                            + "/taskRemoved@"
                            + ageOfState(store, "last_task_removed_at")
                            + "/fetch@"
                            + ageOfState(store, "last_fetch_success_at")
                            + "/analysis@"
                            + ageOfState(store, "last_analysis_success_at")
                            + "/settlement@"
                            + ageOfState(store, "last_settlement_success_at")
                            + "｜TeacherIssueFresh="
                            + getStateSafe(store, "teacher_snapshot_fresh_by_issue", "--")
                            + "/lag="
                            + getStateSafe(store, "teacher_snapshot_issue_lag", "--")
                            + "｜业务健康="
                            + getStateSafe(store, "background_business_health", "--");
        } catch (Throwable e) {
            r.backgroundServiceSummary = "后台服务诊断异常：" + e.getClass().getSimpleName();
        }
    }

    private long parseLongSafe(String v) {
        try {
            return Long.parseLong(v == null ? "0" : v.trim());
        } catch (Throwable e) {
            return 0L;
        }
    }

    private int parseIntSafe(String v) {
        try {
            return Integer.parseInt(v);
        } catch (Throwable e) {
            return 0;
        }
    }

    private void loadLinkModificationV2State(DbStore store, DiagnosisReport r) {
        if (store == null || r == null) return;
        try {
            Models.FrequencyState fs = store.loadFrequencyState();
            if (fs != null) {
                r.realFrequency50 = fs.frequency50;
                r.realFrequency100 = fs.frequency100;
                r.realFrequency300 = fs.frequency300;
                r.frequencyDebt = fs.frequencyDebt;
                r.frequencyState = safe(fs.status);
                r.qualifiedOpportunityCount = fs.qualifiedOpportunityCount;
                r.frequencyFloorRequiredCount = fs.requiredFloorBetCount;
                r.frequencyFloorTargetCount = fs.targetBetCount;
                r.frequencyFloorActualCount = fs.realBetCount;
                r.frequencyFloorDebtCount = fs.floorDebtCount;
                r.frequencyTargetDebtCount = fs.targetDebtCount;
                r.frequencyNoUpperCap = fs.noUpperCap;
                r.recentProbeCount = fs.recentProbeCount;
                r.recentLoseCount = fs.recentLoseCount;
            }
        } catch (Throwable e) {
        }
        try {
            r.probeFrequency = store.getRecentProbeFrequency(AppConfig.FREQ_LOOKBACK_ISSUES);
            r.qualifiedOpportunityCount =
                    Math.max(
                            r.qualifiedOpportunityCount,
                            store.getRecentQualifiedOpportunityCount(
                                    AppConfig.FREQ_LOOKBACK_ISSUES));
        } catch (Throwable e) {
        }
        for (int i = 0; i < r.betCases.size(); i++) {
            BetCase c = r.betCases.get(i);
            if (c == null) continue;
            String domain = safe(c.finalDecisionBetDomain);
            if (domain.indexOf(AppConfig.BET_DOMAIN_FREQUENCY_FLOOR) >= 0) {
                r.frequencyFloorBetCount++;
            } else if (domain.indexOf(AppConfig.BET_DOMAIN_FREQ_RECOVERY) >= 0) {
                r.frequencyRecoveryBetCount++;
            } else if (domain.indexOf(AppConfig.BET_DOMAIN_REAL_BET_PROBE) >= 0) {
                r.realProbeBetCount++;
            } else {
                r.normalBetCount++;
            }
        }
        try {
            List<Models.ShadowDomainStat> domains = store.getShadowPromotionDomainStats("", 240);
            if (domains != null) {
                for (int i = 0; i < domains.size(); i++) {
                    Models.ShadowDomainStat d = domains.get(i);
                    if (d == null) continue;
                    String st = safe(d.promotionStatus);
                    if (AppConfig.PROMOTION_STRONG.equals(st)) r.promotionStrongDomains++;
                    else if (AppConfig.PROMOTION_READY.equals(st)) r.promotionReadyDomains++;
                    else if (AppConfig.PROMOTION_PROMOTE_WATCH.equals(st))
                        r.promotionWatchDomains++;
                    else if (AppConfig.PROMOTION_BLOCK.equals(st)) r.promotionBlockDomains++;
                    if (d.domainKey != null && d.domainKey.indexOf("P36_38") >= 0) {
                        r.p36To38DomainSample += Math.max(0, d.sampleCount);
                        r.p36To38DomainHit += Math.max(0, d.hitCount);
                    }
                }
            }
        } catch (Throwable e) {
        }
        try {
            Models.LayerSnapshot warm = store.loadTeacherWarmSnapshot();
            Models.LayerSnapshot cold = store.loadTeacherColdSnapshot();
            Models.LayerSnapshot deep = store.loadTeacherDeepSnapshot();
            StringBuilder sb = new StringBuilder();
            appendSnapshotFreshness(sb, "WARM", warm);
            appendSnapshotFreshness(sb, "COLD", cold);
            appendSnapshotFreshness(sb, "DEEP", deep);
            r.snapshotFreshness = sb.toString();
        } catch (Throwable e) {
            r.snapshotFreshness = "SNAPSHOT_READ_ERROR";
        }
        String cost = safeStateDirect(store, "analysis_cost_summary_v3");
        if (cost.length() == 0) cost = safeStateDirect(store, "analysis_cost_summary");
        String decisionCost = safeStateDirect(store, "realtime_layered_decision_cost_ms");
        String adaptiveCost = safeStateDirect(store, "adaptive_judge_elapsed_ms");
        String ruleCost = safeStateDirect(store, "rule_health_realtime_elapsed_ms");
        r.performanceSummary =
                "Decision="
                        + emptyDash(decisionCost)
                        + "ms"
                        + "｜Adaptive="
                        + emptyDash(adaptiveCost)
                        + "ms"
                        + "｜RuleHealth="
                        + emptyDash(ruleCost)
                        + "ms"
                        + "｜阶段="
                        + emptyDash(cost);
    }

    private void appendSnapshotFreshness(StringBuilder sb, String name, Models.LayerSnapshot x) {
        if (sb == null) return;
        if (sb.length() > 0) sb.append("｜");
        if (x == null) {
            sb.append(name).append("=NO_SAMPLE");
            return;
        }
        sb.append(name)
                .append("=")
                .append(safe(x.freshnessStatus))
                .append("@")
                .append(safe(x.asOfIssue))
                .append("/max=")
                .append(safe(x.maxIncludedIssue));
    }

    private String safeStateDirect(DbStore store, String key) {
        if (store == null) return "";
        try {
            return safe(store.getState(key, ""));
        } catch (Throwable e) {
            return "";
        }
    }

    private String emptyDash(String s) {
        return s == null || s.length() == 0 ? "--" : s;
    }

    private void analyzeRealtimeValidityFixState(DbStore store, DiagnosisReport r) {
        if (store == null || r == null) return;
        r.buildIdentityStatus = getStateSafe(store, BuildIdentity.STATE_STATUS, "--");
        r.buildIdentityDetail = getStateSafe(store, BuildIdentity.STATE_DETAIL, "--");
        r.softHoldShadowRecovery =
                getStateSafe(store, "soft_hold_shadow_recovery", "--")
                        + "@"
                        + getStateSafe(store, "soft_hold_shadow_recovery_stage", "--")
                        + "/"
                        + getStateSafe(store, "soft_hold_shadow_recovery_original_block", "--");
        r.coldStartNeutralRestore = getStateSafe(store, "cold_start_neutral_restore_last", "--");
        r.teacherReservedSlice = getStateSafe(store, AppConfig.STATE_TEACHER_RESERVED_SLICE, "--");
        r.teacherWarmSlice =
                getStateSafe(store, "teacher_warm_last_slice_status", "--")
                        + "/"
                        + getStateSafe(store, "teacher_warm_last_slice_processed", "0")
                        + "/cursor="
                        + getStateSafe(store, "teacher_warm_last_slice_cursor", "--");
        try {
            r.unsafePromotionDomainCount = store.countUnsafePromotionDomains();
        } catch (Throwable e) {
            r.unsafePromotionDomainCount = 0;
        }
        int total = 0;
        int cover = 0;
        int third = 0;
        int domain = 0;
        int reject = 0;
        try {
            List<Models.Decision> shadows = store.getRecentShadowDecisionsForDiagnosis(100);
            if (shadows != null) {
                for (int i = 0; i < shadows.size(); i++) {
                    Models.Decision d = shadows.get(i);
                    if (d == null) continue;
                    total++;
                    if (d.coverageAvailable == 1
                            || d.coverageQuality > 0.0d
                            || d.coverageQualityScore > 0.0d) cover++;
                    if (d.thirdQualityAvailable == 1
                            || d.thirdQuality > 0.0d
                            || d.thirdNumberQualityScore > 0.0d) third++;
                    if (AppConfig.sanitizePromotionDomainKey(d.shadowPromotionDomain).length() > 0)
                        domain++;
                    String rc = firstNonEmpty(d.finalDecisionRejectCode, d.blockCode);
                    if (rc.length() > 0) reject++;
                }
            }
        } catch (Throwable e) {
        }
        r.shadowCompletenessSample = total;
        r.shadowCoverageComplete = cover;
        r.shadowThirdComplete = third;
        r.shadowDomainComplete = domain;
        r.shadowRejectComplete = reject;
        r.shadowCoverageCompleteRate = total <= 0 ? 0.0d : ((double) cover) / ((double) total);
        r.shadowThirdCompleteRate = total <= 0 ? 0.0d : ((double) third) / ((double) total);
        r.shadowDomainCompleteRate = total <= 0 ? 0.0d : ((double) domain) / ((double) total);
        r.shadowRejectCompleteRate = total <= 0 ? 0.0d : ((double) reject) / ((double) total);
        String issue = safeStateDirect(store, "analysis_cost_issue_v3");
        String stage = safeStateDirect(store, "analysis_cost_summary_v3");
        String totalCost = safeStateDirect(store, "analysis_cost_total_ms_v3");
        r.performanceAuditSummary =
                "issue="
                        + issue
                        + "｜stageSum(excludeTotal)="
                        + emptyDash(totalCost)
                        + "ms｜"
                        + emptyDash(stage)
                        + "｜probLayer="
                        + getStateSafe(store, "probability_audit_before_teacher", "--")
                        + ">"
                        + getStateSafe(store, "probability_audit_after_teacher", "--")
                        + ">"
                        + getStateSafe(store, "probability_audit_after_risk", "--")
                        + ">"
                        + getStateSafe(store, "probability_audit_final", "--")
                        + "｜probCost="
                        + getStateSafe(store, "probability_audit_cost_ms", "--")
                        + "ms";
    }

    private void loadV23EvidenceState(DbStore store, DiagnosisReport r) {
        if (r == null) return;
        r.v22ExpertConsensus = "--";
        r.v22ExpertHealth = "--";
        r.v23CausalProbability = "--";
        r.v23MicroWalkForward = "--";
        r.v23ProbabilityOwner = "--";
        r.v24Probability = "--";
        r.v24ProbabilityPolicy = "--";
        r.v24MicroWalkForward = "--";
        r.v24Authorization = "--";
        r.v24OosState = "--";
        if (store == null) return;
        try {
            r.v22ExpertConsensus = getStateSafe(store, "adaptive_consensus_best", "LEGACY_SKIPPED");
            r.v22ExpertHealth = getStateSafe(store, "adaptive_expert_health_summary", "--");
            r.v23CausalProbability =
                    getStateSafe(store, "v23_realtime_probability_best", "LEGACY_NOT_USED");
            r.v23MicroWalkForward =
                    getStateSafe(
                            store, RollingCausalBacktestEngine.STATE_SUMMARY, "LEGACY_NOT_USED");
            r.v23ProbabilityOwner = getStateSafe(store, "active_realtime_probability_owner", "--");
            r.v24Probability =
                    firstNonEmpty(
                            getStateSafe(store, "v41_realtime_probability_best", ""),
                            getStateSafe(store, "v40_realtime_probability_best", ""),
                            getStateSafe(store, "v24_realtime_probability_best", "--"));
            r.v24ProbabilityPolicy =
                    firstNonEmpty(
                            getStateSafe(store, "v41_realtime_probability_policy", ""),
                            getStateSafe(store, "v40_realtime_probability_policy", ""),
                            getStateSafe(store, "v24_realtime_probability_policy", "--"));
            r.v24MicroWalkForward =
                    getStateSafe(store, MicroWalkForwardEngine.STATE_LAST_SUMMARY, "--")
                            + "｜pump="
                            + getStateSafe(store, "v26_micro_pump_state", "--")
                            + "｜pumpSummary="
                            + getStateSafe(store, "v26_micro_pump_summary", "--");
            r.v24Authorization =
                    "mode="
                            + firstNonEmpty(
                                    getStateSafe(store, "v40_authorization_last_mode", ""),
                                    getStateSafe(store, "v24_authorization_last_mode", "--"))
                            + " reject="
                            + firstNonEmpty(
                                    getStateSafe(store, "v40_authorization_last_reject", ""),
                                    getStateSafe(store, "v24_authorization_last_reject", "--"))
                            + " P="
                            + firstNonEmpty(
                                    getStateSafe(store, "v40_authorization_last_probability", ""),
                                    getStateSafe(store, "v24_authorization_last_probability", "--"))
                            + " Gate="
                            + firstNonEmpty(
                                    getStateSafe(store, "v40_authorization_last_gate", ""),
                                    getStateSafe(store, "v24_authorization_last_gate", "--"))
                            + " Edge="
                            + firstNonEmpty(
                                    getStateSafe(store, "v40_authorization_last_edge", ""),
                                    getStateSafe(store, "v24_authorization_last_edge", "--"))
                            + " Utility="
                            + getStateSafe(store, "v40_authorization_last_utility", "--")
                            + " Uncertainty="
                            + getStateSafe(store, "v40_authorization_last_uncertainty", "--")
                            + " Structure="
                            + getStateSafe(store, "v40_authorization_last_structure", "--")
                            + " Lane="
                            + getStateSafe(store, "v40_authorization_last_lane", "--")
                            + " decisionId="
                            + firstNonEmpty(
                                    getStateSafe(store, "v40_authorization_last_decision_id", ""),
                                    getStateSafe(
                                            store, "v24_authorization_last_decision_id", "--"));
            r.v24OosState =
                    "settledIssue="
                            + getStateSafe(store, "oos_last_settled_issue", "--")
                            + " settledCount="
                            + getStateSafe(store, "oos_last_settled_count", "0")
                            + " published="
                            + getStateSafe(store, MicroWalkForwardEngine.STATE_SAMPLE, "0")
                            + "｜LiveFunnel=checked:"
                            + getStateSafe(store, "v40_decision_oos_checked", "0")
                            + "/eligible:"
                            + getStateSafe(store, "v40_decision_oos_eligible", "0")
                            + "/firstReject:"
                            + getStateSafe(store, "v40_decision_oos_first_reject", "--")
                            + "/all:"
                            + getStateSafe(store, "v41_decision_oos_reject_funnel", "{}")
                            + "｜Budget="
                            + getStateSafe(store, "v41_live_action_budget_summary", "--");
            r.v24UnauthorizedBetCount =
                    scalarCount(
                            store,
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_TRADES
                                    + " WHERE action='BET' AND stat_scope='REAL_TRADE' AND"
                                    + " (authorization_valid<>1 OR authorized_mode<>'BET' OR"
                                    + " authorization_id IS NULL OR authorization_id='')");
            r.v24WatchBetCount =
                    scalarCount(
                            store,
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_TRADES
                                    + " WHERE action='BET' AND stat_scope='REAL_TRADE' AND"
                                    + " (UPPER(COALESCE(teacher_policy,'')) LIKE '%WATCH%' OR"
                                    + " UPPER(COALESCE(dynamic_policy,'')) LIKE '%WATCH%' OR"
                                    + " UPPER(COALESCE(adaptive_policy,'')) LIKE '%WATCH%')");
            r.v24HardRiskBetCount =
                    scalarCount(
                            store,
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_TRADES
                                    + " WHERE action='BET' AND stat_scope='REAL_TRADE' AND"
                                    + " (UPPER(COALESCE(teacher_policy,'')) LIKE '%HARD_RISK%' OR"
                                    + " UPPER(COALESCE(dynamic_policy,'')) LIKE '%HARD_RISK%' OR"
                                    + " UPPER(COALESCE(adaptive_policy,'')) LIKE '%HARD_RISK%')");
            r.v24VersionMismatchBetCount =
                    scalarCount(
                            store,
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_TRADES
                                    + " WHERE action='BET' AND stat_scope='REAL_TRADE' AND"
                                    + " (pipeline_version IS NULL OR pipeline_version<>'"
                                    + AppConfig.PIPELINE_VERSION
                                    + "' OR calibration_version IS NULL OR calibration_version<>'"
                                    + AppConfig.CALIBRATION_VERSION
                                    + "')");
        } catch (Throwable ignore) {
        }
    }

    private int scalarCount(DbStore store, String sql) {
        Cursor c = null;
        try {
            if (store == null || sql == null || sql.length() == 0) return 0;
            c = store.readable().rawQuery(sql, null);
            return c != null && c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
    }

    private void buildDiagnosisText(DiagnosisReport r) {
        if (r == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【V20真实BET复盘诊断】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("诊断编译补丁=").append(AIDE_SCOPE_FIX_PATCH).append("\n");
        sb.append("口径=只诊断 REAL_TRADE + BET + 已结算 WIN/MISS；SHADOW只做反事实，不计真实盈亏。\n\n");
        sb.append("零、Runtime-first真实性\n")
                .append(safe(r.runtimeFirstSummary))
                .append("\nREAL授权=")
                .append(safe(r.realAuthorizationState))
                .append("；requiredRealBet=")
                .append(r.requiredRealBet)
                .append("；debt=")
                .append(r.realBetDebt)
                .append("；MODEL_PROVEN=")
                .append(r.productionModelProven ? "true" : "false")
                .append("\n\n");
        sb.append("一、真实BET总体\n");
        sb.append("最近真实BET：出手=")
                .append(r.totalBet)
                .append("，命中=")
                .append(r.hitBet)
                .append("，未中=")
                .append(r.missBet)
                .append("，胜率=")
                .append(pct(r.winRate))
                .append("，盈亏=")
                .append(money(r.pnl))
                .append("，等待=")
                .append(r.waitBet)
                .append("，数据异常=")
                .append(r.dataErrorBet)
                .append("\n");
        sb.append("平均FinalP=")
                .append(pct(r.avgFinalP))
                .append("，平均Gate=")
                .append(pct(r.avgGate))
                .append("，平均Edge=")
                .append(signedPct(r.avgEdge))
                .append("\n");
        sb.append("负Edge出手=")
                .append(r.negativeEdgeBet)
                .append("，TeacherRisk出手=")
                .append(r.teacherRiskBet)
                .append("，V20风险出手=")
                .append(r.v20RiskBet)
                .append("\n\n");
        sb.append("一-补、频率债务与合格探针\n");
        sb.append("真实频率50/100/300=")
                .append(pct(r.realFrequency50))
                .append("/")
                .append(pct(r.realFrequency100))
                .append("/")
                .append(pct(r.realFrequency300))
                .append("，目标=")
                .append(pct(AppConfig.TARGET_FREQ_MIN))
                .append("/")
                .append(pct(AppConfig.TARGET_FREQ_IDEAL))
                .append("/")
                .append(pct(AppConfig.TARGET_FREQ_SOFT_HIGH))
                .append("\n");
        sb.append("频率状态=")
                .append(safe(r.frequencyState))
                .append("，债务=")
                .append(pct(r.frequencyDebt))
                .append("，合格机会=")
                .append(r.qualifiedOpportunityCount)
                .append("，最近探针=")
                .append(r.recentProbeCount)
                .append("，最近挂=")
                .append(r.recentLoseCount)
                .append("，探针频率=")
                .append(pct(r.probeFrequency))
                .append("\n");
        sb.append("频率硬下限=")
                .append(pct(AppConfig.REAL_BET_HARD_MIN_FREQUENCY))
                .append("，目标=")
                .append(pct(AppConfig.REAL_BET_TARGET_FREQUENCY))
                .append("，上限=")
                .append(AppConfig.REAL_BET_FREQUENCY_HAS_UPPER_CAP ? "启用" : "NONE")
                .append("，应有BET=")
                .append(r.frequencyFloorRequiredCount)
                .append("，目标BET=")
                .append(r.frequencyFloorTargetCount)
                .append("，实际BET=")
                .append(r.frequencyFloorActualCount)
                .append("，硬下限债务=")
                .append(r.frequencyFloorDebtCount)
                .append("，目标债务=")
                .append(r.frequencyTargetDebtCount)
                .append("\n");
        if (r.totalBet == 0 && r.shadowTotal >= 120) {
            sb.append("V42零出手诊断：SHADOW已积累")
                    .append(r.shadowTotal)
                    .append("单但真实BET仍为0；V42已启用唯一Lane合同、四类Regime、关联图、反事实蒸馏与风险覆盖率控制；继续检查下方统一动作漏斗。\n");
        }
        sb.append("BET域：NORMAL=")
                .append(r.normalBetCount)
                .append("，FREQ_RECOVERY=")
                .append(r.frequencyRecoveryBetCount)
                .append("，REAL_PROBE=")
                .append(r.realProbeBetCount)
                .append("，FREQUENCY_FLOOR=")
                .append(r.frequencyFloorBetCount)
                .append("\n");
        sb.append("晋级域：PROMOTE_WATCH=")
                .append(r.promotionWatchDomains)
                .append("，READY=")
                .append(r.promotionReadyDomains)
                .append("，STRONG=")
                .append(r.promotionStrongDomains)
                .append("，BLOCK=")
                .append(r.promotionBlockDomains)
                .append("；P36-38域=")
                .append(r.p36To38DomainSample)
                .append("单/中")
                .append(r.p36To38DomainHit)
                .append("/率")
                .append(pct(safeRate(r.p36To38DomainHit, r.p36To38DomainSample)))
                .append("\n");
        sb.append("快照=").append(safe(r.snapshotFreshness)).append("\n");
        sb.append("性能=").append(safe(r.performanceSummary)).append("\n");
        sb.append("性能对账=").append(safe(r.performanceAuditSummary)).append("\n");
        sb.append("后台恢复=").append(safe(r.backgroundServiceSummary)).append("\n");
        sb.append("V23实时概率=").append(safe(r.v23CausalProbability)).append("\n");
        sb.append("V23微型走前=").append(safe(r.v23MicroWalkForward)).append("\n");
        sb.append("V23概率唯一Owner=").append(safe(r.v23ProbabilityOwner)).append("\n");
        sb.append("V41排序/分布模型/校准概率=").append(safe(r.v24Probability)).append("\n");
        sb.append("V41概率纪律=").append(safe(r.v24ProbabilityPolicy)).append("\n");
        sb.append("V41 Top-K微型走前=").append(safe(r.v24MicroWalkForward)).append("\n");
        sb.append("V41最后授权=").append(safe(r.v24Authorization)).append("\n");
        sb.append("V41 OOS状态=").append(safe(r.v24OosState)).append("\n");
        sb.append("V41可靠性/Top-K/动作漏斗=").append(safe(r.v28OosReliabilityAudit)).append("\n");
        sb.append("\n一-补2、V48单动作Champion、最终40闭环、胜率未达标责任与稳定性根因\n");
        sb.append(safe(r.v44DecisionFunnel)).append("\n");
        sb.append("V42风险覆盖率控制=").append(safe(r.v42RiskCoverage)).append("\n");
        sb.append("V42唯一Lane合同=").append(safe(r.v42LanePolicy)).append("\n");
        sb.append("V42机会漏斗=").append(safe(r.v42OpportunityFunnel)).append("\n");
        sb.append("V42行动特征=").append(safe(r.v42ActionUtility)).append("\n");
        sb.append("V41授权穿透审计：未授权BET=")
                .append(r.v24UnauthorizedBetCount)
                .append("，WATCH BET=")
                .append(r.v24WatchBetCount)
                .append("，HARD_RISK BET=")
                .append(r.v24HardRiskBetCount)
                .append("，版本错配BET=")
                .append(r.v24VersionMismatchBetCount)
                .append(
                        (r.v24UnauthorizedBetCount
                                                + r.v24WatchBetCount
                                                + r.v24HardRiskBetCount
                                                + r.v24VersionMismatchBetCount)
                                        == 0
                                ? "（通过或暂无新样本）"
                                : "（未通过，必须清库或排查链路）")
                .append("\n");
        sb.append("旧V22共识(后台兼容)=").append(safe(r.v22ExpertConsensus)).append("\n");
        sb.append("专家健康=").append(safe(r.v22ExpertHealth)).append("\n");
        sb.append("安装包身份=")
                .append(safe(r.buildIdentityStatus))
                .append("｜")
                .append(safe(r.buildIdentityDetail))
                .append("\n");
        sb.append("软HOLD→SHADOW回退=").append(safe(r.softHoldShadowRecovery)).append("\n");
        sb.append("冷启动中性恢复=").append(safe(r.coldStartNeutralRestore)).append("\n");
        sb.append("Teacher预留片=")
                .append(safe(r.teacherReservedSlice))
                .append("｜WARM=")
                .append(safe(r.teacherWarmSlice))
                .append("\n");
        sb.append("新SHADOW字段完整性：样本=")
                .append(r.shadowCompletenessSample)
                .append(" Cover=")
                .append(pct(r.shadowCoverageCompleteRate))
                .append(" Third=")
                .append(pct(r.shadowThirdCompleteRate))
                .append(" Domain=")
                .append(pct(r.shadowDomainCompleteRate))
                .append(" RejectCode=")
                .append(pct(r.shadowRejectCompleteRate))
                .append("\n");
        sb.append("Promotion结果泄漏域=")
                .append(r.unsafePromotionDomainCount)
                .append(r.unsafePromotionDomainCount == 0 ? "（通过）" : "（必须清理，禁止进入DecisionGate）")
                .append("\n\n");
        sb.append("二、位置诊断\n");
        appendArrayStats(sb, r.positionStats, 1, POSITION_COUNT, 10);
        appendBestWorst(sb, "位置", r.bestPosition, r.worstPosition);
        sb.append("\n");
        sb.append("三、概率/门槛诊断\n");
        appendMapStats(sb, r.finalPStats, 8);
        appendBestWorst(sb, "FinalP分桶", r.bestFinalPBucket, r.worstFinalPBucket);
        sb.append("Edge分桶：\n");
        appendMapStats(sb, r.edgeStats, 8);
        sb.append("门槛反事实：BET如果筛选 FinalP>=36% ")
                .append(r.betP36.shortText())
                .append("；FinalP>=38% ")
                .append(r.betP38.shortText())
                .append("；FinalP>=40% ")
                .append(r.betP40.shortText())
                .append("\n");
        sb.append("Edge反事实：Edge>=0 ")
                .append(r.betEdge0.shortText())
                .append("；Edge>=1% ")
                .append(r.betEdge1.shortText())
                .append("；Edge>=2% ")
                .append(r.betEdge2.shortText())
                .append("\n\n");
        sb.append("三-补、最终BET闭环字段诊断\n");
        sb.append("Teacher弱仍BET=")
                .append(r.teacherWeakNoBetStillBet)
                .append("，假高P仍BET=")
                .append(r.falseHighPStillBet)
                .append("，K3覆盖风险仍BET=")
                .append(r.k3CoverageRiskStillBet)
                .append("，第三号弱仍BET=")
                .append(r.thirdNumberWeakStillBet)
                .append("，Edge虚强仍BET=")
                .append(r.edgeFalseStrongStillBet)
                .append("，P40无强因仍BET=")
                .append(r.p40NoStrongCauseStillBet)
                .append("，RejectCode仍BET=")
                .append(r.finalRejectCodeStillBet)
                .append("\n");
        sb.append("优先域真实BET=")
                .append(r.preferredDomainBet)
                .append("，命中=")
                .append(r.preferredDomainHit)
                .append("，胜率=")
                .append(pct(safeRate(r.preferredDomainHit, r.preferredDomainBet)))
                .append("\n");
        sb.append("覆盖质量分桶：\n");
        appendMapStats(sb, r.coverageStats, 10);
        sb.append("最终RejectCode分桶：\n");
        appendMapStats(sb, r.finalRejectStats, 10);
        sb.append("最终BET域分桶：\n");
        appendMapStats(sb, r.finalBetDomainStats, 10);
        sb.append("Edge策略分桶：\n");
        appendMapStats(sb, r.edgePolicyStats, 10);
        sb.append("\n");
        sb.append("四-补、字段链联调验收\n");
        appendFieldChainVerifyText(sb, r);
        sb.append("\n");
        sb.append("四-补2、主链闭环口径诊断\n");
        appendMainChainDiagnosisText(sb, r);
        sb.append("\n");
        sb.append("四、教师矩阵诊断\n");
        appendMapStats(sb, r.teacherStats, 10);
        appendBestWorst(sb, "Teacher", r.bestTeacherBucket, r.worstTeacherBucket);
        sb.append("\n");
        sb.append("五、V20因链/窗口诊断\n");
        appendMapStats(sb, r.v20Stats, 10);
        appendBestWorst(sb, "V20", r.bestV20Bucket, r.worstV20Bucket);
        sb.append("WindowState Top：\n");
        appendMapStats(sb, r.windowStats, 10);
        sb.append("\n");
        sb.append("五-补、动态因果窗口诊断\n");
        appendDynamicCausalDiagnosisText(sb, r);
        sb.append("\n");
        sb.append("六、错因诊断\n");
        appendMapStats(sb, r.missStats, 10);
        if (r.topMissBucket != null) {
            sb.append("主要错因=")
                    .append(r.topMissBucket.name)
                    .append("，次数=")
                    .append(r.topMissBucket.miss)
                    .append("\n");
        }
        sb.append("\n");
        sb.append("七、SHADOW反事实诊断\n");
        sb.append("最近SHADOW已开奖样本：总数=")
                .append(r.shadowTotal)
                .append("，反事实命中=")
                .append(r.shadowHit)
                .append("，反事实未中=")
                .append(r.shadowMiss)
                .append("，反事实胜率=")
                .append(pct(r.shadowHitRate))
                .append("，等待=")
                .append(r.shadowWait)
                .append("\n");
        sb.append("SHADOW FinalP>=36% ")
                .append(r.shadowP36.shortText())
                .append("；>=38% ")
                .append(r.shadowP38.shortText())
                .append("；>=40% ")
                .append(r.shadowP40.shortText())
                .append("\n");
        sb.append("SHADOW Edge>=0 ")
                .append(r.shadowEdge0.shortText())
                .append("；>=1% ")
                .append(r.shadowEdge1.shortText())
                .append("；>=2% ")
                .append(r.shadowEdge2.shortText())
                .append("\n");
        sb.append("SHADOW优先域=")
                .append(r.shadowPreferredDomain)
                .append("，命中=")
                .append(r.shadowPreferredDomainHit)
                .append("，胜率=")
                .append(pct(safeRate(r.shadowPreferredDomainHit, r.shadowPreferredDomain)))
                .append("；假高P域=")
                .append(r.shadowFalseHighP)
                .append("；Edge虚强域=")
                .append(r.shadowEdgeFalseStrong)
                .append("；覆盖风险域=")
                .append(r.shadowCoverageRisk)
                .append("\n");
        if (r.bestShadowFinalPBucket != null) {
            sb.append("SHADOW最佳概率段=").append(r.bestShadowFinalPBucket.fullText()).append("\n");
        }
        if (r.bestShadowTeacherBucket != null) {
            sb.append("SHADOW最佳Teacher段=")
                    .append(r.bestShadowTeacherBucket.fullText())
                    .append("\n");
        }
        sb.append("\n");
        sb.append("八、真实BET逐单复盘（最近100单）\n");
        appendCaseDiagnosisList(sb, r.caseDiagnoses, 100);
        sb.append("\n");
        sb.append("九、主要失败类型\n");
        appendMapStats(sb, r.failureTypeStats, 14);
        sb.append("\n");
        sb.append("十、模块初步责任\n");
        appendMapStats(sb, r.responsibleModuleStats, 14);
        sb.append("\n");
        appendModuleDiagnosisList(sb, r.moduleDiagnoses, 20);
        sb.append("\n");
        sb.append("十一、SHADOW逐单反事实（最近120单）\n");
        appendShadowDiagnosisList(sb, r.shadowCaseDiagnoses, 120);
        sb.append("\n");
        buildConclusion(r, sb);
        r.fieldChainVerifySummary =
                "字段链验收：WEAK+V20空="
                        + r.fieldChainWeakV20EmptyStillBet
                        + " Cover空="
                        + r.fieldChainCoverEmptyStillBet
                        + " Edge虚强="
                        + r.fieldChainEdgeFalseStrongByPhenomenon
                        + " Reject缺失="
                        + r.fieldChainRejectMissingBet
                        + " 版本缺失="
                        + r.fieldChainVersionMissingBet;
        if (r.fieldChainWeakV20EmptyStillBet > 0
                || r.fieldChainCoverEmptyStillBet > 0
                || r.fieldChainEdgeFalseStrongByPhenomenon > 0
                || r.fieldChainRejectMissingBet > 0
                || r.fieldChainVersionMissingBet > 0) {
            r.fieldChainMainProblem = "字段链联调未通过：仍有弱字段真实BET或字段版本缺失";
        } else {
            r.fieldChainMainProblem = "字段链联调通过或样本不足未发现断链";
        }
        r.detail = limit(sb.toString(), MAX_REPORT_LEN);
        StringBuilder caseSb = new StringBuilder();
        appendCaseDiagnosisList(caseSb, r.caseDiagnoses, 100);
        r.caseReplayText = limit(caseSb.toString(), MAX_REPORT_LEN);
        StringBuilder moduleSb = new StringBuilder();
        appendModuleDiagnosisList(moduleSb, r.moduleDiagnoses, 30);
        r.moduleResponsibilityText = limit(moduleSb.toString(), MAX_REPORT_LEN);
        r.summary = buildShortSummary(r);
    }

    private void buildConclusion(DiagnosisReport r, StringBuilder sb) {
        sb.append("十二、诊断结论\n");
        String problem = "";
        String advice = "";
        if (r.runtimeFirstSummary.indexOf("rootCause=READ_ERROR") >= 0
                || r.runtimeFirstSummary.indexOf("rootCause=RUNTIME_CHAIN") >= 0
                || r.runtimeFirstSummary.indexOf("rootCause=SETTLEMENT_OOS") >= 0) {
            problem = append(problem, "P0运行/结算/OOS链未闭合，策略层Finding仅作SECONDARY_BLOCK");
            advice = append(advice, "先按BusinessTruth根因修复数据读取、Runtime、Settlement/OOS Owner链");
        }
        if (!r.productionModelProven) {
            problem = append(problem, "MODEL_PROVEN=false，本版本REAL频率要求与债务均NOT_APPLICABLE");
            advice = append(advice, "继续严格OOS SHADOW取证；不得以频率债务触发REAL或调低门槛");
        }
        if (r.dynamicCausalMainProblem != null
                && r.dynamicCausalMainProblem.length() > 0
                && r.dynamicCausalMainProblem.indexOf("暂未发现") < 0) {
            problem = append(problem, r.dynamicCausalMainProblem);
            advice = append(advice, r.dynamicCausalNextFix);
        }
        if (r.totalBet < 30) {
            problem = append(problem, "真实BET样本少于30，短期胜率波动大，但仍可看错因方向");
        }
        if (r.teacherWeakNoBetStillBet > 0) {
            problem =
                    append(
                            problem,
                            "Teacher闭环已提示WEAK_NO_BET但仍真实BET=" + r.teacherWeakNoBetStillBet + "单");
            advice = append(advice, "优先联调DecisionGateEngine第5/6最终严格审判字段复制和最终验证顺序");
        }
        if (r.falseHighPStillBet > 0 || r.edgeFalseStrongStillBet > 0) {
            problem =
                    append(
                            problem,
                            "假高P/Edge虚强仍真实BET="
                                    + (r.falseHighPStillBet + r.edgeFalseStrongStillBet)
                                    + "单");
            advice =
                    append(
                            advice,
                            "检查ProbabilityEngine假高P标记、DynamicGate EdgePolicy、DecisionGate"
                                + " finalBetStrictBlockCode是否全链落库");
        }
        if (r.k3CoverageRiskStillBet > 0 || r.thirdNumberWeakStillBet > 0) {
            problem =
                    append(
                            problem,
                            "K3覆盖风险/第三号弱仍真实BET="
                                    + (r.k3CoverageRiskStillBet + r.thirdNumberWeakStillBet)
                                    + "单");
            advice =
                    append(
                            advice,
                            "检查ProbabilityEngine覆盖质量字段是否写入Decision/Trade，并由DecisionGate最终拦截");
        }
        if (r.preferredDomainBet > 0) {
            r.finalBetFixSummary =
                    "优先域BET="
                            + r.preferredDomainBet
                            + "/中"
                            + r.preferredDomainHit
                            + "/率"
                            + pct(safeRate(r.preferredDomainHit, r.preferredDomainBet));
        } else {
            r.finalBetFixSummary = "优先域BET=0，需观察SHADOW优先域是否晋级";
        }
        if (r.totalBet >= 10 && r.winRate < 0.300d) {
            problem = append(problem, "真实BET胜率低于K3基础线30%，说明当前放行质量不够");
        } else if (r.totalBet >= 30 && r.winRate < 0.350d) {
            problem = append(problem, "真实BET胜率低于目标区间，入场过滤或概率校准需要修正");
        }
        if (r.negativeEdgeBet > 0
                && r.totalBet > 0
                && ((double) r.negativeEdgeBet / (double) r.totalBet) >= 0.25d) {
            problem = append(problem, "负Edge出手占比偏高，DecisionGate可能放行了门槛下方候选");
            advice =
                    append(advice, "优先检查 DecisionGateEngine 的 lowFreqRescue / strongEvidence 放行条件");
        }
        if (r.worstFinalPBucket != null
                && r.worstFinalPBucket.total >= 5
                && r.worstFinalPBucket.rate() < 0.300d) {
            problem = append(problem, "存在低效FinalP分桶：" + r.worstFinalPBucket.fullText());
            advice = append(advice, "检查 ProbabilityEngine 是否把Teacher/V20/相似窗口贡献抬得过高");
        }
        if (r.worstTeacherBucket != null
                && r.worstTeacherBucket.total >= 5
                && r.worstTeacherBucket.rate() < 0.300d) {
            problem = append(problem, "Teacher分桶疑似假强：" + r.worstTeacherBucket.fullText());
            advice = append(advice, "检查 TeacherNumberMatrixEngine 的样本可信、近期有效性和相似窗口匹配");
        }
        if (r.worstV20Bucket != null
                && r.worstV20Bucket.total >= 5
                && r.worstV20Bucket.rate() < 0.300d) {
            problem = append(problem, "V20因链分桶疑似假强：" + r.worstV20Bucket.fullText());
            advice = append(advice, "检查 DynamicCauseChainEngine 的证据贡献/反证贡献是否只是解释强而非预测强");
        }
        if (r.shadowTotal >= 20 && r.totalBet >= 10 && r.shadowHitRate > r.winRate + 0.080d) {
            problem = append(problem, "SHADOW反事实胜率明显高于真实BET，最终门可能错杀强单或放行方向错位");
            advice = append(advice, "优先修 DecisionGateEngine / DynamicGateEngine，不要直接全局降门槛");
        }
        if (r.betP40.total >= 5 && r.betP40.rate() > r.winRate + 0.080d) {
            problem = append(problem, "高FinalP样本明显更好，低FinalP可能拖累胜率");
            advice = append(advice, "提高低证据候选的入场要求，保留高FinalP高Edge候选");
        }
        if (r.topMissBucket != null && r.topMissBucket.miss >= 5) {
            problem = append(problem, "错因集中在 " + r.topMissBucket.name + "，需要针对性修对应引擎");
        }
        StatBucket topFailure = topCountBucket(r.failureTypeStats);
        if (topFailure != null
                && topFailure.total >= 5
                && !"HIT_CONFIRMED".equals(topFailure.name)) {
            problem = append(problem, "主要失败类型=" + topFailure.name + "（" + topFailure.total + "单）");
            advice = append(advice, fixHintForFailure(topFailure.name, null, null));
        }
        StatBucket topModule = topCountBucket(r.responsibleModuleStats);
        if (topModule != null && topModule.total >= 5 && !"NONE".equals(topModule.name)) {
            problem = append(problem, "模块初步责任集中在 " + topModule.name + "（" + topModule.total + "单）");
        }
        if (problem.length() == 0) {
            problem = "暂未发现单一明显问题，需要继续积累BET/SHADOW样本";
        }
        if (advice.length() == 0) {
            advice =
                    "先继续积累样本；下一步根据FinalP/Teacher/V20/SHADOW分桶决定修 Probability、Teacher、DynamicCause 或"
                        + " DecisionGate";
        }
        r.mainProblem = problem;
        r.nextFixAdvice = advice;
        sb.append("主要问题：").append(problem).append("\n");
        sb.append("下一步建议：").append(advice).append("\n");
        sb.append("注意：本诊断只读数据，不改策略，不影响出号，不影响结算。\n");
    }

    private String buildShortSummary(DiagnosisReport r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Runtime-first=")
                .append(limit(safe(r.runtimeFirstSummary), 150))
                .append("；REAL=")
                .append(safe(r.realAuthorizationState))
                .append("/required=")
                .append(r.requiredRealBet)
                .append("/debt=")
                .append(r.realBetDebt)
                .append("；历史真实BET诊断：出手=")
                .append(r.totalBet)
                .append(" 中=")
                .append(r.hitBet)
                .append(" 挂=")
                .append(r.missBet)
                .append(" 胜率=")
                .append(pct(r.winRate))
                .append(" 盈亏=")
                .append(money(r.pnl));
        if (r.shadowTotal > 0) {
            sb.append("；SHADOW反事实=")
                    .append(r.shadowHit)
                    .append("/")
                    .append(r.shadowTotal)
                    .append(" ")
                    .append(pct(r.shadowHitRate));
        }
        sb.append("；频率100=")
                .append(pct(r.realFrequency100))
                .append(" 债务=")
                .append(pct(r.frequencyDebt))
                .append(" 探针=")
                .append(r.frequencyRecoveryBetCount + r.realProbeBetCount)
                .append("；主链=")
                .append(safe(r.mainChainSummary))
                .append("；最终闭环：弱师仍BET=")
                .append(r.teacherWeakNoBetStillBet)
                .append(" 假高P=")
                .append(r.falseHighPStillBet)
                .append(" Edge虚强=")
                .append(r.edgeFalseStrongStillBet)
                .append(" 覆盖风险=")
                .append(r.k3CoverageRiskStillBet)
                .append(" 动态坏单=")
                .append(r.dynamicCausalHardRiskStillBet)
                .append(" 动态READY=")
                .append(r.dynamicCausalReadyHit)
                .append("/")
                .append(r.dynamicCausalReadyBet);
        if (r.mainProblem != null && r.mainProblem.length() > 0) {
            sb.append("；问题=").append(limit(r.mainProblem, 180));
        }
        return limit(sb.toString(), 520);
    }

    private void applyR811ReleaseTruth(DbStore store, DiagnosisReport report) {
        if (report == null) return;
        report.productionModelProven = V51BuildManifest.MODEL_PROVEN;
        try {
            RuntimeHealthLedgerR36.BusinessTruth truth =
                    store == null ? null : store.businessTruthR811();
            report.runtimeFirstSummary =
                    truth == null
                            ? "status=FAIL|rootCause=READ_ERROR|error=STORE_NULL"
                            : truth.canonical();
        } catch (Throwable error) {
            report.runtimeFirstSummary =
                    "status=FAIL|rootCause=READ_ERROR|error="
                            + error.getClass().getSimpleName();
        }
        if (!V51BuildManifest.MODEL_PROVEN || V51BuildManifest.SHADOW_ONLY) {
            report.realAuthorizationState = "NOT_APPLICABLE_MODEL_NOT_PROVEN_SHADOW_ONLY";
            report.requiredRealBet = 0;
            report.realBetDebt = 0;
            report.frequencyDebt = 0.0d;
            report.frequencyFloorRequiredCount = 0;
            report.frequencyFloorTargetCount = 0;
            report.frequencyFloorDebtCount = 0;
            report.frequencyTargetDebtCount = 0;
            report.frequencyState = "NOT_APPLICABLE";
        } else {
            report.realAuthorizationState = "APPLICABLE_REQUIRES_RUNTIME_CERTIFICATES";
            report.requiredRealBet = Math.max(0, report.frequencyFloorRequiredCount);
            report.realBetDebt = Math.max(0, report.frequencyFloorDebtCount);
        }
    }

    private void appendMainChainDiagnosisText(StringBuilder sb, DiagnosisReport r) {
        if (sb == null || r == null) return;
        sb.append("版本=")
                .append(MAIN_CHAIN_VERSION)
                .append("；闭环=")
                .append(CLOSED_LOOP_VERSION)
                .append("\n");
        sb.append(safe(r.realShadowSplitSummary)).append("\n");
        sb.append(safe(r.mainChainSummary)).append("\n");
        sb.append("NO_SAMPLE中性真实BET样本=")
                .append(r.noSampleNeutralCount)
                .append("；V20 WATCH中性=")
                .append(r.v20WatchNeutralCount)
                .append("；Adaptive WATCH中性=")
                .append(r.adaptiveWatchNeutralCount)
                .append("\n");
        sb.append("SHADOW中性样本：NO_SAMPLE=")
                .append(r.shadowNoSampleNeutralCount)
                .append("；V20 WATCH=")
                .append(r.shadowV20WatchNeutralCount)
                .append("；Adaptive WATCH=")
                .append(r.shadowAdaptiveWatchNeutralCount)
                .append("\n");
        sb.append("说明：这些中性状态只用于诊断和分桶，不等同弱杀，不计真实盈亏，也不能救援BET。\n");
    }

    private void appendDynamicCausalDiagnosisText(StringBuilder sb, DiagnosisReport r) {
        if (sb == null || r == null) return;
        sb.append("动态因果总览：").append(safe(r.dynamicCausalSummary)).append("\n");
        sb.append("动态簇坏单=")
                .append(r.dynamicClusterBadBet)
                .append("，NeedSupply坏单=")
                .append(r.needSupplyBadBet)
                .append("，反事实坏单=")
                .append(r.counterfactualBadBet)
                .append("，Adaptive冻结仍BET=")
                .append(r.adaptiveFreezeStillBet)
                .append("，Promotion冻结仍BET=")
                .append(r.promotionFreezeStillBet)
                .append("，动态硬风险仍BET=")
                .append(r.dynamicCausalHardRiskStillBet)
                .append("\n");
        sb.append("动态READY真实BET=")
                .append(r.dynamicCausalReadyBet)
                .append("，命中=")
                .append(r.dynamicCausalReadyHit)
                .append("，胜率=")
                .append(pct(safeRate(r.dynamicCausalReadyHit, r.dynamicCausalReadyBet)))
                .append("；SHADOW动态READY=")
                .append(r.shadowDynamicCausalReady)
                .append("，命中=")
                .append(r.shadowDynamicCausalReadyHit)
                .append("，胜率=")
                .append(pct(safeRate(r.shadowDynamicCausalReadyHit, r.shadowDynamicCausalReady)))
                .append("\n");
        sb.append("DynamicCluster 分桶：\n");
        appendMapStats(sb, r.dynamicClusterStats, 10);
        sb.append("NeedSupply 分桶：\n");
        appendMapStats(sb, r.needSupplyStats, 10);
        sb.append("Counterfactual 分桶：\n");
        appendMapStats(sb, r.counterfactualStats, 10);
        sb.append("AdaptiveJudge 分桶：\n");
        appendMapStats(sb, r.adaptiveStats, 10);
        sb.append("Promotion 分桶：\n");
        appendMapStats(sb, r.promotionStats, 10);
        sb.append("动态因果综合分桶：\n");
        appendMapStats(sb, r.dynamicCausalStats, 10);
        sb.append("动态结论：").append(safe(r.dynamicCausalMainProblem)).append("\n");
        sb.append("动态建议：").append(safe(r.dynamicCausalNextFix)).append("\n");
    }

    private void appendArrayStats(StringBuilder sb, StatBucket[] arr, int start, int end, int max) {
        if (sb == null || arr == null) return;
        int count = 0;
        for (int i = start; i <= end && i < arr.length; i++) {
            StatBucket b = arr[i];
            if (b == null || b.total <= 0) continue;
            sb.append(b.fullText()).append("\n");
            count++;
            if (count >= max) break;
        }
        if (count == 0) {
            sb.append("暂无足够样本\n");
        }
    }

    private void appendMapStats(StringBuilder sb, HashMap<String, StatBucket> map, int max) {
        if (sb == null) return;
        ArrayList<StatBucket> list = sortBucketList(map);
        if (list.size() == 0) {
            sb.append("暂无足够样本\n");
            return;
        }
        int count = Math.min(list.size(), max);
        for (int i = 0; i < count; i++) {
            sb.append(list.get(i).fullText()).append("\n");
        }
    }

    private void appendFieldChainVerifyText(StringBuilder sb, DiagnosisReport r) {
        if (sb == null || r == null) return;
        sb.append("AutoFetch版本=").append(safe(r.fieldChainAutoFetchVersion)).append("\n");
        sb.append("DbStore版本=").append(safe(r.fieldChainDbStoreVersion)).append("\n");
        sb.append("候选匹配=")
                .append(safe(r.fieldChainCandidateMatch))
                .append("｜阶段=")
                .append(safe(r.fieldChainCandidateStage))
                .append("\n");
        sb.append("自动降级=")
                .append(safe(r.fieldChainAutoDowngrade))
                .append("｜代码=")
                .append(safe(r.fieldChainAutoDowngradeCode))
                .append("\n");
        sb.append("Trade修复=")
                .append(safe(r.fieldChainTradeRepair))
                .append("｜建Trade结果=")
                .append(safe(r.lastTradeCreateResult))
                .append("｜错误=")
                .append(safe(r.lastTradeCreateError))
                .append("\n");
        sb.append("最后Decision字段=")
                .append(emptyAsNone(r.fieldChainLastDecisionSummary))
                .append("\n");
        sb.append("最后候选字段=").append(emptyAsNone(r.fieldChainCandidateSummary)).append("\n");
        sb.append("最后Trade字段=").append(emptyAsNone(r.fieldChainTradeRepairSummary)).append("\n");
        sb.append("弱字段真实BET验收：")
                .append("WEAK+V20空=")
                .append(r.fieldChainWeakV20EmptyStillBet)
                .append("，Window空=")
                .append(r.fieldChainWindowEmptyStillBet)
                .append("，Cover空=")
                .append(r.fieldChainCoverEmptyStillBet)
                .append("，Edge虚强=")
                .append(r.fieldChainEdgeFalseStrongByPhenomenon)
                .append("，P36-38弱域=")
                .append(r.fieldChainP36_38WeakStillBet)
                .append("，P40无强因=")
                .append(r.fieldChainP40NoStrongStillBet)
                .append("，Reject缺失=")
                .append(r.fieldChainRejectMissingBet)
                .append("，版本缺失=")
                .append(r.fieldChainVersionMissingBet)
                .append("\n");
        if (r.fieldChainWeakV20EmptyStillBet == 0
                && r.fieldChainCoverEmptyStillBet == 0
                && r.fieldChainEdgeFalseStrongByPhenomenon == 0
                && r.fieldChainRejectMissingBet == 0
                && r.fieldChainVersionMissingBet == 0) {
            sb.append("字段链验收=通过：弱字段BET已归零或当前样本未发现穿透断链。\n");
        } else {
            sb.append(
                    "字段链验收=未通过：仍存在弱字段真实BET，优先检查 AutoFetchService→DbStore→DecisionGateEngine"
                        + " 替换是否全部生效。\n");
        }
    }

    private String emptyAsNone(String s) {
        String x = safe(s);
        return x.length() == 0 ? "暂无" : x;
    }

    private void appendCaseDiagnosisList(StringBuilder sb, ArrayList<CaseDiagnosis> list, int max) {
        if (sb == null) return;
        if (list == null || list.size() == 0) {
            sb.append("暂无逐单复盘样本\n");
            return;
        }
        int count = Math.min(list.size(), max);
        for (int i = 0; i < count; i++) {
            CaseDiagnosis d = list.get(i);
            if (d == null) continue;
            sb.append(i + 1)
                    .append(". ")
                    .append("期=")
                    .append(d.targetIssue)
                    .append(" P")
                    .append(d.position)
                    .append(" [")
                    .append(two(d.n1))
                    .append(",")
                    .append(two(d.n2))
                    .append(",")
                    .append(two(d.n3))
                    .append("]")
                    .append(" 开=")
                    .append(two(d.actualNumber))
                    .append(" ")
                    .append(d.result)
                    .append(" P=")
                    .append(pct(d.finalP))
                    .append(" Gate=")
                    .append(pct(d.dynamicGate))
                    .append(" Edge=")
                    .append(signedPct(d.edge))
                    .append(" Teacher=")
                    .append(limit(d.teacherStatus, 24))
                    .append(" V20=")
                    .append(limit(d.v20Status, 24))
                    .append(" Window=")
                    .append(limit(d.windowStateKey, 30))
                    .append(" Cover=")
                    .append(pct(d.coverageQualityScore))
                    .append("/")
                    .append(pct(d.k3CoverageRisk))
                    .append(" Third=")
                    .append(pct(d.thirdNumberQualityScore))
                    .append(" Reject=")
                    .append(limit(d.finalDecisionRejectCode, 26))
                    .append(" 失败类型=")
                    .append(d.failureType)
                    .append(" 责任=")
                    .append(d.responsibleModule)
                    .append(" 动态=")
                    .append(limit(d.dynamicCausalSummary, 160))
                    .append(" 修复=")
                    .append(limit(d.fixHint, 120))
                    .append("\n");
        }
    }

    private void appendShadowDiagnosisList(
            StringBuilder sb, ArrayList<ShadowCaseDiagnosis> list, int max) {
        if (sb == null) return;
        if (list == null || list.size() == 0) {
            sb.append("暂无SHADOW反事实样本\n");
            return;
        }
        int count = Math.min(list.size(), max);
        for (int i = 0; i < count; i++) {
            ShadowCaseDiagnosis d = list.get(i);
            if (d == null) continue;
            sb.append(i + 1)
                    .append(". ")
                    .append("期=")
                    .append(d.targetIssue)
                    .append(" P")
                    .append(d.position)
                    .append(" [")
                    .append(two(d.n1))
                    .append(",")
                    .append(two(d.n2))
                    .append(",")
                    .append(two(d.n3))
                    .append("]")
                    .append(" 开=")
                    .append(two(d.actualNumber))
                    .append(" would=")
                    .append(d.wouldHit == 1 ? "HIT" : "MISS")
                    .append(" P=")
                    .append(pct(d.finalP))
                    .append(" Gate=")
                    .append(pct(d.gate))
                    .append(" Edge=")
                    .append(signedPct(d.edge))
                    .append(" Teacher=")
                    .append(limit(d.teacherStatus, 24))
                    .append(" V20=")
                    .append(limit(d.v20Status, 24))
                    .append(" Cover=")
                    .append(pct(d.coverageQualityScore))
                    .append("/")
                    .append(pct(d.k3CoverageRisk))
                    .append(" Domain=")
                    .append(limit(d.shadowPromotionDomain, 48))
                    .append(" 动态=")
                    .append(limit(d.dynamicCausalSummary, 120))
                    .append(" 晋级提示=")
                    .append(limit(d.promotionHint, 120))
                    .append("\n");
        }
    }

    private void appendModuleDiagnosisList(
            StringBuilder sb, ArrayList<ModuleDiagnosis> list, int max) {
        if (sb == null) return;
        if (list == null || list.size() == 0) {
            sb.append("暂无模块诊断样本\n");
            return;
        }
        int count = Math.min(list.size(), max);
        for (int i = 0; i < count; i++) {
            ModuleDiagnosis m = list.get(i);
            if (m == null) continue;
            sb.append(i + 1)
                    .append(". ")
                    .append(m.moduleName)
                    .append("：支持=")
                    .append(m.supportCount)
                    .append(" BET=")
                    .append(m.betCount)
                    .append(" 中=")
                    .append(m.hitCount)
                    .append(" 挂=")
                    .append(m.missCount)
                    .append(" 胜率=")
                    .append(pct(m.hitRate))
                    .append(" SHADOW反事实=")
                    .append(pct(m.shadowWouldHitRate))
                    .append(" 责任挂=")
                    .append(m.responsibleMissCount)
                    .append(" 状态=")
                    .append(m.effectStatus)
                    .append(" 问题=")
                    .append(limit(m.problem, 80))
                    .append(" 建议=")
                    .append(limit(m.fixAdvice, 140))
                    .append("\n");
        }
    }

    private void appendBestWorst(StringBuilder sb, String name, StatBucket best, StatBucket worst) {
        if (sb == null) return;
        if (best != null) {
            sb.append(name).append("最佳=").append(best.fullText()).append("\n");
        }
        if (worst != null) {
            sb.append(name).append("最差=").append(worst.fullText()).append("\n");
        }
    }

    private void addStat(StatBucket[] arr, int position, BetCase c) {
        if (arr == null || c == null) return;
        if (position < 0 || position >= arr.length) return;
        if (arr[position] == null) arr[position] = new StatBucket("P" + position);
        arr[position].add(c);
    }

    private void addStat(HashMap<String, StatBucket> map, String key, BetCase c) {
        if (map == null || c == null) return;
        key = normalizeBucket(key);
        StatBucket b = map.get(key);
        if (b == null) {
            b = new StatBucket(key);
            map.put(key, b);
        }
        b.add(c);
    }

    private ArrayList<StatBucket> sortBucketList(HashMap<String, StatBucket> map) {
        ArrayList<StatBucket> list = new ArrayList<StatBucket>();
        if (map == null || map.size() == 0) return list;
        for (String key : map.keySet()) {
            StatBucket b = map.get(key);
            if (b != null && b.total > 0) list.add(b);
        }
        Collections.sort(
                list,
                new Comparator<StatBucket>() {
                    public int compare(StatBucket a, StatBucket b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.total != b.total) return b.total - a.total;
                        double ra = a.rate();
                        double rb = b.rate();
                        if (ra < rb) return 1;
                        if (ra > rb) return -1;
                        return a.name.compareTo(b.name);
                    }
                });
        return list;
    }

    private StatBucket bestBucket(HashMap<String, StatBucket> map, boolean high) {
        if (map == null || map.size() == 0) return null;
        StatBucket best = null;
        for (String key : map.keySet()) {
            StatBucket b = map.get(key);
            if (b == null || b.total <= 0) continue;
            if (best == null) {
                best = b;
                continue;
            }
            if (high) {
                if (b.rate() > best.rate() || (b.rate() == best.rate() && b.total > best.total))
                    best = b;
            } else {
                if (b.rate() < best.rate() || (b.rate() == best.rate() && b.total > best.total))
                    best = b;
            }
        }
        return best;
    }

    private StatBucket bestBucketFromArray(StatBucket[] arr, boolean high) {
        if (arr == null) return null;
        StatBucket best = null;
        for (int i = 1; i < arr.length; i++) {
            StatBucket b = arr[i];
            if (b == null || b.total <= 0) continue;
            if (best == null) {
                best = b;
                continue;
            }
            if (high) {
                if (b.rate() > best.rate() || (b.rate() == best.rate() && b.total > best.total))
                    best = b;
            } else {
                if (b.rate() < best.rate() || (b.rate() == best.rate() && b.total > best.total))
                    best = b;
            }
        }
        return best;
    }

    private StatBucket topCountBucket(HashMap<String, StatBucket> map) {
        if (map == null || map.size() == 0) return null;
        StatBucket best = null;
        for (String key : map.keySet()) {
            StatBucket b = map.get(key);
            if (b == null) continue;
            if (best == null || b.miss > best.miss) best = b;
        }
        return best;
    }

    private ArrayList<Models.Decision> loadRecentDecisions(DbStore store, int limit) {
        ArrayList<Models.Decision> out = new ArrayList<Models.Decision>();
        if (store == null) return out;
        try {
            List<Models.Decision> list = store.getRecentDecisions(limit);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Models.Decision d = list.get(i);
                    if (d != null) out.add(d);
                }
            }
        } catch (Throwable e) {
        }
        return out;
    }

    private ArrayList<Models.Trade> loadRecentTradesByReflection(DbStore store, int limit) {
        ArrayList<Models.Trade> out = new ArrayList<Models.Trade>();
        if (store == null) return out;
        String[] names =
                new String[] {
                    "getRecentTrades",
                    "getRecentTradeList",
                    "listRecentTrades",
                    "getTrades",
                    "getAllRecentTrades"
                };
        for (int i = 0; i < names.length; i++) {
            try {
                java.lang.reflect.Method m =
                        store.getClass().getMethod(names[i], new Class[] {int.class});
                Object obj = m.invoke(store, new Object[] {Integer.valueOf(limit)});
                if (obj instanceof List) {
                    List list = (List) obj;
                    for (int j = 0; j < list.size(); j++) {
                        Object o = list.get(j);
                        if (o instanceof Models.Trade) {
                            out.add((Models.Trade) o);
                        }
                    }
                    if (out.size() > 0) return out;
                }
            } catch (Throwable e) {
            }
        }
        return out;
    }

    private Models.Trade getTradeByTargetIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        try {
            return store.getTradeByTargetIssue(issue);
        } catch (Throwable e) {
            return null;
        }
    }

    private Models.Decision getDecisionByTargetIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        try {
            return store.getDecisionByTargetIssue(issue);
        } catch (Throwable e) {
            return null;
        }
    }

    private Models.Draw getDrawByIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        try {
            Models.Draw d = store.getDrawByIssue(issue);
            if (d != null) return d;
        } catch (Throwable e) {
        }
        try {
            Models.Draw d = store.loadDrawByIssue(issue);
            if (d != null) return d;
        } catch (Throwable e) {
        }
        return null;
    }

    private boolean isBetDecision(Models.Decision d) {
        if (d == null) return false;
        return isBetText(d.decision) || isBetText(d.action);
    }

    private boolean isShadowDecision(Models.Decision d) {
        if (d == null) return false;
        String s = safe(d.decision);
        if (AppConfig.DECISION_SHADOW.equals(s)) return true;
        return "SHADOW".equalsIgnoreCase(s) || "影子观察".equals(s);
    }

    private boolean isRealBetTrade(Models.Trade t) {
        if (t == null) return false;
        if (!isRealScope(t.statScope)) return false;
        if (!isBetText(t.action)) {
            String action = safe(t.action);
            if (action.length() > 0) return false;
        }
        if (!AppConfig.isValidPosition(t.position)) return false;
        if (!AppConfig.isValidNumber(t.n1)
                || !AppConfig.isValidNumber(t.n2)
                || !AppConfig.isValidNumber(t.n3)) return false;
        if (t.targetIssue == null || t.targetIssue.length() == 0) return false;
        return true;
    }

    private boolean isRealScope(String scope) {
        String s = safe(scope).trim();
        if (s.length() == 0) return true;
        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(s)) return true;
        return "REAL".equalsIgnoreCase(s)
                || "REAL_TRADE".equalsIgnoreCase(s)
                || "REAL_BET".equalsIgnoreCase(s)
                || "REALBET".equalsIgnoreCase(s);
    }

    private boolean isBetText(String text) {
        String s = safe(text).trim();
        if (s.length() == 0) return false;
        if (AppConfig.DECISION_BET.equals(s)) return true;
        if (AppConfig.ACTION_BET.equals(s)) return true;
        return "BET".equalsIgnoreCase(s) || "出手".equals(s) || "下注".equals(s);
    }

    private boolean containsK3(BetCase c, int actual) {
        if (c == null) return false;
        return actual == c.n1 || actual == c.n2 || actual == c.n3;
    }

    private void sortCasesDesc(ArrayList<BetCase> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<BetCase>() {
                    public int compare(BetCase a, BetCase b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.createdAt < b.createdAt) return 1;
                        if (a.createdAt > b.createdAt) return -1;
                        return safe(b.targetIssue).compareTo(safe(a.targetIssue));
                    }
                });
    }

    private String finalPBucket(double p) {
        if (p <= 0.0d) return "FinalP空";
        if (p < 0.340d) return "P<34%";
        if (p < 0.360d) return "P34-36%";
        if (p < 0.380d) return "P36-38%";
        if (p < 0.400d) return "P38-40%";
        if (p < 0.420d) return "P40-42%";
        return "P>=42%";
    }

    private String edgeBucket(double e) {
        if (e < -0.010d) return "Edge<-1%";
        if (e < 0.0d) return "Edge负";
        if (e < 0.010d) return "Edge0-1%";
        if (e < 0.020d) return "Edge1-2%";
        return "Edge>=2%";
    }

    private String two(int n) {
        if (n <= 0) return "--";
        if (n < 10) return "0" + n;
        return String.valueOf(n);
    }

    private String normalizeBucket(String s) {
        s = safe(s).trim();
        if (s.length() == 0) return BUCKET_EMPTY;
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    private String normalizeResult(String r) {
        String s = safe(r).trim();
        if (s.length() == 0) return AppConfig.RESULT_WAIT;
        String u = s.toUpperCase(java.util.Locale.ROOT);
        if ("WIN".equals(u) || "HIT".equals(u) || "命中".equals(s)) return AppConfig.RESULT_WIN;
        if ("MISS".equals(u)
                || "LOSE".equals(u)
                || "LOSS".equals(u)
                || "未中".equals(s)
                || "不中".equals(s)) return AppConfig.RESULT_MISS;
        if ("WAIT".equals(u) || "WAITING".equals(u) || "等待".equals(s) || "等待结算".equals(s))
            return AppConfig.RESULT_WAIT;
        if ("DATA_ERROR".equals(u) || "ERROR".equals(u) || "数据异常".equals(s))
            return AppConfig.RESULT_DATA_ERROR;
        try {
            return AppConfig.normalizeResult(s);
        } catch (Throwable e) {
            return s;
        }
    }

    private boolean isWinResult(String r) {
        return AppConfig.RESULT_WIN.equals(normalizeResult(r));
    }

    private boolean isMissResult(String r) {
        return AppConfig.RESULT_MISS.equals(normalizeResult(r));
    }

    private double safeRate(int hit, int total) {
        if (total <= 0) return 0.0d;
        return ((double) hit) / ((double) total);
    }

    private double getDouble(Object obj, String field, double def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
        }
        return def;
    }

    private int getInt(Object obj, String field, int def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
        }
        return def;
    }

    private String getString(Object obj, String field, String def) {
        if (obj == null || field == null || field.length() == 0) return def == null ? "" : def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return def == null ? "" : def;
    }

    private double firstPositive(double a, double b, double c) {
        if (a != 0.0d) return a;
        if (b != 0.0d) return b;
        return c;
    }

    private double firstPositive(double a, double b, double c, double d) {
        if (a != 0.0d) return a;
        if (b != 0.0d) return b;
        if (c != 0.0d) return c;
        return d;
    }

    private int firstPositiveInt(int a, int b, int c) {
        if (a != 0) return a;
        if (b != 0) return b;
        return c;
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) return a;
        return b == null ? "" : b;
    }

    private String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        return c == null ? "" : c;
    }

    private String firstNonEmpty(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        if (c != null && c.length() > 0) return c;
        return d == null ? "" : d;
    }

    private String firstNonEmpty(String a, String b, String c, String d, String e) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        if (c != null && c.length() > 0) return c;
        if (d != null && d.length() > 0) return d;
        return e == null ? "" : e;
    }

    private String append(String a, String b) {
        if (b == null || b.length() == 0) return safe(a);
        if (a == null || a.length() == 0) return b;
        return a + "；" + b;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String pct(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
        }
    }

    private String signedPct(double v) {
        try {
            return UiFormatter.signedProbability(v);
        } catch (Throwable e) {
            String s = String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
            return v >= 0.0d ? "+" + s : s;
        }
    }

    private String money(double v) {
        try {
            return UiFormatter.money(v);
        } catch (Throwable e) {
            return String.valueOf(v);
        }
    }

    private String limit(String text, int max) {
        if (text == null) return "";
        if (max <= 0 || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private void trySetState(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) return;
        try {
            store.setState(key, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    public static final class DiagnosisReport {
        public int ok;
        public String version;
        public long createdAt;
        public int betLimit;
        public int shadowLimit;
        public ArrayList<BetCase> betCases;
        public ArrayList<BetCase> shadowCases;
        public int totalBet;
        public int hitBet;
        public int missBet;
        public int waitBet;
        public int dataErrorBet;
        public double winRate;
        public double pnl;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public int negativeEdgeBet;
        public int teacherRiskBet;
        public int v20RiskBet;
        public int shadowTotal;
        public int shadowHit;
        public int shadowMiss;
        public int shadowWait;
        public double shadowHitRate;
        public StatBucket[] positionStats;
        public HashMap<String, StatBucket> finalPStats;
        public HashMap<String, StatBucket> edgeStats;
        public HashMap<String, StatBucket> teacherStats;
        public HashMap<String, StatBucket> v20Stats;
        public HashMap<String, StatBucket> windowStats;
        public HashMap<String, StatBucket> missStats;
        public HashMap<String, StatBucket> stateModeStats;
        public HashMap<String, StatBucket> shadowFinalPStats;
        public HashMap<String, StatBucket> shadowTeacherStats;
        public HashMap<String, StatBucket> shadowV20Stats;
        public HashMap<String, StatBucket> dynamicClusterStats;
        public HashMap<String, StatBucket> needSupplyStats;
        public HashMap<String, StatBucket> counterfactualStats;
        public HashMap<String, StatBucket> adaptiveStats;
        public HashMap<String, StatBucket> promotionStats;
        public HashMap<String, StatBucket> dynamicCausalStats;
        public HashMap<String, StatBucket> shadowDynamicCausalStats;
        public StatBucket betP36;
        public StatBucket betP38;
        public StatBucket betP40;
        public StatBucket betEdge0;
        public StatBucket betEdge1;
        public StatBucket betEdge2;
        public StatBucket shadowP36;
        public StatBucket shadowP38;
        public StatBucket shadowP40;
        public StatBucket shadowEdge0;
        public StatBucket shadowEdge1;
        public StatBucket shadowEdge2;
        public StatBucket bestPosition;
        public StatBucket worstPosition;
        public StatBucket bestFinalPBucket;
        public StatBucket worstFinalPBucket;
        public StatBucket bestTeacherBucket;
        public StatBucket worstTeacherBucket;
        public StatBucket bestV20Bucket;
        public StatBucket worstV20Bucket;
        public StatBucket topMissBucket;
        public StatBucket bestShadowFinalPBucket;
        public StatBucket bestShadowTeacherBucket;
        public StatBucket bestShadowV20Bucket;
        public StatBucket bestDynamicCausalBucket;
        public StatBucket worstDynamicCausalBucket;
        public ArrayList<CaseDiagnosis> caseDiagnoses;
        public ArrayList<ShadowCaseDiagnosis> shadowCaseDiagnoses;
        public ArrayList<ModuleDiagnosis> moduleDiagnoses;
        public HashMap<String, ModuleDiagnosis> moduleDiagnosisMap;
        public HashMap<String, StatBucket> failureTypeStats;
        public HashMap<String, StatBucket> responsibleModuleStats;
        public HashMap<String, StatBucket> coverageStats;
        public HashMap<String, StatBucket> finalRejectStats;
        public HashMap<String, StatBucket> finalBetDomainStats;
        public HashMap<String, StatBucket> edgePolicyStats;
        public HashMap<String, StatBucket> shadowFinalBetDomainStats;
        public HashMap<String, StatBucket> shadowCoverageStats;
        public int teacherWeakNoBetStillBet;
        public int falseHighPStillBet;
        public int k3CoverageRiskStillBet;
        public int thirdNumberWeakStillBet;
        public int edgeFalseStrongStillBet;
        public int p40NoStrongCauseStillBet;
        public int finalRejectCodeStillBet;
        public int preferredDomainBet;
        public int preferredDomainHit;
        public int shadowPreferredDomain;
        public int shadowPreferredDomainHit;
        public int shadowFalseHighP;
        public int shadowEdgeFalseStrong;
        public int shadowCoverageRisk;
        public String finalBetFixSummary;
        public String caseReplayText;
        public String moduleResponsibilityText;
        public String mainProblem;
        public String nextFixAdvice;
        public String summary;
        public String detail;
        public double realFrequency50;
        public double realFrequency100;
        public double realFrequency300;
        public double frequencyDebt;
        public String frequencyState;
        public String realAuthorizationState;
        public String runtimeFirstSummary;
        public int requiredRealBet;
        public int realBetDebt;
        public boolean productionModelProven;
        public double probeFrequency;
        public long serviceHeartbeatAgeMs;
        public int serviceUserEnabled;
        public int serviceRestartCount;
        public String teacherWarmSlice;
        public String teacherColdSlice;
        public String teacherDeepSlice;
        public String backgroundServiceSummary;
        public int qualifiedOpportunityCount;
        public int recentProbeCount;
        public int recentLoseCount;
        public int frequencyFloorRequiredCount;
        public int frequencyFloorTargetCount;
        public int frequencyFloorActualCount;
        public int frequencyFloorDebtCount;
        public int frequencyTargetDebtCount;
        public int frequencyNoUpperCap;
        public int frequencyFloorBetCount;
        public int normalBetCount;
        public int frequencyRecoveryBetCount;
        public int realProbeBetCount;
        public int promotionWatchDomains;
        public int promotionReadyDomains;
        public int promotionStrongDomains;
        public int promotionBlockDomains;
        public int p36To38DomainSample;
        public int p36To38DomainHit;
        public String snapshotFreshness;
        public String performanceSummary;
        public String performanceAuditSummary;
        public int unsafePromotionDomainCount;
        public int shadowCompletenessSample;
        public int shadowCoverageComplete;
        public int shadowThirdComplete;
        public int shadowDomainComplete;
        public int shadowRejectComplete;
        public double shadowCoverageCompleteRate;
        public double shadowThirdCompleteRate;
        public double shadowDomainCompleteRate;
        public double shadowRejectCompleteRate;
        public String buildIdentityStatus;
        public String buildIdentityDetail;
        public String softHoldShadowRecovery;
        public String coldStartNeutralRestore;
        public String teacherReservedSlice;
        public String v22ExpertConsensus;
        public String v22ExpertHealth;
        public String v23CausalProbability;
        public String v23MicroWalkForward;
        public String v23ProbabilityOwner;
        public String v24Probability;
        public String v24ProbabilityPolicy;
        public String v24MicroWalkForward;
        public String v24Authorization;
        public String v24OosState;
        public int v24UnauthorizedBetCount;
        public int v24WatchBetCount;
        public int v24HardRiskBetCount;
        public int v24VersionMismatchBetCount;
        public String v28OosReliabilityAudit;
        public String v44DecisionFunnel;
        public String v42RiskCoverage;
        public String v42LanePolicy;
        public String v42OpportunityFunnel;
        public String v42ActionUtility;

        public DiagnosisReport() {
            v22ExpertConsensus = "--";
            v22ExpertHealth = "--";
            v23CausalProbability = "--";
            v23MicroWalkForward = "--";
            v23ProbabilityOwner = "--";
            v24Probability = "--";
            v24ProbabilityPolicy = "--";
            v24MicroWalkForward = "--";
            v24Authorization = "--";
            v24OosState = "--";
            v24UnauthorizedBetCount = 0;
            v24WatchBetCount = 0;
            v24HardRiskBetCount = 0;
            v24VersionMismatchBetCount = 0;
            v28OosReliabilityAudit = "--";
            v44DecisionFunnel = "--";
            v42RiskCoverage = "--";
            v42LanePolicy = "--";
            v42OpportunityFunnel = "--";
            v42ActionUtility = "--";
            ok = 0;
            version = "";
            createdAt = 0L;
            betLimit = 0;
            shadowLimit = 0;
            betCases = new ArrayList<BetCase>();
            shadowCases = new ArrayList<BetCase>();
            finalPStats = new HashMap<String, StatBucket>();
            edgeStats = new HashMap<String, StatBucket>();
            teacherStats = new HashMap<String, StatBucket>();
            v20Stats = new HashMap<String, StatBucket>();
            windowStats = new HashMap<String, StatBucket>();
            missStats = new HashMap<String, StatBucket>();
            stateModeStats = new HashMap<String, StatBucket>();
            coverageStats = new HashMap<String, StatBucket>();
            finalRejectStats = new HashMap<String, StatBucket>();
            finalBetDomainStats = new HashMap<String, StatBucket>();
            edgePolicyStats = new HashMap<String, StatBucket>();
            shadowFinalBetDomainStats = new HashMap<String, StatBucket>();
            shadowCoverageStats = new HashMap<String, StatBucket>();
            finalBetFixSummary = "";
            shadowFinalPStats = new HashMap<String, StatBucket>();
            shadowTeacherStats = new HashMap<String, StatBucket>();
            shadowV20Stats = new HashMap<String, StatBucket>();
            betP36 = new StatBucket("BET FinalP>=36%");
            betP38 = new StatBucket("BET FinalP>=38%");
            betP40 = new StatBucket("BET FinalP>=40%");
            betEdge0 = new StatBucket("BET Edge>=0");
            betEdge1 = new StatBucket("BET Edge>=1%");
            betEdge2 = new StatBucket("BET Edge>=2%");
            shadowP36 = new StatBucket("SHADOW FinalP>=36%");
            shadowP38 = new StatBucket("SHADOW FinalP>=38%");
            shadowP40 = new StatBucket("SHADOW FinalP>=40%");
            shadowEdge0 = new StatBucket("SHADOW Edge>=0");
            shadowEdge1 = new StatBucket("SHADOW Edge>=1%");
            shadowEdge2 = new StatBucket("SHADOW Edge>=2%");
            caseDiagnoses = new ArrayList<CaseDiagnosis>();
            shadowCaseDiagnoses = new ArrayList<ShadowCaseDiagnosis>();
            moduleDiagnoses = new ArrayList<ModuleDiagnosis>();
            moduleDiagnosisMap = new HashMap<String, ModuleDiagnosis>();
            failureTypeStats = new HashMap<String, StatBucket>();
            responsibleModuleStats = new HashMap<String, StatBucket>();
            caseReplayText = "";
            moduleResponsibilityText = "";
            mainProblem = "";
            nextFixAdvice = "";
            summary = "";
            detail = "";
            frequencyState = AppConfig.FREQ_STATE_NORMAL;
            realAuthorizationState = "UNKNOWN";
            runtimeFirstSummary = "status=UNKNOWN|rootCause=READ_ERROR";
            frequencyNoUpperCap = 1;
            snapshotFreshness = "";
            performanceSummary = "";
            performanceAuditSummary = "";
            unsafePromotionDomainCount = 0;
            shadowCompletenessSample = 0;
            shadowCoverageComplete = 0;
            shadowThirdComplete = 0;
            shadowDomainComplete = 0;
            shadowRejectComplete = 0;
            shadowCoverageCompleteRate = 0.0d;
            shadowThirdCompleteRate = 0.0d;
            shadowDomainCompleteRate = 0.0d;
            shadowRejectCompleteRate = 0.0d;
            buildIdentityStatus = "";
            buildIdentityDetail = "";
            softHoldShadowRecovery = "";
            coldStartNeutralRestore = "";
            teacherReservedSlice = "";
            teacherWarmSlice = "";
        }

        public String fieldChainAutoFetchVersion = "";
        public String fieldChainDbStoreVersion = "";
        public String fieldChainCandidateMatch = "";
        public String fieldChainCandidateStage = "";
        public String fieldChainCandidateSummary = "";
        public String fieldChainAutoDowngrade = "";
        public String fieldChainAutoDowngradeCode = "";
        public String fieldChainAutoDowngradeReason = "";
        public String fieldChainTradeRepair = "";
        public String fieldChainTradeRepairSummary = "";
        public String fieldChainLastDecisionSummary = "";
        public String lastTradeCreateResult = "";
        public String lastTradeCreateError = "";
        public String lastTradeFieldChainReason = "";
        public String fieldChainVerifySummary = "";
        public String fieldChainMainProblem = "";
        public int fieldChainWeakV20EmptyStillBet;
        public int fieldChainWindowEmptyStillBet;
        public int fieldChainCoverEmptyStillBet;
        public int fieldChainEdgeFalseStrongByPhenomenon;
        public int fieldChainP36_38WeakStillBet;
        public int fieldChainP40NoStrongStillBet;
        public int fieldChainRejectMissingBet;
        public int fieldChainVersionMissingBet;
        public int dynamicClusterBadBet;
        public int needSupplyBadBet;
        public int counterfactualBadBet;
        public int adaptiveFreezeStillBet;
        public int promotionFreezeStillBet;
        public int dynamicCausalReadyBet;
        public int dynamicCausalReadyHit;
        public int dynamicCausalHardRiskStillBet;
        public int shadowDynamicCausalReady;
        public int shadowDynamicCausalReadyHit;
        public String dynamicCausalSummary;
        public String dynamicCausalMainProblem;
        public String dynamicCausalNextFix;
        public String dynamicCausalDetailText;
        public int realScopeBetCount;
        public int shadowScopeCount;
        public int holdScopeCount;
        public int noSampleNeutralCount;
        public int v20WatchNeutralCount;
        public int adaptiveWatchNeutralCount;
        public int shadowNoSampleNeutralCount;
        public int shadowV20WatchNeutralCount;
        public int shadowAdaptiveWatchNeutralCount;
        public int mainChainFieldCaseCount;
        public int shadowMainChainFieldCaseCount;
        public int mainChainRejectCodeCount;
        public String mainChainSummary;
        public String realShadowSplitSummary;
        public String dynamicCausalStateSummary;
        public String dynamicClusterStateSummary;
        public String adaptiveJudgeDynamicSummary;
        public String shadowPromotionDynamicSummary;
        public String dynamicCausalAutoFetchState;
        public String dynamicCausalDecisionVersionState;
    }

    public static final class CaseDiagnosis {
        public String targetIssue;
        public String baseIssue;
        public String decisionType;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public String result;
        public int hit;
        public double pnl;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double rawP;
        public double calibratedP;
        public String teacherStatus;
        public int teacherSample;
        public double teacherHitRate;
        public double teacherConfidence;
        public double teacherRecentValidity;
        public String v20Status;
        public double v20Support;
        public double v20Risk;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public String windowStateKey;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String missReason;
        public String failureType;
        public String responsibleModule;
        public String fixHint;
        public String reasonSummary;
        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public int falseHighPFlag;
        public String teacherOrdinaryBetPolicy;
        public String dynamicGateEdgePolicy;
        public String finalDecisionRejectCode;
        public String finalDecisionBetDomain;
        public String finalDecisionShadowReason;
        public String dynamicClusterStatus;
        public int dynamicClusterSample;
        public double dynamicClusterHitRate;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRisk;
        public double counterfactualWhyNotRisk;
        public int causeAblationFalseCauseCount;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeAction;
        public String promotionClusterStatus;
        public String promotionAction;
        public double dynamicCausalDecisionQuality;
        public double dynamicCausalDecisionRisk;
        public String dynamicCausalSummary;
        public String finalDecisionStatus;
        public double finalDecisionQualityScore;
        public double finalDecisionRiskScore;
        public String finalDecisionActionAdvice;
        public String adaptiveStatus;
        public String adaptiveAction;
        public String shadowPromotionStatus;
        public String shadowPromotionDomain;
        public double shadowPromoteScore;
        public double ruleDecayRisk;
        public double thirdDragRisk;
        public double counterfactualRisk;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double netEvidenceScore;
        public String fieldChainVersion;
        public String decisionGateVersion;
        public int noSampleNeutral;
        public int v20WatchNeutral;
        public int adaptiveWatchNeutral;

        public CaseDiagnosis() {
            targetIssue = "";
            baseIssue = "";
            decisionType = "";
            result = "";
            teacherStatus = "";
            v20Status = "";
            windowStateKey = "";
            missReason = "";
            failureType = "";
            responsibleModule = "";
            fixHint = "";
            reasonSummary = "";
            finalDecisionStatus = "";
            finalDecisionActionAdvice = "";
            adaptiveStatus = "";
            adaptiveAction = "";
            shadowPromotionStatus = "";
            shadowPromotionDomain = "";
            fieldChainVersion = "";
            decisionGateVersion = "";
        }
    }

    public static final class ShadowCaseDiagnosis {
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public int wouldHit;
        public double finalP;
        public double gate;
        public double edge;
        public String teacherStatus;
        public String v20Status;
        public String shadowReason;
        public String promotionHint;
        public double coverageQualityScore;
        public double k3CoverageRisk;
        public double thirdNumberQualityScore;
        public String finalDecisionBetDomain;
        public String finalDecisionRejectCode;
        public String dynamicGateEdgePolicy;
        public String dynamicClusterStatus;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double counterfactualWhyNotRisk;
        public String adaptiveJudgeAction;
        public String promotionClusterStatus;
        public String dynamicCausalBucket;
        public String dynamicCausalSummary;
        public String finalDecisionStatus;
        public double finalDecisionQualityScore;
        public double finalDecisionRiskScore;
        public String adaptiveStatus;
        public String shadowPromotionStatus;
        public String shadowPromotionDomain;
        public double shadowPromoteScore;
        public double ruleDecayRisk;
        public double thirdDragRisk;
        public double counterfactualRisk;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public String fieldChainVersion;

        public ShadowCaseDiagnosis() {
            targetIssue = "";
            teacherStatus = "";
            v20Status = "";
            shadowReason = "";
            promotionHint = "";
            finalDecisionStatus = "";
            adaptiveStatus = "";
            shadowPromotionStatus = "";
            shadowPromotionDomain = "";
            fieldChainVersion = "";
        }
    }

    public static final class ModuleDiagnosis {
        public String moduleName;
        public int supportCount;
        public int betCount;
        public int hitCount;
        public int missCount;
        public double hitRate;
        public double avgFinalP;
        public double realEdge;
        public double virtualEdge;
        public double profit;
        public int shadowSupportCount;
        public int shadowTotal;
        public int shadowWouldHit;
        public double shadowWouldHitRate;
        public double reliability;
        public double recentEffectiveness;
        public String effectStatus;
        public String problem;
        public String fixAdvice;
        public int responsibleMissCount;

        public ModuleDiagnosis() {
            moduleName = "";
            effectStatus = "INSUFFICIENT";
            problem = "";
            fixAdvice = "";
        }
    }

    public static final class BetCase {
        public String kind;
        public String targetIssue;
        public String baseIssue;
        public long createdAt;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public int settled;
        public int hit;
        public String result;
        public double pnl;
        public String missReason;
        public double finalP;
        public double rawP;
        public double calibratedP;
        public double dynamicGate;
        public double edge;
        public int effectiveSample;
        public int comboSample;
        public double similarHitRate;
        public double comboHitRate;
        public String stateMode;
        public String reasonSummary;
        public String finalPBucket;
        public String edgeBucket;
        public String teacherBucket;
        public String v20Bucket;
        public String windowBucket;
        public String missBucket;
        public String teacherMatrixStatus;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherMatrixSummary;
        public String teacherEvidenceText;
        public String teacherCounterText;
        public double teacherMatrixScore;
        public double teacherMatrixRisk;
        public double teacherMatrixConfidence;
        public int teacherMatrixSample;
        public double teacherMatrixHitRate;
        public double teacherEvidenceContribution;
        public double teacherCounterContribution;
        public String windowStateKey;
        public String dynamicWindowStateKey;
        public String probabilityWindowStateKey;
        public String decisionWindowStateKey;
        public String dynamicStatus;
        public String v20Status;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String evidenceText;
        public String counterText;
        public String decisionActionAdvice;
        public int decisionHardRisk;
        public int decisionCounterRiskHigh;
        public int decisionEvidenceStrong;
        public int decisionEvidenceElite;
        public double v20SupportScore;
        public double v20RiskScore;
        public double causeScore;
        public double positionCauseScore;
        public double numberCauseScore;
        public double counterfactualScore;
        public double thirdGuardScore;
        public double ruleHealthScore;
        public double shadowHealthScore;
        public double falseStrongRisk;
        public double seedExpansionScore;
        public double teacherRecentValidity;
        public int teacherWeakNoBetFlag;
        public int teacherMatrixOrdinaryBetBlockFlag;
        public String teacherOrdinaryBetPolicy;
        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public double probabilityFalseHighRisk;
        public int falseHighPFlag;
        public int dynamicGateFalseHighPFlag;
        public int dynamicGatePreferredEdgeBand;
        public int probabilityPreferredEdgeBand;
        public int probabilityTeacherWeakOrEmpty;
        public int probabilityV20UnknownOrEmpty;
        public int probabilityStrongCauseFlag;
        public String dynamicGateEdgePolicy;
        public String dynamicGateVersion;
        public String finalDecisionVersion;
        public String finalDecisionEdgePolicy;
        public String finalDecisionTeacherPolicy;
        public String finalDecisionV20Policy;
        public String finalDecisionBetDomain;
        public String finalDecisionRejectCode;
        public String finalDecisionShadowReason;
        public double finalDecisionCoverageRisk;
        public double finalDecisionThirdNumberQuality;
        public int finalDecisionFalseHighPFlag;
        public String dynamicClusterId;
        public String dynamicClusterStatus;
        public int dynamicClusterSample;
        public int dynamicClusterRecentSample;
        public double dynamicClusterHitRate;
        public double dynamicClusterRecentHitRate;
        public int dynamicClusterMaxLose;
        public String dynamicClusterBucket;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRisk;
        public String needSupplyBucket;
        public double counterfactualWhyNotRisk;
        public double causeAblationScore;
        public int causeAblationFalseCauseCount;
        public String counterfactualBucket;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeAction;
        public double adaptiveFreezeScore;
        public String adaptiveBucket;
        public String promotionClusterStatus;
        public String promotionAction;
        public String promotionBucket;
        public String dynamicCausalDecisionSummary;
        public double dynamicCausalDecisionQuality;
        public double dynamicCausalDecisionRisk;
        public String dynamicCausalSummary;
        public String dynamicCausalBucket;
        public String finalDecisionStatus;
        public double finalDecisionQualityScore;
        public double finalDecisionRiskScore;
        public String finalDecisionActionAdvice;
        public String adaptiveStatus;
        public String adaptiveAction;
        public String adaptiveRejectCode;
        public String shadowPromotionStatus;
        public String shadowPromotionDomain;
        public String shadowPromotionRejectCode;
        public double shadowPromoteScore;
        public double ruleDecayRisk;
        public String ruleRejectCode;
        public double thirdDragRisk;
        public String thirdRejectCode;
        public double counterfactualRisk;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double netEvidenceScore;
        public String mainChainRejectCode;
        public String fieldChainVersion;
        public String decisionGateVersion;
        public int realProbeAllowed;
        public int teacherNoSampleNeutralFlag;
        public int v20WatchNeutralFlag;
        public int adaptiveWatchNeutralFlag;

        public BetCase() {
            kind = "";
            targetIssue = "";
            baseIssue = "";
            result = "";
            missReason = "";
            stateMode = "";
            reasonSummary = "";
            finalPBucket = "";
            edgeBucket = "";
            teacherBucket = "";
            v20Bucket = "";
            windowBucket = "";
            missBucket = "";
            teacherMatrixStatus = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherMatrixSummary = "";
            teacherEvidenceText = "";
            teacherCounterText = "";
            windowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";
            decisionWindowStateKey = "";
            dynamicStatus = "";
            v20Status = "";
            evidenceText = "";
            counterText = "";
            decisionActionAdvice = "";
            teacherOrdinaryBetPolicy = "";
            dynamicGateEdgePolicy = "";
            dynamicGateVersion = "";
            finalDecisionVersion = "";
            finalDecisionEdgePolicy = "";
            finalDecisionTeacherPolicy = "";
            finalDecisionV20Policy = "";
            finalDecisionBetDomain = "";
            finalDecisionRejectCode = "";
            finalDecisionShadowReason = "";
            finalDecisionStatus = "";
            finalDecisionActionAdvice = "";
            adaptiveStatus = "";
            adaptiveAction = "";
            adaptiveRejectCode = "";
            shadowPromotionStatus = "";
            shadowPromotionRejectCode = "";
            ruleRejectCode = "";
            thirdRejectCode = "";
            mainChainRejectCode = "";
            fieldChainVersion = "";
            decisionGateVersion = "";
        }
    }

    public static final class StatBucket {
        public String name;
        public int total;
        public int hit;
        public int miss;
        public double pnl;
        public double avgFinalP;
        public double avgEdge;

        public StatBucket(String n) {
            name = n == null ? "" : n;
            total = 0;
            hit = 0;
            miss = 0;
            pnl = 0.0d;
            avgFinalP = 0.0d;
            avgEdge = 0.0d;
        }

        public void add(BetCase c) {
            if (c == null) return;
            total++;
            if (c.hit == 1 || AppConfig.RESULT_WIN.equals(c.result)) hit++;
            else miss++;
            pnl += c.pnl;
            avgFinalP += c.finalP;
            avgEdge += c.edge;
        }

        public double rate() {
            if (total <= 0) return 0.0d;
            return ((double) hit) / ((double) total);
        }

        public double avgP() {
            if (total <= 0) return 0.0d;
            return avgFinalP / (double) total;
        }

        public double avgE() {
            if (total <= 0) return 0.0d;
            return avgEdge / (double) total;
        }

        public String shortText() {
            return total + "单/中" + hit + "/胜率" + pctStatic(rate());
        }

        public String fullText() {
            return name
                    + "："
                    + total
                    + "单，中"
                    + hit
                    + "，挂"
                    + miss
                    + "，胜率="
                    + pctStatic(rate())
                    + "，均P="
                    + pctStatic(avgP())
                    + "，均Edge="
                    + signedPctStatic(avgE())
                    + "，盈亏="
                    + moneyStatic(pnl);
        }

        private static String pctStatic(double v) {
            try {
                return UiFormatter.probability(v);
            } catch (Throwable e) {
                return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
            }
        }

        private static String signedPctStatic(double v) {
            try {
                return UiFormatter.signedProbability(v);
            } catch (Throwable e) {
                String s = String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
                return v >= 0.0d ? "+" + s : s;
            }
        }

        private static String moneyStatic(double v) {
            try {
                return UiFormatter.money(v);
            } catch (Throwable e) {
                return String.valueOf(v);
            }
        }
    }

    public String buildStabilityCoreDiagnosis(android.content.Context context, DbStore store) {
        StringBuilder sb = new StringBuilder();
        try {
            Models.ServiceRuntimeState svc = new ServiceHeartbeatStore(context).snapshot();
            Models.DatabaseQueueState dbq = new Models.DatabaseQueueState();
            DatabaseCoordinator coordinator = DatabaseCoordinator.peek();
            if (coordinator != null) dbq = coordinator.snapshot();
            Models.CrashSnapshot crash = CrashRecorder.load(context);
            sb.append("【稳定性核心/当前Cohort=").append(AppConfig.ACTIVE_FIX_COHORT).append("】");
            sb.append("服务enabled=")
                    .append(svc.userEnabled)
                    .append(" heartbeatAge=")
                    .append(svc.heartbeatAgeMs)
                    .append("ms")
                    .append(" restart=")
                    .append(svc.restartCount)
                    .append(" Alarm=")
                    .append(svc.alarmStatus)
                    .append(" Job=")
                    .append(svc.jobStatus)
                    .append(" lastReceiver=")
                    .append(svc.lastReceiverAction);
            sb.append("｜DB等待写=")
                    .append(dbq.waitingWriters)
                    .append(" 后台队列=")
                    .append(dbq.backgroundQueue)
                    .append(" lastWrite=")
                    .append(dbq.lastWriteCostMs)
                    .append("ms")
                    .append(" maxWrite=")
                    .append(dbq.longestWriteCostMs)
                    .append("ms")
                    .append(" op=")
                    .append(dbq.lastOperation)
                    .append(" err=")
                    .append(dbq.lastError);
            if (crash.at > 0L) {
                sb.append("｜最后崩溃=")
                        .append(crash.errorType)
                        .append(" stage=")
                        .append(crash.stage)
                        .append(" target=")
                        .append(crash.targetIssue)
                        .append(" dbQ=")
                        .append(crash.databaseQueueLength);
            }
            if (store != null) {
                sb.append("｜WARM=").append(store.getBackgroundSliceSummary("TEACHER_WARM"));
                sb.append("｜COLD=").append(store.getBackgroundSliceSummary("TEACHER_COLD"));
                sb.append("｜DEEP=").append(store.getBackgroundSliceSummary("TEACHER_DEEP"));
                sb.append("｜ProbabilityCost=")
                        .append(store.getState("probability_budget_cost_ms", "0"))
                        .append("ms");
                sb.append("｜PumpCost=")
                        .append(store.getState("background_evidence_pump_cost_ms", "0"))
                        .append("ms");
                sb.append("｜Freq20/50/100/300=")
                        .append(store.getState("frequency_20", "0"))
                        .append("/")
                        .append(store.getState("frequency_50", "0"))
                        .append("/")
                        .append(store.getState("frequency_100", "0"))
                        .append("/")
                        .append(store.getState("frequency_300", "0"));
                sb.append("｜TeacherStage=")
                        .append(store.getState("teacher_typed_applied_count", "0"))
                        .append("/")
                        .append(store.getState("teacher_typed_input_count", "0"))
                        .append(" avg=")
                        .append(store.getState("teacher_typed_average_history_sample", "0"));
                sb.append("｜DynamicF/M/C=")
                        .append(store.getState("dynamic_domain_fine_match_count", "0"))
                        .append("/")
                        .append(store.getState("dynamic_domain_mid_match_count", "0"))
                        .append("/")
                        .append(store.getState("dynamic_domain_coarse_match_count", "0"));
                sb.append("｜Build=").append(store.checkModuleBuildConsistencyV20());
                sb.append("｜TeacherReserved=")
                        .append(store.getState(AppConfig.STATE_TEACHER_RESERVED_SLICE, "--"));
                sb.append("｜Pdist=")
                        .append(store.getState("probability_distribution_lt30", "0"))
                        .append("/")
                        .append(store.getState("probability_distribution_30_34", "0"))
                        .append("/")
                        .append(store.getState("probability_distribution_34_36", "0"))
                        .append("/")
                        .append(store.getState("probability_distribution_36_38", "0"))
                        .append("/")
                        .append(store.getState("probability_distribution_ge38", "0"));
                sb.append("｜V26P=").append(store.getState("v24_realtime_probability_best", "--"));
                sb.append("｜V26Micro=")
                        .append(store.getState(MicroWalkForwardEngine.STATE_LAST_SUMMARY, "--"));
                sb.append("｜V26Auth=")
                        .append(store.getState("v24_authorization_last_mode", "--"))
                        .append("/")
                        .append(store.getState("v24_authorization_last_reject", "--"));
            }
        } catch (Throwable e) {
            sb.append("诊断异常=").append(e.getClass().getSimpleName());
        }
        return sb.toString();
    }

    private String ageOfState(DbStore store, String key) {
        long t = parseLongSafe(getStateSafe(store, key, "0"));
        if (t <= 0L) return "--";
        long age = Math.max(0L, System.currentTimeMillis() - t);
        if (age < 60000L) return String.valueOf(age / 1000L) + "s";
        return String.valueOf(age / 60000L) + "m";
    }

    public String buildAlwaysOnClosureSummary(DbStore store) {
        if (store == null) return "常驻闭环=NO_STORE";
        Models.FrequencyState fs = null;
        try {
            fs = store.loadRollingFrequencyStateV20();
        } catch (Throwable ignore) {
        }
        if (fs == null) fs = new Models.FrequencyState();
        String build = store.checkModuleBuildConsistencyV20();
        return "质量频率闭环="
                + AppConfig.ACTIVE_QUALITY_BUILD_VERSION
                + "｜Freq20/50/100/300="
                + formatPctV20(fs.frequency20)
                + "/"
                + formatPctV20(fs.frequency50)
                + "/"
                + formatPctV20(fs.frequency100)
                + "/"
                + formatPctV20(fs.frequency300)
                + "｜BET20/50/100/300="
                + fs.betCount20
                + "/"
                + fs.betCount50
                + "/"
                + fs.betCount100
                + "/"
                + fs.betCount300
                + "｜FloorDebt="
                + fs.floorDebt20
                + "/"
                + fs.floorDebt50
                + "/"
                + fs.floorDebt100
                + "/"
                + fs.floorDebt300
                + " urgent="
                + fs.urgentWindow
                + "｜FloorPacing="
                + fs.floorPacingReason
                + " age="
                + fs.issuesSinceLastRealBet
                + " gap="
                + fs.floorPacingMinGap
                + " consecutive="
                + fs.consecutiveQualifiedFloorBets
                + "｜Teacher应用="
                + store.getState("teacher_typed_applied_count", "0")
                + "/"
                + store.getState("teacher_typed_input_count", "0")
                + " historyAvg="
                + store.getState("teacher_typed_average_history_sample", "0")
                + "｜Dynamic匹配F/M/C="
                + store.getState("dynamic_domain_fine_match_count", "0")
                + "/"
                + store.getState("dynamic_domain_mid_match_count", "0")
                + "/"
                + store.getState("dynamic_domain_coarse_match_count", "0")
                + "｜DynamicREADY="
                + store.getState("dynamic_domain_ready_count", "0")
                + "｜AnalysisProgress="
                + store.getState("analysis_progress", "0")
                + " status="
                + store.getState("analysis_status", "")
                + "｜Build="
                + build;
    }

    private static String formatPctV20(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
        }
    }
}
