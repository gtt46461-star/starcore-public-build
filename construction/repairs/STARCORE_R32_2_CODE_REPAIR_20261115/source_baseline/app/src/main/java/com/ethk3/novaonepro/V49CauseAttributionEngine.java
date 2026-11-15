package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/** Prediction-time hypothesis versus settled-result responsibility classifier. */
public final class V49CauseAttributionEngine {
    public static final String MODULE_VERSION =
            V49BuildManifest.STRATEGY_VERSION + ":WHY_HIT_WHY_MISS";

    private V49CauseAttributionEngine() {}

    public static String classify(
            int selectedPosition,
            int selectedHit,
            int positionHitCount,
            String primary,
            String secondary,
            String counter,
            double[][] probability,
            Models.Draw draw) {
        if (selectedPosition <= 0 || selectedPosition > 10) return "POSITION_SELECTION_MISSING";
        int actual = draw == null ? 0 : draw.getNumberAtPosition(selectedPosition);
        int rank =
                actualRank(probability == null ? null : probability[selectedPosition - 1], actual);
        if (selectedHit == 1) {
            if (rank == 3) return "THIRD_HIT|主因=" + safe(primary) + "|次因=" + safe(secondary);
            if (rank <= 2) return "STRUCTURAL_HIT|主因=" + safe(primary) + "|反证=" + safe(counter);
            return "LUCKY_HIT|证据与结果不一致";
        }
        if (rank == 4) return "RANK_INVERSION|实际号码位于第4名|反证=" + safe(counter);
        if (positionHitCount > 0) return "POSITION_SELECTION_MISS|其他位置Top3命中=" + positionHitCount;
        String p = safe(primary).toUpperCase(java.util.Locale.US);
        if (p.indexOf("HOT") >= 0 || p.indexOf("延续") >= 0) return "FALSE_CONTINUE|" + safe(primary);
        if (p.indexOf("COLD_RELEASE") >= 0 || p.indexOf("释放") >= 0)
            return "FALSE_RELEASE|" + safe(primary);
        if (p.indexOf("REVERSAL") >= 0 || p.indexOf("反转") >= 0)
            return "FALSE_REVERSAL|" + safe(primary);
        if (safe(counter).indexOf("切换") >= 0) return "REGIME_SWITCH|" + safe(counter);
        return "NUMBER_DISTRIBUTION_MISS|actualRank=" + rank + "|主因=" + safe(primary);
    }

    public static void persist(
            SQLiteDatabase db,
            String issue,
            int position,
            String result,
            String primary,
            String secondary,
            String counter,
            String attribution) {
        if (db == null) return;
        ContentValues v = new ContentValues();
        v.put("target_issue", safe(issue));
        v.put("position", position);
        v.put("result", safe(result));
        v.put("primary_cause", safe(primary));
        v.put("secondary_cause", safe(secondary));
        v.put("counter_evidence", safe(counter));
        v.put("ablation_blob", safe(attribution));
        v.put("responsible_module", owner(attribution));
        v.put("created_at", System.currentTimeMillis());
        v.put("version", MODULE_VERSION);
        db.insert(V49Repository.TABLE_ATTRIBUTION, null, v);
    }

    /** Leave-one-expert-out diagnostic over the frozen expert matrices. */
    public static String buildAblation(
            String expertBlob, int position, int actual, double[][] full) {
        if (position < 1 || position > 10 || actual < 1 || actual > 10) return "ablation=INVALID";
        String[] parts = safe(expertBlob).split("~", -1);
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        java.util.ArrayList<double[]> rows = new java.util.ArrayList<double[]>();
        for (int i = 0; i < parts.length; i++) {
            String[] fields = parts[i].split("#", -1);
            if (fields.length < 2 || fields[0].length() == 0) continue;
            String name = fields[0];
            double[][] m = parseMatrix(fields[1]);
            names.add(name);
            rows.add(m[position - 1]);
        }
        if (rows.size() < 2) return "ablation=NO_EXPERT_MATRIX";
        double fullP = full == null ? 0.0d : full[position - 1][actual - 1];
        String best = "", worst = "";
        double bestDelta = -99.0d, worstDelta = 99.0d;
        for (int drop = 0; drop < rows.size(); drop++) {
            double[] log = new double[10];
            for (int e = 0; e < rows.size(); e++)
                if (e != drop) {
                    double[] row = rows.get(e);
                    for (int n = 0; n < 10; n++) log[n] += FeatureContractRegistry.safeLog(row[n]);
                }
            double[] pooled = new double[10];
            V49ExpertSupport.softmax(log, pooled);
            double delta = fullP - pooled[actual - 1];
            if (delta > bestDelta) {
                bestDelta = delta;
                best = names.get(drop);
            }
            if (delta < worstDelta) {
                worstDelta = delta;
                worst = names.get(drop);
            }
        }
        return "ablation_best="
                + best
                + ":"
                + round(bestDelta)
                + "|ablation_mislead="
                + worst
                + ":"
                + round(worstDelta);
    }

    private static double[][] parseMatrix(String text) {
        double[][] out = new double[10][10];
        String[] rs = safe(text).split(";", -1);
        for (int p = 0; p < 10; p++) {
            String[] vs = p < rs.length ? rs[p].split(",", -1) : new String[0];
            for (int n = 0; n < 10; n++) {
                try {
                    out[p][n] = n < vs.length ? Double.parseDouble(vs[n]) : 0.1d;
                } catch (Throwable ignore) {
                    out[p][n] = 0.1d;
                }
            }
            FeatureContractRegistry.normalize10(out[p]);
        }
        return out;
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 100000.0d) / 100000.0d);
    }

    private static int actualRank(double[] p, int actual) {
        if (p == null || p.length != 10 || actual < 1 || actual > 10) return 99;
        double v = p[actual - 1];
        int rank = 1;
        for (int i = 0; i < 10; i++) if (p[i] > v + 1.0e-12d) rank++;
        return rank;
    }

    private static String owner(String attribution) {
        String a = safe(attribution);
        if (a.startsWith("RANK_INVERSION")) return "V49RealtimePipelineEngine";
        if (a.startsWith("POSITION_SELECTION")) return "V49OpportunityRankOwner";
        if (a.startsWith("REGIME_SWITCH")) return "V49WindowStateEngine";
        if (a.startsWith("FALSE_CONTINUE")
                || a.startsWith("FALSE_RELEASE")
                || a.startsWith("FALSE_REVERSAL")) return "MixtureOfExpertsGateEngine";
        if (a.startsWith("THIRD_HIT")) return "V49Top3Constructor";
        return "V49NumberProbabilityOwner";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
