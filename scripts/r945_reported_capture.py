from pathlib import Path
import os,subprocess,sys
r=Path(__file__).resolve().parents[1];o=r/'build/r945-tests';o.mkdir(parents=True,exist_ok=True)
a=r/'build/r930-batch34-sql';h=r/'build/r925-host/compiled-classes';p=Path(os.environ['ANDROID_PLATFORM_JAR'])
cp=os.pathsep.join(map(str,[o,r/'build/r944-tests',a,h,a/'platform-classes',p]))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',os.pathsep.join(map(str,[h,p])),'-d',str(o),*[str(r/'app/src/main/java/com/ethk3/novaonepro'/ (name+'.java')) for name in ['StagedResearchRosterR943','HistoricalStrategyInventionR908','ResearchPredrawCaptureR923']]],check=True)
f=[r/'verification/r925/stubs/android/os/CancellationSignal.java',r/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java',r/'verification/r925/stubs/com/ethk3/novaonepro/DbStore.java',r/'verification/r945/ReportedCaptureHarnessR945.java']
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(o),*map(str,f)],check=True)
subprocess.run(['java','-Xmx192m','-cp',cp,'com.ethk3.novaonepro.ReportedCaptureHarnessR945',str(r/'verification/r925/sqlite_bridge.py'),str(r/'verification/r945/reported_candidates.json'),*sys.argv[1:]],check=True)
