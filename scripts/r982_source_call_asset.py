#!/usr/bin/env python3
"""Materialize the lossless compiled call index and reject stale source bindings.

--freeze --classes DIR refreshes the archive from freshly compiled product classes.
Current builds use --from-classes after project compilation. The historical default
mode still verifies its exact frozen archive; no current build rewrites that archive.
"""
import argparse, gzip, hashlib, json, os, subprocess, sys, tempfile, importlib.util, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARCHIVE = ROOT / 'release/r982_source_calls.tsv.gz'
PIN = ROOT / 'release/r982_source_calls.manifest.json'
OUTPUT = ROOT / 'app/build/generated/assets/r982/research/r975_source_calls.tsv'

def source_binding(path):
    wanted = {f.relative_to(ROOT).as_posix() for f in (ROOT/'app/src/main/java').rglob('*.java')}
    seen = set()
    with path.open(encoding='utf-8') as lines:
        for line in lines:
            if not line.startswith('FILE\t'): continue
            fields=line.rstrip('\n').split('\t'); name, expected=fields[1:3]
            if name not in wanted or name in seen: raise SystemExit('SOURCE_CALL_INDEX_FILE_SET_DRIFT:'+name)
            seen.add(name)
            if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=expected: raise SystemExit('SOURCE_CALL_INDEX_STALE:'+name)
    if seen!=wanted: raise SystemExit('SOURCE_CALL_INDEX_MISSING_FILES:'+str(sorted(wanted-seen)))

def sha_file(path):
    h=hashlib.sha256()
    with Path(path).open('rb') as f:
        while block:=f.read(1<<20):h.update(block)
    return h.hexdigest()

def need(ok,reason):
    if not ok:raise ValueError(reason)

def class_state(directories,jars):
    state={}
    for i,directory in enumerate(directories):
        need(directory.is_dir() and not directory.is_symlink(),'CLASS_DIRECTORY_REQUIRED')
        for f in sorted(directory.rglob('*.class')):
            need(f.is_file() and not f.is_symlink(),'REGULAR_CLASS_REQUIRED')
            state['directory:'+str(i)+':'+f.relative_to(directory).as_posix()]=sha_file(f)
    for i,archive in enumerate(jars):
        need(archive.is_file() and not archive.is_symlink(),'CLASS_JAR_REQUIRED')
        state['jar:'+str(i)]=sha_file(archive)
    return state

def from_classes(args):
    output=Path(args.output).absolute()
    need(output.resolve().is_relative_to((ROOT/'app/build').resolve()),'GENERATED_OUTPUT_MUST_BE_INSIDE_APP_BUILD')
    directories=[Path(p).absolute() for p in args.class_directory];jars=[Path(p).absolute() for p in args.class_jar]
    need(directories or jars,'ACTUAL_PROJECT_CLASSES_REQUIRED')
    need(len(directories)+len(jars)<=128,'CLASS_INPUT_COUNT_BOUND')
    need(len({str(p.resolve()) for p in directories+jars})==len(directories+jars),'DUPLICATE_CLASS_INPUT_LOCATION')
    # The existing report contract inventories main Java sources. A future Kotlin
    # production source set needs an explicit consumer/schema revision, not silent exclusion.
    need(not any((ROOT/'app/src/main').rglob('*.kt')),'KOTLIN_SOURCE_INDEX_CONTRACT_NOT_REGISTERED')
    sources={f.relative_to(ROOT).as_posix():sha_file(f) for f in sorted((ROOT/'app/src/main/java').rglob('*.java'))}
    need(sources,'EMPTY_PRODUCT_SOURCE_SET')
    manifest_path=ROOT/'app/build/generated/r815/source-manifest.json'
    manifest_bytes=manifest_path.read_bytes();manifest=json.loads(manifest_bytes)
    expected={r['path']:r['sha256'] for r in manifest['inputs']['java']}
    facade='app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java'
    for row in manifest['inputs']['buildContracts']:
        need(sha_file(ROOT/row['path'])==row['sha256'],'R815_BUILD_CONTRACT_STALE:'+row['path'])
        if row['path']==facade:expected[facade]=row['sha256']
    need(expected==sources,'R815_SOURCE_PROVENANCE_STALE')
    tree=hashlib.sha256()
    for name,h in sorted(sources.items()):
        if name!=facade:tree.update(('PATH='+name+'\nSHA256='+h+'\n').encode())
    need(manifest.get('format')=='CP12_BUILD_SOURCE_PROVENANCE_V1'
         and tree.hexdigest()==manifest.get('sourceTreeSha256')
         and hashlib.sha256(json.dumps(manifest['inputs'],ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()).hexdigest()==manifest.get('buildInputsSha256'),
         'R815_IDENTITY_DIGEST_MISMATCH')
    for row in manifest['inputs']['generatedJava']:
        need(sha_file(ROOT/row['path'])==row['sha256'],'R815_GENERATED_SEMANTICS_STALE')
    state=class_state(directories,jars)
    parser_path=ROOT/'scripts/r975_source_call_index.py'
    spec=importlib.util.spec_from_file_location('r335_class_parser',parser_path);parser=importlib.util.module_from_spec(spec);spec.loader.exec_module(parser)
    records={key:[] for key in sources};seen=set();covered=set();classes=0;method_count=0;calls=0;ignored=0;total=0
    class Bytes:
        def __init__(self,body):self.body=body
        def read_bytes(self):return self.body
    def consume(body):
        nonlocal classes,method_count,calls,ignored,total
        total+=len(body);need(len(body)<=16*1024*1024 and total<=512*1024*1024,'COMPILED_CLASS_BYTES_BOUND')
        name,source,methods=parser.methods(Bytes(body));need(name not in seen,'DUPLICATE_COMPILED_CLASS:'+name);seen.add(name)
        key='app/src/main/java/'+name.rsplit('/',1)[0]+'/'+str(source)
        if source is None or key not in records:ignored+=1;return
        classes+=1;covered.add(key)
        for nm,desc,flags,lo,hi,edges in methods:
            method_count+=1;calls+=len(edges)
            records[key].append((name,nm,desc,str(lo),str(hi),'SYNTHETIC' if flags&0x1000 else 'DECLARED',';'.join(edges)))
    for directory in directories:
        for f in sorted(directory.rglob('*.class')):consume(f.read_bytes())
    for archive in jars:
        with zipfile.ZipFile(archive) as z:
            names=[x.filename for x in z.infolist()];need(len(names)==len(set(names)),'DUPLICATE_JAR_MEMBER')
            for entry in sorted(z.infolist(),key=lambda x:x.filename):
                if not entry.filename.endswith('.class'):continue
                name=Path(entry.filename);need(not name.is_absolute() and '..' not in name.parts and not entry.filename.startswith('META-INF/versions/'),'UNSUPPORTED_CLASS_JAR_MEMBER')
                need(entry.file_size<=16*1024*1024,'CLASS_JAR_MEMBER_BOUND');consume(z.read(entry))
    missing=sorted(set(records)-covered)
    need(not missing,'SOURCE_WITHOUT_COMPILED_CLASSES:'+str(missing))
    need(state==class_state(directories,jars),'CLASS_BYTES_CHANGED_DURING_INDEX')
    need(sources=={f.relative_to(ROOT).as_posix():sha_file(f) for f in sorted((ROOT/'app/src/main/java').rglob('*.java'))},'SOURCE_BYTES_CHANGED_DURING_INDEX')
    need(manifest_path.read_bytes()==manifest_bytes,'R815_MANIFEST_CHANGED_DURING_INDEX')
    output.parent.mkdir(parents=True,exist_ok=True);handle,temp=tempfile.mkstemp(dir=output.parent,prefix='source-calls-',suffix='.partial')
    scope='HOST_TYPE_FIXTURE_STATIC_CALLS_NOT_ANDROID_APK' if args.host_fixture else 'CURRENT_COMPILED_PROJECT_CLASSES_STATIC_CALLS_NOT_RUNTIME_COVERAGE'
    try:
        with os.fdopen(handle,'w',encoding='utf-8',newline='\n') as stream:
            stream.write('# STATIC_COMPILED_METHOD_CALLS_V1|NOT_RUNTIME_COVERAGE|virtual/reflection/native-dispatch=UNRESOLVED\n')
            stream.write('# R335_SCOPE='+scope+'\n')
            stream.write('record\tfile\tsource_sha256\tclass\tmethod\tdescriptor\tfirst_executable_line\tlast_executable_line\tmethod_kind\tdirect_calls\truntime_status\n')
            for key,rows in records.items():
                h=sources[key];stream.write('\t'.join(['FILE',key,h,'','','','','','','','RUNTIME_NOT_INSTRUMENTED_UNLESS_STAGE_RECEIPT_PRESENT'])+'\n')
                for row in sorted(rows):stream.write('\t'.join(['METHOD',key,h,*row,'RUNTIME_NOT_INSTRUMENTED_UNLESS_STAGE_RECEIPT_PRESENT'])+'\n')
            stream.flush();os.fsync(stream.fileno())
        source_binding(Path(temp));os.replace(temp,output)
    finally:
        if os.path.exists(temp):os.unlink(temp)
    receipt={'schema':'STARCORE_R335_COMPILED_SOURCE_CALL_PROVENANCE_1','scope':scope,'sourceTreeSha256':manifest['sourceTreeSha256'],'sourceBuildInputsSha256':manifest['buildInputsSha256'],'r815ManifestSha256':hashlib.sha256(manifest_bytes).hexdigest(),'sourceFiles':len(sources),'projectClassesForMainJava':classes,'generatedOrUnregisteredClassCount':ignored,'methods':method_count,'directInvocations':calls,'indexSha256':sha_file(output),'indexBytes':output.stat().st_size,'parserSha256':sha_file(parser_path),'classInputSha256':hashlib.sha256(json.dumps(state,sort_keys=True,separators=(',',':')).encode()).hexdigest(),'sourceSetScope':'EXACT_MAIN_JAVA_SOURCE_FILES; GENERATED_CLASSES_SEPARATELY_COUNTED','runtimeCoverage':'NOT_CLAIMED','qualification':'NOT_GRANTED'}
    output.with_suffix('.provenance.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(receipt,ensure_ascii=False))

def main():
    p=argparse.ArgumentParser();p.add_argument('--freeze',action='store_true');p.add_argument('--classes',type=Path);p.add_argument('--from-classes',action='store_true');p.add_argument('--class-directory',action='append',default=[]);p.add_argument('--class-jar',action='append',default=[]);p.add_argument('--output');p.add_argument('--host-fixture',action='store_true');a=p.parse_args()
    if a.from_classes:
        need(a.output is not None and not a.freeze and a.classes is None,'CURRENT_CLASS_MODE_ARGUMENTS');from_classes(a);return
    need(not a.class_directory and not a.class_jar and a.output is None and not a.host_fixture,'LEGACY_FROZEN_MODE_ARGUMENTS')
    OUTPUT.parent.mkdir(parents=True,exist_ok=True)
    if a.freeze:
        if not a.classes: p.error('--freeze requires --classes')
        subprocess.run([sys.executable,str(ROOT/'scripts/r975_source_call_index.py'),'--classes',str(a.classes)],check=True)
        original=ROOT/'app/src/main/assets/research/r975_source_calls.tsv'
        source_binding(original)
        with original.open('rb') as src, ARCHIVE.open('wb') as raw, gzip.GzipFile(filename='',mode='wb',fileobj=raw,mtime=0) as out:
            while block:=src.read(65536):out.write(block)
        digest=hashlib.sha256();count=0
        with original.open('rb') as src:
            while block:=src.read(65536):digest.update(block);count+=len(block)
        PIN.write_text(json.dumps({'sha256':digest.hexdigest(),'bytes':count,'scope':'STATIC_COMPILED_CALLS_NOT_RUNTIME_COVERAGE'},indent=2)+'\n')
        original.unlink()
    pin=json.loads(PIN.read_text());digest=hashlib.sha256();count=0
    handle,temporary=tempfile.mkstemp(dir=OUTPUT.parent,prefix='source-calls-',suffix='.partial')
    try:
        with os.fdopen(handle,'wb') as out, gzip.open(ARCHIVE,'rb') as src:
            while block:=src.read(65536):out.write(block);digest.update(block);count+=len(block)
            out.flush();os.fsync(out.fileno())
        if digest.hexdigest()!=pin['sha256'] or count!=pin['bytes']:raise SystemExit('SOURCE_CALL_INDEX_ARCHIVE_DRIFT')
        source_binding(Path(temporary));os.replace(temporary,OUTPUT)
    finally:
        if os.path.exists(temporary):os.unlink(temporary)
    print('SOURCE_CALL_ASSET_VERIFIED|bytes='+str(count)+'|sha256='+pin['sha256'])

if __name__=='__main__':main()
