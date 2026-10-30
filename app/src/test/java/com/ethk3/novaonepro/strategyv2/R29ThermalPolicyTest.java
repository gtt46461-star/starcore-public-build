package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import org.junit.Test;

public class R29ThermalPolicyTest {
    @Test public void higherHeadroomLimitsAndRecoveryHasHysteresis() {
        ThermalPolicyR29 p = new ThermalPolicyR29();
        p.update(0, .1, 0); assertEquals(2, p.workers); assertFalse(p.paused());
        p.update(0, .95, 1000); assertEquals(1, p.workers); assertFalse(p.paused());
        p.update(0, .1, 2000); p.update(0, .1, 31_999); assertEquals(1, p.workers);
        p.update(0, .1, 32_000); assertEquals(2, p.workers);
    }
    @Test public void nanFallsBackToSystemStatusAndSevereKeepsOneWorker() {
        ThermalPolicyR29 p = new ThermalPolicyR29();
        p.update(3, Double.NaN, 0); assertEquals(1, p.workers); assertFalse(p.paused());
        p.update(4, Double.NaN, 1000); assertTrue(p.paused());
        p.update(3, Double.NaN, 2000); assertFalse(p.paused()); assertEquals(1, p.workers);
        p.update(-1, Double.NaN, 90_000); assertEquals(1, p.workers); // unknown is not evidence of cooling
        p.update(0, Double.NaN, 91_000); p.update(0, Double.NaN, 121_000); assertEquals(2, p.workers);
    }
    @Test public void renewedHeatRestartsRecoveryTimer() {
        ThermalPolicyR29 p = new ThermalPolicyR29(); p.update(2, .9, 0); p.update(0, .5, 1000);
        p.update(0, .9, 20_000); p.update(0, .5, 21_000); p.update(0, .5, 50_999); assertEquals(1, p.workers);
        p.update(0, .5, 51_000); assertEquals(2, p.workers);
    }
}
