package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** R27-026 successive-halving elimination on H_SELECT (docs/05 5.5). Replaces the 120-live-label Wilson pruning
 * contract of ResearchPruningR965. Eliminated candidates are not deleted: the caller archives them as RETIRED with
 * the round and reason returned here. */
public final class SuccessiveHalvingR27 {
    private SuccessiveHalvingR27() {}
    public static final double MIN_ACT = 0.01, MAX_ACT = 0.60, BRIER_SLACK = 0.005, WORST_BLOCK_MIN = 0.22;

    public static final class Candidate {
        public final PolicySpecR27 spec; public final ReplayEvaluatorR27.Trace trace; public final double temperature;
        public String retiredRound, retiredReason; public ReplayScoreR27 select;
        public Candidate(PolicySpecR27 spec, ReplayEvaluatorR27.Trace trace, double temperature) { this.spec = spec; this.trace = trace; this.temperature = temperature; }
    }

    public static final class Result { public final List<Candidate> survivors = new ArrayList<>(), retired = new ArrayList<>(); public final int[] roundIn = new int[3]; }

    public static Result run(List<Candidate> in, SplitR27 split) {
        Result res = new Result(); List<Candidate> alive = new ArrayList<>(in);
        int[] windows = {500, 1500, Integer.MAX_VALUE};
        for (int round = 0; round < 3; round++) {
            res.roundIn[round] = alive.size();
            int from = Math.max(split.selectFrom, split.selectTo - windows[round]), to = split.selectTo;
            List<Candidate> ok = new ArrayList<>(); final double[] key = new double[alive.size()];
            for (Candidate c : alive) {
                ReplayScoreR27 s = ReplayScoreR27.of(SplitR27.SELECT, c.trace, from, to);
                if (round == 2) c.select = s;
                String reason = null;
                if (s.actRate() < MIN_ACT) reason = "ACT_RATE_LT_1PCT";
                else if (s.actRate() > MAX_ACT) reason = "ACT_RATE_GT_60PCT";
                else if (round >= 1 && s.brier > ReplayScoreR27.constantBrier(c.trace, from, to) + BRIER_SLACK) reason = "BRIER_WORSE_THAN_CONSTANT_0_30";
                else if (round >= 2 && Double.isFinite(s.worstBlockHr) && s.worstBlockHr < WORST_BLOCK_MIN) reason = "WORST_720_BLOCK_HR_LT_22PCT";
                if (reason != null) { c.retiredRound = "R" + round; c.retiredReason = reason; res.retired.add(c); }
                else ok.add(c);
            }
            final List<Candidate> sorted = new ArrayList<>(ok);
            final double[] u = new double[sorted.size()];
            for (int i = 0; i < u.length; i++) u[i] = ReplayScoreR27.of(SplitR27.SELECT, sorted.get(i).trace, from, to).pairedUMean;
            List<Integer> order = new ArrayList<>(); for (int i = 0; i < u.length; i++) order.add(i);
            order.sort(Comparator.<Integer>comparingDouble(i -> -u[i]).thenComparing(i -> sorted.get(i).spec.policyHash()));
            int keep = Math.max(1, (sorted.size() + 1) / 2);
            alive = new ArrayList<>();
            for (int k = 0; k < order.size(); k++) {
                Candidate c = sorted.get(order.get(k));
                if (k < keep) alive.add(c); else { c.retiredRound = "R" + round; c.retiredReason = "BELOW_HALF_BY_PAIRED_U"; res.retired.add(c); }
            }
            if (alive.isEmpty()) break;
        }
        for (Candidate c : alive) if (c.select == null) c.select = ReplayScoreR27.of(SplitR27.SELECT, c.trace, split.selectFrom, split.selectTo);
        res.survivors.addAll(alive);
        return res;
    }
}
