package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * DynamicFreezeMatrixEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环升级 · 第5/14
 *
 * <p>新增核心引擎：动态冻结矩阵引擎。
 *
 * <p>作用： 1. 读取真实BET、SHADOW反事实、模块评分、SHADOW晋级结果； 2. 识别“看起来强但真实亏”的假强状态； 3. 生成冻结矩阵，给后续
 * AdaptiveLearningJudgeEngine / DecisionGateEngine 使用； 4.
 * 冻结对象包括位置、窗口状态、Teacher状态、V20状态、FinalP区间、Edge区间、 K3层级、错因、模块标签、组合域； 5. 冻结不是永久的，通过SHADOW恢复验证达标后可以解冻。
 *
 * <p>红线： 1. 不出号； 2. 不结算； 3. 不创建Trade； 4. 不保存BET； 5. 不修改Decision； 6. 不写真实盈亏； 7. 不把SHADOW转BET； 8.
 * 不绕过DecisionGateEngine。
 *
 * <p>冻结流程： 连续误导 / 真实BET低效 / 模块误导 / SHADOW噪声 → 冻结真实BET → 只允许SHADOW观察 → SHADOW恢复验证 → 达标后解冻
 *
 * <p>Java7 / AIDE 兼容：无 lambda、无 stream、无第三方库。
 */
public final class DynamicFreezeMatrixEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final String FREEZE_POSITION = "POSITION_FREEZE";
    public static final String FREEZE_STATE = "STATE_FREEZE";
    public static final String FREEZE_TEACHER = "TEACHER_FREEZE";
    public static final String FREEZE_V20 = "V20_FREEZE";
    public static final String FREEZE_PROB_BUCKET = "PROB_BUCKET_FREEZE";
    public static final String FREEZE_EDGE_BUCKET = "EDGE_BUCKET_FREEZE";
    public static final String FREEZE_K3_LAYER = "K3_LAYER_FREEZE";
    public static final String FREEZE_MISS_REASON = "MISS_REASON_FREEZE";
    public static final String FREEZE_MODULE = "MODULE_FREEZE";
    public static final String FREEZE_COMBO_DOMAIN = "COMBO_DOMAIN_FREEZE";

    public static final String LEVEL_NONE = "NONE";
    public static final String LEVEL_WATCH = "WATCH";
    public static final String LEVEL_SOFT = "SOFT_FREEZE";
    public static final String LEVEL_HARD = "HARD_FREEZE";
    public static final String LEVEL_RECOVERING = "RECOVERING";

    public static final String ACTION_PASS = "PASS";
    public static final String ACTION_WATCH = "WATCH";
    public static final String ACTION_DOWNGRADE_TO_SHADOW = "DOWNGRADE_TO_SHADOW";
    public static final String ACTION_FREEZE = "FREEZE";

    private static final int DEFAULT_BET_LIMIT = 360;
    private static final int DEFAULT_SHADOW_LIMIT = 480;
    private static final int DEFAULT_DECISION_SCAN_LIMIT = 1000;
    private static final int POSITION_COUNT = 10;
    private static final int MIN_FREEZE_SAMPLE = 12;
    private static final int MIN_HARD_FREEZE_SAMPLE = 24;
    private static final int MIN_RECOVER_SAMPLE = 12;
    private static final int MIN_RECOVER_HITS = 5;
    private static final int DEFAULT_FREEZE_ISSUES_SOFT = 80;
    private static final int DEFAULT_FREEZE_ISSUES_HARD = 180;
    private static final long DEFAULT_FREEZE_TIME_SOFT_MS = 90L * 60L * 1000L;
    private static final long DEFAULT_FREEZE_TIME_HARD_MS = 240L * 60L * 1000L;
    private static final int MAX_REPORT_LEN = 20000;
    private static final int MAX_SAMPLE_CASES = 16;

    private static final double BASE_K3_RATE = 0.300d;
    private static final double SOFT_FREEZE_RATE = 0.270d;
    private static final double HARD_FREEZE_RATE = 0.240d;
    private static final double WATCH_RATE = 0.320d;
    private static final double RECOVER_RATE = 0.380d;
    private static final double STRONG_RECOVER_RATE = 0.400d;

    public DynamicFreezeMatrixEngine() {}

    public FreezeReport buildFreezeReport(DbStore store) {
        return buildFreezeReport(store, DEFAULT_BET_LIMIT, DEFAULT_SHADOW_LIMIT, null, null, null);
    }

    public FreezeReport buildFreezeReport(DbStore store, int betLimit, int shadowLimit) {
        return buildFreezeReport(store, betLimit, shadowLimit, null, null, null);
    }

    public FreezeReport buildFreezeReport(
            DbStore store,
            int betLimit,
            int shadowLimit,
            Object moduleEffectReport,
            Object shadowPromotionReport,
            Object diagnosisReport) {
        FreezeReport report = new FreezeReport();
        report.version = VERSION;
        report.createdAt = System.currentTimeMillis();

        if (betLimit <= 0) betLimit = DEFAULT_BET_LIMIT;
        if (shadowLimit <= 0) shadowLimit = DEFAULT_SHADOW_LIMIT;
        report.betLimit = betLimit;
        report.shadowLimit = shadowLimit;

        if (store == null) {
            report.ok = 0;
            report.summary = "动态冻结矩阵失败：DbStore为空";
            report.detail = report.summary;
            return report;
        }

        ArrayList<FreezeCase> betCases = loadRecentRealBetCases(store, betLimit);
        ArrayList<FreezeCase> shadowCases = loadRecentShadowCases(store, shadowLimit);

        report.ok = 1;
        report.betCases = betCases;
        report.shadowCases = shadowCases;

        analyzeRealBetCases(report, betCases);
        analyzeShadowRecovery(report, shadowCases);
        mergeModuleEffectReport(report, moduleEffectReport);
        mergeShadowPromotionReport(report, shadowPromotionReport);
        mergeDiagnosisReport(report, diagnosisReport);
        finalizeFreezeRules(report);
        buildReportText(report);

        return report;
    }

    public FreezeDecision checkCandidate(DbStore store, Object candidate) {
        FreezeReport report = buildFreezeReport(store);
        return checkCandidate(report, candidate);
    }

    public FreezeDecision checkCandidate(FreezeReport report, Object candidate) {
        FreezeDecision decision = new FreezeDecision();
        decision.version = VERSION;
        decision.createdAt = System.currentTimeMillis();

        if (report == null || candidate == null) {
            decision.frozen = 0;
            decision.freezeLevel = LEVEL_NONE;
            decision.action = ACTION_PASS;
            decision.freezeReason = "冻结矩阵或候选为空";
            return decision;
        }

        CandidateSnapshot snap = CandidateSnapshot.fromObject(candidate);
        return checkCandidate(report, snap);
    }

    public FreezeDecision checkCandidate(FreezeReport report, CandidateSnapshot snap) {
        FreezeDecision decision = new FreezeDecision();
        decision.version = VERSION;
        decision.createdAt = System.currentTimeMillis();

        if (report == null || snap == null) {
            decision.frozen = 0;
            decision.freezeLevel = LEVEL_NONE;
            decision.action = ACTION_PASS;
            decision.freezeReason = "冻结矩阵或候选为空";
            return decision;
        }

        String[] keys = buildCandidateFreezeKeys(snap);
        FreezeRule strongest = null;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (key == null || key.length() == 0) continue;
            FreezeRule r = report.ruleMap.get(key);
            if (r == null) continue;
            if (strongest == null
                    || freezeLevelRank(r.freezeLevel) > freezeLevelRank(strongest.freezeLevel)) {
                strongest = r;
            }
        }

        if (strongest == null) {
            decision.frozen = 0;
            decision.freezeLevel = LEVEL_NONE;
            decision.action = ACTION_PASS;
            decision.freezeReason = "未命中冻结矩阵";
            decision.freezeKey = "";
            decision.downgradeToShadow = 0;
            decision.recoverHint = "无需恢复验证";
            return decision;
        }

        decision.frozen = 1;
        decision.freezeType = strongest.freezeType;
        decision.freezeLevel = strongest.freezeLevel;
        decision.freezeKey = strongest.freezeKey;
        decision.freezeReason = strongest.freezeReason;
        decision.freezeUntilIssue = strongest.freezeUntilIssue;
        decision.freezeUntilTime = strongest.freezeUntilTime;
        decision.shadowReviewCount = strongest.shadowReviewCount;
        decision.recoverNeedHits = strongest.recoverNeedHits;
        decision.recoverNeedRate = strongest.recoverNeedRate;
        decision.recoverHint = strongest.recoverHint;

        if (LEVEL_HARD.equals(strongest.freezeLevel)) {
            decision.action = ACTION_FREEZE;
            decision.downgradeToShadow = 1;
        } else if (LEVEL_SOFT.equals(strongest.freezeLevel)
                || LEVEL_RECOVERING.equals(strongest.freezeLevel)) {
            decision.action = ACTION_DOWNGRADE_TO_SHADOW;
            decision.downgradeToShadow = 1;
        } else {
            decision.action = ACTION_WATCH;
            decision.downgradeToShadow = 0;
        }

        return decision;
    }

    public String buildFreezeSummary(DbStore store) {
        FreezeReport r = buildFreezeReport(store);
        return r == null ? "" : safe(r.summary);
    }

    public String buildFreezeDetail(DbStore store) {
        FreezeReport r = buildFreezeReport(store);
        return r == null ? "" : safe(r.detail);
    }

    public String buildFreezeCopyText(DbStore store) {
        FreezeReport r = buildFreezeReport(store);
        if (r == null) return "V20动态冻结矩阵为空";
        return safe(r.detail);
    }

    public FreezeReport saveFreezeState(DbStore store) {
        FreezeReport r = buildFreezeReport(store);
        saveFreezeState(store, r);
        return r;
    }

    public void saveFreezeState(DbStore store, FreezeReport report) {
        if (store == null || report == null) {
            return;
        }
        setStateSafe(store, "dynamic_freeze_matrix_version", safe(report.version));
        setStateSafe(store, "dynamic_freeze_matrix_updated_at", String.valueOf(report.createdAt));
        setStateSafe(store, "dynamic_freeze_matrix_status", report.ok == 1 ? "OK" : "ERROR");
        setStateSafe(
                store,
                "dynamic_freeze_matrix_rule_count",
                String.valueOf(report.freezeRules.size()));
        setStateSafe(
                store, "dynamic_freeze_matrix_hard_count", String.valueOf(report.hardFreezeCount));
        setStateSafe(
                store, "dynamic_freeze_matrix_soft_count", String.valueOf(report.softFreezeCount));
        setStateSafe(store, "dynamic_freeze_matrix_watch_count", String.valueOf(report.watchCount));
        setStateSafe(
                store,
                "dynamic_freeze_matrix_recovering_count",
                String.valueOf(report.recoveringCount));
        setStateSafe(store, "dynamic_freeze_matrix_main_problem", safe(report.mainProblem));
        setStateSafe(store, "dynamic_freeze_matrix_next_advice", safe(report.nextAdvice));
        setStateSafe(store, "dynamic_freeze_matrix_summary", safe(report.summary));
        setStateSafe(store, "dynamic_freeze_matrix_detail", safe(report.detail));
        setStateSafe(store, "dynamic_freeze_matrix_compact_json", safe(buildCompactJson(report)));
    }

    private void analyzeRealBetCases(FreezeReport report, ArrayList<FreezeCase> cases) {
        if (report == null || cases == null) return;

        for (int i = 0; i < cases.size(); i++) {
            FreezeCase c = cases.get(i);
            if (c == null || !"BET".equals(c.kind)) continue;
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
                if (isMissResult(c.result)) {
                    report.consecutiveRecentMiss++;
                }
            }
            report.pnl += c.pnl;
            report.avgFinalP += c.finalP;
            report.avgGate += c.dynamicGate;
            report.avgEdge += c.edge;

            addDomain(report, FREEZE_POSITION, "P" + c.position, c, "位置近期低效");
            addDomain(report, FREEZE_STATE, c.windowBucket, c, "WindowState低效");
            addDomain(report, FREEZE_TEACHER, c.teacherBucket, c, "Teacher状态低效");
            addDomain(report, FREEZE_V20, c.v20Bucket, c, "V20因链状态低效");
            addDomain(report, FREEZE_PROB_BUCKET, c.finalPBucket, c, "FinalP区间低效");
            addDomain(report, FREEZE_EDGE_BUCKET, c.edgeBucket, c, "Edge区间低效");
            addDomain(report, FREEZE_K3_LAYER, k3LayerBucket(c), c, "K3层级低效");
            addDomain(report, FREEZE_COMBO_DOMAIN, comboDomainKey(c), c, "组合域低效");

            if (c.missReason != null && c.missReason.length() > 0) {
                addDomain(report, FREEZE_MISS_REASON, c.missBucket, c, "错因集中");
            }

            addModuleDomains(report, c);

            if (c.edge < 0.0d) report.negativeEdgeBet++;
            if (isTeacherRisk(c)) report.teacherRiskBet++;
            if (isTeacherWeakOrEmpty(c)) report.teacherWeakBet++;
            if (hasV20Risk(c)) report.v20RiskBet++;
            if (!hasV20Support(c)) report.v20EmptyOrWeakBet++;
            if (!hasTrustedWindow(c)) report.windowUntrustedBet++;
            if (c.finalP >= 0.400d && c.hit == 0) report.highPFail++;
        }

        if (report.totalBet > 0) {
            report.winRate = ((double) report.hitBet) / ((double) report.totalBet);
            report.avgFinalP = report.avgFinalP / (double) report.totalBet;
            report.avgGate = report.avgGate / (double) report.totalBet;
            report.avgEdge = report.avgEdge / (double) report.totalBet;
        }
    }

    private void analyzeShadowRecovery(FreezeReport report, ArrayList<FreezeCase> cases) {
        if (report == null || cases == null) return;

        for (int i = 0; i < cases.size(); i++) {
            FreezeCase c = cases.get(i);
            if (c == null || !"SHADOW".equals(c.kind)) continue;

            report.totalShadow++;
            if (c.settled != 1) {
                report.waitShadow++;
                continue;
            }

            report.settledShadow++;
            if (c.hit == 1 || isWinResult(c.result)) {
                report.shadowHit++;
            } else {
                report.shadowMiss++;
            }

            addShadowRecovery(report, FREEZE_POSITION, "P" + c.position, c);
            addShadowRecovery(report, FREEZE_STATE, c.windowBucket, c);
            addShadowRecovery(report, FREEZE_TEACHER, c.teacherBucket, c);
            addShadowRecovery(report, FREEZE_V20, c.v20Bucket, c);
            addShadowRecovery(report, FREEZE_PROB_BUCKET, c.finalPBucket, c);
            addShadowRecovery(report, FREEZE_EDGE_BUCKET, c.edgeBucket, c);
            addShadowRecovery(report, FREEZE_K3_LAYER, k3LayerBucket(c), c);
            addShadowRecovery(report, FREEZE_COMBO_DOMAIN, comboDomainKey(c), c);
            if (c.missReason != null && c.missReason.length() > 0) {
                addShadowRecovery(report, FREEZE_MISS_REASON, c.missBucket, c);
            }
            addShadowModuleRecovery(report, c);
        }

        if (report.settledShadow > 0) {
            report.shadowHitRate = ((double) report.shadowHit) / ((double) report.settledShadow);
        }
    }

    private void addModuleDomains(FreezeReport report, FreezeCase c) {
        if (report == null || c == null) return;

        if (c.finalP >= 0.400d) {
            addDomain(report, FREEZE_MODULE, "ProbabilityEngine:HIGH_P_FAIL", c, "高FinalP失败");
        }
        if (isTeacherRisk(c) || isTeacherWeakOrEmpty(c)) {
            addDomain(
                    report,
                    FREEZE_MODULE,
                    "TeacherNumberMatrixEngine:WEAK_RISK_RELEASE",
                    c,
                    "Teacher弱/风险仍BET");
        }
        if (!hasV20Support(c) || hasV20Risk(c)) {
            addDomain(
                    report,
                    FREEZE_MODULE,
                    "DynamicCauseChainEngine:EMPTY_RISK_RELEASE",
                    c,
                    "V20空/风险仍BET");
        }
        if (!hasTrustedWindow(c)) {
            addDomain(
                    report,
                    FREEZE_MODULE,
                    "WindowState:UNTRUSTED_RELEASE",
                    c,
                    "WindowState样本不可信仍BET");
        }
        if (c.edge < 0.0d) {
            addDomain(
                    report,
                    FREEZE_MODULE,
                    "DynamicGateEngine:NEGATIVE_EDGE_RELEASE",
                    c,
                    "负Edge仍BET");
        }
    }

    private void addShadowModuleRecovery(FreezeReport report, FreezeCase c) {
        if (report == null || c == null) return;
        if (c.finalP >= 0.400d) {
            addShadowRecovery(report, FREEZE_MODULE, "ProbabilityEngine:HIGH_P_FAIL", c);
        }
        if (isTeacherRisk(c) || isTeacherWeakOrEmpty(c)) {
            addShadowRecovery(
                    report, FREEZE_MODULE, "TeacherNumberMatrixEngine:WEAK_RISK_RELEASE", c);
        }
        if (!hasV20Support(c) || hasV20Risk(c)) {
            addShadowRecovery(
                    report, FREEZE_MODULE, "DynamicCauseChainEngine:EMPTY_RISK_RELEASE", c);
        }
        if (!hasTrustedWindow(c)) {
            addShadowRecovery(report, FREEZE_MODULE, "WindowState:UNTRUSTED_RELEASE", c);
        }
        if (c.edge < 0.0d) {
            addShadowRecovery(report, FREEZE_MODULE, "DynamicGateEngine:NEGATIVE_EDGE_RELEASE", c);
        }
    }

    private void addDomain(
            FreezeReport report, String type, String value, FreezeCase c, String reason) {
        if (report == null || c == null) return;
        value = normalizeBucket(value);
        if (value.length() == 0 || "空".equals(value)) {
            if (FREEZE_STATE.equals(type)
                    || FREEZE_TEACHER.equals(type)
                    || FREEZE_V20.equals(type)) {
                value = "空";
            } else {
                return;
            }
        }

        String key = buildFreezeKey(type, value);
        FreezeDomainStats d = report.domainMap.get(key);
        if (d == null) {
            d = new FreezeDomainStats();
            d.freezeType = type;
            d.domainValue = value;
            d.freezeKey = key;
            d.reasonSeed = safe(reason);
            report.domainMap.put(key, d);
        }
        d.addReal(c);
        if (d.sampleCases.size() < MAX_SAMPLE_CASES) {
            d.sampleCases.add(copyBrief(c));
        }
    }

    private void addShadowRecovery(FreezeReport report, String type, String value, FreezeCase c) {
        if (report == null || c == null) return;
        value = normalizeBucket(value);
        if (value.length() == 0 || "空".equals(value)) {
            if (FREEZE_STATE.equals(type)
                    || FREEZE_TEACHER.equals(type)
                    || FREEZE_V20.equals(type)) {
                value = "空";
            } else {
                return;
            }
        }

        String key = buildFreezeKey(type, value);
        FreezeDomainStats d = report.domainMap.get(key);
        if (d == null) {
            d = new FreezeDomainStats();
            d.freezeType = type;
            d.domainValue = value;
            d.freezeKey = key;
            d.reasonSeed = "SHADOW恢复观察";
            report.domainMap.put(key, d);
        }
        d.addShadow(c);
    }

    private void mergeModuleEffectReport(FreezeReport report, Object moduleEffectReport) {
        if (report == null || moduleEffectReport == null) return;

        Object effectsObj = getObject(moduleEffectReport, "effects", null);
        if (!(effectsObj instanceof List)) return;

        List list = (List) effectsObj;
        for (int i = 0; i < list.size(); i++) {
            Object e = list.get(i);
            if (e == null) continue;
            String moduleName = getString(e, "moduleName", "");
            String status = getString(e, "effectStatus", "");
            int betCount = getInt(e, "betCount", 0);
            double hitRate = getDouble(e, "hitRate", 0.0d);
            double reliability = getDouble(e, "reliability", 0.0d);
            String problem = getString(e, "problem", "");

            if ("MISLEADING".equals(status) && betCount >= MIN_FREEZE_SAMPLE) {
                FreezeDomainStats d =
                        forceDomain(report, FREEZE_MODULE, moduleName + ":MISLEADING");
                d.externalMisleading = 1;
                d.externalSample = betCount;
                d.externalHitRate = hitRate;
                d.externalReliability = reliability;
                d.externalProblem = problem;
            } else if ("CONFLICT".equals(status) && betCount >= MIN_FREEZE_SAMPLE) {
                FreezeDomainStats d = forceDomain(report, FREEZE_MODULE, moduleName + ":CONFLICT");
                d.externalConflict = 1;
                d.externalSample = betCount;
                d.externalHitRate = hitRate;
                d.externalReliability = reliability;
                d.externalProblem = problem;
            }
        }
    }

    private void mergeShadowPromotionReport(FreezeReport report, Object shadowPromotionReport) {
        if (report == null || shadowPromotionReport == null) return;

        Object blockedObj = getObject(shadowPromotionReport, "blockedDomains", null);
        if (blockedObj instanceof List) {
            List list = (List) blockedObj;
            for (int i = 0; i < list.size(); i++) {
                Object d0 = list.get(i);
                if (d0 == null) continue;
                String domainKey = getString(d0, "domainKey", "");
                String domainType = getString(d0, "domainType", "");
                int total = getInt(d0, "total", 0);
                double rate = getDouble(d0, "wouldHitRate", 0.0d);
                double reliability = getDouble(d0, "reliability", 0.0d);
                if (domainKey.length() == 0) continue;

                FreezeDomainStats d =
                        forceDomain(
                                report,
                                FREEZE_COMBO_DOMAIN,
                                "SHADOW_BLOCK:" + domainType + ":" + domainKey);
                d.shadowBlocked = 1;
                d.shadowBlockedSample = total;
                d.shadowBlockedHitRate = rate;
                d.shadowBlockedReliability = reliability;
                d.reasonSeed = "SHADOW反事实BLOCK噪声域";
            }
        }

        Object promotedObj = getObject(shadowPromotionReport, "promotedDomains", null);
        if (promotedObj instanceof List) {
            List list = (List) promotedObj;
            for (int i = 0; i < list.size(); i++) {
                Object d0 = list.get(i);
                if (d0 == null) continue;
                String domainKey = getString(d0, "domainKey", "");
                String domainType = getString(d0, "domainType", "");
                int total = getInt(d0, "total", 0);
                double rate = getDouble(d0, "wouldHitRate", 0.0d);
                if (domainKey.length() == 0) continue;

                FreezeDomainStats d =
                        forceDomain(
                                report,
                                FREEZE_COMBO_DOMAIN,
                                "SHADOW_RECOVER:" + domainType + ":" + domainKey);
                d.shadowPromoted = 1;
                d.shadowPromotedSample = total;
                d.shadowPromotedHitRate = rate;
                d.reasonSeed = "SHADOW反事实PROMOTE_WATCH恢复域";
            }
        }
    }

    private void mergeDiagnosisReport(FreezeReport report, Object diagnosisReport) {
        if (report == null || diagnosisReport == null) return;

        String mainProblem = getString(diagnosisReport, "mainProblem", "");
        String nextFix = getString(diagnosisReport, "nextFixAdvice", "");
        if (mainProblem.length() > 0) {
            report.externalDiagnosisProblem = mainProblem;
        }
        if (nextFix.length() > 0) {
            report.externalDiagnosisAdvice = nextFix;
        }

        Object missStatsObj = getObject(diagnosisReport, "missStats", null);
        if (missStatsObj instanceof HashMap) {
            HashMap map = (HashMap) missStatsObj;
            for (Object keyObj : map.keySet()) {
                Object bucket = map.get(keyObj);
                if (bucket == null) continue;
                String name = getString(bucket, "name", String.valueOf(keyObj));
                int total = getInt(bucket, "total", 0);
                int miss = getInt(bucket, "miss", 0);
                if (total >= MIN_FREEZE_SAMPLE && miss >= total * 2 / 3) {
                    FreezeDomainStats d = forceDomain(report, FREEZE_MISS_REASON, name);
                    d.externalMisleading = 1;
                    d.externalSample = total;
                    d.externalProblem = "诊断错因集中：" + name;
                }
            }
        }
    }

    private FreezeDomainStats forceDomain(FreezeReport report, String type, String value) {
        value = normalizeBucket(value);
        String key = buildFreezeKey(type, value);
        FreezeDomainStats d = report.domainMap.get(key);
        if (d == null) {
            d = new FreezeDomainStats();
            d.freezeType = type;
            d.domainValue = value;
            d.freezeKey = key;
            d.reasonSeed = "外部报告触发";
            report.domainMap.put(key, d);
        }
        return d;
    }

    private void finalizeFreezeRules(FreezeReport report) {
        if (report == null) return;

        report.freezeRules.clear();
        report.ruleMap.clear();

        for (String key : report.domainMap.keySet()) {
            FreezeDomainStats d = report.domainMap.get(key);
            if (d == null) continue;
            finalizeDomainStats(d);
            FreezeRule rule = buildRuleFromDomain(report, d);
            if (rule != null) {
                report.freezeRules.add(rule);
                report.ruleMap.put(rule.freezeKey, rule);
                if (LEVEL_HARD.equals(rule.freezeLevel)) report.hardFreezeCount++;
                else if (LEVEL_SOFT.equals(rule.freezeLevel)) report.softFreezeCount++;
                else if (LEVEL_RECOVERING.equals(rule.freezeLevel)) report.recoveringCount++;
                else if (LEVEL_WATCH.equals(rule.freezeLevel)) report.watchCount++;
            }
        }

        sortRules(report.freezeRules);
        buildMainProblemAndAdvice(report);
    }

    private void finalizeDomainStats(FreezeDomainStats d) {
        if (d == null) return;

        if (d.realCount > 0) {
            d.realHitRate = ((double) d.realHit) / ((double) d.realCount);
            d.avgFinalP = d.sumFinalP / (double) d.realCount;
            d.avgGate = d.sumGate / (double) d.realCount;
            d.avgEdge = d.sumEdge / (double) d.realCount;
        } else {
            d.realHitRate = 0.0d;
            d.avgFinalP = 0.0d;
            d.avgGate = 0.0d;
            d.avgEdge = 0.0d;
        }

        if (d.shadowCount > 0) {
            d.shadowHitRate = ((double) d.shadowHit) / ((double) d.shadowCount);
        } else {
            d.shadowHitRate = 0.0d;
        }

        d.realEdge = d.realHitRate - BASE_K3_RATE;
        d.shadowEdge = d.shadowHitRate - BASE_K3_RATE;
        d.reliability = computeDomainReliability(d);
        d.recoverReady = isRecoverReady(d) ? 1 : 0;
    }

    private double computeDomainReliability(FreezeDomainStats d) {
        if (d == null) return 0.0d;

        double realScore;
        if (d.realCount >= 80) realScore = 1.0d;
        else if (d.realCount >= 40) realScore = 0.78d;
        else if (d.realCount >= 24) realScore = 0.62d;
        else if (d.realCount >= 12) realScore = 0.42d;
        else realScore = 0.16d;

        double shadowScore;
        if (d.shadowCount >= 80) shadowScore = 0.82d;
        else if (d.shadowCount >= 40) shadowScore = 0.64d;
        else if (d.shadowCount >= 12) shadowScore = 0.38d;
        else if (d.shadowCount > 0) shadowScore = 0.16d;
        else shadowScore = 0.0d;

        double externalScore = 0.0d;
        if (d.externalMisleading > 0 || d.shadowBlocked > 0) externalScore = 0.35d;
        if (d.externalReliability > externalScore) externalScore = d.externalReliability;

        double score = realScore * 0.62d + shadowScore * 0.20d + externalScore * 0.18d;
        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private boolean isRecoverReady(FreezeDomainStats d) {
        if (d == null) return false;
        if (d.shadowCount < MIN_RECOVER_SAMPLE) return false;
        if (d.shadowHit < MIN_RECOVER_HITS) return false;
        if (d.shadowHitRate < RECOVER_RATE) return false;
        if (d.shadowHitRate >= STRONG_RECOVER_RATE) return true;
        if (d.realHitRate < HARD_FREEZE_RATE && d.shadowHitRate >= STRONG_RECOVER_RATE) return true;
        return d.realCount < MIN_FREEZE_SAMPLE;
    }

    private FreezeRule buildRuleFromDomain(FreezeReport report, FreezeDomainStats d) {
        if (d == null) return null;

        String level = LEVEL_NONE;
        String reason = "";
        String advice = "";
        int freezeIssues = 0;
        long freezeTimeMs = 0L;

        if (d.recoverReady == 1) {
            level = LEVEL_RECOVERING;
            reason = "SHADOW恢复验证达标，进入恢复观察，不直接解冻真实BET。";
            advice = "保持小心，下一步由AdaptiveLearningJudgeEngine按PASS/WATCH判定，不能直接恢复无审判BET。";
            freezeIssues = DEFAULT_FREEZE_ISSUES_SOFT / 2;
            freezeTimeMs = DEFAULT_FREEZE_TIME_SOFT_MS / 2L;
        } else if ((d.realCount >= MIN_HARD_FREEZE_SAMPLE && d.realHitRate <= HARD_FREEZE_RATE)
                || d.externalMisleading > 0
                || d.shadowBlocked > 0) {
            level = LEVEL_HARD;
            reason = "真实BET同域低于硬冻结线或外部报告标记误导/BLOCK。";
            advice =
                    "禁止真实BET，只允许SHADOW观察；恢复需SHADOW样本>="
                            + MIN_RECOVER_SAMPLE
                            + "且命中率>="
                            + pct(RECOVER_RATE)
                            + "。";
            freezeIssues = DEFAULT_FREEZE_ISSUES_HARD;
            freezeTimeMs = DEFAULT_FREEZE_TIME_HARD_MS;
        } else if (d.realCount >= MIN_FREEZE_SAMPLE && d.realHitRate <= SOFT_FREEZE_RATE) {
            level = LEVEL_SOFT;
            reason = "真实BET同域低于软冻结线，疑似假强。";
            advice = "降级到SHADOW观察，等待恢复验证。";
            freezeIssues = DEFAULT_FREEZE_ISSUES_SOFT;
            freezeTimeMs = DEFAULT_FREEZE_TIME_SOFT_MS;
        } else if (d.realCount >= MIN_FREEZE_SAMPLE && d.realHitRate <= WATCH_RATE) {
            level = LEVEL_WATCH;
            reason = "真实BET同域不够强，进入观察名单。";
            advice = "不得作为单独BET理由，后续审判需附加证据。";
            freezeIssues = DEFAULT_FREEZE_ISSUES_SOFT / 3;
            freezeTimeMs = DEFAULT_FREEZE_TIME_SOFT_MS / 3L;
        } else {
            return null;
        }

        FreezeRule r = new FreezeRule();
        r.freezeType = d.freezeType;
        r.freezeKey = d.freezeKey;
        r.freezeValue = d.domainValue;
        r.freezeLevel = level;
        r.freezeReason = reason + " 原因种子=" + safe(d.reasonSeed);
        r.actionAdvice = advice;
        r.realCount = d.realCount;
        r.realHit = d.realHit;
        r.realMiss = d.realMiss;
        r.realHitRate = d.realHitRate;
        r.shadowReviewCount = d.shadowCount;
        r.shadowHit = d.shadowHit;
        r.shadowMiss = d.shadowMiss;
        r.shadowHitRate = d.shadowHitRate;
        r.avgFinalP = d.avgFinalP;
        r.avgGate = d.avgGate;
        r.avgEdge = d.avgEdge;
        r.reliability = d.reliability;
        r.recoverNeedHits = MIN_RECOVER_HITS;
        r.recoverNeedRate = RECOVER_RATE;
        r.freezeUntilTime = report.createdAt + freezeTimeMs;
        r.freezeUntilIssue = buildIssueUntilHint(report.latestIssue, freezeIssues);
        r.recoverHint =
                "当前SHADOW恢复="
                        + d.shadowHit
                        + "/"
                        + d.shadowCount
                        + "/"
                        + pct(d.shadowHitRate)
                        + "；需要>="
                        + r.recoverNeedHits
                        + "中且>="
                        + pct(r.recoverNeedRate)
                        + "；冻结到issueHint="
                        + r.freezeUntilIssue;
        r.sampleCases = d.sampleCases;
        return r;
    }

    private String buildIssueUntilHint(String latestIssue, int add) {
        if (latestIssue == null || latestIssue.length() == 0) return "+" + add + "期";
        try {
            long v = Long.parseLong(latestIssue);
            return String.valueOf(v + add);
        } catch (Throwable e) {
            return latestIssue + "+" + add;
        }
    }

    private void buildMainProblemAndAdvice(FreezeReport r) {
        if (r == null) return;

        String problem = "";
        String advice = "";

        if (r.totalBet < MIN_FREEZE_SAMPLE) {
            problem = append(problem, "真实BET样本少于" + MIN_FREEZE_SAMPLE + "，冻结矩阵只做观察");
            advice = append(advice, "继续积累真实BET与SHADOW样本，不要过早硬冻结全部域");
        }

        if (r.hardFreezeCount > 0) {
            problem = append(problem, "存在" + r.hardFreezeCount + "个硬冻结假强域");
            advice =
                    append(
                            advice,
                            "第6/14 AdaptiveLearningJudgeEngine 必须把 HARD_FREEZE 降级为SHADOW或HOLD");
        }

        if (r.softFreezeCount > 0) {
            problem = append(problem, "存在" + r.softFreezeCount + "个软冻结低效域");
            advice = append(advice, "软冻结域不得直接BET，除非后续审判有极强反证修复且DecisionGate通过");
        }

        if (r.recoveringCount > 0) {
            problem = append(problem, "存在" + r.recoveringCount + "个SHADOW恢复观察域");
            advice = append(advice, "恢复域也不能直接解冻，要先进入WATCH/PASS审判链路");
        }

        if (r.v20EmptyOrWeakBet > 0
                && r.totalBet > 0
                && ((double) r.v20EmptyOrWeakBet / (double) r.totalBet) >= 0.30d) {
            problem = append(problem, "V20空/弱仍BET比例偏高");
            advice =
                    append(advice, "DynamicCauseChainEngine 和 DecisionGateEngine 后续必须接入V20空/风险冻结键");
        }

        if (r.teacherWeakBet > 0
                && r.totalBet > 0
                && ((double) r.teacherWeakBet / (double) r.totalBet) >= 0.30d) {
            problem = append(problem, "Teacher弱/空仍BET比例偏高");
            advice = append(advice, "TeacherNumberMatrixEngine 的 WEAK/RISK/FREEZE 应进入冻结矩阵");
        }

        if (r.externalDiagnosisProblem != null && r.externalDiagnosisProblem.length() > 0) {
            problem = append(problem, "外部诊断提示：" + limit(r.externalDiagnosisProblem, 260));
        }

        if (problem.length() == 0) {
            problem = "暂未发现需要冻结的稳定假强域。";
        }
        if (advice.length() == 0) {
            advice = "冻结矩阵保持观察；后续由AdaptiveLearningJudgeEngine读取本报告，不直接影响BET。";
        }

        r.mainProblem = problem;
        r.nextAdvice = advice;
    }

    private void buildReportText(FreezeReport r) {
        if (r == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("【V20动态冻结矩阵报告】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("口径=只读真实BET/SHADOW/诊断/模块评分；只生成冻结建议；不出号、不结算、不创建Trade、不保存BET。\n\n");

        sb.append("一、总体样本\n");
        sb.append("真实BET=")
                .append(r.totalBet)
                .append("，中")
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
        sb.append("SHADOW恢复观察=")
                .append(r.settledShadow)
                .append("，中")
                .append(r.shadowHit)
                .append("，挂")
                .append(r.shadowMiss)
                .append("，胜率=")
                .append(pct(r.shadowHitRate))
                .append("，等待=")
                .append(r.waitShadow)
                .append("\n\n");

        sb.append("二、冻结矩阵数量\n");
        sb.append("冻结规则总数=")
                .append(r.freezeRules.size())
                .append("，HARD=")
                .append(r.hardFreezeCount)
                .append("，SOFT=")
                .append(r.softFreezeCount)
                .append("，WATCH=")
                .append(r.watchCount)
                .append("，RECOVERING=")
                .append(r.recoveringCount)
                .append("\n");
        sb.append("风险计数：Teacher弱/空BET=")
                .append(r.teacherWeakBet)
                .append("，Teacher风险BET=")
                .append(r.teacherRiskBet)
                .append("，V20空/弱BET=")
                .append(r.v20EmptyOrWeakBet)
                .append("，V20风险BET=")
                .append(r.v20RiskBet)
                .append("，Window不可信BET=")
                .append(r.windowUntrustedBet)
                .append("，负EdgeBET=")
                .append(r.negativeEdgeBet)
                .append("，高P失败=")
                .append(r.highPFail)
                .append("\n\n");

        sb.append("三、冻结规则明细\n");
        if (r.freezeRules.size() == 0) {
            sb.append("暂无冻结规则。\n");
        } else {
            int max = Math.min(r.freezeRules.size(), 80);
            for (int i = 0; i < max; i++) {
                FreezeRule rule = r.freezeRules.get(i);
                if (rule == null) continue;
                sb.append(i + 1).append(". ").append(rule.fullText()).append("\n");
                sb.append("   原因：").append(limit(rule.freezeReason, 260)).append("\n");
                sb.append("   恢复：").append(limit(rule.recoverHint, 260)).append("\n");
                sb.append("   建议：").append(limit(rule.actionAdvice, 260)).append("\n");
            }
        }
        sb.append("\n");

        sb.append("四、冻结矩阵结论\n");
        sb.append("主要问题：").append(safe(r.mainProblem)).append("\n");
        sb.append("下一步建议：").append(safe(r.nextAdvice)).append("\n");
        sb.append("红线：冻结矩阵不能直接BET，不能直接改Decision；只给第6/14智能审判和第14/14最终门使用。\n");

        r.detail = limit(sb.toString(), MAX_REPORT_LEN);
        r.summary = buildShortSummary(r);
    }

    private String buildShortSummary(FreezeReport r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("冻结矩阵：规则=")
                .append(r.freezeRules.size())
                .append(" HARD=")
                .append(r.hardFreezeCount)
                .append(" SOFT=")
                .append(r.softFreezeCount)
                .append(" WATCH=")
                .append(r.watchCount)
                .append(" RECOVER=")
                .append(r.recoveringCount)
                .append("；BET胜率=")
                .append(pct(r.winRate))
                .append("；SHADOW恢复=")
                .append(pct(r.shadowHitRate));
        if (r.mainProblem != null && r.mainProblem.length() > 0) {
            sb.append("；问题=").append(limit(r.mainProblem, 260));
        }
        return limit(sb.toString(), 720);
    }

    private String buildCompactJson(FreezeReport r) {
        if (r == null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"version\":\"").append(jsonSafe(r.version)).append("\",");
        sb.append("\"rules\":").append(r.freezeRules.size()).append(",");
        sb.append("\"hard\":").append(r.hardFreezeCount).append(",");
        sb.append("\"soft\":").append(r.softFreezeCount).append(",");
        sb.append("\"watch\":").append(r.watchCount).append(",");
        sb.append("\"recovering\":").append(r.recoveringCount).append(",");
        sb.append("\"winRate\":").append(r.winRate).append(",");
        sb.append("\"shadowHitRate\":").append(r.shadowHitRate).append(",");
        sb.append("\"mainProblem\":\"").append(jsonSafe(limit(r.mainProblem, 360))).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String[] buildCandidateFreezeKeys(CandidateSnapshot c) {
        if (c == null) return new String[0];
        ArrayList<String> keys = new ArrayList<String>();
        keys.add(buildFreezeKey(FREEZE_POSITION, "P" + c.position));
        keys.add(buildFreezeKey(FREEZE_STATE, c.windowBucket));
        keys.add(buildFreezeKey(FREEZE_TEACHER, c.teacherBucket));
        keys.add(buildFreezeKey(FREEZE_V20, c.v20Bucket));
        keys.add(buildFreezeKey(FREEZE_PROB_BUCKET, c.finalPBucket));
        keys.add(buildFreezeKey(FREEZE_EDGE_BUCKET, c.edgeBucket));
        keys.add(buildFreezeKey(FREEZE_K3_LAYER, c.k3RankPattern));
        keys.add(buildFreezeKey(FREEZE_COMBO_DOMAIN, c.comboDomain));
        String[] out = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) out[i] = keys.get(i);
        return out;
    }

    private String buildFreezeKey(String type, String value) {
        return normalizeBucket(type) + ":" + normalizeBucket(value);
    }

    private String comboDomainKey(FreezeCase c) {
        if (c == null) return "COMBO:空";
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(c.position);
        sb.append("|").append(c.finalPBucket);
        sb.append("|").append(shortBucket(c.teacherBucket));
        sb.append("|").append(shortBucket(c.v20Bucket));
        sb.append("|").append(shortBucket(c.windowBucket));
        sb.append("|").append(c.edgeBucket);
        return sb.toString();
    }

    private String shortBucket(String s) {
        s = normalizeBucket(s);
        if (s.length() <= 26) return s;
        return s.substring(0, 26);
    }

    private String k3LayerBucket(FreezeCase c) {
        if (c == null) return "K3空";
        if (c.actualNumber == c.n1) return "RANK1_HIT";
        if (c.actualNumber == c.n2) return "RANK2_HIT";
        if (c.actualNumber == c.n3) return "RANK3_HIT";
        if (c.actualNumber > 0) return "K3_MISS";
        return "K3_WAIT";
    }

    private FreezeCaseBrief copyBrief(FreezeCase c) {
        FreezeCaseBrief b = new FreezeCaseBrief();
        if (c == null) return b;
        b.kind = c.kind;
        b.targetIssue = c.targetIssue;
        b.position = c.position;
        b.n1 = c.n1;
        b.n2 = c.n2;
        b.n3 = c.n3;
        b.actualNumber = c.actualNumber;
        b.hit = c.hit;
        b.result = c.result;
        b.finalP = c.finalP;
        b.dynamicGate = c.dynamicGate;
        b.edge = c.edge;
        b.teacherStatus = c.teacherBucket;
        b.v20Status = c.v20Bucket;
        b.windowStateKey = c.windowBucket;
        return b;
    }

    private void sortRules(ArrayList<FreezeRule> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<FreezeRule>() {
                    public int compare(FreezeRule a, FreezeRule b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        int la = freezeLevelRank(a.freezeLevel);
                        int lb = freezeLevelRank(b.freezeLevel);
                        if (la != lb) return lb - la;
                        if (a.realCount != b.realCount) return b.realCount - a.realCount;
                        if (a.realHitRate < b.realHitRate) return -1;
                        if (a.realHitRate > b.realHitRate) return 1;
                        return safe(a.freezeKey).compareTo(safe(b.freezeKey));
                    }
                });
    }

    private int freezeLevelRank(String level) {
        if (LEVEL_HARD.equals(level)) return 5;
        if (LEVEL_SOFT.equals(level)) return 4;
        if (LEVEL_RECOVERING.equals(level)) return 3;
        if (LEVEL_WATCH.equals(level)) return 2;
        return 0;
    }

    private ArrayList<FreezeCase> loadRecentRealBetCases(DbStore store, int limit) {
        ArrayList<FreezeCase> out = new ArrayList<FreezeCase>();
        if (store == null) return out;

        ArrayList<Models.Decision> decisions =
                loadRecentDecisions(store, DEFAULT_DECISION_SCAN_LIMIT);
        HashMap<String, String> used = new HashMap<String, String>();

        for (int i = 0; i < decisions.size(); i++) {
            if (out.size() >= limit) break;
            Models.Decision d = decisions.get(i);
            if (d == null || !isBetDecision(d)) continue;
            String issue = safe(getString(d, "targetIssue", ""));
            if (issue.length() == 0 || used.containsKey(issue)) continue;
            Models.Trade t = getTradeByTargetIssueSafe(store, issue);
            if (t == null || !isRealBetTrade(t)) continue;
            FreezeCase c = fromTrade(store, t, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }

        if (out.size() < limit) {
            ArrayList<Models.Trade> trades = loadRecentTradesByReflection(store, limit * 2);
            for (int i = 0; i < trades.size(); i++) {
                if (out.size() >= limit) break;
                Models.Trade t = trades.get(i);
                if (t == null || !isRealBetTrade(t)) continue;
                String issue = safe(getString(t, "targetIssue", ""));
                if (issue.length() == 0 || used.containsKey(issue)) continue;
                Models.Decision d = getDecisionByTargetIssueSafe(store, issue);
                FreezeCase c = fromTrade(store, t, d);
                if (c != null) {
                    out.add(c);
                    used.put(issue, "1");
                }
            }
        }

        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<FreezeCase>(out.subList(0, limit));
        }
        return out;
    }

    private ArrayList<FreezeCase> loadRecentShadowCases(DbStore store, int limit) {
        ArrayList<FreezeCase> out = new ArrayList<FreezeCase>();
        if (store == null) return out;

        ArrayList<Models.Decision> decisions =
                loadRecentDecisions(store, DEFAULT_DECISION_SCAN_LIMIT);
        HashMap<String, String> used = new HashMap<String, String>();

        for (int i = 0; i < decisions.size(); i++) {
            if (out.size() >= limit) break;
            Models.Decision d = decisions.get(i);
            if (d == null || !isShadowDecision(d)) continue;
            String issue = safe(getString(d, "targetIssue", ""));
            if (issue.length() == 0 || used.containsKey(issue)) continue;
            FreezeCase c = fromShadowDecision(store, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }

        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<FreezeCase>(out.subList(0, limit));
        }
        return out;
    }

    private FreezeCase fromTrade(DbStore store, Models.Trade t, Models.Decision d) {
        if (t == null) return null;
        FreezeCase c = new FreezeCase();
        c.kind = "BET";
        c.targetIssue = safe(getString(t, "targetIssue", ""));
        c.baseIssue = safe(getString(t, "baseIssue", ""));
        c.createdAt = getLong(t, "createdAt", 0L);
        if (c.createdAt <= 0L) c.createdAt = getLong(t, "betTime", 0L);
        c.position = getInt(t, "position", 0);
        c.n1 = getInt(t, "n1", 0);
        c.n2 = getInt(t, "n2", 0);
        c.n3 = getInt(t, "n3", 0);
        c.actualNumber = getInt(t, "actualNumber", 0);
        c.settled = getInt(t, "settled", 0);
        c.result = normalizeResult(getString(t, "result", ""));
        c.hit = isWinResult(c.result) ? 1 : 0;
        c.pnl = getDouble(t, "pnl", 0.0d);
        c.missReason = getString(t, "missReason", "");
        c.finalP = getDouble(t, "finalP", 0.0d);
        c.rawP = getDouble(t, "rawP", 0.0d);
        c.calibratedP = getDouble(t, "calibratedP", 0.0d);
        c.dynamicGate = getDouble(t, "dynamicGate", 0.0d);
        c.edge = getDouble(t, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
            c.edge = c.finalP - c.dynamicGate;
        c.effectiveSample = getInt(t, "effectiveSample", 0);
        c.comboSample = getInt(t, "comboSample", 0);
        c.similarHitRate = getDouble(t, "similarHitRate", 0.0d);
        c.comboHitRate = getDouble(t, "comboHitRate", 0.0d);
        c.stateMode = getString(t, "stateMode", "");
        c.reasonSummary = getString(t, "reasonSummary", "");
        copyOptionalFields(c, t);

        if (d != null) mergeDecisionFields(c, d);

        fillActualFromDraw(store, c);
        normalizeCaseBuckets(c);
        return c;
    }

    private FreezeCase fromShadowDecision(DbStore store, Models.Decision d) {
        if (d == null) return null;
        FreezeCase c = new FreezeCase();
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
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
            c.edge = c.finalP - c.dynamicGate;
        c.effectiveSample = getInt(d, "effectiveSample", 0);
        c.comboSample = getInt(d, "comboSample", 0);
        c.similarHitRate = getDouble(d, "similarHitRate", 0.0d);
        c.comboHitRate = getDouble(d, "comboHitRate", 0.0d);
        c.stateMode = getString(d, "stateMode", "");
        c.reasonSummary = getString(d, "reasonSummary", "");
        copyOptionalFields(c, d);

        Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
        if (draw != null && isDrawValid(draw) && isValidPosition(c.position)) {
            int n = getNumberAtPositionSafe(draw, c.position);
            if (isValidNumber(n)) {
                c.actualNumber = n;
                c.settled = 1;
                c.hit = containsK3(c, n) ? 1 : 0;
                c.result = c.hit == 1 ? resultWin() : resultMiss();
            } else {
                c.settled = 0;
                c.result = resultWait();
            }
        } else {
            c.settled = 0;
            c.result = resultWait();
        }

        normalizeCaseBuckets(c);
        return c;
    }

    private void mergeDecisionFields(FreezeCase c, Models.Decision d) {
        if (c == null || d == null) return;
        if (c.reasonSummary.length() == 0) c.reasonSummary = getString(d, "reasonSummary", "");
        if (c.finalP <= 0.0d) c.finalP = getDouble(d, "finalP", 0.0d);
        if (c.rawP <= 0.0d) c.rawP = getDouble(d, "rawP", 0.0d);
        if (c.calibratedP <= 0.0d) c.calibratedP = getDouble(d, "calibratedP", 0.0d);
        if (c.dynamicGate <= 0.0d) c.dynamicGate = getDouble(d, "dynamicGate", 0.0d);
        if (c.edge == 0.0d) c.edge = getDouble(d, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
            c.edge = c.finalP - c.dynamicGate;
        if (c.effectiveSample <= 0) c.effectiveSample = getInt(d, "effectiveSample", 0);
        if (c.comboSample <= 0) c.comboSample = getInt(d, "comboSample", 0);
        if (c.similarHitRate <= 0.0d) c.similarHitRate = getDouble(d, "similarHitRate", 0.0d);
        if (c.comboHitRate <= 0.0d) c.comboHitRate = getDouble(d, "comboHitRate", 0.0d);
        if (c.stateMode.length() == 0) c.stateMode = getString(d, "stateMode", "");
        copyOptionalFields(c, d);
    }

    private void fillActualFromDraw(DbStore store, FreezeCase c) {
        if (store == null || c == null) return;
        if (isValidNumber(c.actualNumber) && c.settled == 1) {
            c.hit = containsK3(c, c.actualNumber) ? 1 : 0;
            c.result = c.hit == 1 ? resultWin() : resultMiss();
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

    private void normalizeCaseBuckets(FreezeCase c) {
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
        c.missBucket = normalizeBucket(c.missReason);
        if (c.missBucket.length() == 0) c.missBucket = "空";
    }

    private void copyOptionalFields(FreezeCase c, Object source) {
        if (c == null || source == null) return;

        c.teacherMatrixStatus = getString(source, "teacherMatrixStatus", c.teacherMatrixStatus);
        c.teacherNumberStatus = getString(source, "teacherNumberStatus", c.teacherNumberStatus);
        c.teacherStatus = getString(source, "teacherStatus", c.teacherStatus);
        c.teacherMatrixSummary = getString(source, "teacherMatrixSummary", c.teacherMatrixSummary);
        c.teacherMatrixSample = getInt(source, "teacherMatrixSample", c.teacherMatrixSample);
        c.teacherMatrixHitRate = getDouble(source, "teacherMatrixHitRate", c.teacherMatrixHitRate);
        c.teacherMatrixConfidence =
                getDouble(source, "teacherMatrixConfidence", c.teacherMatrixConfidence);
        c.teacherMatrixRisk = getDouble(source, "teacherMatrixRisk", c.teacherMatrixRisk);
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

    private boolean isTeacherRisk(FreezeCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("风险") >= 0
                || s.indexOf("冻结") >= 0) return true;
        if (c.teacherMatrixRisk >= 0.65d) return true;
        if (c.teacherCounterContribution > c.teacherEvidenceContribution + 0.080d
                && c.teacherCounterContribution > 0.0d) return true;
        return false;
    }

    private boolean isTeacherWeakOrEmpty(FreezeCase c) {
        if (c == null) return true;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.length() == 0 || "空".equals(s)) return true;
        if (s.indexOf("WEAK") >= 0
                || s.indexOf("弱") >= 0
                || s.indexOf("EMPTY") >= 0
                || s.indexOf("NONE") >= 0) return true;
        if (c.teacherMatrixSample > 0 && c.teacherMatrixSample < 30) return true;
        return false;
    }

    private boolean hasV20Support(FreezeCase c) {
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
        if (c.v20SupportScore > c.v20RiskScore + 0.060d && c.v20SupportScore > 0.0d) return true;
        if (c.decisionEvidenceStrong > 0 || c.decisionEvidenceElite > 0) return true;
        return false;
    }

    private boolean hasV20Risk(FreezeCase c) {
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
        if (c.v20RiskScore > c.v20SupportScore + 0.080d && c.v20RiskScore > 0.0d) return true;
        return false;
    }

    private boolean hasTrustedWindow(FreezeCase c) {
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
                    if (o instanceof Models.Decision) out.add((Models.Decision) o);
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
                        if (o instanceof Models.Trade) out.add((Models.Trade) o);
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
                if (obj instanceof Models.Trade) return (Models.Trade) obj;
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
                if (obj instanceof Models.Decision) return (Models.Decision) obj;
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
                if (obj instanceof Models.Draw) return (Models.Draw) obj;
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
        if (action.length() > 0 && !isBetText(action)) return false;
        if (!isValidPosition(getInt(t, "position", 0))) return false;
        if (!isValidNumber(getInt(t, "n1", 0))
                || !isValidNumber(getInt(t, "n2", 0))
                || !isValidNumber(getInt(t, "n3", 0))) return false;
        String issue = safe(getString(t, "targetIssue", ""));
        return issue.length() > 0;
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
            if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        } catch (Throwable e) {
        }
        return p >= 1 && p <= POSITION_COUNT;
    }

    private boolean isValidNumber(int n) {
        try {
            Method m = AppConfig.class.getMethod("isValidNumber", new Class[] {int.class});
            Object obj = m.invoke(null, new Object[] {Integer.valueOf(n)});
            if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        } catch (Throwable e) {
        }
        return n >= 1 && n <= 10;
    }

    private boolean isDrawValid(Models.Draw draw) {
        if (draw == null) return false;
        try {
            Method m = draw.getClass().getMethod("isValid", new Class[] {});
            Object obj = m.invoke(draw, new Object[] {});
            if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        } catch (Throwable e) {
        }
        return true;
    }

    private int getNumberAtPositionSafe(Models.Draw draw, int position) {
        if (draw == null) return 0;
        try {
            Method m = draw.getClass().getMethod("getNumberAtPosition", new Class[] {int.class});
            Object obj = m.invoke(draw, new Object[] {Integer.valueOf(position)});
            if (obj instanceof Number) return ((Number) obj).intValue();
        } catch (Throwable e) {
        }
        return 0;
    }

    private boolean containsK3(FreezeCase c, int actual) {
        if (c == null) return false;
        return actual == c.n1 || actual == c.n2 || actual == c.n3;
    }

    private void sortCasesDesc(ArrayList<FreezeCase> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<FreezeCase>() {
                    public int compare(FreezeCase a, FreezeCase b) {
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

    private String sampleBucket(FreezeCase c) {
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

    private String recentBucket(FreezeCase c) {
        if (c == null) return "Recent空";
        if (c.recentValidity >= 0.75d) return "Recent强";
        if (c.recentValidity >= 0.60d) return "Recent可用";
        if (c.recentValidity > 0.0d) return "Recent弱";
        return "Recent空";
    }

    private String normalizeBucket(String s) {
        s = safe(s).trim();
        if (s.length() == 0) return "空";
        if (s.length() > 100) s = s.substring(0, 100);
        return s;
    }

    private Object getObject(Object obj, String field, Object def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            return v == null ? def : v;
        } catch (Throwable e) {
        }
        return def;
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

    public static final class FreezeReport {
        public int ok;
        public String version;
        public long createdAt;
        public int betLimit;
        public int shadowLimit;
        public String latestIssue;

        public ArrayList<FreezeCase> betCases;
        public ArrayList<FreezeCase> shadowCases;

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

        public int totalShadow;
        public int settledShadow;
        public int shadowHit;
        public int shadowMiss;
        public int waitShadow;
        public double shadowHitRate;

        public int negativeEdgeBet;
        public int teacherRiskBet;
        public int teacherWeakBet;
        public int v20RiskBet;
        public int v20EmptyOrWeakBet;
        public int windowUntrustedBet;
        public int highPFail;
        public int consecutiveRecentMiss;

        public HashMap<String, FreezeDomainStats> domainMap;
        public ArrayList<FreezeRule> freezeRules;
        public HashMap<String, FreezeRule> ruleMap;

        public int hardFreezeCount;
        public int softFreezeCount;
        public int watchCount;
        public int recoveringCount;

        public String externalDiagnosisProblem;
        public String externalDiagnosisAdvice;
        public String mainProblem;
        public String nextAdvice;
        public String summary;
        public String detail;

        public FreezeReport() {
            ok = 0;
            version = "";
            createdAt = 0L;
            betLimit = 0;
            shadowLimit = 0;
            latestIssue = "";
            betCases = new ArrayList<FreezeCase>();
            shadowCases = new ArrayList<FreezeCase>();
            domainMap = new HashMap<String, FreezeDomainStats>();
            freezeRules = new ArrayList<FreezeRule>();
            ruleMap = new HashMap<String, FreezeRule>();
            externalDiagnosisProblem = "";
            externalDiagnosisAdvice = "";
            mainProblem = "";
            nextAdvice = "";
            summary = "";
            detail = "";
        }

        public FreezeRule getRule(String freezeKey) {
            if (freezeKey == null) return null;
            return ruleMap.get(freezeKey);
        }

        public boolean hasHardFreeze(String freezeKey) {
            FreezeRule r = getRule(freezeKey);
            return r != null && LEVEL_HARD.equals(r.freezeLevel);
        }

        public boolean hasAnyFreeze(String freezeKey) {
            return getRule(freezeKey) != null;
        }
    }

    public static final class FreezeDecision {
        public String version;
        public long createdAt;
        public int frozen;
        public String freezeType;
        public String freezeLevel;
        public String freezeKey;
        public String freezeReason;
        public String action;
        public int downgradeToShadow;
        public String freezeUntilIssue;
        public long freezeUntilTime;
        public int shadowReviewCount;
        public int recoverNeedHits;
        public double recoverNeedRate;
        public String recoverHint;

        public FreezeDecision() {
            version = "";
            createdAt = 0L;
            frozen = 0;
            freezeType = "";
            freezeLevel = LEVEL_NONE;
            freezeKey = "";
            freezeReason = "";
            action = ACTION_PASS;
            downgradeToShadow = 0;
            freezeUntilIssue = "";
            freezeUntilTime = 0L;
            shadowReviewCount = 0;
            recoverNeedHits = 0;
            recoverNeedRate = 0.0d;
            recoverHint = "";
        }

        public boolean shouldBlockRealBet() {
            return frozen == 1
                    && (LEVEL_HARD.equals(freezeLevel) || LEVEL_SOFT.equals(freezeLevel));
        }

        public boolean shouldDowngradeToShadow() {
            return downgradeToShadow == 1
                    || ACTION_DOWNGRADE_TO_SHADOW.equals(action)
                    || ACTION_FREEZE.equals(action);
        }

        public String text() {
            return "FreezeDecision{frozen="
                    + frozen
                    + ", level="
                    + freezeLevel
                    + ", key="
                    + freezeKey
                    + ", action="
                    + action
                    + ", reason="
                    + freezeReason
                    + ", recover="
                    + recoverHint
                    + "}";
        }
    }

    public static final class FreezeRule {
        public String freezeType;
        public String freezeKey;
        public String freezeValue;
        public String freezeLevel;
        public String freezeReason;
        public String actionAdvice;
        public String freezeUntilIssue;
        public long freezeUntilTime;
        public int realCount;
        public int realHit;
        public int realMiss;
        public double realHitRate;
        public int shadowReviewCount;
        public int shadowHit;
        public int shadowMiss;
        public double shadowHitRate;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public double reliability;
        public int recoverNeedHits;
        public double recoverNeedRate;
        public String recoverHint;
        public ArrayList<FreezeCaseBrief> sampleCases;

        public FreezeRule() {
            freezeType = "";
            freezeKey = "";
            freezeValue = "";
            freezeLevel = LEVEL_NONE;
            freezeReason = "";
            actionAdvice = "";
            freezeUntilIssue = "";
            recoverHint = "";
            sampleCases = new ArrayList<FreezeCaseBrief>();
        }

        public String fullText() {
            return freezeKey
                    + "：level="
                    + freezeLevel
                    + "，真实="
                    + realCount
                    + "/"
                    + realHit
                    + "/"
                    + pct(realHitRate)
                    + "，SHADOW恢复="
                    + shadowReviewCount
                    + "/"
                    + shadowHit
                    + "/"
                    + pct(shadowHitRate)
                    + "，均P="
                    + pct(avgFinalP)
                    + "，均Gate="
                    + pct(avgGate)
                    + "，均Edge="
                    + signedPct(avgEdge)
                    + "，可靠度="
                    + pct(reliability)
                    + "，冻结到="
                    + freezeUntilIssue;
        }
    }

    public static final class FreezeDomainStats {
        public String freezeType;
        public String domainValue;
        public String freezeKey;
        public String reasonSeed;
        public int realCount;
        public int realHit;
        public int realMiss;
        public double realHitRate;
        public double sumFinalP;
        public double sumGate;
        public double sumEdge;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public double realEdge;
        public int shadowCount;
        public int shadowHit;
        public int shadowMiss;
        public double shadowHitRate;
        public double shadowEdge;
        public double reliability;
        public int recoverReady;

        public int externalMisleading;
        public int externalConflict;
        public int externalSample;
        public double externalHitRate;
        public double externalReliability;
        public String externalProblem;

        public int shadowBlocked;
        public int shadowBlockedSample;
        public double shadowBlockedHitRate;
        public double shadowBlockedReliability;

        public int shadowPromoted;
        public int shadowPromotedSample;
        public double shadowPromotedHitRate;

        public ArrayList<FreezeCaseBrief> sampleCases;

        public FreezeDomainStats() {
            freezeType = "";
            domainValue = "";
            freezeKey = "";
            reasonSeed = "";
            externalProblem = "";
            sampleCases = new ArrayList<FreezeCaseBrief>();
        }

        public void addReal(FreezeCase c) {
            if (c == null) return;
            realCount++;
            if (c.hit == 1
                    || "WIN".equalsIgnoreCase(c.result)
                    || "HIT".equalsIgnoreCase(c.result)
                    || "命中".equals(c.result)) realHit++;
            else realMiss++;
            sumFinalP += c.finalP;
            sumGate += c.dynamicGate;
            sumEdge += c.edge;
        }

        public void addShadow(FreezeCase c) {
            if (c == null) return;
            shadowCount++;
            if (c.hit == 1
                    || "WIN".equalsIgnoreCase(c.result)
                    || "HIT".equalsIgnoreCase(c.result)
                    || "命中".equals(c.result)) shadowHit++;
            else shadowMiss++;
        }
    }

    public static final class CandidateSnapshot {
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public String finalPBucket;
        public String edgeBucket;
        public String teacherBucket;
        public String v20Bucket;
        public String windowBucket;
        public String k3RankPattern;
        public String comboDomain;

        public CandidateSnapshot() {
            finalPBucket = "";
            edgeBucket = "";
            teacherBucket = "";
            v20Bucket = "";
            windowBucket = "";
            k3RankPattern = "";
            comboDomain = "";
        }

        public static CandidateSnapshot fromObject(Object o) {
            CandidateSnapshot c = new CandidateSnapshot();
            if (o == null) return c;
            c.position = getStaticInt(o, "position", 0);
            c.n1 = getStaticInt(o, "n1", 0);
            c.n2 = getStaticInt(o, "n2", 0);
            c.n3 = getStaticInt(o, "n3", 0);
            c.finalP = getStaticDouble(o, "finalP", 0.0d);
            c.dynamicGate = getStaticDouble(o, "dynamicGate", 0.0d);
            c.edge = getStaticDouble(o, "edge", 0.0d);
            if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
                c.edge = c.finalP - c.dynamicGate;
            c.finalPBucket = staticFinalPBucket(c.finalP);
            c.edgeBucket = staticEdgeBucket(c.edge);
            c.teacherBucket =
                    normalizeStatic(
                            firstNonEmpty(
                                    getStaticString(o, "teacherStatus", ""),
                                    getStaticString(o, "teacherMatrixStatus", ""),
                                    getStaticString(o, "teacherNumberStatus", "")));
            c.v20Bucket =
                    normalizeStatic(
                            firstNonEmpty(
                                    getStaticString(o, "v20Status", ""),
                                    getStaticString(o, "dynamicStatus", ""),
                                    getStaticString(o, "actionAdvice", "")));
            c.windowBucket =
                    normalizeStatic(
                            firstNonEmpty(
                                    getStaticString(o, "windowStateKey", ""),
                                    getStaticString(o, "dynamicWindowStateKey", ""),
                                    getStaticString(o, "probabilityWindowStateKey", "")));
            c.k3RankPattern = normalizeStatic(getStaticString(o, "k3RankPattern", "K3_UNKNOWN"));
            c.comboDomain =
                    "P"
                            + c.position
                            + "|"
                            + c.finalPBucket
                            + "|"
                            + shortStatic(c.teacherBucket)
                            + "|"
                            + shortStatic(c.v20Bucket)
                            + "|"
                            + shortStatic(c.windowBucket)
                            + "|"
                            + c.edgeBucket;
            return c;
        }

        private static int getStaticInt(Object obj, String field, int def) {
            try {
                Field f = obj.getClass().getField(field);
                Object v = f.get(obj);
                if (v instanceof Number) return ((Number) v).intValue();
                if (v != null) return Integer.parseInt(String.valueOf(v));
            } catch (Throwable e) {
            }
            return def;
        }

        private static double getStaticDouble(Object obj, String field, double def) {
            try {
                Field f = obj.getClass().getField(field);
                Object v = f.get(obj);
                if (v instanceof Number) return ((Number) v).doubleValue();
                if (v != null) return Double.parseDouble(String.valueOf(v));
            } catch (Throwable e) {
            }
            return def;
        }

        private static String getStaticString(Object obj, String field, String def) {
            try {
                Field f = obj.getClass().getField(field);
                Object v = f.get(obj);
                if (v != null) return String.valueOf(v);
            } catch (Throwable e) {
            }
            return def == null ? "" : def;
        }

        private static String staticFinalPBucket(double p) {
            if (p <= 0.0d) return "FinalP空";
            if (p < 0.340d) return "P<34%";
            if (p < 0.360d) return "P34-36%";
            if (p < 0.380d) return "P36-38%";
            if (p < 0.400d) return "P38-40%";
            if (p < 0.420d) return "P40-42%";
            return "P>=42%";
        }

        private static String staticEdgeBucket(double e) {
            if (e < -0.010d) return "Edge<-1%";
            if (e < 0.0d) return "Edge负";
            if (e < 0.010d) return "Edge0-1%";
            if (e < 0.020d) return "Edge1-2%";
            return "Edge>=2%";
        }

        private static String normalizeStatic(String s) {
            s = safe(s).trim();
            if (s.length() == 0) return "空";
            if (s.length() > 100) s = s.substring(0, 100);
            return s;
        }

        private static String shortStatic(String s) {
            s = normalizeStatic(s);
            if (s.length() <= 26) return s;
            return s.substring(0, 26);
        }
    }

    public static final class FreezeCaseBrief {
        public String kind;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public int hit;
        public String result;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public String teacherStatus;
        public String v20Status;
        public String windowStateKey;

        public FreezeCaseBrief() {
            kind = "";
            targetIssue = "";
            result = "";
            teacherStatus = "";
            v20Status = "";
            windowStateKey = "";
        }
    }

    public static final class FreezeCase {
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
        public String sampleBucket;
        public String recentBucket;
        public String missBucket;

        public String teacherMatrixStatus;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherMatrixSummary;
        public int teacherMatrixSample;
        public double teacherMatrixHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixRisk;
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
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String decisionActionAdvice;
        public int decisionHardRisk;
        public int decisionCounterRiskHigh;
        public int decisionEvidenceStrong;
        public int decisionEvidenceElite;

        public FreezeCase() {
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
            missBucket = "";
            teacherMatrixStatus = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherMatrixSummary = "";
            windowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";
            decisionWindowStateKey = "";
            dynamicStatus = "";
            v20Status = "";
            decisionActionAdvice = "";
        }
    }
}
