package com.ethk3.novaonepro;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * R929 / batch-3 canonical point-in-time feature contract.
 *
 * <p>This class deliberately separates semantics from tensor shape.  An artifact can only consume a
 * feature surface when both the stage and the manifest hash match.  That closes the old ambiguity
 * where two 96-dimensional arrays with different meaning could be treated as interchangeable.</p>
 *
 * <p>The production dependency graph is fixed and acyclic:
 * RAW_HISTORY -> BASE_EXPERTS -> NUMBER_Q -> ACTION_META_1200 -> ACTION_RANKER
 * -> SELECTED_ACTION -> CALIBRATION -> POLICY.  Labels never appear in the graph; they are joined
 * after the target draw for training/evaluation only.</p>
 */
public final class FeatureDagContractR929 {
    public static final String REVISION = "R929_FEATURE_DAG_V1";
    public static final int ACTION_META_DIM = 96;
    public static final int ACTIONS_PER_POSITION = 120;
    public static final int TOTAL_ACTIONS = 1200;
    public static final int POSITIONS = 10;
    public static final int K = 3;
    public static final int EXPERT_COUNT = 9;

    private FeatureDagContractR929() {}

    public enum Stage {
        RAW_HISTORY,
        BASE_EXPERTS,
        NUMBER_Q,
        ACTION_META_1200,
        ACTION_RANKER,
        SELECTED_ACTION,
        CALIBRATION,
        POLICY
    }

    public enum MissingPolicy {
        REQUIRED,
        MASK_AND_REASON,
        TRAINING_DEFINED_FALLBACK
    }

    public static final class FeatureSpec {
        public final int index;
        public final String name;
        public final String semantic;
        public final String unit;
        public final double min;
        public final double max;
        public final int lag;
        public final int window;
        public final String source;
        public final MissingPolicy missingPolicy;
        public final String costClass;
        public final String version;

        FeatureSpec(int index, String name, String semantic, String unit, double min, double max,
                    int lag, int window, String source, MissingPolicy missingPolicy,
                    String costClass, String version) {
            this.index = index;
            this.name = safe(name);
            this.semantic = safe(semantic);
            this.unit = safe(unit);
            this.min = min;
            this.max = max;
            this.lag = lag;
            this.window = window;
            this.source = safe(source);
            this.missingPolicy = missingPolicy == null ? MissingPolicy.REQUIRED : missingPolicy;
            this.costClass = safe(costClass);
            this.version = safe(version);
            validate();
        }

        private void validate() {
            if (index < 0 || name.length() == 0 || semantic.length() == 0 || unit.length() == 0
                    || source.length() == 0 || version.length() == 0)
                throw new IllegalArgumentException("R929_FEATURE_SPEC_EMPTY:" + index);
            if (!finite(min) || !finite(max) || min > max)
                throw new IllegalArgumentException("R929_FEATURE_RANGE:" + name);
            if (lag < 0 || window < 0)
                throw new IllegalArgumentException("R929_FEATURE_TIME:" + name);
        }

        String canonical() {
            return index + "|" + name + "|" + semantic + "|" + unit + "|" + fmt(min) + "|"
                    + fmt(max) + "|lag=" + lag + "|window=" + window + "|source=" + source
                    + "|missing=" + missingPolicy.name() + "|cost=" + costClass
                    + "|version=" + version;
        }
    }

    public static final class NodeSpec {
        public final Stage stage;
        public final String semantic;
        public final String type;
        public final String shape;
        public final List<Stage> parents;
        public final boolean labelFree;

        NodeSpec(Stage stage, String semantic, String type, String shape, boolean labelFree,
                 Stage... parents) {
            this.stage = stage;
            this.semantic = safe(semantic);
            this.type = safe(type);
            this.shape = safe(shape);
            this.labelFree = labelFree;
            ArrayList<Stage> p = new ArrayList<Stage>();
            if (parents != null) Collections.addAll(p, parents);
            this.parents = Collections.unmodifiableList(p);
            if (stage == null || this.semantic.length() == 0 || this.type.length() == 0
                    || this.shape.length() == 0)
                throw new IllegalArgumentException("R929_NODE_SPEC_INVALID");
        }

        String canonical() {
            StringBuilder b = new StringBuilder();
            b.append(stage.name()).append('|').append(semantic).append('|').append(type)
                    .append('|').append(shape).append("|labelFree=").append(labelFree)
                    .append("|parents=");
            for (int i = 0; i < parents.size(); i++) {
                if (i > 0) b.append(',');
                b.append(parents.get(i).name());
            }
            return b.toString();
        }
    }

    /** Per-value provenance.  A valid numeric zero is distinct from a missing value. */
    public static final class ValueResult {
        public final double value;
        public final boolean present;
        public final String missingReason;
        public final String maxSourceIssue;
        public final long availableAtMs;
        public final long costNs;
        public final String valueHash;

        public ValueResult(double value, boolean present, String missingReason, String maxSourceIssue,
                           long availableAtMs, long costNs, String valueHash) {
            this.value = value;
            this.present = present;
            this.missingReason = safe(missingReason);
            this.maxSourceIssue = safe(maxSourceIssue);
            this.availableAtMs = availableAtMs;
            this.costNs = Math.max(0L, costNs);
            this.valueHash = safe(valueHash);
            if (present && !finite(value)) throw new IllegalArgumentException("R929_VALUE_NONFINITE");
            if (!present && this.missingReason.length() == 0)
                throw new IllegalArgumentException("R929_MISSING_REASON_REQUIRED");
        }
    }

    public static final class Manifest {
        public final String revision;
        public final Stage stage;
        public final List<FeatureSpec> features;
        public final Map<Stage, NodeSpec> dag;
        public final String hash;

        Manifest(String revision, Stage stage, List<FeatureSpec> features, Map<Stage, NodeSpec> dag) {
            this.revision = safe(revision);
            this.stage = stage;
            this.features = Collections.unmodifiableList(new ArrayList<FeatureSpec>(features));
            this.dag = Collections.unmodifiableMap(new LinkedHashMap<Stage, NodeSpec>(dag));
            validateDag(this.dag);
            validateFeatureOrdinals(this.features);
            this.hash = V51ChecksumEngine.sha256(canonicalWithoutHash());
        }

        public String canonicalWithoutHash() {
            StringBuilder b = new StringBuilder(16384);
            b.append("revision=").append(revision).append('\n');
            b.append("stage=").append(stage == null ? "" : stage.name()).append('\n');
            ArrayList<NodeSpec> nodes = new ArrayList<NodeSpec>(dag.values());
            Collections.sort(nodes, new Comparator<NodeSpec>() {
                public int compare(NodeSpec a, NodeSpec c) {
                    return a.stage.ordinal() - c.stage.ordinal();
                }
            });
            for (NodeSpec node : nodes) b.append("node=").append(node.canonical()).append('\n');
            for (FeatureSpec feature : features)
                b.append("feature=").append(feature.canonical()).append('\n');
            return b.toString();
        }

        public void requireCompatible(Stage expectedStage, int expectedDim, String expectedHash) {
            if (stage != expectedStage)
                throw new IllegalStateException("R929_INPUT_STAGE_MISMATCH|expected="
                        + expectedStage + "|actual=" + stage);
            if (features.size() != expectedDim)
                throw new IllegalStateException("R929_INPUT_DIM_MISMATCH|expected="
                        + expectedDim + "|actual=" + features.size());
            if (expectedHash != null && expectedHash.length() > 0 && !hash.equals(expectedHash))
                throw new IllegalStateException("R929_SCHEMA_HASH_MISMATCH|expected="
                        + expectedHash + "|actual=" + hash);
        }
    }

    public static Manifest actionMeta96Manifest() {
        ArrayList<FeatureSpec> x = new ArrayList<FeatureSpec>(ACTION_META_DIM);
        add(x, "raw_action_p", "pre-calibration K3 raw action probability surface", "probability", 0, 1, 1, 0,
                "V51PositionActionBuilder.rawActionP", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "third_margin", "margin at the third-number decision boundary", "score", -10, 10, 1, 0,
                "V51PositionActionBuilder.thirdMargin", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "entropy", "action/position uncertainty entropy", "nat", 0, 10, 1, 0,
                "V51PositionActionBuilder.entropy", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "stability", "frozen predraw action stability", "ratio", 0, 1, 1, 0,
                "V51PositionActionBuilder.stability", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "position_norm", "position ordinal normalized to [0,1]", "ratio", 0, 1, 0, 0,
                "action.position", MissingPolicy.REQUIRED, "O1", "R929");
        add(x, "raw_action_p_centered", "action probability minus issue action mean", "probability", -1, 1, 1, 0,
                "V51ActionMetaFeatureEngine.issueMean", MissingPolicy.REQUIRED, "O1", "R929");
        add(x, "raw_action_p_z", "issue standardized raw action probability", "zscore", -100, 100, 1, 0,
                "V51ActionMetaFeatureEngine.issueSd", MissingPolicy.REQUIRED, "O1", "R929");
        add(x, "n1_norm", "first K3 number normalized", "ratio", 0.1, 1, 0, 0, "action.n1", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "n2_norm", "second K3 number normalized", "ratio", 0.1, 1, 0, 0, "action.n2", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "n3_norm", "third K3 number normalized", "ratio", 0.1, 1, 0, 0, "action.n3", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "n4_norm_legacy", "legacy fourth-number audit slot; never expands fixed K3", "ratio", 0, 1, 0, 0,
                "action.n4", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "state_posterior", "predraw state posterior", "probability", 0, 1, 1, 0,
                "V51StateTrajectoryEngine.posterior", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "state_remaining_life_norm", "predraw state expected remaining life / 20", "ratio", -10, 10, 1, 0,
                "V51StateTrajectoryEngine.remainingLife", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "state_change_risk", "predraw state change-point risk", "probability", 0, 1, 1, 0,
                "V51StateTrajectoryEngine.changePointRisk", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "state_confidence", "predraw state confidence", "ratio", 0, 1, 1, 0,
                "V51StateTrajectoryEngine.confidence", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "consensus_distribution", "expert distribution agreement", "ratio", -10, 10, 1, 0,
                "V51EffectiveConsensusEngine.distributionAgreement", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "consensus_top3", "expert top3 overlap", "ratio", -10, 10, 1, 0,
                "V51EffectiveConsensusEngine.top3Overlap", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "consensus_rank", "expert rank agreement", "ratio", -10, 10, 1, 0,
                "V51EffectiveConsensusEngine.rankAgreement", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "consensus_direction", "expert directional agreement", "ratio", -10, 10, 1, 0,
                "V51EffectiveConsensusEngine.directionalAgreement", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "consensus_signal", "expert fused signal strength", "score", -100, 100, 1, 0,
                "V51EffectiveConsensusEngine.signalStrength", MissingPolicy.REQUIRED, "O1", "B10");
        add(x, "consensus_effective", "expert consensus effective flag/strength", "ratio", 0, 1, 1, 0,
                "V51EffectiveConsensusEngine.effective", MissingPolicy.REQUIRED, "O1", "B10");
        for (int e = 0; e < EXPERT_COUNT; e++) {
            String prefix = "expert" + e + "_";
            add(x, prefix + "trust", "frozen expert trust at position", "ratio", -10, 10, 1, 0,
                    "ExpertEvidence[" + e + "].trust", MissingPolicy.TRAINING_DEFINED_FALLBACK, "O1", "B10");
            add(x, prefix + "uncertainty", "frozen expert uncertainty at position", "ratio", -10, 10, 1, 0,
                    "ExpertEvidence[" + e + "].uncertainty", MissingPolicy.TRAINING_DEFINED_FALLBACK, "O1", "B10");
            add(x, prefix + "signal", "frozen expert signal strength at position", "score", -100, 100, 1, 0,
                    "ExpertEvidence[" + e + "].signalStrength", MissingPolicy.TRAINING_DEFINED_FALLBACK, "O1", "B10");
            add(x, prefix + "gate", "predraw Gate9 weight", "weight", 0, 1, 1, 0,
                    "V51ExpertGateEngine.gate[" + e + "]", MissingPolicy.TRAINING_DEFINED_FALLBACK, "O1", "B10");
            add(x, prefix + "k3_mean_raw", "mean raw expert score over selected K3", "score", -1e6, 1e6, 1, 0,
                    "ExpertEvidence[" + e + "].rawScore", MissingPolicy.TRAINING_DEFINED_FALLBACK, "O1", "B10");
        }
        while (x.size() < ACTION_META_DIM) {
            int slot = x.size() - 66;
            add(x, String.format(Locale.US, "global_or_interaction_%02d", slot),
                    "ordered predraw global feature when available; otherwise deterministic registered interaction fallback",
                    "normalized_score", -1e6, 1e6, 1, 0,
                    "PermutationFeatureSnapshot.globalFeatures/fallbackInteraction",
                    MissingPolicy.TRAINING_DEFINED_FALLBACK, "O1", "R929");
        }
        return new Manifest(REVISION, Stage.ACTION_META_1200, x, defaultDag());
    }

    public static Map<Stage, NodeSpec> defaultDag() {
        LinkedHashMap<Stage, NodeSpec> g = new LinkedHashMap<Stage, NodeSpec>();
        g.put(Stage.RAW_HISTORY, new NodeSpec(Stage.RAW_HISTORY,
                "immutable predraw raw history and typed slow-state inputs", "PIT_CONTEXT", "variable", true));
        g.put(Stage.BASE_EXPERTS, new NodeSpec(Stage.BASE_EXPERTS,
                "nine frozen base-expert logits/evidence", "EXPERT_LOGITS", "9x10x10", true,
                Stage.RAW_HISTORY));
        g.put(Stage.NUMBER_Q, new NodeSpec(Stage.NUMBER_Q,
                "10x10 number probability surface", "NUMBER_SURFACE", "10x10", true,
                Stage.RAW_HISTORY, Stage.BASE_EXPERTS));
        g.put(Stage.ACTION_META_1200, new NodeSpec(Stage.ACTION_META_1200,
                "complete fixed-K3 1200 action meta surface", "ACTION_META", "1200x96", true,
                Stage.RAW_HISTORY, Stage.BASE_EXPERTS, Stage.NUMBER_Q));
        g.put(Stage.ACTION_RANKER, new NodeSpec(Stage.ACTION_RANKER,
                "rank score for all 1200 actions", "RANK_SCORE", "1200", true,
                Stage.ACTION_META_1200));
        g.put(Stage.SELECTED_ACTION, new NodeSpec(Stage.SELECTED_ACTION,
                "deterministically selected one action before policy", "SELECTED_ACTION", "1", true,
                Stage.ACTION_RANKER));
        g.put(Stage.CALIBRATION, new NodeSpec(Stage.CALIBRATION,
                "selected-action calibrated hit probability and support", "CALIBRATION", "1", true,
                Stage.SELECTED_ACTION));
        g.put(Stage.POLICY, new NodeSpec(Stage.POLICY,
                "single policy surface consumed by DecisionGateEngine", "POLICY", "1", true,
                Stage.CALIBRATION));
        validateDag(g);
        return g;
    }

    public static void validateActionSurface(V51Models.PositionAction[] actions) {
        if (actions == null || actions.length != TOTAL_ACTIONS)
            throw new IllegalArgumentException("R929_ACTION_COUNT_NE_1200");
        int ordinal = 0;
        for (int position = 1; position <= POSITIONS; position++) {
            for (int a = 1; a <= 8; a++) {
                for (int b = a + 1; b <= 9; b++) {
                    for (int c = b + 1; c <= 10; c++) {
                        V51Models.PositionAction x = actions[ordinal];
                        if (x == null || x.position != position || x.n1 != a || x.n2 != b || x.n3 != c)
                            throw new IllegalArgumentException("R929_ACTION_ORDINAL:" + ordinal);
                        if (x.metaFeatures == null || x.metaFeatures.length != ACTION_META_DIM)
                            throw new IllegalArgumentException("R929_META96:" + ordinal);
                        for (int k = 0; k < ACTION_META_DIM; k++) {
                            if (!finite(x.metaFeatures[k]))
                                throw new IllegalArgumentException("R929_META_NONFINITE:" + ordinal + ':' + k);
                        }
                        ordinal++;
                    }
                }
            }
        }
        if (ordinal != TOTAL_ACTIONS) throw new IllegalStateException("R929_ACTION_SPACE_DRIFT");
    }

    public static void requirePointInTime(String maxSourceIssue, String targetIssue, long availableAtMs,
                                          long drawAtMs) {
        String max = safe(maxSourceIssue), target = safe(targetIssue);
        if (max.length() == 0 || target.length() == 0 || max.compareTo(target) >= 0)
            throw new IllegalArgumentException("R929_TIME_TRAVEL_ISSUE|max=" + max + "|target=" + target);
        if (availableAtMs <= 0 || drawAtMs <= 0 || availableAtMs >= drawAtMs)
            throw new IllegalArgumentException("R929_TIME_TRAVEL_CLOCK|available=" + availableAtMs
                    + "|draw=" + drawAtMs);
    }

    public static String artifactBinding(Stage inputStage, String schemaHash, String parentArtifactHash,
                                         String trainedUntilIssue) {
        if (inputStage == null) throw new IllegalArgumentException("R929_ARTIFACT_STAGE_REQUIRED");
        String schema = requireHash("schema", schemaHash);
        String parent = requireHash("parent", parentArtifactHash);
        String trained = safe(trainedUntilIssue);
        if (trained.length() == 0) throw new IllegalArgumentException("R929_TRAINED_UNTIL_REQUIRED");
        return V51ChecksumEngine.sha256("R929_ARTIFACT|" + inputStage.name() + '|' + schema + '|'
                + parent + '|' + trained);
    }

    private static void add(List<FeatureSpec> out, String name, String semantic, String unit,
                            double min, double max, int lag, int window, String source,
                            MissingPolicy missing, String cost, String version) {
        out.add(new FeatureSpec(out.size(), name, semantic, unit, min, max, lag, window,
                source, missing, cost, version));
    }

    private static void validateFeatureOrdinals(List<FeatureSpec> features) {
        if (features == null) throw new IllegalArgumentException("R929_FEATURES_NULL");
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < features.size(); i++) {
            FeatureSpec x = features.get(i);
            if (x == null || x.index != i) throw new IllegalArgumentException("R929_FEATURE_ORDINAL:" + i);
            if (!names.add(x.name)) throw new IllegalArgumentException("R929_FEATURE_DUP_NAME:" + x.name);
        }
    }

    public static void validateDag(Map<Stage, NodeSpec> dag) {
        if (dag == null || dag.size() != Stage.values().length)
            throw new IllegalArgumentException("R929_DAG_NODE_COUNT");
        for (Stage s : Stage.values()) if (!dag.containsKey(s))
            throw new IllegalArgumentException("R929_DAG_MISSING:" + s);
        Map<Stage, Integer> indegree = new HashMap<Stage, Integer>();
        Map<Stage, List<Stage>> children = new HashMap<Stage, List<Stage>>();
        for (Stage s : Stage.values()) {
            indegree.put(s, 0);
            children.put(s, new ArrayList<Stage>());
        }
        for (NodeSpec node : dag.values()) {
            if (!node.labelFree) throw new IllegalArgumentException("R929_LABEL_IN_PREDRAW_DAG:" + node.stage);
            LinkedHashSet<Stage> unique = new LinkedHashSet<Stage>();
            for (Stage p : node.parents) {
                if (p == null || p == node.stage || !dag.containsKey(p) || !unique.add(p))
                    throw new IllegalArgumentException("R929_DAG_PARENT_INVALID:" + node.stage);
                indegree.put(node.stage, indegree.get(node.stage) + 1);
                children.get(p).add(node.stage);
            }
        }
        ArrayDeque<Stage> q = new ArrayDeque<Stage>();
        for (Stage s : Stage.values()) if (indegree.get(s) == 0) q.add(s);
        int visited = 0;
        while (!q.isEmpty()) {
            Stage s = q.removeFirst(); visited++;
            for (Stage child : children.get(s)) {
                int n = indegree.get(child) - 1;
                indegree.put(child, n);
                if (n == 0) q.addLast(child);
            }
        }
        if (visited != dag.size()) throw new IllegalArgumentException("R929_DAG_CYCLE");
    }

    private static String requireHash(String field, String value) {
        String x = safe(value);
        if (!x.matches("[0-9a-fA-F]{64}")) throw new IllegalArgumentException("R929_HASH_" + field);
        return x.toLowerCase(Locale.US);
    }

    private static boolean finite(double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x);
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }
    private static String fmt(double x) { return String.format(Locale.US, "%.12g", x); }
}
