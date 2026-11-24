package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Identifies the specific source an owner waits for. It never supplies replacement bytes. */
public final class CheckpointDependencyR341 {
    private CheckpointDependencyR341() {}
    public enum State { READY, WAIT_SOURCE, UNVERIFIED, CONFLICT }

    static final class Source {
        final State state;
        final String detail;
        final File file;
        Source(State state, String detail, File file) {
            this.state = state;
            this.detail = detail;
            this.file = file;
        }
    }

    static final class SourceWait extends IOException {
        final State state;
        SourceWait(State state, String hash, String detail) {
            super("CHECKPOINT_" + state + ":" + hash + ":" + detail);
            this.state = state;
        }
    }

    static Source resolve(File root, String hash, long bytes) throws IOException {
        if (root == null) return new Source(State.UNVERIFIED, "PATH_NOT_EXPORTED", null);
        if (!hash.matches("[a-f0-9]{64}") || bytes < 1L) return new Source(State.CONFLICT, "REFERENCE_IDENTITY", null);
        File declared = root.getAbsoluteFile();
        File actual = declared.getCanonicalFile();
        if (!declared.equals(actual)) return new Source(State.CONFLICT, "ROOT_ALIAS", null);
        if (!actual.isDirectory()) return new Source(State.WAIT_SOURCE, "CAS_ROOT_ABSENT", null);
        File file = new File(actual, hash + ".row");
        if (!file.getCanonicalFile().equals(file)) return new Source(State.CONFLICT, "CAS_FILE_ALIAS", null);
        if (!file.isFile()) return new Source(State.WAIT_SOURCE, "CAS_ROW_ABSENT", null);
        if (file.length() != bytes) return new Source(State.CONFLICT, "CAS_ROW_LENGTH", file);
        return new Source(State.UNVERIFIED, "CAS_ROW_PRESENT_NEEDS_DIGEST", file);
    }

    public static List<String> affectedOwners(String reference) {
        if (reference == null || reference.isEmpty()) return Collections.emptyList();
        String id = reference.split(":", 2)[0];
        if (RuntimeCoordinatorR26.declaresTaskR341(id)) return Collections.singletonList(id);
        if ("STARTUP_CHECKPOINT_EQUALITY".equals(id)) return Collections.singletonList(id);
        return Collections.emptyList();
    }

    public static Map<String, Object> export(SQLiteDatabase db, String afterHash) {
        if (afterHash != null && !afterHash.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("CHECKPOINT_PAGE_CURSOR");
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("scope", "EXACT_CHECKPOINT_RECOVERY_REFERENCES");
        if (!CheckpointRecoveryR335.ready(db)) {
            view.put("status", "MIGRATION_REQUIRED");
            view.put("references", null);
            return view;
        }
        List<Map<String, Object>> references = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT expected_hash,owner_identity,status,source_evidence,last_error,updated_at"
                + " FROM r335_checkpoint_recovery WHERE expected_hash>? ORDER BY expected_hash LIMIT 65",
                new String[]{afterHash == null ? "" : afterHash})) {
            while (cursor.moveToNext()) {
                Map<String, Object> original = new LinkedHashMap<>();
                original.put("expectedHash", cursor.getString(0));
                original.put("ownerReference", cursor.getString(1));
                original.put("affectedOwners", affectedOwners(cursor.getString(1)));
                String recorded = cursor.getString(2);
                State status = "WAIT_SOURCE".equals(recorded) ? State.WAIT_SOURCE
                        : "HASH_OR_SOURCE_INVALID_PRESERVED".equals(recorded) ? State.CONFLICT : State.UNVERIFIED;
                original.put("status", status.name());
                original.put("lastRecordedState", recorded);
                original.put("lastEvidence", cursor.getString(3));
                original.put("lastFailure", cursor.getString(4));
                original.put("lastObservedAt", cursor.getLong(5));
                original.put("root", "PATH_NOT_EXPORTED");
                references.add(original);
            }
        }
        boolean hasMore = references.size() > 64;
        if (hasMore) references.remove(64);
        view.put("status", references.isEmpty() ? "NO_REFERENCES_IN_PAGE" : "REFERENCES_OBSERVED");
        view.put("references", references);
        view.put("nextAfterHash", hasMore ? references.get(63).get("expectedHash") : null);
        return view;
    }
}
