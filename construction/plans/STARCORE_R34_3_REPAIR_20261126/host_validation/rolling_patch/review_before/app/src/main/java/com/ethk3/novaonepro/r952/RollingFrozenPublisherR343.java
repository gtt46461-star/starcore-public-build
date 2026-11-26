package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/** Publishes an independently frozen evaluation object from an immutable rolling
 * development result. It owns no real-decision, certificate or statistical pass.
 * It never changes the base learner's RUN, protocol, cursor or an older frozen row.
 * All scheduling uses the original queue, TRAIN quantum and database writer.
 */
public final class RollingFrozenPublisherR343 {
    private RollingFrozenPublisherR343() {}
    public static final String TABLE="research_rolling_frozen_publication_r343";
    public static final String REJECT="research_rolling_publication_reject_r343";
    public static final String PAGE="research_rolling_publication_page_r343";
    public static final String KIND="R343_ROLLING_FREEZE";
    private static final int PAGE_ROWS=32, MAX_PENDING=32;
    private static final String TERMINAL="('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED')";

    public static void schema(SQLiteDatabase db){
        need(db.inTransaction(),"ROLLING_PUBLICATION_SCHEMA_TX");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(version_id TEXT PRIMARY KEY REFERENCES "+RollingLearnerR343.VERSION+"(version_id),frozen_id TEXT NOT NULL UNIQUE REFERENCES "+ResearchEvaluationRunR967.EVAL+"(candidate_id),run_hash TEXT NOT NULL UNIQUE,learner_id TEXT NOT NULL,first_issue TEXT NOT NULL,ast_hash TEXT NOT NULL,rule_hash TEXT NOT NULL,initial_updates INTEGER NOT NULL CHECK(initial_updates BETWEEN 300 AND 1200),source_artifact TEXT NOT NULL UNIQUE,split_rule TEXT NOT NULL,integrity_epoch TEXT NOT NULL,model_hash TEXT NOT NULL,normalizer_hash TEXT NOT NULL,manifest_hash TEXT NOT NULL,protocol_hash TEXT NOT NULL,input_hash TEXT NOT NULL,training_receipt_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,feature_prefix_hash TEXT NOT NULL,publication_json TEXT NOT NULL,publication_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS r343_rolling_frozen_learner ON "+TABLE+"(learner_id,first_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+REJECT+"(version_id TEXT PRIMARY KEY REFERENCES "+RollingLearnerR343.VERSION+"(version_id),reason TEXT NOT NULL,receipt_json TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PAGE+"(version_id TEXT NOT NULL REFERENCES "+RollingLearnerR343.VERSION+"(version_id),first_index INTEGER NOT NULL CHECK(first_index>=0),end_index INTEGER NOT NULL CHECK(end_index>first_index AND end_index<=1320 AND end_index-first_index<=32),prefix_before TEXT NOT NULL,prefix_after TEXT NOT NULL,receipt_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(version_id,first_index))");
        for(String table:new String[]{TABLE,REJECT,PAGE}){
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_IMMUTABLE'); END");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_RECEIPT_PERMANENT'); END");
        }
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_publication_no_replace BEFORE INSERT ON "+TABLE+" WHEN EXISTS(SELECT 1 FROM "+TABLE+" WHERE version_id=NEW.version_id OR frozen_id=NEW.frozen_id OR run_hash=NEW.run_hash OR source_artifact=NEW.source_artifact OR publication_hash=NEW.publication_hash) BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_DUPLICATE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_publication_reject_no_replace BEFORE INSERT ON "+REJECT+" WHEN EXISTS(SELECT 1 FROM "+REJECT+" WHERE version_id=NEW.version_id OR receipt_hash=NEW.receipt_hash) BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_REJECT_DUPLICATE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_publication_page_no_replace BEFORE INSERT ON "+PAGE+" WHEN EXISTS(SELECT 1 FROM "+PAGE+" WHERE version_id=NEW.version_id AND first_index=NEW.first_index) BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_PAGE_DUPLICATE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_publication_outcome_exclusive BEFORE INSERT ON "+TABLE+" WHEN EXISTS(SELECT 1 FROM "+REJECT+" WHERE version_id=NEW.version_id) BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_OUTCOME_CONFLICT'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_publication_reject_exclusive BEFORE INSERT ON "+REJECT+" WHEN EXISTS(SELECT 1 FROM "+TABLE+" WHERE version_id=NEW.version_id) BEGIN SELECT RAISE(ABORT,'R343_PUBLICATION_OUTCOME_CONFLICT'); END");
    }
    public static boolean installed(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN(?,?,?)",new String[]{TABLE,REJECT,PAGE})){return c.moveToFirst()&&c.getInt(0)==3;}
    }
    /** Fail closed before migration and while an offer or active evaluation needs
     * the original. A hash receipt never substitutes for retained raw bytes. */
    public static boolean pinsRawVersion(SQLiteDatabase db,String version){
        if(!installed(db))return true;
        try(Cursor c=db.rawQuery("SELECT state FROM "+RollingLearnerR343.VERSION+" WHERE version_id=? AND NOT EXISTS(SELECT 1 FROM "+RollingLearnerR343.OFFER+" o WHERE o.version_id=?)",new String[]{version,version})){
            if(c.moveToFirst()&&("FAILED".equals(c.getString(0))||"DEPENDENCY_GAP".equals(c.getString(0))))return false;
        }
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+REJECT+" WHERE version_id=?",new String[]{version})){if(c.moveToFirst())return false;}
        try(Cursor c=db.rawQuery("SELECT p.frozen_id,life.state FROM "+TABLE+" p LEFT JOIN "+ResearchLifecycleR932.TABLE+" life ON life.candidate_key=('algorithm:'||p.frozen_id) WHERE p.version_id=?",new String[]{version})){
            if(!c.moveToFirst())return true;
            return c.isNull(1)||!retired(c.getString(1));
        }
    }
    /** Compact publication/page/rejection receipts are permanent. The learner's
     * existing bounded cleanup owns raw retention and calls pinsRawVersion. */
    public static boolean cleanup(SQLiteDatabase db){need(db.inTransaction(),"ROLLING_PUBLICATION_CLEANUP_TX");return false;}

    public static boolean drainOne(DbStore store,long endNs){
        if(store==null||System.nanoTime()+100_000_000L>=endNs||!installed(store.readable())||!RollingLearnerR343.installed(store.readable()))return false;
        ResearchTaskQueueR967.Lease task=store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_CLAIM",db->ResearchTaskQueueR967.claim(db,KIND,System.currentTimeMillis()),null);
        if(task==null){
            try{return planOne(store);}catch(ResearchTaskQueueR967.Capacity|DevelopmentProductionCP12.Wait waiting){return false;}
        }
        try{
            if(retireSuperseded(store,task))return true;
            try(MemoryBudgetCP4.Lease metadata=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(metadataBytes(store.readable(),task))){
            Offer offer=read(store.readable(),task);
            if(verifyPage(store,task,offer,endNs))return true;
            try(Prepared prepared=prepare(store,offer,endNs)){
                return Boolean.TRUE.equals(store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_COMMIT",db->commit(db,task,prepared),false));
            }
            }
        }catch(ResearchWorkBudgetR927.Yield|RuntimeResourceAuditR927.BudgetExceeded waiting){release(store,task,"REALTIME_PRIORITY",250);return true;}
        catch(DevelopmentProductionCP12.Wait waiting){release(store,task,waiting.reason,1000);return true;}
        catch(IOException waiting){release(store,task,"WAIT_PUBLICATION_INPUT_IO",1000);return true;}
        catch(RuntimeException failure){
            if(ResearchTrainingHookRegistryR952.retryInfrastructureCP12(store,task,failure))return true;
            if("ALGORITHM_MEMORY_BUDGET".equals(failure.getMessage())){release(store,task,"WAIT_RESOURCE",1000);return true;}
            // A bad new version does not rewrite or retire the independently
            // evaluated base/older version. Preserve a terminal rejection receipt.
            String text=failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());
            final String reason=text.length()>512?text.substring(0,512):text;
            Boolean rejected=store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_REJECT",db->{
                if(!ResearchTaskQueueR967.owns(db,task))return false;
                Map<String,Object> r=new TreeMap<>();r.put("revision","R343_ROLLING_PUBLICATION_REJECT_1");r.put("version",task.authorityRef);r.put("task",task.id);r.put("protocol",task.sequence);r.put("input",task.inputHash);r.put("reason",reason);r.put("realAuthority",false);
                String json=StrictCanonicalJson.writeContext(r);
                db.execSQL("INSERT INTO "+REJECT+" VALUES(?,?,?,?,?)",new Object[]{task.authorityRef,reason,json,V51ChecksumEngine.sha256(json),System.currentTimeMillis()});
                need(ResearchTaskQueueR967.failTerminalCP12(db,task,reason),"ROLLING_PUBLICATION_REJECT_LEASE");return true;
            },false);
            if(!Boolean.TRUE.equals(rejected))throw failure;return true;
        }
    }
    private static boolean planOne(DbStore store){
        return Boolean.TRUE.equals(store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_PLAN",db->{
            String[] obsolete=superseded(db,null);
            if(obsolete!=null){insertSuperseded(db,obsolete,"");return true;}
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind=? AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL')",new String[]{KIND})){need(c.moveToFirst(),"ROLLING_PUBLICATION_QUEUE_COUNT");if(c.getInt(0)>=MAX_PENDING)return false;}
            try(Cursor c=db.rawQuery("SELECT o.version_id,o.cutoff_issue,v.protocol_hash,v.input_hash FROM "+RollingLearnerR343.OFFER+" o JOIN "+RollingLearnerR343.VERSION+" v ON v.version_id=o.version_id JOIN r952_candidate parent ON parent.candidate_id=o.candidate_id WHERE v.state='COMPLETE' AND parent.state NOT IN"+TERMINAL+" AND NOT EXISTS(SELECT 1 FROM "+TABLE+" p WHERE p.version_id=o.version_id) AND NOT EXISTS(SELECT 1 FROM "+REJECT+" x WHERE x.version_id=o.version_id) AND NOT EXISTS(SELECT 1 FROM "+ResearchTaskQueueR967.TABLE+" t WHERE t.kind=? AND t.authority_ref=o.version_id) AND NOT EXISTS(SELECT 1 FROM "+ResearchTaskQueueR967.TABLE+" t JOIN "+RollingLearnerR343.VERSION+" pending ON pending.version_id=t.authority_ref WHERE t.kind=? AND pending.candidate_id=o.candidate_id AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL')) ORDER BY o.created_at,o.version_id LIMIT 1",new String[]{KIND,KIND})){
                if(!c.moveToFirst())return false;
                ResearchTaskQueueR967.enqueue(db,KIND,c.getString(0),TargetIssueResolver.next(c.getString(1)).targetIssue,c.getString(2),c.getString(3),c.getString(0));return true;
            }
        },false));
    }
    /** Supersession applies only before independent formal publication. It does
     * not invalidate a result, transfer samples, or touch an older formal window. */
    private static String[] superseded(SQLiteDatabase db,String version){
        String filter=version==null?" AND NOT EXISTS(SELECT 1 FROM "+ResearchTaskQueueR967.TABLE+" t WHERE t.kind='"+KIND+"' AND t.authority_ref=o.version_id)":" AND o.version_id=?";
        try(Cursor c=db.rawQuery("SELECT o.version_id,o.candidate_id,o.cutoff_issue,o.model_hash,o.receipt_hash,h.version_id,h.through_issue,h.model_hash FROM "+RollingLearnerR343.OFFER+" o JOIN "+RollingLearnerR343.VERSION+" v ON v.version_id=o.version_id AND v.state='COMPLETE' AND v.model_hash=o.model_hash AND v.receipt_hash=o.receipt_hash JOIN "+RollingLearnerR343.HEAD+" h ON h.candidate_id=o.candidate_id JOIN "+RollingLearnerR343.VERSION+" successor ON successor.version_id=h.version_id AND successor.candidate_id=h.candidate_id AND successor.state='COMPLETE' AND successor.model_hash=h.model_hash AND successor.cutoff_issue=h.through_issue WHERE h.version_id<>o.version_id AND h.through_issue>o.cutoff_issue AND NOT EXISTS(SELECT 1 FROM "+TABLE+" p WHERE p.version_id=o.version_id) AND NOT EXISTS(SELECT 1 FROM "+REJECT+" x WHERE x.version_id=o.version_id)"+filter+" ORDER BY o.created_at,o.version_id LIMIT 1",version==null?null:new String[]{version})){
            if(!c.moveToFirst())return null;String[] row=new String[8];for(int i=0;i<row.length;i++)row[i]=c.getString(i);return row;
        }
    }
    private static String insertSuperseded(SQLiteDatabase db,String[] row,String task){
        Map<String,Object> r=new TreeMap<>();r.put("revision","R343_ROLLING_UNPUBLISHED_SUPERSEDED_1");r.put("version",row[0]);r.put("candidate",row[1]);r.put("cutoff",row[2]);r.put("model",row[3]);r.put("trainingReceipt",row[4]);r.put("replacement",row[5]);r.put("replacementCutoff",row[6]);r.put("replacementModel",row[7]);r.put("task",task);r.put("reason","SUPERSEDED_UNPUBLISHED");r.put("statisticalFailure",false);r.put("formalFutureCount",0);r.put("realAuthority",false);
        String json=StrictCanonicalJson.writeContext(r),hash=V51ChecksumEngine.sha256(json);
        db.execSQL("INSERT INTO "+REJECT+" VALUES(?,?,?,?,?)",new Object[]{row[0],"SUPERSEDED_UNPUBLISHED",json,hash,System.currentTimeMillis()});return hash;
    }
    private static boolean retireSuperseded(DbStore store,ResearchTaskQueueR967.Lease task){
        if(superseded(store.readable(),task.authorityRef)==null)return false;
        return Boolean.TRUE.equals(store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_SUPERSEDE",db->{
            need(ResearchTaskQueueR967.owns(db,task),"ROLLING_PUBLICATION_SUPERSEDE_LEASE");
            String[] row=superseded(db,task.authorityRef);if(row==null)return false;
            String receipt=insertSuperseded(db,row,task.id);
            need(ResearchTaskQueueR967.expireDependencyR343(db,task,"SUPERSEDED_UNPUBLISHED",receipt),"ROLLING_PUBLICATION_SUPERSEDE_CAS");return true;
        },false));
    }
    private static final class Offer {
        final String version,candidate,cutoff,astHash,protocolHash,protocol,inputHash,prefix,checkpointHash,modelPayload,modelHash,receipt,receiptHash,ast;
        final String trainFirst,trainLast,validationFirst,validationLast;
        final int trainN,next;final long seed,generation,steps;final byte[] checkpoint,input;
        Offer(Cursor c){
            version=c.getString(0);candidate=c.getString(1);cutoff=c.getString(2);astHash=c.getString(3);protocolHash=c.getString(4);protocol=c.getString(5);inputHash=c.getString(6);trainN=c.getInt(7);seed=c.getLong(8);next=c.getInt(9);prefix=c.getString(10);checkpoint=c.getBlob(11);checkpointHash=c.getString(12);generation=c.getLong(13);modelPayload=c.getString(14);modelHash=c.getString(15);receipt=c.getString(16);receiptHash=c.getString(17);steps=c.getLong(18);ast=c.getString(19);input=c.getBlob(20);trainFirst=c.getString(21);trainLast=c.getString(22);validationFirst=c.getString(23);validationLast=c.getString(24);
        }
    }
    private static long metadataBytes(SQLiteDatabase db,ResearchTaskQueueR967.Lease task){
        try(Cursor c=db.rawQuery("SELECT length(CAST(v.model_payload AS BLOB)),length(CAST(c.ast AS BLOB)),length(CAST(v.receipt_json AS BLOB)),length(CAST(v.protocol_json AS BLOB)),length(v.checkpoint),length(v.input_blob) FROM "+RollingLearnerR343.VERSION+" v JOIN r952_candidate c ON c.candidate_id=v.candidate_id WHERE v.version_id=?",new String[]{task.authorityRef})){
            need(c.moveToFirst(),"ROLLING_PUBLICATION_VERSION_MISSING");
            int[] caps={262144,32768,8192,65536,DevelopmentTrainerCP9.MAX_CHECKPOINT,524288};long bytes=128L<<10;
            for(int i=0;i<caps.length;i++){need(!c.isNull(i)&&c.getLong(i)>0&&c.getLong(i)<=caps[i],"ROLLING_PUBLICATION_RAW_SIZE_OR_MISSING");bytes+=(i<4?4L:1L)*c.getLong(i);}
            return bytes;
        }
    }
    private static Offer read(SQLiteDatabase db,ResearchTaskQueueR967.Lease task){
        need(!db.inTransaction()&&KIND.equals(task.kind)&&task.candidate.equals(task.authorityRef),"ROLLING_PUBLICATION_TASK");
        try(Cursor c=db.rawQuery("SELECT length(CAST(v.model_payload AS BLOB)),length(CAST(c.ast AS BLOB)),length(CAST(v.receipt_json AS BLOB)),length(CAST(v.protocol_json AS BLOB)),length(v.checkpoint),length(v.input_blob) FROM "+RollingLearnerR343.VERSION+" v JOIN r952_candidate c ON c.candidate_id=v.candidate_id WHERE v.version_id=?",new String[]{task.authorityRef})){
            need(c.moveToFirst(),"ROLLING_PUBLICATION_VERSION_MISSING");
            int[] caps={262144,32768,8192,65536,DevelopmentTrainerCP9.MAX_CHECKPOINT,524288};
            for(int i=0;i<caps.length;i++)need(!c.isNull(i)&&c.getLong(i)>0&&c.getLong(i)<=caps[i],"ROLLING_PUBLICATION_RAW_SIZE_OR_MISSING");
        }
        String sql="SELECT v.version_id,v.candidate_id,v.cutoff_issue,v.ast_hash,v.protocol_hash,v.protocol_json,v.input_hash,v.train_n,v.seed,v.next_index,v.prefix_hash,v.checkpoint,v.checkpoint_hash,v.checkpoint_generation,v.model_payload,v.model_hash,v.receipt_json,v.receipt_hash,v.optimizer_steps,c.ast,v.input_blob,v.train_first,v.train_last,v.validation_first,v.validation_last FROM "+RollingLearnerR343.VERSION+" v JOIN "+RollingLearnerR343.OFFER+" o ON o.version_id=v.version_id AND o.candidate_id=v.candidate_id AND o.cutoff_issue=v.cutoff_issue AND o.model_hash=v.model_hash AND o.receipt_hash=v.receipt_hash JOIN r952_candidate c ON c.candidate_id=v.candidate_id AND c.structural_hash=v.ast_hash WHERE v.version_id=? AND v.state='COMPLETE' AND c.state NOT IN"+TERMINAL;
        try(Cursor c=db.rawQuery(sql,new String[]{task.authorityRef})){
            need(c.moveToFirst(),"ROLLING_PUBLICATION_OFFER_IDENTITY");Offer v=new Offer(c);need(!c.moveToNext(),"ROLLING_PUBLICATION_AMBIGUOUS");
            need(task.sequence.equals(v.protocolHash)&&task.inputHash.equals(v.inputHash)&&task.issue.equals(TargetIssueResolver.next(v.cutoff).targetIssue)&&v.next==v.trainN+120,"ROLLING_PUBLICATION_TASK_IDENTITY");
            String train=ResearchTaskQueueR967.naturalKey(RollingLearnerR343.KIND,v.version,task.issue,v.protocolHash);
            try(Cursor done=db.rawQuery("SELECT status,result_hash,input_hash,authority_ref FROM "+ResearchTaskQueueR967.TABLE+" WHERE task_id=?",new String[]{train})){
                need(done.moveToFirst()&&"COMPLETED".equals(done.getString(0))&&v.modelHash.equals(done.getString(1))&&v.inputHash.equals(done.getString(2))&&v.version.equals(done.getString(3)),"ROLLING_PUBLICATION_TRAIN_TASK_NOT_COMPLETE");
            }
            return v;
        }
    }
    private static String initialPrefix(Offer v){return V51ChecksumEngine.sha256("R343_FEATURE_PREFIX|"+v.version+'|'+v.protocolHash+'|'+v.inputHash);}
    private static String pageHash(Offer v,int first,int end,String before,String after){return V51ChecksumEngine.sha256("R343_PUBLICATION_ORIGINAL_PAGE_1|"+v.version+'|'+v.protocolHash+'|'+v.inputHash+'|'+v.modelHash+'|'+v.receiptHash+'|'+v.checkpointHash+'|'+first+'|'+end+'|'+before+'|'+after);}
    /** At most 1320 compact page receipts, including deadline-shortened pages. No raw JSON retained. */
    private static PagePosition pagePosition(SQLiteDatabase db,Offer v){
        int at=0;String prefix=initialPrefix(v);
        try(Cursor c=db.rawQuery("SELECT first_index,end_index,prefix_before,prefix_after,receipt_hash FROM "+PAGE+" WHERE version_id=? ORDER BY first_index LIMIT 1321",new String[]{v.version})){
            while(c.moveToNext()){
                int end=c.getInt(1);need(c.getInt(0)==at&&end>at&&end<=Math.min(at+PAGE_ROWS,v.next)&&prefix.equals(c.getString(2))&&pageHash(v,at,end,prefix,c.getString(3)).equals(c.getString(4)),"ROLLING_PUBLICATION_PAGE_CHAIN");
                prefix=c.getString(3);at=end;
            }
        }
        need(at<=v.next&&(at<v.next||prefix.equals(v.prefix)),"ROLLING_PUBLICATION_PAGE_PREFIX");return new PagePosition(at,prefix);
    }
    private static final class PagePosition{final int at;final String prefix;PagePosition(int at,String prefix){this.at=at;this.prefix=prefix;}}
    private static boolean verifyPage(DbStore store,ResearchTaskQueueR967.Lease task,Offer v,long endNs){
        PagePosition position=pagePosition(store.readable(),v);if(position.at==v.next)return false;
        int at=position.at;String prefix=position.prefix;
        String trainingTask=ResearchTaskQueueR967.naturalKey(RollingLearnerR343.KIND,v.version,TargetIssueResolver.next(v.cutoff).targetIssue,v.protocolHash);
        try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(512L<<10);
            Cursor c=store.readable().rawQuery("SELECT row_index,target_issue,actual,raw_hash,payload,row_hash,source_json,source_hash FROM "+RollingLearnerR343.FEATURE+" WHERE version_id=? AND row_index>=? ORDER BY row_index LIMIT 32",new String[]{v.version,Integer.toString(at)})){
            while(c.moveToNext()&&System.nanoTime()+50_000_000L<endNs){
                ResearchWorkBudgetR927.checkpoint();need(c.getInt(0)==at&&at<v.next,"ROLLING_PUBLICATION_ROW_ORDER");
                byte[] payload=c.getBlob(4);String source=c.getString(6),sourceHash=c.getString(7);
                need(HistoricalTrainingReceiptCP12.originalHashR343(source,sourceHash),"ROLLING_PUBLICATION_ORIGINAL_HASH");
                String hash=DevelopmentProductionCP12.featureHashCP12(trainingTask,v.protocolHash,v.inputHash,at,at<v.trainN?"TRAIN":"VALIDATION",c.getString(1),c.getInt(2),sourceHash,c.getString(3),payload);
                need(hash.equals(c.getString(5)),"ROLLING_PUBLICATION_ROW_HASH");
                DevelopmentProductionCP12.decode(c.getString(1),c.getString(3),c.getInt(2),payload);
                prefix=V51ChecksumEngine.sha256(prefix+'|'+hash);at++;
            }
        }
        if(at==position.at){release(store,task,"TIME_BUDGET_EXCEEDED",250);return true;}
        final int next=at;final String after=prefix,receipt=pageHash(v,position.at,next,position.prefix,after);
        store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_VERIFY",db->{
            need(ResearchTaskQueueR967.owns(db,task),"ROLLING_PUBLICATION_PAGE_LEASE");metadataCas(db,v);
            PagePosition current=pagePosition(db,v);need(current.at==position.at&&current.prefix.equals(position.prefix),"ROLLING_PUBLICATION_PAGE_CAS");
            db.execSQL("INSERT INTO "+PAGE+" VALUES(?,?,?,?,?,?,?)",new Object[]{v.version,position.at,next,position.prefix,after,receipt,System.currentTimeMillis()});
            need(ResearchTaskQueueR967.yield(db,task,"ORIGINALS|"+next+'|'+after,"ROLLING_PUBLICATION_ORIGINAL_PAGE",250),"ROLLING_PUBLICATION_PAGE_QUEUE");return true;
        },false);
        return true;
    }
    private static final class Prepared implements AutoCloseable {
        final Offer offer;final CandidateAstR952 ast;final CandidateCompilerR952.Model model;
        final Map<String,Object> protocol;final long sourceEpoch;final String target,artifact,run,epoch,structure,id,json,hash;
        final LongSummaryBindingsR80.PreparedSourcesR81 sources;final MemoryBudgetCP4.Lease memory;
        Prepared(Offer v,CandidateAstR952 ast,CandidateCompilerR952.Model model,Map<String,Object> protocol,long sourceEpoch,String target,LongSummaryBindingsR80.PreparedSourcesR81 sources,MemoryBudgetCP4.Lease memory){
            offer=v;this.ast=ast;this.model=model;this.protocol=protocol;this.sourceEpoch=sourceEpoch;this.target=target;this.sources=sources;this.memory=memory;
            artifact=V51ChecksumEngine.sha256("R343_ROLLING_FROZEN_SOURCE_1|"+v.version+'|'+v.protocolHash+'|'+v.inputHash+'|'+v.receiptHash+'|'+v.modelHash+'|'+v.checkpointHash+'|'+v.prefix);
            ResearchEvaluationRunR967.FrozenIdentityR343 identity=ResearchEvaluationRunR967.independentIdentityR343(v.candidate,target,ast,model,artifact);
            run=identity.run;epoch=identity.epoch;structure=ResearchEvaluationRunR967.evaluationHash(ast,model,TrainingCodeIdentityR964.SHA256,epoch);id=ResearchEvaluationRunR967.PREFIX+structure.substring(0,24);
            Map<String,Object> r=new TreeMap<>();r.put("revision","R343_ROLLING_FROZEN_PUBLICATION_1");r.put("version",v.version);r.put("baseCandidate",v.candidate);r.put("frozen",id);r.put("run",run);r.put("epoch",epoch);r.put("firstFutureIssue",target);r.put("cutoff",v.cutoff);r.put("ast",v.astHash);r.put("model",v.modelHash);r.put("normalizer",model.modern().normalizerHash);r.put("manifest",ast.graphSpec.manifest.hash);r.put("protocol",v.protocolHash);r.put("input",v.inputHash);r.put("trainingReceipt",v.receiptHash);r.put("checkpoint",v.checkpointHash);r.put("featurePrefix",v.prefix);r.put("sourceEpochAtValidation",sourceEpoch);r.put("replay","CP9_DONE_CHECKPOINT_RESTORE_AND_FINISH");r.put("formalFutureCount",0);r.put("realAuthority",false);
            json=StrictCanonicalJson.writeContext(r);hash=V51ChecksumEngine.sha256(json);
        }
        public void close(){memory.close();}
    }
    private static Prepared prepare(DbStore store,Offer v,long endNs)throws IOException{
        Map<String,Object> protocol=canonical(v.protocol,v.protocolHash),receipt=canonical(v.receipt,v.receiptHash);
        CandidateAstR952 ast=CandidateAstR952.parse(v.ast);CandidateCompilerR952.Model model=CandidateCompilerR952.Model.parse(v.modelPayload);
        need(ast.formatVersion==3&&!model.isLegacy()&&ast.structuralHash.equals(v.astHash)&&model.hash().equals(v.modelHash)&&model.updates==v.trainN,"ROLLING_PUBLICATION_MODEL_IDENTITY");
        CandidateCompilerR952.Modern modern=model.modern();modern.requireManifest(ast.graphSpec.manifest);
        need(modern.kind==ast.graphSpec.modelKind&&modern.family==ast.family&&modern.seed==v.seed&&modern.trainedUntil.equals(v.cutoff)&&modern.developmentReceiptHash.equals(v.receiptHash),"ROLLING_PUBLICATION_MODEL_CONTRACT");
        String[][] fields={{"revision","R343_ROLLING_DEV_RESULT_1"},{"version",v.version},{"candidate",v.candidate},{"protocol",v.protocolHash},{"input",v.inputHash},{"ast",v.astHash},{"cutoff",v.cutoff},{"normalizerHash",modern.normalizerHash},{"manifestHash",ast.graphSpec.manifest.hash},{"modelKind",modern.kind.name()},{"weightsHash",FrozenCandidateAdapterR958.developmentWeightsHashCP8(model)},{"codeRevision",TrainingCodeIdentityR964.SHA256},{"trainFirst",v.trainFirst},{"trainLast",v.trainLast},{"validationFirst",v.validationFirst},{"validationLast",v.validationLast}};
        for(String[] pair:fields)need(pair[1].equals(receipt.get(pair[0])),"ROLLING_PUBLICATION_RECEIPT_IDENTITY");
        need(number(receipt,"trainN")==v.trainN&&number(receipt,"validationN")==120&&number(receipt,"optimizerSteps")==v.steps&&number(receipt,"newStrategyCount")==0&&number(receipt,"formalFutureCount")==0&&Boolean.FALSE.equals(receipt.get("realAuthority")),"ROLLING_PUBLICATION_RECEIPT_COUNTS");
        need("R343_ROLLING_DEV_1".equals(protocol.get("revision"))&&v.version.equals(protocol.get("versionId"))&&v.candidate.equals(protocol.get("candidateId"))&&v.astHash.equals(protocol.get("astHash"))&&v.inputHash.equals(protocol.get("inputHash"))&&ast.graphSpec.manifest.hash.equals(protocol.get("manifestHash"))&&TrainingCodeIdentityR964.SHA256.equals(protocol.get("codeRevision"))&&v.cutoff.equals(protocol.get("cutoffIssue"))&&number(protocol,"seed")==v.seed&&number(protocol,"trainN")==v.trainN,"ROLLING_PUBLICATION_PROTOCOL_IDENTITY");
        need(v.trainFirst.equals(protocol.get("trainFirst"))&&v.trainLast.equals(protocol.get("trainLast"))&&v.validationFirst.equals(protocol.get("validationFirst"))&&v.validationLast.equals(protocol.get("validationLast")),"ROLLING_PUBLICATION_PROTOCOL_SPLIT");
        need(protocol.get("canonicalRangeHash") instanceof String&&protocol.get("canonicalRangeHash").equals(receipt.get("canonicalRangeHash")),"ROLLING_PUBLICATION_CANONICAL_SOURCE_RECEIPT");
        need(TrainingCasCP12.hex(TrainingCasCP12.sha().digest(v.checkpoint)).equals(v.checkpointHash),"ROLLING_PUBLICATION_CHECKPOINT_HASH");
        CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,DevelopmentProductionCP12.readSpecs(store.readable(),ast));
        LongSummaryBindingsR80.validateProtocolSources(store.readable(),protocol);
        long sourceEpoch=RollingLearnerR343.verifySourceR343(store.readable(),protocol);
        int width=modern.featureN==0?modern.inputDimension:modern.featureN;
        MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(DevelopmentTrainerCP9.estimatedDatasetBytesCP12(v.trainN,width));boolean success=false;
        try{
            ArrayList<DevelopmentTrainerCP9.Row> rows=new ArrayList<>();
            try(DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(new ByteArrayInputStream(v.input),v.input.length,v.inputHash,new DevelopmentInputCP9.Header((String)protocol.get("header")),RuntimeMemoryGuardR811.algorithmModelBudget())){
            DevelopmentInputCP9.Split split=input.split(ast.graphSpec.manifest.requiredHistory);
            need(input.rowCount()==number(protocol,"historyCount")&&input.issue(0).equals(protocol.get("historyFirst"))&&input.issue(input.rowCount()-1).equals(protocol.get("historyLast")),"ROLLING_PUBLICATION_INPUT_RANGE");
            need(split.trainN==v.trainN&&input.issue(split.trainFirst).equals(v.trainFirst)&&input.issue(split.trainEnd-1).equals(v.trainLast)&&input.issue(split.validationFirst).equals(v.validationFirst)&&input.issue(split.validationEnd-1).equals(v.cutoff),"ROLLING_PUBLICATION_INPUT_SPLIT");
            String prefix=initialPrefix(v);
            try(Cursor c=store.readable().rawQuery("SELECT row_index,target_issue,actual,raw_hash,payload,row_hash,source_hash FROM "+RollingLearnerR343.FEATURE+" WHERE version_id=? ORDER BY row_index LIMIT 1321",new String[]{v.version})){
                String trainTask=ResearchTaskQueueR967.naturalKey(RollingLearnerR343.KIND,v.version,TargetIssueResolver.next(v.cutoff).targetIssue,v.protocolHash);
                while(c.moveToNext()){
                    checkpoint(endNs);int i=rows.size(),inputRow=i<v.trainN?split.trainFirst+i:split.validationFirst+i-v.trainN;
                    need(i<v.next&&c.getInt(0)==i&&input.issue(inputRow).equals(c.getString(1))&&input.actual(inputRow,ast.position)==c.getInt(2),"ROLLING_PUBLICATION_DATASET_INPUT");
                    byte[] payload=c.getBlob(4);String hash=DevelopmentProductionCP12.featureHashCP12(trainTask,v.protocolHash,v.inputHash,i,i<v.trainN?"TRAIN":"VALIDATION",c.getString(1),c.getInt(2),c.getString(6),c.getString(3),payload);need(hash.equals(c.getString(5)),"ROLLING_PUBLICATION_DATASET_HASH");prefix=V51ChecksumEngine.sha256(prefix+'|'+hash);
                    rows.add(DevelopmentProductionCP12.decode(c.getString(1),c.getString(3),c.getInt(2),payload));
                }
            }
            need(rows.size()==v.next&&prefix.equals(v.prefix)&&pagePosition(store.readable(),v).at==v.next,"ROLLING_PUBLICATION_DATASET_PREFIX");
            } // Release SCDEV1's retained 2 MiB before allocating the numeric restore.
            // CP9's shape identity excludes trained parameters and includes kind,
            // seed, manifest, experts, cutoff, dimensions, missing policies and N.
            DevelopmentTrainerCP9.Dataset dataset=new DevelopmentTrainerCP9.Dataset(modern,v.protocolHash,v.inputHash,ast.graphSpec.manifest.orderedFeatureIds,ast.graphSpec.manifest.missingPolicyPerFeature,rows,v.trainN);
            try(MemoryBudgetCP4.Lease numerics=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(DevelopmentTrainerCP9.estimatedNumericsBytesCP12(dataset))){
                checkpoint(endNs);DevelopmentTrainerCP9.State restored=DevelopmentTrainerCP9.restore(dataset,v.checkpoint);
                need(restored.complete(),"ROLLING_PUBLICATION_CHECKPOINT_NOT_DONE");
                double ce=restored.validationCE(),brier=restored.validationBrier();
                need(Double.isFinite(ce)&&ce>=0&&ce<=35&&Double.isFinite(brier)&&brier>=0&&brier<=.2+1e-12&&v.steps>=0&&restored.executedEpochs()>=1&&restored.executedEpochs()<=20&&restored.optimizerSteps()==v.steps&&restored.executedEpochs()==number(receipt,"epochs")&&restored.selectedEpoch()+1==number(receipt,"selectedEpoch")&&Double.toHexString(ce).equals(receipt.get("ce"))&&Double.toHexString(brier).equals(receipt.get("brier"))&&restored.orderedPredictionHash().equals(receipt.get("predictionHash")),"ROLLING_PUBLICATION_CHECKPOINT_RECEIPT");
                CandidateCompilerR952.Model replay=DevelopmentTrainerCP9.finish(dataset,restored,v.receiptHash);
                need(replay.hash().equals(v.modelHash)&&replay.payload().equals(v.modelPayload),"ROLLING_PUBLICATION_CODEC_REPLAY");
            }
            checkpoint(endNs);String target=currentTarget(store.readable());
            if(v.cutoff.compareTo(target)>=0)throw new DevelopmentProductionCP12.Wait("WAIT_FUTURE_PUBLICATION_TARGET");
            LongSummaryBindingsR80.PreparedSourcesR81 sources=LongSummaryBindingsR80.prepareCurrentSourcesR81(store.readable(),ast,target);
            Prepared p=new Prepared(v,ast,model,protocol,sourceEpoch,target,sources,memory);success=true;return p;
        }finally{if(!success)memory.close();}
    }
    private static boolean commit(SQLiteDatabase db,ResearchTaskQueueR967.Lease task,Prepared p){
        need(db.inTransaction()&&ResearchTaskQueueR967.owns(db,task),"ROLLING_PUBLICATION_COMMIT_LEASE");Offer v=p.offer;metadataCas(db,v);
        if(!p.target.equals(currentTarget(db))||ResearchEvaluationRunR967.drawn(db,p.target))throw new DevelopmentProductionCP12.Wait("TARGET_SUPERSEDED");
        if(RollingLearnerR343.sourceEpochR343(db)!=p.sourceEpoch)throw new DevelopmentProductionCP12.Wait("ROLLING_PUBLICATION_SOURCE_CHANGED");
        LongSummaryBindingsR80.validateProtocolSources(db,p.protocol);p.sources.validateAtCommit(db);
        String active=" FROM "+ResearchEvaluationRunR967.EVAL+" f LEFT JOIN "+ResearchLifecycleR932.TABLE+" life ON life.candidate_key=('algorithm:'||f.candidate_id) WHERE (life.state IS NULL OR life.state NOT IN"+TERMINAL+") AND "+ResearchEvaluationRunR967.frozenEligibilitySqlR343(db,"f",p.target);
        try(Cursor c=db.rawQuery("SELECT COUNT(*)"+active+" AND f.learner_id=?",new String[]{v.candidate})){need(c.moveToFirst(),"ROLLING_PUBLICATION_CAPACITY");if(c.getInt(0)>=ResearchEvaluationRunR967.MAX_ACTIVE_PER_LEARNER)throw new DevelopmentProductionCP12.Wait("WAIT_FROZEN_LEARNER_CAPACITY");}
        try(Cursor c=db.rawQuery("SELECT COUNT(*)"+active,null)){need(c.moveToFirst(),"ROLLING_PUBLICATION_CAPACITY");if(c.getInt(0)>=ResearchEvaluationRunR967.MAX_ACTIVE_GLOBAL)throw new DevelopmentProductionCP12.Wait("WAIT_FROZEN_GLOBAL_CAPACITY");}
        long now=System.currentTimeMillis();
        db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.EVAL+"(candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at,run_hash,rule_hash,integrity_epoch,update_endpoint) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0)",new Object[]{p.id,v.candidate,p.ast.canonical,v.modelPayload,p.structure,v.modelHash,v.cutoff,p.target,p.artifact,now,p.run,TrainingCodeIdentityR964.SHA256,p.epoch});
        db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{v.version,p.id,p.run,v.candidate,p.target,v.astHash,TrainingCodeIdentityR964.SHA256,v.trainN,p.artifact,ResearchEvaluationRunR967.SPLIT,p.epoch,v.modelHash,p.model.modern().normalizerHash,p.ast.graphSpec.manifest.hash,v.protocolHash,v.inputHash,v.receiptHash,v.checkpointHash,v.prefix,p.json,p.hash,now});
        ResearchEvaluationRunR967.initializeRollingFrozenLifecycleR343(db,p.id);
        LongSummaryBindingsR80.registerFrozenSourcesR81(db,p.structure,p.sources.epochs);
        MatrixSourceRouterCP12.registerFrozenPolicyMatricesCP12(db,p.structure);
        MatrixSourceRouterCP12.registerFrozenAtomStudiesCP12(db,p.structure,p.ast,p.model);
        Map<String,String> components=new TreeMap<>();components.put("ast",v.astHash);components.put("model",v.modelHash);components.put("features",p.ast.graphSpec.manifest.hash);components.put("development",v.receiptHash);components.put("protocol",v.protocolHash);
        MatrixSourceRouterCP12.enqueueFreezeCP12(db,p.structure,p.target,v.receiptHash,components,now);
        ResearchTaskQueueR967.complete(db,task,p.id);return true;
    }
    private static void metadataCas(SQLiteDatabase db,Offer v){
        try(Cursor c=db.rawQuery("SELECT v.model_hash,v.receipt_hash,v.checkpoint_hash,v.checkpoint_generation,v.prefix_hash,v.next_index,v.protocol_hash,v.input_hash,v.ast_hash,c.state,o.model_hash,o.receipt_hash FROM "+RollingLearnerR343.VERSION+" v JOIN r952_candidate c ON c.candidate_id=v.candidate_id AND c.structural_hash=v.ast_hash JOIN "+RollingLearnerR343.OFFER+" o ON o.version_id=v.version_id AND o.candidate_id=v.candidate_id AND o.cutoff_issue=v.cutoff_issue WHERE v.version_id=? AND v.state='COMPLETE'",new String[]{v.version})){
            need(c.moveToFirst()&&v.modelHash.equals(c.getString(0))&&v.receiptHash.equals(c.getString(1))&&v.checkpointHash.equals(c.getString(2))&&v.generation==c.getLong(3)&&v.prefix.equals(c.getString(4))&&v.next==c.getInt(5)&&v.protocolHash.equals(c.getString(6))&&v.inputHash.equals(c.getString(7))&&v.astHash.equals(c.getString(8))&&!retired(c.getString(9))&&v.modelHash.equals(c.getString(10))&&v.receiptHash.equals(c.getString(11)),"ROLLING_PUBLICATION_METADATA_CAS");
        }
    }
    private static String currentTarget(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){
            if(!c.moveToFirst()||!TargetIssueResolver.isValid(c.getString(0)))throw new DevelopmentProductionCP12.Wait("WAIT_FUTURE_PUBLICATION_TARGET");return TargetIssueResolver.next(c.getString(0)).targetIssue;
        }
    }
    private static Map<String,Object> canonical(String json,String hash){Map<String,Object> value=StrictCanonicalJson.readContextObject(json);need(V51ChecksumEngine.sha256(json).equals(hash)&&StrictCanonicalJson.writeContext(value).equals(json),"ROLLING_PUBLICATION_CANONICAL_HASH");return value;}
    private static long number(Map<String,Object> map,String key){Object n=map.get(key);need(n instanceof Long,"ROLLING_PUBLICATION_NUMBER:"+key);return (Long)n;}
    private static boolean retired(String state){return "QUARANTINED".equals(state)||"QUALITY_PRUNED".equals(state)||"RESOURCE_RETIRED".equals(state);}
    private static void checkpoint(long end){ResearchWorkBudgetR927.checkpoint();if(System.nanoTime()+50_000_000L>=end)throw new DevelopmentProductionCP12.Wait("TIME_BUDGET_EXCEEDED");}
    private static void release(DbStore store,ResearchTaskQueueR967.Lease task,String reason,long delay){ResearchWorkBudgetR927.finishLeaseReceiptR66(()->store.coordinatorForResearchR952().runAtomicWrite("R343_ROLLING_FREEZE_WAIT",db->ResearchTaskQueueR967.yield(db,task,task.cursor,reason,delay),false));}
    private static void need(boolean valid,String reason){if(!valid)throw new IllegalStateException(reason);}
}
