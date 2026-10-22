package com.ethk3.novaonepro;

import java.util.EnumSet;

/** Typed method design. A method is not a long descriptive string: each innovation axis is independently auditable. */
public final class ResearchMethodPlanR906 {
    public final String representation,feature,learner,updateRule,objective,calibration,risk,falsifier,parentAssetIds,geneticOperator;
    public final int generation;
    public final boolean runtimeExecutable;
    public ResearchMethodPlanR906(String representation,String feature,String learner,String updateRule,String objective,
            String calibration,String risk,String falsifier,String parentAssetIds,int generation,String geneticOperator,boolean runtimeExecutable){
        this.representation=req(representation,"REPRESENTATION");this.feature=req(feature,"FEATURE");this.learner=req(learner,"LEARNER");
        this.updateRule=req(updateRule,"UPDATE");this.objective=req(objective,"OBJECTIVE");this.calibration=req(calibration,"CALIBRATION");
        this.risk=req(risk,"RISK");this.falsifier=req(falsifier,"FALSIFIER");this.parentAssetIds=safe(parentAssetIds);this.generation=Math.max(1,generation);
        this.geneticOperator=req(geneticOperator,"OPERATOR");this.runtimeExecutable=runtimeExecutable; if(this.representation.equals("TYPED_EXPRESSION_R932"))TypedExpressionR932.compile(this.feature);else ResearchFeatureGrammarR906.requireAllowed(this.feature); validateNoLeakage();
    }
    public String canonical(){return "R906_METHOD|rep="+representation+"|feature="+feature+"|learner="+learner+"|update="+updateRule+"|objective="+objective+"|cal="+calibration+"|risk="+risk+"|falsifier="+falsifier+"|parents="+parentAssetIds+"|generation="+generation+"|op="+geneticOperator+"|exec="+(runtimeExecutable?1:0);}

    public static ResearchMethodPlanR906 parseCanonical(String canonical){
        String x=safe(canonical);
        if(!x.startsWith("R906_METHOD|"))throw new IllegalArgumentException("R906_METHOD_CANONICAL_PREFIX");
        java.util.HashMap<String,String> m=new java.util.HashMap<String,String>();
        String[] parts=x.split("\\|");
        for(int i=1;i<parts.length;i++){int eq=parts[i].indexOf('=');if(eq>0)m.put(parts[i].substring(0,eq),parts[i].substring(eq+1));}
        int generation=1;try{generation=Integer.parseInt(req(m.get("generation"),"GENERATION"));}catch(NumberFormatException e){throw new IllegalArgumentException("R906_METHOD_GENERATION_INVALID");}
        boolean exec="1".equals(m.get("exec"));
        return new ResearchMethodPlanR906(req(m.get("rep"),"REPRESENTATION"),req(m.get("feature"),"FEATURE"),req(m.get("learner"),"LEARNER"),
                req(m.get("update"),"UPDATE"),req(m.get("objective"),"OBJECTIVE"),req(m.get("cal"),"CALIBRATION"),req(m.get("risk"),"RISK"),
                req(m.get("falsifier"),"FALSIFIER"),safe(m.get("parents")),generation,req(m.get("op"),"OPERATOR"),exec);
    }

        public EnumSet<ResearchAssetContractR906.Dimension> changedFrom(ResearchMethodPlanR906 p){
        EnumSet<ResearchAssetContractR906.Dimension>x=EnumSet.noneOf(ResearchAssetContractR906.Dimension.class);if(p==null)return EnumSet.allOf(ResearchAssetContractR906.Dimension.class);
        if(!representation.equals(p.representation))x.add(ResearchAssetContractR906.Dimension.REPRESENTATION);if(!feature.equals(p.feature))x.add(ResearchAssetContractR906.Dimension.FEATURE);
        if(!learner.equals(p.learner))x.add(ResearchAssetContractR906.Dimension.LEARNER);if(!updateRule.equals(p.updateRule))x.add(ResearchAssetContractR906.Dimension.UPDATE);
        if(!objective.equals(p.objective))x.add(ResearchAssetContractR906.Dimension.OBJECTIVE);if(!calibration.equals(p.calibration))x.add(ResearchAssetContractR906.Dimension.CALIBRATION);
        if(!risk.equals(p.risk))x.add(ResearchAssetContractR906.Dimension.RISK);if(!falsifier.equals(p.falsifier))x.add(ResearchAssetContractR906.Dimension.FALSIFIER);return x;
    }
    public void validateNoLeakage(){String x=canonical().toLowerCase(java.util.Locale.US);String[]forbidden={"actual_number","hit_label","future_draw","post_draw","settled_result","target_actual"};for(String f:forbidden)if(x.contains(f))throw new IllegalArgumentException("R906_FORBIDDEN_FEATURE:"+f);if(x.contains("decisiongate")||x.contains("real_bet")||x.contains("create_bet"))throw new SecurityException("R906_RESEARCH_OWNER_VIOLATION");}
    private static String req(String x,String name){String s=safe(x);if(s.length()==0)throw new IllegalArgumentException("R906_METHOD_"+name+"_EMPTY");return s;}
    private static String safe(String x){return x==null?"":x.trim();}
}
