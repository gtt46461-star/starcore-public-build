package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.File;
import java.lang.reflect.Field;

/** Real production training subslot consumes the cold Creator's durable protocol and CAS input. */
public final class TrainingAnchorConsumerCurrent {
    private static int assertions;
    private static void require(boolean condition,String detail){assertions++;if(!condition)throw new AssertionError(detail);}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"scalar row");return c.getLong(0);}}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            db.execSQL("PRAGMA foreign_keys=ON");
            DbStore store=HostProductR974.store(db);
            Field input=DbStore.class.getDeclaredField("researchInputRootCP11");input.setAccessible(true);input.set(store,new File(args[2]));
            require(scalar(db,"SELECT count(*) FROM r952_candidate")==1,"cold Creator child required");
            require(scalar(db,"SELECT count(*) FROM r952_artifact")==0,"no preloaded model");
            require(scalar(db,"SELECT count(*) FROM research_artifact_identity_v2")==0,"no preloaded anchor");
            int pulses;
            for(pulses=0;pulses<200;pulses++) {
                ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(store,System.nanoTime()+10_000_000_000L);
                if(scalar(db,"SELECT count(*) FROM research_development_receipt_v2")==1)break;
                require(scalar(db,"SELECT count(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='FAILED_TERMINAL'")==0,"training failed terminally");
                Thread.sleep(260); // Real durable cooldown; never edit cursor/status/available_at to force progress.
            }
            require(pulses<200,"production training did not complete");
            require(scalar(db,"SELECT count(*) FROM r952_artifact")==1,"final artifact absent");
            require(scalar(db,"SELECT count(*) FROM research_artifact_identity_v2")==1,"Director failed to register anchor");
            require(scalar(db,"SELECT count(*) FROM research_development_receipt_v2 d JOIN research_artifact_identity_v2 a ON a.artifact_hash=d.artifact_hash JOIN r952_artifact r ON r.artifact_hash=a.artifact_hash WHERE a.candidate_id=r.candidate_id AND a.payload_hash=r.payload_hash AND a.development_receipt_hash=d.receipt_hash AND a.validation_hash=d.validation_hash")==1,"artifact/anchor/receipt identity mismatch");
            long trainN;
            try(Cursor c=db.rawQuery("SELECT model_payload,payload_hash,numeric_updates,development_sample_n,validation_sample_n FROM r952_artifact",null)) {
                require(c.moveToFirst(),"model row");CandidateCompilerR952.Model model=CandidateCompilerR952.Model.parse(c.getString(0));
                trainN=c.getLong(2);require(trainN>=300 && trainN<=1200 && trainN==c.getLong(3),"training count outside contract");
                require(model.updates==trainN && model.hash().equals(c.getString(1)),"computed model/hash not persisted");
                require(c.getLong(4)==120,"validation count");
            }
            require(scalar(db,"SELECT count(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='COMPLETED'")==1,"task completion absent");
            require(scalar(db,"SELECT count(*) FROM v57_research_task_r967 WHERE kind='FREEZE_CANDIDATE'")==1,"downstream freeze task absent");
            require(scalar(db,"SELECT count(*) FROM r952_learning_cursor c JOIN r952_artifact a ON c.artifact_hash=a.artifact_hash WHERE c.trained_until_issue=a.target_issue AND c.last_delivery_hash=a.delivery_hash")==1,"learning cursor does not consume artifact");
            require(scalar(db,"SELECT count(*) FROM r952_candidate WHERE future_n=0 AND may_create_real=0 AND state='VALIDATED'")==1,"development inherited Future or REAL eligibility");
            require(scalar(db,"SELECT count(*) FROM research_durable_model_ref_cp12 WHERE source_table='r952_artifact'")==1,"quota source ref absent");
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){require(!c.moveToFirst(),"foreign key violation");}
            System.out.println("PASS|assertions="+assertions+"|pulses="+(pulses+1)+"|numericUpdates="+trainN+"|validationN=120|childFutureN=0|actual production import/Creator/train/Director anchor+receipt+cursor+complete+freeze enqueue; synthetic host history, no qualification/device/natural Future proof");
        } finally {db.shutdown();}
    }
}
