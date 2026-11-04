package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Real model and actual CooperativeContinuation cancellation; no synthetic quality claims. */
public final class FamilyRecoveryR31Host {
 static final class Once implements TemporalModelR26 {
  final OnlineModelsSU2.Model delegate;final String boundary;final AtomicInteger updates=new AtomicInteger();final CountDownLatch entered=new CountDownLatch(1);final AtomicInteger predictions=new AtomicInteger();
  Once(OnlineModelsSU2.Model delegate,String boundary){this.delegate=delegate;this.boundary=boundary;}
  void block(){entered.countDown();for(;;){CooperativeContinuationR26.poll();Thread.yield();}}
  public int stateWidth(){return delegate.stateWidth();}
  public void predict(float[][] p,float[] st){int count=predictions.incrementAndGet();if(count==6&&boundary.equals("before-predict"))block();delegate.predict(p,st);if(count==6&&boundary.equals("after-predict"))block();}
  public void update(int[] row){int count=updates.incrementAndGet();if(count==6&&boundary.equals("before-update"))block();delegate.update(row);if(count==6&&boundary.equals("after-update"))block();}public void resetTemporalContext(){if(delegate instanceof TemporalModelR26)((TemporalModelR26)delegate).resetTemporalContext();}
  public void write(DataOutput out)throws IOException{delegate.write(out);}public void read(DataInput in)throws IOException{delegate.read(in);}
 }
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
  int family=args.length>1?Integer.parseInt(args[1]):12;String boundary=args.length>2?args[2]:"before-predict";ReplayDatasetR27 data=ReplayDatasetR27.synthetic(120,3199,null);Map<String,Object> result=new LinkedHashMap<>();
  try(FeatureCacheR27 interrupted=new FeatureCacheR27(data);FeatureCacheR27 fresh=new FeatureCacheR27(data)){
   Field map=FeatureCacheR27.class.getDeclaredField("streams");map.setAccessible(true);Map<Integer,FeatureCacheR27.FamilyStream> streams=(Map<Integer,FeatureCacheR27.FamilyStream>)map.get(interrupted);
   interrupted.family(1,family);FeatureCacheR27.FamilyStream warmed=streams.get(family); // Construct real stream, then reproduce an uncompleted first advance on its clean real model.
   FeatureCacheR27.FamilyStream stream=new FeatureCacheR27.FamilyStream(family);Once once=new Once(stream.model,boundary);Field model=FeatureCacheR27.FamilyStream.class.getDeclaredField("model");model.setAccessible(true);model.set(stream,once);streams.put(family,stream);warmed.closeForkOwnerR31();
   CooperativeContinuationR26<double[]> work=new CooperativeContinuationR26<>("family-cancellation-proof",()->interrupted.family(1,family),()->false,null,System::nanoTime,"family-cancellation-proof");
   try{for(int slice=0;slice<100&&!once.entered.await(1,TimeUnit.MILLISECONDS);slice++)work.slice(20);if(!once.entered.await(2,TimeUnit.SECONDS))throw new AssertionError("fault boundary was not reached");result.put("fedAtCancel",stream.fed);result.put("publishedValuesAtCancel",stream.values.length);work.close();if(!work.awaitTermination(2000))throw new AssertionError("actual cancellation body did not exit");result.put("oldWorkerActuallyTerminated",work.isTerminated());}
   finally{work.close();if(!work.awaitTermination(2000))throw new AssertionError("cleanup termination");}
   double[] actual=interrupted.family(1,family),expected=fresh.family(1,family);int differences=0,first=-1;for(int i=0;i<expected.length;i++)if(Double.doubleToLongBits(actual[i])!=Double.doubleToLongBits(expected[i])){differences++;if(first<0)first=i;}
   result.put("bitMismatchesAfterRetry",differences);result.put("family",family);result.put("boundary",boundary);ByteArrayOutputStream actualState=new ByteArrayOutputStream(),expectedState=new ByteArrayOutputStream();stream.model.write(new DataOutputStream(actualState));streams=(Map<Integer,FeatureCacheR27.FamilyStream>)map.get(fresh);streams.get(family).model.write(new DataOutputStream(expectedState));result.put("modelCodecEqualAfterRetry",Arrays.equals(actualState.toByteArray(),expectedState.toByteArray()));result.put("firstMismatch",first);result.put("inputRole","SYNTHETIC_ENGINEERING_ONLY");result.put("scope","ACTUAL_DEFAULT_RUNTIME_MODEL_AND_COOPERATIVE_CANCELLATION_EXPLICIT_DELEGATE_BOUNDARY");System.out.println(JsonR27.write(result));
   if(args.length>0&&args[0].equals("require-recovered")&&differences!=0)throw new AssertionError("family recovery differs from fresh");
  }
 }
}
