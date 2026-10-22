package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B05 point-in-time state trajectory owner.
 *
 * <p>The active V51 runtime receives the same frozen feature snapshot for state construction and
 * deterministic replay. State is represented by typed vectors, prototype posterior, change-point
 * risk, explicit lifecycle and cross-position coherence. This class cannot create probability,
 * lane, BET, settlement or ledger mutation.
 */
public final class V51StateTrajectoryEngine {
    public static final String B05_VERSION = "STARCORE_TIANSHU_B05_STATE_TRAJECTORY_V1";
    public static final int POSITION_COUNT = 10;
    public static final int VECTOR_DIMENSION = V51ObservableStateVectorEngine.DIMENSION;
    public static final int MAX_INFERRED_RUN = 24;
    public static final double HARD_CHANGE_RISK = 0.78d;
    public static final double LOW_CONFIDENCE = 0.28d;

    private final V51ObservableStateVectorEngine vectorEngine =
            new V51ObservableStateVectorEngine();
    private final V51StatePrototypeEngine prototypeEngine = new V51StatePrototypeEngine();
    private final V51ChangePointEngine changePointEngine = new V51ChangePointEngine();
    private final V51LifecycleEngine lifecycleEngine = new V51LifecycleEngine();

    /** Backward-compatible state array used by the runtime. */
    public V51Models.StateTrajectorySnapshot[] build(
            V51Models.PermutationFeatureSnapshot current,
            V51Models.PermutationFeatureSnapshot previous) {
        return buildAudited(current, previous).states;
    }

    public TrajectoryBundle buildAudited(
            V51Models.PermutationFeatureSnapshot current,
            V51Models.PermutationFeatureSnapshot previous) {
        long started = System.currentTimeMillis();
        Validation input = validateInput(current, previous);
        if (!input.valid) return TrajectoryBundle.error(input.detail);
        double[][] now = vectorEngine.build(current);
        double[][] old = previous == null ? neutralVectors() : vectorEngine.build(previous);
        if (now.length != POSITION_COUNT || old.length != POSITION_COUNT)
            return TrajectoryBundle.error("STATE_VECTOR_POSITION_COUNT");
        ArrayList<PositionTrajectory> detail = new ArrayList<PositionTrajectory>();
        V51Models.StateTrajectorySnapshot[] states =
                new V51Models.StateTrajectorySnapshot[POSITION_COUNT];
        int breakCount = 0;
        int recoverCount = 0;
        int lowConfidenceCount = 0;
        double meanChange = 0.0d;
        double meanConfidence = 0.0d;
        for (int p = 0; p < POSITION_COUNT; p++) {
            V51StatePrototypeEngine.PrototypeReport match = prototypeEngine.matchAudited(now[p]);
            V51StatePrototypeEngine.PrototypeReport before = prototypeEngine.matchAudited(old[p]);
            V51ChangePointEngine.ChangePointReport change =
                    changePointEngine.evaluate(now[p], old[p], V51StateModelArtifact.SCALE);
            if (!match.valid || !before.valid || !change.valid)
                return TrajectoryBundle.error("POSITION_" + (p + 1) + "_STATE_COMPONENT_INVALID");
            boolean samePrototype = match.match.id.equals(before.match.id);
            double vectorDistance = normalizedDistance(now[p], old[p]);
            int runLength =
                    inferRunLength(
                            samePrototype,
                            match.match.posterior,
                            match.match.margin,
                            change.probability,
                            vectorDistance);
            double previousRisk =
                    samePrototype
                            ? V51Math.clamp(
                                    change.probability * 0.70d + vectorDistance * 0.10d, 0.0d, 1.0d)
                            : V51Math.clamp(change.probability + 0.20d, 0.0d, 1.0d);
            V51LifecycleEngine.LifecycleReport lifecycle =
                    lifecycleEngine.evaluate(
                            runLength,
                            match.match.posterior,
                            change.probability,
                            previousRisk,
                            match.match.margin,
                            change.confidence);
            if (!lifecycle.valid)
                return TrajectoryBundle.error("POSITION_" + (p + 1) + "_LIFECYCLE_INVALID");
            double coherence =
                    V51Math.clamp(
                            0.35d * match.match.posterior
                                    + 0.25d * match.match.margin
                                    + 0.20d * (1.0d - change.probability)
                                    + 0.20d * (1.0d - Math.min(1.0d, vectorDistance)),
                            0.0d,
                            1.0d);
            String domain =
                    domainId(
                            p + 1,
                            match.match.id,
                            lifecycle.lifecycle,
                            change.probability,
                            coherence);
            String checksum =
                    V51ChecksumEngine.sha256(
                            B05_VERSION
                                    + "|"
                                    + current.context.targetIssue
                                    + "|"
                                    + (p + 1)
                                    + "|"
                                    + match.checksum
                                    + "|"
                                    + before.checksum
                                    + "|"
                                    + change.checksum
                                    + "|"
                                    + lifecycle.checksum
                                    + "|"
                                    + runLength
                                    + "|"
                                    + vectorDistance
                                    + "|"
                                    + coherence
                                    + "|"
                                    + domain);
            V51Models.StateTrajectorySnapshot state =
                    new V51Models.StateTrajectorySnapshot(
                            current.context.targetIssue,
                            p + 1,
                            match.match.id,
                            domain,
                            lifecycle.lifecycle,
                            runLength,
                            match.match.posterior,
                            lifecycle.remainingLife,
                            change.probability,
                            coherence,
                            checksum);
            states[p] = state;
            detail.add(
                    new PositionTrajectory(
                            p + 1,
                            state,
                            match,
                            before,
                            change,
                            lifecycle,
                            vectorDistance,
                            coherence,
                            checksum));
            meanChange += change.probability;
            meanConfidence += coherence;
            if (lifecycle.lifecycle == V51Enums.Lifecycle.BREAK) breakCount++;
            if (lifecycle.lifecycle == V51Enums.Lifecycle.RECOVER) recoverCount++;
            if (coherence < LOW_CONFIDENCE) lowConfidenceCount++;
        }
        meanChange /= POSITION_COUNT;
        meanConfidence /= POSITION_COUNT;
        CrossPositionAudit cross = crossPositionAudit(detail);
        Validation output = validateOutput(states, detail, cross);
        String bundleHash =
                bundleHash(
                        current.context.targetIssue,
                        current.checksum,
                        previous == null ? "" : previous.checksum,
                        detail,
                        cross,
                        output);
        return new TrajectoryBundle(
                output.valid,
                states,
                Collections.unmodifiableList(detail),
                cross,
                breakCount,
                recoverCount,
                lowConfidenceCount,
                meanChange,
                meanConfidence,
                Math.max(0L, System.currentTimeMillis() - started),
                bundleHash,
                output.detail);
    }

    public ReplayAudit replay(
            V51Models.PermutationFeatureSnapshot current,
            V51Models.PermutationFeatureSnapshot previous) {
        TrajectoryBundle first = buildAudited(current, previous);
        TrajectoryBundle second = buildAudited(current, previous);
        boolean same =
                first.valid
                        && second.valid
                        && first.checksum.equals(second.checksum)
                        && stateHash(first.states).equals(stateHash(second.states));
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
                same ? "" : "STATE_TRAJECTORY_REPLAY_MISMATCH");
    }

    public Validation validateInput(
            V51Models.PermutationFeatureSnapshot current,
            V51Models.PermutationFeatureSnapshot previous) {
        ArrayList<String> errors = new ArrayList<String>();
        if (current == null || !current.valid()) errors.add("CURRENT_FEATURE_INVALID");
        if (current != null && current.context == null) errors.add("CURRENT_CONTEXT_NULL");
        if (current != null && current.context != null && current.context.targetIssue.length() == 0)
            errors.add("CURRENT_TARGET_EMPTY");
        if (previous != null && !previous.valid()) errors.add("PREVIOUS_FEATURE_INVALID");
        if (current != null
                && previous != null
                && current.context != null
                && previous.context != null
                && V51PermutationFeatureEngine.compareIssue(
                                previous.context.targetIssue, current.context.targetIssue)
                        >= 0) errors.add("PREVIOUS_TARGET_NOT_BEFORE_CURRENT");
        if (current != null
                && current.globalFeatures.length
                        != V51PermutationFeatureEngine.GLOBAL_FEATURE_COUNT)
            errors.add("CURRENT_GLOBAL_DIMENSION");
        if (previous != null
                && previous.globalFeatures.length
                        != V51PermutationFeatureEngine.GLOBAL_FEATURE_COUNT)
            errors.add("PREVIOUS_GLOBAL_DIMENSION");
        String detail = join(errors);
        return new Validation(
                errors.isEmpty(),
                detail,
                V51ChecksumEngine.sha256(B05_VERSION + "|INPUT|" + detail));
    }

    public Validation validateOutput(
            V51Models.StateTrajectorySnapshot[] states,
            List<PositionTrajectory> detail,
            CrossPositionAudit cross) {
        ArrayList<String> errors = new ArrayList<String>();
        if (states == null || states.length != POSITION_COUNT) errors.add("STATE_COUNT");
        if (detail == null || detail.size() != POSITION_COUNT) errors.add("DETAIL_COUNT");
        if (states != null) {
            boolean[] seen = new boolean[POSITION_COUNT + 1];
            for (int i = 0; i < states.length; i++) {
                V51Models.StateTrajectorySnapshot state = states[i];
                if (state == null) {
                    errors.add("STATE_NULL_" + i);
                    continue;
                }
                if (state.position < 1 || state.position > POSITION_COUNT)
                    errors.add("STATE_POSITION_" + i);
                else if (seen[state.position]) errors.add("STATE_DUPLICATE_" + state.position);
                else seen[state.position] = true;
                if (state.targetIssue.length() == 0) errors.add("STATE_TARGET_EMPTY_" + i);
                if (state.prototypeId.length() == 0) errors.add("STATE_PROTOTYPE_EMPTY_" + i);
                if (state.domainId.length() == 0) errors.add("STATE_DOMAIN_EMPTY_" + i);
                if (state.checksum.length() != 64) errors.add("STATE_HASH_" + i);
                if (state.runLength < 1) errors.add("STATE_RUN_" + i);
                if (Double.isNaN(state.posterior) || Double.isInfinite(state.posterior))
                    errors.add("STATE_POSTERIOR_" + i);
            }
        }
        if (cross == null || !cross.valid) errors.add("CROSS_POSITION_INVALID");
        String detailText = join(errors);
        return new Validation(
                errors.isEmpty(),
                detailText,
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|OUTPUT|"
                                + detailText
                                + "|"
                                + (cross == null ? "" : cross.checksum)));
    }

    public static String schemaHash() {
        return V51ChecksumEngine.sha256(
                B05_VERSION
                        + "|"
                        + POSITION_COUNT
                        + "|"
                        + VECTOR_DIMENSION
                        + "|"
                        + V51StatePrototypeEngine.schemaHash()
                        + "|"
                        + V51ChangePointEngine.schemaHash()
                        + "|"
                        + V51LifecycleEngine.schemaHash()
                        + "|"
                        + rulesHash());
    }

    public static List<ContractRule> contractRules() {
        ArrayList<ContractRule> rules = new ArrayList<ContractRule>();
        rules.add(
                new ContractRule(
                        "B05-TRAJ-001",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_001",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-002",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_002",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-003",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_003",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-004",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_004",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-005",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_005",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-006",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_006",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-007",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_007",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-008",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_008",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-009",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_009",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-010",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_010",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-011",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_011",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-012",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_012",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-013",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_013",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-014",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_014",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-015",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_015",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-016",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_016",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-017",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_017",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-018",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_018",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-019",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_019",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-020",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_020",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-021",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_021",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-022",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_022",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-023",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_023",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-024",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_024",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-025",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_025",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-026",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_026",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-027",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_027",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-028",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_028",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-029",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_029",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-030",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_030",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-031",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_031",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-032",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_032",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-033",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_033",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-034",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_034",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-035",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_035",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-036",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_036",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-037",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_037",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-038",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_038",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-039",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_039",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-040",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_040",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-041",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_041",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-042",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_042",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-043",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_043",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-044",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_044",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-045",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_045",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-046",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_046",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-047",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_047",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-048",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_048",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-049",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_049",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-050",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_050",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-051",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_051",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-052",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_052",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-053",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_053",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-054",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_054",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-055",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_055",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-056",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_056",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-057",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_057",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-058",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_058",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-059",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_059",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-060",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_060",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-061",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_061",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-062",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_062",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-063",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_063",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-064",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_064",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-065",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_065",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-066",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_066",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-067",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_067",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-068",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_068",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-069",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_069",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-070",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_070",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-071",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_071",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-072",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_072",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-073",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_073",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-074",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_074",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-075",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_075",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-076",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_076",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-077",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_077",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-078",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_078",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-079",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_079",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-080",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_080",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-081",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_081",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-082",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_082",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-083",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_083",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-084",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_084",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-085",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_085",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-086",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_086",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-087",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_087",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-088",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_088",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-089",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_089",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-090",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_090",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-091",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_091",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-092",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_092",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-093",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_093",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-094",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_094",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-095",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_095",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-096",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_096",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-097",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_097",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-098",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_098",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-099",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_099",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-100",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_100",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-101",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_101",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-102",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_102",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-103",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_103",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-104",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_104",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-105",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_105",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-106",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_106",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-107",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_107",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-108",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_108",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-109",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_109",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-110",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_110",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-111",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_111",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-112",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_112",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-113",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_113",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-114",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_114",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-115",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_115",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-116",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_116",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-117",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_117",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-118",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_118",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-119",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_119",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-120",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_120",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-121",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_121",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-122",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_122",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-123",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_123",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-124",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_124",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-125",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_125",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-126",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_126",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-127",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_127",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-128",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_128",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-129",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_129",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-130",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_130",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-131",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_131",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-132",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_132",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-133",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_133",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-134",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_134",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-135",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_135",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-136",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_136",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-137",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_137",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-138",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_138",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-139",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_139",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-140",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_140",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-141",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_141",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-142",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_142",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-143",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_143",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-144",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_144",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-145",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_145",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-146",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_146",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-147",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_147",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-148",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_148",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-149",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_149",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-150",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_150",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-151",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_151",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-152",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_152",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-153",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_153",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-154",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_154",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-155",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_155",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-156",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_156",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-157",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_157",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-158",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_158",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-159",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_159",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-160",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_160",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-161",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_161",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-162",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_162",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-163",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_163",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-164",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_164",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-165",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_165",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-166",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_166",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-167",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_167",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-168",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_168",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-169",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_169",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-170",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_170",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-171",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_171",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-172",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_172",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-173",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_173",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-174",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_174",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-175",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_175",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-176",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_176",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-177",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_177",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-178",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_178",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-179",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_179",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-180",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_180",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-181",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_181",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-182",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_182",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-183",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_183",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-184",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_184",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-185",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_185",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-186",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_186",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-187",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_187",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-188",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_188",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-189",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_189",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-190",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_190",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-191",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_191",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-192",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_192",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-193",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_193",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-194",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_194",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-195",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_195",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-196",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_196",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-197",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_197",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-198",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_198",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-199",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_199",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-200",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_200",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-201",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_201",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-202",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_202",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-203",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_203",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-204",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_204",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-205",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_205",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-206",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_206",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-207",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_207",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-208",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_208",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-209",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_209",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-210",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_210",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-211",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_211",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-212",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_212",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-213",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_213",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-214",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_214",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-215",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_215",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-216",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_216",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-217",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_217",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-218",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_218",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-219",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_219",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-220",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_220",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-221",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_221",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-222",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_222",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-223",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_223",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-224",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_224",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-225",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_225",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-226",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_226",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-227",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_227",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-228",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_228",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-229",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_229",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-230",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_230",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-231",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_231",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-232",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_232",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-233",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_233",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-234",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_234",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-235",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_235",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-236",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_236",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-237",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_237",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-238",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_238",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-239",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_239",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-240",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_240",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-241",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_241",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-242",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_242",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-243",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_243",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-244",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_244",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-245",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_245",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-246",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_246",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-247",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_247",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-248",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_248",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-249",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_249",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-250",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_250",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-251",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_251",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-252",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_252",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-253",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_253",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-254",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_254",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-255",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_255",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-256",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_256",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-257",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_257",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-258",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_258",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-259",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_259",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-260",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_260",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-261",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_261",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-262",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_262",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-263",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_263",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-264",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_264",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-265",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_265",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-266",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_266",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-267",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_267",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-268",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_268",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-269",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_269",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-270",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_270",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-271",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_271",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-272",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_272",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-273",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_273",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-274",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_274",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-275",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_275",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-276",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_276",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-277",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_277",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-278",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_278",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-279",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_279",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-280",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_280",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-281",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_281",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-282",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_282",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-283",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_283",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-284",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_284",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-285",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_285",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-286",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_286",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-287",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_287",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-288",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_288",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-289",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_289",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-290",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_290",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-291",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_291",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-292",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_292",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-293",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_293",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-294",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_294",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-295",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_295",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-296",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_296",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-297",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_297",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-298",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_298",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-299",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_299",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-300",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_300",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-301",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_301",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-302",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_302",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-303",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_303",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-304",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_304",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-305",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_305",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-306",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_306",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-307",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_307",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-308",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_308",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-309",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_309",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-310",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_310",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-311",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_311",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-312",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_312",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-313",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_313",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-314",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_314",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-315",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_315",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-316",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_316",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-317",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_317",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-318",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_318",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-319",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_319",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-320",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_320",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-321",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_321",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-322",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_322",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-323",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_323",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-324",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_324",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-325",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_325",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-326",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_326",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-327",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_327",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-328",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_328",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-329",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_329",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-330",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_330",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-331",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_331",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-332",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_332",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-333",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_333",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-334",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_334",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-335",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_335",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-336",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_336",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-337",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_337",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-338",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_338",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-339",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_339",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-340",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_340",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-341",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_341",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-342",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_342",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-343",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_343",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-344",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_344",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-345",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_345",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-346",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_346",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-347",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_347",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-348",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_348",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-349",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_349",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-350",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_350",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-351",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_351",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-352",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_352",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-353",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_353",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-354",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_354",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-355",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_355",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-356",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_356",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-357",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_357",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-358",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_358",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-359",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_359",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-360",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_360",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-361",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_361",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-362",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_362",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-363",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_363",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-364",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_364",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-365",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_365",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-366",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_366",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-367",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_367",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-368",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_368",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-369",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_369",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-370",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_370",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-371",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_371",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-372",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_372",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-373",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_373",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-374",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_374",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-375",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_375",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-376",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_376",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-377",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_377",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-378",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_378",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-379",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_379",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-380",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_380",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-381",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_381",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-382",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_382",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-383",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_383",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-384",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_384",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-385",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_385",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-386",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_386",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-387",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_387",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-388",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_388",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-389",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_389",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-390",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_390",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-391",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_391",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-392",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_392",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-393",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_393",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-394",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_394",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-395",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_395",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-396",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_396",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-397",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_397",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-398",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_398",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-399",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_399",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-400",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_400",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-401",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_401",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-402",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_402",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-403",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_403",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-404",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_404",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-405",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_405",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-406",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_406",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-407",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_407",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-408",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_408",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-409",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_409",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-410",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_410",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-411",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_411",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-412",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_412",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-413",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_413",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-414",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_414",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-415",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_415",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-416",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_416",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-417",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_417",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-418",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_418",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-419",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_419",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-TRAJ-420",
                        "V51StateTrajectoryEngine",
                        "PIT_STATE_TRAJECTORY_420",
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

    private static double[][] neutralVectors() {
        return new double[POSITION_COUNT][VECTOR_DIMENSION];
    }

    private static int inferRunLength(
            boolean samePrototype,
            double posterior,
            double margin,
            double changeRisk,
            double vectorDistance) {
        if (!samePrototype || changeRisk >= HARD_CHANGE_RISK) return 1;
        double persistence =
                V51Math.clamp(
                        0.45d * posterior
                                + 0.20d * margin
                                + 0.25d * (1.0d - changeRisk)
                                + 0.10d * (1.0d - Math.min(1.0d, vectorDistance)),
                        0.0d,
                        1.0d);
        return Math.max(
                2,
                Math.min(
                        MAX_INFERRED_RUN,
                        2 + (int) Math.round(persistence * (MAX_INFERRED_RUN - 2))));
    }

    private static double normalizedDistance(double[] current, double[] previous) {
        if (current == null
                || previous == null
                || current.length != VECTOR_DIMENSION
                || previous.length != VECTOR_DIMENSION) return 1.0d;
        double sum = 0.0d;
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            double scale = Math.max(1.0e-5d, Math.abs(V51StateModelArtifact.SCALE[i]));
            double delta = (V51Math.finite(current[i]) - V51Math.finite(previous[i])) / scale;
            sum += delta * delta;
        }
        return Math.sqrt(sum / VECTOR_DIMENSION);
    }

    private static String domainId(
            int position,
            String prototype,
            V51Enums.Lifecycle lifecycle,
            double changeRisk,
            double coherence) {
        int riskBucket =
                changeRisk >= 0.78d ? 3 : (changeRisk >= 0.55d ? 2 : (changeRisk >= 0.30d ? 1 : 0));
        int confidenceBucket =
                coherence >= 0.70d ? 3 : (coherence >= 0.50d ? 2 : (coherence >= 0.30d ? 1 : 0));
        return "B05_P"
                + position
                + "_"
                + safe(prototype)
                + "_"
                + (lifecycle == null ? V51Enums.Lifecycle.UNKNOWN.name() : lifecycle.name())
                + "_R"
                + riskBucket
                + "_C"
                + confidenceBucket;
    }

    private static CrossPositionAudit crossPositionAudit(List<PositionTrajectory> detail) {
        if (detail == null || detail.size() != POSITION_COUNT)
            return CrossPositionAudit.error("DETAIL_COUNT");
        int breaks = 0;
        int recovery = 0;
        int unknown = 0;
        double meanRisk = 0.0d;
        double varianceRisk = 0.0d;
        double meanCoherence = 0.0d;
        int samePrototypePairs = 0;
        int pairs = 0;
        for (int i = 0; i < detail.size(); i++) {
            PositionTrajectory item = detail.get(i);
            if (item == null || item.state == null)
                return CrossPositionAudit.error("DETAIL_NULL_" + i);
            meanRisk += item.change.probability;
            meanCoherence += item.coherence;
            if (item.state.lifecycle == V51Enums.Lifecycle.BREAK) breaks++;
            if (item.state.lifecycle == V51Enums.Lifecycle.RECOVER) recovery++;
            if (item.state.lifecycle == V51Enums.Lifecycle.UNKNOWN) unknown++;
            for (int j = i + 1; j < detail.size(); j++) {
                pairs++;
                if (item.state.prototypeId.equals(detail.get(j).state.prototypeId))
                    samePrototypePairs++;
            }
        }
        meanRisk /= POSITION_COUNT;
        meanCoherence /= POSITION_COUNT;
        for (int i = 0; i < detail.size(); i++) {
            double delta = detail.get(i).change.probability - meanRisk;
            varianceRisk += delta * delta;
        }
        varianceRisk /= POSITION_COUNT;
        double prototypeConcentration = ((double) samePrototypePairs) / Math.max(1, pairs);
        boolean valid = unknown == 0 && meanCoherence >= 0.0d && meanCoherence <= 1.0d;
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|CROSS|"
                                + breaks
                                + "|"
                                + recovery
                                + "|"
                                + unknown
                                + "|"
                                + meanRisk
                                + "|"
                                + varianceRisk
                                + "|"
                                + meanCoherence
                                + "|"
                                + prototypeConcentration);
        return new CrossPositionAudit(
                valid,
                breaks,
                recovery,
                unknown,
                meanRisk,
                varianceRisk,
                meanCoherence,
                prototypeConcentration,
                checksum,
                valid ? "" : "CROSS_POSITION_INVALID");
    }

    private static String bundleHash(
            String targetIssue,
            String currentHash,
            String previousHash,
            List<PositionTrajectory> detail,
            CrossPositionAudit cross,
            Validation validation) {
        StringBuilder out = new StringBuilder();
        out.append(B05_VERSION)
                .append('|')
                .append(safe(targetIssue))
                .append('|')
                .append(safe(currentHash))
                .append('|')
                .append(safe(previousHash))
                .append('|');
        if (detail != null) {
            for (int i = 0; i < detail.size(); i++) out.append(detail.get(i).checksum).append(';');
        }
        out.append('|').append(cross == null ? "" : cross.checksum);
        out.append('|').append(validation == null ? "" : validation.checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String stateHash(V51Models.StateTrajectorySnapshot[] states) {
        StringBuilder out = new StringBuilder();
        if (states != null) {
            for (int i = 0; i < states.length; i++)
                out.append(states[i] == null ? "NULL" : states[i].checksum).append(';');
        }
        return V51ChecksumEngine.sha256(out.toString());
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

    public static final class TrajectoryBundle {
        public final boolean valid;
        public final V51Models.StateTrajectorySnapshot[] states;
        public final List<PositionTrajectory> positions;
        public final CrossPositionAudit crossPosition;
        public final int breakCount;
        public final int recoverCount;
        public final int lowConfidenceCount;
        public final double meanChangeRisk;
        public final double meanConfidence;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        TrajectoryBundle(
                boolean valid,
                V51Models.StateTrajectorySnapshot[] states,
                List<PositionTrajectory> positions,
                CrossPositionAudit crossPosition,
                int breakCount,
                int recoverCount,
                int lowConfidenceCount,
                double meanChangeRisk,
                double meanConfidence,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.states = copy(states);
            this.positions =
                    positions == null ? Collections.<PositionTrajectory>emptyList() : positions;
            this.crossPosition = crossPosition;
            this.breakCount = Math.max(0, breakCount);
            this.recoverCount = Math.max(0, recoverCount);
            this.lowConfidenceCount = Math.max(0, lowConfidenceCount);
            this.meanChangeRisk = V51Math.clamp(meanChangeRisk, 0.0d, 1.0d);
            this.meanConfidence = V51Math.clamp(meanConfidence, 0.0d, 1.0d);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static TrajectoryBundle error(String detail) {
            return new TrajectoryBundle(
                    false,
                    new V51Models.StateTrajectorySnapshot[0],
                    Collections.<PositionTrajectory>emptyList(),
                    CrossPositionAudit.error(detail),
                    0,
                    0,
                    0,
                    1.0d,
                    0.0d,
                    0L,
                    V51ChecksumEngine.sha256(B05_VERSION + "|ERROR|" + safe(detail)),
                    detail);
        }

        public String summary() {
            return "valid="
                    + (valid ? 1 : 0)
                    + "|states="
                    + states.length
                    + "|break="
                    + breakCount
                    + "|recover="
                    + recoverCount
                    + "|lowConfidence="
                    + lowConfidenceCount
                    + "|meanChange="
                    + meanChangeRisk
                    + "|meanConfidence="
                    + meanConfidence
                    + "|cost="
                    + elapsedMs
                    + "ms|detail="
                    + detail;
        }
    }

    public static final class PositionTrajectory {
        public final int position;
        public final V51Models.StateTrajectorySnapshot state;
        public final V51StatePrototypeEngine.PrototypeReport prototype;
        public final V51StatePrototypeEngine.PrototypeReport previousPrototype;
        public final V51ChangePointEngine.ChangePointReport change;
        public final V51LifecycleEngine.LifecycleReport lifecycle;
        public final double vectorDistance;
        public final double coherence;
        public final String checksum;

        PositionTrajectory(
                int position,
                V51Models.StateTrajectorySnapshot state,
                V51StatePrototypeEngine.PrototypeReport prototype,
                V51StatePrototypeEngine.PrototypeReport previousPrototype,
                V51ChangePointEngine.ChangePointReport change,
                V51LifecycleEngine.LifecycleReport lifecycle,
                double vectorDistance,
                double coherence,
                String checksum) {
            this.position = Math.max(1, position);
            this.state = state;
            this.prototype = prototype;
            this.previousPrototype = previousPrototype;
            this.change = change;
            this.lifecycle = lifecycle;
            this.vectorDistance = Math.max(0.0d, V51Math.finite(vectorDistance));
            this.coherence = V51Math.clamp(coherence, 0.0d, 1.0d);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return "P"
                    + position
                    + "|prototype="
                    + (state == null ? "" : state.prototypeId)
                    + "|lifecycle="
                    + (state == null ? V51Enums.Lifecycle.UNKNOWN.name() : state.lifecycle.name())
                    + "|run="
                    + (state == null ? 0 : state.runLength)
                    + "|change="
                    + (change == null ? 1.0d : change.probability)
                    + "|coherence="
                    + coherence
                    + "|distance="
                    + vectorDistance;
        }
    }

    public static final class CrossPositionAudit {
        public final boolean valid;
        public final int breakCount;
        public final int recoverCount;
        public final int unknownCount;
        public final double meanChangeRisk;
        public final double changeRiskVariance;
        public final double meanCoherence;
        public final double prototypeConcentration;
        public final String checksum;
        public final String detail;

        CrossPositionAudit(
                boolean valid,
                int breakCount,
                int recoverCount,
                int unknownCount,
                double meanChangeRisk,
                double changeRiskVariance,
                double meanCoherence,
                double prototypeConcentration,
                String checksum,
                String detail) {
            this.valid = valid;
            this.breakCount = Math.max(0, breakCount);
            this.recoverCount = Math.max(0, recoverCount);
            this.unknownCount = Math.max(0, unknownCount);
            this.meanChangeRisk = V51Math.clamp(meanChangeRisk, 0.0d, 1.0d);
            this.changeRiskVariance = Math.max(0.0d, V51Math.finite(changeRiskVariance));
            this.meanCoherence = V51Math.clamp(meanCoherence, 0.0d, 1.0d);
            this.prototypeConcentration = V51Math.clamp(prototypeConcentration, 0.0d, 1.0d);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static CrossPositionAudit error(String detail) {
            return new CrossPositionAudit(
                    false,
                    0,
                    0,
                    POSITION_COUNT,
                    1.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    V51ChecksumEngine.sha256(B05_VERSION + "|CROSS_ERROR|" + safe(detail)),
                    detail);
        }
    }

    public static final class Validation {
        public final boolean valid;
        public final String detail;
        public final String checksum;

        Validation(boolean valid, String detail, String checksum) {
            this.valid = valid;
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

    private static V51Models.StateTrajectorySnapshot[] copy(
            V51Models.StateTrajectorySnapshot[] values) {
        if (values == null) return new V51Models.StateTrajectorySnapshot[0];
        V51Models.StateTrajectorySnapshot[] out =
                new V51Models.StateTrajectorySnapshot[values.length];
        System.arraycopy(values, 0, out, 0, values.length);
        return out;
    }
}
