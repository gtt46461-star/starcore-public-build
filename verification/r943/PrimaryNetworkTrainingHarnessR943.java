package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
public final class PrimaryNetworkTrainingHarnessR943 {
 static void check(boolean x,String message){if(!x)throw new AssertionError(message);System.out.println(message+"=PASS");}
 static void label(SQLiteDatabase db,String issue){String text="0.31,0.45,0.03,0.9,0.8,0.6,0.8",hash=V51ChecksumEngine.sha256(text);
  db.execSQL("INSERT INTO "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+"(asset_id,target_issue,selected_ordinal,feature_schema_hash,feature_blob,feature_hash,predicted_probability,actual_number,hit_label,predraw_hash,draw_hash,outcome_hash,frozen_at,settled_at) VALUES('PRODUCTION_SELECTED',?,0,?,?,?,0.31,1,1,?,?,?,1,2)",new Object[]{issue,AppConfig.R906_COMPACT_FEATURE_SCHEMA,text.getBytes(StandardCharsets.UTF_8),hash,hash,hash,V51ChecksumEngine.sha256(issue)});
 }
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=new DbStore(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);RealtimePressureGateR811.resetForTests();
   check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"PRIMARY_LEARNER_FRESH_SCHEMA");
   store.setState("r924_tiny_deep_invalid_cursor","999999999999");label(db,"202609120010");
   ResearchTinyDeepLearnerR909.Result first=ResearchTinyDeepLearnerR909.updateOne(store);
   check(first.success&&first.updates==1,"SWALLOWED_YIELD_CURSOR_RECOVERED_AND_REAL_NETWORK_TRAINED:"+first.status);
   check(!ResearchTinyDeepLearnerR909.updateOne(store).success,"DUPLICATE_PRIMARY_LABEL_NOT_RETRAINED");
   label(db,"202609120011");db.execSQL("CREATE TRIGGER injected_nn_failure BEFORE INSERT ON v57_research_artifact_r811 BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
   check(!ResearchTinyDeepLearnerR909.updateOne(store).success,"INJECTED_WEIGHT_WRITE_FAILURE_REPORTED");
   check(store.getState("r909_tiny_deep_trained_until","").equals("202609120010"),"FAILED_UPDATE_DOES_NOT_ADVANCE_TRAINING_CURSOR");
   db.execSQL("DROP TRIGGER injected_nn_failure");ResearchTinyDeepLearnerR909.Result second=ResearchTinyDeepLearnerR909.updateOne(store);
   check(second.success&&second.updates==2,"FAILED_PRIMARY_LABEL_RESUMES_WITH_REAL_WEIGHTS");
   System.out.println("R943_PRIMARY_NEURAL_TRAINING_PASS|scope=PRODUCTION_UPDATE_ONE_REAL_SQLITE_SYNTHETIC_LABELS");
  }finally{db.shutdown();}
 }
}
