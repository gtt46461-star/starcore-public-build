package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public final class HotHistoryWindowR335Test {
    private static HotHistoryWindowR335 window(String last, Map<String,Integer> counts) {
        long end=TargetIssueResolver.sequenceOrdinalSU1(last);
        long begin=Math.max(1L,end-AppConfig.MAX_LOCAL_HISTORY_SAVE+1);
        return HotHistoryWindowR335.coverage(begin,end,TargetIssueResolver.fromSequenceOrdinalSU1(begin),last,counts);
    }

    @Test public void fifthDateContainsTheLeadingPartOfTheRollingWindow() {
        Map<String,Integer> counts=new HashMap<>();
        counts.put("20261121",80);
        counts.put("20261120",1440);
        counts.put("20261119",1440);
        counts.put("20261118",1440);
        HotHistoryWindowR335 recovered=window("202611210080",counts);
        assertEquals("202611170841",recovered.firstIssue);
        assertEquals(600,recovered.missingPeriods);
        assertEquals(Collections.singletonList("2026-11-17"),recovered.dates);
        counts.put("20261117",600);
        assertEquals(0,window("202611210080",counts).missingPeriods);
        counts.put("20261119",1438);
        assertEquals(Collections.singletonList("2026-11-19"),window("202611210080",counts).dates);
    }

    @Test public void yearBoundaryAndEarliestSupportedDayUseCalendarOrdinals() {
        HotHistoryWindowR335 early=window("202001010002",Collections.emptyMap());
        assertEquals("202001010001",early.firstIssue);
        assertEquals(2,early.missingPeriods);
        HotHistoryWindowR335 january=window("202701010001",Collections.emptyMap());
        assertEquals(5000,january.missingPeriods);
        assertEquals(Arrays.asList("2027-01-01","2026-12-31","2026-12-30","2026-12-29","2026-12-28"),january.dates);
    }
}
