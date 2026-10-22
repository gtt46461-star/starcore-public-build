package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * R926 domain-scoped schema registry and same-user_version migration journal.
 *
 * <p>It is deliberately independent from model/research quality.  A domain stamp authenticates only
 * the DDL objects owned by that domain, so Research/Legacy additions cannot invalidate the Critical
 * startup contract.  Every migration step is individually journaled and idempotent.</p>
 */
public final class SchemaDomainCoordinatorR926 {
    public static final String DOMAIN_CRITICAL = "CRITICAL";
    public static final String DOMAIN_RESEARCH = "RESEARCH";
    public static final String DOMAIN_LEGACY = "LEGACY";
    public static final String TABLE_DOMAIN = "v57_schema_domain_r926";
    public static final String TABLE_JOURNAL = "v57_schema_migration_journal_r926";

    public interface Step { void run(SQLiteDatabase db) throws Exception; }

    public static final class Contract {
        public final String domain;
        public final int revision;
        public final String[] objects;
        public final String contractHash;
        Contract(String d, int r, String[] names) {
            domain = safe(d);
            revision = r;
            objects = names == null ? new String[0] : names.clone();
            Arrays.sort(objects);
            StringBuilder b = new StringBuilder(domain).append('|').append(revision);
            for (String name : objects) b.append('|').append(safe(name));
            contractHash = V51ChecksumEngine.sha256(b.toString());
        }
    }

    private SchemaDomainCoordinatorR926() {}

    public static Contract criticalContract() {
        return new Contract(
                DOMAIN_CRITICAL,
                ReleaseManifestR926.CRITICAL_DOMAIN_REVISION,
                new String[] {
                    DbHelper.TABLE_DRAWS,
                    DrawDependencyInputCP12.TABLE,
                    DrawDependencyInputCP12.SOURCE,
                    "cp12_draw_dependency_immutable",
                    "cp12_draw_dependency_delete_guard",
                    DbHelper.TABLE_DECISIONS,
                    DbHelper.TABLE_TRADES,
                    DbHelper.TABLE_SHADOW,
                    DbHelper.TABLE_APP_STATE,
                    DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                    "cp12_canonical_reason_immutable",
                    DbHelper.TABLE_R35_OOS_LABEL,
                    DbHelper.TABLE_R35_RUNTIME_HEALTH,
                    DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                    DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                    DbHelper.TABLE_R36_SETTLEMENT_TASK,
                    DbHelper.TABLE_R815_CRITICAL_ERROR,
                    DbHelper.TABLE_R815_LONG_TEXT,
                    DbHelper.TABLE_R815_RECOVERY_GENERATION,
                    DbHelper.TABLE_R815_SHADOW_SIDECAR,
                    DbHelper.TABLE_R906_ISSUE_CLOSURE,
                    DbHelper.TABLE_R906_SETTLEMENT_OUTBOX,
                    DbHelper.TABLE_R906_SELECTED_EVIDENCE,
                    DbHelper.TABLE_R906_RUNTIME_TRUTH,
                    V51Repository.ISSUE,
                    V51Repository.ACTION_SPACE,
                    V51Repository.CHAMPION,
                    V51Repository.FUNNEL,
                    V51Repository.SELECTED,
                    V51Repository.AUTH,
                    RuntimeEventLedgerR926.TABLE_EVENT,
                    RuntimeEventLedgerR926.TABLE_PROJECTION,
                    RuntimeEventLedgerR926.TABLE_OUTBOX,
                    SchemaRevisionR925.TABLE_FETCH_LEDGER,
                    SchemaRevisionR925.TABLE_PREDICTION_TIMING,
                    RuntimeResourceAuditR927.TABLE,
                    SchemaRevisionR925.TABLE_RUNTIME_EPOCH
                });
    }

    public static Contract researchContract() {
        return new Contract(
                DOMAIN_RESEARCH,
                ReleaseManifestR926.RESEARCH_DOMAIN_REVISION,
                new String[] {
                    DbHelper.TABLE_R815_RESEARCH_CANDIDATE,
                    DbHelper.TABLE_R815_RESEARCH_EVAL,
                    DbHelper.TABLE_R815_RESEARCH_ARTIFACT,
                    DbHelper.TABLE_R815_RESEARCH_QUEUE,
                    DbHelper.TABLE_R906_RESEARCH_ASSET,
                    DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,
                    DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL,
                    DbHelper.TABLE_R906_RESEARCH_LEARNER_UPDATE,
                    DbHelper.TABLE_R906_RESEARCH_ARTIFACT_JOB,
                    DbHelper.TABLE_R906_RESEARCH_CERTIFICATE,
                    DbHelper.TABLE_R908_ALGORITHM_GENOME,
                    DbHelper.TABLE_R908_ALGORITHM_PREDRAW,
                    SchemaRevisionR925.TABLE_LIFECYCLE_AUDIT,
                    SchemaRevisionR925.TABLE_CAPTURE_OPPORTUNITY,
                    SchemaRevisionR925.TABLE_PROMOTION_WINDOW,
                    SchemaRevisionR925.TABLE_PROMOTION_HISTORY,
                    SchemaRevisionR925.TABLE_HYPOTHESIS,
                    SchemaRevisionR925.TABLE_CANDIDATE_COVERAGE,
                    SchemaRevisionR925.TABLE_ATOM_QUALITY,
                    SchemaRevisionR925.TABLE_ATOM_EDGE,
                    ResearchPlatformStoreR929.TABLE_TRIAL,
                    ResearchPlatformStoreR929.TABLE_CERTIFICATE,
                    ResearchPlatformStoreR929.TABLE_BUNDLE,
                    ResearchPlatformStoreR929.TABLE_CONTRIBUTION,
                    ResearchPlatformStoreR929.TABLE_EVENT,
                    ResearchPlatformStoreR929.TABLE_CERT_GATE,
                    ResearchPlatformStoreR929.TABLE_CERT_HISTORY,
                    ResearchPlatformStoreR929.TABLE_REVOCATION,
                    DirectedResearchCreationR932.GROUP, DirectedResearchCreationR932.ATTEMPT, DirectedResearchCreationR932.CURSOR,
                    ResearchLifecycleR932.TABLE,
                    ResearchLifecycleR932.EVENT,
                    ResearchLifecycleR932.ELITE,
                    ResearchLifecycleR932.CHILD,
                    CandidateCalibrationR932.OBS,
                    CandidateCalibrationR932.MODEL,
                    CandidateCalibrationR932.STUDY,
                    FixedResearchWindowR932.WINDOW,
                    FixedResearchWindowR932.CHAMPION,
                    FixedResearchWindowR932.PAIR,
                    FixedResearchWindowR932.CONTRACT,
                    AtomCaptureR932.TABLE,
                    AtomCaptureR932.QUALITY,
                    ObservationGenerationR932.META,
                    ObservationGenerationR932.RESTART,
                    ResearchMultiplicityR932.FAMILY,
                    ResearchMultiplicityR932.HYPOTHESIS,
                    ResearchMultiplicityR932.ALIAS,
                    ResearchMultiplicityR932.LOOK,
                    ResearchMultiplicityR932.RESEARCH, ResearchMultiplicityR932.IDENTITY,
                    ResearchTaskQueueR967.TABLE, ResearchTaskQueueR967.PIN, ResearchTaskQueueR967.LANE,
                    ResearchGatePlanR967.POLICY, ResearchGatePlanR967.PLAN, ResearchGatePlanR967.REQUIRED,
                    ResearchEvaluationRunR967.RUN, ResearchEvaluationRunR967.INPUT, ResearchEvaluationRunR967.EVAL, ResearchEvaluationRunR967.ALL,
                    "r969_run_no_replace", "r969_input_no_replace", "r969_eval_no_replace",
                    "r967_numeric_training_run_no_update", "r967_numeric_training_run_no_delete", "r967_numeric_training_input_no_update", "r967_numeric_training_input_no_delete", "r967_frozen_evaluation_v2_no_update", "r967_frozen_evaluation_v2_no_delete",
                    HistoricalGenomeImportR967.SOURCE, HistoricalGenomeImportR967.ARCHIVE, HistoricalGenomeImportR967.USE,
                    "research_historical_source_r967_no_update", "research_historical_source_r967_no_delete", "research_historical_source_r967_no_replace",
                    "research_historical_genome_archive_r967_no_update", "research_historical_genome_archive_r967_no_delete", "research_historical_genome_archive_r967_no_replace",
                    "research_historical_genome_use_r967_no_update", "research_historical_genome_use_r967_no_delete", "research_historical_genome_use_r967_no_replace",
                    PairedResearchChallengeR932.TABLE,
                    PairedResearchChallengeR932.PREDICTION,
                    FixedWindowStatisticsR932.TABLE,
                    AtomicToolWorldR943.CONTEXT, AtomicToolWorldR943.QUALITY, AtomicToolWorldR943.MODEL, StagedResearchRosterR943.TABLE, StagedResearchRosterR943.MANIFEST,
                    AlgorithmProofRecoveryR948.HEAD, AlgorithmProofRecoveryR948.AUDIT, AlgorithmProofRecoveryR948.QUARANTINE,
                    GlobalResearchScheduleR932.PLAN, GlobalResearchScheduleR932.TASK,
                    GlobalResearchScheduleR932.IDENTITY, GlobalResearchScheduleR932.PROXY,
                    ResearchCaptureLedgerR932.TABLE, ResearchCaptureLedgerR932.EVENT,
                    CandidateExecutionAuditR932.TABLE, FixedWindowCertificatePublisherR932.TABLE,
                    ResearchCohortR932.IDENTITY, ResearchCohortR932.COMPARISON, ResearchCohortR932.CHAMPION,
                    AtomAdmissionAuditR932.AUDIT, AtomAdmissionAuditR932.RECOVERY, AtomAdmissionAuditR932.ARCHIVE,
                    ModuleAblationR932.TABLE, ModuleCalibrationR932.OBS, ModuleCalibrationR932.MODEL,
                    MolecularToolWorldR947.TABLE, CausalResearchFeedbackR947.TABLE,
                    "r952_runtime_event", "r952_research_outbox", "r952_learning_cursor",
                    "r952_candidate_funnel", "r952_module_registry", "r952_recovery_journal",
                    "r952_candidate", "r952_candidate_predraw", "r952_intent", "r952_artifact",
                    "r952_world_atom", "r952_world_molecule", "r952_causal_edge", "r952_matrix_snapshot",
                    "r952_knowledge", "r952_pareto", "r958_frozen_candidate", "r958_frozen_predraw",
                    "idx_r958_world_semantic", "r952_world_atom_immutable_update", "r952_world_atom_immutable_delete",
                    "r952_world_molecule_immutable_update", "r952_world_molecule_immutable_delete"
                    , "r952_matrix_feedback", "r952_matrix_snapshot_immutable_update", "r952_matrix_feedback_immutable_update"
                    , "r952_knowledge_immutable_update", "r952_knowledge_admission_delete", "r959_schedule_mode_immutable"
                    , "research_plan_migration_v2", "research_task_checkpoint_v2", "research_capture_reservation_v2", "idx_cp2_capture_due", "research_capture_gap_v2", "research_atom_spec_v2", "research_atom_binding_v2", "research_matrix_checkpoint_v2", "research_projection_ack_v2", "research_finding_v2", "idx_cp2_finding_queue", "research_action_v2", "research_result_v2", "research_certificate_policy_v2", "research_evidence_archive_v2", "idx_cp2_archive_prune", "research_finding_evidence_v2", "research_molecule_seed_v2", "idx_cp4_molecule_seed_state", "research_evidence_archive_member_v2", "idx_cp4_archive_member_lookup", "research_retention_permit_v2", "research_evidence_locator_v2", "research_cp4_frozen_lifetime", "research_cp4_window_binding", "cp4_lifetime_no_replace", "cp4_binding_no_replace", "research_cp4_frozen_lifetime_immutable_UPDATE", "research_cp4_frozen_lifetime_immutable_DELETE", "research_cp4_window_binding_immutable_UPDATE", "research_cp4_window_binding_immutable_DELETE", "research_cp4_candidate_resource", "idx_cp4_resource_target", "research_cp4_candidate_resource_immutable_UPDATE", "research_cp4_candidate_resource_immutable_DELETE", "cp4_candidate_resource_conflict"
                    , "diagnosis_fix_plan","cp12_revision_diagnosis_fix_plan_insert","cp12_revision_diagnosis_fix_plan_update","cp12_revision_diagnosis_fix_plan_delete","cp12_archive_guard_diagnosis_fix_plan","cp12_domain_null_diagnosis_fix_plan","cp12_domain_cap_diagnosis_fix_plan","dynamic_freeze_matrix","cp12_revision_dynamic_freeze_matrix_insert","cp12_revision_dynamic_freeze_matrix_update","cp12_revision_dynamic_freeze_matrix_delete","cp12_archive_guard_dynamic_freeze_matrix","cp12_domain_null_dynamic_freeze_matrix","cp12_domain_cap_dynamic_freeze_matrix","module_effect_cache","cp12_revision_module_effect_cache_insert","cp12_revision_module_effect_cache_update","cp12_revision_module_effect_cache_delete","cp12_archive_guard_module_effect_cache","cp12_domain_null_module_effect_cache","cp12_domain_cap_module_effect_cache","shadow_promotion_cache","cp12_revision_shadow_promotion_cache_insert","cp12_revision_shadow_promotion_cache_update","cp12_revision_shadow_promotion_cache_delete","cp12_archive_guard_shadow_promotion_cache","cp12_domain_null_shadow_promotion_cache","cp12_domain_cap_shadow_promotion_cache","adaptive_judge_log","cp12_revision_adaptive_judge_log_insert","cp12_revision_adaptive_judge_log_update","cp12_revision_adaptive_judge_log_delete","cp12_archive_guard_adaptive_judge_log","diagnosis_case_log","cp12_revision_diagnosis_case_log_insert","cp12_revision_diagnosis_case_log_update","cp12_revision_diagnosis_case_log_delete","cp12_archive_guard_diagnosis_case_log"
                    // F05 additive objects change contractHash: an old READY stamp cannot skip migration.
                    , "research_exact_probability_f05", "f05_exact_probability_no_update", "f05_exact_probability_no_replace"
                    , "f05_cal_model_no_update", "f05_cal_model_no_replace", "f05_cal_frozen_obs_no_update", "f05_cal_frozen_obs_no_replace"
                    , "f05_cal_model_archive_delete", "f05_cal_frozen_obs_archive_delete"
                    // Additive seek contract changes contractHash and rearms an old READY domain.
                    , "research_molecule_import_checkpoint_cp12", "cp12_molecule_checkpoint_identity", "cp12_molecule_checkpoint_no_delete", "cp12_molecule_checkpoint_no_replace"
                    // CP12 required owner storage; retain all existing CP4 required objects.
                    , "r967_frozen_evaluation_v2", "research_action_input_boundary_cp12", "research_action_proposal_cp12", "research_action_result_binding_cp12", "research_artifact_identity_v2", "research_atom_catalog_import_cp12", "research_calibration_quality_cp12", "research_development_receipt_v2", "research_durable_model_identity_cp12", "research_durable_model_ref_cp12", "research_family_creation_cp12", "research_family_cursor_cp12", "research_family_wheel_cp12", "research_feedback_rejection_cp12", "research_formal_observation_cp12", "research_matrix_input_cp12", "research_matrix_regime5_cp12", "research_matrix_spec_cp12", "research_matrix_terminal_cp12", "research_molecule_import_cp12", "research_molecule_use_cp12", "research_row_revision_v2", "research_task_input_binding_v2", "research_task_retention_expiry_cp12", "research_training_attachment_cp12", "research_training_feature_cursor_cp12", "research_training_feature_row_cp11", "research_training_input_v2", "research_training_protocol_attachment_cp12", "research_training_protocol_v2", "research_training_registration_row_cp12", "research_working_storage_cp12", "research_working_storage_ref_cp12", "cp12_candidate_semantic_identity", "cp12_candidate_semantic_immutable", "cp12_canonical_reason_immutable", "research_atom_admission_cp12", "research_action_teacher_binding_cp12", "research_child_teacher_lineage_cp12", "research_action_teacher_witness_cp12"
                });
    }

    public static Contract legacyContract() {
        return new Contract(
                DOMAIN_LEGACY,
                ReleaseManifestR926.LEGACY_DOMAIN_REVISION,
                new String[] {
                    DbHelper.TABLE_REALTIME_VALIDATE,
                    DbHelper.TABLE_TEACHER_SNAPSHOT,
                    DbHelper.TABLE_LEARNING_CASE_LOG,
                    DbHelper.TABLE_RUNTIME_PROFILE_CACHE,
                    DbHelper.TABLE_STATE_NUMBER_PROFILE,
                    DbHelper.TABLE_AUTO_LEARN_CURSOR,
                    DbHelper.TABLE_TEACHER_MATRIX_SAMPLE,
                    DbHelper.TABLE_TEACHER_MATRIX_PROFILE,
                    DbHelper.TABLE_TEACHER_MATRIX_COMBO_PROFILE,
                    DbHelper.TABLE_TEACHER_MATRIX_REPORT
                });
    }

    public static void ensureInfrastructure(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R926_SCHEMA_DB_NULL");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DOMAIN
                + "(domain TEXT PRIMARY KEY,revision INTEGER NOT NULL,contract_hash TEXT NOT NULL,"
                + "fingerprint TEXT NOT NULL,state TEXT NOT NULL,cursor TEXT NOT NULL DEFAULT '',"
                + "started_at INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_JOURNAL
                + "(domain TEXT NOT NULL,revision INTEGER NOT NULL,step_id TEXT NOT NULL,status TEXT NOT NULL,"
                + "attempt INTEGER NOT NULL DEFAULT 0,cursor TEXT NOT NULL DEFAULT '',before_hash TEXT NOT NULL DEFAULT '',"
                + "after_hash TEXT NOT NULL DEFAULT '',started_at INTEGER NOT NULL DEFAULT 0,completed_at INTEGER NOT NULL DEFAULT 0,"
                + "error_code TEXT NOT NULL DEFAULT '',PRIMARY KEY(domain,revision,step_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r926_migration_status ON " + TABLE_JOURNAL
                + "(domain,revision,status,started_at)");
    }

    public static boolean matches(SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return false;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT revision,contract_hash,fingerprint,state FROM " + TABLE_DOMAIN
                    + " WHERE domain=?", new String[] {contract.domain});
            if (!c.moveToFirst()) return false;
            if (c.getInt(0) != contract.revision) return false;
            if (!contract.contractHash.equals(c.getString(1))) return false;
            if (!"READY".equals(c.getString(3))) return false;
            return fingerprint(db, contract).equals(c.getString(2));
        } catch (RuntimeException unavailable) { return false; }
        finally { if (c != null) c.close(); }
    }

    public static void verifyAndStamp(SQLiteDatabase db, Contract contract) {
        ensureInfrastructure(db);
        verifyObjects(db, contract);
        String fingerprint = fingerprint(db, contract);
        long now = System.currentTimeMillis();
        db.execSQL("INSERT OR REPLACE INTO " + TABLE_DOMAIN
                        + "(domain,revision,contract_hash,fingerprint,state,cursor,started_at,updated_at,last_error)"
                        + " VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[] {contract.domain, Integer.valueOf(contract.revision), contract.contractHash,
                        fingerprint, "READY", "COMPLETE", Long.valueOf(now), Long.valueOf(now), ""});
    }

    public static void markState(SQLiteDatabase db, Contract contract, String state, String cursor, String error) {
        ensureInfrastructure(db);
        long now = System.currentTimeMillis();
        db.execSQL("INSERT OR REPLACE INTO " + TABLE_DOMAIN
                        + "(domain,revision,contract_hash,fingerprint,state,cursor,started_at,updated_at,last_error)"
                        + " VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[] {contract.domain, Integer.valueOf(contract.revision), contract.contractHash,
                        safeFingerprint(db, contract), safe(state), safe(cursor), Long.valueOf(now), Long.valueOf(now), bounded(error, 900)});
    }

    /**
     * Detects same-revision contract drift (missing/corrupted schema objects, stale fingerprint,
     * or a contract-hash change that was deployed without bumping user_version) and re-arms
     * previously completed idempotent steps.  This is intentionally domain-scoped: repairing a
     * Critical object never causes Research/Legacy DDL to run on the startup path.
     *
     * <p>Returns {@code true} only when a repair was armed.  New installs and normal revision
     * upgrades are left alone because their journal rows are already absent/incomplete.</p>
     */
    public static boolean prepareRepairIfDrift(SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return false;
        ensureInfrastructure(db);
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT revision,contract_hash,fingerprint,state FROM " + TABLE_DOMAIN
                    + " WHERE domain=?", new String[] {contract.domain});
            if (!c.moveToFirst()) return false;
            int storedRevision = c.getInt(0);
            String storedContractHash = safe(c.getString(1));
            String storedFingerprint = safe(c.getString(2));
            String storedState = safe(c.getString(3));
            if (storedRevision != contract.revision) return false;
            // Only stable READY or terminal FAILED boundaries can arm repair below.
            // Intermediate migration states cannot qualify, even when their partial DDL drifts.
            if (!"READY".equals(storedState) && !"FAILED".equals(storedState)) return false;

            String currentFingerprint = safeFingerprint(db, contract);
            boolean contractChanged = !contract.contractHash.equals(storedContractHash);
            boolean fingerprintChanged = !currentFingerprint.equals(storedFingerprint);
            boolean objectsMissing = !allObjectsPresent(db, contract);
            boolean invalid = contractChanged || fingerprintChanged || objectsMissing;
            boolean readyButInvalid = "READY".equals(storedState) && invalid;
            // Missing objects and fingerprint changes are normal while a bounded migration is
            // between steps. Re-arming COMPLETED rows here made a fresh database repeat its first
            // step forever. A FAILED verification with every journaled step complete is the other
            // stable boundary where replay is required; a FAILED individual step resumes itself.
            int journalRows = scalarInt(db, "SELECT COUNT(*) FROM " + TABLE_JOURNAL
                            + " WHERE domain=? AND revision=?",
                    new String[] {contract.domain, String.valueOf(contract.revision)}, 0);
            int incompleteRows = scalarInt(db, "SELECT COUNT(*) FROM " + TABLE_JOURNAL
                            + " WHERE domain=? AND revision=? AND status<>'COMPLETED'",
                    new String[] {contract.domain, String.valueOf(contract.revision)}, 0);
            boolean failedVerification = "FAILED".equals(storedState)
                    && journalRows > 0 && incompleteRows == 0 && invalid;
            boolean requiresRepair = readyButInvalid || failedVerification;
            if (!requiresRepair) return false;

            long now = System.currentTimeMillis();
            boolean ownTx = !db.inTransaction();
            if (ownTx) db.beginTransaction();
            try {
                db.execSQL("UPDATE " + TABLE_JOURNAL
                                + " SET status='REPAIR_REQUIRED',cursor='REPAIR',completed_at=0,error_code=?"
                                + " WHERE domain=? AND revision=? AND status='COMPLETED'",
                        new Object[] {"R926_CONTRACT_DRIFT", contract.domain, Integer.valueOf(contract.revision)});
                db.execSQL("INSERT OR REPLACE INTO " + TABLE_DOMAIN
                                + "(domain,revision,contract_hash,fingerprint,state,cursor,started_at,updated_at,last_error)"
                                + " VALUES(?,?,?,?,?,?,?,?,?)",
                        new Object[] {contract.domain, Integer.valueOf(contract.revision), contract.contractHash,
                                currentFingerprint, "REPAIR_REQUIRED", "REPLAY_IDEMPOTENT_STEPS",
                                Long.valueOf(now), Long.valueOf(now),
                                bounded("R926_CONTRACT_DRIFT|contractChanged=" + contractChanged
                                        + "|fingerprintChanged=" + fingerprintChanged
                                        + "|objectsMissing=" + objectsMissing, 900)});
                if (ownTx) db.setTransactionSuccessful();
            } finally {
                if (ownTx && db.inTransaction()) db.endTransaction();
            }
            return true;
        } finally { if (c != null) c.close(); }
    }

    public static boolean stepComplete(SQLiteDatabase db, Contract contract, String stepId) {
        ensureInfrastructure(db);
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT status FROM " + TABLE_JOURNAL
                            + " WHERE domain=? AND revision=? AND step_id=?",
                    new String[] {contract.domain, String.valueOf(contract.revision), safe(stepId)});
            return c.moveToFirst() && "COMPLETED".equals(c.getString(0));
        } finally { if (c != null) c.close(); }
    }

    public static void runStep(SQLiteDatabase db, Contract contract, String stepId, Step step) throws Exception {
        if (stepComplete(db, contract, stepId)) return;
        ensureInfrastructure(db);
        final long now = System.currentTimeMillis();
        int attempt = scalarInt(db, "SELECT COALESCE(MAX(attempt),0)+1 FROM " + TABLE_JOURNAL
                        + " WHERE domain=? AND revision=? AND step_id=?",
                new String[] {contract.domain, String.valueOf(contract.revision), safe(stepId)}, 1);
        String before = safeFingerprint(db, contract);
        db.execSQL("INSERT OR REPLACE INTO " + TABLE_JOURNAL
                        + "(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                new Object[] {contract.domain, Integer.valueOf(contract.revision), safe(stepId), "RUNNING",
                        Integer.valueOf(attempt), "START", before, "", Long.valueOf(now), Long.valueOf(0L), ""});
        boolean ownTx = !db.inTransaction();
        try {
            if (ownTx) db.beginTransaction();
            step.run(db);
            String after = safeFingerprint(db, contract);
            db.execSQL("UPDATE " + TABLE_JOURNAL
                            + " SET status='COMPLETED',cursor='DONE',after_hash=?,completed_at=?,error_code=''"
                            + " WHERE domain=? AND revision=? AND step_id=?",
                    new Object[] {after, Long.valueOf(System.currentTimeMillis()), contract.domain,
                            Integer.valueOf(contract.revision), safe(stepId)});
            if (ownTx) db.setTransactionSuccessful();
        } catch (Throwable failure) {
            if (ownTx && db.inTransaction()) {
                try { db.endTransaction(); } catch (Throwable ignored) {}
                ownTx = false;
            }
            db.execSQL("UPDATE " + TABLE_JOURNAL
                            + " SET status='FAILED',cursor='RETRY',completed_at=?,error_code=?"
                            + " WHERE domain=? AND revision=? AND step_id=?",
                    new Object[] {Long.valueOf(System.currentTimeMillis()), error(failure), contract.domain,
                            Integer.valueOf(contract.revision), safe(stepId)});
            if (failure instanceof Exception) throw (Exception) failure;
            throw new IllegalStateException("R926_MIGRATION_STEP_FAILED:" + stepId, failure);
        } finally {
            if (ownTx && db.inTransaction()) db.endTransaction();
        }
    }

    public static String fingerprint(SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return "";
        Set<String> wanted = new HashSet<String>(Arrays.asList(contract.objects));
        ArrayList<String> rows = new ArrayList<String>();
        String sql = "SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master"
                + " WHERE name NOT LIKE 'sqlite_%'";
        String[] args = null;
        boolean filter = !wanted.isEmpty() && wanted.size() <= 499;
        for (String name : wanted) {
            if (name == null || name.length() == 0) { filter = false; break; }
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                        || (ch >= '0' && ch <= '9') || ch == '_')) { filter = false; break; }
            }
        }
        if (filter) {
            // Reduce cursor transfer only: SQLite still scans its catalog. Java's filter below
            // remains authoritative. Match String.trim() for ASCII controls; retain suspicious
            // identifiers so NUL, NULL or non-TEXT catalog values cannot hide a matching row.
            StringBuilder marks = new StringBuilder();
            ArrayList<String> values = new ArrayList<String>(wanted);
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) marks.append(',');
                marks.append('?');
            }
            String trimChars = "char(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32)";
            sql += " AND (trim(name," + trimChars + ") COLLATE BINARY IN (" + marks + ")"
                    + " OR trim(tbl_name," + trimChars + ") COLLATE BINARY IN (" + marks + ")"
                    + " OR instr(name,char(0))>0 OR instr(tbl_name,char(0))>0"
                    + " OR typeof(name)<>'text' OR typeof(tbl_name)<>'text')";
            values.addAll(new ArrayList<String>(values));
            args = values.toArray(new String[values.size()]);
        }
        Cursor c = db.rawQuery(sql + " ORDER BY type,name", args);
        try {
            while (c.moveToNext()) {
                String name = safe(c.getString(1));
                String table = safe(c.getString(2));
                if (!wanted.contains(name) && !wanted.contains(table)) continue;
                rows.add(safe(c.getString(0)) + '|' + name + '|' + table + '|' + safe(c.getString(3)));
            }
        } finally { c.close(); }
        Collections.sort(rows);
        StringBuilder b = new StringBuilder(contract.domain).append('|').append(contract.revision)
                .append('|').append(contract.contractHash);
        for (String row : rows) b.append('\n').append(row.length()).append(':').append(row);
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static void verifyObjects(SQLiteDatabase db, Contract contract) {
        ArrayList<String> missing = new ArrayList<String>();
        for (String name : contract.objects) if (!schemaObjectExists(db, name)) missing.add(name);
        if (!missing.isEmpty()) throw new IllegalStateException("R926_" + contract.domain + "_OBJECTS_MISSING:" + join(missing));
    }

    private static boolean allObjectsPresent(SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return false;
        for (String name : contract.objects) if (!schemaObjectExists(db, name)) return false;
        return true;
    }

    private static boolean schemaObjectExists(SQLiteDatabase db, String name) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=? LIMIT 1", new String[] {name});
            return c.moveToFirst();
        } finally { if (c != null) c.close(); }
    }

    private static int scalarInt(SQLiteDatabase db, String sql, String[] args, int fallback) {
        Cursor c = null;
        try { c = db.rawQuery(sql, args); return c.moveToFirst() ? c.getInt(0) : fallback; }
        catch (Throwable ignored) { return fallback; }
        finally { if (c != null) c.close(); }
    }
    private static String safeFingerprint(SQLiteDatabase db, Contract c) {
        try { return fingerprint(db, c); } catch (Throwable ignored) { return ""; }
    }
    private static String error(Throwable t) {
        return bounded((t == null ? "UNKNOWN" : t.getClass().getSimpleName() + ':' + safe(t.getMessage())), 900);
    }
    private static String join(List<String> xs) {
        StringBuilder b = new StringBuilder(); for (String x : xs) { if (b.length() > 0) b.append(','); b.append(x); } return b.toString();
    }
    private static String bounded(String s, int max) { String x=safe(s); return x.length()<=max?x:x.substring(0,max); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }

public static boolean matchesSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return false;
        Cursor c = null;
        try {
            c = report.select("SELECT revision,contract_hash,fingerprint,state FROM " + TABLE_DOMAIN
                    + " WHERE domain=?", new String[] {contract.domain});
            if (!c.moveToFirst()) return false;
            if (c.getInt(0) != contract.revision) return false;
            if (!contract.contractHash.equals(c.getString(1))) return false;
            if (!"READY".equals(c.getString(3))) return false;
            return fingerprintSnapshotCP4(report, db, contract).equals(c.getString(2));
        } catch (RuntimeException unavailable) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",unavailable);}
        finally { if (c != null) c.close(); }
    }

public static String fingerprintSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return "";
        Set<String> wanted = new HashSet<String>(Arrays.asList(contract.objects));
        ArrayList<String> rows = new ArrayList<String>();
        Cursor c = report.select("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master"
                + " WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name", null);
        try {
            while (c.moveToNext()) {
                String name = safeSnapshotCP4(report, c.getString(1));
                String table = safeSnapshotCP4(report, c.getString(2));
                if (!wanted.contains(name) && !wanted.contains(table)) continue;
                rows.add(safeSnapshotCP4(report, c.getString(0)) + '|' + name + '|' + table + '|' + safeSnapshotCP4(report, c.getString(3)));
            }
        } finally { c.close(); }
        Collections.sort(rows);
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, contract.domain).append('|').append(contract.revision)
                .append('|').append(contract.contractHash);
        for (String row : rows) b.append('\n').append(row.length()).append(':').append(row);
        return com.ethk3.novaonepro.ReportTextCP12.sha256(report, b.toString());
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s) { return s == null ? "" : s.trim(); }
}
