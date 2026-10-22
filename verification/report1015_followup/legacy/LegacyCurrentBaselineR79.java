package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** Current remote baseline only. Explicit synthetic fixtures, never acceptance/OOS. */
public final class LegacyCurrentBaselineR79 {
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}}
 static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 public static void main(String[]a)throws Exception {
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
   AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(79081,1,"R79_SYNTHETIC_LATEST_BASELINE");
   int[] atoms=new int[8];int ai=0;Set<Integer> groups=new HashSet<>();
   for(int i=0;i<GenesisSchemaRegistryR915.atomCount()&&ai<8;i++)if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()&&groups.add(i/20))atoms[ai++]=i;
   GenesisAssetGenomeR915 asset=new GenesisAssetGenomeR915("R79_LATEST_BASELINE",1,"","SYNTHETIC_BASELINE",SettlementProgressR72.issue(1),GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,g);
   for(int i=1;i<=200;i++)SettlementProgressR72.draw(db,i);
   db.beginTransaction();try{
    AlgorithmInventionLabR908.registerDirected(db,g,SettlementProgressR72.issue(1),SettlementProgressR72.issue(2),"R79_TEST","SYNTHETIC");
    AssetGenesisEngineR915.registerDirected(db,asset,SettlementProgressR72.issue(1),SettlementProgressR72.issue(2));
    SettlementProgressR72.algorithmFreeze(db,g,201);SettlementProgressR72.assetFreeze(db,asset,201,false);
    Thread.sleep(3L);
    Models.Draw draw=new Models.Draw();draw.issue=SettlementProgressR72.issue(201);draw.openTime=System.currentTimeMillis();draw.fetchTime=draw.openTime;
    draw.sourceLine="EXPLICIT_SYNTHETIC_R79_BASELINE";draw.rawJson="SYNTHETIC_NOT_PROVIDER_DATA";for(int i=0;i<10;i++)draw.numbers[i]=i+1;
    if(!DrawCanonicalizerR811.observeWithinWrite(db,draw).confirmed)throw new AssertionError("synthetic canonical setup");
    db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,source_line,raw_json,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)VALUES(?,?,?,?,?,?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{draw.issue,draw.openTime,draw.fetchTime,draw.fetchTime,draw.sourceLine,draw.rawJson});
    db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET status='RESET_CLEAN',epoch_start_issue=? WHERE genome_id=?",new Object[]{SettlementProgressR72.issue(202),g.genomeId});
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   Models.Draw draw=store.getDrawByIssue(SettlementProgressR72.issue(201));
   String proof=CandidateExecutionAuditR932.verifyAlgorithmSettlementR79(db,g.genomeId,draw);
   int assetRows=AssetGenesisEngineR915.settleDraw(store,draw),algorithmRows=AlgorithmInventionLabR908.settleDraw(store,draw);
   int mirror=ResearchShadowChallengerRuntimeR817.settle(store,draw);
   System.out.println("{\"kind\":\"LATEST_REMOTE_BASELINE_NOT_ACCEPTANCE\",\"scope\":\"ACTUAL_GRADLE_PRODUCT_SYNTHETIC_SQLITE_NO_NATURAL_FUTURE_OR_WAGERS\",\"asset_rows_labeled_without_proof\":"+assetRows+",\"asset_state_updates_without_proof\":"+n(db,"SELECT state_updates FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE asset_id='"+asset.assetId+"'")+",\"algorithm_original_proof\":\""+proof+"\",\"pre_epoch_algorithm_rows_labeled\":"+algorithmRows+",\"pre_epoch_mirror_rows_labeled\":"+mirror+",\"algorithm_future_n\":"+n(db,"SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+g.genomeId+"'")+",\"algorithm_state_updates\":"+n(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+g.genomeId+"'")+"}");
  }
 }
}
