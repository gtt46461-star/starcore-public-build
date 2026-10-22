package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;

/** Action-level calibration: one settled Rank1 diagnostic observation per target issue. */
public final class RankConditionalCalibrationEngine {
    public static final String MODULE_VERSION =
            "V48_RANK_CONDITIONAL_CALIBRATION_20260721_48072148";

    public static final class Bucket {
        int n, hit;

        double rate() {
            return (hit + 3.0d) / (n + 10.0d);
        }
    }

    public static final class Snapshot {
        Bucket global = new Bucket();
        HashMap<Integer, Bucket> position = new HashMap<Integer, Bucket>();
        HashMap<Integer, Bucket> block = new HashMap<Integer, Bucket>();
        String status = "EMPTY";

        String summary() {
            return status + "|global=" + global.hit + "/" + global.n + "|rate=" + global.rate();
        }
    }

    private RankConditionalCalibrationEngine() {}

    public static Snapshot load(DbStore store) {
        Snapshot s = new Snapshot();
        if (store == null) return s;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.query(
                            DbHelper.TABLE_V48_DIAGNOSTIC_OBSERVATION,
                            new String[] {"target_issue", "position", "hit"},
                            "settled=1 AND build_id=? AND cohort_id=? AND schema_version=? AND"
                                + " pipeline_version=? AND model_version=?",
                            new String[] {
                                V48BuildManifest.BUILD_ID,
                                V48BuildManifest.COHORT_ID,
                                V48BuildManifest.SCHEMA_VERSION,
                                V48BuildManifest.STRATEGY_VERSION,
                                V48BuildManifest.MODEL_VERSION
                            },
                            null,
                            null,
                            "target_issue DESC",
                            "2000");
            while (c != null && c.moveToNext()) {
                String issue = c.getString(0);
                int p = c.getInt(1), hit = c.getInt(2) == 1 ? 1 : 0;
                add(s.global, hit);
                add(bucket(s.position, p), hit);
                int b = timeBlock(issue);
                add(bucket(s.block, b), hit);
            }
            s.status = s.global.n >= 30 ? "READY" : "WARM";
        } catch (Throwable e) {
            s.status = "ERROR:" + e.getClass().getSimpleName();
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        try {
            store.setState("v48_rank_conditional_calibration", s.summary());
        } catch (Throwable ignore) {
        }
        return s;
    }

    public static void apply(Snapshot s, Models.ComboCandidate c) {
        if (c == null) return;
        double base =
                valid(c.v46EventProbability)
                        ? c.v46EventProbability
                        : (valid(c.modelP) ? c.modelP : 0.30d);
        if (s == null) {
            bound(c, base, 0);
            return;
        }
        double global = s.global.rate();
        Bucket pb = s.position.get(c.position);
        Bucket tb = s.block.get(timeBlock(c.targetIssue));
        double position = pb == null ? global : shrink(pb.rate(), pb.n, global, 60);
        double block = tb == null ? global : shrink(tb.rate(), tb.n, global, 80);
        double actionRate = 0.50d * global + 0.30d * position + 0.20d * block;
        double confidence = Math.min(1.0d, s.global.n / 240.0d);
        double adjustment = confidence * 0.25d * (actionRate - 0.30d);
        double event = clamp(base + adjustment, base - 0.025d, base + 0.025d);
        bound(c, event, confidence);
        c.v46PositionAdjustment = event - base;
        c.v46RankAdjustment = 0.0d;
        c.v46RegimeAdjustment = 0.0d;
        c.calibrationVersion = MODULE_VERSION;
        c.v46ProbabilityTrace =
                append(c.v46ProbabilityTrace, "V48ActionCal=" + event + "/n=" + s.global.n);
    }

    private static void bound(Models.ComboCandidate c, double event, double confidence) {
        double uncertainty = 0.012d + 0.035d * (1.0d - confidence);
        c.v46GlobalCalibratedProbability = event;
        c.v46EventProbability = event;
        c.v46ActionLcb = clamp(event - uncertainty, 0.001d, event);
        c.v46ProbabilityUncertainty = clamp(uncertainty / 0.060d, 0, 1);
        c.v46ExpectedValue = AppConfig.ODDS * event - AppConfig.BET_COST;
        c.v46CalibrationConfidence = confidence;
        c.finalP = event;
        c.calibratedP = event;
        c.calibratedHitProbability = event;
        c.honestProbability = event;
        c.v43SafeProbability = c.v46ActionLcb;
        c.v46Version = AppConfig.V48_VERSION;
    }

    private static Bucket bucket(HashMap<Integer, Bucket> map, int key) {
        Integer boxed = Integer.valueOf(key);
        Bucket value = map.get(boxed);
        if (value == null) {
            value = new Bucket();
            map.put(boxed, value);
        }
        return value;
    }

    private static void add(Bucket b, int hit) {
        b.n++;
        b.hit += hit;
    }

    private static double shrink(double rate, int n, double prior, int strength) {
        return (n * rate + strength * prior) / (n + strength);
    }

    private static int timeBlock(String issue) {
        try {
            int value = Integer.parseInt(issue.substring(Math.max(0, issue.length() - 3)));
            int mod = value % 4;
            return mod < 0 ? mod + 4 : mod;
        } catch (Throwable e) {
            return 0;
        }
    }

    private static boolean valid(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0 && v < 1;
    }

    private static double clamp(double v, double lo, double hi) {
        return !Double.isNaN(v) && !Double.isInfinite(v) ? Math.max(lo, Math.min(hi, v)) : lo;
    }

    private static String append(String a, String b) {
        return a == null || a.isEmpty() ? b : (a.contains(b) ? a : a + '|' + b);
    }
}
