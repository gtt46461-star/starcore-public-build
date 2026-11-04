"""Read-only curated source call-edge audit. This does not execute Android or infer a runtime trace."""
import collections
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[4]
OUT=Path(__file__).resolve().parent
JAVA=ROOT/'app/src/main/java'
files={}

def source(name):
    if name not in files:
        matches=list(JAVA.rglob(name+'.java'))
        if len(matches)!=1:raise ValueError((name,len(matches)))
        path=matches[0];text=path.read_text();files[name]=(path,text)
    return files[name]

def anchor(name,needle):
    path,text=source(name);pos=text.find(needle)
    if pos<0:raise ValueError(('source anchor missing',name,needle))
    start=text.rfind('\n',0,pos)+1;end=text.find('\n',pos)
    if end<0:end=len(text)
    return dict(path=str(path.relative_to(ROOT)),line=text.count('\n',0,pos)+1,
                exactNeedle=needle,sourceLine=text[start:end].strip()[:420],sourceSHA256=hashlib.sha256(text.encode()).hexdigest())

paths=[]
def path(key,priority,scope,chain,condition,startup,fix,refs):
    paths.append(dict(id=key,priority=priority,staticClassification=scope,chain=chain,
                      reachabilityCondition=condition,startupCoverage=startup,minimalChangeSuggestion=fix,
                      sourceEvidence=[anchor(*ref) for ref in refs],runtimeTrace='NOT_RUN',productionModified=False))

path('D01','P0','DIRECT_BUSINESS_DDL',
 ['DrawCanonicalizerR811.observeWithinWrite conflict branch','SettlementEngine.revokeCertificatesForDrawWithinR30','CertificateRevocationR30.revokeWithin','ensure -> install/sourceTrigger','SQLiteDatabase.execSQL CREATE TABLE/TRIGGER/INDEX'],
 'Any canonical conflict reaching this branch; revokeWithin unconditionally calls ensure. inTransaction only proves writer ownership, not migration ownership.',
 'Already called by SchemaR27.ensure at R26_RUNTIME_SCHEMA startup; again after independent SU2 schema becomes ready. That does not remove the business ensure.',
 'Install base revocation objects before any canonical correction can require them, then install each protocol trigger in its journaled R27/SU2 migration unit. Business revokeWithin should only verify required domain/objects and execute DML; missing applicable authority schema must return typed SCHEMA_PENDING, preserving source/authority safety.',
 [('DrawCanonicalizerR811','if (conflict) SettlementEngine.revokeCertificatesForDrawWithinR30'),('SettlementEngine','CertificateRevocationR30.revokeWithin(db,issue,cause,eventKey)'),('CertificateRevocationR30','public static void revokeWithin'),('CertificateRevocationR30','        ensure(db);'),('CertificateRevocationR30','for(String ddl:DDL)db.execSQL(ddl)'),('CertificateRevocationR30','CREATE TRIGGER IF NOT EXISTS '),('SchemaR27','CertificateRevocationR30.ensure(db)'),('RuntimeCoordinatorR26','StrategySchemaSU2.ensure(db);CertificateRevocationR30.ensure(db)')])

path('D02','P0','DIRECT_BUSINESS_DDL',
 ['DeferredEvidenceWriterR35 worker','V51Repository.persist -> persistWithReport','DatabaseCoordinator.runExistingTransaction','DbHelper.ensureTianshuB03 / ensureR35Schema','SQLiteDatabase.execSQL schema statements'],
 'The wrapper invokes both ensures before checking alreadyCommitted and before its own beginTransaction. The already-committed fast path therefore does not bypass DDL. Other runExistingTransaction callers share this branch.',
 'Critical startup explicitly runs DbHelper.ensureR35Schema and DbHelper.ensureTianshuB03 under SchemaDomain runStep, and verifies/stamps critical readiness.',
 'Remove business ensures from runExistingTransaction after a pure critical-domain readiness guard; schedule missing migration on the existing startup/migration owner and return SCHEMA_PENDING. Do not classify CREATE IF NOT EXISTS as a harmless SELECT or infer no DDL from unchanged schema_version.',
 [('DeferredEvidenceWriterR35','V51Repository.persist(store, result)'),('V51Repository','PersistReport report = persistWithReport(store, result)'),('V51Repository','DatabaseCoordinator.runExistingTransaction('),('DatabaseCoordinator','boolean owns = !db.inTransaction();'),('DatabaseCoordinator','            DbHelper.ensureTianshuB03(db);'),('DatabaseCoordinator','            DbHelper.ensureR35Schema(db);'),('DatabaseCoordinator','public void run(SQLiteDatabase x) { DbHelper.ensureR35Schema(x); }'),('DatabaseCoordinator','public void run(SQLiteDatabase x) { DbHelper.ensureTianshuB03(x); }')])

path('D03','P0','DIRECT_BUSINESS_DDL',
 ['V51Repository.persistWithReport or settleShadow','runExistingTransaction callback','V51Repository.ensure(SQLiteDatabase)','CREATE B03 tables/indexes/triggers'],
 'In addition to wrapper DDL, the prediction and shadow-settlement callbacks themselves call ensure(db). applyV51SettlementFromOwner also calls ensure(store).',
 'V51Repository.ensure is already an explicit Critical startup runStep.',
 'Replace each business ensure with a pure verified critical contract guard. Preserve prediction and settlement DML, replay keys and authority checks. Validate wrapper plus callback together so removing only one leaves runtime DDL.',
 [('V51Repository','                                ensure(db);'),('V51Repository','public static void ensure(SQLiteDatabase db)'),('V51Repository','"CREATE TABLE IF NOT EXISTS "'),('V51Repository','        ensure(store);'),('DatabaseCoordinator','public void run(SQLiteDatabase x) { V51Repository.ensure(x); }')])

path('D04','P0','DIRECT_BUSINESS_DDL',
 ['AutoFetchService.persistSelectedShadowCandidateForSettlement','DbStore.saveShadowCandidates','saveMainChainCandidateSnapshot','ensureMainChainClosedLoopReady','execQuiet CREATE + ensure*Columns ALTER + indexes'],
 'A direct service call exists; FullCandidateScanEngine and AutoFetch also expose reflective saveRealtimeTop40/saveAdaptiveJudgeInput entry points. Nonempty valid candidate list enters unconditionally. ensureMainChainClosedLoopReady catches Throwable and has no committed-readiness cache.',
 'ensureRuntimeExtraTables does not call ensureMainChainClosedLoopReady. Existing base tables alone do not establish all additive columns/indexes used here.',
 'Add a bounded journaled legacy/mainchain extension migration before enabling candidate-save business owners. Replace business ensure with pure domain plus required-column guard, typed SCHEMA_PENDING. Keep the fixed K=3 and real save logic; stop swallowing schema failure as a successful save.',
 [('AutoFetchService','store.saveShadowCandidates(baseIssue, targetIssue, one, 1)'),('DbStore','            saveMainChainCandidateSnapshot('),('DbStore','        ensureMainChainClosedLoopReady(db);'),('DbStore','private void ensureMainChainClosedLoopReady(SQLiteDatabase db)'),('DbStore','"CREATE TABLE IF NOT EXISTS "'),('DbStore','            ensureMainChainIndexes(db);'),('FullCandidateScanEngine','            store.saveShadowCandidates(baseIssue, targetIssue, candidates, limit);')])

path('D05','P1','BUSINESS_DDL_ON_SCHEMA_CACHE_MISS',
 ['ModelBundleRuntimeR932 loader pointer-CAS transaction','ResearchPlatformStoreR929.activateRuntimeBundle','createSchema','CREATE/conditional ALTER'],
 'Bundle activation calls createSchema from its pointer-CAS transaction. TransactionSchemaCacheR941 skips only when a CURRENT scope, active transaction and committed matching schema_version are all present. Cold/mismatched scope or later DDL makes the business DDL branch reachable.',
 'ResearchSchemaCoordinatorR926.ensureUnifiedResearchSchema already calls createSchema, but startup cache publication/order is not a permanent pure business guard.',
 'Complete platform migration before loader activation and use pure required-contract readiness here. Treat missing platform schema as SCHEMA_PENDING without changing the previous active bundle pointer.',
 [('ModelBundleRuntimeR932','ResearchPlatformStoreR929.activateRuntimeBundle(db,frozenSnapshot,target,now)'),('ResearchPlatformStoreR929','static void activateRuntimeBundle('),('ResearchPlatformStoreR929','        createSchema(db);'),('ResearchPlatformStoreR929','if (TransactionSchemaCacheR941.readyCommitted(db,"ResearchPlatformStoreR929")) return;'),('ResearchPlatformStoreR929','if(!revisionColumn)db.execSQL("ALTER TABLE '),('ResearchSchemaCoordinatorR926','ResearchPlatformStoreR929.createSchema(db);'),('TransactionSchemaCacheR941','if (scope == null || scope.database != database || !database.inTransaction()) return false;')])

path('D06','P1','BUSINESS_DDL_ON_SCHEMA_CACHE_MISS',
 ['V51RuntimeOrchestrator realtime preparation','UnifiedResearchChoiceR932.freeze','ResearchPlatformStoreR929.ensureSchema','R929_PLATFORM_SCHEMA -> createSchema'],
 'WeakHashMap READY is process/handle-local and is not populated by startup createSchema. First ensureSchema per handle opens a writer while holding SCHEMA_LOCK; inner committed cache can skip DDL only under its exact scope/version conditions.',
 'Same platform startup owner as D05. R929_PLATFORM_SCHEMA being registered WriteClass.MIGRATION does not make the realtime freeze caller a startup owner.',
 'Freeze must require a pure current platform contract and request the existing migration owner if absent. Remove lazy schema work and writer wait under SCHEMA_LOCK from the realtime path; retain pre-draw/deadline checks and current mainline cache until B10 migration.',
 [('V51RuntimeOrchestrator','UnifiedResearchChoiceR932.freeze(store,targetIssue)'),('UnifiedResearchChoiceR932','ResearchPlatformStoreR929.ensureSchema(store);'),('ResearchPlatformStoreR929','synchronized (SCHEMA_LOCK)'),('ResearchPlatformStoreR929','"R929_PLATFORM_SCHEMA"'),('ResearchPlatformStoreR929','                            createSchema(db);'),('DatabaseCoordinator','"R929_PLATFORM_SCHEMA"')])

path('D07','P1','LEGACY_OWNER_BUSINESS_DDL_ON_CACHE_MISS',
 ['AutoFetchService legacy continuity fallback','ResearchLearningContinuityR912.pulseWithinR927','R967_LANE_CLAIM -> ensureDesired','ResearchTaskQueueR967.schema','CREATE task/control/pin objects'],
 'AutoFetch refuses this legacy fallback while R26 owner is not TERMINATED. pulseWithin also honors r27_legacy_research_enabled=0. If the legacy owner is permitted and history exists, every claim calls schema; cache/scope/version determines DDL execution.',
 'ResearchMigrationRegistryR952 R64_EXTENSION_SCHEDULER_6 already installs ResearchTaskQueue schema through the research migration scheduler.',
 'Replace claim-time schema with pure task-control readiness. Keep the single-owner retirement gates intact; do not re-enable the legacy scheduler to work around schema or health failures.',
 [('AutoFetchService','ResearchLearningContinuityR912.pulse(store,1000L)'),('AutoFetchService','return coordinator != null && !"TERMINATED".equals(coordinator.stopStateR31());'),('ResearchLearningContinuityR912','ResearchTaskQueueR967.schema(db);'),('ResearchLearningContinuityR912','public ResearchTaskQueueR967.Lease call(SQLiteDatabase db){ensureDesired(db,issue,dev,online);'),('ResearchTaskQueueR967','if(TransactionSchemaCacheR941.readyCommitted(db,"R980_DURABLE_TASK_CONTROL_CAPACITY"))return;'),('ResearchTaskQueueR967','db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE'),('ResearchMigrationRegistryR952','com.ethk3.novaonepro.ResearchTaskQueueR967.schema(target);')])

path('D08','P1','LEGACY_OWNER_BUSINESS_DDL_ON_CACHE_MISS',
 ['legacy continuity runLane 6','FixedResearchWindowR932.maintainReviewed','R932_FIXED_WINDOW_SCHEMA -> schema','CandidateCalibrationR932.schema -> AlgorithmProofRecoveryR948.schema','CREATE calibration/proof/window objects'],
 'Reachable when the legacy owner is permitted; multiple called schema methods have transaction cache guards, not immutable startup-only restrictions. maintainReviewed starts a schema writer before its numeric/settlement page.',
 'ResearchSchemaCoordinatorR926.ensureUnifiedResearchSchema already installs FixedResearchWindow and proof/capture/calibration dependencies; revision registry contains additional R64 CP12 units.',
 'Replace the schema preflight in business maintain/capture with pure versioned readiness and enqueue missing migration. Do not change fitting, independent evidence windows or gate thresholds.',
 [('ResearchLearningContinuityR912','case 6: return FixedResearchWindowR932.maintainReviewed(store);'),('FixedResearchWindowR932','"R932_FIXED_WINDOW_SCHEMA"'),('FixedResearchWindowR932','CandidateCalibrationR932.schema(db);'),('CandidateCalibrationR932','AlgorithmProofRecoveryR948.schema(db);'),('CandidateCalibrationR932','db.execSQL("CREATE TABLE IF NOT EXISTS "+STUDY'),('ResearchSchemaCoordinatorR926','FixedResearchWindowR932.schema(db);')])

path('D09','P1','LEGACY_OWNER_BUSINESS_DDL_ON_CACHE_MISS',
 ['legacy continuity runLane 8','AlgorithmProofRecoveryR948.maintain','R948_RESEARCH_INTEGRITY_SCHEMA','schema/CandidateExecutionAudit.schema/ResearchCaptureLedger.schema','CREATE proof/audit/capture objects'],
 'Legacy owner gate as D07. Every maintenance call issues the schema callback; committed cache may suppress SQL but a cold/mismatched cache does not.',
 'All three are already explicitly called by ensureUnifiedResearchSchema.',
 'Read-only readiness before the bounded proof audit. Missing schema becomes SCHEMA_PENDING; old proof audit/recovery receipts must remain immutable and cannot be retrospectively invented.',
 [('ResearchLearningContinuityR912','case 8: {int n=AlgorithmProofRecoveryR948.maintain(store,2);'),('AlgorithmProofRecoveryR948','"R948_RESEARCH_INTEGRITY_SCHEMA"'),('AlgorithmProofRecoveryR948','static void schema(SQLiteDatabase db)'),('AlgorithmProofRecoveryR948','db.execSQL("CREATE TABLE IF NOT EXISTS "+HEAD'),('ResearchSchemaCoordinatorR926','AlgorithmProofRecoveryR948.schema(db);'),('ResearchSchemaCoordinatorR926','ResearchCaptureLedgerR932.schema(db);'),('ResearchSchemaCoordinatorR926','CandidateExecutionAuditR932.schema(db);')])

path('D10','P2','PUBLIC_BUSINESS_API_DDL_CALLER_NOT_ESTABLISHED',
 ['ResearchEvolutionStoreR930.putSnapshot/putAdmission/diagnosticCounts public APIs','ensureSchema handle cache miss','R930_EVOLUTION_SCHEMA -> SchemaR27.ensure','R27/R31/research DDL and guarded ALTER'],
 'Public APIs have a direct DDL chain. Repository reference search found only SchemaR27.createSchema integration and internal calls, so active production caller of these public put/diagnostic APIs is NOT_ESTABLISHED. Do not count this as an observed runtime event.',
 'SchemaR27.ensure already runs from R26_RUNTIME_SCHEMA. Its own call to ResearchEvolutionStore.createSchema does not populate ResearchEvolutionStore READY weak map.',
 'Remove broad SchemaR27.ensure from lazy business APIs; require the relevant persisted evolution contract and request migration. diagnosticCounts should be read-only UNKNOWN/SCHEMA_PENDING if absent.',
 [('ResearchEvolutionStoreR930','public static Map<String, Long> diagnosticCounts(DbStore store)'),('ResearchEvolutionStoreR930','        ensureSchema(store);'),('ResearchEvolutionStoreR930','com.ethk3.novaonepro.strategyv2.SchemaR27.ensure(db);'),('SchemaR27','ResearchEvolutionStoreR930.createSchema(db);'),('RuntimeCoordinatorR26','"R26_RUNTIME_SCHEMA"')])

path('D11','P2','PUBLIC_DIAGNOSTIC_API_DDL_CALLER_NOT_ESTABLISHED',
 ['DbStore.getTianshuB03StorageStatus / verifyTianshuB03Storage','ensureTianshuB03Storage','DbHelper.ensureTianshuB03 + V51Repository.ensure','CREATE schema'],
 'Public diagnostic/storage methods always ensure schema before reading. No direct/reflective call to these exact names was found in the current Java source outside DbStore; current ReportSnapshot path below does not use them. Runtime reachability beyond exposed API is not established.',
 'These B03 schemas already have Critical startup runSteps.',
 'Make diagnostics pure read of contract/object readiness and return UNKNOWN if absent; keep migration as explicit separate operation. Do not globally claim current report export runs DDL from this dormant API alone.',
 [('DbStore','public B03StorageStatus getTianshuB03StorageStatus()'),('DbStore','public DatabaseIntegrityVerifier.B03IntegrityReport verifyTianshuB03Storage()'),('DbStore','public void ensureTianshuB03Storage()'),('DbStore','        DbHelper.ensureTianshuB03(db);'),('DbStore','        V51Repository.ensure(db);'),('DatabaseCoordinator','public void run(SQLiteDatabase x) { DbHelper.ensureTianshuB03(x); }')])

path('D12','P1','LAZY_VERSIONED_MIGRATION_REACHABLE_FROM_BUSINESS',
 ['DbStore.saveShadowCandidates and many save/repair entrances','ensureRuntimeExtraTablesOnce','DatabaseCoordinator.ensureRuntimeSchemaOnce','LegacySchemaCoordinatorR926.ensure -> runStep initializer','ensureRuntimeExtraTables -> teacher/model/profile CREATE/ALTER'],
 'runtimeSchemaReady and persisted legacy contract avoid work when ready. On cold/missing/drifted legacy contract, the first business caller performs the migration. DbStore constructor initializes Critical, not this legacy unit. This is an explicit versioned lazy migration, not an unguarded per-slice CREATE, but strict business-DDL-zero is still not established.',
 'Already journaled via LEGACY_RUNTIME by LegacySchemaCoordinator. No direct startup call that prewarms this initializer before business save was established in this review.',
 'Run the existing Legacy initializer as a bounded migration before enabling dependent save/teacher/model owners; replace business lazy call with pure ready test and SCHEMA_PENDING. No new writer or duplicated schema owner.',
 [('DbStore','            ensureRuntimeExtraTablesOnce(db);'),('DbStore','private void ensureRuntimeExtraTablesOnce(final SQLiteDatabase db)'),('DatabaseCoordinator','if (runtimeSchemaReady.get()) return true;'),('DatabaseCoordinator','LegacySchemaCoordinatorR926.ensure(this, initializer)'),('LegacySchemaCoordinatorR926','SchemaDomainCoordinatorR926.runStep(db, c, "LEGACY_RUNTIME"'),('DbStore','        ensureTeacherMatrixSampleTable(db);'),('DbStore','private void ensureTeacherMatrixSampleTable(SQLiteDatabase db)')])

reviewed=[]
def reviewed_scope(key,status,description,refs):reviewed.append(dict(id=key,status=status,description=description,evidence=[anchor(*ref) for ref in refs]))

reviewed_scope('S01','DESIGNATED_STARTUP_MIGRATION',
 'R26 initialize has explicit MIGRATION R26_RUNTIME_SCHEMA before RuntimeR27 creation; RuntimeSchemaR26 and SchemaR27 populate model/checkpoint/feedback/observation/writer/timeline/pins/retention objects. This startup permission does not transfer to other business callers of those ensures.',
 [('RuntimeCoordinatorR26','"R26_RUNTIME_SCHEMA"'),('RuntimeCoordinatorR26','RuntimeR27 runtime=new RuntimeR27(store,owner)'),('SchemaR27','for (String sql : ResearchSchemaR30R1.DDL) db.execSQL(sql);'),('RuntimeSchemaR26','SchemaDomainCoordinatorR926.migrateR31Feedback(db);')])
reviewed_scope('S02','DESIGNATED_SCHEMA_TASK',
 'The schema task separately installs StrategySchemaSU2 using MIGRATION R26_SU2_SCHEMA; its ready predicate and migration work are part of a designated migration lane, not a normal model save slice. SU2 metadata extends existing snapshots here and at SchemaR27 startup when table exists.',
 [('RuntimeCoordinatorR26','if(!StrategySchemaSU2.ready(store.readable()))'),('RuntimeCoordinatorR26','"R26_SU2_SCHEMA"'),('StrategySchemaSU2','MatrixStoreSU2.ensureMetadata(db);'),('SchemaR27','if(tableExists(db,"su2_matrix_snapshot"))MatrixStoreSU2.ensureMetadata(db);')])
reviewed_scope('S03','BUSINESS_NO_DIRECT_DDL_IN_REVIEWED_METHODS',
 'Current model checkpoint/save, bounded matrix maintenance, main R26 feedback bridge and ReportSnapshot consumers reviewed below do not directly ensure/create schema. This is method-specific source review, not transitive whole-app or Android runtime DDL-zero evidence.',
 [('ModelStoreSU2','public static'),('LivePoolR26','static long'),('MatrixStoreSU2','public static void save(SQLiteDatabase db'),('MatrixStoreSU2','if(!StrategySchemaSU2.ready(db))return"SU2_MATRIX_SCHEMA_PENDING";'),('RuntimeCoordinatorR26','case "feedback":'),('RuntimeSchemaR26','public static long reconcileFeedbackR31'),('ResearchFeedbackCP12','public static boolean drainOne(DbStore store,long deadlineNs)')])
reviewed_scope('S04','STARTUP_FEEDBACK_SCHEMA_SEPARATE_FROM_DISPATCH',
 'R31 feedback objects are migrated/stamped through RESEARCH_R31_FEEDBACK; CP12 feedback schema is in the legacy R64_CP12_CLOSURE_2 unit. The feedback DDL literals are definitions consumed by these owners, not proof every feedback drain runs CREATE. A pure guard at business entry would make missing/drifted schema explicit SCHEMA_PENDING rather than generic SQLite failure.',
 [('SchemaDomainCoordinatorR926','public static void migrateR31Feedback(SQLiteDatabase db)'),('RuntimeSchemaR26','public static void ensureR31Feedback(SQLiteDatabase db)'),('ResearchMigrationRegistryR952','"R64_CP12_CLOSURE_2"'),('ResearchMigrationRegistryR952','com.ethk3.novaonepro.ResearchFeedbackCP12.schemaCP12(target);')])
reviewed_scope('S05','REPORT_SCHEMA_METHOD_IS_A_READ',
 'ReportSnapshotR30.capture uses a separate read snapshot; its helper named schema executes PRAGMA schema_version only. No direct CREATE/ALTER/DROP or ensure-schema invocation was found in the reviewed current snapshot/export/lease source methods. Construction of DbStore in other diagnostic APIs and native SQL generation still require independent review; this is not blanket report exemption.',
 [('ReportSnapshotR30','if(db.inTransaction())throw new IllegalStateException("REPORT_READ_CALLED_IN_EXISTING_TRANSACTION");'),('ReportSnapshotR30','db.beginTransactionReadOnly();'),('ReportSnapshotR30','static long schema(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA schema_version",null))')])
reviewed_scope('S06','CONSTANT_ONLY_MODEL_DDL_NOT_BUSINESS_SAVE',
 'ModelArtifactR29/ModelSurfaceStore/AtomNet/FrozenConfirmation DDL constants are executed by SchemaR27 startup. Their mere literal presence does not establish runtime CREATE in checkpoint/model save methods. RuntimeR27 publishes through immutable batches whose DDL validator rejects CREATE with SCHEMA_PENDING.',
 [('SchemaR27','ModelArtifactR29.DDL,'),('SchemaR27','for (String sql : ModelSurfaceStoreR30.DDL) db.execSQL(sql);'),('SchemaR27','for (String sql : AtomNetStoreR30.DDL) db.execSQL(sql);'),('SchemaR27','for (String sql : FrozenConfirmationStoreR30.DDL) db.execSQL(sql);'),('ResearchWriteQueueR27','SCHEMA_PENDING')])
reviewed_scope('S07','TRANSACTION_SCHEMA_CACHE_IS_NOT_MIGRATION_OWNER_PROOF',
 'readyCommitted requires CURRENT same-handle active transaction and matching schema_version; afterCommit only publishes names verified at the post-commit version. New DDL invalidates earlier cached names; cold process/scope/cache is also a miss. A cache guard changes frequency, not ownership or elimination of reachable business DDL.',
 [('TransactionSchemaCacheR941','public static boolean readyCommitted(SQLiteDatabase database, String name)'),('TransactionSchemaCacheR941','entry.version != version'),('TransactionSchemaCacheR941','item.getValue().longValue() == version')])

inventory=json.loads((ROOT/'verification/r31/p01/ddl-inventory.json').read_text())
result=dict(card='R31-B02-C02',phase='P02_DIAGNOSTIC_ONLY',capturedAt=datetime.now(timezone.utc).isoformat(),
    sourceHead=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),dirtyAtCapture=bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),
    scope='MANUALLY_REVIEWED_SOURCE_CALL_EDGES_AND_GUARDS_NOT_RUNTIME_TRACE_NOT_WHOLE_APP_REACHABILITY',
    startingInventory=dict(path='verification/r31/p01/ddl-inventory.json',literalCount=inventory['ddl_literals'],sourceHead=inventory['source_head'],meaning='navigation only; no count of executed statements'),
    findings=paths,reviewedStartupAndNoDirectDDLScope=reviewed,
    classificationCounts=dict(collections.Counter(p['staticClassification'] for p in paths)),
    runtimeTrace='NOT_RUN_ON_ANDROID',allRuntimeDDLRemoved='NOT_ESTABLISHED',productionModified=False,
    sourceSHA256={str(p.relative_to(ROOT)):hashlib.sha256(t.encode()).hexdigest() for p,t in files.values()},
    requiredFollowup=['Root assigns production owners; do not edit frozen files concurrently.',
      'Move/complete only relevant versioned startup units; business entry checks must never create schema.',
      'Verify fresh install, already-upgraded DB, partial migration/restart, drift/missing-object and rollback paths.',
      'Capture actual SQLite execSQL/trace operation+WriteContext+task/fence after startup during model/matrix/feedback/report/canonicalization work; expect no runtime CREATE/ALTER/DROP from normal business owners.',
      'Include cached warm and cold/invalidated readiness states; unchanged schema_version does not prove no CREATE IF NOT EXISTS was executed.',
      'Full dynamic SQL/reflection/native/legacy coverage and Android device runtime trace remain unverified.'])
OUT.mkdir(parents=True,exist_ok=True)
(OUT/'CALLGRAPH.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
rows=['id\tpriority\tclassification\tchain\tcondition']
for p in paths:rows.append('\t'.join([p['id'],p['priority'],p['staticClassification'],' -> '.join(p['chain']),p['reachabilityCondition']]))
(OUT/'PATHS.tsv').write_text('\n'.join(rows)+'\n')
print(json.dumps(dict(status='STATIC_REVIEW_COMPLETE_NOT_RUNTIME_PASS',paths=len(paths),reviewedScopes=len(reviewed),productionFilesChanged=0,classificationCounts=result['classificationCounts']),ensure_ascii=False))
