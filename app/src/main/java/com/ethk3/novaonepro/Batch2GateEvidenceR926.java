package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * B11 final provenance audit at the existing V51Repository canonical transaction boundary.
 * This class never selects an action and never creates a Trade/BET. It fail-closes only when the
 * already-authorized BET lacks immutable source evidence from the original gate transaction.
 */
public final class Batch2GateEvidenceR926 {
    private Batch2GateEvidenceR926() {}

    public static Batch2ContractCoreR926.GateCheck validateAndAppendWithinTransaction(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request request,
            DeferredEvidenceSnapshotR36.Header snapshot,
            String snapshotHash,
            String canonicalHash) {
        return validateAndAppendWithinTransaction(db,null,request,snapshot,snapshotHash,canonicalHash,null);
    }

    /** Receives the same compiled proof already checked by the sole REAL writer. */
    public static Batch2ContractCoreR926.GateCheck validateAndAppendWithinTransaction(
            SQLiteDatabase db, DbStore store, AnalysisSingleFlightR36.Request request,
            DeferredEvidenceSnapshotR36.Header snapshot, String snapshotHash,
            String canonicalHash, V51CompiledActionContract compiled) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("B11_CANONICAL_TRANSACTION_REQUIRED");
        if (request == null || !request.valid() || snapshot == null)
            throw new IllegalArgumentException("B11_PROVENANCE_INPUT_INVALID");

        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT gate_eligible,governance_status,governance_applied,"
                            + "controlled_influence_eligible,authorized_action,authorized_reject,"
                            + "position,n1,n2,n3,record_hash,gate_code FROM "
                            + ControlledCandidateOpportunityAuditR922.TABLE
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {request.targetIssue});
            if (!c.moveToFirst())
                throw new IllegalStateException("B11_CONTROLLED_OPPORTUNITY_MISSING");

            boolean gateEligible = c.getInt(0) == 1;
            String governanceStatus = safe(c.getString(1));
            int governanceApplied = c.getInt(2);
            boolean controlledEligible = c.getInt(3) == 1;
            String action = safe(c.getString(4));
            if ("BET".equals(action) && !Batch2ContractCoreR926.isSha256(RuntimeResourceAuditR927.frozenBudgetHash()))
                throw new IllegalStateException("B11_RUNTIME_BUDGET_EVIDENCE_MISSING");
            String reject = safe(c.getString(5));
            int position = c.getInt(6);
            int n1 = c.getInt(7), n2 = c.getInt(8), n3 = c.getInt(9);
            String opportunityHash = safe(c.getString(10));
            String gateCode = safe(c.getString(11));

            EvidencePack evidence = readEvidencePack(
                    db, store, request, snapshot, snapshotHash, governanceStatus,compiled);
            Batch2ContractCoreR926.GateInput input =
                    new Batch2ContractCoreR926.GateInput(
                            action,
                            gateEligible,
                            controlledEligible,
                            governanceApplied,
                            position,
                            n1,
                            n2,
                            n3,
                            opportunityHash,
                            canonicalHash,
                            snapshotHash,
                            snapshot.historyHash,
                            snapshot.actionSpaceHash,
                            snapshot.bundleHash,
                            snapshot.contractHash,
                            snapshot.recordHash,
                            evidence.dataHash,
                            evidence.featureHash,
                            evidence.modelHash,
                            evidence.stateHash,
                            evidence.selectionHash,
                            evidence.calibrationHash,
                            evidence.certificateHash,
                            evidence.budgetHash,
                            evidence.actualConfig);
            Batch2ContractCoreR926.GateCheck check = Batch2ContractCoreR926.validateGate(input);
            if (!check.valid && check.isBet)
                throw new IllegalStateException("B11_GATE_PROVENANCE_REJECT:" + check.code);
            if (!check.valid)
                throw new IllegalStateException("B11_GATE_AUDIT_INVALID:" + check.code);

            String payload =
                    "action=" + action
                            + "|gate_eligible=" + (gateEligible ? 1 : 0)
                            + "|gate_code=" + gateCode
                            + "|reject=" + reject
                            + "|position=" + position
                            + "|k3=" + n1 + ',' + n2 + ',' + n3
                            + "|opportunity_hash=" + opportunityHash
                            + "|canonical_hash=" + safe(canonicalHash)
                            + "|snapshot_hash=" + safe(snapshotHash)
                            + "|history_hash=" + safe(snapshot.historyHash)
                            + "|action_space_hash=" + safe(snapshot.actionSpaceHash)
                            + "|bundle_hash=" + safe(snapshot.bundleHash)
                            + "|contract_hash=" + safe(snapshot.contractHash)
                            + "|snapshot_record_hash=" + safe(snapshot.recordHash)
                            + "|source_event_hash=" + evidence.sourceEventHash
                            + "|source_event_mode=" + evidence.sourceEventMode
                            + "|source_event_cutoff_at=" + evidence.sourceEventCutoffAt
                            + "|data_hash=" + evidence.dataHash
                            + "|feature_hash=" + evidence.featureHash
                            + "|model_hash=" + evidence.modelHash
                            + "|state_hash=" + evidence.stateHash
                            + "|selection_hash=" + evidence.selectionHash
                            + "|calibration_hash=" + evidence.calibrationHash
                            + "|certificate_hash=" + evidence.certificateHash
                            + "|budget_hash=" + evidence.budgetHash
                            + "|actual_config_hash=" + V51ChecksumEngine.sha256(evidence.actualConfig)
                            + "|certificate_mode=" + evidence.certificateMode
                            + "|governance_status=" + governanceStatus
                            + "|governance_applied=" + governanceApplied
                            + "|controlled_influence_eligible=" + (controlledEligible ? 1 : 0)
                            + "|model_proven=" + V51BuildManifest.MODEL_PROVEN
                            + "|shadow_only=" + V51BuildManifest.SHADOW_ONLY
                            + "|real_unlock=" + AppConfig.R36_REAL_BET_UNLOCK
                            + "|provenance_hash=" + check.provenanceHash;

            RuntimeEventLedgerR926.appendWithinTransaction(
                    db,
                    RuntimeEventLedgerR926.Event.of(
                            request.targetIssue,
                            String.valueOf(request.generation),
                            request.attemptId(),
                            "GATE_EVIDENCE",
                            check.code,
                            RuntimeEventLedgerR926.RANK_CANONICAL,
                            payload,
                            "Batch2GateEvidenceR926",
                            "GATE_EVIDENCE|" + request.targetIssue + '|' + opportunityHash + '|' + canonicalHash));
            return check;
        } finally {
            if (c != null) c.close();
        }
    }

    private static EvidencePack readEvidencePack(
            SQLiteDatabase db,
            DbStore store,
            AnalysisSingleFlightR36.Request request,
            DeferredEvidenceSnapshotR36.Header snapshot,
            String snapshotHash,
            String governanceStatus,V51CompiledActionContract compiled) {
        String persistedHistory = "", feature = "", state = "", expert = "", bundle = "", cohort = "";
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT history_hash,feature_hash,state_hash,expert_hash,artifact_bundle_hash,cohort_id"
                            + " FROM " + V51Repository.ISSUE + " WHERE target_issue=? LIMIT 1",
                    new String[] {request.targetIssue});
            if (c.moveToFirst()) {
                persistedHistory = safe(c.getString(0));
                feature = safe(c.getString(1));
                state = safe(c.getString(2));
                expert = safe(c.getString(3));
                bundle = safe(c.getString(4));
                cohort = safe(c.getString(5));
            }
        } finally {
            if (c != null) c.close();
        }

        SourceEvidence source = sourceEvidence(db, request);
        String dataHash = "";
        if (Batch2ContractCoreR926.isSha256(persistedHistory)
                && persistedHistory.equals(safe(snapshot.historyHash))
                && Batch2ContractCoreR926.isSha256(snapshotHash)
                && Batch2ContractCoreR926.isSha256(source.hash)) {
            dataHash = V51ChecksumEngine.sha256(
                    "E02D_DATA|" + ReleaseManifestR926.GAME_CODE + '|' + request.targetIssue + '|'
                            + request.baseIssue + '|' + persistedHistory + '|' + snapshotHash + '|'
                            + source.hash + '|' + source.cutoffAt);
        }
        String featureHash = Batch2ContractCoreR926.isSha256(feature) ? feature : "";
        String stateHash = Batch2ContractCoreR926.isSha256(state) ? state : "";
        String modelHash = Batch2ContractCoreR926.isSha256(expert)
                        && Batch2ContractCoreR926.isSha256(bundle)
                ? V51ChecksumEngine.sha256(
                        "E02D_MODEL|" + expert + '|' + bundle + '|' + cohort + '|'
                                + V51BuildManifest.MODEL_PROVEN + '|' + V51BuildManifest.SHADOW_ONLY)
                : "";

        String selectionHash = "";
        double selectedHitP = Double.NaN;
        String decisionId = "";
        c = null;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT record_hash,selected_hit_p,decision_id FROM " + V51Repository.SELECTED
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {request.targetIssue});
            if (c.moveToFirst()) {
                String record = safe(c.getString(0));
                if (Batch2ContractCoreR926.isSha256(record)) selectionHash = record;
                selectedHitP = c.getDouble(1);
                decisionId = safe(c.getString(2));
            }
        } finally {
            if (c != null) c.close();
        }

        String calibrationHash = V51ChecksumEngine.sha256(
                "E02D_CALIBRATION|" + V51SelectedCalibrationArtifact.ARTIFACT_ID + '|'
                        + V51SelectedCalibrationArtifact.DATA_SHA256 + '|'
                        + V51SelectedCalibrationArtifact.PROVEN + '|'
                        + V51SelectedCalibrationArtifact.OOS_ECE + '|'
                        + Double.toString(selectedHitP));

        CertificateEvidence cert = certificateEvidence(db,store,request.targetIssue,compiled,snapshot,snapshotHash);
        String budgetHash = V51ChecksumEngine.sha256(
                "E02D_BUDGET|request_to_durable=" + Batch2ContractCoreR926.REQUEST_TO_DURABLE_BUDGET_MS
                        + "|compute=" + Batch2ContractCoreR926.COMPUTE_BUDGET_MS
                        + "|cold=" + Batch2ContractCoreR926.COLD_START_TARGET_MS
                        + "|cancel=" + Batch2ContractCoreR926.RESEARCH_CANCEL_TARGET_MS
                        + "|db_slice=" + Batch2ContractCoreR926.DB_SLICE_TARGET_MS
                        + "|deadline_at=" + request.deadlineAt
                        + "|runtime_budget_hash=" + RuntimeResourceAuditR927.frozenBudgetHash());
        String actualConfig =
                "game=" + ReleaseManifestR926.GAME_CODE
                        + "|version_name=" + ReleaseManifestR926.VERSION_NAME
                        + "|version_code=" + ReleaseManifestR926.VERSION_CODE
                        + "|manifest=" + ReleaseManifestR926.MANIFEST_SHA256
                        + "|db_user_version=" + ReleaseManifestR926.DB_USER_VERSION
                        + "|play=single-position|fixed-K3|max-one-REAL-per-issue"
                        + "|decision_owner=" + ReleaseManifestR926.DECISION_OWNER
                        + "|settlement_owner=" + ReleaseManifestR926.SETTLEMENT_OWNER
                        + "|model_proven=" + V51BuildManifest.MODEL_PROVEN
                        + "|shadow_only=" + V51BuildManifest.SHADOW_ONLY
                        + "|real_unlock=" + AppConfig.R36_REAL_BET_UNLOCK
                        + "|calibration_artifact=" + V51SelectedCalibrationArtifact.ARTIFACT_ID
                        + "|calibration_proven=" + V51SelectedCalibrationArtifact.PROVEN
                        + "|decision_id=" + decisionId
                        + "|governance_status=" + safe(governanceStatus)
                        + "|source_event_mode=" + source.mode
                        + "|source_event_cutoff_at=" + source.cutoffAt
                        + "|certificate_mode=" + cert.mode
                        + "|research_certificate_may_authorize_real=" + ResearchCertificateR906.MAY_AUTHORIZE_REAL;
        return new EvidencePack(dataHash, featureHash, modelHash, stateHash, selectionHash,
                calibrationHash, cert.hash, budgetHash, actualConfig, cert.mode,
                source.hash, source.mode, source.cutoffAt);
    }

    /**
     * E02B -> E02D hard link.  A durable BET may only use a source-event version whose immutable
     * payload was actually accepted before the pre-draw cutoff.  The cutoff is the request wall time
     * for live prediction and is tightened to target draw time for historical replay, so a correction
     * received after the target draw can never become a historical pre-draw feature.
     */
    private static SourceEvidence sourceEvidence(
            SQLiteDatabase db, AnalysisSingleFlightR36.Request request) {
        long cutoffAt = Math.max(1L, request.requestAt);
        long targetDrawAt = issueDrawAt(db, request.targetIssue);
        if (targetDrawAt > 0L && targetDrawAt < cutoffAt) cutoffAt = targetDrawAt;

        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT request_id,provider,payload_hash,first_available_at,received_at,draw_at,"
                            + "clock_epoch,label_version,status FROM "
                            + SchemaRevisionR925.TABLE_FETCH_LEDGER
                            + " WHERE latest_issue=? AND first_available_at>0 AND first_available_at<=?"
                            + " AND received_at>0 AND received_at<=?"
                            + " AND status IN ('BUSINESS_ACCEPTED','ACCEPTED_DUPLICATE')"
                            + " ORDER BY first_available_at DESC,received_at DESC LIMIT 1",
                    new String[] {request.baseIssue, String.valueOf(cutoffAt), String.valueOf(cutoffAt)});
            if (!c.moveToFirst())
                return new SourceEvidence("", "SOURCE_EVENT_MISSING_BEFORE_PIT", cutoffAt);

            String requestId = safe(c.getString(0));
            String provider = safe(c.getString(1));
            String payloadHash = safe(c.getString(2));
            long firstAvailableAt = c.getLong(3);
            long receivedAt = c.getLong(4);
            long rowDrawAt = c.getLong(5);
            String clockEpoch = safe(c.getString(6));
            String labelVersion = safe(c.getString(7));
            String status = safe(c.getString(8));
            long canonicalDrawAt = issueDrawAt(db, request.baseIssue);
            long effectiveDrawAt = rowDrawAt > 0L ? rowDrawAt : canonicalDrawAt;

            boolean valid = requestId.length() > 0
                    && provider.length() > 0
                    && Batch2ContractCoreR926.isSha256(payloadHash)
                    && firstAvailableAt > 0L && firstAvailableAt <= cutoffAt
                    && receivedAt > 0L && receivedAt <= cutoffAt
                    && firstAvailableAt <= receivedAt
                    && effectiveDrawAt > 0L
                    && Batch2ContractCoreR926.isSha256(clockEpoch)
                    && labelVersion.length() > 0
                    && ("BUSINESS_ACCEPTED".equals(status) || "ACCEPTED_DUPLICATE".equals(status));
            if (rowDrawAt > 0L && canonicalDrawAt > 0L && rowDrawAt != canonicalDrawAt) valid = false;
            if (!valid) return new SourceEvidence("", "SOURCE_EVENT_INVALID_BEFORE_PIT", cutoffAt);

            String hash = V51ChecksumEngine.sha256(
                    "E02B_SOURCE_EVENT|" + ReleaseManifestR926.GAME_CODE + '|' + request.baseIssue + '|'
                            + requestId + '|' + provider + '|' + payloadHash + '|' + firstAvailableAt + '|'
                            + receivedAt + '|' + effectiveDrawAt + '|' + clockEpoch + '|' + labelVersion + '|'
                            + status + "|pit_cutoff=" + cutoffAt);
            return new SourceEvidence(hash, "PREDRAW_SOURCE_EVENT_BOUND", cutoffAt);
        } finally {
            if (c != null) c.close();
        }
    }

    private static long issueDrawAt(SQLiteDatabase db, String issue) {
        String target = safe(issue);
        if (!TargetIssueResolver.isValid(target)) return 0L;
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT COALESCE(open_time,0) FROM " + DbHelper.TABLE_DRAWS
                            + " WHERE issue=? AND dirty=0 LIMIT 1",
                    new String[] {target});
            if (!c.moveToFirst()) return 0L;
            return ProviderFreshnessGateR3611.normalizeEpochMs(c.getLong(0));
        } finally {
            if (c != null) c.close();
        }
    }

    /** Verify the existing owner's serialized envelope against the current compiled contract. */
    private static boolean exactAuthorizationMatches(String authorization,V51CompiledActionContract compiled) {
        if(!V51AuthorizationCore.isBetEnvelope(authorization))return false;
        String decisionId=authorization.split("\\|",-1)[1];
        V51Models.AuthorizedDecision persisted=new V51Models.AuthorizedDecision(
                V51Enums.DecisionAction.BET,V51Enums.RejectCode.NONE,compiled.selected,
                decisionId,authorization,V51ArtifactBundleManifest.BUNDLE_HASH);
        return V51AuthorizationCore.verify(persisted,compiled);
    }

    /** Validate an existing non-BET owner envelope; this never authorizes a Trade. */
    private static boolean nonBetAuthorizationMatches(String action,String authorization,V51CompiledActionContract compiled){
        if(authorization==null)return false;
        String[] fields=authorization.split("\\|",-1);
        if(fields.length!=8||!"V51AUTH".equals(fields[0])||!action.equals(fields[4])||"BET".equals(action))return false;
        try{
            V51Models.AuthorizedDecision persisted=new V51Models.AuthorizedDecision(
                    V51Enums.DecisionAction.valueOf(action),V51Enums.RejectCode.valueOf(fields[5]),compiled.selected,
                    fields[1],authorization,V51ArtifactBundleManifest.BUNDLE_HASH);
            if(V51AuthorizationCore.verify(persisted,compiled))return true;
            // decideV51B12 tightens a rejected input into SHADOW and removes its exact proof.
            // Reconstruct that existing owner's contract for verification only, never authorize.
            V51SelectiveRiskController.Result shadow=new V51SelectiveRiskController.Result(
                    V51Enums.Lane.SHADOW,V51Enums.RejectCode.valueOf(fields[5]),null,false,"B12_HARD_BLOCK");
            V51CompiledActionContract tightened=new V51CompiledActionContract(compiled.selected,shadow,compiled.stability,
                    compiled.featureHash,compiled.stateHash,compiled.expertHash,compiled.probabilityHash,compiled.actionHash);
            return V51AuthorizationCore.verify(persisted,tightened);
        }catch(IllegalArgumentException invalid){return false;}
    }

    /**
     * A Research contribution can never use a missing/revoked/out-of-range certificate as a
     * provenance substitute. Baseline/no-contribution is explicitly hashed as NOT_APPLICABLE rather
     * than pretending that a certificate exists.
     */
    private static CertificateEvidence certificateEvidence(SQLiteDatabase db,DbStore store,String targetIssue,V51CompiledActionContract compiled,DeferredEvidenceSnapshotR36.Header snapshot,String snapshotHash) {
        Cursor c = null;
        String candidateId = "", contributionEvidence = "", contributionRecord = "";
        boolean applied = false, controlledEligible = false;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT candidate_id,applied,controlled_influence_eligible,evidence_hash,record_hash"
                            + " FROM " + ResearchMainlineContributionAuditR922.TABLE
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {targetIssue});
            if (c.moveToFirst()) {
                candidateId = safe(c.getString(0));
                applied = c.getInt(1) == 1;
                controlledEligible = c.getInt(2) == 1;
                contributionEvidence = safe(c.getString(3));
                contributionRecord = safe(c.getString(4));
            }
        } finally {
            if (c != null) c.close();
        }
        if(compiled!=null&&compiled.certifiedPrediction!=null){
            CertifiedPredictionR974.Proof proof=compiled.certifiedPrediction;
            if(store==null||!targetIssue.equals(proof.target)||!proof.matches(compiled.selected)
                    ||!applied||!controlledEligible||!proof.candidate.equals(candidateId)
                    ||!proof.evidence.equals(contributionEvidence)
                    ||!Batch2ContractCoreR926.isSha256(contributionRecord))
                throw new IllegalStateException("B11_EXACT_CERTIFICATE_CONTRIBUTION_MISMATCH");
            String persistedAction, persistedAuthorization;
            try(Cursor decision=BudgetedQueryR927.query(db,"SELECT decision_action,authorization_hash FROM "+V51Repository.SELECTED+" WHERE target_issue=?",new String[]{targetIssue})){
                if(!decision.moveToFirst())throw new IllegalStateException("B11_EXACT_DECISION_MISSING");
                persistedAction=decision.getString(0);persistedAuthorization=decision.getString(1);
            }
            if(!"BET".equals(persistedAction)){
                if(!nonBetAuthorizationMatches(persistedAction,persistedAuthorization,compiled))
                    throw new IllegalStateException("B11_EXACT_NO_REAL_AUTHORIZATION_MISMATCH");
                try(Cursor real=BudgetedQueryR927.query(db,"SELECT 1 FROM r974_exact_real_receipt WHERE target_issue=? UNION ALL SELECT 1 FROM "+DbHelper.TABLE_TRADES+" WHERE target_issue=? AND action='BET' LIMIT 1",new String[]{targetIssue,targetIssue})){
                    if(real.moveToFirst())throw new IllegalStateException("B11_EXACT_NO_REAL_RECEIPT_CONFLICT");
                }
                String certificateBlock=CertifiedPredictionR974.verifyCertificate(store,proof);
                return new CertificateEvidence(V51ChecksumEngine.sha256("E02D_EXACT_NO_REAL|"+targetIssue+'|'+proof.hash+'|'+persistedAuthorization+'|'+certificateBlock),
                        "R974_EXACT_NO_REAL_AUTHORIZATION|certificate_reason="+(certificateBlock.isEmpty()?"GATE_REJECTED":certificateBlock));
            }
            String invalid=CertifiedPredictionR974.verifyCertificate(store,proof);
            if(!invalid.isEmpty())throw new IllegalStateException("B11_EXACT_CERTIFICATE_INVALID:"+invalid);
            // These are the immutable rows just written in THIS canonical transaction. Never
            // substitute an R906 shadow certificate for the current R974 exact consumer.
            try(Cursor exact=BudgetedQueryR927.query(db,
                    "SELECT e.candidate_id,e.model_hash,e.certificate_hash,e.proof_hash,e.recipe_hash,e.action_hash,e.authorization_hash"
                    +" FROM r974_exact_real_receipt e JOIN "+DbHelper.TABLE_TRADES+" t ON t._id=e.trade_id AND t.target_issue=e.target_issue"
                    +" JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o ON o.target_issue=e.target_issue"
                    +" WHERE e.target_issue=? AND t.action='BET' AND t.stat_scope='REAL_TRADE'"
                    +" AND e.authorization_hash=t.authorization_id AND e.authorization_hash=o.authorization_hash"
                    +" AND o.decision_action='BET' AND o.pre_draw_frozen=1 AND o.bundle_hash=? AND o.history_hash=? AND o.snapshot_hash=? AND o.contract_hash=?"
                    +" AND t.position=? AND t.n1=? AND t.n2=? AND t.n3=?",
                    new String[]{targetIssue,snapshot.bundleHash,proof.history,snapshotHash,compiled.selected.contractHash,
                            String.valueOf(compiled.selected.action.position),String.valueOf(compiled.selected.action.n1),
                            String.valueOf(compiled.selected.action.n2),String.valueOf(compiled.selected.action.n3)})){
                if(!exact.moveToFirst()||!proof.candidate.equals(exact.getString(0))
                        ||!proof.modelHash.equals(exact.getString(1))||!proof.certificate.equals(exact.getString(2))
                        ||!proof.hash.equals(exact.getString(3))||!V51ChecksumEngine.sha256(proof.recipe).equals(exact.getString(4))
                        ||!proof.actionHash.equals(exact.getString(5))||!exactAuthorizationMatches(exact.getString(6),compiled))
                    throw new IllegalStateException("B11_EXACT_CERTIFICATE_RECEIPT_MISMATCH");
                String authorization=exact.getString(6);
                if(exact.moveToNext())throw new IllegalStateException("B11_EXACT_CERTIFICATE_RECEIPT_CARDINALITY");
                return new CertificateEvidence(V51ChecksumEngine.sha256(
                        "E02D_EXACT_CERT|"+proof.candidate+'|'+proof.structure+'|'+proof.modelHash+'|'+proof.certificate
                        +'|'+proof.evidence+'|'+proof.hash+'|'+proof.actionHash+'|'+authorization+'|'+contributionRecord),
                        "R974_EXACT_CERTIFICATE_AND_CANONICAL_RECEIPT_BOUND");
            }
        }
        if(tableExists(db,"r974_exact_real_receipt"))try(Cursor exact=BudgetedQueryR927.query(db,
                "SELECT 1 FROM r974_exact_real_receipt WHERE target_issue=? LIMIT 1",new String[]{targetIssue})){
            if(exact.moveToFirst())throw new IllegalStateException("B11_EXACT_CERTIFICATE_PROOF_MISSING");
        }
        if (!applied) {
            return new CertificateEvidence(
                    V51ChecksumEngine.sha256(
                            "E02D_CERT_NOT_APPLICABLE|BASELINE_NO_RESEARCH_CONTRIBUTION|"
                                    + safe(targetIssue) + "|direct_real_authority=false"),
                    "BASELINE_NO_RESEARCH_CONTRIBUTION");
        }
        if (!controlledEligible || candidateId.length() == 0
                || !tableExists(db, DbHelper.TABLE_R906_RESEARCH_CERTIFICATE))
            return new CertificateEvidence("", "APPLIED_RESEARCH_CERTIFICATE_INVALID");

        c = null;
        try {
            c = BudgetedQueryR927.query(db,
                    "SELECT certificate_id,evidence_hash,record_hash,status,valid_from_issue,valid_until_issue"
                            + " FROM " + DbHelper.TABLE_R906_RESEARCH_CERTIFICATE
                            + " WHERE asset_id=? AND status='ACTIVE_SHADOW' ORDER BY updated_at DESC LIMIT 1",
                    new String[] {candidateId});
            if (!c.moveToFirst())
                return new CertificateEvidence("", "APPLIED_RESEARCH_CERTIFICATE_MISSING");
            String certificateId = safe(c.getString(0));
            String evidenceHash = safe(c.getString(1));
            String recordHash = safe(c.getString(2));
            String status = safe(c.getString(3));
            String validFrom = safe(c.getString(4));
            String validUntil = safe(c.getString(5));
            boolean inRange = TargetIssueResolver.isValid(validFrom)
                    && TargetIssueResolver.isValid(validUntil)
                    && TargetIssueResolver.compare(validFrom, targetIssue) <= 0
                    && TargetIssueResolver.compare(targetIssue, validUntil) <= 0;
            if (!inRange || !Batch2ContractCoreR926.isSha256(evidenceHash)
                    || !Batch2ContractCoreR926.isSha256(recordHash))
                return new CertificateEvidence("", "APPLIED_RESEARCH_CERTIFICATE_INVALID");
            String hash = V51ChecksumEngine.sha256(
                    "E02D_CERT|" + candidateId + '|' + certificateId + '|' + evidenceHash + '|'
                            + recordHash + '|' + status + '|' + validFrom + '|' + validUntil + '|'
                            + contributionEvidence + '|' + contributionRecord + '|'
                            + "may_authorize_real=" + ResearchCertificateR906.MAY_AUTHORIZE_REAL);
            return new CertificateEvidence(hash, "ACTIVE_SHADOW_CONTRIBUTION_EVIDENCE_BOUND");
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(db,"SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
                    new String[] {table});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    private static final class SourceEvidence {
        final String hash, mode;
        final long cutoffAt;
        SourceEvidence(String hash, String mode, long cutoffAt) {
            this.hash = safe(hash);
            this.mode = safe(mode);
            this.cutoffAt = Math.max(0L, cutoffAt);
        }
    }

    private static final class CertificateEvidence {
        final String hash, mode;
        CertificateEvidence(String hash, String mode) {
            this.hash = safe(hash);
            this.mode = safe(mode);
        }
    }

    private static final class EvidencePack {
        final String dataHash, featureHash, modelHash, stateHash, selectionHash;
        final String calibrationHash, certificateHash, budgetHash, actualConfig, certificateMode;
        final String sourceEventHash, sourceEventMode;
        final long sourceEventCutoffAt;
        EvidencePack(
                String dataHash,
                String featureHash,
                String modelHash,
                String stateHash,
                String selectionHash,
                String calibrationHash,
                String certificateHash,
                String budgetHash,
                String actualConfig,
                String certificateMode,
                String sourceEventHash,
                String sourceEventMode,
                long sourceEventCutoffAt) {
            this.dataHash = safe(dataHash);
            this.featureHash = safe(featureHash);
            this.modelHash = safe(modelHash);
            this.stateHash = safe(stateHash);
            this.selectionHash = safe(selectionHash);
            this.calibrationHash = safe(calibrationHash);
            this.certificateHash = safe(certificateHash);
            this.budgetHash = safe(budgetHash);
            this.actualConfig = safe(actualConfig);
            this.certificateMode = safe(certificateMode);
            this.sourceEventHash = safe(sourceEventHash);
            this.sourceEventMode = safe(sourceEventMode);
            this.sourceEventCutoffAt = Math.max(0L, sourceEventCutoffAt);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
