package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import sun.misc.Unsafe;
/** Real additive migration and the actual production certify/intent methods. All quality inputs are synthetic. */
public final class CertificateVersionR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void atomic(Connection c,SQLiteDatabase db,ResearchWriteQueueR27.Batch b){try{c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,b,"version-host");c.commit();c.setAutoCommit(true);}catch(Exception e){try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            db.execSQL("CREATE TABLE r27_certificate(policy_hash TEXT PRIMARY KEY,status TEXT NOT NULL,gates_json TEXT NOT NULL,evidence_digest TEXT NOT NULL,issued_at INTEGER NOT NULL,expires_at INTEGER NOT NULL)");
            db.execSQL("INSERT INTO r27_certificate VALUES('legacy','ACTIVE','{\"old\":true}','unchanged',123,456)");
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,dirty INT,open_time INT)");
            connection.setAutoCommit(false);SchemaR27.ensure(db);connection.commit();connection.setAutoCommit(true);
            require("ACTIVE|{\"old\":true}|unchanged|123|456|LEGACY_UNVERSIONED".equals(text(db,"SELECT status||'|'||gates_json||'|'||evidence_digest||'|'||issued_at||'|'||expires_at||'|'||gate_version FROM r27_certificate WHERE policy_hash='legacy'")),"LEGACY_CERTIFICATE_REWRITTEN_OR_FALSELY_VERSIONED");
            connection.setAutoCommit(false);SchemaR27.ensure(db);connection.commit();connection.setAutoCommit(true);
            require("LEGACY_UNVERSIONED".equals(text(db,"SELECT gate_version FROM r27_certificate WHERE policy_hash='legacy'")),"MIGRATION_NOT_IDEMPOTENT");
            try{db.execSQL("UPDATE r27_certificate SET gate_version='' WHERE policy_hash='legacy'");throw new AssertionError("EMPTY_VERSION_ACCEPTED");}catch(RuntimeException expected){}
            String id="synthetic-current";PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","HOST",0,32,"synthetic certificate version mechanics"));
            for(int k=0;k<3;k++){String owner=k==0?id:"control-"+k,status=k==0?"ACTIVE":k==1?"CONTROL_I00":"CONTROL_NULL";db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at) VALUES(?,?,1,?, ?,?,'SYNTHETIC_HOST',1,1)",new Object[]{owner,spec.fullJson(),StoreR27.pack(new double[10]),k+1,status});
                double[] p=new double[10];Arrays.fill(p,.1);for(int n=0;n<300;n++)db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at) VALUES(?,?,?,1,?,1,2,3,1,'FROZEN',1,10000,?,20000)",new Object[]{owner,1000+n,"host-"+(1000+n),StoreR27.pack(p),k==0?(n%5<2?1:0):(n%10<3?1:0)});
            }
            db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at) VALUES(?,1,.001,20,0,300,300,120,30,70,1299,?,1)",new Object[]{id,SequentialQualificationR27.QUALIFIED});
            db.execSQL("INSERT INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,-1,'NONE',1)",new Object[]{id});
            db.execSQL("INSERT INTO r27_calibration(policy_hash,method,fit_source,fit_rows,validation_rows,excluded_late,excluded_gap,a,b,ece,ece_by_stratum,state,reason,updated_at) VALUES(?,'HOST','SYNTHETIC',120,300,0,0,1,0,.03,'{}','FITTED','HOST',1)",new Object[]{id});
            Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);Unsafe unsafe=(Unsafe)field.get(null);RuntimeR27 runtime=(RuntimeR27)unsafe.allocateInstance(RuntimeR27.class);ResearchPoolR27 pool=(ResearchPoolR27)unsafe.allocateInstance(ResearchPoolR27.class);
            java.util.concurrent.atomic.AtomicBoolean revokeBeforeWrite=new java.util.concurrent.atomic.AtomicBoolean(false);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,b)->{
                if(revokeBeforeWrite.getAndSet(false)) {
                    com.ethk3.novaonepro.DatabaseCoordinator.WriteCallable<Integer> prior=b.before;
                    b.before=w->{com.ethk3.novaonepro.SettlementEngine.revokeCertificatesForDrawWithinR30(w,"host-1000","SYNTHETIC_SOURCE_REVISION","synthetic;source'event");return prior==null?0:prior.call(w);};
                }
                atomic(connection,db,b);
            },()->false);unsafe.putObject(pool,unsafe.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);ResearchPoolR27.Worker worker=pool.new Worker(db,"version-host");
            LiveShadowsR27.certify(runtime,worker,1299);
            require("ACTIVE|R27_GATE_V1".equals(text(db,"SELECT status||'|'||gate_version FROM r27_certificate WHERE policy_hash='synthetic-current'")),"ACTUAL_CERTIFY_DID_NOT_BIND_V1");
            require(text(db,"SELECT gates_json FROM r27_certificate WHERE policy_hash='synthetic-current'").contains("R27_GATE_V1"),"CERTIFICATE_GATES_MISSING_VERSION");
            double[] probabilities=new double[10];Arrays.fill(probabilities,.1);long now=System.currentTimeMillis();
            db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,1300,'host-1300',1,?,1,2,3,1,'FROZEN',?,?)",new Object[]{id,StoreR27.pack(probabilities),now-5000,now+60000});
            require(MainlineBridgeSU2.r27Intent(db,1300)!=null,"CURRENT_VALID_CERTIFICATE_NOT_PROJECTED");
            for(String version:new String[]{"R29_GATE_V2","LEGACY_UNVERSIONED"}){db.execSQL("UPDATE r27_certificate SET gate_version=? WHERE policy_hash=?",new Object[]{version,id});require(MainlineBridgeSU2.r27Intent(db,1300)==null,"INCOMPARABLE_VERSION_PROJECTED:"+version);}
            db.execSQL("UPDATE r27_certificate SET gate_version='R27_GATE_V1',expires_at=1 WHERE policy_hash=?",new Object[]{id});require(MainlineBridgeSU2.r27Intent(db,1300)==null,"EXPIRED_CERTIFICATE_PROJECTED");
            require("0".equals(text(db,"SELECT count(*) FROM r27_certificate WHERE gate_version IS NULL OR length(trim(gate_version))=0")),"VERSION_COLUMN_NOT_TOTAL");
            LiveShadowsR27.certify(runtime,worker,1299);
            revokeBeforeWrite.set(true);LiveShadowsR27.certify(runtime,worker,1299);
            require("REVOKED".equals(text(db,"SELECT status FROM r27_certificate WHERE policy_hash='synthetic-current'")),"SOURCE_REVISION_DID_NOT_REVOKE_ACTUAL_CERTIFICATE");
            java.util.Map<?,?> racedChoice=(java.util.Map<?,?>)JsonR27.parse(text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'"));
            require(((Number)racedChoice.get("qualified")).longValue()==0,"SOURCE_RACE_LEFT_REVOKED_POLICY_IN_COMMITTED_CHOICE");
            LiveShadowsR27.certify(runtime,worker,1299);
            java.util.Map<?,?> revokedChoice=(java.util.Map<?,?>)JsonR27.parse(text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'"));
            require("REVOKED".equals(text(db,"SELECT status FROM r27_certificate WHERE policy_hash='synthetic-current'"))&&((Number)revokedChoice.get("qualified")).longValue()==0,"REVOKED_POLICY_REAPPEARED_IN_ACTUAL_PRODUCER_CHOICE");
            System.out.println("R30_CERTIFICATE_VERSION_SQLITE|LEGACY_BYTES_PRESERVED_UNKNOWN|IDEMPOTENT_ADDITIVE_MIGRATION|EMPTY_VERSION_REJECTED|ACTUAL_CERTIFY_V1|CURRENT_PROJECTION_PASS|V2_AND_LEGACY_NOT_COMPARABLE|EXPIRED_REJECTED|NO_V2_AUTHORIZATION|SOURCE_RACE_AND_OLD_EVIDENCE_REISSUE_CHOICE_ZERO|PASS");
        }
    }
}
