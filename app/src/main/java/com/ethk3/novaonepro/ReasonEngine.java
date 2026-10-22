package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 第10/11：V20 原因解释引擎升级补丁
 *
 * <p>本文件只负责把前面 1-9 文件写入的可计算证据转成 UI 可读原因： - WindowState：12/24/48/96/360 窗口状态指纹 - 证据贡献 / 反证贡献 - 样本可信
 * / 近期有效性 - 相似窗口样本 / 特征距离 - 为什么中 / 为什么不中 / 为什么不出手 / 是否该出手
 *
 * <p>红线：ReasonEngine 不生成 BET，不结算，不写真实胜率，不写真实盈亏， 不绕过 DecisionGateEngine。
 */
public final class ReasonEngine {
    private static final String V20_REASON_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_REASON_MAIN_CHAIN_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_REASON_CLOSED_LOOP_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_REASON_NO_BET_OUTPUT_ONLY_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final int DETAIL_LIMIT = 360;
    private static final int SUMMARY_LIMIT = 620;
    private static final int LONG_DETAIL_LIMIT = 860;
    private static final int DB_STATE_DETAIL_LIMIT = 1000;

    public ReasonEngine() {}

    public ArrayList<Models.ReasonItem> buildDecisionReasons(Models.Decision decision) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        if (decision == null) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_DATA_ERROR,
                    "决策为空",
                    "没有生成有效决策，系统保护为不出手；不会产生真实BET，不计真实胜率，不计真实盈亏",
                    0.0d);
            return out;
        }
        Models.Decision d = copyDecisionForReason(decision);
        normalizeDecisionForReason(d);
        addDecisionHeader(out, d);
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            buildBetDecisionReasons(out, d);
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            buildShadowDecisionReasons(out, d);
        } else {
            buildHoldDecisionReasons(out, d);
        }
        addProbabilityBreakdown(out, d);
        addShapeBreakdown(out, d);
        addRiskBreakdown(out, d);
        addFrequencyBreakdown(out, d);
        addLearningBreakdown(out, d);
        addTeacherMatrixDecisionReason(out, d);
        addV20CauseChainDecisionReason(out, d);
        addV20EvidenceDecisionReason(out, d);
        addMainChainDecisionReason(out, d);
        addDeepDecisionReason(out, d);
        addAuditDecisionReason(out, d);
        addBacktestDecisionReason(out, d);
        addScopeReason(out, d);
        addFinalDecisionConclusion(out, d);
        trim(out);
        return out;
    }

    public ArrayList<Models.ReasonItem> buildCandidateReasons(
            Models.ComboCandidate candidate,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            CalibrationEngine.CalibrationSignal calibration,
            DynamicGateEngine.GateSignal gate) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        if (candidate == null || !candidate.isValidK3()) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_COMBO_WEAK,
                    "候选无效",
                    "候选为空，或者不是固定K3组合，不能进入真实出手；系统只允许单位置 + 固定K3",
                    0.0d);
            return out;
        }
        addCandidateBase(out, candidate);
        addProbabilityReason(out, candidate);
        addCandidateProbabilityBreakdown(out, candidate);
        addStateReason(out, candidate);
        addCandidateRiskBreakdown(out, candidate);
        addTrendReason(out, trace);
        addRecoveryReason(out, recovery);
        addSimilarReason(out, similar);
        addMemoryReason(out, memory);
        addCalibrationReason(out, calibration);
        addGateReason(out, gate);
        addDeepCandidateReason(out, candidate);
        addCandidateLearningReason(out, candidate);
        addTeacherMatrixCandidateReason(out, candidate);
        addV20CandidateCauseChainReason(out, candidate);
        addV20EvidenceCandidateReason(out, candidate);
        addMainChainCandidateReason(out, candidate);
        addFinalConclusion(out, candidate, gate);
        trim(out);
        return out;
    }

    public ArrayList<Models.ReasonItem> buildHoldReasons(String blockCode, String reasonSummary) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        String code =
                blockCode == null || blockCode.length() == 0
                        ? AppConfig.BLOCK_LOW_PROBABILITY
                        : blockCode;
        String detail =
                reasonSummary == null || reasonSummary.length() == 0
                        ? "未满足真实出手条件"
                        : displayReason(reasonSummary);
        add(out, AppConfig.REASON_BLOCK, code, "最终不出手", detail, 0.0d);
        add(
                out,
                AppConfig.REASON_INFO,
                "STAT_SCOPE",
                "统计口径",
                "HOLD不下注，不计真实胜率，不计真实盈亏；只保留原因给界面展示，不写入真实交易",
                0.0d);
        return out;
    }

    public ArrayList<Models.ReasonItem> buildRuntimeReasons(Models.AppRuntimeState state) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        if (state == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_DATA_ERROR,
                    "运行状态为空",
                    "后台状态没有读取到，等待下一次刷新",
                    0.0d);
            return out;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "RUNTIME",
                "运行状态",
                "服务="
                        + UiFormatter.serviceStatusText(state.serviceStatus)
                        + "，接口="
                        + UiFormatter.apiLineText(state.apiLine)
                        + "，当前期="
                        + safe(state.currentIssue)
                        + "，目标期="
                        + safe(state.targetIssue)
                        + "，倒计时="
                        + UiFormatter.countdown(state.countdownMs),
                0.0d);
        if (state.schedulerSummary != null && state.schedulerSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SCHEDULER",
                    "调度状态",
                    displayReasonLong(state.schedulerSummary),
                    0.0d);
        }
        if (state.backtestSummary != null && state.backtestSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "BACKTEST",
                    "回测状态",
                    displayReasonLong(state.backtestSummary),
                    0.0d);
        }
        if (state.manualBacktestSummary != null && state.manualBacktestSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "MANUAL_BACKTEST",
                    "手动回测",
                    displayReasonLong(state.manualBacktestSummary),
                    0.0d);
        }
        if (state.learningSummary != null && state.learningSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "LEARNING",
                    "学习状态",
                    displayReasonLong(state.learningSummary),
                    0.0d);
        }
        addRuntimeTeacherMatrixBreakdown(out, state);
        addRuntimeV20CauseChainBreakdown(out, state);
        addRuntimeV20EvidenceBreakdown(out, state);
        addRuntimeMainChainReason(out, state);
        if (state.biasSummary != null && state.biasSummary.length() > 0) {
            add(out, AppConfig.REASON_INFO, "BIAS", "偏差状态", displayReason(state.biasSummary), 0.0d);
        }
        if (state.auditSummary != null && state.auditSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "AUDIT",
                    "概率审计",
                    displayReason(state.auditSummary),
                    0.0d);
        }
        if (state.aiJudgeSummary != null && state.aiJudgeSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "AI_JUDGE",
                    "AI裁决",
                    displayReason(state.aiJudgeSummary),
                    0.0d);
        }
        if (state.promotionSummary != null && state.promotionSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "PROMOTION",
                    "晋级状态",
                    displayReason(state.promotionSummary),
                    0.0d);
        }
        if (state.aPlusSummary != null && state.aPlusSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "A_PLUS",
                    "A+状态",
                    displayReason(state.aPlusSummary),
                    0.0d);
        }
        if (state.selfDiagnosisSummary != null && state.selfDiagnosisSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SELF_DIAGNOSIS",
                    "自诊断",
                    displayReason(state.selfDiagnosisSummary),
                    0.0d);
        }
        if (state.lastError != null && state.lastError.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_DATA_ERROR,
                    "最近错误",
                    displayReason(state.lastError),
                    0.0d);
        }
        trim(out);
        return out;
    }

    public ArrayList<Models.ReasonItem> buildLearningRuntimeReasons(DbStore store) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        if (store == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_DATA_ERROR,
                    "学习状态",
                    "DbStore为空，无法读取自动回测、学习、教师矩阵、教师快照状态",
                    0.0d);
            return out;
        }
        String autoStatus = readState(store, "auto_learn_status", "IDLE");
        String autoSummary = readState(store, "auto_learn_summary", "");
        String learningSummary = readState(store, "learning_summary", "");
        String learningDetail = readState(store, "learning_reason_detail", "");
        String runtimeProfile = readState(store, "runtime_profile_summary", "");
        String tmStatus =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_status", ""),
                        readState(store, "teacher_matrix_status", ""),
                        readState(store, "teacher_number_matrix_realtime_status", ""),
                        "IDLE");
        String tmReady =
                firstNonEmpty(
                        readState(store, "teacher_matrix_ready_status", ""),
                        readState(store, "teacher_number_matrix_ready_status", ""),
                        readState(store, "teacher_matrix_candidate_apply", ""),
                        "");
        String tmIssue =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_issue", ""),
                        readState(store, "teacher_matrix_last_issue", ""),
                        readState(store, "teacher_matrix_issue", ""),
                        "");
        String tmTargetIssue =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_target_issue", ""),
                        readState(store, "teacher_matrix_target_issue", ""),
                        "");
        String tmCells =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_profile_count", ""),
                        readState(store, "teacher_matrix_profile_count", ""),
                        readState(store, "teacher_matrix_last_cells", ""),
                        "0");
        String tmSamples =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_sample_count", ""),
                        readState(store, "teacher_matrix_sample_count", ""),
                        readState(store, "teacher_matrix_candidate_avg_sample", ""),
                        "0");
        String tmProfiles =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_profile_count", ""),
                        readState(store, "teacher_matrix_profile_count", ""),
                        "0");
        String tmRealtimeApplied =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_realtime_applied", ""),
                        readState(store, "teacher_matrix_candidate_apply_count", ""),
                        "");
        String tmRealtimeAvgSample =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_realtime_avg_sample", ""),
                        readState(store, "teacher_matrix_candidate_avg_sample", ""),
                        "");
        String tmStrong =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_strong_profile_count", ""),
                        readState(store, "teacher_matrix_strong_profile_count", ""),
                        readState(store, "teacher_number_matrix_realtime_strong", ""),
                        readState(store, "teacher_matrix_candidate_strong", ""),
                        "0");
        String tmRisk =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_risk_profile_count", ""),
                        readState(store, "teacher_matrix_risk_profile_count", ""),
                        readState(store, "teacher_number_matrix_realtime_risk", ""),
                        readState(store, "teacher_matrix_candidate_risk", ""),
                        "0");
        String tmConfidence =
                firstNonEmpty(
                        readState(store, "teacher_matrix_last_confidence_text", ""),
                        readState(store, "teacher_number_matrix_confidence_text", ""),
                        "");
        String tmSummary =
                firstNonEmpty(
                        readState(store, "teacher_number_matrix_realtime_summary", ""),
                        readState(store, "teacher_matrix_candidate_summary", ""),
                        readState(store, "teacher_number_matrix_summary", ""),
                        readState(store, "teacher_matrix_summary", ""),
                        "");
        String snapStatus = readState(store, "teacher_snapshot_status", "IDLE");
        String snapId = readState(store, "teacher_snapshot_id", "");
        String snapIssue = readState(store, "teacher_snapshot_issue", "");
        String snapSummary = readState(store, "teacher_snapshot_summary", "");
        String serviceCore = readState(store, "service_core_summary", "");
        String causeSummary =
                firstNonEmpty(
                        readState(store, "dynamic_cause_chain_summary", ""),
                        readState(store, "dynamicCauseChainSummary", ""),
                        readState(store, "cause_chain_summary", ""),
                        readState(store, "v20_cause_chain_summary", ""),
                        "");
        String ruleSummary =
                firstNonEmpty(
                        readState(store, "rule_health_summary", ""),
                        readState(store, "shadow_health_summary", ""),
                        readState(store, "false_strong_freeze_summary", ""),
                        readState(store, "third_guard_summary", ""),
                        "");
        add(
                out,
                AppConfig.REASON_INFO,
                "AUTO_LEARN_STATE",
                "自动学习状态",
                "状态="
                        + autoStatus
                        + "；摘要="
                        + displayReasonLong(
                                firstNonEmpty(autoSummary, learningSummary, "暂无自动学习摘要")),
                0.0d);
        add(
                out,
                ("OK".equals(tmStatus) || "DONE".equals(tmStatus))
                        ? AppConfig.REASON_PASS
                        : AppConfig.REASON_INFO,
                "TEACHER_MATRIX_STATE",
                "教师矩阵状态",
                "状态="
                        + tmStatus
                        + "；可用级别="
                        + firstNonEmpty(tmReady, "未标记")
                        + "；最近开奖="
                        + firstNonEmpty(tmIssue, "--")
                        + "；目标="
                        + firstNonEmpty(tmTargetIssue, "--")
                        + "；矩阵格="
                        + tmCells
                        + "；样本="
                        + tmSamples
                        + "；画像="
                        + tmProfiles
                        + "；强="
                        + tmStrong
                        + "；风险="
                        + tmRisk
                        + "；实时应用="
                        + firstNonEmpty(tmRealtimeApplied, "--")
                        + "；实时均样本="
                        + firstNonEmpty(tmRealtimeAvgSample, "--")
                        + "；置信="
                        + firstNonEmpty(tmConfidence, "--")
                        + "；说明="
                        + displayReasonLong(firstNonEmpty(tmSummary, "暂无教师矩阵摘要")),
                parseDouble(tmConfidence));
        add(
                out,
                ("OK".equals(snapStatus) || "DONE".equals(snapStatus))
                        ? AppConfig.REASON_PASS
                        : AppConfig.REASON_INFO,
                "TEACHER_SNAPSHOT_STATE",
                "教师快照",
                "状态="
                        + snapStatus
                        + "；snapshot="
                        + firstNonEmpty(snapId, "--")
                        + "；期号="
                        + firstNonEmpty(snapIssue, "--")
                        + "；说明="
                        + displayReasonLong(firstNonEmpty(snapSummary, "暂无教师快照摘要")),
                0.0d);
        if (runtimeProfile != null && runtimeProfile.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "RUNTIME_PROFILE",
                    "实时画像反哺",
                    displayReasonLong(runtimeProfile),
                    0.0d);
        }
        if (learningDetail != null && learningDetail.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "LEARNING_DETAIL",
                    "学习反推明细",
                    displayReasonLong(learningDetail),
                    0.0d);
        }
        if (causeSummary != null && causeSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_CAUSE_CHAIN_STATE",
                    "V20因链状态",
                    displayReasonLong(causeSummary),
                    0.0d);
        }
        if (ruleSummary != null && ruleSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_HEALTH_STATE",
                    "V20健康门状态",
                    displayReasonLong(ruleSummary),
                    0.0d);
        }
        if (serviceCore != null && serviceCore.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SERVICE_CORE",
                    "核心闭环",
                    displayReasonLong(serviceCore),
                    0.0d);
        }
        String realBetDiagSummary =
                firstNonEmpty(
                        readState(store, "real_bet_diagnosis_summary", ""),
                        readState(store, "real_bet_diagnosis_detail", ""),
                        "");
        if (realBetDiagSummary != null && realBetDiagSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "REAL_BET_DIAGNOSIS_STATE",
                    "真实BET诊断",
                    displayReasonLong(realBetDiagSummary),
                    0.0d);
        }
        String v24Probability = readState(store, "v24_realtime_probability_best", "--");
        String v24Micro = readState(store, MicroWalkForwardEngine.STATE_LAST_SUMMARY, "--");
        String v24Auth =
                "mode="
                        + readState(store, "v24_authorization_last_mode", "--")
                        + "；reject="
                        + readState(store, "v24_authorization_last_reject", "--")
                        + "；P="
                        + readState(store, "v24_authorization_last_probability", "--")
                        + "；Gate="
                        + readState(store, "v24_authorization_last_gate", "--")
                        + "；Edge="
                        + readState(store, "v24_authorization_last_edge", "--");
        add(
                out,
                AppConfig.REASON_INFO,
                "V26_OOS_AUTHORIZATION_STATE",
                "V40事件概率、行动效用与唯一授权",
                displayReasonLong(
                        "概率="
                                + v24Probability
                                + "；微型走前="
                                + v24Micro
                                + "；授权="
                                + v24Auth
                                + "；rawRankScore只排序，FinalP只允许严格OOS单调校准；WATCH/HARD_RISK/版本或Contract不一致必须为SHADOW。"),
                0.0d);
        add(
                out,
                AppConfig.REASON_INFO,
                "TEACHER_RULE",
                "教师矩阵规则",
                "教师矩阵、自动回测、学习反推、因链矩阵、规则健康、影子健康只做画像、风险、解释和小幅反哺；不能越过FinalP、RawP、动态门槛和DecisionGate；不能救援BET；不能污染真实校准；真实胜率和盈亏只统计REAL_TRADE"
                    + " + BET + 已结算WIN/MISS。",
                0.0d);
        trim(out);
        return out;
    }

    public String buildLearningRuntimeSummary(DbStore store) {
        ArrayList<Models.ReasonItem> list = buildLearningRuntimeReasons(store);
        return joinReasonsLong(list);
    }

    public ArrayList<Models.ReasonItem> buildRealBetDiagnosisReasons(DbStore store) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        if (store == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    "REAL_BET_DIAG_STORE_EMPTY",
                    "真实BET诊断",
                    "DbStore为空，无法读取真实BET、SHADOW、Decision、Trade和开奖；诊断不参与出号、不参与结算、不写真实盈亏。",
                    0.0d);
            return out;
        }
        RealBetDiagnosisEngine.DiagnosisReport report = null;
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            report = engine.saveDiagnosisState(store);
        } catch (Throwable e) {
            report = null;
        }
        if (report == null) {
            String summary =
                    firstNonEmpty(
                            readState(store, "real_bet_diagnosis_summary", ""), "真实BET诊断暂未生成");
            add(
                    out,
                    AppConfig.REASON_WARN,
                    "REAL_BET_DIAG_EMPTY",
                    "真实BET诊断",
                    displayReasonLong(summary),
                    0.0d);
            return out;
        }
        String level = AppConfig.REASON_INFO;
        if (report.totalBet >= 20 && report.winRate < 0.300d) {
            level = AppConfig.REASON_BLOCK;
        } else if (report.totalBet >= 20 && report.winRate >= 0.370d) {
            level = AppConfig.REASON_PASS;
        } else if (report.totalBet >= 10 && report.winRate < 0.300d) {
            level = AppConfig.REASON_WARN;
        }
        add(
                out,
                level,
                "REAL_BET_DIAG_OVERVIEW",
                "真实BET复盘总览",
                "最近真实BET="
                        + report.totalBet
                        + "单，中="
                        + report.hitBet
                        + "，挂="
                        + report.missBet
                        + "，等待="
                        + report.waitBet
                        + "，胜率="
                        + UiFormatter.probability(report.winRate)
                        + "，盈亏="
                        + UiFormatter.money(report.pnl)
                        + "，均FinalP="
                        + UiFormatter.probability(report.avgFinalP)
                        + "，均Gate="
                        + UiFormatter.probability(report.avgGate)
                        + "，均Edge="
                        + UiFormatter.signedProbability(report.avgEdge)
                        + "。本诊断只读数据，不改变出号、门槛、结算和真实盈亏。",
                report.winRate);
        add(
                out,
                (report.v24UnauthorizedBetCount
                                        + report.v24WatchBetCount
                                        + report.v24HardRiskBetCount
                                        + report.v24VersionMismatchBetCount)
                                == 0
                        ? AppConfig.REASON_PASS
                        : AppConfig.REASON_BLOCK,
                "V26_AUTHORIZATION_AUDIT",
                "V40授权穿透验收",
                "未授权BET="
                        + report.v24UnauthorizedBetCount
                        + "，WATCH BET="
                        + report.v24WatchBetCount
                        + "，HARD_RISK BET="
                        + report.v24HardRiskBetCount
                        + "，版本错配BET="
                        + report.v24VersionMismatchBetCount
                        + "；概率="
                        + displayReasonLong(report.v24Probability)
                        + "；微型走前="
                        + displayReasonLong(report.v24MicroWalkForward),
                report.v24UnauthorizedBetCount
                        + report.v24WatchBetCount
                        + report.v24HardRiskBetCount
                        + report.v24VersionMismatchBetCount);
        if (report.totalBet < 20) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    "REAL_BET_DIAG_SAMPLE_SMALL",
                    "样本偏少",
                    "真实BET样本少于20单，当前胜率只能作为报警和排错线索，不能直接当长期结论；但如果FinalP、教师矩阵、V20强信号已经集中亏损，仍然要优先排查假强。",
                    report.totalBet);
        }
        if (report.bestPosition != null && report.bestPosition.total > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_BEST_POSITION",
                    "相对较好位置",
                    report.bestPosition.fullText(),
                    report.bestPosition.rate());
        }
        if (report.worstPosition != null && report.worstPosition.total > 0) {
            add(
                    out,
                    report.worstPosition.total >= 3 && report.worstPosition.rate() < 0.300d
                            ? AppConfig.REASON_WARN
                            : AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_WORST_POSITION",
                    "拖后腿位置",
                    report.worstPosition.fullText() + "；如果这个位置持续低于整体，需要下一步查位置因链和位置冻结，不要全局降门槛。",
                    report.worstPosition.rate());
        }
        if (report.bestFinalPBucket != null && report.bestFinalPBucket.total > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_BEST_FINALP",
                    "较有效FinalP区间",
                    report.bestFinalPBucket.fullText(),
                    report.bestFinalPBucket.rate());
        }
        if (report.worstFinalPBucket != null && report.worstFinalPBucket.total > 0) {
            add(
                    out,
                    report.worstFinalPBucket.total >= 3 && report.worstFinalPBucket.rate() < 0.300d
                            ? AppConfig.REASON_WARN
                            : AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_WORST_FINALP",
                    "疑似虚高FinalP区间",
                    report.worstFinalPBucket.fullText()
                            + "；如果高FinalP也不中，下一步优先修ProbabilityEngine/Teacher/因链贡献权重。",
                    report.worstFinalPBucket.rate());
        }
        if (report.bestTeacherBucket != null && report.bestTeacherBucket.total > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_TEACHER_BEST",
                    "教师矩阵相对有效区间",
                    report.bestTeacherBucket.fullText(),
                    report.bestTeacherBucket.rate());
        }
        if (report.worstTeacherBucket != null && report.worstTeacherBucket.total > 0) {
            add(
                    out,
                    report.worstTeacherBucket.total >= 3
                                    && report.worstTeacherBucket.rate() < 0.300d
                            ? AppConfig.REASON_WARN
                            : AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_TEACHER_WORST",
                    "教师矩阵疑似假强",
                    report.worstTeacherBucket.fullText()
                            + "；若STRONG/READY仍低胜率，不能再让教师矩阵抬FinalP，应修TeacherNumberMatrix样本匹配和近期有效性。",
                    report.worstTeacherBucket.rate());
        }
        if (report.bestV20Bucket != null && report.bestV20Bucket.total > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_V20_BEST",
                    "V20因链相对有效区间",
                    report.bestV20Bucket.fullText(),
                    report.bestV20Bucket.rate());
        }
        if (report.worstV20Bucket != null && report.worstV20Bucket.total > 0) {
            add(
                    out,
                    report.worstV20Bucket.total >= 3 && report.worstV20Bucket.rate() < 0.300d
                            ? AppConfig.REASON_WARN
                            : AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_V20_WORST",
                    "V20因链疑似假强",
                    report.worstV20Bucket.fullText()
                            + "；若强因链低胜率，说明解释强不等于预测强，下一步修DynamicCauseChain/Probability。",
                    report.worstV20Bucket.rate());
        }
        if (report.topMissBucket != null && report.topMissBucket.total > 0) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    "REAL_BET_DIAG_TOP_MISS",
                    "主要错因",
                    report.topMissBucket.fullText() + "；错因用于判断是位置错、K3覆盖错、趋势破、漂移高、假恢复还是样本弱。",
                    report.topMissBucket.total);
        }
        if (report.shadowTotal > 0) {
            String shadowLevel = AppConfig.REASON_INFO;
            if (report.shadowTotal >= 10 && report.shadowHitRate > report.winRate + 0.05d) {
                shadowLevel = AppConfig.REASON_WARN;
            }
            add(
                    out,
                    shadowLevel,
                    "REAL_BET_DIAG_SHADOW_COUNTERFACTUAL",
                    "SHADOW反事实",
                    "最近SHADOW="
                            + report.shadowTotal
                            + "，可对照命中="
                            + report.shadowHit
                            + "，对照胜率="
                            + UiFormatter.probability(report.shadowHitRate)
                            + "；真实BET胜率="
                            + UiFormatter.probability(report.winRate)
                            + "。如果SHADOW长期高于BET，说明DecisionGate可能错杀强单或放行结构错位。",
                    report.shadowHitRate);
        }
        if (report.mainProblem != null && report.mainProblem.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    "REAL_BET_DIAG_MAIN_PROBLEM",
                    "核心问题判断",
                    displayReasonLong(report.mainProblem),
                    0.0d);
        }
        if (report.nextFixAdvice != null && report.nextFixAdvice.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "REAL_BET_DIAG_NEXT_FIX",
                    "下一步修复方向",
                    displayReasonLong(report.nextFixAdvice),
                    0.0d);
        }
        trim(out);
        return out;
    }

    public String buildRealBetDiagnosisSummary(DbStore store) {
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            RealBetDiagnosisEngine.DiagnosisReport report = engine.saveDiagnosisState(store);
            if (report != null && report.summary != null && report.summary.length() > 0) {
                return shortText(displayReasonLong(report.summary), DB_STATE_DETAIL_LIMIT);
            }
        } catch (Throwable e) {
        }
        return store == null
                ? "真实BET诊断暂不可用"
                : shortText(
                        readState(store, "real_bet_diagnosis_summary", "真实BET诊断暂未生成"),
                        DB_STATE_DETAIL_LIMIT);
    }

    public String buildRealBetDiagnosisDetail(DbStore store) {
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            RealBetDiagnosisEngine.DiagnosisReport report = engine.saveDiagnosisState(store);
            if (report != null && report.detail != null && report.detail.length() > 0) {
                return shortText(report.detail, 12000);
            }
        } catch (Throwable e) {
        }
        return store == null
                ? "真实BET诊断暂不可用"
                : readState(store, "real_bet_diagnosis_detail", "真实BET诊断暂未生成");
    }

    public String buildRealBetDiagnosisCopyText(DbStore store) {
        String detail = buildRealBetDiagnosisDetail(store);
        if (detail == null || detail.length() == 0) {
            return "V20真实BET诊断暂未生成";
        }
        return detail;
    }

    public ArrayList<Models.ReasonItem> buildTradeResultReasons(Models.Trade trade) {
        ArrayList<Models.ReasonItem> out = new ArrayList<Models.ReasonItem>();
        if (trade == null) {
            add(out, AppConfig.REASON_INFO, "TRADE_EMPTY", "暂无结算", "当前没有真实BET结算记录", 0.0d);
            return out;
        }
        Models.Trade t = copyTradeForReason(trade);
        normalizeTradeForReason(t);
        String result = t.result == null ? AppConfig.RESULT_WAIT : t.result;
        addSettlementRuleReason(out, t);
        if (AppConfig.RESULT_WIN.equals(result)) {
            add(
                    out,
                    AppConfig.REASON_PASS,
                    AppConfig.RESULT_WIN,
                    "真实BET命中",
                    "目标期="
                            + safe(t.targetIssue)
                            + "，只看第"
                            + t.position
                            + "位置"
                            + "，预测K3="
                            + UiFormatter.numberGroup(t.n1, t.n2, t.n3)
                            + "，实际="
                            + UiFormatter.number(t.actualNumber)
                            + "。实际号码等于本位置预测三号之一，所以命中；盈亏="
                            + UiFormatter.money(t.pnl),
                    t.pnl);
            add(out, AppConfig.REASON_PASS, "HIT_LEARNING", "命中学习", buildHitLearningText(t), t.pnl);
        } else if (AppConfig.RESULT_MISS.equals(result)) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    t.missReason == null || t.missReason.length() == 0
                            ? AppConfig.MISS_UNKNOWN
                            : t.missReason,
                    "真实BET未中",
                    "目标期="
                            + safe(t.targetIssue)
                            + "，只看第"
                            + t.position
                            + "位置"
                            + "，预测K3="
                            + UiFormatter.numberGroup(t.n1, t.n2, t.n3)
                            + "，实际="
                            + UiFormatter.number(t.actualNumber)
                            + "。实际号码没有等于预测三号之一，所以未中；错因="
                            + UiFormatter.missReasonText(t.missReason)
                            + "；盈亏="
                            + UiFormatter.money(t.pnl),
                    t.pnl);
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "MISS_REVERSE_REASON",
                    "未中反推",
                    buildMissReverseText(t),
                    t.pnl);
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "MISS_LEARNING",
                    "未中学习",
                    buildMissLearningText(t),
                    t.pnl);
        } else if (AppConfig.RESULT_DATA_ERROR.equals(result)) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_DATA_ERROR,
                    "结算数据异常",
                    "目标期=" + safe(t.targetIssue) + "，开奖数据或目标位置数据异常，不进入学习，避免污染模型",
                    0.0d);
        } else if (AppConfig.RESULT_VOID.equals(result)) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.RESULT_VOID,
                    "交易作废",
                    "该交易被作废，不计真实胜率，不计真实盈亏",
                    0.0d);
        } else {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    AppConfig.RESULT_WAIT,
                    "等待结算",
                    "目标期="
                            + safe(t.targetIssue)
                            + "，第"
                            + t.position
                            + "位置 "
                            + UiFormatter.numberGroup(t.n1, t.n2, t.n3)
                            + "，等待目标期开奖后按单位置K3结算",
                    0.0d);
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "TRADE_PROBABILITY",
                "投注时概率",
                "FinalP="
                        + UiFormatter.probability(t.finalP)
                        + "，Gate="
                        + UiFormatter.probability(t.dynamicGate)
                        + "，Edge="
                        + UiFormatter.signedProbability(t.edge)
                        + "，RawP="
                        + UiFormatter.probability(t.rawP)
                        + "，CalP="
                        + UiFormatter.probability(t.calibratedP),
                t.finalP);
        if (t.reasonSummary != null && t.reasonSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "TRADE_REASON",
                    "出手时原因",
                    displayReasonLong(t.reasonSummary),
                    0.0d);
        }
        addV20EvidenceTradeReason(out, t);
        addMainChainTradeReason(out, t);
        trim(out);
        return out;
    }

    public ArrayList<Models.ReasonItem> buildSettlementReasons(Models.Trade trade) {
        return buildTradeResultReasons(trade);
    }

    public ArrayList<Models.ReasonItem> buildTradeReasons(Models.Trade trade) {
        return buildTradeResultReasons(trade);
    }

    public String buildDecisionSummary(Models.Decision decision) {
        if (decision == null) {
            return "暂无决策";
        }
        Models.Decision d = copyDecisionForReason(decision);
        normalizeDecisionForReason(d);
        StringBuilder sb = new StringBuilder();
        String decisionText = UiFormatter.decisionText(d.decision);
        sb.append(decisionText);
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            sb.append("：第");
            sb.append(d.position);
            sb.append("位置 ");
            sb.append(UiFormatter.numberGroup(d.n1, d.n2, d.n3));
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)
                && d.position > 0
                && Models.isValidK3Mask(d.k3Mask)) {
            sb.append("：影子观察 第");
            sb.append(d.position);
            sb.append("位置 ");
            sb.append(UiFormatter.numberGroup(d.n1, d.n2, d.n3));
        } else if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
            sb.append("：观察候选 第");
            sb.append(d.position);
            sb.append("位置 ");
            sb.append(UiFormatter.numberGroup(d.n1, d.n2, d.n3));
        }
        sb.append("，最终概率=");
        sb.append(UiFormatter.probability(d.finalP));
        sb.append("，出手门槛=");
        sb.append(UiFormatter.probability(d.dynamicGate));
        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }
        sb.append("，概率优势=");
        sb.append(UiFormatter.signedProbability(edge));
        if (d.blockCode != null && d.blockCode.length() > 0) {
            sb.append("，拦截原因=");
            sb.append(UiFormatter.blockReasonText(d.blockCode));
        }
        if (d.freq100 > 0.0d) {
            sb.append("，近100频率=");
            sb.append(UiFormatter.probability(d.freq100));
            sb.append("（风控参考）");
        }
        TeacherMatrixView tm = readTeacherMatrixView(d);
        if (tm.hasAny()) {
            sb.append("，教师矩阵=");
            sb.append(firstNonEmpty(tm.status, "已反哺"));
            if (tm.sample > 0) {
                sb.append("/");
                sb.append(tm.sample);
            }
            if (tm.confidence > 0.0d) {
                sb.append("/");
                sb.append(UiFormatter.probability(tm.confidence));
            }
        }
        V20View v20 = readV20View(d);
        V20EvidenceView ev = readV20EvidenceView(d);
        if (v20.hasAny()) {
            sb.append("，因链=");
            if (v20.status.length() > 0) {
                sb.append(displayReason(v20.status));
            } else {
                sb.append("support=");
                sb.append(UiFormatter.probability(v20.supportScore));
                sb.append("/risk=");
                sb.append(UiFormatter.probability(v20.riskScore));
            }
        }
        if (ev.hasAny()) {
            sb.append("，证据=");
            sb.append(UiFormatter.probability(ev.evidenceContribution));
            sb.append("/反证=");
            sb.append(UiFormatter.probability(ev.counterContribution));
            sb.append("/样本=");
            sb.append(UiFormatter.probability(ev.sampleReliability));
        }
        if (d.reasonSummary != null && d.reasonSummary.length() > 0) {
            sb.append("，说明=");
            sb.append(displayReason(shortText(d.reasonSummary, 150)));
        }
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String buildDecisionDetailText(Models.Decision decision) {
        ArrayList<Models.ReasonItem> list = buildDecisionReasons(decision);
        return joinReasons(list);
    }

    public String buildCandidateSummary(Models.ComboCandidate c) {
        if (c == null) {
            return "候选为空";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("第");
        sb.append(c.position);
        sb.append("位置 ");
        sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
        sb.append("，最终概率=");
        sb.append(UiFormatter.probability(c.finalP));
        sb.append("，出手门槛=");
        sb.append(UiFormatter.probability(c.dynamicGate));
        double edge = c.edge;
        if (edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            edge = c.finalP - c.dynamicGate;
        }
        sb.append("，概率优势=");
        sb.append(UiFormatter.signedProbability(edge));
        sb.append("，状态=");
        sb.append(UiFormatter.modeText(c.stateMode));
        TeacherMatrixView tm = readTeacherMatrixView(c);
        if (tm.hasAny()) {
            sb.append("，教师矩阵=");
            sb.append(firstNonEmpty(tm.status, "已反哺"));
            if (tm.sample > 0) {
                sb.append("/");
                sb.append(tm.sample);
            }
            if (tm.confidence > 0.0d) {
                sb.append("/");
                sb.append(UiFormatter.probability(tm.confidence));
            }
        }
        V20View v20 = readV20View(c);
        V20EvidenceView ev = readV20EvidenceView(c);
        if (v20.hasAny()) {
            sb.append("，因链=");
            if (v20.status.length() > 0) {
                sb.append(displayReason(v20.status));
            } else {
                sb.append(UiFormatter.probability(v20.supportScore));
                sb.append("/");
                sb.append(UiFormatter.probability(v20.riskScore));
            }
        }
        if (ev.hasAny()) {
            sb.append("，证据=");
            sb.append(UiFormatter.probability(ev.evidenceContribution));
            sb.append("/反证=");
            sb.append(UiFormatter.probability(ev.counterContribution));
            sb.append("/建议=");
            sb.append(shortText(firstNonEmpty(ev.actionAdvice, "观察"), 34));
        }
        if (c.blockCode != null && c.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(c.blockCode));
        }
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String joinReasons(List<Models.ReasonItem> reasons) {
        if (reasons == null || reasons.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(reasons.size(), AppConfig.REASON_SHOW_LIMIT);
        for (int i = 0; i < limit; i++) {
            Models.ReasonItem r = reasons.get(i);
            if (r == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(r.title == null ? "" : r.title);
            sb.append("：");
            sb.append(r.detail == null ? "" : displayReason(r.detail));
        }
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String joinReasonsLong(List<Models.ReasonItem> reasons) {
        if (reasons == null || reasons.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(reasons.size(), AppConfig.REASON_SHOW_LIMIT);
        for (int i = 0; i < limit; i++) {
            Models.ReasonItem r = reasons.get(i);
            if (r == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(r.title == null ? "" : r.title);
            sb.append("：");
            sb.append(r.detail == null ? "" : displayReason(r.detail));
        }
        return shortText(sb.toString(), DB_STATE_DETAIL_LIMIT);
    }

    public String topBlockCode(List<Models.ReasonItem> reasons) {
        if (reasons == null || reasons.size() == 0) {
            return "";
        }
        for (int i = 0; i < reasons.size(); i++) {
            Models.ReasonItem r = reasons.get(i);
            if (r == null) {
                continue;
            }
            if (AppConfig.REASON_BLOCK.equals(r.level)) {
                return r.code == null ? "" : r.code;
            }
        }
        return "";
    }

    public String buildResultReason(Models.Trade trade) {
        if (trade == null) {
            return "暂无真实BET结算";
        }
        Models.Trade t = copyTradeForReason(trade);
        normalizeTradeForReason(t);
        StringBuilder sb = new StringBuilder();
        sb.append(UiFormatter.resultText(t.result));
        sb.append("：目标期=");
        sb.append(safe(t.targetIssue));
        sb.append("，只看第");
        sb.append(t.position);
        sb.append("位置，预测K3=");
        sb.append(UiFormatter.numberGroup(t.n1, t.n2, t.n3));
        if (t.actualNumber > 0) {
            sb.append("，实际=");
            sb.append(UiFormatter.number(t.actualNumber));
        }
        sb.append("，盈亏=");
        sb.append(UiFormatter.money(t.pnl));
        if (AppConfig.RESULT_WIN.equals(t.result)) {
            sb.append("，原因=目标位置实际号码等于预测三号之一");
        }
        if (AppConfig.RESULT_MISS.equals(t.result)) {
            sb.append("，错因=");
            sb.append(UiFormatter.missReasonText(t.missReason));
        }
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String buildSettlementDetail(Models.Trade trade) {
        return buildResultReason(trade);
    }

    public String buildMissLearningText(Models.Trade trade) {
        if (trade == null) {
            return "暂无未中学习";
        }
        Models.Trade t = copyTradeForReason(trade);
        normalizeTradeForReason(t);
        if (!AppConfig.RESULT_MISS.equals(t.result)) {
            return "本次不是未中，不生成错因修正";
        }
        StringBuilder sb = new StringBuilder();
        String miss = t.missReason == null ? "" : t.missReason;
        sb.append("未中修正：");
        sb.append(UiFormatter.missReasonText(miss));
        sb.append("。本期严格按目标期=");
        sb.append(safe(t.targetIssue));
        sb.append("、第");
        sb.append(t.position);
        sb.append("位置、K3=");
        sb.append(UiFormatter.numberGroup(t.n1, t.n2, t.n3));
        sb.append("结算，实际=");
        sb.append(UiFormatter.number(t.actualNumber));
        sb.append("。");
        if (AppConfig.MISS_POSITION_LOST.equals(miss)) {
            sb.append("下次同状态要降低该位置优势权重，提高位置切换审计，不再因为单一位置短热就出手");
        } else if (AppConfig.MISS_K3_RANK_4_6.equals(miss)) {
            sb.append("方向接近但K3层级偏窄，下次同形态提高第4-6层观察，不直接扩K，仍保持固定K3");
        } else if (AppConfig.MISS_TREND_BROKEN.equals(miss)) {
            sb.append("趋势已破，后续同状态提高破位门槛，先SHADOW观察，等待趋势重新确认");
        } else if (AppConfig.MISS_FALSE_RECOVERY.equals(miss)) {
            sb.append("恢复是假恢复，后续恢复分不足或恢复证据短时冲高时不提前BET");
        } else if (AppConfig.MISS_DRIFT_SPIKE.equals(miss)) {
            sb.append("漂移突然升高，后续漂移风险权重要加大，同类漂移环境降低FinalP");
        } else if (AppConfig.MISS_HOT_REVERSE.equals(miss)) {
            sb.append("热层反转，后续过热时降低旧热号权重，增加冷/回补层的影子观察");
        } else if (AppConfig.MISS_MARKOV_FAILED.equals(miss)) {
            sb.append("转移模型失效，后续同状态降低Markov权重，不能单靠转移链出手");
        } else if (AppConfig.MISS_THIRD_WEAK.equals(miss)) {
            sb.append("第三号码弱，后续要求K3内部第三号也要有独立证据，否则只保留前二强号观察");
        } else if (AppConfig.MISS_SAMPLE_WEAK.equals(miss)) {
            sb.append("样本不足，后续低样本同形态只进SHADOW，不进真实BET");
        } else if (AppConfig.MISS_DATA_OFFSET.equals(miss)) {
            sb.append("疑似错期/错位，不进入模型学习，先修数据链路与结算链路");
        } else {
            sb.append("错因不明确，后续只轻微降权，不做强惩罚，等待更多教师样本确认");
        }
        sb.append("；真实校准只允许REAL_TRADE已结算样本进入，SHADOW/HOLD/教师矩阵/后台反推只做画像和解释。");
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String buildMissReverseText(Models.Trade trade) {
        if (trade == null) {
            return "暂无反推";
        }
        Models.Trade t = copyTradeForReason(trade);
        normalizeTradeForReason(t);
        if (!AppConfig.RESULT_MISS.equals(t.result)) {
            return "不是未中，不需要未中反推";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("为什么不中：预测只覆盖第");
        sb.append(t.position);
        sb.append("位置的 ");
        sb.append(UiFormatter.numberGroup(t.n1, t.n2, t.n3));
        sb.append("，实际开出 ");
        sb.append(UiFormatter.number(t.actualNumber));
        sb.append("，实际号码没有等于预测三号之一。");
        if (t.actualNumber > 0) {
            if (t.actualNumber != t.n1 && t.actualNumber != t.n2 && t.actualNumber != t.n3) {
                sb.append("这说明本期该位置的号码层级判断没有覆盖到实际层，教师矩阵要记录为：该状态、该位置、这组三号为负样本，实际号为正样本。");
            }
        }
        if (t.reasonSummary != null && t.reasonSummary.length() > 0) {
            sb.append("出手时理由为：");
            sb.append(shortText(displayReason(t.reasonSummary), 180));
            sb.append("。后续学习要检查这些理由是否属于假强、样本弱、漂移突增、状态切换、因链断裂、规则健康失衡或第三防线失效。");
        }
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String buildHitLearningText(Models.Trade trade) {
        if (trade == null) {
            return "暂无命中学习";
        }
        Models.Trade t = copyTradeForReason(trade);
        normalizeTradeForReason(t);
        if (!AppConfig.RESULT_WIN.equals(t.result)) {
            return "本次不是命中，不生成命中强化";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("命中强化：目标期=");
        sb.append(safe(t.targetIssue));
        sb.append("，第");
        sb.append(t.position);
        sb.append("位置实际=");
        sb.append(UiFormatter.number(t.actualNumber));
        sb.append(" 等于K3=");
        sb.append(UiFormatter.numberGroup(t.n1, t.n2, t.n3));
        sb.append(" 内的预测号码。教师矩阵记录为：同状态、同位置、同号码层级为正样本，可小幅增强该状态画像，但不能盲目追下一期，仍需下一期重新过门槛。");
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    public String buildRuntimeSummary(Models.AppRuntimeState state) {
        if (state == null) {
            return "暂无运行状态";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("服务=");
        sb.append(UiFormatter.serviceStatusText(state.serviceStatus));
        sb.append("，当前期=");
        sb.append(safe(state.currentIssue));
        sb.append("，目标期=");
        sb.append(safe(state.targetIssue));
        sb.append("，倒计时=");
        sb.append(UiFormatter.countdown(state.countdownMs));
        if (state.lastDecision != null && state.lastDecision.length() > 0) {
            sb.append("，最新决策=");
            sb.append(UiFormatter.decisionText(state.lastDecision));
        }
        if (state.backtestStatus != null && state.backtestStatus.length() > 0) {
            sb.append("，回测=");
            sb.append(displayReason(state.backtestStatus));
            if (state.backtestProgress > 0) {
                sb.append(" ");
                sb.append(state.backtestProgress);
                sb.append("%");
            }
        }
        if (state.learningSummary != null && state.learningSummary.length() > 0) {
            sb.append("，学习=");
            sb.append(shortText(displayReason(state.learningSummary), 110));
        }
        String tmSummary =
                firstNonEmpty(
                        getStringField(state, "teacherNumberMatrixRealtimeSummary"),
                        getStringField(state, "teacher_number_matrix_realtime_summary"),
                        getStringField(state, "teacherNumberMatrixSummary"),
                        getStringField(state, "teacher_number_matrix_summary"),
                        getStringField(state, "teacherMatrixSummary"));
        if (tmSummary.length() == 0) {
            tmSummary =
                    firstNonEmpty(
                            getStringField(state, "teacher_matrix_summary"),
                            getStringField(state, "teacherSnapshotSummary"),
                            getStringField(state, "teacher_snapshot_summary"),
                            "");
        }
        if (tmSummary.length() > 0) {
            sb.append("，教师矩阵=");
            sb.append(shortText(displayReason(tmSummary), 130));
        }
        String causeSummary =
                firstNonEmpty(
                        getStringField(state, "dynamicCauseChainSummary"),
                        getStringField(state, "dynamic_cause_chain_summary"),
                        getStringField(state, "causeChainSummary"),
                        getStringField(state, "ruleHealthSummary"),
                        "");
        if (causeSummary.length() > 0) {
            sb.append("，因链=");
            sb.append(shortText(displayReason(causeSummary), 130));
        }
        if (state.lastError != null && state.lastError.length() > 0) {
            sb.append("，错误=");
            sb.append(shortText(displayReason(state.lastError), 90));
        }
        return shortText(sb.toString(), SUMMARY_LIMIT);
    }

    private void addDecisionHeader(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        String title;
        String detail;
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            title = "决策总览：真实BET";
            detail =
                    "单位置K3真实出手，第"
                            + d.position
                            + "位置 "
                            + UiFormatter.numberGroup(d.n1, d.n2, d.n3)
                            + "；目标期="
                            + safe(d.targetIssue)
                            + "；不加倍、不倍投、不追期、不多位置；开奖后只按该位置判断中/不中";
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            title = "决策总览：SHADOW";
            detail = "只做影子观察和画像学习，不计真实胜率，不进入真实校准，不产生真实盈亏";
            if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
                detail += "；观察第" + d.position + "位置 " + UiFormatter.numberGroup(d.n1, d.n2, d.n3);
            }
        } else {
            title = "决策总览：HOLD";
            detail = "本期不出手，保护真实胜率和回撤，不生成真实交易";
            if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
                detail +=
                        "；保留观察候选第" + d.position + "位置 " + UiFormatter.numberGroup(d.n1, d.n2, d.n3);
            }
        }
        add(
                out,
                AppConfig.DECISION_BET.equals(d.decision)
                        ? AppConfig.REASON_PASS
                        : AppConfig.REASON_INFO,
                d.decision == null ? "" : d.decision,
                title,
                detail,
                d.finalP);
    }

    private void buildBetDecisionReasons(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        add(
                out,
                AppConfig.REASON_PASS,
                AppConfig.REASON_PASS,
                "最终出手",
                "本期只出一个位置：第"
                        + d.position
                        + "位置，固定K3号码 "
                        + UiFormatter.numberGroup(d.n1, d.n2, d.n3)
                        + "。不加倍、不倍投、不追期、不多位置。",
                d.finalP);
        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }
        add(
                out,
                d.finalP >= d.dynamicGate ? AppConfig.REASON_PASS : AppConfig.REASON_WARN,
                "PROBABILITY",
                "概率通过门槛",
                "最终概率="
                        + UiFormatter.probability(d.finalP)
                        + "，出手门槛="
                        + UiFormatter.probability(d.dynamicGate)
                        + "，概率优势="
                        + UiFormatter.signedProbability(edge)
                        + "，最低真实线="
                        + UiFormatter.probability(AppConfig.MIN_REAL_BET_PROBABILITY),
                edge);
        if (d.reasonSummary != null && d.reasonSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SUMMARY",
                    "综合原因",
                    displayReasonLong(d.reasonSummary),
                    0.0d);
        }
    }

    private void buildHoldDecisionReasons(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        String block =
                d.blockCode == null || d.blockCode.length() == 0
                        ? AppConfig.BLOCK_LOW_PROBABILITY
                        : d.blockCode;
        add(
                out,
                AppConfig.REASON_BLOCK,
                block,
                "最终不出手",
                d.reasonSummary == null || d.reasonSummary.length() == 0
                        ? UiFormatter.blockReasonText(block)
                        : displayReasonLong(d.reasonSummary),
                d.finalP);
        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }
        if (d.finalP < d.dynamicGate) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "概率不足",
                    "最终概率="
                            + UiFormatter.probability(d.finalP)
                            + "，没有超过出手门槛="
                            + UiFormatter.probability(d.dynamicGate)
                            + "，概率优势="
                            + UiFormatter.signedProbability(edge),
                    edge);
        }
        if (d.finalP > 0.0d && d.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "低于最低真实线",
                    "最终概率低于最低真实出手线 "
                            + UiFormatter.probability(AppConfig.MIN_REAL_BET_PROBABILITY)
                            + "，不能真实BET",
                    d.finalP);
        }
        if (d.freq100 > 0.0d) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "FREQUENCY_REFERENCE",
                    "频率参考",
                    "近100期真实BET频率="
                            + UiFormatter.probability(d.freq100)
                            + "。频率是动态门槛和回测保护的参考，不作为低频救援，不越过最终DecisionGate。",
                    d.freq100);
        }
    }

    private void buildShadowDecisionReasons(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        add(
                out,
                AppConfig.REASON_INFO,
                AppConfig.DECISION_SHADOW,
                "影子观察",
                "该候选只用于观察学习、教师矩阵样本和画像反哺；不进入真实出手胜率，不进入真实校准，不产生真实BET，不产生真实盈亏",
                d.finalP);
        if (d.position > 0 && Models.isValidK3Mask(d.k3Mask)) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SHADOW_CANDIDATE",
                    "影子候选",
                    "第"
                            + d.position
                            + "位置 "
                            + UiFormatter.numberGroup(d.n1, d.n2, d.n3)
                            + "，最终概率="
                            + UiFormatter.probability(d.finalP)
                            + "，门槛="
                            + UiFormatter.probability(d.dynamicGate),
                    d.finalP);
        }
        if (d.reasonSummary != null && d.reasonSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SUMMARY",
                    "影子原因",
                    displayReasonLong(d.reasonSummary),
                    0.0d);
        }
    }

    private void addProbabilityBreakdown(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        double edge = d.edge;
        if (edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            edge = d.finalP - d.dynamicGate;
        }
        add(
                out,
                d.finalP >= d.dynamicGate ? AppConfig.REASON_PASS : AppConfig.REASON_BLOCK,
                d.finalP >= d.dynamicGate ? AppConfig.REASON_PASS : AppConfig.BLOCK_LOW_PROBABILITY,
                "概率拆解",
                "RawP="
                        + UiFormatter.probability(d.rawP)
                        + "，CalP="
                        + UiFormatter.probability(d.calibratedP)
                        + "，FinalP="
                        + UiFormatter.probability(d.finalP)
                        + "，Gate="
                        + UiFormatter.probability(d.dynamicGate)
                        + "，Edge="
                        + UiFormatter.signedProbability(edge)
                        + "，校准误差="
                        + UiFormatter.probability(d.calibrationError),
                edge);
        add(
                out,
                AppConfig.REASON_INFO,
                "MODEL_PARTS",
                "模型分量",
                "窗口="
                        + UiFormatter.probability(d.modelP)
                        + "，形态="
                        + UiFormatter.probability(d.shapeP)
                        + "，组合="
                        + UiFormatter.probability(d.comboP)
                        + "，恢复="
                        + UiFormatter.probability(d.recoveryP)
                        + "，相似="
                        + UiFormatter.probability(d.similarP)
                        + "，记忆="
                        + UiFormatter.probability(d.memoryP),
                d.finalP);
    }

    private void addCandidateProbabilityBreakdown(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "MODEL_PARTS",
                "候选概率分量",
                "窗口="
                        + UiFormatter.probability(c.modelP)
                        + "，形态="
                        + UiFormatter.probability(c.shapeP)
                        + "，组合="
                        + UiFormatter.probability(c.comboP)
                        + "，恢复="
                        + UiFormatter.probability(c.recoveryP)
                        + "，相似="
                        + UiFormatter.probability(c.similarP)
                        + "，记忆="
                        + UiFormatter.probability(c.memoryP),
                c.finalP);
    }

    private void addShapeBreakdown(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        add(
                out,
                isShapeSafe(d.stateMode, d.driftRisk, d.breakDepth)
                        ? AppConfig.REASON_PASS
                        : AppConfig.REASON_WARN,
                isShapeSafe(d.stateMode, d.driftRisk, d.breakDepth)
                        ? AppConfig.REASON_PASS
                        : AppConfig.BLOCK_TREND_WEAK,
                "形态强度",
                "状态="
                        + UiFormatter.modeText(d.stateMode)
                        + "，趋势寿命="
                        + UiFormatter.score(d.trendLife)
                        + "，恢复分="
                        + UiFormatter.score(d.recoveryScore)
                        + "，主优势差="
                        + UiFormatter.probability(d.championGap)
                        + "，组合优势差="
                        + UiFormatter.probability(d.comboGap),
                d.trendLife);
    }

    private void addRiskBreakdown(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (d.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_DRIFT_HIGH,
                    "漂移过高",
                    "漂移风险=" + UiFormatter.score(d.driftRisk) + "，形态变化过大，不能追",
                    d.driftRisk);
        }
        if (d.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_BREAK_HIGH,
                    "破位偏深",
                    "破位深度=" + UiFormatter.score(d.breakDepth) + "，趋势存在破位风险",
                    d.breakDepth);
        }
        if (d.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_TREND_WEAK,
                    "过热反转风险",
                    "过热反转风险=" + UiFormatter.score(d.hotReverseRisk) + "，热层可能被反杀",
                    d.hotReverseRisk);
        }
        if (d.falseStrongRisk >= AppConfig.FALSE_STRONG_RISK_MAX) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_AI_CONFLICT,
                    "假强风险",
                    "假强风险=" + UiFormatter.score(d.falseStrongRisk) + "，表面强但内部证据不稳定",
                    d.falseStrongRisk);
        }
        if (d.trendLife > 0.0d && d.trendLife < AppConfig.TREND_LIFE_MIN) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_TREND_WEAK,
                    "趋势寿命不足",
                    "趋势寿命=" + UiFormatter.score(d.trendLife) + "，形态延续性不足",
                    d.trendLife);
        }
    }

    private void addCandidateRiskBreakdown(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        String level = AppConfig.REASON_PASS;
        String code = AppConfig.REASON_PASS;
        if (c.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            level = AppConfig.REASON_WARN;
            code = AppConfig.BLOCK_DRIFT_HIGH;
        }
        if (c.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            level = AppConfig.REASON_WARN;
            code = AppConfig.BLOCK_BREAK_HIGH;
        }
        if (c.evidenceRisk >= AppConfig.EVIDENCE_RISK_MAX) {
            level = AppConfig.REASON_WARN;
            code = AppConfig.BLOCK_AI_CONFLICT;
        }
        add(
                out,
                level,
                code,
                "候选风险",
                "漂移="
                        + UiFormatter.score(c.driftRisk)
                        + "，破位="
                        + UiFormatter.score(c.breakDepth)
                        + "，过热反转="
                        + UiFormatter.score(c.hotReverseRisk)
                        + "，假强="
                        + UiFormatter.score(c.falseStrongRisk)
                        + "，证据风险="
                        + UiFormatter.score(c.evidenceRisk),
                c.evidenceRisk);
    }

    private void addFrequencyBreakdown(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (d.freq100 > 0.0d || d.freq500 > 0.0d || d.freq1000 > 0.0d) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "FREQUENCY_REFERENCE",
                    "频率展示",
                    "近100期真实BET频率="
                            + UiFormatter.probability(d.freq100)
                            + "，近500="
                            + UiFormatter.probability(d.freq500)
                            + "，近1000="
                            + UiFormatter.probability(d.freq1000)
                            + "。频率参与动态风控和回测保护，但不作为低频救援，不越过最终门。",
                    d.freq100);
        }
    }

    private void addLearningBreakdown(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (d.deepProfileKey != null && d.deepProfileKey.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "DEEP_PROFILE",
                    "学习画像",
                    "画像="
                            + d.deepProfileKey
                            + "，画像命中率="
                            + UiFormatter.probability(d.deepProfileHitRate)
                            + "，画像样本="
                            + d.deepProfileSample
                            + "，回退等级="
                            + d.deepFallbackLevel
                            + "，回退画像="
                            + safe(d.fallbackProfileKey),
                    d.deepProfileHitRate);
        }
        if (d.learningSummary != null && d.learningSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "LEARNING",
                    "学习摘要",
                    displayReasonLong(d.learningSummary),
                    0.0d);
        }
    }

    private void addTeacherMatrixDecisionReason(
            ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        TeacherMatrixView tm = readTeacherMatrixView(d);
        if (!tm.hasAny()) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "TEACHER_MATRIX_EMPTY",
                    "教师矩阵",
                    "本次决策没有读取到有效教师矩阵反哺字段，按中性处理；这不影响真实BET裁决，因为教师矩阵不能直接救援BET，也不能越过FinalP/RawP/Gate/DecisionGate。",
                    0.0d);
            return;
        }
        String level = AppConfig.REASON_INFO;
        String code = "TEACHER_MATRIX";
        if ("RISK".equals(tm.status) || tm.risk >= 0.60d) {
            level = AppConfig.REASON_WARN;
            code = "TEACHER_MATRIX_RISK";
        } else if ("STRONG".equals(tm.status)
                || "READY".equals(tm.status)
                || "NORMAL".equals(tm.status)
                || tm.applied > 0
                || tm.sample > 0) {
            level = AppConfig.REASON_PASS;
            code = "TEACHER_MATRIX_READY";
        }
        add(
                out,
                level,
                code,
                "教师矩阵反哺",
                "状态="
                        + firstNonEmpty(tm.status, "未标记")
                        + "，样本="
                        + tm.sample
                        + "，组合样本="
                        + tm.comboSample
                        + "，命中率="
                        + UiFormatter.probability(tm.hitRate)
                        + "，组合率="
                        + UiFormatter.probability(tm.comboHitRate)
                        + "，分数="
                        + UiFormatter.probability(tm.score)
                        + "，置信="
                        + UiFormatter.probability(tm.confidence)
                        + "，风险="
                        + UiFormatter.probability(tm.risk)
                        + "，概率微调="
                        + UiFormatter.signedProbability(tm.probAdjust)
                        + "，门槛微调="
                        + UiFormatter.signedProbability(tm.gateAdjust)
                        + "，是否应用="
                        + (tm.applied > 0 ? "是" : "否")
                        + "；说明：教师矩阵只做画像、风险、解释和小幅反哺，不救援BET，不越过FinalP/RawP/Gate/DecisionGate。",
                tm.confidence);
        if (tm.summary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "TEACHER_MATRIX_SUMMARY",
                    "教师矩阵摘要",
                    displayReasonLong(tm.summary),
                    0.0d);
        }
    }

    private void addTeacherMatrixCandidateReason(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        TeacherMatrixView tm = readTeacherMatrixView(c);
        if (!tm.hasAny()) {
            return;
        }
        String level = AppConfig.REASON_INFO;
        String code = "TEACHER_MATRIX";
        if ("RISK".equals(tm.status) || tm.risk >= 0.60d) {
            level = AppConfig.REASON_WARN;
            code = "TEACHER_MATRIX_RISK";
        } else if ("STRONG".equals(tm.status)
                || "READY".equals(tm.status)
                || "NORMAL".equals(tm.status)
                || tm.applied > 0
                || tm.sample > 0) {
            level = AppConfig.REASON_PASS;
            code = "TEACHER_MATRIX_READY";
        }
        add(
                out,
                level,
                code,
                "候选教师矩阵",
                "状态="
                        + firstNonEmpty(tm.status, "未标记")
                        + "，样本="
                        + tm.sample
                        + "，组合样本="
                        + tm.comboSample
                        + "，命中率="
                        + UiFormatter.probability(tm.hitRate)
                        + "，组合率="
                        + UiFormatter.probability(tm.comboHitRate)
                        + "，分数="
                        + UiFormatter.probability(tm.score)
                        + "，置信="
                        + UiFormatter.probability(tm.confidence)
                        + "，风险="
                        + UiFormatter.probability(tm.risk)
                        + "，概率微调="
                        + UiFormatter.signedProbability(tm.probAdjust)
                        + "，门槛微调="
                        + UiFormatter.signedProbability(tm.gateAdjust)
                        + "；教师矩阵仅参与候选画像、风险解释和小幅反哺，不能直接放行BET。",
                tm.confidence);
        if (tm.summary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "TEACHER_MATRIX_CANDIDATE_SUMMARY",
                    "候选教师摘要",
                    displayReasonLong(tm.summary),
                    0.0d);
        }
    }

    private void addV20CauseChainDecisionReason(
            ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        V20View v = readV20View(d);
        if (!v.hasAny()) {
            return;
        }
        String level = v.hardBlock > 0 ? AppConfig.REASON_BLOCK : AppConfig.REASON_INFO;
        String code = v.hardBlock > 0 ? "V20_CAUSE_CHAIN_BLOCK" : "V20_CAUSE_CHAIN";
        if (v.hardBlock <= 0
                && (v.causeScore >= 0.60d
                        || v.ruleHealth >= 0.60d
                        || v.thirdScore >= 0.60d
                        || v.supportScore >= 0.60d)) {
            level = AppConfig.REASON_PASS;
            code = "V20_CAUSE_CHAIN_PASS";
        }
        add(
                out,
                level,
                code,
                "V20因链矩阵",
                "状态="
                        + firstNonEmpty(v.status, "未标记")
                        + "，综合支撑="
                        + UiFormatter.probability(v.supportScore)
                        + "，综合风险="
                        + UiFormatter.probability(v.riskScore)
                        + "，因链分="
                        + UiFormatter.probability(v.causeScore)
                        + "，位置因子="
                        + UiFormatter.probability(v.positionScore)
                        + "，号码因子="
                        + UiFormatter.probability(v.numberScore)
                        + "，反事实排名="
                        + UiFormatter.probability(v.counterScore)
                        + "，第三防线="
                        + UiFormatter.probability(v.thirdScore)
                        + "，规则健康="
                        + UiFormatter.probability(v.ruleHealth)
                        + "，影子健康="
                        + UiFormatter.probability(v.shadowHealth)
                        + "，假强风险="
                        + UiFormatter.probability(v.falseRisk)
                        + "，种子扩展="
                        + UiFormatter.probability(v.seedScore)
                        + "；说明：V20因链只能辅助解释、排序、风险和小幅反哺，不能替代最终BET门。",
                v.causeScore);
        if (v.summary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_CAUSE_CHAIN_SUMMARY",
                    "V20因链摘要",
                    displayReasonLong(v.summary),
                    0.0d);
        }
    }

    private void addV20CandidateCauseChainReason(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        V20View v = readV20View(c);
        if (!v.hasAny()) {
            return;
        }
        String level = AppConfig.REASON_INFO;
        String code = "V20_CANDIDATE_CAUSE";
        if (v.falseRisk >= 0.70d || v.hardBlock > 0) {
            level = AppConfig.REASON_WARN;
            code = "V20_FALSE_STRONG_RISK";
        } else if (v.causeScore >= 0.60d
                || v.ruleHealth >= 0.60d
                || v.thirdScore >= 0.60d
                || v.supportScore >= 0.60d) {
            level = AppConfig.REASON_PASS;
            code = "V20_CANDIDATE_CAUSE_PASS";
        }
        add(
                out,
                level,
                code,
                "候选V20因链",
                "状态="
                        + firstNonEmpty(v.status, "未标记")
                        + "，综合支撑="
                        + UiFormatter.probability(v.supportScore)
                        + "，综合风险="
                        + UiFormatter.probability(v.riskScore)
                        + "，因链分="
                        + UiFormatter.probability(v.causeScore)
                        + "，位置因子="
                        + UiFormatter.probability(v.positionScore)
                        + "，号码因子="
                        + UiFormatter.probability(v.numberScore)
                        + "，反事实="
                        + UiFormatter.probability(v.counterScore)
                        + "，第三防线="
                        + UiFormatter.probability(v.thirdScore)
                        + "，规则健康="
                        + UiFormatter.probability(v.ruleHealth)
                        + "，假强风险="
                        + UiFormatter.probability(v.falseRisk)
                        + "；候选仍需FinalP、动态门、DecisionGate共同通过。",
                v.causeScore);
        if (v.summary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_CANDIDATE_CAUSE_SUMMARY",
                    "候选因链摘要",
                    displayReasonLong(v.summary),
                    0.0d);
        }
    }

    private void addMainChainDecisionReason(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        MainChainReasonView v = readMainChainReasonView(d);
        if (!v.hasAny()) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "MAIN_CHAIN_EMPTY",
                    "V20主链字段",
                    "本次决策没有读取到完整主链诊断字段，按中性展示；原因层不生成BET，不救援BET，真实BET仍只看DecisionGateEngine最终裁决。",
                    0.0d);
            return;
        }
        String level = mainChainReasonLevel(v);
        String code = mainChainReasonCode(v, "DECISION");
        add(out, level, code, "V20主链最终诊断", buildMainChainReasonText(v, "决策"), v.qualityScore);
        addMainChainNeutralReason(out, v);
        addMainChainRejectReason(out, v);
    }

    private void addMainChainCandidateReason(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        MainChainReasonView v = readMainChainReasonView(c);
        if (!v.hasAny()) {
            return;
        }
        add(
                out,
                mainChainReasonLevel(v),
                mainChainReasonCode(v, "CANDIDATE"),
                "候选主链诊断",
                buildMainChainReasonText(v, "候选"),
                v.qualityScore);
        addMainChainNeutralReason(out, v);
        addMainChainRejectReason(out, v);
    }

    private void addRuntimeMainChainReason(
            ArrayList<Models.ReasonItem> out, Models.AppRuntimeState state) {
        if (state == null) {
            return;
        }
        MainChainReasonView v = readMainChainReasonView(state);
        if (!v.hasAny()) {
            return;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "MAIN_CHAIN_RUNTIME",
                "V20主链运行字段",
                buildMainChainReasonText(v, "运行"),
                v.qualityScore);
    }

    private void addMainChainTradeReason(ArrayList<Models.ReasonItem> out, Models.Trade t) {
        if (t == null) {
            return;
        }
        MainChainReasonView v = readMainChainReasonView(t);
        if (!v.hasAny()) {
            return;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "MAIN_CHAIN_TRADE",
                "出手时主链字段",
                buildMainChainReasonText(v, "交易"),
                v.qualityScore);
    }

    private void addMainChainNeutralReason(
            ArrayList<Models.ReasonItem> out, MainChainReasonView v) {
        if (v == null) {
            return;
        }
        if (!v.teacherNoSampleNeutral && !v.v20WatchNeutral && !v.adaptiveWatchNeutral) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (v.teacherNoSampleNeutral) {
            sb.append("Teacher NO_SAMPLE/NONE/空样本=中性观察，不是弱杀，也不是救援；");
        }
        if (v.v20WatchNeutral) {
            sb.append("V20 WATCH/UNKNOWN/PENDING/NEUTRAL=中性观察，不是硬风险；");
        }
        if (v.adaptiveWatchNeutral) {
            sb.append("Adaptive WATCH/PENDING/NEUTRAL=中性观察，不直接拦真实BET；");
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "MAIN_CHAIN_NEUTRAL_STATUS",
                "中性状态说明",
                sb.toString() + "最终是否BET仍必须由DecisionGateEngine按FinalP/Gate/Edge/硬风险统一裁决。",
                0.0d);
    }

    private void addMainChainRejectReason(ArrayList<Models.ReasonItem> out, MainChainReasonView v) {
        if (v == null) {
            return;
        }
        String reject =
                firstNonEmpty(
                        v.finalRejectCode,
                        v.adaptiveRejectCode,
                        v.mainChainRejectCode,
                        v.ruleRejectCode,
                        v.thirdRejectCode,
                        v.shadowRejectCode);
        if (reject == null || reject.length() == 0) {
            return;
        }
        String level = v.hasHardRisk() ? AppConfig.REASON_BLOCK : AppConfig.REASON_WARN;
        add(
                out,
                level,
                "MAIN_CHAIN_REJECT_CODE",
                "最终门拒绝码",
                "拒绝码="
                        + displayReason(reject)
                        + "；动作建议="
                        + displayReason(
                                firstNonEmpty(
                                        v.actionAdvice,
                                        v.adaptiveActionAdvice,
                                        v.dynamicGateActionAdvice,
                                        "观察"))
                        + "；拒绝码只解释为什么BET/SHADOW/HOLD，不在ReasonEngine里改变决策。",
                v.riskScore);
    }

    private String mainChainReasonLevel(MainChainReasonView v) {
        if (v == null) {
            return AppConfig.REASON_INFO;
        }
        if (v.hasHardRisk()) {
            return AppConfig.REASON_BLOCK;
        }
        if (v.riskScore >= 0.620d
                || v.counterScore >= 0.620d
                || v.ruleDecayRisk >= 0.620d
                || v.thirdDragRisk >= 0.620d
                || v.counterfactualRisk >= 0.620d) {
            return AppConfig.REASON_WARN;
        }
        if (v.qualityScore >= 0.620d
                || v.evidenceScore >= 0.620d
                || v.netScore >= 0.180d
                || v.shadowPromoteScore >= 0.300d) {
            return AppConfig.REASON_PASS;
        }
        return AppConfig.REASON_INFO;
    }

    private String mainChainReasonCode(MainChainReasonView v, String prefix) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "MAIN_CHAIN";
        }
        if (v == null) {
            return prefix + "_EMPTY";
        }
        if (v.hasHardRisk()) {
            return prefix + "_HARD_RISK";
        }
        if (v.riskScore >= 0.620d || v.counterScore >= 0.620d) {
            return prefix + "_COUNTER_RISK";
        }
        if (v.qualityScore >= 0.620d || v.evidenceScore >= 0.620d) {
            return prefix + "_PASS_EVIDENCE";
        }
        if (v.teacherNoSampleNeutral || v.v20WatchNeutral || v.adaptiveWatchNeutral) {
            return prefix + "_NEUTRAL_WATCH";
        }
        return prefix + "_WATCH";
    }

    private String buildMainChainReasonText(MainChainReasonView v, String prefix) {
        if (v == null) {
            return "主链诊断为空；ReasonEngine只展示原因，不生成BET。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(prefix == null || prefix.length() == 0 ? "主链" : prefix);
        sb.append("主链闭环：finalStatus=").append(firstNonEmpty(v.finalStatus, "未标记"));
        sb.append("；quality=").append(UiFormatter.probability(v.qualityScore));
        sb.append("；risk=").append(UiFormatter.probability(v.riskScore));
        sb.append("；evidence=").append(UiFormatter.probability(v.evidenceScore));
        sb.append("；counter=").append(UiFormatter.probability(v.counterScore));
        sb.append("；net=").append(UiFormatter.signedProbability(v.netScore));
        sb.append("；positionNeed=").append(UiFormatter.probability(v.positionNeedScore));
        sb.append("；numberSupply=").append(UiFormatter.probability(v.numberSupplyScore));
        sb.append("；coverage=").append(UiFormatter.probability(v.coverageQuality));
        sb.append("；thirdQuality=").append(UiFormatter.probability(v.thirdQuality));
        sb.append("；thirdDragRisk=").append(UiFormatter.probability(v.thirdDragRisk));
        sb.append("；ruleHealth=").append(UiFormatter.probability(v.ruleHealthScore));
        sb.append("；ruleDecay=").append(UiFormatter.probability(v.ruleDecayRisk));
        sb.append("；counterfactualRisk=").append(UiFormatter.probability(v.counterfactualRisk));
        sb.append("；shadowPromotion=").append(firstNonEmpty(v.shadowPromotionStatus, "未标记"));
        sb.append("/").append(UiFormatter.probability(v.shadowPromoteScore));
        sb.append("；adaptive=").append(firstNonEmpty(v.adaptiveStatus, "未标记"));
        String reject =
                firstNonEmpty(
                        v.finalRejectCode,
                        v.adaptiveRejectCode,
                        v.mainChainRejectCode,
                        v.ruleRejectCode,
                        v.thirdRejectCode,
                        v.shadowRejectCode);
        if (reject.length() > 0) {
            sb.append("；reject=").append(displayReason(reject));
        }
        sb.append("；action=")
                .append(
                        displayReason(
                                firstNonEmpty(
                                        v.actionAdvice,
                                        v.adaptiveActionAdvice,
                                        v.dynamicGateActionAdvice,
                                        "观察，不直接救援BET")));
        sb.append("；neutral=").append(v.neutralSummary());
        sb.append("；version=")
                .append(firstNonEmpty(v.fieldChainVersion, V20_REASON_MAIN_CHAIN_VERSION));
        sb.append(
                "；说明=ReasonEngine只解释为什么BET/SHADOW/HOLD，不出号、不创建Trade、不结算、不改真实盈亏。真实BET唯一出口仍是DecisionGateEngine。"
                    + " ");
        return displayReasonLong(sb.toString());
    }

    private MainChainReasonView readMainChainReasonView(Object obj) {
        MainChainReasonView v = new MainChainReasonView();
        if (obj == null) {
            return v;
        }
        v.finalStatus =
                firstNonEmpty(
                        getStringField(obj, "finalDecisionStatus"),
                        getStringField(obj, "decisionGateStatus"),
                        getStringField(obj, "decisionStatus"));
        v.actionAdvice =
                firstNonEmpty(
                        getStringField(obj, "finalDecisionActionAdvice"),
                        getStringField(obj, "decisionGateActionAdvice"),
                        getStringField(obj, "mainChainActionAdvice"),
                        getStringField(obj, "v20ActionAdvice"),
                        getStringField(obj, "actionAdvice"));
        v.finalRejectCode =
                firstNonEmpty(
                        getStringField(obj, "finalDecisionRejectCode"),
                        getStringField(obj, "rejectCode"),
                        getStringField(obj, "blockCode"));
        v.mainChainRejectCode =
                firstNonEmpty(
                        getStringField(obj, "mainChainRejectCode"),
                        getStringField(obj, "probabilityMainChainRejectCode"),
                        getStringField(obj, "dynamicGateMainChainRejectCode"));
        v.ruleRejectCode =
                firstNonEmpty(
                        getStringField(obj, "ruleRejectCode"),
                        getStringField(obj, "ruleHealthRejectCode"));
        v.thirdRejectCode =
                firstNonEmpty(
                        getStringField(obj, "thirdRejectCode"),
                        getStringField(obj, "thirdGuardRejectCode"));
        v.shadowRejectCode =
                firstNonEmpty(
                        getStringField(obj, "shadowRejectCode"),
                        getStringField(obj, "shadowPromotionRejectCode"));
        v.adaptiveRejectCode =
                firstNonEmpty(
                        getStringField(obj, "adaptiveRejectCode"),
                        getStringField(obj, "adaptiveJudgeRejectCode"));
        v.qualityScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "finalDecisionQualityScore",
                            "decisionQualityScore",
                            "mainChainQualityScore",
                            "dynamicGateMainChainEvidenceScore"
                        });
        v.riskScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "finalDecisionRiskScore",
                            "decisionRiskScore",
                            "mainChainRiskScore",
                            "dynamicGateMainChainCounterScore"
                        });
        v.evidenceScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "mainChainEvidenceScore",
                            "finalDecisionEvidenceContribution",
                            "probabilityMainChainEvidenceScore",
                            "dynamicGateMainChainEvidenceScore",
                            "evidenceScore",
                            "antiEvidenceScore"
                        });
        v.counterScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "mainChainCounterScore",
                            "finalDecisionCounterContribution",
                            "probabilityMainChainCounterScore",
                            "dynamicGateMainChainCounterScore",
                            "counterEvidenceScore",
                            "counterEvidence"
                        });
        v.netScore =
                maxAbsDoubleFields(
                        obj,
                        new String[] {
                            "mainChainNetEvidenceScore",
                            "netEvidenceScore",
                            "dynamicGateMainChainNetScore"
                        });
        v.positionNeedScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "positionNeedScore",
                            "finalDecisionPositionNeedScore",
                            "dynamicGatePositionNeedScore"
                        });
        v.numberSupplyScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "numberSupplyScore",
                            "finalDecisionNumberSupplyScore",
                            "dynamicGateNumberSupplyScore"
                        });
        v.coverageQuality =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "coverageQuality",
                            "coverageQualityScore",
                            "finalDecisionCoverageQuality",
                            "dynamicGateCoverageQualityScore"
                        });
        v.thirdQuality =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "thirdQuality",
                            "thirdNumberQuality",
                            "thirdNumberQualityScore",
                            "finalDecisionThirdNumberQuality",
                            "dynamicGateThirdNumberQuality"
                        });
        v.ruleHealthScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "ruleHealthScore", "ruleHealth", "finalDecisionRuleHealthScore"
                        });
        v.ruleDecayRisk =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "ruleDecayRisk", "ruleHealthDecayRisk", "finalDecisionRuleDecayRisk"
                        });
        v.thirdDragRisk =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "thirdDragRisk",
                            "thirdWeakness",
                            "thirdDragScore",
                            "finalDecisionThirdDragRisk"
                        });
        v.counterfactualRisk =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "counterfactualRisk",
                            "counterEvidenceScore",
                            "replaceGain",
                            "rankCounterEdge",
                            "finalDecisionCounterfactualRisk"
                        });
        v.shadowPromoteScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "shadowPromoteScore", "shadowPromotionScore", "adaptivePromoteScore"
                        });
        v.shadowPromotionStatus =
                firstNonEmpty(
                        getStringField(obj, "shadowPromotionStatus"),
                        getStringField(obj, "shadowPromotionActionAdvice"));
        v.adaptiveStatus =
                firstNonEmpty(
                        getStringField(obj, "adaptiveStatus"),
                        getStringField(obj, "adaptiveJudgeStatus"),
                        getStringField(obj, "adaptiveAction"),
                        getStringField(obj, "adaptiveJudgeAction"));
        v.adaptiveActionAdvice =
                firstNonEmpty(
                        getStringField(obj, "adaptiveActionAdvice"),
                        getStringField(obj, "adaptiveJudgeReason"),
                        getStringField(obj, "adaptiveJudgeAction"));
        v.dynamicGateActionAdvice =
                firstNonEmpty(
                        getStringField(obj, "dynamicGateMainChainActionAdvice"),
                        getStringField(obj, "dynamicGateActionAdvice"));
        v.fieldChainVersion =
                firstNonEmpty(
                        getStringField(obj, "fieldChainVersion"),
                        getStringField(obj, "decisionGateVersion"),
                        getStringField(obj, "probabilityMainChainVersion"),
                        getStringField(obj, "dynamicGateMainChainVersion"));
        String teacherStatus =
                firstNonEmpty(
                        getStringField(obj, "teacherMatrixStatus"),
                        getStringField(obj, "teacherNumberStatus"),
                        getStringField(obj, "teacherStatus"));
        String v20Status =
                firstNonEmpty(
                        getStringField(obj, "v20Status"),
                        getStringField(obj, "dynamicStatus"),
                        getStringField(obj, "dynamicCauseStatus"));
        v.teacherNoSampleNeutral =
                isTeacherNoSampleNeutralReason(teacherStatus)
                        || maxIntFields(
                                        obj,
                                        new String[] {
                                            "teacherNoSampleNeutralFlag",
                                            "teacherMatrixNoSampleNeutralFlag"
                                        })
                                > 0;
        v.v20WatchNeutral =
                isV20WatchNeutralReason(v20Status)
                        || maxIntFields(
                                        obj,
                                        new String[] {
                                            "v20WatchNeutralFlag", "v20WatchOrEmptyNeutralFlag"
                                        })
                                > 0;
        v.adaptiveWatchNeutral =
                isAdaptiveWatchNeutralReason(v.adaptiveStatus)
                        || maxIntFields(obj, new String[] {"adaptiveWatchNeutralFlag"}) > 0;
        v.hardRiskFlag =
                maxIntFields(
                        obj,
                        new String[] {
                            "finalDecisionHardRisk",
                            "decisionHardRisk",
                            "dynamicHardRisk",
                            "falseStrongHardFreeze",
                            "ruleHardBlock",
                            "thirdGuardHardBlock"
                        });
        v.freezeFlag =
                maxIntFields(
                        obj,
                        new String[] {
                            "finalDecisionFreeze",
                            "teacherMatrixFreezeFlag",
                            "falseStrongFreeze",
                            "adaptiveFreezeFlag"
                        });
        return v;
    }

    private boolean isTeacherNoSampleNeutralReason(String s) {
        String u = upperReason(s);
        if (u.length() == 0 || "空".equals(u)) {
            return true;
        }
        return u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("NONE") >= 0
                || u.indexOf("EMPTY") >= 0
                || u.indexOf("UNKNOWN") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || u.indexOf("中性") >= 0;
    }

    private boolean isV20WatchNeutralReason(String s) {
        String u = upperReason(s);
        if (u.length() == 0 || "空".equals(u)) {
            return true;
        }
        return u.indexOf("WATCH") >= 0
                || u.indexOf("UNKNOWN") >= 0
                || u.indexOf("PENDING") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("中性") >= 0
                || u.indexOf("观察") >= 0;
    }

    private boolean isAdaptiveWatchNeutralReason(String s) {
        String u = upperReason(s);
        if (u.length() == 0 || "空".equals(u)) {
            return true;
        }
        return u.indexOf("WATCH") >= 0
                || u.indexOf("PENDING") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("中性") >= 0
                || u.indexOf("观察") >= 0;
    }

    private String upperReason(String s) {
        if (s == null) {
            return "";
        }
        try {
            return s.trim().toUpperCase(java.util.Locale.ROOT);
        } catch (Throwable e) {
            return s.trim();
        }
    }

    private void addBacktestDecisionReason(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (d.manualBacktestSupport > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "BACKTEST_SUPPORT",
                    "回测支持",
                    "手动/教师回测支持="
                            + d.manualBacktestSupport
                            + "，回测模式="
                            + safe(d.backtestMode)
                            + "；回测只做稳定性验证和画像参考，不直接保证下一期命中",
                    d.manualBacktestSupport);
        }
        if (d.backtestMode != null && d.backtestMode.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "BACKTEST_MODE",
                    "回测模式",
                    "本次决策使用模式=" + displayReason(d.backtestMode) + "；回测只用于门槛和画像参考，不直接保证下一期命中",
                    0.0d);
        }
    }

    private void addFinalDecisionConclusion(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            add(
                    out,
                    AppConfig.REASON_PASS,
                    AppConfig.REASON_PASS,
                    "最终结论",
                    "允许真实BET：必须已经通过 DecisionGateEngine"
                        + " 唯一最终门；FinalP、RawP、动态门、Edge、硬风险复核全部通过；只执行单位置K3；结算只看目标期该位置号码；Teacher"
                        + " NO_SAMPLE、V20 WATCH、Adaptive WATCH"
                        + " 只是中性说明，不是救援理由；教师矩阵/学习画像/V20因链不能替代最终门，只能作为风险和小幅反哺。",
                    d.finalP);
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    AppConfig.DECISION_SHADOW,
                    "最终结论",
                    "不真实下注，只记录SHADOW候选，开奖后用于反事实观察、教师矩阵画像和规则健康复核；不计真实胜率、不计真实频率、不计真实盈亏、不进真实校准。",
                    d.finalP);
        } else {
            String block =
                    d.blockCode == null || d.blockCode.length() == 0
                            ? AppConfig.BLOCK_LOW_PROBABILITY
                            : d.blockCode;
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    block,
                    "最终结论",
                    "不出手：" + UiFormatter.blockReasonText(block) + "；本期保护真实统计，不把观察候选算成真实BET",
                    d.finalP);
        }
    }

    private void addCandidateBase(ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        add(
                out,
                AppConfig.REASON_INFO,
                "CANDIDATE",
                "候选组合",
                "第"
                        + c.position
                        + "位置，号码="
                        + UiFormatter.numberGroup(c.n1, c.n2, c.n3)
                        + "，固定K=3，单位置候选",
                c.finalP);
    }

    private void addProbabilityReason(ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        String level = c.finalP >= c.dynamicGate ? AppConfig.REASON_PASS : AppConfig.REASON_BLOCK;
        double edge = c.edge;
        if (edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            edge = c.finalP - c.dynamicGate;
        }
        add(
                out,
                level,
                AppConfig.REASON_PASS.equals(level)
                        ? AppConfig.REASON_PASS
                        : AppConfig.BLOCK_LOW_PROBABILITY,
                "概率判断",
                "原始概率="
                        + UiFormatter.probability(c.rawP)
                        + "，校准概率="
                        + UiFormatter.probability(c.calibratedP)
                        + "，最终概率="
                        + UiFormatter.probability(c.finalP)
                        + "，出手门槛="
                        + UiFormatter.probability(c.dynamicGate)
                        + "，概率优势="
                        + UiFormatter.signedProbability(edge),
                edge);
    }

    private void addStateReason(ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        String level = AppConfig.REASON_PASS;
        String code = AppConfig.REASON_PASS;
        if (AppConfig.MODE_CHAOS.equals(c.stateMode)
                || AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || "REVERSAL".equals(c.stateMode)) {
            level = AppConfig.REASON_BLOCK;
            code = AppConfig.BLOCK_TREND_WEAK;
        } else if (c.driftRisk >= AppConfig.DRIFT_RISK_MAX
                || c.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            level = AppConfig.REASON_WARN;
            code = AppConfig.BLOCK_TREND_WEAK;
        }
        add(
                out,
                level,
                code,
                "形态状态",
                "状态="
                        + UiFormatter.modeText(c.stateMode)
                        + "，趋势寿命="
                        + UiFormatter.score(c.trendLife)
                        + "，恢复分="
                        + UiFormatter.score(c.recoveryScore)
                        + "，漂移风险="
                        + UiFormatter.score(c.driftRisk)
                        + "，破位深度="
                        + UiFormatter.score(c.breakDepth),
                c.trendLife);
    }

    private void addTrendReason(
            ArrayList<Models.ReasonItem> out, TrendStateMachine.TrendTrace trace) {
        if (trace == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_TREND_WEAK,
                    "趋势状态机",
                    "没有趋势连续性证据，按保守处理",
                    0.0d);
            return;
        }
        String level = trace.hardBlock == 1 ? AppConfig.REASON_BLOCK : AppConfig.REASON_PASS;
        String code = trace.hardBlock == 1 ? AppConfig.BLOCK_TREND_WEAK : AppConfig.REASON_PASS;
        add(
                out,
                level,
                code,
                "趋势连续性",
                "连续状态="
                        + trace.sameModeCount
                        + "，确认次数="
                        + trace.confirmCount
                        + "，稳定分="
                        + UiFormatter.score(trace.stateStability)
                        + "，切换风险="
                        + UiFormatter.score(trace.modeSwitchRisk)
                        + "，早破风险="
                        + UiFormatter.score(trace.earlyBreakRisk)
                        + "，趋势连续分="
                        + UiFormatter.score(trace.trendContinuity),
                trace.trendContinuity);
    }

    private void addRecoveryReason(
            ArrayList<Models.ReasonItem> out, BreakRecoveryEngine.RecoveryResult recovery) {
        if (recovery == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_RECOVERY_WEAK,
                    "断点恢复",
                    "没有断点恢复证据，按保守处理",
                    0.0d);
            return;
        }
        boolean hard = recovery.hardBlock == 1;
        add(
                out,
                hard ? AppConfig.REASON_BLOCK : AppConfig.REASON_PASS,
                hard ? recovery.blockCode : AppConfig.REASON_PASS,
                "断点恢复",
                "断点类型="
                        + safe(recovery.breakType)
                        + "，恢复类型="
                        + safe(recovery.recoveryType)
                        + "，断点风险="
                        + UiFormatter.score(recovery.breakRisk)
                        + "，恢复力量="
                        + UiFormatter.score(recovery.recoveryPower)
                        + "，假恢复风险="
                        + UiFormatter.score(recovery.falseRecoveryRisk),
                recovery.recoveryPower);
    }

    private void addSimilarReason(ArrayList<Models.ReasonItem> out, Models.SimilarResult similar) {
        if (similar == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_SAMPLE_WEAK,
                    "相似形态",
                    "没有相似形态证据，样本保守",
                    0.0d);
            return;
        }
        String level;
        String code;
        if (similar.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            level = AppConfig.REASON_WARN;
            code = AppConfig.BLOCK_SAMPLE_WEAK;
        } else if (similar.similarHitRate >= AppConfig.SIMILAR_HIT_RATE_MIN) {
            level = AppConfig.REASON_PASS;
            code = AppConfig.REASON_PASS;
        } else {
            level = AppConfig.REASON_BLOCK;
            code = AppConfig.BLOCK_SAMPLE_WEAK;
        }
        add(
                out,
                level,
                code,
                "相似形态",
                "有效样本="
                        + similar.effectiveSample
                        + "，相似命中率="
                        + UiFormatter.probability(similar.similarHitRate)
                        + "，近期命中率="
                        + UiFormatter.probability(similar.recentHitRate)
                        + "，平均距离="
                        + UiFormatter.score(similar.avgDistance)
                        + "，置信分="
                        + UiFormatter.score(similar.confidence),
                similar.similarHitRate);
    }

    private void addMemoryReason(
            ArrayList<Models.ReasonItem> out, ShapeMemoryEngine.MemorySignal memory) {
        if (memory == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_COMBO_WEAK,
                    "形态记忆",
                    "没有形态记忆证据，按保守处理",
                    0.0d);
            return;
        }
        String level = memory.memoryScore >= 48.0d ? AppConfig.REASON_PASS : AppConfig.REASON_WARN;
        String code =
                memory.memoryScore >= 48.0d ? AppConfig.REASON_PASS : AppConfig.BLOCK_COMBO_WEAK;
        if (memory.usable == 1 && memory.memoryScore < 40.0d) {
            level = AppConfig.REASON_BLOCK;
            code = AppConfig.BLOCK_COMBO_WEAK;
        }
        add(
                out,
                level,
                code,
                "形态记忆",
                "记忆分="
                        + UiFormatter.score(memory.memoryScore)
                        + "，近期记忆率="
                        + UiFormatter.probability(memory.recentMemoryRate)
                        + "，128窗命中率="
                        + UiFormatter.probability(memory.hitRate128)
                        + "，样本="
                        + memory.sample128,
                memory.memoryScore);
    }

    private void addCalibrationReason(
            ArrayList<Models.ReasonItem> out, CalibrationEngine.CalibrationSignal calibration) {
        if (calibration == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "概率校准",
                    "没有校准结果，按保守处理",
                    0.0d);
            return;
        }
        String level = calibration.reliable == 1 ? AppConfig.REASON_PASS : AppConfig.REASON_WARN;
        add(
                out,
                level,
                AppConfig.REASON_PASS.equals(level)
                        ? AppConfig.REASON_PASS
                        : AppConfig.BLOCK_LOW_PROBABILITY,
                "概率校准",
                "概率桶="
                        + safe(calibration.bucketKey)
                        + "，原始概率="
                        + UiFormatter.probability(calibration.rawP)
                        + "，校准概率="
                        + UiFormatter.probability(calibration.calibratedP)
                        + "，最终概率="
                        + UiFormatter.probability(calibration.finalP)
                        + "，校准误差="
                        + UiFormatter.probability(calibration.error)
                        + "，门槛修正="
                        + UiFormatter.signedProbability(calibration.gateAdjust),
                calibration.error);
    }

    private void addGateReason(
            ArrayList<Models.ReasonItem> out, DynamicGateEngine.GateSignal gate) {
        if (gate == null) {
            add(
                    out,
                    AppConfig.REASON_WARN,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "动态门槛",
                    "没有动态门槛结果，按保守处理",
                    0.0d);
            return;
        }
        String level =
                gate.hardBlock == 1
                        ? AppConfig.REASON_BLOCK
                        : (gate.allowedByGate == 1
                                ? AppConfig.REASON_PASS
                                : AppConfig.REASON_BLOCK);
        String code =
                gate.hardBlock == 1
                        ? gate.blockCode
                        : (gate.allowedByGate == 1
                                ? AppConfig.REASON_PASS
                                : AppConfig.BLOCK_LOW_PROBABILITY);
        add(
                out,
                level,
                code,
                "动态门槛",
                "最终门槛="
                        + UiFormatter.probability(gate.finalGate)
                        + "，概率优势="
                        + UiFormatter.signedProbability(gate.edge)
                        + "，近100期频率="
                        + UiFormatter.probability(gate.freqRate100)
                        + "，近500期频率="
                        + UiFormatter.probability(gate.freqRate500)
                        + "，频率参与软风控但不救援不越门"
                        + "，审计风控修正="
                        + UiFormatter.signedProbability(gate.auditAdjust),
                gate.edge);
    }

    private void addDeepCandidateReason(ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        if (c.deepScore > 0.0d || c.aiJudgeScore > 0.0d || c.evidenceScore > 0.0d) {
            String level =
                    c.evidenceRisk <= AppConfig.EVIDENCE_RISK_MAX
                            ? AppConfig.REASON_PASS
                            : AppConfig.REASON_WARN;
            add(
                    out,
                    level,
                    AppConfig.REASON_PASS.equals(level)
                            ? AppConfig.REASON_PASS
                            : AppConfig.BLOCK_AI_CONFLICT,
                    "深度证据",
                    "DeepScore="
                            + UiFormatter.score(c.deepScore)
                            + "，AI分="
                            + UiFormatter.score(c.aiJudgeScore)
                            + "，证据分="
                            + UiFormatter.score(c.evidenceScore)
                            + "，证据风险="
                            + UiFormatter.score(c.evidenceRisk)
                            + "，Bias="
                            + UiFormatter.score(c.biasScore)
                            + "，AuditPenalty="
                            + UiFormatter.signedProbability(0.0d - c.auditPenalty),
                    c.evidenceScore);
        }
        if (c.deepProfileKey != null && c.deepProfileKey.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "DEEP_PROFILE",
                    "深度画像",
                    "画像="
                            + c.deepProfileKey
                            + "，画像命中率="
                            + UiFormatter.probability(c.deepProfileHitRate)
                            + "，画像样本="
                            + c.deepProfileSample
                            + "，晋级="
                            + safe(c.promotionStatus)
                            + "，A+="
                            + safe(c.aPlusStatus),
                    c.profileScore);
        }
    }

    private void addCandidateLearningReason(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        if (c.reasonSummary != null && c.reasonSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "CANDIDATE_LEARNING",
                    "候选学习说明",
                    displayReasonLong(c.reasonSummary),
                    0.0d);
        }
    }

    private void addDeepDecisionReason(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (d.deepScore > 0.0d || d.aiJudgeScore > 0.0d || d.evidenceScore > 0.0d) {
            String level =
                    d.evidenceRisk <= AppConfig.EVIDENCE_RISK_MAX
                            ? AppConfig.REASON_PASS
                            : AppConfig.REASON_WARN;
            add(
                    out,
                    level,
                    AppConfig.REASON_PASS.equals(level)
                            ? AppConfig.REASON_PASS
                            : AppConfig.BLOCK_AI_CONFLICT,
                    "深度AI证据",
                    "DeepScore="
                            + UiFormatter.score(d.deepScore)
                            + "，AI分="
                            + UiFormatter.score(d.aiJudgeScore)
                            + "，AI置信="
                            + UiFormatter.score(d.aiJudgeConfidence)
                            + "，证据分="
                            + UiFormatter.score(d.evidenceScore)
                            + "，证据风险="
                            + UiFormatter.score(d.evidenceRisk)
                            + "，冲突="
                            + UiFormatter.score(d.conflictLevel),
                    d.evidenceScore);
        }
        if (d.aiJudgeSummary != null && d.aiJudgeSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "AI_JUDGE",
                    "AI裁决摘要",
                    displayReasonLong(d.aiJudgeSummary),
                    d.aiJudgeScore);
        }
        if (d.deepReason != null && d.deepReason.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "DEEP_REASON",
                    "深度原因",
                    displayReasonLong(d.deepReason),
                    d.deepScore);
        }
        if (d.deepAttribution != null && d.deepAttribution.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "DEEP_ATTRIBUTION",
                    "深度归因",
                    displayReasonLong(d.deepAttribution),
                    0.0d);
        }
    }

    private void addAuditDecisionReason(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        if (d.auditPenalty > 0.0d || d.auditBoost > 0.0d) {
            String level = d.auditPenalty <= 0.010d ? AppConfig.REASON_INFO : AppConfig.REASON_WARN;
            add(
                    out,
                    level,
                    "AUDIT",
                    "概率审计",
                    "审计扣分="
                            + UiFormatter.signedProbability(0.0d - d.auditPenalty)
                            + "，旧审计增强字段="
                            + UiFormatter.signedProbability(d.auditBoost)
                            + "（仅兼容展示，不作为救援加分，不越过最终门）"
                            + "，校准误差="
                            + UiFormatter.probability(d.calibrationError),
                    0.0d - d.auditPenalty);
        }
        if (d.selfDiagnosisText != null && d.selfDiagnosisText.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "SELF_DIAGNOSIS",
                    "自诊断",
                    displayReasonLong(d.selfDiagnosisText),
                    0.0d);
        }
    }

    private void addScopeReason(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        String scope = d.statScope == null ? "" : d.statScope;
        if (AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope)) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "STAT_SCOPE",
                    "统计口径",
                    "本决策是REAL_TRADE，开奖结算后才计入真实胜率、真实盈亏、回撤；真实统计只读trades表已结算BET",
                    0.0d);
        } else if (AppConfig.STAT_SCOPE_SHADOW.equals(scope)) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "STAT_SCOPE",
                    "统计口径",
                    "本决策是SHADOW，只做观察学习和画像反哺，不计真实胜率，不计真实盈亏，不进真实校准",
                    0.0d);
        } else {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "STAT_SCOPE",
                    "统计口径",
                    "本决策是HOLD，不下注，不计真实胜率，不产生真实盈亏",
                    0.0d);
        }
    }

    private void addSettlementRuleReason(ArrayList<Models.ReasonItem> out, Models.Trade t) {
        if (t == null) {
            return;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "STRICT_SETTLEMENT_RULE",
                "严格结算规则",
                "只按目标期="
                        + safe(t.targetIssue)
                        + "、第"
                        + t.position
                        + "位置、K3="
                        + UiFormatter.numberGroup(t.n1, t.n2, t.n3)
                        + "结算。不能扫全10个位置，不能用其他位置命中冒充本单命中；实际号码必须等于预测三号之一。",
                0.0d);
    }

    private void addFinalConclusion(
            ArrayList<Models.ReasonItem> out,
            Models.ComboCandidate candidate,
            DynamicGateEngine.GateSignal gate) {
        if (candidate == null) {
            return;
        }
        if (gate != null && gate.hardBlock == 1) {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    gate.blockCode,
                    "最终结论",
                    "存在强制拦截：" + UiFormatter.blockReasonText(gate.blockCode) + "，本期不出手",
                    candidate.finalP);
            return;
        }
        double edge = candidate.edge;
        if (edge == 0.0d && candidate.finalP > 0.0d && candidate.dynamicGate > 0.0d) {
            edge = candidate.finalP - candidate.dynamicGate;
        }
        if (candidate.finalP >= candidate.dynamicGate && edge >= 0.0d) {
            add(
                    out,
                    AppConfig.REASON_PASS,
                    AppConfig.REASON_PASS,
                    "最终结论",
                    "候选超过动态门槛，可进入最终裁决；是否真实BET仍以DecisionGate最终结果为准；教师矩阵和V20因链不能单独放行BET。",
                    candidate.finalP);
        } else {
            add(
                    out,
                    AppConfig.REASON_BLOCK,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "最终结论",
                    "候选没有超过动态门槛，本期不出手",
                    candidate.finalP);
        }
    }

    private void addRuntimeTeacherMatrixBreakdown(
            ArrayList<Models.ReasonItem> out, Models.AppRuntimeState state) {
        if (out == null || state == null) {
            return;
        }
        String tmSummary =
                firstNonEmpty(
                        getStringField(state, "teacherNumberMatrixRealtimeSummary"),
                        getStringField(state, "teacher_number_matrix_realtime_summary"),
                        getStringField(state, "teacherMatrixCandidateSummary"),
                        getStringField(state, "teacher_matrix_candidate_summary"),
                        getStringField(state, "teacherNumberMatrixSummary"));
        if (tmSummary.length() == 0) {
            tmSummary =
                    firstNonEmpty(
                            getStringField(state, "teacher_number_matrix_summary"),
                            getStringField(state, "teacherMatrixSummary"),
                            getStringField(state, "teacher_matrix_summary"),
                            getStringField(state, "teacherSummary"),
                            getStringField(state, "tmSummary"));
        }
        String snapshotSummary =
                firstNonEmpty(
                        getStringField(state, "teacherSnapshotSummary"),
                        getStringField(state, "teacher_snapshot_summary"),
                        getStringField(state, "snapshotSummary"),
                        "");
        String runtimeProfile =
                firstNonEmpty(
                        getStringField(state, "runtimeProfileSummary"),
                        getStringField(state, "runtime_profile_summary"),
                        getStringField(state, "profileSummary"),
                        "");
        if (tmSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "TEACHER_MATRIX_RUNTIME",
                    "教师矩阵",
                    displayReasonLong(tmSummary),
                    0.0d);
        }
        if (snapshotSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "TEACHER_SNAPSHOT_RUNTIME",
                    "教师快照",
                    displayReasonLong(snapshotSummary),
                    0.0d);
        }
        if (runtimeProfile.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "RUNTIME_PROFILE_RUNTIME",
                    "画像反哺",
                    displayReasonLong(runtimeProfile),
                    0.0d);
        }
    }

    private void addRuntimeV20CauseChainBreakdown(
            ArrayList<Models.ReasonItem> out, Models.AppRuntimeState state) {
        if (out == null || state == null) {
            return;
        }
        String causeSummary =
                firstNonEmpty(
                        getStringField(state, "dynamicCauseChainSummary"),
                        getStringField(state, "dynamic_cause_chain_summary"),
                        getStringField(state, "causeChainSummary"),
                        getStringField(state, "v20CauseSummary"),
                        getStringField(state, "causeSummary"));
        String ruleSummary =
                firstNonEmpty(
                        getStringField(state, "ruleHealthSummary"),
                        getStringField(state, "shadowHealthSummary"),
                        getStringField(state, "falseStrongFreezeSummary"),
                        getStringField(state, "thirdGuardSummary"),
                        "");
        String predrawSummary =
                firstNonEmpty(
                        getStringField(state, "preDrawSummary"),
                        getStringField(state, "fullCandidateScanSummary"),
                        getStringField(state, "seedCauseExpansionSummary"),
                        "");
        if (causeSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_CAUSE_CHAIN_RUNTIME",
                    "V20动态因链",
                    displayReasonLong(causeSummary),
                    0.0d);
        }
        if (ruleSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_RULE_HEALTH_RUNTIME",
                    "V20规则健康",
                    displayReasonLong(ruleSummary),
                    0.0d);
        }
        if (predrawSummary.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_PREDRAW_RUNTIME",
                    "V20预开奖特征",
                    displayReasonLong(predrawSummary),
                    0.0d);
        }
    }

    private TeacherMatrixView readTeacherMatrixView(Object obj) {
        TeacherMatrixView v = new TeacherMatrixView();
        if (obj == null) {
            return v;
        }
        v.status =
                firstNonEmpty(
                        getStringField(obj, "teacherMatrixReadyStatus"),
                        getStringField(obj, "teacherMatrixStatus"),
                        getStringField(obj, "teacherNumberStatus"),
                        getStringField(obj, "teacherStatus"),
                        getStringField(obj, "tmStatus"));
        v.summary =
                firstNonEmpty(
                        getStringField(obj, "teacherMatrixSummary"),
                        getStringField(obj, "teacherNumberSummary"),
                        getStringField(obj, "teacherSummary"),
                        getStringField(obj, "tmSummary"),
                        "");
        if (v.summary.length() == 0) {
            String reason =
                    firstNonEmpty(
                            getStringField(obj, "reasonSummary"),
                            getStringField(obj, "reasonText"),
                            "");
            v.summary =
                    extractSegment(
                            reason,
                            new String[] {
                                "Teacher Matrix",
                                "TeacherMatrix",
                                "教师矩阵",
                                "教师号码矩阵",
                                "teacherMatrix",
                                "teacherNumber"
                            },
                            300);
        }
        v.sample =
                maxIntFields(
                        obj,
                        new String[] {
                            "teacherMatrixSample",
                            "teacherMatrixSampleCount",
                            "teacherNumberSample",
                            "teacherSample",
                            "teacherMatrixStateSample"
                        });
        v.comboSample =
                maxIntFields(
                        obj,
                        new String[] {
                            "teacherMatrixComboSample",
                            "teacherMatrixComboCount",
                            "teacherNumberComboSample"
                        });
        if (v.comboSample <= 0 && v.sample > 0) {
            v.comboSample = v.sample;
        }
        v.hitRate =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixHitRate", "teacherNumberHitRate", "teacherHitRate"
                        });
        v.comboHitRate =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixRecentHitRate",
                            "teacherMatrixComboHitRate",
                            "teacherNumberAvgHitRate",
                            "teacherNumberRecentHitRate"
                        });
        if (v.comboHitRate <= 0.0d) {
            v.comboHitRate = v.hitRate;
        }
        v.confidence =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixConfidence",
                            "teacherNumberConfidence",
                            "teacherConfidence",
                            "numberTeacherConfidence"
                        });
        v.score =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixScore",
                            "teacherMatrixComboScore",
                            "teacherMatrixNumberScore",
                            "teacherNumberScore",
                            "teacherComboScore",
                            "teacherScore",
                            "numberTeacherScore"
                        });
        v.risk =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixRiskPenalty",
                            "teacherMatrixRisk",
                            "teacherNumberRisk",
                            "teacherComboRisk",
                            "teacherRisk",
                            "numberTeacherRisk"
                        });
        v.probAdjust =
                maxAbsDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixProbAdjust",
                            "teacherNumberProbAdjust",
                            "numberTeacherProbAdjust",
                            "teacherProbAdjust"
                        });
        v.gateAdjust =
                maxAbsDoubleFields(
                        obj,
                        new String[] {
                            "teacherMatrixGateAdjust",
                            "teacherNumberGateAdjust",
                            "teacherGateAdjust"
                        });
        v.applied =
                maxIntFields(
                        obj,
                        new String[] {
                            "teacherMatrixApplied", "teacherApplied", "teacherNumberApplied"
                        });
        if (v.applied <= 0) {
            if (v.sample > 0
                    || v.comboSample > 0
                    || v.hitRate > 0.0d
                    || v.confidence > 0.0d
                    || v.score > 0.0d
                    || v.summary.length() > 0
                    || v.status.length() > 0) {
                v.applied = 1;
            }
        }
        return v;
    }

    private V20View readV20View(Object obj) {
        V20View v = new V20View();
        if (obj == null) {
            return v;
        }
        v.status =
                firstNonEmpty(
                        getStringField(obj, "dynamicCauseStatus"),
                        getStringField(obj, "dynamicCauseChainStatus"),
                        getStringField(obj, "causeChainStatus"),
                        getStringField(obj, "v20CauseStatus"),
                        getStringField(obj, "ruleHealthStatus"),
                        "");
        v.summary =
                firstNonEmpty(
                        getStringField(obj, "dynamicCauseChainSummary"),
                        getStringField(obj, "dynamicCauseSummary"),
                        getStringField(obj, "causeChainSummary"),
                        getStringField(obj, "v20CauseSummary"),
                        getStringField(obj, "ruleHealthSummary"));
        if (v.summary.length() == 0) {
            v.summary =
                    firstNonEmpty(
                            getStringField(obj, "thirdGuardSummary"),
                            getStringField(obj, "shadowHealthSummary"),
                            getStringField(obj, "falseStrongFreezeSummary"),
                            getStringField(obj, "counterfactualSummary"),
                            getStringField(obj, "seedCauseSummary"));
        }
        if (v.summary.length() == 0) {
            String reason =
                    firstNonEmpty(
                            getStringField(obj, "reasonSummary"),
                            getStringField(obj, "reasonText"),
                            "");
            v.summary =
                    extractSegment(
                            reason,
                            new String[] {
                                "V20因链",
                                "动态因链",
                                "因链审计",
                                "DynamicCauseChain",
                                "V20_CAUSE",
                                "ruleHealth",
                                "thirdGuard",
                                "falseStrong"
                            },
                            360);
        }
        v.causeScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "dynamicCauseChainScore",
                            "dynamicCauseScore",
                            "causeChainScore",
                            "causeScore",
                            "finalCauseScore",
                            "chainScore"
                        });
        v.riskScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "dynamicCauseChainRisk",
                            "dynamicCauseRisk",
                            "causeRisk",
                            "riskScore",
                            "chainRisk"
                        });
        v.positionScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "positionCauseScore", "positionHealthScore", "positionScore"
                        });
        v.numberScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "teacherNumberScore",
                            "teacherMatrixNumberScore",
                            "numberCauseScore",
                            "numberHealthScore"
                        });
        v.counterScore =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "counterfactualScore", "counterfactualRankScore", "rankAuditScore"
                        });
        v.thirdScore =
                maxDoubleFields(
                        obj, new String[] {"thirdGuardScore", "thirdGuardHealth", "guardScore"});
        v.ruleHealth =
                maxDoubleFields(obj, new String[] {"ruleHealthScore", "healthScore", "ruleScore"});
        v.shadowHealth = maxDoubleFields(obj, new String[] {"shadowHealthScore", "shadowScore"});
        v.falseRisk =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "falseStrongFreezeRisk",
                            "falseStrongRisk",
                            "falseStrongScore",
                            "freezeRisk",
                            "falseChampionRisk"
                        });
        v.seedScore =
                maxDoubleFields(
                        obj, new String[] {"seedExpandScore", "seedCauseScore", "seedScore"});
        v.hardBlock =
                maxIntFields(
                        obj,
                        new String[] {
                            "ruleHardBlock",
                            "ruleHealthHardCount",
                            "thirdGuardHardBlock",
                            "causeHardBlock",
                            "shadowHardBlock",
                            "falseStrongHardFreeze"
                        });
        v.supportScore =
                clamp01(
                        v.causeScore * 0.24d
                                + v.positionScore * 0.10d
                                + v.numberScore * 0.12d
                                + v.counterScore * 0.10d
                                + v.thirdScore * 0.13d
                                + v.ruleHealth * 0.15d
                                + v.shadowHealth * 0.06d
                                + v.seedScore * 0.10d);
        if (v.riskScore <= 0.0d) {
            v.riskScore =
                    clamp01(
                            v.falseRisk * 0.40d
                                    + maxDoubleFields(
                                                    obj,
                                                    new String[] {
                                                        "ruleHealthRisk",
                                                        "thirdGuardRisk",
                                                        "shadowHealthRisk",
                                                        "positionWrongRisk"
                                                    })
                                            * 0.40d
                                    + maxDoubleFields(
                                                    obj,
                                                    new String[] {
                                                        "counterfactualRisk", "seedCauseRisk"
                                                    })
                                            * 0.20d);
        }
        return v;
    }

    private void addV20EvidenceDecisionReason(ArrayList<Models.ReasonItem> out, Models.Decision d) {
        if (d == null) {
            return;
        }
        V20EvidenceView ev = readV20EvidenceView(d);
        if (!ev.hasAny()) {
            return;
        }
        String level = AppConfig.REASON_INFO;
        String code = "V20_REASON_EVIDENCE";
        if (ev.hardRisk > 0 || ev.counterContribution >= 0.680d) {
            level = AppConfig.REASON_WARN;
            code = "V20_REASON_COUNTER_RISK";
        } else if (ev.evidenceContribution >= 0.620d && ev.counterContribution <= 0.520d) {
            level = AppConfig.REASON_PASS;
            code = "V20_REASON_EVIDENCE_PASS";
        }
        add(
                out,
                level,
                code,
                "V20 可回放证据矩阵",
                buildV20EvidenceText(ev, "决策"),
                ev.evidenceContribution);
        if (ev.evidenceText.length() > 0) {
            add(
                    out,
                    AppConfig.REASON_INFO,
                    "V20_EVIDENCE_DETAIL",
                    "证据贡献明细",
                    displayReasonLong(ev.evidenceText),
                    ev.evidenceContribution);
        }
        if (ev.counterText.length() > 0) {
            add(
                    out,
                    ev.counterContribution >= 0.620d
                            ? AppConfig.REASON_WARN
                            : AppConfig.REASON_INFO,
                    "V20_COUNTER_DETAIL",
                    "反证贡献明细",
                    displayReasonLong(ev.counterText),
                    ev.counterContribution);
        }
    }

    private void addV20EvidenceCandidateReason(
            ArrayList<Models.ReasonItem> out, Models.ComboCandidate c) {
        if (c == null) {
            return;
        }
        V20EvidenceView ev = readV20EvidenceView(c);
        if (!ev.hasAny()) {
            return;
        }
        String level = AppConfig.REASON_INFO;
        String code = "V20_CANDIDATE_EVIDENCE";
        if (ev.hardRisk > 0 || ev.counterContribution >= 0.680d) {
            level = AppConfig.REASON_WARN;
            code = "V20_CANDIDATE_COUNTER_RISK";
        } else if (ev.evidenceContribution >= 0.620d && ev.counterContribution <= 0.520d) {
            level = AppConfig.REASON_PASS;
            code = "V20_CANDIDATE_EVIDENCE_PASS";
        }
        add(out, level, code, "候选可回放证据", buildV20EvidenceText(ev, "候选"), ev.evidenceContribution);
    }

    private void addRuntimeV20EvidenceBreakdown(
            ArrayList<Models.ReasonItem> out, Models.AppRuntimeState state) {
        if (state == null) {
            return;
        }
        V20EvidenceView ev = readV20EvidenceView(state);
        if (!ev.hasAny()) {
            return;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "V20_RUNTIME_EVIDENCE",
                "V20 实时证据摘要",
                buildV20EvidenceText(ev, "运行"),
                ev.evidenceContribution);
    }

    private void addV20EvidenceTradeReason(ArrayList<Models.ReasonItem> out, Models.Trade t) {
        if (t == null) {
            return;
        }
        V20EvidenceView ev = readV20EvidenceView(t);
        if (!ev.hasAny()) {
            return;
        }
        add(
                out,
                AppConfig.REASON_INFO,
                "V20_TRADE_EVIDENCE",
                "出手时可回放证据",
                buildV20EvidenceText(ev, "交易"),
                ev.evidenceContribution);
    }

    private V20EvidenceView readV20EvidenceView(Object obj) {
        V20EvidenceView ev = new V20EvidenceView();
        if (obj == null) {
            return ev;
        }
        ev.stateKey =
                firstNonEmpty(
                        getStringField(obj, "finalDecisionWindowStateKey"),
                        getStringField(obj, "decisionWindowStateKey"),
                        getStringField(obj, "dynamicGateWindowStateKey"),
                        getStringField(obj, "probabilityWindowStateKey"),
                        getStringField(obj, "probabilityStateKey"),
                        getStringField(obj, "teacherWindowStateKey"),
                        getStringField(obj, "teacherMatrixStateKey"),
                        getStringField(obj, "dynamicWindowStateKey"),
                        getStringField(obj, "dynamicStateKey"),
                        getStringField(obj, "windowStateKey"),
                        getStringField(obj, "stateKey"));
        ev.state12 =
                firstNonEmpty(
                        getStringField(obj, "stateKey12"),
                        getStringField(obj, "state_key_12"),
                        getStringField(obj, "windowState12"));
        ev.state24 =
                firstNonEmpty(
                        getStringField(obj, "stateKey24"),
                        getStringField(obj, "state_key_24"),
                        getStringField(obj, "windowState24"));
        ev.state48 =
                firstNonEmpty(
                        getStringField(obj, "stateKey48"),
                        getStringField(obj, "state_key_48"),
                        getStringField(obj, "windowState48"));
        ev.state96 =
                firstNonEmpty(
                        getStringField(obj, "stateKey96"),
                        getStringField(obj, "state_key_96"),
                        getStringField(obj, "windowState96"));
        ev.state360 =
                firstNonEmpty(
                        getStringField(obj, "stateKey360"),
                        getStringField(obj, "state_key_360"),
                        getStringField(obj, "windowState360"));
        ev.evidenceContribution =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "decisionEvidenceContribution",
                            "finalDecisionEvidenceContribution",
                            "dynamicGateEvidenceContribution",
                            "probabilityEvidenceContribution",
                            "dynamicEvidenceContribution",
                            "teacherEvidenceContribution",
                            "evidenceContribution",
                            "evidenceScore"
                        });
        ev.counterContribution =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "decisionCounterContribution",
                            "finalDecisionCounterContribution",
                            "dynamicGateCounterContribution",
                            "probabilityCounterEvidence",
                            "probabilityCounterContribution",
                            "dynamicCounterContribution",
                            "teacherCounterContribution",
                            "counterContribution",
                            "counterEvidence",
                            "riskScore",
                            "evidenceRisk"
                        });
        ev.sampleReliability =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "decisionSampleReliability",
                            "dynamicGateSampleReliability",
                            "probabilitySampleReliability",
                            "dynamicSampleTrust",
                            "teacherSampleTrust",
                            "sampleTrust",
                            "sampleReliability",
                            "confidence"
                        });
        ev.recentValidity =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "decisionRecentValidity",
                            "dynamicGateRecentValidity",
                            "probabilityRecentValidity",
                            "dynamicRecentEffectiveness",
                            "teacherRecentEffectiveness",
                            "recentEffectiveness",
                            "recentValidity"
                        });
        ev.featureDistance =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "decisionFeatureDistance",
                            "dynamicGateFeatureDistance",
                            "probabilityFeatureDistance",
                            "dynamicFeatureDistance",
                            "teacherFeatureDistance",
                            "featureDistance"
                        });
        ev.similarWindowSample =
                maxIntFields(
                        obj,
                        new String[] {
                            "decisionSimilarWindowSample",
                            "dynamicGateSimilarWindowSample",
                            "probabilitySimilarWindowSample",
                            "dynamicSimilarWindowSample",
                            "teacherSimilarWindowSample",
                            "similarWindowSample"
                        });
        ev.similarWindowHitRate =
                maxDoubleFields(
                        obj,
                        new String[] {
                            "decisionSimilarWindowHitRate",
                            "dynamicGateSimilarWindowHitRate",
                            "probabilitySimilarWindowHitRate",
                            "dynamicSimilarWindowHitRate",
                            "teacherSimilarWindowHitRate",
                            "similarWindowHitRate"
                        });
        ev.evidenceText =
                firstNonEmpty(
                        getStringField(obj, "decisionEvidenceText"),
                        getStringField(obj, "dynamicGateEvidenceText"),
                        getStringField(obj, "probabilityEvidenceText"),
                        getStringField(obj, "dynamicEvidenceText"),
                        getStringField(obj, "teacherEvidenceText"),
                        getStringField(obj, "evidenceSupportText"));
        ev.counterText =
                firstNonEmpty(
                        getStringField(obj, "decisionCounterEvidenceText"),
                        getStringField(obj, "dynamicGateCounterEvidenceText"),
                        getStringField(obj, "probabilityCounterEvidenceText"),
                        getStringField(obj, "dynamicCounterEvidenceText"),
                        getStringField(obj, "teacherCounterEvidenceText"),
                        getStringField(obj, "counterEvidenceText"));
        ev.actionAdvice =
                firstNonEmpty(
                        getStringField(obj, "decisionActionAdvice"),
                        getStringField(obj, "dynamicGateActionAdvice"),
                        getStringField(obj, "probabilityActionAdvice"),
                        getStringField(obj, "dynamicActionAdvice"),
                        getStringField(obj, "teacherActionAdvice"),
                        getStringField(obj, "actionAdvice"));
        ev.hardRisk =
                maxIntFields(
                        obj,
                        new String[] {
                            "decisionHardRisk",
                            "dynamicHardRisk",
                            "dynamicGateHardRisk",
                            "probabilityHardRisk",
                            "falseStrongHardFreeze",
                            "thirdGuardHardBlock",
                            "ruleHardBlock"
                        });
        if (ev.actionAdvice.length() == 0) {
            ev.actionAdvice = buildV20ActionAdvice(ev);
        }
        return ev;
    }

    private String buildV20EvidenceText(V20EvidenceView ev, String prefix) {
        if (ev == null) {
            return "暂无 V20 证据";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(prefix == null || prefix.length() == 0 ? "V20" : prefix);
        sb.append("证据矩阵：状态指纹=");
        sb.append(firstNonEmpty(ev.stateKey, "未写入"));
        sb.append("；12/24/48/96/360=");
        sb.append(shortKey(ev.state12));
        sb.append("/");
        sb.append(shortKey(ev.state24));
        sb.append("/");
        sb.append(shortKey(ev.state48));
        sb.append("/");
        sb.append(shortKey(ev.state96));
        sb.append("/");
        sb.append(shortKey(ev.state360));
        sb.append("；证据贡献=");
        sb.append(UiFormatter.probability(ev.evidenceContribution));
        sb.append("；反证贡献=");
        sb.append(UiFormatter.probability(ev.counterContribution));
        sb.append("；样本可信=");
        sb.append(UiFormatter.probability(ev.sampleReliability));
        sb.append("；近期有效性=");
        sb.append(UiFormatter.probability(ev.recentValidity));
        sb.append("；特征距离=");
        sb.append(UiFormatter.probability(ev.featureDistance));
        sb.append("；相似窗样本=");
        sb.append(ev.similarWindowSample);
        sb.append("；相似窗命中率=");
        sb.append(UiFormatter.probability(ev.similarWindowHitRate));
        sb.append("；动作建议=");
        sb.append(displayReason(firstNonEmpty(ev.actionAdvice, "观察，不直接救援真实BET")));
        sb.append("；说明=原因解释只展示可复查证据，不生成BET，不结算，不写真实盈亏");
        return displayReasonLong(sb.toString());
    }

    private String buildV20ActionAdvice(V20EvidenceView ev) {
        if (ev == null) {
            return "HOLD：证据为空，只展示，不出手";
        }
        if (ev.hardRisk > 0 || ev.counterContribution >= 0.700d) {
            return "HOLD/SHADOW：反证或硬风险偏强，不允许用原因层救援真实BET";
        }
        if (ev.evidenceContribution >= 0.620d
                && ev.counterContribution <= 0.520d
                && ev.sampleReliability >= 0.360d) {
            return "ALLOW_TO_GATE：证据强于反证，可交给DecisionGateEngine最终裁决";
        }
        if (ev.evidenceContribution >= 0.540d && ev.counterContribution <= 0.600d) {
            return "WATCH_TO_GATE：证据略强，建议观察并进入最终门复核";
        }
        return "SHADOW/HOLD：证据不足或样本可信不足，只记录原因，不计真实盈亏";
    }

    private String shortKey(String s) {
        if (s == null || s.length() == 0) {
            return "--";
        }
        if (s.length() <= 18) {
            return s;
        }
        return s.substring(0, 18);
    }

    private void add(
            ArrayList<Models.ReasonItem> out,
            String level,
            String code,
            String title,
            String detail,
            double value) {
        if (out == null) {
            return;
        }
        Models.ReasonItem item = new Models.ReasonItem();
        item.level = level == null ? AppConfig.REASON_INFO : level;
        item.code = code == null ? "" : code;
        item.title = title == null ? "" : title;
        item.detail = detail == null ? "" : displayReason(shortText(detail, DETAIL_LIMIT));
        item.value = value;
        item.passed = AppConfig.REASON_PASS.equals(item.level) ? 1 : 0;
        item.createdAt = System.currentTimeMillis();
        out.add(item);
    }

    private void trim(ArrayList<Models.ReasonItem> out) {
        if (out == null) {
            return;
        }
        while (out.size() > AppConfig.REASON_SHOW_LIMIT) {
            out.remove(out.size() - 1);
        }
    }

    private boolean isShapeSafe(String mode, double driftRisk, double breakDepth) {
        if (AppConfig.MODE_CHAOS.equals(mode)
                || AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || "REVERSAL".equals(mode)) {
            return false;
        }
        if (driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            return false;
        }
        if (breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            return false;
        }
        return true;
    }

    private Models.Decision copyDecisionForReason(Models.Decision src) {
        if (src == null) {
            return null;
        }
        Models.Decision out = new Models.Decision();
        copyPublicFields(src, out);
        return out;
    }

    private Models.Trade copyTradeForReason(Models.Trade src) {
        if (src == null) {
            return null;
        }
        Models.Trade out = new Models.Trade();
        copyPublicFields(src, out);
        return out;
    }

    private void copyPublicFields(Object src, Object dst) {
        if (src == null || dst == null) {
            return;
        }
        try {
            Field[] fields = src.getClass().getFields();
            if (fields == null) {
                return;
            }
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                if (f == null) {
                    continue;
                }
                try {
                    Field to = dst.getClass().getField(f.getName());
                    to.set(dst, f.get(src));
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
        }
    }

    private void normalizeDecisionForReason(Models.Decision d) {
        if (d == null) {
            return;
        }
        d.decision = normalizeDecisionTextForReason(d.decision);
        if (d.result != null && d.result.length() > 0) {
            d.result = normalizeResultForReason(d.result);
        }
        if (d.edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
        }
        if (d.k3Mask <= 0 && d.n1 > 0 && d.n2 > 0 && d.n3 > 0) {
            try {
                d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
            } catch (Throwable e) {
            }
        }
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        } else {
            d.statScope = AppConfig.STAT_SCOPE_HOLD;
        }
    }

    private String normalizeDecisionTextForReason(String decision) {
        if (decision == null) {
            return AppConfig.DECISION_HOLD;
        }
        String d = decision.trim();
        if (d.length() == 0) {
            return AppConfig.DECISION_HOLD;
        }
        if (AppConfig.DECISION_BET.equals(d)
                || "BET".equalsIgnoreCase(d)
                || "REAL_BET".equalsIgnoreCase(d)
                || "REAL_TRADE".equalsIgnoreCase(d)
                || "下注".equals(d)
                || "出手".equals(d)) {
            return AppConfig.DECISION_BET;
        }
        if (AppConfig.DECISION_SHADOW.equals(d)
                || "SHADOW".equalsIgnoreCase(d)
                || "SHADOW_BET".equalsIgnoreCase(d)
                || "影子".equals(d)
                || "影子观察".equals(d)
                || "观察".equals(d)) {
            return AppConfig.DECISION_SHADOW;
        }
        return AppConfig.DECISION_HOLD;
    }

    private void normalizeTradeForReason(Models.Trade t) {
        if (t == null) {
            return;
        }
        t.result = normalizeResultForReason(t.result);
        if (t.edge == 0.0d && t.finalP > 0.0d && t.dynamicGate > 0.0d) {
            t.edge = t.finalP - t.dynamicGate;
        }
        if (t.statScope == null || t.statScope.length() == 0) {
            t.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        }
        if (AppConfig.RESULT_WIN.equals(t.result)) {
            t.hit = 1;
        } else if (AppConfig.RESULT_MISS.equals(t.result)) {
            t.hit = 0;
        }
    }

    private String normalizeResultForReason(String result) {
        if (result == null) {
            return AppConfig.RESULT_WAIT;
        }
        String r = result.trim();
        if (r.length() == 0) {
            return AppConfig.RESULT_WAIT;
        }
        if (AppConfig.RESULT_WIN.equals(r) || "HIT".equalsIgnoreCase(r) || "命中".equals(r)) {
            return AppConfig.RESULT_WIN;
        }
        if (AppConfig.RESULT_MISS.equals(r)
                || "LOSE".equalsIgnoreCase(r)
                || "LOSS".equalsIgnoreCase(r)
                || "未中".equals(r)
                || "不中".equals(r)) {
            return AppConfig.RESULT_MISS;
        }
        if (AppConfig.RESULT_DATA_ERROR.equals(r)
                || "DATA_ERROR".equalsIgnoreCase(r)
                || "数据异常".equals(r)) {
            return AppConfig.RESULT_DATA_ERROR;
        }
        if (AppConfig.RESULT_VOID.equals(r) || "VOID".equalsIgnoreCase(r) || "作废".equals(r)) {
            return AppConfig.RESULT_VOID;
        }
        if (AppConfig.RESULT_WAIT.equals(r)
                || "WAIT".equalsIgnoreCase(r)
                || "WAITING".equalsIgnoreCase(r)
                || "等待".equals(r)
                || "等待结算".equals(r)) {
            return AppConfig.RESULT_WAIT;
        }
        return r;
    }

    private String readState(DbStore store, String key, String def) {
        if (store == null || key == null || key.length() == 0) {
            return def == null ? "" : def;
        }
        try {
            return store.getState(key, def == null ? "" : def);
        } catch (Throwable e) {
            return def == null ? "" : def;
        }
    }

    private String getStringField(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return "";
        }
        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            if (v == null) {
                return "";
            }
            return String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private int getIntField(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            if (v == null) {
                return def;
            }
            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }
            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
            return def;
        }
    }

    private double getDoubleField(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            if (v == null) {
                return def;
            }
            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }
            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            return parseDouble(String.valueOf(v));
        } catch (Throwable e) {
            return def;
        }
    }

    private double maxDoubleFields(Object obj, String[] fields) {
        if (obj == null || fields == null || fields.length == 0) {
            return 0.0d;
        }
        double best = 0.0d;
        for (int i = 0; i < fields.length; i++) {
            double v = getDoubleField(obj, fields[i], 0.0d);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private double maxAbsDoubleFields(Object obj, String[] fields) {
        if (obj == null || fields == null || fields.length == 0) {
            return 0.0d;
        }
        double best = 0.0d;
        for (int i = 0; i < fields.length; i++) {
            double v = getDoubleField(obj, fields[i], 0.0d);
            if (Math.abs(v) > Math.abs(best)) {
                best = v;
            }
        }
        return best;
    }

    private int maxIntFields(Object obj, String[] fields) {
        if (obj == null || fields == null || fields.length == 0) {
            return 0;
        }
        int best = 0;
        for (int i = 0; i < fields.length; i++) {
            int v = getIntField(obj, fields[i], 0);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private double parseDouble(String text) {
        if (text == null || text.length() == 0) {
            return 0.0d;
        }
        try {
            String s = text.trim();
            if (s.endsWith("%")) {
                s = s.substring(0, s.length() - 1);
                return Double.parseDouble(s) / 100.0d;
            }
            return Double.parseDouble(s);
        } catch (Throwable e) {
            return 0.0d;
        }
    }

    private double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v < 0.0d) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private String extractSegment(String text, String[] keys, int max) {
        if (text == null || text.length() == 0 || keys == null || keys.length == 0) {
            return "";
        }
        int best = -1;
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            if (k == null || k.length() == 0) {
                continue;
            }
            int p = text.indexOf(k);
            if (p >= 0 && (best < 0 || p < best)) {
                best = p;
            }
        }
        if (best < 0) {
            return "";
        }
        int end = text.length();
        String[] stops = new String[] {"；", "。", "\n"};
        for (int i = 0; i < stops.length; i++) {
            int p = text.indexOf(stops[i], best + 8);
            if (p > best && p < end) {
                end = p + 1;
            }
        }
        if (end <= best) {
            end = Math.min(text.length(), best + max);
        }
        String s = text.substring(best, Math.min(end, text.length()));
        if (max <= 0) {
            max = 200;
        }
        return shortText(s, max);
    }

    private String firstNonEmpty(String... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v != null && v.length() > 0) {
                return v;
            }
        }
        return "";
    }

    private String safe(String value) {
        if (value == null || value.length() == 0) {
            return "--";
        }
        return UiFormatter.sanitizeForUi(value);
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }
        if (max <= 0) {
            max = 120;
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private String displayReasonLong(String text) {
        return shortText(displayReason(text), LONG_DETAIL_LIMIT);
    }

    private String displayReason(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        String s = UiFormatter.sanitizeForUi(text);
        s = replaceSafe(s, "FinalGate", "最终门槛");
        s = replaceSafe(s, "FinalP", "最终概率");
        s = replaceSafe(s, "RawP", "原始概率");
        s = replaceSafe(s, "CalP", "校准概率");
        s = replaceSafe(s, "Calibrated", "校准概率");
        s = replaceSafe(s, "Gate", "出手门槛");
        s = replaceSafe(s, "Edge", "概率优势");
        s = replaceSafe(s, "Block", "拦截原因");
        s = replaceSafe(s, "PreDraw", "预开奖特征");
        s = replaceSafe(s, "FullCandidateScan", "全候选扫描");
        s = replaceSafe(s, "CausalFactor", "因果因子");
        s = replaceSafe(s, "PositionCause", "位置因子");
        s = replaceSafe(s, "TeacherNumber", "教师号码矩阵");
        s = replaceSafe(s, "Teacher Matrix", "教师矩阵");
        s = replaceSafe(s, "TeacherMatrix", "教师矩阵");
        s = replaceSafe(s, "teacherMatrix", "教师矩阵");
        s = replaceSafe(s, "teacherNumber", "教师号码矩阵");
        s = replaceSafe(s, "Counterfactual", "反事实");
        s = replaceSafe(s, "ThirdGuard", "第三防线");
        s = replaceSafe(s, "RuleHealth", "规则健康");
        s = replaceSafe(s, "ShadowHealth", "影子健康");
        s = replaceSafe(s, "FalseStrong", "假强");
        s = replaceSafe(s, "Freeze", "冻结");
        s = replaceSafe(s, "SeedCause", "种子扩展");
        s = replaceSafe(s, "DynamicCauseChain", "动态因链");
        s = replaceSafe(s, "CauseChain", "因链");
        s = replaceSafe(s, "causeChain", "因链");
        s = replaceSafe(s, "ruleHealth", "规则健康");
        s = replaceSafe(s, "shadowHealth", "影子健康");
        s = replaceSafe(s, "thirdGuard", "第三防线");
        s = replaceSafe(s, "counterfactual", "反事实");
        s = replaceSafe(s, "TrendLife", "趋势寿命");
        s = replaceSafe(s, "trendLife", "趋势寿命");
        s = replaceSafe(s, "DriftRisk", "漂移风险");
        s = replaceSafe(s, "driftRisk", "漂移风险");
        s = replaceSafe(s, "BreakDepth", "破位深度");
        s = replaceSafe(s, "breakDepth", "破位深度");
        s = replaceSafe(s, "RecoveryPower", "恢复力量");
        s = replaceSafe(s, "Recovery", "恢复分");
        s = replaceSafe(s, "BreakRisk", "断点风险");
        s = replaceSafe(s, "FalseRecovery", "假恢复风险");
        s = replaceSafe(s, "Similar", "相似形态");
        s = replaceSafe(s, "Sample", "样本");
        s = replaceSafe(s, "ComboHR", "组合命中率");
        s = replaceSafe(s, "ChampionGap", "主优势差");
        s = replaceSafe(s, "ComboGap", "组合优势差");
        s = replaceSafe(s, "StateKey", "状态指纹");
        s = replaceSafe(s, "stateKey", "状态指纹");
        s = replaceSafe(s, "ShapeKey", "形态指纹");
        s = replaceSafe(s, "shapeKey", "形态指纹");
        s = replaceSafe(s, "DeepScore", "深度分");
        s = replaceSafe(s, "Deep", "深度");
        s = replaceSafe(s, "Evidence", "证据");
        s = replaceSafe(s, "AuditPenalty", "审计扣分");
        s = replaceSafe(s, "Audit", "审计");
        s = replaceSafe(s, "Teacher", "教师层");
        s = replaceSafe(s, "Profile", "画像");
        s = replaceSafe(s, "Promotion", "晋级");
        s = replaceSafe(s, "A_PLUS", "A+");
        s = replaceSafe(s, "A+Status", "A+状态");
        s = replaceSafe(s, "teacher_number_matrix_realtime_summary", "教师号码矩阵实时摘要");
        s = replaceSafe(s, "teacher_number_matrix_summary", "教师号码矩阵摘要");
        s = replaceSafe(s, "teacher_matrix_candidate_summary", "教师矩阵候选摘要");
        s = replaceSafe(s, "runtime_profile_cache", "实时画像缓存");
        s = replaceSafe(s, "state_number_profile", "状态号码画像");
        s = replaceSafe(s, "learning_case_log", "学习案例库");
        s = replaceSafe(s, "auto_learn", "后台学习");
        s = replaceSafe(s, "deep_learning_bridge", "深度桥接");
        s = replaceSafe(s, "teacher_matrix_summary", "教师矩阵摘要");
        s = replaceSafe(s, "teacher_snapshot_summary", "教师快照摘要");
        s = replaceSafe(s, "runtime_profile_summary", "实时画像摘要");
        s = replaceSafe(s, "STRICT_SINGLE_POSITION_K3_FAST_CATCHUP", "严格单位置K3补扫");
        s = replaceSafe(s, "STRICT_SINGLE_POSITION_K3", "严格单位置K3");
        s = replaceSafe(s, "STRICT_SETTLEMENT_RULE", "严格结算规则");
        s = replaceSafe(s, "RUNNING", "运行中");
        s = replaceSafe(s, "STOPPED", "已停止");
        s = replaceSafe(s, "FETCHING", "抓取中");
        s = replaceSafe(s, "ANALYZING", "分析中");
        s = replaceSafe(s, "SETTLING", "结算中");
        s = replaceSafe(s, "LEARNING", "学习中");
        s = replaceSafe(s, "BACKTESTING", "回测中");
        s = replaceSafe(s, "WAITING", "等待中");
        s = replaceSafe(s, "IDLE", "空闲");
        s = replaceSafe(s, "SKIP", "跳过");
        s = replaceSafe(s, "ERROR", "异常");
        s = replaceSafe(s, "WARN", "警告");
        s = replaceSafe(s, "INFO", "提示");
        s = replaceSafe(s, "READY", "就绪");
        s = replaceSafe(s, "NORMAL", "正常");
        s = replaceSafe(s, "STRONG", "强支持");
        s = replaceSafe(s, "RISK", "风险");
        s = replaceSafe(s, "WEAK_SAMPLE", "样本偏弱");
        s = replaceSafe(s, "NO_TEACHER", "暂无教师样本");
        s = replaceSafe(s, AppConfig.STAT_SCOPE_REAL_TRADE, "真实BET");
        s = replaceSafe(s, AppConfig.STAT_SCOPE_SHADOW, "影子观察");
        s = replaceSafe(s, AppConfig.STAT_SCOPE_HOLD, "不出手");
        s = replaceSafe(s, AppConfig.DECISION_SHADOW, "影子观察");
        s = replaceSafe(s, AppConfig.DECISION_HOLD, "不出手");
        s = replaceSafe(s, AppConfig.DECISION_BET, "出手");
        s = replaceSafe(s, "HIT", "命中");
        s = replaceSafe(s, "WIN", "命中");
        s = replaceSafe(s, "LOSE", "未中");
        s = replaceSafe(s, "LOSS", "未中");
        s = replaceSafe(s, "MISS", "未中");
        s = replaceSafe(s, "WAIT", "等待");
        s = replaceSafe(s, AppConfig.MODE_CHAOS, "无有效形态");
        s = replaceSafe(s, AppConfig.MODE_FORMING, "形态形成中");
        s = replaceSafe(s, AppConfig.MODE_CONFIRMED, "形态已确认");
        s = replaceSafe(s, AppConfig.MODE_ACCELERATING, "趋势加速");
        s = replaceSafe(s, AppConfig.MODE_PULLBACK, "趋势回拉");
        s = replaceSafe(s, AppConfig.MODE_RECOVERING, "恢复中");
        s = replaceSafe(s, AppConfig.MODE_WEAKENING, "趋势转弱");
        s = replaceSafe(s, AppConfig.MODE_BROKEN, "形态破位");
        s = replaceSafe(s, AppConfig.MODE_REVERSING, "反转风险");
        s = replaceSafe(s, "REVERSAL", "反转风险");
        s = replaceSafe(s, "STABLE", "稳定形态");
        s = replaceSafe(s, "TRANSITION", "过渡形态");
        s = replaceSafe(s, AppConfig.BLOCK_HISTORY_WEAK, "历史数据不足");
        s = replaceSafe(s, AppConfig.BLOCK_LOW_PROBABILITY, "概率不足");
        s = replaceSafe(s, AppConfig.BLOCK_SAMPLE_WEAK, "样本不足");
        s = replaceSafe(s, AppConfig.BLOCK_TREND_WEAK, "趋势偏弱");
        s = replaceSafe(s, AppConfig.BLOCK_DRIFT_HIGH, "漂移过高");
        s = replaceSafe(s, AppConfig.BLOCK_BREAK_HIGH, "破位过深");
        s = replaceSafe(s, AppConfig.BLOCK_RECOVERY_WEAK, "恢复不足");
        s = replaceSafe(s, AppConfig.BLOCK_POSITION_WEAK, "位置优势不足");
        s = replaceSafe(s, AppConfig.BLOCK_COMBO_WEAK, "组合优势不足");
        s = replaceSafe(s, AppConfig.BLOCK_FREQ_HIGH, "频率过高");
        s = replaceSafe(s, AppConfig.BLOCK_DATA_ERROR, "数据异常");
        s = replaceSafe(s, AppConfig.BLOCK_OPEN_TRADE_EXISTS, "已有未结算真实BET");
        s = replaceSafe(s, AppConfig.BLOCK_BACKTEST_WEAK, "回测偏弱");
        s = replaceSafe(s, AppConfig.BLOCK_FULL_MODE_BUSY, "完整深测忙碌");
        s = replaceSafe(s, AppConfig.BLOCK_AI_CONFLICT, "AI证据冲突");
        s = replaceSafe(s, AppConfig.BLOCK_AUDIT_OVER, "概率审计超限");
        s = replaceSafe(s, AppConfig.BLOCK_A_PLUS_NOT_READY, "A+未就绪");
        s = replaceSafe(s, AppConfig.BLOCK_BIAS_WEAK, "偏差证据不足");
        s = replaceSafe(s, AppConfig.BLOCK_SCHEDULER_BUSY, "实时调度忙碌");
        s = replaceSafe(s, AppConfig.MISS_POSITION_LOST, "主位判断错误");
        s = replaceSafe(s, AppConfig.MISS_K3_RANK_4_6, "实际号码落在第4至第6候选");
        s = replaceSafe(s, AppConfig.MISS_TREND_BROKEN, "趋势真实破坏");
        s = replaceSafe(s, AppConfig.MISS_FALSE_RECOVERY, "假恢复");
        s = replaceSafe(s, AppConfig.MISS_DRIFT_SPIKE, "漂移突然升高");
        s = replaceSafe(s, AppConfig.MISS_HOT_REVERSE, "过热反转");
        s = replaceSafe(s, AppConfig.MISS_MARKOV_FAILED, "转移模型失效");
        s = replaceSafe(s, AppConfig.MISS_THIRD_WEAK, "第三号码强度不足");
        s = replaceSafe(s, AppConfig.MISS_SAMPLE_WEAK, "相似样本支撑不足");
        s = replaceSafe(s, AppConfig.MISS_DATA_OFFSET, "错期或结算错位");
        s = replaceSafe(s, AppConfig.MISS_UNKNOWN, "未知错因");
        s = replaceSafe(s, AppConfig.REASON_PASS, "通过");
        s = replaceSafe(s, AppConfig.REASON_BLOCK, "拦截");
        s = replaceSafe(s, AppConfig.REASON_WARN, "警告");
        s = replaceSafe(s, AppConfig.REASON_INFO, "提示");
        s = replaceSafe(s, "NO_SAMPLE", "无教师样本中性");
        s = replaceSafe(s, "WATCH", "观察");
        s = replaceSafe(s, "PENDING", "待复核");
        s = replaceSafe(s, "NEUTRAL", "中性");
        s = replaceSafe(s, "FREEZE", "冻结");
        s = replaceSafe(s, "RISK", "风险");
        s = replaceSafe(s, "DOWNGRADE_TO_SHADOW", "降级为SHADOW");
        s = replaceSafe(s, "PROMOTE_SHADOW_CANDIDATE", "SHADOW晋级候选");
        s = replaceSafe(s, "REAL_BET_PROBE", "真实BET探测字段");
        return s;
    }

    private String replaceSafe(String text, String from, String to) {
        if (text == null) {
            return "";
        }
        if (from == null || from.length() == 0) {
            return text;
        }
        if (to == null) {
            to = "";
        }
        return text.replace(from, to);
    }

    private static final class MainChainReasonView {
        public String finalStatus;
        public String actionAdvice;
        public String finalRejectCode;
        public String mainChainRejectCode;
        public String ruleRejectCode;
        public String thirdRejectCode;
        public String shadowRejectCode;
        public String adaptiveRejectCode;
        public String adaptiveStatus;
        public String adaptiveActionAdvice;
        public String shadowPromotionStatus;
        public String dynamicGateActionAdvice;
        public String fieldChainVersion;
        public double qualityScore;
        public double riskScore;
        public double evidenceScore;
        public double counterScore;
        public double netScore;
        public double positionNeedScore;
        public double numberSupplyScore;
        public double coverageQuality;
        public double thirdQuality;
        public double ruleHealthScore;
        public double ruleDecayRisk;
        public double thirdDragRisk;
        public double counterfactualRisk;
        public double shadowPromoteScore;
        public boolean teacherNoSampleNeutral;
        public boolean v20WatchNeutral;
        public boolean adaptiveWatchNeutral;
        public int hardRiskFlag;
        public int freezeFlag;

        public MainChainReasonView() {
            finalStatus = "";
            actionAdvice = "";
            finalRejectCode = "";
            mainChainRejectCode = "";
            ruleRejectCode = "";
            thirdRejectCode = "";
            shadowRejectCode = "";
            adaptiveRejectCode = "";
            adaptiveStatus = "";
            adaptiveActionAdvice = "";
            shadowPromotionStatus = "";
            dynamicGateActionAdvice = "";
            fieldChainVersion = "";
            qualityScore = 0.0d;
            riskScore = 0.0d;
            evidenceScore = 0.0d;
            counterScore = 0.0d;
            netScore = 0.0d;
            positionNeedScore = 0.0d;
            numberSupplyScore = 0.0d;
            coverageQuality = 0.0d;
            thirdQuality = 0.0d;
            ruleHealthScore = 0.0d;
            ruleDecayRisk = 0.0d;
            thirdDragRisk = 0.0d;
            counterfactualRisk = 0.0d;
            shadowPromoteScore = 0.0d;
            teacherNoSampleNeutral = false;
            v20WatchNeutral = false;
            adaptiveWatchNeutral = false;
            hardRiskFlag = 0;
            freezeFlag = 0;
        }

        public boolean hasAny() {
            return finalStatus.length() > 0
                    || actionAdvice.length() > 0
                    || finalRejectCode.length() > 0
                    || mainChainRejectCode.length() > 0
                    || adaptiveStatus.length() > 0
                    || shadowPromotionStatus.length() > 0
                    || qualityScore > 0.0d
                    || riskScore > 0.0d
                    || evidenceScore > 0.0d
                    || counterScore > 0.0d
                    || netScore != 0.0d
                    || positionNeedScore > 0.0d
                    || numberSupplyScore > 0.0d
                    || coverageQuality > 0.0d
                    || thirdQuality > 0.0d
                    || ruleHealthScore > 0.0d
                    || ruleDecayRisk > 0.0d
                    || thirdDragRisk > 0.0d
                    || counterfactualRisk > 0.0d
                    || shadowPromoteScore > 0.0d
                    || teacherNoSampleNeutral
                    || v20WatchNeutral
                    || adaptiveWatchNeutral
                    || hardRiskFlag > 0
                    || freezeFlag > 0
                    || fieldChainVersion.length() > 0;
        }

        public boolean hasHardRisk() {
            return hardRiskFlag > 0
                    || freezeFlag > 0
                    || riskScore >= 0.760d
                    || counterScore >= 0.760d
                    || ruleDecayRisk >= 0.760d
                    || thirdDragRisk >= 0.760d
                    || counterfactualRisk >= 0.760d
                    || finalRejectCode.indexOf("FREEZE") >= 0
                    || finalRejectCode.indexOf("HARD") >= 0
                    || adaptiveRejectCode.indexOf("FREEZE") >= 0
                    || adaptiveRejectCode.indexOf("HARD") >= 0;
        }

        public String neutralSummary() {
            StringBuilder sb = new StringBuilder();
            if (teacherNoSampleNeutral) {
                sb.append("Teacher无样本中性 ");
            }
            if (v20WatchNeutral) {
                sb.append("V20观察中性 ");
            }
            if (adaptiveWatchNeutral) {
                sb.append("Adaptive观察中性 ");
            }
            if (sb.length() == 0) {
                return "无中性标记";
            }
            return sb.toString();
        }
    }

    private static final class V20EvidenceView {
        public String stateKey;
        public String state12;
        public String state24;
        public String state48;
        public String state96;
        public String state360;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String evidenceText;
        public String counterText;
        public String actionAdvice;
        public int hardRisk;

        public V20EvidenceView() {
            stateKey = "";
            state12 = "";
            state24 = "";
            state48 = "";
            state96 = "";
            state360 = "";
            evidenceContribution = 0.0d;
            counterContribution = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            featureDistance = 0.0d;
            similarWindowSample = 0;
            similarWindowHitRate = 0.0d;
            evidenceText = "";
            counterText = "";
            actionAdvice = "";
            hardRisk = 0;
        }

        public boolean hasAny() {
            return stateKey.length() > 0
                    || state12.length() > 0
                    || state24.length() > 0
                    || state48.length() > 0
                    || state96.length() > 0
                    || state360.length() > 0
                    || evidenceContribution > 0.0d
                    || counterContribution > 0.0d
                    || sampleReliability > 0.0d
                    || recentValidity > 0.0d
                    || featureDistance > 0.0d
                    || similarWindowSample > 0
                    || similarWindowHitRate > 0.0d
                    || evidenceText.length() > 0
                    || counterText.length() > 0
                    || actionAdvice.length() > 0
                    || hardRisk > 0;
        }
    }

    private static final class TeacherMatrixView {
        public String status;
        public String summary;
        public int sample;
        public int comboSample;
        public double hitRate;
        public double comboHitRate;
        public double confidence;
        public double risk;
        public double score;
        public double probAdjust;
        public double gateAdjust;
        public int applied;

        public TeacherMatrixView() {
            status = "";
            summary = "";
            sample = 0;
            comboSample = 0;
            hitRate = 0.0d;
            comboHitRate = 0.0d;
            confidence = 0.0d;
            risk = 0.0d;
            score = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            applied = 0;
        }

        public boolean hasAny() {
            return status.length() > 0
                    || summary.length() > 0
                    || sample > 0
                    || comboSample > 0
                    || hitRate > 0.0d
                    || comboHitRate > 0.0d
                    || confidence > 0.0d
                    || risk > 0.0d
                    || score > 0.0d
                    || probAdjust != 0.0d
                    || gateAdjust != 0.0d
                    || applied > 0;
        }
    }

    private static final class V20View {
        public String status;
        public String summary;
        public double causeScore;
        public double riskScore;
        public double supportScore;
        public double positionScore;
        public double numberScore;
        public double counterScore;
        public double thirdScore;
        public double ruleHealth;
        public double shadowHealth;
        public double falseRisk;
        public double seedScore;
        public int hardBlock;

        public V20View() {
            status = "";
            summary = "";
            causeScore = 0.0d;
            riskScore = 0.0d;
            supportScore = 0.0d;
            positionScore = 0.0d;
            numberScore = 0.0d;
            counterScore = 0.0d;
            thirdScore = 0.0d;
            ruleHealth = 0.0d;
            shadowHealth = 0.0d;
            falseRisk = 0.0d;
            seedScore = 0.0d;
            hardBlock = 0;
        }

        public boolean hasAny() {
            return status.length() > 0
                    || summary.length() > 0
                    || causeScore > 0.0d
                    || riskScore > 0.0d
                    || supportScore > 0.0d
                    || positionScore > 0.0d
                    || numberScore > 0.0d
                    || counterScore > 0.0d
                    || thirdScore > 0.0d
                    || ruleHealth > 0.0d
                    || shadowHealth > 0.0d
                    || falseRisk > 0.0d
                    || seedScore > 0.0d
                    || hardBlock > 0;
        }
    }
}
