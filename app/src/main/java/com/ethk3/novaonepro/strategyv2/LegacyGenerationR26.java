package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;

/** The original R14/R15/R16 engine runs on a fixed snapshot, with durable member replay and fair quanta. */
public final class LegacyGenerationR26 {
    private LegacyGenerationR26() {}
    private static Context active;
    private static final class Context {
        final String id,source,abi;final FoundryCampaignR26.Snapshot snapshot;final int generation;final byte[] evolution;
        volatile RuntimeCoordinatorR26.Claim claim;CooperativeContinuationR26<Result> work;
        Context(String id,String source,String abi,FoundryCampaignR26.Snapshot snapshot,int generation,byte[] evolution){this.id=id;this.source=source;this.abi=abi;this.snapshot=snapshot;this.generation=generation;this.evolution=evolution;}
    }
    private static final class Result {
        ModelEvolutionSU2.State state;List<double[]> populationScores,hofScores;List<StageStrategySU2.Definition> stages;CommitteeSU2 matrixChampion;MatrixSnapshotSU2.Data matrices;
    }
    public static synchronized void cancel(String owner){if(active!=null&&active.claim!=null&&active.claim.owner.equals(owner)){active.work.close();active=null;}}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    private static void status(DbStore store,Context c,String state){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_GENERATION_STATUS",db->{db.execSQL("UPDATE r26_legacy_campaign SET status=?,phase=?,snapshot=CASE WHEN ?='RUNNING' THEN snapshot ELSE X'' END,updated_at=? WHERE campaign_id=?",new Object[]{state,state,state,System.currentTimeMillis(),c.id});return 0;});}
    public static long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws Exception {
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long latestOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);long carry=scalar(store.readable(),"SELECT completed FROM r26_work_total WHERE task_id='legacy-generation'");claim.advanceCursorTo(carry);carry=Math.max(0,carry-claim.cursor);String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        if(active!=null&&(!active.abi.equals(abi)||latestOrdinal-active.snapshot.ordinal[0]>=5000)){active.work.close();status(store,active,"EXPIRED_SOURCE_OR_ABI");active=null;}
        if(active==null){
            String id=null,source=null;byte[] snapshot=null;int generation=0;
            try(Cursor c=store.readable().rawQuery("SELECT campaign_id,snapshot_hash,snapshot,generation FROM r26_legacy_campaign WHERE status='RUNNING' AND numerical_abi=? ORDER BY updated_at LIMIT 1",new String[]{abi})){if(c.moveToFirst()){id=c.getString(0);source=c.getString(1);snapshot=c.getBlob(2);generation=c.getInt(3);}}
            if(id==null){
                long last=-1;try(Cursor c=store.readable().rawQuery("SELECT max(source_through) FROM r26_legacy_campaign WHERE status='DONE' AND numerical_abi=?",new String[]{abi})){if(c.moveToFirst()&&!c.isNull(0))last=c.getLong(0);}if(last>=0&&latestOrdinal-last<ModelFoundryJobSU2.REFRESH_EVERY)return carry;
                FoundryCampaignR26.Snapshot captured=FoundryCampaignR26.capture(store);if(captured==null||captured.draws.length<3400){claim.reason("LEGACY_GENERATION_HISTORY_LT_3400");return carry;}byte[] bytes=captured.encode();source=LiveStateR26.hash(bytes);id=HashingSU1.fields("R26_LEGACY_CAMPAIGN_V1",source,abi).substring(0,32);final String campaign=id,hash=source;
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_GENERATION_REGISTER",db->{db.execSQL("INSERT OR IGNORE INTO r26_legacy_campaign(campaign_id,snapshot_hash,snapshot,source_through,source_first,numerical_abi,updated_at) VALUES(?,?,?,?,?,?,?)",new Object[]{campaign,hash,bytes,captured.through,captured.ordinal[0],abi,System.currentTimeMillis()});
                    int T=captured.draws.length;db.execSQL("INSERT OR IGNORE INTO su2_campaign(campaign_id,through_sequence,dev_start,dev_end,val_start,val_end,generations,population,status,started_at) VALUES(?,?,?,?,?,?,?,?,'RUNNING',?)",new Object[]{campaign,captured.through,1500,T-ModelFoundryJobSU2.VAL_LEN,T-ModelFoundryJobSU2.VAL_LEN,T,ModelFoundryJobSU2.GENERATIONS,ModelFoundryJobSU2.POPULATION,System.currentTimeMillis()});return 0;});snapshot=bytes;
            }
            FoundryCampaignR26.Snapshot frozen=FoundryCampaignR26.Snapshot.decode(snapshot);if(latestOrdinal-frozen.ordinal[0]>=5000){final String expired=id;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_EXPIRE",db->{db.execSQL("UPDATE r26_legacy_campaign SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id=?",new Object[]{expired});return 0;});return carry;}
            Context context=new Context(id,source,abi,frozen,generation,ModelStateCodecSU2.encode(ModelStoreSU2.loadEvolution(store.readable())));context.claim=claim;
            context.work=new CooperativeContinuationR26<>("starcore-r26-legacy-generation",()->compute(store,context),()->RuntimeCoordinatorR26.hasCriticalPressure(),()->RuntimeCoordinatorR26.bindBackgroundClaim(context.claim),()->android.os.SystemClock.currentThreadTimeMillis()*1000000L,claim.owner);active=context;
        }
        Context context=active;context.claim=claim;budget.checkpointOrYield();CooperativeContinuationR26.Slice<Result> slice=context.work.slice(40);claim.addWorkerCpuMillis(slice.cpuNanos/1000000L);
        long committed=scalar(store.readable(),"SELECT completed FROM r26_work_total WHERE task_id='legacy-generation'");claim.advanceCursorTo(committed);long delta=Math.max(0,committed-claim.cursor);
        if(!slice.done){claim.reason("COOPERATIVE_LEGACY_GENERATION:"+context.generation);return delta;}
        if(slice.error!=null){active=null;context.work.close();if(slice.error instanceof InterruptedException||slice.error instanceof BudgetTokenSU1.Yield){claim.reason("LEGACY_CONTINUATION_RESTART_FROM_COMMITTED_REPLAY");return delta;}throw new IllegalStateException("R26_LEGACY_GENERATION",slice.error);}
        Result result=slice.result;try{verifySource(store,context.snapshot);}catch(IOException revised){context.work.close();status(store,context,"INVALIDATED_FROZEN_SOURCE");active=null;claim.reason("LEGACY_SOURCE_REVISED_OR_EXPIRED");return delta;}long generationNow=EventFrameBuilderSU1.generation(store.readable());boolean finished=context.generation+1>=ModelFoundryJobSU2.GENERATIONS;long now=System.currentTimeMillis();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_GENERATION_COMMIT",db->{RuntimeCoordinatorR26.requireFence(db,claim);if(EventFrameBuilderSU1.generation(db)!=generationNow)throw new IllegalStateException("R26_GENERATION_SOURCE_CHANGED_BEFORE_COMMIT");
            try(Cursor c=db.rawQuery("SELECT generation,status FROM r26_legacy_campaign WHERE campaign_id=?",new String[]{context.id})){if(!c.moveToFirst()||c.getInt(0)!=context.generation||!c.getString(1).equals("RUNNING"))throw new IllegalStateException("R26_LEGACY_GENERATION_CAS");}
            for(int i=0;i<result.state.population.size();i++){double[] score=result.populationScores.get(i);ModelStoreSU2.saveGenome(db,result.state.population.get(i),context.id,context.snapshot.through,score[0],score[1],"RESEARCH",now);}
            for(int i=0;i<result.state.hallOfFame.size();i++){double[] score=result.hofScores.get(i);ModelStoreSU2.saveGenome(db,result.state.hallOfFame.get(i),context.id,context.snapshot.through,score[0],score[1],"HALL_OF_FAME",now);}
            ModelStoreSU2.saveEvolution(db,context.id,result.state,now);db.execSQL("UPDATE su2_campaign SET evaluated=evaluated+?,effective_generations=? WHERE campaign_id=?",new Object[]{result.state.population.size(),result.state.effectiveGenerations,context.id});
            if(finished){for(Map.Entry<String,CommitteeSU2> entry:result.state.committees.entrySet()){CommitteeSU2 c=entry.getValue();ModelStoreSU2.saveCommittee(db,c,entry.getKey(),context.id,context.snapshot.through,result.state.committeeJudge,now);ModelStoreSU2.joinLeague(db,"SU2:"+c.key(),"SU2",entry.getKey()+"·委员会",c.key(),latestOrdinal+1);}StageStoreSU2.save(db,result.stages,context.id,context.snapshot.through,now);if(result.matrices!=null)MatrixStoreSU2.save(db,result.matrixChampion.key(),context.snapshot.through,result.matrices,now);db.execSQL("UPDATE su2_campaign SET status='DONE',finished_at=? WHERE campaign_id=?",new Object[]{now,context.id});}
            db.execSQL("UPDATE r26_legacy_campaign SET generation=generation+1,phase=?,status=?,updated_at=? WHERE campaign_id=?",new Object[]{finished?"COMPLETED":"GENERATION_COMMITTED",finished?"DONE":"RUNNING",now,context.id});db.execSQL("INSERT OR IGNORE INTO r26_work_total VALUES('legacy-generation',0)");db.execSQL("UPDATE r26_work_total SET completed=completed+1 WHERE task_id='legacy-generation'");return 0;});
        context.work.close();active=null;committed=scalar(store.readable(),"SELECT completed FROM r26_work_total WHERE task_id='legacy-generation'");claim.advanceCursorTo(committed);return Math.max(0,committed-claim.cursor);
    }
    private static Result compute(DbStore store,Context context)throws Exception {
        int[][] draws=context.snapshot.draws;ReplayWorkspaceR26 durable=new ReplayWorkspaceR26(store,context.id,context.source,context.snapshot.through);long cap=Math.min(ModelFoundryJobSU2.MODEL_BYTES,Math.max(4L<<20,Runtime.getRuntime().maxMemory()/16));
        ModelReplaySU2.Library lib=new ModelReplaySU2.Library(draws,cap,context.source,context.snapshot.through,durable);ModelEvolutionSU2 evo=new ModelEvolutionSU2(lib,ModelFoundryJobSU2.seed(context.id,context.generation),1500,2500,250);ModelEvolutionSU2.Eras eras=new ModelEvolutionSU2.Eras(1500,draws.length-ModelFoundryJobSU2.VAL_LEN,draws.length-ModelFoundryJobSU2.VAL_LEN,draws.length);
        Map<String,String> lineageKeys=new HashMap<>();evo.proposalObserver=(child,parent)->{
            ModelGenomeSU2 next=lib.bind(child.g),prior=lib.bind(parent.g);lineageKeys.put(child.g.key(),next.key());lineageKeys.put(parent.g.key(),prior.key());String structure=StructuralProposalR26.structure(next),category=next.key().equals(prior.key())?"NO_OP":structure.equals(StructuralProposalR26.structure(prior))?"PARAMETER_VARIANT":"STRUCTURAL_CANDIDATE";
            String attempt=HashingSU1.fields("R26_LEGACY_PROPOSAL_V1",context.id,String.valueOf(context.generation),next.key(),prior.key(),child.origin);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_LEGACY_PROPOSAL",db->{long now=System.currentTimeMillis();db.execSQL("INSERT OR IGNORE INTO r26_legacy_proposal VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{attempt,context.id,context.generation,next.key(),next.text(),prior.key(),prior.text(),child.origin,category,now});db.execSQL("INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) VALUES(?,?,?,?,?,?,?)",new Object[]{attempt,context.id,next.key(),prior.key(),child.origin,category,structure});return 0;});
        };
        ModelEvolutionSU2.State state=evo.campaign(ModelStateCodecSU2.decode(context.evolution),eras,1,ModelFoundryJobSU2.POPULATION,ModelFoundryJobSU2.HALL_OF_FAME);
        bind(state,lib,lineageKeys);Result result=new Result();result.state=state;result.populationScores=new ArrayList<>();result.hofScores=new ArrayList<>();
        for(ModelEvolutionSU2.Ranked ranked:state.population){ExecutionBudgetR26.poll();result.populationScores.add(new double[]{evo.dev(ranked.g,eras),evo.val(ranked.g,eras)});}for(ModelEvolutionSU2.Ranked ranked:state.hallOfFame){ExecutionBudgetR26.poll();result.hofScores.add(new double[]{evo.dev(ranked.g,eras),evo.val(ranked.g,eras)});}
        boolean finished=context.generation+1>=ModelFoundryJobSU2.GENERATIONS;result.stages=finished?StageStrategySU2.prepare(state,lib,context.snapshot.through):Collections.emptyList();result.matrixChampion=state.committees.get("F15");result.matrices=finished&&result.matrixChampion!=null?MatrixSnapshotSU2.capture(result.matrixChampion,lib,context.snapshot.through):null;
        if(finished){
            for(Map.Entry<String,CommitteeSU2> entry:state.committees.entrySet())LegacyPublicationR26.prepare(store,context.id,context.source,"L:C:"+entry.getKey(),entry.getValue().text(),context.snapshot.ordinal,lib);
            for(StageStrategySU2.Definition stage:result.stages){StageStrategySU2.register(stage);LegacyPublicationR26.prepare(store,context.id,context.source,"L:S:"+stage.tier,stage.text(),context.snapshot.ordinal,lib);}
            if(state.champion!=null)LegacyPublicationR26.prepare(store,context.id,context.source,"L:P:CHAMPION",state.champion.g.text(),context.snapshot.ordinal,lib);
            ModelEvolutionSU2.Ranked tier=state.tiers.get("F15");if(tier!=null)LegacyPublicationR26.prepare(store,context.id,context.source,"L:P:F15",tier.g.text(),context.snapshot.ordinal,lib);
        }
        return result;
    }
    private static ModelEvolutionSU2.Ranked bind(ModelEvolutionSU2.Ranked ranked,ModelReplaySU2.Library lib,Map<String,String> keys){return new ModelEvolutionSU2.Ranked(lib.bind(ranked.g),ranked.origin,keys.getOrDefault(ranked.parent,ranked.parent),ranked.note);}
    private static void bind(ModelEvolutionSU2.State state,ModelReplaySU2.Library lib,Map<String,String> keys){for(ModelEvolutionSU2.Ranked r:state.population)keys.put(r.g.key(),lib.bind(r.g).key());for(ModelEvolutionSU2.Ranked r:state.hallOfFame)keys.put(r.g.key(),lib.bind(r.g).key());for(int i=0;i<state.population.size();i++)state.population.set(i,bind(state.population.get(i),lib,keys));for(int i=0;i<state.hallOfFame.size();i++)state.hallOfFame.set(i,bind(state.hallOfFame.get(i),lib,keys));if(state.champion!=null)state.champion=bind(state.champion,lib,keys);for(String tier:new ArrayList<>(state.tiers.keySet()))state.tiers.put(tier,bind(state.tiers.get(tier),lib,keys));for(String tier:new ArrayList<>(state.committees.keySet())){CommitteeSU2 c=state.committees.get(tier);List<ModelGenomeSU2> subs=new ArrayList<>();for(ModelGenomeSU2 g:c.subs)subs.add(lib.bind(g));state.committees.put(tier,new CommitteeSU2(subs,c.coverage));}}
    private static void verifySource(DbStore store,FoundryCampaignR26.Snapshot snapshot)throws IOException {
        Map<Long,Models.Draw> current=new HashMap<>();for(Models.Draw draw:store.getRecentDraws(5000))current.put(Long.parseLong(draw.issue),draw);
        for(int i=0;i<snapshot.draws.length;i++){Models.Draw row=current.get(snapshot.issues[i]);if(row==null||row.fetchTime!=snapshot.available[i]||row.openTime!=snapshot.opened[i]||!Arrays.equals(row.numbers,snapshot.draws[i]))throw new IOException("R26_LEGACY_FROZEN_SOURCE_REVISED_OR_EXPIRED");}
    }
}
