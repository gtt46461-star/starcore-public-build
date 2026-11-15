package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B05 deterministic change-point owner.
 *
 * <p>The active runtime combines standardized surprise, symmetric CUSUM, Page-Hinkley drift,
 * bounded energy distance and posterior hazard. It accepts only point-in-time state vectors and
 * never owns action probability, lane, BET, settlement or ledger mutation.
 */
public final class V51ChangePointEngine {
    public static final String B05_VERSION = "STARCORE_TIANSHU_B05_CHANGE_POINT_V1";
    public static final int EXPECTED_DIMENSION = V51ObservableStateVectorEngine.DIMENSION;
    public static final double MIN_SCALE = 1.0e-4d;
    public static final double DEFAULT_HAZARD = 1.0d / 48.0d;
    public static final double CUSUM_REFERENCE = 0.35d;
    public static final double PAGE_HINKLEY_DELTA = 0.05d;
    public static final double WARN_PROBABILITY = 0.55d;
    public static final double BREAK_PROBABILITY = 0.78d;

    /** Backward-compatible compact probability API. */
    public double probability(double[] current, double[] previous, double[] scale) {
        return evaluate(current, previous, scale).probability;
    }

    public ChangePointReport evaluate(double[] current, double[] previous, double[] scale) {
        long started = System.currentTimeMillis();
        Validation validation = validate(current, previous, scale);
        if (!validation.valid) return ChangePointReport.error(validation.detail);
        double[] z = standardizedDelta(current, previous, scale);
        double surprise = meanSquareClipped(z, 16.0d);
        double absolute = meanAbsolute(z);
        double positiveCusum = 0.0d;
        double negativeCusum = 0.0d;
        double pageHinkleyPositive = 0.0d;
        double pageHinkleyNegative = 0.0d;
        double cumulative = 0.0d;
        double minimum = 0.0d;
        double maximum = 0.0d;
        for (int i = 0; i < z.length; i++) {
            double value = V51Math.finite(z[i]);
            positiveCusum += Math.max(0.0d, value - CUSUM_REFERENCE);
            negativeCusum += Math.max(0.0d, -value - CUSUM_REFERENCE);
            cumulative += value - PAGE_HINKLEY_DELTA;
            if (cumulative < minimum) minimum = cumulative;
            if (cumulative > maximum) maximum = cumulative;
            pageHinkleyPositive = Math.max(pageHinkleyPositive, cumulative - minimum);
            pageHinkleyNegative = Math.max(pageHinkleyNegative, maximum - cumulative);
        }
        positiveCusum /= Math.max(1, z.length);
        negativeCusum /= Math.max(1, z.length);
        pageHinkleyPositive /= Math.max(1, z.length);
        pageHinkleyNegative /= Math.max(1, z.length);
        double cusum = Math.max(positiveCusum, negativeCusum);
        double pageHinkley = Math.max(pageHinkleyPositive, pageHinkleyNegative);
        double energy = energyDistance(current, previous, scale);
        double directionAgreement = directionalAgreement(z);
        double bayesSurprise = 1.0d - Math.exp(-0.5d * surprise);
        double cusumProbability = 1.0d - Math.exp(-cusum);
        double pageProbability = 1.0d - Math.exp(-pageHinkley);
        double energyProbability = 1.0d - Math.exp(-energy);
        double hazardPosterior = hazardPosterior(bayesSurprise, DEFAULT_HAZARD, absolute);
        double probability =
                V51Math.clamp(
                        0.34d * bayesSurprise
                                + 0.20d * cusumProbability
                                + 0.16d * pageProbability
                                + 0.16d * energyProbability
                                + 0.14d * hazardPosterior,
                        0.0d,
                        1.0d);
        double confidence =
                V51Math.clamp(
                        0.55d * Math.min(1.0d, absolute / 2.0d)
                                + 0.25d * directionAgreement
                                + 0.20d * (1.0d - validation.missingRate),
                        0.0d,
                        1.0d);
        String state =
                probability >= BREAK_PROBABILITY
                        ? "BREAK"
                        : (probability >= WARN_PROBABILITY ? "WATCH" : "STABLE");
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|"
                                + vectorText(current)
                                + "|"
                                + vectorText(previous)
                                + "|"
                                + vectorText(scale)
                                + "|"
                                + vectorText(z)
                                + "|"
                                + probability
                                + "|"
                                + confidence
                                + "|"
                                + state);
        return new ChangePointReport(
                true,
                probability,
                confidence,
                surprise,
                absolute,
                positiveCusum,
                negativeCusum,
                pageHinkleyPositive,
                pageHinkleyNegative,
                energy,
                directionAgreement,
                bayesSurprise,
                hazardPosterior,
                state,
                z,
                Math.max(0L, System.currentTimeMillis() - started),
                checksum,
                "");
    }

    public SeriesReport evaluateSeries(double[][] trajectory, double[] scale) {
        long started = System.currentTimeMillis();
        if (trajectory == null || trajectory.length < 2) return SeriesReport.error("SERIES_LT_2");
        ArrayList<ChangePointReport> transitions = new ArrayList<ChangePointReport>();
        double max = 0.0d;
        double average = 0.0d;
        int breakIndex = -1;
        int watchCount = 0;
        for (int i = 1; i < trajectory.length; i++) {
            ChangePointReport report = evaluate(trajectory[i], trajectory[i - 1], scale);
            transitions.add(report);
            if (!report.valid) return SeriesReport.error("TRANSITION_" + i + "_" + report.detail);
            average += report.probability;
            if (report.probability > max) {
                max = report.probability;
                breakIndex = i;
            }
            if (report.probability >= WARN_PROBABILITY) watchCount++;
        }
        average /= Math.max(1, transitions.size());
        double persistence = persistence(transitions);
        double recent = transitions.get(transitions.size() - 1).probability;
        double acceleration =
                transitions.size() < 2
                        ? 0.0d
                        : recent - transitions.get(transitions.size() - 2).probability;
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < transitions.size(); i++)
            canonical.append(transitions.get(i).checksum).append(';');
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|SERIES|"
                                + canonical
                                + "|"
                                + average
                                + "|"
                                + max
                                + "|"
                                + persistence
                                + "|"
                                + recent
                                + "|"
                                + acceleration);
        return new SeriesReport(
                true,
                Collections.unmodifiableList(transitions),
                average,
                max,
                recent,
                acceleration,
                persistence,
                breakIndex,
                watchCount,
                Math.max(0L, System.currentTimeMillis() - started),
                checksum,
                "");
    }

    public ReplayAudit replay(double[] current, double[] previous, double[] scale) {
        ChangePointReport first = evaluate(copy(current), copy(previous), copy(scale));
        ChangePointReport second = evaluate(copy(current), copy(previous), copy(scale));
        boolean same =
                first.valid
                        && second.valid
                        && first.checksum.equals(second.checksum)
                        && Math.abs(first.probability - second.probability) <= 1.0e-12d;
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|REPLAY|"
                                + first.checksum
                                + "|"
                                + second.checksum
                                + "|"
                                + (same ? 1 : 0));
        return new ReplayAudit(
                same,
                first.checksum,
                second.checksum,
                checksum,
                same ? "" : "CHANGE_POINT_REPLAY_MISMATCH");
    }

    public Validation validate(double[] current, double[] previous, double[] scale) {
        ArrayList<String> errors = new ArrayList<String>();
        int missing = 0;
        if (current == null || previous == null) errors.add("VECTOR_NULL");
        else if (current.length != previous.length) errors.add("VECTOR_LENGTH_MISMATCH");
        else if (current.length != EXPECTED_DIMENSION)
            errors.add("VECTOR_DIMENSION_" + current.length);
        if (scale == null || scale.length < EXPECTED_DIMENSION) errors.add("SCALE_DIMENSION");
        if (current != null && previous != null && current.length == previous.length) {
            for (int i = 0; i < current.length; i++) {
                if (Double.isNaN(current[i])
                        || Double.isInfinite(current[i])
                        || Double.isNaN(previous[i])
                        || Double.isInfinite(previous[i])) missing++;
            }
        }
        if (scale != null) {
            for (int i = 0; i < Math.min(EXPECTED_DIMENSION, scale.length); i++) {
                if (Double.isNaN(scale[i])
                        || Double.isInfinite(scale[i])
                        || Math.abs(scale[i]) < MIN_SCALE) errors.add("SCALE_INVALID_" + i);
            }
        }
        double missingRate = ((double) missing) / Math.max(1, EXPECTED_DIMENSION);
        if (missingRate > 0.34d) errors.add("MISSING_RATE_HIGH");
        String detail = join(errors);
        String checksum =
                V51ChecksumEngine.sha256(B05_VERSION + "|VALIDATE|" + detail + "|" + missingRate);
        return new Validation(errors.isEmpty(), missing, missingRate, detail, checksum);
    }

    public static String schemaHash() {
        return V51ChecksumEngine.sha256(
                B05_VERSION
                        + "|"
                        + EXPECTED_DIMENSION
                        + "|"
                        + DEFAULT_HAZARD
                        + "|"
                        + CUSUM_REFERENCE
                        + "|"
                        + PAGE_HINKLEY_DELTA
                        + "|"
                        + rulesHash());
    }

    public static List<ContractRule> contractRules() {
        ArrayList<ContractRule> rules = new ArrayList<ContractRule>();
        rules.add(
                new ContractRule(
                        "B05-CP-001", "V51ChangePointEngine", "PIT_CHANGE_POINT_001", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-002", "V51ChangePointEngine", "PIT_CHANGE_POINT_002", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-003", "V51ChangePointEngine", "PIT_CHANGE_POINT_003", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-004", "V51ChangePointEngine", "PIT_CHANGE_POINT_004", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-005", "V51ChangePointEngine", "PIT_CHANGE_POINT_005", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-006", "V51ChangePointEngine", "PIT_CHANGE_POINT_006", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-007", "V51ChangePointEngine", "PIT_CHANGE_POINT_007", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-008", "V51ChangePointEngine", "PIT_CHANGE_POINT_008", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-009", "V51ChangePointEngine", "PIT_CHANGE_POINT_009", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-010", "V51ChangePointEngine", "PIT_CHANGE_POINT_010", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-011", "V51ChangePointEngine", "PIT_CHANGE_POINT_011", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-012", "V51ChangePointEngine", "PIT_CHANGE_POINT_012", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-013", "V51ChangePointEngine", "PIT_CHANGE_POINT_013", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-014", "V51ChangePointEngine", "PIT_CHANGE_POINT_014", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-015", "V51ChangePointEngine", "PIT_CHANGE_POINT_015", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-016", "V51ChangePointEngine", "PIT_CHANGE_POINT_016", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-017", "V51ChangePointEngine", "PIT_CHANGE_POINT_017", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-018", "V51ChangePointEngine", "PIT_CHANGE_POINT_018", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-019", "V51ChangePointEngine", "PIT_CHANGE_POINT_019", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-020", "V51ChangePointEngine", "PIT_CHANGE_POINT_020", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-021", "V51ChangePointEngine", "PIT_CHANGE_POINT_021", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-022", "V51ChangePointEngine", "PIT_CHANGE_POINT_022", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-023", "V51ChangePointEngine", "PIT_CHANGE_POINT_023", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-024", "V51ChangePointEngine", "PIT_CHANGE_POINT_024", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-025", "V51ChangePointEngine", "PIT_CHANGE_POINT_025", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-026", "V51ChangePointEngine", "PIT_CHANGE_POINT_026", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-027", "V51ChangePointEngine", "PIT_CHANGE_POINT_027", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-028", "V51ChangePointEngine", "PIT_CHANGE_POINT_028", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-029", "V51ChangePointEngine", "PIT_CHANGE_POINT_029", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-030", "V51ChangePointEngine", "PIT_CHANGE_POINT_030", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-031", "V51ChangePointEngine", "PIT_CHANGE_POINT_031", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-032", "V51ChangePointEngine", "PIT_CHANGE_POINT_032", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-033", "V51ChangePointEngine", "PIT_CHANGE_POINT_033", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-034", "V51ChangePointEngine", "PIT_CHANGE_POINT_034", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-035", "V51ChangePointEngine", "PIT_CHANGE_POINT_035", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-036", "V51ChangePointEngine", "PIT_CHANGE_POINT_036", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-037", "V51ChangePointEngine", "PIT_CHANGE_POINT_037", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-038", "V51ChangePointEngine", "PIT_CHANGE_POINT_038", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-039", "V51ChangePointEngine", "PIT_CHANGE_POINT_039", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-040", "V51ChangePointEngine", "PIT_CHANGE_POINT_040", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-041", "V51ChangePointEngine", "PIT_CHANGE_POINT_041", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-042", "V51ChangePointEngine", "PIT_CHANGE_POINT_042", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-043", "V51ChangePointEngine", "PIT_CHANGE_POINT_043", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-044", "V51ChangePointEngine", "PIT_CHANGE_POINT_044", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-045", "V51ChangePointEngine", "PIT_CHANGE_POINT_045", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-046", "V51ChangePointEngine", "PIT_CHANGE_POINT_046", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-047", "V51ChangePointEngine", "PIT_CHANGE_POINT_047", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-048", "V51ChangePointEngine", "PIT_CHANGE_POINT_048", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-049", "V51ChangePointEngine", "PIT_CHANGE_POINT_049", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-050", "V51ChangePointEngine", "PIT_CHANGE_POINT_050", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-051", "V51ChangePointEngine", "PIT_CHANGE_POINT_051", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-052", "V51ChangePointEngine", "PIT_CHANGE_POINT_052", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-053", "V51ChangePointEngine", "PIT_CHANGE_POINT_053", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-054", "V51ChangePointEngine", "PIT_CHANGE_POINT_054", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-055", "V51ChangePointEngine", "PIT_CHANGE_POINT_055", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-056", "V51ChangePointEngine", "PIT_CHANGE_POINT_056", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-057", "V51ChangePointEngine", "PIT_CHANGE_POINT_057", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-058", "V51ChangePointEngine", "PIT_CHANGE_POINT_058", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-059", "V51ChangePointEngine", "PIT_CHANGE_POINT_059", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-060", "V51ChangePointEngine", "PIT_CHANGE_POINT_060", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-061", "V51ChangePointEngine", "PIT_CHANGE_POINT_061", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-062", "V51ChangePointEngine", "PIT_CHANGE_POINT_062", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-063", "V51ChangePointEngine", "PIT_CHANGE_POINT_063", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-064", "V51ChangePointEngine", "PIT_CHANGE_POINT_064", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-065", "V51ChangePointEngine", "PIT_CHANGE_POINT_065", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-066", "V51ChangePointEngine", "PIT_CHANGE_POINT_066", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-067", "V51ChangePointEngine", "PIT_CHANGE_POINT_067", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-068", "V51ChangePointEngine", "PIT_CHANGE_POINT_068", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-069", "V51ChangePointEngine", "PIT_CHANGE_POINT_069", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-070", "V51ChangePointEngine", "PIT_CHANGE_POINT_070", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-071", "V51ChangePointEngine", "PIT_CHANGE_POINT_071", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-072", "V51ChangePointEngine", "PIT_CHANGE_POINT_072", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-073", "V51ChangePointEngine", "PIT_CHANGE_POINT_073", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-074", "V51ChangePointEngine", "PIT_CHANGE_POINT_074", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-075", "V51ChangePointEngine", "PIT_CHANGE_POINT_075", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-076", "V51ChangePointEngine", "PIT_CHANGE_POINT_076", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-077", "V51ChangePointEngine", "PIT_CHANGE_POINT_077", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-078", "V51ChangePointEngine", "PIT_CHANGE_POINT_078", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-079", "V51ChangePointEngine", "PIT_CHANGE_POINT_079", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-080", "V51ChangePointEngine", "PIT_CHANGE_POINT_080", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-081", "V51ChangePointEngine", "PIT_CHANGE_POINT_081", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-082", "V51ChangePointEngine", "PIT_CHANGE_POINT_082", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-083", "V51ChangePointEngine", "PIT_CHANGE_POINT_083", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-084", "V51ChangePointEngine", "PIT_CHANGE_POINT_084", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-085", "V51ChangePointEngine", "PIT_CHANGE_POINT_085", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-086", "V51ChangePointEngine", "PIT_CHANGE_POINT_086", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-087", "V51ChangePointEngine", "PIT_CHANGE_POINT_087", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-088", "V51ChangePointEngine", "PIT_CHANGE_POINT_088", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-089", "V51ChangePointEngine", "PIT_CHANGE_POINT_089", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-090", "V51ChangePointEngine", "PIT_CHANGE_POINT_090", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-091", "V51ChangePointEngine", "PIT_CHANGE_POINT_091", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-092", "V51ChangePointEngine", "PIT_CHANGE_POINT_092", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-093", "V51ChangePointEngine", "PIT_CHANGE_POINT_093", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-094", "V51ChangePointEngine", "PIT_CHANGE_POINT_094", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-095", "V51ChangePointEngine", "PIT_CHANGE_POINT_095", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-096", "V51ChangePointEngine", "PIT_CHANGE_POINT_096", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-097", "V51ChangePointEngine", "PIT_CHANGE_POINT_097", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-098", "V51ChangePointEngine", "PIT_CHANGE_POINT_098", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-099", "V51ChangePointEngine", "PIT_CHANGE_POINT_099", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-100", "V51ChangePointEngine", "PIT_CHANGE_POINT_100", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-101", "V51ChangePointEngine", "PIT_CHANGE_POINT_101", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-102", "V51ChangePointEngine", "PIT_CHANGE_POINT_102", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-103", "V51ChangePointEngine", "PIT_CHANGE_POINT_103", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-104", "V51ChangePointEngine", "PIT_CHANGE_POINT_104", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-105", "V51ChangePointEngine", "PIT_CHANGE_POINT_105", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-106", "V51ChangePointEngine", "PIT_CHANGE_POINT_106", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-107", "V51ChangePointEngine", "PIT_CHANGE_POINT_107", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-108", "V51ChangePointEngine", "PIT_CHANGE_POINT_108", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-109", "V51ChangePointEngine", "PIT_CHANGE_POINT_109", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-110", "V51ChangePointEngine", "PIT_CHANGE_POINT_110", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-111", "V51ChangePointEngine", "PIT_CHANGE_POINT_111", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-112", "V51ChangePointEngine", "PIT_CHANGE_POINT_112", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-113", "V51ChangePointEngine", "PIT_CHANGE_POINT_113", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-114", "V51ChangePointEngine", "PIT_CHANGE_POINT_114", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-115", "V51ChangePointEngine", "PIT_CHANGE_POINT_115", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-116", "V51ChangePointEngine", "PIT_CHANGE_POINT_116", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-117", "V51ChangePointEngine", "PIT_CHANGE_POINT_117", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-118", "V51ChangePointEngine", "PIT_CHANGE_POINT_118", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-119", "V51ChangePointEngine", "PIT_CHANGE_POINT_119", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-120", "V51ChangePointEngine", "PIT_CHANGE_POINT_120", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-121", "V51ChangePointEngine", "PIT_CHANGE_POINT_121", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-122", "V51ChangePointEngine", "PIT_CHANGE_POINT_122", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-123", "V51ChangePointEngine", "PIT_CHANGE_POINT_123", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-124", "V51ChangePointEngine", "PIT_CHANGE_POINT_124", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-125", "V51ChangePointEngine", "PIT_CHANGE_POINT_125", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-126", "V51ChangePointEngine", "PIT_CHANGE_POINT_126", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-127", "V51ChangePointEngine", "PIT_CHANGE_POINT_127", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-128", "V51ChangePointEngine", "PIT_CHANGE_POINT_128", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-129", "V51ChangePointEngine", "PIT_CHANGE_POINT_129", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-130", "V51ChangePointEngine", "PIT_CHANGE_POINT_130", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-131", "V51ChangePointEngine", "PIT_CHANGE_POINT_131", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-132", "V51ChangePointEngine", "PIT_CHANGE_POINT_132", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-133", "V51ChangePointEngine", "PIT_CHANGE_POINT_133", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-134", "V51ChangePointEngine", "PIT_CHANGE_POINT_134", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-135", "V51ChangePointEngine", "PIT_CHANGE_POINT_135", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-136", "V51ChangePointEngine", "PIT_CHANGE_POINT_136", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-137", "V51ChangePointEngine", "PIT_CHANGE_POINT_137", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-138", "V51ChangePointEngine", "PIT_CHANGE_POINT_138", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-139", "V51ChangePointEngine", "PIT_CHANGE_POINT_139", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-140", "V51ChangePointEngine", "PIT_CHANGE_POINT_140", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-141", "V51ChangePointEngine", "PIT_CHANGE_POINT_141", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-142", "V51ChangePointEngine", "PIT_CHANGE_POINT_142", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-143", "V51ChangePointEngine", "PIT_CHANGE_POINT_143", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-144", "V51ChangePointEngine", "PIT_CHANGE_POINT_144", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-145", "V51ChangePointEngine", "PIT_CHANGE_POINT_145", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-146", "V51ChangePointEngine", "PIT_CHANGE_POINT_146", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-147", "V51ChangePointEngine", "PIT_CHANGE_POINT_147", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-148", "V51ChangePointEngine", "PIT_CHANGE_POINT_148", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-149", "V51ChangePointEngine", "PIT_CHANGE_POINT_149", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-150", "V51ChangePointEngine", "PIT_CHANGE_POINT_150", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-151", "V51ChangePointEngine", "PIT_CHANGE_POINT_151", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-152", "V51ChangePointEngine", "PIT_CHANGE_POINT_152", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-153", "V51ChangePointEngine", "PIT_CHANGE_POINT_153", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-154", "V51ChangePointEngine", "PIT_CHANGE_POINT_154", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-155", "V51ChangePointEngine", "PIT_CHANGE_POINT_155", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-156", "V51ChangePointEngine", "PIT_CHANGE_POINT_156", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-157", "V51ChangePointEngine", "PIT_CHANGE_POINT_157", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-158", "V51ChangePointEngine", "PIT_CHANGE_POINT_158", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-159", "V51ChangePointEngine", "PIT_CHANGE_POINT_159", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-160", "V51ChangePointEngine", "PIT_CHANGE_POINT_160", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-161", "V51ChangePointEngine", "PIT_CHANGE_POINT_161", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-162", "V51ChangePointEngine", "PIT_CHANGE_POINT_162", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-163", "V51ChangePointEngine", "PIT_CHANGE_POINT_163", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-164", "V51ChangePointEngine", "PIT_CHANGE_POINT_164", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-165", "V51ChangePointEngine", "PIT_CHANGE_POINT_165", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-166", "V51ChangePointEngine", "PIT_CHANGE_POINT_166", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-167", "V51ChangePointEngine", "PIT_CHANGE_POINT_167", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-168", "V51ChangePointEngine", "PIT_CHANGE_POINT_168", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-169", "V51ChangePointEngine", "PIT_CHANGE_POINT_169", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-170", "V51ChangePointEngine", "PIT_CHANGE_POINT_170", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-171", "V51ChangePointEngine", "PIT_CHANGE_POINT_171", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-172", "V51ChangePointEngine", "PIT_CHANGE_POINT_172", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-173", "V51ChangePointEngine", "PIT_CHANGE_POINT_173", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-174", "V51ChangePointEngine", "PIT_CHANGE_POINT_174", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-175", "V51ChangePointEngine", "PIT_CHANGE_POINT_175", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-176", "V51ChangePointEngine", "PIT_CHANGE_POINT_176", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-177", "V51ChangePointEngine", "PIT_CHANGE_POINT_177", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-178", "V51ChangePointEngine", "PIT_CHANGE_POINT_178", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-179", "V51ChangePointEngine", "PIT_CHANGE_POINT_179", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-180", "V51ChangePointEngine", "PIT_CHANGE_POINT_180", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-181", "V51ChangePointEngine", "PIT_CHANGE_POINT_181", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-182", "V51ChangePointEngine", "PIT_CHANGE_POINT_182", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-183", "V51ChangePointEngine", "PIT_CHANGE_POINT_183", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-184", "V51ChangePointEngine", "PIT_CHANGE_POINT_184", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-185", "V51ChangePointEngine", "PIT_CHANGE_POINT_185", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-186", "V51ChangePointEngine", "PIT_CHANGE_POINT_186", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-187", "V51ChangePointEngine", "PIT_CHANGE_POINT_187", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-188", "V51ChangePointEngine", "PIT_CHANGE_POINT_188", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-189", "V51ChangePointEngine", "PIT_CHANGE_POINT_189", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-190", "V51ChangePointEngine", "PIT_CHANGE_POINT_190", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-191", "V51ChangePointEngine", "PIT_CHANGE_POINT_191", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-192", "V51ChangePointEngine", "PIT_CHANGE_POINT_192", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-193", "V51ChangePointEngine", "PIT_CHANGE_POINT_193", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-194", "V51ChangePointEngine", "PIT_CHANGE_POINT_194", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-195", "V51ChangePointEngine", "PIT_CHANGE_POINT_195", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-196", "V51ChangePointEngine", "PIT_CHANGE_POINT_196", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-197", "V51ChangePointEngine", "PIT_CHANGE_POINT_197", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-198", "V51ChangePointEngine", "PIT_CHANGE_POINT_198", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-199", "V51ChangePointEngine", "PIT_CHANGE_POINT_199", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-200", "V51ChangePointEngine", "PIT_CHANGE_POINT_200", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-201", "V51ChangePointEngine", "PIT_CHANGE_POINT_201", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-202", "V51ChangePointEngine", "PIT_CHANGE_POINT_202", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-203", "V51ChangePointEngine", "PIT_CHANGE_POINT_203", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-204", "V51ChangePointEngine", "PIT_CHANGE_POINT_204", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-205", "V51ChangePointEngine", "PIT_CHANGE_POINT_205", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-206", "V51ChangePointEngine", "PIT_CHANGE_POINT_206", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-207", "V51ChangePointEngine", "PIT_CHANGE_POINT_207", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-208", "V51ChangePointEngine", "PIT_CHANGE_POINT_208", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-209", "V51ChangePointEngine", "PIT_CHANGE_POINT_209", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-210", "V51ChangePointEngine", "PIT_CHANGE_POINT_210", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-211", "V51ChangePointEngine", "PIT_CHANGE_POINT_211", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-212", "V51ChangePointEngine", "PIT_CHANGE_POINT_212", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-213", "V51ChangePointEngine", "PIT_CHANGE_POINT_213", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-214", "V51ChangePointEngine", "PIT_CHANGE_POINT_214", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-215", "V51ChangePointEngine", "PIT_CHANGE_POINT_215", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-216", "V51ChangePointEngine", "PIT_CHANGE_POINT_216", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-217", "V51ChangePointEngine", "PIT_CHANGE_POINT_217", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-218", "V51ChangePointEngine", "PIT_CHANGE_POINT_218", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-219", "V51ChangePointEngine", "PIT_CHANGE_POINT_219", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-220", "V51ChangePointEngine", "PIT_CHANGE_POINT_220", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-221", "V51ChangePointEngine", "PIT_CHANGE_POINT_221", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-222", "V51ChangePointEngine", "PIT_CHANGE_POINT_222", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-223", "V51ChangePointEngine", "PIT_CHANGE_POINT_223", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-224", "V51ChangePointEngine", "PIT_CHANGE_POINT_224", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-225", "V51ChangePointEngine", "PIT_CHANGE_POINT_225", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-226", "V51ChangePointEngine", "PIT_CHANGE_POINT_226", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-227", "V51ChangePointEngine", "PIT_CHANGE_POINT_227", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-228", "V51ChangePointEngine", "PIT_CHANGE_POINT_228", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-229", "V51ChangePointEngine", "PIT_CHANGE_POINT_229", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-230", "V51ChangePointEngine", "PIT_CHANGE_POINT_230", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-231", "V51ChangePointEngine", "PIT_CHANGE_POINT_231", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-232", "V51ChangePointEngine", "PIT_CHANGE_POINT_232", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-233", "V51ChangePointEngine", "PIT_CHANGE_POINT_233", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-234", "V51ChangePointEngine", "PIT_CHANGE_POINT_234", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-235", "V51ChangePointEngine", "PIT_CHANGE_POINT_235", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-236", "V51ChangePointEngine", "PIT_CHANGE_POINT_236", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-237", "V51ChangePointEngine", "PIT_CHANGE_POINT_237", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-238", "V51ChangePointEngine", "PIT_CHANGE_POINT_238", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-239", "V51ChangePointEngine", "PIT_CHANGE_POINT_239", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-240", "V51ChangePointEngine", "PIT_CHANGE_POINT_240", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-241", "V51ChangePointEngine", "PIT_CHANGE_POINT_241", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-242", "V51ChangePointEngine", "PIT_CHANGE_POINT_242", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-243", "V51ChangePointEngine", "PIT_CHANGE_POINT_243", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-244", "V51ChangePointEngine", "PIT_CHANGE_POINT_244", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-245", "V51ChangePointEngine", "PIT_CHANGE_POINT_245", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-246", "V51ChangePointEngine", "PIT_CHANGE_POINT_246", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-247", "V51ChangePointEngine", "PIT_CHANGE_POINT_247", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-248", "V51ChangePointEngine", "PIT_CHANGE_POINT_248", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-249", "V51ChangePointEngine", "PIT_CHANGE_POINT_249", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-250", "V51ChangePointEngine", "PIT_CHANGE_POINT_250", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-251", "V51ChangePointEngine", "PIT_CHANGE_POINT_251", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-252", "V51ChangePointEngine", "PIT_CHANGE_POINT_252", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-253", "V51ChangePointEngine", "PIT_CHANGE_POINT_253", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-254", "V51ChangePointEngine", "PIT_CHANGE_POINT_254", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-255", "V51ChangePointEngine", "PIT_CHANGE_POINT_255", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-256", "V51ChangePointEngine", "PIT_CHANGE_POINT_256", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-257", "V51ChangePointEngine", "PIT_CHANGE_POINT_257", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-258", "V51ChangePointEngine", "PIT_CHANGE_POINT_258", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-259", "V51ChangePointEngine", "PIT_CHANGE_POINT_259", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-260", "V51ChangePointEngine", "PIT_CHANGE_POINT_260", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-261", "V51ChangePointEngine", "PIT_CHANGE_POINT_261", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-262", "V51ChangePointEngine", "PIT_CHANGE_POINT_262", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-263", "V51ChangePointEngine", "PIT_CHANGE_POINT_263", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-264", "V51ChangePointEngine", "PIT_CHANGE_POINT_264", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-265", "V51ChangePointEngine", "PIT_CHANGE_POINT_265", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-266", "V51ChangePointEngine", "PIT_CHANGE_POINT_266", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-267", "V51ChangePointEngine", "PIT_CHANGE_POINT_267", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-268", "V51ChangePointEngine", "PIT_CHANGE_POINT_268", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-269", "V51ChangePointEngine", "PIT_CHANGE_POINT_269", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-270", "V51ChangePointEngine", "PIT_CHANGE_POINT_270", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-271", "V51ChangePointEngine", "PIT_CHANGE_POINT_271", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-272", "V51ChangePointEngine", "PIT_CHANGE_POINT_272", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-273", "V51ChangePointEngine", "PIT_CHANGE_POINT_273", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-274", "V51ChangePointEngine", "PIT_CHANGE_POINT_274", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-275", "V51ChangePointEngine", "PIT_CHANGE_POINT_275", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-276", "V51ChangePointEngine", "PIT_CHANGE_POINT_276", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-277", "V51ChangePointEngine", "PIT_CHANGE_POINT_277", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-278", "V51ChangePointEngine", "PIT_CHANGE_POINT_278", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-279", "V51ChangePointEngine", "PIT_CHANGE_POINT_279", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-280", "V51ChangePointEngine", "PIT_CHANGE_POINT_280", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-281", "V51ChangePointEngine", "PIT_CHANGE_POINT_281", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-282", "V51ChangePointEngine", "PIT_CHANGE_POINT_282", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-283", "V51ChangePointEngine", "PIT_CHANGE_POINT_283", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-284", "V51ChangePointEngine", "PIT_CHANGE_POINT_284", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-285", "V51ChangePointEngine", "PIT_CHANGE_POINT_285", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-286", "V51ChangePointEngine", "PIT_CHANGE_POINT_286", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-287", "V51ChangePointEngine", "PIT_CHANGE_POINT_287", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-288", "V51ChangePointEngine", "PIT_CHANGE_POINT_288", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-289", "V51ChangePointEngine", "PIT_CHANGE_POINT_289", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-290", "V51ChangePointEngine", "PIT_CHANGE_POINT_290", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-291", "V51ChangePointEngine", "PIT_CHANGE_POINT_291", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-292", "V51ChangePointEngine", "PIT_CHANGE_POINT_292", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-293", "V51ChangePointEngine", "PIT_CHANGE_POINT_293", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-294", "V51ChangePointEngine", "PIT_CHANGE_POINT_294", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-295", "V51ChangePointEngine", "PIT_CHANGE_POINT_295", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-296", "V51ChangePointEngine", "PIT_CHANGE_POINT_296", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-297", "V51ChangePointEngine", "PIT_CHANGE_POINT_297", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-298", "V51ChangePointEngine", "PIT_CHANGE_POINT_298", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-299", "V51ChangePointEngine", "PIT_CHANGE_POINT_299", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-300", "V51ChangePointEngine", "PIT_CHANGE_POINT_300", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-301", "V51ChangePointEngine", "PIT_CHANGE_POINT_301", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-302", "V51ChangePointEngine", "PIT_CHANGE_POINT_302", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-303", "V51ChangePointEngine", "PIT_CHANGE_POINT_303", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-304", "V51ChangePointEngine", "PIT_CHANGE_POINT_304", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-305", "V51ChangePointEngine", "PIT_CHANGE_POINT_305", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-306", "V51ChangePointEngine", "PIT_CHANGE_POINT_306", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-307", "V51ChangePointEngine", "PIT_CHANGE_POINT_307", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-308", "V51ChangePointEngine", "PIT_CHANGE_POINT_308", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-309", "V51ChangePointEngine", "PIT_CHANGE_POINT_309", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-310", "V51ChangePointEngine", "PIT_CHANGE_POINT_310", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-311", "V51ChangePointEngine", "PIT_CHANGE_POINT_311", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-312", "V51ChangePointEngine", "PIT_CHANGE_POINT_312", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-313", "V51ChangePointEngine", "PIT_CHANGE_POINT_313", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-314", "V51ChangePointEngine", "PIT_CHANGE_POINT_314", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-315", "V51ChangePointEngine", "PIT_CHANGE_POINT_315", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-316", "V51ChangePointEngine", "PIT_CHANGE_POINT_316", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-317", "V51ChangePointEngine", "PIT_CHANGE_POINT_317", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-318", "V51ChangePointEngine", "PIT_CHANGE_POINT_318", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-319", "V51ChangePointEngine", "PIT_CHANGE_POINT_319", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-320", "V51ChangePointEngine", "PIT_CHANGE_POINT_320", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-321", "V51ChangePointEngine", "PIT_CHANGE_POINT_321", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-322", "V51ChangePointEngine", "PIT_CHANGE_POINT_322", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-323", "V51ChangePointEngine", "PIT_CHANGE_POINT_323", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-324", "V51ChangePointEngine", "PIT_CHANGE_POINT_324", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-325", "V51ChangePointEngine", "PIT_CHANGE_POINT_325", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-326", "V51ChangePointEngine", "PIT_CHANGE_POINT_326", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-327", "V51ChangePointEngine", "PIT_CHANGE_POINT_327", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-328", "V51ChangePointEngine", "PIT_CHANGE_POINT_328", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-329", "V51ChangePointEngine", "PIT_CHANGE_POINT_329", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-330", "V51ChangePointEngine", "PIT_CHANGE_POINT_330", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-331", "V51ChangePointEngine", "PIT_CHANGE_POINT_331", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-332", "V51ChangePointEngine", "PIT_CHANGE_POINT_332", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-333", "V51ChangePointEngine", "PIT_CHANGE_POINT_333", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-334", "V51ChangePointEngine", "PIT_CHANGE_POINT_334", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-335", "V51ChangePointEngine", "PIT_CHANGE_POINT_335", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-336", "V51ChangePointEngine", "PIT_CHANGE_POINT_336", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-337", "V51ChangePointEngine", "PIT_CHANGE_POINT_337", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-338", "V51ChangePointEngine", "PIT_CHANGE_POINT_338", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-339", "V51ChangePointEngine", "PIT_CHANGE_POINT_339", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-340", "V51ChangePointEngine", "PIT_CHANGE_POINT_340", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-341", "V51ChangePointEngine", "PIT_CHANGE_POINT_341", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-342", "V51ChangePointEngine", "PIT_CHANGE_POINT_342", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-343", "V51ChangePointEngine", "PIT_CHANGE_POINT_343", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-344", "V51ChangePointEngine", "PIT_CHANGE_POINT_344", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-345", "V51ChangePointEngine", "PIT_CHANGE_POINT_345", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-346", "V51ChangePointEngine", "PIT_CHANGE_POINT_346", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-347", "V51ChangePointEngine", "PIT_CHANGE_POINT_347", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-348", "V51ChangePointEngine", "PIT_CHANGE_POINT_348", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-349", "V51ChangePointEngine", "PIT_CHANGE_POINT_349", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-350", "V51ChangePointEngine", "PIT_CHANGE_POINT_350", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-351", "V51ChangePointEngine", "PIT_CHANGE_POINT_351", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-352", "V51ChangePointEngine", "PIT_CHANGE_POINT_352", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-353", "V51ChangePointEngine", "PIT_CHANGE_POINT_353", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-354", "V51ChangePointEngine", "PIT_CHANGE_POINT_354", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-355", "V51ChangePointEngine", "PIT_CHANGE_POINT_355", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-356", "V51ChangePointEngine", "PIT_CHANGE_POINT_356", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-357", "V51ChangePointEngine", "PIT_CHANGE_POINT_357", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-358", "V51ChangePointEngine", "PIT_CHANGE_POINT_358", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-359", "V51ChangePointEngine", "PIT_CHANGE_POINT_359", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-360", "V51ChangePointEngine", "PIT_CHANGE_POINT_360", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-361", "V51ChangePointEngine", "PIT_CHANGE_POINT_361", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-362", "V51ChangePointEngine", "PIT_CHANGE_POINT_362", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-363", "V51ChangePointEngine", "PIT_CHANGE_POINT_363", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-364", "V51ChangePointEngine", "PIT_CHANGE_POINT_364", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-365", "V51ChangePointEngine", "PIT_CHANGE_POINT_365", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-366", "V51ChangePointEngine", "PIT_CHANGE_POINT_366", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-367", "V51ChangePointEngine", "PIT_CHANGE_POINT_367", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-368", "V51ChangePointEngine", "PIT_CHANGE_POINT_368", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-369", "V51ChangePointEngine", "PIT_CHANGE_POINT_369", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-370", "V51ChangePointEngine", "PIT_CHANGE_POINT_370", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-371", "V51ChangePointEngine", "PIT_CHANGE_POINT_371", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-372", "V51ChangePointEngine", "PIT_CHANGE_POINT_372", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-373", "V51ChangePointEngine", "PIT_CHANGE_POINT_373", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-374", "V51ChangePointEngine", "PIT_CHANGE_POINT_374", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-375", "V51ChangePointEngine", "PIT_CHANGE_POINT_375", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-376", "V51ChangePointEngine", "PIT_CHANGE_POINT_376", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-377", "V51ChangePointEngine", "PIT_CHANGE_POINT_377", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-378", "V51ChangePointEngine", "PIT_CHANGE_POINT_378", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-379", "V51ChangePointEngine", "PIT_CHANGE_POINT_379", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-380", "V51ChangePointEngine", "PIT_CHANGE_POINT_380", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-381", "V51ChangePointEngine", "PIT_CHANGE_POINT_381", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-382", "V51ChangePointEngine", "PIT_CHANGE_POINT_382", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-383", "V51ChangePointEngine", "PIT_CHANGE_POINT_383", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-384", "V51ChangePointEngine", "PIT_CHANGE_POINT_384", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-385", "V51ChangePointEngine", "PIT_CHANGE_POINT_385", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-386", "V51ChangePointEngine", "PIT_CHANGE_POINT_386", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-387", "V51ChangePointEngine", "PIT_CHANGE_POINT_387", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-388", "V51ChangePointEngine", "PIT_CHANGE_POINT_388", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-389", "V51ChangePointEngine", "PIT_CHANGE_POINT_389", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-390", "V51ChangePointEngine", "PIT_CHANGE_POINT_390", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-391", "V51ChangePointEngine", "PIT_CHANGE_POINT_391", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-392", "V51ChangePointEngine", "PIT_CHANGE_POINT_392", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-393", "V51ChangePointEngine", "PIT_CHANGE_POINT_393", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-394", "V51ChangePointEngine", "PIT_CHANGE_POINT_394", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-395", "V51ChangePointEngine", "PIT_CHANGE_POINT_395", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-396", "V51ChangePointEngine", "PIT_CHANGE_POINT_396", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-397", "V51ChangePointEngine", "PIT_CHANGE_POINT_397", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-398", "V51ChangePointEngine", "PIT_CHANGE_POINT_398", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-399", "V51ChangePointEngine", "PIT_CHANGE_POINT_399", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-400", "V51ChangePointEngine", "PIT_CHANGE_POINT_400", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-401", "V51ChangePointEngine", "PIT_CHANGE_POINT_401", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-402", "V51ChangePointEngine", "PIT_CHANGE_POINT_402", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-403", "V51ChangePointEngine", "PIT_CHANGE_POINT_403", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-404", "V51ChangePointEngine", "PIT_CHANGE_POINT_404", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-405", "V51ChangePointEngine", "PIT_CHANGE_POINT_405", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-406", "V51ChangePointEngine", "PIT_CHANGE_POINT_406", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-407", "V51ChangePointEngine", "PIT_CHANGE_POINT_407", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-408", "V51ChangePointEngine", "PIT_CHANGE_POINT_408", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-409", "V51ChangePointEngine", "PIT_CHANGE_POINT_409", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-410", "V51ChangePointEngine", "PIT_CHANGE_POINT_410", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-411", "V51ChangePointEngine", "PIT_CHANGE_POINT_411", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-412", "V51ChangePointEngine", "PIT_CHANGE_POINT_412", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-413", "V51ChangePointEngine", "PIT_CHANGE_POINT_413", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-414", "V51ChangePointEngine", "PIT_CHANGE_POINT_414", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-415", "V51ChangePointEngine", "PIT_CHANGE_POINT_415", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-416", "V51ChangePointEngine", "PIT_CHANGE_POINT_416", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-417", "V51ChangePointEngine", "PIT_CHANGE_POINT_417", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-418", "V51ChangePointEngine", "PIT_CHANGE_POINT_418", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-419", "V51ChangePointEngine", "PIT_CHANGE_POINT_419", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-420", "V51ChangePointEngine", "PIT_CHANGE_POINT_420", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-421", "V51ChangePointEngine", "PIT_CHANGE_POINT_421", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-422", "V51ChangePointEngine", "PIT_CHANGE_POINT_422", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-423", "V51ChangePointEngine", "PIT_CHANGE_POINT_423", true, 4));
        rules.add(
                new ContractRule(
                        "B05-CP-424", "V51ChangePointEngine", "PIT_CHANGE_POINT_424", true, 5));
        rules.add(
                new ContractRule(
                        "B05-CP-425", "V51ChangePointEngine", "PIT_CHANGE_POINT_425", true, 6));
        rules.add(
                new ContractRule(
                        "B05-CP-426", "V51ChangePointEngine", "PIT_CHANGE_POINT_426", true, 7));
        rules.add(
                new ContractRule(
                        "B05-CP-427", "V51ChangePointEngine", "PIT_CHANGE_POINT_427", true, 1));
        rules.add(
                new ContractRule(
                        "B05-CP-428", "V51ChangePointEngine", "PIT_CHANGE_POINT_428", true, 2));
        rules.add(
                new ContractRule(
                        "B05-CP-429", "V51ChangePointEngine", "PIT_CHANGE_POINT_429", true, 3));
        rules.add(
                new ContractRule(
                        "B05-CP-430", "V51ChangePointEngine", "PIT_CHANGE_POINT_430", true, 4));
        return Collections.unmodifiableList(rules);
    }

    public static String rulesHash() {
        StringBuilder out = new StringBuilder();
        List<ContractRule> rules = contractRules();
        for (int i = 0; i < rules.size(); i++) out.append(rules.get(i).canonical()).append(';');
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static double[] standardizedDelta(double[] current, double[] previous, double[] scale) {
        double[] out = new double[EXPECTED_DIMENSION];
        for (int i = 0; i < out.length; i++) {
            double s = Math.max(MIN_SCALE, Math.abs(V51Math.finite(scale[i])));
            out[i] = (V51Math.finite(current[i]) - V51Math.finite(previous[i])) / s;
        }
        return out;
    }

    private static double meanSquareClipped(double[] values, double maximum) {
        if (values == null || values.length == 0) return 0.0d;
        double sum = 0.0d;
        for (int i = 0; i < values.length; i++) {
            double value = V51Math.finite(values[i]);
            sum += Math.min(maximum, value * value);
        }
        return sum / values.length;
    }

    private static double meanAbsolute(double[] values) {
        if (values == null || values.length == 0) return 0.0d;
        double sum = 0.0d;
        for (int i = 0; i < values.length; i++) sum += Math.abs(V51Math.finite(values[i]));
        return sum / values.length;
    }

    private static double energyDistance(double[] current, double[] previous, double[] scale) {
        double distance = 0.0d;
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            double s = Math.max(MIN_SCALE, Math.abs(V51Math.finite(scale[i])));
            distance += Math.abs(V51Math.finite(current[i]) - V51Math.finite(previous[i])) / s;
        }
        return distance / EXPECTED_DIMENSION;
    }

    private static double directionalAgreement(double[] z) {
        if (z == null || z.length == 0) return 0.0d;
        int positive = 0;
        int negative = 0;
        for (int i = 0; i < z.length; i++) {
            if (z[i] > 0.05d) positive++;
            else if (z[i] < -0.05d) negative++;
        }
        return ((double) Math.max(positive, negative)) / Math.max(1, positive + negative);
    }

    private static double hazardPosterior(double surprise, double hazard, double magnitude) {
        surprise = V51Math.clamp(surprise, 0.0d, 1.0d);
        hazard = V51Math.clamp(hazard, 1.0e-6d, 0.50d);
        double likelihoodBreak = 0.15d + 0.85d * surprise;
        double likelihoodStable = Math.max(1.0e-6d, 1.0d - 0.75d * surprise);
        double numerator = hazard * likelihoodBreak * (1.0d + Math.min(3.0d, magnitude) * 0.08d);
        double denominator = numerator + (1.0d - hazard) * likelihoodStable;
        return denominator <= 0.0d ? 1.0d : V51Math.clamp(numerator / denominator, 0.0d, 1.0d);
    }

    private static double persistence(List<ChangePointReport> reports) {
        if (reports == null || reports.size() < 2) return 0.0d;
        int sameDirection = 0;
        int compared = 0;
        double previous = reports.get(0).probability;
        for (int i = 1; i < reports.size(); i++) {
            double current = reports.get(i).probability;
            if ((current - previous) >= 0.0d) sameDirection++;
            compared++;
            previous = current;
        }
        return ((double) sameDirection) / Math.max(1, compared);
    }

    private static double[] copy(double[] value) {
        return V51Math.copy(value);
    }

    private static String vectorText(double[] values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (int i = 0; i < values.length; i++)
                out.append(Long.toHexString(Double.doubleToLongBits(V51Math.finite(values[i]))))
                        .append(',');
        }
        return out.toString();
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) out.append(',');
                out.append(values.get(i));
            }
        }
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class ChangePointReport {
        public final boolean valid;
        public final double probability;
        public final double confidence;
        public final double surprise;
        public final double meanAbsoluteDelta;
        public final double positiveCusum;
        public final double negativeCusum;
        public final double pageHinkleyPositive;
        public final double pageHinkleyNegative;
        public final double energyDistance;
        public final double directionalAgreement;
        public final double bayesSurprise;
        public final double hazardPosterior;
        public final String state;
        public final double[] standardizedDelta;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        ChangePointReport(
                boolean valid,
                double probability,
                double confidence,
                double surprise,
                double meanAbsoluteDelta,
                double positiveCusum,
                double negativeCusum,
                double pageHinkleyPositive,
                double pageHinkleyNegative,
                double energyDistance,
                double directionalAgreement,
                double bayesSurprise,
                double hazardPosterior,
                String state,
                double[] standardizedDelta,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.probability = V51Math.clamp(probability, 0.0d, 1.0d);
            this.confidence = V51Math.clamp(confidence, 0.0d, 1.0d);
            this.surprise = Math.max(0.0d, V51Math.finite(surprise));
            this.meanAbsoluteDelta = Math.max(0.0d, V51Math.finite(meanAbsoluteDelta));
            this.positiveCusum = Math.max(0.0d, V51Math.finite(positiveCusum));
            this.negativeCusum = Math.max(0.0d, V51Math.finite(negativeCusum));
            this.pageHinkleyPositive = Math.max(0.0d, V51Math.finite(pageHinkleyPositive));
            this.pageHinkleyNegative = Math.max(0.0d, V51Math.finite(pageHinkleyNegative));
            this.energyDistance = Math.max(0.0d, V51Math.finite(energyDistance));
            this.directionalAgreement = V51Math.clamp(directionalAgreement, 0.0d, 1.0d);
            this.bayesSurprise = V51Math.clamp(bayesSurprise, 0.0d, 1.0d);
            this.hazardPosterior = V51Math.clamp(hazardPosterior, 0.0d, 1.0d);
            this.state = safe(state);
            this.standardizedDelta = V51Math.copy(standardizedDelta);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static ChangePointReport error(String detail) {
            return new ChangePointReport(
                    false,
                    1.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    1.0d,
                    "ERROR",
                    new double[0],
                    0L,
                    V51ChecksumEngine.sha256(B05_VERSION + "|ERROR|" + safe(detail)),
                    detail);
        }

        public String summary() {
            return "valid="
                    + (valid ? 1 : 0)
                    + "|p="
                    + probability
                    + "|confidence="
                    + confidence
                    + "|surprise="
                    + surprise
                    + "|cusum="
                    + Math.max(positiveCusum, negativeCusum)
                    + "|page="
                    + Math.max(pageHinkleyPositive, pageHinkleyNegative)
                    + "|energy="
                    + energyDistance
                    + "|state="
                    + state
                    + "|cost="
                    + elapsedMs
                    + "ms|detail="
                    + detail;
        }
    }

    public static final class SeriesReport {
        public final boolean valid;
        public final List<ChangePointReport> transitions;
        public final double averageProbability;
        public final double maximumProbability;
        public final double recentProbability;
        public final double acceleration;
        public final double persistence;
        public final int maximumIndex;
        public final int watchCount;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        SeriesReport(
                boolean valid,
                List<ChangePointReport> transitions,
                double averageProbability,
                double maximumProbability,
                double recentProbability,
                double acceleration,
                double persistence,
                int maximumIndex,
                int watchCount,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.transitions =
                    transitions == null ? Collections.<ChangePointReport>emptyList() : transitions;
            this.averageProbability = V51Math.clamp(averageProbability, 0.0d, 1.0d);
            this.maximumProbability = V51Math.clamp(maximumProbability, 0.0d, 1.0d);
            this.recentProbability = V51Math.clamp(recentProbability, 0.0d, 1.0d);
            this.acceleration = V51Math.finite(acceleration);
            this.persistence = V51Math.clamp(persistence, 0.0d, 1.0d);
            this.maximumIndex = maximumIndex;
            this.watchCount = Math.max(0, watchCount);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static SeriesReport error(String detail) {
            return new SeriesReport(
                    false,
                    Collections.<ChangePointReport>emptyList(),
                    1.0d,
                    1.0d,
                    1.0d,
                    0.0d,
                    0.0d,
                    -1,
                    0,
                    0L,
                    V51ChecksumEngine.sha256(B05_VERSION + "|SERIES_ERROR|" + safe(detail)),
                    detail);
        }
    }

    public static final class Validation {
        public final boolean valid;
        public final int missingCount;
        public final double missingRate;
        public final String detail;
        public final String checksum;

        Validation(
                boolean valid,
                int missingCount,
                double missingRate,
                String detail,
                String checksum) {
            this.valid = valid;
            this.missingCount = Math.max(0, missingCount);
            this.missingRate = V51Math.clamp(missingRate, 0.0d, 1.0d);
            this.detail = safe(detail);
            this.checksum = safe(checksum);
        }
    }

    public static final class ReplayAudit {
        public final boolean passed;
        public final String firstHash;
        public final String secondHash;
        public final String checksum;
        public final String detail;

        ReplayAudit(
                boolean passed,
                String firstHash,
                String secondHash,
                String checksum,
                String detail) {
            this.passed = passed;
            this.firstHash = safe(firstHash);
            this.secondHash = safe(secondHash);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }
    }

    public static final class ContractRule {
        public final String id;
        public final String owner;
        public final String invariant;
        public final boolean mandatory;
        public final int severity;

        ContractRule(String id, String owner, String invariant, boolean mandatory, int severity) {
            this.id = safe(id);
            this.owner = safe(owner);
            this.invariant = safe(invariant);
            this.mandatory = mandatory;
            this.severity = Math.max(1, severity);
        }

        public String canonical() {
            return id + "|" + owner + "|" + invariant + "|" + (mandatory ? 1 : 0) + "|" + severity;
        }
    }
}
