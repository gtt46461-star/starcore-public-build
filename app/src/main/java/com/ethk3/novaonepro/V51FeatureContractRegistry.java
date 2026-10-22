package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/** Exact production/training feature contract. */
public final class V51FeatureContractRegistry {
    private V51FeatureContractRegistry() {}

    public static final class Feature {
        public final String id,
                formula,
                missing,
                normalization,
                owner,
                pythonName,
                javaName,
                version;
        public final int window;
        public final double minimum, maximum;

        public Feature(
                String id,
                String formula,
                int window,
                String missing,
                double minimum,
                double maximum,
                String normalization,
                String owner,
                String pythonName,
                String javaName,
                String version) {
            this.id = id;
            this.formula = formula;
            this.window = window;
            this.missing = missing;
            this.minimum = minimum;
            this.maximum = maximum;
            this.normalization = normalization;
            this.owner = owner;
            this.pythonName = pythonName;
            this.javaName = javaName;
            this.version = version;
        }
    }

    public static Feature[] all() {
        List<Feature> x = new ArrayList<Feature>();
        add(
                x,
                "position_norm",
                "(position-1)/9",
                0,
                "error",
                0,
                1,
                "identity",
                "V51TrajectoryFeatureEngine");
        add(
                x,
                "number_norm",
                "(number-1)/9",
                0,
                "error",
                0,
                1,
                "identity",
                "V51TrajectoryFeatureEngine");
        int[] windows = new int[] {4, 6, 8, 12, 18, 24, 36, 48, 72, 96};
        for (int i = 0; i < windows.length; i++)
            add(
                    x,
                    "freq_w" + windows[i],
                    "(count+0.5)/(observed+5), row-normalized",
                    windows[i],
                    "hierarchical-prior",
                    0,
                    1,
                    "identity",
                    "V51TrajectoryFeatureEngine");
        add(
                x,
                "gap_norm",
                "min(gap,96)/96",
                96,
                "1",
                0,
                1,
                "identity",
                "V51TrajectoryFeatureEngine");
        add(
                x,
                "gap_percentile",
                "completed-gaps empirical CDF",
                0,
                "parent-CDF",
                0,
                1,
                "identity",
                "V51TrajectoryFeatureEngine");
        add(
                x,
                "trajectory_velocity",
                "mean(position_delta) over W12",
                12,
                "0",
                -1,
                1,
                "divide-9",
                "V51TrajectoryFeatureEngine");
        add(
                x,
                "trajectory_acceleration",
                "short velocity - medium velocity",
                24,
                "0",
                -2,
                2,
                "divide-9",
                "V51TrajectoryFeatureEngine");
        add(
                x,
                "trajectory_reversal",
                "sign changes of position delta / transitions",
                12,
                "0",
                0,
                1,
                "identity",
                "V51TrajectoryFeatureEngine");
        add(
                x,
                "kendall_tau",
                "1-4*inversions/(n*(n-1))",
                1,
                "0",
                -1,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "mean_abs_displacement",
                "mean(abs(currentPos-prevPos))/9",
                1,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "max_displacement",
                "max(abs(currentPos-prevPos))/9",
                1,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "fixed_point_rate",
                "fixedPoints/10",
                1,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "cycle_rate",
                "nontrivialCycles/10",
                1,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "adjacency_preservation",
                "preserved directed adjacencies/9",
                1,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "region_exchange",
                "numbers crossing thirds/10",
                1,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        add(
                x,
                "permutation_entropy",
                "entropy(abs displacement histogram)",
                12,
                "0",
                0,
                1,
                "identity",
                "V51PermutationFeatureEngine");
        return x.toArray(new Feature[x.size()]);
    }

    private static void add(
            List<Feature> x,
            String id,
            String formula,
            int window,
            String missing,
            double min,
            double max,
            String norm,
            String owner) {
        x.add(new Feature(id, formula, window, missing, min, max, norm, owner, id, id, "V51F1"));
    }

    public static String fingerprint() {
        StringBuilder b = new StringBuilder();
        Feature[] x = all();
        for (int i = 0; i < x.length; i++)
            b.append(x[i].id)
                    .append('|')
                    .append(x[i].formula)
                    .append('|')
                    .append(x[i].window)
                    .append('|')
                    .append(x[i].missing)
                    .append('|')
                    .append(x[i].minimum)
                    .append('|')
                    .append(x[i].maximum)
                    .append('|')
                    .append(x[i].normalization)
                    .append('|')
                    .append(x[i].owner)
                    .append(';');
        return V51ChecksumEngine.sha256(b.toString());
    }
}
