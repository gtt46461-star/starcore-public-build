package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * ModuleEffectScoreEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环升级 · 第3/14
 *
 * <p>新增核心引擎：模块真实效果评分引擎。
 *
 * <p>只负责读取历史真实BET与SHADOW反事实样本，判断每个模块到底有没有真实效果。
 *
 * <p>它不会： 1. 不出号； 2. 不结算； 3. 不创建Trade； 4. 不保存BET； 5. 不修改Decision； 6. 不修改概率； 7. 不修改门槛； 8. 不写真实盈亏； 9.
 * 不把SHADOW转BET； 10. 不绕过DecisionGateEngine。
 *
 * <p>它只输出： 1. ModuleEffectReport； 2. 每个模块的真实BET效果； 3. 每个模块的SHADOW反事实效果； 4. EFFECTIVE / NEUTRAL /
 * MISLEADING / INSUFFICIENT / CONFLICT 分级； 5. 给后续
 * ProbabilityEngine、DynamicFreezeMatrixEngine、AdaptiveLearningJudgeEngine、 DiagnosisFixPlanEngine
 * 使用的模块评分。
 *
 * <p>Java7 / AIDE 兼容：无 lambda、无 stream、无第三方库。
 */
public final class ModuleEffectScoreEngine {
    public static final String V46_ROLE = "SETTLED_EFFECT_ATTRIBUTION_ONLY";

    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;

    public static final String STATUS_EFFECTIVE = "EFFECTIVE";
    public static final String STATUS_NEUTRAL = "NEUTRAL";
    public static final String STATUS_MISLEADING = "MISLEADING";
    public static final String STATUS_INSUFFICIENT = "INSUFFICIENT";
    public static final String STATUS_CONFLICT = "CONFLICT";
    public static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    public static final String MODULE_PROBABILITY = "ProbabilityEngine";
    public static final String MODULE_TEACHER = "TeacherNumberMatrixEngine";
    public static final String MODULE_V20 = "DynamicCauseChainEngine";
    public static final String MODULE_WINDOW = "WindowState";
    public static final String MODULE_DYNAMIC_GATE = "DynamicGateEngine";
    public static final String MODULE_DECISION_GATE = "DecisionGateEngine";
    public static final String MODULE_K3_COVERAGE = "K3Coverage";
    public static final String MODULE_THIRD_NUMBER = "ThirdNumberLayer";
    public static final String MODULE_POSITION = "PositionProfile";
    public static final String MODULE_EDGE = "EdgeQuality";
    public static final String MODULE_SAMPLE = "SampleReliability";
    public static final String MODULE_RECENT = "RecentValidity";

    private static final int DEFAULT_BET_LIMIT = 240;
    private static final int DEFAULT_SHADOW_LIMIT = 360;
    private static final int DEFAULT_DECISION_SCAN_LIMIT = 720;
    private static final int RECENT_TAIL_LIMIT = 60;
    private static final int MIN_SAMPLE_EFFECTIVE = 30;
    private static final int MIN_SAMPLE_LIGHT = 12;
    private static final int MAX_REPORT_LEN = 18000;
    private static final int MAX_CASE_REASON_LEN = 220;
    private static final int POSITION_COUNT = 10;

    private static final double BASE_K3_RATE = 0.300d;
    private static final double EFFECTIVE_MARGIN = 0.055d;
    private static final double MISLEADING_MARGIN = -0.030d;
    private static final double CONFLICT_DROP = 0.070d;
    private static final double SHADOW_PROMOTION_MARGIN = 0.070d;

    public ModuleEffectScoreEngine() {}

    public ModuleEffectReport score(DbStore store) {
        return score(store, DEFAULT_BET_LIMIT, DEFAULT_SHADOW_LIMIT);
    }

    public ModuleEffectReport score(DbStore store, int betLimit, int shadowLimit) {
        ModuleEffectReport report = new ModuleEffectReport();
        report.version = VERSION;
        report.createdAt = System.currentTimeMillis();

        if (betLimit <= 0) {
            betLimit = DEFAULT_BET_LIMIT;
        }
        if (shadowLimit <= 0) {
            shadowLimit = DEFAULT_SHADOW_LIMIT;
        }
        report.betLimit = betLimit;
        report.shadowLimit = shadowLimit;

        if (store == null) {
            report.ok = 0;
            report.summary = "模块评分失败：DbStore为空";
            report.detail = report.summary;
            return report;
        }

        int strictOosSample = countStrictOos(store);
        ArrayList<SignalCase> betCases = loadRecentRealBetCases(store, betLimit);
        ArrayList<SignalCase> shadowCases = loadRecentShadowCases(store, shadowLimit);

        report.ok = 1;
        report.betCases = betCases;
        report.shadowCases = shadowCases;
        report.strictOosSample = strictOosSample;
        report.researchEvidenceStatus =
                state(store, "r811_research_ablation_status", "UNAVAILABLE");

        initCoreModules(report);
        if (strictOosSample <= 0) {
            markStrictOosUnavailable(report);
            buildReportText(report);
            return report;
        }
        analyzeRealBetCases(report, betCases);
        analyzeShadowCases(report, shadowCases);
        finalizeEffects(report);
        buildReportText(report);

        return report;
    }

    public String buildModuleEffectSummary(DbStore store) {
        ModuleEffectReport report = score(store);
        return report == null ? "" : safe(report.summary);
    }

    public String buildModuleEffectDetail(DbStore store) {
        ModuleEffectReport report = score(store);
        return report == null ? "" : safe(report.detail);
    }

    public String buildModuleEffectCopyText(DbStore store) {
        ModuleEffectReport report = score(store);
        if (report == null) {
            return "V20模块真实效果评分为空";
        }
        return safe(report.detail);
    }

    public ModuleEffectReport saveModuleEffectState(DbStore store) {
        ModuleEffectReport report = score(store);
        saveModuleEffectState(store, report);
        return report;
    }

    public void saveModuleEffectState(DbStore store, ModuleEffectReport report) {
        if (store == null || report == null) {
            return;
        }
        setStateSafe(store, "module_effect_score_version", safe(report.version));
        setStateSafe(store, "module_effect_score_updated_at", String.valueOf(report.createdAt));
        setStateSafe(store, "module_effect_score_status", report.ok == 1 ? "OK" : "ERROR");
        setStateSafe(store, "module_effect_score_overall_status", safe(report.overallStatus));
        setStateSafe(
                store,
                "module_effect_score_strict_oos_sample",
                String.valueOf(report.strictOosSample));
        setStateSafe(
                store,
                "module_effect_score_research_status",
                safe(report.researchEvidenceStatus));
        setStateSafe(store, "module_effect_score_total_bet", String.valueOf(report.totalBet));
        setStateSafe(store, "module_effect_score_hit_bet", String.valueOf(report.hitBet));
        setStateSafe(store, "module_effect_score_miss_bet", String.valueOf(report.missBet));
        setStateSafe(store, "module_effect_score_win_rate", String.valueOf(report.winRate));
        setStateSafe(store, "module_effect_score_shadow_total", String.valueOf(report.shadowTotal));
        setStateSafe(
                store, "module_effect_score_shadow_hit_rate", String.valueOf(report.shadowHitRate));
        setStateSafe(
                store,
                "module_effect_score_best_module",
                report.bestEffect == null ? "" : safe(report.bestEffect.moduleName));
        setStateSafe(
                store,
                "module_effect_score_worst_module",
                report.worstEffect == null ? "" : safe(report.worstEffect.moduleName));
        setStateSafe(store, "module_effect_score_main_problem", safe(report.mainProblem));
        setStateSafe(store, "module_effect_score_next_advice", safe(report.nextAdvice));
        setStateSafe(store, "module_effect_score_summary", safe(report.summary));
        setStateSafe(store, "module_effect_score_detail", safe(report.detail));
        setStateSafe(store, "module_effect_score_compact_json", safe(buildCompactJson(report)));
    }

    private void initCoreModules(ModuleEffectReport report) {
        ensureEffect(report, MODULE_PROBABILITY, "FinalP/RawP/CalibratedP 概率模块");
        ensureEffect(report, MODULE_TEACHER, "教师号码矩阵模块");
        ensureEffect(report, MODULE_V20, "V20因链/证据反证模块");
        ensureEffect(report, MODULE_WINDOW, "WindowState/相似窗口模块");
        ensureEffect(report, MODULE_DYNAMIC_GATE, "动态门槛模块");
        ensureEffect(report, MODULE_DECISION_GATE, "最终真实BET出口模块");
        ensureEffect(report, MODULE_K3_COVERAGE, "单位置固定K3覆盖模块");
        ensureEffect(report, MODULE_THIRD_NUMBER, "K3第三号层级模块");
        ensureEffect(report, MODULE_POSITION, "位置画像模块");
        ensureEffect(report, MODULE_EDGE, "Edge质量模块");
        ensureEffect(report, MODULE_SAMPLE, "样本可信模块");
        ensureEffect(report, MODULE_RECENT, "近期有效性模块");
    }

    private void markStrictOosUnavailable(ModuleEffectReport report) {
        if (report == null) return;
        report.effectiveCount = 0;
        report.neutralCount = 0;
        report.misleadingCount = 0;
        report.conflictCount = 0;
        report.insufficientCount = 0;
        for (int i = 0; i < report.effects.size(); i++) {
            ModuleEffect effect = report.effects.get(i);
            if (effect == null) continue;
            effect.effectStatus = STATUS_UNAVAILABLE;
            effect.problem = "缺少合法strict OOS结算样本，不能把零样本解释为零贡献。";
            effect.fixAdvice = "继续积累predraw冻结且after-draw严格结算的OOS证据；Production与Research分栏。";
            effect.sampleWarning = "STRICT_OOS_REQUIRED";
        }
        report.overallStatus = "UNAVAILABLE_STRICT_OOS";
        report.mainProblem = "Production模块效果UNAVAILABLE：strict OOS样本为0。";
        report.nextAdvice =
                "先闭合OOS证据链；Research结果保持独立，不得写成Production模块效果。";
    }

    private ModuleEffect ensureEffect(
            ModuleEffectReport report, String moduleName, String description) {
        if (report == null) {
            return null;
        }
        moduleName = safe(moduleName);
        ModuleEffect effect = report.effectMap.get(moduleName);
        if (effect == null) {
            effect = new ModuleEffect();
            effect.moduleName = moduleName;
            effect.description = safe(description);
            effect.effectStatus = STATUS_INSUFFICIENT;
            report.effectMap.put(moduleName, effect);
            report.effects.add(effect);
        }
        return effect;
    }

    private void analyzeRealBetCases(ModuleEffectReport report, ArrayList<SignalCase> cases) {
        if (report == null || cases == null) {
            return;
        }

        for (int i = 0; i < cases.size(); i++) {
            SignalCase c = cases.get(i);
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
            String qualityPath = upper(firstNonEmpty(c.decisionQualityPath, c.betDomain));
            addModuleSupport(
                    report,
                    MODULE_DECISION_GATE,
                    c,
                    qualityPath.indexOf("QUALIFIED") >= 0 || qualityPath.indexOf("FLOOR") >= 0
                            ? "QUALIFIED_FLOOR_BET"
                            : "NORMAL_BET",
                    true,
                    "真实BET路径分离统计");
            if (c.hit == 1 || isWinResult(c.result)) {
                report.hitBet++;
            } else {
                report.missBet++;
            }
            report.pnl += c.pnl;
            report.avgFinalP += c.finalP;
            report.avgGate += c.dynamicGate;
            report.avgEdge += c.edge;

            addModuleSupport(
                    report, MODULE_DECISION_GATE, c, "BET_RELEASE", true, "DecisionGate真实放行样本");
            addPositionModuleSupport(report, c);
            addK3CoverageSupport(report, c);
            addThirdNumberSupport(report, c);

            if (c.finalP >= 0.400d) {
                addModuleSupport(
                        report, MODULE_PROBABILITY, c, "FINALP_HIGH_40", true, "FinalP>=40%高概率支持");
            } else if (c.finalP >= 0.380d) {
                addModuleSupport(
                        report,
                        MODULE_PROBABILITY,
                        c,
                        "FINALP_READY_38",
                        true,
                        "FinalP>=38%普通概率支持");
            } else if (c.finalP >= 0.360d) {
                addModuleSupport(
                        report, MODULE_PROBABILITY, c, "FINALP_LOW_36", false, "FinalP 36-38低位放行");
            } else {
                addModuleSupport(
                        report, MODULE_PROBABILITY, c, "FINALP_WEAK", false, "FinalP低于36仍放行");
            }

            if (c.edge >= 0.020d) {
                addModuleSupport(report, MODULE_EDGE, c, "EDGE_STRONG", true, "Edge>=2%强边际");
            } else if (c.edge >= 0.010d) {
                addModuleSupport(report, MODULE_EDGE, c, "EDGE_READY", true, "Edge>=1%有效边际");
            } else if (c.edge >= 0.0d) {
                addModuleSupport(report, MODULE_EDGE, c, "EDGE_THIN", false, "Edge接近0薄边际");
            } else {
                addModuleSupport(
                        report, MODULE_EDGE, c, "EDGE_NEGATIVE_RELEASE", false, "负Edge仍放行");
                report.negativeEdgeRelease++;
            }

            if (isTeacherStrong(c)) {
                addModuleSupport(
                        report,
                        MODULE_TEACHER,
                        c,
                        "TEACHER_STRONG_READY",
                        true,
                        "Teacher STRONG/READY支持");
            } else if (isTeacherRisk(c)) {
                addModuleSupport(
                        report,
                        MODULE_TEACHER,
                        c,
                        "TEACHER_RISK_RELEASE",
                        false,
                        "Teacher RISK/FREEZE仍放行");
                report.teacherRiskRelease++;
            } else if (isTeacherWeakOrEmpty(c)) {
                addModuleSupport(
                        report,
                        MODULE_TEACHER,
                        c,
                        "TEACHER_WEAK_EMPTY_RELEASE",
                        false,
                        "Teacher WEAK/空仍放行");
                report.teacherWeakRelease++;
            } else {
                addModuleSupport(
                        report, MODULE_TEACHER, c, "TEACHER_OTHER", false, "Teacher状态未形成强支持");
            }

            if (hasV20Support(c)) {
                addModuleSupport(report, MODULE_V20, c, "V20_SUPPORT", true, "V20因链有效支持");
            } else if (hasV20Risk(c)) {
                addModuleSupport(report, MODULE_V20, c, "V20_RISK_RELEASE", false, "V20反证/风险仍放行");
                report.v20RiskRelease++;
            } else {
                addModuleSupport(report, MODULE_V20, c, "V20_EMPTY_RELEASE", false, "V20因链为空仍BET");
                report.v20EmptyRelease++;
            }

            if (hasTrustedWindow(c)) {
                addModuleSupport(
                        report, MODULE_WINDOW, c, "WINDOW_TRUSTED", true, "WindowState/相似窗口样本可信");
            } else {
                addModuleSupport(
                        report, MODULE_WINDOW, c, "WINDOW_UNTRUSTED", false, "WindowState样本不足或状态空");
                report.windowUntrustedRelease++;
            }

            if (c.finalP >= c.dynamicGate && c.dynamicGate > 0.0d) {
                if (c.edge >= 0.010d) {
                    addModuleSupport(
                            report,
                            MODULE_DYNAMIC_GATE,
                            c,
                            "GATE_EDGE_PASS",
                            true,
                            "FinalP过Gate且Edge有效");
                } else {
                    addModuleSupport(
                            report,
                            MODULE_DYNAMIC_GATE,
                            c,
                            "GATE_THIN_PASS",
                            false,
                            "过Gate但Edge偏薄");
                }
            } else {
                addModuleSupport(
                        report,
                        MODULE_DYNAMIC_GATE,
                        c,
                        "GATE_BELOW_RELEASE",
                        false,
                        "FinalP未明显过Gate仍放行");
                report.gateBelowRelease++;
            }

            if (c.sampleReliability >= 0.600d
                    || c.effectiveSample >= 60
                    || c.comboSample >= 60
                    || c.teacherMatrixSample >= 60) {
                addModuleSupport(report, MODULE_SAMPLE, c, "SAMPLE_TRUSTED", true, "样本可信度较高");
            } else {
                addModuleSupport(
                        report, MODULE_SAMPLE, c, "SAMPLE_LOW_RELEASE", false, "样本可信度不足仍放行");
                report.sampleLowRelease++;
            }

            if (c.recentValidity >= 0.600d
                    || c.decisionEvidenceElite > 0
                    || c.decisionEvidenceStrong > 0) {
                addModuleSupport(report, MODULE_RECENT, c, "RECENT_VALID", true, "近期有效性较高");
            } else {
                addModuleSupport(
                        report, MODULE_RECENT, c, "RECENT_LOW_RELEASE", false, "近期有效性不足仍放行");
                report.recentLowRelease++;
            }

            buildSignalCaseReason(c);
        }

        if (report.totalBet > 0) {
            report.winRate = ((double) report.hitBet) / ((double) report.totalBet);
            report.avgFinalP = report.avgFinalP / (double) report.totalBet;
            report.avgGate = report.avgGate / (double) report.totalBet;
            report.avgEdge = report.avgEdge / (double) report.totalBet;
        }
    }

    private void analyzeShadowCases(ModuleEffectReport report, ArrayList<SignalCase> cases) {
        if (report == null || cases == null) {
            return;
        }

        for (int i = 0; i < cases.size(); i++) {
            SignalCase c = cases.get(i);
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

            addShadowModuleSupport(
                    report,
                    MODULE_DECISION_GATE,
                    c,
                    "SHADOW_BLOCKED_BY_GATE",
                    "DecisionGate拦截后的反事实");
            addPositionShadowSupport(report, c);
            addK3CoverageShadowSupport(report, c);
            addThirdNumberShadowSupport(report, c);

            if (c.finalP >= 0.400d) {
                addShadowModuleSupport(
                        report, MODULE_PROBABILITY, c, "SHADOW_FINALP_HIGH_40", "SHADOW高概率反事实");
            } else if (c.finalP >= 0.380d) {
                addShadowModuleSupport(
                        report, MODULE_PROBABILITY, c, "SHADOW_FINALP_READY_38", "SHADOW普通概率反事实");
            } else {
                addShadowModuleSupport(
                        report, MODULE_PROBABILITY, c, "SHADOW_FINALP_LOW", "SHADOW低概率反事实");
            }

            if (c.edge >= 0.010d) {
                addShadowModuleSupport(
                        report, MODULE_EDGE, c, "SHADOW_EDGE_POSITIVE", "SHADOW Edge有效");
            } else {
                addShadowModuleSupport(report, MODULE_EDGE, c, "SHADOW_EDGE_THIN", "SHADOW Edge偏薄");
            }

            if (isTeacherStrong(c)) {
                addShadowModuleSupport(
                        report,
                        MODULE_TEACHER,
                        c,
                        "SHADOW_TEACHER_STRONG_READY",
                        "SHADOW Teacher强域");
            } else if (isTeacherRisk(c)) {
                addShadowModuleSupport(
                        report, MODULE_TEACHER, c, "SHADOW_TEACHER_RISK", "SHADOW Teacher风险域");
            } else {
                addShadowModuleSupport(
                        report,
                        MODULE_TEACHER,
                        c,
                        "SHADOW_TEACHER_WEAK_EMPTY",
                        "SHADOW Teacher弱/空");
            }

            if (hasV20Support(c)) {
                addShadowModuleSupport(report, MODULE_V20, c, "SHADOW_V20_SUPPORT", "SHADOW V20支持");
            } else if (hasV20Risk(c)) {
                addShadowModuleSupport(report, MODULE_V20, c, "SHADOW_V20_RISK", "SHADOW V20风险");
            } else {
                addShadowModuleSupport(report, MODULE_V20, c, "SHADOW_V20_EMPTY", "SHADOW V20空");
            }

            if (hasTrustedWindow(c)) {
                addShadowModuleSupport(
                        report, MODULE_WINDOW, c, "SHADOW_WINDOW_TRUSTED", "SHADOW Window可信");
            } else {
                addShadowModuleSupport(
                        report, MODULE_WINDOW, c, "SHADOW_WINDOW_UNTRUSTED", "SHADOW Window不足");
            }

            if (c.finalP >= c.dynamicGate && c.dynamicGate > 0.0d) {
                addShadowModuleSupport(
                        report, MODULE_DYNAMIC_GATE, c, "SHADOW_GATE_PASS", "SHADOW过动态门");
            } else {
                addShadowModuleSupport(
                        report, MODULE_DYNAMIC_GATE, c, "SHADOW_GATE_BLOCK", "SHADOW未过动态门");
            }

            if (c.sampleReliability >= 0.600d
                    || c.effectiveSample >= 60
                    || c.comboSample >= 60
                    || c.teacherMatrixSample >= 60) {
                addShadowModuleSupport(
                        report, MODULE_SAMPLE, c, "SHADOW_SAMPLE_TRUSTED", "SHADOW样本可信");
            } else {
                addShadowModuleSupport(report, MODULE_SAMPLE, c, "SHADOW_SAMPLE_LOW", "SHADOW样本不足");
            }

            if (c.recentValidity >= 0.600d
                    || c.decisionEvidenceElite > 0
                    || c.decisionEvidenceStrong > 0) {
                addShadowModuleSupport(
                        report, MODULE_RECENT, c, "SHADOW_RECENT_VALID", "SHADOW近期有效");
            } else {
                addShadowModuleSupport(report, MODULE_RECENT, c, "SHADOW_RECENT_LOW", "SHADOW近期弱");
            }
        }

        if (report.shadowTotal > 0) {
            report.shadowHitRate = ((double) report.shadowHit) / ((double) report.shadowTotal);
        }
    }

    private void finalizeEffects(ModuleEffectReport report) {
        if (report == null) {
            return;
        }

        report.effectiveCount = 0;
        report.neutralCount = 0;
        report.misleadingCount = 0;
        report.insufficientCount = 0;
        report.conflictCount = 0;

        for (int i = 0; i < report.effects.size(); i++) {
            ModuleEffect e = report.effects.get(i);
            if (e == null) {
                continue;
            }

            finalizeOneEffect(e, report);

            if (STATUS_EFFECTIVE.equals(e.effectStatus)) {
                report.effectiveCount++;
            } else if (STATUS_MISLEADING.equals(e.effectStatus)) {
                report.misleadingCount++;
            } else if (STATUS_CONFLICT.equals(e.effectStatus)) {
                report.conflictCount++;
            } else if (STATUS_INSUFFICIENT.equals(e.effectStatus)) {
                report.insufficientCount++;
            } else {
                report.neutralCount++;
            }

            if (e.supportCount > 0) {
                report.moduleSupportTotal += e.supportCount;
            }

            if (report.bestEffect == null || isBetterEffect(e, report.bestEffect)) {
                report.bestEffect = e;
            }
            if (report.worstEffect == null || isWorseEffect(e, report.worstEffect)) {
                report.worstEffect = e;
            }
        }

        sortEffects(report.effects);

        if (report.misleadingCount > 0) {
            report.overallStatus = "HAS_MISLEADING_MODULE";
        } else if (report.conflictCount > 0) {
            report.overallStatus = "HAS_CONFLICT_MODULE";
        } else if (report.effectiveCount > 0 && report.totalBet >= MIN_SAMPLE_EFFECTIVE) {
            report.overallStatus = "HAS_EFFECTIVE_DOMAIN";
        } else if (report.totalBet < MIN_SAMPLE_LIGHT) {
            report.overallStatus = "SAMPLE_TOO_SMALL";
        } else {
            report.overallStatus = "NEEDS_MORE_EVIDENCE";
        }

        buildMainProblemAndAdvice(report);
    }

    private void finalizeOneEffect(ModuleEffect e, ModuleEffectReport report) {
        if (e == null) {
            return;
        }

        if (e.betCount > 0) {
            e.hitRate = ((double) e.hitCount) / ((double) e.betCount);
            e.avgFinalP = e.sumFinalP / (double) e.betCount;
            e.avgGate = e.sumGate / (double) e.betCount;
            e.avgEdge = e.sumEdge / (double) e.betCount;
            e.realEdge = e.hitRate - BASE_K3_RATE;
        } else {
            e.hitRate = 0.0d;
            e.avgFinalP = 0.0d;
            e.avgGate = 0.0d;
            e.avgEdge = 0.0d;
            e.realEdge = 0.0d;
        }

        if (e.recentCount > 0) {
            e.recentHitRate = ((double) e.recentHit) / ((double) e.recentCount);
            e.recentEffectiveness = e.recentHitRate - BASE_K3_RATE;
        } else {
            e.recentHitRate = 0.0d;
            e.recentEffectiveness = 0.0d;
        }

        if (e.shadowSupportCount > 0) {
            e.shadowWouldHitRate = ((double) e.shadowWouldHit) / ((double) e.shadowSupportCount);
            e.virtualEdge = e.shadowWouldHitRate - BASE_K3_RATE;
        } else {
            e.shadowWouldHitRate = 0.0d;
            e.virtualEdge = 0.0d;
        }

        if (e.supportCount > 0 && report != null && report.totalBet > 0) {
            e.supportRate = ((double) e.supportCount) / ((double) report.totalBet);
        } else {
            e.supportRate = 0.0d;
        }

        e.mainBucket = bestBucketName(e.bucketMap);
        e.worstBucket = worstBucketName(e.bucketMap);
        e.reliability = computeReliability(e);
        e.sampleWarning = "";

        if (e.betCount < MIN_SAMPLE_LIGHT && e.shadowSupportCount < MIN_SAMPLE_LIGHT) {
            e.effectStatus = STATUS_INSUFFICIENT;
            e.problem = "样本不足，不能证明该模块真实有效或误导。";
            e.fixAdvice = "继续累积真实BET和SHADOW反事实；不要仅凭少量样本改门槛。";
            e.sampleWarning = "样本不足";
            return;
        }

        if (e.betCount >= MIN_SAMPLE_EFFECTIVE && e.hitRate >= BASE_K3_RATE + EFFECTIVE_MARGIN) {
            if (e.recentCount >= MIN_SAMPLE_LIGHT && e.recentHitRate <= e.hitRate - CONFLICT_DROP) {
                e.effectStatus = STATUS_CONFLICT;
                e.problem = "历史有效但近期明显走弱，存在状态失效风险。";
                e.fixAdvice = "交给 DynamicFreezeMatrixEngine 做短期观察/冻结，避免继续按历史强域放行。";
            } else {
                e.effectStatus = STATUS_EFFECTIVE;
                e.problem = "该模块支持后的真实BET表现高于基础线，属于可保留强域。";
                e.fixAdvice = "后续 ProbabilityEngine 可保留小幅加权，DecisionGate 仍需看反证和冻结状态。";
            }
        } else if (e.betCount >= MIN_SAMPLE_EFFECTIVE
                && e.hitRate <= BASE_K3_RATE + MISLEADING_MARGIN) {
            e.effectStatus = STATUS_MISLEADING;
            e.problem = "该模块支持/放行后的真实BET低于基础线，疑似误导真实出手。";
            e.fixAdvice =
                    "后续 DynamicFreezeMatrixEngine 应冻结该类状态；ProbabilityEngine 降权；DecisionGate"
                        + " 不得按此模块单独放行。";
        } else if (e.betCount >= MIN_SAMPLE_LIGHT
                && e.shadowSupportCount >= MIN_SAMPLE_LIGHT
                && e.shadowWouldHitRate >= e.hitRate + SHADOW_PROMOTION_MARGIN) {
            e.effectStatus = STATUS_CONFLICT;
            e.problem = "真实BET效果一般，但SHADOW反事实明显更好，可能存在错杀强域或放行方向错位。";
            e.fixAdvice = "交给 ShadowPromotionVerifierEngine 细分域，不要全局降低门槛。";
        } else {
            e.effectStatus = STATUS_NEUTRAL;
            e.problem = "暂未证明该模块明显提升或明显伤害。";
            e.fixAdvice = "维持中性，等待更多样本；不作为单独BET理由。";
        }
    }

    private double computeReliability(ModuleEffect e) {
        if (e == null) {
            return 0.0d;
        }
        double sampleScore = 0.0d;
        if (e.betCount >= 120) {
            sampleScore = 1.0d;
        } else if (e.betCount >= 60) {
            sampleScore = 0.78d;
        } else if (e.betCount >= 30) {
            sampleScore = 0.56d;
        } else if (e.betCount >= 12) {
            sampleScore = 0.35d;
        } else {
            sampleScore = 0.18d;
        }

        double recentScore = 0.0d;
        if (e.recentCount >= 30) {
            recentScore = 0.9d;
        } else if (e.recentCount >= 12) {
            recentScore = 0.55d;
        } else if (e.recentCount > 0) {
            recentScore = 0.25d;
        }

        double shadowScore = 0.0d;
        if (e.shadowSupportCount >= 120) {
            shadowScore = 0.8d;
        } else if (e.shadowSupportCount >= 60) {
            shadowScore = 0.58d;
        } else if (e.shadowSupportCount >= 20) {
            shadowScore = 0.36d;
        } else if (e.shadowSupportCount > 0) {
            shadowScore = 0.15d;
        }

        double score = sampleScore * 0.62d + recentScore * 0.23d + shadowScore * 0.15d;
        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private boolean isBetterEffect(ModuleEffect a, ModuleEffect b) {
        if (a == null) return false;
        if (b == null) return true;
        int sa = statusRank(a.effectStatus);
        int sb = statusRank(b.effectStatus);
        if (sa != sb) return sa > sb;
        if (a.hitRate != b.hitRate) return a.hitRate > b.hitRate;
        return a.betCount > b.betCount;
    }

    private boolean isWorseEffect(ModuleEffect a, ModuleEffect b) {
        if (a == null) return false;
        if (b == null) return true;
        int sa = statusRiskRank(a.effectStatus);
        int sb = statusRiskRank(b.effectStatus);
        if (sa != sb) return sa > sb;
        if (a.hitRate != b.hitRate) return a.hitRate < b.hitRate;
        return a.betCount > b.betCount;
    }

    private int statusRank(String status) {
        if (STATUS_EFFECTIVE.equals(status)) return 5;
        if (STATUS_NEUTRAL.equals(status)) return 3;
        if (STATUS_CONFLICT.equals(status)) return 2;
        if (STATUS_INSUFFICIENT.equals(status)) return 1;
        if (STATUS_MISLEADING.equals(status)) return 0;
        return 1;
    }

    private int statusRiskRank(String status) {
        if (STATUS_MISLEADING.equals(status)) return 5;
        if (STATUS_CONFLICT.equals(status)) return 4;
        if (STATUS_INSUFFICIENT.equals(status)) return 2;
        if (STATUS_NEUTRAL.equals(status)) return 1;
        if (STATUS_EFFECTIVE.equals(status)) return 0;
        return 1;
    }

    private void sortEffects(ArrayList<ModuleEffect> list) {
        if (list == null) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<ModuleEffect>() {
                    public int compare(ModuleEffect a, ModuleEffect b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        int ra = statusRiskRank(a.effectStatus);
                        int rb = statusRiskRank(b.effectStatus);
                        if (ra != rb) return rb - ra;
                        if (a.betCount != b.betCount) return b.betCount - a.betCount;
                        if (a.hitRate < b.hitRate) return 1;
                        if (a.hitRate > b.hitRate) return -1;
                        return safe(a.moduleName).compareTo(safe(b.moduleName));
                    }
                });
    }

    private void buildMainProblemAndAdvice(ModuleEffectReport r) {
        if (r == null) {
            return;
        }

        String problem = "";
        String advice = "";

        if (r.totalBet < MIN_SAMPLE_LIGHT) {
            problem = append(problem, "真实BET样本少于" + MIN_SAMPLE_LIGHT + "，模块评分只作方向提示");
            advice = append(advice, "继续运行积累真实BET和SHADOW样本，避免过早冻结全部模块");
        }

        if (r.v20EmptyRelease > 0
                && r.totalBet > 0
                && ((double) r.v20EmptyRelease / (double) r.totalBet) >= 0.30d) {
            problem = append(problem, "V20因链为空仍BET占比偏高");
            advice =
                    append(
                            advice,
                            "优先在 DynamicCauseChainEngine/DecisionGateEngine"
                                + " 接入V20状态、证据贡献、反证贡献、样本可信和近期有效性");
        }

        if (r.teacherWeakRelease > 0
                && r.totalBet > 0
                && ((double) r.teacherWeakRelease / (double) r.totalBet) >= 0.30d) {
            problem = append(problem, "Teacher弱/空仍BET占比偏高");
            advice =
                    append(
                            advice,
                            "TeacherNumberMatrixEngine 需要重做 STRONG/READY/WEAK/RISK/FREEZE"
                                + " 分层，DecisionGate不得让弱Teacher单独放行");
        }

        if (r.negativeEdgeRelease > 0
                && r.totalBet > 0
                && ((double) r.negativeEdgeRelease / (double) r.totalBet) >= 0.12d) {
            problem = append(problem, "负Edge或薄Edge放行偏多");
            advice = append(advice, "DynamicGateEngine 从频率门槛改为质量风险门槛，低频不得救援弱证据");
        }

        if (r.windowUntrustedRelease > 0
                && r.totalBet > 0
                && ((double) r.windowUntrustedRelease / (double) r.totalBet) >= 0.40d) {
            problem = append(problem, "WindowState样本不足仍参与BET较多");
            advice =
                    append(
                            advice,
                            "ProbabilityEngine"
                                + " 对相似窗口低样本、featureDistance高、recentValidity低的候选折扣FinalP");
        }

        if (r.shadowTotal >= MIN_SAMPLE_LIGHT
                && r.totalBet >= MIN_SAMPLE_LIGHT
                && r.shadowHitRate > r.winRate + SHADOW_PROMOTION_MARGIN) {
            problem = append(problem, "SHADOW反事实整体好于真实BET，可能存在错杀强域");
            advice =
                    append(
                            advice,
                            "下一步 ShadowPromotionVerifierEngine 需要细分 promoted/watch/blocked"
                                + " 域，不能全局放开");
        }

        if (r.worstEffect != null && STATUS_MISLEADING.equals(r.worstEffect.effectStatus)) {
            problem =
                    append(
                            problem,
                            "最明显误导模块="
                                    + r.worstEffect.moduleName
                                    + "，"
                                    + r.worstEffect.shortText());
            advice =
                    append(
                            advice,
                            "DynamicFreezeMatrixEngine 应把该模块对应状态纳入冻结矩阵；DecisionGate最后接入冻结结果");
        }

        if (problem.length() == 0) {
            problem = "暂未发现单一误导模块，需要继续累积样本并等待第4-6引擎细分SHADOW、冻结、审判。";
        }
        if (advice.length() == 0) {
            advice =
                    "保持只读评分；后续接入"
                        + " ShadowPromotionVerifierEngine、DynamicFreezeMatrixEngine、AdaptiveLearningJudgeEngine"
                        + " 再进入DecisionGate。";
        }

        r.mainProblem = problem;
        r.nextAdvice = advice;
    }

    private void buildReportText(ModuleEffectReport r) {
        if (r == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【V20模块真实效果评分报告】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("口径=只读真实BET与SHADOW反事实；不出号、不结算、不创建Trade、不修改Decision、不写真实盈亏。\n\n");

        sb.append("一、总体样本\n");
        sb.append("真实BET：")
                .append(r.totalBet)
                .append("单，中")
                .append(r.hitBet)
                .append("，挂")
                .append(r.missBet)
                .append("，胜率=")
                .append(pct(r.winRate))
                .append("，盈亏=")
                .append(money(r.pnl))
                .append("，等待=")
                .append(r.waitBet)
                .append("，异常=")
                .append(r.dataErrorBet)
                .append("\n");
        sb.append("均FinalP=")
                .append(pct(r.avgFinalP))
                .append("，均Gate=")
                .append(pct(r.avgGate))
                .append("，均Edge=")
                .append(signedPct(r.avgEdge))
                .append("\n");
        sb.append("SHADOW反事实：")
                .append(r.shadowTotal)
                .append("单，中")
                .append(r.shadowHit)
                .append("，挂")
                .append(r.shadowMiss)
                .append("，胜率=")
                .append(pct(r.shadowHitRate))
                .append("，等待=")
                .append(r.shadowWait)
                .append("\n");
        sb.append("整体状态=").append(safe(r.overallStatus)).append("\n\n");
        sb.append("Production strict OOS样本=")
                .append(r.strictOosSample)
                .append("；Research证据栏=")
                .append(safe(r.researchEvidenceStatus))
                .append("（不并入Production评分）\n\n");

        sb.append("二、模块评分总览\n");
        sb.append("EFFECTIVE=")
                .append(r.effectiveCount)
                .append("，NEUTRAL=")
                .append(r.neutralCount)
                .append("，MISLEADING=")
                .append(r.misleadingCount)
                .append("，INSUFFICIENT=")
                .append(r.insufficientCount)
                .append("，CONFLICT=")
                .append(r.conflictCount)
                .append("，UNAVAILABLE=")
                .append("UNAVAILABLE_STRICT_OOS".equals(r.overallStatus) ? r.effects.size() : 0)
                .append("\n");
        if (r.bestEffect != null) {
            sb.append("最佳模块=").append(r.bestEffect.fullText()).append("\n");
        }
        if (r.worstEffect != null) {
            sb.append("风险模块=").append(r.worstEffect.fullText()).append("\n");
        }
        sb.append("\n");

        sb.append("三、逐模块真实效果\n");
        for (int i = 0; i < r.effects.size(); i++) {
            ModuleEffect e = r.effects.get(i);
            if (e == null) {
                continue;
            }
            sb.append(i + 1).append(". ").append(e.fullText()).append("\n");
            sb.append("   问题：").append(limit(safe(e.problem), 240)).append("\n");
            sb.append("   建议：").append(limit(safe(e.fixAdvice), 260)).append("\n");
            if (e.mainBucket != null && e.mainBucket.length() > 0) {
                sb.append("   主支持桶：").append(e.mainBucket).append("\n");
            }
            if (e.worstBucket != null && e.worstBucket.length() > 0) {
                sb.append("   风险桶：").append(e.worstBucket).append("\n");
            }
        }
        sb.append("\n");

        sb.append("四、关键风险计数\n");
        sb.append("Teacher弱/空仍BET=")
                .append(r.teacherWeakRelease)
                .append("，Teacher风险仍BET=")
                .append(r.teacherRiskRelease)
                .append("，V20空仍BET=")
                .append(r.v20EmptyRelease)
                .append("，V20风险仍BET=")
                .append(r.v20RiskRelease)
                .append("\n");
        sb.append("Window低可信仍BET=")
                .append(r.windowUntrustedRelease)
                .append("，负Edge放行=")
                .append(r.negativeEdgeRelease)
                .append("，Gate下方放行=")
                .append(r.gateBelowRelease)
                .append("，样本低可信放行=")
                .append(r.sampleLowRelease)
                .append("，近期低有效放行=")
                .append(r.recentLowRelease)
                .append("\n\n");

        sb.append("五、模块评分结论\n");
        sb.append("主要问题：").append(safe(r.mainProblem)).append("\n");
        sb.append("下一步建议：").append(safe(r.nextAdvice)).append("\n");
        sb.append("红线：本文件只给模块评分，不直接BET，不直接冻结，不直接晋级SHADOW，不绕过DecisionGateEngine。\n");

        r.detail = limit(sb.toString(), MAX_REPORT_LEN);
        r.summary = buildShortSummary(r);
    }

    private String buildShortSummary(ModuleEffectReport r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("模块评分：BET=")
                .append(r.totalBet)
                .append(" 胜率=")
                .append(pct(r.winRate))
                .append("；SHADOW=")
                .append(r.shadowTotal)
                .append(" 反事实=")
                .append(pct(r.shadowHitRate))
                .append("；有效=")
                .append(r.effectiveCount)
                .append(" 误导=")
                .append(r.misleadingCount)
                .append(" 冲突=")
                .append(r.conflictCount);
        if (r.worstEffect != null) {
            sb.append("；风险=")
                    .append(r.worstEffect.moduleName)
                    .append("(")
                    .append(r.worstEffect.effectStatus)
                    .append(")");
        }
        if (r.mainProblem != null && r.mainProblem.length() > 0) {
            sb.append("；问题=").append(limit(r.mainProblem, 220));
        }
        return limit(sb.toString(), 650);
    }

    private String buildCompactJson(ModuleEffectReport r) {
        if (r == null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"version\":\"").append(jsonSafe(r.version)).append("\",");
        sb.append("\"status\":\"").append(jsonSafe(r.overallStatus)).append("\",");
        sb.append("\"totalBet\":").append(r.totalBet).append(",");
        sb.append("\"winRate\":").append(r.winRate).append(",");
        sb.append("\"shadowTotal\":").append(r.shadowTotal).append(",");
        sb.append("\"shadowHitRate\":").append(r.shadowHitRate).append(",");
        sb.append("\"effectiveCount\":").append(r.effectiveCount).append(",");
        sb.append("\"misleadingCount\":").append(r.misleadingCount).append(",");
        sb.append("\"conflictCount\":").append(r.conflictCount).append(",");
        sb.append("\"strictOosSample\":").append(r.strictOosSample).append(",");
        sb.append("\"researchEvidenceStatus\":\"")
                .append(jsonSafe(r.researchEvidenceStatus))
                .append("\",");
        sb.append("\"worstModule\":\"")
                .append(jsonSafe(r.worstEffect == null ? "" : r.worstEffect.moduleName))
                .append("\",");
        sb.append("\"mainProblem\":\"").append(jsonSafe(limit(r.mainProblem, 360))).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private void addModuleSupport(
            ModuleEffectReport report,
            String moduleName,
            SignalCase c,
            String bucket,
            boolean positiveSupport,
            String note) {
        if (report == null || c == null) {
            return;
        }
        ModuleEffect e = ensureEffect(report, moduleName, "");
        if (e == null) {
            return;
        }

        e.supportCount++;
        e.betCount++;
        if (c.hit == 1 || isWinResult(c.result)) {
            e.hitCount++;
        } else {
            e.missCount++;
        }
        e.profit += c.pnl;
        e.sumFinalP += c.finalP;
        e.sumGate += c.dynamicGate;
        e.sumEdge += c.edge;
        if (!positiveSupport) {
            e.weakSupportCount++;
        }

        if (e.betCount <= RECENT_TAIL_LIMIT) {
            e.recentCount++;
            if (c.hit == 1 || isWinResult(c.result)) {
                e.recentHit++;
            }
        }

        addBucket(e, bucket, c);
        rememberCase(e, c, bucket, note, false);
    }

    private void addShadowModuleSupport(
            ModuleEffectReport report,
            String moduleName,
            SignalCase c,
            String bucket,
            String note) {
        if (report == null || c == null) {
            return;
        }
        ModuleEffect e = ensureEffect(report, moduleName, "");
        if (e == null) {
            return;
        }

        e.shadowSupportCount++;
        if (c.hit == 1 || isWinResult(c.result)) {
            e.shadowWouldHit++;
        } else {
            e.shadowWouldMiss++;
        }
        addShadowBucket(e, bucket, c);
        rememberCase(e, c, bucket, note, true);
    }

    private void addPositionModuleSupport(ModuleEffectReport report, SignalCase c) {
        if (report == null || c == null) return;
        String bucket = "P" + c.position;
        addModuleSupport(report, MODULE_POSITION, c, bucket, true, "位置画像真实BET样本");
    }

    private void addPositionShadowSupport(ModuleEffectReport report, SignalCase c) {
        if (report == null || c == null) return;
        String bucket = "SHADOW_P" + c.position;
        addShadowModuleSupport(report, MODULE_POSITION, c, bucket, "位置画像SHADOW反事实");
    }

    private void addK3CoverageSupport(ModuleEffectReport report, SignalCase c) {
        if (report == null || c == null) return;
        String bucket;
        boolean positive = true;
        if (c.actualNumber == c.n1) {
            bucket = "K3_RANK1_HIT";
        } else if (c.actualNumber == c.n2) {
            bucket = "K3_RANK2_HIT";
        } else if (c.actualNumber == c.n3) {
            bucket = "K3_RANK3_HIT";
        } else {
            bucket = "K3_COVERAGE_MISS";
            positive = false;
            report.k3CoverageMiss++;
        }
        addModuleSupport(report, MODULE_K3_COVERAGE, c, bucket, positive, "固定K3覆盖层");
    }

    private void addK3CoverageShadowSupport(ModuleEffectReport report, SignalCase c) {
        if (report == null || c == null) return;
        String bucket;
        if (c.actualNumber == c.n1) {
            bucket = "SHADOW_K3_RANK1_HIT";
        } else if (c.actualNumber == c.n2) {
            bucket = "SHADOW_K3_RANK2_HIT";
        } else if (c.actualNumber == c.n3) {
            bucket = "SHADOW_K3_RANK3_HIT";
        } else {
            bucket = "SHADOW_K3_COVERAGE_MISS";
        }
        addShadowModuleSupport(report, MODULE_K3_COVERAGE, c, bucket, "K3覆盖SHADOW反事实");
    }

    private void addThirdNumberSupport(ModuleEffectReport report, SignalCase c) {
        if (report == null || c == null) return;
        boolean hitThird = c.actualNumber == c.n3;
        if (hitThird) {
            report.thirdNumberHit++;
            addModuleSupport(report, MODULE_THIRD_NUMBER, c, "THIRD_NUMBER_HIT", true, "第三号命中贡献");
        } else {
            report.thirdNumberMiss++;
            addModuleSupport(
                    report, MODULE_THIRD_NUMBER, c, "THIRD_NUMBER_NOT_HIT", false, "第三号未贡献");
        }
    }

    private void addThirdNumberShadowSupport(ModuleEffectReport report, SignalCase c) {
        if (report == null || c == null) return;
        if (c.actualNumber == c.n3) {
            addShadowModuleSupport(
                    report, MODULE_THIRD_NUMBER, c, "SHADOW_THIRD_NUMBER_HIT", "第三号SHADOW命中");
        } else {
            addShadowModuleSupport(
                    report, MODULE_THIRD_NUMBER, c, "SHADOW_THIRD_NUMBER_NOT_HIT", "第三号SHADOW未命中");
        }
    }

    private void addBucket(ModuleEffect e, String bucket, SignalCase c) {
        if (e == null || c == null) return;
        bucket = normalizeBucket(bucket);
        EffectBucket b = e.bucketMap.get(bucket);
        if (b == null) {
            b = new EffectBucket(bucket);
            e.bucketMap.put(bucket, b);
            e.buckets.add(b);
        }
        b.add(c);
    }

    private void addShadowBucket(ModuleEffect e, String bucket, SignalCase c) {
        if (e == null || c == null) return;
        bucket = normalizeBucket(bucket);
        EffectBucket b = e.shadowBucketMap.get(bucket);
        if (b == null) {
            b = new EffectBucket(bucket);
            e.shadowBucketMap.put(bucket, b);
            e.shadowBuckets.add(b);
        }
        b.add(c);
    }

    private String bestBucketName(HashMap<String, EffectBucket> map) {
        EffectBucket b = bestBucket(map, true);
        return b == null ? "" : b.fullText();
    }

    private String worstBucketName(HashMap<String, EffectBucket> map) {
        EffectBucket b = bestBucket(map, false);
        return b == null ? "" : b.fullText();
    }

    private EffectBucket bestBucket(HashMap<String, EffectBucket> map, boolean high) {
        if (map == null || map.size() == 0) return null;
        EffectBucket best = null;
        for (String key : map.keySet()) {
            EffectBucket b = map.get(key);
            if (b == null || b.total <= 0) continue;
            if (best == null) {
                best = b;
                continue;
            }
            if (high) {
                if (b.rate() > best.rate() || (b.rate() == best.rate() && b.total > best.total)) {
                    best = b;
                }
            } else {
                if (b.rate() < best.rate() || (b.rate() == best.rate() && b.total > best.total)) {
                    best = b;
                }
            }
        }
        return best;
    }

    private void rememberCase(
            ModuleEffect e, SignalCase c, String bucket, String note, boolean shadow) {
        if (e == null || c == null) return;
        if (e.sampleCases.size() >= 24) {
            return;
        }
        ModuleSignalCase m = new ModuleSignalCase();
        m.kind = c.kind;
        m.targetIssue = c.targetIssue;
        m.position = c.position;
        m.n1 = c.n1;
        m.n2 = c.n2;
        m.n3 = c.n3;
        m.actualNumber = c.actualNumber;
        m.result = c.result;
        m.hit = c.hit;
        m.finalP = c.finalP;
        m.dynamicGate = c.dynamicGate;
        m.edge = c.edge;
        m.teacherStatus = c.teacherBucket;
        m.v20Status = c.v20Bucket;
        m.windowStateKey = c.windowBucket;
        m.bucket = normalizeBucket(bucket);
        m.note = safe(note);
        m.shadow = shadow ? 1 : 0;
        m.reason = c.caseReason;
        e.sampleCases.add(m);
    }

    private void buildSignalCaseReason(SignalCase c) {
        if (c == null) return;
        String reason = "";
        if (c.finalP >= 0.400d && c.hit == 0) {
            reason = append(reason, "FinalP高但未中");
        }
        if (isTeacherWeakOrEmpty(c)) {
            reason = append(reason, "Teacher弱/空");
        }
        if (isTeacherRisk(c)) {
            reason = append(reason, "Teacher风险");
        }
        if (!hasV20Support(c)) {
            reason = append(reason, "V20无有效支持");
        }
        if (hasV20Risk(c)) {
            reason = append(reason, "V20有反证");
        }
        if (!hasTrustedWindow(c)) {
            reason = append(reason, "Window样本不足");
        }
        if (c.edge < 0.0d) {
            reason = append(reason, "负Edge");
        }
        if (c.actualNumber > 0 && c.hit == 0) {
            reason = append(reason, "K3未覆盖实际号" + c.actualNumber);
        }
        if (reason.length() == 0) {
            reason = c.hit == 1 ? "模块支持后命中" : "未形成明确单因归因";
        }
        c.caseReason = limit(reason, MAX_CASE_REASON_LEN);
    }

    private boolean isTeacherStrong(SignalCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.indexOf("STRONG") >= 0) return true;
        if (s.indexOf("READY") >= 0) return true;
        if (s.indexOf("强") >= 0) return true;
        if (s.indexOf("准备") >= 0) return true;
        if (c.teacherMatrixSample >= 60
                && c.teacherMatrixHitRate >= 0.360d
                && c.teacherMatrixConfidence >= 0.50d) return true;
        if (c.teacherEvidenceContribution > c.teacherCounterContribution + 0.080d
                && c.teacherEvidenceContribution > 0.0d) return true;
        return false;
    }

    private boolean isTeacherRisk(SignalCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.indexOf("RISK") >= 0) return true;
        if (s.indexOf("FREEZE") >= 0) return true;
        if (s.indexOf("风险") >= 0) return true;
        if (s.indexOf("冻结") >= 0) return true;
        if (c.teacherMatrixRisk >= 0.65d) return true;
        if (c.teacherCounterContribution > c.teacherEvidenceContribution + 0.080d
                && c.teacherCounterContribution > 0.0d) return true;
        return false;
    }

    private boolean isTeacherWeakOrEmpty(SignalCase c) {
        if (c == null) return true;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.length() == 0 || "空".equals(s)) return true;
        if (s.indexOf("WEAK") >= 0) return true;
        if (s.indexOf("弱") >= 0) return true;
        if (s.indexOf("EMPTY") >= 0) return true;
        if (s.indexOf("NONE") >= 0) return true;
        if (c.teacherMatrixSample > 0 && c.teacherMatrixSample < 30) return true;
        return false;
    }

    private boolean hasV20Support(SignalCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.v20Bucket, c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
        if (s.length() == 0 || "空".equals(s)) return false;
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("风险") >= 0
                || s.indexOf("冻结") >= 0) return false;
        if (s.indexOf("SUPPORT") >= 0
                || s.indexOf("PASS") >= 0
                || s.indexOf("STRONG") >= 0
                || s.indexOf("READY") >= 0) return true;
        if (s.indexOf("有效") >= 0 || s.indexOf("支持") >= 0 || s.indexOf("强") >= 0) return true;
        if (c.evidenceContribution > c.counterContribution + 0.060d
                && c.evidenceContribution > 0.0d) return true;
        if (c.decisionEvidenceStrong > 0 || c.decisionEvidenceElite > 0) return true;
        return false;
    }

    private boolean hasV20Risk(SignalCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.v20Bucket, c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("风险") >= 0
                || s.indexOf("冻结") >= 0) return true;
        if (c.decisionHardRisk > 0 || c.decisionCounterRiskHigh > 0) return true;
        if (c.counterContribution > c.evidenceContribution + 0.080d && c.counterContribution > 0.0d)
            return true;
        return false;
    }

    private boolean hasTrustedWindow(SignalCase c) {
        if (c == null) return false;
        String s =
                firstNonEmpty(
                        c.windowBucket,
                        c.windowStateKey,
                        c.dynamicWindowStateKey,
                        c.probabilityWindowStateKey);
        if (s == null || s.length() == 0 || "空".equals(s)) return false;
        if (c.similarWindowSample >= 30) return true;
        if (c.effectiveSample >= 60) return true;
        if (c.comboSample >= 60) return true;
        if (c.sampleReliability >= 0.60d && c.recentValidity >= 0.45d) return true;
        return false;
    }

    private ArrayList<SignalCase> loadRecentRealBetCases(DbStore store, int limit) {
        ArrayList<SignalCase> out = new ArrayList<SignalCase>();
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
            String issue = safe(getString(d, "targetIssue", ""));
            if (issue.length() == 0 || used.containsKey(issue) || !hasStrictOos(store, issue)) {
                continue;
            }
            Models.Trade t = getTradeByTargetIssueSafe(store, issue);
            if (t == null || !isRealBetTrade(t)) {
                continue;
            }
            SignalCase c = fromTrade(store, t, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }

        if (out.size() < limit) {
            ArrayList<Models.Trade> trades = loadRecentTradesByReflection(store, limit * 2);
            for (int i = 0; i < trades.size(); i++) {
                if (out.size() >= limit) {
                    break;
                }
                Models.Trade t = trades.get(i);
                if (t == null || !isRealBetTrade(t)) {
                    continue;
                }
                String issue = safe(getString(t, "targetIssue", ""));
                if (issue.length() == 0
                        || used.containsKey(issue)
                        || !hasStrictOos(store, issue)) {
                    continue;
                }
                Models.Decision d = getDecisionByTargetIssueSafe(store, issue);
                SignalCase c = fromTrade(store, t, d);
                if (c != null) {
                    out.add(c);
                    used.put(issue, "1");
                }
            }
        }

        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<SignalCase>(out.subList(0, limit));
        }
        return out;
    }

    private ArrayList<SignalCase> loadRecentShadowCases(DbStore store, int limit) {
        ArrayList<SignalCase> out = new ArrayList<SignalCase>();
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
            String issue = safe(getString(d, "targetIssue", ""));
            if (issue.length() == 0 || used.containsKey(issue) || !hasStrictOos(store, issue)) {
                continue;
            }
            SignalCase c = fromShadowDecision(store, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }

        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<SignalCase>(out.subList(0, limit));
        }
        return out;
    }

    private int countStrictOos(DbStore store) {
        android.database.Cursor cursor = null;
        try {
            if (store == null) return 0;
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*) FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE strict_oos=1 AND settled_at>0",
                                    null);
            return cursor.moveToFirst() ? Math.max(0, cursor.getInt(0)) : 0;
        } catch (Throwable error) {
            return 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private boolean hasStrictOos(DbStore store, String targetIssue) {
        android.database.Cursor cursor = null;
        try {
            if (store == null || safe(targetIssue).length() == 0) return false;
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT 1 FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE target_issue=? AND strict_oos=1 AND settled_at>0 LIMIT 1",
                                    new String[] {safe(targetIssue)});
            return cursor.moveToFirst();
        } catch (Throwable error) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private String state(DbStore store, String key, String fallback) {
        try {
            return store == null ? fallback : safe(store.getState(key, fallback));
        } catch (Throwable error) {
            return fallback;
        }
    }

    private SignalCase fromTrade(DbStore store, Models.Trade t, Models.Decision d) {
        if (t == null) {
            return null;
        }

        SignalCase c = new SignalCase();
        c.kind = "BET";
        c.targetIssue = safe(getString(t, "targetIssue", ""));
        c.baseIssue = safe(getString(t, "baseIssue", ""));
        c.createdAt = getLong(t, "createdAt", 0L);
        if (c.createdAt <= 0L) {
            c.createdAt = getLong(t, "betTime", 0L);
        }
        c.position = getInt(t, "position", 0);
        c.n1 = getInt(t, "n1", 0);
        c.n2 = getInt(t, "n2", 0);
        c.n3 = getInt(t, "n3", 0);
        c.actualNumber = getInt(t, "actualNumber", 0);
        c.result = normalizeResult(getString(t, "result", ""));
        c.hit = isWinResult(c.result) ? 1 : 0;
        c.settled = getInt(t, "settled", 0);
        c.pnl = getDouble(t, "pnl", 0.0d);
        c.missReason = getString(t, "missReason", "");
        c.finalP = getDouble(t, "finalP", 0.0d);
        c.rawP = getDouble(t, "rawP", 0.0d);
        c.calibratedP = getDouble(t, "calibratedP", 0.0d);
        c.dynamicGate = getDouble(t, "dynamicGate", 0.0d);
        c.edge = getDouble(t, "edge", 0.0d);
        c.decisionQualityPath =
                firstNonEmpty(
                        getString(t, "decisionQualityPath", ""),
                        d == null ? "" : getString(d, "decisionQualityPath", ""));
        c.betDomain =
                firstNonEmpty(
                        getString(t, "betDomain", ""),
                        d == null ? "" : getString(d, "betDomain", ""));
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        c.effectiveSample = getInt(t, "effectiveSample", 0);
        c.comboSample = getInt(t, "comboSample", 0);
        c.similarHitRate = getDouble(t, "similarHitRate", 0.0d);
        c.comboHitRate = getDouble(t, "comboHitRate", 0.0d);
        c.stateMode = getString(t, "stateMode", "");
        c.reasonSummary = getString(t, "reasonSummary", "");
        copyOptionalSignalFields(c, t);

        if (d != null) {
            mergeDecisionFields(c, d);
        }

        fillActualFromDraw(store, c);
        normalizeSignalBuckets(c);
        return c;
    }

    private SignalCase fromShadowDecision(DbStore store, Models.Decision d) {
        if (d == null || !isShadowDecision(d)) {
            return null;
        }

        SignalCase c = new SignalCase();
        c.kind = "SHADOW";
        c.targetIssue = safe(getString(d, "targetIssue", ""));
        c.baseIssue = safe(getString(d, "baseIssue", ""));
        c.createdAt = getLong(d, "createdAt", 0L);
        c.position = getInt(d, "position", 0);
        c.n1 = getInt(d, "n1", 0);
        c.n2 = getInt(d, "n2", 0);
        c.n3 = getInt(d, "n3", 0);
        c.finalP = getDouble(d, "finalP", 0.0d);
        c.rawP = getDouble(d, "rawP", 0.0d);
        c.calibratedP = getDouble(d, "calibratedP", 0.0d);
        c.dynamicGate = getDouble(d, "dynamicGate", 0.0d);
        c.edge = getDouble(d, "edge", 0.0d);
        c.decisionQualityPath = getString(d, "decisionQualityPath", "");
        c.betDomain = getString(d, "betDomain", "");
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        c.effectiveSample = getInt(d, "effectiveSample", 0);
        c.comboSample = getInt(d, "comboSample", 0);
        c.similarHitRate = getDouble(d, "similarHitRate", 0.0d);
        c.comboHitRate = getDouble(d, "comboHitRate", 0.0d);
        c.stateMode = getString(d, "stateMode", "");
        c.reasonSummary = getString(d, "reasonSummary", "");
        copyOptionalSignalFields(c, d);

        Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
        if (draw != null && isDrawValid(draw) && isValidPosition(c.position)) {
            c.actualNumber = getNumberAtPositionSafe(draw, c.position);
            if (isValidNumber(c.actualNumber)) {
                c.settled = 1;
                c.hit = containsK3(c, c.actualNumber) ? 1 : 0;
                c.result = c.hit == 1 ? resultWin() : resultMiss();
            } else {
                c.settled = 0;
                c.result = resultWait();
            }
        } else {
            c.settled = 0;
            c.result = resultWait();
        }

        normalizeSignalBuckets(c);
        return c;
    }

    private void fillActualFromDraw(DbStore store, SignalCase c) {
        if (store == null || c == null) {
            return;
        }
        if (c.actualNumber > 0 && isValidNumber(c.actualNumber)) {
            if (c.settled == 1 && (isWinResult(c.result) || isMissResult(c.result))) {
                c.hit = containsK3(c, c.actualNumber) ? 1 : 0;
                c.result = c.hit == 1 ? resultWin() : resultMiss();
            }
            return;
        }
        Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
        if (draw != null && isDrawValid(draw) && isValidPosition(c.position)) {
            int n = getNumberAtPositionSafe(draw, c.position);
            if (isValidNumber(n)) {
                c.actualNumber = n;
                c.settled = 1;
                c.hit = containsK3(c, n) ? 1 : 0;
                c.result = c.hit == 1 ? resultWin() : resultMiss();
            }
        }
    }

    private void normalizeSignalBuckets(SignalCase c) {
        if (c == null) return;
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
        c.sampleBucket = sampleBucket(c);
        c.recentBucket = recentBucket(c);
    }

    private void copyOptionalSignalFields(SignalCase c, Object source) {
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
                firstNonZero(
                        getDouble(source, "teacherEvidenceContribution", 0.0d),
                        getDouble(source, "teacherMatrixEvidenceContribution", 0.0d),
                        c.teacherEvidenceContribution);
        c.teacherCounterContribution =
                firstNonZero(
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

        c.v20SupportScore =
                firstNonZero(
                        getDouble(source, "v20SupportScore", 0.0d),
                        getDouble(source, "dynamicSupportScore", 0.0d),
                        c.v20SupportScore);
        c.v20RiskScore =
                firstNonZero(
                        getDouble(source, "v20RiskScore", 0.0d),
                        getDouble(source, "dynamicRiskScore", 0.0d),
                        c.v20RiskScore);
        c.causeScore = getDouble(source, "causeScore", c.causeScore);
        c.positionCauseScore = getDouble(source, "positionCauseScore", c.positionCauseScore);
        c.numberCauseScore = getDouble(source, "numberCauseScore", c.numberCauseScore);
        c.counterfactualScore = getDouble(source, "counterfactualScore", c.counterfactualScore);
        c.thirdGuardScore = getDouble(source, "thirdGuardScore", c.thirdGuardScore);
        c.ruleHealthScore = getDouble(source, "ruleHealthScore", c.ruleHealthScore);
        c.shadowHealthScore = getDouble(source, "shadowHealthScore", c.shadowHealthScore);
        c.falseStrongRisk = getDouble(source, "falseStrongRisk", c.falseStrongRisk);
        c.seedExpansionScore = getDouble(source, "seedExpansionScore", c.seedExpansionScore);

        c.evidenceContribution =
                firstNonZero(
                        getDouble(source, "decisionEvidenceContribution", 0.0d),
                        getDouble(source, "dynamicGateEvidenceContribution", 0.0d),
                        getDouble(source, "probabilityEvidenceContribution", 0.0d),
                        getDouble(source, "evidenceContribution", c.evidenceContribution));
        c.counterContribution =
                firstNonZero(
                        getDouble(source, "decisionCounterContribution", 0.0d),
                        getDouble(source, "dynamicGateCounterContribution", 0.0d),
                        getDouble(source, "probabilityCounterEvidence", 0.0d),
                        getDouble(source, "counterContribution", c.counterContribution));
        c.sampleReliability =
                firstNonZero(
                        getDouble(source, "decisionSampleReliability", 0.0d),
                        getDouble(source, "dynamicGateSampleReliability", 0.0d),
                        getDouble(source, "probabilitySampleReliability", 0.0d),
                        getDouble(source, "sampleReliability", c.sampleReliability));
        c.recentValidity =
                firstNonZero(
                        getDouble(source, "decisionRecentValidity", 0.0d),
                        getDouble(source, "dynamicGateRecentValidity", 0.0d),
                        getDouble(source, "probabilityRecentValidity", 0.0d),
                        getDouble(source, "recentValidity", c.recentValidity));
        c.featureDistance =
                firstNonZero(
                        getDouble(source, "decisionFeatureDistance", 0.0d),
                        getDouble(source, "dynamicGateFeatureDistance", 0.0d),
                        getDouble(source, "probabilityFeatureDistance", 0.0d),
                        getDouble(source, "featureDistance", c.featureDistance));

        c.similarWindowSample =
                firstNonZeroInt(
                        getInt(source, "similarWindowSample", 0),
                        getInt(source, "teacherSimilarWindowSample", 0),
                        c.similarWindowSample);
        c.similarWindowHitRate =
                firstNonZero(
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
    }

    private void mergeDecisionFields(SignalCase c, Models.Decision d) {
        if (c == null || d == null) {
            return;
        }
        if (c.reasonSummary.length() == 0) {
            c.reasonSummary = getString(d, "reasonSummary", "");
        }
        if (c.finalP <= 0.0d) c.finalP = getDouble(d, "finalP", 0.0d);
        if (c.rawP <= 0.0d) c.rawP = getDouble(d, "rawP", 0.0d);
        if (c.calibratedP <= 0.0d) c.calibratedP = getDouble(d, "calibratedP", 0.0d);
        if (c.dynamicGate <= 0.0d) c.dynamicGate = getDouble(d, "dynamicGate", 0.0d);
        if (c.edge == 0.0d) c.edge = getDouble(d, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        if (c.effectiveSample <= 0) c.effectiveSample = getInt(d, "effectiveSample", 0);
        if (c.comboSample <= 0) c.comboSample = getInt(d, "comboSample", 0);
        if (c.similarHitRate <= 0.0d) c.similarHitRate = getDouble(d, "similarHitRate", 0.0d);
        if (c.comboHitRate <= 0.0d) c.comboHitRate = getDouble(d, "comboHitRate", 0.0d);
        if (c.stateMode.length() == 0) c.stateMode = getString(d, "stateMode", "");
        copyOptionalSignalFields(c, d);
    }

    private ArrayList<Models.Decision> loadRecentDecisions(DbStore store, int limit) {
        ArrayList<Models.Decision> out = new ArrayList<Models.Decision>();
        if (store == null) return out;
        try {
            Method m = store.getClass().getMethod("getRecentDecisions", new Class[] {int.class});
            Object obj = m.invoke(store, new Object[] {Integer.valueOf(limit)});
            if (obj instanceof List) {
                List list = (List) obj;
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    if (o instanceof Models.Decision) {
                        out.add((Models.Decision) o);
                    }
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
                Method m = store.getClass().getMethod(names[i], new Class[] {int.class});
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
        String[] names =
                new String[] {
                    "getTradeByTargetIssue", "loadTradeByTargetIssue", "getRealTradeByTargetIssue"
                };
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {String.class});
                Object obj = m.invoke(store, new Object[] {issue});
                if (obj instanceof Models.Trade) {
                    return (Models.Trade) obj;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private Models.Decision getDecisionByTargetIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        String[] names =
                new String[] {
                    "getDecisionByTargetIssue",
                    "loadDecisionByTargetIssue",
                    "getLatestDecisionByTargetIssue"
                };
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {String.class});
                Object obj = m.invoke(store, new Object[] {issue});
                if (obj instanceof Models.Decision) {
                    return (Models.Decision) obj;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private Models.Draw getDrawByIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        String[] names =
                new String[] {"getDrawByIssue", "loadDrawByIssue", "getDraw", "findDrawByIssue"};
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {String.class});
                Object obj = m.invoke(store, new Object[] {issue});
                if (obj instanceof Models.Draw) {
                    return (Models.Draw) obj;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private boolean isBetDecision(Models.Decision d) {
        if (d == null) return false;
        return isBetText(getString(d, "decision", "")) || isBetText(getString(d, "action", ""));
    }

    private boolean isShadowDecision(Models.Decision d) {
        if (d == null) return false;
        String s = safe(getString(d, "decision", "")).trim();
        if (constEquals("DECISION_SHADOW", s)) return true;
        if ("SHADOW".equalsIgnoreCase(s) || "影子观察".equals(s)) return true;
        String action = safe(getString(d, "action", "")).trim();
        return "SHADOW".equalsIgnoreCase(action) || "影子观察".equals(action);
    }

    private boolean isRealBetTrade(Models.Trade t) {
        if (t == null) return false;
        if (!isRealScope(getString(t, "statScope", ""))) return false;

        String action = safe(getString(t, "action", ""));
        if (action.length() > 0 && !isBetText(action)) {
            return false;
        }
        if (!isValidPosition(getInt(t, "position", 0))) return false;
        if (!isValidNumber(getInt(t, "n1", 0))
                || !isValidNumber(getInt(t, "n2", 0))
                || !isValidNumber(getInt(t, "n3", 0))) return false;
        String issue = safe(getString(t, "targetIssue", ""));
        if (issue.length() == 0) return false;
        return true;
    }

    private boolean isRealScope(String scope) {
        String s = safe(scope).trim();
        if (s.length() == 0) return true;
        if (constEquals("STAT_SCOPE_REAL_TRADE", s)) return true;
        return "REAL".equalsIgnoreCase(s)
                || "REAL_TRADE".equalsIgnoreCase(s)
                || "REAL_BET".equalsIgnoreCase(s)
                || "REALBET".equalsIgnoreCase(s);
    }

    private boolean isBetText(String text) {
        String s = safe(text).trim();
        if (s.length() == 0) return false;
        if (constEquals("DECISION_BET", s)) return true;
        if (constEquals("ACTION_BET", s)) return true;
        return "BET".equalsIgnoreCase(s) || "出手".equals(s) || "下注".equals(s);
    }

    private boolean constEquals(String fieldName, String value) {
        if (fieldName == null || value == null) return false;
        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);
            return v != null && value.equals(String.valueOf(v));
        } catch (Throwable e) {
            return false;
        }
    }

    private String resultWin() {
        try {
            Field f = AppConfig.class.getField("RESULT_WIN");
            Object v = f.get(null);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return "WIN";
    }

    private String resultMiss() {
        try {
            Field f = AppConfig.class.getField("RESULT_MISS");
            Object v = f.get(null);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return "MISS";
    }

    private String resultWait() {
        try {
            Field f = AppConfig.class.getField("RESULT_WAIT");
            Object v = f.get(null);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return "WAIT";
    }

    private boolean isWinResult(String r) {
        String s = normalizeResult(r);
        return resultWin().equals(s)
                || "WIN".equalsIgnoreCase(s)
                || "HIT".equalsIgnoreCase(s)
                || "命中".equals(s);
    }

    private boolean isMissResult(String r) {
        String s = normalizeResult(r);
        return resultMiss().equals(s)
                || "MISS".equalsIgnoreCase(s)
                || "LOSE".equalsIgnoreCase(s)
                || "LOSS".equalsIgnoreCase(s)
                || "未中".equals(s)
                || "不中".equals(s);
    }

    private String normalizeResult(String r) {
        String s = safe(r).trim();
        if (s.length() == 0) return resultWait();
        String u = s.toUpperCase(java.util.Locale.ROOT);
        if ("WIN".equals(u) || "HIT".equals(u) || "命中".equals(s)) return resultWin();
        if ("MISS".equals(u)
                || "LOSE".equals(u)
                || "LOSS".equals(u)
                || "未中".equals(s)
                || "不中".equals(s)) return resultMiss();
        if ("WAIT".equals(u) || "WAITING".equals(u) || "等待".equals(s) || "等待结算".equals(s))
            return resultWait();
        try {
            Method m = AppConfig.class.getMethod("normalizeResult", new Class[] {String.class});
            Object obj = m.invoke(null, new Object[] {s});
            if (obj != null) return String.valueOf(obj);
        } catch (Throwable e) {
        }
        return s;
    }

    private boolean isValidPosition(int p) {
        try {
            Method m = AppConfig.class.getMethod("isValidPosition", new Class[] {int.class});
            Object obj = m.invoke(null, new Object[] {Integer.valueOf(p)});
            if (obj instanceof Boolean) {
                return ((Boolean) obj).booleanValue();
            }
        } catch (Throwable e) {
        }
        return p >= 1 && p <= POSITION_COUNT;
    }

    private boolean isValidNumber(int n) {
        try {
            Method m = AppConfig.class.getMethod("isValidNumber", new Class[] {int.class});
            Object obj = m.invoke(null, new Object[] {Integer.valueOf(n)});
            if (obj instanceof Boolean) {
                return ((Boolean) obj).booleanValue();
            }
        } catch (Throwable e) {
        }
        return n >= 1 && n <= 10;
    }

    private boolean isDrawValid(Models.Draw draw) {
        if (draw == null) return false;
        try {
            Method m = draw.getClass().getMethod("isValid", new Class[] {});
            Object obj = m.invoke(draw, new Object[] {});
            if (obj instanceof Boolean) {
                return ((Boolean) obj).booleanValue();
            }
        } catch (Throwable e) {
        }
        return true;
    }

    private int getNumberAtPositionSafe(Models.Draw draw, int position) {
        if (draw == null) return 0;
        try {
            Method m = draw.getClass().getMethod("getNumberAtPosition", new Class[] {int.class});
            Object obj = m.invoke(draw, new Object[] {Integer.valueOf(position)});
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            }
        } catch (Throwable e) {
        }
        return 0;
    }

    private boolean containsK3(SignalCase c, int actual) {
        if (c == null) return false;
        return actual == c.n1 || actual == c.n2 || actual == c.n3;
    }

    private void sortCasesDesc(ArrayList<SignalCase> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<SignalCase>() {
                    public int compare(SignalCase a, SignalCase b) {
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

    private String sampleBucket(SignalCase c) {
        if (c == null) return "Sample空";
        int sample =
                Math.max(
                        Math.max(c.effectiveSample, c.comboSample),
                        Math.max(c.similarWindowSample, c.teacherMatrixSample));
        if (c.sampleReliability >= 0.75d || sample >= 120) return "Sample强";
        if (c.sampleReliability >= 0.60d || sample >= 60) return "Sample可用";
        if (sample >= 30) return "Sample观察";
        return "Sample不足";
    }

    private String recentBucket(SignalCase c) {
        if (c == null) return "Recent空";
        if (c.recentValidity >= 0.75d) return "Recent强";
        if (c.recentValidity >= 0.60d) return "Recent可用";
        if (c.recentValidity > 0.0d) return "Recent弱";
        return "Recent空";
    }

    private String normalizeBucket(String s) {
        s = safe(s).trim();
        if (s.length() == 0) return "空";
        if (s.length() > 90) s = s.substring(0, 90);
        return s;
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

    private long getLong(Object obj, String field, long def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v != null) return Long.parseLong(String.valueOf(v));
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

    private double firstNonZero(double a, double b, double c) {
        if (a != 0.0d) return a;
        if (b != 0.0d) return b;
        return c;
    }

    private double firstNonZero(double a, double b, double c, double d) {
        if (a != 0.0d) return a;
        if (b != 0.0d) return b;
        if (c != 0.0d) return c;
        return d;
    }

    private int firstNonZeroInt(int a, int b, int c) {
        if (a != 0) return a;
        if (b != 0) return b;
        return c;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) return a;
        return b == null ? "" : b;
    }

    private static String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        return c == null ? "" : c;
    }

    private static String firstNonEmpty(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        if (c != null && c.length() > 0) return c;
        return d == null ? "" : d;
    }

    private static String append(String a, String b) {
        if (b == null || b.length() == 0) return safe(a);
        if (a == null || a.length() == 0) return b;
        return a + "；" + b;
    }

    private static String upper(String s) {
        return safe(s).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String limit(String text, int max) {
        if (text == null) return "";
        if (max <= 0 || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private static String jsonSafe(String s) {
        s = safe(s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') sb.append("\\\\");
            else if (ch == '"') sb.append("\\\"");
            else if (ch == '\n') sb.append("\\n");
            else if (ch == '\r') sb.append("\\r");
            else if (ch == '\t') sb.append("\\t");
            else sb.append(ch);
        }
        return sb.toString();
    }

    private static String pct(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
        }
    }

    private static String signedPct(double v) {
        try {
            return UiFormatter.signedProbability(v);
        } catch (Throwable e) {
            String s = String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
            return v >= 0.0d ? "+" + s : s;
        }
    }

    private static String money(double v) {
        try {
            return UiFormatter.money(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 100.0d) / 100.0d);
        }
    }

    private void setStateSafe(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) return;
        try {
            Method m =
                    store.getClass()
                            .getMethod("setState", new Class[] {String.class, String.class});
            m.invoke(store, new Object[] {key, value == null ? "" : value});
        } catch (Throwable e) {
        }
    }

    public static final class ModuleEffectReport {
        public int ok;
        public String version;
        public long createdAt;
        public int betLimit;
        public int shadowLimit;
        public ArrayList<SignalCase> betCases;
        public ArrayList<SignalCase> shadowCases;
        public ArrayList<ModuleEffect> effects;
        public HashMap<String, ModuleEffect> effectMap;

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

        public int shadowTotal;
        public int shadowHit;
        public int shadowMiss;
        public int shadowWait;
        public double shadowHitRate;

        public int effectiveCount;
        public int neutralCount;
        public int misleadingCount;
        public int insufficientCount;
        public int conflictCount;
        public int moduleSupportTotal;
        public int strictOosSample;
        public String researchEvidenceStatus;

        public int teacherWeakRelease;
        public int teacherRiskRelease;
        public int v20EmptyRelease;
        public int v20RiskRelease;
        public int windowUntrustedRelease;
        public int negativeEdgeRelease;
        public int gateBelowRelease;
        public int k3CoverageMiss;
        public int thirdNumberHit;
        public int thirdNumberMiss;
        public int sampleLowRelease;
        public int recentLowRelease;

        public ModuleEffect bestEffect;
        public ModuleEffect worstEffect;
        public String overallStatus;
        public String mainProblem;
        public String nextAdvice;
        public String summary;
        public String detail;

        public ModuleEffectReport() {
            ok = 0;
            version = "";
            createdAt = 0L;
            betLimit = 0;
            shadowLimit = 0;
            betCases = new ArrayList<SignalCase>();
            shadowCases = new ArrayList<SignalCase>();
            effects = new ArrayList<ModuleEffect>();
            effectMap = new HashMap<String, ModuleEffect>();
            overallStatus = "";
            researchEvidenceStatus = "UNAVAILABLE";
            mainProblem = "";
            nextAdvice = "";
            summary = "";
            detail = "";
        }

        public ModuleEffect getEffect(String moduleName) {
            if (moduleName == null) return null;
            return effectMap.get(moduleName);
        }

        public String getEffectStatus(String moduleName) {
            ModuleEffect e = getEffect(moduleName);
            return e == null ? STATUS_INSUFFICIENT : safe(e.effectStatus);
        }

        public double getEffectHitRate(String moduleName) {
            ModuleEffect e = getEffect(moduleName);
            return e == null ? 0.0d : e.hitRate;
        }

        public double getEffectReliability(String moduleName) {
            ModuleEffect e = getEffect(moduleName);
            return e == null ? 0.0d : e.reliability;
        }

        public boolean isModuleMisleading(String moduleName) {
            ModuleEffect e = getEffect(moduleName);
            return e != null && STATUS_MISLEADING.equals(e.effectStatus);
        }

        public boolean isModuleEffective(String moduleName) {
            ModuleEffect e = getEffect(moduleName);
            return e != null && STATUS_EFFECTIVE.equals(e.effectStatus);
        }
    }

    public static final class ModuleEffect {
        public String moduleName;
        public String description;
        public int supportCount;
        public int betCount;
        public int hitCount;
        public int missCount;
        public double hitRate;
        public double sumFinalP;
        public double sumGate;
        public double sumEdge;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public double profit;
        public double realEdge;

        public int recentCount;
        public int recentHit;
        public double recentHitRate;
        public double recentEffectiveness;

        public int shadowSupportCount;
        public int shadowWouldHit;
        public int shadowWouldMiss;
        public double shadowWouldHitRate;
        public double virtualEdge;

        public double supportRate;
        public double reliability;
        public String effectStatus;
        public String problem;
        public String fixAdvice;
        public String sampleWarning;
        public String mainBucket;
        public String worstBucket;
        public int weakSupportCount;
        public int falseStrongCount;
        public int riskReleaseCount;

        public ArrayList<EffectBucket> buckets;
        public ArrayList<EffectBucket> shadowBuckets;
        public HashMap<String, EffectBucket> bucketMap;
        public HashMap<String, EffectBucket> shadowBucketMap;
        public ArrayList<ModuleSignalCase> sampleCases;

        public ModuleEffect() {
            moduleName = "";
            description = "";
            effectStatus = STATUS_INSUFFICIENT;
            problem = "";
            fixAdvice = "";
            sampleWarning = "";
            mainBucket = "";
            worstBucket = "";
            buckets = new ArrayList<EffectBucket>();
            shadowBuckets = new ArrayList<EffectBucket>();
            bucketMap = new HashMap<String, EffectBucket>();
            shadowBucketMap = new HashMap<String, EffectBucket>();
            sampleCases = new ArrayList<ModuleSignalCase>();
        }

        public String shortText() {
            return betCount + "单/中" + hitCount + "/胜率" + pct(hitRate) + "/状态" + effectStatus;
        }

        public String fullText() {
            return moduleName
                    + "：状态="
                    + effectStatus
                    + "，真实BET="
                    + betCount
                    + "，中"
                    + hitCount
                    + "，挂"
                    + missCount
                    + "，胜率="
                    + pct(hitRate)
                    + "，真实Edge="
                    + signedPct(realEdge)
                    + "，均P="
                    + pct(avgFinalP)
                    + "，均Gate="
                    + pct(avgGate)
                    + "，均Edge="
                    + signedPct(avgEdge)
                    + "，盈亏="
                    + money(profit)
                    + "，近期="
                    + recentCount
                    + "/"
                    + recentHit
                    + "/"
                    + pct(recentHitRate)
                    + "，SHADOW="
                    + shadowSupportCount
                    + "/"
                    + shadowWouldHit
                    + "/"
                    + pct(shadowWouldHitRate)
                    + "，可靠度="
                    + pct(reliability);
        }
    }

    public static final class EffectBucket {
        public String name;
        public int total;
        public int hit;
        public int miss;
        public double pnl;
        public double sumFinalP;
        public double sumEdge;

        public EffectBucket(String n) {
            name = n == null ? "" : n;
        }

        public void add(SignalCase c) {
            if (c == null) return;
            total++;
            if (c.hit == 1
                    || "WIN".equalsIgnoreCase(c.result)
                    || "HIT".equalsIgnoreCase(c.result)
                    || "命中".equals(c.result)) {
                hit++;
            } else {
                miss++;
            }
            pnl += c.pnl;
            sumFinalP += c.finalP;
            sumEdge += c.edge;
        }

        public double rate() {
            if (total <= 0) return 0.0d;
            return ((double) hit) / ((double) total);
        }

        public double avgP() {
            if (total <= 0) return 0.0d;
            return sumFinalP / (double) total;
        }

        public double avgEdge() {
            if (total <= 0) return 0.0d;
            return sumEdge / (double) total;
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
                    + pct(rate())
                    + "，均P="
                    + pct(avgP())
                    + "，均Edge="
                    + signedPct(avgEdge())
                    + "，盈亏="
                    + money(pnl);
        }
    }

    public static final class ModuleSignalCase {
        public String kind;
        public String decisionQualityPath;
        public String betDomain;
        public int teacherRealSample;
        public int teacherShadowSample;
        public int dynamicRealSample;
        public int dynamicShadowSample;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public String result;
        public int hit;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public String teacherStatus;
        public String v20Status;
        public String windowStateKey;
        public String bucket;
        public String note;
        public int shadow;
        public String reason;

        public ModuleSignalCase() {
            kind = "";
            decisionQualityPath = "";
            betDomain = "";
            targetIssue = "";
            result = "";
            teacherStatus = "";
            v20Status = "";
            windowStateKey = "";
            bucket = "";
            note = "";
            reason = "";
        }
    }

    public static final class SignalCase {
        public String kind;
        public String decisionQualityPath;
        public String betDomain;
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
        public String sampleBucket;
        public String recentBucket;

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
        public String caseReason;

        public SignalCase() {
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
            sampleBucket = "";
            recentBucket = "";
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
            caseReason = "";
        }
    }
}
