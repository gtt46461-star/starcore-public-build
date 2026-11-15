package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * V40 online residual model.
 *
 * <p>The full-distribution forecaster owns the base event probability. This model may learn only a
 * bounded residual on top of logit(baseP) from immutable settled OOS rows. It never creates a
 * probability from a rank score and it never sees action frequency, decision mode or
 * settlement-derived diagnostic labels.
 */
public final class OnlineActionModelEngine {
    public static final String V46_ROLE =
            "BACKGROUND_OOS_CHALLENGER_NOT_REALTIME_PROBABILITY_OWNER";

    public static final String MODULE_VERSION = AppConfig.ONLINE_ACTION_MODEL_VERSION;
    public static final String STATE_VERSION = "v40_online_residual_version";
    public static final String STATE_SAMPLE = "v40_online_residual_sample";
    public static final String STATE_HIT = "v40_online_residual_hit";
    public static final String STATE_INTERCEPT = "v40_online_residual_intercept";
    public static final String STATE_WEIGHTS = "v40_online_residual_weights";
    public static final String STATE_BRIER = "v40_online_residual_brier_sum";
    public static final String STATE_UPDATED_AT = "v40_online_residual_updated_at";

    private OnlineActionModelEngine() {}

    public static final class FeatureVector {
        public final double[] x = new double[AppConfig.ACTION_MODEL_FEATURE_COUNT];
    }

    public static final class Snapshot {
        public double intercept;
        public final double[] weight = new double[AppConfig.ACTION_MODEL_FEATURE_COUNT];
        public int sample;
        public int hit;
        public double brierSum;
        public String version = MODULE_VERSION;

        public double predict(double baseProbability, FeatureVector f) {
            double base =
                    clamp(
                            baseProbability,
                            AppConfig.CALIBRATED_PROBABILITY_MIN,
                            AppConfig.CALIBRATED_PROBABILITY_MAX);
            double z = logit(base) + intercept;
            if (f != null) {
                int n = Math.min(weight.length, f.x.length);
                for (int i = 1; i < n; i++) z += weight[i] * centered(f.x[i]);
            }
            /* Residual freedom is earned only by settled OOS samples. */
            double maturity =
                    Math.min(
                            1.0d,
                            sample
                                    / (double)
                                            Math.max(1, AppConfig.CALIBRATION_READY_TOTAL_SAMPLE));
            double adjustedLogit = logit(base) + maturity * (z - logit(base));
            return clamp(
                    sigmoid(adjustedLogit),
                    AppConfig.CALIBRATED_PROBABILITY_MIN,
                    AppConfig.CALIBRATED_PROBABILITY_MAX);
        }
    }

    private static final String[] STATE_KEYS_CP12={STATE_VERSION,STATE_SAMPLE,STATE_HIT,STATE_INTERCEPT,STATE_WEIGHTS,STATE_BRIER,STATE_UPDATED_AT};
    public static Snapshot load(DbStore store) {
        if(store==null)throw new IllegalArgumentException("MODEL_STORE_NULL");
        return loadStateCP12(store.readable());
    }
    public static Snapshot loadWithinTransactionCP3(android.database.sqlite.SQLiteDatabase db) {
        requireTransactionCP12(db);return loadStateCP12(db);
    }
    private static Snapshot loadStateCP12(android.database.sqlite.SQLiteDatabase db) {
        java.util.Map<String,String> values=new java.util.HashMap<String,String>();
        try(Cursor c=db.rawQuery("SELECT state_key,state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key IN (?,?,?,?,?,?,?)",STATE_KEYS_CP12)){
            while(c.moveToNext())values.put(c.getString(0),c.getString(1));}
        return decodeStateCP12(values);
    }
    static Snapshot decodeStateCP12(java.util.Map<String,String> v) {
        Snapshot s=new Snapshot();if(v.isEmpty())return s;
        if(v.size()!=STATE_KEYS_CP12.length)throw new IllegalStateException("CP3_MODEL_STATE_TORN");
        for(String k:STATE_KEYS_CP12)if(v.get(k)==null)throw new IllegalStateException("CP3_MODEL_STATE_TORN");
        if(!MODULE_VERSION.equals(v.get(STATE_VERSION)))throw new IllegalStateException("MODEL_VERSION_REBUILD_REQUIRED");
        try {
            s.sample=Integer.parseInt(v.get(STATE_SAMPLE));s.hit=Integer.parseInt(v.get(STATE_HIT));
            s.intercept=Double.parseDouble(v.get(STATE_INTERCEPT));s.brierSum=Double.parseDouble(v.get(STATE_BRIER));
            if(Long.parseLong(v.get(STATE_UPDATED_AT))<0)throw new IllegalArgumentException("MODEL_TIMESTAMP");
            String[] weights=v.get(STATE_WEIGHTS).split(",",-1);
            if(weights.length!=s.weight.length)throw new IllegalArgumentException("MODEL_WEIGHTS_LENGTH");
            for(int i=0;i<weights.length;i++)s.weight[i]=Double.parseDouble(weights[i]);validateCP12(s);return s;
        }catch(IllegalArgumentException e){throw new IllegalStateException("CP3_MODEL_STATE_TORN",e);}
    }
    private static void validateCP12(Snapshot s) {
        if(s==null||s.sample<0||s.hit<0||s.hit>s.sample||!Double.isFinite(s.intercept)
            ||Math.abs(s.intercept)>AppConfig.ACTION_MODEL_WEIGHT_LIMIT||!Double.isFinite(s.brierSum)
            ||s.brierSum<0||s.brierSum>s.sample+1e-8)throw new IllegalArgumentException("CP3_MODEL_STATE_INVALID");
        for(double w:s.weight)if(!Double.isFinite(w)||Math.abs(w)>AppConfig.ACTION_MODEL_WEIGHT_LIMIT)
            throw new IllegalArgumentException("CP3_MODEL_WEIGHT_INVALID");
    }
    private static void requireTransactionCP12(android.database.sqlite.SQLiteDatabase db) {
        if(db==null||!db.inTransaction())throw new IllegalStateException("CP3_MODEL_TX_REQUIRED");
    }
    public static void persistWithinTransactionCP3(android.database.sqlite.SQLiteDatabase db,Snapshot s,long now) {
        requireTransactionCP12(db);validateCP12(s);if(now<0)throw new IllegalArgumentException("MODEL_TIMESTAMP");
        StringBuilder weights=new StringBuilder();for(int i=0;i<s.weight.length;i++){if(i>0)weights.append(',');weights.append(s.weight[i]);}
        String[] vals={MODULE_VERSION,String.valueOf(s.sample),String.valueOf(s.hit),String.valueOf(s.intercept),weights.toString(),String.valueOf(s.brierSum),String.valueOf(now)};
        for(int i=0;i<STATE_KEYS_CP12.length;i++){
            ContentValues cv=new ContentValues();cv.put("state_key",STATE_KEYS_CP12[i]);cv.put("state_value",vals[i]);cv.put("updated_at",now);
            if(db.insertWithOnConflict(DbHelper.TABLE_APP_STATE,null,cv,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)<0)
                throw new IllegalStateException("CP3_MODEL_PERSIST_FAILED");}
    }
    public static void persist(final DbStore store,final Snapshot s) {
        if(store==null)throw new IllegalArgumentException("MODEL_STORE_NULL");validateCP12(s);
        Boolean committed=store.coordinatorR36().runAtomicWrite("CP12_SHADOW_MODEL",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(android.database.sqlite.SQLiteDatabase db){persistWithinTransactionCP3(db,s,System.currentTimeMillis());return Boolean.TRUE;}},Boolean.FALSE);
        if(!Boolean.TRUE.equals(committed))throw new IllegalStateException("CP3_MODEL_PERSIST_FAILED");
    }

    public static FeatureVector extract(Models.ComboCandidate c) {
        FeatureVector f = new FeatureVector();
        if (c == null) {
            f.x[0] = AppConfig.RANDOM_K3_BASELINE;
            return f;
        }
        double base =
                firstProbability(
                        c.basePBeforeFusion,
                        c.rawP,
                        c.historicalPriorP,
                        c.modelP,
                        AppConfig.RANDOM_K3_BASELINE);
        f.x[0] = clamp(base, 0.0d, 1.0d);
        f.x[1] =
                clamp(
                        firstProbability(
                                c.historicalPriorP, c.similarP, AppConfig.RANDOM_K3_BASELINE),
                        0.0d,
                        1.0d);
        f.x[2] =
                clamp(
                        max(
                                c.evidenceForestConfidence,
                                c.historicalPriorConfidence,
                                c.deepConfidence),
                        0.0d,
                        1.0d);
        f.x[3] =
                clamp(
                        max(
                                c.coverageQuality,
                                c.coverageQualityScore,
                                c.dynamicGateCoverageQualityScore),
                        0.0d,
                        1.0d);
        f.x[4] =
                clamp(
                        max(
                                c.thirdQuality,
                                c.thirdNumberQualityScore,
                                c.dynamicGateThirdNumberQuality),
                        0.0d,
                        1.0d);
        f.x[5] =
                1.0d
                        - clamp(
                                max(c.counterRisk, c.counterEvidenceScore, c.evidenceRisk),
                                0.0d,
                                1.0d);
        f.x[6] = 1.0d - clamp(max(c.comboConflict, c.conflictLevel, c.falseStrongRisk), 0.0d, 1.0d);
        f.x[7] = 1.0d - clamp(Math.max(0.0d, c.calibrationError) / 0.15d, 0.0d, 1.0d);
        return f;
    }

    public static double predict(Snapshot snapshot, Models.ComboCandidate c) {
        Snapshot s = snapshot == null ? new Snapshot() : snapshot;
        FeatureVector f = extract(c);
        double p = s.predict(f.x[0], f);
        if (c != null) c.modelP = p;
        return p;
    }

    public static void update(Snapshot s, FeatureVector f, int hit) {
        if (s == null || f == null) return;
        int y = hit == 1 ? 1 : 0;
        double base =
                clamp(
                        f.x[0],
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);
        double z = logit(base) + s.intercept;
        for (int i = 1; i < s.weight.length && i < f.x.length; i++) {
            z += s.weight[i] * centered(f.x[i]);
        }
        double p =
                clamp(
                        sigmoid(z),
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);
        double err = y - p;
        double lr =
                AppConfig.ACTION_MODEL_LEARNING_RATE
                        / Math.sqrt(1.0d + Math.max(0, s.sample) / 32.0d);
        s.intercept =
                clamp(
                        s.intercept + lr * err,
                        -AppConfig.ACTION_MODEL_WEIGHT_LIMIT,
                        AppConfig.ACTION_MODEL_WEIGHT_LIMIT);
        for (int i = 1; i < s.weight.length && i < f.x.length; i++) {
            double grad = err * centered(f.x[i]) - AppConfig.ACTION_MODEL_L2 * s.weight[i];
            s.weight[i] =
                    clamp(
                            s.weight[i] + lr * grad,
                            -AppConfig.ACTION_MODEL_WEIGHT_LIMIT,
                            AppConfig.ACTION_MODEL_WEIGHT_LIMIT);
        }
        s.sample++;
        if (y == 1) s.hit++;
        s.brierSum += (p - y) * (p - y);
    }

    public static void putFeatureValues(ContentValues v, Models.ComboCandidate c) {
        if (v == null) return;
        FeatureVector f = extract(c);
        for (int i = 0; i < f.x.length; i++) v.put("feature_" + i, f.x[i]);
        v.put("model_version", MODULE_VERSION);
    }

    public static FeatureVector readFeatureVector(Cursor c, int firstColumn) {
        FeatureVector f = new FeatureVector();
        if (c == null) {
            f.x[0] = AppConfig.RANDOM_K3_BASELINE;
            return f;
        }
        for (int i = 0; i < f.x.length; i++) {
            try {
                f.x[i] = clamp(c.getDouble(firstColumn + i), 0.0d, 1.0d);
            } catch (Throwable ignore) {
                f.x[i] = i == 0 ? AppConfig.RANDOM_K3_BASELINE : 0.5d;
            }
        }
        return f;
    }

    public static double empiricalRate(Snapshot s) {
        if (s == null || s.sample <= 0) return AppConfig.RANDOM_K3_BASELINE;
        double prior = AppConfig.OOS_CALIBRATION_PRIOR_STRENGTH;
        return (s.hit + prior * AppConfig.RANDOM_K3_BASELINE) / (s.sample + prior);
    }

    private static double centered(double v) {
        return clamp(v, 0.0d, 1.0d) - 0.5d;
    }

    private static double sigmoid(double z) {
        if (z > 35.0d) return 1.0d;
        if (z < -35.0d) return 0.0d;
        return 1.0d / (1.0d + Math.exp(-z));
    }

    private static double logit(double p) {
        p = clamp(p, 0.0001d, 0.9999d);
        return Math.log(p / (1.0d - p));
    }

    private static double firstProbability(
            double a, double b, double c, double d, double fallback) {
        if (validProbability(a)) return a;
        if (validProbability(b)) return b;
        if (validProbability(c)) return c;
        if (validProbability(d)) return d;
        return fallback;
    }

    private static double firstProbability(double a, double b, double fallback) {
        if (validProbability(a)) return a;
        if (validProbability(b)) return b;
        return fallback;
    }

    private static boolean validProbability(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static double parseDouble(String s, double d) {
        try {
            return Double.parseDouble(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double max(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
