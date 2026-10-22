"""Durable global SEALED ownership. Copies, renamed trials and changed hyperparameters cannot reset it."""
from __future__ import annotations
import hashlib,json,os,sqlite3
from pathlib import Path


def _canonical(value):
    return json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(",",":"),allow_nan=False)


def consume(manifest,issues,config,root:Path):
    start,end=manifest["splits"]["sealed"];sealed=issues[start:end]
    if not sealed or len(set(sealed))!=len(sealed):raise ValueError("SEALED_EMPTY_OR_DUPLICATE")
    # Content/selector identity identifies the one experiment; the guard is per game + issue.
    # This also prevents evasion by changing schema, output path, source metadata or split edges.
    identity={"game":manifest["game"],"schema":manifest["schema"],"feature_manifest_sha256":manifest["feature_manifest_sha256"],
              "source_sha256":manifest["source_sha256"],"sealed_issues":sealed,"frozen_selection":config}
    encoded=_canonical(identity);token=hashlib.sha256(encoded.encode()).hexdigest()
    payload={"token":token,"identity":identity,"read_once":True,"start_issue":sealed[0],"end_issue":sealed[-1]}
    root=Path(root);root.mkdir(parents=True,exist_ok=True)
    connection=sqlite3.connect(root/"sealed_registry_r932.sqlite3",timeout=30,isolation_level=None)
    try:
        connection.execute("PRAGMA journal_mode=WAL");connection.execute("PRAGMA synchronous=FULL")
        connection.execute("CREATE TABLE IF NOT EXISTS experiment(token TEXT PRIMARY KEY,identity_json TEXT NOT NULL,payload_json TEXT NOT NULL)")
        connection.execute("CREATE TABLE IF NOT EXISTS sealed_use(game TEXT NOT NULL,issue TEXT NOT NULL,token TEXT NOT NULL,PRIMARY KEY(game,issue))")
        connection.execute("BEGIN IMMEDIATE")
        existing=connection.execute("SELECT identity_json,payload_json FROM experiment WHERE token=?",(token,)).fetchone()
        if existing:
            if existing[0]!=encoded:raise ValueError("SEALED_TOKEN_COLLISION")
            connection.commit();return json.loads(existing[1])
        # Preserve all older consumed-window records. A registry upgrade never refunds them.
        for old in root.glob("sealed-*.json"):
            record=json.loads(old.read_text());old_id=record.get("identity",{})
            if old_id.get("game")==manifest["game"] and set(old_id.get("sealed_issues",[])).intersection(sealed):
                raise RuntimeError("SEALED_WINDOW_ALREADY_CONSUMED_LEGACY_REGISTRY")
        for issue in sealed:
            previous=connection.execute("SELECT token FROM sealed_use WHERE game=? AND issue=?",(manifest["game"],issue)).fetchone()
            if previous and previous[0]!=token:raise RuntimeError("SEALED_WINDOW_ALREADY_CONSUMED_GLOBAL_REGISTRY:"+issue)
        connection.execute("INSERT INTO experiment VALUES(?,?,?)",(token,encoded,_canonical(payload)))
        connection.executemany("INSERT INTO sealed_use VALUES(?,?,?)",((manifest["game"],issue,token)for issue in sealed))
        connection.commit();return payload
    except BaseException:
        if connection.in_transaction:connection.rollback()
        raise
    finally:connection.close()

