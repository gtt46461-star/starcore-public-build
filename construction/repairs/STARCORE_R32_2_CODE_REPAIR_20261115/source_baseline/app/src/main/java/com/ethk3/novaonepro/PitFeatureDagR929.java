package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * R929 typed point-in-time feature graph.
 *
 * <p>This class is deliberately independent from labels and from Android UI code.  A production
 * feature node is accepted only when its semantic identity, shape, parents, source issue,
 * availability time, missing mask, cost and source hash are all explicit.  Legal numeric zero is
 * represented by present=true and can never be confused with a missing value.</p>
 *
 * <p>The graph establishes the only valid direction:
 * raw past window -> typed graph -> experts / NumberQ -> META1200 -> ranker.  Post-draw labels are
 * joined through {@link LabelJoin} after a frozen surface exists and therefore cannot be a parent
 * of a production feature.</p>
 */
public final class PitFeatureDagR929 {
    public static final String VERSION = "R929_PIT_FEATURE_DAG_V1";
    public static final String GAME = "Binance1-PK10";
    public static final int ACTION_COUNT = 1200;
    public static final int POSITIONS = 10;
    public static final int NUMBERS = 10;
    public static final int K = 3;

    private PitFeatureDagR929() {}

    public enum ValueType { SCALAR, VECTOR, MATRIX, CATEGORY, MASK }
    public enum TemporalKind { PREDRAW, POSTDRAW_LABEL, AUDIT_ONLY }
    public enum MissingReason {
        NONE,
        SOURCE_ABSENT,
        SOURCE_LATE,
        SOURCE_INVALID,
        DEPENDENCY_MISSING,
        DOMAIN_UNSUPPORTED,
        LEGACY_UNKNOWN
    }

    public static final class Shape {
        public final int rows;
        public final int columns;

        public Shape(int rows, int columns) {
            if (rows <= 0 || columns <= 0) throw new IllegalArgumentException("R929_SHAPE_INVALID");
            this.rows = rows;
            this.columns = columns;
        }

        public int elements() {
            long n = (long) rows * (long) columns;
            if (n > Integer.MAX_VALUE) throw new IllegalStateException("R929_SHAPE_OVERFLOW");
            return (int) n;
        }

        public String canonical() { return rows + "x" + columns; }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Shape)) return false;
            Shape value = (Shape) other;
            return rows == value.rows && columns == value.columns;
        }

        @Override public int hashCode() { return 31 * rows + columns; }
    }

    public static final class FeatureSpec {
        public final String id;
        public final String semantic;
        public final String unit;
        public final ValueType valueType;
        public final Shape shape;
        public final TemporalKind temporalKind;
        public final List<String> parents;
        public final int lag;
        public final int window;
        public final String source;
        public final String missingPolicy;
        public final long costBudgetNs;
        public final String version;
        public final String specHash;

        public FeatureSpec(
                String id,
                String semantic,
                String unit,
                ValueType valueType,
                Shape shape,
                TemporalKind temporalKind,
                List<String> parents,
                int lag,
                int window,
                String source,
                String missingPolicy,
                long costBudgetNs,
                String version) {
            this.id = required(id, "FEATURE_ID");
            this.semantic = required(semantic, "FEATURE_SEMANTIC");
            this.unit = required(unit, "FEATURE_UNIT");
            if (valueType == null || shape == null || temporalKind == null)
                throw new IllegalArgumentException("R929_FEATURE_TYPE_SHAPE_TEMPORAL_REQUIRED");
            this.valueType = valueType;
            this.shape = shape;
            this.temporalKind = temporalKind;
            ArrayList<String> copy = new ArrayList<String>();
            if (parents != null) {
                for (String parent : parents) copy.add(required(parent, "FEATURE_PARENT"));
            }
            this.parents = Collections.unmodifiableList(copy);
            if (lag < 0 || window < 0 || costBudgetNs < 0L)
                throw new IllegalArgumentException("R929_FEATURE_RANGE_INVALID");
            this.lag = lag;
            this.window = window;
            this.source = required(source, "FEATURE_SOURCE");
            this.missingPolicy = required(missingPolicy, "FEATURE_MISSING_POLICY");
            this.costBudgetNs = costBudgetNs;
            this.version = required(version, "FEATURE_VERSION");
            this.specHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(256);
            out.append(id).append('|').append(semantic).append('|').append(unit).append('|')
                    .append(valueType.name()).append('|').append(shape.canonical()).append('|')
                    .append(temporalKind.name()).append('|').append(lag).append('|').append(window)
                    .append('|').append(source).append('|').append(missingPolicy).append('|')
                    .append(costBudgetNs).append('|').append(version).append("|parents=");
            for (int i = 0; i < parents.size(); i++) {
                if (i > 0) out.append(',');
                out.append(parents.get(i));
            }
            return out.toString();
        }

        public String canonical() { return canonicalWithoutHash() + "|specHash=" + specHash; }
    }

    /** Immutable feature value plus its complete provenance. */
    public static final class ValueResult {
        private final double[] values;
        private final boolean[] present;
        private final MissingReason[] missingReasons;
        public final String maxSourceIssue;
        public final long availableAtMs;
        public final long costNs;
        public final String sourceHash;
        public final String valueHash;

        public ValueResult(
                double[] values,
                boolean[] present,
                MissingReason[] missingReasons,
                String maxSourceIssue,
                long availableAtMs,
                long costNs,
                String sourceHash) {
            if (values == null || present == null || missingReasons == null
                    || values.length == 0 || values.length != present.length
                    || values.length != missingReasons.length)
                throw new IllegalArgumentException("R929_VALUE_SHAPE_INVALID");
            if (availableAtMs < 0L || costNs < 0L)
                throw new IllegalArgumentException("R929_VALUE_TIME_COST_INVALID");
            this.values = Arrays.copyOf(values, values.length);
            this.present = Arrays.copyOf(present, present.length);
            this.missingReasons = Arrays.copyOf(missingReasons, missingReasons.length);
            this.maxSourceIssue = clean(maxSourceIssue);
            this.availableAtMs = availableAtMs;
            this.costNs = costNs;
            this.sourceHash = required(sourceHash, "VALUE_SOURCE_HASH");
            this.valueHash = sha256(canonicalWithoutHash());
        }

        public static ValueResult present(
                double[] values, String maxSourceIssue, long availableAtMs, long costNs,
                String sourceHash) {
            if (values == null) throw new IllegalArgumentException("R929_VALUES_NULL");
            boolean[] mask = new boolean[values.length];
            MissingReason[] reasons = new MissingReason[values.length];
            Arrays.fill(mask, true);
            Arrays.fill(reasons, MissingReason.NONE);
            return new ValueResult(values, mask, reasons, maxSourceIssue, availableAtMs, costNs,
                    sourceHash);
        }

        public static ValueResult missing(
                int size, MissingReason reason, String maxSourceIssue, long availableAtMs,
                long costNs, String sourceHash) {
            if (size <= 0 || reason == null || reason == MissingReason.NONE)
                throw new IllegalArgumentException("R929_MISSING_FACTORY_INVALID");
            double[] values = new double[size];
            boolean[] mask = new boolean[size];
            MissingReason[] reasons = new MissingReason[size];
            Arrays.fill(reasons, reason);
            return new ValueResult(values, mask, reasons, maxSourceIssue, availableAtMs, costNs,
                    sourceHash);
        }

        public int size() { return values.length; }
        public double value(int index) { return values[index]; }
        public boolean isPresent(int index) { return present[index]; }
        public MissingReason missingReason(int index) { return missingReasons[index]; }
        public double[] valuesCopy() { return Arrays.copyOf(values, values.length); }
        public boolean[] presentCopy() { return Arrays.copyOf(present, present.length); }
        public MissingReason[] missingReasonsCopy() {
            return Arrays.copyOf(missingReasons, missingReasons.length);
        }

        public int presentCount() {
            int count = 0;
            for (boolean value : present) if (value) count++;
            return count;
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(values.length * 28 + 128);
            out.append(maxSourceIssue).append('|').append(availableAtMs).append('|').append(costNs)
                    .append('|').append(sourceHash).append('|');
            for (int i = 0; i < values.length; i++) {
                if (i > 0) out.append(';');
                out.append(present[i] ? '1' : '0').append(':')
                        .append(Long.toHexString(Double.doubleToLongBits(values[i]))).append(':')
                        .append(missingReasons[i].name());
            }
            return out.toString();
        }

        public String canonical() { return canonicalWithoutHash() + "|valueHash=" + valueHash; }
    }

    public interface Computer {
        ValueResult compute(ReadOnlyContext context);
    }

    public static final class Node {
        public final FeatureSpec spec;
        public final Computer computer;

        public Node(FeatureSpec spec, Computer computer) {
            if (spec == null || computer == null)
                throw new IllegalArgumentException("R929_NODE_REQUIRED");
            this.spec = spec;
            this.computer = computer;
        }
    }

    public static final class Manifest {
        public final String game;
        public final String inputStage;
        public final String schemaVersion;
        public final List<FeatureSpec> features;
        public final List<String> topologicalOrder;
        public final String manifestHash;
        private final Map<String, FeatureSpec> byId;

        public Manifest(String game, String inputStage, String schemaVersion,
                List<FeatureSpec> features) {
            this.game = required(game, "MANIFEST_GAME");
            this.inputStage = required(inputStage, "MANIFEST_INPUT_STAGE");
            this.schemaVersion = required(schemaVersion, "MANIFEST_SCHEMA");
            if (features == null || features.isEmpty())
                throw new IllegalArgumentException("R929_MANIFEST_EMPTY");
            TreeMap<String, FeatureSpec> sorted = new TreeMap<String, FeatureSpec>();
            for (FeatureSpec spec : features) {
                if (spec == null || sorted.put(spec.id, spec) != null)
                    throw new IllegalArgumentException("R929_DUPLICATE_FEATURE");
            }
            for (FeatureSpec spec : sorted.values()) {
                for (String parent : spec.parents) {
                    if (!sorted.containsKey(parent))
                        throw new IllegalArgumentException("R929_UNKNOWN_PARENT:" + parent);
                    if (parent.equals(spec.id))
                        throw new IllegalArgumentException("R929_SELF_CYCLE:" + parent);
                }
                if (spec.temporalKind == TemporalKind.POSTDRAW_LABEL)
                    throw new IllegalArgumentException("R929_LABEL_IN_PRODUCTION_DAG:" + spec.id);
            }
            this.byId = Collections.unmodifiableMap(
                    new LinkedHashMap<String, FeatureSpec>(sorted));
            ArrayList<String> order = new ArrayList<String>();
            HashMap<String, Integer> color = new HashMap<String, Integer>();
            for (String id : sorted.keySet()) visit(id, sorted, color, order);
            this.topologicalOrder = Collections.unmodifiableList(order);
            ArrayList<FeatureSpec> orderedFeatures = new ArrayList<FeatureSpec>();
            for (String id : order) orderedFeatures.add(sorted.get(id));
            this.features = Collections.unmodifiableList(orderedFeatures);
            this.manifestHash = sha256(canonicalWithoutHash());
        }

        public FeatureSpec require(String id) {
            FeatureSpec spec = byId.get(id);
            if (spec == null) throw new IllegalArgumentException("R929_FEATURE_NOT_REGISTERED:" + id);
            return spec;
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(1024);
            out.append(game).append('|').append(inputStage).append('|').append(schemaVersion);
            for (FeatureSpec spec : features) out.append('\n').append(spec.canonical());
            return out.toString();
        }

        public String canonical() { return canonicalWithoutHash() + "\nmanifestHash=" + manifestHash; }

        private static void visit(String id, Map<String, FeatureSpec> specs,
                Map<String, Integer> color, List<String> order) {
            Integer state = color.get(id);
            if (state != null && state.intValue() == 2) return;
            if (state != null && state.intValue() == 1)
                throw new IllegalArgumentException("R929_FEATURE_CYCLE:" + id);
            color.put(id, Integer.valueOf(1));
            FeatureSpec spec = specs.get(id);
            ArrayList<String> parents = new ArrayList<String>(spec.parents);
            Collections.sort(parents);
            for (String parent : parents) visit(parent, specs, color, order);
            color.put(id, Integer.valueOf(2));
            order.add(id);
        }
    }

    public static final class ReadOnlyContext {
        public final String targetIssue;
        public final long frozenAtMs;
        private final Map<String, ValueResult> values;

        ReadOnlyContext(String targetIssue, long frozenAtMs, Map<String, ValueResult> values) {
            this.targetIssue = targetIssue;
            this.frozenAtMs = frozenAtMs;
            this.values = Collections.unmodifiableMap(
                    new LinkedHashMap<String, ValueResult>(values));
        }

        public ValueResult require(String id) {
            ValueResult value = values.get(id);
            if (value == null) throw new IllegalStateException("R929_PARENT_NOT_COMPUTED:" + id);
            return value;
        }

        public Map<String, ValueResult> snapshot() { return values; }
    }

    public static final class FrozenGraph {
        public final Manifest manifest;
        public final String targetIssue;
        public final long frozenAtMs;
        public final Map<String, ValueResult> values;
        public final String maxSourceIssue;
        public final long totalCostNs;
        public final String graphHash;

        FrozenGraph(Manifest manifest, String targetIssue, long frozenAtMs,
                Map<String, ValueResult> values, String maxSourceIssue, long totalCostNs) {
            this.manifest = manifest;
            this.targetIssue = targetIssue;
            this.frozenAtMs = frozenAtMs;
            this.values = Collections.unmodifiableMap(
                    new LinkedHashMap<String, ValueResult>(values));
            this.maxSourceIssue = maxSourceIssue;
            this.totalCostNs = totalCostNs;
            this.graphHash = sha256(canonicalWithoutHash());
        }

        public ValueResult require(String id) {
            ValueResult value = values.get(id);
            if (value == null) throw new IllegalArgumentException("R929_FROZEN_VALUE_MISSING:" + id);
            return value;
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(2048);
            out.append(VERSION).append('|').append(manifest.manifestHash).append('|')
                    .append(targetIssue).append('|').append(frozenAtMs).append('|')
                    .append(maxSourceIssue).append('|').append(totalCostNs);
            for (String id : manifest.topologicalOrder) {
                ValueResult value = values.get(id);
                out.append('\n').append(id).append('=').append(value.valueHash);
            }
            return out.toString();
        }

        public String canonical() { return canonicalWithoutHash() + "\ngraphHash=" + graphHash; }
    }

    /** Computes and freezes a graph in deterministic topological order. */
    public static FrozenGraph freeze(
            Manifest manifest,
            String targetIssue,
            long frozenAtMs,
            Map<String, Node> nodes) {
        if (manifest == null || nodes == null || frozenAtMs <= 0L)
            throw new IllegalArgumentException("R929_FREEZE_INPUT_INVALID");
        String target = required(targetIssue, "TARGET_ISSUE");
        if (!GAME.equals(manifest.game))
            throw new IllegalArgumentException("R929_WRONG_GAME:" + manifest.game);
        LinkedHashMap<String, ValueResult> computed = new LinkedHashMap<String, ValueResult>();
        String maxSource = "";
        long totalCost = 0L;
        for (String id : manifest.topologicalOrder) {
            Node node = nodes.get(id);
            if (node == null || !node.spec.specHash.equals(manifest.require(id).specHash))
                throw new IllegalArgumentException("R929_NODE_SPEC_MISMATCH:" + id);
            ValueResult result = node.computer.compute(new ReadOnlyContext(target, frozenAtMs, computed));
            validateValue(node.spec, result, target, frozenAtMs, computed);
            if (node.spec.costBudgetNs > 0L && result.costNs > node.spec.costBudgetNs)
                throw new IllegalStateException("R929_NODE_COST_BUDGET:" + id);
            computed.put(id, result);
            totalCost = saturatedAdd(totalCost, result.costNs);
            if (compareIssues(result.maxSourceIssue, maxSource) > 0) maxSource = result.maxSourceIssue;
        }
        if (computed.size() != manifest.features.size())
            throw new IllegalStateException("R929_GRAPH_INCOMPLETE");
        return new FrozenGraph(manifest, target, frozenAtMs, computed, maxSource, totalCost);
    }

    private static void validateValue(
            FeatureSpec spec,
            ValueResult value,
            String targetIssue,
            long frozenAtMs,
            Map<String, ValueResult> parents) {
        if (value == null || value.size() != spec.shape.elements())
            throw new IllegalArgumentException("R929_VALUE_SPEC_SHAPE:" + spec.id);
        if (value.availableAtMs > frozenAtMs)
            throw new IllegalArgumentException("R929_VALUE_AVAILABLE_AFTER_FREEZE:" + spec.id);
        if (spec.temporalKind == TemporalKind.PREDRAW
                && !"CONSTANT".equals(spec.source)
                && !isStrictPast(value.maxSourceIssue, targetIssue))
            throw new IllegalArgumentException("R929_TIME_TRAVEL:" + spec.id);
        for (String parent : spec.parents) {
            ValueResult parentValue = parents.get(parent);
            if (parentValue == null)
                throw new IllegalArgumentException("R929_PARENT_VALUE_MISSING:" + parent);
            if (parentValue.availableAtMs > value.availableAtMs)
                throw new IllegalArgumentException("R929_PARENT_AVAILABLE_AFTER_CHILD:" + spec.id);
            if (compareIssues(parentValue.maxSourceIssue, value.maxSourceIssue) > 0)
                throw new IllegalArgumentException("R929_PARENT_SOURCE_AFTER_CHILD:" + spec.id);
        }
        for (int i = 0; i < value.size(); i++) {
            boolean present = value.isPresent(i);
            MissingReason reason = value.missingReason(i);
            double number = value.value(i);
            if (present) {
                if (reason != MissingReason.NONE)
                    throw new IllegalArgumentException("R929_PRESENT_WITH_MISSING_REASON:" + spec.id);
                if (!finite(number))
                    throw new IllegalArgumentException("R929_NONFINITE_PRESENT:" + spec.id);
                // number == 0.0 is intentionally valid and requires no special branch.
            } else if (reason == null || reason == MissingReason.NONE) {
                throw new IllegalArgumentException("R929_MISSING_WITHOUT_REASON:" + spec.id);
            }
        }
        String replay = sha256(value.canonicalWithoutHash());
        if (!replay.equals(value.valueHash))
            throw new IllegalArgumentException("R929_VALUE_HASH_MISMATCH:" + spec.id);
    }

    /** Label is separate from the graph and can only be attached after the draw. */
    public static final class LabelJoin {
        public final String game;
        public final String targetIssue;
        private final int[] actual;
        public final long drawAtMs;
        public final long availableAtMs;
        public final String labelVersion;
        public final String labelHash;

        public LabelJoin(String game, String targetIssue, int[] actual, long drawAtMs,
                long availableAtMs, String labelVersion) {
            this.game = required(game, "LABEL_GAME");
            this.targetIssue = required(targetIssue, "LABEL_TARGET");
            if (actual == null || actual.length != POSITIONS || drawAtMs <= 0L
                    || availableAtMs < drawAtMs)
                throw new IllegalArgumentException("R929_LABEL_SHAPE_TIME");
            this.actual = Arrays.copyOf(actual, actual.length);
            int mask = 0;
            for (int number : this.actual) {
                if (number < 1 || number > NUMBERS || (mask & (1 << number)) != 0)
                    throw new IllegalArgumentException("R929_LABEL_NOT_PERMUTATION");
                mask |= 1 << number;
            }
            this.drawAtMs = drawAtMs;
            this.availableAtMs = availableAtMs;
            this.labelVersion = required(labelVersion, "LABEL_VERSION");
            this.labelHash = sha256(canonicalWithoutHash());
        }

        public int actualAtPosition(int position) {
            if (position < 1 || position > POSITIONS)
                throw new IllegalArgumentException("R929_POSITION_RANGE");
            return actual[position - 1];
        }

        String canonicalWithoutHash() {
            return game + '|' + targetIssue + '|' + Arrays.toString(actual) + '|' + drawAtMs + '|'
                    + availableAtMs + '|' + labelVersion;
        }
    }

    public static final class ActionRow {
        public final int ordinal;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final double rawActionProbability;
        public final double rawScore;
        private final double[] meta;
        public final String rowHash;

        public ActionRow(int ordinal, int position, int n1, int n2, int n3,
                double rawActionProbability, double rawScore, double[] meta) {
            this.ordinal = ordinal;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.rawActionProbability = rawActionProbability;
            this.rawScore = rawScore;
            if (meta == null || meta.length == 0) throw new IllegalArgumentException("R929_META_EMPTY");
            this.meta = Arrays.copyOf(meta, meta.length);
            StringBuilder value = new StringBuilder(64 + meta.length * 18);
            value.append(ordinal).append('|').append(position).append('|').append(n1).append('|')
                    .append(n2).append('|').append(n3).append('|')
                    .append(Long.toHexString(Double.doubleToLongBits(rawActionProbability))).append('|')
                    .append(Long.toHexString(Double.doubleToLongBits(rawScore)));
            for (double v : this.meta)
                value.append('|').append(Long.toHexString(Double.doubleToLongBits(v)));
            this.rowHash = sha256(value.toString());
        }

        public int metaSize() { return meta.length; }
        public double meta(int index) { return meta[index]; }
        public double[] metaCopy() { return Arrays.copyOf(meta, meta.length); }
        public boolean contains(int number) { return n1 == number || n2 == number || n3 == number; }
    }

    /** Immutable complete action surface.  No shortlist is accepted as training evidence. */
    public static final class ActionSurface {
        public final String game;
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final String inputStage;
        public final String featureGraphHash;
        public final String selectionPolicyHash;
        public final List<ActionRow> actions;
        public final int metaDimensions;
        public final String surfaceHash;

        public ActionSurface(String game, String targetIssue, String maxSourceIssue, long frozenAtMs,
                String inputStage, String featureGraphHash, String selectionPolicyHash,
                List<ActionRow> actions) {
            this.game = required(game, "SURFACE_GAME");
            this.targetIssue = required(targetIssue, "SURFACE_TARGET");
            this.maxSourceIssue = required(maxSourceIssue, "SURFACE_MAX_SOURCE");
            if (frozenAtMs <= 0L || actions == null || actions.size() != ACTION_COUNT)
                throw new IllegalArgumentException("R929_SURFACE_CARDINALITY");
            this.frozenAtMs = frozenAtMs;
            this.inputStage = required(inputStage, "SURFACE_STAGE");
            this.featureGraphHash = required(featureGraphHash, "SURFACE_FEATURE_HASH");
            this.selectionPolicyHash = required(selectionPolicyHash, "SURFACE_POLICY_HASH");
            if (!GAME.equals(this.game) || !isStrictPast(this.maxSourceIssue, this.targetIssue))
                throw new IllegalArgumentException("R929_SURFACE_GAME_OR_PIT");
            int metaSize = -1;
            int expectedOrdinal = 0;
            Set<String> keys = new HashSet<String>();
            ArrayList<ActionRow> copy = new ArrayList<ActionRow>(actions.size());
            for (int position = 1; position <= POSITIONS; position++) {
                for (int a = 1; a <= 8; a++) {
                    for (int b = a + 1; b <= 9; b++) {
                        for (int c = b + 1; c <= 10; c++) {
                            ActionRow row = actions.get(expectedOrdinal);
                            if (row == null || row.ordinal != expectedOrdinal || row.position != position
                                    || row.n1 != a || row.n2 != b || row.n3 != c)
                                throw new IllegalArgumentException(
                                        "R929_ACTION_ORDINAL:" + expectedOrdinal);
                            if (!finite(row.rawActionProbability) || row.rawActionProbability < 0.0d
                                    || row.rawActionProbability > 1.0d || !finite(row.rawScore))
                                throw new IllegalArgumentException("R929_ACTION_NONFINITE_OR_PROBABILITY");
                            if (metaSize < 0) metaSize = row.metaSize();
                            if (row.metaSize() != metaSize)
                                throw new IllegalArgumentException("R929_META_DIMENSION_DRIFT");
                            for (double value : row.metaCopy()) if (!finite(value))
                                throw new IllegalArgumentException("R929_META_NONFINITE");
                            String key = position + ":" + a + ':' + b + ':' + c;
                            if (!keys.add(key)) throw new IllegalArgumentException("R929_ACTION_DUPLICATE");
                            copy.add(row);
                            expectedOrdinal++;
                        }
                    }
                }
            }
            if (expectedOrdinal != ACTION_COUNT || keys.size() != ACTION_COUNT)
                throw new IllegalArgumentException("R929_ACTION_ENUMERATION_INCOMPLETE");
            this.metaDimensions = metaSize;
            this.actions = Collections.unmodifiableList(copy);
            this.surfaceHash = sha256(canonicalWithoutHash());
        }

        public int positiveLabels(LabelJoin label) {
            requireMatchingLabel(label);
            int positives = 0;
            for (ActionRow action : actions)
                if (action.contains(label.actualAtPosition(action.position))) positives++;
            if (positives != 360)
                throw new IllegalStateException("R929_ACTION_POSITIVE_COUNT:" + positives);
            return positives;
        }

        public String labelledHash(LabelJoin label) {
            requireMatchingLabel(label);
            positiveLabels(label);
            return sha256(surfaceHash + '|' + label.labelHash);
        }

        private void requireMatchingLabel(LabelJoin label) {
            if (label == null || !game.equals(label.game) || !targetIssue.equals(label.targetIssue)
                    || label.drawAtMs <= frozenAtMs)
                throw new IllegalArgumentException("R929_LABEL_JOIN_MISMATCH");
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(256 + actions.size() * 65);
            out.append(VERSION).append('|').append(game).append('|').append(targetIssue).append('|')
                    .append(maxSourceIssue).append('|').append(frozenAtMs).append('|')
                    .append(inputStage).append('|').append(featureGraphHash).append('|')
                    .append(selectionPolicyHash).append('|').append(metaDimensions);
            for (ActionRow row : actions) out.append('\n').append(row.rowHash);
            return out.toString();
        }
    }

    public static boolean isStrictPast(String sourceIssue, String targetIssue) {
        String source = clean(sourceIssue);
        String target = clean(targetIssue);
        return source.length() > 0 && target.length() > 0 && compareIssues(source, target) < 0;
    }

    public static int compareIssues(String left, String right) {
        String a = clean(left);
        String b = clean(right);
        if (a.equals(b)) return 0;
        if (digits(a) && digits(b)) {
            a = stripLeadingZeros(a);
            b = stripLeadingZeros(b);
            if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;
        }
        return a.compareTo(b);
    }

    private static boolean digits(String value) {
        if (value.length() == 0) return false;
        for (int i = 0; i < value.length(); i++)
            if (value.charAt(i) < '0' || value.charAt(i) > '9') return false;
        return true;
    }

    private static String stripLeadingZeros(String value) {
        int i = 0;
        while (i + 1 < value.length() && value.charAt(i) == '0') i++;
        return value.substring(i);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }

    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R929_" + code + "_REQUIRED");
        return clean;
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) out.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return out.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R929_SHA256_UNAVAILABLE", failure);
        }
    }
}
