#!/usr/bin/env python3
"""Execute actual product DDL through project API adapters; product source read-only.
Usage: python run_schema_capture.py --source SOURCE_ROOT --android-jar android.jar --out EMPTY_DIR [--after-root CP4_DB_DESIGN_AFTER]
"""
import argparse,subprocess,json,hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--android-jar',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--after-root',type=Path);p.add_argument('--product-classes',type=Path);p.add_argument('--resource-jar',type=Path);a=p.parse_args()
r=a.source.resolve();o=a.out.resolve();h=Path(__file__).resolve().parent
if o.exists() and any(o.iterdir()):raise SystemExit('OUT_MUST_BE_EMPTY_TO_PREVENT_STALE_CLASS_REUSE')
o.mkdir(parents=True,exist_ok=True);cl=o/'classes';cl.mkdir();j=a.android_jar.resolve()
compiler=o/'Compile.java';compiler.write_text('import javax.tools.ToolProvider;public class Compile{public static void main(String[] a){System.exit(ToolProvider.getSystemJavaCompiler().run(null,System.out,System.err,a));}}')
source=list(sorted((r/'app/src/main/java').rglob('*.java')))+list(sorted((r/'app/build/generated/source/r967').rglob('*.java')))+list(sorted((r/'app/build/generated/source/r815').rglob('*.java')))+[r/'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java',r/'verification/r36-stubs/com/ethk3/novaonepro/R.java']
if a.resource_jar:
 if not a.resource_jar.is_file():raise SystemExit('ACTUAL_RESOURCE_JAR_REQUIRED')
 source=[f for f in source if f != r/'verification/r36-stubs/com/ethk3/novaonepro/R.java']
if a.after_root:
 ar=a.after_root.resolve();source=[ar/f.relative_to(r) if (ar/f.relative_to(r)).exists()else f for f in source]
 known={str(f.relative_to(ar) if f.is_relative_to(ar) else f.relative_to(r)) for f in source}
 source += [f for f in sorted(ar.rglob('*.java')) if str(f.relative_to(ar)) not in known and (str(f.relative_to(ar)).startswith('app/src/main/java/') or str(f.relative_to(ar)).startswith('app/build/generated/source/r967/') or str(f.relative_to(ar)).startswith('app/build/generated/source/r815/'))]
def run(name,cmd):
 with (o/(name+'.log')).open('w')as f:
  q=subprocess.run(list(map(str,cmd)),stdout=f,stderr=subprocess.STDOUT)
 if q.returncode:raise SystemExit(name+' FAILED '+str(q.returncode))
classpath=str(cl)+':'+str(j)
if a.resource_jar:classpath+=':'+str(a.resource_jar.resolve())
if a.product_classes:
 if a.after_root:raise SystemExit('PRODUCT_CLASSES_CANNOT_USE_SOURCE_OVERLAY')
 if not a.product_classes.is_dir() or not a.resource_jar or not a.resource_jar.is_file():raise SystemExit('FRESH_ANDROID_PRODUCT_AND_RESOURCE_CLASSES_REQUIRED')
 classpath+=':'+str(a.product_classes.resolve())
def compile(name,files):
 arg=o/(name+'.sources');arg.write_text('\n'.join('"'+str(x)+'"'for x in files))
 run(name,['java','-Xmx1g',compiler,'-encoding','UTF-8','-cp',classpath,'-d',cl,'@'+str(arg)])
if not a.product_classes:compile('product_compile',source)
ad=[f for f in (r/'verification/r974/android').rglob('*.java')if f.name not in ('SQLiteDatabase.java','Looper.java')]+list((r/'verification/r932/android-json').rglob('*.java'))+[r/'verification/r967/host/android/content/ContentValues.java',r/'verification/r967/host/android/os/SystemClock.java',r/'verification/r967/host/android/os/CancellationSignal.java',r/'verification/r974/HostProductR974.java',h/'SQLiteDatabase.java',h/'Looper.java',h/'SchemaCaptureCP4.java']
compile('adapters_compile',ad)
run('schema_capture',['java','-cp',classpath,'com.ethk3.novaonepro.SchemaCaptureCP4',h/'sqlite_bridge.py',o/'schema.db'])
(o/'host-classpath').write_text(classpath+'\n')
inputs={str(f.relative_to(r)):hashlib.sha256(f.read_bytes()).hexdigest()for f in source if f.is_relative_to(r)}
product={str(f.relative_to(a.product_classes)):hashlib.sha256(f.read_bytes()).hexdigest()for f in sorted(a.product_classes.rglob('*.class'))}if a.product_classes else {}
(o/'CURRENT_BYTECODE_BINDING.json').write_text(json.dumps({'sources':inputs,'android_product_classes':product,'resource_jar_sha256':hashlib.sha256(a.resource_jar.read_bytes()).hexdigest()if a.resource_jar else None,'scope':'Current production bytecode with host Android API adapters; not Android runtime/device'},indent=2))
import sqlite3
c=sqlite3.connect(o/'schema.db');rows=c.execute("select type,name,tbl_name,sql from sqlite_master where sql is not null and name not like 'sqlite_%' order by type,name").fetchall()
(o/'schema.json').write_text(json.dumps(rows,indent=2));(o/'receipt.json').write_text(json.dumps({'scope':'actual_source_DDL_real_SQLite_host_API_adapter_NOT_device','integrity':c.execute('pragma integrity_check').fetchall(),'foreign_keys':c.execute('pragma foreign_key_check').fetchall(),'objects':dict(c.execute("select type,count(*) from sqlite_master where name not like 'sqlite_%'group by type").fetchall()),'java_sources':len(source)},indent=2))
print((o/'receipt.json').read_text())
