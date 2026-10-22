from pathlib import Path
import os, subprocess, zipfile, ast, shutil
R=Path(__file__).resolve().parents[2];O=Path(os.environ.get('STARCORE_SQLITE_OUT',str(R/'build/r958-sqlite')));O.mkdir(parents=True,exist_ok=True)
# Remove stale incremental production bytecode: every production owner must come from this full build.
shutil.rmtree(O/'com',ignore_errors=True)
A=Path(os.environ['ANDROID_PLATFORM_JAR']);H=Path(os.environ.get('STARCORE_HOST_CLASSES',str(R/'build/r925-host/compiled-classes')))
def write(name,data):
 p=O/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data);return p
s=(R/'verification/r925/stubs/com/ethk3/novaonepro/DbStore.java').read_text()
s=s.replace('public DatabaseCoordinator coordinatorR36()', 'public DatabaseCoordinator coordinatorForResearchR952(){return coordinatorR36();}\n    public DatabaseCoordinator coordinatorR36()')
s=s.replace('dirty FROM "+DbHelper.TABLE_DRAWS','dirty,open_time,fetch_time FROM "+DbHelper.TABLE_DRAWS')
s=s.replace('draw.issue=issue;', 'draw.issue=issue;draw.openTime=c.getLong(11);draw.fetchTime=c.getLong(12);')
pos=s.index('    public SQLiteDatabase readable()')
s=s[:pos]+'''    public java.util.List<Models.Draw> getDrawsBeforeIssue(String issue,int limit){
        java.util.List<Models.Draw> rows=new java.util.ArrayList<Models.Draw>();
        try(Cursor c=db.rawQuery("SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,open_time,fetch_time FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 AND issue<? ORDER BY issue DESC LIMIT ?",new String[]{issue,String.valueOf(limit)})){
            while(c.moveToNext()){Models.Draw d=new Models.Draw();d.issue=c.getString(0);for(int i=0;i<10;i++)d.numbers[i]=c.getInt(i+1);d.openTime=c.getLong(11);d.fetchTime=c.getLong(12);rows.add(d);}
        }java.util.Collections.reverse(rows);return rows;
    }
'''+s[pos:]
coordinator=(R/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java').read_text()
coordinator=coordinator.replace('    public boolean runAtomicWrite(String tag, final WriteRunnable work)', '    public boolean ensureR811ResearchSchemaOnce(){return ResearchSchemaCoordinatorR926.ensureToReady(this,8); }\n    public boolean runAtomicWrite(String tag, final WriteRunnable work)')
coordinator=coordinator.replace('catch (Throwable failure) { return fallback; }','catch (VirtualMachineError fatal) { throw fatal; } catch (ThreadDeath fatal) { throw fatal; } catch (Throwable failure) { failure.printStackTrace(System.err); return fallback; }')
# Exercise the production cause formatter even with the host coordinator adapter.
production_coordinator=(R/'app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java').read_text()
formatter=production_coordinator[production_coordinator.index('    static String describeCauseChain('):production_coordinator.index('    public static List<StorageRule> storageRules()')]
coordinator=coordinator.replace('    public SQLiteDatabase writable()',formatter+'    public SQLiteDatabase writable()')
files=[write('adapters/com/ethk3/novaonepro/DbStore.java',s),write('adapters/com/ethk3/novaonepro/DatabaseCoordinator.java',coordinator),R/'verification/r925/stubs/android/database/sqlite/SQLiteDatabase.java']
files+=list((R/'verification/r958').glob('*.java'))
files+=[R/'verification/r932/FamilyChampionHarnessR932.java',R/'verification/r945/ReportedCaptureHarnessR945.java',R/'verification/r943/PrimaryNetworkTrainingHarnessR943.java',R/'verification/r943/AlgorithmSettlementHarnessR943.java']
# Host clock/values adapters only; production research classes and SQL remain unmodified.
for n in ['android/util/Log.java']:
 p=R/'verification/r954/android-stubs'/n
 files.append(p)
files.append(write('adapters/android/os/CancellationSignal.java','package android.os; public final class CancellationSignal { private volatile boolean cancelled; public void cancel(){cancelled=true;} public boolean isCanceled(){return cancelled;} }'))
files.append(write('adapters/android/os/OperationCanceledException.java','package android.os; public class OperationCanceledException extends RuntimeException { public OperationCanceledException(){super("The operation has been canceled.");} }'))
for node in ast.walk(ast.parse((R/'scripts/r951_batch34_sql_harness.py').read_text())):
 if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id in ('content_values','cursor_wrapper','system_clock','debug') for t in node.targets):
  files.append(write('adapters/'+ {'content_values':'android/content/ContentValues.java','cursor_wrapper':'android/database/CursorWrapper.java','system_clock':'android/os/SystemClock.java','debug':'android/os/Debug.java'}[node.targets[0].id],(R/'verification/r960/host/android/os/SystemClock.java').read_text() if node.targets[0].id=='system_clock' else ast.literal_eval(node.value.args[1])))
files+=list((R/'verification/r932/android-json').rglob('*.java'))
cp=os.pathsep.join(map(str,[O,H,A]))
def run(cmd,name):
 p=subprocess.run(cmd,cwd=R,capture_output=True,text=True)
 (O/name).write_text(p.stdout+p.stderr)
 print(p.stdout+p.stderr)
 if p.returncode:raise SystemExit(p.returncode)
run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(O),*map(str,files)],'compile.log')
runtime=O/'android-runtime'
if not runtime.exists():
 with zipfile.ZipFile(A) as z:
  for n in z.namelist():
   if n.endswith('.class'):z.extract(n,runtime)
for cls,log in [('QualityMainlineHarnessR966','quality-mainline-r966.txt'),('EliteImprovementHarnessR966','elite-improvement-r966.txt'),('FamilyChampionHarnessR932','family-champion-r966.txt'),('PopulationEvolutionHarnessR966','population-evolution-r966.txt'),('ParentRetryHarnessR966','parent-retry-r966.txt'),('DevelopmentCapacityHarnessR966','development-capacity-r966.txt'),('EvolutionLaneHarnessR965','evolution-lanes-r965.txt'),('ResearchPruningHarnessR965','research-pruning-r965.txt'),('ModuleQueueHarnessR965','module-queue-r965.txt'),('RecoveryDispatchHarnessR965','recovery-dispatch-r965.txt'),('ReportedFailureHarnessR965','reported-failure-r965.txt'),('r952.EvolutionRecoveryHarnessR964','evolution-recovery-r964.txt'),('TrainingUpgradeHarnessR964','training-upgrade-r964.txt'),('FusionHarnessR964','fusion-r964.txt'),('ReportedWorldHarnessR964','reported-world-r964.txt'),('DevelopmentResumeHarnessR964','development-resume-r964.txt'),('ReportedCohortHarnessR963','reported-cohort-r963.txt'),('ResearchPagesHarnessR962','research-pages-r962.txt'),('BudgetedQueryHarnessR962','query-budget-r962.txt'),('r952.NumericLearningHarnessR958','results.txt'),('ReportedSchemaHarnessR958','phone-schema-after.txt'),('ResearchEntryHarnessR958','research-entry.txt'),('PrimaryNetworkTrainingHarnessR943','legacy-neural-training.txt'),('AlgorithmSettlementHarnessR943','legacy-algorithm-training.txt'),('r952.FailureRepairHarnessR958','failure-repair.txt'),('FrozenCandidateHarnessR958','frozen-candidate.txt'),('MolecularExecutionHarnessR958','molecular-execution.txt'),('WorldModelHarnessR958','world-model.txt'),('MatrixIntegrityHarnessR958','matrix-integrity.txt'),('PhoneExecutionRegressionHarnessR959','phone-execution-regression.txt'),('DevelopmentTrainingHarnessR959','development-training.txt'),('PerDrawContinuityHarnessR959','per-draw-continuity.txt'),('MultiAlgorithmRegressionHarnessR960','multi-algorithm-r960.txt')]:
 run(['java','-Xmx256m','-cp',os.pathsep.join(map(str,[O,H,runtime])),'com.ethk3.novaonepro.'+cls,str(R/'verification/r925/sqlite_bridge.py')],log)
# Run the critical transaction contract with the actual coordinator instead of the research adapter.
actual=O/'actual-coordinator';shutil.rmtree(actual,ignore_errors=True)
package=actual/'com/ethk3/novaonepro';package.mkdir(parents=True)
for p in (H/'com/ethk3/novaonepro').glob('DatabaseCoordinator*.class'):shutil.copy2(p,package/p.name)
actual_cp=os.pathsep.join(map(str,[actual,O,H,runtime]))
run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',actual_cp,'-d',str(actual),str(R/'verification/r960/HotTransactionHarnessR960.java')],'hot-transaction-compile.txt')
run(['java','-Xmx256m','-cp',actual_cp,'com.ethk3.novaonepro.HotTransactionHarnessR960',str(R/'verification/r925/sqlite_bridge.py')],'hot-transaction-r960.txt')
