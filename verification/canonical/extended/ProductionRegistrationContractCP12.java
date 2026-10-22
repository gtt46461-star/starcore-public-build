package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
/** Executes actual modern registration and feature-page transactions on real host SQLite.
 * Android DbStore/coordinator/service scheduling itself remains an Android integration check. */
public final class ProductionRegistrationContractCP12 {
 static int checks;
 static void ok(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static void failure(Throwing op)throws Exception {checks++;try{op.run();}catch(RuntimeException expected){return;}throw new AssertionError("accepted invalid operation");}
 interface Throwing{void run()throws Exception;}
 public static void main(String[] a)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);File root=new File(a[2]);root.mkdirs();
  MemoryBudgetCP4 budget=new MemoryBudgetCP4(8L<<20);
  CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(ModernModelContractTest.astRoot(ModelKind.FIXED_GRAPH,CandidateAstR952.Family.FREQUENCY)));
  List<Models.Draw> history=new ArrayList<>();String issue="202609230001";
  for(int i=0;i<441;i++){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=1000L+60000L*i;for(int j=0;j<10;j++)d.numbers[j]=(i+j)%10+1;history.add(d);issue=TargetIssueResolver.next(issue).targetIssue;}
  List<Models.Draw> selected=DevelopmentProductionCP12.selectHistoryCP12(history,0,true);ok(selected.size()==441);ok(selected.get(440).issue.equals(history.get(440).issue));
  List<Models.Draw> many=new ArrayList<>();for(int i=0;i<5;i++)many.addAll(history);List<Models.Draw> tail=DevelopmentProductionCP12.selectHistoryCP12(many,0,true);ok(tail.size()==441&&tail.get(0).issue.equals(history.get(0).issue));ok(DevelopmentProductionCP12.selectHistoryCP12(many,0,false).size()==many.size());failure(()->DevelopmentProductionCP12.selectHistoryCP12(history.subList(0,440),0,true));
  String registration=issue,evidence=V51ChecksumEngine.sha256("registration evidence");
  Map<String,Object> h=new TreeMap<>();h.put("revision","SCDEV1");h.put("game","PK10");h.put("build","R980");h.put("sourceManifestHash",evidence);h.put("scheduleRevision","TARGET_ISSUE_RESOLVER_R980");h.put("bindings",Collections.emptyList());
  DevelopmentInputCP9.Header header=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(h));
  DevelopmentInputCP9.Receipt casIdentity=DevelopmentInputCP9.write(new OutputStream(){public void write(int b){}public void write(byte[] b,int off,int n){}},header,history);
  try(TrainingCasCP12.LockedFile cas=TrainingCasCP12.publish(root,TrainingCasCP12.INPUT_SUFFIX,casIdentity.inputHash,casIdentity.byteCount,out->DevelopmentInputCP9.write(out,header,history).inputHash);
      InputStream stream=cas.open();DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(stream,cas.bytes,cas.hash,header,budget)) {
   Constructor<DevelopmentProductionCP12.PreparedProtocol> ctor=DevelopmentProductionCP12.PreparedProtocol.class.getDeclaredConstructor(CandidateAstR952.class,String.class,String.class,String.class,TrainingCasCP12.LockedFile.class,DevelopmentInputCP9.Header.class,DevelopmentInputCP9.Input.class,List.class,long.class);ctor.setAccessible(true);
   DevelopmentProductionCP12.PreparedProtocol p=ctor.newInstance(ast,"newChild","action",registration,cas,header,input,Collections.emptyList(),1L);
   db.beginTransaction();try{DevelopmentProductionCP12.schema(db);RetentionWorkingStorageQuotaCP12.schema(db);RetentionArtifactIdentityCP12.install(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   db.execSQL("INSERT INTO draws(issue,dirty) VALUES(?,0)",new Object[]{history.get(440).issue});
   db.execSQL("INSERT INTO research_finding_v2 VALUES('finding','PK10','seed',?,?,'FUTURE_DEFICIT','P1',?,'{}','hypothesis','falsification','PLANNED',1,1)",new Object[]{history.get(440).issue,history.get(440).issue,evidence});
   db.execSQL("INSERT INTO research_action_v2 VALUES('action','finding','NEW_FAMILY','CREATE','{}',?,42,'[]','experiment',120,'alpha','PROPOSED',NULL,'')",new Object[]{evidence});
   String boundary=registration,boundaryHash=V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|action|"+history.get(440).issue+'|'+boundary+'|'+evidence);db.execSQL("INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,1)",new Object[]{"action",history.get(440).issue,boundary,evidence,boundaryHash});
   ResearchDirectorR952.PreparedEvolution child=new ResearchDirectorR952.PreparedEvolution("newChild","action","","intent","",evidence);
   failure(()->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,child));
   db.beginTransaction();try{String task=ResearchDirectorR952.commitPreparedCandidateCP5(db,p,child);ok(!task.isEmpty());}finally{db.endTransaction();}
   ok(count(db,"r952_candidate")==0);ok(count(db,"research_training_protocol_v2")==0);ok(count(db,ResearchTaskQueueR967.TABLE)==0);
   db.beginTransaction();try{db.execSQL("WITH RECURSIVE n(x) AS(SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<4096) INSERT INTO v57_research_task_r967(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,created_at,updated_at) SELECT 'fill:'||x,'POST_TRAIN','fill:'||x,'202609230001','fill:'||x,'input','authority','PENDING',1,1 FROM n");
    checks++;try{ResearchDirectorR952.commitPreparedCandidateCP5(db,p,child);throw new AssertionError("capacity accepted");}catch(ResearchTaskQueueR967.Capacity expected){}
   }finally{db.endTransaction();}
   ok(count(db,"r952_candidate")==0);ok(count(db,"research_training_input_v2")==0);
   db.beginTransaction();try{db.execSQL("UPDATE draws SET open_time=99 WHERE issue=?",new Object[]{history.get(440).issue});failure(()->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,child));}finally{db.endTransaction();}
   ok(count(db,"r952_candidate")==0);
   String task;db.beginTransaction();try{task=ResearchDirectorR952.commitPreparedCandidateCP5(db,p,child);db.setTransactionSuccessful();}finally{db.endTransaction();}
   ok(count(db,"r952_candidate")==1);ok(count(db,"research_training_input_v2")==1);ok(count(db,"research_training_protocol_v2")==1);ok(count(db,ResearchTaskQueueR967.TABLE)==1);
   try(Cursor c=db.rawQuery("SELECT future_n,may_create_real FROM r952_candidate",null)){ok(c.moveToFirst()&&c.getInt(0)==0&&c.getInt(1)==0);}
   try(Cursor c=db.rawQuery("SELECT state FROM research_action_v2 WHERE action_id='action'",null)){ok(c.moveToFirst()&&"WAIT_FUTURE".equals(c.getString(0)));}
   ResearchTaskQueueR967.Lease lease;db.beginTransaction();try{lease=ResearchTaskQueueR967.claim(db,"TRAIN_CANDIDATE",System.currentTimeMillis());db.setTransactionSuccessful();}finally{db.endTransaction();}ok(lease!=null);
   Method read=DevelopmentProductionCP12.class.getDeclaredMethod("readProtocol",SQLiteDatabase.class,ResearchTaskQueueR967.Lease.class);read.setAccessible(true);DevelopmentProductionCP12.Protocol protocol=(DevelopmentProductionCP12.Protocol)read.invoke(null,db,lease);
   DevelopmentProductionCP12.FeaturePrefix prefix=new DevelopmentProductionCP12.FeaturePrefix(0,V51ChecksumEngine.sha256("CP12_FEATURE_PREFIX|"+lease.id+'|'+lease.sequence+'|'+lease.inputHash));
   double[] values=new double[10];Arrays.fill(values,.1);boolean[] masks=new boolean[10];Arrays.fill(masks,true);
   DevelopmentTrainerCP9.Row r=new DevelopmentTrainerCP9.Row(input.issue(p.trainFirst),evidence,values,masks,input.actual(p.trainFirst,1));
   DevelopmentProductionCP12.FeatureRow row=DevelopmentProductionCP12.FeatureRow.of(lease,0,"TRAIN",r,evidence);String next=V51ChecksumEngine.sha256(prefix.hash+'|'+row.hash);
   Method commit=DevelopmentProductionCP12.class.getDeclaredMethod("commitFeaturePage",SQLiteDatabase.class,ResearchTaskQueueR967.Lease.class,DevelopmentProductionCP12.Protocol.class,DevelopmentProductionCP12.FeaturePrefix.class,List.class,String.class);commit.setAccessible(true);
   db.beginTransaction();try{ok((Boolean)commit.invoke(null,db,lease,protocol,prefix,Collections.singletonList(row),next));}finally{db.endTransaction();}
   ok(count(db,"research_training_feature_row_cp11")==0);ok(count(db,"research_training_feature_cursor_cp12")==0);
   db.beginTransaction();try{ok((Boolean)commit.invoke(null,db,lease,protocol,prefix,Collections.singletonList(row),next));db.setTransactionSuccessful();}finally{db.endTransaction();}
   ok(count(db,"research_training_feature_row_cp11")==1);ok(count(db,"research_training_feature_cursor_cp12")==1);
   try(Cursor c=db.rawQuery("SELECT status,cursor FROM v57_research_task_r967 WHERE task_id=?",new String[]{lease.id})){ok(c.moveToFirst()&&"YIELDED".equals(c.getString(0))&&("FEATURES|1|"+next).equals(c.getString(1)));}
   failure(()->db.execSQL("DELETE FROM research_training_feature_row_cp11 WHERE task_id=?",new Object[]{lease.id}));
   failure(()->db.execSQL("UPDATE research_training_feature_row_cp11 SET actual_number=1 WHERE task_id=?",new Object[]{lease.id}));
   byte[] encoded=DevelopmentProductionCP12.encode(values,masks);DevelopmentTrainerCP9.Row decoded=DevelopmentProductionCP12.decode(r.issue,r.inputHash,r.actual,encoded);ok(Arrays.equals(values,decoded.valuesCopy()));
   byte[] truncated=Arrays.copyOf(encoded,encoded.length-1);failure(()->DevelopmentProductionCP12.decode(r.issue,r.inputHash,r.actual,truncated));
   double[] nan=values.clone();nan[0]=Double.NaN;failure(()->DevelopmentProductionCP12.encode(nan,masks));
   boolean[] absent=masks.clone();absent[0]=false;failure(()->DevelopmentProductionCP12.encode(values,absent));
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
   ok(ResearchTrainingHookRegistryR952.commitFailureReasonCP12(new RuntimeException("SQLITE MODEL_STORAGE_CAPACITY"),"TRAIN_COMMIT_FAILED").equals("MODEL_STORAGE_CAPACITY"));ok(ResearchTrainingHookRegistryR952.storageHoldCP12("MODEL_STORAGE_CAPACITY"));
   for(String code:new String[]{"TRAINING_WORKING_STORAGE_CAPACITY","TRAINING_WORKING_STORAGE_LEGACY_OVER_BUDGET"}){ok(ResearchTrainingHookRegistryR952.commitFailureReasonCP12(new RuntimeException("SQLITE "+code),"TRAIN_COMMIT_FAILED").equals(code));ok(ResearchTrainingHookRegistryR952.storageHoldCP12(code));}
   for(String code:new String[]{"MODEL_STORAGE_HASH_SIZE_CONFLICT","MODEL_STORAGE_SINGLE_PAYLOAD_BOUND","TRAINING_WORKING_SINGLE_PAYLOAD_BOUND"}){ok(ResearchTrainingHookRegistryR952.commitFailureReasonCP12(new RuntimeException("SQLITE "+code),"TRAIN_COMMIT_FAILED").equals(code));ok(ResearchTrainingHookRegistryR952.terminalFailureCP12(code));ok(!ResearchTrainingHookRegistryR952.storageHoldCP12(code));}
   db.beginTransaction();try{ResearchTaskQueueR967.Lease held=ResearchTaskQueueR967.claim(db,"TRAIN_CANDIDATE",System.currentTimeMillis()+2000);ok(held!=null);ok(ResearchTaskQueueR967.waitInputCP12(db,held,"MODEL_STORAGE_CAPACITY",60000));db.setTransactionSuccessful();}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT status,cursor,last_error FROM v57_research_task_r967 WHERE task_id=?",new String[]{lease.id})){ok(c.moveToFirst()&&"WAIT_INPUT".equals(c.getString(0))&&("FEATURES|1|"+next).equals(c.getString(1))&&"MODEL_STORAGE_CAPACITY".equals(c.getString(2)));}
   db.beginTransaction();try{ResearchTaskQueueR967.Lease doomed=ResearchTaskQueueR967.claim(db,"TRAIN_CANDIDATE",System.currentTimeMillis()+65000);ok(doomed!=null);ok(ResearchDirectorR952.failCandidateTaskCP12(db,doomed,"TEST_INTEGRITY_FAILURE"));}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT state FROM research_action_v2 WHERE action_id='action'",null)){ok(c.moveToFirst()&&"WAIT_FUTURE".equals(c.getString(0)));}
   db.beginTransaction();try{ResearchTaskQueueR967.Lease doomed=ResearchTaskQueueR967.claim(db,"TRAIN_CANDIDATE",System.currentTimeMillis()+65000);ok(doomed!=null);ok(ResearchDirectorR952.failCandidateTaskCP12(db,doomed,"TEST_INTEGRITY_FAILURE"));db.setTransactionSuccessful();}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT state FROM research_action_v2 WHERE action_id='action'",null)){ok(c.moveToFirst()&&"REJECTED".equals(c.getString(0)));}
   try(Cursor c=db.rawQuery("SELECT state FROM research_finding_v2 WHERE finding_id='finding'",null)){ok(c.moveToFirst()&&"ERROR".equals(c.getString(0)));}
   try(Cursor c=db.rawQuery("SELECT state FROM r952_candidate WHERE candidate_id='newChild'",null)){ok(c.moveToFirst()&&"QUARANTINED".equals(c.getString(0)));}
   p.close();failure(()->p.requireOpen());
  }finally{db.shutdown();}
  ok(budget.used()==0);Files.writeString(Path.of(a[3]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual target registration/feature-page SQL, SCDEV/CAS publication and shared-file lifetime on host SQLite/JVM; Android Os directory fsync uses a host FileChannel adapter. No service/device scheduling claim.\"}\n");
 }
}
