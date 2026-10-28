package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.nio.*;
import java.util.*;
/** Independent serialized-count oracle; tests fixed pre-label node across split thresholds. */
public final class R26SuffixDiagnosticsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static byte[] save(FamilyF12KernelR26 m)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();m.write(new DataOutputStream(b));return b.toByteArray();}
 static FamilyF12KernelR26 restore(byte[] b)throws Exception{FamilyF12KernelR26 m=new FamilyF12KernelR26();m.read(new DataInputStream(new ByteArrayInputStream(b)));return m;}
 static int[] draw(int i){int[] d=new int[10];for(int p=0;p<10;p++)d[p]=(p+i)%10+1;return d;}
 static class Oracle {
  long t;int previous,older;long[] support=new long[111];double[][][] count=new double[111][10][10];
  Oracle(byte[] b)throws Exception{DataInputStream in=new DataInputStream(new ByteArrayInputStream(b));check(in.readInt()==0x26120001,"codec changed");t=in.readLong();previous=in.readInt();older=in.readInt();for(int s=0;s<111;s++){support[s]=in.readLong();for(double[] row:count[s])for(int j=0;j<10;j++)row[j]=in.readDouble();}}
  int selected(){if(previous<0)return 0;int shallow=1+previous,deep=older<0?-1:11+10*older+previous;return deep>=0&&support[shallow]>=20&&support[deep]>=4?deep:shallow;}
  double probability(int s,int p,int n){ArrayList<Integer> chain=new ArrayList<>();for(int x=s;x>0;x=x<11?0:1+(x-11)%10)chain.add(x);double q=(count[0][p][n]+1)/(support[0]+10.0);for(int i=chain.size()-1;i>=0;i--){int x=chain.get(i);q=(count[x][p][n]+10*q)/(support[x]+10.0);}return q;}
  double loss(int s,int[] target){double v=0;for(int p=0;p<10;p++)v-=Math.log(probability(s,p,target[p]-1))/10;return v;}
 }
 public static void main(String[] args)throws Exception{
  FamilyF12KernelR26 m=new FamilyF12KernelR26();int deep=0;double maxError=0;
  for(int i=0;i<300;i++){
   if(i==75||i==230){long t=m.completedTrainingUpdates();m.resetTemporalContext();check(m.completedTrainingUpdates()==t&&m.lastStatisticalTrainingStep()==null,"gap lost count");}
   byte[] before=save(m);Oracle old=new Oracle(before);int selected=old.selected();if(selected>=11)deep++;
   int[] target=draw(i%3);double loss=old.loss(selected,target);m.update(target);Oracle after=new Oracle(save(m));StatisticalTrainingDiagnosticsR26.Step step=m.lastStatisticalTrainingStep();
   maxError=Math.max(maxError,Math.max(Math.abs(loss-step.lossBefore),Math.abs(after.loss(selected,target)-step.lossAfter)));check(maxError<1e-14,"fixed suffix loss differs");
   double stats=0,params=0;for(int s=0;s<111;s++){double ds=after.support[s]-old.support[s];stats+=ds*ds;for(int p=0;p<10;p++)for(int n=0;n<10;n++){double dc=after.count[s][p][n]-old.count[s][p][n];params+=dc*dc;}}
   check(step.statisticDeltaNorm==Math.sqrt(stats+params)&&step.parameterDeltaNorm==Math.sqrt(params),"actual count deltas differ");check(step.update==i+1,"lifetime counter differs");
   FamilyF12KernelR26 alternative=restore(before);alternative.update(draw((i+5)%10));check(step.inputHash.equals(alternative.lastStatisticalTrainingStep().inputHash),"target leaked into input");
   FamilyF12KernelR26 copy=restore(before);check(copy.lastStatisticalTrainingStep()==null,"restore fabricated update");copy.update(target);check(Arrays.equals(save(copy),save(m)),"restored next full state differs");
  }
  check(deep>100,"deep supported splits not exercised");byte[] good=save(m);
  for(int kind=0;kind<3;kind++){byte[] bad=good.clone();if(kind==0)ByteBuffer.wrap(bad).putDouble(bad.length-8,Double.NaN);if(kind==1)bad=Arrays.copyOf(bad,bad.length-1);if(kind==2)ByteBuffer.wrap(bad).putLong(20,-1);try{m.read(new DataInputStream(new ByteArrayInputStream(bad)));throw new AssertionError("corrupt state accepted");}catch(IOException expected){}check(Arrays.equals(good,save(m)),"corrupt restore partly changed state");}
  m=restore(good);ExecutionBudgetR26.enter(new BudgetTokenSU1(30000,1,()->false));boolean yielded=false;try{m.update(draw(0));}catch(BudgetTokenSU1.Yield expected){yielded=true;}finally{ExecutionBudgetR26.leave();}check(yielded&&Arrays.equals(good,save(m))&&m.lastStatisticalTrainingStep()==null,"cancel partially changed state");
  if(args.length>0)java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),good);
  System.out.println("R26_SUFFIX_DIAGNOSTICS_PASS|updates=300|deepNodes="+deep+"|maxLossError="+maxError+"|actualStatistics=true|targetFree=true|restoreGapCancelAtomic=true|scope=SYNTHETIC_NOT_NATURAL_QUALITY");
 }
}
