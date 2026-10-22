package com.ethk3.novaonepro;

/** Effective consensus discounts agreement when distributions carry no signal. */
public final class V50EffectiveConsensusEngine {
    public V50Models.EffectiveConsensus[] evaluate(
            V50Models.ExpertEvidence[] experts, V50ExpertRegretEngine.Reliability[][] reliability) {
        V50Models.EffectiveConsensus[] out = new V50Models.EffectiveConsensus[10];
        for (int p = 0; p < 10; p++) {
            int active = 0;
            double distribution = 0.0d;
            double overlap = 0.0d;
            double rank = 0.0d;
            int pairs = 0;
            double signal = 0.0d;
            double historical = 0.0d;
            double recent = 0.0d;
            double state = 0.0d;
            double directional = directionalAgreement(experts, reliability, p);
            for (int e = 0; experts != null && e < experts.length; e++) {
                V50Models.ExpertEvidence expert = experts[e];
                if (!valid(expert, p)) continue;
                V50ExpertRegretEngine.Reliability r = value(reliability, e, p);
                if (r.weight <= 0.0d) continue;
                active++;
                signal += expert.signalStrength[p];
                historical += r.historical;
                recent += r.recent;
                state += r.state;
                for (int f = e + 1; f < experts.length; f++) {
                    V50Models.ExpertEvidence other = experts[f];
                    if (!valid(other, p)) continue;
                    if (value(reliability, f, p).weight <= 0.0d) continue;
                    distribution +=
                            1.0d
                                    - V49ExpertSupport.js(
                                            expert.numberProbability[p],
                                            other.numberProbability[p]);
                    overlap += top3Jaccard(expert.numberProbability[p], other.numberProbability[p]);
                    rank +=
                            rankCorrelation(
                                    expert.numberProbability[p], other.numberProbability[p]);
                    pairs++;
                }
            }
            if (pairs > 0) {
                distribution /= pairs;
                overlap /= pairs;
                rank /= pairs;
            }
            if (active > 0) {
                signal /= active;
                historical /= active;
                recent /= active;
                state /= active;
            }
            double agreement =
                    clamp01(
                            0.34d * distribution
                                    + 0.24d * overlap
                                    + 0.22d * rank
                                    + 0.20d * directional);
            double effective =
                    clamp01(
                            agreement
                                    * signal
                                    * Math.sqrt(Math.max(0.0d, historical * recent))
                                    * Math.sqrt(Math.max(0.0d, state)));
            String checksum =
                    V50ChecksumEngine.sha256(
                            p
                                    + "|"
                                    + distribution
                                    + "|"
                                    + overlap
                                    + "|"
                                    + rank
                                    + "|"
                                    + directional
                                    + "|"
                                    + signal
                                    + "|"
                                    + historical
                                    + "|"
                                    + recent
                                    + "|"
                                    + state
                                    + "|"
                                    + effective);
            out[p] =
                    new V50Models.EffectiveConsensus(
                            p + 1,
                            distribution,
                            overlap,
                            rank,
                            directional,
                            signal,
                            historical,
                            recent,
                            state,
                            effective,
                            checksum);
        }
        return out;
    }

    private static boolean valid(V50Models.ExpertEvidence expert, int position) {
        return expert != null
                && expert.numberProbability != null
                && position >= 0
                && position < expert.numberProbability.length
                && expert.numberProbability[position] != null
                && expert.numberProbability[position].length == 10
                && expert.signalStrength != null
                && position < expert.signalStrength.length;
    }

    private static V50ExpertRegretEngine.Reliability value(
            V50ExpertRegretEngine.Reliability[][] values, int expert, int position) {
        if (values != null
                && expert < values.length
                && values[expert] != null
                && position < values[expert].length
                && values[expert][position] != null) {
            return values[expert][position];
        }
        return new V50ExpertRegretEngine.Reliability(
                0, 0.5d, 0.5d, 0.5d, V50Enums.ExpertStatus.WATCH);
    }

    private static double top3Jaccard(double[] a, double[] b) {
        int[] ta = V49ExpertSupport.top3(a);
        int[] tb = V49ExpertSupport.top3(b);
        int intersection = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (ta[i] == tb[j]) {
                    intersection++;
                    break;
                }
            }
        }
        return ((double) intersection) / (6 - intersection);
    }

    private static double rankCorrelation(double[] a, double[] b) {
        int[] ra = ranks(a);
        int[] rb = ranks(b);
        double sum = 0.0d;
        for (int i = 0; i < 10; i++) {
            double d = ra[i] - rb[i];
            sum += d * d;
        }
        double rho = 1.0d - 6.0d * sum / (10.0d * 99.0d);
        return clamp01((rho + 1.0d) * 0.5d);
    }

    private static int[] ranks(double[] probability) {
        int[] rank = new int[10];
        Integer[] index = new Integer[10];
        for (int i = 0; i < 10; i++) index[i] = Integer.valueOf(i);
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                int left = index[i].intValue();
                int right = index[j].intValue();
                if (probability[right] > probability[left]
                        || (probability[right] == probability[left] && right < left)) {
                    Integer temp = index[i];
                    index[i] = index[j];
                    index[j] = temp;
                }
            }
        }
        for (int i = 0; i < 10; i++) rank[index[i].intValue()] = i + 1;
        return rank;
    }

    private static double directionalAgreement(
            V50Models.ExpertEvidence[] experts,
            V50ExpertRegretEngine.Reliability[][] reliability,
            int position) {
        if (experts == null || experts.length == 0) return 0.0d;
        double agreement = 0.0d;
        int numbers = 0;
        for (int n = 0; n < 10; n++) {
            int positive = 0;
            int negative = 0;
            for (int e = 0; e < experts.length; e++) {
                if (!valid(experts[e], position)) continue;
                if (value(reliability, e, position).weight <= 0.0d) continue;
                double delta = experts[e].numberProbability[position][n] - 0.1d;
                if (delta > 1.0e-5d) positive++;
                else if (delta < -1.0e-5d) negative++;
            }
            int votes = positive + negative;
            if (votes == 0) continue;
            agreement += ((double) Math.max(positive, negative)) / votes;
            numbers++;
        }
        return numbers == 0 ? 0.0d : agreement / numbers;
    }

    private static double clamp01(double value) {
        return FeatureContractRegistry.clamp01(value);
    }
}
