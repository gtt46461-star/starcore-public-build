package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;

/** Storage-only V49 repository. It never creates a BET or changes a probability. */
public final class V49Repository {
    public static final String TABLE_SNAPSHOT = "v49_predraw_snapshot";
    public static final String TABLE_ACTION = "v49_position_action";
    public static final String TABLE_TEACHER = "v49_teacher_posterior";
    public static final String TABLE_EXPERT = "v49_expert_stats";
    public static final String TABLE_CALIBRATION = "v49_dirichlet_calibration";
    public static final String TABLE_OPPORTUNITY = "v49_best_opportunity";
    public static final String TABLE_ATTRIBUTION = "v49_cause_attribution";
    public static final String TABLE_UI_CACHE = "v49_ui_cache";
    private static volatile boolean schemaReady;

    private V49Repository() {}

    public static void ensure(DbStore store) {
        if (store == null || schemaReady) return;
        synchronized (V49Repository.class) {
            if (schemaReady) return;
            SQLiteDatabase db = store.writable();
            ensure(db);
            schemaReady = true;
        }
    }

    public static void ensure(SQLiteDatabase db) {
        if (db == null) return;
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_SNAPSHOT
                        + " (target_issue TEXT PRIMARY KEY,base_issue TEXT,created_at"
                        + " INTEGER,feature_checksum TEXT,state_checksum TEXT,raw_probability_blob"
                        + " TEXT,calibrated_probability_blob TEXT,expert_blob TEXT,top3_blob"
                        + " TEXT,opportunity_blob TEXT,lane TEXT,selected_position"
                        + " INTEGER,selected_action_p REAL,selected_score REAL,primary_cause"
                        + " TEXT,secondary_cause TEXT,counter_evidence TEXT,contract_hash"
                        + " TEXT,settled INTEGER DEFAULT 0,actual_blob TEXT,position_hit_count"
                        + " INTEGER DEFAULT 0,selected_hit INTEGER DEFAULT 0,attribution"
                        + " TEXT,version TEXT)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_ACTION
                        + " (_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT,position"
                        + " INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,action_p REAL,opportunity"
                        + " REAL,agreement REAL,uncertainty REAL,family TEXT,lifecycle"
                        + " TEXT,switch_risk REAL,family_strength REAL,lane TEXT,contract_hash"
                        + " TEXT,settled INTEGER DEFAULT 0,hit INTEGER DEFAULT"
                        + " 0,UNIQUE(target_issue,position) ON CONFLICT REPLACE)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_TEACHER
                        + " (teacher_key TEXT PRIMARY KEY,family TEXT,lifecycle TEXT,position"
                        + " INTEGER,number INTEGER,sample INTEGER DEFAULT 0,hit INTEGER DEFAULT"
                        + " 0,recent_sample REAL DEFAULT 0,recent_hit REAL DEFAULT 0,updated_at"
                        + " INTEGER)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_EXPERT
                        + " (expert_key TEXT PRIMARY KEY,expert_name TEXT,family TEXT,position"
                        + " INTEGER,sample INTEGER DEFAULT 0,logloss_sum REAL DEFAULT 0,brier_sum"
                        + " REAL DEFAULT 0,top3_hit INTEGER DEFAULT 0,recent_quality REAL DEFAULT"
                        + " 0.5,updated_at INTEGER)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_CALIBRATION
                        + " (position INTEGER PRIMARY KEY,a_diag TEXT,a_off REAL,bias TEXT,sample"
                        + " INTEGER DEFAULT 0,updated_at INTEGER,version TEXT)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_OPPORTUNITY
                        + " (target_issue TEXT PRIMARY KEY,score REAL,action_p REAL,lane"
                        + " TEXT,created_at INTEGER)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v49_opportunity_created ON "
                        + TABLE_OPPORTUNITY
                        + "(created_at DESC)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_ATTRIBUTION
                        + " (_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT,position"
                        + " INTEGER,result TEXT,primary_cause TEXT,secondary_cause"
                        + " TEXT,counter_evidence TEXT,ablation_blob TEXT,responsible_module"
                        + " TEXT,created_at INTEGER,version TEXT)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v49_attr_target ON "
                        + TABLE_ATTRIBUTION
                        + "(target_issue)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_UI_CACHE
                        + " (cache_key TEXT PRIMARY KEY,content TEXT,content_hash TEXT,updated_at"
                        + " INTEGER)");
    }

    public static double teacherPosterior(
            DbStore store, String family, String lifecycle, int position, int number) {
        ensure(store);
        String[] keys =
                new String[] {
                    teacherKey(family, lifecycle, position, number),
                    teacherKey(family, "*", position, number),
                    teacherKey("*", "*", position, number),
                    teacherKey("*", "*", 0, number)
                };
        double weighted = 0.0d, weight = 0.0d;
        for (int i = 0; i < keys.length; i++) {
            Cursor c = null;
            try {
                c =
                        store.readable()
                                .query(
                                        TABLE_TEACHER,
                                        new String[] {
                                            "sample", "hit", "recent_sample", "recent_hit"
                                        },
                                        "teacher_key=?",
                                        new String[] {keys[i]},
                                        null,
                                        null,
                                        null,
                                        "1");
                if (c.moveToFirst()) {
                    int sample = c.getInt(0);
                    int hit = c.getInt(1);
                    double rs = c.getDouble(2);
                    double rh = c.getDouble(3);
                    double priorStrength = i == 0 ? 18.0d : (i == 1 ? 28.0d : 45.0d);
                    double post = (hit + priorStrength * 0.10d) / (sample + priorStrength);
                    if (rs > 1.0d) {
                        double recent = (rh + 2.0d) / (rs + 20.0d);
                        post = 0.72d * post + 0.28d * recent;
                    }
                    double w = Math.min(1.0d, sample / (sample + priorStrength));
                    weighted += post * w;
                    weight += w;
                }
            } catch (Throwable ignore) {
            } finally {
                if (c != null) c.close();
            }
        }
        return weight <= 1.0e-9d ? 0.10d : FeatureContractRegistry.clamp01(weighted / weight);
    }

    public static double onlineResidual(
            DbStore store, String family, String lifecycle, int position, int number) {
        ensure(store);
        String key = teacherKey(family, lifecycle, position, number);
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .query(
                                    TABLE_TEACHER,
                                    new String[] {"sample", "hit", "recent_sample", "recent_hit"},
                                    "teacher_key=?",
                                    new String[] {key},
                                    null,
                                    null,
                                    null,
                                    "1");
            if (c.moveToFirst()) {
                int sample = c.getInt(0);
                int hit = c.getInt(1);
                double rs = c.getDouble(2);
                double rh = c.getDouble(3);
                if (sample < 8 || rs < 4.0d) return 0.0d;
                double longRate = (hit + 2.0d) / (sample + 20.0d);
                double recentRate = (rh + 1.0d) / (rs + 10.0d);
                double trust = Math.min(1.0d, rs / 40.0d);
                return Math.max(-0.035d, Math.min(0.035d, (recentRate - longRate) * trust));
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null) c.close();
        }
        return 0.0d;
    }

    public static double expertReliability(
            DbStore store, String expert, String family, int position) {
        ensure(store);
        String[] keys =
                new String[] {
                    expertKey(expert, family, position),
                    expertKey(expert, "*", position),
                    expertKey(expert, "*", 0)
                };
        for (int i = 0; i < keys.length; i++) {
            Cursor c = null;
            try {
                c =
                        store.readable()
                                .query(
                                        TABLE_EXPERT,
                                        new String[] {"sample", "recent_quality"},
                                        "expert_key=?",
                                        new String[] {keys[i]},
                                        null,
                                        null,
                                        null,
                                        "1");
                if (c.moveToFirst()) {
                    int sample = c.getInt(0);
                    double q = c.getDouble(1);
                    double shrink = sample / (sample + 40.0d);
                    return FeatureContractRegistry.clamp01(0.5d * (1.0d - shrink) + q * shrink);
                }
            } catch (Throwable ignore) {
            } finally {
                if (c != null) c.close();
            }
        }
        return 0.50d;
    }

    public static double opportunityThreshold(
            DbStore store, double targetCoverage, int maxRows, double fallback) {
        ensure(store);
        ArrayList<Double> scores = new ArrayList<Double>();
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .query(
                                    TABLE_OPPORTUNITY,
                                    new String[] {"score"},
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC",
                                    String.valueOf(Math.max(20, maxRows)));
            while (c.moveToNext()) scores.add(Double.valueOf(c.getDouble(0)));
        } catch (Throwable ignore) {
        } finally {
            if (c != null) c.close();
        }
        if (scores.size() < 40) return fallback;
        Collections.sort(scores);
        double q = 1.0d - FeatureContractRegistry.clamp01(targetCoverage);
        int index = (int) Math.floor(q * (scores.size() - 1));
        if (index < 0) index = 0;
        if (index >= scores.size()) index = scores.size() - 1;
        return scores.get(index).doubleValue();
    }

    public static void recordOpportunity(
            DbStore store, String targetIssue, double score, double actionP, String lane) {
        ensure(store);
        ContentValues cv = new ContentValues();
        cv.put("target_issue", safe(targetIssue));
        cv.put("score", score);
        cv.put("action_p", actionP);
        cv.put("lane", safe(lane));
        cv.put("created_at", System.currentTimeMillis());
        store.writable()
                .insertWithOnConflict(TABLE_OPPORTUNITY, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static int issuesSinceProbe(DbStore store) {
        ensure(store);
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*) FROM "
                                            + TABLE_OPPORTUNITY
                                            + " WHERE created_at > COALESCE((SELECT MAX(created_at)"
                                            + " FROM "
                                            + TABLE_OPPORTUNITY
                                            + " WHERE lane='CONTROLLED_PROBE'),0)",
                                    null);
            if (c.moveToFirst()) return c.getInt(0);
        } catch (Throwable ignore) {
        } finally {
            if (c != null) c.close();
        }
        return 999;
    }

    public static String teacherKey(String family, String lifecycle, int position, int number) {
        return safe(family) + '|' + safe(lifecycle) + "|P" + position + "|N" + number;
    }

    public static String expertKey(String expert, String family, int position) {
        return safe(expert) + '|' + safe(family) + "|P" + position;
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }
}
