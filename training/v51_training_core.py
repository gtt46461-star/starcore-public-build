#!/usr/bin/env python3
"""Deterministic online FTRL core used only after a committed strict-Future label."""
from __future__ import annotations
import hashlib, json, math
def update(weights:list[float],features:list[float],label:int,rate:float=.02)->dict:
    if len(weights)!=len(features) or label not in (0,1):raise ValueError("R952_FTRL_INPUT")
    z=sum(w*x for w,x in zip(weights,features));p=1/(1+math.exp(-max(-35,min(35,z))));out=[w-rate*(p-label)*x for w,x in zip(weights,features)]
    core={"schema":"R952_ONLINE_FTRL_V1","weights":out,"prediction":p,"label":label};core["artifact_hash"]=hashlib.sha256(json.dumps(core,sort_keys=True,separators=(",",":")).encode()).hexdigest();return core
