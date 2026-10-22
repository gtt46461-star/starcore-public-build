package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.util.*;
public final class MatrixPersistenceCP12Test {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("matrix SQL "+checks);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static void enqueue(SQLiteDatabase db,MatrixProjectionCP12.Spec spec,long n,boolean commit){db.beginTransaction();try{MatrixSnapshotBusR952.enqueueCP12(db,spec,String.format("20260923%04d",n),n,MatrixProjectionCP12.hash("r"+spec.matrixId+n),MatrixProjectionCP12.hash("proof"+n),MatrixProjectionCP12Test.sample(spec.type,n),spec.type==MatrixProjectionCP12.Type.FreezeMatrix?Collections.singletonMap("feature",spec.manifest):null,123);if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static boolean commit(SQLiteDatabase db,MatrixSnapshotBusR952.PreparedProjectionCP12 p,boolean commit){db.beginTransaction();try{boolean out=MatrixSnapshotBusR952.commitProjectionCP12(db,p,124);if(commit)db.setTransactionSuccessful();return out;}finally{db.endTransaction();}}
 public static void main(String[] args)throws Exception{SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);try{
  String manifest=MatrixProjectionCP12.hash("spec");List<MatrixProjectionCP12.Spec> specs=new ArrayList<>();for(MatrixProjectionCP12.Type t:MatrixProjectionCP12.Type.values()){MatrixProjectionCP12.Spec s=new MatrixProjectionCP12.Spec(t,t==MatrixProjectionCP12.Type.ConditionalTransitionMatrix?"partition|regimePolicy:"+MatrixProjectionCP12.hash("CP12_ATOM_REGIME_ARGMAX5_V1"):"partition",t==MatrixProjectionCP12.Type.FreezeMatrix?1:20,manifest);specs.add(s);enqueue(db,s,1,true);}
  ok(count(db,"research_matrix_input_cp12")==20);
  MatrixSnapshotBusR952.PreparedProjectionCP12 initial=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+20_000_000_000L);ok(initial!=null);ok(commit(db,initial,false));ok(count(db,"research_matrix_checkpoint_v2")==0);ok(count(db,"research_projection_ack_v2")==0);
  for(int n=0;n<20;n++){MatrixSnapshotBusR952.PreparedProjectionCP12 p=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+20_000_000_000L);ok(p!=null);ok(commit(db,p,true));ok(commit(db,p,true));}
  ok(MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+20_000_000_000L)==null);ok(count(db,"research_matrix_checkpoint_v2")==20);ok(count(db,"research_projection_ack_v2")==20);
  for(MatrixProjectionCP12.Spec s:specs){enqueue(db,s,1,true);ok(MatrixSnapshotBusR952.readAtCP4(db,s.matrixId,s.manifest,"202609230001")==null);MatrixSnapshotBusR952.CheckpointCP4 cp=MatrixSnapshotBusR952.readAtCP4(db,s.matrixId,s.manifest,"202609230002");ok(cp!=null&&cp.version==0&&cp.sampleCount==1);ok(MatrixProjectionCP12.decode(s,cp.payload()).samples()==1);}
  ok(count(db,"research_matrix_input_cp12")==20);
  MatrixProjectionCP12.Spec frequency=specs.get(MatrixProjectionCP12.Type.FrequencyMatrix.ordinal());enqueue(db,frequency,2,true);MatrixSnapshotBusR952.PreparedProjectionCP12 next=MatrixSnapshotBusR952.prepareProjectionCP11(db,System.nanoTime()+20_000_000_000L);ok(next.expected.version==0&&next.next.version==1);ok(commit(db,next,false));ok(MatrixSnapshotBusR952.readAtCP4(db,frequency.matrixId,frequency.manifest,"202609230003").version==0);ok(commit(db,next,true));ok(MatrixSnapshotBusR952.readAtCP4(db,frequency.matrixId,frequency.manifest,"202609230003").version==1);
  db.beginTransaction();try{try{MatrixSnapshotBusR952.enqueueCP12(db,frequency,"202609230002",2,MatrixProjectionCP12.hash("r"+frequency.matrixId+2),MatrixProjectionCP12.hash("different-proof"),MatrixProjectionCP12Test.sample(frequency.type,2),null,1);throw new AssertionError("drift accepted");}catch(IllegalStateException e){ok(e.getMessage().equals("CP12_MATRIX_INPUT_DRIFT"));}}finally{db.endTransaction();}
  try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
  System.out.println("CP12_MATRIX_PERSISTENCE_PASS checks="+checks+" types=20 rollback=PASS duplicate=PASS watermark=PASS");
 }finally{db.shutdown();}}
}
