package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
public final class FunnelReadOnlyHarnessR967 {
 static int n;static void check(boolean v,String s){if(!v)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 static long count(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}}
 public static void main(String[] args)throws Exception{SQLiteDatabase d=new SQLiteDatabase(args[0]);try{
  ResearchFunnelR967.Snapshot e=ResearchFunnelR967.capture(d);
  check(e.error.isEmpty()&&e.values.get("research_registered")==null,"ABSENT_TABLES_ARE_UNKNOWN_NOT_ZERO");
  check(count(d,"SELECT COUNT(*) FROM sqlite_master")==0,"CAPTURE_ON_EMPTY_DB_PERFORMS_NO_DDL_OR_WRITES");
  d.beginTransaction();try{ResearchMultiplicityR932.schema(d);d.setTransactionSuccessful();}finally{d.endTransaction();}
  ResearchFunnelR967.Snapshot zero=ResearchFunnelR967.capture(d);check(zero.values.get("research_registered")==0L,"REAL_EMPTY_TABLE_IS_ZERO");
  d.beginTransaction();try{ResearchMultiplicityR932.registerCandidate(d,"F1","FAMILY",V51ChecksumEngine.sha256("S"),"202609210001");d.setTransactionSuccessful();}finally{d.endTransaction();}
  d.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER)");d.execSQL("INSERT INTO draws VALUES('202609210001',0),('202609219999',1)");
  ResearchFunnelR967.Snapshot one=ResearchFunnelR967.capture(d);check(one.error.isEmpty()&&one.values.get("research_registered")==1L&&one.values.get("paid_hypotheses")==0L,"REGISTERED_AND_PAID_ARE_SEPARATE_DENOMINATORS");
  check("202609210001".equals(one.drawWatermark),"SINGLE_STATEMENT_WATERMARK_EXCLUDES_DIRTY_DRAWS");
  check(one.sql.startsWith("SELECT ")&&!one.sql.contains(";")&&one.id.matches("[a-f0-9]{64}"),"ONE_METRIC_STATEMENT_AND_SNAPSHOT_ID_EXPOSED");
  check(one.render().contains("UNKNOWN_OR_NOT_READY")&&one.render().contains("NOT_PROVEN"),"UNAVAILABLE_AND_PROOF_STATUS_VISIBLE");
  d.execSQL("CREATE TABLE r952_artifact(wrong_column INTEGER)");
  ResearchFunnelR967.Snapshot bad=ResearchFunnelR967.capture(d);check(!bad.error.isEmpty()&&bad.values.get("research_registered")==null,"SCHEMA_MISMATCH_INVALIDATES_SNAPSHOT_NOT_FAKE_ZERO");
  check(count(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.RESEARCH)==1,"DIAGNOSTIC_FAILURE_DOES_NOT_MUTATE_DATA");
  System.out.println("FUNNEL_READ_ONLY_PASS checks="+n+"; scope=PRODUCT_SQL_HOST_NOT_DEVICE");
 }finally{d.shutdown();}}
}
