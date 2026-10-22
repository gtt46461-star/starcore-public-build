package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

public final class CausalEdgeR952 {
    public enum Status{HYPOTHESIS,CORRELATIONAL,VALIDATED,INTERVENTION_SUPPORTED,REFUTED,RETIRED}
    public final String source,target,relationType,temporalDirection,hypothesisHash,evidenceHash,edgeHash;public final double confidence;public final Status status;
    public CausalEdgeR952(String s,String t,String relation,String direction,String hypothesis,String evidence,double c,Status st){source=req(s);target=req(t);relationType=req(relation);temporalDirection=req(direction);hypothesisHash=req(hypothesis);evidenceHash=evidence==null?"":evidence;if(!Double.isFinite(c))throw new IllegalArgumentException("R958_CAUSAL_CONFIDENCE_NONFINITE");confidence=Math.max(0d,Math.min(1d,c));status=st==null?Status.HYPOTHESIS:st;if(source.equals(target))throw new IllegalArgumentException("R952_CAUSAL_SELF_EDGE");if(status==Status.VALIDATED)throw new IllegalArgumentException("R958_MECHANISM_CAUSAL_EVIDENCE_NOT_AVAILABLE");if(status==Status.INTERVENTION_SUPPORTED&&!evidenceHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R952_CAUSAL_VALIDATION_EVIDENCE");edgeHash=V51ChecksumEngine.sha256("EDGE1|"+source+'|'+target+'|'+relationType+'|'+temporalDirection+'|'+hypothesisHash);}
    private static String req(String v){if(v==null||v.trim().length()==0)throw new IllegalArgumentException("R952_CAUSAL_FIELD");return v.trim();}
}
