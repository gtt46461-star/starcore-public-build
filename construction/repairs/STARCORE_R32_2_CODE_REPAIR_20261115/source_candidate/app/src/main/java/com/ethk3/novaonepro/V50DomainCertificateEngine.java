package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Evidence-only dynamic-domain certificate owner. */
public final class V50DomainCertificateEngine {
    public V50Models.DomainCertificate certify(
            DbStore store, V50Models.StateTrajectorySnapshot state) {
        String domain = state == null ? "" : state.dynamicDomain;
        String prototype =
                state == null || state.prototypes.length == 0
                        ? ""
                        : state.prototypes[0].prototypeId;
        V50Enums.Lifecycle lifecycle = state == null ? V50Enums.Lifecycle.UNKNOWN : state.lifecycle;
        Row row = read(store == null ? null : store.readable(), domain);
        int blocks = 0;
        double minBlockRate = 1.0d;
        double maxBlockRate = 0.0d;
        for (int i = 0; i < 4; i++) {
            if (row.blockSample[i] < 12) continue;
            blocks++;
            double rate = ((double) row.blockHits[i]) / row.blockSample[i];
            minBlockRate = Math.min(minBlockRate, rate);
            maxBlockRate = Math.max(maxBlockRate, rate);
        }
        double rate = row.sample == 0 ? 0.0d : ((double) row.hits) / row.sample;
        double low = wilsonLow(row.hits, row.sample);
        double stability =
                blocks < 2 ? 0.0d : Math.max(0.0d, 1.0d - (maxBlockRate - minBlockRate) / 0.16d);
        V50Enums.DomainStatus status = resolve(row, blocks, minBlockRate, low, stability);
        String checksum =
                V50ChecksumEngine.sha256(
                        domain
                                + '|'
                                + prototype
                                + '|'
                                + lifecycle
                                + '|'
                                + row.sample
                                + '|'
                                + row.hits
                                + '|'
                                + blocks
                                + '|'
                                + low
                                + '|'
                                + stability
                                + '|'
                                + row.calibrationError
                                + '|'
                                + status);
        return new V50Models.DomainCertificate(
                domain,
                prototype,
                lifecycle,
                row.sample,
                row.recentSample,
                blocks,
                rate,
                low,
                stability,
                row.calibrationError,
                row.maxLoseStreak,
                row.causeValidity,
                row.counterFailure,
                row.transitionValidity,
                status,
                checksum);
    }

    private static V50Enums.DomainStatus resolve(
            Row row, int blocks, double minBlockRate, double low, double stability) {
        if (row.status == V50Enums.DomainStatus.RETIRED) return row.status;
        if (row.currentLoseStreak >= 8 || (row.sample >= 30 && row.counterFailure >= 0.45d)) {
            return V50Enums.DomainStatus.HARD_FREEZE;
        }
        if (row.currentLoseStreak >= 5 || (row.sample >= 30 && row.calibrationError > 0.09d)) {
            return V50Enums.DomainStatus.SOFT_RISK;
        }
        if (row.status == V50Enums.DomainStatus.HARD_FREEZE
                || row.status == V50Enums.DomainStatus.SOFT_RISK) {
            return row.recentSample >= 30 && row.currentLoseStreak == 0
                    ? V50Enums.DomainStatus.RECOVERING
                    : row.status;
        }
        if (row.sample >= 160
                && blocks == 4
                && minBlockRate >= 0.37d
                && low >= 0.37d
                && stability >= 0.65d
                && row.calibrationError <= 0.04d) {
            return V50Enums.DomainStatus.PROVEN;
        }
        if (row.sample >= 80
                && blocks >= 3
                && low >= 0.34d
                && stability >= 0.50d
                && row.calibrationError <= 0.06d) {
            return V50Enums.DomainStatus.READY;
        }
        if (row.sample >= 30) return V50Enums.DomainStatus.WATCH;
        return V50Enums.DomainStatus.DISCOVERED;
    }

    private static Row read(SQLiteDatabase db, String domain) {
        Row row = new Row();
        if (db == null || safe(domain).length() == 0) return row;
        Cursor c = null;
        try {
            c =
                    db.query(
                            SchemaContractRegistry.TABLE_DOMAIN,
                            new String[] {
                                "sample",
                                "recent_sample",
                                "hit_count",
                                "recent_hit_count",
                                "block0_sample",
                                "block0_hits",
                                "block1_sample",
                                "block1_hits",
                                "block2_sample",
                                "block2_hits",
                                "block3_sample",
                                "block3_hits",
                                "calibration_error",
                                "max_lose_streak",
                                "current_lose_streak",
                                "cause_validity",
                                "counter_failure",
                                "transition_validity",
                                "status"
                            },
                            "domain_id=?",
                            new String[] {domain},
                            null,
                            null,
                            null,
                            "1");
            if (!c.moveToFirst()) return row;
            row.sample = c.getInt(0);
            row.recentSample = c.getInt(1);
            row.hits = c.getInt(2);
            row.recentHits = c.getInt(3);
            row.blockSample = new int[] {c.getInt(4), c.getInt(6), c.getInt(8), c.getInt(10)};
            row.blockHits = new int[] {c.getInt(5), c.getInt(7), c.getInt(9), c.getInt(11)};
            row.calibrationError = c.getDouble(12);
            row.maxLoseStreak = c.getInt(13);
            row.currentLoseStreak = c.getInt(14);
            row.causeValidity = c.getDouble(15);
            row.counterFailure = c.getDouble(16);
            row.transitionValidity = c.getDouble(17);
            row.status = parse(c.getString(18));
        } catch (Throwable ignore) {
            return new Row();
        } finally {
            if (c != null) c.close();
        }
        return row;
    }

    private static double wilsonLow(int hits, int sample) {
        if (sample <= 0) return 0.0d;
        double z = 1.96d;
        double p = ((double) hits) / sample;
        double denominator = 1.0d + z * z / sample;
        double center = p + z * z / (2.0d * sample);
        double margin = z * Math.sqrt((p * (1.0d - p) + z * z / (4.0d * sample)) / sample);
        return Math.max(0.0d, (center - margin) / denominator);
    }

    private static V50Enums.DomainStatus parse(String value) {
        try {
            return V50Enums.DomainStatus.valueOf(safe(value));
        } catch (Throwable ignore) {
            return V50Enums.DomainStatus.DISCOVERED;
        }
    }

    private static final class Row {
        int sample;
        int recentSample;
        int hits;
        int recentHits;
        int[] blockSample = new int[4];
        int[] blockHits = new int[4];
        double calibrationError = 1.0d;
        int maxLoseStreak;
        int currentLoseStreak;
        double causeValidity;
        double counterFailure;
        double transitionValidity;
        V50Enums.DomainStatus status = V50Enums.DomainStatus.DISCOVERED;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
