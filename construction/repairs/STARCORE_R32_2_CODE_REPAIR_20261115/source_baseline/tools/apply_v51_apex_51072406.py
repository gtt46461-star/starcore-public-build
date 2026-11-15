#!/usr/bin/env python3
"""Atomic launcher for the complete V51 APEX 51072406 materializer payload.

The six immutable payload parts are verified before any project file is written.
Post-materialization semantic fixes are explicit, deterministic and fail closed.
"""
from __future__ import annotations
import base64
import hashlib
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "tools" / "v51_51072406_parts"
EXPECTED_COMPRESSED_B64_SHA256 = "cb1a576cd74c355e19355a2df452500bfbc4882ff373e7ac316b2a6da3d1009b"
EXPECTED_SOURCE_SHA256 = "3e304160451a238d2b41b2a464631f414f21fd1d22658815e4c13e6216703427"

chunks = []
for index in range(1, 7):
    path = PARTS / ("part%02d.b64" % index)
    if not path.is_file():
        raise SystemExit("missing materializer payload part: %s" % path)
    chunks.append(path.read_text(encoding="ascii").strip())
encoded = "".join(chunks)
if hashlib.sha256(encoded.encode("ascii")).hexdigest() != EXPECTED_COMPRESSED_B64_SHA256:
    raise SystemExit("51072406 compressed materializer payload SHA256 mismatch")
source = zlib.decompress(base64.b64decode(encoded))
if hashlib.sha256(source).hexdigest() != EXPECTED_SOURCE_SHA256:
    raise SystemExit("51072406 materializer source SHA256 mismatch")
namespace = {"__name__": "__main__", "__file__": str(Path(__file__).resolve())}
exec(compile(source, str(Path(__file__).resolve()), "exec"), namespace, namespace)

JAVA = ROOT / "app" / "src" / "main" / "java" / "com" / "ethk3" / "novaonepro"
SEMANTIC_PATCHES = {
    "V51RobustTop3Optimizer.java": (
        "V51ChecksumEngine.sha256(position+'|'",
        "V51ChecksumEngine.sha256(String.valueOf(position)+'|'",
    ),
    "V51PositionActionBuilder.java": (
        "V51ChecksumEngine.sha256(p+'|'",
        "V51ChecksumEngine.sha256(String.valueOf(p)+'|'",
    ),
}
for filename, replacement in SEMANTIC_PATCHES.items():
    path = JAVA / filename
    if not path.is_file():
        raise SystemExit("51072406 semantic patch target missing: %s" % path)
    text = path.read_text(encoding="utf-8")
    old, new = replacement
    if new in text:
        continue
    if old not in text:
        raise SystemExit("51072406 semantic patch anchor missing: %s" % filename)
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
