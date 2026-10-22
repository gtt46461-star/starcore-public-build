package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Uses the actual 241 public-capture failed-fit DB, never a hand-authored quality row.
 * Additional synthetic pre-CAL raw draws/canonical confirmations are test inputs only.
 * Every action, mutated AST, child, protocol and queue transition is product-owned. */
public final class CalibrationRepairR75Test {
    static int checks;
    static String hash(String x){return V51ChecksumEngine.sha256(x);}
    static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static long deadline(){return System.nanoTime()+30_000_000_000L;}
    static DbStore store(SQLiteDatabase db,File root,File assets)throws Exception{
        DbStore s=HostProductR974.store(db);root.mkdirs();
        HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,assets));
        HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));
        RealtimePressureGateR811.resetForTests();return s;
    }
    static void oldDraw(SQLiteDatabase db,String issue,int i,long at){
        db.execSQL("INSERT OR IGNORE INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?)",
            new Object[]{issue,at,at+1,(i+0)%10+1,(i+1)%10+1,(i+2)%10+1,(i+3)%10+1,(i+4)%10+1,(i+5)%10+1,(i+6)%10+1,(i+7)%10+1,(i+8)%10+1,(i+9)%10+1});
    }
    static void fixtureHistory(SQLiteDatabase db,DbStore s){
        db.beginTransaction();try{
            for(int i=1241;i<=1440;i++)oldDraw(db,String.format(Locale.ROOT,"20260922%04d",i),i,i-1240);
            for(int i=1;i<=341;i++)oldDraw(db,String.format(Locale.ROOT,"20260923%04d",i),i,i*1000L);
            for(Models.Draw d:s.getDrawsBeforeIssue("202609230422",1000))
                db.execSQL("INSERT OR IGNORE INTO "+DbHelper.TABLE_R811_DRAW_CANONICAL+"(target_issue,status,canonical_draw_hash,evidence_hash,source_count,updated_at) VALUES(?,'CONFIRMED_CANONICAL_DRAW',?,?,1,1)",new Object[]{d.issue,DrawHashR36.sha256V2(d),hash("R75_SYNTHETIC_CANONICAL:"+d.issue)});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    static void noFuture(SQLiteDatabase db){
        ok(n(db,"SELECT COUNT(*) FROM research_formal_observation_cp12")==0&&n(db,"SELECT COUNT(*) FROM research_cp4_frozen_lifetime")==0,"diagnostic creates no formal observation or lifetime");
        ok(n(db,"SELECT COUNT(*) FROM research_action_proposal_cp12")==0,"repair has no fake formal-parent proposal");
        ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"repair creates no certificate or REAL");
        ok(n(db,"SELECT SUM(future_n+may_create_real) FROM r952_candidate")==0,"all child and parent Future counters remain zero");
        ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==241,"diagnostic preserves original 241 CAL observations");
        ok(n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state IN('OBSERVING','VALIDATED','ACTIVE')")==0,"proposal does not promote registered DISCOVERED atoms");
        String frozenId=value(db,"SELECT candidate_id FROM "+ResearchEvaluationRunR967.EVAL);
        FrozenCandidateAdapterR958.Frozen frozen=FrozenCandidateAdapterR958.load(db,frozenId);
        try(AtomAdmissionCP12.Group admission=AtomAdmissionCP12.readForFrozenCP12(db,frozen.structure,frozen.model,frozen.ast,"202609230682",deadline())){
            ok(!admission.eligible,"DISCOVERED research use grants no validated atom admission without OOS");
        }
    }
    static String action(SQLiteDatabase db){return value(db,"SELECT action_id FROM research_action_v2 WHERE operator='CALIBRATION_REPAIR'");}
    static void pending(SQLiteDatabase db){
        ok(n(db,"SELECT COUNT(*) FROM research_action_v2 WHERE operator='CALIBRATION_REPAIR' AND state='PROPOSED'")==1,"failed quality creates one actual repair action");
        ok(n(db,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='RESEARCH_ACTION' AND status='PENDING'")==1,"repair enqueues existing RESEARCH_ACTION owner");
        Map<String,Object> proposal=StrictCanonicalJson.readContextObject(value(db,"SELECT proposal_json FROM research_action_v2 WHERE operator='CALIBRATION_REPAIR'"));
        CandidateAstR952 child=CandidateAstR952.parse((String)proposal.get("ast"));
        ok(child.formatVersion==3&&!child.structuralHash.equals(proposal.get("parent_hash")),"MutationGraph produces a distinct typed AST3 child");
        ok("202609230421".equals(proposal.get("development_cutoff"))&&"202609230682".equals(proposal.get("source_issue")),"CAL diagnostic endpoint and pre-CAL DEV cutoff remain distinct");
        ok("202609230683".equals(value(db,"SELECT selection_boundary FROM research_action_input_boundary_cp12")),"diagnostic selection boundary is next source issue");
    }
    public static void main(String[] a)throws Exception{
        String mode=a[2];
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            DbStore s=store(db,new File(a[3]),new File(a[4]));
            ok("FITTED_BUT_NOT_QUALIFIED".equals(value(db,"SELECT state FROM "+CandidateCalibrationR932.QUALITY_CP12)),"fixture retains actual failed-fit quality");
            if(mode.equals("baseline")){
                ResearchFeedbackCP12.drainOne(s,deadline());
                ok(n(db,"SELECT COUNT(*) FROM research_action_v2")==0,"R74 formal-only feedback misses actual failed calibration");
            }else if(mode.equals("enqueue")||mode.equals("budget")){
                fixtureHistory(db,s);
                boolean progressed=false;
                if(mode.equals("budget")){
                    int attempts=0;
                    for(;attempts<16&&!progressed;attempts++){
                        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(250);
                        long start=System.nanoTime();
                        try{progressed=ResearchFeedbackCP12.drainOne(s,start+250_000_000L);}
                        catch(ResearchWorkBudgetR927.Yield yield){System.out.println("BUDGET_YIELD "+yield.getMessage());}
                        catch(DevelopmentProductionCP12.Wait wait){if(!"TIME_BUDGET_EXCEEDED".equals(wait.reason))throw wait;System.out.println("BUDGET_WAIT "+wait.reason);}
                        finally{ResearchWorkBudgetR927.leave(scope);System.out.println("BUDGET_ATTEMPT "+attempts+" elapsed_ms="+(System.nanoTime()-start)/1_000_000L);}
                    }
                    ok(progressed,"bounded 250ms RESEARCH slices eventually persist the real proposal");
                }else progressed=ResearchFeedbackCP12.drainOne(s,deadline());
                ok(progressed,"public feedback drain consumes failed calibration");
                java.lang.reflect.Method method=ResearchFeedbackCP12.class.getDeclaredMethod("primaryMutationOperatorCP12",String.class,int.class,double.class,double.class,double.class,Double.class,boolean.class);
                method.setAccessible(true);
                ok("HYPERPARAM_MUTATION".equals(method.invoke(null,"CALIBRATION_ERROR",120,.1,Double.NaN,0d,null,true)),"CALIBRATION_ERROR finding reaches exact eligible hyperparameter operator");
                pending(db);String protocol=value(db,"SELECT protocol_hash FROM research_action_v2");
                ok(!CalibrationRepairR75.enqueueOne(s,deadline()),"second enqueue does not replay acknowledged study");
                ok(n(db,"SELECT COUNT(*) FROM research_action_v2")==1&&protocol.equals(value(db,"SELECT protocol_hash FROM research_action_v2")),"proposal remains immutable under same-process replay");
                noFuture(db);
            }else if(mode.equals("resume")){
                pending(db);String protocol=value(db,"SELECT protocol_hash FROM research_action_v2");
                ok(!CalibrationRepairR75.enqueueOne(s,deadline()),"fresh JVM does not duplicate acknowledged study");
                ok(EvolutionControllerR952.drainActionCP12(s,deadline()),"existing EVOLVE owner drains persisted repair after restart");
                String child=value(db,"SELECT child_id FROM research_action_v2 WHERE operator='CALIBRATION_REPAIR'");
                ok(!child.isEmpty()&&n(db,"SELECT COUNT(*) FROM r952_candidate WHERE state='NURSERY' AND future_n=0 AND may_create_real=0")==1,"Director registers fresh NURSERY child without inherited future");
                ok(n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==1&&n(db,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='PENDING'")==1,"Director publishes one real training protocol and TRAIN task");
                Map<String,Object> p=StrictCanonicalJson.readContextObject(value(db,"SELECT protocol_json FROM research_training_protocol_v2"));
                ok("202609230682".equals(p.get("sourceIssue"))&&"202609230683".equals(p.get("selectionBoundary"))&&"202609230421".equals(p.get("developmentCutoff")),"training protocol authenticates diagnostic and DEV boundaries");
                ok("202609230421".equals(value(db,"SELECT last_issue FROM research_training_input_v2")),"actual immutable input ends at original parent DEV cutoff");
                ok(n(db,"SELECT COUNT(*) FROM research_training_protocol_v2 WHERE validation_last='202609230421' AND train_last<embargo_issue AND embargo_issue<validation_first AND validation_first<validation_last")==1,"both train and validation remain strictly before CAL fit_first");
                ok(protocol.equals(value(db,"SELECT protocol_hash FROM research_action_v2")),"restart commits the original immutable mutation proposal");
                ok(!EvolutionControllerR952.drainActionCP12(s,deadline())&&n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==1,"completed repair cannot register duplicate child or TRAIN task");
                for(int i=0;i<3;i++)ResearchFeedbackCP12.drainOne(s,deadline());
                ok("WAIT_FUTURE".equals(value(db,"SELECT state FROM research_action_v2"))&&n(db,"SELECT COUNT(*) FROM research_action_v2")==1,"feedback resumes while child waits for its own independent future");
                noFuture(db);
            }else if(mode.equals("archive")){
                String id=action(db),protocol=value(db,"SELECT protocol_hash FROM research_action_v2"),wire=value(db,"SELECT proposal_json FROM research_action_v2"),child=value(db,"SELECT child_id FROM research_action_v2");
                db.execSQL("DELETE FROM research_projection_ack_v2 WHERE consumer='CALIBRATION_REPAIR_R75'");
                ok(n(db,"SELECT COUNT(*) FROM research_projection_ack_v2 WHERE consumer='CALIBRATION_REPAIR_R75'")==0,"normal retention can archive diagnostic projection ACK");
                ok(!CalibrationRepairR75.enqueueOne(s,deadline()),"permanent action prevents new proposal after ACK archive");
                ok(n(db,"SELECT COUNT(*) FROM research_action_v2")==1&&id.equals(action(db))&&protocol.equals(value(db,"SELECT protocol_hash FROM research_action_v2"))&&wire.equals(value(db,"SELECT proposal_json FROM research_action_v2"))&&child.equals(value(db,"SELECT child_id FROM research_action_v2")),"ACK archive preserves original action AST protocol and child");
                noFuture(db);
            }else if(mode.equals("busy")){
                pending(db);String protocol=value(db,"SELECT protocol_hash FROM research_action_v2");
                db.execSQL("CREATE TRIGGER r75_fixture_child_busy BEFORE INSERT ON r952_candidate BEGIN SELECT RAISE(ABORT,'CP3_DB_BUSY_DEADLINE'); END");
                ok(EvolutionControllerR952.drainActionCP12(s,deadline()),"EVOLVE handles failed publication transaction");
                ok("PROPOSED".equals(value(db,"SELECT state FROM research_action_v2"))&&protocol.equals(value(db,"SELECT protocol_hash FROM research_action_v2")),"transient publication busy preserves original proposed action");
                ok(n(db,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='RESEARCH_ACTION' AND status='WAIT_INPUT' AND last_error='DB_BUSY_RETRY'")==1,"nested coordinator DB_BUSY cause becomes retryable WAIT_INPUT");
                ok(n(db,"SELECT COUNT(*) FROM r952_candidate")==1&&n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==0&&n(db,"SELECT COUNT(*) FROM research_training_input_v2")==0,"failed writer atomically rolls back child protocol and input metadata");
                db.execSQL("DROP TRIGGER r75_fixture_child_busy");noFuture(db);
            }else if(mode.equals("busy_resume")){
                String protocol=value(db,"SELECT protocol_hash FROM research_action_v2");
                ok(EvolutionControllerR952.drainActionCP12(s,deadline()),"fresh JVM retries same action after storage pressure clears");
                ok("WAIT_FUTURE".equals(value(db,"SELECT state FROM research_action_v2"))&&protocol.equals(value(db,"SELECT protocol_hash FROM research_action_v2")),"busy recovery preserves proposal and reaches WAIT_FUTURE");
                ok(n(db,"SELECT COUNT(*) FROM r952_candidate WHERE state='NURSERY'")==1&&n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==1&&n(db,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='PENDING'")==1,"busy recovery commits exactly one child protocol and TRAIN task");
                noFuture(db);
            }else if(mode.equals("parent_changed")){
                db.execSQL("UPDATE r952_candidate SET state='QUARANTINED' WHERE candidate_id='r75-actual-capture'");
                ok(EvolutionControllerR952.drainActionCP12(s,deadline()),"EVOLVE consumes invalidated parent action");
                ok("REJECTED".equals(value(db,"SELECT state FROM research_action_v2"))&&"CAL_REPAIR_PARENT_CHANGED".equals(value(db,"SELECT last_error FROM research_action_v2")),"changed parent is explicitly rejected");
                ok(n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==0&&n(db,"SELECT COUNT(*) FROM r952_candidate")==1,"changed parent publishes no child or DEV input");
                noFuture(db);
            }else if(mode.equals("invalid_cutoff")){
                Map<String,Object> p=new TreeMap<>(StrictCanonicalJson.readContextObject(value(db,"SELECT proposal_json FROM research_action_v2")));p.put("development_cutoff","202609230682");
                String wire=StrictCanonicalJson.writeContext(p);boolean immutable=false;
                try{db.execSQL("UPDATE research_action_v2 SET proposal_json=?,protocol_hash=?",new Object[]{wire,hash(wire)});}catch(IllegalStateException expected){immutable=expected.getMessage().contains("CP5_RESEARCH_ACTION_IDENTITY_IMMUTABLE");}
                ok(immutable,"self-rehashed action cannot move immutable DEV cutoff into CAL labels");
                boolean rejected=false;try{CalibrationRepairR75.developmentCutoff(db,action(db),"202609230421");}catch(IllegalStateException expected){rejected="CAL_REPAIR_SELECTION_BOUNDARY".equals(expected.getMessage());}
                ok(rejected,"caller cannot substitute DEV cutoff for diagnostic selection boundary");
                ok(n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==0,"invalid cutoff publishes no DEV protocol");
                noFuture(db);
            }else throw new IllegalArgumentException(mode);
            Files.writeString(Path.of(a[5]),"{\"status\":\"PASS\",\"mode\":\""+mode+"\",\"checks\":"+checks+",\"scope\":\"ACTUAL_FAILED_CALIBRATION_DIAGNOSTIC_TO_CHILD_HOST_SYNTHETIC_INPUT_NO_LIVE_QUALITY\"}\n");
        }
    }
}
