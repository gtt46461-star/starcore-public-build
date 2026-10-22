package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B05 explicit-duration lifecycle owner.
 *
 * <p>Lifecycle is derived from typed run length, prototype strength, current and previous
 * change-point risk, posterior margin and duration hazard. No string matching is used to authorize
 * production actions.
 */
public final class V51LifecycleEngine {
    public static final String B05_VERSION = "STARCORE_TIANSHU_B05_LIFECYCLE_V1";
    public static final int MAX_TRACKED_RUN = 96;
    public static final double BREAK_RISK = 0.78d;
    public static final double RECOVERY_PREVIOUS_RISK = 0.70d;
    public static final double RECOVERY_CURRENT_MAX = 0.45d;
    public static final double MIN_STRENGTH = 0.35d;
    public static final double MATURE_STRENGTH = 0.55d;

    /** Backward-compatible compact classification API. */
    public V51Enums.Lifecycle classify(
            int runLength, double strength, double changeRisk, double previousChangeRisk) {
        return evaluate(runLength, strength, changeRisk, previousChangeRisk, 0.10d, 0.50d)
                .lifecycle;
    }

    public double remainingLife(int runLength, double changeRisk) {
        return evaluate(runLength, 0.50d, changeRisk, changeRisk, 0.10d, 0.50d).remainingLife;
    }

    public LifecycleReport evaluate(
            int runLength,
            double strength,
            double changeRisk,
            double previousChangeRisk,
            double posteriorMargin,
            double changeConfidence) {
        long started = System.currentTimeMillis();
        runLength = Math.max(1, Math.min(MAX_TRACKED_RUN, runLength));
        strength = V51Math.clamp(strength, 0.0d, 1.0d);
        changeRisk = V51Math.clamp(changeRisk, 0.0d, 1.0d);
        previousChangeRisk = V51Math.clamp(previousChangeRisk, 0.0d, 1.0d);
        posteriorMargin = V51Math.clamp(posteriorMargin, 0.0d, 1.0d);
        changeConfidence = V51Math.clamp(changeConfidence, 0.0d, 1.0d);

        double[] posterior =
                lifecyclePosterior(
                        runLength,
                        strength,
                        changeRisk,
                        previousChangeRisk,
                        posteriorMargin,
                        changeConfidence);
        int best = 0;
        int second = posterior.length > 1 ? 1 : 0;
        for (int i = 0; i < posterior.length; i++) {
            if (posterior[i] > posterior[best]) best = i;
        }
        second = best == 0 && posterior.length > 1 ? 1 : 0;
        for (int i = 0; i < posterior.length; i++) {
            if (i != best && posterior[i] > posterior[second]) second = i;
        }
        V51Enums.Lifecycle lifecycle = lifecycleAt(best);
        double margin =
                Math.max(0.0d, posterior[best] - (second == best ? 0.0d : posterior[second]));
        double entropy = V51Math.entropy(posterior);
        double hazard = durationHazard(lifecycle, runLength, changeRisk, strength);
        double remaining =
                remainingLifeEstimate(lifecycle, runLength, hazard, changeRisk, strength);
        double transitionConfidence =
                V51Math.clamp(
                        0.35d * posterior[best]
                                + 0.25d * margin
                                + 0.20d * (1.0d - entropy)
                                + 0.20d * changeConfidence,
                        0.0d,
                        1.0d);
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|"
                                + runLength
                                + "|"
                                + strength
                                + "|"
                                + changeRisk
                                + "|"
                                + previousChangeRisk
                                + "|"
                                + posteriorMargin
                                + "|"
                                + changeConfidence
                                + "|"
                                + vectorText(posterior)
                                + "|"
                                + lifecycle.name()
                                + "|"
                                + remaining
                                + "|"
                                + hazard);
        return new LifecycleReport(
                true,
                lifecycle,
                runLength,
                strength,
                changeRisk,
                previousChangeRisk,
                posteriorMargin,
                changeConfidence,
                posterior,
                posterior[best],
                margin,
                entropy,
                hazard,
                remaining,
                transitionConfidence,
                Math.max(0L, System.currentTimeMillis() - started),
                checksum,
                "");
    }

    public TransitionReport transition(LifecycleReport previous, LifecycleReport current) {
        if (previous == null || current == null || !previous.valid || !current.valid)
            return TransitionReport.error("LIFECYCLE_REPORT_INVALID");
        boolean allowed = allowedTransition(previous.lifecycle, current.lifecycle);
        double riskShift = current.changeRisk - previous.changeRisk;
        double strengthShift = current.strength - previous.strength;
        double posteriorShift = l1(previous.posterior, current.posterior);
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|TRANSITION|"
                                + previous.checksum
                                + "|"
                                + current.checksum
                                + "|"
                                + (allowed ? 1 : 0)
                                + "|"
                                + riskShift
                                + "|"
                                + strengthShift
                                + "|"
                                + posteriorShift);
        return new TransitionReport(
                allowed,
                previous.lifecycle,
                current.lifecycle,
                riskShift,
                strengthShift,
                posteriorShift,
                checksum,
                allowed ? "" : "TRANSITION_NOT_ALLOWED");
    }

    public ReplayAudit replay(
            int runLength,
            double strength,
            double changeRisk,
            double previousChangeRisk,
            double posteriorMargin,
            double changeConfidence) {
        LifecycleReport first =
                evaluate(
                        runLength,
                        strength,
                        changeRisk,
                        previousChangeRisk,
                        posteriorMargin,
                        changeConfidence);
        LifecycleReport second =
                evaluate(
                        runLength,
                        strength,
                        changeRisk,
                        previousChangeRisk,
                        posteriorMargin,
                        changeConfidence);
        boolean same =
                first.valid
                        && second.valid
                        && first.checksum.equals(second.checksum)
                        && first.lifecycle == second.lifecycle
                        && Math.abs(first.remainingLife - second.remainingLife) <= 1.0e-12d;
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
                same ? "" : "LIFECYCLE_REPLAY_MISMATCH");
    }

    public static String schemaHash() {
        return V51ChecksumEngine.sha256(
                B05_VERSION
                        + "|"
                        + MAX_TRACKED_RUN
                        + "|"
                        + BREAK_RISK
                        + "|"
                        + RECOVERY_PREVIOUS_RISK
                        + "|"
                        + RECOVERY_CURRENT_MAX
                        + "|"
                        + rulesHash());
    }

    public static List<ContractRule> contractRules() {
        ArrayList<ContractRule> rules = new ArrayList<ContractRule>();
        rules.add(
                new ContractRule(
                        "B05-LIFE-001",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_001",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-002",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_002",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-003",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_003",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-004",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_004",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-005",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_005",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-006",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_006",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-007",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_007",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-008",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_008",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-009",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_009",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-010",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_010",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-011",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_011",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-012",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_012",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-013",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_013",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-014",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_014",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-015",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_015",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-016",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_016",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-017",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_017",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-018",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_018",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-019",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_019",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-020",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_020",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-021",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_021",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-022",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_022",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-023",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_023",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-024",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_024",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-025",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_025",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-026",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_026",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-027",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_027",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-028",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_028",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-029",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_029",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-030",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_030",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-031",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_031",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-032",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_032",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-033",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_033",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-034",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_034",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-035",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_035",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-036",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_036",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-037",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_037",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-038",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_038",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-039",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_039",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-040",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_040",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-041",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_041",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-042",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_042",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-043",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_043",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-044",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_044",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-045",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_045",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-046",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_046",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-047",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_047",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-048",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_048",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-049",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_049",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-050",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_050",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-051",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_051",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-052",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_052",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-053",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_053",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-054",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_054",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-055",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_055",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-056",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_056",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-057",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_057",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-058",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_058",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-059",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_059",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-060",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_060",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-061",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_061",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-062",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_062",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-063",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_063",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-064",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_064",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-065",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_065",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-066",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_066",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-067",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_067",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-068",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_068",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-069",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_069",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-070",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_070",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-071",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_071",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-072",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_072",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-073",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_073",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-074",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_074",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-075",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_075",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-076",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_076",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-077",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_077",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-078",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_078",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-079",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_079",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-080",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_080",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-081",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_081",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-082",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_082",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-083",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_083",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-084",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_084",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-085",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_085",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-086",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_086",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-087",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_087",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-088",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_088",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-089",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_089",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-090",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_090",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-091",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_091",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-092",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_092",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-093",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_093",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-094",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_094",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-095",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_095",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-096",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_096",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-097",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_097",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-098",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_098",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-099",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_099",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-100",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_100",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-101",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_101",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-102",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_102",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-103",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_103",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-104",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_104",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-105",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_105",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-106",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_106",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-107",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_107",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-108",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_108",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-109",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_109",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-110",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_110",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-111",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_111",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-112",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_112",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-113",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_113",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-114",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_114",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-115",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_115",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-116",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_116",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-117",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_117",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-118",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_118",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-119",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_119",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-120",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_120",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-121",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_121",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-122",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_122",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-123",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_123",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-124",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_124",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-125",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_125",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-126",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_126",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-127",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_127",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-128",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_128",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-129",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_129",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-130",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_130",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-131",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_131",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-132",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_132",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-133",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_133",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-134",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_134",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-135",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_135",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-136",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_136",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-137",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_137",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-138",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_138",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-139",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_139",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-140",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_140",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-141",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_141",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-142",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_142",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-143",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_143",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-144",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_144",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-145",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_145",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-146",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_146",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-147",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_147",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-148",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_148",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-149",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_149",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-150",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_150",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-151",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_151",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-152",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_152",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-153",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_153",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-154",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_154",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-155",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_155",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-156",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_156",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-157",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_157",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-158",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_158",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-159",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_159",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-160",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_160",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-161",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_161",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-162",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_162",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-163",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_163",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-164",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_164",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-165",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_165",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-166",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_166",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-167",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_167",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-168",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_168",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-169",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_169",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-170",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_170",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-171",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_171",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-172",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_172",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-173",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_173",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-174",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_174",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-175",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_175",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-176",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_176",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-177",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_177",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-178",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_178",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-179",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_179",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-180",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_180",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-181",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_181",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-182",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_182",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-183",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_183",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-184",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_184",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-185",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_185",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-186",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_186",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-187",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_187",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-188",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_188",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-189",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_189",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-190",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_190",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-191",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_191",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-192",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_192",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-193",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_193",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-194",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_194",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-195",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_195",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-196",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_196",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-197",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_197",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-198",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_198",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-199",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_199",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-200",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_200",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-201",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_201",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-202",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_202",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-203",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_203",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-204",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_204",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-205",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_205",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-206",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_206",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-207",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_207",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-208",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_208",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-209",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_209",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-210",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_210",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-211",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_211",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-212",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_212",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-213",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_213",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-214",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_214",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-215",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_215",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-216",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_216",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-217",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_217",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-218",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_218",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-219",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_219",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-220",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_220",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-221",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_221",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-222",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_222",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-223",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_223",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-224",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_224",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-225",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_225",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-226",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_226",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-227",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_227",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-228",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_228",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-229",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_229",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-230",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_230",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-231",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_231",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-232",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_232",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-233",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_233",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-234",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_234",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-235",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_235",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-236",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_236",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-237",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_237",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-238",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_238",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-239",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_239",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-240",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_240",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-241",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_241",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-242",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_242",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-243",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_243",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-244",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_244",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-245",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_245",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-246",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_246",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-247",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_247",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-248",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_248",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-249",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_249",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-250",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_250",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-251",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_251",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-252",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_252",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-253",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_253",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-254",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_254",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-255",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_255",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-256",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_256",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-257",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_257",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-258",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_258",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-259",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_259",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-260",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_260",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-261",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_261",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-262",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_262",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-263",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_263",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-264",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_264",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-265",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_265",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-266",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_266",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-267",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_267",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-268",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_268",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-269",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_269",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-270",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_270",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-271",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_271",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-272",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_272",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-273",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_273",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-274",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_274",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-275",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_275",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-276",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_276",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-277",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_277",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-278",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_278",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-279",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_279",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-280",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_280",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-281",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_281",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-282",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_282",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-283",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_283",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-284",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_284",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-285",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_285",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-286",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_286",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-287",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_287",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-288",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_288",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-289",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_289",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-290",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_290",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-291",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_291",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-292",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_292",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-293",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_293",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-294",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_294",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-295",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_295",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-296",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_296",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-297",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_297",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-298",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_298",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-299",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_299",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-300",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_300",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-301",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_301",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-302",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_302",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-303",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_303",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-304",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_304",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-305",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_305",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-306",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_306",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-307",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_307",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-308",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_308",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-309",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_309",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-310",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_310",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-311",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_311",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-312",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_312",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-313",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_313",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-314",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_314",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-315",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_315",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-316",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_316",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-317",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_317",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-318",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_318",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-319",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_319",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-320",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_320",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-321",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_321",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-322",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_322",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-323",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_323",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-324",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_324",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-325",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_325",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-326",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_326",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-327",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_327",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-328",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_328",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-329",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_329",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-330",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_330",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-331",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_331",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-332",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_332",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-333",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_333",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-334",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_334",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-335",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_335",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-336",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_336",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-337",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_337",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-338",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_338",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-339",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_339",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-340",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_340",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-341",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_341",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-342",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_342",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-343",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_343",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-344",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_344",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-345",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_345",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-346",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_346",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-347",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_347",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-348",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_348",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-349",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_349",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-350",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_350",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-351",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_351",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-352",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_352",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-353",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_353",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-354",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_354",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-355",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_355",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-356",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_356",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-357",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_357",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-358",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_358",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-359",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_359",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-360",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_360",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-361",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_361",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-362",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_362",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-363",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_363",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-364",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_364",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-365",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_365",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-366",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_366",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-367",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_367",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-368",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_368",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-369",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_369",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-370",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_370",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-371",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_371",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-372",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_372",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-373",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_373",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-374",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_374",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-375",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_375",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-376",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_376",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-377",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_377",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-378",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_378",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-379",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_379",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-380",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_380",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-381",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_381",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-382",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_382",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-383",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_383",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-384",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_384",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-385",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_385",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-386",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_386",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-387",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_387",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-388",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_388",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-389",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_389",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-390",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_390",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-391",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_391",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-392",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_392",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-393",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_393",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-394",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_394",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-395",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_395",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-396",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_396",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-397",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_397",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-398",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_398",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-399",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_399",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-400",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_400",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-401",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_401",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-402",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_402",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-403",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_403",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-404",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_404",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-405",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_405",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-406",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_406",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-407",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_407",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-408",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_408",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-409",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_409",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-410",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_410",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-411",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_411",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-412",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_412",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-413",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_413",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-414",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_414",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-415",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_415",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-416",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_416",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-417",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_417",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-418",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_418",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-419",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_419",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-420",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_420",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-421",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_421",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-422",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_422",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-423",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_423",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-424",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_424",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-425",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_425",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-426",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_426",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-427",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_427",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-428",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_428",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-429",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_429",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-430",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_430",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-431",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_431",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-432",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_432",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-433",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_433",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-434",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_434",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-435",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_435",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-436",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_436",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-437",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_437",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-438",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_438",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-439",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_439",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-440",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_440",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-441",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_441",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-442",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_442",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-443",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_443",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-444",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_444",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-445",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_445",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-446",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_446",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-447",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_447",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-448",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_448",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-LIFE-449",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_449",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-LIFE-450",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_450",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-LIFE-451",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_451",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-LIFE-452",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_452",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-LIFE-453",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_453",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-LIFE-454",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_454",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-LIFE-455",
                        "V51LifecycleEngine",
                        "EXPLICIT_DURATION_LIFECYCLE_455",
                        true,
                        1));
        return Collections.unmodifiableList(rules);
    }

    public static String rulesHash() {
        StringBuilder out = new StringBuilder();
        List<ContractRule> rules = contractRules();
        for (int i = 0; i < rules.size(); i++) out.append(rules.get(i).canonical()).append(';');
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static double[] lifecyclePosterior(
            int runLength,
            double strength,
            double changeRisk,
            double previousChangeRisk,
            double posteriorMargin,
            double changeConfidence) {
        double[] logit = new double[7];
        double shortRun = 1.0d / (1.0d + runLength / 2.0d);
        double mediumRun = Math.exp(-Math.abs(runLength - 6.0d) / 4.0d);
        double matureRun = Math.exp(-Math.abs(runLength - 12.0d) / 8.0d);
        double longRun = V51Math.clamp((runLength - 10.0d) / 24.0d, 0.0d, 1.0d);
        double recoverySignal =
                previousChangeRisk * (1.0d - changeRisk) * (0.50d + 0.50d * strength);
        logit[0] = 1.80d * shortRun + 0.35d * strength - 1.10d * changeRisk;
        logit[1] =
                1.30d * mediumRun + 1.00d * strength + 0.35d * posteriorMargin - 0.90d * changeRisk;
        logit[2] =
                1.40d * matureRun + 1.30d * strength + 0.45d * posteriorMargin - 1.10d * changeRisk;
        logit[3] = 1.20d * longRun + 0.90d * (1.0d - strength) + 0.60d * changeRisk;
        logit[4] = 2.80d * changeRisk + 0.50d * changeConfidence - 0.55d * strength;
        logit[5] = 2.60d * recoverySignal + 0.30d * posteriorMargin + 0.20d * changeConfidence;
        logit[6] = 0.15d + 0.40d * (1.0d - posteriorMargin) + 0.35d * (1.0d - changeConfidence);
        if (changeRisk >= BREAK_RISK) logit[4] += 3.0d;
        if (previousChangeRisk >= RECOVERY_PREVIOUS_RISK && changeRisk < RECOVERY_CURRENT_MAX)
            logit[5] += 2.6d;
        if (strength < MIN_STRENGTH && runLength > 8) logit[3] += 1.5d;
        if (runLength <= 2 && changeRisk < 0.55d) logit[0] += 1.2d;
        return softmax(logit);
    }

    private static double durationHazard(
            V51Enums.Lifecycle lifecycle, int runLength, double changeRisk, double strength) {
        double base;
        switch (lifecycle) {
            case ENTRY:
                base = 0.28d;
                break;
            case GROW:
                base = 0.16d;
                break;
            case MATURE:
                base = 0.10d;
                break;
            case EXHAUST:
                base = 0.34d;
                break;
            case BREAK:
                base = 0.78d;
                break;
            case RECOVER:
                base = 0.24d;
                break;
            default:
                base = 0.45d;
                break;
        }
        double runPenalty =
                V51Math.clamp(((double) runLength) / MAX_TRACKED_RUN, 0.0d, 1.0d) * 0.18d;
        double risk = changeRisk * 0.42d;
        double strengthDiscount = strength * 0.16d;
        return V51Math.clamp(base + runPenalty + risk - strengthDiscount, 0.02d, 0.98d);
    }

    private static double remainingLifeEstimate(
            V51Enums.Lifecycle lifecycle,
            int runLength,
            double hazard,
            double changeRisk,
            double strength) {
        double expected = (1.0d - hazard) / Math.max(0.02d, hazard);
        double cap;
        switch (lifecycle) {
            case ENTRY:
                cap = 6.0d;
                break;
            case GROW:
                cap = 12.0d;
                break;
            case MATURE:
                cap = 24.0d;
                break;
            case EXHAUST:
                cap = 6.0d;
                break;
            case BREAK:
                cap = 2.0d;
                break;
            case RECOVER:
                cap = 10.0d;
                break;
            default:
                cap = 4.0d;
                break;
        }
        double adjusted = expected * (0.75d + 0.50d * strength) * (1.0d - 0.65d * changeRisk);
        double runLimit = Math.max(0.0d, MAX_TRACKED_RUN - runLength);
        return Math.max(0.0d, Math.min(Math.min(cap, runLimit), adjusted));
    }

    private static boolean allowedTransition(
            V51Enums.Lifecycle previous, V51Enums.Lifecycle current) {
        if (previous == null || current == null) return false;
        if (previous == current) return true;
        switch (previous) {
            case ENTRY:
                return current == V51Enums.Lifecycle.GROW
                        || current == V51Enums.Lifecycle.BREAK
                        || current == V51Enums.Lifecycle.UNKNOWN;
            case GROW:
                return current == V51Enums.Lifecycle.MATURE
                        || current == V51Enums.Lifecycle.EXHAUST
                        || current == V51Enums.Lifecycle.BREAK;
            case MATURE:
                return current == V51Enums.Lifecycle.EXHAUST
                        || current == V51Enums.Lifecycle.BREAK
                        || current == V51Enums.Lifecycle.RECOVER;
            case EXHAUST:
                return current == V51Enums.Lifecycle.BREAK
                        || current == V51Enums.Lifecycle.RECOVER
                        || current == V51Enums.Lifecycle.ENTRY;
            case BREAK:
                return current == V51Enums.Lifecycle.RECOVER
                        || current == V51Enums.Lifecycle.ENTRY
                        || current == V51Enums.Lifecycle.UNKNOWN;
            case RECOVER:
                return current == V51Enums.Lifecycle.ENTRY
                        || current == V51Enums.Lifecycle.GROW
                        || current == V51Enums.Lifecycle.BREAK;
            default:
                return true;
        }
    }

    private static V51Enums.Lifecycle lifecycleAt(int index) {
        switch (index) {
            case 0:
                return V51Enums.Lifecycle.ENTRY;
            case 1:
                return V51Enums.Lifecycle.GROW;
            case 2:
                return V51Enums.Lifecycle.MATURE;
            case 3:
                return V51Enums.Lifecycle.EXHAUST;
            case 4:
                return V51Enums.Lifecycle.BREAK;
            case 5:
                return V51Enums.Lifecycle.RECOVER;
            default:
                return V51Enums.Lifecycle.UNKNOWN;
        }
    }

    private static double[] softmax(double[] logit) {
        double[] out = new double[logit == null ? 0 : logit.length];
        if (out.length == 0) return out;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < logit.length; i++) {
            double value = V51Math.finite(logit[i]);
            if (value > max) max = value;
        }
        double sum = 0.0d;
        for (int i = 0; i < logit.length; i++) {
            out[i] = Math.exp(Math.max(-60.0d, V51Math.finite(logit[i]) - max));
            sum += out[i];
        }
        if (!(sum > 0.0d)) {
            double uniform = 1.0d / out.length;
            for (int i = 0; i < out.length; i++) out[i] = uniform;
            return out;
        }
        for (int i = 0; i < out.length; i++) out[i] /= sum;
        return out;
    }

    private static double l1(double[] left, double[] right) {
        if (left == null || right == null || left.length != right.length) return 1.0d;
        double sum = 0.0d;
        for (int i = 0; i < left.length; i++)
            sum += Math.abs(V51Math.finite(left[i]) - V51Math.finite(right[i]));
        return 0.5d * sum;
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class LifecycleReport {
        public final boolean valid;
        public final V51Enums.Lifecycle lifecycle;
        public final int runLength;
        public final double strength;
        public final double changeRisk;
        public final double previousChangeRisk;
        public final double posteriorMargin;
        public final double changeConfidence;
        public final double[] posterior;
        public final double selectedPosterior;
        public final double margin;
        public final double entropy;
        public final double hazard;
        public final double remainingLife;
        public final double transitionConfidence;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        LifecycleReport(
                boolean valid,
                V51Enums.Lifecycle lifecycle,
                int runLength,
                double strength,
                double changeRisk,
                double previousChangeRisk,
                double posteriorMargin,
                double changeConfidence,
                double[] posterior,
                double selectedPosterior,
                double margin,
                double entropy,
                double hazard,
                double remainingLife,
                double transitionConfidence,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.lifecycle = lifecycle == null ? V51Enums.Lifecycle.UNKNOWN : lifecycle;
            this.runLength = Math.max(0, runLength);
            this.strength = V51Math.clamp(strength, 0.0d, 1.0d);
            this.changeRisk = V51Math.clamp(changeRisk, 0.0d, 1.0d);
            this.previousChangeRisk = V51Math.clamp(previousChangeRisk, 0.0d, 1.0d);
            this.posteriorMargin = V51Math.clamp(posteriorMargin, 0.0d, 1.0d);
            this.changeConfidence = V51Math.clamp(changeConfidence, 0.0d, 1.0d);
            this.posterior = V51Math.copy(posterior);
            this.selectedPosterior = V51Math.clamp(selectedPosterior, 0.0d, 1.0d);
            this.margin = V51Math.clamp(margin, 0.0d, 1.0d);
            this.entropy = V51Math.clamp(entropy, 0.0d, 1.0d);
            this.hazard = V51Math.clamp(hazard, 0.0d, 1.0d);
            this.remainingLife = Math.max(0.0d, V51Math.finite(remainingLife));
            this.transitionConfidence = V51Math.clamp(transitionConfidence, 0.0d, 1.0d);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        public String summary() {
            return "valid="
                    + (valid ? 1 : 0)
                    + "|life="
                    + lifecycle.name()
                    + "|run="
                    + runLength
                    + "|strength="
                    + strength
                    + "|change="
                    + changeRisk
                    + "|posterior="
                    + selectedPosterior
                    + "|margin="
                    + margin
                    + "|hazard="
                    + hazard
                    + "|remaining="
                    + remainingLife
                    + "|cost="
                    + elapsedMs
                    + "ms|detail="
                    + detail;
        }
    }

    public static final class TransitionReport {
        public final boolean allowed;
        public final V51Enums.Lifecycle previous;
        public final V51Enums.Lifecycle current;
        public final double riskShift;
        public final double strengthShift;
        public final double posteriorShift;
        public final String checksum;
        public final String detail;

        TransitionReport(
                boolean allowed,
                V51Enums.Lifecycle previous,
                V51Enums.Lifecycle current,
                double riskShift,
                double strengthShift,
                double posteriorShift,
                String checksum,
                String detail) {
            this.allowed = allowed;
            this.previous = previous == null ? V51Enums.Lifecycle.UNKNOWN : previous;
            this.current = current == null ? V51Enums.Lifecycle.UNKNOWN : current;
            this.riskShift = V51Math.finite(riskShift);
            this.strengthShift = V51Math.finite(strengthShift);
            this.posteriorShift = V51Math.clamp(posteriorShift, 0.0d, 1.0d);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static TransitionReport error(String detail) {
            return new TransitionReport(
                    false,
                    V51Enums.Lifecycle.UNKNOWN,
                    V51Enums.Lifecycle.UNKNOWN,
                    0.0d,
                    0.0d,
                    1.0d,
                    V51ChecksumEngine.sha256(B05_VERSION + "|TRANSITION_ERROR|" + safe(detail)),
                    detail);
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
