package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** One existing research writer commits the analytical result, proposal and cache frame together. */
public final class ResearchStoreR30R1 {
    private ResearchStoreR30R1(){}
    public static ResearchWriteQueueR27.Batch paired(ResearchContractR30R1 c,ResearchGainR30R1.Estimate e,
            ResearchAdviceR30R1 advice,String measuredCounterfactualJson,String measuredCounterfactualHash,long now){
        if(!advice.evidenceKey.equals(c.key("PAIRED_COMPONENT_ADVICE_V1|"+e.analysisContractHash)))throw new IllegalStateException("ANALYSIS_RECEIPT_IDENTITY");
        PolicySpecR27 without=PolicySpecR27.parse(measuredCounterfactualJson);
        if(!without.policyHash().equals(ResearchContractR30R1.sha(measuredCounterfactualHash)))throw new IllegalStateException("ANALYSIS_CANDIDATE_HASH");
        String run=c.key("PAIRED_COMPONENT_ANALYSIS_V1|"+e.analysisContractHash+'|'+advice.componentPath);Map<String,Object> summary=new LinkedHashMap<>();
        summary.put("status",e.status);summary.put("observedCalendarIssues",(long)e.observedCalendarIssues);summary.put("evaluatedIssues",(long)e.evaluatedIssues);summary.put("completeBlocks",(long)e.completeBlocks);summary.put("incompleteBlocks",(long)e.incompleteBlocks);summary.put("incompleteFullBlocks",(long)e.incompleteFullBlocks);summary.put("excludedTailIssues",(long)e.excludedTailIssues);summary.put("missingIssues",(long)e.missingIssues);summary.put("mean",e.meanBlockGain);summary.put("lower95",e.lower95);summary.put("upper95",e.upper95);
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->SchemaR27.liveSourceRevision(db)==c.sourceRevision&&SchemaR27.liveSourceTriggersValid(db)?0:-1);
        b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,c.datasetHash,c.policyHash,c.role.name(),c.sourceCommit,c.sourceRevision,c.fromOrdinal,c.throughOrdinal,e.analysisContractHash,"COMMITTED",JsonR27.write(summary),now);
        b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,advice.componentPath,"ATOM_PAIRED_GAIN",c.policyHash,"REGISTERED_CALENDAR_BLOCKS",c.fromOrdinal,c.throughOrdinal,e.evaluatedIssues,e.meanBlockGain,e.lower95,e.upper95,JsonR27.write(summary));
        if(advice.action==ResearchAdviceR30R1.Action.PRUNE_COMPONENT){Map<String,Object> p=new LinkedHashMap<>();p.put("componentPath",advice.componentPath);p.put("candidate",measuredCounterfactualJson);p.put("candidateHash",measuredCounterfactualHash);p.put("evidenceKey",advice.evidenceKey);
            String payload=JsonR27.write(p);b.add("INSERT OR IGNORE INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)",advice.proposalId,run,c.policyHash,advice.action.name(),payload,ResearchContractR30R1.hash(payload),"READY",advice.reason,now);}
        Map<String,Object> frame=new LinkedHashMap<>(summary);frame.put("viewId","R06");frame.put("runId",run);frame.put("role",c.role.name());frame.put("parentHash",c.policyHash);frame.put("componentPath",advice.componentPath);frame.put("proposalId",advice.proposalId);frame.put("advice",advice.action.name());frame.put("reason",advice.reason);frame.put("researchOnly",true);
        String payload=JsonR27.write(frame),hash=ResearchContractR30R1.hash(payload);
        b.add("INSERT OR IGNORE INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)","R06",hash,run,payload,hash,now);return b;
    }
    /** Invoke on the SAME batch that writes the child policy, pending generation and evolution cursor. */
    public static void consume(ResearchWriteQueueR27.Batch batch,String proposal,String child,int generation,String consumer,long now){
        ResearchContractR30R1.sha(proposal);ResearchContractR30R1.sha(child);
        batch.guardBefore(db->{try(Cursor c=db.rawQuery("SELECT state FROM r30r1_proposal WHERE proposal_id=?",new String[]{proposal})){return c.moveToFirst()&&"READY".equals(c.getString(0))?0:-1;}});
        String receipt=ResearchContractR30R1.hash(proposal+'|'+child+'|'+generation+'|'+consumer);
        batch.add("INSERT INTO r30r1_consume VALUES(?,?,?,?,?,?)",proposal,child,generation,consumer,receipt,now);
        batch.add("UPDATE r30r1_proposal SET state='CONSUMED' WHERE proposal_id=? AND state='READY'",proposal);
    }
}
