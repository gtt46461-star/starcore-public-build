package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.util.*;
import org.json.*;

/** A failed calibration is an aggregate hypothesis signal, never a development label or
 * a formal observation. The child trains only before its parent's calibration began and
 * obtains entirely new prospective evidence. Uses the existing Director and action queue. */
public final class CalibrationRepairR75 {
    private CalibrationRepairR75() {}
    public static final String OPERATOR="CALIBRATION_REPAIR";
    private static final String ROLE="CALIBRATION_DIAGNOSTIC_NOT_DEV_OR_FORMAL", CONSUMER="CALIBRATION_REPAIR_R75";
    private static final String CURSOR="r75_calibration_repair_cursor";
    private static final class Source {
        String study,key,calHash,first,until,cutoff,evidence,json;
        FrozenCandidateAdapterR958.Frozen frozen;
        double ece,brier,baseline;
    }
    private static Source source(SQLiteDatabase db,String study) {
        CandidateCalibrationR932.Qualification q=CandidateCalibrationR932.qualificationForStudyCP12(db,study);
        need(q.state==CandidateCalibrationR932.QualificationState.FITTED_BUT_NOT_QUALIFIED&&"CALIBRATION_QUALITY_BLOCKED".equals(q.reason),"CAL_REPAIR_QUALITY_REQUIRED");
        Source s=new Source();s.study=study;s.until=q.validationUntil;s.ece=q.ece;s.brier=q.brier;s.baseline=q.baselineBrier;
        try(Cursor c=db.rawQuery("SELECT candidate_key,model_hash,model_json FROM "+CandidateCalibrationR932.MODEL+" WHERE study_hash=?",new String[]{study})){
            need(c.moveToFirst(),"CAL_REPAIR_MODEL_MISSING");s.key=c.getString(0);s.calHash=c.getString(1);
            need(s.key.startsWith("algorithm:"),"CAL_REPAIR_MODERN_KEY_REQUIRED");
            try{JSONObject model=new JSONObject(c.getString(2));s.first=model.getString("fit_first");}catch(JSONException bad){throw new IllegalStateException("CAL_REPAIR_MODEL_JSON",bad);}
        }
        s.frozen=FrozenCandidateAdapterR958.load(db,s.key.substring(10));
        need(s.frozen!=null&&s.frozen.ast.formatVersion==3&&!s.frozen.model.isLegacy()&&TrainingContinuationR973.compatible(s.frozen.rule),"CAL_REPAIR_CURRENT_AST3_REQUIRED");
        s.cutoff=s.frozen.trainedUntil;need(s.cutoff.equals(s.frozen.model.modern().trainedUntil),"CAL_REPAIR_MODEL_CUTOFF_IDENTITY");
        need(TargetIssueResolver.isValid(s.first)&&s.cutoff.compareTo(s.first)<0&&s.first.compareTo(s.until)<0,"CAL_REPAIR_NO_DEV_CAL_OVERLAP");
        try(Cursor c=db.rawQuery("SELECT structural_hash,state,ast FROM r952_candidate WHERE candidate_id=?",new String[]{s.frozen.learner})){
            need(c.moveToFirst()&&s.frozen.ast.structuralHash.equals(c.getString(0))&&s.frozen.ast.canonical.equals(c.getString(2))&&!Arrays.asList("QUARANTINED","QUALITY_PRUNED","RESOURCE_RETIRED").contains(c.getString(1)),"CAL_REPAIR_PARENT_CHANGED");
        }
        Map<String,Object> data=new TreeMap<>();data.put("role",ROLE);data.put("study",study);data.put("candidate_key",s.key);data.put("calibration_hash",s.calHash);data.put("quality_policy",CandidateCalibrationR932.QUALITY_POLICY_HASH_CP12);data.put("parent_id",s.frozen.learner);data.put("parent_ast_hash",s.frozen.ast.structuralHash);data.put("freeze_hash",s.frozen.structure);data.put("parent_model_hash",s.frozen.modelHash);data.put("calibration_first",s.first);data.put("source_issue",s.until);data.put("development_cutoff",s.cutoff);data.put("ece_hex",Double.toHexString(s.ece));data.put("brier_hex",Double.toHexString(s.brier));data.put("baseline_brier_hex",Double.toHexString(s.baseline));
        s.json=StrictCanonicalJson.writeContext(data);s.evidence=V51ChecksumEngine.sha256(s.json);return s;
    }
    /** One indexed quality row per pulse. Cursor advances even for retired/incompatible sources. */
    public static boolean enqueueOne(DbStore store,long deadlineNs) {
        check(deadlineNs);String after=store.getState(CURSOR,""),study="";
        for(int pass=0;pass<2;pass++){
            try(Cursor c=store.readable().rawQuery("SELECT study_hash FROM "+CandidateCalibrationR932.QUALITY_CP12+" WHERE state='FITTED_BUT_NOT_QUALIFIED' AND study_hash>? ORDER BY study_hash LIMIT 1",new String[]{pass==0?after:""})){if(c.moveToFirst())study=c.getString(0);}
            if(!study.isEmpty()||after.isEmpty())break;
        }
        if(study.isEmpty())return false;
        store.setState(CURSOR,study);
        try(Cursor c=store.readable().rawQuery("SELECT 1 FROM research_projection_ack_v2 WHERE consumer=? AND source_hash=?",new String[]{CONSUMER,study})){if(c.moveToFirst())return false;}
        final Source s;
        try{s=source(store.readable(),study);}catch(IllegalArgumentException|IllegalStateException invalid){store.setState("r75_calibration_repair_status","SOURCE_BLOCKED|"+study+'|'+bounded(invalid.getMessage()));return false;}
        final String finding=V51ChecksumEngine.sha256("R75_CAL_FINDING|"+s.evidence),action=V51ChecksumEngine.sha256("R75_CAL_ACTION|"+s.evidence);
        // Projection ACKs are archiveable; the immutable action is the durable idempotency authority.
        if(isAction(store.readable(),action)){readAction(store.readable(),action);return false;}
        final MutationGraphCP12.Proposal proposal;final String mutation;
        try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(4L<<20)){
            List<AtomSpecCP4> registry=registry(store.readable(),s.frozen.ast,deadlineNs,memory.bytes);
            Set<String> occupied=new AbstractSet<String>(){public int size(){return 0;}public Iterator<String> iterator(){return Collections.<String>emptyList().iterator();}public boolean contains(Object hash){try(Cursor c=store.readable().rawQuery("SELECT 1 FROM r952_candidate WHERE structural_hash=? LIMIT 1",new String[]{String.valueOf(hash)})){return c.moveToFirst();}}};
            // ECE diagnoses shrinkage; graphs without a registered alpha pair use an actual
            // consumed window change. Poor Brier uses a consumed feature replacement.
            String op=s.ece>.04?"HYPERPARAM_MUTATION":"FEATURE_REPLACE";
            MutationGraphCP12.Proposal p=MutationGraphCP12.propose(s.frozen.ast,null,action,op,registry,occupied,Collections.emptyList(),null,deadlineNs);
            if(p.ast==null&&op.equals("HYPERPARAM_MUTATION")){op="WINDOW_MUTATION";p=MutationGraphCP12.propose(s.frozen.ast,null,action,op,registry,occupied,Collections.emptyList(),null,deadlineNs);}
            proposal=p;mutation=op;
        }
        check(deadlineNs);
        Map<String,Object> fields=new TreeMap<>();fields.put("revision","R75_CALIBRATION_REPAIR_1");fields.put("role",ROLE);fields.put("study",s.study);fields.put("source_evidence",s.evidence);fields.put("source_issue",s.until);fields.put("development_cutoff",s.cutoff);fields.put("parent_id",s.frozen.learner);fields.put("parent_hash",s.frozen.ast.structuralHash);fields.put("freeze_hash",s.frozen.structure);fields.put("calibration_hash",s.calHash);fields.put("mutation_operator",mutation);fields.put("ast",proposal.ast==null?"":proposal.ast.canonical);fields.put("ast_hash",proposal.ast==null?"":proposal.ast.structuralHash);fields.put("attempt",proposal.attempt);fields.put("proposal_reason",proposal.reason);fields.put("real_authority",false);fields.put("future_inherited",0);
        final String wire=StrictCanonicalJson.writeContext(fields),protocol=V51ChecksumEngine.sha256(wire);need(wire.length()<=65536,"CAL_REPAIR_ACTION_SIZE");
        Boolean done=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R75_CAL_REPAIR_PROPOSE",db->{
            Source current=source(db,s.study);need(current.evidence.equals(s.evidence),"CAL_REPAIR_SOURCE_CHANGED");
            try(Cursor c=db.rawQuery("SELECT result_hash FROM research_projection_ack_v2 WHERE consumer=? AND source_hash=?",new String[]{CONSUMER,s.study})){if(c.moveToFirst()){need(c.getString(0).equals(protocol),"CAL_REPAIR_REPLAY_DRIFT");return true;}}
            long now=System.currentTimeMillis();
            db.execSQL("INSERT INTO research_finding_v2(finding_id,game_code,candidate_version,source_issue,episode_start_issue,problem_type,severity,evidence_hash,evidence_json,hypothesis,falsification_test,state,created_at,updated_at) VALUES(?,?,?,?,?,'CALIBRATION_QUALITY_FAILURE','P2',?,?,?,'NEW_CHILD_INDEPENDENT_DEVELOPMENT_CALIBRATION_AND_FORMAL','PLANNED',?,?)",new Object[]{finding,ReleaseManifestR926.GAME_CODE,s.frozen.id,s.until,s.first,s.evidence,s.json,"Aggregate calibration failure selects a typed repair hypothesis; no labels reused for development",now,now});
            db.execSQL("INSERT INTO research_action_v2(action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity,state,child_id,last_error) VALUES(?,?,'MUTATE',?,?,?,0,?,?,360,'',?,NULL,?)",new Object[]{action,finding,OPERATOR,wire,s.evidence,StrictCanonicalJson.writeContext(Collections.singletonList(s.frozen.ast.structuralHash)),protocol,proposal.ast==null?"NO_OP":"PROPOSED",proposal.ast==null?proposal.reason:""});
            String boundary=TargetIssueResolver.next(s.until).targetIssue;
            db.execSQL("INSERT INTO research_action_input_boundary_cp12(action_id,source_issue,selection_boundary,evidence_hash,boundary_hash,created_at)VALUES(?,?,?,?,?,?)",new Object[]{action,s.until,boundary,s.evidence,V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+action+'|'+s.until+'|'+boundary+'|'+s.evidence),now});
            if(proposal.ast!=null)ResearchTaskQueueR967.enqueue(db,"RESEARCH_ACTION",action,s.until,protocol,protocol,action);
            else db.execSQL("UPDATE research_finding_v2 SET state='HOLD' WHERE finding_id=?",new Object[]{finding});
            db.execSQL("INSERT INTO research_projection_ack_v2(consumer,source_hash,result_hash,through_issue,created_at) VALUES(?,?,?,?,?)",new Object[]{CONSUMER,s.study,protocol,s.until,now});return true;
        });
        return Boolean.TRUE.equals(done);
    }
    private static List<AtomSpecCP4> registry(SQLiteDatabase db,CandidateAstR952 ast,long deadline,long limit){
        // These repairs change a registered shrinkage/window/feature within a consumed
        // family. Loading unrelated world atoms would exhaust the bounded research slice.
        TreeSet<String> families=new TreeSet<>();
        for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes)if(node.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM){
            check(deadline);try(Cursor c=db.rawQuery("SELECT family FROM research_atom_spec_v2 WHERE atom_id=? AND position=?",new String[]{node.source,Integer.toString(ast.position)})){need(c.moveToFirst(),"CAL_REPAIR_ATOM_MISSING");families.add(c.getString(0));}
        }
        need(!families.isEmpty(),"CAL_REPAIR_CONSUMED_ATOM_REQUIRED");List<String> args=new ArrayList<>();args.add(Integer.toString(ast.position));args.addAll(families);
        String placeholders=String.join(",",Collections.nCopies(families.size(),"?"));List<AtomSpecCP4> rows=new ArrayList<>();long bytes=0;
        try(Cursor c=db.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE position=? AND family IN("+placeholders+") AND state IN('DISCOVERED','OBSERVING','VALIDATED','ACTIVE','DOWNWEIGHTED') ORDER BY atom_id LIMIT 4097",args.toArray(new String[0]))){while(c.moveToNext()){check(deadline);bytes+=512+2L*c.getString(2).length();need(rows.size()<4096&&bytes<=limit,"CAL_REPAIR_REGISTRY_BOUND");rows.add(AtomSpecCP4.fromRegistry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getString(5),c.getString(6)));}}return rows;
    }
    public static boolean isAction(SQLiteDatabase db,String action){try(Cursor c=db.rawQuery("SELECT operator FROM research_action_v2 WHERE action_id=?",new String[]{action})){return c.moveToFirst()&&OPERATOR.equals(c.getString(0));}}
    private static Map<String,Object> readAction(SQLiteDatabase db,String action){try(Cursor c=db.rawQuery("SELECT proposal_json,protocol_hash,evidence_hash,parent_hashes_json FROM research_action_v2 WHERE action_id=? AND operator=?",new String[]{action,OPERATOR})){
        need(c.moveToFirst(),"CAL_REPAIR_ACTION_MISSING");String wire=c.getString(0);need(wire.length()<=65536&&V51ChecksumEngine.sha256(wire).equals(c.getString(1)),"CAL_REPAIR_ACTION_HASH");Map<String,Object> p=StrictCanonicalJson.readContextObject(wire);need(ROLE.equals(str(p,"role"))&&"R75_CALIBRATION_REPAIR_1".equals(str(p,"revision")),"CAL_REPAIR_ACTION_ROLE");
        Source s=source(db,str(p,"study"));need(s.evidence.equals(c.getString(2))&&s.evidence.equals(str(p,"source_evidence"))&&s.until.equals(str(p,"source_issue"))&&s.cutoff.equals(str(p,"development_cutoff"))&&s.calHash.equals(str(p,"calibration_hash"))&&s.frozen.structure.equals(str(p,"freeze_hash"))&&s.frozen.learner.equals(str(p,"parent_id"))&&s.frozen.ast.structuralHash.equals(str(p,"parent_hash"))&&StrictCanonicalJson.writeContext(Collections.singletonList(s.frozen.ast.structuralHash)).equals(c.getString(3)),"CAL_REPAIR_SOURCE_IDENTITY");
        need(action.equals(V51ChecksumEngine.sha256("R75_CAL_ACTION|"+s.evidence)),"CAL_REPAIR_ACTION_ID");return p;
    }}
    /** Selection uses the diagnostic endpoint; development is strictly earlier than CAL. */
    public static String developmentCutoff(SQLiteDatabase db,String action,String selectionSource){
        if(!isAction(db,action))return selectionSource;
        Map<String,Object> p=readAction(db,action);need(selectionSource.equals(str(p,"source_issue")),"CAL_REPAIR_SELECTION_BOUNDARY");return str(p,"development_cutoff");
    }
    /** Runs under the one existing EVOLVE lease. No new formal lifetime or proposal row. */
    public static boolean drainClaimed(DbStore store,ResearchTaskQueueR967.Lease lease,long deadline){
        try{
            check(deadline);Map<String,Object> p=readAction(store.readable(),lease.authorityRef);
            need(lease.authorityRef.equals(lease.candidate)&&lease.issue.equals(str(p,"source_issue"))&&lease.sequence.equals(V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(p)))&&lease.inputHash.equals(lease.sequence),"CAL_REPAIR_TASK_IDENTITY");
            CandidateAstR952 ast=CandidateAstR952.parse(str(p,"ast"));need(ast.formatVersion==3&&ast.structuralHash.equals(str(p,"ast_hash"))&&!ast.structuralHash.equals(str(p,"parent_hash")),"CAL_REPAIR_NEW_AST_REQUIRED");
            String latest="";try(Cursor c=store.readable().rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}if(!TargetIssueResolver.isValid(latest))throw new DevelopmentProductionCP12.Wait("WAIT_HISTORY");
            String child="CP12-C-"+ast.structuralHash;
            try(DevelopmentProductionCP12.PreparedProtocol prepared=ResearchDirectorR952.prepareTrainingProtocolCP5(store,ast,child,lease.authorityRef,TargetIssueResolver.next(latest).targetIssue)){
                check(deadline);ResearchDirectorR952.PreparedEvolution expected=new ResearchDirectorR952.PreparedEvolution(child,lease.authorityRef,str(p,"parent_id"),lease.authorityRef,str(p,"parent_hash"),str(p,"source_evidence"));
                return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R75_CAL_REPAIR_CHILD",db->{if(!ResearchTaskQueueR967.owns(db,lease))return false;Map<String,Object> current=readAction(db,lease.authorityRef);need(current.equals(p),"CAL_REPAIR_ACTION_CHANGED");String task=ResearchDirectorR952.commitPreparedCandidateCP5(db,prepared,expected);ResearchTaskQueueR967.complete(db,lease,V51ChecksumEngine.sha256("R75_CAL_CHILD|"+lease.authorityRef+'|'+task));db.execSQL("UPDATE research_finding_v2 SET state=?,updated_at=? WHERE finding_id=(SELECT finding_id FROM research_action_v2 WHERE action_id=?)",new Object[]{task.startsWith("NO_OP:")?"HOLD":"EVALUATING",System.currentTimeMillis(),lease.authorityRef});return true;});
            }
        }catch(ResearchWorkBudgetR927.Yield|RuntimeResourceAuditR927.BudgetExceeded retry){defer(store,lease,"TIME_BUDGET_EXCEEDED");return true;
        }catch(DevelopmentProductionCP12.Wait retry){defer(store,lease,retry.reason);return true;
        }catch(ResearchTaskQueueR967.Capacity retry){defer(store,lease,"TASK_BACKPRESSURE");return true;
        }catch(java.io.IOException retry){defer(store,lease,"WAIT_INPUT_IO");return true;
        }catch(IllegalArgumentException|IllegalStateException bad){String retry=retryReason(bad);if(retry!=null){defer(store,lease,retry);return true;}final String reason=bounded(bad.getMessage());return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R75_CAL_REPAIR_REJECT",db->{if(!ResearchTaskQueueR967.owns(db,lease))return false;db.execSQL("UPDATE research_action_v2 SET state='REJECTED',last_error=? WHERE action_id=? AND state='PROPOSED'",new Object[]{reason,lease.authorityRef});db.execSQL("UPDATE research_finding_v2 SET state='ERROR',updated_at=? WHERE finding_id=(SELECT finding_id FROM research_action_v2 WHERE action_id=?)",new Object[]{System.currentTimeMillis(),lease.authorityRef});ResearchTaskQueueR967.failTerminalCP12(db,lease,reason);return true;});}
    }
    /** Coordinator failures preserve the original cause. Scheduling and storage pressure
     * must not become a statistical/integrity rejection after the writer rolled back. */
    private static String retryReason(Throwable failure){
        boolean wrapped=false;
        for(Throwable c=failure;c!=null;c=c.getCause()){
            if(c instanceof ResearchWorkBudgetR927.Yield||c instanceof RuntimeResourceAuditR927.BudgetExceeded)return "TIME_BUDGET_EXCEEDED";
            if(c instanceof DevelopmentProductionCP12.Wait)return ((DevelopmentProductionCP12.Wait)c).reason;
            if(c instanceof ResearchTaskQueueR967.Capacity)return "TASK_BACKPRESSURE";
            if(c instanceof java.io.IOException)return "WAIT_INPUT_IO";
            String message=Objects.toString(c.getMessage(),"");
            if(message.equals("ALGORITHM_MEMORY_BUDGET"))return "MEMORY_PRESSURE";
            if(message.contains("CP3_DB_BUSY_DEADLINE")||message.contains("database is locked")||c.getClass().getSimpleName().contains("SQLiteDatabaseLocked")||c.getClass().getSimpleName().contains("SQLiteBusy"))return "DB_BUSY_RETRY";
            if(message.startsWith("F01_WRITE_FAILED")&&c.getCause()==null)wrapped=true;
        }
        return wrapped?"COORDINATOR_DEFERRED":null;
    }
    private static void defer(DbStore store,ResearchTaskQueueR967.Lease lease,String reason){ResearchWorkBudgetR927.finishLeaseReceiptR66(()->store.coordinatorForResearchR952().runAtomicWrite("R75_CAL_REPAIR_YIELD",db->ResearchTaskQueueR967.waitInputCP12(db,lease,bounded(reason),1000),false));}
    private static String str(Map<String,Object> p,String key){Object v=p.get(key);need(v instanceof String,"CAL_REPAIR_FIELD:"+key);return (String)v;}
    private static String bounded(String s){s=Objects.toString(s,"UNKNOWN_ERROR");return s.length()>256?s.substring(0,256):s;}
    private static void need(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private static void check(long deadline){ResearchWorkBudgetR927.checkpoint();if(System.nanoTime()>=deadline)throw new DevelopmentProductionCP12.Wait("TIME_BUDGET_EXCEEDED");}
}
