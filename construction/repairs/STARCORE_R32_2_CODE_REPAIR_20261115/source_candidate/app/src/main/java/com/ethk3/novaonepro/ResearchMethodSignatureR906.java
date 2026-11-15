package com.ethk3.novaonepro;
/** Stable semantic signature used to merge duplicates even when names/parameters differ. */
public final class ResearchMethodSignatureR906 {
    private ResearchMethodSignatureR906(){}
    public static String of(ResearchMethodPlanR906 p){if(p==null)throw new IllegalArgumentException("R906_METHOD_PLAN_NULL");return V51ChecksumEngine.sha256("R906_METHOD_SIGNATURE|"+norm(p.representation)+'|'+(p.representation.equals("TYPED_EXPRESSION_R932")?TypedExpressionR932.compile(p.feature).canonical:norm(p.feature))+'|'+norm(p.learner)+'|'+norm(p.updateRule)+'|'+norm(p.objective)+'|'+norm(p.calibration)+'|'+norm(p.risk)+'|'+norm(p.falsifier));}
    private static String norm(String x){return x==null?"":x.trim().toUpperCase(java.util.Locale.US).replaceAll("\\s+","_");}
}
