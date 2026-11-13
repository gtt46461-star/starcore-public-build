"""Fail closed when a new executable source directly names a legacy keep table.
This is a source inventory guard, not runtime/whole-card acceptance evidence.
Actual Store/consumer native execution is in run_keep_store_host.py.
"""
from pathlib import Path
import argparse,hashlib,json
ROOT=Path(__file__).resolve().parents[2]
ROLES={
 'CheckpointGcStoreR31.java':'EXACT_GC_REFERENCE_ATTACHMENT_DECLARATIONS;NO_LEGACY_ROW_PAYLOAD_READER',
 'CheckpointReferencesR26.java':'LEGACY_CHECKPOINT_ROOT_CENSUS_AND_MIGRATED_CAS_ROOT_CENSUS',
 'KeepMigrationR31.java':'KEYSET_MIGRATION_PROTOCOL',
 'KeepMigrationStoreR31.java':'COMPLETE_CAS_ORIGINAL_READERS_AND_REPORT_ATTACHMENT_KEYSET',
 'KeepMismatchReferenceR31.java':'DECLARED_SINGLE_SOURCE_REFERENCE_ONLY_PRODUCER_AND_AUDIT_COUNTS',
 'StartupCheckpointIdentityR31.java':'COMPLETE_STORAGE_CLASS_AND_CHUNKED_ORIGINAL_COMPARISON_OUTSIDE_WRITER;GENERATION_FENCE;REAL_STARTUP_RECEIPTS',
 'GapReferenceR31.java':'DECLARED_GAP_RECEIPT_REFERENCE_PRODUCER_DURABLE_CAS_BATCH_AND_PHASE_COMMIT',
 'KeepArchiveRetirementR31.java':'DUPLICATE_RETIREMENT_ONLY;SOURCE_COPY_PROTOCOL_NOT_RENAMED',
 'QualificationGapRepairR30R1.java':'ORIGINAL_PREDRAW_SCAN_AND_EXACT_RESTORATION_WITHOUT_EVIDENCE_RESET',
 'ResearchRetentionStoreR30.java':'EXACT_ARCHIVE_OWNER_AND_REAL_CAS_AWARE_STARTUP_DEDUPLICATION_CONSUMER',
 'WarmPublicationR31.java':'COMPLETE_NEW_PARENT_ARCHIVE_ROW_VERIFICATION_USING_CAS_AWARE_HIGH_WATER'
}
def check(root):
 records=[];unknown=[]
 for path in sorted((root/'app/src/main').rglob('*')):
  if not path.is_file() or path.suffix not in ('.java','.kt','.cpp','.c','.h'):continue
  content=path.read_text(errors='strict')
  if 'r30_keep_' not in content:continue
  row={'file':str(path.relative_to(root)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'role':ROLES.get(path.name),'scope':'EXECUTABLE_SOURCE_LITERAL_INVENTORY_ONLY_NOT_RUNTIME_PROOF'}
  records.append(row)
  if row['role'] is None:unknown.append(row['file'])
 if unknown:raise RuntimeError('R31_KEEP_UNREGISTERED_LITERAL_READER_OR_WRITER:'+','.join(unknown))
 return {'format':'R31_KEEP_EXECUTABLE_SOURCE_INVENTORY_1','state':'DECLARED_SOURCE_INVENTORY_MATCH','records':records,'wholeCardClosed':False,'sourceReferenceOnlyCoverage':'MISMATCH_AND_GAP_DECLARED_REFERENCE_PRODUCERS;OTHER_SOURCE_COPY_EXPLICIT','runtimeEvidence':'run_keep_store_host.py fresh actual source+SQLite+file+SIGKILL execution required independently'}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT);a=p.parse_args();print(json.dumps(check(a.root),indent=2))
