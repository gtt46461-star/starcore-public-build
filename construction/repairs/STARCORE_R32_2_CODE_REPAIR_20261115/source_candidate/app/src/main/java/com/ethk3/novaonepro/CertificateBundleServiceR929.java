package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unified certificate and model-bundle service for algorithms, generated assets and formula graphs.
 *
 * <p>A certificate can grant bounded ranking influence only.  This service has no Trade writer and
 * no method capable of authorizing a REAL action.  L5 is shadow re-ranking, L6 starts at a five
 * percent cap, and every larger cap requires a new independent chronological window.</p>
 */
public final class CertificateBundleServiceR929 {
    public static final String VERSION = "R959_CERTIFICATE_BUNDLE_SERVICE_V4";
    public static final double MIN_ENTRY_HIT_RATE = 0.34d;
    public static final double MAX_ENTRY_BRIER = 0.24d;
    public static final int MIN_INFLUENCE_FUTURE_N = 360, MIN_INFLUENCE_PAIRED_N = 360;
    public static final int MIN_INDEPENDENT_WINDOWS = 2;
    public static final double MIN_WILSON_LOWER = 0.30d, MIN_EVIDENCE_COVERAGE = 0.15d;
    public static final double MAX_ENTRY_ECE = 0.04d, MAX_ADJUSTED_P = 0.05d;
    // Evidence coverage is sample support, NOT a cap or quota on current REAL opportunities.
    public static final boolean MAY_CREATE_REAL = false;
    public static final double L6_WEIGHT_CAP = 0.05d;
    public static final double L7_WEIGHT_CAP = 0.30d;

    private CertificateBundleServiceR929() {}

    public enum Consumer { INVENTED_ALGORITHM, GENESIS_ASSET, FORMULA_GRAPH }
    public enum Level {
        L0_IDEA,
        L1_NURSERY,
        L2_OBSERVING,
        L3_EVIDENCE_READY,
        L4_CHALLENGER,
        L5_SHADOW,
        L6_CONTROLLED_INFLUENCE,
        L7_CHAMPION
    }
    public enum CertificateState { ACTIVE, PAUSED, REVOKED, LEGACY_REVIEW }
    public enum BundleState { STAGED, ACTIVE, REVOKED, ROLLED_BACK }

    public static final class CandidateKey {
        public final Consumer consumer;
        public final String namespace;
        public final String familyId;
        public final String candidateId;
        public final String structuralHash;
        public final String keyHash;

        public CandidateKey(Consumer consumer, String namespace, String familyId,
                String candidateId, String structuralHash) {
            if (consumer == null) throw new IllegalArgumentException("R929_CONSUMER_REQUIRED");
            this.consumer = consumer;
            this.namespace = required(namespace, "CANDIDATE_NAMESPACE");
            this.familyId = required(familyId, "CANDIDATE_FAMILY");
            this.candidateId = required(candidateId, "CANDIDATE_ID");
            this.structuralHash = requireHash(structuralHash, "CANDIDATE_STRUCTURAL_HASH");
            this.keyHash = sha256(consumer.name() + '|' + this.namespace + '|'
                    + this.familyId + '|' + this.candidateId + '|' + this.structuralHash);
        }
    }

    public static final class GateObservation {
        public final String gateId;
        public final double observed;
        public final double required;
        public final boolean pass;
        public final String source;
        public final String sourceHash;
        public final String detail;
        public final String gateHash;

        public GateObservation(String gateId, double observed, double required, boolean pass,
                String source, String sourceHash, String detail) {
            this.gateId = required(gateId, "GATE_ID");
            if (!finite(observed) || !finite(required))
                throw new IllegalArgumentException("R929_GATE_NONFINITE");
            this.observed = observed;
            this.required = required;
            this.pass = pass;
            this.source = required(source, "GATE_SOURCE");
            this.sourceHash = requireHash(sourceHash, "GATE_SOURCE_HASH");
            this.detail = clean(detail);
            this.gateHash = sha256(this.gateId + '|' + observed + '|' + required + '|'
                    + (pass ? 1 : 0) + '|' + this.source + '|' + this.sourceHash + '|'
                    + this.detail);
        }
    }

    public static final class EvidenceInput {
        public final boolean pitPass;
        public final boolean deterministicPass;
        public final boolean parityPass;
        public final int futureN;
        public final int pairedN;
        public final int independentWindows;
        public final double hitRate;
        public final double wilsonLower;
        public final double coverage;
        public final double brier;
        public final double ece;
        public final double pairedDelta;
        public final double fwerAdjustedP;
        public final boolean domainSupported;
        public final boolean resourcePass;
        public final String evidenceHash;

        public EvidenceInput(boolean pitPass, boolean deterministicPass, boolean parityPass,
                int futureN, int pairedN, int independentWindows, double hitRate,
                double wilsonLower, double coverage, double brier, double ece,
                double pairedDelta, double fwerAdjustedP, boolean domainSupported,
                boolean resourcePass, String evidenceHash) {
            if (futureN < 0 || pairedN < 0 || independentWindows < 0
                    || !metricOrUnknown(hitRate) || !metricOrUnknown(wilsonLower)
                    || !metricOrUnknown(coverage) || !metricOrUnknown(brier)
                    || !metricOrUnknown(ece) || !finiteOrUnknown(pairedDelta)
                    || !metricOrUnknown(fwerAdjustedP))
                throw new IllegalArgumentException("R929_EVIDENCE_INPUT_INVALID");
            this.pitPass = pitPass;
            this.deterministicPass = deterministicPass;
            this.parityPass = parityPass;
            this.futureN = futureN;
            this.pairedN = pairedN;
            this.independentWindows = independentWindows;
            this.hitRate = hitRate;
            this.wilsonLower = wilsonLower;
            this.coverage = coverage;
            this.brier = brier;
            this.ece = ece;
            this.pairedDelta = pairedDelta;
            this.fwerAdjustedP = fwerAdjustedP;
            this.domainSupported = domainSupported;
            this.resourcePass = resourcePass;
            this.evidenceHash = requireHash(evidenceHash, "EVIDENCE_HASH");
        }
    }

    public static final class Certificate {
        public final CandidateKey candidate;
        public final Level level;
        public final CertificateState state;
        public final String bundleHash;
        public final String schemaHash;
        public final String atomGraphHash;
        public final String modelHash;
        public final String policyHash;
        public final String calibrationHash;
        public final String domainHash;
        public final String trialRegistryHash;
        public final String selectionHash;
        public final String codeHash;
        public final String validFromIssue;
        public final String validUntilIssue;
        public final long issuedAtMs;
        public final double maximumWeight;
        public final List<GateObservation> gates;
        public final String reason;
        public final String certificateHash;

        public Certificate(CandidateKey candidate, Level level, CertificateState state,
                String bundleHash, String schemaHash, String atomGraphHash, String modelHash,
                String policyHash, String calibrationHash, String domainHash,
                String trialRegistryHash, String selectionHash, String codeHash,
                String validFromIssue, String validUntilIssue, long issuedAtMs,
                double maximumWeight, List<GateObservation> gates, String reason) {
            if (candidate == null || level == null || state == null || issuedAtMs <= 0L
                    || !finite(maximumWeight) || maximumWeight < 0.0d
                    || maximumWeight > capFor(level))
                throw new IllegalArgumentException("R929_CERTIFICATE_HEADER_INVALID");
            this.candidate = candidate;
            this.level = level;
            this.state = state;
            this.bundleHash = requireHash(bundleHash, "CERT_BUNDLE_HASH");
            this.schemaHash = requireHash(schemaHash, "CERT_SCHEMA_HASH");
            this.atomGraphHash = requireHash(atomGraphHash, "CERT_ATOM_GRAPH_HASH");
            this.modelHash = requireHash(modelHash, "CERT_MODEL_HASH");
            this.policyHash = requireHash(policyHash, "CERT_POLICY_HASH");
            this.calibrationHash = requireHash(calibrationHash, "CERT_CALIBRATION_HASH");
            this.domainHash = requireHash(domainHash, "CERT_DOMAIN_HASH");
            this.trialRegistryHash = requireHash(trialRegistryHash, "CERT_TRIAL_HASH");
            this.selectionHash = requireHash(selectionHash, "CERT_SELECTION_HASH");
            this.codeHash = requireHash(codeHash, "CERT_CODE_HASH");
            this.validFromIssue = required(validFromIssue, "CERT_VALID_FROM");
            this.validUntilIssue = clean(validUntilIssue);
            if (this.validUntilIssue.length() > 0
                    && PitFeatureDagR929.compareIssues(this.validFromIssue, this.validUntilIssue) > 0)
                throw new IllegalArgumentException("R929_CERTIFICATE_ISSUE_RANGE");
            this.issuedAtMs = issuedAtMs;
            this.maximumWeight = maximumWeight;
            ArrayList<GateObservation> copy = new ArrayList<GateObservation>();
            if (gates != null) copy.addAll(gates);
            Collections.sort(copy, new Comparator<GateObservation>() {
                @Override public int compare(GateObservation a, GateObservation b) {
                    return a.gateId.compareTo(b.gateId);
                }
            });
            Set<String> gateIds = new HashSet<String>();
            for (GateObservation gate : copy) {
                if (gate == null || !gateIds.add(gate.gateId))
                    throw new IllegalArgumentException("R929_CERT_GATE_DUPLICATE");
            }
            this.gates = Collections.unmodifiableList(copy);
            this.reason = required(reason, "CERT_REASON");
            this.certificateHash = sha256(canonicalWithoutHash());
        }

        public boolean activeFor(String targetIssue) {
            String target = required(targetIssue, "CERT_TARGET_ISSUE");
            if (state != CertificateState.ACTIVE
                    || PitFeatureDagR929.compareIssues(target, validFromIssue) < 0)
                return false;
            return validUntilIssue.length() == 0
                    || PitFeatureDagR929.compareIssues(target, validUntilIssue) <= 0;
        }

        public double authorizedWeight(String targetIssue) {
            if (!activeFor(targetIssue) || level.ordinal() < Level.L6_CONTROLLED_INFLUENCE.ordinal())
                return 0.0d;
            double cap=validatedInfluenceCap(gates);
            return Math.min(maximumWeight,Math.min(cap,capFor(level)));
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(1024);
            out.append(candidate.keyHash).append('|').append(level.name()).append('|')
                    .append(state.name()).append('|').append(bundleHash).append('|')
                    .append(schemaHash).append('|').append(atomGraphHash).append('|')
                    .append(modelHash).append('|').append(policyHash).append('|')
                    .append(calibrationHash).append('|').append(domainHash).append('|')
                    .append(trialRegistryHash).append('|').append(selectionHash).append('|')
                    .append(codeHash).append('|').append(validFromIssue).append('|')
                    .append(validUntilIssue).append('|').append(issuedAtMs).append('|')
                    .append(maximumWeight).append('|').append(reason);
            for (GateObservation gate : gates) out.append('\n').append(gate.gateHash);
            return out.toString();
        }
    }

    public static final class Evaluation {
        public final Level level;
        public final double weightCap;
        public final List<GateObservation> gates;
        public final String reason;
        public final String evaluationHash;

        Evaluation(Level level, List<GateObservation> gates, String reason) {
            this.level = level;
            this.weightCap = Math.min(capFor(level),validatedInfluenceCap(gates));
            this.gates = Collections.unmodifiableList(new ArrayList<GateObservation>(gates));
            this.reason = reason;
            StringBuilder value = new StringBuilder(level.name()).append('|').append(reason);
            for (GateObservation gate : gates) value.append('|').append(gate.gateHash);
            this.evaluationHash = sha256(value.toString());
        }
    }

    /** Applies one identical ladder to all three consumer classes. */
    public static Evaluation evaluate(EvidenceInput input) {
        if (input == null) throw new IllegalArgumentException("R929_EVIDENCE_REQUIRED");
        ArrayList<GateObservation> gates = new ArrayList<GateObservation>();
        gates.add(gate("PIT", input.pitPass ? 1.0d : 0.0d, 1.0d, input.pitPass, input));
        gates.add(gate("DETERMINISM", input.deterministicPass ? 1.0d : 0.0d, 1.0d,
                input.deterministicPass, input));
        gates.add(gate("PARITY", input.parityPass ? 1.0d : 0.0d, 1.0d,
                input.parityPass, input));
        gates.add(gate("INDEPENDENT_WINDOWS",input.independentWindows,MIN_INDEPENDENT_WINDOWS,input.independentWindows>=MIN_INDEPENDENT_WINDOWS,input));
        gates.add(gate("FUTURE_N", input.futureN, 120.0d, input.futureN >= 120, input));
        gates.add(gate("PAIRED_N", input.pairedN, 120.0d, input.pairedN >= 120, input));
        gates.add(gate("HIT_RATE", input.hitRate, MIN_ENTRY_HIT_RATE,
                metric(input.hitRate) && input.hitRate >= MIN_ENTRY_HIT_RATE, input));
        gates.add(gate("WILSON_LOWER", input.wilsonLower, MIN_WILSON_LOWER,
                metric(input.wilsonLower) && input.wilsonLower >= MIN_WILSON_LOWER, input));
        gates.add(gate("COVERAGE", input.coverage, MIN_EVIDENCE_COVERAGE,
                metric(input.coverage) && input.coverage >= MIN_EVIDENCE_COVERAGE, input));
        gates.add(gate("BRIER", input.brier, MAX_ENTRY_BRIER,
                metric(input.brier) && input.brier <= MAX_ENTRY_BRIER, input));
        gates.add(gate("ECE", input.ece, MAX_ENTRY_ECE,
                metric(input.ece) && input.ece <= MAX_ENTRY_ECE, input));
        gates.add(gate("PAIRED_DELTA", input.pairedDelta, 0.0d,
                finite(input.pairedDelta) && input.pairedDelta > 0.0d, input));
        gates.add(gate("FWER_ADJUSTED_P", input.fwerAdjustedP, MAX_ADJUSTED_P,
                metric(input.fwerAdjustedP) && input.fwerAdjustedP <= MAX_ADJUSTED_P, input));
        gates.add(gate("DOMAIN_SUPPORT", input.domainSupported ? 1.0d : 0.0d, 1.0d,
                input.domainSupported, input));
        gates.add(gate("RESOURCE", input.resourcePass ? 1.0d : 0.0d, 1.0d,
                input.resourcePass, input));

        if (!input.pitPass || !input.deterministicPass || !input.parityPass)
            return new Evaluation(Level.L1_NURSERY, gates, "HARD_INTEGRITY_GATE_FAILED");
        if (input.futureN < 48)
            return new Evaluation(Level.L2_OBSERVING, gates, "COLLECT_NEW_FUTURE_EVIDENCE");
        if (input.futureN < 120 || input.pairedN < 120)
            return new Evaluation(Level.L3_EVIDENCE_READY, gates, "PAIR_SUPPORT_INCOMPLETE");
        if (!metric(input.hitRate) || !metric(input.coverage) || !metric(input.ece))
            return new Evaluation(Level.L3_EVIDENCE_READY, gates, "CALIBRATED_METRICS_MISSING");
        if (input.hitRate < MIN_ENTRY_HIT_RATE)
            return new Evaluation(Level.L3_EVIDENCE_READY, gates, "HIT_RATE_ENTRY_NOT_MET");
        if (input.hitRate < MIN_ENTRY_HIT_RATE || input.coverage < MIN_EVIDENCE_COVERAGE || input.ece > MAX_ENTRY_ECE
                || !input.domainSupported || !input.resourcePass)
            return new Evaluation(Level.L4_CHALLENGER, gates, "QUALITY_GATE_NOT_MET");
        for (GateObservation observation : gates) {
            if (!observation.pass)
                return new Evaluation(Level.L4_CHALLENGER, gates,
                        "CP3_GATE_NOT_MET|" + observation.gateId);
        }
        if (input.independentWindows < 1)
            return new Evaluation(Level.L4_CHALLENGER, gates, "NO_INDEPENDENT_PROMOTION_WINDOW");
        if (input.futureN < MIN_INFLUENCE_FUTURE_N || input.pairedN < MIN_INFLUENCE_PAIRED_N || input.independentWindows < MIN_INDEPENDENT_WINDOWS)
            return new Evaluation(Level.L5_SHADOW, gates, "L5_SHADOW_REQUIRES_120_PLUS_NEW_240");
        if (input.independentWindows < 3 || input.futureN < 600 || input.pairedN < 600)
            return new Evaluation(Level.L6_CONTROLLED_INFLUENCE, gates,
                    "L6_WEIGHT_5_PERCENT_NEW_240_WINDOW");
        if (input.fwerAdjustedP > MAX_ADJUSTED_P || input.pairedDelta <= 0.0d)
            return new Evaluation(Level.L6_CONTROLLED_INFLUENCE, gates,
                    "KEEP_L6_UNTIL_ADJUSTED_PAIRED_GAIN");
        return new Evaluation(Level.L7_CHAMPION, gates,
                "L7_CHAMPION_INDEPENDENT_EVIDENCE_COMPLETE");
    }

    /** Recompute required observations; an empty/all-PASS list cannot manufacture authority. */
    public static double validatedInfluenceCap(List<GateObservation> observations){
        Map<String,GateObservation> g=new HashMap<String,GateObservation>();
        if(observations==null)return 0;for(GateObservation x:observations){if(x==null||g.put(x.gateId,x)!=null||!x.pass)return 0;}
        if(g.containsKey("CP4_POLICY"))return validatedCp4Cap(g);
        String[] hard={"PIT","DETERMINISM","PARITY","DOMAIN_SUPPORT","RESOURCE"};
        for(String id:hard)if(!atLeast(g,id,1))return 0;
        if(!atLeast(g,"FUTURE_N",MIN_INFLUENCE_FUTURE_N)||!atLeast(g,"PAIRED_N",MIN_INFLUENCE_PAIRED_N)||!atLeast(g,"INDEPENDENT_WINDOWS",MIN_INDEPENDENT_WINDOWS)
                ||!atLeast(g,"HIT_RATE",MIN_ENTRY_HIT_RATE)||!atLeast(g,"WILSON_LOWER",MIN_WILSON_LOWER)||!atLeast(g,"COVERAGE",MIN_EVIDENCE_COVERAGE)
                ||!atMost(g,"BRIER",MAX_ENTRY_BRIER)||!atMost(g,"ECE",MAX_ENTRY_ECE)||!atMost(g,"FWER_ADJUSTED_P",MAX_ADJUSTED_P)
                ||!atLeast(g,"PAIRED_DELTA",Double.MIN_VALUE))return 0;
        double n=Math.min(g.get("FUTURE_N").observed,g.get("PAIRED_N").observed),windows=g.get("INDEPENDENT_WINDOWS").observed;
        return n>=1200&&windows>=5?.30:n>=840&&windows>=4?.20:n>=600&&windows>=3?.12:.05;
    }
    private static boolean atLeast(Map<String,GateObservation> g,String id,double value){GateObservation x=g.get(id);return x!=null&&finite(x.observed)&&x.observed>=value;}
    private static boolean atMost(Map<String,GateObservation> g,String id,double value){GateObservation x=g.get(id);return x!=null&&finite(x.observed)&&x.observed>=0&&x.observed<=value;}

    /** CP4 uses one frozen e-process lifetime; legacy FWER/paired-delta gates do not interpret it. */
    public static Evaluation evaluateCp4(org.json.JSONObject m,List<GateObservation> external) {
        ArrayList<GateObservation> g=new ArrayList<GateObservation>();
        try {
            ResearchGatePlanR967.Policy p=ResearchGatePlanR967.installed();
            if(!p.isCp3 || !"CP4_CERTIFICATE_METRICS_V1".equals(m.getString("revision")) || !p.hash.equals(m.getString("policy_hash")))
                throw new IllegalArgumentException("CP4_POLICY_IDENTITY");
            String evidence=requireHash(m.getString("evidence_hash"),"CP4_EVIDENCE");
            g.add(new GateObservation("CP4_POLICY",1,1,true,"CP4_CERTIFICATE_METRICS_V1",p.hash,"FROZEN_POLICY"));
            g.add(new GateObservation("CP4_IDENTITY",1,1,true,"CP4_FROZEN_IDENTITY",requireHash(m.getString("identity_hash"),"CP4_IDENTITY"),"IDENTITY"));
            g.add(new GateObservation("CP4_LIFETIME",1,1,true,"CP4_FROZEN_LIFETIME",requireHash(m.getString("lifetime_id"),"CP4_LIFETIME"),"ONE_HYPOTHESIS"));
            boolean integrity=m.getBoolean("integrity");
            for(String id:new String[]{"PIT","DETERMINISM","PARITY"})g.add(cp4Gate(id,integrity?1:0,1,integrity,evidence,"FROZEN_EXECUTION_VERIFIED"));
            cp4Min(g,m,"FUTURE_N","future_n",360,evidence);cp4Min(g,m,"PAIRED_N","paired_n",360,evidence);cp4Min(g,m,"INDEPENDENT_WINDOWS","independent_windows",2,evidence);
            cp4Min(g,m,"HIT_RATE","hit_rate",.34,evidence);double lower=m.getDouble("wilson_lower");g.add(cp4Gate("WILSON_LOWER",lower,.3,lower>.3,evidence,"IID_DESCRIPTIVE_NOT_SEQUENTIAL_PROOF"));
            cp4Min(g,m,"COVERAGE","coverage",.95,evidence);cp4Max(g,m,"BRIER","brier",.24,evidence);cp4Max(g,m,"ECE","ece",.04,evidence);
            double b=m.getDouble("brier"),baseline=m.getDouble("baseline_brier");g.add(cp4Gate("BASELINE_BRIER",baseline,0,metric(baseline),evidence,"SAME_WINDOW_CONSTANT_POINT_30"));g.add(cp4Gate("BRIER_RELATIVE",b,baseline,metric(b)&&metric(baseline)&&b<=baseline,evidence,"SAME_WINDOW"));
            cp4Max(g,m,"MAX_LOSS_STREAK","max_loss_streak",12,evidence);cp4Max(g,m,"RESIDUAL_DRAWDOWN","residual_drawdown",8,evidence);cp4Min(g,m,"MIN_FIXED_WINDOW_RATE","min_fixed_window_rate",.30,evidence);
            int[] windows={20,50,100,300};double[] minima={.15,.22,.27,.34};for(int i=0;i<windows.length;i++)cp4Min(g,m,"ROLLING_"+windows[i],"rolling_"+windows[i]+"_rate",minima[i],evidence);
            double alpha=m.getDouble("alpha"),logE=m.getDouble("log_e"),anytime=m.getDouble("anytime_p");
            g.add(cp4Gate("LIFETIME_ALPHA",alpha,.05,alpha>0&&alpha<=.05,evidence,"SUMMABLE_GLOBAL_ALPHA"));
            g.add(cp4Gate("CURRENT_LOG_E",logE,alpha>0?-Math.log(alpha):0,alpha>0&&logE>=-Math.log(alpha),evidence,"CURRENT_CROSSING_REQUIRED"));
            g.add(cp4Gate("ANYTIME_P",anytime,alpha,metric(anytime)&&alpha>0&&anytime<=alpha,evidence,"EXP_MINUS_MAX_LOG_E"));
            if(external!=null)g.addAll(external);
            double cap=validatedInfluenceCap(g);
            if(cap<=0){String reason="CP4_REQUIRED_EVIDENCE_MISSING";for(GateObservation x:g)if(!x.pass){reason="CP4_GATE_NOT_MET|"+x.gateId;break;}return new Evaluation(Level.L4_CHALLENGER,g,reason);}
            return new Evaluation(Level.L6_CONTROLLED_INFLUENCE,g,"CP4_CERTIFIED_FIXED_CAP_0_05_NO_REAL_AUTHORITY");
        } catch(org.json.JSONException invalid) {throw new IllegalArgumentException("CP4_METRIC_FIELDS",invalid);}
    }
    private static GateObservation cp4Gate(String id,double observed,double required,boolean pass,String evidence,String detail){return new GateObservation(id,finite(observed)?observed:-1,finite(required)?required:-1,pass&&finite(observed)&&finite(required),"CP4_FROZEN_EVIDENCE",evidence,detail);}
    private static void cp4Min(List<GateObservation> out,org.json.JSONObject m,String id,String key,double min,String evidence)throws org.json.JSONException{double v=m.getDouble(key);out.add(cp4Gate(id,v,min,v>=min,evidence,key));}
    private static void cp4Max(List<GateObservation> out,org.json.JSONObject m,String id,String key,double max,String evidence)throws org.json.JSONException{double v=m.getDouble(key);out.add(cp4Gate(id,v,max,v>=0&&v<=max,evidence,key));}
    private static double validatedCp4Cap(Map<String,GateObservation> g){
        GateObservation policy=g.get("CP4_POLICY");if(policy==null||!"CP4_CERTIFICATE_METRICS_V1".equals(policy.source)||!ResearchGatePlanR967.installed().hash.equals(policy.sourceHash))return 0;
        for(String id:new String[]{"CP4_POLICY","CP4_IDENTITY","CP4_LIFETIME","PIT","DETERMINISM","PARITY","REGIME_ROBUSTNESS","RESOURCE_INTEGRITY"})if(!atLeast(g,id,1))return 0;
        if(!atLeast(g,"DOMAIN_SUPPORT",1)||!atMost(g,"DOMAIN_SUPPORT",15))return 0;
        if(!atLeast(g,"FUTURE_N",360)||!atLeast(g,"PAIRED_N",360)||!atLeast(g,"INDEPENDENT_WINDOWS",2)||!atLeast(g,"HIT_RATE",.34)||!atLeast(g,"COVERAGE",.95))return 0;
        if(!g.containsKey("WILSON_LOWER")||g.get("WILSON_LOWER").observed<=.3)return 0;
        if(!atMost(g,"BRIER",.24)||!atMost(g,"ECE",.04)||!atMost(g,"MAX_LOSS_STREAK",12)||!atMost(g,"RESIDUAL_DRAWDOWN",8)||!atLeast(g,"MIN_FIXED_WINDOW_RATE",.3))return 0;
        int[] w={20,50,100,300};double[] m={.15,.22,.27,.34};for(int i=0;i<w.length;i++)if(!atLeast(g,"ROLLING_"+w[i],m[i]))return 0;
        if(!atMost(g,"BASELINE_BRIER",1)||!g.containsKey("BRIER_RELATIVE")||g.get("BRIER_RELATIVE").observed>g.get("BASELINE_BRIER").observed||g.get("BRIER_RELATIVE").observed!=g.get("BRIER").observed)return 0;
        if(!atMost(g,"LIFETIME_ALPHA",.05)||g.get("LIFETIME_ALPHA").observed<=0)return 0;
        double alpha=g.get("LIFETIME_ALPHA").observed;
        if(!g.containsKey("CURRENT_LOG_E")||g.get("CURRENT_LOG_E").observed < -Math.log(alpha)||!atMost(g,"ANYTIME_P",alpha))return 0;
        if(!atMost(g,"DRIFT",.1)||!atLeast(g,"RESOURCE_N",50)||!atMost(g,"RESOURCE_LATENCY_P95_MS",700)||!atMost(g,"RESOURCE_HEAP_INCREMENT",67108864))return 0;
        for(String id:new String[]{"ATOM_BINDING_ADMISSION","PREDICTION_SIMILARITY","ERROR_SIMILARITY"})if(!g.containsKey(id)||!g.get(id).pass)return 0;
        if(!g.containsKey("DIVERSITY")||!g.containsKey("DUPLICATE_PAIRED_ADVANTAGE"))return 0;
        double similarity=g.get("DIVERSITY").observed,adv=g.get("DUPLICATE_PAIRED_ADVANTAGE").observed;
        if(!metric(similarity)||!finite(adv)||adv < -1||adv>1||(similarity>=.98&&adv<=0))return 0;
        return .05; // policy consumer_cap; no n-dependent rise to legacy 0.30
    }
    public static String cp4CurrentDomainReason(DbStore store,String lifetimeId,String target){return CandidateResourceEvidenceCP4.currentDomainReason(store,lifetimeId,target);}
    public static List<GateObservation> cp4ExternalGates(android.database.sqlite.SQLiteDatabase db,String lifetimeId,String evidenceHash,String endpoint){
        ArrayList<GateObservation> gates=new ArrayList<GateObservation>(CandidateResourceEvidenceCP4.gates(db,lifetimeId,evidenceHash,endpoint));
        gates.add(cp4AtomBindings(db,lifetimeId,endpoint));
        return gates;
    }

    /** The publisher prepares immutable atom evidence outside the certificate writer.
     * Bind the actual frozen AST/model to the same lifetime used by all other CP4 gates. */
    private static GateObservation cp4AtomBindings(android.database.sqlite.SQLiteDatabase db,String lifetime,String endpoint){
        if(db.inTransaction())throw new IllegalStateException("ATOM_CERTIFICATE_PREPARE_IN_WRITER");
        String key="",structure="",state="",first="";
        try {
            try(android.database.Cursor bound=db.rawQuery("SELECT candidate_key,identity_json,first_issue,last_issue,length(CAST(identity_json AS BLOB)) FROM "+ResearchGatePlanR967.CP4_LIFETIME+" WHERE lifetime_id=? AND length(CAST(identity_json AS BLOB)) BETWEEN 2 AND 32768",new String[]{lifetime})){
                if(!bound.moveToFirst())throw new IllegalStateException("ATOM_CERTIFICATE_LIFETIME_MISSING");
                if(bound.getLong(4)<2||bound.getLong(4)>32768)throw new IllegalStateException("ATOM_CERTIFICATE_IDENTITY_BOUND");
                key=bound.getString(0);first=bound.getString(2);
                if(!TargetIssueResolver.isValid(endpoint)||endpoint.compareTo(first)<0||endpoint.compareTo(bound.getString(3))>0)
                    throw new IllegalStateException("ATOM_CERTIFICATE_ENDPOINT_SCOPE");
                org.json.JSONObject identity=new org.json.JSONObject(bound.getString(1));
                if(!key.equals(identity.getString("candidate")))throw new IllegalStateException("ATOM_CERTIFICATE_CANDIDATE_SCOPE");
                structure=identity.getString("structure");state=identity.getString("state");
            }
            if(!key.startsWith("algorithm:"))throw new IllegalStateException("ATOM_CERTIFICATE_FROZEN_OWNER_REQUIRED");
            try(com.ethk3.novaonepro.r952.AtomAdmissionCP12.Group group=com.ethk3.novaonepro.r952.AtomAdmissionCP12.readForLifetimeCP12(db,lifetime,endpoint,System.nanoTime()+700000000L)){
                if(group.eligible&&(!structure.equals(group.freezeHash)||!state.equals(group.modelHash)))
                    throw new IllegalStateException("ATOM_CERTIFICATE_FROZEN_IDENTITY_CHANGED");
                return group.gateCP12();
            }
        }catch(org.json.JSONException invalid){
            return missingAtomBindingGate(lifetime,endpoint,"ATOM_CERTIFICATE_IDENTITY_JSON");
        }catch(RuntimeException unavailable){
            if(unavailable instanceof ResearchWorkBudgetR927.Yield)throw unavailable;
            String reason=unavailable.getMessage();
            return missingAtomBindingGate(lifetime,endpoint,reason==null?unavailable.getClass().getSimpleName():reason);
        }
    }
    private static GateObservation missingAtomBindingGate(String lifetime,String endpoint,String reason){
        return new GateObservation(com.ethk3.novaonepro.r952.AtomAdmissionCP12.GATE,0,1,false,"IMMUTABLE_CONSUMED_ATOM_OOS",V51ChecksumEngine.sha256("CP12_ATOM_CERTIFICATE_MISSING|"+lifetime+'|'+endpoint+'|'+reason),reason);
    }

    private static GateObservation gate(String id, double observed, double required,
            boolean pass, EvidenceInput input) {
        double safeObserved = finite(observed) ? observed : -1.0d;
        return new GateObservation(id, safeObserved, required, pass, "FUTURE_OOS_LEDGER",
                input.evidenceHash, pass ? "PASS" : "NOT_PASS");
    }

    public static Certificate legacyReview(CandidateKey key, String bundleHash,
            String schemaHash, String atomGraphHash, String modelHash, String policyHash,
            String calibrationHash, String domainHash, String trialHash, String selectionHash,
            String codeHash, String validFromIssue, long issuedAtMs) {
        return new Certificate(key, Level.L0_IDEA, CertificateState.LEGACY_REVIEW,
                bundleHash, schemaHash, atomGraphHash, modelHash, policyHash, calibrationHash,
                domainHash, trialHash, selectionHash, codeHash, validFromIssue, "", issuedAtMs,
                0.0d, Collections.<GateObservation>emptyList(),
                "LEGACY_REVIEW_NO_AUTOMATIC_WEIGHT");
    }

    public static double capFor(Level level) {
        if (level == null || level.ordinal() < Level.L6_CONTROLLED_INFLUENCE.ordinal()) return 0.0d;
        return level == Level.L6_CONTROLLED_INFLUENCE ? L6_WEIGHT_CAP : L7_WEIGHT_CAP;
    }

    public static final class CertificateService {
        private final Map<String, Certificate> current = new LinkedHashMap<String, Certificate>();
        private final Map<String, Set<String>> dependents = new LinkedHashMap<String, Set<String>>();

        public synchronized void put(Certificate certificate, List<String> dependencyCertificateHashes) {
            if (certificate == null) throw new IllegalArgumentException("R929_CERT_REQUIRED");
            Certificate existing = current.get(certificate.candidate.keyHash);
            if (existing != null && existing.issuedAtMs > certificate.issuedAtMs)
                throw new IllegalStateException("R929_CERTIFICATE_TIME_REGRESSION");
            current.put(certificate.candidate.keyHash, certificate);
            if (dependencyCertificateHashes != null) {
                for (String parent : dependencyCertificateHashes) {
                    String parentHash = requireHash(parent, "DEPENDENCY_CERT_HASH");
                    Set<String> children = dependents.get(parentHash);
                    if (children == null) {
                        children = new LinkedHashSet<String>();
                        dependents.put(parentHash, children);
                    }
                    children.add(certificate.certificateHash);
                }
            }
        }

        public synchronized Certificate get(CandidateKey key) {
            return key == null ? null : current.get(key.keyHash);
        }

        public synchronized Authorization authorize(CandidateKey key, String targetIssue,
                String expectedBundleHash, String expectedSchemaHash, String expectedPolicyHash,
                String expectedCalibrationHash, double requestedWeight) {
            Certificate certificate = get(key);
            if (certificate == null)
                return Authorization.denied("NO_CERTIFICATE");
            if (!certificate.bundleHash.equals(requireHash(expectedBundleHash, "EXPECTED_BUNDLE_HASH"))
                    || !certificate.schemaHash.equals(requireHash(expectedSchemaHash, "EXPECTED_SCHEMA_HASH"))
                    || !certificate.policyHash.equals(requireHash(expectedPolicyHash, "EXPECTED_POLICY_HASH"))
                    || !certificate.calibrationHash.equals(requireHash(
                            expectedCalibrationHash, "EXPECTED_CALIBRATION_HASH")))
                return Authorization.denied("CERTIFICATE_DEPENDENCY_MISMATCH");
            double authorized = certificate.authorizedWeight(targetIssue);
            if (!finite(requestedWeight) || requestedWeight < 0.0d || requestedWeight > authorized)
                return Authorization.denied("REQUESTED_WEIGHT_EXCEEDS_CERTIFICATE");
            if (requestedWeight <= 0.0d)
                return Authorization.denied("ZERO_WEIGHT_SHADOW_ONLY");
            return new Authorization(true, requestedWeight, certificate.level,
                    certificate.certificateHash, "RANKING_INFLUENCE_ONLY");
        }

        /** Revokes a certificate and every certificate in its dependency closure. */
        public synchronized Set<String> revokeClosure(String certificateHash, String reason,
                long nowMs) {
            String root = requireHash(certificateHash, "REVOKE_CERT_HASH");
            if (nowMs <= 0L) throw new IllegalArgumentException("R929_REVOKE_TIME");
            LinkedHashSet<String> closure = new LinkedHashSet<String>();
            ArrayList<String> queue = new ArrayList<String>();
            queue.add(root);
            for (int cursor = 0; cursor < queue.size(); cursor++) {
                String hash = queue.get(cursor);
                if (!closure.add(hash)) continue;
                Set<String> children = dependents.get(hash);
                if (children != null) queue.addAll(children);
            }
            ArrayList<Map.Entry<String, Certificate>> entries =
                    new ArrayList<Map.Entry<String, Certificate>>(current.entrySet());
            for (Map.Entry<String, Certificate> entry : entries) {
                Certificate value = entry.getValue();
                if (!closure.contains(value.certificateHash)) continue;
                Certificate revoked = new Certificate(value.candidate, value.level,
                        CertificateState.REVOKED, value.bundleHash, value.schemaHash,
                        value.atomGraphHash, value.modelHash, value.policyHash,
                        value.calibrationHash, value.domainHash, value.trialRegistryHash,
                        value.selectionHash, value.codeHash, value.validFromIssue,
                        value.validUntilIssue, nowMs, 0.0d, value.gates,
                        "REVOKED:" + required(reason, "REVOKE_REASON"));
                current.put(entry.getKey(), revoked);
            }
            return Collections.unmodifiableSet(closure);
        }

        public synchronized List<Certificate> snapshot() {
            return Collections.unmodifiableList(new ArrayList<Certificate>(current.values()));
        }
    }

    public static final class Authorization {
        public final boolean authorized;
        public final double rankingWeight;
        public final Level level;
        public final String certificateHash;
        public final String reason;
        public final boolean mayCreateReal;
        public final String authorizationHash;

        Authorization(boolean authorized, double rankingWeight, Level level,
                String certificateHash, String reason) {
            this.authorized = authorized;
            this.rankingWeight = rankingWeight;
            this.level = level;
            this.certificateHash = clean(certificateHash);
            this.reason = reason;
            this.mayCreateReal = false;
            this.authorizationHash = sha256((authorized ? 1 : 0) + "|" + rankingWeight + "|"
                    + (level == null ? "NONE" : level.name()) + "|" + this.certificateHash
                    + "|" + reason + "|directREAL=0");
        }

        static Authorization denied(String reason) {
            return new Authorization(false, 0.0d, null, "", reason);
        }
    }

    public static final class BundleArtifact {
        public final String role;
        public final String type;
        public final String relativePath;
        public final String contentHash;
        public final long sizeBytes;
        public final String schemaHash;
        public final String inputStage;
        public final String parentArtifactHash;
        public final String certificateHash;
        public final String artifactEntryHash;

        public BundleArtifact(String role, String type, String relativePath, String contentHash,
                long sizeBytes, String schemaHash, String inputStage,
                String parentArtifactHash, String certificateHash) {
            this.role = required(role, "BUNDLE_ROLE");
            this.type = required(type, "BUNDLE_TYPE");
            this.relativePath = safeRelativePath(relativePath);
            this.contentHash = requireHash(contentHash, "BUNDLE_CONTENT_HASH");
            if (sizeBytes < 0L || sizeBytes > 64L * 1024L * 1024L)
                throw new IllegalArgumentException("R929_BUNDLE_ARTIFACT_SIZE");
            this.sizeBytes = sizeBytes;
            this.schemaHash = requireHash(schemaHash, "BUNDLE_SCHEMA_HASH");
            this.inputStage = required(inputStage, "BUNDLE_INPUT_STAGE");
            this.parentArtifactHash = clean(parentArtifactHash).length() == 0
                    ? "" : requireHash(parentArtifactHash, "BUNDLE_PARENT_HASH");
            this.certificateHash = requireHash(certificateHash, "BUNDLE_CERT_HASH");
            this.artifactEntryHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            return role + '|' + type + '|' + relativePath + '|' + contentHash + '|'
                    + sizeBytes + '|' + schemaHash + '|' + inputStage + '|'
                    + parentArtifactHash + '|' + certificateHash;
        }
    }

    public static final class BundleManifest {
        private static final String[] REQUIRED_ROLES = new String[] {
            "NUMBER", "GATE", "RANKER", "CALIBRATION", "PREDICTABILITY", "POLICY"
        };

        public final String bundleId;
        public final String game;
        public final String schemaHash;
        public final String codeHash;
        public final String createdAtIssue;
        public final String earliestUseIssue;
        public final String signingKeyId;
        public final String signatureAlgorithm;
        public final List<BundleArtifact> artifacts;
        private final byte[] signatureBytes;
        public final String unsignedManifestHash;
        public final String bundleHash;

        public BundleManifest(String bundleId, String game, String schemaHash, String codeHash,
                String createdAtIssue, String earliestUseIssue, String signingKeyId,
                String signatureAlgorithm, List<BundleArtifact> artifacts, byte[] signatureBytes) {
            this.bundleId = required(bundleId, "BUNDLE_ID");
            this.game = required(game, "BUNDLE_GAME");
            this.schemaHash = requireHash(schemaHash, "BUNDLE_SCHEMA_HASH");
            this.codeHash = requireHash(codeHash, "BUNDLE_CODE_HASH");
            this.createdAtIssue = required(createdAtIssue, "BUNDLE_CREATED_ISSUE");
            this.earliestUseIssue = required(earliestUseIssue, "BUNDLE_EARLIEST_ISSUE");
            if (!PitFeatureDagR929.isStrictPast(createdAtIssue, earliestUseIssue))
                throw new IllegalArgumentException("R929_BUNDLE_EARLIEST_NOT_AFTER_CREATE");
            this.signingKeyId = required(signingKeyId, "BUNDLE_KEY_ID");
            this.signatureAlgorithm = required(signatureAlgorithm, "BUNDLE_SIGNATURE_ALGORITHM");
            ArrayList<BundleArtifact> copy = new ArrayList<BundleArtifact>();
            if (artifacts != null) copy.addAll(artifacts);
            Collections.sort(copy, new Comparator<BundleArtifact>() {
                @Override public int compare(BundleArtifact a, BundleArtifact b) {
                    return a.role.compareTo(b.role);
                }
            });
            TreeMap<String, BundleArtifact> byRole = new TreeMap<String, BundleArtifact>();
            long totalBytes = 0L;
            for (BundleArtifact artifact : copy) {
                if (artifact == null || byRole.put(artifact.role, artifact) != null)
                    throw new IllegalArgumentException("R929_BUNDLE_DUPLICATE_ROLE");
                if (!artifact.schemaHash.equals(this.schemaHash))
                    throw new IllegalArgumentException("R929_BUNDLE_SCHEMA_DRIFT");
                totalBytes += artifact.sizeBytes;
                if (totalBytes > 128L * 1024L * 1024L)
                    throw new IllegalArgumentException("R929_BUNDLE_TOTAL_SIZE");
            }
            for (String role : REQUIRED_ROLES) if (!byRole.containsKey(role))
                throw new IllegalArgumentException("R929_BUNDLE_REQUIRED_ROLE_MISSING:" + role);
            this.artifacts = Collections.unmodifiableList(copy);
            this.signatureBytes = signatureBytes == null
                    ? new byte[0] : Arrays.copyOf(signatureBytes, signatureBytes.length);
            this.unsignedManifestHash = sha256(canonicalUnsigned());
            this.bundleHash = sha256(this.unsignedManifestHash + '|'
                    + sha256Bytes(this.signatureBytes));
        }

        public byte[] signatureCopy() { return Arrays.copyOf(signatureBytes, signatureBytes.length); }

        String canonicalUnsigned() {
            StringBuilder out = new StringBuilder(1024);
            out.append(bundleId).append('|').append(game).append('|').append(schemaHash)
                    .append('|').append(codeHash).append('|').append(createdAtIssue).append('|')
                    .append(earliestUseIssue).append('|').append(signingKeyId).append('|')
                    .append(signatureAlgorithm);
            for (BundleArtifact artifact : artifacts)
                out.append('\n').append(artifact.artifactEntryHash);
            return out.toString();
        }
    }

    public interface ArtifactResolver {
        byte[] read(String relativePath) throws Exception;
    }

    public static final class StagedBundle {
        public final BundleManifest manifest;
        public final BundleState state;
        public final long verifiedAtMs;
        public final String verificationHash;
        public final String reason;

        StagedBundle(BundleManifest manifest, BundleState state, long verifiedAtMs,
                String verificationHash, String reason) {
            this.manifest = manifest;
            this.state = state;
            this.verifiedAtMs = verifiedAtMs;
            this.verificationHash = verificationHash;
            this.reason = reason;
        }

        StagedBundle withState(BundleState next, String reason, long nowMs) {
            return new StagedBundle(manifest, next, nowMs,
                    sha256(verificationHash + '|' + next.name() + '|' + reason + '|' + nowMs),
                    reason);
        }
    }

    public static StagedBundle verifyAndStage(BundleManifest manifest, ArtifactResolver resolver,
            PublicKey trustedOwnerKey, String expectedKeyId, long nowMs) {
        if (manifest == null || resolver == null || trustedOwnerKey == null || nowMs <= 0L)
            throw new IllegalArgumentException("R929_BUNDLE_STAGE_INPUT_INVALID");
        if (!manifest.signingKeyId.equals(required(expectedKeyId, "EXPECTED_KEY_ID")))
            throw new IllegalArgumentException("R929_BUNDLE_KEY_ID_MISMATCH");
        if (!"SHA256withRSA".equals(manifest.signatureAlgorithm)
                && !"SHA256withECDSA".equals(manifest.signatureAlgorithm))
            throw new IllegalArgumentException("R929_BUNDLE_SIGNATURE_ALGORITHM");
        long total = 0L;
        StringBuilder verified = new StringBuilder(manifest.unsignedManifestHash);
        try {
            for (BundleArtifact artifact : manifest.artifacts) {
                byte[] bytes = resolver.read(artifact.relativePath);
                if (bytes == null || bytes.length != artifact.sizeBytes)
                    throw new IllegalArgumentException("R929_BUNDLE_SIZE_MISMATCH:" + artifact.role);
                String hash = sha256Bytes(bytes);
                if (!hash.equals(artifact.contentHash))
                    throw new IllegalArgumentException("R929_BUNDLE_HASH_MISMATCH:" + artifact.role);
                total += bytes.length;
                if (total > 128L * 1024L * 1024L)
                    throw new IllegalArgumentException("R929_BUNDLE_UNPACK_LIMIT");
                verified.append('|').append(artifact.artifactEntryHash).append(':').append(hash);
            }
            Signature verifier = Signature.getInstance(manifest.signatureAlgorithm);
            verifier.initVerify(trustedOwnerKey);
            verifier.update(manifest.canonicalUnsigned().getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(manifest.signatureBytes))
                throw new IllegalArgumentException("R929_BUNDLE_SIGNATURE_INVALID");
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalArgumentException("R929_BUNDLE_VERIFY_FAILED", failure);
        }
        return new StagedBundle(manifest, BundleState.STAGED, nowMs,
                sha256(verified.toString()), "VERIFIED_COMPLETE_STAGED");
    }

    /** Atomic pointer; each target issue pins exactly one complete dependency set. */
    public static final class AtomicBundleRegistry {
        private final AtomicReference<StagedBundle> active = new AtomicReference<StagedBundle>();
        private final Map<String, StagedBundle> pinnedByIssue =
                new LinkedHashMap<String, StagedBundle>();
        private StagedBundle pending;
        private StagedBundle previous;

        public synchronized void stage(StagedBundle bundle) {
            if (bundle == null || bundle.state != BundleState.STAGED)
                throw new IllegalArgumentException("R929_ONLY_VERIFIED_STAGED_BUNDLE");
            pending = bundle;
        }

        public synchronized StagedBundle activateAtIssueBoundary(String targetIssue, long nowMs) {
            String issue = required(targetIssue, "ACTIVATION_TARGET");
            if (pending == null) return active.get();
            if (PitFeatureDagR929.compareIssues(issue, pending.manifest.earliestUseIssue) < 0)
                throw new IllegalStateException("R929_BUNDLE_BEFORE_EARLIEST_USE");
            if (pinnedByIssue.containsKey(issue))
                throw new IllegalStateException("R929_CANNOT_SWAP_PINNED_ISSUE");
            previous = active.get();
            StagedBundle activated = pending.withState(BundleState.ACTIVE,
                    "ATOMIC_ACTIVATE_AT_ISSUE_BOUNDARY:" + issue, nowMs);
            active.set(activated);
            pending = null;
            return activated;
        }

        public synchronized StagedBundle pinForIssue(String targetIssue) {
            String issue = required(targetIssue, "PIN_TARGET");
            StagedBundle pinned = pinnedByIssue.get(issue);
            if (pinned != null) return pinned;
            StagedBundle current = active.get();
            if (current == null || current.state != BundleState.ACTIVE)
                throw new IllegalStateException("R929_NO_ACTIVE_BUNDLE");
            pinnedByIssue.put(issue, current);
            return current;
        }

        public synchronized StagedBundle rollback(String reason, long nowMs) {
            StagedBundle current = active.get();
            if (current == null) return null;
            StagedBundle rolled = current.withState(BundleState.ROLLED_BACK,
                    required(reason, "ROLLBACK_REASON"), nowMs);
            if (previous != null && previous.state == BundleState.ACTIVE)
                active.set(previous);
            else active.set(null);
            pending = null;
            return rolled;
        }

        public synchronized String pinnedBundleHash(String targetIssue) {
            StagedBundle value = pinnedByIssue.get(clean(targetIssue));
            return value == null ? "" : value.manifest.bundleHash;
        }

        public StagedBundle active() { return active.get(); }
    }

    private static String safeRelativePath(String path) {
        String value = required(path, "BUNDLE_PATH").replace('\\', '/');
        if (value.startsWith("/") || value.startsWith("./") || value.contains("../")
                || value.contains("//") || value.indexOf('\u0000') >= 0)
            throw new IllegalArgumentException("R929_BUNDLE_PATH_INVALID");
        return value;
    }

    private static boolean metric(double value) {
        return finite(value) && value >= 0.0d && value <= 1.0d;
    }
    private static boolean metricOrUnknown(double value) {
        return metric(value) || value == -1.0d;
    }
    private static boolean finiteOrUnknown(double value) {
        return finite(value) || value == -1.0d;
    }
    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R929_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (clean.length() != 64) throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        }
        return clean;
    }
    public static String sha256(String text) {
        return sha256Bytes(text.getBytes(StandardCharsets.UTF_8));
    }
    public static String sha256Bytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes == null ? new byte[0] : bytes);
            StringBuilder out = new StringBuilder(64);
            for (byte item : hash)
                out.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return out.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R929_SHA256_UNAVAILABLE", failure);
        }
    }
}
