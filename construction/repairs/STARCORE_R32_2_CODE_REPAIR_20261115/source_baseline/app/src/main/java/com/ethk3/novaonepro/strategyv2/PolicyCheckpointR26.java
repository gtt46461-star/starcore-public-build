package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import com.ethk3.novaonepro.*;
/** The same streaming complete-state path serves shared and private committee/stage learners. */
public final class PolicyCheckpointR26 {
    private PolicyCheckpointR26() {}
    public static CheckpointArtifactsR26.Prepared prepare(CheckpointArtifactsR26 files,PolicyRuntimeR26 runtime)throws IOException {
        return runtime.legacyState()!=null?files.prepareLegacy(runtime.legacyState()):files.prepareLease(runtime.encode());
    }
    public static PolicyRuntimeR26 restore(CheckpointArtifactsR26 files,String text,byte[] reference,String expected)throws IOException {
        byte[] root=files.resolve(reference);if(!ChunkedCheckpointR26.isManifest(root))return PolicyRuntimeR26.decode(text,root,expected);
        LegacyReplayStateR26 state=files.restoreLegacy(reference,expected);if(state.canonical.size()!=1||!state.canonical.get(0).equals(text))throw new IOException("R26_PRIVATE_CHECKPOINT_IDENTITY");return PolicyRuntimeR26.legacy(state);
    }
}
