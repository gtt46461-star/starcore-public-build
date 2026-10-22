package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * R926 same-user_version migration owner (class name retained for binary/source compatibility).
 *
 * <p>Safety contract: checkpoint WAL, capture pre-migration historical digests, close the helper,
 * copy+fsync the database, verify the backup as SQLite, then apply only Critical additive steps using
 * the R926 journal. Each retry resumes from the first incomplete step. On failure, restoration is
 * allowed only from a hash- and quick_check-verified backup; user data is never wiped.</p>
 */
public final class SchemaUpgradeCoordinatorR815 {
    private static final Object LOCK = new Object();
    private static final int BUFFER_SIZE = 32768;
    private static volatile String lastRestoreError = "";

    public static final class Result {
        public final boolean success;
        public final String status;
        public final File backup;
        public final String backupSha256;
        public final boolean checkpointed;
        public final boolean quickCheckPassed;
        public final boolean restored;
        public final String detail;
        public final String beforeSnapshotHash;
        public final String afterSnapshotHash;

        Result(boolean success,String status,File backup,String backupSha256,boolean checkpointed,
               boolean quickCheckPassed,boolean restored,String detail,String beforeHash,String afterHash) {
            this.success=success; this.status=safe(status); this.backup=backup; this.backupSha256=safe(backupSha256);
            this.checkpointed=checkpointed; this.quickCheckPassed=quickCheckPassed; this.restored=restored;
            this.detail=bounded(detail,900); this.beforeSnapshotHash=safe(beforeHash); this.afterSnapshotHash=safe(afterHash);
        }

        public String summary() {
            return status + "|backup=" + (backup==null?"":backup.getName()) + "|hash=" + backupSha256
                    + "|checkpoint=" + (checkpointed?1:0) + "|quick_check=" + (quickCheckPassed?1:0)
                    + "|restored=" + (restored?1:0) + "|before=" + beforeSnapshotHash + "|after=" + afterSnapshotHash
                    + "|detail=" + detail;
        }
    }

    private SchemaUpgradeCoordinatorR815() {}

    public static Result prepareAndVerify(Context context, DbHelper helper) {
        if (context == null || helper == null)
            return new Result(false,"SAFE_DB_RECOVERY",null,"",false,false,false,"ARGUMENT_NULL","","");
        synchronized (LOCK) {
            SQLiteDatabase db=null;
            File backup=null;
            String backupHash="";
            boolean checkpointed=false;
            DatabaseBackupManifestR926 before=null;
            try {
                db=helper.getWritableDatabase();
                if (db==null || !db.isOpen()) throw new IllegalStateException("DATABASE_NOT_OPEN");
                SchemaDomainCoordinatorR926.ensureInfrastructure(db);
                checkpoint(db);
                checkpointed=true;
                File database=context.getDatabasePath(AppConfig.DB_NAME);
                if (database==null || !database.isFile() || database.length()<=0L)
                    throw new IllegalStateException("DATABASE_FILE_MISSING");
                before=DatabaseBackupManifestR926.capture(db,database);
                // No live writer may remain while the filesystem copy is made.
                helper.close();
                backup=new File(database.getParentFile(),database.getName()+".r926."+ReleaseManifestR926.VERSION_CODE+".bak");
                copyAndSync(database,backup);
                backupHash=DatabaseBackupManifestR926.sha256File(backup);
                if (before.databaseSha256.length()==64 && !backupHash.equals(before.databaseSha256))
                    throw new IllegalStateException("BACKUP_SOURCE_HASH_MISMATCH");
                verifyBackupFile(backup,before);
                before.writeNextTo(backup);

                db=helper.getWritableDatabase();
                final SQLiteDatabase migrationDb=db;
                final SchemaDomainCoordinatorR926.Contract critical=SchemaDomainCoordinatorR926.criticalContract();
                SchemaDomainCoordinatorR926.ensureInfrastructure(db);
                SchemaDomainCoordinatorR926.markState(db,critical,"MIGRATING","R815_CRITICAL","");
                SchemaDomainCoordinatorR926.runStep(db,critical,"R815_CRITICAL_SCHEMA",
                        new SchemaDomainCoordinatorR926.Step(){public void run(SQLiteDatabase x){DbHelper.ensureR815CriticalSchema(x);}});
                SchemaDomainCoordinatorR926.runStep(db,critical,"R906_CRITICAL_SCHEMA",
                        new SchemaDomainCoordinatorR926.Step(){public void run(SQLiteDatabase x){DbHelper.ensureR906CriticalSchema(x);}});
                SchemaDomainCoordinatorR926.runStep(db,critical,"R925_CRITICAL_SCHEMA",
                        new SchemaDomainCoordinatorR926.Step(){public void run(SQLiteDatabase x){SchemaRevisionR925.ensureCriticalTables(x);}});
                SchemaDomainCoordinatorR926.runStep(db,critical,"R926_EVENT_LEDGER_SCHEMA",
                        new SchemaDomainCoordinatorR926.Step(){public void run(SQLiteDatabase x){RuntimeEventLedgerR926.ensureSchema(x);}});
                if (!quickCheck(db)) throw new IllegalStateException("QUICK_CHECK_FAILED_AFTER_MIGRATION");
                DatabaseBackupManifestR926.Verification verification=before.verifyHistoricalRows(db);
                if (!verification.passed)
                    throw new IllegalStateException("HISTORICAL_ROW_MUTATION:"+verification.summary());
                DatabaseBackupManifestR926 after=DatabaseBackupManifestR926.capture(db,database);
                return new Result(true,"READY",backup,backupHash,true,true,false,
                        "R926_SAME_VERSION_JOURNAL|HISTORY="+verification.summary(),before.snapshotHash,after.snapshotHash);
            } catch (Throwable failure) {
                boolean restored=restoreVerifiedBackup(context,helper,backup,backupHash);
                String restoreDetail = restored ? "" : "|RESTORE_ERROR=" + safe(lastRestoreError);
                return new Result(false,"SAFE_DB_RECOVERY",backup,backupHash,checkpointed,false,restored,
                        failure.getClass().getSimpleName()+':'+safe(failure.getMessage())+restoreDetail,
                        before==null?"":before.snapshotHash,"");
            }
        }
    }

    public static boolean restoreVerifiedBackup(Context context, DbHelper helper, File backup, String expectedSha256) {
        lastRestoreError = "";
        if (context==null || helper==null || backup==null || !backup.isFile() || safe(expectedSha256).length()!=64) {
            lastRestoreError = "RESTORE_ARGUMENT_OR_BACKUP_INVALID";
            return false;
        }
        SQLiteDatabase check=null;
        try {
            if (!DatabaseBackupManifestR926.sha256File(backup).equals(expectedSha256)) return false;
            check=SQLiteDatabase.openDatabase(backup.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            if (!quickCheck(check)) return false;
            check.close(); check=null;
            File destination=context.getDatabasePath(AppConfig.DB_NAME);
            if (destination==null || destination.getParentFile()==null) return false;
            helper.close();
            File restoreTemp=new File(destination.getParentFile(),destination.getName()+".r926.restore");
            copyAndSync(backup,restoreTemp);
            if (!DatabaseBackupManifestR926.sha256File(restoreTemp).equals(expectedSha256)) return false;
            deleteSqliteSidecars(destination);
            if (destination.exists() && !destination.delete()) throw new IOException("DESTINATION_DELETE_FAILED");
            if (!restoreTemp.renameTo(destination)) {
                copyReplace(restoreTemp,destination);
                if (!restoreTemp.delete()) throw new IOException("RESTORE_TEMP_DELETE_FAILED");
            }
            SQLiteDatabase reopened=helper.getWritableDatabase();
            return reopened!=null && reopened.isOpen() && quickCheck(reopened);
        } catch (Throwable failure) {
            lastRestoreError = failure.getClass().getSimpleName() + ':' + safe(failure.getMessage());
            return false;
        }
        finally { if (check!=null) try{check.close();}catch(Throwable ignored){} }
    }

    private static void verifyBackupFile(File backup, DatabaseBackupManifestR926 before) throws Exception {
        SQLiteDatabase check=null;
        try {
            check=SQLiteDatabase.openDatabase(backup.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            if (!quickCheck(check)) throw new IllegalStateException("BACKUP_QUICK_CHECK_FAILED");
            DatabaseBackupManifestR926.Verification verify=before.verifyHistoricalRows(check);
            if (!verify.passed) throw new IllegalStateException("BACKUP_HISTORY_MISMATCH:"+verify.summary());
        } finally { if (check!=null) check.close(); }
    }

    private static void checkpoint(SQLiteDatabase db) {
        Cursor cursor=null;
        try {
            cursor=db.rawQuery("PRAGMA wal_checkpoint(FULL)",null);
            if (cursor!=null && cursor.moveToFirst() && cursor.getInt(0)!=0)
                throw new IllegalStateException("WAL_CHECKPOINT_BUSY");
        } finally { if(cursor!=null)cursor.close(); }
    }

    private static boolean quickCheck(SQLiteDatabase db) {
        Cursor cursor=null;
        try { cursor=db.rawQuery("PRAGMA quick_check(1)",null); return cursor!=null&&cursor.moveToFirst()&&"ok".equalsIgnoreCase(cursor.getString(0)); }
        finally { if(cursor!=null)cursor.close(); }
    }

    private static void deleteSqliteSidecars(File database) throws IOException {
        if (database == null) return;
        File wal = new File(database.getAbsolutePath() + "-wal");
        File shm = new File(database.getAbsolutePath() + "-shm");
        File journal = new File(database.getAbsolutePath() + "-journal");
        if (wal.exists() && !wal.delete()) throw new IOException("RESTORE_WAL_DELETE_FAILED");
        if (shm.exists() && !shm.delete()) throw new IOException("RESTORE_SHM_DELETE_FAILED");
        if (journal.exists() && !journal.delete()) throw new IOException("RESTORE_JOURNAL_DELETE_FAILED");
    }

    private static void copyAndSync(File source,File destination) throws IOException {
        File parent=destination.getParentFile();
        if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IOException("DESTINATION_PARENT_CREATE_FAILED");
        File temp=new File(destination.getParentFile(),destination.getName()+".copying");
        copyReplace(source,temp);
        if(destination.exists()&&!destination.delete())throw new IOException("DESTINATION_REPLACE_DELETE_FAILED");
        if(!temp.renameTo(destination)){copyReplace(temp,destination);if(!temp.delete())throw new IOException("TEMP_DELETE_FAILED");}
    }

    private static void copyReplace(File source,File destination)throws IOException{
        FileInputStream in=null;FileOutputStream out=null;
        try{in=new FileInputStream(source);out=new FileOutputStream(destination,false);byte[] buffer=new byte[BUFFER_SIZE];int count;
            while((count=in.read(buffer))>=0)if(count>0)out.write(buffer,0,count);out.getFD().sync();}
        finally{if(in!=null)in.close();if(out!=null)out.close();}
    }
    private static String bounded(String v,int max){String c=safe(v);return c.length()<=max?c:c.substring(0,max);}
    private static String safe(String v){return v==null?"":v.trim();}
}
