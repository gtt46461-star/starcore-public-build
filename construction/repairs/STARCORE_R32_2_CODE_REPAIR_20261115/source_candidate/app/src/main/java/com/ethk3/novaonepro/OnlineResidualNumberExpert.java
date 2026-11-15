package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/**
 * Bounded online residual. Deltas are domain/lifecycle specific, clipped, and cannot replace or
 * directly mutate the final probability owner.
 */
public final class OnlineResidualNumberExpert implements V50NumberExpert {
    private static final double MAX_ABS_DELTA = 0.18d;

    public String id() {
        return "ONLINE_BOUNDED_RESIDUAL";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        SQLiteDatabase db = store == null ? null : store.readable();
        for (int p = 1; p <= 10; p++) {
            V50Models.StateTrajectorySnapshot state = V50ExpertSupport.state(states, p);
            Delta[] delta =
                    read(
                            db,
                            state == null ? "*" : state.dynamicDomain,
                            state == null ? V50Enums.Lifecycle.UNKNOWN : state.lifecycle,
                            p);
            int totalSample = 0;
            int maxMislead = 0;
            for (int n = 1; n <= 10; n++) {
                V50Models.NumberFeatureRow row = V50ExpertSupport.row(features, p, n);
                double base =
                        row == null || row.windowFrequency.length < 5
                                ? 0.1d
                                : row.windowFrequency[4];
                double adjustment =
                        Math.max(-MAX_ABS_DELTA, Math.min(MAX_ABS_DELTA, delta[n - 1].value));
                probability[p - 1][n - 1] = Math.max(1.0e-6d, base) * Math.exp(adjustment);
                totalSample += delta[n - 1].sample;
                maxMislead = Math.max(maxMislead, delta[n - 1].misleadRun);
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] =
                    maxMislead >= 4
                            ? 0.0d
                            : V50ExpertSupport.clamp01(Math.min(0.52d, totalSample / 600.0d));
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("DOMAIN_LIFECYCLE_BOUNDED_RESIDUAL"),
                V50ExpertSupport.text("DRIFT_OR_MISLEAD_RUN_FORCES_ZERO_WEIGHT"),
                "V50_ONLINE_RESIDUAL_V1");
    }

    private static Delta[] read(
            SQLiteDatabase db, String domain, V50Enums.Lifecycle lifecycle, int position) {
        Delta[] out = new Delta[10];
        for (int i = 0; i < out.length; i++) out[i] = new Delta();
        if (db == null) return out;
        Cursor c = null;
        try {
            c =
                    db.query(
                            SchemaContractRegistry.TABLE_ONLINE_RESIDUAL,
                            new String[] {"number", "sample", "delta", "mislead_run"},
                            "position=? AND (domain_id=? OR domain_id='*')"
                                    + " AND (lifecycle=? OR lifecycle='UNKNOWN')",
                            new String[] {String.valueOf(position), safe(domain), lifecycle.name()},
                            null,
                            null,
                            "sample DESC");
            while (c.moveToNext()) {
                int n = c.getInt(0);
                if (n < 1 || n > 10 || out[n - 1].sample > 0) continue;
                out[n - 1].sample = c.getInt(1);
                out[n - 1].value = c.getDouble(2);
                out[n - 1].misleadRun = c.getInt(3);
            }
        } catch (Throwable ignore) {
            return out;
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private static final class Delta {
        int sample;
        double value;
        int misleadRun;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
