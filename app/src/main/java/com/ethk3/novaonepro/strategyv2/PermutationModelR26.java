package com.ethk3.novaonepro.strategyv2;

import java.io.*;

/** F17: trained cross-position lag-one scores with exact legal-permutation readout.
 * Online SGD minimizes permutation NLL; no row-softmax probability substitution. */
public final class PermutationModelR26 implements OnlineModelsSU2.Model {
    private final double lr;private final double[][] bias=new double[10][10];
    private final double[][][] weights=new double[100][10][10];
    private int[] previous;private long updates;private double lastLoss;
    public PermutationModelR26(double lr){if(!Double.isFinite(lr)||lr<=0||lr>0.1)throw new IllegalArgumentException("R26_MATCH_LR");this.lr=lr;}
    public int stateWidth(){return 3;}
    private double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++){s[p][n]=bias[p][n];if(previous!=null)for(int q=0;q<10;q++)s[p][n]+=weights[p*10+n][q][previous[q]-1]/10;}return s;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.marginal[i][j];st[0]=(float)Math.log1p(updates);st[1]=(float)lastLoss;st[2]=previous==null?0:1;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);double[][] s=scores();ExactPermutationR26.Result r=ExactPermutationR26.infer(s,null);int[] zero=new int[10];for(int i=0;i<10;i++)zero[i]=draw[i]-1;lastLoss=ExactPermutationR26.nll(s,zero,r);
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){double gradient=r.marginal[p][n]-(draw[p]-1==n?1:0);bias[p][n]=clamp(bias[p][n]-lr*(gradient+1e-4*bias[p][n]));if(previous!=null)for(int q=0;q<10;q++){int x=previous[q]-1;weights[p*10+n][q][x]=clamp(weights[p*10+n][q][x]-lr*(gradient/10+1e-4*weights[p*10+n][q][x]));}}
        previous=draw.clone();updates++;
    }
    private static double clamp(double x){return Math.max(-12,Math.min(12,x));}
    public void write(DataOutput out)throws IOException{out.writeInt(0x52323617);out.writeDouble(lr);out.writeLong(updates);out.writeDouble(lastLoss);out.writeBoolean(previous!=null);if(previous!=null)for(int x:previous)out.writeInt(x);for(double[] r:bias)for(double x:r)out.writeDouble(x);for(double[][] m:weights)for(double[] r:m)for(double x:r)out.writeDouble(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x52323617||Double.doubleToRawLongBits(in.readDouble())!=Double.doubleToRawLongBits(lr))throw new IOException("R26_MATCH_ABI");updates=in.readLong();lastLoss=finite(in.readDouble());if(updates<0)throw new IOException("R26_CURSOR");previous=null;if(in.readBoolean()){previous=new int[10];for(int i=0;i<10;i++)previous[i]=in.readInt();try{OnlineModelsSU2.requireDraw(previous);}catch(IllegalArgumentException e){throw new IOException("R26_DRAW_STATE",e);}}for(double[] r:bias)for(int i=0;i<r.length;i++)r[i]=finite(in.readDouble());for(double[][] m:weights)for(double[] r:m)for(int i=0;i<r.length;i++)r[i]=finite(in.readDouble());}
    private static double finite(double x)throws IOException{if(!Double.isFinite(x)||Math.abs(x)>1e9)throw new IOException("R26_NON_FINITE_STATE");return x;}
}
