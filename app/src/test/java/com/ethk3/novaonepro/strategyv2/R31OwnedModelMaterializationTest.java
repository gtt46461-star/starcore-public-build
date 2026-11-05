package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

/** Real runtime measurements/allocation witnesses; no enlarged heap, fake heap, or forced GC. */
public final class R31OwnedModelMaterializationTest {
    private static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static long n(MemoryBudgetR31 memory,String key){return((Number)memory.snapshot().get(key)).longValue();}
    private static final class Model {final byte[] values=new byte[65536];}
    private static final class DelayedRuntimeProbe implements MemoryBudgetR31.Probe {
        final MemoryBudgetR31.RuntimeProbe actual=new MemoryBudgetR31.RuntimeProbe();
        final CountDownLatch sampled=new CountDownLatch(1),release=new CountDownLatch(1);
        volatile boolean hold;
        public MemoryBudgetR31.Measurement measure(){
            MemoryBudgetR31.Measurement measurement=actual.measure();
            if(hold){sampled.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new AssertionError("real sample release deadline");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
            return measurement;
        }
    }
    @Test public void actualNeuralConstructorKeepsDeclarationWithoutDoubleCountingPrivateTape()throws Exception{
        MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();FeatureCacheR27.FamilyStream stream=new FeatureCacheR27.FamilyStream(3);
        try{
            MemoryBudgetR31.Lease lease=(MemoryBudgetR31.Lease)field(field(stream,"ownerR31"),"model");
            long allocated=((Number)field(lease,"allocatedPayloadBytes")).longValue();
            assertTrue("the real private tape, weights and optimizer must be witnessed",allocated>2L*1024*1024);
            assertTrue("metadata and unallocated safety margin remain reserved",allocated<lease.bytes());
            assertFalse(lease.isClosed());memory.sample();
            assertTrue(n(memory,"allocatedJavaPayloadCoveredByHeapSampleBytes")>=allocated);
            assertEquals("all declarations remain tracked",2674688,lease.bytes());
            assertTrue((Boolean)field(lease,"allocatedPrimitiveGraph"));
        }finally{stream.closeForkOwnerR31();}
    }
    @Test public void actualProbeSampleStartedBeforeAllocationCannotSpendTheGraphPromise()throws Exception{
        DelayedRuntimeProbe probe=new DelayedRuntimeProbe();MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());
        MemoryBudgetR31.Lease model=memory.tryAcquire("actual-model",MemoryBudgetR31.Kind.MODEL,65664,()->false).lease;
        ExecutorService worker=Executors.newSingleThreadExecutor();MemoryBudgetR31.Lease racing=null;
        try{
            probe.hold=true;Future<MemoryBudgetR31.Admission> result=worker.submit(()->memory.tryAcquire("race",MemoryBudgetR31.Kind.RESEARCH,64,()->false));assertTrue(probe.sampled.await(1,TimeUnit.SECONDS));
            Model root=new Model();model.allocatedOwnedPrimitiveGraphR31(root,Collections.singletonList((Object)root.values));
            probe.hold=false;probe.release.countDown();MemoryBudgetR31.Admission admission=result.get(2,TimeUnit.SECONDS);assertTrue(admission.acquired());racing=admission.lease;
            assertEquals("old real sample retains every promise",65728,n(memory,"pendingJavaAllocationBytes"));
            memory.sample();assertEquals("new sample covers only allocated primitive payload",192,n(memory,"pendingJavaAllocationBytes"));
            assertEquals(65536,n(memory,"allocatedJavaPayloadCoveredByHeapSampleBytes"));root.values[0]=42;assertEquals(42,root.values[0]);
        }finally{probe.hold=false;probe.release.countDown();worker.shutdownNow();assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS));if(racing!=null)racing.close();model.close();}
    }
    @Test public void malformedWitnessCannotReducePromisesOrCloseAComplexModelAsOneBacking(){
        MemoryBudgetR31 memory=new MemoryBudgetR31(new MemoryBudgetR31.RuntimeProbe(),new MemoryBudgetR31.Config());Model root=new Model();
        MemoryBudgetR31.Lease lease=memory.tryAcquire("actual-model",MemoryBudgetR31.Kind.MODEL,65664,()->false).lease;
        try{
            for(Collection<Object> bad:Arrays.asList(Arrays.<Object>asList(root.values,root.values),Collections.<Object>singletonList(new Object[1]),Collections.<Object>singletonList(new byte[70000]))){
                try{lease.allocatedOwnedPrimitiveGraphR31(root,bad);fail("invalid proof accepted");}catch(IllegalArgumentException expected){assertEquals(0,((Number)fieldUnchecked(lease,"allocatedPayloadBytes")).longValue());}
            }
            lease.allocatedOwnedPrimitiveGraphR31(root,Collections.singletonList((Object)root.values));
            try{lease.closeWhenPrivateBackingCollectedR31(root);fail("complex graph treated as a single fixed private backing");}catch(IllegalStateException expected){assertEquals("R31_PRIVATE_BACKING_LIFETIME_UNPROVEN",expected.getMessage());}
            assertFalse(lease.isClosed());
        }finally{lease.close();}
    }
    private static Object fieldUnchecked(Object owner,String name){try{return field(owner,name);}catch(Exception e){throw new AssertionError(e);}}
}
