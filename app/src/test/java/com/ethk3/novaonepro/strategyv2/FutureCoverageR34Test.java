package com.ethk3.novaonepro.strategyv2;

import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public final class FutureCoverageR34Test {
    @Test public void anOrdinaryObservedWinRateCannotReplaceCoverage() {
        Map<String,Object> summary=CertificateGateR27.futureSummary(2000,250,100);
        assertEquals(false,summary.get("coveragePass"));
        assertEquals(true,summary.get("expectedPayoffPositive"));
        assertEquals(3/9.9,(Double)summary.get("breakEvenHitRate"),1e-15);
        assertEquals(.125,(Double)summary.get("coverage"),0);
    }
    @Test public void independentWindowsRequireARealDenominatorAndExactCounts() {
        assertEquals(false,CertificateGateR27.futureSummary(0,0,0).get("coveragePass"));
        assertEquals(false,CertificateGateR27.futureSummary(100,101,60).get("coveragePass"));
        assertEquals(true,CertificateGateR27.futureSummary(2000,300,102).get("coveragePass"));
        assertEquals(false,CertificateGateR27.futureSummary(1000,300,90).get("expectedPayoffPositive"));
    }
}
