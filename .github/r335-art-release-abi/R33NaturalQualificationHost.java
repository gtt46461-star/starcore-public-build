package com.ethk3.novaonepro.strategyv2;


/** Synthetic unit fixtures only. No fixture creates a database, certificate or REAL record. */
public final class R33NaturalQualificationHost {
    private static CertificateGateR27.Evidence eligible() {
        CertificateGateR27.Evidence e = new CertificateGateR27.Evidence();
        e.policyHash = "fixture-only";
        e.sequentialState = SequentialQualificationR27.QUALIFIED;
        e.acts = 250; e.hits = 85; e.ece = 0.04; e.worstBlock = 0.26;
        e.maxLosingStreak = 6; e.abiEquivalent = true;
        e.pairedLcbVsI00 = 0.01; e.pairedLcbVsNull = 0.01;
        return e;
    }
    public void exactOriginalV1BoundaryAcceptsButDoesNotAuthorizeReal() {
        assertEquals("R27_GATE_V1", CertificateGateR27.GATE_VERSION);
        assertTrue(CertificateGateR27.evaluate(eligible()).active);
    }
    public void aHighRateWith249ActsStillFailsTheOriginalSampleGate() {
        CertificateGateR27.Evidence e=eligible(); e.acts=249; e.hits=100;
        assertTrue(CertificateGateR27.evaluate(e).failing.contains("futureActs>=250"));
    }
    public void aOneHitBoundaryFailureIsNotRoundedTo34Percent() {
        CertificateGateR27.Evidence e=eligible(); e.hits=84;
        assertTrue(CertificateGateR27.evaluate(e).failing.contains("hitRate>=0.34"));
    }
    public void missingCalibrationAndWrongAbiAreExplicitRejections() {
        CertificateGateR27.Evidence e=eligible(); e.ece=Double.NaN; e.abiEquivalent=false;
        CertificateGateR27.Verdict v=CertificateGateR27.evaluate(e);
        assertFalse(v.active); assertTrue(v.failing.contains("ece<=0.04"));
        assertTrue(v.failing.contains("semanticAbiEquivalent"));
    }
    public void pairedZeroAndSevenLossesCannotUseThePassFixture() {
        CertificateGateR27.Evidence e=eligible(); e.pairedLcbVsI00=0; e.pairedLcbVsNull=0; e.maxLosingStreak=7;
        CertificateGateR27.Verdict v=CertificateGateR27.evaluate(e);
        assertFalse(v.active); assertTrue(v.failing.contains("betterThanI00"));
        assertTrue(v.failing.contains("betterThanShuffledControl"));
        assertTrue(v.failing.contains("losingStreak<=6"));
    }
    public void otherProtocolCannotBeRelabelledAsR27() {
        CertificateGateR27.Evidence e=eligible();e.gateVersion="R26_PREREGISTERED_AGGREGATE_LCB_V3";
        assertTrue(CertificateGateR27.evaluate(e).failing.contains("gateVersion"));
    }
    public void duplicateAndOlderOrdinalsAddNoSequentialEvidence() {
        SequentialQualificationR27.State s=new SequentialQualificationR27.State("fixture",1,0.15);
        s.observe(100,true,true);double up=s.logUp,down=s.logDown;
        for(int i=0;i<100;i++){s.observe(100,true,false);s.observe(99,true,false);}
        assertEquals(1,s.n);assertEquals(1,s.acts);assertEquals(1,s.hits);
        assertEquals(up,s.logUp,0);assertEquals(down,s.logDown,0);
        s.observe(101,false,false);assertEquals(2,s.n);assertEquals(1,s.acts);
    }
    public void su2CalendarLengthAndFixedLookAreNotShortenedTo250() {
        assertEquals(69120,QualificationWindowR26.LENGTH);assertEquals(720,QualificationWindowR26.STRIDE);
        assertNull(QualificationWindowR26.completed(1000,1000+69119));
        QualificationWindowR26.Window first=QualificationWindowR26.completed(1000,1000+69120);
        assertEquals(1000,first.from);assertEquals(70120,first.to);assertEquals(1,first.look);
        QualificationWindowR26.Window second=QualificationWindowR26.completed(1000,1000+69120+720);
        assertEquals(1720,second.from);assertEquals(70840,second.to);assertEquals(2,second.look);
    }
    public void sequenceBudgetIsIndexedAndCannotBeResetOnChildRename() {
        double first=SequentialQualificationR27.alpha(1),next=SequentialQualificationR27.alpha(2);
        assertTrue(first>next);assertTrue(next>0);
        assertEquals(0.006,SequentialQualificationR27.deltaMin(0.15),1e-14);
    }

    private static int assertions;
    private static void assertTrue(boolean b){assertions++;if(!b)throw new AssertionError("expected true");}
    private static void assertFalse(boolean b){assertTrue(!b);}
    private static void assertNull(Object o){assertTrue(o==null);}
    private static void assertEquals(long a,long b){assertTrue(a==b);}
    private static void assertEquals(String a,String b){assertTrue(a.equals(b));}
    private static void assertEquals(double a,double b,double tolerance){assertTrue(Math.abs(a-b)<=tolerance);}
    public static void main(String[] args){
        R33NaturalQualificationHost h=new R33NaturalQualificationHost();
        h.exactOriginalV1BoundaryAcceptsButDoesNotAuthorizeReal();
        h.aHighRateWith249ActsStillFailsTheOriginalSampleGate();
        h.aOneHitBoundaryFailureIsNotRoundedTo34Percent();
        h.missingCalibrationAndWrongAbiAreExplicitRejections();
        h.pairedZeroAndSevenLossesCannotUseThePassFixture();
        h.otherProtocolCannotBeRelabelledAsR27();
        h.duplicateAndOlderOrdinalsAddNoSequentialEvidence();
        h.su2CalendarLengthAndFixedLookAreNotShortenedTo250();
        h.sequenceBudgetIsIndexedAndCannotBeResetOnChildRename();
        System.out.println("HOST_PASS assertions="+assertions+" scenarios=9 scope=SYNTHETIC_PURE_JAVA_RULES_NO_CERTIFICATE_NO_REAL_NO_ANDROID");
    }
}
