"""Inspect the signed application's compact report after saving through Android DocumentsUI."""
import csv
from collections import Counter
import hashlib
import io
import json
import os
from pathlib import Path
import sys
import zipfile

sys.path.insert(0,str(Path('source/tools/r342').resolve()))
import ci_candidate_tests as ci
import report_ui_driver as ui

tmp=Path(os.environ['RUNNER_TEMP'])
out=tmp/'coverage-evidence'
out.mkdir()
apk=tmp/'candidate-207.apk'
source=os.environ['SOURCE']
assert ui.DEVICE.startswith('emulator-') and ci.adb(ui.DEVICE,'shell','getprop','ro.kernel.qemu')=='1'
assert 'Success' in ci.adb(ui.DEVICE,'install',apk)
capture=ci.signed_report_campaign(ui.DEVICE,out,ci.sha(apk),os.environ['BASELINE_SHA'])
capture('requirements','COMPACT_DIAGNOSTIC',source)
saved=json.loads((out/'signed-report-readbacks.json').read_text())['reports'][0]
sha=lambda b:hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(ui.OUT/saved['file']) as z:
    manifest=json.loads(z.read('source-manifest.json'))
    assets=manifest['assets']
    def ledger(name):
        descriptor=assets['diagnostics/construction-current-'+name]
        data=z.read(descriptor['member'])
        assert len(data)==descriptor['bytes'] and sha(data)==descriptor['sha256']
        return data
    identity=json.loads(ledger('IDENTITY.json'))
    assert identity['sourceCommit']==source
    for file,count,key,original,distribution,identity_hash in [
        ('REQUIREMENTS_1832.csv',1832,'requirement_id','exact_requirement','states','requirementLedgerSha256'),
        ('CARDS_299.csv',299,'card_id','original_card','cardStates','cardLedgerSha256')]:
        data=ledger(file)
        assert sha(data)==identity[identity_hash]
        rows=list(csv.DictReader(io.StringIO(data.decode('utf-8-sig'),newline='')))
        assert len(rows)==len({row[key] for row in rows})==count
        assert all(sha(row[original].encode())==row[original+'_sha256'] and row['current_status'] for row in rows)
        states=Counter(row['current_status'] for row in rows)
        assert states==Counter(identity[distribution])
    assert sha(ledger('SUBITEMS.csv'))==identity['subitemsSha256']
    doc=json.loads(z.read('R30_REPORT.json'))
    assert len(doc['chapters'])==12 and len({m['metricId'] for m in doc['metricResults']})==44
    counts={g:len(doc['objectIndex'][g]['objects']) for g in ('legacyCharts','researchCharts','logicalTables','matrices','capabilities')}
    assert counts==dict(legacyCharts=29,researchCharts=18,logicalTables=16,matrices=20,capabilities=82)
    scope=json.loads(z.read('DIAGNOSTIC_SCOPE.json'))
    tables=[row['table'] for row in scope['tables']]
    assert tables and len(tables)==len(set(tables))
    assert saved['checks']==96 and not saved['rawEvidenceIncluded']
    summary={'jobId':saved['jobId'],'source':source,'requirements':1832,'requirementsByRecordedStatus':identity['states'],'fileCards':299,'subitemsIncluded':True,'checks':saved['checks'],'metrics':44,'chapters':12,'objects':counts,'actualSnapshotTables':len(tables),'zipBytes':saved['bytes'],'publicFileSha256':saved['sha256'],'status':saved['status']}
    reference=Path('source/construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip')
    assert hashlib.sha256(reference.read_bytes()).hexdigest()=='3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86'
    required=('R30_REPORT.md','R30_REPORT.json','FILE_STATE.jsonl','R26_POLICY_IDENTITIES.jsonl','R26_FROZEN_SURFACE.json','STRATEGY_MATRICES.json','UPGRADE_DIAGNOSIS.md','UPGRADE_DIAGNOSIS.json','DIAGNOSTIC_SCOPE.json')
    with zipfile.ZipFile(reference) as envelope:
        members=[n for n in envelope.namelist() if not n.endswith('/')]
        assert len(members)==1
        original_bytes=envelope.read(members[0])
    with zipfile.ZipFile(io.BytesIO(original_bytes)) as old:
        historical=json.loads(old.read('R30_REPORT.json'))
        assert all(name in old.namelist() and name in z.namelist() for name in required)
        assert {m['metricId'] for m in historical['metricResults']}=={m['metricId'] for m in doc['metricResults']}
        for key in counts:
            def idset(d):
                return {row['id' if key=='logicalTables' else 'objectId'] for row in d['objectIndex'][key]['objects']}
            assert idset(doc)==idset(historical)
        def check_ids(archive):
            return {row['id'] for n in archive.namelist() if n.startswith('catalog-') and n.endswith('.jsonl') for line in archive.open(n) if (row:=json.loads(line)).get('kind')=='check_result'}
        assert len(check_ids(z))==96 and check_ids(z)==check_ids(old)
    with z.open('FILE_STATE.jsonl') as stream:
        first=json.loads(next(stream))
        assert first['kind']=='scope' and first['boundary']
        file_count=0
        for line in stream:
            record=json.loads(line)
            if record['kind']=='file':
                assert record['path'] and record['state']
                file_count+=1
        assert file_count>0 and record['kind']=='receipt' and ('enumerationComplete' in record)
        assert record['enumerationComplete'] or record['incompleteReason'] or record['unreadable']
    findings=doc['diagnosis']['findings']
    assert findings and all({'id','evidence','conclusion','action','refutedBy'}<=row.keys() for row in findings)
    assert z.read('UPGRADE_DIAGNOSIS.md').strip() and json.loads(z.read('UPGRADE_DIAGNOSIS.json')) is not None
    assert json.loads(z.read('STRATEGY_MATRICES.json')) is not None and json.loads(z.read('R26_FROZEN_SURFACE.json')) is not None
    summary.update({'historicalReference':'577311173','sameCheckMetricAndObjectIdsAs173':True,'fileInventoryRows':file_count,'fileInventoryComplete':record['enumerationComplete'],'diagnosisRules':len(findings),'upgradeAndStrategyMembers':list(required)})
(out/'functional-readback.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(summary,ensure_ascii=False),flush=True)
