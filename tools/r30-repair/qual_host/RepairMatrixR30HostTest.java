package com.ethk3.novaonepro.strategyv2;
import java.sql.*;import java.util.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.ResearchEvolutionStoreR930;
public final class RepairMatrixR30HostTest {
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);}static long count(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 public static void main(String[]args)throws Exception{Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase db=new SQLiteDatabase(c);
  db.execSQL("CREATE TABLE old_model(identity TEXT,payload BLOB)");db.execSQL("INSERT INTO old_model VALUES('protected',X'00112233')");
  c.setAutoCommit(false);ResearchEvolutionStoreR930.createSchema(db);c.rollback();c.setAutoCommit(true);check(count(db,"SELECT count(*) FROM sqlite_master WHERE name='research_matrix_snapshot_r930'")==0,"PARTIAL_SCHEMA_COMMITTED");
  c.setAutoCommit(false);ResearchEvolutionStoreR930.createSchema(db);ResearchEvolutionStoreR930.createSchema(db);c.commit();c.setAutoCommit(true);check(count(db,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'research_%_r930'")==13,"LEGACY_RESTORE_NOT_13");
  double[][]v={{.4,.6,.45}};long[][]n={{200,200,200}};MatrixTensorR30R1.Tensor t=new MatrixTensorR30R1.Tensor("STUDENT_ERROR","SELECT",StatsR27.sha256("dataset"),StatsR27.sha256("parent"),"202610300100","202610300101",1,Collections.singletonList("original-parent"),Arrays.asList("hit","miss","brier"),v,n,Collections.singletonMap("scope","HOST_SYNTHETIC"));
  ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();MatrixTensorR30R1.append(b,t,1);
  c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,b,"HOST_MATRIX");c.rollback();c.setAutoCommit(true);check(count(db,"SELECT count(*) FROM research_matrix_snapshot_r930")==0,"FAILED_MATRIX_PUBLISHED");
  for(int i=0;i<2;i++){c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,b,"HOST_MATRIX");c.commit();c.setAutoCommit(true);}check(count(db,"SELECT count(*) FROM research_matrix_snapshot_r930")==1,"RETRY_INFLATED_MATRIX");
  try(Cursor r=db.rawQuery("SELECT payload_json,matrix_hash,role,producer_version FROM research_matrix_snapshot_r930",null)){check(r.moveToFirst()&&r.getString(0).equals(t.payload)&&r.getString(1).equals(StatsR27.sha256(r.getString(0)))&&r.getString(2).equals("SELECT")&&r.getString(3).equals(MatrixTensorR30R1.VERSION),"PAYLOAD_OR_IDENTITY_MISSING");}
  check(count(db,"SELECT count(*) FROM old_model WHERE hex(payload)='00112233'")==1,"ORIGINAL_MODEL_CHANGED");
  System.out.println("REPAIR_MATRIX_NATIVE|RESTORE_13_BASELINE_DECLARED_TABLES|MIGRATION_ROLLBACK_AND_RETRY|ACTUAL_PAYLOAD_HASH_AND_ROLE|FAILED_WRITE_NO_PUBLICATION|IDEMPOTENT|ORIGINAL_MODEL_BYTES_KEPT|PASS|HOST_SYNTHETIC_ONLY");
 }}
}
