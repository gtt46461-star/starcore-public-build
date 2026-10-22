#!/usr/bin/env python3
"""Materialize the reviewed V51 APEX 51072403 engineering bundle in CI."""
from pathlib import Path
import base64
import gzip
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = "621f43685cdaaedc84233a78d8d6490b34d723fb4964ab2ccd5663aff4cfc574"
PARTS = ROOT / "tools/v51_bundle_parts"
payload = "".join(
    path.read_text(encoding="ascii").strip()
    for path in sorted(PARTS.glob("part*.txt"))
)
raw = gzip.decompress(base64.b64decode(payload.encode("ascii")))
actual = hashlib.sha256(raw).hexdigest()
if actual != EXPECTED:
    raise SystemExit("V51_BOOTSTRAP_HASH_MISMATCH expected=%s actual=%s" % (EXPECTED, actual))
files = json.loads(raw.decode("utf-8"))
for relative, content in files.items():
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
manifest = ROOT / "reports/V51_BOOTSTRAP_PATHS.txt"
manifest.parent.mkdir(parents=True, exist_ok=True)
manifest.write_text("\n".join(files) + "\n", encoding="utf-8")
print("V51_BOOTSTRAP_OK files=%d sha256=%s" % (len(files), EXPECTED))
