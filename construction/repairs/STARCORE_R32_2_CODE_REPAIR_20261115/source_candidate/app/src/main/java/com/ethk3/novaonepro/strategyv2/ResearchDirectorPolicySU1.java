package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.io.*;

/** UCB over measured gains. Priors are explicit and are not observations or evidence. */
public final class ResearchDirectorPolicySU1 {
    public static final String PROTOCOL="SU1_CONTEXT_COST_UCB_V3|EXPLORATION20_CAP35|OBSERVED_FAILURE_TYPE_WEIGHTED";
    public static final String[] FAMILIES={"EXPERT_MIX","RELATION","XI_SELECTOR","DEEP","MATRIX","HEDGE","REGIME","GRAMMAR"};
    public static final String[] OPERATORS={"mutate","structural","crossover","fusion"};
    final LinkedHashMap<String,double[]> arms=new LinkedHashMap<>();
    final LinkedHashMap<String,int[]> ops=new LinkedHashMap<>();
    final LinkedHashMap<String,double[]> live=new LinkedHashMap<>();
    final LinkedHashMap<String,double[]> contexts=new LinkedHashMap<>();
    public boolean enabled=true;
    private String context="UNKNOWN";
    public void context(String key){if(key==null||key.length()>128)throw new IllegalArgumentException("SU1_DIRECTOR_CONTEXT");context=key;}
    public void live(String arm,long actions,long hits){if(!Arrays.asList(FAMILIES).contains(arm)&&!arm.startsWith("EMERGENT:"))return;if(actions<0||hits<0||hits>actions)throw new IllegalArgumentException("SU1_LIVE_REWARD");add(arm);if(arms.containsKey(arm))live.put(arm,new double[]{actions,hits});}
    public ResearchDirectorPolicySU1(){for(String family:FAMILIES)add(family);for(String op:OPERATORS)ops.put(op,new int[]{0,0});}
    public void add(String arm){if(arms.size()<64)arms.putIfAbsent(arm,new double[]{0,0});}
    public String pick(int total,Map<String,Integer> generation,int population){
        if(!enabled){int used=0;for(int n:generation.values())used+=n;return FAMILIES[used%FAMILIES.length];}
        String best=null;double bestValue=-Double.MAX_VALUE;int cap=Math.max(2,(int)(.35*population));
        int used=0;for(int n:generation.values())used+=n;boolean explore=used%5==0;
        for(Map.Entry<String,double[]> e:arms.entrySet()){
            if(generation.getOrDefault(e.getKey(),0)>=cap)continue;double n=1+e.getValue()[0];double value=(.01+e.getValue()[1])/n+.02*Math.sqrt(2*Math.log(total+2)/n);
            double[] local=contexts.get(context+'|'+e.getKey());
            if(local!=null&&local[0]>0){double averageCost=local[2]/local[0];value+=local[1]/(1+local[0])/(1+averageCost/10000)-.01*local[3]/local[0];}
            double[] feedback=live.get(e.getKey());if(feedback!=null&&feedback[0]>0)value+=.05*(feedback[1]/feedback[0]-.30)*feedback[0]/(feedback[0]+200);
            if(explore)value=-e.getValue()[0]-generation.getOrDefault(e.getKey(),0)*1000000d;
            if(value>bestValue){bestValue=value;best=e.getKey();}
        }
        if(best==null)throw new IllegalStateException("SU1_ARM_CAP_EXHAUSTED");return best;
    }
    public String operator(Random rng){if(!enabled)return OPERATORS[rng.nextInt(OPERATORS.length)];double sum=0;for(String op:OPERATORS){int[] x=ops.get(op);sum+=(x[1]+1.0)/(x[0]+2.0);}double point=rng.nextDouble()*sum;for(String op:OPERATORS){int[] x=ops.get(op);point-=(x[1]+1.0)/(x[0]+2.0);if(point<=0)return op;}return "mutate";}
    public void record(String arm,String op,double parent,double child){record(arm,op,parent,child,0,false);}
    public void record(String arm,String op,double parent,double child,long costUnits,boolean resourceFailure){
        if(!Double.isFinite(parent)||!Double.isFinite(child)||costUnits<0)throw new IllegalArgumentException("SU1_RESEARCH_REWARD");
        add(arm);double baseline=parent<=-1?.30:parent,gain=Math.max(0,child-baseline);double[] x=arms.get(arm);if(x==null)return;x[0]++;x[1]+=gain;
        int[] o=ops.computeIfAbsent(op,k->new int[]{0,0});o[0]++;if(child>baseline)o[1]++;
        String key=context+'|'+arm;double[] local=contexts.get(key);if(local==null&&contexts.size()<512){local=new double[4];contexts.put(key,local);}if(local!=null){local[0]++;local[1]+=gain;local[2]+=costUnits;if(resourceFailure)local[3]++;}
    }
    public void failure(String arm,String op,double parent,ResearchFailureSU1 failure,long costUnits){
        if(failure==null||!failure.rejectCandidate)throw new IllegalArgumentException("SU1_NOT_A_CANDIDATE_FAILURE");
        record(arm,op,parent,-1,costUnits,false);
        double[] local=contexts.get(context+'|'+arm);if(local!=null)local[3]+=failure.schedulingPenalty();
    }
    public void write(DataOutput out)throws IOException{
        out.writeInt(0x53553150);out.writeInt(2);out.writeBoolean(enabled);out.writeUTF(context);
        out.writeInt(arms.size());for(Map.Entry<String,double[]> e:arms.entrySet()){out.writeUTF(e.getKey());out.writeDouble(e.getValue()[0]);out.writeDouble(e.getValue()[1]);}out.writeInt(ops.size());for(Map.Entry<String,int[]> e:ops.entrySet()){out.writeUTF(e.getKey());out.writeInt(e.getValue()[0]);out.writeInt(e.getValue()[1]);}
        out.writeInt(contexts.size());for(Map.Entry<String,double[]> e:contexts.entrySet()){out.writeUTF(e.getKey());for(double v:e.getValue())out.writeDouble(v);}
    }
    public static ResearchDirectorPolicySU1 read(DataInput in)throws IOException{
        ResearchDirectorPolicySU1 p=new ResearchDirectorPolicySU1();p.arms.clear();int first=in.readInt();boolean v2=first==0x53553150;int n;
        if(v2){if(in.readInt()!=2)throw new IOException("SU1_DIRECTOR_PROTOCOL");p.enabled=in.readBoolean();p.context=in.readUTF();n=in.readInt();}else n=first;
        if(n<1||n>64)throw new IOException("SU1_DIRECTOR_ARMS");for(int i=0;i<n;i++){String arm=in.readUTF();double trials=in.readDouble(),gain=in.readDouble();if(!Double.isFinite(trials)||!Double.isFinite(gain)||trials<0||gain<0)throw new IOException("SU1_DIRECTOR_STATE");p.arms.put(arm,new double[]{trials,gain});}
        n=in.readInt();if(n<1||n>16)throw new IOException("SU1_DIRECTOR_OPS");p.ops.clear();for(int i=0;i<n;i++){String op=in.readUTF();int trials=in.readInt(),success=in.readInt();if(trials<0||success<0||success>trials)throw new IOException("SU1_DIRECTOR_OPERATOR_STATE");p.ops.put(op,new int[]{trials,success});}
        for(String op:OPERATORS)p.ops.putIfAbsent(op,new int[]{0,0});
        if(v2){n=in.readInt();if(n<0||n>512)throw new IOException("SU1_DIRECTOR_CONTEXTS");for(int i=0;i<n;i++){String key=in.readUTF();double[] values=new double[4];for(int j=0;j<4;j++){values[j]=in.readDouble();if(!Double.isFinite(values[j])||values[j]<0)throw new IOException("SU1_DIRECTOR_CONTEXT_STATE");}p.contexts.put(key,values);}}
        return p;
    }
}
