"""Supplemental 173 field-repair identity; original R34.2 release series is preserved."""
from pathlib import Path
import hashlib
import os
import re
from verify_plan_binding import git, read, require, safe, sha

BASELINE = '4d1de9d83692635b5c5ce39144fece1c8a31c8ac'
BASELINE_CODE = 577311173
BASELINE_APK = '9304472262455c12b4600708fb23480d698acf9f3ba710ddee86cda214ae8750'
REPORT_SHA256 = '3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86'
PLAN_PATH = 'construction/plans/STARCORE_R34_3_REPAIR_20261126'
BINDING = 'release/field173_plan_binding.json'
SCHEMA = 'STARCORE_FIELD173_PLAN_BINDING_1'

def verify_plan(root, seal):
    root=Path(root).resolve()
    manifest=safe(root,'MANIFEST_SHA256.json')
    require(sha(manifest)==seal,'FIELD173_PLAN_SEAL_MISMATCH')
    content=read(manifest)
    require(content['schema']=='STARCORE_R343_PLAN_MANIFEST_1'
            and content['source_commit']==BASELINE,'FIELD173_PLAN_BASELINE_MISMATCH')
    expected={'MANIFEST_SHA256.json'}
    for row in content['files']:
        name=row['path']; leaf=safe(root,name)
        require(name not in expected and leaf.is_file() and leaf.stat().st_size==row['bytes']
                and sha(leaf)==row['sha256'],'FIELD173_PLAN_MEMBER_MISMATCH:'+name)
        expected.add(name)
    actual={p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file()
            and '__pycache__' not in p.parts and p.suffix not in ('.pyc','.pyo')}
    require(expected==actual and len(expected)==content['file_count']+1,'FIELD173_PLAN_MEMBER_SET')
    require(sha(safe(root,'report/ORIGINAL_DIAGNOSTIC_173.zip'))==REPORT_SHA256,
            'FIELD173_ORIGINAL_REPORT_MISMATCH')

def identity(repo, head, tree, version, plan_root=None):
    repo=Path(repo).resolve(); pin=read(safe(repo,BINDING))
    require(set(pin)=={'schema','planCommit','planSha256','sourceBaseline','reportSha256'},
            'FIELD173_BINDING_FIELDS')
    require(pin['schema']==SCHEMA and pin['sourceBaseline']==BASELINE
            and pin['reportSha256']==REPORT_SHA256
            and re.fullmatch('[0-9a-f]{40}',pin['planCommit'])
            and re.fullmatch('[0-9a-f]{64}',pin['planSha256']),'FIELD173_BINDING_INVALID')
    git(repo,'merge-base','--is-ancestor',BASELINE,head)
    raw=git(repo,'show',pin['planCommit']+':'+PLAN_PATH+'/MANIFEST_SHA256.json')
    require(hashlib.sha256(raw).hexdigest()==pin['planSha256'],'FIELD173_GIT_PLAN_PIN_MISMATCH')
    external=plan_root or os.environ.get('STARCORE_FIELD173_PLAN_ROOT')
    require(external,'STARCORE_FIELD173_PLAN_ROOT_REQUIRED')
    for root in {str(safe(repo,PLAN_PATH)),str(Path(external).resolve())}: verify_plan(root,pin['planSha256'])
    for prior in ('r33','r34','r341','r342'):
        name='release/'+prior+'_plan_binding.json'
        require(safe(repo,name).read_bytes()==git(repo,'show',BASELINE+':'+name),
                'FIELD173_HISTORICAL_BINDING_CHANGED:'+prior)
    code=version['versionCode']
    match=re.fullmatch(r'R34\.2-(B(?:0[1-9]|1[0-2]))-(\d{4}-\d\d-\d\d)-(\d+)',version['versionName'])
    require(type(code) is int and BASELINE_CODE<code<=2100000000 and match
            and int(match[3])==code and version['applicationId']=='com.starcore.research.r13',
            'FIELD173_RELEASE_VERSION_MISMATCH')
    return {**pin,'schema':'STARCORE_FIELD173_BUILD_IDENTITY_1','planRevision':'R34.3-173',
            'apkSeries':'R34.2','sourceCommit':head,'sourceTree':tree,
            'batch':match[1],'versionCode':code,'versionName':version['versionName'],
            'sourceApkEvidenceRef':{'versionCode':BASELINE_CODE,'sha256':BASELINE_APK},
            'engineeringAcceptance':'SEPARATE_CURRENT_BATCH_RECEIPT_REQUIRED',
            'deviceAcceptance':'NOT_INFERRED','naturalQuality':'NOT_INFERRED'}
