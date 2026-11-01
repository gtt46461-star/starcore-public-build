package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public final class RepairReportIndexR30Test {
    static Map<String,Object> m(Object... v){return RepairReportFactsR30Test.m(v);}
    static Map<String,Object> summary(String job,String snapshot,String role,String scope){return m("jobId",job,"snapshotSha256",snapshot,"sourceCommit","SYNTHETIC_SAME_SOURCE","definitionVersion",MetricDefinitionsR29.CURRENT_VERSION,"objects",Collections.singletonList(m("objectId","M01","objectKind","METRIC","role",role,"population","P","unit","count","counterScope",scope,"value",job.equals("A")?1L:2L)));}
    @Test public void completeIndexNeverMeansProductEfficacy(){
        List<Object> caps=new ArrayList<>();for(int i=0;i<82;i++)caps.add(m("id","SYNTHETIC_CAP_"+i,"implemented",true,"executable",true));
        Map<String,Object> index=ReportObjectIndexR30.build(m(),m(),m("capabilities",caps));
        String[] keys={"legacyCharts","researchCharts","matrices","capabilities"};int[] sizes={29,18,20,82};Set<String> ids=new HashSet<>();
        for(int i=0;i<keys.length;i++){Map<?,?> category=(Map<?,?>)index.get(keys[i]);assertEquals(sizes[i],category.get("total"));assertEquals(0,category.get("omitted"));for(Object x:(List<?>)category.get("objects")){Map<?,?> row=(Map<?,?>)x;assertTrue(ids.add(row.get("objectKind")+"|"+row.get("objectId")));for(String state:new String[]{"implemented","callable","inputAvailable","produced","consumed","effective"})assertTrue(row.containsKey(state));assertNull(row.get("effective"));}}
        assertNull(index.get("productPassRate"));assertNull(index.get("detectionExecutionRate"));
    }
    @Test public void repeatedBIsNotAThirdPeriod(){
        Map<String,Object> a=summary("A","snapshotA","TRAIN","PERSISTED"),b=summary("B","snapshotB","TRAIN","PERSISTED");
        assertEquals("COMPARABLE_TWO_SUCCESSFUL_SNAPSHOTS",ReportDataR27.compareSuccessfulR30(b,a).get("status"));
        Map<String,Object> duplicate=ReportDataR27.compareSuccessfulR30(b,b);assertEquals("DUPLICATE_JOB_NOT_NEW_PERIOD",duplicate.get("status"));assertEquals(0,duplicate.get("independentPeriodsAdded"));
        assertEquals("SAME_SNAPSHOT_NOT_NEW_PERIOD",ReportDataR27.compareSuccessfulR30(summary("C","snapshotB","TRAIN","PERSISTED"),b).get("status"));
        b.put("sourceCommit","SYNTHETIC_OTHER_SOURCE");assertEquals("INCOMPARABLE_SOURCE_OR_DEFINITION_VERSION",ReportDataR27.compareSuccessfulR30(b,a).get("status"));
    }
    @Test public void roleAndProcessCountersNeverSubtract(){
        Map<String,Object> a=summary("A","snapshotA","TRAIN","PERSISTED"),b=summary("B","snapshotB","CONFIRM","PERSISTED");assertTrue(((List<?>)ReportDataR27.compareSuccessfulR30(b,a).get("objects")).isEmpty());
        b=summary("B","snapshotB","TRAIN","PROCESS_LIFETIME");Map<?,?> change=(Map<?,?>)((List<?>)ReportDataR27.compareSuccessfulR30(b,a).get("objects")).get(0);assertNull(change.get("delta"));
    }
}
