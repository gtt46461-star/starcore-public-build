package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30CertificateVersionTest {
    static CertificateGateR27.Evidence passing(String id){CertificateGateR27.Evidence e=new CertificateGateR27.Evidence();e.policyHash=id;e.sequentialState=SequentialQualificationR27.QUALIFIED;e.acts=300;e.hits=120;e.ece=.03;e.worstBlock=.30;e.maxLosingStreak=3;e.abiEquivalent=true;e.pairedLcbVsI00=.02;e.pairedLcbVsNull=.02;e.calibrationQuality=.5;e.logE=20;return e;}
    @Test public void versionIsBoundWithoutChangingAnyNumericGate(){CertificateGateR27.Evidence e=passing("v1");assertEquals("R27_GATE_V1",e.gateVersion);assertTrue(CertificateGateR27.evaluate(e).active);assertEquals(.34,CertificateGateR27.MIN_HR,0);assertEquals(250,CertificateGateR27.MIN_ACTS);assertEquals(.04,CertificateGateR27.MAX_ECE,0);assertEquals(.26,CertificateGateR27.MIN_WORST_BLOCK,0);assertEquals(6,CertificateGateR27.MAX_LOSS_STREAK);for(String version:Arrays.asList("R29_GATE_V2",CertificateGateR27.LEGACY_UNVERSIONED,"",null)){e.gateVersion=version;assertFalse(CertificateGateR27.evaluate(e).active);assertTrue(CertificateGateR27.evaluate(e).failing.contains("gateVersion"));}}
    @Test public void incomparableVersionCannotWinCurrentChoice(){CertificateGateR27.Evidence current=passing("current"),v2=passing("v2");v2.gateVersion="R29_GATE_V2";v2.logE=1e9;assertEquals("current",CertificateGateR27.choose(Arrays.asList(v2,current)));assertNull(CertificateGateR27.choose(Arrays.asList(v2)));}
}
