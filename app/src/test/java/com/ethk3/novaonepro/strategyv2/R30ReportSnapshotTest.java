package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30ReportSnapshotTest {
    @Test public void twelveChaptersShareDetachedImmutableFactsAndWatermarks(){
        Map<String,Object> facts=new LinkedHashMap<>();facts.put("futurePeriods",16L);facts.put("futureActs",8L);facts.put("futureHits",3L);facts.put("independentFutureIssues",4L);facts.put("receiptStateMismatches",0L);
        Map<String,Object> mark=new LinkedHashMap<>();mark.put("latestIssue","202610310004");mark.put("tables",new LinkedHashMap<String,Object>(Collections.singletonMap("draws",Collections.singletonMap("maxRowid",4L))));
        ReportR27.R30Input input=new ReportR27.R30Input(facts,mark,Collections.emptyMap(),Collections.emptyList());facts.put("futurePeriods",1000L);mark.put("latestIssue","202610310999");
        List<Map<String,Object>> chapters=ReportR27.chaptersR30(input);assertEquals(12,chapters.size());assertTrue(chapters.get(2).get("title").toString().startsWith("第3章 运行"));assertTrue(chapters.get(3).get("title").toString().startsWith("第4章 任务"));
        for(Map<String,Object> chapter:chapters){assertSame(input.watermark,chapter.get("snapshotWatermark"));assertEquals(16L,((Map<?,?>)chapter.get("commonMetrics")).get("futurePeriods"));assertEquals(0L,chapter.get("reportConsistencyConflicts"));}
        try{input.facts.put("futurePeriods",1L);fail("Detached facts mutable");}catch(UnsupportedOperationException expected){}
        assertEquals("202610310004",input.watermark.get("latestIssue"));assertEquals(4L,((Map<?,?>)((Map<?,?>)input.watermark.get("tables")).get("draws")).get("maxRowid"));
    }
    @Test public void missingSourceRemainsUnknownAndRealReceiptConflictIsNotHidden(){
        List<Map<String,Object>> missing=ReportR27.chaptersR30(new ReportR27.R30Input(Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList()));
        assertNull(missing.get(0).get("reportConsistencyConflicts"));assertNull(((Map<?,?>)missing.get(0).get("metrics")).get("futureActs"));
        Map<String,Object> facts=new LinkedHashMap<>();facts.put("receiptStateMismatches",2L);
        for(Map<String,Object> chapter:ReportR27.chaptersR30(new ReportR27.R30Input(facts,Collections.emptyMap(),Collections.emptyMap(),Collections.emptyList())))assertEquals(2L,chapter.get("reportConsistencyConflicts"));
    }
}
