package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Typed FEATURE AST -> LOGIT coefficients -> exact probability readout.
 * F14 shares a stateful relation macro across output heads; all lags are strictly positive. */
public final class SymbolicFamilyR26 implements TemporalModelR26,TrainingDiagnosticsR26 {
    enum Type { FEATURE, LOGIT }
    static abstract class Feature {final Type type=Type.FEATURE;abstract double eval(SymbolicFamilyR26 m,int p);}
    static final class Lag extends Feature {final int delay,offset;Lag(int d,int o){if(d<1||d>3||o<0||o>9)throw new IllegalArgumentException("R26_AST_LAG");delay=d;offset=o;}double eval(SymbolicFamilyR26 m,int p){if(m.t<delay)return 0;return (m.ring[(int)((m.t-delay)%3)][(p+offset)%10]-5.5)/4.5;}}
    static final class Product extends Feature {final Feature a,b;Product(Feature a,Feature b){if(a==null||b==null||a.type!=Type.FEATURE||b.type!=Type.FEATURE)throw new IllegalArgumentException("R26_AST_TYPE");this.a=a;this.b=b;}double eval(SymbolicFamilyR26 m,int p){return a.eval(m,p)*b.eval(m,p);}}
    static final class Macro extends Feature {final Feature body;Macro(Feature body){this.body=body;}double eval(SymbolicFamilyR26 m,int p){return Math.tanh(body.eval(m,p)+m.ema[p]);}}
    private final String family;private final Feature[] features;private double[][] weight=new double[100][5],momentum=new double[100][5];private int[][] ring=new int[3][10];private double[] ema=new double[10];private long t,updates;private boolean known=true;private TrainingDiagnosticsR26.Step last;
    public SymbolicFamilyR26(String family){if(!family.equals("F13")&&!family.equals("F14"))throw new IllegalArgumentException("R26_SYMBOLIC_FAMILY");this.family=family;Feature relation=new Product(new Lag(1,0),new Lag(1,1));features=new Feature[]{new Lag(1,0),new Lag(2,0),relation,family.equals("F14")?new Macro(new Product(relation,new Lag(3,2))):new Product(new Lag(1,3),new Lag(2,5))};}
    private double[][] featureTable(){double[][] f=new double[10][5];for(int p=0;p<10;p++){f[p][0]=1;for(int j=0;j<4;j++)f[p][j+1]=features[j].eval(this,p);}return f;}
    private double[][] scores(double[][] f){return scores(weight,f);}
    private double[][] scores(double[][] parameters,double[][] f){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)for(int j=0;j<5;j++)s[p][n]+=parameters[p*10+n][j]*f[p][j];return s;}
    public void resetTemporalContext(){t=0;for(int[] row:ring)java.util.Arrays.fill(row,0);java.util.Arrays.fill(ema,0);last=null;}
    public int stateWidth(){return 2;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(featureTable()),null);for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.marginal[i][j];st[0]=(float)Math.log1p(t);st[1]=family.equals("F14")?1:0;}
    public TrainingDiagnosticsR26.Step lastTrainingStep(){return last;}
    public long completedTrainingUpdates(){return updates;}
    public String optimizerProtocol(){return "SYMBOLIC_EXACT_JOINT_NLL_MOMENTUM_09_01_LR003_CLIP8|"+(known?"KNOWN_LIFETIME":"LEGACY_COUNTER_LOWER_BOUND");}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(t==Long.MAX_VALUE||updates==Long.MAX_VALUE)throw new IllegalStateException("R26_AST_UPDATE_CLOCK");
        double[][] f=featureTable(),beforeScores=scores(f);ExactPermutationR26.Result r=ExactPermutationR26.infer(beforeScores,null);int[] zero=new int[10];for(int p=0;p<10;p++)zero[p]=draw[p]-1;
        double[][] nextWeight=new double[100][5],nextMomentum=new double[100][5];double gradient=0,delta=0;
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)for(int j=0;j<5;j++){double g=(r.marginal[p][n]-(draw[p]==n+1?1:0))*f[p][j];gradient+=g*g;nextMomentum[p*10+n][j]=.9*momentum[p*10+n][j]+.1*g;nextWeight[p*10+n][j]=Math.max(-8,Math.min(8,weight[p*10+n][j]-.03*nextMomentum[p*10+n][j]));double d=nextWeight[p*10+n][j]-weight[p*10+n][j];delta+=d*d;}
        double[][] afterScores=scores(nextWeight,f);ExactPermutationR26.Result after=ExactPermutationR26.infer(afterScores,null);StringBuilder input=new StringBuilder("R26_SYMBOLIC_FIXED_PRE_LABEL_FEATURES_V1|").append(family).append('|').append(t);for(double[] row:f)for(double x:row)input.append('|').append(Double.toHexString(x));
        TrainingDiagnosticsR26.Step step=new TrainingDiagnosticsR26.Step(updates+1,ExactPermutationR26.nll(beforeScores,zero,r),ExactPermutationR26.nll(afterScores,zero,after),Math.sqrt(gradient),Math.sqrt(delta),PolicyIdentityR26.sha256(input.toString()),"EXACT_PERMUTATION_JOINT_NLL_FIXED_PRE_LABEL_TYPED_FEATURES;MOMENTUM_SGD;PARAMETER_DELTA=LOGIT_COEFFICIENTS_EXCLUDING_MOMENTUM_AND_CONTEXT");
        double[] nextEma=ema.clone();if(family.equals("F14"))for(int p=0;p<10;p++)nextEma[p]=.9*ema[p]+.1*(draw[p]-draw[(p+1)%10])/9.0;int[] history=draw.clone();ExecutionBudgetR26.poll();weight=nextWeight;momentum=nextMomentum;ema=nextEma;ring[(int)(t%3)]=history;t++;updates++;last=step;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26131402);o.writeUTF(family);o.writeLong(t);for(int[] row:ring)for(int x:row)o.writeInt(x);for(double x:ema)o.writeDouble(x);for(double[][] m:new double[][][]{weight,momentum})for(double[] row:m)for(double x:row)o.writeDouble(x);o.writeLong(updates);o.writeBoolean(known);}
    public void read(DataInput in)throws IOException{
        int magic=in.readInt();if((magic!=0x26131401&&magic!=0x26131402)||!family.equals(in.readUTF()))throw new IOException("R26_AST_ABI");SymbolicFamilyR26 restored=new SymbolicFamilyR26(family);restored.t=in.readLong();if(restored.t<0)throw new IOException("R26_AST_CURSOR");
        for(int[] row:restored.ring)for(int j=0;j<10;j++){row[j]=in.readInt();if(row[j]<0||row[j]>10)throw new IOException("R26_AST_HISTORY");}for(int i=0;i<10;i++){restored.ema[i]=in.readDouble();if(!Double.isFinite(restored.ema[i])||Math.abs(restored.ema[i])>1)throw new IOException("R26_AST_EMA");}for(double[][] m:new double[][][]{restored.weight,restored.momentum})for(double[] row:m)for(int j=0;j<row.length;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||Math.abs(row[j])>100)throw new IOException("R26_AST_PARAMETER");}
        long counter=restored.t;boolean complete=false;if(magic==0x26131402){counter=in.readLong();int flag=in.readUnsignedByte();if(counter<restored.t||flag>1)throw new IOException("R26_AST_UPDATE_HISTORY");complete=flag==1;}
        t=restored.t;ring=restored.ring;ema=restored.ema;weight=restored.weight;momentum=restored.momentum;updates=counter;known=complete;last=null;
    }
}
