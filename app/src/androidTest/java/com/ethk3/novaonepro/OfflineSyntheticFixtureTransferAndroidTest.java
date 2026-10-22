package com.ethk3.novaonepro;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Owned offline synthetic fixture transfer only; never a production migration or acceptance test. */
@RunWith(AndroidJUnit4.class)
public final class OfflineSyntheticFixtureTransferAndroidTest {
    private static final String SIGNER="ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f";
    private static final String MARKER="e495769c41cf3dcfb30ea816974b262ae733a415dd56380b0948fa656cc03104";
    private static final String READY="c0375d10f57c0d5fda3ca1c55fa5b14d5ce56b54d64c81dd344d13b2bce3f1d6";
    private static final long READY_BYTES=7479296L;
    private static String hex(byte[] bytes) {
        StringBuilder out=new StringBuilder();
        for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b&255));
        return out.toString();
    }
    private static String hash(File file) throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(FileInputStream input=new FileInputStream(file)) {
            byte[] bytes=new byte[32768];int n;
            while((n=input.read(bytes))!=-1)hash.update(bytes,0,n);
        }
        return hex(hash.digest());
    }
    private static void copyNew(File source,File destination) throws Exception {
        assertFalse("never overwrite an earlier fixture backup",destination.exists());
        try(FileInputStream input=new FileInputStream(source);FileOutputStream output=new FileOutputStream(destination)) {
            byte[] bytes=new byte[32768];int n;
            while((n=input.read(bytes))!=-1)output.write(bytes,0,n);
            output.getFD().sync();
        }
        assertEquals(source.length(),destination.length());assertEquals(hash(source),hash(destination));
    }
    private static File child(File parent,String name) throws Exception {
        File result=new File(parent.getCanonicalFile(),name);
        assertEquals("fixed local path only",result.getAbsolutePath(),result.getCanonicalPath());
        assertEquals(parent.getCanonicalFile(),result.getCanonicalFile().getParentFile());
        return result;
    }
    private static void syncDirectory(File directory) throws Exception {
        assertTrue("directory fsync requires an existing owned directory",directory.isDirectory());
        FileDescriptor fd=Os.open(directory.getCanonicalPath(),OsConstants.O_RDONLY,0);
        try {Os.fsync(fd);} finally {Os.close(fd);}
    }
    private static long scalar(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}
    }
    private static void validateReady(File source) throws Exception {
        assertEquals(READY_BYTES,source.length());assertEquals(READY,hash(source));
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(source.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)) {
            assertEquals(36,db.getVersion());
            try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)) {
                assertTrue(c.moveToFirst());assertEquals("ok",c.getString(0));assertFalse(c.moveToNext());
            }
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){assertFalse(c.moveToFirst());}
            for(String table:new String[]{"draws","trades","research_atom_spec_v2","research_molecule_seed_v2"})
                assertEquals("pinned empty synthetic source",0L,scalar(db,"SELECT COUNT(*) FROM "+table));
            try(Cursor c=db.rawQuery("SELECT state FROM v57_schema_domain_r926 WHERE domain='RESEARCH'",null)) {
                assertTrue(c.moveToFirst());assertEquals("READY",c.getString(0));assertFalse(c.moveToNext());
            }
        }
        assertEquals(READY,hash(source));
    }
    private static ThreadPoolExecutor background(DatabaseCoordinator coordinator) throws Exception {
        Field field=DatabaseCoordinator.class.getDeclaredField("backgroundWriter");field.setAccessible(true);
        return (ThreadPoolExecutor)field.get(coordinator);
    }
    private static boolean idle(ThreadPoolExecutor pool) {
        return AnalysisSingleFlightR36.shared().currentLease()==null
                &&!RealtimePressureGateR811.hasActiveCriticalWork()&&!SettlementExecutionGateR819.isActive()
                &&pool.getActiveCount()==0&&pool.getQueue().isEmpty();
    }
    @Test public void exportAndRestoreBeforeR939() throws Exception { transfer("before-r939"); }
    @Test public void exportAndRestoreBeforeShadow() throws Exception { transfer("before-shadow"); }
    @Test public void exportAfterShadowFinal() throws Exception { transfer("after-shadow-final"); }
    private static void transfer(String slot) throws Exception {
        final boolean exportOnly=slot.equals("after-shadow-final");
        assertTrue(exportOnly||slot.equals("before-r939")||slot.equals("before-shadow"));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.ethk3.novaonepro",context.getPackageName());
        PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNATURES);
        assertEquals(577251039,info.versionCode);
        assertEquals("V57.21.2.0.80-R5.4.2.80-1016-O09",info.versionName);
        assertEquals(1,info.signatures.length);
        assertEquals(SIGNER,hex(MessageDigest.getInstance("SHA-256").digest(info.signatures[0].toByteArray())));
        assertEquals(0,info.applicationInfo.flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE);
        File marker=child(context.getFilesDir(),"synthetic_r80_upgrade_storage_marker.txt");
        assertEquals(104L,marker.length());assertEquals(MARKER,hash(marker));
        File external=context.getExternalFilesDir(null);assertNotNull(external);
        File transfer=child(external,"r80-fixture-transfer");assertTrue(transfer.isDirectory());
        File source=child(transfer,"ready-empty.db");if(!exportOnly)validateReady(source);
        File backup=child(context.getFilesDir(),"r80-fixture-backup-"+slot);
        assertFalse("each explicit restore runs once",backup.exists());assertTrue(backup.mkdir());
        File exported=child(transfer,slot+"-previous.db");assertFalse(exported.exists());
        BootReceiver.persistServiceUserEnabled(context,false);
        BootReceiver.cancelAllRecovery(context,"SYNTHETIC_OWNED_FIXTURE_TRANSFER");
        context.stopService(new Intent(context,AutoFetchService.class));
        DatabaseCoordinator coordinator=DatabaseCoordinator.get(context);
        ThreadPoolExecutor pool=background(coordinator);long until=SystemClock.elapsedRealtime()+10000L;
        while(!idle(pool)&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(20L);
        assertTrue("all existing owners must become idle normally",idle(pool));
        File live=context.getDatabasePath(DatabaseCoordinator.DATABASE_NAME);
        File staged=child(live.getParentFile(),"r80-incoming-"+slot+".db");
        if(!exportOnly) {copyNew(source,staged);assertEquals(READY,hash(staged));}
        // Direct guard has unlock-only close; no runWrite wrapper can touch the closed DB afterward.
        try(DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()) {
            synchronized(coordinator.getHelper()) {
                assertTrue("no queued owner at transfer boundary",idle(pool));
                SQLiteDatabase db=coordinator.writable();assertFalse(db.inTransaction());
                assertEquals("owned synthetic fixture has no REAL trades",0L,scalar(db,"SELECT COUNT(*) FROM trades"));
                assertEquals("only our synthetic test dates are permitted",0L,scalar(db,
                        "SELECT COUNT(*) FROM draws WHERE issue<>'202610150500' AND NOT "
                        +"(length(issue)=12 AND issue GLOB '20260910[0-9][0-9][0-9][0-9]' "
                        +"AND CAST(substr(issue,9) AS INTEGER) BETWEEN 1 AND 243)"));
                for(String suffix:new String[]{"","-wal","-shm","-journal"}) {
                    File member=new File(live.getAbsolutePath()+suffix);
                    if(member.exists())copyNew(member,child(backup,"original"+suffix+".bin"));
                }
                try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)) {
                    assertTrue(c.moveToFirst());assertEquals("checkpoint must not be busy",0,c.getInt(0));
                    assertEquals("all WAL frames must be checkpointed",c.getInt(1),c.getInt(2));
                }
                coordinator.getHelper().close();
                File closed=child(backup,"closed-main.db");copyNew(live,closed);copyNew(live,exported);
                if(exportOnly) {
                    StringBuilder receipt=new StringBuilder("OWNED_SYNTHETIC_EXPORT_ONLY|slot="+slot+"\n");
                    for(String suffix:new String[]{"","-wal","-shm","-journal"}) {
                        File raw=child(backup,"original"+suffix+".bin");
                        if(raw.exists()) {
                            File saved=child(transfer,slot+"-raw"+suffix+".bin");copyNew(raw,saved);
                            receipt.append(saved.getName()).append("|bytes=").append(saved.length())
                                    .append("|sha256=").append(hash(saved)).append('\n');
                        }
                    }
                    receipt.append(exported.getName()).append("|bytes=").append(exported.length())
                            .append("|sha256=").append(hash(exported)).append('\n');
                    File report=child(transfer,slot+"-receipt.txt");assertFalse(report.exists());
                    try(FileOutputStream output=new FileOutputStream(report)) {
                        output.write(receipt.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        output.getFD().sync();
                    }
                    syncDirectory(backup);syncDirectory(transfer);syncDirectory(live.getParentFile());
                    System.out.println(receipt.toString());
                    return; // No replacement, renaming, clearing, or reopening of the live family.
                }
                for(String suffix:new String[]{"-wal","-shm","-journal"}) {
                    File member=new File(live.getAbsolutePath()+suffix);
                    if(member.exists()) {
                        if(!suffix.equals("-shm"))assertEquals("no uncheckpointed journal may be discarded",0L,member.length());
                        Os.rename(member.getAbsolutePath(),child(backup,"closed"+suffix+".bin").getAbsolutePath());
                    }
                }
                syncDirectory(backup);syncDirectory(transfer);syncDirectory(live.getParentFile());
                boolean replaced=false;
                try {
                    Os.rename(staged.getAbsolutePath(),live.getAbsolutePath());replaced=true;
                    syncDirectory(live.getParentFile());
                    assertEquals(READY_BYTES,live.length());assertEquals(READY,hash(live));
                } catch(Throwable failure) {
                    if(replaced) {
                        File recovery=child(live.getParentFile(),"r80-recovery-"+slot+".db");copyNew(closed,recovery);
                        Os.rename(recovery.getAbsolutePath(),live.getAbsolutePath());syncDirectory(live.getParentFile());
                        assertEquals(hash(closed),hash(live));
                    }
                    throw failure;
                }
                System.out.println("OFFLINE_FIXTURE_TRANSFER|slot="+slot+"|previousBytes="+exported.length()
                        +"|previousSha256="+hash(exported)+"|restoredBytes="+live.length()+"|restoredSha256="+READY
                        +"|rawFamilyBackup=true|closedBackup=true|scope=OWNED_SYNTHETIC_FIXTURE_ONLY"
                        +"|requiresExternalForceStopBeforeContract=true");
            }
        }
        // Do not reopen a DbStore here. Controller force-stops the process before the unchanged test.
    }
}
