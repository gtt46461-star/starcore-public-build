package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B07-C04: read-only improvement claim of a child over its parent.
 *
 * <p>Parent and child are compared on the same calendar opportunity sequence: every opportunity of the registered
 * window is present for both, an abstention is a zero-utility opportunity of the same sequence (so different act
 * counts are still paired), and a series that keeps only some opportunities (for example the periods the child
 * won) is a protocol violation. Every prediction must have been frozen before its outcome was available. A claim
 * binds experiment, parent, child, window, primary metric, error budget, cost, the independent segment it consumed,
 * the selection process and the hashes of the frozen predictions and outcomes; when any of them is missing it can
 * only be PROVISIONAL. An independent segment is consumed once: re-running, renaming or restarting on the same
 * segment adds no independent evidence and no new error budget. Only VERIFIED is eligible for champion replacement,
 * and the claim never replaces a champion by itself.</p>
 */
public final class VerifiedImprovementR31 {
    private VerifiedImprovementR31() {}
    public static final String VERSION = "R31_VERIFIED_IMPROVEMENT_V1", METRIC = "PAIRED_ACT_UTILITY_MINUS_BASE";
    public enum Status { VERIFIED, PROVISIONAL, NOT_IMPROVED, REJECTED }

    /** One calendar opportunity. hit is meaningful only when the side acted; p is the probability mass of its K3. */
    public static final class Opportunity {
        public final long ordinal, frozenAt, outcomeAt; public final boolean parentAct, childAct, parentHit, childHit; public final double parentP, childP;
        public Opportunity(long ordinal, long frozenAt, long outcomeAt, boolean parentAct, boolean parentHit, double parentP, boolean childAct, boolean childHit, double childP) {
            this.ordinal = ordinal; this.frozenAt = frozenAt; this.outcomeAt = outcomeAt; this.parentAct = parentAct; this.parentHit = parentHit; this.parentP = parentP;
            this.childAct = childAct; this.childHit = childHit; this.childP = childP;
        }
    }
    public static final class Evidence {
        public final String experimentId, parentHash, childHash, segmentKey, selectionProcessHash, frozenPredictionsHash, outcomesHash;
        public final long windowFrom, windowTo, costUnits; public final double errorBudget, costPenalty; public final List<Opportunity> series;
        public Evidence(String experimentId, String parentHash, String childHash, long windowFrom, long windowTo, String segmentKey, String selectionProcessHash,
                        String frozenPredictionsHash, String outcomesHash, double errorBudget, long costUnits, double costPenalty, List<Opportunity> series) {
            this.experimentId = experimentId; this.parentHash = parentHash; this.childHash = childHash; this.windowFrom = windowFrom; this.windowTo = windowTo;
            this.segmentKey = segmentKey; this.selectionProcessHash = selectionProcessHash; this.frozenPredictionsHash = frozenPredictionsHash; this.outcomesHash = outcomesHash;
            this.errorBudget = errorBudget; this.costUnits = costUnits; this.costPenalty = costPenalty;
            this.series = series == null ? Collections.<Opportunity>emptyList() : Collections.unmodifiableList(new ArrayList<>(series));
        }
    }
    public static final class Claim {
        public final Status status; public final List<String> reasons; public final Evidence evidence;
        public final long opportunities, parentActs, childActs; public final double parentHitRate, childHitRate, parentFrequency, childFrequency, parentBrier, childBrier;
        public final double meanDifference, lowerBound, netImprovement;
        Claim(Status status, List<String> reasons, Evidence e, long n, long pa, long ca, double phr, double chr, double pb, double cb, double mean, double lower) {
            this.status = status; this.reasons = Collections.unmodifiableList(new ArrayList<>(reasons)); this.evidence = e; opportunities = n; parentActs = pa; childActs = ca;
            parentHitRate = phr; childHitRate = chr; parentFrequency = n == 0 ? 0 : pa / (double) n; childFrequency = n == 0 ? 0 : ca / (double) n; parentBrier = pb; childBrier = cb;
            meanDifference = mean; lowerBound = lower; netImprovement = lower - (e == null ? 0 : e.costPenalty);
        }
        /** Everything needed to re-find the frozen predictions, outcomes and selection behind this claim. */
        public Map<String, Object> trace() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("version", VERSION); m.put("status", status.name()); m.put("reasons", new ArrayList<Object>(reasons)); m.put("metric", METRIC);
            if (evidence != null) {
                m.put("experimentId", evidence.experimentId); m.put("parentHash", evidence.parentHash); m.put("childHash", evidence.childHash);
                m.put("windowFrom", evidence.windowFrom); m.put("windowTo", evidence.windowTo); m.put("segmentKey", evidence.segmentKey);
                m.put("selectionProcessHash", evidence.selectionProcessHash); m.put("frozenPredictionsHash", evidence.frozenPredictionsHash); m.put("outcomesHash", evidence.outcomesHash);
                m.put("errorBudget", evidence.errorBudget); m.put("costUnits", evidence.costUnits); m.put("costPenalty", fin(evidence.costPenalty));
            }
            m.put("opportunities", opportunities); m.put("parentActs", parentActs); m.put("childActs", childActs);
            m.put("parentHitRate", fin(parentHitRate)); m.put("childHitRate", fin(childHitRate)); m.put("parentFrequency", parentFrequency); m.put("childFrequency", childFrequency);
            m.put("parentBrier", fin(parentBrier)); m.put("childBrier", fin(childBrier)); m.put("meanDifference", fin(meanDifference)); m.put("lowerBound", fin(lowerBound)); m.put("netImprovement", fin(netImprovement));
            return m;
        }
    }
    static Object fin(double d) { return Double.isFinite(d) ? (Object) d : null; }

    /** Durable set of consumed independent segments (persisted with the store that owns confirmations). */
    public static final class Ledger {
        private final Map<String, String> consumed = new TreeMap<>(); // segment -> first experiment
        public boolean consumed(String segment) { return consumed.containsKey(segment); }
        public String firstExperiment(String segment) { return consumed.get(segment); }
        public Map<String, Object> json() { return new LinkedHashMap<String, Object>(consumed); }
        public static Ledger restore(Map<String, Object> json) { Ledger l = new Ledger(); if (json != null) for (Map.Entry<String, Object> e : json.entrySet()) l.consumed.put(e.getKey(), (String) e.getValue()); return l; }
    }

    /** Protocol violations of the paired comparison; empty when the series is a lawful complete pairing. */
    public static List<String> compareContract(Evidence e) {
        List<String> v = new ArrayList<>();
        if (e.windowTo < e.windowFrom) { v.add("WINDOW_EMPTY"); return v; }
        long expected = e.windowTo - e.windowFrom + 1;
        Set<Long> seen = new HashSet<>(); long previous = Long.MIN_VALUE;
        for (Opportunity o : e.series) {
            if (o.ordinal < e.windowFrom || o.ordinal > e.windowTo) { v.add("OPPORTUNITY_OUTSIDE_WINDOW:" + o.ordinal); continue; }
            if (!seen.add(o.ordinal)) v.add("OPPORTUNITY_REPEATED:" + o.ordinal);
            if (o.ordinal < previous) v.add("OPPORTUNITY_ORDER:" + o.ordinal);
            previous = o.ordinal;
            if (o.frozenAt >= o.outcomeAt) v.add("FUTURE_LABEL_USED:" + o.ordinal);
            if (!(o.parentP >= 0 && o.parentP <= 1 && o.childP >= 0 && o.childP <= 1)) v.add("PROBABILITY_RANGE:" + o.ordinal);
        }
        if (seen.size() != expected) v.add("SELECTED_OR_MISSING_OPPORTUNITIES:" + seen.size() + "/" + expected);
        return v;
    }
    static boolean blank(String s) { return s == null || s.isEmpty(); }

    /** Evaluates one claim and, when it is VERIFIED or NOT_IMPROVED on fresh independent evidence, consumes the segment. */
    public static Claim record(Ledger ledger, Evidence e, double z) {
        List<String> reasons = new ArrayList<>(compareContract(e));
        long n = 0, pa = 0, ca = 0, ph = 0, ch = 0; double sum = 0, sum2 = 0, pb = 0, cb = 0;
        for (Opportunity o : e.series) {
            n++;
            double pu = o.parentAct ? (o.parentHit ? 1 : 0) - StatsR27.BASE : 0, cu = o.childAct ? (o.childHit ? 1 : 0) - StatsR27.BASE : 0, d = cu - pu;
            sum += d; sum2 += d * d;
            if (o.parentAct) { pa++; if (o.parentHit) ph++; pb += sq(o.parentP - (o.parentHit ? 1 : 0)); }
            if (o.childAct) { ca++; if (o.childHit) ch++; cb += sq(o.childP - (o.childHit ? 1 : 0)); }
        }
        double mean = n == 0 ? Double.NaN : sum / n, var = n < 2 ? Double.NaN : Math.max(0, (sum2 - n * mean * mean) / (n - 1));
        double lower = n < 2 ? Double.NaN : mean - z * Math.sqrt(var / n);
        double phr = pa == 0 ? Double.NaN : ph / (double) pa, chr = ca == 0 ? Double.NaN : ch / (double) ca;
        double pBrier = pa == 0 ? Double.NaN : pb / pa, cBrier = ca == 0 ? Double.NaN : cb / ca;
        if (!reasons.isEmpty()) return new Claim(Status.REJECTED, reasons, e, n, pa, ca, phr, chr, pBrier, cBrier, mean, lower);
        for (String[] key : new String[][]{{"experimentId", e.experimentId}, {"parentHash", e.parentHash}, {"childHash", e.childHash}, {"segmentKey", e.segmentKey},
                {"selectionProcessHash", e.selectionProcessHash}, {"frozenPredictionsHash", e.frozenPredictionsHash}, {"outcomesHash", e.outcomesHash}})
            if (blank(key[1])) reasons.add("MISSING_" + key[0]);
        if (!(e.errorBudget > 0 && e.errorBudget < 1)) reasons.add("MISSING_ERROR_BUDGET");
        if (e.costUnits <= 0 || !(e.costPenalty >= 0)) reasons.add("MISSING_COST");
        if (!reasons.isEmpty()) return new Claim(Status.PROVISIONAL, reasons, e, n, pa, ca, phr, chr, pBrier, cBrier, mean, lower);
        if (ledger.consumed(e.segmentKey)) {
            reasons.add("SEGMENT_ALREADY_CONSUMED_BY:" + ledger.firstExperiment(e.segmentKey));
            return new Claim(Status.PROVISIONAL, reasons, e, n, pa, ca, phr, chr, pBrier, cBrier, mean, lower);
        }
        ledger.consumed.put(e.segmentKey, e.experimentId);
        boolean improved = Double.isFinite(lower) && lower - e.costPenalty > 0;
        if (!improved) reasons.add(Double.isFinite(lower) ? "LOWER_BOUND_NOT_ABOVE_COST:" + lower : "INSUFFICIENT_OPPORTUNITIES");
        return new Claim(improved ? Status.VERIFIED : Status.NOT_IMPROVED, reasons, e, n, pa, ca, phr, chr, pBrier, cBrier, mean, lower);
    }
    static double sq(double x) { return x * x; }
    /** z of a one-sided bound at the claim's error budget (Cornish-Fisher free normal quantile by bisection). */
    public static double z(double errorBudget) {
        if (!(errorBudget > 0 && errorBudget < .5)) throw new IllegalArgumentException("R31_ERROR_BUDGET");
        double lo = 0, hi = 10;
        for (int i = 0; i < 200; i++) { double mid = (lo + hi) / 2; if (upperTail(mid) > errorBudget) lo = mid; else hi = mid; }
        return hi;
    }
    static double upperTail(double x) { return 0.5 * erfc(x / Math.sqrt(2)); }
    static double erfc(double x) { double t = 1 / (1 + 0.5 * Math.abs(x)); double y = t * Math.exp(-x * x - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * 0.17087277))))))))); return x >= 0 ? y : 2 - y; }
    public static boolean isEligibleForChampionReplacement(Claim c) { return c != null && c.status == Status.VERIFIED; }

    /** Development-window receipt for a champion succession inside evolution: the same SELECT opportunities for
     * parent and child, indexed by dataset row (a missing calendar period has no row for either side). Development
     * evidence is never independent, so this is PROVISIONAL at most. */
    public static Map<String, Object> developmentReceipt(String parentHash, String childHash, ReplayEvaluatorR27.Trace parent, ReplayEvaluatorR27.Trace child, int from, int to) {
        List<Opportunity> series = new ArrayList<>();
        for (int t = from; t < to; t++) {
            boolean ps = parent.skipped[t], cs = child.skipped[t];
            series.add(new Opportunity(t, 0, 1, !ps && parent.act[t], !ps && parent.hit[t] == 1, ps ? 0 : Math.min(1, parent.pHit[t]), !cs && child.act[t], !cs && child.hit[t] == 1, cs ? 0 : Math.min(1, child.pHit[t])));
        }
        Evidence e = new Evidence("DEVELOPMENT:" + childHash, parentHash, childHash, from, to - 1, "", "", "", "", 0.05, 1, 0, series);
        List<String> violations = compareContract(e);
        Claim c = record(new Ledger(), e, z(0.05));
        Map<String, Object> m = new LinkedHashMap<>(c.trace());
        m.put("status", violations.isEmpty() ? Status.PROVISIONAL.name() : Status.REJECTED.name());
        m.put("reason", violations.isEmpty() ? "DEVELOPMENT_WINDOW_NOT_INDEPENDENT_EVIDENCE" : String.join(",", violations));
        m.put("championReplacedByThisReceipt", false);
        return m;
    }
}
