package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** A single fixed 120/1/120 time split creates a fresh version, with zero inherited evidence. */
public final class CalibrationPipelineSU1 {
    private CalibrationPipelineSU1() {}
    public static void maintain(DbStore store){
        List<String[]> ids=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT p.strategy_id,p.dev_first,p.dev_last,p.val_first,p.val_last FROM su1_sealed_plan p JOIN su1_sealed_result s ON s.plan_campaign=p.campaign_id JOIN su1_strategy_version v ON v.strategy_id=p.strategy_id JOIN su1_runtime_program k ON k.strategy_id=v.strategy_id WHERE s.eligibility='PASS_SCREEN' AND v.calibration_state='MISSING' AND k.kernel_hash=? AND NOT EXISTS(SELECT 1 FROM su1_calibration_attempt a WHERE a.source_strategy=v.strategy_id) ORDER BY s.recorded_at LIMIT 1",new String[]{TrainingCodeIdentityR964.SHA256})){while(c.moveToNext()){String[] row=new String[5];for(int i=0;i<5;i++)row[i]=c.getString(i);ids.add(row);}}
        for(String[] r:ids){
            double[] raw=new double[241];int[] hits=new int[241];int n=0;long first=0,last=0;StringBuilder source=new StringBuilder("SU1_FIXED_LIVE_CAL_120_1_120|").append(r[0]);
            try(Cursor c=store.readable().rawQuery("SELECT l.sequence_ordinal,a.detail,l.hit,l.captured_at,l.position,l.number_mask FROM su1_live_shadow l JOIN su1_live_attempt a ON a.strategy_id=l.strategy_id AND a.sequence_ordinal=l.sequence_ordinal WHERE l.strategy_id=? AND l.hit IS NOT NULL ORDER BY l.sequence_ordinal LIMIT 241",new String[]{r[0]})){
                while(c.moveToNext()){String detail=c.getString(1);if(!detail.startsWith("RAW_K3_HEX="))return;raw[n]=Double.valueOf(detail.substring(11));hits[n]=c.getInt(2);if(n==0)first=c.getLong(0);last=c.getLong(0);source.append('|').append(c.getLong(0)).append(':').append(detail).append(':').append(c.getInt(2)).append(':').append(c.getLong(3)).append(':').append(c.getInt(4)).append(':').append(c.getInt(5));n++;}
            }
            if(n!=241)continue;CalibrationSU1 cal=CalibrationSU1.fit(raw,hits,120,121,HashingSU1.text(source.toString()));long now=System.currentTimeMillis();Models.Draw latest=store.getLatestDraw();if(latest==null)return;long through=LiveShadowLoopSU1.sequence(latest.issue);final long begin=first,end=last;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_FIXED_CALIBRATION",db->{
                if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_calibration_attempt WHERE source_strategy=?",r[0]))return 0;
                String blob=StrategyRepositorySU1.blob(db,cal.payload(),"SU1_CAL_LOGISTIC_V1",now),id=null;
                if(cal.valid()){
                    id=HashingSU1.fields("SU1_CALIBRATED_VERSION_V1",r[0],blob,TrainingCodeIdentityR964.SHA256);
                    db.execSQL("INSERT INTO su1_strategy_version SELECT ?,'SU1',program_hash,artifact_hash,selector_hash,?,'FROZEN',action_contract_hash,state_initial_hash,?,?,strategy_role FROM su1_strategy_version WHERE strategy_id=?",new Object[]{id,blob,through,now,r[0]});
                    db.execSQL("INSERT INTO su1_runtime_program SELECT ?,canonical_text,coverage,margin_gate,parent_id,?,kernel_hash FROM su1_runtime_program WHERE strategy_id=?",new Object[]{id,now,r[0]});
                    try(Cursor ancestor=db.rawQuery("SELECT parent_id FROM su1_runtime_program WHERE strategy_id=?",new String[]{id})){if(ancestor.moveToFirst()&&!ancestor.isNull(0))StrategyRepositorySU1.lineage(db,ancestor.getString(0),id,"CALIBRATE",through,blob,now);}
                    String campaign=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",through,"{\"kind\":\"FROZEN_CALIBRATION_NEW_VERSION\",\"source\":\""+r[0]+"\"}",now);
                    db.execSQL("UPDATE su1_campaign SET status='DONE',finished_at=? WHERE campaign_id=?",new Object[]{now,campaign});
                    if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_sealed_plan p WHERE NOT EXISTS(SELECT 1 FROM su1_sealed_cancelled x WHERE x.campaign_id=p.campaign_id) AND NOT EXISTS(SELECT 1 FROM su1_sealed_result q WHERE q.plan_campaign=p.campaign_id)")){
                        long future=Math.max(through+1,TargetIssueResolver.firstUnseenOrdinalSU1(now));
                        StrategyRepositorySU1.row(db,"su1_sealed_plan","campaign_id",campaign,"strategy_id",id,"dev_first",Long.parseLong(r[1]),"dev_last",Long.parseLong(r[2]),"val_first",Long.parseLong(r[3]),"val_last",Long.parseLong(r[4]),"first_sequence",future,"last_sequence",future+1439,"selected_at",now);
                        if(StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_research_pool WHERE state='ACTIVE'")>=32)db.execSQL("UPDATE su1_research_pool SET state='RETIRED',retired_sequence=max(born_sequence,?),retire_reason='CALIBRATED_VERSION_POOL_SLOT' WHERE entry_id=(SELECT entry_id FROM su1_research_pool WHERE state='ACTIVE' ORDER BY prior_fitness,entry_id LIMIT 1)",new Object[]{through});
                        StrategyRepositorySU1.row(db,"su1_research_pool","entry_id",HashingSU1.fields("SU1_CAL_POOL",id,campaign),"strategy_id",id,"campaign_id",campaign,"arm","CALIBRATED","born_sequence",through+1,"prior_fitness",-1d,"state","ACTIVE","created_at",now);
                    }
                }
                StrategyRepositorySU1.row(db,"su1_calibration_attempt","source_strategy",r[0],"result_hash",blob,"calibrated_strategy",id,"first_sequence",begin,"last_sequence",end,"recorded_at",now);return 0;
            });
        }
    }
}
