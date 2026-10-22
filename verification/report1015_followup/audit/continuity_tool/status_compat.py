#!/usr/bin/env python3
"""Read-only historical continuity audit. Never calls init/apply/checkpoint/handoff.

This adapter does not authorize the current branch or bless current product bytes.
It validates the immutable event chain, preserves typed partial receipts, records
unknown schemas as NOT_CLOSED, and fingerprints the current source independently.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time

STATE = 'construction_state/optimized299'
REQUIRED_CHECKPOINT = {'stage', 'lane', 'status', 'source_sha256'}
LANES = {'engineering', 'runtime', 'natural'}
LANE_STATUSES = {'engineering': {'NOT_RUN','FAILED','VERIFIED'}, 'runtime': {'NOT_RUN','FAILED','VERIFIED'}, 'natural': {'NOT_PROVEN','FAILED','QUALIFIED'}}
KNOWN_AUDIT_ACTIONS = {'INIT', 'APPLIED', 'CHECKPOINT', 'CLOSE_ASSERTION', 'GENERATED_REGISTERED'}

def canonical(x):
    return json.dumps(x, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()

def digest(x):
    return hashlib.sha256(x).hexdigest()

def safe(root, name):
    p = (root / name).resolve()
    if not p.is_relative_to(root.resolve()):
        raise ValueError('PATH_OUTSIDE_REPOSITORY:' + name)
    return p

def file_hash(p):
    if not p.is_file():
        return None
    h = hashlib.sha256()
    with p.open('rb') as f:
        while b := f.read(1024 * 1024):
            h.update(b)
    return h.hexdigest()

def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *args], text=True).strip()

def verify_chain(raw):
    if not raw or not raw.endswith(b'\n'):
        raise ValueError('EVENT_LEDGER_EMPTY_OR_TRUNCATED')
    events, previous = [], '0' * 64
    for number, line in enumerate(raw.splitlines(), 1):
        event = json.loads(line)
        body = dict(event)
        claimed = body.pop('event_sha256', None)
        if event.get('previous_sha256') != previous or digest(canonical(body)) != claimed:
            raise ValueError('EVENT_HASH_CHAIN_INVALID:' + str(number))
        events.append(event)
        previous = claimed
    if events[0].get('action') != 'INIT':
        raise ValueError('EVENT_INIT_MISSING')
    return events

def replay_read_only(events):
    stages, assertions, partials, unknown = {}, [], [], []
    for number, event in enumerate(events, 1):
        action, payload = event.get('action'), event.get('payload', {})
        ref = {'event_number': number, 'event_sha256': event['event_sha256'], 'action': action}
        if not isinstance(payload, dict):
            unknown.append(dict(ref, reason='NON_OBJECT_PAYLOAD', effective='NOT_CLOSED'))
            continue
        if action == 'APPLIED':
            if not {'stage', 'source_sha256'}.issubset(payload):
                unknown.append(dict(ref, reason='APPLIED_SCHEMA_UNKNOWN', effective='NOT_CLOSED'))
                continue
            stages[payload['stage']] = {'application_recorded': 'APPLIED', 'application_source_sha256': payload['source_sha256'], 'engineering': 'NOT_RUN', 'runtime': 'NOT_RUN', 'natural': 'NOT_PROVEN'}
        elif action == 'CHECKPOINT':
            if REQUIRED_CHECKPOINT.issubset(payload) and isinstance(payload.get('lane'),str) and payload['lane'] in LANES and isinstance(payload.get('status'),str) and payload['status'] in LANE_STATUSES[payload['lane']] and isinstance(payload.get('stage'),str) and isinstance(payload.get('source_sha256'),str):
                stage = stages.setdefault(payload['stage'], {'application_recorded': 'UNKNOWN'})
                stage[payload['lane']] = payload['status']
                stage[payload['lane'] + '_source_sha256'] = payload['source_sha256']
                stage[payload['lane'] + '_event_number'] = number
            else:
                # Heterogeneous legacy receipts must remain scoped evidence only.
                partials.append(dict(ref, kind=payload.get('kind'), stage=payload.get('stage'), recorded_scopes={k:payload[k] for k in ('engineering','runtime','natural','overall_1_9','source_commit','production_commit') if k in payload}, missing_standard_keys=sorted(REQUIRED_CHECKPOINT-set(payload)), schema_known=False, effective='NOT_CLOSED'))
        elif action == 'CLOSE_ASSERTION':
            if {'assertion_id','source_sha256','receipt','receipt_sha256'}.issubset(payload):
                assertions.append(dict(ref, **payload))
            else:
                unknown.append(dict(ref, reason='ASSERTION_SCHEMA_UNKNOWN', effective='NOT_CLOSED'))
        elif action not in KNOWN_AUDIT_ACTIONS:
            unknown.append(dict(ref, reason='AMENDMENT_OR_EXTENSION_REQUIRES_ITS_OWN_VERIFIER', effective='NOT_CLOSED'))
    return stages, assertions, partials, unknown

def fingerprint(repo, manifest):
    raw = subprocess.check_output(['git','-C',str(repo),'ls-files','-z','--cached','--others','--exclude-standard'])
    names = {x.decode() for x in raw.split(b'\0') if x}
    names = {n for n in names if n != STATE and not n.startswith(STATE + '/')}
    names.update(r['path'] for r in manifest.get('baseline_files', []))
    names.update(o['path'] for s in manifest.get('stages', []) for o in s.get('operations', []))
    rows, changed = [], []
    for name in sorted(names):
        p = safe(repo, name)
        pre = p.stat() if p.is_file() else None
        h = file_hash(p)
        post = p.stat() if p.is_file() else None
        if (None if pre is None else (pre.st_size,pre.st_mtime_ns,pre.st_ino)) != (None if post is None else (post.st_size,post.st_mtime_ns,post.st_ino)):
            changed.append(name)
        rows.append({'path': name, 'sha256': h})
    excluded = {r['path'] for r in manifest.get('generated_outputs', [])}
    return rows, digest(canonical(rows)), digest(canonical([r for r in rows if r['path'] not in excluded])), changed

def audit(repo, remote_tree_path=None):
    repo = repo.resolve()
    if Path(git(repo, 'rev-parse','--show-toplevel')).resolve() != repo:
        raise ValueError('REPO_MUST_BE_GIT_ROOT')
    state = repo / STATE
    ep = state/'events.jsonl'
    raw_before = ep.read_bytes()
    events = verify_chain(raw_before)
    mp = state/'manifest.snapshot.json'
    manifest_raw = mp.read_bytes()
    manifest = json.loads(manifest_raw)
    initial = events[0]['payload']
    if digest(manifest_raw) != initial.get('manifest_sha256') or manifest.get('package_id') != initial.get('package_id'):
        raise ValueError('HISTORICAL_MANIFEST_IDENTITY_MISMATCH')
    head_before = git(repo,'rev-parse','HEAD')
    branch = git(repo,'branch','--show-current')
    rows, identity, build_identity, changed = fingerprint(repo,manifest)
    remote_tree = json.loads(remote_tree_path.read_bytes()) if remote_tree_path else None
    if remote_tree and (remote_tree.get('truncated') is not False or remote_tree.get('sha') != head_before):
        raise ValueError('REMOTE_TREE_INCOMPLETE_OR_HEAD_MISMATCH')
    remote_entries = {r['path']:r for r in remote_tree['tree'] if r.get('type') == 'blob'} if remote_tree else None
    def availability(name):
        present = safe(repo,name).is_file()
        if remote_entries is None:return 'LOCAL_PRESENT' if present else 'LOCAL_ABSENT_REMOTE_UNCHECKED'
        return ('LOCAL_PRESENT_REMOTE_PRESENT' if name in remote_entries else 'LOCAL_PRESENT_CURRENT_REMOTE_ABSENT') if present else ('NOT_MATERIALIZED_REMOTE_PRESENT' if name in remote_entries else 'CURRENT_REMOTE_ABSENT')
    remote_inventory=[]
    cached={r['path']:r['sha256'] for r in rows}
    if remote_entries is not None:
        for name, entry in sorted(remote_entries.items()):
            p=safe(repo,name)
            remote_inventory.append({'path':name,'remote_git_sha1':entry['sha'],'remote_bytes':entry.get('size'),'local_sha256':cached[name] if name in cached else file_hash(p),'availability':availability(name)})
    stages, assertions, partials, unknown = replay_read_only(events)
    for stage in stages.values():
        for lane in sorted(LANES):
            recorded = stage.get(lane,'NOT_RUN' if lane != 'natural' else 'NOT_PROVEN')
            if recorded in ('VERIFIED','QUALIFIED'):
                stage[lane+'_effective_status'] = 'CURRENT_IDENTITY_RECEIPT_REVERIFY_REQUIRED' if stage.get(lane+'_source_sha256') == identity else 'STALE_SOURCE_REVERIFY'
            else:
                stage[lane+'_effective_status'] = recorded
        stage['whole_acceptance'] = 'NOT_CLOSED'
    source_matches = []
    for a in assertions:
        receipt = safe(state,a['receipt'])
        source_matches.append({'assertion_id':a['assertion_id'],'receipt_path':a['receipt'],'expected_receipt_sha256':a['receipt_sha256'],'event_number':a['event_number'],'recorded_source':a['source_sha256'],'matches_current_source':a['source_sha256']==identity,'receipt_exists':receipt.is_file(),'receipt_sha256_matches':file_hash(receipt)==a['receipt_sha256'],'effective':'NOT_CLOSED_RECEIPT_SCOPE_REVIEW_REQUIRED'})
    receipt_artifacts=[]
    for a in assertions:
        receipt=safe(state,a['receipt'])
        if not receipt.is_file():continue
        for artifact in json.loads(receipt.read_bytes()).get('artifacts',[]):
            name=artifact['path'];actual=file_hash(safe(repo,name))
            receipt_artifacts.append({'assertion_event':a['event_number'],'path':name,'expected_sha256':artifact.get('sha256'),'actual_sha256':actual,'expected_hash_matches':actual is not None and actual==artifact.get('sha256'),'availability':availability(name),'effective':'NOT_CLOSED_HISTORICAL_ARTIFACT_ONLY'})
    pending = [{'path':str(p.relative_to(repo)),'sha256':file_hash(p)} for p in sorted(state.glob('*pending*.json')) if p.is_file()]
    handoff_path=state/'HANDOFF.json'
    handoff=json.loads(handoff_path.read_bytes()) if handoff_path.is_file() else {}
    baseline_observation=[{'path':r['path'],'original_baseline_sha256':r.get('sha256'),'current_sha256':next((x['sha256'] for x in rows if x['path']==r['path']),None)} for r in manifest.get('baseline_files',[])]
    unchanged=ep.read_bytes()==raw_before
    if not unchanged:
        raise ValueError('EVENT_LEDGER_CHANGED_DURING_AUDIT')
    return {'schema':'R79_READ_ONLY_CONTINUITY_STATUS_V1','scope':'AUDIT_ONLY_NO_INIT_APPLY_MIGRATION_OR_ACCEPTANCE','head':head_before,'head_after':git(repo,'rev-parse','HEAD'),'tree':git(repo,'rev-parse','HEAD^{tree}'),'branch':branch,'historical_init_branch':initial.get('branch'),'branch_matches_historical_init':branch==initial.get('branch'),'branch_mismatch_is_recorded_not_bypassed_for_mutation':True,'package_id':manifest['package_id'],'manifest_sha256':digest(manifest_raw),'event_count':len(events),'event_chain_verified':True,'event_file_sha256':digest(raw_before),'event_file_unchanged':unchanged,'last_event_sha256':events[-1]['event_sha256'],'historical_handoff_event_count':handoff.get('event_count'),'historical_handoff_stale':handoff.get('last_event_sha256')!=events[-1]['event_sha256'],'pending_recovery_files':pending,'remote_state_root_pending_files':[n for n in (remote_entries or {}) if str(Path(n).parent)==STATE and 'pending' in Path(n).name and n.endswith('.json')],'pending_recovery_observation':'NONE_AT_STATE_ROOT' if not pending else 'EXPLICIT_RECOVERY_REVIEW_REQUIRED_NO_MUTATION_PERFORMED','source_sha256_original_scope':identity,'build_input_sha256_original_scope':build_identity,'fingerprint_definition':'Original source_rows scope: tracked+untracked nonignored excluding optimized299 state; union manifest baseline and operation paths. Taken before output write; not an atomic filesystem snapshot.','fingerprinted_file_count':len(rows),'files_changed_during_individual_hash':changed,'source_fingerprints':rows,'baseline_fingerprints_observation':baseline_observation,'stage_progress':stages,'assertion_observations':source_matches,'receipt_artifact_observations':receipt_artifacts,'remote_tree_sha':remote_tree.get('sha') if remote_tree else None,'remote_tree_file_inventory':remote_inventory,'remote_tree_availability_counts':{v:sum(1 for r in remote_inventory if r['availability']==v) for v in sorted({r['availability'] for r in remote_inventory})},'partial_checkpoint_receipts':partials,'uninterpreted_extension_events':unknown,'next_unapplied_stage':next((s['id'] for s in manifest['stages'] if stages.get(s['id'],{}).get('application_recorded')!='APPLIED'),None),'next_unverified_engineering_stage':next((s['id'] for s in manifest['stages'] if stages.get(s['id'],{}).get('engineering_effective_status')!='VERIFIED'),None),'whole_1_9':'NOT_CLOSED','all_requirements_closed':False,'device_acceptance':'NOT_GRANTED_BY_THIS_AUDIT','natural_qualification':'NOT_GRANTED_BY_THIS_AUDIT'}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--repo',required=True,type=Path)
    parser.add_argument('--output',type=Path)
    parser.add_argument('--remote-tree',type=Path,help='Optional complete original Git tree response at current HEAD; classifies sparse omissions without restoring or mutating files.')
    args=parser.parse_args()
    try:
        result=audit(args.repo,args.remote_tree)
    except Exception as e:
        result={'status':'AUDIT_REFUSED','error_type':type(e).__name__,'error':str(e),'all_requirements_closed':False}
        print(json.dumps(result,ensure_ascii=False,indent=2));return 2
    encoded=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    if args.output:
        # Intentionally outside the product repository to avoid self-hashing output.
        output=args.output.resolve()
        if output.is_relative_to(args.repo.resolve()):
            raise ValueError('OUTPUT_MUST_BE_OUTSIDE_REPOSITORY')
        output.parent.mkdir(parents=True,exist_ok=True);output.write_text(encoded)
        print(json.dumps({'status':'READ_ONLY_AUDIT_WRITTEN','output':str(output),'event_count':result['event_count'],'fingerprinted_file_count':result['fingerprinted_file_count'],'all_requirements_closed':False},ensure_ascii=False))
    else:print(encoded,end='')
    return 0

if __name__=='__main__':raise SystemExit(main())
