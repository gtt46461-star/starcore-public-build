package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ExactRealAttributionR34Test {
    @Test public void anotherTradeCannotBorrowTheSameTargetSettlement() {
        String draw="12".repeat(32),oos="34".repeat(32);
        String real=V51ChecksumEngine.sha256("R36_REAL|7|"+draw+"|3|1");
        String terminal=V51ChecksumEngine.sha256("R36_SETTLEMENT|"+draw+'|'+oos+'|'+real+"|COMPLETED");
        assertTrue(ResearchMainlineContributionAuditR922.matchesTerminal(7,3,1,"WIN",oos,draw,terminal));
        assertFalse(ResearchMainlineContributionAuditR922.matchesTerminal(8,3,1,"WIN",oos,draw,terminal));
        assertFalse(ResearchMainlineContributionAuditR922.matchesTerminal(7,3,0,"WIN",oos,draw,terminal));
        assertFalse(ResearchMainlineContributionAuditR922.matchesTerminal(7,3,1,"WIN","56".repeat(32),draw,terminal));
    }
}
