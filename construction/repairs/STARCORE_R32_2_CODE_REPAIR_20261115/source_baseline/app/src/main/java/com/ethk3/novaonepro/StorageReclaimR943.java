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
    private static final java.util.concurrent.atomic.AtomicBoolean RUNNING_R31=new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.Map<String,StorageCheckpointR31> POLICIES_R31=new java.util.LinkedHashMap<>();
    /** Kernel fsync/truncate duration has no proven phone bound yet. Keep the
     * controlled native path available to acceptance, disabled in normal use. */
    public static final boolean ONLINE_TRUNCATE_VALIDATED_R31=false;
    private static synchronized StorageCheckpointR31 policy(File database){String key=database.getAbsolutePath();StorageCheckpointR31 p=POLICIES_R31.get(key);if(p==null){if(POLICIES_R31.size()>=4)throw new IllegalStateException("WAL_POLICY_CAPACITY");p=new StorageCheckpointR31();POLICIES_R31.put(key,p);}return p;}
    public static void quiet(final DbStore store){maintainR31(store,true);}
    public static StorageHealthR811.WalCheckpoint checkpointOnlyR31(DbStore store){return maintainR31(store,false);}
    private static StorageHealthR811.WalCheckpoint maintainR31(DbStore store,boolean reclaim){
        if(store==null)return StorageHealthR811.WalCheckpoint.blocked("STORE_NULL");
        if(android.os.Looper.myLooper()!=null&&android.os.Looper.myLooper()==android.os.Looper.getMainLooper())return StorageHealthR811.WalCheckpoint.blocked("YIELD_MAIN_THREAD");
        if(!RUNNING_R31.compareAndSet(false,true))return StorageHealthR811.WalCheckpoint.blocked("WAL_OWNER_BUSY");
        final long began=TimeSourceR811.monoNowMs(),wall=System.currentTimeMillis();java.util.Map<String,Object> evidence=new java.util.LinkedHashMap<>();
        try{
            SQLiteDatabase db=store.readable();File file=new File(db.getPath());long next=0;try{next=Long.parseLong(store.getState("next_open_time","0"));}catch(NumberFormatException invalid){evidence.put("nextDeadlineState","UNKNOWN");}
            OnlineR31 online=new OnlineR31(store,file,began+40L,next,evidence,ONLINE_TRUNCATE_VALIDATED_R31);
            StorageCheckpointR31 policy=policy(file);String status=policy.round(online,began);
            evidence.put("state",status);evidence.put("policy",policy.json());evidence.put("onlineTruncateEnabled",ONLINE_TRUNCATE_VALIDATED_R31);evidence.put("onlineTruncateLimit","NOT_VALIDATED_DEVICE_KERNEL_IO_BOUND;DEFAULT_DISABLED");
            if(reclaim&&online.sliceRemainingMs()>0&&!online.inRealtimeWindow(TimeSourceR811.monoNowMs())&&!Boolean.TRUE.equals(StorageCheckpointClientR31.snapshot().get("inFlight"))){
                long mode=scalar(db,"auto_vacuum"),free=scalar(db,"freelist_count");evidence.put("autoVacuum",mode);evidence.put("freePagesBefore",free);
                if(mode==2&&free>0&&online.sliceRemainingMs()>=5)online.incremental();
                else if(mode!=2)evidence.put("legacyCompaction","OFFLINE_REBUILD_REQUIRED;EXPLICIT_USER_MAINTENANCE_ONLY");
                evidence.put("freePagesAfter",scalar(db,"freelist_count"));
            }
            evidence.put("observedMainBytes",file.length());evidence.put("observedWalBytes",new File(file.getPath()+"-wal").length());
            StorageCheckpointR31.Result actual=online.lastPassive;
            return actual==null?StorageHealthR811.WalCheckpoint.blocked(status):new StorageHealthR811.WalCheckpoint(actual.busy==0&&actual.log>=0&&actual.checkpointed==actual.log,actual.busy,actual.log,actual.checkpointed,status);
        }catch(RuntimeException|java.io.IOException failure){
            if(failure instanceof android.os.OperationCanceledException||failure instanceof java.util.concurrent.CancellationException||Thread.currentThread().isInterrupted())evidence.put("state","CANCELLED_MAINTENANCE_SLICE");else{evidence.put("state","MAINTENANCE_FAILED_OR_INCOMPLETE");evidence.put("failureClass",failure.getClass().getSimpleName());}
            return StorageHealthR811.WalCheckpoint.blocked(String.valueOf(evidence.get("state")));
        }finally{
            evidence.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);evidence.put("observedAt",wall);evidence.put("elapsedMs",Math.max(0,TimeSourceR811.monoNowMs()-began));evidence.put("sliceBudgetMs",40);evidence.put("budgetOverrunObserved",TimeSourceR811.monoNowMs()-began>40);evidence.put("offlineServiceInvoked",false);evidence.put("busyIsCorruption",false);
            try{store.setState("r31_storage_checkpoint",com.ethk3.novaonepro.strategyv2.JsonR27.write(evidence));store.setState("r943_storage_reclaim",String.valueOf(evidence.get("state")));}finally{RUNNING_R31.set(false);}
        }
    }
    static final class OnlineR31 implements StorageCheckpointR31.Database {
        final DbStore store;final File file;final long deadline,nextOpen;final java.util.Map<String,Object> evidence;final boolean allowTruncate;StorageCheckpointR31.Result lastPassive;
        OnlineR31(DbStore store,File file,long deadline,long nextOpen,java.util.Map<String,Object> evidence,boolean allowTruncate){this.store=store;this.file=file;this.deadline=deadline;this.nextOpen=nextOpen;this.evidence=evidence;this.allowTruncate=allowTruncate;}
        public boolean inRealtimeWindow(long now){long wall=System.currentTimeMillis();return RealtimePressureGateR811.isUnderPressure()||SettlementExecutionGateR819.isActive()||(nextOpen>wall&&nextOpen-wall<com.ethk3.novaonepro.strategyv2.ResearchWriteQueueR27.DEADLINE_GUARD_MS);}
        public long sliceRemainingMs(){return Math.max(0,deadline-TimeSourceR811.monoNowMs());}
        boolean continuing(){return !Thread.currentThread().isInterrupted()&&sliceRemainingMs()>0&&!inRealtimeWindow(TimeSourceR811.monoNowMs());}
        public boolean cancelStaleReaders(){try{boolean drained=SourceReadLeaseR82.requestStaleReaders(file,60000L);evidence.put("knownReaderOwnersActuallyClosed",drained);return drained;}catch(java.io.IOException invalid){evidence.put("readerDrain","UNKNOWN_SOURCE_LEASE");return false;}}
        public StorageCheckpointR31.Result checkpoint(String mode){
            StorageCheckpointR31.Result result;
            try{
                if(!continuing())result=new StorageCheckpointR31.Result(1,-1,-1);
                else if(mode.equals("PASSIVE")){
                    long[] actual=StorageCheckpointClientR31.passive(store.contextForR26(),file,deadline);evidence.put("diagnosticOwner",StorageCheckpointClientR31.snapshot());result=actual==null?new StorageCheckpointR31.Result(1,-1,-1):decode(0,actual);
                }else if(!allowTruncate){evidence.put("truncateState","DISABLED_PENDING_DEVICE_IO_BOUND_VALIDATION");result=new StorageCheckpointR31.Result(1,-1,-1);}
                else result=write(1);
            }catch(java.io.IOException unavailable){evidence.put("nativeCheckpointState","SOURCE_LEASE_OR_NATIVE_UNAVAILABLE");result=new StorageCheckpointR31.Result(1,-1,-1);}
            if(mode.equals("PASSIVE"))lastPassive=result;return result;
        }
        void incremental()throws java.io.IOException {
            if(!continuing())return;
            try(DatabaseCoordinator.WriterDeadlineScopeCP3 scope=DatabaseCoordinator.withWriterDeadlineCP3(deadline)){
                store.coordinatorR36().runWrite(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,"R31_INCREMENTAL_16_PAGE_SLICE"),db->{
                    if(db.inTransaction()||!continuing())return 0;
                    long began=TimeSourceR811.monoNowMs();reclaimPage(db,16);evidence.put("incrementalElapsedMs",TimeSourceR811.monoNowMs()-began);evidence.put("incrementalMaximumPages",16);evidence.put("incrementalIoBoundDeviceVerified",false);return 1;
                },0);
            }
        }
        private StorageCheckpointR31.Result write(int mode){evidence.put("truncateState","DISABLED_PENDING_DEVICE_IO_BOUND_VALIDATION");return new StorageCheckpointR31.Result(1,-1,-1);}
        private StorageCheckpointR31.Result decode(int mode,long[] result){
            java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();row.put("mode","PASSIVE");row.put("state",StorageCheckpointServiceR31.resultStateR31(result));row.put("nativeCode",result[0]);row.put("busy",result[1]);row.put("logFrames",result[2]);row.put("checkpointedFrames",result[3]);row.put("nativeElapsedMs",result[4]);row.put("copyBytesEstimate",result[5]);row.put("nativeSqliteVersion",result[6]);row.put("operationAttempted",result[7]==1);row.put("budgetOverrunObserved",result[4]>40);evidence.put("actualPassive",row);
            int busy=(int)result[1];if((result[0]!=0||result[7]!=1)&&busy==0)busy=1;return new StorageCheckpointR31.Result(busy,(int)result[2],(int)result[3]);
        }
    }
    /** Final-report consumer reads only the last durable observation. It neither
     * starts GC nor substitutes an empty snapshot WAL for the actual source. */
    public static java.util.Map<String,Object> checkpointReportR31(SQLiteDatabase db){
        java.util.Map<String,Object> missing=new java.util.LinkedHashMap<>();missing.put("state","NOT_OBSERVED");missing.put("offlineServiceInvoked",false);
        try(Cursor c=db.rawQuery("SELECT state_value,updated_at FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='r31_storage_checkpoint' AND length(CAST(state_value AS BLOB))<=32768",null)){
            if(!c.moveToFirst())return missing;Object value=com.ethk3.novaonepro.strategyv2.JsonR27.parse(c.getString(0));if(!(value instanceof java.util.Map))throw new IllegalArgumentException("CHECKPOINT_OBSERVATION_OBJECT");
            @SuppressWarnings("unchecked") java.util.Map<String,Object> observed=new java.util.LinkedHashMap<>((java.util.Map<String,Object>)value);observed.put("receiptUpdatedAt",c.getLong(1));observed.put("observationScope","LAST_COMMITTED_RUNTIME_OBSERVATION_NOT_CURRENT_HEALTH");return observed;
        }catch(RuntimeException invalid){if(invalid instanceof android.os.OperationCanceledException||invalid instanceof java.util.concurrent.CancellationException)throw invalid;missing.put("state","UNKNOWN_OBSERVATION_UNAVAILABLE");return missing;}
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
