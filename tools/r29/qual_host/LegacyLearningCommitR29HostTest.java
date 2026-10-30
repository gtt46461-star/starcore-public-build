package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.sql.DriverManager;
import java.io.IOException;

/** Actual production transaction/hooks/event ledger on two independent SQLite writers. Synthetic protocol only. */
public final class LegacyLearningCommitR29HostTest {
    static int checks;static final String ISSUE="202610300001",SOURCE="ResearchAdaptiveLearnerStoreR811";static final long FROZEN=100,OPEN=200;
    static void check(String name,boolean ok){System.out.println("R29_LEGACY_LEARNING|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    interface Work{void run()throws Exception;}static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Throwable e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String hash(String s){return V51ChecksumEngine.sha256(s);}
    static String inputHash(int label){return hash("R811_FTRL_INPUT|"+ISSUE+"|100|200|"+label+"|0.1|0.2|0.3|0.4|0.5|0.6|0.7");}
    static String identity(int label){return Batch2ContractCoreR926.learningKey(V51Repository.GAME_CODE,ISSUE,AdaptiveShadowLearnerEngine.UPDATE_RULE,"STRICT_OOS_SETTLED_PREDRAW_V1",inputHash(label));}
    static void rootInput(SQLiteDatabase rt){
        rt.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue TEXT PRIMARY KEY,prediction_created_at INTEGER,draw_open_time INTEGER,hit_label INTEGER,selected_hit_p REAL,strict_oos INTEGER)");
        rt.execSQL("INSERT INTO "+DbHelper.TABLE_R35_OOS_LABEL+" VALUES(?,100,200,1,0.7,1)",new Object[]{ISSUE});
        rt.execSQL("CREATE TABLE "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+"(target_issue TEXT PRIMARY KEY,raw_action_p_value REAL,rank_score_value REAL,third_margin_value REAL,entropy_risk_value REAL,stability_value REAL,window_consensus_value REAL,raw_action_p_status TEXT,rank_status TEXT,third_margin_status TEXT,entropy_risk_status TEXT,stability_status TEXT,window_consensus_status TEXT)");
        rt.execSQL("INSERT INTO "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" VALUES(?,0.1,0.2,0.3,0.4,0.5,0.6,'AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE','AVAILABLE')",new Object[]{ISSUE});
        rt.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT)");rt.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED')",new Object[]{ISSUE});
        rt.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER,settled INTEGER)");rt.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" VALUES(?,1,1)",new Object[]{ISSUE});
    }
    static LegacyLearningCommitR29.Pending pending(){LegacyLearningCommitR29.Pending p=new LegacyLearningCommitR29.Pending();p.id=identity(1);p.issue=ISSUE;p.source=SOURCE;p.before=hash("before");p.after=hash("after");p.trained=ISSUE;p.detail="input_hash="+inputHash(1)+"|updates=1";return p;}
    static DatabaseCoordinator.TransactionResult<Boolean> learn(SQLiteDatabase rp,String key,boolean fail){DatabaseCoordinator.TransactionContext context=new DatabaseCoordinator.TransactionContext("R811_FTRL_ARTIFACT_B09_ATOMIC",ISSUE,key);return DatabaseCoordinator.runExistingTransaction(rp,context,(db,ctx)->{
        db.execSQL("INSERT INTO v57_research_artifact_r811(artifact_hash,trained_until_issue,artifact_blob,artifact_type) VALUES(?,?,X'010203','FTRL_SHADOW')",new Object[]{hash("after"),ISSUE});LegacyLearningCommitR29.Pending p=pending();RuntimeEventHooksR926.learnedWithinTransaction(db,p.issue,p.source,p.id,p.before,p.after,p.trained,p.detail);if(fail)throw new IOException("SYNTHETIC_LEARN_FAILURE_AFTER_FACT");return true;
    },false);}
    public static void main(String[]args)throws Exception{
        try(SQLiteDatabase rt=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));SQLiteDatabase rp=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))){
            rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','schema','root',42,1)");LegacyLearningCommitR29.installResearch(rp);rp.execSQL("CREATE TABLE v57_research_artifact_r811(artifact_hash TEXT PRIMARY KEY,trained_until_issue TEXT,artifact_blob BLOB,artifact_type TEXT)");rp.execSQL("CREATE TABLE outer_survives(id INTEGER PRIMARY KEY)");
            final DatabaseCoordinator.TransactionResult<?>[] failed={null};tx(rp,()->{failed[0]=learn(rp,hash("FAIL_TX"),true);rp.execSQL("INSERT INTO outer_survives VALUES(1)");});
            System.out.println("R29_LEGACY_LEARNING|NESTED_FAILURE_DIAGNOSTIC|cause="+failed[0].failureCause+"|artifact="+n(rp,"SELECT count(*) FROM v57_research_artifact_r811")+"|fact="+n(rp,"SELECT count(*) FROM r29_legacy_local_learned")+"|outbox="+n(rp,"SELECT count(*) FROM r29_legacy_learned_outbox")+"|tx="+n(rp,"SELECT count(*) FROM r29_research_tx_receipt"));
            check("L1_NESTED_FAILURE_ROLLS_BACK_ARTIFACT_FACT_OUTBOX_AND_RECEIPT",!failed[0].success&&failed[0].failureCause instanceof IOException&&n(rp,"SELECT count(*) FROM v57_research_artifact_r811")==0&&n(rp,"SELECT count(*) FROM r29_legacy_local_learned")==0&&n(rp,"SELECT count(*) FROM r29_legacy_learned_outbox")==0&&n(rp,"SELECT count(*) FROM r29_research_tx_receipt")==0);
            check("L2_NESTED_SAVEPOINT_FAILURE_DOES_NOT_CANCEL_OUTER_TRANSACTION",n(rp,"SELECT count(*) FROM outer_survives")==1);
            rootInput(rt);RuntimeEventLedgerR926.ensureSchema(rt);rt.jdbc.setAutoCommit(false);rt.execSQL("UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=hit_label");DatabaseCoordinator.TransactionResult<Boolean> committed;
            try{committed=learn(rp,identity(1),false);}finally{rt.jdbc.rollback();rt.jdbc.setAutoCommit(true);}
            check("L3_ROOT_WRITER_HELD_RESEARCH_LEARNING_STILL_COMMITS",committed.success&&!committed.replay&&n(rp,"SELECT count(*) FROM v57_research_artifact_r811")==1&&n(rp,"SELECT count(*) FROM r29_legacy_local_learned")==1&&n(rp,"SELECT count(*) FROM r29_research_tx_receipt")==1);
            check("L4_RESEARCH_COMMIT_CANNOT_IMPLICITLY_CREATE_ROOT_EVENT",n(rt,"SELECT count(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT)==0&&n(rp,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN('"+RuntimeEventLedgerR926.TABLE_EVENT+"','"+DbHelper.TABLE_R35_OOS_LABEL+"','"+DbHelper.TABLE_R36_SETTLEMENT_TASK+"')")==0);
            check("L5_UNACKNOWLEDGED_MODEL_IS_NOT_LOADABLE_OR_A_NEXT_PARENT",n(rp,"SELECT count(*) FROM v57_research_artifact_r811 WHERE "+LegacyLearningCommitR29.artifactVisibilitySql(rp,"v57_research_artifact_r811"))==0&&LegacyLearningCommitR29.hasUnacknowledgedFtrl(rp));
            DatabaseCoordinator.TransactionResult<Boolean> duplicate=learn(rp,identity(1),false);check("L6_DUPLICATE_TRANSACTION_CANNOT_TRAIN_OR_APPEND_TWICE",duplicate.success&&duplicate.replay&&n(rp,"SELECT count(*) FROM r29_legacy_local_learned")==1&&n(rp,"SELECT count(*) FROM r29_legacy_learned_outbox")==1);
            LegacyLearningCommitR29.Pending p=pending();rt.execSQL("UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=0");boolean rejected=false;try{tx(rt,()->LegacyLearningCommitR29.deliverWithin(rt,p));}catch(IllegalStateException e){rejected=e.getMessage().contains("SOURCE_INPUT_CONFLICT");}
            check("L7_CHANGED_ROOT_INPUT_CANNOT_BECOME_LEARNED_OR_ACKNOWLEDGED",rejected&&n(rt,"SELECT count(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT)==0&&n(rp,"SELECT root_acked_at FROM r29_legacy_local_learned")==0&&n(rp,"SELECT count(*) FROM v57_research_artifact_r811")==1);
            rt.execSQL("UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=1");tx(rt,()->LegacyLearningCommitR29.deliverWithin(rt,p));tx(rt,()->LegacyLearningCommitR29.deliverWithin(rt,p));
            check("L8_ORIGINAL_ROOT_OWNER_COMMITS_ONE_VERIFIED_LEARNED_EVENT",n(rt,"SELECT count(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT+" WHERE event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE'")==1&&n(rt,"SELECT learned_seen FROM "+RuntimeEventLedgerR926.TABLE_PROJECTION)==1);
            check("L9_LOST_ACK_REMAINS_NONVISIBLE_UNTIL_RESEARCH_ACK_COMMIT",n(rp,"SELECT count(*) FROM v57_research_artifact_r811 WHERE "+LegacyLearningCommitR29.artifactVisibilitySql(rp,"v57_research_artifact_r811"))==0);
            tx(rp,()->{LegacyLearnedInputR29.acknowledgeLocalWithin(rp,p.id,System.currentTimeMillis());rp.execSQL("UPDATE r29_legacy_learned_outbox SET acked_at=1,delivery_status='ACKNOWLEDGED'");});
            check("L10_ROOT_ACK_EXPOSES_EXACT_PREVIOUSLY_COMMITTED_MODEL",n(rp,"SELECT count(*) FROM v57_research_artifact_r811 WHERE "+LegacyLearningCommitR29.artifactVisibilitySql(rp,"v57_research_artifact_r811"))==1&&!LegacyLearningCommitR29.hasUnacknowledgedFtrl(rp));
            p.after=hash("conflicting-state");rejected=false;try{tx(rt,()->LegacyLearningCommitR29.deliverWithin(rt,p));}catch(IllegalStateException e){rejected=e.getMessage().contains("ROOT_RECEIPT_CONFLICT");}check("L11_DUPLICATE_KEY_WITH_DIFFERENT_STATE_IS_NOT_FALSELY_ACKNOWLEDGED",rejected&&n(rt,"SELECT count(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT)==1);
            boolean identityConflict=false;try{tx(rp,()->{DatabaseCoordinator.TransactionContext changed=new DatabaseCoordinator.TransactionContext("WRONG_OPERATION",ISSUE,identity(1));DatabaseCoordinator.TransactionResult<Boolean> r=DatabaseCoordinator.runExistingTransaction(rp,changed,(db,ctx)->true,false);if(r.success)throw new AssertionError("CONFLICT_ACCEPTED");});identityConflict=true;}catch(Exception ignored){}
            check("L12_TRANSACTION_KEY_CANNOT_ALIAS_DIFFERENT_OPERATION",identityConflict&&n(rp,"SELECT count(*) FROM r29_research_tx_receipt")==1);
            java.util.Set<String> forbidden=new java.util.HashSet<>(java.util.Arrays.asList(SchemaDomainCoordinatorR926.criticalContract().objects));forbidden.add(RuntimeEventLedgerR926.TABLE_EVENT);int wrong=0;try(Cursor c=rp.rawQuery("SELECT name FROM sqlite_master WHERE type='table'",null)){while(c.moveToNext())if(forbidden.contains(c.getString(0)))wrong++;}
            check("L13_RESEARCH_HAS_ZERO_ORIGINAL_CRITICAL_AUTHORITY_TABLES",wrong==0);

        }
        System.out.println("R29_LEGACY_LEARNING|ALL_PASS|checks="+checks+"|HOST_PROTOCOL_NOT_ANDROID_LIFECYCLE_OR_NATURAL_QUALITY");
    }
}
