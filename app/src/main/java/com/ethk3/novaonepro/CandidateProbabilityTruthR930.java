package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Manifest-aligned C03 contract: raw ranking score and selected-action hit probability are different
 * evidence types. Only a label-free, previously frozen calibration bound to the exact selection
 * policy can contribute Brier/ECE rows. Historical recalculation remains diagnostic-only.
 */
public final class CandidateProbabilityTruthR930 {
    public static final String REVISION = "R930_C03_PROBABILITY_TRUTH_V1";
    public static final int DEFAULT_BINS = 10;
    public static final int MIN_CANDIDATE_CAL_SUPPORT = 120;
    public static final int MIN_FAMILY_CAL_SUPPORT = 240;

    private CandidateProbabilityTruthR930() {}

    public enum ProbabilitySource {
        CANDIDATE_CALIBRATION,
        FROZEN_FAMILY_SHRINKAGE,
        RAW_SCORE_NOT_CALIBRATED,
        HISTORICAL_DIAGNOSTIC_ONLY,
        MISSING_CALIBRATION,
        SELECTION_POLICY_MISMATCH
    }

    /** Frozen calibration identity. Changing selection semantics requires a new calibration. */
    public static final class CalibrationIdentity {
        public final String calibrationId;
        public final String calibrationSha256;
        public final String datasetSha256;
        public final String featureSchemaSha256;
        public final String selectionPolicySha256;
        public final String familyId;
        public final int supportN;
        public final String trainedThroughIssue;
        public final boolean labelFreeAtUse;
        public final boolean frozen;
        public final String identitySha256;

        public CalibrationIdentity(
                String calibrationId,
                String calibrationSha256,
                String datasetSha256,
                String featureSchemaSha256,
                String selectionPolicySha256,
                String familyId,
                int supportN,
                String trainedThroughIssue,
                boolean labelFreeAtUse,
                boolean frozen) {
            this.calibrationId = required(calibrationId, "CALIBRATION_ID");
            this.calibrationSha256 = hash(calibrationSha256, "CALIBRATION_HASH");
            this.datasetSha256 = hash(datasetSha256, "DATASET_HASH");
            this.featureSchemaSha256 = hash(featureSchemaSha256, "FEATURE_SCHEMA_HASH");
            this.selectionPolicySha256 = hash(selectionPolicySha256, "SELECTION_POLICY_HASH");
            this.familyId = clean(familyId);
            this.supportN = Math.max(0, supportN);
            this.trainedThroughIssue = issue(trainedThroughIssue, "TRAINED_THROUGH_ISSUE");
            this.labelFreeAtUse = labelFreeAtUse;
            this.frozen = frozen;
            this.identitySha256 = V51ChecksumEngine.sha256(
                    REVISION + "|CAL|" + this.calibrationId + '|' + this.calibrationSha256 + '|'
                            + this.datasetSha256 + '|' + this.featureSchemaSha256 + '|'
                            + this.selectionPolicySha256 + '|' + this.familyId + '|' + this.supportN
                            + '|' + this.trainedThroughIssue + '|' + this.labelFreeAtUse + '|'
                            + this.frozen);
        }

        public boolean candidateReady() {
            return frozen && labelFreeAtUse && supportN >= MIN_CANDIDATE_CAL_SUPPORT
                    && familyId.length() == 0;
        }

        public boolean familyReady() {
            return frozen && labelFreeAtUse && supportN >= MIN_FAMILY_CAL_SUPPORT
                    && familyId.length() > 0;
        }
    }

    /** One immutable predraw row. rawScore is never silently clamped into selectedHitP. */
    public static final class Row {
        public final String targetIssue;
        public final String actionSha256;
        public final double rawScore;
        public final double selectedHitP;
        public final int hitLabel;
        public final ProbabilitySource source;
        public final String calibrationIdentitySha256;
        public final String selectionPolicySha256;
        public final String maxSourceIssue;
        public final long frozenAtMillis;
        public final long drawAtMillis;
        public final boolean historicalBackfill;
        public final String rowSha256;

        private Row(
                String targetIssue,
                String actionSha256,
                double rawScore,
                double selectedHitP,
                int hitLabel,
                ProbabilitySource source,
                String calibrationIdentitySha256,
                String selectionPolicySha256,
                String maxSourceIssue,
                long frozenAtMillis,
                long drawAtMillis,
                boolean historicalBackfill) {
            this.targetIssue = issue(targetIssue, "TARGET_ISSUE");
            this.actionSha256 = hash(actionSha256, "ACTION_HASH");
            if (!finite(rawScore)) throw new IllegalArgumentException("C03_RAW_SCORE_NONFINITE");
            this.rawScore = rawScore;
            this.selectedHitP = selectedHitP;
            if (hitLabel != -1 && hitLabel != 0 && hitLabel != 1) {
                throw new IllegalArgumentException("C03_HIT_LABEL_INVALID");
            }
            this.hitLabel = hitLabel;
            this.source = source == null ? ProbabilitySource.MISSING_CALIBRATION : source;
            this.calibrationIdentitySha256 = clean(calibrationIdentitySha256);
            this.selectionPolicySha256 = hash(selectionPolicySha256, "ROW_SELECTION_HASH");
            this.maxSourceIssue = issue(maxSourceIssue, "MAX_SOURCE_ISSUE");
            this.frozenAtMillis = frozenAtMillis;
            this.drawAtMillis = drawAtMillis;
            this.historicalBackfill = historicalBackfill;
            if (Batch34EvidenceContractR929.compareIssue(this.maxSourceIssue, this.targetIssue) >= 0) {
                throw new IllegalArgumentException("C03_ROW_PIT_ISSUE_VIOLATION");
            }
            if (frozenAtMillis < 0L || drawAtMillis <= frozenAtMillis) {
                throw new IllegalArgumentException("C03_ROW_PIT_TIME_VIOLATION");
            }
            if (probabilitySourceCanCertify(this.source)) {
                if (!probability(selectedHitP)) {
                    throw new IllegalArgumentException("C03_SELECTED_HIT_P_RANGE");
                }
                hash(this.calibrationIdentitySha256, "ROW_CALIBRATION_IDENTITY_HASH");
                if (historicalBackfill) {
                    throw new IllegalArgumentException("C03_HISTORICAL_BACKFILL_CANNOT_CERTIFY");
                }
            } else if (!Double.isNaN(selectedHitP)) {
                throw new IllegalArgumentException("C03_UNCALIBRATED_ROW_MUST_NOT_HAVE_PROBABILITY");
            }
            this.rowSha256 = V51ChecksumEngine.sha256(
                    REVISION + "|ROW|" + this.targetIssue + '|' + this.actionSha256 + '|'
                            + fmt(this.rawScore) + '|' + fmt(this.selectedHitP) + '|' + this.hitLabel
                            + '|' + this.source + '|' + this.calibrationIdentitySha256 + '|'
                            + this.selectionPolicySha256 + '|' + this.maxSourceIssue + '|'
                            + this.frozenAtMillis + '|' + this.drawAtMillis + '|'
                            + this.historicalBackfill);
        }

        public boolean probabilityCertifiable() {
            return probabilitySourceCanCertify(source) && !historicalBackfill
                    && probability(selectedHitP) && calibrationIdentitySha256.length() == 64;
        }

        public boolean settled() { return hitLabel == 0 || hitLabel == 1; }
    }

    public static Row rawOnly(
            String targetIssue,
            String actionSha256,
            double rawScore,
            int hitLabel,
            String selectionPolicySha256,
            String maxSourceIssue,
            long frozenAtMillis,
            long drawAtMillis) {
        return new Row(targetIssue, actionSha256, rawScore, Double.NaN, hitLabel,
                ProbabilitySource.RAW_SCORE_NOT_CALIBRATED, "", selectionPolicySha256,
                maxSourceIssue, frozenAtMillis, drawAtMillis, false);
    }

    public static Row historicalDiagnostic(
            String targetIssue,
            String actionSha256,
            double rawScore,
            int hitLabel,
            String selectionPolicySha256,
            String maxSourceIssue,
            long frozenAtMillis,
            long drawAtMillis) {
        return new Row(targetIssue, actionSha256, rawScore, Double.NaN, hitLabel,
                ProbabilitySource.HISTORICAL_DIAGNOSTIC_ONLY, "", selectionPolicySha256,
                maxSourceIssue, frozenAtMillis, drawAtMillis, true);
    }

    public static Row calibrated(
            String targetIssue,
            String actionSha256,
            double rawScore,
            double selectedHitP,
            int hitLabel,
            CalibrationIdentity calibration,
            String runtimeSelectionPolicySha256,
            String maxSourceIssue,
            long frozenAtMillis,
            long drawAtMillis) {
        if (calibration == null) throw new IllegalArgumentException("C03_CALIBRATION_NULL");
        String runtimeSelection = hash(runtimeSelectionPolicySha256, "RUNTIME_SELECTION_HASH");
        if (!calibration.selectionPolicySha256.equals(runtimeSelection)) {
            return new Row(targetIssue, actionSha256, rawScore, Double.NaN, hitLabel,
                    ProbabilitySource.SELECTION_POLICY_MISMATCH, "", runtimeSelection,
                    maxSourceIssue, frozenAtMillis, drawAtMillis, false);
        }
        ProbabilitySource source;
        if (calibration.candidateReady()) source = ProbabilitySource.CANDIDATE_CALIBRATION;
        else if (calibration.familyReady()) source = ProbabilitySource.FROZEN_FAMILY_SHRINKAGE;
        else {
            return new Row(targetIssue, actionSha256, rawScore, Double.NaN, hitLabel,
                    ProbabilitySource.MISSING_CALIBRATION, "", runtimeSelection,
                    maxSourceIssue, frozenAtMillis, drawAtMillis, false);
        }
        if (Batch34EvidenceContractR929.compareIssue(
                calibration.trainedThroughIssue, targetIssue) >= 0) {
            throw new IllegalArgumentException("C03_CALIBRATION_TRAINED_THROUGH_TARGET_OR_FUTURE");
        }
        return new Row(targetIssue, actionSha256, rawScore, selectedHitP, hitLabel,
                source, calibration.identitySha256, runtimeSelection,
                maxSourceIssue, frozenAtMillis, drawAtMillis, false);
    }

    public static final class ReliabilityBin {
        public final int index;
        public final double lowerInclusive;
        public final double upperInclusive;
        public final int n;
        public final int hits;
        public final double meanProbability;
        public final double empiricalHitRate;
        public final double absoluteGap;

        ReliabilityBin(int index, double lower, double upper, int n, int hits,
                       double meanProbability, double empiricalHitRate) {
            this.index = index;
            this.lowerInclusive = lower;
            this.upperInclusive = upper;
            this.n = n;
            this.hits = hits;
            this.meanProbability = meanProbability;
            this.empiricalHitRate = empiricalHitRate;
            this.absoluteGap = n == 0 ? Double.NaN
                    : Math.abs(meanProbability - empiricalHitRate);
        }
    }

    public static final class Report {
        public final String selectionPolicySha256;
        public final int totalRows;
        public final int settledRows;
        public final int calibratedSettledRows;
        public final int rawOnlyRows;
        public final int historicalDiagnosticRows;
        public final int excludedRows;
        public final double brier;
        public final double ece;
        public final List<ReliabilityBin> bins;
        public final List<String> exclusionCodes;
        public final String reportSha256;

        Report(String selectionPolicySha256, int totalRows, int settledRows,
               int calibratedSettledRows, int rawOnlyRows, int historicalDiagnosticRows,
               int excludedRows, double brier, double ece, List<ReliabilityBin> bins,
               List<String> exclusionCodes, String canonical) {
            this.selectionPolicySha256 = selectionPolicySha256;
            this.totalRows = totalRows;
            this.settledRows = settledRows;
            this.calibratedSettledRows = calibratedSettledRows;
            this.rawOnlyRows = rawOnlyRows;
            this.historicalDiagnosticRows = historicalDiagnosticRows;
            this.excludedRows = excludedRows;
            this.brier = brier;
            this.ece = ece;
            this.bins = Collections.unmodifiableList(new ArrayList<ReliabilityBin>(bins));
            this.exclusionCodes = Collections.unmodifiableList(
                    new ArrayList<String>(exclusionCodes));
            this.reportSha256 = V51ChecksumEngine.sha256(REVISION + "|REPORT|" + canonical);
        }

        public boolean probabilityEvidenceAvailable() {
            return calibratedSettledRows > 0 && finite(brier) && finite(ece);
        }
    }

    public static Report evaluate(List<Row> rows, String expectedSelectionPolicySha256, int binCount) {
        String expected = hash(expectedSelectionPolicySha256, "EXPECTED_SELECTION_HASH");
        if (binCount < 2 || binCount > 100) {
            throw new IllegalArgumentException("C03_BIN_COUNT_RANGE");
        }
        if (rows == null) rows = Collections.emptyList();
        ArrayList<Row> ordered = new ArrayList<Row>(rows);
        Collections.sort(ordered, new Comparator<Row>() {
            @Override public int compare(Row left, Row right) {
                int issueCompare = Batch34EvidenceContractR929.compareIssue(
                        left.targetIssue, right.targetIssue);
                if (issueCompare != 0) return issueCompare;
                return left.actionSha256.compareTo(right.actionSha256);
            }
        });
        int total = 0, settled = 0, calibrated = 0, raw = 0, historical = 0, excluded = 0;
        double brierSum = 0.0d;
        int[] binN = new int[binCount];
        int[] binHits = new int[binCount];
        double[] binProbability = new double[binCount];
        ArrayList<String> exclusions = new ArrayList<String>();
        StringBuilder canonical = new StringBuilder(expected);
        for (Row row : ordered) {
            if (row == null) { excluded++; exclusions.add("C03_NULL_ROW"); continue; }
            total++;
            canonical.append('|').append(row.rowSha256);
            if (row.source == ProbabilitySource.RAW_SCORE_NOT_CALIBRATED) raw++;
            if (row.source == ProbabilitySource.HISTORICAL_DIAGNOSTIC_ONLY) historical++;
            if (!row.settled()) { excluded++; exclusions.add("C03_UNSETTLED:" + row.targetIssue); continue; }
            settled++;
            if (!row.selectionPolicySha256.equals(expected)) {
                excluded++; exclusions.add("C03_SELECTION_POLICY_MISMATCH:" + row.targetIssue); continue;
            }
            if (!row.probabilityCertifiable()) {
                excluded++; exclusions.add("C03_NOT_CALIBRATED:" + row.source + ':' + row.targetIssue); continue;
            }
            calibrated++;
            double label = row.hitLabel;
            double delta = row.selectedHitP - label;
            brierSum += delta * delta;
            int bin = Math.min(binCount - 1, (int) Math.floor(row.selectedHitP * binCount));
            binN[bin]++;
            binHits[bin] += row.hitLabel;
            binProbability[bin] += row.selectedHitP;
        }
        ArrayList<ReliabilityBin> reliability = new ArrayList<ReliabilityBin>();
        double ece = calibrated == 0 ? Double.NaN : 0.0d;
        for (int i = 0; i < binCount; i++) {
            double mean = binN[i] == 0 ? Double.NaN : binProbability[i] / binN[i];
            double rate = binN[i] == 0 ? Double.NaN : (double) binHits[i] / binN[i];
            ReliabilityBin bin = new ReliabilityBin(i, (double) i / binCount,
                    (double) (i + 1) / binCount, binN[i], binHits[i], mean, rate);
            reliability.add(bin);
            if (binN[i] > 0) ece += ((double) binN[i] / calibrated) * bin.absoluteGap;
            canonical.append("|BIN").append(i).append(':').append(binN[i]).append(':')
                    .append(binHits[i]).append(':').append(fmt(mean)).append(':').append(fmt(rate));
        }
        double brier = calibrated == 0 ? Double.NaN : brierSum / calibrated;
        canonical.append("|TOTAL=").append(total).append("|SETTLED=").append(settled)
                .append("|CAL=").append(calibrated).append("|RAW=").append(raw)
                .append("|HIST=").append(historical).append("|EXCLUDED=").append(excluded)
                .append("|BRIER=").append(fmt(brier)).append("|ECE=").append(fmt(ece));
        return new Report(expected, total, settled, calibrated, raw, historical, excluded,
                brier, ece, reliability, exclusions, canonical.toString());
    }

    /** Ranking scale invariance check: rescaling raw scores must not alter calibrated p evidence. */
    public static boolean sameProbabilityEvidenceAcrossRawScales(List<Row> left, List<Row> right) {
        if (left == null || right == null || left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            Row a = left.get(i), b = right.get(i);
            if (a == null || b == null) return false;
            if (!a.targetIssue.equals(b.targetIssue)
                    || !a.actionSha256.equals(b.actionSha256)
                    || a.hitLabel != b.hitLabel
                    || a.source != b.source
                    || !a.calibrationIdentitySha256.equals(b.calibrationIdentitySha256)
                    || !a.selectionPolicySha256.equals(b.selectionPolicySha256)
                    || Double.doubleToLongBits(a.selectedHitP)
                            != Double.doubleToLongBits(b.selectedHitP)) {
                return false;
            }
        }
        return true;
    }

    private static boolean probabilitySourceCanCertify(ProbabilitySource source) {
        return source == ProbabilitySource.CANDIDATE_CALIBRATION
                || source == ProbabilitySource.FROZEN_FAMILY_SHRINKAGE;
    }

    private static boolean probability(double value) {
        return finite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static String issue(String value, String field) {
        String cleaned = required(value, field);
        if (!Batch34EvidenceContractR929.validIssue(cleaned)) {
            throw new IllegalArgumentException("C03_" + field + "_INVALID");
        }
        return cleaned;
    }

    private static String hash(String value, String field) {
        String cleaned = required(value, field).toLowerCase(Locale.US);
        if (!Batch34EvidenceContractR929.isSha256(cleaned)) {
            throw new IllegalArgumentException("C03_" + field + "_INVALID");
        }
        return cleaned;
    }

    private static String required(String value, String field) {
        String cleaned = clean(value);
        if (cleaned.length() == 0) throw new IllegalArgumentException("C03_" + field + "_EMPTY");
        return cleaned;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    private static String fmt(double value) {
        return finite(value) ? String.format(Locale.US, "%.12g", value) : "N/A";
    }
}
