package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.io.*;

/** UCB over measured gains. Priors are explicit and are not observations or evidence. */
public final class ResearchDirectorPolicySU1 {
    public static final String[] FAMILIES={"EXPERT_MIX","RELATION","XI_SELECTOR","DEEP","MATRIX","HEDGE","REGIME","GRAMMAR"};
    public static final String[] OPERATORS={"mutate","structural","crossover","fusion"};
    final LinkedHashMap<String,double[]> arms=new LinkedHashMap<>();
    final LinkedHashMap<String,int[]> ops=new LinkedHashMap<>();
    public ResearchDirectorPolicySU1(){for(String family:FAMILIES)add(family);for(String op:OPERATORS)ops.put(op,new int[]{0,0});}
    public void add(String arm){if(arms.size()<64)arms.putIfAbsent(arm,new double[]{0,0});}
    public String pick(int total,Map<String,Integer> generation,int population){
        String best=null;double bestValue=-Double.MAX_VALUE;int cap=Math.max(2,(int)(.35*population));
        for(Map.Entry<String,double[]> e:arms.entrySet()){
            if(generation.getOrDefault(e.getKey(),0)>=cap)continue;double n=1+e.getValue()[0];double value=(.01+e.getValue()[1])/n+.02*Math.sqrt(2*Math.log(total+2)/n);
            if(value>bestValue){bestValue=value;best=e.getKey();}
        }
        if(best==null)throw new IllegalStateException("SU1_ARM_CAP_EXHAUSTED");return best;
    }
    public String operator(Random rng){double sum=0;for(int[] x:ops.values())sum+=(x[1]+1.0)/(x[0]+2.0);double point=rng.nextDouble()*sum;for(Map.Entry<String,int[]> e:ops.entrySet()){point-=(e.getValue()[1]+1.0)/(e.getValue()[0]+2.0);if(point<=0)return e.getKey();}return "mutate";}
    public void record(String arm,String op,double parent,double child){add(arm);double[] x=arms.get(arm);x[0]++;x[1]+=Math.max(0,child-parent);int[] o=ops.computeIfAbsent(op,k->new int[]{0,0});o[0]++;if(child>parent)o[1]++;}
    public void write(DataOutput out)throws IOException{out.writeInt(arms.size());for(Map.Entry<String,double[]> e:arms.entrySet()){out.writeUTF(e.getKey());out.writeDouble(e.getValue()[0]);out.writeDouble(e.getValue()[1]);}out.writeInt(ops.size());for(Map.Entry<String,int[]> e:ops.entrySet()){out.writeUTF(e.getKey());out.writeInt(e.getValue()[0]);out.writeInt(e.getValue()[1]);}}
    public static ResearchDirectorPolicySU1 read(DataInput in)throws IOException{ResearchDirectorPolicySU1 p=new ResearchDirectorPolicySU1();p.arms.clear();int n=in.readInt();if(n<1||n>64)throw new IOException("SU1_DIRECTOR_ARMS");for(int i=0;i<n;i++){String arm=in.readUTF();double trials=in.readDouble(),gain=in.readDouble();if(!Double.isFinite(trials)||!Double.isFinite(gain)||trials<0||gain<0)throw new IOException("SU1_DIRECTOR_STATE");p.arms.put(arm,new double[]{trials,gain});}n=in.readInt();if(n<1||n>8)throw new IOException("SU1_DIRECTOR_OPS");p.ops.clear();for(int i=0;i<n;i++)p.ops.put(in.readUTF(),new int[]{in.readInt(),in.readInt()});return p;}
}
