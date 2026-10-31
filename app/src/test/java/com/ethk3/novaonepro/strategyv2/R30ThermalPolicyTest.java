package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import org.junit.Test;

/** R30-B01-06 thermal tiers: NaN/0.1/0.86/0.96 and the 3-reading recovery hysteresis. */
public class R30ThermalPolicyTest {
    @Test public void tiersAndUnknownHeadroom() {
        ThermalPolicyR29 p = new ThermalPolicyR29();
        p.update(0, Double.NaN, 0); assertEquals(2, p.workers); assertFalse(p.paused());
        p.update(-1, Double.NaN, 1000); assertEquals("unknown keeps current", 2, p.workers);
        p.update(0, -0.5, 2000); assertEquals("negative headroom is unknown", 2, p.workers);
        p.update(0, .1, 3000); assertEquals(2, p.workers); assertEquals("NORMAL", p.reason);
        p.update(0, .86, 4000); assertEquals(2, p.workers);
        p.update(0, .96, 5000); assertEquals(1, p.workers); assertEquals("THERMAL_LIMITED", p.reason);
        p.update(-1, Double.NaN, 6000); assertEquals("unknown never raises to the cap", 1, p.workers);
        assertTrue(p.workers > 0);
    }
    @Test public void recoveryNeedsThreeConsecutiveCoolerReadings() {
        ThermalPolicyR29 p = new ThermalPolicyR29();
        p.update(0, .96, 0); assertEquals(1, p.workers);
        p.update(0, .1, 10_000); p.update(0, .1, 20_000); assertEquals(1, p.workers);
        p.update(0, .97, 30_000); assertEquals("heat resets the count", 1, p.workers);
        p.update(0, .1, 40_000); p.update(0, .1, 55_000); assertEquals(1, p.workers);
        p.update(0, .1, 70_000); assertEquals(2, p.workers);
    }
    @Test public void interruptedSequenceByUnknownRestarts() {
        ThermalPolicyR29 p = new ThermalPolicyR29();
        p.update(3, Double.NaN, 0); assertEquals(1, p.workers);
        p.update(0, .2, 10_000); p.update(0, .2, 20_000); p.update(-1, Double.NaN, 30_000);
        p.update(0, .2, 40_000); assertEquals(1, p.workers);
        p.update(0, .2, 55_000); p.update(0, .2, 70_000); assertEquals(2, p.workers);
    }
}
