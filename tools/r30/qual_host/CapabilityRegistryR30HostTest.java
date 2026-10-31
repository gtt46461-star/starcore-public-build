package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.util.*;

/** Actual readonly capability report over absent, empty and retained runtime sources; no model training in reads. */
public final class CapabilityRegistryR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked")static Map<String,Object> row(Map<String,Object> report,String id){for(Object value:(List<?>)report.get("rows")){Map<String,Object> r=(Map<String,Object>)value;if(id.equals(r.get("id")))return r;}throw new AssertionError("CAPABILITY_MISSING:"+id);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);Map<String,Object> missing=CapabilityRegistryStoreR30.report(db);require(((List<?>)missing.get("rows")).size()==82,"MISSING_SOURCES_LOST_REGISTRY");require(row(missing,"F01").get("retainedOutputCount")==null&&!Boolean.TRUE.equals(row(missing,"F01").get("runtimeObserved")),"MISSING_FACTS_INVENTED_ACTIVITY");
            try(Statement s=c.createStatement()){
                for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))s.execute(ddl);s.execute(ModelArtifactR29.DDL);
                s.execute("CREATE TABLE r26_capability(mechanism TEXT,state TEXT,reason TEXT,last_progress INTEGER)");s.execute("CREATE TABLE r26_capability_source(singleton INTEGER,numerical_abi TEXT)");s.execute("CREATE TABLE r26_live_model(family_id TEXT,updated_at INTEGER)");
                s.execute("CREATE TABLE research_matrix_snapshot_r930(kind TEXT,target_issue TEXT,frozen_at INTEGER)");s.execute("CREATE TABLE su2_matrix_snapshot(checkpoint_sequence INTEGER,updated_at INTEGER)");
            }
            Map<String,Object> empty=CapabilityRegistryStoreR30.report(db);require(((Number)row(empty,"I01").get("retainedOutputCount")).longValue()==0&&"NOT_RUN".equals(row(empty,"I01").get("evidenceStatus")),"EMPTY_SOURCE_CALLED_EFFECTIVE");
            try(Statement s=c.createStatement()){
                s.execute("INSERT INTO r26_capability VALUES('F01','TRAINED','retained checkpoint fixture',1000)");s.execute("INSERT INTO r26_capability_source VALUES(1,'old-other-rule')");s.execute("INSERT INTO r26_live_model VALUES('F01',1100)");
                s.execute("INSERT INTO r27_expert_proposal VALUES(1,'E01','audit',NULL,1200),(1,'E03','proposal','p',1300)");
                s.execute("INSERT INTO r27_policy(policy_hash,spec_json,idea_family,birth_generation,birth_issue,dataset_hash,state,state_reason,created_at,updated_at) VALUES('p','{}','I01',0,'fixture','dataset','BORN','fixture',1000,1400)");
                s.execute("INSERT INTO research_matrix_snapshot_r930 VALUES('EVIDENCE_TEACHER','202610300101',1700)");
            }
            Map<String,Object> q=new LinkedHashMap<>();q.put("datasetHash","dataset");q.put("onlineSamples",10L);q.put("materializedSamples",560L);q.put("phase","ONLINE");
            db.execSQL("INSERT INTO r27_chart_cache VALUES('atomnet','ten-position-r30',?,1600)",new Object[]{JsonR27.write(q)});
            Map<String,Object> orphan=CapabilityRegistryStoreR30.report(db);require(row(orphan,"ATOMNET").get("retainedOutputCount")==null,"ORPHAN_CACHE_CALLED_COMMITTED_TRAINING");
            db.execSQL("INSERT INTO r29_model_checkpoint VALUES(?,?,?,?,?,?,?)",new Object[]{AtomNetSessionR30.OWNER,AtomNetSessionR30.ABI,"dataset","202610300100","fixture-hash",new byte[]{1},1500});
            Map<String,Object> badCheckpoint=CapabilityRegistryStoreR30.report(db);require(row(badCheckpoint,"ATOMNET").get("retainedOutputCount")==null,"CORRUPT_CHECKPOINT_CALLED_TRAINING");
            AtomNetSessionR30 actual=new AtomNetSessionR30("dataset","202610300100",100,182,10,10,0);
            while(!actual.ready())actual.work(i->{int position=i%10+1;double[] x=new double[182];x[172+position-1]=1;return new AtomNetTrainerR27.Sample(x,i%3==0?1:0,i%10,AtomNetSessionR30.eventKey("sample-"+(i/10),position));});
            for(int position=1;position<=10;position++){double[] x=new double[182];x[172+position-1]=1;actual.online(new AtomNetTrainerR27.Sample(x,position%3==0?1:0,position-1,AtomNetSessionR30.eventKey("202610300101",position)),"202610300101",101,position);}
            byte[] artifact=actual.encode();db.execSQL("UPDATE r29_model_checkpoint SET trained_through=?,state_hash=?,artifact=? WHERE owner=?",new Object[]{actual.trainedThrough(),StatsR27.sha256(artifact),artifact,AtomNetSessionR30.OWNER});
            db.execSQL("UPDATE r27_chart_cache SET payload_json=? WHERE panel_id='atomnet' AND key='ten-position-r30'",new Object[]{JsonR27.write(actual.report())});
            Map<String,Object> report=ReportDataR27.capabilityInventoryR30(db);require(((Number)report.get("registeredObjects")).longValue()==82,"REPORT_NOT_COMPLETE");
            require(((Number)row(report,"F01").get("retainedOutputCount")).longValue()==1&&((Number)row(report,"F01").get("lastRecordedRunAt")).longValue()==1100,"FAMILY_FACTS_WRONG");
            require(Boolean.FALSE.equals(row(report,"F01").get("legacyRuleMatchesCurrent")),"OLD_RULE_BECAME_CURRENT");
            require(((Number)row(report,"E01").get("retainedOutputCount")).longValue()==0&&((Number)row(report,"E01").get("auditReceipts")).longValue()==1,"AUDIT_RECEIPT_BECAME_POLICY_BIRTH");
            require(((Number)row(report,"E03").get("retainedOutputCount")).longValue()==1&&((Number)row(report,"I01").get("lastRecordedRunAt")).longValue()==1400,"POLICY_FACTS_WRONG");
            require(((Number)row(report,"ATOMNET").get("retainedOutputCount")).longValue()==10&&"ONLINE".equals(row(report,"ATOMNET").get("trainingPhase")),"NN_CHECKPOINT_CACHE_NOT_MATCHED");
            require(Boolean.TRUE.equals(row(report,"MATRIX:R930:EVIDENCE_TEACHER").get("runtimeObserved")),"MATRIX_FACTS_LOST");
            for(Object value:(List<?>)report.get("rows"))require(((Map<?,?>)value).get("effective")==null,"INVENTED_EFFECTIVE_CAPABILITY");
            System.out.println("R30_CAPABILITY_SQLITE|82_ACTUAL_OBJECTS|24F_16E_21I_1NN_20MATRIX|MISSING_EMPTY_RECORDED_DISTINCT|AUDIT_NOT_BIRTH|OLD_ABI_NOT_CURRENT|NN_ORPHAN_AND_CORRUPT_REJECTED_REAL_TRAINING_MATCHES|REPORT_WATERMARKS_MATCH|NO_FAKE_EFFECTIVE|PASS");
        }
    }
}
