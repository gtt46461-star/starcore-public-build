package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** KL distillation from an explicitly bound legacy preference projection, plus real-label NLL.
 * Teacher preference is not represented as calibrated hit probability or transferred evidence. */
public final class DistillationFamilyR26 implements OnlineModelsSU2.Model {
    public interface Teacher {double[][] marginal();String identity();}
    private final Teacher teacher;private final String teacherId;private final double[][] bias=new double[10][10],context=new double[100][10];private int[] previous;private long t,distilled;
    public DistillationFamilyR26(Teacher teacher){if(teacher==null||teacher.identity()==null||teacher.identity().isEmpty())throw new IllegalArgumentException("F23_TEACHER_REQUIRED");this.teacher=teacher;teacherId=teacher.identity();}
    private double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)s[p][n]=bias[p][n]+(previous==null?0:context[p*10+n][previous[p]-1]);return s;}
    public int stateWidth(){return 3;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.marginal[i][j];st[0]=(float)Math.log1p(t);st[1]=(float)Math.log1p(distilled);st[2]=0;}
    public void update(int[] draw){OnlineModelsSU2.requireDraw(draw);if(!teacherId.equals(teacher.identity()))throw new IllegalStateException("F23_TEACHER_IDENTITY_DRIFT");double[][] target=teacher.marginal();double alpha=target==null?0:.35;
        if(target!=null){if(target.length!=10)throw new IllegalArgumentException("F23_TEACHER_SHAPE");for(int p=0;p<10;p++){if(target[p].length!=10)throw new IllegalArgumentException("F23_TEACHER_SHAPE");double row=0,col=0;for(int n=0;n<10;n++){double q=target[p][n];if(!Double.isFinite(q)||q<0||q>1)throw new IllegalArgumentException("F23_TEACHER_RANGE");row+=q;col+=target[n][p];}if(Math.abs(row-1)>1e-6||Math.abs(col-1)>1e-6)throw new IllegalArgumentException("F23_TEACHER_INCOHERENT");}}
        ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);for(int p=0;p<10;p++)for(int n=0;n<10;n++){double soft=target==null?0:target[p][n],g=r.marginal[p][n]-((1-alpha)*(draw[p]==n+1?1:0)+alpha*soft);bias[p][n]=bound(bias[p][n]-.02*g);if(previous!=null)context[p*10+n][previous[p]-1]=bound(context[p*10+n][previous[p]-1]-.02*g);}previous=draw.clone();t++;if(target!=null)distilled++;
    }
    private static double bound(double x){return Math.max(-8,Math.min(8,x));}
    public void write(DataOutput o)throws IOException{o.writeInt(0x26230001);o.writeUTF(teacherId);o.writeLong(t);o.writeLong(distilled);o.writeBoolean(previous!=null);if(previous!=null)for(int n:previous)o.writeInt(n);for(double[][] m:new double[][][]{bias,context})for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{if(in.readInt()!=0x26230001||!teacherId.equals(in.readUTF()))throw new IOException("F23_TEACHER_BINDING");t=in.readLong();distilled=in.readLong();if(t<0||distilled<0||distilled>t)throw new IOException("F23_CURSOR");previous=null;if(in.readBoolean()){previous=new int[10];for(int i=0;i<10;i++)previous[i]=in.readInt();OnlineModelsSU2.requireDraw(previous);}for(double[][] m:new double[][][]{bias,context})for(double[] row:m)for(int j=0;j<row.length;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||Math.abs(row[j])>8)throw new IOException("F23_PARAMETER");}}
}
