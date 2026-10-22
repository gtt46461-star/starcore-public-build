package com.ethk3.novaonepro;

/** Deterministic complete audit bundle renderer. It never overwrites, loads or installs code. */
public final class CandidateCodeSynthesisEngine {
    private CandidateCodeSynthesisEngine() {}
    public static Bundle synthesize(StrategyChallengerFactoryEngine.CandidateSpec candidate){if(candidate==null)throw new IllegalArgumentException("R811_CODEGEN_CANDIDATE");String java=renderJava(candidate),patch=renderPatch(candidate),manifest=renderManifest(candidate),fixtures=renderFixtures(candidate);SecureCodegenGuardR811.Validation imports=SecureCodegenGuardR811.validateImports(java),tokens=SecureCodegenGuardR811.validateTokens(java);if(!imports.accepted||!tokens.accepted||!SecureCodegenGuardR811.withinQuota(4,java.length()+patch.length()+manifest.length()+fixtures.length()))throw new SecurityException("R811_CODEGEN_GUARD:"+imports.code+':'+tokens.code);String hash=V51ChecksumEngine.sha256(java+'\n'+patch+'\n'+manifest+'\n'+fixtures);return new Bundle(candidate.candidateId,candidate.runtimeExecutable?"SOURCE_READY_RUNTIME_DSL_VERIFIED":"OFFLINE_ARTIFACT_REQUIRED",java,patch,manifest,fixtures,hash);}
    public static String renderJava(StrategyChallengerFactoryEngine.CandidateSpec c){
        String id=identifier(c.candidateId),spec=javaString(c.canonicalSpec);
        StringBuilder out=new StringBuilder(4096);
        out.append("package com.ethk3.novaonepro.research.generated;\n\n")
                .append("import java.util.Map;\n")
                .append("import com.ethk3.novaonepro.ResearchDslCandidateRuntimeR811;\n")
                .append("import com.ethk3.novaonepro.ResearchModelCandidateRuntimeR811;\n\n")
                .append("import com.ethk3.novaonepro.ResearchArtifactCodecR811;\n\n")
                .append("/** Generated Research/SHADOW challenger; no production authority. */\n")
                .append("public final class ").append(id).append(" {\n")
                .append("  public static final String CANDIDATE_ID=\"").append(javaString(c.candidateId)).append("\";\n")
                .append("  public static final String CANDIDATE_HASH=\"").append(c.candidateHash).append("\";\n")
                .append("  public static final String DATASET_HASH=\"").append(c.datasetHash).append("\";\n")
                .append("  public static final String CANONICAL_SPEC=\"").append(spec).append("\";\n")
                .append("  public static final boolean RESEARCH_ONLY=true;\n")
                .append("  public static final boolean MAY_AUTHORIZE_REAL=false;\n")
                .append("  private ").append(id).append("() {}\n");
        if(c.type==StrategyChallengerFactoryEngine.CandidateType.FORMULA){
            out.append("  public static double score(Map<String,Double> features) {\n")
                    .append("    ResearchDslCandidateRuntimeR811.Evaluation value=ResearchDslCandidateRuntimeR811.evaluateFormulaCandidate(CANDIDATE_ID,CANONICAL_SPEC,features);\n")
                    .append("    return \"SHADOW_ONLY\".equals(value.status)?value.score:Double.NaN;\n  }\n");
        }else if(c.type==StrategyChallengerFactoryEngine.CandidateType.MODEL&&c.canonicalSpec.indexOf("TINY_MLP")>=0){
            out.append("  public static double[] infer(double[] input,double[][] w1,double[] b1,double[][] w2,double[] b2,double[][] w3,double[] b3) {\n")
                    .append("    return ResearchModelCandidateRuntimeR811.inferTinyMlp96x64x32x100(input,w1,b1,w2,b2,w3,b3);\n  }\n")
                    .append("  public static ResearchModelCandidateRuntimeR811.NeuralInference inferFrozen(byte[] artifactBytes,String expectedHash,double[] input,String targetIssue,double temperature) {\n")
                    .append("    if(!ResearchArtifactCodecR811.verify(artifactBytes,expectedHash)) throw new IllegalArgumentException(\"R811_GENERATED_ARTIFACT_HASH\");\n")
                    .append("    return ResearchModelCandidateRuntimeR811.inferTinyMlpArtifact(ResearchArtifactCodecR811.decode(artifactBytes),input,targetIssue,temperature);\n  }\n");
        }else{
            out.append("  public static String executableSpecification() { return CANONICAL_SPEC; }\n");
        }
        out.append("}\n");return out.toString();
    }
    public static String renderPatch(StrategyChallengerFactoryEngine.CandidateSpec c){String path="research/generated/"+identifier(c.candidateId)+".java";return "# RESEARCH_ONLY\n# candidate="+c.candidateId+"\n# production_mutation=0\n# operation=ADD\n# path="+path+"\n# spec_sha256="+V51ChecksumEngine.sha256(c.canonicalSpec)+"\n# rollback=DELETE_RESEARCH_GENERATED_FILE_AND_RETAIN_PRODUCTION\n";}
    public static String renderManifest(StrategyChallengerFactoryEngine.CandidateSpec c){return "candidate_id="+c.candidateId+"\ncandidate_hash="+c.candidateHash+"\ndataset_hash="+c.datasetHash+"\ncandidate_type="+c.type.name()+"\nseed="+c.seed+"\ncomplexity="+c.complexity+"\nruntime_executable="+c.runtimeExecutable+"\nstatus="+c.status+"\nproduction_owner=NONE\nshadow_only=true\nreal_authority=false\n";}
    public static String renderFixtures(StrategyChallengerFactoryEngine.CandidateSpec c){return "fixture.candidate="+c.candidateHash+"\nfixture.dataset="+c.datasetHash+"\nfixture.seed="+c.seed+"\nfixture.deterministic=1\nfixture.pit_required=1\nfixture.group_by_target_issue=1\nfixture.expected_action_universe=1200\nfixture.real_forbidden=1\nfixture.replay_hash="+V51ChecksumEngine.sha256(c.candidateHash+'|'+c.datasetHash+'|'+c.seed)+"\n";}
    private static String identifier(String value){String x=(value==null?"Candidate":value).replaceAll("[^A-Za-z0-9_]","_");if(x.length()==0||Character.isDigit(x.charAt(0)))x="C_"+x;return x;}
    private static String javaString(String value){return (value==null?"":value).replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    public static final class Bundle{public final String candidateId,status,javaSource,patch,manifest,fixtures,bundleHash;Bundle(String id,String s,String j,String p,String m,String f,String h){candidateId=id;status=s;javaSource=j;patch=p;manifest=m;fixtures=f;bundleHash=h;}}
}
