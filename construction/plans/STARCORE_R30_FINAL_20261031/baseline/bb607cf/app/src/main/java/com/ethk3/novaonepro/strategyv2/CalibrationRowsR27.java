package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-037/038 calibration chain repair (docs/07 7.4). A late row (frozen at/after its open time) or a row whose
 * evaluation period is missing no longer invalidates the whole 241-row study: it is excluded with a specific reason
 * (EXCLUDED_LATE / EXCLUDED_GAP), counted in the opportunity denominator, and collection continues until 120 fit +
 * 1 embargo + 120 validation qualified rows exist. The fit segment may use same-rule replay outputs (H_SELECT);
 * the validation segment must be FUTURE rows. Platt scaling is the method; isotonic regression is an extra comparison
 * when ≥ 1,000 rows exist. Failure reasons are specific (no blanket BLOCKED_INVALID_INPUT). */
public final class CalibrationRowsR27 {
    private CalibrationRowsR27() {}
    public static final int FIT = 120, EMBARGO = 1, VALIDATION = 120;
    public static final String EXCLUDED_LATE = "EXCLUDED_LATE", EXCLUDED_GAP = "EXCLUDED_GAP", IDENTITY_DRIFT = "IDENTITY_DRIFT", INSUFFICIENT_ROWS = "INSUFFICIENT_ROWS";

    public static final class Row {
        public final String issue, source; public final long frozenAt, openTime; public final double score; public final int hit, position; public final boolean evaluationPresent, identityMatches;
        public Row(String issue, String source, long frozenAt, long openTime, double score, int hit, int position, boolean evaluationPresent, boolean identityMatches) {
            this.issue = issue; this.source = source; this.frozenAt = frozenAt; this.openTime = openTime; this.score = score; this.hit = hit; this.position = position; this.evaluationPresent = evaluationPresent; this.identityMatches = identityMatches;
        }
    }
    public static final class Result {
        public String state, reason, method = "PLATT"; public int fitRows, validationRows, excludedLate, excludedGap, identityDrift; public double a = Double.NaN, b = Double.NaN, ece = Double.NaN, eceIsotonic = Double.NaN;
        public final Map<String, Object> eceByStratum = new LinkedHashMap<>(); public String fitSource = "";
    }

    /** @param rows ordered by issue; source is "REPLAY" (H_SELECT walk-forward output) or "FUTURE". */
    public static Result fit(List<Row> rows) {
        Result r = new Result(); List<Row> fit = new ArrayList<>(), val = new ArrayList<>(); int embargo = 0;
        for (Row row : rows) {
            if (!row.identityMatches) { r.identityDrift++; continue; }
            if (!row.evaluationPresent || row.hit < 0) { r.excludedGap++; continue; }
            if (row.source.equals("FUTURE") && (row.frozenAt <= 0 || row.frozenAt >= row.openTime)) { r.excludedLate++; continue; }
            if (fit.size() < FIT) { fit.add(row); continue; }
            if (embargo < EMBARGO) { embargo++; continue; }
            if (row.source.equals("FUTURE") && val.size() < VALIDATION) val.add(row);
        }
        r.fitRows = fit.size(); r.validationRows = val.size();
        java.util.Set<String> src = new java.util.TreeSet<>(); for (Row x : fit) src.add(x.source); r.fitSource = String.join("+", src);
        if (fit.size() < FIT || val.size() < VALIDATION) {
            r.state = "COLLECTING"; r.reason = INSUFFICIENT_ROWS + "(fit=" + fit.size() + "/120,validation=" + val.size() + "/120,late=" + r.excludedLate + ",gap=" + r.excludedGap + ",identity=" + r.identityDrift + ")";
            return r;
        }
        double[] ab = platt(fit); r.a = ab[0]; r.b = ab[1];
        double[] p = new double[val.size()]; int[] y = new int[val.size()];
        for (int i = 0; i < p.length; i++) { p[i] = sigmoid(r.a * val.get(i).score + r.b); y[i] = val.get(i).hit; }
        r.ece = StatsR27.ece(p, y, p.length, 10);
        Map<Integer, List<Integer>> byPos = new java.util.TreeMap<>();
        for (int i = 0; i < p.length; i++) byPos.computeIfAbsent(val.get(i).position, k -> new ArrayList<>()).add(i);
        for (Map.Entry<Integer, List<Integer>> e : byPos.entrySet()) {
            double[] pp = new double[e.getValue().size()]; int[] yy = new int[pp.length];
            for (int k = 0; k < pp.length; k++) { pp[k] = p[e.getValue().get(k)]; yy[k] = y[e.getValue().get(k)]; }
            r.eceByStratum.put("position" + e.getKey(), StatsR27.ece(pp, yy, pp.length, 10));
        }
        if (fit.size() + val.size() >= 1000) r.eceIsotonic = isotonicEce(fit, val);
        r.state = "FITTED"; r.reason = "PLATT_FIT_" + r.fitSource + "_VALIDATED_FUTURE";
        return r;
    }

    static double sigmoid(double z) { return 1 / (1 + Math.exp(-StatsR27.clamp(z, -30, 30))); }

    /** Newton–Raphson logistic regression of hit on score (with a tiny ridge for stability). */
    static double[] platt(List<Row> rows) {
        double a = 0, b = Math.log(0.3 / 0.7);
        for (int it = 0; it < 50; it++) {
            double ga = 0, gb = 0, haa = 1e-6, hab = 0, hbb = 1e-6;
            for (Row r : rows) { double p = sigmoid(a * r.score + b), w = p * (1 - p), e = p - r.hit; ga += e * r.score; gb += e; haa += w * r.score * r.score; hab += w * r.score; hbb += w; }
            double det = haa * hbb - hab * hab; if (Math.abs(det) < 1e-12) break;
            double da = (hbb * ga - hab * gb) / det, db = (haa * gb - hab * ga) / det;
            a -= da; b -= db; if (Math.abs(da) + Math.abs(db) < 1e-10) break;
        }
        return new double[] {a, b};
    }

    /** Pool-adjacent-violators isotonic fit on fit rows, scored by ECE on validation rows. */
    static double isotonicEce(List<Row> fit, List<Row> val) {
        List<Row> s = new ArrayList<>(fit); s.sort((x, y) -> Double.compare(x.score, y.score));
        List<double[]> blocks = new ArrayList<>(); // {sumY, n, maxScore}
        for (Row r : s) {
            blocks.add(new double[] {r.hit, 1, r.score});
            while (blocks.size() > 1) {
                double[] a = blocks.get(blocks.size() - 2), b = blocks.get(blocks.size() - 1);
                if (a[0] / a[1] <= b[0] / b[1]) break;
                a[0] += b[0]; a[1] += b[1]; a[2] = b[2]; blocks.remove(blocks.size() - 1);
            }
        }
        double[] p = new double[val.size()]; int[] y = new int[val.size()];
        for (int i = 0; i < p.length; i++) {
            double sc = val.get(i).score; double v = blocks.get(blocks.size() - 1)[0] / blocks.get(blocks.size() - 1)[1];
            for (double[] bl : blocks) if (sc <= bl[2]) { v = bl[0] / bl[1]; break; }
            p[i] = v; y[i] = val.get(i).hit;
        }
        return StatsR27.ece(p, y, p.length, 10);
    }
}
