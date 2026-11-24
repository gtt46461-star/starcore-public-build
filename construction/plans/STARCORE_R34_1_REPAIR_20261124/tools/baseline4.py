"""Rebuild the source-scoped asset inventory from the second, immutable 122 report."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path


# These member hashes are also itemized by evidence/FIELD3_IDENTITY.json.
MEMBERS = {
    'R26_REPORT.json': '270b7c925a4c20b077b6caffba93febbfdc02703dd11adc350c31c1a36313aeb',
    'R30_REPORT.json': '50a39a79dcee9e10a85b3d50994a4253f77c4912bcf88bc48749d8720e0a9100',
    'DIAGNOSTIC_SCOPE.json': '95962e0dfbfac61d194c51f7dc13b9b1d0dc5dacd9e44bc03682a69f33490b67',
    'R31_SEMANTIC_RECEIPT.json': 'e30f41c232f8fc3fd9dcd415d2fd24e66cddbe356b34e4c0c107bd9aed8ce832',
}


def check(condition, message):
    if not condition:
        raise ValueError(message)


def extract(root):
    docs = {}
    for name, digest in MEMBERS.items():
        raw = (root / name).read_bytes()
        check(hashlib.sha256(raw).hexdigest() == digest, 'foreign or altered report member: ' + name)
        docs[name] = json.loads(raw)
    r26, r30 = docs['R26_REPORT.json'], docs['R30_REPORT.json']
    scope, identity = docs['DIAGNOSTIC_SCOPE.json'], docs['R31_SEMANTIC_RECEIPT.json']
    w = r30['snapshotWatermark']
    check(w['reportJobId'] == identity['jobId'] and r26['snapshotHash'] == identity['snapshotSha256'] == scope['snapshotSha256']
          and r26['sourceCommit'] == w['sourceCommit'] == identity['sourceCommit'], 'incompatible report scopes')
    sections = {s['sectionId']: i for i,s in enumerate(r26['sections'])}
    rows = []
    def add(key, value, unit, role, source):
        rows.append(dict(key=key, value=value, unit=unit, role=role, source=source))
    def section(sid, suffix):
        return 'R26_REPORT.json#/sections/' + str(sections[sid]) + '/' + suffix
    ev = r26['sections'][sections['EVOLUTION']]
    check(len(ev['metrics']) == 1 and ev['asOfIssue'] == w['latestIssue'], 'wrong evolution extract')
    for k,v in ev['metrics'][0].items():
        add(k,v,'ledger count','R26_RETAINED_EVOLUTION',section('EVOLUTION','metrics/0/'+k))
    label = r26['sections'][sections['TRAINING']]['metricResults']['labelsConsumed']['value']
    add('retainedKernelUpdates',label['retainedKernelUpdates'],'retained update events','KERNELS_WITH_MEASURED_UPDATES',
        section('TRAINING','metricResults/labelsConsumed/value/retainedKernelUpdates'))
    for k in ['r30_frozen_confirm','r30_confirm_read','r27_generation','r27_policy','r27_live_frozen','r27_certificate','r31_mainline_freeze','trades']:
        t = w['tables'][k]
        check(t['available'] is True and type(t['rows']) is int,'unavailable table '+k)
        add(k,t['rows'],'retained rows','TABLE_AT_FACT_WATERMARK', 'R30_REPORT.json#/snapshotWatermark/tables/'+k+'/rows')
    for k,v in r30['chapters'][0]['commonMetrics'].items():
        add(k,v,'report-defined count','R30_COMMON_METRICS','R30_REPORT.json#/chapters/0/commonMetrics/'+k)
    for i,m in enumerate(r30['metricResults']):
        if m['id'] in ('Q01','Q02','Q06','Q07'):
            add(m['id'],{k:m[k] for k in ('status','value','numerator','denominator')},'quality metric',
                'SOURCE_QUALITY_STATUS','R30_REPORT.json#/metricResults/'+str(i))
    for k,field in [('researchConsumptionR31','validatedActionReceipts'),('hypothesisCreditR31','supportedHypotheses')]:
        entry=r30['lifecycleEvidenceR31'][k]
        add(field,entry['value'][field],'development receipts or hypotheses',entry['sourceScope'],
            'R30_REPORT.json#/lifecycleEvidenceR31/'+k+'/value/'+field)
    by_table = {t['table']:i for i,t in enumerate(scope['recentEvidence'])}
    def data(name):
        i=by_table[name]
        t=scope['recentEvidence'][i]
        check(t['complete'] is True and t['scope']=='ALL_ROWS_FULL_METADATA_TEXT' and
              t['totalRows']==t['exportedRows']==len(t['rows']), 'incomplete '+name)
        return t['rows'], 'DIAGNOSTIC_SCOPE.json#/recentEvidence/'+str(i)+'/rows'
    cand, cand_path=data('r952_candidate')
    predictions, pred_path=data('r952_candidate_predraw')
    funnel, funnel_path=data('r952_candidate_funnel')
    lifecycle, life_path=data('research_lifecycle_r932')
    cursors, cursor_path=data('r952_learning_cursor')
    assets=[]
    for idx,c in enumerate(cand):
        cid=c['candidate_id']
        pr=[(i,r) for i,r in enumerate(predictions) if r['candidate_id']==cid]
        check(len({r['target_issue'] for _,r in pr})==len(pr),'duplicate target')
        rlife=[(i,r) for i,r in enumerate(lifecycle) if r['candidate_key']=='r952:'+cid and r['structural_hash']==c['structural_hash']]
        rcursors=[(i,r) for i,r in enumerate(cursors) if r['consumer_id']=='R952:'+cid]
        check(len(rlife)==len(rcursors)==1, 'ambiguous lifecycle or cursor')
        li,l=rlife[0]
        ci,u=rcursors[0]
        outcomes=[]
        for i,r in pr:
            check(r['structure_hash']==c['structural_hash'] and r['evidence_role']=='TRAIN_STREAM_V2'
                  and r['settled']=='1' and r['hit'] in ('0','1') and r['settlement_hash'], 'scope or settlement mismatch')
            outcomes.append(dict(target=r['target_issue'],hit=int(r['hit']),role=r['evidence_role'],source=pred_path+'/'+str(i)))
        stages = Counter(r['stage'] for r in funnel if r['candidate_id']==cid)
        assets.append(dict(identity={k:c[k] for k in ('candidate_id','created_issue','ast','structural_hash','semantic_hash','parent_id','state','future_n','may_create_real')},
                           candidate_source=cand_path+'/'+str(idx), lifecycle_source=life_path+'/'+str(li),
                           lifecycle={k:l[k] for k in ('state','reason','future_n','paired_n','last_issue')},
                           last_training=u['trained_until_issue'], cursor_source=cursor_path+'/'+str(ci),
                           stage_counts=dict(sorted(stages.items())), stage_source=funnel_path,
                           evidence_role='TRAIN_STREAM_V2', settled_targets=outcomes,
                           train_hits=sum(x['hit'] for x in outcomes), train_settled=len(outcomes)))
    other=[{'key':r['candidate_key'], 'state':r['state'], 'future_n':r['future_n'], 'paired_n':r['paired_n'], 'source':life_path+'/'+str(i)}
           for i,r in enumerate(lifecycle) if r['candidate_key'] not in {'r952:'+c['candidate_id'] for c in cand}]
    return dict(schema='R341_FIELD_ASSET_INVENTORY_4',job=identity['jobId'],snapshot=identity['snapshotSha256'],
                source_commit=w['sourceCommit'],through=w['latestIssue'],source_sha256=MEMBERS,metrics=rows,
                r952_candidates=assets,other_lifecycle_rows=other,
                across_modules='NO_COMMON_CANDIDATE_COHORT_OR_CONVERSION_RATE_ESTABLISHED')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report-dir',required=True,type=Path)
    dest=parser.add_mutually_exclusive_group(required=True)
    dest.add_argument('--output',type=Path)
    dest.add_argument('--check',type=Path)
    args=parser.parse_args()
    try:
        result=extract(args.report_dir)
        if args.output:
            args.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
        else:
            check(result == json.loads(args.check.read_bytes()), 'asset inventory differs')
            print('PASS_FIELD_ASSET_INVENTORY')
    except (ValueError, OSError, KeyError, TypeError) as err:
        parser.exit(1, 'FAIL_FIELD_ASSET_INVENTORY: '+str(err)+'\n')


if __name__=='__main__':
    main()
