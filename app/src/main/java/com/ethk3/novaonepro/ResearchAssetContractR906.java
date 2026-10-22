package com.ethk3.novaonepro;

import java.util.EnumSet;

/** Contract distinguishing genuine method invention, tuning and non-executable blueprints. */
public final class ResearchAssetContractR906 {
    public enum Dimension {
        REPRESENTATION, FEATURE, LEARNER, UPDATE, OBJECTIVE, CALIBRATION, RISK, FALSIFIER
    }
    public enum NoveltyClass { TUNING_VARIANT, NEW_METHOD_IDEA, BLUEPRINT_ONLY }
    public enum Tier { IDEA, NURSERY, ACTIVE, CHALLENGER, CERTIFIED_SHADOW, ARCHIVED }
    public enum ArtifactStatus { NONE, WAITING_OFFLINE_TRAINER, COMPILED, PARITY_PASS, REJECTED }

    public static NoveltyClass classify(
            String parentSignature,
            String childSignature,
            EnumSet<Dimension> changed,
            boolean runtimeExecutable) {
        if (!runtimeExecutable) return NoveltyClass.BLUEPRINT_ONLY;
        if (safe(parentSignature).equals(safe(childSignature))) return NoveltyClass.TUNING_VARIANT;
        boolean methodAxis = changed.contains(Dimension.REPRESENTATION)
                || changed.contains(Dimension.LEARNER);
        return methodAxis && changed.size() >= 2
                ? NoveltyClass.NEW_METHOD_IDEA
                : NoveltyClass.TUNING_VARIANT;
    }

    public static void requireChineseName(String value) {
        String name = safe(value);
        if (name.length() < 2 || name.length() > 40
                || !name.matches(".*[\\u4e00-\\u9fff].*"))
            throw new IllegalArgumentException("R906_ASSET_CHINESE_NAME_REQUIRED");
    }

    public static boolean mayAffectShadow(
            Tier tier, ArtifactStatus artifact, String trainedUntil, String targetIssue,
            boolean certificateActive) {
        return tier == Tier.CERTIFIED_SHADOW
                && artifact == ArtifactStatus.PARITY_PASS
                && certificateActive
                && TargetIssueResolver.isValid(trainedUntil)
                && TargetIssueResolver.isValid(targetIssue)
                && TargetIssueResolver.compare(trainedUntil, targetIssue) < 0;
    }

    /** Research never owns REAL authorization. */
    public static boolean mayAuthorizeReal() { return false; }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private ResearchAssetContractR906() {}
}

