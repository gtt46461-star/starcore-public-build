#!/usr/bin/env python3
"""Lossless CI evidence fallback when Actions artifact storage is unavailable.

Only the existing validation logs, receipts, generated identities and unit-test XML
are included. Never includes an APK, keystore, environment dump or repository archive.
Decode O09_EVIDENCE_CHUNK lines in order and verify the declared ZIP SHA256.
"""
import base64
import hashlib
import io
import json
from pathlib import Path
import zipfile

root = Path(__file__).resolve().parents[1]
paths = set()
for pattern in (
    'validation-o09/**/*.log', 'validation-o09/**/*.json',
    'validation-o09/input-commit.txt', 'app/build/reports/lint-results-debug.xml',
    'validation-o09/d04/*.bin', 'validation-o09/d04/raw_upstream.jsonl',
    'app/build/generated/r815/source-manifest.json',
    'app/build/generated/r967/semantic_manifest.json',
    'app/build/test-results/testDebugUnitTest/TEST-*.xml',
):
    paths.update(p for p in root.glob(pattern) if p.is_file())
buffer = io.BytesIO()
with zipfile.ZipFile(buffer, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(paths):
        archive.write(path, path.relative_to(root))
data = buffer.getvalue()
if len(data) > 8 * 1024 * 1024:
    raise SystemExit('O09_LOG_EVIDENCE_TOO_LARGE: preserve via artifact; no silent truncation')
encoded = base64.b64encode(data).decode('ascii')
print('O09_EVIDENCE_MANIFEST|' + json.dumps({
    'files': len(paths), 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
    'format': 'ZIP_BASE64_ORDERED_CHUNKS', 'chunk_chars': 4096,
}), flush=True)
for index, start in enumerate(range(0, len(encoded), 4096)):
    print('O09_EVIDENCE_CHUNK|' + str(index) + '|' + encoded[start:start+4096], flush=True)
print('O09_EVIDENCE_END', flush=True)

