from pathlib import Path
import os, subprocess
root=Path(__file__).resolve().parents[2]
out=root/'build/r960-scoring';out.mkdir(parents=True,exist_ok=True)
adapters=root/'build/r958-sqlite';classes=root/'build/r925-host/compiled-classes'
cp=os.pathsep.join(map(str,[out,adapters,classes,adapters/'android-runtime']))
for command,name in [(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(out),str(root/'verification/r960/host/android/os/SystemClock.java'),str(root/'verification/r960/ResumableScoringHarnessR960.java')],'compile.txt'),(['java','-Xmx256m','-cp',cp,'com.ethk3.novaonepro.ResumableScoringHarnessR960'],'results.txt')]:
    result=subprocess.run(command,cwd=root,capture_output=True,text=True)
    (out/name).write_text(result.stdout+result.stderr)
    print(result.stdout+result.stderr)
    if result.returncode:raise SystemExit(result.returncode)
