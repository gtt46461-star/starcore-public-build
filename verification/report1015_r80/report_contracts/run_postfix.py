#!/usr/bin/env python3
"""Run report contracts against a fresh complete Gradle product directory, never product overlays."""
import argparse,json,pathlib,subprocess,hashlib,shutil
p=argparse.ArgumentParser();p.add_argument('--config',type=pathlib.Path,required=True);p.add_argument('--out',type=pathlib.Path,required=True);a=p.parse_args();C=json.loads(a.config.read_text());R=pathlib.Path(__file__).resolve().parents[3];S=pathlib.Path(__file__).resolve().parent;O=a.out.resolve();O.mkdir(parents=True,exist_ok=False);W=R.parent/'audit_work'/O.name;W.mkdir(exist_ok=False);D=W/'drivers';D.mkdir()
base=R.parent/'audit_work/r80_report_contracts/SQLiteDatabase.java';adapter=base.read_text()
# Preserve actual SQLite storage classes in the test transport; Android Cursor type assertions must not see all numbers as strings.
adapter=adapter.replace('a[k].startsWith("B")?Base64.getDecoder().decode(a[k].substring(1)):de(a[k].substring(1))','a[k].startsWith("B")?Base64.getDecoder().decode(a[k].substring(1)):a[k].startsWith("I")?Long.valueOf(a[k].substring(1)):a[k].startsWith("F")?Double.valueOf(a[k].substring(1)):de(a[k].substring(1))')
adapter=adapter.replace('v==null?0:v instanceof byte[]?4:3','v==null?0:v instanceof byte[]?4:v instanceof Long?1:v instanceof Double?2:3');(W/'SQLiteDatabase.java').write_text(adapter)
bridge=pathlib.Path(C['bridge']).read_text().replace("else 'S'+enc(v) for v in row","else ('I'+str(v)) if isinstance(v,int) else ('F'+str(v)) if isinstance(v,float) else 'S'+enc(v) for v in row")
(W/'typed_sqlite_bridge.py').write_text(bridge);C['bridge']=str(W/'typed_sqlite_bridge.py')
cp=':'.join([str(D),C['adapters'],C['product'],C['tests'],C['android']]); records=[]
def run(name,cmd):
 q=subprocess.run(cmd,capture_output=True,text=True,timeout=240);(O/(name+'.log')).write_text(q.stdout+q.stderr);records.append({'case':name,'command':cmd,'exit':q.returncode});(O/'commands.json').write_text(json.dumps(records,indent=2));print(name,q.returncode,q.stdout[-200:],q.stderr[-300:],flush=True);return q.returncode
files=[W/'SQLiteDatabase.java',R/'verification/canonical/adapters/android/util/AtomicFile.java',*S.glob('*.java')]
for n in ['ReportSemanticContractCP12','ReportSpoolContractCP12','ReportPublicationContractCP12','ReportQueueRecoveryContractCP12']:files.append(R/'verification/canonical/tests/audit_cp12/report/tests'/f'{n}.java')
files += [R/'verification/r993/ReportMetadataRecoveryR993.java',R/'verification/r992/ReportExpiryR992.java']
if run('compile',[C['javac'],'-cp',cp,'-d',str(D),*map(str,files)]):raise SystemExit(1)
def java(name,args):return run(name,[C['java'],'-Xmx64m','-cp',cp,'com.ethk3.novaonepro.'+name,*map(str,args)])
for n in ['ReportSemanticContractCP12','ReportSpoolContractCP12','ReportPublicationContractCP12','ReportQueueRecoveryContractCP12','ReportExpiryR992']:java(n,[] if n=='ReportSemanticContractCP12' else [W/n])
java('ReportMetadataRecoveryR993',[W/'metadata','orphan']);java('ReportArchiveAvailabilityR80',[C['bridge'],W/'availability']);java('ReportCompactArchiveBudgetR80',[C['bridge'],W/'budget'])
for name,source in [('probe','r80_probe_owner_terminal_logged/multihorizon.db'),('bound','r80_bound_owner_terminal/chain.db')]:
 d=W/name;d.mkdir();shutil.copy2(R.parent/'audit_work'/source,d/'snapshot.db');run('consumer-'+name,[C['java'],'-Xmx128m','-cp',cp,'com.ethk3.novaonepro.ReportConsumerR80',C['bridge'],str(d/'snapshot.db'),str(d),name])
java('ReportProfileCompatibilityR80',[C['bridge'],W/'probe/snapshot.db'])
shutil.copy2(R.parent/'audit_work/r80_combined_host_owner_terminal/host-schema.db',W/'number-surface.db');java('ReportNumberSurfaceR80',[C['bridge'],W/'number-surface.db'])
ident={}
for name in ['ReportReadContextCP4','ReportAttachmentClosureCP12','ReportDiagnosticEvidenceR64','MultiHorizonProbeR80','ResearchPredrawCaptureR923','LiveResearchDiagnosisOrchestrator']:
 path=pathlib.Path(C['product'])/'com/ethk3/novaonepro'/f'{name}.class';ident[name]=hashlib.sha256(path.read_bytes()).hexdigest()
(O/'product_hashes.json').write_text(json.dumps(ident,indent=2));(O/'scope.json').write_text(json.dumps({'product':C['product'],'product_overlay':False,'adapters':'Host Android API, actual SQLite query_only and filesystem; no product stubs','limits':'Synthetic owner fixtures; no device latency, natural predictions, original user database, whole R1015 acceptance'},indent=2));raise SystemExit(any(x['exit'] for x in records))
