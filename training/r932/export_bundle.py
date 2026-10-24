"""Package verified training artifacts for the APK's signed six-component loader.

Default output is research inference, never an effectiveness certificate. The model owner's
separate RSA key is supplied by path; neither its bytes nor password enter the bundle or APK.
"""
from __future__ import annotations
import argparse,copy,hashlib,json,zipfile
from pathlib import Path
import numpy as np
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import padding,rsa
from training.r929 import pipeline as p,deep_candidates as d
from training.r932 import drift_policy_r82 as drift

def export(directory:Path,contract_path:Path,key_path:Path,output:Path,valid_until_ms:int):
    contract=p.read_json(contract_path);_x,_y,issues,dataset=p.load_dataset(directory)
    if contract['revision']!='R932_RUNTIME_PARAMETERS_V1' or contract['game']!=dataset['game'] or contract['meta_schema_sha256']!=dataset['feature_manifest_sha256']:
        raise ValueError('TARGET_APK_DATASET_CONTRACT_MISMATCH')
    if valid_until_ms<=0:raise ValueError('VALID_UNTIL_REQUIRED')
    comparison=p.read_json(directory/'number_comparison.json');expected=comparison.pop('result_sha256')
    if p.digest(p.canonical(comparison))!=expected:raise ValueError('NUMBER_COMPARISON_HASH_DRIFT')
    family=comparison['tune_champion'];number=p.read_artifact(directory/f'number_{family}.json') if family in ('D0','D1','D2','D3') else None
    gate=p.read_artifact(directory/'gate_stage.json');ranker=p.read_artifact(directory/'ranker_artifact.json');cal=p.read_artifact(directory/'calibration_artifact.json');policy=p.read_artifact(directory/'policy_artifact.json')
    if cal['ranker_sha256']!=ranker['artifact_sha256'] or policy['calibration_sha256']!=cal['artifact_sha256']:raise ValueError('FROZEN_PARENT_ARTIFACT_DRIFT')
    trust=p.read_artifact(directory/'predictability_artifact.json') if (directory/'predictability_artifact.json').exists() else p.stage_predictability(directory)
    components=copy.deepcopy(contract['components']);data=dataset['dataset_contract_sha256']
    for name,component in components.items():
        params=component['parameters'];params['DATA_SHA256']=data
        if 'PROVEN' in params:params['PROVEN']=False
        if 'MODEL_GATE_PASSED' in params:params['MODEL_GATE_PASSED']=False
        component['trained_until_issue']=issues[dataset['splits']['train'][1]-1]
    n=components['NUMBER'];n['input_stage']=n['deep_input_stage'];n['schema_sha256']=n['deep_schema_sha256'];n['parameters']['TRAINED_MODEL']=number['model'] if number else {'kind':family};n['parameters']['TEMPERATURE']=1.0
    if family=='D2':
        d.require_contiguous_issues(issues)
        state,experts,_actual=p.load_raw_dataset(directory,dataset);features=d.feature_vectors(state,experts)
        memory=np.zeros(features.shape[1])
        for row in features:memory=.95*memory+.05*row
        n['parameters']['SLOW_INITIAL']=memory.tolist();n['parameters']['SLOW_CHECKPOINT_ISSUE']=issues[-1]
    g=components['GATE'];g['input_stage']=g['deep_input_stage'];g['schema_sha256']=g['deep_schema_sha256'];g['parameters']['TRAINED_MODEL']=gate['model']
    components['RANKER']['parameters']['TRAINED_MODEL']=ranker['model'];components['RANKER']['parameters']['STABLE_FOLDS']=5
    components['RANKER']['parameters']['RANK_ORDER_PASSED']=False
    # The parameter family is exact; proof flags remain false until separate measured certification.
    c=components['CALIBRATION'];c['parameters']['TRAINED_MODEL']=cal['model'];c['trained_until_issue']=issues[dataset['splits']['calibration'][1]-1]
    m=cal['metrics'];c['parameters'].update(INPUT_SCHEMA_SHA256=contract['meta_schema_sha256'],OOS_BRIER=m['brier'],OOS_ECE=m['ece'],CALIBRATION_SUPPORT_N=m['n'],CALIBRATION_HITS=m['hits'],RELIABILITY_BIN_MIN_N=m['reliability_bin_min_n'])
    components['POLICY']['trained_until_issue']=issues[dataset['splits']['policy'][1]-1]
    drift_path=directory/'drift_policy_r82.json'
    if drift_path.exists():
        monitor=drift.validate(p.read_json(drift_path),issues[dataset['splits']['tune'][1]-1])
        if monitor['tune_first']<issues[dataset['splits']['tune'][0]]:raise ValueError('R82_DRIFT_TUNE_OUTSIDE_DECLARED_SPLIT')
        components['POLICY']['parameters']['drift_policy_r82']=monitor
    files={};entries=[];artifact_hashes={};selector=''
    for name in ('NUMBER','GATE','RANKER','CALIBRATION','PREDICTABILITY','POLICY'):
        component=components[name];params=component['parameters']
        if name=='CALIBRATION':
            identity=p.digest('|'.join(artifact_hashes[k] for k in ('NUMBER','GATE','RANKER')).encode())
            selector=p.digest('|'.join(['R929_SELECTION_EVIDENCE_V1',identity,'V51_RUNTIME_RANK1_THEN_CERTIFIED_RESEARCH_MIXERS','CERTIFIED_RESEARCH_WEIGHT_CONTRACT','PREDRAW_STATE_DOMAIN_ONLY',contract['tie_break'],contract['meta_schema_sha256']]).encode())
            params['SELECTION_POLICY_SHA256']=selector
        if name=='PREDICTABILITY' and trust.get('model') is not None:
            if trust['selection_policy_sha256']!=selector or trust['schema_sha256']!=component['schema_sha256']:
                raise ValueError('PREDICTABILITY_RECAPTURE_REQUIRED_FOR_NEW_SELECTOR')
            params['TRAINED_MODEL']=trust['model'];params['SELECTION_POLICY_SHA256']=selector;params['QUALIFIED_THRESHOLD']=trust['qualified_threshold']
            component['trained_until_issue']=trust['trained_until_issue']
        if name=='POLICY':
            cert={'selection_policy_sha256':selector,'input_schema_sha256':contract['meta_schema_sha256'],'data_sha256':data,'code_sha256':contract['source_sha256'],
                  'host_parity_pass':False,'android_parity_pass':False,'sealed_evaluation_pass':False,'resource_pass':False,'scope':'RESEARCH_INFERENCE_ONLY_NOT_EFFECT_CERTIFICATE',
                  'training_ranker_sha256':ranker['artifact_sha256'],'training_calibration_sha256':cal['artifact_sha256'],'training_policy_sha256':policy['artifact_sha256']}
            certificate=p.canonical(cert).decode();params['certificate_json']=certificate
        payload={'revision':contract['revision'],'type':name,'input_stage':component['input_stage'],'schema_sha256':component['schema_sha256'],'trained_until_issue':component['trained_until_issue'],'valid_until_ms':valid_until_ms,'parameters':params}
        raw=p.canonical(payload);path=name.lower()+'.json';sha=p.digest(raw);files[path]=raw;artifact_hashes[name]=sha
        parent=artifact_hashes['RANKER'] if name=='CALIBRATION' else artifact_hashes['CALIBRATION'] if name=='POLICY' else ''
        entries.append({'type':name,'path':path,'sha256':sha,'size':len(raw),'input_stage':component['input_stage'],'schema_sha256':component['schema_sha256'],'parent_sha256':parent})
    certificate_hash=p.digest(certificate.encode());records=[]
    for entry in sorted(entries,key=lambda e:e['type']):records.append('|'.join(str(entry[k]) for k in ('type','path','sha256','size','input_stage','schema_sha256','parent_sha256')))
    bundle_hash=p.digest((contract['manifest_revision']+'|'+selector+'|'+contract['meta_schema_sha256']+'|'+certificate_hash+'\n'+'\n'.join(records)).encode())
    manifest={'revision':contract['manifest_revision'],'bundle_sha256':bundle_hash,'selection_policy_sha256':selector,'input_schema_sha256':contract['meta_schema_sha256'],'certificate_sha256':certificate_hash,'artifacts':entries}
    key=serialization.load_pem_private_key(key_path.read_bytes(),password=None)
    if not isinstance(key,rsa.RSAPrivateKey) or key.key_size<2048:raise ValueError('MODEL_OWNER_RSA_KEY_2048_REQUIRED')
    files['manifest.json']=p.canonical(manifest);files['signature.bin']=key.sign(bundle_hash.encode(),padding.PKCS1v15(),hashes.SHA256())
    output.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,raw in files.items():archive.writestr(name,raw)
    return {'bundle_hash':bundle_hash,'sha256':p.file_digest(output),'state':'SIGNED_RESEARCH_INFERENCE','model_proven':False,'source_sha256':contract['source_sha256'],'predictability_state':trust['state']}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--models',type=Path,required=True);parser.add_argument('--runtime-contract',type=Path,required=True);parser.add_argument('--owner-key',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--valid-until-ms',type=int,required=True)
    args=parser.parse_args();print(json.dumps(export(args.models,args.runtime_contract,args.owner_key,args.output,args.valid_until_ms),ensure_ascii=False))
if __name__=='__main__':main()

