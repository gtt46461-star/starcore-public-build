package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Fresh disposable emulator only. Synthetic draws never establish natural eligibility. */
@RunWith(AndroidJUnit4.class)
public final class DrawDependencyAndroidCP12Test {
    private static long count(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(c.moveToFirst());
            return c.getLong(0);
        }
    }

    private static String oldest(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT MIN(issue) FROM draws WHERE dirty=0", null)) {
            assertTrue(c.moveToFirst());
            return c.getString(0);
        }
    }

    private static Models.Draw draw(String issue, int ordinal) {
        Models.Draw d = new Models.Draw();
        d.issue = issue;
        d.openTime = 1790812800000L + ordinal * 60000L;
        d.fetchTime = d.openTime + 1000L;
        d.sourceLine = "SYNTHETIC_ANDROID_ROLLING_CONTRACT";
        for (int i = 0; i < 10; i++) d.numbers[i] = (i + ordinal) % 10 + 1;
        return d;
    }

    private static final String TEST_LEASE = "SYNTHETIC_CP12_HELD_SETTLEMENT";

    private static long saveWithHeldConsumer(DbStore store, Models.Draw draw) {
        // The real background recovery owner can correctly terminalize synthetic
        // BACKLOG rows without predraw observations. Make the unfinished-consumer
        // precondition explicit and atomic instead of relying on a scheduling race.
        return store.coordinatorR36().runAtomicWriteOrThrowF01(
                "TEST_SYNTHETIC_HELD_SETTLEMENT", db -> {
                    long row = store.saveDraw(draw);
                    assertTrue(row > 0);
                    android.content.ContentValues lease = new android.content.ContentValues();
                    lease.put("status", "RUNNING");
                    lease.put("lease_owner", TEST_LEASE);
                    lease.put("lease_until", System.currentTimeMillis() + 1800000L);
                    assertEquals("new task must be pending in the same transaction", 1,
                            db.update(DbHelper.TABLE_R36_SETTLEMENT_TASK, lease,
                                    "target_issue=? AND status='PENDING'", new String[]{draw.issue}));
                    return row;
                });
    }

    private static void assertHeldConsumer(SQLiteDatabase db, String issue) {
        try (Cursor c = db.rawQuery("SELECT status,lease_owner,lease_until FROM "
                + DbHelper.TABLE_R36_SETTLEMENT_TASK + " WHERE target_issue=?", new String[]{issue})) {
            assertTrue("consumer exists for " + issue, c.moveToFirst());
            assertEquals("RUNNING", c.getString(0));
            assertEquals(TEST_LEASE, c.getString(1));
            assertTrue("consumer lease is still active", c.getLong(2) > System.currentTimeMillis());
        }
    }

    @Test public void realAndroidStoreRollsAndProtectsUnfinishedSettlement() {
        // A durable lease alone is deliberately orphaned when the process gate is
        // idle. Hold the same production fence as the live settlement worker for
        // the entire unfinished-consumer fixture. Do not disable orphan recovery.
        assertTrue("isolated test must acquire the settlement fence",
                SettlementExecutionGateR819.tryEnter());
        try {
            exerciseRollingWithActiveSettlement();
        } finally {
            SettlementExecutionGateR819.exit();
        }
    }

    private static void exerciseRollingWithActiveSettlement() {
        DbStore store = new DbStore(ApplicationProvider.getApplicationContext());
        SQLiteDatabase db = store.readable();
        assertEquals("Run only on a freshly installed disposable emulator", 0, count(db, "draws"));
        String issue = "202610010001";
        for (int i = 0; i < 5000; i++) {
            Models.Draw input = draw(issue, i);
            assertTrue("initial ingress " + i,
                    (i < 65 ? saveWithHeldConsumer(store, input) : store.saveDraw(input)) > 0);
            if (i == 0) {
                assertEquals("active lease must not enter runnable queue", "",
                        SettlementTaskStoreR36.nextRunnable(store, System.currentTimeMillis()));
                assertEquals("active lease must not be terminalized", 0,
                        SettlementTaskStoreR36.terminalizeAuditedNoPredrawBacklog(store, 500));
                assertHeldConsumer(db, issue);
            }
            issue = TargetIssueResolver.next(issue).targetIssue;
        }
        assertEquals(5000, count(db, "draws"));
        assertEquals(0, count(db, DrawDependencyInputCP12.TABLE));
        assertEquals("active settlement cannot be recovered as an orphan", 0,
                SettlementTaskStoreR36.recoverImpossibleRunningLeasesWhenIdle(store, "ANDROID_TEST"));
        SettlementTaskStoreR36.terminalizeAuditedNoPredrawBacklog(store, 500);
        for (int i = 5000; i < 5064; i++) {
            String evicted = oldest(db);
            assertHeldConsumer(db, evicted);
            Models.Draw original = store.getDrawByIssue(evicted);
            assertNotNull(original);
            assertTrue(store.saveDraw(draw(issue, i)) > 0);
            assertEquals(5000, count(db, "draws"));
            assertFalse(evicted.equals(oldest(db)));
            Models.Draw retained = store.getDrawByIssue(evicted);
            assertHeldConsumer(db, evicted);
            assertNotNull("unfinished settlement input must remain available", retained);
            assertEquals(DrawHashR36.sha256V2(original), DrawHashR36.sha256V2(retained));
            issue = TargetIssueResolver.next(issue).targetIssue;
        }
        assertEquals(64, count(db, DrawDependencyInputCP12.TABLE));
        final String rejected = issue;
        final String before = oldest(db);
        assertHeldConsumer(db, before);
        assertTrue(store.coordinatorR36().runAtomicWrite("TEST_ROLLING_REJECTION", tx -> {
            tx.execSQL("CREATE TRIGGER test_android_reject BEFORE INSERT ON draws WHEN NEW.issue='"
                    + rejected + "' BEGIN SELECT RAISE(ABORT,'TEST_INSERT_FAILURE'); END");
        }));
        try {
            store.saveDraw(draw(issue, 5064));
            fail("injected insert failure must escape and roll back");
        } catch (RuntimeException expected) {
            assertEquals(before, oldest(db));
            assertEquals(5000, count(db, "draws"));
            assertEquals(64, count(db, DrawDependencyInputCP12.TABLE));
        }
        assertTrue(store.coordinatorR36().runAtomicWrite("TEST_ROLLING_RETRY", tx -> {
            tx.execSQL("DROP TRIGGER test_android_reject");
        }));
        Models.Draw retried = draw(issue, 5064);
        assertTrue(store.saveDraw(retried) > 0);
        assertEquals(-1, store.saveDraw(retried));
        assertEquals(65, count(db, DrawDependencyInputCP12.TABLE));
        assertTrue(store.coordinatorR36().runAtomicWrite("TEST_SYNTHETIC_SETTLEMENT_COMPLETE", tx -> {
            tx.execSQL("UPDATE " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                    + " SET status='COMPLETED',lease_owner='',lease_until=0 WHERE lease_owner=?",
                    new Object[]{TEST_LEASE});
        }));
        // Retire the next unleased synthetic BACKLOG inputs through the actual Owner,
        // so only the held consumers above determine the bounded-release assertion.
        SettlementTaskStoreR36.terminalizeAuditedNoPredrawBacklog(store, 500);
        for (int i = 5065; i < 5074; i++) {
            issue = TargetIssueResolver.next(issue).targetIssue;
            assertTrue(store.saveDraw(draw(issue, i)) > 0);
        }
        assertEquals(0, count(db, DrawDependencyInputCP12.TABLE));
        assertEquals(5000, store.getRecentDraws(6000).size());
        assertEquals(0, count(db, DbHelper.TABLE_TRADES));
        assertEquals(0, count(db, DbHelper.TABLE_R35_SHADOW_OBSERVATION));
        try (Cursor c = db.rawQuery("PRAGMA integrity_check", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("ok", c.getString(0));
        }
        System.out.println("ANDROID_CP12_ROLLING_PASS|initial=5000|pinned_rolls=64|rollback=PASS|natural=NOT_PROVEN");
    }
}

