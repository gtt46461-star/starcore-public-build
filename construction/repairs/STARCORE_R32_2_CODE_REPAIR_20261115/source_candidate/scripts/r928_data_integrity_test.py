#!/usr/bin/env python3
"""Synthetic export-contract tests; never an OOS/model performance certificate."""
import importlib.util,json,hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('pipeline',root/'training/r925/pipeline.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
row={'integrity_version':1,'input_stage':'POST_NUMBER_ACTION_META','upstream_raw_available':False,'schema':m.SCHEMA,'game':'Binance1-PK10','target_issue':'202609100002','max_source_issue':'202609100001','frozen_at':100,'draw_at':200,'snapshot_hash':'a'*64,'actual':list(range(1,11)),'actions':[list(map(int,a))+[[0.0]*96] for a in m.ACTIONS]}
def line(r):
 s=json.dumps(r,separators=(',',':'));return s[:-1]+',"payload_sha256":"'+hashlib.sha256(s.encode()).hexdigest()+'"}'
s=line(row);m.validate(m.read_integrity_row(s),'');results={'zero_features_1200_valid':True}
for name,txt in [('tampered_feature',s.replace('0.0','0.1',1)),('duplicate_key',s.replace('"integrity_version":1','"integrity_version":1,"integrity_version":1',1)),('same_shape_wrong_semantics',line(dict(row,input_stage='RAW_NUMBER96'))),('future_input',line(dict(row,max_source_issue='202609100003'))),('duplicate_issue',s)]:
 try:m.validate(m.read_integrity_row(txt),row['target_issue'] if name=='duplicate_issue' else '')
 except ValueError:results[name]=True
 else:raise AssertionError(name)
(root/'reports/r928/DATA_INTEGRITY_TESTS.json').write_text(json.dumps({'scope':'SYNTHETIC_INPUT_CONTRACT_TESTS_NOT_MODEL_PROOF','results':results},indent=2)+'\n')
print('R928_DATA_INTEGRITY_PASS',len(results))
