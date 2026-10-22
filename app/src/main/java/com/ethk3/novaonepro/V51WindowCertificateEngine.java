package com.ethk3.novaonepro;

/** B11 window certificate; a window cannot self-prove from the same target label. */
public final class V51WindowCertificateEngine {
    public static final String V57_B11_OWNER = "WINDOW_CERTIFICATE";

    public static final class Result {
        public final String windowId;
        public final int sample;
        public final double hitRate, lcb, ece, drift;
        public final boolean proven;
        public final String reason, checksum;

        Result(String id, int n, double h, double l, double e, double d, boolean p, String r) {
            windowId = id;
            sample = n;
            hitRate = h;
            lcb = l;
            ece = e;
            drift = d;
            proven = p;
            reason = r;
            checksum =
                    V51ChecksumEngine.sha256(
                            id + '|' + n + '|' + h + '|' + l + '|' + e + '|' + d + '|' + p + '|'
                                    + r);
        }
    }

    public Result certify(SelectedHitCertificationEngine.Certificate c, String windowId) {
        if (c == null) return new Result(windowId, 0, 0, 0, 1, 1, false, "NO_CERT");
        boolean p =
                c.artifactProven
                        && c.sample >= 120
                        && c.hitRate >= 0.40
                        && c.wilsonLower >= 0.35
                        && c.ece <= 0.04
                        && c.driftRisk <= 0.08;
        return new Result(
                windowId,
                c.sample,
                c.hitRate,
                c.wilsonLower,
                c.ece,
                c.driftRisk,
                p,
                p ? "PROVEN_OOS" : "NOT_PROVEN");
    }
}
