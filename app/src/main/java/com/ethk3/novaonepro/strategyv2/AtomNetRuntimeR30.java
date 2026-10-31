package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.TargetIssueResolver;
import android.database.Cursor;
import java.util.*;

/** Runs on r27-atomnet only. Prepare/replay/train outside the sole short writer transaction. */
public final class AtomNetRuntimeR30 {
    private AtomNetRuntimeR30(){}
    static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
        AtomNetSessionR30 saved=rt.atomSessionR30;
        if(saved==null){
            saved=AtomNetStoreR30.restore(w.ro);
            if(saved!=null){rt.atomDataR30=AtomNetStoreR30.dataset(w.ro,saved.datasetHash);rt.atomSessionR30=saved;return ResearchPoolR27.Outcome.progress(1,saved.units);}
            ReplayDatasetR27 d=rt.evoData;if(d==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-replay-dataset)",0,-1);
            SplitR27 split=SplitR27.of(d,null);int first=AtomReplayR27.MIN_ROWS;
            int train=Math.max(0,split.trainTo-first)*10,select=(split.selectTo-Math.max(first,split.selectFrom))*10,hold=(split.holdoutTo-Math.max(first,split.holdoutFrom))*10;
            if(train<=0||select<=0)return ResearchPoolR27.Outcome.idle("INSUFFICIENT_TEN_POSITION_TRAINING_HISTORY",0,-1);
            saved=new AtomNetSessionR30(d.datasetHash,d.issues[split.holdoutTo-1],d.ordinals[split.holdoutTo-1],7+AtomReplayR27.eligible().length+10,train,select,Math.max(0,hold));
            ResearchWriteQueueR27.Batch start=new ResearchWriteQueueR27.Batch();long now=System.currentTimeMillis();AtomNetStoreR30.dataset(start,d,now);AtomNetStoreR30.checkpoint(start,saved,now);RuntimeR27.submit(w,"ATOMNET_R30_INIT",start);
            rt.atomDataR30=d;rt.atomSessionR30=saved;return ResearchPoolR27.Outcome.progress(1,0);
        }
        ReplayDatasetR27 d=rt.atomDataR30;
        if(d==null){d=AtomNetStoreR30.dataset(w.ro,saved.datasetHash);rt.atomDataR30=d;}
        // Copy before computing: no failed/cancelled write can mutate the committed/published learner.
        byte[] previous=saved.encode();AtomNetSessionR30 next=AtomNetSessionR30.decode(previous,StatsR27.sha256(previous));
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();long now=System.currentTimeMillis();int n=0;
        int end=AtomReplayR27.MIN_ROWS+(next.trainCount+next.selectCount+next.holdoutCount)/10;
        if(next.cachePeriod<end){
            long begin=System.nanoTime();int from=next.cachePeriod;
            while(next.cachePeriod<end&&next.cachePeriod<from+12){
                int t=next.cachePeriod;AtomReplayR27.Frame frame=AtomReplayR27.build(d,t,t+1);
                for(int p=0;p<10;p++){
                    if(frame.values[p]==null)throw new IllegalStateException("R30_POSITION_MISSING_INPUT:"+t+":"+(p+1));
                    double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[p],frame.present[p],true),p+1);
                    AtomNetStoreR30.sample(b,next.datasetHash,(t-AtomReplayR27.MIN_ROWS)*10+p,d.issues[t],p+1,x,frame.hit[p],frame.actual[p]-1);n++;
                }
                next.cachePeriod++;if((System.nanoTime()-begin)/1_000_000L>=100)break;
            }
            next.materializedSamples+=n;next.materializeMs+=(System.nanoTime()-begin)/1_000_000L;
        }else if(!next.ready()) n=next.work(AtomNetStoreR30.samples(w.ro,next));
        else{
            ReplayDatasetR27 recent=StoreR27.loadDataset(w.ro,AtomReplayR27.HISTORY+64);
            int t=-1;for(int i=AtomReplayR27.MIN_ROWS;i<recent.size();i++)if(recent.issues[i].compareTo(next.completeIssue)>0){t=i;break;}
            if(t<0){forecast(w,next,recent);return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT",next.trainer.onlineSteps,next.trainer.onlineSteps);}
            if(!next.partialIssue.isEmpty()&&!next.partialIssue.equals(recent.issues[t]))return ResearchPoolR27.Outcome.idle("BLOCKED_PARTIAL_ISSUE_MISSING",next.trainer.onlineSteps,-1);
            AtomReplayR27.Frame frame=AtomReplayR27.build(recent,t,t+1);int p=next.nextPosition-1;
            if(frame.values[p]==null)return ResearchPoolR27.Outcome.idle("BLOCKED_POSITION_INPUT",next.trainer.onlineSteps,-1);
            double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[p],frame.present[p],true),p+1);
            settle(b,w,recent.issues[t],p+1,frame.hit[p],now);
            next.online(new AtomNetTrainerR27.Sample(x,frame.hit[p],frame.actual[p]-1,AtomNetSessionR30.eventKey(recent.issues[t],p+1)),recent.issues[t],recent.ordinals[t],p+1);n=1;
        }
        AtomNetStoreR30.checkpoint(b,next,System.currentTimeMillis());RuntimeR27.submit(w,"ATOMNET_R30_SLICE",b);
        rt.atomSessionR30=next;
        if(next.ready())forecast(w,next,StoreR27.loadDataset(w.ro,AtomReplayR27.HISTORY+64));
        return ResearchPoolR27.Outcome.progress(n,++rt.atomUnitsR30);
    }
    static void settle(ResearchWriteQueueR27.Batch batch,ResearchPoolR27.Worker w,String issue,int position,int hit,long now){
        String suffix=String.format(java.util.Locale.ROOT,"_R30_P%02d",position);
        try(Cursor c=w.ro.rawQuery("SELECT p_hit FROM r30_atomnet_forecast WHERE target_issue=? AND position=? AND frozen_at<deadline_at",new String[]{issue,Integer.toString(position)})){
            if(c.moveToFirst()){double p=c.getDouble(0);batch.add("INSERT OR IGNORE INTO r27_brier_pair VALUES(?,?,?,?,?,?,?,?)",issue,"FUTURE","ATOMNET"+suffix,p,.3,hit,(.3-hit)*(.3-hit)-(p-hit)*(p-hit),now);}
        }
        long ordinal;try{ordinal=TargetIssueResolver.sequenceOrdinalSU1(issue);}catch(RuntimeException fixture){return;}
        if(position!=(int)Math.floorMod(ordinal,10L)+1)return;
        for(String model:new String[]{"ATOMNET","FTRL"})try(Cursor c=w.ro.rawQuery("SELECT p_model FROM r27_atomnet_pending WHERE target_issue=? AND model=? AND frozen_at<deadline_at",new String[]{issue,model})){
            if(c.moveToFirst()){double p=c.getDouble(0);batch.add("INSERT OR IGNORE INTO r27_brier_pair VALUES(?,?,?,?,?,?,?,?)",issue,"FUTURE",model,p,.3,hit,(.3-hit)*(.3-hit)-(p-hit)*(p-hit),now);}
        }
    }
    static void forecast(ResearchPoolR27.Worker w,AtomNetSessionR30 state,ReplayDatasetR27 recent)throws Exception{
        if(!state.ready()||recent.size()<AtomReplayR27.MIN_ROWS)return;
        long targetOrdinal=recent.ordinals[recent.size()-1]+1,deadline=RuntimeR27.latestOpen(w.ro)+60_000L;
        if(state.trainedThroughOrdinal()>=targetOrdinal||System.currentTimeMillis()>=deadline-LiveShadowsR27.FREEZE_MARGIN_MS)return;
        String target=TargetIssueResolver.fromSequenceOrdinalSU1(targetOrdinal);
        try(Cursor c=w.ro.rawQuery("SELECT count(*) FROM r30_atomnet_forecast WHERE target_issue=?",new String[]{target})){if(c.moveToFirst()&&c.getLong(0)==10)return;}
        long revision=SchemaR27.liveSourceRevision(w.ro),schema=SchemaR27.liveSourceSchemaVersion(w.ro);
        if(revision<0||schema<0||!SchemaR27.liveSourceTriggersValid(w.ro))return;
        recent=StoreR27.loadDataset(w.ro,AtomReplayR27.HISTORY+64);
        if(recent.size()==0||recent.ordinals[recent.size()-1]+1!=targetOrdinal)return;
        ReplayDatasetR27 slot=recent.append(new ReplayDatasetR27.Row(target,targetOrdinal,new int[]{1,2,3,4,5,6,7,8,9,10}));
        AtomReplayR27.Frame frame=AtomReplayR27.build(slot,slot.size()-1,slot.size());
        long available=System.currentTimeMillis();ModelSurfaceProviderR30.FrozenModelRef modelRef=new ModelSurfaceProviderR30.FrozenModelRef(state.trainer.net.encode(),state.trainedThroughOrdinal(),available);
        String modelHash=modelRef.modelHash;long at=System.currentTimeMillis();ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        for(int i=0;i<10;i++){
            if(frame.values[i]==null) return;
            double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[i],frame.present[i],true),i+1);
            AtomNetR27.Out prediction=state.trainer.net.forward(x,null);
            long preparedAt=System.currentTimeMillis();
            ModelSurfaceProviderR30.Surface surface=ModelSurfaceProviderR30.predict(new ModelSurfaceProviderR30.Pit(targetOrdinal,recent.ordinals[recent.size()-1],i+1,ModelSurfaceProviderR30.historyHash(slot,slot.size()-1),x,preparedAt,preparedAt,preparedAt,deadline),modelRef);
            ModelSurfaceStoreR30.surface(batch,surface,i==0?modelRef:null);
            List<Object> p=new ArrayList<>();for(double value:surface.probabilities())p.add(value);
            batch.add("INSERT OR IGNORE INTO r30_atomnet_forecast VALUES(?,?,?,?,?,?,?,?)",target,i+1,modelHash,state.trainedThroughOrdinal(),JsonR27.write(p),prediction.pHit,at,deadline);
            if(i+1==(int)Math.floorMod(targetOrdinal,10L)+1){
                batch.add("INSERT OR IGNORE INTO r27_atomnet_pending VALUES(?,?,?,?,?)",target,"ATOMNET",prediction.pHit,at,deadline);
                batch.add("INSERT OR IGNORE INTO r27_atomnet_pending VALUES(?,?,?,?,?)",target,"FTRL",state.ftrl.predict(Arrays.copyOf(x,7)),at,deadline);
                for(String model:new String[]{"ATOMNET","FTRL"})batch.add("INSERT OR IGNORE INTO r29_atomnet_forecast VALUES(?,?,?,?,?,?,?)",target,model,modelHash,state.trainedThrough(),AtomNetSessionR30.ABI,at,deadline);
            }
        }
        final String sourceIssue=recent.issues[recent.size()-1];
        batch.before=tx->LiveShadowsR27.sameDrawHead(tx,sourceIssue,deadline)&&System.currentTimeMillis()<deadline-LiveShadowsR27.FREEZE_MARGIN_MS&&revision==SchemaR27.liveSourceRevision(tx)&&schema==SchemaR27.liveSourceSchemaVersion(tx)&&SchemaR27.liveSourceTriggersValid(tx)?0:-1;
        batch.after=batch.before;
        try{RuntimeR27.submit(w,"ATOMNET_R30_FORECAST",batch);}catch(IllegalStateException late){if(!QualificationInboxR29.precondition(late))throw late;}
    }
}
