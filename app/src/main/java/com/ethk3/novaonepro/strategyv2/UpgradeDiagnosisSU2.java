package com.ethk3.novaonepro.strategyv2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * P50 upgrade diagnosis (检测报告 = 升级诊断书), the Java twin of reference/replay/diagnosis.py (byte-identical
 * report text on the r16_diag fixtures, UpgradeDiagnosisSU2Test).
 *
 *   snapshot text (bounded, <= 16 KB)  -> diagnose() -> findings + projections -> render() <= 64 KB, toJson() <= 256 KB
 *   Ledger.update(findings, seq)        -> issue ledger (<= 256 entries), generation curve (<= 512 points)
 *
 * The report answers, from the live records only: where the gap to 35% is, which capability is missing, what exactly to
 * do (module / parameter / project), what win rate / frequency / worst block / calibration the upgrade is expected to
 * reach (empirical-Bayes projection, KAPPA = 200 actions toward 30%, 90% interval, Bonferroni for "best of K"), and how
 * it is verified (a shadow challenger settled live, or the next campaign). Texts: DiagnosisTextSU2 (generated).
 * Android access lives in DiagnosisStoreSU2 so this class stays pure Java.
 */
public final class UpgradeDiagnosisSU2 {
    private UpgradeDiagnosisSU2() {}
    public static final String VERSION = "P50-1";
    static final double KAPPA = 200.0, PRIOR_HITS = 60.0, CHANCE = 0.30, TARGET = 0.35, Z90 = 1.6449;
    static final int WINDOW_BLOCKS = 40, MIN_BLOCK_ACTS = 30, MIN_PROBE_BLOCK = 100, MAX_S3 = 2;
    static final double MIN_GAIN = 0.05;
    public static final int LEDGER_CAP = 256, SNAP_KEEP = 8, CURVE_CAP = 512, CURVE_RECENT = 64, SNAP_MAX_BYTES = 16384;
    public static final int MD_MAX_BYTES = 65536, JSON_MAX_BYTES = 262144, MAX_FINDINGS_SHOWN = 24, MAX_LEDGER_SHOWN = 30;
    static final double[] ZK = {1.6449, 1.96, 2.128, 2.2414, 2.3263, 2.394, 2.45, 2.4977, 2.5392, 2.5758, 2.6086, 2.6383, 2.6653, 2.6901, 2.7131,
        2.7344, 2.7543, 2.7729, 2.7905, 2.807, 2.8227, 2.8376, 2.8518, 2.8653, 2.8782, 2.8905, 2.9024, 2.9137, 2.9247,
        2.9352, 2.9454, 2.9552, 2.9646, 2.9738, 2.9827, 2.9913, 2.9997, 3.0078, 3.0157, 3.0233, 3.0308, 3.0381, 3.0452,
        3.0521, 3.0588, 3.0654, 3.0718, 3.0781, 3.0842, 3.0902, 3.0961, 3.1019, 3.1075, 3.113, 3.1184, 3.1237, 3.1289,
        3.134, 3.1391, 3.144, 3.1488, 3.1536, 3.1582, 3.1628};
    static final Map<String, Integer> SEV_ORDER = new HashMap<>();
    static final Map<String, String> BETTER = new HashMap<>();
    static {
        SEV_ORDER.put("P0", 0); SEV_ORDER.put("P1", 1); SEV_ORDER.put("P2", 2); SEV_ORDER.put("INFO", 3);
        for (String c : new String[]{"G1", "G2", "G5", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "E1", "E3"}) BETTER.put(c, "high");
        for (String c : new String[]{"G3", "G4", "S9", "Q1", "Q2", "Q3", "Q4", "E2", "M1"}) BETTER.put(c, "low");
    }
    static final Map<String, String> T = DiagnosisTextSU2.T;

    // ------------------------------------------------------------------------------------------------ formatting
    static String fx(double x, int nd) {
        String s = new BigDecimal(x).setScale(nd, RoundingMode.HALF_EVEN).toPlainString();
        if (s.startsWith("-") && Double.parseDouble(s) == 0.0) s = s.substring(1);
        return s;
    }
    static String pct(Double x) { return x == null ? T.get("NA") : fx(x * 100.0, 1) + "%"; }
    static String pts(Double x) { if (x == null) return T.get("NA"); String s = fx(x * 100.0, 1); return s.startsWith("-") ? s : "+" + s; }
    static String num(Double x, int nd) { return x == null ? T.get("NA") : fx(x, nd); }
    static String snum(Double x, int nd) { if (x == null) return T.get("NA"); String s = fx(x, nd); return s.startsWith("-") ? s : "+" + s; }

    /** Single-pass replacement of {name} placeholders (names: a-z 0-9 _); values are never rescanned. */
    static String fillT(String t, Map<String, String> v) {
        StringBuilder b = new StringBuilder(); int i = 0, n = t.length();
        while (i < n) {
            char c = t.charAt(i);
            if (c == '{') {
                int j = i + 1; while (j < n && isName(t.charAt(j))) j++;
                if (j > i + 1 && j < n && t.charAt(j) == '}') {
                    String k = t.substring(i + 1, j), val = v.get(k);
                    if (val == null) throw new IllegalArgumentException("SU2_DIAG_PLACEHOLDER:" + k);
                    b.append(val); i = j + 1; continue;
                }
            }
            b.append(c); i++;
        }
        return b.toString();
    }
    static boolean isName(char c) { return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_'; }
    static String fill(String key, String... kv) {
        Map<String, String> m = new HashMap<>(); for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return fillT(T.get(key), m);
    }
    static String fillM(String key, Map<String, String> m) { return fillT(T.get(key), m); }

    // ------------------------------------------------------------------------------------------------ statistics
    static double[] shrink(int k, int n) {
        double m = (k + PRIOR_HITS) / (n + KAPPA);
        return new double[]{m, Math.sqrt(m * (1.0 - m) / (n + KAPPA + 1.0))};
    }
    static String projText(int k, int n) {
        double[] s = shrink(k, n);
        return pct(s[0]) + "（" + pct(s[0] - Z90 * s[1]) + "–" + pct(s[0] + Z90 * s[1]) + "）";
    }
    static Double wilson(int k, int n) {
        if (n <= 0) return null;
        double z = Z90, p = (double) k / n, z2 = z * z, denom = 1.0 + z2 / n, centre = p + z2 / (2.0 * n);
        double adj = z * Math.sqrt(p * (1.0 - p) / n + z2 / (4.0 * n * n));
        return (centre - adj) / denom;
    }
    static Integer need(Double r, double p0) {
        if (r == null || r <= p0) return null;
        double d = r - p0;
        return (int) Math.ceil(Z90 * Z90 * r * (1.0 - r) / (d * d));
    }
    static double zk(int k) { return ZK[Math.min(Math.max(k, 1), 64) - 1]; }
    static Double r9(Double x) { return x == null ? null : Double.parseDouble(fx(x, 9)); }

    // ------------------------------------------------------------------------------------------------ snapshot
    public static final class Track { int periods; final List<double[]> blocks = new ArrayList<>(); }      // {0,rate} or {1,a,h}
    public static final class ProbeBlock { int n, a15, h15, a30, h30; double llr, llr2, p15, p30; Double st0; }
    public static final class Stage { String cov; int k, use, nl, nc; Double vl, vc, fl, fc; }
    public static final class Gen { String campaign, tier; Double r0, r1; int a0, a1, g; }
    public static final class Snapshot {
        public String dataset = ""; public long seq;
        final LinkedHashMap<String, Track> tracks = new LinkedHashMap<>();
        long[] xishare; double pworse;
        final LinkedHashMap<String, List<ProbeBlock>> probes = new LinkedHashMap<>();
        final Map<String, String> params = new HashMap<>();
        int[] contmat; String contmatPatterns = "";
        final List<Stage> stages = new ArrayList<>();
        final List<Gen> gens = new ArrayList<>();
        final List<Object[]> ops = new ArrayList<>(), fams = new ArrayList<>();
        final List<String[]> atoms = new ArrayList<>();
        int[] repair;
        final List<Object[]> teacher = new ArrayList<>();
        final LinkedHashMap<String, String> formulas = new LinkedHashMap<>();
        final List<Object[]> storage = new ArrayList<>();
    }

    static Double f(String s) { return s.equals("-") ? null : Double.parseDouble(s); }

    public static Snapshot parse(String text) {
        Snapshot S = new Snapshot();
        for (String line : text.split("\n")) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1); String k = f[0];
            switch (k) {
                case "SU2DIAG": if (!f[1].equals("1")) throw new IllegalArgumentException("SU2_DIAG_VERSION"); break;
                case "dataset": S.dataset = f[1]; break;
                case "seq": S.seq = Long.parseLong(f[1]); break;
                case "track": {
                    Track t = new Track(); t.periods = Integer.parseInt(f[2]);
                    if (!f[3].isEmpty()) for (String b : f[3].split(",")) {
                        if (b.startsWith("~")) t.blocks.add(new double[]{0, Double.parseDouble(b.substring(1))});
                        else { String[] ah = b.split(":"); t.blocks.add(new double[]{1, Integer.parseInt(ah[0]), Integer.parseInt(ah[1])}); }
                    }
                    S.tracks.put(f[1], t); break;
                }
                case "xishare": S.xishare = new long[]{Long.parseLong(f[1]), Long.parseLong(f[2])}; S.pworse = Double.parseDouble(f[3]); break;
                case "probe": {
                    List<ProbeBlock> bl = new ArrayList<>();
                    if (!f[3].isEmpty()) for (String b : f[3].split(",")) {
                        String[] v = b.split(":"); ProbeBlock p = new ProbeBlock();
                        p.n = Integer.parseInt(v[0]); p.llr = Double.parseDouble(v[1]); p.llr2 = Double.parseDouble(v[2]);
                        p.a15 = Integer.parseInt(v[3]); p.h15 = Integer.parseInt(v[4]); p.p15 = Double.parseDouble(v[5]);
                        p.a30 = Integer.parseInt(v[6]); p.h30 = Integer.parseInt(v[7]); p.p30 = Double.parseDouble(v[8]); p.st0 = f(v[9]);
                        bl.add(p);
                    }
                    S.probes.put(f[1], new ArrayList<>(bl.subList(Math.max(0, bl.size() - WINDOW_BLOCKS), bl.size())));
                    S.params.put(f[1], f[2].equals("-") ? "" : f[2]); break;
                }
                case "contmat": S.contmat = new int[]{Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3])}; S.contmatPatterns = f.length > 4 ? f[4] : ""; break;
                case "stage": {
                    Stage s = new Stage(); s.cov = f[1]; s.k = Integer.parseInt(f[2]); s.use = Integer.parseInt(f[3]); s.vl = f(f[4]); s.vc = f(f[5]);
                    s.fl = f(f[6]); s.nl = Integer.parseInt(f[7]); s.fc = f(f[8]); s.nc = Integer.parseInt(f[9]); S.stages.add(s); break;
                }
                case "gen": {
                    Gen g = new Gen(); g.campaign = f[1]; g.tier = f[2]; g.r0 = f(f[3]); g.a0 = Integer.parseInt(f[4]); g.r1 = f(f[5]); g.a1 = Integer.parseInt(f[6]); g.g = Integer.parseInt(f[7]);
                    S.gens.add(g); break;
                }
                case "op": S.ops.add(new Object[]{f[1], Double.parseDouble(f[2])}); break;
                case "fam": S.fams.add(new Object[]{f[1], Double.parseDouble(f[2])}); break;
                case "atom": S.atoms.add(new String[]{f[1], f[2]}); break;
                case "repair": S.repair = new int[]{Integer.parseInt(f[1]), Integer.parseInt(f[2])}; break;
                case "teacher": S.teacher.add(new Object[]{f[1], f[2], f(f[3]), f(f[4])}); break;
                case "formula": S.formulas.put(f[1], f[2]); break;
                case "storage": S.storage.add(new Object[]{f[1], Long.parseLong(f[2]), Long.parseLong(f[3])}); break;
                default: throw new IllegalArgumentException("SU2_DIAG_LINE:" + k);
            }
        }
        return S;
    }

    static final class TrackSum { int acts, hits, nb; Double rate, cov, lcb, worst, best, range; }
    static TrackSum trackSum(Track tr) {
        TrackSum d = new TrackSum(); List<Double> rates = new ArrayList<>();
        for (double[] b : tr.blocks) {
            if (b[0] == 0) rates.add(b[1]);
            else { int a = (int) b[1], h = (int) b[2]; d.acts += a; d.hits += h; if (a >= MIN_BLOCK_ACTS) rates.add((double) h / a); }
        }
        d.rate = d.acts > 0 ? (double) d.hits / d.acts : null;
        d.cov = tr.periods > 0 ? (double) d.acts / tr.periods : null;
        d.lcb = d.acts > 0 ? wilson(d.hits, d.acts) : null;
        d.worst = rates.isEmpty() ? null : Collections.min(rates); d.best = rates.isEmpty() ? null : Collections.max(rates); d.nb = rates.size();
        d.range = rates.size() >= 2 ? d.best - d.worst : null;
        return d;
    }

    static final class ProbeSum {
        int n, a15, h15, a30, h30; Double gain, lo, hi, pos, st0;
        Double r15, pred15, cov15, worst15, pb15, r30, pred30, cov30, worst30, pb30;
    }
    static ProbeSum probeSum(List<ProbeBlock> bl) {
        ProbeSum d = new ProbeSum(); double llr = 0.0;
        for (ProbeBlock b : bl) { d.n += b.n; llr += b.llr; }
        d.gain = d.n > 0 ? 100.0 * llr / d.n : null;
        List<Double> bm = new ArrayList<>(); for (ProbeBlock b : bl) if (b.n >= MIN_PROBE_BLOCK) bm.add(100.0 * b.llr / b.n);
        int B = bm.size();
        if (B >= 2) {
            double mean = 0.0; for (double g : bm) mean += g; mean /= B;
            double var = 0.0; for (double g : bm) var += (g - mean) * (g - mean);
            double se = Math.sqrt(var / (B - 1)) / Math.sqrt(B);
            d.lo = d.gain - Z90 * se; d.hi = d.gain + Z90 * se;
        }
        if (B > 0) { int c = 0; for (double g : bm) if (g > 0) c++; d.pos = (double) c / B; }
        for (int tag = 0; tag < 2; tag++) {
            int a = 0, h = 0; double p = 0.0; List<Double> rates = new ArrayList<>();
            for (ProbeBlock b : bl) {
                int ba = tag == 0 ? b.a15 : b.a30, bh = tag == 0 ? b.h15 : b.h30;
                a += ba; h += bh; p += tag == 0 ? b.p15 : b.p30;
                if (ba >= MIN_BLOCK_ACTS) rates.add((double) bh / ba);
            }
            Double r = a > 0 ? (double) h / a : null, pred = a > 0 ? p / a : null, cov = d.n > 0 ? (double) a / d.n : null;
            Double worst = rates.isEmpty() ? null : Collections.min(rates), pb = null;
            if (!rates.isEmpty()) { int c = 0; for (double x : rates) if (x > CHANCE) c++; pb = (double) c / rates.size(); }
            if (tag == 0) { d.a15 = a; d.h15 = h; d.r15 = r; d.pred15 = pred; d.cov15 = cov; d.worst15 = worst; d.pb15 = pb; }
            else { d.a30 = a; d.h30 = h; d.r30 = r; d.pred30 = pred; d.cov30 = cov; d.worst30 = worst; d.pb30 = pb; }
        }
        boolean all = !bl.isEmpty() && d.n > 0; double st = 0;
        for (ProbeBlock b : bl) { if (b.st0 == null) { all = false; break; } st += b.st0; }
        d.st0 = all ? st / d.n : null;
        return d;
    }

    // ------------------------------------------------------------------------------------------------ diagnosis
    public static final class Finding {
        public final String code, sev, scope, presc; public final Double value; final Map<String, String> v;
        Finding(String code, String sev, String scope, Double value, String presc, Map<String, String> v) { this.code = code; this.sev = sev; this.scope = scope; this.value = value; this.presc = presc; this.v = v; }
    }
    static final class Cand { String name; int k, n; Double cov, worst; boolean c30; double m, sd, adjlo; }

    public static final class Diagnosis {
        public Snapshot S; final LinkedHashMap<String, TrackSum> tracks = new LinkedHashMap<>(); final LinkedHashMap<String, ProbeSum> probes = new LinkedHashMap<>();
        final List<Cand> cands = new ArrayList<>(); Cand best; public String verdict;
        final List<String> informative = new ArrayList<>(), hint = new ArrayList<>();
        public final List<Finding> findings = new ArrayList<>();
        double[] genAll;
    }

    static Map<String, String> kv(String... a) { Map<String, String> m = new HashMap<>(); for (int i = 0; i < a.length; i += 2) m.put(a[i], a[i + 1]); return m; }

    public static Diagnosis diagnose(Snapshot S) {
        Diagnosis D = new Diagnosis(); D.S = S;
        for (Map.Entry<String, Track> e : S.tracks.entrySet()) D.tracks.put(e.getKey(), trackSum(e.getValue()));
        for (Map.Entry<String, List<ProbeBlock>> e : S.probes.entrySet()) D.probes.put(e.getKey(), probeSum(e.getValue()));
        Map<String, TrackSum> TR = D.tracks; Map<String, ProbeSum> PR = D.probes;
        TrackSum main = TR.get("MAINLINE"), xi = TR.get("XI");
        String NA = T.get("NA");
        List<Cand> cands = new ArrayList<>();
        for (String t : new String[]{"F15", "F30", "F100", "XI"}) {
            TrackSum s = TR.get(t); if (s == null || s.acts == 0) continue;
            Cand c = new Cand(); c.name = t.equals("XI") ? "XI" : "冠军 " + t; c.k = s.hits; c.n = s.acts; c.cov = s.cov; c.worst = s.worst; c.c30 = s.cov != null && s.cov >= 0.30; cands.add(c);
        }
        for (Map.Entry<String, ProbeSum> e : PR.entrySet()) {
            ProbeSum p = e.getValue();
            for (int tag = 0; tag < 2; tag++) {
                int a = tag == 0 ? p.a15 : p.a30; if (a == 0) continue;
                Cand c = new Cand(); c.name = "探针 " + e.getKey() + " " + (tag == 0 ? "15" : "30") + "% 门"; c.k = tag == 0 ? p.h15 : p.h30; c.n = a;
                c.cov = tag == 0 ? p.cov15 : p.cov30; c.worst = tag == 0 ? p.worst15 : p.worst30; c.c30 = c.cov >= 0.30; cands.add(c);
            }
        }
        for (Stage st : S.stages) if (st.nl > 0 && st.fl != null) {
            Cand c = new Cand(); c.name = "阶段层 " + st.cov + " 档"; c.k = (int) Math.rint(st.fl * st.nl); c.n = st.nl; c.cov = Double.parseDouble(st.cov); c.c30 = c.cov >= 0.30; cands.add(c);
        }
        for (Cand c : cands) if (c.cov != null && c.cov >= 0.15) D.cands.add(c);
        Cand best = null;
        for (Cand c : D.cands) { double[] s = shrink(c.k, c.n); c.m = s[0]; c.sd = s[1]; if (best == null || c.m > best.m) best = c; }
        D.best = best;
        if (best != null) {
            best.adjlo = best.m - zk(D.cands.size()) * best.sd;
            D.verdict = best.adjlo > TARGET ? "A" : best.adjlo > CHANCE ? "B" : best.m - Z90 * best.sd > CHANCE ? "C" : "D";
        }
        List<Finding> F = D.findings;
        String bestProj = best != null ? projText(best.k, best.n) : NA, bestName = best != null ? best.name : NA;
        // ---- G
        if (main != null && main.acts > 0) {
            if (main.lcb < TARGET)
                F.add(new Finding("G1", main.rate < TARGET ? "P0" : "P1", "", main.rate, "", kv("rate", pct(main.rate), "hits", String.valueOf(main.hits), "acts", String.valueOf(main.acts),
                    "lcb", pct(main.lcb), "gap", gapPhrase(main.rate), "proj", bestProj, "best", bestName)));
            if (main.cov < 0.30) {
                Cand b30 = null; for (Cand c : D.cands) if (c.c30 && (b30 == null || c.m > b30.m)) b30 = c;
                F.add(new Finding("G2", main.cov < 0.15 ? "P0" : "P2", "", main.cov, "TIER_COVERAGE F15 0.30", kv("cov", pct(main.cov),
                    "proj", b30 != null ? projText(b30.k, b30.n) : NA, "best", b30 != null ? b30.name : NA, "cov30", b30 != null ? pct(b30.cov) : NA)));
            }
            if (main.range != null && (main.worst < 0.28 || main.range > 0.12)) {
                Cand stable = null; for (Cand c : D.cands) if (c.worst != null && (stable == null || c.worst > stable.worst)) stable = c;
                F.add(new Finding("G3", "P1", "", main.range, "STABILITY_PENALTY x1.5", kv("worst", pct(main.worst), "best", pct(main.best), "range", fx(main.range * 100.0, 1),
                    "nb", String.valueOf(main.nb), "stable", stable != null ? stable.name : NA, "sworst", stable != null ? pct(stable.worst) : NA)));
            }
        }
        if (S.xishare != null && xi != null && xi.acts > 0) {
            long on = S.xishare[0], tot = S.xishare[1]; double pw = S.pworse;
            if (pw < 0.05) F.add(new Finding("G5", "P0", "", pw, "", kv("pworse", fx(pw, 4), "xirate", pct(xi.rate))));
            else if (tot > 0 && (double) on / tot >= 0.95) {
                String bt = null;
                for (String t : new String[]{"F15", "F30"}) { TrackSum s = TR.get(t); if (s != null && s.acts > 0 && (bt == null || s.rate > TR.get(bt).rate)) bt = t; }
                if (bt != null && TR.get(bt).rate > xi.rate) {
                    Integer nd = need(TR.get(bt).rate, xi.rate);
                    F.add(new Finding("G4", "P2", "", (double) on / tot, "", kv("share", pct((double) on / tot), "tier", bt, "rate", pct(TR.get(bt).rate),
                        "acts", String.valueOf(TR.get(bt).acts), "xirate", pct(xi.rate), "need", nd != null ? String.valueOf(nd) : NA)));
                }
            }
        }
        // ---- S
        double kz = PR.isEmpty() ? Z90 : zk(2 * PR.size());
        for (Map.Entry<String, ProbeSum> e : PR.entrySet()) {
            ProbeSum p = e.getValue();
            if (p.lo != null && p.lo > 0 && p.gain >= MIN_GAIN) D.informative.add(e.getKey());
            else if (p.a15 >= 100 && p.pb15 != null && p.pb15 >= 0.6) { double[] s = shrink(p.h15, p.a15); if (s[0] - kz * s[1] > CHANCE) D.hint.add(e.getKey()); }
        }
        int[] cm = S.contmat;
        Integer stageK = S.stages.isEmpty() ? null : S.stages.get(0).k;
        if (!PR.isEmpty() && D.informative.isEmpty() && D.hint.isEmpty()) {
            String bg = bestGainProbe(PR), br = null;
            for (Map.Entry<String, ProbeSum> e : PR.entrySet()) { ProbeSum p = e.getValue(); if (p.a15 > 0 && (br == null || shrink(p.h15, p.a15)[0] > shrink(PR.get(br).h15, PR.get(br).a15)[0])) br = e.getKey(); }
            if (bg != null) {
                ProbeSum pg = PR.get(bg);
                F.add(new Finding("S1", "P0", "", pg.lo != null ? pg.lo : pg.gain, "", kv("n", String.valueOf(PR.size()), "fam", bg, "gain", snum(pg.gain, 3), "lo", snum(pg.lo, 3), "hi", snum(pg.hi, 3),
                    "rfam", br != null ? br : NA, "rate", br != null ? pct(PR.get(br).r15) : NA, "proj", br != null ? projText(PR.get(br).h15, PR.get(br).a15) : NA,
                    "cm", cm != null ? String.valueOf(cm[1]) : NA, "k", stageK != null ? String.valueOf(stageK) : NA)));
            }
        }
        for (String fam : PR.keySet()) {
            if (!D.informative.contains(fam) && !D.hint.contains(fam)) continue;
            ProbeSum p = PR.get(fam); double[] s = shrink(p.h15, p.a15);
            if (main == null || main.acts == 0 || s[0] > main.rate + 0.01) {
                String member = fam + "{" + S.params.getOrDefault(fam, "") + "}";
                F.add(new Finding("S2", "P0", fam, s[0], "ADD_MEMBER " + member, kv("fam", fam, "gain", snum(p.gain, 3), "lo", snum(p.lo, 3), "hi", snum(p.hi, 3),
                    "rate", pct(p.r15), "hits", String.valueOf(p.h15), "acts", String.valueOf(p.a15), "pb", pct(p.pb15), "adjlo", pct(s[0] - kz * s[1]),
                    "proj", projText(p.h15, p.a15), "main", main != null && main.acts > 0 ? pct(main.rate) : NA, "member", member, "worst", pct(p.worst15),
                    "cal", p.a15 > 0 ? pts(p.pred15 - p.r15) : NA)));
            }
        }
        List<String> s3 = new ArrayList<>();
        for (Map.Entry<String, ProbeSum> e : PR.entrySet()) {
            String fam = e.getKey(); ProbeSum p = e.getValue();
            if (D.informative.contains(fam) || D.hint.contains(fam) || p.gain == null || p.pos == null || p.gain < MIN_GAIN) continue;
            int nb = 0; for (ProbeBlock b : S.probes.get(fam)) if (b.n >= MIN_PROBE_BLOCK) nb++;
            if (nb >= 4 && 0.35 <= p.pos && p.pos <= 0.75) s3.add(fam);
        }
        s3.sort((a, b) -> Double.compare(-PR.get(a).gain, -PR.get(b).gain));
        for (String fam : s3.subList(0, Math.min(MAX_S3, s3.size()))) {
            ProbeSum p = PR.get(fam);
            F.add(new Finding("S3", "P1", fam, p.pos, "REGMEM_GATE " + fam, kv("fam", fam, "gain", snum(p.gain, 3), "pos", pct(p.pos), "proj", p.a15 > 0 ? projText(p.h15, p.a15) : NA)));
        }
        if (cm != null) {
            if (cm[2] == 0 && cm[0] >= 4) F.add(new Finding("S4", "P2", "", 0.0, "", kv("refits", String.valueOf(cm[0]))));
            else if (cm[1] > 0) {
                ProbeSum pc = PR.get("CONTMAT");
                F.add(new Finding("S5", "P1", "", (double) cm[1], "ADD_MEMBER CONTMAT{}", kv("n", String.valueOf(cm[1]), "patterns", S.contmatPatterns.isEmpty() ? NA : S.contmatPatterns,
                    "proj", pc != null && pc.a15 > 0 ? projText(pc.h15, pc.a15) : NA)));
            }
        }
        if (!S.stages.isEmpty()) {
            if (stageK != null && stageK <= 1) F.add(new Finding("S6", "P2", "", (double) stageK, "", kv("k", String.valueOf(stageK))));
            for (Stage st : S.stages) {
                if (st.use != 0 && st.fl != null && st.fc != null && st.nl >= MIN_BLOCK_ACTS) {
                    Map<String, String> v = kv("cov", st.cov, "vl", pct(st.vl), "vc", pct(st.vc), "fl", pct(st.fl), "nl", String.valueOf(st.nl), "fc", pct(st.fc), "nc", String.valueOf(st.nc));
                    if (st.fl < st.fc) F.add(new Finding("S7", "P1", st.cov, st.fc - st.fl, "STAGE_GUARD lead>=0.02 n>=150", v));
                    else if (st.fl > st.fc) { v.put("proj", projText((int) Math.rint(st.fl * st.nl), st.nl)); F.add(new Finding("S8", "INFO", st.cov, st.fl, "", v)); }
                }
            }
        }
        ProbeSum reg = PR.get("REGMEM");
        if (reg != null && reg.st0 != null && reg.st0 > 0.9) F.add(new Finding("S9", "P2", "", reg.st0, "", kv("st0", fx(reg.st0, 3))));
        // ---- Q
        for (Map.Entry<String, ProbeSum> e : PR.entrySet()) {
            ProbeSum p = e.getValue(); String fam = e.getKey();
            if (p.a15 >= 200 && p.r15 != null && 0.0 < p.r15 && p.r15 < 1.0) {
                double gap = p.pred15 - p.r15, z = (p.pred15 * p.a15 - p.h15) / Math.sqrt(p.a15 * p.r15 * (1.0 - p.r15));
                if (Math.abs(gap) > 0.03 && Math.abs(z) > 2.0)
                    F.add(new Finding("Q1", "P1", fam, Math.abs(gap), "CAL_LIMIT " + fam + " beta<=1.5 window=1250", kv("fam", fam, "pred", pct(p.pred15), "rate", pct(p.r15),
                        "acts", String.valueOf(p.a15), "gap", pts(gap), "z", fx(z, 2))));
            }
        }
        for (Stage st : S.stages)
            if (st.vc != null && st.fc != null && st.nc >= 60 && st.vc - st.fc > 0.03)
                F.add(new Finding("Q2", "P1", st.cov, st.vc - st.fc, "JUDGE_KAPPA x2", kv("tier", st.cov + " 委员会", "val", pct(st.vc), "fwd", pct(st.fc), "n", String.valueOf(st.nc),
                    "d", fx((st.vc - st.fc) * 100.0, 1), "proj", projText((int) Math.rint(st.fc * st.nc), st.nc))));
        TrackSum f15 = TR.get("F15"), f30 = TR.get("F30");
        if (f15 != null && f30 != null && f15.acts >= 100 && f30.acts >= 100 && f15.rate < f30.rate - 0.01)
            F.add(new Finding("Q3", "P1", "", f30.rate - f15.rate, "F15_GATE STAGE", kv("r15", pct(f15.rate), "n15", String.valueOf(f15.acts), "r30", pct(f30.rate),
                "n30", String.valueOf(f30.acts), "proj", projText(f30.hits, f30.acts))));
        if (S.atoms.size() >= 4) {
            List<String> e = new ArrayList<>(); for (String[] a : S.atoms) if (a[1].equals("E_FALSE")) e.add(a[0]);
            if ((double) e.size() / S.atoms.size() >= 0.3)
                F.add(new Finding("Q4", "P2", "", (double) e.size() / S.atoms.size(), "ATOM_PRIOR E*0.5 AB*1.5", kv("e", String.valueOf(e.size()), "tot", String.valueOf(S.atoms.size()),
                    "names", String.join("、", e.subList(0, Math.min(6, e.size()))))));
        }
        // ---- E
        for (String tier : new String[]{"F15", "F30"}) {
            List<Double> ds = new ArrayList<>(); for (Gen g : S.gens) if (g.tier.equals(tier) && genOk(g)) ds.add(g.r1 - g.r0);
            if (ds.isEmpty()) continue;
            double[] mlh = meanInterval(ds);
            if (mlh[0] <= 0) F.add(new Finding("E1", "P1", tier, mlh[0], "OPERATOR_MIX", kv("tier", tier, "n", String.valueOf(ds.size()), "mean", pts(mlh[0]),
                "lo", pts(ds.size() >= 2 ? mlh[1] : null), "hi", pts(ds.size() >= 2 ? mlh[2] : null))));
        }
        List<String> zero = new ArrayList<>(); for (Object[] o : S.ops) if ((Double) o[1] == 0.0) zero.add((String) o[0]);
        if (!zero.isEmpty()) F.add(new Finding("E2", "P2", "", (double) zero.size(), "OPERATOR_MIX", kv("ops", String.join("、", zero))));
        if (S.repair != null && S.repair[0] >= 10 && (double) S.repair[1] / S.repair[0] < 0.4) {
            int n = S.repair[0], k = S.repair[1];
            F.add(new Finding("E3", "P2", "", (double) k / n, "REPAIR_SIGNIFICANT_ONLY", kv("n", String.valueOf(n), "k", String.valueOf(k), "share", pct((double) k / n))));
        }
        for (Object[] st : S.storage) {
            long b = (Long) st[1], cap = (Long) st[2];
            if (b > cap) F.add(new Finding("M1", "P0", (String) st[0], (double) b / cap, "", kv("cat", (String) st[0], "bytes", mb(b), "cap", mb(cap))));
        }
        F.sort((a, b) -> Integer.compare(SEV_ORDER.get(a.sev), SEV_ORDER.get(b.sev)));     // stable
        return D;
    }

    /** {mean, lo, hi} with a 90% interval (lo/hi only meaningful for n >= 2). */
    static double[] meanInterval(List<Double> ds) {
        double mean = 0.0; for (double x : ds) mean += x; mean /= ds.size();
        if (ds.size() < 2) return new double[]{mean, Double.NaN, Double.NaN};
        double var = 0.0; for (double x : ds) var += (x - mean) * (x - mean);
        double se = Math.sqrt(var / (ds.size() - 1)) / Math.sqrt(ds.size());
        return new double[]{mean, mean - Z90 * se, mean + Z90 * se};
    }

    static String bestGainProbe(Map<String, ProbeSum> PR) {
        String bg = null;
        for (int pass = 0; pass < 2; pass++) {
            for (Map.Entry<String, ProbeSum> e : PR.entrySet()) {
                ProbeSum p = e.getValue();
                if (p.gain == null || (pass == 0 && p.a15 + p.a30 == 0)) continue;
                if (bg == null || p.gain > PR.get(bg).gain) bg = e.getKey();
            }
            if (bg != null) return bg;
        }
        return bg;
    }
    static boolean genOk(Gen g) { return g.r0 != null && g.r1 != null && g.a0 >= MIN_BLOCK_ACTS && g.a1 >= MIN_BLOCK_ACTS; }
    static String gapPhrase(double rate) { return fill(rate < TARGET ? "S0.gap.below" : "S0.gap.above", "gap", fx(Math.abs(TARGET - rate) * 100.0, 1)); }
    static String mb(long b) { return fx(b / 1048576.0, 1) + " MB"; }
    static String scopeS(String s) { return s.isEmpty() ? "" : "·" + s; }

    // ------------------------------------------------------------------------------------------------ ledger
    public static final class Entry {
        public String code, scope, sev, status, presc = "", outcome = ""; public long first, last, since = -1; public int seen; public Double value, best;
        public long sacts, shits, bacts, bhits;
        String key() { return code + "|" + scope; }
    }

    /** Issue ledger (<= 256 entries), last 8 snapshots, generation curve (<= 512 points). Bounded by construction. */
    public static final class Ledger {
        public final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();
        public final List<Object[]> snaps = new ArrayList<>();          // {seq, text}
        public final List<double[]> curve = new ArrayList<>();          // {seq, weight, sum_r0, sum_r1}

        public void update(List<Finding> findings, long seq) {
            Set<String> present = new HashSet<>();
            for (Finding f : findings) {
                String k = f.code + "|" + f.scope; present.add(k);
                String better = BETTER.get(f.code); Double v = r9(f.value);
                Entry e = entries.get(k);
                if (e == null) {
                    e = new Entry(); e.code = f.code; e.scope = f.scope; e.first = seq; e.last = seq; e.sev = f.sev; e.status = "NEW"; e.value = v; e.best = v;
                    entries.put(k, e);
                } else {
                    if (e.status.equals("RESOLVED")) e.status = "REGRESSED";
                    else if (improved(e.value, v, better)) e.status = "IMPROVING";
                    else e.status = "OPEN";
                    e.best = betterOf(e.best, v, better); e.value = v;
                }
                if (!f.presc.equals(e.presc)) {
                    e.presc = f.presc; e.since = f.presc.isEmpty() ? -1 : seq;
                    e.sacts = e.shits = e.bacts = e.bhits = 0;
                    e.outcome = f.presc.isEmpty() ? "" : prescKind(f.presc);
                }
                e.last = seq; e.seen++; e.sev = f.sev;
            }
            for (Entry e : entries.values()) if (!present.contains(e.key()) && !e.status.equals("RESOLVED")) e.status = "RESOLVED";
            while (entries.size() > LEDGER_CAP) evict();
        }

        void evict() {
            String k = pick(0); if (k == null) k = pick(1); if (k == null) k = pick(2); if (k == null) k = pick(3);
            entries.remove(k);
        }
        private String pick(int rule) {
            String best = null;
            for (Map.Entry<String, Entry> me : entries.entrySet()) {
                Entry e = me.getValue();
                boolean ok = rule == 0 ? e.status.equals("RESOLVED") : rule == 1 ? (e.sev.equals("INFO") || e.sev.equals("P2")) : rule == 2 ? e.sev.equals("P1") : true;
                if (!ok) continue;
                if (best == null) { best = me.getKey(); continue; }
                Entry b = entries.get(best);
                if (e.last < b.last || (e.last == b.last && me.getKey().compareTo(best) < 0)) best = me.getKey();
            }
            return best;
        }

        /** Live settlement of a prescribed shadow challenger and its base on the same periods. */
        public void shadow(String key, long sa, long sh, long ba, long bh) {
            Entry e = entries.get(key); if (e == null) return;
            e.sacts += sa; e.shits += sh; e.bacts += ba; e.bhits += bh;
            e.outcome = shadowOutcome(e.sacts, e.shits, e.bacts, e.bhits);
        }

        public void addSnapshot(long seq, String text) {
            byte[] b = text.getBytes(StandardCharsets.UTF_8);
            if (b.length > SNAP_MAX_BYTES) { int nl = lastNewline(b, SNAP_MAX_BYTES); text = new String(b, 0, nl + 1, StandardCharsets.UTF_8); }
            snaps.add(new Object[]{seq, text});
            while (snaps.size() > SNAP_KEEP) snaps.remove(0);
        }

        public void addCampaign(long seq, double r0, double r1) {
            curve.add(new double[]{seq, 1, r9(r0), r9(r1)});
            while (curve.size() > CURVE_CAP) {
                int bi = 0;
                for (int i = 1; i < curve.size(); i++) if (curve.get(i)[3] / curve.get(i)[1] > curve.get(bi)[3] / curve.get(bi)[1]) bi = i;
                int ci = -1; double cw = 0;
                for (int i = 1; i < curve.size() - CURVE_RECENT - 1; i++) {
                    if (i == bi || i + 1 == bi) continue;
                    double w = curve.get(i)[1] + curve.get(i + 1)[1];
                    if (ci < 0 || w < cw) { ci = i; cw = w; }
                }
                double[] a = curve.get(ci), b = curve.get(ci + 1);
                curve.set(ci, new double[]{b[0], a[1] + b[1], r9(a[2] + b[2]), r9(a[3] + b[3])});
                curve.remove(ci + 1);
            }
        }

        public String text() {
            StringBuilder o = new StringBuilder("SU2LEDGER\t1");
            for (Entry e : entries.values())
                o.append("\nE\t").append(e.code).append('\t').append(e.scope).append('\t').append(e.first).append('\t').append(e.last).append('\t').append(e.seen)
                 .append('\t').append(e.sev).append('\t').append(e.status).append('\t').append(e.value == null ? "-" : fx(e.value, 9)).append('\t')
                 .append(e.best == null ? "-" : fx(e.best, 9)).append('\t').append(e.presc).append('\t').append(e.since).append('\t').append(e.sacts).append('\t')
                 .append(e.shits).append('\t').append(e.bacts).append('\t').append(e.bhits).append('\t').append(e.outcome);
            for (Object[] s : snaps) o.append("\nN\t").append((long) (Long) s[0]).append('\t').append(((String) s[1]).getBytes(StandardCharsets.UTF_8).length);
            for (double[] p : curve) o.append("\nC\t").append((long) p[0]).append('\t').append((long) p[1]).append('\t').append(fx(p[2], 9)).append('\t').append(fx(p[3], 9));
            return o.append('\n').toString();
        }

        public static Ledger fromText(String text, List<Object[]> snaps) {
            Ledger L = new Ledger();
            for (String line : text.split("\n")) {
                String[] f = line.split("\t", -1);
                if (f[0].equals("E")) {
                    Entry e = new Entry(); e.code = f[1]; e.scope = f[2]; e.first = Long.parseLong(f[3]); e.last = Long.parseLong(f[4]); e.seen = Integer.parseInt(f[5]);
                    e.sev = f[6]; e.status = f[7]; e.value = f(f[8]); e.best = f(f[9]); e.presc = f[10]; e.since = Long.parseLong(f[11]);
                    e.sacts = Long.parseLong(f[12]); e.shits = Long.parseLong(f[13]); e.bacts = Long.parseLong(f[14]); e.bhits = Long.parseLong(f[15]); e.outcome = f[16];
                    L.entries.put(e.key(), e);
                } else if (f[0].equals("C")) L.curve.add(new double[]{Long.parseLong(f[1]), Long.parseLong(f[2]), Double.parseDouble(f[3]), Double.parseDouble(f[4])});
            }
            if (snaps != null) L.snaps.addAll(snaps);
            return L;
        }
    }

    static int lastNewline(byte[] b, int limit) { for (int i = Math.min(limit, b.length) - 1; i >= 0; i--) if (b[i] == '\n') return i; return -1; }

    static boolean improved(Double prev, Double cur, String better) {
        if (prev == null || cur == null) return false;
        return better.equals("high") ? cur > prev + 0.005 : cur < prev - 0.005;
    }
    static Double betterOf(Double a, Double b, String better) {
        if (a == null) return b; if (b == null) return a;
        return better.equals("high") ? Math.max(a, b) : Math.min(a, b);
    }
    /** SHADOW prescriptions become a live shadow challenger (PENDING until 600 actions); AUTO ones are executed by the
     *  evolution engine already (operator / atom-prior weights follow measured success); the rest are MANUAL parameters. */
    public static String prescKind(String presc) {
        String h = presc.split(" ")[0];
        return h.equals("ADD_MEMBER") || h.equals("TIER_COVERAGE") ? "PENDING" : h.equals("OPERATOR_MIX") || h.equals("ATOM_PRIOR") ? "AUTO" : "MANUAL";
    }
    public static String shadowOutcome(long sa, long sh, long ba, long bh) {
        if (sa < 600 || ba < 1) return "PENDING";
        double rs = (double) sh / sa, rb = (double) bh / ba, p = (double) (sh + bh) / (sa + ba);
        double se = Math.sqrt(p * (1.0 - p) * (1.0 / sa + 1.0 / ba)), z = se > 0 ? (rs - rb) / se : 0.0;
        return z > Z90 ? "EFFECTIVE" : z < -Z90 ? "HARMFUL" : "NO_EFFECT";
    }
    static final Map<String, String> OUTCOME = new HashMap<>();
    static {
        OUTCOME.put("", "—"); OUTCOME.put("PENDING", "影子结算中（满 600 次判定）"); OUTCOME.put("EFFECTIVE", "有效"); OUTCOME.put("NO_EFFECT", "无显著效果");
        OUTCOME.put("HARMFUL", "有害（已撤回）"); OUTCOME.put("AUTO", "引擎已自动执行（按成效配比）"); OUTCOME.put("MANUAL", "参数处方：下一轮施工执行");
    }

    // ------------------------------------------------------------------------------------------------ rendering
    public static String render(Diagnosis D, Ledger L) {
        Snapshot S = D.S; Map<String, TrackSum> TR = D.tracks; Map<String, ProbeSum> PR = D.probes; List<Finding> F = D.findings; String NA = T.get("NA");
        List<String> o = new ArrayList<>();
        o.add(fill("H.title", "dataset", S.dataset, "seq", String.valueOf(S.seq))); o.add(""); o.add(fill("H.rule", "version", VERSION)); o.add("");
        o.add(T.get("S0.h"));
        TrackSum main = TR.get("MAINLINE"), xi = TR.get("XI");
        if (main != null && main.acts > 0) {
            Integer n30 = need(main.rate, CHANCE), n35 = need(main.rate, TARGET);
            String nd = n30 != null ? fill("S0.need", "n30", String.valueOf(n30), "n35", n35 != null ? fill("S0.need35", "n35", String.valueOf(n35)) : "") : T.get("S0.needno");
            o.add(fill("S0.mainline", "rate", pct(main.rate), "hits", String.valueOf(main.hits), "acts", String.valueOf(main.acts), "cov", pct(main.cov),
                "lcb", pct(main.lcb), "worst", pct(main.worst), "gap", gapPhrase(main.rate), "need", nd));
        } else o.add(T.get("S0.nomain"));
        if (xi != null && xi.acts > 0)
            o.add(fill("S0.xi", "rate", pct(xi.rate), "hits", String.valueOf(xi.hits), "acts", String.valueOf(xi.acts), "cov", pct(xi.cov),
                "share", S.xishare != null && S.xishare[1] > 0 ? pct((double) S.xishare[0] / S.xishare[1]) : NA, "pworse", S.xishare != null ? fx(S.pworse, 4) : NA));
        Cand b = D.best;
        if (b != null) {
            o.add(fill("S0.best", "best", b.name, "proj", projText(b.k, b.n), "cov", pct(b.cov), "worst", pct(b.worst), "k", String.valueOf(D.cands.size()), "adjlo", pct(b.adjlo)));
            switch (D.verdict) {
                case "A": o.add(T.get("S0.verdict.A")); break;
                case "B": o.add(fill("S0.verdict.B", "best", b.name, "gap35", fx((TARGET - b.m) * 100.0, 1))); break;
                case "C": { Integer nd = need((double) b.k / b.n, CHANCE); o.add(fill("S0.verdict.C", "need", nd != null ? String.valueOf(nd) : NA)); break; }
                default: o.add(fill("S0.verdict.D", "proj", projText(b.k, b.n)));
            }
        }
        List<Finding> top = new ArrayList<>(); for (Finding f : F) if (!f.sev.equals("INFO") && top.size() < 3) top.add(f);
        if (!top.isEmpty()) {
            o.add(T.get("S0.top"));
            for (int i = 0; i < top.size(); i++) { Finding f = top.get(i); o.add(fill("S0.topi", "i", String.valueOf(i + 1), "sev", f.sev, "code", f.code, "scope", scopeS(f.scope), "title", T.get("F." + f.code + ".title"))); }
        }
        o.add("");
        // 1 creation
        o.add(T.get("S1.h")); o.add(""); o.add(T.get("S1.gen.h"));
        if (!S.gens.isEmpty()) {
            o.add(T.get("S1.gen.th"));
            for (Gen g : S.gens) o.add(fill("S1.gen.row", "campaign", g.campaign, "tier", g.tier, "r0", pct(g.r0), "a0", String.valueOf(g.a0), "r1", pct(g.r1), "a1", String.valueOf(g.a1),
                "d", genOk(g) ? pts(g.r1 - g.r0) : NA, "g", String.valueOf(g.g)));
            List<Double> ds = new ArrayList<>(); for (Gen g : S.gens) if (genOk(g)) ds.add(g.r1 - g.r0);
            if (!ds.isEmpty()) {
                double[] mlh = meanInterval(ds); Double lo = ds.size() >= 2 ? mlh[1] : null, hi = ds.size() >= 2 ? mlh[2] : null;
                int up = 0, down = 0; for (double x : ds) { if (x > 0) up++; if (x < 0) down++; }
                String verdict = lo == null ? T.get("S1.gen.few") : lo > 0 ? T.get("S1.gen.up") : T.get("S1.gen.flat");
                o.add(""); o.add(fill("S1.gen.sum", "n", String.valueOf(ds.size()), "mean", pts(mlh[0]), "lo", pts(lo), "hi", pts(hi), "up", String.valueOf(up), "down", String.valueOf(down), "verdict", verdict));
                D.genAll = new double[]{mlh[0]};
            } else o.add(T.get("S1.gen.none"));
        } else o.add(T.get("S1.gen.none"));
        o.add(""); o.add(T.get("S1.op.h"));
        if (!S.ops.isEmpty()) {
            double tot = 0.0; for (Object[] op : S.ops) tot += (Double) op[1] + 0.05;
            o.add(T.get("S1.op.th")); Object[] bo = S.ops.get(0);
            for (Object[] op : S.ops) { if ((Double) op[1] > (Double) bo[1]) bo = op; o.add(fill("S1.op.row", "op", (String) op[0], "s", pct((Double) op[1]), "w", pct(((Double) op[1] + 0.05) / tot))); }
            o.add(""); o.add(fill("S1.op.note", "best", (String) bo[0]));
        } else o.add(T.get("S1.op.none"));
        o.add(""); o.add(T.get("S1.fam.h")); o.add(T.get("S1.fam.th"));
        Map<String, Double> kg = new HashMap<>(); for (Object[] fm : S.fams) kg.put((String) fm[0], (Double) fm[1]);
        List<String> names = new ArrayList<>(PR.keySet()); for (Object[] fm : S.fams) if (!PR.containsKey((String) fm[0])) names.add((String) fm[0]);
        for (String fam : names) {
            ProbeSum p = PR.get(fam);
            if (p == null) { o.add(fill("S1.fam.row", "fam", fam, "kg", num(kg.get(fam), 5), "gain", NA, "lo", NA, "hi", NA, "hits", NA, "acts", NA, "proj", NA, "advice", NA)); continue; }
            String adv = D.informative.contains(fam) ? T.get("S1.fam.add") : D.hint.contains(fam) ? T.get("S1.fam.hint") : (p.hi != null && p.hi < 0) ? T.get("S1.fam.cut") : T.get("S1.fam.keep");
            o.add(fill("S1.fam.row", "fam", fam, "kg", num(kg.get(fam), 5), "gain", snum(p.gain, 3), "lo", snum(p.lo, 3), "hi", snum(p.hi, 3), "hits", String.valueOf(p.h15),
                "acts", String.valueOf(p.a15), "proj", p.a15 > 0 ? projText(p.h15, p.a15) : NA, "advice", adv));
        }
        o.add(""); o.add(T.get("S1.atom.h"));
        if (!S.atoms.isEmpty()) {
            int a = 0, bb = 0, c = 0, e = 0; List<String> up = new ArrayList<>(), dn = new ArrayList<>();
            for (String[] at : S.atoms) {
                switch (at[1]) { case "A_VALID": a++; break; case "B_USEFUL": bb++; break; case "C_EXPLAIN_ONLY": c++; break; case "E_FALSE": e++; break; default: }
                if ((at[1].equals("A_VALID") || at[1].equals("B_USEFUL")) && up.size() < 6) up.add(at[0]);
                if (at[1].equals("E_FALSE") && dn.size() < 6) dn.add(at[0]);
            }
            o.add(fill("S1.atom.line", "a", String.valueOf(a), "b", String.valueOf(bb), "c", String.valueOf(c), "e", String.valueOf(e),
                "up", up.isEmpty() ? NA : String.join("、", up), "down", dn.isEmpty() ? NA : String.join("、", dn)));
        } else o.add(T.get("S1.atom.none"));
        o.add(""); o.add(T.get("S1.rep.h"));
        if (S.repair != null && S.repair[0] > 0) o.add(fill("S1.rep.line", "n", String.valueOf(S.repair[0]), "k", String.valueOf(S.repair[1]), "share", pct((double) S.repair[1] / S.repair[0])));
        boolean anySel = false;
        for (Stage st : S.stages) if (st.vc != null && st.fc != null) {
            anySel = true;
            o.add(fill("S1.sel.line", "tier", st.cov + " 委员会", "val", pct(st.vc), "fwd", pct(st.fc), "n", String.valueOf(st.nc), "d", fx((st.vc - st.fc) * 100.0, 1)));
        }
        if (!anySel) o.add(T.get("S1.sel.none"));
        o.add("");
        // 2 formulas
        o.add(T.get("S2.h"));
        boolean anyTier = false; String bestfam = bestGainProbe(PR);
        for (String tier : new String[]{"F15", "F30", "F100"}) {
            TrackSum s = TR.get(tier); if (s == null || s.acts == 0) continue;
            anyTier = true;
            o.add(fill("S2.tier", "tier", tier, "rate", pct(s.rate), "hits", String.valueOf(s.hits), "acts", String.valueOf(s.acts), "cov", pct(s.cov), "proj", projText(s.hits, s.acts), "worst", pct(s.worst)));
            if (S.formulas.containsKey(tier)) o.add(fill("S2.formula", "formula", S.formulas.get(tier)));
            if (tier.equals("F15")) {
                List<String> members = new ArrayList<>(); for (Object[] t : S.teacher) if (!members.contains((String) t[0])) members.add((String) t[0]);
                for (String m : members) {
                    List<Object[]> rows = new ArrayList<>(); for (Object[] t : S.teacher) if (t[0].equals(m)) rows.add(t);
                    List<String> parts = new ArrayList<>(); for (Object[] r : rows) parts.add(fill("S2.state", "state", (String) r[1], "hi", snum((Double) r[2], 3), "lo", snum((Double) r[3], 3)));
                    o.add(fill("S2.member", "member", m, "states", String.join("；", parts)));
                    List<Double> vals = new ArrayList<>(); for (Object[] r : rows) { if (r[2] != null) vals.add((Double) r[2]); if (r[3] != null) vals.add((Double) r[3]); }
                    String[] split = null;
                    for (Object[] r : rows) { Double hi = (Double) r[2], lo = (Double) r[3];
                        if (hi != null && lo != null && (hi > 0) != (lo > 0) && hi != 0 && lo != 0) { split = new String[]{(String) r[1], hi > 0 ? "高" : "低"}; break; } }
                    boolean allNeg = !vals.isEmpty(), allPos = !vals.isEmpty(); for (double x : vals) { if (!(x < 0)) allNeg = false; if (!(x >= 0)) allPos = false; }
                    if (allNeg) o.add(fill("S2.allneg", "bestfam", bestfam != null ? bestfam : NA));
                    else if (split != null) o.add(fill("S2.split", "state", split[0], "side", split[1]));
                    else if (allPos) o.add(T.get("S2.pos"));
                    else o.add(T.get("S2.mixed"));
                }
            }
            o.add("");
        }
        if (!anyTier) { o.add(T.get("S2.none")); o.add(""); }
        // 3 structure
        o.add(T.get("S3.h")); o.add(T.get("S3.th"));
        for (Map.Entry<String, ProbeSum> e : PR.entrySet()) { ProbeSum p = e.getValue();
            o.add(fill("S3.row", "fam", e.getKey(), "n", String.valueOf(p.n), "gain", snum(p.gain, 3), "lo", snum(p.lo, 3), "hi", snum(p.hi, 3), "pos", pct(p.pos),
                "hits", String.valueOf(p.h15), "acts", String.valueOf(p.a15), "cov", pct(p.cov15), "pred", pct(p.pred15), "proj", p.a15 > 0 ? projText(p.h15, p.a15) : NA, "worst", pct(p.worst15))); }
        o.add("");
        if (S.contmat != null) o.add(fill("S3.cm", "refits", String.valueOf(S.contmat[0]), "last", String.valueOf(S.contmat[1]), "max", String.valueOf(S.contmat[2]), "patterns", S.contmatPatterns));
        for (Stage st : S.stages) o.add(fill("S3.stage", "cov", st.cov, "k", String.valueOf(st.k), "vl", pct(st.vl), "vc", pct(st.vc), "use", st.use != 0 ? T.get("S3.stage.on") : T.get("S3.stage.off"),
            "fl", pct(st.fl), "nl", String.valueOf(st.nl), "fc", pct(st.fc), "nc", String.valueOf(st.nc)));
        ProbeSum reg = PR.get("REGMEM");
        if (reg != null && reg.st0 != null) o.add(fill("S3.reg", "st0", fx(reg.st0, 3)));
        o.add("");
        // 4 findings
        o.add(T.get("S4.h"));
        if (F.isEmpty()) o.add(T.get("S4.none"));
        for (int i = 0; i < Math.min(MAX_FINDINGS_SHOWN, F.size()); i++) {
            Finding f = F.get(i); String c = f.code;
            o.add(fill("S4.f", "sev", f.sev, "code", c, "scope", scopeS(f.scope), "title", T.get("F." + c + ".title"), "evidence", fillM("F." + c + ".evidence", f.v),
                "cause", fillM("F." + c + ".cause", f.v), "capability", fillM("F." + c + ".capability", f.v), "action", fillM("F." + c + ".action", f.v),
                "projection", fillM("F." + c + ".projection", f.v), "verify", fillM("F." + c + ".verify", f.v)));
            o.add("");
        }
        if (F.size() > MAX_FINDINGS_SHOWN) { o.add(fill("S4.more", "n", String.valueOf(F.size() - MAX_FINDINGS_SHOWN))); o.add(""); }
        // 5 capabilities
        o.add(T.get("S5.h")); o.add(T.get("S5.th"));
        for (Map<String, String> row : capabilityRows(D)) o.add(fillM("S5.row", row));
        o.add("");
        // 6 shadows
        o.add(T.get("S6.h"));
        List<Entry> sh = new ArrayList<>(); for (Entry e : L.entries.values()) if (!e.presc.isEmpty() && (!e.status.equals("RESOLVED") || e.sacts > 0)) sh.add(e);
        if (!sh.isEmpty()) {
            o.add(T.get("S6.th"));
            for (Entry e : sh) {
                String cmp = e.sacts > 0 && e.bacts > 0 ? pct((double) e.shits / e.sacts) + " vs " + pct((double) e.bhits / e.bacts) : NA;
                o.add(fill("S6.row", "code", e.code, "scope", scopeS(e.scope), "presc", e.presc, "since", String.valueOf(e.since), "acts", String.valueOf(e.sacts), "cmp", cmp, "outcome", OUTCOME.get(e.outcome)));
            }
        } else o.add(T.get("S6.none"));
        o.add("");
        // 7 ledger
        o.add(fill("S7.h", "cap", String.valueOf(LEDGER_CAP))); o.add(T.get("S7.th"));
        List<Entry> keys = new ArrayList<>(L.entries.values());
        keys.sort((x, y) -> Long.compare(y.last, x.last));          // stable: insertion order within the same period
        for (int i = 0; i < Math.min(MAX_LEDGER_SHOWN, keys.size()); i++) { Entry e = keys.get(i);
            o.add(fill("S7.row", "code", e.code, "scope", e.scope.isEmpty() ? NA : e.scope, "status", T.get("LS." + e.status), "first", String.valueOf(e.first), "last", String.valueOf(e.last),
                "seen", String.valueOf(e.seen), "value", num(e.value, 4), "best", num(e.best, 4))); }
        if (keys.size() > MAX_LEDGER_SHOWN) o.add(fill("S7.more", "n", String.valueOf(keys.size()), "k", String.valueOf(MAX_LEDGER_SHOWN)));
        o.add("");
        // 8 storage
        o.add(T.get("S8.h"));
        if (!S.storage.isEmpty()) {
            o.add(T.get("S8.th"));
            for (Object[] st : S.storage) { long bb = (Long) st[1], cap = (Long) st[2]; o.add(fill("S8.row", "cat", (String) st[0], "bytes", mb(bb), "cap", mb(cap), "pct", pct((double) bb / cap))); }
        } else o.add(T.get("S8.none"));
        o.add(T.get("S8.rules"));
        return bound(String.join("\n", o) + "\n");
    }

    static String bound(String md) {
        byte[] b = md.getBytes(StandardCharsets.UTF_8);
        if (b.length <= MD_MAX_BYTES) return md;
        String note = "\n" + T.get("S8.trunc") + "\n";
        int room = MD_MAX_BYTES - note.getBytes(StandardCharsets.UTF_8).length;
        int nl = lastNewline(b, room);
        return new String(b, 0, Math.max(0, nl), StandardCharsets.UTF_8) + note;
    }

    static List<Map<String, String>> capabilityRows(Diagnosis D) {
        Snapshot S = D.S; Map<String, ProbeSum> PR = D.probes; Map<String, TrackSum> TR = D.tracks; String NA = T.get("NA");
        Set<String> codes = new HashSet<>(); for (Finding f : D.findings) codes.add(f.code);
        List<Map<String, String>> rows = new ArrayList<>();
        java.util.function.Function<String, String> pproj = fam -> { ProbeSum p = PR.get(fam); return p != null && p.a15 > 0 ? fill("C.proj.rate", "proj", projText(p.h15, p.a15)) : T.get("C.proj.same"); };
        if (!PR.isEmpty()) {
            String bg = bestGainProbe(PR);
            String st = codes.contains("S1") ? "neck" : !D.informative.isEmpty() ? "ok" : "watch";
            row(rows, "C.probe", st, fill("C.probe.ev", "fam", bg, "gain", snum(PR.get(bg).gain, 3), "lo", snum(PR.get(bg).lo, 3)),
                st.equals("neck") ? T.get("C.probe.next.neck") : T.get("C.probe.next.ok"), pproj.apply(bg), T.get("C.verify.probe"));
        } else row(rows, "C.probe", "na", NA, NA, NA, NA);
        if (S.contmat != null) {
            boolean ok = S.contmat[1] > 0;
            row(rows, "C.contmat", ok ? "ok" : "watch", fill("C.contmat.ev", "n", String.valueOf(S.contmat[1]), "refits", String.valueOf(S.contmat[0])),
                ok ? T.get("C.contmat.next.ok") : T.get("C.contmat.next.watch"), ok ? pproj.apply("CONTMAT") : T.get("C.proj.same"), T.get("C.verify.shadow"));
        } else row(rows, "C.contmat", "na", NA, NA, NA, NA);
        if (!S.stages.isEmpty()) {
            boolean on = false; for (Stage st : S.stages) on |= st.use != 0;
            Finding s8 = null; for (Finding f : D.findings) if (f.code.equals("S8")) { s8 = f; break; }
            row(rows, "C.stage", s8 != null ? "ok" : "watch", fill("C.stage.ev", "k", String.valueOf(S.stages.get(0).k), "use", on ? T.get("S3.stage.on") : T.get("S3.stage.off")),
                s8 != null ? T.get("C.stage.next.ok") : T.get("C.stage.next.watch"), s8 != null ? fill("C.proj.rate", "proj", s8.v.get("proj")) : T.get("C.proj.same"), T.get("C.verify.next"));
        } else row(rows, "C.stage", "na", NA, NA, NA, NA);
        ProbeSum reg = PR.get("REGMEM");
        if (reg != null) {
            boolean ok = D.informative.contains("REGMEM");
            row(rows, "C.regime", ok ? "ok" : "watch", fill("C.regime.ev", "gain", snum(reg.gain, 3), "st0", num(reg.st0, 3)),
                ok ? T.get("C.regime.next.ok") : T.get("C.regime.next.watch"), pproj.apply("REGMEM"), T.get("C.verify.probe"));
        } else row(rows, "C.regime", "na", NA, NA, NA, NA);
        Double maxGap = null; for (ProbeSum p : PR.values()) if (p.a15 >= 200) { double g = Math.abs(p.pred15 - p.r15); if (maxGap == null || g > maxGap) maxGap = g; }
        if (maxGap != null) {
            boolean up = codes.contains("Q1");
            row(rows, "C.cal", up ? "up" : "ok", fill("C.cal.ev", "gap", fx(maxGap * 100.0, 1)), up ? T.get("C.cal.next.up") : T.get("C.cal.next.ok"), T.get("C.proj.quality"), T.get("C.verify.shadow"));
        } else row(rows, "C.cal", "na", NA, NA, NA, NA);
        Double maxDec = null; for (Stage st : S.stages) if (st.vc != null && st.fc != null) { double d = st.vc - st.fc; if (maxDec == null || d > maxDec) maxDec = d; }
        if (maxDec != null) {
            boolean up = codes.contains("Q2");
            row(rows, "C.judge", up ? "up" : "ok", fill("C.judge.ev", "d", fx(maxDec * 100.0, 1)), up ? T.get("C.judge.next.up") : T.get("C.judge.next.ok"), T.get("C.proj.decay"), T.get("C.verify.next"));
        } else row(rows, "C.judge", "na", NA, NA, NA, NA);
        if (TR.containsKey("F15") || TR.containsKey("F30") || TR.containsKey("F100")) {
            boolean up = codes.contains("S2") || codes.contains("Q3"); Cand b = D.best;
            row(rows, "C.committee", up ? "up" : "ok", fill("C.committee.ev", "r15", TR.containsKey("F15") ? pct(TR.get("F15").rate) : NA, "r30", TR.containsKey("F30") ? pct(TR.get("F30").rate) : NA,
                "r100", TR.containsKey("F100") ? pct(TR.get("F100").rate) : NA), up ? T.get("C.committee.next.up") : T.get("C.committee.next.ok"),
                b != null ? fill("C.proj.rate", "proj", projText(b.k, b.n)) : NA, T.get("C.verify.shadow"));
        } else row(rows, "C.committee", "na", NA, NA, NA, NA);
        if (!S.gens.isEmpty() || !S.ops.isEmpty()) {
            boolean up = codes.contains("E1") || codes.contains("E2"); Object[] bo = null;
            for (Object[] op : S.ops) if (bo == null || (Double) op[1] > (Double) bo[1]) bo = op;
            row(rows, "C.evo", up ? "up" : "ok", fill("C.evo.ev", "mean", D.genAll != null ? pts(D.genAll[0]) : NA, "op", bo != null ? (String) bo[0] : NA),
                up ? T.get("C.evo.next.up") : T.get("C.evo.next.ok"), T.get("C.proj.shadow"), T.get("C.verify.next"));
        } else row(rows, "C.evo", "na", NA, NA, NA, NA);
        if (!S.atoms.isEmpty()) {
            boolean up = codes.contains("Q4"); int ab = 0, e = 0;
            for (String[] a : S.atoms) { if (a[1].equals("A_VALID") || a[1].equals("B_USEFUL")) ab++; if (a[1].equals("E_FALSE")) e++; }
            row(rows, "C.memory", up ? "up" : "ok", fill("C.memory.ev", "ab", String.valueOf(ab), "e", String.valueOf(e)), up ? T.get("C.memory.next.up") : T.get("C.memory.next.ok"), T.get("C.proj.shadow"), T.get("C.verify.next"));
        } else row(rows, "C.memory", "na", NA, NA, NA, NA);
        if (!S.teacher.isEmpty()) {
            int neg = 0, tot = 0;
            for (Object[] t : S.teacher) for (int j = 2; j < 4; j++) if (t[j] != null) { tot++; if ((Double) t[j] < 0) neg++; }
            boolean up = neg > 0;
            row(rows, "C.teacher", up ? "up" : "ok", fill("C.teacher.ev", "neg", String.valueOf(neg), "tot", String.valueOf(tot)), up ? T.get("C.teacher.next.up") : T.get("C.teacher.next.ok"), T.get("C.proj.shadow"), T.get("C.verify.shadow"));
        } else row(rows, "C.teacher", "na", NA, NA, NA, NA);
        TrackSum main = TR.get("MAINLINE"), xi = TR.get("XI");
        if (main != null && main.acts > 0 && xi != null && xi.acts > 0) {
            boolean up = codes.contains("G5");
            row(rows, "C.ipm", up ? "up" : "ok", fill("C.ipm.ev", "r", pct(main.rate), "xi", pct(xi.rate), "p", S.xishare != null ? fx(S.pworse, 4) : NA),
                up ? T.get("C.ipm.next.up") : T.get("C.ipm.next.ok"), fill("C.proj.rate", "proj", projText(main.hits, main.acts)), T.get("C.verify.next"));
        } else row(rows, "C.ipm", "na", NA, NA, NA, NA);
        if (!S.storage.isEmpty()) {
            boolean up = codes.contains("M1"); Object[] w = null; double wr = 0;
            for (Object[] st : S.storage) { double r = (double) (Long) st[1] / (Long) st[2]; if (w == null || r > wr) { w = st; wr = r; } }
            row(rows, "C.store", up ? "up" : "ok", fill("C.store.ev", "cat", (String) w[0], "pct", pct(wr)), up ? T.get("C.store.next.up") : T.get("C.store.next.ok"), T.get("C.proj.store"), T.get("C.verify.census"));
        } else row(rows, "C.store", "na", T.get("C.store.ev.na"), T.get("C.store.next.ok"), T.get("C.proj.store"), T.get("C.verify.census"));
        return rows;
    }

    static void row(List<Map<String, String>> rows, String name, String status, String ev, String next, String proj, String verify) {
        rows.add(kv("name", T.get(name), "status", T.get("ST." + status), "evidence", ev, "next", next, "proj", proj, "verify", verify));
    }

    public static String toJson(Diagnosis D, Ledger L) {
        Cand b = D.best; StringBuilder o = new StringBuilder();
        o.append("{\"version\":\"").append(VERSION).append("\",\"dataset\":").append(jstr(D.S.dataset)).append(",\"seq\":").append(D.S.seq).append(",\"verdict\":")
         .append(D.verdict != null ? jstr(D.verdict) : "null").append(',');
        o.append("\"best\":").append(b == null ? "null" : "{\"name\":" + jstr(b.name) + ",\"projection\":" + fx(b.m, 6) + ",\"adjusted_lower\":" + fx(b.adjlo, 6) + ",\"candidates\":" + D.cands.size() + "}").append(',');
        o.append("\"findings\":[");
        for (int i = 0; i < D.findings.size(); i++) { Finding f = D.findings.get(i); if (i > 0) o.append(',');
            o.append("{\"code\":\"").append(f.code).append("\",\"scope\":").append(jstr(f.scope)).append(",\"severity\":\"").append(f.sev).append("\",\"value\":")
             .append(f.value == null ? "null" : fx(f.value, 6)).append(",\"prescription\":").append(jstr(f.presc)).append('}'); }
        o.append("],\"capabilities\":[");
        List<Map<String, String>> rows = capabilityRows(D);
        for (int i = 0; i < rows.size(); i++) { if (i > 0) o.append(','); o.append("{\"name\":").append(jstr(rows.get(i).get("name"))).append(",\"status\":").append(jstr(rows.get(i).get("status"))).append('}'); }
        o.append("],\"ledger_entries\":").append(L.entries.size()).append('}');
        String s = o.toString();
        if (s.getBytes(StandardCharsets.UTF_8).length > JSON_MAX_BYTES) throw new IllegalStateException("SU2_DIAG_JSON_BUDGET");
        return s + "\n";
    }
    static String jstr(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    /** R30 rendering adapters preserve the original statistics, prescriptions and legacy fixture outputs. */
    public static List<Map<String,Object>> sectionsR30(Diagnosis D, Ledger L) {
        List<Map<String,Object>> rows=new ArrayList<>();Snapshot S=D.S;
        rows.add(sectionR30("GENERATION",(long)S.gens.size(),"su2_diag_snapshot:gen", "缺少已存父子同源代际记录", "DiagnosisStoreSU2.snapshot：采集同一数据集父子机会收益配对；下界不为正不计验证提升"));
        rows.add(sectionR30("OPERATORS",(long)S.ops.size(),"su2_diag_snapshot:op", "缺少已存算子结果", "DiagnosisStoreSU2.snapshot：采集算子实际尝试与结果，不补造成功率"));
        Set<String> families=new HashSet<>(D.probes.keySet());for(Object[] row:S.fams)families.add(String.valueOf(row[0]));
        rows.add(sectionR30("FAMILIES",(long)families.size(),"su2_diag_snapshot:fam/probe", "缺少家族/探针结果", "DiagnosisStoreSU2.maintain：恢复原信息探针并保留实际数据集与曝光边界"));
        rows.add(sectionR30("ATOMS",(long)S.atoms.size(),"su2_diag_snapshot:atom", "缺少原子分级记录", "DiagnosisStoreSU2.snapshot：采集原子分级与原始贡献数据；不以存在性代替有效性"));
        rows.add(sectionR30("REPAIR",S.repair==null?null:(long)S.repair[0],"su2_diag_snapshot:repair", "缺少修复尝试和结果", "DiagnosisStoreSU2.snapshot：采集真实修复尝试、保留数与拒绝原因"));
        rows.add(sectionR30("STRUCTURE",(long)D.probes.size()+S.stages.size(),"su2_diag_snapshot:probe/stage", "缺少结构探针和分阶段对照", "DiagnosisStoreSU2.maintain：让原探针继续到已结算边界，采集同源验证与前向分块结果"));
        rows.add(sectionR30("LEDGER",(long)L.entries.size(),"su2_diag_state.ledger_text", "缺少升级问题账本记录", "DiagnosisStoreSU2.report：核对最新快照对应 ledger_text 与 diagnosed_sequence，保留问题生命周期"));
        rows.add(sectionR30("STORAGE",(long)S.storage.size(),"su2_diag_snapshot:storage", "缺少存储分类测量", "DiagnosisStoreSU2.snapshot：采集各分类实际字节、预算和测量时刻；不能用缺记录表示0字节"));
        rows.add(sectionR30("ABLATION",null,"R30 chapter10: r27_ablation / su1_model_ablation", "旧SU2诊断快照未包含完整消融配对证据；不能据此称消融表为空", "ReportR29.chapters：读取同一快照第10章的原消融行和已存任务/报告hash；按原同一期配对规则验证Q06"));
        return rows;
    }
    static Map<String,Object> sectionR30(String id,Long count,String source,String reason,String action) {
        Map<String,Object> out=new LinkedHashMap<>();out.put("section",id);out.put("records",count);out.put("source",source);
        out.put("status",count==null?"UNKNOWN":count==0?"INSUFFICIENT":"RECORDED_UNVALIDATED");
        out.put("reason",count==null||count==0?reason:"已存来源记录，不能仅凭行数判定质量或改善");out.put("collectionAction",action);
        out.put("pairedImprovementLower95",null);out.put("qualityEffective",null);return out;
    }
    public static String renderR30(Diagnosis D, Ledger L) {
        String text=render(D,L);
        if(D.probes.isEmpty())text=text.replace(T.get("S3.h")+"\n"+T.get("S3.th"),T.get("S3.h")+"\nINSUFFICIENT：没有已存结构探针结果；DiagnosisStoreSU2.maintain 采集原探针，不输出空表。");
        if(D.probes.isEmpty()&&D.S.fams.isEmpty())text=text.replace(T.get("S1.fam.h")+"\n"+T.get("S1.fam.th"),T.get("S1.fam.h")+"\nINSUFFICIENT：没有家族探针或权重来源；DiagnosisStoreSU2.snapshot 采集实际结果。");
        if(L.entries.isEmpty())text=text.replace(fill("S7.h","cap",String.valueOf(LEDGER_CAP))+"\n"+T.get("S7.th"),fill("S7.h","cap",String.valueOf(LEDGER_CAP))+"\nINSUFFICIENT：升级问题账本没有记录；DiagnosisStoreSU2.report 核对最新快照及账本序列。");
        StringBuilder availability=new StringBuilder("R30 升级诊断来源：缺源 UNKNOWN，零记录 INSUFFICIENT；统计和处方沿用原算法。\n");
        for(Map<String,Object> row:sectionsR30(D,L))availability.append(row.get("section")).append(" · ").append(row.get("status")).append(" · ").append(row.get("reason")).append("；采集：").append(row.get("collectionAction")).append('\n');
        return bound(availability.append('\n').append(text).toString());
    }
    public static String toJsonR30(Diagnosis D, Ledger L) {
        String legacy=toJson(D,L).trim();StringBuilder out=new StringBuilder(legacy.substring(0,legacy.length()-1));
        out.append(",\"r30Sections\":[");boolean first=true;
        for(Map<String,Object> row:sectionsR30(D,L)){if(!first)out.append(',');first=false;out.append('{');boolean field=true;
            for(Map.Entry<String,Object> entry:row.entrySet()){if(!field)out.append(',');field=false;out.append(jstr(entry.getKey())).append(':');Object value=entry.getValue();out.append(value==null?"null":value instanceof Number?value:jstr(String.valueOf(value)));}out.append('}');}
        String result=out.append("]}\n").toString();if(result.getBytes(StandardCharsets.UTF_8).length>JSON_MAX_BYTES)throw new IllegalStateException("R30_DIAG_JSON_BUDGET");return result;
    }
    public static String[] unavailableR30(String status,String reason) {
        if(!status.equals("INSUFFICIENT")&&!status.equals("UNKNOWN"))throw new IllegalArgumentException("R30_DIAG_MISSING_STATUS");
        String action="DiagnosisStoreSU2.report：采集 su2_diag_snapshot 的最新 report_sequence/snapshot 与 su2_diag_state；原探针需2000期已结算开奖，之后每500期生成；保留原门槛";
        String safe=reason.replace('\n',' ').replace('\r',' ').replace('\t',' ');
        return new String[]{"# 升级诊断书\n\n"+status+"："+safe+"\n\n采集："+action+"\n", "{\"version\":"+jstr(VERSION)+",\"status\":"+jstr(status)+",\"reason\":"+jstr(safe)+",\"records\":"+(status.equals("INSUFFICIENT")?"0":"null")+",\"verdict\":null,\"collectionAction\":"+jstr(action)+"}\n"};
    }

    /** Preserves raw same-view rows, including hashes/references, without treating row presence as quality. */
    public static Map<String,Object> evidenceR30(String source,Object rawCount,Object rawRows,String emptyReason,String action) {
        Long count=null;if(rawCount instanceof Number){double n=((Number)rawCount).doubleValue();if(Double.isFinite(n)&&n>=0&&n==Math.rint(n)&&n<Long.MAX_VALUE)count=((Number)rawCount).longValue();}
        Map<String,Object> out=sectionR30(source,count,source,count==null?"来源缺失或不可读；没有测得零记录":emptyReason,action);
        out.put("rows",rawRows);long shown=rawRows instanceof List?((List<?>)rawRows).size():0;
        out.put("displayedRows",rawRows instanceof List?shown:null);out.put("omittedRows",count==null?null:Math.max(0,count-shown));
        out.put("fullEvidence","snapshot.db:"+source+"; referenced model/diagnosis artifacts retain their original hashes");
        if(count!=null&&count>0&&!(rawRows instanceof List)){out.put("status","PARTIAL");out.put("reason","来源计数非零，但所需字段不可读；保留计数与采集动作，不输出空表结论");}
        return out;
    }

    // ------------------------------------------------------------------------------------------------ shadows
    /**
     * The shadow challenger text for a genome-level prescription, or null when the prescription is a parameter or is
     * executed automatically: ADD_MEMBER fam{params} adds a calibrated one-member program to the tier committee
     * (replacing the last of five); TIER_COVERAGE F15 0.30 runs the same committee at 30% frequency.
     */
    public static String shadowText(String presc, String championText) {
        if (championText == null || !CommitteeSU2.isCommittee(championText)) return null;
        CommitteeSU2 c = CommitteeSU2.parse(championText);
        String[] p = presc.split(" ");
        if (p[0].equals("TIER_COVERAGE") && p.length == 3) return new CommitteeSU2(c.subs, Double.parseDouble(p[2])).text();
        if (p[0].equals("ADD_MEMBER") && p.length == 2) {
            String m = p[1]; int brace = m.indexOf('{');
            String g = "SU2|CAL=1|POOL=STATIC|SINK=0|GATE=MASS|COV=" + String.format(Locale.ROOT, "%.2f", c.coverage) + "|M=" + m + "*1";
            ModelGenomeSU2 sub;
            try { sub = ModelGenomeSU2.parse(g); } catch (IllegalArgumentException notCanonical) { return null; }
            List<ModelGenomeSU2> subs = new ArrayList<>(c.subs);
            for (ModelGenomeSU2 s : subs) if (s.text().equals(sub.text())) return null;
            if (subs.size() >= CommitteeSU2.MAX_MEMBERS) subs.set(subs.size() - 1, sub); else subs.add(sub);
            if (brace < 1) return null;
            return new CommitteeSU2(subs, c.coverage).text();
        }
        return null;
    }
}
