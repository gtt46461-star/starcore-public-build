package com.ethk3.novaonepro;
import org.junit.Test;
import static org.junit.Assert.*;
public final class HistoryWatermarkR46Test {
 @Test public void confirmedProviderSurvivesLaggingHistoricalWatermark(){assertEquals("202610080818",MainActivity.visibleCurrentIssueR46("202610080818","202610080549"));}
 @Test public void lateProviderCannotRegressNewerCommittedHistory(){assertEquals("202610080819",MainActivity.visibleCurrentIssueR46("202610080818","202610080819"));}
 @Test public void missingProviderUsesVerifiedHistory(){assertEquals("202610080549",MainActivity.visibleCurrentIssueR46(null,"202610080549"));}
 @Test public void malformedHistoryCannotMaskValidProvider(){assertEquals("202610080818",MainActivity.visibleCurrentIssueR46("202610080818","unavailable"));}
 @Test public void malformedBothDoNotInventAnIssue(){assertEquals("",MainActivity.visibleCurrentIssueR46("--",null));}
 @Test public void targetRollsForwardFromConfirmedWatermark(){assertEquals("202610080819",TargetIssueResolver.next(MainActivity.visibleCurrentIssueR46("202610080818","202610080549")).targetIssue);}
}
