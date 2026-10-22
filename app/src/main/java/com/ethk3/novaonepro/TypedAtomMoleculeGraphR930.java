package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import com.ethk3.novaonepro.r952.TypedOperationsCP4;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * R930 typed atom -> factor -> molecule graph.
 *
 * <p>The graph is an executable semantic boundary, not a naming catalogue.  Each node binds its
 * temporal stage, value type, shape, unit, lag/window, missing policy, source availability and
 * cost contract.  A post-draw label can never become an ancestor of a production molecule.
 * Existing Genesis/legacy atoms are imported by identity into a protected namespace; this class
 * does not rename, delete or overwrite historical matrices.</p>
 */
public final class TypedAtomMoleculeGraphR930 {
    public static final String VERSION = "R930_TYPED_ATOM_MOLECULE_GRAPH_V1";
    public static final String LEGACY_NAMESPACE = "LEGACY_PRESERVED";
    public static final String NEW_NAMESPACE = "R930_CREATED";

    private TypedAtomMoleculeGraphR930() {}

    public enum NodeKind { ATOM, FACTOR, MOLECULE }
    public enum ValueType { DOUBLE, INTEGER, BOOLEAN, CATEGORY, VECTOR, MATRIX, PROBABILITY, SCORE_VECTOR10, RANK_VECTOR10, K3_SET, REGIME }
    public enum Stage {
        RAW_PREDRAW(0),
        FROZEN_PREDRAW(1),
        NUMBER_100(2),
        ACTION_1200(3),
        SELECTION(4),
        POSTDRAW_LABEL(100),
        AUDIT_ONLY(101);
        final int order;
        Stage(int order) { this.order = order; }
        public boolean productionPredraw() { return order >= 0 && order <= 4; }
    }
    public enum MissingPolicy { REJECT, MASK, PROPAGATE, DEFAULT_WITH_MASK }
    public enum AdmissionState {
        ACTIVE,
        RESEARCH_ONLY,
        QUARANTINED,
        LEGACY_PRESERVED,
        RETIRED_WITH_TOMBSTONE
    }
    public enum Operator {
        IDENTITY(1, 1),
        ADD(2, 2),
        SUBTRACT(2, 2),
        MULTIPLY(2, 2),
        SAFE_DIVIDE(2, 2),
        MINIMUM(2, 2),
        MAXIMUM(2, 2),
        ABS_DIFFERENCE(2, 2),
        SIGNED_PRODUCT(2, 2),
        MEAN(2, 64),
        WEIGHTED_MEAN(2, 64),
        SUM(2, 64),
        WEIGHTED_SUM(2, 64),
        LOG1P_ABS(1, 1),
        SIGMOID(1, 1),
        CLIP(1, 1),
        RESIDUAL(2, 2),
        ORTHOGONAL_RESIDUAL(2, 2),
        INTERACTION3(3, 3),
        CONSTANT(0,0), PACK10(10,10), THRESHOLD_GE(2,2), THRESHOLD_GT(2,2), THRESHOLD_LE(2,2),
        CONDITIONAL(3,3), REGIME_GATE(2,2), TEMPORAL_PATTERN(1,32),
        RANK(1,1), TOP_K3(1,1), NORMALIZE_POSITIVE(1,1), NUMERIC_TO_DOUBLE(1,1), SCALAR_MULTIPLY_VECTOR10(2,2);
        final int minimumParents;
        final int maximumParents;
        Operator(int minimumParents, int maximumParents) {
            this.minimumParents = minimumParents;
            this.maximumParents = maximumParents;
        }
    }

    public static final class Shape {
        public final int rows;
        public final int columns;
        public final String shapeHash;

        public Shape(int rows, int columns) {
            if (rows <= 0 || columns <= 0 || (long) rows * columns > 120000L)
                throw new IllegalArgumentException("R930_SHAPE_INVALID");
            this.rows = rows;
            this.columns = columns;
            this.shapeHash = sha256(rows + "x" + columns);
        }

        public int elements() { return rows * columns; }
        public String canonical() { return rows + "x" + columns; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Shape)) return false;
            Shape value = (Shape) other;
            return rows == value.rows && columns == value.columns;
        }
        @Override public int hashCode() { return rows * 65537 + columns; }
    }

    public static final class NodeSpec {
        public final String namespace;
        public final String id;
        public final String semantic;
        public final NodeKind kind;
        public final ValueType valueType;
        public final Shape shape;
        public final Stage stage;
        public final String unit;
        public final int lag;
        public final int window;
        public final MissingPolicy missingPolicy;
        public final String source;
        public final String availabilityContract;
        public final long p95CostBudgetNs;
        public final List<String> parents;
        public final Operator operator;
        private final double[] parameters;
        public final boolean legacyProtected;
        public final String version;
        public final String structuralHash;

        public NodeSpec(String namespace, String id, String semantic, NodeKind kind,
                ValueType valueType, Shape shape, Stage stage, String unit, int lag, int window,
                MissingPolicy missingPolicy, String source, String availabilityContract,
                long p95CostBudgetNs, List<String> parents, Operator operator,
                double[] parameters, boolean legacyProtected, String version) {
            this.namespace = required(namespace, "NODE_NAMESPACE");
            this.id = required(id, "NODE_ID");
            this.semantic = required(semantic, "NODE_SEMANTIC");
            if (kind == null || valueType == null || shape == null || stage == null
                    || missingPolicy == null || lag < 0 || window < 0 || p95CostBudgetNs < 0L)
                throw new IllegalArgumentException("R930_NODE_CONTRACT_INVALID");
            this.kind = kind;
            this.valueType = valueType;
            this.shape = shape;
            this.stage = stage;
            this.unit = required(unit, "NODE_UNIT");
            this.lag = lag;
            this.window = window;
            this.missingPolicy = missingPolicy;
            this.source = required(source, "NODE_SOURCE");
            this.availabilityContract = required(availabilityContract, "NODE_AVAILABILITY");
            this.p95CostBudgetNs = p95CostBudgetNs;
            ArrayList<String> parentCopy = new ArrayList<String>();
            if (parents != null) {
                for (String parent : parents) parentCopy.add(required(parent, "NODE_PARENT"));
            }
            this.parents = Collections.unmodifiableList(parentCopy);
            this.operator = operator;
            this.parameters = parameters == null ? new double[0]
                    : Arrays.copyOf(parameters, parameters.length);
            for (double value : this.parameters) if (!finite(value))
                throw new IllegalArgumentException("R930_NODE_PARAMETER_NONFINITE");
            this.legacyProtected = legacyProtected;
            this.version = required(version, "NODE_VERSION");
            if (kind == NodeKind.ATOM) {
                if (!this.parents.isEmpty() || operator != null)
                    throw new IllegalArgumentException("R930_ATOM_HAS_OPERATOR_OR_PARENT");
            } else {
                if (operator == null || this.parents.size() < operator.minimumParents
                        || this.parents.size() > operator.maximumParents)
                    throw new IllegalArgumentException("R930_COMPOSITE_ARITY_INVALID");
                int expected = operator == Operator.CLIP ? 2
                        : operator == Operator.WEIGHTED_MEAN || operator == Operator.WEIGHTED_SUM
                        ? this.parents.size() : operator == Operator.ORTHOGONAL_RESIDUAL
                        ? this.parameters.length == 0 ? 0 : 1 : 0;
                if (!version.startsWith("AST3") && (this.parameters.length != expected || operator == Operator.CLIP
                        && this.parameters[0] > this.parameters[1]))
                    throw new IllegalArgumentException("R958_OPERATOR_PARAMETER_CONTRACT");
            }
            if (legacyProtected && !LEGACY_NAMESPACE.equals(this.namespace))
                throw new IllegalArgumentException("R930_LEGACY_NAMESPACE_MISMATCH");
            this.structuralHash = sha256(canonicalWithoutHash());
        }

        public String qualifiedId() { return namespace + ':' + id; }
        public double[] parametersCopy() { return Arrays.copyOf(parameters, parameters.length); }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(512);
            out.append(namespace).append('|').append(id).append('|').append(semantic).append('|')
                    .append(kind.name()).append('|').append(valueType.name()).append('|')
                    .append(shape.canonical()).append('|').append(stage.name()).append('|')
                    .append(unit).append('|').append(lag).append('|').append(window).append('|')
                    .append(missingPolicy.name()).append('|').append(source).append('|')
                    .append(availabilityContract).append('|').append(p95CostBudgetNs).append('|')
                    .append(operator == null ? "ATOM" : operator.name()).append('|')
                    .append(legacyProtected ? 1 : 0).append('|').append(version);
            for (String parent : parents) out.append("|p=").append(parent);
            for (double value : parameters)
                out.append("|v=").append(Long.toHexString(Double.doubleToLongBits(value)));
            return out.toString();
        }
    }

    public static final class Graph {
        public final String game;
        public final String inputStage;
        public final String schemaVersion;
        public final List<NodeSpec> orderedNodes;
        public final Map<String, NodeSpec> byQualifiedId;
        public final String graphHash;

        public Graph(String game, String inputStage, String schemaVersion, List<NodeSpec> nodes) {
            this.game = required(game, "GRAPH_GAME");
            this.inputStage = required(inputStage, "GRAPH_INPUT_STAGE");
            this.schemaVersion = required(schemaVersion, "GRAPH_SCHEMA_VERSION");
            if (nodes == null || nodes.isEmpty())
                throw new IllegalArgumentException("R930_GRAPH_EMPTY");
            TreeMap<String, NodeSpec> sorted = new TreeMap<String, NodeSpec>();
            for (NodeSpec node : nodes) {
                if (node == null || sorted.put(node.qualifiedId(), node) != null)
                    throw new IllegalArgumentException("R930_NODE_DUPLICATE");
            }
            for (NodeSpec node : sorted.values()) validateParentsAndSemantics(node, sorted);
            ArrayList<String> order = new ArrayList<String>();
            HashMap<String, Integer> colors = new HashMap<String, Integer>();
            for (String id : sorted.keySet()) visit(id, sorted, colors, order);
            ArrayList<NodeSpec> ordered = new ArrayList<NodeSpec>();
            LinkedHashMap<String, NodeSpec> index = new LinkedHashMap<String, NodeSpec>();
            for (String id : order) {
                NodeSpec node = sorted.get(id);
                ordered.add(node);
                index.put(id, node);
            }
            this.orderedNodes = Collections.unmodifiableList(ordered);
            this.byQualifiedId = Collections.unmodifiableMap(index);
            this.graphHash = sha256(canonicalWithoutHash());
        }

        public NodeSpec require(String qualifiedId) {
            NodeSpec node = byQualifiedId.get(qualifiedId);
            if (node == null) throw new IllegalArgumentException("R930_NODE_NOT_REGISTERED:" + qualifiedId);
            return node;
        }

        private void validateParentsAndSemantics(NodeSpec node, Map<String, NodeSpec> nodes) {
            if (node.kind == NodeKind.ATOM) return;
            if (node.version.startsWith("AST3")) {
                String[] types = new String[node.parents.size()];
                int[] widths = new int[node.parents.size()];
                for (int i=0;i<node.parents.size();i++) {
                    NodeSpec parent=nodes.get(node.parents.get(i));
                    if(parent==null || parent.qualifiedId().equals(node.qualifiedId()))
                        throw new IllegalArgumentException("AST3_PARENT");
                    if(!parent.stage.productionPredraw() || parent.stage.order>node.stage.order)
                        throw new IllegalArgumentException("AST3_STAGE_DIRECTION");
                    types[i]=parent.valueType.name(); widths[i]=parent.shape.elements();
                }
                TypedOperationsCP4.validate(node.operator.name(),node.valueType.name(),node.shape.elements(),types,widths,node.parameters);
                return;
            }
            Shape parentShape = null;
            for (String parentId : node.parents) {
                NodeSpec parent = nodes.get(parentId);
                if (parent == null)
                    throw new IllegalArgumentException("R930_UNKNOWN_PARENT:" + parentId);
                if (parent.qualifiedId().equals(node.qualifiedId()))
                    throw new IllegalArgumentException("R930_SELF_CYCLE:" + parentId);
                if (parent.stage == Stage.POSTDRAW_LABEL && node.stage.productionPredraw())
                    throw new IllegalArgumentException("R930_POSTDRAW_TO_PREDRAW_LEAK:" + node.id);
                if (parent.stage.order > node.stage.order && node.stage.productionPredraw())
                    throw new IllegalArgumentException("R930_STAGE_DIRECTION_INVALID:" + node.id);
                if (!numeric(parent.valueType) || !numeric(node.valueType))
                    throw new IllegalArgumentException("R930_NON_NUMERIC_COMPOSITION:" + node.id);
                if (parentShape == null) parentShape = parent.shape;
                if (!parent.shape.equals(parentShape) || !node.shape.equals(parent.shape))
                    throw new IllegalArgumentException("R930_COMPOSITE_SHAPE_MISMATCH:" + node.id);
                if (!unitCompatible(node.operator, parent.unit, node.unit))
                    throw new IllegalArgumentException("R930_COMPOSITE_UNIT_MISMATCH:" + node.id);
            }
            if (node.operator == Operator.WEIGHTED_MEAN
                    && node.parameters.length != node.parents.size())
                throw new IllegalArgumentException("R930_WEIGHTED_MEAN_WEIGHT_COUNT");
            if (node.operator == Operator.CLIP
                    && (node.parameters.length != 2 || node.parameters[0] > node.parameters[1]))
                throw new IllegalArgumentException("R930_CLIP_PARAMETERS");
        }

        private String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(2048);
            out.append(game).append('|').append(inputStage).append('|').append(schemaVersion);
            for (NodeSpec node : orderedNodes)
                out.append('\n').append(node.structuralHash);
            return out.toString();
        }
    }

    public static final class Value {
        private final double[] values;
        private final boolean[] present;
        public final String maxSourceIssue;
        public final long availableAtMs;
        public final long latencyNs;
        public final String sourceHash;
        public final String valueHash;

        public Value(double[] values, boolean[] present, String maxSourceIssue,
                long availableAtMs, long latencyNs, String sourceHash) {
            if (values == null || present == null || values.length == 0
                    || values.length != present.length || availableAtMs < 0L || latencyNs < 0L)
                throw new IllegalArgumentException("R930_VALUE_INVALID");
            this.values = Arrays.copyOf(values, values.length);
            this.present = Arrays.copyOf(present, present.length);
            for (int i = 0; i < this.values.length; i++) {
                if (this.present[i] && !finite(this.values[i]))
                    throw new IllegalArgumentException("R930_PRESENT_NONFINITE");
            }
            this.maxSourceIssue = clean(maxSourceIssue);
            this.availableAtMs = availableAtMs;
            this.latencyNs = latencyNs;
            this.sourceHash = requireHash(sourceHash, "VALUE_SOURCE_HASH");
            this.valueHash = sha256(canonicalWithoutHash());
        }

        public static Value present(double[] values, String maxSourceIssue,
                long availableAtMs, long latencyNs, String sourceHash) {
            boolean[] mask = new boolean[values.length];
            Arrays.fill(mask, true);
            return new Value(values, mask, maxSourceIssue, availableAtMs, latencyNs, sourceHash);
        }

        public static Value missing(int size, String maxSourceIssue,
                long availableAtMs, long latencyNs, String sourceHash) {
            return new Value(new double[size], new boolean[size], maxSourceIssue,
                    availableAtMs, latencyNs, sourceHash);
        }

        public int size() { return values.length; }
        public double get(int index) { return values[index]; }
        public boolean present(int index) { return present[index]; }
        public double[] valuesCopy() { return Arrays.copyOf(values, values.length); }
        public boolean[] maskCopy() { return Arrays.copyOf(present, present.length); }
        public int presentCount() { int n = 0; for (boolean value : present) if (value) n++; return n; }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(values.length * 24 + 128);
            // Measured latency remains cost evidence; it cannot change a replay's inference identity.
            out.append("R958_VALUE2|").append(maxSourceIssue).append('|').append(availableAtMs).append('|')
                    .append(sourceHash);
            for (int i = 0; i < values.length; i++)
                out.append('|').append(present[i] ? 1 : 0).append(':')
                        .append(Long.toHexString(Double.doubleToLongBits(values[i])));
            return out.toString();
        }
    }

    public static final class FrozenInputs {
        public final String targetIssue;
        public final long frozenAtMs;
        public final String inputHash;
        private final Map<String, Value> values;

        public FrozenInputs(String targetIssue, long frozenAtMs, String inputHash,
                Map<String, Value> values) {
            this.targetIssue = required(targetIssue, "FROZEN_TARGET");
            if (frozenAtMs <= 0L || values == null)
                throw new IllegalArgumentException("R930_FROZEN_INPUT_INVALID");
            this.frozenAtMs = frozenAtMs;
            this.inputHash = requireHash(inputHash, "FROZEN_INPUT_HASH");
            this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Value>(values));
        }

        public Value value(String qualifiedId) { return values.get(qualifiedId); }
    }

    public static final class Evaluation {
        public final Graph graph;
        public final String targetIssue;
        public final long frozenAtMs;
        public final Map<String, Value> values;
        public final String evaluationHash;

        Evaluation(Graph graph, String targetIssue, long frozenAtMs, Map<String, Value> values) {
            this.graph = graph;
            this.targetIssue = targetIssue;
            this.frozenAtMs = frozenAtMs;
            this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Value>(values));
            StringBuilder out = new StringBuilder(graph.graphHash).append('|')
                    .append(targetIssue).append('|').append(frozenAtMs);
            for (NodeSpec node : graph.orderedNodes)
                out.append('|').append(node.qualifiedId()).append(':')
                        .append(this.values.get(node.qualifiedId()).valueHash);
            this.evaluationHash = sha256(out.toString());
        }

        public Value require(String qualifiedId) {
            Value value = values.get(qualifiedId);
            if (value == null) throw new IllegalArgumentException("R930_VALUE_NOT_EVALUATED");
            return value;
        }
    }

    public static Evaluation evaluate(Graph graph, FrozenInputs inputs) {
        if (graph == null || inputs == null)
            throw new IllegalArgumentException("R930_EVALUATION_INPUT_REQUIRED");
        LinkedHashMap<String, Value> computed = new LinkedHashMap<String, Value>();
        for (NodeSpec node : graph.orderedNodes) {
            Value value;
            if (node.kind == NodeKind.ATOM) {
                value = inputs.value(node.qualifiedId());
                if (value == null) value = Value.missing(node.shape.elements(), "",
                        inputs.frozenAtMs, 0L, sha256("MISSING|" + node.qualifiedId()));
            } else {
                ArrayList<Value> parents = new ArrayList<Value>();
                for (String parent : node.parents) parents.add(computed.get(parent));
                value = apply(node, parents, inputs.frozenAtMs);
            }
            validateValue(node, value, inputs);
            computed.put(node.qualifiedId(), value);
        }
        return new Evaluation(graph, inputs.targetIssue, inputs.frozenAtMs, computed);
    }

    private static Value apply(NodeSpec node, List<Value> parents, long frozenAtMs) {
        long started = System.nanoTime();
        int width = node.shape.elements();
        double[] output = new double[width];
        boolean[] present = new boolean[width];
        String maxSource = "";
        long available = 0L;
        StringBuilder sources = new StringBuilder(node.structuralHash);
        for (Value parent : parents) {
            if (parent == null) throw new IllegalStateException("R930_PARENT_VALUE_NULL");
            if (PitFeatureDagR929.compareIssues(parent.maxSourceIssue, maxSource) > 0)
                maxSource = parent.maxSourceIssue;
            available = Math.max(available, parent.availableAtMs);
            sources.append('|').append(parent.valueHash);
        }
        double[] parameters = node.parameters;
        if (node.version.startsWith("AST3")) {
            TypedOperationsCP4.Value[] arguments=new TypedOperationsCP4.Value[parents.size()];
            for(int i=0;i<arguments.length;i++) arguments[i]=new TypedOperationsCP4.Value(parents.get(i).valuesCopy(),parents.get(i).maskCopy());
            TypedOperationsCP4.Value result=TypedOperationsCP4.apply(node.operator.name(),width,arguments,parameters);
            return new Value(result.x,result.present,maxSource,available,Math.max(0,System.nanoTime()-started),sha256(sources.toString()));
        }
        for (int index = 0; index < width; index++) {
            boolean all = true;
            for (Value parent : parents) all &= parent.present(index);
            if (!all && node.missingPolicy == MissingPolicy.REJECT)
                throw new IllegalStateException("R930_REQUIRED_PARENT_MISSING:" + node.id);
            if (!all && (node.missingPolicy == MissingPolicy.MASK
                    || node.missingPolicy == MissingPolicy.PROPAGATE)) {
                present[index] = false;
                output[index] = 0.0d;
                continue;
            }
            double[] values = new double[parents.size()];
            for (int i = 0; i < parents.size(); i++)
                values[i] = parents.get(i).present(index) ? parents.get(i).get(index) : 0.0d;
            output[index] = operation(node.operator, values, parameters);
            present[index] = finite(output[index]);
            if (!present[index]) output[index] = 0.0d;
        }
        long cost = Math.max(0L, System.nanoTime() - started);
        return new Value(output, present, maxSource, Math.min(frozenAtMs, available), cost,
                sha256(sources.toString()));
    }

    /** Executes a previously type-checked scalar node. The caller owns frozen PIT/mask validation. */
    public static double scalarOperation(NodeSpec node, double[] values) {
        if (node == null || node.kind == NodeKind.ATOM || node.shape.elements() != 1
                || !node.stage.productionPredraw() || values == null
                || values.length != node.parents.size())
            throw new IllegalArgumentException("R958_SCALAR_NODE_CONTRACT");
        for (double v : values) if (!Double.isFinite(v))
            throw new IllegalArgumentException("R958_SCALAR_NONFINITE_PARENT");
        double result = operation(node.operator, values, node.parameters);
        if (!Double.isFinite(result)) throw new IllegalArgumentException("R958_SCALAR_NONFINITE_RESULT");
        return result;
    }

    private static double operation(Operator operator, double[] values, double[] parameters) {
        switch (operator) {
            case IDENTITY: return values[0];
            case ADD: return values[0] + values[1];
            case SUBTRACT: return values[0] - values[1];
            case MULTIPLY: return values[0] * values[1];
            case SAFE_DIVIDE:
                return Math.abs(values[1]) < 1.0e-12d ? 0.0d : values[0] / values[1];
            case MINIMUM: return Math.min(values[0], values[1]);
            case MAXIMUM: return Math.max(values[0], values[1]);
            case ABS_DIFFERENCE: return Math.abs(values[0] - values[1]);
            case SIGNED_PRODUCT: return Math.copySign(Math.abs(values[0] * values[1]), values[0]);
            case MEAN: {
                double sum = 0.0d; for (double value : values) sum += value;
                return sum / values.length;
            }
            case SUM: { double sum=0.0d;for(double value:values)sum+=value;return sum; }
            case WEIGHTED_SUM: { double sum=0.0d;for(int i=0;i<values.length;i++)sum+=values[i]*parameters[i];return sum; }
            case WEIGHTED_MEAN: {
                double sum = 0.0d, weight = 0.0d;
                for (int i = 0; i < values.length; i++) {
                    sum += values[i] * parameters[i]; weight += Math.abs(parameters[i]);
                }
                return weight < 1.0e-12d ? 0.0d : sum / weight;
            }
            case LOG1P_ABS: return Math.log1p(Math.abs(values[0]));
            case SIGMOID:
                if (values[0] >= 0.0d) {
                    double z = Math.exp(-Math.min(700.0d, values[0])); return 1.0d / (1.0d + z);
                }
                double z = Math.exp(Math.max(-700.0d, values[0])); return z / (1.0d + z);
            case CLIP: return Math.max(parameters[0], Math.min(parameters[1], values[0]));
            case RESIDUAL: return values[0] - values[1];
            case ORTHOGONAL_RESIDUAL: {
                double projection = parameters.length == 0 ? 1.0d : parameters[0];
                return values[0] - projection * values[1];
            }
            case INTERACTION3: return values[0] * values[1] * values[2];
            default: throw new IllegalArgumentException("R930_OPERATOR_UNSUPPORTED");
        }
    }

    private static void validateValue(NodeSpec node, Value value, FrozenInputs inputs) {
        if (value == null || value.size() != node.shape.elements())
            throw new IllegalArgumentException("R930_VALUE_SHAPE_MISMATCH:" + node.id);
        if(node.version.startsWith("AST3"))
            TypedOperationsCP4.validateValue(node.valueType.name(),new TypedOperationsCP4.Value(value.valuesCopy(),value.maskCopy()));
        if (value.availableAtMs > inputs.frozenAtMs)
            throw new IllegalArgumentException("R930_VALUE_AVAILABLE_AFTER_FREEZE:" + node.id);
        if (node.stage.productionPredraw() && !"CONSTANT".equals(node.source)
                && !PitFeatureDagR929.isStrictPast(value.maxSourceIssue, inputs.targetIssue))
            throw new IllegalArgumentException("R930_ATOM_NOT_PIT:" + node.id);
        if (node.p95CostBudgetNs > 0L && value.latencyNs > node.p95CostBudgetNs * 4L)
            throw new IllegalStateException("R930_NODE_HARD_COST_MULTIPLIER:" + node.id);
        if (node.missingPolicy == MissingPolicy.REJECT
                && value.presentCount() != value.size())
            throw new IllegalStateException("R930_REQUIRED_VALUE_MISSING:" + node.id);
    }

    public static final class AtomObservation {
        public final String qualifiedId;
        public final String targetIssue;
        public final boolean available;
        public final boolean pitPass;
        public final long latencyNs;
        public final double value;
        public final double quality;
        public final String inputHash;
        public final String observationHash;

        public AtomObservation(String qualifiedId, String targetIssue, boolean available,
                boolean pitPass, long latencyNs, double value, double quality, String inputHash) {
            this.qualifiedId = required(qualifiedId, "OBS_ATOM_ID");
            this.targetIssue = required(targetIssue, "OBS_TARGET");
            if (latencyNs < 0L || !finite(value) || !unit(quality))
                throw new IllegalArgumentException("R930_ATOM_OBSERVATION_INVALID");
            this.available = available;
            this.pitPass = pitPass;
            this.latencyNs = latencyNs;
            this.value = value;
            this.quality = quality;
            this.inputHash = requireHash(inputHash, "OBS_INPUT_HASH");
            this.observationHash = sha256(this.qualifiedId + '|' + this.targetIssue + '|'
                    + (available ? 1 : 0) + '|' + (pitPass ? 1 : 0) + '|' + latencyNs + '|'
                    + Long.toHexString(Double.doubleToLongBits(value)) + '|'
                    + Long.toHexString(Double.doubleToLongBits(quality)) + '|' + this.inputHash);
        }
    }

    public static final class AtomQuality {
        public final String qualifiedId;
        public final int support;
        public final int availableN;
        public final int pitFailures;
        public final double missingRate;
        public final double mean;
        public final double variance;
        public final double meanQuality;
        public final long p95LatencyNs;
        public final double recentDriftZ;
        public final String evidenceHash;
        public final String sourceEvidence;

        AtomQuality(String qualifiedId, int support, int availableN, int pitFailures,
                double missingRate, double mean, double variance, double meanQuality,
                long p95LatencyNs, double recentDriftZ, String evidenceHash) {
            this(qualifiedId,support,availableN,pitFailures,missingRate,mean,variance,meanQuality,
                    p95LatencyNs,recentDriftZ,evidenceHash,"");
        }
        AtomQuality(String qualifiedId, int support, int availableN, int pitFailures,
                double missingRate, double mean, double variance, double meanQuality,
                long p95LatencyNs, double recentDriftZ, String evidenceHash,String sourceEvidence) {
            this.qualifiedId = qualifiedId;
            this.support = support;
            this.availableN = availableN;
            this.pitFailures = pitFailures;
            this.missingRate = missingRate;
            this.mean = mean;
            this.variance = variance;
            this.meanQuality = meanQuality;
            this.p95LatencyNs = p95LatencyNs;
            this.recentDriftZ = recentDriftZ;
            this.evidenceHash = evidenceHash;
            this.sourceEvidence = sourceEvidence == null ? "" : sourceEvidence;
        }
    }

    public static AtomQuality summarize(String qualifiedId, List<AtomObservation> observations) {
        if (observations == null || observations.isEmpty())
            throw new IllegalArgumentException("R930_ATOM_OBSERVATIONS_EMPTY");
        String id = required(qualifiedId, "QUALITY_ATOM_ID");
        ArrayList<AtomObservation> rows = new ArrayList<AtomObservation>(observations);
        Set<String> issues = new HashSet<String>();
        for (AtomObservation row : rows) {
            if (row == null || !id.equals(row.qualifiedId))
                throw new IllegalArgumentException("R930_ATOM_OBSERVATION_ID_DRIFT");
            if (!issues.add(row.targetIssue))
                throw new IllegalArgumentException("R958_ATOM_DUPLICATE_OBSERVATION_ISSUE");
        }
        Collections.sort(rows, new Comparator<AtomObservation>() {
            @Override public int compare(AtomObservation left, AtomObservation right) {
                return PitFeatureDagR929.compareIssues(left.targetIssue, right.targetIssue);
            }
        });
        int available = 0, pitFailures = 0;
        double sum = 0.0d, quality = 0.0d;
        ArrayList<Long> latency = new ArrayList<Long>();
        StringBuilder evidence = new StringBuilder(id);
        for (AtomObservation row : rows) {
            if (row == null || !id.equals(row.qualifiedId))
                throw new IllegalArgumentException("R930_ATOM_OBSERVATION_ID_DRIFT");
            if (!row.pitPass) pitFailures++;
            if (row.available) {
                available++;
                sum += row.value;
                quality += row.quality;
                latency.add(Long.valueOf(row.latencyNs));
            }
            evidence.append('|').append(row.observationHash);
        }
        double mean = available == 0 ? 0.0d : sum / available;
        double variance = 0.0d;
        double recent = 0.0d, earlier = 0.0d;
        int rn = 0, en = 0;
        int split = Math.max(1, rows.size() / 2);
        for (int i = 0; i < rows.size(); i++) {
            AtomObservation row = rows.get(i);
            if (!row.available) continue;
            double delta = row.value - mean;
            variance += delta * delta;
            if (i >= split) { recent += row.value; rn++; }
            else { earlier += row.value; en++; }
        }
        if (available > 0) variance /= available;
        double drift = rn == 0 || en == 0 ? 0.0d
                : Math.abs(recent / rn - earlier / en) / Math.sqrt(Math.max(1.0e-12d, variance));
        Collections.sort(latency);
        long p95 = latency.isEmpty() ? 0L
                : latency.get(Math.max(0, (int) Math.ceil(latency.size() * 0.95d) - 1)).longValue();
        return new AtomQuality(id, rows.size(), available, pitFailures,
                1.0d - available / (double) rows.size(), mean, variance,
                available == 0 ? 0.0d : quality / available, p95, drift,
                sha256(evidence.toString()));
    }

    public static final class Admission {
        public final NodeSpec node;
        public final AtomQuality quality;
        public final AdmissionState state;
        public final boolean productionEligible;
        public final String reason;
        public final String admissionHash;

        Admission(NodeSpec node, AtomQuality quality, AdmissionState state,
                boolean productionEligible, String reason) {
            this.node = node;
            this.quality = quality;
            this.state = state;
            this.productionEligible = productionEligible;
            this.reason = reason;
            this.admissionHash = sha256("R958_ADMISSION1|"+node.structuralHash + '|' + quality.evidenceHash + '|'
                    + quality.support+'|'+quality.availableN+'|'+quality.pitFailures+'|'+quality.p95LatencyNs+'|'
                    + Double.toHexString(quality.missingRate)+'|'+Double.toHexString(quality.mean)+'|'
                    + Double.toHexString(quality.variance)+'|'+Double.toHexString(quality.meanQuality)+'|'
                    + Double.toHexString(quality.recentDriftZ)+'|'
                    + state.name() + '|' + (productionEligible ? 1 : 0) + '|' + reason);
        }

        /** Full numerical admission evidence; the original quality owner's source is retained. */
        public String encode() {
            try {
                NodeSpec n=node;AtomQuality q=quality;JSONObject j=new JSONObject();
                JSONArray parents=new JSONArray(),parameters=new JSONArray();
                for(String p:n.parents)parents.put(p);for(double p:n.parametersCopy())parameters.put(p);
                j.put("version","R958_ADMISSION_V1").put("namespace",n.namespace).put("id",n.id)
                    .put("semantic",n.semantic).put("kind",n.kind.name()).put("type",n.valueType.name())
                    .put("rows",n.shape.rows).put("columns",n.shape.columns).put("stage",n.stage.name())
                    .put("unit",n.unit).put("lag",n.lag).put("window",n.window).put("missing",n.missingPolicy.name())
                    .put("source",n.source).put("availability",n.availabilityContract).put("cost",n.p95CostBudgetNs)
                    .put("parents",parents).put("parameters",parameters).put("operator",n.operator==null?"":n.operator.name())
                    .put("legacy",n.legacyProtected).put("node_version",n.version).put("node_hash",n.structuralHash)
                    .put("support",q.support).put("available",q.availableN).put("pit_failures",q.pitFailures)
                    .put("missing_rate",q.missingRate).put("mean",q.mean).put("variance",q.variance)
                    .put("mean_quality",q.meanQuality).put("latency",q.p95LatencyNs).put("drift",q.recentDriftZ)
                    .put("quality_hash",q.evidenceHash).put("quality_source",q.sourceEvidence)
                    .put("state",state.name()).put("production_eligible",productionEligible)
                    .put("reason",reason).put("admission_hash",admissionHash);
                return j.toString();
            }catch(JSONException e){throw new IllegalArgumentException("R958_ADMISSION_ENCODING",e);}
        }
    }

    public static Admission decodeAdmission(String payload) {
        try {
            JSONObject j=new JSONObject(payload);
            if(!"R958_ADMISSION_V1".equals(j.getString("version")))throw new IllegalArgumentException("R958_ADMISSION_VERSION");
            JSONArray ps=j.getJSONArray("parents"),vs=j.getJSONArray("parameters");
            List<String> parents=new ArrayList<String>();for(int i=0;i<ps.length();i++)parents.add(ps.getString(i));
            double[] parameters=new double[vs.length()];for(int i=0;i<parameters.length;i++)parameters[i]=vs.getDouble(i);
            String op=j.getString("operator");
            NodeSpec n=new NodeSpec(j.getString("namespace"),j.getString("id"),j.getString("semantic"),
                NodeKind.valueOf(j.getString("kind")),ValueType.valueOf(j.getString("type")),
                new Shape(j.getInt("rows"),j.getInt("columns")),Stage.valueOf(j.getString("stage")),
                j.getString("unit"),j.getInt("lag"),j.getInt("window"),MissingPolicy.valueOf(j.getString("missing")),
                j.getString("source"),j.getString("availability"),j.getLong("cost"),parents,
                op.length()==0?null:Operator.valueOf(op),parameters,j.getBoolean("legacy"),j.getString("node_version"));
            AtomQuality q=new AtomQuality(n.qualifiedId(),j.getInt("support"),j.getInt("available"),j.getInt("pit_failures"),
                j.getDouble("missing_rate"),j.getDouble("mean"),j.getDouble("variance"),j.getDouble("mean_quality"),
                j.getLong("latency"),j.getDouble("drift"),j.getString("quality_hash"),j.getString("quality_source"));
            Admission result=admit(n,q);
            if(!n.structuralHash.equals(j.getString("node_hash"))||!result.admissionHash.equals(j.getString("admission_hash"))
                ||!result.state.name().equals(j.getString("state"))||!result.reason.equals(j.getString("reason"))
                ||result.productionEligible!=j.getBoolean("production_eligible"))
                throw new IllegalArgumentException("R958_ADMISSION_EVIDENCE_DRIFT");
            return result;
        }catch(JSONException e){throw new IllegalArgumentException("R958_ADMISSION_DECODING",e);}
    }

    /** Quarantines only the defective atom; legacy-protected nodes remain visible and immutable. */
    public static Admission admit(NodeSpec node, AtomQuality quality) {
        if (node == null || quality == null
                || !node.qualifiedId().equals(quality.qualifiedId))
            throw new IllegalArgumentException("R930_ADMISSION_INPUT_MISMATCH");
        if(quality.support<0||quality.availableN<0||quality.availableN>quality.support
                ||quality.pitFailures<0||quality.pitFailures>quality.support||quality.p95LatencyNs<0
                ||!unit(quality.missingRate)||!unit(quality.meanQuality)||!finite(quality.mean)
                ||!finite(quality.variance)||quality.variance<0||!finite(quality.recentDriftZ)||quality.recentDriftZ<0
                ||quality.evidenceHash==null||!quality.evidenceHash.matches("[0-9a-f]{64}")
                ||quality.sourceEvidence.length()>0&&!sha256(quality.sourceEvidence).equals(quality.evidenceHash))
            return new Admission(node,quality,AdmissionState.QUARANTINED,false,"INVALID_QUALITY_EVIDENCE");
        if (node.legacyProtected)
            return new Admission(node, quality, AdmissionState.LEGACY_PRESERVED,
                    false, "LEGACY_IDENTITY_PRESERVED_NO_AUTOMATIC_NEW_WEIGHT");
        if (!node.stage.productionPredraw())
            return new Admission(node, quality, AdmissionState.RESEARCH_ONLY,
                    false, "POSTDRAW_OR_AUDIT_STAGE");
        if (quality.pitFailures > 0)
            return new Admission(node, quality, AdmissionState.QUARANTINED,
                    false, "PIT_FAILURE");
        if (quality.support < 48 || quality.availableN < 30)
            return new Admission(node, quality, AdmissionState.RESEARCH_ONLY,
                    false, "SUPPORT_PENDING");
        if (quality.missingRate > 0.20d)
            return new Admission(node, quality, AdmissionState.QUARANTINED,
                    false, "MISSING_RATE_HIGH");
        if (node.p95CostBudgetNs > 0L && quality.p95LatencyNs > node.p95CostBudgetNs)
            return new Admission(node, quality, AdmissionState.QUARANTINED,
                    false, "P95_LATENCY_OVER_BUDGET");
        if (quality.meanQuality < 0.50d)
            return new Admission(node, quality, AdmissionState.RESEARCH_ONLY,
                    false, "QUALITY_LOW");
        if (quality.variance < 1.0e-12d)
            return new Admission(node, quality, AdmissionState.RESEARCH_ONLY,
                    false, "CONSTANT_NO_INFORMATION");
        if (quality.recentDriftZ > 3.0d)
            return new Admission(node, quality, AdmissionState.RESEARCH_ONLY,
                    false, "DRIFT_WATCH");
        return new Admission(node, quality, AdmissionState.ACTIVE,
                true, "PIT_SUPPORT_QUALITY_COST_PASS");
    }

    /** Imports current Genesis atoms without mutating their IDs or semantic tiers. */
    public static List<NodeSpec> protectedLegacyGenesisNodes() {
        ArrayList<NodeSpec> output = new ArrayList<NodeSpec>();
        for (GenesisSchemaRegistryR915.AtomSpec atom : GenesisSchemaRegistryR915.atoms()) {
            Stage stage = atom.settlementEvidence() ? Stage.POSTDRAW_LABEL
                    : atom.productionScoreEligible() ? Stage.FROZEN_PREDRAW : Stage.AUDIT_ONLY;
            ValueType type;
            if ("INTEGER".equals(atom.valueType)) type = ValueType.INTEGER;
            else if ("BOOLEAN".equals(atom.valueType)) type = ValueType.BOOLEAN;
            else if ("STRING".equals(atom.valueType)) type = ValueType.CATEGORY;
            else type = ValueType.DOUBLE;
            output.add(new NodeSpec(LEGACY_NAMESPACE, atom.id, atom.name,
                    NodeKind.ATOM, type, new Shape(1, 1), stage, atom.valueRange,
                    Math.max(0, atom.maxLag), parseWindow(atom.windowSpec), MissingPolicy.MASK,
                    "GenesisSchemaRegistryR915", atom.updateTiming, costBudget(atom.costClass),
                    Collections.<String>emptyList(), null, null, true,
                    "R915_V" + atom.version + "_PRESERVED_R930"));
        }
        return Collections.unmodifiableList(output);
    }

    private static int parseWindow(String value) {
        if (value == null) return 0;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
            else if (digits.length() > 0) break;
        }
        if (digits.length() == 0) return 0;
        try { return Math.min(10000, Integer.parseInt(digits.toString())); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static long costBudget(String costClass) {
        if (costClass == null) return 0L;
        if (costClass.contains("HIGH")) return 5_000_000L;
        if (costClass.contains("MEDIUM")) return 2_000_000L;
        return 500_000L;
    }

    private static boolean numeric(ValueType type) {
        return type == ValueType.DOUBLE || type == ValueType.INTEGER
                || type == ValueType.BOOLEAN || type == ValueType.VECTOR
                || type == ValueType.MATRIX;
    }

    private static boolean unitCompatible(Operator operator, String parentUnit, String outputUnit) {
        if (operator == Operator.SAFE_DIVIDE || operator == Operator.SIGMOID
                || operator == Operator.LOG1P_ABS)
            return "ratio".equalsIgnoreCase(outputUnit)
                    || "probability".equalsIgnoreCase(outputUnit)
                    || "dimensionless".equalsIgnoreCase(outputUnit)
                    || parentUnit.equals(outputUnit);
        return parentUnit.equals(outputUnit) || "derived".equalsIgnoreCase(outputUnit);
    }

    private static void visit(String id, Map<String, NodeSpec> nodes,
            Map<String, Integer> colors, List<String> order) {
        Integer color = colors.get(id);
        if (color != null && color.intValue() == 2) return;
        if (color != null && color.intValue() == 1)
            throw new IllegalArgumentException("R930_GRAPH_CYCLE:" + id);
        colors.put(id, Integer.valueOf(1));
        NodeSpec node = nodes.get(id);
        ArrayList<String> parents = new ArrayList<String>(node.parents);
        Collections.sort(parents);
        for (String parent : parents) visit(parent, nodes, colors, order);
        colors.put(id, Integer.valueOf(2));
        order.add(id);
    }

    private static boolean unit(double value) { return finite(value) && value >= 0.0d && value <= 1.0d; }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
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
