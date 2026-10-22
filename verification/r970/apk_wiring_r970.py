"""Verify final DEX integration and exact historical asset, not Android runtime or win rate."""
from pathlib import Path
import sys,re,zipfile,hashlib,json
root=Path(__file__).resolve().parents[2];s=Path(sys.argv[1])/'com/ethk3/novaonepro';n=0

def group(name):return '\n'.join(p.read_text() for p in s.glob(name+'*.smali'))
def check(ok,label):
 global n
 assert ok,label
 n+=1;print('PASS '+label)
owner=group('HistoricalGenomeImportR967');creator=group('DirectedResearchCreationR932')
check(bool(owner),'HISTORICAL_IMPORTER_PRESENT_IN_FINAL_DEX')
for call in ('maintain','choose','committed','rejected'):
 check('HistoricalGenomeImportR967;->'+call+'(' in creator,'EXISTING_CREATOR_CALLS:'+call)
check('AlgorithmInventionLabR908;->registerDirected(' in creator,'EXISTING_ALGORITHM_REGISTRATION_OWNER_RETAINED')
check('HistoricalGenomeImportR967;->schema(' in group('r952/ResearchMigrationRegistryR952'),'EXISTING_MIGRATION_OWNER_CALLS_ADDITIVE_HISTORY_SCHEMA')
check('AlgorithmGenomeVmR908$State;->decodeStrict(' in owner,'STRICT_STATE_PARSER_WIRED')
check('HistoricalGenomeImportR967;->writeReport(' in group('CompleteReportFileR927'),'COMPLETE_REPORT_CALLS_ORIGINAL_SOURCE_AND_USAGE_REPORT')
check('Landroid/content/res/AssetManager;->open(' in owner,'REAL_ANDROID_ASSET_LOADER_PRESENT')
check('Landroid/database/sqlite/SQLiteDatabase;->inTransaction(' in owner,'ATOMIC_OWNER_PRECONDITION_PRESENT')
check('R970_HISTORY_COMMIT_FAILED' in owner,'ARCHIVE_FAILURE_NOT_FALSE_SUCCESS')
check('R970_FRESH_SEED_INSERT_OR_ZERO_EVIDENCE_CONTRACT' in owner,'NEW_SEED_ZERO_EVIDENCE_CHECK_PRESENT')
check('CURRENT_QUARANTINE_PRESERVED' in owner,'LIVE_QUARANTINE_NOT_BYPASSED')
for table in ('research_historical_source_r967','research_historical_genome_archive_r967','research_historical_genome_use_r967'):
 check(table in owner and table in group('SchemaDomainCoordinatorR926'),'HISTORY_TABLE_IN_MIGRATION_CONTRACT:'+table)
 for suffix in ('_no_update','_no_delete','_no_replace'):
  check(table+suffix in group('SchemaDomainCoordinatorR926'),'MISSING_GUARD_DETECTED_BY_DOMAIN_OWNER:'+table+suffix)
for metric in ('historical_archived_rows','historical_seed_eligible','historical_rejected_rows','historical_seed_created_n0','historical_seed_use_rejected'):
 check(metric in group('ResearchFunnelR967'),'REAL_READONLY_HISTORY_METRIC:'+metric)
check('old_future_n_transferred' in owner and 'old_semantics_certified' in owner,'HISTORICAL_PROOF_BOUNDARY_EXPLICIT')
check('DecisionGateEngine;->' not in owner and 'SettlementEngine;->' not in owner,'IMPORTER_HAS_NO_NEW_REAL_OR_SETTLEMENT_OWNER')
asset='assets/research/r967_history/R908_125_algorithms.tsv'
with zipfile.ZipFile(sys.argv[2]) as z:
 b=z.read(asset)
 check(hashlib.sha256(b).hexdigest()=='3697570b8db5ad0b14285685e7c2c12712dd02433d7efa6019e123838100ef80','APK_EXACT_ORIGINAL125_BYTE_SHA256')
 check(len(b.decode('utf-8').splitlines())==126,'APK_CONTAINS_ALL125_ORIGINAL_ROWS')
manifest=json.loads((root/'release/release_manifest.json').read_text())
check(manifest['versionCode']>=577250970 and manifest['schema']['researchDomainRevision']>=11,'R970_OR_NEWER_WITH_ADDITIVE_DOMAIN11_RETAINED')
print('R970_FINAL_DEX_WIRING=PASS|checks='+str(n)+'|PHYSICAL_DEVICE=NOT_RUN|FULL_91_PLAN=NOT_COMPLETE')
