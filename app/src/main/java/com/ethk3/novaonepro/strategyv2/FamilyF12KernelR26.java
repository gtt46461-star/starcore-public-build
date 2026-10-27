package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Relation-symbol suffix automaton with supported state splits and hierarchical shrinkage. */
public final class FamilyF12KernelR26 implements OnlineModelsSU2.Model {
    private final double[][][] count=new double[111][10][10];private final long[] support=new long[111];private int previous=-1,older=-1;private long t;
    public int stateWidth(){return 3;}
    private int state(){if(previous<0)return 0;int first=1+previous,deep=older<0?-1:11+older*10+previous;return deep>=0&&support[first]>=20&&support[deep]>=4?deep:first;}
    private double prob(int state,int p,int n){if(state==0)return (count[0][p][n]+1)/(support[0]+10.0);int parent=state<11?0:1+(state-11)%10;return (count[state][p][n]+10*prob(parent,p,n))/(support[state]+10.0);}
    public void predict(float[][] p,float[] st){int state=state();for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)prob(state,i,j);st[0]=state;st[1]=support[state];int nodes=1;for(int i=1;i<111;i++)if(support[i]>0)nodes++;st[2]=nodes;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);int[] states={0,previous<0?-1:1+previous,previous<0||older<0?-1:11+older*10+previous};for(int s:states)if(s>=0){support[s]++;for(int p=0;p<10;p++)count[s][p][draw[p]-1]++;}older=previous;previous=Math.floorMod(draw[0]-draw[1],10);t++;}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26120001);o.writeLong(t);o.writeInt(previous);o.writeInt(older);for(int s=0;s<111;s++){o.writeLong(support[s]);for(double[] row:count[s])for(double x:row)o.writeDouble(x);}}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26120001)throw new IOException("F12_ABI");t=in.readLong();previous=in.readInt();older=in.readInt();if(t<0||previous< -1||previous>9||older< -1||older>9)throw new IOException("F12_STATE");for(int s=0;s<111;s++){support[s]=in.readLong();if(support[s]<0||support[s]>t)throw new IOException("F12_SUPPORT");for(double[] row:count[s])for(int j=0;j<10;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||row[j]<0||row[j]>support[s])throw new IOException("F12_COUNT");}}}
}
