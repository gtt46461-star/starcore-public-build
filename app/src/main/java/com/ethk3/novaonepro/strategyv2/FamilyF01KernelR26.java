package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Three-lag sparse cross-position groups with proximal L2 group shrinkage and exact joint head. */
public final class FamilyF01KernelR26 implements TemporalModelR26 {
    private static final int L=3;private final double[][] w=new double[L*10][1000],bias=new double[10][10];private final int[][] ring=new int[L][10];private long t;
    public void resetTemporalContext(){t=0;for(int[] row:ring)java.util.Arrays.fill(row,0);}
    public int stateWidth(){return 2;}
    double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++){s[p][n]=bias[p][n];for(int l=0;l<Math.min(t,L);l++)for(int q=0;q<10;q++)s[p][n]+=w[l*10+q][(p*10+n)*10+ring[(int)((t-1-l)%L)][q]-1]/10;}return s;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);int groups=0;for(double[] row:w){double z=0;for(double x:row)z+=x*x;if(z>1e-20)groups++;}for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.marginal[i][j];st[0]=groups;st[1]=(float)Math.log1p(t);}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);for(int p=0;p<10;p++)for(int n=0;n<10;n++){double g=r.marginal[p][n]-(draw[p]==n+1?1:0);bias[p][n]-=.02*g;for(int l=0;l<Math.min(t,L);l++)for(int q=0;q<10;q++)w[l*10+q][(p*10+n)*10+ring[(int)((t-1-l)%L)][q]-1]-=.02*g/10;}
        for(double[] group:w){double norm=0;for(double x:group)norm+=x*x;double factor=norm==0?0:Math.max(0,1-.02*.0005/Math.sqrt(norm));for(int i=0;i<group.length;i++)group[i]*=factor;}
        ring[(int)(t%L)]=draw.clone();t++;
    }
    void observeOnly(int[] draw){OnlineModelsSU2.requireDraw(draw);ring[(int)(t%L)]=draw.clone();t++;}
    String parameterHash(){StringBuilder b=new StringBuilder("F01_FROZEN_PARAMETERS_V1");for(double[][] m:new double[][][]{bias,w})for(double[] row:m)for(double x:row)b.append('|').append(Double.toHexString(x));return PolicyIdentityR26.sha256(b.toString());}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26010001);o.writeLong(t);for(int[] row:ring)for(int x:row)o.writeInt(x);for(double[] row:bias)for(double x:row)o.writeDouble(x);for(double[] row:w)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26010001)throw new IOException("F01_ABI");t=in.readLong();if(t<0)throw new IOException("F01_CURSOR");for(int[] row:ring)for(int i=0;i<10;i++){row[i]=in.readInt();if(row[i]<0||row[i]>10)throw new IOException("F01_DRAW");}for(double[][] m:new double[][][]{bias,w})for(double[] row:m)for(int i=0;i<row.length;i++){row[i]=in.readDouble();if(!Double.isFinite(row[i])||Math.abs(row[i])>1e6)throw new IOException("F01_STATE");}}
}
