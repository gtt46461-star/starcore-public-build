package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Scoped ownership and exact state checks, not natural strategy quality or device measurements. */
public final class R31FamilyScopeIsolationTest {
    private static MemoryBudgetR31 memory(AtomicBoolean pressure){return new MemoryBudgetR31(()->new MemoryBudgetR31.Measurement(7,"SYNTHETIC_UNIT",100000000,pressure.get()?85000000:100000,-1,-1,-1,null,System.nanoTime(),0),new MemoryBudgetR31.Config());}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static FeatureCacheR27.FamilyStream stream(FeatureCacheR27 cache,int family)throws Exception{return(FeatureCacheR27.FamilyStream)((Map<?,?>)field(cache,"streams")).get(family);}
    private static byte[] state(FeatureCacheR27.FamilyStream stream)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();stream.model.write(new DataOutputStream(bytes));return bytes.toByteArray();}
    private static long n(Map<String,Object> map,String key){return((Number)map.get(key)).longValue();}
    @Test public void internalFamilyOutputNeverEntersRawMapAndUnchangedLengthPreservesBackingAndState()throws Exception{
        MemoryBudgetR31 memory=memory(new AtomicBoolean());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,272+80*250);ReplayDatasetR27 data=ReplayDatasetR27.synthetic(250,3194,null);
        try(FeatureCacheR27 cache=new FeatureCacheR27(data,8,memory,store);FeatureCacheR27 independent=new FeatureCacheR27(data,8,memory,new SharedFeatureStoreR31(memory,2000000))){
            double[] matrix=null;byte[] before=null;
            for(int position=1;position<=10;position++){
                try(PolicyAstR27.ScopedScoresR31 scope=PolicyAstR27.evalScopedR31(PolicyAstR27.leaf("family",8),cache,position)){
                    double[] expected=independent.leaf("family",position,8,0);for(int i=0;i<expected.length;i++)assertEquals(Double.doubleToLongBits(expected[i]),Double.doubleToLongBits(scope.at(i)));
                    FeatureCacheR27.FamilyStream stream=stream(cache,8);if(matrix==null){matrix=stream.values;before=state(stream);}else{assertSame(matrix,stream.values);assertArrayEquals(before,state(stream));}
                    assertEquals(1,n(store.snapshot(),"workingLoans"));assertEquals(0,((Map<?,?>)field(cache,"cache")).size());
                }
                assertEquals(0,n(store.snapshot(),"workingLoans"));assertEquals(1,n(store.snapshot(),"idleBackings"));
            }
            assertEquals(1,n(store.snapshot(),"backingAllocations"));assertTrue(n(store.snapshot(),"backingReuses")>=9);
            int[][] replacement=new int[data.size()][];for(int row=0;row<replacement.length;row++)replacement[row]=data.draws[row].clone();int old=replacement[0][0];replacement[0][0]=replacement[0][1];replacement[0][1]=old;
            try{stream(cache,8).advance(data.withDraws(replacement));fail("corrected prefix must still be checked before no-op return");}catch(IllegalStateException expected){assertEquals("R30_FAMILY_PREFIX_HASH_CHANGED",expected.getMessage());}
            assertSame(matrix,stream(cache,8).values);assertArrayEquals(before,state(stream(cache,8)));
        }finally{store.close();}
    }
    @Test public void deniedDeepForkLeavesParentStateIntactAndChildUninitialized()throws Exception{
        AtomicBoolean pressure=new AtomicBoolean();MemoryBudgetR31 memory=memory(pressure);SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,2000000);ReplayDatasetR27 data=ReplayDatasetR27.synthetic(250,3195,null);
        try(FeatureCacheR27 parent=new FeatureCacheR27(data,8,memory,store);FeatureCacheR27 child=new FeatureCacheR27(data,8,memory,store)){
            parent.leaf("family",1,8,0);FeatureCacheR27.FamilyStream original=stream(parent,8);byte[] before=state(original);double[] values=original.values;pressure.set(true);
            try{child.extendFrom(parent);fail("real unit admission denial must propagate");}catch(MemoryBudgetR31.CapacityWait expected){assertTrue(expected.getMessage().startsWith("RESOURCE_WAIT:MEMORY_"));}
            assertSame(original,stream(parent,8));assertSame(values,original.values);assertArrayEquals(before,state(original));assertTrue(((Map<?,?>)field(child,"streams")).isEmpty());
        }finally{store.close();}
    }
    @Test public void neuralForkShapeIncludesUnserializedAutodiffWorkspace()throws Exception{
        FeatureCacheR27.FamilyStream neural=new FeatureCacheR27.FamilyStream(3);Method shape=FeatureCacheR27.FamilyStream.class.getDeclaredMethod("modelShapeR31",Object.class);shape.setAccessible(true);long bytes=((Number)shape.invoke(null,neural.model)).longValue();assertTrue("fixed private tape must be counted, not inferred from codec length",bytes>=2129920);byte[] codec=state(neural);assertTrue(bytes>codec.length*10L);
        MemoryBudgetR31 memory=memory(new AtomicBoolean());long before=n(memory.snapshot(),"declaredJavaWorkingBytes");FeatureCacheR27.FamilyStream child=neural.forkR31(memory);try{assertNotSame(neural.model,child.model);assertArrayEquals(codec,state(child));assertTrue(n(memory.snapshot(),"declaredJavaWorkingBytes")-before>=bytes);}finally{child.closeForkOwnerR31();neural.closeForkOwnerR31();}assertEquals(before,n(memory.snapshot(),"declaredJavaWorkingBytes"));
    }
}
