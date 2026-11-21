package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Executes actual production model/session/optimizer/codec; no database model substitute. */
public final class R335EpochReceiptProbe {
 static int checks,slices,gradients,evals;
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 static AtomNetProgressR33.Frame frame(AtomNetSessionR30 s){return new AtomNetProgressR33.Frame(s.datasetHash,s.phase,s.materializedSamples,s.pretrainSamples,s.trainer.onlineSteps,s.epoch,s.offset,s.fillOffset,s.nextPosition,s.completeIssue,s.partialIssue);}
 static AtomNetSessionR30 resume(AtomNetSessionR30 s){byte[] b=s.encode();AtomNetSessionR30 decoded=AtomNetSessionR30.decode(b,StatsR27.sha256(b));check(Arrays.equals(b,decoded.encode()),"lossless exact codec roundtrip");return decoded;}
 static byte[] canonicalWithoutMeasuredTime(AtomNetSessionR30 s){byte[] b=s.encode();AtomNetSessionR30 c=AtomNetSessionR30.decode(b,StatsR27.sha256(b));c.materializeMs=0;c.trainingMs=0;return c.encode();}
 static AtomNetTrainerR27.Sample sample(int index,String issue){
  int position=index%10+1,number=(index/10+position-1)%10;
  double[] features=new double[7];for(int k=0;k<7;k++)features[k]=((index/10+k)%11-5)/5.0;
  return new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(features,position),number<3?1:0,number,AtomNetSessionR30.eventKey(issue,position));
 }
 static void rejected(Runnable r,String why){try{r.run();throw new AssertionError("accepted "+why);}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
 public static void main(String[] ignored){
  String hash=StatsR27.sha256("B04_FIXED_INDEPENDENT_SESSION_FIXTURE_V1");
  final int train=160,select=80,holdout=40,total=train+select;
  AtomNetTrainerR27.Sample[] rows=new AtomNetTrainerR27.Sample[total];
  for(int i=0;i<total;i++)rows[i]=sample(i,String.format(Locale.ROOT,"20261116%04d",25+i/10));
  AtomNetSessionR30.Samples samples=new AtomNetSessionR30.Samples(){
   public void prefetch(int[] indices){check(indices.length<=64,"bounded prefetch");for(int i:indices)check(i>=0&&i<total,"no formal holdout input");}
   public AtomNetTrainerR27.Sample get(int i){if(i<0||i>=total)throw new IllegalArgumentException("FIXTURE_FORMAL_HOLDOUT_ACCESS");return rows[i];}
  };
  AtomNetSessionR30 uninterrupted=new AtomNetSessionR30(hash,"202611160100",100,17,train,select,holdout);
  // Starts at the exact materialization boundary. Actual feature builder/DB publication are separate integration gates.
  uninterrupted.materializedSamples=total;uninterrupted.cachePeriod=24+total/10;
  AtomNetSessionR30 resumed=resume(uninterrupted);byte[] initial=uninterrupted.trainer.net.encode();
  while(!uninterrupted.ready()){
   check(++slices<500,"bounded run reaches ONLINE");AtomNetProgressR33.Frame before=frame(uninterrupted);
   long old=uninterrupted.pretrainSamples;String phase=uninterrupted.phase;
   int n=uninterrupted.work(samples);
   for(AtomNetTrainerR27.Epoch item:uninterrupted.receipts)System.out.println("R335_ANDROID_EPOCH "+item.epoch+" "+item.split+" "+item.logloss+" "+item.brier);String kind=AtomNetProgressR33.validate(before,frame(uninterrupted),n);
   int n2=resumed.work(samples);check(n==n2,"resumed sample count");
   check(Arrays.equals(canonicalWithoutMeasuredTime(uninterrupted),canonicalWithoutMeasuredTime(resumed)),"Adam RNG replay FTRL shuffle cursor parity");
   resumed=resume(resumed);
   if(uninterrupted.pretrainSamples>old){gradients++;check(kind.equals("PRETRAIN_GRADIENT"),"gradient classified");}
   if(phase.startsWith("EVAL_"))evals++;
   if(gradients==2)check(!Arrays.equals(initial,uninterrupted.trainer.net.encode()),"first two real Adam slices changed weights");
  }
  check(gradients>=2,"at least two real gradient slices");check(evals>0&&uninterrupted.epoch>0,"epochs and evaluation executed");
  check(uninterrupted.consumableR31(),"complete model consumable after train/eval/fill");
  String previous=uninterrupted.completeIssue;
  for(int position=1;position<=10;position++){
   AtomNetTrainerR27.Sample next=sample(240+position-1,"202611160101");AtomNetProgressR33.Frame before=frame(uninterrupted);
   uninterrupted.online(next,"202611160101",101,position);resumed.online(next,"202611160101",101,position);
   check(AtomNetProgressR33.validate(before,frame(uninterrupted),1).equals("ONLINE_GRADIENT"),"online gradient classified");
   check(Arrays.equals(canonicalWithoutMeasuredTime(uninterrupted),canonicalWithoutMeasuredTime(resumed)),"online resumed parity");resumed=resume(resumed);
   if(position<10){check(!uninterrupted.consumableR31(),"positions 1..9 unconsumable");check(uninterrupted.completeIssue.equals(previous),"complete watermark retained");}
   else{check(uninterrupted.consumableR31(),"position 10 consumable");check(uninterrupted.completeIssue.equals("202611160101"),"complete watermark advanced once");}
  }
  final AtomNetSessionR30 done=uninterrupted;
  rejected(()->done.online(sample(240,"202611160101"),"202611160101",101,1),"duplicate issue");
  rejected(()->done.trainer.checkDevelopment(Arrays.asList(rows[0]),ResearchContractR30R1.Role.FUTURE),"formal labels optimizing model");
  byte[] bad=done.encode();bad[bad.length/2]^=1;
  rejected(()->AtomNetSessionR30.decode(bad,StatsR27.sha256(done.encode())),"tampered state");
  rejected(()->samples.get(total),"holdout access");
  // Correctly hashed bytes must still reject impossible phase/cursor/optimizer semantics.
  AtomNetSessionR30 corrupt=resume(done);corrupt.offset=-1;final byte[] negativeOffset=corrupt.encode();rejected(()->AtomNetSessionR30.decode(negativeOffset,StatsR27.sha256(negativeOffset)),"correctly hashed negative cursor");
  corrupt=resume(done);corrupt.partialIssue="202611160102";final byte[] partialWithoutPosition=corrupt.encode();rejected(()->AtomNetSessionR30.decode(partialWithoutPosition,StatsR27.sha256(partialWithoutPosition)),"partial issue without a partial position");
  corrupt=resume(done);corrupt.lastLr=Double.NaN;final byte[] invalidLearningRate=corrupt.encode();rejected(()->AtomNetSessionR30.decode(invalidLearningRate,StatsR27.sha256(invalidLearningRate)),"nonfinite learning rate");
  corrupt=resume(done);corrupt.cachePeriod=23;final byte[] invalidCache=corrupt.encode();rejected(()->AtomNetSessionR30.decode(invalidCache,StatsR27.sha256(invalidCache)),"cache cursor before legal history");

  System.out.println("R335_ANDROID_GRADIENT_TRACE initial="+StatsR27.sha256(initial)+" final="+StatsR27.sha256(done.trainer.net.encode())+" resumed="+StatsR27.sha256(resumed.trainer.net.encode())+" completeState="+StatsR27.sha256(canonicalWithoutMeasuredTime(done))+" restoredCompleteState="+StatsR27.sha256(canonicalWithoutMeasuredTime(resumed))+" fixture="+hash+" trainingRows="+train+" selectRows="+select+" heldOutRows="+holdout);
  for(AtomNetTrainerR27.Epoch row:done.receipts)System.out.println("R335_ANDROID_EPOCH "+row.epoch+" "+row.split+" "+row.logloss+" "+row.brier);
  System.out.println("B04_ACTUAL_SESSION_NUMERICS checks="+checks+" workSlices="+slices+" gradientSlices="+gradients+" evalSlices="+evals+" pretrainSamples="+done.pretrainSamples+" epoch="+done.epoch+" onlineUpdates="+done.trainer.onlineSteps+" weightsSha256="+StatsR27.sha256(done.trainer.net.encode()));
 }
}
