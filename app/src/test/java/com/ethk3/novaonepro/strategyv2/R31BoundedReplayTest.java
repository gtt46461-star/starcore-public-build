package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Production REPLAY integration with synthetic candidates and data; never natural quality evidence. */
public final class R31BoundedReplayTest {
    private static final class BoundedExecutor extends ThreadPoolExecutor {
        final Semaphore permits = new Semaphore(2);
        final AtomicInteger running = new AtomicInteger(), peak = new AtomicInteger(), accepted = new AtomicInteger(), exited = new AtomicInteger();
        final CountDownLatch firstPair = new CountDownLatch(2);
        BoundedExecutor() { super(2, 2, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>()); }
        @Override public void execute(Runnable body) {
            if (!permits.tryAcquire()) throw new RejectedExecutionException("R31_COMPUTE_CAPACITY_WAIT");
            try { super.execute(() -> {
                accepted.incrementAndGet(); int active = running.incrementAndGet(); peak.accumulateAndGet(active, Math::max); firstPair.countDown();
                try { if (!firstPair.await(2, TimeUnit.SECONDS)) throw new AssertionError("parallel pair never started"); body.run(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new CancellationException("fixture wrapper cancelled before body"); }
                finally { running.decrementAndGet(); exited.incrementAndGet(); permits.release(); }
            }); } catch (RuntimeException rejected) { permits.release(); throw rejected; }
        }
    }
    @SuppressWarnings("unchecked") private static List<EvolutionLoopR27.Pending> pending(EvolutionLoopR27 loop) throws Exception {
        Field field=EvolutionLoopR27.class.getDeclaredField("pending");field.setAccessible(true);return (List<EvolutionLoopR27.Pending>)field.get(loop);
    }
    private static EvolutionLoopR27 fixture(FeatureCacheR27 cache, int count) throws Exception {
        EvolutionLoopR27.Config config=new EvolutionLoopR27.Config();config.births=count;config.nullControl=false;
        EvolutionLoopR27 loop=new EvolutionLoopR27(config,cache,null,()->0);
        int[] windows={8,32,128};
        for(int i=0;i<count;i++){
            int position=i%10+1;PolicyAstR27.Node ast=PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",position,windows[count<=2?i%3:(i/10)%3]));
            PolicySpecR27 spec=new PolicySpecR27(ast,position,PolicySpecR27.Rule.QUANTILE,0,.1+(i/30)*.025,1,false,.01,.01,null);
            loop.addPendingForTest(spec);
        }
        // This fixture tests scheduling already-admitted candidates. It makes no birth/novelty assertion.
        for(EvolutionLoopR27.Pending p:pending(loop))p.state="BORN";
        return loop;
    }
    private static void same(ReplayEvaluatorR27.Trace left, ReplayEvaluatorR27.Trace right) {
        assertNotNull(left);assertNotNull(right);assertEquals(left.from,right.from);assertEquals(left.to,right.to);assertEquals(left.position,right.position);
        assertEquals(Double.doubleToLongBits(left.temperature),Double.doubleToLongBits(right.temperature));
        assertArrayEquals(left.skipped,right.skipped);assertArrayEquals(left.act,right.act);assertArrayEquals(left.hit,right.hit);assertArrayEquals(left.k3,right.k3);
        assertArrayEquals(left.pHit,right.pHit,0);assertArrayEquals(left.pActual,right.pActual,0);assertArrayEquals(left.u,right.u,0);
        assertArrayEquals(left.bias,right.bias,0);assertArrayEquals(left.lastProbs,right.lastProbs,0);
    }
    @Test public void all128CandidatesReplayWithTwoHelpersAndMatchSerialExactly() throws Exception {
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(3000,3104,null);
        EvolutionLoopR27 serial=fixture(new FeatureCacheR27(data),128), parallel=fixture(new FeatureCacheR27(data),128);
        serial.stepReplay();BoundedExecutor executor=new BoundedExecutor();parallel.executor=executor;
        try { parallel.stepReplay(); }
        finally { executor.shutdown();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS)); }
        assertEquals(128,executor.accepted.get());assertEquals(128,executor.exited.get());assertEquals(2,executor.peak.get());assertEquals(0,executor.running.get());
        assertEquals(128,parallel.candidateReplayMs.size());assertEquals(128,pending(parallel).size());
        for(int i=0;i<128;i++){
            EvolutionLoopR27.Pending expected=pending(serial).get(i),actual=pending(parallel).get(i);
            assertEquals(expected.spec.policyHash(),actual.spec.policyHash());assertEquals(Double.doubleToLongBits(expected.tau),Double.doubleToLongBits(actual.tau));same(expected.trace,actual.trace);
            assertEquals(parallel.context().split.selectTo,actual.trace.to);
        }
    }
    @Test public void capacityWaitIsBoundedAndKeepsAllCandidatesForRetry() throws Exception {
        BoundedExecutor executor=new BoundedExecutor();CountDownLatch occupied=new CountDownLatch(2),release=new CountDownLatch(1);
        for(int i=0;i<2;i++)executor.submit(()->{occupied.countDown();try{release.await();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        EvolutionLoopR27 loop=fixture(new FeatureCacheR27(ReplayDatasetR27.synthetic(800,3105,null)),8);loop.executor=executor;
        try {
            assertTrue(occupied.await(2,TimeUnit.SECONDS));long start=System.nanoTime();
            try { loop.stepReplay();fail("capacity wait must be explicit"); } catch(CooperativeContinuationR26.CapacityUnavailable expected) { assertEquals("R31_COMPUTE_CAPACITY_WAIT",expected.getMessage()); }
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<500);assertEquals(2,executor.accepted.get());
            for(EvolutionLoopR27.Pending p:pending(loop)){assertEquals("BORN",p.state);assertNull(p.trace);}
            release.countDown();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(executor.running.get()!=0&&System.nanoTime()<end)Thread.yield();
            loop.stepReplay();assertEquals(8,loop.candidateReplayMs.size());for(EvolutionLoopR27.Pending p:pending(loop))assertNotNull(p.trace);
        }finally{release.countDown();executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test public void cancellingParentStopsActualHelperBodiesWithoutContinuation() throws Exception {
        FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(800,3106,null));
        CountDownLatch entered=new CountDownLatch(2),exited=new CountDownLatch(2);AtomicInteger interruptsPreserved=new AtomicInteger();
        cache.override("freq",(feature,position,a,b)->{
            assertFalse(CooperativeContinuationR26.installed());assertNull(ExecutionBudgetR26.current());entered.countDown();
            try { for(;;)CooperativeContinuationR26.poll(); }
            finally { if(Thread.currentThread().isInterrupted())interruptsPreserved.incrementAndGet();exited.countDown(); }
        });
        EvolutionLoopR27 loop=fixture(cache,2);BoundedExecutor executor=new BoundedExecutor();loop.executor=executor;AtomicBoolean parentCancelled=new AtomicBoolean();
        Thread parent=new Thread(()->{try{loop.stepReplay();}catch(CancellationException expected){parentCancelled.set(Thread.currentThread().isInterrupted());}});
        parent.start();
        try{
            assertTrue(entered.await(2,TimeUnit.SECONDS));parent.interrupt();parent.join(2000);assertFalse(parent.isAlive());assertTrue(parentCancelled.get());
            assertTrue("both numerical bodies must execute finally",exited.await(2,TimeUnit.SECONDS));assertEquals(2,interruptsPreserved.get());
        }finally{parent.interrupt();parent.join(2000);executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
        assertEquals(2,executor.exited.get());assertEquals(0,executor.running.get());
    }
    @Test public void helpersInheritSelectRangeAndCannotReadConfirm() throws Exception {
        FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(3000,3107,null));
        EvolutionLoopR27 loop=fixture(cache,2);AtomicInteger rejected=new AtomicInteger();
        PolicySpecR27 policy=pending(loop).get(0).spec;
        cache.override("freq",(feature,position,a,b)->{
            try{loop.context().holdoutTrace(policy,1,new HoldoutLedgerR27());throw new AssertionError("helper escaped evolution range");}
            catch(IllegalStateException expected){assertEquals("R30_CONFIRM_DURING_EVOLUTION",expected.getMessage());rejected.incrementAndGet();}
            return new double[feature.data.size()*10];
        });
        BoundedExecutor executor=new BoundedExecutor();loop.executor=executor;
        try{loop.stepReplay();}finally{executor.shutdown();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
        assertEquals(2,rejected.get());assertEquals(2,loop.candidateReplayMs.size());
    }
}
