package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
public final class RepairCommitGuardsR30Test {
    static String hash(char c,int n){char[] a=new char[n];java.util.Arrays.fill(a,c);return new String(a);}
    @Test public void composedGuardsRetainOriginalOwnerAndRefuseUnknown()throws Exception{
        AtomicInteger ran=new AtomicInteger();ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->{ran.addAndGet(1);return -1;});b.guardBefore(db->{ran.addAndGet(10);return 0;});assertEquals(-1,(int)b.before.call(null));assertEquals(1,ran.get());
        b=new ResearchWriteQueueR27.Batch();b.guardBefore(db->null);b.guardBefore(db->{fail("unknown original guard bypassed");return 0;});assertEquals(-1,(int)b.before.call(null));
        b=new ResearchWriteQueueR27.Batch();b.guardBefore(db->1);b.guardBefore(db->{ran.addAndGet(10);return 0;});assertEquals(0,(int)b.before.call(null));assertEquals(11,ran.get());
    }
    @Test public void retryAndFailurePublishNoPreparedState(){
        byte[] prepared={1,2};CommitEnvelopeR30R1 e=new CommitEnvelopeR30R1(hash('a',64),1,2,prepared);prepared[0]=9;
        assertEquals(CommitEnvelopeR30R1.Outcome.RETRY_LATER,e.attempt((id,f,r,next)->{assertEquals(1,next[0]);next[0]=8;return CommitEnvelopeR30R1.Outcome.RETRY_LATER;}));
        try{e.publishedState();fail("uncommitted visible");}catch(IllegalStateException expected){}
        try{e.attempt((id,f,r,next)->{throw new IllegalStateException("SYNTHETIC_DISK_FULL");});fail();}catch(IllegalStateException expected){}
        assertEquals(CommitEnvelopeR30R1.Outcome.COMMITTED,e.attempt((id,f,r,next)->CommitEnvelopeR30R1.Outcome.COMMITTED));assertEquals(1,e.publishedState()[0]);byte[] visible=e.publishedState();visible[0]=7;assertEquals(1,e.publishedState()[0]);
        assertEquals(CommitEnvelopeR30R1.Outcome.COMMITTED,e.attempt((id,f,r,next)->{fail("second commit");return null;}));
    }
    @Test public void twoThreadsCannotPublishOneEnvelopeTwice()throws Exception{
        CommitEnvelopeR30R1 e=new CommitEnvelopeR30R1(hash('b',64),2,3,new byte[]{4});CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();
        ExecutorService pool=Executors.newSingleThreadExecutor();try{Future<?> first=pool.submit(()->e.attempt((id,f,r,next)->{calls.incrementAndGet();entered.countDown();try{assertTrue(release.await(5,TimeUnit.SECONDS));}catch(InterruptedException x){throw new AssertionError(x);}return CommitEnvelopeR30R1.Outcome.COMMITTED;}));assertTrue(entered.await(5,TimeUnit.SECONDS));assertEquals(CommitEnvelopeR30R1.Outcome.RETRY_LATER,e.attempt((id,f,r,next)->{fail("concurrent sink");return null;}));try{e.publishedState();fail();}catch(IllegalStateException expected){}release.countDown();first.get(5,TimeUnit.SECONDS);assertEquals(1,calls.get());}finally{release.countDown();pool.shutdownNow();}
    }
    @Test public void sourceConflictRequiresNewComputation(){CommitEnvelopeR30R1 e=new CommitEnvelopeR30R1(hash('c',64),3,4,new byte[]{1});assertEquals(CommitEnvelopeR30R1.Outcome.SOURCE_CONFLICT,e.attempt((id,f,r,next)->CommitEnvelopeR30R1.Outcome.SOURCE_CONFLICT));assertEquals(CommitEnvelopeR30R1.Outcome.SOURCE_CONFLICT,e.attempt((id,f,r,next)->{fail();return null;}));try{e.publishedState();fail();}catch(IllegalStateException expected){}}
    @Test public void formalAndSyntheticLabelsNeverDriveAdvice(){
        for(ResearchContractR30R1.Role role:ResearchContractR30R1.Role.values()){ResearchContractR30R1 c=new ResearchContractR30R1(hash('a',64),hash('b',64),"TEST_ABI",hash('c',40),hash('d',64),role,1,10,100,2);if(role==ResearchContractR30R1.Role.TRAIN||role==ResearchContractR30R1.Role.SELECT||role==ResearchContractR30R1.Role.ONLINE_RESEARCH)c.requireAdvice(11,101);else try{c.requireAdvice(11,101);fail(role.toString());}catch(IllegalStateException expected){}try{c.requireAdvice(10,101);fail();}catch(IllegalStateException expected){}}
    }
}
