package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PositionCauseMatrixEngine.java
 *
 * <p>V20 主链闭环修复 FIX09 / 22
 *
 * <p>本文件职责： 1. 只做“位置需求 / 位置风险 / 位置状态簇”矩阵计算； 2. 从 PreDrawFeatureEngine 的 100 个号码特征里汇总 10 个位置状态； 3.
 * 回写候选 / CauseChain 的位置字段，供 ProbabilityEngine / DynamicGateEngine / DecisionGateEngine 继续使用； 4.
 * 不出号、不 BET、不结算、不创建 Trade、不改真实账本、不降低门槛。
 *
 * <p>固定玩法红线： - 单位置 + 固定 K=3； - 本层不产生 BET / SHADOW / HOLD； - 最终真实 BET 仍只能由 DecisionGateEngine 输出； -
 * 本层只是证据层和字段桥，不绕过最终门。
 *
 * <p>Java7 / AIDE / Android 原生兼容： - 无 lambda； - 无 stream； - 无第三方库。
 */
public final class PositionCauseMatrixEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String POSITION_CAUSE_MATRIX_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String POSITION_CAUSE_MATRIX_CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double HEALTH_STRONG = 0.680d;
    private static final double HEALTH_NORMAL = 0.520d;
    private static final double HEALTH_WEAK = 0.380d;
    private static final double RISK_DANGER = 0.720d;
    private static final double RISK_WATCH = 0.600d;
    private static final double NEED_STRONG = 0.620d;
    private static final double NEED_NORMAL = 0.480d;
    private static final double EVIDENCE_STRONG = 0.620d;

    public PositionCauseMatrixEngine() {}

    /** V43 canonical live adapter; builds the same point-in-time feature map used by scan. */
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
        Map<Integer, Models.PositionHealth> map =
                buildPositionHealthMap(baseIssue, targetIssue, features);
        applyToCandidates(out, map);
        try {
            if (store != null) {
                store.setState("v43_position_cause_apply_count", String.valueOf(out.size()));
                store.setState(
                        "v43_position_cause_map_count",
                        String.valueOf(map == null ? 0 : map.size()));
                store.setState("v43_position_cause_status", "APPLIED_TYPED");
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

    /* ========================= public matrix build API ========================= */

    public Map<Integer, Models.PositionHealth> buildPositionHealthMap(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> featureMap) {
        LinkedHashMap<Integer, Models.PositionHealth> out =
                new LinkedHashMap<Integer, Models.PositionHealth>();
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            Models.PositionHealth h = buildOnePositionHealth(baseIssue, targetIssue, p, featureMap);
            out.put(Integer.valueOf(p), h);
        }
        return out;
    }

    public Map<Integer, Models.PositionHealth> buildPositionHealthMap(
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildPositionHealthMap("", "", featureMap);
    }

    public Map<Integer, Models.PositionHealth> buildPositionMatrix(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> featureMap) {
        return buildPositionHealthMap(baseIssue, targetIssue, featureMap);
    }

    public Map<Integer, Models.PositionHealth> analyze(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> featureMap) {
        return buildPositionHealthMap(baseIssue, targetIssue, featureMap);
    }

    public Map<Integer, Models.PositionHealth> apply(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> featureMap) {
        return buildPositionHealthMap(baseIssue, targetIssue, featureMap);
    }

    public ArrayList<Models.PositionHealth> buildPositionHealthList(
            String baseIssue, String targetIssue, Map<String, Models.PreDrawFeature> featureMap) {
        Map<Integer, Models.PositionHealth> map =
                buildPositionHealthMap(baseIssue, targetIssue, featureMap);
        ArrayList<Models.PositionHealth> list = new ArrayList<Models.PositionHealth>();
        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            Models.PositionHealth h = map.get(Integer.valueOf(p));
            if (h != null) {
                list.add(h);
            }
        }
        sortPositionHealthList(list);
        return list;
    }

    public ArrayList<Models.PositionHealth> buildPositionHealthList(
            Map<String, Models.PreDrawFeature> featureMap) {
        return buildPositionHealthList("", "", featureMap);
    }

    public Models.PositionHealth buildOnePositionHealth(
            String baseIssue,
            String targetIssue,
            int position,
            Map<String, Models.PreDrawFeature> featureMap) {
        Models.PositionHealth h = new Models.PositionHealth();
        if (!isValidPosition(position)) {
            putInt(h, "position", position);
            putString(h, "status", "INVALID");
            putDouble(h, "healthScore", 0.0d);
            putDouble(h, "riskScore", 1.0d);
            putString(h, "summaryText", "位置因果矩阵：位置非法；本层不出号不BET");
            return h;
        }

        ArrayList<Models.PreDrawFeature> features = collectPositionFeatures(position, featureMap);
        PositionStat stat = calcPositionStat(position, features);
        String status = positionStatus(stat.healthScore, stat.riskScore);
        String stateKey = buildPositionStateKey(targetIssue, position, stat);
        String clusterKey = buildPositionClusterKey(position, stat);
        String factorKey = buildPositionFactorKey(position, stat);
        String actionAdvice = buildActionAdvice(status, stat);
        String rejectCode = buildRejectCode(status, stat);
        String summary = buildSummary(position, stat, status, actionAdvice, rejectCode);

        putString(h, "positionKey", buildPositionKey(targetIssue, position, stat));
        putString(h, "baseIssue", safe(baseIssue));
        putString(h, "targetIssue", safe(targetIssue));
        putLong(h, "createdAt", System.currentTimeMillis());
        putString(h, "engineVersion", POSITION_CAUSE_MATRIX_VERSION);
        putString(h, "fieldChainVersion", POSITION_CAUSE_MATRIX_CLOSED_LOOP_VERSION);

        putInt(h, "position", position);
        putString(h, "status", status);
        putString(h, "positionStatus", status);
        putString(h, "positionStateKey", stateKey);
        putString(h, "positionClusterKey", clusterKey);
        putString(h, "clusterKey", clusterKey);
        putString(h, "factorKey", factorKey);
        putString(h, "factorId", "POSITION_CAUSE_P" + position);
        putString(h, "factorName", "位置需求矩阵P" + position);
        putString(h, "stateKey", stateKey);
        putString(h, "windowStateKey", stateKey);
        putString(h, "dynamicWindowStateKey", stateKey);
        putString(h, "actionAdvice", actionAdvice);
        putString(h, "v20ActionAdvice", actionAdvice);
        putString(h, "rejectCode", rejectCode);
        putString(h, "finalDecisionRejectCode", rejectCode);

        putDouble(h, "healthScore", stat.healthScore);
        putDouble(h, "positionHealthScore", stat.healthScore);
        putDouble(h, "stabilityScore", stat.stabilityScore);
        putDouble(h, "positionStability", stat.stabilityScore);
        putDouble(h, "riskScore", stat.riskScore);
        putDouble(h, "positionRiskScore", stat.riskScore);
        putDouble(h, "noiseRisk", stat.noiseRisk);
        putDouble(h, "positionNoiseRisk", stat.noiseRisk);
        putDouble(h, "driftRisk", stat.driftRisk);
        putDouble(h, "positionDriftRisk", stat.driftRisk);
        putDouble(h, "concentrationScore", stat.concentrationScore);
        putDouble(h, "positionConcentration", stat.concentrationScore);
        putDouble(h, "returnPower", stat.returnPower);
        putDouble(h, "positionReturnPower", stat.returnPower);
        putDouble(h, "transitionPower", stat.transitionPower);
        putDouble(h, "positionTransitionPower", stat.transitionPower);
        putDouble(h, "recoveryPower", stat.recoveryPower);
        putDouble(h, "positionRecoveryPower", stat.recoveryPower);
        putDouble(h, "falseStrongRisk", stat.falseStrongRisk);
        putDouble(h, "positionFalseStrongRisk", stat.falseStrongRisk);
        putDouble(h, "wrongRisk", stat.wrongRisk);
        putDouble(h, "positionWrongRisk", stat.wrongRisk);

        putDouble(h, "positionNeedScore", stat.positionNeedScore);
        putDouble(h, "positionSupplyScore", stat.positionSupplyScore);
        putDouble(h, "positionDemandScore", stat.positionNeedScore);
        putDouble(h, "positionEvidenceScore", stat.evidenceScore);
        putDouble(h, "evidenceScore", stat.evidenceScore);
        putDouble(h, "antiEvidenceScore", stat.antiEvidenceScore);
        putDouble(h, "counterEvidenceScore", stat.antiEvidenceScore);
        putDouble(h, "netEvidenceScore", stat.netEvidenceScore);
        putDouble(h, "sampleReliability", stat.sampleReliability);
        putDouble(h, "recentValidity", stat.recentValidity);
        putDouble(h, "similarHitRate", stat.similarHitRate);
        putDouble(h, "similarSampleCount", (double) stat.similarSampleCount);
        putDouble(h, "crowdRisk", stat.crowdRisk);
        putDouble(h, "syncPower", stat.syncPower);
        putDouble(h, "breakRisk", stat.breakRisk);
        putDouble(h, "reversalPower", stat.reversalPower);
        putDouble(h, "extensionPower", stat.extensionPower);

        putInt(h, "sampleCount", stat.sampleCount);
        putInt(h, "recentSampleCount", stat.recentSampleCount);
        putInt(h, "similarSampleCount", stat.similarSampleCount);
        putInt(h, "strongNumberCount", stat.strongNumberCount);
        putInt(h, "riskNumberCount", stat.riskNumberCount);
        putInt(h, "returnNumberCount", stat.returnNumberCount);
        putInt(h, "syncNumberCount", stat.syncNumberCount);
        putInt(h, "crowdNumberCount", stat.crowdNumberCount);
        putString(h, "bestNumbers", stat.bestNumbers);
        putString(h, "riskNumbers", stat.riskNumbers);
        putString(h, "stateType", stat.stateType);
        putString(h, "positionNeedType", stat.positionNeedType);
        putString(h, "summaryText", summary);
        putString(h, "reasonText", summary);
        putString(h, "featureSummary", summary);
        return h;
    }

    /* ========================= candidate apply API ========================= */

    public void applyToCandidates(List candidates, Map<Integer, Models.PositionHealth> healthMap) {
        if (candidates == null || candidates.size() == 0) {
            return;
        }
        for (int i = 0; i < candidates.size(); i++) {
            Object c = candidates.get(i);
            if (c == null) {
                continue;
            }
            int position = getInt(c, "position", 0);
            Models.PositionHealth h =
                    healthMap == null ? null : healthMap.get(Integer.valueOf(position));
            applyToCandidate(c, h);
        }
        rerankByPositionHealth(candidates);
    }

    public void applyToCandidates(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        Map<Integer, Models.PositionHealth> healthMap =
                buildPositionHealthMap(baseIssue, targetIssue, featureMap);
        applyToCandidates(candidates, healthMap);
    }

    public void apply(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        applyToCandidates(baseIssue, targetIssue, candidates, featureMap);
    }

    public void run(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        applyToCandidates(baseIssue, targetIssue, candidates, featureMap);
    }

    public int applyAndCount(
            String baseIssue,
            String targetIssue,
            List candidates,
            Map<String, Models.PreDrawFeature> featureMap) {
        if (candidates == null) {
            return 0;
        }
        applyToCandidates(baseIssue, targetIssue, candidates, featureMap);
        return candidates.size();
    }

    public void applyToCandidate(Object candidate, Models.PositionHealth health) {
        if (candidate == null) {
            return;
        }
        int position = getInt(candidate, "position", 0);
        if (!isValidPosition(position)) {
            putDouble(candidate, "positionHealthScore", 0.0d);
            putDouble(candidate, "positionNeedScore", 0.0d);
            putDouble(candidate, "positionRiskScore", 1.0d);
            putDouble(candidate, "positionWrongRisk", 1.0d);
            putDouble(candidate, "positionCauseScore", 0.0d);
            putString(candidate, "positionHealthStatus", "INVALID");
            putString(candidate, "positionStatus", "INVALID");
            putString(
                    candidate,
                    "rejectCode",
                    mergeRejectCode(getString(candidate, "rejectCode", ""), "POSITION_INVALID"));
            return;
        }
        if (health == null) {
            putDouble(candidate, "positionHealthScore", 0.0d);
            putDouble(candidate, "positionNeedScore", 0.0d);
            putDouble(candidate, "positionRiskScore", 0.45d);
            putDouble(candidate, "positionWrongRisk", 0.45d);
            putDouble(candidate, "positionCauseScore", 0.0d);
            putString(candidate, "positionHealthStatus", "UNKNOWN");
            putString(candidate, "positionStatus", "UNKNOWN");
            putString(candidate, "positionRejectCode", "POSITION_HEALTH_MISSING");
            appendReason(candidate, "位置矩阵：无位置健康数据，按中性偏弱处理，不由本层拦截BET");
            return;
        }

        double healthScore =
                getDouble(health, "healthScore", getDouble(health, "positionHealthScore", 0.0d));
        double riskScore =
                getDouble(health, "riskScore", getDouble(health, "positionRiskScore", 0.0d));
        double wrongRisk =
                getDouble(health, "wrongRisk", getDouble(health, "positionWrongRisk", riskScore));
        double noiseRisk =
                getDouble(health, "noiseRisk", getDouble(health, "positionNoiseRisk", riskScore));
        double returnPower =
                getDouble(health, "returnPower", getDouble(health, "positionReturnPower", 0.0d));
        double transitionPower =
                getDouble(
                        health,
                        "transitionPower",
                        getDouble(health, "positionTransitionPower", 0.0d));
        double falseStrongRisk =
                getDouble(
                        health,
                        "falseStrongRisk",
                        getDouble(health, "positionFalseStrongRisk", 0.0d));
        double needScore = getDouble(health, "positionNeedScore", 0.0d);
        double supplyScore = getDouble(health, "positionSupplyScore", 0.0d);
        double evidenceScore =
                getDouble(
                        health, "evidenceScore", getDouble(health, "positionEvidenceScore", 0.0d));
        double antiEvidenceScore =
                getDouble(
                        health,
                        "antiEvidenceScore",
                        getDouble(health, "counterEvidenceScore", riskScore));
        double sampleReliability = getDouble(health, "sampleReliability", 0.0d);
        double recentValidity = getDouble(health, "recentValidity", 0.0d);
        String status = getString(health, "status", getString(health, "positionStatus", "UNKNOWN"));
        String stateKey = getString(health, "positionStateKey", getString(health, "stateKey", ""));
        String clusterKey =
                getString(health, "positionClusterKey", getString(health, "clusterKey", ""));
        String actionAdvice = getString(health, "actionAdvice", "POSITION_WATCH");
        String rejectCode = getString(health, "rejectCode", "");

        double oldCause =
                getDouble(candidate, "causeScore", getDouble(candidate, "evidenceScore", 0.0d));
        double oldRisk =
                getDouble(
                        candidate, "riskScore", getDouble(candidate, "counterEvidenceScore", 0.0d));
        double oldNeed = getDouble(candidate, "positionNeedScore", 0.0d);
        double oldSupply = getDouble(candidate, "numberSupplyScore", 0.0d);
        double edge = getDouble(candidate, "edge", 0.0d);
        double coverage = getDouble(candidate, "coverageQuality", 0.0d);
        double third = getDouble(candidate, "thirdQuality", 0.0d);

        double positionCause = calcCandidatePositionCause(candidate, health);
        double mergedNeed = maxNonZero(needScore, oldNeed);
        double mergedSupply = maxNonZero(supplyScore, oldSupply);
        double candidatePositionEvidence =
                clamp01(
                        positionCause * 0.34d
                                + evidenceScore * 0.18d
                                + mergedNeed * 0.16d
                                + mergedSupply * 0.10d
                                + posPart(edge * 4.0d) * 0.06d
                                + coverage * 0.08d
                                + third * 0.08d);
        double candidatePositionRisk =
                clamp01(
                        wrongRisk * 0.34d
                                + riskScore * 0.20d
                                + falseStrongRisk * 0.18d
                                + noiseRisk * 0.14d
                                + antiEvidenceScore * 0.14d);
        double newCause = clamp01(oldCause * 0.82d + candidatePositionEvidence * 0.18d);
        double newRisk = clamp01(Math.max(oldRisk, candidatePositionRisk));
        double net = clampSigned(candidatePositionEvidence - candidatePositionRisk);
        String candidateReject =
                buildCandidatePositionRejectCode(
                        status, wrongRisk, candidatePositionRisk, rejectCode);

        putString(candidate, "positionMatrixVersion", POSITION_CAUSE_MATRIX_VERSION);
        putString(candidate, "fieldChainVersion", POSITION_CAUSE_MATRIX_CLOSED_LOOP_VERSION);
        putString(candidate, "positionHealthStatus", status);
        putString(candidate, "positionStatus", status);
        putString(candidate, "positionStateKey", stateKey);
        putString(candidate, "positionClusterKey", clusterKey);
        putString(
                candidate,
                "clusterKey",
                mergeClusterKey(getString(candidate, "clusterKey", ""), clusterKey));
        putString(candidate, "positionFactorKey", getString(health, "factorKey", ""));
        putString(candidate, "positionActionAdvice", actionAdvice);
        putString(
                candidate,
                "actionAdvice",
                mergeActionAdvice(getString(candidate, "actionAdvice", ""), actionAdvice));
        putString(candidate, "positionRejectCode", candidateReject);
        if (candidateReject.length() > 0) {
            putString(
                    candidate,
                    "rejectCode",
                    mergeRejectCode(getString(candidate, "rejectCode", ""), candidateReject));
            putString(
                    candidate,
                    "finalDecisionRejectCode",
                    mergeRejectCode(
                            getString(candidate, "finalDecisionRejectCode", ""), candidateReject));
        }

        putDouble(candidate, "positionHealthScore", healthScore);
        putDouble(candidate, "positionCauseScore", positionCause);
        putDouble(candidate, "positionNeedScore", mergedNeed);
        putDouble(candidate, "positionSupplyScore", mergedSupply);
        putDouble(candidate, "positionEvidenceScore", candidatePositionEvidence);
        putDouble(candidate, "positionRiskScore", riskScore);
        putDouble(candidate, "positionWrongRisk", wrongRisk);
        putDouble(candidate, "positionNoiseRisk", noiseRisk);
        putDouble(candidate, "positionReturnPower", returnPower);
        putDouble(candidate, "positionTransitionPower", transitionPower);
        putDouble(candidate, "positionFalseStrongRisk", falseStrongRisk);
        putDouble(
                candidate,
                "sampleReliability",
                maxNonZero(getDouble(candidate, "sampleReliability", 0.0d), sampleReliability));
        putDouble(
                candidate,
                "recentValidity",
                maxNonZero(getDouble(candidate, "recentValidity", 0.0d), recentValidity));
        putDouble(
                candidate,
                "evidenceScore",
                maxNonZero(getDouble(candidate, "evidenceScore", 0.0d), candidatePositionEvidence));
        putDouble(
                candidate,
                "antiEvidenceScore",
                Math.max(getDouble(candidate, "antiEvidenceScore", 0.0d), candidatePositionRisk));
        putDouble(
                candidate,
                "counterEvidenceScore",
                Math.max(
                        getDouble(candidate, "counterEvidenceScore", 0.0d), candidatePositionRisk));
        putDouble(
                candidate,
                "netEvidenceScore",
                clampSigned(getDouble(candidate, "netEvidenceScore", 0.0d) * 0.70d + net * 0.30d));
        putDouble(candidate, "causeScore", newCause);
        putDouble(candidate, "riskScore", newRisk);

        String text =
                "位置矩阵｜状态="
                        + status
                        + "｜需求="
                        + percent(mergedNeed)
                        + "｜健康="
                        + percent(healthScore)
                        + "｜位置因="
                        + percent(positionCause)
                        + "｜位置证据="
                        + percent(candidatePositionEvidence)
                        + "｜错位风险="
                        + percent(wrongRisk)
                        + "｜噪声="
                        + percent(noiseRisk)
                        + "｜回归="
                        + percent(returnPower)
                        + "｜转移="
                        + percent(transitionPower)
                        + "｜动作="
                        + actionAdvice;
        appendReason(candidate, text);
    }

    public void applyToCauseChains(List chains, Map<Integer, Models.PositionHealth> healthMap) {
        if (chains == null || chains.size() == 0) {
            return;
        }
        for (int i = 0; i < chains.size(); i++) {
            Object c = chains.get(i);
            if (c == null) {
                continue;
            }
            int position = getInt(c, "position", 0);
            Models.PositionHealth h =
                    healthMap == null ? null : healthMap.get(Integer.valueOf(position));
            applyToCauseChain(c, h);
        }
    }

    public void applyToCauseChain(Object chain, Models.PositionHealth health) {
        if (chain == null) {
            return;
        }
        if (health == null) {
            putDouble(chain, "positionHealthScore", 0.0d);
            putDouble(chain, "positionWrongRisk", 0.45d);
            return;
        }
        double healthScore =
                getDouble(health, "healthScore", getDouble(health, "positionHealthScore", 0.0d));
        double riskScore =
                getDouble(health, "riskScore", getDouble(health, "positionRiskScore", 0.0d));
        double wrongRisk =
                getDouble(health, "wrongRisk", getDouble(health, "positionWrongRisk", riskScore));
        double needScore = getDouble(health, "positionNeedScore", 0.0d);
        double evidence = getDouble(health, "evidenceScore", 0.0d);
        String status = getString(health, "status", "UNKNOWN");
        String stateKey = getString(health, "positionStateKey", getString(health, "stateKey", ""));
        String clusterKey =
                getString(health, "positionClusterKey", getString(health, "clusterKey", ""));
        String actionAdvice = getString(health, "actionAdvice", "POSITION_WATCH");

        double positionCause =
                clampSigned(
                        (healthScore
                                        + needScore * 0.40d
                                        + evidence * 0.35d
                                        - wrongRisk
                                        - riskScore * 0.25d)
                                * 0.95d);
        double oldPositionCause = getDouble(chain, "positionCause", 0.0d);
        double oldFinal = getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
        double oldRisk = getDouble(chain, "riskScore", 0.0d);
        double mergedPositionCause = clampSigned(oldPositionCause * 0.66d + positionCause * 0.34d);
        double newFinal =
                clamp01(
                        oldFinal
                                + posPart(mergedPositionCause) * 0.038d
                                - negPart(mergedPositionCause) * 0.050d);
        double newRisk = clamp01(Math.max(oldRisk, wrongRisk * 0.70d));

        putDouble(chain, "positionCause", mergedPositionCause);
        putDouble(chain, "positionHealthScore", healthScore);
        putDouble(chain, "positionNeedScore", needScore);
        putDouble(chain, "positionWrongRisk", wrongRisk);
        putDouble(chain, "finalCauseScore", newFinal);
        putDouble(chain, "causeScore", newFinal);
        putDouble(chain, "riskScore", newRisk);
        putString(chain, "positionStatus", status);
        putString(chain, "positionStateKey", stateKey);
        putString(chain, "positionClusterKey", clusterKey);
        putString(
                chain,
                "clusterKey",
                mergeClusterKey(getString(chain, "clusterKey", ""), clusterKey));
        putString(
                chain,
                "actionAdvice",
                mergeActionAdvice(getString(chain, "actionAdvice", ""), actionAdvice));

        String old = getString(chain, "summaryText", "");
        String add =
                "｜位置因果矩阵：状态="
                        + status
                        + "，需求="
                        + percent(needScore)
                        + "，健康="
                        + percent(healthScore)
                        + "，位置因="
                        + signed(mergedPositionCause)
                        + "，错位风险="
                        + percent(wrongRisk)
                        + "，动作="
                        + actionAdvice;
        putString(chain, "summaryText", old + add);
    }

    public Models.PositionHealth getHealth(Map<Integer, Models.PositionHealth> map, int position) {
        if (map == null) {
            return null;
        }
        return map.get(Integer.valueOf(position));
    }

    /* ========================= core stat calculation ========================= */

    private PositionStat calcPositionStat(int position, ArrayList<Models.PreDrawFeature> features) {
        PositionStat s = new PositionStat();
        s.position = position;
        if (features == null || features.size() == 0) {
            s.sampleCount = 0;
            s.recentSampleCount = 0;
            s.similarSampleCount = 0;
            s.healthScore = 0.0d;
            s.riskScore = 0.65d;
            s.stabilityScore = 0.0d;
            s.noiseRisk = 0.65d;
            s.driftRisk = 0.55d;
            s.wrongRisk = 0.65d;
            s.falseStrongRisk = 0.55d;
            s.positionNeedScore = 0.0d;
            s.positionSupplyScore = 0.0d;
            s.evidenceScore = 0.0d;
            s.antiEvidenceScore = 0.65d;
            s.netEvidenceScore = -0.65d;
            s.sampleReliability = 0.0d;
            s.recentValidity = 0.0d;
            s.similarHitRate = 0.0d;
            s.stateType = "NO_FEATURE";
            s.positionNeedType = "NO_FEATURE";
            s.bestNumbers = "";
            s.riskNumbers = "";
            return s;
        }

        s.sampleCount = features.size();
        s.recentSampleCount = Math.min(features.size(), 10);
        s.similarSampleCount = features.size();

        double featureScoreSum = 0.0d;
        double confidenceSum = 0.0d;
        double entropySum = 0.0d;
        double top3Sum = 0.0d;
        double driftSum = 0.0d;
        double noiseSum = 0.0d;
        double returnSum = 0.0d;
        double transitionSum = 0.0d;
        double recoverySum = 0.0d;
        double falseRiskSum = 0.0d;
        double breakSum = 0.0d;
        double needSum = 0.0d;
        double supplySum = 0.0d;
        double evidenceSum = 0.0d;
        double antiEvidenceSum = 0.0d;
        double recentValiditySum = 0.0d;
        double syncSum = 0.0d;
        double crowdSum = 0.0d;
        double extensionSum = 0.0d;
        double reversalSum = 0.0d;
        int validCount = 0;

        ArrayList<NumberScore> numberScores = new ArrayList<NumberScore>();
        for (int i = 0; i < features.size(); i++) {
            Models.PreDrawFeature f = features.get(i);
            if (f == null) {
                continue;
            }
            int n = getInt(f, "number", 0);
            double featureScore = getDouble(f, "featureScore", 0.0d);
            double confidence = getDouble(f, "confidence", getDouble(f, "sampleReliability", 0.0d));
            double entropy = getDouble(f, "positionEntropy", 1.0d);
            double top3 = getDouble(f, "positionTop3Mass", 0.30d);
            double drift = getDouble(f, "positionDrift", 0.0d);
            double noise = getDouble(f, "positionNoiseScore", 1.0d);
            double returnZone = getDouble(f, "returnZoneScore", getDouble(f, "gapPressure", 0.0d));
            double coldRepair = getDouble(f, "coldRepairPower", 0.0d);
            double transition = getDouble(f, "transitionLift", 0.0d);
            double transitionConflict = getDouble(f, "transitionConflict", 0.0d);
            double recovery = getDouble(f, "recoveryQuality", 0.0d);
            double falseRisk = getDouble(f, "falseRecoveryRisk", 0.0d);
            double breakDepth = getDouble(f, "breakDepth", 0.0d);
            double hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);
            double freqSlope = getDouble(f, "freqSlope", 0.0d);
            double gapZ = getDouble(f, "gapZ", 0.0d);
            double hotPressure = getDouble(f, "hotPressure", 0.0d);
            double numberNeed =
                    getDouble(
                            f,
                            "positionNeedScore",
                            calcFeatureNeed(f, returnZone, coldRepair, recovery, gapZ, freqSlope));
            double numberSupply =
                    getDouble(
                            f,
                            "numberSupplyScore",
                            calcFeatureSupply(f, featureScore, transition, recovery, confidence));
            double evidence =
                    getDouble(
                            f,
                            "evidenceScore",
                            clamp01(
                                    numberNeed * 0.42d
                                            + numberSupply * 0.38d
                                            + confidence * 0.20d));
            double antiEvidence =
                    getDouble(
                            f,
                            "antiEvidenceScore",
                            getDouble(
                                    f,
                                    "counterEvidenceScore",
                                    clamp01(
                                            falseRisk * 0.30d
                                                    + noise * 0.24d
                                                    + drift * 0.18d
                                                    + breakDepth * 0.18d
                                                    + transitionConflict * 0.10d)));
            double recentValidity =
                    getDouble(
                            f,
                            "recentValidity",
                            clamp01(
                                    recovery * 0.30d
                                            + transition * 0.25d
                                            + returnZone * 0.20d
                                            + confidence * 0.25d));
            double sync =
                    getDouble(
                            f,
                            "crossPositionSync",
                            getDouble(
                                    f,
                                    "syncPower",
                                    clamp01(
                                            transition * 0.55d
                                                    + getDouble(f, "pairCoSupport", 0.0d)
                                                            * 0.45d)));
            double crowd =
                    getDouble(
                            f,
                            "positionCrowding",
                            clamp01(
                                    top3 * 0.40d
                                            + hotPressure * 0.24d
                                            + hotExhaustion * 0.20d
                                            + entropy * 0.16d));
            double extension =
                    clamp01(
                            posPart(freqSlope) * 0.34d
                                    + hotPressure * 0.22d
                                    + transition * 0.18d
                                    + confidence * 0.16d
                                    + recovery * 0.10d);
            double reversal =
                    clamp01(
                            returnZone * 0.30d
                                    + coldRepair * 0.24d
                                    + posPart(gapZ) * 0.14d
                                    + recovery * 0.20d
                                    + (1.0d - hotExhaustion) * 0.12d);

            featureScoreSum += featureScore;
            confidenceSum += confidence;
            entropySum += entropy;
            top3Sum += top3;
            driftSum += drift;
            noiseSum += noise;
            returnSum += (returnZone * 0.60d + coldRepair * 0.40d);
            transitionSum += Math.max(0.0d, transition - transitionConflict * 0.60d);
            recoverySum += recovery;
            falseRiskSum += Math.max(falseRisk, hotExhaustion * 0.70d);
            breakSum += breakDepth;
            needSum += numberNeed;
            supplySum += numberSupply;
            evidenceSum += evidence;
            antiEvidenceSum += antiEvidence;
            recentValiditySum += recentValidity;
            syncSum += sync;
            crowdSum += crowd;
            extensionSum += extension;
            reversalSum += reversal;
            validCount++;

            NumberScore ns = new NumberScore();
            ns.number = n;
            ns.featureScore = featureScore;
            ns.confidence = confidence;
            ns.returnPower = returnZone;
            ns.transitionPower = transition;
            ns.recoveryPower = recovery;
            ns.positionNeed = numberNeed;
            ns.numberSupply = numberSupply;
            ns.evidence = evidence;
            ns.antiEvidence = antiEvidence;
            ns.recentValidity = recentValidity;
            ns.syncPower = sync;
            ns.crowdRisk = crowd;
            ns.extensionPower = extension;
            ns.reversalPower = reversal;
            ns.risk =
                    clamp01(
                            antiEvidence * 0.34d
                                    + falseRisk * 0.20d
                                    + noise * 0.18d
                                    + drift * 0.12d
                                    + breakDepth * 0.16d);
            ns.total =
                    clamp01(
                            featureScore * 0.20d
                                    + numberNeed * 0.20d
                                    + numberSupply * 0.18d
                                    + evidence * 0.16d
                                    + returnZone * 0.08d
                                    + transition * 0.08d
                                    + recovery * 0.06d
                                    + confidence * 0.08d
                                    - ns.risk * 0.14d);
            numberScores.add(ns);
        }

        int count = Math.max(1, validCount);
        double featureAvg = featureScoreSum / count;
        double confidenceAvg = confidenceSum / count;
        double entropyAvg = entropySum / count;
        double top3Avg = top3Sum / count;
        double driftAvg = driftSum / count;
        double noiseAvg = noiseSum / count;
        double returnAvg = returnSum / count;
        double transitionAvg = transitionSum / count;
        double recoveryAvg = recoverySum / count;
        double falseRiskAvg = falseRiskSum / count;
        double breakAvg = breakSum / count;
        double needAvg = needSum / count;
        double supplyAvg = supplySum / count;
        double evidenceAvg = evidenceSum / count;
        double antiEvidenceAvg = antiEvidenceSum / count;
        double recentValidityAvg = recentValiditySum / count;
        double syncAvg = syncSum / count;
        double crowdAvg = crowdSum / count;
        double extensionAvg = extensionSum / count;
        double reversalAvg = reversalSum / count;

        Collections.sort(
                numberScores,
                new Comparator<NumberScore>() {
                    public int compare(NumberScore a, NumberScore b) {
                        if (a.total > b.total) {
                            return -1;
                        }
                        if (a.total < b.total) {
                            return 1;
                        }
                        return a.number - b.number;
                    }
                });

        s.concentrationScore = calcConcentration(numberScores);
        s.stabilityScore =
                clamp01(
                        (1.0d - noiseAvg) * 0.30d
                                + (1.0d - driftAvg) * 0.22d
                                + (1.0d - entropyAvg) * 0.12d
                                + top3Avg * 0.14d
                                + confidenceAvg * 0.12d
                                + recentValidityAvg * 0.10d);
        s.returnPower = clamp01(returnAvg);
        s.transitionPower = clamp01(transitionAvg);
        s.recoveryPower = clamp01(recoveryAvg);
        s.noiseRisk = clamp01(noiseAvg);
        s.driftRisk = clamp01(driftAvg);
        s.falseStrongRisk =
                clamp01(
                        falseRiskAvg * 0.48d
                                + breakAvg * 0.22d
                                + noiseAvg * 0.18d
                                + crowdAvg * 0.12d);
        s.breakRisk = clamp01(breakAvg);
        s.crowdRisk = clamp01(crowdAvg);
        s.syncPower = clamp01(syncAvg);
        s.extensionPower = clamp01(extensionAvg);
        s.reversalPower = clamp01(reversalAvg);
        s.positionNeedScore =
                clamp01(
                        needAvg * 0.34d
                                + returnAvg * 0.20d
                                + reversalAvg * 0.18d
                                + syncAvg * 0.10d
                                + s.concentrationScore * 0.08d
                                + recentValidityAvg * 0.10d);
        s.positionSupplyScore =
                clamp01(
                        supplyAvg * 0.42d
                                + featureAvg * 0.20d
                                + transitionAvg * 0.14d
                                + recoveryAvg * 0.12d
                                + confidenceAvg * 0.12d);
        s.sampleReliability =
                clamp01(
                        ((double) validCount) / 10.0d * 0.42d
                                + confidenceAvg * 0.36d
                                + (1.0d - noiseAvg) * 0.22d);
        s.recentValidity =
                clamp01(
                        recentValidityAvg * 0.46d
                                + transitionAvg * 0.18d
                                + recoveryAvg * 0.18d
                                + (1.0d - driftAvg) * 0.18d);
        s.evidenceScore =
                clamp01(
                        evidenceAvg * 0.34d
                                + s.positionNeedScore * 0.22d
                                + s.positionSupplyScore * 0.18d
                                + s.stabilityScore * 0.12d
                                + s.recentValidity * 0.14d);
        s.antiEvidenceScore =
                clamp01(
                        antiEvidenceAvg * 0.30d
                                + s.falseStrongRisk * 0.22d
                                + s.noiseRisk * 0.18d
                                + s.driftRisk * 0.12d
                                + weakTopRisk(numberScores) * 0.10d
                                + s.breakRisk * 0.08d);
        s.netEvidenceScore = clampSigned(s.evidenceScore - s.antiEvidenceScore);
        s.riskScore =
                clamp01(
                        s.noiseRisk * 0.22d
                                + s.driftRisk * 0.18d
                                + s.falseStrongRisk * 0.20d
                                + s.antiEvidenceScore * 0.18d
                                + breakAvg * 0.10d
                                + (1.0d - confidenceAvg) * 0.07d
                                + weakTopRisk(numberScores) * 0.05d);
        s.healthScore =
                clamp01(
                        featureAvg * 0.14d
                                + s.stabilityScore * 0.18d
                                + s.positionNeedScore * 0.18d
                                + s.positionSupplyScore * 0.14d
                                + s.evidenceScore * 0.13d
                                + s.recentValidity * 0.10d
                                + s.concentrationScore * 0.06d
                                + s.recoveryPower * 0.07d
                                - s.riskScore * 0.16d);
        s.wrongRisk =
                clamp01(
                        s.riskScore * 0.38d
                                + s.noiseRisk * 0.22d
                                + s.driftRisk * 0.16d
                                + s.antiEvidenceScore * 0.14d
                                + weakTopRisk(numberScores) * 0.10d);
        s.similarHitRate =
                clamp01(
                        s.evidenceScore * 0.46d
                                + s.positionSupplyScore * 0.22d
                                + s.recentValidity * 0.20d
                                + s.sampleReliability * 0.12d);
        s.strongNumberCount = countNumbersAbove(numberScores, 0.58d);
        s.returnNumberCount = countReturnNumbers(numberScores);
        s.riskNumberCount = countRiskNumbers(numberScores, 0.55d);
        s.syncNumberCount = countSyncNumbers(numberScores, 0.48d);
        s.crowdNumberCount = countCrowdNumbers(numberScores, 0.58d);
        s.bestNumbers = topNumbersText(numberScores, false);
        s.riskNumbers = topNumbersText(numberScores, true);
        s.stateType = detectStateType(s);
        s.positionNeedType = detectNeedType(s);
        return s;
    }

    private double calcFeatureNeed(
            Models.PreDrawFeature f,
            double returnZone,
            double coldRepair,
            double recovery,
            double gapZ,
            double freqSlope) {
        if (f == null) {
            return 0.0d;
        }
        double old = getDouble(f, "positionNeedScore", -1.0d);
        if (old >= 0.0d) {
            return clamp01(old);
        }
        return clamp01(
                returnZone * 0.30d
                        + coldRepair * 0.22d
                        + recovery * 0.18d
                        + posPart(gapZ) * 0.12d
                        + posPart(freqSlope) * 0.10d
                        + getDouble(f, "gapPressure", 0.0d) * 0.08d);
    }

    private double calcFeatureSupply(
            Models.PreDrawFeature f,
            double featureScore,
            double transition,
            double recovery,
            double confidence) {
        if (f == null) {
            return 0.0d;
        }
        double old = getDouble(f, "numberSupplyScore", -1.0d);
        if (old >= 0.0d) {
            return clamp01(old);
        }
        double seed = getDouble(f, "seedScore", featureScore);
        double support = getDouble(f, "supportScore", featureScore);
        return clamp01(
                seed * 0.24d
                        + support * 0.20d
                        + featureScore * 0.18d
                        + transition * 0.14d
                        + recovery * 0.12d
                        + confidence * 0.12d);
    }

    private double calcCandidatePositionCause(Object c, Models.PositionHealth h) {
        if (c == null || h == null) {
            return 0.0d;
        }
        double health = getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
        double stability = getDouble(h, "stabilityScore", getDouble(h, "positionStability", 0.0d));
        double returnPower = getDouble(h, "returnPower", getDouble(h, "positionReturnPower", 0.0d));
        double transitionPower =
                getDouble(h, "transitionPower", getDouble(h, "positionTransitionPower", 0.0d));
        double need = getDouble(h, "positionNeedScore", 0.0d);
        double supply = getDouble(h, "positionSupplyScore", 0.0d);
        double evidence = getDouble(h, "evidenceScore", 0.0d);
        double recent = getDouble(h, "recentValidity", 0.0d);
        double risk = getDouble(h, "riskScore", getDouble(h, "positionRiskScore", 0.0d));
        double wrong = getDouble(h, "wrongRisk", getDouble(h, "positionWrongRisk", risk));
        double candidateCause = getDouble(c, "causeScore", getDouble(c, "evidenceScore", 0.0d));
        double candidateRisk =
                getDouble(c, "riskScore", getDouble(c, "counterEvidenceScore", 0.0d));
        double edge = getDouble(c, "edge", 0.0d);
        double score = 0.0d;
        score += health * 0.20d;
        score += need * 0.18d;
        score += supply * 0.14d;
        score += evidence * 0.14d;
        score += stability * 0.10d;
        score += returnPower * 0.08d;
        score += transitionPower * 0.06d;
        score += recent * 0.06d;
        score += clamp01(candidateCause) * 0.06d;
        score += posPart(edge * 3.0d) * 0.04d;
        score -= risk * 0.12d;
        score -= wrong * 0.14d;
        score -= candidateRisk * 0.08d;
        return clamp01(score);
    }

    private ArrayList<Models.PreDrawFeature> collectPositionFeatures(
            int position, Map<String, Models.PreDrawFeature> featureMap) {
        ArrayList<Models.PreDrawFeature> list = new ArrayList<Models.PreDrawFeature>();
        if (!isValidPosition(position) || featureMap == null || featureMap.size() == 0) {
            return list;
        }
        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            Models.PreDrawFeature f = getFeature(featureMap, position, n);
            if (f != null) {
                list.add(f);
            }
        }
        return list;
    }

    private Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> featureMap, int position, int number) {
        if (featureMap == null) {
            return null;
        }
        Models.PreDrawFeature f = featureMap.get(keyOf(position, number));
        if (f != null) {
            return f;
        }
        f = featureMap.get(String.valueOf(position) + "|" + String.valueOf(number));
        if (f != null) {
            return f;
        }
        f = featureMap.get("P" + position + "_N" + number);
        if (f != null) {
            return f;
        }
        f = featureMap.get("P" + position + "|N" + number);
        return f;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    /* ========================= ranking / state ========================= */

    private void sortPositionHealthList(ArrayList<Models.PositionHealth> list) {
        if (list == null || list.size() <= 1) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<Models.PositionHealth>() {
                    public int compare(Models.PositionHealth a, Models.PositionHealth b) {
                        double sa = positionSortScore(a);
                        double sb = positionSortScore(b);
                        if (sa > sb) {
                            return -1;
                        }
                        if (sa < sb) {
                            return 1;
                        }
                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);
                        return pa - pb;
                    }
                });
        for (int i = 0; i < list.size(); i++) {
            Models.PositionHealth h = list.get(i);
            if (h != null) {
                putInt(h, "positionRank", i + 1);
            }
        }
    }

    private double positionSortScore(Models.PositionHealth h) {
        if (h == null) {
            return 0.0d;
        }
        double health = getDouble(h, "healthScore", 0.0d);
        double need = getDouble(h, "positionNeedScore", 0.0d);
        double evidence = getDouble(h, "evidenceScore", 0.0d);
        double recent = getDouble(h, "recentValidity", 0.0d);
        double risk = getDouble(h, "riskScore", 0.0d);
        double wrong = getDouble(h, "positionWrongRisk", risk);
        return health * 0.32d
                + need * 0.24d
                + evidence * 0.20d
                + recent * 0.10d
                - risk * 0.09d
                - wrong * 0.05d;
    }

    private void rerankByPositionHealth(List candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }
        Collections.sort(
                candidates,
                new Comparator() {
                    public int compare(Object a, Object b) {
                        double sa =
                                getDouble(a, "causeScore", getDouble(a, "evidenceScore", 0.0d))
                                                * 0.66d
                                        + getDouble(a, "positionCauseScore", 0.0d) * 0.18d
                                        + getDouble(a, "positionNeedScore", 0.0d) * 0.10d
                                        - getDouble(a, "positionWrongRisk", 0.0d) * 0.06d;
                        double sb =
                                getDouble(b, "causeScore", getDouble(b, "evidenceScore", 0.0d))
                                                * 0.66d
                                        + getDouble(b, "positionCauseScore", 0.0d) * 0.18d
                                        + getDouble(b, "positionNeedScore", 0.0d) * 0.10d
                                        - getDouble(b, "positionWrongRisk", 0.0d) * 0.06d;
                        if (sa > sb) {
                            return -1;
                        }
                        if (sa < sb) {
                            return 1;
                        }
                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);
                        if (pa != pb) {
                            return pa - pb;
                        }
                        return getInt(a, "k3Mask", 0) - getInt(b, "k3Mask", 0);
                    }
                });
        for (int i = 0; i < candidates.size(); i++) {
            Object c = candidates.get(i);
            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
                putInt(c, "positionMatrixRank", i + 1);
            }
        }
    }

    private String positionStatus(PositionStat s) {
        if (s == null) {
            return "UNKNOWN";
        }
        return positionStatus(s.healthScore, s.riskScore);
    }

    private String positionStatus(double health, double risk) {
        if (risk >= RISK_DANGER) {
            return "DANGER";
        }
        if (health >= HEALTH_STRONG && risk < 0.46d) {
            return "STRONG";
        }
        if (health >= HEALTH_NORMAL && risk < RISK_WATCH) {
            return "NORMAL";
        }
        if (health >= HEALTH_WEAK && risk < RISK_DANGER) {
            return "WEAK";
        }
        return "BAD";
    }

    private String detectStateType(PositionStat s) {
        if (s == null) {
            return "UNKNOWN";
        }
        if (s.riskScore >= RISK_DANGER || s.breakRisk >= 0.68d) {
            return "BREAK_RISK";
        }
        if (s.crowdRisk >= 0.62d && s.concentrationScore >= 0.58d) {
            return "CROWD";
        }
        if (s.reversalPower >= 0.58d && s.returnPower >= 0.50d) {
            return "REBOUND";
        }
        if (s.extensionPower >= 0.58d && s.transitionPower >= 0.45d) {
            return "EXTENSION";
        }
        if (s.syncPower >= 0.54d) {
            return "SYNC";
        }
        if (s.driftRisk >= 0.60d) {
            return "DRIFT";
        }
        return "BALANCED";
    }

    private String detectNeedType(PositionStat s) {
        if (s == null) {
            return "UNKNOWN";
        }
        if (s.positionNeedScore >= NEED_STRONG && s.riskScore < RISK_WATCH) {
            return "NEED_STRONG";
        }
        if (s.positionNeedScore >= NEED_NORMAL && s.riskScore < RISK_DANGER) {
            return "NEED_NORMAL";
        }
        if (s.antiEvidenceScore >= 0.62d) {
            return "NEED_BLOCKED_BY_COUNTER";
        }
        if (s.sampleReliability < 0.38d) {
            return "NEED_SAMPLE_WEAK";
        }
        return "NEED_WEAK";
    }

    private String buildPositionKey(String targetIssue, int position, PositionStat s) {
        StringBuilder sb = new StringBuilder();
        sb.append("POS|");
        sb.append(safe(targetIssue));
        sb.append("|P");
        sb.append(position);
        sb.append("|H");
        sb.append(level(s == null ? 0.0d : s.healthScore));
        sb.append("|N");
        sb.append(level(s == null ? 0.0d : s.positionNeedScore));
        sb.append("|R");
        sb.append(level(s == null ? 1.0d : s.riskScore));
        return sb.toString();
    }

    private String buildPositionStateKey(String targetIssue, int position, PositionStat s) {
        if (s == null) {
            return "POS_STATE|" + safe(targetIssue) + "|P" + position + "|EMPTY";
        }
        return "POS_STATE|"
                + safe(targetIssue)
                + "|P"
                + position
                + "|"
                + s.stateType
                + "|"
                + s.positionNeedType
                + "|H_"
                + level(s.healthScore)
                + "|N_"
                + level(s.positionNeedScore)
                + "|E_"
                + level(s.evidenceScore)
                + "|R_"
                + level(s.riskScore)
                + "|REC_"
                + level(s.recentValidity);
    }

    private String buildPositionClusterKey(int position, PositionStat s) {
        if (s == null) {
            return "POS_CLUSTER|P" + position + "|EMPTY";
        }
        return "POS_CLUSTER|P"
                + position
                + "|"
                + s.stateType
                + "|"
                + s.positionNeedType
                + "|E"
                + level(s.evidenceScore)
                + "|C"
                + level(s.antiEvidenceScore)
                + "|S"
                + level(s.sampleReliability);
    }

    private String buildPositionFactorKey(int position, PositionStat s) {
        if (s == null) {
            return "POS_FACTOR|P" + position + "|EMPTY";
        }
        return "POS_FACTOR|P"
                + position
                + "|NEED_"
                + level(s.positionNeedScore)
                + "|SUPPLY_"
                + level(s.positionSupplyScore)
                + "|RETURN_"
                + level(s.returnPower)
                + "|TRANS_"
                + level(s.transitionPower)
                + "|RISK_"
                + level(s.riskScore);
    }

    private String buildActionAdvice(String status, PositionStat s) {
        if (s == null) {
            return "POSITION_WATCH_NO_SAMPLE";
        }
        if ("DANGER".equals(status) || s.riskScore >= RISK_DANGER) {
            return "POSITION_FREEZE_HARD_RISK";
        }
        if (s.antiEvidenceScore >= 0.66d && s.evidenceScore < 0.52d) {
            return "POSITION_WATCH_COUNTER_STRONG";
        }
        if (s.positionNeedScore >= NEED_STRONG
                && s.evidenceScore >= EVIDENCE_STRONG
                && s.riskScore < RISK_WATCH) {
            return "POSITION_PASS_NEED_SUPPLY_READY";
        }
        if (s.positionNeedScore >= NEED_NORMAL && s.riskScore < RISK_WATCH) {
            return "POSITION_WATCH_NEED_READY";
        }
        if (s.sampleReliability < 0.38d) {
            return "POSITION_WATCH_SAMPLE_WEAK";
        }
        return "POSITION_WATCH_NEUTRAL";
    }

    private String buildRejectCode(String status, PositionStat s) {
        if (s == null) {
            return "POSITION_NO_FEATURE";
        }
        if ("DANGER".equals(status) || s.riskScore >= RISK_DANGER) {
            return "POSITION_HARD_RISK";
        }
        if (s.wrongRisk >= 0.72d) {
            return "POSITION_WRONG_RISK";
        }
        if (s.antiEvidenceScore >= 0.70d && s.evidenceScore < 0.50d) {
            return "POSITION_COUNTER_EVIDENCE_STRONG";
        }
        if (s.sampleReliability < 0.20d) {
            return "POSITION_SAMPLE_EMPTY";
        }
        return "";
    }

    private String buildCandidatePositionRejectCode(
            String status, double wrongRisk, double candidatePositionRisk, String healthReject) {
        if (healthReject != null && healthReject.length() > 0) {
            return healthReject;
        }
        if ("DANGER".equals(status)) {
            return "POSITION_DANGER";
        }
        if (wrongRisk >= 0.72d) {
            return "POSITION_WRONG_RISK";
        }
        if (candidatePositionRisk >= 0.76d) {
            return "POSITION_CANDIDATE_RISK";
        }
        return "";
    }

    private String buildSummary(
            int position, PositionStat s, String status, String actionAdvice, String rejectCode) {
        if (s == null) {
            return "位置因果矩阵：无数据；本层不出号不BET";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("V20 位置因果矩阵｜位置=");
        sb.append(position);
        sb.append("｜状态=");
        sb.append(status);
        sb.append("｜状态簇=");
        sb.append(s.stateType);
        sb.append("｜需求型=");
        sb.append(s.positionNeedType);
        sb.append("｜需求=");
        sb.append(percent(s.positionNeedScore));
        sb.append("｜供给=");
        sb.append(percent(s.positionSupplyScore));
        sb.append("｜证据=");
        sb.append(percent(s.evidenceScore));
        sb.append("｜反证=");
        sb.append(percent(s.antiEvidenceScore));
        sb.append("｜净证=");
        sb.append(signed(s.netEvidenceScore));
        sb.append("｜健康=");
        sb.append(percent(s.healthScore));
        sb.append("｜稳定=");
        sb.append(percent(s.stabilityScore));
        sb.append("｜集中=");
        sb.append(percent(s.concentrationScore));
        sb.append("｜回归=");
        sb.append(percent(s.returnPower));
        sb.append("｜转移=");
        sb.append(percent(s.transitionPower));
        sb.append("｜同步=");
        sb.append(percent(s.syncPower));
        sb.append("｜恢复=");
        sb.append(percent(s.recoveryPower));
        sb.append("｜延续=");
        sb.append(percent(s.extensionPower));
        sb.append("｜反转=");
        sb.append(percent(s.reversalPower));
        sb.append("｜噪声=");
        sb.append(percent(s.noiseRisk));
        sb.append("｜漂移=");
        sb.append(percent(s.driftRisk));
        sb.append("｜拥挤=");
        sb.append(percent(s.crowdRisk));
        sb.append("｜断层=");
        sb.append(percent(s.breakRisk));
        sb.append("｜伪强=");
        sb.append(percent(s.falseStrongRisk));
        sb.append("｜错位风险=");
        sb.append(percent(s.wrongRisk));
        sb.append("｜样本可信=");
        sb.append(percent(s.sampleReliability));
        sb.append("｜近期有效=");
        sb.append(percent(s.recentValidity));
        sb.append("｜强号=");
        sb.append(s.strongNumberCount);
        sb.append("｜回归号=");
        sb.append(s.returnNumberCount);
        sb.append("｜同步号=");
        sb.append(s.syncNumberCount);
        sb.append("｜风险号=");
        sb.append(s.riskNumberCount);
        if (s.bestNumbers != null && s.bestNumbers.length() > 0) {
            sb.append("｜优势号=");
            sb.append(s.bestNumbers);
        }
        if (s.riskNumbers != null && s.riskNumbers.length() > 0) {
            sb.append("｜风险号明细=");
            sb.append(s.riskNumbers);
        }
        sb.append("｜动作=");
        sb.append(actionAdvice);
        if (rejectCode != null && rejectCode.length() > 0) {
            sb.append("｜拒绝码=");
            sb.append(rejectCode);
        }
        sb.append("｜说明=只判断位置是否健康和是否有需求，不创建BET，最终仍由DecisionGateEngine裁决");
        return sb.toString();
    }

    /* ========================= number list helpers ========================= */

    private double calcConcentration(ArrayList<NumberScore> list) {
        if (list == null || list.size() == 0) {
            return 0.0d;
        }
        double top3 = 0.0d;
        double all = 0.0d;
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n == null) {
                continue;
            }
            all += Math.max(0.0d, n.total);
            if (i < 3) {
                top3 += Math.max(0.0d, n.total);
            }
        }
        if (all <= 0.0d) {
            return 0.0d;
        }
        return clamp01(top3 / all);
    }

    private double weakTopRisk(ArrayList<NumberScore> list) {
        if (list == null || list.size() == 0) {
            return 1.0d;
        }
        double top = 0.0d;
        int count = 0;
        for (int i = 0; i < list.size() && i < 3; i++) {
            NumberScore n = list.get(i);
            if (n != null) {
                top += n.total;
                count++;
            }
        }
        if (count <= 0) {
            return 1.0d;
        }
        double avg = top / ((double) count);
        if (avg >= 0.55d) {
            return 0.0d;
        }
        if (avg <= 0.20d) {
            return 1.0d;
        }
        return clamp01((0.55d - avg) / 0.35d);
    }

    private int countNumbersAbove(ArrayList<NumberScore> list, double line) {
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n != null && n.total >= line) {
                count++;
            }
        }
        return count;
    }

    private int countReturnNumbers(ArrayList<NumberScore> list) {
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n != null && n.returnPower >= 0.45d && n.risk < 0.58d) {
                count++;
            }
        }
        return count;
    }

    private int countRiskNumbers(ArrayList<NumberScore> list, double line) {
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n != null && n.risk >= line) {
                count++;
            }
        }
        return count;
    }

    private int countSyncNumbers(ArrayList<NumberScore> list, double line) {
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n != null && n.syncPower >= line && n.risk < 0.62d) {
                count++;
            }
        }
        return count;
    }

    private int countCrowdNumbers(ArrayList<NumberScore> list, double line) {
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n != null && n.crowdRisk >= line) {
                count++;
            }
        }
        return count;
    }

    private String topNumbersText(ArrayList<NumberScore> list, boolean riskMode) {
        if (list == null || list.size() == 0) {
            return "";
        }
        ArrayList<NumberScore> tmp = new ArrayList<NumberScore>();
        for (int i = 0; i < list.size(); i++) {
            NumberScore n = list.get(i);
            if (n != null) {
                tmp.add(n);
            }
        }
        if (riskMode) {
            Collections.sort(
                    tmp,
                    new Comparator<NumberScore>() {
                        public int compare(NumberScore a, NumberScore b) {
                            if (a.risk > b.risk) {
                                return -1;
                            }
                            if (a.risk < b.risk) {
                                return 1;
                            }
                            return a.number - b.number;
                        }
                    });
        }
        StringBuilder sb = new StringBuilder();
        int max = Math.min(3, tmp.size());
        for (int i = 0; i < max; i++) {
            NumberScore n = tmp.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append(n.number);
            sb.append("(");
            if (riskMode) {
                sb.append(percent(n.risk));
            } else {
                sb.append(percent(n.total));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /* ========================= string merge / reflection ========================= */

    private void appendReason(Object c, String add) {
        if (c == null || add == null || add.length() == 0) {
            return;
        }
        String old = getString(c, "reasonSummary", getString(c, "reasonText", ""));
        if (old == null || old.length() == 0) {
            putString(c, "reasonSummary", add);
            putString(c, "reasonText", add);
        } else if (old.indexOf(add) < 0) {
            putString(c, "reasonSummary", old + "｜" + add);
            putString(c, "reasonText", old + "｜" + add);
        }
    }

    private String mergeRejectCode(String old, String add) {
        if (add == null || add.length() == 0) {
            return old == null ? "" : old;
        }
        if (old == null || old.length() == 0) {
            return add;
        }
        if (old.indexOf(add) >= 0) {
            return old;
        }
        return old + "|" + add;
    }

    private String mergeClusterKey(String old, String add) {
        if (add == null || add.length() == 0) {
            return old == null ? "" : old;
        }
        if (old == null || old.length() == 0) {
            return add;
        }
        if (old.indexOf(add) >= 0) {
            return old;
        }
        return old + "||" + add;
    }

    private String mergeActionAdvice(String old, String add) {
        if (add == null || add.length() == 0) {
            return old == null ? "" : old;
        }
        if (old == null || old.length() == 0) {
            return add;
        }
        if (old.indexOf(add) >= 0) {
            return old;
        }
        return old + "|" + add;
    }

    private boolean isValidPosition(int position) {
        return position >= POSITION_MIN && position <= POSITION_MAX;
    }

    private double maxNonZero(double a, double b) {
        if (a <= 0.0d) {
            return clamp01(b);
        }
        if (b <= 0.0d) {
            return clamp01(a);
        }
        return clamp01(Math.max(a, b));
    }

    private double posPart(double v) {
        if (v <= 0.0d) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private double negPart(double v) {
        if (v >= 0.0d) {
            return 0.0d;
        }
        v = -v;
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private double clamp01(double v) {
        if (v < 0.0d) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private double clampSigned(double v) {
        if (v < -1.0d) {
            return -1.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private String signed(double v) {
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private String level(double v) {
        v = clamp01(v);
        if (v >= 0.75d) {
            return "HIGH";
        }
        if (v >= 0.55d) {
            return "MID";
        }
        if (v >= 0.35d) {
            return "LOW";
        }
        return "BAD";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int getInt(Object obj, String field, int fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }
            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return fallback;
    }

    private double getDouble(Object obj, String field, double fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }
            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return fallback;
    }

    private String getString(Object obj, String field, String fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }
        return fallback;
    }

    private void putInt(Object obj, String field, int value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }
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

    private void putLong(Object obj, String field, long value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }
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

    private void putDouble(Object obj, String field, double value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }
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

    private void putString(Object obj, String field, String value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }
        try {
            Field f = obj.getClass().getField(field);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    /* ========================= internal models ========================= */

    private static final class PositionStat {
        int position;
        int sampleCount;
        int recentSampleCount;
        int similarSampleCount;
        double healthScore;
        double stabilityScore;
        double riskScore;
        double noiseRisk;
        double driftRisk;
        double concentrationScore;
        double returnPower;
        double transitionPower;
        double recoveryPower;
        double falseStrongRisk;
        double wrongRisk;
        double positionNeedScore;
        double positionSupplyScore;
        double evidenceScore;
        double antiEvidenceScore;
        double netEvidenceScore;
        double sampleReliability;
        double recentValidity;
        double similarHitRate;
        double crowdRisk;
        double syncPower;
        double breakRisk;
        double reversalPower;
        double extensionPower;
        int strongNumberCount;
        int riskNumberCount;
        int returnNumberCount;
        int syncNumberCount;
        int crowdNumberCount;
        String bestNumbers;
        String riskNumbers;
        String stateType;
        String positionNeedType;
    }

    private static final class NumberScore {
        int number;
        double featureScore;
        double confidence;
        double returnPower;
        double transitionPower;
        double recoveryPower;
        double positionNeed;
        double numberSupply;
        double evidence;
        double antiEvidence;
        double recentValidity;
        double syncPower;
        double crowdRisk;
        double extensionPower;
        double reversalPower;
        double risk;
        double total;
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:PositionCauseMatrixEngine";

    public static final class V57PositionEvidence {
        public int position;
        public double support;
        public double counter;
        public double exclusive;
        public double stability;
        public int sample;
        public String source = "";
        public String hash = "";

        public String canonical() {
            return position + '|' + support + '|' + counter + '|' + exclusive + '|' + stability
                    + '|' + sample + '|' + source;
        }
    }

    public static V57PositionEvidence v57b08PositionEvidence(
            int position,
            double support,
            double counter,
            double exclusive,
            double stability,
            int sample,
            String source) {
        V57PositionEvidence e = new V57PositionEvidence();
        e.position = Math.max(1, Math.min(10, position));
        e.support = v57b08Bound(support);
        e.counter = v57b08Bound(counter);
        e.exclusive = v57b08Bound(exclusive);
        e.stability = v57b08Bound(stability);
        e.sample = Math.max(0, sample);
        e.source = source == null ? "" : source;
        e.hash = v57b08Hash(e.canonical());
        return e;
    }

    public static boolean v57b08OwnsFinalPosition() {
        return false;
    }

    private static double v57b08Bound(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 0.0d;
        return Math.max(0.0d, Math.min(1.0d, x));
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
