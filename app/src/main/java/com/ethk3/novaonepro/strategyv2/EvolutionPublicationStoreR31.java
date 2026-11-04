package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Owner-prepared bounded staging and one atomic canonical publication. No runtime DDL. */
public final class EvolutionPublicationStoreR31 {
    private EvolutionPublicationStoreR31() {}
    public static final String ABI="R31_EVOLUTION_PUBLICATION_V1", SNAPSHOT_PREFIX="R31_EVOLUTION_SNAPSHOT_V1:";
    public static final int MAX_OPS=4096,MAX_SHAPES=48,MAX_LOGICAL_BYTES=8<<20,CHUNK_BYTES=8192;
    public static final int MAX_PRIVATE_SCRATCH_ROWS=8192,MAX_PRIVATE_MANIFESTS=8192;
    public static final long MAX_PRIVATE_SCRATCH_BYTES=32L<<20,MAX_PRIVATE_CHECKPOINT_BYTES=192L<<20;
    private static final String ARG_COLUMNS;
    static {StringBuilder fields=new StringBuilder();for(int i=0;i<20;i++)fields.append(",a").append(i);ARG_COLUMNS=fields.toString();}
    /** Root's migration owner appends this exact DDL; callers never execute it during research. */
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_evolution_publication(publication_id TEXT PRIMARY KEY,loop TEXT NOT NULL,task_id TEXT NOT NULL,owner_epoch TEXT NOT NULL,fence INTEGER NOT NULL,source_commit TEXT NOT NULL,old_cursor_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,op_count INTEGER NOT NULL,value_count INTEGER NOT NULL,logical_bytes INTEGER NOT NULL,reserved_bytes INTEGER NOT NULL DEFAULT -1,old_snapshot_bytes INTEGER NOT NULL DEFAULT -1,snapshot_bytes INTEGER NOT NULL,snapshot_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT -1,step INTEGER NOT NULL DEFAULT -1,rng_state INTEGER NOT NULL DEFAULT 0,envelope_json TEXT NOT NULL DEFAULT '{}',state TEXT NOT NULL CHECK(state IN ('PREPARING','COMPLETE','ABANDONED')),created_at INTEGER NOT NULL,completed_at INTEGER)",
        "CREATE INDEX IF NOT EXISTS r31_evolution_publication_loop ON r31_evolution_publication(loop,state,created_at)",
        "CREATE TABLE IF NOT EXISTS r31_evolution_op(publication_id TEXT NOT NULL,op_seq INTEGER NOT NULL,kind INTEGER NOT NULL,chunk_mask INTEGER NOT NULL,blob_mask INTEGER NOT NULL"+ARG_COLUMNS+",key0,key1,key2,key3,row_hash TEXT NOT NULL,PRIMARY KEY(publication_id,op_seq))",
        "CREATE INDEX IF NOT EXISTS r31_evolution_op_lookup ON r31_evolution_op(publication_id,kind,key0,key1,key2,key3,op_seq)",
        "CREATE TABLE IF NOT EXISTS r31_evolution_value(publication_id TEXT NOT NULL,op_seq INTEGER NOT NULL,arg_index INTEGER NOT NULL,chunk_seq INTEGER NOT NULL,payload BLOB NOT NULL,chunk_hash TEXT NOT NULL,PRIMARY KEY(publication_id,op_seq,arg_index,chunk_seq))",
        "CREATE TRIGGER IF NOT EXISTS r31_evolution_manifest_immutable BEFORE UPDATE ON r31_evolution_publication WHEN NEW.publication_id IS NOT OLD.publication_id OR NEW.loop IS NOT OLD.loop OR NEW.task_id IS NOT OLD.task_id OR NEW.owner_epoch IS NOT OLD.owner_epoch OR NEW.fence IS NOT OLD.fence OR NEW.source_commit IS NOT OLD.source_commit OR NEW.old_cursor_hash IS NOT OLD.old_cursor_hash OR NEW.payload_hash IS NOT OLD.payload_hash OR NEW.op_count IS NOT OLD.op_count OR NEW.value_count IS NOT OLD.value_count OR NEW.logical_bytes IS NOT OLD.logical_bytes OR NEW.reserved_bytes IS NOT OLD.reserved_bytes OR NEW.old_snapshot_bytes IS NOT OLD.old_snapshot_bytes OR NEW.snapshot_bytes IS NOT OLD.snapshot_bytes OR NEW.snapshot_hash IS NOT OLD.snapshot_hash OR NEW.dataset_hash IS NOT OLD.dataset_hash OR NEW.generation IS NOT OLD.generation OR NEW.step IS NOT OLD.step OR NEW.rng_state IS NOT OLD.rng_state OR NEW.envelope_json IS NOT OLD.envelope_json OR NEW.created_at IS NOT OLD.created_at OR (OLD.state='COMPLETE' AND NEW.state<>'COMPLETE') BEGIN SELECT RAISE(ABORT,'R31_EVOLUTION_MANIFEST_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_evolution_manifest_protected BEFORE DELETE ON r31_evolution_publication WHEN OLD.state<>'ABANDONED' BEGIN SELECT RAISE(ABORT,'R31_EVOLUTION_MANIFEST_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_evolution_op_immutable BEFORE UPDATE ON r31_evolution_op BEGIN SELECT RAISE(ABORT,'R31_EVOLUTION_OP_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_evolution_value_immutable BEFORE UPDATE ON r31_evolution_value BEGIN SELECT RAISE(ABORT,'R31_EVOLUTION_VALUE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_evolution_op_protected BEFORE DELETE ON r31_evolution_op WHEN coalesce((SELECT state FROM r31_evolution_publication WHERE publication_id=OLD.publication_id),'MISSING') NOT IN ('COMPLETE','ABANDONED') BEGIN SELECT RAISE(ABORT,'R31_EVOLUTION_OP_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_evolution_value_protected BEFORE DELETE ON r31_evolution_value WHEN NOT EXISTS(SELECT 1 FROM r31_evolution_publication p WHERE p.publication_id=OLD.publication_id AND (p.state='ABANDONED' OR (p.state='COMPLETE' AND OLD.op_seq>=0))) BEGIN SELECT RAISE(ABORT,'R31_EVOLUTION_VALUE_PROTECTED'); END"
    };
    public static final class CursorState {
        public final String loop,dataset,snapshot;public final long generation,step,rng,updatedAt;public final boolean exists;
        private CursorState(String loop,Cursor row){this.loop=loop;exists=row!=null;generation=row==null?-1:row.getLong(0);step=row==null?-1:row.getLong(1);dataset=row==null?"":row.getString(2);rng=row==null?0:row.getLong(3);snapshot=row==null?"":row.getString(4);updatedAt=row==null?0:row.getLong(5);}
        public static CursorState capture(SQLiteDatabase read,String loop){try(Cursor c=read.rawQuery("SELECT generation,step,dataset_hash,rng_state,snapshot,updated_at FROM r27_evolution_cursor WHERE loop=?",new String[]{loop})){return new CursorState(loop,c.moveToFirst()?c:null);}}
        public String hash(){return StatsR27.sha256(JsonR27.write(Arrays.asList(loop,exists,generation,step,dataset,rng,snapshot,updatedAt)));}
    }
    private static final class Value {
        final int sequence,argument,chunk;final byte[] bytes;final String hash;
        Value(int sequence,int argument,int chunk,byte[] bytes){this.sequence=sequence;this.argument=argument;this.chunk=chunk;this.bytes=bytes;hash=StatsR27.sha256(bytes);}
    }
    private static final class Row {
        final int sequence,kind;final Object[] args,keys;final long chunkMask,blobMask;final String hash;
        Row(int sequence,int kind,Object[] args,Object[] keys,long chunkMask,long blobMask){this.sequence=sequence;this.kind=kind;this.args=args;this.keys=keys;this.chunkMask=chunkMask;this.blobMask=blobMask;Object[] combined=Arrays.copyOf(args,args.length+keys.length);System.arraycopy(keys,0,combined,args.length,keys.length);hash=rowHash(sequence,kind,combined,chunkMask,blobMask);}
    }
    public static final class Prepared implements AutoCloseable {
        private final MemoryBudgetR31.Lease memory;private boolean closed;
        public final String id,loop,owner,task,source,payloadHash;public final long fence,bindLimit;
        public final int originalOps,logicalBytes,snapshotBytes,callbackWriteUpperBound,canonicalRowWriteUpperBound;
        public final Map<String,Object> envelope;
        private final CursorState old;private final List<Row> rows;private final List<Value> values;
        private final EvolutionPublicationSqlR31.Plan plan;
        private final ResearchWriteQueueR27.Batch original;
        private final String snapshotHash,targetDataset,oldCursorHash;private final long preparedAt,targetGeneration,targetStep,targetRng,reservedBytes,oldSnapshotBytes;
        private boolean verified,completeObserved;
        private Prepared(SQLiteDatabase read,String loop,RuntimeCoordinatorR26.Claim claim,String source,
                         CursorState old,ResearchWriteQueueR27.Batch logical,int promotions,MemoryBudgetR31.Lease memory){
            this.memory=memory;
            if(claim==null||!"r27-evolution".equals(claim.task)||loop==null||!loop.equals(old.loop)||source==null||promotions<0||promotions>32)throw new IllegalArgumentException("R31_EVOLUTION_OWNER_INPUT");
            this.loop=loop;this.owner=claim.owner;this.task=claim.task;fence=claim.fence;this.source=source;this.old=old;oldCursorHash=old.hash();
            requireSchema(read);bindLimit=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);if(bindLimit<16384)throw new Capacity("BIND_CAPACITY_TOO_SMALL");
            original=new ResearchWriteQueueR27.Batch();for(ResearchWriteQueueR27.Op op:logical.ops)original.ops.add(new ResearchWriteQueueR27.Op(op.sql,op.args));original.before=logical.before;original.after=logical.after;
            ResearchWriteQueueR27.ImmutableBatch immutable=original.prepareR31(ABI,"prepared",task,owner,fence,oldCursorHash,0,ABI);
            payloadHash=immutable.payloadHash;originalOps=immutable.opCount;if(originalOps>MAX_OPS||immutable.bindBytes>MAX_LOGICAL_BYTES)throw new Capacity("OWNER_LOGICAL_ENVELOPE:"+originalOps+":"+immutable.bindBytes);
            logicalBytes=(int)immutable.bindBytes;id=StatsR27.sha256(ABI+'|'+loop+'|'+oldCursorHash+'|'+payloadHash+'|'+source+'|'+owner+'|'+fence);
            int cursor=-1;String snapshot=null;for(int i=0;i<original.ops.size();i++)if(original.ops.get(i).sql.startsWith("INSERT OR REPLACE INTO r27_evolution_cursor(")){if(cursor>=0)throw new IllegalArgumentException("R31_EVOLUTION_MULTIPLE_CURSORS");cursor=i;snapshot=(String)original.ops.get(i).args[5];if(!loop.equals(original.ops.get(i).args[0]))throw new IllegalArgumentException("R31_EVOLUTION_CURSOR_LOOP");}
            if(cursor<0||snapshot==null)throw new IllegalArgumentException("R31_EVOLUTION_CURSOR_REQUIRED");
            snapshotBytes=snapshot.getBytes(StandardCharsets.UTF_8).length;snapshotHash=StatsR27.sha256(snapshot);preparedAt=System.currentTimeMillis();
            ResearchWriteQueueR27.Op cursorOp=original.ops.get(cursor);targetDataset=(String)cursorOp.args[3];targetGeneration=((Number)cursorOp.args[1]).longValue();targetStep=((Number)cursorOp.args[2]).longValue();targetRng=((Number)cursorOp.args[4]).longValue();Object[] cursorArgs=cursorOp.args.clone();cursorArgs[5]=SNAPSHOT_PREFIX+id;original.ops.set(cursor,new ResearchWriteQueueR27.Op(cursorOp.sql,cursorArgs));
            plan=new EvolutionPublicationSqlR31.Plan(read,original.ops);if(plan.shapes.size()>MAX_SHAPES)throw new Capacity("OWNER_SQL_SHAPES:"+plan.shapes.size());
            rows=new ArrayList<>();values=new ArrayList<>();chunks(values,-1,0,snapshot.getBytes(StandardCharsets.UTF_8));chunks(values,-2,0,old.snapshot.getBytes(StandardCharsets.UTF_8));
            int consume=0;for(int sequence=0;sequence<original.ops.size();sequence++){
                ResearchWriteQueueR27.Op op=original.ops.get(sequence);Object[] args=op.args.clone();long chunkMask=0,blobMask=0;
                if(op.sql.startsWith("INSERT INTO r30r1_consume "))consume++;
                for(int argument=0;argument<args.length;argument++)if(args[argument] instanceof String||args[argument] instanceof byte[]){byte[] bytes=args[argument] instanceof byte[]?(byte[])args[argument]:((String)args[argument]).getBytes(StandardCharsets.UTF_8);if(bytes.length>CHUNK_BYTES){chunkMask|=1L<<argument;if(args[argument] instanceof byte[])blobMask|=1L<<argument;chunks(values,sequence,argument,bytes);args[argument]=null;}}
                plan.shapes.get(plan.kinds.get(sequence)).chunkedArguments|=chunkMask;
                rows.add(new Row(sequence,plan.kinds.get(sequence),args,plan.shapes.get(plan.kinds.get(sequence)).lookup(op),chunkMask,blobMask));
            }
            oldSnapshotBytes=utf8Bytes(old.snapshot);reservedBytes=logicalBytes+oldSnapshotBytes+1024L*rows.size()+256L*values.size();
            Map<String,Object> capacity=capacitySnapshot(read);
            if(((Number)capacity.get("manifestRows")).longValue()>=MAX_PRIVATE_MANIFESTS)throw new Capacity("BLOCKED_PROTECTED_MANIFEST_DEBT");
            if(((Number)capacity.get("scratchRowsAndReservations")).longValue()+rows.size()+values.size()>MAX_PRIVATE_SCRATCH_ROWS||((Number)capacity.get("scratchEncodedBytesAndReservations")).longValue()+reservedBytes>MAX_PRIVATE_SCRATCH_BYTES)throw new Capacity("WAITING_PRIVATE_SCRATCH_CLEANUP:"+JsonR27.write(capacity));
            if(((Number)capacity.get("checkpointPayloadBytesAndReservations")).longValue()+snapshotBytes+oldSnapshotBytes>MAX_PRIVATE_CHECKPOINT_BYTES)throw new Capacity("BLOCKED_PROTECTED_BYTES:"+JsonR27.write(capacity));
            callbackWriteUpperBound=4*consume+5*promotions;canonicalRowWriteUpperBound=originalOps+callbackWriteUpperBound+8;
            Map<String,Object> measured=new LinkedHashMap<>();measured.put("originalOps",originalOps);measured.put("newPrivateEncodedReservation",reservedBytes);measured.put("existingPrivateCapacity",capacity);measured.put("setBasedStatements",plan.shapes.size());measured.put("originalBindBytes",logicalBytes);measured.put("checkpointBytesStagedNotFinalBind",snapshotBytes);measured.put("canonicalCandidateRowWriteUpperBound",canonicalRowWriteUpperBound);measured.put("callbackWriteUpperBound",callbackWriteUpperBound);measured.put("callbackReadUpperBound",8*originalOps+40*promotions);measured.put("stageRows",rows.size());measured.put("valueChunks",values.size());measured.put("bindLimit",bindLimit);measured.put("activeCanonicalInsertUpdateTriggers","NONE_EXCEPT_ROW_LOCAL_ABORT_CONSTRAINTS");measured.put("implicitReplaceDeleteTriggers","INACTIVE_RECURSIVE_TRIGGERS_OFF_CHECKED");measured.put("conservativeWalEnvelopeBytes",Math.addExact(8L*logicalBytes,16384L*(canonicalRowWriteUpperBound+rows.size()+values.size())));measured.put("androidHoldP99","NOT_MEASURED");measured.put("callbackScope","ORIGINAL_GUARDS_PRESERVED_NOT_REPLACED_BY_DOCUMENTS");envelope=Collections.unmodifiableMap(measured);
        }
        public List<ResearchWriteQueueR27.Batch> stagingBatches(){
            if(closed)throw new java.util.concurrent.CancellationException("R31_EVOLUTION_PREPARED_CLOSED");if(completeObserved)return Collections.emptyList();
            List<ResearchWriteQueueR27.Batch> result=new ArrayList<>();ResearchWriteQueueR27.Batch current=new ResearchWriteQueueR27.Batch();
            current.add("INSERT OR IGNORE INTO r31_evolution_publication(publication_id,loop,task_id,owner_epoch,fence,source_commit,old_cursor_hash,payload_hash,op_count,value_count,logical_bytes,reserved_bytes,old_snapshot_bytes,snapshot_bytes,snapshot_hash,dataset_hash,generation,step,rng_state,envelope_json,state,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PREPARING',?)",id,loop,task,owner,fence,source,oldCursorHash,payloadHash,rows.size(),values.size(),logicalBytes,reservedBytes,oldSnapshotBytes,snapshotBytes,snapshotHash,targetDataset,targetGeneration,targetStep,targetRng,JsonR27.write(envelope),preparedAt);
            List<ResearchWriteQueueR27.Op> operations=new ArrayList<>();
            for(Row row:rows){Object[] args=new Object[30];args[0]=id;args[1]=row.sequence;args[2]=row.kind;args[3]=row.chunkMask;args[4]=row.blobMask;System.arraycopy(row.args,0,args,5,row.args.length);System.arraycopy(row.keys,0,args,25,4);args[29]=row.hash;operations.add(new ResearchWriteQueueR27.Op("INSERT OR IGNORE INTO r31_evolution_op(publication_id,op_seq,kind,chunk_mask,blob_mask"+ARG_COLUMNS+",key0,key1,key2,key3,row_hash) SELECT "+slots(30)+" FROM r31_evolution_publication WHERE publication_id=? AND state='PREPARING'",append(args,id)));}
            for(Value value:values)operations.add(new ResearchWriteQueueR27.Op("INSERT OR IGNORE INTO r31_evolution_value(publication_id,op_seq,arg_index,chunk_seq,payload,chunk_hash) SELECT ?,?,?,?,?,? FROM r31_evolution_publication WHERE publication_id=? AND state='PREPARING'",id,value.sequence,value.argument,value.chunk,value.bytes,value.hash,id));
            long currentBytes=bindBytes(current.ops.get(0));
            for(ResearchWriteQueueR27.Op op:operations){long added=bindBytes(op);if(current.ops.size()>=64||currentBytes+added>bindLimit){finishStage(result,current);current=new ResearchWriteQueueR27.Batch();currentBytes=0;}current.ops.add(op);currentBytes+=added;}
            if(!current.ops.isEmpty())finishStage(result,current);return Collections.unmodifiableList(result);
        }
        private void finishStage(List<ResearchWriteQueueR27.Batch> result,ResearchWriteQueueR27.Batch batch){
            ResearchWriteQueueR27.ImmutableBatch size=batch.prepareR31(ABI,"size",task,owner,fence,id,0,ABI);if(size.opCount>64||size.bindBytes>bindLimit)throw new Capacity("STAGING_SLICE_TOO_LARGE");
            batch.guardBefore(db->{if(completeMatch(db)){currentTaskFence(db);return 0;}owned(db);CursorState now=CursorState.capture(db,loop);if(!sameCursor(now,old))throw new Stale();return 0;});
            batch.guardAfter(db->{try(Cursor c=db.rawQuery("SELECT owner_epoch,fence,payload_hash,state FROM r31_evolution_publication WHERE publication_id=?",new String[]{id})){if(!c.moveToFirst()||!owner.equals(c.getString(0))||fence!=c.getLong(1)||!payloadHash.equals(c.getString(2))||!("PREPARING".equals(c.getString(3))||"COMPLETE".equals(c.getString(3))&&completeMatch(db)))throw new Stale();}return 0;});result.add(batch);
        }
        /** Full hash/argument verification runs on the read connection, outside the writer. */
        public void verifyPrepared(SQLiteDatabase read){
            if(closed)throw new java.util.concurrent.CancellationException("R31_EVOLUTION_PREPARED_CLOSED");
            if(read.inTransaction())throw new IllegalStateException("R31_EVOLUTION_VERIFY_OUTSIDE_TRANSACTION");
            if(completeMatch(read)){String restored=resolveSnapshot(read,SNAPSHOT_PREFIX+id);if(!snapshotHash.equals(StatsR27.sha256(restored)))throw new IllegalStateException("R31_EVOLUTION_COMPLETE_SNAPSHOT_MISMATCH");completeObserved=true;verified=true;return;}
            owned(read);
            try(Cursor c=read.rawQuery("SELECT op_seq,kind,chunk_mask,blob_mask"+ARG_COLUMNS+",key0,key1,key2,key3,row_hash FROM r31_evolution_op WHERE publication_id=? ORDER BY op_seq",new String[]{id})){
                for(Row row:rows){if(!c.moveToNext()||c.getInt(0)!=row.sequence||c.getInt(1)!=row.kind||c.getLong(2)!=row.chunkMask||c.getLong(3)!=row.blobMask||!row.hash.equals(c.getString(28)))throw new IllegalStateException("R31_EVOLUTION_STAGE_ROW_CORRUPT");for(int i=0;i<20;i++)if(!cellEquals(c,4+i,i<row.args.length?row.args[i]:null))throw new IllegalStateException("R31_EVOLUTION_STAGE_BIND_CORRUPT");for(int i=0;i<4;i++)if(!cellEquals(c,24+i,row.keys[i]))throw new IllegalStateException("R31_EVOLUTION_STAGE_KEY_CORRUPT");}if(c.moveToNext())throw new IllegalStateException("R31_EVOLUTION_EXTRA_STAGE_ROWS");}
            int index=0;try(Cursor c=read.rawQuery("SELECT op_seq,arg_index,chunk_seq,payload,chunk_hash FROM r31_evolution_value WHERE publication_id=? ORDER BY op_seq,arg_index,chunk_seq",new String[]{id})){
                List<Value> ordered=new ArrayList<>(values);ordered.sort(Comparator.comparingInt((Value v)->v.sequence).thenComparingInt(v->v.argument).thenComparingInt(v->v.chunk));
                while(c.moveToNext()){if(index>=ordered.size())throw new IllegalStateException("R31_EVOLUTION_EXTRA_VALUES");Value value=ordered.get(index++);byte[] bytes=c.getBlob(3);if(value.sequence!=c.getInt(0)||value.argument!=c.getInt(1)||value.chunk!=c.getInt(2)||!value.hash.equals(c.getString(4))||!value.hash.equals(StatsR27.sha256(bytes))||!Arrays.equals(value.bytes,bytes))throw new IllegalStateException("R31_EVOLUTION_VALUE_CORRUPT");}}
            if(index!=values.size())throw new IllegalStateException("R31_EVOLUTION_MISSING_VALUES");verified=true;
        }
        public ResearchWriteQueueR27.Batch publicationBatch(){
            if(closed)throw new java.util.concurrent.CancellationException("R31_EVOLUTION_PREPARED_CLOSED");
            if(!verified)throw new IllegalStateException("R31_EVOLUTION_UNVERIFIED_STAGING");if(completeObserved)return new ResearchWriteQueueR27.Batch();ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
            batch.guardBefore(db->{owned(db);requireSchema(db);CursorState now=CursorState.capture(db,loop);if(!sameCursor(now,old))throw new Stale();
                try(Cursor c=db.rawQuery("SELECT state,payload_hash,op_count,value_count FROM r31_evolution_publication WHERE publication_id=?",new String[]{id})){if(!c.moveToFirst()||!"PREPARING".equals(c.getString(0))||!payloadHash.equals(c.getString(1))||c.getInt(2)!=rows.size()||c.getInt(3)!=values.size())throw new Stale();}
                if(scalar(db,"SELECT count(*) FROM r31_evolution_op WHERE publication_id=?",id)!=rows.size()||scalar(db,"SELECT count(*) FROM r31_evolution_value WHERE publication_id=?",id)!=values.size())throw new IllegalStateException("R31_EVOLUTION_INCOMPLETE_STAGING");
                return original.before==null?0:original.before.call(db);});
            for(int kind:plan.ordered)batch.add(plan.shapes.get(kind).compile(id,kind));
            batch.add("UPDATE r31_evolution_publication SET state='COMPLETE',completed_at=CAST((julianday('now')-2440587.5)*86400000 AS INTEGER) WHERE publication_id=? AND state='PREPARING'",id);
            batch.guardAfter(db->{if(original.after!=null){Integer result=original.after.call(db);if(result!=null&&result<0)return result;}try(Cursor c=db.rawQuery("SELECT snapshot FROM r27_evolution_cursor WHERE loop=?",new String[]{loop})){if(!c.moveToFirst()||!(SNAPSHOT_PREFIX+id).equals(c.getString(0)))throw new IllegalStateException("R31_EVOLUTION_CURSOR_PUBLICATION_MISSING");}return 0;});
            ResearchWriteQueueR27.ImmutableBatch check=batch.prepareR31(ABI,"publication",task,owner,fence,id,0,ABI);if(check.overInitialBudget||check.bindBytes>bindLimit)throw new Capacity("PUBLICATION_BATCH_ENVELOPE");return batch;
        }
        private boolean completeMatch(SQLiteDatabase db){
            try(Cursor c=db.rawQuery("SELECT state,payload_hash,snapshot_hash,dataset_hash,generation,step,rng_state,loop,task_id,owner_epoch,fence,source_commit,old_cursor_hash FROM r31_evolution_publication WHERE publication_id=?",new String[]{id})){
                if(!c.moveToFirst()||!"COMPLETE".equals(c.getString(0)))return false;
                if(!payloadHash.equals(c.getString(1))||!snapshotHash.equals(c.getString(2))||!targetDataset.equals(c.getString(3))||targetGeneration!=c.getLong(4)||targetStep!=c.getLong(5)||targetRng!=c.getLong(6)||!loop.equals(c.getString(7))||!task.equals(c.getString(8))||!owner.equals(c.getString(9))||fence!=c.getLong(10)||!source.equals(c.getString(11))||!oldCursorHash.equals(c.getString(12)))throw new IllegalStateException("R31_EVOLUTION_COMPLETE_MANIFEST_MISMATCH");
            }
            CursorState current=CursorState.capture(db,loop);if(!current.exists||!(SNAPSHOT_PREFIX+id).equals(current.snapshot)||current.generation!=targetGeneration||current.step!=targetStep||current.rng!=targetRng||!targetDataset.equals(current.dataset))throw new Stale();return true;
        }
        @Override public void close(){if(!closed){closed=true;rows.clear();values.clear();original.ops.clear();memory.close();}}
        private void owned(SQLiteDatabase db){if(closed)throw new java.util.concurrent.CancellationException("R31_EVOLUTION_PREPARED_CLOSED");RuntimeCoordinatorR26.Claim active=RuntimeCoordinatorR26.currentClaimR30();if(active==null||!task.equals(active.task)||!owner.equals(active.owner)||fence!=active.fence)throw new Stale();RuntimeCoordinatorR26.requireFence(db,active);}
    }
    public static Prepared prepare(SQLiteDatabase read,String loop,RuntimeCoordinatorR26.Claim claim,String source,
                                   CursorState old,ResearchWriteQueueR27.Batch logical,int promotionCount){
        if(read.inTransaction())throw new IllegalStateException("R31_EVOLUTION_PREPARE_OUTSIDE_TRANSACTION");
        if(logical==null||old==null||claim==null)throw new IllegalArgumentException("R31_EVOLUTION_PREPARED_INPUT");
        long bytes=utf8Bytes(old.snapshot);for(ResearchWriteQueueR27.Op op:logical.ops)if(op.args!=null)for(Object arg:op.args)bytes+=arg instanceof String?utf8Bytes((String)arg):arg instanceof byte[]?((byte[])arg).length:8;
        if(bytes>2L*MAX_LOGICAL_BYTES||logical.ops.size()>MAX_OPS)throw new Capacity("PREPARED_MEMORY_ENVELOPE");
        long declared=Math.addExact(8*bytes,4096L*logical.ops.size()+65536);
        MemoryBudgetR31.Admission admitted=MemoryBudgetR31.processBudget().tryAcquire("evolution-publication:"+loop,claim.fence,MemoryBudgetR31.Kind.CHECKPOINT,declared,()->Thread.currentThread().isInterrupted());
        if(!admitted.acquired())throw new MemoryBudgetR31.CapacityWait(admitted.reason);
        boolean adopted=false;try{Prepared result=new Prepared(read,loop,claim,source,old,logical,promotionCount,admitted.lease);adopted=true;return result;}finally{if(!adopted)admitted.lease.close();}
    }
    private static long utf8Bytes(String text){long bytes=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c<128)bytes++;else if(c<2048)bytes+=2;else if(Character.isHighSurrogate(c)&&i+1<text.length()&&Character.isLowSurrogate(text.charAt(i+1))){bytes+=4;i++;}else bytes+=Character.isSurrogate(c)?1:3;}return bytes;}
    /** Startup migration hook; the sole writer/SchemaDomain supplies the MIGRATION context. */
    public static void ensureSchemaR31(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_EVOLUTION_MIGRATION_TRANSACTION_REQUIRED");for(String ddl:DDL)db.execSQL(ddl);Set<String> columns=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info(r31_evolution_publication)",null)){while(c.moveToNext())columns.add(c.getString(1));}for(String addition:new String[]{"reserved_bytes INTEGER NOT NULL DEFAULT -1","old_snapshot_bytes INTEGER NOT NULL DEFAULT -1","dataset_hash TEXT NOT NULL DEFAULT ''","generation INTEGER NOT NULL DEFAULT -1","step INTEGER NOT NULL DEFAULT -1","rng_state INTEGER NOT NULL DEFAULT 0","envelope_json TEXT NOT NULL DEFAULT '{}'"})if(!columns.contains(addition.substring(0,addition.indexOf(' '))))db.execSQL("ALTER TABLE r31_evolution_publication ADD COLUMN "+addition);requireSchema(db);}
    public static String[][] schemaObjectsR31(){return new String[][]{{"table","r31_evolution_publication"},{"table","r31_evolution_op"},{"table","r31_evolution_value"},{"index","r31_evolution_publication_loop"},{"index","r31_evolution_op_lookup"},{"trigger","r31_evolution_manifest_immutable"},{"trigger","r31_evolution_manifest_protected"},{"trigger","r31_evolution_op_immutable"},{"trigger","r31_evolution_value_immutable"},{"trigger","r31_evolution_op_protected"},{"trigger","r31_evolution_value_protected"}};}
    /** Legacy inline cursors remain readable. New cursors resolve only a COMPLETE immutable bundle. */
    public static String resolveSnapshot(SQLiteDatabase read,String stored){
        if(stored==null||!stored.startsWith(SNAPSHOT_PREFIX))return stored;String id=stored.substring(SNAPSHOT_PREFIX.length());if(!id.matches("[0-9a-f]{64}"))throw new IllegalStateException("R31_EVOLUTION_SNAPSHOT_REFERENCE");
        int length;String hash;try(Cursor c=read.rawQuery("SELECT snapshot_bytes,snapshot_hash,state FROM r31_evolution_publication WHERE publication_id=?",new String[]{id})){if(!c.moveToFirst()||!"COMPLETE".equals(c.getString(2)))throw new IllegalStateException("R31_EVOLUTION_SNAPSHOT_NOT_COMPLETE");length=c.getInt(0);hash=c.getString(1);}
        if(length<0||length>MAX_LOGICAL_BYTES)throw new IllegalStateException("R31_EVOLUTION_SNAPSHOT_CAPACITY");ByteArrayOutputStream out=new ByteArrayOutputStream(length);int sequence=0;
        try(Cursor c=read.rawQuery("SELECT chunk_seq,payload,chunk_hash FROM r31_evolution_value WHERE publication_id=? AND op_seq=-1 AND arg_index=0 ORDER BY chunk_seq",new String[]{id})){while(c.moveToNext()){byte[] bytes=c.getBlob(1);if(c.getInt(0)!=sequence++||bytes.length>CHUNK_BYTES||out.size()+bytes.length>length||!StatsR27.sha256(bytes).equals(c.getString(2)))throw new IllegalStateException("R31_EVOLUTION_SNAPSHOT_CHUNK_CORRUPT");out.write(bytes,0,bytes.length);}}
        byte[] bytes=out.toByteArray();if(bytes.length!=length||!StatsR27.sha256(bytes).equals(hash))throw new IllegalStateException("R31_EVOLUTION_SNAPSHOT_CORRUPT");return new String(bytes,StandardCharsets.UTF_8);
    }
    public static String publicationToken(SQLiteDatabase read,String loop){try(Cursor c=read.rawQuery("SELECT snapshot,generation,step,dataset_hash,rng_state,updated_at FROM r27_evolution_cursor WHERE loop=?",new String[]{loop})){if(!c.moveToFirst())return "ABSENT";String snapshot=c.getString(0);if(snapshot.startsWith(SNAPSHOT_PREFIX))return snapshot;return "LEGACY:"+c.getLong(1)+":"+c.getLong(2)+":"+c.getString(3)+":"+c.getLong(4)+":"+c.getLong(5)+":"+StatsR27.sha256(snapshot);}}
    /** Local encoded-owner ceilings, not measurements of SQLite pages, WAL, filesystem or shared model quota. */
    public static Map<String,Object> capacitySnapshot(SQLiteDatabase read){
        requireSchema(read);Map<String,Object> out=new LinkedHashMap<>();long reservedRows=0,reserved=0,reservedCheckpoints=0;
        try(Cursor c=read.rawQuery("SELECT op_count,value_count,reserved_bytes,snapshot_bytes,old_snapshot_bytes FROM r31_evolution_publication WHERE state='PREPARING'",null)){while(c.moveToNext()){if(c.getLong(2)<0||c.getLong(4)<0)throw new Capacity("UNKNOWN_INHERITED_PRIVATE_RESERVATION");reservedRows+=c.getLong(0)+c.getLong(1);reserved+=c.getLong(2);reservedCheckpoints+=c.getLong(3)+c.getLong(4);}}
        long rows=scalar(read,"SELECT count(*) FROM r31_evolution_op o JOIN r31_evolution_publication p ON p.publication_id=o.publication_id WHERE p.state<>'PREPARING'");
        long encoded=0;StringBuilder expression=new StringBuilder("256");for(int i=0;i<20;i++)expression.append("+coalesce(length(CAST(a").append(i).append(" AS BLOB)),0)+16");
        try(Cursor c=read.rawQuery("SELECT coalesce(sum("+expression+"),0) FROM r31_evolution_op o JOIN r31_evolution_publication p ON p.publication_id=o.publication_id WHERE p.state<>'PREPARING'",null)){if(c.moveToFirst())encoded=c.getLong(0);}
        rows+=scalar(read,"SELECT count(*) FROM r31_evolution_value v JOIN r31_evolution_publication p ON p.publication_id=v.publication_id WHERE p.state<>'PREPARING' AND (v.op_seq>=0 OR p.state='ABANDONED')");
        encoded+=scalar(read,"SELECT coalesce(sum(length(payload)+256),0) FROM r31_evolution_value v JOIN r31_evolution_publication p ON p.publication_id=v.publication_id WHERE p.state<>'PREPARING' AND (v.op_seq>=0 OR p.state='ABANDONED')");
        long protectedBytes=scalar(read,"SELECT coalesce(sum(length(payload)),0) FROM r31_evolution_value v JOIN r31_evolution_publication p ON p.publication_id=v.publication_id WHERE p.state='COMPLETE' AND v.op_seq<0");
        out.put("scope","LOCAL_NEW_EVOLUTION_OWNER_ENCODED_PAYLOAD_NOT_PHYSICAL_DB_OR_SHARED_GLOBAL_QUOTA");out.put("manifestRows",scalar(read,"SELECT count(*) FROM r31_evolution_publication"));out.put("scratchRowsAndReservations",rows+reservedRows);out.put("scratchEncodedBytesAndReservations",encoded+reserved);out.put("checkpointPayloadBytesAndReservations",protectedBytes+reservedCheckpoints);out.put("scratchRowsCeiling",MAX_PRIVATE_SCRATCH_ROWS);out.put("scratchEncodedBytesCeiling",MAX_PRIVATE_SCRATCH_BYTES);out.put("checkpointPayloadBytesCeiling",MAX_PRIVATE_CHECKPOINT_BYTES);out.put("physicalDatabaseBytes",null);out.put("indexOverflowPageBytes",null);out.put("sharedGlobalModelQuota","NOT_COORDINATED_BY_THIS_LOCAL_COMPONENT");out.put("releaseCondition","Fair Health maintenance removes only completed or fenced scratch; checkpoint roots require P03/P06 explicit retirement, never implicit evidence deletion");return out;
    }
    /** Private SQL scratch only. A later maintenance slice owns/retries cleanup independently of business ACK. */
    public static ResearchWriteQueueR27.Batch cleanupSlice(SQLiteDatabase read,String loop,RuntimeCoordinatorR26.Claim maintenance){
        if(read.inTransaction())throw new IllegalStateException("R31_EVOLUTION_CLEANUP_PREPARE_OUTSIDE_TRANSACTION");
        if(maintenance==null||!Arrays.asList("r27-evolution","r27-health","r27-evolution-cleanup").contains(maintenance.task))throw new IllegalArgumentException("R31_EVOLUTION_MAINTENANCE_OWNER");
        requireSchema(read);String id,state,originalOwner,originalTask;long originalFence;
        String eligible="SELECT p.publication_id,p.state,p.owner_epoch,p.fence,p.task_id FROM r31_evolution_publication p WHERE p.loop=? AND ((p.state='COMPLETE' AND (EXISTS(SELECT 1 FROM r31_evolution_op o WHERE o.publication_id=p.publication_id) OR EXISTS(SELECT 1 FROM r31_evolution_value v WHERE v.publication_id=p.publication_id AND v.op_seq>=0))) OR p.state='ABANDONED' OR (p.state='PREPARING' AND NOT EXISTS(SELECT 1 FROM r26_task t WHERE t.task_id=p.task_id AND t.owner IS p.owner_epoch AND t.fence=p.fence))) ORDER BY p.created_at,p.publication_id LIMIT 1";
        try(Cursor c=read.rawQuery(eligible,new String[]{loop})){if(!c.moveToFirst())return null;id=c.getString(0);state=c.getString(1);originalOwner=c.getString(2);originalFence=c.getLong(3);originalTask=c.getString(4);}
        final String selected=id,expectedState=state,oldOwner=originalOwner,oldTask=originalTask;final long oldFence=originalFence;
        final String activeOwner=maintenance.owner,activeTask=maintenance.task;final long activeFence=maintenance.fence;
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        batch.guardBefore(db->{RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();if(claim==null||!activeOwner.equals(claim.owner)||!activeTask.equals(claim.task)||activeFence!=claim.fence)throw new Stale();RuntimeCoordinatorR26.requireFence(db,claim);
            try(Cursor c=db.rawQuery("SELECT state,owner_epoch,fence,task_id FROM r31_evolution_publication WHERE publication_id=?",new String[]{selected})){if(!c.moveToFirst()||!expectedState.equals(c.getString(0))||!oldOwner.equals(c.getString(1))||oldFence!=c.getLong(2)||!oldTask.equals(c.getString(3)))throw new Stale();}
            if(!"COMPLETE".equals(expectedState)){if(scalar(db,"SELECT count(*) FROM r27_evolution_cursor WHERE snapshot=?",SNAPSHOT_PREFIX+selected)!=0)throw new IllegalStateException("R31_EVOLUTION_REFERENCED_SNAPSHOT_PROTECTED");
                if(scalar(db,"SELECT count(*) FROM r26_task WHERE task_id=? AND owner IS ? AND fence=?",oldTask,oldOwner,String.valueOf(oldFence))!=0)throw new Stale();}return 0;});
        if("PREPARING".equals(state))batch.add("UPDATE r31_evolution_publication SET state='ABANDONED' WHERE publication_id=? AND state='PREPARING'",id);
        int selectedRows=0;
        try(Cursor c=read.rawQuery("SELECT op_seq FROM r31_evolution_op WHERE publication_id=? ORDER BY op_seq LIMIT 62",new String[]{id})){while(c.moveToNext()){batch.add("DELETE FROM r31_evolution_op WHERE publication_id=? AND op_seq=?",id,c.getLong(0));selectedRows++;}}
        String condition="COMPLETE".equals(state)?" AND op_seq>=0":"";
        try(Cursor c=read.rawQuery("SELECT op_seq,arg_index,chunk_seq FROM r31_evolution_value WHERE publication_id=?"+condition+" ORDER BY op_seq,arg_index,chunk_seq LIMIT "+(62-selectedRows),new String[]{id})){while(c.moveToNext()){batch.add("DELETE FROM r31_evolution_value WHERE publication_id=? AND op_seq=? AND arg_index=? AND chunk_seq=?",id,c.getLong(0),c.getLong(1),c.getLong(2));selectedRows++;}}
        if(selectedRows==0&&!"COMPLETE".equals(state))batch.add("DELETE FROM r31_evolution_publication WHERE publication_id=? AND state='ABANDONED'",id);
        if(batch.ops.size()>64)throw new IllegalStateException("R31_EVOLUTION_CLEANUP_SLICE_BOUND");return batch;
    }
    private static void currentTaskFence(SQLiteDatabase db){RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();if(claim==null||!"r27-evolution".equals(claim.task))throw new Stale();RuntimeCoordinatorR26.requireFence(db,claim);}
    private static boolean sameCursor(CursorState a,CursorState b){return a.exists==b.exists&&a.generation==b.generation&&a.step==b.step&&a.rng==b.rng&&a.updatedAt==b.updatedAt&&a.dataset.equals(b.dataset)&&a.snapshot.equals(b.snapshot);}
    private static void requireSchema(SQLiteDatabase db){com.ethk3.novaonepro.DatabaseCoordinator.requireSchemaObjectsR31(db,ABI,new String[][]{{"r31_evolution_publication","publication_id,loop,owner_epoch,fence,payload_hash,reserved_bytes,old_snapshot_bytes,snapshot_bytes,snapshot_hash,dataset_hash,generation,step,rng_state,envelope_json,state"},{"r31_evolution_op","publication_id,op_seq,kind,chunk_mask,blob_mask,a0,a19,key0,key3,row_hash"},{"r31_evolution_value","publication_id,op_seq,arg_index,chunk_seq,payload,chunk_hash"}},new String[0][]);for(String trigger:new String[]{"r31_evolution_manifest_immutable","r31_evolution_manifest_protected","r31_evolution_op_immutable","r31_evolution_value_immutable","r31_evolution_op_protected","r31_evolution_value_protected"})try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{trigger})){if(!c.moveToFirst())throw new IllegalStateException("R31_EVOLUTION_IMMUTABILITY_TRIGGER_MISSING:"+trigger);String expected=null;for(String ddl:DDL)if(ddl.startsWith("CREATE TRIGGER IF NOT EXISTS "+trigger+" "))expected=ddl;if(expected==null||!canonicalDdl(expected).equals(canonicalDdl(c.getString(0))))throw new IllegalStateException("R31_EVOLUTION_IMMUTABILITY_TRIGGER_MISMATCH:"+trigger);}}
    private static String canonicalDdl(String ddl){return ddl.replace(" IF NOT EXISTS "," ").trim().replaceAll("\\s+"," ");}
    private static void chunks(List<Value> values,int op,int argument,byte[] bytes){for(int offset=0,sequence=0;offset<bytes.length;offset+=CHUNK_BYTES,sequence++)values.add(new Value(op,argument,sequence,Arrays.copyOfRange(bytes,offset,Math.min(bytes.length,offset+CHUNK_BYTES))));}
    private static Object[] append(Object[] args,Object value){Object[] copy=Arrays.copyOf(args,args.length+1);copy[args.length]=value;return copy;}
    private static long bindBytes(ResearchWriteQueueR27.Op op){long bytes=0;if(op.args!=null)for(Object value:op.args)bytes+=value instanceof String?utf8Bytes((String)value):value instanceof byte[]?((byte[])value).length:8;return bytes;}
    private static String slots(int count){String[] values=new String[count];Arrays.fill(values,"?");return String.join(",",values);}
    private static long scalar(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R31_EVOLUTION_COUNT_MISSING");return c.getLong(0);}}
    private static boolean cellEquals(Cursor c,int column,Object value){if(value==null)return c.isNull(column);if(c.isNull(column))return false;if(value instanceof byte[])return Arrays.equals((byte[])value,c.getBlob(column));if(value instanceof Double||value instanceof Float)return Double.doubleToRawLongBits(((Number)value).doubleValue())==Double.doubleToRawLongBits(c.getDouble(column));if(value instanceof Number)return ((Number)value).longValue()==c.getLong(column);if(value instanceof Boolean)return ((Boolean)value?1:0)==c.getLong(column);return value.equals(c.getString(column));}
    private static String rowHash(int sequence,int kind,Object[] args,long chunks,long blobs){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(sequence);out.writeInt(kind);out.writeLong(chunks);out.writeLong(blobs);for(Object value:args){if(value==null)out.writeByte(0);else if(value instanceof byte[]){out.writeByte(1);out.writeInt(((byte[])value).length);out.write((byte[])value);}else{out.writeByte(2);out.writeUTF(value.getClass().getName());byte[] text=String.valueOf(value).getBytes(StandardCharsets.UTF_8);out.writeInt(text.length);out.write(text);}}out.close();return StatsR27.sha256(bytes.toByteArray());}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    public static final class Stale extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {public Stale(){super("R31_EVOLUTION_STALE_OWNER_OR_CURSOR");}public String failureCategory(){return "STALE_OWNER";}public String operation(){return ABI;}public String phase(){return "PRECOMMIT";}public String commitOutcomeName(){return "ROLLED_BACK";}}
    public static final class Capacity extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {public Capacity(String why){super("R31_EVOLUTION_CAPACITY:"+why);}public String failureCategory(){return "DEPENDENCY";}public String operation(){return ABI;}public String phase(){return "PREPARE";}public String commitOutcomeName(){return "NOT_SUBMITTED";}}
}
