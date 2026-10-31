package com.ethk3.novaonepro.strategyv2;

/** Six executable structure generators. They share the existing policy format, birth gate and qualification path. */
public final class StructureGeneratorRegistryR30 {
    private StructureGeneratorRegistryR30() {}
    public static final IdeaFamilyR27[] ALL = {new WindowDifference(), new PositionRotation(), new GapRebound(),
            new NeighborTransition(), new ParitySizeState(), new ConditionalIntersection()};
    static final int[] W = {8, 32, 128, 512};
    static PolicyAstR27.Node z(PolicyAstR27.Node node) { return PolicyAstR27.op("zscore", node); }

    public abstract static class Generator implements IdeaFamilyR27 {
        private final String id, name, idea; private final String[] kinds;
        Generator(String id, String name, String idea, String... kinds) { this.id = id; this.name = name; this.idea = idea; this.kinds = kinds; }
        public String id() { return id; } public String nameZh() { return name; } public String coreIdea() { return idea; }
        protected int position(StatsR27.SplitMix r, Context ctx) { return Grow.pos(r); }
        protected abstract PolicyAstR27.Node program(int position, StatsR27.SplitMix r);
        public PolicySpecR27 generate(StatsR27.SplitMix r, Context ctx) {
            int tp = position(r, ctx); PolicyAstR27.Node ast = program(tp, r);
            return Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", ctx == null ? 0 : ctx.generation, r.state,
                    nameZh() + "：第" + tp + "位；" + ast.toString()));
        }
        public PolicySpecR27 mutate(PolicySpecR27 parent, StatsR27.SplitMix r, Context ctx) {
            return Grow.swapLeaf(parent, kinds, id(), ctx == null ? 0 : ctx.generation, ctx, r);
        }
    }

    public static final class WindowDifference extends Generator {
        public WindowDifference() { super("I15", "窗口差分族", "同位置短长窗口频率偏离", "freq", "freqnb", "fdecay"); }
        protected PolicyAstR27.Node program(int tp, StatsR27.SplitMix r) {
            int a = r.nextInt(W.length - 1), b = a + 1 + r.nextInt(W.length - a - 1);
            return PolicyAstR27.op("sub", z(PolicyAstR27.leaf("freq", tp, W[a])), z(PolicyAstR27.leaf("freq", tp, W[b])));
        }
    }
    public static final class PositionRotation extends Generator {
        public PositionRotation() { super("I16", "位置轮换族", "以轮换位置的滞后与频率预测固定目标位置", "cross", "crosscond", "freq"); }
        protected PolicyAstR27.Node program(int tp, StatsR27.SplitMix r) {
            int source = (tp - 1 + 1 + r.nextInt(9)) % 10 + 1;
            return PolicyAstR27.op("add", z(PolicyAstR27.leaf("cross", source, 1 + r.nextInt(3))), z(PolicyAstR27.leaf("freq", source, W[r.nextInt(W.length)])));
        }
    }
    public static final class GapRebound extends Generator {
        public GapRebound() { super("I17", "遗漏回补族", "当前遗漏相对历史平均遗漏的偏离", "gap", "gaprank", "meangap"); }
        protected PolicyAstR27.Node program(int tp, StatsR27.SplitMix r) {
            return PolicyAstR27.op("sub", z(PolicyAstR27.leaf("gap", tp)), z(PolicyAstR27.leaf("meangap", tp, W[r.nextInt(W.length)])));
        }
    }
    public static final class NeighborTransition extends Generator {
        public NeighborTransition() { super("I18", "邻号转移族", "上期邻号与一阶转移的组合", "neighbor", "trans1", "trans2"); }
        protected PolicyAstR27.Node program(int tp, StatsR27.SplitMix r) {
            return PolicyAstR27.op("mix", 0.2 + 0.6 * r.nextDouble(), z(PolicyAstR27.leaf("neighbor", tp)), z(PolicyAstR27.leaf("trans1")));
        }
    }
    public static final class ParitySizeState extends Generator {
        public ParitySizeState() { super("I19", "奇偶大小状态族", "依据过去体制在奇偶与大小结构间切换", "oddeven", "bigsmall", "regime"); }
        protected PolicyAstR27.Node program(int tp, StatsR27.SplitMix r) {
            int w = W[r.nextInt(W.length)];
            return PolicyAstR27.op("if", PolicyAstR27.leaf("regime", 128), PolicyAstR27.constant(0.1),
                    z(PolicyAstR27.leaf("oddeven", tp, w)), z(PolicyAstR27.leaf("bigsmall", tp, w)));
        }
    }
    public static final class ConditionalIntersection extends Generator {
        public ConditionalIntersection() { super("I20", "条件交集族", "H_SELECT短窗与邻位置长窗集合交集及过去体制条件", "freq", "freqnb", "regime"); }
        @Override protected int position(StatsR27.SplitMix r, Context ctx) {
            int seed = Grow.pos(r);
            if (ctx == null || ctx.evaluation == null || ctx.evaluation.split.selectTo < 128) return seed;
            EvalContextR27 eval = ctx.evaluation; int target = eval.split.selectTo;
            int[][] shortCounts = StructureWindowR30.counts(eval.data.draws, target, 32);
            int[][] longCounts = StructureWindowR30.counts(eval.data.draws, target, 128), neighbors = new int[10][11];
            for (int p = 0; p < 10; p++) for (int n = 1; n <= 10; n++) neighbors[p][n] = longCounts[(p+9)%10][n] + longCounts[(p+1)%10][n];
            int best = seed, agreement = -1;
            for (int offset = 0; offset < 10; offset++) {
                int position = (seed - 1 + offset) % 10 + 1;
                int[] a = StructureWindowR30.top3(shortCounts, position), b = StructureWindowR30.top3(neighbors, position);
                int overlap = Integer.bitCount(BehaviorSignatureR30.mask(a[0],a[1],a[2]) & BehaviorSignatureR30.mask(b[0],b[1],b[2]));
                if (overlap > agreement) { best = position; agreement = overlap; }
            }
            return best;
        }
        protected PolicyAstR27.Node program(int tp, StatsR27.SplitMix r) {
            PolicyAstR27.Node intersect = PolicyAstR27.op("min", z(PolicyAstR27.leaf("freq", tp, 32)), z(PolicyAstR27.leaf("freqnb", tp, 128)));
            return PolicyAstR27.op("if", PolicyAstR27.leaf("regime", 128), PolicyAstR27.constant(0.1), intersect, z(PolicyAstR27.leaf("freq", tp, 128)));
        }
    }
}
