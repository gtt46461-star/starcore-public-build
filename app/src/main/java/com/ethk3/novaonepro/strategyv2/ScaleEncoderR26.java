package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Exact retained windows; unavailable 8/32/128/512/2048/5000 scales have zero router weight. */
public final class ScaleEncoderR26 implements TemporalModelR26,ScaleInputR26 {
    public static final int[] SCALES={8,32,128,512,2048,5000};
    private final boolean six;private final int[][] ring=new int[5000][10];private final int[][][] counts=new int[6][10][10];
    private final double[] logits=new double[6];private long t;
    public ScaleEncoderR26(boolean six){this.six=six;}
    public int mask(){int mask=0;for(int s=0;s<(six?6:1);s++)if(t>=SCALES[s])mask|=1<<s;return mask;}
    public int configuredScaleMask(){return six?63:1;}
    public int availableScaleMask(){return mask();}
    private double[] weights(){double[] w=new double[6];double z=0,max=-Double.MAX_VALUE;for(int s=0;s<6;s++)if((mask()&(1<<s))!=0)max=Math.max(max,logits[s]);for(int s=0;s<6;s++)if((mask()&(1<<s))!=0){w[s]=Math.exp(logits[s]-max);z+=w[s];}if(z>0)for(int s=0;s<6;s++)w[s]/=z;return w;}
    private double[][] mixed(double[] w){double[][] q=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++){if(mask()==0)q[p][n]=.1;else for(int s=0;s<6;s++)q[p][n]+=w[s]*(counts[s][p][n]+.5)/(SCALES[s]+5.0);}return q;}
    private double[][] score(double[][] q){double[][] score=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)score[p][n]=Math.log(Math.max(1e-12,q[p][n]));return score;}
    public int stateWidth(){return 2;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(score(mixed(weights())),null);for(int i=0;i<10;i++)for(int n=0;n<10;n++)p[i][n]=(float)r.marginal[i][n];st[0]=mask();st[1]=(float)Math.log1p(t);}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);double[] w=weights(),gradient=new double[6];double[][] q=mixed(w);if(mask()!=0){ExactPermutationR26.Result r=ExactPermutationR26.infer(score(q),null);for(int s=0;s<6;s++)if(w[s]>0)for(int p=0;p<10;p++)for(int n=0;n<10;n++)gradient[s]+=(r.marginal[p][n]-(draw[p]==n+1?1:0))*w[s]*((counts[s][p][n]+.5)/(SCALES[s]+5.0)/q[p][n]-1)/10;for(int s=0;s<6;s++)logits[s]=Math.max(-8,Math.min(8,logits[s]-.03*gradient[s]));}
        for(int s=0;s<6;s++){if(t>=SCALES[s]){int[] old=ring[(int)((t-SCALES[s])%5000)];for(int p=0;p<10;p++)counts[s][p][old[p]-1]--;}for(int p=0;p<10;p++)counts[s][p][draw[p]-1]++;}ring[(int)(t%5000)]=draw.clone();t++;
    }
    public void resetTemporalContext(){t=0;for(int[] row:ring)Arrays.fill(row,0);for(int[][] m:counts)for(int[] row:m)Arrays.fill(row,0);}
    public void write(DataOutput out)throws IOException{out.writeInt(0x26e40401);out.writeBoolean(six);out.writeLong(t);for(double x:logits)out.writeDouble(x);for(int[] row:ring)for(int x:row)out.writeInt(x);for(int[][] m:counts)for(int[] row:m)for(int x:row)out.writeInt(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26e40401||in.readBoolean()!=six)throw new IOException("R26_SCALE_ABI");t=in.readLong();if(t<0)throw new IOException("R26_SCALE_CURSOR");for(int i=0;i<6;i++){logits[i]=in.readDouble();if(!Double.isFinite(logits[i])||Math.abs(logits[i])>8)throw new IOException("R26_SCALE_ROUTER");}for(int[] row:ring)for(int i=0;i<10;i++){row[i]=in.readInt();if(row[i]<0||row[i]>10)throw new IOException("R26_SCALE_HISTORY");}for(int s=0;s<6;s++)for(int p=0;p<10;p++){int sum=0;for(int n=0;n<10;n++){counts[s][p][n]=in.readInt();if(counts[s][p][n]<0||counts[s][p][n]>SCALES[s])throw new IOException("R26_SCALE_COUNTS");sum+=counts[s][p][n];}if(sum!=Math.min(t,SCALES[s]))throw new IOException("R26_SCALE_SUPPORT");}}
}
