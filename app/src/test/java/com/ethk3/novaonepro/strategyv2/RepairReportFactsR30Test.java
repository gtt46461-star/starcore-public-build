package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public final class RepairReportFactsR30Test {
    static Map<String,Object> m(Object... v){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)m.put((String)v[i],v[i+1]);return m;}
    static ReportR27.R30Input input(Map<String,Object> facts,Map<String,Object> data,List<String> errors){return new ReportR27.R30Input(facts,m("sourceCommit","SYNTHETIC_TEST_SOURCE","factsReadAt",1000L),data,errors);}
    static Map<String,Object> metric(ReportR27.R30Input in,String id){for(Map<String,Object> x:in.metricResults)if(id.equals(x.get("metricId")))return x;throw new AssertionError(id);}
    @Test public void sameMetricObjectIsUsedByAllChaptersAndCharts(){
        ReportR27.R30Input in=input(m("futureActs",10L,"futureHits",3L,"activeCertificates",0L,"realRows",0L,"behaviorDuplicates",4L),m(),Collections.emptyList());
        assertEquals(44,in.metricResults.size());assertEquals(0.3,((Number)metric(in,"Q01").get("value")).doubleValue(),0);
        assertEquals(10L,metric(in,"Q01").get("denominator"));assertEquals("DESCRIPTIVE_ONLY",metric(in,"Q01").get("status"));assertEquals("NOT_DEPLOYED",metric(in,"Q02").get("status"));
        List<Map<String,Object>> chapters=ReportR29.fromSnapshotR30(in);assertEquals(12,chapters.size());
        int projections=0;for(Map<String,Object> chapter:chapters)for(Object raw:(List<?>)chapter.get("metricResults")){Map<?,?> row=(Map<?,?>)raw;assertSame(metric(in,(String)row.get("metricId")),raw);projections++;}assertTrue(projections>44);
        for(Map<String,Object> chart:ReportChartsR30.panels(in))assertSame(in.metricResults,chart.get("metricResults"));
        assertSame(in.metricResults,ReportR29.document(chapters).get("metricResults"));
        try{metric(in,"Q01").put("value",0.99);fail("metric mutable");}catch(UnsupportedOperationException expected){}
        for(Map<String,Object> row:in.metricResults){assertEquals(MetricDefinitionsR29.R31_VERSION,row.get("definitionVersion"));assertTrue(((List<?>)row.get("repairCardIds")).get(0).toString().startsWith("FR-"));}
    }
    @Test public void emptyAndFailedSourcesNeverBecomeZeroOrPass(){
        ReportR27.R30Input empty=input(m(),m(),Collections.emptyList());
        for(String id:new String[]{"M03","M06","M07","M11","M14","Q01","Q02","Q07"}){Map<String,Object> row=metric(empty,id);assertNull(id,row.get("value"));assertNotEquals(id,"PASS",row.get("status"));assertNotNull(row.get("nextTrigger"));}
        ReportR27.R30Input failed=input(m("futureActs",100L,"futureHits",100L),m(),Collections.singletonList("SYNTHETIC_READ_FAILURE"));for(Map<String,Object> row:failed.metricResults){assertNull(row.get("value"));assertEquals("UNKNOWN_CAPTURE_FAILED",row.get("status"));}
    }
    @Test public void negativeWriterEvidenceRemainsFailureAndMissingFailuresAreNull(){
        Map<String,Object> window=m("critical",1,"count",20L,"wait_p95",201L);
        ReportR27.R30Input in=input(m(),m("writerWindows",Collections.singletonList(window)),Collections.emptyList());
        Map<String,Object> row=metric(in,"M07");assertEquals("FAIL",row.get("status"));assertEquals(201.0,((Number)row.get("value")).doubleValue(),0);assertNull(row.get("numerator"));assertTrue(row.get("missingReason").toString().contains("缺failures"));
        window.put("wait_p95",50L);window.put("failures",1L);in=input(m(),m("writerWindows",Collections.singletonList(window)),Collections.emptyList());assertEquals("FAIL",metric(in,"M07").get("status"));
    }
    @Test public void historicalBackfillCannotPassLiveLatencyMetric(){
        Map<String,Object> legacy=m("issue","SYNTHETIC","mono_first_observed",1L,"mono_settled",2L,"boot_first_observed","A","boot_settled","A");
        ReportR27.R30Input in=input(m(),m("timeline",Collections.singletonList(legacy)),Collections.emptyList());assertNull(metric(in,"M04").get("value"));assertEquals(1L,metric(in,"M04").get("legacyOrNonLiveRowsExcluded"));
        legacy.put("event_role","LIVE");in=input(m(),m("timeline",Collections.singletonList(legacy)),Collections.emptyList());assertEquals(1.0,((Number)metric(in,"M04").get("value")).doubleValue(),0);
    }
}
