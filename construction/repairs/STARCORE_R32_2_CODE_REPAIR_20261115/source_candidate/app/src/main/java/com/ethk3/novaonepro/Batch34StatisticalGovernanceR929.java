package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * C01/C02/C03/C06 statistical governance for Batch-3/4.
 *
 * <p>All windows, hypotheses and domain definitions are immutable after their first observation.
 * A certificate can prove research eligibility, but it can never create a BET or bypass
 * DecisionGateEngine.</p>
 */
public final class Batch34StatisticalGovernanceR929 {
    public static final String VERSION = "R929_STATISTICAL_GOVERNANCE_V2";
    public static final double REQUIRED_HIT_RATE = 0.40d;
    public static final double MAX_ECE = 0.04d;
    public static final int MAX_MISS_STREAK = 8;
    public static final int MIN_FUTURE_SUPPORT = 100;

    private Batch34StatisticalGovernanceR929() {}

    public static final class Metric {
        public final int n;
        public final double rate;
        public final double wilsonLower;
        public final double brier;
        public final double ece;
        public final int longestMiss;
        public final String denominator;

        public Metric(
                int n,
                double rate,
                double wilsonLower,
                double brier,
                double ece,
                int longestMiss,
                String denominator) {
            this.n = n;
            this.rate = rate;
            this.wilsonLower = wilsonLower;
            this.brier = brier;
            this.ece = ece;
            this.longestMiss = longestMiss;
            this.denominator = safe(denominator);
        }

        public String canonical() {
            return n + "|" + finite(rate) + "|" + finite(wilsonLower) + "|" + finite(brier)
                    + "|" + finite(ece) + "|" + longestMiss + "|" + denominator;
        }
    }

    /** Immutable promotion certificate V2; it is evidence, never direct BET authority. */
    public static final class CertificateV2 {
        public final String windowId;
        public final String candidateId;
        public final String candidateSha256;
        public final String championId;
        public final String championSha256;
        public final String datasetSha256;
        public final String featureManifestSha256;
        public final String artifactBundleSha256;
        public final String policySha256;
        public final String domainCertificateSha256;
        public final String trainEndIssue;
        public final String futureStartIssue;
        public final String futureEndIssue;
        public final Metric candidate;
        public final Metric pairedChampion;
        public final double pairedDelta;
        public final double pairedLower;
        public final double pairedUpper;
        public final double alphaSpent;
        public final String hypothesisId;
        public final boolean pitPassed;
        public final boolean oosPassed;
        public final boolean baselineFrozen;
        public final boolean resourcePassed;
        public final boolean parityPassed;
        public final boolean nonRegressionPassed;
        public final boolean exclusionLedgerComplete;
        public final boolean stopRuleHonored;
        public final boolean directBetAuthority;
        public final List<String> exclusions;
        public final String certificateSha256;

        public CertificateV2(
                String windowId,
                String candidateId,
                String candidateSha256,
                String championId,
                String championSha256,
                String datasetSha256,
                String featureManifestSha256,
                String artifactBundleSha256,
                String policySha256,
                String domainCertificateSha256,
                String trainEndIssue,
                String futureStartIssue,
                String futureEndIssue,
                Metric candidate,
                Metric pairedChampion,
                double pairedDelta,
                double pairedLower,
                double pairedUpper,
                double alphaSpent,
                String hypothesisId,
                boolean pitPassed,
                boolean oosPassed,
                boolean baselineFrozen,
                boolean resourcePassed,
                boolean parityPassed,
                boolean nonRegressionPassed,
                boolean exclusionLedgerComplete,
                boolean stopRuleHonored,
                boolean directBetAuthority,
                List<String> exclusions,
                String certificateSha256) {
            this.windowId = safe(windowId);
            this.candidateId = safe(candidateId);
            this.candidateSha256 = safe(candidateSha256);
            this.championId = safe(championId);
            this.championSha256 = safe(championSha256);
            this.datasetSha256 = safe(datasetSha256);
            this.featureManifestSha256 = safe(featureManifestSha256);
            this.artifactBundleSha256 = safe(artifactBundleSha256);
            this.policySha256 = safe(policySha256);
            this.domainCertificateSha256 = safe(domainCertificateSha256);
            this.trainEndIssue = safe(trainEndIssue);
            this.futureStartIssue = safe(futureStartIssue);
            this.futureEndIssue = safe(futureEndIssue);
            this.candidate = candidate;
            this.pairedChampion = pairedChampion;
            this.pairedDelta = pairedDelta;
            this.pairedLower = pairedLower;
            this.pairedUpper = pairedUpper;
            this.alphaSpent = alphaSpent;
            this.hypothesisId = safe(hypothesisId);
            this.pitPassed = pitPassed;
            this.oosPassed = oosPassed;
            this.baselineFrozen = baselineFrozen;
            this.resourcePassed = resourcePassed;
            this.parityPassed = parityPassed;
            this.nonRegressionPassed = nonRegressionPassed;
            this.exclusionLedgerComplete = exclusionLedgerComplete;
            this.stopRuleHonored = stopRuleHonored;
            this.directBetAuthority = directBetAuthority;
            ArrayList<String> copy = new ArrayList<String>();
            if (exclusions != null) copy.addAll(exclusions);
            this.exclusions = Collections.unmodifiableList(copy);
            this.certificateSha256 = safe(certificateSha256);
        }

        public String canonicalWithoutHash() {
            return windowId + "|" + candidateId + "|" + candidateSha256 + "|" + championId + "|"
                    + championSha256 + "|" + datasetSha256 + "|" + featureManifestSha256 + "|"
                    + artifactBundleSha256 + "|" + policySha256 + "|" + domainCertificateSha256
                    + "|" + trainEndIssue + "|" + futureStartIssue + "|" + futureEndIssue + "|"
                    + (candidate == null ? "NULL" : candidate.canonical()) + "|"
                    + (pairedChampion == null ? "NULL" : pairedChampion.canonical()) + "|"
                    + finite(pairedDelta) + "|" + finite(pairedLower) + "|" + finite(pairedUpper)
                    + "|" + finite(alphaSpent) + "|" + hypothesisId + "|" + pitPassed + "|"
                    + oosPassed + "|" + baselineFrozen + "|" + resourcePassed + "|" + parityPassed
                    + "|" + nonRegressionPassed + "|" + exclusionLedgerComplete + "|"
                    + stopRuleHonored + "|" + directBetAuthority + "|"
                    + Batch34EvidenceContractR929.join(exclusions, ",");
        }

        public String calculatedSha256() {
            return Batch34EvidenceContractR929.sha256(
                    "R929_CERTIFICATE_V2|" + canonicalWithoutHash());
        }
    }

    public static Batch34EvidenceContractR929.Check validateCertificate(
            CertificateV2 c, boolean production) {
        ArrayList<String> f = new ArrayList<String>();
        if (c == null) {
            f.add("C01_CERTIFICATE_NULL");
            return check(f, "");
        }
        if (c.windowId.length() == 0) f.add("C01_WINDOW_ID_EMPTY");
        if (c.candidateId.length() == 0) f.add("C01_CANDIDATE_ID_EMPTY");
        if (c.championId.length() == 0) f.add("C01_CHAMPION_ID_EMPTY");
        validateHash(f, "CANDIDATE", c.candidateSha256);
        validateHash(f, "CHAMPION", c.championSha256);
        validateHash(f, "DATASET", c.datasetSha256);
        validateHash(f, "FEATURE_MANIFEST", c.featureManifestSha256);
        validateHash(f, "ARTIFACT_BUNDLE", c.artifactBundleSha256);
        validateHash(f, "POLICY", c.policySha256);
        validateHash(f, "DOMAIN_CERTIFICATE", c.domainCertificateSha256);
        validateHash(f, "CERTIFICATE", c.certificateSha256);
        if (!c.calculatedSha256().equalsIgnoreCase(c.certificateSha256)) {
            f.add("C01_CERTIFICATE_HASH_MISMATCH");
        }
        if (!validRange(c.trainEndIssue, c.futureStartIssue, c.futureEndIssue)) {
            f.add("C01_TEMPORAL_WINDOW_INVALID");
        }
        if (c.candidate == null) {
            f.add("C01_CANDIDATE_METRIC_MISSING");
        } else {
            if (c.candidate.n < (production ? MIN_FUTURE_SUPPORT : 1)) {
                f.add("C01_FUTURE_SUPPORT_INSUFFICIENT");
            }
            if (!"FUTURE_SELECTED".equals(c.candidate.denominator)) {
                f.add("C01_CANDIDATE_DENOMINATOR_NOT_FUTURE_SELECTED");
            }
            if (production && c.candidate.rate < REQUIRED_HIT_RATE) {
                f.add("C01_HIT_RATE_LT_40_PERCENT");
            }
            if (production && c.candidate.wilsonLower < 0.37d) {
                f.add("C01_WILSON_LOWER_LT_37_PERCENT");
            }
            if (c.candidate.ece > MAX_ECE) f.add("C01_ECE_GT_4_PERCENT");
            if (c.candidate.longestMiss > MAX_MISS_STREAK) f.add("C01_LONGEST_MISS_GT_8");
            if (!probability(c.candidate.rate)
                    || !probability(c.candidate.wilsonLower)
                    || !nonNegativeFinite(c.candidate.brier)
                    || !nonNegativeFinite(c.candidate.ece)) {
                f.add("C01_CANDIDATE_METRIC_NONFINITE");
            }
        }
        if (c.pairedChampion == null) {
            f.add("C01_PAIRED_BASELINE_METRIC_MISSING");
        } else if (!"FUTURE_PAIRED_CHAMPION".equals(c.pairedChampion.denominator)) {
            f.add("C01_BASELINE_DENOMINATOR_NOT_PAIRED");
        }
        if (!Double.isFinite(c.pairedDelta)
                || !Double.isFinite(c.pairedLower)
                || !Double.isFinite(c.pairedUpper)
                || c.pairedLower > c.pairedUpper) {
            f.add("C01_PAIRED_INTERVAL_INVALID");
        }
        if (production && c.pairedLower <= 0.0d) f.add("C01_PAIRED_GAIN_NOT_PROVEN");
        if (!nonNegativeFinite(c.alphaSpent) || c.alphaSpent > 0.25d) {
            f.add("C03_ALPHA_SPEND_INVALID");
        }
        if (c.hypothesisId.length() == 0) f.add("C03_HYPOTHESIS_ID_EMPTY");
        if (!c.pitPassed) f.add("C01_PIT_NOT_PASSED");
        if (!c.oosPassed) f.add("C01_OOS_NOT_PASSED");
        if (!c.baselineFrozen) f.add("C02_BASELINE_NOT_FROZEN");
        if (!c.resourcePassed) f.add("C01_RESOURCE_NOT_PASSED");
        if (!c.parityPassed) f.add("C01_PARITY_NOT_PASSED");
        if (!c.nonRegressionPassed) f.add("C01_NON_REGRESSION_NOT_PASSED");
        if (!c.exclusionLedgerComplete) f.add("C01_EXCLUSION_LEDGER_INCOMPLETE");
        if (!c.stopRuleHonored) f.add("C03_STOP_RULE_NOT_HONORED");
        if (c.directBetAuthority) f.add("C01_CERTIFICATE_MUST_NOT_AUTHORIZE_BET");
        return check(f, c.canonicalWithoutHash());
    }

    public enum WindowState {
        DECLARED,
        OBSERVING,
        FINALIZED_PASS,
        FINALIZED_FAIL,
        CANCELLED_BEFORE_OBSERVATION
    }

    public static final class PairedWindow {
        public final String windowId;
        public final String candidateSha256;
        public final String championSha256;
        public final String policySha256;
        public final String firstIssue;
        public final String lastIssue;
        public final int plannedIssues;
        public final long declaredAtMillis;
        private WindowState state;
        private int observations;
        private String terminalDecisionSha256;

        public PairedWindow(
                String windowId,
                String candidateSha256,
                String championSha256,
                String policySha256,
                String firstIssue,
                String lastIssue,
                int plannedIssues,
                long declaredAtMillis) {
            this.windowId = safe(windowId);
            this.candidateSha256 = safe(candidateSha256);
            this.championSha256 = safe(championSha256);
            this.policySha256 = safe(policySha256);
            this.firstIssue = safe(firstIssue);
            this.lastIssue = safe(lastIssue);
            this.plannedIssues = plannedIssues;
            this.declaredAtMillis = declaredAtMillis;
            this.state = WindowState.DECLARED;
            this.terminalDecisionSha256 = "";
        }

        public synchronized String begin() {
            if (state != WindowState.DECLARED) return "C02_WINDOW_BEGIN_REJECTED_" + state;
            state = WindowState.OBSERVING;
            return "C02_WINDOW_OBSERVING";
        }

        public synchronized String observe(String issue) {
            if (state != WindowState.OBSERVING) return "C02_OBSERVE_REJECTED_" + state;
            if (!Batch34EvidenceContractR929.validIssue(issue)
                    || Batch34EvidenceContractR929.compareIssue(issue, firstIssue) < 0
                    || Batch34EvidenceContractR929.compareIssue(issue, lastIssue) > 0) {
                return "C02_ISSUE_OUTSIDE_FROZEN_WINDOW";
            }
            observations++;
            return "C02_OBSERVATION_ACCEPTED";
        }

        public synchronized String finalizeOnce(boolean passed, String decisionSha256) {
            if (state != WindowState.OBSERVING) return "C02_FINALIZE_REJECTED_" + state;
            if (observations != plannedIssues) return "C02_WINDOW_NOT_COMPLETE";
            if (!Batch34EvidenceContractR929.isSha256(decisionSha256)) {
                return "C02_DECISION_HASH_INVALID";
            }
            terminalDecisionSha256 = decisionSha256;
            state = passed ? WindowState.FINALIZED_PASS : WindowState.FINALIZED_FAIL;
            return "C02_FINALIZED_ONCE";
        }

        public synchronized String cancelBeforeObservation() {
            if (state != WindowState.DECLARED || observations != 0) return "C02_CANCEL_REJECTED";
            state = WindowState.CANCELLED_BEFORE_OBSERVATION;
            return "C02_CANCELLED_BEFORE_OBSERVATION";
        }

        public synchronized WindowState state() { return state; }
        public synchronized int observations() { return observations; }
        public synchronized String terminalDecisionSha256() { return terminalDecisionSha256; }

        public String frozenIdentity() {
            return Batch34EvidenceContractR929.sha256(
                    "C02_WINDOW|" + windowId + "|" + candidateSha256 + "|" + championSha256 + "|"
                            + policySha256 + "|" + firstIssue + "|" + lastIssue + "|"
                            + plannedIssues + "|" + declaredAtMillis);
        }
    }

    public static final class WindowRegistry {
        private final Map<String, PairedWindow> byId = new LinkedHashMap<String, PairedWindow>();
        private final Set<String> consumedCandidateWindow = new HashSet<String>();

        public synchronized String declare(PairedWindow window) {
            if (window == null) return "C02_WINDOW_NULL";
            if (window.windowId.length() == 0 || byId.containsKey(window.windowId)) {
                return "C02_WINDOW_ID_DUPLICATE_OR_EMPTY";
            }
            if (!Batch34EvidenceContractR929.isSha256(window.candidateSha256)
                    || !Batch34EvidenceContractR929.isSha256(window.championSha256)
                    || !Batch34EvidenceContractR929.isSha256(window.policySha256)) {
                return "C02_WINDOW_COMPONENT_HASH_INVALID";
            }
            if (!validRange("", window.firstIssue, window.lastIssue) || window.plannedIssues <= 0) {
                return "C02_WINDOW_BOUNDARY_INVALID";
            }
            String key = window.candidateSha256 + "|" + window.firstIssue + "|" + window.lastIssue;
            if (consumedCandidateWindow.contains(key)) return "C02_REPEATED_TRIAL_REJECTED";
            byId.put(window.windowId, window);
            consumedCandidateWindow.add(key);
            return "C02_WINDOW_DECLARED";
        }

        public synchronized PairedWindow get(String id) { return byId.get(safe(id)); }
        public synchronized int size() { return byId.size(); }
    }

    public enum HypothesisState {
        RESERVED,
        OBSERVING,
        REJECTED_NULL,
        FAILED_TO_REJECT,
        CANCELLED_BEFORE_OBSERVATION
    }

    public static final class Hypothesis {
        public final String id;
        public final String family;
        public final int globalIndex;
        public final double alpha;
        public final String windowId;
        private HypothesisState state;
        private double pValue;

        Hypothesis(String id, String family, int globalIndex, double alpha, String windowId) {
            this.id = id;
            this.family = family;
            this.globalIndex = globalIndex;
            this.alpha = alpha;
            this.windowId = windowId;
            this.state = HypothesisState.RESERVED;
            this.pValue = Double.NaN;
        }

        public synchronized String beginObservation() {
            if (state != HypothesisState.RESERVED) return "C03_BEGIN_REJECTED_" + state;
            state = HypothesisState.OBSERVING;
            return "C03_OBSERVING";
        }

        public synchronized String decide(double p) {
            if (state != HypothesisState.OBSERVING) return "C03_DECISION_REJECTED_" + state;
            if (!probability(p)) return "C03_P_VALUE_INVALID";
            pValue = p;
            state = p <= alpha ? HypothesisState.REJECTED_NULL : HypothesisState.FAILED_TO_REJECT;
            return state.name();
        }

        public synchronized String cancelBeforeObservation() {
            if (state != HypothesisState.RESERVED) return "C03_CANCEL_REJECTED_" + state;
            state = HypothesisState.CANCELLED_BEFORE_OBSERVATION;
            return state.name();
        }

        public synchronized HypothesisState state() { return state; }
        public synchronized double pValue() { return pValue; }
    }

    /** Reservations debit alpha immediately; only pre-observation cancellation can refund it. */
    public static final class AlphaLedger {
        private final double initialAlpha;
        private double availableAlpha;
        private int globalIndex;
        private final Map<String, Hypothesis> hypotheses =
                new LinkedHashMap<String, Hypothesis>();

        public AlphaLedger(double initialAlpha) {
            if (!(initialAlpha > 0.0d && initialAlpha <= 0.25d)) {
                throw new IllegalArgumentException("C03_INITIAL_ALPHA_INVALID");
            }
            this.initialAlpha = initialAlpha;
            this.availableAlpha = initialAlpha;
        }

        public synchronized Hypothesis reserve(
                String id, String family, String windowId, double requestedAlpha) {
            String key = safe(id);
            if (key.length() == 0 || hypotheses.containsKey(key)) return null;
            if (!(requestedAlpha > 0.0d) || !Double.isFinite(requestedAlpha)) return null;
            double scheduleCap = lordAlpha(initialAlpha, globalIndex + 1);
            double debit = Math.min(requestedAlpha, Math.min(scheduleCap, availableAlpha));
            if (!(debit > 0.0d)) return null;
            globalIndex++;
            availableAlpha -= debit;
            Hypothesis h = new Hypothesis(
                    key,
                    safe(family).toUpperCase(Locale.US),
                    globalIndex,
                    debit,
                    safe(windowId));
            hypotheses.put(key, h);
            return h;
        }

        public synchronized String cancelAndRefund(String id) {
            Hypothesis h = hypotheses.get(safe(id));
            if (h == null) return "C03_HYPOTHESIS_NOT_FOUND";
            String result = h.cancelBeforeObservation();
            if ("CANCELLED_BEFORE_OBSERVATION".equals(result)) {
                availableAlpha = Math.min(initialAlpha, availableAlpha + h.alpha);
                return "C03_REFUNDED_BEFORE_OBSERVATION";
            }
            return result;
        }

        public synchronized double availableAlpha() { return availableAlpha; }
        public synchronized int hypothesisCount() { return hypotheses.size(); }

        public static double lordAlpha(double familyAlpha, int oneBasedIndex) {
            if (oneBasedIndex <= 0 || !(familyAlpha > 0.0d)) return 0.0d;
            return familyAlpha / ((double) oneBasedIndex * (oneBasedIndex + 1.0d));
        }
    }

    /** Benjamini-Hochberg decisions inside one immutable statistical family. */
    public static boolean[] benjaminiHochberg(final double[] pValues, double q) {
        if (pValues == null) return new boolean[0];
        int n = pValues.length;
        boolean[] selected = new boolean[n];
        if (n == 0 || !(q > 0.0d && q <= 0.25d)) return selected;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            if (!probability(pValues[i])) return selected;
            order[i] = Integer.valueOf(i);
        }
        java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return Double.compare(pValues[a.intValue()], pValues[b.intValue()]);
            }
        });
        int k = -1;
        for (int rank = 1; rank <= n; rank++) {
            if (pValues[order[rank - 1].intValue()] <= q * rank / n) k = rank;
        }
        for (int rank = 0; rank < k; rank++) selected[order[rank].intValue()] = true;
        return selected;
    }

    public static final class DomainComponent {
        public final String domainId;
        public final double posterior;
        public final int trainSupport;
        public final int calibrationSupport;
        public final int futureSupport;
        public final String definitionSha256;
        public final String calibrationSha256;
        public final boolean futureCertificatePassed;

        public DomainComponent(
                String domainId,
                double posterior,
                int trainSupport,
                int calibrationSupport,
                int futureSupport,
                String definitionSha256,
                String calibrationSha256,
                boolean futureCertificatePassed) {
            this.domainId = safe(domainId);
            this.posterior = posterior;
            this.trainSupport = trainSupport;
            this.calibrationSupport = calibrationSupport;
            this.futureSupport = futureSupport;
            this.definitionSha256 = safe(definitionSha256);
            this.calibrationSha256 = safe(calibrationSha256);
            this.futureCertificatePassed = futureCertificatePassed;
        }

        public String canonical() {
            return domainId + "|" + finite(posterior) + "|" + trainSupport + "|"
                    + calibrationSupport + "|" + futureSupport + "|" + definitionSha256 + "|"
                    + calibrationSha256 + "|" + futureCertificatePassed;
        }
    }

    public static final class DomainCertificate {
        public final String domainModelSha256;
        public final String globalCalibrationSha256;
        public final String frozenAtIssue;
        public final List<DomainComponent> components;
        public final boolean trainOnlyClustering;
        public final boolean globalShrinkageEnabled;
        public final String certificateSha256;

        public DomainCertificate(
                String domainModelSha256,
                String globalCalibrationSha256,
                String frozenAtIssue,
                List<DomainComponent> components,
                boolean trainOnlyClustering,
                boolean globalShrinkageEnabled,
                String certificateSha256) {
            this.domainModelSha256 = safe(domainModelSha256);
            this.globalCalibrationSha256 = safe(globalCalibrationSha256);
            this.frozenAtIssue = safe(frozenAtIssue);
            ArrayList<DomainComponent> copy = new ArrayList<DomainComponent>();
            if (components != null) copy.addAll(components);
            this.components = Collections.unmodifiableList(copy);
            this.trainOnlyClustering = trainOnlyClustering;
            this.globalShrinkageEnabled = globalShrinkageEnabled;
            this.certificateSha256 = safe(certificateSha256);
        }

        public String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder();
            out.append(domainModelSha256).append('|').append(globalCalibrationSha256).append('|')
                    .append(frozenAtIssue).append('|').append(trainOnlyClustering).append('|')
                    .append(globalShrinkageEnabled);
            for (DomainComponent component : components) {
                out.append("\n").append(component == null ? "NULL" : component.canonical());
            }
            return out.toString();
        }

        public String calculatedSha256() {
            return Batch34EvidenceContractR929.sha256(
                    "C06_DOMAIN_CERTIFICATE|" + canonicalWithoutHash());
        }
    }

    public static Batch34EvidenceContractR929.Check validateDomainCertificate(
            DomainCertificate certificate, boolean production) {
        ArrayList<String> f = new ArrayList<String>();
        if (certificate == null) {
            f.add("C06_DOMAIN_CERTIFICATE_NULL");
            return check(f, "");
        }
        validateHash(f, "DOMAIN_MODEL", certificate.domainModelSha256);
        validateHash(f, "GLOBAL_CALIBRATION", certificate.globalCalibrationSha256);
        validateHash(f, "DOMAIN_CERTIFICATE", certificate.certificateSha256);
        if (!certificate.calculatedSha256().equalsIgnoreCase(certificate.certificateSha256)) {
            f.add("C06_DOMAIN_CERTIFICATE_HASH_MISMATCH");
        }
        if (!Batch34EvidenceContractR929.validIssue(certificate.frozenAtIssue)) {
            f.add("C06_FROZEN_ISSUE_INVALID");
        }
        if (!certificate.trainOnlyClustering) f.add("C06_CLUSTERING_NOT_TRAIN_ONLY");
        if (!certificate.globalShrinkageEnabled) f.add("C06_GLOBAL_SHRINKAGE_DISABLED");
        if (certificate.components.isEmpty()) f.add("C06_DOMAIN_COMPONENTS_EMPTY");
        double sum = 0.0d;
        Set<String> ids = new HashSet<String>();
        for (DomainComponent component : certificate.components) {
            if (component == null) {
                f.add("C06_NULL_DOMAIN");
                continue;
            }
            if (component.domainId.length() == 0 || !ids.add(component.domainId)) {
                f.add("C06_DUPLICATE_OR_EMPTY_DOMAIN");
            }
            if (!probability(component.posterior)) f.add("C06_POSTERIOR_INVALID:" + component.domainId);
            sum += component.posterior;
            if (component.trainSupport <= 0 || component.calibrationSupport <= 0) {
                f.add("C06_DOMAIN_SUPPORT_MISSING:" + component.domainId);
            }
            if (production && (component.futureSupport < 30 || !component.futureCertificatePassed)) {
                f.add("C06_DOMAIN_FUTURE_CERTIFICATE_MISSING:" + component.domainId);
            }
            validateHash(f, "DOMAIN_DEFINITION_" + component.domainId, component.definitionSha256);
            validateHash(f, "DOMAIN_CALIBRATION_" + component.domainId, component.calibrationSha256);
        }
        if (Math.abs(sum - 1.0d) > 1e-9d) f.add("C06_POSTERIOR_SUM_NOT_ONE");
        return check(f, certificate.canonicalWithoutHash());
    }

    private static Batch34EvidenceContractR929.Check check(List<String> failures, String canonical) {
        return new Batch34EvidenceContractR929.Check(failures, VERSION + "|" + canonical);
    }

    private static void validateHash(List<String> failures, String name, String value) {
        if (!Batch34EvidenceContractR929.isSha256(value)) failures.add("C01_" + name + "_HASH_INVALID");
    }

    private static boolean validRange(String trainEnd, String start, String end) {
        if (!Batch34EvidenceContractR929.validIssue(start)
                || !Batch34EvidenceContractR929.validIssue(end)
                || Batch34EvidenceContractR929.compareIssue(start, end) > 0) return false;
        return trainEnd.length() == 0
                || (Batch34EvidenceContractR929.validIssue(trainEnd)
                        && Batch34EvidenceContractR929.compareIssue(trainEnd, start) < 0);
    }

    private static boolean probability(double value) {
        return Double.isFinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static boolean nonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0.0d;
    }

    private static String finite(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.12g", value) : "NONFINITE";
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
