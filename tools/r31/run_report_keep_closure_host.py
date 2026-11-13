"""Actual keep row codec, snapshot registry, report descriptor/copy and file-pin protocol on native SQLite. No Android Service or SAF claim."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser()
parser.add_argument("--out",type=Path,required=True)
parser.add_argument("--classes",type=Path,required=True)
parser.add_argument("--export-root",type=Path,default=ROOT)
parser.add_argument("--keep-root",type=Path,default=ROOT)
parser.add_argument("--overlay-classes",type=Path)
parser.add_argument("--gc-root",type=Path,default=ROOT)
args=parser.parse_args();out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
sdk=Path(os.environ["ANDROID_HOME"])/"platforms/android-36/android.jar";jdbc=Path(os.environ.get("SQLITE_JDBC","/tmp/r31-baseline-deps/sqlite-jdbc.jar"))
def sha(path):
    if path.is_file():return hashlib.sha256(path.read_bytes()).hexdigest()
    value=hashlib.sha256()
    for child in sorted(path.rglob("*.class")):
        value.update(str(child.relative_to(path)).encode());value.update(b"\0");value.update(hashlib.sha256(child.read_bytes()).digest())
    return value.hexdigest()
extra_classes=([args.overlay_classes] if args.overlay_classes else [])+[args.classes]
runner_source=Path(__file__).resolve()
runner_before=sha(runner_source)
receipt={"scope":"ACTUAL_KEEP_CODEC_REGISTRY_DESCRIPTOR_GENERIC_COPY_INPUT_PIN_NATIVE_NOT_ANDROID_OR_SAF","sourceTreeMode":"SINGLE_CURRENT_SOURCE_TREE" if not args.overlay_classes and all(p.resolve()==ROOT for p in [args.export_root,args.keep_root,args.gc_root]) else "EXPLICIT_MIXED_SOURCE_PRECHECK_NOT_FINAL_GATE","runnerSha256":runner_before,"sourceCapturedAt":datetime.datetime.now(datetime.timezone.utc).isoformat(),"head":subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip(),"classes":str(args.classes),"classesSha256":sha(args.classes),"overlayClassesSha256":sha(args.overlay_classes) if args.overlay_classes else None,"androidJarSha256":sha(sdk),"sqliteJdbcSha256":sha(jdbc),"records":[],"status":"IN_PROGRESS"}
environment=dict(os.environ);environment.pop("JAVA_TOOL_OPTIONS",None)
def save(): (out/"RECEIPT.json").write_text(json.dumps(receipt,indent=2)+"\n")
def run(command,label):
    command=list(map(str,command));started=datetime.datetime.now(datetime.timezone.utc).isoformat()
    try:result=subprocess.run(command,cwd=ROOT,env=environment,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
    except subprocess.TimeoutExpired as failure:
        raw=failure.stdout or b"";(out/(label+".log")).write_text(raw.decode(errors="replace") if isinstance(raw,bytes) else raw);receipt["status"]="FAILED_TIMEOUT";receipt["records"].append({"label":label,"command":command,"start":started,"timeoutSeconds":90});save();raise
    (out/(label+".log")).write_text(result.stdout);receipt["records"].append({"label":label,"command":command,"startedAt":started,"finishedAt":datetime.datetime.now(datetime.timezone.utc).isoformat(),"exitCode":result.returncode});save();print(label,"exit="+str(result.returncode),flush=True)
    if result.returncode:receipt["status"]="FAILED";save();print(result.stdout[-16000:],flush=True);raise RuntimeError(label+" failed; original error preserved")
save()
with tempfile.TemporaryDirectory(prefix="r31-report-keep-native-") as temporary:
    tmp=Path(temporary);platform=tmp/"platform";production=tmp/"production";test=tmp/"test";generated=tmp/"platform-source";fixture=tmp/"fixture"
    for p in [platform,production,test,generated,fixture]:p.mkdir()
    shims=[]
    for source in sorted((ROOT/"tools/r29/qual_host/shim").rglob("*.java")):
        text=source.read_text();target=generated/source.relative_to(ROOT/"tools/r29/qual_host/shim");target.parent.mkdir(parents=True,exist_ok=True)
        if source.name=="SQLiteDatabase.java":
            text=text.replace("public class SQLiteDatabase {","public class SQLiteDatabase implements AutoCloseable {private String directPath;public static int readonlyWriteAttempts;")
            text=text.replace("public void execSQL(String sql, Object[] args) {","public void execSQL(String sql, Object[] args) {if(isReadOnly())readonlyWriteAttempts++;")
            at=text.index("    static final class Rows")
            text=text[:at]+'''    public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;public interface CursorFactory{}
    public static SQLiteDatabase openDatabase(String path,CursorFactory ignored,int flags){try{org.sqlite.SQLiteConfig config=new org.sqlite.SQLiteConfig();config.setReadOnly((flags&OPEN_READONLY)!=0);SQLiteDatabase db=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+path,config.toProperties()));db.directPath=new java.io.File(path).getCanonicalPath();return db;}catch(java.io.IOException|java.sql.SQLException error){throw new SQLiteException(error.getMessage());}}
    public String getPath(){if(directPath==null)throw new IllegalStateException("HOST_EXPLICIT_PATH_REQUIRED");return directPath;}
    public boolean isClosed(){try{return jdbc.isClosed();}catch(java.sql.SQLException error){throw new SQLiteException(error.getMessage());}}
    public void close(){try{jdbc.close();}catch(java.sql.SQLException error){throw new SQLiteException(error.getMessage());}}
''' + text[at:]
        target.write_text(text);shims.append(target)
    extras={
        "android/os/Process.java":"package android.os;public final class Process{public static int myPid(){return Math.toIntExact(ProcessHandle.current().pid());}}",
        "android/os/SystemClock.java":"package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000;}}",
        "android/system/ErrnoException.java":"package android.system;public class ErrnoException extends Exception{public ErrnoException(String operation,int code){super(operation+\":\"+code);}}",
        "android/system/OsConstants.java":"package android.system;public final class OsConstants{public static final int O_RDONLY=0;}",
        "android/system/Os.java":'''package android.system;public final class Os{private static final java.util.Map<java.io.FileDescriptor,java.nio.channels.FileChannel> files=new java.util.IdentityHashMap<>();public static synchronized java.io.FileDescriptor open(String path,int flags,int mode)throws ErrnoException{try{java.io.FileDescriptor key=new java.io.FileDescriptor();files.put(key,java.nio.channels.FileChannel.open(java.nio.file.Paths.get(path),java.nio.file.StandardOpenOption.READ));return key;}catch(java.io.IOException error){throw new ErrnoException(error.toString(),1);}}public static synchronized void fsync(java.io.FileDescriptor key)throws ErrnoException{try{java.nio.channels.FileChannel channel=files.get(key);if(channel==null)throw new java.io.IOException("UNKNOWN_HOST_DESCRIPTOR");channel.force(true);}catch(java.io.IOException error){throw new ErrnoException(error.toString(),1);}}public static synchronized void close(java.io.FileDescriptor key)throws ErrnoException{try{java.nio.channels.FileChannel channel=files.remove(key);if(channel!=null)channel.close();}catch(java.io.IOException error){throw new ErrnoException(error.toString(),1);}}}'''
    }
    for name,text in extras.items():target=generated/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text);shims.append(target)
    cp=os.pathsep.join(map(str,[*extra_classes,sdk,jdbc]));run(["javac","-encoding","UTF-8","-cp",cp,"-d",platform,*shims],"compile-real-host-platform")
    json_sources=sorted((ROOT/"verification/r932/android-json").rglob("*.java"))
    receipt["androidJsonSourceSha256"]={str(p):sha(p) for p in json_sources}
    run(["javac","-encoding","UTF-8","-cp",sdk,"-d",platform,*json_sources],"compile-reviewed-android-json")
    source_base="app/src/main/java/com/ethk3/novaonepro"
    sources=[args.export_root/source_base/name for name in ["ReportAttachmentClosureCP12.java","ReportExportServiceCP4.java","ReportInputsLeaseCP12.java"]]
    sources.extend(args.keep_root/source_base/"strategyv2"/name for name in ["KeepMigrationStoreR31.java","KeepMigrationR31.java","KeepMismatchReferenceR31.java","ResearchRetentionStoreR30.java"])
    sources.extend(args.gc_root/source_base/"strategyv2"/name for name in ["CheckpointGcStoreR31.java","CheckpointScratchGcR31.java","CheckpointArtifactsR26.java","CheckpointManifestR26.java","CheckpointReferencesR26.java","ModelPublicationR31.java"])
    sources.append(args.gc_root/source_base/"StorageGcR31.java")

    snapshot=out/"source-snapshot";snapshot.mkdir();captured=[];receipt["sourceSha256"]={}
    for source in sources:
        target=snapshot/source.name;target.write_bytes(source.read_bytes());captured.append(target);receipt["sourceSha256"][str(source)]=sha(target)
    run(["javac","-encoding","UTF-8","-sourcepath","","-implicit:none","-cp",os.pathsep.join(map(str,[*extra_classes,sdk])),"-d",production,*captured],"compile-actual-export-pin-keep-gc")
    runtime=os.pathsep.join(map(str,[platform,production,test,*extra_classes,jdbc,sdk]));host=ROOT/"tools/r31/report_cas_host/R31ReportKeepClosureHost.java";receipt["hostSourceSha256"]=sha(host)
    run(["javac","-encoding","UTF-8","-cp",runtime,"-d",test,host],"compile-native-scope-test")
    run(["java","-Xmx512m","-cp",runtime,"com.ethk3.novaonepro.R31ReportKeepClosureHost",fixture,out],"actual-keep-closure-readback-negative-cases")
    receipt["sourceClassesSha256"]={str(p.relative_to(production)):sha(p) for p in sorted(production.rglob("*.class"))};receipt["hostPlatformSourceSha256"]={str(p.relative_to(generated)):sha(p) for p in shims};receipt["sourceOriginsUnchanged"]=all(sha(Path(p))==value for p,value in receipt["sourceSha256"].items())
    receipt["hostSourceUnchanged"]=sha(host)==receipt["hostSourceSha256"]
    receipt["androidJsonSourcesUnchanged"]=all(sha(Path(p))==value for p,value in receipt["androidJsonSourceSha256"].items())
    if not receipt["sourceOriginsUnchanged"] or not receipt["hostSourceUnchanged"] or not receipt["androidJsonSourcesUnchanged"] or sha(runner_source)!=runner_before or sha(args.classes)!=receipt["classesSha256"] or (args.overlay_classes and sha(args.overlay_classes)!=receipt["overlayClassesSha256"]):
        receipt["status"]="FAILED_INPUT_CHANGED_NOT_PASS";save();raise RuntimeError("Bound actual source/classes changed; no final coherent receipt")
receipt["status"]="TARGETED_NATIVE_SCOPE_ASSERTIONS_COMPLETED_NOT_DEVICE";receipt["finishedAt"]=datetime.datetime.now(datetime.timezone.utc).isoformat();save();print("R31_REPORT_KEEP_CLOSURE_HOST_COMPLETED",out,flush=True)
