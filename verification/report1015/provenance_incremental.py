"""Unchanged actual facade + replaced generated class, in separate fresh JVMs."""
from pathlib import Path
import importlib.util,json,subprocess,hashlib,os
R=Path(__file__).resolve().parents[2];W=R.parent;O=W/'audit_work'/os.environ.get('STARCORE_PROVENANCE_OUT','provenance-incremental');O.mkdir(exist_ok=False)
spec=importlib.util.spec_from_file_location('provenance',R/'scripts/r815_generate_provenance.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
J=W/'offline_tools/toolchain/zulu17.62.17-ca-jdk17.0.17-linux_x64/bin';facade=R/'app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java'
main=O/'Read.java';main.write_text('package com.ethk3.novaonepro;public class Read{public static void main(String[]a){if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(a[0]))throw new AssertionError("STALE_GENERATED_IDENTITY:"+SourceManifestR815.SOURCE_TREE_SHA256);}}')
results=[]
for mode in ['shipped_constant','runtime_field']:
 d=O/mode;d.mkdir();cl=d/'classes';cl.mkdir();p=d/'BuildSourceIdentityCP12.java'
 def source(value):
  manifest={'sourceTreeSha256':value*64,'resourceManifestSha256':'c'*64,'toolchainSha256':'d'*64,'generatedAtUtc':'2026-10-15T00:00:00Z'}
  s=m.render(manifest).decode()
  if mode=='shipped_constant':
   import re
   s=re.sub(r'= value\("([^"\n]+)"\);',r'= "\1";',s)
  return s
 p.write_text(source('a'))
 subprocess.run([str(J/'javac'),'-d',str(cl),str(p),str(facade),str(main)],check=True,capture_output=True)
 digest=hashlib.sha256((cl/'com/ethk3/novaonepro/SourceManifestR815.class').read_bytes()).hexdigest()
 p.write_text(source('b'))
 subprocess.run([str(J/'javac'),'-d',str(cl),str(p)],check=True,capture_output=True)
 z=subprocess.run([str(J/'java'),'-cp',str(cl),'com.ethk3.novaonepro.Read','b'*64],capture_output=True,text=True)
 assert hashlib.sha256((cl/'com/ethk3/novaonepro/SourceManifestR815.class').read_bytes()).hexdigest()==digest,'FACADE_RECOMPILED'
 assert (z.returncode==0)==(mode=='runtime_field'),z.stderr
 results.append({'mode':mode,'exit_code':z.returncode,'unchanged_facade_sha256':digest,'result':'CURRENT_IDENTITY_READ'if z.returncode==0 else 'OLD_DEFECT_REPRODUCED','diagnostic':z.stderr.strip()})
(O/'result.json').write_text(json.dumps({'status':'PASS','scope':'ACTUAL_RENDERER_AND_FACADE_TWO_GENERATIONS_NO_FACADE_RECOMPILE','runs':results},indent=2))
print('PASS original stale identity reproduced; repaired runtime fields propagate without facade recompilation')
