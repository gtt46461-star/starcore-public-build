#!/usr/bin/env python3
"""Re-derive bounded report inputs from the untouched original nested ZIPs.

This checks original report provenance only. It cannot reconstruct an omitted
database, checkpoint body, device observation or natural future trial.
"""
from pathlib import Path
import argparse
import hashlib
import io
import json
import zipfile


def sha(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(',', ':'), allow_nan=False)


def verify(root):
    pins = json.loads((root/'evidence/INPUTS.json').read_text())
    pins = {row['label']: row for row in pins}
    receipts = []
    for label in ('326', '425'):
        pin = pins[label]
        archive = (root/pin['path']).read_bytes()
        if len(archive) != pin['bytes'] or sha(archive) != pin['sha256']:
            raise ValueError('ORIGINAL_REPORT_ARCHIVE_CHANGED '+label)
        with zipfile.ZipFile(io.BytesIO(archive)) as outer:
            names = [i.filename for i in outer.infolist()
                     if i.filename.endswith('/STARCORE_FULL_REPORT.zip')]
            if len(names) != 1:
                raise ValueError('ORIGINAL_INNER_REPORT_AMBIGUOUS '+label)
            inner_bytes = outer.read(names[0])
        with zipfile.ZipFile(io.BytesIO(inner_bytes)) as inner:
            raw = inner.read('R30_REPORT.json')
        original = json.loads(raw)
        copies = [(index, chapter['completeSnapshotEvidenceR31'])
                  for index, chapter in enumerate(original['chapters'])
                  if 'completeSnapshotEvidenceR31' in chapter]
        if len(copies) != 1:
            raise ValueError('COMPLETE_SNAPSHOT_COPY_AMBIGUOUS '+label)
        index, full = copies[0]
        path = root/('evidence/report_inputs/report'+label+'_input.json')
        input_bytes = path.read_bytes()
        actual = json.loads(input_bytes)
        if set(full['sources'])-set(actual['sources']) != {'objectIndex'}:
            raise ValueError('UNREVIEWED_SOURCE_OMISSION '+label)
        expected = {'facts': full['facts'], 'watermark': full['snapshotWatermark'],
                    'errors': full['sourceErrors'],
                    'sources': {k: v for k, v in full['sources'].items()
                                if k != 'objectIndex'}}
        if canonical(actual) != canonical(expected):
            raise ValueError('REPORT_INPUT_NOT_EXACT_ORIGINAL_PROJECTION '+label)
        if canonical(original['snapshotWatermark']) != canonical(actual['watermark']):
            raise ValueError('ORIGINAL_WATERMARK_CONFLICT '+label)
        receipts.append({'label': label, 'status': 'PASS_EXACT_ORIGINAL_PROJECTION',
                         'outerZipSha256': sha(archive),
                         'innerZipSha256': sha(inner_bytes),
                         'originalR30ReportSha256': sha(raw),
                         'inputSha256': sha(input_bytes),
                         'originalEvidencePath': 'chapters/'+str(index)+'/completeSnapshotEvidenceR31',
                         'sourceCount': len(actual['sources']),
                         'factsCount': len(actual['facts']),
                         'excludedKeys': ['sources/objectIndex'],
                         'exclusionReason': 'RECURSIVE_DUPLICATE_OBJECT_INDEX_ONLY; ORIGINAL_INDEX_REMAINS_IN_UNTOUCHED_REPORT',
                         'factsWatermarkSourcesErrorsDeepEqual': True,
                         'rawDatabaseRecovered': False,
                         'newDeviceObservation': False})
    return {'format': 'R3251_REPORT_INPUT_PROVENANCE_V1', 'status': 'PASS',
            'scope': 'ORIGINAL_PROVENANCE_ONLY_NOT_RUNTIME_OR_QUALITY_ACCEPTANCE',
            'reports': receipts}


if __name__ == '__main__':
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('--package', type=Path, default=Path(__file__).resolve().parents[1])
    args = cli.parse_args()
    print(json.dumps(verify(args.package.resolve()), ensure_ascii=False, indent=2))
