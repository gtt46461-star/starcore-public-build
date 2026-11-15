package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Exact permutation chain factor graph. Pairwise adjacency, not arbitrary high-order inference. */
public final class FamilyF02KernelR26 implements TemporalModelR26,TrainingDiagnosticsR26 {
    private final double[][] unary=new double[10][10],context=new double[100][10];private final double[][][] pair=new double[9][10][10];private final int graph;private final boolean factors;private final int[] order;private int[] previous;private long t;private TrainingDiagnosticsR26.Step last;private transient PermutationChainR26.Result cached;
    public FamilyF02KernelR26(){this(0,true);}
    public FamilyF02KernelR26(int graph,boolean factors){if(graph<0||graph>2)throw new IllegalArgumentException("F02_GRAPH");this.graph=graph;this.factors=factors;order=graph==0?new int[]{0,1,2,3,4,5,6,7,8,9}:graph==1?new int[]{0,2,4,6,8,1,3,5,7,9}:new int[]{0,9,1,8,2,7,3,6,4,5};}
    private double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)s[p][n]=unary[p][n]+(previous==null?0:context[p*10+n][previous[p]-1]);return s;}
    private PermutationChainR26.Result infer(){if(cached==null)cached=PermutationChainR26.infer(scores(),factors?pair:new double[9][10][10],order);return cached;}
    public void resetTemporalContext(){previous=null;cached=null;last=null;}
    public TrainingDiagnosticsR26.Step lastTrainingStep(){return last;}
    public long completedTrainingUpdates(){return t;}
    public String optimizerProtocol(){return "EXACT_CHAIN_SGD_002_PARAM_CLIP8_V1";}
    private String inputHash(){StringBuilder input=new StringBuilder("F02_FIXED_CHAIN_CONTEXT_V1|").append(graph).append('|').append(factors).append('|').append(t);if(previous==null)input.append("|NO_PREVIOUS");else for(int value:previous)input.append('|').append(value);return PolicyIdentityR26.sha256(input.toString());}
    private double loss(PermutationChainR26.Result distribution,double[][] scores,double[][][] interactions,int[] draw){double value=distribution.z;for(int p=0;p<10;p++)value-=scores[p][draw[p]-1];if(factors)for(int p=0;p<9;p++)value-=interactions[p][draw[order[p]]-1][draw[order[p+1]]-1];return value;}
    public int stateWidth(){return 2;}
    public void predict(float[][] p,float[] st){PermutationChainR26.Result r=infer();for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.p[i][j];st[0]=(float)Math.log1p(t);st[1]=factors?9:0;}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(t==Long.MAX_VALUE)throw new IllegalStateException("F02_UPDATE_CLOCK_CAPACITY");PermutationChainR26.Result distribution=infer();double before=loss(distribution,scores(),pair,draw),gradientSquared=0,deltaSquared=0;double[][] nextUnary=new double[10][10],nextContext=new double[100][10],afterScores=new double[10][10];double[][][] nextPair=new double[9][10][10];for(int i=0;i<100;i++)System.arraycopy(context[i],0,nextContext[i],0,10);for(int p=0;p<9;p++)for(int a=0;a<10;a++)System.arraycopy(pair[p][a],0,nextPair[p][a],0,10);
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){double gradient=distribution.p[p][n]-(draw[p]==n+1?1:0),next=bound(unary[p][n]-.02*gradient),delta=next-unary[p][n];nextUnary[p][n]=next;gradientSquared+=gradient*gradient;deltaSquared+=delta*delta;if(previous!=null){int x=previous[p]-1;next=bound(context[p*10+n][x]-.02*gradient);delta=next-context[p*10+n][x];nextContext[p*10+n][x]=next;gradientSquared+=gradient*gradient;deltaSquared+=delta*delta;}afterScores[p][n]=nextUnary[p][n]+(previous==null?0:nextContext[p*10+n][previous[p]-1]);}
        if(factors)for(int p=0;p<9;p++)for(int a=0;a<10;a++)for(int n=0;n<10;n++){double gradient=distribution.edge[p][a][n]-(draw[order[p]]==a+1&&draw[order[p+1]]==n+1?1:0),next=bound(pair[p][a][n]-.02*gradient),delta=next-pair[p][a][n];nextPair[p][a][n]=next;gradientSquared+=gradient*gradient;deltaSquared+=delta*delta;}
        cached=null;double after=loss(PermutationChainR26.infer(afterScores,factors?nextPair:new double[9][10][10],order),afterScores,nextPair,draw);TrainingDiagnosticsR26.Step step=new TrainingDiagnosticsR26.Step(t+1,before,after,Math.sqrt(gradientSquared),Math.sqrt(deltaSquared),inputHash(),"EXACT_PERMUTATION_CHAIN_JOINT_NLL_FIXED_PRE_LABEL_LAG_CONTEXT;DELTA=UNARY_ACTIVE_CONTEXT_AND_ENABLED_ADJACENCY_FACTORS");ExecutionBudgetR26.poll();
        for(int p=0;p<10;p++)System.arraycopy(nextUnary[p],0,unary[p],0,10);for(int p=0;p<100;p++)System.arraycopy(nextContext[p],0,context[p],0,10);for(int p=0;p<9;p++)for(int a=0;a<10;a++)System.arraycopy(nextPair[p][a],0,pair[p][a],0,10);previous=draw.clone();t++;cached=null;last=step;
    }
    private static double bound(double x){return Math.max(-8,Math.min(8,x));}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26020002);o.writeInt(graph);o.writeBoolean(factors);o.writeLong(t);o.writeBoolean(previous!=null);if(previous!=null)for(int n:previous)o.writeInt(n);for(double[][] m:new double[][][]{unary,context})for(double[] row:m)for(double x:row)o.writeDouble(x);for(double[][] m:pair)for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{
        int header=in.readInt();if(header==0x26020002){if(in.readInt()!=graph||in.readBoolean()!=factors)throw new IOException("F02_GRAPH_BINDING");}else if(header!=0x26020001||graph!=0||!factors)throw new IOException("F02_ABI");long clock=in.readLong();if(clock<0)throw new IOException("F02_CURSOR");int[] history=null;if(in.readBoolean()){history=new int[10];for(int i=0;i<10;i++)history[i]=in.readInt();OnlineModelsSU2.requireDraw(history);}
        double[][] nextUnary=new double[10][10],nextContext=new double[100][10];double[][][] nextPair=new double[9][10][10];for(double[][] matrix:new double[][][]{nextUnary,nextContext})for(double[] row:matrix)for(int j=0;j<row.length;j++)row[j]=value(in);for(double[][] matrix:nextPair)for(double[] row:matrix)for(int j=0;j<row.length;j++)row[j]=value(in);
        for(int p=0;p<10;p++)System.arraycopy(nextUnary[p],0,unary[p],0,10);for(int p=0;p<100;p++)System.arraycopy(nextContext[p],0,context[p],0,10);for(int p=0;p<9;p++)for(int a=0;a<10;a++)System.arraycopy(nextPair[p][a],0,pair[p][a],0,10);t=clock;previous=history;cached=null;last=null;
    }
    private static double value(DataInput in)throws IOException{double x=in.readDouble();if(!Double.isFinite(x)||Math.abs(x)>8)throw new IOException("F02_PARAMETER");return x;}
}
