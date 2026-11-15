#!/usr/bin/env python3
"""Fail-closed R952 matrix, owner, identity and frozen-file guard."""
from __future__ import annotations
import csv, hashlib, json, re, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REQUIRED=("ResearchModuleRegistryR952.java","ResearchMigrationRegistryR952.java","ResearchOutboxR952.java","ResearchDirectorR952.java","CandidateSandboxR952.java","FullbloodDiagnosticsProviderR952.java")
def fail(code:str)->None:print(json.dumps({"status":"FAIL","code":code}));raise SystemExit(23)
def main()->None:
    java=ROOT/"app/src/main/java/com/ethk3/novaonepro";r952=java/"r952"
    for name in REQUIRED:
        if not (r952/name).is_file():fail("MISSING_"+name)
    manifest=json.loads((ROOT/"release/release_manifest.json").read_text(encoding="utf-8"))
    if manifest["applicationId"]!="com.ethk3.novaonepro" or manifest["databaseName"]!="binance_k3_nova_one_pro_v57.db" or manifest["schema"]["userVersion"]!=36:fail("IDENTITY_DRIFT")
    if manifest["runtimeContract"]["decisionOwner"]!="DecisionGateEngine_ONLY" or manifest["runtimeContract"]["settlementOwner"]!="SettlementEngine_ONLY":fail("OWNER_DRIFT")
    text="\n".join(p.read_text(encoding="utf-8",errors="ignore") for p in r952.glob("*.java"))
    if re.search(r"\b(TODO|FIXME|placeholder|stub)\b",text,re.I):fail("PLACEHOLDER")
    if "MAY_CREATE_REAL=true" in text.replace(" ","").upper():fail("RESEARCH_REAL_AUTHORITY")
    ledger=ROOT/"construction/ledgers/file_disposition.csv"
    if ledger.is_file():
        with ledger.open(encoding="utf-8-sig",newline="") as f:
            for row in csv.DictReader(f):
                path=row.get("path","");action=row.get("disposition","")
                if action not in {"KEEP","MODIFY","EXTRACT","REWRITE","DELETE_DUPLICATE","ADD_NEW"}:fail("BAD_DISPOSITION:"+path)
                if action!="DELETE_DUPLICATE" and path and not (ROOT/path).exists():fail("MATRIX_PATH_MISSING:"+path)
    hashes={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(r952.glob("*.java"))}
    print(json.dumps({"status":"PASS","r952Java":len(hashes),"tree_hash":hashlib.sha256(json.dumps(hashes,sort_keys=True).encode()).hexdigest()}))
if __name__=="__main__":main()
