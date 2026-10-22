package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
public final class SchemaMigrationHarnessR968 {
 static int n;static void check(boolean v,String s){if(!v)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 static long count(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}}
 public static void main(String[] args)throws Exception{SQLiteDatabase d=new SQLiteDatabase(args[0]);try{
  DbStore store=new DbStore(d);DatabaseCoordinator owner=store.coordinatorR36();
  d.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0,open_time INTEGER,fetch_time INTEGER)");
  DbHelper.ensureR35Schema(d);DbHelper.ensureR36Schema(d);RuntimeEventLedgerR926.ensureSchema(d);DbHelper.ensureR906CriticalSchema(d);V51Repository.ensure(d);RealtimePressureGateR811.resetForTests();
  check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"FRESH_ALL_PRODUCT_RESEARCH_MIGRATIONS_READY:"+ResearchSchemaCoordinatorR926.summary());
  check(count(d,"SELECT COUNT(*) FROM pragma_table_info('"+FixedResearchWindowR932.WINDOW+"') WHERE name='formal_plan_revision'")==1,"ORIGINAL_WINDOW_HAS_STABLE_PLAN_REVISION");
  String[] required={ResearchMultiplicityR932.RESEARCH,ResearchMultiplicityR932.IDENTITY,ResearchTaskQueueR967.TABLE,ResearchTaskQueueR967.PIN,ResearchTaskQueueR967.LANE,ResearchGatePlanR967.PLAN,ResearchGatePlanR967.POLICY,ResearchGatePlanR967.REQUIRED};
  for(String name:required)check(count(d,"SELECT COUNT(*) FROM sqlite_master WHERE name='"+name+"'")==1,"REQUIRED_SCHEMA_OBJECT:"+name);
  d.beginTransaction();try{
   ResearchMultiplicityR932.registerCandidate(d,"preserve","FAMILY",V51ChecksumEngine.sha256("immutable"),"202609210001");
   ResearchMultiplicityR932.registerLook(d,"preserve","W1",1,"202609210002","202609210121");
   ResearchTaskQueueR967.enqueue(d,"UNIT","preserve","202609210001","SEQ",V51ChecksumEngine.sha256("input"),"receipt");d.setTransactionSuccessful();
  }finally{d.endTransaction();}
  // Simulate the observed predecessor's revision-8 READY stamp without the new v9 immutable-plan object.
  d.execSQL("DROP VIEW "+ResearchMultiplicityR932.IDENTITY);d.execSQL("DROP TABLE "+ResearchGatePlanR967.PLAN);
  d.execSQL("UPDATE "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" SET revision=8,state='READY' WHERE domain='RESEARCH'");
  d.execSQL("DELETE FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain='RESEARCH' AND revision=9");
  check(!SchemaDomainCoordinatorR926.matches(d,SchemaDomainCoordinatorR926.researchContract()),"OLD_READY_CANNOT_HIDE_MISSING_R967_OBJECTS");
  check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"REVISION_8_TO_9_REPAIR_READY:"+ResearchSchemaCoordinatorR926.summary());
  check(count(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.RESEARCH)==1&&count(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==1&&count(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"UPGRADE_PRESERVES_RESEARCH_AND_SPENT_ALPHA_HISTORY");
  check(count(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE status='PENDING'")==1,"UPGRADE_PRESERVES_PENDING_TASKS");
  d.execSQL("DROP VIEW "+ResearchMultiplicityR932.IDENTITY);check(!SchemaDomainCoordinatorR926.matches(d,SchemaDomainCoordinatorR926.researchContract()),"SAME_VERSION_VIEW_DRIFT_REJECTED");
  check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"SAME_VERSION_VIEW_REPAIRED");
  ResearchFunnelR967.Snapshot snap=ResearchFunnelR967.capture(d);check(snap.error.isEmpty(),"FULL_SCHEMA_FUNNEL_QUERY_VALID:"+snap.error);
  check(snap.values.get("research_registered")==1L&&snap.values.get("paid_hypotheses")==1L,"FULL_SCHEMA_FUNNEL_RECONCILES_REGISTRY");
  long attempts=count(d,"SELECT SUM(attempt) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL);check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8)&&attempts==count(d,"SELECT SUM(attempt) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL),"READY_MIGRATION_IDEMPOTENT");
  check(count(d,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"MIGRATION_DOES_NOT_FABRICATE_CERTIFICATES");
  System.out.println("SCHEMA_MIGRATION_PASS checks="+n+"; production_DDL_SQL=YES; DBStore_writer=HOST_ADAPTER; physical_upgrade=NOT_RUN");
 }finally{d.shutdown();}}
}
