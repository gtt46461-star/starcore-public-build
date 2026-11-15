package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;import static org.junit.Assert.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
/** Real default process budget and real operation-token exhaustion, synthetic engineering inputs only. */
public final class R31FamilyBudgetResumeTest {
 @SuppressWarnings("unchecked")static FeatureCacheR27.FamilyStream stream(FeatureCacheR27 cache)throws Exception{Field f=FeatureCacheR27.class.getDeclaredField("streams");f.setAccessible(true);return((Map<Integer,FeatureCacheR27.FamilyStream>)f.get(cache)).get(12);}
 static byte[] state(FeatureCacheR27.FamilyStream stream)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();stream.model.write(new DataOutputStream(bytes));return bytes.toByteArray();}
 static void bits(double[] actual,double[] expected){assertEquals(expected.length,actual.length);for(int i=0;i<expected.length;i++)assertEquals("value "+i,Double.doubleToLongBits(expected[i]),Double.doubleToLongBits(actual[i]));}
 @Test public void shortExecutionBudgetsKeepCommittedRowsAndEventuallyMatchFreshWithoutReset()throws Exception{
  ReplayDatasetR27 data=ReplayDatasetR27.synthetic(120,3215,null);try(FeatureCacheR27 cache=new FeatureCacheR27(data);FeatureCacheR27 fresh=new FeatureCacheR27(data)){
   double[] completed=null;int previous=-1,yields=0;
   for(int attempt=0;attempt<200&&completed==null;attempt++){
    ExecutionBudgetR26.enter(new BudgetTokenSU1(1000,3,()->false));try{completed=cache.family(1,12);}catch(BudgetTokenSU1.Yield expected){int fed=stream(cache).fed;assertTrue("each bounded attempt must retain actual progress",fed>previous);previous=fed;yields++;assertEquals("incomplete output must not be published",0,stream(cache).values.length);}finally{ExecutionBudgetR26.leave();}
   }
   assertNotNull("bounded retries must finish",completed);assertTrue(yields>=10);bits(completed,fresh.family(1,12));assertEquals(119,stream(cache).fed);assertArrayEquals(state(stream(fresh)),state(stream(cache)));
  }
 }
 @Test public void deepForkOfAPartiallyCompletedStreamKeepsParentStateAndBothOutputsExact()throws Exception{
  ReplayDatasetR27 full=ReplayDatasetR27.synthetic(251,3216,null);List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(int i=0;i<250;i++)rows.add(new ReplayDatasetR27.Row(full.issues[i],full.ordinals[i],full.draws[i]));ReplayDatasetR27 old=ReplayDatasetR27.of(rows);
  try(FeatureCacheR27 parent=new FeatureCacheR27(old);FeatureCacheR27 child=new FeatureCacheR27(full);FeatureCacheR27 freshOld=new FeatureCacheR27(old);FeatureCacheR27 freshFull=new FeatureCacheR27(full)){
   ExecutionBudgetR26.enter(new BudgetTokenSU1(1000,7,()->false));try{parent.family(1,12);fail();}catch(BudgetTokenSU1.Yield expected){assertTrue(stream(parent).fed>0);assertEquals(0,stream(parent).values.length);}finally{ExecutionBudgetR26.leave();}
   byte[] before=state(stream(parent));child.extendFrom(parent);for(int position=1;position<=10;position++)bits(child.family(position,12),freshFull.family(position,12));assertArrayEquals(before,state(stream(parent)));for(int position=1;position<=10;position++)bits(parent.family(position,12),freshOld.family(position,12));assertArrayEquals(state(stream(freshOld)),state(stream(parent)));
  }
 }
}
