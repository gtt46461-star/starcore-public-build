package com.ethk3.novaonepro;

import android.database.Cursor;

/** Read-only classification of frozen SHADOW rows that have not reached strict settlement. */
public final class SettlementBacklogAuditR3611 {
    public enum State {
        READY_TO_SETTLE,
        WAIT_DRAW,
        LEASE_ACTIVE,
        PIT_INVALID,
        TERMINAL,
        DATA_CORRUPT
    }

    private SettlementBacklogAuditR3611() {}

    public static Summary scan(DbStore store, int limit) {
        Summary out = new Summary();
        if (store == null) {
            out.readError = "STORE_NULL";
            return out;
        }
        int bounded = Math.max(1, Math.min(500, limit));
        long now = System.currentTimeMillis();
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT o.target_issue,o.base_issue,o.commit_at,o.created_at,"
                                            + "o.pre_draw_frozen,o.settled,d.issue,d.open_time,d.first_seen_at,"
                                            + "t.status,t.lease_until,t.next_attempt_at,t.last_error "
                                            + "FROM "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o LEFT JOIN "
                                            + DrawDependencyInputCP12.SOURCE
                                            + " d ON d.issue=o.target_issue LEFT JOIN "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t ON t.target_issue=o.target_issue "
                                            + "WHERE o.pre_draw_frozen=1 AND o.settled=0 "
                                            + "ORDER BY o.target_issue ASC LIMIT ?",
                                    new String[] {String.valueOf(bounded)});
            while (c.moveToNext()) {
                Row row = new Row();
                row.targetIssue = safe(c.getString(0));
                row.baseIssue = safe(c.getString(1));
                row.commitAt = c.getLong(2);
                row.createdAt = c.getLong(3);
                row.preDrawFrozen = c.getInt(4);
                row.settled = c.getInt(5);
                row.drawIssue = safe(c.getString(6));
                row.drawOpenTime = c.isNull(7) ? 0L : c.getLong(7);
                row.drawFirstSeenAt = c.isNull(8) ? 0L : c.getLong(8);
                row.taskStatus = safe(c.getString(9));
                row.leaseUntil = c.isNull(10) ? 0L : c.getLong(10);
                row.nextAttemptAt = c.isNull(11) ? 0L : c.getLong(11);
                row.lastError = safe(c.getString(12));
                row.state = classify(row, now);
                out.add(row);
            }
        } catch (Throwable error) {
            out.readError = error.getClass().getSimpleName();
        } finally {
            if (c != null) c.close();
        }
        out.scannedAt = now;
        return out;
    }

    private static State classify(Row row, long now) {
        if (row == null
                || !TargetIssueResolver.isValid(row.targetIssue)
                || row.preDrawFrozen != 1
                || row.commitAt <= 0L) return State.DATA_CORRUPT;
        if (isTerminal(row.taskStatus)) return State.TERMINAL;
        if (row.drawIssue.length() == 0) return State.WAIT_DRAW;
        if (!row.targetIssue.equals(row.drawIssue)) return State.DATA_CORRUPT;
        if (row.drawFirstSeenAt <= 0L || row.drawFirstSeenAt <= row.commitAt)
            return State.PIT_INVALID;
        if (SettlementTaskStoreR36.RUNNING.equals(row.taskStatus) && row.leaseUntil > now)
            return State.LEASE_ACTIVE;
        return State.READY_TO_SETTLE;
    }

    private static boolean isTerminal(String status) {
        return SettlementTaskStoreR36.COMPLETED.equals(status)
                || SettlementTaskStoreR36.FAILED_TERMINAL.equals(status)
                || SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL.equals(status)
                || SettlementTaskStoreR36.INVALID_DRAW_TERMINAL.equals(status);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Row {
        public String targetIssue = "", baseIssue = "", drawIssue = "", taskStatus = "", lastError = "";
        public long commitAt, createdAt, drawOpenTime, drawFirstSeenAt, leaseUntil, nextAttemptAt;
        public int preDrawFrozen, settled;
        public State state = State.DATA_CORRUPT;
    }

    public static final class Summary {
        public int total, ready, waitDraw, leaseActive, pitInvalid, terminal, dataCorrupt;
        public long scannedAt;
        public String readError = "";
        public String oldestTarget = "";
        public long oldestCreatedAt;

        void add(Row row) {
            total++;
            if (oldestCreatedAt == 0L || (row.createdAt > 0L && row.createdAt < oldestCreatedAt)) {
                oldestCreatedAt = row.createdAt;
                oldestTarget = row.targetIssue;
            }
            if (row.state == State.READY_TO_SETTLE) ready++;
            else if (row.state == State.WAIT_DRAW) waitDraw++;
            else if (row.state == State.LEASE_ACTIVE) leaseActive++;
            else if (row.state == State.PIT_INVALID) pitInvalid++;
            else if (row.state == State.TERMINAL) terminal++;
            else dataCorrupt++;
        }

        public String compact() {
            return "total="
                    + total
                    + "|ready="
                    + ready
                    + "|waitDraw="
                    + waitDraw
                    + "|leaseActive="
                    + leaseActive
                    + "|pitInvalid="
                    + pitInvalid
                    + "|terminal="
                    + terminal
                    + "|dataCorrupt="
                    + dataCorrupt
                    + "|oldest="
                    + oldestTarget
                    + "|readError="
                    + readError;
        }
    }
}
