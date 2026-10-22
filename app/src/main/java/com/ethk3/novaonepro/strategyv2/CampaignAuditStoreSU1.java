package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Historical audit sessions have no pool admission, CONFIRM allocation, alpha or certificate side effects. */
public final class CampaignAuditStoreSU1 {
    private CampaignAuditStoreSU1() {}
    public static void enqueue(SQLiteDatabase db,CampaignStateSU1 source,long now){
        ModelAblationStoreSU1.enqueue(db,source,now);
        for(CampaignControlsSU1.Variant variant:CampaignControlsSU1.Variant.values()){
            if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_campaign_audit WHERE source_campaign=? AND variant=?",source.campaign,variant.name()))continue;
            String config="{\"kind\":\"MATCHED_HISTORICAL_AUDIT_RESEARCH_ONLY\",\"source\":\""+source.campaign+"\",\"variant\":\""+variant.name()+"\",\"kernel\":\""+TrainingCodeIdentityR964.SHA256+"\"}";
            String audit=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",source.through,config,now);
            StrategyRepositorySU1.row(db,"su1_campaign_audit","source_campaign",source.campaign,"variant",variant.name(),"audit_campaign",audit,"dataset_blob",source.datasetBlob,"generation",0,"phase",source.foundry.valEnd>=4840?"SEARCH":"WAIT_HISTORY","updated_at",now);
        }
    }
    public static boolean advanceOne(DbStore store,BudgetTokenSU1 budget){
        String[] row;
        try(Cursor c=store.readable().rawQuery("SELECT a.source_campaign,a.variant,a.audit_campaign,a.dataset_blob,a.generation,a.checkpoint_hash,a.phase,a.selected_hash,a.selected_program,s.data_through_sequence FROM su1_campaign_audit a JOIN su1_campaign s ON s.campaign_id=a.source_campaign WHERE a.phase IN ('SEARCH','SEALED') AND s.config_json LIKE ? ORDER BY a.updated_at,a.source_campaign,a.variant LIMIT 1",new String[]{"%"+TrainingCodeIdentityR964.SHA256+"%"})){
            if(!c.moveToFirst())return false;row=new String[10];for(int i=0;i<row.length;i++)row[i]=c.isNull(i)?null:c.getString(i);
        }
        int[][] raw=CampaignStateSU1.unpack(CampaignStateSU1.localBlob(store.readable(),row[3]));
        try(CampaignAuditSessionSU1 session=row[5]==null?new CampaignAuditSessionSU1(raw,row[3],CampaignControlsSU1.Variant.valueOf(row[1]),Long.parseLong(row[9])):CampaignAuditSessionSU1.restore(raw,row[3],CampaignStateSU1.localBlob(store.readable(),row[5]))){
            if(!session.phase.equals(row[6])||!Objects.equals(session.selectedHash(),row[7])||!Objects.equals(session.selectedText(),row[8]))throw new IllegalStateException("SU1_AUDIT_SELECTED_IDENTITY");
            List<String> seen=new ArrayList<>();try(Cursor c=store.readable().rawQuery("SELECT genome_key FROM su1_search_attempt WHERE campaign_id=? AND result='EVALUATED'",new String[]{row[2]})){while(c.moveToNext())seen.add(c.getString(0));}session.foundry.restoreSeen(seen);
            try{session.advance(budget);}catch(BudgetTokenSU1.Yield yielded){/* The exact cursor is durable, including a partial held-out replay. */}
            String checkpoint=session.checkpoint(),result=session.phase.equals("DONE")?session.result():null;long now=System.currentTimeMillis(),generation=Long.parseLong(row[4]);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_HISTORICAL_AUDIT_CHECKPOINT",db->{
                if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_campaign_audit WHERE source_campaign=? AND variant=? AND generation=? AND phase=?",row[0],row[1],row[4],row[6]))throw new IllegalStateException("SU1_AUDIT_GENERATION_CAS");
                String hash=StrategyRepositorySU1.blob(db,checkpoint,"SU1_AUDIT_CHECKPOINT_V1",now);
                if(session.attempt!=null){StrategyFoundrySU1.Attempt a=session.attempt;String id=HashingSU1.fields(row[2],String.valueOf(session.attempts),a.genome.key());
                    if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_search_attempt WHERE attempt_id=?",id))StrategyRepositorySU1.row(db,"su1_search_attempt","attempt_id",id,"genome_key",a.genome.key(),"campaign_id",row[2],"program_text",a.genome.text,"arm",a.genome.arm,"operator",a.genome.operator,"parent_id",a.genome.parent,"dev_fitness",a.ranked==null?null:a.ranked.fitness,"val_fitness",a.ranked==null?null:a.ranked.valFitness,"result",a.result,"detail","HISTORICAL_CONTROL_ONLY|"+row[1]+"|"+a.detail,"created_at",now);
                }
                if(result!=null){String output=StrategyRepositorySU1.blob(db,result,"SU1_HISTORICAL_CONTROL_REPORT_V1",now);StrategyRepositorySU1.row(db,"su1_campaign_audit_open","source_campaign",row[0],"variant",row[1],"selected_hash",session.selectedHash(),"report_hash",output,"opened_at",now);}
                db.execSQL("UPDATE su1_campaign_audit SET generation=generation+1,checkpoint_hash=?,phase=?,selected_hash=?,selected_program=?,updated_at=? WHERE source_campaign=? AND variant=? AND generation=?",new Object[]{hash,session.phase,session.selectedHash(),session.selectedText(),now,row[0],row[1],generation});
                if(StrategyRepositorySU1.count(db,"SELECT changes()")!=1)throw new IllegalStateException("SU1_AUDIT_CHECKPOINT_CAS");
                if(result!=null)db.execSQL("UPDATE su1_campaign SET status='DONE',finished_at=?,programs_evaluated=? WHERE campaign_id=? AND status='RUNNING'",new Object[]{now,session.foundry.evaluated,row[2]});
                return 0;
            });
            return true;
        }
    }
}
