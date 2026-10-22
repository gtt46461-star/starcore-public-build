package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Sole V50 NumberP owner. It uses a reliability-weighted log opinion pool; state modules and
 * experts cannot write the final distribution directly.
 */
public final class V50MixtureOfExpertsEngine {
    public static final class FusionResult {
        public final V50Models.ExpertEvidence[] experts;
        public final V50ExpertRegretEngine.Reliability[][] reliability;
        public final V50Models.EffectiveConsensus[] consensus;
        public final double[][] numberProbability;
        public final String checksum;

        FusionResult(
                V50Models.ExpertEvidence[] experts,
                V50ExpertRegretEngine.Reliability[][] reliability,
                V50Models.EffectiveConsensus[] consensus,
                double[][] numberProbability,
                String checksum) {
            this.experts = experts;
            this.reliability = reliability;
            this.consensus = consensus;
            this.numberProbability = numberProbability;
            this.checksum = checksum;
        }
    }

    private final V50NumberExpert[] core =
            new V50NumberExpert[] {
                new HierarchicalPriorNumberExpert(),
                new MultiScaleTrendNumberExpert(),
                new ContextTreeNumberExpert(),
                new StateDurationNumberExpert(),
                new MotifNumberExpert(),
                new RelationGraphNumberExpert(),
                new TeacherPosteriorNumberExpert(),
                new OfflineResidualNumberExpert(),
                new OnlineResidualNumberExpert()
            };
    private final V50ExpertRegretEngine regret = new V50ExpertRegretEngine();
    private final V50EffectiveConsensusEngine consensusEngine = new V50EffectiveConsensusEngine();

    public FusionResult fuse(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        ArrayList<V50Models.ExpertEvidence> evidence = new ArrayList<V50Models.ExpertEvidence>();
        for (int i = 0; i < core.length; i++) {
            V50Models.ExpertEvidence value = core[i].evaluate(features, states, history, store);
            if (valid(value)) evidence.add(value);
        }
        V50Models.ExpertEvidence[] experts =
                evidence.toArray(new V50Models.ExpertEvidence[evidence.size()]);
        V50ExpertRegretEngine.Reliability[][] reliability =
                regret.load(store == null ? null : store.readable(), experts, states);
        V50Models.EffectiveConsensus[] consensus = consensusEngine.evaluate(experts, reliability);
        double[][] probability = new double[10][10];
        for (int p = 0; p < 10; p++) {
            double weightSum = 0.0d;
            double[] logPool = new double[10];
            for (int e = 0; e < experts.length; e++) {
                V50Models.ExpertEvidence expert = experts[e];
                V50ExpertRegretEngine.Reliability profile = reliability[e][p];
                double trust = p < expert.trust.length ? expert.trust[p] : 0.0d;
                double uncertainty = p < expert.uncertainty.length ? expert.uncertainty[p] : 1.0d;
                double signal = p < expert.signalStrength.length ? expert.signalStrength[p] : 0.0d;
                double reliabilityScore =
                        (profile.historical + profile.recent + profile.state) / 3.0d;
                double weight =
                        trust
                                * profile.weight
                                * reliabilityScore
                                * (1.0d - 0.55d * uncertainty)
                                * signal;
                if (weight <= 1.0e-9d) continue;
                weightSum += weight;
                for (int n = 0; n < 10; n++) {
                    logPool[n] +=
                            weight * Math.log(Math.max(1.0e-12d, expert.numberProbability[p][n]));
                }
            }
            if (weightSum <= 1.0e-9d) {
                for (int n = 0; n < 10; n++) probability[p][n] = 0.1d;
            } else {
                double max = -Double.MAX_VALUE;
                for (int n = 0; n < 10; n++) {
                    logPool[n] /= weightSum;
                    max = Math.max(max, logPool[n]);
                }
                for (int n = 0; n < 10; n++) {
                    probability[p][n] =
                            Math.exp(Math.max(-40.0d, Math.min(40.0d, logPool[n] - max)));
                }
                FeatureContractRegistry.normalize10(probability[p]);
            }
        }
        String checksum =
                V50ChecksumEngine.hashMatrix(
                        V50BuildManifest.NUMBER_PROBABILITY_OWNER, probability);
        return new FusionResult(experts, reliability, consensus, probability, checksum);
    }

    private static boolean valid(V50Models.ExpertEvidence evidence) {
        if (evidence == null
                || evidence.numberProbability == null
                || evidence.numberProbability.length != 10) return false;
        for (int p = 0; p < 10; p++) {
            if (!V50PredictionContract.normalized10(evidence.numberProbability[p], 1.0e-8d))
                return false;
        }
        return true;
    }
}
