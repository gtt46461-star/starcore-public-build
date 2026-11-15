package com.ethk3.novaonepro;

/** Shared evidence construction without assigning final probability ownership. */
public final class V51ExpertSupport {
    private V51ExpertSupport() {}

    public static V51Models.ExpertEvidence evidence(
            String id,
            String artifact,
            double[][] raw,
            double baseTrust,
            String evidence,
            String counter) {
        double[] trust = new double[10], uncertainty = new double[10], signal = new double[10];
        String[] e = new String[10], c = new String[10];
        for (int p = 0; p < 10; p++) {
            double mean = 0;
            for (int n = 0; n < 10; n++) mean += raw[p][n];
            mean /= 10.0d;
            double var = 0;
            for (int n = 0; n < 10; n++) {
                double d = raw[p][n] - mean;
                var += d * d;
            }
            var /= 10.0d;
            signal[p] = V51Math.clamp(Math.sqrt(var) / 1.5d, 0, 1);
            trust[p] = V51Math.clamp(baseTrust * (0.35d + 0.65d * signal[p]), 0, 1);
            uncertainty[p] = V51Math.clamp(1.0d - signal[p] * trust[p], 0, 1);
            e[p] = evidence;
            c[p] = counter;
        }
        String hash =
                V51ChecksumEngine.sha256(
                        id + '|' + artifact + '|' + V51ChecksumEngine.hashDoubles(raw));
        return new V51Models.ExpertEvidence(
                id, artifact, raw, trust, uncertainty, signal, e, c, hash);
    }

    public static double logFrequency(double value) {
        return Math.log(Math.max(1.0e-6d, value));
    }
}
