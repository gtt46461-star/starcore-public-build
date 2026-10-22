package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.LinkedHashMap;

/** Synthetic strict-OOS inputs; actual FTRL owner commits before projection fault injection. */
public final class ResearchTelemetryR77 {
    static int checks;
    static void ok(boolean v,String m){if(!v)throw new AssertionError(m);checks++;}
    static String h(String x){return V51ChecksumEngine.sha256(x);}
    static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static final class FaultDb extends SQLiteDatabase {
        String prefix="";int seen,failAt,injected;
        FaultDb(String bridge,String path)throws Exception{super(bridge,path);}
        void arm(String value,int at){prefix=value;seen=0;failAt=at;}
        @Override public synchronized long insertWithOnConflict(String table,String nullColumn,ContentValues values,int conflict){
            String key=values.getAsString("state_key");
            if("app_state".equals(table)&&!prefix.isEmpty()&&key!=null&&key.startsWith(prefix)&&++seen==failAt){
                prefix="";injected++;throw new android.database.sqlite.SQLiteException("R77_SYNTHETIC_POSTCOMMIT_DISPLAY_FAILURE");
            }
            return super.insertWithOnConflict(table,nullColumn,values,conflict);
        }
    }
    static String issue(int i){return String.format(java.util.Locale.ROOT,"20261013%04d",i);}
    static void seed(SQLiteDatabase d,int i){
        String target=issue(i),base=issue(i-1);long frozen=1000+i*100,opened=frozen+50;
        d.execSQL("INSERT INTO v57_oos_label_r35(target_issue,game_code,base_issue,position,n1,n2,n3,selected_hit_p,actual_number,hit_label,strict_oos,pre_draw_frozen,prediction_created_at,draw_open_time,settled_at,provenance_hash,record_hash) VALUES(?,'Binance1-PK10',?,1,1,2,3,.35,1,1,1,1,?,?,?,?,?)",new Object[]{target,base,frozen,opened,opened+1,h("provenance"+i),h("oos"+i)});
        d.execSQL("INSERT INTO v57_selected_evidence_r906(target_issue,action_ordinal,action_hash,raw_action_p_value,raw_action_p_status,raw_action_p_source,rank_no,rank_score_value,rank_status,third_margin_value,third_margin_status,entropy_risk_value,entropy_risk_status,entropy_risk_source,stability_value,stability_status,stability_source,window_consensus_value,window_consensus_status,assignment_support_status,metric_schema_hash,frozen_at,record_hash) VALUES(?,0,?,.35,'AVAILABLE','fixture',1,.7,'AVAILABLE',.1,'AVAILABLE',.1,'AVAILABLE','fixture',.9,'AVAILABLE','fixture',.9,'AVAILABLE','AVAILABLE',?,?,?)",new Object[]{target,h("action"+i),h("metrics"),frozen,h("evidence"+i)});
        d.execSQL("INSERT INTO v57_settlement_task_r36(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',?,?,?,?,?)",new Object[]{target,target,opened,opened,opened,h("draw"+i),h("settlement"+i)});
        d.execSQL("INSERT INTO v57_shadow_observation_r35(target_issue,game_code,base_issue,position,n1,n2,n3,lane,decision_action,reject_code,bundle_hash,contract_hash,authorization_hash,record_hash,created_at,pre_draw_frozen,settled) VALUES(?,'Binance1-PK10',?,1,1,2,3,'SHADOW','HOLD','fixture',?,?,?,?,?,1,1)",new Object[]{target,base,h("b"),h("c"),h("auth"),h("shadow"+i),frozen});
    }
    public static void main(String[] a)throws Exception{
        try(FaultDb d=new FaultDb(a[0],a[1])){
            DbStore store=HostProductR974.store(d);RealtimePressureGateR811.resetForTests();
            d.beginTransaction();try{seed(d,1);seed(d,2);d.setTransactionSuccessful();}finally{d.endTransaction();}
            d.arm("r811_ftrl_",2);
            ResearchAdaptiveLearnerStoreR811.UpdateResult first=ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store,issue(1));
            ok(first.updated&&first.updates==1,"actual committed learner result survives display failure");
            ok(d.injected==1,"fault occurred on second FTRL projection key");
            ok(n(d,"SELECT COUNT(*) FROM v57_research_artifact_r811")==1,"numeric artifact remains committed");
            ok(n(d,"SELECT COUNT(*) FROM app_state WHERE state_key LIKE 'r811_ftrl_%'")==0,"partial display batch is rolled back");
            ok(!d.inTransaction(),"postcommit failure leaves no open transaction");
            ResearchAdaptiveLearnerStoreR811.UpdateResult replay=ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store,issue(1));
            ok(replay.updated&&replay.updates==1&&n(d,"SELECT COUNT(*) FROM v57_research_artifact_r811")==1,"replay remains exactly once");
            ResearchAdaptiveLearnerStoreR811.UpdateResult second=ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store,issue(2));
            ok(second.updated&&second.updates==2,"next period learner proceeds from durable model");
            java.lang.reflect.Method publish=ResearchSettlementSidecarR811.class.getDeclaredMethod("publishLearnerPair",DbStore.class,String.class,String.class,ResearchTinyDeepLearnerR909.Result.class);publish.setAccessible(true);
            d.arm("r947_learning_pair_",1);
            publish.invoke(null,store,issue(2),second.status,new ResearchTinyDeepLearnerR909.Result(false,"FIXTURE_NO_TINY_UPDATE","","",0,-1));
            ok(d.injected==2,"actual sidecar projection tolerates independent display failure");
            ok(n(d,"SELECT COUNT(*) FROM app_state WHERE state_key='r811_adaptive_learner_status'")==0,"sidecar projection also rolls back its prior key");
            ok(n(d,"SELECT COUNT(*) FROM v57_research_artifact_r811")==2,"sidecar summary cannot change learner receipts");
            LinkedHashMap<String,String> map=new LinkedHashMap<>();map.put("r77_nested_projection","forbidden");boolean rejected=false;
            d.beginTransaction();try{try{store.trySetTelemetryStatesR77(map);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("INDEPENDENT_TRANSACTION");}}finally{d.endTransaction();}
            ok(rejected&&n(d,"SELECT COUNT(*) FROM app_state WHERE state_key='r77_nested_projection'")==0,"optional display API rejects nested use before writing");
            ok(n(d,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(d,"SELECT COUNT(*) FROM trades")==0,"synthetic learner fixture creates no qualification or REAL");
            Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_FTRL_COMMIT_AND_SIDECAR_DISPLAY_FAULT_SYNTHETIC_HOST\"}\n");
        }
    }
}
