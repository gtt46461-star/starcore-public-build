#!/usr/bin/env python3
"""Persist all working ledgers and bounded receipts, excluding runtime private data."""
import gzip
import hashlib
import io
import json
from pathlib import Path
import subprocess
import tarfile

root = Path(__file__).resolve().parents[1]
work = root/'construction/numbered'
archive = root/'release/numbered_execution_checkpoint.tar.gz'
allowed = {'.csv', '.json', '.log', '.patch', '.diff', '.txt', '.java', '.py', '.xml', '.html'}
excluded_parts = {'classes', 'native', 'native-fixture', 'archive-fixture', 'cas-fixture', '__pycache__'}
excluded_names = {'ARCHIVE_RECEIPT.json', 'CHECKPOINT_FILE_MANIFEST.json'}
# The checkpoint is the container of this diff. Embedding its previous binary delta
# recursively expands every subsequent checkpoint; its full bytes are separately
# pinned by ARCHIVE_RECEIPT and Git, rather than nested inside itself.
diff = subprocess.check_output(['git', 'diff', '--binary', '--', '.',
                                ':(exclude)release/numbered_execution_checkpoint.tar.gz'], cwd=root)
(work/'CURRENT_UNCOMMITTED.diff').write_bytes(diff)
files = []
for path in sorted(work.rglob('*')):
    if not path.is_file() or path.name in excluded_names:
        continue
    if any(part in excluded_parts for part in path.relative_to(work).parts):
        continue
    if path.suffix not in allowed and not path.name.endswith('.json.gz'):
        continue
    files.append(path)
for name in ['REQUIREMENT_EXECUTION_LEDGER.csv', 'CARD_EXECUTION_LEDGER.csv',
             'DETECTION_EXECUTION_LEDGER.csv', 'TEST_EXECUTION_LEDGER.csv',
             'SUBCONDITION_EXECUTION_LEDGER.csv', 'EXECUTION_STATE.json']:
    assert work/name in files, ('REQUIRED_FULL_LEDGER_MISSING', name)
manifest = [{'path':str(p.relative_to(root)), 'bytes':p.stat().st_size,
             'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in files]
(work/'CHECKPOINT_FILE_MANIFEST.json').write_text(json.dumps(manifest, indent=2)+'\n')
with archive.open('wb') as raw, gzip.GzipFile(fileobj=raw, mode='wb', mtime=0) as compressed:
    with tarfile.open(fileobj=compressed, mode='w') as package:
        for path in files+[work/'CHECKPOINT_FILE_MANIFEST.json']:
            payload=path.read_bytes()
            info=tarfile.TarInfo(str(path.relative_to(root)))
            info.size=len(payload);info.mode=0o644;info.mtime=0
            package.addfile(info, io.BytesIO(payload))
with tarfile.open(archive) as package:
    for entry in manifest:
        assert hashlib.sha256(package.extractfile(entry['path']).read()).hexdigest()==entry['sha256']
receipt={'path':str(archive.relative_to(root)), 'bytes':archive.stat().st_size,
         'sha256':hashlib.sha256(archive.read_bytes()).hexdigest(), 'files':len(files)+1,
         'diff_container_exclusion':'Only this generated checkpoint archive; all other tracked dirty changes retained. Archive full bytes/hash are separately persisted in Git and this receipt.',
         'scope':'Complete work-in-progress ledgers and current failures/receipts. Excludes DB/APK/classes/native binaries and private signing/API inputs. Open device, release and downstream clauses remain open.'}
(work/'ARCHIVE_RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt))
