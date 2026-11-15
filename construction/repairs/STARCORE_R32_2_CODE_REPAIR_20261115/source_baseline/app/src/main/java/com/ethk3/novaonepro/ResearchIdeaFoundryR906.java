package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/** Failure-driven method factory. Tuning variants and genuinely different executable families are explicit. */
public final class ResearchIdeaFoundryR906 {
    private ResearchIdeaFoundryR906() {}

    public static List<ResearchAssetRegistryR906.Asset> create(SQLiteDatabase db, FailurePortrait p, int max) {
        ArrayList<ResearchAssetRegistryR906.Asset> out = new ArrayList<ResearchAssetRegistryR906.Asset>();
        if (db == null || p == null) return out;
        int bound = Math.max(1, Math.min(8, max));
        long seed = seed(p.hash);
        for (int i = 0; i < bound; i++) {
            seed = next(seed);
            int mode = i < 3 ? i : (int) Math.floorMod(seed, 7);
            String rep, learner, update, feature;
            boolean executable = true;
            switch (mode) {
                case 0:
                    rep = "BAYESIAN_TRANSITION";
                    learner = "DIRICHLET_BAYES_MARKOV";
                    update = "ORDERED_DIRICHLET_NEXT_ISSUE_ONLY";
                    feature = "raw_action_p+assignment_support+window_consensus+third_margin+stability";
                    break;
                case 1:
                    rep = "METRIC_RETRIEVAL";
                    learner = "KNN_METRIC_RETRIEVAL";
                    update = "FROZEN_CENTROID_NEXT_ISSUE_ONLY";
                    feature = "raw_action_p+rank_score+third_margin+entropy_risk+stability+window_consensus+assignment_support";
                    break;
                case 2:
                    rep = "COMPACT_FOREST";
                    learner = "COMPACT_OBLIVIOUS_FOREST";
                    update = "ORDERED_TREE_REFRESH_NEXT_ISSUE_ONLY";
                    feature = "raw_action_p+rank_score+third_margin+entropy_risk+stability+window_consensus+assignment_support";
                    break;
                case 3:
                    rep = "TEMPORAL_NEURAL";
                    learner = "TINY_TEMPORAL_NEURAL_OFFLINE";
                    update = "OFFLINE_EXPANDING_WINDOW";
                    feature = "short_long_residual+window_consensus+stability";
                    executable = false; // must wait for an imported offline artifact/parity fixture
                    break;
                case 4:
                    rep = "PERMUTATION_GRAPH";
                    learner = "FTRL_LINEAR_GRAPH";
                    update = "ONLINE_FTRL_NEXT_ISSUE_ONLY";
                    feature = p.relationFailure
                            ? "assignment_support+window_consensus+raw_action_p"
                            : "raw_action_p+rank_score+third_margin+stability";
                    break;
                case 5:
                    rep = "EVIDENCE_WEAVE";
                    learner = "FTRL_LINEAR_GRAPH";
                    update = "ONLINE_FTRL_NEXT_ISSUE_ONLY";
                    feature = p.windowFailure
                            ? "short_long_residual+window_consensus+stability"
                            : "raw_action_p+third_margin+entropy_risk+stability";
                    break;
                default:
                    rep = "RESIDUAL_TRANSPORT";
                    learner = "FTRL_LINEAR_GRAPH";
                    update = "ONLINE_FTRL_NEXT_ISSUE_ONLY";
                    feature = p.rankFailure
                            ? "rank_score+third_margin+rank_regret_prior"
                            : "raw_action_p+rank_score+third_margin+stability";
                    break;
            }
            ResearchMethodPlanR906 plan = new ResearchMethodPlanR906(
                    rep,
                    feature,
                    learner,
                    update,
                    p.coverageFailure ? "PAIRED_HIT_PLUS_COVERAGE" : "PAIRED_LOGLOSS_PLUS_HIT",
                    "SELECTION_CONDITIONED_ISOTONIC",
                    "ENTROPY_DRIFT_BLOCK_GUARD",
                    "SEALED_FUTURE_BLOCK_AND_PERMUTATION_TEST",
                    "",
                    Math.max(1, p.generation + i),
                    i == 0 ? "FAILURE_GUIDED_INVENTION" : "DIVERSE_MUTATION",
                    executable);
            try {
                out.add(ResearchAssetRegistryR906.register(db, plan, null));
            } catch (Throwable duplicate) {
                // Method signature/name uniqueness intentionally collapses semantic duplicates.
            }
        }
        return out;
    }

    public static final class FailurePortrait {
        public final boolean rankFailure, windowFailure, relationFailure, coverageFailure;
        public final int generation;
        public final String hash;

        public FailurePortrait(boolean r, boolean w, boolean rel, boolean cov, int g, String basis) {
            rankFailure = r;
            windowFailure = w;
            relationFailure = rel;
            coverageFailure = cov;
            generation = Math.max(1, g);
            hash = V51ChecksumEngine.sha256(
                    "R906_FAILURE|" + r + '|' + w + '|' + rel + '|' + cov + '|'
                            + generation + '|' + safe(basis));
        }
    }

    private static long seed(String x) {
        long h = 1469598103934665603L;
        for (int i = 0; i < x.length(); i++) { h ^= x.charAt(i); h *= 1099511628211L; }
        return h;
    }
    private static long next(long x) { return x * 6364136223846793005L + 1442695040888963407L; }
    private static String safe(String x) { return x == null ? "" : x.trim(); }
}
