package com.ethk3.novaonepro.strategyv2;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Runs the actual production TaskContinuation and LegacyReplayContinuation maps with controlled bodies.
 * This harness proves lifecycle races, not SQL fencing, Android scheduling or prediction quality. */
public final class ContinuationOwnerRaceR31HostTest {
    private static final AtomicInteger held=new AtomicInteger(),peak=new AtomicInteger(),returned=new AtomicInteger();
    private static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(1000,1000000,()->false);}
    private static void awaitIgnoringInterrupt(CountDownLatch release){for(;;)try{release.await();return;}catch(InterruptedException ignored){}}
    private static void cancelledOwnerCannotBeReplaced(boolean legacy)throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        LifecycleBodies.action=target->{entered.countDown();awaitIgnoringInterrupt(release);return 1;};
        String task=legacy?"legacy-replay":"host-race";
        RuntimeCoordinatorR26.Claim original=new RuntimeCoordinatorR26.Claim(task,"first-owner",1);
        ExecutorService scheduler=Executors.newSingleThreadExecutor();
        Future<Long> pending=scheduler.submit(()->legacy?LegacyReplayContinuationR26.step(null,original,budget()):TaskContinuationR26.step(null,original,budget(),1));
        try{
            require(entered.await(1,TimeUnit.SECONDS),"worker did not enter");
            long start=System.nanoTime();if(legacy)LegacyReplayContinuationR26.cancel(original.owner);else TaskContinuationR26.cancel(original.owner);
            require(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<250,"cancel blocked behind numerical slice");
            pending.get(1,TimeUnit.SECONDS);
            RuntimeCoordinatorR26.Claim next=new RuntimeCoordinatorR26.Claim(task,"next-owner",2);
            int calls=LifecycleBodies.entered.get();
            long first=legacy?LegacyReplayContinuationR26.step(null,next,budget()):TaskContinuationR26.step(null,next,budget(),2);
            require(first==0&&LifecycleBodies.entered.get()==calls,"new owner started while old body alive");
            require(held.get()==1&&LifecycleBodies.active.get()==1,"cancel freed resources too early");
            release.countDown();require(CooperativeContinuationR26.awaitOwnerStopped(original.owner,1000),"old worker failed to stop");
            LifecycleBodies.action=target->target;
            long value=legacy?LegacyReplayContinuationR26.step(null,next,budget()):TaskContinuationR26.step(null,next,budget(),2);
            require(value==1,"replacement did not run after actual exit");
            require(CooperativeContinuationR26.awaitOwnerStopped(next.owner,1000),"replacement worker leaked");
            System.out.println("OWNER_RACE_PASS|wrapper="+(legacy?"LegacyReplayContinuationR26":"TaskContinuationR26")+"|noReplacementBeforeExit=true|cancelBounded=true|synthetic=true");
        }finally{release.countDown();scheduler.shutdownNow();require(scheduler.awaitTermination(2,TimeUnit.SECONDS),"scheduler leaked");CooperativeContinuationR26.awaitOwnerStopped(original.owner,1000);}
    }
    private static void sameOwnerCancelledContextReloads(boolean legacy)throws Exception{
        String owner=legacy?"same-legacy-owner":"same-task-owner",task=legacy?"legacy-replay":"incumbent-forecast";
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);LifecycleBodies.action=target->{entered.countDown();awaitIgnoringInterrupt(release);return -777;};
        RuntimeCoordinatorR26.Claim original=new RuntimeCoordinatorR26.Claim(task,owner,11);ExecutorService dispatcher=Executors.newSingleThreadExecutor();
        Future<Long> pending=dispatcher.submit(()->legacy?LegacyReplayContinuationR26.step(null,original,budget()):TaskContinuationR26.step(null,original,budget(),5));
        try{
            require(entered.await(1,TimeUnit.SECONDS),"same owner old body not entered");
            require(CooperativeContinuationR26.requestTaskCancellationR31(owner,"starcore-r26-"+task),"named task cancel did not match worker");pending.get(1,TimeUnit.SECONDS);
            require(!CooperativeContinuationR26.isTaskTerminatedR31(owner,"starcore-r26-"+task),"cancel reported actual exit before body release");
            RuntimeCoordinatorR26.Claim next=new RuntimeCoordinatorR26.Claim(task,owner,12);int before=LifecycleBodies.entered.get();
            long paused=legacy?LegacyReplayContinuationR26.step(null,next,budget()):TaskContinuationR26.step(null,next,budget(),5);require(paused==0&&LifecycleBodies.entered.get()==before,"same owner replaced still-live cancelled context");
            release.countDown();require(CooperativeContinuationR26.awaitOwnerStopped(owner,1000),"same owner cancelled old work failed to exit");LifecycleBodies.action=target->target;
            long result=legacy?LegacyReplayContinuationR26.step(null,next,budget()):TaskContinuationR26.step(null,next,budget(),5);require(result==(legacy?1:5),"same owner consumed old cancelled result instead of reloading");require(LifecycleBodies.entered.get()==before+1,"same owner durable reload did not create exactly one new body");require(CooperativeContinuationR26.awaitOwnerStopped(owner,1000),"same owner reload worker leaked");
            System.out.println("SAME_OWNER_CANCELLATION_RELOAD_PASS|wrapper="+(legacy?"LegacyReplayContinuationR26":"TaskContinuationR26")+"|noReplacementBeforeActualExit=true|oldResultDiscarded=true|syntheticLifecycleBody=true");
        }finally{release.countDown();dispatcher.shutdownNow();require(dispatcher.awaitTermination(2,TimeUnit.SECONDS),"same owner dispatcher leaked");CooperativeContinuationR26.awaitOwnerStopped(owner,1000);}
    }
    public static void main(String[] args)throws Exception{
        CooperativeContinuationR26.installPermitProvider((owner,name)->{
            if(!held.compareAndSet(0,1))return null;peak.accumulateAndGet(held.get(),Math::max);AtomicBoolean released=new AtomicBoolean();
            return ()->{if(released.compareAndSet(false,true)){held.decrementAndGet();returned.incrementAndGet();}};
        });
        cancelledOwnerCannotBeReplaced(false);cancelledOwnerCannotBeReplaced(true);
        sameOwnerCancelledContextReloads(false);sameOwnerCancelledContextReloads(true);
        int before=LifecycleBodies.entered.get();
        for(int i=0;i<10000;i++){
            String owner="aba-owner-"+(i%2);long target=i+100L;
            RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("incumbent-forecast",owner,i+3);
            long value=TaskContinuationR26.step(null,claim,budget(),target);
            require(value==target,"wrong owner/target result at "+i+": "+value);
        }
        CooperativeContinuationR26.reapTerminated();
        require(LifecycleBodies.entered.get()-before==10000,"owner switches restarted a computation");
        require(LifecycleBodies.peak.get()==1&&peak.get()==1&&held.get()==0&&LifecycleBodies.active.get()==0,"global compute bound or termination resource accounting failed");
        require(returned.get()==LifecycleBodies.entered.get(),"permit not returned exactly once per actual worker");
        System.out.println("OWNER_TARGET_ABA_PASS|switches=10000|bodyEntries="+(LifecycleBodies.entered.get()-before)+"|maxBodyConcurrency="+LifecycleBodies.peak.get()+"|heldPermits="+held.get()+"|sqlFenceIntegration=NOT_TESTED|androidDevice=NOT_RUN|notNaturalEvidence=true");
    }
}
