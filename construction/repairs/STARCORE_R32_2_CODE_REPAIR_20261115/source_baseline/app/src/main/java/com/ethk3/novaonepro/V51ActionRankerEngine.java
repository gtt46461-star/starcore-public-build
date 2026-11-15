package com.ethk3.novaonepro;

/** B10 sole within-issue rank owner over one 1200-action group. */
public final class V51ActionRankerEngine {
    public static final String V57_B10_OWNER = "SOLE_ACTION_RANK_OWNER";

    public enum RankStatus {
        PROVEN_PAIRWISE_LISTWISE_ARTIFACT,
        UNTRAINED_RESEARCH_ORDER
    }

    public static final class RankResult {
        public final V51Models.PositionAction[] ranked;
        public final V51Models.PositionAction rankOne;
        public final RankStatus status;
        public final boolean artifactProven;
        public final double topMargin;
        public final String checksum;

        RankResult(
                V51Models.PositionAction[] a,
                V51Models.PositionAction one,
                RankStatus s,
                boolean proven,
                double m) {
            ranked = a;
            rankOne = one;
            status = s;
            artifactProven = proven;
            topMargin = m;
            checksum =
                    V51ChecksumEngine.sha256(
                            String.valueOf(s)
                                    + '|'
                                    + proven
                                    + '|'
                                    + (one == null ? "" : one.checksum)
                                    + '|'
                                    + m
                                    + '|'
                                    + a.length);
        }
    }

    public V51Models.PositionAction[] rank(V51Models.PositionAction[] actions, double[][] meta) {
        return rankDetailed(actions, meta).ranked;
    }

    public RankResult rankDetailed(V51Models.PositionAction[] actions, double[][] meta) {
        if (actions == null
                || actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS
                || meta == null
                || meta.length != actions.length)
            throw new IllegalArgumentException("B10_RANK_GROUP_NOT_1200");
        java.util.HashSet<Integer> actionKeys=new java.util.HashSet<Integer>();
        for(V51Models.PositionAction action:actions){
            if(action==null||action.position<1||action.position>10||action.n4>0
                    ||!DecisionGateEngine.b12K3(action.n1,action.n2,action.n3)
                    ||!Double.isFinite(action.rawActionP)||action.rawActionP<0||action.rawActionP>1)
                throw new IllegalArgumentException("F03_RANK_ACTION_IDENTITY");
            int key=(action.position<<10)|AppConfig.buildK3Mask(action.n1,action.n2,action.n3);
            if(!actionKeys.add(key))throw new IllegalArgumentException("F03_RANK_DUPLICATE_ACTION");
        }
        double[] score = new double[actions.length];
        boolean proven =
                ModelBundleRuntimeR932.ranker().PROVEN
                        && ModelBundleRuntimeR932.ranker().RANK_ORDER_PASSED
                        && ModelBundleRuntimeR932.ranker().STABLE_FOLDS >= 4;
        for (int i = 0; i < actions.length; i++)
            {
            if(actions[i]==null || meta[i]==null || meta[i].length!=96 || !Double.isFinite(actions[i].rawActionP))
                throw new IllegalArgumentException("R932_RANK_INPUT_INVALID");
            for(double value:meta[i])if(!Double.isFinite(value))throw new IllegalArgumentException("R932_RANK_NONFINITE_INPUT");
            score[i] = (proven || ModelBundleRuntimeR932.ranker().TRAINED_MODEL!=null) ? artifactScore(meta[i],actions[i].rawActionP) : researchScore(actions[i], meta[i]);
            if(!Double.isFinite(score[i]))throw new IllegalArgumentException("R932_RANK_NONFINITE_SCORE");
        }
        int[] order = order(score,actions);
        V51Models.PositionAction[] out = new V51Models.PositionAction[actions.length];
        for (int r = 0; r < order.length; r++) {
            int i = order[r];
            V51Models.PositionAction a = actions[i];
            double margin = r == 0 && order.length > 1 ? score[i] - score[order[1]] : 0.0d;
            out[i] =
                    new V51Models.PositionAction(
                            a.position,
                            a.n1,
                            a.n2,
                            a.n3,
                            a.n4,
                            a.rawActionP,
                            score[i],
                            margin,
                            a.thirdMargin,
                            a.entropy,
                            a.stability,
                            meta[i],
                            a.fingerprint,
                            r + 1,
                            V51ChecksumEngine.sha256(
                                    a.checksum
                                            + '|'
                                            + score[i]
                                            + '|'
                                            + (r + 1)
                                            + '|'
                                            + (proven ? "PROVEN" : "RESEARCH")));
        }
        V51Models.PositionAction one = out[order[0]];
        double margin = order.length > 1 ? score[order[0]] - score[order[1]] : 0;
        return new RankResult(
                out,
                one,
                proven
                        ? RankStatus.PROVEN_PAIRWISE_LISTWISE_ARTIFACT
                        : RankStatus.UNTRAINED_RESEARCH_ORDER,
                proven,
                margin);
    }

    public V51Models.PositionAction rankOne(V51Models.PositionAction[] ranked) {
        if (ranked == null) return null;
        for (int i = 0; i < ranked.length; i++)
            if (ranked[i] != null && ranked[i].rank == 1) return ranked[i];
        return null;
    }

    private static double artifactScore(double[] x,double rawP) {
        if(ModelBundleRuntimeR932.ranker().TRAINED_MODEL!=null)return ModelBundleRuntimeR932.ranker().TRAINED_MODEL.score(x,rawP);
        return RankerMathR925.score(x,ModelBundleRuntimeR932.ranker().MEAN,
                ModelBundleRuntimeR932.ranker().SCALE,ModelBundleRuntimeR932.ranker().WEIGHT,ModelBundleRuntimeR932.ranker().BIAS);
    }

    private static double researchScore(V51Models.PositionAction a, double[] x) {
        double z =
                1.80d * a.rawActionP
                        + 0.28d * a.stability
                        - 0.14d * a.entropy
                        + 0.10d * V51Math.clamp(a.thirdMargin * 12.0d, -1, 1);
        for (int i = 0; i < x.length; i++) {
            double c = ((i % 7) - 3) * 0.0025d;
            z += c * V51Math.clamp(x[i], -3, 3);
        }
        return V51Math.finite(z);
    }

    private static int[] order(double[] s,V51Models.PositionAction[] actions) {
        int[] x = new int[s.length];
        for (int i = 0; i < x.length; i++) x[i] = i;
        /* Frozen training/phone contract: score descending, raw probability descending, ordinal ascending. */
        int[] work = new int[x.length];
        mergeSort(x, work, 0, x.length, s, actions);
        return x;
    }

    private static void mergeSort(int[] values, int[] work, int from, int to, double[] score,V51Models.PositionAction[] actions) {
        if (to - from <= 1) return;
        int middle = from + (to - from) / 2;
        mergeSort(values, work, from, middle, score, actions);
        mergeSort(values, work, middle, to, score, actions);
        int left = from, right = middle, out = from;
        while (left < middle && right < to) {
            int a = values[left], b = values[right];
            if (before(a, b, score, actions)) work[out++] = values[left++];
            else work[out++] = values[right++];
        }
        while (left < middle) work[out++] = values[left++];
        while (right < to) work[out++] = values[right++];
        System.arraycopy(work, from, values, from, to - from);
    }

    private static boolean before(int left, int right, double[] score,V51Models.PositionAction[] actions) {
        return score[left] > score[right]
                || (score[left] == score[right] && (actions[left].rawActionP>actions[right].rawActionP
                || (actions[left].rawActionP==actions[right].rawActionP && left < right)));
    }
}
