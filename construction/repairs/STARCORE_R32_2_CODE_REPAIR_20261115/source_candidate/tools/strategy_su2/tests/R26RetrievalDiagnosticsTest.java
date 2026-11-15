package com.ethk3.novaonepro.strategyv2;
import java.io.*;import java.lang.reflect.*;import java.nio.*;import java.util.*;
public final class R26RetrievalDiagnosticsTest {
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 static Object get(FamilyF11KernelR26 m,String n)throws Exception{Field f=FamilyF11KernelR26.class.getDeclaredField(n);f.setAccessible(true);return f.get(m);}
 static byte[] save(FamilyF11KernelR26 m)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();m.write(new DataOutputStream(b));return b.toByteArray();}
 static FamilyF11KernelR26 restore(byte[] b)throws Exception{FamilyF11KernelR26 m=new FamilyF11KernelR26(.02);m.read(new DataInputStream(new ByteArrayInputStream(b)));return m;}
 static byte[] old(byte[] b){byte[] v=Arrays.copyOf(b,b.length-9);ByteBuffer.wrap(v).putInt(0x26110001);return v;}
 static int[] draw(Random r){int[] d=new int[10];for(int i=0;i<10;i++)d[i]=i+1;for(int i=9;i>0;i--){int j=r.nextInt(i+1),v=d[i];d[i]=d[j];d[j]=v;}return d;}
 static double loss(FamilyF11KernelR26 m,double[] metric,int[] target)throws Exception{
  int size=(Integer)get(m,"size");int[] previous=(int[])get(m,"previous");int[][] keys=(int[][])get(m,"keys"),labels=(int[][])get(m,"labels");double[] weight=new double[size];double z=0;
  for(int i=0;i<size;i++){double distance=0;for(int p=0;p<10;p++)if(previous[p]!=keys[i][p])distance+=metric[p];weight[i]=Math.exp(-distance);z+=weight[i];}
  double value=0;for(int p=0;p<10;p++){double probability=.01;if(size==0)probability=.1;else for(int i=0;i<size;i++)if(labels[i][p]==target[p])probability+=.9*weight[i]/z;value-=Math.log(probability);}for(double x:metric)value+=.5e-4*x*x;return value;
 }
 public static void main(String[] args)throws Exception{
  Random random=new Random(2611);FamilyF11KernelR26 m=new FamilyF11KernelR26(.02);m.update(draw(random));check(m.completedTrainingUpdates()==0&&m.lastTrainingStep()==null,"seed counted as parameter training");double maxError=0;int coords=0;
  for(int i=0;i<400;i++){
   if(i==70||i==300){long u=m.completedTrainingUpdates();m.resetTemporalContext();check(m.lastTrainingStep()==null,"gap retained stale diagnostic");m.update(draw(random));check(m.completedTrainingUpdates()==u&&m.lastTrainingStep()==null,"gap seed counted as parameter training");}
   byte[] before=save(m);int[] target=draw(random);double[] oldMetric=((double[])get(m,"metric")).clone();double beforeLoss=loss(m,oldMetric,target);double[] finite=new double[10];boolean checkGradient=i==0||i==40||i==280||i==399;
   if(checkGradient)for(int p=0;p<10;p++){double[] plus=oldMetric.clone(),minus=oldMetric.clone();plus[p]+=1e-5;minus[p]-=1e-5;finite[p]=(loss(m,plus,target)-loss(m,minus,target))/2e-5;coords++;}
   m.update(target);TrainingDiagnosticsR26.Step step=m.lastTrainingStep();double[] actual=(double[])get(m,"metric");FamilyF11KernelR26 frozen=restore(before);check(Math.abs(step.lossBefore-beforeLoss)<1e-12&&Math.abs(step.lossAfter-loss(frozen,actual,target))<1e-12,"loss used newly inserted memory or advanced query");double squared=0;for(int p=0;p<10;p++){double delta=actual[p]-oldMetric[p];squared+=delta*delta;if(checkGradient){double expected=Math.max(.001,Math.min(8,oldMetric[p]-.02*finite[p]));maxError=Math.max(maxError,Math.abs(expected-actual[p]));check(Math.abs(expected-actual[p])<1e-9,"metric SGD differs from numerical derivative");}}check(step.parameterDeltaNorm==Math.sqrt(squared),"parameter delta includes memory insertion");if(checkGradient){double g2=0;for(double g:finite)g2+=g*g;check(Math.abs(Math.sqrt(g2)-step.gradientNorm)<1e-8,"gradient norm not measured objective");}
   frozen.update(target);check(Arrays.equals(save(frozen),save(m)),"restored next update differs");FamilyF11KernelR26 alt=restore(before);alt.update(draw(random));check(alt.lastTrainingStep().inputHash.equals(step.inputHash),"target leaked into input hash");
  }
  check(m.completedTrainingUpdates()==400,"update clock differs across gaps");byte[] good=save(m);FamilyF11KernelR26 legacy=restore(old(good));check(legacy.completedTrainingUpdates()==0&&legacy.optimizerProtocol().endsWith("UNKNOWN_PRIOR_UPDATES_COUNTED_SINCE_IMPORT"),"legacy t fabricated update count");check(Arrays.equals(old(good),old(save(legacy))),"legacy numbers lost");int[] next=draw(random);m.update(next);legacy.update(next);check(Arrays.equals(old(save(m)),old(save(legacy)))&&legacy.completedTrainingUpdates()==1,"legacy continuation changed numerical behavior");m=restore(good);
  for(int kind=0;kind<3;kind++){byte[] bad=good.clone();if(kind==0)bad=Arrays.copyOf(bad,bad.length-1);if(kind==1)bad[bad.length-1]=2;if(kind==2)ByteBuffer.wrap(bad).putInt(bad.length-13,99);try{m.read(new DataInputStream(new ByteArrayInputStream(bad)));throw new AssertionError("bad state accepted");}catch(IOException expected){}check(Arrays.equals(good,save(m)),"bad restore partially changed state");}
  ExecutionBudgetR26.enter(new BudgetTokenSU1(30000,1,()->false));boolean yielded=false;try{m.update(next);}catch(BudgetTokenSU1.Yield expected){yielded=true;}finally{ExecutionBudgetR26.leave();}check(yielded&&Arrays.equals(good,save(m))&&m.lastTrainingStep()==null,"interrupted retrieval update partly committed");
  if(args.length>0)java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),good);
  System.out.println("R26_RETRIEVAL_DIAGNOSTICS_PASS|updates=400|finiteDifferenceCoordinates="+coords+"|maxParameterError="+maxError+"|seedNotTraining=true|legacyCounterUnknown=true|fixedQueryAndMemory=true|restoreGapCancelAtomic=true|scope=SYNTHETIC_NOT_NATURAL_QUALITY");
 }
}
