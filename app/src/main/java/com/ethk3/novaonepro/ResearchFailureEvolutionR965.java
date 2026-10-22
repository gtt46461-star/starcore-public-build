package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

/** Pre-certificate research hypotheses from settled, proof-bound predictions. No promotion authority. */
public final class ResearchFailureEvolutionR965 {
    static final String KIND="PREDRAW_FAILURE_REPAIR";
    static final int MIN_LABELS=12;
    private ResearchFailureEvolutionR965(){}
    static final class Plan {
        final AlgorithmGenomeR908 parent;
        final String evidence,lastIssue;
        final int labels,hits,maxMiss;
        final AlgorithmGenomeR908.MutationIntent intent;
        Plan(AlgorithmGenomeR908 g,String e,String last,int n,int h,int miss,int early,int late){
            parent=g;evidence=e;lastIssue=last;labels=n;hits=h;maxMiss=miss;
            intent=miss>=5?AlgorithmGenomeR908.MutationIntent.LONG_MISS_MEMORY:
                    late<early?AlgorithmGenomeR908.MutationIntent.BLOCK_STABILITY:
                    AlgorithmGenomeR908.MutationIntent.REGIME_ROUTER;
        }
        AlgorithmGenomeR908 create(long seed){return parent.mutateForIntent(seed,parent.generation+1,intent);}
        void describe(JSONObject p)throws JSONException {
            p.put("research_failure_labels",labels);p.put("research_failure_hits",hits);p.put("research_max_miss",maxMiss);
            p.put("research_failure_evidence",evidence);p.put("research_failure_last_issue",lastIssue);
            p.put("mutation_intent",intent.name());p.put("minimum_research_labels",MIN_LABELS);
            p.put("evidence_role","HISTORICAL_FAILURE_DIAGNOSIS_ACROSS_ORIGINAL_CODE_IDENTITIES_NOT_CERTIFICATE_OR_CAUSAL_PROOF");
            p.put("child_future_n",0);p.put("parent_retention","SUBJECT_TO_BOUNDED_RESOURCE_REPLACEMENT_NOT_QUALITY_VERDICT");
        }
    }

    static Plan choose(SQLiteDatabase db,String latest) {
        // One paid algorithm attempt per draw is enforced by the creation transaction.
        // A parent may try again only after twelve additional independently settled predictions.
        String last=read(db,"r965_failure_next_parent");
        java.util.ArrayList<String> candidates=new java.util.ArrayList<String>();
        try(Cursor c=db.rawQuery("SELECT g.genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g JOIN "+ResearchLifecycleR932.TABLE+" l ON l.candidate_key='algorithm:'||g.genome_id AND l.structural_hash=g.structural_hash"
                +" WHERE g.future_n>=12 AND g.future_hit*1.0/g.future_n<"+CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE+" AND l.state IN ('NURSERY','OBSERVING')"
                +" AND g.status NOT LIKE 'PRUNED%' AND g.status NOT LIKE 'QUARANTINED%' AND g.status NOT IN ('RETIRED','ARCHIVED')"
                +" ORDER BY CASE WHEN g.genome_id>? THEN 0 ELSE 1 END,g.genome_id LIMIT 64",new String[]{last})) {while(c.moveToNext())candidates.add(c.getString(0));}
        for(String spec:candidates){
            ResearchWorkBudgetR927.checkpoint();AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(spec);
            db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES('r965_failure_next_parent',?,?)",new Object[]{g.genomeId,System.currentTimeMillis()});
            String previous="";
            try(Cursor c=db.rawQuery("SELECT created_issue FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE parent_key=? AND kind=? AND state='CREATED' ORDER BY created_issue DESC LIMIT 1",new String[]{"algorithm:"+g.genomeId,KIND})){if(c.moveToFirst())previous=c.getString(0);}
            Plan plan=diagnose(db,g,latest,previous);
            if(plan!=null)return plan;
        }
        return null;
    }

    static Plan diagnose(SQLiteDatabase db,AlgorithmGenomeR908 g,String latest) {return diagnose(db,g,latest,"");}
    private static Plan diagnose(SQLiteDatabase db,AlgorithmGenomeR908 g,String latest,String after) {
        int n=0,hits=0,run=0,max=0,early=0,late=0;
        String last="",previous="";
        String observations="(SELECT 'algorithm:'||p.genome_id AS candidate_key,p.target_issue,? AS structure_hash,p.hit_label AS hit,p.record_hash AS prediction_hash,p.position,p.n1,p.n2,p.n3 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" p WHERE p.genome_id=? AND p.hit_label IN (0,1) UNION ALL SELECT o.candidate_key,o.target_issue,o.structure_hash,o.hit,o.prediction_hash,o.position,o.n1,o.n2,o.n3 FROM "+CandidateCalibrationR932.OBS+" o WHERE o.candidate_key=? AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" p WHERE p.genome_id=? AND p.target_issue=o.target_issue AND p.hit_label IN (0,1)))";
        StringBuilder evidence=new StringBuilder("R965_RESEARCH_FAILURE|"+g.structuralHash);
        try(Cursor c=db.rawQuery("SELECT o.target_issue,o.hit,o.prediction_hash,e.payload,e.proof_hash,e.base_issue,e.frozen_at,o.position,o.n1,o.n2,o.n3 FROM "+observations+" o JOIN "+CandidateExecutionAuditR932.TABLE+" e ON e.candidate_key=o.candidate_key AND e.target_issue=o.target_issue AND e.prediction_hash=o.prediction_hash AND e.structural_hash=o.structure_hash"
                +" WHERE o.candidate_key=? AND o.structure_hash=? AND o.hit IN (0,1) AND o.target_issue<=? AND o.target_issue>? AND e.action_count=1200 AND e.max_error<=0.000000000001"
                +" AND NOT EXISTS(SELECT 1 FROM "+AlgorithmProofRecoveryR948.QUARANTINE+" q WHERE q.genome_id=? AND q.invalid_issue=o.target_issue) ORDER BY o.target_issue DESC LIMIT 120",
                new String[]{g.structuralHash,g.genomeId,"algorithm:"+g.genomeId,g.genomeId,"algorithm:"+g.genomeId,g.structuralHash,latest,after,g.genomeId})) {
            while(c.moveToNext()) {
                ResearchWorkBudgetR927.checkpoint();String issue=c.getString(0);
                if(issue.equals(previous))continue;previous=issue;
                if(!V51ChecksumEngine.sha256(c.getString(3)).equals(c.getString(4))||c.getLong(6)<=0
                        ||!TargetIssueResolver.exact(c.getString(5),issue).valid)continue;
                try {
                    JSONObject proof=new JSONObject(c.getString(3));
                    if(!proof.getString("candidate_key").equals("algorithm:"+g.genomeId)
                            ||!proof.getString("structural_hash").equals(g.structuralHash)
                            ||!proof.getString("prediction_hash").equals(c.getString(2))
                            ||!proof.getString("target_issue").equals(issue)
                            ||!proof.getString("base_issue").equals(c.getString(5))
                            ||proof.getInt("action_count")!=1200||!CandidateExecutionAuditR932.REVISION.equals(proof.getString("revision")))continue;
                    int ordinal=proof.getInt("selected_ordinal");
                    int expected=K3ActionSpaceEngine.globalOrdinal(c.getInt(7),c.getInt(8),c.getInt(9),c.getInt(10));
                    if(ordinal!=expected)continue;
                }catch(JSONException|IllegalArgumentException invalid){continue;}
                if(n==0)last=issue;
                int h=c.getInt(1);n++;hits+=h;run=h==0?run+1:0;max=Math.max(max,run);
                if(n<=6)late+=h;else if(n<=12)early+=h;
                evidence.append('|').append(issue).append(':').append(c.getString(4)).append(':').append(h);
            }
        }
        if(n<MIN_LABELS||hits*1.0/n>=CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE)return null;
        return new Plan(g,V51ChecksumEngine.sha256(evidence.toString()),last,n,hits,max,early,late);
    }
    private static String read(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):"";}}
    public static String report(DbStore store) {
        StringBuilder out=new StringBuilder("\nR965_R908_EVOLUTION|minimumDiagnosticLabels=12|poorObservedRateBelow=0.34|repairActiveCap=64|replacement=RESOURCE_RETIREMENT|parentRetry=12_NEW_PROOF_BOUND_LABELS|generationCap=NONE|newChildFutureN=0|diagnosticIsNotPromotion=1");
        try {
            out.append("\nR966_POPULATION|active=").append(ResearchPopulationR966.active(store.readable())).append("|capacity=64|retirement=RESOURCE_NOT_QUALITY|generationCap=NONE");
            try(Cursor c=store.readable().rawQuery("SELECT generation,COUNT(*),SUM(CASE WHEN future_n>0 THEN 1 ELSE 0 END),SUM(state_updates) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" GROUP BY generation ORDER BY generation",null)) {
                while(c.moveToNext())out.append("\nALGORITHM_GENERATION|generation=").append(c.getInt(0)).append("|programs=").append(c.getInt(1)).append("|withLiveLabels=").append(c.getInt(2)).append("|numericLiveUpdates=").append(c.getLong(3));
            }
            try(Cursor c=store.readable().rawQuery("SELECT kind,state,COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT+" GROUP BY kind,state",null)) {
                while(c.moveToNext())out.append("\nCREATION_ATTEMPTS|kind=").append(c.getString(0)).append("|state=").append(c.getString(1)).append("|count=").append(c.getInt(2));
            }
            try(Cursor c=store.readable().rawQuery("SELECT created_issue,parent_key,child_key,state,reason,plan_json FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE kind=? ORDER BY created_issue DESC LIMIT 16",new String[]{KIND})) {
                while(c.moveToNext())out.append("\nFAILURE_REPAIR|issue=").append(c.getString(0)).append("|parent=").append(c.getString(1)).append("|child=").append(c.getString(2)).append("|state=").append(c.getString(3)).append("|reason=").append(c.getString(4)).append("|plan=").append(c.getString(5));
            }
            out.append("\nR965_ABLATION_QUEUE|code=").append(store.getState("r965_ablation_source","NOT_RUN")).append("|cursor=").append(store.getState("r932_ablation_cursor","")).append("|current=").append(store.getState("r932_ablation_current","")).append("|module=").append(store.getState("r932_ablation_module","0"));
            try(Cursor c=store.readable().rawQuery("SELECT state_key,state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key LIKE 'r965_research_pruning_%' ORDER BY updated_at DESC LIMIT 32",null)) {
                while(c.moveToNext())out.append("\nRESEARCH_RETIREMENT_CONTRACT|key=").append(c.getString(0)).append("|contract=").append(c.getString(1));
            }
        }catch(RuntimeException error){out.append("\nEVOLUTION_REPORT_UNAVAILABLE|").append(error.getClass().getSimpleName());}
        return out.toString();
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 out=new com.ethk3.novaonepro.ReportTextCP12(report, "\nR965_R908_EVOLUTION|minimumDiagnosticLabels=12|poorObservedRateBelow=0.34|repairActiveCap=64|replacement=RESOURCE_RETIREMENT|parentRetry=12_NEW_PROOF_BOUND_LABELS|generationCap=NONE|newChildFutureN=0|diagnosticIsNotPromotion=1");
        try {
            out.append("\nR966_POPULATION|active=").append(ResearchPopulationR966.activeSnapshotCP4(report, store.readable())).append("|capacity=64|retirement=RESOURCE_NOT_QUALITY|generationCap=NONE");
            try(Cursor c=report.select("SELECT generation,COUNT(*),SUM(CASE WHEN future_n>0 THEN 1 ELSE 0 END),SUM(state_updates) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" GROUP BY generation ORDER BY generation",null)) {
                while(c.moveToNext())out.append("\nALGORITHM_GENERATION|generation=").append(c.getInt(0)).append("|programs=").append(c.getInt(1)).append("|withLiveLabels=").append(c.getInt(2)).append("|numericLiveUpdates=").append(c.getLong(3));
            }
            try(Cursor c=report.select("SELECT kind,state,COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT+" GROUP BY kind,state",null)) {
                while(c.moveToNext())out.append("\nCREATION_ATTEMPTS|kind=").append(c.getString(0)).append("|state=").append(c.getString(1)).append("|count=").append(c.getInt(2));
            }
            try(Cursor c=report.select("SELECT created_issue,parent_key,child_key,state,reason,plan_json FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE kind=? ORDER BY created_issue DESC LIMIT 16",new String[]{KIND})) {
                while(c.moveToNext())out.append("\nFAILURE_REPAIR|issue=").append(c.getString(0)).append("|parent=").append(c.getString(1)).append("|child=").append(c.getString(2)).append("|state=").append(c.getString(3)).append("|reason=").append(c.getString(4)).append("|plan=").append(c.getString(5));
            }
            out.append("\nR965_ABLATION_QUEUE|code=").append(store.getState("r965_ablation_source","NOT_RUN")).append("|cursor=").append(store.getState("r932_ablation_cursor","")).append("|current=").append(store.getState("r932_ablation_current","")).append("|module=").append(store.getState("r932_ablation_module","0"));
            try(Cursor c=report.select("SELECT state_key,state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key LIKE 'r965_research_pruning_%' ORDER BY updated_at DESC LIMIT 32",null)) {
                while(c.moveToNext())out.append("\nRESEARCH_RETIREMENT_CONTRACT|key=").append(c.getString(0)).append("|contract=").append(c.getString(1));
            }
        }catch(RuntimeException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
        return out.toString();
    }
}
