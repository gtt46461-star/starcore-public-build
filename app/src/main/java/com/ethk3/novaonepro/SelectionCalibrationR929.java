package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Selection-bound probability calibration, uncertainty decomposition and joint quality evidence.
 * Raw ranking scores are never accepted as hit probabilities.
 */
public final class SelectionCalibrationR929 {
    public static final String VERSION = "R929_SELECTION_CALIBRATION_V1";
    public static final double TARGET_HIT_RATE = 0.40d;
    public static final double TARGET_COVERAGE = 0.15d;
    public static final double TARGET_ECE = 0.04d;
    public static final int DEFAULT_MIN_FUTURE = 120;

    private SelectionCalibrationR929() {}

    public enum ProbabilityKind {
        RAW_SCORE,
        RAW_ACTION_PROBABILITY,
        SELECTED_HIT_PROBABILITY,
        UNKNOWN
    }

    public enum CalibrationKind { LOGISTIC, BETA, ISOTONIC, DOMAIN_SHRINKAGE }
    public enum SupportState { SUPPORTED, LOW_SUPPORT, POLICY_MISMATCH, DOMAIN_UNSUPPORTED, INVALID }

    public static final class SelectionPolicy {
        public final String rankerHash;
        public final String consumerId;
        public final double consumerWeight;
        public final String regimeSchemaHash;
        public final String tieBreakContract;
        public final String candidateFamily;
        public final String actionSpaceHash;
        public final String policyHash;

        public SelectionPolicy(String rankerHash, String consumerId, double consumerWeight,
                String regimeSchemaHash, String tieBreakContract, String candidateFamily,
                String actionSpaceHash) {
            this.rankerHash = requireHash(rankerHash, "RANKER_HASH");
            this.consumerId = required(consumerId, "CONSUMER_ID");
            if (!finite(consumerWeight) || consumerWeight < 0.0d || consumerWeight > 0.30d)
                throw new IllegalArgumentException("R929_CONSUMER_WEIGHT_INVALID");
            this.consumerWeight = consumerWeight;
            this.regimeSchemaHash = requireHash(regimeSchemaHash, "REGIME_SCHEMA_HASH");
            this.tieBreakContract = required(tieBreakContract, "TIE_BREAK");
            this.candidateFamily = required(candidateFamily, "CANDIDATE_FAMILY");
            this.actionSpaceHash = requireHash(actionSpaceHash, "ACTION_SPACE_HASH");
            this.policyHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return rankerHash + '|' + consumerId + '|'
                    + Long.toHexString(Double.doubleToLongBits(consumerWeight)) + '|'
                    + regimeSchemaHash + '|' + tieBreakContract + '|' + candidateFamily + '|'
                    + actionSpaceHash;
        }
    }

    public static final class ScoreEvidence {
        public final double rawScore;
        public final double rawActionProbability;
        public final ProbabilityKind probabilityKind;
        public final String sourceHash;

        public ScoreEvidence(double rawScore, double rawActionProbability,
                ProbabilityKind probabilityKind, String sourceHash) {
            if (!finite(rawScore) || probabilityKind == null)
                throw new IllegalArgumentException("R929_SCORE_EVIDENCE_INVALID");
            if (probabilityKind == ProbabilityKind.RAW_ACTION_PROBABILITY
                    && !probability(rawActionProbability))
                throw new IllegalArgumentException("R929_RAW_ACTION_PROBABILITY_INVALID");
            this.rawScore = rawScore;
            this.rawActionProbability = rawActionProbability;
            this.probabilityKind = probabilityKind;
            this.sourceHash = requireHash(sourceHash, "SCORE_SOURCE_HASH");
        }
    }

    public static final class CalibrationBin {
        public final double lowerInclusive;
        public final double upperInclusive;
        public final int support;
        public final int hits;
        public final double empiricalRate;
        public final double wilsonLower;
        public final double wilsonUpper;
        public final String binHash;

        public CalibrationBin(double lowerInclusive, double upperInclusive, int support, int hits) {
            if (!probability(lowerInclusive) || !probability(upperInclusive)
                    || lowerInclusive > upperInclusive || support < 0 || hits < 0 || hits > support)
                throw new IllegalArgumentException("R929_CALIBRATION_BIN_INVALID");
            this.lowerInclusive = lowerInclusive;
            this.upperInclusive = upperInclusive;
            this.support = support;
            this.hits = hits;
            this.empiricalRate = support == 0 ? Double.NaN : hits / (double) support;
            double[] interval = wilson(hits, support, 1.959963984540054d);
            this.wilsonLower = interval[0];
            this.wilsonUpper = interval[1];
            this.binHash = sha256(lowerInclusive + "|" + upperInclusive + "|" + support + "|" + hits);
        }

        public boolean contains(double p, boolean finalBin) {
            return p >= lowerInclusive && (p < upperInclusive || (finalBin && p <= upperInclusive));
        }
    }

    /** Immutable calibration artifact tied to one final selection policy. */
    public static final class CalibrationArtifact {
        public final CalibrationKind kind;
        public final String selectionPolicyHash;
        public final String inputSchemaHash;
        public final String parentModelHash;
        public final String domain;
        public final String domainSchemaHash;
        public final String trainedUntilIssue;
        public final String calibrationRangeHash;
        public final int support;
        private final double[] parameters;
        private final double[] knotX;
        private final double[] knotY;
        public final List<CalibrationBin> bins;
        public final String artifactHash;

        public CalibrationArtifact(CalibrationKind kind, String selectionPolicyHash,
                String inputSchemaHash, String parentModelHash, String domain,
                String domainSchemaHash, String trainedUntilIssue, String calibrationRangeHash,
                int support, double[] parameters, double[] knotX, double[] knotY,
                List<CalibrationBin> bins) {
            if (kind == null || support < 0)
                throw new IllegalArgumentException("R929_CALIBRATION_ARTIFACT_INVALID");
            this.kind = kind;
            this.selectionPolicyHash = requireHash(selectionPolicyHash, "SELECTION_POLICY_HASH");
            this.inputSchemaHash = requireHash(inputSchemaHash, "CAL_INPUT_SCHEMA_HASH");
            this.parentModelHash = requireHash(parentModelHash, "CAL_PARENT_MODEL_HASH");
            this.domain = required(domain, "CAL_DOMAIN");
            this.domainSchemaHash = requireHash(domainSchemaHash, "CAL_DOMAIN_SCHEMA_HASH");
            this.trainedUntilIssue = required(trainedUntilIssue, "CAL_TRAINED_UNTIL");
            this.calibrationRangeHash = requireHash(calibrationRangeHash, "CAL_RANGE_HASH");
            this.support = support;
            this.parameters = parameters == null ? new double[0] : Arrays.copyOf(parameters, parameters.length);
            this.knotX = knotX == null ? new double[0] : Arrays.copyOf(knotX, knotX.length);
            this.knotY = knotY == null ? new double[0] : Arrays.copyOf(knotY, knotY.length);
            ArrayList<CalibrationBin> binCopy = new ArrayList<CalibrationBin>();
            if (bins != null) binCopy.addAll(bins);
            validateDefinition(kind, this.parameters, this.knotX, this.knotY, binCopy);
            this.bins = Collections.unmodifiableList(binCopy);
            this.artifactHash = sha256(canonicalWithoutHash());
        }

        private static void validateDefinition(CalibrationKind kind, double[] parameters,
                double[] knotX, double[] knotY, List<CalibrationBin> bins) {
            for (double value : parameters) if (!finite(value))
                throw new IllegalArgumentException("R929_CALIBRATION_PARAMETER_NONFINITE");
            if (kind == CalibrationKind.LOGISTIC && parameters.length != 2)
                throw new IllegalArgumentException("R929_LOGISTIC_PARAMETER_COUNT");
            if (kind == CalibrationKind.BETA && parameters.length != 3)
                throw new IllegalArgumentException("R929_BETA_PARAMETER_COUNT");
            if (kind == CalibrationKind.DOMAIN_SHRINKAGE && parameters.length != 3)
                throw new IllegalArgumentException("R929_SHRINKAGE_PARAMETER_COUNT");
            if (kind == CalibrationKind.ISOTONIC) {
                if (knotX.length < 2 || knotX.length != knotY.length)
                    throw new IllegalArgumentException("R929_ISOTONIC_KNOT_COUNT");
                for (int i = 0; i < knotX.length; i++) {
                    if (!finite(knotX[i]) || !probability(knotY[i])
                            || (i > 0 && (knotX[i] <= knotX[i - 1] || knotY[i] < knotY[i - 1])))
                        throw new IllegalArgumentException("R929_ISOTONIC_MONOTONICITY");
                }
            }
            double previousUpper = -1.0d;
            for (int i = 0; i < bins.size(); i++) {
                CalibrationBin bin = bins.get(i);
                if (bin == null || (i > 0 && Math.abs(bin.lowerInclusive - previousUpper) > 1.0e-12d))
                    throw new IllegalArgumentException("R929_CALIBRATION_BIN_GAP");
                previousUpper = bin.upperInclusive;
            }
        }

        public double[] parametersCopy() { return Arrays.copyOf(parameters, parameters.length); }
        public double[] knotXCopy() { return Arrays.copyOf(knotX, knotX.length); }
        public double[] knotYCopy() { return Arrays.copyOf(knotY, knotY.length); }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(512);
            out.append(kind.name()).append('|').append(selectionPolicyHash).append('|')
                    .append(inputSchemaHash).append('|').append(parentModelHash).append('|')
                    .append(domain).append('|').append(domainSchemaHash).append('|')
                    .append(trainedUntilIssue).append('|').append(calibrationRangeHash).append('|')
                    .append(support).append('|');
            appendDoubles(out, parameters);
            out.append('|'); appendDoubles(out, knotX);
            out.append('|'); appendDoubles(out, knotY);
            for (CalibrationBin bin : bins) out.append('|').append(bin.binHash);
            return out.toString();
        }
    }

    public static final class CalibrationResult {
        public final double selectedHitProbability;
        public final ProbabilityKind kind;
        public final SupportState supportState;
        public final String reason;
        public final String calibrationHash;
        public final CalibrationBin reliabilityBin;
        public final String resultHash;

        CalibrationResult(double probability, ProbabilityKind kind, SupportState state,
                String reason, String calibrationHash, CalibrationBin reliabilityBin) {
            this.selectedHitProbability = probability;
            this.kind = kind;
            this.supportState = state;
            this.reason = reason;
            this.calibrationHash = calibrationHash;
            this.reliabilityBin = reliabilityBin;
            this.resultHash = sha256(probability + "|" + kind.name() + "|" + state.name()
                    + "|" + reason + "|" + calibrationHash + "|"
                    + (reliabilityBin == null ? "" : reliabilityBin.binHash));
        }

        public boolean usableProbability() {
            return kind == ProbabilityKind.SELECTED_HIT_PROBABILITY
                    && supportState == SupportState.SUPPORTED
                    && probability(selectedHitProbability);
        }
    }

    public static CalibrationResult calibrate(SelectionPolicy policy, ScoreEvidence score,
            CalibrationArtifact artifact, String targetIssue, String currentDomain,
            String currentDomainSchemaHash, int minimumSupport) {
        if (policy == null || score == null || artifact == null)
            return unavailable(SupportState.INVALID, "CALIBRATION_INPUT_NULL", artifact);
        if (!policy.policyHash.equals(artifact.selectionPolicyHash))
            return unavailable(SupportState.POLICY_MISMATCH,
                    "SELECTION_POLICY_HASH_MISMATCH", artifact);
        if (!policy.regimeSchemaHash.equals(artifact.domainSchemaHash)
                || !requireHash(currentDomainSchemaHash, "CURRENT_DOMAIN_SCHEMA_HASH")
                        .equals(artifact.domainSchemaHash))
            return unavailable(SupportState.DOMAIN_UNSUPPORTED, "DOMAIN_SCHEMA_MISMATCH", artifact);
        if (!artifact.domain.equals(required(currentDomain, "CURRENT_DOMAIN")))
            return unavailable(SupportState.DOMAIN_UNSUPPORTED, "DOMAIN_NOT_CERTIFIED", artifact);
        if (!PitFeatureDagR929.isStrictPast(artifact.trainedUntilIssue,
                required(targetIssue, "TARGET_ISSUE")))
            return unavailable(SupportState.INVALID, "CALIBRATION_NOT_PIT", artifact);
        if (artifact.support < Math.max(1, minimumSupport))
            return unavailable(SupportState.LOW_SUPPORT, "CALIBRATION_SUPPORT_LOW", artifact);
        double p;
        if (artifact.kind == CalibrationKind.LOGISTIC) {
            p = sigmoid(artifact.parameters[0] + artifact.parameters[1] * score.rawScore);
        } else if (artifact.kind == CalibrationKind.BETA) {
            if (!probability(score.rawActionProbability))
                return unavailable(SupportState.INVALID, "BETA_REQUIRES_RAW_ACTION_P", artifact);
            double base = clamp(score.rawActionProbability, 1.0e-9d, 1.0d - 1.0e-9d);
            p = sigmoid(artifact.parameters[0] + artifact.parameters[1] * Math.log(base)
                    + artifact.parameters[2] * Math.log(1.0d - base));
        } else if (artifact.kind == CalibrationKind.ISOTONIC) {
            p = interpolate(score.rawScore, artifact.knotX, artifact.knotY);
        } else {
            // parameters: local estimate, global estimate, prior strength.
            double local = clamp(artifact.parameters[0], 0.0d, 1.0d);
            double global = clamp(artifact.parameters[1], 0.0d, 1.0d);
            double prior = Math.max(0.0d, artifact.parameters[2]);
            p = (local * artifact.support + global * prior)
                    / Math.max(1.0d, artifact.support + prior);
        }
        if (!probability(p))
            return unavailable(SupportState.INVALID, "CALIBRATION_NONFINITE", artifact);
        CalibrationBin bin = findBin(artifact.bins, p);
        return new CalibrationResult(p, ProbabilityKind.SELECTED_HIT_PROBABILITY,
                SupportState.SUPPORTED, "CALIBRATED_FOR_EXACT_SELECTION", artifact.artifactHash, bin);
    }

    private static CalibrationResult unavailable(SupportState state, String reason,
            CalibrationArtifact artifact) {
        return new CalibrationResult(Double.NaN, ProbabilityKind.UNKNOWN, state, reason,
                artifact == null ? "" : artifact.artifactHash, null);
    }

    private static CalibrationBin findBin(List<CalibrationBin> bins, double p) {
        for (int i = 0; i < bins.size(); i++)
            if (bins.get(i).contains(p, i == bins.size() - 1)) return bins.get(i);
        return null;
    }

    public static final class Observation {
        public final String issue;
        public final boolean eligible;
        public final boolean selected;
        public final boolean real;
        public final boolean hit;
        public final double probability;
        public final String selectionPolicyHash;
        public final String calibrationHash;
        public final String domain;
        public final String inputHash;

        public Observation(String issue, boolean eligible, boolean selected, boolean real,
                boolean hit, double probability, String selectionPolicyHash,
                String calibrationHash, String domain, String inputHash) {
            this.issue = required(issue, "OBSERVATION_ISSUE");
            if (selected && !eligible) throw new IllegalArgumentException("R929_SELECTED_NOT_ELIGIBLE");
            if (real && !selected) throw new IllegalArgumentException("R929_REAL_NOT_SELECTED");
            if (selected && !probability(probability))
                throw new IllegalArgumentException("R929_SELECTED_PROBABILITY_INVALID");
            this.eligible = eligible;
            this.selected = selected;
            this.real = real;
            this.hit = hit;
            this.probability = probability;
            this.selectionPolicyHash = requireHash(selectionPolicyHash, "OBS_POLICY_HASH");
            this.calibrationHash = requireHash(calibrationHash, "OBS_CAL_HASH");
            this.domain = required(domain, "OBS_DOMAIN");
            this.inputHash = requireHash(inputHash, "OBS_INPUT_HASH");
        }
    }

    public static final class Metrics {
        public final int planned;
        public final int eligible;
        public final int selected;
        public final int real;
        public final int hits;
        public final double hitRate;
        public final double coverage;
        public final double realCoverage;
        public final double brier;
        public final double nll;
        public final double ece;
        public final double wilsonLower;
        public final double wilsonUpper;
        public final List<CalibrationBin> bins;
        public final String policyHash;
        public final String calibrationHash;
        public final String metricsHash;

        Metrics(int planned, int eligible, int selected, int real, int hits,
                double hitRate, double coverage, double realCoverage, double brier,
                double nll, double ece, double wilsonLower, double wilsonUpper,
                List<CalibrationBin> bins, String policyHash, String calibrationHash) {
            this.planned = planned;
            this.eligible = eligible;
            this.selected = selected;
            this.real = real;
            this.hits = hits;
            this.hitRate = hitRate;
            this.coverage = coverage;
            this.realCoverage = realCoverage;
            this.brier = brier;
            this.nll = nll;
            this.ece = ece;
            this.wilsonLower = wilsonLower;
            this.wilsonUpper = wilsonUpper;
            this.bins = bins;
            this.policyHash = policyHash;
            this.calibrationHash = calibrationHash;
            this.metricsHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(512);
            out.append(planned).append('|').append(eligible).append('|').append(selected)
                    .append('|').append(real).append('|').append(hits).append('|')
                    .append(hitRate).append('|').append(coverage).append('|')
                    .append(realCoverage).append('|').append(brier).append('|').append(nll)
                    .append('|').append(ece).append('|').append(wilsonLower).append('|')
                    .append(wilsonUpper).append('|').append(policyHash).append('|')
                    .append(calibrationHash);
            for (CalibrationBin bin : bins) out.append('|').append(bin.binHash);
            return out.toString();
        }
    }

    public static Metrics metrics(List<Observation> rows, int fixedBinCount) {
        if (rows == null || rows.isEmpty() || fixedBinCount < 2 || fixedBinCount > 100)
            throw new IllegalArgumentException("R929_METRICS_INPUT_INVALID");
        String policyHash = rows.get(0).selectionPolicyHash;
        String calibrationHash = rows.get(0).calibrationHash;
        int planned = rows.size(), eligible = 0, selected = 0, real = 0, hits = 0;
        double brier = 0.0d, nll = 0.0d;
        int[] binN = new int[fixedBinCount];
        int[] binHits = new int[fixedBinCount];
        double[] binP = new double[fixedBinCount];
        for (Observation row : rows) {
            if (row == null || !policyHash.equals(row.selectionPolicyHash)
                    || !calibrationHash.equals(row.calibrationHash))
                throw new IllegalArgumentException("R929_METRICS_POLICY_OR_CAL_DRIFT");
            if (row.eligible) eligible++;
            if (!row.selected) continue;
            selected++;
            if (row.real) real++;
            if (row.hit) hits++;
            double y = row.hit ? 1.0d : 0.0d;
            double p = clamp(row.probability, 1.0e-12d, 1.0d - 1.0e-12d);
            double error = p - y;
            brier += error * error;
            nll += -(y * Math.log(p) + (1.0d - y) * Math.log(1.0d - p));
            int index = Math.min(fixedBinCount - 1, (int) Math.floor(p * fixedBinCount));
            binN[index]++;
            binHits[index] += row.hit ? 1 : 0;
            binP[index] += p;
        }
        ArrayList<CalibrationBin> bins = new ArrayList<CalibrationBin>();
        double ece = 0.0d;
        for (int i = 0; i < fixedBinCount; i++) {
            double lower = i / (double) fixedBinCount;
            double upper = (i + 1) / (double) fixedBinCount;
            CalibrationBin bin = new CalibrationBin(lower, upper, binN[i], binHits[i]);
            bins.add(bin);
            if (selected > 0 && binN[i] > 0) {
                double meanP = binP[i] / binN[i];
                double rate = binHits[i] / (double) binN[i];
                ece += binN[i] / (double) selected * Math.abs(meanP - rate);
            }
        }
        double hitRate = selected == 0 ? Double.NaN : hits / (double) selected;
        double coverage = planned == 0 ? 0.0d : selected / (double) planned;
        double realCoverage = planned == 0 ? 0.0d : real / (double) planned;
        double[] interval = wilson(hits, selected, 1.959963984540054d);
        return new Metrics(planned, eligible, selected, real, hits, hitRate, coverage,
                realCoverage, selected == 0 ? Double.NaN : brier / selected,
                selected == 0 ? Double.NaN : nll / selected,
                selected == 0 ? Double.NaN : ece, interval[0], interval[1],
                Collections.unmodifiableList(bins), policyHash, calibrationHash);
    }

    public static final class StabilityResult {
        public final int perturbations;
        public final double k3Retention;
        public final double rankOneRetention;
        public final double thirdNumberRobustness;
        public final String definitionVersion;
        public final String inputHash;
        public final String resultHash;

        StabilityResult(int perturbations, double k3Retention, double rankOneRetention,
                double thirdNumberRobustness, String definitionVersion, String inputHash) {
            this.perturbations = perturbations;
            this.k3Retention = k3Retention;
            this.rankOneRetention = rankOneRetention;
            this.thirdNumberRobustness = thirdNumberRobustness;
            this.definitionVersion = definitionVersion;
            this.inputHash = inputHash;
            this.resultHash = sha256(perturbations + "|" + k3Retention + "|"
                    + rankOneRetention + "|" + thirdNumberRobustness + "|"
                    + definitionVersion + "|" + inputHash);
        }
    }

    /** Stability is defined by frozen perturbations/expert deletions, not by a fixed UI number. */
    public static StabilityResult stability(int[] baselineK3, List<int[]> perturbedK3,
            String definitionVersion, String inputHash) {
        validateK3(baselineK3);
        if (perturbedK3 == null || perturbedK3.isEmpty())
            throw new IllegalArgumentException("R929_STABILITY_PERTURBATIONS_EMPTY");
        int totalIntersection = 0, exact = 0, thirdRetained = 0;
        for (int[] candidate : perturbedK3) {
            validateK3(candidate);
            int intersection = 0;
            for (int value : baselineK3) if (contains(candidate, value)) intersection++;
            totalIntersection += intersection;
            if (intersection == 3) exact++;
            if (contains(candidate, baselineK3[2])) thirdRetained++;
        }
        int n = perturbedK3.size();
        return new StabilityResult(n, totalIntersection / (3.0d * n), exact / (double) n,
                thirdRetained / (double) n, required(definitionVersion, "STABILITY_VERSION"),
                requireHash(inputHash, "STABILITY_INPUT_HASH"));
    }

    public static final class UncertaintyBreakdown {
        public final double selectedHitProbability;
        public final int reliabilityBinN;
        public final double reliabilityWilsonLower;
        public final double reliabilityWilsonUpper;
        public final int calibrationSupport;
        public final int regimeSupport;
        public final double modelDisagreement;
        public final double driftScore;
        public final double nonconformity;
        public final double stability;
        public final SupportState state;
        public final String reason;
        public final String latestSuccessfulIssue;
        public final String inputHash;
        public final String breakdownHash;

        public UncertaintyBreakdown(CalibrationResult calibration, int calibrationSupport,
                int regimeSupport, double modelDisagreement, double driftScore,
                double nonconformity, StabilityResult stability, String latestSuccessfulIssue,
                String inputHash) {
            if (calibration == null || calibrationSupport < 0 || regimeSupport < 0
                    || !unit(modelDisagreement) || !unit(driftScore) || !unit(nonconformity)
                    || stability == null)
                throw new IllegalArgumentException("R929_UNCERTAINTY_INPUT_INVALID");
            this.selectedHitProbability = calibration.selectedHitProbability;
            this.reliabilityBinN = calibration.reliabilityBin == null
                    ? 0 : calibration.reliabilityBin.support;
            this.reliabilityWilsonLower = calibration.reliabilityBin == null
                    ? Double.NaN : calibration.reliabilityBin.wilsonLower;
            this.reliabilityWilsonUpper = calibration.reliabilityBin == null
                    ? Double.NaN : calibration.reliabilityBin.wilsonUpper;
            this.calibrationSupport = calibrationSupport;
            this.regimeSupport = regimeSupport;
            this.modelDisagreement = modelDisagreement;
            this.driftScore = driftScore;
            this.nonconformity = nonconformity;
            this.stability = stability.k3Retention;
            SupportState derived = calibration.supportState;
            String derivedReason = calibration.reason;
            if (derived == SupportState.SUPPORTED && regimeSupport < 30) {
                derived = SupportState.DOMAIN_UNSUPPORTED;
                derivedReason = "REGIME_SUPPORT_LOW";
            } else if (derived == SupportState.SUPPORTED
                    && (driftScore > 0.70d || modelDisagreement > 0.70d
                            || stability.k3Retention < 0.50d)) {
                derived = SupportState.INVALID;
                derivedReason = "SELECTIVE_REJECT_UNCERTAINTY";
            }
            this.state = derived;
            this.reason = derivedReason;
            this.latestSuccessfulIssue = clean(latestSuccessfulIssue);
            this.inputHash = requireHash(inputHash, "UNCERTAINTY_INPUT_HASH");
            this.breakdownHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return selectedHitProbability + "|" + reliabilityBinN + "|"
                    + reliabilityWilsonLower + "|" + reliabilityWilsonUpper + "|"
                    + calibrationSupport + "|" + regimeSupport + "|" + modelDisagreement
                    + "|" + driftScore + "|" + nonconformity + "|" + stability + "|"
                    + state.name() + "|" + reason + "|" + latestSuccessfulIssue + "|" + inputHash;
        }

        /** A nonconformity score is a rejection feature, never a hit probability. */
        public boolean selectivePass() { return state == SupportState.SUPPORTED; }
    }

    public static final class JointQualityEvidence {
        public final Metrics metrics;
        public final int minimumFuture;
        public final boolean enoughFuture;
        public final boolean hitRatePass;
        public final boolean coveragePass;
        public final boolean calibrationPass;
        public final boolean pointTargetMet;
        public final String status;
        public final String evidenceHash;

        JointQualityEvidence(Metrics metrics, int minimumFuture) {
            this.metrics = metrics;
            this.minimumFuture = minimumFuture;
            this.enoughFuture = metrics.planned >= minimumFuture && metrics.selected > 0;
            this.hitRatePass = enoughFuture && finite(metrics.hitRate)
                    && metrics.hitRate >= TARGET_HIT_RATE;
            this.coveragePass = enoughFuture && metrics.coverage >= TARGET_COVERAGE;
            this.calibrationPass = enoughFuture && finite(metrics.ece) && metrics.ece <= TARGET_ECE;
            this.pointTargetMet = hitRatePass && coveragePass && calibrationPass;
            if (!enoughFuture) status = "WAIT_FUTURE_SUPPORT";
            else if (!coveragePass) status = "FAIL_EFFECT_COVERAGE";
            else if (!hitRatePass) status = "FAIL_EFFECT_HIT_RATE";
            else if (!calibrationPass) status = "FAIL_EFFECT_ECE";
            else status = "POINT_TARGET_MET_REQUIRES_CERTIFICATE";
            this.evidenceHash = sha256(metrics.metricsHash + '|' + minimumFuture + '|'
                    + status + '|' + (pointTargetMet ? 1 : 0));
        }
    }

    public static JointQualityEvidence jointQuality(List<Observation> sameFutureRows,
            int fixedBinCount, int minimumFuture) {
        if (minimumFuture < 1) throw new IllegalArgumentException("R929_MIN_FUTURE_INVALID");
        return new JointQualityEvidence(metrics(sameFutureRows, fixedBinCount), minimumFuture);
    }

    /** Market regime is independent from runtime/device health. */
    public static final class DomainRoute {
        public final String marketRegime;
        public final String runtimeHealth;
        public final int marketSupport;
        public final boolean marketCertified;
        public final boolean runtimeHealthy;
        public final double contributionWeight;
        public final String reason;
        public final String routeHash;

        public DomainRoute(String marketRegime, String runtimeHealth, int marketSupport,
                boolean marketCertified, boolean runtimeHealthy, double requestedWeight,
                String targetIssue, String routeInputHash) {
            this.marketRegime = required(marketRegime, "MARKET_REGIME");
            this.runtimeHealth = required(runtimeHealth, "RUNTIME_HEALTH");
            if (marketSupport < 0 || !finite(requestedWeight) || requestedWeight < 0.0d
                    || requestedWeight > 0.30d)
                throw new IllegalArgumentException("R929_DOMAIN_ROUTE_INPUT_INVALID");
            this.marketSupport = marketSupport;
            this.marketCertified = marketCertified && marketSupport >= 120;
            this.runtimeHealthy = runtimeHealthy;
            if (!this.runtimeHealthy) {
                this.contributionWeight = 0.0d;
                this.reason = "RUNTIME_HEALTH_PAUSE";
            } else if (!this.marketCertified) {
                this.contributionWeight = 0.0d;
                this.reason = "MARKET_DOMAIN_FALLBACK_OR_PAUSE";
            } else {
                this.contributionWeight = requestedWeight;
                this.reason = "DOMAIN_CERTIFIED_PREDRAW_ROUTE";
            }
            this.routeHash = sha256(this.marketRegime + '|' + this.runtimeHealth + '|'
                    + marketSupport + '|' + (this.marketCertified ? 1 : 0) + '|'
                    + (runtimeHealthy ? 1 : 0) + '|' + this.contributionWeight + '|'
                    + required(targetIssue, "DOMAIN_TARGET") + '|'
                    + requireHash(routeInputHash, "DOMAIN_ROUTE_INPUT_HASH"));
        }
    }

    public static double[] wilson(int hits, int n, double z) {
        if (n <= 0) return new double[] {Double.NaN, Double.NaN};
        if (hits < 0 || hits > n || !finite(z) || z <= 0.0d)
            throw new IllegalArgumentException("R929_WILSON_INPUT_INVALID");
        double p = hits / (double) n;
        double z2 = z * z;
        double denominator = 1.0d + z2 / n;
        double center = (p + z2 / (2.0d * n)) / denominator;
        double half = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n)
                / denominator;
        return new double[] {Math.max(0.0d, center - half), Math.min(1.0d, center + half)};
    }

    private static double interpolate(double x, double[] xs, double[] ys) {
        if (x <= xs[0]) return ys[0];
        if (x >= xs[xs.length - 1]) return ys[ys.length - 1];
        int low = 0;
        int high = xs.length - 1;
        while (high - low > 1) {
            int middle = (low + high) >>> 1;
            if (x < xs[middle]) high = middle; else low = middle;
        }
        double width = xs[high] - xs[low];
        double ratio = width <= 0.0d ? 0.0d : (x - xs[low]) / width;
        return clamp(ys[low] + ratio * (ys[high] - ys[low]), 0.0d, 1.0d);
    }

    private static double sigmoid(double value) {
        if (value >= 0.0d) {
            double z = Math.exp(-Math.min(value, 700.0d));
            return 1.0d / (1.0d + z);
        }
        double z = Math.exp(Math.max(value, -700.0d));
        return z / (1.0d + z);
    }

    private static void validateK3(int[] values) {
        if (values == null || values.length != 3)
            throw new IllegalArgumentException("R929_K3_SHAPE");
        boolean[] seen = new boolean[11];
        for (int value : values) {
            if (value < 1 || value > 10 || seen[value])
                throw new IllegalArgumentException("R929_K3_VALUE");
            seen[value] = true;
        }
    }

    private static boolean contains(int[] values, int target) {
        for (int value : values) if (value == target) return true;
        return false;
    }

    private static void appendDoubles(StringBuilder out, double[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(Long.toHexString(Double.doubleToLongBits(values[i])));
        }
    }

    private static boolean unit(double value) { return probability(value); }
    private static boolean probability(double value) {
        return finite(value) && value >= 0.0d && value <= 1.0d;
    }
    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
    private static double clamp(double value, double low, double high) {
        if (!finite(value)) return low;
        return Math.max(low, Math.min(high, value));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R929_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (clean.length() != 64) throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        }
        return clean;
    }
    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte item : bytes)
                out.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return out.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R929_SHA256_UNAVAILABLE", failure);
        }
    }
}
