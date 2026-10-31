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
        public final String issue, source; public final long frozenAt, openTime,ordinal; public final double score; public final int hit, position; public final boolean evaluationPresent, identityMatches;
        public Row(String issue, String source, long frozenAt, long openTime, double score, int hit, int position, boolean evaluationPresent, boolean identityMatches) {
            this(issue,source,frozenAt,openTime,score,hit,position,evaluationPresent,identityMatches,Long.MIN_VALUE);
        }
        public Row(String issue,String source,long frozenAt,long openTime,double score,int hit,int position,boolean evaluationPresent,boolean identityMatches,long ordinal){
            this.ordinal=ordinal;this.issue = issue; this.source = source; this.frozenAt = frozenAt; this.openTime = openTime; this.score = score; this.hit = hit; this.position = position; this.evaluationPresent = evaluationPresent; this.identityMatches = identityMatches;
        }
    }
    public static final class Result {
        public String state, reason, method = "PLATT"; public int fitRows, validationRows, excludedLate, excludedGap, identityDrift; public double a = Double.NaN, b = Double.NaN, ece = Double.NaN, eceIsotonic = Double.NaN;
        public int excludedInvalid,excludedConflicts,excludedDuplicates;
        public final Map<String,Object> quality=new LinkedHashMap<>();public final List<Evaluation> evaluations=new ArrayList<>();
        public final Map<String, Object> eceByStratum = new LinkedHashMap<>(); public String fitSource = "";
    }

    /** Legacy 120-row diagnostic API retained for compatibility; production uses fitRolling with explicit ordinals.
     * @param rows ordered by issue; source is "REPLAY" (H_SELECT walk-forward output) or "FUTURE". */
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

    public static final class Evaluation {
        public final long ordinal,fitThroughOrdinal;public final int position,hit,fitRows;public final double probability,supportLo,supportHi;public final boolean supported;public final String fitHash;
        Evaluation(Row row,long through,int count,double p,double lo,double hi,String hash){ordinal=row.ordinal;fitThroughOrdinal=through;position=row.position;hit=row.hit;fitRows=count;probability=p;supportLo=lo;supportHi=hi;supported=p>=lo&&p<=hi;fitHash=hash;}
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("ordinal",ordinal);m.put("fitThroughOrdinal",fitThroughOrdinal);m.put("fitRows",(long)fitRows);m.put("position",(long)position);m.put("p",probability);m.put("hit",(long)hit);m.put("supportLo",supportLo);m.put("supportHi",supportHi);m.put("supported",supported);m.put("fitHash",fitHash);return m;}
    }
    /** Production rolling Platt: every validation row uses only the most recent 120 earlier qualified rows,
     * with the original one-period embargo. Input order, duplicate rows, or later labels cannot move its cutoff. */
    public static Result fitRolling(List<Row> rows){
        Result r=new Result();r.method="PLATT";
        Map<String,Row> unique=new java.util.TreeMap<>();java.util.Set<String> conflicts=new java.util.HashSet<>();
        for(Row row:rows){
            if(!row.identityMatches){r.identityDrift++;continue;}
            if(!row.evaluationPresent||row.hit<0){r.excludedGap++;continue;}
            if(!Double.isFinite(row.score)||row.hit>1||row.position<1||row.position>10||row.ordinal==Long.MIN_VALUE||(!row.source.equals("REPLAY")&&!row.source.equals("FUTURE"))){r.excludedInvalid++;continue;}
            if(row.source.equals("FUTURE")&&(row.frozenAt<=0||row.openTime<=0||row.frozenAt>row.openTime-PitGuardR30.MIN_MARGIN_MS)){r.excludedLate++;continue;}
            String key=row.ordinal+":"+row.position;Row old=unique.get(key);
            // A live action can differ from a later replay's counterfactual K3. Prefer its original FUTURE receipt.
            if(old!=null&&!old.source.equals(row.source)){r.excludedDuplicates++;if(row.source.equals("FUTURE"))unique.put(key,row);continue;}
            if(old!=null&&(old.hit!=row.hit||Double.doubleToLongBits(old.score)!=Double.doubleToLongBits(row.score))){conflicts.add(key+":"+row.source);r.excludedConflicts++;continue;}
            if(old==null)unique.put(key,row);else r.excludedDuplicates++;
        }
        unique.entrySet().removeIf(e->conflicts.contains(e.getKey()+":"+e.getValue().source));
        List<Row> all=new ArrayList<>(unique.values());all.sort(java.util.Comparator.comparingLong((Row row)->row.ordinal).thenComparingInt(row->row.position));
        int totalFuture=0;for(Row row:all)if(row.source.equals("FUTURE"))totalFuture++;
        java.util.ArrayDeque<Row> prior=new java.util.ArrayDeque<>();java.util.Set<String> sources=new java.util.TreeSet<>();int cursor=0,futureSeen=0;
        for(int i=0;i<all.size();i++){
            Row row=all.get(i);if(!row.source.equals("FUTURE"))continue;
            if(futureSeen++<Math.max(0,totalFuture-5000))continue;
            while(cursor<i&&all.get(cursor).ordinal<row.ordinal-EMBARGO){prior.addLast(all.get(cursor++));while(prior.size()>FIT)prior.removeFirst();}
            r.fitRows=Math.max(r.fitRows,prior.size());if(prior.size()<FIT)continue;
            List<Row> fit=new ArrayList<>(prior);long through=fit.get(fit.size()-1).ordinal;
            if(through>=row.ordinal-EMBARGO)throw new IllegalStateException("R30_CALIBRATION_FUTURE_FIT");
            // Canonical accumulation preserves the same likelihood while identical histograms get identical
            // floating-point sums. Otherwise a constant 0.30 can oscillate across an ECE bin boundary.
            List<Row> estimatorRows=new ArrayList<>(fit);estimatorRows.sort(java.util.Comparator.comparingDouble((Row q)->q.score).thenComparingInt(q->q.hit));
            double[] ab=platt(estimatorRows);double p=sigmoid(ab[0]*row.score+ab[1]),lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;
            StringBuilder identity=new StringBuilder("R30_ROLLING_PLATT_120_EMBARGO1|").append(Double.toHexString(ab[0])).append('|').append(Double.toHexString(ab[1]));
            for(Row q:fit){sources.add(q.source);double fp=sigmoid(ab[0]*q.score+ab[1]);lo=Math.min(lo,fp);hi=Math.max(hi,fp);identity.append('|').append(q.ordinal).append(':').append(q.position).append(':').append(Double.toHexString(q.score)).append(':').append(q.hit);}
            r.a=ab[0];r.b=ab[1];r.evaluations.add(new Evaluation(row,through,fit.size(),p,lo,hi,StatsR27.sha256(identity.toString())));
        }
        r.validationRows=r.evaluations.size();r.fitSource=String.join("+",sources);double[] p=new double[r.validationRows];int[] y=new int[p.length];long supported=0;
        Map<Integer,List<Integer>> strata=new java.util.TreeMap<>();
        for(int i=0;i<p.length;i++){Evaluation e=r.evaluations.get(i);p[i]=e.probability;y[i]=e.hit;if(e.supported)supported++;strata.computeIfAbsent(e.position,k->new ArrayList<>()).add(i);}
        r.ece=p.length==0?Double.NaN:StatsR27.ece(p,y,p.length,10);
        for(Map.Entry<Integer,List<Integer>> stratum:strata.entrySet()){double[] pp=new double[stratum.getValue().size()];int[] yy=new int[pp.length];for(int i=0;i<pp.length;i++){int at=stratum.getValue().get(i);pp[i]=p[at];yy[i]=y[at];}r.eceByStratum.put("position"+stratum.getKey(),StatsR27.ece(pp,yy,pp.length,10));}
        double lo=p.length==0?Double.NaN:r.evaluations.get(0).supportLo,hi=p.length==0?Double.NaN:r.evaluations.get(0).supportHi;
        r.quality.putAll(CalibrationR30.evaluate(p,y,p.length,10,lo,hi));r.quality.put("eceEqualWidth",p.length==0?null:r.ece);
        Double coverage=p.length==0?null:supported/(double)p.length;
        r.quality.put("supportCoverage",coverage);r.quality.put("supportCoverageMethod","PER_ROW_PRIOR_FIT_PROBABILITY_RANGE");
        r.quality.put("q05Pass",p.length<CalibrationR30.MIN_SAMPLES?null:((Number)r.quality.get("eceEqualMass")).doubleValue()<=.04&&coverage>=.90);
        r.quality.put("fitRowsPerEvaluation",(long)FIT);r.quality.put("embargoPeriods",(long)EMBARGO);r.quality.put("temporalRowsChecked",(long)p.length);r.quality.put("temporalViolations",0L);
        r.quality.put("excludedLate",(long)r.excludedLate);r.quality.put("excludedGap",(long)r.excludedGap);r.quality.put("excludedInvalid",(long)r.excludedInvalid);r.quality.put("excludedConflicts",(long)r.excludedConflicts);r.quality.put("excludedDuplicates",(long)r.excludedDuplicates);r.quality.put("windowLimit",5000L);
        r.quality.put("measurementSource","PRIOR_ORDINAL_PLATT_ON_H_SELECT_AND_FUTURE_RECEIPTS");
        r.quality.put("status",p.length<CalibrationR30.MIN_SAMPLES?"INSUFFICIENT":"MEASURED");
        r.state=p.length>=VALIDATION?"FITTED":"COLLECTING";r.reason=p.length>=VALIDATION?"ROLLING_PLATT_PRIOR_ORDINAL_EMBARGO1_VALIDATED_FUTURE":INSUFFICIENT_ROWS+"(fit="+r.fitRows+"/120,validation="+p.length+"/120)";
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
