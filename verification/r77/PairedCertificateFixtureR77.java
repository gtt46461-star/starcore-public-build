package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import android.database.sqlite.SQLiteDatabase;
/** Explicit synthetic shared-Future fixture. Uses actual formal-source and projection owners. */
public final class PairedCertificateFixtureR77 {
 static String h(String x){return V51ChecksumEngine.sha256("EXPLICIT_TEST_ONLY:"+x);}
 static String issue(int n){return String.format("20260923%04d",n);}
 static long drawAt(SQLiteDatabase db,int n){try(android.database.Cursor c=db.rawQuery("SELECT open_time FROM draws WHERE issue=?",new String[]{issue(n)})){if(!c.moveToFirst())throw new AssertionError("SYNTHETIC_DRAW_REQUIRED");return c.getLong(0);}}
 static void tx(SQLiteDatabase db,Runnable f){db.beginTransaction();try{f.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public static void add(SQLiteDatabase db,String candidateFreeze)throws Exception {
  String peer=h("peer"),life=h("peer-life"),key="algorithm:TEST_ONLY_PEER",window="zz_TEST_ONLY_PEER_WINDOW";
  tx(db,()->{
   db.execSQL("INSERT INTO research_cp4_frozen_lifetime VALUES(?,?,?,?,?,?,?,?,?,?,?,1)",new Object[]{life,key,"g1",h("identity"),"{}",h("policy"),"{}",h("hypothesis"),"{}",issue(121),issue(360)});
   db.execSQL("INSERT INTO research_fixed_window_r932 VALUES(?,?,?,?,?,?,?,240,2,1,'PASS','',?,'R967_FORMAL_PLAN_V1')",new Object[]{window,key,peer,h("bundle"),h("selector"),issue(121),issue(360),h("result")});
   db.execSQL("INSERT INTO research_cp4_window_binding VALUES(?,?,2,?,?,240,?)",new Object[]{window,life,issue(121),issue(360),h("plan")});
   for(int n=121;n<=360;n++){
    boolean candidateHit=(n-1)%5<2;long drawAt=drawAt(db,n);
    db.execSQL("INSERT INTO v57_settlement_task_r36(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',?,?,?,?,?)",new Object[]{issue(n),issue(n),drawAt+1,drawAt+1,drawAt+1,h("draw"+n),h("settlement"+n)});
    db.execSQL("INSERT INTO research_fixed_pair_r932 VALUES(?,?,?,?,1,4,5,6,.6,?,?,?,0,?)",new Object[]{window,issue(n),h("prediction"+n),h("champion"+n),h("calibration"),drawAt-1,candidateHit?0:1,drawAt+1});
   }
   ProspectiveDiversityCP12.registerFormalPeersCP12(db,candidateFreeze,issue(121));
  });
  for(MatrixProjectionCP12.Type type:new MatrixProjectionCP12.Type[]{MatrixProjectionCP12.Type.CandidateSimilarityMatrix,MatrixProjectionCP12.Type.CandidateDiversityMatrix}){
   MatrixProjectionCP12.Spec spec=ProspectiveDiversityCP12.specCP12(type,candidateFreeze,peer);
   for(int n=121;n<=360;n++){
    final long sourceAt=drawAt(db,n)+2,projectionAt=sourceAt+1;
    MatrixSourceRouterCP12.PreparedInput input=MatrixSourceRouterCP12.preparePairedFormalCP12(db,spec);
    if(input==null||!input.issue.equals(issue(n)))throw new AssertionError("ACTUAL_FORMAL_SOURCE_MISSING:"+n);
    tx(db,()->MatrixSourceRouterCP12.commitPrepared(db,input,sourceAt));
    MatrixSnapshotBusR952.PreparedProjectionCP12 projection=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+60_000_000_000L);
    if(projection==null)throw new AssertionError("ACTUAL_PROJECTION_MISSING");
    tx(db,()->{if(!MatrixSnapshotBusR952.commitProjectionCP12(db,projection,projectionAt))throw new AssertionError("ACTUAL_PROJECTION_FENCE_FAILED");});
   }
  }
 }
}
