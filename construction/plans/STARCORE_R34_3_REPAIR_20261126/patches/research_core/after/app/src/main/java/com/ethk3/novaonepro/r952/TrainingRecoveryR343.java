package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** One narrowly scoped upgrade recovery. Old failed tasks and their cursors stay immutable.
 * A fresh task recomputes features from the verified ORIGINAL input. No new strategy,
 * qualification, Future sample or real authority is created by recovery. */
public final class TrainingRecoveryR343 {
    private TrainingRecoveryR343() {}
    public static final String TABLE="research_training_recovery_r343";
    public static final String REASON="WriteRejectedR31:F01_WRITE_FAILED|operation=CP12_TRAIN_FEATURE_PAGE|category=DEPENDENCY|phase=ADMISSION|commitOutcome=ROLLED_BACK|cause=IllegalStateException:R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN";
    private static final String[][] KNOWN={
        {"e3b213d36a7ed00285d3eb1d5b3c9cdb2ed7c27da335ecc0669430e6276230af","CP12-FAMILY-872002fb10d1f206e96eae54","68d2ab3bb4937fa695f7e25fbedd42b3153e7f8be7691167c405e234184128d5","2bac10f86871fbee7b0c7eae5111b48499f7b1fe6714fe0f122e8efb720cf004","1f60e3a7e4f9491351ae8b74ffc7cb55891c5c449e39646dcd20f474189ee5cd"},
        {"cb45e8fea8dcd1299fbbf07aa0d0207e523d21b98b0cc3b494bd07a3abd7fb91","CP12-FAMILY-f0f49290b8fcab8188deb631","b615b8587cb245d475cf973bf57b9dedfa7a3f345102d4f782456f64dcd20218","735ed22d0d458606994911222372c46d6021cc6c7fa40e84351102316e4f5aa7","42fc320e62d76b1920d9c538b49bff5e8cf3d0e0d2911dbd4f0a367a8b5a8583"},
        {"1b2b6718fa182a34b2cb479ecadac9e093f2ebc71a9714ef807599d8c6c1ddd2","CP12-FAMILY-5cf823977a24cef2b339f9d6","93df87de2cbcba870994e1de94401aef64d686f8f070b320e1809d8966392a12","735ed22d0d458606994911222372c46d6021cc6c7fa40e84351102316e4f5aa7","b874033b66b662adb70c33fec951ef1d6761e209541f0fffde7b90607d0c583c"},
        {"50a155a379dd3897a692b8d0448d68228ddb097dd02bb6b29a3c88af6dc2a683","CP12-FAMILY-3df5c0a347965ee5d558bf6a","6e2798118f779d82da802ba62ac272c40df9bb427f87f3015001a4874e0b89a3","642b4f9808a50c5ccfdbe348bbc7b9a70f8a37dab11a300ae8515582a30bb622","7e88ef547202dc6452311ee7a71914cec00646ac7ab6b8b81361fd5ff59b69e2"}
    };
    public static void schema(SQLiteDatabase db){
        need(db.inTransaction(),"RECOVERY_SCHEMA_TX");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(recovery_id TEXT PRIMARY KEY CHECK(length(recovery_id)=64),failed_task_id TEXT NOT NULL UNIQUE REFERENCES v57_research_task_r967(task_id),successor_task_id TEXT NOT NULL UNIQUE REFERENCES v57_research_task_r967(task_id),candidate_id TEXT NOT NULL REFERENCES r952_candidate(candidate_id),protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),input_hash TEXT NOT NULL REFERENCES research_training_input_v2(input_hash),ast_hash TEXT NOT NULL,action_id TEXT NOT NULL REFERENCES research_action_v2(action_id),reason_hash TEXT NOT NULL,receipt_json TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_training_recovery_no_update BEFORE UPDATE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R343_RECOVERY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_training_recovery_no_delete BEFORE DELETE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R343_RECOVERY_PERMANENT'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_training_recovery_no_replace BEFORE INSERT ON "+TABLE+" WHEN EXISTS(SELECT 1 FROM "+TABLE+" r WHERE r.recovery_id=NEW.recovery_id OR r.failed_task_id=NEW.failed_task_id OR r.successor_task_id=NEW.successor_task_id) BEGIN SELECT RAISE(ABORT,'R343_RECOVERY_REPLACE_FORBIDDEN'); END");
    }
    public static boolean installed(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE (type='table' AND name=?) OR (type='trigger' AND name IN('r343_training_recovery_no_update','r343_training_recovery_no_delete','r343_training_recovery_no_replace'))",new String[]{TABLE})){
            return c.moveToFirst()&&c.getInt(0)==4;
        }
    }
    static boolean known(String[] row){
        if(row==null||row.length<13||!REASON.equals(row[6])||!"FAILED_TERMINAL".equals(row[7])
                ||!"QUARANTINED".equals(row[10])||!"REJECTED".equals(row[11])||!REASON.equals(row[12]))return false;
        for(String[] k:KNOWN)if(k[0].equals(row[0])&&k[1].equals(row[1])&&k[2].equals(row[2])
                &&k[3].equals(row[3])&&k[4].equals(row[4]))return true;
        return false;
    }
    private static String[] read(SQLiteDatabase db,String id){
        try(Cursor c=db.rawQuery("SELECT t.task_id,t.candidate_id,t.stream_seq,t.input_hash,t.authority_ref,t.target_issue,t.last_error,t.status,t.cursor,c.structural_hash,c.state,a.state,a.last_error,c.ast,p.protocol_json,i.byte_count,p.train_n,p.validation_last,i.row_count,t.generation,t.updated_at,a.finding_id FROM v57_research_task_r967 t JOIN r952_candidate c ON c.candidate_id=t.candidate_id JOIN research_action_v2 a ON a.action_id=t.authority_ref AND a.child_id=c.candidate_id JOIN research_training_protocol_v2 p ON p.protocol_hash=t.stream_seq AND p.candidate_id=c.candidate_id AND p.input_hash=t.input_hash JOIN research_training_input_v2 i ON i.input_hash=t.input_hash WHERE t.task_id=? AND t.kind='TRAIN_CANDIDATE' AND c.future_n=0 AND c.may_create_real=0 AND NOT EXISTS(SELECT 1 FROM "+TABLE+" r WHERE r.failed_task_id=t.task_id) AND NOT EXISTS(SELECT 1 FROM r952_artifact x WHERE x.candidate_id=c.candidate_id) AND NOT EXISTS(SELECT 1 FROM r952_learning_cursor l WHERE l.consumer_id=('R952:'||c.candidate_id)) AND NOT EXISTS(SELECT 1 FROM v57_research_task_r967 x WHERE x.candidate_id=c.candidate_id AND x.kind IN('TRAIN_CANDIDATE','FREEZE_CANDIDATE') AND x.task_id<>t.task_id)",new String[]{id})){
            if(!c.moveToFirst())return null;
            String[] row=new String[22];for(int i=0;i<row.length;i++)row[i]=c.getString(i);
            need(!c.moveToNext(),"RECOVERY_SOURCE_NOT_UNIQUE");return row;
        }
    }
    /** Existing TRAIN owner calls at most once per quantum; missing files stay held. */
    public static boolean drainOne(DbStore store,long deadlineNs){
        if(store==null||System.nanoTime()+250_000_000L>=deadlineNs||!installed(store.readable()))return false;
        String[] row=null;
        for(String[] k:KNOWN){
            String held=store.getState("r343_recovery_hold_"+k[0],"");
            int separator=held.indexOf('|');long since=0;
            if(separator>0)try{since=Long.parseLong(held.substring(0,separator));}catch(NumberFormatException ignored){}
            if(since>0&&System.currentTimeMillis()>=since&&System.currentTimeMillis()-since<60000L)continue;
            String[] candidate=read(store.readable(),k[0]);if(known(candidate)){row=candidate;break;}
        }
        if(row==null)return false;
        try{return verifyAndResume(store,row,deadlineNs);}
        catch(IllegalArgumentException invalid){hold(store,row[0],"ORIGINAL_INPUT_INVALID:"+invalid.getMessage());return false;}
        catch(IllegalStateException invalid){
            if(invalid.getMessage()==null||!invalid.getMessage().startsWith("RECOVERY_"))throw invalid;
            hold(store,row[0],"RECOVERY_CONTRACT_HOLD:"+invalid.getMessage());return false;
        }
    }
    private static void hold(DbStore store,String id,String reason){
        store.trySetTelemetryStatesR77(java.util.Collections.singletonMap("r343_recovery_hold_"+id,
                System.currentTimeMillis()+"|"+reason));
    }
    private static boolean verifyAndResume(DbStore store,String[] row,long deadlineNs){
        final String[] expected=row;
        CandidateAstR952 ast=CandidateAstR952.parse(row[13]);
        need(ast.formatVersion==3&&ast.structuralHash.equals(row[9]),"RECOVERY_AST_HASH");
        Map<String,Object> protocol=StrictCanonicalJson.readContextObject(row[14]);
        need(V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(protocol)).equals(row[2])
                &&row[3].equals(protocol.get("inputHash"))&&row[9].equals(protocol.get("astHash"))
                &&ast.graphSpec.manifest.hash.equals(protocol.get("manifestHash")),"RECOVERY_PROTOCOL_HASH");
        // These four audited tasks have data-only fixed graphs. Any changed external
        // dependency must be investigated; this upgrade never invents its receipt.
        need(ast.graphSpec.experts.isEmpty()&&ast.graphSpec.manifest.bindingHashes.isEmpty()
                &&ast.graphSpec.manifest.summarySpecRefs.isEmpty()
                &&protocol.get("boundFiles") instanceof List&&((List<?>)protocol.get("boundFiles")).isEmpty(),"RECOVERY_EXTERNAL_DEPENDENCY");
        List<AtomSpecCP4> specs=new ArrayList<>();
        java.util.TreeSet<String> atomIds=new java.util.TreeSet<>();
        for(TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes)
            if(node.kind==TypedAtomMoleculeGraphR930.NodeKind.ATOM)atomIds.add(node.source);
        for(String atom:atomIds)try(Cursor c=store.readable().rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{atom})){
            need(c.moveToFirst(),"RECOVERY_ATOM_MISSING");
            specs.add(AtomSpecCP4.fromRegistry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getString(5),c.getString(6)));
        }
        CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);
        if(System.nanoTime()+100_000_000L>=deadlineNs)return false;
        try(TrainingCasCP12.LockedFile file=TrainingCasCP12.open(store.researchInputRootCP11(),row[3],TrainingCasCP12.INPUT_SUFFIX)){
            need(file.bytes==Long.parseLong(row[15]),"RECOVERY_INPUT_BYTES");
            try(InputStream stream=file.open();DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(stream,file.bytes,row[3],new DevelopmentInputCP9.Header((String)protocol.get("header")),RuntimeMemoryGuardR811.algorithmModelBudget())){
                DevelopmentInputCP9.Split split=input.split(ast.graphSpec.manifest.requiredHistory);
                need(input.rowCount()==Integer.parseInt(row[18])&&split.trainN==Integer.parseInt(row[16])
                        &&input.issue(split.validationEnd-1).equals(row[17]),"RECOVERY_INPUT_SPLIT");
            }
            ResearchWorkBudgetR927.checkpoint();
            String target=TargetIssueResolver.next(store.getLatestDrawIssue()).targetIssue;
            need(TargetIssueResolver.isValid(target)&&target.compareTo(row[5])>0,"RECOVERY_TARGET");
            return Boolean.TRUE.equals(store.coordinatorForResearchR952().runAtomicWrite("R343_TRAIN_INFRA_RECOVERY",db->commit(db,expected,target),false));
        }catch(IOException missing){
            hold(store,row[0],"WAIT_ORIGINAL_CAS_IO");return false;
        }catch(IllegalArgumentException invalid){
            // Missing/hash-invalid originals never cause an unverified resumption.
            hold(store,row[0],"ORIGINAL_CAS_VERIFICATION_FAILED:"+invalid.getMessage());return false;
        }
    }
    private static boolean commit(SQLiteDatabase db,String[] expected,String target){
        need(db.inTransaction(),"RECOVERY_COMMIT_TX");
        String[] actual=read(db,expected[0]);
        if(actual==null)return false;
        need(known(actual)&&Arrays.equals(expected,actual),"RECOVERY_SOURCE_CAS");
        if(!ResearchDirectorR952.trainingQueueHasCapacityR65(db))return false;
        String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(!TargetIssueResolver.isValid(latest)||!TargetIssueResolver.next(latest).targetIssue.equals(target))return false;
        String successor=ResearchTaskQueueR967.enqueue(db,"TRAIN_CANDIDATE",actual[1],target,actual[2],actual[3],actual[4]);
        String recovery=V51ChecksumEngine.sha256("R343_INFRA_RECOVERY|"+actual[0]+'|'+successor);
        Map<String,Object> receipt=new TreeMap<>();receipt.put("revision","R343_INFRA_RECOVERY_1");
        receipt.put("failedTask",actual[0]);receipt.put("successorTask",successor);receipt.put("candidate",actual[1]);
        receipt.put("protocol",actual[2]);receipt.put("input",actual[3]);receipt.put("ast",actual[9]);
        receipt.put("oldTaskStatus",actual[7]);receipt.put("oldCursor",actual[8]);receipt.put("oldGeneration",actual[19]);
        receipt.put("oldCandidateState",actual[10]);receipt.put("oldActionState",actual[11]);receipt.put("oldError",actual[12]);
        receipt.put("featureRestart",0);receipt.put("newStrategyCount",0);receipt.put("futureCount",0);receipt.put("realAuthority",false);
        String json=StrictCanonicalJson.writeContext(receipt),hash=V51ChecksumEngine.sha256(json);long now=System.currentTimeMillis();
        db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{recovery,actual[0],successor,actual[1],actual[2],actual[3],actual[9],actual[4],V51ChecksumEngine.sha256(REASON),json,hash,now});
        db.execSQL("UPDATE r952_candidate SET state='NURSERY' WHERE candidate_id=? AND state='QUARANTINED' AND structural_hash=? AND future_n=0 AND may_create_real=0",new Object[]{actual[1],actual[9]});need(changes(db)==1,"RECOVERY_CANDIDATE_CAS");
        db.execSQL("UPDATE research_action_v2 SET state='WAIT_FUTURE',last_error=? WHERE action_id=? AND child_id=? AND state='REJECTED' AND last_error=?",new Object[]{"R343_INFRA_RECOVERED:"+hash,actual[4],actual[1],REASON});need(changes(db)==1,"RECOVERY_ACTION_CAS");
        // The original task-specific TRAIN_INPUT_INTEGRITY finding remains ERROR.
        // Only its originating experiment resumes; the immutable recovery receipt
        // records the exact old rejection and expressly supplies no quality evidence.
        db.execSQL("UPDATE research_finding_v2 SET state='EVALUATING',updated_at=? WHERE finding_id=? AND state='ERROR'",new Object[]{now,actual[21]});need(changes(db)==1,"RECOVERY_FINDING_CAS");
        return true;
    }
    private static int changes(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
    private static void need(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
}
