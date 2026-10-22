package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import org.json.*;
import java.util.*;

/** Report-shaped regressions using actual schema/owners. Synthetic rows are never live evidence. */
public final class PhoneExecutionRegressionHarnessR959 {
    static int checks;
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void reject(Runnable r,String name){boolean failed=false;try{r.run();}catch(RuntimeException expected){failed=true;}check(failed,name);}
    static void tx(SQLiteDatabase db,Runnable r){db.beginTransaction();try{r.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static String h(String s){return V51ChecksumEngine.sha256(s);}
    static JSONObject row(int i,String tier)throws Exception{JSONObject r=new JSONObject();r.put("key","formula:fixture-"+i);r.put("family","fixture-family-"+(i%8));r.put("structure",h("fixture-structure-"+i));r.put("semantic",h("fixture-structure-"+i));r.put("tier",tier);return r;}
    static void insertRequired(SQLiteDatabase db,String table,String issue){
        ArrayList<String> cols=new ArrayList<String>();ArrayList<Object> values=new ArrayList<Object>();
        try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext()){
            String name=c.getString(1);if(name.equals("seq"))continue;
            if(name.equals("target_issue")){cols.add(name);values.add(issue);}
            else if(name.equals("game_code")){cols.add(name);values.add(ReleaseManifestR926.GAME_CODE);}
            else if(name.equals("base_issue")){cols.add(name);values.add("202609180099");}
            else if(name.matches("n[1-3]")){cols.add(name);values.add(Integer.valueOf(name.substring(1)));}
            else if(c.getInt(3)==1&&c.isNull(4)||c.getInt(5)==1){cols.add(name);values.add(c.getString(2).contains("INT")?1:c.getString(2).contains("REAL")?.5:h(table+name+issue));}
        }}
        StringBuilder names=new StringBuilder(),marks=new StringBuilder();for(String col:cols){if(names.length()>0){names.append(',');marks.append(',');}names.append(col);marks.append('?');}
        db.execSQL("INSERT INTO "+table+"("+names+") VALUES("+marks+")",values.toArray());
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DbStore store=new DbStore(db);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);V51Repository.ensure(db);
            RealtimePressureGateR811.resetForTests();check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"ACTUAL_RESEARCH_SCHEMA_READY");
            String old="202609180100",cutoff="202609190100";
            String[] immutable={Batch34SchemaR930.TABLE_CAPTURE_TRANSITION,Batch34SchemaR930.TABLE_EVIDENCE_LINK,Batch34SchemaR930.TABLE_FAILURE_CASE,Batch34SchemaR930.TABLE_POLICY_DECISION,Batch34SchemaR930.TABLE_POLICY_OUTCOME};
            for(String table:immutable){insertRequired(db,table,old);reject(()->db.delete(table,"target_issue=?",new String[]{old}),"ORIGINAL_APPEND_ONLY_TRIGGER:"+table);check(DataRetentionGovernorR900.deletePage(db,table,"target_issue",cutoff,10)==0&&n(db,"SELECT COUNT(*) FROM "+table)==1,"RETENTION_PRESERVES_IMMUTABLE_LEDGER:"+table);}
            for(String table:new String[]{DbHelper.TABLE_R35_OOS_LABEL,DbHelper.TABLE_R906_SELECTED_EVIDENCE}){insertRequired(db,table,old);insertRequired(db,table,"202609180101");}
            db.execSQL("INSERT INTO "+RuntimeEventLedgerR926.TABLE_EVENT+"(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_hash,source,created_at) VALUES('fixture-learned','fixture-learned',?,?,'fixture','fixture','LEARNED','LEARNED_EXACTLY_ONCE',100,?,'ResearchAdaptiveLearnerStoreR811',1)",new Object[]{ReleaseManifestR926.GAME_CODE,old,h("label")});
            check(DataRetentionGovernorR900.deletePage(db,RuntimeEventLedgerR926.TABLE_EVENT,"target_issue",cutoff,10)==0,"RUNTIME_EVENT_APPEND_ONLY_PRESERVED");
            for(String table:new String[]{DbHelper.TABLE_R35_OOS_LABEL,DbHelper.TABLE_R906_SELECTED_EVIDENCE}){
                reject(()->db.delete(table,"target_issue=?",new String[]{old}),"ORIGINAL_LEARNED_INPUT_TRIGGER:"+table);
                check(DataRetentionGovernorR900.deletePage(db,table,"target_issue",cutoff,10)==1&&n(db,"SELECT COUNT(*) FROM "+table+" WHERE target_issue='"+old+"'")==1,"ONLY_UNLEARNED_EXPIRED_ROW_DELETED:"+table);
            }
            JSONArray rows=new JSONArray();for(int i=0;i<120;i++)rows.put(row(i,i<40?"B":"C"));
            java.lang.reflect.Method admission=StagedResearchRosterR943.class.getDeclaredMethod("freezeAdmission",SQLiteDatabase.class,String.class,JSONArray.class);admission.setAccessible(true);
            String target="202609190200",input=h("staged-input");
            JSONArray frozen=(JSONArray)admission.invoke(null,db,target,rows);String payload=frozen.toString();
            db.execSQL("INSERT INTO "+StagedResearchRosterR943.TABLE+" VALUES(?,?,?,?,0,1)",new Object[]{target,input,payload,h(payload)});
            check(!StagedResearchRosterR943.prepareSlice(store,target,input),"FIRST_BOUNDED_PAGE_COMMITS");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE selected=1")==32,"FIRST_PAGE_ADMITS_32_SELECTED_IDENTITIES");
            db.execSQL("CREATE TRIGGER fixture_retry_fail BEFORE INSERT ON "+GlobalResearchScheduleR932.TASK+" WHEN NEW.queue_rank=40 BEGIN SELECT RAISE(ABORT,'INJECTED_RETRY_FAILURE'); END");
            reject(()->StagedResearchRosterR943.prepareSlice(store,target,input),"FAILED_SECOND_PAGE_PROPAGATES");
            check(n(db,"SELECT cursor FROM "+StagedResearchRosterR943.TABLE+" WHERE target_issue='"+target+"'")==32,"FAILED_PAGE_PRESERVES_DURABLE_CURSOR");db.execSQL("DROP TRIGGER fixture_retry_fail");
            StagedResearchRosterR943.prepare(store,target,input);
            check(n(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE selected=1 AND tier='B'")==40&&n(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE selected=1 AND tier='C'")==64,"ALL_40_EVALUATION_CANDIDATES_ADMITTED_WITH_64_NURSERY_CAP");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE eligible=1")==120&&n(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE selected=0")==16,"UNSELECTED_REMAIN_IN_OPPORTUNITY_DENOMINATOR");
            check(n(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE execution_mode='PROXY120' AND tier='C'")==80,"NURSERY_MODE_FROZEN_FOR_ENTIRE_MANIFEST");
            reject(()->db.execSQL("UPDATE "+GlobalResearchScheduleR932.TASK+" SET execution_mode='FULL1200'"),"FROZEN_EXECUTION_MODE_CANNOT_BE_CHANGED");
            check(!ResearchCaptureQueueR946.complete(store,target),"REGISTRATION_ALONE_IS_NOT_CAPTURE_COMPLETION");
            String legacy="202609190201";JSONArray legacyRows=new JSONArray();legacyRows.put(row(200,"C"));String legacyBody=legacyRows.toString();
            db.execSQL("INSERT INTO "+StagedResearchRosterR943.TABLE+" VALUES(?,?,?,?,0,1)",new Object[]{legacy,input,legacyBody,h(legacyBody)});StagedResearchRosterR943.prepare(store,legacy,input);
            check(!GlobalResearchScheduleR932.proxy(store,legacy,"formula:fixture-200"),"LEGACY_FROZEN_FULL1200_COMMITMENT_RETAINED");
            String proxyIssue="202609190202",key="fixture-proxy";List<GlobalResearchScheduleR932.Candidate> single=new ArrayList<GlobalResearchScheduleR932.Candidate>();single.add(new GlobalResearchScheduleR932.Candidate(key,"fixture",h("proxy-structure"),"C",""));
            tx(db,()->GlobalResearchScheduleR932.freeze(db,proxyIssue,input,single));
            check(GlobalResearchScheduleR932.saveProxy(store,proxyIssue,key,input,0,.3),"PROXY_COMMIT_USING_EXISTING_WRITER");
            check(ResearchCaptureQueueR946.complete(store,proxyIssue),"VALID_PROXY_CLOSES_ITS_ADMITTED_MODE");
            check(n(db,"SELECT full1200 FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE target_issue='"+proxyIssue+"'")==0,"PROXY_COMPLETION_NEVER_FABRICATES_FULL1200");
            check(n(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE)==0&&n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"SCHEDULE_COMPLETION_CANNOT_CREATE_CERTIFICATE");
            ResearchModuleRegistryR952.reconcile(store);long writes=n(db,"SELECT total_changes()");ResearchModuleRegistryR952.reconcile(store);check(n(db,"SELECT total_changes()")==writes,"CURRENT_MODULE_REGISTRY_IS_READ_ONLY");
            db.execSQL("UPDATE r952_module_registry SET revision='CORRUPT'");db.execSQL("CREATE TRIGGER fixture_module_failure BEFORE INSERT ON r952_module_registry BEGIN SELECT RAISE(ABORT,'INJECTED_MODULE_FAILURE'); END");reject(()->ResearchModuleRegistryR952.reconcile(store),"FAILED_RECONCILIATION_NOT_REPORTED_SUCCESS");
            System.out.println("R959_PHONE_EXECUTION_REGRESSION_PASS|checks="+checks+"|scope=PRODUCTION_OWNERS_REAL_SQLITE_SYNTHETIC_NOT_PHONE");
        }finally{db.shutdown();}
    }
}
