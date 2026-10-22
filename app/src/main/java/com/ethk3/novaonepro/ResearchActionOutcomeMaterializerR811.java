package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model-free exact-1200 grouped labels derived only from a completed durable predraw snapshot.
 *
 * <p>The deferred writer has already authenticated and validated the snapshot before it marks the
 * job COMPLETED. This sidecar therefore streams immutable materialized rows in exact ordinal order
 * instead of decoding a second 1200-object graph on a 256 MiB heap. It never invokes a model and
 * never mutates a Production decision.
 */
public final class ResearchActionOutcomeMaterializerR811 {
    private ResearchActionOutcomeMaterializerR811() {}

    public static Materialization materialize(
            final DbStore store, final String targetIssue, final Models.Draw draw) {
        if (store == null
                || draw == null
                || !draw.isValid()
                || !safe(targetIssue).equals(draw.issue))
            return Materialization.fail(targetIssue, "INVALID_INPUT");
        if (RealtimePressureGateR811.hasActiveCriticalWork())
            return Materialization.fail(targetIssue, "YIELD_REALTIME");
        if (!store.coordinatorR36().ensureR811ResearchSchemaOnce())
            return Materialization.fail(targetIssue, "SCHEMA_NOT_READY");

        GroupVerification replay = verifyGroup(store, targetIssue);
        if (replay.passed)
            return new Materialization(
                    true, targetIssue, replay.rows, replay.groupHash, "REPLAY_IDEMPOTENT");

        Frozen frozen = readCompletedFrozen(store, targetIssue);
        if (frozen == null)
            return Materialization.fail(targetIssue, "COMPLETED_FROZEN_SNAPSHOT_MISSING");
        if (frozen.frozenAt <= 0L || draw.openTime <= frozen.frozenAt)
            return Materialization.fail(targetIssue, "PIT_TIME_ORDER_INVALID");

        WriteResult written =
                store.coordinatorR36()
                        .runWrite(
                                "R811_ACTION_LABEL_1200_STREAM",
                                new DatabaseCoordinator.WriteCallable<WriteResult>() {
                                    @Override
                                    public WriteResult call(SQLiteDatabase db) {
                                        return streamAndPersist(db, targetIssue, draw);
                                    }
                                },
                                WriteResult.fail("DATABASE_WRITE_FAILED"));
        if (!written.success) return Materialization.fail(targetIssue, written.status);
        GroupVerification persisted = verifyGroup(store, targetIssue);
        return persisted.passed
                ? new Materialization(
                        true, targetIssue, persisted.rows, persisted.groupHash, "COMPLETED")
                : Materialization.fail(targetIssue, "PERSIST_VERIFY_FAILED");
    }

    private static WriteResult streamAndPersist(
            SQLiteDatabase db, String targetIssue, Models.Draw draw) {
        Cursor cursor = null;
        boolean owns = !db.inTransaction();
        if (owns) db.beginTransaction();
        try {
            cursor =
                    db.rawQuery(
                            "SELECT position,n1,n2,n3,action_hash,record_hash FROM "
                                    + V51Repository.ACTION_SPACE
                                    + " WHERE target_issue=? ORDER BY position,n1,n2,n3",
                            new String[] {targetIssue});
            int expected = 0;
            int[] perPosition = new int[10];
            StringBuilder hashes = new StringBuilder(1200 * 64);
            while (cursor.moveToNext()) {
                int position = cursor.getInt(0);
                int n1 = cursor.getInt(1);
                int n2 = cursor.getInt(2);
                int n3 = cursor.getInt(3);
                String actionHash = safe(cursor.getString(4));
                String recordHash = safe(cursor.getString(5));
                int ordinal = K3ActionSpaceEngine.globalOrdinal(position, n1, n2, n3);
                if (ordinal != expected
                        || position < 1
                        || position > 10
                        || !DecisionGateEngine.b12K3(n1, n2, n3)
                        || actionHash.length() != 64
                        || recordHash.length() != 64)
                    throw new IllegalStateException("R811_STREAM_ACTION_INVALID_" + expected);
                int actual = draw.getNumberAtPosition(position);
                int hit = actual == n1 || actual == n2 || actual == n3 ? 1 : 0;
                String labelHash =
                        V51ChecksumEngine.sha256(
                                targetIssue
                                        + '|'
                                        + ordinal
                                        + '|'
                                        + actionHash
                                        + '|'
                                        + actual
                                        + '|'
                                        + hit);
                Label label =
                        new Label(
                                targetIssue,
                                ordinal,
                                actionHash,
                                position,
                                n1,
                                n2,
                                n3,
                                actual,
                                hit,
                                labelHash);
                long row =
                        db.insertWithOnConflict(
                                "v57_research_action_label_r811",
                                null,
                                values(label),
                                SQLiteDatabase.CONFLICT_IGNORE);
                if (row < 0L && !matches(db, label))
                    throw new IllegalStateException("R811_ACTION_LABEL_CONFLICT_" + ordinal);
                hashes.append(labelHash);
                perPosition[position - 1]++;
                expected++;
            }
            if (expected != 1200)
                throw new IllegalStateException("R811_ACTION_LABEL_COUNT_" + expected);
            for (int p = 0; p < perPosition.length; p++)
                if (perPosition[p] != 120)
                    throw new IllegalStateException(
                            "R811_ACTION_LABEL_POSITION_" + (p + 1) + '_' + perPosition[p]);
            if (owns) db.setTransactionSuccessful();
            return new WriteResult(true, V51ChecksumEngine.sha256(hashes.toString()), "OK");
        } finally {
            if (cursor != null) cursor.close();
            if (owns && db.inTransaction()) db.endTransaction();
        }
    }

    private static ContentValues values(Label label) {
        ContentValues values = new ContentValues();
        values.put("target_issue", label.targetIssue);
        values.put("global_ordinal", label.globalOrdinal);
        values.put("action_hash", label.actionHash);
        values.put("position", label.position);
        values.put("n1", label.n1);
        values.put("n2", label.n2);
        values.put("n3", label.n3);
        values.put("actual_number", label.actualNumber);
        values.put("hit_label", label.hitLabel);
        values.put("label_hash", label.labelHash);
        values.put("created_at", TimeSourceR811.wallNowMs());
        return values;
    }

    /** Host-parity helper retained for deterministic snapshot fixture verification. */
    public static List<Label> label1200(
            DeferredEvidenceSnapshotR36.Snapshot snapshot, Models.Draw draw) {
        if (snapshot == null
                || draw == null
                || !draw.isValid()
                || !snapshot.targetIssue.equals(draw.issue)
                || snapshot.actions.size() != 1200)
            throw new IllegalArgumentException("R811_ACTION_LABEL_INPUT");
        Label[] ordered = new Label[1200];
        for (int i = 0; i < snapshot.actions.size(); i++) {
            DeferredEvidenceSnapshotR36.ActionRow action = snapshot.actions.get(i);
            int ordinal =
                    K3ActionSpaceEngine.globalOrdinal(
                            action.position, action.n1, action.n2, action.n3);
            if (ordinal < 0 || ordinal >= 1200 || ordered[ordinal] != null)
                throw new IllegalStateException("R811_ACTION_ORDINAL_" + ordinal);
            int actual = draw.getNumberAtPosition(action.position);
            int hit = actual == action.n1 || actual == action.n2 || actual == action.n3 ? 1 : 0;
            String hash =
                    V51ChecksumEngine.sha256(
                            snapshot.targetIssue
                                    + '|'
                                    + ordinal
                                    + '|'
                                    + action.actionHash
                                    + '|'
                                    + actual
                                    + '|'
                                    + hit);
            ordered[ordinal] =
                    new Label(
                            snapshot.targetIssue,
                            ordinal,
                            action.actionHash,
                            action.position,
                            action.n1,
                            action.n2,
                            action.n3,
                            actual,
                            hit,
                            hash);
        }
        ArrayList<Label> output = new ArrayList<Label>(1200);
        for (int i = 0; i < ordered.length; i++) {
            if (ordered[i] == null)
                throw new IllegalStateException("R811_ACTION_ORDINAL_GAP_" + i);
            output.add(ordered[i]);
        }
        return Collections.unmodifiableList(output);
    }

    public static boolean verifyGroup(List<Label> labels) {
        if (labels == null || labels.size() != 1200) return false;
        int[] perPosition = new int[10];
        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);
            if (label == null
                    || label.globalOrdinal != i
                    || label.position < 1
                    || label.position > 10
                    || label.labelHash.length() != 64
                    || label.actionHash.length() != 64
                    || label.hitLabel < 0
                    || label.hitLabel > 1)
                return false;
            perPosition[label.position - 1]++;
        }
        for (int count : perPosition) if (count != 120) return false;
        return true;
    }

    public static GroupVerification verifyGroup(DbStore store, String targetIssue) {
        if (store == null) return new GroupVerification(false, 0, "");
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT global_ordinal,label_hash FROM"
                                            + " v57_research_action_label_r811 WHERE target_issue=?"
                                            + " ORDER BY global_ordinal ASC",
                                    new String[] {targetIssue});
            int expected = 0;
            StringBuilder hashes = new StringBuilder(1200 * 64);
            while (cursor.moveToNext()) {
                if (cursor.getInt(0) != expected++)
                    return new GroupVerification(false, expected, "");
                String hash = safe(cursor.getString(1));
                if (hash.length() != 64)
                    return new GroupVerification(false, expected, "");
                hashes.append(hash);
            }
            return new GroupVerification(
                    expected == 1200,
                    expected,
                    expected == 1200 ? V51ChecksumEngine.sha256(hashes.toString()) : "");
        } catch (Throwable error) {
            return new GroupVerification(false, 0, "");
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static Frozen readCompletedFrozen(DbStore store, String targetIssue) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT j.snapshot_hash,o.commit_at FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " j JOIN "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o ON o.target_issue=j.target_issue"
                                            + " WHERE j.target_issue=? AND j.status='COMPLETED'"
                                            + " AND j.core_materialized=1 AND j.action_rows=1200"
                                            + " AND o.pre_draw_frozen=1 LIMIT 1",
                                    new String[] {targetIssue});
            if (!cursor.moveToFirst()) return null;
            String hash = safe(cursor.getString(0));
            return hash.length() == 64 ? new Frozen(hash, cursor.getLong(1)) : null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static boolean matches(SQLiteDatabase db, Label label) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT label_hash FROM v57_research_action_label_r811"
                                    + " WHERE target_issue=? AND global_ordinal=?",
                            new String[] {
                                label.targetIssue, String.valueOf(label.globalOrdinal)
                            });
            return cursor.moveToFirst() && label.labelHash.equals(safe(cursor.getString(0)));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Frozen {
        final String snapshotHash;
        final long frozenAt;

        Frozen(String hash, long frozenAt) {
            snapshotHash = hash;
            this.frozenAt = frozenAt;
        }
    }

    private static final class WriteResult {
        final boolean success;
        final String groupHash, status;

        WriteResult(boolean success, String hash, String status) {
            this.success = success;
            groupHash = safe(hash);
            this.status = safe(status);
        }

        static WriteResult fail(String status) {
            return new WriteResult(false, "", status);
        }
    }

    public static final class Label {
        public final String targetIssue, actionHash, labelHash;
        public final int globalOrdinal, position, n1, n2, n3, actualNumber, hitLabel;

        Label(
                String target,
                int ordinal,
                String actionHash,
                int position,
                int n1,
                int n2,
                int n3,
                int actual,
                int hit,
                String labelHash) {
            targetIssue = safe(target);
            globalOrdinal = ordinal;
            this.actionHash = safe(actionHash);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            actualNumber = actual;
            hitLabel = hit;
            this.labelHash = safe(labelHash);
        }
    }

    public static final class GroupVerification {
        public final boolean passed;
        public final int rows;
        public final String groupHash;

        GroupVerification(boolean passed, int rows, String hash) {
            this.passed = passed;
            this.rows = rows;
            groupHash = safe(hash);
        }
    }

    public static final class Materialization {
        public final boolean success;
        public final String targetIssue, groupHash, status;
        public final int rows;

        Materialization(boolean success, String target, int rows, String hash, String status) {
            this.success = success;
            targetIssue = safe(target);
            this.rows = rows;
            groupHash = safe(hash);
            this.status = safe(status);
        }

        static Materialization fail(String target, String status) {
            return new Materialization(false, target, 0, "", status);
        }
    }
}
