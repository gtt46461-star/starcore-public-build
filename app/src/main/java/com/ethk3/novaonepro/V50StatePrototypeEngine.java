package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Produces multi-label observable state prototypes; it never emits probability. */
public final class V50StatePrototypeEngine {
    public static final String MODULE_VERSION = "V50_STATE_PROTOTYPE_50072401";

    public V50Models.StatePrototypeScore[] score(
            V50Models.NumberFeatureSnapshot snapshot, int position) {
        if (snapshot == null || position < 1 || position > 10) {
            return new V50Models.StatePrototypeScore[0];
        }
        V50Models.NumberFeatureRow[] rows = positionRows(snapshot, position);
        if (rows.length != 10) return new V50Models.StatePrototypeScore[0];
        double maxSlope = -1.0d;
        double minSlope = 1.0d;
        double maxAcceleration = -1.0d;
        double maxGap = 0.0d;
        double maxRelation = 0.0d;
        double entropy = rows[0].positionEntropy;
        double concentration = rows[0].concentration;
        double drift = rows[0].drift;
        double parityBias = 0.0d;
        for (int i = 0; i < rows.length; i++) {
            V50Models.NumberFeatureRow row = rows[i];
            maxSlope = Math.max(maxSlope, row.slope);
            minSlope = Math.min(minSlope, row.slope);
            maxAcceleration = Math.max(maxAcceleration, row.acceleration);
            maxGap = Math.max(maxGap, row.gapPercentile);
            maxRelation = Math.max(maxRelation, Math.abs(row.relationScore));
            parityBias += row.attributeScore * Math.abs(row.slope);
        }
        ArrayList<V50Models.StatePrototypeScore> out =
                new ArrayList<V50Models.StatePrototypeScore>();
        add(
                out,
                "HOT_ACCELERATION",
                clamp01(0.18d + maxSlope * 5.0d + Math.max(0.0d, maxAcceleration) * 3.0d));
        add(
                out,
                "COLD_RELEASE",
                clamp01(0.16d + maxGap * 0.55d + Math.max(0.0d, maxAcceleration) * 1.8d));
        add(out, "OSCILLATION", clamp01(0.30d + entropy * 0.48d - Math.abs(maxSlope) * 1.4d));
        add(
                out,
                "COMPRESSION_BREAKOUT",
                clamp01((1.0d - entropy) * 0.75d + concentration * 0.35d + drift * 0.85d));
        add(out, "REVERSAL", clamp01(0.18d + Math.abs(minSlope) * 3.2d + drift * 0.70d));
        add(out, "POSITION_MIGRATION", clamp01(maxRelation * 0.65d + drift * 0.35d));
        add(out, "ATTRIBUTE_ROTATION", clamp01(parityBias * 2.5d + entropy * 0.18d));
        add(
                out,
                "STABLE_BALANCED",
                clamp01(0.72d - drift * 1.4d - Math.abs(entropy - 0.92d) * 1.1d));
        Collections.sort(
                out,
                new Comparator<V50Models.StatePrototypeScore>() {
                    public int compare(
                            V50Models.StatePrototypeScore a, V50Models.StatePrototypeScore b) {
                        int value = Double.compare(b.score, a.score);
                        return value != 0 ? value : a.prototypeId.compareTo(b.prototypeId);
                    }
                });
        return out.toArray(new V50Models.StatePrototypeScore[out.size()]);
    }

    private static V50Models.NumberFeatureRow[] positionRows(
            V50Models.NumberFeatureSnapshot snapshot, int position) {
        ArrayList<V50Models.NumberFeatureRow> out = new ArrayList<V50Models.NumberFeatureRow>();
        for (int i = 0; i < snapshot.rows.length; i++) {
            V50Models.NumberFeatureRow row = snapshot.rows[i];
            if (row != null && row.position == position) out.add(row);
        }
        Collections.sort(
                out,
                new Comparator<V50Models.NumberFeatureRow>() {
                    public int compare(V50Models.NumberFeatureRow a, V50Models.NumberFeatureRow b) {
                        return a.number - b.number;
                    }
                });
        return out.toArray(new V50Models.NumberFeatureRow[out.size()]);
    }

    private static void add(ArrayList<V50Models.StatePrototypeScore> out, String id, double value) {
        out.add(new V50Models.StatePrototypeScore(id, value));
    }

    private static double clamp01(double value) {
        return FeatureContractRegistry.clamp01(value);
    }
}
