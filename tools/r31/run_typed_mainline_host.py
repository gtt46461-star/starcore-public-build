#!/usr/bin/env python3
"""Actual R27 freeze -> unique choice -> R974 -> original DecisionGate on
native SQLite. Test namespace mechanics only; never natural quality or Android."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,runpy,shutil,subprocess,tempfile,uuid,zipfile
from host_runtime_fixture import compile_owned_platform
ROOT=Path(__file__).resolve().parents[2]
OWNERS=['strategyv2/MainlineFreezeProofR31','strategyv2/MainlineAdapterR31','strategyv2/MainlineBridgeSU2','strategyv2/MainlineModelBindingR31','strategyv2/MainlineChoiceStoreR31','strategyv2/LiveShadowsR27','UnifiedResearchChoiceR932','CertifiedPredictionR974','DbStore','DecisionGateEngine','V51CandidateRealGateR915','strategyv2/RuntimeR27','strategyv2/ResearchWriteQueueR27','ResearchPlatformStoreR929','strategyv2/RealAuthoritySU2','strategyv2/MainlineAdapterSU2','strategyv2/Su2CommitAuthorityR30','strategyv2/CertificateSU2']
def digest(path):
 if path.is_file():return hashlib.sha256(path.read_bytes()).hexdigest()
 h=hashlib.sha256()
 for f in sorted(path.rglob('*.class')):h.update(str(f.relative_to(path)).encode()+b'\0'+hashlib.sha256(f.read_bytes()).digest())
 return h.hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--classes',type=Path,default=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes');a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));slf=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.slf4j/slf4j-api/*/*/*.jar'))
 owner_runner=ROOT/'tools/r31/run_lifecycle_stores_host.py';owner_names=runpy.run_path(str(owner_runner),run_name='typed_owned_declarations')['OWNERS'];names=list(dict.fromkeys([*OWNERS,*owner_names,'DatabaseCoordinator','strategyv2/RuntimeSchemaR26','strategyv2/IssueTimelineWriterR30']))
 sources=[ROOT/'app/src/main/java/com/ethk3/novaonepro'/f'{name}.java' for name in names];host=ROOT/'tools/r31/unified_host/TypedMainlineR31Host.java';runtime_fixture=ROOT/'tools/r31/unified_host/TypedFreezeRuntimeR31Fixture.java';rebirth=ROOT/'tools/r31/unified_host/TypedRebirthR31Host.java';segment=ROOT/'tools/r31/unified_host/TypedRegisteredSegmentR31Host.java'
 json_sources=sorted((ROOT/'verification/r932/android-json').rglob('*.java'))
 values_source=ROOT/'tools/r31/ddl_host/android/content/ContentValues.java'
 old_adapter=ROOT/'tools/r30/qual_host/authority_shim/com/ethk3/novaonepro/DbStore.java'
 old_hosts=[ROOT/'tools/r30/qual_host/RealAuthorityR30HostTest.java',ROOT/'tools/r30/qual_host/CertificateRevocationR30HostTest.java',ROOT/'tools/r30-repair/qual_host/R31MainlineAuthorityHost.java']
 bound=sources+[host,runtime_fixture,rebirth,segment,*old_hosts,old_adapter,ROOT/'app/src/main/assets/r27/golden_512.bin',Path(__file__).resolve(),owner_runner,ROOT/'tools/r31/host_runtime_fixture.py',values_source,*sorted((ROOT/'tools/r29/qual_host/shim').rglob('*.java')),*json_sources]
 hashes={str(f.relative_to(ROOT)):digest(f) for f in bound};support=digest(a.classes)
 dependencies={str(f):digest(f) for f in [sdk,jdbc,slf]}
 receipt={'format':'R31_TYPED_MAINLINE_NATIVE_EXECUTION_V1','invocationId':str(uuid.uuid4()),'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'tree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip(),'dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sourceSha256':hashes,'compiledSupportSha256':support,'dependenciesSha256':dependencies,'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'ACTUAL_TYPED_R27_FREEZE_CHOICE_R974_DECISIONGATE_AND_ORIGINAL_DBSTORE_TERMINAL_COMMIT_CLOSE_REOPEN_THREE_PRODUCTS_NATIVE_SQLITE; SYNTHETIC_TEST_NAMESPACE_MECHANICS_NOT_ANDROID_OR_NATURAL; ORIGINAL_SU2_AUTHORITY_FIXTURES_USE_THEIR_UNCHANGED_READ_ADAPTER','status':'STARTED_NOT_PASS','records':[]}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 def run(command,name):
  r=subprocess.run(list(map(str,command)),cwd=ROOT,capture_output=True,text=True,timeout=240);(out/(name+'.log')).write_text(r.stdout+r.stderr);receipt['records'].append({'stage':name,'exitCode':r.returncode});save();print(name,'exit='+str(r.returncode),flush=True)
  if r.returncode:print((r.stdout+r.stderr)[-9000:]);r.check_returncode()
 save()
 try:
  with tempfile.TemporaryDirectory(prefix='r31-typed-mainline-') as temp:
   work=Path(temp);product=work/'product';product.mkdir();tests=work/'tests';tests.mkdir()
   run(['javac','-J-Xmx512m','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[a.classes,sdk])),'-d',product,*sources],'compile-current-production')
   combined=work/'support'
   if a.classes.is_dir():shutil.copytree(a.classes,combined)
   else:
    combined.mkdir()
    with zipfile.ZipFile(a.classes) as archive:
     for item in archive.infolist():
      name=Path(item.filename)
      if name.is_absolute() or '..' in name.parts:raise RuntimeError('SUPPORT_ARCHIVE_PATH_NOT_RELATIVE')
      if item.is_dir():continue
      target=combined/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(archive.read(item))
   shutil.copytree(product,combined,dirs_exist_ok=True)
   platform=compile_owned_platform(ROOT,work/'adapter',combined,sdk)
   # Real DbStore is compiled above. Only Android SQLite/Context/clock/helper
   # are adapted; never replace DbStore/DecisionGate/certificate business code.
   (platform/'com/ethk3/novaonepro/DbStore.class').unlink()
   cursor=work/'adapter/sources/android/database/Cursor.java';code=cursor.read_text().replace('    void close();','    default int getColumnIndex(String name){for(int i=0;i<getColumnCount();i++)if(name.equals(getColumnName(i)))return i;return -1;}\n    default int getColumnIndexOrThrow(String name){int i=getColumnIndex(name);if(i<0)throw new IllegalArgumentException("HOST_COLUMN_MISSING:"+name);return i;}\n    void close();');cursor.write_text(code)
   sqlite=work/'adapter/sources/android/database/sqlite/SQLiteDatabase.java';code=sqlite.read_text();at=code.index('    static final class Rows')
   code=code[:at]+'''    public Cursor query(String table,String[] columns,String selection,String[] args,String group,String having,String order,String limit){String sql="SELECT "+(columns==null?"*":String.join(",",columns))+" FROM "+table+(selection==null?"":" WHERE "+selection)+(group==null?"":" GROUP BY "+group)+(having==null?"":" HAVING "+having)+(order==null?"":" ORDER BY "+order)+(limit==null?"":" LIMIT "+limit);return rawQuery(sql,args);}
    public Cursor query(String table,String[] columns,String selection,String[] args,String group,String having,String order){return query(table,columns,selection,args,group,having,order,null);}
    public long insertWithOnConflict(String table,String nullColumn,android.content.ContentValues values,int conflict){java.util.List<String> columns=new java.util.ArrayList<>();java.util.List<Object> args=new java.util.ArrayList<>();for(java.util.Map.Entry<String,Object> e:values.valueSet()){columns.add(e.getKey());args.add(e.getValue());}execSQL("INSERT "+(conflict==5?"OR REPLACE ":conflict==4?"OR IGNORE ":"")+"INTO "+table+"("+String.join(",",columns)+") VALUES("+String.join(",",java.util.Collections.nCopies(columns.size(),"?"))+")",args.toArray());try(Cursor c=rawQuery("SELECT last_insert_rowid()",null)){return c.moveToFirst()?c.getLong(0):-1;}}
    public long insertOrThrow(String table,String nullColumn,android.content.ContentValues values){return insertWithOnConflict(table,nullColumn,values,0);}
    public long insert(String table,String nullColumn,android.content.ContentValues values){return insertWithOnConflict(table,nullColumn,values,0);}
'''+code[at:];sqlite.write_text(code)
   values=work/'adapter/sources/android/content/ContentValues.java';values.write_text(values_source.read_text())
   utils=work/'adapter/sources/android/database/DatabaseUtils.java';utils.write_text('package android.database;public final class DatabaseUtils{public static long longForQuery(android.database.sqlite.SQLiteDatabase db,String sql,String[] args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("HOST_NO_SCALAR");return c.getLong(0);}}}')
   run(['javac','-cp',os.pathsep.join(map(str,[platform,sdk])),'-d',platform,cursor,values,sqlite,utils,*json_sources],'compile-native-platform-query-adapter')
   receipt['platformAdapterSha256']={str(f.relative_to(work/'adapter/sources')):digest(f) for f in [cursor,values,sqlite,utils]}
   cp=os.pathsep.join(map(str,[product,platform,tests,a.classes,jdbc,slf,sdk]));run(['javac','-cp',cp,'-d',tests,host,runtime_fixture,rebirth,segment],'compile-current-path-fixture')
   receipt['compiledCurrentProductSha256']=digest(product)
   run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.TypedMainlineR31Host',out/'execution'],'actual-freeze-choice-decisiongate-and-revocation')
   run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.TypedRebirthR31Host',out/'rebirth'],'actual-semantic-rebirth-and-unknown-identity')
   run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.TypedRegisteredSegmentR31Host',out/'registered-segment'],'actual-registered-new-future-segment-and-identity-rejection')
   old=work/'original-authority-adapter';old.mkdir();run(['javac','-cp',cp,'-d',old,old_adapter],'compile-original-authority-read-adapter')
   old_cp=os.pathsep.join(map(str,[old,product,platform,tests,a.classes,jdbc,slf,sdk]));run(['javac','-cp',old_cp,'-d',tests,*old_hosts],'compile-unchanged-original-authority-fixtures')
   for fixture in old_hosts:run(['java','-Xmx768m','-cp',old_cp,'com.ethk3.novaonepro.strategyv2.'+fixture.stem],'original-'+fixture.stem)
   if receipt['compiledCurrentProductSha256']!=digest(product):raise RuntimeError('COMPILED_CURRENT_PRODUCT_CHANGED_DURING_EXECUTION')
  if hashes!={str(f.relative_to(ROOT)):digest(f) for f in bound} or support!=digest(a.classes) or dependencies!={str(f):digest(f) for f in [sdk,jdbc,slf]}:raise RuntimeError('SOURCE_OR_SUPPORT_CHANGED_DURING_EXECUTION')
  receipt['status']='PASS_HOST_ACTUAL_PATH_NOT_ANDROID_OR_NATURAL';receipt['sourceAndSupportUnchanged']=True
 except BaseException:
  receipt['status']='FAILED_CURRENT_EXECUTION';raise
 finally:
  receipt['finishedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
if __name__=='__main__':main()
