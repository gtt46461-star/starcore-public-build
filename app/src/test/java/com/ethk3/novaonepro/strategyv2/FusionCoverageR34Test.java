package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FusionCoverageR34Test {
    @Test public void lowCorrelationCannotHideMissingPeriodsOrOneSidedValue() {
        assertFalse(new FusionStudyR30R1.Pair(300,320,30,90,12,21,.18).complementary());
        assertFalse(new FusionStudyR30R1.Pair(320,320,30,90,0,21,.18).complementary());
        assertTrue(new FusionStudyR30R1.Pair(320,320,30,90,12,21,.18).complementary());
    }
}
