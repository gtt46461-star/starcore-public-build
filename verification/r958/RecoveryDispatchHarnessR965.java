package com.ethk3.novaonepro;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Slow receiver work and queue saturation, without Android device claims. */
public final class RecoveryDispatchHarnessR965 {
 static int checks;static void check(boolean x,String label){if(!x)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 public static void main(String[] args)throws Exception {
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
  AtomicInteger finishes=new AtomicInteger(),ran=new AtomicInteger();String caller=Thread.currentThread().getName();AtomicReference<String> worker=new AtomicReference<>();
  RecoveryDispatchR965.submit(()->{worker.set(Thread.currentThread().getName());entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}},()->{finishes.incrementAndGet();finished.countDown();},100);
  check(entered.await(1,TimeUnit.SECONDS)&&!caller.equals(worker.get()),"RECEIVER_WORK_OFF_CALLING_THREAD");
  check(finished.await(1,TimeUnit.SECONDS)&&finishes.get()==1,"PENDING_RESULT_FINISHES_WHILE_WORK_IS_BLOCKED");
  CountDownLatch queuedFinished=new CountDownLatch(1),rejectedFinished=new CountDownLatch(1);
  RecoveryDispatchR965.submit(()->ran.incrementAndGet(),queuedFinished::countDown,100);
  RecoveryDispatchR965.submit(()->{throw new AssertionError("REJECTED_WORK_MUST_NOT_RUN");},rejectedFinished::countDown,100);
  check(rejectedFinished.await(1,TimeUnit.SECONDS),"SATURATED_QUEUE_FINISHES_REJECTED_BROADCAST");
  check(queuedFinished.await(1,TimeUnit.SECONDS),"QUEUED_BROADCAST_DEADLINE_IS_BOUNDED");release.countDown();
  CountDownLatch drained=new CountDownLatch(1);
  // The bounded queue is drained after the blocked worker exits.
  Thread.sleep(30);RecoveryDispatchR965.submit(drained::countDown,()->{},1000);
  check(drained.await(1,TimeUnit.SECONDS)&&ran.get()==0,"EXPIRED_QUEUED_RECOVERY_NEVER_STARTS");
  check(finishes.get()==1,"COMPLETION_AND_TIMEOUT_FINISH_EXACTLY_ONCE");
  System.out.println("R965_RECOVERY_DISPATCH_PASS|checks="+checks+"|BOUNDED_WORKER_NOT_PHYSICAL_ANR_ACCEPTANCE");
 }
}
