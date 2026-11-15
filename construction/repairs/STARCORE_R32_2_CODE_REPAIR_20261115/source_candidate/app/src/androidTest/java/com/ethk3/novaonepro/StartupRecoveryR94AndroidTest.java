package com.ethk3.novaonepro;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android handlers and production SQLite DDL. Baseline mode targets the exact .93
 * original-signed APK solely to reproduce its two recovery defects and capture the schema oracle.
 * Candidate mode requires that oracle and .94. No product classes are packaged in this test APK. */
@RunWith(AndroidJUnit4.class)
public final class StartupRecoveryR94AndroidTest {
    private Context app;
    private boolean baseline, enabled;
    @Before public void bindExactTargetAndStopDisposableRuntime() throws Exception {
        app = ApplicationProvider.getApplicationContext();
        baseline = "baseline".equals(InstrumentationRegistry.getArguments().getString("r94Mode"));
        assertEquals(baseline ? 577251052 : 577251053,
                app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode);
        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);
        enabled = state.isUserEnabled();
        assertTrue(BootReceiver.stopAutoFetchService(app));
        SystemClock.sleep(300);
    }
    @After public void restoreDisposablePreference() {
        if (app != null) new ServiceHeartbeatStore(app).setUserEnabled(enabled);
    }
    private static Field field(Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static Method method(Class<?> owner, String name, Class<?>... types) throws Exception {
        Method m = owner.getDeclaredMethod(name, types); m.setAccessible(true); return m;
    }
    private static void stop(HandlerThread thread) throws Exception {
        if (thread == null) return;
        thread.quitSafely(); thread.join(5000); assertFalse("test owner must terminate", thread.isAlive());
    }
    @Test public void exitedInitializationOwnerIsRecreatedAndActuallyConsumesItsTask() throws Exception {
        AutoFetchService service = new AutoFetchService();
        HandlerThread old = new HandlerThread("r94-dead-init-fixture"); old.start();
        Handler dead = new Handler(old.getLooper()); stop(old);
        assertFalse("real dead Android queue rejects work", dead.post(() -> fail("dead task ran")));
        field(AutoFetchService.class, "startupInitThread").set(service, old);
        field(AutoFetchService.class, "startupInitHandler").set(service, dead);
        HandlerThread replacement = null;
        try {
            if (baseline) {
                // Invokes the actual .93 entry. Its false post result leaves this flag stuck.
                method(AutoFetchService.class, "ensureRuntimeInitializationR32").invoke(service);
                assertTrue("original rejected post strands initialization",
                        field(AutoFetchService.class, "runtimeInitializationRunning").getBoolean(service));
                assertSame(old, field(AutoFetchService.class, "startupInitThread").get(service));
                System.out.println("R94_BASELINE_REJECTED_INIT_POST_REPRODUCED");
            } else {
                Handler live = (Handler) method(AutoFetchService.class,
                        "ensureStartupInitOwnerR94").invoke(service);
                replacement = (HandlerThread) field(AutoFetchService.class, "startupInitThread").get(service);
                assertNotSame(old, replacement); assertTrue(replacement.isAlive());
                AtomicInteger consumed = new AtomicInteger(); CountDownLatch latch = new CountDownLatch(1);
                assertTrue(live.post(() -> { consumed.incrementAndGet(); latch.countDown(); }));
                assertTrue("replacement actually consumes", latch.await(5, TimeUnit.SECONDS));
                assertEquals(1, consumed.get());
                assertSame(live, method(AutoFetchService.class, "ensureStartupInitOwnerR94").invoke(service));
                assertSame(replacement, field(AutoFetchService.class, "startupInitThread").get(service));
                System.out.println("R94_CURRENT_INIT_OWNER_REAL_QUEUE_PASS");
            }
        } finally { stop(replacement); stop(old); }
    }
    @Test public void partialInitializationFailureDisposesItsIndependentLivenessOwner() throws Exception {
        AutoFetchService service = new AutoFetchService();
        HandlerThread pulse = new HandlerThread("r94-partial-liveness-fixture"); pulse.start();
        field(AutoFetchService.class, "livenessThreadR817").set(service, pulse);
        field(AutoFetchService.class, "livenessHandlerR817").set(service, new Handler(pulse.getLooper()));
        try {
            method(AutoFetchService.class, "resetPartialRuntimeAfterInitFailureR363").invoke(service);
            if (baseline) {
                assertTrue("original failure cleanup leaks this live owner", pulse.isAlive());
                assertSame(pulse, field(AutoFetchService.class, "livenessThreadR817").get(service));
                System.out.println("R94_BASELINE_PARTIAL_LIVENESS_LEAK_REPRODUCED");
            } else {
                pulse.join(5000); assertFalse(pulse.isAlive());
                assertNull(field(AutoFetchService.class, "livenessThreadR817").get(service));
                assertNull(field(AutoFetchService.class, "livenessHandlerR817").get(service));
                method(AutoFetchService.class, "resetPartialRuntimeAfterInitFailureR363").invoke(service);
                assertNull(field(AutoFetchService.class, "livenessThreadR817").get(service));
                assertFalse((Boolean) method(AutoFetchService.class,
                        "finishCancelledRuntimeInitializationR94").invoke(service));
                HandlerThread cancelled = new HandlerThread("r94-cancelled-init-fixture"); cancelled.start();
                try {
                    field(AutoFetchService.class, "livenessThreadR817").set(service, cancelled);
                    field(AutoFetchService.class, "livenessHandlerR817").set(service, new Handler(cancelled.getLooper()));
                    field(AutoFetchService.class, "runtimeInitializationRunning").setBoolean(service, true);
                    field(AutoFetchService.class, "runtimeInitializationCancelled").setBoolean(service, true);
                    assertTrue((Boolean) method(AutoFetchService.class,
                            "finishCancelledRuntimeInitializationR94").invoke(service));
                    cancelled.join(5000); assertFalse(cancelled.isAlive());
                    assertFalse(field(AutoFetchService.class, "runtimeInitializationRunning").getBoolean(service));
                    assertNull(field(AutoFetchService.class, "livenessThreadR817").get(service));
                } finally { stop(cancelled); }
                System.out.println("R94_CURRENT_PARTIAL_OWNER_CLEANUP_IDEMPOTENT_PASS");
            }
        } finally { stop(pulse); }
    }
    /** Only redirects a disposable physical database path; all SQLite classes, connection pools,
     * transactions, production helpers, SQL and migrations remain the actual Android runtime. */
    private static final class IsolatedDatabaseContext extends ContextWrapper {
        final File root;
        IsolatedDatabaseContext(Context app, File root) { super(app); this.root = root; }
        @Override public File getDatabasePath(String name) {
            assertEquals(AppConfig.DB_NAME, name); return new File(root, name);
        }
        @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                SQLiteDatabase.CursorFactory factory) {
            return SQLiteDatabase.openDatabase(getDatabasePath(name).getPath(), factory,
                    SQLiteDatabase.CREATE_IF_NECESSARY);
        }
        @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
            return SQLiteDatabase.openDatabase(getDatabasePath(name).getPath(), factory,
                    SQLiteDatabase.CREATE_IF_NECESSARY, handler);
        }
    }
    private static String hash(String text) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)))
            out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
    private static String schema(SQLiteDatabase db, boolean emit) throws Exception {
        JSONArray rows = new JSONArray();
        try (Cursor c = db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master "
                + "WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name", null)) {
            while (c.moveToNext()) {
                JSONArray row = new JSONArray();
                for (int i = 0; i < 4; i++) row.put(c.isNull(i) ? org.json.JSONObject.NULL : c.getString(i));
                rows.put(row); if (emit) System.out.println("R94_SCHEMA_OBJECT|" + row);
            }
        }
        assertTrue("actual production schema required", rows.length() > 50);
        return hash(rows.toString());
    }
    private void noRetainedColumnSnapshot(DbHelper helper) throws Exception {
        if (!baseline) assertNull("metadata cannot escape its schema transaction",
                field(DbHelper.class, "schemaColumnsR94").get(helper));
    }
    @Test public void exactSchemaOracleReopenAndRollbackRetainAllOriginalDdl() throws Exception {
        File root = new File(app.getCacheDir(), "r94-schema-" + System.nanoTime());
        assertTrue(root.mkdirs());
        IsolatedDatabaseContext context = new IsolatedDatabaseContext(app, root);
        DbHelper helper = new DbHelper(context);
        long started = SystemClock.elapsedRealtime();
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            long elapsed = SystemClock.elapsedRealtime() - started;
            db.execSQL("CREATE TABLE r94_schema_marker(id INTEGER PRIMARY KEY,payload BLOB NOT NULL)");
            db.execSQL("INSERT INTO r94_schema_marker VALUES(1,?)", new Object[]{new byte[]{3,1,4,1,5}});
            String oracle = schema(db, true);
            if (!baseline) {
                String expected = InstrumentationRegistry.getArguments().getString("r94ExpectedSchema", "");
                assertTrue("original .93 Android schema oracle must be supplied", expected.matches("[0-9a-f]{64}"));
                assertEquals("all table/index/trigger SQL stays byte-exact", expected, oracle);
            }
            assertEquals(36, db.getVersion()); noRetainedColumnSnapshot(helper);
            helper.close(); db = helper.getWritableDatabase();
            assertEquals(oracle, schema(db, false)); noRetainedColumnSnapshot(helper);
            try (Cursor c = db.rawQuery("SELECT payload FROM r94_schema_marker WHERE id=1", null)) {
                assertTrue(c.moveToFirst()); assertArrayEquals(new byte[]{3,1,4,1,5}, c.getBlob(0));
            }
            System.out.println("R94_SCHEMA_ORACLE|mode=" + (baseline ? "baseline" : "candidate")
                    + "|sha256=" + oracle + "|onCreateMs=" + elapsed
                    + "|scope=DISPOSABLE_REAL_ANDROID_SQLITE_NOT_PHONE_PERFORMANCE");
        } finally { helper.close(); }
        File rollback = new File(root, "rollback.db");
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(rollback, null)) {
            Method ensure = method(DbHelper.class, "ensureSchema", SQLiteDatabase.class);
            String provisional;
            db.beginTransaction();
            try { ensure.invoke(helper, db); provisional = schema(db, false); noRetainedColumnSnapshot(helper); }
            finally { db.endTransaction(); }
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='draws'", null)) {
                assertTrue(c.moveToFirst()); assertEquals("outer rollback removes original DDL", 0, c.getInt(0));
            }
            ensure.invoke(helper, db);
            assertEquals("retry after rollback reads actual columns", provisional, schema(db, false));
            noRetainedColumnSnapshot(helper);
            try (Cursor c = db.rawQuery("PRAGMA quick_check(1)", null)) { assertTrue(c.moveToFirst()); assertEquals("ok", c.getString(0)); }
            System.out.println("R94_SCHEMA_ROLLBACK_REOPEN_IDEMPOTENCE_PASS");
        }
        // Dedicated cache paths only. The installed application's business DB is never removed.
        assertTrue(SQLiteDatabase.deleteDatabase(context.getDatabasePath(AppConfig.DB_NAME)));
        assertTrue(SQLiteDatabase.deleteDatabase(rollback)); assertTrue(root.delete());
    }
}
