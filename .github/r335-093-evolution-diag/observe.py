"""Count the released scheduler's task receipts, public fault sites and cursor steps during a normal import."""
from pathlib import Path
import re
import subprocess
import sys

original = Path('.github/r335-093-research-import/observe.py').resolve()
sys.path.insert(0, str(original.parent))
source = original.read_text()
cutoff = source.index('try:main()')
program = source[:cutoff].replace('R335-093-research-import-', 'R335-093-evolution-diag-')
program = program.replace('(30,180,600,1200,2400,3600)', '(30,180,480,900)')
program = program.replace('if at in (600,2400,3600):read_panels()', 'if at==900:read_panels()')
namespace = {'__name__': 'original_research_observation'}
exec(compile(program, str(original), 'exec'), namespace)
query = namespace['query']
record = namespace['record']
original_sample = namespace['sample']
owners = ('r27-evolution', 'r27-replay-dataset', 'r27-atomnet', 'r27-chart-cache')
symbols = (
    'ACTIVE_SCHEMA_SESSION_TASK_LEASE_OR_RECOVERY','COOPERATIVE_PREPARED_STEP','DEPENDENCY(r27-replay-dataset)',
    'CORRECTED_REPLAY_DATASET','DRAINING_NOT_TERMINATED','RESULT_CLEANUP_PENDING','RUNNING','HALF_OPEN',
    'RESOURCE_WAIT','WRITER_HOLD_FAULT_BACKLOG','R31_COMPUTE_CAPACITY_WAIT','R26_UNSLICED_COMPUTATION_CAPACITY',
    'CP3_DB_BUSY_DEADLINE','CLAIM_WAIT','CLAIMED','READY','DRAIN','STALE','CANCEL','EXPIRE','RESEARCH',
    'QUARANTINE','UncertainCommit','StaleResearch','DatabaseLockedException','SQLiteException','Integrity','RetryLater',
    'ResearchStoreR30R1','ResearchInputPinR30R1','EvolutionPublicationStoreR31','EvolutionPublicationSqlR31',
    'BirthGateR27','FeatureCacheR27','RuntimeR27','CreationBudgetR31','CooperativeContinuationR26',
    'IllegalStateException','IllegalArgumentException','OutOfMemoryError','InterruptedException',
    'RuntimeCoordinatorR26.claimWithinR31','ResearchPoolR27.claimPrepared','revalidateResearchAdmissions','commitResearchAdmissions',
    'commitWithin','preparePublicationR31','submit','requireFence','CooperativeContinuationR26.resume','CONTINUATION',
    'NO_NEW_EVENT','EVOLUTION_STEP_','EVOLUTION_STAGING_','CLAIM','MEMORY','RESERV','LEASE','HEALTH')
source_dir = Path('app-source/app/src/main/java')
files = {f.name: f for f in source_dir.rglob('*.java')}
sources = {}
for name in ['RuntimeR27','RuntimeCoordinatorR26','CooperativeContinuationR26','ResearchPoolR27','ResearchStoreR30R1','BirthGateR27','EvolutionLoopR27','DatabaseCoordinator','ResearchWriteQueueR27','EvolutionPublicationStoreR31','PreparedTaskR30R1','ResearchInputPinR30R1','FeatureCacheR27','ReplayEvaluatorR27','ResearchContextR30R1','MemoryBudgetR31']:
    path = files.get(name+'.java')
    if path:
        sources[path.name] = len(path.read_text().splitlines())

def numbers(statement):
    try:
        result = query(statement)
        if result == '':return []
        return [[int(n) for n in l.split('|')] for l in result.splitlines()]
    except (ValueError,subprocess.TimeoutExpired):
        return None

def aggregate_counts(expr, source):
    out={}
    for pos in range(0,len(symbols),24):
        batch=symbols[pos:pos+24]
        fields=','.join("coalesce(sum(case when instr("+expr+",'"+s+"')>0 then 1 else 0 end),0)" for s in batch)
        counts=numbers('select '+fields+' from '+source)
        if counts is not None and len(counts)==1:
            out.update({s:v for s,v in zip(batch,counts[0]) if v>0})
    return out

def threads():
    observed={}
    try:
        pid=subprocess.check_output(['adb','shell','pidof','-s',namespace['f'].PKG],text=True,timeout=10).strip()
        if not re.fullmatch(r'[1-9][0-9]*',pid):return {'count':0}
        trace=subprocess.run(['adb','shell','debuggerd','-j',pid],capture_output=True,text=True,timeout=15)
        target=None
        for line in trace.stdout.splitlines():
            match=re.search(r'^\s*"([^"]+)"',line)
            if match:
                name=match[1]
                target=name if name in ('starcore-r30-prepared-r27-evolution','starcore-r30-prepared-r27-replay-dataset','starcore-r27-research-0','starcore-r27-research-1') else None
                if target:observed.setdefault(target,[])
            elif target:
                frame=re.search(r'\(([A-Za-z0-9_]+\.java):(\d+)\)',line)
                if frame and 0<int(frame[2])<=sources.get(frame[1],0):
                    observed[target].append([frame[1],int(frame[2])])
        return {'commandExit':trace.returncode,'publicSourceLines':observed}
    except (subprocess.TimeoutExpired,ValueError):return {'sampleAvailable':False}

def sampled(elapsed):
    original_sample(elapsed)
    result={}
    for task in owners:
        pred=" where task_id='"+task+"'"
        result[task]={
            'taskFenceStepsCursorCpu':numbers('select fence,steps,cursor,last_cpu_ms from r26_task'+pred),
            'observedAttemptsCpuUnitsCommittedFailed':numbers('select attempts,work_cpu_ms,observed_work_units,task_receipts_committed,task_receipts_failed from r31_task_observation'+pred),
            'claimsLevelGenerationFaultRounds':numbers('select level,generation,fault_rounds from r31_task_control'+pred),
            'progressReceiptsMovedCpu':numbers('select count(*),coalesce(sum(cursor_after!=cursor_before),0),coalesce(sum(cpu_ms),0) from r26_progress'+pred),
            'latestBlockedSymbols':aggregate_counts("coalesce(blocked_reason,'')",'r26_task'+pred),
            'faultSymbols':aggregate_counts("coalesce(last_stack,'')||' '||coalesce(last_cause_chain_json,'')||' '||coalesce(phase,'')||' '||coalesce(category,'')",'r31_runtime_fault'+pred),
            'lastProgressSymbols':aggregate_counts("coalesce(blocked_reason,'')",'(select blocked_reason from r26_progress'+pred+' order by receipt_id desc limit 25)'),
        }
    row=record['observations'][-1]
    row['schedulerEvidence']=result
    row['evolutionCursor']=numbers('select generation,step from r27_evolution_cursor order by generation desc limit 3')
    row['sourceLines']=threads()
    namespace['save']()
namespace['sample']=sampled
try:
    namespace['main']()
except Exception as error:
    record.update(completed=False,failureType=type(error).__name__)
    namespace['save']()
    raise
