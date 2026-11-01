package com.ethk3.novaonepro.strategyv2;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public final class WriterRotateTest {
 public static void main(String[] args)throws Exception{
  WriterHoldLedgerR30.rotate();int perThread=80000;AtomicLong sum=new AtomicLong();CountDownLatch start=new CountDownLatch(1);AtomicBoolean done=new AtomicBoolean();
  Runnable record=()->{try{start.await();}catch(InterruptedException e){throw new AssertionError(e);}for(int i=0;i<perThread;i++)WriterHoldLedgerR30.record("CONCURRENT_RECORD",false,0,1,false);};
  Thread a=new Thread(record),b=new Thread(record),rotate=new Thread(()->{try{start.await();}catch(InterruptedException e){throw new AssertionError(e);}while(!done.get()){for(WriterHoldLedgerR30.Stat s:WriterHoldLedgerR30.rotate())sum.addAndGet(s.count());Thread.yield();}});
  a.start();b.start();rotate.start();start.countDown();a.join();b.join();done.set(true);rotate.join();for(WriterHoldLedgerR30.Stat s:WriterHoldLedgerR30.rotate())sum.addAndGet(s.count());
  if(sum.get()!=2L*perThread)throw new AssertionError("ROTATION_LOST_EVENTS expected="+(2L*perThread)+" actual="+sum);
  System.out.println("WRITER_ROTATE_HOST_PASS events="+sum+" lost=0");
 }
}
