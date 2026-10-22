"""Recalibrate a deployed selector without changing one byte of Number/Gate/Ranker.

Consumes only later, directly frozen final-selection scores and trust inputs. This
is the second pass after deploying a new upstream research bundle and collecting
new observations; missing evidence blocks, rather than being reconstructed.
"""
from pathlib import Path
import argparse,copy,json,zipfile
import numpy as np
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import padding,rsa
from training.r929 import pipeline as p
from training.r932 import predictability as trust

ROLES=('NUMBER','GATE','RANKER','CALIBRATION','PREDICTABILITY','POLICY')


def parent(path:Path,public_path:Path,contract):
    with zipfile.ZipFile(path) as archive:
        entries=archive.infolist()
        if len(entries)!=8 or len({x.filename for x in entries})!=8 or any(x.file_size>40_000_000 for x in entries):raise ValueError('FROZEN_PARENT_ZIP_CONTRACT')
        files={x.filename:archive.read(x) for x in entries}
    manifest=json.loads(files['manifest.json'],object_pairs_hook=p.unique_object)
    if manifest['revision']!=contract['manifest_revision'] or manifest['input_schema_sha256']!=contract['meta_schema_sha256']:raise ValueError('FROZEN_PARENT_SCHEMA')
    artifacts=manifest['artifacts']
    if len(artifacts)!=6 or {x['type'] for x in artifacts}!=set(ROLES):raise ValueError('FROZEN_PARENT_SIX_ROLES')
    records=[];components={};payloads={}
    for entry in sorted(artifacts,key=lambda x:x['type']):
        raw=files[entry['path']]
        if len(raw)!=entry['size'] or p.digest(raw)!=entry['sha256']:raise ValueError('FROZEN_PARENT_PAYLOAD_HASH')
        records.append('|'.join(str(entry[k]) for k in ('type','path','sha256','size','input_stage','schema_sha256','parent_sha256')))
        component=json.loads(raw,object_pairs_hook=p.unique_object)
        if component['type']!=entry['type'] or component['input_stage']!=entry['input_stage'] or component['schema_sha256']!=entry['schema_sha256']:raise ValueError('FROZEN_PARENT_PAYLOAD_IDENTITY')
        components[entry['type']]=component;payloads[entry['type']]=raw
    bundle=p.digest((manifest['revision']+'|'+manifest['selection_policy_sha256']+'|'+manifest['input_schema_sha256']+'|'+manifest['certificate_sha256']+'\n'+'\n'.join(records)).encode())
    if bundle!=manifest['bundle_sha256']:raise ValueError('FROZEN_PARENT_MANIFEST_HASH')
    public=serialization.load_der_public_key(public_path.read_bytes())
    if not isinstance(public,rsa.RSAPublicKey) or public.key_size<2048:raise ValueError('FROZEN_PARENT_OWNER_KEY')
    public.verify(files['signature.bin'],bundle.encode(),padding.PKCS1v15(),hashes.SHA256())
    certificate=components['POLICY']['parameters']['certificate_json']
    if p.digest(certificate.encode())!=manifest['certificate_sha256'] or json.loads(certificate)['code_sha256']!=contract['source_sha256']:raise ValueError('FROZEN_PARENT_CODE_OR_CERTIFICATE')
    return manifest,components,payloads


def fit(out:Path,parent_path:Path,public_path:Path,contract_path:Path,evaluate=True):
    _x,_y,issues,dataset=p.load_dataset(out);contract=p.read_json(contract_path)
    manifest,components,payloads=parent(parent_path,public_path,contract)
    selector=manifest['selection_policy_sha256'];path=out/'predictability_captures.json'
    if p.file_digest(path)!=dataset['predictability_captures_sha256']:raise ValueError('FROZEN_CAPTURE_HASH')
    captures=p.read_json(path)
    if len(captures)!=len(issues) or any(row is None or 'rank_score' not in row for row in captures):raise ValueError('FROZEN_FINAL_SELECTION_CAPTURE_REQUIRED')
    upstream_end=max(c['trained_until_issue'] for c in components.values())
    for issue,row in zip(issues,captures):
        if issue<=upstream_end or row['model_bundle_sha256']!=manifest['bundle_sha256'] or row['selection_policy_sha256']!=selector:raise ValueError('FROZEN_SELECTOR_IDENTITY_OR_PIT')
        if row['schema_sha256']!=contract['components']['PREDICTABILITY']['schema_sha256'] or not np.isfinite(row['rank_score']):raise ValueError('FROZEN_SELECTOR_INPUT_SCHEMA')
        if not 0<row.get('created_at',0)<min(c['valid_until_ms'] for c in components.values()):raise ValueError('FROZEN_SELECTOR_CAPTURE_EXPIRY')
    bound={'revision':'R932_FROZEN_UPSTREAM_SECOND_PASS_V1','parent_bundle_sha256':manifest['bundle_sha256'],
           'parent_file_sha256':p.file_digest(parent_path),'selection_policy_sha256':selector,
           'dataset_sha256':dataset['dataset_contract_sha256'],
           'upstream_hashes':{name:p.digest(payloads[name]) for name in ('NUMBER','GATE','RANKER')}}
    p.write_json(out/'frozen_upstream_contract.json',bound)
    independent=trust.stage(out)
    if independent.get('model') is None or independent['selection_policy_sha256']!=selector:raise ValueError('FROZEN_TRUST_MODEL_UNAVAILABLE')
    scores=np.asarray([row['rank_score'] for row in captures]);hits=np.asarray([row['hit'] for row in captures]);inputs=np.asarray([row['inputs'] for row in captures])
    ca,cb=dataset['splits']['calibration'];cut=ca+max(2,int((cb-ca)*.6));validation=cut+1
    if cb-validation<2:raise ValueError('FROZEN_CAL_EMBARGO_SUPPORT')
    trials=[]
    for learner in (p.logistic_fit,p.beta_fit,p.isotonic_fit):
        model=learner(scores[ca:cut],hits[ca:cut]);metrics=p.calibration_metrics(p.calibration_predict(model,scores[validation:cb]),hits[validation:cb]);trials.append({'model':model,'metrics':metrics})
    chosen=min(trials,key=lambda row:(row['metrics']['brier'],row['metrics']['ece'],row['model']['family']))
    cal={'revision':'R932_FROZEN_SELECTOR_CALIBRATION_V1',**chosen,'selection_policy_sha256':selector,
         'ranker_sha256':bound['upstream_hashes']['RANKER'],'fit_range':[ca,cut],'embargo_range':[cut,validation],
         'validation_range':[validation,cb],'trained_until_issue':issues[cut-1],'validation_until_issue':issues[cb-1],
         'dataset_sha256':dataset['dataset_contract_sha256'],'production_authority':False}
    cal['artifact_sha256']=p.digest(p.canonical(cal));p.write_json(out/'frozen_calibration_artifact.json',cal)
    prob=p.calibration_predict(cal['model'],scores);confidence=trust.predict(independent['model'],inputs)
    pa,pb=dataset['splits']['policy'];rows=[]
    for threshold in np.arange(.20,.701,.02):
        for q in (.35,.40,.45,.50,.60):
            selected=(prob[pa:pb]>=threshold)&(confidence[pa:pb]>=q);n=int(selected.sum())
            rows.append({'threshold':float(round(threshold,4)),'predictability_threshold':q,'eligible_n':pb-pa,'selected_n':n,
                         'coverage':float(selected.mean()),'hit_rate':float(hits[pa:pb][selected].mean()) if n else 0.,
                         'brier':float(np.mean((prob[pa:pb][selected]-hits[pa:pb][selected])**2)) if n else 1.,
                         'risk':float(1-hits[pa:pb][selected].mean()) if n else 1.})
    front=p.pareto_front(rows);feasible=[row for row in front if row['coverage']>=.15 and row['hit_rate']>=.40]
    selected=max(feasible,key=lambda row:(row['hit_rate'],row['coverage'],-row['brier'])) if feasible else None
    policy={'revision':'R932_FROZEN_SELECTOR_POLICY_V1','selection_policy_sha256':selector,'calibration_sha256':cal['artifact_sha256'],
            'predictability_sha256':independent['artifact_sha256'],'pareto':rows,'pareto_front':front,'chosen':selected,
            'trained_until_issue':issues[pb-1],'effect_status':'POLICY_TARGET_MET_ON_POLICY_SPLIT' if selected else 'FAIL_EFFECT',
            'decision_owner':'DecisionGateEngine_ONLY','production_authority':False,'frequency_debt':False,'forced_bet':False}
    policy['artifact_sha256']=p.digest(p.canonical(policy));p.write_json(out/'frozen_policy_artifact.json',policy)
    p.register_trial(dataset,'frozen-selector-fit',bound,'FROZEN_RESEARCH',policy['artifact_sha256'])
    if not evaluate:return {'contract':bound,'calibration':cal,'trust':independent,'policy':policy}
    token=p.consume_sealed_once(dataset,issues,{'frozen_parent':manifest['bundle_sha256'],'cal':cal['artifact_sha256'],'policy':policy['artifact_sha256']})
    sa,sb=dataset['splits']['sealed'];mask=np.zeros(sb-sa,dtype=bool)
    if selected:mask=(prob[sa:sb]>=selected['threshold'])&(confidence[sa:sb]>=selected['predictability_threshold'])
    report={'eligible_n':sb-sa,'selected_n':int(mask.sum()),'real_n':0,'real_scope':'OFFLINE_EVALUATION_HAS_NO_EXECUTION',
            'selected_coverage':float(mask.mean()),'selected_hit_rate':float(hits[sa:sb][mask].mean()) if mask.any() else 0.,
            'coverage_shortfall':float(max(0,.15-mask.mean())),'calibration':p.calibration_metrics(prob[sa:sb],hits[sa:sb]),
            'sealed_token':token,'future_live_n':0,'model_proven':False,'scope':'SEALED_NOT_FUTURE_NOT_DEVICE'}
    report['effect_status']='PASS_EFFECT' if report['selected_coverage']>=.15 and report['selected_hit_rate']>=.40 else 'FAIL_EFFECT'
    report['report_sha256']=p.digest(p.canonical(report));p.write_json(out/'frozen_sealed_evaluation.json',report)
    return {'contract':bound,'calibration':cal,'trust':independent,'policy':policy,'evaluation':report}


def export(out:Path,parent_path:Path,public_path:Path,contract_path:Path,owner_key:Path,output:Path):
    contract=p.read_json(contract_path);manifest,components,payloads=parent(parent_path,public_path,contract)
    bound=p.read_json(out/'frozen_upstream_contract.json');cal=p.read_artifact(out/'frozen_calibration_artifact.json');independent=p.read_artifact(out/'predictability_artifact.json');policy=p.read_artifact(out/'frozen_policy_artifact.json')
    _,_,_,dataset=p.load_dataset(out)
    if bound['parent_file_sha256']!=p.file_digest(parent_path) or bound['dataset_sha256']!=dataset['dataset_contract_sha256'] or any(p.digest(payloads[name])!=value for name,value in bound['upstream_hashes'].items()):raise ValueError('FROZEN_EXPORT_PARENT_CHANGED')
    if policy['calibration_sha256']!=cal['artifact_sha256'] or policy['predictability_sha256']!=independent['artifact_sha256'] or any(item['selection_policy_sha256']!=manifest['selection_policy_sha256'] for item in (cal,independent,policy)):raise ValueError('FROZEN_EXPORT_DOWNSTREAM_CHANGED')
    data=dataset['dataset_contract_sha256'];components=copy.deepcopy(components)
    for name in ('CALIBRATION','PREDICTABILITY','POLICY'):
        params=components[name]['parameters'];params['DATA_SHA256']=data
        if 'PROVEN' in params:params['PROVEN']=False
        if 'MODEL_GATE_PASSED' in params:params['MODEL_GATE_PASSED']=False
    c=components['CALIBRATION'];c['parameters']['TRAINED_MODEL']=cal['model'];c['trained_until_issue']=cal['validation_until_issue']
    m=cal['metrics'];c['parameters'].update(OOS_BRIER=m['brier'],OOS_ECE=m['ece'],CALIBRATION_SUPPORT_N=m['n'],CALIBRATION_HITS=m['hits'],RELIABILITY_BIN_MIN_N=m['reliability_bin_min_n'])
    t=components['PREDICTABILITY'];t['parameters'].update(TRAINED_MODEL=independent['model'],SELECTION_POLICY_SHA256=manifest['selection_policy_sha256'],QUALIFIED_THRESHOLD=policy['chosen']['predictability_threshold'] if policy['chosen'] else .40);t['trained_until_issue']=independent['trained_until_issue']
    component=components['POLICY'];component['trained_until_issue']=policy['trained_until_issue'];params=component['parameters'];params['CELLS']=[];params.pop('domains',None)
    # A research cell exposes frozen thresholds but cannot claim a proven policy surface.
    if policy['chosen']:
        row=policy['chosen'];params['CELLS']=[{'id':'R932_FROZEN_JOINT_RESEARCH','domain_prefix':'','lifecycle':'UNKNOWN','position':0,
            'selected_p_min':row['threshold'],'predictability_min':row['predictability_threshold'],'rank_margin_min':0.,'third_margin_min':-1.,
            'consensus_min':0.,'uncertainty_max':1.,'falsifier_max':1.,'sample':row['selected_n'],'hit_rate':row['hit_rate'],
            'credible_low':0.,'ece':1.,'block_minimum':0.,'max_drift':1.,'active':False}]
    certificate={'selection_policy_sha256':manifest['selection_policy_sha256'],'input_schema_sha256':manifest['input_schema_sha256'],
        'data_sha256':data,'code_sha256':contract['source_sha256'],'host_parity_pass':False,'android_parity_pass':False,
        'sealed_evaluation_pass':False,'resource_pass':False,'scope':'RESEARCH_INFERENCE_ONLY_NOT_EFFECT_CERTIFICATE',
        'frozen_upstream':{'parent_bundle_sha256':manifest['bundle_sha256'],'artifacts':bound['upstream_hashes'],
                           'data_sha256':components['NUMBER']['parameters']['DATA_SHA256'],'downstream_data_sha256':data},
        'calibration_artifact_sha256':cal['artifact_sha256'],'predictability_artifact_sha256':independent['artifact_sha256'],'policy_artifact_sha256':policy['artifact_sha256']}
    params['certificate_json']=p.canonical(certificate).decode();cert_hash=p.digest(params['certificate_json'].encode())
    files={};entries=[];hashes_by_role={}
    for name in ROLES:
        raw=payloads[name] if name in ('NUMBER','GATE','RANKER') else p.canonical(components[name]);component=components[name]
        path=name.lower()+'.json';sha=p.digest(raw);hashes_by_role[name]=sha;files[path]=raw
        entries.append({'type':name,'path':path,'sha256':sha,'size':len(raw),'input_stage':component['input_stage'],'schema_sha256':component['schema_sha256'],
                        'parent_sha256':hashes_by_role['RANKER'] if name=='CALIBRATION' else hashes_by_role['CALIBRATION'] if name=='POLICY' else ''})
    records=['|'.join(str(e[k]) for k in ('type','path','sha256','size','input_stage','schema_sha256','parent_sha256')) for e in sorted(entries,key=lambda x:x['type'])]
    bundle=p.digest((manifest['revision']+'|'+manifest['selection_policy_sha256']+'|'+manifest['input_schema_sha256']+'|'+cert_hash+'\n'+'\n'.join(records)).encode())
    result=dict(manifest,bundle_sha256=bundle,certificate_sha256=cert_hash,artifacts=entries);key=serialization.load_pem_private_key(owner_key.read_bytes(),password=None)
    public=serialization.load_der_public_key(public_path.read_bytes())
    if not isinstance(key,rsa.RSAPrivateKey) or key.public_key().public_numbers()!=public.public_numbers():raise ValueError('FROZEN_EXPORT_OWNER_CHANGED')
    files['manifest.json']=p.canonical(result);files['signature.bin']=key.sign(bundle.encode(),padding.PKCS1v15(),hashes.SHA256())
    with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,raw in files.items():archive.writestr(name,raw)
    return {'bundle_sha256':bundle,'selection_policy_sha256':manifest['selection_policy_sha256'],'upstream_bytes_unchanged':True,'model_proven':False,'sha256':p.file_digest(output)}


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--models',type=Path,required=True);parser.add_argument('--parent-bundle',type=Path,required=True);parser.add_argument('--owner-public-key',type=Path,required=True);parser.add_argument('--runtime-contract',type=Path,required=True);parser.add_argument('--owner-key',type=Path);parser.add_argument('--output',type=Path)
    args=parser.parse_args();result=fit(args.models,args.parent_bundle,args.owner_public_key,args.runtime_contract)
    if args.output:
        if args.owner_key is None:parser.error('--output requires --owner-key')
        result['package']=export(args.models,args.parent_bundle,args.owner_public_key,args.runtime_contract,args.owner_key,args.output)
    print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()

