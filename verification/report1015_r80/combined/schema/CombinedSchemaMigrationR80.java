package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
/** Exercises actual combined coordinator with real SQLite through the host API adapter. */
public final class CombinedSchemaMigrationR80 {
 static int checks;
 static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why+" / "+ResearchSchemaCoordinatorR926.summary());}
 static String rows(SQLiteDatabase db,String sql){StringBuilder b=new StringBuilder();try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext()){for(int i=0;i<c.getColumnCount();i++)b.append(c.getString(i)).append('|');b.append('\n');}}return b.toString();}
 static String journals(SQLiteDatabase db){return rows(db,"SELECT * FROM v57_schema_migration_journal_r926 WHERE step_id NOT IN ('R79_MATRIX_READY_ORDER_V1','R79_LEGACY_ORIGINAL_PROOF_V1','R80_BOUND_ATOM_ORIGINAL_INPUT_V1','R80_MULTI_HORIZON_PROBE_V1') ORDER BY domain,revision,step_id");}
 static String domain(SQLiteDatabase db){return rows(db,"SELECT * FROM v57_schema_domain_r926 ORDER BY domain");}
 static int ready(SQLiteDatabase db){return (ResearchMigrationRegistryR952.readyOrderR79(db)?1:0)+(LegacySettlementProofR79.ready(db)?1:0)+(BoundAtomLifecycleR80.readyR80(db)?1:0)+(MultiHorizonProbeR80.readyR80(db)?1:0);}
 static DatabaseCoordinator owner(SQLiteDatabase db)throws Exception{return HostProductR974.store(db).coordinatorR36();}
 static void reset(SQLiteDatabase db){
  db.execSQL("PRAGMA foreign_keys=OFF");
  java.util.ArrayList<String> fences=new java.util.ArrayList<>();try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='trigger' AND (name LIKE 'r80_asset_audit_fence_%' OR name LIKE 'r80_probe_%')",null)){while(c.moveToNext())fences.add(c.getString(0));}for(String f:fences)db.execSQL("DROP TRIGGER "+f);
  db.execSQL("DROP INDEX IF EXISTS idx_r80_asset_audit_order");
  db.execSQL("DROP INDEX IF EXISTS idx_r79_legacy_classification_candidate");
  String[] tables={"r80_probe_diagnostic_ref","research_legacy_asset_head_snapshot_r80","research_legacy_asset_head_audit_r80","research_legacy_asset_head_baseline_r80","research_legacy_settlement_classification_r79","research_bound_input_r80","research_bound_source_r80","research_bound_parent_r80","research_bound_cohort_r80","r80_probe_input","r80_probe_forecast","r80_probe_label","r80_probe_opportunity"};
  db.execSQL("DROP VIEW IF EXISTS r80_probe_pending");
  for(String t:tables)db.execSQL("DROP TABLE IF EXISTS "+t);
  for(String t:new String[]{"r80_atom_binding_original","r80_atom_binding_immutable","r80_atom_binding_no_replace"})db.execSQL("DROP TRIGGER IF EXISTS "+t);
  db.execSQL("DROP INDEX IF EXISTS idx_r79_matrix_ready_partition_order");
  db.execSQL("DELETE FROM v57_schema_migration_journal_r926 WHERE step_id IN ('R79_MATRIX_READY_ORDER_V1','R79_LEGACY_ORIGINAL_PROOF_V1','R80_BOUND_ATOM_ORIGINAL_INPUT_V1','R80_MULTI_HORIZON_PROBE_V1')");
  SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.researchContract());
 }
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
  String mode=args[2];DatabaseCoordinator o=owner(db);
  if("prepare".equals(mode)){
   reset(db);ok(ready(db)==0,"baseline absent additives");String j=journals(db),d=domain(db);
   RealtimePressureGateR811.beginAnalysis();try{ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"pressure defers");ok(domain(db).equals(d),"pressure preserves exact READY stamp");ok(journals(db).equals(j),"pressure preserves journals");}finally{RealtimePressureGateR811.endAnalysis();}
   db.beginTransaction();try{ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"first helper only");ok(ready(db)==1,"one additive helper installed");ok(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()),"intermediate baseline READY valid");}finally{db.endTransaction();}
   ok(ready(db)==0,"outer rollback removes helper");ok(domain(db).equals(d),"rollback exact domain");ok(journals(db).equals(j),"rollback exact historical journal");
  }else if("restart".equals(mode)){
   ok(ready(db)==0,"restart observes rolled back baseline");String j=journals(db);int version=db.getVersion();
   for(int n=1;n<=4;n++){boolean r=ResearchSchemaCoordinatorR926.ensureOneStep(o);ok(r==(n==4),"READY only after fourth helper");ok(ready(db)==n,"one helper per writer callback");ok(journals(db).equals(j),"old journals never replay");ok(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()),"atomic fingerprint per slice");}
   ok(db.getVersion()==version,"same user version");String d=domain(db);ok(ResearchSchemaCoordinatorR926.ensureOneStep(o),"repeat ready");ok(d.equals(domain(db)),"repeat no restamp");
  }else if("fault".equals(mode)){
   reset(db);ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"matrix helper first");String j=journals(db),d=domain(db);
   db.execSQL("CREATE INDEX idx_r79_legacy_classification_candidate ON "+DbHelper.TABLE_APP_STATE+"(state_key)");
   ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"collision rejected");ok(ResearchSchemaCoordinatorR926.summary().startsWith("FAILED|"),"explicit failure");ok(domain(db).equals(d),"failure preserves exact READY baseline");ok(journals(db).equals(j),"failure preserves old journals");ok(rows(db,"SELECT name FROM sqlite_master WHERE name='research_legacy_settlement_classification_r79'").isEmpty(),"failure rolls back entire helper");
   db.execSQL("DROP INDEX idx_r79_legacy_classification_candidate");ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"retry resumes legacy only");ok(ready(db)==2,"legacy retry installed");ok(journals(db).equals(j),"retry never replays baseline");
   ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"bound next");ok(ResearchSchemaCoordinatorR926.ensureOneStep(o),"probe final");
  }else if("trigger-dml".equals(mode)){
   reset(db);for(int n=0;n<4;n++)ResearchSchemaCoordinatorR926.ensureOneStep(o);ok(ready(db)==4,"all helpers ready before DML");
   java.util.ArrayList<String> targets=new java.util.ArrayList<>();try(Cursor c=db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE type='trigger' AND name LIKE 'r80_asset_audit_fence_%' ORDER BY tbl_name",null)){while(c.moveToNext())targets.add(c.getString(0));}
   ok(targets.size()>=8,"all authority/base targets exercised");
   for(String table:targets){StringBuilder updates=new StringBuilder();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext()){if(updates.length()>0)updates.append(',');updates.append('"').append(c.getString(1)).append("\"=\"").append(c.getString(1)).append('"');}}
    db.beginTransaction();try{
     // SQLite compiles every trigger body even when SELECT/WHERE produces no rows.
     // This executes all three statements without weakening preexisting business guards.
     db.execSQL("INSERT INTO "+table+" SELECT * FROM "+table+" WHERE 0");ok(true,"INSERT trigger bodies compile: "+table);
     db.execSQL("UPDATE "+table+" SET "+updates+" WHERE 0");ok(true,"UPDATE trigger bodies compile: "+table);
     db.execSQL("DELETE FROM "+table+" WHERE 0");ok(true,"DELETE trigger bodies compile: "+table);
    }finally{db.endTransaction();}
   }
  }else if("fingerprints".equals(mode)){
   reset(db);for(int n=0;n<4;n++)ResearchSchemaCoordinatorR926.ensureOneStep(o);ok(ready(db)==4,"all helpers ready before tamper");
   String[][] guards={{"r79_legacy_classification_no_update","research_legacy_settlement_classification_r79"},{"r80_bound_source_identity","research_bound_source_r80"},{"r80_probe_input_no_update","r80_probe_input"}};
   for(String[] guard:guards){String d=domain(db),j=journals(db);db.beginTransaction();try{
    db.execSQL("DROP TRIGGER "+guard[0]);db.execSQL("CREATE TRIGGER "+guard[0]+" BEFORE UPDATE ON "+guard[1]+" BEGIN SELECT 1; END");
    ok(ready(db)==3,"exact same-name malformed guard rejected");ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"owner cannot bless weakened guard");
   }finally{db.endTransaction();}
   ok(ready(db)==4,"tamper rollback restores exact fingerprints");ok(domain(db).equals(d),"tamper rollback domain exact");ok(journals(db).equals(j),"tamper rollback baseline journals exact");ok(ResearchSchemaCoordinatorR926.ensureOneStep(o),"ready after tamper rollback");}
  }else if("pressure-expiry".equals(mode)){
   reset(db);String j=journals(db);java.lang.reflect.Field f=ResearchSchemaCoordinatorR926.class.getDeclaredField("firstRequestedAt");f.setAccessible(true);f.setLong(null,System.currentTimeMillis()-16000);
   RealtimePressureGateR811.beginAnalysis();try{ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"expired pressure permits one helper");ok(ready(db)==1,"pressure expiry one slice only");ok(journals(db).equals(j),"pressure expiry no baseline replay");ok(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()),"pressure expiry atomic baseline");}finally{RealtimePressureGateR811.endAnalysis();}
  }else if("nested-fault".equals(mode)){
   reset(db);ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"matrix first");String d=domain(db),j=journals(db);
   db.execSQL("CREATE INDEX idx_r79_legacy_classification_candidate ON "+DbHelper.TABLE_APP_STATE+"(state_key)");
   db.beginTransaction();try{ok(!ResearchSchemaCoordinatorR926.ensureOneStep(o),"nested fault rejected");db.setTransactionSuccessful();}finally{db.endTransaction();}
   ok(domain(db).equals(d),"even enclosing commit cannot preserve partial failed migration");ok(journals(db).equals(j),"nested fault preserves journals");
   ok(rows(db,"SELECT name FROM sqlite_master WHERE name='research_legacy_settlement_classification_r79'").isEmpty(),"nested failure rollback-only");
  }else if("prior-shipped".equals(mode)||"prior-populated".equals(mode)){
   if("prior-populated".equals(mode))db.execSQL("WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<6000) INSERT INTO research_new_asset_freeze_v41(asset_id,target_issue,asset_version_hash,base_issue,history_hash,surface_hash,history_rows,position,n1,n2,n3,action_ordinal,score,confidence,risk,stability,context_blob,context_digest,record_hash,frozen_ms) SELECT 'synthetic-upgrade',printf('%012d',x),'v','base','h','s',1,1,1,2,3,0,0.5,0.5,0.5,0.5,'{}','c','synthetic-upgrade-'||x,1 FROM n");
   String freezeBefore=rows(db,"SELECT * FROM research_new_asset_freeze_v41 ORDER BY asset_id,target_issue");
   ok(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()),"untouched shipped baseline READY");String j=journals(db);int version=db.getVersion(),calls=0;
   while(!ResearchSchemaCoordinatorR926.ensureOneStep(o)){ok(++calls<=4,"bounded additive call count");ok(journals(db).equals(j),"shipped migration journals unchanged between slices");}
   ok(ready(db)==4,"shipped upgrade all exact helper fingerprints");ok(journals(db).equals(j),"shipped historical journals unchanged");ok(db.getVersion()==version,"shipped user version unchanged");ok(freezeBefore.equals(rows(db,"SELECT * FROM research_new_asset_freeze_v41 ORDER BY asset_id,target_issue")),"populated history unchanged by upgrade");
  }else throw new IllegalArgumentException(mode);
  System.out.println("COMBINED_SCHEMA_PASS mode="+mode+" checks="+checks);
 }}
}
