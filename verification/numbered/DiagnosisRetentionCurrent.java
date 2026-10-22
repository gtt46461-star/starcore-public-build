package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Production schema/retention methods on real SQLite; synthetic receipts are explicitly scoped. */
public final class DiagnosisRetentionCurrent {
    private static final String[] TABLES={"adaptive_judge_log","diagnosis_case_log",
        "diagnosis_fix_plan","dynamic_freeze_matrix","module_effect_cache","shadow_promotion_cache"};
    private static final String[] KEYS={"judge_key","case_key","plan_key","freeze_key","cache_key","promotion_key"};
    private static final String H="a".repeat(64);
    private static final JSONArray assertions=new JSONArray();
    private static void check(boolean value,String assertion){
        if(!value)throw new AssertionError(assertion);
        assertions.put(assertion);
    }
    private static long scalar(SQLiteDatabase db,String sql){
        try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}
    }
    private static void refused(Runnable body,String code){
        try{body.run();}catch(IllegalArgumentException|IllegalStateException expected){
            check(expected.getMessage().contains(code),"typed refusal "+code);return;
        }
        throw new AssertionError("accepted forbidden operation "+code);
    }
    private static String schema(SQLiteDatabase db){
        StringBuilder result=new StringBuilder();
        try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name",null)){
            while(c.moveToNext())for(int i=0;i<4;i++)result.append(c.getString(i)).append('\n');
        }
        return result.toString();
    }
    private static void integrity(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){check(c.moveToFirst()&&"ok".equals(c.getString(0)),"integrity ok");}
        try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){check(!c.moveToFirst(),"foreign keys clean");}
    }
    private static RetentionArchiveProtocolCP12.Receipt receipt(SQLiteDatabase db,RetentionArchiveProtocolCP12.Page page,String hash){
        List<RetentionArchiveProtocolCP12.Member> members=new ArrayList<>();
        for(RetentionArchiveProtocolCP12.RowVersion row:page.rows){
            members.add(new RetentionArchiveProtocolCP12.Member(row.rowId,row.generation,
                DataRetentionGovernorR900.canonicalLiveKeyCP12(db,page.table,row.rowId,H),H));
        }
        return new RetentionArchiveProtocolCP12.Receipt(page,hash,100,H,H,1,members);
    }
    private static int commit(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt receipt,boolean commit){
        db.beginTransaction();
        try{int n=DataRetentionGovernorR900.publishAndPruneCP12(db,receipt,"numbered-diagnosis",Long.MAX_VALUE);
            if(commit)db.setTransactionSuccessful();return n;
        }finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try{
            db.execSQL("PRAGMA foreign_keys=ON");
            DbStore store=HostProductR974.store(db);DatabaseCoordinator owner=store.coordinatorR36();
            String before=schema(db);
            refused(()->DbHelper.installDiagnosisLoopSchemaCP12(db),"DIAGNOSIS_SCHEMA_OWNER");
            refused(()->RetentionRevisionSchemaCP12.diagnosisSchemaCP12(db),"RETENTION_SCHEMA_OWNER");
            if(scalar(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='diagnosis_case_log'")==0){
                refused(store::ensureDiagnosisLoopTablesNow,"DIAGNOSIS_SCHEMA_NOT_READY");
                check(before.equals(schema(db)),"runtime schema check performs no DDL");
            }
            check(!owner.runAtomicWrite("NUMBERED_DIAGNOSIS_ROLLBACK",x->{
                DbHelper.installDiagnosisLoopSchemaCP12(x);
                RetentionRevisionSchemaCP12.diagnosisSchemaCP12(x);
                x.execSQL("INSERT INTO app_state VALUES('numbered_diag_rollback','provisional',1)");
                throw new IllegalStateException("EXPLICIT_DIAGNOSIS_MIGRATION_ROLLBACK");
            }),"injected failure reports rollback");
            check(before.equals(schema(db)),"DDL failure rolls back full sqlite_master");
            check(scalar(db,"SELECT COUNT(*) FROM app_state WHERE state_key='numbered_diag_rollback'")==0,"no provisional state committed");
            check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"registered research Owner progresses to ready");
            SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.researchContract());
            check(scalar(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name<>'android_metadata'")==411,"all 411 business tables installed");
            check(RetentionTableRegistryCP12.ALL.size()==405,"original canonical 405 rules preserved");
            check(RetentionTableRegistryCP12.ALL_INSTALLED.size()==411,"current installed 411 rules consumed");
            check(RetentionTableRegistryCP12.hashForTable("draws").equals(RetentionTableRegistryCP12.HASH),"old draw page identity retained");
            check(!RetentionTableRegistryCP12.hashForTable("diagnosis_case_log").equals(RetentionTableRegistryCP12.HASH),"new rule page identity independent");
            refused(()->RetentionTableRegistryCP12.require("unregistered_test_table"),"RETENTION_UNREGISTERED_TABLE");
            String installed=schema(db);
            for(int repeat=0;repeat<2;repeat++){
                check(owner.runAtomicWrite("NUMBERED_DIAGNOSIS_REENTRY",x->{
                    com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.ensure(x);
                    DbHelper.installDiagnosisLoopSchemaCP12(x);
                    RetentionRevisionSchemaCP12.diagnosisSchemaCP12(x);
                }),"migration reentry "+repeat);
                check(installed.equals(schema(db)),"schema identical after reentry "+repeat);
            }
            if("synthetic".equals(args[3])){
                for(int i=0;i<TABLES.length;i++){
                    String table=TABLES[i];
                    db.execSQL("INSERT INTO "+table+"("+KEYS[i]+",target_issue,created_at,updated_at) VALUES('numbered-row','202601010001',1,1)");
                    long row=scalar(db,"SELECT rowid FROM "+table+" WHERE "+KEYS[i]+"='numbered-row'");
                    long generation=scalar(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='"+table+"' AND source_rowid="+row);
                    check(generation>0,"insert revision "+table);
                    db.execSQL("UPDATE "+table+" SET detail_text='changed' WHERE rowid="+row);
                    check(scalar(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='"+table+"' AND source_rowid="+row)>generation,"update advances generation "+table);
                    refused(()->db.execSQL("DELETE FROM "+table+" WHERE rowid="+row),"CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE");
                    check(scalar(db,"SELECT COUNT(*) FROM "+table+" WHERE rowid="+row)==1,"unarchived row preserved "+table);
                }
                // Populate only a synthetic draw fixture for production cutoff selection.
                db.beginTransaction();
                try{
                    String issue="202609230001";StringBuilder values=new StringBuilder();
                    for(int i=0;i<5000;i++){
                        if(values.length()>0)values.append(',');
                        values.append("('").append(issue).append("',").append(10000+i).append(",0,1,2,3,4,5,6,7,8,9,10)");
                        issue=TargetIssueResolver.next(issue).targetIssue;
                        if(i%100==99){db.execSQL("INSERT INTO draws(issue,open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES "+values);values.setLength(0);}
                    }
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                for(String table:new String[]{"adaptive_judge_log","diagnosis_case_log"}){
                    RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,table);
                    check(page!=null&&page.rows.size()==1,"Governor consumes current raw rule "+table);
                    String task;
                    db.beginTransaction();
                    try{task=ResearchTaskQueueR967.enqueue(db,"POST_TRAIN","numbered-pin-"+table,"202601010001",H,H,H);
                        ResearchTaskQueueR967.pin(db,task,table,"202601010001");db.setTransactionSuccessful();
                    }finally{db.endTransaction();}
                    check(DataRetentionGovernorR900.prepareArchivePageCP12(db,table)==null,"live research pin blocks selection "+table);
                    db.execSQL("UPDATE v57_research_task_r967 SET status='EXPIRED' WHERE task_id=?",new Object[]{task});
                    page=DataRetentionGovernorR900.prepareArchivePageCP12(db,table);
                    check(page!=null,"expired pin allows retry "+table);
                    RetentionArchiveProtocolCP12.Receipt proof=receipt(db,page,table.startsWith("adaptive")?H:"b".repeat(64));
                    db.execSQL("UPDATE "+table+" SET detail_text='late mutation'");
                    check(commit(db,proof,true)==0,"stale generation prevents deletion "+table);
                    check(scalar(db,"SELECT COUNT(*) FROM "+table)==1,"stale row retained "+table);
                    RetentionArchiveProtocolCP12.Receipt fresh=receipt(db,DataRetentionGovernorR900.prepareArchivePageCP12(db,table),table.startsWith("adaptive")?"c".repeat(64):"d".repeat(64));
                    check(commit(db,fresh,false)==1,"publication negative fixture selects one row "+table);
                    check(scalar(db,"SELECT COUNT(*) FROM "+table)==1,"publication rollback keeps row "+table);
                    check(commit(db,fresh,true)==1,"matching proof publishes and prunes "+table);
                    check(commit(db,fresh,true)==0,"duplicate publication is idempotent "+table);
                }
                java.lang.reflect.Method save=DbStore.class.getDeclaredMethod("saveDiagnosisRecord",String.class,String.class,android.content.ContentValues.class);
                save.setAccessible(true);
                for(int i=2;i<TABLES.length;i++){
                    String table=TABLES[i],key=KEYS[i];StringBuilder values=new StringBuilder();
                    db.beginTransaction();
                    try{
                        for(int n=1;n<4096;n++){
                            if(values.length()>0)values.append(',');
                            values.append("('numbered-domain-").append(n).append("',1,1)");
                            if(n%100==0||n==4095){db.execSQL("INSERT INTO "+table+"("+key+",created_at,updated_at) VALUES "+values);values.setLength(0);}
                        }
                        db.setTransactionSuccessful();
                    }finally{db.endTransaction();}
                    check(scalar(db,"SELECT COUNT(*) FROM "+table)==4096,"cache cap fixture "+table);
                    android.content.ContentValues row=new android.content.ContentValues();row.put(key,"numbered-overflow");
                    refused(()->RetentionAggregateCapacityCP12.checkAdmission(db,table,row),"RETENTION_AGGREGATE_CAPACITY");
                    android.content.ContentValues invalid=new android.content.ContentValues();invalid.putNull(key);
                    refused(()->RetentionAggregateCapacityCP12.checkAdmission(db,table,invalid),"RETENTION_AGGREGATE_INVALID_KEY");
                    check((Long)save.invoke(store,table,key,row)==-1L,"actual diagnostic DAO refuses overflow "+table);
                    check(scalar(db,"SELECT COUNT(*) FROM "+table)==4096,"overflow preserves all cache rows "+table);
                    try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='cp12_aggregate_blocker'",null)){
                        check(c.moveToFirst()&&c.getString(0).contains(table)&&c.getString(0).contains("RETENTION_AGGREGATE_CAPACITY"),"durable capacity blocker "+table);
                    }
                    row.put(key,"numbered-row");
                    check((Long)save.invoke(store,table,key,row)>0,"existing cache key remains writable "+table);
                    check(scalar(db,"SELECT COUNT(*) FROM "+table)==4096,"existing cache update preserves cap "+table);
                }
            }
            integrity(db);
            JSONObject receipt=new JSONObject();receipt.put("status","PASS");receipt.put("assertions",assertions);
            receipt.put("assertion_count",assertions.length());receipt.put("mode",args[3]);
            receipt.put("scope","Actual current DbHelper/research coordinator/registry/revision/Governor methods with real host SQLite and Android API adapters. Synthetic mode uses synthetic metadata and archive receipts; Service/native byte verification, phone lifecycle and natural Future are separate. Customer mode creates no synthetic data.");
            Files.writeString(Path.of(args[2]),receipt.toString(2)+'\n');
            System.out.println("PASS|diagnosis migration/retention|"+args[3]+"|assertions="+assertions.length());
        }finally{db.shutdown();}
    }
}
