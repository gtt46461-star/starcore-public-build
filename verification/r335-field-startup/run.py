"""Replay the third compact 095 field catalog. The checkpoint body is an explicit test fixture."""
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/r31"))
from upgrade_matrix import Host


def seed(archive, database):
    if hashlib.sha256(archive.read_bytes()).hexdigest() != "8a3eedd03b53693266f8a8744fc7b7e59b326c575d5f84c5741e4148ca26d0d2":
        raise ValueError("expected verified third 095 report")
    with zipfile.ZipFile(archive) as outer:
        inner = outer.read(next(n for n in outer.namelist() if n.endswith(".zip")))
    with zipfile.ZipFile(io.BytesIO(inner)) as report:
        chunks = {}
        for name in sorted(n for n in report.namelist() if n.startswith("catalog-") and n.endswith(".jsonl")):
            for line in report.read(name).splitlines():
                row = json.loads(line)
                if row["kind"] == "schema_sql_chunk":
                    chunks.setdefault((row["type"], row["name"]), []).append((row["start_char"], row["text"]))
        with sqlite3.connect(database) as db:
            for kind in ("table", "view", "index", "trigger"):
                for (obj_type, name), pieces in chunks.items():
                    if obj_type == kind and not name.startswith("sqlite_"):
                        db.execute("".join(piece for _, piece in sorted(pieces)))
            # This compact profile omits the index rows. Derive the owner's exact column signature.
            columns = [[c[1], c[2], str(c[5])] for c in db.execute("PRAGMA table_info(r29_model_checkpoint)")]
            encoded = json.dumps(columns, separators=(",", ":"))
            db.execute("INSERT INTO r30_retention_archive_index VALUES(?,?,?,?,0,1)",
                       ("r29_model_checkpoint", "r30_keep_r29_model_checkpoint",
                        hashlib.sha256(encoded.encode()).hexdigest(), encoded))


def main():
    with tempfile.TemporaryDirectory() as directory:
        work = Path(directory)
        host = Host(work)
        seed(Path(sys.argv[1]), work / "seed.db")
        subprocess.run(["javac", "-cp", host.cp, "-d", work / "host", Path(__file__).with_name("FieldStartup.java")], check=True)
        for mode, marker in [("ordinary", None), ("workspace", None),
                             ("dirty", "R31_STARTUP_CHECKPOINT_SOURCE_CHANGED"), ("shadow", "R31_STARTUP_CHECKPOINT_TEMP_SCHEMA")]:
            db = work / (mode + ".db")
            shutil.copyfile(work / "seed.db", db)
            result = subprocess.run(["java", "-cp", host.cp, "com.ethk3.novaonepro.FieldStartup", db, work, mode],
                                    capture_output=True, text=True)
            if marker is None:
                assert result.returncode == 0 and "COMMITTED_CHECKPOINT_AND_RECEIPT" in result.stdout, result.stderr
            else:
                assert result.returncode != 0 and marker in result.stderr, result.stderr
                with sqlite3.connect(db) as check:
                    assert check.execute("SELECT count(*) FROM r31_startup_checkpoint_receipt").fetchone() == (0,)
            print(mode, marker or "COMMITTED_CHECKPOINT_AND_RECEIPT")


if __name__ == "__main__":
    main()
