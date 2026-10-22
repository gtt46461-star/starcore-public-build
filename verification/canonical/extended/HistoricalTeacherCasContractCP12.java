package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;import java.util.*;import java.io.*;import java.nio.file.*;
public final class HistoricalTeacherCasContractCP12 {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("teacher/cas "+checks);}interface Call{void run()throws Exception;}static void bad(Call r)throws Exception{checks++;try{r.run();}catch(IllegalArgumentException|IllegalStateException|DevelopmentProductionCP12.Wait e){return;}throw new AssertionError("bad accepted");}
 public static void main(String[] args)throws Exception {
  List<Models.Draw> history=new ArrayList<>();String target="202609230030",wire="R958_PIT_V2|"+target;
  for(int n=10;n<30;n++){Models.Draw d=new Models.Draw();d.issue=String.format("20260923%04d",n);d.openTime=1000+n;for(int p=0;p<10;p++)d.numbers[p]=(n+p)%10+1;history.add(d);}
  for(int n=29;n>=10;n--)wire+='|'+history.get(n-10).issue+':'+history.get(n-10).numbersText();PITSnapshotContractR952 pit=PITSnapshotContractR952.fromFrozenHistory(target,history,V51ChecksumEngine.sha256(wire));
  android.database.sqlite.SQLiteDatabase db=new android.database.sqlite.SQLiteDatabase(args[2],args[3]);List<Object> teacherRefs=new ArrayList<>();List<double[]> expectedTeachers=new ArrayList<>();
  for(ModelKind kind:new ModelKind[]{ModelKind.SOFTMAX_LINEAR,ModelKind.HISTOGRAM_TREE,ModelKind.MLP_D32_16_10}){
   CandidateAstR952.Family family=kind==ModelKind.SOFTMAX_LINEAR?CandidateAstR952.Family.LINEAR:kind==ModelKind.HISTOGRAM_TREE?CandidateAstR952.Family.TREE:CandidateAstR952.Family.NEURAL;
   CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(ModernModelContractTest.astRoot(kind,family)));Program p=CandidateCompilerR952.compile(ast,FeatureManifestR952.forAst(ast),Collections.emptyList());Model model=Model.fromModern(ModernModelContractTest.bound(ModernModelContractTest.sample(kind,family),p.manifest));
   MemoryBudgetCP4 budget=new MemoryBudgetCP4(8L<<20);
   try(FrozenGraphContextCP4 ctx=FrozenGraphContextCP4.fromCaptured(p,model,pit,V51ChecksumEngine.sha256("teacher:"+kind),100,Collections.emptyMap(),Collections.emptyList(),Collections.emptyMap(),budget)){
    Map<String,Object> context=StrictCanonicalJson.readContextObject(ctx.canonicalJson());ExpertRef ref=new ExpertRef(ctx.freezeHash,model.hash(),p.manifest.hash,ast.position);
    FrozenGraphContextCP4.ExpertPrediction replay=HistoricalTrainingReceiptCP12.replayOwnTeacherCP12(ast,model,context,ctx.contextHash,ref,target,1000);
    db.execSQL("INSERT INTO r958_frozen_candidate(candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at) VALUES(?,?,?,?,?,?,?,?,?,1)",new Object[]{"teacher:"+kind,"learner:"+kind,ast.canonical,model.payload(),ctx.freezeHash,model.hash(),model.modern().trainedUntil,target,ModernModelContractTest.H});
    db.execSQL("INSERT INTO r958_frozen_predraw(target_issue,candidate_id,record_hash,input_hash,position,n1,n2,n3,score,captured_at,frozen_context,context_hash) VALUES(?,?,?,?,1,1,2,3,.3,120,?,?)",new Object[]{target,"teacher:"+kind,V51ChecksumEngine.sha256("predraw:"+kind),ModernModelContractTest.H,ctx.canonicalJson(),ctx.contextHash});
    teacherRefs.add(ModernModelContractTest.map("freezeHash",ref.freezeHash,"modelHash",ref.modelHash,"featureManifestHash",ref.featureManifestHash,"position",ref.position));expectedTeachers.add(replay.probabilities());
    WorldFeatureComputationR974.Result live=p.compute(pit,ctx);ok(Arrays.equals(replay.probabilities(),model.probabilities(live.values())));ok(replay.sourceHash.equals(live.outputHash));ok(replay.matchesFrozenRef(ref));ok(replay.availableAtMs==100);
    bad(()->HistoricalTrainingReceiptCP12.replayOwnTeacherCP12(ast,model,context,ctx.contextHash,ref,target,100));
    bad(()->HistoricalTrainingReceiptCP12.replayOwnTeacherCP12(ast,model,context,ctx.contextHash,new ExpertRef(ctx.freezeHash,V51ChecksumEngine.sha256("other"),p.manifest.hash,ast.position),target,1000));
    Map<String,Object> changed=new TreeMap<>(context);changed.put("normalizerHash",V51ChecksumEngine.sha256("changed"));bad(()->HistoricalTrainingReceiptCP12.replayOwnTeacherCP12(ast,model,changed,V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(changed)),ref,target,1000));
   }ok(budget.used()==0);
  }
  Map<String,Object> hedgeRoot=ModernModelContractTest.astRoot(ModelKind.EXPERT_HEDGE,CandidateAstR952.Family.BANDIT);hedgeRoot.put("experts",new ArrayList<>(teacherRefs.subList(0,2)));((Map<String,Object>)hedgeRoot.get("featureManifest")).put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(hedgeRoot));
  CandidateAstR952 hedgeAst=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(hedgeRoot));Program hedgeProgram=CandidateCompilerR952.compile(hedgeAst,FeatureManifestR952.forAst(hedgeAst),Collections.emptyList());
  java.lang.reflect.Method extract=HistoricalTrainingReceiptCP12.class.getDeclaredMethod("extract",android.database.sqlite.SQLiteDatabase.class,Program.class,String.class,long.class);extract.setAccessible(true);String historical=(String)extract.invoke(null,db,hedgeProgram,target,1000L);
  java.lang.reflect.Method decode=HistoricalTrainingReceiptCP12.class.getDeclaredMethod("decode",Program.class,Map.class,String.class,long.class);decode.setAccessible(true);HistoricalTrainingReceiptCP12.Row actual=(HistoricalTrainingReceiptCP12.Row)decode.invoke(null,hedgeProgram,StrictCanonicalJson.readContextObject(historical),historical,1000L);
  ok(actual.experts.size()==2);ok(Arrays.equals(actual.experts.get(0).probabilities(),expectedTeachers.get(0)));ok(Arrays.equals(actual.experts.get(1).probabilities(),expectedTeachers.get(1)));ok(actual.atoms.isEmpty());db.shutdown();
  File root=new File(args[0]);root.mkdirs();byte[] data="SCDEV_TEST_DISK_ADMISSION".getBytes(java.nio.charset.StandardCharsets.UTF_8);String hash=TrainingCasCP12.hex(TrainingCasCP12.sha().digest(data));
  try(TrainingCasCP12.LockedFile file=TrainingCasCP12.publish(root,TrainingCasCP12.INPUT_SUFFIX,hash,data.length,out->{out.write(data);return hash;})){ok(file.bytes==data.length);}
  long left=TrainingCasCP12.DISK_LIMIT_CP12-data.length;int n=0;while(left>0){long bytes=Math.min(DevelopmentInputCP9.MAX_BYTES,left);File file=new File(root,V51ChecksumEngine.sha256("quotaFixture"+(n++))+TrainingCasCP12.BOUND_SUFFIX);try(RandomAccessFile f=new RandomAccessFile(file,"rw")){f.setLength(bytes);}left-=bytes;}
  File stale=new File(root,"pending-00000000-0000-0000-0000-000000000001.partial");Files.write(stale.toPath(),new byte[]{5});ok(stale.setLastModified(0));
  File recent=new File(root,"pending-00000000-0000-0000-0000-000000000002.partial");Files.write(recent.toPath(),new byte[]{5});
  try(TrainingCasCP12.LockedFile file=TrainingCasCP12.publish(root,TrainingCasCP12.INPUT_SUFFIX,hash,data.length,out->{throw new AssertionError("idempotent CAS replay must not write");})){ok(file.bytes==data.length);}
  ok(!stale.exists());ok(recent.exists());java.lang.reflect.Method disk=TrainingCasCP12.class.getDeclaredMethod("diskBytesCP12",File.class);disk.setAccessible(true);ok((Long)disk.invoke(null,root)==TrainingCasCP12.DISK_LIMIT_CP12+1);recent.delete();
  bad(()->TrainingCasCP12.publish(root,TrainingCasCP12.INPUT_SUFFIX,TrainingCasCP12.hex(TrainingCasCP12.sha().digest(new byte[]{1,2,3})),3,out->{throw new AssertionError("hard capacity must reject before temp allocation/producer");}));
  try(java.nio.file.DirectoryStream<Path> files=Files.newDirectoryStream(root.toPath())){for(Path f:files)ok(!f.getFileName().toString().endsWith(".partial"));}
  Files.writeString(Path.of(args[1]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Real frozen ordinary Linear/Tree/MLP teacher replay equals live feature/model output; wrong ref/cutoff/normalizer rejected; CAS128MiB admission and idempotent reuse; sparse host disk quota fixture. No Android scheduler claim.\"}\n");
 }
}
