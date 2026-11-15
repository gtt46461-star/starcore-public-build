package com.ethk3.novaonepro;

/** B10 full formal K3 action builder: 10 positions x C(10,3)=1200 actions. */
public final class V51PositionActionBuilder {
    public static final String V57_B10_OWNER = "FORMAL_K3_ACTION_BUILDER";
    public static final int POSITIONS = 10,
            NUMBERS = 10,
            K = 3,
            COMBINATIONS_PER_POSITION = 120,
            TOTAL_ACTIONS = 1200;

    public V51Models.PositionAction[] build(
            V51Models.NumberProbabilityMatrix matrix,
            V51Models.StateTrajectorySnapshot[] states,
            V51EffectiveConsensusEngine.Consensus[] consensus) {
        if (matrix == null || matrix.probability == null || matrix.probability.length != 10)
            throw new IllegalArgumentException("B10_MATRIX_INVALID");
        if (states == null || states.length != 10 || consensus == null || consensus.length != 10)
            throw new IllegalArgumentException("B10_CONTEXT_INVALID");
        V51Models.PositionAction[] out = new V51Models.PositionAction[TOTAL_ACTIONS];
        int idx = 0;
        for (int p = 0; p < POSITIONS; p++) {
            double[] q = matrix.probability[p];
            double rowEntropy = V51Math.entropy(q);
            for (int a = 1; a <= 8; a++)
                for (int b = a + 1; b <= 9; b++)
                    for (int c = b + 1; c <= 10; c++) {
                        double raw = q[a - 1] + q[b - 1] + q[c - 1];
                        int n4 = bestExcluded(q, a, b, c);
                        double minSelected = Math.min(q[a - 1], Math.min(q[b - 1], q[c - 1]));
                        double bestExcluded = q[n4 - 1];
                        double boundary = minSelected - bestExcluded;
                        double state = states[p] == null ? 0.0d : states[p].confidence;
                        double con = consensus[p] == null ? 0.0d : consensus[p].effective;
                        double stability =
                                V51Math.clamp(
                                        0.45d * state
                                                + 0.35d * con
                                                + 0.20d
                                                        * V51Math.clamp(
                                                                0.5d + boundary * 12.0d, 0, 1),
                                        0,
                                        1);
                        String identity = (p + 1) + "|" + a + "|" + b + "|" + c;
                        String fp =
                                V51ChecksumEngine.sha256(
                                        "B10_ACTION|"
                                                + identity
                                                + '|'
                                                + matrix.checksum
                                                + '|'
                                                + states[p].prototypeId);
                        out[idx++] =
                                new V51Models.PositionAction(
                                        p + 1,
                                        a,
                                        b,
                                        c,
                                        n4,
                                        raw,
                                        0.0d,
                                        0.0d,
                                        boundary,
                                        rowEntropy,
                                        stability,
                                        new double[0],
                                        fp,
                                        0,
                                        V51ChecksumEngine.sha256(fp + '|' + raw + '|' + boundary));
                    }
        }
        if (idx != TOTAL_ACTIONS) throw new IllegalStateException("B10_ACTION_ENUMERATION_" + idx);
        K3ActionSpaceEngine.validateOrThrow(out);
        return out;
    }

    private static int bestExcluded(double[] q, int a, int b, int c) {
        int best = -1;
        double v = -Double.MAX_VALUE;
        for (int n = 1; n <= 10; n++) {
            if (n == a || n == b || n == c) continue;
            double x = q[n - 1];
            if (x > v || (x == v && (best < 0 || n < best))) {
                v = x;
                best = n;
            }
        }
        return best;
    }
}
