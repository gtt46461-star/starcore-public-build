package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** Baseline defect reproduction on actual product classes. Synthetic records only.
 * This intentionally records the old unsafe behavior; it is not an acceptance test. */
public final class LegacyProofBaselineR79 {
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())return -1;return c.getLong(0);}}
 static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 public static void main(String[]a)throws Exception {
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
   AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(79081,1,"R79_SYNTHETIC_LEGACY_PROOF_REPRO");
   int[] atoms=new int[8];int ai=0;Set<Integer> groups=new HashSet<>();
   for(int i=0;i<GenesisSchemaRegistryR915.atomCount()&&ai<8;i++)if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()&&groups.add(i/20))atoms[ai++]=i;
   if(ai!=8)throw new AssertionError("fixture atom coverage");
   GenesisAssetGenomeR915 asset=new GenesisAssetGenomeR915("R79_PROOF_FIXTURE",1,"","SYNTHETIC_BASELINE",SettlementProgressR72.issue(1),GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,g);
   for(int i=1;i<=200;i++)SettlementProgressR72.draw(db,i);
   db.beginTransaction();try{
    AlgorithmInventionLabR908.registerDirected(db,g,SettlementProgressR72.issue(1),SettlementProgressR72.issue(2),"R79_TEST","SYNTHETIC");
    AssetGenesisEngineR915.registerDirected(db,asset,SettlementProgressR72.issue(1),SettlementProgressR72.issue(2));
    SettlementProgressR72.algorithmFreeze(db,g,201);
    SettlementProgressR72.assetFreeze(db,asset,201);
    SettlementProgressR72.draw(db,201);
    db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET status='RESET_CLEAN',epoch_start_issue=? WHERE genome_id=?",new Object[]{SettlementProgressR72.issue(202),g.genomeId});
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   String before=CandidateExecutionAuditR932.verifyAlgorithm(db,g.genomeId,SettlementProgressR72.issue(201));
   long assetProof=n(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key='genesis:"+asset.assetId+"'");
   int assetRows=AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(SettlementProgressR72.issue(201)));
   int algorithmRows=AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(SettlementProgressR72.issue(201)));
   System.out.println("{\"kind\":\"BASELINE_DEFECT_REPRO_NOT_ACCEPTANCE\",\"scope\":\"ACTUAL_PRODUCT_SYNTHETIC_SQLITE_NO_NATURAL_FUTURE_OR_WAGERS\",\"asset_execution_proof_rows\":"+assetProof+",\"asset_rows_labeled_without_proof\":"+assetRows+",\"asset_state_updates_without_proof\":"+n(db,"SELECT state_updates FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE asset_id='"+asset.assetId+"'")+",\"algorithm_original_proof\":\""+before+"\",\"pre_epoch_algorithm_rows_labeled\":"+algorithmRows+",\"pre_epoch_algorithm_quarantine_reason\":\""+text(db,"SELECT reason FROM "+AlgorithmProofRecoveryR948.QUARANTINE+" WHERE genome_id='"+g.genomeId+"'")+"\"}");
  }
 }
}
