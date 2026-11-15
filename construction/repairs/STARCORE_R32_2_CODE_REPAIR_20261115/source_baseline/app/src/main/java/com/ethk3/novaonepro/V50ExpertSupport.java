package com.ethk3.novaonepro;

import java.util.List;

/** Package-private deterministic helpers shared by V50 evidence-only experts. */
final class V50ExpertSupport {
    private V50ExpertSupport() {}

    static double[][] emptyMatrix() {
        double[][] out = new double[10][10];
        for (int p = 0; p < 10; p++) {
            for (int n = 0; n < 10; n++) out[p][n] = 0.1d;
        }
        return out;
    }

    static V50Models.NumberFeatureRow row(
            V50Models.NumberFeatureSnapshot snapshot, int position, int number) {
        if (snapshot == null || snapshot.rows == null) return null;
        int expected = (position - 1) * 10 + (number - 1);
        if (expected >= 0 && expected < snapshot.rows.length) {
            V50Models.NumberFeatureRow candidate = snapshot.rows[expected];
            if (candidate != null && candidate.position == position && candidate.number == number)
                return candidate;
        }
        for (int i = 0; i < snapshot.rows.length; i++) {
            V50Models.NumberFeatureRow candidate = snapshot.rows[i];
            if (candidate != null && candidate.position == position && candidate.number == number)
                return candidate;
        }
        return null;
    }

    static V50Models.StateTrajectorySnapshot state(
            V50Models.StateTrajectorySnapshot[] states, int position) {
        if (states == null) return null;
        int index = position - 1;
        if (index >= 0
                && index < states.length
                && states[index] != null
                && states[index].position == position) return states[index];
        for (int i = 0; i < states.length; i++) {
            if (states[i] != null && states[i].position == position) return states[i];
        }
        return null;
    }

    static void normalize(double[][] matrix) {
        if (matrix == null) return;
        for (int p = 0; p < matrix.length; p++) {
            if (matrix[p] != null && matrix[p].length == 10) {
                FeatureContractRegistry.normalize10(matrix[p]);
            }
        }
    }

    static double signal(double[] probability) {
        if (probability == null || probability.length != 10) return 0.0d;
        double l1 = 0.0d;
        for (int n = 0; n < 10; n++) l1 += Math.abs(probability[n] - 0.1d);
        return clamp01(l1 / 1.8d);
    }

    static double uncertainty(double[] probability) {
        return FeatureContractRegistry.entropy10(probability);
    }

    static V50Models.ExpertEvidence evidence(
            String id,
            double[][] probability,
            double[] trust,
            String[] reasons,
            String[] counters,
            String artifactId) {
        normalize(probability);
        double[] uncertainty = new double[10];
        double[] strength = new double[10];
        StringBuilder checksum = new StringBuilder(safe(id)).append('|').append(safe(artifactId));
        for (int p = 0; p < 10; p++) {
            uncertainty[p] = uncertainty(probability[p]);
            strength[p] = signal(probability[p]);
            checksum.append('|')
                    .append(V50ChecksumEngine.hashDoubles("P" + p, probability[p]))
                    .append(':')
                    .append(
                            Double.doubleToLongBits(
                                    trust == null || p >= trust.length ? 0.0d : trust[p]));
        }
        return new V50Models.ExpertEvidence(
                id,
                probability,
                trust,
                uncertainty,
                strength,
                reasons,
                counters,
                artifactId,
                V50ChecksumEngine.sha256(checksum.toString()));
    }

    static double[] trust(double value) {
        double[] out = new double[10];
        double clamped = clamp01(value);
        for (int i = 0; i < 10; i++) out[i] = clamped;
        return out;
    }

    static String[] text(String value) {
        String[] out = new String[10];
        for (int i = 0; i < 10; i++) out[i] = safe(value);
        return out;
    }

    static int historyNumber(List<Models.Draw> history, int index, int position) {
        if (history == null || index < 0 || index >= history.size()) return 0;
        Models.Draw draw = history.get(index);
        return draw == null ? 0 : draw.getNumberAtPosition(position);
    }

    static double clamp01(double value) {
        return FeatureContractRegistry.clamp01(value);
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }
}
