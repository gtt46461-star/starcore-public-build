"""Exact final-Dex wiring and release identity. Not a physical-device benchmark."""
from pathlib import Path
import sys,json,re,zipfile
r=Path(__file__).resolve().parents[2]
s=Path(sys.argv[1])/'com/ethk3/novaonepro'; n=0

def group(name): return '\n'.join(p.read_text() for p in s.glob(name+'*.smali'))
def check(ok,label):
 global n
 assert ok,label
 n+=1;print('PASS '+label)

def method(body,name):
 return '\n'.join(m.group(0) for m in re.finditer(r'^\.method[^\n]*\b'+re.escape(name)+r'\([^\n]*\n.*?^\.end method',body,re.M|re.S))
main=group('MainActivity');history=group('HistoryActivity');reports=group('CompleteReportFileR927')
check('CountdownProjectionR971;->remaining(' in main,'ACTUAL_UI_CALLS_MONOTONIC_PROJECTION')
check('CountdownProjectionR971;->atRead(' in main,'STALE_SNAPSHOT_DEADLINE_PROJECTED_NOT_FROZEN_VALUE')
check('r971_tick_elapsed' in group('AutoFetchService'),'TICK_PRODUCER_SENDS_MONOTONIC_TIMESTAMP')
check('ExecutorService;->submit(' in history or 'ExecutorService;->execute(' in history,'HISTORY_SQL_DISPATCHED_TO_BACKGROUND_EXECUTOR')
check('DbStore;-><init>' not in method((s/'HistoryActivity.smali').read_text(),'onCreate'),'HISTORY_ONCREATE_NO_DATABASE_CONSTRUCTOR')
check('DbStore;-><init>' in history,'HISTORY_REAL_DATABASE_STILL_LOADED_NOT_STUBBED')
check('LosslessReportArchiveR971;->create(' in reports,'FULL_REPORT_CALLS_STREAMING_ARCHIVE')
check('LosslessReportArchiveR971;->create(' in group('DiagnosisActivity'),'DIAGNOSIS_DEFAULT_EXPORT_CALLS_SAME_STREAMING_ARCHIVE')
check('FullResearchInventoryR923;->writeToStrict(' in reports,'FULL_REPORT_USES_STRICT_ALL_TABLE_WRITER')
check('r967_*' in group('FullResearchInventoryR923'),'NEW_TRAINING_TABLE_PREFIX_IN_FULL_REPORT')
check('application/zip' in group('DiagnosticReportProviderR923'),'CONTENT_PROVIDER_ZIP_MIME_WIRED')
archive=group('LosslessReportArchiveR971')
for evidence in ['java/util/zip/ZipOutputStream;','java/security/MessageDigest;','java/io/FileDescriptor;->sync(','java/io/File;->renameTo(','manifest.json','PER_TABLE_READ_TIME_NOT_GLOBAL_ATOMIC_BACKUP']:
 check(evidence in archive,'ARCHIVE_EVIDENCE:'+evidence)
check('startupReconciledR971(' in group('DbStore'),'REPEATED_DB_READER_SKIPS_NOOP_STARTUP_WRITE')
check('LegacyTrainingStreamMigrationR971;->retireBatch(' in group('r952/ResearchOutboxR952'),'REAL_TRAINING_OWNER_CALLS_LEGACY_MIGRATION')
legacy=group('r952/LegacyTrainingStreamMigrationR971')
for evidence in ['DEPENDENCY_RETIRED','R971_LEGACY_TRAIN_STREAM_RETIRED_NOT_TRAINED','research_training_input_r967','research_training_run_r967','R971_MIGRATION_TRANSACTION_REQUIRED','RuntimeRecoveryJournalR952;->record(']:
 check(evidence in legacy,'LEGACY_MIGRATION_GUARD:'+evidence)
check("SET state='COMPLETED'" not in legacy,'MIGRATION_DOES_NOT_FAKE_TRAINING_COMPLETION')
check('r971_maintenance_turn' in group('ResearchLearningContinuityR912'),'BOUNDED_TRAINING_EXTRA_TURN_PRESENT')
director=group('r952/ResearchDirectorR952')
check('CandidateSandboxR952;->execute(' in (s/'r952/ResearchDirectorR952.smali').read_text(),'PREDRAW_SANDBOX_EXECUTES_IN_PREPARE_STAGE')
check(not any('CandidateSandboxR952;->execute(' in p.read_text() for p in (s/'r952').glob('ResearchDirectorR952$*.smali')),'NO_PREDRAW_SANDBOX_COMPUTATION_INSIDE_WRITE_CALLBACK')
check('captureStillCurrentR971(' in director and 'R971_CAPTURE_CAS_TRANSACTION' in director,'PREPARED_MODEL_AST_COMMIT_FENCE_PRESENT')
m=json.loads((r/'release/release_manifest.json').read_text())
check((m['versionCode']==577250971 and m['versionName']=='V57.21.2.0.14-R5.4.2.14-971') or (m['versionCode']==577250972 and m['versionName']=='V57.21.2.0.15-R5.4.2.15-972'),'R971_BASELINE_OR_R972_COMPATIBLE_SUCCESSOR_IDENTITY')
check(m['applicationId']=='com.ethk3.novaonepro' and m['databaseName']=='binance_k3_nova_one_pro_v57.db' and m['schema']['userVersion']==36,'EXISTING_APP_AND_DATABASE_IDENTITIES_PRESERVED')
print(f'R971_FINAL_DEX_WIRING=PASS|checks={n}|PHYSICAL_DEVICE=NOT_RUN|FULL_91_PLAN=NOT_COMPLETE')
