package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.R29FoldBackR30;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * R30-B00-03 真实 SQLite（模拟器）上的 R29 回收测试。只在一次性模拟器上运行（会改写应用数据库）。
 * 五个场景各自独立：每个测试开始先清理自己造的表/触发器/文件。
 */
@RunWith(AndroidJUnit4.class)
public final class R30FoldBackInstrumentedTest {
    private static Context ctx() { return ApplicationProvider.getApplicationContext(); }
    private static long scalar(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { assertTrue(c.moveToFirst()); return c.getLong(0); } }
    private static String text(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getString(0) : null; } }

    private static void reset(DbStore store) throws Exception {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_RESET", db -> {
            try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='trigger' AND (name LIKE 'r29\\_critical%' ESCAPE '\\' OR name LIKE 'r29\\_copy\\_track%' ESCAPE '\\')", null)) {
                while (c.moveToNext()) db.execSQL("DROP TRIGGER IF EXISTS \"" + c.getString(0) + "\"");
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS r29_database_route(singleton INTEGER PRIMARY KEY CHECK(singleton=1),phase TEXT NOT NULL,database_name TEXT NOT NULL,schema_hash TEXT NOT NULL,source_root TEXT NOT NULL,epoch INTEGER NOT NULL,activated_at INTEGER NOT NULL)");
            db.execSQL("DELETE FROM r29_database_route");
            db.execSQL("CREATE TABLE IF NOT EXISTS r27_r30_probe(id INTEGER PRIMARY KEY, v TEXT)");
            db.execSQL("DELETE FROM r27_r30_probe");
            return 0;
        });
        File rp = ctx().getDatabasePath(R29FoldBackR30.RP_FILE);
        for (String s : new String[] {"", "-wal", "-shm", ".r30-retired", ".r30-retired-wal", ".r30-retired-shm"}) new File(rp.getPath() + s).delete();
    }

    @Test public void cp06DataIsNoOp() throws Exception {
        android.app.Application app = (android.app.Application) ctx();
        assertTrue(app instanceof StarcoreApplication);
        assertFalse("framework receiver dispatch requires the original base", app.getBaseContext() instanceof android.content.ContextWrapper);
        assertTrue(app.getDatabasePath("r30-framework-context.db").getPath().contains("/r30-instrumentation/"));
        assertNotEquals("framework base must not remove the scoped DB routing", app.getBaseContext().getDatabasePath("r30-framework-context.db"), app.getDatabasePath("r30-framework-context.db"));
        java.util.concurrent.CountDownLatch receiverFinished = new java.util.concurrent.CountDownLatch(1);
        android.content.Intent invalid = new android.content.Intent(app, InternalWatchdogReceiverR815.class)
                .setPackage(app.getPackageName()).setAction("R30_FRAMEWORK_CONTEXT_REJECTED_ACTION");
        app.sendOrderedBroadcast(invalid, null, new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context context, android.content.Intent intent) { receiverFinished.countDown(); }
        }, new android.os.Handler(android.os.Looper.getMainLooper()), android.app.Activity.RESULT_OK, null, null);
        assertTrue("actual framework delivery and original receiver goAsync must finish", receiverFinished.await(10, java.util.concurrent.TimeUnit.SECONDS));
        DbStore store = new DbStore(ctx()); reset(store);
        long t0 = android.os.SystemClock.elapsedRealtime();
        assertEquals("NONE", R29FoldBackR30.run(store));
        assertTrue("耗时应<50ms", android.os.SystemClock.elapsedRealtime() - t0 < 50);
        SQLiteDatabase ro = store.coordinatorForResearchR952().readable();
        assertTrue(text(ro, "SELECT state FROM r30_foldback_state WHERE singleton=1").startsWith("NO_ACTIVE_R29_ROUTE"));
    }

    @Test public void r29GuardTriggerIsDroppedAndWritesRecover() throws Exception {
        DbStore store = new DbStore(ctx()); reset(store);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_GUARD", db -> {
            db.execSQL("CREATE TRIGGER \"r29_critical_input_write_guard_r27_r30_probe\" BEFORE INSERT ON r27_r30_probe BEGIN SELECT RAISE(ABORT,'R29_GUARD'); END");
            return 0;
        });
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_BLOCKED", db -> { db.execSQL("INSERT INTO r27_r30_probe VALUES(1,'x')"); return 0; });
            fail("R29 guard 应阻止写入");
        } catch (RuntimeException expected) { /* 证明问题真实存在 */ }
        R29FoldBackR30.run(store);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_AFTER", db -> { db.execSQL("INSERT INTO r27_r30_probe VALUES(1,'x')"); return 0; });
        assertEquals(1, scalar(store.coordinatorForResearchR952().readable(), "SELECT count(*) FROM r27_r30_probe"));
        assertEquals(0, scalar(store.coordinatorForResearchR952().readable(), "SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name='r29_critical_input_write_guard_r27_r30_probe'"));
    }

    private static byte[] blobChunks(SQLiteDatabase db,String column){
        int length=(int)scalar(db,"SELECT length("+column+") FROM r27_r30_blob_fixture WHERE k='SYNTHETIC_NOT_NATURAL'");byte[] all=new byte[length];
        for(int at=0;at<length;){int need=Math.min(65536,length-at);try(Cursor c=db.rawQuery("SELECT substr("+column+","+(at+1)+","+need+") FROM r27_r30_blob_fixture WHERE k='SYNTHETIC_NOT_NATURAL'",null)){assertTrue(c.moveToFirst());byte[] part=c.getBlob(0);assertEquals(need,part.length);System.arraycopy(part,0,all,at,need);at+=need;}}return all;
    }

    @Test public void activeRouteIsFoldedBack() throws Exception {
        DbStore store = new DbStore(ctx()); reset(store);
        File rp = ctx().getDatabasePath(R29FoldBackR30.RP_FILE);
        SQLiteDatabase r = SQLiteDatabase.openOrCreateDatabase(rp, null);
        r.execSQL("CREATE TABLE r27_r30_probe(id INTEGER PRIMARY KEY, v TEXT)");
        r.execSQL("CREATE TABLE r27_r30_only_in_rp(k TEXT PRIMARY KEY, n INTEGER) WITHOUT ROWID");
        r.execSQL("CREATE TABLE r27_r30_blob_fixture(k TEXT PRIMARY KEY,checkpoint BLOB,optimizer BLOB,cursor INTEGER) WITHOUT ROWID");
        byte[] checkpoint=new byte[2*1024*1024],optimizer=new byte[4097];new java.util.Random(30).nextBytes(checkpoint);new java.util.Random(31).nextBytes(optimizer);
        r.execSQL("INSERT INTO r27_r30_blob_fixture VALUES('SYNTHETIC_NOT_NATURAL',?,?,241)",new Object[]{checkpoint,optimizer});
        r.beginTransaction();
        try { for (int i = 1; i <= 1234; i++) r.execSQL("INSERT INTO r27_r30_probe VALUES(?,?)", new Object[] {i, "rp" + i}); for (int i = 0; i < 37; i++) r.execSQL("INSERT INTO r27_r30_only_in_rp VALUES(?,?)", new Object[] {"k" + i, i}); r.setTransactionSuccessful(); }
        finally { r.endTransaction(); }
        r.close();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_ROUTE", db -> {
            db.execSQL("INSERT INTO r27_r30_probe VALUES(5,'stale-root')");
            db.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'h','r',1,1)", new Object[] {R29FoldBackR30.RP_FILE});
            return 0;
        });
        String out = R29FoldBackR30.run(store);
        assertTrue(out, out.startsWith("FOLDED_BACK"));
        SQLiteDatabase ro = store.coordinatorForResearchR952().readable();
        assertEquals(1234, scalar(ro, "SELECT count(*) FROM r27_r30_probe"));
        assertEquals("rp5", text(ro, "SELECT v FROM r27_r30_probe WHERE id=5"));
        assertEquals(37, scalar(ro, "SELECT count(*) FROM r27_r30_only_in_rp"));
        assertEquals("RETIRED_BY_R30", text(ro, "SELECT phase FROM r29_database_route WHERE singleton=1"));
        assertArrayEquals(checkpoint,blobChunks(ro,"checkpoint"));assertArrayEquals(optimizer,blobChunks(ro,"optimizer"));assertEquals(241,scalar(ro,"SELECT cursor FROM r27_r30_blob_fixture WHERE k='SYNTHETIC_NOT_NATURAL'"));
        assertFalse(rp.exists());
        assertTrue(new File(rp.getPath() + ".r30-retired").exists());
        assertEquals("NONE", R29FoldBackR30.run(store)); // 第二次启动为空操作
    }

    @Test public void activeRouteWithMissingRpKeepsRoot() throws Exception {
        DbStore store = new DbStore(ctx()); reset(store);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_ROUTE_MISSING", db -> {
            db.execSQL("INSERT INTO r27_r30_probe VALUES(9,'root')");
            db.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'h','r',1,1)", new Object[] {R29FoldBackR30.RP_FILE});
            return 0;
        });
        assertEquals("RP_MISSING", R29FoldBackR30.run(store));
        SQLiteDatabase ro = store.coordinatorForResearchR952().readable();
        assertEquals("root", text(ro, "SELECT v FROM r27_r30_probe WHERE id=9"));
        assertEquals("RETIRED_RP_MISSING_BY_R30", text(ro, "SELECT phase FROM r29_database_route WHERE singleton=1"));
    }
    @Test public void missingModelColumnCannotPublishFoldbackOrRetireOriginal()throws Exception{
        DbStore store=new DbStore(ctx());reset(store);File rp=ctx().getDatabasePath(R29FoldBackR30.RP_FILE);
        try(SQLiteDatabase source=SQLiteDatabase.openOrCreateDatabase(rp,null)){source.execSQL("CREATE TABLE r27_r30_probe(id INTEGER PRIMARY KEY,v TEXT,original_optimizer BLOB)");source.execSQL("INSERT INTO r27_r30_probe VALUES(1,'SYNTHETIC_MISSING_COLUMN',x'0001FF00')");}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TEST_INCOMPLETE_ROUTE",db->{db.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'h','r',1,1)",new Object[]{R29FoldBackR30.RP_FILE});return 0;});
        try{R29FoldBackR30.run(store);fail("missing model field was published as complete");}catch(IllegalStateException expected){assertTrue(expected.getMessage(),expected.getMessage().startsWith("R30_FOLDBACK_INCOMPLETE:"));}
        assertTrue(rp.isFile());assertFalse(new File(rp.getPath()+".r30-retired").exists());assertEquals("ACTIVE",text(store.readable(),"SELECT phase FROM r29_database_route WHERE singleton=1"));assertEquals("FAILED_ROUTE_KEPT",text(store.readable(),"SELECT state FROM r30_foldback_state WHERE singleton=1"));
        try(SQLiteDatabase source=SQLiteDatabase.openDatabase(rp.getPath(),null,SQLiteDatabase.OPEN_READONLY);Cursor c=source.rawQuery("SELECT hex(original_optimizer) FROM r27_r30_probe",null)){assertTrue(c.moveToFirst());assertEquals("0001FF00",c.getString(0));}
    }
}
