#!/usr/bin/env python3
"""Bind existing source-review findings to all original cards without inventing passes."""
from pathlib import Path
import collections, hashlib, json, shutil, subprocess

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112'
def read(p): return json.loads(p.read_text())
def dump(p,x): p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
base=read(OUT/'HANDOFF_BASE.json'); source=base['source_checkpoint']
original=ROOT/'construction/STARCORE_R31_FINAL_20261104/ledgers'
cards=read(original/'cards.json')+read(original/'repair_cards.json')
items=read(original/'acceptance_items.json')
historical={r['id']:r for r in read(ROOT/'construction/r31q/021/SOURCE_ACCEPTANCE_AUDIT.json')['cards']}
repairs=read(OUT/'REPAIR_CARDS_20.json')
findings=[]
for name in ['migration_plan_audit_rows.json','report_plan_audit_rows.json','research_plan_audit_rows.json',
             'runtime_completion_audit_rows.json','lifecycle_completion_audit_rows.json','root_completion_audit_rows.json']:
    p=ROOT.parent/name
    if not p.exists():raise SystemExit('Missing actual reviewer findings: '+str(p))
    value=read(p)
    rows=value if isinstance(value,list) else value.get('rows',value.get('cards',value.get('audit_rows',[])))
    if not rows:raise SystemExit('No rows: '+str(p))
    for r in rows:r['review_file']=name
    findings+=rows
    shutil.copyfile(p,OUT/name)

def ids(r):
    v=r.get('card_ids',r.get('card_id',r.get('id',[])))
    if isinstance(v,str):v=[v]
    return ['R31-'+i if not i.startswith('R31-') else i for i in v]
known={c['id'] for c in cards}
unknown=sorted({v for r in findings for v in ids(r)}-known)
if unknown:raise SystemExit('Unknown reviewer card IDs: '+str(unknown))
rows=[]
source_paths=set()
for c in cards:
    related=[r for r in findings if c['id'] in ids(r)]
    reviewed=[r for r in related if r.get('source_evidence') and 'NOT_AUDITED' not in r.get('current_source_status','')]
    h=historical.get(c['id'],{})
    ufs=[r['id'] for r in repairs if c['id'] in r['original_cards']]
    paths=[]
    for entry in c['files']:
        f=entry['path'] if isinstance(entry,dict) else entry
        p=ROOT/f
        if not p.exists():raise SystemExit('Missing original card file '+f)
        data=p.read_bytes()
        actual=subprocess.check_output(['git','show',source+':'+f],cwd=ROOT)
        if data!=actual:raise SystemExit('Reviewed file differs from checkpoint '+f)
        paths.append({'path':f,'sha256':hashlib.sha256(data).hexdigest(),'matches_checkpoint':True})
        source_paths.add(f)
    row={'card_id':c['id'],'title':c['title'],'batch':c.get('construction_batch',c['batch']),
         'historical_receipt_source':h.get('historicalReceiptSource'),
         'historical_local_items':h.get('localReceiptItems',{}),
         'current_status':'SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION' if reviewed else 'NOT_FULLY_AUDITED_NOT_CLOSED',
         'has_particular_source_review':bool(reviewed),
         'currently_accepted_complete':False,
         'source_files':paths,
         'current_findings':related,
         'repair_cards':ufs,
         'original_acceptance_levels':dict(collections.Counter(i['level'] for i in items if i['card']==c['id'])),
         'current_full_card_execution':'NOT_COMPLETED_NO_CURRENT_EXACT_SOURCE_FULL_CLOSURE',
         'required_next_action':('按本卡事实缺口及'+(','.join(ufs) or '原卡要求')+'修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。' if reviewed else
            '继续逐项审查本卡真实入口、生产者、持久产物、消费者、反馈/撤销及报告字段，并执行当前源码原验收；未深入核实不能默认完成。')}
    rows.append(row)

lock=read(ROOT/'verification/r31q/021/LOCK_REGRESSION.json')
for p,h in lock['sources'].items():
    if hashlib.sha256((ROOT/p).read_bytes()).hexdigest()!=h:raise SystemExit('Stale host evidence '+p)
receipts=[]
for p in sorted((ROOT/'construction/r31/batches').glob('*/RECEIPT.json')):
    x=read(p);receipts.append({'batch':x['batch'],'build_source_head':x.get('build_source_head'),
         'closed_commit':x.get('closed_commit'),'closed_at':x.get('closed_at'),'unit':x.get('unit'),
         'path':str(p.relative_to(ROOT)),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
result={'format':'R31_UNIFIED_CURRENT_SOURCE_STATUS_V1','source_checkpoint':source,
    'application_branch':base['source_branch'],'installed_apk_source':base['installed_apk_source'],
    'scope':'58-card coverage index with focused source review; not an exhaustive 1278-file semantic audit or current Android acceptance',
    'source_paths_checked_against_checkpoint':len(source_paths),
    'original_cards':56,'original_engineering_cards':2,
    'cards_with_specific_source_findings':sum(r['has_particular_source_review'] for r in rows),
    'cards_requiring_additional_source_review':sum(not r['has_particular_source_review'] for r in rows),
    'currently_accepted_complete_cards':0,
    'meaning_of_zero':'No card has a new full exact-source end-to-end closure in this handoff; it does not mean zero useful implementation exists.',
    'current_host_evidence':lock,'historical_receipts':receipts,'rows':rows}
dump(OUT/'CURRENT_SOURCE_STATUS_58.json',result)
text=['核对源码：`'+source+'`；分支：`'+base['source_branch']+'`。原机APK源码另列，不能把源码原型当已安装修复。',
      '\n原卡路径均与上述提交字节核对一致。以下58行是覆盖索引；有事实审阅的卡仍需按验收关门，其他卡明确列待深入核实。当前没有新完整关门的卡，不代表源码毫无实现。',
      '\n历史K11记录622项单测、0失败/错误/跳过，绑定历史源码`63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84`。当前仅保留带文件hash的OS锁HOST五场景PASS；它不证明完整报告、Android、原机或自然质量。',
      '\n| 原卡 | 批次 | 当前核验状态 | 返修去向 |\n|---|---|---|---|']
for r in rows:
    text.append('| '+r['card_id']+' '+r['title']+' | '+r['batch']+' | '+('已审具体接点，须返修/复验' if r['has_particular_source_review'] else '尚未完整核实，不得默认完成')+' | '+(', '.join(r['repair_cards']) or '原卡继续核实实施')+' |')
def lines(value):
    if value is None:return []
    if isinstance(value,list):return [s for item in value for s in lines(item)]
    if isinstance(value,dict):return [value.get('detail',value.get('fact',value.get('source_fact',json.dumps(value,ensure_ascii=False))))]
    return [str(value)]
for r in rows:
    text+=['\n### '+r['card_id']+' '+r['title'],
           '\n历史声明：'+str(r['historical_receipt_source'] or '未取得单独历史来源')+'；原验收分层：'+json.dumps(r['original_acceptance_levels'],ensure_ascii=False)+'。',
           '\n当前结论：'+r['current_status']+'；当前整卡执行：NOT_COMPLETED。',
           '\n实际源码：'+', '.join('`'+x['path']+'`' for x in r['source_files'])+'。']
    if r['has_particular_source_review']:
        for i,f in enumerate(r['current_findings'],1):
            if not f.get('source_evidence'):continue
            text+=['\n事实记录 '+str(i)+'（'+f['review_file']+'，保留完整机器证据）：']
            for v in lines(f.get('actual_implemented',f.get('actual_implemented_behavior'))):text+=['\n- 已实现：'+v]
            for v in lines(f.get('unclosed_gap',f.get('concrete_unclosed_gap'))):text+=['\n- 未闭环/待验证：'+v]
            for e in f['source_evidence']:
                if isinstance(e,dict):
                    path=e.get('file',e.get('path',''))
                    path=str(path).replace(str(ROOT)+'/', '')
                    method=e.get('method',', '.join(e.get('methods',[])))
                    loc='L'+str(e['line']) if 'line' in e else ('L'+','.join(map(str,e.get('lines',[])[:4])) if e.get('lines') else '')
                    text+=['\n- 源码依据：`'+path+'` '+str(method)+' '+loc+'。']
                else:text+=['\n- 源码依据：'+str(e)]
    else:text+=['\n尚未完成本卡全部实际调用、行为与验收核查。文件存在与历史LOCAL收据只作为线索；禁止把它们写成当前运行通过。']
    text+=['\n如何闭环：'+r['required_next_action']]
(OUT/'CURRENT_SOURCE_STATUS_58.md').write_text('\n'.join(text)+'\n')
print(json.dumps({k:v for k,v in result.items() if k not in ['rows','historical_receipts','current_host_evidence']},ensure_ascii=False))
