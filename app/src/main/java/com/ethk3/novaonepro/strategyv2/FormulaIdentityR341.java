package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Additional research identities. The existing policy hash and executable bytes remain authoritative. */
public final class FormulaIdentityR341 {
    private static final String VERSION = "R341_EXECUTABLE_FORMULA_1";
    private static final Set<String> COMMUTATIVE = new HashSet<>(Arrays.asList("add", "mul", "max", "min"));
    private static final Set<String> POSITION_INPUT = new HashSet<>(Arrays.asList(
            "freq", "freqnb", "fdecay", "gap", "gaprank", "meangap", "oddeven", "bigsmall",
            "cross", "crosscond", "model_surface", "neighbor"));
    private FormulaIdentityR341() {}

    public static final class Abi {
        final String numerical, feature;
        public Abi(String numerical, String feature) {
            this.numerical = numerical;
            this.feature = feature;
        }
    }

    static final Abi CURRENT = new Abi(ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256, "R27_DRAW_AND_MODEL_FEATURES_1");

    public static byte[] canonical(PolicyAstR27.Node node, Abi abi) {
        PolicyAstR27.type(node);
        String reason = PolicyAstR27.rangeAndCausality(node);
        if (reason != null) throw new IllegalArgumentException(reason);
        return JsonR27.write(Arrays.asList(VERSION, abi.numerical, abi.feature, normalize(node).toString()))
                .getBytes(StandardCharsets.UTF_8);
    }

    static String normalizedPolicy(PolicySpecR27 spec) {
        Map<String, Object> executable = spec.executable();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("ast", spec.rule == PolicySpecR27.Rule.RANDOM ? spec.ast.toString() : normalize(spec.ast).toString());
        executable.put("model", model);
        return StatsR27.sha256(JsonR27.write(Arrays.asList(VERSION, CURRENT.numerical, CURRENT.feature, executable)));
    }

    private static PolicyAstR27.Node normalize(PolicyAstR27.Node node) {
        if (node.leaf()) return node;
        PolicyAstR27.Node[] children = new PolicyAstR27.Node[node.kids.length];
        for (int i = 0; i < children.length; i++) children[i] = normalize(node.kids[i]);
        if (node.op.equals("neg") && children.length == 1 && children[0].op.equals("neg")
                && children[0].kids.length == 1
                && PolicyAstR27.type(children[0].kids[0]) == PolicyAstR27.Type.SCORE) return children[0].kids[0];
        if (children.length == 2 && COMMUTATIVE.contains(node.op)
                && children[0].toString().compareTo(children[1].toString()) > 0) {
            PolicyAstR27.Node swap = children[0];
            children[0] = children[1];
            children[1] = swap;
        }
        return new PolicyAstR27.Node(node.op, node.ints, node.param, children);
    }

    static String structure(PolicySpecR27 spec) {
        Set<Integer> external = new HashSet<>();
        String shape = shape(normalize(spec.ast), spec.position, external);
        return StatsR27.sha256(JsonR27.write(Arrays.asList(VERSION, shape, external.size(), spec.rule.name(),
                spec.fitTemperature, SetIntersectionPolicyR30.exact(spec))));
    }

    private static String shape(PolicyAstR27.Node node, int target, Set<Integer> sources) {
        String operator = node.op;
        if (node.ints.length > 0 && POSITION_INPUT.contains(operator)) {
            int source = node.ints[0];
            if (source != target) sources.add(source);
            operator += source == target ? ":TARGET" : ":OTHER_POSITION";
        } else if (node.ints.length > 0 && operator.equals("family")) {
            operator += ':' + Integer.toString(node.ints[0]);
        }
        List<String> children = new ArrayList<>();
        for (PolicyAstR27.Node child : node.kids) children.add(shape(child, target, sources));
        if (COMMUTATIVE.contains(node.op)) Collections.sort(children);
        return operator + children;
    }

    static String structure(ModelGenomeSU2 genome) {
        List<Object> members = new ArrayList<>();
        for (ModelGenomeSU2.Member member : genome.members) {
            Map<String, Object> graph = new LinkedHashMap<>();
            graph.put("implementation", member.kind);
            switch (member.kind) {
                case "R26F01":
                    graph.put("cross", member.params.getOrDefault("cross", "true"));
                    graph.put("lagEdges",member.params.getOrDefault("lags","3"));
                    break;
                case "R26F02":
                    graph.put("factors", member.params.getOrDefault("factors", "true"));
                    graph.put("graph", member.params.getOrDefault("graph", "0"));
                    break;
                case "R26F03": case "R26F04": case "R26F05": case "R26F06": case "R26F07":
                    graph.put("graph", member.params.getOrDefault("graph", "0"));
                    break;
                case "R26F08":
                    graph.put("transition", member.params.getOrDefault("transition", "true"));
                    if (!"false".equals(graph.get("transition"))) {
                        graph.put("latentStates", member.params.getOrDefault("K", ModelEvolutionSU2.centralParams("HMM").get("K")));
                    }
                    break;
                case "R26F13": case "R26F14":
                    graph.put("executedFormula",member.params.getOrDefault("formula","0"));
                    break;
                case "R26F19":
                    for (String key : Arrays.asList("tensor", "position", "state")) graph.put(key, member.params.get(key));
                    break;
                case "R26F20":
                    graph.put("fragmentImplementation", member.params.getOrDefault("fragments", "legacy"));
                    graph.put("retrieval", member.params.getOrDefault("retrieval", "MATCH"));
                    break;
                default:
                    break;
            }
            members.add(graph);
        }
        return StatsR27.sha256(JsonR27.write(Arrays.asList(VERSION, members, genome.pool, genome.sink, genome.calibrated)));
    }

    static String origin(PolicySpecR27 spec) {
        if (spec.meta == null) return "REGISTERED_SEED";
        if ("LEGACY_SEED".equals(spec.meta.operator)) return "IMPORTED_SEED";
        if (spec.meta.parents.isEmpty()) return "ROOT_PROPOSAL_AWAITING_FIRST_EVALUATION";
        return "PARENTED_PROPOSAL";
    }

    public static final class NodeEdit {
        public final String path, before, after;
        NodeEdit(String path, String before, String after) {
            this.path = path;
            this.before = before;
            this.after = after;
        }
        Map<String, String> fields() {
            Map<String, String> result = new LinkedHashMap<>();
            result.put("path", path); result.put("beforeSubtreeSha256", before); result.put("afterSubtreeSha256", after);
            return result;
        }
    }

    public static List<NodeEdit> diff(PolicyAstR27.Node parent, PolicyAstR27.Node child) {
        List<NodeEdit> changes = new ArrayList<>();
        diff(normalize(parent), normalize(child), "root", changes);
        return Collections.unmodifiableList(changes);
    }

    private static void diff(PolicyAstR27.Node prior, PolicyAstR27.Node next, String path, List<NodeEdit> edits) {
        if (prior.toString().equals(next.toString())) return;
        if (!prior.op.equals(next.op) || prior.kids.length != next.kids.length
                || !Arrays.equals(prior.ints, next.ints) || Double.compare(prior.param, next.param) != 0) {
            edits.add(new NodeEdit(path, StatsR27.sha256(prior.toString()), StatsR27.sha256(next.toString())));
            if (!prior.op.equals(next.op) || prior.kids.length != next.kids.length) return;
        }
        for (int i = 0; i < prior.kids.length; i++) diff(prior.kids[i], next.kids[i], path + '/' + i, edits);
    }

    public static final class Behavior {
        public final String probabilitySha256, decisionSha256;
        public final int supported;
        Behavior(String probabilitySha256, String decisionSha256, int supported) {
            this.probabilitySha256 = probabilitySha256;
            this.decisionSha256 = decisionSha256;
            this.supported = supported;
        }
    }

    static Behavior fingerprint(ReplayEvaluatorR27.Trace probe, ReplayDatasetR27 input, int start, int end) {
        List<Object> probabilities = new ArrayList<>(), decisions = new ArrayList<>();
        probabilities.add(input.datasetHash); decisions.add(input.datasetHash);
        probabilities.add(probe.position); decisions.add(probe.position);
        int supported = 0;
        for (int row = start; row < end; row++) {
            if (probe.skipped[row]) {
                probabilities.add(Arrays.asList(input.ordinals[row], "MISSING"));
                decisions.add(Arrays.asList(input.ordinals[row], "MISSING"));
                continue;
            }
            supported++;
            double[] values = probe.probabilityRows[row];
            List<String> exact = new ArrayList<>(10);
            for (double probability : values) exact.add(Long.toHexString(Double.doubleToLongBits(probability)));
            probabilities.add(Arrays.asList(input.ordinals[row], exact));
            decisions.add(Arrays.asList(input.ordinals[row], probe.act[row], probe.k3[row * 3],
                    probe.k3[row * 3 + 1], probe.k3[row * 3 + 2]));
        }
        return new Behavior(StatsR27.sha256(JsonR27.write(probabilities)),
                StatsR27.sha256(JsonR27.write(decisions)), supported);
    }
}
