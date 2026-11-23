package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact-byte recovery only. A missing body never becomes a newly trained model under an old identity. */
public final class CheckpointRecoveryR335 {
    private CheckpointRecoveryR335(){}
    public static final String DOMAIN="RESEARCH_R335_CHECKPOINT_RECOVERY",DIRECTORY="r335-checkpoint-recovery";
    public static final String[] OBJECTS={"r335_checkpoint_recovery","r335_checkpoint_recovery_order","r335_checkpoint_recovery_cursor"};
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r335_checkpoint_recovery(expected_hash TEXT PRIMARY KEY,owner_identity TEXT NOT NULL,status TEXT NOT NULL,attempts INTEGER NOT NULL DEFAULT 0,child_cursor INTEGER NOT NULL DEFAULT 0,source_evidence TEXT NOT NULL DEFAULT '',last_error TEXT NOT NULL DEFAULT '',updated_seq INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL DEFAULT 0)",
        "CREATE INDEX IF NOT EXISTS r335_checkpoint_recovery_order ON r335_checkpoint_recovery(status,updated_seq,expected_hash)",
        "CREATE TABLE IF NOT EXISTS r335_checkpoint_recovery_cursor(singleton INTEGER PRIMARY KEY CHECK(singleton=1),sequence INTEGER NOT NULL,intake_after TEXT NOT NULL DEFAULT '')"
    };
    private static final LinkedHashMap<String,String> SIGNALS=new LinkedHashMap<>();
    private static long lostSignals;
    private static volatile String observation="STARTUP_WAIT";
    /** Read callbacks perform no SQL or file mutation. Original exception remains the consumer's dependency. */
    public static synchronized void request(String expected,String owner){
        if(expected==null||!expected.matches("[0-9a-f]{64}"))return;
        if(SIGNALS.containsKey(expected))return;if(SIGNALS.size()>=64){lostSignals++;return;}
        SIGNALS.put(expected,RuntimeFaultEvidenceR31.clean(owner==null?"UNKNOWN_CONSUMER":owner,256));
    }
    public static String currentOwner(){RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();return claim==null?"CAS_CONSUMER_NO_TASK_CLAIM":claim.task+":"+claim.owner+":"+claim.fence;}
    public static void ensureSchema(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R335_CHECKPOINT_SCHEMA_OWNER");for(String sql:DDL)db.execSQL(sql);db.execSQL("INSERT OR IGNORE INTO r335_checkpoint_recovery_cursor(singleton,sequence,intake_after) VALUES(1,0,'')");}
    public static boolean ready(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE name IN ('r335_checkpoint_recovery','r335_checkpoint_recovery_order','r335_checkpoint_recovery_cursor')",null)){return c.moveToFirst()&&c.getLong(0)==3;}}
    private static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R335_CHECKPOINT_QUERY_EMPTY");return c.getLong(0);}}
    private static DatabaseCoordinator.WriteContext context(String op){return DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,op).withDeadlineMonoMs(TimeSourceR811.monoNowMs()+20L);}
    private static void check(long deadline)throws InterruptedIOException {if(Thread.currentThread().isInterrupted()||android.os.SystemClock.elapsedRealtime()>=deadline||RealtimePressureGateR811.hasActiveCriticalWork())throw new InterruptedIOException("R335_CHECKPOINT_RECOVERY_YIELDED");}
    private static long sequence(SQLiteDatabase db){db.execSQL("UPDATE r335_checkpoint_recovery_cursor SET sequence=sequence+1 WHERE singleton=1");return scalar(db,"SELECT sequence FROM r335_checkpoint_recovery_cursor WHERE singleton=1");}
    private static void add(SQLiteDatabase db,String hash,String owner){
        if(scalar(db,"SELECT count(*) FROM r335_checkpoint_recovery")>=16384&&scalar(db,"SELECT count(*) FROM r335_checkpoint_recovery WHERE expected_hash=?",hash)==0)
            throw new IllegalStateException("DEPENDENCY(R335_CHECKPOINT_RECOVERY_INDEX_CAPACITY)");
        db.execSQL("INSERT OR IGNORE INTO r335_checkpoint_recovery(expected_hash,owner_identity,status) VALUES(?,?,'WAIT_SOURCE')",new Object[]{hash,owner});
    }
    private static void persistSignals(DbStore store){
        Map<String,String> pending=new LinkedHashMap<>();synchronized(CheckpointRecoveryR335.class){for(Map.Entry<String,String> e:SIGNALS.entrySet()){pending.put(e.getKey(),e.getValue());if(pending.size()==8)break;}}
        if(pending.isEmpty())return;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(context("R335_CHECKPOINT_RECOVERY_REQUEST"),db->{
            for(Map.Entry<String,String> e:pending.entrySet()){add(db,e.getKey(),e.getValue());db.execSQL("UPDATE r335_checkpoint_recovery SET status='WAIT_SOURCE',child_cursor=0,last_error='CONSUMER_MISSING_ORIGINAL',updated_seq=?,updated_at=? WHERE expected_hash=? AND status='BYTES_RESTORED_NOT_MODEL_ACCEPTED'",new Object[]{sequence(db),System.currentTimeMillis(),e.getKey()});}return 0;
        });
        synchronized(CheckpointRecoveryR335.class){for(Map.Entry<String,String> e:pending.entrySet())SIGNALS.remove(e.getKey(),e.getValue());}
    }
    /** Installation/SAF tooling supplies an explicit exact-byte intake. Discovery is keyset-paged and creates no model. */
    private static void discoverIntake(DbStore store,long deadline)throws IOException {
        File directory=new File(store.contextForR26().getFilesDir(),DIRECTORY);if(!directory.isDirectory())return;
        if(!directory.getAbsoluteFile().equals(directory.getCanonicalFile()))throw new IOException("R335_RECOVERY_INTAKE_SYMLINK");
        String after;try(Cursor c=store.readable().rawQuery("SELECT intake_after FROM r335_checkpoint_recovery_cursor WHERE singleton=1",null)){if(!c.moveToFirst())throw new IOException("R335_RECOVERY_CURSOR_MISSING");after=c.getString(0);}
        TreeSet<String> page=new TreeSet<>();int scanned=0;
        try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(directory)){File file;while((file=cursor.next())!=null){check(deadline);if(++scanned>32770)throw new IOException("R335_RECOVERY_INTAKE_ENTRY_BOUND");String name=file.getName();if(!name.matches("[0-9a-f]{64}\\.proof\\.json"))continue;String hash=name.substring(0,64);if(hash.compareTo(after)<=0)continue;page.add(hash);if(page.size()>8)page.pollLast();}}
        final String next=page.isEmpty()?"":page.last();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(context("R335_CHECKPOINT_RECOVERY_INTAKE"),db->{for(String hash:page){
                add(db,hash,"EXPLICIT_EXACT_BYTE_INTAKE");
                db.execSQL("UPDATE r335_checkpoint_recovery SET updated_at=0 WHERE expected_hash=? AND status='WAIT_SOURCE'",
                    new Object[]{hash});
            }db.execSQL("UPDATE r335_checkpoint_recovery_cursor SET intake_after=? WHERE singleton=1",new Object[]{next});return 0;});
    }
    private static final class Item {String hash,owner,status,source;long attempts,seq;int childCursor;}
    private static Item next(DbStore store){return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(context("R335_CHECKPOINT_RECOVERY_CLAIM"),db->{
        Item out=new Item();try(Cursor c=db.rawQuery("SELECT expected_hash,owner_identity,status,attempts,child_cursor,source_evidence FROM r335_checkpoint_recovery WHERE status<>'BYTES_RESTORED_NOT_MODEL_ACCEPTED'"
            +" AND (status<>'WAIT_SOURCE' OR attempts=0 OR updated_at<=? OR updated_at>?) ORDER BY updated_seq,expected_hash LIMIT 1",
            new String[]{Long.toString(System.currentTimeMillis()-15L*60L*1000L),Long.toString(System.currentTimeMillis())})){
            if(!c.moveToFirst())return null;out.hash=c.getString(0);out.owner=c.getString(1);out.status=c.getString(2);out.attempts=Math.addExact(c.getLong(3),1);out.childCursor=c.getInt(4);out.source=c.getString(5);
        }
        out.seq=sequence(db);db.execSQL("UPDATE r335_checkpoint_recovery SET attempts=?,updated_seq=?,updated_at=? WHERE expected_hash=?",new Object[]{out.attempts,out.seq,System.currentTimeMillis(),out.hash});return out;
    });}
    private static void finish(DbStore store,Item item,String state,int cursor,String source,String detail){
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(context("R335_CHECKPOINT_RECOVERY_OBSERVE"),db->{
            db.execSQL("UPDATE r335_checkpoint_recovery SET status=?,child_cursor=?,source_evidence=?,last_error=?,updated_at=? WHERE expected_hash=? AND updated_seq=?",new Object[]{state,cursor,source,RuntimeFaultEvidenceR31.clean(detail,512),System.currentTimeMillis(),item.hash,item.seq});
            if(scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R335_CHECKPOINT_RECOVERY_CAS");return 0;
        });observation=state+":"+item.hash+":"+RuntimeFaultEvidenceR31.clean(detail,128);
    }
    private static File inside(File directory,String name)throws IOException {
        File f=new File(directory,name).getAbsoluteFile();if(!f.equals(f.getCanonicalFile())||!f.getPath().startsWith(directory.getCanonicalPath()+File.separator))throw new IOException("R335_CHECKPOINT_SOURCE_PATH");return f;
    }
    private static byte[] bounded(File file,long expected,long deadline)throws IOException {
        if(!file.isFile()||expected<1||expected>CheckpointArtifactsR26.MAX_BYTES||file.length()!=expected)throw new IOException("R335_CHECKPOINT_SOURCE_BYTES");
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream((int)expected)){
            byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){check(deadline);if(n>expected-out.size())throw new IOException("R335_CHECKPOINT_SOURCE_GREW");out.write(buffer,0,n);}
            if(out.size()!=expected)throw new IOException("R335_CHECKPOINT_SOURCE_TRUNCATED");return out.toByteArray();
        }
    }
    public static String step(DbStore store,long deadline){
        if(store==null||!RuntimeFaultOriginalStoreR335.mainProcess(store.contextForR26()))return "WAIT_MAIN_OWNER";
        SQLiteDatabase db=store.readable();if(db.inTransaction())throw new IllegalStateException("R335_CHECKPOINT_RECOVERY_WRITER_HELD");
        if(!ready(db)||!CheckpointGcStoreR31.ready(db))return "WAIT_SCHEMA";
        Item item=null;
        try{
            check(deadline);persistSignals(store);discoverIntake(store,deadline);item=next(store);if(item==null){observation="NO_RUNNABLE_RECOVERY_REQUEST";return observation;}
            final Item selected=item;File files=store.contextForR26().getFilesDir().getCanonicalFile(),hotRoot=new File(files,"r26-checkpoints");
            File intake=new File(files,DIRECTORY),sourceFile=inside(intake,item.hash+".bin");
            boolean exists=ColdCheckpointR324.exists(hotRoot,item.hash);long length=exists?ColdCheckpointR324.rawSize(hotRoot,item.hash):sourceFile.length();
            if(!exists&&!sourceFile.isFile()){finish(store,item,"WAIT_SOURCE",0,"","NO_EXACT_BYTES_IN_HOT_COLD_OR_RECOVERY_INTAKE; OLD_IDENTITY_PRESERVED");return observation;}
            if(length<1||length>CheckpointArtifactsR26.MAX_BYTES){finish(store,item,"HASH_OR_SOURCE_INVALID_PRESERVED",0,"","SOURCE_SIZE_BOUND");return observation;}
            MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("r335-checkpoint-recovery",MemoryBudgetR31.Kind.CHECKPOINT,4L*length+(2L<<20),()->Thread.currentThread().isInterrupted());
            if(!admission.acquired()){finish(store,item,"WAIT_BUDGET",item.childCursor,item.source,"MEMORY:"+admission.reason);return observation;}
            try(MemoryBudgetR31.Lease memory=admission.lease){
                byte[] body=exists?ColdCheckpointR324.read(hotRoot,item.hash):bounded(sourceFile,length,deadline);check(deadline);
                if(!item.hash.equals(CheckpointArtifactsR26.sha(body))){finish(store,item,"HASH_OR_SOURCE_INVALID_PRESERVED",0,"","EXACT_OLD_HASH_MISMATCH; SOURCE_RETAINED");return observation;}
                String evidence=exists?"EXACT_HOT_OR_COLD_BYTES:"+item.hash:proof(intake,item.hash,length,deadline);
                if(body.length>=3&&body[0]==0x26&&body[1]==0x43&&body[2]==0x53&&!ChunkedCheckpointR26.isManifest(body)){
                    finish(store,item,"UNKNOWN_CHILD_CLOSURE_PRESERVED",0,evidence,"UNKNOWN_MANIFEST_VERSION");return observation;
                }
                List<String> children=ChunkedCheckpointR26.isManifest(body)?new ArrayList<>(ChunkedCheckpointR26.children(body)):Collections.emptyList();
                int start=Math.min(item.childCursor,children.size()),end=Math.min(children.size(),start+16);
                if(start<end){
                    store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(context("R335_CHECKPOINT_RECOVERY_CHILDREN"),tx->{for(int i=start;i<end;i++)add(tx,children.get(i),"CHILD_OF:"+selected.hash);return 0;});
                    for(int i=start;i<end;i++){
                        check(deadline);String child=children.get(i);
                        if(!ColdCheckpointR324.exists(hotRoot,child)||CheckpointGcStoreR31.storedState(db,child)!=StorageGcR31.State.LIVE){finish(store,item,"WAIT_CHILDREN",i,evidence,"CHILD_NOT_VERIFIED_LIVE:"+child);return observation;}
                        try(InputStream in=ColdCheckpointR324.open(hotRoot,child)){byte[] buffer=new byte[32768];while(in.read(buffer)!=-1)check(deadline);}
                    }
                    if(end<children.size()){finish(store,item,"VERIFYING_CHILDREN",end,evidence,"PAGE_VERIFIED");return observation;}
                }
                // Existing referenced roots and all recovery rows are GC roots; no historical reference changes.
                long growth=2L*body.length+65536L;
                try(StorageBudgetR31.Reservation budget=StorageBudgetR31.forFiles(files).reserve("R335_CHECKPOINT_EXACT_RECOVERY",item.seq,StorageBudgetR31.Category.MODEL,"files/r26-checkpoints",growth,growth,false)){
                    check(deadline);budget.beforeChunk(growth);
                    CheckpointArtifactsR26 cas=CheckpointArtifactsR26.forRecoveryR335(store);
                    try(CheckpointArtifactsR26.Prepared prepared=cas.prepareLease(body)){
                        if(!prepared.hash().equals(item.hash)||!Arrays.equals(body,cas.readHash(item.hash)))throw new IOException("R335_CHECKPOINT_RESTORE_IDENTITY");
                        final String sourceEvidence=evidence;String key="R335_CHECKPOINT_RESTORE:"+item.hash+":"+item.seq;
                        String payload=HashingSU1.fields(item.hash,sourceEvidence,Long.toString(length),Integer.toString(children.size()));
                        DatabaseCoordinator.CommitIdentityR31 identity=new DatabaseCoordinator.CommitIdentityR31(key,payload,"CHECKPOINT_EXACT_RECOVERY",WriterHoldLedgerR30.PROCESS_SESSION,item.seq,item.hash,0,"R335_EXACT_BYTES_NO_MODEL_ACCEPTANCE");
                        store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(context("R335_CHECKPOINT_RECOVERY_RECEIPT"),identity,tx->{
                            if(CheckpointGcStoreR31.storedState(tx,selected.hash)!=StorageGcR31.State.LIVE)throw new IllegalStateException("R335_RECOVERED_GC_NOT_LIVE");
                            long liveEdges=scalar(tx,"SELECT count(*) FROM r31_gc_edge e JOIN r31_gc_artifact a ON a.artifact_hash=e.to_hash AND a.state='LIVE' WHERE e.from_hash=?",selected.hash);
                            if(liveEdges!=children.size())throw new IllegalStateException("R335_RECOVERED_CHILD_NOT_LIVE");
                            tx.execSQL("UPDATE r335_checkpoint_recovery SET status='BYTES_RESTORED_NOT_MODEL_ACCEPTED',child_cursor=?,source_evidence=?,last_error='',updated_at=? WHERE expected_hash=? AND updated_seq=?",new Object[]{children.size(),sourceEvidence,System.currentTimeMillis(),selected.hash,selected.seq});
                            if(scalar(tx,"SELECT changes()")!=1)throw new IllegalStateException("R335_CHECKPOINT_RECEIPT_CAS");return 1;
                        },1);
                        try(SQLiteDatabase fresh=SQLiteDatabase.openDatabase(db.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
                            if(scalar(fresh,"SELECT count(*) FROM r335_checkpoint_recovery w JOIN r31_commit_receipt r ON r.commit_key=? WHERE w.expected_hash=? AND w.status='BYTES_RESTORED_NOT_MODEL_ACCEPTED' AND w.updated_seq=? AND r.payload_hash=?",key,item.hash,Long.toString(item.seq),payload)!=1)throw new IOException("R335_CHECKPOINT_RECEIPT_READBACK");
                        }
                        if(!Arrays.equals(body,cas.readHash(item.hash)))throw new IOException("R335_CHECKPOINT_FINAL_BYTES_CHANGED");
                    }
                }
                observation="BYTES_RESTORED_NOT_MODEL_ACCEPTED:"+item.hash;return observation;
            }
        }catch(InterruptedIOException yielded){observation="YIELDED";return observation;}
        catch(IOException failed){if(item!=null)finish(store,item,failed instanceof StorageBudgetR31.Rejected?"WAIT_BUDGET":"WAIT_SOURCE",item.childCursor,item.source,failed.getMessage());else observation="IO_UNAVAILABLE:"+failed.getMessage();return observation;}
        catch(RuntimeException failed){Error fatal=FailureDispositionR31.processFatalCause(failed);if(fatal!=null)throw fatal;observation="DEPENDENCY_OR_INTEGRITY:"+RuntimeFaultEvidenceR31.clean(failed.getMessage(),256);throw failed;}
    }
    private static String proof(File intake,String hash,long bytes,long deadline)throws IOException {
        File file=inside(intake,hash+".proof.json");if(!file.isFile()||file.length()>8192)throw new IOException("R335_RECOVERY_SOURCE_PROOF_MISSING");
        Object parsed=JsonR27.parse(new String(bounded(file,file.length(),deadline),StandardCharsets.UTF_8));if(!(parsed instanceof Map))throw new IOException("R335_RECOVERY_SOURCE_PROOF_CODEC");
        Map<?,?> p=(Map<?,?>)parsed;Object count=p.get("bytes");String member="r26-checkpoint-"+hash+".bin";
        if(!"R335_EXACT_CHECKPOINT_INTAKE_V1".equals(p.get("format"))||!hash.equals(p.get("expectedSha256"))||!(count instanceof Number)||((Number)count).longValue()!=bytes||!member.equals(p.get("sourceMember"))||!(p.get("sourceArchiveSha256") instanceof String)||!((String)p.get("sourceArchiveSha256")).matches("[0-9a-f]{64}"))throw new IOException("R335_RECOVERY_SOURCE_PROOF_IDENTITY");
        return "EXACT_MEMBER_SHA_BOUND_TO_OLD_REFERENCE;ARCHIVE="+p.get("sourceArchiveSha256")+";MEMBER="+member;
    }
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("schemaReady",ready(db));out.put("restorationAuthority","EXACT_OLD_BYTES_ONLY; NO_EVIDENCE_N_OR_CERTIFICATE_OR_MODEL_ACCEPTANCE");out.put("missingSourceAction","PRESERVE_OLD_IDENTITY; ORIGINAL_CONSUMER_STAYS_DEPENDENCY_ISOLATED; INDEPENDENT_RETRAINING_REQUIRES_NEW_IDENTITY");
        if(ready(db)){List<Object> counts=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT status,count(*) FROM r335_checkpoint_recovery GROUP BY status ORDER BY status",null)){while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();row.put("status",c.getString(0));row.put("count",c.getLong(1));counts.add(row);}}out.put("states",counts);}
        return out;
    }
    public static synchronized Map<String,Object> processObservation(){Map<String,Object> out=new LinkedHashMap<>();out.put("pendingSignals",SIGNALS.size());out.put("droppedSignals",lostSignals);out.put("lastState",observation);out.put("scope","CURRENT_PROCESS_DIAGNOSTIC; DURABLE_COUNTS_ARE_SEPARATE");return out;}
}
