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

    /** R31 observation schema has its own journal; it cannot invalidate Critical startup. */
    public static Contract r31ObservationContract() {
        return new Contract("RESEARCH_R31_OBSERVATION",
                com.ethk3.novaonepro.strategyv2.SchemaR27.R31_OBSERVATION_REVISION,
                new String[] {"r31_runtime_fault", "r31_runtime_fault_order", "r31_telemetry_counter",
                        "r31_task_observation", "r31_task_control", "r31_migration"});
    }

    /** Called once by the existing R26_RUNTIME_SCHEMA writer, never by a business receipt. */
    public static void migrateR31Observations(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31ObservationContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_OBSERVATION_V" + contract.revision,
                    com.ethk3.novaonepro.strategyv2.SchemaR27::ensureR31Observations);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_OBSERVATION_MIGRATION_FAILED", failure);
        }
    }

    public static Contract r31WriteContract() {
        return new Contract("RESEARCH_R31_WRITE",
                com.ethk3.novaonepro.strategyv2.SchemaR27.R31_WRITE_REVISION,
                new String[] {"r31_commit_receipt", "r31_commit_receipt_task", "r31_task_lease",
                        "r31_recovery_clock", "r30_cache_rebuild"});
    }

    /** Lease columns and durable commit receipts change together under the existing schema owner. */
    public static void migrateR31WriteProtocol(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31WriteContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_WRITE_TASK_PROTOCOL_V1",
                    com.ethk3.novaonepro.strategyv2.SchemaR27::ensureR31WriteProtocol);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_WRITE_PROTOCOL_MIGRATION_FAILED", failure);
        }
    }

    /** Complete atom/molecule sets are published independently of the legacy table layout. */
    public static Contract r31AtomPublicationContract() {
        return new Contract("RESEARCH_R31_ATOM_PUBLICATION", 1, new String[] {
                "r31_atom_result_run", "r31_atom_result_chunk", "r31_atom_result_active",
                "r31_atom_score_stage", "r31_atom_contribution_stage", "r31_molecule_stage",
                "r31_atom_run_publication", "r31_atom_score_identity", "r31_atom_contribution_identity",
                "r31_molecule_identity", "r31_atom_score_visible", "r31_molecule_visible",
                "r31_atom_contribution_visible", "r31_atom_score_stage_insert_guard",
                "r31_atom_score_stage_no_update", "r31_atom_score_stage_protect_active",
                "r31_atom_contribution_stage_insert_guard", "r31_atom_contribution_stage_no_update",
                "r31_atom_contribution_stage_protect_active", "r31_molecule_stage_insert_guard",
                "r31_molecule_stage_no_update", "r31_molecule_stage_protect_active",
                "r31_atom_result_chunk_insert_guard", "r31_atom_result_chunk_no_update",
                "r31_atom_result_chunk_protect_active", "r31_atom_run_identity",
                "r31_atom_run_protect_active", "r31_atom_active_insert_guard", "r31_atom_active_update_guard"
        });
    }

    /** The existing startup migration writer is the sole schema owner. */
    public static void migrateR31AtomPublication(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31AtomPublicationContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_ATOM_PUBLICATION_V1",
                    com.ethk3.novaonepro.strategyv2.AtomPublicationStoreR31::ensureSchemaR31);
            com.ethk3.novaonepro.strategyv2.AtomPublicationStoreR31.requireReadyR31(db);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_ATOM_PUBLICATION_MIGRATION_FAILED", failure);
        }
    }

    /** R31-B10-C01: typed mainline choice authority (one writer: the certificate transaction). */
    public static Contract r31MainlineChoiceContract() {
        return new Contract(com.ethk3.novaonepro.strategyv2.MainlineChoiceStoreR31.DOMAIN, 1, com.ethk3.novaonepro.strategyv2.MainlineChoiceStoreR31.OBJECTS);
    }

    private static final String[] R31_MAINLINE_LOOKUP_INDEXES={
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_choice_current ON r31_mainline_choice(selection_fence DESC,created_at DESC) WHERE status IN ('ACTIVE','FALLBACK')",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_choice_fence ON r31_mainline_choice(selection_fence)"
    };
    public static boolean isR31MainlineChoiceLookupIndex(String sql){for(String known:R31_MAINLINE_LOOKUP_INDEXES)if(known.equals(sql))return true;return false;}
    private static String mainlineLookupNameR31(String sql){return sql.substring("CREATE INDEX IF NOT EXISTS ".length()).split(" ",2)[0];}
    private static String mainlineLookupFingerprintR31(Contract contract,java.util.List<String> base,int mask){
        java.util.ArrayList<String> rows=new java.util.ArrayList<>(base);
        for(int i=0;i<R31_MAINLINE_LOOKUP_INDEXES.length;i++)if((mask&(1<<i))!=0){String ddl=R31_MAINLINE_LOOKUP_INDEXES[i];rows.add("index|"+mainlineLookupNameR31(ddl)+"|r31_mainline_choice|"+ddl.replaceFirst(" IF NOT EXISTS "," "));}
        java.util.Collections.sort(rows);StringBuilder b=new StringBuilder(contract.domain).append('|').append(contract.revision).append('|').append(contract.contractHash);for(String row:rows)b.append('\n').append(row.length()).append(':').append(row);return V51ChecksumEngine.sha256(b.toString());
    }
    /** Two exact lifecycle-owned query indexes are the only permitted delta.
     * Authenticate the previous domain stamp against the same complete catalogue
     * with one of the four declared index-presence states. This also repairs a
     * missing known index after a committed unit, without accepting any unknown
     * attachment, table/trigger drift, literal change or an untrusted stamp. */
    public static void installR31MainlineChoiceLookupIndex(SQLiteDatabase db,String sql){
        if(!db.inTransaction())throw new IllegalStateException("R31_MAINLINE_LOOKUP_MIGRATION_WRITER_REQUIRED");
        if(!isR31MainlineChoiceLookupIndex(sql))throw new IllegalArgumentException("R31_MAINLINE_LOOKUP_UNDECLARED");
        Contract contract=r31MainlineChoiceContract();String sealed=null;
        try(Cursor c=db.rawQuery("SELECT fingerprint FROM "+TABLE_DOMAIN+" WHERE domain=? AND revision=? AND contract_hash=? AND state='READY'",new String[]{contract.domain,Integer.toString(contract.revision),contract.contractHash})){if(c.moveToFirst())sealed=c.getString(0);}
        if(sealed==null)throw new DatabaseCoordinator.SchemaPendingR31("R31_MAINLINE_LOOKUP","AUTHENTICATED_PRIOR_DOMAIN_REQUIRED");
        java.util.Set<String> wanted=new java.util.HashSet<>(java.util.Arrays.asList(contract.objects));java.util.Map<String,String> known=new java.util.HashMap<>();for(String ddl:R31_MAINLINE_LOOKUP_INDEXES)known.put(mainlineLookupNameR31(ddl),ddl.replaceFirst(" IF NOT EXISTS "," "));
        java.util.ArrayList<String> core=new java.util.ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name",null)){
            while(c.moveToNext()){String kind=safe(c.getString(0)),name=safe(c.getString(1)),table=safe(c.getString(2)),actual=safe(c.getString(3));if(!wanted.contains(name)&&!wanted.contains(table))continue;
                if(known.containsKey(name)){if(!"index".equals(kind)||!"r31_mainline_choice".equals(table)||!known.get(name).equals(actual))throw new DatabaseCoordinator.SchemaPendingR31("R31_MAINLINE_LOOKUP","UNKNOWN_INDEX_DEFINITION:"+name);continue;}
                core.add(kind+'|'+name+'|'+table+'|'+actual);
            }
        }
        boolean authenticated=false;for(int mask=0;mask<4;mask++)if(sealed.equals(mainlineLookupFingerprintR31(contract,core,mask)))authenticated=true;
        if(!authenticated)throw new DatabaseCoordinator.SchemaPendingR31("R31_MAINLINE_LOOKUP","UNRELATED_SCHEMA_DRIFT");
        String before=fingerprint(db,contract);db.execSQL(sql);String installed=null;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='index' AND name=?",new String[]{mainlineLookupNameR31(sql)})){if(c.moveToFirst())installed=c.getString(0);}
        if(!sql.replaceFirst(" IF NOT EXISTS "," ").equals(installed))throw new IllegalStateException("R31_MAINLINE_LOOKUP_NOT_EXACT");
        // Every remaining schema object must still be identical after this DDL.
        java.util.Set<String> delta=known.keySet();String coreAfter=fingerprint(db,contract,false,null,delta);
        if(!mainlineLookupFingerprintR31(contract,core,0).equals(coreAfter))throw new IllegalStateException("R31_MAINLINE_LOOKUP_CHANGED_UNRELATED_SCHEMA");
        String after=fingerprint(db,contract);long now=System.currentTimeMillis();
        db.execSQL("INSERT OR REPLACE INTO "+TABLE_JOURNAL+"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code) VALUES(?,?,?,'COMPLETED',1,?,?,?,?,?,'')",new Object[]{contract.domain,contract.revision,"R31_EXACT_LOOKUP_INDEX:"+mainlineLookupNameR31(sql),"EXACT_DECLARED_INDEX_DELTA",before,after,now,now});
        verifyAndStamp(db,contract);
    }

    private static final String R31_MAINLINE_PUBLICATION_LOOKUP="CREATE INDEX IF NOT EXISTS idx_r31_mainline_model_publication ON r31_atomnet_publication(model_hash,weights_hash,published_at DESC,publication_id) WHERE status='PUBLISHED'";
    private static java.util.Map<String,String> publicationAttachmentsR31(SQLiteDatabase db,String table){
        java.util.Map<String,String> known=new java.util.TreeMap<>();
        if("r31_atomnet_publication".equals(table))known.put("idx_r31_mainline_model_publication",R31_MAINLINE_PUBLICATION_LOOKUP);
        for(String ddl:com.ethk3.novaonepro.strategyv2.CheckpointGcStoreR31.referenceGuards(db))if(ddl.contains(" ON \""+table+"\" ")){
            LifecycleSchemaR31.Definition d=new LifecycleSchemaR31.Definition(ddl);known.put(d.name,ddl);
        }return known;
    }
    private static String publicationAttachmentTableR31(SQLiteDatabase db,String sql){
        if(R31_MAINLINE_PUBLICATION_LOOKUP.equals(sql))return "r31_atomnet_publication";
        for(String table:new String[]{"r31_atomnet_publication","r31_model_publication_item"})if(sql!=null&&sql.startsWith("CREATE TRIGGER IF NOT EXISTS \"r31_gc_ref_"+table+"_")&&publicationAttachmentsR31(db,table).containsValue(sql))return table;return null;
    }
    public static boolean isR31PublicationAttachment(SQLiteDatabase db,String sql){return publicationAttachmentTableR31(db,sql)!=null;}
    /** Exact GC reference guards and the one query index may attach to these
     * two existing publication domains. Preserve a durable authenticated core
     * in the existing migration journal, rather than enumerating 2^32 states or
     * accepting arbitrary current attachments. A missing declared attachment
     * can be repaired only while the prior stamp and this core proof agree. */
    public static void installR31PublicationAttachment(SQLiteDatabase db,String sql){
        if(!db.inTransaction())throw new IllegalStateException("R31_PUBLICATION_ATTACHMENT_MIGRATION_WRITER_REQUIRED");
        String table=publicationAttachmentTableR31(db,sql);if(table==null)throw new IllegalArgumentException("R31_PUBLICATION_ATTACHMENT_UNDECLARED");
        Contract contract="r31_atomnet_publication".equals(table)?r31AtomNetPublicationContract():r31ModelPublicationContract();
        java.util.Map<String,String> known=publicationAttachmentsR31(db,table);String manifest=V51ChecksumEngine.sha256(known.toString()),step="R31_EXACT_LIFECYCLE_CORE:"+manifest;
        String sealed=null;try(Cursor c=db.rawQuery("SELECT fingerprint FROM "+TABLE_DOMAIN+" WHERE domain=? AND revision=? AND contract_hash=? AND state='READY'",new String[]{contract.domain,Integer.toString(contract.revision),contract.contractHash})){if(c.moveToFirst())sealed=c.getString(0);}
        if(sealed==null)throw new DatabaseCoordinator.SchemaPendingR31("R31_PUBLICATION_ATTACHMENT","AUTHENTICATED_PRIOR_DOMAIN_REQUIRED:"+contract.domain);
        // Definitions are checked even when the completed proof already exists.
        for(java.util.Map.Entry<String,String> definition:known.entrySet())try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{definition.getKey()})){
            if(c.moveToFirst()&&!LifecycleSchemaR31.canonical(definition.getValue()).equals(LifecycleSchemaR31.canonical(c.getString(0))))throw new DatabaseCoordinator.SchemaPendingR31("R31_PUBLICATION_ATTACHMENT","UNKNOWN_DEFINITION:"+definition.getKey());
        }
        String before=fingerprint(db,contract),core=fingerprint(db,contract,false,null,known.keySet());boolean trusted=sealed.equals(before)||sealed.equals(core);
        if(!trusted)try(Cursor c=db.rawQuery("SELECT 1 FROM "+TABLE_JOURNAL+" WHERE domain=? AND revision=? AND step_id=? AND status='COMPLETED' AND before_hash=? AND after_hash=? AND cursor=?",new String[]{contract.domain,Integer.toString(contract.revision),step,core,sealed,"EXACT_ATTACHMENT_MANIFEST:"+manifest})){trusted=c.moveToFirst();}
        if(!trusted)throw new DatabaseCoordinator.SchemaPendingR31("R31_PUBLICATION_ATTACHMENT","UNRELATED_SCHEMA_DRIFT:"+contract.domain);
        db.execSQL(sql);LifecycleSchemaR31.Definition target=new LifecycleSchemaR31.Definition(sql);if(!target.present(db))throw new IllegalStateException("R31_PUBLICATION_ATTACHMENT_NOT_EXACT");
        if(!core.equals(fingerprint(db,contract,false,null,known.keySet())))throw new IllegalStateException("R31_PUBLICATION_ATTACHMENT_CHANGED_UNRELATED_SCHEMA");
        String after=fingerprint(db,contract);long now=System.currentTimeMillis();
        db.execSQL("INSERT OR REPLACE INTO "+TABLE_JOURNAL+"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code) VALUES(?,?,?,'COMPLETED',1,?,?,?,?,?,'')",new Object[]{contract.domain,contract.revision,step,"EXACT_ATTACHMENT_MANIFEST:"+manifest,core,after,now,now});
        verifyAndStamp(db,contract);
    }

    /** The existing startup migration writer is the sole schema owner. */
    public static void migrateR31MainlineChoice(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31MainlineChoiceContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_MAINLINE_CHOICE_V1", com.ethk3.novaonepro.strategyv2.MainlineChoiceStoreR31::ensureSchemaR31);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_MAINLINE_CHOICE_MIGRATION_FAILED", failure);
        }
    }

    public static Contract r31HistoryImportContract() {
        return new Contract("RESEARCH_R31_HISTORY_IMPORT", 1, new String[] {
                "r31_history_import_run", "r31_history_import_row", "r31_history_import_order",
                "r31_history_import_issue", "r31_history_import_publication",
                "r31_history_import_run_immutable", "r31_history_import_run_keep",
                "r31_history_import_row_no_insert", "r31_history_import_row_no_update",
                "r31_history_import_row_keep"
        });
    }

    /** Completed imports become visible atomically; startup alone installs their schema. */
    public static void migrateR31HistoryImport(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31HistoryImportContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_HISTORY_IMPORT_V1",
                    com.ethk3.novaonepro.strategyv2.HistoryImportR27::ensureR31PublicationSchema);
            if (!com.ethk3.novaonepro.strategyv2.HistoryImportR27.publicationSchemaReadyReadOnlyR31(db))
                throw new DatabaseCoordinator.SchemaPendingR31("R31_HISTORY_IMPORT_MIGRATION", "EXACT_PUBLICATION_SCHEMA_NOT_READY");
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_HISTORY_IMPORT_MIGRATION_FAILED", failure);
        }
    }

    public static Contract r31QualificationReceiptContract() {
        String[][] objects = com.ethk3.novaonepro.strategyv2.QualificationInboxR29.legacyReceiptGuardObjectsR31();
        String[] names = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            if (objects[i].length != 2 || !"trigger".equals(objects[i][0]))
                throw new IllegalStateException("R31_QUALIFICATION_RECEIPT_SCHEMA_CONTRACT");
            names[i] = objects[i][1];
        }
        return new Contract("RESEARCH_R31_QUALIFICATION_RECEIPT", 2, names);
    }

    /** Immutable exposure receipts are installed together with their compatible consumer. */
    public static void migrateR31QualificationReceipts(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31QualificationReceiptContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_QUALIFICATION_RECEIPT_V2", tx -> {
                com.ethk3.novaonepro.strategyv2.QualificationInboxR29.ensureLegacyReceiptGuardsR31(tx);
                com.ethk3.novaonepro.strategyv2.QualificationInboxR29.requireLegacyReceiptGuardsR31(tx);
            });
            com.ethk3.novaonepro.strategyv2.QualificationInboxR29.requireLegacyReceiptGuardsR31(db);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_QUALIFICATION_RECEIPT_MIGRATION_FAILED", failure);
        }
    }

    public static Contract r31SourceJournalContract() {
        return new Contract("RESEARCH_R31_SOURCE_JOURNAL", com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.REVISION,
                com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.schemaObjectsR31());
    }

    /** Only the declared draw-trigger delta may change Critical's authenticated startup layout. */
    public static void migrateR31SourceJournal(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        ensureInfrastructure(db);
        Contract source = r31SourceJournalContract(), critical = criticalContract();
        String priorReason = matchReason(db, critical);
        boolean trustedCritical = priorReason.startsWith("MATCH");
        Set<String> delta = sourceDrawTriggerDeltaR31();
        String beforeCore = fingerprint(db, critical, true, delta);
        String beforeCritical = fingerprint(db, critical);
        prepareRepairIfDrift(db, source);
        try {
            runStep(db, source, "R31_SOURCE_JOURNAL_V1", tx -> {
                String upgrade = com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.inspectUpgradeR31(tx);
                if (!("OLD_EXACT".equals(upgrade) || "R31_EXACT".equals(upgrade) || "R31_V1_EXACT".equals(upgrade) || "EMPTY".equals(upgrade)))
                    throw new IllegalStateException("R31_SOURCE_TRIGGER_UPGRADE_CONFLICT");
                com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.ensureSchemaR31(tx);
                if (!"R31_EXACT".equals(com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.inspectUpgradeR31(tx))
                        || !com.ethk3.novaonepro.strategyv2.ResearchInputPinR30R1.triggersValid(tx))
                    throw new IllegalStateException("R31_SOURCE_TRIGGER_UPGRADE_NOT_VERIFIED");
                if (!beforeCore.equals(fingerprint(tx, critical, true, delta)))
                    throw new IllegalStateException("R31_SOURCE_UPGRADE_CHANGED_UNRELATED_CRITICAL_SCHEMA");
                if (trustedCritical) {
                    // The source schema and the new authenticated Critical stamp roll back together.
                    String afterCritical = fingerprint(tx, critical);
                    long now = System.currentTimeMillis();
                    tx.execSQL("INSERT OR IGNORE INTO " + TABLE_JOURNAL
                                    + "(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                    + " VALUES(?,?,?,'COMPLETED',1,'EXACT_DRAW_TRIGGER_DELTA',?,?,?,?, '')",
                            new Object[] {critical.domain, critical.revision, "R31_SOURCE_DRAW_CUTOVER_V1",
                                    beforeCritical, afterCritical, now, now});
                    tx.execSQL("INSERT OR IGNORE INTO " + TABLE_JOURNAL
                                    +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code) VALUES(?,?,?,'COMPLETED',1,'EXACT_FIRST_SEEN_UPDATE_DELTA',?,?,?,?, '')",
                            new Object[]{critical.domain,critical.revision,"R31_SOURCE_KNOWN_AT_CUTOVER_V2",beforeCritical,afterCritical,now,now});
                    verifyAndStamp(tx, critical);
                } else if (!"STAMP_MISSING".equals(priorReason) && !matches(tx, critical)) {
                    throw new IllegalStateException("R31_SOURCE_UPGRADE_UNTRUSTED_CRITICAL_STAMP:" + priorReason);
                }
            });
            verifyAndStamp(db, source);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_SOURCE_JOURNAL_MIGRATION_FAILED", failure);
        }
    }

    private static Set<String> sourceDrawTriggerDeltaR31() {
        Set<String> names = new HashSet<>();
        for (String[] definitions : new String[][] {
                com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.legacyDrawTriggerSqlR31(),
                com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.sourceDrawTriggerSqlR31()}) {
            for (String sql : definitions) {
                java.util.regex.Matcher match = java.util.regex.Pattern.compile(
                        "(?is)^CREATE TRIGGER IF NOT EXISTS ([A-Za-z0-9_]+) AFTER (?:INSERT|UPDATE(?: OF first_seen_at)?|DELETE) ON draws(?:\\s|$)").matcher(sql);
                if (!match.find()) throw new IllegalStateException("R31_SOURCE_DRAW_TRIGGER_DELTA_CONTRACT");
                names.add(match.group(1));
            }
        }
        if (names.isEmpty() || names.size() > 6) throw new IllegalStateException("R31_SOURCE_DRAW_TRIGGER_DELTA_BOUND");
        return names;
    }

    public static Contract r31EvolutionPublicationContract() {
        String[][] objects = com.ethk3.novaonepro.strategyv2.EvolutionPublicationStoreR31.schemaObjectsR31();
        String[] names = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            if (objects[i].length != 2 || !("table".equals(objects[i][0])
                    || "index".equals(objects[i][0]) || "trigger".equals(objects[i][0])))
                throw new IllegalStateException("R31_EVOLUTION_PUBLICATION_SCHEMA_CONTRACT");
            names[i] = objects[i][1];
        }
        return new Contract("RESEARCH_R31_EVOLUTION_PUBLICATION", 1, names);
    }

    public static void migrateR31EvolutionPublication(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31EvolutionPublicationContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_EVOLUTION_PUBLICATION_V1",
                    com.ethk3.novaonepro.strategyv2.EvolutionPublicationStoreR31::ensureSchemaR31);
            com.ethk3.novaonepro.strategyv2.EvolutionPublicationStoreR31.requireSchemaR31(db);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_EVOLUTION_PUBLICATION_MIGRATION_FAILED", failure);
        }
    }

    public static Contract r31AtomNetPublicationContract() {
        String[][] objects=com.ethk3.novaonepro.strategyv2.AtomNetPublicationR31.schemaObjectsR31();
        String[] names=new String[objects.length];
        for(int i=0;i<objects.length;i++){
            if(objects[i].length!=2||!("table".equals(objects[i][0])||"index".equals(objects[i][0])||"trigger".equals(objects[i][0])))throw new IllegalStateException("R31_ATOMNET_PUBLICATION_SCHEMA_CONTRACT");
            names[i]=objects[i][1];
        }
        return new Contract("RESEARCH_R31_ATOMNET_PUBLICATION",com.ethk3.novaonepro.strategyv2.AtomNetPublicationR31.REVISION,names);
    }
    /** Startup migration only; business preparation never creates or repairs schema. */
    public static void migrateR31AtomNetPublication(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract=r31AtomNetPublicationContract();prepareRepairIfDrift(db,contract);
        try {
            runStep(db,contract,"R31_ATOMNET_PUBLICATION_V1",com.ethk3.novaonepro.strategyv2.AtomNetPublicationR31::ensureSchemaR31);
            com.ethk3.novaonepro.strategyv2.AtomNetPublicationR31.requireReady(db);
            requireExactR31Definitions(db,"R31_ATOMNET_PUBLICATION_MIGRATION",com.ethk3.novaonepro.strategyv2.AtomNetPublicationR31.DDL);
            verifyAndStamp(db,contract);
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_ATOMNET_PUBLICATION_MIGRATION_FAILED",failure);}
    }

    public static Contract r31ModelPublicationContract() {
        String[][] objects=com.ethk3.novaonepro.strategyv2.ModelPublicationR31.schemaObjectsR31();
        String[] names=new String[objects.length];
        for(int i=0;i<objects.length;i++){
            if(objects[i].length!=2||!("table".equals(objects[i][0])||"index".equals(objects[i][0])||"trigger".equals(objects[i][0])))throw new IllegalStateException("R31_MODEL_PUBLICATION_SCHEMA_CONTRACT");
            names[i]=objects[i][1];
        }
        return new Contract("RESEARCH_R31_MODEL_PUBLICATION",com.ethk3.novaonepro.strategyv2.ModelPublicationR31.REVISION,names);
    }
    /** Startup migration only; business preparation never creates or repairs schema. */
    public static void migrateR31ModelPublication(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract=r31ModelPublicationContract();prepareRepairIfDrift(db,contract);
        try {
            runStep(db,contract,"R31_MODEL_PUBLICATION_V1",com.ethk3.novaonepro.strategyv2.ModelPublicationR31::ensureSchemaR31);
            com.ethk3.novaonepro.strategyv2.ModelPublicationR31.requireSchemaR31(db);
            verifyAndStamp(db,contract);
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_MODEL_PUBLICATION_MIGRATION_FAILED",failure);}
    }

    public static Contract r31WarmPublicationContract() {
        return new Contract("RESEARCH_R31_WARM_PUBLICATION",
                com.ethk3.novaonepro.strategyv2.WarmPublicationR31.REVISION,
                new String[]{"r31_warm_candidate_state_owner"});
    }

    /** Startup owner only, after the retention owner has installed its protected archives. */
    public static void migrateR31WarmPublication(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract=r31WarmPublicationContract();prepareRepairIfDrift(db,contract);
        try {
            runStep(db,contract,"R31_WARM_PUBLICATION_V1",com.ethk3.novaonepro.strategyv2.WarmPublicationR31::ensureSchemaR31);
            com.ethk3.novaonepro.strategyv2.WarmPublicationR31.requireSchemaR31(db);
            verifyAndStamp(db,contract);
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_WARM_PUBLICATION_MIGRATION_FAILED",failure);}
    }

    public static Contract r31PlatformPrerequisitesContract() {
        return new Contract("RESEARCH_R31_PLATFORM_PREREQUISITES", 1,
                ResearchPlatformStoreR929.schemaObjectsR31());
    }

    /** Minimal startup prerequisites; this does not declare the entire Research schema ready. */
    public static void migrateR31PlatformPrerequisites(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        Contract contract = r31PlatformPrerequisitesContract();
        prepareRepairIfDrift(db, contract);
        try {
            runStep(db, contract, "R31_PLATFORM_PREREQUISITES_V1", tx -> {
                ResearchPlatformStoreR929.createSchema(tx);
                ResearchPlatformStoreR929.requireSchemaReadOnlyR31(tx);
            });
            ResearchPlatformStoreR929.requireSchemaReadOnlyR31(db);
            verifyAndStamp(db, contract);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_PLATFORM_PREREQUISITES_MIGRATION_FAILED", failure);
        }
    }

    public static Contract r31FeedbackContract() {
        return new Contract("RESEARCH_R31_FEEDBACK",
                com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.R31_FEEDBACK_REVISION,
                new String[] {"r31_feedback_event", "r31_feedback_event_order", "r31_feedback_receipt",
                        "r31_feedback_receipt_event", "r31_frozen_feedback_effect", "r31_feedback_event_immutable",
                        "r31_feedback_model_receipt", "r31_feedback_source",
                        "r31_feedback_event_no_replace", "r31_feedback_event_no_delete"});
    }

    /** Authenticate the original global source-revision guards before the separate pin-journal cutover. */
    public static void migrateR31LiveSourceRevision(SQLiteDatabase db,Step install){
        if(!db.inTransaction())throw new IllegalStateException("R31_LIVE_SOURCE_REVISION_MIGRATION_OWNER_REQUIRED");
        ensureInfrastructure(db);String[] definitions=com.ethk3.novaonepro.strategyv2.SchemaR27.sourceRevisionDefinitionsR31();
        requireExistingR31Definitions(db,"R31_LIVE_SOURCE_REVISION",definitions);
        Contract critical=criticalContract();String reason=matchReason(db,critical);boolean trusted=reason.startsWith("MATCH");
        if(!trusted&&!"STAMP_MISSING".equals(reason))throw new DatabaseCoordinator.SchemaPendingR31("R31_LIVE_SOURCE_REVISION","UNTRUSTED_CRITICAL:"+reason);
        Set<String> delta=new HashSet<>(Arrays.asList("r30_live_revision_draws_insert","r30_live_revision_draws_update","r30_live_revision_draws_delete"));
        String before=fingerprint(db,critical),other=fingerprint(db,critical,false,null,delta);
        try{
            install.run(db);requireExactR31Definitions(db,"R31_LIVE_SOURCE_REVISION",definitions);
            if(!other.equals(fingerprint(db,critical,false,null,delta)))throw new IllegalStateException("R31_LIVE_SOURCE_REVISION_CHANGED_UNRELATED_CRITICAL_SCHEMA");
            String after=fingerprint(db,critical);
            if(trusted&&!before.equals(after)){
                long now=System.currentTimeMillis();
                db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                                +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                +" VALUES(?,?,?,'COMPLETED',1,'EXACT_GLOBAL_REVISION_DRAW_GUARD_DELTA',?,?,?,?, '')",
                        new Object[]{critical.domain,critical.revision,"R31_GLOBAL_SOURCE_REVISION_CUTOVER_V1",before,after,now,now});
                verifyAndStamp(db,critical);
            }
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_LIVE_SOURCE_REVISION_MIGRATION_FAILED",failure);}
    }

    /** The original runtime owner adds its feedback table/index before the R31 feedback owner runs. */
    public static void migrateR31RuntimeBase(SQLiteDatabase db,Step install){
        if(!db.inTransaction())throw new IllegalStateException("R31_RUNTIME_BASE_MIGRATION_OWNER_REQUIRED");
        ensureInfrastructure(db);
        String[] definitions=com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.criticalBaseDefinitionsR31();
        requireExistingR31Definitions(db,"R31_RUNTIME_BASE",definitions);
        Contract critical=criticalContract();String reason=matchReason(db,critical);boolean trusted=reason.startsWith("MATCH");
        if(!trusted&&!"STAMP_MISSING".equals(reason))throw new DatabaseCoordinator.SchemaPendingR31("R31_RUNTIME_BASE","UNTRUSTED_CRITICAL:"+reason);
        Set<String> delta=new HashSet<>(Arrays.asList("r26_feedback","r26_feedback_order"));
        String before=fingerprint(db,critical),other=fingerprint(db,critical,false,null,delta);
        try{
            install.run(db);requireExactR31Definitions(db,"R31_RUNTIME_BASE",definitions);
            if(!other.equals(fingerprint(db,critical,false,null,delta)))throw new IllegalStateException("R31_RUNTIME_BASE_CHANGED_UNRELATED_CRITICAL_SCHEMA");
            String after=fingerprint(db,critical);
            if(trusted&&!before.equals(after)){
                long now=System.currentTimeMillis();
                db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                                +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                +" VALUES(?,?,?,'COMPLETED',1,'EXACT_LEGACY_FEEDBACK_BASE_DELTA',?,?,?,?, '')",
                        new Object[]{critical.domain,critical.revision,"R31_RUNTIME_FEEDBACK_BASE_CUTOVER_V1",before,after,now,now});
                verifyAndStamp(db,critical);
            }
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_RUNTIME_BASE_MIGRATION_FAILED",failure);}
    }

    /** Research event/consumer extensions are migrated after the legacy feedback and checkpoint schema. */
    public static void migrateR31Feedback(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_MIGRATION_OWNER_REQUIRED");
        ensureInfrastructure(db);
        Contract contract=r31FeedbackContract(), critical=criticalContract();
        boolean trustedCritical=matchReason(db,critical).startsWith("MATCH");
        String beforeCritical=fingerprint(db,critical);
        Set<String> delta=Collections.singleton("r31_feedback_source");
        String beforeCore=fingerprint(db,critical,false,null,delta);
        prepareRepairIfDrift(db,contract);
        try {
            runStep(db,contract,"R31_FEEDBACK_V1",
                    com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26::ensureR31Feedback);
            requireExactR31Definitions(db,"R31_FEEDBACK_MIGRATION",
                    com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.R31_FEEDBACK_DDL);
            if(!beforeCore.equals(fingerprint(db,critical,false,null,delta)))
                throw new IllegalStateException("R31_FEEDBACK_CHANGED_UNRELATED_CRITICAL_SCHEMA");
            String afterCritical=fingerprint(db,critical);
            if(trustedCritical&&!beforeCritical.equals(afterCritical)) {
                long now=System.currentTimeMillis();
                db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                                +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                +" VALUES(?,?,?,'COMPLETED',1,'EXACT_INDEX_DELTA:r31_feedback_source',?,?,?,?, '')",
                        new Object[]{critical.domain,critical.revision,"R31_FEEDBACK_INDEX_CUTOVER_V1",
                                beforeCritical,afterCritical,now,now});
                // The authenticated addition and Critical stamp commit or roll back together.
                verifyAndStamp(db,critical);
            }
            verifyAndStamp(db,contract);
        } catch(RuntimeException failure) {throw failure;}
        catch(Exception failure) {throw new IllegalStateException("R31_FEEDBACK_MIGRATION_FAILED",failure);}
    }

    /** Read-only cold-start barrier: generic Core repair must not authenticate unknown Research attachments. */
    public static void requireExistingResearchCriticalGuardsR31(SQLiteDatabase db){
        requireExistingR31Definitions(db,"R31_COLD_RUNTIME_BASE",com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.criticalBaseDefinitionsR31());
        requireExistingR31Definitions(db,"R31_COLD_LIVE_SOURCE_REVISION",com.ethk3.novaonepro.strategyv2.SchemaR27.sourceRevisionDefinitionsR31());
        com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.requireGuardDefinitionsR31(db,
                com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.criticalProtectionTriggersR31(db),false);
        if(schemaObjectExists(db,"r31_feedback_source")){
            for(String expected:com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.R31_FEEDBACK_DDL)
                if(expected.startsWith("CREATE INDEX IF NOT EXISTS r31_feedback_source "))requireExactR31Definitions(db,"R31_COLD_FEEDBACK_INDEX",new String[]{expected});
        }
        com.ethk3.novaonepro.strategyv2.CertificateRevocationR30.requireSourceTriggerDefinitionsR31(db,
                com.ethk3.novaonepro.strategyv2.CertificateRevocationR30.criticalSourceTriggersR31(db),false);
        String sourceShape=com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.inspectUpgradeR31(db);
        if(!("EMPTY".equals(sourceShape)||"OLD_EXACT".equals(sourceShape)||"R31_V1_EXACT".equals(sourceShape)||"R31_EXACT".equals(sourceShape)))
            throw new DatabaseCoordinator.SchemaPendingR31("R31_COLD_SOURCE_GUARDS","UNKNOWN_DRAW_OR_HISTORY_TRIGGER_DEFINITION");
    }

    private static final java.util.regex.Pattern SU1_DRAW_CLOCK_R31=java.util.regex.Pattern.compile(
            "^CREATE TRIGGER IF NOT EXISTS (su1_draw_(?:insert|update|delete)_clock) AFTER (?:INSERT|UPDATE|DELETE) ON draws\\b");
    private static String normalizedTriggerR31(String sql){
        return sql==null?"":sql.replace(" IF NOT EXISTS "," ").trim().replaceAll(";\\s*$","").replaceAll("\\s+"," ");
    }
    /** True only for SU1's three declared draw clock triggers (Critical-table attachments). */
    public static boolean isR31Su1DrawClockTrigger(String sql){return sql!=null&&SU1_DRAW_CLOCK_R31.matcher(sql).find();}
    /** SU1 owns three exact clock triggers on the Critical draws table. Install each as an authenticated
     *  Critical delta in the SU1 owner transaction: untrusted Critical and unknown definitions are refused,
     *  every other Critical object must be unchanged, and the journal/stamp advance in the same transaction. */
    public static void installR31Su1DrawClockTrigger(SQLiteDatabase db,String sql){
        if(!db.inTransaction())throw new IllegalStateException("R31_SU1_DRAW_CLOCK_OWNER_REQUIRED");
        java.util.regex.Matcher declared=SU1_DRAW_CLOCK_R31.matcher(sql==null?"":sql);
        if(!declared.find())throw new IllegalArgumentException("R31_SU1_DRAW_CLOCK_UNDECLARED");
        String name=declared.group(1);
        ensureInfrastructure(db);
        Contract critical=criticalContract();String priorReason=matchReason(db,critical);boolean trustedCritical=priorReason.startsWith("MATCH");
        if(!trustedCritical&&!"STAMP_MISSING".equals(priorReason))throw new DatabaseCoordinator.SchemaPendingR31("R31_SU1_DRAW_CLOCK","UNTRUSTED_CRITICAL:"+priorReason);
        String existing=null;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{name})){if(c.moveToFirst())existing=c.getString(0);}
        if(existing!=null&&!normalizedTriggerR31(existing).equals(normalizedTriggerR31(sql)))
            throw new DatabaseCoordinator.SchemaPendingR31("R31_SU1_DRAW_CLOCK","UNKNOWN_TRIGGER_DEFINITION:"+name);
        Set<String> delta=Collections.singleton(name);
        String beforeCritical=fingerprint(db,critical),beforeCore=fingerprint(db,critical,false,null,delta);
        db.execSQL(sql);
        String installed=null;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{name})){if(c.moveToFirst())installed=c.getString(0);}
        if(!normalizedTriggerR31(sql).equals(normalizedTriggerR31(installed)))throw new IllegalStateException("R31_SU1_DRAW_CLOCK_NOT_EXACT:"+name);
        if(!beforeCore.equals(fingerprint(db,critical,false,null,delta)))throw new IllegalStateException("R31_SU1_DRAW_CLOCK_CHANGED_UNRELATED_CRITICAL_SCHEMA");
        String afterCritical=fingerprint(db,critical);
        if(trustedCritical&&!beforeCritical.equals(afterCritical)){
            long now=System.currentTimeMillis();
            db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                            +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                            +" VALUES(?,?,?,'COMPLETED',1,?,?,?,?,?, '')",
                    new Object[]{critical.domain,critical.revision,"R31_SU1_DRAW_CLOCK_CUTOVER_V1:"+name,"EXACT_DRAW_CLOCK_TRIGGER_DELTA:"+name,beforeCritical,afterCritical,now,now});
            verifyAndStamp(db,critical);
        }
    }

    /** SU2 becomes available in a later schema task: install only its exact source-revocation additions. */
    public static void migrateR31CertificateRevocationProtection(SQLiteDatabase db,Step install){
        if(!db.inTransaction())throw new IllegalStateException("R31_REVOCATION_MIGRATION_OWNER_REQUIRED");
        ensureInfrastructure(db);
        String[][] guards=com.ethk3.novaonepro.strategyv2.CertificateRevocationR30.criticalSourceTriggersR31(db);
        com.ethk3.novaonepro.strategyv2.CertificateRevocationR30.requireSourceTriggerDefinitionsR31(db,guards,false);
        Contract critical=criticalContract();String priorReason=matchReason(db,critical);boolean trustedCritical=priorReason.startsWith("MATCH");
        if(!trustedCritical&&!"STAMP_MISSING".equals(priorReason))throw new DatabaseCoordinator.SchemaPendingR31("R31_CERTIFICATE_REVOCATION","UNTRUSTED_CRITICAL:"+priorReason);
        Set<String> delta=new HashSet<>();for(String[] guard:guards)delta.add(guard[0]);
        String beforeCritical=fingerprint(db,critical),beforeCore=fingerprint(db,critical,false,null,delta);
        try{
            install.run(db);
            com.ethk3.novaonepro.strategyv2.CertificateRevocationR30.requireSourceTriggerDefinitionsR31(db,guards,true);
            if(!beforeCore.equals(fingerprint(db,critical,false,null,delta)))throw new IllegalStateException("R31_REVOCATION_CHANGED_UNRELATED_CRITICAL_SCHEMA");
            String afterCritical=fingerprint(db,critical);
            if(trustedCritical&&!beforeCritical.equals(afterCritical)){
                long now=System.currentTimeMillis();
                db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                                +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                +" VALUES(?,?,?,'COMPLETED',1,'EXACT_DRAW_REVOCATION_GUARD_DELTA',?,?,?,?, '')",
                        new Object[]{critical.domain,critical.revision,"R31_REVOCATION_GUARD_CUTOVER_V1",beforeCritical,afterCritical,now,now});
                verifyAndStamp(db,critical);
            }
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_REVOCATION_PROTECTION_MIGRATION_FAILED",failure);}
    }

    /** Authenticate only the retention owner's exact feedback guard delta, in its existing migration transaction. */
    public static void migrateR31RetentionProtection(SQLiteDatabase db,Step install){
        if(!db.inTransaction())throw new IllegalStateException("R31_RETENTION_MIGRATION_OWNER_REQUIRED");
        ensureInfrastructure(db);
        String[][] guards=com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.criticalProtectionTriggersR31(db);
        com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.requireGuardDefinitionsR31(db,guards,false);
        Contract critical=criticalContract();String priorReason=matchReason(db,critical);
        boolean trustedCritical=priorReason.startsWith("MATCH");
        if(!trustedCritical&&!"STAMP_MISSING".equals(priorReason))throw new DatabaseCoordinator.SchemaPendingR31("R31_RETENTION","UNTRUSTED_CRITICAL:"+priorReason);
        Set<String> delta=new HashSet<>();for(String[] guard:guards)delta.add(guard[0]);
        String beforeCritical=fingerprint(db,critical),beforeCore=fingerprint(db,critical,false,null,delta);
        try {
            install.run(db);
            com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.requireGuardDefinitionsR31(db,guards,true);
            if(!beforeCore.equals(fingerprint(db,critical,false,null,delta)))throw new IllegalStateException("R31_RETENTION_CHANGED_UNRELATED_CRITICAL_SCHEMA");
            String afterCritical=fingerprint(db,critical);
            if(trustedCritical&&!beforeCritical.equals(afterCritical)){
                long now=System.currentTimeMillis();
                db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                                +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                +" VALUES(?,?,?,'COMPLETED',1,'EXACT_FEEDBACK_ARCHIVE_GUARD_DELTA',?,?,?,?, '')",
                        new Object[]{critical.domain,critical.revision,"R31_RETENTION_GUARD_CUTOVER_V1",beforeCritical,afterCritical,now,now});
                verifyAndStamp(db,critical);
            }
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_RETENTION_PROTECTION_MIGRATION_FAILED",failure);}
    }

    private static void requireExistingR31Definitions(SQLiteDatabase db,String operation,String[] definitions){
        java.util.regex.Pattern declaration=java.util.regex.Pattern.compile("^CREATE (?:UNIQUE )?(?:TABLE|INDEX|TRIGGER|VIEW) IF NOT EXISTS ([A-Za-z0-9_]+)(?:\\s|\\()");
        for(String expected:definitions){java.util.regex.Matcher named=declaration.matcher(expected);if(!named.find())throw new IllegalStateException("R31_SCHEMA_DECLARATION_CONTRACT:"+operation);if(schemaObjectExists(db,named.group(1)))requireExactR31Definitions(db,operation,new String[]{expected});}
    }

    private static void requireExactR31Definitions(SQLiteDatabase db,String operation,String[] definitions) {
        java.util.regex.Pattern declaration=java.util.regex.Pattern.compile(
                "^CREATE (?:UNIQUE )?(TABLE|INDEX|TRIGGER|VIEW) IF NOT EXISTS ([A-Za-z0-9_]+)(?:\\s|\\()");
        for(String expected:definitions) {
            java.util.regex.Matcher named=declaration.matcher(expected);
            if(!named.find())throw new IllegalStateException("R31_SCHEMA_DECLARATION_CONTRACT:"+operation);
            try(Cursor actual=db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?",new String[]{named.group(2)})) {
                if(!actual.moveToFirst()||!named.group(1).toLowerCase(java.util.Locale.ROOT).equals(actual.getString(0))
                        ||!canonicalR31Definition(expected).equals(canonicalR31Definition(actual.getString(1))))
                    throw new DatabaseCoordinator.SchemaPendingR31(operation,"EXACT_SCHEMA_DEFINITION_MISMATCH:"+named.group(2));
            }
        }
    }

    private static String canonicalR31Definition(String sql) {
        return sql==null?"":sql.replace(" IF NOT EXISTS "," ").trim().replaceAll("\\s+"," ");
    }

    public static Contract r31TransportTimingContract() {
        return new Contract("REALTIME_R31_TIMING", FetchLedgerR925.R31_TIMING_REVISION,
                new String[] {"r31_fetch_timing", "r31_fetch_timing_issue"});
    }

    /** Startup-only additive domain, under the existing sole writer guard. */
    public static void migrateR31TransportTiming(SQLiteDatabase db) {
        boolean own = !db.inTransaction();
        try {
            if (own) db.beginTransaction();
            Contract contract = r31TransportTimingContract();
            prepareRepairIfDrift(db, contract);
            runStep(db, contract, "R31_TRANSPORT_TIMING_V1", FetchLedgerR925::ensureR31Timing);
            verifyAndStamp(db, contract);
            if (own) db.setTransactionSuccessful();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("R31_TRANSPORT_TIMING_MIGRATION_FAILED", failure);
        } finally {
            if (own && db.inTransaction()) db.endTransaction();
        }
    }

    /** One original Legacy DDL statement, its exact before/after proof and the
     * Critical stamp share a commit. Never authenticate an already drifted
     * layout: only the production owner's declared object may change. */
    static void migrateLegacyDdlR31(SQLiteDatabase db,String sql,String object,String table,Step install) {
        Contract critical=criticalContract();
        Set<String> roots=new HashSet<>(Arrays.asList(critical.objects));
        boolean affected=roots.contains(object)||roots.contains(table),own=!db.inTransaction();
        try {
            if(own)db.beginTransaction();
            if(!affected){install.run(db);if(own)db.setTransactionSuccessful();return;}
            ensureInfrastructure(db);
            String reason=matchReason(db,critical);boolean trusted=reason.startsWith("MATCH");
            if(!trusted&&!"STAMP_MISSING".equals(reason))throw new DatabaseCoordinator.SchemaPendingR31("R31_LEGACY_CRITICAL_DDL","UNTRUSTED_CRITICAL:"+reason);
            Set<String> delta=Collections.singleton(object);
            String before=fingerprint(db,critical),core=fingerprint(db,critical,false,null,delta);
            install.run(db);
            if(!core.equals(fingerprint(db,critical,false,null,delta)))throw new IllegalStateException("R31_LEGACY_DDL_CHANGED_UNDECLARED_CRITICAL_OBJECT");
            String after=fingerprint(db,critical);
            if(trusted&&!before.equals(after)) {
                long now=System.currentTimeMillis();String statement=V51ChecksumEngine.sha256(sql);
                db.execSQL("INSERT OR IGNORE INTO "+TABLE_JOURNAL
                                +"(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                                +" VALUES(?,?,?,'COMPLETED',1,?,?,?,?,?, '')",
                        new Object[]{critical.domain,critical.revision,"R31_LEGACY_CRITICAL_DDL:"+statement,"EXACT_OWNER_DDL:"+statement,before,after,now,now});
                verifyAndStamp(db,critical);
            }
            if(own)db.setTransactionSuccessful();
        }catch(RuntimeException failure){throw failure;}
        catch(Exception failure){throw new IllegalStateException("R31_LEGACY_CRITICAL_DDL_FAILED",failure);}
        finally{if(own&&db.inTransaction())db.endTransaction();}
    }

    public static Contract criticalContract() {
        return new Contract(
                DOMAIN_CRITICAL,
                ReleaseManifestR926.CRITICAL_DOMAIN_REVISION,
                new String[] {
                    "r26_feedback",
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
                    UnifiedResearchChoiceR932.TABLE,
                    "idx_r929_trial_family", "idx_r929_bundle_one_active",
                    "idx_r929_cert_lookup", "idx_r929_contribution_issue",
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
        List<String> objects=new ArrayList<>(Arrays.asList(new String[] {
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
                }));
        Collections.addAll(objects,LegacySchemaCoordinatorR926.requiredMainChainObjectsR31());
        return new Contract(DOMAIN_LEGACY,ReleaseManifestR926.LEGACY_DOMAIN_REVISION,
                new java.util.LinkedHashSet<>(objects).toArray(new String[0]));
    }

    public static void ensureInfrastructure(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R926_SCHEMA_DB_NULL");
        // Read paths call this repeatedly while the legacy catalogue is growing.
        // CREATE IF NOT EXISTS still makes Android compile a schema statement each time.
        // A fresh, typed census is equivalent to those no-op creates when all three
        // objects already exist; missing objects still execute the original DDL below.
        // No schema_version, domain fingerprint or READY result is cached.
        int present=0;
        try(Cursor c=db.rawQuery("SELECT name,type FROM sqlite_master WHERE name IN (?,?,?)",
                new String[]{TABLE_DOMAIN,TABLE_JOURNAL,"idx_r926_migration_status"})){
            while(c.moveToNext()){
                String name=c.getString(0),type=c.getString(1);
                if(TABLE_DOMAIN.equals(name)&&"table".equals(type))present|=1;
                if(TABLE_JOURNAL.equals(name)&&"table".equals(type))present|=2;
                if("idx_r926_migration_status".equals(name)&&"index".equals(type))present|=4;
            }
        }
        if(present==7)return;
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
        return matchReason(db, contract).startsWith("MATCH");
    }

    /** Records which existing stamp check failed; never treats missing evidence as ready. */
    public static String matchReason(SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return "ARGUMENT_MISSING";
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT revision,contract_hash,fingerprint,state FROM " + TABLE_DOMAIN
                    + " WHERE domain=?", new String[] {contract.domain});
            if (!c.moveToFirst()) return "STAMP_MISSING";
            if (c.getInt(0) != contract.revision) return "REVISION_MISMATCH";
            if (!contract.contractHash.equals(c.getString(1))) return "CONTRACT_MISMATCH";
            if (!"READY".equals(c.getString(3))) return "STATE_NOT_READY";
            String stored = c.getString(2);
            if (fingerprint(db, contract).equals(stored)) return "MATCH";
            if (DOMAIN_CRITICAL.equals(contract.domain)
                    && completeCriticalJournal(db, contract)
                    && (com.ethk3.novaonepro.strategyv2.ResearchInputPinR30R1.triggersValid(db)
                        || "OLD_EXACT".equals(com.ethk3.novaonepro.strategyv2.ResearchSourceJournalR31.inspectUpgradeR31(db)))
                    && fingerprintWithoutResearchPinDrawTriggers(db, contract).equals(stored))
                return "MATCH_R30_PIN_DRAW_ADDITION";
            return "FINGERPRINT_MISMATCH";
        } catch (RuntimeException unavailable) { return "STAMP_UNREADABLE"; }
        finally { if (c != null) c.close(); }
    }

    private static boolean completeCriticalJournal(SQLiteDatabase db, Contract contract) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN status<>'COMPLETED' THEN 1 ELSE 0 END) FROM "
                + TABLE_JOURNAL + " WHERE domain=? AND revision=?",
                new String[]{contract.domain, String.valueOf(contract.revision)})) {
            if (!c.moveToFirst() || c.getLong(0) == 0 || c.getLong(1) != 0) return false;
        }
        for (String step : new String[]{"F02_SETTLEMENT_RECOVERY_AUDIT_V1", "CP12_CRITICAL_CANONICAL_REASON_V1",
                "R815_CRITICAL_SCHEMA", "R906_CRITICAL_SCHEMA", "R925_CRITICAL_SCHEMA", "R926_EVENT_LEDGER_SCHEMA",
                "R35_SCHEMA", "R36_SCHEMA", "R811_CRITICAL_SCHEMA", "V51_RUNTIME_SCHEMA", "B03_RUNTIME_SCHEMA",
                "R26_FEEDBACK_SCHEMA"}) if (!stepComplete(db, contract, step)) return false;
        return true;
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

    /**
     * R31Q: runs an idempotent ensure-style step WITHOUT one wrapping transaction. Every DDL statement
     * commits on its own, so a process killed part-way (vivo cleaner, overlay install) keeps the work it
     * finished instead of rolling hours of ALTER/INDEX work back and restarting from zero next launch.
     * Only for steps whose body is safe to re-run (CREATE IF NOT EXISTS / add-column-if-missing).
     */
    public static void runStepAutocommitR31(SQLiteDatabase db, Contract contract, String stepId, Step step) throws Exception {
        AUTOCOMMIT_STEP_R31Q.set(Boolean.TRUE);
        try { runStep(db, contract, stepId, step); }
        finally { AUTOCOMMIT_STEP_R31Q.remove(); }
    }
    private static final ThreadLocal<Boolean> AUTOCOMMIT_STEP_R31Q = new ThreadLocal<>();

    /** Targeted re-arm: unrelated completed units and every business row survive. */
    static void invalidateStepR31(SQLiteDatabase db,Contract contract,String step,String reason) {
        db.execSQL("UPDATE "+TABLE_JOURNAL+" SET status='REPAIR_REQUIRED',cursor='POSTCONDITION_DRIFT',completed_at=0,error_code=? WHERE domain=? AND revision=? AND step_id=?",
                new Object[]{bounded(reason,1800),contract.domain,contract.revision,step});
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
        boolean ownTx = !db.inTransaction() && AUTOCOMMIT_STEP_R31Q.get() == null;
        try {
            if (ownTx) db.beginTransaction();
            cutPointR31(contract.domain, stepId, "STEP_STARTED");
            step.run(db);
            cutPointR31(contract.domain, stepId, "STEP_BODY_DONE");
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
                            + " SET status=?,cursor='RETRY',completed_at=?,error_code=?"
                            + " WHERE domain=? AND revision=? AND step_id=?",
                    new Object[] {failure instanceof LegacyDdlR31.Pending ? "PENDING" : "FAILED", Long.valueOf(System.currentTimeMillis()), error(failure), contract.domain,
                            Integer.valueOf(contract.revision), safe(stepId)});
            if (failure instanceof Exception) throw (Exception) failure;
            throw new IllegalStateException("R926_MIGRATION_STEP_FAILED:" + stepId, failure);
        } finally {
            if (ownTx && db.inTransaction()) db.endTransaction();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // R31-B12-C01: overlay-install upgrade journal.
    //
    // PREPARE -> VERIFY_SOURCE -> TRANSFORM -> VERIFY_TARGET -> PUBLISH -> CLEANUP. Every phase is one
    // short durable commit of its work plus its journal row and completion hash, preceded by a durable
    // STARTED row, so a process killed at any point re-enters at the first phase that has not committed:
    // a COMPLETED phase is never run again (its attempt never grows) and a STARTED one is re-executed
    // from its committed predecessor. Rows live in the existing R926 domain/journal tables (domain
    // R31_UPGRADE:<id>), so the journal adds no schema object of its own.
    //
    // Protected roots (draws, models, certificates, frozen predictions) are hashed over the columns
    // they had before the upgrade, inside the TRANSFORM write transaction, before and after the
    // transform body: a transform that changes one original value is rolled back before it commits.
    // Added columns keep old-value semantics (NULL/unknown or their declared default); the journal
    // never back-fills a historical value. The journal never deletes, renames or rebuilds a source
    // table and never changes PRAGMA user_version, so an older reader keeps reading the original
    // objects; CLEANUP runs only after PUBLISH and only the spec's declared, non-destructive step.
    // Low disk blocks before TRANSFORM (no new additions) and deletes nothing.
    // ---------------------------------------------------------------------------------------------
    public enum UpgradePhaseR31 { PREPARE, VERIFY_SOURCE, TRANSFORM, VERIFY_TARGET, PUBLISH, CLEANUP }
    public static final String UPGRADE_PREFIX_R31 = "R31_UPGRADE:";
    public static final String[] PROTECTED_ROOTS_R31 = {"draws", "r27_policy", "r27_live_shadow", "r27_live_frozen",
            "r27_atomnet_state", "r27_certificate"};
    public static final long UPGRADE_MIN_FREE_BYTES_R31 = 64L * 1024L * 1024L;

    /** One durable write transaction (production: the sole MIGRATION writer). */
    public interface WriterR31 { void write(String label, Step body) throws Exception; }
    /** Test seam: called at every durable boundary; production installs none. */
    public interface CutPointR31 { void reached(String domain, String step, String point); }
    private static volatile CutPointR31 cutPointsR31;
    public static void installCutPointsR31(CutPointR31 hook) { cutPointsR31 = hook; }
    static void cutPointR31(String domain, String step, String point) {
        CutPointR31 hook = cutPointsR31; if (hook != null) hook.reached(domain, safe(step), point);
    }
    /** Free bytes next to the database file; the default reads the filesystem. */
    public interface DiskProbeR31 { long usableBytes(SQLiteDatabase db); }
    public static final DiskProbeR31 FILESYSTEM_DISK_R31 = db -> {
        String path = db.getPath();
        if (path == null || path.length() == 0 || path.startsWith(":memory:")) return Long.MAX_VALUE;
        java.io.File dir = new java.io.File(path).getAbsoluteFile().getParentFile();
        // Same API as StorageHealthR811 (minSdk 21). HOST runs pass their own probe (no android.os.StatFs there).
        return dir == null ? Long.MAX_VALUE : new android.os.StatFs(dir.getPath()).getAvailableBytes();
    };

    public static final class UpgradeSpecR31 {
        public final String id, contractHash; public final int revision; public final String[] roots;
        public final long requiredFreeBytes; public final Step transform, cleanup; public final DiskProbeR31 disk;
        public UpgradeSpecR31(String id, int revision, String contractHash, String[] roots, long requiredFreeBytes,
                              DiskProbeR31 disk, Step transform, Step cleanup) {
            if (safe(id).isEmpty() || transform == null) throw new IllegalArgumentException("R31_UPGRADE_SPEC_INCOMPLETE");
            this.id = safe(id); this.revision = revision; this.contractHash = safe(contractHash);
            this.roots = roots == null ? new String[0] : roots.clone(); this.requiredFreeBytes = Math.max(0, requiredFreeBytes);
            this.disk = disk == null ? FILESYSTEM_DISK_R31 : disk; this.transform = transform; this.cleanup = cleanup;
        }
        /** The journal key changes whenever the upgrade contract changes, so a new contract is a new upgrade. */
        public String domain() { return UPGRADE_PREFIX_R31 + id + ":" + V51ChecksumEngine.sha256(id + '|' + revision + '|' + contractHash + '|' + join(Arrays.asList(roots))).substring(0, 16); }
    }

    public static final class UpgradeOutcomeR31 {
        public final String state, domain; public final java.util.Map<String, String> phaseHashes; public final java.util.Map<String, Integer> attempts;
        public final String forwardCompatibility;
        UpgradeOutcomeR31(String state, String domain, java.util.Map<String, String> hashes, java.util.Map<String, Integer> attempts, String forward) {
            this.state = state; this.domain = domain; this.phaseHashes = Collections.unmodifiableMap(hashes); this.attempts = Collections.unmodifiableMap(attempts); this.forwardCompatibility = forward;
        }
    }

    /** Phase status/attempt/hash as committed; absent phases are not in the maps. */
    public static UpgradeOutcomeR31 upgradeStateR31(SQLiteDatabase db, UpgradeSpecR31 spec) {
        java.util.Map<String, String> hashes = new java.util.LinkedHashMap<>(); java.util.Map<String, Integer> attempts = new java.util.LinkedHashMap<>();
        String domain = spec.domain(), state = "NOT_STARTED";
        if (!schemaObjectExists(db, TABLE_JOURNAL)) return new UpgradeOutcomeR31(state, domain, hashes, attempts, forwardCompatibilityR31(false));
        boolean cleaned = false;
        try (Cursor c = db.rawQuery("SELECT step_id,status,attempt,after_hash FROM " + TABLE_JOURNAL + " WHERE domain=? AND revision=?",
                new String[] {domain, String.valueOf(spec.revision)})) {
            while (c.moveToNext()) {
                String phase = c.getString(0); attempts.put(phase, c.getInt(2));
                if ("COMPLETED".equals(c.getString(1))) { hashes.put(phase, c.getString(3)); if (UpgradePhaseR31.CLEANUP.name().equals(phase)) cleaned = true; }
            }
        }
        try (Cursor c = db.rawQuery("SELECT state FROM " + TABLE_DOMAIN + " WHERE domain=?", new String[] {domain})) { if (c.moveToFirst()) state = c.getString(0); }
        return new UpgradeOutcomeR31(state, domain, hashes, attempts, forwardCompatibilityR31(cleaned));
    }
    /** What an older APK can rely on. Measured on a device only; the host never claims an old-APK read. */
    private static String forwardCompatibilityR31(boolean cleaned) {
        return (cleaned ? "PUBLISHED" : "IN_PROGRESS") + "|ADDITIVE_ONLY|SOURCE_OBJECTS_RETAINED|USER_VERSION_UNCHANGED|OLD_APK_READ=NOT_RUN_HOST|RECOVERY=ROLL_FORWARD";
    }

    /**
     * Runs or resumes the upgrade. Call from a background thread with no open transaction; each phase
     * takes the writer for one short commit. Returns COMPLETE (also on a re-entry after completion) or
     * BLOCKED_LOW_DISK; throws when a phase fails, with the failure journaled and nothing published.
     */
    public static UpgradeOutcomeR31 runUpgradeR31(SQLiteDatabase reader, WriterR31 writer, UpgradeSpecR31 spec) throws Exception {
        if (android.os.Looper.myLooper() != null && android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
            throw new IllegalStateException("R31_UPGRADE_ON_MAIN_THREAD");
        if (reader != null && reader.inTransaction()) throw new IllegalStateException("R31_UPGRADE_INSIDE_TRANSACTION");
        final String domain = spec.domain();
        final java.util.Map<String, String> done = new java.util.LinkedHashMap<>();
        writer.write("R31_UPGRADE_INFRA", db -> { ensureInfrastructure(db); done.putAll(completedPhasesR31(db, domain, spec.revision)); });
        if (done.containsKey(UpgradePhaseR31.CLEANUP.name())) {
            // The schema owner is still re-entered on every start (its steps are idempotent and their own
            // journal skips completed DDL); only the upgrade's verification phases are not repeated.
            writer.write(spec.id, spec.transform); // same operation label as before the journal
            return new UpgradeOutcomeR31("COMPLETE", domain, done, Collections.<String, Integer>emptyMap(), forwardCompatibilityR31(true));
        }
        final String[] sourceColumns = new String[1];
        for (final UpgradePhaseR31 phase : UpgradePhaseR31.values()) {
            if (done.containsKey(phase.name())) { if (phase == UpgradePhaseR31.VERIFY_SOURCE) sourceColumns[0] = cursorR31(writer, domain, spec.revision, phase); continue; }
            writer.write("R31_UPGRADE_" + phase + "_START", db -> {
                // Plain INSERT OR IGNORE + UPDATE: minSdk 21 SQLite has no UPSERT.
                Long now = Long.valueOf(System.currentTimeMillis());
                db.execSQL("INSERT OR IGNORE INTO " + TABLE_JOURNAL + "(domain,revision,step_id,status,attempt,cursor,before_hash,after_hash,started_at,completed_at,error_code)"
                        + " VALUES(?,?,?,'NEW',0,'START','','',?,0,'')", new Object[] {domain, Integer.valueOf(spec.revision), phase.name(), now});
                db.execSQL("UPDATE " + TABLE_JOURNAL + " SET status='STARTED',attempt=attempt+1,cursor='START',started_at=?,error_code='' WHERE domain=? AND revision=? AND step_id=? AND status<>'COMPLETED'",
                        new Object[] {now, domain, Integer.valueOf(spec.revision), phase.name()});
                if (phase == UpgradePhaseR31.PREPARE) markUpgradeR31(db, spec, "MIGRATING", "PREPARE", "");
            });
            cutPointR31(domain, phase.name(), "PHASE_STARTED_COMMITTED");
            final String[] result = new String[2];
            try {
                writer.write(phase == UpgradePhaseR31.TRANSFORM ? spec.id : "R31_UPGRADE_" + phase, db -> {
                    cutPointR31(domain, phase.name(), "PHASE_TX_OPEN");
                    String before = previousPhaseHashR31(db, domain, spec.revision, phase);
                    switch (phase) {
                        case PREPARE: {
                            long free = spec.disk.usableBytes(db);
                            if (free < spec.requiredFreeBytes) { result[0] = "BLOCKED_LOW_DISK"; markUpgradeR31(db, spec, "BLOCKED_LOW_DISK", "PREPARE", "free=" + free + "<" + spec.requiredFreeBytes); break; }
                            result[1] = "free=" + free;
                            result[0] = V51ChecksumEngine.sha256("PREPARE|" + scalarInt(db, "PRAGMA user_version", null, -1) + '|' + spec.contractHash + '|' + domain);
                            break;
                        }
                        case VERIFY_SOURCE: {
                            String columns = rootColumnsR31(db, spec.roots); result[1] = columns;
                            result[0] = rootsHashR31(tableHashesR31(db, columns)); break;
                        }
                        case TRANSFORM: {
                            String columns = sourceColumns[0];
                            java.util.Map<String, String> sourceTables = tableHashesR31(db, columns);
                            String source = rootsHashR31(sourceTables);
                            String recorded = phaseHashR31(db, domain, spec.revision, UpgradePhaseR31.VERIFY_SOURCE);
                            if (!source.equals(recorded)) result[1] = "SOURCE_CHANGED_BETWEEN_PHASES_REBASED";
                            spec.transform.run(db);
                            cutPointR31(domain, phase.name(), "TRANSFORM_BODY_DONE");
                            java.util.Map<String, String> afterTables = tableHashesR31(db, columns);
                            String after = rootsHashR31(afterTables);
                            if (!after.equals(source)) throw new IllegalStateException("R31_UPGRADE_ROOT_CHANGED:" + changedRootsR31(sourceTables, afterTables));
                            result[0] = after; break;
                        }
                        case VERIFY_TARGET: {
                            String target = rootsHashR31(tableHashesR31(db, sourceColumns[0]));
                            String transformed = phaseHashR31(db, domain, spec.revision, UpgradePhaseR31.TRANSFORM);
                            result[1] = target.equals(transformed) ? "ROOTS_EQUAL_TRANSFORM" : "ROOTS_ADVANCED_BY_LIVE_WRITERS_AFTER_TRANSFORM";
                            result[0] = V51ChecksumEngine.sha256("VERIFY_TARGET|" + transformed + '|' + objectCensusR31(db, spec.roots));
                            break;
                        }
                        case PUBLISH: {
                            markUpgradeR31(db, spec, "PUBLISHED", "PUBLISH", "");
                            result[0] = V51ChecksumEngine.sha256("PUBLISH|" + before); break;
                        }
                        case CLEANUP: {
                            if (spec.cleanup != null) spec.cleanup.run(db);
                            markUpgradeR31(db, spec, "READY", "COMPLETE", "");
                            result[0] = V51ChecksumEngine.sha256("CLEANUP|" + before); break;
                        }
                    }
                    if (!"BLOCKED_LOW_DISK".equals(result[0])) {
                        db.execSQL("UPDATE " + TABLE_JOURNAL + " SET status='COMPLETED',cursor=?,before_hash=?,after_hash=?,completed_at=?,error_code=''"
                                        + " WHERE domain=? AND revision=? AND step_id=?",
                                new Object[] {safe(result[1]), before, result[0], Long.valueOf(System.currentTimeMillis()), domain, Integer.valueOf(spec.revision), phase.name()});
                    }
                    cutPointR31(domain, phase.name(), "PHASE_BEFORE_COMMIT");
                });
            } catch (Exception failure) {
                writer.write("R31_UPGRADE_" + phase + "_FAILED", db -> {
                    db.execSQL("UPDATE " + TABLE_JOURNAL + " SET status='FAILED',cursor='RETRY',completed_at=?,error_code=? WHERE domain=? AND revision=? AND step_id=?",
                            new Object[] {Long.valueOf(System.currentTimeMillis()), error(failure), domain, Integer.valueOf(spec.revision), phase.name()});
                    markUpgradeR31(db, spec, "FAILED", phase.name(), error(failure));
                });
                throw failure;
            }
            if ("BLOCKED_LOW_DISK".equals(result[0])) {
                java.util.Map<String, String> partial = new java.util.LinkedHashMap<>(done);
                return new UpgradeOutcomeR31("BLOCKED_LOW_DISK", domain, partial, attemptsR31(writer, domain, spec.revision), forwardCompatibilityR31(false));
            }
            cutPointR31(domain, phase.name(), "PHASE_COMMITTED");
            done.put(phase.name(), result[0]);
            if (phase == UpgradePhaseR31.VERIFY_SOURCE) sourceColumns[0] = result[1];
        }
        return new UpgradeOutcomeR31("COMPLETE", domain, done, attemptsR31(writer, domain, spec.revision), forwardCompatibilityR31(true));
    }

    private static void markUpgradeR31(SQLiteDatabase db, UpgradeSpecR31 spec, String state, String cursor, String error) {
        Long now = Long.valueOf(System.currentTimeMillis());
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_DOMAIN + "(domain,revision,contract_hash,fingerprint,state,cursor,started_at,updated_at,last_error) VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[] {spec.domain(), Integer.valueOf(spec.revision), V51ChecksumEngine.sha256(spec.contractHash + '|' + spec.id), spec.id, safe(state), safe(cursor), now, now, bounded(error, 900)});
        db.execSQL("UPDATE " + TABLE_DOMAIN + " SET state=?,cursor=?,updated_at=?,last_error=? WHERE domain=?",
                new Object[] {safe(state), safe(cursor), now, bounded(error, 900), spec.domain()});
    }
    private static java.util.Map<String, String> completedPhasesR31(SQLiteDatabase db, String domain, int revision) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        try (Cursor c = db.rawQuery("SELECT step_id,after_hash FROM " + TABLE_JOURNAL + " WHERE domain=? AND revision=? AND status='COMPLETED'",
                new String[] {domain, String.valueOf(revision)})) { while (c.moveToNext()) out.put(c.getString(0), c.getString(1)); }
        java.util.Map<String, String> ordered = new java.util.LinkedHashMap<>();
        for (UpgradePhaseR31 p : UpgradePhaseR31.values()) { if (!out.containsKey(p.name())) break; ordered.put(p.name(), out.get(p.name())); }
        return ordered;
    }
    private static String previousPhaseHashR31(SQLiteDatabase db, String domain, int revision, UpgradePhaseR31 phase) {
        if (phase.ordinal() == 0) return "";
        return phaseHashR31(db, domain, revision, UpgradePhaseR31.values()[phase.ordinal() - 1]);
    }
    private static String phaseHashR31(SQLiteDatabase db, String domain, int revision, UpgradePhaseR31 phase) {
        try (Cursor c = db.rawQuery("SELECT after_hash FROM " + TABLE_JOURNAL + " WHERE domain=? AND revision=? AND step_id=? AND status='COMPLETED'",
                new String[] {domain, String.valueOf(revision), phase.name()})) { return c.moveToFirst() ? safe(c.getString(0)) : ""; }
    }
    private static String cursorR31(WriterR31 writer, final String domain, final int revision, final UpgradePhaseR31 phase) throws Exception {
        final String[] out = {""};
        writer.write("R31_UPGRADE_READ_CURSOR", db -> { try (Cursor c = db.rawQuery("SELECT cursor FROM " + TABLE_JOURNAL + " WHERE domain=? AND revision=? AND step_id=?",
                new String[] {domain, String.valueOf(revision), phase.name()})) { if (c.moveToFirst()) out[0] = safe(c.getString(0)); } });
        return out[0];
    }
    private static java.util.Map<String, Integer> attemptsR31(WriterR31 writer, final String domain, final int revision) throws Exception {
        final java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        writer.write("R31_UPGRADE_READ_ATTEMPTS", db -> { try (Cursor c = db.rawQuery("SELECT step_id,attempt FROM " + TABLE_JOURNAL + " WHERE domain=? AND revision=?",
                new String[] {domain, String.valueOf(revision)})) { while (c.moveToNext()) out.put(c.getString(0), c.getInt(1)); } });
        return out;
    }
    /** "table:col1,col2;table2:..." for the roots that exist now (absent roots hash as absent). */
    static String rootColumnsR31(SQLiteDatabase db, String[] roots) {
        StringBuilder b = new StringBuilder();
        for (String table : roots) {
            if (!isPlainIdentifierR31(table) || !schemaObjectExists(db, table)) continue;
            List<String> columns = new ArrayList<>();
            try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) { while (c.moveToNext()) columns.add(c.getString(1)); }
            if (b.length() > 0) b.append(';');
            b.append(table).append(':').append(join(columns));
        }
        return b.toString();
    }
    private static boolean isPlainIdentifierR31(String name) {
        if (name == null || name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) { char ch = name.charAt(i); if (!(ch == '_' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9')) return false; }
        return true;
    }
    /** Hash of every original row value of every root, read in bounded rowid pages. */
    static java.util.Map<String, String> tableHashesR31(SQLiteDatabase db, String columns) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String entry : safe(columns).isEmpty() ? new String[0] : columns.split(";")) out.put(entry.substring(0, entry.indexOf(':')), tableHashR31(db, entry));
        return out;
    }
    static String rootsHashR31(java.util.Map<String, String> tables) {
        StringBuilder all = new StringBuilder("R31_ROOTS_V1");
        for (java.util.Map.Entry<String, String> e : tables.entrySet()) all.append('|').append(e.getKey()).append('=').append(e.getValue());
        return V51ChecksumEngine.sha256(all.toString());
    }
    private static String tableHashR31(SQLiteDatabase db, String entry) {
        String table = entry.substring(0, entry.indexOf(':'));
        String[] names = entry.substring(entry.indexOf(':') + 1).split(",");
        for (String n : names) if (!isPlainIdentifierR31(n)) throw new IllegalStateException("R31_UPGRADE_ROOT_COLUMN_NAME:" + table);
        java.security.MessageDigest digest;
        try { digest = java.security.MessageDigest.getInstance("SHA-256"); } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        if (!schemaObjectExists(db, table)) return "ABSENT";
        StringBuilder select = new StringBuilder("SELECT rowid");
        for (String n : names) select.append(',').append(n);
        select.append(" FROM ").append(table).append(" WHERE rowid>? ORDER BY rowid LIMIT 256");
        long last = Long.MIN_VALUE, rows = 0;
        while (true) {
            int page = 0;
            try (Cursor c = db.rawQuery(select.toString(), new String[] {String.valueOf(last)})) {
                while (c.moveToNext()) {
                    page++; rows++; last = c.getLong(0);
                    StringBuilder row = new StringBuilder().append(last);
                    for (int i = 1; i <= names.length; i++) {
                        int type = c.getType(i); row.append('|').append(type).append(':');
                        if (type == Cursor.FIELD_TYPE_BLOB) { digest.update(row.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); row.setLength(0); byte[] blob = c.getBlob(i); digest.update(blob); row.append('#').append(blob.length); }
                        else if (type == Cursor.FIELD_TYPE_INTEGER) row.append(c.getLong(i));
                        else if (type == Cursor.FIELD_TYPE_FLOAT) row.append(Double.doubleToLongBits(c.getDouble(i)));
                        else if (type != Cursor.FIELD_TYPE_NULL) { String v = c.getString(i); row.append(v.length()).append('~').append(v); }
                    }
                    digest.update(row.append('\n').toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (page < 256) break;
        }
        byte[] out = digest.digest(); StringBuilder hex = new StringBuilder(rows + "/");
        for (byte x : out) hex.append(String.format(java.util.Locale.ROOT, "%02x", x & 0xff));
        return hex.toString();
    }
    private static String changedRootsR31(java.util.Map<String, String> before, java.util.Map<String, String> after) {
        List<String> changed = new ArrayList<>();
        for (java.util.Map.Entry<String, String> e : before.entrySet()) if (!e.getValue().equals(after.get(e.getKey()))) changed.add(e.getKey());
        return join(changed);
    }
    private static String objectCensusR31(SQLiteDatabase db, String[] roots) {
        StringBuilder b = new StringBuilder();
        for (String table : roots) b.append(table).append('=').append(schemaObjectExists(db, table) ? 1 : 0).append(';');
        return b.toString();
    }

    public static String fingerprint(SQLiteDatabase db, Contract contract) {
        if(db==null||contract==null)return "";
        return LegacyDdlR31.scopedFingerprint(db,contract.contractHash,()->fingerprint(db, contract, false));
    }

    private static String fingerprintWithoutResearchPinDrawTriggers(SQLiteDatabase db, Contract contract) {
        return fingerprint(db, contract, true);
    }

    private static String fingerprint(SQLiteDatabase db, Contract contract, boolean excludeResearchPinDrawTriggers) {
        return fingerprint(db, contract, excludeResearchPinDrawTriggers, null);
    }

    private static String fingerprint(SQLiteDatabase db, Contract contract, boolean excludeResearchPinDrawTriggers,
                                      Set<String> declaredDrawTriggerDelta) {
        return fingerprint(db,contract,excludeResearchPinDrawTriggers,declaredDrawTriggerDelta,null);
    }

    private static String fingerprint(SQLiteDatabase db, Contract contract, boolean excludeResearchPinDrawTriggers,
                                      Set<String> declaredDrawTriggerDelta,Set<String> declaredOwnerObjects) {
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
            // Ordinary identifiers take the direct membership path. Edge controls, NUL,
            // NULL and non-TEXT values are retained for the authoritative Java trim/filter.
            // This is a lossless prefilter; it never caches a catalogue or a READY decision.
            sql += " AND (name COLLATE BINARY IN (" + marks + ")"
                    + " OR tbl_name COLLATE BINARY IN (" + marks + ")"
                    + " OR unicode(substr(name,1,1)) BETWEEN 1 AND 32"
                    + " OR unicode(substr(name,-1,1)) BETWEEN 1 AND 32"
                    + " OR unicode(substr(tbl_name,1,1)) BETWEEN 1 AND 32"
                    + " OR unicode(substr(tbl_name,-1,1)) BETWEEN 1 AND 32"
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
                if (declaredOwnerObjects != null && declaredOwnerObjects.contains(name)) continue;
                if (excludeResearchPinDrawTriggers && "trigger".equals(safe(c.getString(0)))
                        && "draws".equals(table)
                        && (declaredDrawTriggerDelta != null ? declaredDrawTriggerDelta.contains(name)
                            : "r30r1_pin_draws_insert".equals(name) || "r30r1_pin_draws_update".equals(name))) continue;
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
        Set<String> present = existingSchemaObjects(db, contract);
        for (String name : contract.objects)
            if (present == null ? !schemaObjectExists(db, name) : !present.contains(name)) missing.add(name);
        if (!missing.isEmpty()) throw new IllegalStateException("R926_" + contract.domain + "_OBJECTS_MISSING:" + join(missing));
    }

    private static boolean allObjectsPresent(SQLiteDatabase db, Contract contract) {
        if (db == null || contract == null) return false;
        Set<String> present = existingSchemaObjects(db, contract);
        for (String name : contract.objects)
            if (present == null ? !schemaObjectExists(db, name) : !present.contains(name)) return false;
        return true;
    }

    /** Fresh bounded membership query, never a cache. Keep the original per-name path for
     * unusual identifiers and oversized contracts so their SQLite equality semantics stay intact. */
    private static Set<String> existingSchemaObjects(SQLiteDatabase db, Contract contract) {
        if (contract.objects.length == 0) return Collections.emptySet();
        if (contract.objects.length > 499) return null;
        StringBuilder marks = new StringBuilder();
        for (String name : contract.objects) {
            if (name == null || name.length() == 0) return null;
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                        || (ch >= '0' && ch <= '9') || ch == '_')) return null;
            }
            if (marks.length() > 0) marks.append(',');
            marks.append('?');
        }
        Set<String> present = new HashSet<String>();
        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE name COLLATE BINARY IN ("
                + marks + ")", contract.objects)) {
            while (c.moveToNext()) present.add(c.getString(0));
        }
        return present;
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
