package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Full GENESIS_NEW asset invention, freeze, settlement, evidence and lifecycle engine.
 *
 * <p>It is additive and Research-only. Every candidate starts with N=0, freezes a complete action
 * observation before the target draw, updates only from authoritative settlement, and can reach
 * the certified store only after chronological multi-metric evidence. Legacy assets are protected
 * by {@link LegacyIntegrityGuardR915} and never become mutation targets.</p>
 */
public final class AssetGenesisEngineR915 {
    public static final String VERSION = "R915_ASSET_GENESIS_V4_1";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    public static final boolean MAY_WRITE_LEGACY = false;
    /** Every active candidate must be affordable on every one-minute draw; dormant bulk is banned. */
    public static final int MAX_ACTIVE_ASSETS = 8;
    public static final int MAX_CAPTURE_ASSETS = 8;
    public static final int GENERATION_INTERVAL_SETTLED = 24;
    public static final int MIN_CERTIFICATE_N = 240;

    public static final String TABLE_LEGACY_REGISTRY = "research_legacy_fixed_registry_v41";
    public static final String TABLE_ATOM_SCHEMA = "research_atom_schema_v41";
    public static final String TABLE_ATOM_PROPOSAL = "research_new_atom_proposal_v41";
    public static final String TABLE_ASSET_GENOME = "research_new_asset_genome_v41";
    public static final String TABLE_ASSET_FREEZE = "research_new_asset_freeze_v41";
    public static final String TABLE_ASSET_CERTIFICATE = "research_new_asset_certificate_v41";
    public static final String TABLE_EVOLUTION_EVENT = "research_evolution_event_v41";

    private AssetGenesisEngineR915() {}

    public static void ensureSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R915_GENESIS_DB_NULL");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LEGACY_REGISTRY + "("
                + "asset_key TEXT PRIMARY KEY,name_zh TEXT NOT NULL,owner_class TEXT NOT NULL UNIQUE,"
                + "output_schema TEXT NOT NULL,consumer TEXT NOT NULL,source_digest TEXT NOT NULL,"
                + "semantic_digest TEXT NOT NULL,db_contract_digest TEXT NOT NULL,"
                + "baseline_behavior_digest TEXT NOT NULL,locked INTEGER NOT NULL CHECK(locked=1),"
                + "registered_ms INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATOM_SCHEMA + "("
                + "atom_id TEXT PRIMARY KEY,group_id TEXT NOT NULL,name TEXT NOT NULL UNIQUE,"
                + "datatype TEXT NOT NULL,value_range TEXT NOT NULL,window_spec TEXT NOT NULL,"
                + "max_lag INTEGER NOT NULL,min_support INTEGER NOT NULL,evidence_stage TEXT NOT NULL,"
                + "leakage_class TEXT NOT NULL,update_timing TEXT NOT NULL,"
                + "version INTEGER NOT NULL,cost_class TEXT NOT NULL,enabled INTEGER NOT NULL,"
                + "semantic_digest TEXT NOT NULL UNIQUE)");
        ensureColumn(db,TABLE_ATOM_SCHEMA,"value_range","TEXT NOT NULL DEFAULT 'UNKNOWN'");
        ensureColumn(db,TABLE_ATOM_SCHEMA,"window_spec","TEXT NOT NULL DEFAULT 'UNKNOWN'");
        ensureColumn(db,TABLE_ATOM_SCHEMA,"max_lag","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(db,TABLE_ATOM_SCHEMA,"min_support","INTEGER NOT NULL DEFAULT 1");
        ensureColumn(db,TABLE_ATOM_SCHEMA,"evidence_stage","TEXT NOT NULL DEFAULT 'UNCLASSIFIED'");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATOM_PROPOSAL + "("
                + "proposal_id TEXT PRIMARY KEY,namespace TEXT NOT NULL CHECK(namespace='ATOM_PROPOSAL'),"
                + "proposed_name TEXT NOT NULL,value_type TEXT NOT NULL,definition_spec TEXT NOT NULL,"
                + "source_atom_ids TEXT NOT NULL,leakage_class TEXT NOT NULL,update_timing TEXT NOT NULL,"
                + "value_range TEXT NOT NULL,max_lag INTEGER NOT NULL,cost_class TEXT NOT NULL,"
                + "consumer_family TEXT NOT NULL,failure_vector TEXT NOT NULL,reducibility_evidence TEXT NOT NULL,"
                + "status TEXT NOT NULL,reject_reason TEXT NOT NULL DEFAULT '',future_n INTEGER NOT NULL DEFAULT 0,"
                + "incremental_info REAL NOT NULL DEFAULT 0,activation_schema_version TEXT NOT NULL DEFAULT '',"
                + "semantic_digest TEXT NOT NULL UNIQUE,created_issue TEXT NOT NULL DEFAULT '',"
                + "created_ms INTEGER NOT NULL,updated_ms INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_r915_atom_proposal_name ON "
                + TABLE_ATOM_PROPOSAL + "(proposed_name)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r915_atom_proposal_state ON "
                + TABLE_ATOM_PROPOSAL + "(status,created_ms DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSET_GENOME + "("
                + "asset_id TEXT PRIMARY KEY,namespace TEXT NOT NULL CHECK(namespace='GENESIS_NEW'),"
                + "display_name_zh TEXT NOT NULL,mechanism_spec_zh TEXT NOT NULL,genome_spec TEXT NOT NULL,"
                + "structural_hash TEXT NOT NULL UNIQUE,behavior_hash TEXT NOT NULL UNIQUE,"
                + "family_id TEXT NOT NULL,output_type TEXT NOT NULL,generation INTEGER NOT NULL,"
                + "parent_ids TEXT NOT NULL DEFAULT '',creation_reason TEXT NOT NULL,"
                + "trained_until_issue TEXT NOT NULL DEFAULT '',earliest_use_issue TEXT NOT NULL DEFAULT '',"
                + "state_blob TEXT NOT NULL,state_updates INTEGER NOT NULL DEFAULT 0,"
                + "status TEXT NOT NULL,may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0),"
                + "future_n INTEGER NOT NULL DEFAULT 0,future_hit INTEGER NOT NULL DEFAULT 0,"
                + "future_hit_rate REAL NOT NULL DEFAULT 0,future_wilson_lower REAL NOT NULL DEFAULT 0,"
                + "future_brier REAL NOT NULL DEFAULT 1,future_ece REAL NOT NULL DEFAULT 1,"
                + "future_coverage REAL NOT NULL DEFAULT 0,future_block_min REAL NOT NULL DEFAULT 0,"
                + "future_regime_min REAL NOT NULL DEFAULT 0,future_max_miss INTEGER NOT NULL DEFAULT 0,"
                + "paired_mainline_n INTEGER NOT NULL DEFAULT 0,paired_mainline_delta REAL NOT NULL DEFAULT 0,"
                + "incremental_info REAL NOT NULL DEFAULT 0,redundancy REAL NOT NULL DEFAULT 1,"
                + "fdr_q REAL NOT NULL DEFAULT 1,evidence_hash TEXT NOT NULL DEFAULT '',"
                + "lifecycle_reason TEXT NOT NULL DEFAULT '',created_ms INTEGER NOT NULL,updated_ms INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r915_asset_lifecycle ON " + TABLE_ASSET_GENOME
                + "(status,future_wilson_lower DESC,paired_mainline_delta DESC,updated_ms)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSET_FREEZE + "("
                + "asset_id TEXT NOT NULL,target_issue TEXT NOT NULL,asset_version_hash TEXT NOT NULL,"
                + "base_issue TEXT NOT NULL,history_hash TEXT NOT NULL,surface_hash TEXT NOT NULL,"
                + "history_rows INTEGER NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,"
                + "n3 INTEGER NOT NULL,action_ordinal INTEGER NOT NULL,score REAL NOT NULL,confidence REAL NOT NULL,"
                + "risk REAL NOT NULL,stability REAL NOT NULL,output_type TEXT NOT NULL DEFAULT 'SCALAR_SCORE',"
                + "output_shape TEXT NOT NULL DEFAULT '1',output_blob TEXT NOT NULL DEFAULT '',"
                + "output_hash TEXT NOT NULL DEFAULT '',context_blob TEXT NOT NULL,context_digest TEXT NOT NULL,"
                + "record_hash TEXT NOT NULL UNIQUE,frozen_ms INTEGER NOT NULL,actual_number INTEGER NOT NULL DEFAULT 0,"
                + "hit_label INTEGER NOT NULL DEFAULT -1 CHECK(hit_label IN(-1,0,1)),base_hit INTEGER NOT NULL DEFAULT -1,"
                + "brier REAL NOT NULL DEFAULT 0,settled_ms INTEGER NOT NULL DEFAULT 0,outcome_hash TEXT NOT NULL DEFAULT '',"
                + "PRIMARY KEY(asset_id,target_issue))");
        ensureColumn(db,TABLE_ASSET_FREEZE,"output_type","TEXT NOT NULL DEFAULT 'SCALAR_SCORE'");
        ensureColumn(db,TABLE_ASSET_FREEZE,"output_shape","TEXT NOT NULL DEFAULT '1'");
        ensureColumn(db,TABLE_ASSET_FREEZE,"output_blob","TEXT NOT NULL DEFAULT ''");
        ensureColumn(db,TABLE_ASSET_FREEZE,"output_hash","TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r915_asset_freeze_pending ON " + TABLE_ASSET_FREEZE
                + "(target_issue,hit_label,asset_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSET_CERTIFICATE + "("
                + "certificate_id TEXT PRIMARY KEY,asset_id TEXT NOT NULL,asset_version_hash TEXT NOT NULL,"
                + "display_name_zh TEXT NOT NULL,evidence_start_issue TEXT NOT NULL,evidence_end_issue TEXT NOT NULL,"
                + "future_n INTEGER NOT NULL,future_hit_rate REAL NOT NULL,wilson_lower REAL NOT NULL,"
                + "brier REAL NOT NULL,ece REAL NOT NULL,coverage REAL NOT NULL,block_min REAL NOT NULL,"
                + "regime_min REAL NOT NULL,max_miss INTEGER NOT NULL,paired_n INTEGER NOT NULL,paired_delta REAL NOT NULL,"
                + "incremental_info REAL NOT NULL,redundancy REAL NOT NULL,fdr_q REAL NOT NULL,"
                + "status TEXT NOT NULL,revocation_code TEXT NOT NULL DEFAULT '',evidence_hash TEXT NOT NULL,"
                + "created_ms INTEGER NOT NULL,updated_ms INTEGER NOT NULL,UNIQUE(asset_id,asset_version_hash))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r915_asset_certificate ON " + TABLE_ASSET_CERTIFICATE
                + "(status,wilson_lower DESC,paired_delta DESC,future_n DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EVOLUTION_EVENT + "("
                + "event_id TEXT PRIMARY KEY,event_issue TEXT NOT NULL,entity_type TEXT NOT NULL,entity_id TEXT NOT NULL,"
                + "operation TEXT NOT NULL,parent_ids TEXT NOT NULL,child_ids TEXT NOT NULL,failure_vector TEXT NOT NULL,"
                + "need_plan TEXT NOT NULL,created_ms INTEGER NOT NULL,record_hash TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r915_evolution_type ON " + TABLE_EVOLUTION_EVENT
                + "(entity_type,created_ms DESC)");
        CertifiedResearchMixerR915.ensureSchema(db);
        seedImmutableRegistries(db);
        writeState(db,"r915_genesis_schema_ready","1");
        writeState(db,"r915_genesis_schema_fingerprint",GenesisSchemaRegistryR915.fingerprint());
        writeState(db,"r915_legacy_registry_fingerprint",LegacyFixedAssetRegistryR915.fingerprint());
    }

    private static void seedImmutableRegistries(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        // This table is R915 manifest metadata, not historical user evidence. Replacing its rows is
        // required so cover-install upgrades the earlier incomplete 14-row draft to all 133 locks.
        db.delete(TABLE_LEGACY_REGISTRY, null, null);
        for (LegacyFixedAssetRegistryR915.Entry entry : LegacyFixedAssetRegistryR915.entries()) {
            ResearchWorkBudgetR927.checkpoint();
            ContentValues value = new ContentValues();
            value.put("asset_key",entry.id); value.put("name_zh",entry.nameZh);
            value.put("owner_class",entry.sourceClass); value.put("output_schema",entry.output);
            value.put("consumer",entry.consumer); value.put("source_digest",entry.sourceHash);
            value.put("semantic_digest",V51ChecksumEngine.sha256("SEMANTIC|"+entry.canonical()));
            value.put("db_contract_digest",V51ChecksumEngine.sha256("DB_READ_ONLY|"+entry.id));
            value.put("baseline_behavior_digest",V51ChecksumEngine.sha256("BASELINE|"+entry.canonical()));
            value.put("locked",1); value.put("registered_ms",now);
            db.insertOrThrow(TABLE_LEGACY_REGISTRY,null,value);
        }
        for (GenesisSchemaRegistryR915.AtomSpec atom : GenesisSchemaRegistryR915.atoms()) {
            ResearchWorkBudgetR927.checkpoint();
            ContentValues value = new ContentValues();
            value.put("atom_id",atom.id); value.put("group_id",atom.group); value.put("name",atom.name);
            value.put("datatype",atom.valueType); value.put("value_range",atom.valueRange);
            value.put("window_spec",atom.windowSpec); value.put("max_lag",atom.maxLag);
            value.put("min_support",atom.minSupport); value.put("evidence_stage",atom.evidenceStage);
            value.put("leakage_class",atom.leakagePolicy); value.put("update_timing",atom.updateTiming);
            value.put("version",atom.version); value.put("cost_class",atom.costClass); value.put("enabled",1);
            value.put("semantic_digest",V51ChecksumEngine.sha256(atom.canonical()));
            db.insertWithOnConflict(TABLE_ATOM_SCHEMA,null,value,SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /** Failure-driven generation. Existing Legacy owners are never parents or mutation targets. */
    public static String maintain(final DbStore store, long budgetMs) {
        if(store==null)return "STORE_NULL";
        auditLegacyHeads(store);
        LegacyIntegrityGuardR915.Result integrity=LegacyIntegrityGuardR915.verify(store);
        if(!integrity.passed)return publish(store,"YIELD_LEGACY_INTEGRITY_FAIL:"+integrity.violations);
        return publish(store,DirectedResearchCreationR932.maintain(store,"genesis"));
    }

    /** Continue at most two legacy heads, eight original settlements each, under this owner. */
    public static int auditLegacyHeads(final DbStore store) {
        if(store==null||RealtimePressureGateR811.hasActiveCriticalWork())return 0;
        Integer result=store.coordinatorR36().runWrite("R80_GENESIS_ORIGINAL_HEAD_AUDIT",new DatabaseCoordinator.WriteCallable<Integer>() {
            @Override public Integer call(SQLiteDatabase db) {
                boolean owns=!db.inTransaction();if(owns)db.beginTransaction();
                try {
                    LegacySettlementProofR79.cleanupTerminalSnapshots(db);
                    java.util.ArrayList<String> ids=new java.util.ArrayList<>();
                    try(Cursor c=db.rawQuery("SELECT g.asset_id FROM "+TABLE_ASSET_GENOME+" g LEFT JOIN "+LegacySettlementProofR79.HEAD_AUDIT+" a ON a.asset_id=g.asset_id WHERE (a.asset_id IS NULL OR a.status IN('COPYING','RUNNING','STALE')) AND NOT ("+LegacySettlementProofR79.assetHeadEligibleSql("g.asset_id")+") ORDER BY CASE WHEN a.status='RUNNING' THEN 0 ELSE 1 END,g.asset_id LIMIT 2",null)) {while(c.moveToNext())ids.add(c.getString(0));}
                    int verified=0;
                    for(String id:ids){ResearchWorkBudgetR927.checkpoint();if("VERIFIED".equals(LegacySettlementProofR79.auditAssetHead(db,id))){refreshEvidence(db,id);verified++;}}
                    if(owns)db.setTransactionSuccessful();return verified;
                }finally{if(owns)db.endTransaction();}
            }
        },Integer.valueOf(0));
        return result==null?0:result;
    }

    static boolean registerDirected(SQLiteDatabase db,GenesisAssetGenomeR915 genome,String trained,String earliest){
        return insertGenome(db,genome,trained,earliest,System.currentTimeMillis());
    }

    /**
     * Persist an add-only L0 proposal without changing the meaning or index of any existing atom.
     *
     * <p>A genuinely new primitive cannot be hot-activated by an unsigned database row.  Accepted
     * proposals therefore stop at {@code AWAITING_SIGNED_SCHEMA}; a later signed build must supply
     * the executable implementation and then gather Future OOS evidence through an AssetGenome.
     * This boundary prevents a database mutation from silently changing historical PIT semantics.</p>
     */
    public static String proposeNewAtom(
            final DbStore store,
            String proposedName,
            String valueType,
            String definitionSpec,
            List<String> sourceAtomIds,
            String leakageClass,
            String updateTiming,
            String valueRange,
            int maxLag,
            String costClass,
            String consumerFamily,
            String failureVector,
            String reducibilityEvidence,
            String createdIssue) {
        if (store == null) return "STORE_NULL";
        LegacyIntegrityGuardR915.assertEvolutionTarget("ATOM_PROPOSAL","PROPOSE");
        final String name=safe(proposedName),type=safe(valueType),definition=safe(definitionSpec);
        final String leakage=safe(leakageClass),timing=safe(updateTiming),range=safe(valueRange);
        final String cost=safe(costClass),family=safe(consumerFamily),failure=safe(failureVector);
        final String reducibility=safe(reducibilityEvidence),issue=safe(createdIssue);
        final ArrayList<String> sources=normalizeAtomIds(sourceAtomIds);
        final ProposalValidation validation=validateAtomProposal(name,type,definition,sources,leakage,
                timing,range,maxLag,cost,family,reducibility);
        final String canonical=name+'|'+type+'|'+definition+'|'+join(sources)+'|'+leakage+'|'
                +timing+'|'+range+'|'+maxLag+'|'+cost+'|'+family+'|'+failure+'|'+reducibility;
        final String digest=V51ChecksumEngine.sha256("R915_NEW_ATOM_PROPOSAL|"+canonical);
        final String proposalId="GN-ATM-"+digest.substring(0,24);
        String displayName=name.length()==0?"invalid_atom_"+digest.substring(0,12):name;
        final String persistedName=displayName;
        String committed=store.coordinatorR36().runWrite("R915_NEW_ATOM_PROPOSAL",
                new DatabaseCoordinator.WriteCallable<String>() {
                    @Override public String call(SQLiteDatabase db) {
                        Cursor existing=null;
                        try {
                            existing=db.rawQuery("SELECT proposal_id,status FROM "+TABLE_ATOM_PROPOSAL
                                    +" WHERE semantic_digest=? LIMIT 1",new String[]{digest});
                            if(existing.moveToFirst())return "EXISTS:"+safe(existing.getString(1))+':'
                                    +safe(existing.getString(0));
                        } finally { if(existing!=null)existing.close(); }
                        long now=System.currentTimeMillis();
                        ContentValues value=new ContentValues();value.put("proposal_id",proposalId);
                        value.put("namespace","ATOM_PROPOSAL");value.put("proposed_name",persistedName);
                        value.put("value_type",type);value.put("definition_spec",definition);
                        value.put("source_atom_ids",join(sources));value.put("leakage_class",leakage);
                        value.put("update_timing",timing);value.put("value_range",range);
                        value.put("max_lag",maxLag);value.put("cost_class",cost);
                        value.put("consumer_family",family);value.put("failure_vector",failure);
                        value.put("reducibility_evidence",reducibility);
                        value.put("status",validation.accepted?"AWAITING_SIGNED_SCHEMA":"REJECTED_STATIC");
                        value.put("reject_reason",validation.reason);value.put("future_n",0);
                        value.put("incremental_info",0.0d);value.put("activation_schema_version","");
                        value.put("semantic_digest",digest);value.put("created_issue",issue);
                        value.put("created_ms",now);value.put("updated_ms",now);
                        long row=db.insertWithOnConflict(TABLE_ATOM_PROPOSAL,null,value,
                                SQLiteDatabase.CONFLICT_IGNORE);
                        if(row<0)return "CONFLICT_OR_DUPLICATE_NAME";
                        insertEvent(db,issue,"ATOM",proposalId,
                                validation.accepted?"NEW_ATOM_PROPOSAL":"REJECT_ATOM_PROPOSAL","","",
                                failure,validation.accepted?reducibility:validation.reason,now);
                        return (validation.accepted?"AWAITING_SIGNED_SCHEMA:":"REJECTED_STATIC:")
                                +proposalId+(validation.accepted?"":":"+validation.reason);
                    }
                },"COMMIT_FAILED");
        String result=committed==null?"COMMIT_FAILED":committed;
        store.setState("r915_new_atom_proposal_status",result);
        store.setState("r915_new_atom_proposal_at",String.valueOf(System.currentTimeMillis()));
        return result;
    }

    private static String proposeFailureDrivenAtom(
            DbStore store,FailureVector failure,NeedPlan need,int generation,String createdIssue) {
        int pending=count(store,"SELECT COUNT(1) FROM "+TABLE_ATOM_PROPOSAL
                +" WHERE status='AWAITING_SIGNED_SCHEMA'");
        if(pending>=16)return "CAPACITY_WAIT:"+pending;
        String[] primitives={"ORDERED_EVENT_HYPEREDGE","CONDITIONAL_MEMORY_CURVATURE",
                "COUNTERFACTUAL_NEIGHBOR_FLUX","REGIME_CAUSAL_RESIDUAL",
                "UNCERTAINTY_TOPOLOGY_COUPLING","FAILURE_RECOVERY_TRANSPORT"};
        long seed=seedOf(failure.canonical()+'|'+need.canonical()+'|'+generation);
        String primitive=primitives[Math.floorMod((int)seed,primitives.length)];
        ArrayList<String> sourceIds=new ArrayList<String>();
        List<GenesisSchemaRegistryR915.AtomSpec> atoms=GenesisSchemaRegistryR915.atoms();
        int cursor=Math.floorMod((int)(seed^(seed>>>32)),atoms.size());
        for(int tries=0;tries<atoms.size()*2&&sourceIds.size()<5;tries++){
            ResearchWorkBudgetR927.checkpoint();
            GenesisSchemaRegistryR915.AtomSpec atom=atoms.get((cursor+tries*37)%atoms.size());
            if(atom.numeric()&&!sourceIds.contains(atom.id))sourceIds.add(atom.id);
        }
        String family=need.families.isEmpty()?"AF01":need.families.get(0);
        String definition="NEW_PRIMITIVE("+primitive+")|sources="+join(sourceIds)
                +"|lag=96|deterministic=1|pit=1";
        String shortDigest=V51ChecksumEngine.sha256(definition+'|'+failure.primary+'|'+generation)
                .substring(0,10).toLowerCase(Locale.US);
        String name="genesis_"+failure.primary.toLowerCase(Locale.US)+"_"+shortDigest;
        String reducibility="NEW_PRIMITIVE_REQUIRED:"+primitive
                +"|NOT_LOW_COST_EXPRESSIBLE_BY_V41_TYPED_GRAMMAR";
        return proposeNewAtom(store,name,"DOUBLE",definition,sourceIds,"PREDRAW_ONLY",
                "FREEZE_BEFORE_TARGET_DRAW","[0,1]",96,"MOBILE_MEDIUM",family,
                failure.canonical(),reducibility,createdIssue);
    }

    private static ProposalValidation validateAtomProposal(
            String name,String type,String definition,List<String> sources,String leakage,String timing,
            String range,int maxLag,String cost,String family,String reducibility) {
        if(!GenesisSchemaRegistryR915.validateNewAtomProposal(name,type,leakage,timing))
            return new ProposalValidation(false,"NAME_TYPE_OR_PIT_CONTRACT_INVALID");
        if(sources==null||sources.size()<2)return new ProposalValidation(false,"ATOMIC_PROVENANCE_TOO_SMALL");
        for(String id:sources){
            ResearchWorkBudgetR927.checkpoint();GenesisSchemaRegistryR915.AtomSpec atom=findAtom(id);
            if(atom==null)return new ProposalValidation(false,"SOURCE_ATOM_UNKNOWN:"+id);
            if(!atom.numeric())return new ProposalValidation(false,"SOURCE_ATOM_NOT_NUMERIC:"+id);}
        if(!definition.startsWith("NEW_PRIMITIVE(")||definition.length()<32)
            return new ProposalValidation(false,"DEFINITION_NOT_NEW_PRIMITIVE");
        String upper=definition.toUpperCase(Locale.US);
        String[] forbidden={"TARGET_DRAW","ACTUAL_NUMBER","OUTCOME_LABEL","HIT_LABEL",
                "POST_SETTLEMENT","FUTURE_DRAW","LOOKAHEAD"};
        for(String token:forbidden)if(upper.contains(token))
            return new ProposalValidation(false,"LEAKAGE_TOKEN:"+token);
        if(range.length()<3||maxLag<1||maxLag>512)
            return new ProposalValidation(false,"RANGE_OR_LAG_INVALID");
        if(!("MOBILE_LOW".equals(cost)||"MOBILE_MEDIUM".equals(cost)))
            return new ProposalValidation(false,"RESOURCE_COST_REJECTED");
        if(!isFamily(family))return new ProposalValidation(false,"CONSUMER_FAMILY_UNKNOWN");
        if(!reducibility.startsWith("NEW_PRIMITIVE_REQUIRED:")
                ||!reducibility.contains("NOT_LOW_COST_EXPRESSIBLE"))
            return new ProposalValidation(false,"REDUCIBILITY_EVIDENCE_MISSING");
        return new ProposalValidation(true,"");
    }

    /** Freeze a complete observation for every bounded active asset before the exact target draw. */
    public static int capturePredraw(
            final DbStore store, final V51RuntimeResult runtime, int maxAssets, long budgetMs) {
        if (store==null || runtime==null || !runtime.success || runtime.context==null
                || runtime.actions==null || runtime.actions.length!=K3ActionSpaceEngine.TOTAL_ACTIONS)
            return captureZero(store,"","RUNTIME_SURFACE_INVALID");
        final String base=safe(runtime.context.baseIssue), target=safe(runtime.context.targetIssue);
        if (!TargetIssueResolver.exact(base,target).valid)return captureZero(store,target,"ISSUE_NOT_EXACT");
        if(store.getDrawByIssue(target)!=null)return captureZero(store,target,"TARGET_ALREADY_DRAWN");
        if (RealtimePressureGateR811.hasActiveCriticalWork())return captureZero(store,target,"YIELD_P0_PRESSURE");
        GlobalResearchScheduleR932.prepareConsumer(store,target,runtime.checksum);
        K3ActionSpaceEngine.validateOrThrow(runtime.actions);
        int limit=Math.max(1,Math.min(96,maxAssets));
        List<AssetRow> assets=loadRunnable(store,target,limit);
        if(assets.isEmpty()){
            int existing=freezeCountForTarget(store,target);
            if(existing>0)return captureExisting(store,target,existing);
            return captureZero(store,target,"NO_RUNNABLE_GENESIS_ASSET");
        }
        java.util.ArrayList<String> dispatchedKeys=new java.util.ArrayList<String>();
        for(AssetRow row:assets)dispatchedKeys.add("genesis:"+row.genome.assetId);
        ResearchCaptureLedgerR932.markDispatched(store,target,dispatchedKeys,"GENESIS_R915");
        HistoricalStrategyInventionR908.Profile profile=GlobalResearchScheduleR932.profile(store,base);
        if (profile==null || !profile.ready)return captureZero(store,target,"PIT_PROFILE_NOT_READY");
        final long deadline=TimeSourceR811.monoNowMs()+Math.max(300L,Math.min(2200L,budgetMs));
        final GenesisScanR967.Surface surface=GenesisScanR967.acquireSurface(store,"LIVE_RUNTIME",base,target,
                runtime.checksum,runtime.context.historyHash,runtime.context.historyRows,runtime.elapsedMs,runtime.actions);
        if(!surface.prepare(new GenesisScanR967.ContextFactory(){
            public GenesisAssetVmR915.PredrawContext build(int i){return GenesisAssetVmR915.buildContext(profile,runtime,runtime.actions[i]);}
        },deadline))return captureZero(store,target,"CONTEXT_SLICE_YIELDED");
        return capturePrepared(store,target,runtime.actions,surface,assets,deadline,"LIVE_RUNTIME");
    }

    /** Crash-durable predraw recovery from the immutable canonical snapshot. */
    public static int capturePredraw(final DbStore store,
            final DeferredEvidenceSnapshotR36.Snapshot snapshot,int maxAssets,long budgetMs) {
        if(store==null||snapshot==null)return captureZero(store,"","SNAPSHOT_SURFACE_INVALID");
        final String base=safe(snapshot.baseIssue),target=safe(snapshot.targetIssue);
        if(!TargetIssueResolver.exact(base,target).valid)return captureZero(store,target,"SNAPSHOT_ISSUE_NOT_EXACT");
        if(store.getDrawByIssue(target)!=null)return captureZero(store,target,"SNAPSHOT_TARGET_ALREADY_DRAWN");
        if(RealtimePressureGateR811.hasActiveCriticalWork())return captureZero(store,target,"YIELD_P0_PRESSURE");
        GlobalResearchScheduleR932.prepareConsumer(store,target,snapshot.recordHash);
        final V51Models.PositionAction[] actions=snapshot.positionActions();
        K3ActionSpaceEngine.validateOrThrow(actions);
        int limit=Math.max(1,Math.min(96,maxAssets));
        List<AssetRow> assets=loadRunnable(store,target,limit);
        if(assets.isEmpty()){
            int existing=freezeCountForTarget(store,target);
            if(existing>0)return captureExisting(store,target,existing);
            return captureZero(store,target,"NO_RUNNABLE_GENESIS_ASSET");
        }
        java.util.ArrayList<String> dispatchedKeys=new java.util.ArrayList<String>();
        for(AssetRow row:assets)dispatchedKeys.add("genesis:"+row.genome.assetId);
        ResearchCaptureLedgerR932.markDispatched(store,target,dispatchedKeys,"GENESIS_R915");
        HistoricalStrategyInventionR908.Profile profile=GlobalResearchScheduleR932.profile(store,base);
        if(profile==null||!profile.ready)return captureZero(store,target,"SNAPSHOT_PIT_PROFILE_NOT_READY");
        int historyRows=count(store,"SELECT COUNT(1) FROM "+DbHelper.TABLE_DRAWS
                +" WHERE dirty=0 AND issue<='"+sqlLiteral(base)+"'");
        final long deadline=TimeSourceR811.monoNowMs()+Math.max(300L,Math.min(2200L,budgetMs));
        final GenesisScanR967.Surface surface=GenesisScanR967.acquireSurface(store,"DURABLE_SNAPSHOT",base,target,
                snapshot.recordHash,snapshot.historyHash,historyRows,0L,actions);
        if(!surface.prepare(new GenesisScanR967.ContextFactory(){
            public GenesisAssetVmR915.PredrawContext build(int i){return GenesisAssetVmR915.buildContext(profile,snapshot,historyRows,actions[i]);}
        },deadline))return captureZero(store,target,"CONTEXT_SLICE_YIELDED");
        return capturePrepared(store,target,actions,surface,assets,deadline,"DURABLE_SNAPSHOT");
    }

    private static int capturePrepared(final DbStore store,final String target,
            final V51Models.PositionAction[] actions,
            final GenesisScanR967.Surface surface,
            final List<AssetRow> assets,long deadline,String source) {
        if(assets==null||assets.isEmpty())throw new IllegalArgumentException("R951_GENESIS_RUNNABLE_REQUIRED");
        final ArrayList<Freeze> freezes=new ArrayList<Freeze>();
        java.util.Set<Integer> quarantined=AtomCaptureR932.quarantined(store);
        for (AssetRow row:assets) {
            ResearchWorkBudgetR927.checkpoint();
            if (TimeSourceR811.monoNowMs()>=deadline || RealtimePressureGateR811.hasActiveCriticalWork()) break;
            boolean proxy=GlobalResearchScheduleR932.proxy(store,target,"genesis:"+row.genome.assetId);
            // An admitted candidate that reaches its owner has been attempted even when a frozen
            // dependency is unavailable. Persist that attempt before dependency validation so one
            // impossible candidate cannot keep hasUnstartedAdmitted() true forever. This is NOT a
            // frozen/pass receipt: the follow-up status remains non-frozen and cannot satisfy the
            // completion/certificate gates.
            ResearchCaptureLedgerR932.record(store,target,"genesis:"+row.genome.assetId,"EXECUTION_STARTED",proxy?"PROXY120":"FULL1200",0);
            String missing=GenesisAtomDependencyR932.unavailable(row.genome,surface.first(),!proxy,quarantined);
            if(missing.length()>0){ResearchCaptureLedgerR932.record(store,target,"genesis:"+row.genome.assetId,"EXECUTION_INPUT_UNAVAILABLE",missing,0);continue;}
            GenesisScanR967 scan=surface.scan(row.genome,row.state,proxy);
            if(!scan.advance(row.genome,row.state,deadline))continue;
            String programBefore=scan.programBefore,stateBefore=scan.stateBefore;
            GenesisAssetVmR915.State replayState=scan.replayState;
            CandidateExecutionAuditR932.Builder audit=scan.audit;
            int best=scan.best;double bestScore=scan.bestScore;
            GenesisAssetVmR915.PredrawContext bestContext=scan.selectedContext();
            GenesisAssetVmR915.AssetValue bestValue=scan.bestValue;
            if(proxy){if(best>=0)GlobalResearchScheduleR932.saveProxy(store,target,"genesis:"+row.genome.assetId,bestContext.digest(),best,bestScore);surface.release(row.genome.assetId,true);continue;}
            if (best>=0 && bestContext!=null && bestValue!=null){
                GenesisAssetVmR915.AssetValue materialized=
                        GenesisAssetVmR915.evaluate(row.genome,row.state,bestContext);
                Freeze frozen=new Freeze(row,actions[best],bestContext,materialized);
                V51Models.PositionAction selected=actions[best];
                frozen.audit=audit.finish(K3ActionSpaceEngine.globalOrdinal(selected.position,selected.n1,selected.n2,selected.n3),
                    programBefore,row.genome.canonical(),stateBefore,row.state.encode(),replayState.encode());
                final Freeze completed=frozen;
                Boolean committed=store.coordinatorR36().runAtomicWrite("R943_RESEARCH_GENESIS_FREEZE_ONE",
                    new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){
                        return !drawExists(db,target)&&insertFreeze(db,completed);
                    }},Boolean.FALSE);
                if(Boolean.TRUE.equals(committed)){freezes.add(frozen);surface.release(row.genome.assetId,false);}
            }
        }
        if (freezes.isEmpty())return captureZero(store,target,"NO_FREEZE_WITHIN_BUDGET");
        int count=freezes.size();
        int total=freezeCountForTarget(store,target);
        if(count>0){store.setState("r915_genesis_predraw_target",target);
            store.setState("r915_genesis_predraw_frozen",String.valueOf(total));
            store.setState("r915_genesis_predraw_names",freezeNames(freezes));
            store.setState("r919_genesis_capture_status","FROZEN_NEW="+count+"|TOTAL="+total
                    +"|source="+source+"|target="+target);}
        else if(total>0)return captureExisting(store,target,total);
        else captureZero(store,target,"FREEZE_COMMIT_ZERO");
        return total;
    }

    private static int captureExisting(DbStore store,String target,int existing){if(store!=null)try{
        store.setState("r915_genesis_predraw_target",safe(target));
        store.setState("r915_genesis_predraw_frozen",String.valueOf(Math.max(0,existing)));
        store.setState("r919_genesis_capture_status","ALREADY_FROZEN_FIRST_WRITE="
                +Math.max(0,existing)+"|target="+safe(target));
        store.setState("r919_genesis_capture_at",String.valueOf(System.currentTimeMillis()));
    }catch(Throwable ignored){}return Math.max(0,existing);}

    private static int freezeCountForTarget(DbStore store,String target){Cursor c=null;try{
        c=store.readable().rawQuery("SELECT COUNT(1) FROM "+TABLE_ASSET_FREEZE
                +" WHERE target_issue=?",new String[]{safe(target)});
        return c.moveToFirst()?Math.max(0,c.getInt(0)):0;
    }catch(Throwable ignored){return 0;}finally{if(c!=null)c.close();}}

    private static int captureZero(DbStore store,String target,String reason){if(store!=null)try{
        store.setState("r919_genesis_capture_status",safe(reason)+"|target="+safe(target));
        store.setState("r919_genesis_capture_at",String.valueOf(System.currentTimeMillis()));
    }catch(Throwable ignored){}return 0;}

    /** Settle every pending new-asset freeze for one authoritative draw exactly once. */
    public static int settleDraw(final DbStore store, final Models.Draw draw) {
        if(store==null||draw==null||!draw.isValid())return 0;
        Integer changed=store.coordinatorR36().runWrite("R915_GENESIS_SETTLE",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        Cursor c=null;int settled=0;
                        try{
                            c=db.rawQuery("SELECT f.asset_id,f.asset_version_hash,f.context_blob,"
                                            +"f.context_digest,f.record_hash,f.score,g.genome_spec,g.state_blob "
                                            +"FROM "+TABLE_ASSET_FREEZE+" f JOIN "+TABLE_ASSET_GENOME
                                            +" g ON g.asset_id=f.asset_id WHERE f.target_issue=? AND f.hit_label=-1 "
                                            +"ORDER BY f.asset_id",new String[]{draw.issue});
                            while(c.moveToNext()){
                                ResearchWorkBudgetR927.checkpoint();
                                boolean owns=!db.inTransaction();if(owns)db.beginTransaction();
                                // The cursor above only schedules work. Re-read the frozen inputs
                                // and mutable head after acquiring this row's writer transaction.
                                try (Cursor row=db.rawQuery("SELECT f.asset_id,f.asset_version_hash,f.context_blob,"
                                        +"f.context_digest,f.record_hash,f.score,g.genome_spec,g.state_blob FROM "
                                        +TABLE_ASSET_FREEZE+" f JOIN "+TABLE_ASSET_GENOME+" g ON g.asset_id=f.asset_id"
                                        +" WHERE f.asset_id=? AND f.target_issue=? AND f.hit_label=-1",
                                        new String[]{c.getString(0),draw.issue})) {
                                if(!row.moveToFirst())continue;
                                String assetId=safe(row.getString(0)),version=safe(row.getString(1));
                                String blob=safe(row.getString(2)),digest=safe(row.getString(3));
                                LegacySettlementProofR79.Original original=LegacySettlementProofR79.original(db,"genesis:"+assetId,draw.issue);
                                String proofReason=LegacySettlementProofR79.verifyAssetSettlement(db,assetId,draw,original);
                                if(!"VALID".equals(proofReason)) {
                                    LegacySettlementProofR79.record(db,original,draw,"REJECTED",proofReason);
                                    if(owns)db.setTransactionSuccessful();
                                    continue;
                                }
                                boolean historicalOnly= !LegacySettlementProofR79.assetHeadEligible(db,assetId);
                                if(!V51ChecksumEngine.sha256("R915_FROZEN_ATOMS|"+blob).equals(digest))continue;
                                GenesisAssetGenomeR915 genome=GenesisAssetGenomeR915.parse(row.getString(6));
                                if(!assetId.equals(genome.assetId)||!version.equals(genome.structuralHash))continue;
                                GenesisAssetVmR915.PredrawContext context=GenesisAssetVmR915.PredrawContext.decodeFrozen(blob);
                                GenesisAssetVmR915.State state=GenesisAssetVmR915.State.decode(row.getString(7));
                                int actual=draw.getNumberAtPosition(context.position);
                                int hit=contains(actual,context.n1,context.n2,context.n3)?1:0;
                                int baseHit=baseHit(db,draw.issue);
                                double score=clamp01(row.getDouble(5));
                                GenesisAssetVmR915.SettlementResult update=historicalOnly
                                        ?new GenesisAssetVmR915.SettlementResult(false,"HISTORICAL_OUTCOME_ONLY_UNPROVEN_HEAD",state.hash(),state.hash())
                                        :GenesisAssetVmR915.settle(genome,state,context,draw.issue,actual,hit,score);
                                boolean lateLabel="EXACTLY_ONCE_OR_ORDER_REJECT".equals(update.code);
                                if(!update.updated&&!lateLabel&&!historicalOnly)continue;
                                double brier=-1.0d; // Legacy score is not a frozen calibrated probability.
                                String outcome=V51ChecksumEngine.sha256("R915_ASSET_OUTCOME|"+row.getString(4)
                                        +'|'+draw.issue+'|'+actual+'|'+hit+'|'+baseHit+'|'+brier+'|'+update.afterHash);
                                ContentValues fv=new ContentValues();fv.put("actual_number",actual);
                                fv.put("hit_label",hit);fv.put("base_hit",baseHit);fv.put("brier",brier);
                                fv.put("settled_ms",System.currentTimeMillis());fv.put("outcome_hash",outcome);
                                int rows=db.update(TABLE_ASSET_FREEZE,fv,"asset_id=? AND target_issue=? AND hit_label=-1",
                                        new String[]{assetId,draw.issue});
                                if(rows!=1)continue;
                                if(update.updated) {
                                    ContentValues gv=new ContentValues();gv.put("state_blob",state.encode());
                                    gv.put("state_updates",state.updates);gv.put("trained_until_issue",draw.issue);
                                    gv.put("earliest_use_issue",TargetIssueResolver.next(draw.issue).targetIssue);
                                    gv.put("updated_ms",System.currentTimeMillis());
                                    if(db.update(TABLE_ASSET_GENOME,gv,"asset_id=?",new String[]{assetId})!=1)throw new IllegalStateException("R943_GENESIS_STATE_UPDATE_MISSING");
                                } else if(!historicalOnly)db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE
                                        +"(state_key,state_value,updated_at) VALUES('r72_asset_late_label',?,?)",
                                        new Object[]{assetId+'|'+draw.issue+"|OUTCOME_ONLY_HEAD_PRESERVED",System.currentTimeMillis()});
                                LegacySettlementProofR79.record(db,original,draw,
                                        historicalOnly?"HISTORICAL_OUTCOME_ONLY":update.updated?"UPDATED_HEAD":"LATE_OUTCOME_ONLY","VALID");
                                if(!historicalOnly)refreshEvidence(db,assetId);
                                if(owns)db.setTransactionSuccessful();settled++;
                                } finally {if(owns)db.endTransaction();}
                            }
                        }finally{if(c!=null)c.close();}
                        return Integer.valueOf(settled);
                    }
                },Integer.valueOf(0));
        int count=changed==null?0:changed.intValue();
        if(count>0){store.setState("r915_genesis_last_settled_issue",draw.issue);
            store.setState("r915_genesis_last_settled_rows",String.valueOf(count));}
        return count;
    }

    public static String truthSummary(DbStore store) {
        if(store==null)return "GENESIS_NEW=STORE_NULL";
        int total=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_GENOME);
        int executable=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_GENOME
                +" WHERE genome_spec<>'' AND structural_hash<>'' AND behavior_hash<>''");
        int observing=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_GENOME+" WHERE status='OBSERVING_FUTURE_OOS'");
        int certified=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_GENOME+" WHERE status='CERTIFIED_FUTURE_OOS'");
        int pruned=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_GENOME+" WHERE status LIKE 'PRUNED%'");
        int rejected=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_GENOME+" WHERE status LIKE 'REJECTED%'");
        int uniqueBehavior=count(store,"SELECT COUNT(DISTINCT behavior_hash) FROM "+TABLE_ASSET_GENOME
                +" WHERE behavior_hash<>''");
        int frozen=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_FREEZE);
        int settled=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_FREEZE+" WHERE hit_label IN(0,1)");
        int settlementEligible=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_FREEZE
                +" f WHERE EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS
                +" d WHERE d.issue=f.target_issue AND d.dirty=0)");
        int atomProposals=count(store,"SELECT COUNT(1) FROM "+TABLE_ATOM_PROPOSAL);
        int atomAwaiting=count(store,"SELECT COUNT(1) FROM "+TABLE_ATOM_PROPOSAL
                +" WHERE status='AWAITING_SIGNED_SCHEMA'");
        int atomRejected=count(store,"SELECT COUNT(1) FROM "+TABLE_ATOM_PROPOSAL
                +" WHERE status='REJECTED_STATIC'");
        int leakageRejected=count(store,"SELECT COUNT(1) FROM "+TABLE_ATOM_PROPOSAL
                +" WHERE reject_reason LIKE 'LEAKAGE_TOKEN:%'");
        int certificates=count(store,"SELECT COUNT(1) FROM "+TABLE_ASSET_CERTIFICATE+" WHERE status='ACTIVE'");
        int contributions=count(store,"SELECT COUNT(1) FROM "+CertifiedResearchMixerR915.TABLE_CONTRIBUTION
                +" WHERE status='CERTIFIED_NEW_ASSET_INFLUENCE'");
        String firstEligible=scalarString(store,"SELECT MIN(earliest_use_issue) FROM "+TABLE_ASSET_GENOME
                +" WHERE earliest_use_issue<>''");
        int eligibleIssues=firstEligible.length()==0?0:count(store,"SELECT COUNT(DISTINCT target_issue) FROM "
                +DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 AND target_issue>='"
                +sqlLiteral(firstEligible)+"'");
        int frozenIssues=firstEligible.length()==0?0:count(store,"SELECT COUNT(DISTINCT target_issue) FROM "
                +TABLE_ASSET_FREEZE+" WHERE target_issue>='"+sqlLiteral(firstEligible)+"'");
        double freezeCoverage=eligibleIssues<=0?0.0d:clamp01(frozenIssues/(double)eligibleIssues);
        double settlementCoverage=settlementEligible<=0?0.0d:clamp01(settled/(double)settlementEligible);
        String best="";Cursor c=null;try{c=store.readable().rawQuery("SELECT display_name_zh,future_n,"
                +"future_hit_rate,future_wilson_lower,paired_mainline_delta,status FROM "+TABLE_ASSET_GENOME
                +" ORDER BY future_wilson_lower DESC,future_n DESC LIMIT 1",null);if(c.moveToFirst())
            best=safe(c.getString(0))+"|N="+c.getInt(1)+"|HR="+percent(c.getDouble(2))
                    +"|Wilson="+percent(c.getDouble(3))+"|paired="+percent(c.getDouble(4))
                    +"|"+safe(c.getString(5));}catch(Throwable ignored){}finally{if(c!=null)c.close();}
        return "GENESIS_NEW|atoms="+GenesisSchemaRegistryR915.atomCount()+"|families="
                +GenesisSchemaRegistryR915.familyCount()+"|atomProposals="+atomProposals
                +"|atomAwaitingSignedSchema="+atomAwaiting+"|atomRejected="+atomRejected
                +"|leakageRejected="+leakageRejected+"|generated="+total+"|executable="+executable
                +"|uniqueBehavior="+uniqueBehavior+"|cloneCount="+Math.max(0,total-uniqueBehavior)
                +"|observing="+observing+"|certified="+certified+"|pruned="+pruned+"|rejected="+rejected
                +"|freezeRows="+frozen+"|settledRows="+settled+"|settlementEligible="+settlementEligible
                +"|freezeIssueCoverage="+percent(freezeCoverage)+"("+frozenIssues+'/'+eligibleIssues+")"
                +"|settlementCoverage="+percent(settlementCoverage)+"("+settled+'/'+settlementEligible+")"
                +"|activeCertificates="+certificates+"|appliedContributions="+contributions+"|best="+best;
    }

    private static boolean insertGenome(SQLiteDatabase db,GenesisAssetGenomeR915 g,String trained,
            String earliest,long now){
        // E04A production boundary: every new molecule must reversibly resolve every explicit,
        // fixed-semantic and materialized-shape parent to the immutable L0 registry before it can
        // enter the persistent candidate population.
        GenesisMoleculeTraceR932.Trace moleculeTrace=GenesisMoleculeTraceR932.trace(g,true);
        if(!moleculeTrace.completeFor(g))
            throw new IllegalStateException("R932_GENESIS_MOLECULE_PARENT_TRACE_INCOMPLETE");
        ContentValues v=new ContentValues();v.put("asset_id",g.assetId);v.put("namespace",g.namespace());
        v.put("display_name_zh",g.mechanismNameZh);v.put("mechanism_spec_zh",g.mechanismSpecZh);
        v.put("genome_spec",g.canonical());v.put("structural_hash",g.structuralHash);
        v.put("behavior_hash",g.behaviorHash);v.put("family_id",g.familyId);v.put("output_type",g.outputType.name());
        v.put("generation",g.generation);v.put("parent_ids",g.parentIds);v.put("creation_reason",g.creationReason);
        v.put("trained_until_issue",safe(trained));v.put("earliest_use_issue",safe(earliest));
        v.put("state_blob",new GenesisAssetVmR915.State().encode());v.put("state_updates",0);
        v.put("status","IDEA_WAIT_FUTURE_OOS");v.put("may_authorize_real",0);
        v.put("future_n",0);v.put("future_hit",0);v.put("future_brier",1);v.put("future_ece",1);
        v.put("future_coverage",0);v.put("redundancy",1);v.put("fdr_q",1);
        v.put("lifecycle_reason","CHILD_EVIDENCE_RESET_N0");v.put("created_ms",now);v.put("updated_ms",now);
        return db.insertWithOnConflict(TABLE_ASSET_GENOME,null,v,SQLiteDatabase.CONFLICT_IGNORE)>=0;
    }

    private static boolean insertFreeze(SQLiteDatabase db,Freeze f){
        if(!LegacySettlementProofR79.assetHeadEligible(db,f.row.genome.assetId))
            throw new IllegalStateException("R79_GENESIS_PRIOR_HEAD_PROVENANCE_REQUIRED");
        String blob=f.context.encodeFrozen(),digest=f.context.digest();
        String record=V51ChecksumEngine.sha256("R915_ASSET_FREEZE|"+f.row.genome.assetId+'|'
                +f.context.targetIssue+'|'+f.row.genome.structuralHash+'|'+digest+'|'+f.value.valueHash);
        ContentValues v=new ContentValues();v.put("asset_id",f.row.genome.assetId);
        v.put("target_issue",f.context.targetIssue);v.put("asset_version_hash",f.row.genome.structuralHash);
        v.put("base_issue",f.context.baseIssue);v.put("history_hash",f.context.historyHash);
        v.put("surface_hash",f.context.surfaceHash);v.put("history_rows",f.context.historyRows);
        v.put("position",f.action.position);v.put("n1",f.action.n1);v.put("n2",f.action.n2);v.put("n3",f.action.n3);
        v.put("action_ordinal",K3ActionSpaceEngine.globalOrdinal(f.action.position,f.action.n1,f.action.n2,f.action.n3));
        v.put("score",f.value.primary);v.put("confidence",f.value.confidence);v.put("risk",f.value.risk);
        v.put("stability",f.value.stability);v.put("output_type",f.value.outputType);
        v.put("output_shape",f.value.outputShape);v.put("output_blob",f.value.encodeOutput());
        v.put("output_hash",f.value.valueHash);v.put("context_blob",blob);v.put("context_digest",digest);
        v.put("record_hash",record);v.put("frozen_ms",System.currentTimeMillis());
        long inserted=db.insertWithOnConflict(TABLE_ASSET_FREEZE,null,v,SQLiteDatabase.CONFLICT_IGNORE);
        if(inserted<0) {try(Cursor c=db.rawQuery("SELECT record_hash FROM "+TABLE_ASSET_FREEZE+" WHERE asset_id=? AND target_issue=?",new String[]{f.row.genome.assetId,f.context.targetIssue})){if(!c.moveToFirst()||!record.equals(c.getString(0)))throw new IllegalStateException("R932_GENESIS_FREEZE_CONFLICT");}}
        if(f.audit==null)throw new IllegalStateException("R932_GENESIS_FULL_REPLAY_REQUIRED");
        f.audit.freeze(db,"genesis:"+f.row.genome.assetId,f.row.genome.structuralHash,f.context.surfaceHash,
            f.context.baseIssue,f.context.targetIssue,record);
        FixedResearchWindowR932.freezePair(db,"genesis:"+f.row.genome.assetId,f.row.genome.structuralHash,f.context.targetIssue,record,f.action.position,f.action.n1,f.action.n2,f.action.n3,f.value.primary);
        AtomCaptureR932.freeze(db,f.context);
        ResearchCaptureLedgerR932.record(db,f.context.targetIssue,"genesis:"+f.row.genome.assetId,"FROZEN","RAW_SCORE_NOT_CALIBRATED",record,Math.max(1L,f.audit.elapsedNs/1000000L),true);
        return inserted>=0;
    }

    /** Compact raw-outcome summary. FixedResearchWindowR932 owns calibrated quality decisions;
     * decoding every historical 240-atom context on every label used to starve the state update. */
    private static void refreshEvidence(SQLiteDatabase db,String assetId){
        int n=0,hit=0,paired=0;double delta=0;String first="",last="";
        try(Cursor c=db.rawQuery("SELECT COUNT(*),COALESCE(SUM(hit_label),0),COALESCE(SUM(CASE WHEN base_hit IN(0,1) THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN base_hit IN(0,1) THEN hit_label-base_hit ELSE 0 END),0),MIN(target_issue),MAX(target_issue) FROM "+TABLE_ASSET_FREEZE+" f WHERE asset_id=? AND hit_label IN(0,1) AND "+LegacySettlementProofR79.assetOutcomeEligibleSql("f.asset_id","f.target_issue","f.record_hash"),new String[]{assetId})){
            if(c.moveToFirst()){n=c.getInt(0);hit=c.getInt(1);paired=c.getInt(2);delta=paired==0?0:c.getDouble(3)/paired;first=safe(c.getString(4));last=safe(c.getString(5));}
        }
        int eligible=eligibleFreezes(db,assetId);double rate=n==0?0d:hit/(double)n;
        ContentValues values=new ContentValues();values.put("future_n",n);values.put("future_hit",hit);
        values.put("future_hit_rate",rate);values.put("future_wilson_lower",wilsonLower(hit,n));
        values.put("future_coverage",eligible==0?0d:Math.min(1d,n/(double)eligible));
        values.put("future_brier",-1d);values.put("future_ece",-1d);
        values.put("paired_mainline_n",paired);values.put("paired_mainline_delta",delta);
        values.put("evidence_hash",V51ChecksumEngine.sha256("R943_RAW_GENESIS_OUTCOMES|"+assetId+'|'+first+'|'+last+'|'+n+'|'+hit+'|'+paired+'|'+delta));
        values.put("updated_ms",System.currentTimeMillis());db.update(TABLE_ASSET_GENOME,values,"asset_id=?",new String[]{assetId});
    }

    private static List<AssetRow> loadRunnable(DbStore store,String target,int limit){
        ArrayList<AssetRow> out=new ArrayList<AssetRow>();Cursor c=null;try{
            c=store.readable().rawQuery("SELECT asset_id,genome_spec,state_blob FROM "+TABLE_ASSET_GENOME
                    +" WHERE (status IN('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CERTIFIED_FUTURE_OOS') OR "+PairedResearchChallengeR932.referenceCondition("'genesis:'||"+TABLE_ASSET_GENOME+".asset_id",target)+")"
                    +" AND "+LegacySettlementProofR79.assetHeadEligibleSql(TABLE_ASSET_GENOME+".asset_id")
                    +" AND may_authorize_real=0 AND (earliest_use_issue='' OR earliest_use_issue<=?)"
                    +" AND (trained_until_issue='' OR trained_until_issue<?)"
                    + GlobalResearchScheduleR932.predicate(target,"'genesis:'||"+TABLE_ASSET_GENOME+".asset_id")
                    +" AND NOT EXISTS(SELECT 1 FROM "+TABLE_ASSET_FREEZE
                    +" f WHERE f.asset_id="+TABLE_ASSET_GENOME+".asset_id AND f.target_issue=?)"
                    +" ORDER BY COALESCE((SELECT cap.execution_started FROM "+ResearchCaptureLedgerR932.TABLE
                    +" cap WHERE cap.target_issue=? AND cap.candidate_key=('genesis:'||"+TABLE_ASSET_GENOME+".asset_id)),0) ASC,"
                    +"CASE status WHEN 'CERTIFIED_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 ELSE 2 END,"
                    +"future_n ASC,updated_ms ASC LIMIT ?",new String[]{target,target,target,target,String.valueOf(limit)});
            while(c.moveToNext())try{GenesisAssetGenomeR915 g=GenesisAssetGenomeR915.parse(c.getString(1));
                out.add(new AssetRow(g,GenesisAssetVmR915.State.decode(c.getString(2))));}catch(Throwable ignored){}
        }catch(Throwable ignored){}finally{if(c!=null)c.close();}return out;
    }

    private static List<AssetRow> loadParents(DbStore store,int limit){
        ArrayList<AssetRow> out=new ArrayList<AssetRow>();Cursor c=null;try{
            c=store.readable().rawQuery("SELECT genome_spec,state_blob FROM "+TABLE_ASSET_GENOME
                    +" WHERE status IN('OBSERVING_FUTURE_OOS','CERTIFIED_FUTURE_OOS')"
                    +" ORDER BY future_wilson_lower DESC,incremental_info DESC,future_n DESC LIMIT ?",
                    new String[]{String.valueOf(limit)});while(c.moveToNext())try{
                out.add(new AssetRow(GenesisAssetGenomeR915.parse(c.getString(0)),
                        GenesisAssetVmR915.State.decode(c.getString(1))));}catch(Throwable ignored){}
        }catch(Throwable ignored){}finally{if(c!=null)c.close();}return out;
    }

    private static int pruneCapacity(final DbStore store,final int need){
        Integer out=store.coordinatorR36().runWrite("R915_GENESIS_CAPACITY_PRUNE",
                new DatabaseCoordinator.WriteCallable<Integer>(){@Override public Integer call(SQLiteDatabase db){
                    ContentValues v=new ContentValues();v.put("status","PRUNED_CAPACITY");
                    v.put("lifecycle_reason","CAPACITY_LOWEST_EVIDENCE");v.put("updated_ms",System.currentTimeMillis());
                    int rows=0;Cursor c=null;ArrayList<String> ids=new ArrayList<String>();try{
                        c=db.rawQuery("SELECT asset_id FROM "+TABLE_ASSET_GENOME+" WHERE status NOT LIKE 'CERTIFIED%'"
                                +" AND status NOT LIKE 'PRUNED%' AND status NOT LIKE 'REVOKED%' ORDER BY "
                                +"future_wilson_lower ASC,incremental_info ASC,future_n DESC LIMIT ?",
                                new String[]{String.valueOf(Math.max(1,need))});while(c.moveToNext())ids.add(c.getString(0));
                    }finally{if(c!=null)c.close();}for(String id:ids)rows+=db.update(TABLE_ASSET_GENOME,v,"asset_id=?",new String[]{id});
                    return Integer.valueOf(rows);}},Integer.valueOf(0));return out==null?0:out.intValue();
    }

    private static void insertEvent(SQLiteDatabase db,String issue,String type,String entity,String op,
            String parents,String children,String failure,String need,long now){
        String canonical=issue+'|'+type+'|'+entity+'|'+op+'|'+parents+'|'+children+'|'+failure+'|'+need+'|'+now;
        String hash=V51ChecksumEngine.sha256("R915_EVOLUTION|"+canonical);
        ContentValues v=new ContentValues();v.put("event_id","R915-EVT-"+hash.substring(0,24));
        v.put("event_issue",safe(issue));v.put("entity_type",safe(type));v.put("entity_id",safe(entity));
        v.put("operation",safe(op));v.put("parent_ids",safe(parents));v.put("child_ids",safe(children));
        v.put("failure_vector",safe(failure));v.put("need_plan",safe(need));v.put("created_ms",now);v.put("record_hash",hash);
        db.insertWithOnConflict(TABLE_EVOLUTION_EVENT,null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static int baseHit(SQLiteDatabase db,String issue){Cursor c=null;try{c=db.rawQuery(
            "SELECT hit_label FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue=? AND strict_oos=1",
            new String[]{issue});return c.moveToFirst()?c.getInt(0):-1;}catch(Throwable ignored){return -1;}
        finally{if(c!=null)c.close();}}
    private static boolean drawExists(SQLiteDatabase db,String issue){Cursor c=null;try{c=db.rawQuery(
            "SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{issue});return c.moveToFirst();}
        finally{if(c!=null)c.close();}}
    private static int strictSettledCount(DbStore store){return count(store,"SELECT COUNT(1) FROM "
            +DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1");}
    private static String latestSettledIssue(DbStore store){Cursor c=null;try{c=store.readable().rawQuery(
            "SELECT MAX(target_issue) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1",null);
        return c.moveToFirst()?safe(c.getString(0)):"";}catch(Throwable ignored){return "";}finally{if(c!=null)c.close();}}
    private static int nextGeneration(DbStore store){Cursor c=null;try{c=store.readable().rawQuery(
            "SELECT COALESCE(MAX(generation),0)+1 FROM "+TABLE_ASSET_GENOME,null);return c.moveToFirst()?c.getInt(0):1;}
        catch(Throwable ignored){return 1;}finally{if(c!=null)c.close();}}
    private static int count(DbStore store,String sql){Cursor c=null;try{c=store.readable().rawQuery(sql,null);
        return c.moveToFirst()?c.getInt(0):0;}catch(Throwable ignored){return 0;}finally{if(c!=null)c.close();}}
    private static String scalarString(DbStore store,String sql){Cursor c=null;try{c=store.readable().rawQuery(sql,null);
        return c.moveToFirst()&&!c.isNull(0)?safe(c.getString(0)):"";}catch(Throwable ignored){return "";}
        finally{if(c!=null)c.close();}}
    private static Set<String> hashes(DbStore store,String column){HashSet<String> out=new HashSet<String>();Cursor c=null;
        try{c=store.readable().rawQuery("SELECT "+column+" FROM "+TABLE_ASSET_GENOME,null);while(c.moveToNext())out.add(safe(c.getString(0)));
        }catch(Throwable ignored){}finally{if(c!=null)c.close();}return out;}
    private static ArrayList<String> normalizeAtomIds(List<String> source){ArrayList<String> out=new ArrayList<String>();
        if(source!=null)for(String raw:source){
            ResearchWorkBudgetR927.checkpoint();String id=safe(raw);if(id.length()>0&&!out.contains(id))out.add(id);}return out;}
    private static GenesisSchemaRegistryR915.AtomSpec findAtom(String id){for(GenesisSchemaRegistryR915.AtomSpec atom:
        GenesisSchemaRegistryR915.atoms())if(atom.id.equals(id))return atom;return null;}
    private static boolean isFamily(String id){for(GenesisSchemaRegistryR915.FamilySpec family:
        GenesisSchemaRegistryR915.families())if(family.id.equals(id))return true;return false;}
    private static int[] seedAtoms(long seed,int size){int[] out=new int[Math.max(8,Math.min(24,size))];
        HashSet<Integer> used=new HashSet<Integer>();long x=seed;
        for(int i=0;i<out.length;i++){
            ResearchWorkBudgetR927.checkpoint();int value;do{x^=x>>>12;x^=x<<25;x^=x>>>27;
            value=(int)Math.floorMod(x*2685821657736338717L,GenesisSchemaRegistryR915.atomCount());
        }while(!GenesisSchemaRegistryR915.atoms().get(value).productionScoreEligible()
                ||!used.add(Integer.valueOf(value)));out[i]=value;}return out;}
    private static long seedOf(String s){String h=V51ChecksumEngine.sha256(s);try{return Long.parseUnsignedLong(h.substring(0,16),16);}
        catch(Throwable ignored){return h.hashCode();}}
    private static double wilsonLower(int hit,int n){if(n<=0)return 0;double z=1.959963984540054d,p=hit/(double)n,
            den=1+z*z/n,center=p+z*z/(2*n),margin=z*Math.sqrt((p*(1-p)+z*z/(4*n))/n);return clamp01((center-margin)/den);}
    private static double blockMin(List<Outcome> rows,int width){if(rows.isEmpty())return 0;double min=1;int blocks=0;
        for(int start=0;start<rows.size();start+=width){
            ResearchWorkBudgetR927.checkpoint();int end=Math.min(rows.size(),start+width);if(end-start<Math.min(12,width/2))continue;
            int hit=0;for(int i=start;i<end;i++)hit+=rows.get(i).hit;min=Math.min(min,hit/(double)(end-start));blocks++;}
        return blocks==0?0:min;}
    private static int eligibleFreezes(SQLiteDatabase db,String assetId){Cursor c=null;try{
        c=db.rawQuery("SELECT COUNT(1) FROM "+TABLE_ASSET_FREEZE+" f WHERE f.asset_id=? AND EXISTS(SELECT 1 FROM "
                +DbHelper.TABLE_DRAWS+" d WHERE d.issue=f.target_issue AND d.dirty=0)",new String[]{assetId});
        return c.moveToFirst()?Math.max(0,c.getInt(0)):0;}finally{if(c!=null)c.close();}}
    private static boolean productionSemanticEligible(SQLiteDatabase db,String assetId){Cursor c=null;try{
        c=db.rawQuery("SELECT genome_spec FROM "+TABLE_ASSET_GENOME+" WHERE asset_id=? LIMIT 1",
                new String[]{assetId});
        if(!c.moveToFirst())return false;
        return GenesisAssetGenomeR915.parse(c.getString(0)).usesOnlyProductionScoreAtoms();
    }catch(Throwable ignored){return false;}finally{if(c!=null)c.close();}}
    private static String publish(DbStore store,String status){try{store.setState("r915_genesis_tick_status",status);
        store.setState("r915_genesis_tick_at",String.valueOf(System.currentTimeMillis()));}catch(Throwable ignored){}return status;}
    private static void writeState(SQLiteDatabase db,String key,String value){db.execSQL("INSERT OR REPLACE INTO "
            +DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",
            new Object[]{key,safe(value),Long.valueOf(System.currentTimeMillis())});}
    private static void ensureColumn(SQLiteDatabase db,String table,String column,String ddl){Cursor c=null;boolean found=false;try{
        c=db.rawQuery("PRAGMA table_info("+table+")",null);while(c.moveToNext())if(column.equals(c.getString(1))){
            ResearchWorkBudgetR927.checkpoint();found=true;break;}}
        finally{if(c!=null)c.close();}if(!found)db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+' '+ddl);}
    private static boolean contains(int actual,int a,int b,int c){return actual==a||actual==b||actual==c;}
    private static String join(List<String> rows){StringBuilder out=new StringBuilder();for(String row:rows){
            ResearchWorkBudgetR927.checkpoint();if(out.length()>0)out.append(',');out.append(row);}return out.toString();}
    private static String names(List<GenesisAssetGenomeR915> rows){StringBuilder out=new StringBuilder();for(GenesisAssetGenomeR915 g:rows){
            ResearchWorkBudgetR927.checkpoint();if(out.length()>0)out.append('\n');out.append(g.mechanismNameZh);}return out.toString();}
    private static String freezeNames(List<Freeze> rows){StringBuilder out=new StringBuilder();for(Freeze f:rows){
            ResearchWorkBudgetR927.checkpoint();if(out.length()>0)out.append('\n');out.append(f.row.genome.mechanismNameZh);}return out.toString();}
    private static int parseInt(String value,int fallback){try{return Integer.parseInt(safe(value));}catch(Throwable ignored){return fallback;}}
    private static double clamp01(double v){return Math.max(0,Math.min(1,Double.isNaN(v)||Double.isInfinite(v)?0:v));}
    private static String percent(double v){return String.format(Locale.US,"%.2f%%",100*clamp01(v));}
    private static String sqlLiteral(String v){return safe(v).replace("'","''");}
    private static String safe(String v){return v==null?"":v.trim();}

    private static final class AssetRow{final GenesisAssetGenomeR915 genome;final GenesisAssetVmR915.State state;
        AssetRow(GenesisAssetGenomeR915 g,GenesisAssetVmR915.State s){genome=g;state=s;}}
    private static final class ProposalValidation{final boolean accepted;final String reason;
        ProposalValidation(boolean accepted,String reason){this.accepted=accepted;this.reason=safe(reason);}}
    private static final class Freeze{CandidateExecutionAuditR932.Proof audit;final AssetRow row;final V51Models.PositionAction action;
        final GenesisAssetVmR915.PredrawContext context;final GenesisAssetVmR915.AssetValue value;
        Freeze(AssetRow r,V51Models.PositionAction a,GenesisAssetVmR915.PredrawContext c,GenesisAssetVmR915.AssetValue v){row=r;action=a;context=c;value=v;}}
    private static final class Outcome{final String issue;final double score,brier,regime;final int hit,baseHit;
        Outcome(String i,double s,int h,int b,double br,double r){issue=safe(i);score=clamp01(s);hit=h;baseHit=b;brier=br;regime=r;}}

    private static final class FailureVector{
        final String primary;final int settled,realMissStreak;final double recentHit,missPressure,calibrationPressure;
        FailureVector(String p,int n,double hit,double miss,double calibration,int streak){primary=p;settled=n;recentHit=hit;missPressure=miss;calibrationPressure=calibration;realMissStreak=Math.max(0,streak);}
        static FailureVector analyze(DbStore store,int settled){Cursor c=null;int n=0,hit=0;double brier=0;try{c=store.readable().rawQuery(
                "SELECT hit_label,selected_hit_p FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 96",null);
            while(c.moveToNext()){
            ResearchWorkBudgetR927.checkpoint();int y=c.getInt(0);double p=c.isNull(1)||c.getDouble(1)<0?0.30d:c.getDouble(1);n++;hit+=y;brier+=(p-y)*(p-y);}}
            catch(Throwable ignored){}finally{if(c!=null)c.close();}double rate=n==0?0:hit/(double)n,br=n==0?1:brier/n;
            int streak=parseInt(store.getState("r923_miss_streak_current","0"),0);
            String primary=streak>=MissStreakEscalationR923.ESCALATE_AT?"REAL_MISS_STREAK_ESCALATION":br>0.30d?"CALIBRATION_RESIDUAL":rate<0.25d?"LOW_HIT_STRUCTURE":rate<0.32d?"MISS_CONCENTRATION":"COMPLEMENTARITY_SEARCH";
            return new FailureVector(primary,settled,rate,1-rate,br,streak);}
        String canonical(){return primary+"|settled="+settled+"|recentHit="+recentHit+"|miss="+missPressure+"|brier="+calibrationPressure+"|realMissStreak="+realMissStreak;}}
    private static final class NeedPlan{
        final ArrayList<String> families;final String reason;NeedPlan(ArrayList<String> f,String r){families=f;reason=r;}
        static NeedPlan from(FailureVector failure,int settled){ArrayList<String> f=new ArrayList<String>();int start=Math.floorMod(failure.primary.hashCode()+settled,72);
            for(int i=0;i<12;i++)f.add(String.format(Locale.US,"AF%02d",1+(start+i*7)%72));return new NeedPlan(f,"failure="+failure.primary+"|diverseFamilies=12");}
        String canonical(){return reason+"|families="+families;}}
}
