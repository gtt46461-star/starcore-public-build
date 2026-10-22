package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.nio.file.*;
/** Synthetic eligibility fixtures; actual creation, VM training, SQLite retirement and restart. */
public final class PopulationEvolutionHarnessR966 {
 static int checks;static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
 static long n(SQLiteDatabase d,String q){return DevelopmentTrainingHarnessR959.n(d,q);}
 static void eligible(SQLiteDatabase db,AlgorithmGenomeR908 g,String issue){
  String key="algorithm:"+g.genomeId,e=V51ChecksumEngine.sha256("SYNTHETIC_ELIGIBILITY_NOT_LIVE|"+key+'|'+issue);
  db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET future_n=120,future_hit=90 WHERE genome_id=?",new Object[]{g.genomeId});
  db.execSQL("UPDATE "+ResearchLifecycleR932.TABLE+" SET state='OBSERVING',future_n=120,paired_n=120,evidence_hash=?,last_issue=? WHERE candidate_key=?",new Object[]{e,issue,key});
  db.execSQL("INSERT OR REPLACE INTO "+ResearchLifecycleR932.ELITE+" VALUES(?,?,120,0.75,0.1,0.02,0.2,'SYNTHETIC_SELECTION_NOT_CERTIFICATE',?,?)",new Object[]{key,g.structuralHash,e,System.currentTimeMillis()});
 }
 public static void main(String[] args)throws Exception{
  Path file=Files.createTempFile("r966-population-",".sqlite");SQLiteDatabase db=new SQLiteDatabase(args[0],file.toString());
  try{
   DbStore store=DevelopmentTrainingHarnessR959.init(db);Random random=new Random(966);String issue="202609200001";
   for(int i=0;i<121;i++){DevelopmentTrainingHarnessR959.draw(db,issue,random);if(i<120)issue=TargetIssueResolver.next(issue).targetIssue;}
   AlgorithmGenomeR908 anchor=AlgorithmGenomeR908.seed(96601,1,"FIXTURE"),parent=AlgorithmGenomeR908.seed(96602,1,"FIXTURE");
   db.beginTransaction();try{
    for(int i=0;i<64;i++){
     AlgorithmGenomeR908 g=i==0?anchor:i==1?parent:AlgorithmGenomeR908.seed(96600+i+100,1,"CAPACITY_FIXTURE");
     AlgorithmInventionLabR908.registerDirected(db,g,issue,TargetIssueResolver.next(issue).targetIssue,"FIXTURE","SYNTHETIC_CAPACITY_METADATA");
     ResearchLifecycleR932.enroll(db,"algorithm:"+g.genomeId,g.structuralHash,issue,"B");
    }
    check(ResearchPopulationR966.active(db)==64,"INITIAL_CAPACITY_64");
    check(ResearchPopulationR966.replacement(db)==null,"NO_EVICTION_OF_UNOBSERVED_NEWBORNS");
    db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET future_n=120,future_hit=40");
    db.execSQL("UPDATE "+ResearchLifecycleR932.TABLE+" SET state='SHADOW'");
    check(ResearchPopulationR966.replacement(db)==null,"CERTIFIED_STAGES_PROTECTED");
    db.execSQL("UPDATE "+ResearchLifecycleR932.TABLE+" SET state='OBSERVING'");
    eligible(db,anchor,issue);eligible(db,parent,issue);db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   String victim=ResearchPopulationR966.replacement(db);check(victim!=null&&!victim.equals(anchor.genomeId)&&!victim.equals(parent.genomeId),"ELITE_PARENTS_PROTECTED");
   // A failure in retirement must roll back the new program, attempt and every side effect.
   db.execSQL("CREATE TRIGGER fail_r966_retire BEFORE UPDATE ON "+ResearchLifecycleR932.TABLE+" WHEN NEW.state='RESOURCE_RETIRED' BEGIN SELECT RAISE(ABORT,'INJECTED_R966_RETIRE_FAILURE'); END");
   boolean failed=false;try{DirectedResearchCreationR932.maintain(store,"algorithm");}catch(IllegalStateException e){failed=true;}
   check(failed&&n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT)==0&&ResearchPopulationR966.active(db)==64,"FAILED_RETIREMENT_ROLLS_BACK_CHILD_AND_ATTEMPT");
   db.execSQL("DROP TRIGGER fail_r966_retire");
   HashSet<String> structures=new HashSet<String>();long started=System.currentTimeMillis();
   for(int generation=2;generation<=101;generation++){
    if(generation>2){issue=TargetIssueResolver.next(issue).targetIssue;DevelopmentTrainingHarnessR959.draw(db,issue,random);}
    db.beginTransaction();try{db.execSQL("DELETE FROM "+ResearchLifecycleR932.ELITE);eligible(db,anchor,issue);eligible(db,parent,issue);db.setTransactionSuccessful();}finally{db.endTransaction();}
    String result=DirectedResearchCreationR932.maintain(store,"algorithm");check(result.contains("ELITE_PROGRAM_CROSSOVER"),"FUSION_ADMISSION_AT_CAPACITY:"+generation);
    AlgorithmGenomeR908 child;try(Cursor c=db.rawQuery("SELECT g.genome_spec,g.future_n,g.state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g JOIN "+DirectedResearchCreationR932.ATTEMPT+" a ON a.child_key='algorithm:'||g.genome_id WHERE a.created_issue=?",new String[]{issue})){
     check(c.moveToFirst(),"CHILD_COMMITTED");child=AlgorithmGenomeR908.parse(c.getString(0));check(c.getInt(1)==0&&c.getInt(2)==0,"NO_INHERITED_FUTURE_OR_UPDATES");
    }
    check(child.generation==generation&&child.parentIds.contains(parent.genomeId)&&child.parentIds.contains(anchor.genomeId)&&structures.add(child.structuralHash),"NOVEL_PROGRAM_AND_REAL_PARENTS:"+generation);
    AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.initialState(child);List<Models.Draw> past=store.getDrawsBeforeIssue(issue,76);
    for(int j=64;j<76;j++)AlgorithmInventionLabR908.trainDevelopmentStep(child,state,past.subList(0,j),past.get(j),Long.MAX_VALUE);
    check(state.updates==12&&child.scoreProgram.size()<=AlgorithmGenomeR908.MAX_PROGRAM&&child.updateProgram.size()<=AlgorithmGenomeR908.MAX_PROGRAM,"FULL1200_VM_TRAINING_AND_BOUNDED_PROGRAM:"+generation);
    check(ResearchPopulationR966.active(db)==64,"COMMITTED_POPULATION_BOUNDED:"+generation);
    long count=n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT);DirectedResearchCreationR932.maintain(store,"algorithm");
    check(n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT)==count,"SAME_DRAW_NO_SECOND_ATTEMPT");
    parent=child;
    if(generation==51){db.shutdown();db=new SQLiteDatabase(args[0],file.toString());store=new DbStore(db);check(ResearchPopulationR966.active(db)==64,"REOPEN_DATABASE_AT_GENERATION_51");}
    if(generation%10==1)System.out.println("R966_GENERATION_PROGRESS|generation="+generation+"|active="+ResearchPopulationR966.active(db));
   }
   check(n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE state='RESOURCE_RETIRED'")==100,"ONE_HUNDRED_RESOURCE_REPLACEMENTS");
   String retired="";try(Cursor c=db.rawQuery("SELECT candidate_key FROM "+ResearchLifecycleR932.TABLE+" WHERE state='RESOURCE_RETIRED' LIMIT 1",null)){c.moveToFirst();retired=c.getString(0);}
   db.beginTransaction();try{ResearchLifecycleR932.review(db,retired,issue);check(ResearchLifecycleR932.tier(db,retired,"B").isEmpty(),"RETIRED_NOT_RESURRECTED_OR_SCHEDULED");boolean blocked=false;try{ResearchLifecycleR932.transition(db,retired,ResearchLifecycleR932.State.OBSERVING,"TEST",issue,V51ChecksumEngine.sha256("TEST"),0,0);}catch(IllegalStateException e){blocked=true;}check(blocked,"ORDINARY_TRANSITION_CANNOT_REOPEN_RETIRED");db.setTransactionSuccessful();}finally{db.endTransaction();}
   check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_CERTIFICATES_FROM_SYNTHETIC_TRAINING");
   db.beginTransaction();try{
    for(int k=0;k<12;k++){
     AlgorithmGenomeR908 extra=AlgorithmGenomeR908.seed(396600+k,1,"LEGACY_OVERCAPACITY_FIXTURE");
     AlgorithmInventionLabR908.registerDirected(db,extra,issue,TargetIssueResolver.next(issue).targetIssue,"FIXTURE","LEGACY_OVERCAPACITY");
     ResearchLifecycleR932.enroll(db,"algorithm:"+extra.genomeId,extra.structuralHash,issue,"B");
     db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET future_n=120,future_hit=0 WHERE genome_id=?",new Object[]{extra.genomeId});
    }
    check(ResearchPopulationR966.active(db)==76,"LEGACY_OVERCAPACITY_FIXTURE");
    check(!ResearchPopulationR966.normalize(db,issue)&&ResearchPopulationR966.active(db)==68,"LEGACY_DRAIN_BOUNDED_TO_EIGHT");
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   store=new DbStore(db);
   db.beginTransaction();try{check(ResearchPopulationR966.normalize(db,issue)&&ResearchPopulationR966.active(db)==64,"LEGACY_DRAIN_RESUMES_NEXT_SLICE");db.setTransactionSuccessful();}finally{db.endTransaction();}
   System.out.println("R966_POPULATION_EVOLUTION_PASS|checks="+checks+"|generations=1_TO_101|created=100|vmUpdates=1200|active=64|replacements=100|restartAt=51|elapsedMs="+(System.currentTimeMillis()-started)+"|SYNTHETIC_ELIGIBILITY_AND_DEV_LABELS_NOT_100_LIVE_GENERATIONS");
  }finally{db.shutdown();Files.deleteIfExists(file);}
 }
}
