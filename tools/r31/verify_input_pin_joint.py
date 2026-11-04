#!/usr/bin/env python3
"""Freeze explicit source/API inputs and execute actual source guards on native SQLite. No Gradle."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[2]
# tools/r31/file -> repository root is parents[2].
BASE=Path('app/src/main/java/com/ethk3/novaonepro')
SHA=lambda data:hashlib.sha256(data).hexdigest()

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--support-jar',type=Path,required=True)
    parser.add_argument('--shared-api-classes',type=Path,required=True)
    parser.add_argument('--domain-root',type=Path,default=Path('/workspace/r31-shared-migrations'))
    parser.add_argument('--qualification-root',type=Path,default=Path('/workspace/r31-live-writer-budgets'))
    parser.add_argument('--evolution-root',type=Path,default=Path('/workspace/r31-evolution-publication-de9'))
    parser.add_argument('--jdk',type=Path,default=Path('/workspace/starcore-environment/tools/jdk-17.0.17+10'))
    parser.add_argument('--sdk',type=Path,default=Path('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar'))
    parser.add_argument('--jdbc',type=Path,default=Path('/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
    parser.add_argument('--out',type=Path)
    args=parser.parse_args()
    out=(args.out or ROOT/'verification/r31/p01/input-pin-budget/runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')).resolve()
    out.mkdir(parents=True,exist_ok=False)
    work=Path(tempfile.mkdtemp(prefix='r31-source-pin-joint-'))
    source,production,platform,host,rich,critical=[work/name for name in ['source','production','platform','host','critical-platform','critical-host']]
    for directory in [source,production,platform,host,rich,critical]:directory.mkdir()
    receipt={'schema':'R31_SOURCE_PIN_NATIVE_1','scope':'EXPLICIT_PRODUCTION_SOURCE_AND_FROZEN_SHARED_API_NATIVE_SQLITE_NOT_WHOLE_ANDROID',
      'started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'work':str(work),'source_head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
      'status':'IMPLEMENTING','commands':[],'sources':[],'runner_sha256':SHA(Path(__file__).read_bytes()),'support_jar':{'path':str(args.support_jar),'sha256':SHA(args.support_jar.read_bytes())},
      'jdbc':{'path':str(args.jdbc),'sha256':SHA(args.jdbc.read_bytes())},'shared_api_classes':str(args.shared_api_classes),
      'device':'NOT_RUN','android_whole_build':'NOT_RUN','natural_quality':'NOT_EVALUATED',
      'limits':['The fixed shared API classes supply unrelated protocols; all thirteen modified pin/consumer/Domain/Q/Evolution source inputs override their older classes.',
      'Host Android adapters replace platform connection/lifecycle only. Source guards, writer batches, migrations, Critical predecessor DDL and SQLite triggers are actual production methods.',
      'Source/member journal lifecycle and unsupported SQL REPLACE are not established by these tests. Per-source-row write bounds do not certify every upstream bulk writer.',
      'No APK identity, device duration, or future quality proof is produced here.']}
    def save(): (out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    def run(name,cmd,timeout=180):
        start=datetime.datetime.now(datetime.timezone.utc);result=subprocess.run(list(map(str,cmd)),cwd=work,capture_output=True,text=True,timeout=timeout)
        (out/(name+'.stdout.log')).write_text(result.stdout);(out/(name+'.stderr.log')).write_text(result.stderr)
        receipt['commands'].append({'name':name,'argv':list(map(str,cmd)),'exit_code':result.returncode,'elapsed_seconds':(datetime.datetime.now(datetime.timezone.utc)-start).total_seconds(),
          'stdout_sha256':SHA(result.stdout.encode()),'stderr_sha256':SHA(result.stderr.encode())});save();print(json.dumps({'name':name,'exit_code':result.returncode,'output':str(out)}),flush=True)
        if result.returncode:raise RuntimeError(name+' failed; original stdout/stderr retained')
        return result.stdout
    def snapshot(path,target):
        data=path.read_bytes();target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
        receipt['sources'].append({'origin':str(path),'snapshot':str(target),'sha256':SHA(data)})
        return target
    try:
        # Never execute another owner's live compilation directory. Copy its fixed API-only closure
        # and disclose every byte; the thirteen source inputs below take precedence over it.
        api=work/'shared-api-frozen';api.mkdir();inventory={}
        for origin in sorted(args.shared_api_classes.rglob('*.class')):
            relative=origin.relative_to(args.shared_api_classes);data=origin.read_bytes();target=api/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);inventory[str(relative)]=SHA(data)
        if not inventory:raise RuntimeError('Empty shared API class closure')
        receipt['shared_api_class_inventory_sha256']=SHA(json.dumps(inventory,sort_keys=True,separators=(',',':')).encode())
        receipt['shared_api_class_inventory']=inventory
        receipt['shared_api_frozen_path']=str(api)
        names=['ResearchSourceJournalR31','PinSnapshotProofR31','ResearchInputPinR30R1','AtomNetRuntimeR30','MatrixEvidenceR30R1','MatrixResearchR30R1','ModelReplayStudyR30R1','OnlineResearchWindowR30R1']
        origins=[ROOT/BASE/'strategyv2'/f'{name}.java' for name in names]
        origins += [args.domain_root/BASE/name for name in ['SchemaDomainCoordinatorR926.java','strategyv2/SchemaR27.java']]
        origins += [args.qualification_root/BASE/'strategyv2/QualificationInboxR29.java']
        origins += [args.evolution_root/BASE/'strategyv2'/f'{name}.java' for name in ['EvolutionPublicationStoreR31','EvolutionPublicationSqlR31']]
        files=[snapshot(origin,source/'production'/origin.name) for origin in origins]
        classpath=os.pathsep.join(map(str,[api,args.support_jar,args.sdk]))
        javac=args.jdk/'bin/javac';java=args.jdk/'bin/java'
        run('production-javac',[javac,'-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',classpath,'-d',production,*files])
        shims=[snapshot(origin,source/'plain-shim'/origin.relative_to(ROOT/'tools/r29/qual_host/shim')) for origin in (ROOT/'tools/r29/qual_host/shim').rglob('*.java')]
        run('plain-platform-javac',[javac,'-encoding','UTF-8','-cp',str(args.sdk),'-d',platform,*shims])
        runtime=os.pathsep.join(map(str,[platform,production,api,args.support_jar,args.jdbc,args.sdk]))
        hosts=[snapshot(ROOT/'tools/r31/input_pin_host/InputPinJournalR31Host.java',source/'InputPinJournalR31Host.java'),snapshot(ROOT/'tools/r30-repair/qual_host/RepairInputPinR30HostTest.java',source/'RepairInputPinR30HostTest.java')]
        run('pin-host-javac',[javac,'-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',runtime,'-d',host,*hosts])
        for name in ['InputPinJournalR31Host','RepairInputPinR30HostTest']:run(name,[java,'-Xmx512m','-cp',str(host)+os.pathsep+runtime,'com.ethk3.novaonepro.strategyv2.'+name])
        rich_sources=[]
        for origin in (ROOT/'tools/r31/ddl_host').rglob('*.java'):
            target=snapshot(origin,source/'rich-shim'/origin.relative_to(ROOT/'tools/r31/ddl_host'))
            if origin.name=='SQLiteDatabase.java':
                text=target.read_text();old='public Cursor rawQuery(String sql,String[] args){log('
                if text.count(old)!=1:raise RuntimeError('Unexpected DDL shim; explicit Android null-binding adapter must be reviewed')
                text=text.replace(old,'public Cursor rawQuery(String sql,String[] args){if(args!=null)for(int i=0;i<args.length;i++)if(args[i]==null)throw new IllegalArgumentException("the bind value at index "+(i+1)+" is null");log(');target.write_text(text)
                receipt['strict_null_binding_adapter_sha256']=SHA(text.encode())
            rich_sources.append(target)
        rich_sources.append(snapshot(ROOT/'tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java',source/'rich-shim/android/database/sqlite/SQLiteException.java'))
        adapters={'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000;}}',
          'android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
          'android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}'}
        for name,text in adapters.items():path=source/'rich-shim'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text);rich_sources.append(path)
        receipt['host_only_adapters']=adapters
        run('critical-platform-javac',[javac,'-encoding','UTF-8','-cp',classpath,'-d',rich,*rich_sources])
        fixture=ROOT/'verification/r31/p02/schema_mainchain/runs/20261104T112314Z/source_snapshot/MainChainSchemaR31Host.java'
        fixture_snapshot=snapshot(fixture,source/fixture.name)
        text=fixture_snapshot.read_text();old='static final class Db extends SQLiteDatabase'
        if text.count(old)!=1:raise RuntimeError('Unexpected existing Critical owner adapter; explicit subclass fault injection must be reviewed')
        text=text.replace(old,'static class Db extends SQLiteDatabase');fixture_snapshot.write_text(text)
        receipt['critical_fixture_adapter']={'purpose':'Allow test Db subclass for explicit source-DDL failure injection; original schema methods/guards/assertions unchanged','compiled_sha256':SHA(text.encode()),'patch':'Remove final from the HOST Db adapter class only'}
        fixtures=[fixture_snapshot,snapshot(ROOT/'tools/r31/input_pin_host/InputSourceMigrationR31Host.java',source/'InputSourceMigrationR31Host.java')]
        runtime=os.pathsep.join(map(str,[rich,production,api,args.support_jar,args.jdbc,args.sdk]))
        run('critical-host-javac',[javac,'-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',runtime,'-d',critical,*fixtures])
        run('InputSourceMigrationR31Host',[java,'-Xmx512m','-cp',str(critical)+os.pathsep+runtime,'com.ethk3.novaonepro.InputSourceMigrationR31Host',out/'critical-sql-api.tsv'])
        changed=[]
        for fact in receipt['sources']:
            fact['origin_unchanged_after_run']=SHA(Path(fact['origin']).read_bytes())==fact['sha256']
            if not fact['origin_unchanged_after_run']:changed.append(fact['origin'])
        if changed:raise RuntimeError('Source drift; evidence only applies to frozen snapshot: '+repr(changed))
        receipt['status']='NATIVE_NAMED_PROTOCOLS_PASS_NOT_ANDROID_OR_DEVICE'
    except Exception as failure:
        receipt['status']='FAILED';receipt['failure']=repr(failure);raise
    finally:
        receipt['finished_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat();save();print('RECEIPT '+str(out/'RECEIPT.json'),flush=True)

if __name__=='__main__':main()
