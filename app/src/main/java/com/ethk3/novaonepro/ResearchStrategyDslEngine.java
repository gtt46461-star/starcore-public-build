package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded, deterministic Research AST. It cannot invoke Java, reflection, files or network. */
public final class ResearchStrategyDslEngine {
    public static final int MAX_DEPTH = 8, MAX_NODES = 64;
    private static final Set<String> OPS =
            new HashSet<String>(
                    Arrays.asList(
                            "CONST",
                            "FEATURE",
                            "ADD",
                            "SUB",
                            "MUL",
                            "SAFE_DIV",
                            "MIN",
                            "MAX",
                            "ABS",
                            "ONE_MINUS",
                            "LOG1P_POS",
                            "SQRT_POS",
                            "SIGMOID",
                            "CLIP",
                            "EMA",
                            "ROLLING_MEAN",
                            "ROLLING_SLOPE",
                            "LAG",
                            "INTERACTION"));
    private static final String[] HARD_FORBIDDEN =
            new String[] {"ACTUAL", "DRAW_RESULT", "FUTURE", "POST_DRAW", "SETTLED", "HIT_LABEL"};
    private static final List<String> PRIMITIVE_REGISTRY =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "DISTRIBUTION|DirichletSmoothedProbability|dirichlet(count,N,alpha,K)",
                            "DISTRIBUTION|ExpDecayProbability|expDecay(indicator,age,tau)",
                            "DISTRIBUTION|NormalizedEntropy|entropy10(p)",
                            "DISTRIBUTION|JensenShannon|js(P,Q)",
                            "TEMPORAL|EMA|ema(x,tau)",
                            "TEMPORAL|RollingOLSlope|slope(x,w)",
                            "TEMPORAL|Acceleration|accel(x,w1,w2)",
                            "TEMPORAL|CUSUMPositive|cusum(x,mu,k,h)",
                            "TEMPORAL|LagDifference|diff(x,k)",
                            "CROSS_POSITION|PositionExclusivity|exclusive(Q,p,n)",
                            "CROSS_POSITION|PositionMargin|positionMargin(Q,p,n)",
                            "CROSS_POSITION|DiversityAdjustedVote|divVote(votes,weights,corr,lambda)",
                            "CROSS_POSITION|LaggedMutualInformation|lagMI(X,Y,k)",
                            "PERMUTATION|NumberPosition|pos(pi,n)",
                            "PERMUTATION|MigrationVelocity|migrationV(pi_t,pi_prev,n)",
                            "PERMUTATION|MigrationAcceleration|migrationA(v_t,v_prev)",
                            "PERMUTATION|RelativePermutation|relativePerm(pi_t,pi_prev)",
                            "PERMUTATION|KendallTauDistance|kendall(piA,piB)",
                            "PERMUTATION|CycleFeatures|cycleStats(rho)",
                            "MOTIF|WeightedFeatureDistance|featureDistance(x,y,w)",
                            "WINDOW|WindowConsensus|windowConsensus(signal,reliability)",
                            "WINDOW|ShortLongConflict|shortLongConflict(s12,s96)",
                            "THIRD_BOUNDARY|ThirdMargin|thirdMargin(Q,K3)",
                            "THIRD_BOUNDARY|BoundaryRiskLogit|boundaryRisk(x,beta)",
                            "CALIBRATION|Brier|brier(p,y)",
                            "CALIBRATION|ECE10|ece10(p,y)",
                            "CALIBRATION|WilsonLower95|wilson(h,n)",
                            "STABILITY|CoverageCV|coverageCV(blockCoverage)",
                            "STABILITY|BlockMinimum|blockMin(metricBlocks)",
                            "CROSS_POSITION|LocationEntropy|locationEntropy(Q,n)",
                            "CROSS_POSITION|BestPositionMargin|bestPosMargin(Q,n)",
                            "CROSS_POSITION|CollisionPressure|collision(Q,p,n)",
                            "CROSS_POSITION|LocationIntersectionVote|locationVote(p,n,topK,w,corr,lambda)",
                            "CROSS_POSITION|AssignmentEnsembleSupport|assignmentSupport(Q,p,n,M,tau)"));

    private ResearchStrategyDslEngine() {}

    public static Node parse(String text) {
        Parser parser = new Parser(text);
        Node node = parser.node();
        parser.space();
        if (!parser.end()) throw new IllegalArgumentException("R811_DSL_TRAILING");
        validate(node);
        return node;
    }

    public static String canonical(Node node) {
        validate(node);
        return canonicalUnchecked(node);
    }

    public static String hash(Node node) {
        return V51ChecksumEngine.sha256(canonical(node));
    }

    public static List<String> primitiveRegistry() {
        return PRIMITIVE_REGISTRY;
    }

    public static String primitiveRegistryCanonicalHash() {
        StringBuilder canonical = new StringBuilder("R811_PRIMITIVE_REGISTRY_V1\n");
        for (int i = 0; i < PRIMITIVE_REGISTRY.size(); i++)
            canonical.append(PRIMITIVE_REGISTRY.get(i)).append('\n');
        return V51ChecksumEngine.sha256(canonical.toString());
    }

    /** Exact plan complexity: nodes + 0.5*depth + 2*nonlinear + 3*temporal. */
    public static double complexity(Node node) {
        validate(node);
        Complexity value = complexityOf(node, 1);
        return value.nodes
                + 0.5d * value.depth
                + 2.0d * value.nonlinear
                + 3.0d * value.temporal;
    }

    public static Node mutate(Node node, long seed) {
        validate(node);
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(seed);
        return mutateOne(node, rng);
    }

    public static Node crossover(Node left, Node right, long seed) {
        validate(left);
        validate(right);
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(seed);
        Node out =
                new Node(
                        rng.nextDouble() < 0.5d ? "ADD" : "MUL",
                        "",
                        Arrays.asList(left, right));
        validate(out);
        return out;
    }

    public static double evaluate(Node node, Map<String, Double> features) {
        validate(node);
        return finite(eval(node, features == null ? Collections.<String, Double>emptyMap() : features));
    }

    /** Primitive lookup path used by the streaming experiment runner to avoid per-row boxing. */
    public interface FeatureProvider {
        double value(String name);
    }

    public static double evaluate(Node node, FeatureProvider features) {
        validate(node);
        if (features == null) throw new IllegalArgumentException("R811_DSL_FEATURE_PROVIDER_NULL");
        return finite(eval(node, features));
    }

    public static final class Node {
        public final String op, value;
        public final List<Node> children;

        public Node(String op, String value, List<Node> children) {
            this.op = safe(op).toUpperCase(java.util.Locale.US);
            this.value = safe(value);
            this.children =
                    Collections.unmodifiableList(
                            children == null
                                    ? new ArrayList<Node>()
                                    : new ArrayList<Node>(children));
        }
    }

    public static final class FeatureRef { public final String name; public FeatureRef(String n) { name = safe(n); } }
    public static final class Window { public final int length; public Window(int n) { length = n; } }
    public static final class Transform { public final Node ast; public Transform(Node n) { ast = n; } }
    public static final class Interaction { public final Node ast; public Interaction(Node n) { ast = n; } }
    public static final class Score { public final Node ast; public Score(Node n) { ast = n; } }
    public static final class Gate { public final Node ast; public Gate(Node n) { ast = n; } }
    public static final class Calibrator { public final Node ast; public Calibrator(Node n) { ast = n; } }
    public static final class RankLoss { public final Node ast; public RankLoss(Node n) { ast = n; } }

    private static void validate(Node root) {
        if (root == null) throw new IllegalArgumentException("R811_DSL_NULL");
        Counter count = new Counter();
        validate(root, 1, count);
        if (count.value > MAX_NODES) throw new IllegalArgumentException("R811_DSL_NODE_LIMIT");
    }

    private static void validate(Node node, int depth, Counter count) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("R811_DSL_DEPTH_LIMIT");
        count.value++;
        if (!OPS.contains(node.op)) throw new IllegalArgumentException("R811_DSL_OP:" + node.op);
        int n = node.children.size();
        if ("CONST".equals(node.op)) {
            if (n != 0) throw new IllegalArgumentException("R811_DSL_CONST_ARITY");
            double value = Double.parseDouble(node.value);
            if (Double.isNaN(value) || Double.isInfinite(value))
                throw new IllegalArgumentException("R811_DSL_CONST_NONFINITE");
        } else if ("FEATURE".equals(node.op)) {
            if (n != 0 || !node.value.matches("[A-Za-z][A-Za-z0-9_.-]{0,63}"))
                throw new IllegalArgumentException("R811_DSL_FEATURE");
            String upper = node.value.toUpperCase(java.util.Locale.US);
            for (int i = 0; i < HARD_FORBIDDEN.length; i++)
                if (upper.indexOf(HARD_FORBIDDEN[i]) >= 0)
                    throw new SecurityException("R811_DSL_FORBIDDEN_FEATURE:" + node.value);
        } else if ("CLIP".equals(node.op) || "SAFE_DIV".equals(node.op)) {
            if (n != 3) throw new IllegalArgumentException("R811_DSL_TERNARY_ARITY:" + node.op);
        } else if (isTemporal(node.op)) {
            if (n != 2) throw new IllegalArgumentException("R811_DSL_TEMPORAL_ARITY:" + node.op);
        } else if (isBinary(node.op)) {
            if (n != 2) throw new IllegalArgumentException("R811_DSL_BINARY_ARITY:" + node.op);
        } else if (n != 1) throw new IllegalArgumentException("R811_DSL_UNARY_ARITY:" + node.op);
        for (int i = 0; i < n; i++) validate(node.children.get(i), depth + 1, count);
    }

    private static boolean isBinary(String op) {
        return "ADD".equals(op)
                || "SUB".equals(op)
                || "MUL".equals(op)
                || "MIN".equals(op)
                || "MAX".equals(op)
                || "INTERACTION".equals(op);
    }

    private static boolean isTemporal(String op) {
        return "EMA".equals(op)
                || "ROLLING_MEAN".equals(op)
                || "ROLLING_SLOPE".equals(op)
                || "LAG".equals(op);
    }

    private static String canonicalUnchecked(Node node) {
        if ("CONST".equals(node.op))
            return "CONST@" + Long.toHexString(Double.doubleToLongBits(Double.parseDouble(node.value)));
        if ("FEATURE".equals(node.op)) return "FEATURE@" + node.value;
        ArrayList<String> children = new ArrayList<String>();
        for (int i = 0; i < node.children.size(); i++)
            children.add(canonicalUnchecked(node.children.get(i)));
        if ("ADD".equals(node.op) || "MUL".equals(node.op)) Collections.sort(children);
        StringBuilder out = new StringBuilder(node.op).append('(');
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) out.append(',');
            out.append(children.get(i));
        }
        return out.append(')').toString();
    }

    private static Node mutateOne(Node node, ResearchDeterministicRngR811 rng) {
        if ("CONST".equals(node.op)) {
            double value = Double.parseDouble(node.value);
            return new Node("CONST", Double.toString(value + (rng.nextDouble() - 0.5d) * 0.1d), null);
        }
        if (node.children.size() == 0) return node;
        ArrayList<Node> children = new ArrayList<Node>(node.children);
        int index = rng.nextInt(children.size());
        children.set(index, mutateOne(children.get(index), rng));
        return new Node(node.op, node.value, children);
    }

    private static double eval(Node node, Map<String, Double> f) {
        if ("CONST".equals(node.op)) return Double.parseDouble(node.value);
        if ("FEATURE".equals(node.op)) {
            Double value = f.get(node.value);
            return value == null ? 0.0d : finite(value.doubleValue());
        }
        double a = eval(node.children.get(0), f);
        double b = node.children.size() > 1 ? eval(node.children.get(1), f) : 0.0d;
        if ("ADD".equals(node.op)) return a + b;
        if ("SUB".equals(node.op)) return a - b;
        if ("MUL".equals(node.op) || "INTERACTION".equals(node.op)) return a * b;
        if ("SAFE_DIV".equals(node.op)) {
            double eps = Math.max(1e-15d, Math.abs(eval(node.children.get(2), f)));
            double denominator = Math.abs(b) < eps ? (b < 0.0d ? -eps : eps) : b;
            return a / denominator;
        }
        if ("MIN".equals(node.op)) return Math.min(a, b);
        if ("MAX".equals(node.op)) return Math.max(a, b);
        if ("ABS".equals(node.op)) return Math.abs(a);
        if ("ONE_MINUS".equals(node.op)) return 1.0d - a;
        if ("LOG1P_POS".equals(node.op)) return Math.log1p(Math.max(0.0d, a));
        if ("SQRT_POS".equals(node.op)) return Math.sqrt(Math.max(0.0d, a));
        if ("SIGMOID".equals(node.op)) return sigmoid(a);
        if ("CLIP".equals(node.op)) {
            double hi = eval(node.children.get(2), f);
            double lo = Math.min(b, hi), upper = Math.max(b, hi);
            return Math.max(lo, Math.min(a, upper));
        }
        return a; // Window transforms consume pre-draw, precomputed feature values.
    }

    private static double eval(Node node, FeatureProvider f) {
        if ("CONST".equals(node.op)) return Double.parseDouble(node.value);
        if ("FEATURE".equals(node.op)) return finite(f.value(node.value));
        double a = eval(node.children.get(0), f);
        double b = node.children.size() > 1 ? eval(node.children.get(1), f) : 0.0d;
        if ("ADD".equals(node.op)) return a + b;
        if ("SUB".equals(node.op)) return a - b;
        if ("MUL".equals(node.op) || "INTERACTION".equals(node.op)) return a * b;
        if ("SAFE_DIV".equals(node.op)) {
            double eps = Math.max(1e-15d, Math.abs(eval(node.children.get(2), f)));
            double denominator = Math.abs(b) < eps ? (b < 0.0d ? -eps : eps) : b;
            return a / denominator;
        }
        if ("MIN".equals(node.op)) return Math.min(a, b);
        if ("MAX".equals(node.op)) return Math.max(a, b);
        if ("ABS".equals(node.op)) return Math.abs(a);
        if ("ONE_MINUS".equals(node.op)) return 1.0d - a;
        if ("LOG1P_POS".equals(node.op)) return Math.log1p(Math.max(0.0d, a));
        if ("SQRT_POS".equals(node.op)) return Math.sqrt(Math.max(0.0d, a));
        if ("SIGMOID".equals(node.op)) return sigmoid(a);
        if ("CLIP".equals(node.op)) {
            double hi = eval(node.children.get(2), f);
            double lo = Math.min(b, hi), upper = Math.max(b, hi);
            return Math.max(lo, Math.min(a, upper));
        }
        return a;
    }

    private static double sigmoid(double value) {
        if (value >= 0.0d) return 1.0d / (1.0d + Math.exp(-Math.min(60.0d, value)));
        double exp = Math.exp(Math.max(-60.0d, value));
        return exp / (1.0d + exp);
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    private static Complexity complexityOf(Node node, int depth) {
        int nodes = 1;
        int maximumDepth = depth;
        int nonlinear = isNonlinear(node.op) ? 1 : 0;
        int temporal = isTemporal(node.op) ? 1 : 0;
        for (int i = 0; i < node.children.size(); i++) {
            Complexity child = complexityOf(node.children.get(i), depth + 1);
            nodes += child.nodes;
            maximumDepth = Math.max(maximumDepth, child.depth);
            nonlinear += child.nonlinear;
            temporal += child.temporal;
        }
        return new Complexity(nodes, maximumDepth, nonlinear, temporal);
    }

    private static boolean isNonlinear(String op) {
        return "SAFE_DIV".equals(op)
                || "ABS".equals(op)
                || "LOG1P_POS".equals(op)
                || "SQRT_POS".equals(op)
                || "SIGMOID".equals(op)
                || "CLIP".equals(op)
                || "INTERACTION".equals(op);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static final class Counter { int value; }
    private static final class Complexity {
        final int nodes, depth, nonlinear, temporal;
        Complexity(int nodes, int depth, int nonlinear, int temporal) {
            this.nodes = nodes;
            this.depth = depth;
            this.nonlinear = nonlinear;
            this.temporal = temporal;
        }
    }

    private static final class Parser {
        final String text;
        int at;
        Parser(String text) { this.text = safe(text); }
        boolean end() { return at >= text.length(); }
        void space() { while (!end() && Character.isWhitespace(text.charAt(at))) at++; }
        Node node() {
            space();
            String token = token();
            space();
            if (!end() && text.charAt(at) == '@') {
                at++;
                int start = at;
                while (!end() && text.charAt(at) != ',' && text.charAt(at) != ')') at++;
                String value = text.substring(start, at).trim();
                if ("CONST".equalsIgnoreCase(token)) {
                    try {
                        long bits = Long.parseUnsignedLong(value, 16);
                        return new Node(
                                "CONST",
                                Double.toString(Double.longBitsToDouble(bits)),
                                null);
                    } catch (RuntimeException error) {
                        throw new IllegalArgumentException("R811_DSL_CANONICAL_CONST", error);
                    }
                }
                if ("FEATURE".equalsIgnoreCase(token))
                    return new Node("FEATURE", value, null);
                throw new IllegalArgumentException("R811_DSL_CANONICAL_AT:" + token);
            }
            if (end() || text.charAt(at) != '(') {
                if (token.matches("[-+]?[0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?"))
                    return new Node("CONST", token, null);
                return new Node("FEATURE", token, null);
            }
            at++;
            String op = token.toUpperCase(java.util.Locale.US);
            if ("CONST".equals(op) || "FEATURE".equals(op)) {
                int start = at;
                while (!end() && text.charAt(at) != ')') at++;
                if (end()) throw new IllegalArgumentException("R811_DSL_UNCLOSED");
                String value = text.substring(start, at).trim();
                at++;
                return new Node(op, value, null);
            }
            ArrayList<Node> children = new ArrayList<Node>();
            for (; ; ) {
                space();
                if (!end() && text.charAt(at) == ')') { at++; break; }
                children.add(node());
                space();
                if (!end() && text.charAt(at) == ',') { at++; continue; }
                if (!end() && text.charAt(at) == ')') { at++; break; }
                throw new IllegalArgumentException("R811_DSL_SEPARATOR");
            }
            return new Node(op, "", children);
        }
        String token() {
            space();
            int start = at;
            while (!end()) {
                char c = text.charAt(at);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-' || c == '+') at++;
                else break;
            }
            if (start == at) throw new IllegalArgumentException("R811_DSL_TOKEN");
            return text.substring(start, at);
        }
    }
}
