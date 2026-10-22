package com.ethk3.novaonepro;
import android.database.*;
import android.database.sqlite.*;
import com.ethk3.novaonepro.r952.*;
import java.util.*;
import java.nio.file.*;

/** Actual numeric stream and evolution owners on deterministic random synthetic draws.
 * Seed candidates/terminal source receipts are fixtures. Children/reviews/weights are never seeded.
 */
public final class EvolutionIntegrationHarnessR977 {
 static int checks;static long trained;
 static void check(boolean v,String n){if(!v)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static String issue(int i){return String.format(Locale.US,"20260921%04d",i);}
 static long n(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static void tx(SQLiteDatabase d,Runnable r){d.beginTransaction();try{r.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 static Models.Draw draw(SQLiteDatabase db,int i,Random rng){
  Models.Draw d=new Models.Draw();d.issue=issue(i);for(int k=0;k<10;k++)d.numbers[k]=k+1;
  for(int k=9;k>0;k--){int j=rng.nextInt(k+1),tmp=d.numbers[k];d.numbers[k]=d.numbers[j];d.numbers[j]=tmp;}
  d.openTime=System.currentTimeMillis()+1000;
  Object[] args=new Object[12];args[0]=d.issue;args[1]=d.openTime;for(int k=0;k<10;k++)args[k+2]=d.numbers[k];
  db.execSQL("INSERT INTO draws(issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0)",args);return d;
 }
 static void seed(SQLiteDatabase db,String id,CandidateAstR952 ast){
  db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES(?,'EXPLICIT_TEST_PARENT',?,?,'','NURSERY',0,?,0,1)",new Object[]{id,ast.canonical,ast.structuralHash,issue(201)});
  ResearchLifecycleR932.enrollR952(db,"r952:"+id,ast.structuralHash,issue(201));
 }
 static void train(DbStore store,SQLiteDatabase db,int i,Random rng){
  String target=issue(i);int captured=0;
  for(int page=0;page<20;page++){
   int one=ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("SYNTHETIC_PRE_DRAW_"+target),16);
   captured+=one;if(!ResearchDirectorR952.hasPendingCapture(store,target))break;
   if(page==19)throw new AssertionError("CAPTURE_NOT_DRAINED_"+i);
  }
  if(captured==0)throw new AssertionError("NO_CAPTURE_"+i);
  Models.Draw d=draw(db,i,rng);String sh=V51ChecksumEngine.sha256("SYNTHETIC_TERMINAL_"+d.issue);
  db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{d.issue,sh,d.openTime+1000});
  int labels=ResearchDirectorR952.onSettlementCompleted(store,d,sh,d.openTime+1000);
  if(labels!=captured)throw new AssertionError("CAPTURE_LABEL_MISMATCH_"+i+":"+captured+":"+labels);
  int updates=0,projections=0;
  while(updates<captured&&ResearchOutboxR952.drainOne(store))updates++;
  while(projections<captured&&PostSettlementResearchLoopR952.drainOne(store))projections++;
  if(updates!=captured||projections!=captured)throw new AssertionError("REAL_TRAINING_OR_PROJECTION_MISSING_"+i+":"+updates+":"+projections);
  trained+=updates;
  if(i%40==0)System.out.println("TRAIN issue="+target+" learners="+captured+" actual_updates="+trained);
 }
 static int evolve(DbStore store,SQLiteDatabase db,int endpoint){
  int children=0;
  for(int pulse=0;pulse<180;pulse++){
   String result=EvolutionControllerR952.evolveOne(store,issue(endpoint));
   if(result.startsWith("CHILD_REGISTERED:")){
    children++;String id=result.substring("CHILD_REGISTERED:".length()).split("\\|")[0];
    check(n(db,"SELECT future_n FROM r952_candidate WHERE candidate_id='"+id+"'")==0,"ACTUAL_CHILD_STARTS_N0_"+id);
    check(n(db,"SELECT COUNT(*) FROM r952_artifact WHERE candidate_id='"+id+"'")==0,"CHILD_DID_NOT_INHERIT_MODEL_WEIGHTS_"+id);
    System.out.println("EVOLUTION "+result);
   }
   if(result.equals("WAIT_FUTURE_EVIDENCE"))return children;
   if(result.contains("COMMIT_FAILED"))throw new AssertionError(result);
  }
  throw new AssertionError("EVOLUTION_NOT_DRAINED");
 }
 public static void main(String[] args)throws Exception{
  Path file=Path.of(args[1]);Files.deleteIfExists(file);SQLiteDatabase db=new SQLiteDatabase(args[0],file.toString());
  try{
   DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
   db.execSQL("DROP TABLE "+ResearchLifecycleR932.TABLE);
   tx(db,()->{ResearchLifecycleR932.schema(db);FixedResearchWindowR932.schema(db);seed(db,"R977-PARENT-A",new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,40));seed(db,"R977-PARENT-B",new CandidateAstR952(CandidateAstR952.Operator.MARKOV_TRANSITION,1,40));});
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
   Random rng=new Random(97720260921L);for(int i=1;i<=200;i++)draw(db,i,rng);
   check(EvolutionControllerR952.evolveOne(store,issue(200)).equals("WAIT_AUTHORITATIVE_SETTLEMENT"),"EVOLUTION_REQUIRES_TERMINAL_SETTLEMENT_AUTHORITY");
   for(int i=201;i<=320;i++)train(store,db,i,rng);
   check(trained==360,"THREE_LEARNERS_EACH_120_ACTUAL_UPDATES");
   int first=evolve(store,db,320);check(first==3,"FIRST_THREE_CHILDREN_CREATED_BY_REAL_EVOLUTION_OWNER");
   for(int i=321;i<=440;i++)train(store,db,i,rng);
   check(n(db,"SELECT COUNT(*) FROM r952_candidate WHERE parent_id<>'' AND future_n=120")==3,"ALL_FIRST_CHILDREN_TRAIN_ON_120_NEW_ISSUES");
   int second=evolve(store,db,440);check(second>=3,"SECOND_EVOLUTION_ROUND_PRODUCES_NEW_CHILDREN");
   check(n(db,"SELECT COUNT(*) FROM "+ComputationReceiptR974.LINEAGE+" WHERE parent_role='FUSION_SECONDARY'")>0,"TWO_PARENT_FUSION_FORMULAS_RETAINED");
   check(n(db,"SELECT COUNT(*) FROM r952_candidate WHERE ast LIKE 'AST2(FUSE,%'")>0,"GENUINE_COMPILED_FUSION_CHILD_EXISTS");
   check(n(db,"SELECT COUNT(*) FROM r975_evolution_review WHERE state='CONSUMED' AND verified_n=120")==first+second,"ALL_CHILDREN_HAVE_120_VERIFIED_COMPLETED_LABEL_REVIEW");
   long before=n(db,"SELECT COUNT(*) FROM r952_candidate");evolve(store,db,440);
   check(n(db,"SELECT COUNT(*) FROM r952_candidate")==before,"SAME_REVIEW_NO_DUPLICATE_CHILD");
   train(store,db,441,rng);
   check(n(db,"SELECT COUNT(*) FROM r952_candidate WHERE created_issue='"+issue(440)+"' AND future_n=1")==second,"NEW_MUTATION_AND_FUSION_CHILDREN_TRAIN_NEXT_ISSUE");
   check(n(db,"SELECT COUNT(*) FROM trades")==0,"RESEARCH_EVOLUTION_CANNOT_WRITE_REAL");
   check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"TRAINING_SAMPLES_CANNOT_BECOME_CERTIFICATES");
   check(n(db,"SELECT COUNT(*) FROM r952_artifact")==trained,"ARTIFACT_COUNT_MATCHES_ACTUAL_NUMERIC_COMMITS");
   System.out.println("R977_EVOLUTION_INTEGRATION_PASS|checks="+checks+"|numeric_commits="+trained+"|first_children="+first+"|second_children="+second+"|device=NOT_RUN|future_quality=NOT_PROVEN");
  }finally{db.shutdown();}
 }
}
