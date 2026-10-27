package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Predeclared parent-bootstrap phase, frozen parent parameters, then gated residual training.
 * No parent evidence transfers. Parent is a new F01 model, not an invented old winning asset. */
public final class ResidualFamilyR26 implements OnlineModelsSU2.Model {
    private static final int BOOTSTRAP=128;private final FamilyF01KernelR26 parent=new FamilyF01KernelR26();private final double[][] residual=new double[100][3];private final double[] gate=new double[2];private final double[] drift=new double[10];private int[] previous;private long t;private String frozenParent="";
    public int stateWidth(){return 3;}
    private double[] feature(int p){return new double[]{1,previous==null?0:(previous[p]-5.5)/4.5,drift[p]};}
    private double g(){double x=0;for(double v:drift)x+=Math.abs(v)/10;return 1/(1+Math.exp(-Math.max(-20,Math.min(20,gate[0]+gate[1]*x))));}
    private double[][] scores(){double[][] s=parent.scores();if(t>=BOOTSTRAP){double g=g();for(int p=0;p<10;p++){double[] x=feature(p);for(int n=0;n<10;n++)for(int j=0;j<3;j++)s[p][n]+=g*residual[p*10+n][j]*x[j];}}return s;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.marginal[i][j];st[0]=(float)Math.log1p(t);st[1]=t>=BOOTSTRAP?1:0;st[2]=(float)g();}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);if(t<BOOTSTRAP){parent.update(draw);if(t+1==BOOTSTRAP)frozenParent=parent.parameterHash();}else{
        if(!parent.parameterHash().equals(frozenParent))throw new IllegalStateException("F22_PARENT_CHANGED");ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);double g=g(),gg=0,context=0;for(double v:drift)context+=Math.abs(v)/10;
        double[][] grad=new double[100][3];for(int p=0;p<10;p++){double[] x=feature(p);for(int n=0;n<10;n++){double loss=r.marginal[p][n]-(draw[p]==n+1?1:0),value=0;for(int j=0;j<3;j++){value+=residual[p*10+n][j]*x[j];grad[p*10+n][j]=loss*g*x[j];}gg+=loss*value*g*(1-g);}}
        for(int i=0;i<100;i++)for(int j=0;j<3;j++)residual[i][j]=Math.max(-8,Math.min(8,residual[i][j]-.02*grad[i][j]));gate[0]=Math.max(-8,Math.min(8,gate[0]-.01*gg));gate[1]=Math.max(-8,Math.min(8,gate[1]-.01*gg*context));parent.observeOnly(draw);
    }for(int p=0;p<10;p++)drift[p]=.9*drift[p]+.1*(previous==null?0:(draw[p]-previous[p])/9.0);previous=draw.clone();t++;}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26220001);o.writeLong(t);o.writeUTF(frozenParent);o.writeBoolean(previous!=null);if(previous!=null)for(int n:previous)o.writeInt(n);for(double[] row:residual)for(double x:row)o.writeDouble(x);for(double x:gate)o.writeDouble(x);for(double x:drift)o.writeDouble(x);parent.write(o);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26220001)throw new IOException("F22_ABI");t=in.readLong();frozenParent=in.readUTF();if(t<0)throw new IOException("F22_CURSOR");previous=null;if(in.readBoolean()){previous=new int[10];for(int i=0;i<10;i++)previous[i]=in.readInt();OnlineModelsSU2.requireDraw(previous);}for(double[] row:residual)for(int j=0;j<3;j++)row[j]=value(in);for(int j=0;j<2;j++)gate[j]=value(in);for(int j=0;j<10;j++)drift[j]=value(in);parent.read(in);if(t>=BOOTSTRAP&&!frozenParent.equals(parent.parameterHash()))throw new IOException("F22_PARENT_HASH");}
    private static double value(DataInput in)throws IOException{double x=in.readDouble();if(!Double.isFinite(x)||Math.abs(x)>100)throw new IOException("F22_STATE");return x;}
}
