package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.StorageBudgetR31;
import java.io.*;
import java.lang.ref.*;
import java.util.*;

/** Admission for new optional research pins, never for canonical source events or evidence deletion. */
public final class PinPublicationR31 {
    private PinPublicationR31(){}
    private static final long MiB=1L<<20;
    private static final int MAX_PAYLOADS=1024; // Same finite global lease-owner ceiling; admission normally binds first.
    private static final Object PAYLOAD_LOCK=new Object();
    private static final ReferenceQueue<String> PAYLOAD_QUEUE=new ReferenceQueue<>();
    private static final Map<Reference<?>,MemoryBudgetR31.Lease> PAYLOADS=new IdentityHashMap<>();
    private static int preparing;private static long collectedPayloads;
    private static volatile String lastAdmission="UNOBSERVED",lastCleanup="",lastFailure="";
    private static volatile long lastGrowthBound,lastBindBytes;private static volatile boolean lastBindExact;
    private static final Runnable REAP=PinPublicationR31::reapCollectedPayloadsR31;
    public static final class CapacityWait extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        public final String reason;
        CapacityWait(String reason,Throwable cause){super("RESOURCE_WAIT:PIN_"+reason,cause);this.reason=reason;}
        public String failureCategory(){return reason.equals("CANCELLED")?"CANCELLED":reason.equals("OWNER_CLOSED")||reason.equals("OWNER_THREAD_CHANGED")?"STALE_OWNER":"DEPENDENCY";}
        public String operation(){return "RESEARCH_INPUT_PIN";}public String phase(){return "PIN_ADMISSION";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    private static CapacityWait waitFor(String reason,Throwable cause){lastAdmission="WAITING";lastFailure=reason;return new CapacityWait(reason,cause);}
    /** Working estimate covers bounded codec/parse/snapshot scratch, not an observed live-heap measurement. */
    public static long preparationBytesR31(int rows){if(rows<1||rows>10000)throw new IllegalArgumentException("PIN_INPUT_BOUNDARY");return Math.addExact(2*MiB,Math.multiplyExact(4096L,rows));}
    public static long bindCapR31(){return Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);}
    static Scope memoryOnly(String dataset,long revision,int rows){return acquire(null,0,dataset,revision,rows);}
    public static Scope admit(SQLiteDatabase ro,File filesRoot,String dataset,long revision,int rows){
        if(ro==null||filesRoot==null||ro.inTransaction())throw new IllegalArgumentException("PIN_PREPARE_OUTSIDE_WRITER_REQUIRED");
        try{return admit(ro,filesRoot,StorageBudgetR31.forFiles(filesRoot),dataset,revision,rows);}catch(IOException failure){throw waitFor("STORAGE_"+failure.getClass().getSimpleName()+":"+failure.getMessage(),failure);}
    }
    /** Explicit storage configuration injection is confined to package fixtures; production uses forFiles(). */
    static Scope admit(SQLiteDatabase ro,File filesRoot,StorageBudgetR31 budget,String dataset,long revision,int rows){
        if(ro==null||filesRoot==null||budget==null||ro.inTransaction())throw new IllegalArgumentException("PIN_PREPARE_OUTSIDE_WRITER_REQUIRED");
        Scope scope=acquire(null,0,dataset,revision,rows);StorageBudgetR31.Reservation disk=null;
        try{
            File root=filesRoot.getCanonicalFile().getParentFile(),db=new File(ro.getPath()).getCanonicalFile();
            if(!db.getParentFile().equals(new File(root,"databases").getCanonicalFile()))throw waitFor("DATABASE_SCOPE_UNSUPPORTED",null);
            long page;try(Cursor q=ro.rawQuery("PRAGMA page_size",null)){if(!q.moveToFirst())throw waitFor("PAGE_SIZE_UNKNOWN",null);page=q.getLong(0);}
            if(page<512||page>65536||(page&(page-1))!=0)throw waitFor("PAGE_SIZE_UNSUPPORTED",null);
            long rowBound=Math.addExact(bindCapR31(),Math.multiplyExact(16L,page));
            // The old WAL can be materialized into the DB at a checkpoint. The directory scope
            // also covers a rollback journal/SHM plus a bounded critical-concurrency margin.
            long wal=new File(db.getPath()+"-wal").length();
            long bound=Math.addExact(Math.addExact(Math.multiplyExact(3L,rowBound),wal),2*MiB);
            disk=budget.reserve("research-pin:"+dataset,revision,StorageBudgetR31.Category.INPUT_RESEARCH,"databases",bound,false);
            disk.beforeChunk(bound);lastGrowthBound=bound;scope.disk=disk;scope.maximumDatabaseGrowthBytes=bound;lastAdmission="ADMITTED_NEW_RESEARCH_PIN";
            return scope;
        }catch(IOException failure){closeDisk(disk);scope.close();throw waitFor("STORAGE_"+failure.getMessage(),failure);}
        catch(RuntimeException|Error failure){closeDisk(disk);scope.close();throw failure;}
    }
    private static Scope acquire(StorageBudgetR31.Reservation disk,long growth,String dataset,long revision,int rows){
        preparationBytesR31(rows);MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();memory.registerPressureReclaimerR31("PIN_PAYLOAD_COLLECTION",REAP);reapCollectedPayloadsR31();
        synchronized(PAYLOAD_LOCK){if(preparing+PAYLOADS.size()>=MAX_PAYLOADS){closeDisk(disk);throw waitFor("PAYLOAD_TRACKER_CAPACITY",null);}preparing++;}
        MemoryBudgetR31.Lease scratch=null,payload=null;
        try{
            MemoryBudgetR31.Admission a=memory.tryAcquire("pin-prepare:"+dataset,revision,MemoryBudgetR31.Kind.RESEARCH,preparationBytesR31(rows),()->Thread.currentThread().isInterrupted());
            if(!a.acquired()){lastAdmission="WAITING";lastFailure="MEMORY_"+a.reason;throw new MemoryBudgetR31.CapacityWait(a.reason);}scratch=a.lease;
            // Retained UTF-16 JSON plus bounded immutable-batch UTF-8 hash/copy scratch.
            a=memory.tryAcquire("pin-bind:"+dataset,revision,MemoryBudgetR31.Kind.RESEARCH,4*bindCapR31()+65536,()->Thread.currentThread().isInterrupted());
            if(!a.acquired()){lastAdmission="WAITING";lastFailure="MEMORY_"+a.reason;throw new MemoryBudgetR31.CapacityWait(a.reason);}payload=a.lease;
            lastAdmission=disk==null?"MEMORY_ONLY_LEGACY_DISK_NOT_ADMITTED":"ADMITTED_NEW_RESEARCH_PIN";lastFailure="";
            return new Scope(Thread.currentThread(),disk,growth,scratch,payload);
        }catch(RuntimeException|Error failed){if(scratch!=null)scratch.close();if(payload!=null)payload.close();synchronized(PAYLOAD_LOCK){preparing--;}closeDisk(disk);throw failed;}
    }
    private static void closeDisk(StorageBudgetR31.Reservation disk){if(disk!=null)try{disk.close();}catch(IOException failure){lastCleanup=failure.getClass().getSimpleName()+":"+failure.getMessage();}}
    public static final class Scope implements AutoCloseable {
        private final Thread owner;private StorageBudgetR31.Reservation disk;private MemoryBudgetR31.Lease scratch,payload;
        private long maximumDatabaseGrowthBytes;private boolean closed,tracked;
        private Scope(Thread owner,StorageBudgetR31.Reservation disk,long growth,MemoryBudgetR31.Lease scratch,MemoryBudgetR31.Lease payload){this.owner=owner;this.disk=disk;maximumDatabaseGrowthBytes=growth;this.scratch=scratch;this.payload=payload;}
        public long maximumDatabaseGrowthBytes(){return maximumDatabaseGrowthBytes;}
        public synchronized void requireActive(){if(closed)throw waitFor("OWNER_CLOSED",null);if(Thread.currentThread()!=owner)throw waitFor("OWNER_THREAD_CHANGED",null);if(owner.isInterrupted())throw waitFor("CANCELLED",null);}
        void payload(String json){requireActive();synchronized(PAYLOAD_LOCK){if(tracked)throw new IllegalStateException("PIN_PAYLOAD_ALREADY_TRACKED");PAYLOADS.put(new WeakReference<String>(json,PAYLOAD_QUEUE),payload);payload=null;tracked=true;preparing--;}}
        /** Only codec/verification scratch ends at append return; immutable bind data still has references. */
        void preparationFinished(){if(scratch!=null){scratch.close();scratch=null;}}
        @Override public synchronized void close(){if(closed)return;if(Thread.currentThread()!=owner)throw waitFor("OWNER_THREAD_CHANGED",null);closed=true;preparationFinished();if(payload!=null){payload.close();payload=null;}if(!tracked)synchronized(PAYLOAD_LOCK){preparing--;}StorageBudgetR31.Reservation previous=disk;disk=null;closeDisk(previous);}
    }
    static void enforceBatch(ResearchWriteQueueR27.Batch batch,Object[] newArgs){
        if(batch.ops.size()+1>64)throw waitFor("BATCH_OP_CAPACITY",null);long cap=bindCapR31(),bytes=bindBytes(newArgs,cap);
        for(ResearchWriteQueueR27.Op op:batch.ops){if(bytes>cap)break;bytes+=bindBytes(op.args,cap-bytes);}
        lastBindBytes=bytes;lastBindExact=bytes<=cap;if(bytes>cap)throw waitFor("BIND_CAPACITY:AT_LEAST_"+bytes+">"+cap,null);
    }
    private static long bindBytes(Object[] args,long limit){long size=0;if(args!=null)for(Object arg:args){
        if(arg instanceof String){String s=(String)arg;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<128)size++;else if(c<2048)size+=2;else if(Character.isHighSurrogate(c)&&i+1<s.length()&&Character.isLowSurrogate(s.charAt(i+1))){size+=4;i++;}else if(Character.isSurrogate(c))size++;else size+=3;if(size>limit)return limit+1;}}
        else size+=arg instanceof byte[]?((byte[])arg).length:8L;if(size>limit)return limit+1;
    }return size;}
    public static void reapCollectedPayloadsR31(){List<MemoryBudgetR31.Lease> finished=new ArrayList<>();synchronized(PAYLOAD_LOCK){Reference<?> ref;while((ref=PAYLOAD_QUEUE.poll())!=null){MemoryBudgetR31.Lease lease=PAYLOADS.remove(ref);if(lease!=null){finished.add(lease);collectedPayloads++;}}}for(MemoryBudgetR31.Lease lease:finished)lease.close();}
    public static Map<String,Object> snapshotR31(){reapCollectedPayloadsR31();Map<String,Object> out=new LinkedHashMap<>();synchronized(PAYLOAD_LOCK){out.put("retainedImmutablePayloadDeclarations",PAYLOADS.size());out.put("preparing",preparing);out.put("payloadDeclarationsCollected",collectedPayloads);}out.put("lastAdmission",lastAdmission);out.put("lastFailure",lastFailure);out.put("lastCleanupFailure",lastCleanup);out.put("lastDeclaredDatabaseGrowthBytes",lastGrowthBound);out.put("lastBatchBindBytes",lastBindBytes);out.put("lastBatchBindExact",lastBindExact);out.put("normalHardBytes",2048*MiB);out.put("transientHardBytes",4096*MiB);out.put("freeFloorBytes",256*MiB);out.put("sourceJournalRetention","OPEN_APPEND_ONLY_CRITICAL_SOURCE_NOT_QUOTA_BLOCKED");out.put("importMemberDiskAdmission","OPEN_UNTIL_HISTORY_OWNER_INTEGRATION");out.put("onlineWindowFenceAdmission","OPEN_UNTIL_ONLINE_OWNER_INTEGRATION");out.put("nativeCodecCapacity","OPEN_NOT_INFERRED_FROM_JAVA_HEAP");out.put("heapBytesFreedClaimed",false);return Collections.unmodifiableMap(out);}
}
