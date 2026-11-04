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
    public static void main(String[] args)throws Exception{
        CooperativeContinuationR26.installPermitProvider((owner,name)->{
            if(!held.compareAndSet(0,1))return null;peak.accumulateAndGet(held.get(),Math::max);AtomicBoolean released=new AtomicBoolean();
            return ()->{if(released.compareAndSet(false,true)){held.decrementAndGet();returned.incrementAndGet();}};
        });
        cancelledOwnerCannotBeReplaced(false);cancelledOwnerCannotBeReplaced(true);
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
