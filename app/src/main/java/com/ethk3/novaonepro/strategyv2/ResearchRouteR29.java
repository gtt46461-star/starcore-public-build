package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AppConfig;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** Active research routing boundary. Publication requires a complete verified migration and a realtime route
 * receipt. A missing/corrupt published file fails closed; it never silently resumes the old budget/model state.
 * The independent writer has no ATTACH and no DatabaseCoordinator queue. Combined readers are read-only. */
public final class ResearchRouteR29 {
    private ResearchRouteR29() {}
    public static final String FILE_NAME="starcore-r29-research.db";
    public static final String ROUTE_DDL="CREATE TABLE IF NOT EXISTS r29_database_route(singleton INTEGER PRIMARY KEY CHECK(singleton=1),phase TEXT NOT NULL,database_name TEXT NOT NULL,schema_hash TEXT NOT NULL,source_root TEXT NOT NULL,epoch INTEGER NOT NULL,activated_at INTEGER NOT NULL)";
    public static final String OWNER_DDL="CREATE TABLE IF NOT EXISTS r29_research_owner(singleton INTEGER PRIMARY KEY CHECK(singleton=1),phase TEXT NOT NULL,schema_hash TEXT NOT NULL,source_root TEXT NOT NULL,epoch INTEGER NOT NULL,prepared_at INTEGER NOT NULL)";
    private static final Map<String,Owner> OWNERS=new HashMap<>();
    private static final Map<String,java.util.concurrent.locks.ReentrantReadWriteLock> BARRIERS=new HashMap<>();
    private static java.util.concurrent.locks.ReentrantReadWriteLock barrier(String path){synchronized(BARRIERS){java.util.concurrent.locks.ReentrantReadWriteLock lock=BARRIERS.get(path);if(lock==null){lock=new java.util.concurrent.locks.ReentrantReadWriteLock(true);BARRIERS.put(path,lock);}return lock;}}

    public static void ensureRealtime(SQLiteDatabase realtime){realtime.execSQL(ROUTE_DDL);}
    public static boolean active(SQLiteDatabase realtime){
        try(Cursor c=realtime.rawQuery("SELECT phase,database_name FROM r29_database_route WHERE singleton=1",null)){
            if(!c.moveToFirst())return false;
            if(!"ACTIVE".equals(c.getString(0))||!FILE_NAME.equals(c.getString(1)))throw new IllegalStateException("R29_INVALID_PUBLISHED_ROUTE_RECOVERY_REQUIRED");
            return true;
        }catch(android.database.sqlite.SQLiteException missing){
            // Missing table means an old pre-migration database; other errors must not masquerade as old routing.
            if(String.valueOf(missing.getMessage()).contains("no such table"))return false;throw missing;
        }
    }
    private static String realtimePath(Context context){return context.getDatabasePath(AppConfig.DB_NAME).getAbsolutePath();}
    private static Owner owner(SQLiteDatabase realtime){
        if(!active(realtime))return null;
        String path=realtime.getPath();
        synchronized(OWNERS){Owner owner=OWNERS.get(path);if(owner==null){owner=new Owner(new File(path),realtime);OWNERS.put(path,owner);}return owner;}
    }
    public static <T>T write(DbStore store,String operation,DatabaseCoordinator.WriteCallable<T> action){
        java.util.concurrent.locks.Lock gate=barrier(realtimePath(store.contextForR26())).readLock();boolean held=false;
        try{
            held=gate.tryLock(200,TimeUnit.MILLISECONDS);if(!held)throw new IllegalStateException("CP3_DB_BUSY_RESEARCH_HANDOFF");
            // Resolve AFTER acquiring the gate: a CSV import may have waited across the ownership change.
            Owner owner=owner(store.readable());
            if(owner==null)return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(operation,action);
            return owner.write(operation,action);
        }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("CP3_DB_BUSY_RESEARCH_HANDOFF_INTERRUPTED",interrupted);}
        finally{if(held)gate.unlock();}
    }
    public static final class Handoff implements AutoCloseable{private final java.util.concurrent.locks.Lock gate;private Handoff(java.util.concurrent.locks.Lock gate){this.gate=gate;}public void close(){gate.unlock();}}
    public static Handoff handoff(DbStore store)throws InterruptedException{
        java.util.concurrent.locks.Lock gate=barrier(realtimePath(store.contextForR26())).writeLock();
        if(!gate.tryLock(200,TimeUnit.MILLISECONDS))return null;return new Handoff(gate);
    }
    /** A worker owns the returned connection and closes it after its task threads join. */
    public static SQLiteDatabase openWorkerReader(Context context){
        String source=realtimePath(context);SQLiteDatabase realtime=SQLiteDatabase.openDatabase(source,null,SQLiteDatabase.OPEN_READONLY);
        try{
            Owner owner=owner(realtime);
            if(owner==null){queryOnly(realtime);return realtime;}
            SQLiteDatabase research=owner.reader();realtime.close();return research;
        }catch(RuntimeException failure){realtime.close();throw failure;}
    }
    /** Shared read-only UI/report connection. Snapshot exports must bind the research snapshot separately. */
    public static boolean isResearchDatabase(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name='r29_research_owner'",null)){return c.moveToFirst();}}
    public static SQLiteDatabase reader(SQLiteDatabase original){
        if(isResearchDatabase(original))return original;
        if(active(original)&&!new File(original.getPath()).getName().equals(AppConfig.DB_NAME))throw new IllegalStateException("R29_REPORT_RESEARCH_SNAPSHOT_NOT_BOUND");
        Owner owner=owner(original);return owner==null?original:owner.sharedReader();
    }
    static SQLiteDatabase realtimeForWriter(SQLiteDatabase database){
        synchronized(OWNERS){for(Owner owner:OWNERS.values())if(owner.writer==database)return owner.realtimeReader;}
        return database;
    }
    public static void publish(DbStore store,File prepared,String schemaHash,String sourceRoot,long epoch){
        File expected=store.contextForR26().getDatabasePath(FILE_NAME);
        if(!prepared.getAbsolutePath().equals(expected.getAbsolutePath())||!prepared.isFile())throw new IllegalStateException("R29_PREPARED_RESEARCH_FILE_REQUIRED");
        try(SQLiteDatabase probe=SQLiteDatabase.openDatabase(prepared.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            verifyOwner(probe,schemaHash,sourceRoot,epoch);
            try(Cursor c=probe.rawQuery("PRAGMA quick_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0))||c.moveToNext())throw new IllegalStateException("R29_RESEARCH_INTEGRITY_FAILED");}
        }
        // This transaction never waits for a research writer: preparation/close/fsync precede publication.
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_RESEARCH_CUTOVER",db->{
            ensureRealtime(db);
            try(Cursor c=db.rawQuery("SELECT phase,source_root,baseline_sha FROM r29_migration_publication WHERE singleton=1 AND epoch=?",new String[]{String.valueOf(epoch)})){
                if(!c.moveToFirst()||!"RENAMED".equals(c.getString(0))||!sourceRoot.equals(c.getString(1))||c.getString(2)==null||c.getString(2).length()!=64)throw new IllegalStateException("R29_CUTOVER_PREPARATION_RECEIPT_REQUIRED");
            }
            db.execSQL("INSERT OR IGNORE INTO r29_database_route(singleton,phase,database_name,schema_hash,source_root,epoch,activated_at) VALUES(1,'ACTIVE',?,?,?,?,?)",new Object[]{FILE_NAME,schemaHash,sourceRoot,epoch,System.currentTimeMillis()});
            try(Cursor c=db.rawQuery("SELECT phase,schema_hash,source_root,epoch FROM r29_database_route WHERE singleton=1",null)){
                if(!c.moveToFirst()||!"ACTIVE".equals(c.getString(0))||!schemaHash.equals(c.getString(1))||!sourceRoot.equals(c.getString(2))||epoch!=c.getLong(3))throw new IllegalStateException("R29_CUTOVER_RECEIPT_CONFLICT");
            }
            db.execSQL("UPDATE r29_migration_publication SET phase='PUBLISHED',updated_at=? WHERE singleton=1 AND epoch=?",new Object[]{System.currentTimeMillis(),epoch});
            return 0;
        });
    }
    private static void verifyOwner(SQLiteDatabase research,String schemaHash,String sourceRoot,long epoch){
        try(Cursor c=research.rawQuery("SELECT phase,schema_hash,source_root,epoch FROM r29_research_owner WHERE singleton=1",null)){
            if(!c.moveToFirst()||!"READY".equals(c.getString(0))||!schemaHash.equals(c.getString(1))||!sourceRoot.equals(c.getString(2))||epoch!=c.getLong(3))throw new IllegalStateException("R29_RESEARCH_OWNER_MISMATCH");
        }
    }
    private static void queryOnly(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA query_only=1",null)){c.moveToFirst();}}
    static SQLiteDatabase combinedReader(File file,String realtimePath){
        // Android forbids CREATE TEMP VIEW on a connection opened with OPEN_READONLY. Open solely to install
        // the two TEMP views, then enforce query_only for main AND attached databases before exposing it.
        SQLiteDatabase ro=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READWRITE);
        try{
            ro.execSQL("ATTACH DATABASE ? AS realtime",new Object[]{realtimePath});
            ro.execSQL("CREATE TEMP VIEW r26_task AS SELECT * FROM main.r26_task WHERE lane='R27' UNION ALL SELECT * FROM realtime.r26_task WHERE lane<>'R27'");
            ro.execSQL("CREATE TEMP VIEW r26_evolution AS SELECT * FROM main.r26_evolution WHERE attempt_id LIKE 'R27:%' UNION ALL SELECT * FROM realtime.r26_evolution WHERE attempt_id NOT LIKE 'R27:%'");
            queryOnly(ro);return ro;
        }catch(RuntimeException failure){ro.close();throw failure;}
    }
    static void verifyPreparedReader(File file,String realtimePath){
        try(SQLiteDatabase ro=combinedReader(file,realtimePath);Cursor c=ro.rawQuery("PRAGMA query_only",null)){
            if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R29_COMBINED_READER_NOT_QUERY_ONLY");
            try(Cursor p=ro.rawQuery("SELECT count(*) FROM r26_task",null)){p.moveToFirst();}
            try(Cursor p=ro.rawQuery("SELECT count(*) FROM draws WHERE dirty=0",null)){p.moveToFirst();}
        }
    }

    static final class Owner{
        final File file;final SQLiteDatabase writer,realtimeReader;final String realtimePath;
        final ReentrantLock lock=new ReentrantLock(true);SQLiteDatabase sharedReader;
        Owner(File realtime,SQLiteDatabase authority){
            realtimePath=realtime.getAbsolutePath();file=new File(realtime.getParentFile(),FILE_NAME);
            if(!file.isFile())throw new IllegalStateException("R29_PUBLISHED_RESEARCH_FILE_MISSING_RECOVERY_REQUIRED");
            String schema,root;long epoch;
            try(Cursor c=authority.rawQuery("SELECT schema_hash,source_root,epoch FROM r29_database_route WHERE singleton=1",null)){
                if(!c.moveToFirst())throw new IllegalStateException("R29_ROUTE_MISSING");schema=c.getString(0);root=c.getString(1);epoch=c.getLong(2);
            }
            SQLiteDatabase opened=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READWRITE);
            try{verifyOwner(opened,schema,root,epoch);if(!opened.enableWriteAheadLogging())throw new IllegalStateException("R29_RESEARCH_WAL_REQUIRED");opened.execSQL("PRAGMA synchronous=FULL");}
            catch(RuntimeException failure){opened.close();throw failure;}
            writer=opened;realtimeReader=SQLiteDatabase.openDatabase(realtimePath,null,SQLiteDatabase.OPEN_READONLY);queryOnly(realtimeReader);
        }
        <T>T write(String operation,DatabaseCoordinator.WriteCallable<T> action){
            boolean acquired=false;
            try{
                if(android.os.Looper.myLooper()!=null&&android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("R29_RESEARCH_WRITE_ON_MAIN_THREAD");
                acquired=lock.tryLock(200,TimeUnit.MILLISECONDS);
                if(!acquired)throw new IllegalStateException("CP3_DB_BUSY_RESEARCH_FILE");
                writer.execSQL("PRAGMA synchronous=FULL");writer.beginTransactionNonExclusive();
                try{
                    try(Cursor c=writer.rawQuery("PRAGMA database_list",null)){while(c.moveToNext())if(!"main".equals(c.getString(1))&&!"temp".equals(c.getString(1)))throw new IllegalStateException("R29_WRITER_MUST_NOT_ATTACH_REALTIME");}
                    T value=action.call(writer);writer.setTransactionSuccessful();return value;
                }finally{writer.endTransaction();}
            }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException("CP3_DB_BUSY_RESEARCH_INTERRUPTED",interrupted);}
            catch(RuntimeException failure){throw failure;}
            catch(Exception failure){throw new IllegalStateException("R29_RESEARCH_WRITE_FAILED:"+operation,failure);}
            finally{if(acquired)lock.unlock();}
        }
        SQLiteDatabase reader(){
            return combinedReader(file,realtimePath);
        }
        synchronized SQLiteDatabase sharedReader(){if(sharedReader==null||!sharedReader.isOpen())sharedReader=reader();return sharedReader;}
    }
}
