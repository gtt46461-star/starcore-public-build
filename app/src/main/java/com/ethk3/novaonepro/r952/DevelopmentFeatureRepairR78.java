package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Feature-coverage failure is a DEV design diagnosis, never a Future label.
 * Uses existing RESEARCH/EVOLVE/TRAIN owners and existing action/protocol ledgers.
 * Corrupt/missing/expired input cannot enter this repair path. */
public final class DevelopmentFeatureRepairR78 {
    public static final String OPERATOR="DEV_FEATURE_REPAIR_R78";
    private static final String ROLE="DEVELOPMENT_COVERAGE_DIAGNOSTIC_NOT_FUTURE",CURSOR="r78_dev_repair_cursor";
    private DevelopmentFeatureRepairR78(){}
    private static String h(String s){return V51ChecksumEngine.sha256(s);}
    private static void need(boolean b,String reason){if(!b)throw new IllegalStateException(reason);}
    private static void check(long end){ResearchWorkBudgetR927.checkpoint();if(System.nanoTime()+50_000_000L>=end)throw new DevelopmentProductionCP12.Wait("TIME_BUDGET_EXCEEDED");}
    private static String bounded(String s){s=Objects.toString(s,"UNKNOWN");return s.substring(0,Math.min(256,s.length()));}
    static String coverageCode(String reason){
        for(String code:new String[]{"TRAIN_ALL_FEATURES_MISSING","TRAIN_FEATURE_ALL_MISSING"})
            if(code.equals(reason)||("IllegalArgumentException:"+code+"|cause=IllegalArgumentException:"+code).equals(reason))return code;
        return "";
    }
    private static final class Source {
        String task,parent,protocol,input,reason,code,cutoff,hash;int trainN,total;CandidateAstR952 ast;
    }
    private static Source source(SQLiteDatabase db,String task){
        Source s=new Source();s.task=task;
        try(Cursor c=db.rawQuery("SELECT t.candidate_id,t.stream_seq,t.input_hash,t.last_error,t.status,p.protocol_json,p.train_n,p.validation_n,p.validation_last,p.ast_hash,p.feature_manifest_hash,c.ast,c.structural_hash,c.state,p.input_hash,p.candidate_id FROM v57_research_task_r967 t JOIN research_training_protocol_v2 p ON p.protocol_hash=t.stream_seq JOIN r952_candidate c ON c.candidate_id=t.candidate_id WHERE t.task_id=? AND t.kind='TRAIN_CANDIDATE'",new String[]{task})){
            need(c.moveToFirst()&&"FAILED_TERMINAL".equals(c.getString(4))&&"QUARANTINED".equals(c.getString(13)),"DEV_REPAIR_TERMINAL_PARENT_REQUIRED");
            s.parent=c.getString(0);s.protocol=c.getString(1);s.input=c.getString(2);s.reason=c.getString(3);s.code=coverageCode(s.reason);
            need(!s.code.isEmpty(),"DEV_REPAIR_NOT_COVERAGE_FAILURE");need(s.protocol.equals(h(c.getString(5)))&&s.input.equals(c.getString(14))&&s.parent.equals(c.getString(15)),"DEV_REPAIR_PROTOCOL_IDENTITY");
            s.ast=CandidateAstR952.parse(c.getString(11));need(s.ast.formatVersion==3&&s.ast.structuralHash.equals(c.getString(9))&&s.ast.structuralHash.equals(c.getString(12))&&s.ast.graphSpec.manifest.hash.equals(c.getString(10)),"DEV_REPAIR_AST_IDENTITY");
            s.trainN=c.getInt(6);s.total=s.trainN+c.getInt(7);s.cutoff=c.getString(8);
            need(s.trainN>=300&&s.trainN<=1200&&c.getInt(7)==120&&s.total<=1320&&TargetIssueResolver.isValid(s.cutoff),"DEV_REPAIR_PROTOCOL_SHAPE");
        }
        s.hash=h("R78_DEV_REPAIR_SOURCE|"+s.task+'|'+s.parent+'|'+s.protocol+'|'+s.input+'|'+s.reason+'|'+s.ast.structuralHash+'|'+s.cutoff);return s;
    }
    private static final class Coverage {String prefix;int rows,emptyRows;int[] counts;Set<String> retained=new TreeSet<>();}
    private static Coverage coverage(SQLiteDatabase db,Source s,long end){
        Coverage out=new Coverage();int width=s.ast.graphSpec.featureOutputIds.size();need(width>=1&&width<=80,"DEV_REPAIR_FEATURE_WIDTH");out.counts=new int[width];
        out.prefix=h("CP12_FEATURE_PREFIX|"+s.task+'|'+s.protocol+'|'+s.input);
        try(Cursor c=db.rawQuery("SELECT row_index,target_issue,role,actual_number,feature_payload,feature_hash,source_receipt_hash,raw_row_hash,protocol_hash,input_hash FROM research_training_feature_row_cp11 WHERE task_id=? ORDER BY row_index LIMIT 1321",new String[]{s.task})){
            while(c.moveToNext()){
                if((out.rows&15)==0)check(end);int i=out.rows++;String role=i<s.trainN?"TRAIN":"VALIDATION";
                need(i<s.total&&i==c.getInt(0)&&role.equals(c.getString(2))&&s.protocol.equals(c.getString(8))&&s.input.equals(c.getString(9)),"DEV_REPAIR_FEATURE_SEQUENCE");
                byte[] payload=c.getBlob(4);need(DevelopmentProductionCP12.featureHashCP12(s.task,s.protocol,s.input,i,role,c.getString(1),c.getInt(3),c.getString(6),c.getString(7),payload).equals(c.getString(5)),"DEV_REPAIR_FEATURE_HASH");
                DevelopmentTrainerCP9.Row row=DevelopmentProductionCP12.decode(c.getString(1),c.getString(7),c.getInt(3),payload);boolean[] present=row.presentCopy();need(present.length==width,"DEV_REPAIR_FEATURE_WIDTH");int valid=0;
                for(int f=0;f<width;f++)if(present[f]){valid++;if(i<s.trainN)out.counts[f]++;}
                if(valid==0)out.emptyRows++;out.prefix=h(out.prefix+'|'+c.getString(5));
            }
        }
        need(out.rows==s.total,"DEV_REPAIR_MISSING_FEATURE_INPUT");boolean emptyColumn=false;for(int f=0;f<width;f++){if(out.counts[f]>0)out.retained.add(s.ast.graphSpec.featureOutputIds.get(f));else emptyColumn=true;}
        need(s.code.equals("TRAIN_ALL_FEATURES_MISSING")?out.emptyRows>0:emptyColumn,"DEV_REPAIR_DIAGNOSIS_NOT_REPRODUCED");fenceCoverage(db,s,out);return out;
    }
    private static void fenceCoverage(SQLiteDatabase db,Source s,Coverage c){
        try(Cursor row=db.rawQuery("SELECT next_index,prefix_hash,protocol_hash,input_hash,(SELECT COUNT(*) FROM research_training_feature_row_cp11 WHERE task_id=?) FROM research_training_feature_cursor_cp12 WHERE task_id=?",new String[]{s.task,s.task})){
            need(row.moveToFirst()&&row.getInt(0)==s.total&&c.prefix.equals(row.getString(1))&&s.protocol.equals(row.getString(2))&&s.input.equals(row.getString(3))&&row.getInt(4)==s.total,"DEV_REPAIR_COVERAGE_CHANGED");
        }
    }
    /** At most one original failed task is examined per existing RESEARCH turn. */
    public static boolean enqueueOne(DbStore store,long end){
        check(end);String after=store.getState(CURSOR,""),task="";
        for(int pass=0;pass<2;pass++){
            try(Cursor c=store.readable().rawQuery("SELECT task_id FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='FAILED_TERMINAL' AND task_id>? ORDER BY task_id LIMIT 1",new String[]{pass==0?after:""})){if(c.moveToFirst())task=c.getString(0);}
            if(!task.isEmpty()||after.isEmpty())break;
        }
        if(task.isEmpty())return false;
        // Advance only after a completed diagnosis/rejection; a budget yield retries the same source.
        try{
            final Source s=source(store.readable(),task);final String action=h("R78_DEV_REPAIR_ACTION|"+s.hash),finding=h("R78_DEV_REPAIR_FINDING|"+s.hash);
            if(isAction(store.readable(),action)){store.setState(CURSOR,task);return false;}
            try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(4L<<20)){
                final Coverage coverage=coverage(store.readable(),s,end);check(end);
                final CandidateAstR952 child=ModernFamilyFactoryCP12.repairFeatureCoverageR78(store.readable(),s.ast,coverage.retained);need(!child.structuralHash.equals(s.ast.structuralHash),"DEV_REPAIR_NO_NEW_STRUCTURE");
                Map<String,Object> diagnostic=new TreeMap<>();diagnostic.put("role",ROLE);diagnostic.put("source_hash",s.hash);diagnostic.put("source_task",s.task);diagnostic.put("source_protocol",s.protocol);diagnostic.put("source_input",s.input);diagnostic.put("parent_id",s.parent);diagnostic.put("parent_hash",s.ast.structuralHash);diagnostic.put("failure_code",s.code);diagnostic.put("feature_prefix",coverage.prefix);diagnostic.put("feature_rows",coverage.rows);diagnostic.put("all_missing_rows",coverage.emptyRows);List<Integer> counts=new ArrayList<>();for(int count:coverage.counts)counts.add(count);diagnostic.put("train_present_counts",counts);diagnostic.put("retained_outputs",new ArrayList<>(coverage.retained));diagnostic.put("development_cutoff",s.cutoff);
                final String evidenceJson=StrictCanonicalJson.writeContext(diagnostic),evidence=h(evidenceJson);
                Map<String,Object> proposal=new TreeMap<>(diagnostic);proposal.put("revision",OPERATOR);proposal.put("evidence_hash",evidence);proposal.put("ast",child.canonical);proposal.put("ast_hash",child.structuralHash);proposal.put("source_issue",s.cutoff);proposal.put("real_authority",false);proposal.put("future_inherited",0);
                final String wire=StrictCanonicalJson.writeContext(proposal),protocol=h(wire);need(wire.length()<=65536,"DEV_REPAIR_PROPOSAL_SIZE");check(end);
                Boolean done=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R78_DEV_REPAIR_PROPOSE",db->{
                    need(source(db,s.task).hash.equals(s.hash),"DEV_REPAIR_SOURCE_CHANGED");fenceCoverage(db,s,coverage);
                    if(isAction(db,action))return false;long now=System.currentTimeMillis();
                    db.execSQL("INSERT INTO research_finding_v2(finding_id,game_code,candidate_version,source_issue,episode_start_issue,problem_type,severity,evidence_hash,evidence_json,hypothesis,falsification_test,state,created_at,updated_at) VALUES(?,?,?,?,?,'DEVELOPMENT_FEATURE_COVERAGE','P1',?,?,?,'NEW_AST_INDEPENDENT_TRAIN_FREEZE_FUTURE','PLANNED',?,?)",new Object[]{finding,ReleaseManifestR926.GAME_CODE,s.parent,s.cutoff,s.cutoff,evidence,evidenceJson,"Measured missing-feature coverage selects a new multiscale design; no outcome is a Future claim",now,now});
                    db.execSQL("INSERT INTO research_action_v2(action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity,state,last_error) VALUES(?,?,'MUTATE',?,?,?,0,?,?,360,'','PROPOSED','')",new Object[]{action,finding,OPERATOR,wire,evidence,StrictCanonicalJson.writeContext(Collections.singletonList(s.ast.structuralHash)),protocol});
                    String boundary=TargetIssueResolver.next(s.cutoff).targetIssue;db.execSQL("INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,?)",new Object[]{action,s.cutoff,boundary,evidence,h("CP12_ACTION_BOUNDARY|"+action+'|'+s.cutoff+'|'+boundary+'|'+evidence),now});
                    ResearchTaskQueueR967.enqueue(db,"RESEARCH_ACTION",action,s.cutoff,protocol,protocol,action);return true;
                });store.setState(CURSOR,task);return Boolean.TRUE.equals(done);
            }
        }catch(DevelopmentProductionCP12.Wait|ResearchWorkBudgetR927.Yield retry){throw retry;}
        catch(IllegalArgumentException|IllegalStateException blocked){
            String pressure=retryReason(blocked);if(pressure!=null)throw new DevelopmentProductionCP12.Wait(pressure);
            store.setState("r78_dev_repair_blocker",task+'|'+bounded(blocked.getMessage()));store.setState(CURSOR,task);return false;
        }
    }
    public static boolean isAction(SQLiteDatabase db,String action){try(Cursor c=db.rawQuery("SELECT operator FROM research_action_v2 WHERE action_id=?",new String[]{action})){return c.moveToFirst()&&OPERATOR.equals(c.getString(0));}}
    private static Map<String,Object> readAction(SQLiteDatabase db,String action){
        try(Cursor c=db.rawQuery("SELECT proposal_json,protocol_hash,evidence_hash,parent_hashes_json FROM research_action_v2 WHERE action_id=? AND operator=?",new String[]{action,OPERATOR})){
            need(c.moveToFirst(),"DEV_REPAIR_ACTION_MISSING");String wire=c.getString(0);need(wire.length()<=65536&&h(wire).equals(c.getString(1)),"DEV_REPAIR_ACTION_HASH");Map<String,Object> p=StrictCanonicalJson.readContextObject(wire);
            need(ROLE.equals(str(p,"role"))&&OPERATOR.equals(str(p,"revision")),"DEV_REPAIR_ACTION_ROLE");Source s=source(db,str(p,"source_task"));
            need(s.hash.equals(str(p,"source_hash"))&&s.cutoff.equals(str(p,"development_cutoff"))&&s.cutoff.equals(str(p,"source_issue"))&&s.parent.equals(str(p,"parent_id"))&&s.ast.structuralHash.equals(str(p,"parent_hash"))&&s.protocol.equals(str(p,"source_protocol"))&&s.input.equals(str(p,"source_input"))&&s.code.equals(str(p,"failure_code")),"DEV_REPAIR_ACTION_SOURCE");
            need(action.equals(h("R78_DEV_REPAIR_ACTION|"+s.hash))&&c.getString(2).equals(str(p,"evidence_hash"))&&StrictCanonicalJson.writeContext(Collections.singletonList(s.ast.structuralHash)).equals(c.getString(3)),"DEV_REPAIR_ACTION_IDENTITY");
            Map<String,Object> diagnosis=new TreeMap<>(p);for(String key:new String[]{"revision","evidence_hash","ast","ast_hash","source_issue","real_authority","future_inherited"})diagnosis.remove(key);need(h(StrictCanonicalJson.writeContext(diagnosis)).equals(c.getString(2)),"DEV_REPAIR_DIAGNOSTIC_HASH");
            CandidateAstR952 ast=CandidateAstR952.parse(str(p,"ast"));need(ast.structuralHash.equals(str(p,"ast_hash"))&&!ast.structuralHash.equals(s.ast.structuralHash)&&ast.position==s.ast.position,"DEV_REPAIR_NEW_AST_REQUIRED");return p;
        }
    }
    /** Narrow Director exception: reuse a diagnosed design, never a quarantined model. */
    public static boolean permitsQuarantinedParent(SQLiteDatabase db,String action,String parent,String hash){
        if(!isAction(db,action))return false;Map<String,Object> p=readAction(db,action);return parent.equals(str(p,"parent_id"))&&hash.equals(str(p,"parent_hash"));
    }
    public static boolean drainClaimed(DbStore store,ResearchTaskQueueR967.Lease lease,long end){
        try{
            check(end);final Map<String,Object> p=readAction(store.readable(),lease.authorityRef);
            need(lease.candidate.equals(lease.authorityRef)&&lease.issue.equals(str(p,"source_issue"))&&lease.sequence.equals(h(StrictCanonicalJson.writeContext(p)))&&lease.inputHash.equals(lease.sequence),"DEV_REPAIR_TASK_IDENTITY");
            String latest=store.getLatestDrawIssue();if(!TargetIssueResolver.isValid(latest))throw new DevelopmentProductionCP12.Wait("WAIT_HISTORY");CandidateAstR952 ast=CandidateAstR952.parse(str(p,"ast"));String child="CP12-C-"+ast.structuralHash;
            try(DevelopmentProductionCP12.PreparedProtocol prepared=ResearchDirectorR952.prepareTrainingProtocolCP5(store,ast,child,lease.authorityRef,TargetIssueResolver.next(latest).targetIssue)){
                check(end);ResearchDirectorR952.PreparedEvolution expected=new ResearchDirectorR952.PreparedEvolution(child,lease.authorityRef,str(p,"parent_id"),lease.authorityRef,str(p,"parent_hash"),str(p,"evidence_hash"));
                return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R78_DEV_REPAIR_CHILD",db->{
                    if(!ResearchTaskQueueR967.owns(db,lease))return false;need(readAction(db,lease.authorityRef).equals(p),"DEV_REPAIR_ACTION_CHANGED");
                    String task=ResearchDirectorR952.commitPreparedCandidateCP5(db,prepared,expected);ResearchTaskQueueR967.complete(db,lease,h("R78_DEV_REPAIR_CHILD|"+lease.authorityRef+'|'+task));
                    db.execSQL("UPDATE research_finding_v2 SET state=?,updated_at=? WHERE finding_id=(SELECT finding_id FROM research_action_v2 WHERE action_id=?)",new Object[]{task.startsWith("NO_OP:")?"HOLD":"EVALUATING",System.currentTimeMillis(),lease.authorityRef});return true;
                });
            }
        }catch(ResearchWorkBudgetR927.Yield|RuntimeResourceAuditR927.BudgetExceeded retry){return defer(store,lease,"TIME_BUDGET_EXCEEDED");}
        catch(DevelopmentProductionCP12.Wait retry){return defer(store,lease,retry.reason);}
        catch(ResearchTaskQueueR967.Capacity retry){return defer(store,lease,"TASK_BACKPRESSURE");}
        catch(java.io.IOException retry){return defer(store,lease,"WAIT_INPUT_IO");}
        catch(IllegalArgumentException|IllegalStateException bad){
            String pressure=retryReason(bad);if(pressure!=null)return defer(store,lease,pressure);
            final String reason=bounded(bad.getMessage());return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R78_DEV_REPAIR_REJECT",db->{if(!ResearchTaskQueueR967.owns(db,lease))return false;db.execSQL("UPDATE research_action_v2 SET state='REJECTED',last_error=? WHERE action_id=? AND state='PROPOSED'",new Object[]{reason,lease.authorityRef});ResearchTaskQueueR967.failTerminalCP12(db,lease,reason);return true;});
        }
    }
    private static String retryReason(Throwable error){
        for(Throwable t=error;t!=null;t=t.getCause()){
            if(t instanceof ResearchWorkBudgetR927.Yield||t instanceof RuntimeResourceAuditR927.BudgetExceeded)return "TIME_BUDGET_EXCEEDED";
            if(t instanceof DevelopmentProductionCP12.Wait)return ((DevelopmentProductionCP12.Wait)t).reason;
            if(t instanceof ResearchTaskQueueR967.Capacity)return "TASK_BACKPRESSURE";
            if(t instanceof java.io.IOException)return "WAIT_INPUT_IO";
            String message=Objects.toString(t.getMessage(),"");if(message.equals("ALGORITHM_MEMORY_BUDGET"))return "MEMORY_PRESSURE";
            if(message.contains("CP3_DB_BUSY_DEADLINE")||message.contains("database is locked")||t.getClass().getSimpleName().contains("SQLiteBusy")||t.getClass().getSimpleName().contains("SQLiteDatabaseLocked"))return "DB_BUSY_RETRY";
        }return null;
    }
    private static boolean defer(DbStore s,ResearchTaskQueueR967.Lease lease,String reason){return Boolean.TRUE.equals(ResearchWorkBudgetR927.finishLeaseReceiptR66(()->s.coordinatorForResearchR952().runAtomicWrite("R78_DEV_REPAIR_WAIT",db->ResearchTaskQueueR967.waitInputCP12(db,lease,bounded(reason),1000),false)));}
    private static String str(Map<String,Object> p,String key){Object v=p.get(key);need(v instanceof String,"DEV_REPAIR_FIELD:"+key);return (String)v;}
}
