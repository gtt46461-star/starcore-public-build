package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Exact permutation chain factor graph. Pairwise adjacency, not arbitrary high-order inference. */
public final class FamilyF02KernelR26 implements TemporalModelR26 {
    private final double[][] unary=new double[10][10],context=new double[100][10];private final double[][][] pair=new double[9][10][10];private int[] previous;private long t;private transient Inference cached;
    private static final class Inference {double z;double[][] p=new double[10][10];double[][][] edge=new double[9][10][10];}
    private static double add(double a,double b){if(a==Double.NEGATIVE_INFINITY)return b;if(b==Double.NEGATIVE_INFINITY)return a;double m=Math.max(a,b);return m+Math.log1p(Math.exp(Math.min(a,b)-m));}
    private double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)s[p][n]=unary[p][n]+(previous==null?0:context[p*10+n][previous[p]-1]);return s;}
    private Inference infer(){if(cached!=null)return cached;double[][] s=scores(),f=new double[1024][10],b=new double[1024][10];for(double[] row:f)Arrays.fill(row,Double.NEGATIVE_INFINITY);for(double[] row:b)Arrays.fill(row,Double.NEGATIVE_INFINITY);for(int j=0;j<10;j++){f[1<<j][j]=s[0][j];b[1023][j]=0;}
        for(int mask=1;mask<1023;mask++){if((mask&31)==0)ExecutionBudgetR26.poll();int p=Integer.bitCount(mask);for(int last=0;last<10;last++)if((mask&(1<<last))!=0)for(int n=0;n<10;n++)if((mask&(1<<n))==0){int next=mask|1<<n;f[next][n]=add(f[next][n],f[mask][last]+s[p][n]+pair[p-1][last][n]);}}
        for(int mask=1022;mask>=1;mask--){if((mask&31)==0)ExecutionBudgetR26.poll();int p=Integer.bitCount(mask);for(int last=0;last<10;last++)if((mask&(1<<last))!=0)for(int n=0;n<10;n++)if((mask&(1<<n))==0)b[mask][last]=add(b[mask][last],s[p][n]+pair[p-1][last][n]+b[mask|1<<n][n]);}
        Inference r=new Inference();r.z=Double.NEGATIVE_INFINITY;for(int j=0;j<10;j++)r.z=add(r.z,f[1023][j]);for(int j=0;j<10;j++)r.p[0][j]=Math.exp(s[0][j]+b[1<<j][j]-r.z);
        for(int mask=1;mask<1023;mask++){if((mask&31)==0)ExecutionBudgetR26.poll();int p=Integer.bitCount(mask);for(int last=0;last<10;last++)if((mask&(1<<last))!=0)for(int n=0;n<10;n++)if((mask&(1<<n))==0){double mass=Math.exp(f[mask][last]+s[p][n]+pair[p-1][last][n]+b[mask|1<<n][n]-r.z);r.p[p][n]+=mass;r.edge[p-1][last][n]+=mass;}}
        cached=r;return r;
    }
    public void resetTemporalContext(){previous=null;cached=null;}
    public int stateWidth(){return 2;}
    public void predict(float[][] p,float[] st){Inference r=infer();for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.p[i][j];st[0]=(float)Math.log1p(t);st[1]=9;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);Inference r=infer();for(int p=0;p<10;p++)for(int n=0;n<10;n++){double g=r.p[p][n]-(draw[p]==n+1?1:0);unary[p][n]=bound(unary[p][n]-.02*g);if(previous!=null)context[p*10+n][previous[p]-1]=bound(context[p*10+n][previous[p]-1]-.02*g);}for(int p=0;p<9;p++)for(int a=0;a<10;a++)for(int n=0;n<10;n++){double g=r.edge[p][a][n]-(draw[p]==a+1&&draw[p+1]==n+1?1:0);pair[p][a][n]=bound(pair[p][a][n]-.02*g);}previous=draw.clone();t++;cached=null;}
    private static double bound(double x){return Math.max(-8,Math.min(8,x));}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26020001);o.writeLong(t);o.writeBoolean(previous!=null);if(previous!=null)for(int n:previous)o.writeInt(n);for(double[][] m:new double[][][]{unary,context})for(double[] row:m)for(double x:row)o.writeDouble(x);for(double[][] m:pair)for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26020001)throw new IOException("F02_ABI");t=in.readLong();if(t<0)throw new IOException("F02_CURSOR");previous=null;if(in.readBoolean()){previous=new int[10];for(int i=0;i<10;i++)previous[i]=in.readInt();OnlineModelsSU2.requireDraw(previous);}for(double[][] m:new double[][][]{unary,context})for(double[] row:m)for(int j=0;j<row.length;j++)row[j]=value(in);for(double[][] m:pair)for(double[] row:m)for(int j=0;j<row.length;j++)row[j]=value(in);cached=null;}
    private static double value(DataInput in)throws IOException{double x=in.readDouble();if(!Double.isFinite(x)||Math.abs(x)>8)throw new IOException("F02_PARAMETER");return x;}
}
