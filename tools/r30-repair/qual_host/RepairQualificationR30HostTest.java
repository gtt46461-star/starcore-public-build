package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Four explicit synthetic gap dispositions on actual production bytecode and immutable native SQLite archives. */
public final class RepairQualificationR30HostTest {
    static final long BASE=TargetIssueResolver.sequenceOrdinalSU1("202610300001");
    static SQLiteDatabase db;static Connection connection;
    static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static long n(String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static String text(String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;}}
    static String issue(int period){return TargetIssueResolver.fromSequenceOrdinalSU1(BASE+period-1);}
    static void commit(ResearchWriteQueueR27.Batch batch)throws Exception{connection.setAutoCommit(false);try{ResearchWriteQueueR27.execute(db,batch,"HOST_QUALIFICATION");connection.commit();}catch(Exception|Error failure){connection.rollback();throw failure;}finally{connection.setAutoCommit(true);}}
    static QualificationInboxR29.Commit sink(){return (op,b)->{commit(b);return true;};}
    static void frozen(String id,int period,int hit,int first){db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at) VALUES(?,?,?,1,?,?,?,?,1,'FROZEN',1000,10000,?,12000)",new Object[]{id,BASE+period-1,issue(period),StoreR27.pack(new double[]{.1,.1,.1,.1,.1,.1,.1,.1,.1,.1}),first,first+1,first+2,hit});}
    static String experiment(String name,int index,String status)throws Exception{
        String id=HashingSU1.text(name);PolicySpecR27 spec=NullPipelineSimulationR30.neutralSeed();long at=System.currentTimeMillis();SequentialQualificationR27.State state=new SequentialQualificationR27.State(id,index,.15);state.observe(BASE,true,true);if(status.startsWith("CONTROL"))state.state="CONTROL_NOT_ELIGIBLE";
        db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,1,?,?,?,?,?,?,-1,?,'HOST_SYNTHETIC')",new Object[]{id,spec.fullJson(),new byte[80],index,status,"HOST_SYNTHETIC_NOT_NATURAL",at,at,spec.policyHash()});
        db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,target_rate,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,index,state.alpha,state.logUp,state.logDown,state.n,state.acts,state.hits,state.sumU,state.sumU2,state.lastOrdinal,state.state,.15,at});
        db.execSQL("INSERT INTO r29_qual_calendar VALUES(?,?,?,'REGISTERED_BEFORE_FUTURE')",new Object[]{id,BASE,1});db.execSQL("INSERT INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,?,'MISSING_OPPORTUNITY',1)",new Object[]{id,BASE+1});
        frozen(id,1,1,1);frozen(id,3,1,1);QualificationInboxR29.Row row=new QualificationInboxR29.Row(BASE,issue(1),"FROZEN",1,1,2,3,1,1);
        db.execSQL("INSERT INTO r29_qual_receipt VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{id,BASE,issue(1),1,1,.7,QualificationInboxR29.digest(id,row),JsonR27.write(state.json()),1});return id;
    }
    static String state(String id){return JsonR27.write(StoreR27.readEprocess(db,id).json());}
    static void scanUntil(String id,String expected)throws Exception{for(int i=0;i<20;i++){QualificationGapRepairR30R1.step(db,sink());String result=String.valueOf(QualificationGapRepairR30R1.read(db,id+"|"+(BASE+1)).get("status"));if(result.equals(expected))return;}throw new AssertionError("gap not disposed: "+id+" expected "+expected);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path file=Files.createTempFile("repair-qualification-",".db");
        try{
            connection=DriverManager.getConnection("jdbc:sqlite:"+file);db=new SQLiteDatabase(connection);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");
            // Use the production startup order and full readiness contract, including indexes.
            connection.setAutoCommit(false);
            try{RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);connection.commit();}
            catch(Exception|Error failure){connection.rollback();throw failure;}
            finally{connection.setAutoCommit(true);}
            for(int period=1;period<=4;period++){Object[] values=new Object[12];values[0]=issue(period);values[1]=0;for(int p=0;p<10;p++)values[p+2]=(p+period-1)%10+1;db.execSQL("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",values);}
            String recovered=experiment("HOST_RECOVER_ORIGINAL",1,"ACTIVE");
            connection.setAutoCommit(false);ResearchRetentionStoreR30.ensureProtected(db);connection.commit();connection.setAutoCommit(true);
            for(int i=0;i<70;i++){String id="HOST_DUMMY_ARCHIVE_"+i;frozen(id,2,1,1);db.execSQL("DELETE FROM r27_live_frozen WHERE policy_hash=?",new Object[]{id});}
            frozen(recovered,2,1,1);db.execSQL("DELETE FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new Object[]{recovered,BASE+1});
            QualificationGapRepairR30R1.step(db,sink());Map<String,Object> journal=QualificationGapRepairR30R1.read(db,recovered+"|"+(BASE+1));need("SCANNING_ORIGINAL_ARCHIVE".equals(journal.get("status"))&&((Number)journal.get("archiveCursor")).longValue()==64,"scan bounded at 64 and durably journaled");
            connection.close();connection=DriverManager.getConnection("jdbc:sqlite:"+file);db=new SQLiteDatabase(connection);scanUntil(recovered,"ORIGINAL_RESTORED_REPLAY_PENDING");
            need(n("SELECT count(*) FROM r27_live_frozen WHERE policy_hash='"+recovered+"' AND target_ordinal="+(BASE+1))==1,"exact original restored after connection restart");QualificationInboxR29.consume(db,sink(),"('ACTIVE')",BASE+2);need(StoreR27.readEprocess(db,recovered).n==3,"original contiguous prefix resumes without fabricated skip");need("ORIGINAL_RESTORED_CONTIGUOUS_PREFIX_ADVANCED".equals(QualificationGapRepairR30R1.read(db,recovered+"|"+(BASE+1)).get("status")),"original recovery disposition closes in the same prefix commit");
            frozen(recovered,4,0,1);GateV2R29.Evidence evidence=GateV2R29.evidence(db,recovered,BASE+3);need(evidence.settledActs==3&&evidence.hits==3&&evidence.rawSettledActs==4&&evidence.rawHits==3,"V2 uses exact canonical prefix while raw frozen counts remain separate");
            String absent=experiment("HOST_ABSENT_ORIGINAL",2,"ACTIVE"),before=state(absent);db.execSQL("INSERT INTO r27_certificate VALUES(?,'ACTIVE','{}','HOST_SYNTHETIC',1,9999999999999,'R27_GATE_V1')",new Object[]{absent});scanUntil(absent,"SEALED_NO_AVAILABLE_IMMUTABLE_ORIGINAL");need(before.equals(state(absent)),"sealed segment retains exact original alpha index and accumulated state");need(QualificationGapRepairR30R1.SEALED.equals(text("SELECT status FROM r27_live_shadow WHERE policy_hash=?",absent)),"only affected live segment sealed");need("REVOKED_MISSING_OPPORTUNITY".equals(text("SELECT status FROM r27_certificate WHERE policy_hash=?",absent)),"gap cannot keep active certificate");
            String unavailable=experiment("HOST_UNAVAILABLE_ARCHIVE",3,"CONTROL_I00");db.execSQL("DROP TRIGGER r30_keep_r27_live_frozen_immutable_update");scanUntil(unavailable,"BLOCKED_IMMUTABLE_ARCHIVE_UNAVAILABLE");need("CONTROL_I00".equals(text("SELECT status FROM r27_live_shadow WHERE policy_hash=?",unavailable)),"unknown archive availability is not claimed absent");db.execSQL("CREATE TRIGGER r30_keep_r27_live_frozen_immutable_update BEFORE UPDATE ON r30_keep_r27_live_frozen BEGIN SELECT RAISE(ABORT,'R30_PROTECTED_ARCHIVE_IMMUTABLE'); END");
            String conflict=experiment("HOST_CONFLICTING_ORIGINALS",4,"CONTROL_NULL"),prior=state(conflict);frozen(conflict,2,1,1);db.execSQL("DELETE FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new Object[]{conflict,BASE+1});frozen(conflict,2,0,4);db.execSQL("DELETE FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new Object[]{conflict,BASE+1});scanUntil(conflict,"BLOCKED_ORIGINAL_CONFLICT");need(prior.equals(state(conflict)),"conflicting originals never choose favorable data or reset spending");
            need(n("SELECT max(global_index) FROM r27_eprocess")==4,"all four permanent budget indices retained");need(n("SELECT count(*) FROM r27_live_frozen WHERE status='SKIPPED_GAP'")==0,"no post-result SKIP manufactured");
            System.out.println("REPAIR_QUALIFICATION_NATIVE_SQLITE_HOST_PASS four_explicit_dispositions=1 immutable_original_restored=1 bounded64_scan_connection_restart=1 canonical_prefix_replay=1 missing_segment_sealed_no_alpha_reset=1 unavailable_archive_not_claimed_absent=1 conflicting_originals_no_cherry_pick=1 v2_canonical_and_raw_denominators_separate=1 no_fabricated_SKIP=1 HOST_SYNTHETIC_NOT_PHONE_OR_NATURAL");
        }finally{if(connection!=null)connection.close();Files.deleteIfExists(file);}
    }
}
