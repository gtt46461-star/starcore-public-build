package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** V45 separates probability rank, expected-value rank and action rank. */
public final class RankingAuditEngine {
    public static final String V46_ROLE = "FINAL40_RANK_DIAGNOSTIC_AUDITOR";

    public static final String MODULE_VERSION = AppConfig.V46_VERSION;

    public static final class Row {
        public Models.ComboCandidate candidate;
        public int probabilityRank;
        public int expectedValueRank;
        public int actionRank;
        public double probability;
        public double expectedValue;
        public double actionScore;

        public String summary() {
            return PredictionEnvelope.candidateKey(candidate)
                    + "|P#"
                    + probabilityRank
                    + "|EV#"
                    + expectedValueRank
                    + "|A#"
                    + actionRank
                    + "|P="
                    + round(probability)
                    + "|EV="
                    + round(expectedValue)
                    + "|Action="
                    + round(actionScore);
        }
    }

    private RankingAuditEngine() {}

    public static ArrayList<Row> build(List<Models.ComboCandidate> input) {
        ArrayList<Row> rows = new ArrayList<Row>();
        if (input == null) return rows;
        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null || !c.isValidK3()) continue;
            PredictionEnvelope e = PredictionEnvelope.readOnly(c);
            Row r = new Row();
            r.candidate = c;
            r.probability = e == null ? 0.0d : e.canonicalProbability;
            r.expectedValue = expectedValue(r.probability);
            r.actionScore = actionScore(c, r.probability, r.expectedValue);
            rows.add(r);
        }
        assign(rows, 0);
        assign(rows, 1);
        assign(rows, 2);
        return rows;
    }

    public static void persist(DbStore store, List<Models.ComboCandidate> input) {
        if (store == null) return;
        ArrayList<Row> rows = build(input);
        try {
            store.setState("v46_ranking_audit_version", MODULE_VERSION);
            store.setState("v45_ranking_count", String.valueOf(rows.size()));
            StringBuilder sb = new StringBuilder();
            int n = Math.min(12, rows.size());
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append("\n");
                sb.append(rows.get(i).summary());
            }
            store.setState("v45_ranking_detail", sb.toString());
        } catch (Throwable ignore) {
        }
    }

    public static double expectedValue(double probability) {
        /* K3 costs 3 units and wins net 6.9 units at odds 9.9. */
        return clamp01(probability) * 6.9d - (1.0d - clamp01(probability)) * 3.0d;
    }

    private static double actionScore(Models.ComboCandidate c, double p, double ev) {
        double uncertainty = clamp01(c.actionUncertaintyPenalty);
        double hard = c.hardBlockFlag == 1 || c.v43HardRisk == 1 ? 1.0d : 0.0d;
        double quality =
                clamp01(
                        0.50d
                                        * max3(
                                                c.coverageQuality,
                                                c.coverageQualityScore,
                                                c.dynamicGateCoverageQualityScore)
                                + 0.50d
                                        * max3(
                                                c.thirdQuality,
                                                c.thirdNumberQualityScore,
                                                c.dynamicGateThirdNumberQuality));
        return ev + 0.30d * quality - 0.55d * uncertainty - 3.0d * hard;
    }

    private static void assign(ArrayList<Row> rows, final int mode) {
        ArrayList<Row> copy = new ArrayList<Row>(rows);
        Collections.sort(
                copy,
                new Comparator<Row>() {
                    public int compare(Row a, Row b) {
                        double av =
                                mode == 0
                                        ? a.probability
                                        : (mode == 1 ? a.expectedValue : a.actionScore);
                        double bv =
                                mode == 0
                                        ? b.probability
                                        : (mode == 1 ? b.expectedValue : b.actionScore);
                        int x = Double.compare(bv, av);
                        if (x != 0) return x;
                        return PredictionEnvelope.candidateKey(a.candidate)
                                .compareTo(PredictionEnvelope.candidateKey(b.candidate));
                    }
                });
        for (int i = 0; i < copy.size(); i++) {
            Row r = copy.get(i);
            if (mode == 0) r.probabilityRank = i + 1;
            else if (mode == 1) r.expectedValueRank = i + 1;
            else r.actionRank = i + 1;
        }
    }

    private static double max3(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }
}
