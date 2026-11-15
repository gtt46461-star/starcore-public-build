#!/usr/bin/env python3
"""Keep per-ID current evidence without importing historical PASS results."""
import collections,json,shutil,sys,xml.etree.ElementTree as ET
from pathlib import Path
from run_step import ROOT,PACKAGE,REPO,sha,now

b=sys.argv[1]
dest=ROOT/(b+'-gradle-results');dest.mkdir(exist_ok=True)
tests={};counts=collections.Counter()
for p in sorted((REPO/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
    shutil.copy2(p,dest/p.name)
    for c in ET.parse(p).getroot().iter('testcase'):
        status='FAIL' if c.find('failure') is not None else 'ERROR' if c.find('error') is not None else 'SKIP' if c.find('skipped') is not None else 'PASS'
        tests[c.get('classname')+'#'+c.get('name')]={'status':status,'xml':str((dest/p.name).relative_to(ROOT)),'xmlSha256':sha(p)}
        counts[status]+=1
for p in (REPO/'app/build/reports').glob('lint-results-debug.*'):
    if p.suffix in ('.xml','.txt','.html'):shutil.copy2(p,dest/p.name)
commands=[json.loads(p.read_text()) for p in ROOT.glob(b+'-gradle*.command.json')]
required_tasks={':app:compileDebugJavaWithJavac',':app:compileDebugAndroidTestJavaWithJavac',':app:testDebugUnitTest',':app:lintDebug'}
full_commands=[x for x in commands if x.get('exitCode')==0 and {arg for arg in x.get('argv',[]) if arg.startswith(':app:')}==required_tasks and not any(arg.startswith('--tests') for arg in x.get('argv',[]))]
gradle_ok=bool(full_commands)
minimum_tests=655 if b=='P1' else 659
assert gradle_ok and counts['PASS']>=minimum_tests and not counts['FAIL'] and not counts['ERROR'] and not counts['SKIP'],dict(counts)
mapping=json.loads((PACKAGE/'contracts/ACCEPTANCE_EXECUTION_MAP_222.json').read_text())
current=json.loads((PACKAGE/'contracts/CURRENT_ACCEPTANCE_222.json').read_text())
original={x['id']:x for x in current}
items=[]
for x in mapping['items']:
    if x['runAt']!=b:continue
    refs=[]
    for e in x.get('entries',[]):
        if e.startswith('HOST:'):
            candidates=[p for p in ROOT.glob(b+'-bound-'+e[5:]+'*.command.json')]
            executed=[(p,json.loads(p.read_text())) for p in candidates]
            executed.sort(key=lambda item:item[1].get('finishedAt',''),reverse=True)
            good=next(((p,v) for p,v in executed if v.get('exitCode')==0),None)
            refs.append(dict(entry=e,status='PASS' if good else 'NOT_PASSED',command=good[0].name if good else None,logSha256=good[1]['logSha256'] if good else None,priorAttempts=[dict(command=p.name,status=v.get('status'),exitCode=v.get('exitCode'),sourceCommit=v.get('sourceCommit')) for p,v in executed]))
        else:refs.append(dict(entry=e,**tests.get(e,{'status':'NOT_EXECUTED'})))
    all_ok=bool(refs) and all(r['status']=='PASS' for r in refs)
    row=dict(original[x['id']]);row.update(original_text=x['original_text'],original_text_sha256=x['original_text_sha256'],original_card_text=x['original_card_text'],original_card_text_sha256=x['original_card_text_sha256'],level=x['level'],at=now(),engineeringEntryResults=refs,engineeringStatus='PASS_ALL_INDIVIDUALLY_NAMED_ENTRIES' if all_ok else 'BLOCKED_EXTERNAL_EVIDENCE' if not refs else 'INCOMPLETE_ENTRY_RESULTS',wholeClauseProved=False,historicalPassImported=False)
    row['remaining']='Review complete original clause against recorded assertion scope; Android/device/natural evidence is separate.' if refs else 'Requires actual device, original database/artifacts, signed installation or independent natural observations per EXTERNAL_ACCEPTANCE_PROTOCOLS_33.md.'
    items.append(row)
(ROOT/(b+'-acceptance.json')).write_text(json.dumps(items,ensure_ascii=False,indent=2)+'\n')
(ROOT/(b+'-junit-summary.json')).write_text(json.dumps({'batch':b,'counts':dict(counts),'tests':tests,'gradleCommands':commands,'scope':'CURRENT_STAGE_GRADLE_JUNIT_AND_LINT_NOT_DEVICE_OR_NATURAL','at':now()},indent=2)+'\n')
print(json.dumps({'batch':b,'junit':dict(counts),'originalIds':len(items),'entryStatus':dict(collections.Counter(x['engineeringStatus'] for x in items))}))
