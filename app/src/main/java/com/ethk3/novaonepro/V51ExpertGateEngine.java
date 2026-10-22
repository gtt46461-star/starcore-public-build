package com.ethk3.novaonepro;

/**
 * B09 state-conditioned expert router. A non-proven artifact is explicitly RESEARCH_ROUTING and can
 * never be interpreted as a proven production gate.
 */
public final class V51ExpertGateEngine {
    public static final String V57_B09_OWNER = "EXPERT_GATE_ONLY";

    public enum GateStatus {
        PROVEN_ARTIFACT,
        UNTRAINED_RESEARCH_ROUTING,
        NO_EXPERT_EVIDENCE
    }

    public static final class Result {
        public final double[][] weight;
        public final V51ExpertHealthRegistry.Snapshot[] health;
        public final GateStatus status;
        public final boolean artifactProven;
        public final double[] effectiveExperts;
        /** Diagnostic-only; excluded from canonical() and every routing calculation. */
        public final boolean[] fallbackUsed;
        public final int[] fallbackExpertIndex;
        public final double[] sumBeforeFallback;
        public final String checksum;

        Result(
                double[][] w,
                V51ExpertHealthRegistry.Snapshot[] h,
                GateStatus s,
                boolean proven,
                double[] neff,
                boolean[] fallbackUsed,
                int[] fallbackExpertIndex,
                double[] sumBeforeFallback) {
            weight = V51Math.copy(w);
            health = h;
            status = s;
            artifactProven = proven;
            effectiveExperts = V51Math.copy(neff);
            this.fallbackUsed = copy(fallbackUsed);
            this.fallbackExpertIndex = copy(fallbackExpertIndex);
            this.sumBeforeFallback = V51Math.copy(sumBeforeFallback);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            StringBuilder b = new StringBuilder(status.name()).append('|').append(artifactProven);
            for (int p = 0; p < weight.length; p++) {
                b.append('|').append(effectiveExperts[p]);
                for (int e = 0; e < weight[p].length; e++) b.append(',').append(weight[p][e]);
            }
            return b.toString();
        }
    }

    private final V51ExpertHealthRegistry healthRegistry = new V51ExpertHealthRegistry();

    public double[][] weights(
            V51Models.ExpertEvidence[] experts,
            V51Models.StateTrajectorySnapshot[] states,
            V51ExpertRegretEngine regret) {
        return evaluate(experts, states, regret).weight;
    }

    public Result evaluate(
            V51Models.ExpertEvidence[] experts,
            V51Models.StateTrajectorySnapshot[] states,
            V51ExpertRegretEngine regret) {
        if (experts == null || experts.length == 0)
            return new Result(
                    new double[10][0],
                    new V51ExpertHealthRegistry.Snapshot[0],
                    GateStatus.NO_EXPERT_EVIDENCE,
                    false,
                    new double[10],
                    new boolean[10],
                    fillMinusOne(new int[10]),
                    new double[10]);
        V51ExpertHealthRegistry.Snapshot[] hs=new V51ExpertHealthRegistry.Snapshot[experts.length];
        long now=System.currentTimeMillis();
        for(int e=0;e<experts.length;e++)hs[e]=healthRegistry.assess(experts[e].expertId,regret==null?null:regret.snapshot(experts[e].expertId),now);
        return evaluateFrozen(experts,states,hs,new boolean[experts.length]);
    }

    /** Replay the exact router with frozen health; an ablation masks a channel after inference. */
    Result evaluateFrozen(V51Models.ExpertEvidence[] experts,V51Models.StateTrajectorySnapshot[] states,
            V51ExpertHealthRegistry.Snapshot[] hs,boolean[] disabled) {
        if(experts==null||experts.length!=9||hs==null||hs.length!=9||disabled==null||disabled.length!=9)throw new IllegalArgumentException("R932_FROZEN_GATE_INPUT");
        double[][] out=new double[10][experts.length];
        boolean[] fallbackUsed=new boolean[10];int[] fallbackExpertIndex=fillMinusOne(new int[10]);double[] sumBeforeFallback=new double[10];
        for (int p = 0; p < 10; p++) {
            int proto =
                    prototypeIndex(
                            states != null && p < states.length && states[p] != null
                                    ? states[p].prototypeId
                                    : "");
            double sum = 0;
            for (int e = 0; e < experts.length; e++) {
                V51Models.ExpertEvidence x = experts[e];
                double artifactBase = artifactBase(proto, e, experts.length);
                double trust =
                        x != null && x.trust != null && p < x.trust.length
                                ? clamp01(x.trust[p])
                                : 0.0d;
                double uncertainty =
                        x != null && x.uncertainty != null && p < x.uncertainty.length
                                ? clamp01(x.uncertainty[p])
                                : 1.0d;
                double signal =
                        x != null && x.signalStrength != null && p < x.signalStrength.length
                                ? clamp01(x.signalStrength[p])
                                : 0.0d;
                double evidenceQuality = trust * (1.0d - uncertainty) * (0.40d + 0.60d * signal);
                double health = hs[e].weightMultiplier;
                double w = artifactBase * Math.max(0.0d, evidenceQuality) * health;
                if (!ModelBundleRuntimeR932.gate().PROVEN) {
                    double neutral = 1.0d / Math.max(1, experts.length);
                    w = neutral * (0.20d + 0.80d * evidenceQuality) * health;
                }
                out[p][e] = disabled[e]?0:Math.max(0.0d, w);
                sum += out[p][e];
            }
            sumBeforeFallback[p] = sum;
            if (sum <= 1.0e-15d) {
                fallbackUsed[p] = true;
                int fallback = bestNonFrozen(hs,disabled);
                fallbackExpertIndex[p] = fallback;
                if (fallback >= 0) out[p][fallback] = 1.0d;
                else {int enabled=0;for(boolean off:disabled)if(!off)enabled++;if(enabled==0)throw new IllegalStateException("R932_GATE_NO_ENABLED_EXPERT");for(int e=0;e<experts.length;e++)out[p][e]=disabled[e]?0:1.0d/enabled;}
            } else for (int e = 0; e < experts.length; e++) out[p][e] /= sum;
        }
        if(ModelBundleRuntimeR932.gate().TRAINED_MODEL!=null) {
            for(int e=0;e<hs.length;e++)if(!disabled[e]&&hs[e].state==V51ExpertHealthRegistry.HealthState.HARD_FREEZE)
                throw new IllegalStateException("R932_TRAINED_GATE_EXPERT_UNAVAILABLE");
            double[] trained=ModelBundleRuntimeR932.gate().TRAINED_MODEL.weights(UpstreamModelMathR932.stateValues(states),UpstreamModelMathR932.expertValues(experts));
            double mass=0;boolean masked=false;for(int e=0;e<9;e++){if(disabled[e]){trained[e]=0;masked=true;}mass+=trained[e];}
            if(masked){if(mass<=0)throw new IllegalStateException("R932_TRAINED_GATE_NO_ENABLED_MASS");for(int e=0;e<9;e++)trained[e]/=mass;}
            for(int p=0;p<10;p++){System.arraycopy(trained,0,out[p],0,9);fallbackUsed[p]=false;fallbackExpertIndex[p]=-1;sumBeforeFallback[p]=1;}
        }
        double[] neff = new double[10];
        for (int p = 0; p < 10; p++) neff[p] = effectiveExperts(out[p]);
        return new Result(
                out,
                hs,
                ModelBundleRuntimeR932.gate().PROVEN
                        ? GateStatus.PROVEN_ARTIFACT
                        : GateStatus.UNTRAINED_RESEARCH_ROUTING,
                ModelBundleRuntimeR932.gate().PROVEN,
                neff,
                fallbackUsed,
                fallbackExpertIndex,
                sumBeforeFallback);
    }

    private static double artifactBase(int proto, int e, int expertCount) {
        if (ModelBundleRuntimeR932.gate().PROTOTYPE_WEIGHT != null
                && proto >= 0
                && proto < ModelBundleRuntimeR932.gate().PROTOTYPE_WEIGHT.length
                && ModelBundleRuntimeR932.gate().PROTOTYPE_WEIGHT[proto] != null
                && e < ModelBundleRuntimeR932.gate().PROTOTYPE_WEIGHT[proto].length)
            return Math.max(0.0d, ModelBundleRuntimeR932.gate().PROTOTYPE_WEIGHT[proto][e]);
        return 1.0d / Math.max(1, expertCount);
    }

    private static int bestNonFrozen(V51ExpertHealthRegistry.Snapshot[] h,boolean[] disabled) {
        int best = -1;
        double score = -1;
        for (int i = 0; i < h.length; i++) {
            if (disabled[i]||h[i].state == V51ExpertHealthRegistry.HealthState.HARD_FREEZE) continue;
            double q = h[i].weightMultiplier;
            if (q > score) {
                score = q;
                best = i;
            }
        }
        return best;
    }

    private static int prototypeIndex(String id) {
        for (int i = 0; i < V51StateModelArtifact.IDS.length; i++)
            if (V51StateModelArtifact.IDS[i].equals(id)) return i;
        return 0;
    }

    private static double effectiveExperts(double[] w) {
        double s = 0, s2 = 0;
        for (int i = 0; i < w.length; i++) {
            double x = Math.max(0.0d, w[i]);
            s += x;
            s2 += x * x;
        }
        return s2 <= 1e-15 ? 0.0d : (s * s / s2);
    }

    private static double clamp01(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 0.0d;
        return x < 0 ? 0 : (x > 1 ? 1 : x);
    }

    private static boolean[] copy(boolean[] source) {
        if (source == null) return new boolean[0];
        boolean[] out = new boolean[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static int[] copy(int[] source) {
        if (source == null) return new int[0];
        int[] out = new int[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static int[] fillMinusOne(int[] values) {
        for (int i = 0; i < values.length; i++) values[i] = -1;
        return values;
    }
}
