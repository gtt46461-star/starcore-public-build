#!/usr/bin/env python3
"""Consistency-only check of normalized period evidence, never a source attestation.
Run on a read-only export joined to original SQLite rows by B04/B12's exporter.
Synthetic fixtures must stay SYNTHETIC; this script cannot approve natural quality.
"""
import json, argparse, pathlib, collections, re

def check(d):
 errors=[]
 def need(ok,code):
  if not ok:errors.append(code)
 def unique(rows,key,label):
  out={}
  for row in rows:
   k=tuple(row.get(f) for f in key)
   need(k not in out,label+':DUPLICATE:'+repr(k));out[k]=row
  return out
 opportunities=unique(d.get('opportunities',[]),('game','target'),'OPPORTUNITY')
 predictions=unique(d.get('predictions',[]),('game','target','lane','policy_hash'),'FREEZE')
 settlements=unique(d.get('settlements',[]),('freeze_id',),'SETTLEMENT')
 reals=unique(d.get('real',[]),('game','target'),'REAL')
 draws=unique(d.get('draws',[]),('game','target'),'DRAW')
 cert_rows=unique(d.get('certificates',[]),('id',),'CERTIFICATE')
 certs={k[0]:c for k,c in cert_rows.items()}
 need(bool(opportunities),'NO_OPPORTUNITIES')
 freeze_by_id={}
 for key,p in predictions.items():
  label=str(p.get('freeze_id'));need(label not in freeze_by_id,'FREEZE_ID_DUPLICATE:'+label);freeze_by_id[label]=p
  need(bool(p.get('freeze_id')),'FREEZE_ID_EMPTY')
  for h in ['policy_hash','model_hash','abi']:
   need(isinstance(p.get(h),str) and bool(p[h]) and p[h]!='UNKNOWN','IDENTITY_'+h+':'+label)
  o=opportunities.get((p.get('game'),p.get('target')))
  need(o is not None,'FREEZE_WITHOUT_OPPORTUNITY:'+label)
  nums=p.get('numbers',[])
  need(len(nums)==3 and len(set(nums))==3 and all(isinstance(n,int) and not isinstance(n,bool) and 1<=n<=10 for n in nums),'K3:'+label)
  need(isinstance(p.get('position'),int) and 1<=p['position']<=10,'POSITION:'+label)
  need(p.get('input_ordinal',10**30)<p.get('target_ordinal',-1),'PIT_INPUT:'+label)
  if o:
   need(p.get('durable_wall',10**30)<=o.get('fixed_deadline_wall',-1),'LATE_FREEZE:'+label)
   need(bool(p.get('boot_id')) and p.get('boot_id')==o.get('boot_id'),'CLOCK_EPOCH:'+label)
   need(p.get('durable_mono',10**30)<=o.get('fixed_deadline_mono',-1),'LATE_MONOTONIC_FREEZE:'+label)
  dr=draws.get((p.get('game'),p.get('target')))
  if dr:need(p.get('durable_wall',10**30)<dr.get('first_available_wall',-1),'FUTURE_VISIBLE:'+label)
 for _,s in settlements.items():
  label=str(s.get('freeze_id'));p=freeze_by_id.get(label);need(p is not None,'SETTLE_WITHOUT_FREEZE:'+label)
  if not p:continue
  dr=draws.get((p.get('game'),p.get('target')));need(dr is not None,'SETTLE_WITHOUT_DRAW:'+label)
  if dr:
   need(s.get('label_wall',-1)>=max(dr.get('source_result_wall',10**30),dr.get('first_available_wall',10**30)),'EARLY_LABEL:'+label)
   need(s.get('draw_hash')==dr.get('hash'),'DRAW_HASH:'+label)
 for key,r in reals.items():
  p=freeze_by_id.get(str(r.get('freeze_id')));c=certs.get(r.get('certificate_id'))
  need(p is not None,'REAL_WITHOUT_FREEZE:'+repr(key));need(c is not None,'REAL_WITHOUT_CERT:'+repr(key))
  if p:
   need(p.get('lane')=='REAL','SHADOW_AS_REAL:'+repr(key))
   need((p.get('game'),p.get('target'))==key,'REAL_TARGET:'+repr(key))
   need(r.get('position')==p.get('position'),'REAL_POSITION:'+repr(key))
  if c and p:
   for k in ['policy_hash','model_hash','abi']:need(c.get(k)==p.get(k),'CERT_'+k+':'+repr(key))
   need(c.get('state')=='ACTIVE' and c.get('active_from_wall',10**30)<=p.get('durable_wall',-1)<c.get('expires_wall',-1),'CERT_VALIDITY:'+repr(key))
 need(d.get('evidence_role') in ['SYNTHETIC','DEVICE_SQL_READBACK','REPLAY'],'EVIDENCE_ROLE')
 return {'schema':'R34_PERIOD_TRACE_CONSISTENCY_1','status':'PASS' if not errors else 'FAIL','errors':errors,'opportunities':len(opportunities),'frozen_rows':len(predictions),'settled_rows':len(settlements),'real_authorizations':len(reals),'source_attestation':'NOT_VERIFIED_BY_THIS_SCRIPT','natural_quality':'NOT_EVALUATED','may_authorize_real':False}
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('trace',type=pathlib.Path);a=p.parse_args();out=check(json.loads(a.trace.read_text()));print(json.dumps(out,ensure_ascii=False,indent=2));raise SystemExit(out['status']!='PASS')
