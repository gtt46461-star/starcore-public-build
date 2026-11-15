package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Independent tensor reconstruction/finite differences and causal mechanism removal. */
public final class R26TensorMechanismTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static byte[] save(OnlineModelsSU2.Model model)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new DataOutputStream(bytes));return bytes.toByteArray();}
    static int[] permutation(Random random){int[] d=new int[10];for(int i=0;i<10;i++)d[i]=i+1;for(int i=9;i>0;i--){int j=random.nextInt(i+1),value=d[i];d[i]=d[j];d[j]=value;}return d;}
    static int[] mode(boolean first){int[] d=new int[10];for(int p=0;p<10;p++)d[p]=first?p+1:10-p;return d;}
    static double marginalLoss(OnlineModelsSU2.Model model,int[] draw){float[][] p=new float[10][10];model.predict(p,new float[model.stateWidth()]);double loss=0;for(int i=0;i<10;i++)loss-=Math.log(Math.max(1e-12,p[i][draw[i]-1]))/10;return loss;}
    static int[][] history(FamilyF19TensorR26 model)throws Exception{int[][] original=(int[][])field(model,"ring"),copy=new int[original.length][];for(int i=0;i<copy.length;i++)copy[i]=original[i].clone();return copy;}
    static double objective(FamilyF19TensorR26 model,int[] draw,int[][] history,long clock,int seen)throws Exception{
        double[][][] f=(double[][][])field(model,"factor");boolean position=(boolean)field(model,"positionMode"),state=(boolean)field(model,"stateMode");int category=seen==0?0:history[(int)((clock-1)%3)][0]<=5?0:1;double[][] scores=new double[10][10];
        // Expand the full tensor directly instead of using the product kernel's factorized context.
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)for(int lag=0;lag<seen;lag++)for(int q=0;q<10;q++)for(int k=0;k<3;k++){
            double a=f[2][q][k];if(!position){a=0;for(int z=0;z<10;z++)a+=f[2][z][k]/10;}
            int observed=history[(int)((clock-1-lag)%3)][q]-1;scores[p][n]+=f[0][p][k]*f[1][n][k]*a*f[3][observed][k]*f[4][lag][k]*(state?f[5][category][k]:1);
        }
        double loss=ExactPermutationR26.infer(scores,null).logZ;for(int p=0;p<10;p++)loss-=scores[p][draw[p]-1];return loss;
    }
    static void gradients(boolean position,boolean state)throws Exception{
        FamilyF19TensorR26 model=new FamilyF19TensorR26(.02,position,state);Random random=new Random(2619);model.update(permutation(random));
        double[][][] factors=(double[][][])field(model,"factor");for(int m=0;m<factors.length;m++)for(double[] row:factors[m])for(int k=0;k<row.length;k++)row[k]=m==5&&!state?1:(random.nextDouble()-.5)*1.2;
        int[] draw=permutation(random);int[][] h=history(model);long clock=model.completedUpdates();int seen=(int)field(model,"seen");double before=objective(model,draw,h,clock,seen),norm=0,eps=1e-4;
        for(int m=0;m<factors.length;m++)if(m!=5||state)for(double[] row:factors[m])for(int k=0;k<row.length;k++){
            double value=row[k];row[k]=value+eps;double plus=objective(model,draw,h,clock,seen);row[k]=value-eps;double minus=objective(model,draw,h,clock,seen);row[k]=value;double gradient=(plus-minus)/(2*eps);norm+=gradient*gradient;
        }
        model.update(draw);TrainingDiagnosticsR26.Step step=model.lastTrainingStep();check(Math.abs(step.lossBefore-before)<1e-10,"Tensor objective differs from dense reconstruction");check(Math.abs(step.lossAfter-objective(model,draw,h,clock,seen))<1e-10,"Post-update loss changed its input frame");check(Math.abs(step.gradientNorm-Math.sqrt(norm))<2e-6,"CP mode chain rule differs from finite differences");check(step.parameterDeltaNorm>0,"Trainable tensor did not update");
    }
    public static void main(String[] args)throws Exception{
        gradients(true,false);gradients(true,true);gradients(false,false);
        OnlineModelsSU2.Model archived=new FamilyF19KernelR26(.02),legacy=ModelReplaySU2.build("R26F19",Collections.singletonMap("abi","1"));check(legacy instanceof FamilyF19KernelR26,"Archived SGD policy silently changed executor");Random oldRandom=new Random(261900);for(int i=0;i<48;i++){int[] draw=permutation(oldRandom);archived.update(draw);legacy.update(draw);}check(Arrays.equals(save(archived),save(legacy)),"Archived SGD arithmetic/codec changed");
        ModelGenomeSU2 base=FamilyRegistryR26.genome("F19");List<StructuralProposalR26.Proposal> proposals=StructuralProposalR26.mechanismChildren(base);check(proposals.size()==2,"Missing state-mode mutation and position ablation");check(!StructuralProposalR26.structure(base).equals(StructuralProposalR26.structure(proposals.get(0).genome))&&proposals.get(0).category.equals("STRUCTURAL_CANDIDATE"),"Added tensor mode did not change the graph");for(StructuralProposalR26.Proposal proposal:proposals){ModelGenomeSU2.Member member=proposal.genome.members.get(0);check(ModelReplaySU2.build(member.kind,member.params) instanceof FamilyF19TensorR26,"Typed tensor graph did not execute");}check(StructuralProposalR26.control(proposals.get(1).genome),"Removed tensor mechanism could publish");check(StructuralProposalR26.mechanismChildren(proposals.get(0).genome).stream().anyMatch(p->p.operation.equals("REMOVE_TENSOR_STATE_MODE")&&StructuralProposalR26.control(p.genome)),"State removal lacked an explicit non-publishing control");

        FamilyF19TensorR26 contextual=new FamilyF19TensorR26(.02,true,true),noState=new FamilyF19TensorR26(.02,true,false),bag=new FamilyF19TensorR26(.02,false,false);FamilyF19TensorR26[] models={contextual,noState,bag};Random random=new Random(261019);
        for(int t=0;t<2500;t++){int[] input=permutation(random),target=mode(input[0]<=5);for(FamilyF19TensorR26 model:models){model.resetTemporalContext();model.update(input);marginalLoss(model,target);model.update(target);}}
        double[] held=new double[3];random=new Random(261020);for(int t=0;t<500;t++){int[] input=permutation(random),target=mode(input[0]<=5);for(int m=0;m<models.length;m++){models[m].resetTemporalContext();models[m].update(input);held[m]+=marginalLoss(models[m],target)/500;}}
        check(held[0]<.05&&held[1]>.5&&held[2]>.5,"Causal state advantage did not disappear on removal: "+Arrays.toString(held));
        byte[] checkpoint=save(contextual);FamilyF19TensorR26 restored=new FamilyF19TensorR26(.02,true,true);restored.read(new DataInputStream(new ByteArrayInputStream(checkpoint)));check(restored.lastTrainingStep()==null,"Restored diagnostic claimed a new update");check(Arrays.equals(checkpoint,save(restored)),"Factor/optimizer checkpoint differs");
        if(args.length>0)try(FileOutputStream file=new FileOutputStream(args[0])){file.write(checkpoint);file.getFD().sync();}
        for(int i=0;i<16;i++){int[] draw=permutation(random);contextual.update(draw);restored.update(draw);}check(Arrays.equals(save(contextual),save(restored)),"Optimizer or next update did not resume exactly");long updates=restored.completedUpdates();restored.resetTemporalContext();check(restored.completedUpdates()==updates&&restored.lastTrainingStep()==null,"Gap reset learned lifetime/diagnostics");for(int[] row:history(restored))for(int n:row)check(n==0,"Expired raw ring survived");
        byte[] good=save(contextual),corrupt=good.clone();Arrays.fill(corrupt,corrupt.length-8,corrupt.length,(byte)0xff);try{contextual.read(new DataInputStream(new ByteArrayInputStream(corrupt)));throw new AssertionError("Invalid optimizer accepted");}catch(IOException expected){}check(Arrays.equals(good,save(contextual)),"Rejected restore mutated the existing learner");
        try{new FamilyF19TensorR26(.02,false,true);throw new AssertionError("Position ablation retained hidden position state");}catch(IllegalArgumentException expected){}
        try{new FamilyF19TensorR26(.02,true,false).read(new DataInputStream(new ByteArrayInputStream(good)));throw new AssertionError("Checkpoint accepted a different relation graph");}catch(IOException expected){}
        FamilyF19TensorR26 counterA=new FamilyF19TensorR26(.02,true,true),counterB=new FamilyF19TensorR26(.02,true,false);FamilyF19TensorR26[] controls={counterA,counterB};random=new Random(261021);
        for(int t=0;t<1500;t++){int[] input=permutation(random),target=permutation(random);for(FamilyF19TensorR26 model:controls){model.resetTemporalContext();model.update(input);model.update(target);}}
        double[] iid=new double[2];for(int t=0;t<1000;t++){int[] input=permutation(random),target=permutation(random);for(int m=0;m<2;m++){controls[m].resetTemporalContext();controls[m].update(input);iid[m]+=marginalLoss(controls[m],target)/1000;}}check(iid[0]>=iid[1]-.02,"IID produced an unjustified causal-state advantage");
        FamilyF19TensorR26 a=new FamilyF19TensorR26(.02,true,true),b=new FamilyF19TensorR26(.02,true,true);int[] input=permutation(random);a.update(input);b.update(input);a.update(mode(true));b.update(mode(false));check(a.lastTrainingStep().inputHash.equals(b.lastTrainingStep().inputHash),"Diagnostic input digest observed its target label");
        FamilyF19TensorR26 canceled=new FamilyF19TensorR26(.02,true,true);canceled.update(input);byte[] unchanged=save(canceled);boolean interrupted=false;for(int operations:new int[]{1,10,30,100,300}){
            canceled.read(new DataInputStream(new ByteArrayInputStream(unchanged)));ExecutionBudgetR26.enter(new BudgetTokenSU1(30000,operations,()->false));try{canceled.update(mode(true));}catch(BudgetTokenSU1.Yield expected){interrupted=true;check(Arrays.equals(unchanged,save(canceled)),"Interrupted update changed factors/optimizer/context");}finally{ExecutionBudgetR26.leave();}
        }check(interrupted,"Cancellation control never interrupted inference");
        System.out.println("R26_TENSOR_MECHANISM_PASS|heldLaterMarginalNll="+Arrays.toString(held)+"|iidNll="+Arrays.toString(iid)+"|finiteDifferencesAllModes=true|fixedContextTrainingReceipt=true|restoreNextGapCancelExact=true|scope=SYNTHETIC_KERNEL_ONLY_NOT_DENSE_BUDGET_OR_NATURAL_QUALIFICATION");
    }
}
