"""Safely expand preserved archives and discover paths. No external downloads."""
from pathlib import Path
import argparse,json,zipfile,subprocess,sys
p=Path(__file__).resolve().parents[1]
def expand(z,d):
 d.mkdir(parents=True,exist_ok=True);base=d.resolve()
 with zipfile.ZipFile(z) as f:
  for i in f.infolist():
   q=(d/i.filename).resolve()
   if not q.is_relative_to(base):raise ValueError('UNSAFE_ZIP_PATH')
   if (i.external_attr>>16)&0o170000==0o120000:raise ValueError('ZIP_SYMLINK_REJECTED')
  f.extractall(d)
a=argparse.ArgumentParser();a.add_argument('--out',required=True,type=Path);a.add_argument('--audit',action='store_true');x=a.parse_args()
expand(p/'history/10.25晚上100m总包施工方案.zip',x.out/'plan')
expand(p/'history/10.25检测报告.zip',x.out/'report')
# Reports are sometimes wrapped in one additional ZIP.
for depth in range(3):
 hits=list((x.out/'report').rglob('DIAGNOSTIC_SCOPE.json'))
 if hits:break
 archives=list((x.out/'report').rglob('*.zip'))
 if not archives:break
 for z in archives:expand(z,z.with_suffix(''))
plans=list((x.out/'plan').rglob('report_4743_draws.csv'))
if not plans or not hits:raise SystemExit('INPUT_DISCOVERY_FAILED: inspect extracted archive layout')
plan=plans[0].parent.parent.resolve();report=hits[0].parent.resolve()
resolved={'plan':str(plan),'report':str(report)};(x.out/'resolved_inputs.json').write_text(json.dumps(resolved,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(resolved,ensure_ascii=False,indent=2))
if x.audit:subprocess.run([sys.executable,str(p/'code/audit_inputs_r24.py'),'--plan',str(plan),'--report',str(report),'--out',str(x.out/'recomputed_evidence')],check=True)
