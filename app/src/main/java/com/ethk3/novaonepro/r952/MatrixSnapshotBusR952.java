package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.DbHelper;

public final class MatrixSnapshotBusR952 {
    private MatrixSnapshotBusR952(){}
    public static String publish(final DbStore store,final String issue,final String featureHash,final String payloadHash){
        String committed=store.coordinatorForResearchR952().runAtomicWrite("R952_MATRIX_PUBLISH",new DatabaseCoordinator.WriteCallable<String>(){
            public String call(SQLiteDatabase db){return publish(db,issue,featureHash,payloadHash);}},null);
        if(committed==null)throw new IllegalStateException("R958_MATRIX_COMMIT_FAILED");return committed;
    }
    public static String publish(SQLiteDatabase db,String issue,String featureHash,String payloadHash){
        if(!TargetIssueResolver.isValid(issue)||featureHash==null||!featureHash.matches("[0-9a-f]{64}")||payloadHash==null||!payloadHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R958_MATRIX_IDENTITY");
        ResearchMigrationRegistryR952.ensure(db);String hash=V51ChecksumEngine.sha256("MATRIX1|"+issue+'|'+featureHash+'|'+payloadHash);
        try(Cursor existing=db.rawQuery("SELECT snapshot_hash,feature_hash,payload_hash,state FROM r952_matrix_snapshot WHERE target_issue=?",new String[]{issue})){
            if(existing.moveToFirst()){
                if(!hash.equals(existing.getString(0))||!featureHash.equals(existing.getString(1))||!payloadHash.equals(existing.getString(2))||!"PREDRAW_FROZEN".equals(existing.getString(3)))throw new IllegalStateException("R952_MATRIX_HASH_DRIFT");
                return hash;
            }
        }
        try(Cursor drawn=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{issue})){
            if(drawn.moveToFirst())throw new IllegalStateException("R958_MATRIX_POSTDRAW_CREATION_REJECTED");
        }
        db.execSQL("INSERT OR IGNORE INTO r952_matrix_snapshot(target_issue,snapshot_hash,feature_hash,payload_hash,state,created_at) VALUES(?,?,?,?,?,?)",new Object[]{issue,hash,featureHash,payloadHash,"PREDRAW_FROZEN",Long.valueOf(System.currentTimeMillis())});
        Cursor c=db.rawQuery("SELECT snapshot_hash FROM r952_matrix_snapshot WHERE target_issue=?",new String[]{issue});try{if(!c.moveToFirst()||!hash.equals(c.getString(0)))throw new IllegalStateException("R952_MATRIX_HASH_DRIFT");}finally{c.close();}return hash;
    }
    /** CP4 identity is stable across checkpoint versions; payloads never exceed 256 KiB. */
    public static final class CheckpointCP4 {
        public final String matrixId,matrixType,partitionKey,featureManifestHash,schemaHash,throughIssue,lastReceipt,payloadHash;
        public final int windowSize; public final long version,sampleCount; private final byte[] payload;
        public CheckpointCP4(String type,String partition,int window,String manifest,String schema,
                long version,String through,String receipt,long count,byte[] bytes) {
            if(type==null||type.isEmpty()||partition==null||partition.isEmpty()||window<1||window>5000
                    ||version<0||count<0||!TargetIssueResolver.isValid(through)||bytes==null||bytes.length>262144
                    ||!hashCP4(manifest)||!hashCP4(schema)||!hashCP4(receipt))throw new IllegalArgumentException("CP4_MATRIX_CONTRACT");
            matrixType=type;partitionKey=partition;windowSize=window;featureManifestHash=manifest;schemaHash=schema;
            this.version=version;throughIssue=through;lastReceipt=receipt;sampleCount=count;payload=bytes.clone();
            matrixId=V51ChecksumEngine.sha256("MATRIX_CP4|"+type+'|'+partition+'|'+window+'|'+manifest);
            payloadHash=bytesHashCP4(bytes);
        }
        public byte[] payload(){return payload.clone();}
    }
    /** A null return means absent/too-new checkpoint; caller schedules bounded PIT replay, never uses future data. */
    public static CheckpointCP4 readAtCP4(SQLiteDatabase db,String matrixId,String manifest,String target) {
        if(!TargetIssueResolver.isValid(target)||!hashCP4(matrixId)||!hashCP4(manifest))throw new IllegalArgumentException("CP4_MATRIX_READ");
        try(Cursor c=db.rawQuery("SELECT matrix_type,partition_key,window_size,feature_manifest_hash,schema_hash,version,through_issue,last_receipt,sample_count,payload,payload_hash,payload_codec FROM research_matrix_checkpoint_v2 WHERE matrix_id=? AND feature_manifest_hash=? AND through_issue<?",new String[]{matrixId,manifest,target})) {
            if(!c.moveToFirst())return null;
            if(!"BINARY_V1".equals(c.getString(11)))throw new IllegalStateException("CP4_MATRIX_CODEC");
            CheckpointCP4 found=new CheckpointCP4(c.getString(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getLong(5),c.getString(6),c.getString(7),c.getLong(8),c.getBlob(9));
            if(!matrixId.equals(found.matrixId)||!found.payloadHash.equals(c.getString(10)))throw new IllegalStateException("CP4_MATRIX_CORRUPT");
            return found;
        }
    }
    /** Caller owns short DatabaseCoordinator write transaction; numerical apply must already have completed. */
    public static boolean commitCP4(SQLiteDatabase db,CheckpointCP4 expected,CheckpointCP4 next,long now) {
        if(!db.inTransaction()||next==null)throw new IllegalStateException("CP4_MATRIX_TRANSACTION");
        // A payload hash does not authenticate its schema/header. Reject an incompatible
        // prepared identity before either the checkpoint or its consumption ACK is written.
        if(!MatrixProjectionCP12.SCHEMA_HASH.equals(next.schemaHash)
                ||expected!=null&&!next.schemaHash.equals(expected.schemaHash))
            throw new IllegalArgumentException("CP12_MATRIX_SCHEMA_MISMATCH");
        String consumer="MATRIX_CP4/"+next.matrixId;
        try(Cursor ack=db.rawQuery("SELECT result_hash,through_issue FROM research_projection_ack_v2 WHERE consumer=? AND source_hash=?",new String[]{consumer,next.lastReceipt})) {
            if(ack.moveToFirst()) {
                if(!next.payloadHash.equals(ack.getString(0))||!next.throughIssue.equals(ack.getString(1)))throw new IllegalStateException("CP4_MATRIX_ACK_DRIFT");
                return true;
            }
        }
        if(expected==null) {
            if(next.version!=0)throw new IllegalArgumentException("CP4_MATRIX_FIRST_VERSION");
            db.execSQL("INSERT OR IGNORE INTO research_matrix_checkpoint_v2(matrix_id,matrix_type,partition_key,window_size,feature_manifest_hash,schema_hash,version,through_issue,last_receipt,payload_codec,payload,payload_hash,sample_count,updated_at) VALUES(?,?,?,?,?,?,?,?,?,'BINARY_V1',?,?,?,?)",new Object[]{next.matrixId,next.matrixType,next.partitionKey,next.windowSize,next.featureManifestHash,next.schemaHash,next.version,next.throughIssue,next.lastReceipt,next.payload(),next.payloadHash,next.sampleCount,now});
        } else {
            if(!expected.matrixId.equals(next.matrixId)||next.version!=expected.version+1
                    ||TargetIssueResolver.compare(next.throughIssue,expected.throughIssue)<0)
                throw new IllegalArgumentException("CP4_MATRIX_VERSION_ORDER");
            // Header changes during off-writer preparation must lose the same CAS as
            // payload/version changes. Numerical decoding remains outside this transaction.
            db.execSQL("UPDATE research_matrix_checkpoint_v2 SET version=?,through_issue=?,last_receipt=?,payload=?,payload_hash=?,sample_count=?,updated_at=? WHERE matrix_id=? AND version=? AND payload_hash=? AND last_receipt=? AND through_issue=? AND matrix_type=? AND partition_key=? AND window_size=? AND feature_manifest_hash=? AND schema_hash=? AND sample_count=? AND payload_codec='BINARY_V1'",new Object[]{next.version,next.throughIssue,next.lastReceipt,next.payload(),next.payloadHash,next.sampleCount,now,expected.matrixId,expected.version,expected.payloadHash,expected.lastReceipt,expected.throughIssue,expected.matrixType,expected.partitionKey,expected.windowSize,expected.featureManifestHash,expected.schemaHash,expected.sampleCount});
        }
        try(Cursor changes=db.rawQuery("SELECT changes()",null)){if(!changes.moveToFirst()||changes.getInt(0)!=1)return false;}
        db.execSQL("INSERT INTO research_projection_ack_v2(consumer,source_hash,result_hash,through_issue,created_at) VALUES(?,?,?,?,?)",new Object[]{consumer,next.lastReceipt,next.payloadHash,next.throughIssue,now});
        return true;
    }
    private static boolean hashCP4(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static String bytesHashCP4(byte[] bytes) {
        try {byte[] result=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder(64);for(byte b:result){out.append(Character.forDigit((b>>>4)&15,16));out.append(Character.forDigit(b&15,16));}return out.toString();}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }

    public static void registerSpecCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec) {
        if(!db.inTransaction())throw new IllegalStateException("CP12_MATRIX_SPEC_TRANSACTION");
        db.execSQL("INSERT OR IGNORE INTO research_matrix_spec_cp12(matrix_id,matrix_type,partition_key,window_size,spec_manifest_hash,schema_hash) VALUES(?,?,?,?,?,?)",
            new Object[]{spec.matrixId,spec.type.name(),spec.partition,spec.window,spec.manifest,MatrixProjectionCP12.SCHEMA_HASH});
        try(Cursor c=db.rawQuery("SELECT matrix_type,partition_key,window_size,spec_manifest_hash,schema_hash FROM research_matrix_spec_cp12 WHERE matrix_id=?",new String[]{spec.matrixId})) {
            if(!c.moveToFirst()||!spec.type.name().equals(c.getString(0))||!spec.partition.equals(c.getString(1))||spec.window!=c.getInt(2)
                ||!spec.manifest.equals(c.getString(3))||!MatrixProjectionCP12.SCHEMA_HASH.equals(c.getString(4)))throw new IllegalStateException("CP12_MATRIX_SPEC_DRIFT");
        }
    }
    /** Only a frozen input producer calls this inside its receipt transaction. Never computes statistics here. */
    public static void enqueueCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec,String sourceIssue,long sourceOrdinal,
            String sourceReceiptHash,String proofHash,MatrixProjectionCP12.Sample sample,java.util.Map<String,String> components,long now) {
        if(!db.inTransaction()||!TargetIssueResolver.isValid(sourceIssue)||sourceOrdinal<0||sample.ordinal>sourceOrdinal
                ||!hashCP4(sourceReceiptHash)||!hashCP4(proofHash))throw new IllegalArgumentException("CP12_MATRIX_INPUT_IDENTITY");
        registerSpecCP12(db,spec);byte[] payload=MatrixProjectionCP12.sampleBytes(sample,components);
        String inputHash=MatrixProjectionCP12.hash("CP12_MATRIX_INPUT|"+spec.matrixId+'|'+sourceIssue+'|'+sourceOrdinal+'|'+sourceReceiptHash+'|'+proofHash+'|'+MatrixProjectionCP12.bytesHash(payload));
        try(Cursor existing=db.rawQuery("SELECT input_hash FROM research_matrix_input_cp12 WHERE matrix_id=? AND source_receipt_hash=?",new String[]{spec.matrixId,sourceReceiptHash})) {
            if(existing.moveToFirst()){if(!inputHash.equals(existing.getString(0)))throw new IllegalStateException("CP12_MATRIX_INPUT_DRIFT");return;}
        }
        try(Cursor count=db.rawQuery("SELECT COUNT(*) FROM research_matrix_input_cp12 WHERE state='READY'",null)) {
            if(!count.moveToFirst()||count.getLong(0)>=16384)throw new IllegalStateException("CP12_MATRIX_QUEUE_FULL");
        }
        db.execSQL("INSERT INTO research_matrix_input_cp12(matrix_id,source_receipt_hash,source_issue,source_ordinal,input_hash,proof_hash,input_payload,state,created_at,updated_at) VALUES(?,?,?,?,?,?,?,'READY',?,?)",
            new Object[]{spec.matrixId,sourceReceiptHash,sourceIssue,sourceOrdinal,inputHash,proofHash,payload,now,now});
    }
    public static final class PreparedProjectionCP12 {
        public final String matrixId,sourceReceiptHash,inputHash,sourceIssue;public final CheckpointCP4 expected,next;
        public final boolean alreadyApplied;public final AtomAdmissionCP12.Assessment atomAdmission;
        PreparedProjectionCP12(String matrixId,String receipt,String inputHash,String issue,CheckpointCP4 expected,CheckpointCP4 next,boolean already){this(matrixId,receipt,inputHash,issue,expected,next,already,null);}
        PreparedProjectionCP12(String matrixId,String receipt,String inputHash,String issue,CheckpointCP4 expected,CheckpointCP4 next,boolean already,AtomAdmissionCP12.Assessment atomAdmission){
            this.matrixId=matrixId;sourceReceiptHash=receipt;this.inputHash=inputHash;sourceIssue=issue;this.expected=expected;this.next=next;alreadyApplied=already;this.atomAdmission=atomAdmission;
        }
    }
    private static CheckpointCP4 latestCP12(SQLiteDatabase db,String matrixId) {
        try(Cursor c=db.rawQuery("SELECT matrix_type,partition_key,window_size,feature_manifest_hash,schema_hash,version,through_issue,last_receipt,sample_count,payload,payload_hash,payload_codec FROM research_matrix_checkpoint_v2 WHERE matrix_id=?",new String[]{matrixId})) {
            if(!c.moveToFirst())return null;
            if(!"BINARY_V1".equals(c.getString(11))||!MatrixProjectionCP12.SCHEMA_HASH.equals(c.getString(4)))throw new IllegalStateException("CP12_MATRIX_REBUILD_REQUIRED");
            CheckpointCP4 checkpoint=new CheckpointCP4(c.getString(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getLong(5),c.getString(6),c.getString(7),c.getLong(8),c.getBlob(9));
            if(!matrixId.equals(checkpoint.matrixId)||!checkpoint.payloadHash.equals(c.getString(10)))throw new IllegalStateException("CP12_MATRIX_CHECKPOINT_CORRUPT");return checkpoint;
        }
    }
    /** Called on research executor, no write lock is held while decoding/applying the bounded ring. */
    static PreparedProjectionCP12 prepareProjectionCP11(SQLiteDatabase db,long deadlineNanos) {return prepareProjectionCP12(db,deadlineNanos,java.util.Collections.<String>emptySet());}
    private static PreparedProjectionCP12 prepareProjectionCP12(SQLiteDatabase db,long deadlineNanos,java.util.Set<String> excluded) {
        if(db.inTransaction())throw new IllegalStateException("CP12_MATRIX_PREPARE_IN_WRITE_TRANSACTION");
        if(System.nanoTime()+50000000L>=deadlineNanos)return null;
        String id,receipt,issue,inputHash,proof,type,partition,manifest;long ordinal;int window;byte[] payload;
        StringBuilder excludeSql=new StringBuilder();java.util.ArrayList<String> excludeArgs=new java.util.ArrayList<>();for(String key:excluded){if(!hashCP4(key))throw new IllegalArgumentException("CP12_MATRIX_EXCLUDE");excludeSql.append(" AND i.matrix_id<>?");excludeArgs.add(key);}
        // Event ordinals, unlike draw ordinals, need not follow source issue order.
        // Preserve each metadata partition's head before applying global oldest-issue fairness.
        try(Cursor c=db.rawQuery("SELECT i.matrix_id,i.source_receipt_hash,i.source_issue,i.source_ordinal,i.input_hash,i.proof_hash,i.input_payload,s.matrix_type,s.partition_key,s.window_size,s.spec_manifest_hash FROM research_matrix_input_cp12 i JOIN research_matrix_spec_cp12 s ON s.matrix_id=i.matrix_id WHERE i.state='READY'"+excludeSql+" AND (s.matrix_type NOT IN ('LatencyMatrix','FailureMatrix') OR NOT EXISTS(SELECT 1 FROM research_matrix_input_cp12 earlier WHERE earlier.matrix_id=i.matrix_id AND earlier.state='READY' AND earlier.source_ordinal<i.source_ordinal)) ORDER BY i.source_issue,i.source_ordinal,i.matrix_id,i.source_receipt_hash LIMIT 1",excludeArgs.toArray(new String[0]))) {
            if(!c.moveToFirst())return null;id=c.getString(0);receipt=c.getString(1);issue=c.getString(2);ordinal=c.getLong(3);inputHash=c.getString(4);proof=c.getString(5);payload=c.getBlob(6);type=c.getString(7);partition=c.getString(8);window=c.getInt(9);manifest=c.getString(10);
        }
        try {
        MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.valueOf(type),partition,window,manifest);
        if(!id.equals(spec.matrixId)||!inputHash.equals(MatrixProjectionCP12.hash("CP12_MATRIX_INPUT|"+id+'|'+issue+'|'+ordinal+'|'+receipt+'|'+proof+'|'+MatrixProjectionCP12.bytesHash(payload))))throw new IllegalStateException("CP12_MATRIX_INPUT_CORRUPT");
        try(Cursor ack=db.rawQuery("SELECT through_issue FROM research_projection_ack_v2 WHERE consumer=? AND source_hash=?",new String[]{"MATRIX_CP4/"+id,receipt})) {
            if(ack.moveToFirst()){if(TargetIssueResolver.compare(ack.getString(0),issue)<0)throw new IllegalStateException("CP12_MATRIX_ACK_DRIFT");return new PreparedProjectionCP12(id,receipt,inputHash,issue,null,null,true);}
        }
        CheckpointCP4 expected=latestCP12(db,id);MatrixProjectionCP12.State state=expected==null?MatrixProjectionCP12.empty(spec):MatrixProjectionCP12.decode(spec,expected.payload());
        boolean metadata=spec.type==MatrixProjectionCP12.Type.LatencyMatrix||spec.type==MatrixProjectionCP12.Type.FailureMatrix;
        if(ordinal<state.throughOrdinal()||(!metadata&&expected!=null&&TargetIssueResolver.compare(issue,expected.throughIssue)<0))throw new IllegalStateException("CP12_MATRIX_PIT_REPLAY_REQUIRED");
        MatrixProjectionCP12.apply(state,ordinal,MatrixProjectionCP12.sampleFromBytes(payload),MatrixProjectionCP12.componentsFromBytes(payload));
        byte[] nextPayload=MatrixProjectionCP12.encode(state);CheckpointCP4 next=new CheckpointCP4(type,partition,window,manifest,MatrixProjectionCP12.SCHEMA_HASH,expected==null?0:expected.version+1,metadata&&expected!=null&&TargetIssueResolver.compare(expected.throughIssue,issue)>0?expected.throughIssue:issue,receipt,state.samples(),nextPayload);
        if(System.nanoTime()+50000000L>=deadlineNanos)return null;
        return new PreparedProjectionCP12(id,receipt,inputHash,issue,expected,next,false,AtomAdmissionCP12.prepareProjectionCP12(spec,state.samplesCopy(),next));
        } catch(IllegalArgumentException|IllegalStateException e){throw new ProjectionFailureCP12(id,receipt,inputHash,e);}
    }
    public static boolean commitProjectionCP12(SQLiteDatabase db,PreparedProjectionCP12 p,long now) {
        if(!db.inTransaction()||p==null)throw new IllegalStateException("CP12_MATRIX_TRANSACTION");
        try(Cursor c=db.rawQuery("SELECT input_hash,state FROM research_matrix_input_cp12 WHERE matrix_id=? AND source_receipt_hash=?",new String[]{p.matrixId,p.sourceReceiptHash})) {
            if(!c.moveToFirst()||!p.inputHash.equals(c.getString(0)))throw new IllegalStateException("CP12_MATRIX_INPUT_DRIFT");
            if("DONE".equals(c.getString(1)))return true;if(!"READY".equals(c.getString(1)))return false;
        }
        if(!p.alreadyApplied&&!commitCP4(db,p.expected,p.next,now))return false;
        if(!p.alreadyApplied)AtomAdmissionCP12.commitProjectionCP12(db,p.atomAdmission,p.next,now);
        if(p.alreadyApplied){try(Cursor a=db.rawQuery("SELECT through_issue FROM research_projection_ack_v2 WHERE consumer=? AND source_hash=?",new String[]{"MATRIX_CP4/"+p.matrixId,p.sourceReceiptHash})) {if(!a.moveToFirst()||TargetIssueResolver.compare(a.getString(0),p.sourceIssue)<0)throw new IllegalStateException("CP12_MATRIX_ACK_MISSING");}}
        db.execSQL("UPDATE research_matrix_input_cp12 SET state='DONE',last_error='',updated_at=? WHERE matrix_id=? AND source_receipt_hash=? AND input_hash=? AND state='READY'",new Object[]{now,p.matrixId,p.sourceReceiptHash,p.inputHash});
        try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("CP12_MATRIX_INPUT_COMPLETE_CAS");}return true;
    }
    public static final class ProjectionFailureCP12 extends RuntimeException {
        public final String matrixId,receipt,inputHash,reason;
        ProjectionFailureCP12(String id,String receipt,String inputHash,RuntimeException cause){super(cause.getMessage(),cause);matrixId=id;this.receipt=receipt;this.inputHash=inputHash;reason=cause.getMessage();}
    }
    public static boolean quarantineProjectionCP12(SQLiteDatabase db,ProjectionFailureCP12 failed,long now) {
        if(!db.inTransaction())throw new IllegalStateException("CP12_MATRIX_TRANSACTION");
        String state=failed.reason!=null&&failed.reason.contains("REPLAY_REQUIRED")?"REPLAY_REQUIRED":"QUARANTINED";
        db.execSQL("UPDATE research_matrix_input_cp12 SET state=?,last_error=?,updated_at=? WHERE matrix_id=? AND source_receipt_hash=? AND input_hash=? AND state='READY'",new Object[]{state,failed.reason,now,failed.matrixId,failed.receipt,failed.inputHash});
        try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()&&c.getInt(0)==1;}
    }
    /** Terminal ownership remains here; retention archives the full input before authorized deletion. No numeric ACK is issued. */
    public static int expireInputsCP12(SQLiteDatabase db,long now,int limit) {
        if(!db.inTransaction()||limit<1||limit>32)throw new IllegalStateException("CP12_MATRIX_EXPIRY_TRANSACTION");
        String cutoff=null;try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999",null)){if(c.moveToFirst())cutoff=c.getString(0);}if(cutoff==null)return 0;
        java.util.ArrayList<String[]> rows=new java.util.ArrayList<>();try(Cursor c=db.rawQuery("SELECT matrix_id,source_receipt_hash,input_hash,proof_hash,source_issue,state,last_error FROM research_matrix_input_cp12 WHERE source_issue<? AND state IN('READY','QUARANTINED','REPLAY_REQUIRED') ORDER BY source_issue,matrix_id,source_receipt_hash LIMIT "+limit,new String[]{cutoff})){while(c.moveToNext())rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6)});}
        for(String[] row:rows){String reason="MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION",terminal=MatrixProjectionCP12.hash("CP12_MATRIX_TERMINAL|"+row[0]+'|'+row[1]+'|'+row[2]+'|'+row[3]+'|'+row[4]+'|'+reason);
            db.execSQL("INSERT OR IGNORE INTO research_matrix_terminal_cp12(matrix_id,source_receipt_hash,input_hash,proof_hash,terminal_hash,reason,source_issue,created_at) VALUES(?,?,?,?,?,?,?,?)",new Object[]{row[0],row[1],row[2],row[3],terminal,reason,row[4],now});
            try(Cursor c=db.rawQuery("SELECT terminal_hash FROM research_matrix_terminal_cp12 WHERE matrix_id=? AND source_receipt_hash=?",new String[]{row[0],row[1]})){if(!c.moveToFirst()||!terminal.equals(c.getString(0)))throw new IllegalStateException("CP12_MATRIX_TERMINAL_DRIFT");}
            db.execSQL("UPDATE research_matrix_input_cp12 SET state='EXPIRED_ARCHIVE_REQUIRED',last_error=?,updated_at=? WHERE matrix_id=? AND source_receipt_hash=? AND input_hash=? AND state IN('READY','QUARANTINED','REPLAY_REQUIRED')",new Object[]{reason+"|prior_state="+row[5]+"|prior_error="+row[6],now,row[0],row[1],row[2]});
        }return rows.size();
    }
    /** One bounded Matrix phase: at most 32 distinct partitions, <=250ms including prepare, short writes, and 50ms margin. */
    public static boolean drainOneCP4(final DbStore store,long deadlineNanos) {
        long start=System.nanoTime();final long phaseDeadline=Math.min(deadlineNanos,start+250000000L);
        if(store==null||start+50000000L>=phaseDeadline)return false;
        boolean worked=false;java.util.Set<String> visited=new java.util.HashSet<>();
        final boolean[] registrationFirst={false};
        Integer expired=store.coordinatorForResearchR952().runAtomicWrite("CP12_MATRIX_EXPIRE",new DatabaseCoordinator.WriteCallable<Integer>(){public Integer call(SQLiteDatabase db){registrationFirst[0]=MatrixRegistrationR75.registrationFirst(db);return expireInputsCP12(db,System.currentTimeMillis(),32);}},null);worked=expired!=null&&expired>0;
        if(registrationFirst[0])worked|=MatrixRegistrationR75.drain(store,Math.min(phaseDeadline-75000000L,System.nanoTime()+75000000L));
        try(MemoryBudgetCP4.Lease scratch=com.ethk3.novaonepro.RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1572864)) {
            for(int partition=0;partition<32&&System.nanoTime()+50000000L<phaseDeadline;partition++){
                PreparedProjectionCP12 prepared;
                try {
                    prepared=prepareProjectionCP12(store.readable(),phaseDeadline,visited);
                    if(prepared==null){final MatrixSourceRouterCP12.PreparedInput source=MatrixSourceRouterCP12.prepareNext(store,phaseDeadline,visited);
                        if(source!=null){Boolean queued=store.coordinatorForResearchR952().runAtomicWrite("CP12_MATRIX_CAPTURE_SOURCE",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){MatrixSourceRouterCP12.commitPrepared(db,source,System.currentTimeMillis());return true;}},null);if(!Boolean.TRUE.equals(queued))break;worked=true;prepared=prepareProjectionCP12(store.readable(),phaseDeadline,visited);}}
                }catch(final ProjectionFailureCP12 failed){visited.add(failed.matrixId);
                    Boolean isolated=store.coordinatorForResearchR952().runAtomicWrite("CP12_MATRIX_QUARANTINE",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){return quarantineProjectionCP12(db,failed,System.currentTimeMillis());}},null);
                    worked|=Boolean.TRUE.equals(isolated);continue;
                }
                if(prepared==null)break;visited.add(prepared.matrixId);final PreparedProjectionCP12 p=prepared;
                final String[] capacityReason={null};
                Boolean result=store.coordinatorForResearchR952().runAtomicWrite("CP12_MATRIX_PROJECT",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){try{return commitProjectionCP12(db,p,System.currentTimeMillis());}catch(RuntimeException failed){for(Throwable cause=failed;cause!=null;cause=cause.getCause())if(cause.getMessage()!=null&&cause.getMessage().contains("MATRIX_CHECKPOINT_STORAGE_CAPACITY")){capacityReason[0]="MATRIX_CHECKPOINT_STORAGE_CAPACITY";break;}throw failed;}}},null);
                if(capacityReason[0]!=null){store.setState("cp12_matrix_blocker",capacityReason[0]);store.setState("cp12_matrix_blocked_id",p.matrixId);return worked;}
                if(Boolean.TRUE.equals(result)&&p.matrixId.equals(store.getState("cp12_matrix_blocked_id",""))){store.setState("cp12_matrix_blocker","");store.setState("cp12_matrix_blocked_id","");}
                worked|=Boolean.TRUE.equals(result);
            }
            // Alternate a bounded discovery quantum with consumption-first pulses; both retain progress.
            if(!registrationFirst[0])worked|=MatrixRegistrationR75.drain(store,phaseDeadline);
            return worked;
        }
    }
}
