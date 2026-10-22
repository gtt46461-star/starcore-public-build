package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.*;
import android.database.Cursor;

/** A changed executable kernel retires active versions; old evidence and lifetime alpha remain. */
public final class KernelTransitionSU1 {
    private KernelTransitionSU1() {}
    public static void ensure(DbStore store,long sequence){
        String kernel=TrainingCodeIdentityR964.SHA256;
        try(Cursor c=store.readable().rawQuery("SELECT kernel_hash FROM su1_kernel_state WHERE singleton=1",null)){if(c.moveToFirst()&&kernel.equals(c.getString(0)))return;}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_KERNEL_TRANSITION",db->{
            db.execSQL("INSERT INTO su1_sealed_cancelled SELECT p.campaign_id,'EXECUTION_KERNEL_CHANGED',? FROM su1_sealed_plan p JOIN su1_runtime_program k ON k.strategy_id=p.strategy_id WHERE k.kernel_hash<>? AND NOT EXISTS(SELECT 1 FROM su1_sealed_cancelled x WHERE x.campaign_id=p.campaign_id) AND NOT EXISTS(SELECT 1 FROM su1_sealed_result x WHERE x.plan_campaign=p.campaign_id)",new Object[]{System.currentTimeMillis(),kernel});
            db.execSQL("UPDATE su1_research_pool SET state='RETIRED',retired_sequence=max(born_sequence,?),retire_reason='EXECUTION_KERNEL_CHANGED' WHERE state='ACTIVE' AND strategy_id IN(SELECT strategy_id FROM su1_runtime_program WHERE kernel_hash<>?)",new Object[]{sequence,kernel});
            db.execSQL("UPDATE su1_cohort_runtime SET state='STOPPED',stop_reason='EXECUTION_KERNEL_CHANGED_NO_ALPHA_REFUND' WHERE state='OPEN' AND cohort_id IN(SELECT c.cohort_id FROM su1_eval_cohort c JOIN su1_runtime_program p ON p.strategy_id=c.strategy_id WHERE p.kernel_hash<>?)",new Object[]{kernel});
            if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_kernel_state WHERE singleton=1"))db.execSQL("INSERT INTO su1_kernel_state VALUES(1,?,?)",new Object[]{kernel,System.currentTimeMillis()});else db.execSQL("UPDATE su1_kernel_state SET kernel_hash=?,updated_at=? WHERE singleton=1",new Object[]{kernel,System.currentTimeMillis()});return 0;
        });
    }
}
