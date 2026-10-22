package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;

/** Reclaims reusable pages and reports actual file bytes, separately from Java heap. */
public final class StorageReclaimR943 {
    private StorageReclaimR943(){}
    private static long scalar(SQLiteDatabase db,String pragma){try(Cursor c=db.rawQuery("PRAGMA "+pragma,null)){return c.moveToFirst()?c.getLong(0):0;}}
    public static String summary(DbStore store){
        try{SQLiteDatabase db=store.readable();File f=new File(db.getPath());
            return "PHONE_STORAGE|limitIssues=5000|databaseBytes="+f.length()+"|walBytes="+new File(f+"-wal").length()
                +"|pageSize="+scalar(db,"page_size")+"|pageCount="+scalar(db,"page_count")+"|freePages="+scalar(db,"freelist_count")
                +"|autoVacuum="+scalar(db,"auto_vacuum")+"|cleanup="+store.getState("r900_retention_truth","")+"|reclaim="+store.getState("r943_storage_reclaim","");
        }catch(Exception e){return "PHONE_STORAGE_READ_ERROR:"+e.getClass().getSimpleName()+":"+e.getMessage();}
    }
    public static void quiet(final DbStore store){
        if(store==null||RealtimePressureGateR811.hasActiveCriticalWork())return;
        long last=0;try{last=Long.parseLong(store.getState("r943_storage_reclaim_at","0"));}catch(NumberFormatException ignored){}
        if(System.currentTimeMillis()-last<120000)return;
        String status=store.coordinatorR36().runWrite("R943_RESEARCH_STORAGE_RECLAIM",new DatabaseCoordinator.WriteCallable<String>(){
            public String call(SQLiteDatabase db){
                if(db.inTransaction()||RealtimePressureGateR811.hasActiveCriticalWork())return "YIELD_ACTIVE";
                File f=new File(db.getPath());long before=f.length(),free=scalar(db,"freelist_count"),pages=scalar(db,"page_count"),mode=scalar(db,"auto_vacuum");
                try {
                    if(mode==2&&free>0) {
                        // Android execSQL rejects this row-producing PRAGMA. Step the cursor
                        // to completion so every requested page can be reclaimed. Yield between
                        // small calls; storage maintenance must not monopolize the writer.
                        long deadline=TimeSourceR811.monoNowMs()+40L;
                        for(int i=0;i<8&&!RealtimePressureGateR811.hasActiveCriticalWork()
                                &&TimeSourceR811.monoNowMs()<deadline;i++)reclaimPage(db,16);
                    } else if(mode==0&&free>1024&&free*2>pages) {
                        return "LEGACY_COMPACTION_REQUIRED|reusablePages="+free
                            +"|reason=LEGACY_SCOPE_EXPLICIT_VERIFIED_COPY_REBUILD_REQUIRED";
                    }
                    try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(PASSIVE)",null)){
                        if(!c.moveToFirst())return "WAL_STATUS_MISSING_RETRY";
                        int busy=c.getInt(0),frames=c.getInt(1),checkpointed=c.getInt(2);
                        // PASSIVE can return busy=0 while a reader pins uncheckpointed
                        // frames. Reusable pages may already be zero although the main
                        // file cannot shrink until that reader releases its snapshot.
                        String checkpoint="|busy="+busy+"|walFrames="+frames+"|checkpointedFrames="+checkpointed
                                +"|freePages="+scalar(db,"freelist_count");
                        if(busy!=0)return "WAL_BUSY_RETRY"+checkpoint;
                        if(frames>=0&&checkpointed<frames)return "WAL_PENDING_RETRY"+checkpoint;
                    }
                    long remaining=scalar(db,"freelist_count");
                    // A bounded slice is not a completed reclamation. With a large
                    // freelist, marking every 128-page slice complete imposed a
                    // two-minute cooldown after at most 512 KiB. Keep the same
                    // 40 ms / P0 budget, but let the existing owner resume next
                    // quiet turn until the actual reusable-page backlog is empty.
                    String outcome=mode==2&&remaining>0?"RECLAIM_PROGRESS":"RECLAIM_COMPLETE";
                    return outcome+"|beforeBytes="+before+"|afterBytes="+f.length()+"|freePages="+remaining;
                }catch(Exception e){return "RECLAIM_ERROR:"+e.getClass().getSimpleName()+":"+e.getMessage();}
            }
        },"RECLAIM_DEFERRED");
        store.setState("r943_storage_reclaim",status);
        // A partial/yielded/contended attempt is not completed maintenance. Retry next quiet slice.
        if(status.startsWith("RECLAIM_COMPLETE")||status.startsWith("LEGACY_COMPACTION_REQUIRED"))
            store.setState("r943_storage_reclaim_at",String.valueOf(System.currentTimeMillis()));
    }

    static void reclaimPage(SQLiteDatabase db,int pages){
        if(pages<1||pages>256)throw new IllegalArgumentException("RECLAIM_PAGE_LIMIT");
        try(Cursor cursor=db.rawQuery("PRAGMA incremental_vacuum("+pages+")",null)){
            while(cursor.moveToNext()){ /* Advancing executes the incremental SQLite operation. */ }
        }
    }

public static String summarySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store){
        try{SQLiteDatabase db=store.readable();File f=new File(db.getPath());
            return "PHONE_STORAGE|limitIssues=5000|measurementScope=IMMUTABLE_REPORT_SNAPSHOT"
                +"|databaseBytes="+f.length()+"|walBytes="+new File(f+"-wal").length()
                +"|snapshotSha256="+report.snapshotSha256()+"|snapshotAt="+report.nowMs()
                +liveStorageObservationSnapshotCP4(report)
                +maintenanceObservationSnapshotR82(report)
                +"|pageSize="+scalarSnapshotCP4(report, db,"page_size")+"|pageCount="+scalarSnapshotCP4(report, db,"page_count")+"|freePages="+scalarSnapshotCP4(report, db,"freelist_count")
                +"|autoVacuum="+scalarSnapshotCP4(report, db,"auto_vacuum")+"|cleanup="+store.getState("r900_retention_truth","")+"|reclaim="+store.getState("r943_storage_reclaim","");
        }catch(Exception e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
    }

    /** Runtime file sizes are observations from before backup, not snapshot properties.
     * Old durable requests may lack them; never substitute the snapshot's zero-WAL copy. */
    static String liveStorageObservationSnapshotCP4(ReportReadContextCP4 report){
        try {
            if(!report.observationBoolean("liveDatabaseFilePresent"))
                return "|liveStorageScope=OBSERVED_SOURCE_MISSING|liveDatabaseBytes=NOT_AVAILABLE|liveWalBytes=NOT_AVAILABLE";
            long at=report.observationLong("storageSampleWallMs"),database=report.observationLong("liveDatabaseFileBytes"),wal=report.observationLong("liveWalFileBytes");
            boolean walPresent=report.observationBoolean("liveWalFilePresent");
            if(at<=0||database<0||wal<0||(!walPresent&&wal!=0))
                return "|liveStorageScope=INVALID_OBSERVATION|liveDatabaseBytes=NOT_AVAILABLE|liveWalBytes=NOT_AVAILABLE";
            return "|liveStorageScope=PRE_BACKUP_NONATOMIC_FILE_OBSERVATION|liveStorageSampleAt="+at
                +"|liveDatabaseBytes="+database+"|liveWalBytes="+wal+"|liveWalFilePresent="+walPresent;
        }catch(ReportReadContextCP4.ReportReadFailureCP4 unavailable){
            return "|liveStorageScope=NOT_SAMPLED|liveDatabaseBytes=NOT_AVAILABLE|liveWalBytes=NOT_AVAILABLE";
        }
    }

    static String maintenanceObservationSnapshotR82(ReportReadContextCP4 report) {
        try { return "|maintenanceScope=PRE_BACKUP_FILE_RECEIPT|maintenance="+report.observationString("storageMaintenanceSummary"); }
        catch(ReportReadContextCP4.ReportReadFailureCP4 unavailable) { return "|maintenanceScope=NOT_SAMPLED|maintenance=NOT_SAMPLED"; }
    }

public static long scalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String pragma){try(Cursor c=report.select("PRAGMA "+pragma,null)){return c.moveToFirst()?c.getLong(0):0;}}
}
