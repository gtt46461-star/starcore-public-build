package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/** R31-B06-C02: the replay checkpoint envelope binds numeric state, input revision, next index and model steps under
 * one content hash; tampering, unknown versions and non-canonical bytes fail closed. */
public final class R31ReplayCheckpointEnvelopeTest {
    private static byte[] state(int seed) { byte[] b = new byte[4096]; for (int i = 0; i < b.length; i++) b[i] = (byte) (i * 31 + seed); return b; }
    @Test public void cursorAndStepsShareTheStateHash() {
        ReplayCheckpointR31.Envelope a = ReplayCheckpointR31.prepare("foundry-F03", "rev-1", 64, 64, state(1));
        ReplayCheckpointR31.Envelope sameStateLaterCursor = ReplayCheckpointR31.prepare("foundry-F03", "rev-1", 128, 64, state(1));
        ReplayCheckpointR31.Envelope sameStateMoreSteps = ReplayCheckpointR31.prepare("foundry-F03", "rev-1", 64, 65, state(1));
        ReplayCheckpointR31.Envelope otherRevision = ReplayCheckpointR31.prepare("foundry-F03", "rev-2", 64, 64, state(1));
        assertNotEquals("the cursor is part of the reference", a.stateHash, sameStateLaterCursor.stateHash);
        assertNotEquals("model steps are part of the reference", a.stateHash, sameStateMoreSteps.stateHash);
        assertNotEquals("the input revision is part of the reference", a.stateHash, otherRevision.stateHash);
        assertEquals("deterministic", a.stateHash, ReplayCheckpointR31.prepare("foundry-F03", "rev-1", 64, 64, state(1)).stateHash);
    }
    @Test public void verifyRestoresExactlyWhatWasPrepared() {
        ReplayCheckpointR31.Envelope a = ReplayCheckpointR31.prepare("legacy-generation", "rev-9", 256, 1024, state(7));
        ReplayCheckpointR31.Envelope b = ReplayCheckpointR31.verify(a.encoded(), a.stateHash);
        assertEquals(a.owner, b.owner); assertEquals(a.inputRevision, b.inputRevision);
        assertEquals(256, b.nextIndex); assertEquals(1024, b.modelSteps);
        assertArrayEquals(state(7), b.state());
        byte[] mutable = a.state(); mutable[0] ^= 1;
        assertArrayEquals("state is copied, not aliased", state(7), a.state());
    }
    @Test public void tamperedUnknownVersionAndTrailingBytesFailClosed() {
        ReplayCheckpointR31.Envelope a = ReplayCheckpointR31.prepare("o", "r", 1, 1, state(3));
        byte[] tampered = a.encoded(); tampered[tampered.length - 1] ^= 1;
        assertRejected(tampered, a.stateHash);
        assertRejected(tampered, ReplayCheckpointR31.sha256(tampered));
        byte[] version = a.encoded(); version[7] = 2;
        assertRejected(version, ReplayCheckpointR31.sha256(version));
        byte[] trailing = Arrays.copyOf(a.encoded(), a.encoded().length + 1);
        assertRejected(trailing, ReplayCheckpointR31.sha256(trailing));
    }
    @Test public void commitKeyIsBoundToOwnerRevisionPreviousAndCursor() {
        String k = ReplayCheckpointR31.commitKey("o", "r", "p", 64);
        assertEquals(k, ReplayCheckpointR31.commitKey("o", "r", "p", 64));
        assertNotEquals(k, ReplayCheckpointR31.commitKey("o", "r", "p", 65));
        assertNotEquals(k, ReplayCheckpointR31.commitKey("o", "r2", "p", 64));
        assertNotEquals(k, ReplayCheckpointR31.commitKey("o", "r", "q", 64));
        assertNotEquals(k, ReplayCheckpointR31.commitKey("o2", "r", "p", 64));
    }
    private static void assertRejected(byte[] bytes, String hash) {
        try { ReplayCheckpointR31.verify(bytes, hash); fail("must fail closed"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().startsWith("R31_REPLAY_CHECKPOINT_")); }
    }
}
