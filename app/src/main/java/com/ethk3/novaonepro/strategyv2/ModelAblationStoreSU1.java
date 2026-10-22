package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** The common model cohort and frozen optimizer states are durable; historical controls cannot enter REAL. */
public final class ModelAblationStoreSU1 {
    private ModelAblationStoreSU1(){}
    public static void enqueue(SQLiteDatabase db,CampaignStateSU1 source,long now){
        if(source.foundry.valEnd<1600||StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_model_ablation WHERE source_campaign=?",source.campaign))return;
        StrategyRepositorySU1.row(db,"su1_model_ablation","source_campaign",source.campaign,"dataset_blob",source.datasetBlob,"kernel_hash",TrainingCodeIdentityR964.SHA256,"generation",0,"phase","TRAIN","updated_at",now);
    }
    public static boolean advanceOne(DbStore store,BudgetTokenSU1 budget){
        if(!ArchiveStoreSU1.canCreate(store.readable()))return false;String[] row=new String[6];
        try(Cursor c=store.readable().rawQuery("SELECT source_campaign,dataset_blob,generation,phase,checkpoint_hash,frozen_hash FROM su1_model_ablation WHERE kernel_hash=? AND phase IN ('TRAIN','LOCKED') ORDER BY updated_at,source_campaign LIMIT 1",new String[]{TrainingCodeIdentityR964.SHA256})){
            if(!c.moveToFirst())return false;for(int i=0;i<row.length;i++)row[i]=c.isNull(i)?null:c.getString(i);
        }
        budget.checkpointOrYield();int[][] raw=CampaignStateSU1.unpack(CampaignStateSU1.localBlob(store.readable(),row[1]));
        ModelAblationSessionSU1 session=row[4]==null?new ModelAblationSessionSU1(raw,row[1],TrainingCodeIdentityR964.SHA256,11):ModelAblationSessionSU1.restore(raw,row[1],TrainingCodeIdentityR964.SHA256,CampaignStateSU1.localBlob(store.readable(),row[4]));
        if((!session.phase.equals(row[3])||!java.util.Objects.equals(session.frozenHash,row[5])))throw new IllegalStateException("SU1_MODEL_AUDIT_PHASE_BINDING");
        try{session.advance(budget);}catch(BudgetTokenSU1.Yield partial){/* The complete Adam state and exact paired replay cursor persist below. */}
        String checkpoint=session.checkpoint(),report=session.phase.equals("DONE")?session.report():null;long now=System.currentTimeMillis();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_MODEL_ABLATION_CHECKPOINT",db->{
            if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_model_ablation WHERE source_campaign=? AND generation=? AND phase=?",row[0],row[2],row[3]))throw new IllegalStateException("SU1_MODEL_AUDIT_GENERATION_CAS");
            String saved=StrategyRepositorySU1.blob(db,checkpoint,"SU1_MODEL_ABLATION_CHECKPOINT_V1",now),output=report==null?null:StrategyRepositorySU1.blob(db,report,"SU1_MODEL_ABLATION_REPORT_V1",now);
            db.execSQL("UPDATE su1_model_ablation SET generation=generation+1,phase=?,checkpoint_hash=?,frozen_hash=?,report_hash=?,updated_at=? WHERE source_campaign=? AND generation=? AND phase=?",new Object[]{session.phase,saved,session.frozenHash,output,now,row[0],Long.parseLong(row[2]),row[3]});
            if(StrategyRepositorySU1.count(db,"SELECT changes()")!=1)throw new IllegalStateException("SU1_MODEL_AUDIT_CHECKPOINT_CAS");return 0;
        });
        return true;
    }
}
