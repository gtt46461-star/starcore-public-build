package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;

/** Durable lease -> read/compute -> receipt + acknowledgement CAS, never an uncommitted model mutation. */
public final class ResearchFeedbackSU1 {
    private ResearchFeedbackSU1() {}
    public static boolean drainOne(DbStore store){
        final String owner="SU1_JOB_"+android.os.Process.myPid();long now=System.currentTimeMillis();
        String[] lease=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_OUTBOX_LEASE",db->{
            try(Cursor c=db.rawQuery("SELECT event_id,event_type,prediction_id,settlement_id,source_record_hash,lease_generation FROM su1_outbox WHERE (state='PENDING' AND retry_at<=?) OR (state='LEASED' AND lease_until<?) ORDER BY created_at,event_id LIMIT 1",new String[]{String.valueOf(now),String.valueOf(now)})){
                if(!c.moveToFirst())return null;long next=c.getLong(5)+1;db.execSQL("UPDATE su1_outbox SET state='LEASED',lease_owner=?,lease_until=?,lease_generation=lease_generation+1,attempt_count=attempt_count+1,updated_at=? WHERE event_id=? AND lease_generation=?",new Object[]{owner,now+30000,now,c.getString(0),c.getLong(5)});if(StrategyRepositorySU1.count(db,"SELECT changes()")!=1)throw new IllegalStateException("SU1_OUTBOX_LEASE_CAS");return new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),String.valueOf(next)};
            }
        });
        if(lease==null)return false;String payload="FROZEN_INPUT_DELIVERED|"+lease[4],kind="DATA_QUALITY";
        if(lease[1].equals("SETTLED"))try(Cursor c=store.readable().rawQuery("SELECT s.hit,p.selected_hit_p,s.label_hash FROM su1_settlement s JOIN su1_prediction p ON p.prediction_id=s.prediction_id WHERE s.settlement_id=? AND s.prediction_id=?",new String[]{lease[3],lease[2]})){
            if(!c.moveToFirst()||!lease[4].equals(c.getString(2)))throw new IllegalStateException("SU1_OUTBOX_SOURCE_CONFLICT");
            if(c.isNull(1))payload="SU1_FEEDBACK_V1|CALIBRATION_UNKNOWN|hit="+c.getInt(0)+"|label="+lease[4];
            else{kind="RESIDUAL";payload="SU1_FEEDBACK_V1|residual="+Double.toHexString(c.getInt(0)-c.getDouble(1))+"|label="+lease[4];}
        }
        final String result=payload,feedbackKind=kind;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_OUTBOX_EFFECT_ACK",db->{
            if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_outbox WHERE event_id=? AND state='LEASED' AND lease_generation=? AND lease_owner=?",lease[0],lease[5],owner))throw new IllegalStateException("SU1_OUTBOX_STALE_LEASE");
            if(lease[1].equals("SETTLED")){long at=System.currentTimeMillis();String blob=StrategyRepositorySU1.blob(db,result,"SU1_FEEDBACK_V1",at),contract=HashingSU1.text("SU1_WINDOW_RESET_CANONICAL_FEEDBACK_V1"),hash=HashingSU1.fields(lease[3],feedbackKind,contract,blob);
                if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_feedback WHERE feedback_hash=?",hash))StrategyRepositorySU1.row(db,"su1_feedback","feedback_hash",hash,"settlement_id",lease[3],"kind",feedbackKind,"diagnostic_contract_hash",contract,"payload_hash",blob,"created_at",at);
            }
            db.execSQL("UPDATE su1_outbox SET state='DONE',lease_owner=NULL,lease_until=NULL,updated_at=? WHERE event_id=? AND lease_generation=? AND lease_owner=?",new Object[]{System.currentTimeMillis(),lease[0],Long.parseLong(lease[5]),owner});if(StrategyRepositorySU1.count(db,"SELECT changes()")!=1)throw new IllegalStateException("SU1_OUTBOX_ACK_CAS");return 0;
        });return true;
    }
}
