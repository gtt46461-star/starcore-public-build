package com.ethk3.novaonepro.strategyv2;

/** Complete H_SELECT analytical computation. Scheduling and the existing atomic writer remain external owners. */
public final class PairedComponentStudyR30R1 {
    public static final class Product {
        public final PolicySpecR27 counterfactual;public final ResearchGainR30R1.Estimate gain;public final ResearchAdviceR30R1 advice;
        Product(PolicySpecR27 p,ResearchGainR30R1.Estimate e,ResearchAdviceR30R1 a){counterfactual=p;gain=e;advice=a;}
    }
    private PairedComponentStudyR30R1(){}
    public static Product evaluate(ResearchContractR30R1 contract,PolicySpecR27 parent,String path,
            EvalContextR27 context,double frozenTemperature,int generation,int blockPeriods,long target,long preparedAt){
        contract.requireAdvice(target,preparedAt);
        if(contract.role!=ResearchContractR30R1.Role.SELECT||!contract.policyHash.equals(parent.policyHash())||!contract.datasetHash.equals(context.data.datasetHash))throw new IllegalArgumentException("STUDY_ROLE_OR_DATASET");
        int from=context.split.selectFrom,to=context.split.selectTo;
        if(from>=to||contract.fromOrdinal!=context.data.ordinals[from]||contract.throughOrdinal!=context.data.ordinals[to-1])throw new IllegalArgumentException("STUDY_REGISTERED_WINDOW");
        PolicySpecR27 without=DirectedResearchPolicyR30R1.withoutChild(parent,path,generation);
        ReplayEvaluatorR27.Trace with=ReplayEvaluatorR27.selectTraceWithProbabilities(parent,context,frozenTemperature);
        ReplayEvaluatorR27.Trace removed=ReplayEvaluatorR27.selectTraceWithProbabilities(without,context,frozenTemperature);
        ResearchGainR30R1 gain=new ResearchGainR30R1(contract.fromOrdinal,contract.throughOrdinal,blockPeriods,4,200);
        // Multiclass Brier pairs compare the same outcome even when the two policies choose different K3 sets.
        for(int t=from;t<to;t++){
            if(with.skipped[t]||removed.skipped[t]||with.probabilityRows[t]==null||removed.probabilityRows[t]==null)continue;
            double a=0,b=0;int actual=context.data.draws[t][parent.position-1]-1;
            for(int k=0;k<10;k++){double y=k==actual?1:0;a+=Math.pow(with.probabilityRows[t][k]-y,2);b+=Math.pow(removed.probabilityRows[t][k]-y,2);}
            // Divide by two: normalized 10-class Brier is bounded in [0,1]. Candidate=with component.
            gain.add(context.data.ordinals[t],b/2,a/2);
        }
        ResearchGainR30R1.Estimate e=gain.estimate();return new Product(without,e,ResearchAdviceR30R1.component(contract,path,e,target,preparedAt));
    }
}
