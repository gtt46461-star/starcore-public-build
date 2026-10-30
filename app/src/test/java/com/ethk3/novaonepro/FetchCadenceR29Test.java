package com.ethk3.novaonepro;
import org.junit.Test;
import static org.junit.Assert.*;
public final class FetchCadenceR29Test {
    @Test public void healthyPeriodIncludesTimeSpentInTheRequest(){FetchCadenceR29 c=new FetchCadenceR29();c.started(1000);assertEquals(700,c.nextDelay(1300));assertEquals(20,c.nextDelay(2500));}
    @Test public void failuresBackOffWithinTheCeiling(){FetchCadenceR29 c=new FetchCadenceR29();for(int i=0;i<12;i++){c.started(i*1000);c.failure(i*1000);assertTrue(c.nextDelay(i*1000)<=30000);}assertTrue(c.backingOff());assertTrue(c.nextDelay(11000)>=25000);}
    @Test public void recoveryRestoresHealthyCadence(){FetchCadenceR29 c=new FetchCadenceR29();c.started(1000);c.failure(1000);c.failure(1000);c.success();assertFalse(c.backingOff());assertEquals(500,c.nextDelay(1500));}
    @Test public void MonotonicResetDoesNotCreateAnUnboundedDelay(){FetchCadenceR29 c=new FetchCadenceR29();c.started(10000);assertEquals(1000,c.nextDelay(10));}
}
