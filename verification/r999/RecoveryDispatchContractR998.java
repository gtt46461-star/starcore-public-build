package com.ethk3.novaonepro;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Executes the production CAS mailbox, worker and completion methods. */
public final class RecoveryDispatchContractR998 {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    private static void await(CountDownLatch latch,String label)throws Exception{check(latch.await(3,TimeUnit.SECONDS),label);}
    public static void main(String[] args)throws Exception{
        boolean rejected=false;try{RecoveryDispatchR965.warmUpOffMain();}catch(IllegalStateException expected){rejected="CP4_RECOVERY_WARMUP_ON_MAIN".equals(expected.getMessage());}
        check(rejected,"MAIN_WARMUP_REJECTED");
        AtomicInteger coldWork=new AtomicInteger(),coldFinishes=new AtomicInteger();
        for(int i=0;i<80;i++)RecoveryDispatchR965.submit(coldWork::incrementAndGet,coldFinishes::incrementAndGet,1);
        check(coldWork.get()==0&&coldFinishes.get()==16,"COLD_INGRESS_BOUNDED_NO_INLINE_WORK");
        CountDownLatch warmed=new CountDownLatch(1);
        new Thread(()->{RecoveryDispatchR965.warmUpOffMain();RecoveryDispatchR965.warmUpOffMain();warmed.countDown();},"host-startup-health").start();
        await(warmed,"OFF_MAIN_WARMUP");
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(coldFinishes.get()<80&&System.nanoTime()<until)Thread.yield();
        check(coldFinishes.get()==80&&coldWork.get()==0,"EXPIRED_MAILBOX_ENTRIES_FINISHED_NOT_EXECUTED");
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
        AtomicInteger finishes=new AtomicInteger(),queuedRuns=new AtomicInteger();
        AtomicReference<String> worker=new AtomicReference<>();String caller=Thread.currentThread().getName();
        RecoveryDispatchR965.submit(()->{worker.set(Thread.currentThread().getName());entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException x){Thread.currentThread().interrupt();}},()->{finishes.incrementAndGet();finished.countDown();},100);
        await(entered,"RECOVERY_WORK_ENTERED");check(!caller.equals(worker.get()),"RECOVERY_WORK_OFF_CALLER");
        await(finished,"DEADLINE_FINISHES_BLOCKED_WORK_CALLBACK");
        check(finishes.get()==1,"TIMEOUT_CALLBACK_ONCE");
        CountDownLatch queued=new CountDownLatch(1),overflow=new CountDownLatch(1);
        RecoveryDispatchR965.submit(queuedRuns::incrementAndGet,queued::countDown,80);
        RecoveryDispatchR965.submit(queuedRuns::incrementAndGet,overflow::countDown,80);
        await(queued,"QUEUED_CALLBACK_BOUNDED");await(overflow,"SATURATED_WORKER_CALLBACK_BOUNDED");
        release.countDown();Thread.sleep(40);
        CountDownLatch drained=new CountDownLatch(1);
        RecoveryDispatchR965.submit(drained::countDown,()->{},1000);await(drained,"WORKER_RECOVERS_AFTER_BLOCKED_WORK");
        check(finishes.get()==1&&queuedRuns.get()==0,"LATE_COMPLETION_AND_EXPIRED_QUEUE_IDEMPOTENT");
        CountDownLatch failedWork=new CountDownLatch(1);
        RecoveryDispatchR965.submit(()->{throw new IllegalStateException("fixture-work-failure");},failedWork::countDown,1000);await(failedWork,"FAILED_WORK_FINISHES_CALLBACK");
        CountDownLatch failedFinish=new CountDownLatch(1);
        RecoveryDispatchR965.submit(()->{},()->{failedFinish.countDown();throw new IllegalStateException("fixture-finish-failure");},1000);await(failedFinish,"FAILED_FINISH_ATTEMPT_RECORDED");
        Thread.sleep(30);CountDownLatch afterFailure=new CountDownLatch(1);
        RecoveryDispatchR965.submit(afterFailure::countDown,()->{},1000);await(afterFailure,"MAILBOX_SURVIVES_CALLBACK_FAILURE");
        int nullRejected=0;try{RecoveryDispatchR965.submit(null,()->{});}catch(IllegalArgumentException x){nullRejected++;}
        try{RecoveryDispatchR965.submit(()->{},null);}catch(IllegalArgumentException x){nullRejected++;}
        check(nullRejected==2,"NULL_CALLBACKS_REJECTED");
        AtomicInteger concurrent=new AtomicInteger(),peak=new AtomicInteger();AtomicBoolean inline=new AtomicBoolean();CountDownLatch allFinished=new CountDownLatch(200);
        for(int i=0;i<200;i++)RecoveryDispatchR965.submit(()->{if(Thread.currentThread().getName().equals(caller))inline.set(true);int now=concurrent.incrementAndGet();peak.accumulateAndGet(now,Math::max);concurrent.decrementAndGet();},allFinished::countDown,1000);
        await(allFinished,"OVERLOAD_ALL_CALLBACKS_FINISHED");check(!inline.get()&&peak.get()<=1,"SINGLE_WORKER_NO_INLINE_OVERLOAD");
        long pumps=Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("CP4-recovery-mailbox")).count();
        check(pumps==1,"WARMUP_IDEMPOTENT_ONE_MAILBOX");
        System.out.println("R998_RECOVERY_DISPATCH|checks="+checks+"|CURRENT_PRODUCTION_HOST_NOT_DEVICE");
    }
}
