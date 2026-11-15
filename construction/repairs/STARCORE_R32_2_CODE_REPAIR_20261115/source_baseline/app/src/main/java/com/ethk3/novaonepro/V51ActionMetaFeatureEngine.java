package com.ethk3.novaonepro;

/**
 * B10/R929 dynamic 96-dimensional meta features for every one of the 1200 formal actions.
 * The tensor is now bound to {@link FeatureDagContractR929}; shape equality alone is not a
 * compatibility contract.
 */
public final class V51ActionMetaFeatureEngine {
    public static final int FEATURE_COUNT = 96;
    public static final String V57_B10_OWNER = "ACTION_META_FEATURES_1200";
    public static final String INPUT_STAGE = "ACTION_META_1200";

    private static final FeatureDagContractR929.Manifest MANIFEST =
            FeatureDagContractR929.actionMeta96Manifest();

    static {
        MANIFEST.requireCompatible(FeatureDagContractR929.Stage.ACTION_META_1200, FEATURE_COUNT, MANIFEST.hash);
    }

    public static String featureManifestHash() { return MANIFEST.hash; }
    public static String featureManifestCanonical() { return MANIFEST.canonicalWithoutHash(); }

    public double[][] build(
            V51Models.PositionAction[] actions,
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51Models.ExpertEvidence[] experts,
            double[][] gate,
            V51EffectiveConsensusEngine.Consensus[] consensus) {
        if (actions == null || actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            throw new IllegalArgumentException("B10_META_ACTION_COUNT");
        if (features == null || states == null || states.length != 10 || experts == null
                || consensus == null || consensus.length != 10)
            throw new IllegalArgumentException("R929_META_UPSTREAM_REQUIRED");
        if (experts.length != FeatureDagContractR929.EXPERT_COUNT)
            throw new IllegalArgumentException("R929_EXPERT_COUNT|expected="
                    + FeatureDagContractR929.EXPERT_COUNT + "|actual=" + experts.length);

        double[][] out = new double[actions.length][FEATURE_COUNT];
        double mean = 0, sd = 0;
        for (int i = 0; i < actions.length; i++) mean += finite(actions[i].rawActionP);
        mean /= actions.length;
        for (int i = 0; i < actions.length; i++) {
            double d = finite(actions[i].rawActionP) - mean;
            sd += d * d;
        }
        sd = Math.sqrt(sd / actions.length + 1e-12);
        for (int i = 0; i < actions.length; i++) {
            V51Models.PositionAction a = actions[i];
            int p = a.position - 1;
            if (p < 0 || p >= 10) throw new IllegalArgumentException("R929_POSITION_RANGE:" + a.position);
            double[] x = out[i];
            int k = 0;
            x[k++] = finite(a.rawActionP);
            x[k++] = finite(a.thirdMargin);
            x[k++] = finite(a.entropy);
            x[k++] = finite(a.stability);
            x[k++] = (double) p / 9.0d;
            x[k++] = finite(a.rawActionP) - mean;
            x[k++] = (finite(a.rawActionP) - mean) / sd;
            x[k++] = a.n1 / 10.0d;
            x[k++] = a.n2 / 10.0d;
            x[k++] = a.n3 / 10.0d;
            // Legacy audit slot only.  It must never be interpreted as dynamic K=4.
            x[k++] = a.n4 / 10.0d;
            V51Models.StateTrajectorySnapshot st = states[p];
            if (st == null) throw new IllegalArgumentException("R929_STATE_MISSING:" + p);
            x[k++] = finite(st.posterior);
            x[k++] = finite(st.remainingLife) / 20.0d;
            x[k++] = finite(st.changePointRisk);
            x[k++] = finite(st.confidence);
            V51EffectiveConsensusEngine.Consensus co = consensus[p];
            if (co == null) throw new IllegalArgumentException("R929_CONSENSUS_MISSING:" + p);
            x[k++] = finite(co.distributionAgreement);
            x[k++] = finite(co.top3Overlap);
            x[k++] = finite(co.rankAgreement);
            x[k++] = finite(co.directionalAgreement);
            x[k++] = finite(co.signalStrength);
            x[k++] = finite(co.effective);
            for (int e = 0; e < experts.length; e++) {
                V51Models.ExpertEvidence ev = experts[e];
                if (ev == null) throw new IllegalArgumentException("R929_EXPERT_MISSING:" + e);
                x[k++] = safe(ev.trust, p);
                x[k++] = safe(ev.uncertainty, p);
                x[k++] = safe(ev.signalStrength, p);
                x[k++] = gate != null && p < gate.length && gate[p] != null && e < gate[p].length
                        ? finite(gate[p][e]) : 0.0d;
                x[k++] = meanRaw(ev, p, a.n1, a.n2, a.n3);
            }
            double[] gf = features.globalFeatures;
            for (int g = 0; gf != null && g < gf.length && k < FEATURE_COUNT; g++)
                x[k++] = finite(gf[g]);
            while (k < FEATURE_COUNT) {
                int source = (k * 17 + 13) % Math.max(1, k);
                double base = x[source];
                double interaction = (finite(a.rawActionP) - 0.30d) * (0.5d + finite(a.stability));
                x[k++] = V51Math.clamp(base * interaction, -3, 3);
            }
            for (int j = 0; j < FEATURE_COUNT; j++) {
                if (Double.isNaN(x[j]) || Double.isInfinite(x[j]))
                    throw new IllegalStateException("R929_META_NONFINITE:" + i + ':' + j);
            }
        }
        return out;
    }

    private static double safe(double[] x, int i) {
        return x != null && i >= 0 && i < x.length ? finite(x[i]) : 0.0d;
    }

    private static double finite(double x) {
        return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x;
    }

    private static double meanRaw(V51Models.ExpertEvidence e, int p, int a, int b, int c) {
        if (e == null || e.rawScore == null || p >= e.rawScore.length || e.rawScore[p] == null
                || e.rawScore[p].length < 10) return 0;
        double[] r = e.rawScore[p];
        return (finite(r[a - 1]) + finite(r[b - 1]) + finite(r[c - 1])) / 3.0d;
    }
}
