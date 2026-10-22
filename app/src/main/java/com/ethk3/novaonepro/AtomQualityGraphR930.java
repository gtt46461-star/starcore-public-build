package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * R930 C10/C11 per-atom provenance, missingness, cost and quarantine graph layered over the existing
 * 240 Genesis atoms. Existing legacy matrices are referenced read-only; nothing here deletes or
 * rewrites their formulas.
 */
public final class AtomQualityGraphR930 {
    public static final String REVISION="R930_ATOM_QUALITY_GRAPH_V1";
    public static final int EXPECTED_ATOMS=240;
    public static final int PREDRAW_PRODUCTION=165;
    public static final int RESEARCH_PROXY=40;
    public static final int SETTLEMENT_ONLY=20;
    public static final int AUDIT_IDENTITY=15;
    public static final int MIN_SOFT_QUALITY_SUPPORT=120;
    private AtomQualityGraphR930(){}

    public enum State { ACTIVE,WATCH,SHADOW_ONLY,QUARANTINED }
    public static final class ValueResult {
        public final String atomId; public final double value; public final boolean present;
        public final String missingReason,maxSourceIssue,valueHash; public final long availableAtMs,costNs;
        public ValueResult(String atomId,double value,boolean present,String missingReason,String maxSourceIssue,long availableAtMs,long costNs,String valueHash){
            this.atomId=required(atomId);this.value=value;this.present=present;this.missingReason=missingReason==null?"":missingReason.trim();
            this.maxSourceIssue=maxSourceIssue==null?"":maxSourceIssue.trim();this.availableAtMs=availableAtMs;this.costNs=Math.max(0,costNs);this.valueHash=hash(valueHash);
            if(present&&!finite(value))throw new IllegalArgumentException("R930_ATOM_NONFINITE:"+atomId);
            if(!present&&this.missingReason.length()==0)throw new IllegalArgumentException("R930_ATOM_MISSING_REASON:"+atomId);
            if(present&&this.maxSourceIssue.length()==0)throw new IllegalArgumentException("R930_ATOM_SOURCE_ISSUE:"+atomId);
        }
        public void requirePredraw(String targetIssue,long drawAt){if(present)FeatureDagContractR929.requirePointInTime(maxSourceIssue,targetIssue,availableAtMs,drawAt);}
    }

    public static final class Quality {
        public final String atomId;public final State state;public final int supportN,missingN;
        public final double missingRate,harmfulDelta;public final long meanCostNs,maxCostNs;
        public final boolean deterministic,pitPass;public final String ruleVersion,evidenceHash,reason;
        public Quality(String atomId,State state,int supportN,int missingN,double harmfulDelta,long meanCostNs,long maxCostNs,
                       boolean deterministic,boolean pitPass,String ruleVersion,String evidenceHash,String reason){
            this.atomId=required(atomId);this.state=state==null?State.WATCH:state;this.supportN=Math.max(0,supportN);this.missingN=Math.max(0,missingN);
            this.missingRate=this.supportN==0?Double.NaN:Math.min(1.0d,(double)this.missingN/this.supportN);this.harmfulDelta=harmfulDelta;
            this.meanCostNs=Math.max(0,meanCostNs);this.maxCostNs=Math.max(0,maxCostNs);this.deterministic=deterministic;this.pitPass=pitPass;
            this.ruleVersion=required(ruleVersion);this.evidenceHash=hash(evidenceHash);this.reason=required(reason);
        }
    }

    /** Exactly one shared predraw atom context per issue/schema/state hash. */
    public static final class Context {
        public final String targetIssue,schemaHash,stateHash,contextHash;private final LinkedHashMap<String,ValueResult> values=new LinkedHashMap<String,ValueResult>();
        public Context(String targetIssue,String schemaHash,String stateHash){this.targetIssue=required(targetIssue);this.schemaHash=hash(schemaHash);this.stateHash=hash(stateHash);this.contextHash=V51ChecksumEngine.sha256(REVISION+'|'+this.targetIssue+'|'+this.schemaHash+'|'+this.stateHash);}
        public synchronized void put(ValueResult value){if(value==null)throw new IllegalArgumentException("R930_ATOM_VALUE_NULL");if(values.containsKey(value.atomId))throw new IllegalStateException("R930_ATOM_COMPUTED_TWICE:"+value.atomId);values.put(value.atomId,value);}
        public synchronized ValueResult get(String atomId){return values.get(atomId);}
        public synchronized Map<String,ValueResult> snapshot(){return Collections.unmodifiableMap(new LinkedHashMap<String,ValueResult>(values));}
    }

    public static void assertRegistry(){
        GenesisSchemaRegistryR915.assertContract();List<GenesisSchemaRegistryR915.AtomSpec> atoms=GenesisSchemaRegistryR915.atoms();
        if(atoms.size()!=EXPECTED_ATOMS||GenesisSchemaRegistryR915.productionScoreEligibleCount()!=PREDRAW_PRODUCTION
                ||GenesisSchemaRegistryR915.researchProxyCount()!=RESEARCH_PROXY
                ||GenesisSchemaRegistryR915.settlementEvidenceCount()!=SETTLEMENT_ONLY
                ||GenesisSchemaRegistryR915.auditIdentityCount()!=AUDIT_IDENTITY)
            throw new IllegalStateException("R930_ATOM_TIER_CARDINALITY");
    }

    public static State evaluateState(GenesisSchemaRegistryR915.AtomSpec spec,Quality prior,boolean pitPass,
                                      boolean deterministic,int supportN,int missingN,double harmfulDelta){
        if(spec==null)throw new IllegalArgumentException("R930_ATOM_SPEC_NULL");
        if(!pitPass||!deterministic)return State.QUARANTINED;
        if(!spec.productionScoreEligible())return State.SHADOW_ONLY;
        if(supportN<MIN_SOFT_QUALITY_SUPPORT)return State.WATCH;
        double missingRate=supportN==0?1.0d:(double)missingN/supportN;
        if(missingRate>0.25d)return State.QUARANTINED;
        if(finite(harmfulDelta)&&harmfulDelta<-0.03d)return State.QUARANTINED;
        if(missingRate>0.10d||(finite(harmfulDelta)&&harmfulDelta<0.0d))return State.WATCH;
        return State.ACTIVE;
    }

    public static boolean mayEnterNewCandidate(GenesisSchemaRegistryR915.AtomSpec spec,Quality quality){
        return spec!=null&&spec.productionScoreEligible()&&quality!=null&&quality.state==State.ACTIVE&&quality.pitPass&&quality.deterministic;
    }
    public static boolean mayContributeProduction(GenesisSchemaRegistryR915.AtomSpec spec,Quality quality){return mayEnterNewCandidate(spec,quality);}

    public static Quality observe(DbStore store,GenesisSchemaRegistryR915.AtomSpec spec,ValueResult value,String targetIssue,long drawAt,
                                  boolean deterministic,double ablationDelta,String evidenceHash){
        if(store==null||spec==null||value==null)throw new IllegalArgumentException("R930_ATOM_OBSERVE_INPUT");
        boolean pit=true;try{value.requirePredraw(targetIssue,drawAt);}catch(Throwable leak){pit=false;}
        String prefix="r930_atom_"+V51ChecksumEngine.sha256(spec.id).substring(0,16);
        int n=parseInt(store.getState(prefix+"_n","0"),0)+1,missing=parseInt(store.getState(prefix+"_missing","0"),0)+(value.present?0:1);
        long totalCost=parseLong(store.getState(prefix+"_cost_sum","0"),0)+value.costNs,maxCost=Math.max(parseLong(store.getState(prefix+"_cost_max","0"),0),value.costNs);
        double priorHarm=parseDouble(store.getState(prefix+"_harm_sum","0"),0),harmSum=priorHarm+(finite(ablationDelta)?ablationDelta:0.0d);
        int harmN=parseInt(store.getState(prefix+"_harm_n","0"),0)+(finite(ablationDelta)?1:0);double harm=harmN==0?Double.NaN:harmSum/harmN;
        State state=evaluateState(spec,null,pit,deterministic,n,missing,harm);
        LinkedHashMap<String,String> s=new LinkedHashMap<String,String>();s.put(prefix+"_n",String.valueOf(n));s.put(prefix+"_missing",String.valueOf(missing));s.put(prefix+"_cost_sum",String.valueOf(totalCost));s.put(prefix+"_cost_max",String.valueOf(maxCost));s.put(prefix+"_harm_sum",String.valueOf(harmSum));s.put(prefix+"_harm_n",String.valueOf(harmN));s.put(prefix+"_state",state.name());s.put(prefix+"_last_issue",required(targetIssue));s.put(prefix+"_evidence",hash(evidenceHash));s.put(prefix+"_rule",REVISION);store.setStates(s);
        return new Quality(spec.id,state,n,missing,harm,totalCost/Math.max(1,n),maxCost,deterministic,pit,REVISION,evidenceHash,reason(state,pit,deterministic,n,missing,harm));
    }

    /** Recovery is explicit: only a new audit evidence hash can leave quarantine. */
    public static State recover(DbStore store,GenesisSchemaRegistryR915.AtomSpec spec,String newAuditEvidenceHash,boolean pitPass,boolean deterministic,int supportN,int missingN,double harmfulDelta){
        if(store==null||spec==null)throw new IllegalArgumentException("R930_ATOM_RECOVER_INPUT");String prefix="r930_atom_"+V51ChecksumEngine.sha256(spec.id).substring(0,16);String old=store.getState(prefix+"_evidence","");String newer=hash(newAuditEvidenceHash);if(newer.equals(old))throw new IllegalStateException("R930_QUARANTINE_RECOVERY_REQUIRES_NEW_EVIDENCE");State next=evaluateState(spec,null,pitPass,deterministic,supportN,missingN,harmfulDelta);store.setState(prefix+"_state",next.name());store.setState(prefix+"_evidence",newer);return next;
    }

    /** Existing fixed matrices are an immutable dependency namespace, not quarantine targets. */
    public static String legacyFixedMatrixBinding(String name,String sourceHash){return V51ChecksumEngine.sha256("legacy_fixed|"+required(name)+'|'+hash(sourceHash)+"|READ_ONLY_FORMULA_PRESERVED");}
    public static boolean productionPostdrawCallsAreZero(Iterable<GenesisSchemaRegistryR915.AtomSpec> consumed){if(consumed==null)return true;for(GenesisSchemaRegistryR915.AtomSpec a:consumed)if(a!=null&&a.settlementEvidence())return false;return true;}

    private static String reason(State s,boolean pit,boolean deterministic,int n,int missing,double harm){if(!pit)return "PIT_HARD_FAIL";if(!deterministic)return "NONDETERMINISTIC_HARD_FAIL";if(s==State.SHADOW_ONLY)return "NON_PRODUCTION_SEMANTIC_TIER";if(n<MIN_SOFT_QUALITY_SUPPORT)return "WAIT_SUPPORT_"+n;if(n>0&&(double)missing/n>0.25)return "MISSINGNESS_HIGH";if(finite(harm)&&harm<-0.03)return "HARMFUL_ABLATION";return s.name();}
    private static int parseInt(String x,int d){try{return Integer.parseInt(x);}catch(Throwable e){return d;}}private static long parseLong(String x,long d){try{return Long.parseLong(x);}catch(Throwable e){return d;}}private static double parseDouble(String x,double d){try{return Double.parseDouble(x);}catch(Throwable e){return d;}}
    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R930_ATOM_REQUIRED");return v;}private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R930_ATOM_HASH");return v;}private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
}
