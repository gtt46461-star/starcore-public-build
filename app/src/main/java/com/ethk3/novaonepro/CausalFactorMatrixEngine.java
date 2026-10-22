package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CausalFactorMatrixEngine.java
 *
 * <p>V20 主链闭环修复第 8/22 文件。
 *
 * <p>作用： 1. 读取 PreDrawFeatureEngine 生成的 10 位置 × 10 号码开奖前特征。 2. 读取 FullCandidateScanEngine 生成的单位置固定
 * K=3 候选。 3. 把每个 K3 候选拆成真实可计算的因果因子矩阵： 趋势因、遗漏/间隔因、冷热因、转移因、位置需求因、号码供给因、 K3
 * 覆盖因、第三号质量因、反事实/反证因、样本可信因、近期有效因。 4. 输出 Models.CauseChain，并把 factorKey / clusterKey / evidenceScore
 * / antiEvidenceScore / sampleReliability / recentValidity / actionAdvice 等字段回写候选。 5.
 * 本文件只做开奖前证据加工，不创建 BET、不创建 Trade、不结算、不改概率、不改门槛， 最终真实出手仍只能由 DecisionGateEngine 决定。
 *
 * <p>Java7 / AIDE / Android 原生兼容： - 无 lambda - 无 stream - 无第三方库
 */
public final class CausalFactorMatrixEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String MATRIX_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;
    private static final int K_SIZE = 3;

    private static final double DEFAULT_GRADE_S = 0.780d;
    private static final double DEFAULT_GRADE_A = 0.680d;
    private static final double DEFAULT_GRADE_B = 0.560d;
    private static final double DEFAULT_PASS_CAUSE = 0.520d;
    private static final double HARD_RISK_LINE = 0.720d;
    private static final double MID_RISK_LINE = 0.550d;
    private static final double EPS = 0.0000001d;

    public CausalFactorMatrixEngine() {}

    /** V43 canonical live adapter; no reflection ambiguity. */
    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        if (out.size() == 0) return out;
        Map<String, Models.PreDrawFeature> features =
                new PreDrawFeatureEngine(store).buildFeatures(baseIssue, targetIssue, history);
        ArrayList<Models.CauseChain> chains =
                buildCauseChains(baseIssue, targetIssue, out, features);
        try {
            if (store != null) {
                store.setState("v43_causal_factor_apply_count", String.valueOf(out.size()));
                store.setState(
                        "v43_causal_factor_chain_count",
                        String.valueOf(chains == null ? 0 : chains.size()));
                store.setState("v43_causal_factor_status", "APPLIED_TYPED");
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToAll(store, candidates, history, baseIssue, targetIssue);
    }

    /** 主入口：兼容 Models.CandidateK3 / Models.ComboCandidate / 反射候选对象。 */
    public ArrayList<Models.CauseChain> buildCauseChains(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        ArrayList<Models.CauseChain> out = new ArrayList<Models.CauseChain>();
        if (candidates == null || candidates.size() == 0) {
            return out;
        }
        for (int i = 0; i < candidates.size(); i++) {
            Object c = candidates.get(i);
            if (c == null) {
                continue;
            }
            Models.CauseChain chain = buildOneCauseChain(baseIssue, targetIssue, c, featureMap);
            if (chain != null) {
                out.add(chain);
            }
        }
        sortChains(out);
        rerankCandidatesByCause(candidates);
        return out;
    }

    public ArrayList<Models.CauseChain> buildCauseChains(
            List candidates, Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains("", "", candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> buildCauseMatrix(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> analyze(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> analyze(
            List candidates, Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains("", "", candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> applyCauseMatrix(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> applyToCandidates(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> apply(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
    }

    public ArrayList<Models.CauseChain> run(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
    }

    public int applyAndCount(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        ArrayList<Models.CauseChain> list =
                buildCauseChains(baseIssue, targetIssue, candidates, featureMap);
        return list == null ? 0 : list.size();
    }

    public Models.CauseChain buildOneCauseChain(
            String baseIssue,
            String targetIssue,
            Object candidate,
            Map<String, Models.PreDrawFeature> featureMap) {
        if (candidate == null) {
            return null;
        }

        int position = getInt(candidate, "position", 0);
        int n1 = getInt(candidate, "n1", 0);
        int n2 = getInt(candidate, "n2", 0);
        int n3 = getInt(candidate, "n3", 0);
        int mask = getInt(candidate, "k3Mask", getInt(candidate, "k3_mask", 0));

        if (!isValidPosition(position)) {
            writeCandidateBlocked(candidate, "CAUSE_INVALID_POSITION", "因果矩阵拒绝：位置非法");
            return null;
        }
        if (!isValidNumber(n1) || !isValidNumber(n2) || !isValidNumber(n3)) {
            writeCandidateBlocked(candidate, "CAUSE_INVALID_NUMBER", "因果矩阵拒绝：K3号码非法");
            return null;
        }
        if (mask <= 0) {
            mask = buildMask(n1, n2, n3);
            putInt(candidate, "k3Mask", mask);
        }
        if (!isValidK3Mask(mask)) {
            writeCandidateBlocked(candidate, "CAUSE_INVALID_K3", "因果矩阵拒绝：K3组合非法");
            return null;
        }

        String realBaseIssue = firstNonEmpty(baseIssue, getString(candidate, "baseIssue", ""));
        String realTargetIssue =
                firstNonEmpty(targetIssue, getString(candidate, "targetIssue", ""));
        String candidateKey = getString(candidate, "candidateKey", "");
        if (candidateKey.length() == 0) {
            candidateKey = buildCandidateKey(realTargetIssue, position, n1, n2, n3);
            putString(candidate, "candidateKey", candidateKey);
        }

        NumberCause f1 =
                buildNumberCause(getFeature(featureMap, position, n1), candidate, position, n1, 1);
        NumberCause f2 =
                buildNumberCause(getFeature(featureMap, position, n2), candidate, position, n2, 2);
        NumberCause f3 =
                buildNumberCause(getFeature(featureMap, position, n3), candidate, position, n3, 3);
        CauseAggregate agg = aggregate(position, n1, n2, n3, mask, f1, f2, f3, candidate);

        String grade = gradeOf(agg.finalScore, agg.riskScore, agg.confidence);
        int passCause = passCause(agg.finalScore, agg.riskScore, agg.confidence) ? 1 : 0;
        String stateKey = buildStateKey(position, n1, n2, n3, f1, f2, f3, agg);
        String clusterKey = buildClusterKey(position, n1, n2, n3, f1, f2, f3, agg, grade);
        String factorKey = buildFactorKey(position, n1, n2, n3, agg);
        String chainKey = buildChainKey(realTargetIssue, candidateKey, stateKey);
        String positiveText = buildPositiveText(position, n1, n2, n3, agg);
        String negativeText = buildNegativeText(position, n1, n2, n3, agg);
        String actionAdvice = buildActionAdvice(grade, passCause, agg);
        String summary =
                buildSummaryText(
                        position,
                        n1,
                        n2,
                        n3,
                        grade,
                        passCause,
                        agg.finalScore,
                        agg.positiveScore,
                        agg.negativeScore,
                        agg.riskScore,
                        agg.confidence,
                        agg,
                        actionAdvice);

        Models.CauseChain chain = new Models.CauseChain();
        putString(chain, "chainKey", chainKey);
        putString(chain, "candidateKey", candidateKey);
        putString(chain, "baseIssue", realBaseIssue);
        putString(chain, "targetIssue", realTargetIssue);
        putLong(chain, "createdAt", System.currentTimeMillis());
        putInt(chain, "position", position);
        putInt(chain, "n1", n1);
        putInt(chain, "n2", n2);
        putInt(chain, "n3", n3);
        putInt(chain, "k3Mask", mask);
        putString(chain, "stateKey", stateKey);
        putString(chain, "windowStateKey", stateKey);
        putString(chain, "clusterKey", clusterKey);
        putString(chain, "factorKey", factorKey);
        putString(chain, "factorId", factorKey);
        putString(chain, "factorName", "趋势/遗漏/冷热/转移/位置/供给/覆盖/第三号/反证综合因子矩阵");
        putString(chain, "grade", grade);
        putDouble(chain, "trendCause", agg.trendCause);
        putDouble(chain, "gapCause", agg.gapCause);
        putDouble(chain, "hotColdCause", agg.hotColdCause);
        putDouble(chain, "transitionCause", agg.transitionCause);
        putDouble(chain, "positionCause", agg.positionCause);
        putDouble(chain, "numberSupplyCause", agg.numberSupplyCause);
        putDouble(chain, "coverageCause", agg.coverageCause);
        putDouble(chain, "thirdCause", agg.thirdCause);
        putDouble(chain, "comboRoleCause", agg.comboCause);
        putDouble(chain, "recoveryCause", agg.recoveryCause);
        putDouble(chain, "falseStrongCause", agg.falseStrongCause);
        putDouble(chain, "counterCause", agg.counterCause);
        putDouble(chain, "positiveCauseScore", agg.positiveScore);
        putDouble(chain, "negativeCauseScore", agg.negativeScore);
        putDouble(chain, "finalCauseScore", agg.finalScore);
        putDouble(chain, "causeScore", agg.finalScore);
        putDouble(chain, "evidenceScore", agg.evidenceScore);
        putDouble(chain, "antiEvidenceScore", agg.antiEvidenceScore);
        putDouble(chain, "counterEvidenceScore", agg.antiEvidenceScore);
        putDouble(chain, "netEvidenceScore", agg.netEvidenceScore);
        putDouble(chain, "riskScore", agg.riskScore);
        putDouble(chain, "confidence", agg.confidence);
        putDouble(chain, "sampleReliability", agg.sampleReliability);
        putDouble(chain, "recentValidity", agg.recentValidity);
        putInt(chain, "sampleCount", agg.sampleCount);
        putInt(chain, "recentSampleCount", agg.recentSampleCount);
        putInt(chain, "factorCount", agg.factorCount);
        putInt(chain, "strongFactorCount", agg.strongFactorCount);
        putInt(chain, "riskFactorCount", agg.riskFactorCount);
        putInt(chain, "passCause", passCause);
        putString(
                chain,
                "blockCode",
                passCause == 1 ? "" : blockCodeOf(agg.finalScore, agg.riskScore, agg.confidence));
        putString(
                chain,
                "rejectCode",
                passCause == 1 ? "" : blockCodeOf(agg.finalScore, agg.riskScore, agg.confidence));
        putString(chain, "actionAdvice", actionAdvice);
        putString(chain, "positiveText", positiveText);
        putString(chain, "negativeText", negativeText);
        putString(chain, "summaryText", summary);
        putString(chain, "reasonText", summary);
        putString(chain, "matrixVersion", MATRIX_VERSION);
        putString(chain, "createdBy", "CausalFactorMatrixEngine");

        writeCandidateCause(
                candidate, chain, agg, grade, passCause, summary, negativeText, actionAdvice);
        return chain;
    }

    private void writeCandidateCause(
            Object candidate,
            Models.CauseChain chain,
            CauseAggregate agg,
            String grade,
            int passCause,
            String summary,
            String riskText,
            String actionAdvice) {
        if (candidate == null || chain == null || agg == null) {
            return;
        }
        String chainKey = getString(chain, "chainKey", "");
        String stateKey = getString(chain, "stateKey", "");
        String clusterKey = getString(chain, "clusterKey", "");
        String factorKey = getString(chain, "factorKey", "");
        String reject =
                passCause == 1 ? "" : blockCodeOf(agg.finalScore, agg.riskScore, agg.confidence);

        putString(candidate, "causeKey", chainKey);
        putString(candidate, "factorId", factorKey);
        putString(candidate, "factorName", "CAUSAL_FACTOR_MATRIX_FULL");
        putString(candidate, "factorKey", factorKey);
        putString(candidate, "clusterKey", clusterKey);
        putString(candidate, "windowStateKey", stateKey);
        putString(candidate, "dynamicWindowStateKey", stateKey);
        putString(candidate, "finalDecisionWindowStateKey", stateKey);
        putString(candidate, "probabilityWindowStateKey", stateKey);
        putString(candidate, "grade", grade);
        putString(candidate, "v20Status", buildV20Status(grade, passCause, agg));
        putString(candidate, "v20ActionAdvice", actionAdvice);
        putString(candidate, "actionAdvice", actionAdvice);
        putString(candidate, "rejectCode", reject);
        putString(candidate, "blockCode", reject);
        putString(candidate, "finalDecisionRejectCode", reject);
        putString(candidate, "reasonSummary", summary);
        putString(candidate, "riskSummary", riskText);
        putString(
                candidate,
                "auditText",
                "V20因果因子矩阵已完成：只写候选证据，不创建BET，最终由DecisionGateEngine裁决。版本=" + VERSION);
        putString(candidate, "fieldChainVersion", VERSION);

        putDouble(candidate, "causeScore", agg.finalScore);
        putDouble(candidate, "positiveCauseScore", agg.positiveScore);
        putDouble(candidate, "negativeCauseScore", agg.negativeScore);
        putDouble(candidate, "evidenceScore", agg.evidenceScore);
        putDouble(candidate, "antiEvidenceScore", agg.antiEvidenceScore);
        putDouble(candidate, "counterEvidenceScore", agg.antiEvidenceScore);
        putDouble(candidate, "netEvidenceScore", agg.netEvidenceScore);
        putDouble(candidate, "riskScore", agg.riskScore);
        putDouble(candidate, "evidenceRisk", agg.riskScore);
        putDouble(candidate, "confidence", agg.confidence);
        putDouble(candidate, "sampleReliability", agg.sampleReliability);
        putDouble(candidate, "recentValidity", agg.recentValidity);
        putDouble(candidate, "positionNeedScore", clamp01(0.50d + agg.positionCause * 0.50d));
        putDouble(candidate, "numberSupplyScore", clamp01(0.50d + agg.numberSupplyCause * 0.50d));
        putDouble(candidate, "coverageQuality", clamp01(0.50d + agg.coverageCause * 0.50d));
        putDouble(candidate, "coverageQualityScore", clamp01(0.50d + agg.coverageCause * 0.50d));
        putDouble(candidate, "thirdQuality", clamp01(0.50d + agg.thirdCause * 0.50d));
        putDouble(candidate, "thirdNumberQualityScore", clamp01(0.50d + agg.thirdCause * 0.50d));
        putDouble(candidate, "positionEdge", agg.positionCause);
        putDouble(candidate, "comboRoleEdge", agg.comboCause);
        putDouble(candidate, "rankCounterEdge", agg.counterCause);
        putDouble(candidate, "falseStrongRisk", agg.falseStrongCause);
        putDouble(candidate, "ruleDecayRisk", agg.ruleDecayRisk);
        putDouble(candidate, "guardDragRisk", agg.guardDragRisk);
        putDouble(candidate, "positionWrongRisk", agg.positionWrongRisk);
        putInt(candidate, "sampleCount", agg.sampleCount);
        putInt(candidate, "recentSampleCount", agg.recentSampleCount);
        putInt(candidate, "similarSampleCount", agg.sampleCount);
        putInt(candidate, "passCause", passCause);
    }

    private void writeCandidateBlocked(Object candidate, String blockCode, String reason) {
        if (candidate == null) {
            return;
        }
        putString(candidate, "grade", "C");
        putString(candidate, "rejectCode", blockCode);
        putString(candidate, "blockCode", blockCode);
        putString(candidate, "finalDecisionRejectCode", blockCode);
        putString(candidate, "v20Status", "V20_CAUSE_INVALID");
        putString(candidate, "v20ActionAdvice", "HOLD_INVALID_CAUSE_INPUT");
        putDouble(candidate, "causeScore", 0.0d);
        putDouble(candidate, "evidenceScore", 0.0d);
        putDouble(candidate, "antiEvidenceScore", 1.0d);
        putDouble(candidate, "counterEvidenceScore", 1.0d);
        putDouble(candidate, "netEvidenceScore", -1.0d);
        putDouble(candidate, "riskScore", 1.0d);
        putInt(candidate, "passCause", 0);
        putString(candidate, "reasonSummary", reason == null ? "" : reason);
        putString(candidate, "riskSummary", reason == null ? "" : reason);
    }

    private NumberCause buildNumberCause(
            Models.PreDrawFeature f, Object candidate, int position, int number, int roleIndex) {
        NumberCause nc = new NumberCause();
        nc.position = position;
        nc.number = number;
        nc.roleIndex = roleIndex;
        nc.hasFeature = f != null;
        if (f == null) {
            fillMissingNumberCause(nc);
            return nc;
        }

        double freq12 = getDoubleCompat(f, 0.0d, "freq12", "freq16");
        double freq24 = getDoubleCompat(f, 0.0d, "freq24", "freq32");
        double freq48 = getDoubleCompat(f, 0.0d, "freq48", "freq64");
        double freq96 = getDoubleCompat(f, 0.0d, "freq96", "freq120");
        double freq360 = getDoubleCompat(f, 0.0d, "freq360", "freq500", "freq240");
        double freqSlope = getDouble(f, "freqSlope", (freq12 - freq96) * 3.0d);
        double freqAccel = getDouble(f, "freqAccel", (freq12 - freq24) - (freq48 - freq96));
        double shortVsMid = getDouble(f, "shortVsMid", freq12 - freq48);
        double midVsLong = getDouble(f, "midVsLong", freq48 - freq360);
        double trendConsistency = getDouble(f, "trendConsistency", 0.50d);
        int currentGap = getInt(f, "currentGap", 0);
        double avgGap = getDouble(f, "avgGap", 10.0d);
        double gapZ = getDouble(f, "gapZ", 0.0d);
        double gapPhase = getDouble(f, "gapPhase", 0.0d);
        double hotPressure = getDouble(f, "hotPressure", 0.0d);
        double hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);
        double coldRepairPower = getDouble(f, "coldRepairPower", 0.0d);
        double returnZoneScore = getDouble(f, "returnZoneScore", 0.0d);
        double transitionLift = getDouble(f, "transitionLift", 0.0d);
        double transitionConflict = getDouble(f, "transitionConflict", 0.0d);
        double pairCoSupport = getDouble(f, "pairCoSupport", 0.0d);
        double pairConflict = getDouble(f, "pairConflict", 0.0d);
        double breakDepth = getDouble(f, "breakDepth", 0.0d);
        double recoverySpeed = getDouble(f, "recoverySpeed", 0.0d);
        double recoveryQuality = getDouble(f, "recoveryQuality", 0.0d);
        double falseRecoveryRisk = getDouble(f, "falseRecoveryRisk", 0.0d);
        double positionEntropy = getDouble(f, "positionEntropy", 1.0d);
        double positionTop3Mass = getDouble(f, "positionTop3Mass", 0.30d);
        double positionStrengthSlope = getDouble(f, "positionStrengthSlope", 0.0d);
        double positionDrift = getDouble(f, "positionDrift", 0.0d);
        double positionNoiseScore = getDouble(f, "positionNoiseScore", 1.0d);
        double featureScore = getDouble(f, "featureScore", getDouble(f, "healthScore", 0.0d));
        double confidence = getDouble(f, "confidence", 0.0d);
        double predEvidence = getDouble(f, "evidenceScore", -1.0d);
        double predAnti =
                getDouble(f, "antiEvidenceScore", getDouble(f, "counterEvidenceScore", -1.0d));
        double sampleReliability = getDouble(f, "sampleReliability", estimateSampleReliability(f));
        double recentValidity =
                getDouble(
                        f,
                        "recentValidity",
                        estimateRecentValidity(
                                freq12, freq24, freq48, transitionLift, transitionConflict));
        double positionNeed =
                getDouble(
                        f,
                        "positionNeedScore",
                        estimatePositionNeed(
                                returnZoneScore,
                                coldRepairPower,
                                positionTop3Mass,
                                positionNoiseScore));
        double numberSupply =
                getDouble(
                        f,
                        "numberSupplyScore",
                        estimateNumberSupply(
                                featureScore,
                                recoveryQuality,
                                transitionLift,
                                pairCoSupport,
                                falseRecoveryRisk));

        double trendPos = 0.0d;
        trendPos += posPart(freqSlope) * 0.34d;
        trendPos += posPart(freqAccel) * 0.16d;
        trendPos += trendConsistency * 0.16d;
        trendPos += posPart(shortVsMid * 4.5d) * 0.13d;
        trendPos += posPart(midVsLong * 3.5d) * 0.08d;
        trendPos += clamp01((freq96 - 0.06d) * 3.0d) * 0.06d;
        trendPos += recentValidity * 0.07d;

        double trendRisk = 0.0d;
        trendRisk += negPart(freqSlope) * 0.32d;
        trendRisk += negPart(freqAccel) * 0.15d;
        if (trendConsistency < 0.36d) trendRisk += 0.15d;
        if (freq12 < 0.035d && freq48 < 0.060d) trendRisk += 0.12d;
        trendRisk += (1.0d - sampleReliability) * 0.06d;

        double gapPos = 0.0d;
        gapPos += returnZoneScore * 0.40d;
        gapPos += coldRepairPower * 0.28d;
        if (gapZ >= -0.20d && gapZ <= 1.70d) gapPos += 0.14d;
        if (avgGap > 0.0d) {
            double ratio = ((double) currentGap) / Math.max(1.0d, avgGap);
            if (ratio >= 0.70d && ratio <= 1.90d) gapPos += 0.12d;
        }
        gapPos += positionNeed * 0.06d;

        double gapRisk = 0.0d;
        if (gapZ > 2.20d) gapRisk += 0.22d;
        if (gapZ < -0.85d) gapRisk += 0.18d;
        if (gapPhase > 0.86d && returnZoneScore < 0.24d) gapRisk += 0.18d;
        gapRisk += Math.max(0.0d, breakDepth - recoveryQuality) * 0.08d;

        double hotColdPos = 0.0d;
        hotColdPos += hotPressure * (1.0d - hotExhaustion) * 0.34d;
        hotColdPos += coldRepairPower * 0.30d;
        hotColdPos += returnZoneScore * 0.16d;
        hotColdPos += numberSupply * 0.12d;

        double hotColdRisk = 0.0d;
        hotColdRisk += hotExhaustion * 0.40d;
        if (hotPressure > 0.55d && freqSlope < -0.06d) hotColdRisk += 0.18d;
        if (hotPressure < 0.06d && coldRepairPower < 0.15d) hotColdRisk += 0.12d;
        hotColdRisk += falseRecoveryRisk * 0.08d;

        double transitionPos = 0.0d;
        transitionPos += transitionLift * 0.44d;
        transitionPos += pairCoSupport * 0.24d;
        if (transitionLift > 0.12d && pairCoSupport > 0.08d) transitionPos += 0.10d;
        transitionPos += recentValidity * 0.08d;

        double transitionRisk = 0.0d;
        transitionRisk += transitionConflict * 0.42d;
        transitionRisk += pairConflict * 0.26d;
        if (transitionLift < 0.02d && pairCoSupport < 0.02d && confidence >= 0.40d)
            transitionRisk += 0.08d;
        transitionRisk += (1.0d - recentValidity) * 0.08d;

        double positionPos = 0.0d;
        positionPos += (1.0d - positionNoiseScore) * 0.30d;
        positionPos += positionTop3Mass * 0.16d;
        positionPos += posPart(positionStrengthSlope) * 0.17d;
        positionPos += (1.0d - positionEntropy) * 0.10d;
        positionPos += positionNeed * 0.22d;

        double positionRisk = 0.0d;
        positionRisk += positionNoiseScore * 0.30d;
        positionRisk += positionDrift * 0.22d;
        positionRisk += negPart(positionStrengthSlope) * 0.13d;
        if (positionEntropy > 0.92d && positionTop3Mass < 0.33d) positionRisk += 0.12d;
        positionRisk += (1.0d - positionNeed) * 0.06d;

        double recoveryPos = 0.0d;
        recoveryPos += recoveryQuality * 0.38d;
        recoveryPos += recoverySpeed * 0.20d;
        recoveryPos += returnZoneScore * 0.16d;
        recoveryPos += transitionLift * 0.09d;
        recoveryPos += numberSupply * 0.11d;

        double recoveryRisk = 0.0d;
        recoveryRisk += falseRecoveryRisk * 0.40d;
        recoveryRisk += breakDepth * 0.22d;
        if (recoverySpeed > 0.25d && recoveryQuality < 0.30d) recoveryRisk += 0.16d;
        recoveryRisk += (1.0d - numberSupply) * 0.05d;

        if (predEvidence >= 0.0d) {
            featureScore = clamp01(featureScore * 0.70d + predEvidence * 0.30d);
        }
        if (predAnti >= 0.0d) {
            falseRecoveryRisk = clamp01(falseRecoveryRisk * 0.72d + predAnti * 0.28d);
        }

        nc.trendPositive = clamp01(trendPos);
        nc.trendRisk = clamp01(trendRisk);
        nc.gapPositive = clamp01(gapPos);
        nc.gapRisk = clamp01(gapRisk);
        nc.hotColdPositive = clamp01(hotColdPos);
        nc.hotColdRisk = clamp01(hotColdRisk);
        nc.transitionPositive = clamp01(transitionPos);
        nc.transitionRisk = clamp01(transitionRisk);
        nc.positionPositive = clamp01(positionPos);
        nc.positionRisk = clamp01(positionRisk);
        nc.recoveryPositive = clamp01(recoveryPos);
        nc.recoveryRisk = clamp01(recoveryRisk);
        nc.positionNeedScore = clamp01(positionNeed);
        nc.numberSupplyScore = clamp01(numberSupply);
        nc.sampleReliability = clamp01(sampleReliability);
        nc.recentValidity = clamp01(recentValidity);
        nc.featureScore = clamp01(featureScore);
        nc.confidence =
                clamp01(confidence * 0.62d + sampleReliability * 0.20d + recentValidity * 0.18d);
        nc.freqSlope = freqSlope;
        nc.gapZ = gapZ;
        nc.hotPressure = hotPressure;
        nc.falseRecoveryRisk = falseRecoveryRisk;
        nc.positionNoiseScore = positionNoiseScore;
        nc.transitionLift = transitionLift;
        nc.transitionConflict = transitionConflict;
        nc.returnZoneScore = returnZoneScore;
        nc.recoveryQuality = recoveryQuality;
        nc.positive =
                clamp01(
                        nc.trendPositive * 0.15d
                                + nc.gapPositive * 0.14d
                                + nc.hotColdPositive * 0.11d
                                + nc.transitionPositive * 0.14d
                                + nc.positionPositive * 0.12d
                                + nc.recoveryPositive * 0.14d
                                + nc.positionNeedScore * 0.10d
                                + nc.numberSupplyScore * 0.10d);
        nc.risk =
                clamp01(
                        nc.trendRisk * 0.12d
                                + nc.gapRisk * 0.12d
                                + nc.hotColdRisk * 0.12d
                                + nc.transitionRisk * 0.14d
                                + nc.positionRisk * 0.15d
                                + nc.recoveryRisk * 0.18d
                                + falseRecoveryRisk * 0.09d
                                + (1.0d - nc.confidence) * 0.08d);
        nc.stateKey = getString(f, "windowStateKey", getString(f, "stateKey", ""));
        nc.clusterKey = getString(f, "clusterKey", getString(f, "sameClassKey", ""));
        nc.factorKey = getString(f, "factorKey", "P" + position + "_N" + number + "_CAUSE");
        nc.summary = buildNumberSummary(position, number, nc);
        return nc;
    }

    private void fillMissingNumberCause(NumberCause nc) {
        nc.trendPositive = 0.0d;
        nc.trendRisk = 0.30d;
        nc.gapPositive = 0.0d;
        nc.gapRisk = 0.25d;
        nc.hotColdPositive = 0.0d;
        nc.hotColdRisk = 0.20d;
        nc.transitionPositive = 0.0d;
        nc.transitionRisk = 0.20d;
        nc.positionPositive = 0.0d;
        nc.positionRisk = 0.30d;
        nc.recoveryPositive = 0.0d;
        nc.recoveryRisk = 0.30d;
        nc.positionNeedScore = 0.0d;
        nc.numberSupplyScore = 0.0d;
        nc.sampleReliability = 0.0d;
        nc.recentValidity = 0.0d;
        nc.featureScore = 0.0d;
        nc.confidence = 0.0d;
        nc.positive = 0.0d;
        nc.risk = 0.55d;
        nc.stateKey = "NO_FEATURE";
        nc.clusterKey = "NO_FEATURE";
        nc.factorKey = "NO_FEATURE_P" + nc.position + "_N" + nc.number;
        nc.summary = "P" + nc.position + "-N" + nc.number + "：无开奖前特征，按弱证据处理";
    }

    private CauseAggregate aggregate(
            int position,
            int n1,
            int n2,
            int n3,
            int mask,
            NumberCause a,
            NumberCause b,
            NumberCause c,
            Object candidate) {
        CauseAggregate out = new CauseAggregate();
        NumberCause[] arr = new NumberCause[] {a, b, c};
        double trendPos = avg(arr, "trendPositive");
        double trendRisk = avg(arr, "trendRisk");
        double gapPos = avg(arr, "gapPositive");
        double gapRisk = avg(arr, "gapRisk");
        double hotColdPos = avg(arr, "hotColdPositive");
        double hotColdRisk = avg(arr, "hotColdRisk");
        double transitionPos = avg(arr, "transitionPositive");
        double transitionRisk = avg(arr, "transitionRisk");
        double positionPos = avg(arr, "positionPositive");
        double positionRisk = avg(arr, "positionRisk");
        double recoveryPos = avg(arr, "recoveryPositive");
        double recoveryRisk = avg(arr, "recoveryRisk");
        double positionNeed = avg(arr, "positionNeedScore");
        double numberSupply = avg(arr, "numberSupplyScore");
        double featureAvg = avg(arr, "featureScore");
        double featureMin = min(arr, "featureScore");
        double confidenceAvg = avg(arr, "confidence");
        double riskMax = max(arr, "risk");
        double positiveMin = min(arr, "positive");
        double sampleReliability = avg(arr, "sampleReliability");
        double recentValidity = avg(arr, "recentValidity");

        out.trendCause = clampSigned((trendPos - trendRisk) * 1.25d);
        out.gapCause = clampSigned((gapPos - gapRisk) * 1.18d);
        out.hotColdCause = clampSigned((hotColdPos - hotColdRisk) * 1.10d);
        out.transitionCause = clampSigned((transitionPos - transitionRisk) * 1.16d);
        out.positionCause = clampSigned((positionPos - positionRisk) * 1.08d);
        out.recoveryCause = clampSigned((recoveryPos - recoveryRisk) * 1.22d);
        out.numberSupplyCause = clampSigned((numberSupply - 0.50d) * 1.36d);
        out.coverageCause = calcCoverageCause(n1, n2, n3, arr, candidate);
        out.thirdCause = calcThirdCause(c, arr, candidate);
        out.comboCause = calcComboCause(n1, n2, n3, mask, a, b, c, candidate);
        out.falseStrongCause = calcFalseStrongRisk(a, b, c, featureMin, riskMax, candidate);
        out.counterCause = calcCounterCause(a, b, c, candidate);

        double positive = 0.0d;
        positive += posPart(out.trendCause) * 0.12d;
        positive += posPart(out.gapCause) * 0.12d;
        positive += posPart(out.hotColdCause) * 0.08d;
        positive += posPart(out.transitionCause) * 0.12d;
        positive += posPart(out.positionCause) * 0.08d;
        positive += posPart(out.recoveryCause) * 0.12d;
        positive += posPart(out.numberSupplyCause) * 0.11d;
        positive += posPart(out.coverageCause) * 0.10d;
        positive += posPart(out.thirdCause) * 0.06d;
        positive += posPart(out.comboCause) * 0.06d;
        positive += posPart(out.counterCause) * 0.03d;

        double negative = 0.0d;
        negative += negPart(out.trendCause) * 0.08d;
        negative += negPart(out.gapCause) * 0.08d;
        negative += negPart(out.hotColdCause) * 0.08d;
        negative += negPart(out.transitionCause) * 0.11d;
        negative += negPart(out.positionCause) * 0.10d;
        negative += negPart(out.recoveryCause) * 0.10d;
        negative += negPart(out.numberSupplyCause) * 0.09d;
        negative += negPart(out.coverageCause) * 0.08d;
        negative += negPart(out.thirdCause) * 0.07d;
        negative += negPart(out.comboCause) * 0.08d;
        negative += out.falseStrongCause * 0.13d;

        out.positiveScore = clamp01(positive);
        out.negativeScore = clamp01(negative);
        out.evidenceScore = out.positiveScore;
        out.antiEvidenceScore =
                clamp01(out.negativeScore * 0.72d + out.falseStrongCause * 0.18d + riskMax * 0.10d);
        out.netEvidenceScore = clampSigned(out.evidenceScore - out.antiEvidenceScore);
        out.riskScore =
                clamp01(
                        out.antiEvidenceScore * 0.40d
                                + out.falseStrongCause * 0.23d
                                + riskMax * 0.14d
                                + (1.0d - confidenceAvg) * 0.08d
                                + (1.0d - sampleReliability) * 0.06d
                                + (1.0d - recentValidity) * 0.05d
                                + (positiveMin < 0.18d ? 0.04d : 0.0d));
        out.confidence =
                clamp01(
                        confidenceAvg * 0.42d
                                + sampleReliability * 0.18d
                                + recentValidity * 0.16d
                                + featureAvg * 0.12d
                                + (1.0d - riskMax) * 0.07d
                                + (isDistinctK3(n1, n2, n3) ? 0.05d : 0.0d));
        out.finalScore =
                clamp01(
                        0.30d
                                + out.evidenceScore * 0.72d
                                - out.antiEvidenceScore * 0.58d
                                - out.falseStrongCause * 0.14d
                                + out.confidence * 0.12d
                                + sampleReliability * 0.06d
                                + recentValidity * 0.05d);
        out.ruleDecayRisk = clamp01(out.negativeScore * 0.50d + (1.0d - out.confidence) * 0.25d);
        out.guardDragRisk = clamp01(max3(transitionRisk, positionRisk, recoveryRisk));
        out.positionWrongRisk = clamp01(positionRisk * 0.55d + riskMax * 0.20d);
        out.factorCount = 11;
        out.strongFactorCount =
                countStrong(
                        out.trendCause,
                        out.gapCause,
                        out.hotColdCause,
                        out.transitionCause,
                        out.positionCause,
                        out.recoveryCause,
                        out.numberSupplyCause,
                        out.coverageCause,
                        out.thirdCause,
                        out.comboCause,
                        out.counterCause);
        out.riskFactorCount =
                countRisk(
                        out.trendCause,
                        out.gapCause,
                        out.hotColdCause,
                        out.transitionCause,
                        out.positionCause,
                        out.recoveryCause,
                        out.numberSupplyCause,
                        out.coverageCause,
                        out.thirdCause,
                        out.comboCause,
                        -out.falseStrongCause);
        out.featureAvg = featureAvg;
        out.featureMin = featureMin;
        out.riskMax = riskMax;
        out.positiveMin = positiveMin;
        out.sampleReliability = sampleReliability;
        out.recentValidity = recentValidity;
        out.sampleCount = estimateSampleCount(candidate, arr);
        out.recentSampleCount = estimateRecentSampleCount(candidate, arr);
        out.positionNeedScore = positionNeed;
        out.numberSupplyScore = numberSupply;
        return out;
    }

    private double calcComboCause(
            int n1,
            int n2,
            int n3,
            int mask,
            NumberCause a,
            NumberCause b,
            NumberCause c,
            Object candidate) {
        double score = 0.0d;
        if (isDistinctK3(n1, n2, n3)) {
            score += 0.18d;
        } else {
            score -= 0.35d;
        }
        NumberCause[] arr = new NumberCause[] {a, b, c};
        double minPositive = min(arr, "positive");
        double maxRisk = max(arr, "risk");
        double avgTransition = avg(arr, "transitionPositive");
        double avgConflict = avg(arr, "transitionRisk");
        if (minPositive >= 0.24d) {
            score += 0.16d;
        } else if (minPositive < 0.10d) {
            score -= 0.16d;
        }
        if (avgTransition > avgConflict) {
            score += Math.min(0.16d, (avgTransition - avgConflict) * 0.48d);
        } else {
            score -= Math.min(0.16d, (avgConflict - avgTransition) * 0.48d);
        }
        if (maxRisk >= 0.68d) {
            score -= 0.18d;
        }
        double oldComboEdge = getDouble(candidate, "comboRoleEdge", 0.0d);
        double rawScore = getDouble(candidate, "rawScore", getDouble(candidate, "scanScore", 0.0d));
        double scanScore =
                getDouble(candidate, "featureScore", getDouble(candidate, "healthScore", 0.0d));
        score += clampSigned(oldComboEdge) * 0.12d;
        score += clampSigned((rawScore - 0.50d) * 0.30d);
        score += clampSigned((scanScore - 0.50d) * 0.22d);
        return clampSigned(score);
    }

    private double calcCoverageCause(int n1, int n2, int n3, NumberCause[] arr, Object candidate) {
        double coverage =
                getDouble(
                        candidate,
                        "coverageQuality",
                        getDouble(candidate, "coverageQualityScore", -1.0d));
        if (coverage >= 0.0d) {
            return clampSigned((coverage - 0.50d) * 1.40d);
        }
        double roleDiversity = 0.0d;
        if (isLow(n1) != isLow(n2)) roleDiversity += 0.12d;
        if (isOdd(n1) != isOdd(n2)) roleDiversity += 0.08d;
        if (sameTail(n1, n2) || sameTail(n1, n3) || sameTail(n2, n3)) roleDiversity += 0.05d;
        double minPositive = min(arr, "positive");
        double avgSupply = avg(arr, "numberSupplyScore");
        double maxRisk = max(arr, "risk");
        double score = minPositive * 0.35d + avgSupply * 0.35d + roleDiversity - maxRisk * 0.24d;
        return clampSigned((score - 0.28d) * 1.60d);
    }

    private double calcThirdCause(NumberCause third, NumberCause[] arr, Object candidate) {
        double thirdQuality =
                getDouble(
                        candidate,
                        "thirdQuality",
                        getDouble(candidate, "thirdNumberQualityScore", -1.0d));
        if (thirdQuality >= 0.0d) {
            return clampSigned((thirdQuality - 0.50d) * 1.35d);
        }
        if (third == null) {
            return -0.30d;
        }
        double avgPositive = avg(arr, "positive");
        double avgRisk = avg(arr, "risk");
        double quality =
                third.positive * 0.38d
                        + third.numberSupplyScore * 0.26d
                        + third.recentValidity * 0.16d
                        + third.sampleReliability * 0.10d
                        + (1.0d - third.risk) * 0.10d;
        double drag =
                Math.max(0.0d, avgPositive - third.positive) * 0.34d
                        + Math.max(0.0d, third.risk - avgRisk) * 0.30d;
        return clampSigned((quality - 0.42d) * 1.30d - drag);
    }

    private double calcFalseStrongRisk(
            NumberCause a,
            NumberCause b,
            NumberCause c,
            double featureMin,
            double riskMax,
            Object candidate) {
        NumberCause[] arr = new NumberCause[] {a, b, c};
        double risk = 0.0d;
        risk += avg(arr, "falseRecoveryRisk") * 0.22d;
        risk += avg(arr, "positionNoiseScore") * 0.17d;
        risk += avg(arr, "transitionRisk") * 0.13d;
        risk += avg(arr, "recoveryRisk") * 0.15d;
        risk += riskMax * 0.13d;
        risk += (1.0d - avg(arr, "sampleReliability")) * 0.08d;
        risk += (1.0d - avg(arr, "recentValidity")) * 0.06d;
        if (featureMin < 0.18d) risk += 0.13d;
        double oldFalseStrong = getDouble(candidate, "falseStrongRisk", 0.0d);
        if (oldFalseStrong > 0.0d) risk = Math.max(risk, oldFalseStrong);
        return clamp01(risk);
    }

    private double calcCounterCause(NumberCause a, NumberCause b, NumberCause c, Object candidate) {
        NumberCause[] arr = new NumberCause[] {a, b, c};
        double avgPositive = avg(arr, "positive");
        double avgRisk = avg(arr, "risk");
        double minPositive = min(arr, "positive");
        double maxRisk = max(arr, "risk");
        double counter = 0.0d;
        if (avgPositive >= 0.30d && avgRisk <= 0.38d) counter += 0.16d;
        if (minPositive >= 0.20d) counter += 0.10d;
        if (avg(arr, "sampleReliability") >= 0.42d) counter += 0.08d;
        if (avg(arr, "recentValidity") >= 0.45d) counter += 0.08d;
        if (maxRisk >= 0.68d) counter -= 0.22d;
        double oldRankCounter = getDouble(candidate, "rankCounterEdge", 0.0d);
        counter += clampSigned(oldRankCounter) * 0.14d;
        double seedSimilarity = getDouble(candidate, "seedSimilarity", 0.0d);
        if (seedSimilarity > 0.0d) counter += clampSigned((seedSimilarity - 0.50d) * 0.22d);
        return clampSigned(counter);
    }

    private boolean passCause(double score, double risk, double confidence) {
        double pass = readDouble("V20_PASS_CAUSE_SCORE", DEFAULT_PASS_CAUSE);
        if (score < pass) return false;
        if (risk >= HARD_RISK_LINE) return false;
        if (risk >= MID_RISK_LINE && confidence < 0.56d) return false;
        return true;
    }

    private String gradeOf(double score, double risk, double confidence) {
        double sLine = readDouble("V20_GRADE_S_MIN_SCORE", DEFAULT_GRADE_S);
        double aLine = readDouble("V20_GRADE_A_MIN_SCORE", DEFAULT_GRADE_A);
        double bLine = readDouble("V20_GRADE_B_MIN_SCORE", DEFAULT_GRADE_B);
        if (risk >= HARD_RISK_LINE) return "C";
        if (score >= sLine && confidence >= 0.62d && risk < 0.46d) return "S";
        if (score >= aLine && confidence >= 0.50d && risk < 0.58d) return "A";
        if (score >= bLine && risk < 0.68d) return "B";
        return "C";
    }

    private String blockCodeOf(double score, double risk, double confidence) {
        double pass = readDouble("V20_PASS_CAUSE_SCORE", DEFAULT_PASS_CAUSE);
        if (risk >= HARD_RISK_LINE) return "CAUSE_HARD_RISK";
        if (risk >= MID_RISK_LINE && confidence < 0.56d) return "CAUSE_RISK_CONFIDENCE_WEAK";
        if (score < pass) return "CAUSE_SCORE_NOT_ENOUGH";
        return "CAUSE_WATCH";
    }

    private String buildV20Status(String grade, int passCause, CauseAggregate a) {
        if (a == null) return "V20_CAUSE_EMPTY";
        if (a.riskScore >= HARD_RISK_LINE) return "V20_CAUSE_RISK";
        if (passCause == 1 && ("S".equals(grade) || "A".equals(grade))) return "V20_CAUSE_PASS";
        if (passCause == 1) return "V20_CAUSE_WATCH_PASS";
        if (a.finalScore >= DEFAULT_PASS_CAUSE * 0.92d && a.riskScore < MID_RISK_LINE)
            return "V20_CAUSE_WATCH";
        return "V20_CAUSE_WEAK";
    }

    private String buildActionAdvice(String grade, int passCause, CauseAggregate a) {
        if (a == null) return "WATCH_EMPTY_CAUSE";
        if (a.riskScore >= HARD_RISK_LINE) return "FREEZE_CAUSE_HARD_RISK";
        if (a.falseStrongCause >= 0.68d) return "WATCH_FALSE_STRONG";
        if (passCause == 1 && ("S".equals(grade) || "A".equals(grade)))
            return "PASS_CAUSE_TO_NEXT_GATE";
        if (passCause == 1) return "WATCH_PASS_TO_NEXT_GATE";
        if (a.finalScore >= DEFAULT_PASS_CAUSE * 0.92d && a.riskScore < MID_RISK_LINE)
            return "WATCH_NEAR_PASS";
        return "WATCH_CAUSE_NOT_ENOUGH";
    }

    private void rerankCandidatesByCause(List candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }
        Collections.sort(
                candidates,
                new Comparator() {
                    public int compare(Object a, Object b) {
                        double sa = getDouble(a, "causeScore", getDouble(a, "evidenceScore", 0.0d));
                        double sb = getDouble(b, "causeScore", getDouble(b, "evidenceScore", 0.0d));
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        double ca = getDouble(a, "confidence", 0.0d);
                        double cb = getDouble(b, "confidence", 0.0d);
                        if (ca > cb) return -1;
                        if (ca < cb) return 1;
                        return 0;
                    }
                });
        for (int i = 0; i < candidates.size(); i++) {
            Object c = candidates.get(i);
            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
                putInt(c, "causalRank", i + 1);
            }
        }
    }

    private void sortChains(ArrayList<Models.CauseChain> chains) {
        if (chains == null || chains.size() <= 1) {
            return;
        }
        Collections.sort(
                chains,
                new Comparator<Models.CauseChain>() {
                    public int compare(Models.CauseChain a, Models.CauseChain b) {
                        double sa =
                                getDouble(a, "finalCauseScore", getDouble(a, "causeScore", 0.0d));
                        double sb =
                                getDouble(b, "finalCauseScore", getDouble(b, "causeScore", 0.0d));
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        double ca = getDouble(a, "confidence", 0.0d);
                        double cb = getDouble(b, "confidence", 0.0d);
                        if (ca > cb) return -1;
                        if (ca < cb) return 1;
                        return 0;
                    }
                });
    }

    private Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> map, int position, int number) {
        if (map == null || map.size() == 0) return null;
        Models.PreDrawFeature f = map.get(keyOf(position, number));
        if (f != null) return f;
        f = map.get(String.valueOf(position) + "|" + String.valueOf(number));
        if (f != null) return f;
        f = map.get("P" + position + "_N" + number);
        if (f != null) return f;
        f = map.get("P" + position + "|N" + number);
        return f;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private String buildCandidateKey(String targetIssue, int position, int n1, int n2, int n3) {
        return safe(targetIssue) + "|P" + position + "|K3_" + n1 + "_" + n2 + "_" + n3;
    }

    private String buildChainKey(String targetIssue, String candidateKey, String stateKey) {
        return "CAUSE|"
                + safe(targetIssue)
                + "|"
                + cleanKey(candidateKey)
                + "|"
                + cleanKey(stateKey);
    }

    private String buildStateKey(
            int position,
            int n1,
            int n2,
            int n3,
            NumberCause a,
            NumberCause b,
            NumberCause c,
            CauseAggregate agg) {
        StringBuilder sb = new StringBuilder();
        sb.append("CAUSE|P").append(position);
        sb.append("|K3=").append(n1).append("_").append(n2).append("_").append(n3);
        sb.append("|TR=").append(bucketSigned(avg(new NumberCause[] {a, b, c}, "freqSlope")));
        sb.append("|GAP=").append(bucketGap(avg(new NumberCause[] {a, b, c}, "gapZ")));
        sb.append("|SUP=").append(bucket01(avg(new NumberCause[] {a, b, c}, "numberSupplyScore")));
        sb.append("|COV=").append(bucketSigned(agg.coverageCause));
        sb.append("|TH=").append(bucketSigned(agg.thirdCause));
        sb.append("|RISK=").append(bucket01(agg.riskScore));
        sb.append("|GRADE=").append(gradeOf(agg.finalScore, agg.riskScore, agg.confidence));
        return sb.toString();
    }

    private String buildClusterKey(
            int position,
            int n1,
            int n2,
            int n3,
            NumberCause a,
            NumberCause b,
            NumberCause c,
            CauseAggregate agg,
            String grade) {
        StringBuilder sb = new StringBuilder();
        sb.append("CF_CLUSTER|P").append(position);
        sb.append("|G").append(safe(grade));
        sb.append("|NEED_").append(bucket01(agg.positionNeedScore));
        sb.append("|SUP_").append(bucket01(agg.numberSupplyScore));
        sb.append("|NET_").append(bucketSigned(agg.netEvidenceScore));
        sb.append("|R_").append(bucket01(agg.riskScore));
        sb.append("|K3_").append(k3Shape(n1, n2, n3));
        return sb.toString();
    }

    private String buildFactorKey(int position, int n1, int n2, int n3, CauseAggregate agg) {
        StringBuilder sb = new StringBuilder();
        sb.append("CF_FACTOR|P").append(position);
        sb.append("|K3_").append(n1).append("_").append(n2).append("_").append(n3);
        sb.append("|E_").append(bucket01(agg.evidenceScore));
        sb.append("|A_").append(bucket01(agg.antiEvidenceScore));
        sb.append("|S_").append(bucket01(agg.sampleReliability));
        sb.append("|RV_").append(bucket01(agg.recentValidity));
        return sb.toString();
    }

    private String buildPositiveText(int position, int n1, int n2, int n3, CauseAggregate a) {
        StringBuilder sb = new StringBuilder();
        sb.append("正向因子矩阵｜位置=")
                .append(position)
                .append("｜K3=")
                .append(n1)
                .append(",")
                .append(n2)
                .append(",")
                .append(n3);
        appendCause(sb, "趋势因", a.trendCause);
        appendCause(sb, "间隔因", a.gapCause);
        appendCause(sb, "冷热因", a.hotColdCause);
        appendCause(sb, "转移因", a.transitionCause);
        appendCause(sb, "位置需求", a.positionCause);
        appendCause(sb, "号码供给", a.numberSupplyCause);
        appendCause(sb, "K3覆盖", a.coverageCause);
        appendCause(sb, "第三号", a.thirdCause);
        appendCause(sb, "恢复因", a.recoveryCause);
        appendCause(sb, "组合协同", a.comboCause);
        appendCause(sb, "反事实支撑", a.counterCause);
        sb.append("｜正因总分=").append(percent(a.positiveScore));
        sb.append("｜样本可信=").append(percent(a.sampleReliability));
        sb.append("｜近期有效=").append(percent(a.recentValidity));
        return sb.toString();
    }

    private String buildNegativeText(int position, int n1, int n2, int n3, CauseAggregate a) {
        StringBuilder sb = new StringBuilder();
        sb.append("反向/风险因子矩阵｜位置=")
                .append(position)
                .append("｜K3=")
                .append(n1)
                .append(",")
                .append(n2)
                .append(",")
                .append(n3);
        if (a.falseStrongCause >= 0.35d) sb.append("｜伪强风险=").append(percent(a.falseStrongCause));
        if (a.ruleDecayRisk >= 0.35d) sb.append("｜规则衰减=").append(percent(a.ruleDecayRisk));
        if (a.guardDragRisk >= 0.35d) sb.append("｜保护拖拽=").append(percent(a.guardDragRisk));
        if (a.positionWrongRisk >= 0.35d)
            sb.append("｜位置错判风险=").append(percent(a.positionWrongRisk));
        if (a.thirdCause <= -0.18d) sb.append("｜第三号拖累=").append(signed(a.thirdCause));
        if (a.coverageCause <= -0.18d) sb.append("｜K3覆盖不足=").append(signed(a.coverageCause));
        sb.append("｜风险总分=").append(percent(a.riskScore));
        sb.append("｜反因总分=").append(percent(a.negativeScore));
        if (a.riskScore < 0.35d) sb.append("｜风险结构较干净");
        else if (a.riskScore < 0.55d) sb.append("｜风险中等，需要后续门控确认");
        else sb.append("｜风险偏高，后续门控应谨慎");
        return sb.toString();
    }

    private String buildSummaryText(
            int position,
            int n1,
            int n2,
            int n3,
            String grade,
            int passCause,
            double finalScore,
            double positive,
            double negative,
            double risk,
            double confidence,
            CauseAggregate agg,
            String actionAdvice) {
        StringBuilder sb = new StringBuilder();
        sb.append("V20因果因子矩阵｜位置=")
                .append(position)
                .append("｜K3=")
                .append(n1)
                .append(",")
                .append(n2)
                .append(",")
                .append(n3);
        sb.append("｜等级=").append(grade);
        sb.append("｜因链分=").append(percent(finalScore));
        sb.append("｜正因=").append(percent(positive));
        sb.append("｜反因=").append(percent(negative));
        sb.append("｜净证据=").append(signed(agg.netEvidenceScore));
        sb.append("｜风险=").append(percent(risk));
        sb.append("｜置信=").append(percent(confidence));
        sb.append("｜样本可信=").append(percent(agg.sampleReliability));
        sb.append("｜近期有效=").append(percent(agg.recentValidity));
        sb.append("｜强因子=").append(agg.strongFactorCount).append("/").append(agg.factorCount);
        sb.append("｜风险因子=").append(agg.riskFactorCount);
        sb.append("｜动作建议=").append(actionAdvice);
        if (passCause == 1) sb.append("｜因果层通过，交给后续概率/动态门/DecisionGate");
        else sb.append("｜因果层未通过：").append(blockCodeOf(finalScore, risk, confidence));
        sb.append("｜说明=本文件只沉淀因链证据，不创建BET，最终裁决仍由DecisionGateEngine执行");
        return sb.toString();
    }

    private String buildNumberSummary(int position, int number, NumberCause n) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(position).append("-N").append(number);
        sb.append("：趋势=").append(signed(n.trendPositive - n.trendRisk));
        sb.append("，间隔=").append(signed(n.gapPositive - n.gapRisk));
        sb.append("，冷热=").append(signed(n.hotColdPositive - n.hotColdRisk));
        sb.append("，转移=").append(signed(n.transitionPositive - n.transitionRisk));
        sb.append("，位置需求=").append(percent(n.positionNeedScore));
        sb.append("，号码供给=").append(percent(n.numberSupplyScore));
        sb.append("，恢复=").append(signed(n.recoveryPositive - n.recoveryRisk));
        sb.append("，风险=").append(percent(n.risk));
        return sb.toString();
    }

    private void appendCause(StringBuilder sb, String name, double value) {
        if (sb == null || name == null) return;
        sb.append("｜").append(name).append("=").append(signed(value));
    }

    private static double avg(NumberCause[] arr, String field) {
        if (arr == null || arr.length == 0) return 0.0d;
        double sum = 0.0d;
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            NumberCause n = arr[i];
            if (n == null) continue;
            sum += readNumberCauseField(n, field);
            count++;
        }
        if (count <= 0) return 0.0d;
        return sum / ((double) count);
    }

    private static double min(NumberCause[] arr, String field) {
        if (arr == null || arr.length == 0) return 0.0d;
        double v = 999.0d;
        boolean ok = false;
        for (int i = 0; i < arr.length; i++) {
            NumberCause n = arr[i];
            if (n == null) continue;
            double x = readNumberCauseField(n, field);
            if (!ok || x < v) {
                v = x;
                ok = true;
            }
        }
        return ok ? v : 0.0d;
    }

    private static double max(NumberCause[] arr, String field) {
        if (arr == null || arr.length == 0) return 0.0d;
        double v = -999.0d;
        boolean ok = false;
        for (int i = 0; i < arr.length; i++) {
            NumberCause n = arr[i];
            if (n == null) continue;
            double x = readNumberCauseField(n, field);
            if (!ok || x > v) {
                v = x;
                ok = true;
            }
        }
        return ok ? v : 0.0d;
    }

    private static double readNumberCauseField(NumberCause n, String field) {
        if (n == null || field == null) return 0.0d;
        if ("trendPositive".equals(field)) return n.trendPositive;
        if ("trendRisk".equals(field)) return n.trendRisk;
        if ("gapPositive".equals(field)) return n.gapPositive;
        if ("gapRisk".equals(field)) return n.gapRisk;
        if ("hotColdPositive".equals(field)) return n.hotColdPositive;
        if ("hotColdRisk".equals(field)) return n.hotColdRisk;
        if ("transitionPositive".equals(field)) return n.transitionPositive;
        if ("transitionRisk".equals(field)) return n.transitionRisk;
        if ("positionPositive".equals(field)) return n.positionPositive;
        if ("positionRisk".equals(field)) return n.positionRisk;
        if ("recoveryPositive".equals(field)) return n.recoveryPositive;
        if ("recoveryRisk".equals(field)) return n.recoveryRisk;
        if ("positionNeedScore".equals(field)) return n.positionNeedScore;
        if ("numberSupplyScore".equals(field)) return n.numberSupplyScore;
        if ("sampleReliability".equals(field)) return n.sampleReliability;
        if ("recentValidity".equals(field)) return n.recentValidity;
        if ("featureScore".equals(field)) return n.featureScore;
        if ("confidence".equals(field)) return n.confidence;
        if ("positive".equals(field)) return n.positive;
        if ("risk".equals(field)) return n.risk;
        if ("falseRecoveryRisk".equals(field)) return n.falseRecoveryRisk;
        if ("positionNoiseScore".equals(field)) return n.positionNoiseScore;
        if ("freqSlope".equals(field)) return n.freqSlope;
        if ("gapZ".equals(field)) return n.gapZ;
        if ("transitionLift".equals(field)) return n.transitionLift;
        return 0.0d;
    }

    private int estimateSampleCount(Object candidate, NumberCause[] arr) {
        int sample =
                getInt(
                        candidate,
                        "similarSampleCount",
                        getInt(candidate, "effectiveSample", getInt(candidate, "stateSample", 0)));
        if (sample > 0) return sample;
        double rel = avg(arr, "sampleReliability");
        return (int) Math.round(rel * 360.0d);
    }

    private int estimateRecentSampleCount(Object candidate, NumberCause[] arr) {
        int sample = getInt(candidate, "recentSampleCount", getInt(candidate, "recentSample", 0));
        if (sample > 0) return sample;
        double rel = avg(arr, "recentValidity");
        return (int) Math.round(rel * 96.0d);
    }

    private double estimateSampleReliability(Object f) {
        int history = getInt(f, "historySize", getInt(f, "visibleDrawCount", 0));
        if (history <= 0) return 0.0d;
        double v = ((double) history) / 360.0d;
        return clamp01(v);
    }

    private double estimateRecentValidity(
            double f12, double f24, double f48, double lift, double conflict) {
        double v = 0.50d;
        v += clampSigned((f12 - f48) * 3.5d) * 0.18d;
        v += clampSigned((f24 - f48) * 2.5d) * 0.12d;
        v += lift * 0.18d;
        v -= conflict * 0.18d;
        return clamp01(v);
    }

    private double estimatePositionNeed(
            double returnZone, double coldRepair, double topMass, double noise) {
        return clamp01(
                returnZone * 0.34d + coldRepair * 0.26d + topMass * 0.18d + (1.0d - noise) * 0.22d);
    }

    private double estimateNumberSupply(
            double feature, double recovery, double lift, double pair, double risk) {
        return clamp01(
                feature * 0.30d
                        + recovery * 0.22d
                        + lift * 0.18d
                        + pair * 0.12d
                        + (1.0d - risk) * 0.18d);
    }

    private static int countStrong(
            double a,
            double b,
            double c,
            double d,
            double e,
            double f,
            double g,
            double h,
            double i,
            double j,
            double k) {
        int count = 0;
        if (a >= 0.18d) count++;
        if (b >= 0.18d) count++;
        if (c >= 0.18d) count++;
        if (d >= 0.18d) count++;
        if (e >= 0.18d) count++;
        if (f >= 0.18d) count++;
        if (g >= 0.18d) count++;
        if (h >= 0.18d) count++;
        if (i >= 0.18d) count++;
        if (j >= 0.18d) count++;
        if (k >= 0.18d) count++;
        return count;
    }

    private static int countRisk(
            double a,
            double b,
            double c,
            double d,
            double e,
            double f,
            double g,
            double h,
            double i,
            double j,
            double k) {
        int count = 0;
        if (a <= -0.18d) count++;
        if (b <= -0.18d) count++;
        if (c <= -0.18d) count++;
        if (d <= -0.18d) count++;
        if (e <= -0.18d) count++;
        if (f <= -0.18d) count++;
        if (g <= -0.18d) count++;
        if (h <= -0.18d) count++;
        if (i <= -0.18d) count++;
        if (j <= -0.18d) count++;
        if (k <= -0.35d) count++;
        return count;
    }

    private static boolean isValidPosition(int position) {
        return position >= POSITION_MIN && position <= POSITION_MAX;
    }

    private static boolean isValidNumber(int number) {
        return number >= NUMBER_MIN && number <= NUMBER_MAX;
    }

    private static boolean isDistinctK3(int n1, int n2, int n3) {
        return isValidNumber(n1)
                && isValidNumber(n2)
                && isValidNumber(n3)
                && n1 != n2
                && n1 != n3
                && n2 != n3;
    }

    private static int buildMask(int n1, int n2, int n3) {
        int mask = 0;
        if (isValidNumber(n1)) mask |= (1 << n1);
        if (isValidNumber(n2)) mask |= (1 << n2);
        if (isValidNumber(n3)) mask |= (1 << n3);
        return mask;
    }

    private static boolean isValidK3Mask(int mask) {
        if (mask <= 0) return false;
        int count = 0;
        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            if ((mask & (1 << n)) != 0) count++;
        }
        return count == K_SIZE;
    }

    private static boolean isLow(int n) {
        return n <= 5;
    }

    private static boolean isOdd(int n) {
        return (n % 2) != 0;
    }

    private static boolean sameTail(int a, int b) {
        return (a % 10) == (b % 10);
    }

    private static String k3Shape(int n1, int n2, int n3) {
        int odd = 0;
        int low = 0;
        if (isOdd(n1)) odd++;
        if (isOdd(n2)) odd++;
        if (isOdd(n3)) odd++;
        if (isLow(n1)) low++;
        if (isLow(n2)) low++;
        if (isLow(n3)) low++;
        return "O" + odd + "L" + low;
    }

    private static double max3(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double posPart(double v) {
        if (v <= 0.0d) return 0.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private static double negPart(double v) {
        if (v >= 0.0d) return 0.0d;
        v = -v;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private static double clamp01(double v) {
        if (v < 0.0d) return 0.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private static double clampSigned(double v) {
        if (v < -1.0d) return -1.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private static String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private static String signed(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private static String bucketSigned(double v) {
        if (v >= 0.18d) return "UP_STRONG";
        if (v >= 0.06d) return "UP";
        if (v <= -0.18d) return "DOWN_STRONG";
        if (v <= -0.06d) return "DOWN";
        return "FLAT";
    }

    private static String bucketGap(double z) {
        if (z >= 1.80d) return "OVERDUE";
        if (z >= 0.45d) return "RETURN_ZONE";
        if (z <= -0.80d) return "TOO_SHORT";
        return "NORMAL";
    }

    private static String bucket01(double v) {
        v = clamp01(v);
        if (v >= 0.75d) return "HIGH";
        if (v >= 0.50d) return "MID_HIGH";
        if (v >= 0.25d) return "MID";
        if (v > 0.0d) return "LOW";
        return "ZERO";
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        return "";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String cleanKey(String s) {
        if (s == null) return "";
        String out = s.replace('|', '_');
        out = out.replace(' ', '_');
        out = out.replace('\n', '_');
        out = out.replace('\r', '_');
        out = out.replace('\t', '_');
        if (out.length() > 120) out = out.substring(0, 120);
        return out;
    }

    private static int getInt(Object obj, String field, int fallback) {
        if (obj == null || field == null || field.length() == 0) return fallback;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Integer) return ((Integer) v).intValue();
            if (v instanceof Long) return (int) ((Long) v).longValue();
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
        }
        return fallback;
    }

    private static double getDouble(Object obj, String field, double fallback) {
        if (obj == null || field == null || field.length() == 0) return fallback;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Double) return ((Double) v).doubleValue();
            if (v instanceof Float) return ((Float) v).doubleValue();
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
        }
        return fallback;
    }

    private static double getDoubleCompat(Object obj, double fallback, String a, String b) {
        double v = getDouble(obj, a, -999999.0d);
        if (v != -999999.0d) return v;
        return getDouble(obj, b, fallback);
    }

    private static double getDoubleCompat(
            Object obj, double fallback, String a, String b, String c) {
        double v = getDouble(obj, a, -999999.0d);
        if (v != -999999.0d) return v;
        v = getDouble(obj, b, -999999.0d);
        if (v != -999999.0d) return v;
        return getDouble(obj, c, fallback);
    }

    private static String getString(Object obj, String field, String fallback) {
        if (obj == null || field == null || field.length() == 0) return fallback;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return fallback;
    }

    private static void putInt(Object obj, String field, int value) {
        if (obj == null || field == null || field.length() == 0) return;
        try {
            Field f = obj.getClass().getField(field);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putLong(Object obj, String field, long value) {
        if (obj == null || field == null || field.length() == 0) return;
        try {
            Field f = obj.getClass().getField(field);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putDouble(Object obj, String field, double value) {
        if (obj == null || field == null || field.length() == 0) return;
        try {
            Field f = obj.getClass().getField(field);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putString(Object obj, String field, String value) {
        if (obj == null || field == null || field.length() == 0) return;
        try {
            Field f = obj.getClass().getField(field);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private static double readDouble(String fieldName, double fallback) {
        double v = readDoubleFromClass(AppConfig.class, fieldName, -999999.0d);
        if (v != -999999.0d) return v;
        v = readDoubleFromClass(Models.class, fieldName, -999999.0d);
        if (v != -999999.0d) return v;
        return fallback;
    }

    private static double readDoubleFromClass(Class cls, String fieldName, double fallback) {
        if (cls == null || fieldName == null || fieldName.length() == 0) return fallback;
        try {
            Field f = cls.getField(fieldName);
            Object v = f.get(null);
            if (v instanceof Double) return ((Double) v).doubleValue();
            if (v instanceof Float) return ((Float) v).doubleValue();
            if (v instanceof Integer) return ((Integer) v).doubleValue();
            if (v instanceof Long) return ((Long) v).doubleValue();
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
        }
        return fallback;
    }

    private static final class NumberCause {
        int position;
        int number;
        int roleIndex;
        boolean hasFeature;
        double trendPositive;
        double trendRisk;
        double gapPositive;
        double gapRisk;
        double hotColdPositive;
        double hotColdRisk;
        double transitionPositive;
        double transitionRisk;
        double positionPositive;
        double positionRisk;
        double recoveryPositive;
        double recoveryRisk;
        double positionNeedScore;
        double numberSupplyScore;
        double sampleReliability;
        double recentValidity;
        double featureScore;
        double confidence;
        double positive;
        double risk;
        double freqSlope;
        double gapZ;
        double hotPressure;
        double falseRecoveryRisk;
        double positionNoiseScore;
        double transitionLift;
        double transitionConflict;
        double returnZoneScore;
        double recoveryQuality;
        String stateKey;
        String clusterKey;
        String factorKey;
        String summary;
    }

    private static final class CauseAggregate {
        double trendCause;
        double gapCause;
        double hotColdCause;
        double transitionCause;
        double positionCause;
        double numberSupplyCause;
        double coverageCause;
        double thirdCause;
        double comboCause;
        double recoveryCause;
        double falseStrongCause;
        double counterCause;
        double positiveScore;
        double negativeScore;
        double evidenceScore;
        double antiEvidenceScore;
        double netEvidenceScore;
        double finalScore;
        double riskScore;
        double confidence;
        double sampleReliability;
        double recentValidity;
        double positionNeedScore;
        double numberSupplyScore;
        double ruleDecayRisk;
        double guardDragRisk;
        double positionWrongRisk;
        double featureAvg;
        double featureMin;
        double riskMax;
        double positiveMin;
        int sampleCount;
        int recentSampleCount;
        int factorCount;
        int strongFactorCount;
        int riskFactorCount;
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:CausalFactorMatrixEngine";

    public static final class V57CausalEvidence {
        public String factor = "";
        public int sample;
        public double treatmentRate;
        public double controlRate;
        public double effect;
        public double counterEvidence;
        public double stability;
        public double counterfactualEffect;
        public double confidence;
        public boolean pitValid;
        public String verdict = "NO_SAMPLE";
        public String hash = "";

        public String canonical() {
            return factor
                    + '|'
                    + sample
                    + '|'
                    + treatmentRate
                    + '|'
                    + controlRate
                    + '|'
                    + effect
                    + '|'
                    + counterEvidence
                    + '|'
                    + stability
                    + '|'
                    + counterfactualEffect
                    + '|'
                    + confidence
                    + '|'
                    + pitValid
                    + '|'
                    + verdict;
        }
    }

    public static V57CausalEvidence v57b08CausalEvidence(
            String factor,
            int treatedHit,
            int treatedN,
            int controlHit,
            int controlN,
            double blockVariance,
            double counterfactualEffect,
            boolean pitValid) {
        V57CausalEvidence e = new V57CausalEvidence();
        e.factor = factor == null ? "" : factor;
        e.sample = Math.max(0, treatedN) + Math.max(0, controlN);
        e.treatmentRate = treatedN <= 0 ? 0.0d : (double) Math.max(0, treatedHit) / treatedN;
        e.controlRate = controlN <= 0 ? 0.0d : (double) Math.max(0, controlHit) / controlN;
        e.effect = e.treatmentRate - e.controlRate;
        e.counterEvidence = Math.max(0.0d, -e.effect);
        e.stability =
                Math.max(0.0d, Math.min(1.0d, 1.0d - Math.sqrt(Math.max(0.0d, blockVariance))));
        e.counterfactualEffect = counterfactualEffect;
        e.pitValid = pitValid;
        double balance =
                Math.min(Math.max(0, treatedN), Math.max(0, controlN))
                        / (double)
                                Math.max(1, Math.max(Math.max(0, treatedN), Math.max(0, controlN)));
        double rel = 1.0d - Math.exp(-e.sample / 120.0d);
        double cfAgree = 1.0d - Math.min(1.0d, Math.abs(e.effect - counterfactualEffect));
        e.confidence = Math.max(0.0d, Math.min(1.0d, rel * balance * e.stability * cfAgree));
        if (!pitValid) e.verdict = "PIT_REJECT";
        else if (e.sample < 60) e.verdict = "NO_SAMPLE";
        else if (e.effect > 0.0d && counterfactualEffect > 0.0d && e.confidence >= 0.45d)
            e.verdict = "SUPPORTED_CAUSAL_EVIDENCE";
        else if (e.effect <= 0.0d || counterfactualEffect <= 0.0d) e.verdict = "COUNTEREVIDENCE";
        else e.verdict = "WATCH";
        e.hash = v57b08Hash(e.canonical());
        return e;
    }

    public static boolean v57b08CorrelationAloneCanPass() {
        return false;
    }

    private static String v57b08Hash(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 255;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "HASH_ERROR";
        }
    }
}
