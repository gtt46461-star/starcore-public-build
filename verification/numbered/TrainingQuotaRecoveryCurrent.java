package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchTrainingHookRegistryR952;
import java.io.File;
import java.lang.reflect.Field;

/** Actual quota SQL rejection → atomic rollback → typed WAIT_INPUT → natural cooldown/restart. */
public final class TrainingQuotaRecoveryCurrent {
    private static int checks;
    private static void require(boolean condition,String detail){checks++;if(!condition)throw new AssertionError(detail);}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"scalar row");return c.getLong(0);}}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);long availableAt=0;
        try {
            db.execSQL("PRAGMA foreign_keys=ON");DbStore store=HostProductR974.store(db);
            Field input=DbStore.class.getDeclaredField("researchInputRootCP11");input.setAccessible(true);input.set(store,new File(args[2]));
            DatabaseCoordinator owner=store.coordinatorR36();
            require(owner.runAtomicWrite("NUMBERED_MODEL_QUOTA_BOUNDARY",x->{
                x.execSQL("INSERT INTO research_durable_model_identity_cp12 VALUES('EXPLICIT_COUNTER_BOUNDARY',67108864,0)");
                x.execSQL("INSERT INTO research_durable_model_ref_cp12 VALUES('COUNTER_FIXTURE','boundary','EXPLICIT_COUNTER_BOUNDARY',67108864)");
            }),"quota boundary prerequisite");
            boolean held=false;String cursor="";long generation=0,pins=0;int pulses;
            for(pulses=0;pulses<200;pulses++) {
                ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(store,System.nanoTime()+10_000_000_000L);
                try(Cursor c=db.rawQuery("SELECT status,last_error,cursor,generation,available_at FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE'",null)) {
                    require(c.moveToFirst(),"durable train task");
                    require(!"FAILED_TERMINAL".equals(c.getString(0)),"capacity falsely quarantined candidate");
                    if("WAIT_INPUT".equals(c.getString(0))){require("MODEL_STORAGE_CAPACITY".equals(c.getString(1)),"typed quota reason");cursor=c.getString(2);generation=c.getLong(3);availableAt=c.getLong(4);held=true;break;}
                }
                Thread.sleep(260);
            }
            require(held,"quota did not reach typed WAIT_INPUT");
            require(cursor.startsWith("FEATURES|849|"),"exact feature prefix lost");
            pins=scalar(db,"SELECT count(*) FROM v57_research_input_pin_r967");
            require(scalar(db,"SELECT count(*) FROM research_training_feature_row_cp11")==849,"prepared feature rows lost");
            for(String table:new String[]{"r952_artifact","research_artifact_identity_v2","research_development_receipt_v2","r952_learning_cursor"})
                require(scalar(db,"SELECT count(*) FROM "+table)==0,"partial final publication: "+table);
            require(scalar(db,"SELECT count(*) FROM r952_candidate WHERE future_n=0 AND may_create_real=0")==1,"capacity changed qualification");
            require(owner.runAtomicWrite("NUMBERED_RELEASE_SYNTHETIC_QUOTA_BOUNDARY",x->x.execSQL("DELETE FROM research_durable_model_ref_cp12 WHERE source_table='COUNTER_FIXTURE' AND source_key='boundary'")),"release synthetic quota boundary");
            require(scalar(db,"SELECT count(*) FROM research_durable_model_identity_cp12 WHERE model_key='EXPLICIT_COUNTER_BOUNDARY'")==0,"automatic zero-ref cleanup failed");
            try(Cursor c=db.rawQuery("SELECT cursor,generation FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE'",null)) {
                require(c.moveToFirst()&&cursor.equals(c.getString(0))&&generation==c.getLong(1),"capacity release rewrote task cursor/generation");
            }
            require(scalar(db,"SELECT count(*) FROM v57_research_input_pin_r967")==pins,"capacity release dropped input pins");
            System.out.println("QUOTA_HOLD_VERIFIED|checks="+checks+"|pulses="+(pulses+1)+"|typedWait=MODEL_STORAGE_CAPACITY|naturalCooldownMs="+Math.max(0,availableAt-System.currentTimeMillis()));
        } finally {db.shutdown();}
        while(System.currentTimeMillis()<=availableAt)Thread.sleep(Math.min(1000,Math.max(1,availableAt-System.currentTimeMillis()+1)));
        // Recreate store/connection through the existing consumer; no task timestamp/status edits.
        TrainingAnchorConsumerCurrent.main(args);
        System.out.println("PASS|quota rollback, exact durable cursor/pins, real60s cooldown, recreated unique writer, production model/anchor/receipt completion; synthetic host scope");
    }
}
