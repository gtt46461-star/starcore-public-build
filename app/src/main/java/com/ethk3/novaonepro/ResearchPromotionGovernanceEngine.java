package com.ethk3.novaonepro;

/** R811 promotion boundary: generic runtime artifacts may enter SHADOW; Production is build-gated. */
public final class ResearchPromotionGovernanceEngine {
    private ResearchPromotionGovernanceEngine() {}
    public static Decision evaluatePromotion(StrategyChallengerFactoryEngine.CandidateSpec candidate,ResearchCertificationEngineR811.Certificate certificate,boolean parityPass,boolean pitPass){if(candidate==null||certificate==null)return Decision.deny("MISSING_CERTIFICATE");if(!candidate.candidateId.equals(certificate.candidateId))return Decision.deny("CERTIFICATE_CANDIDATE_MISMATCH");if(!candidate.runtimeExecutable)return Decision.deny("BUILD_REQUIRED");if(!parityPass||!pitPass)return Decision.deny("PARITY_OR_PIT_FAIL");if(!"SHADOW_OBSERVE".equals(certificate.phase)&&!"RESEARCH_PROVEN_45".equals(certificate.phase))return Decision.deny("INSUFFICIENT_RESEARCH_EVIDENCE");return new Decision(true,"SHADOW_CHALLENGER","RESEARCH_TO_SHADOW_ONLY");}
    public static Decision allowResearchToShadow(StrategyChallengerFactoryEngine.CandidateSpec c,ResearchCertificationEngineR811.Certificate cert,boolean parity,boolean pit){return evaluatePromotion(c,cert,parity,pit);}
    public static Decision promoteToShadow(StrategyChallengerFactoryEngine.CandidateSpec c,ResearchCertificationEngineR811.Certificate cert){return Decision.deny("EXPLICIT_PARITY_AND_PIT_CERTIFICATES_REQUIRED");}
    public static Decision requireBuildApprovalForProduction(){return Decision.deny("R368_811_PRODUCTION_HOT_REPLACEMENT_FORBIDDEN");}
    public static Decision evaluateProductionPromotion(){return requireBuildApprovalForProduction();}
    public static Decision rollback(String candidateId,String reason){return new Decision(true,"ROLLED_BACK_TO_RESEARCH","candidate="+(candidateId==null?"":candidateId)+"|reason="+(reason==null?"":reason));}
    public static ArtifactPointer rollbackArtifactPointer(
            String currentArtifactHash, String previousArtifactHash, String reason) {
        if (!sha256(currentArtifactHash) || !sha256(previousArtifactHash))
            throw new IllegalArgumentException("R811_ROLLBACK_ARTIFACT_HASH");
        return new ArtifactPointer(
                previousArtifactHash,
                currentArtifactHash,
                "ROLLED_BACK_TO_PREVIOUS_RESEARCH_ARTIFACT",
                V51ChecksumEngine.sha256(
                        currentArtifactHash
                                + '|'
                                + previousArtifactHash
                                + '|'
                                + (reason == null ? "" : reason)));
    }
    private static boolean sha256(String value){return value!=null&&value.matches("[0-9a-fA-F]{64}");}
    public static final class Decision{public final boolean allowed;public final String state,reason;Decision(boolean a,String s,String r){allowed=a;state=s;reason=r;}static Decision deny(String r){return new Decision(false,"DENIED",r);}}
    public static final class ArtifactPointer{public final String activeArtifactHash,rolledBackArtifactHash,status,auditHash;ArtifactPointer(String active,String rolled,String status,String audit){activeArtifactHash=active;rolledBackArtifactHash=rolled;this.status=status;auditHash=audit;}}
}
