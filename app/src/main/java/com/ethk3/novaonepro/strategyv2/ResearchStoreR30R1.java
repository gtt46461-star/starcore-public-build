package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Typed facts are written by the original artifact transaction, never by a scheduler heartbeat. */
public final class ResearchStoreR30R1 {
    private ResearchStoreR30R1(){}
    public static final class Facts {
        public final long cursor,latest; public final Long pending,oldest,completed; public final String identity;
        public Facts(long cursor,long latest,Long pending,Long oldest,Long completed,String identity){
            if(cursor<0||latest<cursor||pending!=null&&pending<0||identity==null||identity.isEmpty())throw new IllegalArgumentException("INVALID_TYPED_COMMIT_FACTS");
            if(pending!=null&&pending==0&&oldest!=null)throw new IllegalArgumentException("EMPTY_PENDING_HAS_OLDEST");
            this.cursor=cursor;this.latest=latest;this.pending=pending;this.oldest=oldest;this.completed=completed;this.identity=identity;
        }
    }
    public static boolean ready(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r30r1_analysis_run','r30r1_research_measure','r30r1_proposal','r30r1_consume','r30r1_chart_cache','r30r1_task_progress')",null)){return c.moveToFirst()&&c.getLong(0)==6;}}
    public static void appendOwnedProgress(ResearchWriteQueueR27.Batch batch,long calendar,Facts facts){RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();if(claim!=null)appendProgress(batch,claim.task,claim.owner,claim.fence,calendar,facts);}
    public static void appendProgress(ResearchWriteQueueR27.Batch batch,String task,String session,long fence,long calendar,Facts facts){
        // Progress first, then the original final source/deadline guard; both are in the same transaction.
        final com.ethk3.novaonepro.DatabaseCoordinator.WriteCallable<Integer> original=batch.after;
        batch.after=db->{commitProgress(db,task,session,fence,calendar<0?calendar(db):calendar,facts);return original==null?0:original.call(db);};
    }
    public static void commitProgress(SQLiteDatabase db,String task,String session,long fence,long calendar,Facts facts){
        if(!db.inTransaction())throw new IllegalStateException("TYPED_PROGRESS_REQUIRES_ARTIFACT_TRANSACTION");
        ResearchTaskRegistryR30R1.Entry entry=ResearchTaskRegistryR30R1.require(task);
        long now=System.currentTimeMillis();
        try(Cursor c=db.rawQuery("SELECT artifact_identity FROM r30r1_task_progress WHERE task_id=?",new String[]{task})){if(c.moveToFirst()&&facts.identity.equals(c.getString(0)))return;}
        db.execSQL("INSERT OR REPLACE INTO r30r1_task_progress(task_id,owner_version,work_unit,work_cursor,known_latest,pending,oldest_pending_ordinal,last_commit_clock_ordinal,completed_input_ordinal,fence,session_id,updated_at,artifact_identity) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{task,ResearchTaskRegistryR30R1.VERSION,entry.unit.name(),facts.cursor,facts.latest,facts.pending,facts.oldest,calendar<0?null:calendar,facts.completed,fence,session,now,facts.identity});
        if(task.startsWith("r27-"))db.execSQL("UPDATE r26_task SET cursor=?,last_progress=?,steps=steps+1,first_progress_at=CASE WHEN first_progress_at=0 THEN ? ELSE first_progress_at END WHERE task_id=? AND owner=? AND fence=?",new Object[]{facts.cursor,now,now,task,session,fence});
    }
    /** Exact owner adapters. Missing adapters stay UNKNOWN, instead of treating a receipt as a product. */
    public static void afterOwnedArtifact(SQLiteDatabase db,String operation,RuntimeCoordinatorR26.Claim claim){
        if(claim==null||!ResearchTaskRegistryR30R1.ENTRIES.containsKey(claim.task)||operation.contains("TASK_RECEIPT")||operation.contains("THREAD_CPU")||operation.contains("CLAIM")||operation.contains("WRITE_WAIT")||!ready(db))return;
        String task=claim.task;String query=null;
        if(task.equals("r27-live")&&operation.equals("R27_LIVE_SETTLE"))query="SELECT min(through_ordinal),group_concat(policy_hash||':'||through_ordinal,'|') FROM (SELECT policy_hash,through_ordinal FROM r27_live_shadow WHERE status IN ('ACTIVE','OBSERVE') ORDER BY policy_hash)";
        else if(task.equals("r27-atoms")&&operation.equals("R27_ATOM_SCORES"))query="SELECT coalesce(sum(n_select),0),group_concat(atom_id||':'||dataset_hash||':'||computed_at,'|') FROM (SELECT atom_id,dataset_hash,computed_at,n_select FROM r27_atom_score ORDER BY atom_id)";
        else if(task.equals("r27-abi")&&(operation.equals("R27_SEMANTIC_ABI")||operation.equals("R27_LEGACY_ABI_CONTINUITY")))query="SELECT count(*),group_concat(policy_hash||':'||fingerprint,'|') FROM (SELECT policy_hash,fingerprint FROM r27_semantic_fingerprint ORDER BY policy_hash,build)";
        else if(task.equals("r27-calibration")&&operation.equals("R27_CALIBRATION"))query="SELECT count(*),group_concat(policy_hash||':'||state||':'||updated_at,'|') FROM (SELECT policy_hash,state,updated_at FROM r27_calibration ORDER BY policy_hash)";
        else if(task.equals("r27-legacy-export")&&operation.startsWith("R27_LEGACY"))query="SELECT count(*),group_concat(source_id||':'||status||':'||policy_hash,'|') FROM (SELECT source_id,status,policy_hash FROM r27_legacy_export ORDER BY source_id)";

        else if(task.equals("r27-evolution")&&operation.startsWith("R27_EVOLUTION_STEP_"))query="SELECT generation*10+step,dataset_hash||'|'||rng_state||'|'||generation||'|'||step FROM r27_evolution_cursor WHERE loop='"+RuntimeR27.LOOP+"'";
        else if(task.equals("r27-replay-dataset")&&operation.equals("R27_REPLAY_DATASET"))query="SELECT to_issue,dataset_hash FROM r27_replay_dataset ORDER BY to_issue DESC LIMIT 1";
        else if(task.equals("live-model-training")&&operation.equals("R26_LIVE_LEARN_COMMIT"))query="SELECT min(through_ordinal),group_concat(policy_hash||':'||checkpoint_hash,'|') FROM (SELECT policy_hash,through_ordinal,checkpoint_hash FROM r26_live_model WHERE status='ACTIVE' ORDER BY policy_hash)";
        else if(task.equals("legacy-replay"))query="SELECT through_ordinal,checkpoint_hash FROM r26_legacy_runtime WHERE singleton=1";
        else if(task.startsWith("expert-"))query="SELECT coalesce(sum(cursor),0),group_concat(state_hash,'|') FROM (SELECT cursor,state_hash FROM r26_expert_experiment WHERE expert_id='"+task.substring(7)+"' ORDER BY experiment_id)";
        else if(task.equals("retention")&&operation.startsWith("R26_RETENTION_PHASE_")){RetentionR26.Checkpoint cp=RetentionR26.checkpoint(db);long cursor=cp.workCursor;commitProgress(db,task,claim.owner,claim.fence,calendar(db),new Facts(cursor,cursor,null,null,null,HashingSU1.text(cp.original)));return;}
        
        if(query==null)return;
        try(Cursor c=db.rawQuery(query,null)){
            if(!c.moveToFirst()||c.isNull(0)||c.isNull(1)||c.getLong(0)<0)return;long cursor=c.getLong(0),at=calendar(db);Long completed=null,pending=null,oldest=null;
            if(task.equals("r27-replay-dataset")){cursor=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(c.getString(0));completed=cursor;pending=at<0?null:Math.max(0,at-cursor-RuntimeR27.REFRESH_NEW_ROWS+1);}
            else if(task.equals("live-model-training")||task.equals("legacy-replay")||task.equals("r27-live")){completed=cursor;pending=at<0?null:Math.max(0,at-cursor);if(pending!=null&&pending>0)oldest=cursor+1;}
            else if(task.equals("r27-atomnet")){AtomNetSessionR30 state=AtomNetStoreR30.restore(db);if(state==null)return;cursor=state.pretrainSamples+state.trainer.onlineSteps;completed=state.ready()?state.trainedThroughOrdinal():null;pending=null;}
            if(task.equals("r27-evolution"))pending=1L;
            long latest=(task.equals("live-model-training")||task.equals("legacy-replay")||task.equals("r27-live")||task.equals("r27-replay-dataset"))&&at>=0?Math.max(cursor,at):cursor;
            commitProgress(db,task,claim.owner,claim.fence,at,new Facts(cursor,latest,pending,oldest,completed,HashingSU1.text(c.getString(1))));
        }
    }
    static long calendar(SQLiteDatabase db){String issue=RuntimeR27.latestIssue(db);return issue==null?-1:com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue);}
    public static ResearchWriteQueueR27.Batch paired(ResearchContractR30R1 c,ResearchGainR30R1.Estimate e,
            ResearchAdviceR30R1 advice,String measuredCounterfactualJson,String measuredCounterfactualHash,long now){
        if(!advice.evidenceKey.equals(c.key("PAIRED_COMPONENT_ADVICE_V1|"+e.analysisContractHash)))throw new IllegalStateException("ANALYSIS_RECEIPT_IDENTITY");
        PolicySpecR27 without=PolicySpecR27.parse(measuredCounterfactualJson);
        if(!without.policyHash().equals(ResearchContractR30R1.sha(measuredCounterfactualHash)))throw new IllegalStateException("ANALYSIS_CANDIDATE_HASH");
        String run=c.key("PAIRED_COMPONENT_ANALYSIS_V1|"+e.analysisContractHash+'|'+advice.componentPath);Map<String,Object> summary=new LinkedHashMap<>();
        summary.put("sourceContract",PairedReplayRunnerR30R1.contractJson(c));summary.put("counterfactualHash",measuredCounterfactualHash);summary.put("componentPath",advice.componentPath);summary.put("status",e.status);summary.put("observedCalendarIssues",(long)e.observedCalendarIssues);summary.put("evaluatedIssues",(long)e.evaluatedIssues);summary.put("completeBlocks",(long)e.completeBlocks);summary.put("incompleteBlocks",(long)e.incompleteBlocks);summary.put("incompleteFullBlocks",(long)e.incompleteFullBlocks);summary.put("excludedTailIssues",(long)e.excludedTailIssues);summary.put("missingIssues",(long)e.missingIssues);summary.put("mean",e.meanBlockGain);summary.put("lower95",e.lower95);summary.put("upper95",e.upper95);
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->{if(!SchemaR27.liveSourceTriggersValid(db))throw new IllegalStateException("RESEARCH_SOURCE_TRIGGER_CONFLICT");ResearchInputPinR30R1.require(db,c.datasetHash,c.sourceRevision);return 0;});
        b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,c.datasetHash,c.policyHash,c.role.name(),c.sourceCommit,c.sourceRevision,c.fromOrdinal,c.throughOrdinal,e.analysisContractHash,"COMMITTED",JsonR27.write(summary),now);
        b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,advice.componentPath,"ATOM_PAIRED_GAIN",c.policyHash,"REGISTERED_CALENDAR_BLOCKS",c.fromOrdinal,c.throughOrdinal,e.evaluatedIssues,e.meanBlockGain,e.lower95,e.upper95,JsonR27.write(summary));
        if(advice.action==ResearchAdviceR30R1.Action.PRUNE_COMPONENT){Map<String,Object> p=new LinkedHashMap<>();p.put("componentPath",advice.componentPath);p.put("candidate",measuredCounterfactualJson);p.put("candidateHash",measuredCounterfactualHash);p.put("evidenceKey",advice.evidenceKey);p.put("analysisContractHash",e.analysisContractHash);
            String payload=JsonR27.write(p);b.add("INSERT OR IGNORE INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)",advice.proposalId,run,c.policyHash,advice.action.name(),payload,ResearchContractR30R1.hash(payload),"READY",advice.reason,now);}
        if(e.supported()&&e.upper95<0){Map<String,Object> failure=new LinkedHashMap<>(summary);failure.put("mechanismHash",ResearchContractR30R1.hash(c.policyHash+'|'+advice.componentPath));failure.put("inputHash",c.inputHash);failure.put("sourceAbi",c.abi);failure.put("role",c.role.name());failure.put("budgetVersion","R30_REPAIR_RESEARCH_BUDGET1");failure.put("retestAllowedForNewInputs",true);failure.put("familyPermanentlyDisabled",false);b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,"FAILURE_MEMORY:"+advice.componentPath,"FAILURE_MEMORY",c.policyHash,"REGISTERED_CALENDAR_BLOCKS",c.fromOrdinal,c.throughOrdinal,e.evaluatedIssues,e.meanBlockGain,e.lower95,e.upper95,JsonR27.write(failure));}
        Map<String,Object> frame=new LinkedHashMap<>(summary);frame.put("viewId","R06");frame.put("runId",run);frame.put("role",c.role.name());frame.put("parentHash",c.policyHash);frame.put("componentPath",advice.componentPath);frame.put("proposalId",advice.proposalId);frame.put("advice",advice.action.name());frame.put("reason",advice.reason);frame.put("researchOnly",true);
        String payload=JsonR27.write(frame),hash=ResearchContractR30R1.hash(payload);
        b.add("INSERT OR IGNORE INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)","R06",hash,run,payload,hash,now);return b;
    }
    public static ResearchWriteQueueR27.Batch paired(PairedReplayRunnerR30R1 runner,long now){
        if(!runner.complete())throw new IllegalStateException("INCOMPLETE_STUDY_CANNOT_PUBLISH");
        ResearchGainR30R1 g=runner.gain();ResearchGainR30R1.Estimate e=g.estimate();ResearchAdviceR30R1 advice=runner.advice();ResearchContractR30R1 c=runner.contract;
        ResearchWriteQueueR27.Batch b=paired(c,e,advice,runner.without.fullJson(),runner.without.policyHash(),now);
        String run=c.key("PAIRED_COMPONENT_ANALYSIS_V1|"+e.analysisContractHash+'|'+advice.componentPath);
        Map<String,Object> details=new LinkedHashMap<>();details.put("runId",run);details.put("checkpoint",runner.snapshot());details.put("blocks",g.blocks());details.put("sourceContract",PairedReplayRunnerR30R1.contractJson(c));details.put("mean",e.meanBlockGain);details.put("lower95",e.lower95);details.put("upper95",e.upper95);details.put("status",e.status);details.put("counterfactualHash",runner.without.policyHash());details.put("componentPath",runner.path);details.put("parent",runner.parent.fullJson());details.put("supported",e.supported());details.put("missingIssues",e.missingIssues);details.put("completeBlocks",e.completeBlocks);details.put("excludedTailIssues",e.excludedTailIssues);details.put("evaluatedIssues",e.evaluatedIssues);
        b.add("UPDATE r30r1_analysis_run SET summary_json=? WHERE run_id=?",JsonR27.write(details),run);
        b.add("UPDATE r30r1_research_measure SET payload_json=? WHERE run_id=? AND measure_key=?",JsonR27.write(details),run,runner.path);
        int count=g.blocks().size();if(count>0&&count<=64){List<String> cols=new ArrayList<>();double[][] values=new double[1][count];long[][] support=new long[1][count];for(int i=0;i<count;i++){Map<String,Object> block=g.blocks().get(i);cols.add("block:"+block.get("fromOrdinal"));Object mean=block.get("meanGain");if(mean instanceof Number){values[0][i]=((Number)mean).doubleValue();support[0][i]=((Number)block.get("presentIssues")).longValue();}}
            MatrixTensorR30R1.Tensor tensor=new MatrixTensorR30R1.Tensor("COUNTERFACTUAL",c.role.name(),c.datasetHash,c.policyHash,runnerIssue(c.throughOrdinal),runnerIssue(c.throughOrdinal+1),c.sourceRevision,Collections.singletonList(runner.path),cols,values,support,details);MatrixTensorR30R1.append(b,tensor,now);
            Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("status","PRODUCED");receipt.put("matrixId","R930:COUNTERFACTUAL");receipt.put("matrixHash",tensor.hash);receipt.put("runId",run);receipt.put("role",c.role.name());receipt.put("sourceRevision",c.sourceRevision);receipt.put("exactMeasuredChildHash",runner.without.policyHash());receipt.put("qualityEffective",null);
            b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",MatrixResearchR30R1.PANEL,"COUNTERFACTUAL",JsonR27.write(receipt),now);
        }return b;
    }
    static String runnerIssue(long ordinal){return com.ethk3.novaonepro.TargetIssueResolver.fromSequenceOrdinalSU1(ordinal);}

    static boolean currentSource(String sourceCommit,String summaryJson){if(!com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26.equals(sourceCommit))return false;Object parsed=JsonR27.parse(summaryJson);if(!(parsed instanceof Map))return false;Object contract=((Map<?,?>)parsed).get("sourceContract");return contract instanceof Map&&FrozenConfirmationR30.currentSemantics().equals(((Map<?,?>)contract).get("abi"));}
    static boolean stale(Throwable failure){for(Throwable t=failure;t!=null&&t.getCause()!=t;t=t.getCause())if(t instanceof StaleResearch)return true;return false;}
    public static final class StaleResearch extends IllegalStateException {StaleResearch(){super("RESEARCH_CONSUME_STALE");}}
    public static final class Ready {public final String proposal;public final PolicySpecR27 child;Ready(String proposal,PolicySpecR27 child){this.proposal=proposal;this.child=child;}}
    @SuppressWarnings("unchecked") public static List<Ready> ready(SQLiteDatabase db,EvalContextR27 ctx,int generation,int limit){
        List<Ready> out=new ArrayList<>();long revision=ResearchInputPinR30R1.revision(db,ctx.data.datasetHash);
        try(Cursor c=db.rawQuery("SELECT p.proposal_id,p.payload_json,p.payload_hash,r.summary_json FROM r30r1_proposal p JOIN r30r1_analysis_run r ON r.run_id=p.run_id WHERE p.state='READY' AND p.kind='PRUNE_COMPONENT' AND r.role='SELECT' AND r.status='COMMITTED' AND r.dataset_hash=? AND r.source_revision=? AND r.source_commit=? ORDER BY p.created_at,p.proposal_id LIMIT "+Math.min(4,Math.max(0,limit)),new String[]{ctx.data.datasetHash,String.valueOf(revision),com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26})){
            while(c.moveToNext()){
                if(!StatsR27.sha256(c.getString(1)).equals(c.getString(2)))throw new IllegalStateException("PROPOSAL_PAYLOAD_HASH");Map<String,Object> p=(Map<String,Object>)JsonR27.parse(c.getString(1)),summary=(Map<String,Object>)JsonR27.parse(c.getString(3));ResearchContractR30R1 source=PairedReplayRunnerR30R1.contractParse((Map<?,?>)summary.get("sourceContract"));if(!FrozenConfirmationR30.currentSemantics().equals(source.abi))continue;source.requireAdvice(ctx.data.ordinals[ctx.data.size()-1]+1,System.currentTimeMillis());
                PolicySpecR27 parent=PolicySpecR27.parse((String)summary.get("parent")),child=PolicySpecR27.parse((String)p.get("candidate"));String path=(String)p.get("componentPath");
                if(!source.policyHash.equals(parent.policyHash())||!child.policyHash().equals(p.get("candidateHash"))||!child.policyHash().equals(DirectedResearchPolicyR30R1.withoutChild(parent,path,generation).policyHash()))throw new IllegalStateException("UNMEASURED_COUNTERFACTUAL_SUBSTITUTION");
                out.add(new Ready(c.getString(0),child.withMeta(new PolicySpecR27.Meta(child.meta==null?"I09":child.meta.idea,"MEASURED_PRUNE",Collections.singletonList(parent.policyHash()),generation,0,"PROPOSAL:"+c.getString(0)))));
            }
        }return out;
    }
    /** Invoke on the SAME batch that writes the child policy, pending generation and evolution cursor. */
    public static void consume(ResearchWriteQueueR27.Batch batch,String proposal,String child,int generation,String consumer,long now){
        ResearchContractR30R1.sha(proposal);ResearchContractR30R1.sha(child);
        batch.guardBefore(db->{try(Cursor c=db.rawQuery("SELECT p.state,p.payload_json,p.payload_hash,r.role,r.source_revision,r.source_commit,r.summary_json,r.dataset_hash FROM r30r1_proposal p JOIN r30r1_analysis_run r ON r.run_id=p.run_id WHERE p.proposal_id=?",new String[]{proposal})){if(!c.moveToFirst()||!"READY".equals(c.getString(0))||ResearchInputPinR30R1.revision(db,c.getString(7))!=c.getLong(4))throw new StaleResearch();if(!com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26.equals(c.getString(5)))throw new StaleResearch();Object sourceSummary=JsonR27.parse(c.getString(6));if(!(sourceSummary instanceof Map)||!(((Map<?,?>)sourceSummary).get("sourceContract") instanceof Map)||!FrozenConfirmationR30.currentSemantics().equals(((Map<?,?>)((Map<?,?>)sourceSummary).get("sourceContract")).get("abi")))throw new StaleResearch();if(!"SELECT".equals(c.getString(3))||!StatsR27.sha256(c.getString(1)).equals(c.getString(2)))return -1;Object p=JsonR27.parse(c.getString(1));return p instanceof Map&&child.equals(((Map<?,?>)p).get("candidateHash"))?0:-1;}});
        String receipt=ResearchContractR30R1.hash(proposal+'|'+child+'|'+generation+'|'+consumer);
        batch.add("INSERT INTO r30r1_consume VALUES(?,?,?,?,?,?)",proposal,child,generation,consumer,receipt,now);
        batch.add("UPDATE r30r1_proposal SET state='CONSUMED' WHERE proposal_id=? AND state='READY'",proposal);
        final com.ethk3.novaonepro.DatabaseCoordinator.WriteCallable<Integer> original=batch.after;
        batch.after=db->{try(Cursor c=db.rawQuery("SELECT spec_json,state FROM r27_policy WHERE policy_hash=?",new String[]{child})){if(!c.moveToFirst()||!"BORN".equals(c.getString(1))||!PolicySpecR27.parse(c.getString(0)).policyHash().equals(child))throw new IllegalStateException("RESEARCH_CONSUMPTION_REQUIRES_ACTUAL_BORN_CHILD");}return original==null?0:original.call(db);};
    }
}
