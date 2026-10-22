package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Online multiclass Dirichlet calibration over log probabilities. */
public final class V49DirichletCalibrationEngine {
    public static final String MODULE_VERSION = V49BuildManifest.CALIBRATION_SCHEMA;

    public double[] apply(DbStore store, int position, double[] p) {
        Params z = load(store, position);
        double[] score = new double[10];
        double sumLog = 0.0d;
        for (int i = 0; i < 10; i++) sumLog += FeatureContractRegistry.safeLog(p[i]);
        for (int i = 0; i < 10; i++) {
            double li = FeatureContractRegistry.safeLog(p[i]);
            double others = (sumLog - li) / 9.0d;
            score[i] = z.diag[i] * li + z.off * others + z.bias[i];
        }
        double[] out = new double[10];
        V49ExpertSupport.softmax(score, out);
        return out;
    }

    public void update(DbStore store, int position, double[] raw, int actualNumber) {
        if (store == null
                || raw == null
                || raw.length != 10
                || actualNumber < 1
                || actualNumber > 10) return;
        V49Repository.ensure(store);
        Params z = load(store, position);
        double[] calibrated = apply(store, position, raw);
        double lr = 0.012d / Math.sqrt(1.0d + z.sample / 80.0d);
        double sumLog = 0.0d;
        for (int i = 0; i < 10; i++) sumLog += FeatureContractRegistry.safeLog(raw[i]);
        double offGrad = 0.0d;
        for (int i = 0; i < 10; i++) {
            double y = actualNumber == i + 1 ? 1.0d : 0.0d;
            double error = calibrated[i] - y;
            double li = FeatureContractRegistry.safeLog(raw[i]);
            double others = (sumLog - li) / 9.0d;
            z.bias[i] = clip(z.bias[i] - lr * error, -0.35d, 0.35d);
            z.diag[i] = clip(z.diag[i] - lr * error * li, 0.65d, 1.45d);
            offGrad += error * others;
        }
        z.off = clip(z.off - lr * offGrad / 10.0d, -0.30d, 0.30d);
        z.sample++;
        save(store, position, z);
    }

    private Params load(DbStore store, int position) {
        Params z = new Params();
        if (store == null) return z;
        V49Repository.ensure(store);
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .query(
                                    V49Repository.TABLE_CALIBRATION,
                                    new String[] {"a_diag", "a_off", "bias", "sample"},
                                    "position=?",
                                    new String[] {String.valueOf(position)},
                                    null,
                                    null,
                                    null,
                                    "1");
            if (c.moveToFirst()) {
                parse(c.getString(0), z.diag, 1.0d);
                z.off = c.getDouble(1);
                parse(c.getString(2), z.bias, 0.0d);
                z.sample = c.getInt(3);
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null) c.close();
        }
        return z;
    }

    private void save(DbStore store, int position, Params z) {
        ContentValues cv = new ContentValues();
        cv.put("position", position);
        cv.put("a_diag", join(z.diag));
        cv.put("a_off", z.off);
        cv.put("bias", join(z.bias));
        cv.put("sample", z.sample);
        cv.put("updated_at", System.currentTimeMillis());
        cv.put("version", MODULE_VERSION);
        store.writable()
                .insertWithOnConflict(
                        V49Repository.TABLE_CALIBRATION, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void parse(String s, double[] out, double fallback) {
        for (int i = 0; i < out.length; i++) out[i] = fallback;
        if (s == null) return;
        String[] a = s.split(",");
        for (int i = 0; i < out.length && i < a.length; i++) {
            try {
                out[i] = Double.parseDouble(a[i]);
            } catch (Throwable ignore) {
            }
        }
    }

    private static String join(double[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            if (i > 0) b.append(',');
            b.append(Double.toString(x[i]));
        }
        return b.toString();
    }

    private static double clip(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static final class Params {
        final double[] diag = new double[10];
        final double[] bias = new double[10];
        double off;
        int sample;

        Params() {
            for (int i = 0; i < 10; i++) diag[i] = 1.0d;
            off = 0.0d;
            sample = 0;
        }
    }
}
