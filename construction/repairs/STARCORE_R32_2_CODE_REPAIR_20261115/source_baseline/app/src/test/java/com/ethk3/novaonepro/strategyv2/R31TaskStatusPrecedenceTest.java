package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Regression: a stale pending=0 receipt must not make a quarantined task healthy. */
public class R31TaskStatusPrecedenceTest {
    @Test public void controlStateOutranksMissingOrIdleCounters() {
        assertEquals(TaskLivenessR30.State.QUARANTINED,
                TaskLivenessR30.classify("QUARANTINED", 0, 0, false, true, false));
        assertEquals(TaskLivenessR30.State.ERROR,
                TaskLivenessR30.classify("FAILED", -1, -1, false, false, false));
        assertEquals(TaskLivenessR30.State.NEEDS_FIX,
                TaskLivenessR30.classify("NEEDS_FIX", 0, 0, false, true, false));
        assertEquals(TaskLivenessR30.State.UNKNOWN,
                TaskLivenessR30.classify("READY", -1, -1, false, false, false));
    }

    @Test public void typedProjectionPreservesRawStateAndMissingFacts() {
        Map<String,Object> owner=new LinkedHashMap<>();
        owner.put("task_id", "r27-replay-dataset");owner.put("status", "QUARANTINED");
        owner.put("blocked_reason", "ERROR:claim");
        Map<String,Object> fact=new LinkedHashMap<>();
        fact.put("task_id", "r27-replay-dataset");fact.put("work_unit", "ISSUE");
        fact.put("work_cursor", 100L);fact.put("known_latest", 100L);fact.put("pending", 0L);
        fact.put("oldest_pending_ordinal", -1L);fact.put("last_commit_clock_ordinal", 100L);
        Map<String,Object> row=find(TaskProgressR30R1.summarize(Arrays.asList(owner),Arrays.asList(fact),100L),"r27-replay-dataset");
        assertEquals("QUARANTINED",row.get("state"));
        assertEquals("IDLE_WAITING_INPUT",row.get("sourceState"));
        assertEquals("QUARANTINED",row.get("rawSchedulerState"));
        row=find(TaskProgressR30R1.summarize(Arrays.asList(owner),Collections.emptyList(),100L),"r27-replay-dataset");
        assertEquals("QUARANTINED",row.get("state"));
        assertEquals("NO_ARTIFACT_BOUND_TYPED_PROGRESS",row.get("sourceFailure"));
    }

    @Test public void quarantineCannotPassHealthSummary() {
        Map<String,Object> sum=TaskLivenessR30.summarize(new TaskLivenessR30.State[]{TaskLivenessR30.State.QUARANTINED});
        assertEquals(1,((Number)sum.get("m12")).intValue());assertEquals(false,sum.get("m12Pass"));
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> find(Map<String,Object> result,String id) {
        for(Object row:(List<?>)result.get("tasks"))
            if(id.equals(((Map<?,?>)row).get("taskId")))return (Map<String,Object>)row;
        throw new AssertionError(id);
    }
}
