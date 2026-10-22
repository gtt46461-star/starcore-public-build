package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;

/** Complete strategy replay: causal scores, deterministic K3 action and block-causal coverage gate. */
public final class HistoricalReplayEngineSU1 {
    private HistoricalReplayEngineSU1() {}
    public static final class Replay {
        public final boolean[] act,hit;
        public final int[] position,mask;
        public final double[] gate;
        public final String[] probeScores=new String[64];
        Replay(int n){act=new boolean[n];hit=new boolean[n];position=new int[n];mask=new int[n];gate=new double[n];Arrays.fill(gate,Double.NaN);}
        public int actions(int a,int b){int n=0;for(int t=a;t<b;t++)if(act[t])n++;return n;}
        public int hits(int a,int b){int n=0;for(int t=a;t<b;t++)if(act[t]&&hit[t])n++;return n;}
    }
    public static double[] positionMasses(double[][] s){
        double[] mass=new double[10];
        for(int p=0;p<10;p++){
            double max=-Double.MAX_VALUE;for(double v:s[p]){if(!Double.isFinite(v))throw new IllegalArgumentException("SU1_NONFINITE_SCORE");max=Math.max(max,v);}
            final double[] row=s[p];Integer[] order=new Integer[10];for(int i=0;i<10;i++)order[i]=i;
            Arrays.sort(order,(a,b)->Double.compare(row[b]-b*1e-6,row[a]-a*1e-6));
            double sum=0,top=0;for(int n=0;n<10;n++)sum+=Math.exp(row[n]-max);for(int n=0;n<3;n++)top+=Math.exp(row[order[n]]-max);
            mass[p]=Math.round(top/sum*1e9)/1e9;
        }
        return mass;
    }
    public static double gateValue(double[][] scores,int selectedPosition,boolean margin){
        double[] mass=positionMasses(scores);double best=mass[selectedPosition-1];if(!margin)return best;
        double second=-1;for(int p=0;p<10;p++)if(p!=selectedPosition-1)second=Math.max(second,mass[p]);return best-second;
    }
    public static double quantileLinear(double[] v,int lo,int hi,double q){
        if(lo<0||hi>v.length||lo>=hi||q<0||q>1)throw new IllegalArgumentException("SU1_QUANTILE");
        double[] x=Arrays.copyOfRange(v,lo,hi);for(double d:x)if(!Double.isFinite(d))throw new IllegalArgumentException("SU1_GATE_HISTORY_MISSING");
        Arrays.sort(x);double pos=(x.length-1)*q;int i=(int)Math.floor(pos);return i+1<x.length?x[i]+(pos-i)*(x[i+1]-x[i]):x[i];
    }
    public static Replay run(int[][] draws,String text,double coverage,boolean margin,int start,int end,BudgetTokenSU1 budget){
        if(start<1||end<=start||end>draws.length||draws.length>20000||coverage<=0||coverage>1)throw new IllegalArgumentException("SU1_REPLAY_RANGE");
        for(int[] d:draws)RelationalStateInference.validateDraw(d);
        ProgramTreeSU1.Node program=ProgramTreeSU1.parse(text);Replay r=new Replay(end);
        ScoreHistoryCacheSU1 cache=new ScoreHistoryCacheSU1(2048);
        int from=Math.max(0,start-720);
        try(AtomLiftSU1 atoms=new AtomLiftSU1(draws)){
            ProgramTreeSU1.Context root=new ProgramTreeSU1.Context(draws,from,atoms,cache,budget);
            for(int t=from;t<end;t++){
                budget.checkpointOrYield();double[][] s=program.score(root.atTarget(t));int[] a=ProgramTreeSU1.action(s);
                r.gate[t]=gateValue(s,a[0],margin);r.position[t]=a[0];r.mask[t]=(1<<(a[1]-1))|(1<<(a[2]-1))|(1<<(a[3]-1));r.hit[t]=(r.mask[t]&(1<<(draws[t][a[0]-1]-1)))!=0;
                int probe=BehaviorSignatureSU1.probeIndex(t,start,end);if(probe>=0)r.probeScores[probe]=BehaviorSignatureSU1.scoreHash(s);
            }
        }
        for(int b=start;b<end;b+=50){double threshold=coverage>=.999?Double.NEGATIVE_INFINITY:quantileLinear(r.gate,Math.max(0,b-720),b,1-coverage);
            for(int t=b;t<Math.min(b+50,end);t++)r.act[t]=r.gate[t]>=threshold;}
        return r;
    }
}
