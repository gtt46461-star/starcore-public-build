package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class ShadowWaitingUiR45Test {
    private static final String TARGET = "202610080768";

    @Test public void runningIsAnalysisRatherThanGenericWaiting() {
        assertEquals("正在分析", MainActivity.noDecisionTitleR45(TARGET, "RUNNING", "", ""));
    }
    @Test public void queuedIsExplicit() {
        assertEquals("分析已排队", MainActivity.noDecisionTitleR45(TARGET, "WAITING", "", ""));
    }
    @Test public void currentFailureRemainsVisibleAfterAWaitingPulse() {
        assertEquals("分析异常，等待重试", MainActivity.noDecisionTitleR45(TARGET, "WAITING", TARGET, "DISPATCH_FAILED"));
    }
    @Test public void previousIssueFailureCannotLabelCurrentIssue() {
        assertEquals("分析已排队", MainActivity.noDecisionTitleR45(TARGET, "WAITING", "202610080767", "DISPATCH_FAILED"));
    }
    @Test public void actualRetryExecutionSupersedesEarlierFailure() {
        assertEquals("正在分析", MainActivity.noDecisionTitleR45(TARGET, "RUNNING", TARGET, "DISPATCH_FAILED"));
    }
    @Test public void idleProducesNoPrediction() {
        assertEquals("等待分析", MainActivity.noDecisionTitleR45(TARGET, "IDLE", "", ""));
    }
}
