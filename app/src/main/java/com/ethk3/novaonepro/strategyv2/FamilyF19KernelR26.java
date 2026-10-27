package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Trainable CP relation tensor U[p,k]V[n,k]A[q,k]B[r,k]C[lag,k]. */
public final class FamilyF19KernelR26 implements TemporalModelR26 {
    private static final int R=3,L=3;private final double[][] u=new double[10][R],v=new double[10][R],a=new double[10][R],b=new double[10][R],c=new double[L][R];
    private final int[][] ring=new int[L][10];private long t;private final double lr;
    public FamilyF19KernelR26(double lr){if(lr<=0||lr>.1||!Double.isFinite(lr))throw new IllegalArgumentException("F19_LR");this.lr=lr;long seed=1719;for(double[][] m:new double[][][]{u,v,a,b,c})for(double[] row:m)for(int k=0;k<R;k++){seed^=seed<<13;seed^=seed>>>7;seed^=seed<<17;row[k]=(Math.floorMod(seed,10000)/10000.0-.5)*.4;}}
    private double[] context(){double[] h=new double[R];for(int l=0;l<Math.min(t,L);l++)for(int q=0;q<10;q++){int n=ring[(int)((t-1-l)%L)][q]-1;for(int k=0;k<R;k++)h[k]+=a[q][k]*b[n][k]*c[l][k];}return h;}
    private double[][] scores(double[] h){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)for(int k=0;k<R;k++)s[p][n]+=u[p][k]*v[n][k]*h[k];return s;}
    public void resetTemporalContext(){t=0;for(int[] row:ring)java.util.Arrays.fill(row,0);}
    public int stateWidth(){return 2;}
    public void predict(float[][] out,float[] st){ExactPermutationR26.Result p=ExactPermutationR26.infer(scores(context()),null);for(int i=0;i<10;i++)for(int j=0;j<10;j++)out[i][j]=(float)p.marginal[i][j];st[0]=(float)Math.log1p(t);st[1]=R;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);double[] h=context(),gh=new double[R];ExactPermutationR26.Result pred=ExactPermutationR26.infer(scores(h),null);double[][] du=new double[10][R],dv=new double[10][R],da=new double[10][R],db=new double[10][R],dc=new double[L][R];
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){double g=pred.marginal[p][n]-(draw[p]==n+1?1:0);for(int k=0;k<R;k++){du[p][k]+=g*v[n][k]*h[k];dv[n][k]+=g*u[p][k]*h[k];gh[k]+=g*u[p][k]*v[n][k];}}
        for(int l=0;l<Math.min(t,L);l++)for(int q=0;q<10;q++){int n=ring[(int)((t-1-l)%L)][q]-1;for(int k=0;k<R;k++){da[q][k]+=gh[k]*b[n][k]*c[l][k];db[n][k]+=gh[k]*a[q][k]*c[l][k];dc[l][k]+=gh[k]*a[q][k]*b[n][k];}}
        double[][][] ms={u,v,a,b,c},gs={du,dv,da,db,dc};for(int j=0;j<ms.length;j++)for(int i=0;i<ms[j].length;i++)for(int k=0;k<R;k++)ms[j][i][k]=Math.max(-3,Math.min(3,ms[j][i][k]-lr*(gs[j][i][k]+1e-5*ms[j][i][k])));
        ring[(int)(t%L)]=draw.clone();t++;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26190001);o.writeDouble(lr);o.writeLong(t);for(int[] row:ring)for(int n:row)o.writeInt(n);for(double[][] m:new double[][][]{u,v,a,b,c})for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26190001||in.readDouble()!=lr)throw new IOException("F19_ABI");t=in.readLong();if(t<0)throw new IOException("F19_CURSOR");for(int[] row:ring)for(int j=0;j<10;j++){int n=in.readInt();if(n<0||n>10)throw new IOException("F19_DRAW");row[j]=n;}for(double[][] m:new double[][][]{u,v,a,b,c})for(double[] row:m)for(int j=0;j<R;j++){double x=in.readDouble();if(!Double.isFinite(x)||Math.abs(x)>3)throw new IOException("F19_STATE");row[j]=x;}}
}
