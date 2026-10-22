package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite persistence for the R929 experiment registry, unified certificates and atomic bundles.
 * All mutations run through DatabaseCoordinatorR36; readers never activate or repair state.
 */
public final class ResearchPlatformStoreR929 {
    public static final String VERSION = "R929_RESEARCH_PLATFORM_STORE_V1";
    public static final String TABLE_TRIAL = "research_trial_registry_r929";
    public static final String TABLE_CERTIFICATE = "research_certificate_r929";
    public static final String TABLE_BUNDLE = "research_model_bundle_r929";
    public static final String TABLE_CONTRIBUTION = "research_mainline_contribution_r929";
    public static final String TABLE_EVENT = "research_platform_event_r929";
    public static final boolean MAY_CREATE_REAL = false;

    private static final java.util.WeakHashMap<SQLiteDatabase,Boolean> READY=new java.util.WeakHashMap<SQLiteDatabase,Boolean>();
    private static final Object SCHEMA_LOCK = new Object();

    public static final String TABLE_CERT_GATE="research_certificate_gate_r932";
    public static final String TABLE_CERT_HISTORY="research_certificate_history_r932",TABLE_REVOCATION="research_certificate_revocation_r932";
    private ResearchPlatformStoreR929() {}

    public static void ensureSchema(final DbStore store) {
        if (store == null) throw new IllegalArgumentException("R929_STORE_REQUIRED");
        synchronized (SCHEMA_LOCK) {
            SQLiteDatabase handle=store.readable();
            if (READY.containsKey(handle)) return;
            Boolean ok = store.coordinatorR36().runAtomicWrite("R929_PLATFORM_SCHEMA",
                    new DatabaseCoordinator.WriteCallable<Boolean>() {
                        @Override public Boolean call(SQLiteDatabase db) {
                            createSchema(db);
                            return Boolean.TRUE;
                        }
                    }, Boolean.FALSE);
            if (!Boolean.TRUE.equals(ok))
                throw new IllegalStateException("R929_PLATFORM_SCHEMA_FAILED");
            READY.put(handle,Boolean.TRUE);
        }
    }

    public static void createSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R929_DB_REQUIRED");
        // Bundle capture runs every issue. Reuse only committed DDL readiness;
        // schema_version changes and rollbacks invalidate this optimization.
        if (TransactionSchemaCacheR941.readyCommitted(db,"ResearchPlatformStoreR929")) return;
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TRIAL + "("
                + "trial_id TEXT PRIMARY KEY NOT NULL,identity_hash TEXT NOT NULL UNIQUE,"
                + "stage TEXT NOT NULL,status TEXT NOT NULL,dataset_hash TEXT NOT NULL,"
                + "schema_hash TEXT NOT NULL,source_hash TEXT NOT NULL,hyper_hash TEXT NOT NULL,"
                + "family_id TEXT NOT NULL,structural_hash TEXT NOT NULL,split_hash TEXT NOT NULL,"
                + "seed INTEGER NOT NULL,sealed_token_hash TEXT NOT NULL DEFAULT '',"
                + "sealed_consumed INTEGER NOT NULL DEFAULT 0,output_hash TEXT NOT NULL DEFAULT '',"
                + "failure_code TEXT NOT NULL DEFAULT '',registered_at INTEGER NOT NULL,"
                + "started_at INTEGER NOT NULL DEFAULT 0,finished_at INTEGER NOT NULL DEFAULT 0,"
                + "record_hash TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r929_trial_family ON " + TABLE_TRIAL
                + "(family_id,status,registered_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUNDLE + "("
                + "bundle_hash TEXT PRIMARY KEY NOT NULL,bundle_id TEXT NOT NULL,game TEXT NOT NULL,"
                + "schema_hash TEXT NOT NULL,code_hash TEXT NOT NULL,manifest_hash TEXT NOT NULL,"
                + "key_id TEXT NOT NULL,signature_algorithm TEXT NOT NULL,signature_verified INTEGER NOT NULL,"
                + "artifact_count INTEGER NOT NULL,total_bytes INTEGER NOT NULL,created_issue TEXT NOT NULL,"
                + "earliest_issue TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,"
                + "verified_at INTEGER NOT NULL,activated_at INTEGER NOT NULL DEFAULT 0,"
                + "rollback_of TEXT NOT NULL DEFAULT '',row_hash TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_r929_bundle_one_active ON "
                + TABLE_BUNDLE + "(state) WHERE state='ACTIVE'");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CERTIFICATE + "("
                + "candidate_key TEXT PRIMARY KEY NOT NULL,consumer TEXT NOT NULL,namespace TEXT NOT NULL,"
                + "family_id TEXT NOT NULL,candidate_id TEXT NOT NULL,structural_hash TEXT NOT NULL,"
                + "evidence_hash TEXT NOT NULL,level TEXT NOT NULL,state TEXT NOT NULL,"
                + "bundle_hash TEXT NOT NULL,schema_hash TEXT NOT NULL,atom_graph_hash TEXT NOT NULL,"
                + "model_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,calibration_hash TEXT NOT NULL,"
                + "domain_hash TEXT NOT NULL,trial_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,"
                + "code_hash TEXT NOT NULL,valid_from_issue TEXT NOT NULL,valid_until_issue TEXT NOT NULL,"
                + "maximum_weight REAL NOT NULL,all_gates_pass INTEGER NOT NULL,gate_hash TEXT NOT NULL,"
                + "reason TEXT NOT NULL,certificate_hash TEXT NOT NULL UNIQUE,issued_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL,contract_revision TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_CERT_HISTORY+"(certificate_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,canonical TEXT NOT NULL,evidence_hash TEXT NOT NULL,issued_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_REVOCATION+"(certificate_hash TEXT PRIMARY KEY,reason TEXT NOT NULL,revoked_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_CERT_GATE+"(certificate_hash TEXT NOT NULL,gate_id TEXT NOT NULL,observed REAL NOT NULL,required REAL NOT NULL,pass INTEGER NOT NULL,source TEXT NOT NULL,source_hash TEXT NOT NULL,detail TEXT NOT NULL,gate_hash TEXT NOT NULL,PRIMARY KEY(certificate_hash,gate_id))");
        boolean revisionColumn=false;
        try(Cursor columns=db.rawQuery("PRAGMA table_info("+TABLE_CERTIFICATE+")",null)){while(columns.moveToNext())if("contract_revision".equals(columns.getString(1)))revisionColumn=true;}
        if(!revisionColumn)db.execSQL("ALTER TABLE "+TABLE_CERTIFICATE+" ADD COLUMN contract_revision TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r929_cert_lookup ON " + TABLE_CERTIFICATE
                + "(consumer,candidate_id,state,valid_from_issue,valid_until_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTRIBUTION + "("
                + "target_issue TEXT NOT NULL,consumer TEXT NOT NULL,candidate_id TEXT NOT NULL,"
                + "certificate_hash TEXT NOT NULL,bundle_hash TEXT NOT NULL,requested_weight REAL NOT NULL,"
                + "authorized_weight REAL NOT NULL,before_rank1_hash TEXT NOT NULL,"
                + "after_rank1_hash TEXT NOT NULL,input_hash TEXT NOT NULL,status TEXT NOT NULL,"
                + "reason TEXT NOT NULL,record_hash TEXT PRIMARY KEY NOT NULL,created_at INTEGER NOT NULL,"
                + "UNIQUE(target_issue,consumer,candidate_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r929_contribution_issue ON "
                + TABLE_CONTRIBUTION + "(target_issue,status,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + UnifiedResearchChoiceR932.TABLE
                + "(target_issue TEXT PRIMARY KEY,bundle_hash TEXT NOT NULL,consumer TEXT NOT NULL,"
                + "candidate_id TEXT NOT NULL,certificate_hash TEXT NOT NULL,roster_hash TEXT NOT NULL,"
                + "frozen_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EVENT + "("
                + "event_hash TEXT PRIMARY KEY NOT NULL,event_type TEXT NOT NULL,entity_id TEXT NOT NULL,"
                + "previous_hash TEXT NOT NULL,current_hash TEXT NOT NULL,status TEXT NOT NULL,"
                + "reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        TransactionSchemaCacheR941.markCommitted(db,"ResearchPlatformStoreR929");
    }

    public static boolean registerTrial(final DbStore store,
            final ExperimentProtocolR929.TrialRecord record) {
        if (record == null) throw new IllegalArgumentException("R929_TRIAL_RECORD_REQUIRED");
        ensureSchema(store);
        Boolean value = store.coordinatorR36().runAtomicWrite("R929_TRIAL_REGISTER",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = trialValues(record);
                        long inserted = db.insertWithOnConflict(TABLE_TRIAL, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE);
                        if (inserted >= 0L) return Boolean.TRUE;
                        Cursor cursor = db.rawQuery("SELECT identity_hash,record_hash FROM "
                                + TABLE_TRIAL + " WHERE trial_id=?",
                                new String[] {record.identity.trialId});
                        try {
                            if (!cursor.moveToFirst()) return Boolean.FALSE;
                            return Boolean.valueOf(record.identity.trialId.equals(cursor.getString(0))
                                    && record.recordHash.equals(cursor.getString(1)));
                        } finally { cursor.close(); }
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(value);
    }

    public static boolean compareAndSetTrial(final DbStore store, final String trialId,
            final String expectedRecordHash, final ExperimentProtocolR929.TrialRecord replacement) {
        if (replacement == null || !replacement.identity.trialId.equals(clean(trialId)))
            throw new IllegalArgumentException("R929_TRIAL_CAS_INPUT");
        ensureSchema(store);
        Integer rows = store.coordinatorR36().runAtomicWrite("R929_TRIAL_CAS",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        return Integer.valueOf(db.update(TABLE_TRIAL, trialValues(replacement),
                                "trial_id=? AND record_hash=?",
                                new String[] {trialId, requireHash(expectedRecordHash,
                                        "EXPECTED_RECORD_HASH")}));
                    }
                }, Integer.valueOf(0));
        return rows != null && rows.intValue() == 1;
    }

    static ContentValues trialValues(ExperimentProtocolR929.TrialRecord record) {
        ExperimentProtocolR929.TrialIdentity id = record.identity;
        ContentValues values = new ContentValues();
        values.put("trial_id", id.trialId);
        values.put("identity_hash", id.trialId);
        values.put("stage", id.stage.name());
        values.put("status", record.status.name());
        values.put("dataset_hash", id.datasetContentHash);
        values.put("schema_hash", id.schemaHash);
        values.put("source_hash", id.sourceCodeHash);
        values.put("hyper_hash", id.hyperParameterHash);
        values.put("family_id", id.statisticalFamilyId);
        values.put("structural_hash", id.candidateStructuralHash);
        values.put("split_hash", id.splitPlanHash);
        values.put("seed", id.seed);
        values.put("sealed_token_hash", record.sealedTokenHash);
        values.put("sealed_consumed", record.sealedTokenConsumed ? 1 : 0);
        values.put("output_hash", record.outputArtifactHash);
        values.put("failure_code", record.failureCode);
        values.put("registered_at", record.registeredAtMs);
        values.put("started_at", record.startedAtMs);
        values.put("finished_at", record.finishedAtMs);
        values.put("record_hash", record.recordHash);
        return values;
    }

    public static boolean stageBundle(final DbStore store,
            final CertificateBundleServiceR929.StagedBundle staged) {
        if (staged == null
                || staged.state != CertificateBundleServiceR929.BundleState.STAGED
                || staged.manifest == null)
            throw new IllegalArgumentException("R929_VERIFIED_STAGED_BUNDLE_REQUIRED");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R929_BUNDLE_STAGE",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        CertificateBundleServiceR929.BundleManifest manifest = staged.manifest;
                        long total = 0L;
                        for (CertificateBundleServiceR929.BundleArtifact artifact
                                : manifest.artifacts) total += artifact.sizeBytes;
                        ContentValues values = new ContentValues();
                        values.put("bundle_hash", manifest.bundleHash);
                        values.put("bundle_id", manifest.bundleId);
                        values.put("game", manifest.game);
                        values.put("schema_hash", manifest.schemaHash);
                        values.put("code_hash", manifest.codeHash);
                        values.put("manifest_hash", manifest.unsignedManifestHash);
                        values.put("key_id", manifest.signingKeyId);
                        values.put("signature_algorithm", manifest.signatureAlgorithm);
                        values.put("signature_verified", 1);
                        values.put("artifact_count", manifest.artifacts.size());
                        values.put("total_bytes", total);
                        values.put("created_issue", manifest.createdAtIssue);
                        values.put("earliest_issue", manifest.earliestUseIssue);
                        values.put("state", "STAGED");
                        values.put("reason", staged.reason);
                        values.put("verified_at", staged.verifiedAtMs);
                        values.put("activated_at", 0L);
                        values.put("rollback_of", "");
                        values.put("row_hash", staged.verificationHash);
                        return Boolean.valueOf(db.insertWithOnConflict(TABLE_BUNDLE, null,
                                values, SQLiteDatabase.CONFLICT_REPLACE) >= 0L);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    /** Atomically swaps only at a not-yet-pinned issue boundary. */
    public static boolean activateBundle(final DbStore store, final String bundleHash,
            final String targetIssue, final long nowMs) {
        final String bundle = requireHash(bundleHash, "ACTIVATE_BUNDLE_HASH");
        final String target = required(targetIssue, "ACTIVATE_TARGET");
        if (nowMs <= 0L) throw new IllegalArgumentException("R929_ACTIVATE_TIME");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R929_BUNDLE_ACTIVATE",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        Cursor check = db.rawQuery("SELECT earliest_issue,signature_verified,state "
                                + "FROM " + TABLE_BUNDLE + " WHERE bundle_hash=?",
                                new String[] {bundle});
                        String earliest;
                        try {
                            if (!check.moveToFirst()) return Boolean.FALSE;
                            earliest = check.getString(0);
                            if (check.getInt(1) != 1 || !"STAGED".equals(check.getString(2)))
                                return Boolean.FALSE;
                        } finally { check.close(); }
                        if (PitFeatureDagR929.compareIssues(target, earliest) < 0)
                            return Boolean.FALSE;
                        Cursor pinned = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CONTRIBUTION
                                + " WHERE target_issue=?", new String[] {target});
                        try {
                            if (pinned.moveToFirst() && pinned.getInt(0) > 0) return Boolean.FALSE;
                        } finally { pinned.close(); }
                        db.execSQL("UPDATE " + TABLE_BUNDLE
                                + " SET state='ROLLED_BACK',reason='REPLACED_AT_ISSUE_BOUNDARY',"
                                + "rollback_of=?,activated_at=? WHERE state='ACTIVE'",
                                new Object[] {bundle, Long.valueOf(nowMs)});
                        ContentValues activate = new ContentValues();
                        activate.put("state", "ACTIVE");
                        activate.put("reason", "ATOMIC_ACTIVATE:" + target);
                        activate.put("activated_at", nowMs);
                        int rows = db.update(TABLE_BUNDLE, activate,
                                "bundle_hash=? AND state='STAGED'", new String[] {bundle});
                        if (rows != 1) return Boolean.FALSE;
                        event(db, "BUNDLE_ACTIVATE", bundle, "", bundle,
                                "ACTIVE", target, nowMs);
                        return Boolean.TRUE;
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putCertificate(final DbStore store,
            final CertificateBundleServiceR929.Certificate certificate,
            final String evidenceHash) {
        if (certificate == null) throw new IllegalArgumentException("R929_CERT_REQUIRED");
        final String evidence = requireHash(evidenceHash, "CERT_EVIDENCE_HASH");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R929_CERTIFICATE_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        return Boolean.valueOf(putCertificate(db,certificate,evidence));
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    /** Publisher ledger and certificate commit together through the existing authority. */
    static boolean putCertificate(SQLiteDatabase db,
            CertificateBundleServiceR929.Certificate certificate,String evidence) {
        if(!db.inTransaction())throw new IllegalStateException("R932_CERTIFICATE_TRANSACTION_REQUIRED");
        if(ResearchGatePlanR967.installed().isCp3){String rejection=CertificateDependenciesR932.validateCp4Publication(db,certificate);if(!rejection.isEmpty())throw new IllegalStateException(rejection);}
                        Cursor bundle = db.rawQuery("SELECT state,signature_verified,schema_hash,code_hash "
                                + "FROM " + TABLE_BUNDLE + " WHERE bundle_hash=?",
                                new String[] {certificate.bundleHash});
                        try {
                            if (!bundle.moveToFirst() || !"ACTIVE".equals(bundle.getString(0))
                                    || bundle.getInt(1) != 1
                                    || !certificate.schemaHash.equals(bundle.getString(2))
                                    || (ResearchGatePlanR967.installed().isCp3
                                            ? !CalibrationRuntimeIdentityF04.numericHash().equals(certificate.codeHash)
                                                || !SourceManifestR815.SOURCE_TREE_SHA256.equals(bundle.getString(3))
                                            : !certificate.codeHash.equals(bundle.getString(3))))
                                return false;
                        } finally { bundle.close(); }
                        try(Cursor previous=db.rawQuery("SELECT certificate_hash,issued_at FROM "+TABLE_CERTIFICATE+" WHERE candidate_key=?",new String[]{certificate.candidate.keyHash})){
                            if(previous.moveToFirst()&&!previous.getString(0).equals(certificate.certificateHash)&&previous.getLong(1)>=certificate.issuedAtMs)return false;
                        }
                        try(Cursor revoked=db.rawQuery("SELECT 1 FROM "+TABLE_REVOCATION+" WHERE certificate_hash=?",new String[]{certificate.certificateHash})){if(revoked.moveToFirst())return false;}
                        db.execSQL("INSERT OR IGNORE INTO "+TABLE_CERT_HISTORY+" VALUES(?,?,?,?,?)",new Object[]{certificate.certificateHash,certificate.candidate.keyHash,certificate.canonicalWithoutHash(),evidence,certificate.issuedAtMs});
                        boolean gatesPass = certificate.authorizedWeight(certificate.validFromIssue)>0;
                        StringBuilder gateHashes = new StringBuilder();
                        for (CertificateBundleServiceR929.GateObservation gate
                                : certificate.gates) {
                            if (!gate.pass) gatesPass = false;
                            gateHashes.append(gate.gateHash);
                            db.execSQL("INSERT OR IGNORE INTO "+TABLE_CERT_GATE+" VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{certificate.certificateHash,gate.gateId,gate.observed,gate.required,gate.pass?1:0,gate.source,gate.sourceHash,gate.detail,gate.gateHash});
                        }
                        ContentValues values = new ContentValues();
                        values.put("contract_revision",currentCertificateContractCp4());
                        values.put("candidate_key", certificate.candidate.keyHash);
                        values.put("consumer", certificate.candidate.consumer.name());
                        values.put("namespace", certificate.candidate.namespace);
                        values.put("family_id", certificate.candidate.familyId);
                        values.put("candidate_id", certificate.candidate.candidateId);
                        values.put("structural_hash", certificate.candidate.structuralHash);
                        values.put("evidence_hash", evidence);
                        values.put("level", certificate.level.name());
                        values.put("state", certificate.state.name());
                        values.put("bundle_hash", certificate.bundleHash);
                        values.put("schema_hash", certificate.schemaHash);
                        values.put("atom_graph_hash", certificate.atomGraphHash);
                        values.put("model_hash", certificate.modelHash);
                        values.put("policy_hash", certificate.policyHash);
                        values.put("calibration_hash", certificate.calibrationHash);
                        values.put("domain_hash", certificate.domainHash);
                        values.put("trial_hash", certificate.trialRegistryHash);
                        values.put("selection_hash", certificate.selectionHash);
                        values.put("code_hash", certificate.codeHash);
                        values.put("valid_from_issue", certificate.validFromIssue);
                        values.put("valid_until_issue", certificate.validUntilIssue);
                        values.put("maximum_weight", Math.min(certificate.maximumWeight,CertificateBundleServiceR929.validatedInfluenceCap(certificate.gates)));
                        values.put("all_gates_pass", gatesPass ? 1 : 0);
                        values.put("gate_hash", PitFeatureDagR929.sha256(gateHashes.toString()));
                        values.put("reason", certificate.reason);
                        values.put("certificate_hash", certificate.certificateHash);
                        values.put("issued_at", certificate.issuedAtMs);
                        values.put("updated_at", System.currentTimeMillis());
                        return db.insertWithOnConflict(TABLE_CERTIFICATE, null,
                                values, SQLiteDatabase.CONFLICT_REPLACE) >= 0L;
    }

    /** Called only with the loader's verified snapshot inside its pointer-CAS transaction. */
    static void activateRuntimeBundle(SQLiteDatabase db,ModelBundleRuntimeR932.Snapshot snapshot,String issue,long at) {
        if(!db.inTransaction())throw new IllegalStateException("R932_BUNDLE_REGISTRY_TRANSACTION");
        createSchema(db);
        String schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256;
        try(Cursor current=db.rawQuery("SELECT bundle_hash,schema_hash,code_hash FROM "+TABLE_BUNDLE+" WHERE state='ACTIVE'",null)){
            if(current.moveToFirst()&&snapshot.hash.equals(current.getString(0))
                    &&schema.equals(current.getString(1))&&code.equals(current.getString(2)))return;
        }
        boolean numericMode=ResearchGatePlanR967.installed().isCp3;
        String marked="contract_revision='"+currentCertificateContractCp4()+"' AND EXISTS(SELECT 1 FROM "+TABLE_CERT_GATE+" sg WHERE sg.certificate_hash="+TABLE_CERTIFICATE+".certificate_hash AND sg.gate_id='F04_NUMERIC_SCOPE' AND sg.source='"+CalibrationRuntimeIdentityF04.REVISION+"' AND sg.observed=1 AND sg.required=1 AND sg.pass=1 AND sg.source_hash="+TABLE_CERTIFICATE+".code_hash)";
        String drift=numericMode?"bundle_hash<>? OR schema_hash<>? OR (contract_revision='"+currentCertificateContractCp4()+"' AND NOT ("+marked+")) OR (("+marked+") AND code_hash<>?) OR (contract_revision<>'"+currentCertificateContractCp4()+"' AND code_hash<>?)":"bundle_hash<>? OR schema_hash<>? OR code_hash<>?";
        String[] readArgs=numericMode?new String[]{snapshot.hash,schema,CalibrationRuntimeIdentityF04.numericHash(),code}:new String[]{snapshot.hash,schema,code};
        try(Cursor old=db.rawQuery("SELECT certificate_hash FROM "+TABLE_CERTIFICATE+" WHERE state='ACTIVE' AND ("+drift+")",readArgs)){
            while(old.moveToNext())db.execSQL("INSERT OR IGNORE INTO "+TABLE_REVOCATION+" VALUES(?,?,?)",new Object[]{old.getString(0),"ACTIVE_MODEL_BUNDLE_CHANGED",at});
        }
        Object[] writeArgs=numericMode?new Object[]{at,snapshot.hash,schema,CalibrationRuntimeIdentityF04.numericHash(),code}:new Object[]{at,snapshot.hash,schema,code};
        db.execSQL("UPDATE "+TABLE_CERTIFICATE+" SET state='REVOKED',reason='ACTIVE_MODEL_BUNDLE_CHANGED',updated_at=? WHERE state='ACTIVE' AND ("+drift+")",writeArgs);
        db.execSQL("UPDATE "+TABLE_BUNDLE+" SET state='ROLLED_BACK',reason='NEW_VERIFIED_RUNTIME_BUNDLE' WHERE state='ACTIVE'");
        String row=V51ChecksumEngine.sha256(snapshot.hash+'|'+schema+'|'+code+'|'+issue);
        db.execSQL("INSERT OR REPLACE INTO "+TABLE_BUNDLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{snapshot.hash,"R932_RUNTIME_"+snapshot.hash,V51Repository.GAME_CODE,schema,code,snapshot.hash,
                snapshot.artifactHashes.isEmpty()?"INSTALLED_APK_SEED":"PINNED_MODEL_OWNER","SHA256withRSA",1,6,snapshot.payloadBytes,
                snapshot.trainedUntil.length()==0?issue:snapshot.trainedUntil,issue,"ACTIVE","EXACT_VERIFIED_SIX_COMPONENT_SNAPSHOT",at,at,"",row});
    }

    public static boolean revoke(final DbStore store,final String certificateHash,final String reason) {
        ensureSchema(store);requireHash(certificateHash,"REVOKE_HASH");required(reason,"REVOKE_REASON");
        return store.coordinatorR36().runAtomicWrite("R932_CERTIFICATE_REVOKE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            revokeInTransaction(db,certificateHash,reason,System.currentTimeMillis());
        }});
    }

    /** Reuse the one revocation owner when observation recovery and revocation must commit together. */
    static void revokeInTransaction(SQLiteDatabase db,String certificateHash,String reason,long at){
        if(!db.inTransaction())throw new IllegalStateException("R932_CERTIFICATE_REVOCATION_TRANSACTION");
        requireHash(certificateHash,"REVOKE_HASH");required(reason,"REVOKE_REASON");
        db.execSQL("INSERT OR IGNORE INTO "+TABLE_REVOCATION+" VALUES(?,?,?)",new Object[]{certificateHash,reason,at});
        db.execSQL("UPDATE "+TABLE_CERTIFICATE+" SET state='REVOKED',reason=?,updated_at=? WHERE certificate_hash=?",new Object[]{reason,at,certificateHash});
    }

    /** Existing maintenance lane appends revocations; production authorization itself stays read-only. */
    public static void revokeStaleDependencies(DbStore store) {
        ensureSchema(store);String target=store.getState("r932_bundle_capture_issue","");
        if(target.length()==0)return;
        String active=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");
        if(active.length()!=64)active=V51ArtifactBundleManifest.BUNDLE_HASH;
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(active,target,System.currentTimeMillis())){
            java.util.List<String[]> revoke=new java.util.ArrayList<String[]>();
            String cursor=store.getState("r932_certificate_audit_cursor","");String next="";
            try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT certificate_hash,bundle_hash,schema_hash,selection_hash,code_hash,domain_hash,atom_graph_hash,model_hash,policy_hash,calibration_hash,trial_hash,structural_hash,family_id,issued_at,candidate_key FROM "+TABLE_CERTIFICATE+" WHERE state='ACTIVE' AND contract_revision='"+currentCertificateContractCp4()+"' AND candidate_key>? ORDER BY candidate_key LIMIT 16",new String[]{cursor})){
                while(c.moveToNext()){
                    ResearchWorkBudgetR927.checkpoint();next=c.getString(14);String reason="";
                    if(!active.equals(c.getString(1))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(c.getString(2))||!CertificateDependenciesR932.selectionHash().equals(c.getString(3))||!(ResearchGatePlanR967.installed().isCp3?CalibrationRuntimeIdentityF04.numericHash():SourceManifestR815.SOURCE_TREE_SHA256).equals(c.getString(4))||!DomainRouterR929.installedSchemaHash().equals(c.getString(5)))reason="FROZEN_BUNDLE_SCHEMA_SELECTION_CODE_DOMAIN_CHANGED";
                    if(reason.length()==0)reason=CertificateDependenciesR932.check(store,c.getString(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getString(12),c.getLong(13));
                    if(reason.length()>0)revoke.add(new String[]{c.getString(0),reason});
                }
            }
            for(String[] row:revoke){ResearchWorkBudgetR927.checkpoint();if(!revoke(store,row[0],row[1]))throw new IllegalStateException("R932_DEPENDENCY_REVOCATION_FAILED");}
            store.setState("r932_certificate_audit_cursor",next);
        }
    }

    public static Authorization authorize(DbStore store,CertificateBundleServiceR929.Consumer consumer,
            String candidateId,String structuralHash,String evidenceHash,String targetIssue,double requestedWeight) {
        if(store==null||consumer==null)return Authorization.denied("AUTHORIZATION_INPUT_INVALID");
        try(Cursor c=store.readable().rawQuery("SELECT evidence_hash FROM "+TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND structural_hash=?",new String[]{consumer.name(),candidateId,structuralHash})){
            if(!c.moveToFirst()||!clean(evidenceHash).equals(c.getString(0))||c.moveToNext())return Authorization.denied("FROZEN_EVIDENCE_IDENTITY_MISMATCH");
        }catch(RuntimeException e){if(e instanceof ResearchWorkBudgetR927.Yield)throw e;return Authorization.denied("FROZEN_EVIDENCE_UNAVAILABLE");}
        return authorizeCurrent(store,consumer,candidateId,structuralHash,targetIssue,requestedWeight);
    }

    private static Authorization authorizeStored(DbStore store,
            CertificateBundleServiceR929.Consumer consumer, String candidateId,
            String structuralHash, String evidenceHash, String targetIssue,
            double requestedWeight) {
        if (store == null || consumer == null || !finite(requestedWeight)
                || requestedWeight < 0.0d || requestedWeight > 0.30d)
            return Authorization.denied("AUTHORIZATION_INPUT_INVALID");
        String id = clean(candidateId);
        String structural = clean(structuralHash);
        String evidence = clean(evidenceHash);
        String target = clean(targetIssue);
        if (id.length() == 0 || target.length() == 0 || !hash(structural) || !hash(evidence))
            return Authorization.denied("AUTHORIZATION_IDENTITY_INVALID");
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery("SELECT c.level,c.state,c.bundle_hash,"
                    + "c.maximum_weight,c.all_gates_pass,c.certificate_hash,c.reason,"
                    + "b.state,b.signature_verified,c.valid_from_issue,c.valid_until_issue "
                    + "FROM " + TABLE_CERTIFICATE + " c JOIN " + TABLE_BUNDLE
                    + " b ON b.bundle_hash=c.bundle_hash WHERE c.consumer=? AND c.candidate_id=? "
                    + "AND c.structural_hash=? AND c.evidence_hash=? AND NOT EXISTS(SELECT 1 FROM research_certificate_revocation_r932 rv WHERE rv.certificate_hash=c.certificate_hash) AND c.contract_revision='"+currentCertificateContractCp4()+"' ORDER BY c.updated_at DESC LIMIT 2",
                    new String[] {consumer.name(), id, structural, evidence});
            if (!cursor.moveToFirst()) return Authorization.denied("NO_UNIFIED_CERTIFICATE");
            String level = cursor.getString(0);
            String state = cursor.getString(1);
            String bundle = cursor.getString(2);
            double maximum = cursor.getDouble(3);
            boolean gatesPass = cursor.getInt(4) == 1;
            String certificateHash = cursor.getString(5);
            String reason = cursor.getString(6);
            String bundleState = cursor.getString(7);
            boolean signature = cursor.getInt(8) == 1;
            String validFrom = cursor.getString(9);
            String validUntil = cursor.getString(10);
            if (cursor.moveToNext()) return Authorization.denied("CERTIFICATE_CARDINALITY_GT1");
            if (!"ACTIVE".equals(state) || !"ACTIVE".equals(bundleState) || !signature)
                return Authorization.denied("CERTIFICATE_OR_BUNDLE_NOT_ACTIVE");
            if (!gatesPass) return Authorization.denied("CERTIFICATE_GATE_FAILED");
            if (PitFeatureDagR929.compareIssues(target, validFrom) < 0
                    || (clean(validUntil).length() > 0
                            && PitFeatureDagR929.compareIssues(target, validUntil) > 0))
                return Authorization.denied("CERTIFICATE_OUTSIDE_ISSUE_RANGE");
            double levelCap = "L6_CONTROLLED_INFLUENCE".equals(level) ? 0.05d
                    : "L7_CHAMPION".equals(level) ? 0.30d : 0.0d;
            double allowed = Math.min(levelCap, Math.max(0.0d, maximum));
            if (allowed <= 0.0d || requestedWeight <= 0.0d)
                return Authorization.denied("SHADOW_ONLY_LEVEL");
            double granted = Math.min(requestedWeight, allowed);
            return new Authorization(true, granted, level, certificateHash, bundle,
                    "UNIFIED_CERTIFICATE_AUTHORIZED|" + reason);
        } catch (Throwable failure) {
            if(failure instanceof ResearchWorkBudgetR927.Yield)throw (ResearchWorkBudgetR927.Yield)failure;
            return Authorization.denied("CERTIFICATE_READ_ERROR_"
                    + failure.getClass().getSimpleName());
        } finally { if (cursor != null) cursor.close(); }
    }

    public static Authorization authorizeCurrent(DbStore store,CertificateBundleServiceR929.Consumer consumer,
            String id,String structural,String target,double requested){
        if(store==null||consumer==null)return Authorization.denied("AUTHORIZATION_INPUT_INVALID");
        if(consumer==CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM&&id!=null&&id.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU1.PREFIX)){
            try{com.ethk3.novaonepro.strategyv2.CertificateSU1.Authority a=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.authority(store,id,structural,target,"");
                return a==null||!Double.isFinite(requested)||requested<=0?Authorization.denied("SU1_EXACT_CERTIFICATE_REQUIRED"):new Authorization(true,Math.min(.05,requested),"L6",a.certificate,ModelBundleRuntimeR932.current().hash,"SU1_VERIFIED_EXACT_K3");
            }catch(RuntimeException unavailable){return Authorization.denied("SU1_AUTHORITY_UNAVAILABLE:"+unavailable.getClass().getSimpleName());}
        }
        String prefix=consumer.name().equals("INVENTED_ALGORITHM")?"algorithm":consumer.name().equals("GENESIS_ASSET")?"genesis":"formula";
        try {
            if(ResearchLifecycleR932.managed(store.readable(),prefix+":"+id)){
                try(Cursor life=store.readable().rawQuery("SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",new String[]{prefix+":"+id})){
                    if(life.moveToFirst()&&java.util.Arrays.asList("QUALITY_PRUNED","RESOURCE_RETIRED","QUARANTINED","PAUSED").contains(life.getString(0)))return Authorization.denied("LIFECYCLE_CLOSED_OR_PAUSED");
                }
            }
        } catch(ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
        catch(RuntimeException lifecycleUnavailable) {
            return Authorization.denied("LIFECYCLE_UNAVAILABLE_"+lifecycleUnavailable.getClass().getSimpleName());
        }
        try(Cursor c=store.readable().rawQuery("SELECT evidence_hash,bundle_hash,schema_hash,selection_hash,code_hash,domain_hash,issued_at,atom_graph_hash,model_hash,policy_hash,calibration_hash,trial_hash,family_id FROM "
                +TABLE_CERTIFICATE+" WHERE consumer=? AND candidate_id=? AND structural_hash=? AND contract_revision='"+currentCertificateContractCp4()+"' ORDER BY issued_at DESC LIMIT 2",
                new String[]{consumer.name(),id,structural})){
            if(!c.moveToFirst())return Authorization.denied("LEGACY_REVIEW_NO_UNIFIED_CERTIFICATE");
            String evidence=c.getString(0);
            if(!target.equals(ModelBundleRuntimeR932.currentTarget())||c.getLong(6)>=ModelBundleRuntimeR932.capturedAt())
                return Authorization.denied("CERTIFICATE_NOT_FROZEN_BEFORE_REQUEST");
            if(!ModelBundleRuntimeR932.current().hash.equals(c.getString(1))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(c.getString(2))
                    ||!CertificateDependenciesR932.selectionHash().equals(c.getString(3))||!(ResearchGatePlanR967.installed().isCp3?CalibrationRuntimeIdentityF04.numericHash():SourceManifestR815.SOURCE_TREE_SHA256).equals(c.getString(4))
                    ||!DomainRouterR929.installedSchemaHash().equals(c.getString(5)))return Authorization.denied("CERTIFICATE_DEPENDENCY_DRIFT");
            String dependency=CertificateDependenciesR932.check(store,c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),structural,c.getString(12),c.getLong(6));
            if(dependency.length()>0)return Authorization.denied(dependency);
            if(c.moveToNext())return Authorization.denied("CERTIFICATE_CARDINALITY_GT1");
            return authorizeStored(store,consumer,id,structural,evidence,target,requested);
        }catch(RuntimeException e){if(e instanceof ResearchWorkBudgetR927.Yield)throw e;return Authorization.denied("CERTIFICATE_UNAVAILABLE_"+e.getClass().getSimpleName());}
    }

    /**
     * Read-only projection of the newest unified certificate stage. Unlike authorizeCurrent(),
     * this method deliberately does not require a positive influence weight, so L4/L5 research
     * certificates remain visible to lifecycle/diagnostics without accidentally granting REAL
     * or ranking authority. Dependency, validity-window, revocation and freeze-before-request
     * checks are still identical in spirit to production authorization.
     */
    /**
     * R954 research-stage validation is intentionally different from production influence
     * authorization. L4/L5 have zero legal weight, so all_gates_pass is necessarily false for
     * those levels (authorizedWeight() returns zero below L6). Requiring that field made a real
     * L4/L5 certificate impossible to project. Validate the immutable gate observations needed
     * to have legitimately reached each research stage instead; L6/L7 still require the full
     * all_gates_pass authority bit in currentStage() and authorizeCurrent().
     */
    private static boolean researchStageGateContract(SQLiteDatabase db,String certificateHash,String level) {
        if (!("L4_CHALLENGER".equals(level)||"L5_SHADOW".equals(level))) return true;
        HashMap<String,double[]> gate=new HashMap<String,double[]>();
        try(Cursor c=db.rawQuery("SELECT gate_id,observed,pass FROM "+TABLE_CERT_GATE+" WHERE certificate_hash=?",new String[]{certificateHash})) {
            while(c.moveToNext()) gate.put(clean(c.getString(0)),new double[]{c.getDouble(1),c.getInt(2)});
        }
        String[] base={"PIT","DETERMINISM","PARITY","FUTURE_N","PAIRED_N"};
        for(String id:base){double[] value=gate.get(id);if(value==null||value[1]!=1.0d)return false;}
        if ("L4_CHALLENGER".equals(level)) return true;
        // L5 requires one independent promotion window, while the influence gate intentionally
        // asks for two; therefore inspect the frozen observed value rather than its L6 pass bit.
        double[] independent=gate.get("INDEPENDENT_WINDOWS");
        if(independent==null||independent[0]<1.0d)return false;
        String[] quality={"HIT_RATE","COVERAGE","ECE","DOMAIN_SUPPORT","RESOURCE"};
        for(String id:quality){double[] value=gate.get(id);if(value==null||value[1]!=1.0d)return false;}
        return true;
    }

    public static CertificateStage currentStage(DbStore store,
            CertificateBundleServiceR929.Consumer consumer, String id,
            String structural, String target) {
        if (store == null || consumer == null || !hash(clean(structural))
                || clean(id).length() == 0 || !TargetIssueResolver.isValid(clean(target)))
            return CertificateStage.invalid("STAGE_INPUT_INVALID");
        Cursor c = null;
        try {
            c = store.readable().rawQuery("SELECT c.level,c.state,c.bundle_hash,c.certificate_hash,"
                    + "c.reason,c.valid_from_issue,c.valid_until_issue,c.issued_at,c.schema_hash,"
                    + "c.selection_hash,c.code_hash,c.domain_hash,c.atom_graph_hash,c.model_hash,"
                    + "c.policy_hash,c.calibration_hash,c.trial_hash,c.family_id,b.state,b.signature_verified,c.all_gates_pass "
                    + "FROM " + TABLE_CERTIFICATE + " c JOIN " + TABLE_BUNDLE
                    + " b ON b.bundle_hash=c.bundle_hash WHERE c.consumer=? AND c.candidate_id=? "
                    + "AND c.structural_hash=? AND c.contract_revision='"+currentCertificateContractCp4()+"' "
                    + "AND NOT EXISTS(SELECT 1 FROM " + TABLE_REVOCATION
                    + " rv WHERE rv.certificate_hash=c.certificate_hash) "
                    + "ORDER BY c.issued_at DESC LIMIT 2",
                    new String[]{consumer.name(), clean(id), clean(structural)});
            if (!c.moveToFirst()) return CertificateStage.invalid("NO_UNIFIED_CERTIFICATE");
            String level=clean(c.getString(0)), state=clean(c.getString(1)), bundle=clean(c.getString(2));
            String certificate=clean(c.getString(3)), reason=clean(c.getString(4));
            String validFrom=clean(c.getString(5)), validUntil=clean(c.getString(6));
            long issuedAt=c.getLong(7);
            String schema=clean(c.getString(8)), selection=clean(c.getString(9)), code=clean(c.getString(10)), domain=clean(c.getString(11));
            String atom=clean(c.getString(12)), model=clean(c.getString(13)), policy=clean(c.getString(14)), calibration=clean(c.getString(15)), trial=clean(c.getString(16)), family=clean(c.getString(17));
            String bundleState=clean(c.getString(18)); boolean signature=c.getInt(19)==1; boolean gatesPass=c.getInt(20)==1;
            if (c.moveToNext()) return CertificateStage.invalid("CERTIFICATE_CARDINALITY_GT1");
            if (!"ACTIVE".equals(state)||!"ACTIVE".equals(bundleState)||!signature)
                return CertificateStage.invalid("CERTIFICATE_OR_BUNDLE_NOT_ACTIVE");
            boolean researchOnly="L4_CHALLENGER".equals(level)||"L5_SHADOW".equals(level);
            if (researchOnly) {
                if(!researchStageGateContract(store.readable(),certificate,level))
                    return CertificateStage.invalid("CERTIFICATE_RESEARCH_STAGE_GATE_FAILED");
            } else if (("L6_CONTROLLED_INFLUENCE".equals(level)||"L7_CHAMPION".equals(level))&&!gatesPass) {
                return CertificateStage.invalid("CERTIFICATE_GATE_FAILED");
            }
            if (!target.equals(ModelBundleRuntimeR932.currentTarget()) || issuedAt>=ModelBundleRuntimeR932.capturedAt())
                return CertificateStage.invalid("CERTIFICATE_NOT_FROZEN_BEFORE_REQUEST");
            if (PitFeatureDagR929.compareIssues(target,validFrom)<0 || (validUntil.length()>0&&PitFeatureDagR929.compareIssues(target,validUntil)>0))
                return CertificateStage.invalid("CERTIFICATE_OUTSIDE_ISSUE_RANGE");
            if (!ModelBundleRuntimeR932.current().hash.equals(bundle)
                    || !V51ActionMetaFeatureEngine.featureManifestHash().equals(schema)
                    || !CertificateDependenciesR932.selectionHash().equals(selection)
                    || !(ResearchGatePlanR967.installed().isCp3?CalibrationRuntimeIdentityF04.numericHash():SourceManifestR815.SOURCE_TREE_SHA256).equals(code)
                    || !DomainRouterR929.installedSchemaHash().equals(domain))
                return CertificateStage.invalid("CERTIFICATE_DEPENDENCY_DRIFT");
            String dependency=CertificateDependenciesR932.check(store,atom,model,policy,calibration,trial,structural,family,issuedAt);
            if (dependency.length()>0) return CertificateStage.invalid(dependency);
            return new CertificateStage(true,level,certificate,bundle,reason);
        } catch (RuntimeException failure) {
            if (failure instanceof ResearchWorkBudgetR927.Yield) throw failure;
            return CertificateStage.invalid("CERTIFICATE_STAGE_READ_ERROR_"+failure.getClass().getSimpleName());
        } finally { if (c != null) c.close(); }
    }

    public static boolean recordContribution(final DbStore store, final String targetIssue,
            final CertificateBundleServiceR929.Consumer consumer, final String candidateId,
            final Authorization authorization, final double requestedWeight,
            final String beforeRankOneHash, final String afterRankOneHash,
            final String inputHash, final String status, final String reason) {
        if (store == null || consumer == null || authorization == null)
            return false;
        ensureSchema(store);
        final String target = required(targetIssue, "CONTRIBUTION_TARGET");
        final String id = required(candidateId, "CONTRIBUTION_CANDIDATE");
        final String before = requireHash(beforeRankOneHash, "CONTRIBUTION_BEFORE_HASH");
        final String after = requireHash(afterRankOneHash, "CONTRIBUTION_AFTER_HASH");
        final String input = requireHash(inputHash, "CONTRIBUTION_INPUT_HASH");
        final String recordHash = PitFeatureDagR929.sha256(target + '|' + consumer.name() + '|'
                + id + '|' + authorization.certificateHash + '|' + authorization.bundleHash
                + '|' + requestedWeight + '|' + authorization.grantedWeight + '|' + before
                + '|' + after + '|' + input + '|' + clean(status) + '|' + clean(reason));
        Boolean result = store.coordinatorR36().runAtomicWrite("R929_CONTRIBUTION_RECORD",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = new ContentValues();
                        values.put("target_issue", target);
                        values.put("consumer", consumer.name());
                        values.put("candidate_id", id);
                        values.put("certificate_hash", authorization.certificateHash);
                        values.put("bundle_hash", authorization.bundleHash);
                        values.put("requested_weight", requestedWeight);
                        values.put("authorized_weight", authorization.grantedWeight);
                        values.put("before_rank1_hash", before);
                        values.put("after_rank1_hash", after);
                        values.put("input_hash", input);
                        values.put("status", clean(status));
                        values.put("reason", clean(reason));
                        values.put("record_hash", recordHash);
                        values.put("created_at", System.currentTimeMillis());
                        return Boolean.valueOf(db.insertWithOnConflict(TABLE_CONTRIBUTION, null,
                                values, SQLiteDatabase.CONFLICT_IGNORE) >= 0L);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static final class CertificateStage {
        public final boolean valid;
        public final String level,certificateHash,bundleHash,reason;
        CertificateStage(boolean valid,String level,String certificateHash,String bundleHash,String reason){
            this.valid=valid;this.level=clean(level);this.certificateHash=clean(certificateHash);
            this.bundleHash=clean(bundleHash);this.reason=clean(reason);
        }
        public static CertificateStage invalid(String reason){return new CertificateStage(false,"","","",reason);}
        public boolean is(String expected){return valid&&clean(expected).equals(level);}
    }

    public static final class Authorization {
        public final boolean authorized;
        public final double grantedWeight;
        public final String level;
        public final String certificateHash;
        public final String bundleHash;
        public final String reason;
        public final boolean mayCreateReal;
        public final String authorizationHash;

        Authorization(boolean authorized, double grantedWeight, String level,
                String certificateHash, String bundleHash, String reason) {
            this.authorized = authorized;
            this.grantedWeight = authorized ? grantedWeight : 0.0d;
            this.level = clean(level);
            this.certificateHash = clean(certificateHash);
            this.bundleHash = clean(bundleHash);
            this.reason = clean(reason);
            this.mayCreateReal = false;
            this.authorizationHash = PitFeatureDagR929.sha256((authorized ? 1 : 0) + "|"
                    + this.grantedWeight + '|' + this.level + '|' + this.certificateHash + '|'
                    + this.bundleHash + '|' + this.reason + "|directREAL=0");
        }

        public static Authorization denied(String reason) {
            return new Authorization(false, 0.0d, "", "", "", reason);
        }
    }

    private static void event(SQLiteDatabase db, String type, String entity,
            String previousHash, String currentHash, String status, String reason, long now) {
        String hash = PitFeatureDagR929.sha256(type + '|' + entity + '|' + previousHash + '|'
                + currentHash + '|' + status + '|' + reason + '|' + now);
        ContentValues values = new ContentValues();
        values.put("event_hash", hash);
        values.put("event_type", type);
        values.put("entity_id", entity);
        values.put("previous_hash", clean(previousHash));
        values.put("current_hash", clean(currentHash));
        values.put("status", status);
        values.put("reason", reason);
        values.put("created_at", now);
        db.insertWithOnConflict(TABLE_EVENT, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R929_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (!hash(clean)) throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        return clean;
    }

    private static String currentCertificateContractCp4(){return ResearchGatePlanR967.installed().isCp3?"CP4_REQUIRED_GATES_V1":"R932_REQUIRED_GATES_V1";}
}
