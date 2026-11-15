package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * V46 one and only final ranking after every evidence producer has finished.
 *
 * <p>The input surface is up to 60 fully evaluated candidates. The formal surface is 40 candidates.
 * At least two candidates per position are retained when available, then the remaining slots are
 * filled globally. UI display count is separate.
 */
public final class FinalCandidateRankingEngine {
    public static final String MODULE_VERSION =
            "V48_SINGLE_ACTION_FINAL40_FROM60_RANKING_20260721_48072148";

    private FinalCandidateRankingEngine() {}

    public static ArrayList<Models.ComboCandidate> rank(
            DbStore store, List<Models.ComboCandidate> input, String targetIssue) {
        ArrayList<Models.ComboCandidate> ranked = new ArrayList<Models.ComboCandidate>();
        if (input != null) {
            for (int i = 0; i < input.size(); i++) {
                Models.ComboCandidate c = input.get(i);
                if (c != null && c.isValidK3()) ranked.add(c);
            }
        }
        Collections.sort(ranked, comparator());

        int cap = Math.min(AppConfig.V48_FORMAL_CANDIDATE_COUNT, ranked.size());
        ArrayList<Models.ComboCandidate> selected = new ArrayList<Models.ComboCandidate>();
        HashSet<String> used = new HashSet<String>();
        int[] perPosition = new int[AppConfig.POSITION_COUNT + 1];

        /* Diversity floor.  It prevents a small number of positions from consuming the
         * entire formal surface, but it never changes the final probability-first order. */
        for (int i = 0; i < ranked.size() && selected.size() < cap; i++) {
            Models.ComboCandidate c = ranked.get(i);
            int p = c.position;
            if (p >= 1
                    && p <= AppConfig.POSITION_COUNT
                    && perPosition[p] < AppConfig.V48_MIN_FORMAL_PER_POSITION) {
                selected.add(c);
                used.add(key(c));
                perPosition[p]++;
            }
        }
        for (int i = 0; i < ranked.size() && selected.size() < cap; i++) {
            Models.ComboCandidate c = ranked.get(i);
            if (used.contains(key(c))) continue;
            selected.add(c);
            used.add(key(c));
            if (c.position >= 1 && c.position <= AppConfig.POSITION_COUNT)
                perPosition[c.position]++;
        }
        Collections.sort(selected, comparator());

        int coveredPositions = 0;
        int minPerPosition = Integer.MAX_VALUE;
        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            if (perPosition[p] > 0) coveredPositions++;
            if (perPosition[p] < minPerPosition) minPerPosition = perPosition[p];
        }
        if (minPerPosition == Integer.MAX_VALUE) minPerPosition = 0;

        ArrayList<Models.ComboCandidate> formal = new ArrayList<Models.ComboCandidate>();
        for (int i = 0; i < selected.size(); i++) {
            Models.ComboCandidate c = selected.get(i);
            c.rank = i + 1;
            c.oosRankSlot = i + 1;
            c.oosCandidateCount = selected.size();
            c.v46FinalRank = i + 1;
            c.v46FormalCandidateCount = selected.size();
            c.v46Version = AppConfig.V48_VERSION;
            c.v46EventProbability = eventP(c);
            c.v46ActionLcb = actionLcb(c);
            c.v46ExpectedValue = ev(c);
            c.rankingVersion = MODULE_VERSION;
            c.v46ProbabilityTrace = append(c.v46ProbabilityTrace, "FinalRank40=" + (i + 1));
            formal.add(c);
        }
        try {
            if (store != null) {
                store.setState("v46_final_rank_version", MODULE_VERSION);
                store.setState("v46_final_rank_target", safe(targetIssue));
                store.setState("v46_final_rank_input_count", String.valueOf(ranked.size()));
                store.setState("v46_final_rank_count", String.valueOf(formal.size()));
                store.setState(
                        "v46_final_rank_position_coverage", String.valueOf(coveredPositions));
                store.setState("v46_final_rank_min_per_position", String.valueOf(minPerPosition));
                store.setState(
                        "v46_final_rank_cutoff_p",
                        formal.size() == 0
                                ? "0"
                                : String.valueOf(eventP(formal.get(formal.size() - 1))));
                store.setState(
                        "v46_final_rank_best",
                        formal.size() == 0 ? "EMPTY" : summary(formal.get(0)));
                store.setState(
                        "v46_candidate_surface_policy",
                        "FULL_SCAN1200_LIGHT->CROSSFIT_PROBABILITY60->FINAL_EVIDENCE60->FORMAL40->UI12");
            }
        } catch (Throwable ignore) {
        }
        return formal;
    }

    private static Comparator<Models.ComboCandidate> comparator() {
        return new Comparator<Models.ComboCandidate>() {
            public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                int x = Double.compare(eventP(b), eventP(a));
                if (x != 0) return x;
                x = Double.compare(actionLcb(b), actionLcb(a));
                if (x != 0) return x;
                x = Double.compare(b.v46CalibrationConfidence, a.v46CalibrationConfidence);
                if (x != 0) return x;
                x = Double.compare(b.rawRankScore, a.rawRankScore);
                if (x != 0) return x;
                x = Double.compare(b.actionUtility, a.actionUtility);
                if (x != 0) return x;
                x = a.position - b.position;
                if (x != 0) return x;
                return a.k3Mask - b.k3Mask;
            }
        };
    }

    private static String key(Models.ComboCandidate c) {
        return c == null ? "" : c.position + "_" + c.k3Mask;
    }

    public static String summary(Models.ComboCandidate c) {
        return c == null
                ? "EMPTY"
                : "R"
                        + c.v46FinalRank
                        + "|P="
                        + eventP(c)
                        + "|EV="
                        + ev(c)
                        + "|LCB="
                        + actionLcb(c)
                        + "|P"
                        + c.position
                        + "|"
                        + c.n1
                        + "/"
                        + c.n2
                        + "/"
                        + c.n3;
    }

    public static double eventP(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        return valid(c.v46EventProbability)
                ? c.v46EventProbability
                : (valid(c.finalP) ? c.finalP : c.calibratedHitProbability);
    }

    public static double actionLcb(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        return valid(c.v46ActionLcb)
                ? c.v46ActionLcb
                : (valid(c.v43SafeProbability) ? c.v43SafeProbability : eventP(c));
    }

    public static double ev(Models.ComboCandidate c) {
        return AppConfig.ODDS * eventP(c) - AppConfig.BET_COST;
    }

    private static boolean valid(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "|" + b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
