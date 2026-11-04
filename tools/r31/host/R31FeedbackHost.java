package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.Method;

/** Actual input consumer and typed numeric consumer. Numerical model-kernel quality is outside this fixture. */
public final class R31FeedbackHost {
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    private static void need(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void verify(DbStore store,SQLiteDatabase db,DatabaseCoordinator coordinator)throws Exception{
        String owner="R31_HOST_FEEDBACK_OWNER";long mono=android.os.SystemClock.elapsedRealtime();String boot=IssueTimelineWriterR30.bootId();
        db.execSQL("INSERT INTO r26_session VALUES(?,1,?,'RUNNING',?,0)",new Object[]{owner,boot,System.currentTimeMillis()});
        db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,owner,fence,status) VALUES('feedback','P1','feedback',?,1,'RUNNING')",new Object[]{owner});
        db.execSQL("INSERT INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms) VALUES('feedback',?,1,?,?)",new Object[]{owner,boot,mono+120000});
        RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("feedback",owner,1,0);
        BudgetTokenSU1 budget=new BudgetTokenSU1(120000,Long.MAX_VALUE/4,()->false);
        RuntimeSchemaR26.reconcileFeedbackR31(store,claim,budget);
        String[] next=LegacyReplayR26.nextInput(store,TargetIssueResolver.sequenceOrdinalSU1("202611040100")-1);
        need(next!=null&&!next[0].isEmpty()&&"202611040100".equals(next[1]),"actual legacy input consumer receives draw label without canonical prediction or terminal");
        need(scalar(db,"SELECT count(*) FROM r31_feedback_event WHERE kind='DRAW_LABEL_REVEALED'")==2,"typed draw events from actual producer");
        need(scalar(db,"SELECT count(*) FROM r31_feedback_event WHERE kind='FROZEN_RESULT'")==1,"missing prediction cannot generate frozen result");
        need(scalar(db,"SELECT events FROM r31_frozen_feedback_effect")==1,"actual frozen numeric consumer effect");
        for(int i=0;i<100;i++)RuntimeSchemaR26.reconcileFeedbackR31(store,claim,budget);
        need(scalar(db,"SELECT events FROM r31_frozen_feedback_effect")==1,"100 repeated deliveries do not repeat numeric effect");
        need(scalar(db,"SELECT calibrated_events FROM r31_frozen_feedback_effect")==1,"calibration support is separate from total result count");
        String legacy=next[0],source=next[2],checkpoint=StatsR27.sha256("HOST_ENGINEERING_CHECKPOINT"),artifact=StatsR27.sha256("HOST_ENGINEERING_ARTIFACT");long ordinal=Long.parseLong(next[3]);
        db.execSQL("CREATE TABLE host_learner(id INTEGER PRIMARY KEY,parameter INTEGER NOT NULL,cursor INTEGER NOT NULL)");db.execSQL("INSERT INTO host_learner VALUES(1,0,0)");
        try{
            coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_HOST_MODEL_RECEIPT_ROLLBACK"),tx->{
                tx.execSQL("UPDATE host_learner SET parameter=1,cursor=? WHERE id=1",new Object[]{ordinal});
                tx.execSQL("INSERT INTO r26_checkpoint_artifact VALUES(?,?,?,?,?,?,?)",new Object[]{checkpoint,artifact,"HOST_POLICY",source,"",ordinal,System.currentTimeMillis()});
                tx.execSQL("INSERT INTO r26_consumption VALUES(?,?,?,'REVEALED_LIVE_LABEL')",new Object[]{legacy,"HOST_POLICY",checkpoint});throw new IllegalStateException("HOST_INJECT_AFTER_REAL_RECEIPT_TRIGGER");
            });throw new AssertionError("rollback injection absent");
        }catch(DatabaseCoordinator.WriteRejectedR31 expected){}
        need(scalar(db,"SELECT parameter FROM host_learner")==0&&scalar(db,"SELECT count(*) FROM r31_feedback_receipt WHERE policy_hash='HOST_POLICY'")==0,"parameter/cursor/checkpoint/typed receipt all roll back atomically");
        for(int i=0;i<100;i++)coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_HOST_MODEL_RECEIPT_DUPLICATE"),tx->{
            try(Cursor old=tx.rawQuery("SELECT 1 FROM r26_consumption WHERE event_id=? AND policy_hash='HOST_POLICY'",new String[]{legacy})){if(old.moveToFirst())return 0;}
            tx.execSQL("UPDATE host_learner SET parameter=parameter+1,cursor=? WHERE id=1",new Object[]{ordinal});
            tx.execSQL("INSERT INTO r26_checkpoint_artifact VALUES(?,?,?,?,?,?,?)",new Object[]{checkpoint,artifact,"HOST_POLICY",source,"",ordinal,System.currentTimeMillis()});
            tx.execSQL("INSERT INTO r26_consumption VALUES(?,?,?,'REVEALED_LIVE_LABEL')",new Object[]{legacy,"HOST_POLICY",checkpoint});return 1;
        });
        need(scalar(db,"SELECT parameter FROM host_learner")==1&&scalar(db,"SELECT cursor FROM host_learner")==ordinal&&scalar(db,"SELECT count(*) FROM r31_feedback_receipt WHERE policy_hash='HOST_POLICY'")==1,"same event/version fixture effect and real trigger receipt exactly once");
        for(String sql:new String[]{"UPDATE r31_feedback_event SET payload='changed'","DELETE FROM r31_feedback_event","INSERT OR REPLACE INTO r31_feedback_event SELECT * FROM r31_feedback_event LIMIT 1"}){
            try{db.execSQL(sql);throw new AssertionError("immutable event mutation accepted: "+sql);}catch(android.database.sqlite.SQLiteException expected){}
        }
        System.out.println("R31_FEEDBACK_NATIVE_HOST_PASS actual_legacy_next_input_without_freeze=true actual_frozen_residual_consumer_once_for_100_replays=true checkpoint_receipt_atomic_rollback=true fixture_parameter_once_for_100_replays=true event_immutable=true scope=HOST_ENGINEERING_FIXTURE_NOT_MODEL_KERNEL_EFFECTIVENESS_OR_NATURAL_QUALITY");
    }
}
