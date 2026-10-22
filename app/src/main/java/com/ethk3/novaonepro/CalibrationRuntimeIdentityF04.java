package com.ethk3.novaonepro;

/** Proposed runtime identity bridge. Build's explicit conservative NUMERIC closure includes
 * feature/PIT/trainer/state/selection implementations; PRESENTATION bytes are excluded.
 * This is deliberately not automatic old-evidence or certificate compatibility.
 * Changing an inline log within a NUMERIC file still requires a reviewed semantic proof.
 */
public final class CalibrationRuntimeIdentityF04 {
    public static final String REVISION="F04_NUMERIC_SCOPE_V1";
    public static final String EXACT_SCOPE_POLICY_F04="FULL1200_SCORE_DESC_GLOBAL_ORDINAL_ASC_CANDIDATE_CAL_ONLY_V1";
    private CalibrationRuntimeIdentityF04() {}
    private static String requireHash(String value) {
        if(value==null||!value.matches("[0-9a-f]{64}"))
            throw new IllegalStateException("F04_NUMERIC_SCOPE_PROOF_REQUIRED");
        return value;
    }
    public static String numericHash() {
        return requireHash(ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);
    }
    public static String executionScope(String key,String structure,String bundle) {
        if(key==null||key.isEmpty())throw new IllegalArgumentException("F04_SCOPE_CANDIDATE");
        return new ResearchSemanticsR967.Builder("F04_EXECUTION_SCOPE_V1")
                .text("game",V51Repository.GAME_CODE).text("candidate",key)
                .hash("structure",requireHash(structure)).hash("bundle",requireHash(bundle))
                .hash("numeric",numericHash())
                .hash("schema",requireHash(V51ActionMetaFeatureEngine.featureManifestHash()))
                .hash("policy",requireHash(ResearchGatePlanR967.installed().hash)).digest();
    }
    public static String exactScope(String candidate,String structure,String model,String selector) {
        if(candidate==null||!candidate.startsWith(FrozenCandidateAdapterR958.PREFIX))
            throw new IllegalArgumentException("F05_EXACT_SCOPE_IDENTITY");
        requireHash(structure);requireHash(model);requireHash(selector);
        return V51ChecksumEngine.sha256(EXACT_SCOPE_POLICY_F04+'|'+candidate+'|'+structure+'|'+model+'|'+selector);
    }
    /** Prospective legacy-score calibration identity. It grants no static-model certificate.
     * Mutable legacy VM diagnostic labels cannot be reassigned to immutable R967 evaluation.
     * Existing old-tree studies remain auditable; no automatic row/header migration is performed.
     */
    public static String legacyCalibrationBase(String key,String hypothesis,String structure,
            String bundle,String selector,String integrity) {
        return new ResearchSemanticsR967.Builder("F04_LEGACY_SCORE_CAL_V1")
                .text("game",V51Repository.GAME_CODE).text("candidate",key)
                .hash("hypothesis",requireHash(hypothesis)).hash("program",requireHash(structure))
                .hash("bundle",requireHash(bundle)).hash("selector",requireHash(selector))
                .hash("scope",executionScope(key,structure,bundle))
                .hash("calibrator",CandidateCalibrationR932.QUALITY_POLICY_HASH_CP12)
                .text("selected_scope_protocol",EXACT_SCOPE_POLICY_F04)
                .hash("integrity",requireHash(integrity))
                .text("role","LEGACY_SCORE_DIAGNOSTIC_NOT_STATIC_FUTURE").digest();
    }
    /** Existing signature is global. Runtime atom availability remains enforced at each
     * producer's exact used-dependency check; unrelated registry admission cannot reset all
     * frozen windows. AST/binding/model dependencies remain in immutable structure/bundle.
     */
    public static String formalContract() {
        return new ResearchSemanticsR967.Builder("F04_FORMAL_CONTRACT_V1")
                .text("game",V51Repository.GAME_CODE).hash("numeric",numericHash())
                .hash("schema",requireHash(V51ActionMetaFeatureEngine.featureManifestHash()))
                .hash("policy",requireHash(ResearchGatePlanR967.installed().hash))
                .hash("calibrator",CandidateCalibrationR932.QUALITY_POLICY_HASH_CP12).digest();
    }
}
