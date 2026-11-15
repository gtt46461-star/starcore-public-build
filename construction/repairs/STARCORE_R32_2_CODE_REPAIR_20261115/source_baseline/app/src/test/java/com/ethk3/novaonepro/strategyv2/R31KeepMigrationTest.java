package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B05-C04#A1-#A3: staged keep-table migration with kill points, no permanent copies, guarded drop. */
public final class R31KeepMigrationTest {
    /** In-memory durable store; `killAfter` aborts a commit (nothing applied) to simulate a process kill. */
    static final class MemStore implements KeepMigrationR31.Store {
        final Map<String, TreeMap<Long, String>> rows = new HashMap<>(); final Map<String, String> recorded = new HashMap<>();
        final Map<String, Map<String, Object>> checkpoints = new HashMap<>(); final Map<String, Map<Long, String>> refs = new HashMap<>();
        final Map<String, List<Long>> migrated = new HashMap<>(); final Set<String> switched = new HashSet<>(); int commits, killAt = -1;
        public List<Long> rowIds(String t) { return new ArrayList<>(rows.get(t).keySet()); }
        public String rowDigest(String t, long id) { return StatsR27.sha256(rows.get(t).get(id)); }
        public String recordedDigest(String t) { return recorded.get(t); }
        public Map<String, Object> checkpoint(String t) { Map<String, Object> c = checkpoints.get(t); return c == null ? null : new LinkedHashMap<>(c); }
        public void commit(String t, Map<String, Object> cp, Map<Long, String> r, List<Long> m, boolean sw) {
            if (commits++ == killAt) throw new IllegalStateException("KILLED_BEFORE_COMMIT");
            checkpoints.put(t, new LinkedHashMap<>(cp)); refs.computeIfAbsent(t, k -> new LinkedHashMap<>()).putAll(r);
            migrated.computeIfAbsent(t, k -> new ArrayList<>()).addAll(m); if (sw) switched.add(t);
        }
    }
    static MemStore store(String table, int rows) {
        MemStore s = new MemStore(); TreeMap<Long, String> r = new TreeMap<>(); for (long i = 1; i <= rows; i++) r.put(i * 3, "original-row-" + i); s.rows.put(table, r);
        s.recorded.put(table, KeepMigrationR31.tableDigest(s, table)); return s;
    }
    static void runToDone(MemStore s, String t) { for (int i = 0; i < 1000 && KeepMigrationR31.stage(s, t) != KeepMigrationR31.Stage.DONE; i++) KeepMigrationR31.step(s, t); }

    @Test public void aKillAtEveryStageLosesAndDuplicatesNothing() {
        MemStore reference = store("r30_keep_r27_policy", 200); runToDone(reference, "r30_keep_r27_policy");
        int totalCommits = reference.commits;
        for (int kill = 0; kill < totalCommits; kill++) {
            MemStore s = store("r30_keep_r27_policy", 200); s.killAt = kill; boolean killed = false;
            for (int i = 0; i < 1000 && KeepMigrationR31.stage(s, "r30_keep_r27_policy") != KeepMigrationR31.Stage.DONE; i++) {
                try { KeepMigrationR31.step(s, "r30_keep_r27_policy"); } catch (IllegalStateException e) { assertEquals("KILLED_BEFORE_COMMIT", e.getMessage()); killed = true; }
            }
            assertTrue(killed);
            List<Long> m = s.migrated.get("r30_keep_r27_policy");
            assertEquals("kill " + kill + ": every row migrated exactly once", reference.migrated.get("r30_keep_r27_policy"), m);
            assertEquals(new HashSet<>(m).size(), m.size());
            assertEquals(reference.refs.get("r30_keep_r27_policy"), s.refs.get("r30_keep_r27_policy"));
            assertEquals(200L, s.checkpoints.get("r30_keep_r27_policy").get("migrated")); assertEquals(200L, s.checkpoints.get("r30_keep_r27_policy").get("referenced"));
        }
        MemStore tampered = store("r30_keep_r27_policy", 10); tampered.rows.get("r30_keep_r27_policy").put(3L, "changed");
        try { KeepMigrationR31.step(tampered, "r30_keep_r27_policy"); fail(); } catch (IllegalStateException e) { assertEquals("R31_KEEP_ORIGINAL_HASH_MISMATCH:r30_keep_r27_policy", e.getMessage()); }
    }

    @Test public void r31WritesAreReferencedAndOldKeepShrinksInBoundedPages() {
        MemStore s = store("r30_keep_r26_frozen", 300);
        assertEquals("LEGACY_COPY_UNTIL_SWITCH", KeepMigrationR31.admitNewWrite(s, "r30_keep_r26_frozen"));
        int before = s.commits; int pages = 0;
        while (KeepMigrationR31.stage(s, "r30_keep_r26_frozen") != KeepMigrationR31.Stage.DONE) { int migrated = s.migrated.getOrDefault("r30_keep_r26_frozen", Collections.<Long>emptyList()).size(); KeepMigrationR31.step(s, "r30_keep_r26_frozen"); assertTrue(s.migrated.getOrDefault("r30_keep_r26_frozen", Collections.<Long>emptyList()).size() - migrated <= KeepMigrationR31.PAGE_ROWS); pages++; }
        assertTrue("bounded pages, not one transaction: " + pages, pages >= 2 * (300 / KeepMigrationR31.PAGE_ROWS));
        assertTrue(s.switched.contains("r30_keep_r26_frozen"));
        assertEquals("after the switch new writes are references, never permanent copies", "REFERENCE_NOT_COPY", KeepMigrationR31.admitNewWrite(s, "r30_keep_r26_frozen"));
        assertTrue(s.commits > before);
    }

    @Test public void evenAnEmptyKeepTableNeedsConsumerProofAndASchemaMigration() {
        MemStore empty = store("r30_keep_su1_alpha_ledger", 0); runToDone(empty, "r30_keep_su1_alpha_ledger");
        KeepMigrationR31.Stage done = KeepMigrationR31.stage(empty, "r30_keep_su1_alpha_ledger");
        assertEquals(KeepMigrationR31.Stage.DONE, done);
        assertEquals("REFUSED_READERS_NOT_MIGRATED:[ReportR30]", KeepMigrationR31.dropDecision("r30_keep_su1_alpha_ledger", Collections.singleton("ReportR30"), true, done));
        assertEquals("REFUSED_NO_SCHEMA_MIGRATION", KeepMigrationR31.dropDecision("r30_keep_su1_alpha_ledger", Collections.<String>emptySet(), false, done));
        assertEquals("REFUSED_MIGRATION_NOT_DONE", KeepMigrationR31.dropDecision("t", Collections.<String>emptySet(), true, KeepMigrationR31.Stage.MIGRATE));
        assertTrue(KeepMigrationR31.dropDecision("r30_keep_su1_alpha_ledger", Collections.<String>emptySet(), true, done).startsWith("DROP_ALLOWED_EVIDENCE_NOW_SUMMARY_ONLY"));
        for (ResearchRetentionR30.TablePolicyR31 p : ResearchRetentionR30.POLICIES_R31) {
            assertFalse("default deny: " + p.table, ResearchRetentionR30.deletableByAgeR31(p.table)); assertEquals("NONE_DEFAULT_DENY", p.expiry); assertEquals(false, p.json().get("deletable"));
            if (p.table.contains("frozen") || p.table.contains("certificate") || p.table.contains("mainline")) assertEquals(p.table, "ORIGINAL_ROWS_REQUIRED_NO_SUMMARY_SUBSTITUTE", p.replay);
        }
        assertEquals(ResearchRetentionR30.PROTECTED_TABLES.size(), ResearchRetentionR30.POLICIES_R31.size());
    }
}
