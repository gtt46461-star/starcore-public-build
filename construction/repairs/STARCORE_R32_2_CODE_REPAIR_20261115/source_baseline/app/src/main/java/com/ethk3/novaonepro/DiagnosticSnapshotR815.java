package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded read-only diagnosis snapshot; formatting happens only after cursors are closed. */
public final class DiagnosticSnapshotR815 {
    public static final class Row {
        public final String id;
        public final String hash;
        public final String status;

        Row(String id, String hash, String status) {
            this.id = safe(id);
            this.hash = safe(hash);
            this.status = safe(status);
        }
    }

    public static final class Snapshot {
        public final boolean retryable;
        public final String status;
        public final String targetIssue;
        public final long capturedAt;
        public final String schemaRevision;
        public final String canonicalHash;
        public final List<Row> recentErrors;
        public final String checksum;

        Snapshot(
                boolean retryable,
                String status,
                String targetIssue,
                long capturedAt,
                String schemaRevision,
                String canonicalHash,
                List<Row> rows) {
            this.retryable = retryable;
            this.status = safe(status);
            this.targetIssue = safe(targetIssue);
            this.capturedAt = capturedAt;
            this.schemaRevision = safe(schemaRevision);
            this.canonicalHash = safe(canonicalHash);
            this.recentErrors = Collections.unmodifiableList(new ArrayList<Row>(rows));
            StringBuilder canonical = new StringBuilder(this.status);
            canonical.append('|').append(this.targetIssue).append('|').append(capturedAt)
                    .append('|').append(this.schemaRevision).append('|').append(this.canonicalHash);
            for (int i = 0; i < rows.size(); i++) canonical.append('|').append(rows.get(i).id).append(':').append(rows.get(i).hash);
            checksum = V51ChecksumEngine.sha256(canonical.toString());
        }

        public String formatSummary() {
            StringBuilder out = new StringBuilder();
            out.append("R815诊断快照\n状态: ").append(status)
                    .append("\n目标期: ").append(targetIssue)
                    .append("\n采集时间: ").append(capturedAt)
                    .append("\nSchema: ").append(schemaRevision)
                    .append("\nCanonical: ").append(canonicalHash)
                    .append("\n错误行: ").append(recentErrors.size())
                    .append("\nHash: ").append(checksum);
            for (int i = 0; i < recentErrors.size(); i++) {
                Row row = recentErrors.get(i);
                out.append("\n- ").append(row.status).append(" #").append(row.id).append(" ").append(row.hash);
            }
            return out.toString();
        }
    }

    private DiagnosticSnapshotR815() {}

    public static Snapshot capture(DbStore store, String targetIssue, int maxRows) {
        int capped = Math.max(1, Math.min(64, maxRows));
        String target = safe(targetIssue);
        if (store == null || (target.length() > 0 && !TargetIssueResolver.isValid(target)))
            return new Snapshot(true, "SNAPSHOT_RETRYABLE", target, System.currentTimeMillis(), "", "", new ArrayList<Row>());
        SQLiteDatabase db = store.readable();
        Cursor state = null;
        Cursor canonical = null;
        Cursor errors = null;
        ArrayList<Row> rows = new ArrayList<Row>();
        long captured = System.currentTimeMillis();
        try {
            state =
                    db.rawQuery(
                            "SELECT state_value FROM "
                                    + DbHelper.TABLE_APP_STATE
                                    + " WHERE state_key='r815_schema_revision' LIMIT 1",
                            null);
            String revision = state.moveToFirst() ? safe(state.getString(0)) : "";
            String hash = "";
            if (target.length() > 0) {
                canonical =
                        db.rawQuery(
                                "SELECT record_hash FROM "
                                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                        + " WHERE target_issue=? LIMIT 1",
                                new String[] {target});
                if (canonical.moveToFirst()) hash = safe(canonical.getString(0));
            }
            errors =
                    db.rawQuery(
                            "SELECT id,event_key,error_code FROM "
                                    + DbHelper.TABLE_R815_CRITICAL_ERROR
                                    + " ORDER BY created_at DESC,id DESC LIMIT "
                                    + capped,
                            null);
            while (errors.moveToNext())
                rows.add(new Row(String.valueOf(errors.getLong(0)), errors.getString(1), errors.getString(2)));
            return new Snapshot(false, "OK", target, captured, revision, hash, rows);
        } catch (RuntimeException failure) {
            return new Snapshot(true, "SNAPSHOT_RETRYABLE", target, captured, "", "", rows);
        } finally {
            if (state != null) state.close();
            if (canonical != null) canonical.close();
            if (errors != null) errors.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
