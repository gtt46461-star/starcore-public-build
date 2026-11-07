package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import org.junit.Test;

/** R31-B03-C03#A3: settlement work health and prediction-coverage failure are two independent columns. The
 * device report shape (72/72 predictions settled while 36 realtime draws had no pre-draw freeze) must not read
 * as "all healthy", and an empty denominator is unknown, never 0% or 100%. */
public final class R31SettlementCoverageColumnsTest {
    @Test public void healthySettlementDoesNotHidePredictionCoverageGap() {
        SettlementCoverageTruthR906 truth = new SettlementCoverageTruthR906(72, 72, 36, 0, 0, 108, 108, 0);
        assertEquals("settlement work fully closed", 1.0, truth.drawTaskClosure, 0);
        assertEquals("every frozen prediction settled", 1.0, truth.settlementCoverage, 0);
        assertEquals("but only 72 of 108 draws had a pre-draw prediction", 72.0 / 108.0, truth.predictionPresenceCoverage, 1e-12);
        assertEquals("72/108(缺36)", truth.predictionPresenceText());
        String canonical = truth.canonical();
        assertTrue(canonical.contains("|drawTaskClosure=1.0") && canonical.contains("|excludedNoPredraw=36"));
    }
    @Test public void noEligibleDrawIsUnknownNotZeroOrFull() {
        SettlementCoverageTruthR906 empty = new SettlementCoverageTruthR906(0, 0, 0, 0, 0, 0, 0, 0);
        assertTrue(Double.isNaN(empty.predictionPresenceCoverage));
        assertEquals("UNKNOWN_NO_ELIGIBLE_DRAW", empty.predictionPresenceText());
        SettlementCoverageTruthR906 noneFrozen = new SettlementCoverageTruthR906(0, 0, 5, 0, 0, 5, 5, 0);
        assertEquals("draws closed but none predicted is an explicit coverage failure", 0.0, noneFrozen.predictionPresenceCoverage, 0);
        assertEquals("0/5(缺5)", noneFrozen.predictionPresenceText());
        assertEquals(1.0, noneFrozen.drawTaskClosure, 0);
    }
}
