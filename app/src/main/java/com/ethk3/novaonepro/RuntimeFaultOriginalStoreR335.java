package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.system.Os;
import android.system.OsConstants;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/** One immutable WRITER_HOLD original per slice. No aggregate fault is retired by this owner. */
public final class RuntimeFaultOriginalStoreR335 implements RuntimeFaultArchiveB01.Worker {
    public static final String DOMAIN="RESEARCH_R335_FAULT_ORIGINAL",DIRECTORY="r335-fault-originals";
    public static final String[] OBJECTS={"r335_fault_original","r335_fault_original_order","r335_fault_original_no_update","r335_fault_original_no_delete","r335_fault_archive_work"};
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r335_fault_original(event_id TEXT PRIMARY KEY,original_hash TEXT NOT NULL UNIQUE,original_bytes INTEGER NOT NULL CHECK(original_bytes>0 AND original_bytes<=65536),codec TEXT NOT NULL,relative_path TEXT NOT NULL,owner_epoch TEXT NOT NULL,task_id TEXT NOT NULL,category TEXT NOT NULL,last_wall_ms INTEGER NOT NULL,receipt_key TEXT NOT NULL UNIQUE,release_hash TEXT NOT NULL,archived_at INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS r335_fault_original_order ON r335_fault_original(last_wall_ms,event_id)",
        "CREATE TRIGGER IF NOT EXISTS r335_fault_original_no_update BEFORE UPDATE ON r335_fault_original BEGIN SELECT RAISE(ABORT,'R335_FAULT_ORIGINAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r335_fault_original_no_delete BEFORE DELETE ON r335_fault_original BEGIN SELECT RAISE(ABORT,'R335_FAULT_ORIGINAL_PRESERVED'); END",
        "CREATE TABLE IF NOT EXISTS r335_fault_archive_work(singleton INTEGER PRIMARY KEY CHECK(singleton=1),revision INTEGER NOT NULL,after_event TEXT NOT NULL,selected_event TEXT NOT NULL,selected_hash TEXT NOT NULL,stage TEXT NOT NULL,owner_epoch TEXT NOT NULL,fence INTEGER NOT NULL,updated_at INTEGER NOT NULL)"
    };
    private static final String LOCATOR_COLUMNS="event_id,original_hash,original_bytes,codec,relative_path,owner_epoch,task_id,category,last_wall_ms,receipt_key,release_hash,archived_at";
    private static final String WORK_COLUMNS="singleton,revision,after_event,selected_event,selected_hash,stage,owner_epoch,fence,updated_at";
    private static final String SELECT_ORIGINAL=originalSelect();
    private static final long ROWS_TARGET=1024,BYTES_TARGET=2L*1024*1024;
    private static volatile String lastDetail="STARTUP_SCAN";

    public static void ensureSchema(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R335_FAULT_MIGRATION_OWNER_REQUIRED");
        for(String sql:DDL)db.execSQL(sql);
        db.execSQL("INSERT OR IGNORE INTO r335_fault_archive_work VALUES(1,0,'','','','IDLE','',0,0)");
        requireReady(db);
    }
    public static boolean ready(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE name IN ('r335_fault_original','r335_fault_original_order','r335_fault_original_no_update','r335_fault_original_no_delete','r335_fault_archive_work')",null)){
            return c.moveToFirst()&&c.getLong(0)==OBJECTS.length;
        }
    }
    private static void requireReady(SQLiteDatabase db){
        DatabaseCoordinator.requireSchemaObjectsR31(db,DOMAIN,new String[][]{
            {"r31_runtime_fault",FaultOriginalCodecR335.COLUMNS},{"r335_fault_original",LOCATOR_COLUMNS},
            {"r335_fault_archive_work",WORK_COLUMNS},{"r31_commit_receipt","commit_key,payload_hash,operation,task_id,owner_epoch,fence,input_snapshot,chunk_seq,operator_version,committed_at"}},new String[0][]);
    }
    private static String originalSelect(){
        StringJoiner out=new StringJoiner(",","SELECT "," FROM r31_runtime_fault WHERE event_id=?");
        for(String name:FaultOriginalCodecR335.NAMES){out.add("typeof("+name+")");out.add("CASE WHEN typeof("+name+")='text' THEN CAST("+name+" AS BLOB) ELSE "+name+" END");}
        return out.toString();
    }
    public static byte[] hot(SQLiteDatabase db,String event)throws IOException {
        try(Cursor c=db.rawQuery(SELECT_ORIGINAL,new String[]{event})){
            if(!c.moveToFirst())return null;
            FaultOriginalCodecR335.Cell[] cells=new FaultOriginalCodecR335.Cell[FaultOriginalCodecR335.NAMES.length];
            for(int i=0;i<cells.length;i++){
                String type=c.getString(i*2);int column=i*2+1;char tag;byte[] raw;
                switch(type){
                    case "null":tag='N';raw=new byte[0];break;
                    case "integer":tag='I';raw=ByteBuffer.allocate(8).putLong(c.getLong(column)).array();break;
                    case "real":tag='F';raw=ByteBuffer.allocate(8).putLong(Double.doubleToRawLongBits(c.getDouble(column))).array();break;
                    case "text":tag='T';raw=c.getBlob(column);break;
                    case "blob":tag='B';raw=c.getBlob(column);break;
                    default:throw new IOException("R335_FAULT_UNKNOWN_SQL_TYPE");
                }
                cells[i]=new FaultOriginalCodecR335.Cell(tag,raw);
            }
            return FaultOriginalCodecR335.encode(cells);
        }
    }
    private static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R335_FAULT_QUERY_EMPTY");return c.getLong(0);}}
    private static boolean independent(byte[] body)throws IOException {
        Map<String,Object> v=FaultOriginalCodecR335.values(body);Object event=v.get("event_id");
        return event instanceof String&&((String)event).matches("[0-9a-f]{64}")
            &&"WRITER_HOLD".equals(v.get("category"))&&"WRITER_UNLOCKED".equals(v.get("phase"))
            &&event.equals(v.get("root_fingerprint"))&&Long.valueOf(1).equals(v.get("occurrences"))
            &&Objects.equals(v.get("first_wall_ms"),v.get("last_wall_ms"))
            &&Objects.equals(v.get("first_elapsed_ms"),v.get("last_elapsed_ms"))
            &&Objects.equals(v.get("cause_chain_json"),v.get("last_cause_chain_json"))
            &&"NOT_CAPTURED_NO_BUSINESS_THROWABLE".equals(v.get("stack"))
            &&Objects.equals(v.get("stack"),v.get("last_stack"))
            &&Objects.equals(v.get("stack_hash"),v.get("last_stack_hash"));
    }
    private static final class Work {
        long revision,fence;String after,event,hash,stage,owner;
    }
    private static Work work(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT revision,after_event,selected_event,selected_hash,stage,owner_epoch,fence FROM r335_fault_archive_work WHERE singleton=1",null)){
        if(!c.moveToFirst())throw new IllegalStateException("R335_FAULT_WORK_MISSING");Work w=new Work();w.revision=c.getLong(0);w.after=c.getString(1);w.event=c.getString(2);w.hash=c.getString(3);w.stage=c.getString(4);w.owner=c.getString(5);w.fence=c.getLong(6);return w;
    }}
    private static DatabaseCoordinator.WriteContext context(String op){return DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,op).withDeadlineMonoMs(TimeSourceR811.monoNowMs()+20L);}
    private static boolean cancelled(long deadline){return Thread.currentThread().isInterrupted()||android.os.SystemClock.elapsedRealtime()>=deadline||RealtimePressureGateR811.hasActiveCriticalWork();}
    private static RuntimeFaultArchiveB01.Page retain(RuntimeFaultArchiveB01.State state,String detail){lastDetail=detail;return RuntimeFaultArchiveB01.Page.retain(state);}

    @Override public RuntimeFaultArchiveB01.Page runOne(DbStore store,long deadline){
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("R335_FAULT_ARCHIVE_MAIN_THREAD");
        if(cancelled(deadline))return retain(RuntimeFaultArchiveB01.State.YIELDED,"CANCELLED_OR_CRITICAL_PRESSURE");
        Context app=store.contextForR26();
        if(!mainProcess(app))return retain(RuntimeFaultArchiveB01.State.WAIT_READER,"MAIN_PROCESS_OWNER_REQUIRED");
        MemoryBudgetR31.Admission memory=MemoryBudgetR31.processBudget().tryAcquire("r335-fault-original",MemoryBudgetR31.Kind.CHECKPOINT,512L*1024,()->Thread.currentThread().isInterrupted());
        if(!memory.acquired())return retain(RuntimeFaultArchiveB01.State.BLOCKED_BUDGET,"MEMORY:"+memory.reason);
        try(MemoryBudgetR31.Lease heldMemory=memory.lease){
            SQLiteDatabase db=store.readable();if(db.inTransaction())throw new IllegalStateException("R335_FAULT_ARCHIVE_EXTERNAL_TRANSACTION");
            if(!ready(db))return retain(RuntimeFaultArchiveB01.State.WAIT_READER,"SCHEMA_PENDING:"+DOMAIN);
            requireReady(db);
            File files=app.getFilesDir().getCanonicalFile();
            // The snapshot/report owner holds the shared side from backup through attachment closure.
            try(ReportInputsLeaseCP12 publication=ReportInputsLeaseCP12.tryAcquireExclusive(files)){
                if(publication==null)return retain(RuntimeFaultArchiveB01.State.WAIT_READER,"REPORT_INPUT_PIN");
                Work prior=work(db);String selected=null;
                if("PREPARING".equals(prior.stage))selected=prior.event;
                if(selected==null){
                    long count=scalar(db,"SELECT count(*) FROM r31_runtime_fault"),bytes=scalar(db,"SELECT coalesce(sum(byte_count),0) FROM r31_runtime_fault");
                    if(count<=ROWS_TARGET&&bytes<=BYTES_TARGET)return retain(RuntimeFaultArchiveB01.State.NO_ELIGIBLE_VERIFIED,"HOT_CAPACITY_WITHIN_TARGET");
                    try(Cursor c=db.rawQuery("SELECT event_id FROM r31_runtime_fault WHERE category='WRITER_HOLD' AND phase='WRITER_UNLOCKED' ORDER BY CASE WHEN event_id>? THEN 0 ELSE 1 END,event_id LIMIT 1",new String[]{prior.after})){if(c.moveToFirst())selected=c.getString(0);}
                    if(selected==null)return retain(RuntimeFaultArchiveB01.State.BLOCKED_PROTECTED,"ONLY_MUTABLE_OR_UNKNOWN_FAULT_PRODUCERS_REMAIN");
                }
                final String event=selected;byte[] body=hot(db,event);
                if(body==null)return retain(RuntimeFaultArchiveB01.State.WAIT_VERIFIED,"SELECTED_HOT_ORIGINAL_MISSING:"+event);
                if(!independent(body)||WriterHoldFaultsR31.retainsHotOriginalR33(event)){
                    advanceSkipped(store,prior,event,"PROTECTED");return retain(RuntimeFaultArchiveB01.State.BLOCKED_PROTECTED,"PRODUCER_PENDING_OR_MUTABLE:"+event);
                }
                final String hash=FaultOriginalCodecR335.sha256(body);final Map<String,Object> values=FaultOriginalCodecR335.values(body);
                if(cancelled(deadline))return retain(RuntimeFaultArchiveB01.State.YIELDED,"BEFORE_CLAIM");
                final Work expected=prior;
                Work claim=store.coordinatorR36().runAtomicWriteOrThrowF01(context("R335_FAULT_ARCHIVE_CLAIM"),tx->{
                    Work current=work(tx);if(current.revision!=expected.revision)throw new IllegalStateException("R335_FAULT_ARCHIVE_CAS");
                    Work next=new Work();next.revision=Math.addExact(current.revision,1);next.fence=Math.addExact(current.fence,1);next.after=current.after;next.event=event;next.hash=hash;next.stage="PREPARING";next.owner=WriterHoldLedgerR30.PROCESS_SESSION;
                    tx.execSQL("UPDATE r335_fault_archive_work SET revision=?,selected_event=?,selected_hash=?,stage='PREPARING',owner_epoch=?,fence=?,updated_at=? WHERE singleton=1",new Object[]{next.revision,event,hash,next.owner,next.fence,System.currentTimeMillis()});return next;
                });
                File directory=new File(files,DIRECTORY);long growth=2L*body.length+65536L;
                try(StorageBudgetR31.Reservation budget=StorageBudgetR31.forFiles(files).reserve("R335_FAULT_ORIGINAL",claim.fence,StorageBudgetR31.Category.DIAGNOSTICS,"files/"+DIRECTORY,growth,growth,false)){
                    if(cancelled(deadline))return retain(RuntimeFaultArchiveB01.State.YIELDED,"BEFORE_CAS_PUBLICATION");
                    File cas=publish(files,body,hash,budget);byte[] verified=readFile(files,hash,body.length);
                    if(!Arrays.equals(body,verified))throw new IOException("R335_FAULT_CAS_ORIGINAL_CHANGED");
                    if(cancelled(deadline))return retain(RuntimeFaultArchiveB01.State.YIELDED,"BEFORE_LOCATOR_COMMIT");
                    String key="R335_FAULT_ORIGINAL:"+event+":"+hash;
                    DatabaseCoordinator.CommitIdentityR31 identity=new DatabaseCoordinator.CommitIdentityR31(key,hash,"R335_FAULT_ARCHIVE_COMMIT",claim.owner,claim.fence,hash,0,FaultOriginalCodecR335.CODEC);
                    store.coordinatorR36().runIdempotentAtomicWriteR31(context("R335_FAULT_ARCHIVE_COMMIT"),identity,tx->{
                        Work current=work(tx);
                        if(current.revision!=claim.revision||current.fence!=claim.fence||!current.owner.equals(claim.owner)||!current.hash.equals(hash)||!current.event.equals(event)||!"PREPARING".equals(current.stage))throw new IllegalStateException("R335_FAULT_ARCHIVE_FENCE");
                        if(cancelled(deadline)||WriterHoldFaultsR31.retainsHotOriginalR33(event))throw new IllegalStateException("DEPENDENCY(R335_FAULT_ORIGINAL_PINNED)");
                        byte[] actual=hot(tx,event);if(actual==null||!Arrays.equals(body,actual)||!independent(actual))throw new IllegalStateException("R335_FAULT_ARCHIVE_STALE_ORIGINAL");
                        tx.execSQL("INSERT INTO r335_fault_original("+LOCATOR_COLUMNS+") VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{event,hash,body.length,FaultOriginalCodecR335.CODEC,DIRECTORY+"/"+hash+".row",values.get("owner_epoch"),values.get("task_id"),values.get("category"),values.get("last_wall_ms"),key,ReleaseManifestR926.MANIFEST_SHA256,System.currentTimeMillis()});
                        tx.execSQL("DELETE FROM r31_runtime_fault WHERE event_id=?",new Object[]{event});
                        if(scalar(tx,"SELECT changes()")!=1)throw new IllegalStateException("R335_FAULT_ARCHIVE_DELETE_COUNT");
                        tx.execSQL("UPDATE r335_fault_archive_work SET revision=revision+1,after_event=?,selected_event='',selected_hash='',stage='COMPLETED',updated_at=? WHERE singleton=1",new Object[]{event,System.currentTimeMillis()});
                        return 1;
                    },1);
                    // Independent connection, then independently re-read exact file bytes, before reporting release.
                    try(SQLiteDatabase readback=SQLiteDatabase.openDatabase(db.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
                        if(hot(readback,event)!=null)throw new IOException("R335_FAULT_READBACK_HOT_REMAINS");
                        requireLocatorReceipt(readback,event,hash,body.length,key);
                        if(!Arrays.equals(body,readFile(files,hash,body.length)))throw new IOException("R335_FAULT_READBACK_BODY_CHANGED");
                    }
                    lastDetail="VERIFIED_ORIGINAL_RETIRED:"+event+":"+hash;return RuntimeFaultArchiveB01.Page.committed(1);
                }
            }
        }catch(StorageBudgetR31.Rejected capacity){return retain(RuntimeFaultArchiveB01.State.BLOCKED_BUDGET,capacity.getMessage());}
        catch(IOException unavailable){return retain(RuntimeFaultArchiveB01.State.WAIT_VERIFIED,unavailable.getMessage());}
        catch(RuntimeException failure){Error fatal=FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;lastDetail=failure.getClass().getSimpleName()+":"+RuntimeFaultEvidenceR31.clean(failure.getMessage(),256);throw failure;}
    }
    private static void advanceSkipped(DbStore store,Work old,String event,String state){
        store.coordinatorR36().runAtomicWriteOrThrowF01(context("R335_FAULT_ARCHIVE_SKIP"),db->{Work current=work(db);if(current.revision!=old.revision)throw new IllegalStateException("R335_FAULT_SKIP_CAS");
            db.execSQL("UPDATE r335_fault_archive_work SET revision=revision+1,after_event=?,selected_event='',selected_hash='',stage=?,updated_at=? WHERE singleton=1",new Object[]{event,state,System.currentTimeMillis()});return 0;});
    }
    private static File publish(File files,byte[] body,String hash,StorageBudgetR31.Reservation budget)throws IOException {
        File root=new File(files,DIRECTORY);if(!root.isDirectory()&&!root.mkdir())throw new IOException("R335_FAULT_DIRECTORY");
        File target=path(files,hash),partial=StorageBudgetR31.inside(root.getCanonicalFile(),hash+".new");
        if(target.exists()){readFile(files,hash,body.length);return target;}
        try(FileOutputStream output=new FileOutputStream(partial);OutputStream protectedOutput=budget.wrap(partial,output)){protectedOutput.write(body);protectedOutput.flush();output.getFD().sync();}
        if(!Arrays.equals(body,StorageBudgetR31.readBounded(partial,FaultOriginalCodecR335.MAX_BYTES)))throw new IOException("R335_FAULT_PARTIAL_CHANGED");
        if(!partial.renameTo(target))throw new IOException("R335_FAULT_ATOMIC_PUBLICATION");
        syncDirectory(root);readFile(files,hash,body.length);return target;
    }
    static void syncDirectory(File root)throws IOException {
        FileDescriptor fd=null;try{fd=Os.open(root.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}catch(Exception e){throw new IOException("R335_FAULT_DIRECTORY_FSYNC",e);}finally{if(fd!=null)try{Os.close(fd);}catch(Exception e){throw new IOException("R335_FAULT_DIRECTORY_CLOSE",e);}}
    }
    public static File path(File files,String hash)throws IOException {if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("R335_FAULT_HASH");return StorageBudgetR31.inside(files.getCanonicalFile(),DIRECTORY+"/"+hash+".row");}
    public static byte[] readFile(File files,String hash,long expectedBytes)throws IOException {
        File file=path(files,hash);if(!file.isFile()||file.length()!=expectedBytes||expectedBytes<1||expectedBytes>FaultOriginalCodecR335.MAX_BYTES)throw new IOException("R335_FAULT_ORIGINAL_MISSING_OR_BYTES");
        byte[] body=StorageBudgetR31.readBounded(file,FaultOriginalCodecR335.MAX_BYTES);if(!hash.equals(FaultOriginalCodecR335.sha256(body)))throw new IOException("R335_FAULT_ORIGINAL_HASH");FaultOriginalCodecR335.decode(body);return body;
    }
    private static void requireLocatorReceipt(SQLiteDatabase db,String event,String hash,long bytes,String key)throws IOException {
        try(Cursor c=db.rawQuery("SELECT o.original_hash,o.original_bytes,o.codec,o.relative_path,r.payload_hash,r.operator_version FROM r335_fault_original o JOIN r31_commit_receipt r ON r.commit_key=o.receipt_key WHERE o.event_id=? AND o.receipt_key=?",new String[]{event,key})){
            if(!c.moveToFirst()||!hash.equals(c.getString(0))||bytes!=c.getLong(1)||!FaultOriginalCodecR335.CODEC.equals(c.getString(2))||!(DIRECTORY+"/"+hash+".row").equals(c.getString(3))||!hash.equals(c.getString(4))||!FaultOriginalCodecR335.CODEC.equals(c.getString(5)))throw new IOException("R335_FAULT_LOCATOR_RECEIPT_INTEGRITY");
        }
    }
    /** An absent cold body is an integrity error, never an absent historical fault. No cold original expires. */
    public static Map<String,Object> readHotCold(SQLiteDatabase db,File files,String event)throws IOException {
        byte[] hot=hot(db,event);
        if(!ready(db))return hot==null?null:FaultOriginalCodecR335.values(hot);
        return readWithColdLocator(db,files,event,hot);
    }
    private static Map<String,Object> readWithColdLocator(SQLiteDatabase db,File files,String event,byte[] hot)throws IOException {
        try(Cursor c=db.rawQuery("SELECT original_hash,original_bytes,receipt_key FROM r335_fault_original WHERE event_id=?",new String[]{event})){
            if(!c.moveToFirst())return hot==null?null:FaultOriginalCodecR335.values(hot);String hash=c.getString(0);long bytes=c.getLong(1);String key=c.getString(2);
            requireLocatorReceipt(db,event,hash,bytes,key);byte[] cold=readFile(files,hash,bytes);
            if(hot!=null&&!Arrays.equals(hot,cold))throw new IOException("R335_FAULT_HOT_COLD_IDENTITY_CONFLICT");
            Map<String,Object> out=FaultOriginalCodecR335.values(cold);
            if(!event.equals(out.get("event_id")))throw new IOException("R335_FAULT_ORIGINAL_EVENT_ID");return out;
        }
    }
    private static boolean hasTable(SQLiteDatabase db,String table){
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){
            return c.moveToFirst();
        }
    }

    /** A bounded display sample from one read-only report snapshot. The exported index remains the denominator. */
    public static Map<String,Object> reportSample(SQLiteDatabase db,File files)throws IOException {
        if(!db.isReadOnly()||!ReportInputsLeaseCP12.currentThreadHoldsInput(files))throw new IOException("R335_FAULT_REPORT_SNAPSHOT_AND_PIN_REQUIRED");
        boolean hotSource=hasTable(db,"r31_runtime_fault");
        boolean coldLocator=hasTable(db,"r335_fault_original"),commitSource=hasTable(db,"r31_commit_receipt");
        boolean coldSource=coldLocator&&commitSource;
        List<String> missing=new ArrayList<>();
        if(!hotSource)missing.add("r31_runtime_fault");
        if(!coldLocator)missing.add("r335_fault_original");
        if(!commitSource)missing.add("r31_commit_receipt");
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("scope","EXACT_READ_ONLY_REPORT_SNAPSHOT; MAX_20_ROWS_16_KIB");
        out.put("hotSchemaReady",hotSource);out.put("coldSchemaReady",coldSource);out.put("missingSourceTables",missing);
        out.put("rows",null);out.put("totalOriginals",null);out.put("observedOriginals",null);
        if(!hotSource&&!coldSource){
            out.put("sourceObservationR341",ReportSourceStateR341.observation("R335_RUNTIME_FAULT_ORIGINALS",
                    "IMMUTABLE_REPORT_COPY","WAITING_SOURCE","MISSING_SCHEMA:"+String.join(",",missing),null,null));
            return out;
        }
        StringJoiner sources=new StringJoiner(" UNION ");
        if(hotSource)sources.add("SELECT event_id,last_wall_ms FROM r31_runtime_fault");
        if(coldSource)sources.add("SELECT event_id,last_wall_ms FROM r335_fault_original");
        String union=sources.toString();
        List<Object> rows=new ArrayList<>();int bytes=0,omitted=0,unavailable=0;
        try {
            long total=scalar(db,"SELECT count(*) FROM (SELECT event_id FROM ("+union+") GROUP BY event_id)");
            try(Cursor c=db.rawQuery("SELECT event_id,max(last_wall_ms) FROM ("+union+") GROUP BY event_id ORDER BY max(last_wall_ms) DESC,event_id LIMIT 20",null)){
                while(c.moveToNext()){
                    String event=c.getString(0);Map<String,Object> row;
                    try{
                        byte[] current=hotSource?hot(db,event):null;
                        row=coldSource?readWithColdLocator(db,files,event,current):current==null?null:FaultOriginalCodecR335.values(current);
                        if(row==null)throw new IOException("R335_FAULT_REPORT_ORIGINAL_DISAPPEARED");
                    }catch(IOException unreadable){
                        row=new LinkedHashMap<>();row.put("event_id",event);
                        row.put("readState","ORIGINAL_UNAVAILABLE_PRESERVED_LOCATOR");
                        row.put("error",unreadable.getMessage());unavailable++;
                    }
                    int length=JsonR27.write(row).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    if(length>16384-bytes){omitted++;continue;}bytes+=length;rows.add(row);
                }
            }
            Long knownTotal=missing.isEmpty()?total:null;
            String state=!missing.isEmpty()?"PARTIAL":unavailable>0?"READ_ERROR":total==0?"OBSERVED_EMPTY":"READ_OK";
            String reason=!missing.isEmpty()?"MISSING_SCHEMA:"+String.join(",",missing):unavailable>0?"ORIGINAL_UNAVAILABLE_PRESERVED_LOCATOR":"";
            out.put("rows",rows);out.put("totalOriginals",knownTotal);out.put("observedOriginals",total);
            out.put("shown",rows.size());out.put("omitted",knownTotal==null?null:Math.max(0,total-rows.size()));
            out.put("omittedFromAvailableSources",Math.max(0,total-rows.size()));
            out.put("byteOmittedAmongFirst20",omitted);out.put("unavailableAmongFirst20",unavailable);
            out.put("sourceObservationR341",ReportSourceStateR341.observation("R335_RUNTIME_FAULT_ORIGINALS",
                    "IMMUTABLE_REPORT_COPY",state,reason,rows.size(),knownTotal));
        }catch(android.database.sqlite.SQLiteException failure){
            out.put("sourceObservationR341",ReportSourceStateR341.observation("R335_RUNTIME_FAULT_ORIGINALS",
                    "IMMUTABLE_REPORT_COPY","READ_ERROR",RuntimeFaultEvidenceR31.clean(failure.getMessage(),240),null,null));
        }
        return out;
    }
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("schemaReady",ready(db));out.put("scope","INDEPENDENT_IMMUTABLE_WRITER_HOLD_ONLY; MUTABLE_TASK_FAULTS_RETAINED_HOT");
        out.put("physicalBytesReleased",null);out.put("releaseMeaning","HOT_QUOTA_ROWS_ONLY; NO_SQLITE_FILE_SHRINK_CLAIM");out.put("originalExpiry","NONE; ALL_LOCATORS_AND_CAS_PROTECTED");
        if(ready(db)){out.put("coldOriginals",scalar(db,"SELECT count(*) FROM r335_fault_original"));out.put("coldOriginalBytes",scalar(db,"SELECT coalesce(sum(original_bytes),0) FROM r335_fault_original"));Work w=work(db);out.put("workRevision",w.revision);out.put("afterEvent",w.after);out.put("selectedEvent",w.event);out.put("selectedHash",w.hash);out.put("stage",w.stage);out.put("workerFence",w.fence);out.put("owner",w.owner);}
        return out;
    }
    public static String processDetail(){return lastDetail;}
    public static boolean mainProcess(Context context){
        String expected=context.getApplicationInfo().processName;
        if(android.os.Build.VERSION.SDK_INT>=28)return expected.equals(android.app.Application.getProcessName());
        try(InputStream in=new FileInputStream("/proc/self/cmdline")){ByteArrayOutputStream out=new ByteArrayOutputStream();for(int i=0;i<256;i++){int b=in.read();if(b<=0)break;out.write(b);}return expected.equals(out.toString("UTF-8"));}catch(IOException unavailable){return false;}
    }
}
