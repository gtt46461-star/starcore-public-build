package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;
/** Expiry keeps terminal/archival rules while preserving the real pre-expiry diagnosis. */
public final class MatrixExpiryR79 {
 static int checks;static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static String h(String s){return MatrixProjectionCP12.hash(s);}
 static void tx(SQLiteDatabase db,Runnable r,boolean commit){db.beginTransaction();try{r.run();if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){ok(c.moveToFirst(),"count");return c.getLong(0);}}
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
  ok(count(db,"draws")==5000,"explicit retained-tail fixture");
  MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FrequencyMatrix,"position:1",20,h("expiry"));
  String[] states={"READY","QUARANTINED","REPLAY_REQUIRED"},errors={"","CP12_MATRIX_INPUT_CORRUPT","CP12_MATRIX_PIT_REPLAY_REQUIRED"};Map<String,String> hashes=new HashMap<>();
  tx(db,()->{for(int i=0;i<3;i++){String receipt=h("expire:"+i);MatrixSnapshotBusR952.enqueueCP12(db,spec,"20260921000"+(i+1),i+1,receipt,h("proof:"+i),new MatrixProjectionCP12.Sample(i+1,0,0,0,0,0,0,0,0,0),null,1);db.execSQL("UPDATE research_matrix_input_cp12 SET state=?,last_error=? WHERE matrix_id=? AND source_receipt_hash=?",new Object[]{states[i],errors[i],spec.matrixId,receipt});}},true);
  try(Cursor c=db.rawQuery("SELECT source_receipt_hash,input_hash FROM research_matrix_input_cp12",null)){while(c.moveToNext())hashes.put(c.getString(0),c.getString(1));}
  tx(db,()->ok(MatrixSnapshotBusR952.expireInputsCP12(db,2,32)==3,"rollback expiry selected3"),false);ok(count(db,"research_matrix_terminal_cp12")==0,"rollback leaves no terminal");
  tx(db,()->ok(MatrixSnapshotBusR952.expireInputsCP12(db,3,32)==3,"commit expiry selected3"),true);
  for(int i=0;i<3;i++)try(Cursor c=db.rawQuery("SELECT i.state,i.last_error,i.input_hash,t.reason,t.input_hash FROM research_matrix_input_cp12 i JOIN research_matrix_terminal_cp12 t ON t.matrix_id=i.matrix_id AND t.source_receipt_hash=i.source_receipt_hash WHERE i.source_receipt_hash=?",new String[]{h("expire:"+i)})){
   ok(c.moveToFirst(),"terminal exists");ok(c.getString(0).equals("EXPIRED_ARCHIVE_REQUIRED"),"raw retained for verified archive");ok(c.getString(1).equals("MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION|prior_state="+states[i]+"|prior_error="+errors[i]),"diagnostic cause retained");ok(c.getString(2).equals(hashes.get(h("expire:"+i)))&&c.getString(2).equals(c.getString(4)),"immutable input hash retained");ok(c.getString(3).equals("MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION"),"terminal contract unchanged");
  }
  tx(db,()->ok(MatrixSnapshotBusR952.expireInputsCP12(db,4,32)==0,"repeat no op"),true);ok(count(db,"research_projection_ack_v2")==0,"expiry never creates numerical acknowledgement");ok(count(db,"research_matrix_checkpoint_v2")==0,"expiry never creates numerical checkpoint");ok(count(db,"research_matrix_input_cp12")==3,"original input not deleted");
  System.out.println("R79_MATRIX_EXPIRY_PASS assertions="+checks+" previousStateAndCause=RETAINED expiryBoundary=UNCHANGED archiveRequired=PASS numericAck=NONE rollback=PASS");
 }}
}
