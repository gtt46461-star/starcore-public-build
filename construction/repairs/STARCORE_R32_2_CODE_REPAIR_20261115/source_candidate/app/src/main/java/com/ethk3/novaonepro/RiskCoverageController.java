package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * V42 single risk-coverage owner.
 *
 * <p>The controller never changes probability and never creates BET. It observes settled real
 * trades plus rolling coverage, then exposes one consistent pacing contract to LanePolicyCompiler
 * and LiveActionBudgetEngine.
 */
public final class RiskCoverageController {
    public static final String V46_ROLE = "BACKGROUND_COVERAGE_FRONTIER_CHALLENGER_NO_FORCED_BET";

    public static final String MODULE_VERSION = AppConfig.V42_RISK_COVERAGE_VERSION;

    public static final class State {
        public Models.FrequencyState frequency;
        public int settledSample;
        public int hitCount;
        public int probeUsed50;
        public double hitRate;
        public double wilson80;
        public double wilson95;
        public double coverage50;
        public double coverage100;
        public double coverage300;
        public double coveragePressure;
        public double normalAcceptMin;
        public double floorAcceptMin;
        public double probeAcceptMin;
        public int probeCap50;
        public int probeMinGap;
        public boolean allowNormal = true;
        public boolean allowFloor = true;
        public boolean allowProbe = true;
        public String mode = "BOOTSTRAP";
        public long builtAt;

        public String summary() {
            return "mode="
                    + mode
                    + "|sample="
                    + settledSample
                    + "|hit="
                    + hitCount
                    + "|rate="
                    + round(hitRate)
                    + "|W80="
                    + round(wilson80)
                    + "|W95="
                    + round(wilson95)
                    + "|cover50/100/300="
                    + round(coverage50)
                    + "/"
                    + round(coverage100)
                    + "/"
                    + round(coverage300)
                    + "|pressure="
                    + round(coveragePressure)
                    + "|accept="
                    + round(normalAcceptMin)
                    + "/"
                    + round(floorAcceptMin)
                    + "/"
                    + round(probeAcceptMin)
                    + "|probe="
                    + probeUsed50
                    + "/"
                    + probeCap50
                    + "|gap="
                    + probeMinGap
                    + "|allow="
                    + (allowNormal ? 1 : 0)
                    + "/"
                    + (allowFloor ? 1 : 0)
                    + "/"
                    + (allowProbe ? 1 : 0);
        }
    }

    private static State cached;
    private static int cachedStoreIdentity;
    private static long cachedAt;

    private RiskCoverageController() {}

    public static synchronized State evaluate(DbStore store) {
        int identity = store == null ? 0 : System.identityHashCode(store);
        long now = System.currentTimeMillis();
        if (cached != null
                && identity == cachedStoreIdentity
                && now - cachedAt <= AppConfig.V42_RISK_COVERAGE_CACHE_MS) {
            return cached;
        }

        State s = new State();
        s.builtAt = now;
        try {
            if (store != null) s.frequency = store.loadRollingFrequencyStateV20();
        } catch (Throwable ignore) {
        }
        if (s.frequency == null) s.frequency = new Models.FrequencyState();
        s.coverage50 = s.frequency.frequency50;
        s.coverage100 = s.frequency.frequency100;
        s.coverage300 = s.frequency.frequency300;
        loadSettledQuality(store, s);

        applyPolicy(s);

        cached = s;
        cachedStoreIdentity = identity;
        cachedAt = now;
        persist(store, s);
        return s;
    }

    /** Deterministic snapshot evaluator used by self-checks and offline replay. */
    public static State evaluateSnapshot(
            Models.FrequencyState frequency, int settledSample, int hitCount, int probeUsed50) {
        State s = new State();
        s.builtAt = System.currentTimeMillis();
        s.frequency = frequency == null ? new Models.FrequencyState() : frequency;
        s.coverage50 = s.frequency.frequency50;
        s.coverage100 = s.frequency.frequency100;
        s.coverage300 = s.frequency.frequency300;
        s.settledSample = Math.max(0, settledSample);
        s.hitCount = Math.max(0, Math.min(hitCount, s.settledSample));
        s.probeUsed50 = Math.max(0, probeUsed50);
        s.hitRate = s.settledSample <= 0 ? 0.0d : ((double) s.hitCount) / (double) s.settledSample;
        s.wilson80 = wilsonLower(s.hitCount, s.settledSample, 1.2815515655446004d);
        s.wilson95 = wilsonLower(s.hitCount, s.settledSample, 1.959963984540054d);
        applyPolicy(s);
        return s;
    }

    private static void applyPolicy(State s) {
        if (s == null) return;
        double p50 = deficit(s.coverage50, AppConfig.V42_COVERAGE_MIN);
        double p100 = deficit(s.coverage100, AppConfig.V42_COVERAGE_MIN);
        double p300 = deficit(s.coverage300, AppConfig.V42_COVERAGE_MIN);
        s.coveragePressure = clamp01(0.45d * p50 + 0.35d * p100 + 0.20d * p300);

        s.normalAcceptMin = AppConfig.V42_NORMAL_ACCEPT_MIN;
        s.floorAcceptMin =
                AppConfig.V42_FLOOR_ACCEPT_MIN
                        - AppConfig.V42_COVERAGE_ACCEPT_RELAX_MAX * s.coveragePressure;
        s.probeAcceptMin =
                AppConfig.V42_PROBE_ACCEPT_MIN
                        - AppConfig.V42_COVERAGE_ACCEPT_RELAX_MAX * s.coveragePressure;
        s.probeCap50 = AppConfig.V42_PROBE_CAP_PER_50;
        s.probeMinGap = AppConfig.V42_PROBE_MIN_GAP;

        if (s.settledSample < AppConfig.V42_RC_MIN_QUALITY_SAMPLE) {
            s.mode = s.frequency.betCount100 == 0 ? "ZERO_START" : "BOOTSTRAP";
            s.probeCap50 = AppConfig.V42_PROBE_CAP_PER_50;
            s.probeMinGap =
                    s.frequency.betCount100 == 0
                            ? AppConfig.V42_ZERO_START_PROBE_GAP
                            : AppConfig.V42_PROBE_MIN_GAP;
            s.allowProbe = s.coverage100 < AppConfig.V42_COVERAGE_TARGET;
        } else if (s.hitRate < AppConfig.V42_RC_HARD_CONTRACT_RATE
                || s.wilson80 < AppConfig.V42_RC_HARD_CONTRACT_W80) {
            s.mode = "QUALITY_CONTRACT";
            s.allowProbe = false;
            s.allowFloor =
                    s.coverage100 < AppConfig.V42_COVERAGE_MIN
                            && s.hitRate >= AppConfig.BREAK_EVEN_PROBABILITY;
            s.probeCap50 = 0;
            s.normalAcceptMin += 0.04d;
            s.floorAcceptMin += 0.05d;
            s.probeAcceptMin += 0.08d;
        } else if (s.hitRate >= AppConfig.V42_RC_EXPAND_RATE
                && s.wilson80 >= AppConfig.V42_RC_EXPAND_W80) {
            s.mode = "QUALITY_EXPAND";
            s.allowProbe = s.coverage100 < AppConfig.V42_COVERAGE_TARGET;
            s.floorAcceptMin -= 0.015d;
            s.probeAcceptMin -= 0.020d;
            s.probeMinGap = Math.max(1, AppConfig.V42_PROBE_MIN_GAP - 1);
        } else {
            s.mode = "QUALITY_STABLE";
            s.allowProbe = s.coverage100 < AppConfig.V42_COVERAGE_MIN;
        }

        s.normalAcceptMin = clamp(s.normalAcceptMin, 0.0d, 1.0d);
        s.floorAcceptMin = clamp(s.floorAcceptMin, AppConfig.V42_FLOOR_ACCEPT_HARD_MIN, 1.0d);
        s.probeAcceptMin = clamp(s.probeAcceptMin, AppConfig.V42_PROBE_ACCEPT_HARD_MIN, 1.0d);
    }

    public static synchronized void invalidate() {
        cached = null;
        cachedAt = 0L;
        cachedStoreIdentity = 0;
    }

    private static void loadSettledQuality(DbStore store, State s) {
        if (store == null || s == null) return;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            String where =
                    "settled=1 AND stat_scope='REAL_TRADE' AND source_type='REALTIME' AND"
                        + " is_realtime_real=1";
            c =
                    db.query(
                            DbHelper.TABLE_TRADES,
                            new String[] {"hit", "result", "bet_domain"},
                            where,
                            null,
                            null,
                            null,
                            "settle_time DESC, _id DESC",
                            String.valueOf(AppConfig.V42_RC_HISTORY_LIMIT));
            int row = 0;
            while (c != null && c.moveToNext()) {
                row++;
                boolean hit = getInt(c, "hit") == 1 || isWin(getString(c, "result"));
                s.settledSample++;
                if (hit) s.hitCount++;
                if (row <= 50
                        && AppConfig.BET_DOMAIN_REAL_BET_PROBE.equals(getString(c, "bet_domain"))) {
                    s.probeUsed50++;
                }
            }
        } catch (Throwable e) {
            try {
                store.setState("v42_risk_coverage_query_error", String.valueOf(e));
            } catch (Throwable ignore) {
            }
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        s.hitRate = s.settledSample <= 0 ? 0.0d : ((double) s.hitCount) / (double) s.settledSample;
        s.wilson80 = wilsonLower(s.hitCount, s.settledSample, 1.2815515655446004d);
        s.wilson95 = wilsonLower(s.hitCount, s.settledSample, 1.959963984540054d);
    }

    private static void persist(DbStore store, State s) {
        if (store == null || s == null) return;
        try {
            store.setState("v42_risk_coverage_version", MODULE_VERSION);
            store.setState("v42_risk_coverage_summary", s.summary());
            store.setState("v42_risk_coverage_mode", s.mode);
            store.setState("v42_risk_coverage_allow_probe", s.allowProbe ? "1" : "0");
        } catch (Throwable ignore) {
        }
    }

    private static double deficit(double actual, double target) {
        if (target <= 0.0d || actual >= target) return 0.0d;
        return clamp01((target - actual) / target);
    }

    private static boolean isWin(String r) {
        return "WIN".equals(r) || "HIT".equals(r) || "命中".equals(r);
    }

    private static int getInt(Cursor c, String name) {
        try {
            int i = c.getColumnIndex(name);
            return i < 0 ? 0 : c.getInt(i);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static String getString(Cursor c, String name) {
        try {
            int i = c.getColumnIndex(name);
            return i < 0 ? "" : safe(c.getString(i));
        } catch (Throwable e) {
            return "";
        }
    }

    private static double wilsonLower(int hit, int n, double z) {
        if (n <= 0) return 0.0d;
        double p = ((double) hit) / (double) n;
        double z2 = z * z;
        double denominator = 1.0d + z2 / n;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp01((center - margin) / denominator);
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
