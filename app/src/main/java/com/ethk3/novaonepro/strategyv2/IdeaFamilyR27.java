package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** R27-023 idea family ("思路族", docs/04 4.3): a generator, a family-specific mutation and a leaf prior.
 * Implementations are the IdeaI00…IdeaI13 classes in this package (kept flat so the host protocol compile, which
 * compiles strategyv2/*.java, keeps working). */
public interface IdeaFamilyR27 {
    String id();
    String nameZh();
    String coreIdea();
    PolicySpecR27 generate(StatsR27.SplitMix rng, Context ctx);
    /** Main-pool proposal boundary: generators must return complete contracts with their own provenance. */
    default PolicySpecR27 propose(StatsR27.SplitMix rng, Context ctx) {
        PolicySpecR27 spec = generate(rng, ctx);
        List<String> violations;
        try { violations = PolicyContractR30.violations(spec); }
        catch (RuntimeException malformed) { throw new IllegalStateException("R30_GENERATOR_CONTRACT:" + id() + ":MALFORMED_PROGRAM", malformed); }
        if (!violations.isEmpty()) throw new IllegalStateException("R30_GENERATOR_CONTRACT:" + id() + ":" + violations);
        PolicySpecR27.Meta m = spec.meta;
        return spec.withMeta(new PolicySpecR27.Meta(m.idea, m.operator, m.parents, m.generation, m.seed, m.reason + "；" + id() + "：" + coreIdea()));
    }
    /** Family-specific mutation; may return null to defer to the generic operators. */
    PolicySpecR27 mutate(PolicySpecR27 parent, StatsR27.SplitMix rng, Context ctx);

    /** Everything a generator may read. All of it is derived from H_TRAIN/H_SELECT only. */
    final class Context {
        public int generation, maxDepth = BirthGateR27.MAX_DEPTH;
        public double championActRate = 0.15;
        public PolicySpecR27 champion;
        /** Champion's H_SELECT failure cluster (I10), e.g. "REGIME_LOW". */
        public String failureCluster = "";
        public List<PolicySpecR27> archive = Collections.emptyList();
        /** Legacy seeds exported by LegacyPolicyExportR27 (I12). */
        public List<PolicySpecR27> legacySeeds = Collections.emptyList();
        /** Atom-state weight per leaf kind (USEFUL ×2, HARMFUL ×0.25; docs/07 7.1). */
        public java.util.Map<String, Double> leafWeight = new java.util.HashMap<>();
        /** Derived atoms / molecules usable as leaves (docs/07 7.1-3, 7.2). */
        public List<PolicyAstR27.Node> derivedLeaves = Collections.emptyList();
        /** Current dataset's role-checked evaluator; selectors and repair profiles use H_TRAIN/H_SELECT only. */
        public EvalContextR27 evaluation;
    }

    static PolicySpecR27.Meta meta(String idea, String op, int gen, long seed, String reason, String... parents) {
        return new PolicySpecR27.Meta(idea, op, Arrays.asList(parents), gen, seed, reason);
    }

    /** Typed random program construction (ramped half-and-half) used by all generators. */
    final class Grow {
        private Grow() {}
        static final String[] UNARY = {"neg", "abs", "sigmoid", "tanh", "rank", "zscore", "lagdiff"};
        static final String[] BINARY = {"add", "sub", "mul", "max", "min"};

        public static int window(StatsR27.SplitMix r) { return FeatureCacheR27.WINDOWS[r.nextInt(5)]; }
        public static int pos(StatsR27.SplitMix r) { return 1 + r.nextInt(10); }

        /** Random leaf of the given kind with legal parameters; src defaults to the target position half the time. */
        public static PolicyAstR27.Node leaf(String kind, int tp, StatsR27.SplitMix r) {
            int src = r.chance(0.5) ? tp : pos(r);
            switch (kind) {
                case "freq": case "freqnb": case "meangap": case "oddeven": case "bigsmall": return PolicyAstR27.leaf(kind, src, window(r));
                case "fdecay": return PolicyAstR27.leaf(kind, src, FeatureCacheR27.HALF_LIVES[r.nextInt(3)]);
                case "gap": case "gaprank": case "crosscond": case "neighbor": return PolicyAstR27.leaf(kind, src);
                case "cross": return PolicyAstR27.leaf(kind, src, 1 + r.nextInt(3));
                case "regime": return PolicyAstR27.leaf(kind, window(r));
                case "knn": return PolicyAstR27.leaf(kind, 2 + r.nextInt(4), 10 + r.nextInt(41));
                case "family": return PolicyAstR27.leaf(kind, PolicyAstR27.FAMILY_LEAVES.get(r.nextInt(PolicyAstR27.FAMILY_LEAVES.size())));
                case "randu": return PolicyAstR27.leaf(kind, r.nextInt(1 << 20));
                default: return PolicyAstR27.leaf(kind);
            }
        }

        public static String pick(String[] kinds, Context ctx, StatsR27.SplitMix r) {
            double[] w = new double[kinds.length]; double s = 0;
            for (int i = 0; i < kinds.length; i++) { Double x = ctx == null ? null : ctx.leafWeight.get(kinds[i]); w[i] = x == null ? 1 : x; s += w[i]; }
            double u = r.nextDouble() * s;
            for (int i = 0; i < kinds.length; i++) { u -= w[i]; if (u <= 0) return kinds[i]; }
            return kinds[kinds.length - 1];
        }

        /** Grows a SCORE-typed tree over the given leaf kinds; every leaf is wrapped by zscore so sums are type-safe. */
        public static PolicyAstR27.Node tree(String[] kinds, int tp, int depth, boolean full, Context ctx, StatsR27.SplitMix r) {
            if (depth <= 1 || (!full && r.chance(0.3))) {
                if (ctx != null && !ctx.derivedLeaves.isEmpty() && r.chance(0.1)) return ctx.derivedLeaves.get(r.nextInt(ctx.derivedLeaves.size()));
                return PolicyAstR27.op("zscore", leaf(pick(kinds, ctx, r), tp, r));
            }
            double u = r.nextDouble();
            if (u < 0.25) return PolicyAstR27.op(UNARY[r.nextInt(UNARY.length)], tree(kinds, tp, depth - 1, full, ctx, r));
            if (u < 0.75) return PolicyAstR27.op(BINARY[r.nextInt(BINARY.length)], tree(kinds, tp, depth - 1, full, ctx, r), tree(kinds, tp, depth - 1, full, ctx, r));
            if (u < 0.85) return PolicyAstR27.op("mix", round(r.nextDouble()), tree(kinds, tp, depth - 1, full, ctx, r), tree(kinds, tp, depth - 1, full, ctx, r));
            if (u < 0.92) return PolicyAstR27.op("ema", round(0.05 + 0.9 * r.nextDouble()), tree(kinds, tp, depth - 1, full, ctx, r));
            return PolicyAstR27.op("mul", PolicyAstR27.constant(round(0.25 + 2 * r.nextDouble())), tree(kinds, tp, depth - 1, full, ctx, r));
        }

        public static double round(double x) { return Math.round(x * 1000) / 1000.0; }

        public static PolicySpecR27 spec(PolicyAstR27.Node ast, int tp, StatsR27.SplitMix r, PolicySpecR27.Meta m) {
            boolean quantile = r.chance(0.75);
            double threshold = quantile ? 0 : round(0.01 + 0.12 * r.nextDouble()), rate = quantile ? round(0.05 + 0.25 * r.nextDouble()) : 0;
            return new PolicySpecR27(ast, tp, quantile ? PolicySpecR27.Rule.QUANTILE : PolicySpecR27.Rule.MARGIN, threshold, rate, 1.0, true, round(0.002 + 0.03 * r.nextDouble()), round(0.002 + 0.02 * r.nextDouble()), m);
        }

        /** Leaf-swap mutation restricted to the family's own kinds (family-specific operator). */
        public static PolicySpecR27 swapLeaf(PolicySpecR27 parent, String[] kinds, String idea, int gen, Context ctx, StatsR27.SplitMix r) {
            return MutationOpsR27.familyLeaf(parent,kinds,idea,gen,ctx,r);
        }
    }
}
