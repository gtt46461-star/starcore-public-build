package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.StatFs;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Independent single writer for the staged research baseline. No realtime database is ATTACHed to this writer.
 * A successful baseline copy does not change active readers/owners. The file is retained for resume and later
 * catch-up/cutover; old application data is never deleted here. FULL synchronous protects the migration manifest. */
public final class ResearchDatabaseR29 implements AutoCloseable {
    public static final String FILE_NAME="starcore-research-r29-stage.db";
    private static final long MAX_BYTES=128L<<20, RESERVE_BYTES=64L<<20;
    private static final Map<String,ResearchDatabaseR29> OPEN=new HashMap<>();
    private final SQLiteDatabase destination;private final File file;private int references;
    private List<MigrationCopyR29.Table> tables;private boolean prepared,closed;

    public static final class StorageBlocked extends IllegalStateException {
        StorageBlocked(String reason){super(reason);}
    }
    public static synchronized ResearchDatabaseR29 acquire(DbStore store) {
        File file=store.contextForR26().getDatabasePath(FILE_NAME);String key=file.getAbsolutePath();
        ResearchDatabaseR29 db=OPEN.get(key);
        if(db==null){db=new ResearchDatabaseR29(file);OPEN.put(key,db);}
        db.references++;return db;
    }
    private ResearchDatabaseR29(File file) {
        this.file=file;File parent=file.getParentFile();
        if(!parent.isDirectory()&&!parent.mkdirs())throw new IllegalStateException("R29_RESEARCH_DIRECTORY_UNAVAILABLE");
        storage();
        SQLiteDatabase opened=SQLiteDatabase.openOrCreateDatabase(file,null);
        try {
            if(!opened.enableWriteAheadLogging())throw new IllegalStateException("R29_RESEARCH_WAL_REQUIRED");
            opened.execSQL("PRAGMA synchronous=FULL");opened.setMaximumSize(MAX_BYTES);
            this.destination=opened;
            write(db->{for(String ddl:MigrationCopyR29.DESTINATION_DDL)db.execSQL(ddl);});
        }catch(Exception failure){opened.close();throw new IllegalStateException("R29_RESEARCH_STAGE_OPEN",failure);}
    }
    private void storage(){
        long available=new StatFs(file.getParentFile().getAbsolutePath()).getAvailableBytes();
        if(available<RESERVE_BYTES)throw new StorageBlocked("R29_MIGRATION_RESERVE_64M_REQUIRED");
    }
    /** All destination transactions use this monitor/file, not DatabaseCoordinator's realtime queue. */
    private synchronized void write(MigrationCopyR29.Body body)throws Exception {
        if(closed)throw new IllegalStateException("R29_RESEARCH_STAGE_CLOSED");
        destination.beginTransactionNonExclusive();
        try{body.run(destination);destination.setTransactionSuccessful();}finally{destination.endTransaction();}
    }
    private void prepare(DbStore store,SQLiteDatabase realtime)throws Exception {
        if(prepared)return;
        tables=MigrationCopyR29.tables(realtime);
        // Register all work immediately. Root tracker DDL is admitted only for the table selected
        // for copying, so hundreds of unrelated Root transactions cannot prevent the first baseline.
        MigrationCopyR29.prepare(tables,this::write);
        prepared=true;
    }
    public synchronized MigrationCopyR29.Result step(DbStore store,SQLiteDatabase realtime)throws Exception {
        storage();prepare(store,realtime);
        long pages=MigrationCopyR29.scalar(destination,"PRAGMA page_count",null),size=MigrationCopyR29.scalar(destination,"PRAGMA page_size",null);
        if(pages*size>MAX_BYTES-(8L<<20))throw new StorageBlocked("R29_MIGRATION_STAGE_128M_BUDGET_REACHED");
        return MigrationCopyR29.step(realtime,destination,tables,
                body->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_MIGRATION_TRACK",db->{body.run(db);return 0;}),this::write,System.currentTimeMillis());
    }
    public synchronized long verifiedTables(){return MigrationCopyR29.scalar(destination,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'",null);}
    public synchronized int tableCount(){return tables==null?0:tables.size();}
    public synchronized Map<String,Object> diagnostics(SQLiteDatabase realtime){
        Map<String,Object> m=MigrationCopyR29.diagnostics(realtime,destination);
        m.put("file",FILE_NAME);m.put("byteBudget",MAX_BYTES);m.put("freeSpaceReserve",RESERVE_BYTES);
        m.put("writer","INDEPENDENT_STAGE_FILE_FULL_SYNC_NO_REALTIME_ATTACH");
        m.put("verifiedBaselineTables",verifiedTables());
        try(Cursor c=destination.rawQuery("PRAGMA synchronous",null)){if(c.moveToFirst())m.put("synchronous",c.getInt(0));}
        return m;
    }
    @Override public void close(){
        synchronized(ResearchDatabaseR29.class){
            if(references<=0)return;
            if(--references==0){synchronized(this){closed=true;destination.close();}OPEN.remove(file.getAbsolutePath());}
        }
    }
}
