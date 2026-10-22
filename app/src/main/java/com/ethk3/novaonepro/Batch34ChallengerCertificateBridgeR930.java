package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * R930 bridge from the long-running R908 algorithm evidence ledger into the Batch-3/4 lifecycle.
 *
 * <p>Historical evidence may qualify an algorithm for L4 CHALLENGER observation, but is never
 * copied into the new Future support counter.  The zero-weight L4 certificate therefore makes the
 * challenger/certificate pipeline real without granting production authority.  New pre-draw
 * R908 freezes are mirrored exactly-once as Batch34 SHADOW applications.  Only fresh post-admission
 * Future labels may move the certificate ladder, and every certificate remains incapable of
 * creating a REAL decision.</p>
 */
public final class Batch34ChallengerCertificateBridgeR930 {
    public static final String VERSION = "R931_LEGACY_OBSERVATION_BRIDGE_V2";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    public static final int LEGACY_MIN_N = 1000;
    public static final double LEGACY_MIN_HIT_RATE = 0.35d;
    public static final double LEGACY_MIN_WILSON = 0.30d;
    public static final double LEGACY_MIN_BLOCK = 0.22d;
    public static final double LEGACY_MIN_REGIME = 0.18d;
    public static final double LEGACY_MIN_COVERAGE = 0.50d;
    public static final int MAX_ADMISSIONS_PER_TICK = 4;

    private static final String EVIDENCE_ADMISSION = "LEGACY_L4_ADMISSION";
    private static final String EVIDENCE_SHADOW = "LEGACY_PREDRAW_OBSERVATION";
    private static final String EVIDENCE_FRESH = "FRESH_FUTURE_CERTIFICATE_EVALUATION";

    private Batch34ChallengerCertificateBridgeR930() {}

    public static final class Summary {
        public final int scanned;
        public final int eligible;
        public final int admitted;
        public final int challengers;
        public final int certificates;
        public final int shadowApplications;
        public final int freshSettled;
        public final int controlledInfluenceEligible;
        public final String status;

        Summary(int scanned, int eligible, int admitted, int challengers, int certificates,
                int shadowApplications, int freshSettled, int controlledInfluenceEligible,
                String status) {
            this.scanned = Math.max(0, scanned);
            this.eligible = Math.max(0, eligible);
            this.admitted = Math.max(0, admitted);
            this.challengers = Math.max(0, challengers);
            this.certificates = Math.max(0, certificates);
            this.shadowApplications = Math.max(0, shadowApplications);
            this.freshSettled = Math.max(0, freshSettled);
            this.controlledInfluenceEligible = Math.max(0, controlledInfluenceEligible);
            this.status = safe(status);
        }

        public String canonical() {
            return VERSION + "|scanned=" + scanned + "|eligible=" + eligible
                    + "|admitted=" + admitted + "|challengers=" + challengers
                    + "|certificates=" + certificates + "|shadowApplications=" + shadowApplications
                    + "|freshSettled=" + freshSettled
                    + "|controlledInfluenceEligible=" + controlledInfluenceEligible
                    + "|status=" + status;
        }
    }

    private static final class LegacyCandidate {
        final String id, nameZh, structuralHash, evidenceHash, trainedUntil, status;
        final int n, maxMiss;
        final double hitRate, wilson, block, regime, coverage, brier, ece, pairedDelta, fdrQ;
        final int pairedN;

        LegacyCandidate(Cursor c) {
            id = safe(c.getString(0));
            nameZh = safe(c.getString(1));
            structuralHash = hashOrEmpty(c.getString(2));
            String sourceEvidence = hashOrEmpty(c.getString(3));
            n = Math.max(0, c.getInt(4));
            hitRate = finiteOr(c.getDouble(5), 0.0d);
            wilson = finiteOr(c.getDouble(6), 0.0d);
            block = finiteOr(c.getDouble(7), 0.0d);
            regime = finiteOr(c.getDouble(8), 0.0d);
            coverage = finiteOr(c.getDouble(9), 0.0d);
            maxMiss = Math.max(0, c.getInt(10));
            brier = finiteOr(c.getDouble(11), 1.0d);
            ece = finiteOr(c.getDouble(12), 1.0d);
            pairedN = Math.max(0, c.getInt(13));
            pairedDelta = finiteOr(c.getDouble(14), 0.0d);
            fdrQ = finiteOr(c.getDouble(15), 1.0d);
            trainedUntil = safe(c.getString(16));
            status = safe(c.getString(17));
            evidenceHash = sourceEvidence.length() == 64 ? sourceEvidence : V51ChecksumEngine.sha256(
                    VERSION + "|LEGACY_EVIDENCE|" + id + '|' + structuralHash + '|' + n + '|'
                            + hitRate + '|' + wilson + '|' + block + '|' + regime + '|'
                            + coverage + '|' + maxMiss + '|' + brier + '|' + ece + '|'
                            + pairedN + '|' + pairedDelta + '|' + fdrQ + '|' + trainedUntil);
        }

        boolean eligibleFor(String target) {
            return id.length() > 0 && structuralHash.length() == 64
                    && n >= LEGACY_MIN_N
                    && hitRate >= LEGACY_MIN_HIT_RATE
                    && wilson >= LEGACY_MIN_WILSON
                    && block >= LEGACY_MIN_BLOCK
                    && regime >= LEGACY_MIN_REGIME
                    && coverage >= LEGACY_MIN_COVERAGE
                    && !status.startsWith("PRUNED")
                    && !status.contains("QUARANTIN")
                    && trainedUntil.length() > 0
                    && PitFeatureDagR929.compareIssues(trainedUntil, target) < 0;
        }

        String admissionDetail(String target) {
            return "candidate=" + id + "|name=" + nameZh + "|legacyN=" + n
                    + "|legacyHR=" + fmt(hitRate) + "|legacyWilson=" + fmt(wilson)
                    + "|legacyBlock=" + fmt(block) + "|legacyRegime=" + fmt(regime)
                    + "|legacyCoverage=" + fmt(coverage) + "|legacyMaxMiss=" + maxMiss
                    + "|legacyBrier=" + fmt(brier) + "|legacyECE=" + fmt(ece)
                    + "|legacyPairedN=" + pairedN + "|legacyPairedDelta=" + fmt(pairedDelta)
                    + "|legacyFdrQ=" + fmt(fdrQ) + "|legacyStatus=" + status
                    + "|admitIssue=" + target + "|freshFutureSupport=0"
                    + "|historicalEvidenceIsAdmissionOnly=1|productionWeight=0";
        }
    }

    private static final class FreshMetrics {
        int n, hit, pairedN, pairedCandidateHit, pairedMainlineHit, available;
        int[] binN = new int[10], binHit = new int[10];
        double[] binProbability = new double[10];
        double brierSum;
        String lastIssue = "";
        boolean readSucceeded;
        HeadIdentity head;
        String predictionChain = V51ChecksumEngine.sha256("R79_CURRENT_HEAD_OBSERVATIONS");

        double hitRate() { return n <= 0 ? Double.NaN : (double) hit / n; }
        double wilson() { return wilsonLower(hit, n); }
        double coverage() { return available <= 0 ? (n <= 0 ? Double.NaN : 1.0d)
                : clamp((double) n / available, 0.0d, 1.0d); }
        // R908 score has no matching frozen calibration/selection artifact. It is not a probability.
        double brier() { return Double.NaN; }
        double ece() {
            return Double.NaN;
        }
        double pairedDelta() { return pairedN <= 0 ? Double.NaN
                : (double) (pairedCandidateHit - pairedMainlineHit) / pairedN; }
    }

    /** The original head boundary is read without rewriting any historical evidence. */
    private static final class HeadIdentity {
        final String status, epoch, structure, digest;
        final boolean eligible;
        HeadIdentity(String status, String epoch, String evidence, String structure) {
            this.status = status;
            this.epoch = epoch;
            this.structure = structure;
            this.eligible = ("CLEAN".equals(status) || "RESET_CLEAN".equals(status))
                    && (epoch.isEmpty() || TargetIssueResolver.isValid(epoch))
                    && hashOrEmpty(evidence).length() == 64 && hashOrEmpty(structure).length() == 64;
            this.digest = V51ChecksumEngine.sha256("R79_BRIDGE_HEAD|" + status + '|' + epoch
                    + '|' + evidence + '|' + structure);
        }
    }

    private static HeadIdentity headIdentity(SQLiteDatabase db, String id) {
        try (Cursor c = BudgetedQueryR927.query(db,
                "SELECT h.status,h.epoch_start_issue,h.evidence_hash,g.structural_hash FROM "
                        + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g LEFT JOIN "
                        + AlgorithmProofRecoveryR948.HEAD + " h ON h.genome_id=g.genome_id"
                        + " WHERE g.genome_id=?", new String[] { id })) {
            if (!c.moveToFirst()) return null;
            // An absent/auditing head is known ineligible, not a failed database read.
            return new HeadIdentity(c.isNull(0) ? "MISSING_HEAD" : safe(c.getString(0)),
                    safe(c.getString(1)), safe(c.getString(2)), safe(c.getString(3)));
        }
    }

    private static boolean sameHead(SQLiteDatabase db, String id, HeadIdentity expected) {
        HeadIdentity current = headIdentity(db, id);
        return expected != null && current != null && expected.digest.equals(current.digest);
    }

    /** Background/research entry. Never call this from the canonical decision transaction. */
    public static Summary sync(final DbStore store, final String targetIssue) {
        String target = safe(targetIssue);
        if (store == null || !TargetIssueResolver.isValid(target))
            return new Summary(0, 0, 0, 0, 0, 0, 0, 0, "INPUT_INVALID");
        if (MAY_AUTHORIZE_REAL || CertificateBundleServiceR929.MAY_CREATE_REAL)
            throw new IllegalStateException("R930_CHALLENGER_BRIDGE_AUTHORITY_VIOLATION");
        if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive())
            return snapshotSummary(store, "YIELD_P0_PRESSURE");

        final ArrayList<LegacyCandidate> candidates = loadLegacy(store, target);
        int eligible = 0;
        for (LegacyCandidate candidate : candidates) if (candidate.eligibleFor(target)) eligible++;
        final int eligibleFinal = eligible;
        Integer admitted = store.coordinatorR36().runAtomicWrite(
                "R930_L4_LEGACY_CHALLENGER_ADMISSION",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        Batch34SchemaR930.ensure(db);
                        int added = 0;
                        for (LegacyCandidate candidate : candidates) {
                            ResearchWorkBudgetR927.checkpoint();
                            if (!candidate.eligibleFor(targetIssue)) continue;
                            if (admitOne(db, candidate, targetIssue)) added++;
                        }
                        return Integer.valueOf(added);
                    }
                }, Integer.valueOf(-1));

        if (admitted == null || admitted.intValue() < 0)
            throw new IllegalStateException("R931_ADMISSION_TRANSACTION_NOT_COMMITTED");
        mirrorPredrawApplications(store, target);
        syncFreshCertificates(store, target);
        Summary summary = querySummary(store, candidates.size(), eligibleFinal,
                admitted == null ? 0 : admitted.intValue(), "SYNC_COMPLETE");
        persistSummary(store, summary);
        return summary;
    }

    private static ArrayList<LegacyCandidate> loadLegacy(DbStore store, String target) {
        ArrayList<LegacyCandidate> out = new ArrayList<LegacyCandidate>();
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),
                    "SELECT genome_id,display_name_zh,structural_hash,future_evidence_hash,"
                            + "future_n,future_hit_rate,future_wilson_lower,future_block_min,"
                            + "future_regime_min,future_coverage,future_max_miss,future_brier,"
                            + "future_ece,paired_mainline_n,paired_mainline_delta,future_fdr_q,"
                            + "trained_until_issue,status FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                            + " WHERE may_authorize_real=0 AND trained_until_issue<?"
                            + " AND future_n>=" + LEGACY_MIN_N + " AND future_hit_rate>=" + LEGACY_MIN_HIT_RATE
                            + " AND future_wilson_lower>=" + LEGACY_MIN_WILSON + " AND future_block_min>=" + LEGACY_MIN_BLOCK
                            + " AND future_regime_min>=" + LEGACY_MIN_REGIME + " AND future_coverage>=" + LEGACY_MIN_COVERAGE
                            + " AND status NOT LIKE 'PRUNED%' AND status NOT LIKE '%QUARANTIN%'"
                            + " AND length(structural_hash)=64 AND trained_until_issue<>''"
                            + " AND NOT EXISTS (SELECT 1 FROM " + Batch34SchemaR930.TABLE_LIFECYCLE
                            + " l WHERE l.algorithm_id=genome_id)"
                            + " ORDER BY future_n DESC,future_wilson_lower DESC,future_hit_rate DESC,updated_at DESC"
                            + " LIMIT ?",
                    new String[] { target, String.valueOf(MAX_ADMISSIONS_PER_TICK) });
            while (c.moveToNext()) { ResearchWorkBudgetR927.checkpoint(); out.add(new LegacyCandidate(c)); }
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) {
            out.clear();
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private static boolean admitOne(SQLiteDatabase db, LegacyCandidate candidate, String target) {
        long now = System.currentTimeMillis();
        String detail = candidate.admissionDetail(target);
        String admissionHash = V51ChecksumEngine.sha256(VERSION + "|ADMISSION|" + detail);
        String evidenceId = V51ChecksumEngine.sha256("R930_EVIDENCE|" + EVIDENCE_ADMISSION + '|'
                + candidate.id + '|' + target);
        boolean existed = exists(db, Batch34SchemaR930.TABLE_LIFECYCLE, "algorithm_id", candidate.id);
        // Admission is immutable. Re-admission would reset the Future anchor and overwrite evidence.
        if (existed) return false;
        if (!existed) {
            ContentValues lifecycle = new ContentValues();
            lifecycle.put("algorithm_id", candidate.id);
            lifecycle.put("parent_algorithm_id", "");
            lifecycle.put("genome_sha256", candidate.structuralHash);
            lifecycle.put("repair_target", "LEGACY_STABLE_ADMISSION_TO_NEW_FUTURE");
            lifecycle.put("falsification_rule", "FAIL_IF_NEW_FUTURE_DOES_NOT_RETAIN_QUALITY");
            lifecycle.put("state", "CHALLENGER");
            lifecycle.put("future_support", 0);
            lifecycle.put("last_evidence_sha256", admissionHash);
            lifecycle.put("first_prune_reason", "");
            lifecycle.put("first_prune_metrics", "");
            lifecycle.put("first_prune_issue", "");
            lifecycle.put("created_at", now);
            lifecycle.put("updated_at", now);
            long row = db.insertWithOnConflict(Batch34SchemaR930.TABLE_LIFECYCLE, null,
                    lifecycle, SQLiteDatabase.CONFLICT_IGNORE);
            if (row != -1L) {
                ContentValues event = new ContentValues();
                event.put("event_id", V51ChecksumEngine.sha256("R930_LIFECYCLE_EVENT|" + candidate.id + "|ADMIT|" + target));
                event.put("algorithm_id", candidate.id);
                event.put("from_state", "LEGACY_R908");
                event.put("to_state", "CHALLENGER");
                event.put("added_future_support", 0);
                event.put("evidence_sha256", admissionHash);
                event.put("reason", "L4_ZERO_WEIGHT_ADMISSION_HISTORICAL_NOT_FUTURE_PROOF");
                event.put("created_at", now);
                db.insertWithOnConflict(Batch34SchemaR930.TABLE_LIFECYCLE_EVENT, null,
                        event, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }

        insertEvidence(db, evidenceId, target, EVIDENCE_ADMISSION, admissionHash, detail, now);
        CertificateBundleServiceR929.Certificate certificate = challengeCertificate(candidate, target, admissionHash);
        ContentValues module = new ContentValues();
        module.put("module_id", moduleId(candidate.id));
        module.put("module_state", "L4_CHALLENGER_ZERO_WEIGHT");
        module.put("reason", detail + "|certificateLevel=" + certificate.level.name());
        module.put("implementation_sha256", V51ChecksumEngine.sha256(VERSION));
        module.put("contract_sha256", Batch34SchemaR930.CONTRACT_HASH);
        module.put("certificate_sha256", certificate.certificateHash);
        module.put("updated_at", now);
        db.insertWithOnConflict(Batch34SchemaR930.TABLE_MODULE_STATE, null, module,
                SQLiteDatabase.CONFLICT_REPLACE);
        return !existed;
    }

    private static CertificateBundleServiceR929.Certificate challengeCertificate(
            LegacyCandidate candidate, String target, String evidenceHash) {
        CertificateBundleServiceR929.CandidateKey key = new CertificateBundleServiceR929.CandidateKey(
                CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,
                "R908_LEGACY_BRIDGE", "LEGACY_STABLE", candidate.id, candidate.structuralHash);
        ArrayList<CertificateBundleServiceR929.GateObservation> gates =
                new ArrayList<CertificateBundleServiceR929.GateObservation>();
        gates.add(gate("LEGACY_N", candidate.n, LEGACY_MIN_N, candidate.n >= LEGACY_MIN_N, evidenceHash));
        gates.add(gate("LEGACY_HIT_RATE", candidate.hitRate, LEGACY_MIN_HIT_RATE,
                candidate.hitRate >= LEGACY_MIN_HIT_RATE, evidenceHash));
        gates.add(gate("LEGACY_WILSON", candidate.wilson, LEGACY_MIN_WILSON,
                candidate.wilson >= LEGACY_MIN_WILSON, evidenceHash));
        gates.add(gate("LEGACY_BLOCK", candidate.block, LEGACY_MIN_BLOCK,
                candidate.block >= LEGACY_MIN_BLOCK, evidenceHash));
        gates.add(gate("LEGACY_REGIME", candidate.regime, LEGACY_MIN_REGIME,
                candidate.regime >= LEGACY_MIN_REGIME, evidenceHash));
        gates.add(gate("LEGACY_COVERAGE", candidate.coverage, LEGACY_MIN_COVERAGE,
                candidate.coverage >= LEGACY_MIN_COVERAGE, evidenceHash));
        return certificate(key, CertificateBundleServiceR929.Level.L4_CHALLENGER, target,
                evidenceHash, candidate.structuralHash, gates, 0.0d,
                "L4_ZERO_WEIGHT_HISTORICAL_ADMISSION_ONLY_NEW_FUTURE_REQUIRED");
    }

    private static CertificateBundleServiceR929.GateObservation gate(
            String id, double observed, double required, boolean pass, String evidenceHash) {
        return new CertificateBundleServiceR929.GateObservation(
                id, finiteOr(observed, 0.0d), required, pass, VERSION, evidenceHash,
                "LEGACY_ADMISSION_ONLY");
    }

    private static CertificateBundleServiceR929.Certificate certificate(
            CertificateBundleServiceR929.CandidateKey key,
            CertificateBundleServiceR929.Level level,
            String target,
            String evidenceHash,
            String structuralHash,
            List<CertificateBundleServiceR929.GateObservation> gates,
            double weight,
            String reason) {
        return new CertificateBundleServiceR929.Certificate(
                key, level, CertificateBundleServiceR929.CertificateState.ACTIVE,
                V51ChecksumEngine.sha256("R930_BUNDLE|" + key.keyHash + '|' + evidenceHash),
                Batch34SchemaR930.CONTRACT_HASH,
                V51ChecksumEngine.sha256("R930_ATOM_GRAPH|" + key.keyHash),
                structuralHash,
                V51ChecksumEngine.sha256("R930_POLICY|" + key.keyHash),
                V51ChecksumEngine.sha256("R930_CALIBRATION|" + key.keyHash + '|' + evidenceHash),
                V51ChecksumEngine.sha256("R930_DOMAIN|" + key.keyHash),
                V51ChecksumEngine.sha256("R930_TRIAL|" + key.keyHash),
                V51ChecksumEngine.sha256("R930_SELECTION|" + key.keyHash),
                structuralHash,
                target, "", System.currentTimeMillis(), weight, gates, reason);
    }

    /** Mirrors already-computed R908 pre-draw selections; it does not rescore the 1200 surface. */
    private static void mirrorPredrawApplications(final DbStore store, final String target) {
        store.coordinatorR36().runAtomicWrite("R930_MIRROR_L4_SHADOW_APPLICATIONS",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        Batch34SchemaR930.ensure(db);
                        Cursor c = null;
                        int inserted = 0;
                        try {
                            c = BudgetedQueryR927.query(db,
                                    "SELECT p.genome_id,p.position,p.n1,p.n2,p.n3,p.selected_ordinal,p.score,"
                                            + "p.feature_hash,p.record_hash FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p"
                                            + " JOIN " + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id) AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash"
                                            + " JOIN " + Batch34SchemaR930.TABLE_LIFECYCLE + " l ON l.algorithm_id=p.genome_id"
                                            + " WHERE p.target_issue=? AND NOT EXISTS (SELECT 1 FROM " + DbHelper.TABLE_DRAWS
                                            + " d WHERE d.issue=p.target_issue) AND ("
                                            + AlgorithmProofRecoveryR948.eligible("p.genome_id", "p.target_issue", "p.record_hash")
                                            + ") AND l.state IN ('CHALLENGER','SHADOW_L5','L5','PROMOTED_L6','MATURE_L7')",
                                    new String[] { target });
                            while (c.moveToNext()) {
                                ResearchWorkBudgetR927.checkpoint();
                                String id = safe(c.getString(0));
                                if (!"VALID".equals(CandidateExecutionAuditR932.verifyAlgorithm(db, id, target))) continue;
                                String detail = "candidate=" + id + "|position=" + c.getInt(1)
                                        + "|k3=" + c.getInt(2) + ',' + c.getInt(3) + ',' + c.getInt(4)
                                        + "|ordinal=" + c.getInt(5) + "|score=" + fmt(c.getDouble(6))
                                        + "|featureHash=" + safe(c.getString(7)) + "|sourceRecord=" + safe(c.getString(8))
                                        + "|lane=SHADOW|productionWeight=0_OBSERVATION_ONLY";
                                String payload = V51ChecksumEngine.sha256(VERSION + "|SHADOW|" + target + '|' + detail);
                                String eid = V51ChecksumEngine.sha256("R930_EVIDENCE|" + EVIDENCE_SHADOW + '|' + id + '|' + target);
                                if (insertEvidence(db, eid, target, EVIDENCE_SHADOW, payload, detail,
                                        System.currentTimeMillis())) inserted++;
                            }
                        } finally { if (c != null) c.close(); }
                        return Integer.valueOf(inserted);
                    }
                }, Integer.valueOf(0));
    }

    /** Re-evaluates only labels strictly after the admission issue; historical N is never copied. */
    private static void syncFreshCertificates(final DbStore store, final String currentTarget) {
        final ArrayList<String> ids = admittedIds(store);
        for (final String id : ids) {
            ResearchWorkBudgetR927.checkpoint();
            if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive()) break;
            final String admissionIssue = admissionIssue(store, id);
            if (admissionIssue.length() == 0) continue;
            final FreshMetrics fresh = freshMetrics(store, id, admissionIssue, currentTarget);
            if (!fresh.readSucceeded) continue;
            final LegacyCandidate legacy = loadLegacyById(store, id);
            if (legacy == null || !legacy.structuralHash.equals(fresh.head.structure)) continue;
            final String freshHash = V51ChecksumEngine.sha256(VERSION + "|FRESH|" + id + '|'
                    + fresh.head.digest + '|' + fresh.predictionChain + '|' + admissionIssue + '|' + fresh.n + '|' + fresh.hit + '|' + fresh.pairedN + '|'
                    + fmt(fresh.hitRate()) + '|' + fmt(fresh.wilson()) + '|'
                    + fmt(fresh.coverage()) + '|' + fmt(fresh.brier()) + '|'
                    + fmt(fresh.ece()) + '|' + fmt(fresh.pairedDelta()) + '|' + fresh.lastIssue);
            final ArrayList<CertificateBundleServiceR929.GateObservation> pendingGates =
                    new ArrayList<CertificateBundleServiceR929.GateObservation>();
            pendingGates.add(gate("FRESH_OBSERVATION_SUPPORT", fresh.n, 120, fresh.n >= 120, freshHash));
            pendingGates.add(gate("CALIBRATED_PROBABILITY_SUPPORT", 0, 120, false, freshHash));
            pendingGates.add(gate("FROZEN_PAIRED_TRIAL_SUPPORT", 0, 120, false, freshHash));
            final String pendingReason = fresh.head.eligible
                    ? "WAIT_FOR_CALIBRATION_AND_PREREGISTERED_PAIRED_TRIAL"
                    : "HEAD_INELIGIBLE_WAIT_INTEGRITY:" + fresh.head.status;
            // A rolling legacy/mainline join is not a fixed paired trial. Observation cannot promote.
            final CertificateBundleServiceR929.Level effectiveLevel =
                    CertificateBundleServiceR929.Level.L4_CHALLENGER;
            final double requestedWeight = 0.0d;
            final CertificateBundleServiceR929.CandidateKey key = new CertificateBundleServiceR929.CandidateKey(
                    CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,
                    "R908_LEGACY_BRIDGE", "LEGACY_STABLE", id, legacy.structuralHash);
            final CertificateBundleServiceR929.Certificate cert = certificate(
                    key, effectiveLevel, fresh.lastIssue.length() == 0 ? currentTarget : fresh.lastIssue,
                    freshHash, legacy.structuralHash, pendingGates, requestedWeight,
                    "OBSERVATION_ONLY|CALIBRATION_AND_FROZEN_BASELINE_MISSING|"
                            + pendingReason + "|historicalNNotCopied=1");
            store.coordinatorR36().runAtomicWrite("R930_FRESH_CERTIFICATE_SYNC",
                    new DatabaseCoordinator.WriteCallable<Integer>() {
                        @Override public Integer call(SQLiteDatabase db) {
                            Batch34SchemaR930.ensure(db);
                            // A reset between the read and this writer must not republish the old head's support.
                            if (!sameHead(db, id, fresh.head)) return Integer.valueOf(0);
                            long now = System.currentTimeMillis();
                            ContentValues update = new ContentValues();
                            update.put("state", lifecycleState(effectiveLevel));
                            update.put("future_support", fresh.n);
                            update.put("last_evidence_sha256", freshHash);
                            update.put("updated_at", now);
                            db.update(Batch34SchemaR930.TABLE_LIFECYCLE, update, "algorithm_id=?",
                                    new String[] { id });
                            String detail = "candidate=" + id + "|freshN=" + fresh.n
                                    + "|freshHR=" + fmt(fresh.hitRate()) + "|freshWilson=" + fmt(fresh.wilson())
                                    + "|freshCoverage=" + fmt(fresh.coverage()) + "|freshBrier=" + fmt(fresh.brier())
                                    + "|freshECE=" + fmt(fresh.ece()) + "|pairedN=" + fresh.pairedN
                                    + "|pairedDelta=" + fmt(fresh.pairedDelta()) + "|level=" + effectiveLevel.name()
                                    + "|authorizedWeight=" + fmt(cert.authorizedWeight(currentTarget))
                                    + "|evaluation=" + pendingReason + "|headEpoch=" + fresh.head.epoch
                                    + "|headIdentity=" + fresh.head.digest
                                    + "|headEligible=" + (fresh.head.eligible ? 1 : 0);
                            insertEvidence(db, V51ChecksumEngine.sha256("R930_EVIDENCE|" + EVIDENCE_FRESH + '|'
                                            + id + '|' + fresh.n + '|' + freshHash),
                                    fresh.lastIssue.length() == 0 ? currentTarget : fresh.lastIssue,
                                    EVIDENCE_FRESH, freshHash, detail, now);
                            ContentValues module = new ContentValues();
                            module.put("module_id", moduleId(id));
                            module.put("module_state", effectiveLevel.name());
                            module.put("reason", detail);
                            module.put("implementation_sha256", V51ChecksumEngine.sha256(VERSION));
                            module.put("contract_sha256", Batch34SchemaR930.CONTRACT_HASH);
                            module.put("certificate_sha256", cert.certificateHash);
                            module.put("updated_at", now);
                            db.insertWithOnConflict(Batch34SchemaR930.TABLE_MODULE_STATE, null, module,
                                    SQLiteDatabase.CONFLICT_REPLACE);
                            return Integer.valueOf(1);
                        }
                    }, Integer.valueOf(0));
        }
    }

    private static FreshMetrics freshMetrics(DbStore store, String id, String admissionIssue, String currentTarget) {
        FreshMetrics out = new FreshMetrics();
        try {
            SQLiteDatabase db = store.readable();
            out.head = headIdentity(db, id);
            if (out.head == null) return out;
            if (!out.head.eligible) {
                out.readSucceeded = sameHead(db, id, out.head);
                return out;
            }
            try (Cursor c = BudgetedQueryR927.query(db,
                    "SELECT p.target_issue,p.hit_label,p.score,o.hit_label,p.record_hash FROM "
                            + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN " + CandidateExecutionAuditR932.TABLE
                            + " e ON e.candidate_key=('algorithm:'||p.genome_id) AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash LEFT JOIN "
                            + DbHelper.TABLE_R35_OOS_LABEL + " o ON o.target_issue=p.target_issue AND o.strict_oos=1"
                            + " WHERE p.genome_id=? AND p.target_issue>? AND p.target_issue<?"
                            + " AND p.hit_label IN (0,1) AND ("
                            + AlgorithmProofRecoveryR948.eligible("p.genome_id", "p.target_issue", "p.record_hash")
                            + ") ORDER BY p.target_issue ASC LIMIT 1200",
                    new String[] { id, admissionIssue, currentTarget })) {
                while (c.moveToNext()) {
                    ResearchWorkBudgetR927.checkpoint();
                    String issue = c.getString(0);
                    if (!"VALID".equals(CandidateExecutionAuditR932.verifyAlgorithm(db, id, issue))) continue;
                    int hit = c.getInt(1);
                    out.n++;
                    out.hit += hit;
                    out.lastIssue = safe(issue);
                    out.predictionChain = V51ChecksumEngine.sha256(out.predictionChain + '|' + issue
                            + '|' + c.getString(4) + '|' + hit);
                    if (!c.isNull(3)) {
                        out.pairedN++;
                        out.pairedCandidateHit += hit;
                        out.pairedMainlineHit += c.getInt(3);
                    }
                }
            }
            try (Cursor available = BudgetedQueryR927.query(db,
                    "SELECT COUNT(*) FROM " + DbHelper.TABLE_DRAWS + " WHERE issue>? AND issue<?"
                            + " AND (?='' OR issue>=?)",
                    new String[] { admissionIssue, currentTarget, out.head.epoch, out.head.epoch })) {
                if (!available.moveToFirst()) return new FreshMetrics();
                out.available = Math.max(out.n, available.getInt(0));
            }
            // A successful empty scan clears stale projected support; read errors never impersonate zero.
            out.readSucceeded = sameHead(db, id, out.head);
            return out;
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
        catch (Throwable unavailable) { return new FreshMetrics(); }
    }

    private static ArrayList<String> admittedIds(DbStore store) {
        ArrayList<String> out = new ArrayList<String>(); Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),"SELECT algorithm_id FROM " + Batch34SchemaR930.TABLE_LIFECYCLE
                    + " WHERE repair_target='LEGACY_STABLE_ADMISSION_TO_NEW_FUTURE'"
                    + " AND state NOT IN ('PRUNED','QUARANTINED','ARCHIVED') ORDER BY updated_at ASC LIMIT 4", null);
            while (c.moveToNext()) out.add(safe(c.getString(0)));
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) { out.clear(); }
        finally { if (c != null) c.close(); }
        return out;
    }

    private static String admissionIssue(DbStore store, String id) {
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),"SELECT target_issue FROM " + Batch34SchemaR930.TABLE_EVIDENCE_LINK
                    + " WHERE evidence_type=? AND detail LIKE ? ORDER BY created_at ASC LIMIT 1",
                    new String[] { EVIDENCE_ADMISSION, "candidate=" + id + "|%" });
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) { return ""; }
        finally { if (c != null) c.close(); }
    }

    private static LegacyCandidate loadLegacyById(DbStore store, String id) {
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),
                    "SELECT genome_id,display_name_zh,structural_hash,future_evidence_hash,"
                            + "future_n,future_hit_rate,future_wilson_lower,future_block_min,"
                            + "future_regime_min,future_coverage,future_max_miss,future_brier,"
                            + "future_ece,paired_mainline_n,paired_mainline_delta,future_fdr_q,"
                            + "trained_until_issue,status FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                            + " WHERE genome_id=? LIMIT 1", new String[] { id });
            return c.moveToFirst() ? new LegacyCandidate(c) : null;
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) { return null; }
        finally { if (c != null) c.close(); }
    }

    private static Summary querySummary(DbStore store, int scanned, int eligible, int admitted, String status) {
        int challengers = count(store, Batch34SchemaR930.TABLE_LIFECYCLE,
                "repair_target='LEGACY_STABLE_ADMISSION_TO_NEW_FUTURE' AND state NOT IN ('PRUNED','QUARANTINED','ARCHIVED')", null);
        int certificates = count(store, Batch34SchemaR930.TABLE_MODULE_STATE,
                "module_id LIKE 'R930_L4_%' AND certificate_sha256<>''", null);
        int shadow = count(store, Batch34SchemaR930.TABLE_EVIDENCE_LINK,
                "evidence_type=?", new String[] { EVIDENCE_SHADOW });
        int fresh = 0, controlled = 0; Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),"SELECT COALESCE(SUM(future_support),0),"
                    + "SUM(CASE WHEN state IN ('PROMOTED_L6','MATURE_L7') THEN 1 ELSE 0 END) FROM "
                    + Batch34SchemaR930.TABLE_LIFECYCLE
                    + " WHERE repair_target='LEGACY_STABLE_ADMISSION_TO_NEW_FUTURE'", null);
            if (c.moveToFirst()) { fresh = Math.max(0, c.getInt(0)); controlled = Math.max(0, c.getInt(1)); }
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) {} finally { if (c != null) c.close(); }
        return new Summary(scanned, eligible, admitted, challengers, certificates, shadow, fresh, controlled, status);
    }

    private static Summary snapshotSummary(DbStore store, String status) {
        return querySummary(store, 0, 0, 0, status);
    }

    private static void persistSummary(DbStore store, Summary summary) {
        try {
            LinkedHashMap<String,String> state = new LinkedHashMap<String,String>();
            state.put("r930_batch34_challenger_count", String.valueOf(summary.challengers));
            state.put("r930_batch34_certificate_count", String.valueOf(summary.certificates));
            state.put("r930_batch34_mainline_shadow_application_count", String.valueOf(summary.shadowApplications));
            state.put("r930_batch34_fresh_future_support", String.valueOf(summary.freshSettled));
            state.put("r930_batch34_controlled_production_influence_count", String.valueOf(summary.controlledInfluenceEligible));
            state.put("r930_batch34_challenger_bridge_status", summary.canonical());
            state.put("r930_batch34_challenger_bridge_at", String.valueOf(System.currentTimeMillis()));
            store.setStates(state);
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) {}
    }

    public static String report(DbStore store) {
        Summary summary = snapshotSummary(store, "REPORT");
        return ResearchPipelineStatusR941.capture(store) + "\n\n【旧算法观察桥接（独立历史口径）】\n" + summary.canonical()
                + "\n最近实际扫描：" + store.getState("r930_batch34_challenger_bridge_status", "NOT_RUN")
                + "\n口径：历史长期证据只负责进入L4挑战者，new Future支持从0开始；"
                + "本桥接仅记录旧算法赛前观察，最多读取最早1200个新样本；不是主链重排。"
                + "旧score不是校准概率，Brier/ECE为N/A；缺固定配对基线和校准包时保持L4零权重。"
                + "任何证书都无REAL授权权力。";
    }

    private static boolean insertEvidence(SQLiteDatabase db, String evidenceId, String target,
            String type, String payloadHash, String detail, long now) {
        ContentValues value = new ContentValues();
        value.put("evidence_id", evidenceId);
        value.put("game_code", ReleaseManifestR926.GAME_CODE);
        value.put("target_issue", target);
        value.put("evidence_type", type);
        value.put("owner", VERSION);
        value.put("payload_hash", payloadHash);
        value.put("detail", detail);
        value.put("created_at", now);
        return db.insertWithOnConflict(Batch34SchemaR930.TABLE_EVIDENCE_LINK, null, value,
                SQLiteDatabase.CONFLICT_IGNORE) != -1L;
    }

    private static boolean exists(SQLiteDatabase db, String table, String column, String value) {
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(db,"SELECT 1 FROM " + table + " WHERE " + column + "=? LIMIT 1",
                    new String[] { value });
            return c.moveToFirst();
        } finally { if (c != null) c.close(); }
    }

    private static int count(DbStore store, String table, String where, String[] args) {
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),"SELECT COUNT(*) FROM " + table + " WHERE " + where, args);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; } catch (Throwable ignored) { return 0; }
        finally { if (c != null) c.close(); }
    }

    private static String lifecycleState(CertificateBundleServiceR929.Level level) {
        if (level == CertificateBundleServiceR929.Level.L7_CHAMPION) return "MATURE_L7";
        if (level == CertificateBundleServiceR929.Level.L6_CONTROLLED_INFLUENCE) return "PROMOTED_L6";
        if (level == CertificateBundleServiceR929.Level.L5_SHADOW) return "SHADOW_L5";
        return "CHALLENGER";
    }

    private static String moduleId(String id) { return "R930_L4_" + safe(id); }
    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return Double.NaN;
        double z = 1.959963984540054d;
        double p = (double) hit / n;
        double z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp((center - margin) / (1.0d + z2 / n), 0.0d, 1.0d);
    }
    private static String hashOrEmpty(String value) {
        String v = safe(value).toLowerCase(Locale.US);
        return v.matches("[0-9a-f]{64}") ? v : "";
    }
    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').replace('\n', ' ').replace('\r', ' ').trim();
    }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    private static double finiteOr(double value, double fallback) { return finite(value) ? value : fallback; }
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, finiteOr(value, min)));
    }
    private static String fmt(double value) {
        return finite(value) ? String.format(Locale.US, "%.6f", value) : "N/A";
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        Summary summary = snapshotSummarySnapshotCP4(report, store, "REPORT");
        return ResearchPipelineStatusR941.captureSnapshotCP4(report, store) + "\n\n【旧算法观察桥接（独立历史口径）】\n" + summary.canonical()
                + "\n最近实际扫描：" + store.getState("r930_batch34_challenger_bridge_status", "NOT_RUN")
                + "\n口径：历史长期证据只负责进入L4挑战者，new Future支持从0开始；"
                + "本桥接仅记录旧算法赛前观察，最多读取最早1200个新样本；不是主链重排。"
                + "旧score不是校准概率，Brier/ECE为N/A；缺固定配对基线和校准包时保持L4零权重。"
                + "任何证书都无REAL授权权力。";
    }

public static Summary snapshotSummarySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String status) {
        return querySummarySnapshotCP4(report, store, 0, 0, 0, status);
    }

public static Summary querySummarySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, int scanned, int eligible, int admitted, String status) {
        int challengers = countSnapshotCP4(report, store, Batch34SchemaR930.TABLE_LIFECYCLE,
                "repair_target='LEGACY_STABLE_ADMISSION_TO_NEW_FUTURE' AND state NOT IN ('PRUNED','QUARANTINED','ARCHIVED')", null);
        int certificates = countSnapshotCP4(report, store, Batch34SchemaR930.TABLE_MODULE_STATE,
                "module_id LIKE 'R930_L4_%' AND certificate_sha256<>''", null);
        int shadow = countSnapshotCP4(report, store, Batch34SchemaR930.TABLE_EVIDENCE_LINK,
                "evidence_type=?", new String[] { EVIDENCE_SHADOW });
        int fresh = 0, controlled = 0; Cursor c = null;
        try {
            c = BudgetedQueryR927.querySnapshotCP4(report, store.readable(),"SELECT COALESCE(SUM(future_support),0),"
                    + "SUM(CASE WHEN state IN ('PROMOTED_L6','MATURE_L7') THEN 1 ELSE 0 END) FROM "
                    + Batch34SchemaR930.TABLE_LIFECYCLE
                    + " WHERE repair_target='LEGACY_STABLE_ADMISSION_TO_NEW_FUTURE'", null);
            if (c.moveToFirst()) { fresh = Math.max(0, c.getInt(0)); controlled = Math.max(0, c.getInt(1)); }
        } catch (ResearchWorkBudgetR927.Yield yielded) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",yielded);} catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);} finally { if (c != null) c.close(); }
        return new Summary(scanned, eligible, admitted, challengers, certificates, shadow, fresh, controlled, status);
    }

public static int countSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String table, String where, String[] args) {
        Cursor c = null;
        try {
            c = BudgetedQueryR927.querySnapshotCP4(report, store.readable(),"SELECT COUNT(*) FROM " + table + " WHERE " + where, args);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (ResearchWorkBudgetR927.Yield yielded) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",yielded);} catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);}
        finally { if (c != null) c.close(); }
    }
}
