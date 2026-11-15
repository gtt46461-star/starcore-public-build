package com.ethk3.novaonepro;

/** Immutable V51 runtime values. All arrays are deep-copied at boundaries. */
public final class V51Models {
    private V51Models() {}

    public static final class DrawFrame {
        public final String issue;
        public final int[] permutation;
        public final long openTime;

        public DrawFrame(String issue, int[] permutation, long openTime) {
            this.issue = issue == null ? "" : issue;
            this.permutation = V51Math.copy(permutation);
            this.openTime = Math.max(0L, openTime);
            if (!V51Math.validPermutation(this.permutation))
                throw new IllegalArgumentException("V51_INVALID_PERMUTATION");
        }
    }

    public static final class PredictionContext {
        public final String baseIssue,
                targetIssue,
                historyMaxIssue,
                historyHash,
                cohortId,
                artifactBundleHash;
        public final long createdAt;
        public final int historyRows;

        public PredictionContext(
                String baseIssue,
                String targetIssue,
                String historyMaxIssue,
                String historyHash,
                String cohortId,
                String artifactBundleHash,
                long createdAt,
                int historyRows) {
            this.baseIssue = s(baseIssue);
            this.targetIssue = s(targetIssue);
            this.historyMaxIssue = s(historyMaxIssue);
            this.historyHash = s(historyHash);
            this.cohortId = s(cohortId);
            this.artifactBundleHash = s(artifactBundleHash);
            this.createdAt = Math.max(0L, createdAt);
            this.historyRows = Math.max(0, historyRows);
        }
    }

    public static final class PermutationFeatureSnapshot {
        public final PredictionContext context;
        public final double[] globalFeatures;
        public final double[][][] numberFeatures;
        public final String checksum;

        public PermutationFeatureSnapshot(
                PredictionContext context,
                double[] globalFeatures,
                double[][][] numberFeatures,
                String checksum) {
            this.context = context;
            this.globalFeatures = V51Math.copy(globalFeatures);
            this.numberFeatures = copy3(numberFeatures);
            this.checksum = s(checksum);
        }

        public boolean valid() {
            return context != null
                    && numberFeatures.length == 10
                    && numberFeatures[0].length == 10
                    && checksum.length() > 0;
        }
    }

    public static final class StateTrajectorySnapshot {
        public final String targetIssue, prototypeId, domainId, checksum;
        public final int position, runLength;
        public final V51Enums.Lifecycle lifecycle;
        public final double posterior, remainingLife, changePointRisk, confidence;

        public StateTrajectorySnapshot(
                String targetIssue,
                int position,
                String prototypeId,
                String domainId,
                V51Enums.Lifecycle lifecycle,
                int runLength,
                double posterior,
                double remainingLife,
                double changePointRisk,
                double confidence,
                String checksum) {
            this.targetIssue = s(targetIssue);
            this.position = position;
            this.prototypeId = s(prototypeId);
            this.domainId = s(domainId);
            this.lifecycle = lifecycle == null ? V51Enums.Lifecycle.UNKNOWN : lifecycle;
            this.runLength = Math.max(0, runLength);
            this.posterior = V51Math.clamp(posterior, 0, 1);
            this.remainingLife = Math.max(0, V51Math.finite(remainingLife));
            this.changePointRisk = V51Math.clamp(changePointRisk, 0, 1);
            this.confidence = V51Math.clamp(confidence, 0, 1);
            this.checksum = s(checksum);
        }
    }

    public static final class ExpertEvidence {
        public final String expertId, artifactId, checksum;
        public final double[][] rawScore;
        public final double[] trust, uncertainty, signalStrength;
        public final String[] evidence, counterEvidence;

        public ExpertEvidence(
                String expertId,
                String artifactId,
                double[][] rawScore,
                double[] trust,
                double[] uncertainty,
                double[] signalStrength,
                String[] evidence,
                String[] counterEvidence,
                String checksum) {
            this.expertId = s(expertId);
            this.artifactId = s(artifactId);
            this.rawScore = V51Math.copy(rawScore);
            this.trust = V51Math.copy(trust);
            this.uncertainty = V51Math.copy(uncertainty);
            this.signalStrength = V51Math.copy(signalStrength);
            this.evidence = copy(evidence);
            this.counterEvidence = copy(counterEvidence);
            this.checksum = s(checksum);
        }
    }

    public static final class NumberProbabilityMatrix {
        public final double[][] probability;
        public final double maxRowError, maxColumnError;
        public final int iterations;
        public final boolean converged, usedFallback;
        public final int[] mapPermutation;
        public final String checksum;

        public NumberProbabilityMatrix(
                double[][] probability,
                double maxRowError,
                double maxColumnError,
                int iterations,
                boolean converged,
                boolean usedFallback,
                int[] mapPermutation,
                String checksum) {
            this.probability = V51Math.copy(probability);
            this.maxRowError = Math.max(0, V51Math.finite(maxRowError));
            this.maxColumnError = Math.max(0, V51Math.finite(maxColumnError));
            this.iterations = Math.max(0, iterations);
            this.converged = converged;
            this.usedFallback = usedFallback;
            this.mapPermutation = V51Math.copy(mapPermutation);
            this.checksum = s(checksum);
        }

        public boolean validForDecision() {
            return converged
                    && !usedFallback
                    && probability.length == 10
                    && mapPermutation.length == 10;
        }
    }

    public static final class PositionAction {
        public final int position, n1, n2, n3, n4, rank;
        public final double rawActionP, rankScore, rankMargin, thirdMargin, entropy, stability;
        public final double[] metaFeatures;
        public final String fingerprint, checksum;

        public PositionAction(
                int position,
                int n1,
                int n2,
                int n3,
                int n4,
                double rawActionP,
                double rankScore,
                double rankMargin,
                double thirdMargin,
                double entropy,
                double stability,
                double[] metaFeatures,
                String fingerprint,
                int rank,
                String checksum) {
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.n4 = n4;
            this.rawActionP = V51Math.clamp(rawActionP, 0, 1);
            this.rankScore = V51Math.finite(rankScore);
            this.rankMargin = V51Math.finite(rankMargin);
            this.thirdMargin = V51Math.finite(thirdMargin);
            this.entropy = V51Math.clamp(entropy, 0, 1);
            this.stability = V51Math.clamp(stability, 0, 1);
            this.metaFeatures = V51Math.copy(metaFeatures);
            this.fingerprint = s(fingerprint);
            this.rank = rank;
            this.checksum = s(checksum);
        }

        public int[] top3() {
            return new int[] {n1, n2, n3};
        }
    }

    public static final class PredictabilityEvidence {
        public final double probability, stability, changePointRisk, falsifierRisk;
        public final boolean qualified;
        public final String checksum,missingReason;
        public final double[] inputFeatures;

        public PredictabilityEvidence(
                double probability,
                double stability,
                double changePointRisk,
                double falsifierRisk,
                boolean qualified,
                String checksum) {
            this(probability,stability,changePointRisk,falsifierRisk,qualified,checksum,null,"LEGACY_INPUT_NOT_CAPTURED");
        }
        public PredictabilityEvidence(double probability,double stability,double changePointRisk,double falsifierRisk,boolean qualified,String checksum,double[] inputs,String missing) {
            inputFeatures=inputs==null?new double[0]:inputs.clone();missingReason=s(missing);
            this.probability = Double.isFinite(probability)?V51Math.clamp(probability, 0, 1):Double.NaN;
            this.stability = V51Math.clamp(stability, 0, 1);
            this.changePointRisk = V51Math.clamp(changePointRisk, 0, 1);
            this.falsifierRisk = V51Math.clamp(falsifierRisk, 0, 1);
            this.qualified = qualified;
            this.checksum = s(checksum);
        }
    }

    public static final class DomainCertificate {
        public final String domainId, checksum;
        public final V51Enums.DomainStatus status;
        public final int sample;
        public final double hitRate, credibleLow, ece, blockMinimum, driftRisk;

        public DomainCertificate(
                String domainId,
                V51Enums.DomainStatus status,
                int sample,
                double hitRate,
                double credibleLow,
                double ece,
                double blockMinimum,
                double driftRisk,
                String checksum) {
            this.domainId = s(domainId);
            this.status = status == null ? V51Enums.DomainStatus.DISCOVERED : status;
            this.sample = Math.max(0, sample);
            this.hitRate = V51Math.clamp(hitRate, 0, 1);
            this.credibleLow = V51Math.clamp(credibleLow, 0, 1);
            this.ece = V51Math.clamp(ece, 0, 1);
            this.blockMinimum = V51Math.clamp(blockMinimum, 0, 1);
            this.driftRisk = V51Math.clamp(driftRisk, 0, 1);
            this.checksum = s(checksum);
        }
    }

    public static final class SelectedActionEnvelope {
        public final PredictionContext context;
        public final PositionAction action;
        public final double selectedHitP;
        public final PredictabilityEvidence predictability;
        public final DomainCertificate domain;
        public final V51Enums.Lane lane;
        public final double uncertainty, falsifierScore;
        public final String contractHash,selectionPolicyHash;

        public SelectedActionEnvelope(
                PredictionContext context,
                PositionAction action,
                double selectedHitP,
                PredictabilityEvidence predictability,
                DomainCertificate domain,
                V51Enums.Lane lane,
                double uncertainty,
                double falsifierScore,
                String contractHash) {
            this(context,action,selectedHitP,predictability,domain,lane,uncertainty,falsifierScore,contractHash,"");
        }
        public SelectedActionEnvelope(PredictionContext context,PositionAction action,double selectedHitP,PredictabilityEvidence predictability,
                DomainCertificate domain,V51Enums.Lane lane,double uncertainty,double falsifierScore,String contractHash,String selectionPolicyHash) {
            this.selectionPolicyHash=s(selectionPolicyHash);
            this.context = context;
            this.action = action;
            this.selectedHitP =
                    (Double.isNaN(selectedHitP) || Double.isInfinite(selectedHitP))
                            ? Double.NaN
                            : V51Math.clamp(selectedHitP, 0, 1);
            this.predictability = predictability;
            this.domain = domain;
            this.lane = lane == null ? V51Enums.Lane.HOLD : lane;
            this.uncertainty = V51Math.clamp(uncertainty, 0, 1);
            this.falsifierScore = V51Math.clamp(falsifierScore, 0, 1);
            this.contractHash = s(contractHash);
        }
    }

    public static final class AuthorizedDecision {
        public final V51Enums.DecisionAction action;
        public final V51Enums.RejectCode rejectCode;
        public final SelectedActionEnvelope selected;
        public final String decisionId, authorizationHash, artifactBundleHash;

        public AuthorizedDecision(
                V51Enums.DecisionAction action,
                V51Enums.RejectCode rejectCode,
                SelectedActionEnvelope selected,
                String decisionId,
                String authorizationHash,
                String artifactBundleHash) {
            this.action = action == null ? V51Enums.DecisionAction.HOLD : action;
            this.rejectCode =
                    rejectCode == null ? V51Enums.RejectCode.RUNTIME_EXCEPTION : rejectCode;
            this.selected = selected;
            this.decisionId = s(decisionId);
            this.authorizationHash = s(authorizationHash);
            this.artifactBundleHash = s(artifactBundleHash);
        }
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }

    private static String[] copy(String[] x) {
        if (x == null) return new String[0];
        String[] y = new String[x.length];
        System.arraycopy(x, 0, y, 0, x.length);
        return y;
    }

    private static double[][][] copy3(double[][][] x) {
        if (x == null) return new double[0][0][0];
        double[][][] y = new double[x.length][][];
        for (int i = 0; i < x.length; i++) y[i] = V51Math.copy(x[i]);
        return y;
    }

    /* STARCORE_TIANSHU_B02_MATERIALIZED_20260805 */
    public static final String B02_VERSION = "STARCORE_TIANSHU_B02_V51_MODELS_V1";
    public static final boolean B02_SHADOW_ONLY = true;
    public static final boolean B02_REAL_BET_HARD_DISABLED = true;

    public static final class FrozenDrawLedger {
        public final Models.B02DrawIdentity identity;
        public final String gamePartition;
        public final String dedupKey;
        public final String immutableHash;
        public final long writtenAt;

        public FrozenDrawLedger(Models.B02DrawIdentity identity, long writtenAt) {
            if (identity == null) throw new IllegalArgumentException("B02_IDENTITY_NULL");
            this.identity = identity;
            this.gamePartition = identity.gameCode;
            this.dedupKey =
                    identity.gameCode + "|" + identity.issue + "|" + identity.canonicalDrawHash;
            this.immutableHash = Models.b02Sha256(identity.canonical() + "|" + writtenAt);
            this.writtenAt = writtenAt;
        }

        public String canonical() {
            return identity.canonical()
                    + '|'
                    + gamePartition
                    + '|'
                    + dedupKey
                    + '|'
                    + immutableHash
                    + '|'
                    + writtenAt;
        }
    }

    public static final class FrozenPredictionLedger {
        public final Models.B02PitSnapshot pit;
        public final Models.B02EvidencePacket evidence;
        public final Models.B02Champion champion;
        public final Models.B02SelectedHitP selectedHitP;
        public final Models.B02Certificate certificate;
        public final String ledgerHash;
        public final boolean realTrade;

        public FrozenPredictionLedger(
                Models.B02PitSnapshot pit,
                Models.B02EvidencePacket evidence,
                Models.B02Champion champion,
                Models.B02SelectedHitP selectedHitP,
                Models.B02Certificate certificate) {
            if (pit == null
                    || evidence == null
                    || champion == null
                    || selectedHitP == null
                    || certificate == null)
                throw new IllegalArgumentException("B02_PREDICTION_COMPONENT_NULL");
            if (!pit.targetIssue.equals(champion.targetIssue)
                    || !pit.targetIssue.equals(selectedHitP.targetIssue))
                throw new IllegalArgumentException("B02_TARGET_ISSUE_MISMATCH");
            if (champion.realBetAuthorized || certificate.proven || selectedHitP.modelProven)
                throw new IllegalArgumentException("B02_PRODUCTION_LOCK");
            this.pit = pit;
            this.evidence = evidence;
            this.champion = champion;
            this.selectedHitP = selectedHitP;
            this.certificate = certificate;
            this.realTrade = false;
            this.ledgerHash =
                    Models.b02Sha256(
                            pit.contractHash()
                                    + '|'
                                    + evidence.contractHash()
                                    + '|'
                                    + champion.contractHash()
                                    + '|'
                                    + selectedHitP.contractHash()
                                    + '|'
                                    + certificate.contractHash());
        }

        public String canonical() {
            return pit.canonical()
                    + '|'
                    + evidence.canonical()
                    + '|'
                    + champion.canonical()
                    + '|'
                    + selectedHitP.canonical()
                    + '|'
                    + certificate.canonical()
                    + '|'
                    + ledgerHash
                    + "|0";
        }
    }

    public static final class FrozenSettlementLedger {
        public final String gameCode;
        public final String targetIssue;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final int actualNumber;
        public final boolean wouldHit;
        public final boolean realTrade;
        public final long settledAt;
        public final String predictionHash;
        public final String actualDrawHash;
        public final String settlementHash;

        public FrozenSettlementLedger(
                String gameCode,
                String targetIssue,
                int position,
                int n1,
                int n2,
                int n3,
                int actualNumber,
                boolean realTrade,
                long settledAt,
                String predictionHash,
                String actualDrawHash) {
            if (!"Binance1-PK10".equals(gameCode))
                throw new IllegalArgumentException("B02_SETTLE_GAME");
            if (position < 1 || position > 10)
                throw new IllegalArgumentException("B02_SETTLE_POSITION");
            if (actualNumber < 1 || actualNumber > 10)
                throw new IllegalArgumentException("B02_SETTLE_ACTUAL");
            if (realTrade) throw new IllegalArgumentException("B02_REAL_TRADE_DISABLED");
            this.gameCode = gameCode;
            this.targetIssue = targetIssue == null ? "" : targetIssue;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.actualNumber = actualNumber;
            this.wouldHit = actualNumber == n1 || actualNumber == n2 || actualNumber == n3;
            this.realTrade = false;
            this.settledAt = settledAt;
            this.predictionHash = predictionHash == null ? "" : predictionHash;
            this.actualDrawHash = actualDrawHash == null ? "" : actualDrawHash;
            this.settlementHash = Models.b02Sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return gameCode
                    + '|'
                    + targetIssue
                    + '|'
                    + position
                    + '|'
                    + n1
                    + '|'
                    + n2
                    + '|'
                    + n3
                    + '|'
                    + actualNumber
                    + '|'
                    + (wouldHit ? 1 : 0)
                    + "|0|"
                    + settledAt
                    + '|'
                    + predictionHash
                    + '|'
                    + actualDrawHash;
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + settlementHash;
        }
    }

    public static final class LedgerEnvelope {
        public final String ledgerType;
        public final String gameCode;
        public final String targetIssue;
        public final String payloadHash;
        public final String previousHash;
        public final long sequence;
        public final long createdAt;
        public final String envelopeHash;

        public LedgerEnvelope(
                String ledgerType,
                String gameCode,
                String targetIssue,
                String payloadHash,
                String previousHash,
                long sequence,
                long createdAt) {
            this.ledgerType = safe(ledgerType);
            this.gameCode = safe(gameCode);
            this.targetIssue = safe(targetIssue);
            this.payloadHash = safe(payloadHash);
            this.previousHash = safe(previousHash);
            this.sequence = sequence;
            this.createdAt = createdAt;
            if (!"Binance1-PK10".equals(this.gameCode))
                throw new IllegalArgumentException("B02_ENVELOPE_GAME");
            if (sequence < 0L || createdAt <= 0L || this.payloadHash.length() != 64)
                throw new IllegalArgumentException("B02_ENVELOPE_INVALID");
            this.envelopeHash = Models.b02Sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return ledgerType
                    + '|'
                    + gameCode
                    + '|'
                    + targetIssue
                    + '|'
                    + payloadHash
                    + '|'
                    + previousHash
                    + '|'
                    + sequence
                    + '|'
                    + createdAt;
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + envelopeHash;
        }
    }

    public static final class LedgerChain {
        private final java.util.ArrayList<LedgerEnvelope> entries =
                new java.util.ArrayList<LedgerEnvelope>();

        public synchronized LedgerEnvelope append(
                String type, String targetIssue, String payloadHash, long createdAt) {
            String previous =
                    entries.size() == 0
                            ? Models.b02Sha256("B02_GENESIS")
                            : entries.get(entries.size() - 1).envelopeHash;
            LedgerEnvelope e =
                    new LedgerEnvelope(
                            type,
                            "Binance1-PK10",
                            targetIssue,
                            payloadHash,
                            previous,
                            entries.size(),
                            createdAt);
            entries.add(e);
            return e;
        }

        public synchronized java.util.List<LedgerEnvelope> snapshot() {
            return java.util.Collections.unmodifiableList(
                    new java.util.ArrayList<LedgerEnvelope>(entries));
        }

        public synchronized boolean verify() {
            String previous = Models.b02Sha256("B02_GENESIS");
            for (int i = 0; i < entries.size(); i++) {
                LedgerEnvelope e = entries.get(i);
                if (e.sequence != i
                        || !previous.equals(e.previousHash)
                        || !e.envelopeHash.equals(Models.b02Sha256(e.canonicalWithoutHash())))
                    return false;
                previous = e.envelopeHash;
            }
            return true;
        }

        public synchronized String chainHash() {
            return entries.size() == 0
                    ? Models.b02Sha256("B02_GENESIS")
                    : entries.get(entries.size() - 1).envelopeHash;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class FieldContract {
        public final String id, name, type, owner, defaultValue, failureMode;
        public final boolean required, immutable;

        public FieldContract(
                String id,
                String name,
                String type,
                String owner,
                String defaultValue,
                String failureMode,
                boolean required,
                boolean immutable) {
            this.id = safe(id);
            this.name = safe(name);
            this.type = safe(type);
            this.owner = safe(owner);
            this.defaultValue = safe(defaultValue);
            this.failureMode = safe(failureMode);
            this.required = required;
            this.immutable = immutable;
        }

        public String canonical() {
            return id
                    + ':'
                    + name
                    + ':'
                    + type
                    + ':'
                    + owner
                    + ':'
                    + defaultValue
                    + ':'
                    + failureMode
                    + ':'
                    + (required ? 1 : 0)
                    + ':'
                    + (immutable ? 1 : 0);
        }
    }

    public static java.util.List<FieldContract> b02FieldContracts() {
        java.util.ArrayList<FieldContract> out = new java.util.ArrayList<FieldContract>();
        out.add(
                new FieldContract(
                        "B02-FIELD-001",
                        "game_code",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-002",
                        "provider_issue",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-003",
                        "gmt_issue",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-004",
                        "issue",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-005",
                        "raw_open_time",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-006",
                        "provider_open_time",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-007",
                        "fetch_time",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-008",
                        "source_line",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-009",
                        "canonical_draw_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-010",
                        "raw_payload_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-011",
                        "numbers_blob",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-012",
                        "dirty",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-013",
                        "dirty_reason",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-014",
                        "base_issue",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-015",
                        "target_issue",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-016",
                        "history_max_issue",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-017",
                        "frozen_at",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-018",
                        "provider_cutoff_time",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-019",
                        "history_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-020",
                        "feature_schema_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-021",
                        "state_schema_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-022",
                        "model_bundle_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-023",
                        "future_leak_free",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-024",
                        "shadow_only",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-025",
                        "packet_id",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-026",
                        "pit_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-027",
                        "draw_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-028",
                        "feature_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-029",
                        "state_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-030",
                        "expert_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-031",
                        "number_probability_hash",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-032",
                        "action_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-033",
                        "certificate_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-034",
                        "evidence_ids",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-035",
                        "counter_evidence_ids",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-036",
                        "complete",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-037",
                        "position",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-038",
                        "n1",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-039",
                        "n2",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-040",
                        "n3",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-041",
                        "action_index",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-042",
                        "raw_probability",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-043",
                        "calibrated_probability",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-044",
                        "selected_hit_p",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-045",
                        "lane",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-046",
                        "champion_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-047",
                        "model_proven",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-048",
                        "real_bet_authorized",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-049",
                        "certificate_id",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-050",
                        "domain_id",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-051",
                        "sample",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-052",
                        "oos_hit_rate",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-053",
                        "oos_coverage",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-054",
                        "ece",
                        "REAL",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-055",
                        "block_results",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-056",
                        "artifact_hash",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-057",
                        "finding_id",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-058",
                        "severity",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-059",
                        "owner",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-060",
                        "code",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-061",
                        "summary",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-062",
                        "repair_action",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-063",
                        "created_at",
                        "INTEGER",
                        "V51Models",
                        "0",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        out.add(
                new FieldContract(
                        "B02-FIELD-064",
                        "blocks_production",
                        "TEXT",
                        "V51Models",
                        "",
                        "HOLD_OR_SHADOW",
                        true,
                        true));
        return java.util.Collections.unmodifiableList(out);
    }

    public static String b02FieldContractHash() {
        StringBuilder sb = new StringBuilder(B02_VERSION);
        java.util.List<FieldContract> list = b02FieldContracts();
        for (int i = 0; i < list.size(); i++) sb.append('|').append(list.get(i).canonical());
        return Models.b02Sha256(sb.toString());
    }

    public static final class LedgerInvariant {
        public final String id;
        public final String owner;
        public final String predicate;
        public final String failureMode;
        public final boolean hard;

        public LedgerInvariant(
                String id, String owner, String predicate, String failureMode, boolean hard) {
            this.id = safe(id);
            this.owner = safe(owner);
            this.predicate = safe(predicate);
            this.failureMode = safe(failureMode);
            this.hard = hard;
        }

        public String canonical() {
            return id + ':' + owner + ':' + predicate + ':' + failureMode + ':' + (hard ? 1 : 0);
        }
    }

    public static java.util.List<LedgerInvariant> b02LedgerInvariants() {
        java.util.ArrayList<LedgerInvariant> out = new java.util.ArrayList<LedgerInvariant>();
        out.add(
                new LedgerInvariant(
                        "B02-INV-001",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-002",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-003",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-004",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-005",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-006",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-007",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-008",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-009",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-010",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-011",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-012",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-013",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-014",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-015",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-016",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-017",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-018",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-019",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-020",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-021",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-022",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-023",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-024",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-025",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-026",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-027",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-028",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-029",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-030",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-031",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-032",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-033",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-034",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-035",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-036",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-037",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-038",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-039",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-040",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-041",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-042",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-043",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-044",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-045",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-046",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-047",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-048",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-049",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-050",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-051",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-052",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-053",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-054",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-055",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-056",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-057",
                        "V51Models",
                        "game_partition_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-058",
                        "V51Models",
                        "immutable_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-059",
                        "V51Models",
                        "sequence_monotonic_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-060",
                        "V51Models",
                        "previous_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-061",
                        "V51Models",
                        "payload_hash_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-062",
                        "V51Models",
                        "target_issue_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-063",
                        "V51Models",
                        "shadow_only_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        out.add(
                new LedgerInvariant(
                        "B02-INV-064",
                        "V51Models",
                        "real_trade_disabled_must_hold",
                        "HOLD_OR_SHADOW",
                        true));
        return java.util.Collections.unmodifiableList(out);
    }

    public static String b02InvariantHash() {
        StringBuilder sb = new StringBuilder(B02_VERSION);
        for (LedgerInvariant i : b02LedgerInvariants()) sb.append('|').append(i.canonical());
        return Models.b02Sha256(sb.toString());
    }

    public static boolean b02SelfTest() {
        LedgerChain c = new LedgerChain();
        c.append("DRAW", "I1", Models.b02Sha256("D1"), 1L);
        c.append("PREDICTION", "I2", Models.b02Sha256("D2"), 2L);
        return c.verify()
                && b02FieldContracts().size() == 64
                && b02LedgerInvariants().size() == 64
                && b02FieldContractHash().length() == 64
                && b02InvariantHash().length() == 64
                && B02_REAL_BET_HARD_DISABLED;
    }
}
