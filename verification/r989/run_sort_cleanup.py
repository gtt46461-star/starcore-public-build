#!/usr/bin/env python3
"""Actual sort failure/consumer test against complete freshly compiled product classes."""
from pathlib import Path
import argparse,json,subprocess,time,hashlib
p=argparse.ArgumentParser();p.add_argument('--classes',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--jdk-bin',type=Path,default=Path('/tmp/starcore-cdaf1f33/toolchain/jdk-17.0.16+8/bin'));p.add_argument('--android-jar',type=Path,default=Path('/tmp/starcore-cdaf1f33/toolchain/android-sdk/platforms/android-36/android.jar'));a=p.parse_args();src=Path(__file__).resolve().parents[2]
assert not a.out.exists()or not any(a.out.iterdir());a.out.mkdir(parents=True,exist_ok=True);drivers=a.out/'drivers';drivers.mkdir();cp=str(a.classes)+':'+str(a.android_jar);source=src/'verification/r989/ReportSortFailureCleanupR989.java';records=[]
for name,cmd in [('compile_driver_only',[str(a.jdk_bin/'javac'),'-cp',cp,'-d',str(drivers),str(source)]),('actual_current_sort_failure_cleanup',[str(a.jdk_bin/'java'),'-Xmx128m','-cp',str(drivers)+':'+cp,'com.ethk3.novaonepro.ReportSortFailureCleanupR989',str(a.out/'files')])]:
 t=time.monotonic()
 with(a.out/(name+'.log')).open('w')as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=120)
 records.append({'operation':name,'command':cmd,'exit_code':q.returncode,'elapsed_s':time.monotonic()-t})
 receipt={'operations':records,'status':'PASS'if q.returncode==0 else'FAIL','scope':'Actual current product; synthetic host files, no Android provider/process/production-data claim','actual_product_source_sha256':hashlib.sha256((src/'app/src/main/java/com/ethk3/novaonepro/ReportMemberPlanCP12.java').read_bytes()).hexdigest(),'actual_complete_product_class_sha256':hashlib.sha256((a.classes/'com/ethk3/novaonepro/ReportMemberPlanCP12.class').read_bytes()).hexdigest(),'test_source_sha256':hashlib.sha256(source.read_bytes()).hexdigest()};(a.out/'receipt.json').write_text(json.dumps(receipt,indent=2))
 if q.returncode:raise SystemExit(q.returncode)
print((a.out/'actual_current_sort_failure_cleanup.log').read_text())
