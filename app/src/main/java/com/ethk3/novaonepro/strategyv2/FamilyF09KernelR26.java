package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Explicit-duration semi-Markov filter, exact unary permutation emissions and online gradients. */
public final class FamilyF09KernelR26 implements TemporalModelR26 {
    private static final int K=3,D=32;private final double[][] logits=new double[K][100],prior=new double[K][D],post=new double[K][D],exit=new double[K][D],stay=new double[K][D];private long t;
    public FamilyF09KernelR26(){for(int s=0;s<K;s++){prior[s][0]=1.0/K;for(int d=0;d<D;d++){exit[s][d]=1;stay[s][d]=9+s*3;}for(int j=0;j<100;j++)logits[s][j]=.03*Math.sin((s+1)*(j+1));}}
    public void resetTemporalContext(){for(int s=0;s<K;s++){java.util.Arrays.fill(prior[s],0);java.util.Arrays.fill(post[s],0);prior[s][0]=1.0/K;}}
    public int stateWidth(){return 3;}
    private double hazard(int s,int d){return d==D-1?1:exit[s][d]/(exit[s][d]+stay[s][d]);}
    private ExactPermutationR26.Result[] emissions(){ExactPermutationR26.Result[] out=new ExactPermutationR26.Result[K];for(int s=0;s<K;s++){double[][] a=new double[10][10];for(int p=0;p<10;p++)System.arraycopy(logits[s],p*10,a[p],0,10);out[s]=ExactPermutationR26.infer(a,null);}return out;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result[] e=emissions();for(int i=0;i<10;i++)for(int j=0;j<10;j++){double v=0;for(int s=0;s<K;s++)for(int d=0;d<D;d++)v+=prior[s][d]*e[s].marginal[i][j];p[i][j]=(float)v;}double duration=0;for(int s=0;s<K;s++){double v=0;for(int d=0;d<D;d++){v+=prior[s][d];duration+=d*prior[s][d];}st[s]=(float)v;}st[2]=(float)duration;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);ExactPermutationR26.Result[] e=emissions();double[] ll=new double[K],likelihood=new double[K],gamma=new double[K];double max=-1e100,z=0;for(int s=0;s<K;s++){ll[s]=-e[s].logZ;for(int p=0;p<10;p++)ll[s]+=logits[s][p*10+draw[p]-1];max=Math.max(max,ll[s]);}for(int s=0;s<K;s++){likelihood[s]=Math.exp(ll[s]-max);for(int d=0;d<D;d++)z+=prior[s][d]*likelihood[s];}if(!(z>0))throw new IllegalStateException("F09_ZERO_MASS");
        if(t>0)for(int s=0;s<K;s++)for(int d=0;d<D;d++){double h=hazard(s,d),ex=0,kept=d<D-1?post[s][d]*(1-h)*likelihood[s]/z:0;for(int n=0;n<K;n++)if(n!=s)ex+=post[s][d]*h/(K-1)*likelihood[n]/z;exit[s][d]+=ex;stay[s][d]+=kept;}
        for(int s=0;s<K;s++)for(int d=0;d<D;d++){post[s][d]=prior[s][d]*likelihood[s]/z;gamma[s]+=post[s][d];prior[s][d]=0;}
        for(int s=0;s<K;s++){for(int p=0;p<10;p++)for(int n=0;n<10;n++){int j=p*10+n;double g=gamma[s]*(e[s].marginal[p][n]-(draw[p]==n+1?1:0));logits[s][j]=Math.max(-8,Math.min(8,logits[s][j]-.03*(g+1e-5*logits[s][j])));}for(int d=0;d<D;d++){double h=hazard(s,d);if(d<D-1)prior[s][d+1]+=post[s][d]*(1-h);for(int n=0;n<K;n++)if(n!=s)prior[n][0]+=post[s][d]*h/(K-1);}}t++;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26090001);o.writeLong(t);for(double[][] m:new double[][][]{logits,prior,post,exit,stay})for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26090001)throw new IOException("F09_ABI");t=in.readLong();if(t<0)throw new IOException("F09_CURSOR");for(double[][] m:new double[][][]{logits,prior,post,exit,stay})for(double[] row:m)for(int j=0;j<row.length;j++){double x=in.readDouble();if(!Double.isFinite(x)||Math.abs(x)>1e12)throw new IOException("F09_STATE");row[j]=x;}double z=0;for(double[] row:prior)for(double x:row){if(x<0||x>1)throw new IOException("F09_POSTERIOR");z+=x;}if(Math.abs(z-1)>1e-8)throw new IOException("F09_POSTERIOR_MASS");}
}
