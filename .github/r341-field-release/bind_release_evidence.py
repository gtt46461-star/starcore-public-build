"""Include the completed run's raw Android and Gradle evidence in the delivery ZIP."""
import hashlib
import json
from pathlib import Path
import sys
from zipfile import ZIP_DEFLATED, ZIP_STORED, ZipFile


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def bind(folder):
    evidence = folder / 'engineering-originals.zip'
    index = folder / 'ZIP_MEMBERS_SHA256.json'
    checksums = folder / 'SHA256SUMS.txt'
    total, = folder.glob('*-APK-SOURCE-EVIDENCE.zip')
    receipt_path = folder / 'DELIVERY.json'
    old = json.loads(index.read_bytes())
    listed = {name: digest for digest, name in (row.split(maxsplit=1) for row in checksums.read_text().splitlines())}
    assert len(listed) == 5 and listed[total.name] == sha(total)
    assert sha(receipt_path) == listed[receipt_path.name]
    assert all(Path(name).name == name and sha(folder / name) == digest for name, digest in old.items())
    with ZipFile(evidence) as archive:
        assert archive.testzip() is None and 'engineering/device-run.json' in archive.namelist()
        raw = json.loads(archive.read('engineering/device-run.json'))
    receipt = json.loads(receipt_path.read_bytes())
    assert raw['sourceCommit'] == receipt['sourceCommit'] and raw['versionCode'] == receipt['r341']['versionCode']
    attached = sha(evidence)
    receipt['engineeringEvidenceAttachment'] = {'file': evidence.name, 'sha256': attached,
        'bytes': evidence.stat().st_size, 'scope': 'EXACT_SOURCE_EMULATOR_AND_HOST_WITNESSES'}
    pending_receipt = receipt_path.with_suffix('.pending')
    pending_receipt.write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    members = {name: folder / name for name in old}
    members[receipt_path.name] = pending_receipt
    members[evidence.name] = evidence
    expected = {name: sha(path) for name, path in members.items()}
    assert expected[evidence.name] == attached
    pending_index = index.with_suffix('.pending')
    pending_index.write_text(json.dumps(expected, indent=2) + '\n')
    members[index.name] = pending_index
    pending_total = total.with_suffix('.pending')
    with ZipFile(pending_total, 'x', ZIP_DEFLATED, compresslevel=6) as archive:
        for name, path in sorted(members.items()):
            archive.write(path, name, compress_type=ZIP_STORED if path.suffix in {'.zip', '.apk', '.bundle'} else ZIP_DEFLATED)
    with ZipFile(pending_total) as archive:
        assert set(archive.namelist()) == set(members)
        for name, path in members.items():
            with archive.open(name) as stream:
                assert hashlib.file_digest(stream, 'sha256').hexdigest() == sha(path), name
    listed[total.name] = sha(pending_total)
    listed[receipt_path.name] = expected[receipt_path.name]
    pending_checksums = checksums.with_suffix('.pending')
    pending_checksums.write_text(''.join(f'{digest}  {name}\n' for name, digest in listed.items()))
    pending_receipt.replace(receipt_path)
    pending_index.replace(index)
    pending_total.replace(total)
    pending_checksums.replace(checksums)


if __name__ == '__main__':
    bind(Path(sys.argv[1]))
