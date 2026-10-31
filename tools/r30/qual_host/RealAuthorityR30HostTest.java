package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.sql.*;

/** Executes the actual compiled RealAuthority on SQLite using a read-only DbStore adapter. */
public final class RealAuthorityR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
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
            c.commit();c.setAutoCommit(true);DbStore store=new DbStore(db);String candidate=CertificateSU2.PREFIX+key;
            require(CertificateSU2.read(db,key,now)!=null,"FIXTURE_CERTIFICATE_NOT_ACCEPTED_BY_ACTUAL_READER");
            if(args.length==0)require(!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"MISSING_FULL_FREEZE_AUTHORIZED");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){for(int i=0;i<100;i++)out.writeFloat(.1f);}
            db.execSQL("INSERT INTO r26_frozen VALUES(?,?,?,?,?,?,1,?,?,?)",new Object[]{key,seq,seq-1,HashingSU1.text("SYNTHETIC_CHECKPOINT"),HashingSU1.text("SYNTHETIC_STATE"),HashingSU1.text("SYNTHETIC_SOURCE"),now-6000,now+60000,bytes.toByteArray()});
            require(RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"VALID_EXISTING_AUTHORITY_BROKEN");
            String calibration=V51ChecksumEngine.sha256("SU2_PLATT|"+Double.toHexString(1d)+"|"+Double.toHexString(0d));
            double probability=new SelectiveCalibrationSU2.Platt(1,0).apply(EvidenceBlockR26.quantized(.4));
            require(MainlineAdapterSU2.matchesFrozenR30(store,db,candidate,cert,program,program,MainlineAdapterSU2.CONTRACT,calibration,target,now+60000,probability,1,1,2,3),"ORIGINAL_REAL_WRITER_REJECTS_VALID_SU2_FROZEN_ACTION");
            require(!MainlineAdapterSU2.matchesFrozenR30(store,db,candidate,cert,program,program,MainlineAdapterSU2.CONTRACT,calibration,target,now+60000,Math.nextUp(probability),1,1,2,3),"REAL_WRITER_ACCEPTS_CHANGED_CALIBRATED_PROBABILITY");
            require(!MainlineAdapterSU2.matchesFrozenR30(store,db,candidate,cert,program,program,MainlineAdapterSU2.CONTRACT,calibration,target,now+60000,probability,1,1,2,4),"REAL_WRITER_ACCEPTS_DIFFERENT_NUMBERS");
            db.execSQL("UPDATE su2_certificate SET calibration_a=2");
            require(!MainlineAdapterSU2.matchesFrozenR30(store,db,candidate,cert,program,program,MainlineAdapterSU2.CONTRACT,calibration,target,now+60000,probability,1,1,2,3),"REAL_WRITER_ACCEPTS_CHANGED_ORIGINAL_PLATT");
            db.execSQL("UPDATE su2_certificate SET calibration_a=1");
            try{db.execSQL("UPDATE su2_live_decision SET number_mask=11");throw new AssertionError("ORIGINAL_FROZEN_DECISION_MUTATION_ACCEPTED");}
            catch(android.database.sqlite.SQLiteException immutable){require(String.valueOf(immutable.getMessage()).contains("SU2_DECISION_FROZEN"),"UNRELATED_SQL_ERROR_NOT_IMMUTABILITY");}
            db.execSQL("UPDATE host_state SET value='0'");
            require(!MainlineAdapterSU2.matchesFrozenR30(store,db,candidate,cert,program,program,MainlineAdapterSU2.CONTRACT,calibration,target,now+60000,probability,1,1,2,3),"REAL_WRITER_BYPASSES_ORIGINAL_USER_SWITCH");
            db.execSQL("UPDATE host_state SET value='1'");
            db.execSQL("CREATE TABLE research_consumer_choice_r932(target_issue TEXT PRIMARY KEY,bundle_hash TEXT,consumer TEXT,candidate_id TEXT,certificate_hash TEXT)");
            db.execSQL("INSERT INTO research_consumer_choice_r932 VALUES(?,'HOST_SYNTHETIC','INVENTED_ALGORITHM',?,?)",new Object[]{target,candidate,cert});
            UnifiedResearchChoiceR932.Evidence chosen=UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);
            require(chosen!=null,"FROZEN_VALID_SU2_CERTIFICATE_DROPPED_BY_SELECTED_BRIDGE");
            java.lang.reflect.Method current=UnifiedResearchChoiceR932.class.getDeclaredMethod("currentSu2ChoiceR30",SQLiteDatabase.class,String.class,String.class,String.class);current.setAccessible(true);
            require(current.invoke(null,db,candidate,program,cert)!=null,"ORIGINAL_WRITER_CANNOT_FREEZE_VALID_SU2_IDENTITY");
            require(current.invoke(null,db,candidate,HashingSU1.text("DIFFERENT_PROGRAM"),cert)==null,"WRITER_ACCEPTED_SU2_PROGRAM_MISMATCH");
            require(current.invoke(null,db,candidate,program,HashingSU1.text("DIFFERENT_CERTIFICATE"))==null,"WRITER_ACCEPTED_SU2_CERTIFICATE_MISMATCH");
            require(candidate.equals(chosen.id)&&program.equals(chosen.structure)&&cert.equals(chosen.certificate)&&HashingSU1.text("SYNTHETIC").equals(chosen.evidence),"FROZEN_SU2_IDENTITY_OR_ORIGINAL_EVIDENCE_CHANGED");
            require(chosen.metric("ACT_N")==1000&&chosen.metric("HIT_RATE")==.4&&chosen.metric("COVERAGE")==.2&&chosen.metric("ECE")==.03&&Double.isNaN(chosen.metric("BRIER")),"FROZEN_SU2_OBSERVED_METRICS_OR_UNKNOWN_CHANGED");
            db.execSQL("UPDATE research_consumer_choice_r932 SET certificate_hash=?",new Object[]{HashingSU1.text("DIFFERENT_CERTIFICATE")});
            require(UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM)==null,"FROZEN_SU2_CERTIFICATE_MISMATCH_ACCEPTED");
            db.execSQL("UPDATE research_consumer_choice_r932 SET certificate_hash=?",new Object[]{cert});
            db.execSQL("UPDATE research_consumer_choice_r932 SET candidate_id=?",new Object[]{CertificateSU2.PREFIX+HashingSU1.text("DIFFERENT_CANDIDATE")});
            require(UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM)==null,"FROZEN_SU2_CANDIDATE_MISMATCH_ACCEPTED");
            db.execSQL("UPDATE research_consumer_choice_r932 SET candidate_id=?",new Object[]{candidate});
            db.execSQL("UPDATE su2_certificate SET expires_at=?",new Object[]{now-1});
            require(UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM)==null,"FROZEN_SU2_EXPIRED_CERTIFICATE_ACCEPTED");
            db.execSQL("UPDATE su2_certificate SET expires_at=?",new Object[]{now+60000});
            System.out.println("R30_FROZEN_SU2_BRIDGE|ACTUAL_EXISTING_CERTIFICATE_AND_PROTOCOL|ORIGINAL_METRICS_AND_UNKNOWN_BRIER|WRONG_CERTIFICATE_OR_CANDIDATE_AND_EXPIRED_REJECTED|READ_ONLY|HOST_SYNTHETIC_NOT_NATURAL|PASS");
            java.lang.reflect.Method collect=DecisionExplanationR30.class.getDeclaredMethod("collect",DbStore.class,String.class,String.class,String.class,String.class,int.class,CertifiedPredictionR974.Proof.class,java.util.Map.class);collect.setAccessible(true);
            DecisionChainR30.Facts observed=(DecisionChainR30.Facts)collect.invoke(null,store,"202610310001",target,"202610310001",HashingSU1.text("SYNTHETIC_CONTEXT"),1,null,new java.util.LinkedHashMap<String,Object>());
            require(DecisionChainR30.explain(observed).allowReal,"ACTUAL_OBSERVER_DID_NOT_MATCH_VALID_EXISTING_AUTHORITY:"+JsonR27.write(DecisionChainR30.explain(observed).items));
            db.execSQL("INSERT INTO v57_shadow_observation_r35 VALUES(?,'BET',1)",new Object[]{target});
            require(!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"EXISTING_CANONICAL_REAL_AUTHORIZED_AGAIN");
            db.execSQL("DELETE FROM v57_shadow_observation_r35");db.execSQL("INSERT INTO trades VALUES(?,'BET')",new Object[]{target});
            require(!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"EXISTING_LEGACY_REAL_AUTHORIZED_AGAIN");
            db.execSQL("DELETE FROM trades");db.execSQL("UPDATE host_state SET value='0'");require(!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"USER_DEFAULT_OFF_BYPASSED");
            db.execSQL("UPDATE host_state SET value='1'");db.execSQL("UPDATE su1_source_clock SET generation=2");require(!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"REVISED_SOURCE_FREEZE_AUTHORIZED");
            db.execSQL("UPDATE su1_source_clock SET generation=1");db.execSQL("UPDATE su2_certificate SET expires_at=?",new Object[]{now-1});require(!RealAuthoritySU2.allows(store,candidate,cert,target,now+60000),"EXPIRED_CERTIFICATE_AUTHORIZED");
            db.execSQL("UPDATE su2_certificate SET expires_at=?",new Object[]{now+60000});
            db.execSQL("INSERT INTO r26_certificate_revocation VALUES(?,'HOST_SYNTHETIC_REVOCATION',?)",new Object[]{cert,now});
            require(current.invoke(null,db,candidate,program,cert)==null,"WRITER_ACCEPTED_REVOKED_SU2_CERTIFICATE");
            require(UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM)==null,"READER_ACCEPTED_REVOKED_SU2_CERTIFICATE");
            System.out.println("R30_REAL_AUTHORITY_SQLITE|ACTUAL_AUTHORITY_BYTECODE|VALID_EXISTING_LANE_PASS|MISSING_FULL_FREEZE_REJECTED|EXISTING_CANONICAL_AND_LEGACY_REAL_REJECTED|DEFAULT_OFF|SOURCE_REVISION_AND_EXPIRED_REJECTED|SYNTHETIC_MECHANICS_NOT_NATURAL_QUALITY|PASS");
        }
    }
}
