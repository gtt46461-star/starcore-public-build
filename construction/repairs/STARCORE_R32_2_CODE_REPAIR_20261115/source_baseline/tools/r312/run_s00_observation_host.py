#!/usr/bin/env python3
"""Re-execute S00 observation contracts without touching historical receipts."""
import argparse,hashlib,importlib.util,json,os,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def load(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--kind',choices=['evidence','transaction'],required=True);a=p.parse_args();head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
 if a.kind=='evidence':
  module=load('s00_actual_original_observation',ROOT/'tools/r31/verify_runtime_observation.py');module.OUT=ROOT/'app/build/r312/s00-observation-evidence';module.OUT.mkdir(parents=True,exist_ok=True);module.main();print(json.dumps({'s00_kind':a.kind,'source_head':head,'historical_receipts_modified':False}));return 0
 sys.path.insert(0,str(ROOT/'tools/r31'));from host_tool_support import dependencies
 classes,sdk,resources,jdbc=dependencies(ROOT);support_path=os.environ.get('R312_ACTUAL_HOST_SUPPORT_RECEIPT')
 if not support_path:raise ValueError('CURRENT_GATE_SUPPORT_RECEIPT_REQUIRED_NO_OLD_ARTIFACT_FALLBACK')
 support=json.loads(Path(support_path).read_text());assert support['source_head']==head,'CURRENT_SUPPORT_SOURCE_HEAD_MISMATCH';assert support['classes_jar_sha256']==hashlib.sha256(classes.read_bytes()).hexdigest(),'CURRENT_SUPPORT_CLASSES_SHA_MISMATCH'
 portable=load('s00_android_boundaries',ROOT/'tools/r31/run_atomnet_publication_host.py');base=ROOT/'app/build/r312';base.mkdir(parents=True,exist_ok=True)
 with tempfile.TemporaryDirectory(prefix='s00-observation-native-',dir=base) as directory:
  out=Path(directory);sources=out/'sources';platform=out/'platform';tests=out/'tests';sources.mkdir();platform.mkdir();tests.mkdir();defs=portable.platform_sources();defs['android/os/Build.java']='package android.os;public final class Build{public static final String MODEL="HOST_NATIVE_BOUNDARY";public static final class VERSION{public static final int SDK_INT=0;}}';files=[]
  for name,body in defs.items():
   f=sources/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body);files.append(f)
  javac=portable.java_tool('javac');java=portable.java_tool('java');compile=[str(javac),'-J-Djava.io.tmpdir='+str(out),'-sourcepath','','-implicit:none','-encoding','UTF-8'];subprocess.run([*compile,'-cp',str(classes)+os.pathsep+str(sdk),'-d',str(platform),*map(str,files)],cwd=ROOT,check=True)
  cp=os.pathsep.join(map(str,[platform,tests,classes,jdbc,sdk]+([resources] if resources.is_file() else [])));subprocess.run([*compile,'-cp',cp,'-d',str(tests),str(ROOT/'tools/r312/S00ObservationTransactionHost.java')],cwd=ROOT,check=True);subprocess.run([str(java),'-Djava.io.tmpdir='+str(out),'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.S00ObservationTransactionHost',str(out/'data/databases/main.db'),str(classes)],cwd=ROOT,check=True)
 print(json.dumps({'s00_kind':a.kind,'source_head':head,'support_sha256':support['classes_jar_sha256'],'historical_receipts_modified':False}));return 0
if __name__=='__main__':
 try:sys.exit(main())
 except Exception as error:print('S00_OBSERVATION_FAIL:'+type(error).__name__+':'+str(error),file=sys.stderr);sys.exit(1)
