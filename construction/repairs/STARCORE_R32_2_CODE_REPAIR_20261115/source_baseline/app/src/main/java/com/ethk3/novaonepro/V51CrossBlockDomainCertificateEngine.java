package com.ethk3.novaonepro;

/** B11 cross-block veto. At least 3/4 blocks must be stable; one catastrophic block vetoes REAL. */
public final class V51CrossBlockDomainCertificateEngine {
    public static final String V57_B11_OWNER = "CROSS_BLOCK_CERTIFICATE";

    public static final class Result {
        public final boolean pass;
        public final int stableBlocks;
        public final double blockMinimum, drift;
        public final String reason, checksum;

        Result(boolean p, int s, double b, double d, String r) {
            pass = p;
            stableBlocks = s;
            blockMinimum = b;
            drift = d;
            reason = r;
            checksum =
                    V51ChecksumEngine.sha256(
                            String.valueOf(p) + '|' + s + '|' + b + '|' + d + '|' + r);
        }
    }

    public Result certify(SelectedHitCertificationEngine.Certificate c) {
        if (c == null) return new Result(false, 0, 0, 1, "NO_CERT");
        boolean p =
                c.artifactProven
                        && c.stableBlocks >= 3
                        && c.blockMinimum >= 0.33d
                        && c.driftRisk <= 0.08d;
        return new Result(
                p, c.stableBlocks, c.blockMinimum, c.driftRisk, p ? "PASS" : "BLOCK_OR_DRIFT_VETO");
    }
}
