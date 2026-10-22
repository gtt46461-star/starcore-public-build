package com.ethk3.novaonepro;

/** B11 selective-risk upper-bound gate derived from OOS certificate. */
public final class V51ConformalSelectiveRiskEngine {
    public static final String V57_B11_OWNER = "SELECTIVE_RISK_BOUND";

    public static final class Result {
        public final double empiricalRisk, upperRisk, coverageLower;
        public final boolean pass;
        public final String checksum;

        Result(double e, double u, double c, boolean p) {
            empiricalRisk = e;
            upperRisk = u;
            coverageLower = c;
            pass = p;
            checksum = V51ChecksumEngine.sha256(String.valueOf(e) + '|' + u + '|' + c + '|' + p);
        }
    }

    public Result evaluate(SelectedHitCertificationEngine.Certificate c) {
        if (c == null || c.sample <= 0) return new Result(1, 1, 0, false);
        double risk = 1.0d - c.hitRate;
        double radius = Math.sqrt(Math.log(40.0d) / (2.0d * c.sample));
        double upper = Math.min(1.0d, risk + radius);
        double cov = Math.max(0.0d, c.coverage - radius);
        return new Result(
                risk,
                upper,
                cov,
                c.artifactProven && cov >= 0.15d && upper <= 0.65d && c.ece <= 0.04d);
    }
}
