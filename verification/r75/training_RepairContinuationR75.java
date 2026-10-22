package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;

/** Continues the product-owned child from the actual failed-fit owner fixture.
 * Pre-CAL development draws are labelled synthetic inputs by its fixture builder.
 * This harness never inserts labels, quality rows, training rows, artifacts or freezes. */
public final class training_RepairContinuationR75 {
    static int checks;
    static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;System.out.println("PASS "+why);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static DbStore owner(SQLiteDatabase db,String[] a)throws Exception{
        DbStore s=HostProductR974.store(db);File root=new File(a[2]);root.mkdirs();
        HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,new File(a[3])));
        HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));return s;
    }
    static long childCount(SQLiteDatabase db,String sql,String child){try(Cursor c=db.rawQuery(sql,new String[]{child})){return c.moveToFirst()?c.getLong(0):0;}}
    static void pulse(SQLiteDatabase db,String[] a)throws Exception{
        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
        try{ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(owner(db,a),System.nanoTime()+775_000_000L);}
        catch(ResearchWorkBudgetR927.Yield expected){}finally{ResearchWorkBudgetR927.leave(scope);}
    }
    public static void main(String[]a)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            RealtimePressureGateR811.resetForTests();String child,cutoff,source,fit;int trainN;
            try(Cursor c=db.rawQuery("SELECT a.child_id,a.proposal_json,p.train_n,p.protocol_json,i.last_issue,p.validation_last,p.train_last FROM research_action_v2 a JOIN research_training_protocol_v2 p ON p.candidate_id=a.child_id JOIN research_training_input_v2 i ON i.input_hash=p.input_hash WHERE a.operator='CALIBRATION_REPAIR' AND a.state='WAIT_FUTURE'",null)){
                ok(c.moveToFirst(),"PRODUCT_REGISTERED_REPAIR_CHILD_REQUIRED");child=c.getString(0);trainN=c.getInt(2);
                Map<String,Object> action=StrictCanonicalJson.readContextObject(c.getString(1)),protocol=StrictCanonicalJson.readContextObject(c.getString(3));
                cutoff=(String)action.get("development_cutoff");source=(String)action.get("source_issue");
                try(Cursor cal=db.rawQuery("SELECT model_json FROM "+CandidateCalibrationR932.MODEL+" WHERE study_hash=?",new String[]{(String)action.get("study")})){ok(cal.moveToFirst(),"ACTUAL_CALIBRATION_MODEL_REQUIRED");fit=new JSONObject(cal.getString(0)).getString("fit_first");}
                ok(cutoff.equals(c.getString(4))&&cutoff.equals(c.getString(5))&&c.getString(6).compareTo(cutoff)<0&&cutoff.compareTo(fit)<0,"TRAIN_AND_VALIDATION_END_BEFORE_PARENT_CAL_FIT");
                ok(source.equals(protocol.get("sourceIssue"))&&cutoff.equals(protocol.get("developmentCutoff"))&&TargetIssueResolver.next(source).targetIssue.equals(protocol.get("selectionBoundary")),"DIAGNOSTIC_SELECTION_AND_DEV_INPUT_REMAIN_DISTINCT");
            }
            long originalCal=n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS);
            ok(originalCal==241,"ACTUAL_FAILED_FIT_241_OBSERVATIONS_PRESERVED");
            long discovered=n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='DISCOVERED'"),requiresBinding=n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='REQUIRES_BINDING'");
            ok(discovered>0&&discovered+requiresBinding==n(db,"SELECT COUNT(*) FROM research_atom_spec_v2"),"REAL_UNPROMOTED_REGISTRY_RETAINED");
            int turns=0;long started=System.nanoTime();
            for(;turns<450&&childCount(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL+" WHERE learner_id=?",child)==0;turns++){
                pulse(db,a);
                if(turns%20==0)System.out.println("PULSE "+turns+" rows="+n(db,"SELECT COUNT(*) FROM research_training_feature_row_cp11"));
                if(childCount(db,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE candidate_id=? AND kind IN('TRAIN_CANDIDATE','FREEZE_CANDIDATE') AND status='FAILED_TERMINAL'",child)>0)throw new AssertionError("TERMINAL_TRAIN_OR_FREEZE_FAILURE");
                Thread.sleep(100);
            }
            String frozenId,modelHash,createdIssue;
            try(Cursor c=db.rawQuery("SELECT candidate_id,trained_until_issue,created_issue,model_hash FROM "+ResearchEvaluationRunR967.EVAL+" WHERE learner_id=?",new String[]{child})){
                ok(c.moveToFirst(),"REPAIR_CHILD_REACHES_ACTUAL_FROZEN_MODEL");frozenId=c.getString(0);modelHash=c.getString(3);createdIssue=c.getString(2);
                ok(cutoff.equals(c.getString(1))&&createdIssue.equals(TargetIssueResolver.next(source).targetIssue),"NEW_FREEZE_USES_PRE_CAL_MODEL_AND_POST_DIAGNOSTIC_TARGET");
            }
            FrozenCandidateAdapterR958.Frozen frozen=FrozenCandidateAdapterR958.load(db,frozenId);
            ok(frozen!=null&&!frozen.model.isLegacy()&&frozen.model.modern().kind==CandidateCompilerR952.ModelKind.FIXED_GRAPH&&frozen.model.modern().trainedUntil.equals(cutoff)&&frozen.model.updates==trainN,"REAL_FIXED_GRAPH_MODEL_LOADS_WITH_OWN_DEVELOPMENT_RECEIPT");
            ok(childCount(db,"SELECT COUNT(*) FROM r952_artifact WHERE candidate_id=? AND training_kind='DEVELOPMENT_AST3'",child)==1,"EXACTLY_ONE_REAL_CHILD_DEVELOPMENT_ARTIFACT");
            ok(childCount(db,"SELECT COUNT(*) FROM research_development_receipt_v2 d JOIN research_training_protocol_v2 p ON p.protocol_hash=d.protocol_hash WHERE p.candidate_id=?",child)==1,"EXACTLY_ONE_REAL_CHILD_DEVELOPMENT_RECEIPT");
            ok(childCount(db,"SELECT COUNT(*) FROM research_training_feature_row_cp11 f JOIN research_training_protocol_v2 p ON p.protocol_hash=f.protocol_hash WHERE p.candidate_id=?",child)==trainN+120,"ALL_TRAIN_AND_VALIDATION_FEATURE_ROWS_COMPUTED");
            try(Cursor c=db.rawQuery("SELECT MAX(f.target_issue) FROM research_training_feature_row_cp11 f JOIN research_training_protocol_v2 p ON p.protocol_hash=f.protocol_hash WHERE p.candidate_id=?",new String[]{child})){ok(c.moveToFirst()&&cutoff.equals(c.getString(0)),"NO_CALIBRATION_LABEL_ENTERED_FEATURE_CACHE");}
            for(int i=0;i<6;i++)pulse(db,a);
            ok(childCount(db,"SELECT COUNT(*) FROM r952_artifact WHERE candidate_id=?",child)==1&&childCount(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL+" WHERE learner_id=?",child)==1,"REPLAY_DOES_NOT_DUPLICATE_CHILD_ARTIFACT_OR_FREEZE");
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==originalCal,"TRAINING_DOES_NOT_REWRITE_CALIBRATION_OBSERVATIONS");
            ok(n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='DISCOVERED'")==discovered&&n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='REQUIRES_BINDING'")==requiresBinding,"TRAINING_DOES_NOT_PROMOTE_ATOM_REGISTRY");
            ok(n(db,"SELECT SUM(future_n+may_create_real) FROM r952_candidate")==0&&n(db,"SELECT COUNT(*) FROM research_formal_observation_cp12")==0&&n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM trades")==0,"NO_INHERITED_FUTURE_CERTIFICATE_OR_REAL");
            Map<String,Object> result=new TreeMap<>();result.put("status","PASS");result.put("checks",checks);result.put("training_pulses",turns);result.put("elapsed_ms",(System.nanoTime()-started)/1_000_000);result.put("child",child);result.put("frozenId",frozenId);result.put("modelHash",modelHash);result.put("trainN",trainN);result.put("validationN",120);result.put("developmentCutoff",cutoff);result.put("calibrationFitFirst",fit);result.put("diagnosticSource",source);result.put("createdIssue",createdIssue);result.put("scope","ACTUAL_CAL_REPAIR_OWNER_CONTINUATION_SYNTHETIC_DEV_INPUT_HOST_NO_LIVE_QUALITY");
            Files.writeString(Path.of(a[4]),StrictCanonicalJson.writeContext(result));System.out.println(StrictCanonicalJson.writeContext(result));
        }
    }
}
