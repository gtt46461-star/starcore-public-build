"""Current report + actual published History scalar, native HOST SQLite; never Android/device acceptance."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser()
parser.add_argument("--out",type=Path,required=True)
default_classes=ROOT/"app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
parser.add_argument("--classes",type=Path,default=Path(os.environ.get("R31_HOST_CLASSES",str(default_classes))))
parser.add_argument("--producer-support",type=Path,help="Optional fixed real compiled producer dependency snapshot for pre-integration HOST compile; not a replacement implementation")
args=parser.parse_args()
out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
sdk=Path(os.environ["ANDROID_HOME"])/"platforms/android-36/android.jar"
jdbc=Path(os.environ.get("SQLITE_JDBC","/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
def digest(path):
    if path.is_file():return hashlib.sha256(path.read_bytes()).hexdigest()
    if not path.is_dir():raise FileNotFoundError(str(path))
    value=hashlib.sha256()
    for child in sorted(path.rglob("*.class")):
        value.update(str(child.relative_to(path)).encode());value.update(b"\0");value.update(hashlib.sha256(child.read_bytes()).digest())
    return value.hexdigest()
receipt={"scope":"ACTUAL_REPORT_HISTORY_SCALAR_NATIVE_HOST_ONLY_NOT_ANDROID_NOT_IMPORTER_E2E_NOT_QUALITY","sourceCapturedAt":datetime.datetime.now(datetime.timezone.utc).isoformat(),"head":subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip(),"classes":str(args.classes),"classesSha256":digest(args.classes),"jdbcSha256":digest(jdbc),"androidSdkSha256":digest(sdk),"records":[],"status":"IN_PROGRESS"}
if args.producer_support:receipt["producerDependencySnapshot"]={"path":str(args.producer_support),"classTreeSha256":digest(args.producer_support),"scope":"Fixed actual production dependency bytecode; current History and ReportSnapshot are separately recompiled from recorded source bytes"}
environment=dict(os.environ);environment.pop("JAVA_TOOL_OPTIONS",None)
def save(): (out/"RECEIPT.json").write_text(json.dumps(receipt,indent=2)+"\n")
def run(command,label):
    command=list(map(str,command));result=subprocess.run(command,cwd=ROOT,env=environment,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
    (out/(label+".log")).write_text(result.stdout);receipt["records"].append({"label":label,"command":command,"exitCode":result.returncode});save();print(label,"exit="+str(result.returncode),flush=True)
    if result.returncode:receipt["status"]="FAILED";save();print(result.stdout[-12000:],flush=True);raise RuntimeError(label+" failed; original log retained")
save()
with tempfile.TemporaryDirectory(prefix="r31-report-history-reader-") as temporary:
    tmp=Path(temporary);platform=tmp/"platform";current=tmp/"current";old=tmp/"old";test=tmp/"test";generated=tmp/"platform-source"
    for path in [platform,current,old,test,generated]:path.mkdir()
    shims=[]
    for source in sorted((ROOT/"tools/r29/qual_host/shim").rglob("*.java")):
        target=generated/source.relative_to(ROOT/"tools/r29/qual_host/shim");target.parent.mkdir(parents=True,exist_ok=True);text=source.read_text()
        if source.name=="SQLiteDatabase.java":
            text=text.replace("public class SQLiteDatabase {","public class SQLiteDatabase {public static java.util.function.BiConsumer<SQLiteDatabase,String> readObserver;private boolean successful;")
            text=text.replace("    public Cursor rawQuery(String sql, String[] args) {","    public Cursor rawQuery(String sql, String[] args) {if(readObserver!=null)readObserver.accept(this,sql);")
            at=text.index("    static final class Rows")
            text=text[:at]+'''    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();jdbc.setAutoCommit(true);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
''' +text[at:]
        target.write_text(text);shims.append(target)
    extras={"android/os/Process.java":"package android.os;public final class Process{public static int myPid(){return Math.toIntExact(ProcessHandle.current().pid());}}", "android/os/SystemClock.java":"package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}}"}
    for name,text in extras.items():target=generated/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text);shims.append(target)
    dependencies=([args.producer_support] if args.producer_support else [])+[args.classes,sdk]
    cp=os.pathsep.join(map(str,dependencies))
    run(["javac","-encoding","UTF-8","-cp",cp,"-d",platform,*shims],"compile-host-platform")
    sources=[ROOT/"app/src/main/java/com/ethk3/novaonepro/strategyv2"/name for name in ["ReportSnapshotR30.java","HistoryImportR27.java"]]
    snapshot=out/"source-snapshot";snapshot.mkdir();receipt["sourceSha256"]={}
    prepared=[]
    for source in sources:
        target=snapshot/source.name;target.write_bytes(source.read_bytes());receipt["sourceSha256"][str(source.relative_to(ROOT))]=digest(target);prepared.append(target)
    run(["javac","-encoding","UTF-8","-cp",cp,"-d",current,*prepared],"compile-actual-production")
    reference=ROOT/"tools/r31/report_history_host/baseline/ReportSnapshotR30.java.txt";origin=json.loads((reference.parent/"ORIGIN.json").read_text());need_hash=digest(reference)
    if need_hash!=origin["sha256"]:raise RuntimeError("Actual previous report source mismatch")
    original=tmp/"old-source/ReportSnapshotR30.java";original.parent.mkdir();original.write_bytes(reference.read_bytes());receipt["baselineOrigin"]=origin
    run(["javac","-encoding","UTF-8","-cp",os.pathsep.join(map(str,[current,*dependencies])),"-d",old,original],"compile-actual-original-report")
    nativecp=os.pathsep.join(map(str,[platform,current,test,*dependencies,jdbc]));host=ROOT/"tools/r31/report_history_host/R31ReportHistoryReaderHost.java";receipt["hostTestSourceSha256"]=digest(host)
    run(["javac","-encoding","UTF-8","-cp",nativecp,"-d",test,host],"compile-native-reader-test")
    run(["java","-Xmx512m","-cp",os.pathsep.join(map(str,[platform,old,current,test,*dependencies,jdbc])),"com.ethk3.novaonepro.strategyv2.R31ReportHistoryReaderHost","baseline",out],"baseline-actual-report")
    run(["java","-Xmx512m","-cp",nativecp,"com.ethk3.novaonepro.strategyv2.R31ReportHistoryReaderHost","current",out],"current-report-native")
    receipt["compiledSourceClassSha256"]={str(p.relative_to(current)):digest(p) for p in sorted(current.rglob("*.class"))};receipt["hostShimSourceSha256"]={str(p.relative_to(generated)):digest(p) for p in shims}
    receipt["originalSourcesUnchanged"] = all(digest(ROOT/name)==value for name,value in receipt["sourceSha256"].items())
    if not receipt["originalSourcesUnchanged"] or digest(args.classes)!=receipt["classesSha256"]:raise RuntimeError("Source or complete app dependencies changed during bound run")
receipt["status"]="TARGETED_NATIVE_READER_ASSERTIONS_COMPLETED_NOT_DEVICE";receipt["finishedAt"]=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
print("R31_REPORT_HISTORY_READER_HOST_COMPLETED",out,flush=True)
