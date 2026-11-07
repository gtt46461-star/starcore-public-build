package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;

/** R31-B06-C03#A1-#A3: the durable checkpoint is published after every position, but the consumer-visible shared
 * ten-position model changes only when all ten positions of an issue are committed. Positions 1..9 keep the previous
 * complete model; position 10 switches it, the weights really change, and the old reference stays replayable. */
public final class R31AtomNetCompleteIssueConsumerTest {
    private static RuntimeR27 runtime() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeType.getDeclaredField("theUnsafe"); theUnsafe.setAccessible(true);
        return (RuntimeR27) unsafeType.getMethod("allocateInstance", Class.class).invoke(theUnsafe.get(null), RuntimeR27.class);
    }
    private static ModelSurfaceProviderR30.FrozenModelRef persisted(AtomNetSessionR30 s) {
        return new ModelSurfaceProviderR30.FrozenModelRef(s.trainer.net.encode(), s.trainedThroughOrdinal(), 1_700_000_000_000L);
    }
    @Test public void consumerSeesOnlyCompleteIssuesWhileCheckpointsAdvanceEveryPosition() throws Exception {
        AtomNetSessionR30 s = R30TenPositionTrainingTest.session();
        while (!s.ready()) s.work(R30TenPositionTrainingTest::sample);
        RuntimeR27 rt = runtime();
        ModelSurfaceProviderR30.FrozenModelRef complete = persisted(s);
        rt.publishAtomStateR31(s, complete);
        assertTrue(s.consumableR31());
        assertSame("a complete state is consumable", complete, rt.atomPublicationR30.model);
        byte[] completeWeights = s.trainer.net.encode();
        for (int p = 1; p <= 10; p++) {
            AtomNetTrainerR27.Sample sample = new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.2, .7}, p), p % 3 == 0 ? 1 : 0, p - 1, AtomNetSessionR30.eventKey("000000001000", p));
            s = R30TenPositionTrainingTest.copy(s); // restart between positions: resume from the durable checkpoint
            s.online(sample, "000000001000", 1000, p);
            ModelSurfaceProviderR30.FrozenModelRef checkpoint = persisted(s);
            rt.publishAtomStateR31(s, checkpoint);
            assertSame("the newest durable state is always tracked", s, rt.atomPublicationR30.state);
            Map<String, Object> report = s.report();
            if (p < 10) {
                assertFalse("partial issue is not consumable at position " + p, s.consumableR31());
                assertSame("consumer keeps the previous complete model at position " + p, complete, rt.atomPublicationR30.model);
                assertEquals("000000000999", report.get("completeTrainedThroughIssue"));
                assertEquals("000000001000", report.get("partialObservedIssue"));
                assertEquals((long) p, report.get("partialPositionsCommitted"));
            } else {
                assertTrue(s.consumableR31());
                assertSame("position 10 publishes the complete issue to consumers", checkpoint, rt.atomPublicationR30.model);
                assertEquals(1000L, rt.atomPublicationR30.model.trainedThroughOrdinal);
                assertEquals("000000001000", report.get("completeTrainedThroughIssue"));
                assertNull(report.get("partialObservedIssue"));
                assertNotEquals("non-zero gradients really changed the consumed weights", complete.weightsHash, checkpoint.weightsHash);
                assertNotEquals(complete.modelHash, rt.atomPublicationR30.model.modelHash);
            }
        }
        assertEquals("ten new online positions trained once each", 10, s.trainer.onlineSteps);
        assertArrayEquals("the old complete reference is immutable and replayable", completeWeights, complete.weights());
        assertEquals(StatsR27.sha256(completeWeights), complete.weightsHash);
    }
    @Test public void restartInsidePartialIssueHasNoConsumableModelUntilItCompletes() throws Exception {
        AtomNetSessionR30 s = R30TenPositionTrainingTest.session();
        while (!s.ready()) s.work(R30TenPositionTrainingTest::sample);
        for (int p = 1; p <= 4; p++) s.online(new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.3, .1}, p), 0, p - 1, AtomNetSessionR30.eventKey("000000001000", p)), "000000001000", 1000, p);
        AtomNetSessionR30 restarted = R30TenPositionTrainingTest.copy(s);
        RuntimeR27 fresh = runtime();
        fresh.publishAtomStateR31(restarted, persisted(restarted));
        assertNull("a cold start inside a partial issue never hands a partial model to consumers", fresh.atomPublicationR30.model);
        assertEquals(5, restarted.nextPosition);
        try { restarted.online(new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.3, .1}, 4), 0, 3, AtomNetSessionR30.eventKey("000000001000", 4)), "000000001000", 1000, 4); fail("position 4 retrained"); }
        catch (IllegalStateException expected) { assertEquals("R30_POSITION_CURSOR", expected.getMessage()); }
    }
}
