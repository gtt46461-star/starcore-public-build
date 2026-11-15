package com.ethk3.novaonepro;

/** Separates agreement from actual signal strength and reliability. */
public final class V51EffectiveConsensusEngine {
    public static final class Consensus {
        public final double distributionAgreement,
                top3Overlap,
                rankAgreement,
                directionalAgreement,
                signalStrength,
                historicalReliability,
                recentReliability,
                stateReliability,
                effective;

        Consensus(
                double a,
                double b,
                double c,
                double d,
                double e,
                double f,
                double g,
                double h,
                double i) {
            distributionAgreement = a;
            top3Overlap = b;
            rankAgreement = c;
            directionalAgreement = d;
            signalStrength = e;
            historicalReliability = f;
            recentReliability = g;
            stateReliability = h;
            effective = i;
        }
    }

    public Consensus[] evaluate(
            V51Models.ExpertEvidence[] experts,
            V51Models.StateTrajectorySnapshot[] states,
            V51ExpertRegretEngine regret) {
        double[] reliability=new double[10];
        double average=0;for(V51Models.ExpertEvidence expert:experts)average+=regret==null?.5:regret.reliability(expert.expertId);average/=Math.max(1,experts.length);
        java.util.Arrays.fill(reliability,average);
        return evaluateFrozen(experts,states,reliability,new boolean[experts.length]);
    }
    Consensus[] evaluateFrozen(V51Models.ExpertEvidence[] experts,V51Models.StateTrajectorySnapshot[] states,double[] frozenReliability,boolean[] disabled) {
        if(frozenReliability==null||frozenReliability.length!=10||disabled==null||disabled.length!=experts.length)throw new IllegalArgumentException("R932_FROZEN_CONSENSUS_INPUT");
        Consensus[] out = new Consensus[10];
        for (int p = 0; p < 10; p++) {
            double js = 0, overlap = 0, rank = 0, direction = 0, signal = 0, reliability = 0;
            int pairs = 0,enabled=0;
            int[][] top = new int[experts.length][];
            double[][] prob = new double[experts.length][10];
            for (int e = 0; e < experts.length; e++) {
                if(disabled[e])continue;enabled++;
                prob[e] = softmax(experts[e].rawScore[p]);
                top[e] = top3(prob[e]);
                signal += experts[e].signalStrength[p];
            }
            for (int a = 0; a < experts.length; a++)
                for (int b = a + 1; b < experts.length; b++) {
                    if(disabled[a]||disabled[b])continue;
                    js += jensenShannon(prob[a], prob[b]);
                    overlap += overlap(top[a], top[b]) / 3.0d;
                    rank += spearman(prob[a], prob[b]);
                    direction += direction(prob[a], prob[b]);
                    pairs++;
                }
            double dist = 1.0d - (pairs == 0 ? 1.0d : js / pairs);
            double ov = pairs == 0 ? 0 : overlap / pairs;
            double ra = pairs == 0 ? 0 : rank / pairs;
            double di = pairs == 0 ? 0 : direction / pairs;
            signal /= Math.max(1, enabled);
            reliability = frozenReliability[p];
            double state = states[p].confidence;
            double effective =
                    V51Math.clamp(
                            dist
                                    * ov
                                    * Math.max(0, ra)
                                    * Math.max(0, di)
                                    * signal
                                    * reliability
                                    * state,
                            0,
                            1);
            out[p] =
                    new Consensus(
                            V51Math.clamp(dist, 0, 1),
                            V51Math.clamp(ov, 0, 1),
                            V51Math.clamp((ra + 1) / 2, 0, 1),
                            V51Math.clamp((di + 1) / 2, 0, 1),
                            V51Math.clamp(signal, 0, 1),
                            V51Math.clamp(reliability, 0, 1),
                            V51Math.clamp(reliability, 0, 1),
                            state,
                            effective);
        }
        return out;
    }

    private static double[] softmax(double[] x) {
        double m = -Double.MAX_VALUE;
        for (int i = 0; i < x.length; i++) m = Math.max(m, x[i]);
        double[] p = new double[x.length];
        double s = 0;
        for (int i = 0; i < x.length; i++) {
            p[i] = Math.exp(Math.max(-50, x[i] - m));
            s += p[i];
        }
        for (int i = 0; i < x.length; i++) p[i] /= s;
        return p;
    }

    private static int[] top3(double[] p) {
        int[] idx = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < idx.length; i++)
            for (int j = i + 1; j < idx.length; j++)
                if (p[idx[j]] > p[idx[i]]) {
                    int t = idx[i];
                    idx[i] = idx[j];
                    idx[j] = t;
                }
        return new int[] {idx[0], idx[1], idx[2]};
    }

    private static int overlap(int[] a, int[] b) {
        int c = 0;
        for (int i = 0; i < a.length; i++) for (int j = 0; j < b.length; j++) if (a[i] == b[j]) c++;
        return c;
    }

    private static double jensenShannon(double[] a, double[] b) {
        double d = 0;
        for (int i = 0; i < a.length; i++) {
            double m = (a[i] + b[i]) * 0.5d;
            d += 0.5d * a[i] * Math.log(a[i] / m) + 0.5d * b[i] * Math.log(b[i] / m);
        }
        return V51Math.clamp(d / Math.log(2.0d), 0, 1);
    }

    private static double spearman(double[] a, double[] b) {
        int n = a.length;
        int[] ra = rank(a), rb = rank(b);
        double d = 0;
        for (int i = 0; i < n; i++) {
            double x = ra[i] - rb[i];
            d += x * x;
        }
        return 1.0d - 6.0d * d / (n * (n * n - 1.0d));
    }

    private static int[] rank(double[] x) {
        int[] r = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            int k = 0;
            for (int j = 0; j < x.length; j++) if (x[j] > x[i] || (x[j] == x[i] && j < i)) k++;
            r[i] = k;
        }
        return r;
    }

    private static double direction(double[] a, double[] b) {
        double ma = V51Math.mean(a), mb = V51Math.mean(b), sum = 0;
        for (int i = 0; i < a.length; i++) sum += Math.signum(a[i] - ma) * Math.signum(b[i] - mb);
        return sum / a.length;
    }
}
