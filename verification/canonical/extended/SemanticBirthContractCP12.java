package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;

/** Real root AST/semantic identity, schema and Director commit; no mirror writer. */
public final class SemanticBirthContractCP12 {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("semantic assertion "+checks);}
 interface Op{void run()throws Exception;}
 static void bad(Op op,String reason)throws Exception{checks++;try{op.run();}catch(RuntimeException e){if(reason.isEmpty()||String.valueOf(e).contains(reason))return;throw e;}throw new AssertionError("invalid accepted: "+reason);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static String error(SQLiteDatabase db,String id){try(Cursor c=db.rawQuery("SELECT last_error FROM research_action_v2 WHERE action_id=?",new String[]{id})){if(!c.moveToFirst())throw new AssertionError(id);return c.getString(0);}}
 static String state(SQLiteDatabase db,String id){try(Cursor c=db.rawQuery("SELECT state FROM research_action_v2 WHERE action_id=?",new String[]{id})){if(!c.moveToFirst())throw new AssertionError(id);return c.getString(0);}}
 @SuppressWarnings("unchecked") static CandidateAstR952 ast(Map<String,Object> root){((Map<String,Object>)root.get("featureManifest")).put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));return CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));}
 static Map<String,Object> fixed(){return ModernModelContractTest.astRoot(ModelKind.FIXED_GRAPH,CandidateAstR952.Family.FREQUENCY);}
 static void action(SQLiteDatabase db,String id,String source,String evidence){
  db.execSQL("INSERT INTO research_finding_v2 VALUES(?,?,?, ?,?,'FUTURE_DEFICIT','P1',?,'{}','hypothesis','falsification','PLANNED',1,1)",new Object[]{"finding:"+id,"PK10","seed:"+id,source,source,evidence});
  db.execSQL("INSERT INTO research_action_v2 VALUES(?,?,'NEW_FAMILY','CREATE','{}',?,42,'[]','experiment',120,'alpha','PROPOSED',NULL,'')",new Object[]{id,"finding:"+id,evidence});
  String boundary=TargetIssueResolver.next(source).targetIssue;
  db.execSQL("INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,1)",new Object[]{id,source,boundary,evidence,V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+id+'|'+source+'|'+boundary+'|'+evidence)});
 }
 static DevelopmentProductionCP12.PreparedProtocol protocol(CandidateAstR952 a,String cid,String action,String issue,File root,String hash,DevelopmentInputCP9.Header header,DevelopmentInputCP9.Input input,long generation)throws Exception{
  Constructor<DevelopmentProductionCP12.PreparedProtocol> ctor=DevelopmentProductionCP12.PreparedProtocol.class.getDeclaredConstructor(CandidateAstR952.class,String.class,String.class,String.class,TrainingCasCP12.LockedFile.class,DevelopmentInputCP9.Header.class,DevelopmentInputCP9.Input.class,List.class,long.class);ctor.setAccessible(true);
  return ctor.newInstance(a,cid,action,issue,TrainingCasCP12.open(root,hash,TrainingCasCP12.INPUT_SUFFIX),header,input,Collections.emptyList(),generation);
 }
 static ResearchDirectorR952.PreparedEvolution expected(String cid,String action,String evidence){return new ResearchDirectorR952.PreparedEvolution(cid,action,"","intent:"+action,"",evidence);}
 static String commit(SQLiteDatabase db,DevelopmentProductionCP12.PreparedProtocol p,ResearchDirectorR952.PreparedEvolution child,boolean successful){db.beginTransaction();try{String result=ResearchDirectorR952.commitPreparedCandidateCP5(db,p,child);if(successful)db.setTransactionSuccessful();return result;}finally{db.endTransaction();}}
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
  CandidateAstR952 base=ast(fixed());Map<String,Object> taxonomy=fixed();taxonomy.put("family","OMISSION");CandidateAstR952 same=ast(taxonomy);
  ok(!base.structuralHash.equals(same.structuralHash));ok(base.semanticHashCP12.equals(same.semanticHashCP12));ok(base.semanticHashCP12.matches("[0-9a-f]{64}"));
  Map<String,Object> numeric=fixed();((List<Object>)((Map<String,Object>)((List<?>)numeric.get("nodes")).get(0)).get("parameters")).set(0,"0x1.8p0");CandidateAstR952 different=ast(numeric);ok(!base.semanticHashCP12.equals(different.semanticHashCP12));
  Map<String,Object> otherPosition=fixed();otherPosition.put("position",2);ok(!base.semanticHashCP12.equals(ast(otherPosition).semanticHashCP12));
  Map<String,Object> order=ModernModelContractTest.astRoot(ModelKind.SOFTMAX_LINEAR,CandidateAstR952.Family.LINEAR);CandidateAstR952 ordered=ast(order);
  order.put("featureOutputIds",Arrays.asList("n1","n0"));Map<String,Object> manifest=(Map<String,Object>)order.get("featureManifest");manifest.put("orderedFeatureIds",Arrays.asList("n1","n0"));manifest.put("missingPolicyPerFeature",Arrays.asList("MASK","REJECT"));ok(!ordered.semanticHashCP12.equals(ast(order).semanticHashCP12));
  Map<String,Object> binding=ModernModelContractTest.astRoot(ModelKind.SOFTMAX_LINEAR,CandidateAstR952.Family.LINEAR);((Map<String,Object>)binding.get("featureManifest")).put("bindingHashes",Collections.singletonList(ModernModelContractTest.H));CandidateAstR952 bound=ast(binding);((Map<String,Object>)binding.get("featureManifest")).put("bindingHashes",Collections.singletonList(V51ChecksumEngine.sha256("other binding")));ok(!bound.semanticHashCP12.equals(ast(binding).semanticHashCP12));
  Map<String,Object> matrix=ModernModelContractTest.astRoot(ModelKind.SOFTMAX_LINEAR,CandidateAstR952.Family.LINEAR);Map<String,Object> node=(Map<String,Object>)((List<?>)matrix.get("nodes")).get(0);node.put("operator","ATOM");node.put("atomId","ATOM2-"+ModernModelContractTest.H.substring(0,24));node.put("parameters",Collections.emptyList());manifest=(Map<String,Object>)matrix.get("featureManifest");manifest.put("atomSpecHashes",Collections.singletonList(ModernModelContractTest.H));manifest.put("requiredHistory",20);
  manifest.put("matrixSpecRefs",Collections.singletonList(ModernModelContractTest.map("type","FrequencyMatrix","partitionRule","CP12/20/POSITION/probabilities~0/n0","manifest",MatrixBindingsCP12.specManifest(MatrixProjectionCP12.Type.FrequencyMatrix,20,"POSITION","probabilities~0"))));CandidateAstR952 matrixA=ast(matrix);
  manifest.put("matrixSpecRefs",Collections.singletonList(ModernModelContractTest.map("type","FrequencyMatrix","partitionRule","CP12/20/POSITION/probabilities~1/n0","manifest",MatrixBindingsCP12.specManifest(MatrixProjectionCP12.Type.FrequencyMatrix,20,"POSITION","probabilities~1"))));ok(!matrixA.semanticHashCP12.equals(ast(matrix).semanticHashCP12));
  CandidateAstR952 old1=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20),old2=old1.withLookback(40);ok(old1.semanticHashCP12.isEmpty());ok(old2.semanticHashCP12.isEmpty());
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);File root=new File(args[2]);root.mkdirs();List<Models.Draw> history=new ArrayList<>();String issue="202609230001";
  for(int i=0;i<441;i++){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=1000L+60000L*i;for(int j=0;j<10;j++)d.numbers[j]=(i+j)%10+1;history.add(d);issue=TargetIssueResolver.next(issue).targetIssue;}
  String registration=issue,source=history.get(440).issue,evidence=V51ChecksumEngine.sha256("semantic fixture evidence");DevelopmentInputCP9.Header header=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(ModernModelContractTest.map("revision","SCDEV1","game","PK10","build","R980","sourceManifestHash",evidence,"scheduleRevision","TARGET_ISSUE_RESOLVER_R980","bindings",Collections.emptyList())));
  DevelopmentInputCP9.Receipt identity=DevelopmentInputCP9.write(new OutputStream(){public void write(int b){}public void write(byte[] b,int off,int n){}},header,history);MemoryBudgetCP4 budget=new MemoryBudgetCP4(4L<<20);
  bad(()->CanonicalSchemaCP12.installSemantic(db),"CP12_SCHEMA_OWNER_TRANSACTION");
  db.beginTransaction();try{DevelopmentProductionCP12.schema(db);RetentionWorkingStorageQuotaCP12.schema(db);RetentionArtifactIdentityCP12.install(db);CanonicalSchemaCP12.installSemantic(db);CanonicalSchemaCP12.installSemantic(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
  try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name='cp12_candidate_semantic_identity'",null)){ok(c.moveToFirst()&&c.getString(0).contains("WHERE semantic_hash<>''"));}
  db.execSQL("INSERT INTO draws(issue,dirty) VALUES(?,0)",new Object[]{source});long generation=DevelopmentProductionCP12.historyGeneration(db);
  try(TrainingCasCP12.LockedFile master=TrainingCasCP12.publish(root,TrainingCasCP12.INPUT_SUFFIX,identity.inputHash,identity.byteCount,out->DevelopmentInputCP9.write(out,header,history).inputHash);InputStream stream=master.open();DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(stream,master.bytes,master.hash,header,budget)){
   action(db,"base",source,evidence);action(db,"taxonomy",source,evidence);action(db,"exact",source,evidence);action(db,"numeric",source,evidence);
   try(DevelopmentProductionCP12.PreparedProtocol p=protocol(base,"first","base",registration,root,master.hash,header,input,generation)){
    bad(()->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,expected("first","base",evidence)),"REGISTER_TX_REQUIRED");
    String rolled=commit(db,p,expected("first","base",evidence),false);ok(!rolled.startsWith("NO_OP:"));ok(count(db,"r952_candidate")==0);ok(count(db,"research_training_protocol_v2")==0);ok(count(db,ResearchTaskQueueR967.TABLE)==0);ok(state(db,"base").equals("PROPOSED"));
    String firstTask=commit(db,p,expected("first","base",evidence),true);ok(firstTask.equals(rolled));ok(count(db,"r952_candidate")==1);ok(count(db,"research_training_protocol_v2")==1);ok(count(db,ResearchTaskQueueR967.TABLE)==1);ok(state(db,"base").equals("WAIT_FUTURE"));
    db.beginTransaction();try{bad(()->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,expected("first","base",evidence)),"REGISTER_ACTION_CAS");}finally{db.endTransaction();}
   }
   try(DevelopmentProductionCP12.PreparedProtocol p=protocol(same,"taxonomyChild","taxonomy",registration,root,master.hash,header,input,generation)){
    ok(commit(db,p,expected("taxonomyChild","taxonomy",evidence),false).equals("NO_OP:first"));ok(state(db,"taxonomy").equals("PROPOSED"));ok(count(db,"r952_candidate")==1);
    ok(commit(db,p,expected("taxonomyChild","taxonomy",evidence),true).equals("NO_OP:first"));ok(state(db,"taxonomy").equals("NO_OP"));ok(error(db,"taxonomy").equals("EXISTING_SEMANTIC_PROGRAM_NO_NEW_GENERATION"));ok(count(db,"r952_candidate")==1);ok(count(db,"research_training_protocol_v2")==1);ok(count(db,ResearchTaskQueueR967.TABLE)==1);ok(count(db,"r974_model_lineage")==0);
   }
   try(DevelopmentProductionCP12.PreparedProtocol p=protocol(base,"exactChild","exact",registration,root,master.hash,header,input,generation)){
    ok(commit(db,p,expected("exactChild","exact",evidence),true).equals("NO_OP:first"));ok(error(db,"exact").equals("EXISTING_STRUCTURE_NO_NEW_GENERATION"));ok(count(db,"r952_candidate")==1);ok(count(db,"research_training_protocol_v2")==1);ok(count(db,ResearchTaskQueueR967.TABLE)==1);
   }
   db.beginTransaction();try{bad(()->db.execSQL("UPDATE r952_candidate SET semantic_hash=? WHERE candidate_id='first'",new Object[]{base.semanticHashCP12}),"SEMANTIC_GENOME_IMMUTABLE");bad(()->db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at,semantic_hash) VALUES('index-bypass','intent',?,?,'','NURSERY',0,?,0,1,?)",new Object[]{same.canonical,same.structuralHash,registration,same.semanticHashCP12}),"UNIQUE constraint failed");}finally{db.endTransaction();}ok(count(db,"r952_candidate")==1);
   try(DevelopmentProductionCP12.PreparedProtocol p=protocol(different,"numericChild","numeric",registration,root,master.hash,header,input,generation)){
    db.beginTransaction();try{db.execSQL("UPDATE draws SET open_time=9 WHERE issue=?",new Object[]{source});bad(()->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,expected("numericChild","numeric",evidence)),"TRAIN_HISTORY_CHANGED");}finally{db.endTransaction();}ok(count(db,"r952_candidate")==1);
    db.beginTransaction();try{bad(()->db.execSQL("UPDATE research_action_v2 SET evidence_hash=? WHERE action_id='numeric'",new Object[]{V51ChecksumEngine.sha256("stale action")}),"CP5_RESEARCH_ACTION_IDENTITY_IMMUTABLE");db.execSQL("UPDATE research_action_v2 SET state='NO_OP' WHERE action_id='numeric'");bad(()->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,expected("numericChild","numeric",evidence)),"REGISTER_ACTION_CAS");}finally{db.endTransaction();}ok(count(db,"r952_candidate")==1);
    String task=commit(db,p,expected("numericChild","numeric",evidence),true);ok(!task.startsWith("NO_OP:"));ok(count(db,"r952_candidate")==2);ok(count(db,"research_training_protocol_v2")==2);ok(count(db,ResearchTaskQueueR967.TABLE)==2);
   }
   try(Cursor c=db.rawQuery("SELECT future_n,may_create_real,semantic_hash,state FROM r952_candidate ORDER BY candidate_id",null)){int rows=0;while(c.moveToNext()){rows++;ok(c.getInt(0)==0&&c.getInt(1)==0&&c.getString(2).matches("[0-9a-f]{64}")&&c.getString(3).equals("NURSERY"));}ok(rows==2);}
   db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES('legacy1','intent',?,?,'','NURSERY',0,?,0,1)",new Object[]{old1.canonical,old1.structuralHash,registration});db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES('legacy2','intent',?,?,'','NURSERY',0,?,0,1)",new Object[]{old2.canonical,old2.structuralHash,registration});try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM r952_candidate WHERE semantic_hash=''",null)){c.moveToFirst();ok(c.getLong(0)==2);}
   try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){ok(c.moveToFirst()&&c.getString(0).equals("ok"));}try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
  }finally{db.shutdown();}ok(budget.used()==0);
  Files.writeString(Path.of(args[3]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual root CandidateAst/SemanticGenome/CanonicalSchema/Director registration method with real SCDEV and SQLite: family-only same semantic NO_OP without extra child/protocol/task/lineage; numeric/order/binding/Matrix changes differ; structural gate first; partial unique and immutable SQL; rollback/replay/action/history CAS; new childFuture0/REAL0. Legacy AST1/2 blank semantic compatible. No device execution claim.\"}\n");
 }
}
