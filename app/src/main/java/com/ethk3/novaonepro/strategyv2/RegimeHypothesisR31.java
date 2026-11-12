package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B07-C06: short-window state hypotheses composed from existing family outputs.
 *
 * <ul>
 * <li>{@link #proposeWindow} picks a window by nested time validation: for every outer fold the choice uses only
 * inner folds strictly before it, and the reported window is the one fixed before the final outer fold.</li>
 * <li>{@link Detector} is a registered two-sided CUSUM over a past outcome stream (onset, then expiry). Its state at
 * decision t uses outcomes through t-1 only; an outcome at or after the decision is refused.</li>
 * <li>{@link #routePastState} routes to the regime expert only while the past-state detector is active.</li>
 * <li>{@link #toSinglePositionK3} turns cross-position support (lag >= 1) into a single target position q and a
 * candidate set C = intersection of the supports: |C| < 3 abstains, |C| >= 3 picks three distinct numbers by a fixed
 * tie-break (score descending, then number ascending). Expiry only changes later decisions.</li>
 * </ul>
 */
public final class RegimeHypothesisR31 {
    private RegimeHypothesisR31() {}
    public static final String VERSION = "R31_REGIME_HYPOTHESIS_V1";

    /** Registered detector protocol. */
    public static final class Protocol {
        public final double p0, p1, threshold;
        public Protocol(double p0, double p1, double threshold) {
            if (!(p0 > 0 && p0 < p1 && p1 < 1 && threshold > 0)) throw new IllegalArgumentException("R31_REGIME_PROTOCOL");
            this.p0 = p0; this.p1 = p1; this.threshold = threshold;
        }
        public static Protocol registered() { return new Protocol(StatsR27.BASE, 0.55, 10); }
        public String hash() { return StatsR27.sha256(VERSION + "|" + p0 + "|" + p1 + "|" + threshold); }
    }

    /** Two-sided CUSUM: onset when the elevated-rate log-likelihood ratio crosses the threshold, expiry when the base rate does. */
    public static final class Detector {
        public final Protocol protocol; private double up, down; private boolean active; private long lastOrdinal = Long.MIN_VALUE;
        private final List<Long> onsets = new ArrayList<>(), expiries = new ArrayList<>();
        public Detector(Protocol protocol) { this.protocol = protocol; }
        /** Feeds the settled outcome of period `ordinal` (a hit of the focal rule). Older or repeated ordinals are refused. */
        public void observe(long ordinal, boolean hit) {
            if (ordinal <= lastOrdinal) throw new IllegalStateException("R31_REGIME_ORDINAL_ORDER:" + ordinal);
            double llr = hit ? Math.log(protocol.p1 / protocol.p0) : Math.log((1 - protocol.p1) / (1 - protocol.p0));
            if (!active) { up = Math.max(0, up + llr); if (up >= protocol.threshold) { active = true; down = 0; onsets.add(ordinal); } }
            else { down = Math.max(0, down - llr); if (down >= protocol.threshold) { active = false; up = 0; expiries.add(ordinal); } }
            lastOrdinal = ordinal;
        }
        /** State for a decision at `decisionOrdinal`: only outcomes strictly before it may have been observed. */
        public boolean activeFor(long decisionOrdinal) {
            if (lastOrdinal >= decisionOrdinal) throw new IllegalStateException("R31_REGIME_FUTURE_INPUT:" + lastOrdinal + ">=" + decisionOrdinal);
            return active;
        }
        public List<Long> onsets() { return Collections.unmodifiableList(onsets); }
        public List<Long> expiries() { return Collections.unmodifiableList(expiries); }
        public long observedThrough(){return lastOrdinal;}
        public boolean expired() { return !active && !expiries.isEmpty(); }
        public String encode() {
            StringBuilder b = new StringBuilder(protocol.hash()).append('|').append(Double.doubleToLongBits(up)).append('|').append(Double.doubleToLongBits(down)).append('|').append(active).append('|').append(lastOrdinal);
            b.append('|'); for (long o : onsets) b.append(o).append(','); b.append('|'); for (long o : expiries) b.append(o).append(',');
            return b.toString();
        }
        public static Detector decode(Protocol protocol, String encoded) {
            String[] f = encoded.split("\\|", -1);
            if (f.length != 7 || !f[0].equals(protocol.hash())) throw new IllegalArgumentException("R31_REGIME_STATE_PROTOCOL");
            Detector d = new Detector(protocol); d.up = Double.longBitsToDouble(Long.parseLong(f[1])); d.down = Double.longBitsToDouble(Long.parseLong(f[2]));
            d.active = Boolean.parseBoolean(f[3]); d.lastOrdinal = Long.parseLong(f[4]);
            for (String o : f[5].split(",")) if (!o.isEmpty()) d.onsets.add(Long.parseLong(o));
            for (String o : f[6].split(",")) if (!o.isEmpty()) d.expiries.add(Long.parseLong(o));
            return d;
        }
    }

    public interface WindowScore { double score(int window, int from, int to); }
    public static final class WindowSelection {
        public final int window; public final List<Integer> foldChoices; public final List<Integer> foldStarts;
        WindowSelection(int window, List<Integer> choices, List<Integer> starts) { this.window = window; foldChoices = Collections.unmodifiableList(choices); foldStarts = Collections.unmodifiableList(starts); }
    }
    /** Nested time validation on [devFrom, devTo): fold k is scored only by windows chosen on folds before it. */
    public static WindowSelection proposeWindow(int[] windows, WindowScore score, int devFrom, int devTo, int folds) {
        if (windows.length == 0 || folds < 2 || devTo - devFrom < folds) throw new IllegalArgumentException("R31_REGIME_WINDOW_PROTOCOL");
        int size = (devTo - devFrom) / folds; List<Integer> choices = new ArrayList<>(), starts = new ArrayList<>(); int chosen = windows[0];
        for (int k = 1; k < folds; k++) {
            int outer = devFrom + k * size; double best = Double.NEGATIVE_INFINITY;
            for (int w : windows) { double s = 0; for (int j = 0; j < k; j++) s += score.score(w, devFrom + j * size, devFrom + (j + 1) * size); if (s > best + 1e-12) { best = s; chosen = w; } }
            choices.add(chosen); starts.add(outer);
        }
        return new WindowSelection(chosen, choices, starts);
    }

    /** Route of one decision: the regime expert while the past-state detector is active, otherwise the base expert. */
    public static String routePastState(Detector detector, long decisionOrdinal, String regimeExpert, String baseExpert) { return detector.activeFor(decisionOrdinal) ? regimeExpert : baseExpert; }

    public static final class K3 {
        public final int position; public final boolean abstain; public final int[] numbers; public final double[] probabilities; public final int candidateSize; public final String reason;
        K3(int position, int[] numbers, double[] probabilities, int size, String reason) { this.position = position; this.numbers = numbers; this.abstain = numbers == null; this.probabilities = probabilities; candidateSize = size; this.reason = reason; }
    }
    /**
     * @param position target position q (1..10)
     * @param supports per source position, the set of numbers (1..10) supported for q, each built with inputs up to t-1
     * @param supportOrdinals per source position, the latest input ordinal used
     * @param scores optional per-number score for the tie-break (null = all equal)
     */
    public static K3 toSinglePositionK3(int position, long decisionOrdinal, List<Set<Integer>> supports, long[] supportOrdinals, double[] scores) {
        if (position < 1 || position > 10) throw new IllegalArgumentException("R31_REGIME_TARGET_POSITION");
        if (supports == null || supports.isEmpty() || supportOrdinals == null || supportOrdinals.length != supports.size()) throw new IllegalArgumentException("R31_REGIME_SUPPORTS");
        for (long o : supportOrdinals) if (o >= decisionOrdinal) throw new IllegalArgumentException("R31_REGIME_FUTURE_INPUT:" + o + ">=" + decisionOrdinal);
        TreeSet<Integer> c = new TreeSet<>(supports.get(0));
        for (Set<Integer> s : supports) c.retainAll(s);
        for (int n : c) if (n < 1 || n > 10) throw new IllegalArgumentException("R31_REGIME_NUMBER:" + n);
        double[] p = new double[10];
        if (c.size() < 3) { Arrays.fill(p, 0.1); return new K3(position, null, p, c.size(), "ABSTAIN_INTERSECTION_LT_3"); } // uninformative, never filled with random numbers
        List<Integer> order = new ArrayList<>(c);
        order.sort((a, b) -> { double sa = scores == null ? 0 : scores[a - 1], sb = scores == null ? 0 : scores[b - 1]; return sa != sb ? Double.compare(sb, sa) : Integer.compare(a, b); });
        int[] pick = {order.get(0), order.get(1), order.get(2)}; Arrays.sort(pick);
        for (int n : c) p[n - 1] = 1.0 / c.size();
        return new K3(position, pick, p, c.size(), "STRICT_INTERSECTION");
    }
}
