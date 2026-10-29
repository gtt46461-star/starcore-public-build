package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Contextual soft router over three distinct probability mechanisms, with independent states. */
public final class PortfolioFamilyR26 implements TemporalModelR26 {
    private final OnlineModelsSU2.Model[] models={new FamilyF01KernelR26(),new FamilyF09KernelR26(),new FamilyF11KernelR26(.02)};private final double[][] gate=new double[3][3],momentum=new double[3][3];private double entropy=.0,previousLoss=2.302585;private long t;
    public void resetTemporalContext(){for(OnlineModelsSU2.Model m:models)TemporalResetR26.reset(m);entropy=0;previousLoss=2.302585;}
    public int stateWidth(){return 3;}
    private double[] feature(){return new double[]{1,entropy,Math.max(-2,Math.min(2,previousLoss-2.302585))};}
    private double[] routing(){double[] x=feature(),w=new double[3];double max=-1e30,z=0;for(int i=0;i<3;i++){for(int j=0;j<3;j++)w[i]+=gate[i][j]*x[j];max=Math.max(max,w[i]);}for(int i=0;i<3;i++){w[i]=Math.exp(w[i]-max);z+=w[i];}for(int i=0;i<3;i++)w[i]/=z;return w;}
    private float[][][] tables(){float[][][] p=new float[3][10][10];for(int i=0;i<3;i++)models[i].predict(p[i],new float[models[i].stateWidth()]);return p;}
    public void predict(float[][] p,float[] st){double[] w=routing();float[][][] members=tables();for(int a=0;a<10;a++)for(int n=0;n<10;n++){double x=0;for(int m=0;m<3;m++)x+=w[m]*members[m][a][n];p[a][n]=(float)x;}for(int m=0;m<3;m++)st[m]=(float)w[m];}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);double[] w=routing(),x=feature(),grad=new double[3];float[][][] p=tables();double loss=0,newEntropy=0;for(int a=0;a<10;a++){double target=0;for(int m=0;m<3;m++)target+=w[m]*p[m][a][draw[a]-1];loss-=Math.log(Math.max(1e-12,target))/10;for(int m=0;m<3;m++)grad[m]+=-w[m]*(p[m][a][draw[a]-1]/Math.max(1e-12,target)-1)/10;for(int n=0;n<10;n++){double q=0;for(int m=0;m<3;m++)q+=w[m]*p[m][a][n];newEntropy-=q*Math.log(Math.max(q,1e-12))/(10*Math.log(10));}}
        for(int m=0;m<3;m++)for(int j=0;j<3;j++){momentum[m][j]=.9*momentum[m][j]+.1*grad[m]*x[j];gate[m][j]=Math.max(-8,Math.min(8,gate[m][j]-.04*momentum[m][j]));}for(OnlineModelsSU2.Model m:models)m.update(draw);entropy=newEntropy;previousLoss=loss;t++;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26240001);o.writeLong(t);o.writeDouble(entropy);o.writeDouble(previousLoss);for(double[][] m:new double[][][]{gate,momentum})for(double[] row:m)for(double x:row)o.writeDouble(x);for(OnlineModelsSU2.Model m:models)m.write(o);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26240001)throw new IOException("F24_ABI");t=in.readLong();entropy=in.readDouble();previousLoss=in.readDouble();if(t<0||!Double.isFinite(entropy)||!Double.isFinite(previousLoss))throw new IOException("F24_STATE");for(double[][] m:new double[][][]{gate,momentum})for(double[] row:m)for(int j=0;j<row.length;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||Math.abs(row[j])>100)throw new IOException("F24_GATE");}for(OnlineModelsSU2.Model m:models)m.read(in);}
}
