package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Final active probability core for V22.
 *
 * <p>The old project repeatedly added and subtracted partially duplicated scores. This engine
 * instead performs one final reliability-weighted consensus after every upstream expert has
 * finished. Experts with poor settled performance are down-weighted or, when sufficiently
 * anti-correlated, used in the opposite ordering direction. No-sample Teacher/Dynamic evidence is
 * neutral. Frequency debt never changes a probability.
 */
public final class AdaptiveExpertConsensusEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final double BASE = 0.30d;
    private final EvidenceHealthSnapshotEngine healthEngine = new EvidenceHealthSnapshotEngine();

    private static final class Accumulator {
        double sumWeight;
        double sumLogitDelta;
        double sumP;
        double sumP2;
        int sourceCount;

        void add(double p, double w) {
            if (!validProbability(p) || w <= 0.0d) return;
            p = clamp(p, 0.05d, 0.65d);
            w = clamp(w, 0.0d, 1.50d);
            sumWeight += w;
            sumLogitDelta += w * (logit(p) - logit(BASE));
            sumP += w * p;
            sumP2 += w * p * p;
            sourceCount++;
        }
    }

    public ArrayList<Models.ComboCandidate> apply(
            DbStore store, List<Models.ComboCandidate> input) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input == null) return out;
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "AdaptiveExpertConsensusEngine",
                        MODULE_VERSION,
                        "ACTIVE_FINAL_PROBABILITY");
        } catch (Throwable ignore) {
        }
        EvidenceHealthSnapshotEngine.Snapshot health = healthEngine.load(store);
        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null || !c.isValidK3()) continue;
            fuseOne(c, health);
            out.add(c);
        }
        sort(out);
        try {
            if (store != null) {
                store.setState("adaptive_consensus_version", MODULE_VERSION);
                store.setState("adaptive_consensus_candidate_count", String.valueOf(out.size()));
                store.setState(
                        "adaptive_consensus_best", out.size() == 0 ? "" : compact(out.get(0)));
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private void fuseOne(Models.ComboCandidate c, EvidenceHealthSnapshotEngine.Snapshot h) {
        Accumulator a = new Accumulator();
        addLegacy(a, c.modelP, h, EvidenceHealthSnapshotEngine.MODEL);
        addLegacy(a, c.shapeP, h, EvidenceHealthSnapshotEngine.SHAPE);
        addLegacy(a, c.comboP, h, EvidenceHealthSnapshotEngine.COMBO);
        addLegacy(a, c.recoveryP, h, EvidenceHealthSnapshotEngine.RECOVERY);
        addLegacy(a, c.similarP, h, EvidenceHealthSnapshotEngine.SIMILAR);
        addLegacy(a, c.memoryP, h, EvidenceHealthSnapshotEngine.MEMORY);
        addLegacy(a, c.rawP, h, EvidenceHealthSnapshotEngine.RAW);
        addLegacy(a, c.finalP, h, EvidenceHealthSnapshotEngine.FINAL);

        double priorWeight = 0.0d;
        if (c.historicalPriorSample >= 24 && validProbability(c.historicalPriorP)) {
            priorWeight =
                    clamp(
                            0.15d
                                    + 0.55d
                                            * clamp01(c.historicalPriorConfidence)
                                            * Math.min(
                                                    1.0d,
                                                    Math.sqrt(c.historicalPriorSample / 360.0d)),
                            0.10d,
                            0.62d);
            a.add(c.historicalPriorP, priorWeight);
        }

        double forestWeight = 0.0d;
        if (c.evidenceForestTreeCount >= 3
                && c.evidenceForestSample >= 6
                && validProbability(c.evidenceForestP)) {
            forestWeight = clamp(0.08d + 0.34d * clamp01(c.evidenceForestConfidence), 0.08d, 0.40d);
            a.add(c.evidenceForestP, forestWeight);
        }

        double teacherWeight = 0.0d;
        int teacherSample =
                Math.max(
                        c.teacherDomainRealSample,
                        Math.max(c.teacherDomainShadowSample, c.teacherMatrixSample));
        String teacherSource = safe(c.teacherFieldSource).toUpperCase(Locale.US);
        boolean teacherDuplicatesPrior = teacherSource.indexOf("HISTORICAL_STATE_PRIOR") >= 0;
        if (!teacherDuplicatesPrior
                && teacherSample >= 24
                && validProbability(c.teacherMatrixHitRate)) {
            teacherWeight =
                    clamp(
                            0.10d
                                    + 0.42d
                                            * clamp01(c.teacherMatrixConfidence)
                                            * Math.min(1.0d, Math.sqrt(teacherSample / 120.0d)),
                            0.08d,
                            0.48d);
            a.add(c.teacherMatrixHitRate, teacherWeight);
        }

        double dynamicWeight = 0.0d;
        int dynamicSample = c.dynamicRealSample + c.dynamicShadowSample;
        if (dynamicSample >= 24 && validProbability(c.dynamicClusterHitRate)) {
            dynamicWeight =
                    clamp(
                            0.08d + 0.35d * Math.min(1.0d, Math.sqrt(dynamicSample / 120.0d)),
                            0.08d,
                            0.42d);
            a.add(c.dynamicClusterHitRate, dynamicWeight);
        }

        double weightedMean = a.sumWeight <= 0.0d ? BASE : a.sumP / a.sumWeight;
        double variance =
                a.sumWeight <= 0.0d
                        ? 0.0d
                        : Math.max(0.0d, a.sumP2 / a.sumWeight - weightedMean * weightedMean);
        double agreement = clamp01(1.0d - Math.sqrt(variance) / 0.105d);
        double settledFactor =
                h == null ? 0.0d : Math.min(1.0d, Math.sqrt(h.settledSample / 120.0d));
        double sourceFactor = Math.min(1.0d, a.sourceCount / 6.0d);
        double confidence =
                clamp(
                        0.28d + 0.32d * agreement + 0.20d * settledFactor + 0.20d * sourceFactor,
                        0.30d,
                        0.96d);

        double consensus =
                a.sumWeight <= 0.0d ? BASE : logistic(logit(BASE) + a.sumLogitDelta / a.sumWeight);
        double rawConsensus = BASE + (consensus - BASE) * confidence;

        /* When the recently selected lane is clearly below baseline, do not keep trusting
         * its old final score.  The source-level health snapshot already reorders/inverts
         * anti-correlated experts; this extra shrink only prevents stale overconfidence. */
        if (h != null && h.settledSample >= 30 && h.hitRate < 0.245d) {
            double badLane = clamp((0.245d - h.hitRate) / 0.12d, 0.0d, 1.0d);
            rawConsensus = BASE + (rawConsensus - BASE) * (1.0d - 0.28d * badLane);
            confidence *= (1.0d - 0.18d * badLane);
        }

        rawConsensus = clamp(rawConsensus, 0.20d, 0.48d);
        double calibrated = clamp(rawConsensus, 0.20d, 0.48d);

        c.basePBeforeFusion = c.finalP > 0.0d ? c.finalP : (c.rawP > 0.0d ? c.rawP : BASE);
        c.rawP = rawConsensus;
        c.calibratedP = calibrated;
        c.finalP = calibrated;
        c.finalPAfterFusion = calibrated;
        c.fusionWeightBase = a.sumWeight;
        c.fusionWeightPrior = priorWeight;
        c.fusionWeightTeacher = teacherWeight;
        c.fusionWeightDynamic = dynamicWeight;
        c.fusionWeightForest = forestWeight;
        c.edge = c.finalP - (c.dynamicGate > 0.0d ? c.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE);
        c.finalPAfterDiagnosis = c.finalP;
        c.probabilityPipelineVersion = MODULE_VERSION;
        c.probabilityFieldVersion = MODULE_VERSION;
        c.moduleBuildVersion = MODULE_VERSION;
        c.fusionSummary =
                "V22统一专家共识：Final="
                        + pct(c.finalP)
                        + "｜confidence="
                        + pct(confidence)
                        + "｜sources="
                        + a.sourceCount
                        + "｜agreement="
                        + pct(agreement)
                        + "｜priorW="
                        + fmt(priorWeight)
                        + "｜forestW="
                        + fmt(forestWeight)
                        + "｜teacherW="
                        + fmt(teacherWeight)
                        + "｜dynamicW="
                        + fmt(dynamicWeight)
                        + "｜recent="
                        + (h == null ? "0/0.0%" : h.settledSample + "/" + pct(h.hitRate))
                        + "｜频率债务不参与概率；无样本证据中性；负相关专家自动反向排序";
        c.probabilityActionAdvice = append(c.probabilityActionAdvice, c.fusionSummary);
        c.reasonSummary = append(c.reasonSummary, c.fusionSummary);
    }

    private static void addLegacy(
            Accumulator a, double p, EvidenceHealthSnapshotEngine.Snapshot h, int index) {
        if (!validProbability(p)) return;
        double weight = h == null ? 0.25d : h.weight[index];
        boolean invert = h != null && h.invert[index];
        a.add(EvidenceHealthSnapshotEngine.orient(p, invert), weight);
    }

    private static void sort(ArrayList<Models.ComboCandidate> list) {
        for (int i = 1; i < list.size(); i++) {
            Models.ComboCandidate x = list.get(i);
            double xs = score(x);
            int j = i - 1;
            while (j >= 0 && score(list.get(j)) < xs) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, x);
        }
        for (int i = 0; i < list.size(); i++) list.get(i).rank = i + 1;
    }

    private static double score(Models.ComboCandidate c) {
        if (c == null) return -999.0d;
        double cover = Math.max(c.coverageQuality, c.coverageQualityScore);
        double third = Math.max(c.thirdQuality, c.thirdNumberQualityScore);
        double hard = c.hardBlockFlag == 1 ? 1.0d : 0.0d;
        return c.finalP * 1.20d
                + c.rawP * 0.22d
                + cover * 0.025d
                + third * 0.018d
                - c.counterRisk * 0.025d
                - hard * 1.0d;
    }

    private static String compact(Models.ComboCandidate c) {
        if (c == null) return "";
        return "P"
                + c.position
                + "["
                + c.n1
                + ","
                + c.n2
                + ","
                + c.n3
                + "]"
                + "/Final="
                + pct(c.finalP)
                + "/Raw="
                + pct(c.rawP)
                + "/Gate="
                + pct(c.dynamicGate);
    }

    private static boolean validProbability(double p) {
        return !Double.isNaN(p) && !Double.isInfinite(p) && p >= 0.05d && p <= 0.75d;
    }

    private static double logit(double p) {
        p = clamp(p, 0.0001d, 0.9999d);
        return Math.log(p / (1.0d - p));
    }

    private static double logistic(double x) {
        return 1.0d / (1.0d + Math.exp(-x));
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String append(String a, String b) {
        if (a == null || a.length() == 0) return b == null ? "" : b;
        if (b == null || b.length() == 0) return a;
        return a + "；" + b;
    }

    private static String pct(double p) {
        return String.format(Locale.US, "%.1f%%", p * 100.0d);
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.3f", v);
    }
}
