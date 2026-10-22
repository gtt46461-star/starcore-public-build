package com.ethk3.novaonepro.strategyv2;

import java.util.*;

public final class RelGraphContractsSU1Test {
    private static int checks;
    private static final String SCHEMA=HashingSU1.text("SYNTHETIC_RELGRAPH_CONTRACT_C2_R2_T2");
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(600000,100000000,null);}
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static void rejects(Runnable operation){try{operation.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError("invalid graph accepted");}
    public static RelGraphModelSU1.Input input(int labelShift,boolean reverse,boolean permute,boolean missing,boolean edges){
        RelGraphModelSU1.Step[] steps=new RelGraphModelSU1.Step[3];Random random=new Random(23);int[] remap=new int[100];for(int i=0;i<100;i++)remap[i]=i;if(permute)for(int i=99;i>0;i--){int q=random.nextInt(i+1),x=remap[i];remap[i]=remap[q];remap[q]=x;}
        for(int t=0;t<3;t++){
            int[] positions=new int[100],numbers=new int[100],draw=new int[10];double[] features=new double[200];boolean[] mask=new boolean[200];List<RelGraphModelSU1.Edge> links=new ArrayList<>();String root=HashingSU1.text("synthetic source "+t);
            for(int p=0;p<10;p++)draw[p]=p+1;
            for(int old=0;old<100;old++){int node=remap[old],p=old/10,n=old%10;positions[node]=p;numbers[node]=n;features[node*2]=(n==(p+labelShift)%10?1:-1);features[node*2+1]=Math.sin(old*.31+t*.47);mask[node*2]=!missing;mask[node*2+1]=!missing;}
            if(edges)for(int old=0;old<100;old++)for(int type=0;type<2;type++){
                int q=(old/10+3+type)%10,r=(q+2)%10;links.add(new RelGraphModelSU1.Edge(remap[old],type,new int[]{remap[q*10+q],remap[r*10+r]},new int[]{0,1},Math.sin(old*.13+type),10+old%7,t+1,root));
            }
            if(reverse)Collections.reverse(links);steps[t]=new RelGraphModelSU1.Step(t+1,t+1,root,draw,positions,numbers,features,mask,2,links.toArray(new RelGraphModelSU1.Edge[0]));
        }
        return new RelGraphModelSU1.Input(SCHEMA,steps);
    }
    public static void main(String[] args){
        RelGraphModelSU1.Input x=input(3,false,false,false,true);RelGraphModelSU1.Weights weights=RelGraphModelSU1.initial(2,2,2,2,SCHEMA,19);int[] label=new int[10];for(int p=0;p<10;p++)label[p]=(p+3)%10+1;
        RelGraphModelSU1.Gradient gradient=RelGraphModelSU1.gradient(weights,x,label,budget());double[][] prediction=RelGraphModelSU1.predict(weights,x,budget());check(Arrays.deepEquals(prediction,gradient.probabilities),"training and inference forward differ");
        for(String key:RelGraphModelSU1.GROUPS){double[] tensor=weights.get(key),g=gradient.tensors.get(key);check(gradient.norm(key)>1e-12,"parameter group has no real gradient: "+key);
            for(int i=0;i<tensor.length;i++){
                double value=tensor[i],epsilon=1e-4;tensor[i]=value+epsilon;double plus=RelGraphModelSU1.gradient(weights,x,label,budget()).loss;tensor[i]=value-epsilon;double minus=RelGraphModelSU1.gradient(weights,x,label,budget()).loss;tensor[i]=value;
                check(Math.abs((plus-minus)/(2*epsilon)-g[i])<2e-7,"finite difference mismatch "+key+"["+i+"]");
            }
        }
        RelGraphOptimizerSU1 state=RelGraphOptimizerSU1.initial(weights),updated=state.apply(gradient,.001,1);for(String key:RelGraphModelSU1.GROUPS)check(!Arrays.equals(state.weights.get(key),updated.weights.get(key)),"no actual parameter update: "+key);
        String wire=RelGraphCodecSU1.encode(updated);RelGraphOptimizerSU1 restored=RelGraphCodecSU1.decode(wire,HashingSU1.text(wire));check(Arrays.deepEquals(RelGraphModelSU1.predict(updated.weights,x,budget()),RelGraphModelSU1.predict(restored.weights,x,budget())),"graph state transport altered inference");
        RelGraphModelSU1.Gradient nextGradient=RelGraphModelSU1.gradient(updated.weights,x,label,budget());check(RelGraphCodecSU1.encode(updated.apply(nextGradient,.001,1)).equals(RelGraphCodecSU1.encode(restored.apply(nextGradient,.001,1))),"graph Adam continuation diverged");
        check(Arrays.deepEquals(prediction,RelGraphModelSU1.predict(weights,input(3,true,false,false,true),budget())),"edge storage order changed inference");
        double[][] remapped=RelGraphModelSU1.predict(weights,input(3,true,true,false,true),budget());for(int p=0;p<10;p++)for(int n=0;n<10;n++)check(Math.abs(prediction[p][n]-remapped[p][n])<1e-12,"event node renumbering changed inference");
        double[][] maskedA=RelGraphModelSU1.predict(weights,input(3,false,false,true,false),budget()),maskedB=RelGraphModelSU1.predict(weights,input(7,false,false,true,false),budget());check(Arrays.deepEquals(maskedA,maskedB),"missing features were treated as observed");
        RelGraphModelSU1.Input other=input(7,false,false,false,true);int[] secondLabel=new int[10];for(int p=0;p<10;p++)secondLabel[p]=(p+7)%10+1;RelGraphOptimizerSU1 learned=RelGraphOptimizerSU1.initial(weights);
        double before=(RelGraphModelSU1.gradient(weights,x,label,budget()).loss+RelGraphModelSU1.gradient(weights,other,secondLabel,budget()).loss)/2;
        for(int i=0;i<80;i++){RelGraphModelSU1.Input use=i%2==0?x:other;int[] actual=i%2==0?label:secondLabel;learned=learned.apply(RelGraphModelSU1.gradient(learned.weights,use,actual,budget()),.01,1);}
        double after=(RelGraphModelSU1.gradient(learned.weights,x,label,budget()).loss+RelGraphModelSU1.gradient(learned.weights,other,secondLabel,budget()).loss)/2;check(after<before-.5,"model did not learn a relation-dependent task with identical raw numbers");
        check(!Arrays.deepEquals(RelGraphModelSU1.predict(learned.weights,x,budget()),RelGraphModelSU1.predict(learned.weights,other,budget())),"raw-number-identical relation inputs were collapsed");
        int[][] raw=new int[120][10],changed=new int[120][10];for(int t=0;t<120;t++)for(int p=0;p<10;p++){raw[t][p]=(p+t)%10+1;changed[t][p]=raw[t][p];}for(int t=100;t<120;t++){int v=changed[t][0];changed[t][0]=changed[t][1];changed[t][1]=v;}
        RelationTensorAdapterSU1.Factory factory=new RelationTensorAdapterSU1.Factory(raw),futureChanged=new RelationTensorAdapterSU1.Factory(changed);check(factory.input(100,4,budget()).hash.equals(futureChanged.input(100,4,budget()).hash),"future rows changed an earlier relation frame");
        double[][] live=ProgramTreeSU1.parse("GDEEP(2,4,19)").score(new ProgramTreeSU1.Context(raw,100));double[][] same=ProgramTreeSU1.parse("GDEEP(2,4,19)").score(new ProgramTreeSU1.Context(changed,100));check(Arrays.deepEquals(live,same),"deployed graph grammar read future labels");
        rejects(()->RelGraphCodecSU1.decode(wire+"A",HashingSU1.text(wire)));
        System.out.println("SU1_RELGRAPH_CONTRACTS_PASS checks="+checks+" groups=18 synthetic_ce_before="+before+" after="+after+" scope=synthetic_numerics_not_prediction_quality");
    }
}
