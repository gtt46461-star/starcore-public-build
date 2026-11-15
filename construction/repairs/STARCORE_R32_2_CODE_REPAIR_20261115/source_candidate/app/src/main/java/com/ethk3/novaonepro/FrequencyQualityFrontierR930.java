package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Honest frequency-quality feasible-frontier evaluation.
 *
 * <p>Fifteen percent is an effect/coverage target, never a debt to be repaid by unsafe output.
 * Thresholds are pre-registered, calibrated on a policy window and evaluated once on a later
 * Future window. If no policy jointly supports hit-rate, coverage, calibration, stability and
 * resource constraints, the result is NOT_PROVEN and production keeps its existing decision.
 * This class cannot create or authorize REAL actions.</p>
 */
public final class FrequencyQualityFrontierR930 {
    public static final String VERSION = "R930_FREQUENCY_QUALITY_FRONTIER_V1";
    public static final double TARGET_HIT_RATE = 0.40d;
    public static final double TARGET_COVERAGE = 0.15d;
    public static final double TARGET_ECE = 0.04d;
    public static final double TARGET_STABILITY = 0.50d;
    public static final int MINIMUM_FUTURE_ISSUES = 120;
    public static final boolean FREQUENCY_DEBT_ALLOWED = false;
    public static final boolean MAY_CREATE_REAL = false;

    private FrequencyQualityFrontierR930() {}

    public enum WindowRole { POLICY_TUNING, SEALED_FUTURE, INDEPENDENT_CONFIRMATION }
    public enum Status {
        SUPPORT_PENDING,
        NOT_PROVEN,
        FEASIBLE_POINT_FOUND,
        INDEPENDENT_CONFIRMATION_REQUIRED,
        CONFIRMED_EFFECT_TARGET,
        INVALID
    }

    public static final class ThresholdPolicy {
        public final String policyId;
        public final double minimumCalibratedP;
        public final double maximumUncertainty;
        public final double minimumStability;
        public final double maximumDrift;
        public final double maximumNonconformity;
        public final long maximumLatencyMs;
        public final int minimumDomainSupport;
        public final String calibrationHash;
        public final String selectionPolicyHash;
        public final String domainSchemaHash;
        public final String registrationHash;
        public final String policyHash;

        public ThresholdPolicy(String policyId, double minimumCalibratedP,
                double maximumUncertainty, double minimumStability, double maximumDrift,
                double maximumNonconformity, long maximumLatencyMs, int minimumDomainSupport,
                String calibrationHash, String selectionPolicyHash, String domainSchemaHash,
                String registrationHash) {
            this.policyId = required(policyId, "THRESHOLD_POLICY_ID");
            if (!unit(minimumCalibratedP) || !unit(maximumUncertainty)
                    || !unit(minimumStability) || !unit(maximumDrift)
                    || !unit(maximumNonconformity) || maximumLatencyMs <= 0L
                    || minimumDomainSupport < 0)
                throw new IllegalArgumentException("R930_THRESHOLD_POLICY_INVALID");
            this.minimumCalibratedP = minimumCalibratedP;
            this.maximumUncertainty = maximumUncertainty;
            this.minimumStability = minimumStability;
            this.maximumDrift = maximumDrift;
            this.maximumNonconformity = maximumNonconformity;
            this.maximumLatencyMs = maximumLatencyMs;
            this.minimumDomainSupport = minimumDomainSupport;
            this.calibrationHash = requireHash(calibrationHash, "THRESHOLD_CALIBRATION_HASH");
            this.selectionPolicyHash = requireHash(selectionPolicyHash, "THRESHOLD_SELECTION_HASH");
            this.domainSchemaHash = requireHash(domainSchemaHash, "THRESHOLD_DOMAIN_HASH");
            this.registrationHash = requireHash(registrationHash, "THRESHOLD_REGISTRATION_HASH");
            this.policyHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return policyId + '|' + minimumCalibratedP + '|' + maximumUncertainty + '|'
                    + minimumStability + '|' + maximumDrift + '|' + maximumNonconformity + '|'
                    + maximumLatencyMs + '|' + minimumDomainSupport + '|' + calibrationHash
                    + '|' + selectionPolicyHash + '|' + domainSchemaHash + '|'
                    + registrationHash;
        }
    }

    public static final class Observation {
        public final String targetIssue;
        public final String maxSourceIssue;
        public final boolean baseEligible;
        public final double calibratedP;
        public final double uncertainty;
        public final double stability;
        public final double drift;
        public final double nonconformity;
        public final int domainSupport;
        public final long latencyMs;
        public final boolean hit;
        public final String calibrationHash;
        public final String selectionPolicyHash;
        public final String domainSchemaHash;
        public final String inputHash;
        public final String observationHash;

        public Observation(String targetIssue, String maxSourceIssue, boolean baseEligible,
                double calibratedP, double uncertainty, double stability, double drift,
                double nonconformity, int domainSupport, long latencyMs, boolean hit,
                String calibrationHash, String selectionPolicyHash, String domainSchemaHash,
                String inputHash) {
            this.targetIssue = required(targetIssue, "OBS_TARGET");
            this.maxSourceIssue = required(maxSourceIssue, "OBS_MAX_SOURCE");
            if (!PitFeatureDagR929.isStrictPast(this.maxSourceIssue, this.targetIssue)
                    || !unit(calibratedP) || !unit(uncertainty) || !unit(stability)
                    || !unit(drift) || !unit(nonconformity) || domainSupport < 0 || latencyMs < 0L)
                throw new IllegalArgumentException("R930_FRONTIER_OBSERVATION_INVALID");
            this.baseEligible = baseEligible;
            this.calibratedP = calibratedP;
            this.uncertainty = uncertainty;
            this.stability = stability;
            this.drift = drift;
            this.nonconformity = nonconformity;
            this.domainSupport = domainSupport;
            this.latencyMs = latencyMs;
            this.hit = hit;
            this.calibrationHash = requireHash(calibrationHash, "OBS_CALIBRATION_HASH");
            this.selectionPolicyHash = requireHash(selectionPolicyHash, "OBS_SELECTION_HASH");
            this.domainSchemaHash = requireHash(domainSchemaHash, "OBS_DOMAIN_HASH");
            this.inputHash = requireHash(inputHash, "OBS_INPUT_HASH");
            this.observationHash = sha256(this.targetIssue + '|' + this.maxSourceIssue + '|'
                    + (baseEligible ? 1 : 0) + '|' + calibratedP + '|' + uncertainty + '|'
                    + stability + '|' + drift + '|' + nonconformity + '|' + domainSupport + '|'
                    + latencyMs + '|' + (hit ? 1 : 0) + '|' + this.calibrationHash + '|'
                    + this.selectionPolicyHash + '|' + this.domainSchemaHash + '|'
                    + this.inputHash);
        }
    }

    public static final class Decision {
        public final String targetIssue;
        public final String policyHash;
        public final boolean selected;
        public final String reason;
        public final boolean mayCreateReal;
        public final String decisionHash;

        Decision(String targetIssue, String policyHash, boolean selected, String reason,
                String observationHash) {
            this.targetIssue = targetIssue;
            this.policyHash = policyHash;
            this.selected = selected;
            this.reason = reason;
            this.mayCreateReal = false;
            this.decisionHash = sha256(targetIssue + '|' + policyHash + '|'
                    + (selected ? 1 : 0) + '|' + reason + '|' + observationHash
                    + "|directREAL=0|frequencyDebt=0");
        }
    }

    public static Decision apply(ThresholdPolicy policy, Observation observation) {
        if (policy == null || observation == null)
            throw new IllegalArgumentException("R930_FRONTIER_APPLY_INPUT");
        if (!policy.calibrationHash.equals(observation.calibrationHash)
                || !policy.selectionPolicyHash.equals(observation.selectionPolicyHash)
                || !policy.domainSchemaHash.equals(observation.domainSchemaHash))
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "POLICY_ARTIFACT_MISMATCH", observation.observationHash);
        if (!observation.baseEligible)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "BASE_NOT_ELIGIBLE", observation.observationHash);
        if (observation.domainSupport < policy.minimumDomainSupport)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "DOMAIN_SUPPORT_LOW", observation.observationHash);
        if (observation.latencyMs > policy.maximumLatencyMs)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "LATENCY_OVER_POLICY", observation.observationHash);
        if (observation.calibratedP < policy.minimumCalibratedP)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "CALIBRATED_P_LOW", observation.observationHash);
        if (observation.uncertainty > policy.maximumUncertainty)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "UNCERTAINTY_HIGH", observation.observationHash);
        if (observation.stability < policy.minimumStability)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "STABILITY_LOW", observation.observationHash);
        if (observation.drift > policy.maximumDrift)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "DRIFT_HIGH", observation.observationHash);
        if (observation.nonconformity > policy.maximumNonconformity)
            return new Decision(observation.targetIssue, policy.policyHash, false,
                    "NONCONFORMITY_HIGH", observation.observationHash);
        return new Decision(observation.targetIssue, policy.policyHash, true,
                "SELECTED_BY_FIXED_QUALITY_POLICY", observation.observationHash);
    }

    public static final class Metrics {
        public final ThresholdPolicy policy;
        public final WindowRole role;
        public final String firstIssue;
        public final String lastIssue;
        public final String windowContentHash;
        public final int plannedN;
        public final int baseEligibleN;
        public final int selectedN;
        public final int hitN;
        public final int longestMissStreak;
        public final double hitRate;
        public final double coverage;
        public final double eligibleCoverage;
        public final double brier;
        public final double nll;
        public final double ece;
        public final double meanStability;
        public final double meanUncertainty;
        public final long p50LatencyMs;
        public final long p95LatencyMs;
        public final double wilsonLower;
        public final double wilsonUpper;
        public final boolean hitTarget;
        public final boolean coverageTarget;
        public final boolean calibrationTarget;
        public final boolean stabilityTarget;
        public final String metricsHash;

        Metrics(ThresholdPolicy policy, WindowRole role, String firstIssue, String lastIssue,
                String windowContentHash, int plannedN, int baseEligibleN, int selectedN,
                int hitN, int longestMissStreak, double hitRate, double coverage,
                double eligibleCoverage, double brier, double nll, double ece,
                double meanStability, double meanUncertainty, long p50LatencyMs,
                long p95LatencyMs, double wilsonLower, double wilsonUpper) {
            this.policy = policy;
            this.role = role;
            this.firstIssue = firstIssue;
            this.lastIssue = lastIssue;
            this.windowContentHash = windowContentHash;
            this.plannedN = plannedN;
            this.baseEligibleN = baseEligibleN;
            this.selectedN = selectedN;
            this.hitN = hitN;
            this.longestMissStreak = longestMissStreak;
            this.hitRate = hitRate;
            this.coverage = coverage;
            this.eligibleCoverage = eligibleCoverage;
            this.brier = brier;
            this.nll = nll;
            this.ece = ece;
            this.meanStability = meanStability;
            this.meanUncertainty = meanUncertainty;
            this.p50LatencyMs = p50LatencyMs;
            this.p95LatencyMs = p95LatencyMs;
            this.wilsonLower = wilsonLower;
            this.wilsonUpper = wilsonUpper;
            this.hitTarget = selectedN > 0 && hitRate >= TARGET_HIT_RATE;
            this.coverageTarget = coverage >= TARGET_COVERAGE;
            this.calibrationTarget = selectedN > 0 && ece <= TARGET_ECE;
            this.stabilityTarget = selectedN > 0 && meanStability >= TARGET_STABILITY;
            this.metricsHash = sha256(policy.policyHash + '|' + role.name() + '|'
                    + firstIssue + '|' + lastIssue + '|' + windowContentHash + '|'
                    + plannedN + '|' + baseEligibleN + '|' + selectedN + '|' + hitN + '|'
                    + longestMissStreak + '|' + hitRate + '|' + coverage + '|'
                    + eligibleCoverage + '|' + brier + '|' + nll + '|' + ece + '|'
                    + meanStability + '|' + meanUncertainty + '|' + p50LatencyMs + '|'
                    + p95LatencyMs + '|' + wilsonLower + '|' + wilsonUpper);
        }

        public boolean jointlyFeasible(long allowedP95Ms) {
            return plannedN >= MINIMUM_FUTURE_ISSUES && hitTarget && coverageTarget
                    && calibrationTarget && stabilityTarget && p95LatencyMs <= allowedP95Ms;
        }
    }

    public static Metrics evaluate(ThresholdPolicy policy, List<Observation> observations,
            WindowRole role, String windowContentHash, int calibrationBins) {
        if (policy == null || observations == null || observations.isEmpty() || role == null
                || calibrationBins < 2 || calibrationBins > 50)
            throw new IllegalArgumentException("R930_FRONTIER_EVALUATE_INPUT");
        String windowHash = requireHash(windowContentHash, "FRONTIER_WINDOW_HASH");
        ArrayList<Observation> rows = new ArrayList<Observation>(observations);
        Collections.sort(rows, new Comparator<Observation>() {
            @Override public int compare(Observation left, Observation right) {
                return PitFeatureDagR929.compareIssues(left.targetIssue, right.targetIssue);
            }
        });
        HashSet<String> issues = new HashSet<String>();
        int eligible = 0, selected = 0, hits = 0, streak = 0, longest = 0;
        double brier = 0.0d, nll = 0.0d, stability = 0.0d, uncertainty = 0.0d;
        int[] binN = new int[calibrationBins];
        int[] binHit = new int[calibrationBins];
        double[] binP = new double[calibrationBins];
        ArrayList<Long> latencies = new ArrayList<Long>();
        StringBuilder computedWindow = new StringBuilder(role.name());
        for (Observation observation : rows) {
            if (observation == null || !issues.add(observation.targetIssue))
                throw new IllegalArgumentException("R930_FRONTIER_ISSUE_DUPLICATE");
            computedWindow.append('|').append(observation.observationHash);
            if (observation.baseEligible) eligible++;
            Decision decision = apply(policy, observation);
            if (!decision.selected) continue;
            selected++;
            if (observation.hit) { hits++; streak = 0; }
            else { streak++; longest = Math.max(longest, streak); }
            double y = observation.hit ? 1.0d : 0.0d;
            double p = clamp(observation.calibratedP, 1.0e-12d, 1.0d - 1.0e-12d);
            double error = p - y;
            brier += error * error;
            nll += -(y * Math.log(p) + (1.0d - y) * Math.log(1.0d - p));
            stability += observation.stability;
            uncertainty += observation.uncertainty;
            latencies.add(Long.valueOf(observation.latencyMs));
            int index = Math.min(calibrationBins - 1,
                    (int) Math.floor(observation.calibratedP * calibrationBins));
            binN[index]++;
            binHit[index] += observation.hit ? 1 : 0;
            binP[index] += observation.calibratedP;
        }
        String computedHash = sha256(computedWindow.toString());
        if (!computedHash.equals(windowHash))
            throw new IllegalArgumentException("R930_FRONTIER_WINDOW_CONTENT_MISMATCH");
        double ece = 0.0d;
        if (selected > 0) {
            for (int i = 0; i < calibrationBins; i++) if (binN[i] > 0) {
                double empirical = binHit[i] / (double) binN[i];
                double predicted = binP[i] / binN[i];
                ece += binN[i] / (double) selected * Math.abs(empirical - predicted);
            }
        } else ece = Double.NaN;
        Collections.sort(latencies);
        long p50 = percentile(latencies, 0.50d);
        long p95 = percentile(latencies, 0.95d);
        double[] interval = SelectionCalibrationR929.wilson(hits, selected,
                1.959963984540054d);
        int planned = rows.size();
        return new Metrics(policy, role, rows.get(0).targetIssue,
                rows.get(rows.size() - 1).targetIssue, windowHash, planned, eligible, selected,
                hits, longest, selected == 0 ? Double.NaN : hits / (double) selected,
                selected / (double) planned, eligible == 0 ? 0.0d : selected / (double) eligible,
                selected == 0 ? Double.NaN : brier / selected,
                selected == 0 ? Double.NaN : nll / selected, ece,
                selected == 0 ? Double.NaN : stability / selected,
                selected == 0 ? Double.NaN : uncertainty / selected,
                p50, p95, interval[0], interval[1]);
    }

    public static final class FrontierPoint {
        public final Metrics metrics;
        public final boolean dominated;
        public final List<String> dominatedByPolicyHashes;
        public final double utility;
        public final String pointHash;

        FrontierPoint(Metrics metrics, boolean dominated, List<String> dominatedBy,
                double utility) {
            this.metrics = metrics;
            this.dominated = dominated;
            this.dominatedByPolicyHashes = Collections.unmodifiableList(
                    new ArrayList<String>(dominatedBy));
            this.utility = utility;
            StringBuilder canonical = new StringBuilder(metrics.metricsHash).append('|')
                    .append(dominated ? 1 : 0).append('|').append(utility);
            for (String hash : dominatedBy) canonical.append('|').append(hash);
            this.pointHash = sha256(canonical.toString());
        }
    }

    public static final class FrontierResult {
        public final WindowRole role;
        public final List<FrontierPoint> points;
        public final FrontierPoint selected;
        public final Status status;
        public final String reason;
        public final String frontierHash;

        FrontierResult(WindowRole role, List<FrontierPoint> points, FrontierPoint selected,
                Status status, String reason) {
            this.role = role;
            this.points = Collections.unmodifiableList(new ArrayList<FrontierPoint>(points));
            this.selected = selected;
            this.status = status;
            this.reason = reason;
            StringBuilder canonical = new StringBuilder(role.name()).append('|')
                    .append(status.name()).append('|').append(reason).append('|')
                    .append(selected == null ? "" : selected.pointHash);
            for (FrontierPoint point : points) canonical.append('|').append(point.pointHash);
            this.frontierHash = sha256(canonical.toString());
        }
    }

    public static FrontierResult frontier(List<Metrics> metrics, long allowedP95Ms) {
        if (metrics == null || metrics.isEmpty() || allowedP95Ms <= 0L)
            throw new IllegalArgumentException("R930_FRONTIER_INPUT");
        WindowRole role = metrics.get(0).role;
        HashSet<String> policies = new HashSet<String>();
        ArrayList<FrontierPoint> points = new ArrayList<FrontierPoint>();
        for (Metrics candidate : metrics) {
            if (candidate == null || candidate.role != role
                    || !policies.add(candidate.policy.policyHash))
                throw new IllegalArgumentException("R930_FRONTIER_POLICY_OR_ROLE_INVALID");
            ArrayList<String> dominatedBy = new ArrayList<String>();
            for (Metrics other : metrics) {
                if (candidate == other || other == null) continue;
                if (dominates(other, candidate)) dominatedBy.add(other.policy.policyHash);
            }
            Collections.sort(dominatedBy);
            double utility = utility(candidate, allowedP95Ms);
            points.add(new FrontierPoint(candidate, !dominatedBy.isEmpty(), dominatedBy, utility));
        }
        Collections.sort(points, new Comparator<FrontierPoint>() {
            @Override public int compare(FrontierPoint left, FrontierPoint right) {
                int byDominated = Boolean.compare(left.dominated, right.dominated);
                if (byDominated != 0) return byDominated;
                int byUtility = Double.compare(right.utility, left.utility);
                if (byUtility != 0) return byUtility;
                return left.metrics.policy.policyHash.compareTo(right.metrics.policy.policyHash);
            }
        });
        FrontierPoint selected = null;
        for (FrontierPoint point : points) {
            if (!point.dominated && point.metrics.jointlyFeasible(allowedP95Ms)) {
                selected = point;
                break;
            }
        }
        Status status;
        String reason;
        int planned = points.get(0).metrics.plannedN;
        if (planned < MINIMUM_FUTURE_ISSUES) {
            status = Status.SUPPORT_PENDING;
            reason = "FUTURE_N_LT_120";
            selected = null;
        } else if (selected == null) {
            status = Status.NOT_PROVEN;
            reason = "NO_JOINT_HIT_COVERAGE_ECE_STABILITY_RESOURCE_POINT";
        } else if (role == WindowRole.POLICY_TUNING) {
            status = Status.INDEPENDENT_CONFIRMATION_REQUIRED;
            reason = "POLICY_WINDOW_POINT_CANNOT_PROVE_EFFECT";
        } else if (role == WindowRole.SEALED_FUTURE) {
            status = Status.FEASIBLE_POINT_FOUND;
            reason = "SEALED_FUTURE_POINT_REQUIRES_INDEPENDENT_CONFIRMATION";
        } else {
            status = Status.CONFIRMED_EFFECT_TARGET;
            reason = "INDEPENDENT_FUTURE_JOINT_TARGET_CONFIRMED";
        }
        return new FrontierResult(role, points, selected, status, reason);
    }

    private static boolean dominates(Metrics left, Metrics right) {
        if (!finite(left.hitRate) || !finite(right.hitRate) || !finite(left.ece)
                || !finite(right.ece) || !finite(left.meanStability)
                || !finite(right.meanStability)) return false;
        boolean noWorse = left.hitRate >= right.hitRate
                && left.coverage >= right.coverage
                && left.ece <= right.ece
                && left.meanStability >= right.meanStability
                && left.p95LatencyMs <= right.p95LatencyMs
                && left.longestMissStreak <= right.longestMissStreak;
        boolean strict = left.hitRate > right.hitRate
                || left.coverage > right.coverage
                || left.ece < right.ece
                || left.meanStability > right.meanStability
                || left.p95LatencyMs < right.p95LatencyMs
                || left.longestMissStreak < right.longestMissStreak;
        return noWorse && strict;
    }

    private static double utility(Metrics value, long allowedP95Ms) {
        if (!finite(value.hitRate) || !finite(value.ece) || !finite(value.meanStability))
            return -Double.MAX_VALUE;
        double latencyPenalty = Math.max(0.0d,
                value.p95LatencyMs / (double) allowedP95Ms - 1.0d);
        return 4.0d * value.hitRate + 2.0d * value.coverage
                + value.meanStability - 2.0d * value.ece
                - 0.05d * value.longestMissStreak - latencyPenalty;
    }

    public static String contentHash(WindowRole role, List<Observation> observations) {
        if (role == null || observations == null || observations.isEmpty())
            throw new IllegalArgumentException("R930_WINDOW_CONTENT_INPUT");
        ArrayList<Observation> rows = new ArrayList<Observation>(observations);
        Collections.sort(rows, new Comparator<Observation>() {
            @Override public int compare(Observation left, Observation right) {
                return PitFeatureDagR929.compareIssues(left.targetIssue, right.targetIssue);
            }
        });
        StringBuilder canonical = new StringBuilder(role.name());
        for (Observation row : rows) canonical.append('|').append(row.observationHash);
        return sha256(canonical.toString());
    }

    private static long percentile(List<Long> values, double fraction) {
        if (values.isEmpty()) return 0L;
        int index = Math.max(0, (int) Math.ceil(values.size() * fraction) - 1);
        return values.get(Math.min(values.size() - 1, index)).longValue();
    }
    private static boolean unit(double value) { return finite(value) && value >= 0.0d && value <= 1.0d; }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    private static double clamp(double value, double low, double high) {
        if (!finite(value)) return low;
        return Math.max(low, Math.min(high, value));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R930_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (clean.length() != 64) throw new IllegalArgumentException("R930_" + code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("R930_" + code + "_NOT_SHA256");
        }
        return clean;
    }
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : bytes)
                output.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return output.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R930_SHA256_UNAVAILABLE", failure);
        }
    }
}
