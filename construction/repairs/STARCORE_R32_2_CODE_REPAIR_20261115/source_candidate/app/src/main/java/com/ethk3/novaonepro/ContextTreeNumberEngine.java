package com.ethk3.novaonepro;

import java.util.List;

/** Variable-order context tree using Krichevsky-Trofimov smoothing. */
public final class ContextTreeNumberEngine implements NumberExpert {
    public static final String MODULE_VERSION = V49BuildManifest.EXPERT_SCHEMA + ":CTW";

    public String name() {
        return "CONTEXT_TREE";
    }

    public String version() {
        return MODULE_VERSION;
    }

    public Result evaluate(Context c) {
        Result r = new Result(name(), version());
        if (c == null || c.features == null || c.history == null || c.history.size() < 12) {
            r.valid = false;
            return r;
        }
        List<Models.Draw> h = c.history;
        int maxOrder = Math.min(AppConfig.V49_CONTEXT_MAX_ORDER, Math.max(1, h.size() / 40));
        for (int p = 0; p < 10; p++) {
            double[] mix = new double[10];
            double totalWeight = 0.0d;
            int strongestSupport = 0;
            int strongestOrder = 0;
            for (int order = 0; order <= maxOrder; order++) {
                int[] counts = new int[10];
                int support = 0;
                for (int i = order; i < h.size(); i++) {
                    if (!contextMatches(h, p + 1, i, order)) continue;
                    int next = h.get(i).getNumberAtPosition(p + 1);
                    if (next >= 1 && next <= 10) {
                        counts[next - 1]++;
                        support++;
                    }
                }
                double evidence = Math.log1p(support) / (1.0d + order * 0.45d);
                if (order > 0 && support < 3) evidence *= 0.15d;
                if (support > strongestSupport) {
                    strongestSupport = support;
                    strongestOrder = order;
                }
                if (evidence <= 0.0d) continue;
                double denom = support + 5.0d;
                for (int n = 0; n < 10; n++) {
                    double kt = (counts[n] + 0.5d) / denom;
                    mix[n] += evidence * kt;
                }
                totalWeight += evidence;
            }
            if (totalWeight <= 0.0d) {
                for (int n = 0; n < 10; n++) r.probability[p][n] = c.features.frequency[4][p][n];
            } else {
                for (int n = 0; n < 10; n++) r.probability[p][n] = mix[n] / totalWeight;
            }
            FeatureContractRegistry.normalize10(r.probability[p]);
            r.trust[p] =
                    FeatureContractRegistry.clamp01(0.28d + Math.log1p(strongestSupport) / 8.0d);
            r.uncertainty[p] =
                    FeatureContractRegistry.clamp01(
                            FeatureContractRegistry.entropy10(r.probability[p])
                                    + (strongestSupport < 8 ? 0.18d : 0.0d));
            r.summary[p] = "CTW可变阶上下文：order=" + strongestOrder + " support=" + strongestSupport;
        }
        r.normalize();
        return r;
    }

    private static boolean contextMatches(
            List<Models.Draw> h, int position, int nextIndex, int order) {
        if (order == 0) return true;
        int currentEnd = h.size() - 1;
        if (nextIndex - order < 0 || currentEnd - order + 1 < 0) return false;
        for (int k = 0; k < order; k++) {
            int a = h.get(nextIndex - order + k).getNumberAtPosition(position);
            int b = h.get(currentEnd - order + 1 + k).getNumberAtPosition(position);
            if (a != b) return false;
        }
        return true;
    }
}
