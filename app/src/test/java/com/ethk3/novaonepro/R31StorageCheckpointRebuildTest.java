package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;

/** R31-B05-C09#A1/#A2a/#A3 and R31-B05-C10#A1-#A3: non-blocking WAL checkpoints and a provable rebuild swap. */
public final class R31StorageCheckpointRebuildTest {
    static final class FakeDb implements StorageCheckpointR31.Database {
        boolean longReader = true, realtime; int cancels; long slice = 100; final List<String> calls = new ArrayList<>();
        public StorageCheckpointR31.Result checkpoint(String mode) { calls.add(mode); return longReader ? new StorageCheckpointR31.Result(1, 9000, 1200) : new StorageCheckpointR31.Result(0, 9000, 9000); }
        public boolean inRealtimeWindow(long now) { return realtime; }
        public boolean cancelStaleReaders() { cancels++; if (cancels >= 2) longReader = false; return !longReader; }
        public long sliceRemainingMs() { return slice; }
    }
    @Test public void aLongReaderCausesBoundedBackoffAndTheWalRecoversWithEvidence() {
        StorageCheckpointR31 c = new StorageCheckpointR31(); FakeDb db = new FakeDb(); long now = 0; int attempts = 0; String last = "";
        for (int i = 0; i < 200 && !last.equals("TRUNCATED"); i++) { String r = c.round(db, now); if (!r.startsWith("BACKOFF_UNTIL")) attempts++; last = r; now += 1000; }
        assertEquals("the WAL is truncated once the reader's owner cancelled it", "TRUNCATED", last);
        assertTrue("no unbounded busy loop: " + attempts, attempts < 20);
        assertEquals(false, c.json().get("busyIsCorruption")); assertTrue(c.json().get("evidence").toString().contains("TRUNCATE busy=0"));
        assertTrue("checkpoints wait at most the cap between attempts", (Long) c.json().get("busyRounds") >= 1);
    }
    @Test public void theRealtimeWindowIsNeverBlockedAndTheOfflineServiceIsNeverCalled() throws Exception {
        StorageCheckpointR31 c = new StorageCheckpointR31(); FakeDb db = new FakeDb(); db.realtime = true;
        for (int i = 0; i < 50; i++) assertEquals("SKIPPED_REALTIME_WINDOW", c.round(db, i * 10L));
        assertTrue("no checkpoint call during the freeze/settle window", db.calls.isEmpty());
        db.realtime = false; db.longReader = false; db.slice = 5; assertEquals("PASSIVE_COMPLETE", c.round(db, 1000));
        assertFalse("a short slice never attempts TRUNCATE", db.calls.contains("TRUNCATE"));
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/ethk3/novaonepro/StorageCheckpointR31.java")), "UTF-8");
        String code = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        assertFalse("daily GC never calls the offline maintenance service", code.contains("StorageMaintenanceServiceR82") || code.contains("rebuildAndSwap"));
        assertEquals(false, c.json().get("offlineServiceInvoked"));
    }

    static void sync(String d) {}
    static File setup(File dir, String name, String content) throws IOException { File f = new File(dir, name); Files.write(f.toPath(), content.getBytes("UTF-8")); return f; }
    static void journal(File db, String state, String oldHash, String newHash) throws IOException {
        Properties p = new Properties(); p.setProperty("database", db.getCanonicalPath()); p.setProperty("source_sha256", oldHash); p.setProperty("candidate_sha256", newHash); p.setProperty("state", state);
        try (OutputStream out = new FileOutputStream(new File(db.getCanonicalFile().getParentFile(), db.getName() + ".r82-maintenance-journal"))) { p.store(out, "test"); }
    }
    static String sha(File f) throws Exception { return com.ethk3.novaonepro.strategyv2.StatsR27.sha256(Files.readAllBytes(f.toPath())); }
    @Test public void everyRenameAndJournalPointRecoversTheOriginalOrTheVerifiedCopy() throws Exception {
        StorageMaintenanceFileR82.directorySyncR31 = R31StorageCheckpointRebuildTest::sync;
        for (String point : new String[]{"PREPARED_BEFORE_RENAME", "ORIGINAL_RENAMED_DB_MISSING", "INSTALLED_BEFORE_RECEIPT"}) {
            File dir = Files.createTempDirectory("r31-rebuild-" + point).toFile(); File db = setup(dir, "main.db", "original-bytes"), out = new File(dir, "main.db.r82-rebuilt"), old = new File(dir, "main.db.r82-original-backup");
            Files.write(out.toPath(), "rebuilt-bytes".getBytes("UTF-8")); String oldHash = sha(db), newHash = sha(out);
            String result;
            switch (point) {
                case "PREPARED_BEFORE_RENAME": journal(db, "PREPARED", oldHash, newHash); result = StorageMaintenanceFileR82.recover(db);
                    assertEquals("RECOVERED_ORIGINAL_BEFORE_SWAP", result); assertEquals(oldHash, sha(db)); assertFalse(out.exists()); break;
                case "ORIGINAL_RENAMED_DB_MISSING": journal(db, "ORIGINAL_RENAMED", oldHash, newHash); Files.move(db.toPath(), old.toPath()); result = StorageMaintenanceFileR82.recover(db);
                    assertEquals("RECOVERED_ORIGINAL_AFTER_INTERRUPTED_RENAME", result); assertEquals(oldHash, sha(db)); break;
                default: journal(db, "INSTALLED", oldHash, newHash); Files.move(db.toPath(), old.toPath()); Files.move(out.toPath(), db.toPath()); result = StorageMaintenanceFileR82.recover(db);
                    assertTrue(result, result.startsWith("RECOVERED_INSTALL")); assertEquals(newHash, sha(db)); assertEquals("the original stays as backup", oldHash, sha(old));
            }
            assertFalse("journal resolved", new File(dir, "main.db.r82-maintenance-journal").exists());
        }
        File dir = Files.createTempDirectory("r31-rebuild-uncertain").toFile(); File db = setup(dir, "main.db", "third-bytes");
        journal(db, "ORIGINAL_RENAMED", com.ethk3.novaonepro.strategyv2.StatsR27.sha256("a"), com.ethk3.novaonepro.strategyv2.StatsR27.sha256("b"));
        try { StorageMaintenanceFileR82.recover(db); fail("uncertain state guessed"); } catch (IOException expected) { assertTrue(expected.getMessage().contains("preserved all files")); }
    }
    static Properties proof(String sha, String method) {
        Properties p = new Properties(); for (String k : StorageMaintenanceFileR82.RESTORE_PROOF_KEYS_R31) p.setProperty(k, "PASS:" + k);
        p.setProperty("restore_active_sha256", sha); p.setProperty("restore_method", method); return p;
    }
    @Test public void aBackupIsDeletedOnlyWithACompleteRestoreProofOfTheExactBytes() throws Exception {
        String sha = com.ethk3.novaonepro.strategyv2.StatsR27.sha256("active");
        assertNull(StorageMaintenanceFileR82.restoreProofRefusalR31(proof(sha, "RESTART_AND_FULL_VERIFY"), sha));
        Properties none = new Properties(); assertEquals("REFUSED_NO_RESTORE_PROOF:restore_active_sha256", StorageMaintenanceFileR82.restoreProofRefusalR31(none, sha));
        for (String weak : new String[]{"QUICK_CHECK_ONLY", "MTIME", "FILE_SIZE"}) assertEquals("REFUSED_RESTORE_PROOF_METHOD:" + weak, StorageMaintenanceFileR82.restoreProofRefusalR31(proof(sha, weak), sha));
        assertEquals("REFUSED_RESTORE_PROOF_FOR_OTHER_BYTES", StorageMaintenanceFileR82.restoreProofRefusalR31(proof(sha, "RESTART_AND_FULL_VERIFY"), com.ethk3.novaonepro.strategyv2.StatsR27.sha256("other")));
        Properties failed = proof(sha, "RESTART_AND_FULL_VERIFY"); failed.setProperty("restore_model_load", "FAIL:missing checkpoint");
        assertEquals("REFUSED_RESTORE_PROOF_NOT_PASSED:restore_model_load", StorageMaintenanceFileR82.restoreProofRefusalR31(failed, sha));
        StorageMaintenanceFileR82.directorySyncR31 = R31StorageCheckpointRebuildTest::sync;
        File dir = Files.createTempDirectory("r31-backup").toFile(); File db = setup(dir, "main.db", "active"), old = setup(dir, "main.db.r82-original-backup", "original");
        Properties receipt = new Properties(); receipt.setProperty("database", db.getCanonicalPath()); receipt.setProperty("source_sha256", sha(old));
        try (OutputStream out = new FileOutputStream(new File(dir, "main.db.r82-backup-receipt"))) { receipt.store(out, "test"); }
        try { StorageMaintenanceFileR82.deleteVerifiedBackup(db); fail("backup deleted without restore proof"); } catch (IOException expected) { assertTrue(expected.getMessage().startsWith("REFUSED_NO_RESTORE_PROOF")); }
        assertTrue("backup kept", old.exists());
        StorageMaintenanceFileR82.recordRestoreProofR31(db, proof(sha(db), "RESTART_AND_FULL_VERIFY"));
        assertEquals("original".length(), StorageMaintenanceFileR82.deleteVerifiedBackup(db)); assertFalse(old.exists());
    }
}
