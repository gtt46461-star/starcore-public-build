package com.ethk3.novaonepro;

/** Immutable diagnosis text built from V51 identities and persisted runtime evidence only. */
public final class V51ModelHealthDiagnosisEngine {
    public static final class Snapshot {
        public final String releaseState,
                bundleHash,
                runtimeStatus,
                modelGate,
                ranker,
                calibration,
                policy;
        public final boolean realBetAllowed;

        Snapshot(
                String releaseState,
                String bundleHash,
                String runtimeStatus,
                String modelGate,
                String ranker,
                String calibration,
                String policy,
                boolean realBetAllowed) {
            this.releaseState = releaseState;
            this.bundleHash = bundleHash;
            this.runtimeStatus = runtimeStatus;
            this.modelGate = modelGate;
            this.ranker = ranker;
            this.calibration = calibration;
            this.policy = policy;
            this.realBetAllowed = realBetAllowed;
        }
    }

    public Snapshot build(DbStore store) {
        String runtime = store == null ? "" : store.getState("v51_runtime_status", "");
        String gate = V51BuildManifest.MODEL_PROVEN ? "PASSED" : "NOT_PROVEN";
        return new Snapshot(
                V51BuildManifest.releaseState(),
                V51ArtifactBundleManifest.BUNDLE_HASH,
                runtime,
                gate,
                V51ActionRankerArtifact.ARTIFACT_ID,
                V51SelectedCalibrationArtifact.ARTIFACT_ID,
                V51PolicySurfaceArtifact.ARTIFACT_ID,
                V51BuildManifest.mayAuthorizeRealBet()
                        && !V51ArtifactRollbackEngine.forcedShadow(store));
    }
}
