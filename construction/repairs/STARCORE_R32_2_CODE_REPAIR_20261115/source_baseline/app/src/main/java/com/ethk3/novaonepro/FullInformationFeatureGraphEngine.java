package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B04 unique point-in-time full-information feature DAG. The graph is metadata-only: it never
 * duplicates the full history or owns a probability, lane, authorization, BET, settlement or ledger
 * decision.
 */
public final class FullInformationFeatureGraphEngine {
    public static final String B04_VERSION = "STARCORE_TIANSHU_B04_FEATURE_GRAPH_V1";
    public static final int MAX_CACHE_ENTRIES = 2;
    private static final FullInformationFeatureGraphEngine INSTANCE =
            new FullInformationFeatureGraphEngine();

    private final LinkedHashMap<String, FrozenFeatureGraph> cache;
    private long buildCount;
    private long cacheHitCount;

    private FullInformationFeatureGraphEngine() {
        cache =
                new LinkedHashMap<String, FrozenFeatureGraph>(4, 0.75f, true) {
                    protected boolean removeEldestEntry(
                            Map.Entry<String, FrozenFeatureGraph> eldest) {
                        return size() > MAX_CACHE_ENTRIES;
                    }
                };
    }

    public static FullInformationFeatureGraphEngine shared() {
        return INSTANCE;
    }

    public synchronized FrozenFeatureGraph build(
            V51Models.PredictionContext context,
            V51Models.DrawFrame[] history,
            double[] globalFeatures,
            double[][][] numberFeatures,
            RollingWindowCache.SnapshotSet windows) {
        GraphRequest request =
                new GraphRequest(context, history, globalFeatures, numberFeatures, windows);
        String key = request.cacheKey();
        FrozenFeatureGraph cached = cache.get(key);
        if (cached != null) {
            cacheHitCount++;
            return cached;
        }
        buildCount++;
        FrozenFeatureGraph graph = execute(request);
        cache.put(key, graph);
        return graph;
    }

    public synchronized String diagnostics() {
        return B04_VERSION
                + "|build="
                + buildCount
                + "|cacheHit="
                + cacheHitCount
                + "|entries="
                + cache.size();
    }

    public synchronized void clear() {
        cache.clear();
    }

    private FrozenFeatureGraph execute(GraphRequest request) {
        long started = System.currentTimeMillis();
        ArrayList<NodeRecord> nodes = new ArrayList<NodeRecord>();

        PointInTimeSnapshotEngine.FrameSnapshot pit =
                new PointInTimeSnapshotEngine()
                        .buildFrameSnapshotBeforeDecision(request.context, request.history);
        nodes.add(
                node(
                        "PIT_CERTIFICATE",
                        new String[0],
                        pit.featureInputHash,
                        pit.noFutureLeak ? 0 : 1,
                        "PointInTimeSnapshotEngine"));

        RollingWindowCache.SnapshotSet windows =
                request.windows == null
                        ? RollingWindowCache.shared().getOrBuild(request.history)
                        : request.windows;
        RollingWindowCache.Validation windowValidation = windows.validate();
        nodes.add(
                node(
                        "ROLLING_WINDOWS",
                        new String[] {"PIT_CERTIFICATE"},
                        windows.checksum,
                        windowValidation.ok ? 0 : 1,
                        "RollingWindowCache"));

        int globalMissing = countMissing(request.globalFeatures);
        String globalHash = hash(request.globalFeatures);
        nodes.add(
                node(
                        "PERMUTATION_GLOBAL",
                        new String[] {"PIT_CERTIFICATE"},
                        globalHash,
                        globalMissing,
                        "V51PermutationFeatureEngine"));

        int numberMissing = countMissing(request.numberFeatures);
        String numberHash = hash(request.numberFeatures);
        nodes.add(
                node(
                        "TRAJECTORY_NUMBER",
                        new String[] {"PIT_CERTIFICATE", "ROLLING_WINDOWS"},
                        numberHash,
                        numberMissing,
                        "V51TrajectoryFeatureEngine"));

        String missingHash =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|global="
                                + globalMissing
                                + "|number="
                                + numberMissing
                                + "|pit="
                                + (pit.noFutureLeak ? 0 : 1)
                                + "|window="
                                + (windowValidation.ok ? 0 : 1));
        nodes.add(
                node(
                        "MISSING_MASK",
                        new String[] {"PERMUTATION_GLOBAL", "TRAJECTORY_NUMBER"},
                        missingHash,
                        globalMissing + numberMissing,
                        "FullInformationFeatureGraphEngine"));

        String provenanceHash =
                V51ChecksumEngine.sha256(
                        request.context.cohortId
                                + "|"
                                + request.context.artifactBundleHash
                                + "|"
                                + request.context.historyHash
                                + "|"
                                + request.context.baseIssue
                                + "|"
                                + request.context.targetIssue
                                + "|"
                                + windows.checksum);
        nodes.add(
                node(
                        "PROVENANCE",
                        new String[] {"PIT_CERTIFICATE", "ROLLING_WINDOWS"},
                        provenanceHash,
                        0,
                        "V51BuildManifest"));

        LeakAudit leakAudit = auditLeakage(request.context, request.history, pit);
        nodes.add(
                node(
                        "LEAK_AUDIT",
                        new String[] {"PIT_CERTIFICATE", "PROVENANCE"},
                        leakAudit.checksum,
                        leakAudit.passed ? 0 : leakAudit.violations,
                        "FullInformationFeatureGraphEngine"));

        StringBuilder canonical = new StringBuilder();
        canonical
                .append(B04_VERSION)
                .append('|')
                .append(request.context.targetIssue)
                .append('|')
                .append(request.context.historyHash);
        for (int i = 0; i < nodes.size(); i++) {
            canonical.append('|').append(nodes.get(i).canonical());
        }
        String featureHash = V51ChecksumEngine.sha256(canonical.toString());
        nodes.add(
                node(
                        "FROZEN_GRAPH",
                        new String[] {
                            "PIT_CERTIFICATE",
                            "ROLLING_WINDOWS",
                            "PERMUTATION_GLOBAL",
                            "TRAJECTORY_NUMBER",
                            "MISSING_MASK",
                            "PROVENANCE",
                            "LEAK_AUDIT"
                        },
                        featureHash,
                        globalMissing
                                + numberMissing
                                + (leakAudit.passed ? 0 : leakAudit.violations),
                        "FullInformationFeatureGraphEngine"));

        long elapsed = Math.max(0L, System.currentTimeMillis() - started);
        return new FrozenFeatureGraph(
                request.context.baseIssue,
                request.context.targetIssue,
                request.context.historyHash,
                windows.checksum,
                pit.featureInputHash,
                globalHash,
                numberHash,
                featureHash,
                globalMissing + numberMissing,
                leakAudit,
                Collections.unmodifiableList(nodes),
                elapsed);
    }

    private static NodeRecord node(
            String id, String[] dependencies, String valueHash, int missingCount, String owner) {
        return new NodeRecord(id, dependencies, valueHash, missingCount, owner);
    }

    private static LeakAudit auditLeakage(
            V51Models.PredictionContext context,
            V51Models.DrawFrame[] history,
            PointInTimeSnapshotEngine.FrameSnapshot pit) {
        int violations = 0;
        String firstBadIssue = "";
        String previous = "";
        if (history == null) {
            violations++;
        } else {
            for (int i = 0; i < history.length; i++) {
                V51Models.DrawFrame frame = history[i];
                if (frame == null || frame.issue.length() == 0) {
                    violations++;
                    if (firstBadIssue.length() == 0) {
                        firstBadIssue = "NULL_OR_EMPTY";
                    }
                    continue;
                }
                if (frame.issue.compareTo(context.baseIssue) > 0
                        || frame.issue.compareTo(context.targetIssue) >= 0
                        || (previous.length() > 0 && frame.issue.compareTo(previous) <= 0)) {
                    violations++;
                    if (firstBadIssue.length() == 0) {
                        firstBadIssue = frame.issue;
                    }
                }
                previous = frame.issue;
            }
        }
        if (!pit.noFutureLeak) {
            violations++;
            if (firstBadIssue.length() == 0) {
                firstBadIssue = "PIT_CERTIFICATE";
            }
        }
        boolean passed = violations == 0;
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|"
                                + context.baseIssue
                                + "|"
                                + context.targetIssue
                                + "|"
                                + violations
                                + "|"
                                + firstBadIssue
                                + "|"
                                + pit.featureInputHash);
        return new LeakAudit(passed, violations, firstBadIssue, checksum);
    }

    private static int countMissing(double[] values) {
        if (values == null) {
            return 1;
        }
        int missing = 0;
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i]) || Double.isInfinite(values[i])) {
                missing++;
            }
        }
        return missing;
    }

    private static int countMissing(double[][][] values) {
        if (values == null) {
            return 1;
        }
        int missing = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                missing++;
                continue;
            }
            for (int j = 0; j < values[i].length; j++) {
                missing += countMissing(values[i][j]);
            }
        }
        return missing;
    }

    private static String hash(double[] values) {
        StringBuilder payload = new StringBuilder();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                payload.append(Long.toHexString(Double.doubleToLongBits(V51Math.finite(values[i]))))
                        .append(',');
            }
        }
        return V51ChecksumEngine.sha256(payload.toString());
    }

    private static String hash(double[][][] values) {
        StringBuilder payload = new StringBuilder();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    payload.append("null;");
                    continue;
                }
                for (int j = 0; j < values[i].length; j++) {
                    double[] row = values[i][j];
                    if (row == null) {
                        payload.append("null,");
                        continue;
                    }
                    for (int k = 0; k < row.length; k++) {
                        payload.append(
                                        Long.toHexString(
                                                Double.doubleToLongBits(V51Math.finite(row[k]))))
                                .append(',');
                    }
                    payload.append(';');
                }
            }
        }
        return V51ChecksumEngine.sha256(payload.toString());
    }

    public static List<NodeSpec> schema() {
        ArrayList<NodeSpec> out = new ArrayList<NodeSpec>();
        out.add(
                new NodeSpec(
                        "PIT_CERTIFICATE",
                        new String[0],
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "ROLLING_WINDOWS",
                        new String[] {"PIT_CERTIFICATE"},
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "PERMUTATION_GLOBAL",
                        new String[] {"PIT_CERTIFICATE"},
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "TRAJECTORY_NUMBER",
                        new String[] {"PIT_CERTIFICATE", "ROLLING_WINDOWS"},
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "MISSING_MASK",
                        new String[] {"PERMUTATION_GLOBAL", "TRAJECTORY_NUMBER"},
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "PROVENANCE",
                        new String[] {"PIT_CERTIFICATE", "ROLLING_WINDOWS"},
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "LEAK_AUDIT",
                        new String[] {"PIT_CERTIFICATE", "PROVENANCE"},
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        out.add(
                new NodeSpec(
                        "FROZEN_GRAPH",
                        new String[] {
                            "PIT_CERTIFICATE",
                            "ROLLING_WINDOWS",
                            "PERMUTATION_GLOBAL",
                            "TRAJECTORY_NUMBER",
                            "MISSING_MASK",
                            "PROVENANCE",
                            "LEAK_AUDIT"
                        },
                        true,
                        "PIT_ONLY",
                        "SINGLE_BUILD_PER_TARGET"));
        return Collections.unmodifiableList(out);
    }

    public static final class GraphRequest {
        public final V51Models.PredictionContext context;
        public final V51Models.DrawFrame[] history;
        public final double[] globalFeatures;
        public final double[][][] numberFeatures;
        public final RollingWindowCache.SnapshotSet windows;

        GraphRequest(
                V51Models.PredictionContext context,
                V51Models.DrawFrame[] history,
                double[] globalFeatures,
                double[][][] numberFeatures,
                RollingWindowCache.SnapshotSet windows) {
            if (context == null) {
                throw new IllegalArgumentException("B04_GRAPH_CONTEXT_NULL");
            }
            this.context = context;
            this.history = history == null ? new V51Models.DrawFrame[0] : history;
            this.globalFeatures = V51Math.copy(globalFeatures);
            this.numberFeatures = numberFeatures;
            this.windows = windows;
        }

        String cacheKey() {
            return V51ChecksumEngine.sha256(
                    B04_VERSION
                            + "|" + context.baseIssue
                            + "|" + context.artifactBundleHash
                            + "|" + context.cohortId
                            + "|"
                            + context.targetIssue
                            + "|"
                            + context.historyHash
                            + "|"
                            + hash(globalFeatures)
                            + "|"
                            + hash(numberFeatures)
                            + "|"
                            + (windows == null ? "" : windows.checksum));
        }
    }

    public static final class FrozenFeatureGraph {
        public final String baseIssue;
        public final String targetIssue;
        public final String historyHash;
        public final String windowHash;
        public final String pitHash;
        public final String globalHash;
        public final String numberHash;
        public final String featureHash;
        public final int missingCount;
        public final LeakAudit leakAudit;
        public final List<NodeRecord> nodes;
        public final long elapsedMs;

        FrozenFeatureGraph(
                String baseIssue,
                String targetIssue,
                String historyHash,
                String windowHash,
                String pitHash,
                String globalHash,
                String numberHash,
                String featureHash,
                int missingCount,
                LeakAudit leakAudit,
                List<NodeRecord> nodes,
                long elapsedMs) {
            this.baseIssue = safe(baseIssue);
            this.targetIssue = safe(targetIssue);
            this.historyHash = safe(historyHash);
            this.windowHash = safe(windowHash);
            this.pitHash = safe(pitHash);
            this.globalHash = safe(globalHash);
            this.numberHash = safe(numberHash);
            this.featureHash = safe(featureHash);
            this.missingCount = Math.max(0, missingCount);
            this.leakAudit = leakAudit;
            this.nodes = nodes == null ? Collections.<NodeRecord>emptyList() : nodes;
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public boolean valid() {
            return targetIssue.length() > 0
                    && historyHash.length() == 64
                    && featureHash.length() == 64
                    && leakAudit != null
                    && leakAudit.passed
                    && nodes.size() == schema().size();
        }

        public String replayHash() {
            StringBuilder payload = new StringBuilder();
            payload.append(B04_VERSION)
                    .append('|')
                    .append(targetIssue)
                    .append('|')
                    .append(historyHash);
            for (int i = 0; i < nodes.size() - 1; i++) {
                payload.append('|').append(nodes.get(i).canonical());
            }
            return V51ChecksumEngine.sha256(payload.toString());
        }

        public String summary() {
            return "target="
                    + targetIssue
                    + "|nodes="
                    + nodes.size()
                    + "|missing="
                    + missingCount
                    + "|leak="
                    + (leakAudit == null ? "NULL" : leakAudit.summary())
                    + "|cost="
                    + elapsedMs
                    + "ms"
                    + "|hash="
                    + featureHash;
        }
    }

    public static final class NodeRecord {
        public final String id;
        public final String[] dependencies;
        public final String valueHash;
        public final int missingCount;
        public final String owner;

        NodeRecord(
                String id,
                String[] dependencies,
                String valueHash,
                int missingCount,
                String owner) {
            this.id = safe(id);
            this.dependencies = copy(dependencies);
            this.valueHash = safe(valueHash);
            this.missingCount = Math.max(0, missingCount);
            this.owner = safe(owner);
        }

        public String canonical() {
            StringBuilder out = new StringBuilder();
            out.append(id).append(':');
            for (int i = 0; i < dependencies.length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(dependencies[i]);
            }
            out.append(':')
                    .append(valueHash)
                    .append(':')
                    .append(missingCount)
                    .append(':')
                    .append(owner);
            return out.toString();
        }
    }

    public static final class LeakAudit {
        public final boolean passed;
        public final int violations;
        public final String firstBadIssue;
        public final String checksum;

        LeakAudit(boolean passed, int violations, String firstBadIssue, String checksum) {
            this.passed = passed;
            this.violations = Math.max(0, violations);
            this.firstBadIssue = safe(firstBadIssue);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|violations="
                    + violations
                    + "|first="
                    + firstBadIssue
                    + "|hash="
                    + checksum;
        }
    }

    public static final class NodeSpec {
        public final String id;
        public final String[] dependencies;
        public final boolean required;
        public final String pointInTimePolicy;
        public final String uniquenessPolicy;

        NodeSpec(
                String id,
                String[] dependencies,
                boolean required,
                String pointInTimePolicy,
                String uniquenessPolicy) {
            this.id = safe(id);
            this.dependencies = copy(dependencies);
            this.required = required;
            this.pointInTimePolicy = safe(pointInTimePolicy);
            this.uniquenessPolicy = safe(uniquenessPolicy);
        }

        public String canonical() {
            StringBuilder out = new StringBuilder();
            out.append(id).append(':');
            for (int i = 0; i < dependencies.length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(dependencies[i]);
            }
            out.append(':')
                    .append(required ? 1 : 0)
                    .append(':')
                    .append(pointInTimePolicy)
                    .append(':')
                    .append(uniquenessPolicy);
            return out.toString();
        }
    }

    private static String[] copy(String[] source) {
        if (source == null) {
            return new String[0];
        }
        String[] out = new String[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /* B04 full graph governance, replay, budget and provenance support. */
    public static List<FieldContract> fieldContracts() {
        ArrayList<FieldContract> out = new ArrayList<FieldContract>();
        out.add(
                new FieldContract(
                        "B04-FIELD-001",
                        "GLOBAL_0",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-002",
                        "GLOBAL_1",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-003",
                        "GLOBAL_2",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-004",
                        "GLOBAL_3",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-005",
                        "GLOBAL_4",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-006",
                        "GLOBAL_5",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-007",
                        "GLOBAL_6",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-008",
                        "GLOBAL_7",
                        "GLOBAL",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-009",
                        "GLOBAL_8",
                        "RESERVED",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-010",
                        "GLOBAL_9",
                        "RESERVED",
                        "V51PermutationFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-011",
                        "P1_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-012",
                        "P1_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-013",
                        "P1_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-014",
                        "P1_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-015",
                        "P2_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-016",
                        "P2_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-017",
                        "P2_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-018",
                        "P2_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-019",
                        "P3_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-020",
                        "P3_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-021",
                        "P3_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-022",
                        "P3_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-023",
                        "P4_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-024",
                        "P4_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-025",
                        "P4_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-026",
                        "P4_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-027",
                        "P5_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-028",
                        "P5_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-029",
                        "P5_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-030",
                        "P5_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-031",
                        "P6_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-032",
                        "P6_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-033",
                        "P6_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-034",
                        "P6_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-035",
                        "P7_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-036",
                        "P7_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-037",
                        "P7_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-038",
                        "P7_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-039",
                        "P8_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-040",
                        "P8_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-041",
                        "P8_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-042",
                        "P8_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-043",
                        "P9_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-044",
                        "P9_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-045",
                        "P9_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-046",
                        "P9_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-047",
                        "P10_N1_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-048",
                        "P10_N4_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-049",
                        "P10_N7_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        out.add(
                new FieldContract(
                        "B04-FIELD-050",
                        "P10_N10_TRAJECTORY",
                        "NUMBER_TENSOR",
                        "V51TrajectoryFeatureEngine",
                        true,
                        "PIT_ONLY",
                        "MISSING_MASK_REQUIRED"));
        return Collections.unmodifiableList(out);
    }

    public GraphValidation validateGraph(FrozenFeatureGraph graph) {
        ArrayList<String> errors = new ArrayList<String>();
        if (graph == null) {
            errors.add("GRAPH_NULL");
            return new GraphValidation(false, errors, "", "", 0, 0L);
        }
        if (graph.targetIssue.length() == 0) errors.add("TARGET_ISSUE_EMPTY");
        if (graph.historyHash.length() != 64) errors.add("HISTORY_HASH_INVALID");
        if (graph.featureHash.length() != 64) errors.add("FEATURE_HASH_INVALID");
        if (graph.leakAudit == null || !graph.leakAudit.passed) errors.add("LEAK_AUDIT_FAIL");
        if (graph.nodes.size() != schema().size()) errors.add("NODE_COUNT=" + graph.nodes.size());
        List<NodeSpec> specs = schema();
        for (int i = 0; i < specs.size(); i++) {
            NodeSpec spec = specs.get(i);
            NodeRecord record = findNode(graph.nodes, spec.id);
            if (record == null) {
                errors.add("NODE_MISSING=" + spec.id);
                continue;
            }
            if (!sameDependencies(spec.dependencies, record.dependencies))
                errors.add("DEPENDENCY_MISMATCH=" + spec.id);
            if (record.valueHash.length() != 64) errors.add("NODE_HASH_INVALID=" + spec.id);
            if (!"FullInformationFeatureGraphEngine".equals(record.owner)
                    && !"PointInTimeSnapshotEngine".equals(record.owner)
                    && !"RollingWindowCache".equals(record.owner)
                    && !"V51PermutationFeatureEngine".equals(record.owner)
                    && !"V51TrajectoryFeatureEngine".equals(record.owner))
                errors.add("OWNER_INVALID=" + spec.id + ':' + record.owner);
        }
        String replayHash = graph.replayHash();
        if (!graph.featureHash.equals(replayHash)) errors.add("REPLAY_HASH_MISMATCH");
        return new GraphValidation(
                errors.isEmpty(),
                errors,
                graph.featureHash,
                replayHash,
                graph.missingCount,
                graph.elapsedMs);
    }

    public ReplayResult replay(GraphRequest request) {
        long start = System.currentTimeMillis();
        FrozenFeatureGraph first =
                build(
                        request.context,
                        request.history,
                        request.globalFeatures,
                        request.numberFeatures,
                        request.windows);
        FrozenFeatureGraph second =
                build(
                        request.context,
                        request.history,
                        request.globalFeatures,
                        request.numberFeatures,
                        request.windows);
        GraphValidation firstValidation = validateGraph(first);
        GraphValidation secondValidation = validateGraph(second);
        boolean deterministic =
                first.featureHash.equals(second.featureHash)
                        && first.replayHash().equals(second.replayHash());
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|REPLAY|"
                                + first.featureHash
                                + '|'
                                + second.featureHash
                                + '|'
                                + deterministic
                                + '|'
                                + firstValidation.canonical()
                                + '|'
                                + secondValidation.canonical());
        return new ReplayResult(
                deterministic && firstValidation.passed && secondValidation.passed,
                deterministic,
                first,
                second,
                firstValidation,
                secondValidation,
                checksum,
                Math.max(0L, System.currentTimeMillis() - start));
    }

    public GraphDiff diff(FrozenFeatureGraph left, FrozenFeatureGraph right) {
        ArrayList<String> changed = new ArrayList<String>();
        if (left == null || right == null) {
            changed.add("NULL_GRAPH");
            return new GraphDiff(false, changed, "", "", "");
        }
        if (!left.targetIssue.equals(right.targetIssue)) changed.add("TARGET_ISSUE");
        if (!left.historyHash.equals(right.historyHash)) changed.add("HISTORY_HASH");
        if (!left.windowHash.equals(right.windowHash)) changed.add("WINDOW_HASH");
        if (!left.pitHash.equals(right.pitHash)) changed.add("PIT_HASH");
        if (!left.globalHash.equals(right.globalHash)) changed.add("GLOBAL_HASH");
        if (!left.numberHash.equals(right.numberHash)) changed.add("NUMBER_HASH");
        if (left.missingCount != right.missingCount) changed.add("MISSING_COUNT");
        List<NodeSpec> specs = schema();
        for (int i = 0; i < specs.size(); i++) {
            NodeRecord a = findNode(left.nodes, specs.get(i).id);
            NodeRecord b = findNode(right.nodes, specs.get(i).id);
            if (a == null || b == null || !a.valueHash.equals(b.valueHash))
                changed.add("NODE:" + specs.get(i).id);
        }
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|DIFF|"
                                + left.featureHash
                                + '|'
                                + right.featureHash
                                + '|'
                                + join(changed));
        return new GraphDiff(
                changed.isEmpty(), changed, left.featureHash, right.featureHash, checksum);
    }

    public BudgetResult checkBudget(
            FrozenFeatureGraph graph, long softBudgetMs, long hardBudgetMs) {
        long soft = Math.max(1L, softBudgetMs);
        long hard = Math.max(soft, hardBudgetMs);
        long elapsed = graph == null ? hard + 1L : graph.elapsedMs;
        String state =
                elapsed <= soft
                        ? "WITHIN_SOFT"
                        : (elapsed <= hard ? "SOFT_OVERRUN" : "HARD_OVERRUN");
        boolean accepted = elapsed <= hard;
        return new BudgetResult(
                accepted,
                state,
                elapsed,
                soft,
                hard,
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|BUDGET|"
                                + state
                                + '|'
                                + elapsed
                                + '|'
                                + soft
                                + '|'
                                + hard));
    }

    public ProvenanceAudit auditProvenance(FrozenFeatureGraph graph) {
        ArrayList<String> errors = new ArrayList<String>();
        List<FieldContract> fields = fieldContracts();
        if (graph == null) errors.add("GRAPH_NULL");
        else {
            if (graph.pitHash.length() != 64) errors.add("PIT_HASH");
            if (graph.windowHash.length() != 64) errors.add("WINDOW_HASH");
            if (graph.globalHash.length() != 64) errors.add("GLOBAL_HASH");
            if (graph.numberHash.length() != 64) errors.add("NUMBER_HASH");
            if (graph.historyHash.length() != 64) errors.add("HISTORY_HASH");
            if (graph.leakAudit == null || !graph.leakAudit.passed) errors.add("LEAK_AUDIT");
        }
        StringBuilder payload = new StringBuilder(B04_VERSION).append("|PROVENANCE");
        for (int i = 0; i < fields.size(); i++)
            payload.append('|').append(fields.get(i).canonical());
        payload.append('|')
                .append(graph == null ? "" : graph.featureHash)
                .append('|')
                .append(join(errors));
        return new ProvenanceAudit(
                errors.isEmpty(),
                fields.size(),
                errors,
                V51ChecksumEngine.sha256(payload.toString()));
    }

    public TopologyAudit auditTopology() {
        List<NodeSpec> specs = schema();
        ArrayList<String> errors = new ArrayList<String>();
        ArrayList<String> emitted = new ArrayList<String>();
        for (int i = 0; i < specs.size(); i++) {
            NodeSpec spec = specs.get(i);
            if (emitted.contains(spec.id)) errors.add("DUPLICATE_NODE=" + spec.id);
            for (int d = 0; d < spec.dependencies.length; d++)
                if (!emitted.contains(spec.dependencies[d]))
                    errors.add("FORWARD_OR_MISSING_DEP=" + spec.id + "->" + spec.dependencies[d]);
            emitted.add(spec.id);
        }
        return new TopologyAudit(
                errors.isEmpty(),
                specs.size(),
                errors,
                V51ChecksumEngine.sha256(
                        B04_VERSION + "|TOPOLOGY|" + join(emitted) + '|' + join(errors)));
    }

    private static NodeRecord findNode(List<NodeRecord> nodes, String id) {
        if (nodes == null) return null;
        for (int i = 0; i < nodes.size(); i++)
            if (safe(id).equals(nodes.get(i).id)) return nodes.get(i);
        return null;
    }

    private static boolean sameDependencies(String[] a, String[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (!safe(a[i]).equals(safe(b[i]))) return false;
        return true;
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) out.append(',');
                out.append(safe(values.get(i)));
            }
        return out.toString();
    }

    public static final class FieldContract {
        public final String id, name, kind, owner;
        public final boolean required;
        public final String pointInTimePolicy, missingPolicy;

        FieldContract(
                String id,
                String name,
                String kind,
                String owner,
                boolean required,
                String pointInTimePolicy,
                String missingPolicy) {
            this.id = safe(id);
            this.name = safe(name);
            this.kind = safe(kind);
            this.owner = safe(owner);
            this.required = required;
            this.pointInTimePolicy = safe(pointInTimePolicy);
            this.missingPolicy = safe(missingPolicy);
        }

        public String canonical() {
            return id
                    + ':'
                    + name
                    + ':'
                    + kind
                    + ':'
                    + owner
                    + ':'
                    + (required ? 1 : 0)
                    + ':'
                    + pointInTimePolicy
                    + ':'
                    + missingPolicy;
        }
    }

    public static final class GraphValidation {
        public final boolean passed;
        public final List<String> errors;
        public final String featureHash, replayHash;
        public final int missingCount;
        public final long elapsedMs;

        GraphValidation(
                boolean passed,
                List<String> errors,
                String featureHash,
                String replayHash,
                int missingCount,
                long elapsedMs) {
            this.passed = passed;
            this.errors =
                    errors == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(errors));
            this.featureHash = safe(featureHash);
            this.replayHash = safe(replayHash);
            this.missingCount = Math.max(0, missingCount);
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public String canonical() {
            return (passed ? 1 : 0)
                    + "|"
                    + join(errors)
                    + "|"
                    + featureHash
                    + "|"
                    + replayHash
                    + "|"
                    + missingCount
                    + "|"
                    + elapsedMs;
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|errors="
                    + errors.size()
                    + "|missing="
                    + missingCount
                    + "|cost="
                    + elapsedMs
                    + "ms";
        }
    }

    public static final class ReplayResult {
        public final boolean passed, deterministic;
        public final FrozenFeatureGraph first, second;
        public final GraphValidation firstValidation, secondValidation;
        public final String checksum;
        public final long elapsedMs;

        ReplayResult(
                boolean passed,
                boolean deterministic,
                FrozenFeatureGraph first,
                FrozenFeatureGraph second,
                GraphValidation firstValidation,
                GraphValidation secondValidation,
                String checksum,
                long elapsedMs) {
            this.passed = passed;
            this.deterministic = deterministic;
            this.first = first;
            this.second = second;
            this.firstValidation = firstValidation;
            this.secondValidation = secondValidation;
            this.checksum = safe(checksum);
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|det="
                    + (deterministic ? 1 : 0)
                    + "|cost="
                    + elapsedMs
                    + "ms|hash="
                    + checksum;
        }
    }

    public static final class GraphDiff {
        public final boolean identical;
        public final List<String> changed;
        public final String leftHash, rightHash, checksum;

        GraphDiff(
                boolean identical,
                List<String> changed,
                String leftHash,
                String rightHash,
                String checksum) {
            this.identical = identical;
            this.changed =
                    changed == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(changed));
            this.leftHash = safe(leftHash);
            this.rightHash = safe(rightHash);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return (identical ? "IDENTICAL" : "CHANGED")
                    + "|fields="
                    + changed.size()
                    + "|hash="
                    + checksum;
        }
    }

    public static final class BudgetResult {
        public final boolean accepted;
        public final String state;
        public final long elapsedMs, softBudgetMs, hardBudgetMs;
        public final String checksum;

        BudgetResult(
                boolean accepted,
                String state,
                long elapsedMs,
                long softBudgetMs,
                long hardBudgetMs,
                String checksum) {
            this.accepted = accepted;
            this.state = safe(state);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.softBudgetMs = Math.max(1L, softBudgetMs);
            this.hardBudgetMs = Math.max(this.softBudgetMs, hardBudgetMs);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return state
                    + "|elapsed="
                    + elapsedMs
                    + "|soft="
                    + softBudgetMs
                    + "|hard="
                    + hardBudgetMs
                    + "|hash="
                    + checksum;
        }
    }

    public static final class ProvenanceAudit {
        public final boolean passed;
        public final int contractCount;
        public final List<String> errors;
        public final String checksum;

        ProvenanceAudit(boolean passed, int contractCount, List<String> errors, String checksum) {
            this.passed = passed;
            this.contractCount = Math.max(0, contractCount);
            this.errors =
                    errors == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(errors));
            this.checksum = safe(checksum);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|contracts="
                    + contractCount
                    + "|errors="
                    + errors.size()
                    + "|hash="
                    + checksum;
        }
    }

    public static final class TopologyAudit {
        public final boolean passed;
        public final int nodeCount;
        public final List<String> errors;
        public final String checksum;

        TopologyAudit(boolean passed, int nodeCount, List<String> errors, String checksum) {
            this.passed = passed;
            this.nodeCount = Math.max(0, nodeCount);
            this.errors =
                    errors == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(errors));
            this.checksum = safe(checksum);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|nodes="
                    + nodeCount
                    + "|errors="
                    + errors.size()
                    + "|hash="
                    + checksum;
        }
    }

    public static List<ExecutionRule> executionRules() {
        ArrayList<ExecutionRule> out = new ArrayList<ExecutionRule>();
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-001",
                        "UNIQUE_TARGET_GRAPH",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-002",
                        "TOPOLOGY_ACYCLIC",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-003", "PIT_FIRST", true, "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-004",
                        "ROLLING_AFTER_PIT",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-005",
                        "GLOBAL_AFTER_PIT",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-006",
                        "TRAJECTORY_AFTER_WINDOWS",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-007",
                        "MISSING_AFTER_FEATURES",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-008",
                        "PROVENANCE_AFTER_PIT",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-009",
                        "LEAK_AUDIT_AFTER_PROVENANCE",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-010",
                        "FROZEN_GRAPH_LAST",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-011",
                        "CACHE_BOUND_2",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-012",
                        "SINGLE_BUILD_KEY",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-013",
                        "FEATURE_HASH_REPLAY",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-014",
                        "BUDGET_HARD_LIMIT",
                        true,
                        "FullInformationFeatureGraphEngine"));
        out.add(
                new ExecutionRule(
                        "B04-GRAPH-015",
                        "PROVENANCE_COMPLETE",
                        true,
                        "FullInformationFeatureGraphEngine"));
        return Collections.unmodifiableList(out);
    }

    public static final class ExecutionRule {
        public final String id, name, owner;
        public final boolean required;

        ExecutionRule(String id, String name, boolean required, String owner) {
            this.id = safe(id);
            this.name = safe(name);
            this.required = required;
            this.owner = safe(owner);
        }

        public String canonical() {
            return id + ":" + name + ":" + (required ? 1 : 0) + ":" + owner;
        }
    }
}
