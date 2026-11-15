package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.*;

public final class R940RecoveryContinuationTest {
    @Test public void unknownBacklogRetriesWithoutSpinning() {
        assertTrue(DeferredEvidenceWriterR36.shouldContinueDrain(true, -1));
        assertEquals(5000L, DeferredEvidenceWriterR36.continuationDelayMs(-1, false));
    }
    @Test public void emptyOrStoppedDrainDoesNotRestart() {
        assertFalse(DeferredEvidenceWriterR36.shouldContinueDrain(true, 0));
        assertFalse(DeferredEvidenceWriterR36.shouldContinueDrain(false, 6754));
        assertFalse(DeferredEvidenceWriterR36.shouldContinueDrain(false, -1));
    }
    @Test public void pendingDrainYieldsToRealtime() {
        assertTrue(DeferredEvidenceWriterR36.shouldContinueDrain(true, 6754));
        assertEquals(5000L, DeferredEvidenceWriterR36.continuationDelayMs(6754, true));
        assertEquals(800L, DeferredEvidenceWriterR36.continuationDelayMs(6754, false));
    }
}
