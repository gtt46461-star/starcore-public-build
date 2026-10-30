package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R29-034/036/037 R29_GATE_V2 (docs/12 §2/§4, registries/r29_policy_defaults.json). Evaluated next to the unchanged
 * R27 certificate gate and recorded separately (r29_gate_v2): it reports support, statistics, time blocks, frequency
 * and risk as independent sections and states whether V2 would issue. ENABLED is false, so a V2 verdict never issues,
 * revokes or reinterprets a certificate; activation is a policy-versioned change after device and power validation.
 *
 * Calibration (§2): fixed 10 equal-width bins (p=1 in the last bin), validation ACT ≥ 200, ECE weighted over all
 * samples ≤ 0.04, bins with ≥ 20 samples are "supported" and must hold ≥ 90% of the validation mass. Sparse bins stay
 * in the ECE; nothing requires predictions to span several bins; a new prediction outside the supported range is
 * OUT_OF_SUPPORT. Validation uses the probability frozen before the draw (nothing is fitted on these outcomes).
 * Frequency (§4): FUTURE_ACT_RATE = FUTURE ACT decisions / every calendar opportunity since registration (missed and
 * not-frozen periods stay in the denominator); Q02 is NOT_DEPLOYED until the policy is deployed, never a prerequisite.
 * Hit rate uses settled ACT only; pending ACT is listed separately. */
public final class GateV2R29 {
    private GateV2R29() {}
    public static final String POLICY = "R29_GATE_V2"; public static final boolean ENABLED = false;
    public static final int BINS = 10, MIN_VALIDATION_ACTS = 200, MIN_BIN_SUPPORT = 20, MIN_ACTS = 250, BLOCK = 720, MIN_BLOCKS = 3, MIN_BLOCK_ACTS = 80;
    public static final double MAX_ECE = 0.04, MIN_SUPPORTED_MASS = 0.9, MIN_HR = 0.34, DESIRED_HR = 0.37, MIN_WORST_BLOCK = 0.26, TARGET_ACT_RATE = 0.15, RISK_TAIL = 0.01;
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r29_gate_v2(policy_hash TEXT PRIMARY KEY,policy_version TEXT NOT NULL,would_issue INTEGER NOT NULL,failing TEXT NOT NULL,sections_json TEXT NOT NULL,evaluated_through INTEGER NOT NULL,evaluated_at INTEGER NOT NULL)";

    // ---------------------------------------------------------------- calibration support
    public static final class Calibration {
        public long n, invalidRows; public double ece = Double.NaN, supportedMass = Double.NaN, unsupportedMass = Double.NaN, supportLow = Double.NaN, supportHigh = Double.NaN;
        public final long[] count = new long[BINS]; public final double[] meanP = new double[BINS], rate = new double[BINS], lo = new double[BINS], hi = new double[BINS];
        public String status;
        public boolean inSupport(double p) { return probability(p) && count[bin(p)] >= MIN_BIN_SUPPORT; }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("status", status); m.put("validationActs", n); m.put("ece", fin(ece)); m.put("supportedMass", fin(supportedMass));
            m.put("unsupportedMass", fin(unsupportedMass)); m.put("supportRange", Double.isFinite(supportLow) ? supportLow + "-" + supportHigh : null);
            m.put("invalidRows", invalidRows);
            List<Object> bins = new ArrayList<>();
            for (int b = 0; b < BINS; b++) { if (count[b] == 0) continue; Map<String, Object> x = new LinkedHashMap<>(); x.put("bin", b); x.put("n", count[b]); x.put("meanP", meanP[b]); x.put("observed", rate[b]); x.put("wilson95", lo[b] + "-" + hi[b]); x.put("supported", count[b] >= MIN_BIN_SUPPORT); bins.add(x); }
            m.put("bins", bins); return m;
        }
    }
    static boolean probability(double p) { return Double.isFinite(p) && p >= 0 && p <= 1; }
    public static int bin(double p) {
        if (!probability(p)) throw new IllegalArgumentException("R29_INVALID_PROBABILITY");
        return Math.min(BINS - 1, (int) Math.floor(p * BINS));
    }

    public static Calibration calibration(double[] p, int[] y, int n) {
        Calibration c = new Calibration(); c.n = n;
        if (p == null || y == null || n < 0 || n > p.length || n > y.length) { c.status = "INVALID_INPUT"; return c; }
        for (int i = 0; i < n; i++) if (!probability(p[i]) || (y[i] != 0 && y[i] != 1)) c.invalidRows++;
        if (c.invalidRows > 0) { c.status = "INVALID_INPUT"; return c; }
        double[] sp = new double[BINS], sy = new double[BINS];
        for (int i = 0; i < n; i++) { int b = bin(p[i]); c.count[b]++; sp[b] += p[i]; sy[b] += y[i]; }
        if (n == 0) { c.status = "INSUFFICIENT"; return c; }
        double e = 0, supported = 0;
        for (int b = 0; b < BINS; b++) {
            if (c.count[b] == 0) continue;
            c.meanP[b] = sp[b] / c.count[b]; c.rate[b] = sy[b] / c.count[b];
            double[] w = wilson((long) sy[b], c.count[b]); c.lo[b] = w[0]; c.hi[b] = w[1];
            e += c.count[b] / (double) n * Math.abs(c.meanP[b] - c.rate[b]);
            if (c.count[b] >= MIN_BIN_SUPPORT) { supported += c.count[b]; double l = b / (double) BINS, h = (b + 1) / (double) BINS;
                c.supportLow = Double.isNaN(c.supportLow) ? l : Math.min(c.supportLow, l); c.supportHigh = Double.isNaN(c.supportHigh) ? h : Math.max(c.supportHigh, h); }
        }
        c.ece = e; c.supportedMass = supported / n; c.unsupportedMass = 1 - c.supportedMass;
        c.status = n < MIN_VALIDATION_ACTS ? "INSUFFICIENT" : e <= MAX_ECE && c.supportedMass >= MIN_SUPPORTED_MASS ? "PASS" : "FAIL";
        return c;
    }
    static double[] wilson(long k, long n) {
        if (n == 0) return new double[] {Double.NaN, Double.NaN};
        double z = 1.959963984540054, ph = k / (double) n, d = 1 + z * z / n, c = (ph + z * z / (2 * n)) / d, h = z * Math.sqrt(ph * (1 - ph) / n + z * z / (4.0 * n * n)) / d;
        return new double[] {Math.max(0, c - h), Math.min(1, c + h)};
    }

    // ---------------------------------------------------------------- evidence and verdict
    public static final class Evidence {
        public String policyHash, sequentialState; public long registeredOrdinal = -1, latestOrdinal = -1, opportunities, frozen, actDecisions, settledActs, pendingActs, hits;
        public double[] p = new double[0]; public int[] y = new int[0]; public long[] actOrdinal = new long[0];
        public double pairedLcbVsI00 = Double.NaN, pairedLcbVsNull = Double.NaN; public boolean deployed;
    }
    public static final class Verdict { public boolean wouldIssue; public final List<String> failing = new ArrayList<>(); public final Map<String, Object> sections = new LinkedHashMap<>(); }

    public static Verdict evaluate(Evidence e) {
        Verdict v = new Verdict();
        // statistics
        double hr = e.settledActs == 0 ? Double.NaN : e.hits / (double) e.settledActs; double[] hrCi = wilson(e.hits, e.settledActs);
        Map<String, Object> st = new LinkedHashMap<>(); st.put("sequentialState", e.sequentialState); st.put("settledActs", e.settledActs); st.put("pendingActs", e.pendingActs);
        st.put("hits", e.hits); st.put("hitRate", fin(hr)); st.put("hitRateWilson95", e.settledActs == 0 ? null : hrCi[0] + "-" + hrCi[1]); st.put("desired037", Double.isFinite(hr) && hr >= DESIRED_HR);
        gate(v, "sequentialTest", SequentialQualificationR27.QUALIFIED.equals(e.sequentialState));
        gate(v, "settledActs>=250", e.settledActs >= MIN_ACTS);
        gate(v, "hitRate>=0.34", Double.isFinite(hr) && hr >= MIN_HR);
        gate(v, "betterThanI00", Double.isFinite(e.pairedLcbVsI00) && e.pairedLcbVsI00 > 0);
        gate(v, "betterThanShuffledControl", Double.isFinite(e.pairedLcbVsNull) && e.pairedLcbVsNull > 0);
        st.put("pairedLcbVsI00", fin(e.pairedLcbVsI00)); st.put("pairedLcbVsNull", fin(e.pairedLcbVsNull));
        v.sections.put("statistics", st);
        // calibration support
        Calibration c = calibration(e.p, e.y, e.p.length);
        gate(v, "calibrationSupportedEce", "PASS".equals(c.status));
        v.sections.put("calibration", c.json());
        // time blocks registered before the first outcome: block = floor(ordinal/720), complete when fully elapsed after registration
        Map<Long, long[]> blocks = new LinkedHashMap<>();
        for (int i = 0; i < e.actOrdinal.length; i++) { long[] a = blocks.computeIfAbsent(Math.floorDiv(e.actOrdinal[i], (long) BLOCK), k -> new long[2]); a[0]++; a[1] += e.y[i]; }
        List<Object> rows = new ArrayList<>(); int complete = 0, eligible = 0; double worst = Double.NaN;
        long firstBlock = Math.floorDiv(e.registeredOrdinal, (long) BLOCK);
        if (firstBlock * BLOCK < e.registeredOrdinal) firstBlock++;
        // The first three full calendar blocks are fixed by registration, including empty/weak blocks.
        // Selecting any three blocks with >=80 ACTs after seeing their outcomes would cherry-pick stability.
        for (int j = 0; j < MIN_BLOCKS; j++) {
            long key = firstBlock + j, start = key * BLOCK, end = start + BLOCK - 1; boolean done = e.registeredOrdinal >= 0 && end <= e.latestOrdinal;
            long[] values = blocks.get(key); if (values == null) values = new long[2];
            Map<String, Object> x = new LinkedHashMap<>(); x.put("block", key); x.put("acts", values[0]); x.put("hits", values[1]); x.put("complete", done); rows.add(x);
            if (!done) continue; complete++;
            if (values[0] >= MIN_BLOCK_ACTS) { eligible++; double r = values[1] / (double) values[0]; worst = Double.isNaN(worst) ? r : Math.min(worst, r); }
        }
        Map<String, Object> tb = new LinkedHashMap<>(); tb.put("completeBlocks", complete); tb.put("blocksWith80Acts", eligible); tb.put("worstBlockHitRate", fin(worst)); tb.put("blocks", rows);
        gate(v, "completeBlocks>=3(80acts)", eligible >= MIN_BLOCKS);
        gate(v, "worstBlock>=0.26", Double.isFinite(worst) && worst >= MIN_WORST_BLOCK);
        v.sections.put("timeBlocks", tb);
        // frequency (reported, not an admission gate; Q02 only after deployment)
        Map<String, Object> fr = new LinkedHashMap<>();
        double futureRate = e.opportunities == 0 ? Double.NaN : e.actDecisions / (double) e.opportunities;
        fr.put("futureActDecisions", e.actDecisions); fr.put("scheduledOpportunities", e.opportunities); fr.put("frozenOpportunities", e.frozen);
        fr.put("FUTURE_ACT_RATE", fin(futureRate)); fr.put("target", TARGET_ACT_RATE); fr.put("targetMet", Double.isFinite(futureRate) && futureRate >= TARGET_ACT_RATE);
        fr.put("Q02", e.deployed ? "MEASURED_FROM_REAL" : "NOT_DEPLOYED"); fr.put("quota", "never forced");
        v.sections.put("frequency", fr);
        // risk: longest losing streak and its tail probability under the observed hit rate (registered report, no hard cap)
        int streak = 0, max = 0; for (int y : e.y) { if (y == 0) { streak++; max = Math.max(max, streak); } else streak = 0; }
        double tail = Double.isFinite(hr) ? streakTail(e.y.length, max, 1 - hr) : Double.NaN;
        Map<String, Object> rk = new LinkedHashMap<>(); rk.put("maxLosingStreak", max); rk.put("tailProbabilityAtObservedHr", fin(tail));
        rk.put("riskReview", Double.isFinite(tail) && tail < RISK_TAIL ? "REVIEW_REQUIRED" : "NONE"); rk.put("staking", "fixed; martingale/chasing forbidden");
        v.sections.put("risk", rk);
        v.wouldIssue = v.failing.isEmpty();
        Map<String, Object> pol = new LinkedHashMap<>(); pol.put("policy", POLICY); pol.put("enabled", ENABLED); pol.put("effect", ENABLED ? "ISSUES" : "REPORT_ONLY; R27 certificate rule unchanged");
        v.sections.put("policy", pol);
        return v;
    }
    static void gate(Verdict v, String name, boolean ok) { if (!ok) v.failing.add(name); }
    static Object fin(double d) { return Double.isFinite(d) ? (Object) d : null; }

    /** P(longest run of misses ≥ L in n Bernoulli trials with miss probability q), exact DP. */
    public static double streakTail(int n, int len, double q) {
        if (len <= 0) return 1; if (n < len) return 0;
        double[] s = new double[len]; s[0] = 1; double hitAll = 0; // s[k]: run currently k (<len), not yet reached len
        for (int t = 0; t < n; t++) {
            double[] x = new double[len]; double total = 0; for (double v : s) total += v;
            x[0] = total * (1 - q);
            for (int k = 0; k < len - 1; k++) x[k + 1] += s[k] * q;
            hitAll += s[len - 1] * q; s = x;
        }
        return Math.min(1, hitAll);
    }

    // ---------------------------------------------------------------- database
    /** Reads the FUTURE evidence of one live experiment. */
    public static Evidence evidence(SQLiteDatabase ro, String id, long latestOrdinal) {
        Evidence e = new Evidence(); e.policyHash = id; e.latestOrdinal = latestOrdinal;
        try (Cursor c = ro.rawQuery("SELECT state FROM r27_eprocess WHERE policy_hash=?", new String[] {id})) { if (c.moveToFirst()) e.sequentialState = c.getString(0); }
        try (Cursor c = ro.rawQuery("SELECT min(target_ordinal),sum(CASE WHEN status='FROZEN' THEN 1 ELSE 0 END),sum(CASE WHEN status='FROZEN' AND act=1 THEN 1 ELSE 0 END),"
                + "sum(CASE WHEN status='FROZEN' AND act=1 AND hit<0 THEN 1 ELSE 0 END) FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal<=?", new String[] {id, String.valueOf(latestOrdinal)})) {
            if (c.moveToFirst() && !c.isNull(0)) { e.registeredOrdinal = c.getLong(0); e.frozen = c.getLong(1); e.actDecisions = c.getLong(2); e.pendingActs = c.getLong(3); e.opportunities = latestOrdinal - e.registeredOrdinal + 1; }
        }
        long registered = QualificationInboxR29.calendarStart(ro, id);
        if (registered >= 0) { e.registeredOrdinal = registered; e.opportunities = Math.max(0, latestOrdinal - registered + 1); }
        List<double[]> rows = new ArrayList<>();
        try (Cursor c = ro.rawQuery("SELECT target_ordinal,probabilities,n1,n2,n3,hit FROM r27_live_frozen WHERE policy_hash=? AND status='FROZEN' AND act=1 AND hit>=0 AND target_ordinal<=? ORDER BY target_ordinal", new String[] {id, String.valueOf(latestOrdinal)})) {
            while (c.moveToNext()) {
                double[] pr = StoreR27.unpack(c.getBlob(1)); double p = Double.NaN;
                int a = c.getInt(2), b = c.getInt(3), z = c.getInt(4);
                if (pr.length == 10 && a >= 1 && a <= 10 && b >= 1 && b <= 10 && z >= 1 && z <= 10 && a != b && a != z && b != z) {
                    double sum = 0; boolean valid = true;
                    for (double value : pr) { sum += value; valid &= probability(value); }
                    if (valid && Math.abs(sum - 1) <= 1e-6) p = Math.min(1, pr[a - 1] + pr[b - 1] + pr[z - 1]);
                }
                rows.add(new double[] {c.getLong(0), p, c.getInt(5)});
            }
        }
        int n = rows.size(); e.settledActs = n; e.p = new double[n]; e.y = new int[n]; e.actOrdinal = new long[n];
        for (int i = 0; i < n; i++) { double[] r = rows.get(i); e.actOrdinal[i] = (long) r[0]; e.p[i] = r[1]; e.y[i] = (int) r[2]; e.hits += e.y[i]; }
        return e;
    }

    /** Statement recording one V2 evaluation (added to the certify batch). */
    public static void record(ResearchWriteQueueR27.Batch b, Evidence e, Verdict v) {
        b.add("INSERT OR REPLACE INTO r29_gate_v2(policy_hash,policy_version,would_issue,failing,sections_json,evaluated_through,evaluated_at) VALUES(?,?,?,?,?,?,?)",
                e.policyHash, POLICY, v.wouldIssue ? 1 : 0, String.join(",", v.failing), JsonR27.write(v.sections), e.latestOrdinal, System.currentTimeMillis());
    }
}
