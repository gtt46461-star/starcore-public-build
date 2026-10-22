package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
/** Compatibility is exact and finite, never a wildcard, and selection matches enrollment. */
public final class ContinuationHarnessR976 {
 static int checks;
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static String invoke(String name,Class<?>[] types,Object... values)throws Exception{
  java.lang.reflect.Method m=ResearchEvaluationRunR967.class.getDeclaredMethod(name,types);m.setAccessible(true);return (String)m.invoke(null,values);
 }
 static void insertRun(SQLiteDatabase d,String rule,String id)throws Exception{
  CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20);
  String first="202609220100",initial=CandidateCompilerR952.Model.initial().payload(),artifact="";
  Class<?>[] seven={String.class,String.class,String.class,String.class,String.class,String.class};
  String epoch=invoke("originHash",seven,id,first,ast.structuralHash,rule,initial,artifact);
  Class<?>[] eight={String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class};
  String hash=invoke("runHash",eight,id,first,ast.structuralHash,rule,initial,artifact,ResearchEvaluationRunR967.SPLIT,epoch);
  d.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,future_n,created_issue,may_create_real,created_at) VALUES(?,'HOST_COMPAT',?,?,'NURSERY',0,?,0,1)",new Object[]{id,ast.canonical,ast.structuralHash,first});
  d.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,?,?,?,?,0,?,?,?,1)",new Object[]{hash,id,first,ast.structuralHash,rule,initial,artifact,ResearchEvaluationRunR967.SPLIT,epoch});
 }
 public static void main(String[] args)throws Exception{
  String[] old=ResearchSemanticsGeneratedR967.COMPATIBLE_PREDECESSOR_RULES_SHA256.split(",");
  check(old.length==2,"TWO_EXACT_REVIEWED_BASELINES");
  check(TrainingContinuationR973.compatible(TrainingCodeIdentityR964.SHA256),"CURRENT_RULE_ACCEPTED");
  check(!TrainingContinuationR973.compatible(V51ChecksumEngine.sha256("UNREVIEWED")),"UNKNOWN_RULE_REJECTED");
  check(!TrainingContinuationR973.compatible(null)&&!TrainingContinuationR973.compatible(""),"NULL_EMPTY_RULE_REJECTED");
  boolean invalid=false;try{TrainingContinuationR973.predicate("r.rule_hash OR 1=1");}catch(IllegalArgumentException e){invalid=true;}
  check(invalid,"PREDICATE_COLUMN_INJECTION_REJECTED");
  for(int i=0;i<old.length;i++){
   SQLiteDatabase d=new SQLiteDatabase(args[0]);try{
    DbStore s=HostProductR974.store(d);HostProductR974.schema(d,s);insertRun(d,old[i],"HOST_REVIEWED_"+i);
    check(TrainingContinuationR973.compatible(old[i]),"REVIEWED_RULE_ACCEPTED_"+i);
    check(ResearchDirectorR952.hasPendingCapture(s,"202609220101"),"PENDING_CAPTURE_SEES_REVIEWED_RULE_"+i);
    check(ResearchEvaluationRunR967.enroll(s,"202609220101")==0,"UPGRADE_DOES_NOT_REENROLL_REVIEWED_LEARNER_"+i);
    ResearchEvaluationRunR967.Run run=ResearchEvaluationRunR967.current(d,"HOST_REVIEWED_"+i,"202609220101");
    check(run!=null&&run.rule.equals(old[i])&&run.first.equals("202609220100")&&run.initialUpdates==0,"ORIGINAL_RUN_PROVENANCE_NOT_REWRITTEN_"+i);
   }finally{d.shutdown();}
  }
  System.out.println("R976_CONTINUATION_PASS|checks="+checks+"|certificate_migration=0|scope=SYNTHETIC_RUN_IDENTITIES_REAL_PRODUCT_SQL");
 }
}
