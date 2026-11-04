package com.ethk3.novaonepro.strategyv2;

/** A research prescription requires measured evidence and never confers live eligibility. */
public final class ResearchAdviceR30R1 {
    public enum Action { NONE, PRUNE_COMPONENT, KEEP_FOR_CONFIRMATION }
    public final String proposalId,evidenceKey,parentPolicy,componentPath,reason,analysisContractHash;
    public final Action action;
    private ResearchAdviceR30R1(ResearchContractR30R1 c,String component,ResearchGainR30R1.Estimate e,Action action,String reason){
        analysisContractHash=e.analysisContractHash;evidenceKey=c.key("PAIRED_COMPONENT_ADVICE_V1|"+analysisContractHash);parentPolicy=c.policyHash;componentPath=component;this.action=action;this.reason=reason;
        proposalId=ResearchContractR30R1.hash(evidenceKey+'|'+component+'|'+action.name());
    }
    public static ResearchAdviceR30R1 component(ResearchContractR30R1 c,String component,ResearchGainR30R1.Estimate e,long target,long preparedAt){
        if(e.loss!=ResearchGainR30R1.Loss.MULTICLASS_BRIER_HALF)throw new IllegalArgumentException("COMPONENT_ADVICE_REQUIRES_SAME_TARGET_NORMALIZED_TEN_CLASS_LOSS");
        c.requireAdvice(target,preparedAt);if(component==null||!component.matches("root(?:/[0-9]+)*"))throw new IllegalArgumentException("COMPONENT_PATH");
        if(e.fromOrdinal!=c.fromOrdinal||e.throughOrdinal!=c.throughOrdinal)throw new IllegalArgumentException("GAIN_CONTRACT_WINDOW_MISMATCH");
        // estimate compares candidate-with-component to otherwise-identical baseline-without-component.
        if(!e.supported()||e.missingIssues>0||e.incompleteFullBlocks>0)return new ResearchAdviceR30R1(c,component,e,Action.NONE,"INSUFFICIENT_OR_INCOMPLETE_PAIRED_RESEARCH");
        if(e.upper95<0)return new ResearchAdviceR30R1(c,component,e,Action.PRUNE_COMPONENT,"COMPONENT_HARMFUL_IN_REGISTERED_RESEARCH_BLOCKS");
        if(e.lower95>0)return new ResearchAdviceR30R1(c,component,e,Action.KEEP_FOR_CONFIRMATION,"RESEARCH_GAIN_ONLY_REQUIRES_NEW_INDEPENDENT_CONFIRMATION");
        return new ResearchAdviceR30R1(c,component,e,Action.NONE,"PAIRED_RESEARCH_INTERVAL_CONTAINS_ZERO");
    }
}
