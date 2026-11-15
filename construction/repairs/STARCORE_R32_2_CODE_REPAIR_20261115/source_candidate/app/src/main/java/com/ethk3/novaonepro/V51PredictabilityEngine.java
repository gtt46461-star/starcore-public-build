package com.ethk3.novaonepro;

/**
 * Predicts whether the selected action is trustworthy, not which number will occur. When the
 * trained artifact is unavailable this engine produces a conservative engineering score for
 * CONTROLLED_PROBE only; it never changes MODEL_PROVEN.
 */
public final class V51PredictabilityEngine {
    public V51Models.PredictabilityEvidence evaluate(
            V51Models.PositionAction action,
            V51EffectiveConsensusEngine.Consensus consensus,
            V51Models.StateTrajectorySnapshot state,
            V51RankStabilityEngine.Result stability,
            double relationReliability,
            double motifReliability,
            double teacherReliability,
            double falsifier) {
        return evaluate(action,consensus,state,stability,relationReliability,motifReliability,
                teacherReliability,falsifier,SelectionEvidenceContractR929.installedPolicy().hash);
    }
    public V51Models.PredictabilityEvidence evaluate(
            V51Models.PositionAction action,V51EffectiveConsensusEngine.Consensus consensus,
            V51Models.StateTrajectorySnapshot state,V51RankStabilityEngine.Result stability,
            double relationReliability,double motifReliability,double teacherReliability,double falsifier,
            String selectionPolicyHash) {
        double[] x =
                new double[] {
                    state.confidence,
                    1.0d - state.changePointRisk,
                    consensus.effective,
                    consensus.signalStrength,
                    stability.retention,
                    stability.thirdRobustness,
                    margin(action.rankMargin, 0.12d),
                    margin(action.thirdMargin, 0.08d),
                    clamp01(relationReliability),
                    clamp01(motifReliability),
                    clamp01(teacherReliability),
                    1.0d - clamp01(falsifier)
                };
        String missing="";for(int i=0;i<x.length;i++)if(!Double.isFinite(x[i])){missing="MISSING_PREDICTABILITY_INPUT:"+i;break;}
        double p;
        ModelBundleParametersR932.Predictability artifact=ModelBundleRuntimeR932.predictability();
        if(artifact.TRAINED_MODEL!=null&&!artifact.SELECTION_POLICY_SHA256.equals(selectionPolicyHash))missing="PREDICTABILITY_SELECTION_POLICY_CHANGED";
        if(missing.length()>0)p=Double.NaN;
        else if(artifact.TRAINED_MODEL!=null)p=artifact.TRAINED_MODEL.predict(x);
        else if (ModelBundleRuntimeR932.predictability().PROVEN) {
            double z = ModelBundleRuntimeR932.predictability().BIAS;
            for (int i = 0; i < x.length; i++) z += x[i] * ModelBundleRuntimeR932.predictability().WEIGHT[i];
            p = V51Math.sigmoid(z);
        } else {
            p =
                    0.05d
                            + 0.11d * x[0]
                            + 0.10d * x[1]
                            + 0.12d * x[2]
                            + 0.06d * x[3]
                            + 0.12d * x[4]
                            + 0.10d * x[5]
                            + 0.08d * x[6]
                            + 0.07d * x[7]
                            + 0.04d * x[8]
                            + 0.04d * x[9]
                            + 0.04d * x[10]
                            + 0.07d * x[11];
            p = clamp01(p);
        }
        boolean qualified =
                ModelBundleRuntimeR932.predictability().PROVEN
                        && p >= ModelBundleRuntimeR932.predictability().QUALIFIED_THRESHOLD
                        && stability.stable;
        return new V51Models.PredictabilityEvidence(
                p,
                stability.retention,
                state.changePointRisk,
                falsifier,
                qualified,
                V51ChecksumEngine.sha256(
                        action.checksum
                                + '|'
                                + p
                                + '|'
                                + qualified
                                + '|'
                                + (ModelBundleRuntimeR932.predictability().PROVEN
                                        ? "PROVEN"
                                        : "ENGINEERING_PROBE")),x,missing);
    }

    public static double expertReliability(V51Models.ExpertEvidence[] experts,String id,int position) {
        if(experts==null||position<1||position>10)return Double.NaN;
        for(V51Models.ExpertEvidence e:experts)if(e!=null&&id.equals(e.expertId)) {
            if(e.trust==null||e.uncertainty==null||e.trust.length<position||e.uncertainty.length<position)return Double.NaN;
            return clamp01(e.trust[position-1])*clamp01(1-e.uncertainty[position-1]);
        }return Double.NaN;
    }
    private static double clamp01(double x) {
        return Double.isFinite(x)?V51Math.clamp(x, 0.0d, 1.0d):Double.NaN;
    }

    private static double margin(double x, double scale) {
        return clamp01(x / Math.max(1.0e-9d, scale));
    }
}
