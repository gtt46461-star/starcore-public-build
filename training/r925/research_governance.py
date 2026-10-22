#!/usr/bin/env python3
"""Immutable trial/sealed-window registry with cross-directory content identity."""
from __future__ import annotations
import hashlib, json
from pathlib import Path
def register(registry:Path,source:Path,sealed_issues:list[str],config:dict)->dict:
    source_hash=hashlib.sha256(source.read_bytes()).hexdigest();identity={"schema":"R952_TRIAL_V1","source_hash":source_hash,"sealed_issues":sorted(set(sealed_issues)),"config":config};trial=hashlib.sha256(json.dumps(identity,sort_keys=True,separators=(",",":")).encode()).hexdigest();registry.mkdir(parents=True,exist_ok=True);target=registry/(trial+".json")
    if target.exists():raise ValueError("R952_TRIAL_OR_SEALED_REUSE")
    identity.update({"trial_id":trial,"state":"REGISTERED","sealed_consumed":False});target.write_text(json.dumps(identity,sort_keys=True,indent=2)+"\n",encoding="utf-8");return identity
