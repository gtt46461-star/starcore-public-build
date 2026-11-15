#!/usr/bin/env python3
"""Fixed-input native SQLite checks for the real R929 startup prerequisite and strict diagnostics."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[2]
SHA=lambda data:hashlib.sha256(data).hexdigest()
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--joint-manifest',type=Path,required=True);ap.add_argument('--domain-root',type=Path,default=Path('/workspace/r31-shared-migrations'));args=ap.parse_args()
    stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ');out=ROOT/'verification/r31/p02/platform-readiness/runs'/stamp;out.mkdir(parents=True)
    work=Path(tempfile.mkdtemp(prefix='r31-platform-native-'));src,platform,current,host,api=[work/n for n in ['sources','platform','current','host','joint-api']]
    for p in [src,platform,current,host,api]:p.mkdir()
    joint=json.loads(args.joint_manifest.read_text());support=joint['support'];jar=Path(support['actual014Classes']['path']);sdk=Path(support['androidSdk']['path']);jdbc=Path(support['sqliteJdbc']['path']);generated=Path(support['actualGeneratedR']['path']);jdk=Path('/workspace/starcore-environment/tools/jdk-17.0.17+10/bin')
    receipt={'scope':'FIXED_PRODUCTION_SCHEMA_DOMAIN_NATIVE_SQLITE_NOT_ART_OR_DEVICE','base':'961a6b8a74b9624a2b195e9562fbdfc9283b29fc','head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'started':stamp,'work':str(work),'status':'IMPLEMENTING','sources':[],'commands':[],'runnerSha256':SHA(Path(__file__).read_bytes()),'jointManifest':str(args.joint_manifest),'jointManifestSha256':SHA(args.joint_manifest.read_bytes()),'support':support,'device':'NOT_RUN','artTests':'NOT_RUN','naturalQuality':'NOT_EVALUATED'}
    def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    def snap(p,target):
        data=p.read_bytes();target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);receipt['sources'].append({'origin':str(p),'snapshot':str(target),'sha256':SHA(data)});return target
    def run(name,argv):
        p=subprocess.run(list(map(str,argv)),cwd=work,capture_output=True,text=True,timeout=180);(out/(name+'.stdout.log')).write_text(p.stdout);(out/(name+'.stderr.log')).write_text(p.stderr);receipt['commands'].append({'name':name,'argv':list(map(str,argv)),'exit_code':p.returncode,'stdoutSha256':SHA(p.stdout.encode()),'stderrSha256':SHA(p.stderr.encode())});save();print(json.dumps({'name':name,'exit_code':p.returncode,'out':str(out)}),flush=True)
        if p.returncode:raise RuntimeError(name+' failed; raw logs retained')
        return p.stdout
    try:
        inventory={}
        for p in sorted(Path(joint['production_classes']).rglob('*.class')):
            relative=p.relative_to(joint['production_classes']);b=p.read_bytes();q=api/relative;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(b);inventory[str(relative)]=SHA(b)
        receipt['jointClassInventory']=inventory;receipt['jointClassInventorySha256']=SHA(json.dumps(inventory,sort_keys=True,separators=(',',':')).encode())
        for key,path in [('actual014Classes',jar),('androidSdk',sdk),('sqliteJdbc',jdbc),('actualGeneratedR',generated)]:
            if SHA(path.read_bytes())!=support[key]['sha256']:raise RuntimeError('Support changed:'+key)
        shim=[]
        for p in (ROOT/'tools/r31/ddl_host').rglob('*.java'):
            q=snap(p,src/'platform'/p.relative_to(ROOT/'tools/r31/ddl_host'))
            if p.name=='SQLiteDatabase.java':
                text=q.read_text();old='public Cursor rawQuery(String sql,String[] args){log(';assert text.count(old)==1
                text=text.replace(old,'public Cursor rawQuery(String sql,String[] args){if(args!=null)for(int i=0;i<args.length;i++)if(args[i]==null)throw new IllegalArgumentException("the bind value at index "+(i+1)+" is null");log(');q.write_text(text);receipt['strictAndroidNullBindingAdapterSha256']=SHA(text.encode())
            shim.append(q)
        shim.append(snap(ROOT/'tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java',src/'platform/android/database/sqlite/SQLiteException.java'))
        for p in (ROOT/'verification/r932/android-json').rglob('*.java'):shim.append(snap(p,src/'json'/p.relative_to(ROOT/'verification/r932/android-json')))
        adapters={'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000;}}','android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}','android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}'}
        for name,text in adapters.items():q=src/'platform'/name;q.parent.mkdir(parents=True,exist_ok=True);q.write_text(text);shim.append(q)
        receipt['hostOnlyAdapters']=adapters
        run('platform-javac',[jdk/'javac','-encoding','UTF-8','-cp',sdk,'-d',platform,*shim])
        base='app/src/main/java/com/ethk3/novaonepro/'
        origins=[ROOT/base/n for n in ['ResearchPlatformStoreR929.java','ResearchSchemaCoordinatorR926.java']]+[args.domain_root/base/n for n in ['SchemaDomainCoordinatorR926.java','DbStore.java']]
        files=[snap(p,src/'production'/p.name) for p in origins]
        cp=os.pathsep.join(map(str,[api,jar,generated,sdk]));run('production-javac',[jdk/'javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',cp,'-d',current,*files])
        fixture=ROOT/'verification/r31/p02/schema_mainchain/runs/20261104T112314Z/source_snapshot/MainChainSchemaR31Host.java'
        hosts=[snap(fixture,src/fixture.name)]+[snap(ROOT/'tools/r31/platform_readiness_host'/n,src/n) for n in ['R929StartupOrderR31Host.java','R929MetadataR31Host.java']]
        run('host-javac',[jdk/'javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[platform,current,api,jar,generated,sdk,jdbc])),'-d',host,*hosts])
        baseline_cp=os.pathsep.join(map(str,[platform,host,jar,generated,sdk,jdbc]))
        run('baseline-actual014-startup',[jdk/'java','-Xmx512m','-cp',baseline_cp,'com.ethk3.novaonepro.R929StartupOrderR31Host',work/'baseline.db',out/'baseline-sql-api.tsv'])
        current_cp=os.pathsep.join(map(str,[platform,current,host,api,jar,generated,sdk,jdbc]))
        run('current-native-metadata',[jdk/'java','-Xmx512m','-cp',current_cp,'com.ethk3.novaonepro.R929MetadataR31Host',out/'current-sql-api.tsv'])
        changed=[]
        for x in receipt['sources']:
            x['originUnchanged']=SHA(Path(x['origin']).read_bytes())==x['sha256']
            if not x['originUnchanged']:changed.append(x['origin'])
        if changed:raise RuntimeError('Source drift; fixed snapshot only:'+repr(changed))
        receipt['status']='NATIVE_NAMED_STARTUP_GAP_AND_OWNER_METADATA_PASS_NOT_ART'
        receipt['limits']=['Normal actual DbStore constructor/ART context binding is covered by a separate joint HOST/Android test; this tool executes exact schema callbacks and actual Domain owner/capture on native SQLite.','No complete Research READY, APK identity, physical duration or natural quality result is asserted.']
    except Exception as error:receipt['status']='FAILED';receipt['failure']=repr(error);raise
    finally:receipt['finished']=datetime.datetime.now(datetime.timezone.utc).isoformat();save();print('RECEIPT '+str(out/'RECEIPT.json'),flush=True)
if __name__=='__main__':main()
