package com.ethk3.novaonepro;

import java.util.List;

/** A V49 expert may only emit evidence and ten-number distributions. */
public interface NumberExpert {
    String name();

    String version();

    Result evaluate(Context context);

    final class Context {
        public final DbStore store;
        public final List<Models.Draw> history;
        public final V49FeatureSnapshot features;

        public Context(DbStore s, List<Models.Draw> h, V49FeatureSnapshot f) {
            store = s;
            history = h;
            features = f;
        }
    }

    final class Result {
        public final String expertName;
        public final String expertVersion;
        public final double[][] probability = new double[10][10];
        public final double[] trust = new double[10];
        public final double[] uncertainty = new double[10];
        public final String[] summary = new String[10];
        public boolean valid;

        public Result(String n, String v) {
            expertName = n == null ? "" : n;
            expertVersion = v == null ? "" : v;
            for (int p = 0; p < 10; p++) {
                for (int x = 0; x < 10; x++) probability[p][x] = 0.10d;
                trust[p] = 0.50d;
                uncertainty[p] = 0.50d;
                summary[p] = "";
            }
            valid = true;
        }

        public void normalize() {
            for (int p = 0; p < 10; p++) {
                FeatureContractRegistry.normalize10(probability[p]);
                trust[p] = FeatureContractRegistry.clamp01(trust[p]);
                uncertainty[p] = FeatureContractRegistry.clamp01(uncertainty[p]);
            }
        }
    }
}
