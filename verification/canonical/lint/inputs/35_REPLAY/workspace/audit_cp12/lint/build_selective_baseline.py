"""Create an exact, audited residual baseline. Unknown finding => failure, no baseline.

Usage: python build_selective_baseline.py <final-project-root> <fresh-raw-lint.xml>
The subsequent acceptance lint must enable abortOnError=true,warningsAsErrors=true.
"""
from pathlib import Path
import sys,json,collections,os,xml.etree.ElementTree as ET
if len(sys.argv)!=3:raise SystemExit('usage: build_selective_baseline.py <final-project-root> <fresh-raw-lint.xml>')
root=Path(sys.argv[1]).resolve();report=Path(sys.argv[2]).resolve()
ledger=json.loads(Path('audit_cp12/lint/LINT_FINDING_MASTER_CP12.json').read_text())['rows']
incremental=Path('audit_cp12/lint/INCREMENTAL_LINT_DISPOSITIONS_CP12.json')
if incremental.exists():ledger+=json.loads(incremental.read_text())['rows']
approved=[r for r in ledger if r['baseline_approved']]
def norm(s):return ' '.join((s or '').split())
used=collections.Counter();entries=[];rejected=[];matches=[]
oldroot=Path('current_source/STARCORE_5.4.2.23_R980_SOURCE')
for finding in ET.parse(report).getroot().findall('issue'):
    # Lint's own informational clean-baseline notices are receipt metadata, not defects.
    if finding.get('id') in ('LintBaseline','LintBaselineFixed') and finding.get('severity')=='Information':continue
    location=finding.find('location')
    try:path=str(Path(location.get('file')).resolve().relative_to(root))
    except (ValueError,AttributeError):rejected.append({'id':finding.get('id'),'message':finding.get('message'),'reason':'LOCATION_OUTSIDE_APP'});continue
    candidates=[r for r in approved if r['detector']==finding.get('id') and r['message']==finding.get('message') and r['file']==path and used[r['issue_id']]==0]
    chosen=None
    for row in candidates:
        if finding.get('severity')!='Warning':break
        if row['detector']=='TooManyViews':
            n='{http://schemas.android.com/apk/res/android}';xml=ET.parse(root/path).getroot();old=ET.parse(oldroot/path).getroot()
            ids=lambda t:{e.get(n+'id') for e in t.iter() if e.get(n+'id')}
            if len(list(xml.iter()))==97 and ids(xml)==ids(old):chosen=row;break
        elif norm(row['error_line'])==norm(finding.get('errorLine1','')):chosen=row;break
    if chosen is None:
        rejected.append({'id':finding.get('id'),'severity':finding.get('severity'),'file':path,'line':location.get('line'),
                         'message':finding.get('message'),'source_anchor':finding.get('errorLine1',''),
                         'reason':'NO_APPROVED_EXACT_SOURCE_ANCHOR_OR_REMEDIATION_STILL_PRESENT'})
        continue
    used[chosen['issue_id']]+=1
    attrs={k:v for k,v in finding.attrib.items() if k in ('id','message','errorLine1','errorLine2')}
    node=ET.Element('issue',attrs)
    for loc in finding.findall('location'):
        absolute=Path(loc.get('file')).resolve();absolute.relative_to(root)
        relative=os.path.relpath(absolute,root/'app')
        ET.SubElement(node,'location',dict(loc.attrib,file=relative))
    entries.append(node);matches.append({'issue_id':chosen['issue_id'],'detector':chosen['detector'],'file':path,'fresh_line':location.get('line'),'rationale':chosen['exact_change_or_rationale']})

receipt={'status':'PASS_EXACT_BASELINE_READY' if not rejected else 'FAIL_UNAPPROVED_FINDINGS',
         'approved_residual_count':len(entries),'rejected_count':len(rejected),'matches':matches,'rejected':rejected,
         'acceptance':'This baseline is not a PASS lint receipt. Run actual strict acceptance lint/D8 with abortOnError=true and warningsAsErrors=true; unknown findings cannot be suppressed.'}
Path('audit_cp12/lint/SELECTIVE_BASELINE_RECEIPT_CP12.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
if rejected:
    print(json.dumps({'status':receipt['status'],'approved':len(entries),'rejected':len(rejected)},ensure_ascii=False))
    raise SystemExit(2)

baseline=ET.Element('issues',{'format':'6','by':'lint 8.7.3','type':'baseline','client':'gradle','dependencies':'false','name':'AGP (8.7.3)','variant':'all','version':'8.7.3'})
for node in entries:baseline.append(node)
ET.indent(baseline,space='    ');ET.ElementTree(baseline).write(root/'app/lint-baseline.xml',encoding='utf-8',xml_declaration=True)
print(json.dumps({'status':receipt['status'],'approved':len(entries),'baseline':str(root/'app/lint-baseline.xml')}))
