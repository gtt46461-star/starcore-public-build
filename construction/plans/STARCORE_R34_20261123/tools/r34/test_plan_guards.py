#!/usr/bin/env python3
import copy,json,pathlib,tempfile,zipfile,hashlib
from unittest.mock import patch
from verify_period_trace import check
from check_acceptance import inspect
D={'evidence_role':'SYNTHETIC','opportunities':[{'game':'BINANCE1','target':'T2','fixed_deadline_wall':2000,'fixed_deadline_mono':1900,'boot_id':'boot1'}],
'predictions':[{'game':'BINANCE1','target':'T2','target_ordinal':2,'input_ordinal':1,'freeze_id':'f1','lane':'REAL','policy_hash':'p1','model_hash':'m1','abi':'a1','position':1,'numbers':[1,2,3],'durable_wall':1990,'durable_mono':1890,'boot_id':'boot1'}],
'draws':[{'game':'BINANCE1','target':'T2','source_result_wall':2050,'first_available_wall':2100,'hash':'d1'}],
'settlements':[{'freeze_id':'f1','label_wall':2100,'draw_hash':'d1'}],
'certificates':[{'id':'c1','policy_hash':'p1','model_hash':'m1','abi':'a1','state':'ACTIVE','active_from_wall':1000,'expires_wall':5000}],
'real':[{'game':'BINANCE1','target':'T2','freeze_id':'f1','certificate_id':'c1','position':1}]}
assert check(D)['status']=='PASS';cases=['valid_known_fixture']
def reject(name,fn):
 d=copy.deepcopy(D);fn(d);assert check(d)['status']=='FAIL',name;cases.append(name)
reject('future_label_in_features',lambda d:d['predictions'][0].update(input_ordinal=2))
reject('deadline_late_even_before_result',lambda d:d['predictions'][0].update(durable_wall=2001))
reject('result_already_visible',lambda d:d['draws'][0].update(first_available_wall=1980))
reject('wall_clock_looks_ok_monotonic_late',lambda d:d['predictions'][0].update(durable_mono=2000))
reject('clock_epoch_changed',lambda d:d['predictions'][0].update(boot_id='boot2'))
reject('draw_label_before_observation',lambda d:d['settlements'][0].update(label_wall=2099))
reject('two_real_positions_same_game_target',lambda d:d['real'].append(dict(d['real'][0],position=2)))
reject('shadow_must_not_become_real',lambda d:d['predictions'][0].update(lane='SHADOW'))
reject('changed_policy_cannot_inherit_certificate',lambda d:d['predictions'][0].update(policy_hash='p2'))
reject('duplicate_number_not_k3',lambda d:d['predictions'][0].update(numbers=[1,1,3]))
reject('changed_draw_cannot_reuse_label',lambda d:d['settlements'][0].update(draw_hash='d2'))
reject('repeated_settlement',lambda d:d['settlements'].append(dict(d['settlements'][0])))
reject('missing_opportunity_denominator',lambda d:d['opportunities'].clear())
reject('certificate_expired',lambda d:d['certificates'][0].update(expires_wall=1980))
with tempfile.TemporaryDirectory() as tmp:
 result=inspect(pathlib.Path(tmp),{'_plan_sha256':'x','B00':{'all_done':True}},{'batches':[{'id':'B00','issues':['I1']} ]})
 assert not result['all_delivery_verified'] and not result['application_runtime_accepted'] and not result['natural_advantage_proven'];cases.append('self_reported_all_done_cannot_approve')
 # This fixture tests SDK-output parsing and receipt state handling only. Its
 # verifier outputs are mocked and are never evidence of a real signed APK.
 root=pathlib.Path(tmp)
 (root/'log.txt').write_text('synthetic identity parser fixture')
 with zipfile.ZipFile(root/'fixture.apk','w') as z:
  z.writestr('AndroidManifest.xml',b'synthetic');z.writestr('classes.dex',b'synthetic')
 def item(name):
  p=root/name
  return {'path':name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
 receipt={'source_commit':'1'*40,'plan_sha256':'2'*64,'issue_receipts':{'I1':item('log.txt')},'apk':dict(item('fixture.apk'),certificate_sha256='3'*64,version_code=577311096),'host_log':item('log.txt'),'android_build_log':item('log.txt'),'device_evidence':None,'natural_evidence':None}
 scope={'batches':[{'id':'B00','issues':['I1']}]}
 def inspect_fixture(package,rec=None):
  def fake_sdk(args):
   return 'Signer #1 certificate SHA-256 digest: '+('3'*64) if args[0]=='fixture-apksigner' else "package: name='"+package+"' versionCode='577311096'"
  with patch('check_acceptance.run',side_effect=fake_sdk):
   return inspect(root,{'_plan_sha256':'2'*64,'B00':rec or receipt},scope,'fixture-apksigner','fixture-aapt2','2'*64)
 correct=inspect_fixture('com.starcore.research.r13')
 assert correct['all_delivery_verified'] and not correct['application_runtime_accepted'];cases.append('real_application_id_parser_fixture')
 wrong=inspect_fixture('com.ethk3.novaonepro')
 assert not wrong['all_delivery_verified'] and 'APK_PACKAGE_VERSION' in wrong['batches'][0]['errors'];cases.append('java_package_is_not_application_id')
 bad=copy.deepcopy(receipt);bad['device_evidence']={'path':'missing.json','bytes':1,'sha256':'4'*64}
 assert not inspect_fixture('com.starcore.research.r13',bad)['all_delivery_verified'];cases.append('missing_device_reference_does_not_leave_delivery_verified')
 assert not inspect(root,{}, {'batches':[]})['all_delivery_verified'];cases.append('empty_batch_scope_not_verified')
 untrusted=inspect(root,{'_plan_sha256':'2'*64,'B00':receipt},scope,'fixture-apksigner','fixture-aapt2','5'*64)
 assert not untrusted['all_delivery_verified'] and 'UNTRUSTED_PLAN_SEAL' in untrusted['batches'][0]['errors'];cases.append('self_consistent_but_wrong_plan_seal_rejected')
print(json.dumps({'status':'PASS','cases':cases,'assertion_count':len(cases),'application_compile':'NOT_RUN','device':'NOT_RUN','nature':'NOT_PROVEN'},ensure_ascii=False,indent=2))
