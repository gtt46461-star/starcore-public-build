package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TargetIssueResolverTest {
    @Test public void rollsExactlyAcrossUtcDay() {
        TargetIssueResolver.Resolution result=TargetIssueResolver.next("202608101440");
        assertTrue(result.valid);
        assertEquals("202608110001",result.targetIssue);
        assertEquals(1,TargetIssueResolver.distance("202608101440",result.targetIssue));
    }

    @Test public void rejectsMalformedAndImpossibleIssues() {
        assertFalse(TargetIssueResolver.isValid("202608101441"));
        assertFalse(TargetIssueResolver.isValid("202602300001"));
        assertFalse(TargetIssueResolver.next("bad").valid);
        assertEquals(Integer.MIN_VALUE,TargetIssueResolver.distance("bad","202608100001"));
    }

    @Test public void compareAndDistanceAreCrossDayConsistent() {
        assertTrue(TargetIssueResolver.isAfter("202608110002","202608101439"));
        assertEquals(3,TargetIssueResolver.distance("202608101439","202608110002"));
        assertTrue(TargetIssueResolver.compare("202608110002","202608101439")>0);
    }
}
