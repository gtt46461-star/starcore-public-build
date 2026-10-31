package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.sql.*;
import java.util.*;

/** Real SQLite projection/receipts. Injected quality values test mechanics, never prove natural qualification. */
public final class DecisionExplanationR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static DecisionChainR30.Facts all(){DecisionChainR30.Facts f=new DecisionChainR30.Facts();f.dataHealthy=f.freezeComplete=f.modelAvailable=f.certificateActive=f.selectorConfirmed=f.onePerIssue=f.stakeFixed=f.actRuleFired=f.userRealEnabled=true;return f;}
    static void persist(Connection c,SQLiteDatabase db,DecisionExplanationStoreR30.Observation observation,boolean fail)throws Exception{
        try{c.setAutoCommit(false);DecisionExplanationStoreR30.persistWithin(db,observation);if(fail)throw new SQLException("INJECTED_EXPLANATION_COMMIT_FAILURE");c.commit();c.setAutoCommit(true);}catch(Exception e){c.rollback();c.setAutoCommit(true);throw e;}
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            require(DecisionExplanationStoreR30.report(db).get("decisions")==null,"MISSING_SOURCE_REPORTED_ZERO");
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);for(String ddl:DecisionExplanationStoreR30.DDL)db.execSQL(ddl);
            require("NOT_RUN".equals(DecisionExplanationStoreR30.report(db).get("status")),"EMPTY_LEDGER_REPORTED_PASS");
            PolicySpecR27 policy=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","HOST",0,32,"synthetic explanation fixture"));
            String id=policy.policyHash(),issue="202610310002";long ordinal=TargetIssueResolver.sequenceOrdinalSU1(issue),now=System.currentTimeMillis();
            db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,base_policy,semantic_abi) VALUES(?,?,1,?,1,'ACTIVE','HOST',1,1,?,'host-abi')",new Object[]{id,policy.fullJson(),StoreR27.pack(new double[10]),id});
            db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,sum_u,sum_u2,last_ordinal,state,updated_at) VALUES(?,1,.001,20,0,300,0,0,?,?,1)",new Object[]{id,ordinal-1,SequentialQualificationR27.QUALIFIED});
            db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version) VALUES(?,'ACTIVE','{}','synthetic',?,?,?)",new Object[]{id,now-1000,now+60000,CertificateGateR27.GATE_VERSION});
            db.execSQL("INSERT INTO r27_semantic_fingerprint(policy_hash,build,fingerprint,operator_abi,computed_at) VALUES(?,?,'actual-fixture-fingerprint','actual-fixture-operator',1)",new Object[]{id,BuildConfig.SOURCE_COMMIT_R26});
            db.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,1)",new Object[]{JsonR27.write(Collections.singletonMap("choice",id))});
            double[] probabilities=new double[10];Arrays.fill(probabilities,.1);
            db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,1,?,1,2,3,1,'FROZEN',?,?)",new Object[]{id,ordinal,issue,StoreR27.pack(probabilities),now-6000,now+60000});
            MainlineBridgeSU2.ProjectionR30 p=MainlineBridgeSU2.projectionR30(db,ordinal,now);
            require(Boolean.TRUE.equals(p.certificateActive)&&Boolean.TRUE.equals(p.freezeComplete)&&Boolean.TRUE.equals(p.modelAvailable)&&Boolean.TRUE.equals(p.actRuleFired),"ACTUAL_CURRENT_CERTIFICATE_FREEZE_OR_MODEL_NOT_PROJECTED");
            require(Boolean.FALSE.equals(p.selectorConfirmed)&&"AWAITING_EXISTING_AUTHORIZATION_CHAIN".equals(p.sources.get("authorizationState")),"R27_REQUEST_FALSELY_CLAIMED_TYPED_AUTHORIZATION");
            DecisionChainR30.Facts facts=all();facts.selectorConfirmed=p.selectorConfirmed;
            DecisionExplanationStoreR30.Observation blocked=DecisionExplanationStoreR30.observe(issue,"HOST_R27_REQUEST","actual-projection",false,"AWAITING_EXISTING_AUTHORIZATION_CHAIN",facts,p.sources,now);
            for(int i=0;i<100;i++)persist(connection,db,blocked,false);
            require(count(db,"SELECT count(*) FROM r30_decision_explain")==1&&count(db,"SELECT count(*) FROM r30_decision_mismatch")==0,"REPEATED_DECISION_DUPLICATED_OR_MISMATCHED");
            Map<String,Object> report=DecisionExplanationStoreR30.report(db);require(((Map<?,?>)report.get("certificatePresentButNoReal")).get("selectorConfirmed:FAIL").equals(1L),"CERTIFICATE_NO_ACTION_REASON_NOT_IN_REPORT");
            for(String version:new String[]{"LEGACY_UNVERSIONED","R29_GATE_V2"}){db.execSQL("UPDATE r27_certificate SET gate_version=? WHERE policy_hash=?",new Object[]{version,id});require(Boolean.FALSE.equals(MainlineBridgeSU2.projectionR30(db,ordinal,now).certificateActive),"WRONG_VERSION_EXPLAINED_ACTIVE");}
            db.execSQL("UPDATE r27_certificate SET gate_version='R27_GATE_V1',expires_at=? WHERE policy_hash=?",new Object[]{now-1,id});require(Boolean.FALSE.equals(MainlineBridgeSU2.projectionR30(db,ordinal,now).certificateActive),"EXPIRED_EXPLAINED_ACTIVE");
            require(Boolean.FALSE.equals(MainlineBridgeSU2.projectionR30(db,ordinal+1,now).freezeComplete),"MISSING_FREEZE_EXPLAINED_COMPLETE");
            DecisionExplanationStoreR30.Observation stricter=DecisionExplanationStoreR30.observe(issue,"HOST_FAULT_PROBE","strict",false,"ORIGINAL_GATE_BLOCKS",all(),Collections.singletonMap("scope","SYNTHETIC_FAULT_INJECTION"),now+1);
            try{persist(connection,db,stricter,true);throw new AssertionError("COMMIT_FAILURE_NOT_INJECTED");}catch(SQLException expected){}
            require(count(db,"SELECT count(*) FROM r30_decision_explain")==1&&count(db,"SELECT count(*) FROM r30_decision_mismatch")==0,"FAILED_COMMIT_LEFT_HALF_RECEIPT");
            persist(connection,db,stricter,false);require(count(db,"SELECT count(*) FROM r30_decision_mismatch WHERE direction='ORIGINAL_STRICTER'")==1,"STRICTER_DIRECTION_LOST");
            DecisionChainR30.Facts unsafeFacts=all();unsafeFacts.userRealEnabled=null;
            DecisionExplanationStoreR30.Observation unsafe=DecisionExplanationStoreR30.observe(issue,"HOST_FAULT_PROBE","unsafe",true,"INJECTED_UNSAFE_ORIGINAL",unsafeFacts,Collections.singletonMap("scope","SYNTHETIC_FAULT_INJECTION"),now+2);persist(connection,db,unsafe,false);
            require(((Number)DecisionExplanationStoreR30.report(db).get("originalMorePermissive")).longValue()==1,"UNSAFE_MISMATCH_HIDDEN_OR_RELABELED");
            System.out.println("R30_DECISION_EXPLAIN_SQLITE|REAL_R27_CERT_FREEZE_MODEL_PROJECTION|AWAITING_TYPED_AUTH_NOT_BYPASSED|100_IDEMPOTENT_RECEIPTS|CERTIFICATE_NO_ACTION_REASON|V2_LEGACY_EXPIRED_AND_MISSING_FREEZE_REJECTED|ATOMIC_ROLLBACK|BOTH_MISMATCH_DIRECTIONS_PRESERVED|FAULT_PROBES_NOT_NATURAL_QUALITY|PASS");
        }
    }
}
