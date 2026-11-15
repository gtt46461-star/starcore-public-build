package com.ethk3.novaonepro.strategyv2;

import org.junit.Before;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class R31WriterOutcomeCountsTest {
    @Before public void reset(){WriterHoldLedgerR30.resetForTest();}
    static Map<String,Object> row(long count,String histogram){Map<String,Object> r=new LinkedHashMap<>();r.put("count",count);r.put("failures",99L);r.put("histogram_json",histogram);return r;}
    static Map<String,Object> project(Map<String,Object> row){Map<String,Object> data=new LinkedHashMap<>();data.put("writerWindows",Collections.singletonList(row));ReportSnapshotR30.writerOutcomeViewsR31(data);return row;}
    @Test public void failedDiagnosticOnCommittedTransactionIsNotRollback(){
        WriterHoldLedgerR30.record("COMMITTED_DIAGNOSTIC",false,1,2,true,"COMMITTED_DIAGNOSTIC_FAILED");
        WriterHoldLedgerR30.Stat stat=WriterHoldLedgerR30.snapshot().get(0);
        Map<String,Object> report=project(row(stat.count(),JsonR27.write(stat.histogram())));
        assertEquals(1,stat.failures());assertEquals(0L,report.get("rolledBack"));
        assertEquals("COMPLETE_OBSERVED_TRANSACTION_OUTCOMES",report.get("rollbackEvidenceState"));
    }
    @Test public void unknownCommitDoesNotBecomeZeroOrAFailureCount(){
        WriterHoldLedgerR30.record("MIXED",false,1,2,false,"COMMITTED");
        WriterHoldLedgerR30.record("MIXED",false,1,2,true,"ROLLED_BACK");
        WriterHoldLedgerR30.record("MIXED",false,1,2,true,"UNKNOWN");
        WriterHoldLedgerR30.Stat stat=WriterHoldLedgerR30.snapshot().get(0);
        Map<String,Object> report=project(row(3,JsonR27.write(stat.histogram())));
        assertEquals(2,stat.failures());assertEquals(1L,report.get("rolledBackKnown"));
        assertEquals(1L,report.get("commitOutcomeUnknown"));assertNull(report.get("rolledBack"));
        assertEquals("PARTIAL_OBSERVED_TRANSACTION_OUTCOMES",report.get("rollbackEvidenceState"));
    }
    @Test public void legacyCallerAndLegacyPersistedWindowStayUnknown(){
        WriterHoldLedgerR30.record("LEGACY_CALLER",false,1,2,true);
        WriterHoldLedgerR30.Stat stat=WriterHoldLedgerR30.snapshot().get(0);
        assertNull(project(row(1,JsonR27.write(stat.histogram()))).get("rolledBack"));
        Map<String,Object> legacy=project(row(1,"{\"version\":\"R30_HISTOGRAM_16_OPEN_UPPER_V1\"}"));
        assertNull(legacy.get("rolledBack"));assertNull(legacy.get("rolledBackKnown"));
        assertEquals("UNKNOWN_LEGACY_OR_MISSING_OUTCOME_SOURCE",legacy.get("rollbackEvidenceState"));
    }
    @Test public void malformedOrInconsistentCountsCannotProduceKnownRollback(){
        WriterHoldLedgerR30.record("ONE",false,1,2,true,"ROLLED_BACK");
        String histogram=JsonR27.write(WriterHoldLedgerR30.snapshot().get(0).histogram());
        Map<String,Object> wrong=project(row(2,histogram));assertNull(wrong.get("rolledBack"));
        assertEquals("UNKNOWN_INVALID_OUTCOME_COUNTS",wrong.get("rollbackEvidenceState"));
        assertNull(project(row(1,"not-json")).get("rolledBack"));
    }
    @Test public void reportExportsActualWriterProtocolObservation(){
        Map<String,Object> observation=new LinkedHashMap<>();observation.put("diagnosticFailures",3L);
        Map<String,Object> data=new LinkedHashMap<>();data.put("r31WriterProtocolObservation",observation);
        ReportR27.R30Input input=new ReportR27.R30Input(new LinkedHashMap<>(),new LinkedHashMap<>(),data,Collections.emptyList());
        Map<?,?> exported=(Map<?,?>)ReportR27.chaptersR30(input).get(2).get("data");
        assertEquals(observation,exported.get("r31WriterProtocolObservation"));
    }
}
