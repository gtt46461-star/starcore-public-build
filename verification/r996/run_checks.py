from pathlib import Path
import subprocess,sqlite3,shutil,json,datetime,random,hashlib
r=Path(__file__).resolve().parents[3];p=r/'project';o=r/'evidence/r996/java-verified';o.mkdir(parents=True,exist_ok=True);cl=o/'drivers';cl.mkdir(exist_ok=True);jdk=r/'toolchain_restore/toolchain/zulu17.62.17-ca-jdk17.0.17-linux_x64/bin';jar=r/'toolchain_restore/toolchain/android-sdk/platforms/android-36/android.jar';product=p/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes';host=r/'evidence/r992-schema-fixed/classes';res=p/'app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/release/processReleaseResources/R.jar';cp=':'.join(map(str,[cl,product,res,host,jar]));results=[]
def run(name,cmd,expected=0,timeout=180):
 q=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=timeout);(o/(name+'.log')).write_text(q.stdout+q.stderr);rec={'case':name,'exitCode':q.returncode,'expected':expected,'passed':q.returncode==expected,'command':list(map(str,cmd))};results.append(rec);(o/'receipt.json').write_text(json.dumps({'scope':'entire final product bytecode; Android lifecycle/API adapters; real SQLite/native; synthetic fixtures; not phone','results':results},indent=2));print(name,q.returncode,flush=True);return q
sources=list((p/'verification/r996').glob('*.java'))+list((p/'verification/r983/android').rglob('*.java'))+[p/'verification/canonical/adapters/android/util/Base64.java',p/'verification/canonical/adapters/android/util/AtomicFile.java',p/'verification/r996/android/system/Os.java',p/'verification/r36-stubs/android/widget/Toast.java']
q=run('compile',[jdk/'javac','-J-XX:-UsePerfData','-cp',cp,'-d',cl,*sources]);assert q.returncode==0,q.stderr
before=o/'before';before.mkdir(exist_ok=True);old=p/'construction/r996/before/app/src/main/java/com/ethk3/novaonepro'
q=run('before-compile',[jdk/'javac','-J-XX:-UsePerfData','-cp',':'.join(map(str,[product,res,jar,host])),'-d',before,old/'MainActivity.java',old/'AutoFetchService.java',old/'DataRetentionGovernorR900.java']);assert q.returncode==0,q.stderr
def java(name,classname,args=(),before_version=False,expected=0,timeout=180):
 return run(name,[jdk/'java','-XX:-UsePerfData','-Xmx256m','-Djava.library.path='+str(r/'evidence/r992-native'),'-Dr982.readonly.bridge='+str(p/'verification/r996/read_only_bridge.py'),'-cp',(str(before)+':' if before_version else '')+cp,'com.ethk3.novaonepro.'+classname,*args],expected,timeout)
java('before-ui','UiContinuityR996',before_version=True,expected=1);java('after-ui','UiContinuityR996')
schema=r/'evidence/r992-schema-fixed/schema.db';bridge=p/'verification/r983/typed_live_bridge.py'
for era in ('before','after'):
 db=o/(era+'-status.db');shutil.copy2(schema,db);java(era+'-analysis-status','AnalysisStatusR996',[bridge,db],before_version=era=='before',expected=1 if era=='before' else 0)
db=o/'pipeline.db';shutil.copy2(schema,db);d=sqlite3.connect(db);rng=random.Random(996);base=datetime.datetime(2026,10,5,9,13,tzinfo=datetime.timezone.utc);rows=[]
for i in range(5000):
 t=base-datetime.timedelta(minutes=4999-i);issue=t.strftime('%Y%m%d')+f'{t.hour*60+t.minute+1:04d}';numbers=list(range(1,11));rng.shuffle(numbers);ms=int(t.timestamp()*1000);rows.append((issue,ms,ms+1,'EXPLICIT_SYNTHETIC_R996',*numbers,'{}',ms))
d.executemany('INSERT INTO draws(issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,raw_json,first_seen_at) VALUES('+','.join(['?']*16)+')',rows);d.commit();d.close();files=o/'pipeline-files';files.mkdir(exist_ok=True)
for era in ('before','after'):
 head=o/(era+'-headroom.db');shutil.copy2(db,head);h=sqlite3.connect(head);h.execute('DELETE FROM draws WHERE issue IN(SELECT issue FROM draws ORDER BY issue DESC LIMIT 128)');h.commit();h.close();java(era+'-headroom','RetentionHeadroomR996',[bridge,head,o/(era+'-headroom-files')],before_version=era=='before',expected=1 if era=='before' else 0)
java('backup-progress','BackupProgressR996',[schema,o/'progress-files'])
java('continuous-pipeline','ContinuousPipelineR996',[bridge,db,files],timeout=180)
assert all(x['passed'] for x in results),results
