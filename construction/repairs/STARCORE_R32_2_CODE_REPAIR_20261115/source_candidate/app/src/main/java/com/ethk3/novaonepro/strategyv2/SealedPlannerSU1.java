package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** One champion, chosen before a disjoint future span; never opens a second winner on that span. */
public final class SealedPlannerSU1 {
    private SealedPlannerSU1() {}
    public static void plan(SQLiteDatabase db,CampaignStateSU1 state,long now,Models.Draw latest){
        if(!db.inTransaction())throw new IllegalStateException("SU1_PLAN_OWNER");
        if(latest==null||!TargetIssueResolver.isValid(latest.issue)||state.foundry.ranked().isEmpty())return;
        if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_sealed_plan p WHERE NOT EXISTS(SELECT 1 FROM su1_sealed_cancelled x WHERE x.campaign_id=p.campaign_id) AND NOT EXISTS(SELECT 1 FROM su1_sealed_result r WHERE r.plan_campaign=p.campaign_id)"))return;
        List<StrategyFoundrySU1.Ranked> finalists=new ArrayList<>(state.foundry.ranked().subList(0,Math.min(4,state.foundry.ranked().size())));
        finalists.sort(Comparator.comparingDouble((StrategyFoundrySU1.Ranked r)->r.valFitness).reversed().thenComparing(r->r.genome.key()));
        StrategyFoundrySU1.Ranked champion=state.foundry.champion()==null?finalists.get(0):state.foundry.champion();if(champion.valFitness<=-1)return;
        // Completion can be hours after the training snapshot. Choose from the wall clock,
        // the canonical latest issue and the training boundary, never an already seen target.
        long first=Math.max(state.through+1,TargetIssueResolver.firstFutureOrdinalSU1(latest,now));
        long last=first+1439,offset=state.through-state.foundry.valEnd+1;
        String parent=StrategyRepositorySU1.findParent(db,champion.genome.parent);if(!champion.genome.parent.isEmpty()&&parent==null)return;
        String second=StrategyRepositorySU1.findParent(db,champion.genome.secondaryParent);if(!champion.genome.secondaryParent.isEmpty()&&second==null)return;
        String id=StrategyRepositorySU1.candidate(db,champion.genome.text,champion.genome.coverage,champion.genome.margin,champion.genome.arm,parent,state.campaign,state.through,state.datasetBlob,champion.fitness,now);
        if(second!=null)StrategyRepositorySU1.lineage(db,second,id,"FUSE",state.through,HashingSU1.text(champion.genome.text),now);
        StrategyRepositorySU1.row(db,"su1_sealed_plan","campaign_id",state.campaign,"strategy_id",id,"dev_first",offset+state.foundry.devStart,"dev_last",offset+state.foundry.devEnd-1,"val_first",offset+state.foundry.valStart,"val_last",offset+state.foundry.valEnd-1,"first_sequence",first,"last_sequence",last,"selected_at",now);
    }
    private static String issue(long sequence){return TargetIssueResolver.fromSequenceOrdinalSU1(sequence);}
    private static long terminalCount(SQLiteDatabase db,long first,long last){
        try(Cursor c=db.rawQuery("SELECT count(*) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue>=? AND target_issue<=? AND status='COMPLETED'",new String[]{issue(first),issue(last)})){c.moveToFirst();return c.getLong(0);}
    }
    public static void maintain(DbStore store){
        List<String[]> rows=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT p.campaign_id,p.strategy_id,p.dev_first,p.dev_last,p.val_first,p.val_last,p.first_sequence,p.last_sequence,p.selected_at FROM su1_sealed_plan p JOIN su1_runtime_program k ON k.strategy_id=p.strategy_id WHERE k.kernel_hash=? AND NOT EXISTS(SELECT 1 FROM su1_sealed_cancelled x WHERE x.campaign_id=p.campaign_id) AND NOT EXISTS(SELECT 1 FROM su1_sealed_result r WHERE r.plan_campaign=p.campaign_id) ORDER BY p.selected_at LIMIT 1",new String[]{TrainingCodeIdentityR964.SHA256})){while(c.moveToNext()){String[] row=new String[9];for(int i=0;i<9;i++)row[i]=c.getString(i);rows.add(row);}}
        for(String[] r:rows){
            long first=Long.parseLong(r[6]),last=Long.parseLong(r[7]);if(terminalCount(store.readable(),first,last)!=last-first+1)continue;
            int actions=0,hits=0;int[] bn=new int[4],bk=new int[4];StringBuilder source=new StringBuilder("SU1_PRESELECTED_FUTURE_V1|").append(r[0]).append('|').append(r[1]);
            try(Cursor c=store.readable().rawQuery("SELECT sequence_ordinal,hit,captured_at,position,number_mask FROM su1_live_shadow WHERE strategy_id=? AND sequence_ordinal BETWEEN ? AND ? AND hit IS NOT NULL ORDER BY sequence_ordinal",new String[]{r[1],r[6],r[7]})){
                while(c.moveToNext()){int b=(int)Math.min(3,(c.getLong(0)-first)*4/(last-first+1));actions++;hits+=c.getInt(1);bn[b]++;bk[b]+=c.getInt(1);source.append('|').append(c.getLong(0)).append(':').append(c.getInt(1)).append(':').append(c.getLong(2)).append(':').append(c.getInt(3)).append(':').append(c.getInt(4));}
            }
            Map<String,Object> report=new TreeMap<>();report.put("protocol","SU1_PRESELECTED_FUTURE_V1");report.put("plan",r[0]);report.put("selectedAt",Long.parseLong(r[8]));report.put("first",first);report.put("last",last);report.put("actions",actions);report.put("hits",hits);report.put("calendarN",last-first+1);report.put("source",HashingSU1.text(source.toString()));List<Object> blocks=new ArrayList<>();for(int i=0;i<4;i++)blocks.add(Arrays.asList(bn[i],bk[i]));report.put("blocks",blocks);
            String payload=StrictCanonicalJson.write(report);long now=System.currentTimeMillis();boolean eligible=FoldStabilityFitnessSU1.wilsonLower(hits,actions)>.30;for(int i=0;i<4;i++)eligible&=bn[i]>=30&&bk[i]/(double)Math.max(1,bn[i])>.30;final boolean passes=eligible;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_OPEN_PRESELECTED_SEALED",db->{
                if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_sealed_result WHERE plan_campaign=?",r[0]))return 0;
                if(terminalCount(db,first,last)!=last-first+1)throw new IllegalStateException("SU1_SEALED_TERMINAL_CHANGED");
                String config="{\"kind\":\"PRESELECTED_FUTURE_SEALED\",\"plan\":\""+r[0]+"\"}",campaign=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",last,config,now);
                String[] kinds={"DEV","VAL","SEALED"};long[][] ranges={{Long.parseLong(r[2]),Long.parseLong(r[3])},{Long.parseLong(r[4]),Long.parseLong(r[5])},{first,last}};String sealed=null;
                for(int i=0;i<3;i++){String era=HashingSU1.fields(campaign,kinds[i]);StrategyRepositorySU1.row(db,"su1_era","era_id",era,"campaign_id",campaign,"kind",kinds[i],"first_sequence",ranges[i][0],"last_sequence",ranges[i][1]);if(i==2)sealed=era;}
                String result=StrategyRepositorySU1.blob(db,payload,"SU1_SEALED_RESULT_V1",now);StrategyRepositorySU1.row(db,"su1_sealed_open","era_id",sealed,"campaign_id",campaign,"strategy_id",r[1],"opened_at",now,"result_hash",result);
                StrategyRepositorySU1.row(db,"su1_sealed_result","plan_campaign",r[0],"open_campaign",campaign,"eligibility",passes?"PASS_SCREEN":"FAIL_SCREEN","recorded_at",now);
                db.execSQL("UPDATE su1_campaign SET status='DONE',finished_at=?,programs_evaluated=1 WHERE campaign_id=?",new Object[]{now,campaign});
                if(passes&&StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_runtime_program WHERE strategy_id=? AND coverage>=0.999 AND EXISTS(SELECT 1 FROM su1_strategy_version v WHERE v.strategy_id=su1_runtime_program.strategy_id AND v.calibration_state='FROZEN')",r[1])){
                    Models.Draw latest=store.getLatestDraw();if(latest!=null){String next=issue(TargetIssueResolver.firstFutureOrdinalSU1(latest,now));String parent=null;try(Cursor ancestry=db.rawQuery("SELECT parent_id FROM su1_runtime_program WHERE strategy_id=?",new String[]{r[1]})){if(ancestry.moveToFirst())parent=ancestry.getString(0);}
                    PromotionPipelineSU1.fromSealed(db,campaign,r[1],parent,HashingSU1.fields("SU1_FOUNDRY_LINEAGE_V1",TrainingCodeIdentityR964.SHA256),next,TargetIssueResolver.sequenceOrdinalSU1(latest.issue),4096);}
                }
                return 0;
            });
        }
    }
}
