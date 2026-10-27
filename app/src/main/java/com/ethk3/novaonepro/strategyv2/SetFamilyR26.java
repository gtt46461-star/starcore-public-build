package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Full 1200-action risk scoring. F16 adds two soft probability branches and seven support layers.
 * Set utility is never substituted for the selected set's marginal probability. */
public final class SetFamilyR26 implements OnlineModelsSU2.Model,SetReadoutR26 {
    private final String family;private final OnlineModelsSU2.Model left=new FamilyF01KernelR26(),right=new FamilyF11KernelR26(.02);private final double[][] risk=new double[10][57];private final double[] router=new double[2];private long t;
    public SetFamilyR26(String family){if(!family.equals("F15")&&!family.equals("F16"))throw new IllegalArgumentException("R26_SET_FAMILY");this.family=family;}
    public int stateWidth(){return 3;}
    private double route(){return 1/(1+Math.exp(-Math.max(-20,Math.min(20,router[0]))));}
    private float[][] table(){float[][] a=new float[10][10];left.predict(a,new float[left.stateWidth()]);if(family.equals("F16")){float[][] b=new float[10][10];right.predict(b,new float[right.stateWidth()]);double w=route();for(int p=0;p<10;p++)for(int n=0;n<10;n++)a[p][n]=(float)(w*a[p][n]+(1-w)*b[p][n]);}return a;}
    public void predict(float[][] p,float[] st){float[][] q=table();for(int i=0;i<10;i++)System.arraycopy(q[i],0,p[i],0,10);st[0]=(float)Math.log1p(t);st[1]=1200;st[2]=family.equals("F16")?7:1;}
    private static int pair(int a,int b){return 11+a*(19-a)/2+(b-a-1);}
    private double risk(int p,int a,int b,int c,double mass){double z=risk[p][0]+risk[p][1+a]+risk[p][1+b]+risk[p][1+c]+risk[p][pair(a,b)]+risk[p][pair(a,c)]+risk[p][pair(b,c)]+risk[p][56]*mass;return 1/(1+Math.exp(-Math.max(-30,Math.min(30,z))));}
    private double support(float[] row,int a,int b,int c){double score=0;for(int width:new int[]{10,9,8,7,6,5,3}){float[] sorted=row.clone();java.util.Arrays.sort(sorted);double threshold=sorted[10-width],sum=0;for(int n:new int[]{a,b,c})sum+=1/(1+Math.exp(-20*(row[n]-threshold)));score+=Math.log(Math.max(1e-12,sum/3));}return score/7;}
    public Action choose(float[][] p){Action best=null;for(int pos=0;pos<10;pos++){ExecutionBudgetR26.poll();for(int a=0;a<8;a++)for(int b=a+1;b<9;b++)for(int c=b+1;c<10;c++){double q=p[pos][a]+p[pos][b]+p[pos][c],utility=q-.05*risk(pos,a,b,c,q);if(family.equals("F16"))utility+=.01*support(p[pos],a,b,c);if(best==null||utility>best.utility)best=new Action(pos,a,b,c,q,utility);}}return best;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);float[][] p=table();double[][] grad=new double[10][57];for(int pos=0;pos<10;pos++)for(int a=0;a<8;a++)for(int b=a+1;b<9;b++)for(int c=b+1;c<10;c++){double q=p[pos][a]+p[pos][b]+p[pos][c],miss=draw[pos]==a+1||draw[pos]==b+1||draw[pos]==c+1?0:1;double d=(risk(pos,a,b,c,q)-miss)/1200;grad[pos][0]+=d;for(int n:new int[]{a,b,c})grad[pos][1+n]+=d;grad[pos][pair(a,b)]+=d;grad[pos][pair(a,c)]+=d;grad[pos][pair(b,c)]+=d;grad[pos][56]+=d*q;}
        if(family.equals("F16")){float[][] a=new float[10][10],b=new float[10][10];left.predict(a,new float[left.stateWidth()]);right.predict(b,new float[right.stateWidth()]);double w=route(),g=0;for(int pos=0;pos<10;pos++){int n=draw[pos]-1;g-=w*(1-w)*(a[pos][n]-b[pos][n])/Math.max(1e-12,p[pos][n])/10;}router[1]=.9*router[1]+.1*g;router[0]=Math.max(-8,Math.min(8,router[0]-.03*router[1]));}
        for(int pos=0;pos<10;pos++)for(int j=0;j<57;j++)risk[pos][j]=Math.max(-8,Math.min(8,risk[pos][j]-.1*grad[pos][j]));left.update(draw);if(family.equals("F16"))right.update(draw);t++;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26151601);o.writeUTF(family);o.writeLong(t);for(double[] row:risk)for(double x:row)o.writeDouble(x);for(double x:router)o.writeDouble(x);left.write(o);if(family.equals("F16"))right.write(o);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26151601||!family.equals(in.readUTF()))throw new IOException("R26_SET_ABI");t=in.readLong();if(t<0)throw new IOException("R26_SET_CURSOR");for(double[] row:risk)for(int j=0;j<row.length;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||Math.abs(row[j])>8)throw new IOException("R26_SET_PARAMETER");}for(int j=0;j<2;j++){router[j]=in.readDouble();if(!Double.isFinite(router[j])||Math.abs(router[j])>100)throw new IOException("R26_SET_ROUTER");}left.read(in);if(family.equals("F16"))right.read(in);}
}
