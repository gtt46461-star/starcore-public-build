package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.AblationRepairCreationR930.CounterfactualRow;
import java.util.List;

/** Computational intervention evidence is not mechanism causal proof or certificate authority. */
public final class CausalValidationEngineR952 {
    private CausalValidationEngineR952(){}
    /** Caller-supplied aggregates are association, not verified evidence. */
    public static Result evaluate(String hypothesis,int windows,double delta,String confounders,int negatives){
        if(windows<0||negatives<0||negatives>windows||!Double.isFinite(delta))
            throw new IllegalArgumentException("R958_CAUSAL_INVALID_AGGREGATE");
        String evidence=V51ChecksumEngine.sha256("R958_ASSOCIATION_ONLY|"+hypothesis+'|'+windows+'|'+delta+'|'+confounders+'|'+negatives);
        return new Result(CausalEdgeR952.Status.CORRELATIONAL,evidence,windows,negatives,confounders,
                "UNVERIFIED_AGGREGATE_NOT_CAUSAL",delta);
    }
    /** Rows are produced by the existing paired re-execution owner, including failed interventions. */
    public static Result evaluateMeasured(String hypothesisHash,List<List<CounterfactualRow>> windows){
        if(hypothesisHash==null||!hypothesisHash.matches("[0-9a-f]{64}")||windows==null||windows.isEmpty())
            throw new IllegalArgumentException("R958_CAUSAL_MEASURED_INPUT");
        StringBuilder evidence=new StringBuilder("R958_COMPUTATIONAL_INTERVENTION|").append(hypothesisHash);
        String last="",candidate="",intervention="",confound="";int negative=0,count=0;double sum=0;
        for(List<CounterfactualRow> window:windows){
            if(window==null||window.isEmpty())throw new IllegalArgumentException("R958_CAUSAL_EMPTY_WINDOW");
            double gain=0;
            for(CounterfactualRow row:window){
                if(row==null||row.targetIssue.compareTo(last)<=0||!row.baseline.selected||!row.counterfactual.selected)
                    throw new IllegalArgumentException("R958_CAUSAL_OVERLAP_OR_SELECTION");
                if(candidate.isEmpty()){candidate=row.candidateHash;intervention=row.intervention.interventionHash;confound=row.confoundPolicyHash;}
                if(!candidate.equals(row.candidateHash)||!intervention.equals(row.intervention.interventionHash)
                        ||!confound.equals(row.confoundPolicyHash)||!row.baseline.inputHash.equals(row.counterfactual.inputHash)
                        ||row.baseline.executionHash.equals(row.counterfactual.executionHash))
                    throw new IllegalArgumentException("R958_CAUSAL_MIXED_OR_NOT_RECOMPUTED");
                double improvement=row.baseline.brier()-row.counterfactual.brier();
                if(!Double.isFinite(improvement))throw new IllegalArgumentException("R958_CAUSAL_NONFINITE_LOSS");
                gain+=improvement;sum+=improvement;count++;last=row.targetIssue;evidence.append('|').append(row.rowHash);
            }
            if(gain<=0)negative++;
        }
        int n=windows.size();CausalEdgeR952.Status status=n>=5&&negative==0?CausalEdgeR952.Status.INTERVENTION_SUPPORTED
                :negative==n?CausalEdgeR952.Status.REFUTED:CausalEdgeR952.Status.CORRELATIONAL;
        return new Result(status,V51ChecksumEngine.sha256(evidence.toString()),n,negative,confound,
                "COMPUTATIONAL_REEXECUTION_NOT_MECHANISM_CAUSAL_NOT_FUTURE_NOT_REAL",sum/count);
    }
    public static final class Result{
        public final CausalEdgeR952.Status status;public final String evidenceHash,confounderNotes,scope;
        public final int supportingWindows,negativeWindows;public final double measuredMeanGain;
        Result(CausalEdgeR952.Status s,String h,int w,int n,String c,String scope,double gain){status=s;evidenceHash=h;supportingWindows=w;negativeWindows=n;confounderNotes=c;this.scope=scope;measuredMeanGain=gain;}
    }
}
