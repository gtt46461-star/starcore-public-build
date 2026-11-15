package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.sql.*;
import java.util.*;
/** Real SQLite transactions and actual compiled authority readers. Timing is HOST only. */
public final class CertificateRevocationR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void event(SQLiteDatabase db,String mode,String issue){
        if(mode.equals("numbers"))db.execSQL("UPDATE draws SET n1=2,n2=1 WHERE issue=?",new Object[]{issue});
        else if(mode.equals("hash"))db.execSQL("UPDATE draws SET draw_hash_v2='revised' WHERE issue=?",new Object[]{issue});
        else if(mode.equals("dirty"))db.execSQL("UPDATE draws SET dirty=1 WHERE issue=?",new Object[]{issue});
        else SettlementEngine.revokeCertificatesForDrawWithinR30(db,issue,"CANONICAL_DRAW_HASH_CONFLICT","hash;quoted'conflict");
    }
    static void lateSchema()throws Exception {
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            c.setAutoCommit(false);SQLiteDatabase db=new SQLiteDatabase(c);
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,draw_hash_v2 TEXT)");
            CertificateRevocationR30.ensure(db);require(count(db,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'r30_revoke_su2_%'")==0,"MISSING_SU2_SOURCE_FABRICATED_AS_READY");
            for(String ddl:RuntimeSchemaR26.DDL)db.execSQL(ddl);StrategySchemaSU2.ensure(db);CertificateRevocationR30.ensure(db);
            require(count(db,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'r30_revoke_su2_%'")>=3,"LATER_SU2_SCHEMA_MISSED_REVOCATION_GUARDS");c.commit();
            System.out.println("R30_CERTIFICATE_REVOCATION_SQLITE|LATE_SU2_SCHEMA_ADDITIVE_GUARDS|NO_DROP|PASS");
        }
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");
        lateSchema();
        for(String mode:new String[]{"numbers","hash","dirty","canonical"})try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SQLiteDatabase db=new SQLiteDatabase(c);c.setAutoCommit(false);
            for(String sql:RuntimeSchemaR26.DDL)db.execSQL(sql);StrategySchemaSU2.ensure(db);
            for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE"))db.execSQL(sql);
            db.execSQL("CREATE TABLE host_state(key TEXT PRIMARY KEY,value TEXT)");db.execSQL("INSERT INTO host_state VALUES(?, '1')",new Object[]{RealAuthoritySU2.USER_SWITCH});
            db.execSQL("CREATE TABLE trades(target_issue TEXT PRIMARY KEY,action TEXT)");db.execSQL("CREATE TABLE v57_shadow_observation_r35(target_issue TEXT PRIMARY KEY,decision_action TEXT,pre_draw_frozen INT)");
            db.execSQL("CREATE TABLE su1_source_clock(singleton INT PRIMARY KEY,generation INT)");db.execSQL("INSERT INTO su1_source_clock VALUES(1,1)");
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");db.execSQL("INSERT INTO draws VALUES('202610310001',0,1,2,3,4,5,6,7,8,9,10)");
            String target="202610310002";long seq=TargetIssueResolver.sequenceOrdinalSU1(target),now=System.currentTimeMillis();
            ModelGenomeSU2 genome=FamilyRegistryR26.genome("F17").bindBirth(HashingSU1.text("SYNTHETIC_AUTHORITY_FIXTURE"),seq-1);
            String key=genome.key(),cert=HashingSU1.text("SYNTHETIC_CERTIFICATE_NOT_NATURAL_QUALITY"),program=HashingSU1.text(genome.text());
            db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,created_at) VALUES(?,?,'synthetic','F15','SEED','HOST',?,'LEAGUE',?)",new Object[]{key,genome.text(),seq-1,now});
            db.execSQL("INSERT INTO su2_mainline_choice VALUES(?,?,1,0,'HOST',?,?)",new Object[]{seq,"SU2:"+key,seq-1,now});
            db.execSQL("INSERT INTO su2_certificate(certificate_id,genome_key,program_hash,window_start,window_end,acts,hits,lower_bound,coverage,worst_block,look,alpha,calibration_a,calibration_b,ece,issued_at,expires_at) VALUES(?,?,?,1,70000,1000,400,.35,.2,.3,1,.001,1,0,.03,?,?)",new Object[]{cert,key,program,now-1000,now+60000});
            db.execSQL("INSERT INTO r26_certificate_protocol VALUES(?,'R26_PREREGISTERED_AGGREGATE_LCB_V3',.02,?)",new Object[]{cert,HashingSU1.text("SYNTHETIC")});
            db.execSQL("INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time) VALUES(?,?,1,7,1,.5,.4,?,?)",new Object[]{key,seq,now-6000,now+60000});
            db.execSQL("ALTER TABLE draws ADD COLUMN fetch_time INT DEFAULT 1");db.execSQL("ALTER TABLE draws ADD COLUMN draw_hash_v2 TEXT DEFAULT 'original'");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){for(int i=0;i<100;i++)out.writeFloat(.1f);}
            db.execSQL("INSERT INTO r26_frozen VALUES(?,?,?,?,?,?,1,?,?,?)",new Object[]{key,seq,seq-1,HashingSU1.text("SYNTHETIC_CHECKPOINT"),HashingSU1.text("SYNTHETIC_STATE"),HashingSU1.text("SYNTHETIC_SOURCE"),now-6000,now+60000,bytes.toByteArray()});
            db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at) VALUES(?,?,X'01020304','retained','retained','',?,1,?,1)",new Object[]{key,genome.text(),seq-1,seq-1});
            PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","HOST",0,32,"synthetic revocation fixture"));String policy=spec.policyHash();
            db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,base_policy) VALUES(?,?,1,?,1,'ACTIVE','HOST',1,1,?)",new Object[]{policy,spec.fullJson(),StoreR27.pack(new double[10]),policy});
            db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at) VALUES(?,1,.001,20,0,300,300,120,30,70,?,?,1)",new Object[]{policy,seq-1,SequentialQualificationR27.QUALIFIED});
            db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version) VALUES(?,'ACTIVE','{\"original\":true}','original-r27-evidence',?,?,'R27_GATE_V1')",new Object[]{policy,now-1000,now+60000});
            db.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,1)",new Object[]{JsonR27.write(Collections.singletonMap("choice",policy))});
            double[] probabilities=new double[10];Arrays.fill(probabilities,.1);db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,1,?,1,2,3,1,'FROZEN',?,?)",new Object[]{policy,seq,target,StoreR27.pack(probabilities),now-6000,now+60000});
            CertificateRevocationR30.ensure(db);CertificateRevocationR30.ensure(db);c.commit();c.setAutoCommit(true);
            DbStore store=new DbStore(db);String candidate=CertificateSU2.PREFIX+key;
            require(MainlineBridgeSU2.r27Intent(db,seq)!=null&&RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"PRE_REVOCATION_READERS_NOT_LEGAL");
            String protectedState=text(db,"SELECT hex(state)||'|'||state_hash||'|'||checkpoint_hash FROM r26_live_model");
            String qualification=text(db,"SELECT global_index||'|'||n||'|'||acts||'|'||hits||'|'||log_e_up||'|'||last_ordinal FROM r27_eprocess");
            db.execSQL("UPDATE draws SET fetch_time=2,n1=n1");db.execSQL("INSERT INTO draws(issue,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES('2026103001440',0,1,2,3,4,5,6,7,8,9,10)");
            require(count(db,"SELECT count(*) FROM r30_certificate_revocation_event")==0&&CertificateSU2.read(db,key,now)!=null,"METADATA_NOOP_OR_NEW_DRAW_REVOKED_CERTIFICATE");
            c.setAutoCommit(false);event(db,mode,"202610310001");
            require("REVOKED".equals(text(db,"SELECT status FROM r27_certificate"))&&CertificateSU2.read(db,key,now)==null,"REVOCATION_NOT_VISIBLE_IN_SAME_TRANSACTION");c.rollback();c.setAutoCommit(true);
            require("ACTIVE".equals(text(db,"SELECT status FROM r27_certificate"))&&CertificateSU2.read(db,key,now)!=null&&count(db,"SELECT count(*) FROM r30_certificate_revocation_event")==0,"ROLLBACK_LEFT_PARTIAL_REVOCATION");
            long started=System.nanoTime();c.setAutoCommit(false);event(db,mode,"202610310001");c.commit();c.setAutoCommit(true);
            require(MainlineBridgeSU2.r27Intent(db,seq)==null&&!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"NEXT_ACTUAL_READER_DID_NOT_REJECT");
            double rejectMs=(System.nanoTime()-started)/1e6;require(rejectMs<=5000,"HOST_REVOKE_TO_REJECT_EXCEEDED_5000MS:"+rejectMs);
            require(count(db,"SELECT count(*) FROM r30_revoked_policy")==2&&count(db,"SELECT count(*) FROM r26_certificate_revocation")==1,"PROTOCOLS_NOT_ATOMICALLY_REVOKED");
            require(protectedState.equals(text(db,"SELECT hex(state)||'|'||state_hash||'|'||checkpoint_hash FROM r26_live_model"))&&qualification.equals(text(db,"SELECT global_index||'|'||n||'|'||acts||'|'||hits||'|'||log_e_up||'|'||last_ordinal FROM r27_eprocess")),"MODEL_OR_QUALIFICATION_WAS_RESET");
            db.execSQL("INSERT OR REPLACE INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version) VALUES(?,'ACTIVE','{\"attempt\":true}','attempt',?,?,'R27_GATE_V1')",new Object[]{policy,now,now+60000});
            require("REVOKED".equals(text(db,"SELECT status FROM r27_certificate")),"OLD_R27_EVIDENCE_RESURRECTED_CERTIFICATE");
            require("{\"original\":true}".equals(text(db,"SELECT gates_json FROM r30_revoked_certificate WHERE evidence_digest='original-r27-evidence'")),"ORIGINAL_CERTIFICATE_EVIDENCE_NOT_ARCHIVED");
            String reissue=HashingSU1.text("SYNTHETIC_REISSUE");db.execSQL("INSERT INTO su2_certificate SELECT ?,genome_key,program_hash,window_start,window_end,acts,hits,lower_bound,coverage,worst_block,paired_upper,look,alpha,calibration_a,calibration_b,ece,issued_at,expires_at FROM su2_certificate WHERE certificate_id=?",new Object[]{reissue,cert});
            require(CertificateSU2.read(db,key,now)==null&&count(db,"SELECT count(*) FROM su2_certificate")==2&&count(db,"SELECT count(*) FROM r26_certificate_revocation")==2,"OLD_SU2_EVIDENCE_RESURRECTED_OR_OLD_ROW_LOST");
            require(CertificateRevocationR30.report(db).get("deviceRevokeToRejectMs")==null,"HOST_TIME_FALSELY_LABELED_DEVICE");
            System.out.println("R30_CERTIFICATE_REVOCATION_SQLITE|"+mode+"|SAME_TX_REVOKE_AND_ATOMIC_ROLLBACK|ACTUAL_NEXT_READERS_REJECT|HOST_REVOKE_TO_REJECT_MS="+rejectMs+"|ORIGINAL_CERT_MODEL_CURSOR_RETAINED|OLD_EVIDENCE_REISSUE_BLOCKED|METADATA_NOOP_NEW_DRAW_PRESERVED|CONSERVATIVE_SCOPE_EXPLICIT|PASS");
        }
    }
}
