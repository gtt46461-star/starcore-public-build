package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.TargetIssueResolver;
import android.database.Cursor;
import java.util.*;

/** Runs on r27-atomnet only. Prepare/replay/train outside the sole short writer transaction. */
public final class AtomNetRuntimeR30 {
    private AtomNetRuntimeR30(){}
    static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
        AtomNetPublicationR31.requireConsumerSchemaR342(w.ro);
        AtomNetSessionR30 saved=rt.atomSessionR30;
        if(saved!=null){
            OnlineResearchWindowR30R1.register(w,saved.datasetHash);
            // Forecast before a training slice can yield, using only the last committed complete model.
            // Partial positions and validation do not prevent this independent read-only consumer.
            if(forecastModelR32(rt,saved)!=null)forecast(rt,w,saved,StoreR27.loadDataset(w.ro,AtomReplayR27.HISTORY+64));
        }
        // R32.5 archive maintenance has its own fenced task; an old damaged segment
        // must not become a prerequisite for restoring or training this learner.
        if(saved==null){
            // Cold start: a published checkpoint may be a file reference. Register this database's owned
            // checkpoint directory first instead of relying on another module having called forStore earlier.
            CheckpointArtifactsR26.forStore(w.store());
            String restoringHead=AtomNetPublicationR31.currentStateHash(w.ro);
            saved=AtomNetStoreR30.restore(w.ro);
            if(saved!=null){
                ReplayDatasetR27 restoredData=AtomNetStoreR30.dataset(w.ro,saved.datasetHash);
                ModelSurfaceProviderR30.FrozenModelRef restoredModel=AtomNetStoreR30.publicationModelR31(w.ro,saved);
                if(!restoringHead.equals(AtomNetPublicationR31.currentStateHash(w.ro)))
                    return ResearchPoolR27.Outcome.idle("COLD_ATOMNET_SOURCE_CHANGED_RETRY",0,-1);
                rt.atomDataR30=restoredData;rt.restoreAtomStateR32(saved,restoredModel);
                return ResearchPoolR27.Outcome.progress(1,saved.units);
            }
            ReplayDatasetR27 d=rt.evoData;if(d==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-replay-dataset)",0,-1);
            SplitR27 split=SplitR27.of(d,null);int first=AtomReplayR27.MIN_ROWS;
            int train=Math.max(0,split.trainTo-first)*10,select=(split.selectTo-Math.max(first,split.selectFrom))*10,hold=(split.holdoutTo-Math.max(first,split.holdoutFrom))*10;
            if(train<=0||select<=0)return ResearchPoolR27.Outcome.idle("INSUFFICIENT_TEN_POSITION_TRAINING_HISTORY",0,-1);
            saved=new AtomNetSessionR30(d.datasetHash,d.issues[split.holdoutTo-1],d.ordinals[split.holdoutTo-1],7+AtomReplayR27.eligible().length+10,train,select,Math.max(0,hold));
            try(MemoryBudgetR31.Lease work=AtomNetPublicationR31.workReservationR31(saved)){
                ResearchWriteQueueR27.Batch start=new ResearchWriteQueueR27.Batch();long now=System.currentTimeMillis();AtomNetStoreR30.dataset(start,d,now);
                try(AtomNetPublicationR31.Prepared publication=AtomNetPublicationR31.prepare(w.store(),w.ro,start,saved,now,"")){
                    stage(w,publication);publication.verifyPrepared(w.ro);RuntimeR27.submit(w,"ATOMNET_R30_INIT",publication.publicationBatch(start,saved),false,publication.id);
                    rt.atomDataR30=d;rt.publishAtomStateR31(saved,publication.model);return ResearchPoolR27.Outcome.progress(1,0);
                }
            }
        }
        ReplayDatasetR27 d=rt.atomDataR30;
        if(d==null){d=AtomNetStoreR30.dataset(w.ro,saved.datasetHash);rt.atomDataR30=d;}
        try(MemoryBudgetR31.Lease work=AtomNetPublicationR31.workReservationR31(saved)){
        final String expectedCheckpointR31=AtomNetPublicationR31.currentStateHash(w.ro);
        if(!saved.partialIssue.isEmpty()&&saved.partialInputHashR341.isEmpty())
            return recoverUnboundPartialR341(rt,w,saved,expectedCheckpointR31);
        final AtomNetProgressR33.Frame progressBeforeR33=progressFrameR33(saved);
        // Copy before computing: no failed/cancelled write can mutate the committed/published learner.
        byte[] previous=saved.encode();AtomNetSessionR30 next=AtomNetSessionR30.decode(previous,StatsR27.sha256(previous));
        next.bindReplayRolesR341();
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();long now=System.currentTimeMillis();int n=0;ModelReplayStudyR30R1.ReplayDecision replayDecision=null;
        String trainingSource="H_TRAIN|"+saved.epoch+"|"+saved.offset;
        String onlineWindow=null;
        // Formal confirmation labels never enter the optimizer. Materializing their unused
        // feature rows used to delay the first gradient without supplying a consumer.
        // Existing original holdout rows remain intact; new work stops at SELECT.
        int end=AtomReplayR27.MIN_ROWS+(next.trainCount+next.selectCount)/10;
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
            final long sourceRevision=SchemaR27.liveSourceRevision(w.ro);
            OnlineResearchWindowR30R1.Label label=OnlineResearchWindowR30R1.nextLabel(w.ro,next.datasetHash,next.completeIssue,next.partialIssue);
            if(label==null)return ResearchPoolR27.Outcome.idle(next.partialIssue.isEmpty()
                    ?"WAITING_PREREGISTERED_ONLINE_RESEARCH_INPUT":"BLOCKED_PARTIAL_ISSUE_MISSING",next.trainer.onlineSteps,-1);
            if(label.excludedNoPredraw()&&next.partialIssue.isEmpty())
                return recordUnavailableR341(w,b,sourceRevision,label,next,"VERIFIED_TERMINAL_WITHOUT_PREDRAW",now);
            if(!label.authoritative())return ResearchPoolR27.Outcome.idle(
                    "WAITING_EXACT_AUTHORITATIVE_SETTLEMENT:"+label.issue,next.trainer.onlineSteps,-1);
            long ordinal=TargetIssueResolver.sequenceOrdinalSU1(label.issue);
            label.window.require(ordinal,label.seen);
            ReplayDatasetR27 recent=StoreR27.loadDatasetThrough(w.ro,AtomReplayR27.HISTORY+1,label.issue);
            int t=recent.size()-1;
            if(t<0||!label.issue.equals(recent.issues[t])||label.seen!=recent.knownAtR31[t]
                    ||label.open!=recent.openTimeR31[t])return ResearchPoolR27.Outcome.idle("ONLINE_LABEL_SOURCE_CHANGED",next.trainer.onlineSteps,-1);
            long cutoff=label.open-LiveShadowsR27.FREEZE_MARGIN_MS;
            AtomReplayR27.VisibleFrame visible=cutoff>0?AtomReplayR27.buildAsOf(recent,t,cutoff):null;
            final String dataset=next.datasetHash,window=label.window.id;
            if(visible==null){
                if(!next.partialIssue.isEmpty())return ResearchPoolR27.Outcome.idle("BLOCKED_PARTIAL_ISSUE_PIT_INPUT_MISSING",next.trainer.onlineSteps,-1);
                return recordUnavailableR341(w,b,sourceRevision,label,next,"INSUFFICIENT_VISIBLE_CONSECUTIVE_PREDRAW_CONTEXT",now);
            }
            AtomReplayR27.Frame frame=visible.frame;
            String inputHash=HashingSU1.fields("R341_SETTLED_ATOMNET_POSITION_SET",dataset,window,
                    label.issue,label.drawHash,label.settlementHash,visible.historyHash);
            if(!next.partialIssue.isEmpty()&&!inputHash.equals(next.partialInputHashR341)) {
                return ResearchPoolR27.Outcome.idle("BLOCKED_PARTIAL_INPUT_IDENTITY:"+label.issue,next.trainer.onlineSteps,-1);
            }
            int p=next.nextPosition-1;
            if(frame.values[p]==null)return ResearchPoolR27.Outcome.idle("BLOCKED_POSITION_INPUT",next.trainer.onlineSteps,-1);
            double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[p],frame.present[p],true),p+1);
            settle(b,w,label.issue,p+1,frame.hit[p],now);
            requireLabel(b,sourceRevision,label);
            replayDecision=ModelReplayStudyR30R1.allocateReplay(w.ro,next,b);
            next.online(new AtomNetTrainerR27.Sample(x,frame.hit[p],frame.actual[p]-1,AtomNetSessionR30.eventKey(label.issue,p+1)),label.issue,ordinal,p+1,inputHash);
            trainingSource=AtomNetSessionR30.eventKey(label.issue,p+1);
            onlineWindow=window+"|"+visible.historyHash+"|"+label.drawHash+"|"+label.settlementHash;
            n=1;
        }
        AtomNetProgressR33.validate(progressBeforeR33,progressFrameR33(next),n);
        try(AtomNetPublicationR31.Prepared publication=AtomNetPublicationR31.prepare(w.store(),w.ro,b,next,System.currentTimeMillis(),expectedCheckpointR31)){
            stage(w,publication);publication.verifyPrepared(w.ro);ModelReplayStudyR30R1.recordReplayDecision(b,next,replayDecision,publication.stateHash);
            AtomNetR27.OptimizerStep optimizer=next.trainer.net.lastStep();
            if(optimizer!=null){
                String input=onlineWindow==null?optimizer.inputHash:HashingSU1.fields(optimizer.inputHash,onlineWindow);
                String role=onlineWindow==null?SplitR27.TRAIN:"ONLINE_RESEARCH";
                b.add("INSERT OR IGNORE INTO r34_atomnet_optimizer_step VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    publication.id,expectedCheckpointR31,publication.stateHash,next.datasetHash,
                    role,trainingSource,input,optimizer.adamStep,
                    optimizer.batchSize,optimizer.gradientNorm,optimizer.parameterChange,optimizer.batchLoss,now);
                ResearchProvenanceGraphR30R1.optimizerParametersR342(b,next,optimizer,publication.id,
                    expectedCheckpointR31,publication.stateHash,role,trainingSource,input,now);
                if(onlineWindow!=null)ResearchProvenanceGraphR30R1.onlineInputsR341(b,next,publication.id,
                    publication.stateHash,onlineWindow,trainingSource,input,now);
            }
            try{RuntimeR27.submit(w,"ATOMNET_R30_SLICE",publication.publicationBatch(b,next),false,publication.id);}catch(IllegalStateException changed){if(QualificationInboxR29.precondition(changed))return ResearchPoolR27.Outcome.idle("DEPENDENCY(NEURAL_SOURCE_CHANGED_PREPARED_RETRY)",saved.trainer.onlineSteps,-1);throw changed;}
            rt.publishAtomStateR31(next,publication.model);
        }
        if(forecastModelR32(rt,next)!=null)forecast(rt,w,next,StoreR27.loadDataset(w.ro,AtomReplayR27.HISTORY+64));
        return ResearchPoolR27.Outcome.progress(n,++rt.atomUnitsR30);
        }
    }
    private static ResearchPoolR27.Outcome recoverUnboundPartialR341(RuntimeR27 rt,ResearchPoolR27.Worker worker,
            AtomNetSessionR30 prior,String expected)throws Exception {
        AtomNetSessionR30 base=AtomNetStoreR30.legacyPartialBaseR341(worker.ro,prior,expected);
        if(base==null)return ResearchPoolR27.Outcome.idle("BLOCKED_LEGACY_PARTIAL_BASE_UNVERIFIED:"+prior.partialIssue,prior.trainer.onlineSteps,-1);
        base.bindReplayRolesR341();
        long now=System.currentTimeMillis();
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        batch.add("INSERT OR IGNORE INTO r29_legacy_model_checkpoint(owner,artifact_hash,artifact,trained_through,online_steps,learning_rate,last_check_logloss,archived_at,reason)"
                +" SELECT owner,state_hash,artifact,trained_through,?,?,NULL,?,'R341_LEGACY_PARTIAL_ORIGINAL_PRESERVED_BEFORE_EXACT_ANCESTOR_REPLAY'"
                +" FROM r29_model_checkpoint WHERE owner=? AND state_hash=?",prior.trainer.onlineSteps,prior.trainer.onlineLr,now,AtomNetSessionR30.OWNER,expected);
        try(AtomNetPublicationR31.Prepared publication=AtomNetPublicationR31.prepare(worker.store(),worker.ro,batch,base,now,expected)){
            stage(worker,publication);
            publication.verifyPrepared(worker.ro);
            try{RuntimeR27.submit(worker,"ATOMNET_LEGACY_PARTIAL_REPLAY_BASE",publication.publicationBatch(batch,base),false,publication.id);}
            catch(IllegalStateException stale){
                if(!QualificationInboxR29.precondition(stale))throw stale;
                return ResearchPoolR27.Outcome.idle("LEGACY_PARTIAL_HEAD_CHANGED_RETRY",prior.trainer.onlineSteps,-1);
            }
            rt.publishAtomStateR31(base,publication.model);
            return ResearchPoolR27.Outcome.progress(1,++rt.atomUnitsR30);
        }
    }
    private static ResearchPoolR27.Outcome recordUnavailableR341(ResearchPoolR27.Worker w,ResearchWriteQueueR27.Batch batch,
            long revision,OnlineResearchWindowR30R1.Label label,AtomNetSessionR30 state,String reason,long now)throws Exception {
        requireLabel(batch,revision,label);
        batch.add("INSERT OR IGNORE INTO r34_atomnet_unavailable_label VALUES(?,?,?,?,?,?,?)",
            state.datasetHash,label.issue,label.window.id,revision,label.seen,reason,now);
        try { RuntimeR27.submit(w,"ATOMNET_ONLINE_UNAVAILABLE_INPUT",batch); }
        catch(IllegalStateException stale) {
            if(!QualificationInboxR29.precondition(stale))throw stale;
            return ResearchPoolR27.Outcome.idle("ONLINE_LABEL_CHANGED_RETRY:"+label.issue,state.trainer.onlineSteps,-1);
        }
        return ResearchPoolR27.Outcome.progress(1,state.trainer.onlineSteps);
    }
    private static void requireLabel(ResearchWriteQueueR27.Batch batch,long revision,OnlineResearchWindowR30R1.Label label){
        batch.guardBefore(db->revision>=0&&SchemaR27.liveSourceRevision(db)==revision
                &&SchemaR27.liveSourceTriggersValid(db)&&ResearchInputPinR30R1.runValid(db,label.window.id)
                &&OnlineResearchWindowR30R1.matchesSettledLabel(db,label)?0:-1);
    }
    private static AtomNetProgressR33.Frame progressFrameR33(AtomNetSessionR30 s){
        return new AtomNetProgressR33.Frame(s.datasetHash,s.phase,s.materializedSamples,s.pretrainSamples,s.trainer.onlineSteps,s.epoch,s.offset,s.fillOffset,s.nextPosition,s.completeIssue,s.partialIssue);
    }
    private static void stage(ResearchPoolR27.Worker w,AtomNetPublicationR31.Prepared publication)throws InterruptedException{
        int chunk=0;for(ResearchWriteQueueR27.Batch staging:publication.stagingBatches())RuntimeR27.submit(w,"ATOMNET_R31_STAGE_"+chunk,staging,false,publication.id+"|STAGE|"+(chunk++));
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
    static ModelSurfaceProviderR30.FrozenModelRef forecastModelR32(RuntimeR27 rt,AtomNetSessionR30 state){
        RuntimeR27.AtomPublicationR30 publication=rt.atomPublicationR30;
        return publication==null||publication.state!=state?null:publication.model;
    }
    static void forecast(RuntimeR27 rt,ResearchPoolR27.Worker w,AtomNetSessionR30 state,ReplayDatasetR27 recent)throws Exception{
        // Only a model published after a complete issue may be consumed. The learner can independently be
        // training positions 1..9 or validating; its mutable network and watermark are not prediction inputs.
        ModelSurfaceProviderR30.FrozenModelRef modelRef=forecastModelR32(rt,state);
        if(modelRef==null||recent.size()<AtomReplayR27.MIN_ROWS)return;
        long targetOrdinal=recent.ordinals[recent.size()-1]+1,deadline=RuntimeR27.latestOpen(w.ro)+60_000L;
        if(modelRef.trainedThroughOrdinal>=targetOrdinal||System.currentTimeMillis()>=deadline-LiveShadowsR27.FREEZE_MARGIN_MS)return;
        String target=TargetIssueResolver.fromSequenceOrdinalSU1(targetOrdinal);
        try(Cursor c=w.ro.rawQuery("SELECT count(*) FROM r30_atomnet_forecast WHERE target_issue=?",new String[]{target})){if(c.moveToFirst()&&c.getLong(0)==10)return;}
        long revision=SchemaR27.liveSourceRevision(w.ro),schema=SchemaR27.liveSourceSchemaVersion(w.ro);
        if(revision<0||schema<0||!SchemaR27.liveSourceTriggersValid(w.ro))return;
        recent=StoreR27.loadDataset(w.ro,AtomReplayR27.HISTORY+64);
        if(recent.size()==0||recent.ordinals[recent.size()-1]+1!=targetOrdinal)return;
        ReplayDatasetR27 slot=recent.append(new ReplayDatasetR27.Row(target,targetOrdinal,new int[]{1,2,3,4,5,6,7,8,9,10}));
        AtomReplayR27.VisibleFrame visible=AtomReplayR27.buildAsOf(slot,slot.size()-1,System.currentTimeMillis());
        if(visible==null)return;
        AtomReplayR27.Frame frame=visible.frame;
        if(forecastModelR32(rt,state)!=modelRef)return; // do not mix two committed publications
        AtomNetR27 frozenNet=AtomNetR27.decode(modelRef.weights());
        boolean completeCurrent=state.consumableR31()&&modelRef.trainedThroughOrdinal==state.trainedThroughOrdinal()
                &&modelRef.weightsHash.equals(StatsR27.sha256(state.trainer.net.encode()));
        String modelHash=modelRef.modelHash;long at=System.currentTimeMillis();ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        OnlineResearchWindowR30R1.Window researchWindow=OnlineResearchWindowR30R1.find(w.ro,state.datasetHash,targetOrdinal);
        ModelSurfaceProviderR30.FrozenModelRef previous=researchWindow==null?null:ModelReplayStudyR30R1.previous(w.ro,modelRef,targetOrdinal,at);
        for(int i=0;i<10;i++){
            if(frame.values[i]==null) return;
            double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[i],frame.present[i],true),i+1);
            AtomNetR27.Out prediction=frozenNet.forward(x,null);
            long preparedAt=System.currentTimeMillis();
            ModelSurfaceProviderR30.Pit pit=new ModelSurfaceProviderR30.Pit(targetOrdinal,recent.ordinals[recent.size()-1],i+1,visible.historyHash,x,visible.lastKnownAt,preparedAt,preparedAt,deadline);
            ModelSurfaceProviderR30.Surface surface=ModelSurfaceProviderR30.predict(pit,modelRef);
            ModelReplayStudyR30R1.appendRequestedR335(w.ro,batch,researchWindow,pit,surface,previous);
            ModelSurfaceStoreR30.surface(batch,surface,i==0?modelRef:null);
            List<Object> p=new ArrayList<>();for(double value:surface.probabilities())p.add(value);
            batch.add("INSERT OR IGNORE INTO r30_atomnet_forecast VALUES(?,?,?,?,?,?,?,?)",target,i+1,modelHash,modelRef.trainedThroughOrdinal,JsonR27.write(p),prediction.pHit,at,deadline);
            if(i+1==(int)Math.floorMod(targetOrdinal,10L)+1){
                batch.add("INSERT OR IGNORE INTO r27_atomnet_pending VALUES(?,?,?,?,?)",target,"ATOMNET",prediction.pHit,at,deadline);
                // FTRL has no immutable prior reference. Never bind its partially updated draft to the old
                // AtomNet publication. It remains eligible at a complete, matching learner boundary.
                String trainedThrough=TargetIssueResolver.fromSequenceOrdinalSU1(modelRef.trainedThroughOrdinal);
                batch.add("INSERT OR IGNORE INTO r29_atomnet_forecast VALUES(?,?,?,?,?,?,?)",target,"ATOMNET",modelHash,trainedThrough,AtomNetSessionR30.ABI,at,deadline);
                if(completeCurrent){
                    batch.add("INSERT OR IGNORE INTO r27_atomnet_pending VALUES(?,?,?,?,?)",target,"FTRL",state.ftrl.predict(Arrays.copyOf(x,7)),at,deadline);
                    batch.add("INSERT OR IGNORE INTO r29_atomnet_forecast VALUES(?,?,?,?,?,?,?)",target,"FTRL",modelHash,trainedThrough,AtomNetSessionR30.ABI,at,deadline);
                }
            }
        }
        final String sourceIssue=recent.issues[recent.size()-1];
        batch.guardBefore(tx->LiveShadowsR27.sameDrawHead(tx,sourceIssue,deadline)&&System.currentTimeMillis()<deadline-LiveShadowsR27.FREEZE_MARGIN_MS&&revision==SchemaR27.liveSourceRevision(tx)&&schema==SchemaR27.liveSourceSchemaVersion(tx)&&SchemaR27.liveSourceTriggersValid(tx)?0:-1);
        batch.after=batch.before;
        try{RuntimeR27.submit(w,"ATOMNET_R30_FORECAST",batch);}catch(IllegalStateException late){if(!QualificationInboxR29.precondition(late))throw late;}
    }
}
