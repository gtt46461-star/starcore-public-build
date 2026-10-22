package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.os.Build;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real framework SQLite pool on the disposable API 35 emulator, never a host pool stub. */
@RunWith(AndroidJUnit4.class)
public final class SqliteBusyPoolR77Test {
    private static final long DEFAULT_ANDROID_BUSY_MS = 2500L;
    private static final long AWAIT_SECONDS = 10L;

    private static long scalar(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) {
            assertTrue("result required for " + sql, cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static long busy(SQLiteDatabase db) {
        return scalar(db, "PRAGMA busy_timeout");
    }

    private static SQLiteDatabase openWal(File path) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(path.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY
                        | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
        assertTrue("contract must exercise a real WAL connection pool",
                db.isWriteAheadLoggingEnabled());
        return db;
    }

    private static File path(String name) throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        return File.createTempFile("r77-busy-" + name + "-", ".db", context.getCacheDir());
    }

    private static long primaryTimeout(SQLiteDatabase db) {
        db.beginTransactionNonExclusive();
        try {
            long timeout = busy(db);
            db.setTransactionSuccessful();
            return timeout;
        } finally {
            db.endTransaction();
        }
    }

    private static void assertPrimaryTimeout(SQLiteDatabase db, long expected) {
        assertEquals("transaction pins the primary writer", expected, primaryTimeout(db));
    }

    private static void createColdReader(SQLiteDatabase db) {
        db.beginTransactionReadOnly();
        try {
            assertEquals("explicitly pinned cold reader", DEFAULT_ANDROID_BUSY_MS, busy(db));
        } finally {
            db.endTransaction();
        }
    }

    @Before public void requireActualApi35Pool() {
        // API 35 is the configured CI image. Fail rather than count an older API's
        // skipped future-reader case as successful pool coverage.
        assertTrue("API 35 public read-only transactions are required for this contract",
                Build.VERSION.SDK_INT >= 35);
        assertEquals("production cooperative SQL slice", 50, AppConfig.DB_BUSY_TIMEOUT_MS);
    }

    @Test public void oldPerQueryConfigurationLeavesFutureConnectionAtNativeDefault() throws Exception {
        File path = path("negative");
        try (SQLiteDatabase db = openWal(path)) {
            db.execSQL("CREATE TABLE sample(id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
            assertPrimaryTimeout(db, DEFAULT_ANDROID_BUSY_MS);
            createColdReader(db);
            // Exact unpinned R76 setter. rawQuery is not synonymous with reader:
            // API 35 prepares PRAGMAs with primary affinity, and busy_timeout
            // changes the connection during preparation. Record that primary
            // result without assuming it stays at the native default.
            assertEquals(50L, scalar(db, "PRAGMA busy_timeout=50"));
            assertEquals("ordinary follow-up appears configured", 50L, busy(db));
            long observedPrimary = primaryTimeout(db);
            assertPinnedExistingAndFuture(db, observedPrimary, 50L, DEFAULT_ANDROID_BUSY_MS);
            System.out.println("ANDROID_R77_BUSY_NEGATIVE|ordinary_query=50|primary="
                    + observedPrimary + "|existing_reader=50|future_reader=2500"
                    + "|old_query_does_not_configure_future_pool_connections=PASS");
        } finally {
            assertTrue("isolated negative fixture removed", SQLiteDatabase.deleteDatabase(path));
        }
    }

    private static void assertPinnedExistingAndFuture(SQLiteDatabase db, long primaryExpected,
            long existingExpected, long futureExpected) throws Exception {
        ExecutorService readers = Executors.newFixedThreadPool(2);
        CountDownLatch firstPinned = new CountDownLatch(1);
        CountDownLatch releaseReaders = new CountDownLatch(1);
        Future<Long> first = null;
        Future<Long> future = null;
        try {
            // Pin the primary and the one reader which already exists. A second
            // simultaneous read-only transaction can only run on a new third
            // connection. No reflection, custom pool or cursor-window assumption.
            db.beginTransactionNonExclusive();
            try {
                assertEquals("pinned primary", primaryExpected, busy(db));
                first = readers.submit(() -> {
                    db.beginTransactionReadOnly();
                    try {
                        long timeout = busy(db);
                        firstPinned.countDown();
                        assertTrue("bounded first-reader release",
                                releaseReaders.await(AWAIT_SECONDS, TimeUnit.SECONDS));
                        return timeout;
                    } finally {
                        firstPinned.countDown();
                        db.endTransaction();
                    }
                });
                assertTrue("existing reader pinned while primary is held",
                        firstPinned.await(AWAIT_SECONDS, TimeUnit.SECONDS));
                assertFalse("first reader remains checked out", first.isDone());
                future = readers.submit(() -> {
                    db.beginTransactionReadOnly();
                    try {
                        return busy(db);
                    } finally {
                        db.endTransaction();
                    }
                });
                assertEquals("forced new connection has expected timeout", Long.valueOf(futureExpected),
                        future.get(AWAIT_SECONDS, TimeUnit.SECONDS));
            } finally {
                releaseReaders.countDown();
                db.endTransaction();
                if (first != null) assertEquals("pinned existing reader", Long.valueOf(existingExpected),
                        first.get(AWAIT_SECONDS, TimeUnit.SECONDS));
                if (future != null) future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            releaseReaders.countDown();
            readers.shutdownNow();
            assertTrue("reader workers stopped", readers.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test public void productionConfigurationCoversExistingAndFutureConnections() throws Exception {
        File path = path("future");
        try (SQLiteDatabase db = openWal(path)) {
            db.execSQL("CREATE TABLE sample(id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
            createColdReader(db);
            DbHelper.configureBusyTimeoutR77(db);
            assertEquals(50L, busy(db));
            assertPrimaryTimeout(db, 50L);
            assertPinnedExistingAndFuture(db, 50L, 50L, 50L);
            // Per-pool configuration must also survive Android pool reconfiguration.
            db.setForeignKeyConstraintsEnabled(true);
            assertPrimaryTimeout(db, 50L);
            assertEquals(50L, busy(db));
            System.out.println("ANDROID_R77_BUSY_POOL_PASS|api=" + Build.VERSION.SDK_INT
                    + "|configured_primary=50|existing_reader=50|future_reader=50"
                    + "|reconfigure=PASS|scope=REAL_ANDROID_SQLITE_SYNTHETIC_NOT_PHONE_LATENCY");
        } finally {
            assertTrue("isolated future-reader fixture removed", SQLiteDatabase.deleteDatabase(path));
        }
    }

    @Test public void legacyPrimaryStrategyOnActualSQLite() throws Exception {
        File path = path("legacy-primary");
        try (SQLiteDatabase db = openWal(path)) {
            db.execSQL("CREATE TABLE sample(id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
            assertPrimaryTimeout(db, DEFAULT_ANDROID_BUSY_MS);
            DbHelper.configurePrimaryBusyTimeoutR77(db);
            assertFalse("legacy configuration releases its transaction", db.inTransaction());
            assertPrimaryTimeout(db, 50L);
            assertEquals("current reader also configured", 50L, busy(db));
            DbHelper.configurePrimaryBusyTimeoutR77(db);
            assertFalse("repeated configuration leaves no transaction", db.inTransaction());
            assertPrimaryTimeout(db, 50L);
            db.execSQL("INSERT INTO sample VALUES(1,'after-repeated-configuration')");
            assertEquals(1L, scalar(db, "SELECT COUNT(*) FROM sample"));
            System.out.println("ANDROID_R77_BUSY_LEGACY_STRATEGY|actual_api=" + Build.VERSION.SDK_INT
                    + "|cold_primary=2500|configured_primary=50|repeat=PASS"
                    + "|transaction_released=TRUE|legacy_os_runtime=NOT_RUN");
        } finally {
            assertTrue("isolated legacy strategy fixture removed", SQLiteDatabase.deleteDatabase(path));
        }
    }

    @Test public void externalWriteContentionIsBoundedAndAtomicAcrossReopen() throws Exception {
        File path = path("contention");
        try {
            try (SQLiteDatabase db = openWal(path)) {
                db.execSQL("CREATE TABLE sample(id INTEGER PRIMARY KEY, value TEXT NOT NULL)");
                DbHelper.configureBusyTimeoutR77(db);
                try (SQLiteDatabase blocker = openWal(path)) {
                    blocker.beginTransactionNonExclusive();
                    try {
                        blocker.execSQL("INSERT INTO sample VALUES(99,'external-uncommitted')");
                        boolean began = false;
                        boolean busyRejected = false;
                        long started = SystemClock.elapsedRealtime();
                        try {
                            db.beginTransactionNonExclusive();
                            began = true;
                            db.execSQL("INSERT INTO sample VALUES(1,'first')");
                            db.execSQL("INSERT INTO sample VALUES(2,'second')");
                            db.setTransactionSuccessful();
                        } catch (SQLiteDatabaseLockedException expected) {
                            busyRejected = true;
                        } finally {
                            if (began) db.endTransaction();
                        }
                        long elapsed = SystemClock.elapsedRealtime() - started;
                        assertTrue("independent SQLite writer must produce SQLITE_BUSY", busyRejected);
                        // Scheduling margin is deliberately far above the 50 ms SQL
                        // busy handler, but below the old native 2500 ms setting.
                        assertTrue("short busy timeout, elapsed=" + elapsed, elapsed < 1500L);
                        assertEquals("failed transaction exposes no partial rows", 0L,
                                scalar(db, "SELECT COUNT(*) FROM sample"));
                        System.out.println("ANDROID_R77_BUSY_CONTENTION|elapsed_ms=" + elapsed
                                + "|partial_rows=0|error=SQLiteDatabaseLockedException");
                    } finally {
                        // Roll back the external sentinel before the retry.
                        blocker.endTransaction();
                    }
                }
                assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM sample"));
                db.beginTransactionNonExclusive();
                try {
                    db.execSQL("INSERT INTO sample VALUES(1,'first')");
                    db.execSQL("INSERT INTO sample VALUES(2,'second')");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                assertEquals("retry after lock release commits both rows", 2L,
                        scalar(db, "SELECT COUNT(*) FROM sample"));
            }
            try (SQLiteDatabase reopened = openWal(path)) {
                // Closing a database discards its pool. Real onConfigure calls this
                // production function again for each newly opened pool.
                DbHelper.configureBusyTimeoutR77(reopened);
                assertPrimaryTimeout(reopened, 50L);
                assertEquals(50L, busy(reopened));
                assertEquals("committed retry survives reopen", 2L,
                        scalar(reopened, "SELECT COUNT(*) FROM sample"));
                assertEquals(0L, scalar(reopened, "SELECT COUNT(*) FROM sample WHERE id=99"));
            }
            System.out.println("ANDROID_R77_BUSY_CONTENTION_PASS|rollback=PASS|retry=PASS|reopen=PASS"
                    + "|scope=REAL_ANDROID_SQLITE_SYNTHETIC_NOT_PHONE_LATENCY");
        } finally {
            assertTrue("isolated contention fixture removed", SQLiteDatabase.deleteDatabase(path));
        }
    }
}
