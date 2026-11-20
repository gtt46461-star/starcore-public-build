#!/usr/bin/env python3
"""Recompile the complete staged Java source and run report-closure regressions.

Requires Python 3, JDK17, Android API36 android.jar, annotation.jar, the complete
R33.5 plan and a staged source checkout with its genuine R967 generated source.
No previous machine's classpath or cached class file is required.
"""
from pathlib import Path
import argparse,hashlib,io,json,os,subprocess,sys,zipfile

ROOT=Path(__file__).resolve().parent
ORIGINALS=[('STARCORE-e476ca6126c21e0368a62fe93c6a8db9e4a82b4eb4a.zip','e164852debfeab84eaffa4f98e215ce855bbeec5ff20334da5b0e0aab909f19a'),
           ('STARCORE-d8ca0632b9fc9ac425b04aa32600750bf16bb513e4c.zip','da57ec5917c8922132c331c600f607d0833054879955895a8550897174c2b18f')]
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def run(command,log):
    result=subprocess.run(command,capture_output=True,text=True)
    log.write_text(result.stdout+result.stderr,encoding='utf-8')
    if result.returncode:raise ValueError('COMMAND_FAILED:'+str(result.returncode)+':'+str(log))
    return result.stdout
def originals(plan,out):
    result=[];out.mkdir(parents=True,exist_ok=True)
    for i,(name,digest) in enumerate(ORIGINALS):
        source=plan/'evidence/original'/name
        if not source.is_file() or sha(source)!=digest:raise ValueError('ORIGINAL_REPORT_IDENTITY:'+name)
        with zipfile.ZipFile(source) as archive:
            members=[x for x in archive.namelist() if x.endswith('/STARCORE_FULL_REPORT.zip')]
            if len(members)!=1 or archive.getinfo(members[0]).file_size>256*1024*1024:raise ValueError('REPORT_INNER_BOUND')
            data=archive.read(members[0])
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            if archive.getinfo('R30_REPORT.json').file_size>128*1024*1024:raise ValueError('REPORT_JSON_BOUND')
            body=archive.read('R30_REPORT.json')
        dest=out/('report_'+str(i+1)+'.json');dest.write_bytes(body);result.append(dest)
    return result
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,required=True);p.add_argument('--plan',type=Path,required=True)
    p.add_argument('--android-jar',type=Path,required=True);p.add_argument('--annotation-jar',type=Path,required=True);p.add_argument('--java',required=True);p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();source=a.source.resolve();plan=a.plan.resolve();out=a.out.resolve()
    if out.exists() or out.is_relative_to(source):raise ValueError('FRESH_EXTERNAL_OUTPUT_REQUIRED')
    out.mkdir(parents=True)
    commands=[]
    def invoke(argv,filename):commands.append(argv);return run(argv,out/filename)
    invoke([sys.executable,str(ROOT/'apply_report_payload.py'),'--source',str(source),'--check'],'payload-check.log')
    invoke([sys.executable,str(ROOT/'generate_need_registry.py'),'--plan',str(plan),'--output',str(source/'app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityNeedRegistryR335.java'),'--check'],'registry-check.log')
    checker=plan/'verification/typecheck_full_source_r335.py'
    if not checker.is_file():raise ValueError('PLAN_TYPECHECKER_MISSING')
    invoke([sys.executable,str(checker),'--repo',str(source),'--android-jar',str(a.android_jar.resolve()),'--annotation-jar',str(a.annotation_jar.resolve()),'--java',a.java,'--jdk-mode','jdk17','--out',str(out/'java')],'typecheck.log')
    type_receipt=json.loads((out/'java/RECEIPT.json').read_text());classes=out/'java/classes'
    classpath=os.pathsep.join(map(str,[classes,a.android_jar.resolve(),a.annotation_jar.resolve()]))
    invoke([a.java,'-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-encoding','UTF-8','-d',str(out/'test-classes'),'-cp',classpath,str(ROOT/'tests/ReportClosureTest.java')],'test-compile.log')
    report_paths=originals(plan,out/'original-reports')
    stdout=invoke([a.java,'-Xmx512m','-cp',str(out/'test-classes')+os.pathsep+classpath,'com.ethk3.novaonepro.strategyv2.ReportClosureTest',*map(str,report_paths)],'host-execution.log')
    result=json.loads(stdout);(out/'host-report-readback.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    result={'status':result['status'],'assertions':result['assertions'],'sourceJavaCount':type_receipt['sourceJavaCount'],'javaTypeInputSha256':type_receipt['inputSha256'],'sourceBytesVerified':7,'originalReports':[{'path':str(x),'sha256':sha(x)} for x in report_paths],'commands':commands,'scope':'ALL_JAVA_TYPES_AND_PRODUCTION_REPORT_HOST_READBACK; ANDROID_SQL_DEVICE_AND_NATURAL_QUALITY_NOT_RUN'}
    (out/'REPORT_CLOSURE_RECEIPT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ['commands','originalReports']},ensure_ascii=False))

if __name__=='__main__':
    try:main()
    except (ValueError,KeyError,TypeError,OSError,zipfile.BadZipFile) as e:print('REPORT_TEST_REFUSED:'+str(e),file=sys.stderr);sys.exit(2)
