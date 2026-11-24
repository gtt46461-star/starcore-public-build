package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public final class R341QualificationTestCases {
    private R341QualificationTestCases() {}

    private static QualificationInboxR29.Row row(long ordinal,String status,int act,int hit,boolean timely){
        return new QualificationInboxR29.Row(ordinal,Long.toString(ordinal),status,act,1,2,3,hit,hit==1?2:4,timely);
    }

    public static void orderedCalendar(Context ignored){
        SequentialQualificationR27.State seq=new SequentialQualificationR27.State("synthetic-a",41,.15);
        QualificationInboxR29.Row first=row(100,"FROZEN",1,1,true);
        QualificationInboxR29.Row second=row(101,"FROZEN",0,-1,true);
        QualificationInboxR29.Row third=row(102,"FROZEN",1,0,true);
        QualificationInboxR29.Plan gap=QualificationInboxR29.plan(seq,Arrays.asList(first,third),102,20,100);
        assertEquals(QualificationInboxR29.BLOCK_MISSING_OPPORTUNITY,gap.block);assertEquals(101,gap.blockedOrdinal);
        assertEquals(1,seq.n);
        QualificationInboxR29.Plan resumed=QualificationInboxR29.plan(seq,Arrays.asList(first,second,second,third),102,20,100);
        assertEquals(2,resumed.consumed.size());assertEquals(3,seq.n);assertEquals(2,seq.acts);assertEquals(1,seq.hits);
        SequentialQualificationR27.State full=new SequentialQualificationR27.State("synthetic-a",41,.15);
        QualificationInboxR29.plan(full,Arrays.asList(first,second,third),102,20,100);
        assertEquals(full.json(),seq.json());
    }

    public static void lateAndMissingCannotBecomeHold(Context ignored){
        SequentialQualificationR27.State seq=new SequentialQualificationR27.State("synthetic-a",42,.15);
        QualificationInboxR29.Plan late=QualificationInboxR29.plan(seq,Collections.singletonList(row(100,"FROZEN",1,1,false)),100,10,100);
        assertEquals(QualificationInboxR29.BLOCK_LATE_FREEZE,late.block);
        assertEquals(0,seq.n);
        QualificationInboxR29.Plan gap=QualificationInboxR29.plan(seq,Collections.singletonList(row(100,"SKIPPED_GAP",0,-1,true)),100,10,100);
        assertEquals(QualificationInboxR29.BLOCK_INVALID_FREEZE,gap.block);
        assertEquals(0,seq.n);
        QualificationInboxR29.Plan conflict=QualificationInboxR29.plan(seq,Collections.singletonList(
            new QualificationInboxR29.Row(100,"100","FROZEN",1,1,2,3,0,2,true)),100,10,100);
        assertEquals(QualificationInboxR29.BLOCK_CONFLICT,conflict.block);
        assertEquals(0,seq.n);
    }

    public static void zeroActsAreUnobserved(Context ignored){
        SequentialQualificationR27.State seq=new SequentialQualificationR27.State("synthetic-a",43,.15);
        QualificationInboxR29.plan(seq,Arrays.asList(row(100,"FROZEN",0,-1,true),row(101,"ABSTAIN_SET_SUPPORT",0,-1,true)),101,10,100);
        assertEquals(2,seq.n);assertEquals(0,seq.acts);
        Map<String,Object> report=CertificateGateR27.futureSummary(2,0,0);
        assertEquals(0d,(Double)report.get("coverage"),0);
        assertNull(report.get("hitRate"));assertNull(report.get("netRecordedPayoff"));
    }

    private static CertificateGateR27.Evidence certificateEvidence(){
        CertificateGateR27.Evidence e=new CertificateGateR27.Evidence();
        e.policyHash="engineering-asset";e.acts=250;e.hits=90;e.sequentialState=SequentialQualificationR27.QUALIFIED;
        e.ece=.03;e.worstBlock=.27;e.maxLosingStreak=6;e.abiEquivalent=true;e.pairedLcbVsI00=.02;e.pairedLcbVsNull=.01;
        return e;
    }

    public static void mathematicalAndActualIssuanceAreDifferent(Context ignored){
        CertificateGateR27.Verdict v1=CertificateGateR27.evaluate(certificateEvidence());assertTrue(v1.active);
        assertEquals(true,CertificateGateR27.futureSummary(1666,250,90).get("coveragePass"));
        assertEquals(false,CertificateGateR27.futureSummary(1667,250,90).get("coveragePass"));
        GateV2R29.Evidence evidence=new GateV2R29.Evidence();
        SequentialQualificationR27.State seq=new SequentialQualificationR27.State("test",43,.15);
        Map<String,Object> joined=AssetQualityVerdictR341.evaluate(v1,evidence,seq,true,false);
        assertEquals(true,joined.get("v1MathematicalGatesPassed"));assertEquals(false,joined.get("v1ActualIssuancePreconditionsMet"));
        assertEquals(false,joined.get("completeOriginalCalendar"));
        assertEquals(8,((Map<?,?>)joined.get("checks")).size());
    }

    public static void invalidOrMissingGatesDoNotIssue(Context ignored){
        CertificateGateR27.Evidence e=certificateEvidence();e.ece=Double.NaN;e.pairedLcbVsNull=Double.NaN;
        CertificateGateR27.Verdict incomplete=CertificateGateR27.evaluate(e);
        assertFalse(incomplete.active);assertEquals("UNKNOWN",((Map<?,?>)incomplete.gates.get("ece<=0.04")).get("state"));
        assertTrue(incomplete.failing.contains("betterThanShuffledControl"));
        e.ece=0;e.pairedLcbVsNull=.02;e.sequentialState=SequentialQualificationR27.ACCUMULATING;
        assertFalse(CertificateGateR27.evaluate(e).active);
    }

    public static void syntheticScopeSurvivesCheckpoint(Context app)throws Exception{
        Path path=Files.createTempDirectory(app.getCacheDir().toPath(),"test-r341-b09-");
        String source=com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26;
        assertFalse(QualityExperimentR341.run(source,path,1));
        Map<?,?> original=(Map<?,?>)JsonR27.parse(new String(Files.readAllBytes(path.resolve("registration.json")),java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(false,original.get("productionWritable"));
        assertFalse(QualityExperimentR341.run(source,path,1));
        Map<?,?> summary=(Map<?,?>)JsonR27.parse(new String(Files.readAllBytes(path.resolve("summary.json")),java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(120,((Number)summary.get("plannedTrials")).intValue());assertEquals(false,summary.get("realPermission"));
        assertEquals("NOT_PROVEN_SYNTHETIC_ONLY",summary.get("naturalQuality"));
        try{QualityExperimentR341.run(StatsR27.sha256("different").substring(0,40),path,1);fail("rebound original experiment");}
        catch(IllegalStateException refused){assertEquals("R341_FACTORY_REGISTRATION_DRIFT",refused.getMessage());}
    }
}
