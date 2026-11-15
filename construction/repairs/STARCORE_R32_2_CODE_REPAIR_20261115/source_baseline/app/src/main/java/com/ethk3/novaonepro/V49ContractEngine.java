package com.ethk3.novaonepro;

import java.util.Locale;

/** Immutable prediction contract hashing and verification. */
public final class V49ContractEngine {
    private V49ContractEngine() {}

    public static String candidateHash(Models.ComboCandidate c, String featureChecksum) {
        if (c == null) return "";
        String payload =
                V49BuildManifest.STRATEGY_VERSION
                        + '|'
                        + safe(c.baseIssue)
                        + '|'
                        + safe(c.targetIssue)
                        + '|'
                        + c.position
                        + '|'
                        + c.n1
                        + ','
                        + c.n2
                        + ','
                        + c.n3
                        + '|'
                        + fmt(c.finalP)
                        + '|'
                        + fmt(c.dynamicGate)
                        + '|'
                        + fmt(c.v43StrategySupport)
                        + '|'
                        + fmt(c.coverageQualityScore)
                        + '|'
                        + fmt(c.k3CoverageRisk)
                        + '|'
                        + safe(c.stateKey)
                        + '|'
                        + safe(c.v46Lane)
                        + '|'
                        + safe(featureChecksum)
                        + '|'
                        + FeatureContractRegistry.contractChecksum();
        return FeatureContractRegistry.sha256(payload);
    }

    public static boolean verify(Models.ComboCandidate c, String featureChecksum) {
        if (c == null || c.v43EvidenceFingerprint == null || c.v43EvidenceFingerprint.length() == 0)
            return false;
        return c.v43EvidenceFingerprint.equals(candidateHash(c, featureChecksum));
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.12f", v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
