package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Evidence visibility and semantic state tests; no device or strategy-quality claim. */
public final class R343ChartEvidenceTest {
    @Test public void bulkRowsCannotConsumeGraphIdentityReservations() {
        ReportInlineBudgetR343 budget=new ReportInlineBudgetR343();
        int total=0;
        for(int i=0;i<100;i++)total+=budget.take("bulk:"+i,128*1024);
        assertEquals(656*1024,total);
        assertEquals(16*1024,budget.take("chartObjectIndex",16*1024));
        assertEquals(32*1024,budget.take("repairFrameIndex",32*1024));
        assertEquals(32*1024,budget.take("researchCurrentHeadsR342",8*1024));
        assertEquals(32*1024,budget.take("researchCurrentActionsR342",8*1024));
        assertEquals(0,budget.take("repairFrameIndex",32*1024));
        assertEquals(0,budget.take("more",16*1024));
    }
    @Test public void publicationTablesAreVisibleToTheActualSnapshotMetadataReader() {
        assertTrue(Arrays.asList(ReportSnapshotR30.TABLES).contains("r33_research_input_head"));
        assertTrue(Arrays.asList(ReportSnapshotR30.TABLES).contains("r33_research_applied"));
    }
    @Test public void unknownConsumptionRemainsUnknownAndZeroIsNotUnknown() {
        Map<String,Object> unknown=ReportObjectIndexR30.layersR31(new ReportObjectIndexR30.ObjectFactsR31(
            "R01","RESEARCH_CHART","card",true,null,null,null,null,null,true));
        assertEquals("UNKNOWN",unknown.get("effective"));
        Map<String,Object> empty=ReportObjectIndexR30.layersR31(new ReportObjectIndexR30.ObjectFactsR31(
            "R01","RESEARCH_CHART","card",true,null,false,null,false,null,true));
        assertEquals("NO",empty.get("produced"));
        assertEquals("UNKNOWN",empty.get("ran"));
        assertEquals("NOT_CONSUMED",empty.get("effective"));
        assertEquals("NOT_PASS",empty.get("status"));
    }
    @Test public void historicalHashCannotOverrideCurrentSourceZero() {
        Map<String,Object> facts=new LinkedHashMap<>();facts.put("researchProduced:R01",0L);
        Map<String,Object> oldFrame=new LinkedHashMap<>();oldFrame.put("view_id","R01");
        oldFrame.put("latestOriginalHash",String.join("",Collections.nCopies(64,"a")));
        Map<String,Object> data=new LinkedHashMap<>();data.put("repairFrameIndex",Collections.singletonList(oldFrame));
        Map<String,Object> index=ReportObjectIndexR30.buildProductionR31(facts,Collections.emptyMap(),data);
        for(Object raw:(List<?>)index.get("lifecycleEntriesR31")){
            Map<?,?> row=(Map<?,?>)raw;if("R01".equals(row.get("id"))){
                assertEquals("NO",row.get("produced"));assertEquals("UNKNOWN",row.get("ran"));return;
            }
        }
        fail("R01 missing");
    }
    @Test public void formalReadOnlyChartsDoNotRequireAnOptimizerMutation() {
        Map<String,Object> index=ReportObjectIndexR30.buildProductionR31(Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap());
        Set<String> formal=new HashSet<>(Arrays.asList("R11","R14","R18"));
        int seen=0;
        for(Object raw:(List<?>)index.get("lifecycleEntriesR31")){
            Map<?,?> row=(Map<?,?>)raw;
            if(formal.contains(row.get("id"))){seen++;assertEquals("NOT_APPLICABLE",row.get("consumed"));assertEquals("NOT_APPLICABLE",row.get("effective"));assertEquals("NOT_PASS",row.get("status"));}
        }
        assertEquals(3,seen);
    }
}
