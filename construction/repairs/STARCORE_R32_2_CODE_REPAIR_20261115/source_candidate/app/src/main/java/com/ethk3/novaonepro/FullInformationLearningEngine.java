package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Learns from all ten position actions after every draw, independently of whether a real Trade
 * existed. Real BET accounting remains untouched.
 */
public final class FullInformationLearningEngine {
    public static final String MODULE_VERSION =
            V49BuildManifest.STRATEGY_VERSION + ":FULL_INFORMATION_LEARNING";
    private final V49DirichletCalibrationEngine calibration = new V49DirichletCalibrationEngine();

    public Report settleAndLearn(DbStore store, Models.Draw draw) {
        Report r = new Report();
        if (store == null || draw == null || !draw.isValid()) {
            r.status = "INVALID_INPUT";
            return r;
        }
        V49Repository.ensure(store);
        SQLiteDatabase db = store.writable();
        Cursor snapshot = null;
        try {
            snapshot =
                    db.query(
                            V49Repository.TABLE_SNAPSHOT,
                            new String[] {
                                "raw_probability_blob",
                                "calibrated_probability_blob",
                                "expert_blob",
                                "selected_position",
                                "primary_cause",
                                "secondary_cause",
                                "counter_evidence"
                            },
                            "target_issue=? AND settled=0",
                            new String[] {safe(draw.issue)},
                            null,
                            null,
                            null,
                            "1");
            if (!snapshot.moveToFirst()) {
                r.status = "NO_PREDRAW_SNAPSHOT";
                persist(store, r, draw.issue);
                return r;
            }
            double[][] raw = parseMatrix(snapshot.getString(0));
            double[][] calibrated = parseMatrix(snapshot.getString(1));
            String expertBlob = safe(snapshot.getString(2));
            int selectedPosition = snapshot.getInt(3);
            String primary = safe(snapshot.getString(4));
            String secondary = safe(snapshot.getString(5));
            String counter = safe(snapshot.getString(6));

            db.beginTransaction();
            try {
                Cursor action =
                        db.query(
                                V49Repository.TABLE_ACTION,
                                new String[] {
                                    "position", "n1", "n2", "n3", "family", "lifecycle", "lane"
                                },
                                "target_issue=? AND settled=0",
                                new String[] {safe(draw.issue)},
                                null,
                                null,
                                "position ASC");
                try {
                    while (action.moveToNext()) {
                        int position = action.getInt(0);
                        int n1 = action.getInt(1), n2 = action.getInt(2), n3 = action.getInt(3);
                        String family = safe(action.getString(4));
                        String lifecycle = safe(action.getString(5));
                        int actual = draw.getNumberAtPosition(position);
                        int hit = actual == n1 || actual == n2 || actual == n3 ? 1 : 0;
                        ContentValues av = new ContentValues();
                        av.put("settled", 1);
                        av.put("hit", hit);
                        db.update(
                                V49Repository.TABLE_ACTION,
                                av,
                                "target_issue=? AND position=?",
                                new String[] {safe(draw.issue), String.valueOf(position)});
                        r.positionActions++;
                        r.positionHits += hit;
                        if (position == selectedPosition) r.selectedHit = hit;

                        updateTeacherFamily(db, family, lifecycle, position, actual);
                        updateTeacherFamily(db, family, "*", position, actual);
                        updateTeacherFamily(db, "*", "*", position, actual);
                        updateTeacherFamily(db, "*", "*", 0, actual);
                        if (position >= 1 && position <= 10 && raw[position - 1] != null) {
                            calibration.update(store, position, raw[position - 1], actual);
                        }
                    }
                } finally {
                    action.close();
                }

                updateExpertStats(db, expertBlob, draw);
                int selectedActual =
                        selectedPosition >= 1 && selectedPosition <= 10
                                ? draw.getNumberAtPosition(selectedPosition)
                                : 0;
                String ablation =
                        V49CauseAttributionEngine.buildAblation(
                                expertBlob, selectedPosition, selectedActual, calibrated);
                String attribution =
                        V49CauseAttributionEngine.classify(
                                        selectedPosition,
                                        r.selectedHit,
                                        r.positionHits,
                                        primary,
                                        secondary,
                                        counter,
                                        calibrated,
                                        draw)
                                + "|"
                                + ablation;
                ContentValues sv = new ContentValues();
                sv.put("settled", 1);
                sv.put("actual_blob", drawNumbers(draw));
                sv.put("position_hit_count", r.positionHits);
                sv.put("selected_hit", r.selectedHit);
                sv.put("attribution", attribution);
                db.update(
                        V49Repository.TABLE_SNAPSHOT,
                        sv,
                        "target_issue=?",
                        new String[] {safe(draw.issue)});
                V49CauseAttributionEngine.persist(
                        db,
                        draw.issue,
                        selectedPosition,
                        r.selectedHit == 1 ? "HIT" : "MISS",
                        primary,
                        secondary,
                        counter,
                        attribution);
                db.setTransactionSuccessful();
                r.attribution = attribution;
            } finally {
                db.endTransaction();
            }
            r.status = "LEARNED";
            persist(store, r, draw.issue);
            return r;
        } catch (Throwable e) {
            r.status = "ERROR:" + e.getClass().getSimpleName() + ":" + safe(e.getMessage());
            persist(store, r, draw.issue);
            return r;
        } finally {
            if (snapshot != null)
                try {
                    snapshot.close();
                } catch (Throwable ignore) {
                }
        }
    }

    private static void updateTeacherFamily(
            SQLiteDatabase db, String family, String lifecycle, int position, int actual) {
        for (int number = 1; number <= 10; number++) {
            String key = V49Repository.teacherKey(family, lifecycle, position, number);
            Cursor c =
                    db.query(
                            V49Repository.TABLE_TEACHER,
                            new String[] {"sample", "hit", "recent_sample", "recent_hit"},
                            "teacher_key=?",
                            new String[] {key},
                            null,
                            null,
                            null,
                            "1");
            int sample = 0, hit = 0;
            double rs = 0.0d, rh = 0.0d;
            try {
                if (c.moveToFirst()) {
                    sample = c.getInt(0);
                    hit = c.getInt(1);
                    rs = c.getDouble(2);
                    rh = c.getDouble(3);
                }
            } finally {
                c.close();
            }
            int y = number == actual ? 1 : 0;
            ContentValues v = new ContentValues();
            v.put("teacher_key", key);
            v.put("family", family);
            v.put("lifecycle", lifecycle);
            v.put("position", position);
            v.put("number", number);
            v.put("sample", sample + 1);
            v.put("hit", hit + y);
            v.put("recent_sample", Math.min(60.0d, rs * 0.96d + 1.0d));
            v.put("recent_hit", Math.min(60.0d, rh * 0.96d + y));
            v.put("updated_at", System.currentTimeMillis());
            db.insertWithOnConflict(
                    V49Repository.TABLE_TEACHER, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private static void updateExpertStats(SQLiteDatabase db, String blob, Models.Draw draw) {
        if (blob.length() == 0) return;
        String[] experts = blob.split("~");
        for (int e = 0; e < experts.length; e++) {
            String[] part = experts[e].split("#", -1);
            if (part.length < 2) continue;
            String name = part[0];
            double[][] matrix = parseMatrix(part[1]);
            for (int p = 0; p < 10; p++) {
                int actual = draw.getNumberAtPosition(p + 1);
                if (actual < 1 || actual > 10) continue;
                double prob = matrix[p][actual - 1];
                double logloss = -Math.log(Math.max(1.0e-12d, prob));
                double brier = 0.0d;
                for (int n = 0; n < 10; n++) {
                    double y = n == actual - 1 ? 1.0d : 0.0d;
                    double d = matrix[p][n] - y;
                    brier += d * d;
                }
                int top3 = contains(V49ExpertSupport.top3(matrix[p]), actual - 1) ? 1 : 0;
                String key = V49Repository.expertKey(name, "*", p + 1);
                Cursor c =
                        db.query(
                                V49Repository.TABLE_EXPERT,
                                new String[] {
                                    "sample",
                                    "logloss_sum",
                                    "brier_sum",
                                    "top3_hit",
                                    "recent_quality"
                                },
                                "expert_key=?",
                                new String[] {key},
                                null,
                                null,
                                null,
                                "1");
                int sample = 0, hit = 0;
                double ll = 0.0d, bs = 0.0d, rq = 0.5d;
                try {
                    if (c.moveToFirst()) {
                        sample = c.getInt(0);
                        ll = c.getDouble(1);
                        bs = c.getDouble(2);
                        hit = c.getInt(3);
                        rq = c.getDouble(4);
                    }
                } finally {
                    c.close();
                }
                ContentValues v = new ContentValues();
                v.put("expert_key", key);
                v.put("expert_name", name);
                v.put("family", "*");
                v.put("position", p + 1);
                v.put("sample", sample + 1);
                v.put("logloss_sum", ll + logloss);
                v.put("brier_sum", bs + brier);
                v.put("top3_hit", hit + top3);
                v.put("recent_quality", FeatureContractRegistry.clamp01(rq * 0.96d + top3 * 0.04d));
                v.put("updated_at", System.currentTimeMillis());
                db.insertWithOnConflict(
                        V49Repository.TABLE_EXPERT, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    private static double[][] parseMatrix(String text) {
        double[][] out = new double[10][10];
        String[] rows = safe(text).split(";", -1);
        for (int p = 0; p < 10; p++) {
            String[] values = p < rows.length ? rows[p].split(",", -1) : new String[0];
            for (int n = 0; n < 10; n++) {
                try {
                    out[p][n] = n < values.length ? Double.parseDouble(values[n]) : 0.1d;
                } catch (Throwable ignore) {
                    out[p][n] = 0.1d;
                }
            }
            FeatureContractRegistry.normalize10(out[p]);
        }
        return out;
    }

    private static boolean contains(int[] x, int v) {
        if (x != null) for (int i = 0; i < x.length; i++) if (x[i] == v) return true;
        return false;
    }

    private static String drawNumbers(Models.Draw d) {
        StringBuilder b = new StringBuilder();
        for (int p = 1; p <= 10; p++) {
            if (p > 1) b.append(',');
            b.append(d.getNumberAtPosition(p));
        }
        return b.toString();
    }

    private static void persist(DbStore store, Report r, String issue) {
        try {
            store.setState("v49_full_information_learning_version", MODULE_VERSION);
            store.setState("v49_full_information_learning_issue", safe(issue));
            store.setState("v49_full_information_learning_status", r.status);
            store.setState("v49_full_information_learning_summary", r.summary());
        } catch (Throwable ignore) {
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final class Report {
        public String status = "NOT_RUN";
        public int positionActions;
        public int positionHits;
        public int selectedHit;
        public String attribution = "";

        public String summary() {
            return "status="
                    + status
                    + "|actions="
                    + positionActions
                    + "|positionHits="
                    + positionHits
                    + "|selectedHit="
                    + selectedHit
                    + "|attribution="
                    + attribution;
        }
    }
}
