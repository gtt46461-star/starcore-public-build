package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * DiagnosisFixPlanEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环升级 · 第2/14
 *
 * <p>文件性质：新增核心引擎。 作用：诊断翻译器，把 RealBetDiagnosisEngine / ModuleEffectScore / SHADOW晋级 / 冻结矩阵 / 智能审判
 * 的结果，翻译成“到底应该修哪个文件、为什么修、优先级、怎么修、预期效果、风险提醒”。
 *
 * <p>红线： 1. 只读诊断结果，只生成修复计划。 2. 不出号、不结算、不创建Trade、不保存BET、不修改Decision、不改概率、不改门槛。 3. 不把SHADOW写入真实胜率盈亏。
 * 4. 不绕过 DecisionGateEngine。
 *
 * <p>Java7 / AIDE 兼容：无 lambda、无 stream、无第三方库。
 */
public final class DiagnosisFixPlanEngine {

    public static final String VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";

    private static final double TARGET_MIN_RATE = 0.370d;
    private static final double BASE_K3_RATE = 0.300d;
    private static final double HIGH_FINAL_P = 0.390d;
    private static final int MIN_TRUST_SAMPLE = 20;
    private static final int MIN_BUCKET_SAMPLE = 5;
    private static final int MAX_REPORT_LEN = 18000;
    private static final int MAX_SUMMARY_LEN = 900;

    public DiagnosisFixPlanEngine() {}

    public FixPlanReport buildFixPlan(DbStore store) {
        RealBetDiagnosisEngine.DiagnosisReport diagnosis = null;
        try {
            RealBetDiagnosisEngine engine = new RealBetDiagnosisEngine();
            diagnosis = engine.diagnose(store);
        } catch (Throwable e) {
            diagnosis = null;
        }
        FixPlanReport report = buildFixPlan(diagnosis, null, null, null, null);
        applyRuntimeFirstPlan(store, report);
        sortFilePlans(report.filePlans);
        buildText(report, report.snapshot);
        return report;
    }

    private void applyRuntimeFirstPlan(DbStore store, FixPlanReport report) {
        if (report == null) return;
        RuntimeHealthLedgerR36.BusinessTruth truth;
        try {
            truth = store == null ? null : store.businessTruthR811();
        } catch (Throwable error) {
            truth = null;
        }
        String root = truth == null ? "READ_ERROR" : safe(truth.rootCause);
        if (root.length() == 0 || "NONE".equals(root) || "MODEL_SAMPLE".equals(root)) return;

        FileFixPlan p = new FileFixPlan();
        if ("READ_ERROR".equals(root)) {
            p.fileName = "DbStore.java / R811SchemaIntegrityVerifier.java";
            p.priority = 0;
            p.whatToChange = "先恢复可读Schema/Truth快照，保留精确异常；禁止把读取失败折算为0或PASS。";
            p.expectedEffect = "BusinessTruth可读，后续Runtime/Settlement/模型Finding才有合法输入。";
        } else if ("RUNTIME_CHAIN".equals(root)) {
            p.fileName = "AutoFetchService.java / BusinessReconcilerR36.java";
            p.priority = 1;
            p.whatToChange = "恢复唯一生产循环的fetch→settlement→analysis链并验证单飞、目标期和业务心跳。";
            p.expectedEffect = "当前开奖可形成同目标canonical，且不存在第二runtime owner。";
        } else {
            p.fileName = "SettlementEngine.java / OosAfterDrawWriterR36.java";
            p.priority = 2;
            p.whatToChange = "按精确draw target闭合LIVE_CURRENT结算、strict OOS和task terminal。";
            p.expectedEffect = "Settlement/OOS lag归零且失败保留结构化terminal code。";
        }
        p.status = "P0_RUNTIME_FIRST";
        p.problemEvidence =
                truth == null ? "BusinessTruth=READ_ERROR" : truth.canonical();
        p.whyThisFile = "Finding DAG要求DATA/READ→RUNTIME→SETTLEMENT→OOS先于任何模型调参。";
        p.riskIfNotChange = "模型层会替上游故障背锅，形成错误调参和假健康。";
        p.dependency = "验证Owner唯一、CORE/FULL双状态、相同Primary Root Cause。";
        p.sourceTags = "R811_P0_" + root;
        report.filePlans.add(p);
        addEvidence(
                report,
                "R811_P0_RUNTIME_FIRST",
                100,
                p.problemEvidence + "；P0关闭前模型调参只能标SECONDARY_BLOCK。" );

        for (int i = 0; i < report.filePlans.size(); i++) {
            FileFixPlan candidate = report.filePlans.get(i);
            if (candidate == null || candidate == p) continue;
            String file = safe(candidate.fileName).toUpperCase(java.util.Locale.ROOT);
            if (file.indexOf("PROBABILITY") >= 0
                    || file.indexOf("MODEL") >= 0
                    || file.indexOf("TEACHER") >= 0
                    || file.indexOf("DYNAMICGATE") >= 0
                    || file.indexOf("DECISIONGATE") >= 0) {
                candidate.status = "SECONDARY_BLOCK_UNTIL_P0_CLOSED";
                candidate.priority = Math.max(100, candidate.priority);
            }
        }
    }

    public FixPlanReport buildFixPlan(RealBetDiagnosisEngine.DiagnosisReport diagnosisReport) {
        return buildFixPlan(diagnosisReport, null, null, null, null);
    }

    /**
     * 为了第2/14可以独立编译，本文件对后续新增引擎报告全部使用 Object + 反射读取。 后续第3/14、第4/14、第5/14、第6/14 完成后，可以直接把对应 report
     * 传进来。
     */
    public FixPlanReport buildFixPlan(
            RealBetDiagnosisEngine.DiagnosisReport diagnosisReport,
            Object moduleEffectReport,
            Object shadowPromotionReport,
            Object freezeReport,
            Object judgeReport) {
        FixPlanReport report = new FixPlanReport();
        report.version = VERSION;
        report.createdAt = System.currentTimeMillis();
        report.ok = 1;
        DiagnosisSnapshot s =
                buildSnapshot(
                        diagnosisReport,
                        moduleEffectReport,
                        shadowPromotionReport,
                        freezeReport,
                        judgeReport);
        report.snapshot = s;

        buildEvidence(report, s);
        buildFilePlans(report, s);
        buildStrategyGap(report, s);
        sortFilePlans(report.filePlans);
        buildText(report, s);
        return report;
    }

    public FixPlanReport saveFixPlanState(DbStore store) {
        FixPlanReport report = buildFixPlan(store);
        saveFixPlanState(store, report);
        return report;
    }

    public void saveFixPlanState(DbStore store, FixPlanReport report) {
        if (store == null || report == null) {
            return;
        }
        trySetState(store, "diagnosis_fix_plan_version", safe(report.version));
        trySetState(store, "diagnosis_fix_plan_updated_at", String.valueOf(report.createdAt));
        trySetState(store, "diagnosis_fix_plan_status", report.ok == 1 ? "OK" : "ERROR");
        trySetState(store, "diagnosis_fix_plan_summary", safe(report.summary));
        trySetState(store, "diagnosis_fix_plan_detail", safe(report.detail));
        trySetState(store, "diagnosis_fix_plan_overall_problem", safe(report.overallProblem));
        trySetState(store, "diagnosis_fix_plan_priority_list", safe(report.priorityList));
        trySetState(store, "diagnosis_fix_plan_new_engine_needed", safe(report.newEngineNeeded));
        trySetState(store, "diagnosis_fix_plan_strategy_gap", safe(report.strategyGap));
        trySetState(store, "diagnosis_fix_plan_expected_effect", safe(report.expectedEffect));
        trySetState(store, "diagnosis_fix_plan_risk_warning", safe(report.riskWarning));
        trySetState(
                store,
                "diagnosis_fix_plan_file_count",
                String.valueOf(report.filePlans == null ? 0 : report.filePlans.size()));
    }

    public String buildFixPlanSummary(DbStore store) {
        FixPlanReport r = buildFixPlan(store);
        return r == null ? "" : safe(r.summary);
    }

    public String buildFixPlanDetail(DbStore store) {
        FixPlanReport r = buildFixPlan(store);
        return r == null ? "" : safe(r.detail);
    }

    public String buildFixPlanCopyText(DbStore store) {
        FixPlanReport r = buildFixPlan(store);
        if (r == null) {
            return "V20诊断修复计划为空";
        }
        return safe(r.detail);
    }

    public String buildFixPlanCopyText(
            RealBetDiagnosisEngine.DiagnosisReport diagnosisReport,
            Object moduleEffectReport,
            Object shadowPromotionReport,
            Object freezeReport,
            Object judgeReport) {
        FixPlanReport r =
                buildFixPlan(
                        diagnosisReport,
                        moduleEffectReport,
                        shadowPromotionReport,
                        freezeReport,
                        judgeReport);
        if (r == null) {
            return "V20诊断修复计划为空";
        }
        return safe(r.detail);
    }

    private DiagnosisSnapshot buildSnapshot(
            RealBetDiagnosisEngine.DiagnosisReport d,
            Object moduleEffectReport,
            Object shadowPromotionReport,
            Object freezeReport,
            Object judgeReport) {
        DiagnosisSnapshot s = new DiagnosisSnapshot();
        if (d == null) {
            s.hasDiagnosis = 0;
            s.totalBet = 0;
            s.winRate = 0.0d;
            s.mainProblem = "RealBetDiagnosisEngine诊断为空，无法读取真实BET效果";
            return s;
        }

        s.hasDiagnosis = 1;
        s.totalBet = d.totalBet;
        s.hitBet = d.hitBet;
        s.missBet = d.missBet;
        s.waitBet = d.waitBet;
        s.dataErrorBet = d.dataErrorBet;
        s.winRate = d.winRate;
        s.pnl = d.pnl;
        s.avgFinalP = d.avgFinalP;
        s.avgGate = d.avgGate;
        s.avgEdge = d.avgEdge;
        s.negativeEdgeBet = d.negativeEdgeBet;
        s.teacherRiskBet = d.teacherRiskBet;
        s.v20RiskBet = d.v20RiskBet;
        s.shadowTotal = d.shadowTotal;
        s.shadowHit = d.shadowHit;
        s.shadowMiss = d.shadowMiss;
        s.shadowHitRate = d.shadowHitRate;
        s.mainProblem = safe(d.mainProblem);
        s.nextFixAdvice = safe(d.nextFixAdvice);

        s.bestPosition = bucketName(d.bestPosition);
        s.worstPosition = bucketName(d.worstPosition);
        s.bestPositionRate = bucketRate(d.bestPosition);
        s.worstPositionRate = bucketRate(d.worstPosition);
        s.bestPositionTotal = bucketTotal(d.bestPosition);
        s.worstPositionTotal = bucketTotal(d.worstPosition);

        s.bestFinalPBucket = bucketName(d.bestFinalPBucket);
        s.worstFinalPBucket = bucketName(d.worstFinalPBucket);
        s.bestFinalPRate = bucketRate(d.bestFinalPBucket);
        s.worstFinalPRate = bucketRate(d.worstFinalPBucket);
        s.worstFinalPTotal = bucketTotal(d.worstFinalPBucket);

        s.bestTeacherBucket = bucketName(d.bestTeacherBucket);
        s.worstTeacherBucket = bucketName(d.worstTeacherBucket);
        s.bestTeacherRate = bucketRate(d.bestTeacherBucket);
        s.worstTeacherRate = bucketRate(d.worstTeacherBucket);
        s.worstTeacherTotal = bucketTotal(d.worstTeacherBucket);

        s.bestV20Bucket = bucketName(d.bestV20Bucket);
        s.worstV20Bucket = bucketName(d.worstV20Bucket);
        s.bestV20Rate = bucketRate(d.bestV20Bucket);
        s.worstV20Rate = bucketRate(d.worstV20Bucket);
        s.worstV20Total = bucketTotal(d.worstV20Bucket);

        s.topMissBucket = bucketName(d.topMissBucket);
        s.topMissCount = bucketMiss(d.topMissBucket);

        s.betP36Total = bucketTotal(d.betP36);
        s.betP36Rate = bucketRate(d.betP36);
        s.betP38Total = bucketTotal(d.betP38);
        s.betP38Rate = bucketRate(d.betP38);
        s.betP40Total = bucketTotal(d.betP40);
        s.betP40Rate = bucketRate(d.betP40);

        s.shadowP36Total = bucketTotal(d.shadowP36);
        s.shadowP36Rate = bucketRate(d.shadowP36);
        s.shadowP38Total = bucketTotal(d.shadowP38);
        s.shadowP38Rate = bucketRate(d.shadowP38);
        s.shadowP40Total = bucketTotal(d.shadowP40);
        s.shadowP40Rate = bucketRate(d.shadowP40);

        s.v20EmptyTotal = findBucketTotal(d.v20Stats, "空");
        s.v20EmptyRate = findBucketRate(d.v20Stats, "空");
        if (s.v20EmptyTotal <= 0) {
            s.v20EmptyTotal = findBucketTotal(d.v20Stats, "V20空");
            s.v20EmptyRate = findBucketRate(d.v20Stats, "V20空");
        }
        s.teacherWeakTotal = findWeakLikeBucketTotal(d.teacherStats);
        s.teacherWeakRate = findWeakLikeBucketRate(d.teacherStats);
        s.windowEmptyTotal = findBucketTotal(d.windowStats, "空");
        s.windowEmptyRate = findBucketRate(d.windowStats, "空");

        readCaseDiagnosisByReflection(d, s);
        readExternalReports(
                moduleEffectReport, shadowPromotionReport, freezeReport, judgeReport, s);
        inferFlags(s);
        return s;
    }

    private void readCaseDiagnosisByReflection(Object diagnosisReport, DiagnosisSnapshot s) {
        if (diagnosisReport == null || s == null) {
            return;
        }
        Object listObj =
                firstObjectField(
                        diagnosisReport,
                        new String[] {
                            "caseDiagnoses",
                            "caseDiagnosisList",
                            "diagnosisCases",
                            "recentCaseDiagnoses",
                            "realCaseDiagnoses"
                        });
        if (!(listObj instanceof List)) {
            return;
        }
        List list = (List) listObj;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item == null) {
                continue;
            }
            String failureType =
                    firstNonEmpty(
                            getString(item, "failureType", ""),
                            getString(item, "missType", ""),
                            getString(item, "diagnosisType", ""));
            String module =
                    firstNonEmpty(
                            getString(item, "responsibleModule", ""),
                            getString(item, "moduleName", ""),
                            getString(item, "fileName", ""));
            if (failureType.length() > 0) {
                addCount(s.failureTypeCounts, failureType);
            }
            if (module.length() > 0) {
                addCount(s.moduleResponsibilityCounts, module);
            }
            if ("PROBABILITY_OVER_ESTIMATED".equals(failureType)) s.probabilityOverEstimatedCount++;
            if ("TEACHER_FAKE_STRONG".equals(failureType)) s.teacherFakeStrongCount++;
            if ("TEACHER_WEAK_STILL_BET".equals(failureType)) s.teacherWeakStillBetCount++;
            if ("V20_EMPTY_STILL_BET".equals(failureType)) s.v20EmptyStillBetCount++;
            if ("V20_FAKE_SUPPORT".equals(failureType)) s.v20FakeSupportCount++;
            if ("WINDOW_STATE_UNTRUSTED".equals(failureType)) s.windowStateUntrustedCount++;
            if ("POSITION_DRAG".equals(failureType)) s.positionDragCount++;
            if ("K3_COVERAGE_MISS".equals(failureType)) s.k3CoverageMissCount++;
            if ("THIRD_NUMBER_WEAK".equals(failureType)) s.thirdNumberWeakCount++;
            if ("NEGATIVE_EDGE_BET".equals(failureType)) s.negativeEdgeDiagnosisCount++;
            if ("SHADOW_MISSED_OPPORTUNITY".equals(failureType)) s.shadowMissedOpportunityCount++;
            if ("GATE_WRONG_RELEASE".equals(failureType)) s.gateWrongReleaseCount++;
            if ("GATE_WRONG_BLOCK".equals(failureType)) s.gateWrongBlockCount++;
        }
    }

    private void readExternalReports(
            Object moduleEffectReport,
            Object shadowPromotionReport,
            Object freezeReport,
            Object judgeReport,
            DiagnosisSnapshot s) {
        if (s == null) {
            return;
        }
        if (moduleEffectReport != null) {
            s.hasModuleEffectReport = 1;
            s.externalMisleadingModuleCount =
                    firstPositiveInt(
                            getInt(moduleEffectReport, "misleadingModuleCount", 0),
                            getInt(moduleEffectReport, "misleadingCount", 0),
                            getInt(moduleEffectReport, "badModuleCount", 0));
            s.externalEffectiveModuleCount =
                    firstPositiveInt(
                            getInt(moduleEffectReport, "effectiveModuleCount", 0),
                            getInt(moduleEffectReport, "effectiveCount", 0),
                            getInt(moduleEffectReport, "goodModuleCount", 0));
            s.externalModuleSummary =
                    firstNonEmpty(
                            getString(moduleEffectReport, "summary", ""),
                            getString(moduleEffectReport, "detail", ""),
                            getString(moduleEffectReport, "moduleSummary", ""));
        }
        if (shadowPromotionReport != null) {
            s.hasShadowPromotionReport = 1;
            s.externalPromotedDomainCount =
                    firstPositiveInt(
                            getInt(shadowPromotionReport, "promotedDomainCount", 0),
                            getInt(shadowPromotionReport, "promoteCount", 0),
                            getInt(shadowPromotionReport, "watchDomainCount", 0));
            s.externalShadowSummary =
                    firstNonEmpty(
                            getString(shadowPromotionReport, "summary", ""),
                            getString(shadowPromotionReport, "promotionSummary", ""),
                            getString(shadowPromotionReport, "detail", ""));
        }
        if (freezeReport != null) {
            s.hasFreezeReport = 1;
            s.externalFrozenCount =
                    firstPositiveInt(
                            getInt(freezeReport, "frozenCount", 0),
                            getInt(freezeReport, "freezeCount", 0),
                            getInt(freezeReport, "activeFreezeCount", 0));
            s.externalFreezeSummary =
                    firstNonEmpty(
                            getString(freezeReport, "summary", ""),
                            getString(freezeReport, "freezeSummary", ""),
                            getString(freezeReport, "detail", ""));
        }
        if (judgeReport != null) {
            s.hasJudgeReport = 1;
            s.externalDowngradeCount =
                    firstPositiveInt(
                            getInt(judgeReport, "downgradeCount", 0),
                            getInt(judgeReport, "downgradeToShadowCount", 0),
                            getInt(judgeReport, "watchCount", 0));
            s.externalJudgeSummary =
                    firstNonEmpty(
                            getString(judgeReport, "summary", ""),
                            getString(judgeReport, "judgeSummary", ""),
                            getString(judgeReport, "detail", ""));
        }
    }

    private void inferFlags(DiagnosisSnapshot s) {
        if (s == null) {
            return;
        }
        if (s.hasDiagnosis == 0) {
            s.needDiagnosisStorage = 1;
            return;
        }
        if (s.totalBet < MIN_TRUST_SAMPLE) {
            s.sampleInsufficient = 1;
        }
        if (s.totalBet >= 10 && s.winRate < BASE_K3_RATE) {
            s.realBetBelowBase = 1;
        }
        if (s.totalBet >= MIN_TRUST_SAMPLE && s.winRate < TARGET_MIN_RATE) {
            s.realBetBelowTarget = 1;
        }
        if (s.avgFinalP >= HIGH_FINAL_P && s.totalBet >= 10 && s.winRate + 0.030d < s.avgFinalP) {
            s.finalPOverEstimated = 1;
        }
        if (s.worstFinalPTotal >= MIN_BUCKET_SAMPLE && s.worstFinalPRate < BASE_K3_RATE) {
            s.badFinalPBucket = 1;
        }
        if (s.negativeEdgeBet > 0
                && s.totalBet > 0
                && ((double) s.negativeEdgeBet / (double) s.totalBet) >= 0.080d) {
            s.negativeEdgeRelease = 1;
        }
        if (s.teacherRiskBet > 0) {
            s.teacherRiskRelease = 1;
        }
        if (s.teacherWeakTotal >= MIN_BUCKET_SAMPLE && s.teacherWeakRate < BASE_K3_RATE) {
            s.teacherWeakBad = 1;
        }
        if (s.worstTeacherTotal >= MIN_BUCKET_SAMPLE && s.worstTeacherRate < BASE_K3_RATE) {
            s.teacherFakeStrong = 1;
        }
        if (s.v20RiskBet > 0) {
            s.v20RiskRelease = 1;
        }
        if (s.v20EmptyTotal >= MIN_BUCKET_SAMPLE) {
            s.v20EmptyStillBet = 1;
        }
        if (s.worstV20Total >= MIN_BUCKET_SAMPLE && s.worstV20Rate < BASE_K3_RATE) {
            s.v20FakeSupport = 1;
        }
        if (s.windowEmptyTotal >= MIN_BUCKET_SAMPLE
                || s.windowStateUntrustedCount >= MIN_BUCKET_SAMPLE) {
            s.windowStateUntrusted = 1;
        }
        if (s.worstPositionTotal >= MIN_BUCKET_SAMPLE && s.worstPositionRate < BASE_K3_RATE) {
            s.positionDrag = 1;
        }
        if (s.topMissCount >= MIN_BUCKET_SAMPLE) {
            s.missReasonConcentrated = 1;
        }
        if (s.shadowTotal >= MIN_TRUST_SAMPLE
                && s.totalBet >= 10
                && s.shadowHitRate > s.winRate + 0.060d) {
            s.shadowBetterThanBet = 1;
        }
        if (s.betP40Total >= MIN_BUCKET_SAMPLE && s.betP40Rate > s.winRate + 0.060d) {
            s.highPDomainEffective = 1;
        }
        if (s.hasModuleEffectReport == 0) {
            s.needModuleEffectEngine = 1;
        }
        if (s.hasShadowPromotionReport == 0) {
            s.needShadowPromotionEngine = 1;
        }
        if (s.hasFreezeReport == 0) {
            s.needFreezeEngine = 1;
        }
        if (s.hasJudgeReport == 0) {
            s.needAdaptiveJudgeEngine = 1;
        }
        if (s.finalPOverEstimated == 1
                || s.teacherFakeStrong == 1
                || s.v20FakeSupport == 1
                || s.negativeEdgeRelease == 1
                || s.shadowBetterThanBet == 1) {
            s.needRealtimeJudge = 1;
        }
    }

    private void buildEvidence(FixPlanReport report, DiagnosisSnapshot s) {
        if (report == null || s == null) {
            return;
        }
        if (s.hasDiagnosis == 0) {
            addEvidence(report, "DIAGNOSIS_EMPTY", 95, "RealBetDiagnosisEngine诊断为空，无法把真实效果翻译成修复计划");
            return;
        }
        addEvidence(
                report,
                "REAL_BET_OVERVIEW",
                40,
                "真实BET="
                        + s.totalBet
                        + "，中="
                        + s.hitBet
                        + "，挂="
                        + s.missBet
                        + "，胜率="
                        + pct(s.winRate)
                        + "，盈亏="
                        + money(s.pnl));
        if (s.sampleInsufficient == 1) {
            addEvidence(
                    report,
                    "SAMPLE_INSUFFICIENT",
                    55,
                    "真实BET样本少于" + MIN_TRUST_SAMPLE + "，不能用短样本强行下绝对结论，但可以先看错因方向");
        }
        if (s.realBetBelowBase == 1) {
            addEvidence(report, "REAL_BET_BELOW_BASE", 95, "真实BET胜率低于K3基础线30%，说明当前BET放行质量明显不足");
        } else if (s.realBetBelowTarget == 1) {
            addEvidence(report, "REAL_BET_BELOW_TARGET", 85, "真实BET胜率低于目标线37%，需要收纯强域、冻结假强域");
        }
        if (s.finalPOverEstimated == 1) {
            addEvidence(
                    report,
                    "PROBABILITY_OVER_ESTIMATED",
                    90,
                    "平均FinalP=" + pct(s.avgFinalP) + "，真实胜率=" + pct(s.winRate) + "，FinalP疑似虚高");
        }
        if (s.badFinalPBucket == 1) {
            addEvidence(
                    report,
                    "BAD_FINALP_BUCKET",
                    78,
                    "最差FinalP分桶="
                            + s.worstFinalPBucket
                            + "，样本="
                            + s.worstFinalPTotal
                            + "，胜率="
                            + pct(s.worstFinalPRate));
        }
        if (s.negativeEdgeRelease == 1) {
            addEvidence(
                    report,
                    "NEGATIVE_EDGE_BET",
                    92,
                    "负Edge真实出手=" + s.negativeEdgeBet + "，说明最终门/动态门可能放行了门槛下方候选");
        }
        if (s.teacherRiskRelease == 1) {
            addEvidence(
                    report,
                    "TEACHER_RISK_RELEASE",
                    88,
                    "Teacher风险状态仍进入真实BET=" + s.teacherRiskBet + "单");
        }
        if (s.teacherWeakBad == 1 || s.teacherFakeStrong == 1) {
            addEvidence(
                    report,
                    "TEACHER_FAKE_OR_WEAK",
                    86,
                    "Teacher弱/差分桶样本="
                            + Math.max(s.teacherWeakTotal, s.worstTeacherTotal)
                            + "，胜率="
                            + pct(
                                    Math.min(
                                            nonZeroOrOne(s.teacherWeakRate),
                                            nonZeroOrOne(s.worstTeacherRate)))
                            + "，教师分级需要重做近期有效性");
        }
        if (s.v20EmptyStillBet == 1) {
            addEvidence(
                    report,
                    "V20_EMPTY_STILL_BET",
                    94,
                    "V20因链为空仍进入真实BET=" + s.v20EmptyTotal + "单，说明V20字段没有完整进入候选/Decision/Trade诊断链路");
        }
        if (s.v20FakeSupport == 1 || s.v20RiskRelease == 1) {
            addEvidence(
                    report,
                    "V20_FAKE_SUPPORT_OR_RISK",
                    84,
                    "V20最差分桶="
                            + s.worstV20Bucket
                            + "，样本="
                            + s.worstV20Total
                            + "，胜率="
                            + pct(s.worstV20Rate)
                            + "，V20支撑可能只是解释强，不是预测强");
        }
        if (s.windowStateUntrusted == 1) {
            addEvidence(
                    report,
                    "WINDOW_STATE_UNTRUSTED",
                    75,
                    "WindowState为空/低可信样本较多，需要把相似窗口样本数、近期有效性和featureDistance落库");
        }
        if (s.positionDrag == 1) {
            addEvidence(
                    report,
                    "POSITION_DRAG",
                    82,
                    "拖后腿位置="
                            + s.worstPosition
                            + "，样本="
                            + s.worstPositionTotal
                            + "，胜率="
                            + pct(s.worstPositionRate));
        }
        if (s.missReasonConcentrated == 1) {
            addEvidence(
                    report,
                    "MISS_REASON_CONCENTRATED",
                    72,
                    "主要错因集中在=" + s.topMissBucket + "，次数=" + s.topMissCount);
        }
        if (s.shadowBetterThanBet == 1) {
            addEvidence(
                    report,
                    "SHADOW_MISSED_OPPORTUNITY",
                    90,
                    "SHADOW反事实胜率="
                            + pct(s.shadowHitRate)
                            + "，高于真实BET="
                            + pct(s.winRate)
                            + "，需要反事实晋级验证");
        }
        if (s.highPDomainEffective == 1) {
            addEvidence(
                    report,
                    "HIGH_P_DOMAIN_EFFECTIVE",
                    68,
                    "FinalP>=40%样本胜率=" + pct(s.betP40Rate) + "，高于整体，说明强P域可保留，弱域要降级");
        }
        if (s.needModuleEffectEngine == 1) {
            addEvidence(report, "MODULE_EFFECT_MISSING", 80, "缺少独立模块效果评分，无法判断哪个模块真有效、哪个模块误导");
        }
        if (s.needFreezeEngine == 1) {
            addEvidence(report, "FREEZE_MATRIX_MISSING", 78, "缺少动态冻结矩阵，假强状态容易连续误导下一期BET");
        }
        if (s.needAdaptiveJudgeEngine == 1) {
            addEvidence(
                    report,
                    "ADAPTIVE_JUDGE_MISSING",
                    82,
                    "缺少实时智能审判，诊断结果还不能在出手前参与PASS/WATCH/FREEZE/DOWNGRADE");
        }
    }

    private void buildFilePlans(FixPlanReport report, DiagnosisSnapshot s) {
        if (report == null || s == null) {
            return;
        }
        addDecisionFunnelPlan(report, s);
        addLanePolicyPlan(report, s);
        addAuthorizationPlan(report, s);
        addRealBetDiagnosisPlan(report, s);
        addModuleEffectPlan(report, s);
        addShadowPromotionPlan(report, s);
        addDynamicFreezePlan(report, s);
        addAdaptiveJudgePlan(report, s);
        addDbStorePlan(report, s);
        addMainActivityPlan(report, s);
        addAutoFetchPlan(report, s);
        addDynamicCausePlan(report, s);
        addTeacherPlan(report, s);
        addProbabilityPlan(report, s);
        addDynamicGatePlan(report, s);
        addDecisionGatePlan(report, s);
    }

    private void addDecisionFunnelPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DecisionFunnelDiagnosisEngine.java";
        p.priority = 5;
        p.status = "V46_REPLACED_BY_FINAL40_DIAGNOSIS";
        p.problemEvidence = "当前只显示结果汇总，缺少逐条件拒绝率、滚动胜率、校准误差、最大连挂和频率可达性。";
        p.whyThisFile = "先知道候选在哪一层被挡、哪个概率段真实失效，才能修出手而不是盲目降门槛。";
        p.whatToChange =
                "读取不可变OOS"
                    + " Top-K日志，按目标期去重，输出V46最终40合同的真实漏斗、Wilson下界、Brier、滚动30/60/120/240、概率桶、TopK排序和拒绝码。";
        p.expectedEffect = "完整回答为什么不出手、为什么胜率低、为什么波动以及15%-20%频率是否由合格机会支持。";
        p.riskIfNotChange = "只能看总胜率，继续误把样本不足、概率失校准和门控过严混为一谈。";
        p.dependency = "只读DbHelper.TABLE_OOS_ACTION_LOG，不改BET。";
        p.sourceTags = "ZERO_BET_FULL_FUNNEL";
        report.filePlans.add(p);
    }

    private void addLanePolicyPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "LanePolicyCompiler.java";
        p.priority = 8;
        p.status = "V46_REPLACED_BY_SINGLE_LANE_QUORUM";
        p.problemEvidence = "NORMAL/FLOOR/PROBE把多项高度相关的成熟度和质量条件全部AND，任一冷启动字段即可把所有候选变SHADOW。";
        p.whyThisFile = "它才是唯一NORMAL/FLOOR/PROBE合同所有者，RealBetDiagnosisEngine不能出手。";
        p.whatToChange =
                "保留概率、覆盖、第三号、硬风险和预算硬底线；将family/structure/regime/association/maturity等相关证据改成质量法定人数，受控FLOOR/PROBE要求FinalP>=35%、ModelP>=34.5%、Edge>=-1%。";
        p.expectedEffect = "解除结构性零出手死锁，但不会用低P或硬风险候选硬补频率。";
        p.riskIfNotChange = "影子样本继续增长，真实BET仍可能长期为0。";
        p.dependency = "DecisionAuthorizationEngine必须复用同一编译结果。";
        p.sourceTags = "OVER_CONJUNCTIVE_LANE";
        report.filePlans.add(p);
    }

    private void addAuthorizationPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DecisionAuthorizationEngine.java";
        p.priority = 9;
        p.status = "V46_REPLACED_BY_EVENTP_ACTIONLCB_CONTRACT";
        p.problemEvidence = "旧授权对所有Lane统一要求Edge>=0，使受控FLOOR/PROBE的-1%边缘合同在最终签名前再次被拒绝。";
        p.whyThisFile = "它是唯一签名器，任何Lane通过后仍必须在这里获得真实BET授权。";
        p.whatToChange = "NORMAL继续Edge>=0；仅受控FLOOR/PROBE允许Edge>=-1%，并继续复核K3、版本、概率锁、硬风险和预算。";
        p.expectedEffect = "消除Lane合同与签名合同矛盾。";
        p.riskIfNotChange = "Lane显示就绪但授权仍归零。";
        p.dependency = "严格复用LanePolicyCompiler，不另造门槛。";
        p.sourceTags = "AUTH_EDGE_MISMATCH";
        report.filePlans.add(p);
    }

    private void addRealBetDiagnosisPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "RealBetDiagnosisEngine.java";
        p.priority = 20;
        p.status = "V46_REPLACED_BY_ROOT_CAUSE_REPORT";
        p.problemEvidence = "第1/14负责逐单诊断、失败类型、模块责任、SHADOW反事实前置数据。";
        p.whyThisFile = "没有真实BET逐单复盘，后续文件不知道该冻结谁、晋级谁、降级谁。";
        p.whatToChange =
                "保持只读诊断；输出CaseDiagnosis、ShadowCaseDiagnosis、ModuleDiagnosis；不创建Trade、不改Decision、不写真实盈亏。";
        p.expectedEffect = "为第2/14修复计划、第3/14模块评分、第5/14冻结矩阵、第6/14智能审判提供真实样本依据。";
        p.riskIfNotChange = "如果只有总胜率显示，没有逐单错因，后续只能凭感觉调参数。";
        p.dependency = "已完成后，DiagnosisFixPlanEngine读取其DiagnosisReport。";
        p.sourceTags = "DIAGNOSIS_CASE_LOG";
        report.filePlans.add(p);
    }

    private void addModuleEffectPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "ModuleEffectScoreEngine.java";
        p.priority = s.needModuleEffectEngine == 1 ? 20 : 65;
        p.status = "EXISTS_NEEDS_REAL_SAMPLE";
        p.problemEvidence =
                evidenceText(
                        report,
                        "MODULE_EFFECT_MISSING",
                        "当前还没有独立模块效果评分；Teacher/V20/Window/Probability/Gate到底谁有效无法量化。");
        p.whyThisFile = "十四文件方案要求每个模块都能评分，判断EFFECTIVE/NEUTRAL/MISLEADING/INSUFFICIENT/CONFLICT。";
        p.whatToChange =
                "新增模块效果评分：统计Teacher支持、V20支持、WindowState支持、Probability高P、DynamicGate低门槛、DecisionGate放行/拦截后的真实命中、盈亏、近期有效性和SHADOW反事实。";
        p.expectedEffect =
                "给ProbabilityEngine动态折扣、DynamicFreezeMatrix冻结、AdaptiveLearningJudge实时审判提供模块可信度。";
        p.riskIfNotChange = "模块只会写原因，不知道真实支持后到底赚不赚钱，假强会继续误导。";
        p.dependency = "依赖RealBetDiagnosisEngine输出的逐单诊断和BET/SHADOW样本。";
        p.sourceTags = "MODULE_EFFECT_MISSING";
        report.filePlans.add(p);
    }

    private void addShadowPromotionPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "ShadowPromotionVerifierEngine.java";
        p.priority = s.shadowBetterThanBet == 1 ? 25 : 70;
        p.status = "EXISTS_NEEDS_REAL_SAMPLE";
        p.problemEvidence =
                evidenceText(
                        report,
                        "SHADOW_MISSED_OPPORTUNITY",
                        "需要把SHADOW从只观察升级为反事实晋级池，避免错杀强域。SHADOW样本="
                                + s.shadowTotal
                                + "，反事实胜率="
                                + pct(s.shadowHitRate));
        p.whyThisFile = "频率不设硬上限，新增频率必须来自被SHADOW真实验证过的强域，而不是低频救援BET。";
        p.whatToChange =
                "新增SHADOW反事实域统计：位置、FinalP分桶、Teacher/V20/WindowState组合、edge分桶、连续错杀域，输出PROMOTE_WATCH/WATCH/BLOCK。";
        p.expectedEffect = "发现可晋级强域后，只给AdaptiveLearningJudge和DecisionGate加分依据，不直接转BET。";
        p.riskIfNotChange = "系统可能继续错杀强SHADOW，同时放行弱BET，频率和胜率都会错位。";
        p.dependency = "依赖RealBetDiagnosisEngine的ShadowCaseDiagnosis和DbStore的SHADOW记录。";
        p.sourceTags = "SHADOW_MISSED_OPPORTUNITY";
        report.filePlans.add(p);
    }

    private void addDynamicFreezePlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DynamicFreezeMatrixEngine.java";
        p.priority =
                (s.teacherFakeStrong == 1
                                || s.v20FakeSupport == 1
                                || s.positionDrag == 1
                                || s.badFinalPBucket == 1)
                        ? 30
                        : 75;
        p.status = "EXISTS_NEEDS_REAL_SAMPLE";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {
                            "TEACHER_FAKE_OR_WEAK",
                            "V20_FAKE_SUPPORT_OR_RISK",
                            "POSITION_DRAG",
                            "BAD_FINALP_BUCKET",
                            "MISS_REASON_CONCENTRATED"
                        },
                        "需要冻结假强状态，防止同类低效位置/状态/概率段连续进入真实BET。");
        p.whyThisFile = "诊断只能发现问题，冻结矩阵负责把假强状态变成可执行的DOWNGRADE/FREEZE。";
        p.whatToChange =
                "新增冻结对象：position、windowStateKey、teacherStatus、v20Status、finalPBucket、edgeBucket、missReason、moduleTag、k3RankPattern；冻结后只允许SHADOW观察，达标后解冻。";
        p.expectedEffect = "减少连续误导和连挂，把RISK/FREEZE域挡在真实BET之外。";
        p.riskIfNotChange = "假强状态下一期还会继续被概率或教师抬高，导致重复MISS。";
        p.dependency =
                "依赖RealBetDiagnosisEngine错因、ModuleEffectScoreEngine模块状态、ShadowPromotionVerifierEngine恢复验证。";
        p.sourceTags = "FREEZE_MATRIX_MISSING";
        report.filePlans.add(p);
    }

    private void addAdaptiveJudgePlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "AdaptiveLearningJudgeEngine.java";
        p.priority = s.needRealtimeJudge == 1 ? 35 : 80;
        p.status = "EXISTS_NEEDS_REAL_SAMPLE";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {
                            "PROBABILITY_OVER_ESTIMATED",
                            "NEGATIVE_EDGE_BET",
                            "TEACHER_FAKE_OR_WEAK",
                            "V20_EMPTY_STILL_BET",
                            "SHADOW_MISSED_OPPORTUNITY",
                            "ADAPTIVE_JUDGE_MISSING"
                        },
                        "需要实时审判，把诊断结果转成PASS/WATCH/DOWNGRADE/FREEZE。");
        p.whyThisFile = "后台学习如果不参与出手前审判，只是记录员，不是智能学习机器人。";
        p.whatToChange =
                "新增JudgeResult：PASS、WATCH、DOWNGRADE_TO_SHADOW、FREEZE、REJECT_FAKE_STRONG、PROMOTE_SHADOW_CANDIDATE；读取FinalP/Gate/Edge、Teacher、V20、WindowState、模块评分、SHADOW晋级、冻结状态。";
        p.expectedEffect = "在DecisionGate之前给出风险和降级建议，但不直接BET，仍由DecisionGate唯一裁决。";
        p.riskIfNotChange = "诊断发现的问题不会影响下一期，系统仍可能高P弱证据照样BET。";
        p.dependency =
                "依赖ModuleEffectScoreEngine、ShadowPromotionVerifierEngine、DynamicFreezeMatrixEngine。";
        p.sourceTags = "ADAPTIVE_JUDGE_MISSING";
        report.filePlans.add(p);
    }

    private void addDbStorePlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DbStore.java";
        p.priority = 45;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence =
                "十四文件闭环需要保存diagnosis_case_log、module_effect_cache、shadow_promotion_cache、dynamic_freeze_matrix、adaptive_judge_log、diagnosis_fix_plan。";
        p.whyThisFile = "诊断闭环必须跨期生效，不能只停留在内存和UI文本。";
        p.whatToChange =
                "新增表和读写接口：get/saveRecentDiagnosisCases、ModuleEffectCache、ShadowPromotionCache、DynamicFreezeState、AdaptiveJudgeLog、DiagnosisFixPlan。";
        p.expectedEffect = "让下一期AdaptiveLearningJudge和DecisionGate能读取上一期真实诊断结果。";
        p.riskIfNotChange = "软件重启或下一期分析时拿不到历史错因，冻结和晋级无法持续。";
        p.dependency = "第2-6引擎确定字段后再集中落库。";
        p.sourceTags = "DIAGNOSIS_STORAGE";
        report.filePlans.add(p);
    }

    private void addMainActivityPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "MainActivity.java";
        p.priority = 55;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence = "需要在UI显示满级诊断总览、逐单复盘、模块归因、冻结矩阵、SHADOW晋级、智能审判、文件修复建议，并支持长按复制。";
        p.whyThisFile = "用户需要直接复制诊断报告给后续修复，不应该导数据库。";
        p.whatToChange = "新增诊断区域和复制入口；读取DbStore状态或引擎实时生成的summary/detail。";
        p.expectedEffect = "让每次错因、责任模块、下一个应修文件一眼能看懂。";
        p.riskIfNotChange = "后台有诊断但用户看不到，无法继续精准施工。";
        p.dependency = "依赖DbStore保存报告和各引擎buildCopyText。";
        p.sourceTags = "UI_COPY_REPORT";
        report.filePlans.add(p);
    }

    private void addAutoFetchPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "AutoFetchService.java";
        p.priority = 60;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence = "诊断闭环需要在服务启动、新期开奖、结算完成、Decision保存、SHADOW保存、空闲时低优先级刷新。";
        p.whyThisFile = "AutoFetchService是调度层，必须让诊断/评分/冻结/修复计划自动更新，但不能影响出号和结算。";
        p.whatToChange =
                "新增postFullDiagnosisRefresh、postModuleEffectRefresh、postShadowPromotionRefresh、postFreezeMatrixRefresh、postAdaptiveJudgeRefresh、postFixPlanRefresh；全部让路实时出号和结算。";
        p.expectedEffect = "结算后自动复盘，下一期可读取最新审判依据。";
        p.riskIfNotChange = "诊断只能手动刷新，实时审判拿到的可能是旧状态。";
        p.dependency = "依赖DbStore和第2-6引擎。";
        p.sourceTags = "BACKGROUND_REFRESH";
        report.filePlans.add(p);
    }

    private void addDynamicCausePlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DynamicCauseChainEngine.java";
        p.priority = (s.v20EmptyStillBet == 1 || s.v20FakeSupport == 1) ? 15 : 85;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {
                            "V20_EMPTY_STILL_BET",
                            "V20_FAKE_SUPPORT_OR_RISK",
                            "WINDOW_STATE_UNTRUSTED"
                        },
                        "需要把V20因链从后台摘要变成真实候选字段。");
        p.whyThisFile = "如果V20字段为空或只写解释，不进入Decision/Trade，诊断和审判都无法判断V20强弱。";
        p.whatToChange =
                "每个候选写入v20Status、v20SupportScore、v20RiskScore、causeScore、positionCauseScore、numberCauseScore、counterfactualScore、thirdGuardScore、ruleHealthScore、shadowHealthScore、falseStrongRisk、seedExpansionScore、evidenceContribution、counterContribution、sampleReliability、recentValidity、actionAdvice，并复制到Decision/Trade/诊断日志。";
        p.expectedEffect = "V20因链可被Probability折扣、DynamicGate质量门、AdaptiveJudge审判、DecisionGate最终门读取。";
        p.riskIfNotChange = "V20继续为空，真实BET只靠概率和旧Teacher，因链无法控制出手质量。";
        p.dependency = "第10/14修改，依赖DbStore字段扩展。";
        p.sourceTags = "V20_EMPTY_STILL_BET";
        report.filePlans.add(p);
    }

    private void addTeacherPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "TeacherNumberMatrixEngine.java";
        p.priority =
                (s.teacherWeakBad == 1 || s.teacherFakeStrong == 1 || s.teacherRiskRelease == 1)
                        ? 18
                        : 86;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {"TEACHER_RISK_RELEASE", "TEACHER_FAKE_OR_WEAK"},
                        "需要重做Teacher STRONG/READY/WEAK/RISK/FREEZE分级。");
        p.whyThisFile = "Teacher矩阵如果弱仍BET、假强仍加权，会直接拉低真实胜率。";
        p.whatToChange =
                "综合总样本、近期样本、总命中率、近期命中率、真实BET前推、SHADOW反事实、位置表现、状态一致性、样本距离、连续误导次数；输出必须落到ComboCandidate、Decision、Trade、diagnosis"
                    + " log。";
        p.expectedEffect = "Teacher从后台标签变成可审判的强弱分层，RISK/FREEZE不再支持真实BET。";
        p.riskIfNotChange = "教师偏弱仍会被当成支撑，导致FinalP虚高和连续MISS。";
        p.dependency = "第11/14修改，依赖ModuleEffectScore和DynamicFreeze反馈。";
        p.sourceTags = "TEACHER_FAKE_OR_WEAK";
        report.filePlans.add(p);
    }

    private void addProbabilityPlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "ProbabilityEngine.java";
        p.priority = (s.finalPOverEstimated == 1 || s.badFinalPBucket == 1) ? 22 : 88;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {
                            "PROBABILITY_OVER_ESTIMATED",
                            "BAD_FINALP_BUCKET",
                            "HIGH_P_DOMAIN_EFFECTIVE"
                        },
                        "需要把FinalP从固定融合升级为诊断折扣后的概率。");
        p.whyThisFile = "平均FinalP高于真实胜率时，不能全局降概率，要按Teacher/V20/WindowState/模块效果/冻结状态动态折扣。";
        p.whatToChange =
                "新增diagnosisAdjustedP、moduleAdjustedP、freezeAdjustedP、shadowPromotionBoost、finalPBeforeDiagnosis、finalPAfterDiagnosis、probabilityDiagnosisSummary；Teacher弱、V20空、反证高、低样本、recentValidity低、moduleEffect"
                    + " MISLEADING时折扣。";
        p.expectedEffect = "减少FinalP虚高，让FinalP更接近真实前推效果。";
        p.riskIfNotChange = "FinalP看起来40%但实际只有34%左右，DecisionGate会被虚高概率误导。";
        p.dependency = "第12/14修改，依赖Teacher/V20/ModuleEffect/Freeze/ShadowPromotion字段。";
        p.sourceTags = "PROBABILITY_OVER_ESTIMATED";
        report.filePlans.add(p);
    }

    private void addDynamicGatePlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DynamicGateEngine.java";
        p.priority =
                (s.negativeEdgeRelease == 1 || s.badFinalPBucket == 1 || s.shadowBetterThanBet == 1)
                        ? 32
                        : 89;
        p.status = "EXISTING_FILE_TO_MODIFY";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {
                            "NEGATIVE_EDGE_BET", "BAD_FINALP_BUCKET", "SHADOW_MISSED_OPPORTUNITY"
                        },
                        "需要把动态门从频率软控升级为质量风险门槛。");
        p.whyThisFile = "频率不设硬上限，但门槛必须按质量风险变化；频率低不能救援弱证据，频率高但质量好不硬压。";
        p.whatToChange =
                "门槛读取evidenceContribution、counterContribution、sampleReliability、recentValidity、moduleEffect、freezeState、shadowPromotion、teacherStatus、v20Status；同类假强聚集抬该类门槛，不做全局频率硬压。";
        p.expectedEffect = "让强域自然出手，假强域局部收紧，避免低频救援和高频误杀。";
        p.riskIfNotChange = "门槛仍可能被频率或旧逻辑牵着走，不知道当前候选质量是否可信。";
        p.dependency = "第13/14修改，依赖前面所有诊断字段。";
        p.sourceTags = "QUALITY_GATE";
        report.filePlans.add(p);
    }

    private void addDecisionGatePlan(FixPlanReport report, DiagnosisSnapshot s) {
        FileFixPlan p = new FileFixPlan();
        p.fileName = "DecisionGateEngine.java";
        p.priority =
                (s.negativeEdgeRelease == 1
                                || s.teacherRiskRelease == 1
                                || s.v20EmptyStillBet == 1
                                || s.needRealtimeJudge == 1)
                        ? 40
                        : 90;
        p.status = "EXISTING_FILE_TO_MODIFY_LAST";
        p.problemEvidence =
                joinEvidence(
                        report,
                        new String[] {
                            "NEGATIVE_EDGE_BET",
                            "TEACHER_RISK_RELEASE",
                            "V20_EMPTY_STILL_BET",
                            "ADAPTIVE_JUDGE_MISSING"
                        },
                        "最终门必须最后接入智能审判和冻结矩阵。");
        p.whyThisFile = "DecisionGateEngine是唯一真实BET出口，只能最后改，不能提前让新增引擎绕过它。";
        p.whatToChange =
                "接入AdaptiveLearningJudgeResult、FreezeDecision、ModuleEffectReport、ShadowPromotionSignal、DiagnosisFixHint；FREEZE→HOLD/SHADOW，DOWNGRADE→SHADOW，WATCH除非极强否则SHADOW，PASS继续原DecisionGate；Teacher"
                    + " RISK/FREEZE不BET，V20空且普通高P不BET，ModuleEffect误导不BET。";
        p.expectedEffect = "只放行经过诊断审判的高质量候选，BET/SHADOW/HOLD边界清楚。";
        p.riskIfNotChange = "前面所有诊断、评分、冻结、晋级都不会真正影响真实出手。";
        p.dependency = "第14/14最后修改，依赖第1-13全部完成。";
        p.sourceTags = "DECISION_GATE_LAST";
        report.filePlans.add(p);
    }

    private void buildStrategyGap(FixPlanReport report, DiagnosisSnapshot s) {
        if (report == null || s == null) {
            return;
        }
        String gap = "";
        String need = "";
        String effect = "";
        String risk = "";

        if (s.hasDiagnosis == 0) {
            gap = append(gap, "真实BET诊断为空，无法闭环");
        }
        if (s.needModuleEffectEngine == 1) {
            gap = append(gap, "缺少模块效果评分，无法判断模块真强/假强");
            need = append(need, "ModuleEffectScoreEngine.java");
        }
        if (s.needShadowPromotionEngine == 1) {
            gap = append(gap, "缺少SHADOW反事实晋级池，频率不能自然从强域提高");
            need = append(need, "ShadowPromotionVerifierEngine.java");
        }
        if (s.needFreezeEngine == 1) {
            gap = append(gap, "缺少动态冻结矩阵，假强状态不能自动降级");
            need = append(need, "DynamicFreezeMatrixEngine.java");
        }
        if (s.needAdaptiveJudgeEngine == 1) {
            gap = append(gap, "缺少实时审判机器人，诊断不能反哺下一期出手前判断");
            need = append(need, "AdaptiveLearningJudgeEngine.java");
        }
        if (s.v20EmptyStillBet == 1) {
            gap = append(gap, "V20因链字段没有完整进入真实BET链路");
        }
        if (s.finalPOverEstimated == 1) {
            gap = append(gap, "FinalP存在虚高，需要诊断折扣");
        }
        if (s.shadowBetterThanBet == 1) {
            gap = append(gap, "SHADOW强域可能被错杀，需要晋级验证");
        }
        if (gap.length() == 0) {
            gap = "当前未发现单一巨大缺口，但十四文件闭环仍需按顺序补齐：诊断→评分→晋级→冻结→审判→最终门。";
        }
        if (need.length() == 0) {
            need = "按十四文件顺序继续补齐新增引擎和现有文件接入。";
        }

        effect =
                "预期不是写死胜率，而是让真实有效强域越来越纯：弱Teacher/V20空/负Edge/低样本状态降级，SHADOW验证强域晋级，假强状态冻结，最后由DecisionGate唯一放行BET。";
        risk = "禁止用修改结算制造胜率；禁止SHADOW污染真实盈亏；禁止AutoFetchService或新增引擎直接保存BET；禁止频率硬上限和低频救援替代证据质量。";

        report.strategyGap = gap;
        report.newEngineNeeded = need;
        report.expectedEffect = effect;
        report.riskWarning = risk;
    }

    private void buildText(FixPlanReport report, DiagnosisSnapshot s) {
        if (report == null || s == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【V20诊断修复计划】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("定位=第2/14 新增诊断翻译器，只生成修复建议，不影响BET/结算/概率/门槛。\n");
        sb.append(
                "红线=DecisionGateEngine仍是唯一真实BET出口；本文件不创建Trade、不改Decision、不写真实盈亏、不把SHADOW转BET。\n\n");

        sb.append("一、读取到的真实诊断\n");
        if (s.hasDiagnosis == 0) {
            sb.append("RealBetDiagnosisEngine诊断为空，当前只能输出结构性施工建议。\n\n");
        } else {
            sb.append("真实BET：出手=")
                    .append(s.totalBet)
                    .append("，中=")
                    .append(s.hitBet)
                    .append("，挂=")
                    .append(s.missBet)
                    .append("，胜率=")
                    .append(pct(s.winRate))
                    .append("，盈亏=")
                    .append(money(s.pnl))
                    .append("\n");
            sb.append("均FinalP=")
                    .append(pct(s.avgFinalP))
                    .append("，均Gate=")
                    .append(pct(s.avgGate))
                    .append("，均Edge=")
                    .append(signedPct(s.avgEdge))
                    .append("，负Edge出手=")
                    .append(s.negativeEdgeBet)
                    .append("\n");
            sb.append("TeacherRisk出手=")
                    .append(s.teacherRiskBet)
                    .append("，V20风险出手=")
                    .append(s.v20RiskBet)
                    .append("，V20空BET=")
                    .append(s.v20EmptyTotal)
                    .append("\n");
            sb.append("最差位置=")
                    .append(emptyText(s.worstPosition, "暂无"))
                    .append(" ")
                    .append(pct(s.worstPositionRate))
                    .append("；最差Teacher=")
                    .append(emptyText(s.worstTeacherBucket, "暂无"))
                    .append(" ")
                    .append(pct(s.worstTeacherRate))
                    .append("；最差V20=")
                    .append(emptyText(s.worstV20Bucket, "暂无"))
                    .append(" ")
                    .append(pct(s.worstV20Rate))
                    .append("\n");
            sb.append("SHADOW：样本=")
                    .append(s.shadowTotal)
                    .append("，反事实中=")
                    .append(s.shadowHit)
                    .append("，反事实胜率=")
                    .append(pct(s.shadowHitRate))
                    .append("\n");
            if (s.mainProblem.length() > 0) {
                sb.append("原诊断主要问题=").append(s.mainProblem).append("\n");
            }
            sb.append("\n");
        }

        sb.append("二、核心证据\n");
        if (report.evidenceList.size() == 0) {
            sb.append("暂无强证据，继续积累BET/SHADOW样本。\n");
        } else {
            for (int i = 0; i < report.evidenceList.size(); i++) {
                PlanEvidence e = report.evidenceList.get(i);
                sb.append(i + 1)
                        .append(". [")
                        .append(e.tag)
                        .append("/S")
                        .append(e.severity)
                        .append("] ")
                        .append(e.text)
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append("三、总体问题\n");
        report.overallProblem = buildOverallProblem(report, s);
        sb.append(report.overallProblem).append("\n\n");

        sb.append("四、文件级修复计划\n");
        for (int i = 0; i < report.filePlans.size(); i++) {
            FileFixPlan p = report.filePlans.get(i);
            sb.append(i + 1).append("）").append(p.fileName).append("\n");
            sb.append("优先级=").append(p.priority).append("，状态=").append(p.status).append("\n");
            sb.append("证据=").append(p.problemEvidence).append("\n");
            sb.append("为什么修=").append(p.whyThisFile).append("\n");
            sb.append("怎么修=").append(p.whatToChange).append("\n");
            sb.append("预期效果=").append(p.expectedEffect).append("\n");
            sb.append("不修风险=").append(p.riskIfNotChange).append("\n");
            sb.append("依赖=").append(p.dependency).append("\n\n");
        }

        report.priorityList = buildPriorityList(report.filePlans);
        sb.append("五、优先级列表\n");
        sb.append(report.priorityList).append("\n\n");

        sb.append("六、是否需要新增引擎\n");
        sb.append(report.newEngineNeeded).append("\n\n");

        sb.append("七、策略缺口\n");
        sb.append(report.strategyGap).append("\n\n");

        sb.append("八、预期效果\n");
        sb.append(report.expectedEffect).append("\n\n");

        sb.append("九、风险提醒\n");
        sb.append(report.riskWarning).append("\n\n");

        sb.append("十、本文件自检口径\n");
        sb.append("1. 本文件只生成FixPlanReport/FileFixPlan，不出BET。\n");
        sb.append("2. 后续引擎报告全部Object反射读取，避免第2/14提前依赖第3-6新增文件导致AIDE编译失败。\n");
        sb.append("3. saveFixPlanState只写state文本，不写真实盈亏，不写Trade。\n");
        sb.append("4. 所有建议最终都必须等第14/14 DecisionGateEngine接入后才影响真实BET。\n");

        report.detail = limit(sb.toString(), MAX_REPORT_LEN);
        report.summary = buildSummary(report, s);
    }

    private String buildOverallProblem(FixPlanReport report, DiagnosisSnapshot s) {
        String problem = "";
        if (s == null || s.hasDiagnosis == 0) {
            return "当前没有可读取的真实BET诊断，先保证RealBetDiagnosisEngine能输出DiagnosisReport，再继续补齐模块评分、SHADOW晋级、冻结矩阵和智能审判。";
        }
        if (s.realBetBelowBase == 1) {
            problem = append(problem, "真实BET低于K3基础线，说明放行质量明显不足");
        } else if (s.realBetBelowTarget == 1) {
            problem = append(problem, "真实BET低于目标线37%，需要筛纯强域");
        }
        if (s.finalPOverEstimated == 1) {
            problem = append(problem, "FinalP高于真实效果，概率需要诊断折扣");
        }
        if (s.teacherWeakBad == 1 || s.teacherFakeStrong == 1 || s.teacherRiskRelease == 1) {
            problem = append(problem, "Teacher分级没有有效阻断弱/风险状态");
        }
        if (s.v20EmptyStillBet == 1 || s.v20FakeSupport == 1 || s.v20RiskRelease == 1) {
            problem = append(problem, "V20因链为空或假支撑，尚未成为可靠裁决依据");
        }
        if (s.negativeEdgeRelease == 1) {
            problem = append(problem, "负Edge仍被放行，DynamicGate/DecisionGate需要接入质量风险");
        }
        if (s.shadowBetterThanBet == 1) {
            problem = append(problem, "SHADOW存在可疑强域，需要反事实晋级而不是救援BET");
        }
        if (s.needRealtimeJudge == 1) {
            problem = append(problem, "诊断还没有在下一期出手前形成PASS/WATCH/DOWNGRADE/FREEZE审判");
        }
        if (problem.length() == 0) {
            problem = "当前没有单一压倒性错误，但十四文件闭环仍需补齐，使诊断能反哺下一期审判。";
        }
        return problem;
    }

    private String buildSummary(FixPlanReport report, DiagnosisSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("诊断修复计划：");
        if (s == null || s.hasDiagnosis == 0) {
            sb.append("诊断为空，先补RealBetDiagnosisEngine/DbStore读取链路");
        } else {
            sb.append("BET=")
                    .append(s.totalBet)
                    .append(" 胜率=")
                    .append(pct(s.winRate))
                    .append(" 均P=")
                    .append(pct(s.avgFinalP));
            if (s.v20EmptyStillBet == 1) sb.append("；V20空仍BET");
            if (s.teacherWeakBad == 1 || s.teacherFakeStrong == 1) sb.append("；Teacher假强/弱");
            if (s.finalPOverEstimated == 1) sb.append("；FinalP虚高");
            if (s.negativeEdgeRelease == 1) sb.append("；负Edge放行");
            if (s.shadowBetterThanBet == 1) sb.append("；SHADOW可晋级");
        }
        if (report != null && report.filePlans != null && report.filePlans.size() > 0) {
            sb.append("；优先修=");
            int max = Math.min(5, report.filePlans.size());
            for (int i = 0; i < max; i++) {
                if (i > 0) sb.append(" > ");
                sb.append(report.filePlans.get(i).fileName);
            }
        }
        return limit(sb.toString(), MAX_SUMMARY_LEN);
    }

    private String buildPriorityList(ArrayList<FileFixPlan> plans) {
        if (plans == null || plans.size() == 0) {
            return "暂无文件级修复计划";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < plans.size(); i++) {
            FileFixPlan p = plans.get(i);
            if (i > 0) sb.append("\n");
            sb.append(i + 1)
                    .append(". ")
                    .append(p.fileName)
                    .append("（P")
                    .append(p.priority)
                    .append("，")
                    .append(p.status)
                    .append("）");
        }
        return sb.toString();
    }

    private void sortFilePlans(ArrayList<FileFixPlan> list) {
        if (list == null) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<FileFixPlan>() {
                    public int compare(FileFixPlan a, FileFixPlan b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.priority != b.priority) return a.priority - b.priority;
                        return safe(a.fileName).compareTo(safe(b.fileName));
                    }
                });
    }

    private void addEvidence(FixPlanReport report, String tag, int severity, String text) {
        if (report == null) {
            return;
        }
        PlanEvidence e = new PlanEvidence();
        e.tag = safe(tag);
        e.severity = severity;
        e.text = safe(text);
        report.evidenceList.add(e);
        report.totalEvidenceCount++;
        if (severity >= 85) {
            report.criticalRiskCount++;
        }
        if (report.evidenceNotes.length() == 0) {
            report.evidenceNotes = e.tag + ":" + e.text;
        } else {
            report.evidenceNotes = report.evidenceNotes + "；" + e.tag + ":" + e.text;
        }
    }

    private String evidenceText(FixPlanReport report, String tag, String fallback) {
        if (report == null || tag == null) {
            return safe(fallback);
        }
        for (int i = 0; i < report.evidenceList.size(); i++) {
            PlanEvidence e = report.evidenceList.get(i);
            if (e != null && tag.equals(e.tag)) {
                return e.text;
            }
        }
        return safe(fallback);
    }

    private String joinEvidence(FixPlanReport report, String[] tags, String fallback) {
        if (report == null || tags == null || tags.length == 0) {
            return safe(fallback);
        }
        String out = "";
        for (int i = 0; i < tags.length; i++) {
            String t = tags[i];
            for (int j = 0; j < report.evidenceList.size(); j++) {
                PlanEvidence e = report.evidenceList.get(j);
                if (e != null && t != null && t.equals(e.tag)) {
                    out = append(out, e.text);
                }
            }
        }
        if (out.length() == 0) {
            return safe(fallback);
        }
        return out;
    }

    private int findBucketTotal(
            HashMap<String, RealBetDiagnosisEngine.StatBucket> map, String containsText) {
        RealBetDiagnosisEngine.StatBucket b = findBucket(map, containsText);
        return bucketTotal(b);
    }

    private double findBucketRate(
            HashMap<String, RealBetDiagnosisEngine.StatBucket> map, String containsText) {
        RealBetDiagnosisEngine.StatBucket b = findBucket(map, containsText);
        return bucketRate(b);
    }

    private RealBetDiagnosisEngine.StatBucket findBucket(
            HashMap<String, RealBetDiagnosisEngine.StatBucket> map, String containsText) {
        if (map == null || map.size() == 0) {
            return null;
        }
        String c = safe(containsText).toUpperCase(java.util.Locale.ROOT);
        RealBetDiagnosisEngine.StatBucket best = null;
        for (String key : map.keySet()) {
            String k = safe(key);
            if (k.length() == 0) {
                continue;
            }
            String ku = k.toUpperCase(java.util.Locale.ROOT);
            if (ku.indexOf(c) >= 0 || k.indexOf(containsText) >= 0) {
                RealBetDiagnosisEngine.StatBucket b = map.get(key);
                if (b == null) {
                    continue;
                }
                if (best == null || b.total > best.total) {
                    best = b;
                }
            }
        }
        return best;
    }

    private int findWeakLikeBucketTotal(HashMap<String, RealBetDiagnosisEngine.StatBucket> map) {
        RealBetDiagnosisEngine.StatBucket b = findWeakLikeBucket(map);
        return bucketTotal(b);
    }

    private double findWeakLikeBucketRate(HashMap<String, RealBetDiagnosisEngine.StatBucket> map) {
        RealBetDiagnosisEngine.StatBucket b = findWeakLikeBucket(map);
        return bucketRate(b);
    }

    private RealBetDiagnosisEngine.StatBucket findWeakLikeBucket(
            HashMap<String, RealBetDiagnosisEngine.StatBucket> map) {
        if (map == null || map.size() == 0) {
            return null;
        }
        RealBetDiagnosisEngine.StatBucket best = null;
        for (String key : map.keySet()) {
            String k = safe(key).toUpperCase(java.util.Locale.ROOT);
            if (k.indexOf("WEAK") < 0
                    && k.indexOf("RISK") < 0
                    && k.indexOf("FREEZE") < 0
                    && key.indexOf("偏弱") < 0
                    && key.indexOf("风险") < 0
                    && key.indexOf("冻结") < 0) {
                continue;
            }
            RealBetDiagnosisEngine.StatBucket b = map.get(key);
            if (b == null) {
                continue;
            }
            if (best == null || b.total > best.total) {
                best = b;
            }
        }
        return best;
    }

    private String bucketName(RealBetDiagnosisEngine.StatBucket b) {
        if (b == null) {
            return "";
        }
        return safe(b.name);
    }

    private int bucketTotal(RealBetDiagnosisEngine.StatBucket b) {
        if (b == null) {
            return 0;
        }
        return b.total;
    }

    private int bucketMiss(RealBetDiagnosisEngine.StatBucket b) {
        if (b == null) {
            return 0;
        }
        return b.miss;
    }

    private double bucketRate(RealBetDiagnosisEngine.StatBucket b) {
        if (b == null || b.total <= 0) {
            return 0.0d;
        }
        try {
            return b.rate();
        } catch (Throwable e) {
            return ((double) b.hit) / ((double) b.total);
        }
    }

    private void addCount(HashMap<String, Integer> map, String key) {
        if (map == null || key == null || key.length() == 0) {
            return;
        }
        Integer old = map.get(key);
        if (old == null) {
            map.put(key, Integer.valueOf(1));
        } else {
            map.put(key, Integer.valueOf(old.intValue() + 1));
        }
    }

    private Object firstObjectField(Object obj, String[] names) {
        if (obj == null || names == null) {
            return null;
        }
        for (int i = 0; i < names.length; i++) {
            try {
                Field f = obj.getClass().getField(names[i]);
                Object v = f.get(obj);
                if (v != null) {
                    return v;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private String getString(Object obj, String field, String def) {
        if (obj == null || field == null || field.length() == 0) {
            return def == null ? "" : def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }
        return def == null ? "" : def;
    }

    private int getInt(Object obj, String field, int def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private int firstPositiveInt(int a, int b, int c) {
        if (a > 0) return a;
        if (b > 0) return b;
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

    private double nonZeroOrOne(double v) {
        if (v <= 0.0d) {
            return 1.0d;
        }
        return v;
    }

    private String append(String a, String b) {
        if (b == null || b.length() == 0) return safe(a);
        if (a == null || a.length() == 0) return b;
        return a + "；" + b;
    }

    private String emptyText(String s, String fallback) {
        if (s == null || s.length() == 0) {
            return fallback == null ? "" : fallback;
        }
        return s;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static String safeStatic(String s) {
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

    private static String pctStatic(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
        }
    }

    private String limit(String text, int max) {
        if (text == null) return "";
        if (max <= 0 || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private void trySetState(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) {
            return;
        }
        try {
            Method m =
                    store.getClass()
                            .getMethod("setState", new Class[] {String.class, String.class});
            m.invoke(store, new Object[] {key, value == null ? "" : value});
        } catch (Throwable e) {
        }
    }

    public static final class FixPlanReport {
        public int ok;
        public String version;
        public long createdAt;
        public DiagnosisSnapshot snapshot;
        public ArrayList<PlanEvidence> evidenceList;
        public ArrayList<FileFixPlan> filePlans;
        public int totalEvidenceCount;
        public int criticalRiskCount;
        public String evidenceNotes;
        public String overallProblem;
        public String priorityList;
        public String newEngineNeeded;
        public String strategyGap;
        public String expectedEffect;
        public String riskWarning;
        public String summary;
        public String detail;

        public FixPlanReport() {
            ok = 0;
            version = "";
            createdAt = 0L;
            snapshot = null;
            evidenceList = new ArrayList<PlanEvidence>();
            filePlans = new ArrayList<FileFixPlan>();
            totalEvidenceCount = 0;
            criticalRiskCount = 0;
            evidenceNotes = "";
            overallProblem = "";
            priorityList = "";
            newEngineNeeded = "";
            strategyGap = "";
            expectedEffect = "";
            riskWarning = "";
            summary = "";
            detail = "";
        }
    }

    public static final class FileFixPlan {
        public String fileName;
        public int priority;
        public String problemEvidence;
        public String whyThisFile;
        public String whatToChange;
        public String expectedEffect;
        public String riskIfNotChange;
        public String dependency;
        public String status;
        public String sourceTags;

        public FileFixPlan() {
            fileName = "";
            priority = 999;
            problemEvidence = "";
            whyThisFile = "";
            whatToChange = "";
            expectedEffect = "";
            riskIfNotChange = "";
            dependency = "";
            status = "";
            sourceTags = "";
        }

        public String shortText() {
            return fileName + " P" + priority + "：" + problemEvidence;
        }

        public String fullText() {
            StringBuilder sb = new StringBuilder();
            sb.append(fileName).append("\n");
            sb.append("优先级=").append(priority).append("，状态=").append(status).append("\n");
            sb.append("证据=").append(problemEvidence).append("\n");
            sb.append("为什么修=").append(whyThisFile).append("\n");
            sb.append("怎么修=").append(whatToChange).append("\n");
            sb.append("预期效果=").append(expectedEffect).append("\n");
            sb.append("不修风险=").append(riskIfNotChange).append("\n");
            sb.append("依赖=").append(dependency);
            return sb.toString();
        }
    }

    public static final class PlanEvidence {
        public String tag;
        public int severity;
        public String text;

        public PlanEvidence() {
            tag = "";
            severity = 0;
            text = "";
        }

        public String toString() {
            return "[" + tag + "/S" + severity + "] " + text;
        }
    }

    public static final class DiagnosisSnapshot {
        public int hasDiagnosis;
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
        public double shadowHitRate;
        public String mainProblem;
        public String nextFixAdvice;
        public String bestPosition;
        public String worstPosition;
        public double bestPositionRate;
        public double worstPositionRate;
        public int bestPositionTotal;
        public int worstPositionTotal;
        public String bestFinalPBucket;
        public String worstFinalPBucket;
        public double bestFinalPRate;
        public double worstFinalPRate;
        public int worstFinalPTotal;
        public String bestTeacherBucket;
        public String worstTeacherBucket;
        public double bestTeacherRate;
        public double worstTeacherRate;
        public int worstTeacherTotal;
        public String bestV20Bucket;
        public String worstV20Bucket;
        public double bestV20Rate;
        public double worstV20Rate;
        public int worstV20Total;
        public String topMissBucket;
        public int topMissCount;
        public int betP36Total;
        public double betP36Rate;
        public int betP38Total;
        public double betP38Rate;
        public int betP40Total;
        public double betP40Rate;
        public int shadowP36Total;
        public double shadowP36Rate;
        public int shadowP38Total;
        public double shadowP38Rate;
        public int shadowP40Total;
        public double shadowP40Rate;
        public int v20EmptyTotal;
        public double v20EmptyRate;
        public int teacherWeakTotal;
        public double teacherWeakRate;
        public int windowEmptyTotal;
        public double windowEmptyRate;
        public HashMap<String, Integer> failureTypeCounts;
        public HashMap<String, Integer> moduleResponsibilityCounts;
        public int probabilityOverEstimatedCount;
        public int teacherFakeStrongCount;
        public int teacherWeakStillBetCount;
        public int v20EmptyStillBetCount;
        public int v20FakeSupportCount;
        public int windowStateUntrustedCount;
        public int positionDragCount;
        public int k3CoverageMissCount;
        public int thirdNumberWeakCount;
        public int negativeEdgeDiagnosisCount;
        public int shadowMissedOpportunityCount;
        public int gateWrongReleaseCount;
        public int gateWrongBlockCount;
        public int hasModuleEffectReport;
        public int externalMisleadingModuleCount;
        public int externalEffectiveModuleCount;
        public String externalModuleSummary;
        public int hasShadowPromotionReport;
        public int externalPromotedDomainCount;
        public String externalShadowSummary;
        public int hasFreezeReport;
        public int externalFrozenCount;
        public String externalFreezeSummary;
        public int hasJudgeReport;
        public int externalDowngradeCount;
        public String externalJudgeSummary;
        public int sampleInsufficient;
        public int realBetBelowBase;
        public int realBetBelowTarget;
        public int finalPOverEstimated;
        public int badFinalPBucket;
        public int negativeEdgeRelease;
        public int teacherRiskRelease;
        public int teacherWeakBad;
        public int teacherFakeStrong;
        public int v20RiskRelease;
        public int v20EmptyStillBet;
        public int v20FakeSupport;
        public int windowStateUntrusted;
        public int positionDrag;
        public int missReasonConcentrated;
        public int shadowBetterThanBet;
        public int highPDomainEffective;
        public int needDiagnosisStorage;
        public int needModuleEffectEngine;
        public int needShadowPromotionEngine;
        public int needFreezeEngine;
        public int needAdaptiveJudgeEngine;
        public int needRealtimeJudge;

        public DiagnosisSnapshot() {
            hasDiagnosis = 0;
            totalBet = 0;
            hitBet = 0;
            missBet = 0;
            waitBet = 0;
            dataErrorBet = 0;
            winRate = 0.0d;
            pnl = 0.0d;
            avgFinalP = 0.0d;
            avgGate = 0.0d;
            avgEdge = 0.0d;
            negativeEdgeBet = 0;
            teacherRiskBet = 0;
            v20RiskBet = 0;
            shadowTotal = 0;
            shadowHit = 0;
            shadowMiss = 0;
            shadowHitRate = 0.0d;
            mainProblem = "";
            nextFixAdvice = "";
            bestPosition = "";
            worstPosition = "";
            bestFinalPBucket = "";
            worstFinalPBucket = "";
            bestTeacherBucket = "";
            worstTeacherBucket = "";
            bestV20Bucket = "";
            worstV20Bucket = "";
            topMissBucket = "";
            failureTypeCounts = new HashMap<String, Integer>();
            moduleResponsibilityCounts = new HashMap<String, Integer>();
            externalModuleSummary = "";
            externalShadowSummary = "";
            externalFreezeSummary = "";
            externalJudgeSummary = "";
        }

        public String shortText() {
            return "BET="
                    + totalBet
                    + " 中="
                    + hitBet
                    + " 挂="
                    + missBet
                    + " 胜率="
                    + pctStatic(winRate)
                    + " 均P="
                    + pctStatic(avgFinalP)
                    + " SHADOW="
                    + shadowHit
                    + "/"
                    + shadowTotal;
        }
    }
}
