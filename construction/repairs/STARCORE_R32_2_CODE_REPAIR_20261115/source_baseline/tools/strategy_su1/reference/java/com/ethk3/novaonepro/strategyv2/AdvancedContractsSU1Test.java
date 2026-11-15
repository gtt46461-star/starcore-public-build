package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Independent rebuilds and numerical derivatives exercise the production implementation. */
public final class AdvancedContractsSU1Test {
    private static int checks;
    private static void check(boolean condition,String detail){checks++;if(!condition)throw new AssertionError(detail);}
    private static void rejects(Runnable fn){try{fn.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError("invalid contract accepted");}
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(600000,100000000,null);}
    private static int[][] draws(int n){Random random=new Random(8731);int[][] rows=new int[n][10];for(int t=0;t<n;t++){for(int p=0;p<10;p++)rows[t][p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),x=rows[t][p];rows[t][p]=rows[t][q];rows[t][q]=x;}}return rows;}
    public static void main(String[] args){
        int[][] d=draws(1240);String root=HashingSU1.text("immutable source");RelationSpecSU1 spec=RelationSpecSU1.xt(3);
        SparseConditionalCountsSU1 fast=new SparseConditionalCountsSU1(d,spec,root);
        for(int t=0;t<1205;t++){
            SparseConditionalCountsSU1.Estimate result=fast.at(t,budget());
            if(t<4||t%83==0||t>=1199){
                SparseConditionalCountsSU1.Estimate full=new SparseConditionalCountsSU1(d,spec,root).at(t,budget());
                check(full.payload().equals(result.payload()),"incremental/full disagreement at "+t);
                check(Arrays.deepEquals(ProgramTreeSU1.Experts.crossTransition(d,t,3,1000),ProgramTreeSU1.Experts.znorm(result.probabilities())),"original XT numeric semantics changed "+t);
            }
        }
        check(fast.incrementalUpdates()==1204&&fast.fullRebuilds()==1,"sliding updater did not run");
        check(fast.at(400,budget()).payload().equals(new SparseConditionalCountsSU1(d,spec,root).at(400,budget()).payload()),"backward query contaminated counts");
        RelationSpecSU1 joint=new RelationSpecSU1(2,1,1000,new RelationSpecSU1.Condition(1,2,RelationSpecSU1.Slice.ODD),new RelationSpecSU1.Condition(7,1,RelationSpecSU1.Slice.BIG));
        SparseConditionalCountsSU1.Estimate estimate=new SparseConditionalCountsSU1(d,joint,root).at(1200,budget());
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){
            int expected=0;for(int u=200;u<1200;u++)if(joint.key(d,u,p)==joint.key(d,1200,p)&&d[u][p]==n+1)expected++;
            check(expected==estimate.count(p,n),"joint condition denominator/count mismatch");
        }
        SparseConditionalCountsSU1 stopped=new SparseConditionalCountsSU1(d,spec,root);String prior=stopped.at(1000,budget()).payload();
        try{stopped.at(1001,new BudgetTokenSU1(10000,10,()->true));throw new AssertionError("cancel ignored");}catch(BudgetTokenSU1.Yield expected){checks++;}
        check(prior.equals(stopped.at(1000,budget()).payload()),"cancel published partial update");
        RelationTypeSU1 a=new RelationTypeSU1(RelationTypeSU1.Unit.PROBABILITY,RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE,"NEXT_POSITION_NUMBER",RelationTypeSU1.Axis.POSITION,RelationTypeSU1.Axis.NUMBER);
        rejects(()->a.requireCompatible(new RelationTypeSU1(RelationTypeSU1.Unit.PROBABILITY,RelationTypeSU1.Knowledge.STATISTICAL_ESTIMATE,"NEXT_POSITION_NUMBER",RelationTypeSU1.Axis.NUMBER,RelationTypeSU1.Axis.POSITION)));
        VariableRefSU1 position=new VariableRefSU1(VariableRefSU1.Domain.POSITION,VariableRefSU1.Binding.BOUND,VariableRefSU1.Identity.RELABEL_EQUIVARIANT,"p",0,1);
        rejects(()->position.require(VariableRefSU1.Domain.NUMBER));
        check(position.canonical(0).equals(new VariableRefSU1(VariableRefSU1.Domain.POSITION,VariableRefSU1.Binding.BOUND,VariableRefSU1.Identity.RELABEL_EQUIVARIANT,"renamed",0,1).canonical(0)),"variable alpha-renaming changed semantics");
        ProgramContractSU1 compiled=ProgramContractSU1.compile(ProgramTextSU1.parse("IF(ODD(1,2),ADD(XT(3),DEEP(4,8,17)),REL(SUM,3,2,7))"));
        check(compiled.historyWindow>=1001&&compiled.stateBytes>0&&compiled.operationUnits<=1000000,"static program budget missing");
        check(compiled.manifest.contains("OBSERVED_FACT")&&compiled.manifest.contains("STATISTICAL_ESTIMATE")&&compiled.manifest.contains("RESEARCH_HYPOTHESIS"),"epistemic leaf classification incomplete");
        RelationalStateInference.Weights w=RelationalTrainerSU1.initial(3,71);int[][] history=Arrays.copyOfRange(d,0,5);int[] label=d[5];
        TrainingObjectiveSU1 objective=new TrainingObjectiveSU1(.13,.04,.03,.00001);
        RelationalTrainerSU1.Gradient gradient=RelationalTrainerSU1.gradient(w,history,label,budget(),objective);
        double[][][] groups={w.e,w.u,w.r,w.wx,w.wm,w.wh,w.wo};double[][][] derivs={gradient.e,gradient.u,gradient.r,gradient.wx,gradient.wm,gradient.wh,gradient.wo};
        for(int g=0;g<groups.length;g++)for(int x=0;x<groups[g].length;x++)for(int y=0;y<groups[g][x].length;y++){
            double original=groups[g][x][y],epsilon=1e-5;
            groups[g][x][y]=original+epsilon;double plus=RelationalTrainerSU1.gradient(w,history,label,budget(),objective).loss;
            groups[g][x][y]=original-epsilon;double minus=RelationalTrainerSU1.gradient(w,history,label,budget(),objective).loss;groups[g][x][y]=original;
            check(Math.abs((plus-minus)/(2*epsilon)-derivs[g][x][y])<2e-7,"joint objective gradient mismatch group="+g);
        }
        check(Math.abs(gradient.loss-gradient.crossEntropyLoss-gradient.brierLoss-gradient.smoothnessLoss-gradient.l2Loss-gradient.resourceLoss)<1e-12,"loss terms not auditable");
        TrainingCheckpointSU1 checkpoint=new TrainingCheckpointSU1(w,5,0,.03,1,objective,root,"202610210005",HashingSU1.text("no parent"));
        TrainingCheckpointSU1 restored=TrainingCheckpointSU1.decode(checkpoint.wire);
        TrainingCheckpointSU1 next=checkpoint.continueOne(history,label,root,"202610210006",budget());
        TrainingCheckpointSU1 resumed=restored.continueOne(history,label,root,"202610210006",budget());
        check(next.wire.equals(resumed.wire)&&next.parentHash.equals(checkpoint.hash)&&next.steps==1,"SGD continuation is not exact");
        check(!next.weightsHash.equals(checkpoint.weightsHash),"training weights did not change");
        rejects(()->TrainingCheckpointSU1.decode(checkpoint.wire.replace("SGD_NO_MOMENTUM_V1","ADAM")));
        String program="MODEL("+next.hash+",5)";
        double[][] scores=ProgramTreeSU1.parse(program).score(new ProgramTreeSU1.Context(d,10).withFitted(Collections.singletonMap(next.hash,new ProgramTreeSU1.Fitted(next.weights,7))));
        double[][] direct=RelationalStateInference.predict(next.weights,Arrays.copyOfRange(d,5,10));
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)check(scores[p][n]==ProgramTreeSU1.clean(Math.log(Math.max(1e-12,direct[p][n]))),"exported trained weights not consumed");
        rejects(()->ProgramTreeSU1.parse(program).score(new ProgramTreeSU1.Context(d,6).withFitted(Collections.singletonMap(next.hash,new ProgramTreeSU1.Fitted(next.weights,7)))));
        MainlineChoiceSU1.Candidate parent=new MainlineChoiceSU1.Candidate("parent","parent-cert",null,1,100),child=new MainlineChoiceSU1.Candidate("child","child-cert","parent",2,100),unrelated=new MainlineChoiceSU1.Candidate("other","other-cert",null,3,100);
        check(MainlineChoiceSU1.choose("parent","parent-cert",Arrays.asList(parent,unrelated),id->null,false)==null,"unrelated branch replaced a valid parent");
        MainlineChoiceSU1.Choice promote=MainlineChoiceSU1.choose("parent","parent-cert",Arrays.asList(parent,child),id->null,false);check(promote.operation.equals("PROMOTE")&&promote.candidate==child,"qualified direct child not promoted");
        check(MainlineChoiceSU1.choose("parent","parent-cert",Arrays.asList(parent,child),id->null,true)==null,"user rollback hold ignored");
        MainlineChoiceSU1.Choice fallback=MainlineChoiceSU1.choose("child","expired-child",Arrays.asList(parent),id->id.equals("child")?"parent":null,false);check(fallback.operation.equals("FALLBACK")&&fallback.candidate==parent,"expired child did not fall back");
        check(MainlineChoiceSU1.choose("child","expired",Collections.emptyList(),id->null,false).operation.equals("REVOKE"),"expired head retained authority");
        ResearchDirectorPolicySU1 director=new ResearchDirectorPolicySU1();director.context("BASE_RATE|FULL_HISTORY");director.record("GRAMMAR","mutate",.20,.25,100,false);
        try{java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();director.write(new java.io.DataOutputStream(bytes));ResearchDirectorPolicySU1 restoredDirector=ResearchDirectorPolicySU1.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));check(restoredDirector.contexts.size()==1&&restoredDirector.arms.get("GRAMMAR")[0]==1,"contextual research memory not restored");}catch(java.io.IOException error){throw new AssertionError(error);}
        check(ProgramTextSU1.semanticCanonical("ADD(XT(3),EXPERT(0))").equals(ProgramTextSU1.semanticCanonical("ADD(EXPERT(0),XT(3))")),"proven commutative rewrite not deduplicated");
        int[][] future=draws(1240);for(int i=1100;i<future.length;i++){int x=future[i][0];future[i][0]=future[i][1];future[i][1]=x;}
        String fusion="FUSE(EXPERT(0),XT(3),60)";double[][] fusedA=ProgramTreeSU1.parse(fusion).score(new ProgramTreeSU1.Context(d,1100)),fusedB=ProgramTreeSU1.parse(fusion).score(new ProgramTreeSU1.Context(future,1100));check(Arrays.deepEquals(fusedA,fusedB),"fusion read future labels");
        System.out.println("SU1_ADVANCED_CONTRACTS_PASS checks="+checks);
    }
}
