package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V45 offline/replay coverage frontier. It never creates BET and never changes P. It measures which
 * score threshold can honestly support 5/10/15/20/25% coverage.
 */
public final class CoverageFrontierEngine {
    public static final String MODULE_VERSION = "V45_COVERAGE_FRONTIER_20260720";

    public static final class Sample {
        public String targetIssue = "";
        public double score;
        public double probability;
        public int hit;
    }

    public static final class Point {
        public double targetCoverage;
        public double actualCoverage;
        public double threshold;
        public int total;
        public int bets;
        public int hits;
        public double hitRate;
        public double pnl;
        public int maxLose;
        public double wilson80;
        public String status = "WATCH";

        public String summary() {
            return "coverage="
                    + round(actualCoverage)
                    + "/target="
                    + round(targetCoverage)
                    + "|threshold="
                    + round(threshold)
                    + "|bets="
                    + bets
                    + "/"
                    + total
                    + "|hit="
                    + hits
                    + "|rate="
                    + round(hitRate)
                    + "|W80="
                    + round(wilson80)
                    + "|pnl="
                    + round(pnl)
                    + "|maxLose="
                    + maxLose
                    + "|status="
                    + status;
        }
    }

    private CoverageFrontierEngine() {}

    public static ArrayList<Point> evaluate(List<Sample> input) {
        ArrayList<Point> out = new ArrayList<Point>();
        double[] targets = new double[] {0.05d, 0.10d, 0.15d, 0.20d, 0.25d};
        if (input == null || input.size() == 0) {
            for (int i = 0; i < targets.length; i++) {
                Point p = new Point();
                p.targetCoverage = targets[i];
                p.status = "NO_SAMPLE";
                out.add(p);
            }
            return out;
        }
        ArrayList<Sample> sorted = new ArrayList<Sample>();
        for (int i = 0; i < input.size(); i++) if (input.get(i) != null) sorted.add(input.get(i));
        Collections.sort(
                sorted,
                new Comparator<Sample>() {
                    public int compare(Sample a, Sample b) {
                        int x = Double.compare(b.score, a.score);
                        if (x != 0) return x;
                        return safe(a.targetIssue).compareTo(safe(b.targetIssue));
                    }
                });
        for (int t = 0; t < targets.length; t++) out.add(point(sorted, targets[t]));
        return out;
    }

    public static void persist(DbStore store, List<Sample> input) {
        if (store == null) return;
        ArrayList<Point> points = evaluate(input);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(points.get(i).summary());
        }
        try {
            store.setState("v45_coverage_frontier_version", MODULE_VERSION);
            store.setState("v45_coverage_frontier_detail", sb.toString());
        } catch (Throwable ignore) {
        }
    }

    private static Point point(ArrayList<Sample> sorted, double target) {
        Point p = new Point();
        p.targetCoverage = target;
        p.total = sorted.size();
        p.bets = Math.max(1, (int) Math.round(sorted.size() * target));
        if (p.bets > sorted.size()) p.bets = sorted.size();
        p.threshold = sorted.get(p.bets - 1).score;
        int lose = 0;
        for (int i = 0; i < p.bets; i++) {
            Sample s = sorted.get(i);
            if (s.hit == 1) {
                p.hits++;
                lose = 0;
                p.pnl += 6.9d;
            } else {
                lose++;
                if (lose > p.maxLose) p.maxLose = lose;
                p.pnl -= 3.0d;
            }
        }
        p.actualCoverage = p.total <= 0 ? 0.0d : ((double) p.bets) / p.total;
        p.hitRate = p.bets <= 0 ? 0.0d : ((double) p.hits) / p.bets;
        p.wilson80 = wilsonLower(p.hits, p.bets, 1.2815515655446004d);
        if (p.bets < 30) p.status = "SAMPLE_WEAK";
        else if (p.hitRate >= 0.37d
                && p.wilson80 > AppConfig.BREAK_EVEN_PROBABILITY
                && p.pnl > 0.0d) p.status = "VERIFIED";
        else if (p.hitRate >= AppConfig.BREAK_EVEN_PROBABILITY && p.pnl >= 0.0d)
            p.status = "WATCH_POSITIVE";
        else p.status = "NOT_PROVEN";
        return p;
    }

    private static double wilsonLower(int hit, int n, double z) {
        if (n <= 0) return 0.0d;
        double rate = ((double) hit) / n;
        double z2 = z * z;
        double denominator = 1.0d + z2 / n;
        double center = rate + z2 / (2.0d * n);
        double margin = z * Math.sqrt((rate * (1.0d - rate) + z2 / (4.0d * n)) / n);
        return clamp01((center - margin) / denominator);
    }

    private static double clamp01(double v) {
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
