package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Locale;

/**
 * V41 read-only OOS audit: reliability buckets, Top-K rank monotonicity and action-lane funnel. It
 * never changes probability, authorization or trades.
 */
public final class OosReliabilityAuditEngine {
    public static final String MODULE_VERSION = "V48_OOS_RELIABILITY_AUDIT_20260721_48072148";
    private static final int BUCKETS = 20; // 5 percentage points per bucket
    private static final int MAX_RANK = 5;

    private OosReliabilityAuditEngine() {}

    public static final class Report {
        public int sample;
        public int hit;
        public double hitRate;
        public double ece;
        public int raw;
        public int utilityQualified;
        public int probeCandidate;
        public int floorCandidate;
        public int normalCandidate;
        public int authorized;
        public int selectedBet;
        public int[] bucketN = new int[BUCKETS];
        public int[] bucketHit = new int[BUCKETS];
        public double[] bucketPredSum = new double[BUCKETS];
        public int[] rankN = new int[MAX_RANK + 1];
        public int[] rankHit = new int[MAX_RANK + 1];
        public String topKStatus = "INSUFFICIENT";
        public String summary = "";
    }

    public static Report build(DbStore store) {
        Report r = new Report();
        if (store == null) {
            r.summary = "V48 OOS审计=NO_STORE";
            return r;
        }
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            String rankColumn =
                    hasColumn(db, DbHelper.TABLE_OOS_ACTION_LOG, "rank_slot")
                            ? "rank_slot"
                            : (hasColumn(db, DbHelper.TABLE_OOS_ACTION_LOG, "oos_rank_slot")
                                    ? "oos_rank_slot"
                                    : "0 AS rank_slot");
            c =
                    db.query(
                            DbHelper.TABLE_OOS_ACTION_LOG,
                            new String[] {
                                "calibrated_probability",
                                "hit",
                                rankColumn,
                                "lane_candidate",
                                "selected_mode",
                                "authorization_valid",
                                "action_utility",
                                "uncertainty_penalty",
                                "honest_probability"
                            },
                            "settled=1 AND build_id=? AND cohort_id=? AND schema_version=? AND"
                                + " pipeline_version=? AND calibration_version=? AND version=?",
                            new String[] {
                                AppConfig.BUILD_ID,
                                AppConfig.COHORT_ID,
                                AppConfig.SCHEMA_VERSION,
                                AppConfig.PIPELINE_VERSION,
                                AppConfig.CALIBRATION_VERSION,
                                OosActionLogEngine.MODULE_VERSION
                            },
                            null,
                            null,
                            "target_issue DESC,_id DESC",
                            "1200");
            while (c != null && c.moveToNext()) {
                double p = clamp(c.getDouble(8) > 0.0d ? c.getDouble(8) : c.getDouble(0));
                int hit = c.getInt(1) == 1 ? 1 : 0;
                int rank = c.getInt(2);
                String lane = safe(c.getString(3));
                String mode = safe(c.getString(4));
                int auth = c.getInt(5);
                double utility = clamp(c.getDouble(6));
                double uncertainty = clamp(c.getDouble(7));
                int b = Math.min(BUCKETS - 1, Math.max(0, (int) Math.floor(p * BUCKETS)));
                r.sample++;
                r.hit += hit;
                r.raw++;
                r.bucketN[b]++;
                r.bucketHit[b] += hit;
                r.bucketPredSum[b] += p;
                if (rank >= 1 && rank <= MAX_RANK) {
                    r.rankN[rank]++;
                    r.rankHit[rank] += hit;
                }
                boolean utilityQualified =
                        p >= AppConfig.AUTH_REAL_PROBE_MIN_HONEST_P
                                && utility >= AppConfig.AUTH_REAL_PROBE_MIN_UTILITY
                                && uncertainty <= AppConfig.AUTH_REAL_PROBE_MAX_UNCERTAINTY;
                if (utilityQualified) r.utilityQualified++;
                if ("REAL_PROBE".equals(lane)) r.probeCandidate++;
                else if ("QUALIFIED_FLOOR".equals(lane)) r.floorCandidate++;
                else if ("NORMAL".equals(lane)) r.normalCandidate++;
                if (auth == 1) r.authorized++;
                if (AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(mode)))
                    r.selectedBet++;
            }
        } catch (Throwable e) {
            r.summary =
                    "V48 OOS审计=ERROR:" + e.getClass().getSimpleName() + ":" + safe(e.getMessage());
            return r;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        r.hitRate = rate(r.hit, r.sample);
        double weightedError = 0.0d;
        for (int i = 0; i < BUCKETS; i++) {
            if (r.bucketN[i] <= 0) continue;
            double pred = r.bucketPredSum[i] / r.bucketN[i];
            double actual = rate(r.bucketHit[i], r.bucketN[i]);
            weightedError += Math.abs(pred - actual) * r.bucketN[i];
        }
        r.ece = r.sample <= 0 ? 0.0d : weightedError / r.sample;
        r.topKStatus = rankStatus(r);
        r.summary = format(r);
        return r;
    }

    public static String buildAndPersist(DbStore store) {
        Report r = build(store);
        if (store != null) {
            try {
                store.setState("v28_oos_audit_version", MODULE_VERSION);
                store.setState("v28_oos_audit_sample", String.valueOf(r.sample));
                store.setState("v28_oos_audit_hit_rate", String.valueOf(r.hitRate));
                store.setState("v28_oos_audit_ece", String.valueOf(r.ece));
                store.setState("v28_oos_audit_rank_status", safe(r.topKStatus));
                store.setState("v28_oos_audit_summary", safe(r.summary));
                store.setState(
                        "v28_oos_audit_updated_at", String.valueOf(System.currentTimeMillis()));
            } catch (Throwable ignore) {
            }
        }
        return r.summary;
    }

    private static String rankStatus(Report r) {
        int usable = 0;
        double previous = 2.0d;
        boolean monotonic = true;
        for (int rank = 1; rank <= MAX_RANK; rank++) {
            if (r.rankN[rank] < 8) continue;
            double x = rate(r.rankHit[rank], r.rankN[rank]);
            if (usable > 0 && x > previous + 0.08d) monotonic = false;
            previous = x;
            usable++;
        }
        if (usable < 2) return "INSUFFICIENT";
        return monotonic ? "MONOTONIC_OK" : "RANK_INVERSION_WATCH";
    }

    private static String format(Report r) {
        StringBuilder sb = new StringBuilder();
        sb.append("V48 OOS可靠性审计：n=")
                .append(r.sample)
                .append("/hit=")
                .append(r.hit)
                .append("/rate=")
                .append(pct(r.hitRate))
                .append("/ECE=")
                .append(pct(r.ece))
                .append("｜漏斗 raw=")
                .append(r.raw)
                .append("→utilityQualified=")
                .append(r.utilityQualified)
                .append("→probe/floor/normal=")
                .append(r.probeCandidate)
                .append('/')
                .append(r.floorCandidate)
                .append('/')
                .append(r.normalCandidate)
                .append("→authorized=")
                .append(r.authorized)
                .append("→selectedBET=")
                .append(r.selectedBet)
                .append("｜TopK=")
                .append(r.topKStatus)
                .append(" [");
        for (int rank = 1; rank <= MAX_RANK; rank++) {
            if (rank > 1) sb.append(',');
            sb.append('R')
                    .append(rank)
                    .append('=')
                    .append(r.rankN[rank])
                    .append('/')
                    .append(pct(rate(r.rankHit[rank], r.rankN[rank])));
        }
        sb.append("]｜Reliability=");
        int shown = 0;
        for (int i = 0; i < BUCKETS; i++) {
            if (r.bucketN[i] <= 0) continue;
            if (shown++ > 0) sb.append(';');
            double pred = r.bucketPredSum[i] / r.bucketN[i];
            sb.append(pct(pred))
                    .append("→")
                    .append(pct(rate(r.bucketHit[i], r.bucketN[i])))
                    .append("(n=")
                    .append(r.bucketN[i])
                    .append(')');
        }
        if (shown == 0) sb.append("NO_SETTLED_OOS");
        return sb.toString();
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
        if (db == null || safe(table).length() == 0 || safe(column).length() == 0) return false;
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            while (c != null && c.moveToNext()) {
                int nameIndex = c.getColumnIndex("name");
                if (nameIndex >= 0 && column.equalsIgnoreCase(safe(c.getString(nameIndex))))
                    return true;
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        return false;
    }

    private static double rate(int h, int n) {
        return n <= 0 ? 0.0d : ((double) h) / n;
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.1f%%", clamp(v) * 100.0d);
    }

    private static double clamp(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
