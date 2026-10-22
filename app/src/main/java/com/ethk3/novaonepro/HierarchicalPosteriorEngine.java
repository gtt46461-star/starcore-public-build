package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Locale;

/**
 * Finite coarse/mid/fine family posterior with deterministic fallback.
 *
 * <p>V40 realtime optimization: all posterior rows for the active version are read once per
 * prediction batch. Candidate evaluation then uses the immutable in-memory snapshot instead of
 * issuing up to five SQLite queries for every candidate.
 */
public final class HierarchicalPosteriorEngine {
    public static final String V46_ROLE = "PRE_DRAW_POSTERIOR_EVIDENCE_ONLY_NOT_FINAL_GATE";

    public static final String MODULE_VERSION = AppConfig.HIERARCHICAL_POSTERIOR_VERSION;
    public static final String POLICY_UNKNOWN = "UNKNOWN";
    public static final String POLICY_WATCH = "WATCH";
    public static final String POLICY_READY = "READY";
    public static final String POLICY_STRONG = "STRONG";
    public static final String POLICY_SOFT_RISK = "SOFT_RISK";
    public static final String POLICY_HARD_RISK = "HARD_RISK";

    public static final class FamilyKeys {
        public String global = "GLOBAL";
        public String position = "";
        public String coarse = "";
        public String mid = "";
        public String fine = "";
    }

    public static final class Posterior {
        public String level = "GLOBAL";
        public String familyKey = "GLOBAL";
        public int sample;
        public int hit;
        public double mean = AppConfig.RANDOM_K3_BASELINE;
        public double wilsonLower;
        public double recentRate;
        public double drift;
        public String policy = POLICY_UNKNOWN;
        public String version = MODULE_VERSION;
        public boolean ready;
    }

    /** Immutable batch snapshot. */
    public static final class Snapshot {
        private final HashMap<String, Posterior> rows = new HashMap<String, Posterior>();
        public int rowCount;
        public int totalSample;
        public long loadedAt;
        public String version = MODULE_VERSION;
        public String status = "POSTERIOR_SNAPSHOT_EMPTY";

        private Posterior get(String level, String key) {
            Posterior p = rows.get(mapKey(level, key));
            return p == null ? empty(level, key) : copyOf(p);
        }
    }

    public Snapshot loadSnapshot(DbStore store) {
        Snapshot snapshot = new Snapshot();
        snapshot.loadedAt = System.currentTimeMillis();
        if (store == null) return snapshot;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.query(
                            DbHelper.TABLE_HIERARCHICAL_POSTERIOR,
                            new String[] {
                                "family_level",
                                "family_key",
                                "sample_count",
                                "hit_count",
                                "recent_sample",
                                "recent_hit",
                                "posterior_mean",
                                "wilson_lower",
                                "drift_score",
                                "version"
                            },
                            "version=?",
                            new String[] {MODULE_VERSION},
                            null,
                            null,
                            null);
            while (c != null && c.moveToNext()) {
                Posterior p = new Posterior();
                p.level = safe(c.getString(0));
                p.familyKey = safe(c.getString(1));
                p.sample = Math.max(0, c.getInt(2));
                p.hit = Math.max(0, Math.min(p.sample, c.getInt(3)));
                int rs = Math.max(0, c.getInt(4));
                int rh = Math.max(0, Math.min(rs, c.getInt(5)));
                p.mean = finite(c.getDouble(6), betaMean(p.hit, p.sample));
                p.wilsonLower = finite(c.getDouble(7), wilsonLower(p.hit, p.sample));
                p.recentRate = rs > 0 ? ((double) rh) / ((double) rs) : p.mean;
                p.drift = finite(c.getDouble(8), Math.abs(p.recentRate - p.mean));
                String version = safe(c.getString(9));
                if (version.length() > 0) p.version = version;
                snapshot.rows.put(mapKey(p.level, p.familyKey), p);
                snapshot.rowCount++;
                if ("GLOBAL".equals(p.level) && "GLOBAL".equals(p.familyKey)) {
                    snapshot.totalSample = p.sample;
                }
            }
            snapshot.status =
                    snapshot.rowCount > 0 ? "POSTERIOR_SNAPSHOT_READY" : "POSTERIOR_SNAPSHOT_EMPTY";
        } catch (Throwable e) {
            snapshot.status = "POSTERIOR_SNAPSHOT_ERROR:" + e.getClass().getSimpleName();
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        return snapshot;
    }

    public Posterior apply(DbStore store, Models.ComboCandidate c) {
        return apply(loadSnapshot(store), c);
    }

    public Posterior apply(Snapshot snapshot, Models.ComboCandidate c) {
        FamilyKeys keys = deriveKeys(c);
        Posterior p = loadBest(snapshot, keys);
        writeCandidate(c, keys, p);
        return p;
    }

    public Posterior loadBest(DbStore store, FamilyKeys keys) {
        return loadBest(loadSnapshot(store), keys);
    }

    public Posterior loadBest(Snapshot snapshot, FamilyKeys keys) {
        Snapshot s = snapshot == null ? new Snapshot() : snapshot;
        FamilyKeys k = keys == null ? new FamilyKeys() : keys;
        Posterior global = s.get("GLOBAL", k.global);
        Posterior position = s.get("POSITION", k.position);
        Posterior coarse = s.get("COARSE", k.coarse);
        Posterior mid = s.get("MID", k.mid);
        Posterior fine = s.get("FINE", k.fine);

        Posterior selected;
        if (fine.sample >= AppConfig.POSTERIOR_FINE_MIN_SAMPLE) selected = fine;
        else if (mid.sample >= AppConfig.POSTERIOR_MID_MIN_SAMPLE) selected = mid;
        else if (coarse.sample >= AppConfig.POSTERIOR_COARSE_MIN_SAMPLE) selected = coarse;
        else if (position.sample >= AppConfig.POSTERIOR_POSITION_MIN_SAMPLE) selected = position;
        else selected = mostInformative(global, position, coarse, mid, fine);

        Posterior out = copyOf(selected);
        double mean = global.sample > 0 ? global.mean : AppConfig.RANDOM_K3_BASELINE;
        double recent = global.sample > 0 ? global.recentRate : mean;
        mean = shrink(mean, position.mean, position.sample, 16.0d);
        recent = shrink(recent, position.recentRate, position.sample, 18.0d);
        mean = shrink(mean, coarse.mean, coarse.sample, 14.0d);
        recent = shrink(recent, coarse.recentRate, coarse.sample, 16.0d);
        mean = shrink(mean, mid.mean, mid.sample, 20.0d);
        recent = shrink(recent, mid.recentRate, mid.sample, 22.0d);
        mean = shrink(mean, fine.mean, fine.sample, 28.0d);
        recent = shrink(recent, fine.recentRate, fine.sample, 30.0d);
        out.mean = clamp(mean, 0.05d, 0.95d);
        out.recentRate = clamp(recent, 0.0d, 1.0d);
        out.drift = Math.abs(out.recentRate - out.mean);
        return finalizePolicy(out);
    }

    private Posterior mostInformative(
            Posterior global, Posterior position, Posterior coarse, Posterior mid, Posterior fine) {
        Posterior best = global;
        Posterior[] rows = new Posterior[] {position, coarse, mid, fine};
        for (int i = 0; i < rows.length; i++) {
            Posterior p = rows[i];
            if (p != null && p.sample > 0 && (best == null || p.sample > best.sample)) best = p;
        }
        return best == null ? new Posterior() : best;
    }

    private double shrink(double parent, double child, int sample, double strength) {
        if (sample <= 0 || Double.isNaN(child) || Double.isInfinite(child)) return parent;
        double w = ((double) sample) / (sample + Math.max(1.0d, strength));
        return parent * (1.0d - w) + child * w;
    }

    private void writeCandidate(Models.ComboCandidate c, FamilyKeys keys, Posterior p) {
        if (c == null || keys == null || p == null) return;
        c.positionFamilyKey = keys.position;
        c.coarseFamilyKey = keys.coarse;
        c.midFamilyKey = keys.mid;
        c.fineFamilyKey = keys.fine;
        c.posteriorMatchedLevel = p.level;
        c.posteriorFamilyKey = p.familyKey;
        c.posteriorSample = p.sample;
        c.posteriorHit = p.hit;
        c.posteriorMean = p.mean;
        c.posteriorWilsonLower = p.wilsonLower;
        c.posteriorRecentRate = p.recentRate;
        c.posteriorDrift = p.drift;
        c.posteriorPolicy = p.policy;
        c.posteriorVersion = p.version;
        c.familyWilsonLower = p.wilsonLower;
    }

    private Posterior finalizePolicy(Posterior p) {
        if (p == null) p = new Posterior();
        if (p.sample <= 0) {
            p.policy = POLICY_UNKNOWN;
            p.ready = false;
            p.mean = AppConfig.RANDOM_K3_BASELINE;
            p.recentRate = p.mean;
            return p;
        }
        if (p.sample < AppConfig.V40_POSTERIOR_MIN_READY) {
            p.policy = POLICY_WATCH;
            p.ready = false;
            return p;
        }
        if (p.sample >= AppConfig.POSTERIOR_HARD_RISK_MIN_SAMPLE
                && upperWilson(p.hit, p.sample) < 0.290d) {
            p.policy = POLICY_HARD_RISK;
            p.ready = false;
        } else if (p.drift >= AppConfig.POSTERIOR_DRIFT_HARD
                || (p.sample >= AppConfig.POSTERIOR_STRONG_MIN_SAMPLE
                        && p.recentRate < AppConfig.POSTERIOR_SOFT_RISK_RATE)) {
            p.policy = POLICY_SOFT_RISK;
            p.ready = false;
        } else if (p.sample >= AppConfig.POSTERIOR_STRONG_MIN_SAMPLE
                && p.mean >= 0.365d
                && p.wilsonLower >= 0.285d
                && p.recentRate >= 0.330d) {
            p.policy = POLICY_STRONG;
            p.ready = true;
        } else if (p.sample >= AppConfig.V40_POSTERIOR_MIN_READY
                && p.mean >= 0.315d
                && p.wilsonLower >= 0.235d
                && p.recentRate >= 0.285d) {
            p.policy = POLICY_READY;
            p.ready = true;
        } else {
            p.policy = POLICY_WATCH;
            p.ready = false;
        }
        return p;
    }

    public static FamilyKeys deriveKeys(Models.ComboCandidate c) {
        FamilyKeys k = new FamilyKeys();
        if (c == null) return k;
        String pg = c.position <= 3 ? "FRONT" : (c.position <= 7 ? "MID" : "BACK");
        String registered =
                compact(
                        first(
                                c.historicalPriorCoarseKey,
                                c.probabilityWindowStateKey,
                                c.windowStateKey,
                                c.stateKey),
                        72);
        String entropy = token(registered, "ENT_", "ENT_UNKNOWN");
        String drift = token(registered, "DRIFT_", "DRIFT_UNKNOWN");
        String agreement = token(registered, "AGR_", "AGR_UNKNOWN");
        String probabilityBand = probabilityBand(c.modelP > 0.0d ? c.modelP : c.rawP);
        int min = Math.min(c.n1, Math.min(c.n2, c.n3));
        int max = Math.max(c.n1, Math.max(c.n2, c.n3));
        int span = max - min;
        String spanBand = span <= 3 ? "TIGHT" : (span <= 6 ? "MID" : "WIDE");
        int odd = (c.n1 % 2) + (c.n2 % 2) + (c.n3 % 2);
        String parity = odd >= 2 ? "ODD_HEAVY" : "EVEN_HEAVY";
        int low = (c.n1 <= 5 ? 1 : 0) + (c.n2 <= 5 ? 1 : 0) + (c.n3 <= 5 ? 1 : 0);
        String size = low >= 2 ? "LOW_HEAVY" : "HIGH_HEAVY";
        String coverage =
                bucket(Math.max(c.coverageQuality, c.coverageQualityScore), 0.42d, 0.62d, "COV");
        String counter =
                bucket(Math.max(c.counterRisk, c.counterEvidenceScore), 0.30d, 0.55d, "CTR");
        String role =
                compact(
                        first(
                                c.k3RoleStructure,
                                c.needSupplyMatchKey,
                                c.dynamicClusterPrototypeKey),
                        24);

        k.position = "P" + c.position;
        k.coarse = "REG40|PG=" + pg + "|" + entropy + "|" + drift + "|" + agreement;
        k.mid =
                k.coarse
                        + "|P="
                        + c.position
                        + "|PB="
                        + probabilityBand
                        + "|SPAN="
                        + spanBand
                        + "|PAR="
                        + parity
                        + "|SIZE="
                        + size;
        /* Fine remains a finite structural family. Exact K3 identities are excluded
         * because 1,200 cells per issue cannot support stable partial pooling. */
        k.fine = k.mid + "|" + coverage + "|" + counter + "|ROLE=" + role;
        return k;
    }

    private static String probabilityBand(double p) {
        if (p < 0.285d) return "LT285";
        if (p < 0.305d) return "P285_305";
        if (p < 0.325d) return "P305_325";
        if (p < 0.345d) return "P325_345";
        if (p < 0.365d) return "P345_365";
        return "GE365";
    }

    private static String token(String source, String prefix, String fallback) {
        String s = safe(source).toUpperCase(Locale.US);
        int at = s.indexOf(prefix);
        if (at < 0) return fallback;
        int end = s.indexOf('_', at + prefix.length());
        int pipe = s.indexOf('|', at);
        if (pipe >= 0 && (end < 0 || pipe < end)) end = pipe;
        if (end < 0) end = s.length();
        return compact(s.substring(at, end), 24);
    }

    private static String typedPolicy(String s) {
        String policy = DecisionAuthorizationEngine.resolvePolicy(s);
        if (DecisionAuthorizationEngine.POLICY_HARD_RISK.equals(policy)
                || DecisionAuthorizationEngine.POLICY_SOFT_RISK.equals(policy)) return "RISK";
        if (DecisionAuthorizationEngine.POLICY_STRONG.equals(policy)) return "STRONG";
        if (DecisionAuthorizationEngine.POLICY_READY.equals(policy)) return "READY";
        return "WATCH";
    }

    private static Posterior empty(String level, String key) {
        Posterior p = new Posterior();
        p.level = safe(level);
        p.familyKey = safe(key);
        return p;
    }

    private static Posterior copyOf(Posterior p) {
        Posterior x = new Posterior();
        if (p == null) return x;
        x.level = p.level;
        x.familyKey = p.familyKey;
        x.sample = p.sample;
        x.hit = p.hit;
        x.mean = p.mean;
        x.wilsonLower = p.wilsonLower;
        x.recentRate = p.recentRate;
        x.drift = p.drift;
        x.policy = p.policy;
        x.version = p.version;
        x.ready = p.ready;
        return x;
    }

    private static String mapKey(String level, String key) {
        return safe(level) + "\u0001" + safe(key);
    }

    private static String classifyText(String s, String pos, String neg, String neutral) {
        String x = safe(s).toUpperCase(Locale.US);
        if (x.indexOf(pos) >= 0 || x.indexOf("RISE") >= 0) return pos;
        if (x.indexOf(neg) >= 0 || x.indexOf("FALL") >= 0) return neg;
        return neutral;
    }

    private static String bucket(double v, double a, double b, String p) {
        return p + (v <= 0 ? "0" : (v < a ? "L" : (v < b ? "M" : "H")));
    }

    private static String compact(String s, int max) {
        String x = safe(s).replace(' ', '_').replace('|', '_');
        return x.length() <= max ? x : x.substring(0, max);
    }

    private static int[] sort3(int a, int b, int c) {
        int[] x = {a, b, c};
        if (x[0] > x[1]) {
            int t = x[0];
            x[0] = x[1];
            x[1] = t;
        }
        if (x[1] > x[2]) {
            int t = x[1];
            x[1] = x[2];
            x[2] = t;
        }
        if (x[0] > x[1]) {
            int t = x[0];
            x[0] = x[1];
            x[1] = t;
        }
        return x;
    }

    private static String first(String a, String b, String c) {
        if (safe(a).length() > 0) return a;
        if (safe(b).length() > 0) return b;
        return safe(c);
    }

    private static String first(String a, String b, String c, String d) {
        if (safe(a).length() > 0) return a;
        if (safe(b).length() > 0) return b;
        if (safe(c).length() > 0) return c;
        return safe(d);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double finite(double v, double d) {
        return Double.isNaN(v) || Double.isInfinite(v) ? d : v;
    }

    public static double betaMean(int hit, int sample) {
        return (hit + 1.0d) / (sample + 2.0d);
    }

    public static double wilsonLower(int hit, int sample) {
        if (sample <= 0) return 0.0d;
        double n = sample, p = ((double) hit) / n, z = 1.96d, den = 1.0d + z * z / n;
        return Math.max(
                0.0d,
                (p + z * z / (2 * n) - z * Math.sqrt((p * (1 - p) + z * z / (4 * n)) / n)) / den);
    }

    public static double upperWilson(int hit, int sample) {
        if (sample <= 0) return 1.0d;
        double n = sample, p = ((double) hit) / n, z = 1.96d, den = 1.0d + z * z / n;
        return Math.min(
                1.0d,
                (p + z * z / (2 * n) + z * Math.sqrt((p * (1 - p) + z * z / (4 * n)) / n)) / den);
    }
}
