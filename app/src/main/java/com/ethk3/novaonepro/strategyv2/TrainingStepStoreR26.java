package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Fenced caller commits diagnostics only with its completed candidate, live or expert-pair checkpoint.
 * Intermediate state digests are commitments; only batch_checkpoint names a durable file manifest. */
public final class TrainingStepStoreR26 {
    public static final String SCOPE="FIXED_INPUT_KERNEL_UPDATE_DIAGNOSTIC_NOT_QUALIFICATION";
    public static final int DETAIL_LIMIT=8192;
    private TrainingStepStoreR26(){}
    public static final class Pending {
        final String policy,campaign,source,sourceKind,issue,member,before,after,optimizer;
        final long ordinal;final boolean gap;final TrainingDiagnosticsR26.Step step;
        Pending(String policy,String campaign,String source,String sourceKind,String issue,String member,String before,String after,String optimizer,long ordinal,boolean gap,TrainingDiagnosticsR26.Step step){this.policy=policy;this.campaign=campaign;this.source=source;this.sourceKind=sourceKind;this.issue=issue;this.member=member;this.before=before;this.after=after;this.optimizer=optimizer;this.ordinal=ordinal;this.gap=gap;this.step=step;}
        String digest(String checkpoint){return PolicyIdentityR26.sha256(PolicyIdentityR26.canonical(Arrays.asList("R26_TRAINING_STEP_V1",policy,campaign,ordinal,issue,member,source,sourceKind,step.update,step.lossBefore,step.lossAfter,step.gradientNorm,step.parameterDeltaNorm,step.inputHash,step.objective,before,after,gap,optimizer,SCOPE,checkpoint)));}
    }
    public static boolean supported(ModelLiveSU2.Hub hub){for(OnlineModelsSU2.Model model:hub.models.values())if(model instanceof TrainingDiagnosticsR26)return true;return false;}
    public static Map<String,Long> counters(ModelLiveSU2.Hub hub){Map<String,Long> before=new HashMap<>();for(Map.Entry<String,OnlineModelsSU2.Model> entry:hub.models.entrySet())if(entry.getValue() instanceof TrainingDiagnosticsR26)before.put(entry.getKey(),((TrainingDiagnosticsR26)entry.getValue()).completedTrainingUpdates());return before;}
    public static List<Pending> capture(String policy,ModelLiveSU2.Hub hub,String campaign,String source,String sourceKind,long ordinal,long issue,boolean gap,String prior,String current,Map<String,Long> before){
        List<Pending> result=new ArrayList<>();
        for(Map.Entry<String,OnlineModelsSU2.Model> entry:hub.models.entrySet()){if(!(entry.getValue() instanceof TrainingDiagnosticsR26))continue;TrainingDiagnosticsR26 model=(TrainingDiagnosticsR26)entry.getValue();Long previous=before.get(entry.getKey());long after=model.completedTrainingUpdates();if(previous!=null&&after==previous)continue;
            TrainingDiagnosticsR26.Step step=model.lastTrainingStep();if(previous==null||after!=previous+1||step==null||step.update!=after)throw new IllegalStateException("R26_COMPLETED_UPDATE_DIAGNOSTICS_MISSING");result.add(new Pending(policy,campaign,source,sourceKind,Long.toString(issue),entry.getKey(),prior,current,model.optimizerProtocol(),ordinal,gap,step));
        }return result;
    }
    public static void record(SQLiteDatabase db,List<Pending> pending,String checkpoint){
        if(!db.inTransaction())throw new IllegalStateException("R26_TRAINING_RECEIPT_WRITER");long now=System.currentTimeMillis();
        for(Pending p:pending){TrainingDiagnosticsR26.Step s=p.step;db.execSQL("INSERT INTO r26_training_step(policy_hash,campaign_id,sample_ordinal,sample_issue,member_key,source_hash,source_kind,kernel_update,loss_before,loss_after,gradient_norm,parameter_delta_norm,input_hash,objective,before_state_hash,after_state_hash,gap_reset,optimizer,scope,batch_checkpoint,receipt_hash,recorded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{p.policy,p.campaign,p.ordinal,p.issue,p.member,p.source,p.sourceKind,s.update,s.lossBefore,s.lossAfter,s.gradientNorm,s.parameterDeltaNorm,s.inputHash,s.objective,p.before,p.after,p.gap?1:0,p.optimizer,SCOPE,checkpoint,p.digest(checkpoint),now});}
        if(!pending.isEmpty())trim(db);
    }
    public static void trim(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_TRAINING_RECEIPT_RETENTION_WRITER");long excess;
        try(Cursor c=db.rawQuery("SELECT max(0,count(*)-?) FROM r26_training_step",new String[]{Integer.toString(DETAIL_LIMIT)})){if(!c.moveToFirst())return;excess=c.getLong(0);}if(excess==0)return;
        db.execSQL("DELETE FROM r26_training_step WHERE rowid IN (SELECT rowid FROM r26_training_step ORDER BY rowid LIMIT ?)",new Object[]{excess});
        db.execSQL("INSERT OR IGNORE INTO r26_retention_total VALUES('training_step_details',0)");db.execSQL("UPDATE r26_retention_total SET folded=folded+? WHERE kind='training_step_details'",new Object[]{excess});
    }
    /** Full retained-row hash audit; caller decides whether to run this diagnostic. */
    public static boolean verify(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT policy_hash,campaign_id,sample_ordinal,sample_issue,member_key,source_hash,source_kind,kernel_update,loss_before,loss_after,gradient_norm,parameter_delta_norm,input_hash,objective,before_state_hash,after_state_hash,gap_reset,optimizer,scope,batch_checkpoint,receipt_hash FROM r26_training_step ORDER BY rowid",null)){
            while(c.moveToNext()){if(!SCOPE.equals(c.getString(18)))return false;try{TrainingDiagnosticsR26.Step step=new TrainingDiagnosticsR26.Step(c.getLong(7),c.getDouble(8),c.getDouble(9),c.getDouble(10),c.getDouble(11),c.getString(12),c.getString(13));Pending p=new Pending(c.getString(0),c.getString(1),c.getString(5),c.getString(6),c.getString(3),c.getString(4),c.getString(14),c.getString(15),c.getString(17),c.getLong(2),c.getInt(16)!=0,step);if(!p.digest(c.getString(19)).equals(c.getString(20)))return false;}catch(IllegalArgumentException invalid){return false;}}
        }return true;
    }
}
