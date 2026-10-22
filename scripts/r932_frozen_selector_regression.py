#!/usr/bin/env python3
"""Synthetic frozen-parent second-pass fitting, unchanged upstream bytes and actual phone loader."""
import os,sys,json,copy,subprocess,zipfile
from pathlib import Path
import numpy as np
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R))
from cryptography.hazmat.primitives import serialization,hashes
from cryptography.hazmat.primitives.asymmetric import rsa,padding
from training.r929 import pipeline as p
from training.r932 import frozen_selector as f
from training.r932.export_bundle import export as upstream_export

D=R/'build/r932-frozen-selector';D.mkdir(parents=True,exist_ok=True)
source=R/'build/r932-training-roundtrip/models';contract=R/'build/r932-training-roundtrip/runtime_contract.json'
O=R/'build/r932-model-bundle';A=R/'build/r930-batch34-sql';H=R/'build/r925-host/compiled-classes';platform=Path(os.environ.get('ANDROID_PLATFORM_JAR',R.parent/'08_TOOLCHAIN/android-platform-36/android.jar'))
cp=os.pathsep.join(map(str,[O,A,H,platform]))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(O),str(R/'verification/r932/TrainingBundleHarnessR932.java'),str(R/'app/src/main/java/com/ethk3/novaonepro/ModelBundleRuntimeR932.java')],check=True)
cp=os.pathsep.join(map(str,[O,A,H,A/'platform-classes',platform]))
key=rsa.generate_private_key(public_exponent=65537,key_size=2048);private=D/'synthetic-owner.pem';public=D/'synthetic-owner.der'
private.write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()));private.chmod(0o600)
public.write_bytes(key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo))
checks=[]
def check(ok,name):
    if not ok:raise AssertionError(name)
    checks.append(name)
def reject(fn,name):
    try:fn()
    except ValueError:check(True,name);return
    raise AssertionError(name+' accepted')
try:
    parent_path=D/'synthetic-parent.zip';upstream_export(source,contract,private,parent_path,4102444800000)
    parent,components,payloads=f.parent(parent_path,public,p.read_json(contract))
    for name in ('features.npy','labels.npy'):
        target=D/name
        if not target.exists():target.symlink_to(source/name)
    x=np.load(D/'features.npy',mmap_mode='r');y=np.load(D/'labels.npy',mmap_mode='r');n=len(x)
    scores=p.model_scores(p.read_artifact(source/'ranker_artifact.json')['model'],x);ordinals=p.rank_order(scores,x[:,:,0])[:,0]
    rng=np.random.default_rng(9320911);inputs=rng.uniform(0,1,(n,12));issues=['20270101%04d'%(i+1) for i in range(n)]
    manifest=p.read_json(source/'dataset_manifest.json');manifest.update(start_issue=issues[0],end_issue=issues[-1],issues_sha256=p.digest(p.canonical(issues)))
    p.write_json(D/'issues.json',issues)
    captures=[{'inputs':inputs[i].tolist(),'hit':int(y[i,ordinals[i]]),'selected_ordinal':int(ordinals[i]),
               'selection_policy_sha256':parent['selection_policy_sha256'],'schema_sha256':components['PREDICTABILITY']['schema_sha256'],
               'prediction_sha256':p.digest(('SYNTHETIC_PREDRAW_'+str(i)).encode()),'rank_score':float(scores[i,ordinals[i]]),
               'model_bundle_sha256':parent['bundle_sha256'],'created_at':1798761600000+i*60000} for i in range(n)]
    def install(rows):
        p.write_json(D/'predictability_captures.json',rows);manifest['predictability_captures_sha256']=p.file_digest(D/'predictability_captures.json')
        manifest.pop('dataset_contract_sha256',None);manifest['dataset_contract_sha256']=p.digest(p.canonical(manifest));p.write_json(D/'dataset_manifest.json',manifest)
    # Isolate this artificial experiment by its newly generated parent; production registry remains global.
    os.environ['STARCORE_EXPERIMENT_REGISTRY']=str(D/('synthetic-registry-'+parent['bundle_sha256'][:16]))
    install(captures);result=f.fit(D,parent_path,public,contract)
    check(result['trust']['model'] is not None,'INDEPENDENT_TRUST_FITS_LATER_EXACT_FROZEN_SELECTOR')
    check(result['calibration']['selection_policy_sha256']==parent['selection_policy_sha256'],'CALIBRATION_KEEPS_INSTALLED_SELECTOR_IDENTITY')
    check(result['evaluation']['real_n']==0 and not result['evaluation']['model_proven'],'OFFLINE_SELECTION_NEVER_INVENTS_REAL_EXECUTIONS')
    changed=copy.deepcopy(captures);sa,sb=manifest['splits']['sealed']
    for i in range(sa,sb):changed[i]['hit']=1-changed[i]['hit']
    install(changed);after=f.fit(D,parent_path,public,contract,evaluate=False)
    check(after['calibration']['model']==result['calibration']['model'] and after['trust']['model']==result['trust']['model'] and after['policy']['chosen']==result['policy']['chosen'],'SEALED_LABELS_CANNOT_CHANGE_CAL_TRUST_OR_POLICY')
    missing=copy.deepcopy(captures);missing[3].pop('rank_score');install(missing)
    reject(lambda:f.fit(D,parent_path,public,contract),'MISSING_FROZEN_FINAL_SCORE_BLOCKS_SECOND_PASS')
    drift=copy.deepcopy(captures);drift[3]['selection_policy_sha256']=p.digest(b'OTHER');install(drift)
    reject(lambda:f.fit(D,parent_path,public,contract),'CHANGED_SELECTOR_CAPTURE_BLOCKS_SECOND_PASS')
    install(captures);f.fit(D,parent_path,public,contract)
    child=D/'synthetic-second-pass.zip';package=f.export(D,parent_path,public,contract,private,child)
    with zipfile.ZipFile(child) as archive:
        child_files={name:archive.read(name) for name in archive.namelist()}
        for name in ('NUMBER','GATE','RANKER'):check(child_files[name.lower()+'.json']==payloads[name],name+'_PRESERVED_BYTE_FOR_BYTE')
    loaded=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.TrainingBundleHarnessR932',str(child),str(public),str(R/'verification/r925/sqlite_bridge.py'),'202701010200'],capture_output=True,text=True)
    if loaded.returncode:raise AssertionError(loaded.stdout+loaded.stderr)
    check('R932_TRAINING_TO_SIGNED_PHONE_LOADER_PASS' in loaded.stdout,'SIGNED_SECOND_PASS_LOADS_THROUGH_REAL_JAVA_IMPORTER')
    # Even an owner-signed package cannot relabel changed upstream bytes as preserved parents.
    policy=json.loads(child_files['policy.json']);certificate=json.loads(policy['parameters']['certificate_json'])
    certificate['frozen_upstream']['artifacts']['NUMBER']=p.digest(b'WRONG_PARENT')
    policy['parameters']['certificate_json']=p.canonical(certificate).decode();child_files['policy.json']=p.canonical(policy)
    child_manifest=json.loads(child_files['manifest.json']);child_manifest['certificate_sha256']=p.digest(policy['parameters']['certificate_json'].encode())
    for entry in child_manifest['artifacts']:
        if entry['type']=='POLICY':entry.update(size=len(child_files['policy.json']),sha256=p.digest(child_files['policy.json']))
    records=['|'.join(str(e[k]) for k in ('type','path','sha256','size','input_stage','schema_sha256','parent_sha256')) for e in sorted(child_manifest['artifacts'],key=lambda e:e['type'])]
    bundle=p.digest((child_manifest['revision']+'|'+child_manifest['selection_policy_sha256']+'|'+child_manifest['input_schema_sha256']+'|'+child_manifest['certificate_sha256']+'\n'+'\n'.join(records)).encode())
    child_manifest['bundle_sha256']=bundle;child_files['manifest.json']=p.canonical(child_manifest);child_files['signature.bin']=key.sign(bundle.encode(),padding.PKCS1v15(),hashes.SHA256())
    bad=D/'synthetic-wrong-parent.zip'
    with zipfile.ZipFile(bad,'w') as archive:
        for name,raw in child_files.items():archive.writestr(name,raw)
    rejected=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.TrainingBundleHarnessR932',str(bad),str(public),str(R/'verification/r925/sqlite_bridge.py'),'202701010200'],capture_output=True,text=True)
    check(rejected.returncode!=0 and 'R932_FROZEN_UPSTREAM_BYTES_CHANGED' in rejected.stderr,'SIGNED_FALSE_PARENT_BINDING_REJECTED_BY_PRODUCTION_LOADER')
    report={'status':'PASS','scope':'SYNTHETIC_FROZEN_INPUT_SECOND_PASS_HOST_SQLITE_NOT_REAL_FUTURE_NOT_DEVICE','checks':checks,'package':package}
    p.write_json(R/'reports/r932/FROZEN_SELECTOR_REGRESSION.json',report);print(json.dumps(report))
finally:
    private.unlink(missing_ok=True)
