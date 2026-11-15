package com.ethk3.novaonepro.strategyv2;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Controlled synthetic lifecycle evidence; does not establish natural model quality. */
public final class B01C04BaselineRepro {
    static void require(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    static long elapsedMs(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start); }
    public static void main(String[] args) throws Exception {
        ExecutorService control=Executors.newSingleThreadExecutor();
        try {
            Field field=CooperativeContinuationR26.class.getDeclaredField("OWNED");field.setAccessible(true);
            Object owned=field.get(null);
            AtomicReference<Thread> worker=new AtomicReference<>();
            CooperativeContinuationR26<Integer> completed=new CooperativeContinuationR26<>("baseline-cleanup",()->{worker.set(Thread.currentThread());return 7;},()->false,null,System::nanoTime,"baseline-cleanup-owner");
            synchronized(owned) {
                Future<CooperativeContinuationR26.Slice<Integer>> sliced=control.submit(()->completed.slice(20));
                CooperativeContinuationR26.Slice<Integer> result=sliced.get(2,TimeUnit.SECONDS);
                require(result.done&&result.result==7,"return did not complete");
                require(completed.isDone()&&worker.get().isAlive(),"expected done notification before actual worker exit");
                System.out.println("BASELINE_REPRO|doneBeforeWorkerExit=true|registryCleanupBlocked=true|synthetic=true");
            }
            require(CooperativeContinuationR26.awaitOwnerStopped("baseline-cleanup-owner",1000),"cleanup did not finish after registry unlocked");
            AtomicBoolean release=new AtomicBoolean();CountDownLatch entered=new CountDownLatch(1);
            CooperativeContinuationR26<Integer> blocked=new CooperativeContinuationR26<>("baseline-no-poll",()->{entered.countDown();while(!release.get()){try{Thread.sleep(1);}catch(InterruptedException ignored){}}return 1;},()->false,null,System::nanoTime,"baseline-no-poll-owner");
            Future<CooperativeContinuationR26.Slice<Integer>> running=control.submit(()->blocked.slice(200));
            require(entered.await(1,TimeUnit.SECONDS),"body never started");
            long cancelStart=System.nanoTime();blocked.close();long cancelMs=elapsedMs(cancelStart);
            boolean stopped=CooperativeContinuationR26.awaitOwnerStopped("baseline-no-poll-owner",30);
            require(!stopped&&!blocked.isDone(),"cancel unexpectedly meant termination");
            System.out.println("BASELINE_REPRO|cancelMeansTerminated=false|cancelCallMs="+cancelMs+"|boundedJoinFalse=true|synthetic=true");
            Thread.currentThread().interrupt();long interruptStart=System.nanoTime();
            boolean stoppedOnInterrupt=CooperativeContinuationR26.awaitOwnerStopped("baseline-no-poll-owner",30);
            boolean interruptPreserved=Thread.currentThread().isInterrupted();
            require(!stoppedOnInterrupt&&!interruptPreserved,"expected baseline interrupt loss");
            System.out.println("BASELINE_REPRO|awaitOwnerStoppedInterruptPreserved="+interruptPreserved+"|elapsedMs="+elapsedMs(interruptStart)+"|synthetic=true");
            Thread.interrupted();release.set(true);running.get(2,TimeUnit.SECONDS);
            require(CooperativeContinuationR26.awaitOwnerStopped("baseline-no-poll-owner",1000),"released no-poll worker leaked");
            System.out.println("BASELINE_REPRO_COMPLETE|baseline=5237b265a27fe5ad27fd842901c445d5f8c27749|synthetic=true|notNaturalEvidence=true");
        } finally { control.shutdownNow(); }
    }
}
