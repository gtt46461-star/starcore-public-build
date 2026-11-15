#!/usr/bin/env python3
"""One-time, idempotent refactor of the existing Ranker stage; no scoring changes elsewhere."""
from pathlib import Path
import hashlib
import os

ROOT=Path(__file__).resolve().parents[1]
path=ROOT/'training/r929/pipeline.py'
text=path.read_text(encoding='utf-8')
start=text.index('def stage_ranker(out: Path):\n')
stop=text.index('\ndef logistic_fit(',start)
old=text[start:stop]
new='''def stage_ranker(out: Path):
    from training.r932.ranker_comparison import stage
    return stage(out)

'''
if old==new:
    print('R932_RANKER_STAGE_ALREADY_INTEGRATED')
else:
    required=['create_stack(out)','expanding_folds(x[a:b], y[a:b], b-a)','write_json(out / "ranker_artifact.json", artifact)','FROZEN_TUNE_SELECTED']
    if any(anchor not in old for anchor in required):
        raise SystemExit('R932_RANKER_UNREVIEWED_CONCURRENT_STAGE_CHANGE')
    replacement=text[:start]+new+text[stop:]
    compile(replacement,str(path),'exec')
    temp=path.with_suffix('.r932-tmp')
    temp.write_text(replacement,encoding='utf-8');os.replace(temp,path)
    print('R932_RANKER_STAGE_REFACTORED before='+hashlib.sha256(text.encode()).hexdigest()+' after='+hashlib.sha256(replacement.encode()).hexdigest())
