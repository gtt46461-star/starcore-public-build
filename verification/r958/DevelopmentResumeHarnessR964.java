package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import org.json.*;
/** Real reported programs, controlled clock interruptions, actual VM; no live evidence. */
public final class DevelopmentResumeHarnessR964 {
 static int checks;
 static void check(boolean x,String name){if(!x)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=DevelopmentTrainingHarnessR959.init(db);Random random=new Random(964);String issue="202609190001";
   for(int i=0;i<121;i++){DevelopmentTrainingHarnessR959.draw(db,issue,random);if(i<120)issue=TargetIssueResolver.next(issue).targetIssue;}
   List<Models.Draw> history=store.getDrawsBeforeIssue(issue,120);Models.Draw label=store.getDrawByIssue(issue);
   JSONObject root=new JSONObject(new String(Files.readAllBytes(Paths.get("verification/r958/reported_r963_world.json")),"UTF-8"));
   JSONObject table=root.getJSONObject(DbHelper.TABLE_R908_ALGORITHM_GENOME);JSONArray rows=table.getJSONArray("rows");
   int spec=ReportedCohortHarnessR963.column(table,"genome_spec");
   ArrayList<AlgorithmGenomeR908> genomes=new ArrayList<>();ArrayList<AlgorithmGenomeVmR908.State> states=new ArrayList<>(),expected=new ArrayList<>();ArrayList<AlgorithmInventionLabR908.DevelopmentStep> outputs=new ArrayList<>();
   for(int i=0;i<rows.length();i++){
    AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(ReportedCohortHarnessR963.value(rows.getJSONArray(i),spec));genomes.add(g);
    AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.initialState(g),reference=AlgorithmGenomeVmR908.initialState(g);states.add(state);expected.add(reference);
    outputs.add(AlgorithmInventionLabR908.trainDevelopmentStep(g,reference,history,label,Long.MAX_VALUE));
   }
   check(genomes.size()==29,"EXACT_NEW_REPORT_29_ALGORITHMS");
   boolean[] done=new boolean[29];int yields=0,completed=0,round=0;
   android.os.SystemClock.startTicksForTests();
   try{for(;round<20&&completed<29;round++)for(int i=0;i<29;i++)if(!done[i]){
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(220);
    try{
     AlgorithmInventionLabR908.DevelopmentStep output=AlgorithmInventionLabR908.trainDevelopmentStep(store,genomes.get(i),states.get(i),history,label,Long.MAX_VALUE);
     check(output.ordinal==outputs.get(i).ordinal&&output.hit==outputs.get(i).hit&&Double.doubleToLongBits(output.score)==Double.doubleToLongBits(outputs.get(i).score)&&states.get(i).encode().equals(expected.get(i).encode()),"INTERRUPTED_FULL1200_BIT_PARITY:"+i);
     done[i]=true;completed++;
    }catch(ResearchWorkBudgetR927.Yield y){yields++;if(states.get(i).updates!=0)throw new AssertionError("PARTIAL_LABEL_MUST_NOT_UPDATE_STATE");}
    finally{ResearchWorkBudgetR927.leave(scope);}
   }}finally{android.os.SystemClock.endTicksForTests();}
   check(completed==29&&yields>=29&&round>1,"ALL_29_ROUND_ROBIN_PARTIAL_SCANS_RESUME");
   // Change a historical value at the same issue after partial arithmetic. Old input must be discarded.
   AlgorithmGenomeR908 g=genomes.get(0);AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.initialState(g);
   try{AlgorithmInventionLabR908.trainDevelopmentStep(store,g,state,history,label,0);throw new AssertionError("EXPECTED_YIELD");}catch(ResearchWorkBudgetR927.Yield expectedYield){}
   int a=history.get(0).numbers[0];history.get(0).numbers[0]=history.get(0).numbers[1];history.get(0).numbers[1]=a;
   AlgorithmGenomeVmR908.State reference=AlgorithmGenomeVmR908.initialState(g);
   AlgorithmInventionLabR908.DevelopmentStep fresh=AlgorithmInventionLabR908.trainDevelopmentStep(g,reference,history,label,Long.MAX_VALUE);
   AlgorithmInventionLabR908.DevelopmentStep corrected=AlgorithmInventionLabR908.trainDevelopmentStep(store,g,state,history,label,Long.MAX_VALUE);
   check(state.encode().equals(reference.encode())&&fresh.ordinal==corrected.ordinal,"SAME_ISSUE_CORRECTED_HISTORY_INVALIDATES_PENDING_SCAN");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW)==0,"PARTIAL_OR_DEV_ARITHMETIC_CREATES_NO_FUTURE_PREDICTIONS");
   System.out.println("R964_DEVELOPMENT_RESUME_PASS|checks="+checks+"|yields="+yields+"|rounds="+round+"|NOT_PHONE_TIMING");
  }finally{db.shutdown();}
 }
}
