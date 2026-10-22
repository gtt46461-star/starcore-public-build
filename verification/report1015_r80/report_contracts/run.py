import json, pathlib,subprocess,hashlib,shutil
R=pathlib.Path(__file__).resolve().parents[3]; O=pathlib.Path(__file__).resolve().parent; C=json.load(open(R.parent/'audit_work/r80_combined_host_owner_terminal/config.json')); W=R.parent/'audit_work/r80_report_contracts';W.mkdir(exist_ok=True); (W/'classes').mkdir(exist_ok=True)
s=pathlib.Path(R.parent/'audit_work/r80_combined_host_owner_terminal/SQLiteDatabase.java').read_text();s=s.replace('public String getPath(){return "HOST_SQLITE";}','public String getPath(){return actualPath;}\n private String actualPath; private boolean readOnly; public boolean isReadOnly(){return readOnly;} public void makeReadOnly(){execSQL("PRAGMA query_only=ON");readOnly=true;}')
s=s.replace('process=(copy==null?', 'actualPath=copy;process=(copy==null?');(W/'SQLiteDatabase.java').write_text(s)
files=[W/'SQLiteDatabase.java',O/'ReportConsumerR80.java',R/'verification/canonical/adapters/android/util/AtomicFile.java']
for name in ['ReportSemanticContractCP12','ReportSpoolContractCP12','ReportPublicationContractCP12','ReportQueueRecoveryContractCP12']:
 files.append(R/'verification/canonical/tests/audit_cp12/report/tests'/f'{name}.java')
files.extend([R/'verification/r993/ReportMetadataRecoveryR993.java',R/'verification/r992/ReportExpiryR992.java'])
cp=':'.join([str(W/'classes'),C['adapters'],C['product'],C['tests'],C['android']]); results=[]
def run(name,cmd):
 q=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=180);(O/(name+'.log')).write_text(q.stdout);results.append({'case':name,'command':cmd,'exit':q.returncode});(O/'commands.json').write_text(json.dumps(results,indent=2));print(name,q.returncode, q.stdout[-300:],flush=True);return q.returncode
if run('compile',[C['javac'],'-cp',cp,'-d',str(W/'classes'),*map(str,files)]):raise SystemExit(1)
for n,args in [('ReportSemanticContractCP12',[]),('ReportSpoolContractCP12',[str(W/'spool')]),('ReportPublicationContractCP12',[str(W/'publication')]),('ReportQueueRecoveryContractCP12',[str(W/'queue')]),('ReportMetadataRecoveryR993',[str(W/'metadata'),'orphan']),('ReportExpiryR992',[str(W/'expiry')])]:run(n,[C['java'],'-Xmx64m','-cp',cp,'com.ethk3.novaonepro.'+n,*args])
for name,source in [('probe','r80_probe_owner_terminal_logged/multihorizon.db'),('bound','r80_bound_owner_terminal/chain.db')]:
 d=W/name;d.mkdir(exist_ok=True);shutil.copy2(R.parent/'audit_work'/source,d/'snapshot.db');run('consumer-'+name,[C['java'],'-Xmx128m','-cp',cp,'com.ethk3.novaonepro.ReportConsumerR80',C['bridge'],str(d/'snapshot.db'),str(d),name])
hashes={}
for name in ['ReportDiagnosticEvidenceR64','ReportReadContextCP4','ReportPublicationCP12','ReportArchiveCP4','MultiHorizonProbeR80','ResearchPredrawCaptureR923']:
 p=pathlib.Path(C['product'])/'com/ethk3/novaonepro'/f'{name}.class';hashes[name]=hashlib.sha256(p.read_bytes()).hexdigest()
(O/'product_hashes.json').write_text(json.dumps(hashes,indent=2))
