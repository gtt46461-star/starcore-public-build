package com.ethk3.novaonepro;

import java.util.List;

/**
 * Cross-position conditional lift tensor. Uses only draws strictly before target. Output is
 * evidence; it never mutates the canonical probability owner directly.
 */
public final class RelationTensorEngine implements NumberExpert {
    public static final String MODULE_VERSION = V49BuildManifest.EXPERT_SCHEMA + ":RELATION_TENSOR";

    public String name() {
        return "RELATION_TENSOR";
    }

    public String version() {
        return MODULE_VERSION;
    }

    public Result evaluate(Context c) {
        Result r = new Result(name(), version());
        if (c == null || c.history == null || c.features == null || c.history.size() < 80) {
            r.valid = false;
            return r;
        }
        List<Models.Draw> h = c.history;
        Models.Draw last = h.get(h.size() - 1);
        int start = Math.max(1, h.size() - 720);
        for (int targetP = 0; targetP < 10; targetP++) {
            double[] score = new double[10];
            double evidenceMass = 0.0d;
            for (int n = 0; n < 10; n++) {
                double base = c.features.frequency[6][targetP][n];
                score[n] = FeatureContractRegistry.safeLog(base);
            }
            for (int sourceP = 0; sourceP < 10; sourceP++) {
                if (sourceP == targetP) continue;
                int sourceValue = last.getNumberAtPosition(sourceP + 1);
                if (sourceValue < 1 || sourceValue > 10) continue;
                int[] count = new int[10];
                int support = 0;
                for (int i = start; i < h.size(); i++) {
                    Models.Draw prev = h.get(i - 1);
                    Models.Draw next = h.get(i);
                    if (prev.getNumberAtPosition(sourceP + 1) != sourceValue) continue;
                    int outcome = next.getNumberAtPosition(targetP + 1);
                    if (outcome >= 1 && outcome <= 10) {
                        count[outcome - 1]++;
                        support++;
                    }
                }
                if (support < 5) continue;
                double reliability = Math.min(0.22d, Math.log1p(support) / 35.0d);
                for (int n = 0; n < 10; n++) {
                    double cond = (count[n] + 0.5d) / (support + 5.0d);
                    double base = Math.max(1.0e-6d, c.features.frequency[6][targetP][n]);
                    double lift = Math.log(cond / base);
                    score[n] += reliability * Math.max(-1.2d, Math.min(1.2d, lift));
                }
                evidenceMass += reliability;
            }
            V49ExpertSupport.softmax(score, r.probability[targetP]);
            r.trust[targetP] = FeatureContractRegistry.clamp01(0.25d + evidenceMass * 0.55d);
            r.uncertainty[targetP] =
                    FeatureContractRegistry.clamp01(
                            0.62d
                                    - Math.min(0.35d, evidenceMass)
                                    + 0.18d * c.features.switchRisk[targetP]);
            r.summary[targetP] =
                    "跨位置条件提升张量：effectiveMass="
                            + String.format(java.util.Locale.US, "%.3f", evidenceMass);
        }
        r.normalize();
        return r;
    }
}
