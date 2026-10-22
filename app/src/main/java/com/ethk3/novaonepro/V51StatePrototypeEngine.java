package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B05 deterministic prototype posterior owner.
 *
 * <p>This engine maps the frozen 14-dimensional observable state vector to the generated
 * state-prototype artifact. It never owns probability calibration, action ranking, lane selection,
 * BET authorization, settlement or ledger mutation. Untrained artifacts remain engineering evidence
 * only.
 */
public final class V51StatePrototypeEngine {
    public static final String B05_VERSION = "STARCORE_TIANSHU_B05_STATE_PROTOTYPE_V1";
    public static final int EXPECTED_DIMENSION = V51ObservableStateVectorEngine.DIMENSION;
    public static final double MIN_SCALE = 1.0e-5d;
    public static final double POSTERIOR_FLOOR = 1.0e-15d;
    public static final double DEFAULT_TEMPERATURE = 1.0d;
    public static final double OOD_DISTANCE_WARN = 5.50d;
    public static final double OOD_DISTANCE_HARD = 8.00d;
    public static final double LOW_MARGIN = 0.08d;

    /** Backward-compatible compact result used by the active V51 runtime. */
    public static final class Match {
        public final String id;
        public final double posterior;
        public final double distance;
        public final double runnerUpPosterior;
        public final double margin;
        public final double entropy;
        public final boolean outOfDistribution;
        public final int missingCount;
        public final String checksum;

        Match(String id, double posterior, double distance) {
            this(
                    id,
                    posterior,
                    distance,
                    0.0d,
                    posterior,
                    1.0d,
                    true,
                    0,
                    V51ChecksumEngine.sha256(
                            B05_VERSION + "|" + safe(id) + "|" + posterior + "|" + distance));
        }

        Match(
                String id,
                double posterior,
                double distance,
                double runnerUpPosterior,
                double margin,
                double entropy,
                boolean outOfDistribution,
                int missingCount,
                String checksum) {
            this.id = safe(id);
            this.posterior = V51Math.clamp(posterior, 0.0d, 1.0d);
            this.distance = Math.max(0.0d, V51Math.finite(distance));
            this.runnerUpPosterior = V51Math.clamp(runnerUpPosterior, 0.0d, 1.0d);
            this.margin = V51Math.clamp(margin, 0.0d, 1.0d);
            this.entropy = V51Math.clamp(entropy, 0.0d, 1.0d);
            this.outOfDistribution = outOfDistribution;
            this.missingCount = Math.max(0, missingCount);
            this.checksum = safe(checksum);
        }

        public String canonical() {
            return id
                    + "|"
                    + posterior
                    + "|"
                    + distance
                    + "|"
                    + runnerUpPosterior
                    + "|"
                    + margin
                    + "|"
                    + entropy
                    + "|"
                    + (outOfDistribution ? 1 : 0)
                    + "|"
                    + missingCount
                    + "|"
                    + checksum;
        }
    }

    public Match match(double[] vector) {
        return matchAudited(vector, null, DEFAULT_TEMPERATURE).match;
    }

    public PrototypeReport matchAudited(double[] vector) {
        return matchAudited(vector, null, DEFAULT_TEMPERATURE);
    }

    public PrototypeReport matchAudited(
            double[] vector, boolean[] missingMask, double temperature) {
        long started = System.currentTimeMillis();
        ArtifactAudit artifact = auditArtifact();
        if (!artifact.valid) {
            return PrototypeReport.error("ARTIFACT_INVALID:" + artifact.detail);
        }
        if (vector == null || vector.length != EXPECTED_DIMENSION) {
            return PrototypeReport.error("VECTOR_DIMENSION_INVALID");
        }
        temperature = Math.max(0.10d, Math.min(5.0d, V51Math.finite(temperature)));
        int missing = countMissing(vector, missingMask);
        double[] normalized = standardize(vector, missingMask);
        ArrayList<PrototypeScore> scores = new ArrayList<PrototypeScore>();
        double maxLogit = -Double.MAX_VALUE;
        for (int i = 0; i < V51StateModelArtifact.CENTERS.length; i++) {
            DistanceBreakdown distance =
                    distanceTo(
                            normalized,
                            V51StateModelArtifact.CENTERS[i],
                            V51StateModelArtifact.SCALE,
                            missingMask);
            double missingPenalty = ((double) missing) / Math.max(1, EXPECTED_DIMENSION);
            double logit = -0.5d * distance.squaredDistance / temperature - 0.35d * missingPenalty;
            if (logit > maxLogit) maxLogit = logit;
            scores.add(new PrototypeScore(V51StateModelArtifact.IDS[i], distance, logit, 0.0d, i));
        }
        double sum = 0.0d;
        for (int i = 0; i < scores.size(); i++) {
            PrototypeScore score = scores.get(i);
            double weight = Math.exp(Math.max(-60.0d, score.logit - maxLogit));
            score.posterior = weight;
            sum += weight;
        }
        if (!(sum > 0.0d)) {
            return PrototypeReport.error("POSTERIOR_NORMALIZATION_FAILED");
        }
        int best = 0;
        int second = scores.size() > 1 ? 1 : 0;
        for (int i = 0; i < scores.size(); i++) {
            scores.get(i).posterior /= sum;
            if (scores.get(i).posterior > scores.get(best).posterior) best = i;
        }
        second = best == 0 && scores.size() > 1 ? 1 : 0;
        for (int i = 0; i < scores.size(); i++) {
            if (i != best && scores.get(i).posterior > scores.get(second).posterior) second = i;
        }
        double[] posterior = new double[scores.size()];
        for (int i = 0; i < scores.size(); i++) posterior[i] = scores.get(i).posterior;
        double entropy = V51Math.entropy(posterior);
        PrototypeScore winner = scores.get(best);
        PrototypeScore runner = scores.get(second);
        double margin =
                Math.max(0.0d, winner.posterior - (second == best ? 0.0d : runner.posterior));
        double distance = Math.sqrt(Math.max(0.0d, winner.distance.squaredDistance));
        boolean ood =
                distance >= OOD_DISTANCE_HARD
                        || (distance >= OOD_DISTANCE_WARN && margin < LOW_MARGIN)
                        || missing > EXPECTED_DIMENSION / 3;
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|"
                                + winner.id
                                + "|"
                                + vectorText(vector)
                                + "|"
                                + vectorText(posterior)
                                + "|"
                                + distance
                                + "|"
                                + margin
                                + "|"
                                + entropy
                                + "|"
                                + missing
                                + "|"
                                + (ood ? 1 : 0));
        Match match =
                new Match(
                        winner.id,
                        winner.posterior,
                        distance,
                        second == best ? 0.0d : runner.posterior,
                        margin,
                        entropy,
                        ood,
                        missing,
                        checksum);
        return new PrototypeReport(
                true,
                match,
                Collections.unmodifiableList(scores),
                normalized,
                artifact,
                Math.max(0L, System.currentTimeMillis() - started),
                checksum,
                "");
    }

    public List<Match> matchAll(double[][] vectors) {
        ArrayList<Match> out = new ArrayList<Match>();
        if (vectors == null) return out;
        for (int i = 0; i < vectors.length; i++) out.add(match(vectors[i]));
        return Collections.unmodifiableList(out);
    }

    public DistanceBreakdown distanceBreakdown(double[] vector, int prototypeIndex) {
        if (prototypeIndex < 0 || prototypeIndex >= V51StateModelArtifact.CENTERS.length)
            return DistanceBreakdown.error("PROTOTYPE_INDEX_INVALID");
        if (vector == null || vector.length != EXPECTED_DIMENSION)
            return DistanceBreakdown.error("VECTOR_DIMENSION_INVALID");
        return distanceTo(
                standardize(vector, null),
                V51StateModelArtifact.CENTERS[prototypeIndex],
                V51StateModelArtifact.SCALE,
                null);
    }

    public ArtifactAudit auditArtifact() {
        ArrayList<String> errors = new ArrayList<String>();
        if (V51StateModelArtifact.IDS == null
                || V51StateModelArtifact.CENTERS == null
                || V51StateModelArtifact.SCALE == null) {
            errors.add("ARTIFACT_NULL");
        } else {
            if (V51StateModelArtifact.IDS.length == 0) errors.add("PROTOTYPE_EMPTY");
            if (V51StateModelArtifact.IDS.length != V51StateModelArtifact.CENTERS.length)
                errors.add("ID_CENTER_COUNT_MISMATCH");
            if (V51StateModelArtifact.SCALE.length != EXPECTED_DIMENSION)
                errors.add("SCALE_DIMENSION_MISMATCH");
            for (int i = 0; i < V51StateModelArtifact.SCALE.length; i++) {
                double scale = V51StateModelArtifact.SCALE[i];
                if (Double.isNaN(scale) || Double.isInfinite(scale) || Math.abs(scale) < MIN_SCALE)
                    errors.add("SCALE_INVALID_" + i);
            }
            for (int i = 0; i < V51StateModelArtifact.CENTERS.length; i++) {
                double[] center = V51StateModelArtifact.CENTERS[i];
                if (center == null || center.length != EXPECTED_DIMENSION) {
                    errors.add("CENTER_DIMENSION_" + i);
                    continue;
                }
                for (int j = 0; j < center.length; j++) {
                    if (Double.isNaN(center[j]) || Double.isInfinite(center[j]))
                        errors.add("CENTER_NON_FINITE_" + i + "_" + j);
                }
            }
        }
        String detail = join(errors);
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|"
                                + V51StateModelArtifact.ARTIFACT_ID
                                + "|"
                                + V51StateModelArtifact.DATA_SHA256
                                + "|"
                                + detail
                                + "|"
                                + artifactCanonical());
        return new ArtifactAudit(
                errors.isEmpty(),
                V51StateModelArtifact.PROVEN,
                V51StateModelArtifact.IDS == null ? 0 : V51StateModelArtifact.IDS.length,
                EXPECTED_DIMENSION,
                detail,
                checksum);
    }

    public ReplayAudit replay(double[] vector) {
        PrototypeReport first = matchAudited(copy(vector));
        PrototypeReport second = matchAudited(copy(vector));
        boolean same =
                first.valid
                        && second.valid
                        && first.checksum.equals(second.checksum)
                        && first.match.canonical().equals(second.match.canonical());
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
                same ? "" : "PROTOTYPE_REPLAY_MISMATCH");
    }

    public Comparison compare(double[] left, double[] right) {
        PrototypeReport a = matchAudited(left);
        PrototypeReport b = matchAudited(right);
        double vectorDistance = normalizedDistance(left, right);
        boolean same = a.valid && b.valid && a.match.id.equals(b.match.id);
        double posteriorShift =
                a.valid && b.valid ? Math.abs(a.match.posterior - b.match.posterior) : 1.0d;
        String checksum =
                V51ChecksumEngine.sha256(
                        B05_VERSION
                                + "|COMPARE|"
                                + a.checksum
                                + "|"
                                + b.checksum
                                + "|"
                                + vectorDistance
                                + "|"
                                + posteriorShift);
        return new Comparison(
                same,
                vectorDistance,
                posteriorShift,
                a.valid ? a.match.id : "",
                b.valid ? b.match.id : "",
                checksum);
    }

    public static String schemaHash() {
        return V51ChecksumEngine.sha256(
                B05_VERSION
                        + "|"
                        + EXPECTED_DIMENSION
                        + "|"
                        + V51StateModelArtifact.ARTIFACT_ID
                        + "|"
                        + artifactCanonical()
                        + "|"
                        + rulesHash());
    }

    public static List<ContractRule> contractRules() {
        ArrayList<ContractRule> rules = new ArrayList<ContractRule>();
        rules.add(
                new ContractRule(
                        "B05-PROTO-001",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_001",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-002",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_002",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-003",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_003",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-004",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_004",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-005",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_005",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-006",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_006",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-007",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_007",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-008",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_008",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-009",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_009",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-010",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_010",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-011",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_011",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-012",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_012",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-013",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_013",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-014",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_014",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-015",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_015",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-016",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_016",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-017",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_017",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-018",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_018",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-019",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_019",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-020",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_020",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-021",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_021",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-022",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_022",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-023",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_023",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-024",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_024",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-025",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_025",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-026",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_026",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-027",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_027",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-028",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_028",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-029",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_029",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-030",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_030",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-031",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_031",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-032",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_032",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-033",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_033",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-034",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_034",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-035",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_035",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-036",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_036",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-037",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_037",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-038",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_038",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-039",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_039",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-040",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_040",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-041",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_041",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-042",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_042",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-043",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_043",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-044",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_044",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-045",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_045",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-046",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_046",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-047",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_047",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-048",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_048",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-049",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_049",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-050",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_050",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-051",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_051",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-052",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_052",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-053",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_053",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-054",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_054",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-055",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_055",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-056",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_056",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-057",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_057",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-058",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_058",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-059",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_059",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-060",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_060",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-061",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_061",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-062",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_062",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-063",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_063",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-064",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_064",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-065",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_065",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-066",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_066",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-067",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_067",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-068",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_068",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-069",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_069",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-070",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_070",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-071",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_071",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-072",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_072",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-073",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_073",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-074",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_074",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-075",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_075",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-076",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_076",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-077",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_077",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-078",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_078",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-079",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_079",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-080",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_080",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-081",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_081",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-082",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_082",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-083",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_083",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-084",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_084",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-085",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_085",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-086",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_086",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-087",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_087",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-088",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_088",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-089",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_089",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-090",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_090",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-091",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_091",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-092",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_092",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-093",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_093",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-094",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_094",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-095",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_095",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-096",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_096",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-097",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_097",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-098",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_098",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-099",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_099",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-100",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_100",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-101",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_101",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-102",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_102",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-103",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_103",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-104",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_104",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-105",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_105",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-106",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_106",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-107",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_107",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-108",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_108",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-109",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_109",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-110",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_110",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-111",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_111",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-112",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_112",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-113",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_113",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-114",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_114",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-115",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_115",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-116",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_116",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-117",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_117",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-118",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_118",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-119",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_119",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-120",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_120",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-121",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_121",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-122",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_122",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-123",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_123",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-124",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_124",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-125",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_125",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-126",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_126",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-127",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_127",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-128",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_128",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-129",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_129",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-130",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_130",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-131",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_131",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-132",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_132",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-133",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_133",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-134",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_134",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-135",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_135",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-136",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_136",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-137",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_137",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-138",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_138",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-139",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_139",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-140",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_140",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-141",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_141",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-142",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_142",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-143",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_143",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-144",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_144",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-145",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_145",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-146",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_146",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-147",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_147",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-148",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_148",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-149",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_149",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-150",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_150",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-151",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_151",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-152",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_152",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-153",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_153",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-154",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_154",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-155",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_155",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-156",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_156",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-157",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_157",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-158",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_158",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-159",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_159",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-160",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_160",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-161",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_161",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-162",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_162",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-163",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_163",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-164",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_164",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-165",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_165",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-166",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_166",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-167",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_167",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-168",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_168",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-169",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_169",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-170",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_170",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-171",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_171",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-172",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_172",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-173",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_173",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-174",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_174",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-175",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_175",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-176",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_176",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-177",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_177",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-178",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_178",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-179",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_179",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-180",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_180",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-181",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_181",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-182",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_182",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-183",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_183",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-184",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_184",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-185",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_185",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-186",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_186",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-187",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_187",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-188",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_188",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-189",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_189",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-190",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_190",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-191",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_191",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-192",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_192",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-193",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_193",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-194",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_194",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-195",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_195",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-196",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_196",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-197",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_197",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-198",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_198",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-199",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_199",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-200",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_200",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-201",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_201",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-202",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_202",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-203",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_203",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-204",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_204",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-205",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_205",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-206",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_206",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-207",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_207",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-208",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_208",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-209",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_209",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-210",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_210",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-211",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_211",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-212",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_212",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-213",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_213",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-214",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_214",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-215",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_215",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-216",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_216",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-217",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_217",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-218",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_218",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-219",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_219",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-220",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_220",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-221",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_221",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-222",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_222",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-223",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_223",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-224",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_224",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-225",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_225",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-226",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_226",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-227",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_227",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-228",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_228",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-229",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_229",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-230",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_230",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-231",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_231",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-232",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_232",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-233",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_233",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-234",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_234",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-235",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_235",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-236",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_236",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-237",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_237",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-238",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_238",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-239",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_239",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-240",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_240",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-241",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_241",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-242",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_242",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-243",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_243",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-244",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_244",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-245",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_245",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-246",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_246",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-247",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_247",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-248",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_248",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-249",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_249",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-250",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_250",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-251",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_251",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-252",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_252",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-253",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_253",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-254",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_254",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-255",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_255",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-256",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_256",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-257",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_257",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-258",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_258",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-259",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_259",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-260",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_260",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-261",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_261",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-262",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_262",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-263",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_263",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-264",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_264",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-265",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_265",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-266",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_266",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-267",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_267",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-268",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_268",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-269",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_269",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-270",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_270",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-271",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_271",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-272",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_272",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-273",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_273",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-274",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_274",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-275",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_275",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-276",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_276",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-277",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_277",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-278",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_278",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-279",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_279",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-280",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_280",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-281",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_281",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-282",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_282",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-283",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_283",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-284",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_284",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-285",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_285",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-286",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_286",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-287",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_287",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-288",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_288",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-289",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_289",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-290",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_290",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-291",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_291",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-292",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_292",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-293",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_293",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-294",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_294",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-295",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_295",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-296",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_296",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-297",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_297",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-298",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_298",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-299",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_299",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-300",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_300",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-301",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_301",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-302",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_302",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-303",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_303",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-304",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_304",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-305",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_305",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-306",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_306",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-307",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_307",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-308",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_308",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-309",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_309",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-310",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_310",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-311",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_311",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-312",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_312",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-313",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_313",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-314",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_314",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-315",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_315",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-316",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_316",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-317",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_317",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-318",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_318",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-319",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_319",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-320",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_320",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-321",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_321",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-322",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_322",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-323",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_323",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-324",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_324",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-325",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_325",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-326",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_326",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-327",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_327",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-328",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_328",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-329",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_329",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-330",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_330",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-331",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_331",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-332",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_332",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-333",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_333",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-334",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_334",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-335",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_335",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-336",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_336",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-337",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_337",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-338",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_338",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-339",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_339",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-340",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_340",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-341",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_341",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-342",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_342",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-343",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_343",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-344",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_344",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-345",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_345",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-346",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_346",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-347",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_347",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-348",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_348",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-349",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_349",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-350",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_350",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-351",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_351",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-352",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_352",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-353",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_353",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-354",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_354",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-355",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_355",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-356",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_356",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-357",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_357",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-358",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_358",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-359",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_359",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-360",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_360",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-361",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_361",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-362",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_362",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-363",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_363",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-364",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_364",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-365",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_365",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-366",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_366",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-367",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_367",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-368",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_368",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-369",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_369",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-370",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_370",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-371",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_371",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-372",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_372",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-373",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_373",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-374",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_374",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-375",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_375",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-376",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_376",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-377",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_377",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-378",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_378",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-379",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_379",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-380",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_380",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-381",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_381",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-382",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_382",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-383",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_383",
                        true,
                        6));
        rules.add(
                new ContractRule(
                        "B05-PROTO-384",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_384",
                        true,
                        7));
        rules.add(
                new ContractRule(
                        "B05-PROTO-385",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_385",
                        true,
                        1));
        rules.add(
                new ContractRule(
                        "B05-PROTO-386",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_386",
                        true,
                        2));
        rules.add(
                new ContractRule(
                        "B05-PROTO-387",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_387",
                        true,
                        3));
        rules.add(
                new ContractRule(
                        "B05-PROTO-388",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_388",
                        true,
                        4));
        rules.add(
                new ContractRule(
                        "B05-PROTO-389",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_389",
                        true,
                        5));
        rules.add(
                new ContractRule(
                        "B05-PROTO-390",
                        "V51StatePrototypeEngine",
                        "PIT_STATE_PROTOTYPE_390",
                        true,
                        6));
        return Collections.unmodifiableList(rules);
    }

    public static String rulesHash() {
        StringBuilder out = new StringBuilder();
        List<ContractRule> rules = contractRules();
        for (int i = 0; i < rules.size(); i++) out.append(rules.get(i).canonical()).append(';');
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static double[] standardize(double[] vector, boolean[] missingMask) {
        double[] out = new double[EXPECTED_DIMENSION];
        for (int i = 0; i < out.length; i++) {
            boolean missing = missingMask != null && i < missingMask.length && missingMask[i];
            double value = missing ? 0.0d : V51Math.finite(vector[i]);
            double scale = Math.max(MIN_SCALE, Math.abs(V51StateModelArtifact.SCALE[i]));
            out[i] = value / scale;
        }
        return out;
    }

    private static DistanceBreakdown distanceTo(
            double[] normalized, double[] center, double[] scale, boolean[] missingMask) {
        if (normalized == null
                || center == null
                || normalized.length != EXPECTED_DIMENSION
                || center.length != EXPECTED_DIMENSION)
            return DistanceBreakdown.error("DISTANCE_DIMENSION_INVALID");
        double[] contribution = new double[EXPECTED_DIMENSION];
        double sum = 0.0d;
        double maximum = 0.0d;
        int effective = 0;
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            if (missingMask != null && i < missingMask.length && missingMask[i]) {
                contribution[i] = 0.0d;
                continue;
            }
            double s = Math.max(MIN_SCALE, Math.abs(scale[i]));
            double centerNormalized = V51Math.finite(center[i]) / s;
            double delta = V51Math.finite(normalized[i]) - centerNormalized;
            double value = delta * delta;
            contribution[i] = value;
            sum += value;
            if (value > maximum) maximum = value;
            effective++;
        }
        if (effective == 0) return DistanceBreakdown.error("NO_EFFECTIVE_DIMENSION");
        double adjusted = sum * ((double) EXPECTED_DIMENSION / effective);
        return new DistanceBreakdown(
                true,
                adjusted,
                maximum,
                effective,
                contribution,
                V51ChecksumEngine.hashDoubles(new double[][] {contribution}),
                "");
    }

    private static int countMissing(double[] vector, boolean[] mask) {
        int missing = 0;
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            boolean marked = mask != null && i < mask.length && mask[i];
            double value = vector == null || i >= vector.length ? Double.NaN : vector[i];
            if (marked || Double.isNaN(value) || Double.isInfinite(value)) missing++;
        }
        return missing;
    }

    private static double normalizedDistance(double[] left, double[] right) {
        if (left == null
                || right == null
                || left.length != EXPECTED_DIMENSION
                || right.length != EXPECTED_DIMENSION) return 1.0d;
        double sum = 0.0d;
        for (int i = 0; i < EXPECTED_DIMENSION; i++) {
            double scale = Math.max(MIN_SCALE, Math.abs(V51StateModelArtifact.SCALE[i]));
            double delta = (V51Math.finite(left[i]) - V51Math.finite(right[i])) / scale;
            sum += delta * delta;
        }
        return Math.sqrt(sum / EXPECTED_DIMENSION);
    }

    private static String artifactCanonical() {
        StringBuilder out = new StringBuilder();
        if (V51StateModelArtifact.IDS != null) {
            for (int i = 0; i < V51StateModelArtifact.IDS.length; i++)
                out.append(V51StateModelArtifact.IDS[i]).append(',');
        }
        out.append('|');
        if (V51StateModelArtifact.SCALE != null)
            out.append(vectorText(V51StateModelArtifact.SCALE));
        out.append('|');
        if (V51StateModelArtifact.CENTERS != null) {
            for (int i = 0; i < V51StateModelArtifact.CENTERS.length; i++)
                out.append(vectorText(V51StateModelArtifact.CENTERS[i])).append(';');
        }
        return out.toString();
    }

    private static double[] copy(double[] values) {
        return V51Math.copy(values);
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

    public static final class PrototypeReport {
        public final boolean valid;
        public final Match match;
        public final List<PrototypeScore> scores;
        public final double[] normalizedVector;
        public final ArtifactAudit artifact;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        PrototypeReport(
                boolean valid,
                Match match,
                List<PrototypeScore> scores,
                double[] normalizedVector,
                ArtifactAudit artifact,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.match = match;
            this.scores = scores == null ? Collections.<PrototypeScore>emptyList() : scores;
            this.normalizedVector = V51Math.copy(normalizedVector);
            this.artifact = artifact;
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static PrototypeReport error(String detail) {
            String checksum = V51ChecksumEngine.sha256(B05_VERSION + "|ERROR|" + safe(detail));
            return new PrototypeReport(
                    false,
                    new Match(
                            "PROTO_UNKNOWN",
                            0.0d,
                            OOD_DISTANCE_HARD,
                            0.0d,
                            0.0d,
                            1.0d,
                            true,
                            EXPECTED_DIMENSION,
                            checksum),
                    Collections.<PrototypeScore>emptyList(),
                    new double[0],
                    null,
                    0L,
                    checksum,
                    detail);
        }

        public String summary() {
            return "valid="
                    + (valid ? 1 : 0)
                    + "|prototype="
                    + (match == null ? "" : match.id)
                    + "|posterior="
                    + (match == null ? 0.0d : match.posterior)
                    + "|margin="
                    + (match == null ? 0.0d : match.margin)
                    + "|distance="
                    + (match == null ? 0.0d : match.distance)
                    + "|ood="
                    + (match != null && match.outOfDistribution ? 1 : 0)
                    + "|cost="
                    + elapsedMs
                    + "ms|detail="
                    + detail;
        }
    }

    public static final class PrototypeScore {
        public final String id;
        public final DistanceBreakdown distance;
        public final double logit;
        public double posterior;
        public final int index;

        PrototypeScore(
                String id, DistanceBreakdown distance, double logit, double posterior, int index) {
            this.id = safe(id);
            this.distance = distance;
            this.logit = V51Math.finite(logit);
            this.posterior = V51Math.clamp(posterior, 0.0d, 1.0d);
            this.index = Math.max(0, index);
        }

        public String canonical() {
            return id
                    + "|"
                    + logit
                    + "|"
                    + posterior
                    + "|"
                    + (distance == null ? "" : distance.canonical())
                    + "|"
                    + index;
        }
    }

    public static final class DistanceBreakdown {
        public final boolean valid;
        public final double squaredDistance;
        public final double maximumContribution;
        public final int effectiveDimensions;
        public final double[] contribution;
        public final String checksum;
        public final String detail;

        DistanceBreakdown(
                boolean valid,
                double squaredDistance,
                double maximumContribution,
                int effectiveDimensions,
                double[] contribution,
                String checksum,
                String detail) {
            this.valid = valid;
            this.squaredDistance = Math.max(0.0d, V51Math.finite(squaredDistance));
            this.maximumContribution = Math.max(0.0d, V51Math.finite(maximumContribution));
            this.effectiveDimensions = Math.max(0, effectiveDimensions);
            this.contribution = V51Math.copy(contribution);
            this.checksum = safe(checksum);
            this.detail = safe(detail);
        }

        static DistanceBreakdown error(String detail) {
            return new DistanceBreakdown(
                    false,
                    0.0d,
                    0.0d,
                    0,
                    new double[0],
                    V51ChecksumEngine.sha256(B05_VERSION + "|DISTANCE_ERROR|" + safe(detail)),
                    detail);
        }

        public String canonical() {
            return valid
                    + "|"
                    + squaredDistance
                    + "|"
                    + maximumContribution
                    + "|"
                    + effectiveDimensions
                    + "|"
                    + checksum
                    + "|"
                    + detail;
        }
    }

    public static final class ArtifactAudit {
        public final boolean valid;
        public final boolean proven;
        public final int prototypeCount;
        public final int dimension;
        public final String detail;
        public final String checksum;

        ArtifactAudit(
                boolean valid,
                boolean proven,
                int prototypeCount,
                int dimension,
                String detail,
                String checksum) {
            this.valid = valid;
            this.proven = proven;
            this.prototypeCount = Math.max(0, prototypeCount);
            this.dimension = Math.max(0, dimension);
            this.detail = safe(detail);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return "valid="
                    + (valid ? 1 : 0)
                    + "|proven="
                    + (proven ? 1 : 0)
                    + "|prototypes="
                    + prototypeCount
                    + "|dimension="
                    + dimension
                    + "|detail="
                    + detail
                    + "|hash="
                    + checksum;
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

    public static final class Comparison {
        public final boolean samePrototype;
        public final double vectorDistance;
        public final double posteriorShift;
        public final String leftPrototype;
        public final String rightPrototype;
        public final String checksum;

        Comparison(
                boolean samePrototype,
                double vectorDistance,
                double posteriorShift,
                String leftPrototype,
                String rightPrototype,
                String checksum) {
            this.samePrototype = samePrototype;
            this.vectorDistance = Math.max(0.0d, V51Math.finite(vectorDistance));
            this.posteriorShift = V51Math.clamp(posteriorShift, 0.0d, 1.0d);
            this.leftPrototype = safe(leftPrototype);
            this.rightPrototype = safe(rightPrototype);
            this.checksum = safe(checksum);
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
