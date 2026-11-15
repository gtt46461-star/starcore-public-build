package com.ethk3.novaonepro;

import java.util.ArrayList;

/** Recomputes the complete action surface after removing each expert. */
public final class V50TrueAblationEngine {
    public static final class Entry {
        public final String removedExpert;
        public final int selectedPosition;
        public final int[] top3;
        public final double rankScore;
        public final double selectedHitP;
        public final boolean selectionChanged;
        public final String checksum;

        Entry(
                String removedExpert,
                int selectedPosition,
                int[] top3,
                double rankScore,
                double selectedHitP,
                boolean selectionChanged,
                String checksum) {
            this.removedExpert = removedExpert == null ? "" : removedExpert;
            this.selectedPosition = selectedPosition;
            this.top3 = top3 == null ? new int[0] : (int[]) top3.clone();
            this.rankScore = rankScore;
            this.selectedHitP = selectedHitP;
            this.selectionChanged = selectionChanged;
            this.checksum = checksum == null ? "" : checksum;
        }
    }

    public static final class Result {
        public final Entry[] entries;
        public final String frozenBlob;
        public final String checksum;

        Result(Entry[] entries, String frozenBlob, String checksum) {
            this.entries = entries == null ? new Entry[0] : entries;
            this.frozenBlob = frozenBlob == null ? "" : frozenBlob;
            this.checksum = checksum == null ? "" : checksum;
        }
    }

    public Result recompute(
            V50Models.PredictionContext context,
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            V50Models.ExpertEvidence[] experts,
            V50ExpertRegretEngine.Reliability[][] reliability,
            V50Models.PositionAction fullRankOne) {
        ArrayList<Entry> out = new ArrayList<Entry>();
        StringBuilder blob = new StringBuilder();
        for (int removed = 0; experts != null && removed < experts.length; removed++) {
            double[][] probability = fuseWithout(experts, reliability, removed);
            V50Models.EffectiveConsensus[] consensus =
                    consensusWithout(experts, reliability, removed);
            V50Models.PositionAction[] unranked =
                    new V50PositionActionBuilder()
                            .build(context, probability, features, states, consensus);
            V50Models.PositionAction[] ranked = new V50ActionRankerEngine().rank(unranked);
            V50Models.PositionAction rankOne = new V50ActionRankerEngine().rankOne(ranked);
            if (rankOne == null) continue;
            double calibrated = new V50SelectedActionCalibrationEngine().calibrate(rankOne);
            boolean changed =
                    fullRankOne == null
                            || fullRankOne.position != rankOne.position
                            || !sameTop3(fullRankOne.top3, rankOne.top3);
            String checksum =
                    V50ChecksumEngine.sha256(
                            experts[removed].expertId
                                    + '|'
                                    + rankOne.checksum
                                    + '|'
                                    + calibrated
                                    + '|'
                                    + changed);
            Entry entry =
                    new Entry(
                            experts[removed].expertId,
                            rankOne.position,
                            rankOne.top3,
                            rankOne.rankScore,
                            calibrated,
                            changed,
                            checksum);
            out.add(entry);
            if (blob.length() > 0) blob.append(';');
            blob.append(entry.removedExpert)
                    .append(':')
                    .append(entry.selectedPosition)
                    .append(':')
                    .append(entry.top3[0])
                    .append(',')
                    .append(entry.top3[1])
                    .append(',')
                    .append(entry.top3[2])
                    .append(':')
                    .append(entry.selectedHitP)
                    .append(':')
                    .append(entry.selectionChanged ? 1 : 0)
                    .append(':')
                    .append(entry.checksum);
        }
        String frozen = blob.toString();
        return new Result(
                out.toArray(new Entry[out.size()]), frozen, V50ChecksumEngine.sha256(frozen));
    }

    private static double[][] fuseWithout(
            V50Models.ExpertEvidence[] experts,
            V50ExpertRegretEngine.Reliability[][] reliability,
            int removed) {
        double[][] out = new double[10][10];
        for (int p = 0; p < 10; p++) {
            double weightSum = 0.0d;
            double[] logPool = new double[10];
            for (int e = 0; e < experts.length; e++) {
                if (e == removed || experts[e] == null) continue;
                V50ExpertRegretEngine.Reliability profile = reliability[e][p];
                double weight =
                        experts[e].trust[p]
                                * profile.weight
                                * (profile.historical + profile.recent + profile.state)
                                / 3.0d
                                * (1.0d - 0.55d * experts[e].uncertainty[p])
                                * experts[e].signalStrength[p];
                if (weight <= 1.0e-9d) continue;
                weightSum += weight;
                for (int n = 0; n < 10; n++) {
                    logPool[n] +=
                            weight
                                    * Math.log(
                                            Math.max(1.0e-12d, experts[e].numberProbability[p][n]));
                }
            }
            if (weightSum <= 1.0e-9d) {
                for (int n = 0; n < 10; n++) out[p][n] = 0.1d;
            } else {
                double max = -Double.MAX_VALUE;
                for (int n = 0; n < 10; n++) {
                    logPool[n] /= weightSum;
                    max = Math.max(max, logPool[n]);
                }
                for (int n = 0; n < 10; n++)
                    out[p][n] = Math.exp(Math.max(-40.0d, Math.min(40.0d, logPool[n] - max)));
                FeatureContractRegistry.normalize10(out[p]);
            }
        }
        return out;
    }

    private static V50Models.EffectiveConsensus[] consensusWithout(
            V50Models.ExpertEvidence[] experts,
            V50ExpertRegretEngine.Reliability[][] reliability,
            int removed) {
        V50Models.ExpertEvidence[] subset = new V50Models.ExpertEvidence[experts.length - 1];
        V50ExpertRegretEngine.Reliability[][] profiles =
                new V50ExpertRegretEngine.Reliability[experts.length - 1][10];
        int index = 0;
        for (int e = 0; e < experts.length; e++) {
            if (e == removed) continue;
            subset[index] = experts[e];
            profiles[index] = reliability[e];
            index++;
        }
        return new V50EffectiveConsensusEngine().evaluate(subset, profiles);
    }

    private static boolean sameTop3(int[] a, int[] b) {
        if (a == null || b == null || a.length != 3 || b.length != 3) return false;
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return false;
        return true;
    }
}
