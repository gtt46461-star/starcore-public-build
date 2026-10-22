#!/usr/bin/env python3
"""Compatibility entry point: V50 is replay-only and cannot issue R952 production proof."""
from pathlib import Path
import json, sys
def main()->None:
    if "--delegate-r952" not in sys.argv:raise SystemExit("V50_LEGACY_REPLAY_ONLY_USE_R952_PIPELINE")
    print(json.dumps({"status":"DELEGATE","target":str(Path(__file__).parent/"r925"/"pipeline.py"),"productionProof":False}))
if __name__=="__main__":main()
