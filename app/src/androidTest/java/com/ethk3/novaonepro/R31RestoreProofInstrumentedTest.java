package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real Android process/Binder/native restore on an explicitly disposable VM.
 * Synthetic fault input only; no HTTP, prediction, settled result or quality claim. */
@RunWith(AndroidJUnit4.class)
public final class R31RestoreProofInstrumentedTest {
    private static Context app(){
        assertEquals("true",InstrumentationRegistry.getArguments().getString("r31qRealService"));
        assertEquals("true",InstrumentationRegistry.getArguments().getString("r31RestoreDisposable"));
        assertTrue("DISPOSABLE_EMULATOR_REQUIRED",Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish")||Build.FINGERPRINT.contains("generic"));
        return ApplicationProvider.getApplicationContext();
    }
    private static void ready(DbStore store)throws Exception {
        long end=SystemClock.elapsedRealtime()+240000;
        do{store.requestLegacySchemaMigrationR31();if(store.coordinatorR36().isLegacyRuntimeSchemaReady())return;Thread.sleep(100);}while(SystemClock.elapsedRealtime()<end);
        fail("RESTORE_DISPOSABLE_LEGACY_SCHEMA_NOT_READY");
    }
    private static void await(Context app,String wanted)throws Exception {
        long end=SystemClock.elapsedRealtime()+600000;
        do{String phase=app.getSharedPreferences("r82_storage_maintenance",0).getString("phase","");
            if(!DatabaseCoordinator.isExplicitMaintenanceActiveR82()){
                if(wanted.equals(phase))return;
                if("FAILED_OR_RECOVERY_REQUIRED".equals(phase))fail("RESTORE_DISPOSABLE_MAINTENANCE_FAILED");
            }Thread.sleep(100);
        }while(SystemClock.elapsedRealtime()<end);fail("RESTORE_DISPOSABLE_TERMINAL_NOT_OBSERVED");
    }
    private static void awaitRefusal(Context app)throws Exception {
        long end=SystemClock.elapsedRealtime()+120000;
        do{if(!DatabaseCoordinator.isExplicitMaintenanceActiveR82()){
            assertEquals("FAILED_OR_RECOVERY_REQUIRED",app.getSharedPreferences("r82_storage_maintenance",0).getString("phase",""));return;
        }Thread.sleep(100);}while(SystemClock.elapsedRealtime()<end);fail("RESTORE_DISPOSABLE_REFUSAL_NOT_OBSERVED");
    }
    @Test public void independentRestoreBindsActualPidCloneAndCurrentBytesBeforeSeparateCleanup()throws Exception {
        Context app=app();DbStore store=new DbStore(app);ready(store);new ServiceHeartbeatStore(app).setUserEnabled(false);
        assertFalse("LIVE_RUNTIME_MUST_ALREADY_BE_STOPPED",AutoFetchService.isLifecycleActiveR82());
        File db=app.getDatabasePath(AppConfig.DB_NAME),backup=new File(db.getPath()+".r82-original-backup"),receiptFile=new File(db.getPath()+".r82-backup-receipt");
        assertFalse("FRESH_DISPOSABLE_BACKUP_REQUIRED",backup.exists());assertFalse(receiptFile.exists());
        byte[] payload=new byte[16384];new Random(3110).nextBytes(payload);
        ModelArtifactR29 model=new ModelArtifactR29(new AtomNetTrainerR27(12,31),new FtrlR27(7,.05,1,1e-4,1e-3),StatsR27.sha256("SYNTHETIC_DISPOSABLE_RESTORE_DATASET"),"");byte[] modelBytes=model.encode();
        store.coordinatorR36().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_RESTORE_DISPOSABLE_FIXTURE"),sql->{
            sql.execSQL("CREATE TABLE r31_restore_process_fixture(k TEXT PRIMARY KEY,payload BLOB NOT NULL)");sql.execSQL("INSERT INTO r31_restore_process_fixture(rowid,k,payload) VALUES(7919,'restore',?)",new Object[]{payload});
            sql.execSQL(ModelArtifactR29.LEGACY_DDL);sql.execSQL("INSERT INTO r29_legacy_model_checkpoint VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{"RESTORE_DISPOSABLE_CONTROL",StatsR27.sha256(modelBytes),modelBytes,"",0,.01,null,System.currentTimeMillis(),"SYNTHETIC_DISPOSABLE_FULL_MODEL_RESTORE_CONTROL"});return 1;
        });
        Models.Draw latest=store.getLatestDraw(),draw=new Models.Draw();draw.issue=latest==null?"202610240001":TargetIssueResolver.next(latest.issue).targetIssue;draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};draw.fetchTime=draw.openTime=System.currentTimeMillis();draw.rawJson="{\"scope\":\"SYNTHETIC_DISPOSABLE_RESTORE_CONTROL\"}";
        assertTrue("CONTROL_DRAW_NOT_PERSISTED",store.saveDraw(draw)>=0);assertTrue(SettlementTaskStoreR36.enqueueDraw(store,draw));
        store.coordinatorR36().runAtomicWriteOrThrowF01("R31_RESTORE_DISPOSABLE_ORPHAN",sql->{sql.execSQL("UPDATE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" SET status='RUNNING',lease_owner='RESTORE_DISPOSABLE',lease_until=?,attempts=3 WHERE target_issue=?",new Object[]{System.currentTimeMillis()+3600000,draw.issue});return 1;});
        StorageMaintenanceServiceR82.request(app,false);await(app,"RESTORE_VERIFIED_BACKUP_RETAINED");
        assertTrue(backup.isFile());Properties first=StorageMaintenanceFileR82.read(receiptFile);
        assertEquals(StorageRestoreProofR31.METHOD,first.getProperty("restore_method"));assertEquals("BINDER_PID_AND_FRESH_OFFLINE_NONCE",first.getProperty("restore_collector_authenticated"));assertEquals("PRIVATE_PROCESS_NO_CREDENTIAL_OWNER_NO_LIVE_DB",first.getProperty("restore_application_boot"));
        assertEquals(Integer.toString(android.os.Process.myPid()),first.getProperty("restore_parent_pid"));assertNotEquals(first.getProperty("restore_parent_pid"),first.getProperty("restore_probe_pid"));assertEquals(BuildConfig.SOURCE_COMMIT_R26,first.getProperty("restore_source_commit"));assertTrue(first.getProperty("restore_clone_sha256").matches("[0-9a-f]{64}"));assertNull(StorageMaintenanceFileR82.restoreProofRefusalR31(first,StorageMaintenanceFileR82.sha256(db)));
        Matcher recovery=Pattern.compile("recovered=([0-9]+)").matcher(first.getProperty("restore_pending_settlement_evidence"));assertTrue(recovery.find());assertTrue(Integer.parseInt(recovery.group(1))>=1);
        try(SQLiteDatabase read=SQLiteDatabase.openDatabase(db.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
            try(Cursor c=read.rawQuery("SELECT rowid,payload FROM r31_restore_process_fixture WHERE k='restore'",null)){assertTrue(c.moveToFirst());assertEquals(7919,c.getLong(0));assertArrayEquals(payload,c.getBlob(1));}
            try(Cursor c=read.rawQuery("SELECT status,attempts FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=?",new String[]{draw.issue})){assertTrue(c.moveToFirst());assertEquals("RUNNING",c.getString(0));assertEquals(3,c.getInt(1));}
        }
        assertEquals(db.getCanonicalPath(),new File(DatabaseCoordinator.peek().readable().getPath()).getCanonicalPath());assertFalse(new ServiceHeartbeatStore(app).isUserEnabled());
        // A later actual write invalidates the old exact-byte proof, even after
        // the cleanup owner successfully checkpoints and closes the WAL.
        store.coordinatorR36().runAtomicWriteOrThrowF01("R31_RESTORE_DISPOSABLE_NEW_BYTES",sql->{sql.execSQL("INSERT INTO r31_restore_process_fixture VALUES('later',?)",new Object[]{new byte[]{1}});return 1;});
        StorageMaintenanceServiceR82.request(app,true);awaitRefusal(app);assertTrue("BACKUP_DELETED_FOR_CHANGED_ACTIVE_BYTES",backup.isFile());
        StorageMaintenanceServiceR82.request(app,false);await(app,"RESTORE_VERIFIED_BACKUP_RETAINED");Properties second=StorageMaintenanceFileR82.read(receiptFile);assertNotEquals(first.getProperty("restore_nonce"),second.getProperty("restore_nonce"));assertNotEquals(first.getProperty("restore_active_sha256"),second.getProperty("restore_active_sha256"));assertTrue(backup.exists());
        StorageMaintenanceServiceR82.request(app,true);await(app,"BACKUP_EXPLICITLY_REMOVED");assertFalse(backup.exists());assertFalse(receiptFile.exists());assertFalse(new ServiceHeartbeatStore(app).isUserEnabled());
        System.out.println("R31_RESTORE_ANDROID|REAL_DISTINCT_PID_BINDER_PRIVATE_CLONE|ORIGINAL_MODEL_LOAD_AND_PENDING_RECOVERY|SOURCE_UNCHANGED_BY_PROBE|CHANGED_BYTES_REFUSE|FRESH_RESTORE_THEN_SEPARATE_EXPLICIT_CLEANUP|PASS|SYNTHETIC_DISPOSABLE_NOT_NATURAL_QUALITY");
    }
}
