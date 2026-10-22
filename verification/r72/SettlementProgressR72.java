package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;import java.util.*;import java.nio.file.*;
/** Isolated synthetic frozen records on actual product schema/SQLite/settlement owners.
 * This verifies recovery behavior, never model quality, prospective OOS or user bets. */
public final class SettlementProgressR72 {
 static int checks;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static long n(SQLiteDatabase db,String sql){return Long.parseLong(text(db,sql));}
 static String issue(int n){return String.format(java.util.Locale.US,"202610120%03d",n);}
 static void draw(SQLiteDatabase db,int n){
  if(n>200)try{Thread.sleep(3L);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IllegalStateException(interrupted);}
  Models.Draw d=new Models.Draw();d.issue=issue(n);d.openTime=n>200?System.currentTimeMillis():1700000000000L+60000L*n;d.fetchTime=d.openTime;
  d.sourceLine="EXPLICIT_SYNTHETIC_R72";d.rawJson="SYNTHETIC_NOT_PROVIDER_DATA";for(int i=0;i<10;i++)d.numbers[i]=i+1;
  if(!DrawCanonicalizerR811.observeWithinWrite(db,d).confirmed)throw new AssertionError("synthetic confirmed draw");
  db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,source_line,raw_json,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,draw_hash_version,draw_hash_v2)VALUES(?,?,?,?,?,?,0,1,2,3,4,5,6,7,8,9,10,?,?)",new Object[]{d.issue,d.openTime,d.fetchTime,d.fetchTime,d.sourceLine,d.rawJson,DrawHashR36.CURRENT_VERSION,DrawHashR36.sha256V2(d)});
 }
 static void mirror(SQLiteDatabase db,String id,int i,int position,String formula,String input,String record){
  db.execSQL("INSERT INTO "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+"(candidate_id,target_issue,phase,position,n1,n2,n3,selected_ordinal,score,probability,formula_hash,input_hash,frozen_at,record_hash)VALUES(?,?,'R908_INVENTED_ALGORITHM_FUTURE_OOS',?,1,2,3,0,.3,.3,?,?,1,?)",new Object[]{id,issue(i),position,formula,input,record});
 }
 static void algorithmFreeze(SQLiteDatabase db,AlgorithmGenomeR908 g,int i){algorithmFreeze(db,g,i,true);}
 static void algorithmFreeze(SQLiteDatabase db,AlgorithmGenomeR908 g,int i,boolean originalProof){
  StringBuilder feature=new StringBuilder("R908F1");for(int j=0;j<AlgorithmGenomeR908.INPUTS;j++)feature.append("|0");
  String input=h("R908_ALG_FEATURE|"+feature),record=h("R72_SYNTHETIC_PREDRAW|"+i);
  AlgorithmProofRecoveryR948.requireCleanForFreeze(db,g.genomeId);
  db.execSQL("INSERT INTO "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+"(genome_id,target_issue,position,n1,n2,n3,selected_ordinal,score,feature_blob,feature_hash,frozen_at,record_hash)VALUES(?,?,1,1,2,3,0,.3,?,?,1,?)",new Object[]{g.genomeId,issue(i),feature.toString(),input,record});
  mirror(db,g.genomeId,i,1,g.structuralHash,input,record);
  if(!originalProof)return;
  CandidateExecutionAuditR932.Builder b=new CandidateExecutionAuditR932.Builder();for(int j=0;j<1200;j++)b.add(j,.3,.3,j);
  String state=AlgorithmGenomeVmR908.initialState(g).encode();b.finish(0,g.canonical(),g.canonical(),state,state,state).freeze(db,"algorithm:"+g.genomeId,g.structuralHash,input,issue(i-1),issue(i),record);
  ResearchCaptureLedgerR932.record(db,issue(i),"algorithm:"+g.genomeId,"FROZEN","R72_SYNTHETIC_FIXTURE",record,1,true);
 }
 static void assetFreeze(SQLiteDatabase db,GenesisAssetGenomeR915 g,int i){assetFreeze(db,g,i,true);}
 static void assetFreeze(SQLiteDatabase db,GenesisAssetGenomeR915 g,int i,boolean originalProof){
  GenesisAssetVmR915.PredrawContext ctx=new GenesisAssetVmR915.PredrawContext(issue(i-1),issue(i),h("history"),h("surface"),300,1,1,2,3,0,new double[GenesisSchemaRegistryR915.atomCount()]);
  GenesisAssetVmR915.AssetValue out=new GenesisAssetVmR915.AssetValue(g.outputType,.3,.3,0,0,new double[0]);
  String record=h("R915_ASSET_FREEZE|"+g.assetId+'|'+issue(i)+'|'+g.structuralHash+'|'+ctx.digest()+'|'+out.valueHash);
  ContentValues v=new ContentValues();v.put("asset_id",g.assetId);v.put("target_issue",issue(i));v.put("asset_version_hash",g.structuralHash);v.put("base_issue",issue(i-1));v.put("history_hash",h("history"));v.put("surface_hash",h("surface"));v.put("history_rows",300);v.put("position",1);v.put("n1",1);v.put("n2",2);v.put("n3",3);v.put("action_ordinal",0);v.put("score",.3);v.put("confidence",.3);v.put("risk",0);v.put("stability",0);v.put("output_type",out.outputType);v.put("output_shape",out.outputShape);v.put("output_blob",out.encodeOutput());v.put("output_hash",out.valueHash);v.put("context_blob",ctx.encodeFrozen());v.put("context_digest",ctx.digest());v.put("record_hash",record);v.put("frozen_ms",System.currentTimeMillis());
  db.insertOrThrow(AssetGenesisEngineR915.TABLE_ASSET_FREEZE,null,v);
  if(originalProof){
   CandidateExecutionAuditR932.Builder b=new CandidateExecutionAuditR932.Builder();for(int j=0;j<1200;j++)b.add(j,.3,.3,j);
   String state=new GenesisAssetVmR915.State().encode();b.finish(0,g.canonical(),g.canonical(),state,state,state).freeze(db,"genesis:"+g.assetId,g.structuralHash,ctx.surfaceHash,ctx.baseIssue,ctx.targetIssue,record);
   ResearchCaptureLedgerR932.record(db,issue(i),"genesis:"+g.assetId,"FROZEN","EXPLICIT_SYNTHETIC_R72_ORIGINAL_PROOF",record,1,true);
  }
 }
 public static void main(String[]a)throws Exception {
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
   AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(72001,1,"R72_SYNTHETIC_LABEL_RECOVERY");
   int[] atoms=new int[8];int ai=0;Set<Integer> groups=new HashSet<>();
   for(int i=0;i<GenesisSchemaRegistryR915.atomCount()&&ai<8;i++)if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()&&groups.add(i/20))atoms[ai++]=i;
   ok(ai==8,"synthetic asset uses eight distinct production atom groups");
   GenesisAssetGenomeR915 asset=new GenesisAssetGenomeR915("R72_FIXTURE",1,"","SYNTHETIC_RECOVERY",issue(1),GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,g);
   for(int i=1;i<=200;i++)draw(db,i);
   db.beginTransaction();try{
    AlgorithmInventionLabR908.registerDirected(db,g,issue(1),issue(2),"R72_TEST","SYNTHETIC");
    AssetGenesisEngineR915.registerDirected(db,asset,issue(1),issue(2));
    for(int i=201;i<=203;i++){algorithmFreeze(db,g,i);assetFreeze(db,asset,i);draw(db,i);}
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   for(int i=201;i<=203;i++)ok("VALID".equals(CandidateExecutionAuditR932.verifyAlgorithm(db,g.genomeId,issue(i))),"synthetic proof valid before label "+i);
   Models.Draw newer=store.getDrawByIssue(issue(202));
   ok(AlgorithmInventionLabR908.settleDraw(store,newer)==1,"newer algorithm outcome committed");
   ok(AssetGenesisEngineR915.settleDraw(store,newer)==1,"newer asset outcome committed");
   String ga=" FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+g.genomeId+"'",aa=" FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE asset_id='"+asset.assetId+"'";
   String state=text(db,"SELECT state_blob"+ga),assetState=text(db,"SELECT state_blob"+aa);
   ok(AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(issue(201)))==1,"older algorithm exact outcome recovered");
   ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(201)))==1,"older asset exact outcome recovered");
   ok(state.equals(text(db,"SELECT state_blob"+ga))&&assetState.equals(text(db,"SELECT state_blob"+aa)),"late labels preserve both newer model states");
   ok(text(db,"SELECT trained_until_issue"+ga).equals(issue(202))&&text(db,"SELECT future_last_issue"+ga).equals(issue(202)),"late label never rewinds watermarks");
   ok(n(db,"SELECT future_n"+ga)==2&&n(db,"SELECT state_updates"+ga)==1,"outcomes counted separately from actual updates");
   ok(AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(issue(201)))==0&&AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(201)))==0,"duplicate label changes nothing");
   ok(AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(issue(203)))==1&&AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(203)))==1,"next chronological issue still trains both models");
   ok(n(db,"SELECT state_updates"+ga)==2&&n(db,"SELECT state_updates"+aa)==2,"exactly two chronological updates");
   ResearchShadowChallengerRuntimeR817.settle(store,store.getDrawByIssue(issue(201)));ResearchShadowChallengerRuntimeR817.settle(store,newer);ResearchShadowChallengerRuntimeR817.settle(store,store.getDrawByIssue(issue(203)));
   db.beginTransaction();try{for(int i=1;i<=13;i++)mirror(db,"R72-SYNTHETIC-"+i,i,i<=12?0:1,h("formula"),h("input"),h("fair"+i));db.setTransactionSuccessful();}finally{db.endTransaction();}
   ResearchPredrawSettlementPumpR908.Result first=ResearchPredrawSettlementPumpR908.drainBatch(store,12,1600);
   ok(first.rows==0,"twelve malformed historical rows reproduce zero progress");
   ResearchPredrawSettlementPumpR908.Result second=ResearchPredrawSettlementPumpR908.drainBatch(HostProductR974.store(db),12,1600);
   ok(second.rows==1,"recreated owner reaches thirteenth valid issue");
   ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id LIKE 'R72-SYNTHETIC-%' AND hit_label=-1")==12,"invalid rows preserved without fabricated outcomes");
   ok(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"recovery mints no certificates or bets");
   Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_PRODUCT_SYNTHETIC_SQLITE_NOT_DEVICE_OR_OOS\"}\n");
  }
 }
}
