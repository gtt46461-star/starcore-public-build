#!/usr/bin/env python3
"""Validate the sealed construction plan, never sign application completion.

--seal is a deliberate drafting operation. Normal execution is read-only.
The manifest excludes its own bytes, its seal, and SELF_CHECK.json to avoid
hash cycles. Transient Python cache files are never published.
"""
import argparse
import collections
import hashlib
import json
import pathlib
import re
import subprocess

EXCLUDED = {'MANIFEST_SHA256.json', 'control/PLAN_SEAL.json', 'SELF_CHECK.json'}
BASELINE = '5562f9b19aa09f9f7d9e69ca085db56c537967fc'


def need(ok, message):
    if not ok:
        raise ValueError(message)


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def digest(data):
    return hashlib.sha256(data).hexdigest()


def files(root):
    return {p.relative_to(root).as_posix():p for p in root.rglob('*') if p.is_file()
            and '__pycache__' not in p.parts and p.suffix != '.pyc'}


def validate_structure(root):
    ident=read(root/'control/PLAN_IDENTITY.json')
    need(ident['application_baseline_commit']==BASELINE,'Unexpected installed-source baseline')
    need(ident['status']=='DESIGNED_APP_IMPLEMENTATION_NOT_RUN','Plan cannot sign application status')
    work=read(root/'WORK_ORDERS.json')
    batches=work['batches']
    need([b['id'] for b in batches]==[f'B{x:02}' for x in range(1,13)],'Missing/duplicate/out-of-order batch')
    ids=[i['id'] for b in batches for i in b['issues']]
    need(len(ids)==len(set(ids))==work['issue_count'],'Issue identities/count mismatch')
    need(work['source_baseline']==BASELINE,'Wrong work-order source baseline')
    known=set();allcases={};allpaths=set()
    for b in batches:
        need(set(b.get('depends_on',[])).issubset(known),'Future/missing batch prerequisite '+b['id'])
        need((root/f'batches/{b["id"]}.md').is_file(),'Missing batch document')
        known.add(b['id'])
        for i in b['issues']:
            need(i['application_status']=='TODO','Unexecuted plan falsely signs issue '+i['id'])
            for k in ('evidence_refs','diagnosis','files','implementation_steps','acceptance','rollback','forbidden'):
                need(bool(i.get(k)),f'Empty implementation contract {i["id"]}:{k}')
            for f in i['files']:
                path=pathlib.PurePosixPath(f['path'])
                need(not path.is_absolute() and '..' not in path.parts,'Unsafe source path')
                need(f.get('changes'),'File contract lacks changes')
                allpaths.add(f['path'])
            for a in i['acceptance']:
                need(a['id'] not in allcases,'Duplicate acceptance id '+a['id'])
                need(a.get('level') in {'ENGINEERING','DEVICE','NATURAL'},'Missing acceptance level')
                for k in ('scenario','expected','evidence'):
                    need(a.get(k),'Empty acceptance field '+a['id'])
                allcases[a['id']]={**a,'issue_id':i['id'],'batch':b['id']}
    matrix=read(root/'ACCEPTANCE_MATRIX.json')['cases']
    need(len(matrix)==len(allcases) and {x['id'] for x in matrix}==set(allcases),'Acceptance matrix coverage differs')
    for a in matrix:
        need(all(a[k]==v for k,v in allcases[a['id']].items()),'Matrix changed case contract '+a['id'])
        need(a.get('test_contract'),'Missing execution contract '+a['id'])
        if a['level']=='ENGINEERING':
            need(a['test_contract']['path'] in allpaths,'Test harness missing from file ownership')
    cards=read(root/'FILE_CARDS.json')
    cf={x['production_path']:x for x in cards['files']}
    need(len(cf)==len(cards['files'])==cards['file_count'],'Duplicate file card/count mismatch')
    need(allpaths.issubset(cf),'Work-order path missing from file cards')
    for p,c in cf.items():
        need(set(c['issue_ids']).issubset(ids),'File references missing issue '+p)
        need(c['ordered_batches']==sorted(set(c['ordered_batches'])),'Ambiguous shared-file edit order')
        need(c['ordered_batches'] and c['primary_owner_batch']==c['ordered_batches'][0],'Missing primary file owner')
        need(c['before_state'] in {'EXISTING_122','NEW_REQUIRED','REPLACED_OLD_PLAN_TOOL'},'Unknown baseline disposition')
        if c['before_state']=='EXISTING_122':
            need(re.fullmatch('[0-9a-f]{40}',c['before_git_blob_sha'] or ''),'Missing baseline blob')
    old=read(root/'inherited/R34_WORK_ORDERS.json')['issues']
    cross=read(root/'R34_CROSSWALK.json')['requirements']
    need(len(old)==len(cross)==66,'Old R34 count changed')
    need({i['id'] for i in old}=={i['legacy_id'] for i in cross},'Old R34 requirement omitted')
    for x in cross:
        need(x['current_issue_ids'] and set(x['current_issue_ids']).issubset(ids),'Unassigned old R34 issue')
        need(x['status']=='REVERIFY_ON_122_BASELINE_NO_OLD_PASS_REUSED','Old pass silently reused')
    oc=read(root/'inherited/R34_FILE_CARDS.json')['files']
    need(len(oc)==152 and {x['production_path'] for x in oc}.issubset(cf),'Old 152-file scope dropped')
    old335=read(root/'inherited/R34_R335_CROSSWALK.json')['requirements']
    chain=read(root/'R335_CROSSWALK.json')['requirements']
    need(len(chain)==66 and {x['legacy_id'] for x in old335}=={x['legacy_id'] for x in chain},'R33.5 requirement dropped')
    for x in chain:
        need(x['current_issue_ids'] and set(x['current_issue_ids']).issubset(ids),'R33.5 requirement not assigned')
    anchors=read(root/'evidence/SOURCE_ANCHORS.json')['anchors']
    need(not [x for x in anchors if x['status']=='UNVERIFIED_METHOD'],'Unverified existing method anchor remains')
    need(set(x['issue_id'] for x in anchors).issubset(ids),'Anchor refers to missing issue')
    baseline=read(root/'evidence/SOURCE_MANIFEST.json')
    need(baseline['source_commit']==BASELINE,'Source manifest identity differs')
    ledger=read(root/'control/EXECUTION_LEDGER.json')
    need(all(b['engineering']=='TODO' and b['build']=='NOT_RUN' and b['device']=='NOT_RUN'
             and b['natural']=='NOT_OBSERVED' for b in ledger['batches']),'Initial ledger makes unexecuted claims')
    need({x['batch'] for x in ledger['batches']}==known,'Initial ledger incomplete')
    for path,p in files(root).items():
        if p.suffix=='.py':
            compile(p.read_text(encoding='utf-8'),path,'exec')
        elif p.suffix=='.json' and path not in EXCLUDED:
            read(p)
    objects=root/'specs/RESEARCH_OBJECT_CONTRACTS.json'
    need(objects.is_file(),'Missing 165-object appendix')
    objectdata=read(objects)
    rows=objectdata['objects']
    need(len(rows)==objectdata['total_index_objects']==165,'Research object index is incomplete')
    need(len({x['key'] for x in rows})==165,'Duplicate research object key')
    need(dict(collections.Counter(x['group'] for x in rows))==objectdata['counts'],
         'Research object group counts differ')
    for row in rows:
        need(row['work_orders'] and set(row['work_orders']).issubset(ids),
             'Research object has no valid work-order owner '+row['key'])
    from review_contracts import verify as verify_review_contracts
    review_counts = verify_review_contracts(root)
    from dispatch5 import check_construction_order
    review_counts.update(check_construction_order(root))
    updates = [r for r in read(root / 'control/REVIEW_CONTRACTS.json')['checks'] if r['id'].startswith('F5-')]
    field5 = dict(field5_work_items=len(updates), field5_affected_cases=len({r['case'] for r in updates}),
                  field5_affected_issues=len({r['issue'] for r in updates}),
                  field5_android_argv_count=review_counts['android_scoped_cases'])
    scope = read(root / 'control/SCOPE_COUNTS.json')
    need(all(scope[k] == v for k,v in field5.items()), 'Current receipt and published v5 scope differ')
    need(ident['document_revision'] == 5 and ident['reviewed_plan_commit'] ==
         read(root/'control/REVIEW_CONTRACTS.json')['field5_reviewed_plan_commit'], 'Wrong v5 parent identity')
    need(allcases['A-B12-03b'].get('joined_lineage_required') is True and
         allcases['A-B12-03b'].get('continuity_index_required') is True, 'Incomplete integrated service contract')
    review_counts.update(field5)
    return {**review_counts, 'batches':len(batches),'issues':len(ids),'files':len(cf),'acceptance_cases':len(allcases),
            'acceptance_by_level':dict(collections.Counter(x['level'] for x in allcases.values())),
            'r34_requirements':len(cross),'r34_files':len(oc),'r335_requirements':len(chain),
            'source_manifest_entries':len(baseline['files']),'method_contracts':len(anchors)}


def seal(root):
    listed=[{'path':p,'bytes':f.stat().st_size,'sha256':digest(f.read_bytes())}
            for p,f in sorted(files(root).items()) if p not in EXCLUDED]
    manifest={'schema':'STARCORE_R341_PLAN_MANIFEST_1','source_baseline':BASELINE,
              'exclusions':sorted(EXCLUDED),'excluded_transient_patterns':['**/__pycache__/**','**/*.pyc'],
              'files':listed}
    m=json.dumps(manifest,ensure_ascii=False,indent=2)+'\n'
    (root/'MANIFEST_SHA256.json').write_text(m,encoding='utf-8')
    s={'schema':'STARCORE_R341_PLAN_SEAL_1','manifest_file':'MANIFEST_SHA256.json',
       'manifest_sha256':digest(m.encode()),'source_baseline':BASELINE,
       'artifact_status':'DESIGNED_APP_IMPLEMENTATION_NOT_RUN','member_count':len(listed),
       'hash_boundary':'manifest excludes itself, seal and self-check; self-check is informational and reproducible'}
    (root/'control/PLAN_SEAL.json').write_text(json.dumps(s,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


def verify_seal(root):
    mpath=root/'MANIFEST_SHA256.json'
    manifest=read(mpath);s=read(root/'control/PLAN_SEAL.json')
    need(digest(mpath.read_bytes())==s['manifest_sha256'],'Manifest digest does not match seal')
    need(set(manifest['exclusions'])==EXCLUDED,'Unexpected unsealed source exclusions')
    need(manifest['source_baseline']==s['source_baseline']==BASELINE,'Seal/source mismatch')
    listed={x['path']:x for x in manifest['files']}
    actual={p:f for p,f in files(root).items() if p not in EXCLUDED}
    need(len(listed)==len(manifest['files'])==s['member_count'],'Duplicate manifest member')
    need(set(listed)==set(actual),'Missing or unlisted plan member')
    for path,row in listed.items():
        data=actual[path].read_bytes()
        need(len(data)==row['bytes'] and digest(data)==row['sha256'],'Changed sealed member '+path)
    return s


def verify_source(root, repo):
    manifest=read(root/'evidence/SOURCE_MANIFEST.json')
    count=0
    for row in manifest['files']:
        data=subprocess.run(['git','-C',str(repo),'show',BASELINE+':'+row['path']],check=True,
                            stdout=subprocess.PIPE,stderr=subprocess.PIPE).stdout
        blob=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
        need(blob==row['git_blob_sha'],'Baseline source bytes differ '+row['path'])
        if row['sha256']:
            need(digest(data)==row['sha256'],'Baseline SHA256 differs '+row['path'])
        count+=1
    return count


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--plan',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    p.add_argument('--seal',action='store_true',help='Explicit draft reseal; invalidates old plan-bound receipts')
    p.add_argument('--source-root',type=pathlib.Path,help='Optional exact original Git blob recheck')
    p.add_argument('--output',type=pathlib.Path,help='Write result outside sealed members, normally SELF_CHECK.json')
    a=p.parse_args()
    try:
        stats=validate_structure(a.plan)
        if a.seal:
            seal(a.plan)
        s=verify_seal(a.plan)
        source_count=verify_source(a.plan,a.source_root) if a.source_root else None
        result={'status':'PASS_PLAN_STRUCTURE_AND_SEAL','checks':stats,'manifest_sha256':s['manifest_sha256'],
                'git_source_recheck_count':source_count,
                'application_implementation':'NOT_RUN','android_build':'NOT_RUN',
                'device_24_72h':'NOT_RUN','natural_quality':'NOT_OBSERVED',
                'boundary':'Design integrity and host tool syntax only; application execution requires its own receipts.'}
        if a.output:
            target=a.output.resolve();r=a.plan.resolve()
            need(not target.is_relative_to(r) or target.relative_to(r).as_posix()=='SELF_CHECK.json',
                 'Output cannot overwrite a sealed plan member')
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print(json.dumps(result,ensure_ascii=False))
    except (ValueError,KeyError,OSError,subprocess.CalledProcessError) as e:
        raise SystemExit('FAIL_PLAN: '+str(e))


if __name__=='__main__':
    main()
