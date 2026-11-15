#!/usr/bin/env python3
"""Verify package bytes, issue ownership, original text retention and complete text payload cards.
This is a package coherence check, not business, Android or natural quality acceptance.
"""
from pathlib import Path, PurePosixPath
import argparse, collections, hashlib, importlib.util, json, re, sys, zipfile

def sha(data): return hashlib.sha256(data).hexdigest()
def readj(p): return json.loads(p.read_text())
def validate_acceptance_migration(acceptance, files, cards, original):
    """A moved file cannot be accepted before its actual delivery batch.

    Original requirement text/identity and its original scheduled batch remain
    immutable. A reschedule changes current ownership only, with an explicit
    reason; file presence or a historical PASS is never clause sign-off.
    """
    card_by_id={c['id']:c for c in cards}
    original_by_id={row['id']:row for row in original}
    if len(card_by_id)!=len(cards) or len(original_by_id)!=len(original):
        raise ValueError('ACCEPTANCE_DUPLICATE_CARD_OR_ORIGINAL')
    allowed={'P1','P2','P3','P4','P5'}
    if len({row['id'] for row in acceptance})!=len(acceptance) or set(original_by_id)!={row['id'] for row in acceptance}:
        raise ValueError('ACCEPTANCE_ORIGINAL_ID_SET_CHANGED')
    moved=[]
    for row in acceptance:
        identity=row['id'];old=original_by_id[identity]
        for key in ('oldRun','textSha256','observable'):
            if row.get(key)!=old.get(key):
                raise ValueError('ACCEPTANCE_ORIGINAL_CHANGED '+identity+' '+key)
        run_at=row.get('runAt')
        if run_at not in allowed or row.get('card') not in card_by_id:
            raise ValueError('ACCEPTANCE_RUN_OR_CARD_UNKNOWN '+identity)
        card=card_by_id[row['card']]
        if card.get('batch')!=run_at:
            raise ValueError('ACCEPTANCE_CARD_BATCH_DIFFERS_FROM_RUN '+identity)
        if row.get('currentBatchContract')!='batches/'+run_at+'.md':
            raise ValueError('ACCEPTANCE_BATCH_DOCUMENT_DIFFERS_FROM_RUN '+identity)
        owner=files.get(row.get('ownerFile'),{}).get('owner')
        if owner is not None:
            if owner not in allowed or int(run_at[1:])<int(owner[1:]):
                raise ValueError('ACCEPTANCE_RUN_BEFORE_SOURCE_OWNER '+identity)
            if row['ownerFile'] not in card.get('files',[]):
                raise ValueError('ACCEPTANCE_OWNER_FILE_NOT_IN_CURRENT_CARD '+identity)
        if (run_at,row['card'],row.get('ownerFile'))!=(old.get('runAt'),old.get('card'),old.get('ownerFile')):
            reason=row.get('migration_reason')
            if not isinstance(reason,str) or not reason.strip():
                raise ValueError('ACCEPTANCE_MIGRATION_REASON_MISSING '+identity)
            moved.append(identity)
    return {'checked':len(acceptance),'rescheduled':len(moved),'rescheduled_ids':moved,'source_owner_inversions':0}

def validate_file_card_coverage(files, cards):
    bindings = collections.defaultdict(set)
    for card in cards:
        if len(card['files']) != len(set(card['files'])):
            raise ValueError('DUPLICATE_FILE_WITHIN_ISSUE '+card['id'])
        for path in card['files']:
            bindings[path].add(card['id'])
    missing, extra = sorted(set(files)-set(bindings)), sorted(set(bindings)-set(files))
    if missing or extra:
        raise ValueError('FILE_CARD_COVERAGE missing='+','.join(missing)+' extra='+','.join(extra))
    for path, entry in files.items():
        if 'cards' in entry and set(entry['cards']) != bindings[path]:
            raise ValueError('FILE_CARD_MANIFEST_BINDING_MISMATCH '+path)
    return {'owned_paths':len(files), 'covered_paths':len(bindings),
            'issue_file_relations':sum(len(c['files']) for c in cards),
            'uncovered_paths':0, 'manifest_card_bindings_match':True}

def check(root, sealed=False):
    root=root.resolve();m=readj(root/'code_manifest.json');c=readj(root/'contracts/COUNTS.json')
    spec=importlib.util.spec_from_file_location('package_apply',root/'tools/apply_batch.py');a=importlib.util.module_from_spec(spec);spec.loader.exec_module(a);a.validate_manifest(m)
    files={};counts=collections.Counter()
    for bid,b in m['batches'].items():
        for row in b['files']:
            assert row['path'] not in files, 'duplicate owner'
            files[row['path']]=row;counts[row['action']]+=1
            p=a.safe(root,row['source']);assert p.is_file() and sha(p.read_bytes())==row['after_sha256'], 'payload mismatch '+row['path']
            if row['action']=='PRESERVE_REFERENCE':assert row['before_sha256']==row['after_sha256']
            elif row['action']=='ADD':assert row['before_sha256'] is None
            else:assert row['before_sha256']!=row['after_sha256']
    cards=readj(root/'contracts/repair_cards.json');assert len(cards)==18 and len(m['batches'])==5
    coverage=validate_file_card_coverage(files,cards)
    filecards=0;binarycards=0
    for card in cards:
        for p in card['files']:
            row=files[p];assert int(row['owner'][1:])<=int(card['batch'][1:]), 'later owner in early issue '+p
            name=sha(p.encode())[:12]+'.md';text=(root/'issues'/card['id']/name).read_text()
            assert row['after_sha256'] in text and '../../'+row['source'] in text
            if p.endswith('.gz'):binarycards+=1
            else:assert (root/row['source']).read_text() in text,'complete text missing '+p
            filecards+=1
        for p in card['writes']:assert files[p]['owner']==card['batch'] and files[p]['action']!='PRESERVE_REFERENCE'
    assert c['owned_file_paths']==len(files) and c['actions']==dict(counts)
    assert c['issue_file_cards_with_full_code']==filecards
    trace=[json.loads(l) for l in (root/'contracts/CURRENT_REQUIREMENT_TRACE_1832.jsonl').read_text().splitlines()]
    assert len(trace)==1832 and len({x['id'] for x in trace})==1832
    for row in trace:
        assert sha(row['original_text'].encode())==row['original_text_sha256']
        if row.get('requires_runtime_entry'):assert row['bindings'], 'runtime clause lacks entry '+row['id']
        for b in row['bindings']:
            if b['path'] in files:
                x=files[b['path']];assert b['current_sha256']==x['after_sha256'] and b['current_owner']==x['owner'] and b['payload']==x['source']
    acceptance=readj(root/'contracts/CURRENT_ACCEPTANCE_222.json');assert len(acceptance)==222 and len({x['id'] for x in acceptance})==222
    original=readj(root/'reference/R32.1/contracts/acceptance_consumers.json')
    assert {x['id']:x['textSha256'] for x in acceptance}=={x['id']:x['textSha256'] for x in original}
    migration=validate_acceptance_migration(acceptance,files,cards,original)
    # Three distinct original-text layers must not be conflated: pre-split card,
    # canonical 222 clauses, and the current observation instruction.
    bridge=readj(root/'contracts/ORIGINAL_ACCEPTANCE_TEXTS_222.json')
    bridge_by={x['id']:x for x in bridge['items']}
    assert len(bridge_by)==222 and set(bridge_by)=={x['id'] for x in acceptance}
    sample=bridge['items'][0]
    with zipfile.ZipFile(root/'SOURCE_BASELINE.zip') as z:
        canonical_bytes=z.read(sample['canonical_source'])
        card_sources={}
        for item in bridge['items']:
            path=item['original_card_text_source']
            if path not in card_sources:card_sources[path]=z.read(path)
            assert sha(card_sources[path])==item['original_card_text_source_sha256'], 'original card archive hash mismatch '+path
    with zipfile.ZipFile(root/'reference/R31_CONTROL_ARCHIVES.zip') as z:
        r313_bytes=z.read(sample['r313_source'])
    assert sha(canonical_bytes)==sample['canonical_source_sha256']
    assert sha(r313_bytes)==sample['r313_source_sha256']
    canonical={x['item']:x for x in json.loads(canonical_bytes)}
    r313={x['item']:x for x in json.loads(r313_bytes)}
    pre_split={}
    for card_bytes in card_sources.values():
        for card in json.loads(card_bytes):
            for i,text in enumerate(card['acceptance']):
                key=card['id']+'#A'+str(i+1)
                assert key not in pre_split, 'duplicate pre-split acceptance '+key
                pre_split[key]=text
    assert len(pre_split)==200, 'expected 194 original plus 6 engineering acceptance clauses'
    for row in acceptance:
        b=bridge_by[row['id']];raw=canonical[row['id']]
        for key in ('original_text','original_text_sha256','original_item','original_card_text','original_card_text_sha256','level','blocking'):
            assert row[key]==b[key], 'original text bridge mismatch '+row['id']+' '+key
        assert row['original_text']==raw['text']==r313[row['id']]['text']
        assert row['original_item']==raw['original_item']
        assert row['original_card_text']==pre_split[row['original_item']]
        assert sha(row['original_text'].encode())==row['original_text_sha256']
        assert sha(row['original_card_text'].encode())==row['original_card_text_sha256']==row['textSha256']
        assert (row['level'],row['blocking'])==(raw['level'],raw['blocking'])
    for row in acceptance:
        assert (root/row['currentBatchContract']).is_file()
        if row['ownerFile'] in files:assert row['current_source_sha256']==files[row['ownerFile']]['after_sha256']
    # Every author-controlled navigation link must land in a real file. Historical
    # attached source excerpts may contain examples, so check only generated link forms.
    broken=[]
    for p in [root/'README.md',root/'START_HERE.html',*(root/'batches').glob('*.md'),*(root/'issues').rglob('README.md')]:
        s=p.read_text();links=re.findall(r'\]\(([^)]+)\)',s) if p.suffix=='.md' else re.findall(r'href="([^"]+)"',s)
        for href in links:
            if href.startswith(('http:','https:','#')):continue
            if not sealed and href=='MANIFEST_SHA256.json':continue
            target=(p.parent/href.split('#')[0]).resolve()
            if not target.exists():broken.append({'document':str(p.relative_to(root)),'target':href})
    assert not broken, 'broken current links '+json.dumps(broken,ensure_ascii=False)
    for name in ('report326_input.json','report425_input.json'):assert (root/'evidence/report_inputs'/name).is_file()
    obj=readj(root/'audit/report/OBJECT_ACTIONS_165_RECEIPT.json')
    # Receipt formats are retained verbatim; its manifest pin must be present.
    assert sha((root/'code_manifest.json').read_bytes()) in json.dumps(obj), 'object document manifest stale'
    if sealed:
        member_manifest=readj(root/'MANIFEST_SHA256.json');items=member_manifest['files'];seen=set()
        for row in items:
            p=a.safe(root,row['path']);assert row['path'] not in seen;seen.add(row['path']);data=p.read_bytes()
            assert len(data)==row['bytes'] and sha(data)==row['sha256'], 'sealed mismatch '+row['path']
        actual={p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.name!='MANIFEST_SHA256.json'}
        assert actual==seen, 'sealed members differ'
    return {'status':'PASS','scope':'PACKAGE_COHERENCE_NOT_BUSINESS_OR_ANDROID_OR_NATURAL_QUALITY',
            'manifest_sha256':sha((root/'code_manifest.json').read_bytes()),'batches':5,'joint_issues':18,
            'manual_payload_files':len(files),'actions':dict(counts),'issue_file_cards':filecards,'binary_data_cards':binarycards,
            'file_card_coverage':coverage,
            'original_requirements_preserved':1832,'original_acceptances_preserved':222,'canonical_acceptance_texts_checked_against_original_archives':222,'pre_split_card_texts_separately_preserved':True,'acceptance_migration':migration,'later_owner_dependencies':0,
            'broken_current_navigation_links':0,'sealed_member_check':sealed}

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--package',type=Path,required=True);p.add_argument('--sealed',action='store_true');args=p.parse_args()
    print(json.dumps(check(args.package,args.sealed),ensure_ascii=False,indent=2))
