package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Exact permutation chain factor graph. Pairwise adjacency, not arbitrary high-order inference. */
public final class FamilyF02KernelR26 implements TemporalModelR26 {
    private final double[][] unary=new double[10][10],context=new double[100][10];private final double[][][] pair=new double[9][10][10];private final int graph;private final boolean factors;private final int[] order;private int[] previous;private long t;private transient PermutationChainR26.Result cached;
    public FamilyF02KernelR26(){this(0,true);}
    public FamilyF02KernelR26(int graph,boolean factors){if(graph<0||graph>2)throw new IllegalArgumentException("F02_GRAPH");this.graph=graph;this.factors=factors;order=graph==0?new int[]{0,1,2,3,4,5,6,7,8,9}:graph==1?new int[]{0,2,4,6,8,1,3,5,7,9}:new int[]{0,9,1,8,2,7,3,6,4,5};}
    private double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)s[p][n]=unary[p][n]+(previous==null?0:context[p*10+n][previous[p]-1]);return s;}
    private PermutationChainR26.Result infer(){if(cached==null)cached=PermutationChainR26.infer(scores(),factors?pair:new double[9][10][10],order);return cached;}
    public void resetTemporalContext(){previous=null;cached=null;}
    public int stateWidth(){return 2;}
    public void predict(float[][] p,float[] st){PermutationChainR26.Result r=infer();for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.p[i][j];st[0]=(float)Math.log1p(t);st[1]=factors?9:0;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);PermutationChainR26.Result r=infer();for(int p=0;p<10;p++)for(int n=0;n<10;n++){double g=r.p[p][n]-(draw[p]==n+1?1:0);unary[p][n]=bound(unary[p][n]-.02*g);if(previous!=null)context[p*10+n][previous[p]-1]=bound(context[p*10+n][previous[p]-1]-.02*g);}if(factors)for(int p=0;p<9;p++)for(int a=0;a<10;a++)for(int n=0;n<10;n++){double g=r.edge[p][a][n]-(draw[order[p]]==a+1&&draw[order[p+1]]==n+1?1:0);pair[p][a][n]=bound(pair[p][a][n]-.02*g);}previous=draw.clone();t++;cached=null;}
    private static double bound(double x){return Math.max(-8,Math.min(8,x));}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26020002);o.writeInt(graph);o.writeBoolean(factors);o.writeLong(t);o.writeBoolean(previous!=null);if(previous!=null)for(int n:previous)o.writeInt(n);for(double[][] m:new double[][][]{unary,context})for(double[] row:m)for(double x:row)o.writeDouble(x);for(double[][] m:pair)for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{int header=in.readInt();if(header==0x26020002){if(in.readInt()!=graph||in.readBoolean()!=factors)throw new IOException("F02_GRAPH_BINDING");}else if(header!=0x26020001||graph!=0||!factors)throw new IOException("F02_ABI");t=in.readLong();if(t<0)throw new IOException("F02_CURSOR");previous=null;if(in.readBoolean()){previous=new int[10];for(int i=0;i<10;i++)previous[i]=in.readInt();OnlineModelsSU2.requireDraw(previous);}for(double[][] m:new double[][][]{unary,context})for(double[] row:m)for(int j=0;j<row.length;j++)row[j]=value(in);for(double[][] m:pair)for(double[] row:m)for(int j=0;j<row.length;j++)row[j]=value(in);cached=null;}
    private static double value(DataInput in)throws IOException{double x=in.readDouble();if(!Double.isFinite(x)||Math.abs(x)>8)throw new IOException("F02_PARAMETER");return x;}
}
