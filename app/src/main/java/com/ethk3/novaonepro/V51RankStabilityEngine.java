package com.ethk3.novaonepro;

/** B10 perturbation audit for a 1200-action group. */
public final class V51RankStabilityEngine {
    public static final String V57_B10_OWNER = "RANK_STABILITY_AUDIT_1200";

    public static final class Result {
        public final double retention, thirdRobustness, rankMarginRobustness;
        public final boolean stable;

        Result(double r, double t, double m) {
            retention = r;
            thirdRobustness = t;
            rankMarginRobustness = m;
            stable = r >= 0.75d && t >= 0.60d && m >= 0.50d;
        }
    }

    public Result evaluate(V51Models.PositionAction rankOne, V51Models.PositionAction[] all) {
        if (rankOne == null || all == null || all.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return new Result(0, 0, 0);
        double[] delta = new double[] {-0.020, -0.015, -0.010, -0.005, 0.005, 0.010, 0.015, 0.020};
        int wins = 0;
        for (int d = 0; d < delta.length; d++) {
            double best = -Double.MAX_VALUE;
            String bestFp = "";
            for (int i = 0; i < all.length; i++) {
                V51Models.PositionAction a = all[i];
                double s =
                        a.rankScore
                                + (a.fingerprint.equals(rankOne.fingerprint)
                                        ? delta[d]
                                        : -delta[d] / Math.max(1, all.length - 1));
                if (s > best) {
                    best = s;
                    bestFp = a.fingerprint;
                }
            }
            if (rankOne.fingerprint.equals(bestFp)) wins++;
        }
        double retention = wins / (double) delta.length;
        double third = V51Math.clamp(0.5d + rankOne.thirdMargin / 0.04d, 0, 1);
        double margin = V51Math.clamp(rankOne.rankMargin / 0.025d, 0, 1);
        return new Result(retention, third, margin);
    }
}
