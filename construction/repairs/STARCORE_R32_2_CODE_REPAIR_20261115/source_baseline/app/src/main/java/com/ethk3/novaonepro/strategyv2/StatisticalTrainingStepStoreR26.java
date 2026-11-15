package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Statistical witnesses have their own typed fields and no gradient column. Same checkpoint transaction. */
public final class StatisticalTrainingStepStoreR26 {
    public static final String TABLE="r26_statistic_training_step";
    public static final String SCOPE="FIXED_INPUT_STATISTICAL_UPDATE_DIAGNOSTIC_NOT_QUALIFICATION";
    private StatisticalTrainingStepStoreR26(){}
    static boolean installed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){return c.moveToFirst();}}
    static void record(SQLiteDatabase db,TrainingStepStoreR26.Pending p,String checkpoint,long now){
        StatisticalTrainingDiagnosticsR26.Step s=p.statistic;
        db.execSQL("INSERT INTO "+TABLE+"(policy_hash,campaign_id,sample_ordinal,sample_issue,member_key,source_hash,source_kind,kernel_update,loss_before,loss_after,statistic_delta_norm,parameter_delta_norm,input_hash,objective,statistic_name,before_state_hash,after_state_hash,gap_reset,optimizer,scope,batch_checkpoint,receipt_hash,recorded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{p.policy,p.campaign,p.ordinal,p.issue,p.member,p.source,p.sourceKind,s.update,s.lossBefore,s.lossAfter,s.statisticDeltaNorm,s.parameterDeltaNorm,s.inputHash,s.objective,s.statisticName,p.before,p.after,p.gap?1:0,p.optimizer,SCOPE,checkpoint,p.digest(checkpoint),now});
    }
    static boolean verify(SQLiteDatabase db){
        if(!installed(db))return true; // Historical gradient-only databases make no statistical claim.
        try(Cursor c=db.rawQuery("SELECT policy_hash,campaign_id,sample_ordinal,sample_issue,member_key,source_hash,source_kind,kernel_update,loss_before,loss_after,statistic_delta_norm,parameter_delta_norm,input_hash,objective,statistic_name,before_state_hash,after_state_hash,gap_reset,optimizer,scope,batch_checkpoint,receipt_hash FROM "+TABLE+" ORDER BY rowid",null)){
            while(c.moveToNext()){if(!SCOPE.equals(c.getString(19)))return false;try{StatisticalTrainingDiagnosticsR26.Step s=new StatisticalTrainingDiagnosticsR26.Step(c.getLong(7),c.getDouble(8),c.getDouble(9),c.getDouble(10),c.getDouble(11),c.getString(12),c.getString(13),c.getString(14));TrainingStepStoreR26.Pending p=new TrainingStepStoreR26.Pending(c.getString(0),c.getString(1),c.getString(5),c.getString(6),c.getString(3),c.getString(4),c.getString(15),c.getString(16),c.getString(18),c.getLong(2),c.getInt(17)!=0,s);if(!p.digest(c.getString(20)).equals(c.getString(21)))return false;}catch(IllegalArgumentException invalid){return false;}}
        }return true;
    }
    /** Snapshot both tables' oldest ids before either deletion; otherwise a second selection over-trims. */
    static void trimCombined(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_TRAINING_RECEIPT_RETENTION_WRITER");boolean statistics=installed(db);long excess;
        String population="SELECT rowid AS row_id,recorded_at,'G' AS kind FROM r26_training_step"+(statistics?" UNION ALL SELECT rowid AS row_id,recorded_at,'S' AS kind FROM "+TABLE:"");
        try(Cursor c=db.rawQuery("SELECT max(0,count(*)-?) FROM ("+population+")",new String[]{Integer.toString(TrainingStepStoreR26.DETAIL_LIMIT)})){if(!c.moveToFirst())return;excess=c.getLong(0);}if(excess==0)return;
        List<Long> gradients=new ArrayList<>(),sufficient=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT row_id,kind FROM ("+population+") ORDER BY recorded_at,row_id,kind LIMIT ?",new String[]{Long.toString(excess)})){while(c.moveToNext())("G".equals(c.getString(1))?gradients:sufficient).add(c.getLong(0));}
        if(gradients.size()+sufficient.size()!=excess)throw new IllegalStateException("R26_TRAINING_RETENTION_SNAPSHOT");
        remove(db,"r26_training_step","training_step_details",gradients);remove(db,TABLE,"statistic_training_step_details",sufficient);
    }
    private static void remove(SQLiteDatabase db,String table,String fold,List<Long> ids){
        if(ids.isEmpty())return;for(int start=0;start<ids.size();start+=256){int end=Math.min(ids.size(),start+256);StringBuilder sql=new StringBuilder("DELETE FROM ").append(table).append(" WHERE rowid IN (");Object[] values=new Object[end-start];for(int i=start;i<end;i++){if(i>start)sql.append(',');sql.append('?');values[i-start]=ids.get(i);}db.execSQL(sql.append(')').toString(),values);}
        db.execSQL("INSERT OR IGNORE INTO r26_retention_total VALUES(?,0)",new Object[]{fold});db.execSQL("UPDATE r26_retention_total SET folded=folded+? WHERE kind=?",new Object[]{ids.size(),fold});
    }
    static String reportRows(){
        String common="policy_hash,campaign_id,sample_ordinal,member_key,source_hash,source_kind,kernel_update,loss_before,loss_after,";
        String tail="parameter_delta_norm,input_hash,objective,before_state_hash,after_state_hash,gap_reset,optimizer,scope,batch_checkpoint,receipt_hash,recorded_at,";
        return "SELECT "+common+"gradient_norm,NULL AS statistic_delta_norm,NULL AS statistic_name,"+tail+"'OBJECTIVE_GRADIENT' AS diagnostic_kind,'MEASURED' AS gradient_availability,rowid AS row_id FROM r26_training_step UNION ALL SELECT "+common+"NULL AS gradient_norm,statistic_delta_norm,statistic_name,"+tail+"'SUFFICIENT_STATISTIC_CHANGE' AS diagnostic_kind,'NOT_APPLICABLE_STATISTICAL_UPDATE' AS gradient_availability,rowid AS row_id FROM "+TABLE+" ORDER BY recorded_at DESC,row_id DESC,diagnostic_kind LIMIT 64";
    }
}
