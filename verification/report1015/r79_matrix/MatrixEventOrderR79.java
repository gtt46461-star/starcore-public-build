package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;
/** Actual SQLite queue/checkpoint owner, with an independently inspected legacy ordering witness. */
public final class MatrixEventOrderR79 {
 static int checks;static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static String h(String s){return MatrixProjectionCP12.hash(s);}
 static void tx(SQLiteDatabase db,Runnable work,boolean commit){db.beginTransaction();try{work.run();if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static void enqueue(SQLiteDatabase db,MatrixProjectionCP12.Spec spec,String issue,long ordinal,MatrixProjectionCP12.Sample sample){MatrixSnapshotBusR952.enqueueCP12(db,spec,issue,ordinal,h(spec.matrixId+":"+ordinal),h("proof:"+ordinal),sample,null,1);}
 static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){ok(c.moveToFirst(),"count row");return c.getLong(0);}}
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
  // Upgrade migration is idempotent and the ready-only order index is present in real schema.
  ResearchMigrationRegistryR952.ensure(db);ResearchMigrationRegistryR952.ensure(db);
  ok(count(db,"SELECT COUNT(*) FROM sqlite_master WHERE name='idx_r79_matrix_ready_partition_order' AND sql LIKE '%WHERE state=%READY%'")==1,"ready partial index installed");
  MatrixProjectionCP12.Spec latency=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.LatencyMatrix,"stage:HISTORY|unit:WALL_MICROS|device:ANDROID|release:979",20,h("event order"));
  tx(db,()->{enqueue(db,latency,"202609230020",100,new MatrixProjectionCP12.Sample(100,0,0,0,0,0,0,1000,0,0));enqueue(db,latency,"202609230010",200,new MatrixProjectionCP12.Sample(200,0,0,0,0,0,0,2000,0,0));},true);
  try(Cursor c=db.rawQuery("SELECT source_ordinal FROM research_matrix_input_cp12 WHERE state='READY' ORDER BY source_issue,source_ordinal,matrix_id,source_receipt_hash LIMIT 1",null)){ok(c.moveToFirst()&&c.getLong(0)==200,"legacy issue ordering chooses newer event first");}
  MatrixSnapshotBusR952.PreparedProjectionCP12 first=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+30000000000L);ok(first.sourceReceiptHash.equals(h(latency.matrixId+":100")),"earlier event chosen first");
  tx(db,()->ok(MatrixSnapshotBusR952.commitProjectionCP12(db,first,2),"rollback commit"),false);ok(count(db,"SELECT COUNT(*) FROM research_matrix_checkpoint_v2")==0,"rollback checkpoint unchanged");
  tx(db,()->ok(MatrixSnapshotBusR952.commitProjectionCP12(db,first,3),"first durable commit"),true);
  MatrixSnapshotBusR952.PreparedProjectionCP12 second=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+30000000000L);ok(second.sourceReceiptHash.equals(h(latency.matrixId+":200")),"second event chosen");ok(second.next.throughIssue.equals("202609230020"),"issue watermark never regresses");
  tx(db,()->ok(MatrixSnapshotBusR952.commitProjectionCP12(db,second,4),"second durable commit"),true);tx(db,()->ok(MatrixSnapshotBusR952.commitProjectionCP12(db,second,5),"repeat idempotent"),true);
  MatrixProjectionCP12.State state=MatrixProjectionCP12.decode(latency,second.next.payload());ok(state.throughOrdinal()==200&&state.samples()==2,"both real event samples retained");ok(MatrixProjectionCP12.metrics(state).get("count")[0]==2,"numerical count");
  MatrixProjectionCP12.Spec failure=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FailureMatrix,"EVENT_TEST|domain:SYSTEM",20,h("failure order"));
  tx(db,()->{enqueue(db,failure,"202609230030",300,new MatrixProjectionCP12.Sample(300,0,2,0,0,0,0,0,0,0));enqueue(db,failure,"202609230009",400,new MatrixProjectionCP12.Sample(300,MatrixProjectionCP12.RECOVERED,2,0,0,0,0,0,0,500));},true);
  for(int i=0;i<2;i++){MatrixSnapshotBusR952.PreparedProjectionCP12 p=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+30000000000L);ok(p!=null,"failure event present");tx(db,()->ok(MatrixSnapshotBusR952.commitProjectionCP12(db,p,6),"failure event commit"),true);if(i==1){MatrixProjectionCP12.State f=MatrixProjectionCP12.decode(failure,p.next.payload());ok(f.samples()==1&&f.samplesCopy().get(0).has(MatrixProjectionCP12.RECOVERED),"recovery follows original despite older issue");}}
  ok(count(db,"SELECT COUNT(*) FROM research_matrix_input_cp12 WHERE state='DONE'")==4,"all four inputs numerically projected");ok(count(db,"SELECT COUNT(*) FROM research_matrix_input_cp12 WHERE state IN('REPLAY_REQUIRED','QUARANTINED')")==0,"no false replay isolation");ok(count(db,"SELECT COUNT(*) FROM research_projection_ack_v2")==4,"one ack per input");
  try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN SELECT 1 FROM research_matrix_input_cp12 earlier WHERE earlier.matrix_id=? AND earlier.state='READY' AND earlier.source_ordinal<?",new String[]{latency.matrixId,"200"})){StringBuilder plan=new StringBuilder();while(c.moveToNext())plan.append(c.getString(3));ok(plan.toString().contains("idx_r79_matrix_ready_partition_order"),"ready lookup bounded by partial index");}
  System.out.println("R79_MATRIX_EVENT_ORDER_PASS assertions="+checks+" issueRegression=PASS failureRecovery=PASS rollback=PASS idempotence=PASS noFabricatedAck=PASS");
 }}
}
