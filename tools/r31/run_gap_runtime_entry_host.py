#!/usr/bin/env python3
"""Fresh actual Runtime retention-task -> fence -> typed coordinator -> Gap CAS.
Explicit source maps are interface prechecks only; no Android/quality claim.
"""
from pathlib import Path
import argparse,datetime,hashlib,importlib.util,json,os,runpy,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
def digest(path):
 if path.is_file():return hashlib.sha256(path.read_bytes()).hexdigest()
 h=hashlib.sha256()
 for f in sorted(path.rglob('*.class')):h.update(str(f.relative_to(path)).encode()+b'\0'+hashlib.sha256(f.read_bytes()).digest())
 return h.hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--source-map',type=Path);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 classes=Path(os.environ.get('R31_COMPILED_SUPPORT',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'));sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));slf=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.slf4j/slf4j-api/*/*/*.jar'))
 owners=runpy.run_path(str(ROOT/'tools/r31/run_lifecycle_stores_host.py'),run_name='owned_names')['OWNERS'];owners=list(dict.fromkeys([*owners,'DatabaseCoordinator','strategyv2/RuntimeSchemaR26','strategyv2/IssueTimelineWriterR30']))
 names=['app/src/main/java/com/ethk3/novaonepro/'+x+'.java' for x in owners];sources={x:ROOT/x for x in names}
 if a.source_map:
  mp=json.loads(a.source_map.read_text());assert all(x in mp for x in names);sources={x:Path(mp[x]).resolve() for x in names}
 host=ROOT/'tools/r31/unified_host/GapRuntimeEntryR31Host.java';runner=Path(__file__);portable_path=ROOT/'tools/r31/run_atomnet_publication_host.py';support=digest(classes);before={x:digest(y) for x,y in sources.items()};extra={str(x.relative_to(ROOT)):digest(x) for x in [host,runner,portable_path,ROOT/'tools/r31/run_lifecycle_stores_host.py']};commands=[]
 def run(cmd,label):
  r=subprocess.run(list(map(str,cmd)),cwd=ROOT,capture_output=True,text=True,timeout=180);(out/(label+'.log')).write_text(r.stdout+r.stderr);commands.append({'argv':list(map(str,cmd)),'exitCode':r.returncode,'label':label})
  if r.returncode:raise RuntimeError(label+'\n'+(r.stdout+r.stderr)[-6000:])
  return r.stdout
 (out/'INPUTS_BEFORE.json').write_text(json.dumps({'sources':before,'support':support,'harness':extra},indent=2)+'\n')
 spec=importlib.util.spec_from_file_location('platform_adapter',portable_path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 platform_sources=module.platform_sources()
 cursor_name='android/database/Cursor.java'
 platform_sources[cursor_name]=platform_sources[cursor_name].replace('    void close();','    default int getColumnIndex(String name){for(int i=0;i<getColumnCount();i++)if(name.equalsIgnoreCase(getColumnName(i)))return i;return -1;}\n    default int getColumnIndexOrThrow(String name){int n=getColumnIndex(name);if(n<0)throw new IllegalArgumentException(name);return n;}\n    void close();',1)
 sqlite_name='android/database/sqlite/SQLiteDatabase.java'
 query_adapter='public android.database.Cursor query(String table,String[] columns,String where,String[] args,String group,String having,String order,String limit){String sql="SELECT "+(columns==null?"*":String.join(",",columns))+" FROM "+table+(where==null?"":" WHERE "+where)+(group==null?"":" GROUP BY "+group)+(having==null?"":" HAVING "+having)+(order==null?"":" ORDER BY "+order)+(limit==null?"":" LIMIT "+limit);return rawQuery(sql,args);}'
 platform_sources[sqlite_name]=platform_sources[sqlite_name].replace('public final Connection jdbc;',query_adapter+'public final Connection jdbc;',1)
 platform_sources['android/os/Build.java']='package android.os;public final class Build{public static final String MODEL="HOST_NATIVE_RUNTIME";public static final class VERSION{public static final int SDK_INT=0;}}'
 with tempfile.TemporaryDirectory(prefix='r31-gap-runtime-') as temp:
  work=Path(temp);platform=work/'platform';production=work/'production';tests=work/'tests'
  for d in [platform,production,tests]:d.mkdir()
  shims=[]
  for name,body in platform_sources.items():
   f=work/'src'/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body);shims.append(f)
  run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',platform,*shims],'platform')
  run(['javac','-J-Xmx768m','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',production,*sources.values()],'production')
  cp=os.pathsep.join(map(str,[platform,production,tests,classes,jdbc,slf,sdk]));run(['javac','-cp',cp,'-d',tests,host],'host');result=run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.GapRuntimeEntryR31Host',work/'data'],'native-runtime')
 assert before=={x:digest(y) for x,y in sources.items()},'SOURCE_CHANGED_DURING_EXECUTION';assert support==digest(classes),'COMPILED_SUPPORT_CHANGED_DURING_EXECUTION';assert extra=={str(x.relative_to(ROOT)):digest(x) for x in [host,runner,portable_path,ROOT/'tools/r31/run_lifecycle_stores_host.py']},'HARNESS_CHANGED_DURING_EXECUTION'
 print(result);(out/'RECEIPT.json').write_text(json.dumps({'state':'PASS_NATIVE_INTERFACE_PRECHECK_NOT_FINAL_GATE' if a.source_map else 'PASS_ACTUAL_RUNTIME_GAP_NATIVE','sourceMode':'MULTI_WORKTREE_EXPLICIT_SOURCE_MAP' if a.source_map else 'SINGLE_CURRENT_SOURCE_TREE','scope':'ACTUAL_RUNTIME_EXECUTE_RETENTION_PHASE20_FENCE_TYPED_COORDINATOR_GAP_SOURCE_CAS; CONTROLLED_NATIVE_NOT_ANDROID','sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sources':before,'supportSha256':support,'harness':extra,'platformSha256':{name:hashlib.sha256(body.encode()).hexdigest() for name,body in platform_sources.items()},'sourceAndSupportUnchangedDuringExecution':True,'commands':commands,'result':result.strip(),'device':'NOT_RUN','naturalQuality':'NOT_PROVEN','capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()},indent=2)+'\n')
if __name__=='__main__':main()
