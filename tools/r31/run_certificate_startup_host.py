#!/usr/bin/env python3
"""Native SQLite reproducer for the CI015 Certificate critical fingerprint failure. HOST, not ART/device."""
import os,re,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ['SQLITE_JDBC'])
    classes=Path(os.environ.get('R31_CLASSES',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
    work=Path(tempfile.mkdtemp(prefix='r31-cert-startup-'));plat,host=work/'platform',work/'host';plat.mkdir();host.mkdir()
    shim=[str(p) for p in (ROOT/'tools/r31/ddl_host').rglob('*.java')]+[str(ROOT/'tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java')]+[str(p) for p in (ROOT/'verification/r932/android-json').rglob('*.java')]
    text=(ROOT/'tools/r31/verify_platform_readiness.py').read_text();adapters=eval(re.search(r"adapters=(\{.*?\})\n",text,re.S).group(1))
    for name,body in adapters.items():q=work/'adapters'/name;q.parent.mkdir(parents=True,exist_ok=True);q.write_text(body);shim.append(str(q))
    subprocess.run(['javac','-nowarn','-encoding','UTF-8','-cp',str(sdk),'-d',str(plat),*shim],check=True)
    fixture=ROOT/'verification/r31/p02/schema_mainchain/runs/20261104T112314Z/source_snapshot/MainChainSchemaR31Host.java'
    cp=os.pathsep.join(map(str,[plat,classes,sdk,jdbc]))
    subprocess.run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',str(host),str(fixture),str(ROOT/'tools/r31/certificate_startup_host/CertificateStartupR31Host.java')],check=True)
    r=subprocess.run(['java','-Xmx512m','-cp',os.pathsep.join(map(str,[plat,host,classes,sdk,jdbc])),'com.ethk3.novaonepro.CertificateStartupR31Host',str(work/'startup.db'),os.environ.get('R31_ORDER','TEST_OPEN')],capture_output=True,text=True,timeout=600)
    out=r.stdout+r.stderr;print(out[-12000:])
    order=os.environ.get('R31_ORDER','TEST_OPEN')
    if order.startswith('NEG_'):sys.exit(0 if 'NEGATIVE_REFUSED|' in out and 'NEGATIVE_NOT_REFUSED|' not in out else 1)
    ok='SCHEMA_R27|PASS' in out and 'REPRODUCED|' not in out and 'FINGERPRINT_MISMATCH' not in out
    if order=='SU_AFTER_R27':ok=ok and 'SCHEMA_R27_SECOND|PASS' in out
    sys.exit(0 if ok else 1)
if __name__=='__main__':main()
