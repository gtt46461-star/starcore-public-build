#!/usr/bin/env python3
from pathlib import Path
import os,subprocess
r=Path(__file__).resolve().parents[1];out=r/'build/r941-tests';out.mkdir(parents=True,exist_ok=True)
platform=Path(os.environ['ANDROID_PLATFORM_JAR']);h=r/'build/r925-host/compiled-classes';a=r/'build/r930-batch34-sql';m=r/'build/r932-model-bundle'
cp=os.pathsep.join(map(str,[m,a,h,platform]))
files=[r/'verification/r941/RuntimeRepairHarnessR941.java']
for name in ['DiagnosticFileStoreR941','TransactionSchemaCacheR941','ResearchPipelineStatusR941','ResearchLearningContinuityR912','DeferredEvidenceWriterR36','V51AuthorizationCore','ControlledRealContractAuditR922','RealtimeProvenanceRepairR923']:
 files.append(r/'app/src/main/java/com/ethk3/novaonepro'/(name+'.java'))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(out),*map(str,files)],check=True)
cp=os.pathsep.join(map(str,[out,m,a,h,platform,a/'platform-classes']))
p=subprocess.run(['java','-Xmx96m','-cp',cp,'com.ethk3.novaonepro.RuntimeRepairHarnessR941',str(r/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
(r/'reports/r941/RUNTIME_REGRESSION.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
if p.returncode or 'R941_RUNTIME_REPAIR_PASS' not in p.stdout:raise SystemExit(1)
