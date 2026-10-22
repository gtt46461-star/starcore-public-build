package com.ethk3.novaonepro;
import java.lang.reflect.*;
/** Deterministic clock slices exercise the actual scorer and 1200-action proof builder. */
public final class ResumableScoringHarnessR960 {
 static int checks;
 static void check(boolean ok,String n){if(!ok)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 public static void main(String[] args)throws Exception {
  Class<?> observation=Class.forName("com.ethk3.novaonepro.AlgorithmInventionLabR908$RuntimeObservation"),scanClass=Class.forName("com.ethk3.novaonepro.AlgorithmInventionLabR908$RuntimeScan");
  Constructor<?> observationCtor=observation.getDeclaredConstructors()[0];observationCtor.setAccessible(true);
  Object rows=Array.newInstance(observation,1200);int ordinal=0;
  for(DeferredEvidenceSnapshotR36.ActionRow a:ReportedCaptureHarnessR945.snapshot("202609190001","202609190002").actions){
   double[] features=new double[AlgorithmGenomeR908.INPUTS];for(int j=0;j<features.length;j++)features[j]=(ordinal+j)%97/97.0;
   V51Models.PositionAction action=new V51Models.PositionAction(a.position,a.n1,a.n2,a.n3,a.n4,.3,.3,0,0,0,0,features,"",ordinal+1,"");
   Array.set(rows,ordinal,observationCtor.newInstance(action,features,0.1,ordinal));ordinal++;
  }
  Constructor<?> scanCtor=scanClass.getDeclaredConstructors()[0];scanCtor.setAccessible(true);
  Method scorer=AlgorithmInventionLabR908.class.getDeclaredMethod("selectRuntime",AlgorithmGenomeR908.class,AlgorithmGenomeVmR908.State.class,rows.getClass(),long.class,scanClass);scorer.setAccessible(true);
  AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(960123L,1,"RESUME_TEST");AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.initialState(g);
  Object scan=scanCtor.newInstance(g,state,1200),result=null;int pulses=0;
  RealtimePressureGateR811.resetForTests();android.os.SystemClock.startTicksForTests();
  try{
   for(;pulses<30&&result==null;pulses++){
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(80);
    try{result=scorer.invoke(null,g,state,rows,Long.MAX_VALUE,scan);}
    catch(InvocationTargetException e){if(!(e.getCause() instanceof ResearchWorkBudgetR927.Yield))throw e;}
    finally{ResearchWorkBudgetR927.leave(scope);}
    if(pulses==0){int cursor=(Integer)field(scan,"cursor");check(cursor>0&&cursor<1200,"TIME_SLICE_STOPS_AFTER_REAL_PARTIAL_SCORING");check(field(field(scan,"best"),"audit")==null,"PARTIAL_SCORING_HAS_NO_FULL_PROOF");}
   }
  }finally{android.os.SystemClock.endTicksForTests();}
  check(result!=null&&pulses>1&&pulses<30&&(Integer)field(scan,"cursor")==1200,"MULTIPLE_SLICES_FINISH_ALL_1200_WITHOUT_RESTARTING");
  Object uninterrupted=scorer.invoke(null,g,state,rows,Long.MAX_VALUE,scanCtor.newInstance(g,state,1200));
  CandidateExecutionAuditR932.Proof a=(CandidateExecutionAuditR932.Proof)field(result,"audit"),b=(CandidateExecutionAuditR932.Proof)field(uninterrupted,"audit");
  check(a.scoreHash.equals(b.scoreHash)&&a.rankingHash.equals(b.rankingHash)&&a.selectedOrdinal==b.selectedOrdinal,"RESUMED_SCORES_RANKING_AND_SELECTION_MATCH_UNINTERRUPTED");
  Method matches=scanClass.getDeclaredMethod("matches",AlgorithmGenomeR908.class,AlgorithmGenomeVmR908.State.class);matches.setAccessible(true);state.updates++;
  check(!(Boolean)matches.invoke(scan,g,state),"CHANGED_LEARNER_STATE_INVALIDATES_PARTIAL_SCAN");
  System.out.println("R960_RESUMABLE_SCORING_PASS|checks="+checks+"|scope=ACTUAL_SCORER_DETERMINISTIC_CLOCK_NOT_PHONE");
 }
}
