package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
public final class RepairWriterLedgerR30Test {
    @Test public void concurrentRotateRetains160000ActualRecords()throws Exception{
        WriterHoldLedgerR30.resetForTest();CountDownLatch start=new CountDownLatch(1);AtomicBoolean done=new AtomicBoolean();AtomicLong count=new AtomicLong();
        Runnable record=()->{try{start.await();for(int i=0;i<80000;i++)WriterHoldLedgerR30.record("REPAIR_CONCURRENT",false,1,2,false);}catch(InterruptedException e){throw new AssertionError(e);}};
        Thread a=new Thread(record),b=new Thread(record),rotator=new Thread(()->{while(!done.get()){for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.rotate())count.addAndGet(stat.count());Thread.yield();}});
        a.start();b.start();rotator.start();try{start.countDown();a.join(15000);b.join(15000);assertFalse(a.isAlive());assertFalse(b.isAlive());}finally{done.set(true);rotator.join(15000);}for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.rotate())count.addAndGet(stat.count());assertEquals(160000,count.get());
    }
    @Test public void openUpperPercentileIsAnIntervalNotNineQuintillionMs(){
        WriterHoldLedgerR30.resetForTest();WriterHoldLedgerR30.record("OPEN",true,40000,45000,true);Map<String,Object> row=WriterHoldLedgerR30.toMap(WriterHoldLedgerR30.snapshot().get(0));assertNull(row.get("holdP95"));assertNull(row.get("waitP95"));assertEquals(true,((Map<?,?>)row.get("waitP95Interval")).get("openUpper"));assertEquals(30000L,((Map<?,?>)row.get("waitP95Interval")).get("lowerExclusiveMs"));assertEquals(45000L,row.get("holdMax"));assertFalse(JsonR27.write(row).contains(Long.toString(Long.MAX_VALUE)));assertEquals(1L,row.get("failures"));
    }
}
