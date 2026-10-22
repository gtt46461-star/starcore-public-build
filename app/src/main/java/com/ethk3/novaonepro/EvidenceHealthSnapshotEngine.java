package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Uses only settled REAL/SHADOW decisions to measure whether each legacy expert is currently
 * useful, neutral, or anti-correlated. It never creates a BET and never changes settlement. The
 * snapshot is point-in-time and can therefore be used by the realtime consensus without future
 * leakage.
 */
public final class EvidenceHealthSnapshotEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final int MODEL = 0;
    public static final int SHAPE = 1;
    public static final int COMBO = 2;
    public static final int RECOVERY = 3;
    public static final int SIMILAR = 4;
    public static final int MEMORY = 5;
    public static final int RAW = 6;
    public static final int FINAL = 7;
    public static final int COUNT = 8;

    public static final class Snapshot {
        public final int[] sample = new int[COUNT];
        public final double[] weight = new double[COUNT];
        public final boolean[] invert = new boolean[COUNT];
        public final double[] gain = new double[COUNT];
        public int settledSample;
        public int hitCount;
        public double hitRate;
        public R3HealthAudit.Report r3;
        public String summary = "";
    }

    public Snapshot load(DbStore store) {
        return loadInternal(store, true);
    }

    public Snapshot loadReadOnlyR36(DbStore store) {
        return loadInternal(store, false);
    }

    private Snapshot loadInternal(DbStore store, boolean persist) {
        try {
            if (persist && store != null)
                store.registerModuleBuildV20(
                        "EvidenceHealthSnapshotEngine",
                        MODULE_VERSION,
                        "SETTLED_REAL_SHADOW_HEALTH");
        } catch (Throwable ignore) {
        }
        Snapshot out = new Snapshot();
        double[] baseLoss = new double[COUNT];
        double[] directLoss = new double[COUNT];
        double[] mirrorLoss = new double[COUNT];

        ArrayList<Models.Decision> rows = new ArrayList<Models.Decision>();
        if (store != null) {
            try {
                add(rows, store.getRecentShadowDecisionsForDiagnosis(240));
            } catch (Throwable ignore) {
            }
            try {
                add(rows, store.getRecentBetDecisionsForDiagnosis(120));
            } catch (Throwable ignore) {
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            Models.Decision d = rows.get(i);
            if (d == null || !isSettledOutcome(d)) continue;
            int y = isHitOutcome(d) ? 1 : 0;
            out.settledSample++;
            if (y == 1) out.hitCount++;
            double[] p = values(d);
            for (int e = 0; e < COUNT; e++) {
                if (!validProbability(p[e])) continue;
                double direct = clamp(p[e], 0.05d, 0.65d);
                double mirror = clamp(0.60d - direct, 0.05d, 0.65d);
                out.sample[e]++;
                baseLoss[e] += logLoss(0.30d, y);
                directLoss[e] += logLoss(direct, y);
                mirrorLoss[e] += logLoss(mirror, y);
            }
        }

        out.hitRate = out.settledSample <= 0 ? 0.0d : ((double) out.hitCount) / out.settledSample;
        StringBuilder sb = new StringBuilder();
        sb.append("专家健康：settled=")
                .append(out.settledSample)
                .append("/hit=")
                .append(out.hitCount)
                .append("/rate=")
                .append(pct(out.hitRate));
        for (int e = 0; e < COUNT; e++) {
            int n = out.sample[e];
            if (n <= 0) {
                out.weight[e] = defaultWeight(e) * 0.35d;
                out.invert[e] = false;
                out.gain[e] = 0.0d;
            } else {
                double b = baseLoss[e] / n;
                double d = directLoss[e] / n;
                double m = mirrorLoss[e] / n;
                boolean inverse = n >= 24 && m + 0.008d < d;
                double best = inverse ? m : d;
                double gain = b - best;
                double sampleFactor = Math.min(1.0d, Math.sqrt(n / 80.0d));
                double reliability = Math.exp(clamp(gain * 5.0d, -1.35d, 1.35d));
                out.weight[e] =
                        clamp(
                                defaultWeight(e) * (0.35d + 0.65d * sampleFactor) * reliability,
                                0.025d,
                                1.40d);
                out.invert[e] = inverse;
                out.gain[e] = gain;
            }
            sb.append("｜")
                    .append(name(e))
                    .append("=")
                    .append(out.sample[e])
                    .append("/")
                    .append(fmt(out.weight[e]))
                    .append(out.invert[e] ? "/INV" : "/DIR");
        }
        out.r3 =
                persist
                        ? R3HealthAudit.analyzeAndPersist(store, rows)
                        : R3HealthAudit.analyze(rows);
        out.summary = sb.toString() + "｜R3=" + (out.r3 == null ? "NONE" : out.r3.summary());
        try {
            if (persist && store != null) {
                store.setState("adaptive_expert_health_version", MODULE_VERSION);
                store.setState("adaptive_expert_health_summary", out.summary);
                store.setState("adaptive_expert_health_sample", String.valueOf(out.settledSample));
                store.setState("adaptive_expert_health_hit_rate", String.valueOf(out.hitRate));
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public static double orient(double p, boolean invert) {
        if (!validProbability(p)) return 0.0d;
        p = clamp(p, 0.05d, 0.65d);
        return invert ? clamp(0.60d - p, 0.05d, 0.65d) : p;
    }

    private static boolean isSettledOutcome(Models.Decision d) {
        if (d == null) return false;
        if (d.settled == 1) return true;
        String r = d.result == null ? "" : d.result.trim().toUpperCase(Locale.US);
        return "WIN".equals(r)
                || "MISS".equals(r)
                || "HIT".equals(r)
                || "LOSE".equals(r)
                || "LOSS".equals(r)
                || "命中".equals(d.result)
                || "未中".equals(d.result)
                || "不中".equals(d.result);
    }

    private static boolean isHitOutcome(Models.Decision d) {
        if (d == null) return false;
        if (d.hit == 1) return true;
        String r = d.result == null ? "" : d.result.trim().toUpperCase(Locale.US);
        return "WIN".equals(r) || "HIT".equals(r) || "命中".equals(d.result);
    }

    private static double[] values(Models.Decision d) {
        return new double[] {
            d.modelP, d.shapeP, d.comboP, d.recoveryP, d.similarP, d.memoryP, d.rawP, d.finalP
        };
    }

    private static void add(ArrayList<Models.Decision> out, List<Models.Decision> in) {
        if (in == null) return;
        for (int i = 0; i < in.size(); i++) {
            Models.Decision d = in.get(i);
            if (d != null) out.add(d);
        }
    }

    private static double defaultWeight(int e) {
        switch (e) {
            case MODEL:
                return 0.85d;
            case SHAPE:
                return 0.72d;
            case COMBO:
                return 0.82d;
            case RECOVERY:
                return 0.42d;
            case SIMILAR:
                return 0.45d;
            case MEMORY:
                return 0.38d;
            case RAW:
                return 0.95d;
            case FINAL:
                return 0.55d;
            default:
                return 0.30d;
        }
    }

    private static String name(int e) {
        switch (e) {
            case MODEL:
                return "MODEL";
            case SHAPE:
                return "SHAPE";
            case COMBO:
                return "COMBO";
            case RECOVERY:
                return "RECOVERY";
            case SIMILAR:
                return "SIMILAR";
            case MEMORY:
                return "MEMORY";
            case RAW:
                return "RAW";
            case FINAL:
                return "LEGACY_FINAL";
            default:
                return "E" + e;
        }
    }

    private static double logLoss(double p, int y) {
        p = clamp(p, 0.0001d, 0.9999d);
        return y == 1 ? -Math.log(p) : -Math.log(1.0d - p);
    }

    private static boolean validProbability(double p) {
        return !Double.isNaN(p) && !Double.isInfinite(p) && p >= 0.05d && p <= 0.75d;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String pct(double p) {
        return String.format(Locale.US, "%.1f%%", p * 100.0d);
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.3f", v);
    }

    public static final String B07R3_HEALTH_FULLBLOOD_MARKER = "B07R3_ROLLING_OOS_HEALTH";

    public static final class HealthMetricSpec {
        public final String id, expert, metric;
        public final int window;
        public final double ready, freeze;

        HealthMetricSpec(String i, String e, int w, String m, double r, double f) {
            id = i;
            expert = e;
            window = w;
            metric = m;
            ready = r;
            freeze = f;
        }

        public String text() {
            return id
                    + "#"
                    + expert
                    + "#W"
                    + window
                    + "#"
                    + metric
                    + "#ready="
                    + ready
                    + "#freeze="
                    + freeze;
        }
    }

    public static java.util.List<HealthMetricSpec> healthMetricCatalogR3() {
        java.util.ArrayList<HealthMetricSpec> x = new java.util.ArrayList<HealthMetricSpec>();
        x.add(new HealthMetricSpec("HM001", "MODEL", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM002", "MODEL", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM003", "MODEL", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM004", "MODEL", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM005", "MODEL", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM006", "MODEL", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM007", "MODEL", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM008", "MODEL", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM009", "MODEL", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM010", "MODEL", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM011", "MODEL", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM012", "MODEL", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM013", "MODEL", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM014", "MODEL", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM015", "MODEL", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM016", "SHAPE", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM017", "SHAPE", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM018", "SHAPE", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM019", "SHAPE", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM020", "SHAPE", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM021", "SHAPE", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM022", "SHAPE", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM023", "SHAPE", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM024", "SHAPE", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM025", "SHAPE", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM026", "SHAPE", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM027", "SHAPE", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM028", "SHAPE", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM029", "SHAPE", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM030", "SHAPE", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM031", "COMBO", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM032", "COMBO", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM033", "COMBO", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM034", "COMBO", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM035", "COMBO", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM036", "COMBO", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM037", "COMBO", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM038", "COMBO", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM039", "COMBO", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM040", "COMBO", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM041", "COMBO", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM042", "COMBO", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM043", "COMBO", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM044", "COMBO", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM045", "COMBO", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM046", "RECOVERY", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM047", "RECOVERY", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM048", "RECOVERY", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM049", "RECOVERY", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM050", "RECOVERY", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM051", "RECOVERY", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM052", "RECOVERY", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM053", "RECOVERY", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM054", "RECOVERY", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM055", "RECOVERY", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM056", "RECOVERY", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM057", "RECOVERY", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM058", "RECOVERY", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM059", "RECOVERY", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM060", "RECOVERY", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM061", "SIMILAR", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM062", "SIMILAR", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM063", "SIMILAR", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM064", "SIMILAR", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM065", "SIMILAR", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM066", "SIMILAR", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM067", "SIMILAR", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM068", "SIMILAR", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM069", "SIMILAR", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM070", "SIMILAR", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM071", "SIMILAR", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM072", "SIMILAR", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM073", "SIMILAR", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM074", "SIMILAR", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM075", "SIMILAR", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM076", "MEMORY", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM077", "MEMORY", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM078", "MEMORY", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM079", "MEMORY", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM080", "MEMORY", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM081", "MEMORY", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM082", "MEMORY", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM083", "MEMORY", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM084", "MEMORY", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM085", "MEMORY", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM086", "MEMORY", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM087", "MEMORY", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM088", "MEMORY", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM089", "MEMORY", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM090", "MEMORY", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM091", "RAW", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM092", "RAW", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM093", "RAW", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM094", "RAW", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM095", "RAW", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM096", "RAW", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM097", "RAW", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM098", "RAW", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM099", "RAW", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM100", "RAW", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM101", "RAW", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM102", "RAW", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM103", "RAW", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM104", "RAW", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM105", "RAW", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM106", "FINAL", 12, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM107", "FINAL", 12, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM108", "FINAL", 12, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM109", "FINAL", 24, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM110", "FINAL", 24, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM111", "FINAL", 24, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM112", "FINAL", 48, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM113", "FINAL", 48, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM114", "FINAL", 48, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM115", "FINAL", 96, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM116", "FINAL", 96, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM117", "FINAL", 96, "LOGLOSS_GAIN", 0.360d, 0.280d));
        x.add(new HealthMetricSpec("HM118", "FINAL", 360, "HIT_RATE", 0.340d, 0.270d));
        x.add(new HealthMetricSpec("HM119", "FINAL", 360, "WILSON_LCB", 0.350d, 0.275d));
        x.add(new HealthMetricSpec("HM120", "FINAL", 360, "LOGLOSS_GAIN", 0.360d, 0.280d));
        return x;
    }

    public static final class R3HealthAudit {
        public static final class Window {
            public final int size;
            public int sample, hit;
            public double rate, wilson, brier, ece;
            public String state = "WATCH";

            Window(int s) {
                size = s;
            }

            public String summary() {
                return "W"
                        + size
                        + "="
                        + state
                        + "("
                        + hit
                        + "/"
                        + sample
                        + ",rate="
                        + fmt(rate)
                        + ",lcb="
                        + fmt(wilson)
                        + ",brier="
                        + fmt(brier)
                        + ",ece="
                        + fmt(ece)
                        + ")";
            }
        }

        public static final class Expert {
            public final String name;
            public final Window[] windows;
            public double stability, recentGain;
            public String state = "WATCH";

            Expert(String n) {
                name = n;
                windows =
                        new Window[] {
                            new Window(12),
                            new Window(24),
                            new Window(48),
                            new Window(96),
                            new Window(360)
                        };
            }

            public String summary() {
                StringBuilder s =
                        new StringBuilder(
                                name
                                        + "="
                                        + state
                                        + "|stability="
                                        + fmt(stability)
                                        + "|gain="
                                        + fmt(recentGain));
                for (int i = 0; i < windows.length; i++) s.append("|").append(windows[i].summary());
                return s.toString();
            }
        }

        public static final class Report {
            public final Expert[] experts;
            public int ready, strong, freeze, watch;
            public double portfolioStability;
            public String state = "WATCH";

            Report() {
                experts =
                        new Expert[] {
                            new Expert("MODEL"),
                            new Expert("SHAPE"),
                            new Expert("COMBO"),
                            new Expert("RECOVERY"),
                            new Expert("SIMILAR"),
                            new Expert("MEMORY"),
                            new Expert("RAW"),
                            new Expert("FINAL")
                        };
            }

            public String summary() {
                return "state="
                        + state
                        + "|strong="
                        + strong
                        + "|ready="
                        + ready
                        + "|watch="
                        + watch
                        + "|freeze="
                        + freeze
                        + "|stability="
                        + fmt(portfolioStability);
            }
        }

        public static Report analyzeAndPersist(
                DbStore store, java.util.List<Models.Decision> rows) {
            Report r = analyze(rows);
            if (store != null)
                try {
                    store.setState("b07r3_health_state", r.state);
                    store.setState("b07r3_health_summary", r.summary());
                    StringBuilder s = new StringBuilder(12000);
                    for (int i = 0; i < r.experts.length; i++)
                        s.append(r.experts[i].summary()).append('\n');
                    store.setState("b07r3_health_experts", s.toString());
                    store.registerModuleBuildV20(
                            "EvidenceHealthSnapshotEngine", AppConfig.B07R3_VERSION, r.summary());
                } catch (Throwable ignore) {
                }
            return r;
        }

        public static Report analyze(java.util.List<Models.Decision> rows) {
            Report out = new Report();
            java.util.ArrayList<Models.Decision> settled =
                    new java.util.ArrayList<Models.Decision>();
            if (rows != null)
                for (int i = 0; i < rows.size(); i++) {
                    Models.Decision d = rows.get(i);
                    if (d != null && isSettledOutcome(d)) settled.add(d);
                }
            for (int e = 0; e < out.experts.length; e++) evaluateExpert(out.experts[e], settled, e);
            double st = 0;
            for (int e = 0; e < out.experts.length; e++) {
                Expert x = out.experts[e];
                st += x.stability;
                if ("STRONG".equals(x.state)) out.strong++;
                else if ("READY".equals(x.state)) out.ready++;
                else if ("FREEZE".equals(x.state)) out.freeze++;
                else out.watch++;
            }
            out.portfolioStability = out.experts.length == 0 ? 0 : st / out.experts.length;
            if (out.freeze >= 3) out.state = "FREEZE";
            else if (out.strong >= 3 && out.freeze == 0) out.state = "STRONG";
            else if (out.ready + out.strong >= 4) out.state = "READY";
            else out.state = "WATCH";
            return out;
        }

        private static void evaluateExpert(
                Expert out, java.util.List<Models.Decision> rows, int expert) {
            for (int w = 0; w < out.windows.length; w++)
                evaluateWindow(out.windows[w], rows, expert);
            double mean = 0, var = 0;
            int n = 0;
            for (int w = 0; w < out.windows.length; w++) {
                Window q = out.windows[w];
                if (q.sample <= 0) continue;
                mean += q.rate;
                n++;
            }
            mean = n == 0 ? 0 : mean / n;
            for (int w = 0; w < out.windows.length; w++) {
                Window q = out.windows[w];
                if (q.sample <= 0) continue;
                double d = q.rate - mean;
                var += d * d;
            }
            out.stability = n <= 1 ? 0 : clamp(1 - Math.sqrt(var / n) * 3, 0, 1);
            Window recent = out.windows[1];
            out.recentGain = recent.sample <= 0 ? 0 : recent.rate - 0.30d;
            if (out.windows[2].sample >= 30
                    && out.windows[2].wilson >= 0.36d
                    && out.stability >= 0.55d) out.state = "STRONG";
            else if (out.windows[1].sample >= 18 && out.windows[1].wilson >= 0.31d)
                out.state = "READY";
            else if (out.windows[2].sample >= 30 && out.windows[2].rate < 0.27d)
                out.state = "FREEZE";
            else out.state = "WATCH";
        }

        private static void evaluateWindow(
                Window out, java.util.List<Models.Decision> rows, int expert) {
            int start = Math.max(0, rows.size() - out.size), bins = 10;
            double brier = 0;
            double[] sumP = new double[bins];
            int[] count = new int[bins], hit = new int[bins];
            for (int i = start; i < rows.size(); i++) {
                Models.Decision d = rows.get(i);
                double p = expertProbability(d, expert);
                if (!validProbability(p)) continue;
                int y = isHitOutcome(d) ? 1 : 0;
                out.sample++;
                out.hit += y;
                double z = p - y;
                brier += z * z;
                int bin = (int) Math.floor(clamp(p, 0, 0.999999d) * bins);
                if (bin < 0) bin = 0;
                if (bin >= bins) bin = bins - 1;
                sumP[bin] += p;
                count[bin]++;
                hit[bin] += y;
            }
            out.rate = out.sample <= 0 ? 0 : (double) out.hit / out.sample;
            out.wilson = wilson(out.hit, out.sample, 1.96d);
            out.brier = out.sample <= 0 ? 0 : brier / out.sample;
            double ece = 0;
            for (int b = 0; b < bins; b++) {
                if (count[b] <= 0) continue;
                double mp = sumP[b] / count[b], mh = (double) hit[b] / count[b];
                ece += ((double) count[b] / Math.max(1, out.sample)) * Math.abs(mp - mh);
            }
            out.ece = ece;
            if (out.sample >= 30 && out.rate < 0.27d) out.state = "FREEZE";
            else if (out.sample >= 24 && out.wilson >= 0.36d && out.ece <= 0.08d)
                out.state = "STRONG";
            else if (out.sample >= 12 && out.wilson >= 0.29d) out.state = "READY";
            else out.state = "WATCH";
        }

        private static double expertProbability(Models.Decision d, int e) {
            if (d == null) return 0;
            switch (e) {
                case MODEL:
                    return d.modelP;
                case SHAPE:
                    return d.shapeP;
                case COMBO:
                    return d.comboP;
                case RECOVERY:
                    return d.recoveryP;
                case SIMILAR:
                    return d.similarP;
                case MEMORY:
                    return d.memoryP;
                case RAW:
                    return d.rawP;
                case FINAL:
                    return d.finalP;
                default:
                    return 0;
            }
        }

        private static double wilson(int hit, int n, double z) {
            if (n <= 0) return 0;
            double p = (double) hit / n,
                    zz = z * z,
                    den = 1 + zz / n,
                    center = p + zz / (2 * n),
                    rad = z * Math.sqrt((p * (1 - p) + zz / (4 * n)) / n);
            return clamp((center - rad) / den, 0, 1);
        }
    }

    public static String healthMetricManifestR3() {
        StringBuilder s = new StringBuilder(24000);
        java.util.List<HealthMetricSpec> x = healthMetricCatalogR3();
        for (int i = 0; i < x.size(); i++) s.append(x.get(i).text()).append('\n');
        return s.toString();
    }
}
