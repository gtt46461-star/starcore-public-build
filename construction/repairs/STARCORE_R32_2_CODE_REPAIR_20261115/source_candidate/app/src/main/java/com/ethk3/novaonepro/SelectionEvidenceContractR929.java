package com.ethk3.novaonepro;

import java.util.Locale;

/**
 * R929 typed selected-action evidence.  Score, probability, interval and stability are intentionally
 * different types so no caller can silently clamp a rank score and publish it as hit probability.
 */
public final class SelectionEvidenceContractR929 {
    public static final String REVISION = "R929_SELECTION_EVIDENCE_V1";
    private SelectionEvidenceContractR929() {}

    public static final class SelectionPolicyIdentity {
        public final String rankerArtifact;
        public final String consumerContract;
        public final String weightContract;
        public final String regimeContract;
        public final String tieBreakContract;
        public final String featureManifestHash;
        public final String hash;

        public SelectionPolicyIdentity(String rankerArtifact, String consumerContract,
                                       String weightContract, String regimeContract,
                                       String tieBreakContract, String featureManifestHash) {
            this.rankerArtifact = safe(rankerArtifact);
            this.consumerContract = safe(consumerContract);
            this.weightContract = safe(weightContract);
            this.regimeContract = safe(regimeContract);
            this.tieBreakContract = safe(tieBreakContract);
            this.featureManifestHash = requireHash(featureManifestHash);
            if (this.rankerArtifact.length() == 0 || this.consumerContract.length() == 0
                    || this.weightContract.length() == 0 || this.regimeContract.length() == 0
                    || this.tieBreakContract.length() == 0)
                throw new IllegalArgumentException("R929_SELECTION_POLICY_COMPONENT_EMPTY");
            this.hash = V51ChecksumEngine.sha256(
                    REVISION + '|' + this.rankerArtifact + '|' + this.consumerContract + '|'
                            + this.weightContract + '|' + this.regimeContract + '|'
                            + this.tieBreakContract + '|' + this.featureManifestHash);
        }
    }

    public static SelectionPolicyIdentity installedPolicy() {
        return new SelectionPolicyIdentity(
                ModelBundleRuntimeR932.rankerIdentity(),
                "V51_RUNTIME_RANK1_THEN_CERTIFIED_RESEARCH_MIXERS",
                "CERTIFIED_RESEARCH_WEIGHT_CONTRACT",
                "PREDRAW_STATE_DOMAIN_ONLY",
                "RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",
                V51ActionMetaFeatureEngine.featureManifestHash());
    }

    public static String actualPolicy(String consumer,String candidate,double weight,boolean applied) {
        if(!applied)return installedPolicy().hash;
        if(consumer==null||candidate==null||candidate.length()==0||!Double.isFinite(weight)||weight<=0)
            throw new IllegalArgumentException("R932_APPLIED_SELECTOR_IDENTITY");
        return new SelectionPolicyIdentity(ModelBundleRuntimeR932.rankerIdentity(),
                consumer+":"+candidate,Double.toHexString(weight),DomainRouterR929.installedSchemaHash(),
                "RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",V51ActionMetaFeatureEngine.featureManifestHash()).hash;
    }

    public static final class Probability {
        public final double value;
        public final int supportN;
        public final int reliabilityBinN;
        public final double brier;
        public final double ece;
        public final String calibrationArtifact;
        public final String selectionPolicyHash;
        public final boolean supported;

        public Probability(double value, int supportN, int reliabilityBinN, double brier, double ece,
                           String calibrationArtifact, String selectionPolicyHash, boolean supported) {
            this.value = value;
            this.supportN = Math.max(0, supportN);
            this.reliabilityBinN = Math.max(0, reliabilityBinN);
            this.brier = brier;
            this.ece = ece;
            this.calibrationArtifact = safe(calibrationArtifact);
            this.selectionPolicyHash = requireHash(selectionPolicyHash);
            this.supported = supported;
            if (supported) {
                if (!finite(value) || value < 0 || value > 1)
                    throw new IllegalArgumentException("R929_PROBABILITY_RANGE");
                if (this.supportN <= 0 || this.reliabilityBinN <= 0)
                    throw new IllegalArgumentException("R929_PROBABILITY_SUPPORT_REQUIRED");
                if (!finite(brier) || brier < 0 || !finite(ece) || ece < 0 || ece > 1)
                    throw new IllegalArgumentException("R929_CALIBRATION_METRIC_INVALID");
            }
        }
    }

    /** Wilson is explicitly a group-frequency interval, never an individual-event guarantee. */
    public static final class WilsonInterval {
        public final int n;
        public final int hits;
        public final double lower;
        public final double upper;
        public final String semantics = "GROUP_FREQUENCY_INTERVAL_NOT_INDIVIDUAL_EVENT_PROBABILITY";

        private WilsonInterval(int n, int hits, double lower, double upper) {
            this.n = n; this.hits = hits; this.lower = lower; this.upper = upper;
        }
    }

    public static WilsonInterval wilson95(int hits, int n) {
        if (n <= 0 || hits < 0 || hits > n)
            return new WilsonInterval(Math.max(0, n), Math.max(0, hits), Double.NaN, Double.NaN);
        double z = 1.959963984540054d, p = (double) hits / n;
        double den = 1.0d + z * z / n;
        double center = p + z * z / (2.0d * n);
        double radius = z * Math.sqrt((p * (1.0d - p) + z * z / (4.0d * n)) / n);
        return new WilsonInterval(n, hits, Math.max(0.0d, (center - radius) / den),
                Math.min(1.0d, (center + radius) / den));
    }

    public static final class StabilityEvidence {
        public final double retention;
        public final double thirdRobustness;
        public final double expertDropAgreement;
        public final double combined;
        public final int perturbationCount;
        public final String method;
        public final String inputHash;
        public final boolean supported;

        public StabilityEvidence(double retention, double thirdRobustness, double expertDropAgreement,
                                 int perturbationCount, String method, String inputHash) {
            this.retention = normalizeOrNaN(retention);
            this.thirdRobustness = normalizeOrNaN(thirdRobustness);
            this.expertDropAgreement = normalizeOrNaN(expertDropAgreement);
            this.perturbationCount = Math.max(0, perturbationCount);
            this.method = safe(method);
            this.inputHash = inputHash == null || inputHash.length() == 0 ? "" : requireHash(inputHash);
            this.supported = finite(this.retention) && finite(this.thirdRobustness)
                    && finite(this.expertDropAgreement) && this.perturbationCount > 0
                    && this.method.length() > 0 && this.inputHash.length() == 64;
            this.combined = supported
                    ? Math.min(this.retention, Math.min(this.thirdRobustness, this.expertDropAgreement))
                    : Double.NaN;
        }
    }

    public static final class Uncertainty {
        public final double modelDisagreement;
        public final double drift;
        public final int regimeSupportN;
        public final int calibrationSupportN;
        public final double nonconformity;
        public final String nonconformitySemantics = "REJECTION_FEATURE_NOT_PROBABILITY";
        public final String missingReason;

        public Uncertainty(double modelDisagreement, double drift, int regimeSupportN,
                           int calibrationSupportN, double nonconformity, String missingReason) {
            this.modelDisagreement = normalizeOrNaN(modelDisagreement);
            this.drift = normalizeOrNaN(drift);
            this.regimeSupportN = Math.max(0, regimeSupportN);
            this.calibrationSupportN = Math.max(0, calibrationSupportN);
            this.nonconformity = normalizeOrNaN(nonconformity);
            this.missingReason = safe(missingReason);
        }

        public boolean supported() {
            return finite(modelDisagreement) && finite(drift) && finite(nonconformity)
                    && calibrationSupportN > 0 && missingReason.length() == 0;
        }
    }

    public static String calibrationBinding(String calibrationArtifactId, String dataSha256,
                                            String selectionPolicyHash, String inputSchemaHash) {
        String id = safe(calibrationArtifactId), data = safe(dataSha256);
        if (id.length() == 0 || data.length() == 0)
            throw new IllegalArgumentException("R929_CALIBRATION_IDENTITY_EMPTY");
        return V51ChecksumEngine.sha256(REVISION + "|CAL|" + id + '|' + data + '|'
                + requireHash(selectionPolicyHash) + '|' + requireHash(inputSchemaHash));
    }

    public static void requireCalibrationCompatible(String artifactSelectionPolicyHash) {
        String expected = installedPolicy().hash;
        if (!expected.equals(safe(artifactSelectionPolicyHash)))
            throw new IllegalStateException("R929_CALIBRATION_SELECTION_POLICY_MISMATCH|expected="
                    + expected + "|actual=" + safe(artifactSelectionPolicyHash));
    }

    public static String summary(Probability p, WilsonInterval w, StabilityEvidence s, Uncertainty u) {
        StringBuilder b = new StringBuilder(256);
        b.append("revision=").append(REVISION);
        b.append("|p=").append(p != null && p.supported ? fmt(p.value) : "N/A");
        b.append("|calN=").append(p == null ? 0 : p.supportN);
        b.append("|binN=").append(p == null ? 0 : p.reliabilityBinN);
        b.append("|wilson=").append(w != null && finite(w.lower)
                ? fmt(w.lower) + ".." + fmt(w.upper) : "N/A");
        b.append("|stability=").append(s != null && s.supported ? fmt(s.combined) : "N/A");
        b.append("|uncertainty=").append(u != null && u.supported()
                ? fmt(Math.max(u.modelDisagreement, Math.max(u.drift, u.nonconformity))) : "N/A");
        return b.toString();
    }

    private static double normalizeOrNaN(double x) {
        if (!finite(x)) return Double.NaN;
        if (x < 0) return 0; if (x > 1) return 1; return x;
    }
    private static boolean finite(double x) { return !Double.isNaN(x) && !Double.isInfinite(x); }
    private static String safe(String x) { return x == null ? "" : x.trim(); }
    private static String requireHash(String x) {
        String v = safe(x).toLowerCase(Locale.US);
        if (!v.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("R929_HASH_REQUIRED");
        return v;
    }
    private static String fmt(double x) { return String.format(Locale.US, "%.6f", x); }
}
