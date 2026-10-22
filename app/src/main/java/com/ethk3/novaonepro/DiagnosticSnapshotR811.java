package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Immutable short-read diagnostic snapshot; all long report rendering happens outside SQLite. */
public final class DiagnosticSnapshotR811 {
    public final long capturedAt;
    public final String latestDraw,
            latestCanonical,
            latestSettled,
            latestOos,
            targetIssue,
            schemaRevision,
            coreStatus,
            fullStatus;
    public final int settlementPending,
            deferredPending,
            shadowPredicted,
            shadowSettled,
            shadowHit,
            shadowMiss;
    public final long dbLockWaitMs;
    public final boolean realtimePressure;
    public final WindowStats recent30, recent60, recent120;

    private DiagnosticSnapshotR811(
            long at,
            String draw,
            String canonical,
            String settled,
            String oos,
            String target,
            String revision,
            String core,
            String full,
            int settlementPending,
            int deferredPending,
            int predicted,
            int shadowSettled,
            int hit,
            int miss,
            long wait,
            boolean pressure,
            WindowStats recent30,
            WindowStats recent60,
            WindowStats recent120) {
        capturedAt = at;
        latestDraw = safe(draw);
        latestCanonical = safe(canonical);
        latestSettled = safe(settled);
        latestOos = safe(oos);
        targetIssue = safe(target);
        schemaRevision = safe(revision);
        coreStatus = safe(core);
        fullStatus = safe(full);
        this.settlementPending = settlementPending;
        this.deferredPending = deferredPending;
        shadowPredicted = predicted;
        this.shadowSettled = shadowSettled;
        shadowHit = hit;
        shadowMiss = miss;
        dbLockWaitMs = wait;
        realtimePressure = pressure;
        this.recent30 = recent30;
        this.recent60 = recent60;
        this.recent120 = recent120;
    }

    public static DiagnosticSnapshotR811 capture(DbStore store) {
        if (store == null) return readError();
        SQLiteDatabase db = store.readable();
        String draw =
                scalar(
                        db,
                        "SELECT issue FROM "
                                + DbHelper.TABLE_DRAWS
                                + " WHERE dirty=0 ORDER BY issue DESC LIMIT 1");
        String canonical =
                scalar(
                        db,
                        "SELECT target_issue FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE pre_draw_frozen=1 ORDER BY target_issue DESC LIMIT 1");
        String settled =
                scalar(
                        db,
                        "SELECT target_issue FROM "
                                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                + " WHERE status IN ('COMPLETED','FAILED_TERMINAL',"
                                + "'NO_PREDRAW_CANONICAL_TERMINAL','INVALID_DRAW_TERMINAL')"
                                + " ORDER BY target_issue DESC LIMIT 1");
        String oos =
                scalar(
                        db,
                        "SELECT target_issue FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 1");
        String target =
                TargetIssueResolver.isValid(draw)
                        ? TargetIssueResolver.next(draw).targetIssue
                        : "";
        PipelineInvariantGuard.CanonicalEvidenceStatus evidence =
                TargetIssueResolver.isValid(canonical)
                        ? PipelineInvariantGuard.auditCanonicalEvidence(store, canonical)
                        : null;
        DatabaseCoordinator coordinator = store.coordinatorR36();
        int predicted =
                integer(db, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION);
        int settledCount =
                integer(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE settled=1");
        int hit =
                integer(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE settled=1 AND hit_label=1");
        int miss =
                integer(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE settled=1 AND hit_label=0");
        return new DiagnosticSnapshotR811(
                TimeSourceR811.wallNowMs(),
                draw,
                canonical,
                settled,
                oos,
                target,
                store.getState("r811_schema_revision", ""),
                evidence == null ? "FAIL" : evidence.core.name(),
                evidence == null ? "PENDING" : evidence.full.name(),
                integer(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')"),
                DeferredEvidenceWriterR36.pendingCount(store),
                predicted,
                settledCount,
                hit,
                miss,
                coordinator == null ? 0L : coordinator.lastDbLockWaitMs(),
                RealtimePressureGateR811.isUnderPressure(),
                window(db, 30),
                window(db, 60),
                window(db, 120));
    }

    private static WindowStats window(SQLiteDatabase db, int limit) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT hit_label,selected_hit_p FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE settled=1 AND hit_label IN (0,1)"
                                    + " ORDER BY target_issue DESC LIMIT ?",
                            new String[] {String.valueOf(limit)});
            int sample = 0, hit = 0, probabilitySample = 0;
            double brier = 0.0d;
            while (c.moveToNext()) {
                int y = c.getInt(0);
                double p = c.getDouble(1);
                sample++;
                if (y == 1) hit++;
                if (!c.isNull(1) && Double.isFinite(p) && p >= 0.0d && p <= 1.0d) {
                    double error = p - y;
                    brier += error * error;
                    probabilitySample++;
                }
            }
            double rate = sample == 0 ? Double.NaN : ((double) hit) / sample;
            double brierMean =
                    probabilitySample == 0 ? Double.NaN : brier / probabilitySample;
            return new WindowStats(limit, sample, hit, rate, wilsonLower(hit, sample), brierMean);
        } finally {
            if (c != null) c.close();
        }
    }

    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return Double.NaN;
        double z = 1.959963984540054d;
        double p = ((double) hit) / n;
        double z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return (center - margin) / (1.0d + z2 / n);
    }

    private static String scalar(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            return c.moveToFirst() && c.getString(0) != null ? c.getString(0) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static int integer(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static DiagnosticSnapshotR811 readError() {
        WindowStats empty =
                new WindowStats(0, 0, 0, Double.NaN, Double.NaN, Double.NaN);
        return new DiagnosticSnapshotR811(
                TimeSourceR811.wallNowMs(),
                "",
                "",
                "",
                "",
                "",
                "READ_ERROR",
                "FAIL",
                "FAIL",
                0,
                0,
                0,
                0,
                0,
                0,
                0L,
                false,
                empty,
                empty,
                empty);
    }

    public static final class WindowStats {
        public final int requested, sample, hit;
        public final double hitRate, wilsonLower, brier;

        WindowStats(
                int requested,
                int sample,
                int hit,
                double hitRate,
                double wilsonLower,
                double brier) {
            this.requested = requested;
            this.sample = sample;
            this.hit = hit;
            this.hitRate = hitRate;
            this.wilsonLower = wilsonLower;
            this.brier = brier;
        }

        public boolean available() {
            return sample > 0;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

public static DiagnosticSnapshotR811 captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return readErrorSnapshotCP4(report);
        SQLiteDatabase db = store.readable();
        String draw =
                scalarSnapshotCP4(report, 
                        db,
                        "SELECT issue FROM "
                                + DbHelper.TABLE_DRAWS
                                + " WHERE dirty=0 ORDER BY issue DESC LIMIT 1");
        String canonical =
                scalarSnapshotCP4(report, 
                        db,
                        "SELECT target_issue FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE pre_draw_frozen=1 ORDER BY target_issue DESC LIMIT 1");
        String settled =
                scalarSnapshotCP4(report, 
                        db,
                        "SELECT target_issue FROM "
                                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                + " WHERE status IN ('COMPLETED','FAILED_TERMINAL',"
                                + "'NO_PREDRAW_CANONICAL_TERMINAL','INVALID_DRAW_TERMINAL')"
                                + " ORDER BY target_issue DESC LIMIT 1");
        String oos =
                scalarSnapshotCP4(report, 
                        db,
                        "SELECT target_issue FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 1");
        String target =
                TargetIssueResolver.isValid(draw)
                        ? TargetIssueResolver.next(draw).targetIssue
                        : "";
        PipelineInvariantGuard.CanonicalEvidenceStatus evidence =
                TargetIssueResolver.isValid(canonical)
                        ? PipelineInvariantGuard.auditCanonicalEvidenceSnapshotCP4(report, store, canonical)
                        : null;
        
        int predicted =
                integerSnapshotCP4(report, db, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION);
        int settledCount =
                integerSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE settled=1");
        int hit =
                integerSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE settled=1 AND hit_label=1");
        int miss =
                integerSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE settled=1 AND hit_label=0");
        return new DiagnosticSnapshotR811(
                report.nowMs(),
                draw,
                canonical,
                settled,
                oos,
                target,
                store.getState("r811_schema_revision", ""),
                evidence == null ? "FAIL" : evidence.core.name(),
                evidence == null ? "PENDING" : evidence.full.name(),
                integerSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')"),
                DeferredEvidenceWriterR36.pendingCountSnapshotCP4(report, store),
                predicted,
                settledCount,
                hit,
                miss,
                report.observationLong("dbWaitMs"),
                report.observationBoolean("realtimePressure"),
                windowSnapshotCP4(report, db, 30),
                windowSnapshotCP4(report, db, 60),
                windowSnapshotCP4(report, db, 120));
    }

public static WindowStats windowSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, int limit) {
        Cursor c = null;
        try {
            c =
                    report.select(
                            "SELECT hit_label,selected_hit_p FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE settled=1 AND hit_label IN (0,1)"
                                    + " ORDER BY target_issue DESC LIMIT ?",
                            new String[] {String.valueOf(limit)});
            int sample = 0, hit = 0, probabilitySample = 0;
            double brier = 0.0d;
            while (c.moveToNext()) {
                int y = c.getInt(0);
                double p = c.getDouble(1);
                sample++;
                if (y == 1) hit++;
                if (!c.isNull(1) && Double.isFinite(p) && p >= 0.0d && p <= 1.0d) {
                    double error = p - y;
                    brier += error * error;
                    probabilitySample++;
                }
            }
            double rate = sample == 0 ? Double.NaN : ((double) hit) / sample;
            double brierMean =
                    probabilitySample == 0 ? Double.NaN : brier / probabilitySample;
            return new WindowStats(limit, sample, hit, rate, wilsonLowerSnapshotCP4(report, hit, sample), brierMean);
        } finally {
            if (c != null) c.close();
        }
    }

public static double wilsonLowerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int hit, int n) {
        if (n <= 0) return Double.NaN;
        double z = 1.959963984540054d;
        double p = ((double) hit) / n;
        double z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return (center - margin) / (1.0d + z2 / n);
    }

public static int integerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = report.select(sql, null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

public static String scalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = report.select(sql, null);
            return c.moveToFirst() && c.getString(0) != null ? c.getString(0) : "";
        } finally {
            if (c != null) c.close();
        }
    }

public static DiagnosticSnapshotR811 readErrorSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report) {
        WindowStats empty =
                new WindowStats(0, 0, 0, Double.NaN, Double.NaN, Double.NaN);
        return new DiagnosticSnapshotR811(
                report.nowMs(),
                "",
                "",
                "",
                "",
                "",
                "READ_ERROR",
                "FAIL",
                "FAIL",
                0,
                0,
                0,
                0,
                0,
                0,
                0L,
                false,
                empty,
                empty,
                empty);
    }
}
