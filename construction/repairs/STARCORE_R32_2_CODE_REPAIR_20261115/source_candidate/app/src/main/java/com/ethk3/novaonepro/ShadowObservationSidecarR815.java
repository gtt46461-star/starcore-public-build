package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Idempotent post-production shadow observation; it never calls a decision or settlement owner. */
public final class ShadowObservationSidecarR815 {
    public static final class SettledEvent {
        public final String targetIssue;
        public final String settlementHash;
        public final String canonicalHash;
        public final int actualNumber;
        public final int hitLabel;
        public final long settledAt;

        public SettledEvent(
                String targetIssue,
                String settlementHash,
                String canonicalHash,
                int actualNumber,
                int hitLabel,
                long settledAt) {
            this.targetIssue = safe(targetIssue);
            this.settlementHash = safe(settlementHash);
            this.canonicalHash = safe(canonicalHash);
            this.actualNumber = actualNumber;
            this.hitLabel = hitLabel;
            this.settledAt = settledAt > 0L ? settledAt : System.currentTimeMillis();
            if (!TargetIssueResolver.isValid(this.targetIssue)
                    || !hash(this.settlementHash)
                    || !hash(this.canonicalHash)
                    || !AppConfig.isValidNumber(actualNumber)
                    || (hitLabel != 0 && hitLabel != 1))
                throw new IllegalArgumentException("R815_SHADOW_EVENT_INVALID");
        }

        String recordHash() {
            return V51ChecksumEngine.sha256(
                    "R815_SHADOW|"
                            + targetIssue
                            + '|'
                            + settlementHash
                            + '|'
                            + canonicalHash
                            + '|'
                            + actualNumber
                            + '|'
                            + hitLabel);
        }
    }

    public static final class Result {
        public final boolean success;
        public final boolean replay;
        public final String status;
        public final String recordHash;

        Result(boolean success, boolean replay, String status, String recordHash) {
            this.success = success;
            this.replay = replay;
            this.status = safe(status);
            this.recordHash = safe(recordHash);
        }
    }

    private ShadowObservationSidecarR815() {}

    public static Result onProductionSettled(final DbStore store, final SettledEvent event) {
        if (store == null || event == null) return new Result(false, false, "ARGUMENT_NULL", "");
        final String record = event.recordHash();
        try {
            Result result =
                    store.coordinatorR36()
                            .runWrite(
                                    "R815_SHADOW_SIDECAR",
                                    new DatabaseCoordinator.WriteCallable<Result>() {
                                        @Override
                                        public Result call(SQLiteDatabase db) {
                                            ContentValues values = new ContentValues();
                                            values.put("target_issue", event.targetIssue);
                                            values.put("settlement_hash", event.settlementHash);
                                            values.put("canonical_hash", event.canonicalHash);
                                            values.put("actual_number", event.actualNumber);
                                            values.put("hit_label", event.hitLabel);
                                            values.put("created_at", event.settledAt);
                                            values.put("retry_count", 0);
                                            values.put("last_error", "");
                                            long inserted =
                                                    db.insertWithOnConflict(
                                                            DbHelper.TABLE_R815_SHADOW_SIDECAR,
                                                            null,
                                                            values,
                                                            SQLiteDatabase.CONFLICT_IGNORE);
                                            if (inserted >= 0L)
                                                return new Result(true, false, "APPLIED", record);
                                            Cursor cursor = null;
                                            try {
                                                cursor =
                                                        db.rawQuery(
                                                                "SELECT settlement_hash,canonical_hash,actual_number,hit_label FROM "
                                                                        + DbHelper.TABLE_R815_SHADOW_SIDECAR
                                                                        + " WHERE target_issue=? LIMIT 1",
                                                                new String[] {event.targetIssue});
                                                if (!cursor.moveToFirst())
                                                    return new Result(false, false, "ROW_MISSING", record);
                                                boolean same =
                                                        event.settlementHash.equals(safe(cursor.getString(0)))
                                                                && event.canonicalHash.equals(safe(cursor.getString(1)))
                                                                && event.actualNumber == cursor.getInt(2)
                                                                && event.hitLabel == cursor.getInt(3);
                                                return new Result(
                                                        same,
                                                        same,
                                                        same ? "ALREADY_APPLIED" : "HARD_CONFLICT",
                                                        record);
                                            } finally {
                                                if (cursor != null) cursor.close();
                                            }
                                        }
                                    },
                                    new Result(false, false, "WRITE_FAILED", record));
            if (result.success && !result.replay) {
                if (!ResearchSchedulerR815.enqueueFromProduction(store, event)) {
                    CriticalPathErrorLedgerR815.record(
                            store,
                            new CriticalPathErrorLedgerR815.Event(
                                    "RESEARCH",
                                    "QUEUE_ENQUEUE_FAILED",
                                    event.targetIssue,
                                    0L,
                                    true,
                                    "",
                                    record,
                                    System.currentTimeMillis()));
                }
            }
            if (!result.success)
                CriticalPathErrorLedgerR815.record(
                        store,
                        new CriticalPathErrorLedgerR815.Event(
                                "SHADOW",
                                result.status,
                                event.targetIssue,
                                0L,
                                true,
                                "",
                                record,
                                System.currentTimeMillis()));
            return result;
        } catch (RuntimeException failure) {
            CriticalPathErrorLedgerR815.record(
                    store,
                    new CriticalPathErrorLedgerR815.Event(
                            "SHADOW", "SIDECAR_WRITE_FAILED", event.targetIssue, 0L, true, failure, record));
            return new Result(false, false, "WRITE_EXCEPTION", record);
        }
    }

    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
