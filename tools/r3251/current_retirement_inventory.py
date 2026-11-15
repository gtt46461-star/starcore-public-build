#!/usr/bin/env python3
"""P5 current static inventory; preserve the original P1 snapshot and generator."""
import argparse,hashlib,importlib.util,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
GENERATOR=ROOT/'tools/r31/retirement_inventory.py'
P1=ROOT/'tools/r31/retirement_manifest.json'
CURRENT=ROOT/'release/r3251_retirement_inventory.json'
P1_SHA='f38c7f30b83a4e186472d8f2a8887bb07581d38b9417bfe1ee93cf55a61e0038'
GENERATOR_SHA='1976ba4111aef65cc5ba7afa9cc259ae1a1b518d9b971be31c8194e3807818cf'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--write',action='store_true');p.add_argument('--check',action='store_true');p.add_argument('--manifest',type=Path);a=p.parse_args()
 if sha(P1)!=P1_SHA:raise SystemExit('R3251_RETIREMENT_P1_SNAPSHOT_CHANGED')
 if sha(GENERATOR)!=GENERATOR_SHA:raise SystemExit('R3251_RETIREMENT_ORIGINAL_GENERATOR_CHANGED')
 spec=importlib.util.spec_from_file_location('unchanged_original_retirement_inventory',GENERATOR);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 value=module.build();assert value['approved_drops']==[]
 prior=json.loads(P1.read_text());assert value['preserved_capabilities']==prior['preserved_capabilities'] and value['candidates']==prior['candidates'] and value['rebuild_in_place']==prior['rebuild_in_place']
 expected=json.dumps(value,indent=1,ensure_ascii=False,sort_keys=False)+'\n';target=a.manifest or CURRENT
 if a.write:target.write_text(expected);print('R3251_CURRENT_RETIREMENT_INVENTORY_WRITTEN_ORIGINAL_P1_PRESERVED')
 else:
  valid=target.is_file() and target.read_text()==expected
  print('R3251_CURRENT_RETIREMENT_INVENTORY_'+('CURRENT' if valid else 'STALE')+'|approved_data_drops=0|P1_snapshot_unchanged=true|original_generator_unchanged=true');return 0 if valid else 1
 return 0
if __name__=='__main__':sys.exit(main())
