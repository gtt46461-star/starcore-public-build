package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

/** R30-B03-05: interrupt (encode → decode) then N more steps equals N uninterrupted steps within
 * ModelArtifactR29.TOLERANCE; the same event is never learned twice. */
public class R30ResumeTrainingTest {
    @Test public void resumedTrainingEqualsUninterrupted() {
        ModelArtifactR29 uninterrupted = R29ModelArtifactTest.state();
        byte[] saved = R29ModelArtifactTest.state().encode();
        ModelArtifactR29 resumed = ModelArtifactR29.decode(saved, StatsR27.sha256(saved));
        for (int i = 40; i < 70; i++) {
            AtomNetTrainerR27.Sample s = R29ModelArtifactTest.sample(i);
            double a = uninterrupted.trainer.online(s), b = resumed.trainer.online(s);
            uninterrupted.ftrl.update(Arrays.copyOf(s.x, 7), s.y); resumed.ftrl.update(Arrays.copyOf(s.x, 7), s.y);
            assertEquals(a, b, ModelArtifactR29.TOLERANCE);
        }
        double[] x = R29ModelArtifactTest.sample(99).x;
        assertEquals(uninterrupted.trainer.net.predict(x), resumed.trainer.net.predict(x), ModelArtifactR29.TOLERANCE);
        assertEquals(uninterrupted.ftrl.predict(Arrays.copyOf(x, 7)), resumed.ftrl.predict(Arrays.copyOf(x, 7)), ModelArtifactR29.TOLERANCE);
        assertArrayEquals(new ModelArtifactR29(uninterrupted.trainer, uninterrupted.ftrl, "D", "T").encode(), new ModelArtifactR29(resumed.trainer, resumed.ftrl, "D", "T").encode());
    }

    @Test public void sameEventIsNotLearnedTwice() {
        ModelArtifactR29 a = R29ModelArtifactTest.state();
        long steps = a.trainer.onlineSteps;
        try { a.trainer.online(R29ModelArtifactTest.sample(39)); fail("duplicate learned"); }
        catch (IllegalStateException e) { assertTrue(e.getMessage().startsWith("R30_DUPLICATE_LEARNING")); }
        assertEquals(steps, a.trainer.onlineSteps);
    }
}
