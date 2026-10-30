package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

/** Synthetic mechanism checks: real forward/backward/Adam/FTRL operations, never natural-quality evidence. */
public class R29ModelArtifactTest {
    static AtomNetTrainerR27.Sample sample(int i) {
        double[] x = new double[8]; for (int j = 0; j < x.length; j++) x[j] = Math.sin((i + 1) * (j + .3));
        return new AtomNetTrainerR27.Sample(x, i % 3 == 0 ? 1 : 0, i % 10, "ISSUE_" + i);
    }
    static ModelArtifactR29 state() {
        AtomNetTrainerR27 t = new AtomNetTrainerR27(8, 29); t.onlineLr = .001;
        t.checkpoint = t.net.encode(); t.lastCheckLogloss = .65; t.lastCheckSe = .02;
        FtrlR27 f = new FtrlR27(7, .05, 1, .0001, .001);
        for (int i = 0; i < 40; i++) { AtomNetTrainerR27.Sample s = sample(i); t.online(s); f.update(Arrays.copyOf(s.x, 7), s.y); }
        return new ModelArtifactR29(t, f, "DATASET_A", "ISSUE_39");
    }

    @Test public void completeStateRestoreProducesIdenticalNextUpdates() {
        ModelArtifactR29 a = state(); byte[] bytes = a.encode(); ModelArtifactR29 b = ModelArtifactR29.decode(bytes, StatsR27.sha256(bytes));
        assertArrayEquals(bytes, b.encode()); assertEquals(40, b.trainer.onlineSteps); assertEquals(40, b.ftrl.updates);
        assertEquals(a.trainer.net.predict(sample(41).x), b.trainer.net.predict(sample(41).x), 0);
        double before = a.trainer.net.predict(sample(41).x); byte[] priorNetwork = a.trainer.net.encode();
        for (int i = 40; i < 45; i++) {
            AtomNetTrainerR27.Sample s = sample(i);
            assertEquals(a.trainer.online(s), b.trainer.online(s), 0);
            a.ftrl.update(Arrays.copyOf(s.x, 7), s.y); b.ftrl.update(Arrays.copyOf(s.x, 7), s.y);
        }
        assertArrayEquals(a.encode(), b.encode());
        assertFalse(Arrays.equals(priorNetwork, a.trainer.net.encode()));
        assertTrue(Math.abs(before - a.trainer.net.predict(sample(41).x)) > 1e-12);
    }

    @Test public void abandonedStageDoesNotMutateCommittedModel() {
        ModelArtifactR29 committed = state(); byte[] original = committed.encode();
        ModelArtifactR29 staging = ModelArtifactR29.decode(original, StatsR27.sha256(original));
        staging.trainer.online(sample(41)); staging.ftrl.update(Arrays.copyOf(sample(41).x, 7), 1);
        assertArrayEquals(original, committed.encode()); assertFalse(Arrays.equals(original, staging.encode()));
    }

    @Test public void corruptAndTrailingBytesFailClosed() {
        byte[] original = state().encode(), corrupted = original.clone(); corrupted[corrupted.length / 2] ^= 1;
        try { ModelArtifactR29.decode(corrupted, StatsR27.sha256(original)); fail("checksum accepted corruption"); } catch (IllegalArgumentException expected) { }
        byte[] trailing = Arrays.copyOf(original, original.length + 1);
        try { ModelArtifactR29.decode(trailing, StatsR27.sha256(trailing)); fail("trailing bytes accepted"); } catch (IllegalArgumentException expected) { }
        try { ModelArtifactR29.decode(new byte[0], StatsR27.sha256(new byte[0])); fail("empty state accepted"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void rollingDatasetChangeDoesNotResetLearningState() {
        ModelArtifactR29 a = state();
        ModelArtifactR29 rolled = new ModelArtifactR29(a.trainer, a.ftrl, "DATASET_B", a.trainedThrough);
        ModelArtifactR29 b = ModelArtifactR29.decode(rolled.encode(), StatsR27.sha256(rolled.encode()));
        assertEquals("DATASET_B", b.datasetHash); assertEquals(40, b.trainer.onlineSteps);
        assertArrayEquals(a.trainer.net.encode(), b.trainer.net.encode()); assertEquals(a.trainer.rng.state, b.trainer.rng.state);
    }
}
