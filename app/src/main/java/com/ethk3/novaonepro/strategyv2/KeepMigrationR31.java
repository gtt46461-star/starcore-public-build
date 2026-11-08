package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B05-C04: bounded migration of an R30 keep table (r30_keep_*) into an evidence lifecycle.
 *
 * <p>Stages per table: VERIFY_HASH (the original rows hash to the recorded digest) -> REGISTER_REFERENCE (every row
 * gets a reference entry) -> MIGRATE (bounded pages, each page's rows and the cursor commit together) -> SWITCH
 * (short transaction replaces the copy-on-delete trigger) -> DONE. Every stage commits its own durable checkpoint
 * with its work, so a process kill between any two commits resumes at the next uncommitted unit with no lost and no
 * duplicated row. New R31 writes are referenced, never copied into a permanent table. A keep table can be dropped
 * only after its consumers were migrated and a schema migration declares it; an empty table is no exception.</p>
 */
public final class KeepMigrationR31 {
    private KeepMigrationR31() {}
    public static final String VERSION = "R31_KEEP_MIGRATION_V1";
    public static final int PAGE_ROWS = 64;
    public enum Stage { VERIFY_HASH, REGISTER_REFERENCE, MIGRATE, SWITCH, DONE }

    /** Durable storage of one keep table's migration (implemented over SQLite by the retention owner). */
    public interface Store {
        List<Long> rowIds(String table);                 // keep table rows, ascending
        String rowDigest(String table, long rowId);       // hash of the original row bytes
        String recordedDigest(String table);              // digest recorded when the keep table was sealed
        Map<String, Object> checkpoint(String table);     // null before the first commit
        /** One atomic commit: the work of a unit and the checkpoint that names it. */
        void commit(String table, Map<String, Object> checkpoint, Map<Long, String> referencedRows, List<Long> migratedRows, boolean switchTrigger);
    }
    static String tableDigest(Store s, String table) {
        StringBuilder b = new StringBuilder(VERSION).append('|').append(table);
        for (long id : s.rowIds(table)) b.append('|').append(id).append('=').append(s.rowDigest(table, id));
        return StatsR27.sha256(b.toString());
    }
    static Map<String, Object> cp(Stage stage, long cursor, long migrated, long referenced) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("version", VERSION); m.put("stage", stage.name()); m.put("cursor", cursor); m.put("migrated", migrated); m.put("referenced", referenced); return m;
    }
    public static Stage stage(Store s, String table) { Map<String, Object> c = s.checkpoint(table); return c == null ? Stage.VERIFY_HASH : Stage.valueOf((String) c.get("stage")); }

    /** Runs one bounded unit of work and returns the stage reached. Safe to call again after any crash. */
    public static Stage step(Store s, String table) {
        Map<String, Object> c = s.checkpoint(table);
        Stage stage = c == null ? Stage.VERIFY_HASH : Stage.valueOf((String) c.get("stage"));
        long cursor = c == null ? -1 : ((Number) c.get("cursor")).longValue(), migrated = c == null ? 0 : ((Number) c.get("migrated")).longValue(), referenced = c == null ? 0 : ((Number) c.get("referenced")).longValue();
        switch (stage) {
            case VERIFY_HASH: {
                if (!tableDigest(s, table).equals(s.recordedDigest(table))) throw new IllegalStateException("R31_KEEP_ORIGINAL_HASH_MISMATCH:" + table);
                s.commit(table, cp(Stage.REGISTER_REFERENCE, -1, 0, 0), Collections.<Long, String>emptyMap(), Collections.<Long>emptyList(), false); return Stage.REGISTER_REFERENCE;
            }
            case REGISTER_REFERENCE: {
                Map<Long, String> page = new LinkedHashMap<>(); long last = cursor;
                for (long id : s.rowIds(table)) if (id > cursor && page.size() < PAGE_ROWS) { page.put(id, s.rowDigest(table, id)); last = id; }
                Stage next = page.size() < PAGE_ROWS || last == maxId(s, table) ? Stage.MIGRATE : Stage.REGISTER_REFERENCE;
                s.commit(table, cp(next, next == Stage.MIGRATE ? -1 : last, 0, referenced + page.size()), page, Collections.<Long>emptyList(), false); return next;
            }
            case MIGRATE: {
                List<Long> page = new ArrayList<>(); long last = cursor;
                for (long id : s.rowIds(table)) if (id > cursor && page.size() < PAGE_ROWS) { page.add(id); last = id; }
                Stage next = page.size() < PAGE_ROWS || last == maxId(s, table) ? Stage.SWITCH : Stage.MIGRATE;
                s.commit(table, cp(next, next == Stage.SWITCH ? -1 : last, migrated + page.size(), referenced), Collections.<Long, String>emptyMap(), page, false); return next;
            }
            case SWITCH: s.commit(table, cp(Stage.DONE, -1, migrated, referenced), Collections.<Long, String>emptyMap(), Collections.<Long>emptyList(), true); return Stage.DONE;
            default: return Stage.DONE;
        }
    }
    static long maxId(Store s, String table) { List<Long> ids = s.rowIds(table); return ids.isEmpty() ? -1 : ids.get(ids.size() - 1); }

    /** R31 writes after the switch: a reference to the original artifact, never a permanent copy. */
    public static String admitNewWrite(Store s, String table) { return stage(s, table) == Stage.DONE ? "REFERENCE_NOT_COPY" : "LEGACY_COPY_UNTIL_SWITCH"; }

    /** A keep table (empty or not) may be dropped only after every reader moved and a schema migration declares it. */
    public static String dropDecision(String table, Set<String> remainingReaders, boolean declaredBySchemaMigration, Stage stage) {
        if (stage != Stage.DONE) return "REFUSED_MIGRATION_NOT_DONE";
        if (!remainingReaders.isEmpty()) return "REFUSED_READERS_NOT_MIGRATED:" + new TreeSet<>(remainingReaders);
        if (!declaredBySchemaMigration) return "REFUSED_NO_SCHEMA_MIGRATION";
        return "DROP_ALLOWED_EVIDENCE_NOW_SUMMARY_ONLY:" + table;
    }
}
