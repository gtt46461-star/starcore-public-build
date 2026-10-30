package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import org.junit.Test;

/** Synthetic protocol identity checks; database transaction failures are tested with the real SQLite host runner. */
public class R29EventBridgeTest {
    private static final String ISSUE = "202610300001";
    private static final int[] NUMBERS = {1,2,3,4,5,6,7,8,9,10};

    @Test public void repeatObservationAndMetadataRepairKeepBusinessIdentity() {
        ResearchEventBridgeR29.Event a = ResearchEventBridgeR29.Event.draw(1, ISSUE, NUMBERS, "NEW_CANONICAL_COMMIT", 100);
        ResearchEventBridgeR29.Event b = ResearchEventBridgeR29.Event.draw(2, ISSUE, NUMBERS, "EXISTING_CANONICAL_OBSERVED", 200);
        assertEquals(a.id, b.id); assertEquals(a.payload, b.payload); assertEquals(a.hash, b.hash);
        assertNotEquals(a.origin, b.origin);
    }

    @Test public void invalidPermutationAndNonintegralLabelsAreRejected() {
        int[] repeated = NUMBERS.clone(); repeated[9] = 1;
        try { ResearchEventBridgeR29.Event.draw(1, ISSUE, repeated, "NEW_CANONICAL_COMMIT", 100); fail(); }
        catch (IllegalArgumentException expected) { }
        ResearchEventBridgeR29.Event a = ResearchEventBridgeR29.Event.draw(1, ISSUE, NUMBERS, "NEW_CANONICAL_COMMIT", 100);
        String payload = a.payload.replace("[1,", "[1.5,"); String hash = StatsR27.sha256(payload);
        try { new ResearchEventBridgeR29.Event(1, StatsR27.sha256("DRAW_COMMITTED|" + ISSUE + "|" + hash), ISSUE, payload, hash, a.origin, 100); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void tamperedPayloadAndMismatchedIssueFailBeforeDelivery() {
        ResearchEventBridgeR29.Event a = ResearchEventBridgeR29.Event.draw(1, ISSUE, NUMBERS, "NEW_CANONICAL_COMMIT", 100);
        try { new ResearchEventBridgeR29.Event(1, a.id, a.issue, a.payload + " ", a.hash, a.origin, 100); fail(); }
        catch (IllegalArgumentException expected) { }
        String payload = a.payload.replace(ISSUE, "202610300002"); String hash = StatsR27.sha256(payload);
        try { new ResearchEventBridgeR29.Event(1, StatsR27.sha256("DRAW_COMMITTED|" + ISSUE + "|" + hash), ISSUE, payload, hash, a.origin, 100); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void emptyAndUncommittedDeliveryCannotMintTrainingEvidence() {
        ResearchWriteQueueR27.Batch empty = ResearchEventBridgeR29.prepare(java.util.Collections.emptyList(), 100);
        assertTrue(empty.isEmpty());
        try { ResearchEventBridgeR29.modelCommitted(empty, ISSUE, "", 1, 100); fail(); }
        catch (IllegalArgumentException expected) { }
        try { ResearchEventBridgeR29.modelCommitted(empty, ISSUE, StatsR27.sha256("state"), 0, 100); fail(); }
        catch (IllegalArgumentException expected) { }
    }
}
