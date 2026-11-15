package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Deterministic structural formula creator for the Research sandbox.
 *
 * <p>This is not a list of coefficient variants.  It composes different causal motifs (agreement
 * floor, signal disagreement, raw/rank residual, uncertainty ratio and complementary evidence)
 * into bounded DSL trees.  Every result is parser-validated, reproducible and forbidden from
 * accessing outcomes or Production authorization.
 */
public final class ResearchNovelFormulaSynthesizerR903 {
    private static final String[] SIGNALS =
            new String[] {
                "raw_action_p", "rank_score", "third_margin", "stability",
                "window_consensus", "assignment_support"
            };

    private ResearchNovelFormulaSynthesizerR903() {}

    public static List<ResearchStrategyDslEngine.Node> create(
            String failureLayer, String hypothesis, String datasetHash, int requested) {
        int bound = Math.max(4, Math.min(24, requested));
        long seed =
                ResearchDeterministicRngR811.seed(
                        "R903_NOVEL_TOPOLOGY|"
                                + safe(failureLayer)
                                + '|'
                                + safe(hypothesis)
                                + '|'
                                + safe(datasetHash));
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(seed);
        ArrayList<ResearchStrategyDslEngine.Node> output =
                new ArrayList<ResearchStrategyDslEngine.Node>();
        HashSet<String> seen = new HashSet<String>();
        int attempts = 0;
        while (output.size() < bound && attempts++ < bound * 12) {
            String a = feature(SIGNALS[rng.nextInt(SIGNALS.length)]);
            String b = feature(SIGNALS[rng.nextInt(SIGNALS.length)]);
            String c = feature(SIGNALS[rng.nextInt(SIGNALS.length)]);
            String risk = feature("entropy");
            double w1 = weight(rng, 0.25d, 0.70d);
            double w2 = weight(rng, 0.10d, 0.45d);
            double w3 = weight(rng, 0.08d, 0.35d);
            String expression;
            switch (rng.nextInt(5)) {
                case 0: // reward agreement but penalize the absolute disagreement explicitly.
                    expression =
                            "SIGMOID(SUB(ADD(MUL(CONST("
                                    + w1 + ")," + a + "),MUL(CONST(" + w2 + "),MIN(" + b + ',' + c
                                    + "))),MUL(CONST(" + w3 + "),ABS(SUB(" + b + ',' + c + ")))))";
                    break;
                case 1: // the weakest supporting axis limits confidence (robust bottleneck).
                    expression =
                            "CLIP(ADD(MUL(CONST(" + w1 + "),CLIP(" + a
                                    + ",CONST(0),CONST(1))),MUL(CONST(" + w2 + "),MIN(MIN(" + b
                                    + ',' + c + "),ONE_MINUS(CLIP(" + risk
                                    + ",CONST(0),CONST(1)))))),CONST(0),CONST(1))";
                    break;
                case 2: // raw/rank residual is evidence of model disagreement, not a bonus.
                    expression =
                            "CLIP(SUB(ADD(MUL(CONST(" + w1
                                    + "),FEATURE(raw_action_p)),MUL(CONST(" + w2
                                    + "),SIGMOID(FEATURE(rank_score)))),MUL(CONST(" + w3
                                    + "),ABS(SUB(FEATURE(raw_action_p),SIGMOID(FEATURE(rank_score)))))),CONST(0),CONST(1))";
                    break;
                case 3: // margin-to-risk ratio is bounded by a consensus floor.
                    expression =
                            "SIGMOID(ADD(MUL(CONST(" + w1 + ")," + a + "),MUL(CONST(" + w2
                                    + "),MIN(SAFE_DIV(ABS(FEATURE(third_margin)),ADD(CONST(0.1),"
                                    + risk + "),CONST(0.001)),MIN(" + b + ',' + c + ")))))";
                    break;
                default: // complementary evidence: high score only when distinct axes co-support.
                    expression =
                            "SIGMOID(SUB(ADD(MUL(CONST(" + w1 + ")," + a
                                    + "),MUL(CONST(" + w2 + "),INTERACTION(" + b
                                    + ",ONE_MINUS(CLIP(" + c
                                    + ",CONST(0),CONST(1)))))),MUL(CONST(" + w3 + ")," + risk + ")))";
                    break;
            }
            try {
                ResearchStrategyDslEngine.Node node = ResearchStrategyDslEngine.parse(expression);
                String canonical = ResearchStrategyDslEngine.canonical(node);
                if (seen.add(canonical)) output.add(node);
            } catch (IllegalArgumentException invalidGeneratedTree) {
                // A bounded generation miss is discarded; no invalid AST is ever persisted.
            }
        }
        return Collections.unmodifiableList(output);
    }

    private static String feature(String name) {
        return "FEATURE(" + name + ')';
    }

    private static double weight(ResearchDeterministicRngR811 rng, double min, double max) {
        double value = min + (max - min) * rng.nextDouble();
        return Math.rint(value * 1000.0d) / 1000.0d;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
